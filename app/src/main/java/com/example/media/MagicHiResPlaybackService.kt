package com.example.media

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import androidx.annotation.OptIn
import androidx.media3.common.AudioAttributes
import androidx.media3.common.C
import androidx.media3.common.MediaItem
import androidx.media3.common.MediaMetadata
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.DefaultRenderersFactory
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.exoplayer.audio.AudioSink
import androidx.media3.exoplayer.audio.DefaultAudioSink
import androidx.media3.session.MediaSession
import androidx.media3.session.MediaSessionService
import com.example.core.compat.AndroidVersionCompat
import com.example.core.library.LibraryRepository
import com.example.core.library.LocalTrackEntity
import com.example.core.library.MediaStoreScanner
import com.example.core.library.codecLabel
import com.example.core.media.LocalAudioEqualizerBridge
import com.example.core.media.MediaRepository
import com.example.data.EqualizerConfig
import com.example.data.LyricsData
import com.example.data.PreferencesRepository
import com.example.data.QueueTrackItem
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

data class HiResPlaybackState(
    val isActive: Boolean = false,
    val isPlaying: Boolean = false,
    val playlist: List<LocalTrackEntity> = emptyList(),
    val currentIndex: Int = 0,
    val positionMs: Long = 0L,
    val durationMs: Long = 0L,
    val shuffleEnabled: Boolean = false,
    val repeatEnabled: Boolean = false,
    val isMuted: Boolean = false,
    val currentLyrics: LyricsData? = null,
    val audioSessionId: Int = 0,
    val streamFormatBadge: String = "FLAC • 24 bit / 48 kHz",
) {
    val currentTrack: LocalTrackEntity?
        get() = playlist.getOrNull(currentIndex)

    fun toQueueItems(): List<QueueTrackItem> = playlist.mapIndexed { idx, track ->
        QueueTrackItem(
            queueId = track.id,
            title = track.title,
            artist = track.artist,
            durationMs = track.durationMs,
            isCurrent = idx == currentIndex,
        )
    }
}

@OptIn(UnstableApi::class)
class MagicHiResPlaybackService : MediaSessionService() {

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
    private lateinit var player: ExoPlayer
    private var mediaSession: MediaSession? = null
    private lateinit var prefsRepo: PreferencesRepository
    private lateinit var libraryRepo: LibraryRepository
    private val eqBridge = LocalAudioEqualizerBridge()

    private var eqConfig = EqualizerConfig()
    private var sleepFadeMultiplier = 1f
    private var positionJob: Job? = null
    private var activePlaylist: List<LocalTrackEntity> = emptyList()
    private var lastCountedTrackId: Long = -1L

    override fun onCreate() {
        super.onCreate()
        activeInstance = this
        prefsRepo = PreferencesRepository(applicationContext)
        libraryRepo = LibraryRepository(applicationContext)

        val renderersFactory = object : DefaultRenderersFactory(this) {
            override fun buildAudioSink(
                context: android.content.Context,
                enableFloatOutput: Boolean,
                enableAudioTrackPlaybackParams: Boolean,
            ): AudioSink {
                return DefaultAudioSink.Builder(context)
                    .setEnableFloatOutput(true)
                    .setEnableAudioTrackPlaybackParams(enableAudioTrackPlaybackParams)
                    .build()
            }
        }

        val audioAttributes = AudioAttributes.Builder()
            .setUsage(C.USAGE_MEDIA)
            .setContentType(C.AUDIO_CONTENT_TYPE_MUSIC)
            .build()

        player = ExoPlayer.Builder(this, renderersFactory)
            .setAudioAttributes(audioAttributes, true)
            .setHandleAudioBecomingNoisy(true)
            .build()

        player.addListener(playerListener)
        mediaSession = MediaSession.Builder(this, player)
            .setId("MagicMusicHiResSession")
            .build()

        scope.launch {
            prefsRepo.equalizerConfigFlow.collect { cfg ->
                eqConfig = cfg
                eqBridge.attachAndApply(player.audioSessionId, cfg)
                applyPlayerVolume()
            }
        }

        // Apply pending queue if service was started specifically to play local tracks
        pendingQueueRequest?.let { req ->
            pendingQueueRequest = null
            playQueueInternal(req.tracks, req.startIndex, req.shuffle)
        }
        startPositionTicker()
    }

