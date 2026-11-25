package com.example.ui.screens

import android.content.res.Configuration
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.automirrored.rounded.QueueMusic
import androidx.compose.material.icons.outlined.GraphicEq
import androidx.compose.material.icons.outlined.LibraryMusic
import androidx.compose.material.icons.outlined.Palette
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material.icons.rounded.Bedtime
import androidx.compose.material.icons.rounded.DeleteSweep
import androidx.compose.material.icons.rounded.Favorite
import androidx.compose.material.icons.rounded.FavoriteBorder
import androidx.compose.material.icons.rounded.GraphicEq
import androidx.compose.material.icons.rounded.ImageSearch
import androidx.compose.material.icons.rounded.LibraryMusic
import androidx.compose.material.icons.rounded.Lyrics
import androidx.compose.material.icons.rounded.MoreVert
import androidx.compose.material.icons.rounded.Palette
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.example.core.artwork.LocalArtworkDiagnostics
import com.example.core.artwork.SessionArtworkDebugState
import com.example.data.CustomControlAction
import com.example.data.MusicServiceApp
import com.example.data.UnifiedMediaState
import com.example.designsystem.ArtworkPaletteColors
import com.example.designsystem.ArtworkPaletteExtractor
import com.example.designsystem.MagicMusicTheme
import com.example.designsystem.MagicTheme
import com.example.designsystem.SurfaceToneStyle
import com.example.designsystem.ThemeMode
import com.example.designsystem.ThemePreferences
import com.example.designsystem.VioletSeedColor
import com.example.designsystem.buildArtworkTonalSurface
import com.example.ui.components.CoverGridItem
import com.example.ui.components.EqualizerBand
import com.example.ui.components.EqualizerPreset
import com.example.ui.components.MagicAlbumArtwork
import com.example.ui.components.MagicBottomNavBar
import com.example.ui.components.MagicContextMenuSheet
import com.example.ui.components.MagicEqualizerPanel
import com.example.ui.components.MagicFilterChip
import com.example.ui.components.MagicHorizontalCoverGrid
import com.example.ui.components.MagicLyricsSheet
import com.example.ui.components.MagicMiniPlayer
import com.example.ui.components.MagicPlaybackControls
import com.example.ui.components.MagicSearchBarPill
import com.example.ui.components.MagicSettingsRowCard
import com.example.ui.components.MagicSettingsSwitchRow
import com.example.ui.components.MagicSleepTimerSheet
import com.example.ui.components.MagicSurfaceCard
import com.example.ui.components.MagicTrackListItem
import com.example.ui.components.MagicWidePrimaryButton
import com.example.ui.components.NavItemUiModel
import com.example.ui.components.TrackItemVariant
import com.example.ui.components.TrackListItemModel

enum class ShowcaseDestination(val title: String) {
    GALLERY("Componentes M3 Expressive"),
    ARTWORK_INSPECTOR("Inspector de Carátulas"),
    TEST_HOME("Prueba 1 · Biblioteca"),
    TEST_NOW_PLAYING("Prueba 2 · Ahora suena"),
    TEST_EQUALIZER("Prueba 3 · Ecualizador"),
}

private enum class PreviewVisualVariant(val label: String) {
    FOLLOW_APP("Actual"),
    DARK("Oscuro"),
    TONAL("Tonal"),
    LIGHT("Claro"),
    AMOLED("AMOLED"),
}

