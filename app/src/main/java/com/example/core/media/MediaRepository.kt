package com.example.core.media

import android.Manifest
import android.annotation.SuppressLint
import android.appwidget.AppWidgetManager
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.media.AudioManager
import android.media.MediaMetadata
import android.media.Rating
import android.media.session.MediaController
import android.media.session.MediaSessionManager
import android.media.session.PlaybackState
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.SystemClock
import android.provider.Settings
import androidx.core.content.ContextCompat
import com.example.core.compat.AndroidVersionCompat
import com.example.core.compat.DiscoveredPlayerApp
import com.example.core.compat.InstalledPlayersScanner
import com.example.core.compat.OemBatteryGuide
import com.example.core.compat.OemCompat
import com.example.core.library.LibraryRepository
import com.example.core.library.LocalTrackEntity
import com.example.data.AudioOutputCategory
import com.example.data.AudioOutputDevice
import com.example.data.CustomControlAction
import com.example.data.InstalledServiceStatus
import com.example.data.MediaSessionPreferences
import com.example.data.MusicServiceApp
import com.example.data.PreferencesRepository
import com.example.data.QueueTrackItem
import com.example.data.SessionPriorityRule
import com.example.data.UnifiedMediaState
import com.example.media.LocalAmbientAudioEngine
import com.example.media.MagicHiResPlaybackService
import com.example.media.MagicMediaBridgeService
import com.example.media.MagicNotificationListenerService
import com.example.quicksettings.LockScreenDeckActivity
import com.example.quicksettings.MagicTileSync
import com.example.widget.MagicWidgetCapsuleProvider
import com.example.widget.MagicWidgetCardProvider
import com.example.widget.MagicWidgetMiniProvider
import com.example.widget.MagicWidgetSquareProvider
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
import kotlinx.coroutines.withContext

