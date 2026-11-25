package com.example.ui.screens

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.automirrored.rounded.PlaylistAdd
import androidx.compose.material.icons.rounded.Album
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.Folder
import androidx.compose.material.icons.rounded.GraphicEq
import androidx.compose.material.icons.rounded.GridView
import androidx.compose.material.icons.rounded.HealthAndSafety
import androidx.compose.material.icons.rounded.LibraryMusic
import androidx.compose.material.icons.rounded.MoreVert
import androidx.compose.material.icons.rounded.MusicNote
import androidx.compose.material.icons.rounded.Person
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material.icons.rounded.Shuffle
import androidx.compose.material.icons.rounded.Sort
import androidx.compose.material.icons.rounded.ViewAgenda
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.PrimaryTabRow
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Tab
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.paging.compose.collectAsLazyPagingItems
import androidx.paging.compose.itemKey
import com.example.R
import com.example.core.artwork.LocalArtworkRequest
import com.example.core.library.AlbumSummary
import com.example.core.library.ArtistSummary
import com.example.core.library.FolderSummary
import com.example.core.library.LocalTrackEntity
import com.example.core.library.PlaylistWithSummary
import com.example.core.library.codecLabel
import com.example.core.media.HealthLight
import com.example.data.ActiveSessionInfo
import com.example.data.LibrarySortOption
import com.example.data.LibrarySourceFilter
import com.example.data.MusicServiceApp
import com.example.data.TrackHistoryEntry
import com.example.designsystem.MagicTheme
import com.example.designsystem.expressive.MagicConnectedButtonGroup
import com.example.designsystem.expressive.MagicExpressiveLoading
import com.example.designsystem.expressive.MagicFlexibleLargeTopBar
import com.example.designsystem.expressive.MagicFloatingToolbarBar
import com.example.designsystem.expressive.MagicSplitActionButton
import com.example.ui.MagicMusicUiState
import com.example.ui.components.AnimatedEqualizerBars
import com.example.ui.components.CoverGridItem
import com.example.ui.components.MagicAlbumArtwork
import com.example.ui.components.MagicAppIconBadge
import com.example.ui.components.MagicContextMenuSheet
import com.example.ui.components.MagicFilterChip
import com.example.ui.components.MagicHorizontalCoverGrid
import com.example.ui.components.MagicSearchBarPill
import com.example.ui.components.MagicSurfaceCard
import com.example.ui.components.MagicTrackListItem
import com.example.ui.components.TrackItemVariant
import com.example.ui.components.TrackListItemModel
import com.example.ui.components.formatTimeMs
import kotlinx.coroutines.flow.Flow

private sealed interface LibraryDrillTarget {
    data class Album(val albumId: Long, val title: String, val artist: String) : LibraryDrillTarget
    data class Artist(val artistId: Long, val name: String) : LibraryDrillTarget
    data class Folder(val folderPath: String, val folderName: String) : LibraryDrillTarget
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
fun LibraryScreen(
    state: MagicMusicUiState,
    pagedTracksFlow: Flow<androidx.paging.PagingData<LocalTrackEntity>>,
    albums: List<AlbumSummary>,
    artists: List<ArtistSummary>,
    folders: List<FolderSummary>,
    playlists: List<PlaylistWithSummary>,
    gridColumns: Int,
    onSearchQueryChange: (String) -> Unit,
    onSortOptionChange: (LibrarySortOption) -> Unit,
    onSourceFilterChange: (LibrarySourceFilter) -> Unit,
    onGridColumnsChange: (Int) -> Unit,
    onScanLocalLibrary: () -> Unit,
    onShuffleAllLocal: () -> Unit,
    onPlayLocalTracks: (List<LocalTrackEntity>, Int, Boolean) -> Unit,
    onToggleTrackFavorite: (LocalTrackEntity) -> Unit,
    onAddTrackToPlaylist: (Long, Long) -> Unit,
    onCreatePlaylist: (String) -> Unit,
    onShareTrack: (LocalTrackEntity) -> Unit,
    onSelectSession: (String) -> Unit,
    onOpenSessionMenu: (ActiveSessionInfo) -> Unit,
    onToggleLocalEngine: () -> Unit,
    onPlayRecentTrack: (TrackHistoryEntry) -> Unit,
    onOpenEqualizer: () -> Unit,
    onOpenSystemHealth: () -> Unit,
    onOpenServices: () -> Unit,
    onOpenNowPlaying: () -> Unit,
    onOpenActiveApp: (String) -> Unit,
    onShowFeedback: (String) -> Unit,
    pagedTracksForAlbum: (Long) -> Flow<androidx.paging.PagingData<LocalTrackEntity>>,
    pagedTracksForArtist: (Long) -> Flow<androidx.paging.PagingData<LocalTrackEntity>>,
    pagedTracksForFolder: (String) -> Flow<androidx.paging.PagingData<LocalTrackEntity>>,
) {
    val context = LocalContext.current
    val tokens = MagicTheme.tokens
    val colors = MaterialTheme.colorScheme

    var selectedTab by rememberSaveable { mutableIntStateOf(0) }
    var drillTarget by remember { mutableStateOf<LibraryDrillTarget?>(null) }
    var showSortMenu by remember { mutableStateOf(false) }
    var showShuffleMenu by remember { mutableStateOf(false) }
    var trackForContextMenu by remember { mutableStateOf<LocalTrackEntity?>(null) }
    var showAddToPlaylistDialog by remember { mutableStateOf<List<Long>?>(null) }
    val selectedTrackIds = remember { mutableStateListOf<Long>() }

    if (drillTarget != null) {
        BackHandler { drillTarget = null }
    } else if (selectedTrackIds.isNotEmpty()) {
        BackHandler { selectedTrackIds.clear() }
    }

    var hasAudioPermission by remember(state.hasAudioLibraryPermission) {
        mutableStateOf(
            state.hasAudioLibraryPermission ||
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                    ContextCompat.checkSelfPermission(context, Manifest.permission.READ_MEDIA_AUDIO) == PackageManager.PERMISSION_GRANTED
                } else {
                    ContextCompat.checkSelfPermission(context, Manifest.permission.READ_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED
                },
        )
    }

    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission(),
    ) { granted ->
        hasAudioPermission = granted
        if (granted) {
            onScanLocalLibrary()
        } else {
            onShowFeedback("Permiso de audio denegado")
        }
    }