@Composable
fun DesignShowcaseHostScreen(
    destination: ShowcaseDestination,
    appThemePreferences: ThemePreferences,
    media: UnifiedMediaState,
    artworkColors: ArtworkPaletteColors,
    localArtworkDiagnostics: LocalArtworkDiagnostics = LocalArtworkDiagnostics(),
    sessionArtworkDebug: SessionArtworkDebugState = SessionArtworkDebugState(),
    onClearArtworkCache: () -> Unit = {},
    onPlayPause: () -> Unit,
    onNext: () -> Unit,
    onPrevious: () -> Unit,
    onSeek: (Long) -> Unit,
    onCustomAction: (CustomControlAction) -> Unit,
    onSelectSession: (String) -> Unit,
    onOpenActiveApp: (String) -> Unit,
    onDestinationChange: (ShowcaseDestination) -> Unit,
    onBack: () -> Unit,
) {
    BackHandler(onBack = onBack)

    var previewVariant by remember { mutableStateOf(PreviewVisualVariant.FOLLOW_APP) }
    val effectivePrefs = remember(previewVariant, appThemePreferences) {
        when (previewVariant) {
            PreviewVisualVariant.FOLLOW_APP -> appThemePreferences
            PreviewVisualVariant.DARK -> appThemePreferences.copy(
                themeMode = ThemeMode.DARK,
                surfaceToneStyle = SurfaceToneStyle.DARK,
                pureAmoledBlack = false,
            )
            PreviewVisualVariant.TONAL -> appThemePreferences.copy(
                themeMode = ThemeMode.DARK,
                surfaceToneStyle = SurfaceToneStyle.TONAL,
                pureAmoledBlack = false,
            )
            PreviewVisualVariant.LIGHT -> appThemePreferences.copy(
                themeMode = ThemeMode.LIGHT,
                surfaceToneStyle = SurfaceToneStyle.LIGHT,
                pureAmoledBlack = false,
            )
            PreviewVisualVariant.AMOLED -> appThemePreferences.copy(
                themeMode = ThemeMode.DARK,
                surfaceToneStyle = SurfaceToneStyle.DARK,
                pureAmoledBlack = true,
            )
        }
    }

    MagicMusicTheme(themePreferences = effectivePrefs) {
        val tokens = MagicTheme.tokens
        val colors = MaterialTheme.colorScheme

        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(tokens.backgroundColor)
                .testTag("showcase_container"),
        ) {
            // Top bar + instant mode switcher (Oscuro / Tonal / Claro / AMOLED)
            Surface(
                color = tokens.semantic.navBarContainer,
                border = BorderStroke(1.dp, tokens.semantic.divider),
            ) {
                Column(modifier = Modifier.fillMaxWidth().padding(bottom = 10.dp)) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 8.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        IconButton(
                            onClick = onBack,
                            modifier = Modifier.testTag("showcase_back_btn"),
                        ) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Rounded.ArrowBack,
                                contentDescription = "Volver",
                                tint = colors.onSurface,
                            )
                        }
                        Text(
                            text = destination.title,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = colors.onSurface,
                            modifier = Modifier.weight(1f),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                    }

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState())
                            .padding(horizontal = tokens.spacing.screenMargin),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        PreviewVisualVariant.entries.forEach { variant ->
                            MagicFilterChip(
                                label = variant.label,
                                selected = previewVariant == variant,
                                onClick = { previewVariant = variant },
                                modifier = Modifier.testTag("preview_variant_${variant.name.lowercase()}"),
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState())
                            .padding(horizontal = tokens.spacing.screenMargin),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        ShowcaseDestination.entries.forEach { dest ->
                            MagicFilterChip(
                                label = dest.title.substringAfter("· ").ifBlank { dest.title },
                                selected = destination == dest,
                                onClick = { onDestinationChange(dest) },
                            )
                        }
                    }
                }
            }

            Box(modifier = Modifier.weight(1f).fillMaxWidth()) {
                when (destination) {
                    ShowcaseDestination.GALLERY -> ComponentsGalleryView(
                        media = media,
                        onPlayPause = onPlayPause,
                        onNext = onNext,
                        onPrevious = onPrevious,
                    )
                    ShowcaseDestination.ARTWORK_INSPECTOR -> ArtworkInspectorView(
                        media = media,
                        localDiagnostics = localArtworkDiagnostics,
                        sessionDebug = sessionArtworkDebug,
                        onClearArtworkCache = onClearArtworkCache,
                    )
                    ShowcaseDestination.TEST_HOME -> TestHomePreviewScreen(
                        media = media,
                        onPlayPause = onPlayPause,
                        onNext = onNext,
                        onSelectSession = onSelectSession,
                        onOpenNowPlaying = { onDestinationChange(ShowcaseDestination.TEST_NOW_PLAYING) },
                    )
                    ShowcaseDestination.TEST_NOW_PLAYING -> TestNowPlayingPreviewScreen(
                        media = media,
                        artworkColors = artworkColors,
                        useArtworkTint = effectivePrefs.colorFromArtwork,
                        onPlayPause = onPlayPause,
                        onNext = onNext,
                        onPrevious = onPrevious,
                        onSeek = onSeek,
                        onCustomAction = onCustomAction,
                        onOpenActiveApp = onOpenActiveApp,
                    )
                    ShowcaseDestination.TEST_EQUALIZER -> TestEqualizerPreviewScreen(
                        media = media,
                        onPlayPause = onPlayPause,
                        onNext = onNext,
                    )
                }
            }
        }
    }
}

