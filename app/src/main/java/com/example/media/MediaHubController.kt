package com.example.media

import android.Manifest
import android.annotation.SuppressLint
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.media.AudioManager
import android.media.MediaMetadata
import android.media.session.MediaController
import android.media.session.MediaSessionManager
import android.media.session.PlaybackState
import android.net.Uri
import android.os.Build
import android.provider.Settings
import androidx.core.content.ContextCompat
import com.example.data.ActiveSessionInfo
import com.example.data.CustomControlAction
import com.example.data.InstalledServiceStatus
import com.example.data.MusicServiceApp
import com.example.data.PreferencesRepository
import com.example.data.UnifiedMediaState
import com.example.quicksettings.LockScreenDeckActivity
import com.example.quicksettings.MagicTileSync
import com.example.widget.MagicWidgetUpdater
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

@SuppressLint("StaticFieldLeak")
object MediaHubController {

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)

    private var appContext: Context? = null
    private var prefsRepo: PreferencesRepository? = null
    private var mediaSessionManager: MediaSessionManager? = null
    private var activeSessionListener: MediaSessionManager.OnActiveSessionsChangedListener? = null
    private var progressJob: Job? = null

    val localEngine = LocalAmbientAudioEngine()

    private val externalControllers = mutableListOf<MediaController>()
    private val controllerCallbacks = mutableMapOf<String, MediaController.Callback>()
    private var selectedPackageName: String? = null

    private val _mediaState = MutableStateFlow(UnifiedMediaState())
    val mediaState: StateFlow<UnifiedMediaState> = _mediaState.asStateFlow()

    private val _hasListenerPermission = MutableStateFlow(false)
    val hasListenerPermission: StateFlow<Boolean> = _hasListenerPermission.asStateFlow()

    private val _hasPostNotificationsPermission = MutableStateFlow(true)
    val hasPostNotificationsPermission: StateFlow<Boolean> = _hasPostNotificationsPermission.asStateFlow()

    private val _installedServices = MutableStateFlow<List<InstalledServiceStatus>>(emptyList())
    val installedServices: StateFlow<List<InstalledServiceStatus>> = _installedServices.asStateFlow()

    private val _bridgeActive = MutableStateFlow(false)
    val bridgeActive: StateFlow<Boolean> = _bridgeActive.asStateFlow()

    @Synchronized
    fun init(context: Context) {
        if (appContext != null) {
            refreshPermissionsAndServices()
            return
        }
        val ctx = context.applicationContext
        appContext = ctx
        prefsRepo = PreferencesRepository(ctx)
        mediaSessionManager = ctx.getSystemService(Context.MEDIA_SESSION_SERVICE) as? MediaSessionManager

        scope.launch {
            localEngine.state.collect { localState ->
                if (localState.isActive || externalControllers.isEmpty()) {
                    rebuildUnifiedState()
                }
            }
        }

        scope.launch {
            prefsRepo?.widgetConfigFlow?.collect {
                MagicWidgetUpdater.updateAllWidgets(ctx)
            }
        }

        scope.launch {
            prefsRepo?.lockAndQsConfigFlow?.collect {
                if (_bridgeActive.value) {
                    MagicMediaBridgeService.syncNotification(ctx)
                }
            }
        }

        refreshPermissionsAndServices()
        startProgressTicker()
    }

    fun setBridgeRunning(running: Boolean) {
        _bridgeActive.value = running
    }

    fun refreshPermissionsAndServices() {
        val ctx = appContext ?: return
        val listenerGranted = isNotificationListenerEnabled(ctx)
        _hasListenerPermission.value = listenerGranted

        val postNotifGranted = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            ContextCompat.checkSelfPermission(ctx, Manifest.permission.POST_NOTIFICATIONS) ==
                PackageManager.PERMISSION_GRANTED
        } else {
            true
        }
        _hasPostNotificationsPermission.value = postNotifGranted

        if (listenerGranted) {
            attachMediaSessionListener(ctx)
        } else {
            detachMediaSessionListener()
        }

        refreshInstalledServices(ctx)
        rebuildUnifiedState()
    }

    private fun isNotificationListenerEnabled(ctx: Context): Boolean {
        val flat = Settings.Secure.getString(ctx.contentResolver, "enabled_notification_listeners")
            ?: return false
        val expected = ComponentName(ctx, MagicNotificationListenerService::class.java).flattenToString()
        val shortExpected = ComponentName(ctx, MagicNotificationListenerService::class.java).flattenToShortString()
        return flat.split(':').any { it == expected || it == shortExpected || it.contains(ctx.packageName) }
    }

    private fun attachMediaSessionListener(ctx: Context) {
        val msm = mediaSessionManager ?: return
        val component = ComponentName(ctx, MagicNotificationListenerService::class.java)
        try {
            val controllers = msm.getActiveSessions(component)
            updateControllers(controllers)

            if (activeSessionListener == null) {
                val listener = MediaSessionManager.OnActiveSessionsChangedListener { updated ->
                    updateControllers(updated.orEmpty())
                }
                activeSessionListener = listener
                msm.addOnActiveSessionsChangedListener(listener, component)
            }
        } catch (_: SecurityException) {
            _hasListenerPermission.value = false
        }
    }

    private fun detachMediaSessionListener() {
        val msm = mediaSessionManager
        activeSessionListener?.let { listener ->
            runCatching { msm?.removeOnActiveSessionsChangedListener(listener) }
        }
        activeSessionListener = null
        clearControllerCallbacks()
    }

    private fun clearControllerCallbacks() {
        externalControllers.forEach { controller ->
            controllerCallbacks[controller.packageName]?.let { cb ->
                runCatching { controller.unregisterCallback(cb) }
            }
        }
        externalControllers.clear()
        controllerCallbacks.clear()
    }

    private fun updateControllers(controllers: List<MediaController>) {
        val ctx = appContext ?: return
        val filtered = controllers.filter { it.packageName != ctx.packageName }

        clearControllerCallbacks()
        externalControllers.addAll(filtered)

        filtered.forEach { controller ->
            val cb = object : MediaController.Callback() {
                override fun onPlaybackStateChanged(state: PlaybackState?) {
                    rebuildUnifiedState()
                }

                override fun onMetadataChanged(metadata: MediaMetadata?) {
                    rebuildUnifiedState()
                }

                override fun onSessionDestroyed() {
                    refreshPermissionsAndServices()
                }
            }
            controller.registerCallback(cb)
            controllerCallbacks[controller.packageName] = cb
        }

        if (selectedPackageName == null || filtered.none { it.packageName == selectedPackageName }) {
            val playing = filtered.firstOrNull {
                it.playbackState?.state == PlaybackState.STATE_PLAYING
            }
            selectedPackageName = (playing ?: filtered.firstOrNull())?.packageName
        }

        refreshInstalledServices(ctx)
        rebuildUnifiedState()
    }

    private fun refreshInstalledServices(ctx: Context) {
        val pm = ctx.packageManager
        val activePkgs = externalControllers.map { it.packageName }.toSet()
        val statuses = MusicServiceApp.supportedThirdParty.map { svc ->
            val installed = runCatching {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                    pm.getPackageInfo(svc.packageName, PackageManager.PackageInfoFlags.of(0))
                } else {
                    @Suppress("DEPRECATION")
                    pm.getPackageInfo(svc.packageName, 0)
                }
                true
            }.getOrDefault(false)
            InstalledServiceStatus(
                service = svc,
                isInstalled = installed,
                isActiveSession = activePkgs.contains(svc.packageName),
            )
        }
        _installedServices.value = statuses
    }

    fun rebuildUnifiedState() {
        val ctx = appContext ?: return
        val local = localEngine.state.value

        val sessions = externalControllers.map { ctrl ->
            val meta = ctrl.metadata
            val pb = ctrl.playbackState
            val svc = MusicServiceApp.fromPackageName(ctrl.packageName)
            ActiveSessionInfo(
                packageName = ctrl.packageName,
                service = svc,
                title = meta?.getString(MediaMetadata.METADATA_KEY_TITLE) ?: svc.displayName,
                artist = meta?.getString(MediaMetadata.METADATA_KEY_ARTIST) ?: "Sesión activa",
                isPlaying = pb?.state == PlaybackState.STATE_PLAYING,
                isSelected = ctrl.packageName == selectedPackageName && !local.isActive,
            )
        }.toMutableList()

        if (local.isActive) {
            sessions.add(
                0,
                ActiveSessionInfo(
                    packageName = MusicServiceApp.LOCAL_ENGINE.packageName,
                    service = MusicServiceApp.LOCAL_ENGINE,
                    title = local.currentTrack.title,
                    artist = local.currentTrack.artist,
                    isPlaying = local.isPlaying,
                    isSelected = true,
                ),
            )
        }

        val activeCtrl = externalControllers.firstOrNull { it.packageName == selectedPackageName }
            ?: externalControllers.firstOrNull()

        val newState = when {
            local.isActive -> {
                val track = local.currentTrack
                UnifiedMediaState(
                    title = track.title,
                    artist = track.artist,
                    album = track.album,
                    packageName = MusicServiceApp.LOCAL_ENGINE.packageName,
                    service = MusicServiceApp.LOCAL_ENGINE,
                    isPlaying = local.isPlaying,
                    positionMs = local.positionMs,
                    durationMs = track.durationMs,
                    albumArt = null,
                    isLocalEngine = true,
                    activeSessions = sessions,
                    lastUpdatedMs = System.currentTimeMillis(),
                )
            }
            activeCtrl != null -> {
                val meta = activeCtrl.metadata
                val pb = activeCtrl.playbackState
                val svc = MusicServiceApp.fromPackageName(activeCtrl.packageName)
                val art: Bitmap? = meta?.getBitmap(MediaMetadata.METADATA_KEY_ALBUM_ART)
                    ?: meta?.getBitmap(MediaMetadata.METADATA_KEY_ART)
                    ?: meta?.getBitmap(MediaMetadata.METADATA_KEY_DISPLAY_ICON)
                UnifiedMediaState(
                    title = meta?.getString(MediaMetadata.METADATA_KEY_TITLE)
                        ?: svc.displayName,
                    artist = meta?.getString(MediaMetadata.METADATA_KEY_ARTIST)
                        ?: meta?.getString(MediaMetadata.METADATA_KEY_ALBUM_ARTIST)
                        ?: "Reproduciendo en ${svc.displayName}",
                    album = meta?.getString(MediaMetadata.METADATA_KEY_ALBUM) ?: svc.displayName,
                    packageName = activeCtrl.packageName,
                    service = svc,
                    isPlaying = pb?.state == PlaybackState.STATE_PLAYING,
                    positionMs = (pb?.position ?: 0L).coerceAtLeast(0L),
                    durationMs = (meta?.getLong(MediaMetadata.METADATA_KEY_DURATION) ?: 0L).coerceAtLeast(0L),
                    albumArt = art,
                    isLocalEngine = false,
                    activeSessions = sessions,
                    lastUpdatedMs = System.currentTimeMillis(),
                )
            }
            else -> UnifiedMediaState(activeSessions = sessions)
        }

        _mediaState.value = newState
        MagicWidgetUpdater.updateAllWidgets(ctx)
        MagicTileSync.requestTileUpdates(ctx)
        if (_bridgeActive.value) {
            MagicMediaBridgeService.syncNotification(ctx)
        }
    }

    fun selectSession(packageName: String) {
        if (packageName == MusicServiceApp.LOCAL_ENGINE.packageName) {
            localEngine.startOrResume()
        } else {
            if (localEngine.state.value.isActive && !localEngine.state.value.isPlaying) {
                localEngine.stop()
            } else if (localEngine.state.value.isPlaying) {
                localEngine.pause()
                localEngine.stop()
            }
            selectedPackageName = packageName
        }
        rebuildUnifiedState()
    }

    fun playPause() {
        val current = _mediaState.value
        if (current.isLocalEngine) {
            if (current.isPlaying) localEngine.pause() else localEngine.startOrResume()
            rebuildUnifiedState()
            return
        }

        val ctrl = activeExternalController()
        if (ctrl != null) {
            val isPlaying = ctrl.playbackState?.state == PlaybackState.STATE_PLAYING
            if (isPlaying) ctrl.transportControls.pause() else ctrl.transportControls.play()
        } else {
            // No external active session found: start local Hi-Res engine so controls always work immediately
            localEngine.startOrResume()
            appContext?.let { MagicMediaBridgeService.startOrUpdate(it) }
        }
        rebuildUnifiedState()
    }

    fun next() {
        val current = _mediaState.value
        if (current.isLocalEngine) {
            localEngine.nextTrack()
            rebuildUnifiedState()
            return
        }
        activeExternalController()?.transportControls?.skipToNext()
    }

    fun previous() {
        val current = _mediaState.value
        if (current.isLocalEngine) {
            localEngine.previousTrack()
            rebuildUnifiedState()
            return
        }
        activeExternalController()?.transportControls?.skipToPrevious()
    }

    fun seekTo(positionMs: Long) {
        val current = _mediaState.value
        if (current.isLocalEngine) {
            localEngine.seekTo(positionMs)
            rebuildUnifiedState()
            return
        }
        activeExternalController()?.transportControls?.seekTo(positionMs.coerceAtLeast(0L))
    }

    fun seekBy(deltaMs: Long) {
        val current = _mediaState.value
        if (current.isLocalEngine) {
            localEngine.seekBy(deltaMs)
            rebuildUnifiedState()
            return
        }
        val ctrl = activeExternalController() ?: return
        val currentPos = ctrl.playbackState?.position ?: current.positionMs
        val target = (currentPos + deltaMs).coerceAtLeast(0L)
        ctrl.transportControls.seekTo(target)
    }

    fun rotateActiveSource() {
        val all = _mediaState.value.activeSessions
        if (all.size <= 1) {
            val ctx = appContext ?: return
            val installed = _installedServices.value.firstOrNull { it.isInstalled }
            installed?.let { launchMusicApp(ctx, it.service.packageName) }
            return
        }
        val idx = all.indexOfFirst { it.isSelected }
        val nextIdx = (idx + 1) % all.size
        selectSession(all[nextIdx].packageName)
    }

    fun toggleMuteAudio() {
        val ctx = appContext ?: return
        if (_mediaState.value.isLocalEngine) {
            localEngine.toggleMute()
            rebuildUnifiedState()
            return
        }
        val am = ctx.getSystemService(Context.AUDIO_SERVICE) as? AudioManager ?: return
        val isMuted = am.isStreamMute(AudioManager.STREAM_MUSIC)
        am.adjustStreamVolume(
            AudioManager.STREAM_MUSIC,
            if (isMuted) AudioManager.ADJUST_UNMUTE else AudioManager.ADJUST_MUTE,
            0,
        )
    }

    fun executeCustomAction(action: CustomControlAction) {
        val ctx = appContext ?: return
        when (action) {
            CustomControlAction.REWIND_15 -> seekBy(-15_000L)
            CustomControlAction.FORWARD_15 -> seekBy(15_000L)
            CustomControlAction.SWITCH_SOURCE -> rotateActiveSource()
            CustomControlAction.OPEN_PLAYER -> openActiveOrPreferredApp(ctx)
            CustomControlAction.MUTE_AUDIO -> toggleMuteAudio()
        }
    }

    fun openActiveOrPreferredApp(ctx: Context) {
        val pkg = _mediaState.value.packageName
        if (pkg.isNotBlank() && pkg != ctx.packageName) {
            launchMusicApp(ctx, pkg)
            return
        }
        val installed = _installedServices.value.firstOrNull { it.isInstalled }
        if (installed != null) {
            launchMusicApp(ctx, installed.service.packageName)
        } else {
            ctx.startActivity(
                Intent(ctx, LockScreenDeckActivity::class.java).apply {
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                },
            )
        }
    }

    fun launchMusicApp(ctx: Context, packageName: String) {
        val launchIntent = ctx.packageManager.getLaunchIntentForPackage(packageName)
        if (launchIntent != null) {
            launchIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            ctx.startActivity(launchIntent)
        } else {
            val marketIntent = Intent(
                Intent.ACTION_VIEW,
                Uri.parse("https://play.google.com/store/apps/details?id=$packageName"),
            ).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            runCatching { ctx.startActivity(marketIntent) }
        }
    }

    private fun activeExternalController(): MediaController? {
        return externalControllers.firstOrNull { it.packageName == selectedPackageName }
            ?: externalControllers.firstOrNull()
    }

    private fun startProgressTicker() {
        progressJob?.cancel()
        progressJob = scope.launch {
            while (isActive) {
                delay(1000L)
                val state = _mediaState.value
                if (state.isPlaying && !state.isLocalEngine && state.durationMs > 0L) {
                    val ctrl = activeExternalController()
                    val pos = ctrl?.playbackState?.position ?: (state.positionMs + 1000L)
                    _mediaState.value = state.copy(positionMs = pos.coerceAtMost(state.durationMs))
                    appContext?.let { MagicWidgetUpdater.updateAllWidgets(it) }
                }
            }
        }
    }
}
