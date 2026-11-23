package com.example.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.Lock
import androidx.compose.material.icons.rounded.MusicNote
import androidx.compose.material.icons.rounded.Pause
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.SkipNext
import androidx.compose.material.icons.rounded.SkipPrevious
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.data.CustomControlAction
import com.example.data.QsTileMode
import com.example.ui.MagicMusicUiState
import com.example.ui.components.MagicGlassCard
import com.example.ui.components.WidgetArtworkThumb
import com.example.ui.components.customActionIcon
import com.example.ui.theme.HonorCobalt
import com.example.ui.theme.HonorCyan

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun LockAndQuickSettingsScreen(
    state: MagicMusicUiState,
    onToggleBridge: (Boolean) -> Unit,
    onToggleLockVisibility: (Boolean) -> Unit,
    onSelectLockAction1: (CustomControlAction) -> Unit,
    onSelectLockAction2: (CustomControlAction) -> Unit,
    onSelectQsTileMode: (QsTileMode) -> Unit,
    onRequestAddTile: (isPlayPause: Boolean) -> Unit,
    onOpenLockDeck: () -> Unit,
    onPlayPause: () -> Unit,
    onNext: () -> Unit,
    onPrevious: () -> Unit,
) {
    val cfg = state.lockAndQsConfig
    val media = state.media

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .testTag("lock_qs_list"),
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        item {
            Text(
                text = "Bloqueo & Centro de Control",
                style = MaterialTheme.typography.headlineMedium,
                color = MaterialTheme.colorScheme.onBackground,
            )
        }

        // Lock screen preview + MediaStyle bridge
        item {
            Surface(
                shape = RoundedCornerShape(28.dp),
                color = Color(0xFF070C17),
                border = BorderStroke(1.2.dp, HonorCyan.copy(alpha = 0.45f)),
                modifier = Modifier.fillMaxWidth(),
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(18.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.Lock,
                            contentDescription = null,
                            tint = HonorCyan,
                            modifier = Modifier.size(14.dp),
                        )
                        Text(
                            text = "VISTA PREVIA PANTALLA DE BLOQUEO ANDROID 16",
                            style = MaterialTheme.typography.labelSmall,
                            color = HonorCyan,
                        )
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    Text(
                        text = "12:45",
                        style = MaterialTheme.typography.displaySmall,
                        fontWeight = FontWeight.Bold,
                        color = Color.White,
                    )

                    Spacer(modifier = Modifier.height(14.dp))

                    Surface(
                        shape = RoundedCornerShape(24.dp),
                        color = Color(0xFF131D35),
                        border = BorderStroke(1.dp, Color.White.copy(alpha = 0.16f)),
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        Column(modifier = Modifier.padding(14.dp)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                WidgetArtworkThumb(media = media, size = 48.dp, radius = 14.dp)
                                Spacer(modifier = Modifier.width(12.dp))
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = "MagicOS • ${media.service.displayName}",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = HonorCyan,
                                    )
                                    Text(
                                        text = media.title,
                                        style = MaterialTheme.typography.titleMedium,
                                        color = Color.White,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis,
                                    )
                                    Text(
                                        text = media.artist,
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = Color(0xFF94A3B8),
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis,
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.height(10.dp))

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceEvenly,
                                verticalAlignment = Alignment.CenterVertically,
                            ) {
                                Icon(
                                    imageVector = customActionIcon(cfg.lockScreenAction1),
                                    contentDescription = cfg.lockScreenAction1.label,
                                    tint = Color(0xFF94A3B8),
                                )
                                IconButton(onClick = onPrevious) {
                                    Icon(
                                        imageVector = Icons.Rounded.SkipPrevious,
                                        contentDescription = "Anterior",
                                        tint = Color.White,
                                    )
                                }
                                Box(
                                    modifier = Modifier
                                        .size(42.dp)
                                        .clip(CircleShape)
                                        .background(Brush.linearGradient(listOf(HonorCyan, HonorCobalt)))
                                        .clickable(onClick = onPlayPause),
                                    contentAlignment = Alignment.Center,
                                ) {
                                    Icon(
                                        imageVector = if (media.isPlaying) Icons.Rounded.Pause else Icons.Rounded.PlayArrow,
                                        contentDescription = "Reproducir o pausar",
                                        tint = Color.White,
                                    )
                                }
                                IconButton(onClick = onNext) {
                                    Icon(
                                        imageVector = Icons.Rounded.SkipNext,
                                        contentDescription = "Siguiente",
                                        tint = Color.White,
                                    )
                                }
                                Icon(
                                    imageVector = customActionIcon(cfg.lockScreenAction2),
                                    contentDescription = cfg.lockScreenAction2.label,
                                    tint = Color(0xFF94A3B8),
                                )
                            }
                        }
                    }
                }
            }
        }

        item {
            FilledTonalButton(
                onClick = onOpenLockDeck,
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("open_lock_deck_preview_btn"),
            ) {
                Text("Abrir Panel Pantalla de Bloqueo Completa")
            }
        }

        // Lock screen MediaStyle bridge controls
        item {
            MagicGlassCard {
                Text(
                    text = "Controles en Pantalla de Bloqueo",
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurface,
                )
                Spacer(modifier = Modifier.height(8.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text("Puente MediaStyle Android 16", style = MaterialTheme.typography.labelLarge)
                        Text(
                            text = "Publica una sesión con tus botones extra en la pantalla de bloqueo",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                    Switch(
                        checked = state.bridgeRunning,
                        onCheckedChange = onToggleBridge,
                        modifier = Modifier.testTag("bridge_service_switch"),
                    )
                }

                Spacer(modifier = Modifier.height(8.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text("Visibilidad pública al bloquear", style = MaterialTheme.typography.labelLarge)
                        Text(
                            text = "Muestra carátula y botones con el teléfono bloqueado",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                    Switch(
                        checked = cfg.showOnLockScreen,
                        onCheckedChange = onToggleLockVisibility,
                    )
                }

                Spacer(modifier = Modifier.height(12.dp))
                Text("Acción Personalizada 1 (Izquierda)", style = MaterialTheme.typography.labelLarge, color = HonorCyan)
                Spacer(modifier = Modifier.height(6.dp))
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp),
                ) {
                    CustomControlAction.entries.forEach { action ->
                        FilterChip(
                            selected = cfg.lockScreenAction1 == action,
                            onClick = { onSelectLockAction1(action) },
                            label = { Text(action.shortLabel) },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = HonorCyan.copy(alpha = 0.22f),
                                selectedLabelColor = Color.White,
                            ),
                        )
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))
                Text("Acción Personalizada 2 (Derecha)", style = MaterialTheme.typography.labelLarge, color = HonorCyan)
                Spacer(modifier = Modifier.height(6.dp))
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp),
                ) {
                    CustomControlAction.entries.forEach { action ->
                        FilterChip(
                            selected = cfg.lockScreenAction2 == action,
                            onClick = { onSelectLockAction2(action) },
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

        // Quick Settings Honor Control Center Tiles
        item {
            MagicGlassCard(highlightBorder = true) {
                Text(
                    text = "Ajustes Rápidos (Centro de Control)",
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurface,
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "Añade botones interactivos estilo Honor MagicOS 10 directamente al panel deslizable de Ajustes Rápidos.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )

                Spacer(modifier = Modifier.height(14.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    QsPreviewTile(
                        title = if (media.hasActiveMedia) media.service.displayName else "Música",
                        subtitle = media.title,
                        active = media.isPlaying,
                        icon = Icons.Rounded.MusicNote,
                        modifier = Modifier.weight(1f),
                    )
                    QsPreviewTile(
                        title = "Siguiente",
                        subtitle = "Saltar pista",
                        active = media.hasActiveMedia,
                        icon = Icons.Rounded.SkipNext,
                        modifier = Modifier.weight(1f),
                    )
                }

                Spacer(modifier = Modifier.height(14.dp))
                Text("Comportamiento del Tile Principal", style = MaterialTheme.typography.labelLarge, color = HonorCyan)
                Spacer(modifier = Modifier.height(8.dp))

                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    QsTileMode.entries.forEach { mode ->
                        val selected = cfg.qsTileMode == mode
                        Surface(
                            shape = RoundedCornerShape(16.dp),
                            color = if (selected) HonorCyan.copy(alpha = 0.14f) else Color.White.copy(alpha = 0.04f),
                            border = BorderStroke(
                                1.dp,
                                if (selected) HonorCyan else Color.White.copy(alpha = 0.12f),
                            ),
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { onSelectQsTileMode(mode) },
                        ) {
                            Column(modifier = Modifier.padding(12.dp)) {
                                Text(mode.label, style = MaterialTheme.typography.labelLarge, color = Color.White)
                                Text(
                                    mode.description,
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = Color(0xFF94A3B8),
                                )
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    Button(
                        onClick = { onRequestAddTile(true) },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = HonorCobalt,
                            contentColor = Color.White,
                        ),
                        modifier = Modifier
                            .weight(1f)
                            .testTag("add_qs_play_tile_btn"),
                    ) {
                        Icon(Icons.Rounded.Add, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Tile Música", maxLines = 1)
                    }

                    FilledTonalButton(
                        onClick = { onRequestAddTile(false) },
                        modifier = Modifier
                            .weight(1f)
                            .testTag("add_qs_next_tile_btn"),
                    ) {
                        Icon(Icons.Rounded.Add, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Tile Siguiente", maxLines = 1)
                    }
                }
            }
        }
    }
}

@Composable
private fun QsPreviewTile(
    title: String,
    subtitle: String,
    active: Boolean,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    modifier: Modifier = Modifier,
) {
    Surface(
        shape = RoundedCornerShape(22.dp),
        color = if (active) HonorCobalt else Color(0xFF1A2540),
        border = BorderStroke(1.dp, if (active) HonorCyan else Color.White.copy(alpha = 0.14f)),
        modifier = modifier,
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 14.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Box(
                modifier = Modifier
                    .size(34.dp)
                    .clip(CircleShape)
                    .background(Color.White.copy(alpha = if (active) 0.22f else 0.10f)),
                contentAlignment = Alignment.Center,
            ) {
                Icon(icon, contentDescription = null, tint = Color.White, modifier = Modifier.size(20.dp))
            }
            Spacer(modifier = Modifier.width(10.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.labelLarge,
                    color = Color.White,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.labelSmall,
                    color = Color.White.copy(alpha = 0.78f),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }
    }
}