@Composable
fun ComponentsGalleryView(
    media: UnifiedMediaState,
    onPlayPause: () -> Unit,
    onNext: () -> Unit,
    onPrevious: () -> Unit,
) {
    val tokens = MagicTheme.tokens
    val colors = MaterialTheme.colorScheme
    var selectedFilter by remember { mutableIntStateOf(0) }
    var switchSample by remember { mutableStateOf(true) }
    var showContextSheet by remember { mutableStateOf(false) }
    var showLyricsSheet by remember { mutableStateOf(false) }
    var showTimerSheet by remember { mutableStateOf(false) }
    var selectedTimerMin by remember { mutableIntStateOf(30) }
    var fadeOut by remember { mutableStateOf(true) }
    var shuffleOn by remember { mutableStateOf(false) }
    var repeatOn by remember { mutableStateOf(false) }
    var eqPreset by remember { mutableStateOf(EqualizerPreset.VOCAL) }
    var eqAdvanced by remember { mutableStateOf(false) }
    val eqBands = remember {
        mutableStateListOf(
            EqualizerBand("60Hz", 2f),
            EqualizerBand("230Hz", 4f),
            EqualizerBand("910Hz", 1f),
            EqualizerBand("3.6kHz", 5f),
            EqualizerBand("14kHz", 3f),
        )
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .testTag("components_gallery_list"),
        contentPadding = PaddingValues(vertical = tokens.spacing.md),
        verticalArrangement = Arrangement.spacedBy(18.dp),
    ) {
        item {
            Column(
                modifier = Modifier.padding(horizontal = tokens.spacing.screenMargin),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                SectionLabel("Barra de búsqueda píldora y chips de filtro")
                MagicSearchBarPill(
                    query = "",
                    placeholder = "Buscar canciones, artistas o sesiones…",
                    onClick = {},
                    onMicClick = {},
                )
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.horizontalScroll(rememberScrollState()),
                ) {
                    listOf("Todo", "Spotify", "YouTube Music", "Motor Local").forEachIndexed { idx, label ->
                        MagicFilterChip(
                            label = label,
                            selected = selectedFilter == idx,
                            onClick = { selectedFilter = idx },
                        )
                    }
                }
            }
        }

        item {
            Column(
                modifier = Modifier.padding(horizontal = tokens.spacing.screenMargin),
                verticalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                SectionLabel("Botón principal ancho (lila)")
                MagicWidePrimaryButton(
                    text = "Aleatorio",
                    onClick = onPlayPause,
                )
            }
        }

        item {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text(
                    text = "Carátula 24dp y cuadrícula horizontal",
                    style = MaterialTheme.typography.titleMedium,
                    color = colors.onBackground,
                    modifier = Modifier.padding(horizontal = tokens.spacing.screenMargin),
                )
                MagicHorizontalCoverGrid(
                    items = sampleCoverItems(media),
                    onItemClick = {},
                )
            }
        }

        item {
            Column(
                modifier = Modifier.padding(horizontal = tokens.spacing.screenMargin),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                SectionLabel("Elementos de pista (4 variantes)")
                MagicTrackListItem(
                    item = TrackListItemModel("t1", "Marea Alta", "Brisa y Horizonte", "3:18"),
                    variant = TrackItemVariant.DEFAULT,
                    onClick = {},
                    onMoreClick = { showContextSheet = true },
                )
                MagicTrackListItem(
                    item = TrackListItemModel("t2", media.title.ifBlank { "Resonancia Nocturna" }, media.artist, "4:02", bitmap = media.albumArt),
                    variant = TrackItemVariant.HIGHLIGHTED_CURRENT,
                    onClick = {},
                    onMoreClick = { showContextSheet = true },
                )
                MagicTrackListItem(
                    item = TrackListItemModel("t3", "Luz de Mañana", "Noche de Tenerife", "2:54", trackNumber = 3),
                    variant = TrackItemVariant.NUMBERED,
                    onClick = {},
                )
                MagicTrackListItem(
                    item = TrackListItemModel("t4", "Eco Mineral", "Studio Master", "3:45"),
                    variant = TrackItemVariant.QUEUE_DRAG,
                    onClick = {},
                )
            }
        }

        item {
            Column(
                modifier = Modifier.padding(horizontal = tokens.spacing.screenMargin),
                verticalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                SectionLabel("Controles de reproducción y barra de progreso")
                MagicSurfaceCard {
                    MagicPlaybackControls(
                        positionMs = if (media.hasActiveMedia) media.positionMs else 95_000L,
                        durationMs = if (media.hasActiveMedia && media.durationMs > 0L) media.durationMs else 210_000L,
                        isPlaying = media.isPlaying,
                        shuffleEnabled = shuffleOn,
                        repeatEnabled = repeatOn,
                        onSeekFraction = {},
                        onPlayPause = onPlayPause,
                        onPrevious = onPrevious,
                        onNext = onNext,
                        onToggleShuffle = { shuffleOn = !shuffleOn },
                        onToggleRepeat = { repeatOn = !repeatOn },
                    )
                }
            }
        }

        item {
            Column(
                modifier = Modifier.padding(horizontal = tokens.spacing.screenMargin),
                verticalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                SectionLabel("Mini-reproductor flotante y barra de navegación M3")
                MagicMiniPlayer(
                    media = media,
                    onPlayPause = onPlayPause,
                    onNext = onNext,
                    onClick = {},
                )
                Surface(
                    shape = RoundedCornerShape(tokens.radii.card),
                    border = BorderStroke(1.dp, tokens.semantic.divider),
                ) {
                    MagicBottomNavBar(
                        items = listOf(
                            NavItemUiModel("library", "Biblioteca", Icons.Rounded.LibraryMusic, Icons.Outlined.LibraryMusic, "prev_nav_library"),
                            NavItemUiModel("equalizer", "Ecualizador", Icons.Rounded.GraphicEq, Icons.Outlined.GraphicEq, "prev_nav_eq"),
                            NavItemUiModel("style", "Estilo", Icons.Rounded.Palette, Icons.Outlined.Palette, "prev_nav_style"),
                            NavItemUiModel("settings", "Ajustes", Icons.Rounded.Settings, Icons.Outlined.Settings, "prev_nav_settings"),
                        ),
                        selectedId = "library",
                        onSelect = {},
                    )
                }
            }
        }

        item {
            Column(
                modifier = Modifier.padding(horizontal = tokens.spacing.screenMargin),
                verticalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                SectionLabel("Ecualizador con escala dB vertical")
                MagicEqualizerPanel(
                    bands = eqBands,
                    selectedPreset = eqPreset,
                    isAdvancedMode = eqAdvanced,
                    onBandChange = { idx, db -> eqBands[idx] = eqBands[idx].copy(gainDb = db) },
                    onSelectPreset = { preset ->
                        eqPreset = preset
                        preset.gains.forEachIndexed { idx, gain ->
                            eqBands[idx] = eqBands[idx].copy(gainDb = gain)
                        }
                    },
                    onModeChange = { eqAdvanced = it },
                )
            }
        }

        item {
            Column(
                modifier = Modifier.padding(horizontal = tokens.spacing.screenMargin),
                verticalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                SectionLabel("Hojas inferiores y ajustes")
                MagicSettingsRowCard(
                    icon = Icons.Rounded.MoreVert,
                    title = "Abrir Menú contextual",
                    subtitle = "ModalBottomSheet con esquinas de 28dp y acciones de pista",
                    onClick = { showContextSheet = true },
                )
                MagicSettingsRowCard(
                    icon = Icons.Rounded.Lyrics,
                    title = "Abrir Letras",
                    subtitle = "Panel deslizable con estrofa destacada",
                    onClick = { showLyricsSheet = true },
                )
                MagicSettingsRowCard(
                    icon = Icons.Rounded.Bedtime,
                    title = "Abrir Temporizador",
                    subtitle = "Selector rápido de apagado y fundido suave",
                    onClick = { showTimerSheet = true },
                )
                MagicSurfaceCard {
                    MagicSettingsSwitchRow(
                        title = "Interruptor Material 3",
                        subtitle = "Alineado con tokens de superficie tonal",
                        checked = switchSample,
                        onCheckedChange = { switchSample = it },
                    )
                }
            }
        }
    }

    if (showContextSheet) {
        MagicContextMenuSheet(
            title = if (media.hasActiveMedia) media.title else "Marea Alta",
            artist = if (media.hasActiveMedia) media.artist else "Brisa y Horizonte",
            bitmap = media.albumArt,
            onDismiss = { showContextSheet = false },
            onActionSelected = {},
        )
    }

    if (showLyricsSheet) {
        MagicLyricsSheet(
            title = if (media.hasActiveMedia) media.title else "Marea Alta",
            artist = if (media.hasActiveMedia) media.artist else "Brisa y Horizonte",
            lines = sampleLyricsLines(),
            activeLineIndex = 2,
            onDismiss = { showLyricsSheet = false },
        )
    }

    if (showTimerSheet) {
        MagicSleepTimerSheet(
            selectedMinutes = selectedTimerMin,
            fadeOutEnabled = fadeOut,
            onSelectMinutes = { selectedTimerMin = it },
            onToggleFadeOut = { fadeOut = it },
            onDismiss = { showTimerSheet = false },
        )
    }
}