@SuppressLint("StaticFieldLeak")
object MediaRepository {

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)

    private var appContext: Context? = null
    private var prefsRepo: PreferencesRepository? = null
    private var libraryRepo: LibraryRepository? = null
    private var mediaSessionManager: MediaSessionManager? = null
    private var activeSessionListener: MediaSessionManager.OnActiveSessionsChangedListener? = null
    private var progressJob: Job? = null

    val localEngine = LocalAmbientAudioEngine()

    private val externalControllers = mutableListOf<MediaController>()
    private val controllerCallbacks = mutableMapOf<String, MediaController.Callback>()
    private val lastActivityTimestamps = mutableMapOf<String, Long>()
    private val localArtworkCache = mutableMapOf<Long, Bitmap?>()
    private var manualSelectedPackage: String? = null
    private var currentSessionPrefs = MediaSessionPreferences()
    private var listenerConnectedFlag = false
    private var quickTileAddedFlag = false
    private var lastRecordedTrackKey: String? = null
    private var accumulatedPlaySeconds: Int = 0
    private var preFadeExternalStreamVolume: Int? = null
    private var lastDetectedOutputCategory: AudioOutputCategory? = null

    private val sleepTimerManager = SleepTimerManager(
        scope = scope,
        onApplyFadeVolumeFraction = { fraction -> applySleepFadeFraction(fraction) },
        onTimerTriggeredPause = { pauseActivePlaybackForSleepTimer() },
    )
    val sleepTimerState: StateFlow<SleepTimerState> = sleepTimerManager.state

    private val _playerState = MutableStateFlow(PlayerState())
    val playerState: StateFlow<PlayerState> = _playerState.asStateFlow()

    private val _mediaState = MutableStateFlow(UnifiedMediaState())
    val mediaState: StateFlow<UnifiedMediaState> = _mediaState.asStateFlow()

    private val _hasListenerPermission = MutableStateFlow(false)
    val hasListenerPermission: StateFlow<Boolean> = _hasListenerPermission.asStateFlow()

    private val _hasPostNotificationsPermission = MutableStateFlow(true)
    val hasPostNotificationsPermission: StateFlow<Boolean> = _hasPostNotificationsPermission.asStateFlow()

    private val _discoveredPlayers = MutableStateFlow<List<DiscoveredPlayerApp>>(emptyList())
    val discoveredPlayers: StateFlow<List<DiscoveredPlayerApp>> = _discoveredPlayers.asStateFlow()

    private val _installedServices = MutableStateFlow<List<InstalledServiceStatus>>(emptyList())
    val installedServices: StateFlow<List<InstalledServiceStatus>> = _installedServices.asStateFlow()

    private val _oemBatteryGuide = MutableStateFlow<OemBatteryGuide?>(null)
    val oemBatteryGuide: StateFlow<OemBatteryGuide?> = _oemBatteryGuide.asStateFlow()

    private val _bridgeActive = MutableStateFlow(false)
    val bridgeActive: StateFlow<Boolean> = _bridgeActive.asStateFlow()

    private val _systemHealth = MutableStateFlow(SystemHealthSummary())
    val systemHealth: StateFlow<SystemHealthSummary> = _systemHealth.asStateFlow()

    private val _audioOutputs = MutableStateFlow<List<AudioOutputDevice>>(emptyList())
    val audioOutputs: StateFlow<List<AudioOutputDevice>> = _audioOutputs.asStateFlow()

    private val _hiResReport = MutableStateFlow(HiResHardwareReport())
    val hiResReport: StateFlow<HiResHardwareReport> = _hiResReport.asStateFlow()

    @Synchronized
    fun init(context: Context) {
        if (appContext != null) {
            refreshPermissionsAndServices()
            return
        }
        val ctx = context.applicationContext
        appContext = ctx
        val repo = PreferencesRepository(ctx)
        prefsRepo = repo
        libraryRepo = LibraryRepository(ctx)
        mediaSessionManager = ctx.getSystemService(Context.MEDIA_SESSION_SERVICE) as? MediaSessionManager

        scope.launch {
            localEngine.state.collect { localState ->
                if ((localState.isActive || externalControllers.isEmpty()) && !MagicHiResPlaybackService.state.value.isActive) {
                    rebuildState()
                }
            }
        }

        scope.launch {
            MagicHiResPlaybackService.state.collect { hiResState ->
                hiResState.currentTrack?.let { track ->
                    if (!localArtworkCache.containsKey(track.albumId)) {
                        localArtworkCache[track.albumId] = loadLocalAlbumArtBitmap(ctx, track.albumArtUri)
                    }
                }
                if (hiResState.isActive) {
                    if (localEngine.state.value.isActive) {
                        localEngine.stop()
                    }
                    if (_bridgeActive.value) {
                        MagicMediaBridgeService.stop(ctx)
                    }
                }
                rebuildState()
            }
        }

        scope.launch {
            repo.mediaSessionPreferencesFlow.collect { prefs ->
                currentSessionPrefs = prefs
                refreshDiscoveredPlayers(ctx)
                rebuildState()
            }
        }

        scope.launch {
            repo.widgetConfigFlow.collect {
                MagicWidgetUpdater.updateAllWidgets(ctx)
                evaluateSystemHealth(ctx)
            }
        }

        scope.launch {
            repo.equalizerConfigFlow.collect { eqCfg ->
                localEngine.applyEqualizerConfig(eqCfg)
            }
        }

        scope.launch {
            repo.quickTileAddedFlow.collect { added ->
                quickTileAddedFlag = added
                evaluateSystemHealth(ctx)
            }
        }

        scope.launch {
            repo.lockAndQsConfigFlow.collect {
                if (_bridgeActive.value) {
                    MagicMediaBridgeService.syncNotification(ctx)
                }
            }
        }

        refreshPermissionsAndServices()
        refreshAudioOutputs()
        startProgressTicker()
    }

    fun isHiResServiceOwningNotification(): Boolean =
        MagicHiResPlaybackService.state.value.isActive

    fun onListenerConnectionChanged(connected: Boolean) {
        listenerConnectedFlag = connected
        refreshPermissionsAndServices()
    }

    fun setBridgeRunning(running: Boolean) {
        _bridgeActive.value = running
    }

    fun clearError() {
        _playerState.value = _playerState.value.copy(lastError = null)
    }

    fun refreshPermissionsAndServices() {
        val ctx = appContext ?: return
        val listenerGranted = isNotificationListenerEnabled(ctx)
        _hasListenerPermission.value = listenerGranted

        val postNotifGranted = if (AndroidVersionCompat.supportsPostNotificationsRuntime()) {
            ContextCompat.checkSelfPermission(ctx, Manifest.permission.POST_NOTIFICATIONS) ==
                PackageManager.PERMISSION_GRANTED
        } else {
            true
        }
        _hasPostNotificationsPermission.value = postNotifGranted

        if (listenerGranted) {
            attachMediaSessionListener(ctx)
        } else {
            listenerConnectedFlag = false
            detachMediaSessionListener()
        }

        _oemBatteryGuide.value = OemCompat.getBatteryAndBackgroundGuide(ctx)
        refreshDiscoveredPlayers(ctx)
        evaluateSystemHealth(ctx)
        rebuildState()
    }

    fun refreshAudioOutputs() {
        val ctx = appContext ?: return
        _audioOutputs.value = AudioOutputRouter.listOutputDevices(ctx)
        val report = HiResCapabilities.inspectHardware(ctx)
        _hiResReport.value = report
        if (lastDetectedOutputCategory != null && lastDetectedOutputCategory != report.activeCategory) {
            scope.launch {
                prefsRepo?.switchEqualizerOutputCategory(report.activeCategory)
            }
        }
        lastDetectedOutputCategory = report.activeCategory
    }

    fun selectAudioOutput(device: AudioOutputDevice): Boolean {
        val ctx = appContext ?: return false
        val ok = AudioOutputRouter.selectOutputRoute(ctx, device)
        refreshAudioOutputs()
        return ok
    }

    fun evaluateSystemHealth(ctx: Context? = appContext) {
        val c = ctx ?: return
        val batteryOk = _oemBatteryGuide.value?.isIgnoringBatteryOptimizations
            ?: OemCompat.getBatteryAndBackgroundGuide(c).isIgnoringBatteryOptimizations
        val hasWidget = hasAnyPinnedWidget(c)
        _systemHealth.value = SystemHealthCalculator.evaluate(
            hasListenerPermission = _hasListenerPermission.value,
            listenerConnected = listenerConnectedFlag,
            hasPostNotificationsPermission = _hasPostNotificationsPermission.value,
            isIgnoringBatteryOptimizations = batteryOk,
            hasPinnedWidget = hasWidget,
            hasQuickTileAdded = quickTileAddedFlag,
        )
    }

    fun hasAnyPinnedWidget(ctx: Context): Boolean {
        val awm = AppWidgetManager.getInstance(ctx) ?: return false
        val providers = listOf(
            MagicWidgetCapsuleProvider::class.java,
            MagicWidgetCardProvider::class.java,
            MagicWidgetSquareProvider::class.java,
            MagicWidgetMiniProvider::class.java,
        )
        return providers.any { cls ->
            runCatching { awm.getAppWidgetIds(ComponentName(ctx, cls)).isNotEmpty() }.getOrDefault(false)
        }
    }

    fun setTileAddedStatus(isPlayPause: Boolean, added: Boolean) {
        scope.launch {
            prefsRepo?.setQuickTileAdded(isPlayPause, added)
        }
    }

    fun setQuickTileAdded(tileKey: String, added: Boolean) {
        scope.launch {
            prefsRepo?.setQuickTileAdded(tileKey, added)
        }
    }

    fun playLocalTracks(tracks: List<LocalTrackEntity>, startIndex: Int = 0, shuffle: Boolean = false) {
        val ctx = appContext ?: return
        if (tracks.isEmpty()) return
        if (localEngine.state.value.isActive) {
            localEngine.stop()
        }
        if (_bridgeActive.value) {
            MagicMediaBridgeService.stop(ctx)
        }
        manualSelectedPackage = MusicServiceApp.LOCAL_ENGINE.packageName
        MagicHiResPlaybackService.playLocalQueue(ctx, tracks, startIndex, shuffle)
        rebuildState()
    }

    private suspend fun loadLocalAlbumArtBitmap(ctx: Context, artUri: String): Bitmap? = withContext(Dispatchers.IO) {
        if (artUri.isBlank()) return@withContext null
        runCatching {
            ctx.contentResolver.openInputStream(Uri.parse(artUri))?.use { stream ->
                BitmapFactory.decodeStream(stream)
            }
        }.getOrNull()
    }

    private fun isNotificationListenerEnabled(ctx: Context): Boolean {
        val flat = Settings.Secure.getString(ctx.contentResolver, "enabled_notification_listeners")
            ?: return false
        val component = ComponentName(ctx, MagicNotificationListenerService::class.java)
        val expected = component.flattenToString()
        val shortExpected = component.flattenToShortString()
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
            reportError("Falta permiso de acceso a notificaciones para leer reproductores.")
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

        val now = System.currentTimeMillis()
        filtered.forEach { controller ->
            lastActivityTimestamps.putIfAbsent(controller.packageName, now)
            val cb = object : MediaController.Callback() {
                override fun onPlaybackStateChanged(state: PlaybackState?) {
                    lastActivityTimestamps[controller.packageName] = System.currentTimeMillis()
                    rebuildState()
                }

                override fun onMetadataChanged(metadata: MediaMetadata?) {
                    lastActivityTimestamps[controller.packageName] = System.currentTimeMillis()
                    rebuildState()
                }

                override fun onQueueChanged(queue: MutableList<android.media.session.MediaSession.QueueItem>?) {
                    rebuildState()
                }

                override fun onSessionDestroyed() {
                    refreshPermissionsAndServices()
                }
            }
            controller.registerCallback(cb)
            controllerCallbacks[controller.packageName] = cb
        }

        refreshDiscoveredPlayers(ctx)
        rebuildState()
    }

    private fun refreshDiscoveredPlayers(ctx: Context) {
        val activePkgs = externalControllers.map { it.packageName }.toSet()
        val scanned = InstalledPlayersScanner.scanPlayers(ctx, activePkgs)
        _discoveredPlayers.value = scanned

        _installedServices.value = scanned
            .filter { !it.isVideoLikely || it.isActiveSession }
            .map { player ->
                InstalledServiceStatus(
                    service = player.serviceCategory,
                    isInstalled = player.isInstalled,
                    isActiveSession = player.isActiveSession,
                    packageName = player.packageName,
                    realAppName = player.appName,
                    appIcon = player.icon,
                    isManagedEnabled = !currentSessionPrefs.disabledPackages.contains(player.packageName),
                )
            }
    }

    fun rebuildState() {
        val ctx = appContext ?: return
        val hiRes = MagicHiResPlaybackService.state.value
        val local = localEngine.state.value
        val pm = ctx.packageManager
        val prefs = currentSessionPrefs

        val rawSnapshots = mutableListOf<SessionSnapshot>()

        if (hiRes.isActive && hiRes.currentTrack != null) {
            val track = hiRes.currentTrack!!
            rawSnapshots.add(
                SessionSnapshot(
                    packageName = MusicServiceApp.LOCAL_ENGINE.packageName,
                    appName = MusicServiceApp.LOCAL_ENGINE.displayName,
                    service = MusicServiceApp.LOCAL_ENGINE,
                    title = track.title,
                    artist = track.artist,
                    album = track.album,
                    isPlaying = hiRes.isPlaying,
                    playbackStateCode = if (hiRes.isPlaying) PlaybackState.STATE_PLAYING else PlaybackState.STATE_PAUSED,
                    positionMs = hiRes.positionMs,
                    lastPositionUpdateElapsedMs = SystemClock.elapsedRealtime(),
                    playbackSpeed = if (hiRes.isPlaying) 1.0f else 0.0f,
                    durationMs = hiRes.durationMs.coerceAtLeast(track.durationMs),
                    albumArt = localArtworkCache[track.albumId],
                    albumArtUri = track.albumArtUri,
                    capabilities = PlaybackCapabilities(
                        canPlayPause = true,
                        canSkipNext = true,
                        canSkipPrevious = true,
                        canSeek = true,
                        canStop = true,
                    ),
                    isLocalEngine = true,
                    isSelected = false,
                    lastActiveTimestampMs = System.currentTimeMillis(),
                    canFavorite = true,
                    isFavorite = track.isFavorite,
                    canShuffle = true,
                    isShuffleEnabled = hiRes.shuffleEnabled,
                    canRepeat = true,
                    isRepeatEnabled = hiRes.repeatEnabled,
                    queue = hiRes.toQueueItems(),
                    lyrics = hiRes.currentLyrics,
                ),
            )
        } else if (local.isActive) {
            val track = local.currentTrack
            rawSnapshots.add(
                SessionSnapshot(
                    packageName = MusicServiceApp.LOCAL_ENGINE.packageName,
                    appName = MusicServiceApp.LOCAL_ENGINE.displayName,
                    service = MusicServiceApp.LOCAL_ENGINE,
                    title = track.title,
                    artist = track.artist,
                    album = track.album,
                    isPlaying = local.isPlaying,
                    playbackStateCode = if (local.isPlaying) PlaybackState.STATE_PLAYING else PlaybackState.STATE_PAUSED,
                    positionMs = local.positionMs,
                    lastPositionUpdateElapsedMs = SystemClock.elapsedRealtime(),
                    playbackSpeed = if (local.isPlaying) 1.0f else 0.0f,
                    durationMs = track.durationMs,
                    albumArt = null,
                    albumArtUri = null,
                    capabilities = PlaybackCapabilities(
                        canPlayPause = true,
                        canSkipNext = true,
                        canSkipPrevious = true,
                        canSeek = true,
                        canStop = true,
                    ),
                    isLocalEngine = true,
                    isSelected = false,
                    lastActiveTimestampMs = System.currentTimeMillis(),
                    canFavorite = true,
                    isFavorite = local.isCurrentFavorite,
                    canShuffle = true,
                    isShuffleEnabled = local.shuffleEnabled,
                    canRepeat = true,
                    isRepeatEnabled = local.repeatEnabled,
                    queue = local.toQueueItems(),
                    lyrics = local.toLyricsData(),
                ),
            )
        }

        externalControllers.forEach { ctrl ->
            val pkg = ctrl.packageName
            if (prefs.disabledPackages.contains(pkg)) return@forEach
            if (prefs.ignoreVideoSessions && InstalledPlayersScanner.isLikelyVideoApp(pkg)) return@forEach

            val meta = ctrl.metadata
            val pb = ctrl.playbackState
            val svc = MusicServiceApp.fromPackageName(pkg)
            val realLabel = resolveAppLabel(pm, pkg, svc.displayName)
            val art: Bitmap? = meta?.getBitmap(MediaMetadata.METADATA_KEY_ALBUM_ART)
                ?: meta?.getBitmap(MediaMetadata.METADATA_KEY_ART)
                ?: meta?.getBitmap(MediaMetadata.METADATA_KEY_DISPLAY_ICON)
            val artUri = meta?.getString(MediaMetadata.METADATA_KEY_ALBUM_ART_URI)
                ?: meta?.getString(MediaMetadata.METADATA_KEY_ART_URI)
            val isPlaying = pb?.state == PlaybackState.STATE_PLAYING
            val caps = PlaybackCapabilities.fromPlaybackState(pb)

            val ratingObj = meta?.getRating(MediaMetadata.METADATA_KEY_USER_RATING)
            val favCustom = findMatchingCustomAction(caps.customActions, FAVORITE_KEYWORDS)
            val shuffleCustom = findMatchingCustomAction(caps.customActions, SHUFFLE_KEYWORDS)
            val repeatCustom = findMatchingCustomAction(caps.customActions, REPEAT_KEYWORDS)

            val supportsFavorite = (ctrl.ratingType != Rating.RATING_NONE) || (favCustom != null)
            val isFav = (ratingObj?.isThumbUp == true) || (ratingObj?.hasHeart() == true)

            val activeQueueId = pb?.activeQueueItemId ?: -1L
            val mappedQueue = ctrl.queue.orEmpty().map { item ->
                val desc = item.description
                QueueTrackItem(
                    queueId = item.queueId,
                    title = desc.title?.toString().orEmpty().ifBlank { "Pista en cola" },
                    artist = desc.subtitle?.toString().orEmpty().ifBlank { realLabel },
                    durationMs = 0L,
                    isCurrent = item.queueId == activeQueueId,
                )
            }

            rawSnapshots.add(
                SessionSnapshot(
                    packageName = pkg,
                    appName = realLabel,
                    service = svc,
                    title = meta?.getString(MediaMetadata.METADATA_KEY_TITLE) ?: realLabel,
                    artist = meta?.getString(MediaMetadata.METADATA_KEY_ARTIST)
                        ?: meta?.getString(MediaMetadata.METADATA_KEY_ALBUM_ARTIST)
                        ?: "Reproduciendo en $realLabel",
                    album = meta?.getString(MediaMetadata.METADATA_KEY_ALBUM) ?: realLabel,
                    isPlaying = isPlaying,
                    playbackStateCode = pb?.state ?: PlaybackState.STATE_NONE,
                    positionMs = (pb?.position ?: 0L).coerceAtLeast(0L),
                    lastPositionUpdateElapsedMs = pb?.lastPositionUpdateTime ?: 0L,
                    playbackSpeed = pb?.playbackSpeed ?: if (isPlaying) 1.0f else 0.0f,
                    durationMs = (meta?.getLong(MediaMetadata.METADATA_KEY_DURATION) ?: 0L).coerceAtLeast(0L),
                    albumArt = art,
                    albumArtUri = artUri,
                    capabilities = caps,
                    isLocalEngine = false,
                    isSelected = false,
                    lastActiveTimestampMs = lastActivityTimestamps[pkg] ?: 0L,
                    canFavorite = supportsFavorite,
                    isFavorite = isFav,
                    canShuffle = shuffleCustom != null,
                    isShuffleEnabled = false,
                    canRepeat = repeatCustom != null,
                    isRepeatEnabled = false,
                    queue = mappedQueue,
                    lyrics = null,
                ),
            )
        }

        val chosen = selectBestSession(
            sessions = rawSnapshots,
            manualSelectedPackage = manualSelectedPackage,
            prefs = prefs,
        )

        val finalSnapshots = rawSnapshots.map { snap ->
            snap.copy(isSelected = chosen != null && snap.packageName == chosen.packageName)
        }
        val selectedSnap = finalSnapshots.firstOrNull { it.isSelected }

        val listenerState = when {
            !_hasListenerPermission.value -> ListenerConnectionState.PERMISSION_MISSING
            listenerConnectedFlag -> ListenerConnectionState.CONNECTED
            else -> ListenerConnectionState.DISCONNECTED_REBINDING
        }

        val (volumePercent, isMuted) = readStreamVolume(ctx)
        val interpolatedPos = selectedSnap?.computeInterpolatedPosition() ?: 0L

        val newPlayerState = PlayerState(
            activeSession = selectedSnap,
            sessions = finalSnapshots,
            interpolatedPositionMs = interpolatedPos,
            volumePercent = volumePercent,
            isMuted = isMuted,
            listenerState = listenerState,
            priorityRule = prefs.priorityRule,
            ignoreVideoSessions = prefs.ignoreVideoSessions,
            lastError = _playerState.value.lastError,
        )

        _playerState.value = newPlayerState
        val unified = newPlayerState.toUnifiedMediaState()
        _mediaState.value = unified

        // Record history & check end-of-track sleep timer
        if (unified.hasActiveMedia && unified.isPlaying) {
            val key = unified.trackKey
            if (key != lastRecordedTrackKey && unified.title.isNotBlank()) {
                lastRecordedTrackKey = key
                scope.launch {
                    prefsRepo?.recordTrackPlay(
                        trackKey = key,
                        title = unified.title,
                        artist = unified.artist,
                        album = unified.album,
                        packageName = unified.packageName,
                        serviceId = unified.service.id,
                        durationMs = unified.durationMs,
                    )
                }
            }
            sleepTimerManager.onTrackProgressOrChange(
                currentTrackKey = key,
                positionMs = unified.positionMs,
                durationMs = unified.durationMs,
                isPlaying = unified.isPlaying,
            )
        }

        MagicWidgetUpdater.updateAllWidgets(ctx)
        MagicTileSync.requestTileUpdates(ctx)
        if (_bridgeActive.value && !isHiResServiceOwningNotification()) {
            MagicMediaBridgeService.syncNotification(ctx)
        }
    }

    fun selectBestSession(
        sessions: List<SessionSnapshot>,
        manualSelectedPackage: String?,
        prefs: MediaSessionPreferences,
    ): SessionSnapshot? = SessionSelector.selectBestSession(
        sessions = sessions,
        manualSelectedPackage = manualSelectedPackage,
        prefs = prefs,
    )

    fun selectSession(packageName: String) {
        manualSelectedPackage = packageName
        if (packageName == MusicServiceApp.LOCAL_ENGINE.packageName) {
            if (MagicHiResPlaybackService.state.value.isActive) {
                MagicHiResPlaybackService.playPause()
            } else {
                localEngine.startOrResume()
            }
        } else {
            if (MagicHiResPlaybackService.state.value.isPlaying) {
                MagicHiResPlaybackService.playPause()
            }
            if (localEngine.state.value.isPlaying) {
                localEngine.pause()
            }
            if (localEngine.state.value.isActive) {
                localEngine.stop()
            }
        }
        rebuildState()
    }

    fun playPause() {
        val active = _playerState.value.activeSession
        if (active != null && active.isLocalEngine) {
            if (MagicHiResPlaybackService.state.value.isActive) {
                MagicHiResPlaybackService.playPause()
            } else {
                if (active.isPlaying) localEngine.pause() else localEngine.startOrResume()
            }
            rebuildState()
            return
        }

        val ctrl = activeExternalController()
        if (ctrl != null) {
            runCatching {
                val isPlaying = ctrl.playbackState?.state == PlaybackState.STATE_PLAYING
                if (isPlaying) ctrl.transportControls.pause() else ctrl.transportControls.play()
            }.onFailure {
                reportError("No se pudo alternar reproducción en ${active?.appName ?: "reproductor"}.")
            }
        } else {
            localEngine.startOrResume()
            appContext?.let { MagicMediaBridgeService.startOrUpdate(it) }
        }
        rebuildState()
    }

    fun next() {
        val active = _playerState.value.activeSession
        if (active != null && active.isLocalEngine) {
            if (MagicHiResPlaybackService.state.value.isActive) {
                MagicHiResPlaybackService.next()
            } else {
                localEngine.nextTrack()
            }
            rebuildState()
            return
        }
        val ctrl = activeExternalController()
        if (ctrl == null) {
            reportError("No hay reproductor activo para avanzar pista.")
            return
        }
        if (active != null && !active.capabilities.canSkipNext) {
            reportError("${active.appName} no admite saltar a la siguiente pista aquí.")
            return
        }
        runCatching { ctrl.transportControls.skipToNext() }
            .onFailure { reportError("Error al saltar pista en ${active?.appName ?: "reproductor"}.") }
    }

    fun previous() {
        val active = _playerState.value.activeSession
        if (active != null && active.isLocalEngine) {
            if (MagicHiResPlaybackService.state.value.isActive) {
                MagicHiResPlaybackService.previous()
            } else {
                localEngine.previousTrack()
            }
            rebuildState()
            return
        }
        val ctrl = activeExternalController()
        if (ctrl == null) {
            reportError("No hay reproductor activo para retroceder pista.")
            return
        }
        if (active != null && !active.capabilities.canSkipPrevious) {
            reportError("${active.appName} no admite ir a la pista anterior aquí.")
            return
        }
        runCatching { ctrl.transportControls.skipToPrevious() }
            .onFailure { reportError("Error al retroceder pista en ${active?.appName ?: "reproductor"}.") }
    }

    fun seekTo(positionMs: Long) {
        val active = _playerState.value.activeSession
        if (active != null && active.isLocalEngine) {
            if (MagicHiResPlaybackService.state.value.isActive) {
                MagicHiResPlaybackService.seekTo(positionMs)
            } else {
                localEngine.seekTo(positionMs)
            }
            rebuildState()
            return
        }
        val ctrl = activeExternalController() ?: return
        if (active != null && !active.capabilities.canSeek) {
            reportError("${active.appName} no permite mover la barra de progreso.")
            return
        }
        runCatching { ctrl.transportControls.seekTo(positionMs.coerceAtLeast(0L)) }
            .onFailure { reportError("No se pudo actualizar la posición.") }
    }

    fun seekBy(deltaMs: Long) {
        val active = _playerState.value.activeSession
        if (active != null && active.isLocalEngine) {
            if (MagicHiResPlaybackService.state.value.isActive) {
                MagicHiResPlaybackService.seekBy(deltaMs)
            } else {
                localEngine.seekBy(deltaMs)
            }
            rebuildState()
            return
        }
        val currentPos = _playerState.value.interpolatedPositionMs
        seekTo((currentPos + deltaMs).coerceAtLeast(0L))
    }

    fun toggleFavorite() {
        val active = _playerState.value.activeSession
        if (active != null && active.isLocalEngine) {
            val hiResTrack = MagicHiResPlaybackService.state.value.currentTrack
            if (hiResTrack != null) {
                val nextFav = !hiResTrack.isFavorite
                scope.launch {
                    libraryRepo?.setTrackFavorite(hiResTrack.id, nextFav)
                }
            } else {
                localEngine.toggleFavoriteCurrent()
            }
            rebuildState()
            return
        }
        val ctrl = activeExternalController() ?: return
        val favCustom = findMatchingCustomAction(active?.capabilities?.customActions.orEmpty(), FAVORITE_KEYWORDS)
        if (favCustom != null) {
            sendCustomSessionAction(favCustom.action)
            return
        }
        if (ctrl.ratingType == Rating.RATING_HEART) {
            val nextHeart = !(_playerState.value.activeSession?.isFavorite ?: false)
            runCatching { ctrl.transportControls.setRating(Rating.newHeartRating(nextHeart)) }
        } else if (ctrl.ratingType == Rating.RATING_THUMB_UP_DOWN) {
            val nextUp = !(_playerState.value.activeSession?.isFavorite ?: false)
            runCatching { ctrl.transportControls.setRating(Rating.newThumbRating(nextUp)) }
        } else {
            reportError("Este reproductor no expone valoración en MediaSession.")
        }
    }

    fun toggleShuffle() {
        val active = _playerState.value.activeSession
        if (active != null && active.isLocalEngine) {
            if (MagicHiResPlaybackService.state.value.isActive) {
                MagicHiResPlaybackService.toggleShuffle()
            } else {
                localEngine.toggleShuffle()
            }
            rebuildState()
            return
        }
        val shuffleCustom = findMatchingCustomAction(active?.capabilities?.customActions.orEmpty(), SHUFFLE_KEYWORDS)
        if (shuffleCustom != null) {
            sendCustomSessionAction(shuffleCustom.action)
        } else {
            reportError("${active?.appName ?: "El reproductor"} no expone modo aleatorio externo.")
        }
    }

    fun toggleRepeat() {
        val active = _playerState.value.activeSession
        if (active != null && active.isLocalEngine) {
            if (MagicHiResPlaybackService.state.value.isActive) {
                MagicHiResPlaybackService.toggleRepeat()
            } else {
                localEngine.toggleRepeat()
            }
            rebuildState()
            return
        }
        val repeatCustom = findMatchingCustomAction(active?.capabilities?.customActions.orEmpty(), REPEAT_KEYWORDS)
        if (repeatCustom != null) {
            sendCustomSessionAction(repeatCustom.action)
        } else {
            reportError("${active?.appName ?: "El reproductor"} no expone modo repetir externo.")
        }
    }

    fun skipToQueueItem(queueId: Long) {
        val active = _playerState.value.activeSession
        if (active != null && active.isLocalEngine) {
            if (MagicHiResPlaybackService.state.value.isActive) {
                MagicHiResPlaybackService.skipToQueueItem(queueId)
            } else {
                localEngine.playTrackByQueueId(queueId)
            }
            rebuildState()
            return
        }
        val ctrl = activeExternalController() ?: return
        runCatching { ctrl.transportControls.skipToQueueItem(queueId) }
            .onFailure { reportError("No se pudo saltar a ese elemento de la cola.") }
    }

    fun reorderLocalQueue(fromIndex: Int, toIndex: Int) {
        if (_playerState.value.activeSession?.isLocalEngine == true) {
            if (MagicHiResPlaybackService.state.value.isActive) {
                MagicHiResPlaybackService.moveQueueItem(fromIndex, toIndex)
            } else {
                localEngine.moveQueueItem(fromIndex, toIndex)
            }
            rebuildState()
        }
    }

    fun startSleepTimerCountdown(minutes: Int, fadeOutEnabled: Boolean) {
        sleepTimerManager.startCountdown(minutes, fadeOutEnabled)
        val ctx = appContext
        if (ctx != null && !_bridgeActive.value && !isHiResServiceOwningNotification()) {
            MagicMediaBridgeService.startOrUpdate(ctx)
        }
        ctx?.let { MagicTileSync.requestTileUpdates(it) }
    }

    fun startSleepTimerEndOfTrack(fadeOutEnabled: Boolean) {
        sleepTimerManager.startEndOfTrack(_mediaState.value.trackKey, fadeOutEnabled)
        appContext?.let { MagicTileSync.requestTileUpdates(it) }
    }

    fun setSleepTimerFadeOut(enabled: Boolean) {
        sleepTimerManager.setFadeOutEnabled(enabled)
    }

    fun cancelSleepTimer() {
        sleepTimerManager.cancel()
        appContext?.let { MagicTileSync.requestTileUpdates(it) }
    }

    fun sendCustomSessionAction(action: String, extras: Bundle? = null) {
        val ctrl = activeExternalController() ?: return
        runCatching { ctrl.transportControls.sendCustomAction(action, extras) }
            .onFailure { reportError("Acción personalizada no soportada por este reproductor.") }
    }

    fun rotateActiveSource() {
        val all = _playerState.value.sessions
        if (all.size <= 1) {
            val ctx = appContext ?: return
            val installed = _discoveredPlayers.value.firstOrNull { it.isInstalled && !it.isVideoLikely }
            if (installed != null) {
                launchMusicApp(ctx, installed.packageName)
            } else {
                reportError("No hay otro reproductor de música instalado.")
            }
            return
        }
        val idx = all.indexOfFirst { it.isSelected }
        val nextIdx = (idx + 1) % all.size
        selectSession(all[nextIdx].packageName)
    }

    fun toggleMuteAudio() {
        val ctx = appContext ?: return
        if (_playerState.value.activeSession?.isLocalEngine == true) {
            if (MagicHiResPlaybackService.state.value.isActive) {
                MagicHiResPlaybackService.toggleMute()
            } else {
                localEngine.toggleMute()
            }
            rebuildState()
            return
        }
        val am = ctx.getSystemService(Context.AUDIO_SERVICE) as? AudioManager ?: return
        val isMuted = am.isStreamMute(AudioManager.STREAM_MUSIC)
        am.adjustStreamVolume(
            AudioManager.STREAM_MUSIC,
            if (isMuted) AudioManager.ADJUST_UNMUTE else AudioManager.ADJUST_MUTE,
            0,
        )
        rebuildState()
    }

    fun setMusicVolumePercent(percent: Int) {
        val ctx = appContext ?: return
        val am = ctx.getSystemService(Context.AUDIO_SERVICE) as? AudioManager ?: return
        val max = am.getStreamMaxVolume(AudioManager.STREAM_MUSIC).coerceAtLeast(1)
        val target = ((percent.coerceIn(0, 100) / 100f) * max).toInt().coerceIn(0, max)
        am.setStreamVolume(AudioManager.STREAM_MUSIC, target, 0)
        rebuildState()
    }

    fun executeCustomAction(action: CustomControlAction) {
        val ctx = appContext ?: return
        when (action) {
            CustomControlAction.REWIND_15 -> seekBy(-15_000L)
            CustomControlAction.FORWARD_15 -> seekBy(15_000L)
            CustomControlAction.SWITCH_SOURCE -> rotateActiveSource()
            CustomControlAction.OPEN_PLAYER -> openActiveOrPreferredApp(ctx)
            CustomControlAction.MUTE_AUDIO -> toggleMuteAudio()
            CustomControlAction.LIKE_TRACK -> toggleFavorite()
            CustomControlAction.SHUFFLE -> toggleShuffle()
            CustomControlAction.REPEAT -> toggleRepeat()
        }
    }

    fun openActiveOrPreferredApp(ctx: Context) {
        val pkg = _playerState.value.activeSession?.packageName.orEmpty()
        if (pkg.isNotBlank() && pkg != ctx.packageName) {
            launchMusicApp(ctx, pkg)
            return
        }
        val preferredPkg = currentSessionPrefs.preferredServicePackage
        val preferredInstalled = _discoveredPlayers.value.firstOrNull {
            it.packageName == preferredPkg && it.isInstalled
        }
        val anyInstalled = preferredInstalled
            ?: _discoveredPlayers.value.firstOrNull { it.isInstalled && !it.isVideoLikely }

        if (anyInstalled != null) {
            launchMusicApp(ctx, anyInstalled.packageName)
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
            runCatching { ctx.startActivity(launchIntent) }
                .onFailure { reportError("No se pudo abrir la aplicación seleccionada.") }
        } else {
            val marketIntent = Intent(
                Intent.ACTION_VIEW,
                Uri.parse("https://play.google.com/store/apps/details?id=$packageName"),
            ).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            runCatching { ctx.startActivity(marketIntent) }
                .onFailure { reportError("Tienda de aplicaciones no disponible en este dispositivo.") }
        }
    }

    private fun applySleepFadeFraction(fraction: Float) {
        localEngine.setVolumeMultiplier(fraction)
        MagicHiResPlaybackService.setSleepFadeMultiplier(fraction)
        val ctx = appContext ?: return
        if (_playerState.value.activeSession?.isLocalEngine == false) {
            val am = ctx.getSystemService(Context.AUDIO_SERVICE) as? AudioManager ?: return
            if (fraction < 0.99f) {
                if (preFadeExternalStreamVolume == null) {
                    preFadeExternalStreamVolume = am.getStreamVolume(AudioManager.STREAM_MUSIC)
                }
                val baseVol = preFadeExternalStreamVolume ?: return
                val scaled = (baseVol * fraction).toInt().coerceAtLeast(0)
                am.setStreamVolume(AudioManager.STREAM_MUSIC, scaled, 0)
            } else {
                preFadeExternalStreamVolume?.let { restored ->
                    am.setStreamVolume(AudioManager.STREAM_MUSIC, restored, 0)
                }
                preFadeExternalStreamVolume = null
            }
        }
    }

    private fun pauseActivePlaybackForSleepTimer() {
        val active = _playerState.value.activeSession
        if (active != null && active.isLocalEngine) {
            if (MagicHiResPlaybackService.state.value.isActive) {
                MagicHiResPlaybackService.playPause()
            } else {
                localEngine.pause()
            }
        } else if (active != null && active.isPlaying) {
            activeExternalController()?.transportControls?.pause()
        }
        rebuildState()
    }

    private fun activeExternalController(): MediaController? {
        val selectedPkg = _playerState.value.activeSession?.packageName ?: manualSelectedPackage
        return externalControllers.firstOrNull { it.packageName == selectedPkg }
            ?: externalControllers.firstOrNull()
    }

    private fun findMatchingCustomAction(
        actions: List<SessionCustomAction>,
        keywords: List<String>,
    ): SessionCustomAction? = actions.firstOrNull { act ->
        val haystack = "${act.action} ${act.name}".lowercase()
        keywords.any { haystack.contains(it) }
    }

    private fun resolveAppLabel(pm: PackageManager, packageName: String, fallback: String): String {
        return runCatching {
            val appInfo = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                pm.getApplicationInfo(packageName, PackageManager.ApplicationInfoFlags.of(0))
            } else {
                @Suppress("DEPRECATION")
                pm.getApplicationInfo(packageName, 0)
            }
            pm.getApplicationLabel(appInfo).toString()
        }.getOrDefault(fallback)
    }

    private fun readStreamVolume(ctx: Context): Pair<Int, Boolean> {
        val am = ctx.getSystemService(Context.AUDIO_SERVICE) as? AudioManager ?: return 50 to false
        val max = am.getStreamMaxVolume(AudioManager.STREAM_MUSIC).coerceAtLeast(1)
        val cur = am.getStreamVolume(AudioManager.STREAM_MUSIC).coerceIn(0, max)
        val pct = ((cur.toFloat() / max.toFloat()) * 100f).toInt()
        val muted = am.isStreamMute(AudioManager.STREAM_MUSIC) || cur == 0
        return pct to muted
    }

    private fun reportError(message: String) {
        _playerState.value = _playerState.value.copy(lastError = message)
    }

    private fun startProgressTicker() {
        progressJob?.cancel()
        progressJob = scope.launch {
            while (isActive) {
                delay(1000L)
                val ctx = appContext ?: continue
                val current = _playerState.value
                val active = current.activeSession ?: continue

                if (active.isPlaying) {
                    accumulatedPlaySeconds++
                    if (accumulatedPlaySeconds >= 10) {
                        val delta = accumulatedPlaySeconds
                        accumulatedPlaySeconds = 0
                        prefsRepo?.addListeningSeconds(delta)
                    }
                }

                if (!active.isPlaying || active.isLocalEngine) continue
                val interpolated = active.computeInterpolatedPosition()
                val updated = current.copy(interpolatedPositionMs = interpolated)
                _playerState.value = updated
                val unified = updated.toUnifiedMediaState()
                _mediaState.value = unified

                sleepTimerManager.onTrackProgressOrChange(
                    currentTrackKey = unified.trackKey,
                    positionMs = unified.positionMs,
                    durationMs = unified.durationMs,
                    isPlaying = unified.isPlaying,
                )

                // Only refresh widgets during active playback when the screen is awake
                if (AndroidVersionCompat.isInteractiveScreenOn(ctx)) {
                    MagicWidgetUpdater.updateAllWidgets(ctx)
                }
            }
        }
    }

    private val FAVORITE_KEYWORDS = listOf("like", "favorite", "fav", "heart", "thumb_up", "love")
    private val SHUFFLE_KEYWORDS = listOf("shuffle", "aleatorio")
    private val REPEAT_KEYWORDS = listOf("repeat", "repetir", "loop")
}
