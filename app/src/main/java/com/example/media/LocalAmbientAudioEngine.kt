package com.example.media

import android.media.AudioAttributes
import android.media.AudioFormat
import android.media.AudioTrack
import com.example.core.media.LocalAudioEqualizerBridge
import com.example.data.EqualizerConfig
import com.example.data.LyricsData
import com.example.data.LyricsLine
import com.example.data.QueueTrackItem
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
import kotlin.random.Random

data class LocalAudioTrack(
    val id: Long,
    val title: String,
    val artist: String,
    val album: String,
    val baseFreqHz: Double,
    val durationMs: Long,
    val lyrics: List<LyricsLine>,
)

data class LocalAudioState(
    val isActive: Boolean = false,
    val isPlaying: Boolean = false,
    val trackIndex: Int = 0,
    val positionMs: Long = 0L,
    val isMuted: Boolean = false,
    val shuffleEnabled: Boolean = false,
    val repeatEnabled: Boolean = false,
    val favoriteTrackIds: Set<Long> = emptySet(),
    val playlist: List<LocalAudioTrack> = defaultTracks,
    val fadeVolumeMultiplier: Float = 1f,
) {
    val currentTrack: LocalAudioTrack
        get() = playlist[trackIndex.mod(playlist.size.coerceAtLeast(1))]

    val isCurrentFavorite: Boolean
        get() = favoriteTrackIds.contains(currentTrack.id)

    fun toQueueItems(): List<QueueTrackItem> = playlist.mapIndexed { idx, track ->
        QueueTrackItem(
            queueId = track.id,
            title = track.title,
            artist = track.artist,
            durationMs = track.durationMs,
            isCurrent = idx == trackIndex,
        )
    }

    fun toLyricsData(): LyricsData = LyricsData(
        lines = currentTrack.lyrics,
        isSynced = true,
        sourceNote = "Motor Local Hi-Res • Sincronización local",
    )

    companion object {
        val defaultTracks = listOf(
            LocalAudioTrack(
                id = 1001L,
                title = "Resonancia Nocturna",
                artist = "Estudio Acústico",
                album = "Sesión Hi-Res 24-bit",
                baseFreqHz = 220.0,
                durationMs = 180_000L,
                lyrics = listOf(
                    LyricsLine(0L, "En la inmensidad de la noche calma,"),
                    LyricsLine(15_000L, "suben los armónicos en baja latencia."),
                    LyricsLine(35_000L, "Sigue el pulso lento sobre la marea,"),
                    LyricsLine(60_000L, "cruzando el silencio con luz violeta."),
                    LyricsLine(95_000L, "Cada frecuencia respira sin prisa,"),
                    LyricsLine(130_000L, "mientras el estudio descansa en calma."),
                ),
            ),
            LocalAudioTrack(
                id = 1002L,
                title = "Horizonte Violeta",
                artist = "Armónicos Ambientales",
                album = "Studio Master",
                baseFreqHz = 261.63,
                durationMs = 210_000L,
                lyrics = listOf(
                    LyricsLine(0L, "Cuando cae la tarde frente al océano,"),
                    LyricsLine(18_000L, "un acorde limpio abre el horizonte."),
                    LyricsLine(42_000L, "Brilla cada nota como vidrio cálido,"),
                    LyricsLine(78_000L, "envolviendo el cuarto en ondas tonales."),
                    LyricsLine(118_000L, "Sin ruido ni prisa, solo claridad,"),
                    LyricsLine(156_000L, "hasta que el eco vuelve al origen."),
                ),
            ),
            LocalAudioTrack(
                id = 1003L,
                title = "Pulso Mineral",
                artist = "Laboratorio Sonoro",
                album = "Pure Spatial Mix",
                baseFreqHz = 196.0,
                durationMs = 195_000L,
                lyrics = listOf(
                    LyricsLine(0L, "Graves profundos bajo piedra y viento,"),
                    LyricsLine(20_000L, "marcan el compás del estudio abierto."),
                    LyricsLine(48_000L, "Sube la textura con precisión firme,"),
                    LyricsLine(84_000L, "resonando estable en cada rincón."),
                    LyricsLine(125_000L, "Se apagan las luces del panel frontal,"),
                    LyricsLine(160_000L, "queda el pulso puro hasta descansar."),
                ),
            ),
        )
    }
}

