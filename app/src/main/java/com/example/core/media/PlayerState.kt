package com.example.core.media

import android.graphics.Bitmap
import android.media.session.PlaybackState
import android.os.SystemClock
import com.example.data.ActiveSessionInfo
import com.example.data.LyricsData
import com.example.data.MusicServiceApp
import com.example.data.QueueTrackItem
import com.example.data.SessionPriorityRule
import com.example.data.UnifiedMediaState

enum class ListenerConnectionState {
    CONNECTED,
    DISCONNECTED_REBINDING,
    PERMISSION_MISSING,
}

data class SessionCustomAction(
    val action: String,
    val name: String,
    val iconRes: Int,
)

data class PlaybackCapabilities(
    val canPlayPause: Boolean = false,
    val canSkipNext: Boolean = false,
    val canSkipPrevious: Boolean = false,
    val canSeek: Boolean = false,
    val canSetSpeed: Boolean = false,
    val canStop: Boolean = false,
    val customActions: List<SessionCustomAction> = emptyList(),
) {
    companion object {
        fun fromPlaybackState(pb: PlaybackState?): PlaybackCapabilities {
            if (pb == null) {
                return PlaybackCapabilities(canPlayPause = true)
            }
            val actions = pb.actions
            val canPlayOrPause = (
                actions and (
                    PlaybackState.ACTION_PLAY or
                        PlaybackState.ACTION_PAUSE or
                        PlaybackState.ACTION_PLAY_PAUSE
                    )
                ) != 0L
            val customs = pb.customActions.orEmpty().map {
                SessionCustomAction(
                    action = it.action,
                    name = it.name?.toString().orEmpty(),
                    iconRes = it.icon,
                )
            }
            return PlaybackCapabilities(
                canPlayPause = canPlayOrPause,
                canSkipNext = (actions and PlaybackState.ACTION_SKIP_TO_NEXT) != 0L,
                canSkipPrevious = (actions and PlaybackState.ACTION_SKIP_TO_PREVIOUS) != 0L,
                canSeek = (actions and PlaybackState.ACTION_SEEK_TO) != 0L,
                canSetSpeed = (actions and PlaybackState.ACTION_SET_PLAYBACK_SPEED) != 0L,
                canStop = (actions and PlaybackState.ACTION_STOP) != 0L,
                customActions = customs,
            )
        }
    }
}

data class SessionSnapshot(
    val packageName: String,
    val appName: String,
    val service: MusicServiceApp,
    val title: String,
    val artist: String,
    val album: String,
    val isPlaying: Boolean,
    val playbackStateCode: Int,
    val positionMs: Long,
    val lastPositionUpdateElapsedMs: Long,
    val playbackSpeed: Float,
    val durationMs: Long,
    val albumArt: Bitmap?,
    val albumArtUri: String?,
    val capabilities: PlaybackCapabilities,
    val isLocalEngine: Boolean,
    val isSelected: Boolean,
    val lastActiveTimestampMs: Long,
    val canFavorite: Boolean = false,
    val isFavorite: Boolean = false,
    val canShuffle: Boolean = false,
    val isShuffleEnabled: Boolean = false,
    val canRepeat: Boolean = false,
    val isRepeatEnabled: Boolean = false,
    val queue: List<QueueTrackItem> = emptyList(),
    val lyrics: LyricsData? = null,
    val streamFormatBadge: String = "Hi-Res Local",
    val localTrackId: Long? = null,
) {
    fun computeInterpolatedPosition(nowElapsedMs: Long = SystemClock.elapsedRealtime()): Long {
        if (!isPlaying || lastPositionUpdateElapsedMs <= 0L) {
            return positionMs.coerceAtLeast(0L)
        }
        val delta = ((nowElapsedMs - lastPositionUpdateElapsedMs).coerceAtLeast(0L) * playbackSpeed.coerceAtLeast(0.5f)).toLong()
        val raw = positionMs + delta
        return if (durationMs > 0L) raw.coerceIn(0L, durationMs) else raw.coerceAtLeast(0L)
    }
}