    override fun onGetSession(controllerInfo: MediaSession.ControllerInfo): MediaSession? {
        return mediaSession
    }

    override fun onTaskRemoved(rootIntent: Intent?) {
        val p = mediaSession?.player
        if (p == null || !p.playWhenReady || p.mediaItemCount == 0) {
            stopSelf()
        }
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        super.onStartCommand(intent, flags, startId)
        if (intent?.action == ACTION_PLAY_QUEUE) {
            pendingQueueRequest?.let { req ->
                pendingQueueRequest = null
                playQueueInternal(req.tracks, req.startIndex, req.shuffle)
            }
        } else if (intent?.action == ACTION_STOP_ENGINE) {
            stopEngineCleanly()
        }
        return START_NOT_STICKY
    }

    private fun playQueueInternal(tracks: List<LocalTrackEntity>, startIndex: Int, shuffle: Boolean) {
        if (tracks.isEmpty()) return
        activePlaylist = tracks
        val safeIdx = startIndex.coerceIn(0, tracks.lastIndex)
        val items = tracks.map { track ->
            MediaItem.Builder()
                .setMediaId(track.id.toString())
                .setUri(Uri.parse(track.contentUri))
                .setMediaMetadata(
                    MediaMetadata.Builder()
                        .setTitle(track.title)
                        .setArtist(track.artist)
                        .setAlbumTitle(track.album)
                        .setArtworkUri(Uri.parse(track.albumArtUri))
                        .build(),
                )
                .build()
        }
        player.shuffleModeEnabled = shuffle
        player.setMediaItems(items, safeIdx, 0L)
        player.prepare()
        player.play()
        eqBridge.attachAndApply(player.audioSessionId, eqConfig)
        syncStateSnapshot(loadLyricsIfNew = true)
    }

    private fun togglePlayPauseInternal() {
        if (player.isPlaying) {
            player.pause()
        } else {
            if (player.mediaItemCount > 0) {
                player.play()
            }
        }
        syncStateSnapshot()
    }

    private fun skipNextInternal() {
        if (player.hasNextMediaItem()) {
            player.seekToNextMediaItem()
        } else if (player.mediaItemCount > 0) {
            player.seekToDefaultPosition(0)
        }
        syncStateSnapshot(loadLyricsIfNew = true)
    }

    private fun skipPrevInternal() {
        if (player.currentPosition > 3_500L) {
            player.seekTo(0L)
        } else if (player.hasPreviousMediaItem()) {
            player.seekToPreviousMediaItem()
        }
        syncStateSnapshot(loadLyricsIfNew = true)
    }

    private fun seekToInternal(positionMs: Long) {
        player.seekTo(positionMs.coerceAtLeast(0L))
        syncStateSnapshot()
    }

    private fun skipToQueueIdInternal(queueId: Long) {
        val idx = activePlaylist.indexOfFirst { it.id == queueId }
        if (idx >= 0 && idx < player.mediaItemCount) {
            player.seekToDefaultPosition(idx)
            player.play()
            syncStateSnapshot(loadLyricsIfNew = true)
        }
    }

    private fun moveQueueItemInternal(fromIndex: Int, toIndex: Int) {
        if (fromIndex !in activePlaylist.indices || toIndex !in activePlaylist.indices || fromIndex == toIndex) return
        val mutable = activePlaylist.toMutableList()
        val moved = mutable.removeAt(fromIndex)
        mutable.add(toIndex, moved)
        activePlaylist = mutable
        player.moveMediaItem(fromIndex, toIndex)
        syncStateSnapshot()
    }

