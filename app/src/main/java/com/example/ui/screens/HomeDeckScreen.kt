package com.example.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
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
import androidx.compose.material.icons.rounded.GraphicEq
import androidx.compose.material.icons.rounded.HealthAndSafety
import androidx.compose.material.icons.rounded.MoreVert
import androidx.compose.material.icons.rounded.MusicNote
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.R
import com.example.core.media.HealthLight
import com.example.data.ActiveSessionInfo
import com.example.data.MusicServiceApp
import com.example.data.TrackHistoryEntry
import com.example.designsystem.MagicTheme
import com.example.ui.MagicMusicUiState
import com.example.ui.components.AnimatedEqualizerBars
import com.example.ui.components.CoverGridItem
import com.example.ui.components.MagicAppIconBadge
import com.example.ui.components.MagicFilterChip
import com.example.ui.components.MagicHorizontalCoverGrid
import com.example.ui.components.MagicSearchBarPill
import com.example.ui.components.MagicSurfaceCard
import java.util.Calendar

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun HomeDeckScreen(
    state: MagicMusicUiState,
    onSearchQueryChange: (String) -> Unit,
    onSelectSession: (String) -> Unit,
    onOpenSessionMenu: (ActiveSessionInfo) -> Unit,
    onToggleLocalEngine: () -> Unit,
    onOpenNowPlaying: () -> Unit,
    onOpenSystemHealth: () -> Unit,
    onOpenServices: () -> Unit,
    onOpenActiveApp: (String) -> Unit,
    onPlayRecentTrack: (TrackHistoryEntry) -> Unit,
) {
    val tokens = MagicTheme.tokens
    val colors = MaterialTheme.colorScheme
    val media = state.media
    var selectedFilterId by remember { mutableStateOf("all") }

    val greeting = rememberTimeGreeting()
    val iconLookup = remember(state.installedServices) {
        state.installedServices.associateBy({ it.packageName }, { it.appIcon })
    }

    val query = state.searchQuery.trim()
    val filteredSessions = remember(media.activeSessions, selectedFilterId, query) {
        media.activeSessions.filter { session ->
            val matchesChip = when (selectedFilterId) {
                "all" -> true
                "local" -> session.service == MusicServiceApp.LOCAL_ENGINE
                else -> session.packageName == selectedFilterId
            }
            val matchesQuery = query.isEmpty() ||
                session.title.contains(query, ignoreCase = true) ||
                session.artist.contains(query, ignoreCase = true) ||
                session.appLabel.contains(query, ignoreCase = true)
            matchesChip && matchesQuery
        }
    }

    val recentHistory = remember(state.listeningStats.recentTracks, selectedFilterId, query) {
        state.listeningStats.recentTracks.filter { entry ->
            val matchesChip = when (selectedFilterId) {
                "all" -> true
                "local" -> entry.service == MusicServiceApp.LOCAL_ENGINE
                else -> entry.packageName == selectedFilterId
            }
            val matchesQuery = query.isEmpty() ||
                entry.title.contains(query, ignoreCase = true) ||
                entry.artist.contains(query, ignoreCase = true) ||
                entry.service.displayName.contains(query, ignoreCase = true)
            matchesChip && matchesQuery
        }
    }

    val coverGridItems = remember(recentHistory, media) {
        recentHistory.map { entry ->
            val activeArt = if (entry.packageName == media.packageName && entry.title == media.title) {
                media.albumArt
            } else {
                null
            }
            CoverGridItem(
                id = entry.id,
                title = entry.title,
                subtitle = "${entry.artist} • ${entry.service.displayName}",
                accentTint = entry.service.accentColor,
                bitmap = activeArt,
                packageName = entry.packageName,
            )
        }
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .testTag("home_deck_list"),
        contentPadding = PaddingValues(
            start = tokens.spacing.screenMargin,
            end = tokens.spacing.screenMargin,
            top = tokens.spacing.sm,
            bottom = tokens.spacing.lg,
        ),
        verticalArrangement = Arrangement.spacedBy(tokens.spacing.md),
    ) {
        // 1. Neutral time-of-day greeting + subtle active badge
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = greeting,
                    style = MaterialTheme.typography.headlineMedium,
                    color = colors.onBackground,
                    modifier = Modifier.testTag("home_greeting_text"),
                )
                FilledTonalButton(
                    onClick = onToggleLocalEngine,
                    contentPadding = PaddingValues(horizontal = 14.dp, vertical = 6.dp),
                    modifier = Modifier.testTag("home_local_engine_chip"),
                ) {
                    Icon(
                        imageVector = Icons.Rounded.GraphicEq,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp),
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = if (media.isLocalEngine && media.isPlaying) "Motor Hi-Res activo" else "Motor Local",
                        style = MaterialTheme.typography.labelLarge,
                    )
                }
            }
        }

        // 2. Editable Material 3 search pill matching reference #01
        item {
            MagicSearchBarPill(
                query = state.searchQuery,
                placeholder = stringResource(R.string.search_placeholder),
                onClick = {},
                onMicClick = onOpenServices,
                onQueryChange = onSearchQueryChange,
            )
        }

        // 3. Dynamic filter chips: Todo + each active session service + Local
        item {
            LazyRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.fillMaxWidth(),
            ) {
                item(key = "chip_all") {
                    MagicFilterChip(
                        label = stringResource(R.string.chip_all),
                        selected = selectedFilterId == "all",
                        onClick = { selectedFilterId = "all" },
                        modifier = Modifier.testTag("home_chip_all"),
                    )
                }
                items(
                    items = media.activeSessions.distinctBy { it.packageName },
                    key = { "chip_${it.packageName}" },
                ) { session ->
                    MagicFilterChip(
                        label = session.appLabel,
                        selected = selectedFilterId == session.packageName,
                        onClick = {
                            selectedFilterId = session.packageName
                            onSelectSession(session.packageName)
                        },
                    )
                }
                if (media.activeSessions.none { it.service == MusicServiceApp.LOCAL_ENGINE }) {
                    item(key = "chip_local") {
                        MagicFilterChip(
                            label = stringResource(R.string.chip_local),
                            selected = selectedFilterId == "local",
                            onClick = { selectedFilterId = "local" },
                            leadingIcon = Icons.Rounded.MusicNote,
                        )
                    }
                }
            }
        }

        // 4. Pending system health banner (only visible when there are amber/red items)
        if (state.systemHealth.hasPendingIssues) {
            item {
                val isRed = state.systemHealth.overallLight == HealthLight.RED
                val badgeTint = if (isRed) colors.error else tokens.semantic.warning
                MagicSurfaceCard(
                    highlightBorder = isRed,
                    containerColor = tokens.semantic.elevatedCardSurface,
                    modifier = Modifier.testTag("home_health_banner"),
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Box(
                            modifier = Modifier
                                .size(38.dp)
                                .clip(CircleShape)
                                .background(badgeTint.copy(alpha = 0.16f)),
                            contentAlignment = Alignment.Center,
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.HealthAndSafety,
                                contentDescription = null,
                                tint = badgeTint,
                            )
                        }
                        Spacer(modifier = Modifier.width(12.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "${state.systemHealth.pendingCount} ajustes pendientes",
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.SemiBold,
                                color = colors.onSurface,
                            )
                            Text(
                                text = "Revisa permisos y batería para widgets estables",
                                style = MaterialTheme.typography.bodySmall,
                                color = colors.onSurfaceVariant,
                            )
                        }
                        FilledTonalButton(
                            onClick = onOpenSystemHealth,
                            modifier = Modifier.testTag("home_health_fix_btn"),
                        ) {
                            Text("Revisar")
                        }
                    }
                }
            }
        }

        // 5. Active sessions section (tap to switch, long-press / menu for options)
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = "Sesiones activas",
                    style = MaterialTheme.typography.titleLarge,
                    color = colors.onBackground,
                )
                FilledTonalButton(
                    onClick = onOpenServices,
                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp),
                ) {
                    Icon(
                        imageVector = Icons.Rounded.Settings,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp),
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Servicios", style = MaterialTheme.typography.labelLarge)
                }
            }
        }

        if (filteredSessions.isNotEmpty()) {
            items(filteredSessions, key = { it.packageName }) { session ->
                Surface(
                    shape = RoundedCornerShape(tokens.radii.card),
                    color = if (session.isSelected) {
                        tokens.semantic.selectedContainer.copy(alpha = 0.55f)
                    } else {
                        tokens.semantic.cardSurface
                    },
                    border = BorderStroke(
                        width = if (session.isSelected) 1.4.dp else 1.dp,
                        color = if (session.isSelected) session.service.accentColor.copy(alpha = 0.85f) else tokens.semantic.divider,
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(tokens.radii.card))
                        .combinedClickable(
                            onClick = {
                                if (session.isSelected) {
                                    onOpenNowPlaying()
                                } else {
                                    onSelectSession(session.packageName)
                                }
                            },
                            onLongClick = { onOpenSessionMenu(session) },
                        )
                        .testTag("active_session_${session.service.id}"),
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 14.dp, vertical = 12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        MagicAppIconBadge(
                            drawable = iconLookup[session.packageName],
                            fallbackTint = session.service.accentColor,
                            contentDescription = session.appLabel,
                            size = 46.dp,
                            cornerRadius = 14.dp,
                            service = session.service,
                            serviceName = session.appLabel,
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                            ) {
                                Text(
                                    text = session.appLabel,
                                    style = MaterialTheme.typography.labelMedium,
                                    fontWeight = FontWeight.SemiBold,
                                    color = session.service.accentColor,
                                )
                                if (session.isSelected) {
                                    Surface(
                                        shape = CircleShape,
                                        color = colors.primary.copy(alpha = 0.18f),
                                    ) {
                                        Text(
                                            text = "Activa",
                                            style = MaterialTheme.typography.labelSmall,
                                            color = colors.primary,
                                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp),
                                        )
                                    }
                                }
                            }
                            Text(
                                text = session.title.ifBlank { "Sin título de pista" },
                                style = MaterialTheme.typography.titleMedium,
                                color = colors.onSurface,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                            )
                            Text(
                                text = session.artist.ifBlank { session.service.subtitle },
                                style = MaterialTheme.typography.bodySmall,
                                color = colors.onSurfaceVariant,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                            )
                        }
                        if (session.isPlaying) {
                            AnimatedEqualizerBars(
                                color = session.service.accentColor,
                                modifier = Modifier.padding(horizontal = 6.dp),
                            )
                        }
                        IconButton(onClick = { onOpenSessionMenu(session) }) {
                            Icon(
                                imageVector = Icons.Rounded.MoreVert,
                                contentDescription = stringResource(R.string.action_more_options),
                                tint = colors.onSurfaceVariant,
                            )
                        }
                    }
                }
            }
        } else {
            item {
                HomeEmptySessionsCard(
                    hasQuery = query.isNotBlank(),
                    onStartLocal = onToggleLocalEngine,
                    onOpenServices = onOpenServices,
                )
            }
        }

        // 6. Recently played horizontal cover grid backed by real local history
        if (coverGridItems.isNotEmpty()) {
            item {
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "Escuchado recientemente",
                    style = MaterialTheme.typography.titleLarge,
                    color = colors.onBackground,
                )
            }
            item {
                MagicHorizontalCoverGrid(
                    items = coverGridItems,
                    horizontalPadding = 0.dp,
                    onItemClick = { gridItem ->
                        val entry = recentHistory.firstOrNull { it.id == gridItem.id }
                        if (entry != null) {
                            onPlayRecentTrack(entry)
                        } else if (gridItem.packageName.isNotBlank()) {
                            onOpenActiveApp(gridItem.packageName)
                        }
                    },
                )
            }
        }
    }
}