@Composable
fun TestHomePreviewScreen(
    media: UnifiedMediaState,
    onPlayPause: () -> Unit,
    onNext: () -> Unit,
    onSelectSession: (String) -> Unit,
    onOpenNowPlaying: () -> Unit,
) {
    val tokens = MagicTheme.tokens
    val colors = MaterialTheme.colorScheme
    var activeFilter by remember { mutableStateOf("Todo") }

    Box(modifier = Modifier.fillMaxSize().testTag("test_screen_home")) {
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(top = 12.dp, bottom = 92.dp),
            verticalArrangement = Arrangement.spacedBy(18.dp),
        ) {
            item {
                Column(
                    modifier = Modifier.padding(horizontal = tokens.spacing.screenMargin),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    Text(
                        text = "Inicio",
                        style = MaterialTheme.typography.headlineLarge,
                        color = colors.onBackground,
                    )
                    MagicSearchBarPill(
                        query = "",
                        placeholder = "Buscar sesiones o reproductores…",
                        onClick = {},
                        onMicClick = {},
                    )
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.horizontalScroll(rememberScrollState()),
                    ) {
                        listOf("Todo", "Spotify", "YouTube Music", "Amazon Music", "Local").forEach { chip ->
                            MagicFilterChip(
                                label = chip,
                                selected = activeFilter == chip,
                                onClick = { activeFilter = chip },
                            )
                        }
                    }
                }
            }

            item {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text(
                        text = "Sesiones recientes",
                        style = MaterialTheme.typography.titleMedium,
                        color = colors.onBackground,
                        modifier = Modifier.padding(horizontal = tokens.spacing.screenMargin),
                    )
                    MagicHorizontalCoverGrid(
                        items = sampleCoverItems(media),
                        onItemClick = { item ->
                            val svc = MusicServiceApp.entries.firstOrNull { it.displayName == item.subtitle }
                            if (svc != null && svc.packageName.isNotBlank()) {
                                onSelectSession(svc.packageName)
                            } else {
                                onOpenNowPlaying()
                            }
                        },
                    )
                }
            }

            item {
                Column(
                    modifier = Modifier.padding(horizontal = tokens.spacing.screenMargin),
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    Text(
                        text = "Reproductores en segundo plano",
                        style = MaterialTheme.typography.titleMedium,
                        color = colors.onBackground,
                    )
                    val sessions = if (media.activeSessions.isNotEmpty()) {
                        media.activeSessions.mapIndexed { idx, s ->
                            TrackListItemModel(
                                id = s.packageName,
                                title = s.title,
                                artist = "${s.artist} • ${s.service.displayName}",
                                trackNumber = idx + 1,
                                accentTint = s.service.accentColor,
                            ) to s.isSelected
                        }
                    } else {
                        listOf(
                            TrackListItemModel("s1", "Marea Alta", "Spotify • Sesión lista", "3:18", accentTint = MusicServiceApp.SPOTIFY.accentColor) to true,
                            TrackListItemModel("s2", "Horizonte Violeta", "Motor Local Hi-Res • 24-bit", "3:30", accentTint = VioletSeedColor) to false,
                            TrackListItemModel("s3", "Brisa Nocturna", "YouTube Music • En pausa", "4:12", accentTint = MusicServiceApp.YOUTUBE_MUSIC.accentColor) to false,
                        )
                    }
                    sessions.forEach { (model, isCurrent) ->
                        MagicTrackListItem(
                            item = model,
                            variant = if (isCurrent) TrackItemVariant.HIGHLIGHTED_CURRENT else TrackItemVariant.DEFAULT,
                            onClick = onOpenNowPlaying,
                            onMoreClick = {},
                        )
                    }
                }
            }
        }

        MagicMiniPlayer(
            media = media,
            onPlayPause = onPlayPause,
            onNext = onNext,
            onClick = onOpenNowPlaying,
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(horizontal = tokens.spacing.md, vertical = 10.dp),
        )
    }
}