    val scrollBehavior = TopAppBarDefaults.exitUntilCollapsedScrollBehavior()
    val pagedTracks = pagedTracksFlow.collectAsLazyPagingItems()
    val media = state.media
    val iconLookup = remember(state.installedServices) {
        state.installedServices.associateBy({ it.packageName }, { it.appIcon })
    }

    val activeSubtitle = buildString {
        append("${state.localTrackCount} pistas")
        if (media.activeSessions.isNotEmpty()) {
            append(" • ${media.activeSessions.size} sesiones")
        }
    }

    Scaffold(
        modifier = Modifier
            .fillMaxSize()
            .nestedScroll(scrollBehavior.nestedScrollConnection),
        containerColor = Color.Transparent,
        topBar = {
            MagicFlexibleLargeTopBar(
                title = when (val target = drillTarget) {
                    is LibraryDrillTarget.Album -> target.title
                    is LibraryDrillTarget.Artist -> target.name
                    is LibraryDrillTarget.Folder -> target.folderName
                    null -> stringResource(R.string.library_title)
                },
                subtitle = when (val target = drillTarget) {
                    is LibraryDrillTarget.Album -> target.artist
                    is LibraryDrillTarget.Artist -> "Artista local"
                    is LibraryDrillTarget.Folder -> target.folderPath
                    null -> activeSubtitle
                },
                useExpressiveM3 = tokens.useExpressiveM3,
                scrollBehavior = scrollBehavior,
                navigationIcon = {
                    if (drillTarget != null) {
                        IconButton(onClick = { drillTarget = null }) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Rounded.ArrowBack,
                                contentDescription = stringResource(R.string.action_back),
                            )
                        }
                    }
                },
                actions = {
                    IconButton(
                        onClick = {
                            if (hasAudioPermission) {
                                onScanLocalLibrary()
                            } else {
                                val perm = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                                    Manifest.permission.READ_MEDIA_AUDIO
                                } else {
                                    Manifest.permission.READ_EXTERNAL_STORAGE
                                }
                                permissionLauncher.launch(perm)
                            }
                        },
                        modifier = Modifier.testTag("library_scan_btn"),
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.Refresh,
                            contentDescription = stringResource(R.string.library_scan_btn),
                        )
                    }
                },
            )
        },
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
        ) {
            val currentDrill = drillTarget
            if (currentDrill != null) {
                val drillFlow = remember(currentDrill) {
                    when (currentDrill) {
                        is LibraryDrillTarget.Album -> pagedTracksForAlbum(currentDrill.albumId)
                        is LibraryDrillTarget.Artist -> pagedTracksForArtist(currentDrill.artistId)
                        is LibraryDrillTarget.Folder -> pagedTracksForFolder(currentDrill.folderPath)
                    }
                }
                val drillItems = drillFlow.collectAsLazyPagingItems()
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(
                        start = tokens.spacing.screenMargin,
                        end = tokens.spacing.screenMargin,
                        top = tokens.spacing.sm,
                        bottom = 100.dp,
                    ),
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    items(
                        count = drillItems.itemCount,
                        key = drillItems.itemKey { it.id },
                    ) { index ->
                        val track = drillItems[index] ?: return@items
                        TrackRowItem(
                            track = track,
                            isCurrent = media.localTrackId == track.id,
                            isSelected = selectedTrackIds.contains(track.id),
                            onClick = {
                                if (selectedTrackIds.isNotEmpty()) {
                                    if (selectedTrackIds.contains(track.id)) selectedTrackIds.remove(track.id)
                                    else selectedTrackIds.add(track.id)
                                } else {
                                    val snapshot = (0 until drillItems.itemCount).mapNotNull { drillItems[it] }
                                    onPlayLocalTracks(snapshot, index.coerceAtMost((snapshot.size - 1).coerceAtLeast(0)), false)
                                }
                            },
                            onLongClick = {
                                if (selectedTrackIds.contains(track.id)) selectedTrackIds.remove(track.id)
                                else selectedTrackIds.add(track.id)
                            },
                            onMoreClick = { trackForContextMenu = track },
                        )
                    }
                }
            } else {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .testTag("library_screen_list"),
                    contentPadding = PaddingValues(
                        start = tokens.spacing.screenMargin,
                        end = tokens.spacing.screenMargin,
                        top = tokens.spacing.xs,
                        bottom = 96.dp,
                    ),
                    verticalArrangement = Arrangement.spacedBy(tokens.spacing.md),
                ) {
                    item {
                        MagicSearchBarPill(
                            query = state.searchQuery,
                            placeholder = stringResource(R.string.search_library_placeholder),
                            onClick = {},
                            onMicClick = onOpenServices,
                            onQueryChange = onSearchQueryChange,
                        )
                    }

                    // Expressive Split Action + Quick Equalizer shortcut (matches reference video)
                    item {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(10.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Box(modifier = Modifier.weight(1f)) {
                                MagicSplitActionButton(
                                    label = "Aleatorio",
                                    leadingIcon = Icons.Rounded.Shuffle,
                                    useExpressiveM3 = tokens.useExpressiveM3,
                                    menuExpanded = showShuffleMenu,
                                    onPrimaryClick = onShuffleAllLocal,
                                    onToggleMenu = { showShuffleMenu = !showShuffleMenu },
                                    primaryTestTag = "library_shuffle_btn",
                                    modifier = Modifier.fillMaxWidth(),
                                )
                                DropdownMenu(
                                    expanded = showShuffleMenu,
                                    onDismissRequest = { showShuffleMenu = false },
                                ) {
                                    DropdownMenuItem(
                                        text = { Text("Reproducir todo en aleatorio") },
                                        leadingIcon = { Icon(Icons.Rounded.Shuffle, contentDescription = null) },
                                        onClick = {
                                            showShuffleMenu = false
                                            onShuffleAllLocal()
                                        },
                                    )
                                    DropdownMenuItem(
                                        text = { Text("Motor Hi-Res integrado") },
                                        leadingIcon = { Icon(Icons.Rounded.PlayArrow, contentDescription = null) },
                                        onClick = {
                                            showShuffleMenu = false
                                            onToggleLocalEngine()
                                        },
                                    )
                                    DropdownMenuItem(
                                        text = { Text("Escanear carpetas ahora") },
                                        leadingIcon = { Icon(Icons.Rounded.Refresh, contentDescription = null) },
                                        onClick = {
                                            showShuffleMenu = false
                                            onScanLocalLibrary()
                                        },
                                    )
                                }
                            }

                            FilledTonalButton(
                                onClick = onOpenEqualizer,
                                modifier = Modifier.testTag("library_quick_eq_btn"),
                            ) {
                                Icon(Icons.Rounded.GraphicEq, contentDescription = null, modifier = Modifier.size(18.dp))
                                Spacer(Modifier.width(6.dp))
                                Text("Ecualizador", maxLines = 1)
                            }
                        }
                    }

                    // Source Filter Chips
                    item {
                        LazyRow(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            modifier = Modifier.fillMaxWidth(),
                        ) {
                            items(LibrarySourceFilter.entries, key = { it.name }) { filter ->
                                MagicFilterChip(
                                    label = filter.label,
                                    selected = state.librarySourceFilter == filter,
                                    onClick = { onSourceFilterChange(filter) },
                                    modifier = Modifier.testTag("source_chip_${filter.name.lowercase()}"),
                                )
                            }
                        }
                    }

                    // System Health pending banner (compact M3 tonal card)
                    if (state.systemHealth.hasPendingIssues) {
                        item {
                            val isRed = state.systemHealth.overallLight == HealthLight.RED
                            val badgeTint = if (isRed) colors.error else tokens.semantic.warning
                            MagicSurfaceCard(
                                highlightBorder = isRed,
                                containerColor = colors.surfaceContainerHigh,
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
                                            text = "Permisos de sesión y batería para widgets",
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

                    // Active MediaSessions Strip (absorbed from HomeDeckScreen)
                    if (media.activeSessions.isNotEmpty() &&
                        (state.librarySourceFilter == LibrarySourceFilter.ALL || state.librarySourceFilter != LibrarySourceFilter.LOCAL)
                    ) {
                        item {
                            Text(
                                text = "Sesiones activas",
                                style = MaterialTheme.typography.titleMedium,
                                color = colors.onBackground,
                            )
                        }
                        items(media.activeSessions, key = { "sess_${it.packageName}" }) { session ->
                            Surface(
                                shape = RoundedCornerShape(tokens.radii.card),
                                color = if (session.isSelected) colors.primaryContainer.copy(alpha = 0.5f) else colors.surfaceContainer,
                                border = BorderStroke(
                                    width = if (session.isSelected) 1.5.dp else 1.dp,
                                    color = if (session.isSelected) colors.primary else tokens.semantic.divider,
                                ),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(tokens.radii.card))
                                    .combinedClickable(
                                        onClick = {
                                            if (session.isSelected) onOpenNowPlaying()
                                            else onSelectSession(session.packageName)
                                        },
                                        onLongClick = { onOpenSessionMenu(session) },
                                    )
                                    .testTag("active_session_${session.service.id}"),
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(horizontal = 14.dp, vertical = 10.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                ) {
                                    MagicAppIconBadge(
                                        drawable = iconLookup[session.packageName],
                                        fallbackTint = session.service.accentColor,
                                        contentDescription = session.appLabel,
                                        size = 44.dp,
                                        cornerRadius = 14.dp,
                                        service = session.service,
                                        serviceName = session.appLabel,
                                    )
                                    Spacer(modifier = Modifier.width(12.dp))
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = session.appLabel,
                                            style = MaterialTheme.typography.labelMedium,
                                            color = colors.primary,
                                        )
                                        Text(
                                            text = session.title.ifBlank { "Sin título de pista" },
                                            style = MaterialTheme.typography.titleSmall,
                                            fontWeight = FontWeight.SemiBold,
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
                                            color = colors.primary,
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
                    }

                    // Escuchado / Popular horizontal cover strip
                    val recentHistory = state.listeningStats.recentTracks
                    if (recentHistory.isNotEmpty() && state.searchQuery.isBlank()) {
                        item {
                            Text(
                                text = stringResource(R.string.library_listened_title),
                                style = MaterialTheme.typography.titleMedium,
                                color = colors.onBackground,
                            )
                        }
                        item {
                            val covers = remember(recentHistory, media.albumArt, media.packageName, media.title) {
                                recentHistory.take(12).map { entry ->
                                    val activeArt = if (entry.packageName == media.packageName && entry.title == media.title) {
                                        media.albumArt
                                    } else {
                                        null
                                    }
                                    CoverGridItem(
                                        id = entry.id,
                                        title = entry.title,
                                        subtitle = entry.artist,
                                        accentTint = entry.service.accentColor,
                                        bitmap = activeArt,
                                        packageName = entry.packageName,
                                    )
                                }
                            }
                            MagicHorizontalCoverGrid(
                                items = covers,
                                horizontalPadding = 0.dp,
                                onItemClick = { gridItem ->
                                    val entry = recentHistory.firstOrNull { it.id == gridItem.id }
                                    if (entry != null) onPlayRecentTrack(entry)
                                    else if (gridItem.packageName.isNotBlank()) onOpenActiveApp(gridItem.packageName)
                                },
                            )
                        }
                    }

                    // Local Library Inner Category Tabs: Pistas / Álbumes / Artistas / Carpetas
                    item {
                        PrimaryTabRow(
                            selectedTabIndex = selectedTab,
                            containerColor = Color.Transparent,
                        ) {
                            Tab(
                                selected = selectedTab == 0,
                                onClick = { selectedTab = 0 },
                                text = { Text("Pistas") },
                                modifier = Modifier.testTag("library_tab_songs"),
                            )
                            Tab(
                                selected = selectedTab == 1,
                                onClick = { selectedTab = 1 },
                                text = { Text(stringResource(R.string.library_filter_albums)) },
                                modifier = Modifier.testTag("library_tab_albums"),
                            )
                            Tab(
                                selected = selectedTab == 2,
                                onClick = { selectedTab = 2 },
                                text = { Text(stringResource(R.string.library_filter_artists)) },
                                modifier = Modifier.testTag("library_tab_artists"),
                            )
                            Tab(
                                selected = selectedTab == 3,
                                onClick = { selectedTab = 3 },
                                text = { Text(stringResource(R.string.library_filter_folders)) },
                                modifier = Modifier.testTag("library_tab_folders"),
                            )
                        }
                    }

                    if (!hasAudioPermission) {
                        item {
                            MagicSurfaceCard(highlightBorder = true) {
                                Column(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalAlignment = Alignment.CenterHorizontally,
                                    verticalArrangement = Arrangement.spacedBy(10.dp),
                                ) {
                                    Icon(
                                        imageVector = Icons.Rounded.LibraryMusic,
                                        contentDescription = null,
                                        tint = colors.primary,
                                        modifier = Modifier.size(36.dp),
                                    )
                                    Text(
                                        text = stringResource(R.string.library_permission_title),
                                        style = MaterialTheme.typography.titleMedium,
                                        color = colors.onSurface,
                                    )
                                    Text(
                                        text = stringResource(R.string.library_permission_desc),
                                        style = MaterialTheme.typography.bodySmall,
                                        color = colors.onSurfaceVariant,
                                    )
                                    Button(
                                        onClick = {
                                            val perm = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                                                Manifest.permission.READ_MEDIA_AUDIO
                                            } else {
                                                Manifest.permission.READ_EXTERNAL_STORAGE
                                            }
                                            permissionLauncher.launch(perm)
                                        },
                                        modifier = Modifier.testTag("grant_audio_permission_btn"),
                                    ) {
                                        Text(stringResource(R.string.library_permission_btn))
                                    }
                                }
                            }
                        }
                    } else if (state.isLibraryScanning && state.localTrackCount == 0) {
                        item {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 32.dp),
                                contentAlignment = Alignment.Center,
                            ) {
                                Column(
                                    horizontalAlignment = Alignment.CenterHorizontally,
                                    verticalArrangement = Arrangement.spacedBy(12.dp),
                                ) {
                                    MagicExpressiveLoading(useExpressiveM3 = tokens.useExpressiveM3)
                                    Text(
                                        text = "Escaneando almacenamiento de audio...",
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = colors.onSurfaceVariant,
                                    )
                                }
                            }
                        }
                    } else when (selectedTab) {
                        0 -> {
                            // Sort & Layout toolbar
                            item {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically,
                                ) {
                                    Box {
                                        FilledTonalButton(
                                            onClick = { showSortMenu = true },
                                            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                                        ) {
                                            Icon(Icons.Rounded.Sort, contentDescription = null, modifier = Modifier.size(16.dp))
                                            Spacer(Modifier.width(6.dp))
                                            Text(state.librarySortOption.label, style = MaterialTheme.typography.labelLarge)
                                        }
                                        DropdownMenu(
                                            expanded = showSortMenu,
                                            onDismissRequest = { showSortMenu = false },
                                        ) {
                                            LibrarySortOption.entries.forEach { sortOpt ->
                                                DropdownMenuItem(
                                                    text = { Text(sortOpt.label) },
                                                    onClick = {
                                                        showSortMenu = false
                                                        onSortOptionChange(sortOpt)
                                                    },
                                                )
                                            }
                                        }
                                    }
                                    Text(
                                        text = "${pagedTracks.itemCount} pistas",
                                        style = MaterialTheme.typography.labelLarge,
                                        color = colors.onSurfaceVariant,
                                    )
                                }
                            }

                            if (pagedTracks.itemCount == 0) {
                                item {
                                    EmptyLibraryCard(onScanLocalLibrary)
                                }
                            } else {
                                items(
                                    count = pagedTracks.itemCount,
                                    key = pagedTracks.itemKey { it.id },
                                ) { index ->
                                    val track = pagedTracks[index] ?: return@items
                                    TrackRowItem(
                                        track = track,
                                        isCurrent = media.localTrackId == track.id,
                                        isSelected = selectedTrackIds.contains(track.id),
                                        onClick = {
                                            if (selectedTrackIds.isNotEmpty()) {
                                                if (selectedTrackIds.contains(track.id)) selectedTrackIds.remove(track.id)
                                                else selectedTrackIds.add(track.id)
                                            } else {
                                                val snapshot = (0 until pagedTracks.itemCount).mapNotNull { pagedTracks[it] }
                                                onPlayLocalTracks(snapshot, index.coerceAtMost((snapshot.size - 1).coerceAtLeast(0)), false)
                                            }
                                        },
                                        onLongClick = {
                                            if (selectedTrackIds.contains(track.id)) selectedTrackIds.remove(track.id)
                                            else selectedTrackIds.add(track.id)
                                        },
                                        onMoreClick = { trackForContextMenu = track },
                                    )
                                }
                            }
                        }

                        1 -> {
                            item {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically,
                                ) {
                                    Text(
                                        text = "${albums.size} álbumes",
                                        style = MaterialTheme.typography.labelLarge,
                                        color = colors.onSurfaceVariant,
                                    )
                                    IconButton(
                                        onClick = { onGridColumnsChange(if (gridColumns == 2) 3 else 2) },
                                    ) {
                                        Icon(
                                            imageVector = if (gridColumns == 2) Icons.Rounded.GridView else Icons.Rounded.ViewAgenda,
                                            contentDescription = "Cambiar columnas de cuadrícula",
                                        )
                                    }
                                }
                            }
                            if (albums.isEmpty()) {
                                item { EmptyLibraryCard(onScanLocalLibrary) }
                            } else {
                                val rows = albums.chunked(gridColumns.coerceIn(2, 3))
                                items(rows) { rowAlbums ->
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                                    ) {
                                        rowAlbums.forEach { album ->
                                            val req = remember(album.albumId, album.sampleTrackId) {
                                                LocalArtworkRequest(
                                                    trackId = album.sampleTrackId,
                                                    albumId = album.albumId,
                                                    contentUri = album.sampleContentUri,
                                                    albumArtUri = album.albumArtUri,
                                                    targetSizePx = 256,
                                                )
                                            }
                                            Surface(
                                                onClick = {
                                                    drillTarget = LibraryDrillTarget.Album(album.albumId, album.album, album.artist)
                                                },
                                                shape = RoundedCornerShape(tokens.radii.card),
                                                color = colors.surfaceContainer,
                                                modifier = Modifier
                                                    .weight(1f)
                                                    .testTag("album_card_${album.albumId}"),
                                            ) {
                                                Column(modifier = Modifier.padding(10.dp)) {
                                                    MagicAlbumArtwork(
                                                        bitmap = null,
                                                        artworkModel = req,
                                                        contentDescription = album.album,
                                                        cornerRadius = tokens.radii.artworkMedium,
                                                        modifier = Modifier
                                                            .fillMaxWidth()
                                                            .aspectRatio(1f),
                                                    )
                                                    Spacer(Modifier.height(8.dp))
                                                    Text(
                                                        text = album.album,
                                                        style = MaterialTheme.typography.titleSmall,
                                                        color = colors.onSurface,
                                                        maxLines = 1,
                                                        overflow = TextOverflow.Ellipsis,
                                                    )
                                                    Text(
                                                        text = "${album.artist} • ${album.trackCount}",
                                                        style = MaterialTheme.typography.bodySmall,
                                                        color = colors.onSurfaceVariant,
                                                        maxLines = 1,
                                                        overflow = TextOverflow.Ellipsis,
                                                    )
                                                }
                                            }
                                        }
                                        repeat((gridColumns.coerceIn(2, 3) - rowAlbums.size).coerceAtLeast(0)) {
                                            Spacer(Modifier.weight(1f))
                                        }
                                    }
                                }
                            }
                        }

                        2 -> {
                            item {
                                Text(
                                    text = "${artists.size} artistas",
                                    style = MaterialTheme.typography.labelLarge,
                                    color = colors.onSurfaceVariant,
                                )
                            }
                            items(artists, key = { it.artistId }) { artist ->
                                Surface(
                                    onClick = {
                                        drillTarget = LibraryDrillTarget.Artist(artist.artistId, artist.artist)
                                    },
                                    shape = RoundedCornerShape(tokens.radii.listItem),
                                    color = colors.surfaceContainer,
                                    modifier = Modifier.fillMaxWidth(),
                                ) {
                                    Row(
                                        modifier = Modifier.padding(14.dp),
                                        verticalAlignment = Alignment.CenterVertically,
                                    ) {
                                        Box(
                                            modifier = Modifier
                                                .size(44.dp)
                                                .clip(CircleShape)
                                                .background(colors.secondaryContainer),
                                            contentAlignment = Alignment.Center,
                                        ) {
                                            Icon(
                                                imageVector = Icons.Rounded.Person,
                                                contentDescription = null,
                                                tint = colors.onSecondaryContainer,
                                            )
                                        }
                                        Spacer(Modifier.width(12.dp))
                                        Column(modifier = Modifier.weight(1f)) {
                                            Text(
                                                text = artist.artist,
                                                style = MaterialTheme.typography.titleSmall,
                                                color = colors.onSurface,
                                                maxLines = 1,
                                                overflow = TextOverflow.Ellipsis,
                                            )
                                            Text(
                                                text = "${artist.albumCount} álbumes • ${artist.trackCount} pistas",
                                                style = MaterialTheme.typography.bodySmall,
                                                color = colors.onSurfaceVariant,
                                            )
                                        }
                                    }
                                }
                            }
                        }

                        else -> {
                            item {
                                Text(
                                    text = "${folders.size} carpetas",
                                    style = MaterialTheme.typography.labelLarge,
                                    color = colors.onSurfaceVariant,
                                )
                            }
                            items(folders, key = { it.folderPath }) { folder ->
                                Surface(
                                    onClick = {
                                        drillTarget = LibraryDrillTarget.Folder(folder.folderPath, folder.folderName)
                                    },
                                    shape = RoundedCornerShape(tokens.radii.listItem),
                                    color = colors.surfaceContainer,
                                    modifier = Modifier.fillMaxWidth(),
                                ) {
                                    Row(
                                        modifier = Modifier.padding(14.dp),
                                        verticalAlignment = Alignment.CenterVertically,
                                    ) {
                                        Box(
                                            modifier = Modifier
                                                .size(44.dp)
                                                .clip(RoundedCornerShape(12.dp))
                                                .background(colors.tertiaryContainer),
                                            contentAlignment = Alignment.Center,
                                        ) {
                                            Icon(
                                                imageVector = Icons.Rounded.Folder,
                                                contentDescription = null,
                                                tint = colors.onTertiaryContainer,
                                            )
                                        }
                                        Spacer(Modifier.width(12.dp))
                                        Column(modifier = Modifier.weight(1f)) {
                                            Text(
                                                text = folder.folderName.ifBlank { "Raíz" },
                                                style = MaterialTheme.typography.titleSmall,
                                                color = colors.onSurface,
                                                maxLines = 1,
                                                overflow = TextOverflow.Ellipsis,
                                            )
                                            Text(
                                                text = "${folder.trackCount} pistas • ${folder.folderPath}",
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
                }
            }

            // Floating multi-select toolbar
            if (selectedTrackIds.isNotEmpty()) {
                MagicFloatingToolbarBar(
                    useExpressiveM3 = tokens.useExpressiveM3,
                    containerColor = colors.primaryContainer,
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .padding(16.dp)
                        .testTag("library_multiselect_toolbar"),
                ) {
                    Text(
                        text = "${selectedTrackIds.size} sel.",
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.Bold,
                        color = colors.onPrimaryContainer,
                        modifier = Modifier.padding(horizontal = 8.dp),
                    )
                    TextButton(
                        onClick = {
                            val selected = (0 until pagedTracks.itemCount)
                                .mapNotNull { pagedTracks[it] }
                                .filter { selectedTrackIds.contains(it.id) }
                            if (selected.isNotEmpty()) {
                                onPlayLocalTracks(selected, 0, false)
                            }
                            selectedTrackIds.clear()
                        },
                    ) {
                        Text("Reproducir", color = colors.onPrimaryContainer)
                    }
                    TextButton(
                        onClick = {
                            showAddToPlaylistDialog = selectedTrackIds.toList()
                        },
                    ) {
                        Text("A lista", color = colors.onPrimaryContainer)
                    }
                    IconButton(onClick = { selectedTrackIds.clear() }) {
                        Icon(
                            imageVector = Icons.Rounded.Close,
                            contentDescription = "Cancelar selección",
                            tint = colors.onPrimaryContainer,
                        )
                    }
                }
            }
        }
    }

    trackForContextMenu?.let { track ->
        val req = remember(track.id) {
            LocalArtworkRequest(
                trackId = track.id,
                albumId = track.albumId,
                contentUri = track.contentUri,
                albumArtUri = track.albumArtUri,
                dateModifiedSec = track.dateModified,
                targetSizePx = 256,
            )
        }
        MagicContextMenuSheet(
            title = track.title,
            artist = "${track.artist} • ${track.album}",
            bitmap = null,
            artworkModel = req,
            onDismiss = { trackForContextMenu = null },
            onActionSelected = { label ->
                when {
                    label.contains("playlist", ignoreCase = true) || label.contains("lista", ignoreCase = true) -> {
                        showAddToPlaylistDialog = listOf(track.id)
                    }
                    label.contains("Compartir", ignoreCase = true) -> onShareTrack(track)
                    label.contains("artista", ignoreCase = true) -> onSearchQueryChange(track.artist)
                    label.contains("Abrir", ignoreCase = true) -> onToggleTrackFavorite(track)
                }
            },
        )
    }

    showAddToPlaylistDialog?.let { trackIdsToAdd ->
        AlertDialog(
            onDismissRequest = { showAddToPlaylistDialog = null },
            title = { Text("Añadir a lista (${trackIdsToAdd.size})") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    if (playlists.isEmpty()) {
                        Text(
                            text = "No tienes listas creadas todavía. Crea 'Favoritas' ahora o ve a la pestaña Listas.",
                            style = MaterialTheme.typography.bodySmall,
                        )
                        FilledTonalButton(
                            onClick = {
                                onCreatePlaylist("Mi selección")
                                showAddToPlaylistDialog = null
                                selectedTrackIds.clear()
                            },
                            modifier = Modifier.fillMaxWidth(),
                        ) {
                            Text("Crear lista 'Mi selección'")
                        }
                    } else {
                        playlists.forEach { pl ->
                            Surface(
                                onClick = {
                                    trackIdsToAdd.forEach { tid -> onAddTrackToPlaylist(pl.id, tid) }
                                    showAddToPlaylistDialog = null
                                    selectedTrackIds.clear()
                                },
                                shape = RoundedCornerShape(tokens.radii.listItem),
                                color = colors.surfaceContainerLow,
                                modifier = Modifier.fillMaxWidth(),
                            ) {
                                Row(
                                    modifier = Modifier.padding(12.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                ) {
                                    Icon(Icons.AutoMirrored.Rounded.PlaylistAdd, contentDescription = null, tint = colors.primary)
                                    Spacer(Modifier.width(10.dp))
                                    Text("${pl.name} (${pl.trackCount})", style = MaterialTheme.typography.titleSmall)
                                }
                            }
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showAddToPlaylistDialog = null }) { Text("Cerrar") }
            },
        )
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun TrackRowItem(
    track: LocalTrackEntity,
    isCurrent: Boolean,
    isSelected: Boolean,
    onClick: () -> Unit,
    onLongClick: () -> Unit,
    onMoreClick: () -> Unit,
) {
    val req = remember(track.id, track.dateModified) {
        LocalArtworkRequest(
            trackId = track.id,
            albumId = track.albumId,
            contentUri = track.contentUri,
            albumArtUri = track.albumArtUri,
            dateModifiedSec = track.dateModified,
            targetSizePx = 128,
        )
    }
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .combinedClickable(
                onClick = onClick,
                onLongClick = onLongClick,
            ),
    ) {
        MagicTrackListItem(
            item = TrackListItemModel(
                id = track.id.toString(),
                title = track.title,
                artist = "${track.artist} • ${track.album}",
                durationLabel = formatTimeMs(track.durationMs),
                artworkModel = req,
                formatBadge = track.codecLabel(),
                accentTint = MaterialTheme.colorScheme.primary,
            ),
            variant = if (isCurrent || isSelected) TrackItemVariant.HIGHLIGHTED_CURRENT else TrackItemVariant.DEFAULT,
            onClick = onClick,
            onMoreClick = onMoreClick,
        )
        if (isSelected) {
            Icon(
                imageVector = Icons.Rounded.CheckCircle,
                contentDescription = "Seleccionada",
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier
                    .align(Alignment.TopStart)
                    .padding(6.dp)
                    .size(18.dp),
            )
        }
    }
}

@Composable
private fun EmptyLibraryCard(onScan: () -> Unit) {
    val colors = MaterialTheme.colorScheme
    MagicSurfaceCard {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 14.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Icon(Icons.Rounded.MusicNote, contentDescription = null, tint = colors.primary, modifier = Modifier.size(32.dp))
            Text(
                text = "No hay pistas indexadas",
                style = MaterialTheme.typography.titleMedium,
                color = colors.onSurface,
            )
            Text(
                text = "Escanea tus carpetas de música (FLAC, WAV, MP3, AAC, OPUS).",
                style = MaterialTheme.typography.bodySmall,
                color = colors.onSurfaceVariant,
            )
            FilledTonalButton(onClick = onScan) {
                Text(stringResource(R.string.library_scan_btn))
            }
        }
    }
}
