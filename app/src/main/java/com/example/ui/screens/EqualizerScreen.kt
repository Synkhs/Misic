package com.example.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.BookmarkAdd
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.GraphicEq
import androidx.compose.material.icons.rounded.SpeakerGroup
import androidx.compose.material.icons.rounded.Tune
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.core.media.DspCapabilityReport
import com.example.data.AudioOutputCategory
import com.example.data.AudioOutputDevice
import com.example.data.CustomEqPreset
import com.example.designsystem.MagicTheme
import com.example.designsystem.expressive.MagicConnectedButtonGroup
import com.example.designsystem.expressive.MagicFlexibleMediumTopBar
import com.example.ui.MagicMusicUiState
import com.example.ui.components.EqualizerBand
import com.example.ui.components.EqualizerPreset
import com.example.ui.components.MagicEqualizerPanel
import com.example.ui.components.MagicSettingsSwitchRow
import com.example.ui.components.MagicSurfaceCard
import kotlin.math.roundToInt

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun EqualizerScreen(
    state: MagicMusicUiState,
    customPresets: List<CustomEqPreset>,
    dspReport: DspCapabilityReport,
    onToggleEqEnabled: (Boolean) -> Unit,
    onSelectOutputCategory: (AudioOutputCategory) -> Unit,
    onBandChange: (Int, Float) -> Unit,
    onSelectPreset: (Int, EqualizerPreset) -> Unit,
    onModeChange: (Boolean) -> Unit,
    onSaveCustomPreset: (String) -> Unit,
    onApplyCustomPreset: (CustomEqPreset) -> Unit,
    onDeleteCustomPreset: (String) -> Unit,
    onBassBoostChange: (Int) -> Unit,
    onVirtualizerChange: (Int) -> Unit,
    onLoudnessChange: (Int) -> Unit,
    onPreampChange: (Float) -> Unit,
    onStereoBalanceChange: (Float) -> Unit,
    onGaplessChange: (Boolean) -> Unit,
    onReplayGainChange: (Boolean) -> Unit,
    onFloat24BitChange: (Boolean) -> Unit,
    onHiResBufferChange: (Boolean) -> Unit,
    onCrossfadeChange: (Int) -> Unit,
    onRefreshOutputs: () -> Unit,
    onSelectOutput: (AudioOutputDevice) -> Unit,
) {
    val tokens = MagicTheme.tokens
    val colors = MaterialTheme.colorScheme
    val eqCfg = state.equalizerConfig
    val presets = EqualizerPreset.entries
    val activePreset = presets.getOrElse(eqCfg.presetIndex) { EqualizerPreset.FLAT }
    val freqs = remember(dspReport.bandCenterFreqHz) {
        val hz = dspReport.bandCenterFreqHz
        if (hz.size >= 5) {
            hz.take(5).map { freq -> if (freq >= 1000) "${(freq / 1000f).let { if (it % 1f == 0f) it.toInt().toString() else "%.1f".format(it) }}kHz" else "${freq}Hz" }
        } else {
            listOf("60Hz", "230Hz", "910Hz", "3.6kHz", "14kHz")
        }
    }
    val bands = remember(eqCfg.bandGainsDb, freqs) {
        freqs.mapIndexed { idx, label ->
            EqualizerBand(freqLabel = label, gainDb = eqCfg.bandGainsDb.getOrElse(idx) { 0f })
        }
    }

    var showSavePresetDialog by remember { mutableStateOf(false) }
    var presetNameDraft by remember { mutableStateOf("") }
    val scrollBehavior = TopAppBarDefaults.exitUntilCollapsedScrollBehavior()

    Scaffold(
        modifier = Modifier
            .fillMaxSize()
            .nestedScroll(scrollBehavior.nestedScrollConnection),
        containerColor = Color.Transparent,
        topBar = {
            MagicFlexibleMediumTopBar(
                title = "Ecualizador",
                subtitle = "Perfil activo: ${eqCfg.activeOutputCategory.label} • ${if (eqCfg.enabled) "DSP Activo" else "Bypass"}",
                useExpressiveM3 = tokens.useExpressiveM3,
                scrollBehavior = scrollBehavior,
            )
        },
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .testTag("equalizer_screen_list"),
            contentPadding = PaddingValues(
                start = tokens.spacing.screenMargin,
                end = tokens.spacing.screenMargin,
                top = tokens.spacing.xs,
                bottom = 96.dp,
            ),
            verticalArrangement = Arrangement.spacedBy(tokens.spacing.md),
        ) {
            // Master Switch + Per-Output Profile Selector (Altavoz / Cable / Bluetooth)
            item {
                MagicSurfaceCard {
                    MagicSettingsSwitchRow(
                        title = "Procesamiento DSP en Motor Local",
                        subtitle = if (dspReport.equalizerAttached) {
                            "Ecualizador Android adjunto en tiempo real a AudioSession"
                        } else {
                            "Los perfiles quedan guardados y se aplican al iniciar el Motor Local"
                        },
                        checked = eqCfg.enabled,
                        onCheckedChange = onToggleEqEnabled,
                        switchTestTag = "eq_enabled_switch",
                    )
                    Spacer(Modifier.height(10.dp))
                    Text(
                        text = "Perfil automático por salida",
                        style = MaterialTheme.typography.labelLarge,
                        color = colors.onSurfaceVariant,
                    )
                    Spacer(Modifier.height(6.dp))
                    val categories = AudioOutputCategory.entries
                    MagicConnectedButtonGroup(
                        options = categories,
                        selectedIndex = categories.indexOf(eqCfg.activeOutputCategory).coerceAtLeast(0),
                        labelProvider = { it.label },
                        useExpressiveM3 = tokens.useExpressiveM3,
                        onSelect = { _, cat -> onSelectOutputCategory(cat) },
                        itemTestTagPrefix = "eq_output_cat",
                    )
                }
            }

            // 5-band Equalizer Panel + Save custom preset
            item {
                MagicEqualizerPanel(
                    bands = bands,
                    selectedPreset = activePreset,
                    isAdvancedMode = eqCfg.isAdvancedMode,
                    onBandChange = onBandChange,
                    onSelectPreset = { preset -> onSelectPreset(presets.indexOf(preset), preset) },
                    onModeChange = onModeChange,
                )
            }

            item {
                MagicSurfaceCard {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Rounded.Tune, contentDescription = null, tint = colors.primary)
                            Spacer(Modifier.width(8.dp))
                            Text(
                                text = "Curvas guardadas",
                                style = MaterialTheme.typography.titleMedium,
                                color = colors.onSurface,
                            )
                        }
                        FilledTonalButton(
                            onClick = {
                                presetNameDraft = ""
                                showSavePresetDialog = true
                            },
                            modifier = Modifier.testTag("eq_save_custom_btn"),
                        ) {
                            Icon(Icons.Rounded.BookmarkAdd, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(Modifier.width(6.dp))
                            Text("Guardar curva")
                        }
                    }
                    if (customPresets.isNotEmpty()) {
                        Spacer(Modifier.height(10.dp))
                        FlowRow(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp),
                        ) {
                            customPresets.forEach { custom ->
                                val selected = eqCfg.presetIndex == -1 && eqCfg.customPresetName == custom.name
                                FilterChip(
                                    selected = selected,
                                    onClick = { onApplyCustomPreset(custom) },
                                    label = { Text(custom.name) },
                                    trailingIcon = {
                                        IconButton(
                                            onClick = { onDeleteCustomPreset(custom.id) },
                                            modifier = Modifier.size(18.dp),
                                        ) {
                                            Icon(Icons.Rounded.Close, contentDescription = "Borrar ${custom.name}", modifier = Modifier.size(14.dp))
                                        }
                                    },
                                    colors = FilterChipDefaults.filterChipColors(
                                        selectedContainerColor = colors.primaryContainer,
                                        selectedLabelColor = colors.onPrimaryContainer,
                                    ),
                                )
                            }
                        }
                    } else {
                        Spacer(Modifier.height(6.dp))
                        Text(
                            text = "Ajusta los deslizadores y guarda tus propias curvas por auriculares o altavoces.",
                            style = MaterialTheme.typography.bodySmall,
                            color = colors.onSurfaceVariant,
                        )
                    }
                }
            }

            // Dynamic FX Sliders: BassBoost, Virtualizer 3D, Loudness, Preamp, Stereo Balance
            item {
                MagicSurfaceCard {
                    Text(
                        text = "Dinámica y Efectos DSP",
                        style = MaterialTheme.typography.titleMedium,
                        color = colors.onSurface,
                    )
                    Spacer(Modifier.height(10.dp))

                    DspSliderRow(
                        label = "Refuerzo de graves (BassBoost)",
                        valueLabel = "${(eqCfg.bassBoostStrength / 10)}%",
                        value = eqCfg.bassBoostStrength.toFloat(),
                        range = 0f..1000f,
                        enabled = eqCfg.enabled,
                        onValueChange = { onBassBoostChange(it.roundToInt()) },
                    )
                    DspSliderRow(
                        label = "Espacializador 3D (Virtualizer)",
                        valueLabel = "${(eqCfg.virtualizerStrength / 10)}%",
                        value = eqCfg.virtualizerStrength.toFloat(),
                        range = 0f..1000f,
                        enabled = eqCfg.enabled,
                        onValueChange = { onVirtualizerChange(it.roundToInt()) },
                    )
                    DspSliderRow(
                        label = "Ganancia de sonoridad (Loudness)",
                        valueLabel = "+${eqCfg.loudnessGainMb / 100f} dB",
                        value = eqCfg.loudnessGainMb.toFloat(),
                        range = 0f..1000f,
                        enabled = eqCfg.enabled,
                        onValueChange = { onLoudnessChange(it.roundToInt()) },
                    )
                    DspSliderRow(
                        label = "Preamplificador",
                        valueLabel = "${if (eqCfg.preampGainDb >= 0f) "+" else ""}${"%.1f".format(eqCfg.preampGainDb)} dB",
                        value = eqCfg.preampGainDb,
                        range = -6f..6f,
                        enabled = eqCfg.enabled,
                        onValueChange = onPreampChange,
                    )
                    DspSliderRow(
                        label = "Balance estéreo (I / D)",
                        valueLabel = when {
                            eqCfg.stereoBalance < -0.05f -> "Izq ${(eqCfg.stereoBalance * -100).roundToInt()}%"
                            eqCfg.stereoBalance > 0.05f -> "Der ${(eqCfg.stereoBalance * 100).roundToInt()}%"
                            else -> "Centro"
                        },
                        value = eqCfg.stereoBalance,
                        range = -1f..1f,
                        enabled = eqCfg.enabled,
                        onValueChange = onStereoBalanceChange,
                    )
                }
            }

            // Hi-Res Engine Toggles & Crossfade
            item {
                MagicSurfaceCard {
                    Text(
                        text = "Calidad de Reproducción Hi-Res",
                        style = MaterialTheme.typography.titleMedium,
                        color = colors.onSurface,
                    )
                    Spacer(Modifier.height(6.dp))
                    MagicSettingsSwitchRow(
                        title = "Reproducción sin pausas (Gapless)",
                        subtitle = "Encadena pistas de álbumes sin silencios",
                        checked = eqCfg.gaplessEnabled,
                        onCheckedChange = onGaplessChange,
                    )
                    MagicSettingsSwitchRow(
                        title = "Normalización ReplayGain / Sonoridad",
                        subtitle = "Mantiene nivel constante entre álbumes",
                        checked = eqCfg.replayGainEnabled,
                        onCheckedChange = onReplayGainChange,
                    )
                    MagicSettingsSwitchRow(
                        title = "Salida flotante 24/32-bit PCM",
                        subtitle = "Mayor rango dinámico en DAC USB-C compatible",
                        checked = eqCfg.float24BitOutput,
                        onCheckedChange = onFloat24BitChange,
                    )
                    MagicSettingsSwitchRow(
                        title = "Buffer Hi-Res estable",
                        subtitle = "Evita microcortes en Bluetooth LDAC / aptX HD",
                        checked = eqCfg.hiResBufferHigh,
                        onCheckedChange = onHiResBufferChange,
                    )
                    Spacer(Modifier.height(6.dp))
                    DspSliderRow(
                        label = "Crossfade entre pistas",
                        valueLabel = if (eqCfg.crossfadeSeconds == 0) "Desactivado" else "${eqCfg.crossfadeSeconds} s",
                        value = eqCfg.crossfadeSeconds.toFloat(),
                        range = 0f..12f,
                        enabled = true,
                        onValueChange = { onCrossfadeChange(it.roundToInt()) },
                    )
                }
            }

            // Detected Outputs & Hardware DSP Capability Diagnostics
            item {
                MagicSurfaceCard {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Rounded.SpeakerGroup, contentDescription = null, tint = colors.primary)
                            Spacer(Modifier.width(8.dp))
                            Text(
                                text = "Salidas detectadas y Hardware DSP",
                                style = MaterialTheme.typography.titleMedium,
                                color = colors.onSurface,
                            )
                        }
                        TextButton(onClick = onRefreshOutputs) { Text("Actualizar") }
                    }
                    Spacer(Modifier.height(6.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        CapabilityPill("EQ", dspReport.equalizerAttached)
                        CapabilityPill("Bass", dspReport.bassBoostSupported)
                        CapabilityPill("3D", dspReport.virtualizerSupported)
                        CapabilityPill("Loud", dspReport.loudnessSupported)
                    }
                    Spacer(Modifier.height(10.dp))
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        state.audioOutputs.forEach { dev ->
                            Surface(
                                onClick = { onSelectOutput(dev) },
                                shape = RoundedCornerShape(tokens.radii.listItem),
                                color = if (dev.isActive) colors.primaryContainer else colors.surfaceContainerLow,
                                border = BorderStroke(1.dp, if (dev.isActive) colors.primary else tokens.semantic.divider),
                                modifier = Modifier.fillMaxWidth(),
                            ) {
                                Row(
                                    modifier = Modifier.padding(12.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically,
                                ) {
                                    Column {
                                        Text(
                                            text = dev.name,
                                            style = MaterialTheme.typography.titleSmall,
                                            color = if (dev.isActive) colors.onPrimaryContainer else colors.onSurface,
                                        )
                                        Text(
                                            text = dev.typeLabel,
                                            style = MaterialTheme.typography.bodySmall,
                                            color = if (dev.isActive) colors.onPrimaryContainer.copy(alpha = 0.8f) else colors.onSurfaceVariant,
                                        )
                                    }
                                    if (dev.isActive) {
                                        Icon(Icons.Rounded.Check, contentDescription = "Activa", tint = colors.onPrimaryContainer)
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    if (showSavePresetDialog) {
        AlertDialog(
            onDismissRequest = { showSavePresetDialog = false },
            title = { Text("Guardar curva personalizada") },
            text = {
                OutlinedTextField(
                    value = presetNameDraft,
                    onValueChange = { presetNameDraft = it },
                    label = { Text("Nombre (ej. Auriculares Sony)") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                )
            },
            confirmButton = {
                Button(
                    enabled = presetNameDraft.isNotBlank(),
                    onClick = {
                        onSaveCustomPreset(presetNameDraft.trim())
                        showSavePresetDialog = false
                    },
                ) {
                    Text("Guardar")
                }
            },
            dismissButton = {
                TextButton(onClick = { showSavePresetDialog = false }) { Text("Cancelar") }
            },
        )
    }
}

@Composable
private fun DspSliderRow(
    label: String,
    valueLabel: String,
    value: Float,
    range: ClosedFloatingPointRange<Float>,
    enabled: Boolean,
    onValueChange: (Float) -> Unit,
) {
    val colors = MaterialTheme.colorScheme
    Column(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Text(label, style = MaterialTheme.typography.labelLarge, color = colors.onSurface)
            Text(valueLabel, style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.SemiBold, color = colors.primary)
        }
        Slider(
            value = value.coerceIn(range.start, range.endInclusive),
            onValueChange = onValueChange,
            valueRange = range,
            enabled = enabled,
            colors = SliderDefaults.colors(
                thumbColor = colors.primary,
                activeTrackColor = colors.primary,
                inactiveTrackColor = colors.surfaceContainerHighest,
            ),
        )
    }
}

@Composable
private fun CapabilityPill(label: String, supported: Boolean) {
    val colors = MaterialTheme.colorScheme
    Surface(
        shape = RoundedCornerShape(8.dp),
        color = if (supported) colors.tertiaryContainer else colors.surfaceContainerHighest,
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            Icon(
                imageVector = if (supported) Icons.Rounded.GraphicEq else Icons.Rounded.Close,
                contentDescription = null,
                tint = if (supported) colors.onTertiaryContainer else colors.onSurfaceVariant,
                modifier = Modifier.size(12.dp),
            )
            Text(
                text = label,
                style = MaterialTheme.typography.labelSmall,
                color = if (supported) colors.onTertiaryContainer else colors.onSurfaceVariant,
            )
        }
    }
}