@Composable
fun TestNowPlayingPreviewScreen(
    media: UnifiedMediaState,
    artworkColors: ArtworkPaletteColors,
    useArtworkTint: Boolean,
    onPlayPause: () -> Unit,
    onNext: () -> Unit,
    onPrevious: () -> Unit,
    onSeek: (Long) -> Unit,
    onCustomAction: (CustomControlAction) -> Unit,
    onOpenActiveApp: (String) -> Unit,
) {
    val tokens = MagicTheme.tokens
    val colors = MaterialTheme.colorScheme
    var liked by remember { mutableStateOf(false) }
    var shuffleOn by remember { mutableStateOf(false) }
    var repeatOn by remember { mutableStateOf(false) }
    var showLyricsSheet by remember { mutableStateOf(false) }
    var showContextSheet by remember { mutableStateOf(false) }
    var showTimerSheet by remember { mutableStateOf(false) }
    var selectedTimerMin by remember { mutableIntStateOf(30) }
    var fadeOut by remember { mutableStateOf(true) }

    val bgColor = if (useArtworkTint) {
        buildArtworkTonalSurface(
            artworkColors = artworkColors,
            baseBackground = colors.background,
            isDark = tokens.isDark,
        )
    } else {
        tokens.backgroundColor
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(bgColor)
            .testTag("test_screen_now_playing"),
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = tokens.spacing.screenMargin, vertical = 14.dp),
            verticalArrangement = Arrangement.SpaceBetween,
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            // Top source header
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Column {
                    Text(
                        text = if (media.hasActiveMedia) media.service.displayName.uppercase() else "REPRODUCIENDO DESDE",
                        style = MaterialTheme.typography.labelMedium,
                        color = colors.onSurfaceVariant,
                    )
                    Text(
                        text = if (media.hasActiveMedia) media.album else "Noche de Tenerife",
                        style = MaterialTheme.typography.titleSmall,
                        color = colors.onBackground,
                    )
                }
                IconButton(onClick = { showContextSheet = true }) {
                    Icon(
                        imageVector = Icons.Rounded.MoreVert,
                        contentDescription = "Menú contextual",
                        tint = colors.onBackground,
                    )
                }
            }

            // Large 28dp rounded artwork
            MagicAlbumArtwork(
                bitmap = media.albumArt,
                contentDescription = "Carátula principal",
                cornerRadius = tokens.radii.artworkLarge,
                fallbackTint = artworkColors.dominant,
                modifier = Modifier
                    .fillMaxWidth(0.84f)
                    .height(276.dp),
            )

            // Title, artist + favorite
            Column(modifier = Modifier.fillMaxWidth()) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = if (media.hasActiveMedia) media.title else "Marea Alta",
                            style = MaterialTheme.typography.headlineSmall,
                            fontWeight = FontWeight.Bold,
                            color = colors.onBackground,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                        Text(
                            text = if (media.hasActiveMedia) media.artist else "Brisa y Horizonte",
                            style = MaterialTheme.typography.bodyLarge,
                            color = colors.onSurfaceVariant,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                    }
                    IconButton(onClick = { liked = !liked }) {
                        Icon(
                            imageVector = if (liked) Icons.Rounded.Favorite else Icons.Rounded.FavoriteBorder,
                            contentDescription = "Me gusta",
                            tint = if (liked) colors.primary else colors.onSurfaceVariant,
                        )
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                MagicPlaybackControls(
                    positionMs = if (media.hasActiveMedia) media.positionMs else 95_000L,
                    durationMs = if (media.hasActiveMedia && media.durationMs > 0L) media.durationMs else 210_000L,
                    isPlaying = media.isPlaying,
                    shuffleEnabled = shuffleOn,
                    repeatEnabled = repeatOn,
                    onSeekFraction = { frac ->
                        if (media.durationMs > 0L) onSeek((frac * media.durationMs).toLong())
                    },
                    onPlayPause = onPlayPause,
                    onPrevious = onPrevious,
                    onNext = onNext,
                    onToggleShuffle = { shuffleOn = !shuffleOn },
                    onToggleRepeat = { repeatOn = !repeatOn },
                    onRewind15 = { onCustomAction(CustomControlAction.REWIND_15) },
                    onForward15 = { onCustomAction(CustomControlAction.FORWARD_15) },
                    onVolumeClick = { onCustomAction(CustomControlAction.MUTE_AUDIO) },
                    onOutputClick = { showTimerSheet = true },
                )
            }

            // Bottom quick tools matching reference 02 · Ahora suena (Letras / Cola / Temporizador)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                MagicFilterChip(
                    label = "Letras",
                    selected = showLyricsSheet,
                    leadingIcon = Icons.Rounded.Lyrics,
                    onClick = { showLyricsSheet = true },
                )
                Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    IconButton(onClick = { showTimerSheet = true }) {
                        Icon(
                            imageVector = Icons.Rounded.Bedtime,
                            contentDescription = "Temporizador",
                            tint = colors.onSurfaceVariant,
                        )
                    }
                    IconButton(onClick = { showContextSheet = true }) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Rounded.QueueMusic,
                            contentDescription = "Cola y opciones",
                            tint = colors.onSurfaceVariant,
                        )
                    }
                }
            }
        }
    }

    if (showContextSheet) {
        MagicContextMenuSheet(
            title = if (media.hasActiveMedia) media.title else "Marea Alta",
            artist = if (media.hasActiveMedia) media.artist else "Brisa y Horizonte",
            bitmap = media.albumArt,
            onDismiss = { showContextSheet = false },
            onActionSelected = { action ->
                if (action.contains("origen") && media.packageName.isNotBlank()) {
                    onOpenActiveApp(media.packageName)
                }
            },
        )
    }

    if (showLyricsSheet) {
        MagicLyricsSheet(
            title = if (media.hasActiveMedia) media.title else "Marea Alta",
            artist = if (media.hasActiveMedia) media.artist else "Brisa y Horizonte",
            lines = sampleLyricsLines(),
            activeLineIndex = 2,
            onDismiss = { showLyricsSheet = false },
        )
    }

    if (showTimerSheet) {
        MagicSleepTimerSheet(
            selectedMinutes = selectedTimerMin,
            fadeOutEnabled = fadeOut,
            onSelectMinutes = { selectedTimerMin = it },
            onToggleFadeOut = { fadeOut = it },
            onDismiss = { showTimerSheet = false },
        )
    }
}

