package com.example.core.media

import android.media.audiofx.BassBoost
import android.media.audiofx.Equalizer
import android.media.audiofx.LoudnessEnhancer
import com.example.data.EqualizerConfig
import kotlin.math.roundToInt

/**
 * Attaches Android's native DSP Equalizer, BassBoost, and LoudnessEnhancer to an
 * audioSessionId whenever available, degrading gracefully on devices without DSP support.
 */
class LocalAudioEqualizerBridge {

    private var equalizer: Equalizer? = null
    private var bassBoost: BassBoost? = null
    private var loudnessEnhancer: LoudnessEnhancer? = null
    private var attachedSessionId: Int = 0
    private var lastConfig: EqualizerConfig = EqualizerConfig()

    fun attachAndApply(audioSessionId: Int, config: EqualizerConfig) {
        lastConfig = config
        if (audioSessionId <= 0) return

        if (attachedSessionId != audioSessionId || equalizer == null) {
            release()
            equalizer = runCatching {
                Equalizer(0, audioSessionId).apply { enabled = config.enabled }
            }.getOrNull()
            bassBoost = runCatching {
                BassBoost(0, audioSessionId).apply { enabled = config.enabled && config.bassBoostStrength > 0 }
            }.getOrNull()
            loudnessEnhancer = runCatching {
                LoudnessEnhancer(audioSessionId).apply { enabled = config.enabled && config.loudnessGainMb > 0 }
            }.getOrNull()
            attachedSessionId = if (equalizer != null || bassBoost != null || loudnessEnhancer != null) {
                audioSessionId
            } else {
                0
            }
        }

        equalizer?.let { eq ->
            runCatching {
                eq.enabled = config.enabled
                val numBands = eq.numberOfBands.toInt().coerceAtLeast(0)
                val range = eq.bandLevelRange
                val minMb = range.getOrNull(0)?.toInt() ?: -1200
                val maxMb = range.getOrNull(1)?.toInt() ?: 1200
                for (bandIdx in 0 until numBands) {
                    val gainDb = config.bandGainsDb.getOrElse(bandIdx) { 0f }
                    val targetMb = (gainDb * 100f).roundToInt().coerceIn(minMb, maxMb).toShort()
                    eq.setBandLevel(bandIdx.toShort(), targetMb)
                }
            }
        }

        bassBoost?.let { bb ->
            runCatching {
                val active = config.enabled && config.bassBoostStrength > 0
                bb.enabled = active
                if (bb.strengthSupported) {
                    bb.setStrength(config.bassBoostStrength.coerceIn(0, 1000).toShort())
                }
            }
        }

        loudnessEnhancer?.let { le ->
            runCatching {
                val replayGainBonusMb = if (config.replayGainEnabled && config.loudnessGainMb == 0) 150 else 0
                val totalGain = (config.loudnessGainMb + replayGainBonusMb).coerceIn(0, 1000)
                le.enabled = config.enabled && totalGain > 0
                le.setTargetGain(totalGain)
            }
        }
    }

    fun applyConfig(config: EqualizerConfig) {
        lastConfig = config
        if (attachedSessionId > 0) {
            attachAndApply(attachedSessionId, config)
        }
    }

    fun release() {
        runCatching { equalizer?.release() }
        runCatching { bassBoost?.release() }
        runCatching { loudnessEnhancer?.release() }
        equalizer = null
        bassBoost = null
        loudnessEnhancer = null
        attachedSessionId = 0
    }
}
