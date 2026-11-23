package com.example.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
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
import androidx.compose.material.icons.automirrored.rounded.AddToHomeScreen
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import com.example.data.CustomControlAction
import com.example.data.WidgetStyle
import com.example.ui.MagicMusicUiState
import com.example.ui.components.MagicGlassCard
import com.example.ui.components.WidgetPreviewCard
import com.example.ui.components.customActionIcon
import com.example.ui.theme.HonorCobalt
import com.example.ui.theme.HonorCyan

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun WidgetStudioScreen(
    state: MagicMusicUiState,
    onPlayPause: () -> Unit,
    onPrev: () -> Unit,
    onNext: () -> Unit,
    onCustomAction: (CustomControlAction) -> Unit,
    onOpenSource: () -> Unit,
    onStyleSelected: (WidgetStyle) -> Unit,
    onCornerRadiusChange: (Int) -> Unit,
    onOpacityChange: (Float) -> Unit,
    onToggleProgressBar: (Boolean) -> Unit,
    onToggleServiceBadge: (Boolean) -> Unit,
    onToggleCapsuleHeader: (Boolean) -> Unit,
    onLeftCustomSelected: (CustomControlAction) -> Unit,
    onRightCustomSelected: (CustomControlAction) -> Unit,
    onPinWidget: (isCapsule: Boolean) -> Unit,
) {
    var previewCapsule by remember { mutableStateOf(false) }
    val cfg = state.widgetConfig

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .testTag("widget_studio_list"),
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        item {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text(
                    text = "Estudio de Widgets MagicOS 10",
                    style = MaterialTheme.typography.headlineMedium,
                    color = MaterialTheme.colorScheme.onBackground,
                )

                SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
                    SegmentedButton(
                        selected = !previewCapsule,
                        onClick = { previewCapsule = false },
                        shape = SegmentedButtonDefaults.itemShape(index = 0, count = 2),
                        modifier = Modifier.testTag("studio_mode_card"),
                    ) {
                        Text("Tarjeta Control 4×2")
                    }
                    SegmentedButton(
                        selected = previewCapsule,
                        onClick = { previewCapsule = true },
                        shape = SegmentedButtonDefaults.itemShape(index = 1, count = 2),
                        modifier = Modifier.testTag("studio_mode_capsule"),
                    ) {
                        Text("Cápsula Viva 4×1")
                    }
                }

                WidgetPreviewCard(
                    media = state.media,
                    config = cfg,
                    isCapsuleMode = previewCapsule,
                    onPlayPause = onPlayPause,
                    onPrev = onPrev,
                    onNext = onNext,
                    onCustomAction = onCustomAction,
                    onOpenSource = onOpenSource,
                )

                Button(
                    onClick = { onPinWidget(previewCapsule) },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = HonorCobalt,
                        contentColor = Color.White,
                    ),
                    shape = RoundedCornerShape(20.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(52.dp)
                        .testTag("studio_pin_widget_btn"),
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Rounded.AddToHomeScreen,
                        contentDescription = null,
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = if (previewCapsule) {
                            "Fijar Cápsula Viva 4×1 en Escritorio"
                        } else {
                            "Fijar Tarjeta Control 4×2 en Escritorio"
                        },
                    )
                }
            }
        }

        // Style selection
        item {
            MagicGlassCard {
                Text(
                    text = "Acabado Visual Honor",
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurface,
                )
                Spacer(modifier = Modifier.height(10.dp))

                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    WidgetStyle.entries.forEach { style ->
                        val selected = cfg.style == style
                        Surface(
                            shape = RoundedCornerShape(16.dp),
                            color = if (selected) HonorCyan.copy(alpha = 0.14f) else Color.White.copy(alpha = 0.04f),
                            border = BorderStroke(
                                1.dp,
                                if (selected) HonorCyan else Color.White.copy(alpha = 0.12f),
                            ),
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { onStyleSelected(style) }
                                .testTag("style_option_${style.name.lowercase()}"),
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 14.dp, vertical = 12.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically,
                            ) {
                                Column {
                                    Text(
                                        text = style.title,
                                        style = MaterialTheme.typography.labelLarge,
                                        color = Color.White,
                                    )
                                    Text(
                                        text = style.subtitle,
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = Color(0xFF94A3B8),
                                    )
                                }
                                if (selected) {
                                    Text(
                                        text = "ACTIVO",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = HonorCyan,
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }

        // Custom controls slots
        item {
            MagicGlassCard {
                Text(
                    text = "Botones Personalizados del Widget",
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurface,
                )
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = "Elige las acciones rápidas laterales de la Tarjeta 4×2.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )

                Spacer(modifier = Modifier.height(12.dp))
                Text(
                    text = "Botón Izquierdo",
                    style = MaterialTheme.typography.labelLarge,
                    color = HonorCyan,
                )
                Spacer(modifier = Modifier.height(6.dp))
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp),
                ) {
                    CustomControlAction.entries.forEach { action ->
                        FilterChip(
                            selected = cfg.leftCustomAction == action,
                            onClick = { onLeftCustomSelected(action) },
                            leadingIcon = {
                                Icon(
                                    imageVector = customActionIcon(action),
                                    contentDescription = null,
                                    modifier = Modifier.size(16.dp),
                                )
                            },
                            label = { Text(action.shortLabel) },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = HonorCyan.copy(alpha = 0.22f),
                                selectedLabelColor = Color.White,
                            ),
                        )
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))
                Text(
                    text = "Botón Derecho",
                    style = MaterialTheme.typography.labelLarge,
                    color = HonorCyan,
                )
                Spacer(modifier = Modifier.height(6.dp))
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp),
                ) {
                    CustomControlAction.entries.forEach { action ->
                        FilterChip(
                            selected = cfg.rightCustomAction == action,
                            onClick = { onRightCustomSelected(action) },
                            leadingIcon = {
                                Icon(
                                    imageVector = customActionIcon(action),
                                    contentDescription = null,
                                    modifier = Modifier.size(16.dp),
                                )
                            },
                            label = { Text(action.shortLabel) },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = HonorCyan.copy(alpha = 0.22f),
                                selectedLabelColor = Color.White,
                            ),
                        )
                    }
                }
            }
        }

        // Geometry & Glass details
        item {
            MagicGlassCard {
                Text(
                    text = "Geometría y Transparencia",
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurface,
                )
                Spacer(modifier = Modifier.height(10.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                ) {
                    Text("Radio de esquinas MagicOS", style = MaterialTheme.typography.bodyMedium)
                    Text("${cfg.cornerRadiusDp} dp", style = MaterialTheme.typography.labelSmall, color = HonorCyan)
                }
                Slider(
                    value = cfg.cornerRadiusDp.toFloat(),
                    onValueChange = { onCornerRadiusChange(it.toInt()) },
                    valueRange = 12f..34f,
                    colors = SliderDefaults.colors(thumbColor = HonorCyan, activeTrackColor = HonorCyan),
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                ) {
                    Text("Opacidad cristal esmerilado", style = MaterialTheme.typography.bodyMedium)
                    Text("${(cfg.glassOpacity * 100).toInt()}%", style = MaterialTheme.typography.labelSmall, color = HonorCyan)
                }
                Slider(
                    value = cfg.glassOpacity,
                    onValueChange = onOpacityChange,
                    valueRange = 0.55f..1.0f,
                    colors = SliderDefaults.colors(thumbColor = HonorCyan, activeTrackColor = HonorCyan),
                )

                Spacer(modifier = Modifier.height(6.dp))
                StudioSwitchRow(
                    label = "Mostrar barra de progreso",
                    checked = cfg.showProgressBar,
                    onCheckedChange = onToggleProgressBar,
                )
                StudioSwitchRow(
                    label = "Insignia de servicio (Spotify / YT / Amazon)",
                    checked = cfg.showServiceBadge,
                    onCheckedChange = onToggleServiceBadge,
                )
                StudioSwitchRow(
                    label = "Cabecera Magic Capsule / MagicOS 10",
                    checked = cfg.showMagicCapsuleHeader,
                    onCheckedChange = onToggleCapsuleHeader,
                )
            }
        }
    }
}

@Composable
private fun StudioSwitchRow(
    label: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.weight(1f),
        )
        Switch(checked = checked, onCheckedChange = onCheckedChange)
    }
}