@Composable
fun TestEqualizerPreviewScreen(
    media: UnifiedMediaState,
    onPlayPause: () -> Unit,
    onNext: () -> Unit,
) {
    val tokens = MagicTheme.tokens
    val colors = MaterialTheme.colorScheme
    var eqPreset by remember { mutableStateOf(EqualizerPreset.FLAT) }
    var isAdvanced by remember { mutableStateOf(false) }
    val bands = remember {
        mutableStateListOf(
            EqualizerBand("60Hz", 3f),
            EqualizerBand("230Hz", 1f),
            EqualizerBand("910Hz", -1f),
            EqualizerBand("3.6kHz", 4f),
            EqualizerBand("14kHz", 2f),
        )
    }

    Box(modifier = Modifier.fillMaxSize().testTag("test_screen_equalizer")) {
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(
                start = tokens.spacing.screenMargin,
                end = tokens.spacing.screenMargin,
                top = 14.dp,
                bottom = 96.dp,
            ),
            verticalArrangement = Arrangement.spacedBy(18.dp),
        ) {
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        text = "Ecualizador",
                        style = MaterialTheme.typography.headlineLarge,
                        color = colors.onBackground,
                    )
                    Surface(
                        shape = CircleShape,
                        color = colors.secondaryContainer,
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.GraphicEq,
                                contentDescription = null,
                                tint = colors.onSecondaryContainer,
                                modifier = Modifier.size(16.dp),
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "Motor 24-bit",
                                style = MaterialTheme.typography.labelMedium,
                                color = colors.onSecondaryContainer,
                            )
                        }
                    }
                }
            }

            item {
                MagicEqualizerPanel(
                    bands = bands,
                    selectedPreset = eqPreset,
                    isAdvancedMode = isAdvanced,
                    onBandChange = { idx, db ->
                        bands[idx] = bands[idx].copy(gainDb = db)
                    },
                    onSelectPreset = { preset ->
                        eqPreset = preset
                        preset.gains.forEachIndexed { idx, gain ->
                            bands[idx] = bands[idx].copy(gainDb = gain)
                        }
                    },
                    onModeChange = { isAdvanced = it },
                )
            }

            item {
                MagicSurfaceCard {
                    Text(
                        text = "Perfil activo: ${eqPreset.label}",
                        style = MaterialTheme.typography.titleSmall,
                        color = colors.onSurface,
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "Los ajustes de bandas y presets aplican al perfil de salida local sin alterar las sesiones de apps externas.",
                        style = MaterialTheme.typography.bodySmall,
                        color = colors.onSurfaceVariant,
                    )
                }
            }
        }

        MagicMiniPlayer(
            media = media,
            onPlayPause = onPlayPause,
            onNext = onNext,
            onClick = {},
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(horizontal = tokens.spacing.md, vertical = 10.dp),
        )
    }
}

