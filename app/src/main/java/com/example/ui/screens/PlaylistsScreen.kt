package com.example.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
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
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.OpenInNew
import androidx.compose.material.icons.automirrored.rounded.PlaylistAdd
import androidx.compose.material.icons.automirrored.rounded.QueueMusic
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.ArrowDownward
import androidx.compose.material.icons.rounded.ArrowUpward
import androidx.compose.material.icons.rounded.DeleteOutline
import androidx.compose.material.icons.rounded.Edit
import androidx.compose.material.icons.rounded.Favorite
import androidx.compose.material.icons.rounded.History
import androidx.compose.material.icons.rounded.LibraryMusic
import androidx.compose.material.icons.rounded.NewReleases
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material.icons.rounded.Shuffle
import androidx.compose.material.icons.rounded.TrendingUp
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.BottomSheetDefaults
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.core.accounts.MusicSourceId
import com.example.core.accounts.SourceAccountSnapshot
import com.example.core.artwork.LocalArtworkRequest
import com.example.core.library.LocalTrackEntity
import com.example.core.library.PlaylistWithSummary
import com.example.core.library.codecLabel
import com.example.designsystem.MagicTheme
import com.example.designsystem.expressive.MagicFabMenu
import com.example.designsystem.expressive.MagicFabMenuAction
import com.example.designsystem.expressive.MagicFlexibleMediumTopBar
import com.example.ui.components.MagicAlbumArtwork
import com.example.ui.components.MagicSurfaceCard
import com.example.ui.components.MagicTrackListItem
import com.example.ui.components.TrackItemVariant
import com.example.ui.components.TrackListItemModel
import com.example.ui.components.formatTimeMs
import kotlinx.coroutines.flow.Flow

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PlaylistsScreen(
    playlists: List<PlaylistWithSummary>,
    mostPlayed: List<LocalTrackEntity>,
    recentlyAdded: List<LocalTrackEntity>,
    favorites: List<LocalTrackEntity>,
    recentlyPlayed: List<LocalTrackEntity>,
    spotifySnapshot: SourceAccountSnapshot,
    youtubeSnapshot: SourceAccountSnapshot,
    observePlaylistTracks: (Long) -> Flow<List<LocalTrackEntity>>,
    onPlaySmartPlaylist: (kind: String, shuffle: Boolean) -> Unit,
    onPlayPlaylist: (playlistId: Long, shuffle: Boolean) -> Unit,
    onPlayLocalTracks: (List<LocalTrackEntity>, Int, Boolean) -> Unit,
    onCreatePlaylist: (String) -> Unit,
    onRenamePlaylist: (Long, String) -> Unit,
    onDeletePlaylist: (Long) -> Unit,
    onRemoveTrackFromPlaylist: (Long, Long) -> Unit,
    onReorderPlaylistTracks: (Long, List<Long>) -> Unit,
    onScanLocalLibrary: () -> Unit,
    onLaunchAccountUri: (MusicSourceId, String?) -> Unit,
    onOpenServices: () -> Unit,
) {
    val tokens = MagicTheme.tokens
    val colors = MaterialTheme.colorScheme
    val scrollBehavior = TopAppBarDefaults.exitUntilCollapsedScrollBehavior()

    var fabExpanded by remember { mutableStateOf(false) }
    var showCreateDialog by remember { mutableStateOf(false) }
    var newNameDraft by remember { mutableStateOf("") }
    var renameTarget by remember { mutableStateOf<PlaylistWithSummary?>(null) }
    var renameDraft by remember { mutableStateOf("") }
    var activePlaylistSheet by remember { mutableStateOf<PlaylistWithSummary?>(null) }

    Scaffold(
        modifier = Modifier
            .fillMaxSize()
            .nestedScroll(scrollBehavior.nestedScrollConnection),
        containerColor = Color.Transparent,
        topBar = {
            MagicFlexibleMediumTopBar(
                title = "Listas",
                subtitle = "${playlists.size} listas locales • 4 colecciones inteligentes",
                useExpressiveM3 = tokens.useExpressiveM3,
                scrollBehavior = scrollBehavior,
            )
        },
        floatingActionButton = {
            MagicFabMenu(
                expanded = fabExpanded,
                useExpressiveM3 = tokens.useExpressiveM3,
                fabIcon = Icons.Rounded.Add,
                fabContentDescription = "Acciones de listas",
                onToggle = { fabExpanded = !fabExpanded },
                actions = listOf(
                    MagicFabMenuAction(
                        label = "Nueva lista",
                        icon = Icons.AutoMirrored.Rounded.PlaylistAdd,
                        testTag = "fab_new_playlist",
                        onClick = {
                            fabExpanded = false
                            newNameDraft = ""
                            showCreateDialog = true
                        },
                    ),
                    MagicFabMenuAction(
                        label = "Escanear carpetas",
                        icon = Icons.Rounded.Refresh,
                        testTag = "fab_scan_library",
                        onClick = {
                            fabExpanded = false
                            onScanLocalLibrary()
                        },
                    ),
                ),
            )
        },
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .testTag("playlists_screen_list"),
            contentPadding = PaddingValues(
                start = tokens.spacing.screenMargin,
                end = tokens.spacing.screenMargin,
                top = tokens.spacing.xs,
                bottom = 110.dp,
            ),
            verticalArrangement = Arrangement.spacedBy(tokens.spacing.md),
        ) {
            // Smart Playlists horizontal cards
            item {
                Text(
                    text = "Listas inteligentes",
                    style = MaterialTheme.typography.titleMedium,
                    color = colors.onBackground,
                )
            }
            item {
                val smartItems = listOf(
                    Triple("most_played", "Más escuchadas" to "${mostPlayed.size} pistas", Icons.Rounded.TrendingUp),
                    Triple("recent", "Recién añadidas" to "${recentlyAdded.size} pistas", Icons.Rounded.NewReleases),
                    Triple("favorites", "Favoritas" to "${favorites.size} pistas", Icons.Rounded.Favorite),
                    Triple("history", "Sesiones recientes" to "${recentlyPlayed.size} pistas", Icons.Rounded.History),
                )
                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    items(smartItems, key = { it.first }) { (kind, texts, icon) ->
                        Surface(
                            onClick = { onPlaySmartPlaylist(kind, false) },
                            shape = RoundedCornerShape(tokens.radii.card),
                            color = colors.surfaceContainerHigh,
                            border = BorderStroke(1.dp, tokens.semantic.divider),
                            modifier = Modifier
                                .width(172.dp)
                                .testTag("smart_playlist_$kind"),
                        ) {
                            Column(
                                modifier = Modifier.padding(14.dp),
                                verticalArrangement = Arrangement.spacedBy(8.dp),
                            ) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically,
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size(38.dp)
                                            .clip(RoundedCornerShape(12.dp))
                                            .background(colors.primaryContainer),
                                        contentAlignment = Alignment.Center,
                                    ) {
                                        Icon(icon, contentDescription = null, tint = colors.onPrimaryContainer)
                                    }
                                    IconButton(
                                        onClick = { onPlaySmartPlaylist(kind, true) },
                                        modifier = Modifier.size(34.dp),
                                    ) {
                                        Icon(
                                            Icons.Rounded.Shuffle,
                                            contentDescription = "Aleatorio ${texts.first}",
                                            tint = colors.primary,
                                            modifier = Modifier.size(18.dp),
                                        )
                                    }
                                }
                                Text(
                                    text = texts.first,
                                    style = MaterialTheme.typography.titleSmall,
                                    fontWeight = FontWeight.SemiBold,
                                    color = colors.onSurface,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                )
                                Text(
                                    text = texts.second,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = colors.onSurfaceVariant,
                                )
                            }
                        }
                    }
                }
            }

            // User-created local Room playlists
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        text = "Tus listas locales",
                        style = MaterialTheme.typography.titleMedium,
                        color = colors.onBackground,
                    )
                    TextButton(onClick = {
                        newNameDraft = ""
                        showCreateDialog = true
                    }) {
                        Icon(Icons.Rounded.Add, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(4.dp))
                        Text("Crear")
                    }
                }
            }

            if (playlists.isEmpty()) {
                item {
                    MagicSurfaceCard {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 10.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(8.dp),
                        ) {
                            Icon(Icons.AutoMirrored.Rounded.QueueMusic, contentDescription = null, tint = colors.primary, modifier = Modifier.size(32.dp))
                            Text("Crea tu primera lista local", style = MaterialTheme.typography.titleSmall, color = colors.onSurface)
                            Text(
                                text = "Agrupa canciones FLAC o MP3 de tu biblioteca, reordénalas y reprodúcelas al instante.",
                                style = MaterialTheme.typography.bodySmall,
                                color = colors.onSurfaceVariant,
                            )
                        }
                    }
                }
            } else {
                items(playlists, key = { it.id }) { pl ->
                    Surface(
                        onClick = { activePlaylistSheet = pl },
                        shape = RoundedCornerShape(tokens.radii.card),
                        color = colors.surfaceContainer,
                        border = BorderStroke(1.dp, tokens.semantic.divider),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("user_playlist_${pl.id}"),
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(52.dp)
                                    .clip(RoundedCornerShape(tokens.radii.artworkMedium))
                                    .background(colors.secondaryContainer),
                                contentAlignment = Alignment.Center,
                            ) {
                                if (!pl.coverUri.isNullOrBlank()) {
                                    MagicAlbumArtwork(
                                        bitmap = null,
                                        artworkModel = pl.coverUri,
                                        contentDescription = pl.name,
                                        cornerRadius = tokens.radii.artworkMedium,
                                        modifier = Modifier.fillMaxSize(),
                                    )
                                } else {
                                    Icon(
                                        imageVector = Icons.Rounded.LibraryMusic,
                                        contentDescription = null,
                                        tint = colors.onSecondaryContainer,
                                    )
                                }
                            }
                            Spacer(Modifier.width(12.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = pl.name,
                                    style = MaterialTheme.typography.titleSmall,
                                    fontWeight = FontWeight.SemiBold,
                                    color = colors.onSurface,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                )
                                Text(
                                    text = "${pl.trackCount} pistas locales",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = colors.onSurfaceVariant,
                                )
                            }
                            IconButton(onClick = { onPlayPlaylist(pl.id, false) }) {
                                Icon(Icons.Rounded.PlayArrow, contentDescription = "Reproducir ${pl.name}", tint = colors.primary)
                            }
                            IconButton(onClick = {
                                renameTarget = pl
                                renameDraft = pl.name
                            }) {
                                Icon(Icons.Rounded.Edit, contentDescription = "Renombrar", tint = colors.onSurfaceVariant)
                            }
                            IconButton(onClick = { onDeletePlaylist(pl.id) }) {
                                Icon(Icons.Rounded.DeleteOutline, contentDescription = "Eliminar", tint = colors.error)
                            }
                        }
                    }
                }
            }

            // Connected Official Cloud Playlists Preview (Spotify / YouTube Music)
            if (spotifySnapshot.playlists.isNotEmpty() || youtubeSnapshot.playlists.isNotEmpty()) {
                item {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Text(
                            text = "Cuentas oficiales sincronizadas",
                            style = MaterialTheme.typography.titleMedium,
                            color = colors.onBackground,
                        )
                        TextButton(onClick = onOpenServices) {
                            Text("Gestionar")
                        }
                    }
                }

                items(spotifySnapshot.playlists + youtubeSnapshot.playlists, key = { "${it.sourceId}_${it.id}" }) { cloudPl ->
                    Surface(
                        onClick = { onLaunchAccountUri(cloudPl.sourceId, cloudPl.externalUri) },
                        shape = RoundedCornerShape(tokens.radii.listItem),
                        color = colors.surfaceContainerLow,
                        border = BorderStroke(1.dp, tokens.semantic.divider),
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        Row(
                            modifier = Modifier.padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            MagicAlbumArtwork(
                                bitmap = null,
                                artworkModel = cloudPl.artworkUrl,
                                contentDescription = cloudPl.title,
                                cornerRadius = 12.dp,
                                modifier = Modifier.size(46.dp),
                            )
                            Spacer(Modifier.width(12.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = cloudPl.title,
                                    style = MaterialTheme.typography.titleSmall,
                                    color = colors.onSurface,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                )
                                Text(
                                    text = "${cloudPl.sourceId.displayName} • ${cloudPl.subtitle}",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = colors.onSurfaceVariant,
                                )
                            }
                            Icon(
                                imageVector = Icons.AutoMirrored.Rounded.OpenInNew,
                                contentDescription = "Abrir en app oficial",
                                tint = colors.primary,
                            )
                        }
                    }
                }
            } else {
                item {
                    MagicSurfaceCard {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Column(modifier = Modifier.weight(1f).padding(end = 8.dp)) {
                                Text(
                                    text = "Conecta Spotify o YouTube Music",
                                    style = MaterialTheme.typography.titleSmall,
                                    color = colors.onSurface,
                                )
                                Text(
                                    text = "Visualiza tus listas oficiales y ábrelas directo en su app mediante OAuth seguro.",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = colors.onSurfaceVariant,
                                )
                            }
                            FilledTonalButton(onClick = onOpenServices) {
                                Text("Conectar")
                            }
                        }
                    }
                }
            }
        }
    }

    if (showCreateDialog) {
        AlertDialog(
            onDismissRequest = { showCreateDialog = false },
            title = { Text("Nueva lista") },
            text = {
                OutlinedTextField(
                    value = newNameDraft,
                    onValueChange = { newNameDraft = it },
                    label = { Text("Nombre de la lista") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                )
            },
            confirmButton = {
                Button(
                    enabled = newNameDraft.isNotBlank(),
                    onClick = {
                        onCreatePlaylist(newNameDraft.trim())
                        showCreateDialog = false
                    },
                ) {
                    Text("Crear")
                }
            },
            dismissButton = {
                TextButton(onClick = { showCreateDialog = false }) { Text("Cancelar") }
            },
        )
    }

    renameTarget?.let { target ->
        AlertDialog(
            onDismissRequest = { renameTarget = null },
            title = { Text("Renombrar lista") },
            text = {
                OutlinedTextField(
                    value = renameDraft,
                    onValueChange = { renameDraft = it },
                    label = { Text("Nuevo nombre") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                )
            },
            confirmButton = {
                Button(
                    enabled = renameDraft.isNotBlank(),
                    onClick = {
                        onRenamePlaylist(target.id, renameDraft.trim())
                        renameTarget = null
                    },
                ) {
                    Text("Guardar")
                }
            },
            dismissButton = {
                TextButton(onClick = { renameTarget = null }) { Text("Cancelar") }
            },
        )
    }

    activePlaylistSheet?.let { pl ->
        val tracksFlow = remember(pl.id) { observePlaylistTracks(pl.id) }
        val tracks by tracksFlow.collectAsStateWithLifecycle(initialValue = emptyList())
        ModalBottomSheet(
            onDismissRequest = { activePlaylistSheet = null },
            sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
            shape = RoundedCornerShape(topStart = tokens.radii.sheet, topEnd = tokens.radii.sheet),
            containerColor = colors.surfaceContainerHigh,
            dragHandle = { BottomSheetDefaults.DragHandle() },
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = tokens.spacing.screenMargin)
                    .padding(bottom = 28.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(pl.name, style = MaterialTheme.typography.titleLarge, color = colors.onSurface)
                        Text("${tracks.size} pistas", style = MaterialTheme.typography.bodySmall, color = colors.onSurfaceVariant)
                    }
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        FilledTonalButton(onClick = { onPlayPlaylist(pl.id, true) }) {
                            Icon(Icons.Rounded.Shuffle, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(Modifier.width(4.dp))
                            Text("Aleatorio")
                        }
                        Button(
                            onClick = { onPlayPlaylist(pl.id, false) },
                            colors = ButtonDefaults.buttonColors(containerColor = colors.primary),
                        ) {
                            Icon(Icons.Rounded.PlayArrow, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(Modifier.width(4.dp))
                            Text("Reproducir")
                        }
                    }
                }
                if (tracks.isEmpty()) {
                    Text(
                        text = "Esta lista está vacía. Mantén pulsada una pista en Biblioteca o usa su menú contextual para añadirla.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = colors.onSurfaceVariant,
                        modifier = Modifier.padding(vertical = 12.dp),
                    )
                } else {
                    LazyColumn(
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.height(340.dp),
                    ) {
                        itemsIndexed(tracks, key = { _, t -> t.id }) { idx, track ->
                            val req = LocalArtworkRequest(
                                trackId = track.id,
                                albumId = track.albumId,
                                contentUri = track.contentUri,
                                albumArtUri = track.albumArtUri,
                                dateModifiedSec = track.dateModified,
                                targetSizePx = 128,
                            )
                            Column {
                                MagicTrackListItem(
                                    item = TrackListItemModel(
                                        id = track.id.toString(),
                                        title = track.title,
                                        artist = track.artist,
                                        durationLabel = formatTimeMs(track.durationMs),
                                        artworkModel = req,
                                        formatBadge = track.codecLabel(),
                                    ),
                                    variant = TrackItemVariant.NUMBERED,
                                    onClick = { onPlayLocalTracks(tracks, idx, false) },
                                )
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.End,
                                ) {
                                    IconButton(
                                        enabled = idx > 0,
                                        onClick = {
                                            val ids = tracks.map { it.id }.toMutableList()
                                            val moved = ids.removeAt(idx)
                                            ids.add(idx - 1, moved)
                                            onReorderPlaylistTracks(pl.id, ids)
                                        },
                                    ) {
                                        Icon(Icons.Rounded.ArrowUpward, contentDescription = "Subir", modifier = Modifier.size(18.dp))
                                    }
                                    IconButton(
                                        enabled = idx < tracks.lastIndex,
                                        onClick = {
                                            val ids = tracks.map { it.id }.toMutableList()
                                            val moved = ids.removeAt(idx)
                                            ids.add(idx + 1, moved)
                                            onReorderPlaylistTracks(pl.id, ids)
                                        },
                                    ) {
                                        Icon(Icons.Rounded.ArrowDownward, contentDescription = "Bajar", modifier = Modifier.size(18.dp))
                                    }
                                    IconButton(onClick = { onRemoveTrackFromPlaylist(pl.id, track.id) }) {
                                        Icon(Icons.Rounded.DeleteOutline, contentDescription = "Quitar", tint = colors.error, modifier = Modifier.size(18.dp))
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
