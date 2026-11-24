package com.example.ui.screens

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
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
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Album
import androidx.compose.material.icons.rounded.LibraryMusic
import androidx.compose.material.icons.rounded.MusicNote
import androidx.compose.material.icons.rounded.Person
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.PrimaryTabRow
import androidx.compose.material3.Surface
import androidx.compose.material3.Tab
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import com.example.R
import com.example.core.library.LibraryRepository
import com.example.core.library.LocalTrackEntity
import com.example.core.media.MediaRepository
import com.example.designsystem.MagicTheme
import com.example.ui.MagicMusicUiState
import com.example.ui.components.MagicSearchBarPill
import com.example.ui.components.MagicSurfaceCard
import com.example.ui.components.MagicTrackListItem
import com.example.ui.components.TrackItemVariant
import com.example.ui.components.TrackListItemModel
import com.example.ui.components.formatTimeMs
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LibraryScreen(
    state: MagicMusicUiState,
    onSearchQueryChange: (String) -> Unit,
    onShowFeedback: (String) -> Unit,
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val tokens = MagicTheme.tokens
    val colors = MaterialTheme.colorScheme

    val libraryRepo = remember { LibraryRepository(context) }
    var selectedTab by rememberSaveable { mutableIntStateOf(0) }
    var localTracks by remember { mutableStateOf<List<LocalTrackEntity>>(emptyList()) }
    var isScanning by remember { mutableStateOf(false) }

    var hasPermission by remember {
        mutableStateOf(
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                ContextCompat.checkSelfPermission(context, Manifest.permission.READ_MEDIA_AUDIO) == PackageManager.PERMISSION_GRANTED
            } else {
                ContextCompat.checkSelfPermission(context, Manifest.permission.READ_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED
            }
        )
    }

    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission(),
    ) { granted ->
        hasPermission = granted
        if (granted) {
            onShowFeedback("Permiso concedido • Escaneando biblioteca local")
            scope.launch {
                isScanning = true
                libraryRepo.syncMediaStoreIncremental()
                localTracks = libraryRepo.getAllTracksSnapshot()
                isScanning = false
            }
        } else {
            onShowFeedback("Permiso de audio denegado")
        }
    }

    LaunchedEffect(hasPermission) {
        if (hasPermission) {
            localTracks = libraryRepo.getAllTracksSnapshot()
            if (localTracks.isEmpty()) {
                isScanning = true
                libraryRepo.syncMediaStoreIncremental()
                localTracks = libraryRepo.getAllTracksSnapshot()
                isScanning = false
            }
        }
    }

    val query = state.searchQuery.trim()
    val filteredTracks = remember(localTracks, query) {
        if (query.isBlank()) localTracks
        else localTracks.filter {
            it.title.contains(query, ignoreCase = true) ||
                it.artist.contains(query, ignoreCase = true) ||
                it.album.contains(query, ignoreCase = true)
        }
    }

    val artists = remember(localTracks) { localTracks.map { it.artist }.distinct().sorted() }
    val albums = remember(localTracks) { localTracks.map { it.album }.distinct().sorted() }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .testTag("library_screen_list"),
        contentPadding = PaddingValues(
            start = tokens.spacing.screenMargin,
            end = tokens.spacing.screenMargin,
            top = tokens.spacing.sm,
            bottom = tokens.spacing.lg,
        ),
        verticalArrangement = Arrangement.spacedBy(tokens.spacing.md),
    ) {
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = stringResource(R.string.library_title),
                    style = MaterialTheme.typography.headlineMedium,
                    color = colors.onBackground,
                )
                IconButton(
                    onClick = {
                        scope.launch {
                            isScanning = true
                            libraryRepo.syncMediaStoreIncremental()
                            localTracks = libraryRepo.getAllTracksSnapshot()
                            isScanning = false
                            onShowFeedback("Biblioteca actualizada (${localTracks.size} pistas)")
                        }
                    },
                    modifier = Modifier.testTag("library_scan_btn"),
                ) {
                    Icon(Icons.Rounded.Refresh, contentDescription = stringResource(R.string.library_scan_btn))
                }
            }
        }

        item {
            MagicSearchBarPill(
                query = state.searchQuery,
                placeholder = stringResource(R.string.search_library_placeholder),
                onClick = {},
                onMicClick = {},
                onQueryChange = onSearchQueryChange,
            )
        }

        item {
            PrimaryTabRow(
                selectedTabIndex = selectedTab,
                containerColor = androidx.compose.ui.graphics.Color.Transparent,
            ) {
                Tab(
                    selected = selectedTab == 0,
                    onClick = { selectedTab = 0 },
                    text = { Text(stringResource(R.string.library_filter_tracks)) },
                    modifier = Modifier.testTag("library_tab_songs"),
                )
                Tab(
                    selected = selectedTab == 1,
                    onClick = { selectedTab = 1 },
                    text = { Text(stringResource(R.string.library_filter_playlists)) },
                    modifier = Modifier.testTag("library_tab_playlists"),
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
                    text = { Text(stringResource(R.string.library_filter_albums)) },
                    modifier = Modifier.testTag("library_tab_albums"),
                )
            }
        }

        if (!hasPermission) {
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
        } else if (selectedTab == 0) {
            if (filteredTracks.isNotEmpty()) {
                item {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Text(
                            text = "${filteredTracks.size} canciones indexadas",
                            style = MaterialTheme.typography.labelLarge,
                            color = colors.onSurfaceVariant,
                        )
                        FilledTonalButton(
                            onClick = {
                                MediaRepository.playLocalTracks(filteredTracks, 0, shuffle = true)
                                onShowFeedback("Reproduciendo biblioteca en aleatorio")
                            },
                        ) {
                            Icon(Icons.Rounded.PlayArrow, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Aleatorio")
                        }
                    }
                }

                items(filteredTracks, key = { it.id }) { track ->
                    MagicTrackListItem(
                        item = TrackListItemModel(
                            id = track.id.toString(),
                            title = track.title,
                            artist = "${track.artist} • ${track.album}",
                            durationLabel = formatTimeMs(track.durationMs),
                            accentTint = colors.primary,
                        ),
                        variant = TrackItemVariant.DEFAULT,
                        onClick = {
                            val idx = filteredTracks.indexOf(track).coerceAtLeast(0)
                            MediaRepository.playLocalTracks(filteredTracks, idx, shuffle = false)
                            onShowFeedback("Reproduciendo ${track.title}")
                        },
                    )
                }
            } else {
                item {
                    MagicSurfaceCard {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 16.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(8.dp),
                        ) {
                            Icon(Icons.Rounded.MusicNote, contentDescription = null, tint = colors.primary, modifier = Modifier.size(32.dp))
                            Text(
                                text = if (isScanning) "Escaneando almacenamiento..." else "No se encontraron canciones locales",
                                style = MaterialTheme.typography.titleMedium,
                                color = colors.onSurface,
                            )
                            Text(
                                text = "Asegúrate de tener archivos de audio (FLAC, MP3, WAV, AAC) en tu dispositivo.",
                                style = MaterialTheme.typography.bodySmall,
                                color = colors.onSurfaceVariant,
                            )
                        }
                    }
                }
            }
        } else if (selectedTab == 1) {
            item {
                MagicSurfaceCard {
                    Text("Listas de reproducción", style = MaterialTheme.typography.titleMedium, color = colors.onSurface)
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "Tus canciones favoritas y listas rápidas creadas en MagicMusic.",
                        style = MaterialTheme.typography.bodySmall,
                        color = colors.onSurfaceVariant,
                    )
                }
            }
        } else if (selectedTab == 2) {
            item {
                Text("${artists.size} artistas encontrados", style = MaterialTheme.typography.labelLarge, color = colors.onSurfaceVariant)
            }
            items(artists, key = { it }) { artist ->
                val count = localTracks.count { it.artist == artist }
                Surface(
                    shape = RoundedCornerShape(tokens.radii.listItem),
                    color = tokens.semantic.cardSurface,
                    border = BorderStroke(1.dp, tokens.semantic.divider),
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Row(
                        modifier = Modifier.padding(14.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Box(
                            modifier = Modifier
                                .size(42.dp)
                                .clip(CircleShape)
                                .background(colors.primary.copy(alpha = 0.16f)),
                            contentAlignment = Alignment.Center,
                        ) {
                            Icon(Icons.Rounded.Person, contentDescription = null, tint = colors.primary)
                        }
                        Spacer(modifier = Modifier.width(12.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(text = artist, style = MaterialTheme.typography.titleSmall, color = colors.onSurface)
                            Text(text = "$count pistas", style = MaterialTheme.typography.bodySmall, color = colors.onSurfaceVariant)
                        }
                    }
                }
            }
        } else {
            item {
                Text("${albums.size} álbumes encontrados", style = MaterialTheme.typography.labelLarge, color = colors.onSurfaceVariant)
            }
            items(albums, key = { it }) { album ->
                val sampleTrack = localTracks.firstOrNull { it.album == album }
                Surface(
                    shape = RoundedCornerShape(tokens.radii.listItem),
                    color = tokens.semantic.cardSurface,
                    border = BorderStroke(1.dp, tokens.semantic.divider),
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Row(
                        modifier = Modifier.padding(14.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Box(
                            modifier = Modifier
                                .size(42.dp)
                                .clip(RoundedCornerShape(10.dp))
                                .background(colors.secondary.copy(alpha = 0.2f)),
                            contentAlignment = Alignment.Center,
                        ) {
                            Icon(Icons.Rounded.Album, contentDescription = null, tint = colors.secondary)
                        }
                        Spacer(modifier = Modifier.width(12.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(text = album, style = MaterialTheme.typography.titleSmall, color = colors.onSurface)
                            Text(text = sampleTrack?.artist ?: "Artista desconocido", style = MaterialTheme.typography.bodySmall, color = colors.onSurfaceVariant)
                        }
                    }
                }
            }
        }
    }
}