@Composable
private fun SectionLabel(text: String) {
    Text(
        text = text,
        style = MaterialTheme.typography.titleMedium,
        color = MaterialTheme.colorScheme.onBackground,
    )
}

private fun sampleCoverItems(media: UnifiedMediaState): List<CoverGridItem> = listOf(
    CoverGridItem(
        id = "c1",
        title = if (media.hasActiveMedia) media.title else "Marea Alta",
        subtitle = MusicServiceApp.SPOTIFY.displayName,
        accentTint = MusicServiceApp.SPOTIFY.accentColor,
        bitmap = media.albumArt,
    ),
    CoverGridItem(
        id = "c2",
        title = "Horizonte Violeta",
        subtitle = MusicServiceApp.YOUTUBE_MUSIC.displayName,
        accentTint = MusicServiceApp.YOUTUBE_MUSIC.accentColor,
    ),
    CoverGridItem(
        id = "c3",
        title = "Ruta Nocturna",
        subtitle = MusicServiceApp.AMAZON_MUSIC.displayName,
        accentTint = MusicServiceApp.AMAZON_MUSIC.accentColor,
    ),
    CoverGridItem(
        id = "c4",
        title = "Pulso Mineral",
        subtitle = MusicServiceApp.LOCAL_ENGINE.displayName,
        accentTint = VioletSeedColor,
    ),
)

private fun sampleLyricsLines(): List<String> = listOf(
    "En la inmensidad de la noche calma,",
    "suenan las ondas entre luz violeta.",
    "Sigue el pulso lento sobre la marea,",
    "mientras todo el ruido se vuelve silencio.",
)

