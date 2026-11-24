package com.example.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.ChevronRight
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import com.example.designsystem.MagicTheme
import com.example.designsystem.SeedColorSwatch
import com.example.designsystem.SurfaceToneStyle
import kotlin.math.roundToInt

@Immutable
data class EqualizerBand(
    val freqLabel: String,
    val gainDb: Float, // -12f .. +12f
)

enum class EqualizerPreset(
    val label: String,
    val gains: List<Float>,
) {
    FLAT("Plano", listOf(0f, 0f, 0f, 0f, 0f)),
    VOCAL("Vocal", listOf(-2f, 2f, 5f, 4f, 1f)),
    BASS("Graves", listOf(7f, 4f, 0f, 1f, 2f)),
    ROCK("Rock", listOf(5f, 3f, -1f, 3f, 6f)),
    JAZZ("Jazz", listOf(3f, 1f, 3f, 2f, 4f)),
    ACOUSTIC("Acústico", listOf(2f, 3f, 2f, 4f, 3f)),
}

@Composable
fun MagicEqualizerPanel(
    bands: List<EqualizerBand>,
    selectedPreset: EqualizerPreset,
    isAdvancedMode: Boolean,
    onBandChange: (index: Int, newDb: Float) -> Unit,
    onSelectPreset: (EqualizerPreset) -> Unit,
    onModeChange: (isAdvanced: Boolean) -> Unit,
    modifier: Modifier = Modifier,
) {
    val tokens = MagicTheme.tokens
    val colors = MaterialTheme.colorScheme

    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        // Mode segmented selector (Básico / Avanzado)
        SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
            SegmentedButton(
                selected = !isAdvancedMode,
                onClick = { onModeChange(false) },
                shape = SegmentedButtonDefaults.itemShape(index = 0, count = 2),
                modifier = Modifier.testTag("eq_mode_basic"),
            ) {
                Text("Básico")
            }
            SegmentedButton(
                selected = isAdvancedMode,
                onClick = { onModeChange(true) },
                shape = SegmentedButtonDefaults.itemShape(index = 1, count = 2),
                modifier = Modifier.testTag("eq_mode_advanced"),
            ) {
                Text("Avanzado")
            }
        }

        // Preset chips row
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            EqualizerPreset.entries.take(4).forEach { preset ->
                MagicFilterChip(
                    label = preset.label,
                    selected = selectedPreset == preset,
                    onClick = { onSelectPreset(preset) },
                    modifier = Modifier.weight(1f),
                )
            }
        }

        // Main dB vertical slider deck matching reference screen 17
        MagicSurfaceCard(
            cornerRadius = tokens.radii.card,
            contentPadding = tokens.spacing.md,
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(210.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                // dB vertical scale
                Column(
                    modifier = Modifier
                        .fillMaxHeight()
                        .padding(end = 10.dp),
                    verticalArrangement = Arrangement.SpaceBetween,
                    horizontalAlignment = Alignment.End,
                ) {
                    Text("+12", style = MaterialTheme.typography.labelSmall, color = colors.onSurfaceVariant)
                    Text("0 dB", style = MaterialTheme.typography.labelSmall, color = colors.primary)
                    Text("-12", style = MaterialTheme.typography.labelSmall, color = colors.onSurfaceVariant)
                }

                // Vertical band sliders
                Row(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxHeight(),
                    horizontalArrangement = Arrangement.SpaceEvenly,
                ) {
                    bands.forEachIndexed { idx, band ->
                        VerticalEqBandSlider(
                            freqLabel = band.freqLabel,
                            gainDb = band.gainDb,
                            onValueChange = { onBandChange(idx, it) },
                            modifier = Modifier.weight(1f),
                        )
                    }
                }
            }

            if (isAdvancedMode) {
                Spacer(modifier = Modifier.height(12.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    EqualizerPreset.entries.drop(4).forEach { preset ->
                        MagicFilterChip(
                            label = preset.label,
                            selected = selectedPreset == preset,
                            onClick = { onSelectPreset(preset) },
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun VerticalEqBandSlider(
    freqLabel: String,
    gainDb: Float,
    onValueChange: (Float) -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = MaterialTheme.colorScheme
    val tokens = MagicTheme.tokens
    var trackHeightPx by remember { mutableFloatStateOf(1f) }
    val normalized = ((gainDb + 12f) / 24f).coerceIn(0f, 1f)

    Column(
        modifier = modifier.fillMaxHeight(),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(
            text = "${if (gainDb >= 0f) "+" else ""}${gainDb.roundToInt()}",
            style = MaterialTheme.typography.labelSmall,
            color = colors.primary,
        )
        Spacer(modifier = Modifier.height(6.dp))
        Box(
            modifier = Modifier
                .weight(1f)
                .width(36.dp)
                .onSizeChanged { trackHeightPx = it.height.toFloat().coerceAtLeast(1f) }
                .pointerInput(Unit) {
                    detectVerticalDragGestures { change, dragAmount ->
                        change.consume()
                        val deltaFraction = -dragAmount / trackHeightPx
                        val nextDb = (gainDb + deltaFraction * 24f).coerceIn(-12f, 12f)
                        onValueChange(nextDb)
                    }
                },
            contentAlignment = Alignment.Center,
        ) {
            // Inactive vertical track
            Box(
                modifier = Modifier
                    .width(5.dp)
                    .fillMaxHeight()
                    .clip(CircleShape)
                    .background(tokens.semantic.divider),
            )
            // Active vertical track from bottom
            Box(
                modifier = Modifier
                    .width(5.dp)
                    .fillMaxHeight(normalized)
                    .align(Alignment.BottomCenter)
                    .clip(CircleShape)
                    .background(colors.primary),
            )
            // Thumb
            val offsetY = ((1f - normalized) * (trackHeightPx - 36f)).roundToInt()
            Box(
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .offset { IntOffset(0, offsetY) }
                    .size(18.dp)
                    .clip(CircleShape)
                    .background(colors.primary)
                    .border(2.dp, colors.onPrimary, CircleShape),
            )
        }
        Spacer(modifier = Modifier.height(6.dp))
        Text(
            text = freqLabel,
            style = MaterialTheme.typography.labelSmall,
            color = colors.onSurfaceVariant,
            maxLines = 1,
        )
    }
}

@Composable
fun MagicSettingsRowCard(
    icon: ImageVector,
    title: String,
    subtitle: String? = null,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val tokens = MagicTheme.tokens
    val colors = MaterialTheme.colorScheme
    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(tokens.radii.listItem),
        color = tokens.semantic.cardSurface,
        border = BorderStroke(1.dp, tokens.semantic.divider),
        modifier = modifier.fillMaxWidth(),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 14.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = colors.primary,
                modifier = Modifier.size(22.dp),
            )
            Spacer(modifier = Modifier.width(14.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleSmall,
                    color = colors.onSurface,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                if (!subtitle.isNullOrBlank()) {
                    Text(
                        text = subtitle,
                        style = MaterialTheme.typography.bodySmall,
                        color = colors.onSurfaceVariant,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
            }
            Icon(
                imageVector = Icons.Rounded.ChevronRight,
                contentDescription = null,
                tint = colors.onSurfaceVariant,
            )
        }
    }
}

@Composable
fun MagicSettingsSwitchRow(
    title: String,
    subtitle: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    modifier: Modifier = Modifier,
    switchTestTag: String? = null,
) {
    val colors = MaterialTheme.colorScheme
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(modifier = Modifier.weight(1f).padding(end = 12.dp)) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleSmall,
                color = colors.onSurface,
            )
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodySmall,
                color = colors.onSurfaceVariant,
            )
        }
        Switch(
            checked = checked,
            onCheckedChange = onCheckedChange,
            modifier = if (switchTestTag != null) Modifier.testTag(switchTestTag) else Modifier,
        )
    }
}

@Composable
fun MagicModeThumbnailsSelector(
    selectedTone: SurfaceToneStyle,
    onSelectTone: (SurfaceToneStyle) -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = MaterialTheme.colorScheme
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        SurfaceToneStyle.entries.forEach { style ->
            val selected = style == selectedTone
            val (bg, card, accent) = when (style) {
                SurfaceToneStyle.DARK -> Triple(Color(0xFF100E14), Color(0xFF1E1B24), colors.primary)
                SurfaceToneStyle.TONAL -> Triple(Color(0xFF16131A), Color(0xFF2B2630), Color(0xFFCDA9FF))
                SurfaceToneStyle.LIGHT -> Triple(Color(0xFFFCF8FF), Color(0xFFE8E0EF), Color(0xFF6A45B5))
            }
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier
                    .weight(1f)
                    .clip(RoundedCornerShape(18.dp))
                    .clickable { onSelectTone(style) }
                    .testTag("surface_tone_${style.name.lowercase()}"),
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(86.dp)
                        .clip(RoundedCornerShape(18.dp))
                        .background(bg)
                        .border(
                            width = if (selected) 2.5.dp else 1.dp,
                            color = if (selected) colors.primary else colors.outlineVariant,
                            shape = RoundedCornerShape(18.dp),
                        )
                        .padding(8.dp),
                ) {
                    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth(0.65f)
                                .height(8.dp)
                                .clip(CircleShape)
                                .background(accent),
                        )
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(26.dp)
                                .clip(RoundedCornerShape(8.dp))
                                .background(card),
                        )
                        Box(
                            modifier = Modifier
                                .fillMaxWidth(0.8f)
                                .height(16.dp)
                                .clip(RoundedCornerShape(6.dp))
                                .background(card.copy(alpha = 0.75f)),
                        )
                    }
                }
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = style.label,
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium,
                    color = if (selected) colors.primary else colors.onSurfaceVariant,
                )
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun MagicColorSwatchGrid(
    swatches: List<SeedColorSwatch>,
    selectedArgb: Long,
    dynamicEnabled: Boolean,
    onSelectSwatch: (SeedColorSwatch) -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = MaterialTheme.colorScheme
    FlowRow(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        swatches.forEach { swatch ->
            val selected = !dynamicEnabled && swatch.argb == selectedArgb
            Box(
                modifier = Modifier
                    .size(44.dp)
                    .clip(CircleShape)
                    .background(swatch.color)
                    .border(
                        width = if (selected) 3.dp else 1.dp,
                        color = if (selected) colors.onBackground else Color.White.copy(alpha = 0.22f),
                        shape = CircleShape,
                    )
                    .clickable { onSelectSwatch(swatch) }
                    .testTag("swatch_${swatch.id}"),
                contentAlignment = Alignment.Center,
            ) {
                if (selected) {
                    Icon(
                        imageVector = Icons.Rounded.Check,
                        contentDescription = swatch.label,
                        tint = if (swatch.color.luminance() > 0.45f) Color(0xFF1A1033) else Color.White,
                        modifier = Modifier.size(20.dp),
                    )
                }
            }
        }
    }
}
