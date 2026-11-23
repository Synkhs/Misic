package com.example.media

import android.media.AudioAttributes
import android.media.AudioFormat
import android.media.AudioTrack
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
import kotlin.math.PI
import kotlin.math.sin

data class LocalAudioTrack(
    val title: String,
    val artist: String,
    val album: String,
    val baseFreqHz: Double,
    val durationMs: Long,
)

data class LocalAudioState(
    val isActive: Boolean = false,
    val isPlaying: Boolean = false,
    val trackIndex: Int = 0,
    val positionMs: Long = 0L,
    val isMuted: Boolean = false,
) {
    val currentTrack: LocalAudioTrack
        get() = tracks[trackIndex.mod(tracks.size)]

    companion object {
        val tracks = listOf(
            LocalAudioTrack(
                title = "MagicOS Spatial Resonance",
                artist = "Honor Acoustic Lab",
                album = "MagicOS 10 Reference",
                baseFreqHz = 220.0,
                durationMs = 180_000L,
            ),
            LocalAudioTrack(
                title = "Obsidian Glass Horizon",
                artist = "Sapphire Harmonics",
                album = "Android 16 Studio",
                baseFreqHz = 261.63,
                durationMs = 210_000L,
            ),
            LocalAudioTrack(
                title = "Cyan Pulse Ambient",
                artist = "Magic Capsule Audio",
                album = "Pure Spatial Mix",
                baseFreqHz = 196.0,
                durationMs = 195_000L,
            ),
        )
    }
}

class LocalAmbientAudioEngine {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    private var playJob: Job? = null
    private var tickerJob: Job? = null

    private val _state = MutableStateFlow(LocalAudioState())
    val state: StateFlow<LocalAudioState> = _state.asStateFlow()

    fun startOrResume() {
        val current = _state.value
        _state.value = current.copy(isActive = true, isPlaying = true)
        startAudioLoop()
        startTicker()
    }

    fun pause() {
        _state.value = _state.value.copy(isPlaying = false)
        playJob?.cancel()
        playJob = null
        tickerJob?.cancel()
        tickerJob = null
    }

    fun stop() {
        playJob?.cancel()
        tickerJob?.cancel()
        playJob = null
        tickerJob = null
        _state.value = LocalAudioState()
    }

    fun nextTrack() {
        val nextIdx = (_state.value.trackIndex + 1) % LocalAudioState.tracks.size
        val wasPlaying = _state.value.isPlaying
        _state.value = _state.value.copy(
            isActive = true,
            trackIndex = nextIdx,
            positionMs = 0L,
        )
        if (wasPlaying) {
            startAudioLoop()
        }
    }

    fun previousTrack() {
        val prevIdx = if (_state.value.trackIndex - 1 < 0) {
            LocalAudioState.tracks.size - 1
        } else {
            _state.value.trackIndex - 1
        }
        val wasPlaying = _state.value.isPlaying
        _state.value = _state.value.copy(
            isActive = true,
            trackIndex = prevIdx,
            positionMs = 0L,
        )
        if (wasPlaying) {
            startAudioLoop()
        }
    }

    fun seekTo(positionMs: Long) {
        val max = _state.value.currentTrack.durationMs
        _state.value = _state.value.copy(
            isActive = true,
            positionMs = positionMs.coerceIn(0L, max),
        )
    }

    fun seekBy(deltaMs: Long) {
        seekTo(_state.value.positionMs + deltaMs)
    }

    fun toggleMute() {
        _state.value = _state.value.copy(isMuted = !_state.value.isMuted)
    }

    private fun startTicker() {
        tickerJob?.cancel()
        tickerJob = scope.launch {
            while (isActive && _state.value.isPlaying) {
                delay(500L)
                val cur = _state.value
                val nextPos = cur.positionMs + 500L
                if (nextPos >= cur.currentTrack.durationMs) {
                    nextTrack()
                } else {
                    _state.value = cur.copy(positionMs = nextPos)
                }
            }
        }
    }

    private fun startAudioLoop() {
        playJob?.cancel()
        playJob = scope.launch(Dispatchers.IO) {
            val sampleRate = 22050
            val bufferFrames = 2048
            val minBufBytes = AudioTrack.getMinBufferSize(
                sampleRate,
                AudioFormat.CHANNEL_OUT_MONO,
                AudioFormat.ENCODING_PCM_16BIT,
            ).coerceAtLeast(bufferFrames * 2)

            val track = runCatching {
                AudioTrack.Builder()
                    .setAudioAttributes(
                        AudioAttributes.Builder()
                            .setUsage(AudioAttributes.USAGE_MEDIA)
                            .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
                            .build(),
                    )
                    .setAudioFormat(
                        AudioFormat.Builder()
                            .setEncoding(AudioFormat.ENCODING_PCM_16BIT)
                            .setSampleRate(sampleRate)
                            .setChannelMask(AudioFormat.CHANNEL_OUT_MONO)
                            .build(),
                    )
                    .setBufferSizeInBytes(minBufBytes)
                    .setTransferMode(AudioTrack.MODE_STREAM)
                    .build()
            }.getOrNull() ?: return@launch

            val buffer = ShortArray(bufferFrames)
            var sampleCursor = 0L

            try {
                track.play()
                while (isActive && _state.value.isPlaying) {
                    val baseFreq = _state.value.currentTrack.baseFreqHz
                    val muted = _state.value.isMuted
                    val amplitude = if (muted) 0.0 else 1600.0
                    for (i in buffer.indices) {
                        val t = (sampleCursor + i).toDouble() / sampleRate
                        val envelope = 0.75 + 0.25 * sin(2.0 * PI * 0.2 * t)
                        val wave = (
                            sin(2.0 * PI * baseFreq * t) * 0.55 +
                                sin(2.0 * PI * (baseFreq * 1.5) * t) * 0.30 +
                                sin(2.0 * PI * (baseFreq * 2.0) * t) * 0.15
                            ) * amplitude * envelope
                        buffer[i] = wave.toInt().coerceIn(Short.MIN_VALUE.toInt(), Short.MAX_VALUE.toInt()).toShort()
                    }
                    sampleCursor += bufferFrames
                    track.write(buffer, 0, buffer.size)
                }
            } catch (_: Throwable) {
                // AudioTrack gracefully stopped
            } finally {
                runCatching {
                    track.pause()
                    track.flush()
                    track.release()
                }
            }
        }
    }
}