@Composable
private fun ArtworkInspectorView(
    media: UnifiedMediaState,
    localDiagnostics: LocalArtworkDiagnostics,
    sessionDebug: SessionArtworkDebugState,
    onClearArtworkCache: () -> Unit,
) {
    val colors = MaterialTheme.colorScheme
    val tokens = MagicTheme.tokens
    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .testTag("artwork_inspector_list"),
        contentPadding = PaddingValues(tokens.spacing.screenMargin),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        item {
            MagicSurfaceCard {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    MagicAlbumArtwork(
                        bitmap = media.albumArt,
                        artworkModel = media.albumArt ?: media.albumArtUri,
                        contentDescription = "Carátula inspeccionada",
                        cornerRadius = 16.dp,
                        fallbackTint = colors.primary,
                        modifier = Modifier.size(64.dp),
                    )
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Sesión MediaSession activa",
                            style = MaterialTheme.typography.titleMedium,
                            color = colors.onSurface,
                        )
                        Text(
                            text = "Estado estabilizador: ${sessionDebug.status.name}",
                            style = MaterialTheme.typography.bodySmall,
                            color = colors.primary,
                        )
                        Text(
                            text = "TrackKey: ${sessionDebug.activeTrackKey.ifBlank { "—" }}",
                            style = MaterialTheme.typography.labelSmall,
                            color = colors.onSurfaceVariant,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                    }
                }
                Spacer(modifier = Modifier.height(10.dp))
                Text(
                    text = "Bitmap válido: ${sessionDebug.hasBitmap} (${sessionDebug.bitmapSizeLabel})\n" +
                        "URI: ${sessionDebug.artUri ?: "ninguna"}\n" +
                        "Aciertos KEEP_LAST_GOOD: ${sessionDebug.keepLastGoodHits} • Marcos uniformes rechazados: ${sessionDebug.rejectedUniformFrames} • Reintentos URI: ${sessionDebug.uriRetries}",
                    style = MaterialTheme.typography.bodySmall,
                    color = colors.onSurfaceVariant,
                )
            }
        }

        item {
            MagicSurfaceCard {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    Icon(Icons.Rounded.ImageSearch, contentDescription = null, tint = colors.primary)
                    Text(
                        text = "Pipeline Local Pre-Reproducción (Coil)",
                        style = MaterialTheme.typography.titleMedium,
                        color = colors.onSurface,
                    )
                }
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "Aciertos caché disco: ${localDiagnostics.cacheHits}\n" +
                        "Decodificaciones realizadas: ${localDiagnostics.decodedCount}\n" +
                        "Último origen resuelto: ${localDiagnostics.lastSource.name}\n" +
                        "Último tiempo decodificación: ${localDiagnostics.lastDecodeMs} ms (track #${localDiagnostics.lastTrackId})",
                    style = MaterialTheme.typography.bodySmall,
                    color = colors.onSurfaceVariant,
                )
                Spacer(modifier = Modifier.height(10.dp))
                FilledTonalButton(
                    onClick = onClearArtworkCache,
                    modifier = Modifier.fillMaxWidth().testTag("clear_artwork_cache_btn"),
                ) {
                    Icon(Icons.Rounded.DeleteSweep, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Vaciar caché de carátulas")
                }
            }
        }
    }
}

@Preview(name = "Test Biblioteca - Tonal", showBackground = true, uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
private fun TestHomePreview() {
    MagicMusicTheme(themePreferences = ThemePreferences()) {
        Box(modifier = Modifier.background(MagicTheme.tokens.backgroundColor)) {
            TestHomePreviewScreen(
                media = UnifiedMediaState(),
                onPlayPause = {},
                onNext = {},
                onSelectSession = {},
                onOpenNowPlaying = {},
            )
        }
    }
}

@Preview(name = "Test Ahora Suena - Tintado", showBackground = true, uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
private fun TestNowPlayingPreview() {
    MagicMusicTheme(themePreferences = ThemePreferences()) {
        TestNowPlayingPreviewScreen(
            media = UnifiedMediaState(),
            artworkColors = ArtworkPaletteExtractor.deriveFromFallbackSeed(VioletSeedColor),
            useArtworkTint = true,
            onPlayPause = {},
            onNext = {},
            onPrevious = {},
            onSeek = {},
            onCustomAction = {},
            onOpenActiveApp = {},
        )
    }
}

@Preview(name = "Test Ecualizador - Claro", showBackground = true)
@Composable
private fun TestEqualizerLightPreview() {
    val light = ThemePreferences(themeMode = ThemeMode.LIGHT, surfaceToneStyle = SurfaceToneStyle.LIGHT)
    MagicMusicTheme(themePreferences = light) {
        Box(modifier = Modifier.background(MagicTheme.tokens.backgroundColor)) {
            TestEqualizerPreviewScreen(
                media = UnifiedMediaState(),
                onPlayPause = {},
                onNext = {},
            )
        }
    }
}
