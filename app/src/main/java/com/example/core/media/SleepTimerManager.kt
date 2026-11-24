package com.example.core.media

import androidx.compose.runtime.Immutable
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

enum class SleepTimerMode {
    OFF,
    COUNTDOWN,
    END_OF_TRACK,
}

@Immutable
data class SleepTimerState(
    val mode: SleepTimerMode = SleepTimerMode.OFF,
    val selectedMinutes: Int = 0,
    val remainingMs: Long = 0L,
    val fadeOutEnabled: Boolean = true,
    val isFading: Boolean = false,
    val trackKeyAtArm: String = "",
) {
    val isActive: Boolean
        get() = mode != SleepTimerMode.OFF
}

class SleepTimerManager(
    private val scope: CoroutineScope,
    private val onApplyFadeVolumeFraction: (Float) -> Unit,
    private val onTimerTriggeredPause: () -> Unit,
) {
    private var countdownJob: Job? = null
    private val _state = MutableStateFlow(SleepTimerState())
    val state: StateFlow<SleepTimerState> = _state.asStateFlow()

    fun startCountdown(minutes: Int, fadeOutEnabled: Boolean) {
        if (minutes <= 0) {
            cancel()
            return
        }
        countdownJob?.cancel()
        val totalMs = minutes * 60_000L
        _state.value = SleepTimerState(
            mode = SleepTimerMode.COUNTDOWN,
            selectedMinutes = minutes,
            remainingMs = totalMs,
            fadeOutEnabled = fadeOutEnabled,
            isFading = false,
        )
        countdownJob = scope.launch {
            var remaining = totalMs
            while (isActive && remaining > 0L) {
                delay(1_000L)
                remaining = (remaining - 1_000L).coerceAtLeast(0L)
                val cur = _state.value
                if (cur.mode != SleepTimerMode.COUNTDOWN) break

                val fadeMult = if (cur.fadeOutEnabled) {
                    computeFadeVolumeMultiplier(remaining, FADE_WINDOW_MS)
                } else {
                    1f
                }
                val fading = cur.fadeOutEnabled && remaining in 1L..FADE_WINDOW_MS
                if (fading) {
                    onApplyFadeVolumeFraction(fadeMult)
                }
                _state.value = cur.copy(
                    remainingMs = remaining,
                    isFading = fading,
                )
            }
            if (_state.value.mode == SleepTimerMode.COUNTDOWN && _state.value.remainingMs <= 0L) {
                onTimerTriggeredPause()
                onApplyFadeVolumeFraction(1f)
                _state.value = SleepTimerState(fadeOutEnabled = _state.value.fadeOutEnabled)
            }
        }
    }

    fun startEndOfTrack(currentTrackKey: String, fadeOutEnabled: Boolean) {
        countdownJob?.cancel()
        _state.value = SleepTimerState(
            mode = SleepTimerMode.END_OF_TRACK,
            selectedMinutes = 0,
            remainingMs = 0L,
            fadeOutEnabled = fadeOutEnabled,
            trackKeyAtArm = currentTrackKey,
        )
    }

    fun setFadeOutEnabled(enabled: Boolean) {
        _state.value = _state.value.copy(fadeOutEnabled = enabled)
    }

    fun onTrackProgressOrChange(
        currentTrackKey: String,
        positionMs: Long,
        durationMs: Long,
        isPlaying: Boolean,
    ) {
        val cur = _state.value
        if (cur.mode != SleepTimerMode.END_OF_TRACK || !isPlaying) return

        val remainingInTrack = if (durationMs > 0L) (durationMs - positionMs).coerceAtLeast(0L) else Long.MAX_VALUE
        if (cur.fadeOutEnabled && remainingInTrack in 1L..FADE_WINDOW_MS) {
            onApplyFadeVolumeFraction(computeFadeVolumeMultiplier(remainingInTrack, FADE_WINDOW_MS))
        }

        if (shouldTriggerOnTrackChange(cur, currentTrackKey, positionMs, durationMs)) {
            onTimerTriggeredPause()
            onApplyFadeVolumeFraction(1f)
            _state.value = SleepTimerState(fadeOutEnabled = cur.fadeOutEnabled)
        }
    }

    fun cancel() {
        countdownJob?.cancel()
        countdownJob = null
        val fade = _state.value.fadeOutEnabled
        onApplyFadeVolumeFraction(1f)
        _state.value = SleepTimerState(fadeOutEnabled = fade)
    }

    companion object {
        const val FADE_WINDOW_MS = 10_000L

        fun computeFadeVolumeMultiplier(remainingMs: Long, fadeWindowMs: Long = FADE_WINDOW_MS): Float {
            if (fadeWindowMs <= 0L || remainingMs >= fadeWindowMs) return 1f
            if (remainingMs <= 0L) return 0f
            return (remainingMs.toFloat() / fadeWindowMs.toFloat()).coerceIn(0f, 1f)
        }

        fun shouldTriggerOnTrackChange(
            state: SleepTimerState,
            currentTrackKey: String,
            positionMs: Long,
            durationMs: Long,
        ): Boolean {
            if (state.mode != SleepTimerMode.END_OF_TRACK) return false
            if (state.trackKeyAtArm.isNotBlank() && currentTrackKey.isNotBlank() && currentTrackKey != state.trackKeyAtArm) {
                return true
            }
            return durationMs > 0L && (durationMs - positionMs) <= 600L
        }

        fun formatRemaining(remainingMs: Long): String {
            val totalSeconds = (remainingMs / 1000L).coerceAtLeast(0L)
            val hours = totalSeconds / 3600L
            val minutes = (totalSeconds % 3600L) / 60L
            val seconds = totalSeconds % 60L
            return if (hours > 0L) {
                "%d:%02d:%02d".format(hours, minutes, seconds)
            } else {
                "%02d:%02d".format(minutes, seconds)
            }
        }
    }
}
