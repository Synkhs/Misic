package com.example.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.AddToHomeScreen
import androidx.compose.material.icons.automirrored.rounded.OpenInNew
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.GraphicEq
import androidx.compose.material.icons.rounded.NotificationsActive
import androidx.compose.material.icons.rounded.Pause
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.RadioButtonUnchecked
import androidx.compose.material.icons.rounded.SkipNext
import androidx.compose.material.icons.rounded.SkipPrevious
import androidx.compose.material.icons.rounded.Tune
import androidx.compose.material.icons.rounded.Widgets
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.R
import com.example.data.CustomControlAction
import com.example.ui.MagicMusicUiState
import com.example.ui.components.AnimatedEqualizerBars
import com.example.ui.components.MagicGlassCard
import com.example.ui.components.WidgetArtworkThumb
import com.example.ui.components.customActionIcon
import com.example.ui.components.formatTimeMs
import com.example.ui.theme.HonorCobalt
import com.example.ui.theme.HonorCyan
import com.example.ui.theme.HonorMint

@Composable
fun HomeDeckScreen(
    state: MagicMusicUiState,
    onPlayPause: () -> Unit,
    onNext: () -> Unit,
    onPrevious: () -> Unit,
    onSeek: (Long) -> Unit,
    onCustomAction: (CustomControlAction) -> Unit,
    onSelectSession: (String) -> Unit,
    onToggleLocalEngine: () -> Unit,
    onGrantListenerPermission: () -> Unit,
    onRequestPostNotifications: () -> Unit,
    onPinWidget: () -> Unit,
    onAddQuickTile: () -> Unit,
    onOpenActiveApp: (String) -> Unit,
    onNavigateToWidgets: () -> Unit,
) {
    val media = state.media

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        // Hero MagicOS 10 Now Playing Deck
        item {
            Surface(
                shape = RoundedCornerShape(30.dp),
                border = BorderStroke(
                    1.2.dp,
                    Brush.linearGradient(
                        listOf(
                            HonorCyan.copy(alpha = 0.55f),
                            Color.White.copy(alpha = 0.14f),
                        ),
                    ),
                ),
                modifier = Modifier.fillMaxWidth(),
            ) {
                Box(modifier = Modifier.fillMaxWidth()) {
                    Image(
                        painter = painterResource(id = R.drawable.img_magicos_hero),
                        contentDescription = null,
                        contentScale = ContentScale.Crop,
                        alpha = 0.55f,
                        modifier = Modifier.matchParentSize(),
                    )
                    Box(
                        modifier = Modifier
                            .matchParentSize()
                            .background(
                                Brush.verticalGradient(
                                    listOf(
                                        Color(0x99070D1D),
                                        Color(0xE60B1224),
                                    ),
                                ),
                            ),
                    )

                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(20.dp),
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween,
                            modifier = Modifier.fillMaxWidth(),
                        ) {
                            Surface(
                                shape = CircleShape,
                                color = media.service.accentColor.copy(alpha = 0.20f),
                                border = BorderStroke(1.dp, media.service.accentColor.copy(alpha = 0.5f)),
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp),
                                ) {
                                    if (media.isPlaying) {
                                        AnimatedEqualizerBars(color = media.service.accentColor)
                                        Spacer(modifier = Modifier.width(6.dp))
                                    }
                                    Text(
                                        text = media.service.displayName.uppercase(),
                                        style = MaterialTheme.typography.labelSmall,
                                        color = Color.White,
                                    )
                                }
                            }

                            if (media.packageName.isNotBlank() && !media.isLocalEngine) {
                                IconButton(
                                    onClick = { onOpenActiveApp(media.packageName) },
                                    modifier = Modifier.testTag("home_open_source_btn"),
                                ) {
                                    Icon(
                                        imageVector = Icons.AutoMirrored.Rounded.OpenInNew,
                                        contentDescription = "Abrir app musical",
                                        tint = HonorCyan,
                                    )
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(16.dp))

                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.fillMaxWidth(),
                        ) {
                            WidgetArtworkThumb(media = media, size = 76.dp, radius = 22.dp)
                            Spacer(modifier = Modifier.width(16.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = media.title,
                                    style = MaterialTheme.typography.titleLarge,
                                    color = Color.White,
                                    maxLines = 2,
                                    overflow = TextOverflow.Ellipsis,
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = media.artist,
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = Color(0xFFB8C7E0),
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                )
                                Text(
                                    text = media.album,
                                    style = MaterialTheme.typography.labelSmall,
                                    color = HonorCyan.copy(alpha = 0.85f),
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(14.dp))

                        Slider(
                            value = media.progressFraction,
                            onValueChange = { frac ->
                                if (media.durationMs > 0L) {
                                    onSeek((frac * media.durationMs).toLong())
                                }
                            },
                            colors = SliderDefaults.colors(
                                thumbColor = HonorCyan,
                                activeTrackColor = HonorCyan,
                                inactiveTrackColor = Color.White.copy(alpha = 0.16f),
                            ),
                            modifier = Modifier.fillMaxWidth(),
                        )

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                        ) {
                            Text(
                                text = formatTimeMs(media.positionMs),
                                style = MaterialTheme.typography.labelSmall,
                                color = Color(0xFF94A3B8),
                            )
                            Text(
                                text = if (media.durationMs > 0L) formatTimeMs(media.durationMs) else "--:--",
                                style = MaterialTheme.typography.labelSmall,
                                color = Color(0xFF94A3B8),
                            )
                        }

                        Spacer(modifier = Modifier.height(10.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            IconButton(
                                onClick = { onCustomAction(state.widgetConfig.leftCustomAction) },
                                modifier = Modifier.testTag("home_custom_left_btn"),
                            ) {
                                Icon(
                                    imageVector = customActionIcon(state.widgetConfig.leftCustomAction),
                                    contentDescription = state.widgetConfig.leftCustomAction.label,
                                    tint = Color(0xFF94A3B8),
                                )
                            }

                            IconButton(
                                onClick = onPrevious,
                                modifier = Modifier.testTag("home_prev_btn"),
                            ) {
                                Icon(
                                    imageVector = Icons.Rounded.SkipPrevious,
                                    contentDescription = "Anterior",
                                    tint = Color.White,
                                    modifier = Modifier.size(32.dp),
                                )
                            }

                            Box(
                                modifier = Modifier
                                    .size(62.dp)
                                    .clip(CircleShape)
                                    .background(Brush.linearGradient(listOf(HonorCyan, HonorCobalt)))
                                    .clickable(onClick = onPlayPause)
                                    .testTag("home_play_pause_btn"),
                                contentAlignment = Alignment.Center,
                            ) {
                                Icon(
                                    imageVector = if (media.isPlaying) Icons.Rounded.Pause else Icons.Rounded.PlayArrow,
                                    contentDescription = "Reproducir o pausar",
                                    tint = Color.White,
                                    modifier = Modifier.size(34.dp),
                                )
                            }

                            IconButton(
                                onClick = onNext,
                                modifier = Modifier.testTag("home_next_btn"),
                            ) {
                                Icon(
                                    imageVector = Icons.Rounded.SkipNext,
                                    contentDescription = "Siguiente",
                                    tint = Color.White,
                                    modifier = Modifier.size(32.dp),
                                )
                            }

                            IconButton(
                                onClick = { onCustomAction(state.widgetConfig.rightCustomAction) },
                                modifier = Modifier.testTag("home_custom_right_btn"),
                            ) {
                                Icon(
                                    imageVector = customActionIcon(state.widgetConfig.rightCustomAction),
                                    contentDescription = state.widgetConfig.rightCustomAction.label,
                                    tint = Color(0xFF94A3B8),
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(12.dp))

                        FilledTonalButton(
                            onClick = onToggleLocalEngine,
                            colors = ButtonDefaults.filledTonalButtonColors(
                                containerColor = if (media.isLocalEngine && media.isPlaying) {
                                    HonorCyan.copy(alpha = 0.22f)
                                } else {
                                    Color.White.copy(alpha = 0.10f)
                                },
                                contentColor = Color.White,
                            ),
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("toggle_local_engine_btn"),
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.GraphicEq,
                                contentDescription = null,
                                tint = HonorCyan,
                                modifier = Modifier.size(18.dp),
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = if (media.isLocalEngine && media.isPlaying) {
                                    "Pausar Audio Hi-Res de Prueba"
                                } else {
                                    "Probar Audio Hi-Res Local (Widgets & Bloqueo)"
                                },
                            )
                        }
                    }
                }
            }
        }

        // Active sessions switcher
        if (media.activeSessions.isNotEmpty()) {
            item {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        text = "Sesiones Activas Detectadas",
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onBackground,
                    )
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        items(media.activeSessions, key = { it.packageName }) { session ->
                            Surface(
                                shape = RoundedCornerShape(20.dp),
                                color = if (session.isSelected) {
                                    HonorCyan.copy(alpha = 0.18f)
                                } else {
                                    MaterialTheme.colorScheme.surfaceVariant
                                },
                                border = BorderStroke(
                                    1.dp,
                                    if (session.isSelected) HonorCyan else Color.White.copy(alpha = 0.14f),
                                ),
                                modifier = Modifier.clickable { onSelectSession(session.packageName) },
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size(10.dp)
                                            .clip(CircleShape)
                                            .background(session.service.accentColor),
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Column {
                                        Text(
                                            text = session.service.displayName,
                                            style = MaterialTheme.typography.labelLarge,
                                            color = Color.White,
                                        )
                                        Text(
                                            text = session.title,
                                            style = MaterialTheme.typography.labelSmall,
                                            color = Color(0xFF94A3B8),
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis,
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }

        // Android 16 + MagicOS 10 Quick Setup Checklist
        item {
            MagicGlassCard(highlightBorder = !state.hasListenerPermission) {
                Text(
                    text = "Integración Android 16 & MagicOS 10",
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurface,
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "Conecta Spotify, YouTube Music y Amazon Music con tu escritorio, pantalla de bloqueo y Ajustes Rápidos.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )

                Spacer(modifier = Modifier.height(14.dp))

                ChecklistRow(
                    title = "Sincronización Multimedia Universal",
                    subtitle = if (state.hasListenerPermission) {
                        "Activo • Leyendo sesiones de Spotify, YT Music y Amazon"
                    } else {
                        "Requiere permiso de acceso a notificaciones multimedia"
                    },
                    isReady = state.hasListenerPermission,
                    actionLabel = if (state.hasListenerPermission) "Listo" else "Activar",
                    onClick = onGrantListenerPermission,
                    testTag = "grant_listener_btn",
                )

                Spacer(modifier = Modifier.height(10.dp))

                ChecklistRow(
                    title = "Controles en Pantalla de Bloqueo",
                    subtitle = if (state.hasPostNotificationsPermission) {
                        "Permitido • Notificación MediaStyle Android 16 lista"
                    } else {
                        "Autoriza notificaciones para mostrar controles al bloquear"
                    },
                    isReady = state.hasPostNotificationsPermission,
                    actionLabel = if (state.hasPostNotificationsPermission) "Listo" else "Permitir",
                    onClick = onRequestPostNotifications,
                    testTag = "grant_notifications_btn",
                )

                Spacer(modifier = Modifier.height(16.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    Button(
                        onClick = onPinWidget,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = HonorCobalt,
                            contentColor = Color.White,
                        ),
                        modifier = Modifier
                            .weight(1f)
                            .testTag("home_pin_widget_btn"),
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Rounded.AddToHomeScreen,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp),
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Fijar Widget", maxLines = 1)
                    }

                    FilledTonalButton(
                        onClick = onAddQuickTile,
                        modifier = Modifier
                            .weight(1f)
                            .testTag("home_add_qs_tile_btn"),
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.Tune,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp),
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Ajustes Ráp.", maxLines = 1)
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                FilledTonalButton(
                    onClick = onNavigateToWidgets,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("home_open_studio_btn"),
                ) {
                    Icon(
                        imageVector = Icons.Rounded.Widgets,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp),
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Personalizar Estilo MagicOS 10 en Estudio")
                }
            }
        }
    }
}

@Composable
private fun ChecklistRow(
    title: String,
    subtitle: String,
    isReady: Boolean,
    actionLabel: String,
    onClick: () -> Unit,
    testTag: String,
) {
    Surface(
        shape = RoundedCornerShape(18.dp),
        color = Color.White.copy(alpha = 0.05f),
        border = BorderStroke(
            1.dp,
            if (isReady) HonorMint.copy(alpha = 0.4f) else HonorCyan.copy(alpha = 0.35f),
        ),
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .testTag(testTag),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(
                imageVector = if (isReady) Icons.Rounded.CheckCircle else Icons.Rounded.NotificationsActive,
                contentDescription = null,
                tint = if (isReady) HonorMint else HonorCyan,
            )
            Spacer(modifier = Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.labelLarge,
                    color = Color.White,
                )
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodyMedium,
                    color = Color(0xFF94A3B8),
                )
            }
            Spacer(modifier = Modifier.width(8.dp))
            Surface(
                shape = CircleShape,
                color = if (isReady) HonorMint.copy(alpha = 0.16f) else HonorCyan.copy(alpha = 0.2f),
            ) {
                Text(
                    text = actionLabel,
                    style = MaterialTheme.typography.labelSmall,
                    color = if (isReady) HonorMint else HonorCyan,
                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                )
            }
        }
    }
}
