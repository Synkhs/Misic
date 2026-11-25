package com.example.media

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.drawable.Icon
import android.media.MediaMetadata
import android.media.session.MediaSession
import android.media.session.PlaybackState
import android.os.Bundle
import android.os.IBinder
import androidx.compose.ui.graphics.toArgb
import com.example.R
import com.example.core.compat.AndroidVersionCompat
import com.example.core.media.MediaRepository
import com.example.data.CustomControlAction
import com.example.data.LockAndQsConfig
import com.example.data.LockPrivacyLevel
import com.example.data.PreferencesRepository
import com.example.designsystem.ArtworkPaletteExtractor
import com.example.designsystem.VioletSeedColor
import com.example.quicksettings.LockScreenDeckActivity
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class MagicMediaBridgeService : Service() {

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
    private lateinit var prefsRepo: PreferencesRepository
    private var mediaSession: MediaSession? = null
    private var autoStopJob: Job? = null
    private var cachedPlaceholderBitmap: Bitmap? = null

    override fun onCreate() {
        super.onCreate()
        MediaRepository.init(applicationContext)
        prefsRepo = PreferencesRepository(applicationContext)
        createNotificationChannel()
        initMediaSession()
        MediaRepository.setBridgeRunning(true)
        scope.launch(Dispatchers.IO) {
            cachedPlaceholderBitmap = runCatching {
                BitmapFactory.decodeResource(resources, R.drawable.img_magic_album_art)
            }.getOrNull()
        }
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_STOP_BRIDGE -> {
                stopBridgeCleanly()
                return START_NOT_STICKY
            }
            ACTION_NOTIFICATION_CONTROL -> {
                val cmd = intent.getStringExtra(EXTRA_CONTROL_CMD).orEmpty()
                handleNotificationControl(cmd)
            }
        }

        scope.launch {
            val cfg = prefsRepo.getLockConfigSnapshot()
            if (!cfg.bridgeEnabled || MediaRepository.isHiResServiceOwningNotification()) {
                stopBridgeCleanly()
                return@launch
            }
            publishNotification(cfg)
            scheduleAutoStopIfIdle()
        }
        return START_NOT_STICKY
    }

    private fun handleNotificationControl(cmd: String) {
        when (cmd) {
            CMD_PLAY_PAUSE -> MediaRepository.playPause()
            CMD_PREVIOUS -> MediaRepository.previous()
            CMD_NEXT -> MediaRepository.next()
            CMD_CUSTOM_1, CMD_CUSTOM_2 -> {
                scope.launch {
                    val cfg = prefsRepo.getLockConfigSnapshot()
                    val action = if (cmd == CMD_CUSTOM_1) cfg.lockScreenAction1 else cfg.lockScreenAction2
                    MediaRepository.executeCustomAction(action)
                }
            }
        }
    }

    private fun scheduleAutoStopIfIdle() {
        autoStopJob?.cancel()
        val media = MediaRepository.mediaState.value
        val sleepActive = MediaRepository.sleepTimerState.value.isActive
        if (!media.isPlaying && !sleepActive) {
            autoStopJob = scope.launch {
                delay(25_000L)
                val nowMedia = MediaRepository.mediaState.value
                val nowSleep = MediaRepository.sleepTimerState.value.isActive
                if (!nowMedia.isPlaying && !nowSleep) {
                    stopBridgeCleanly()
                }
            }
        }
    }

    private fun stopBridgeCleanly() {
        autoStopJob?.cancel()
        MediaRepository.setBridgeRunning(false)
        stopForeground(STOP_FOREGROUND_REMOVE)
        stopSelf()
    }

    private fun initMediaSession() {
        val session = MediaSession(this, "MagicMusicBridgeSession")
        session.setCallback(
            object : MediaSession.Callback() {
                override fun onPlay() {
                    MediaRepository.playPause()
                }

                override fun onPause() {
                    MediaRepository.playPause()
                }

                override fun onSkipToNext() {
                    MediaRepository.next()
                }

                override fun onSkipToPrevious() {
                    MediaRepository.previous()
                }

                override fun onSeekTo(pos: Long) {
                    MediaRepository.seekTo(pos)
                }

                override fun onCustomAction(action: String, extras: Bundle?) {
                    val parsed = runCatching { CustomControlAction.valueOf(action) }.getOrNull()
                    if (parsed != null) {
                        MediaRepository.executeCustomAction(parsed)
                    }
                }
            },
        )
        session.isActive = true
        mediaSession = session
    }

    private suspend fun publishNotification(cfg: LockAndQsConfig) {
        val session = mediaSession ?: return
        val media = MediaRepository.mediaState.value
        val placeholderArt = cachedPlaceholderBitmap ?: withContext(Dispatchers.IO) {
            runCatching { BitmapFactory.decodeResource(resources, R.drawable.img_magic_album_art) }
                .getOrNull()
                .also { cachedPlaceholderBitmap = it }
        }
        val allowArtwork = cfg.showArtworkOnLockScreen && cfg.lockPrivacyLevel == LockPrivacyLevel.PUBLIC
        val artwork = if (allowArtwork) (media.albumArt ?: placeholderArt) else placeholderArt

        val metaBuilder = MediaMetadata.Builder()
            .putString(MediaMetadata.METADATA_KEY_TITLE, media.title)
            .putString(MediaMetadata.METADATA_KEY_ARTIST, "${media.artist} • ${media.service.displayName}")
            .putString(MediaMetadata.METADATA_KEY_ALBUM, media.album)
            .putLong(MediaMetadata.METADATA_KEY_DURATION, media.durationMs)
        if (allowArtwork && artwork != null && !artwork.isRecycled) {
            metaBuilder.putBitmap(MediaMetadata.METADATA_KEY_ALBUM_ART, artwork)
        }
        session.setMetadata(metaBuilder.build())

        val pbBuilder = PlaybackState.Builder()
            .setActions(
                PlaybackState.ACTION_PLAY or
                    PlaybackState.ACTION_PAUSE or
                    PlaybackState.ACTION_PLAY_PAUSE or
                    PlaybackState.ACTION_SKIP_TO_NEXT or
                    PlaybackState.ACTION_SKIP_TO_PREVIOUS or
                    PlaybackState.ACTION_SEEK_TO,
            )
            .setState(
                if (media.isPlaying) PlaybackState.STATE_PLAYING else PlaybackState.STATE_PAUSED,
                media.positionMs,
                if (media.isPlaying) 1.0f else 0.0f,
            )
            .addCustomAction(
                PlaybackState.CustomAction.Builder(
                    cfg.lockScreenAction1.name,
                    cfg.lockScreenAction1.shortLabel,
                    iconResForAction(cfg.lockScreenAction1),
                ).build(),
            )
            .addCustomAction(
                PlaybackState.CustomAction.Builder(
                    cfg.lockScreenAction2.name,
                    cfg.lockScreenAction2.shortLabel,
                    iconResForAction(cfg.lockScreenAction2),
                ).build(),
            )

        session.setPlaybackState(pbBuilder.build())

        val openDeckIntent = PendingIntent.getActivity(
            this,
            101,
            Intent(this, LockScreenDeckActivity::class.java).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP)
            },
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )

        val action0 = buildControlAction(
            requestCode = 201,
            cmd = CMD_CUSTOM_1,
            iconRes = iconResForAction(cfg.lockScreenAction1),
            title = cfg.lockScreenAction1.shortLabel,
        )
        val action1 = buildControlAction(
            requestCode = 202,
            cmd = CMD_PREVIOUS,
            iconRes = R.drawable.ic_magic_prev,
            title = getString(R.string.action_previous),
        )
        val action2 = buildControlAction(
            requestCode = 203,
            cmd = CMD_PLAY_PAUSE,
            iconRes = if (media.isPlaying) R.drawable.ic_magic_pause else R.drawable.ic_magic_play,
            title = getString(R.string.action_play_pause),
        )
        val action3 = buildControlAction(
            requestCode = 204,
            cmd = CMD_NEXT,
            iconRes = R.drawable.ic_magic_next,
            title = getString(R.string.action_next),
        )
        val action4 = buildControlAction(
            requestCode = 205,
            cmd = CMD_CUSTOM_2,
            iconRes = iconResForAction(cfg.lockScreenAction2),
            title = cfg.lockScreenAction2.shortLabel,
        )

        val compactIndices = cfg.compactActionIndices
            .filter { it in 0..4 }
            .distinct()
            .take(3)
            .ifEmpty { listOf(1, 2, 3) }
            .toIntArray()

        val mediaStyle = Notification.MediaStyle()
            .setMediaSession(session.sessionToken)
            .setShowActionsInCompactView(*compactIndices)

        val effectiveVisibility = when {
            !cfg.showOnLockScreen -> Notification.VISIBILITY_SECRET
            cfg.lockPrivacyLevel == LockPrivacyLevel.SECRET -> Notification.VISIBILITY_SECRET
            cfg.lockPrivacyLevel == LockPrivacyLevel.HIDE_SENSITIVE -> Notification.VISIBILITY_PRIVATE
            else -> Notification.VISIBILITY_PUBLIC
        }

        val palette = ArtworkPaletteExtractor.deriveFromFallbackSeed(
            seed = if (media.hasActiveMedia) media.service.accentColor else VioletSeedColor,
        )

        val publicFallbackNotification = Notification.Builder(this, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_magic_music_note)
            .setContentTitle(getString(R.string.app_name))
            .setContentText("Controles multimedia privados")
            .setVisibility(Notification.VISIBILITY_PUBLIC)
            .build()

        val builder = Notification.Builder(this, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_magic_music_note)
            .setContentTitle(media.title)
            .setContentText("${media.artist} • ${media.service.displayName}")
            .setSubText("MagicMusic • ${media.service.displayName}")
            .setContentIntent(openDeckIntent)
            .setVisibility(effectiveVisibility)
            .setPublicVersion(publicFallbackNotification)
            .setOngoing(media.isPlaying)
            .setColor(palette.dominant.toArgb())
            .setColorized(allowArtwork)
            .addAction(action0)
            .addAction(action1)
            .addAction(action2)
            .addAction(action3)
            .addAction(action4)
            .setStyle(mediaStyle)

        if (allowArtwork && artwork != null && !artwork.isRecycled) {
            builder.setLargeIcon(Icon.createWithBitmap(artwork))
        }

        val started = AndroidVersionCompat.startMediaPlaybackForegroundCompat(
            service = this,
            notificationId = NOTIFICATION_ID,
            notification = builder.build(),
        )
        if (!started) {
            MediaRepository.setBridgeRunning(false)
            stopSelf()
        }
    }

    private fun buildControlAction(
        requestCode: Int,
        cmd: String,
        iconRes: Int,
        title: String,
    ): Notification.Action {
        val intent = Intent(this, MagicMediaBridgeService::class.java).apply {
            action = ACTION_NOTIFICATION_CONTROL
            putExtra(EXTRA_CONTROL_CMD, cmd)
        }
        val pi = PendingIntent.getService(
            this,
            requestCode,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
        return Notification.Action.Builder(
            Icon.createWithResource(this, iconRes),
            title,
            pi,
        ).build()
    }

    private fun iconResForAction(action: CustomControlAction): Int = when (action) {
        CustomControlAction.REWIND_15 -> R.drawable.ic_magic_replay15
        CustomControlAction.FORWARD_15 -> R.drawable.ic_magic_forward15
        CustomControlAction.SWITCH_SOURCE -> R.drawable.ic_magic_swap
        CustomControlAction.OPEN_PLAYER -> R.drawable.ic_magic_open
        CustomControlAction.MUTE_AUDIO -> R.drawable.ic_magic_mute
        CustomControlAction.LIKE_TRACK -> R.drawable.ic_magic_favorite
        CustomControlAction.SHUFFLE -> R.drawable.ic_magic_shuffle
        CustomControlAction.REPEAT -> R.drawable.ic_magic_repeat
    }

    private fun createNotificationChannel() {
        val nm = getSystemService(NotificationManager::class.java) ?: return
        val channel = NotificationChannel(
            CHANNEL_ID,
            getString(R.string.channel_media_bridge_name),
            NotificationManager.IMPORTANCE_LOW,
        ).apply {
            description = getString(R.string.channel_media_bridge_desc)
            lockscreenVisibility = Notification.VISIBILITY_PUBLIC
            setShowBadge(false)
            setSound(null, null)
            enableVibration(false)
        }
        nm.createNotificationChannel(channel)
    }

    override fun onDestroy() {
        autoStopJob?.cancel()
        MediaRepository.setBridgeRunning(false)
        mediaSession?.isActive = false
        mediaSession?.release()
        mediaSession = null
        scope.cancel()
        super.onDestroy()
    }

    override fun onBind(intent: Intent?): IBinder? = null

    companion object {
        const val CHANNEL_ID = "magic_media_bridge_channel"
        const val NOTIFICATION_ID = 1016
        const val ACTION_SYNC = "com.example.media.ACTION_SYNC_BRIDGE"
        const val ACTION_STOP_BRIDGE = "com.example.media.ACTION_STOP_BRIDGE"
        const val ACTION_NOTIFICATION_CONTROL = "com.example.media.ACTION_NOTIFICATION_CONTROL"
        const val EXTRA_CONTROL_CMD = "extra_control_cmd"

        private const val CMD_CUSTOM_1 = "custom_1"
        private const val CMD_PREVIOUS = "previous"
        private const val CMD_PLAY_PAUSE = "play_pause"
        private const val CMD_NEXT = "next"
        private const val CMD_CUSTOM_2 = "custom_2"

        fun startOrUpdate(context: Context) {
            if (MediaRepository.isHiResServiceOwningNotification()) {
                return
            }
            val intent = Intent(context, MagicMediaBridgeService::class.java).apply {
                action = ACTION_SYNC
            }
            val started = AndroidVersionCompat.startForegroundServiceSafe(context, intent)
            if (!started) {
                MediaRepository.setBridgeRunning(false)
            }
        }

        fun syncNotification(context: Context) {
            startOrUpdate(context)
        }

        fun stop(context: Context) {
            val intent = Intent(context, MagicMediaBridgeService::class.java).apply {
                action = ACTION_STOP_BRIDGE
            }
            runCatching { context.startService(intent) }
        }
    }
}