data class PlayerState(
    val activeSession: SessionSnapshot? = null,
    val sessions: List<SessionSnapshot> = emptyList(),
    val interpolatedPositionMs: Long = 0L,
    val volumePercent: Int = 50,
    val isMuted: Boolean = false,
    val listenerState: ListenerConnectionState = ListenerConnectionState.PERMISSION_MISSING,
    val priorityRule: SessionPriorityRule = SessionPriorityRule.PLAYING_FIRST,
    val ignoreVideoSessions: Boolean = false,
    val lastError: String? = null,
) {
    fun toUnifiedMediaState(): UnifiedMediaState {
        val active = activeSession
        val sessionInfos = sessions.map { snap ->
            ActiveSessionInfo(
                packageName = snap.packageName,
                service = snap.service,
                title = snap.title,
                artist = snap.artist,
                album = snap.album,
                isPlaying = snap.isPlaying,
                isSelected = snap.isSelected,
                appLabel = snap.appName,
                isLocalEngine = snap.isLocalEngine,
            )
        }
        return if (active != null) {
            UnifiedMediaState(
                title = active.title,
                artist = active.artist,
                album = active.album,
                packageName = active.packageName,
                service = active.service,
                isPlaying = active.isPlaying,
                positionMs = interpolatedPositionMs.coerceAtLeast(0L),
                durationMs = active.durationMs,
                albumArt = active.albumArt,
                albumArtUri = active.albumArtUri,
                isLocalEngine = active.isLocalEngine,
                activeSessions = sessionInfos,
                lastUpdatedMs = active.lastActiveTimestampMs,
                canPlayPause = active.capabilities.canPlayPause,
                canSkipNext = active.capabilities.canSkipNext,
                canSkipPrevious = active.capabilities.canSkipPrevious,
                canSeek = active.capabilities.canSeek,
                canFavorite = active.canFavorite,
                isFavorite = active.isFavorite,
                canShuffle = active.canShuffle,
                isShuffleEnabled = active.isShuffleEnabled,
                canRepeat = active.canRepeat,
                isRepeatEnabled = active.isRepeatEnabled,
                isMuted = isMuted,
                streamFormatBadge = active.streamFormatBadge,
                queue = active.queue,
                lyrics = active.lyrics,
                localTrackId = active.localTrackId,
            )
        } else {
            UnifiedMediaState(activeSessions = sessionInfos, isMuted = isMuted)
        }
    }
}

object SessionSelector {
    fun selectBestSession(
        sessions: List<SessionSnapshot>,
        manualSelectedPackage: String?,
        prefs: com.example.data.MediaSessionPreferences,
    ): SessionSnapshot? {
        if (sessions.isEmpty()) return null

        // Local engine takes precedence while actively playing
        sessions.firstOrNull { it.isLocalEngine && it.isPlaying }?.let { return it }

        // Respect manual user pick if still present
        if (!manualSelectedPackage.isNullOrBlank()) {
            sessions.firstOrNull { it.packageName == manualSelectedPackage }?.let { return it }
        }

        return when (prefs.priorityRule) {
            SessionPriorityRule.PLAYING_FIRST -> {
                sessions.firstOrNull { it.isPlaying }
                    ?: sessions.maxByOrNull { it.lastActiveTimestampMs }
            }
            SessionPriorityRule.LAST_ACTIVE -> {
                sessions.maxByOrNull { it.lastActiveTimestampMs }
                    ?: sessions.firstOrNull { it.isPlaying }
            }
            SessionPriorityRule.PINNED_PREFERRED -> {
                sessions.firstOrNull { it.packageName == prefs.preferredServicePackage }
                    ?: sessions.firstOrNull { it.isPlaying }
                    ?: sessions.maxByOrNull { it.lastActiveTimestampMs }
            }
        }
    }
}
