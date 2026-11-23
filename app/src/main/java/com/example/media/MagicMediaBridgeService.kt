package com.example.media

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.graphics.BitmapFactory
import android.graphics.drawable.Icon
import android.media.MediaMetadata
import android.media.session.MediaSession
import android.media.session.PlaybackState
import android.os.Build
import android.os.Bundle
import android.os.IBinder
import com.example.R
import com.example.data.CustomControlAction
import com.example.data.LockAndQsConfig
import com.example.data.PreferencesRepository
import com.example.quicksettings.LockScreenDeckActivity
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch

class MagicMediaBridgeService : Service() {

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
    private lateinit var prefsRepo: PreferencesRepository
    private var mediaSession: MediaSession? = null

    override fun onCreate() {
        super.onCreate()
        MediaHubController.init(applicationContext)
        prefsRepo = PreferencesRepository(applicationContext)
        createNotificationChannel()
        initMediaSession()
        MediaHubController.setBridgeRunning(true)
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_STOP_BRIDGE -> {
                MediaHubController.setBridgeRunning(false)
                stopForeground(STOP_FOREGROUND_REMOVE)
                stopSelf()
                return START_NOT_STICKY
            }
            ACTION_SYNC -> {
                scope.launch {
                    val cfg = prefsRepo.getLockConfigSnapshot()
                    publishNotification(cfg)
                }
            }
            else -> {
                scope.launch {
                    val cfg = prefsRepo.getLockConfigSnapshot()
                    publishNotification(cfg)
                }
            }
        }
        return START_STICKY
    }

    private fun initMediaSession() {
        val session = MediaSession(this, "MagicMusicBridgeSession")
        session.setCallback(
            object : MediaSession.Callback() {
                override fun onPlay() {
                    MediaHubController.playPause()
                }

                override fun onPause() {
                    MediaHubController.playPause()
                }

                override fun onSkipToNext() {
                    MediaHubController.next()
                }

                override fun onSkipToPrevious() {
                    MediaHubController.previous()
                }

                override fun onSeekTo(pos: Long) {
                    MediaHubController.seekTo(pos)
                }

                override fun onCustomAction(action: String, extras: Bundle?) {
                    val parsed = runCatching { CustomControlAction.valueOf(action) }.getOrNull()
                    if (parsed != null) {
                        MediaHubController.executeCustomAction(parsed)
                    }
                }
            },
        )
        session.isActive = true
        mediaSession = session
    }

    private fun publishNotification(cfg: LockAndQsConfig) {
        val session = mediaSession ?: return
        val media = MediaHubController.mediaState.value

        val artwork = media.albumArt ?: BitmapFactory.decodeResource(resources, R.drawable.img_magic_album_art)

        session.setMetadata(
            MediaMetadata.Builder()
                .putString(MediaMetadata.METADATA_KEY_TITLE, media.title)
                .putString(MediaMetadata.METADATA_KEY_ARTIST, "${media.artist} • ${media.service.displayName}")
                .putString(MediaMetadata.METADATA_KEY_ALBUM, media.album)
                .putLong(MediaMetadata.METADATA_KEY_DURATION, media.durationMs)
                .putBitmap(MediaMetadata.METADATA_KEY_ALBUM_ART, artwork)
                .build(),
        )

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

        val mediaStyle = Notification.MediaStyle()
            .setMediaSession(session.sessionToken)

        val builder = Notification.Builder(this, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_magic_music_note)
            .setContentTitle(media.title)
            .setContentText("${media.artist} • ${media.service.displayName}")
            .setSubText("MagicOS 10 • Pantalla de Bloqueo")
            .setLargeIcon(Icon.createWithBitmap(artwork))
            .setContentIntent(openDeckIntent)
            .setVisibility(
                if (cfg.showOnLockScreen) Notification.VISIBILITY_PUBLIC else Notification.VISIBILITY_PRIVATE,
            )
            .setOngoing(media.isPlaying)
            .setStyle(mediaStyle)

        val notification = builder.build()
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            startForeground(
                NOTIFICATION_ID,
                notification,
                ServiceInfo.FOREGROUND_SERVICE_TYPE_MEDIA_PLAYBACK,
            )
        } else {
            startForeground(NOTIFICATION_ID, notification)
        }
    }

    private fun iconResForAction(action: CustomControlAction): Int = when (action) {
        CustomControlAction.REWIND_15 -> R.drawable.ic_magic_replay15
        CustomControlAction.FORWARD_15 -> R.drawable.ic_magic_forward15
        CustomControlAction.SWITCH_SOURCE -> R.drawable.ic_magic_swap
        CustomControlAction.OPEN_PLAYER -> R.drawable.ic_magic_open
        CustomControlAction.MUTE_AUDIO -> R.drawable.ic_magic_mute
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val nm = getSystemService(NotificationManager::class.java) ?: return
            val channel = NotificationChannel(
                CHANNEL_ID,
                getString(R.string.channel_media_bridge_name),
                NotificationManager.IMPORTANCE_LOW,
            ).apply {
                description = getString(R.string.channel_media_bridge_desc)
                lockscreenVisibility = Notification.VISIBILITY_PUBLIC
                setShowBadge(false)
            }
            nm.createNotificationChannel(channel)
        }
    }

    override fun onDestroy() {
        MediaHubController.setBridgeRunning(false)
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

        fun startOrUpdate(context: Context) {
            val intent = Intent(context, MagicMediaBridgeService::class.java).apply {
                action = ACTION_SYNC
            }
            runCatching {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    context.startForegroundService(intent)
                } else {
                    context.startService(intent)
                }
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
