package com.example.ui.screens

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
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
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.AddToHomeScreen
import androidx.compose.material.icons.rounded.BookmarkAdd
import androidx.compose.material.icons.rounded.ContentCopy
import androidx.compose.material.icons.rounded.DeleteOutline
import androidx.compose.material.icons.rounded.Download
import androidx.compose.material.icons.rounded.Upload
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.core.compat.AndroidVersionCompat
import com.example.data.CustomControlAction
import com.example.data.WidgetFormat
import com.example.data.WidgetStyle
import com.example.data.WidgetTemplate
import com.example.designsystem.ArtworkPaletteColors
import com.example.designsystem.MagicTheme
import com.example.ui.MagicMusicUiState
import com.example.ui.components.MagicSurfaceCard
import com.example.ui.components.WidgetPreviewCard
import com.example.ui.components.customActionIcon
import kotlin.math.roundToInt

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun WidgetStudioScreen(
    state: MagicMusicUiState,
    artworkColors: ArtworkPaletteColors,
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
    onToggleCompactHeader: (Boolean) -> Unit,
    onLeftCustomSelected: (CustomControlAction) -> Unit,
    onRightCustomSelected: (CustomControlAction) -> Unit,
    onPinWidget: (WidgetFormat) -> Unit,
    onSaveTemplate: (String, WidgetFormat) -> Unit,
    onApplyTemplate: (WidgetTemplate) -> Unit,
    onDuplicateTemplate: (String) -> Unit,
    onDeleteTemplate: (String) -> Unit,
    onExportTemplatesJson: ((String) -> Unit) -> Unit,
    onImportTemplatesJson: (String) -> Unit,
    onCopyFeedback: (String) -> Unit,
) {
    val context = LocalContext.current
    val tokens = MagicTheme.tokens
    val colors = MaterialTheme.colorScheme
    val cfg = state.widgetConfig

    var selectedFormat by remember { mutableStateOf(WidgetFormat.CARD_4X2) }
    var showSaveDialog by remember { mutableStateOf(false) }
    var templateNameDraft by remember { mutableStateOf("") }
    var showImportDialog by remember { mutableStateOf(false) }
    var importJsonDraft by remember { mutableStateOf("") }
    var exportedJsonPreview by remember { mutableStateOf<String?>(null) }

    val pinSupported = remember(context) { AndroidVersionCompat.supportsRequestPinAppWidget(context) }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .testTag("widget_studio_list"),
        contentPadding = PaddingValues(
            start = tokens.spacing.screenMargin,
            end = tokens.spacing.screenMargin,
            top = tokens.spacing.sm,
            bottom = tokens.spacing.lg,
        ),
        verticalArrangement = Arrangement.spacedBy(tokens.spacing.md),
    ) {
        // 1. Header + 4-Format Gallery Cards (4x2, 4x1, 2x2, 2x1)
        item {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text(
                    text = "Estudio de Widgets",
                    style = MaterialTheme.typography.headlineMedium,
                    color = colors.onBackground,
                )
                Text(
                    text = "Elige uno de los 4 tamaños adaptables Material 3 y personalízalo en vivo.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = colors.onSurfaceVariant,
                )

                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("studio_mode_card"),
                ) {
                    items(WidgetFormat.entries, key = { it.name }) { format ->
                        val selected = selectedFormat == format
                        Surface(
                            onClick = { selectedFormat = format },
                            shape = RoundedCornerShape(tokens.radii.listItem),
                            color = if (selected) tokens.semantic.selectedContainer else tokens.semantic.cardSurface,
                            border = BorderStroke(
                                width = if (selected) 1.5.dp else 1.dp,
                                color = if (selected) colors.primary else tokens.semantic.divider,
                            ),
                            modifier = Modifier.testTag("studio_format_${format.name.lowercase()}"),
                        ) {
                            Column(
                                modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
                                verticalArrangement = Arrangement.spacedBy(2.dp),
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                                ) {
                                    Text(
                                        text = format.title,
                                        style = MaterialTheme.typography.titleSmall,
                                        fontWeight = FontWeight.SemiBold,
                                        color = if (selected) colors.primary else colors.onSurface,
                                    )
                                    Surface(
                                        shape = RoundedCornerShape(6.dp),
                                        color = colors.primary.copy(alpha = 0.16f),
                                    ) {
                                        Text(
                                            text = format.gridLabel,
                                            style = MaterialTheme.typography.labelSmall,
                                            color = colors.primary,
                                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                                        )
                                    }
                                }
                                Text(
                                    text = format.description,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = colors.onSurfaceVariant,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                )
                            }
                        }
                    }
                }
            }
        }

        // 2. Unified Live Widget Preview + Pin button
        item {
            WidgetPreviewCard(
                media = state.media,
                config = cfg,
                format = selectedFormat,
                dominantArtworkColor = artworkColors.dominant,
                lastHistoryTrack = state.listeningStats.recentTracks.firstOrNull(),
                preferredPackage = state.sessionPreferences.preferredServicePackage,
                onPlayPause = onPlayPause,
                onPrev = onPrev,
                onNext = onNext,
                onCustomAction = onCustomAction,
                onOpenSource = onOpenSource,
            )
            Spacer(modifier = Modifier.height(12.dp))
            Button(
                onClick = { onPinWidget(selectedFormat) },
                colors = ButtonDefaults.buttonColors(
                    containerColor = colors.primary,
                    contentColor = colors.onPrimary,
                ),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp)
                    .testTag("studio_pin_widget_btn"),
            ) {
                Icon(Icons.AutoMirrored.Rounded.AddToHomeScreen, contentDescription = null)
                Spacer(modifier = Modifier.width(8.dp))
                Text("Añadir ${selectedFormat.title} (${selectedFormat.gridLabel}) al escritorio")
            }
            if (!pinSupported) {
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = "Si tu lanzador no muestra la ventana directa: mantén pulsado el fondo > Widgets > MagicMusic.",
                    style = MaterialTheme.typography.bodySmall,
                    color = colors.onSurfaceVariant,
                )
            }
        }

        // 3. 4 Material 3 Widget Styles
        item {
            MagicSurfaceCard {
                Text(
                    text = "Estilo Material 3 del Widget",
                    style = MaterialTheme.typography.titleMedium,
                    color = colors.onSurface,
                )
                Spacer(modifier = Modifier.height(10.dp))
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    WidgetStyle.entries.forEach { style ->
                        val selected = cfg.style == style
                        Surface(
                            shape = RoundedCornerShape(tokens.radii.listItem),
                            color = if (selected) tokens.semantic.selectedContainer.copy(alpha = 0.7f) else colors.surfaceContainerLow,
                            border = BorderStroke(
                                width = if (selected) 1.5.dp else 1.dp,
                                color = if (selected) colors.primary else tokens.semantic.divider,
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
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween,
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = style.title,
                                        style = MaterialTheme.typography.titleSmall,
                                        color = colors.onSurface,
                                    )
                                    Text(
                                        text = style.subtitle,
                                        style = MaterialTheme.typography.bodySmall,
                                        color = colors.onSurfaceVariant,
                                    )
                                }
                                if (selected) {
                                    Text(
                                        text = "Activo",
                                        style = MaterialTheme.typography.labelMedium,
                                        color = colors.primary,
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }

        // 4. Geometry, Transparency & Toggles
        item {
            MagicSurfaceCard {
                Text(
                    text = "Forma, Opacidad y Elementos",
                    style = MaterialTheme.typography.titleMedium,
                    color = colors.onSurface,
                )
                Spacer(modifier = Modifier.height(10.dp))

                Text(
                    text = "Radio de esquinas: ${cfg.cornerRadiusDp} dp",
                    style = MaterialTheme.typography.labelLarge,
                    color = colors.onSurfaceVariant,
                )
                Slider(
                    value = cfg.cornerRadiusDp.toFloat(),
                    onValueChange = { onCornerRadiusChange(it.roundToInt()) },
                    valueRange = 12f..32f,
                    colors = SliderDefaults.colors(
                        thumbColor = colors.primary,
                        activeTrackColor = colors.primary,
                    ),
                )

                Text(
                    text = "Opacidad del contenedor: ${(cfg.glassOpacity * 100).roundToInt()}%",
                    style = MaterialTheme.typography.labelLarge,
                    color = colors.onSurfaceVariant,
                )
                Slider(
                    value = cfg.glassOpacity,
                    onValueChange = onOpacityChange,
                    valueRange = 0.45f..1.0f,
                    colors = SliderDefaults.colors(
                        thumbColor = colors.primary,
                        activeTrackColor = colors.primary,
                    ),
                )

                StudioToggleRow(
                    title = "Mostrar barra de progreso",
                    checked = cfg.showProgressBar,
                    onCheckedChange = onToggleProgressBar,
                )
                StudioToggleRow(
                    title = "Insignia del servicio (Spotify, YT Music…)",
                    checked = cfg.showServiceBadge,
                    onCheckedChange = onToggleServiceBadge,
                )
                StudioToggleRow(
                    title = "Cabecera compacta en formato 4×1",
                    checked = cfg.showCompactHeader,
                    onCheckedChange = onToggleCompactHeader,
                )
            }
        }

        // 5. Left & Right Custom Actions (all 8 actions)
        item {
            MagicSurfaceCard {
                Text(
                    text = "Botones Configurables (Tarjeta 4×2)",
                    style = MaterialTheme.typography.titleMedium,
                    color = colors.onSurface,
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "Acción izquierda",
                    style = MaterialTheme.typography.labelLarge,
                    color = colors.onSurfaceVariant,
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
                                selectedContainerColor = colors.primaryContainer,
                                selectedLabelColor = colors.onPrimaryContainer,
                            ),
                        )
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))
                Text(
                    text = "Acción derecha",
                    style = MaterialTheme.typography.labelLarge,
                    color = colors.onSurfaceVariant,
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
                                selectedContainerColor = colors.primaryContainer,
                                selectedLabelColor = colors.onPrimaryContainer,
                            ),
                        )
                    }
                }
            }
        }

        // 6. Widget Templates & JSON Export/Import
        item {
            MagicSurfaceCard(modifier = Modifier.testTag("studio_templates_card")) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        text = "Plantillas de Widget",
                        style = MaterialTheme.typography.titleMedium,
                        color = colors.onSurface,
                    )
                    FilledTonalButton(
                        onClick = {
                            templateNameDraft = "${cfg.style.title} ${selectedFormat.gridLabel}"
                            showSaveDialog = true
                        },
                        modifier = Modifier.testTag("studio_save_template_btn"),
                    ) {
                        Icon(Icons.Rounded.BookmarkAdd, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Guardar")
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    state.widgetTemplates.forEach { tpl ->
                        Surface(
                            onClick = {
                                selectedFormat = tpl.format
                                onApplyTemplate(tpl)
                            },
                            shape = RoundedCornerShape(tokens.radii.listItem),
                            color = colors.surfaceContainerLow,
                            border = BorderStroke(1.dp, tokens.semantic.divider),
                            modifier = Modifier.fillMaxWidth(),
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 12.dp, vertical = 10.dp),
                                verticalAlignment = Alignment.CenterVertically,
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = tpl.name,
                                        style = MaterialTheme.typography.titleSmall,
                                        color = colors.onSurface,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis,
                                    )
                                    Text(
                                        text = "${tpl.format.gridLabel} • ${tpl.config.style.title} • ${tpl.config.cornerRadiusDp}dp",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = colors.onSurfaceVariant,
                                    )
                                }
                                IconButton(onClick = { onDuplicateTemplate(tpl.id) }) {
                                    Icon(
                                        imageVector = Icons.Rounded.ContentCopy,
                                        contentDescription = "Duplicar plantilla",
                                        tint = colors.onSurfaceVariant,
                                    )
                                }
                                IconButton(onClick = { onDeleteTemplate(tpl.id) }) {
                                    Icon(
                                        imageVector = Icons.Rounded.DeleteOutline,
                                        contentDescription = "Eliminar plantilla",
                                        tint = colors.onSurfaceVariant,
                                    )
                                }
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    OutlinedButton(
                        onClick = {
                            onExportTemplatesJson { json ->
                                exportedJsonPreview = json
                            }
                        },
                        modifier = Modifier.weight(1f),
                    ) {
                        Icon(Icons.Rounded.Upload, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Exportar JSON")
                    }
                    OutlinedButton(
                        onClick = { showImportDialog = true },
                        modifier = Modifier.weight(1f),
                    ) {
                        Icon(Icons.Rounded.Download, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Importar JSON")
                    }
                }
            }
        }
    }

    if (showSaveDialog) {
        AlertDialog(
            onDismissRequest = { showSaveDialog = false },
            title = { Text("Guardar plantilla") },
            text = {
                OutlinedTextField(
                    value = templateNameDraft,
                    onValueChange = { templateNameDraft = it },
                    label = { Text("Nombre de la plantilla") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        onSaveTemplate(templateNameDraft, selectedFormat)
                        showSaveDialog = false
                    },
                ) {
                    Text("Guardar")
                }
            },
            dismissButton = {
                TextButton(onClick = { showSaveDialog = false }) {
                    Text("Cancelar")
                }
            },
        )
    }

    if (showImportDialog) {
        AlertDialog(
            onDismissRequest = { showImportDialog = false },
            title = { Text("Importar plantillas JSON") },
            text = {
                OutlinedTextField(
                    value = importJsonDraft,
                    onValueChange = { importJsonDraft = it },
                    label = { Text("Pega aquí el JSON exportado") },
                    minLines = 3,
                    maxLines = 6,
                    modifier = Modifier.fillMaxWidth(),
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        onImportTemplatesJson(importJsonDraft)
                        importJsonDraft = ""
                        showImportDialog = false
                    },
                ) {
                    Text("Importar")
                }
            },
            dismissButton = {
                TextButton(onClick = { showImportDialog = false }) {
                    Text("Cancelar")
                }
            },
        )
    }

    exportedJsonPreview?.let { jsonText ->
        AlertDialog(
            onDismissRequest = { exportedJsonPreview = null },
            title = { Text("JSON de plantillas") },
            text = {
                OutlinedTextField(
                    value = jsonText,
                    onValueChange = {},
                    readOnly = true,
                    maxLines = 8,
                    modifier = Modifier.fillMaxWidth(),
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        val cm = context.getSystemService(Context.CLIPBOARD_SERVICE) as? ClipboardManager
                        cm?.setPrimaryClip(ClipData.newPlainText("MagicMusic Templates", jsonText))
                        onCopyFeedback("Plantillas JSON copiadas al portapapeles")
                        exportedJsonPreview = null
                    },
                ) {
                    Text("Copiar JSON")
                }
            },
            dismissButton = {
                TextButton(onClick = { exportedJsonPreview = null }) {
                    Text("Cerrar")
                }
            },
        )
    }
}

@Composable
private fun StudioToggleRow(
    title: String,
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
            text = title,
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.weight(1f),
        )
        Switch(checked = checked, onCheckedChange = onCheckedChange)
    }
}