class LocalAmbientAudioEngine {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    private val eqBridge = LocalAudioEqualizerBridge()
    private var eqConfig = EqualizerConfig()
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
        eqBridge.release()
        _state.value = LocalAudioState(
            playlist = _state.value.playlist,
            favoriteTrackIds = _state.value.favoriteTrackIds,
        )
    }

    fun nextTrack() {
        val cur = _state.value
        val size = cur.playlist.size.coerceAtLeast(1)
        val nextIdx = when {
            cur.repeatEnabled -> cur.trackIndex
            cur.shuffleEnabled && size > 1 -> {
                var pick = Random.nextInt(size)
                if (pick == cur.trackIndex) pick = (cur.trackIndex + 1) % size
                pick
            }
            else -> (cur.trackIndex + 1) % size
        }
        val wasPlaying = cur.isPlaying
        _state.value = cur.copy(
            isActive = true,
            trackIndex = nextIdx,
            positionMs = 0L,
        )
        if (wasPlaying) {
            startAudioLoop()
        }
    }

    fun previousTrack() {
        val cur = _state.value
        val prevIdx = if (cur.trackIndex - 1 < 0) {
            cur.playlist.size - 1
        } else {
            cur.trackIndex - 1
        }
        val wasPlaying = cur.isPlaying
        _state.value = cur.copy(
            isActive = true,
            trackIndex = prevIdx,
            positionMs = 0L,
        )
        if (wasPlaying) {
            startAudioLoop()
        }
    }

    fun playTrackByQueueId(queueId: Long) {
        val cur = _state.value
        val idx = cur.playlist.indexOfFirst { it.id == queueId }
        if (idx < 0) return
        _state.value = cur.copy(
            isActive = true,
            isPlaying = true,
            trackIndex = idx,
            positionMs = 0L,
        )
        startAudioLoop()
        startTicker()
    }

    fun moveQueueItem(fromIndex: Int, toIndex: Int) {
        val cur = _state.value
        val list = cur.playlist.toMutableList()
        if (fromIndex !in list.indices || toIndex !in list.indices || fromIndex == toIndex) return
        val currentId = cur.currentTrack.id
        val moved = list.removeAt(fromIndex)
        list.add(toIndex, moved)
        val newCurrentIdx = list.indexOfFirst { it.id == currentId }.coerceAtLeast(0)
        _state.value = cur.copy(
            playlist = list,
            trackIndex = newCurrentIdx,
        )
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

    fun toggleShuffle() {
        _state.value = _state.value.copy(shuffleEnabled = !_state.value.shuffleEnabled)
    }

    fun toggleRepeat() {
        _state.value = _state.value.copy(repeatEnabled = !_state.value.repeatEnabled)
    }

    fun toggleFavoriteCurrent(): Boolean {
        val cur = _state.value
        val id = cur.currentTrack.id
        val nextFavs = cur.favoriteTrackIds.toMutableSet()
        val added = if (nextFavs.contains(id)) {
            nextFavs.remove(id)
            false
        } else {
            nextFavs.add(id)
            true
        }
        _state.value = cur.copy(favoriteTrackIds = nextFavs)
        return added
    }

    fun setVolumeMultiplier(multiplier: Float) {
        _state.value = _state.value.copy(fadeVolumeMultiplier = multiplier.coerceIn(0f, 1f))
    }

    fun applyEqualizerConfig(config: EqualizerConfig) {
        eqConfig = config
        eqBridge.applyConfig(config)
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
            val bufferFrames = if (eqConfig.hiResBufferHigh) 2048 else 1024
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

            eqBridge.attachAndApply(track.audioSessionId, eqConfig)

            val buffer = ShortArray(bufferFrames)
            var sampleCursor = 0L

            try {
                track.play()
                while (isActive && _state.value.isPlaying) {
                    val snap = _state.value
                    val baseFreq = snap.currentTrack.baseFreqHz
                    val muted = snap.isMuted
                    val amplitude = if (muted) 0.0 else 1600.0 * snap.fadeVolumeMultiplier
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
                eqBridge.release()
                runCatching {
                    track.pause()
                    track.flush()
                    track.release()
                }
            }
        }
    }
}