@Composable
private fun HomeEmptySessionsCard(
    hasQuery: Boolean,
    onStartLocal: () -> Unit,
    onOpenServices: () -> Unit,
) {
    val colors = MaterialTheme.colorScheme
    MagicSurfaceCard(modifier = Modifier.testTag("home_empty_sessions_card")) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 8.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            Box(
                modifier = Modifier
                    .size(56.dp)
                    .clip(CircleShape)
                    .background(colors.primary.copy(alpha = 0.16f)),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    imageVector = Icons.Rounded.MusicNote,
                    contentDescription = null,
                    tint = colors.primary,
                    modifier = Modifier.size(28.dp),
                )
            }
            Text(
                text = if (hasQuery) "No hay coincidencias con tu búsqueda" else "Sin reproducción externa activa",
                style = MaterialTheme.typography.titleMedium,
                color = colors.onSurface,
            )
            Text(
                text = "Abre Spotify, YouTube Music, Amazon Music u otra app, o inicia el Motor Local Hi-Res para probar controles y widgets.",
                style = MaterialTheme.typography.bodySmall,
                color = colors.onSurfaceVariant,
            )
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                Button(
                    onClick = onStartLocal,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = colors.primary,
                        contentColor = colors.onPrimary,
                    ),
                    modifier = Modifier
                        .weight(1f)
                        .testTag("home_start_local_btn"),
                ) {
                    Icon(Icons.Rounded.PlayArrow, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Motor Local")
                }
                FilledTonalButton(
                    onClick = onOpenServices,
                    modifier = Modifier.weight(1f),
                ) {
                    Text("Abrir app")
                }
            }
        }
    }
}

@Composable
private fun rememberTimeGreeting(): String {
    val hour = remember { Calendar.getInstance().get(Calendar.HOUR_OF_DAY) }
    return when (hour) {
        in 6..11 -> stringResource(R.string.greeting_morning)
        in 12..19 -> stringResource(R.string.greeting_afternoon)
        else -> stringResource(R.string.greeting_evening)
    }
}