    private fun toggleShuffleInternal() {
        player.shuffleModeEnabled = !player.shuffleModeEnabled
        syncStateSnapshot()
    }

    private fun toggleRepeatInternal() {
        player.repeatMode = if (player.repeatMode == Player.REPEAT_MODE_ALL) {
            Player.REPEAT_MODE_OFF
        } else {
            Player.REPEAT_MODE_ALL
        }
        syncStateSnapshot()
    }

    private fun toggleMuteInternal() {
        val nextMuted = !_state.value.isMuted
        _state.value = _state.value.copy(isMuted = nextMuted)
        applyPlayerVolume()
    }

    private fun setSleepFadeMultiplierInternal(multiplier: Float) {
        sleepFadeMultiplier = multiplier.coerceIn(0f, 1f)
        applyPlayerVolume()
    }

    private fun stopEngineCleanly() {
        player.stop()
        player.clearMediaItems()
        activePlaylist = emptyList()
        _state.value = HiResPlaybackState()
        eqBridge.release()
        stopSelf()
    }

    private fun applyPlayerVolume() {
        if (_state.value.isMuted) {
            player.volume = 0f
            return
        }
        val crossfadeMs = (eqConfig.crossfadeSeconds.coerceIn(0, 12)) * 1000L
        var crossfadeFactor = 1f
        if (crossfadeMs > 0L && player.isPlaying) {
            val dur = player.duration.coerceAtLeast(0L)
            val pos = player.currentPosition.coerceAtLeast(0L)
            if (dur > crossfadeMs) {
                val remaining = dur - pos
                if (remaining in 1..crossfadeMs) {
                    crossfadeFactor = (remaining.toFloat() / crossfadeMs.toFloat()).coerceIn(0.15f, 1f)
                } else if (pos in 0..crossfadeMs) {
                    crossfadeFactor = (pos.toFloat() / crossfadeMs.toFloat()).coerceIn(0.25f, 1f)
                }
            }
        }
        player.volume = (sleepFadeMultiplier * crossfadeFactor).coerceIn(0f, 1f)
    }

    private fun startPositionTicker() {
        positionJob?.cancel()
        positionJob = scope.launch {
            while (isActive) {
                delay(500L)
                if (_state.value.isActive && player.mediaItemCount > 0) {
                    applyPlayerVolume()
                    val pos = player.currentPosition.coerceAtLeast(0L)
                    val dur = player.duration.coerceAtLeast(_state.value.currentTrack?.durationMs ?: 0L)
                    _state.value = _state.value.copy(
                        positionMs = pos,
                        durationMs = dur,
                        isPlaying = player.isPlaying,
                    )
                }
            }
        }
    }

    private fun syncStateSnapshot(loadLyricsIfNew: Boolean = false) {
        val idx = player.currentMediaItemIndex.coerceIn(0, (activePlaylist.size - 1).coerceAtLeast(0))
        val track = activePlaylist.getOrNull(idx)
        val hasItems = player.mediaItemCount > 0 && track != null
        val badge = if (track != null) {
            val depth = if (eqConfig.float24BitOutput) "${track.bitDepth} bit" else "16 bit"
            "${track.codecLabel()} • $depth / ${track.sampleRateHz / 1000} kHz"
        } else {
            "FLAC • 24 bit / 48 kHz"
        }

        _state.value = _state.value.copy(
            isActive = hasItems,
            isPlaying = player.isPlaying,
            playlist = activePlaylist,
            currentIndex = idx,
            positionMs = player.currentPosition.coerceAtLeast(0L),
            durationMs = player.duration.coerceAtLeast(track?.durationMs ?: 0L),
            shuffleEnabled = player.shuffleModeEnabled,
            repeatEnabled = player.repeatMode != Player.REPEAT_MODE_OFF,
            audioSessionId = player.audioSessionId,
            streamFormatBadge = badge,
        )

        if (loadLyricsIfNew && track != null && track.id != lastCountedTrackId) {
            lastCountedTrackId = track.id
            scope.launch(Dispatchers.IO) {
                libraryRepo.recordLocalTrackPlay(track.id)
                val lyrics = MediaStoreScanner.tryLoadLyricsForTrack(applicationContext, track)
                _state.value = _state.value.copy(currentLyrics = lyrics)
            }
        }
        MediaRepository.rebuildState()
    }

