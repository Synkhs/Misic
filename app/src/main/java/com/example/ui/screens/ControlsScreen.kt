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
import androidx.compose.material.icons.rounded.Bedtime
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.Favorite
import androidx.compose.material.icons.rounded.Info
import androidx.compose.material.icons.rounded.Lock
import androidx.compose.material.icons.rounded.MusicNote
import androidx.compose.material.icons.rounded.Pause
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.Repeat
import androidx.compose.material.icons.rounded.Shuffle
import androidx.compose.material.icons.rounded.SkipNext
import androidx.compose.material.icons.rounded.SkipPrevious
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.PrimaryTabRow
import androidx.compose.material3.Surface
import androidx.compose.material3.Tab
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.R
import com.example.core.compat.AndroidVersionCompat
import com.example.data.CustomControlAction
import com.example.data.LockPrivacyLevel
import com.example.data.QsTileMode
import com.example.data.QuickTileType
import com.example.designsystem.MagicTheme
import com.example.quicksettings.TileStateCalculator
import com.example.ui.MagicMusicUiState
import com.example.ui.components.MagicSettingsSwitchRow
import com.example.ui.components.MagicSurfaceCard
import com.example.ui.components.WidgetArtworkThumb
import com.example.ui.components.customActionIcon

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun ControlsScreen(
    state: MagicMusicUiState,
    initialTab: Int = 0,
    onToggleBridge: (Boolean) -> Unit,
    onToggleLockVisibility: (Boolean) -> Unit,
    onToggleLockArtwork: (Boolean) -> Unit,
    onSelectLockPrivacyLevel: (LockPrivacyLevel) -> Unit,
    onSelectLockAutoCloseSeconds: (Int) -> Unit,
    onToggleCompactActionIndex: (Int) -> Unit,
    onDismissNotifyChannelGuide: () -> Unit,
    onToggleRequireUnlockToOpenApp: (Boolean) -> Unit,
    onToggleRequireUnlockToSwitchSource: (Boolean) -> Unit,
    onToggleRequireUnlockForCustomActions: (Boolean) -> Unit,
    onSelectLockAction1: (CustomControlAction) -> Unit,
    onSelectLockAction2: (CustomControlAction) -> Unit,
    onSelectQsTileMode: (QsTileMode) -> Unit,
    onRequestAddQuickTile: (QuickTileType) -> Unit,
    onOpenLockDeck: () -> Unit,
    onPlayPause: () -> Unit,
    onNext: () -> Unit,
    onPrevious: () -> Unit,
) {
    var selectedTab by rememberSaveable(initialTab) { mutableIntStateOf(initialTab.coerceIn(0, 1)) }
    val cfg = state.lockAndQsConfig
    val media = state.media
    val tokens = MagicTheme.tokens
    val colors = MaterialTheme.colorScheme
    val supportsOneTapTile = AndroidVersionCompat.supportsRequestAddTileService()

    Column(modifier = Modifier.fillMaxSize()) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = tokens.spacing.screenMargin, vertical = tokens.spacing.xs),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Text(
                text = stringResource(R.string.nav_controls),
                style = MaterialTheme.typography.headlineMedium,
                color = colors.onBackground,
            )

            PrimaryTabRow(
                selectedTabIndex = selectedTab,
                containerColor = Color.Transparent,
            ) {
                Tab(
                    selected = selectedTab == 0,
                    onClick = { selectedTab = 0 },
                    text = { Text(stringResource(R.string.nav_lock)) },
                    modifier = Modifier.testTag("controls_tab_lock"),
                )
                Tab(
                    selected = selectedTab == 1,
                    onClick = { selectedTab = 1 },
                    text = { Text(stringResource(R.string.nav_quick_settings)) },
                    modifier = Modifier.testTag("controls_tab_qs"),
                )
            }
        }

        LazyColumn(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .testTag("lock_qs_list"),
            contentPadding = PaddingValues(
                start = tokens.spacing.screenMargin,
                end = tokens.spacing.screenMargin,
                top = tokens.spacing.sm,
                bottom = tokens.spacing.lg,
            ),
            verticalArrangement = Arrangement.spacedBy(tokens.spacing.md),
        ) {
            if (selectedTab == 0) {
                // Silent channel notice (first-use)
                if (!cfg.notifyChannelGuideShown) {
                    item {
                        MagicSurfaceCard(highlightBorder = true) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(10.dp),
                            ) {
                                Icon(Icons.Rounded.Info, contentDescription = null, tint = colors.primary)
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = "Notificación silenciosa MediaStyle",
                                        style = MaterialTheme.typography.titleSmall,
                                        color = colors.onSurface,
                                    )
                                    Text(
                                        text = "Usa un canal de baja importancia sin sonido ni vibración y se detiene automáticamente al terminar tu música.",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = colors.onSurfaceVariant,
                                    )
                                }
                                TextButton(onClick = onDismissNotifyChannelGuide) {
                                    Text("Entendido")
                                }
                            }
                        }
                    }
                }

                // Lock screen simulation & MediaStyle bridge
                item {
                    Surface(
                        shape = RoundedCornerShape(tokens.radii.sheet),
                        color = Color(0xFF0A0D16),
                        border = BorderStroke(1.dp, colors.primary.copy(alpha = 0.45f)),
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        Column(
                            modifier = Modifier
                                .background(
                                    Brush.verticalGradient(
                                        listOf(
                                            colors.primary.copy(alpha = 0.22f),
                                            Color(0xFF0A0D16),
                                        ),
                                    ),
                                )
                                .padding(18.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.Lock,
                                contentDescription = null,
                                tint = colors.primary,
                                modifier = Modifier.size(18.dp),
                            )
                            Text(
                                text = "12:57",
                                style = MaterialTheme.typography.displayMedium,
                                fontWeight = FontWeight.Bold,
                                color = Color.White,
                            )
                            Text(
                                text = when {
                                    !cfg.showOnLockScreen || cfg.lockPrivacyLevel == LockPrivacyLevel.SECRET ->
                                        "Oculto en pantalla de bloqueo"
                                    cfg.lockPrivacyLevel == LockPrivacyLevel.HIDE_SENSITIVE ->
                                        "Modo privado: carátula y título ocultos al bloquear"
                                    else -> "MediaStyle activo sobre bloqueo"
                                },
                                style = MaterialTheme.typography.bodySmall,
                                color = Color.White.copy(alpha = 0.72f),
                            )

                            Spacer(modifier = Modifier.height(14.dp))

                            Surface(
                                shape = RoundedCornerShape(22.dp),
                                color = Color(0xFF1A1E2B).copy(alpha = 0.94f),
                                border = BorderStroke(1.dp, Color.White.copy(alpha = 0.14f)),
                                modifier = Modifier.fillMaxWidth(),
                            ) {
                                Column(modifier = Modifier.padding(14.dp)) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        if (cfg.showArtworkOnLockScreen && cfg.lockPrivacyLevel == LockPrivacyLevel.PUBLIC) {
                                            WidgetArtworkThumb(media = media, size = 46.dp, radius = 12.dp)
                                            Spacer(modifier = Modifier.width(12.dp))
                                        }
                                        Column(modifier = Modifier.weight(1f)) {
                                            Text(
                                                text = when {
                                                    cfg.lockPrivacyLevel != LockPrivacyLevel.PUBLIC -> "Contenido multimedia privado"
                                                    media.hasActiveMedia -> media.title
                                                    else -> "Sin música activa"
                                                },
                                                style = MaterialTheme.typography.titleMedium,
                                                color = Color.White,
                                                maxLines = 1,
                                                overflow = TextOverflow.Ellipsis,
                                            )
                                            Text(
                                                text = if (cfg.lockPrivacyLevel != LockPrivacyLevel.PUBLIC) {
                                                    "Desbloquea para ver detalles"
                                                } else {
                                                    "${media.artist} • ${media.service.displayName}"
                                                },
                                                style = MaterialTheme.typography.bodySmall,
                                                color = Color.White.copy(alpha = 0.72f),
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
                                            tint = colors.primary,
                                        )
                                        IconButton(onClick = onPrevious) {
                                            Icon(Icons.Rounded.SkipPrevious, contentDescription = "Anterior", tint = Color.White)
                                        }
                                        IconButton(onClick = onPlayPause) {
                                            Icon(
                                                imageVector = if (media.isPlaying) Icons.Rounded.Pause else Icons.Rounded.PlayArrow,
                                                contentDescription = "Play/Pausa",
                                                tint = Color.White,
                                            )
                                        }
                                        IconButton(onClick = onNext) {
                                            Icon(Icons.Rounded.SkipNext, contentDescription = "Siguiente", tint = Color.White)
                                        }
                                        Icon(
                                            imageVector = customActionIcon(cfg.lockScreenAction2),
                                            contentDescription = cfg.lockScreenAction2.label,
                                            tint = colors.primary,
                                        )
                                    }
                                }
                            }

                            Spacer(modifier = Modifier.height(12.dp))

                            FilledTonalButton(
                                onClick = onOpenLockDeck,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .testTag("open_lock_deck_preview_btn"),
                            ) {
                                Text("Abrir Panel en Pantalla de Bloqueo")
                            }
                        }
                    }
                }

                // Lock screen configuration & privacy levels
                item {
                    MagicSurfaceCard {
                        Text(
                            text = "Notificación y Privacidad en Bloqueo",
                            style = MaterialTheme.typography.titleMedium,
                            color = colors.onSurface,
                        )
                        Spacer(modifier = Modifier.height(10.dp))

                        MagicSettingsSwitchRow(
                            title = "Puente de notificación MediaStyle",
                            subtitle = if (state.bridgeRunning) "Servicio foreground activo" else "Solo activo durante reproducción",
                            checked = cfg.bridgeEnabled,
                            onCheckedChange = onToggleBridge,
                            switchTestTag = "bridge_enable_switch",
                        )
                        MagicSettingsSwitchRow(
                            title = "Visible en pantalla de bloqueo",
                            subtitle = "Permite abrir controles y widgets sobre el bloqueo",
                            checked = cfg.showOnLockScreen,
                            onCheckedChange = onToggleLockVisibility,
                        )
                        MagicSettingsSwitchRow(
                            title = "Mostrar carátula en bloqueo",
                            subtitle = "Muestra la portada del álbum cuando el nivel es público",
                            checked = cfg.showArtworkOnLockScreen,
                            onCheckedChange = onToggleLockArtwork,
                        )

                        Spacer(modifier = Modifier.height(10.dp))
                        Text(
                            text = "Nivel de privacidad en bloqueo",
                            style = MaterialTheme.typography.labelLarge,
                            color = colors.onSurfaceVariant,
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            LockPrivacyLevel.entries.forEach { level ->
                                val selected = cfg.lockPrivacyLevel == level
                                Surface(
                                    onClick = { onSelectLockPrivacyLevel(level) },
                                    shape = RoundedCornerShape(tokens.radii.listItem),
                                    color = if (selected) tokens.semantic.selectedContainer.copy(alpha = 0.65f) else colors.surfaceContainerLow,
                                    border = BorderStroke(
                                        width = if (selected) 1.5.dp else 1.dp,
                                        color = if (selected) colors.primary else tokens.semantic.divider,
                                    ),
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .testTag("lock_privacy_${level.name.lowercase()}"),
                                ) {
                                    Column(modifier = Modifier.padding(12.dp)) {
                                        Text(
                                            text = level.label,
                                            style = MaterialTheme.typography.titleSmall,
                                            color = colors.onSurface,
                                        )
                                        Text(
                                            text = level.description,
                                            style = MaterialTheme.typography.bodySmall,
                                            color = colors.onSurfaceVariant,
                                        )
                                    }
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(12.dp))
                        Text(
                            text = "Cierre automático del panel tras inactividad",
                            style = MaterialTheme.typography.labelLarge,
                            color = colors.onSurfaceVariant,
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            listOf(0 to "Nunca", 15 to "15 s", 30 to "30 s", 60 to "60 s").forEach { (sec, label) ->
                                FilterChip(
                                    selected = cfg.lockAutoCloseSeconds == sec,
                                    onClick = { onSelectLockAutoCloseSeconds(sec) },
                                    label = { Text(label) },
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(10.dp))
                        MagicSettingsSwitchRow(
                            title = "Pedir desbloqueo al abrir la app de origen",
                            subtitle = "Usa biometría, PIN o patrón antes de saltar a la app",
                            checked = cfg.requireUnlockToOpenApp,
                            onCheckedChange = onToggleRequireUnlockToOpenApp,
                        )
                        MagicSettingsSwitchRow(
                            title = "Pedir desbloqueo al cambiar de reproductor",
                            subtitle = "Evita cambios accidentales de sesión desde el bolsillo",
                            checked = cfg.requireUnlockToSwitchSource,
                            onCheckedChange = onToggleRequireUnlockToSwitchSource,
                        )
                        MagicSettingsSwitchRow(
                            title = "Pedir desbloqueo en acciones rápidas",
                            subtitle = "Protege Me gusta, Aleatorio y Repetir en bloqueo",
                            checked = cfg.requireUnlockForCustomActions,
                            onCheckedChange = onToggleRequireUnlockForCustomActions,
                        )
                    }
                }

                // Custom actions & compact notification actions
                item {
                    MagicSurfaceCard {
                        Text(
                            text = "Acciones personalizadas y notificación compacta",
                            style = MaterialTheme.typography.titleMedium,
                            color = colors.onSurface,
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "Botón rápido 1",
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
                                    selected = cfg.lockScreenAction1 == action,
                                    onClick = { onSelectLockAction1(action) },
                                    label = { Text(action.shortLabel) },
                                    colors = FilterChipDefaults.filterChipColors(
                                        selectedContainerColor = colors.primaryContainer,
                                        selectedLabelColor = colors.onPrimaryContainer,
                                    ),
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(12.dp))
                        Text(
                            text = "Botón rápido 2",
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
                                    selected = cfg.lockScreenAction2 == action,
                                    onClick = { onSelectLockAction2(action) },
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
                            text = "Botones visibles en notificación compacta (máx. 3)",
                            style = MaterialTheme.typography.labelLarge,
                            color = colors.onSurfaceVariant,
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        val actionNames = listOf(
                            0 to cfg.lockScreenAction1.shortLabel,
                            1 to "Anterior",
                            2 to "Play/Pausa",
                            3 to "Siguiente",
                            4 to cfg.lockScreenAction2.shortLabel,
                        )
                        FlowRow(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalArrangement = Arrangement.spacedBy(6.dp),
                        ) {
                            actionNames.forEach { (index, title) ->
                                val selected = cfg.compactActionIndices.contains(index)
                                FilterChip(
                                    selected = selected,
                                    onClick = { onToggleCompactActionIndex(index) },
                                    label = { Text(title) },
                                    leadingIcon = if (selected) {
                                        { Icon(Icons.Rounded.Check, contentDescription = null, modifier = Modifier.size(16.dp)) }
                                    } else {
                                        null
                                    },
                                )
                            }
                        }
                    }
                }
            } else {
                // Quick Settings Tab: 7 Native Tiles
                item {
                    MagicSurfaceCard {
                        Text(
                            text = "7 Mosaicos en Ajustes Rápidos",
                            style = MaterialTheme.typography.titleMedium,
                            color = colors.onSurface,
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = if (supportsOneTapTile) {
                                "En Android 13–16 añade cualquiera de los 7 mosaicos con un toque. Mantén pulsado un mosaico en la barra superior para abrir este panel."
                            } else {
                                "En Android 8–12 desliza dos veces el panel superior, pulsa el icono de Editar (lápiz) y arrastra los mosaicos de MagicMusic."
                            },
                            style = MaterialTheme.typography.bodyMedium,
                            color = colors.onSurfaceVariant,
                        )

                        Spacer(modifier = Modifier.height(14.dp))

                        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                            QuickTileType.entries.forEach { type ->
                                val spec = TileStateCalculator.compute(
                                    type = type,
                                    media = media,
                                    listenerState = state.listenerState,
                                    sleepTimer = state.sleepTimer,
                                    qsTileMode = cfg.qsTileMode,
                                )
                                val isAdded = cfg.qsAddedTileKeys.contains(type.key)
                                QsTileManageRow(
                                    title = spec.label,
                                    subtitle = spec.subtitle,
                                    isActive = spec.tileState == TileStateCalculator.STATE_ACTIVE,
                                    isUnavailable = spec.tileState == TileStateCalculator.STATE_UNAVAILABLE,
                                    isAdded = isAdded,
                                    supportsOneTap = supportsOneTapTile,
                                    icon = iconForQuickTile(type),
                                    onAddClick = { onRequestAddQuickTile(type) },
                                    testTag = "add_qs_tile_${type.key}",
                                )
                            }
                        }
                    }
                }

                item {
                    MagicSurfaceCard {
                        Text(
                            text = "Acción al pulsar el mosaico 'Música'",
                            style = MaterialTheme.typography.titleMedium,
                            color = colors.onSurface,
                        )
                        Spacer(modifier = Modifier.height(10.dp))

                        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            QsTileMode.entries.forEach { mode ->
                                val selected = cfg.qsTileMode == mode
                                Surface(
                                    shape = RoundedCornerShape(tokens.radii.listItem),
                                    color = if (selected) tokens.semantic.selectedContainer.copy(alpha = 0.65f) else colors.surfaceContainerLow,
                                    border = BorderStroke(
                                        width = if (selected) 1.5.dp else 1.dp,
                                        color = if (selected) colors.primary else tokens.semantic.divider,
                                    ),
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clickable { onSelectQsTileMode(mode) },
                                ) {
                                    Column(modifier = Modifier.padding(14.dp)) {
                                        Text(
                                            text = mode.label,
                                            style = MaterialTheme.typography.titleSmall,
                                            color = colors.onSurface,
                                        )
                                        Text(
                                            text = mode.description,
                                            style = MaterialTheme.typography.bodySmall,
                                            color = colors.onSurfaceVariant,
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun QsTileManageRow(
    title: String,
    subtitle: String,
    isActive: Boolean,
    isUnavailable: Boolean,
    isAdded: Boolean,
    supportsOneTap: Boolean,
    icon: ImageVector,
    onAddClick: () -> Unit,
    testTag: String,
) {
    val colors = MaterialTheme.colorScheme
    val tokens = MagicTheme.tokens
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        Surface(
            shape = CircleShape,
            color = when {
                isActive -> colors.primary
                isUnavailable -> colors.surfaceContainerLowest
                else -> colors.surfaceContainerHigh
            },
            border = BorderStroke(1.dp, tokens.semantic.divider),
            modifier = Modifier
                .weight(1f)
                .height(60.dp),
        ) {
            Row(
                modifier = Modifier.padding(horizontal = 14.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = when {
                        isActive -> colors.onPrimary
                        isUnavailable -> colors.onSurfaceVariant.copy(alpha = 0.5f)
                        else -> colors.primary
                    },
                    modifier = Modifier.size(22.dp),
                )
                Spacer(modifier = Modifier.width(10.dp))
                Column {
                    Text(
                        text = title,
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.SemiBold,
                        color = if (isActive) colors.onPrimary else colors.onSurface,
                        maxLines = 1,
                    )
                    Text(
                        text = subtitle,
                        style = MaterialTheme.typography.bodySmall,
                        color = if (isActive) colors.onPrimary.copy(alpha = 0.85f) else colors.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
            }
        }

        if (isAdded) {
            FilledTonalButton(
                onClick = onAddClick,
                modifier = Modifier.testTag(testTag),
            ) {
                Icon(Icons.Rounded.Check, contentDescription = null, modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(4.dp))
                Text("Añadido")
            }
        } else {
            Button(
                onClick = onAddClick,
                colors = ButtonDefaults.buttonColors(
                    containerColor = colors.primary,
                    contentColor = colors.onPrimary,
                ),
                modifier = Modifier.testTag(testTag),
            ) {
                Icon(Icons.Rounded.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(4.dp))
                Text(if (supportsOneTap) "Añadir" else "Guía")
            }
        }
    }
}

private fun iconForQuickTile(type: QuickTileType): ImageVector = when (type) {
    QuickTileType.PLAY_PAUSE -> Icons.Rounded.MusicNote
    QuickTileType.NEXT -> Icons.Rounded.SkipNext
    QuickTileType.PREVIOUS -> Icons.Rounded.SkipPrevious
    QuickTileType.SHUFFLE -> Icons.Rounded.Shuffle
    QuickTileType.REPEAT -> Icons.Rounded.Repeat
    QuickTileType.FAVORITE -> Icons.Rounded.Favorite
    QuickTileType.SLEEP_TIMER -> Icons.Rounded.Bedtime
}