    private val playerListener = object : Player.Listener {
        override fun onIsPlayingChanged(isPlaying: Boolean) {
            syncStateSnapshot()
        }

        override fun onPlaybackStateChanged(playbackState: Int) {
            syncStateSnapshot()
        }

        override fun onMediaItemTransition(mediaItem: MediaItem?, reason: Int) {
            syncStateSnapshot(loadLyricsIfNew = true)
        }

        override fun onAudioSessionIdChanged(audioSessionId: Int) {
            eqBridge.attachAndApply(audioSessionId, eqConfig)
            syncStateSnapshot()
        }
    }

    override fun onDestroy() {
        positionJob?.cancel()
        eqBridge.release()
        player.removeListener(playerListener)
        mediaSession?.release()
        mediaSession = null
        player.release()
        activeInstance = null
        _state.value = HiResPlaybackState()
        scope.cancel()
        super.onDestroy()
    }

    companion object {
        private const val ACTION_PLAY_QUEUE = "com.example.media.ACTION_PLAY_HIRES_QUEUE"
        private const val ACTION_STOP_ENGINE = "com.example.media.ACTION_STOP_HIRES_ENGINE"

        private data class PendingQueueRequest(
            val tracks: List<LocalTrackEntity>,
            val startIndex: Int,
            val shuffle: Boolean,
        )

        private var activeInstance: MagicHiResPlaybackService? = null
        private var pendingQueueRequest: PendingQueueRequest? = null

        private val _state = MutableStateFlow(HiResPlaybackState())
        val state: StateFlow<HiResPlaybackState> = _state.asStateFlow()

        fun playLocalQueue(
            context: android.content.Context,
            tracks: List<LocalTrackEntity>,
            startIndex: Int = 0,
            shuffle: Boolean = false,
        ) {
            if (tracks.isEmpty()) return
            val inst = activeInstance
            if (inst != null) {
                inst.playQueueInternal(tracks, startIndex, shuffle)
                return
            }
            pendingQueueRequest = PendingQueueRequest(tracks, startIndex, shuffle)
            val intent = Intent(context, MagicHiResPlaybackService::class.java).apply {
                action = ACTION_PLAY_QUEUE
            }
            AndroidVersionCompat.startForegroundServiceSafe(context, intent)
        }

        fun playPause() = activeInstance?.togglePlayPauseInternal()
        fun next() = activeInstance?.skipNextInternal()
        fun previous() = activeInstance?.skipPrevInternal()
        fun seekTo(posMs: Long) = activeInstance?.seekToInternal(posMs)
        fun seekBy(deltaMs: Long) {
            val inst = activeInstance ?: return
            inst.seekToInternal(inst.player.currentPosition + deltaMs)
        }

        fun skipToQueueItem(queueId: Long) = activeInstance?.skipToQueueIdInternal(queueId)
        fun moveQueueItem(fromIndex: Int, toIndex: Int) = activeInstance?.moveQueueItemInternal(fromIndex, toIndex)
        fun toggleShuffle() = activeInstance?.toggleShuffleInternal()
        fun toggleRepeat() = activeInstance?.toggleRepeatInternal()
        fun toggleMute() = activeInstance?.toggleMuteInternal()
        fun setSleepFadeMultiplier(multiplier: Float) = activeInstance?.setSleepFadeMultiplierInternal(multiplier)
        fun stop() = activeInstance?.stopEngineCleanly()
    }
}
