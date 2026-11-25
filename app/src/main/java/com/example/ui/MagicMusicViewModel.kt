package com.example.ui

import android.app.Application
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import androidx.paging.PagingData
import androidx.paging.cachedIn
import com.example.R
import com.example.core.accounts.MusicAccountsCoordinator
import com.example.core.accounts.MusicSourceId
import com.example.core.accounts.SourceAccountSnapshot
import com.example.core.accounts.SourceTrackSummary
import com.example.core.artwork.LocalArtworkDiagnostics
import com.example.core.artwork.LocalArtworkPipeline
import com.example.core.artwork.SessionArtworkDebugState
import com.example.core.compat.AndroidVersionCompat
import com.example.core.compat.CompatActionResult
import com.example.core.compat.OemBatteryGuide
import com.example.core.compat.OemCompat
import com.example.core.library.AlbumSummary
import com.example.core.library.ArtistSummary
import com.example.core.library.FolderSummary
import com.example.core.library.LibraryRepository
import com.example.core.library.LibraryScanWorker
import com.example.core.library.LocalTrackEntity
import com.example.core.library.PlaylistWithSummary
import com.example.core.library.TrackBookmarkEntity
import com.example.core.media.DspCapabilityReport
import com.example.core.media.HealthCheckId
import com.example.core.media.HiResHardwareReport
import com.example.core.media.ListenerConnectionState
import com.example.core.media.MediaRepository
import com.example.core.media.PlayerState
import com.example.core.media.SleepTimerState
import com.example.core.media.SystemHealthSummary
import com.example.data.AudioOutputCategory
import com.example.data.AudioOutputDevice
import com.example.data.CustomControlAction
import com.example.data.CustomEqPreset
import com.example.data.EqualizerConfig
import com.example.data.InstalledServiceStatus
import com.example.data.LibrarySortOption
import com.example.data.LibrarySourceFilter
import com.example.data.LocalListeningStats
import com.example.data.LockAndQsConfig
import com.example.data.LockPrivacyLevel
import com.example.data.MediaSessionPreferences
import com.example.data.PreferencesRepository
import com.example.data.QsTileMode
import com.example.data.QuickTileType
import com.example.data.SessionPriorityRule
import com.example.data.UnifiedMediaState
import com.example.data.WidgetConfig
import com.example.data.WidgetFormat
import com.example.data.WidgetInstanceConfig
import com.example.data.WidgetStyle
import com.example.data.WidgetTemplate
import com.example.designsystem.ArtworkPaletteColors
import com.example.designsystem.ArtworkPaletteExtractor
import com.example.designsystem.ColorRoleOverrides
import com.example.designsystem.ContrastPreset
import com.example.designsystem.McuSchemeVariant
import com.example.designsystem.SavedColorProfile
import com.example.designsystem.SurfaceToneStyle
import com.example.designsystem.ThemeMode
import com.example.designsystem.ThemePreferences
import com.example.designsystem.VioletSeedColor
import com.example.designsystem.expressive.ArtworkShapeStyle
import com.example.designsystem.expressive.GeometryPreset
import com.example.designsystem.expressive.MiniPlayerStyle
import com.example.designsystem.expressive.NavLabelVisibility
import com.example.media.MagicMediaBridgeService
import com.example.quicksettings.LockScreenDeckActivity
import com.example.quicksettings.MagicNextTrackTileService
import com.example.quicksettings.MagicPlayPauseTileService
import com.example.ui.components.EqualizerPreset
import com.example.widget.MagicWidgetCapsuleProvider
import com.example.widget.MagicWidgetCardProvider
import com.example.widget.MagicWidgetMiniProvider
import com.example.widget.MagicWidgetSquareProvider
import com.example.widget.MagicWidgetUpdater
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.mapLatest
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class MagicMusicUiState(
    val playerState: PlayerState = PlayerState(),
    val media: UnifiedMediaState = UnifiedMediaState(),
    val widgetConfig: WidgetConfig = WidgetConfig(),
    val lockAndQsConfig: LockAndQsConfig = LockAndQsConfig(),
    val sessionPreferences: MediaSessionPreferences = MediaSessionPreferences(),
    val equalizerConfig: EqualizerConfig = EqualizerConfig(),
    val listeningStats: LocalListeningStats = LocalListeningStats(),
    val widgetTemplates: List<WidgetTemplate> = emptyList(),
    val sleepTimer: SleepTimerState = SleepTimerState(),
    val systemHealth: SystemHealthSummary = SystemHealthSummary(),
    val audioOutputs: List<AudioOutputDevice> = emptyList(),
    val hiResReport: HiResHardwareReport = HiResHardwareReport(),
    val hasListenerPermission: Boolean = false,
    val hasAudioLibraryPermission: Boolean = false,
    val hasPostNotificationsPermission: Boolean = true,
    val onboardingCompleted: Boolean = false,
    val listenerState: ListenerConnectionState = ListenerConnectionState.PERMISSION_MISSING,
    val installedServices: List<InstalledServiceStatus> = emptyList(),
    val oemBatteryGuide: OemBatteryGuide? = null,
    val bridgeRunning: Boolean = false,
    val searchQuery: String = "",
    val librarySortOption: LibrarySortOption = LibrarySortOption.TITLE,
    val librarySourceFilter: LibrarySourceFilter = LibrarySourceFilter.ALL,
    val isLibraryScanning: Boolean = false,
    val localTrackCount: Int = 0,
    val feedbackMessage: String? = null,
)

@OptIn(ExperimentalCoroutinesApi::class)
class MagicMusicViewModel(application: Application) : AndroidViewModel(application) {

    private val prefsRepo = PreferencesRepository(application)
    private val libraryRepo = LibraryRepository(application)
    private val accountsCoordinator = MusicAccountsCoordinator(application)

    private val _feedbackMessage = MutableStateFlow<String?>(null)
    private val _searchQuery = MutableStateFlow("")
    private val _librarySort = MutableStateFlow(LibrarySortOption.TITLE)
    private val _librarySourceFilter = MutableStateFlow(LibrarySourceFilter.ALL)
    private val _isLibraryScanning = MutableStateFlow(false)
    private val _hasAudioPermission = MutableStateFlow(libraryRepo.hasAudioPermission())
    private val _spotifySearchResults = MutableStateFlow<List<SourceTrackSummary>>(emptyList())

    init {
        MediaRepository.init(application)
        viewModelScope.launch {
            accountsCoordinator.restoreAll()
            if (libraryRepo.hasAudioPermission()) {
                LibraryScanWorker.enqueueIncremental(application)
            }
        }
        viewModelScope.launch {
            accountsCoordinator.lastAccountMessage.collect { msg ->
                if (!msg.isNullOrBlank()) {
                    _feedbackMessage.value = msg
                    accountsCoordinator.clearMessage()
                }
            }
        }
    }

    // Isolated high-frequency progress stream so ticks don't trigger full UI recompositions
    val playbackProgressFlow: StateFlow<Pair<Long, Long>> = MediaRepository.playbackProgressFlow

    // Isolated ThemePreferences stream
    val themePreferences: StateFlow<ThemePreferences> = prefsRepo.themePreferencesFlow.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = ThemePreferences(),
    )

    val artworkColors: StateFlow<ArtworkPaletteColors> = MediaRepository.mediaState
        .mapLatest { media ->
            ArtworkPaletteExtractor.extractCached(
                cacheKey = "${media.packageName}_${media.album}",
                bitmap = media.albumArt,
                fallbackColor = if (media.hasActiveMedia) media.service.accentColor else VioletSeedColor,
            )
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = ArtworkPaletteExtractor.deriveFromFallbackSeed(VioletSeedColor),
        )

    // Library Paging 3 & Room streams
    val pagedLibraryTracks: Flow<PagingData<LocalTrackEntity>> = combine(
        _searchQuery,
        _librarySort,
    ) { q, sort -> q to sort }
        .flatMapLatest { (q, sort) ->
            libraryRepo.pagedTracks(q, sort)
        }
        .cachedIn(viewModelScope)

    val albumsFlow: StateFlow<List<AlbumSummary>> = _searchQuery
        .flatMapLatest { q -> libraryRepo.observeAlbums(q) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    val artistsFlow: StateFlow<List<ArtistSummary>> = _searchQuery
        .flatMapLatest { q -> libraryRepo.observeArtists(q) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    val foldersFlow: StateFlow<List<FolderSummary>> = _searchQuery
        .flatMapLatest { q -> libraryRepo.observeFolders(q) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    val playlistsFlow: StateFlow<List<PlaylistWithSummary>> = libraryRepo.observePlaylists()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    val mostPlayedFlow: StateFlow<List<LocalTrackEntity>> = libraryRepo.observeMostPlayed(25)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    val recentlyAddedFlow: StateFlow<List<LocalTrackEntity>> = libraryRepo.observeRecentlyAdded(25)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    val favoritesFlow: StateFlow<List<LocalTrackEntity>> = libraryRepo.observeFavorites()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    val recentlyPlayedLocalFlow: StateFlow<List<LocalTrackEntity>> = libraryRepo.observeRecentlyPlayed(12)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    val currentTrackBookmarksFlow: StateFlow<List<TrackBookmarkEntity>> = MediaRepository.mediaState
        .flatMapLatest { media ->
            val key = media.trackKey
            if (key.isBlank() || !media.hasActiveMedia) flowOf(emptyList())
            else libraryRepo.observeBookmarks(key)
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    // Color studio, widget instances & custom EQ presets
    val savedColorProfilesFlow: StateFlow<List<SavedColorProfile>> = prefsRepo.savedColorProfilesFlow
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    val widgetInstancesFlow: StateFlow<Map<Int, WidgetInstanceConfig>> = prefsRepo.widgetInstancesFlow
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyMap())

    val customEqPresetsFlow: StateFlow<List<CustomEqPreset>> = prefsRepo.customEqPresetsFlow
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    // Official Accounts
    val spotifyAccountState: StateFlow<SourceAccountSnapshot> = accountsCoordinator.spotify.state
    val appleMusicAccountState: StateFlow<SourceAccountSnapshot> = accountsCoordinator.appleMusic.state
    val youtubeAccountState: StateFlow<SourceAccountSnapshot> = accountsCoordinator.youtube.state
    val spotifySearchResults: StateFlow<List<SourceTrackSummary>> = _spotifySearchResults.asStateFlow()

    // Artwork diagnostics
    val localArtworkDiagnostics: StateFlow<LocalArtworkDiagnostics> = LocalArtworkPipeline.diagnostics
    val sessionArtworkDebug: StateFlow<SessionArtworkDebugState> = MediaRepository.sessionArtworkDebug

    fun currentDspCapabilityReport(): DspCapabilityReport = MediaRepository.currentDspCapabilityReport()

    fun pagedTracksForAlbum(albumId: Long): Flow<PagingData<LocalTrackEntity>> =
        libraryRepo.pagedTracksByAlbum(albumId).cachedIn(viewModelScope)

    fun pagedTracksForArtist(artistId: Long): Flow<PagingData<LocalTrackEntity>> =
        libraryRepo.pagedTracksByArtist(artistId).cachedIn(viewModelScope)

    fun pagedTracksForFolder(folderPath: String): Flow<PagingData<LocalTrackEntity>> =
        libraryRepo.pagedTracksByFolder(folderPath).cachedIn(viewModelScope)

    fun observePlaylistTracks(playlistId: Long): Flow<List<LocalTrackEntity>> =
        libraryRepo.observePlaylistTracks(playlistId)

    private data class SystemSnapshot(
        val hasListener: Boolean,
        val hasNotifications: Boolean,
        val services: List<InstalledServiceStatus>,
        val oemGuide: OemBatteryGuide?,
        val bridgeRunning: Boolean,
        val systemHealth: SystemHealthSummary,
        val audioOutputs: List<AudioOutputDevice>,
        val hiResReport: HiResHardwareReport,
        val sleepTimer: SleepTimerState,
        val searchQuery: String,
        val sortOption: LibrarySortOption,
        val sourceFilter: LibrarySourceFilter,
        val isScanning: Boolean,
        val hasAudioPerm: Boolean,
        val trackCount: Int,
        val feedback: String?,
    )

    private data class ConfigSnapshot(
        val widgetConfig: WidgetConfig,
        val lockConfig: LockAndQsConfig,
        val sessionPreferences: MediaSessionPreferences,
        val equalizerConfig: EqualizerConfig,
        val listeningStats: LocalListeningStats,
        val widgetTemplates: List<WidgetTemplate>,
        val onboardingCompleted: Boolean,
    )

    private val permissionsAndServicesFlow = combine(
        MediaRepository.hasListenerPermission,
        MediaRepository.hasPostNotificationsPermission,
        MediaRepository.installedServices,
        MediaRepository.oemBatteryGuide,
        MediaRepository.bridgeActive,
    ) { hasListener, hasNotif, services, oemGuide, bridge ->
        listOf(hasListener, hasNotif, services, oemGuide, bridge)
    }

    private val audioAndQueryFlow = combine(
        MediaRepository.systemHealth,
        MediaRepository.audioOutputs,
        MediaRepository.hiResReport,
        MediaRepository.sleepTimerState,
        _searchQuery,
    ) { health, outputs, hiRes, timer, query ->
        listOf(health, outputs, hiRes, timer, query)
    }

    private val libraryMetaFlow = combine(
        combine(_librarySort, _librarySourceFilter, _isLibraryScanning) { s, f, scan -> Triple(s, f, scan) },
        combine(_hasAudioPermission, libraryRepo.observeTrackCount(), _feedbackMessage) { perm, count, msg -> Triple(perm, count, msg) },
    ) { t1, t2 ->
        listOf<Any?>(t1.first, t1.second, t1.third, t2.first, t2.second, t2.third)
    }

    @Suppress("UNCHECKED_CAST")
    private val systemSnapshotFlow = combine(
        permissionsAndServicesFlow,
        audioAndQueryFlow,
        libraryMetaFlow,
    ) { base, ext, lib ->
        SystemSnapshot(
            hasListener = base[0] as Boolean,
            hasNotifications = base[1] as Boolean,
            services = base[2] as List<InstalledServiceStatus>,
            oemGuide = base[3] as OemBatteryGuide?,
            bridgeRunning = base[4] as Boolean,
            systemHealth = ext[0] as SystemHealthSummary,
            audioOutputs = ext[1] as List<AudioOutputDevice>,
            hiResReport = ext[2] as HiResHardwareReport,
            sleepTimer = ext[3] as SleepTimerState,
            searchQuery = ext[4] as String,
            sortOption = lib[0] as LibrarySortOption,
            sourceFilter = lib[1] as LibrarySourceFilter,
            isScanning = lib[2] as Boolean,
            hasAudioPerm = lib[3] as Boolean,
            trackCount = lib[4] as Int,
            feedback = lib[5] as String?,
        )
    }

    private val configBaseFlow = combine(
        prefsRepo.widgetConfigFlow,
        prefsRepo.lockAndQsConfigFlow,
        prefsRepo.mediaSessionPreferencesFlow,
        prefsRepo.equalizerConfigFlow,
    ) { w, l, s, eq ->
        listOf(w, l, s, eq)
    }

    private val configUserFlow = combine(
        prefsRepo.localStatsFlow,
        prefsRepo.widgetTemplatesFlow,
        prefsRepo.onboardingCompletedFlow,
    ) { stats, templates, onboardingDone ->
        Triple(stats, templates, onboardingDone)
    }

    private val configSnapshotFlow = combine(
        configBaseFlow,
        configUserFlow,
    ) { base, usr ->
        ConfigSnapshot(
            widgetConfig = base[0] as WidgetConfig,
            lockConfig = base[1] as LockAndQsConfig,
            sessionPreferences = base[2] as MediaSessionPreferences,
            equalizerConfig = base[3] as EqualizerConfig,
            listeningStats = usr.first,
            widgetTemplates = usr.second,
            onboardingCompleted = usr.third,
        )
    }

    val uiState: StateFlow<MagicMusicUiState> = combine(
        MediaRepository.playerState,
        MediaRepository.mediaState,
        configSnapshotFlow,
        systemSnapshotFlow,
    ) { player, media, cfg, sys ->
        MagicMusicUiState(
            playerState = player,
            media = media,
            widgetConfig = cfg.widgetConfig,
            lockAndQsConfig = cfg.lockConfig,
            sessionPreferences = cfg.sessionPreferences,
            equalizerConfig = cfg.equalizerConfig,
            listeningStats = cfg.listeningStats,
            widgetTemplates = cfg.widgetTemplates,
            sleepTimer = sys.sleepTimer,
            systemHealth = sys.systemHealth,
            audioOutputs = sys.audioOutputs,
            hiResReport = sys.hiResReport,
            hasListenerPermission = sys.hasListener,
            hasAudioLibraryPermission = sys.hasAudioPerm,
            hasPostNotificationsPermission = sys.hasNotifications,
            onboardingCompleted = cfg.onboardingCompleted,
            listenerState = player.listenerState,
            installedServices = sys.services,
            oemBatteryGuide = sys.oemGuide,
            bridgeRunning = sys.bridgeRunning,
            searchQuery = sys.searchQuery,
            librarySortOption = sys.sortOption,
            librarySourceFilter = sys.sourceFilter,
            isLibraryScanning = sys.isScanning,
            localTrackCount = sys.trackCount,
            feedbackMessage = sys.feedback ?: player.lastError,
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = MagicMusicUiState(),
    )

    fun refreshSystemState() {
        _hasAudioPermission.value = libraryRepo.hasAudioPermission()
        MediaRepository.refreshPermissionsAndServices()
        MediaRepository.refreshAudioOutputs()
    }

    fun clearFeedback() {
        _feedbackMessage.value = null
        MediaRepository.clearError()
    }

    fun showFeedback(msg: String) {
        _feedbackMessage.value = msg
    }

    fun setSearchQuery(query: String) {
        _searchQuery.value = query
    }

    fun setLibrarySortOption(sort: LibrarySortOption) {
        _librarySort.value = sort
    }

    fun setLibrarySourceFilter(filter: LibrarySourceFilter) {
        _librarySourceFilter.value = filter
    }

    fun scanLocalLibraryNow() {
        val ctx = getApplication<Application>()
        _hasAudioPermission.value = libraryRepo.hasAudioPermission()
        if (!_hasAudioPermission.value) {
            _feedbackMessage.value = "Concede acceso a audio para escanear música del dispositivo"
            return
        }
        viewModelScope.launch {
            _isLibraryScanning.value = true
            val count = runCatching { libraryRepo.syncMediaStoreIncremental() }.getOrDefault(0)
            _isLibraryScanning.value = false
            LibraryScanWorker.enqueueIncremental(ctx)
            _feedbackMessage.value = "Biblioteca actualizada: $count pistas locales"
        }
    }

    fun completeOnboarding() {
        viewModelScope.launch {
            prefsRepo.setOnboardingCompleted(true)
        }
    }

    // Material 3 & Expressive Theme / Estilo Preferences
    fun setUseDynamicColor(enabled: Boolean) {
        viewModelScope.launch {
            prefsRepo.updateThemePreferences { it.copy(useDynamicColor = enabled) }
        }
    }

    fun setThemeMode(themeMode: ThemeMode) {
        viewModelScope.launch {
            prefsRepo.updateThemePreferences {
                val syncedTone = when (themeMode) {
                    ThemeMode.LIGHT -> SurfaceToneStyle.LIGHT
                    ThemeMode.DARK -> if (it.surfaceToneStyle == SurfaceToneStyle.LIGHT) SurfaceToneStyle.TONAL else it.surfaceToneStyle
                    ThemeMode.SYSTEM -> it.surfaceToneStyle
                }
                it.copy(themeMode = themeMode, surfaceToneStyle = syncedTone)
            }
        }
    }

    fun setSurfaceToneStyle(style: SurfaceToneStyle) {
        viewModelScope.launch {
            prefsRepo.updateThemePreferences {
                val syncedMode = when (style) {
                    SurfaceToneStyle.LIGHT -> ThemeMode.LIGHT
                    SurfaceToneStyle.DARK, SurfaceToneStyle.TONAL -> ThemeMode.DARK
                }
                it.copy(surfaceToneStyle = style, themeMode = syncedMode)
            }
        }
    }

    fun setSeedColor(argb: Long) {
        viewModelScope.launch {
            prefsRepo.updateThemePreferences {
                it.copy(useDynamicColor = false, seedColorArgb = argb)
            }
        }
    }

    fun setSchemeVariant(variant: McuSchemeVariant) {
        viewModelScope.launch {
            prefsRepo.updateThemePreferences { it.copy(schemeVariant = variant) }
        }
    }

    fun setContrastPreset(preset: ContrastPreset) {
        viewModelScope.launch {
            prefsRepo.updateThemePreferences { it.copy(contrastLevel = preset) }
        }
    }

    fun setPureAmoledBlack(enabled: Boolean) {
        viewModelScope.launch {
            prefsRepo.updateThemePreferences { it.copy(pureAmoledBlack = enabled) }
        }
    }

    fun setColorFromArtwork(enabled: Boolean) {
        viewModelScope.launch {
            prefsRepo.updateThemePreferences { it.copy(colorFromArtwork = enabled) }
        }
    }

    fun setReduceAnimations(enabled: Boolean) {
        viewModelScope.launch {
            prefsRepo.updateThemePreferences { it.copy(reduceAnimations = enabled) }
        }
    }

    fun setUseExpressiveM3(enabled: Boolean) {
        viewModelScope.launch {
            prefsRepo.updateThemePreferences { it.copy(useExpressiveM3 = enabled) }
        }
    }

    fun setHapticFeedback(enabled: Boolean) {
        viewModelScope.launch {
            prefsRepo.updateThemePreferences { it.copy(hapticFeedbackEnabled = enabled) }
        }
    }

    fun setGeometryPreset(preset: GeometryPreset) {
        viewModelScope.launch {
            prefsRepo.updateThemePreferences { it.copy(geometryPreset = preset) }
        }
    }

    fun setArtworkShape(shape: ArtworkShapeStyle) {
        viewModelScope.launch {
            prefsRepo.updateThemePreferences { it.copy(artworkShape = shape) }
        }
    }

    fun setNavLabelVisibility(visibility: NavLabelVisibility) {
        viewModelScope.launch {
            prefsRepo.updateThemePreferences { it.copy(navLabelVisibility = visibility) }
        }
    }

    fun setMiniPlayerStyle(style: MiniPlayerStyle) {
        viewModelScope.launch {
            prefsRepo.updateThemePreferences { it.copy(miniPlayerStyle = style) }
        }
    }

    fun setLibraryGridColumns(columns: Int) {
        viewModelScope.launch {
            prefsRepo.updateThemePreferences { it.copy(libraryGridColumns = columns.coerceIn(2, 3)) }
        }
    }

    fun setLyricsAutoScroll(enabled: Boolean) {
        viewModelScope.launch {
            prefsRepo.updateThemePreferences { it.copy(lyricsAutoScroll = enabled) }
        }
    }

    fun setLyricsFontScale(scale: Float) {
        viewModelScope.launch {
            prefsRepo.updateThemePreferences { it.copy(lyricsFontScale = scale.coerceIn(0.85f, 1.4f)) }
        }
    }

    fun setRoleOverrides(overrides: ColorRoleOverrides) {
        viewModelScope.launch {
            prefsRepo.updateThemePreferences { it.copy(useDynamicColor = false, roleOverrides = overrides) }
        }
    }

    fun resetRoleOverrides() {
        setRoleOverrides(ColorRoleOverrides())
    }

    fun saveColorProfile(name: String) {
        viewModelScope.launch {
            prefsRepo.saveColorProfile(name, themePreferences.value)
            _feedbackMessage.value = "Paleta guardada en tu colección"
        }
    }

    fun deleteColorProfile(profileId: String) {
        viewModelScope.launch {
            prefsRepo.deleteColorProfile(profileId)
            _feedbackMessage.value = "Paleta eliminada"
        }
    }

    fun applyColorProfile(profile: SavedColorProfile) {
        viewModelScope.launch {
            prefsRepo.updateThemePreferences { cur ->
                cur.copy(
                    useDynamicColor = false,
                    seedColorArgb = profile.seedColorArgb,
                    schemeVariant = profile.variant,
                    themeMode = profile.themeMode,
                    surfaceToneStyle = profile.surfaceToneStyle,
                    contrastLevel = profile.contrastPreset,
                    pureAmoledBlack = profile.pureAmoled,
                    roleOverrides = profile.overrides,
                )
            }
            _feedbackMessage.value = "Paleta '${profile.name}' aplicada"
        }
    }

    fun exportColorProfilesJson(onResult: (String) -> Unit) {
        viewModelScope.launch {
            onResult(prefsRepo.exportColorProfilesJson())
        }
    }

    fun importColorProfilesJson(raw: String) {
        viewModelScope.launch {
            val count = prefsRepo.importColorProfilesJson(raw)
            _feedbackMessage.value = if (count > 0) "Se importaron $count paletas" else "JSON de paletas no válido"
        }
    }

    fun updateWidgetInstance(appWidgetId: Int, transform: (WidgetInstanceConfig) -> WidgetInstanceConfig) {
        val ctx = getApplication<Application>()
        viewModelScope.launch {
            prefsRepo.updateWidgetInstanceConfig(appWidgetId, transform)
            MagicWidgetUpdater.updateAllWidgets(ctx)
        }
    }

    fun resetWidgetInstanceToApp(appWidgetId: Int) {
        val theme = themePreferences.value
        updateWidgetInstance(appWidgetId) { cur ->
            cur.copy(
                colorSource = com.example.data.WidgetColorSource.FOLLOW_APP,
                seedColorArgb = theme.seedColorArgb,
                schemeVariant = theme.schemeVariant,
                themeMode = theme.themeMode,
                contrastLevel = theme.contrastLevel,
                roleOverrides = theme.roleOverrides,
            )
        }
        _feedbackMessage.value = "Widget #$appWidgetId sincronizado con la app"
    }

    // Playback controls
    fun playPause() = MediaRepository.playPause()
    fun next() = MediaRepository.next()
    fun previous() = MediaRepository.previous()
    fun seekTo(posMs: Long) = MediaRepository.seekTo(posMs)
    fun seekBy(deltaMs: Long) = MediaRepository.seekBy(deltaMs)
    fun selectSession(pkg: String) = MediaRepository.selectSession(pkg)
    fun toggleFavorite() = MediaRepository.toggleFavorite()
    fun toggleShuffle() = MediaRepository.toggleShuffle()
    fun toggleRepeat() = MediaRepository.toggleRepeat()
    fun skipToQueueItem(queueId: Long) = MediaRepository.skipToQueueItem(queueId)
    fun reorderLocalQueue(fromIndex: Int, toIndex: Int) = MediaRepository.reorderLocalQueue(fromIndex, toIndex)
    fun clearLocalQueue() = MediaRepository.clearLocalQueue()
    fun executeCustomAction(action: CustomControlAction) = MediaRepository.executeCustomAction(action)
    fun setVolumePercent(percent: Int) = MediaRepository.setMusicVolumePercent(percent)

    // Library playback & CRUD
    fun playLocalTracks(tracks: List<LocalTrackEntity>, startIndex: Int = 0, shuffle: Boolean = false) {
        MediaRepository.playLocalTracks(tracks, startIndex, shuffle)
    }

    fun shuffleAllLocalTracks() {
        viewModelScope.launch {
            val all = libraryRepo.getAllTracksSnapshot()
            if (all.isNotEmpty()) {
                MediaRepository.playLocalTracks(all, startIndex = 0, shuffle = true)
            } else {
                MediaRepository.toggleShuffle()
                _feedbackMessage.value = "Escanea tu biblioteca para reproducir pistas locales en aleatorio"
            }
        }
    }

    fun playAlbum(albumId: Long) {
        viewModelScope.launch {
            val tracks = libraryRepo.getAlbumTracksSnapshot(albumId)
            if (tracks.isNotEmpty()) MediaRepository.playLocalTracks(tracks, 0, false)
        }
    }

    fun playArtist(artistId: Long) {
        viewModelScope.launch {
            val tracks = libraryRepo.getArtistTracksSnapshot(artistId)
            if (tracks.isNotEmpty()) MediaRepository.playLocalTracks(tracks, 0, false)
        }
    }

    fun playFolder(folderPath: String) {
        viewModelScope.launch {
            val tracks = libraryRepo.getFolderTracksSnapshot(folderPath)
            if (tracks.isNotEmpty()) MediaRepository.playLocalTracks(tracks, 0, false)
        }
    }

    fun playPlaylist(playlistId: Long, shuffle: Boolean = false) {
        viewModelScope.launch {
            val tracks = libraryRepo.getPlaylistTracksSnapshot(playlistId)
            if (tracks.isNotEmpty()) {
                MediaRepository.playLocalTracks(tracks, 0, shuffle)
            } else {
                _feedbackMessage.value = "Esta lista todavía no tiene pistas"
            }
        }
    }

    fun playSmartPlaylist(kind: String, shuffle: Boolean = false) {
        viewModelScope.launch {
            val tracks = when (kind) {
                "most_played" -> libraryRepo.getMostPlayedSnapshot()
                "recent" -> libraryRepo.getRecentlyAddedSnapshot()
                "favorites" -> libraryRepo.getFavoritesSnapshot()
                else -> libraryRepo.getAllTracksSnapshot()
            }
            if (tracks.isNotEmpty()) {
                MediaRepository.playLocalTracks(tracks, 0, shuffle)
            } else {
                _feedbackMessage.value = "Todavía no hay pistas para esta lista inteligente"
            }
        }
    }

    fun toggleLocalTrackFavorite(track: LocalTrackEntity) {
        viewModelScope.launch {
            libraryRepo.setTrackFavorite(track.id, !track.isFavorite)
        }
    }

    fun shareLocalTrack(track: LocalTrackEntity) {
        val ctx = getApplication<Application>()
        val intent = libraryRepo.buildShareTrackIntent(track)
        val chooser = Intent.createChooser(intent, "Compartir ${track.title}").apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        runCatching { ctx.startActivity(chooser) }
            .onFailure { _feedbackMessage.value = "No se encontró una app para compartir" }
    }

    fun createPlaylist(name: String) {
        viewModelScope.launch {
            libraryRepo.createPlaylist(name)
            _feedbackMessage.value = "Lista '$name' creada"
        }
    }

    fun renamePlaylist(playlistId: Long, newName: String) {
        viewModelScope.launch {
            libraryRepo.renamePlaylist(playlistId, newName)
            _feedbackMessage.value = "Lista renombrada"
        }
    }

    fun deletePlaylist(playlistId: Long) {
        viewModelScope.launch {
            libraryRepo.deletePlaylist(playlistId)
            _feedbackMessage.value = "Lista eliminada"
        }
    }

    fun addLocalTrackToPlaylist(playlistId: Long, trackId: Long) {
        viewModelScope.launch {
            libraryRepo.addTrackToPlaylist(playlistId, trackId)
            _feedbackMessage.value = "Pista añadida a la lista"
        }
    }

    fun removeLocalTrackFromPlaylist(playlistId: Long, trackId: Long) {
        viewModelScope.launch {
            libraryRepo.removeTrackFromPlaylist(playlistId, trackId)
        }
    }

    fun reorderPlaylistTracks(playlistId: Long, orderedTrackIds: List<Long>) {
        viewModelScope.launch {
            libraryRepo.reorderPlaylistTracks(playlistId, orderedTrackIds)
        }
    }

    fun addTrackBookmark(label: String, customPositionMs: Long? = null) {
        val media = MediaRepository.mediaState.value
        if (!media.hasActiveMedia) return
        val pos = customPositionMs ?: MediaRepository.playbackProgressFlow.value.first.coerceAtLeast(media.positionMs)
        viewModelScope.launch {
            libraryRepo.addBookmark(
                trackKey = media.trackKey,
                positionMs = pos,
                label = label.ifBlank { "Marca ${com.example.ui.components.formatTimeMs(pos)}" },
            )
            _feedbackMessage.value = "Marca guardada en ${com.example.ui.components.formatTimeMs(pos)}"
        }
    }

    fun deleteTrackBookmark(bookmarkId: Long) {
        viewModelScope.launch {
            libraryRepo.deleteBookmark(bookmarkId)
        }
    }

    fun addTrackToQuickPlaylist() {
        val m = uiState.value.media
        if (!m.hasActiveMedia) return
        viewModelScope.launch {
            val localId = m.localTrackId
            if (localId != null) {
                val existing = playlistsFlow.value.firstOrNull()
                val pid = existing?.id ?: libraryRepo.createPlaylist("Favoritas rápidas")
                libraryRepo.addTrackToPlaylist(pid, localId)
            }
            val added = prefsRepo.addTrackToQuickPlaylist(m.trackKey)
            _feedbackMessage.value = if (added || localId != null) {
                "Pista guardada en tu lista"
            } else {
                "La pista ya estaba en tu lista rápida"
            }
        }
    }

    // Official Music Accounts (Spotify / Apple Music / YouTube)
    fun buildSpotifyAuthIntent(): Intent? = accountsCoordinator.buildSpotifyAuthIntent()

    fun handleSpotifyAuthResult(data: Intent?) {
        viewModelScope.launch {
            accountsCoordinator.handleSpotifyAuthResult(data)
        }
    }

    fun connectYouTubeWithGoogle(activityContext: Context) {
        viewModelScope.launch {
            accountsCoordinator.connectYouTubeWithGoogle(activityContext)
        }
    }

    fun syncAccountSource(sourceId: MusicSourceId) {
        viewModelScope.launch {
            accountsCoordinator.syncSource(sourceId)
        }
    }

    fun disconnectAccountSource(sourceId: MusicSourceId) {
        viewModelScope.launch {
            accountsCoordinator.disconnectSource(sourceId)
        }
    }

    fun searchSpotifyTracks(query: String) {
        viewModelScope.launch {
            _spotifySearchResults.value = accountsCoordinator.spotify.search(query)
        }
    }

    fun launchAccountExternalUri(sourceId: MusicSourceId, uri: String?) {
        val ctx = getApplication<Application>()
        val ok = when (sourceId) {
            MusicSourceId.SPOTIFY -> accountsCoordinator.spotify.launchExternalUri(ctx, uri)
            MusicSourceId.APPLE_MUSIC -> accountsCoordinator.appleMusic.launchExternalUri(ctx, uri)
            MusicSourceId.YOUTUBE_MUSIC -> accountsCoordinator.youtube.launchExternalUri(ctx, uri)
            else -> false
        }
        if (!ok) {
            _feedbackMessage.value = "No se encontró aplicación compatible para abrir este enlace"
        }
    }

    // Artwork debug actions
    fun clearArtworkCache() {
        MediaRepository.clearArtworkCaches(getApplication())
        _feedbackMessage.value = "Caché de carátulas vaciada"
    }

    // Sleep timer
    fun startSleepTimerCountdown(minutes: Int, fadeOut: Boolean) {
        MediaRepository.startSleepTimerCountdown(minutes, fadeOut)
        _feedbackMessage.value = "Temporizador programado para $minutes min"
    }

    fun startSleepTimerEndOfTrack(fadeOut: Boolean) {
        MediaRepository.startSleepTimerEndOfTrack(fadeOut)
        _feedbackMessage.value = "Se detendrá al terminar esta pista"
    }

    fun setSleepTimerFadeOut(enabled: Boolean) = MediaRepository.setSleepTimerFadeOut(enabled)

    fun cancelSleepTimer() {
        MediaRepository.cancelSleepTimer()
        _feedbackMessage.value = "Temporizador desactivado"
    }

    // Audio outputs & Equalizer / Hi-Res DSP
    fun refreshAudioOutputs() = MediaRepository.refreshAudioOutputs()

    fun selectAudioOutput(device: AudioOutputDevice) {
        val ok = MediaRepository.selectAudioOutput(device)
        _feedbackMessage.value = if (ok) {
            "Salida ajustada: ${device.name}"
        } else {
            "Abre el selector de salida del sistema para conectar tu dispositivo"
        }
    }

    fun setEqualizerEnabled(enabled: Boolean) {
        viewModelScope.launch {
            prefsRepo.updateEqualizerConfig { it.copy(enabled = enabled) }
        }
    }

    fun setEqualizerPreset(presetIndex: Int, preset: EqualizerPreset) {
        viewModelScope.launch {
            prefsRepo.updateEqualizerConfig {
                it.copy(
                    presetIndex = presetIndex,
                    customPresetName = "",
                    bandGainsDb = preset.gains,
                )
            }
        }
    }

    fun setEqualizerBand(bandIndex: Int, gainDb: Float) {
        viewModelScope.launch {
            prefsRepo.updateEqualizerConfig { cur ->
                val list = cur.bandGainsDb.toMutableList()
                while (list.size < 5) list.add(0f)
                if (bandIndex in list.indices) {
                    list[bandIndex] = gainDb.coerceIn(-12f, 12f)
                }
                cur.copy(bandGainsDb = list)
            }
        }
    }

    fun setEqualizerAdvanced(advanced: Boolean) {
        viewModelScope.launch {
            prefsRepo.updateEqualizerConfig { it.copy(isAdvancedMode = advanced) }
        }
    }

    fun setHiResBufferHigh(high: Boolean) {
        viewModelScope.launch {
            prefsRepo.updateEqualizerConfig { it.copy(hiResBufferHigh = high) }
        }
    }

    fun setBassBoostStrength(strength: Int) {
        viewModelScope.launch {
            prefsRepo.updateEqualizerConfig { it.copy(bassBoostStrength = strength.coerceIn(0, 1000)) }
        }
    }

    fun setVirtualizerStrength(strength: Int) {
        viewModelScope.launch {
            prefsRepo.updateEqualizerConfig { it.copy(virtualizerStrength = strength.coerceIn(0, 1000)) }
        }
    }

    fun setLoudnessGainMb(gainMb: Int) {
        viewModelScope.launch {
            prefsRepo.updateEqualizerConfig { it.copy(loudnessGainMb = gainMb.coerceIn(0, 1000)) }
        }
    }

    fun setPreampGainDb(preampDb: Float) {
        viewModelScope.launch {
            prefsRepo.updateEqualizerConfig { it.copy(preampGainDb = preampDb.coerceIn(-6f, 6f)) }
        }
    }

    fun setStereoBalance(balance: Float) {
        viewModelScope.launch {
            prefsRepo.updateEqualizerConfig { it.copy(stereoBalance = balance.coerceIn(-1f, 1f)) }
        }
    }

    fun setGaplessEnabled(enabled: Boolean) {
        viewModelScope.launch {
            prefsRepo.updateEqualizerConfig { it.copy(gaplessEnabled = enabled) }
        }
    }

    fun setReplayGainEnabled(enabled: Boolean) {
        viewModelScope.launch {
            prefsRepo.updateEqualizerConfig { it.copy(replayGainEnabled = enabled) }
        }
    }

    fun setFloat24BitOutput(enabled: Boolean) {
        viewModelScope.launch {
            prefsRepo.updateEqualizerConfig { it.copy(float24BitOutput = enabled) }
        }
    }

    fun setCrossfadeSeconds(seconds: Int) {
        viewModelScope.launch {
            prefsRepo.updateEqualizerConfig { it.copy(crossfadeSeconds = seconds.coerceIn(0, 12)) }
        }
    }

    fun switchEqOutputCategory(category: AudioOutputCategory) {
        viewModelScope.launch {
            prefsRepo.switchEqualizerOutputCategory(category)
            _feedbackMessage.value = "Perfil DSP: ${category.label}"
        }
    }

    fun saveCustomEqPreset(name: String) {
        viewModelScope.launch {
            prefsRepo.saveCustomEqPreset(name, uiState.value.equalizerConfig)
            _feedbackMessage.value = "Preset '$name' guardado"
        }
    }

    fun deleteCustomEqPreset(presetId: String) {
        viewModelScope.launch {
            prefsRepo.deleteCustomEqPreset(presetId)
        }
    }

    fun applyCustomEqPreset(preset: CustomEqPreset) {
        viewModelScope.launch {
            prefsRepo.updateEqualizerConfig { cur ->
                cur.copy(
                    presetIndex = -1,
                    customPresetName = preset.name,
                    bandGainsDb = preset.bandGainsDb,
                    bassBoostStrength = preset.bassBoostStrength,
                    virtualizerStrength = preset.virtualizerStrength,
                    loudnessGainMb = preset.loudnessGainMb,
                    preampGainDb = preset.preampGainDb,
                )
            }
        }
    }

    fun clearLocalStats() {
        viewModelScope.launch {
            prefsRepo.clearLocalStats()
            _feedbackMessage.value = "Historial y estadísticas locales borrados"
        }
    }

    fun toggleLocalAudioEngine() {
        val ctx = getApplication<Application>()
        val local = MediaRepository.localEngine.state.value
        if (local.isActive && local.isPlaying) {
            MediaRepository.localEngine.pause()
            _feedbackMessage.value = "Motor local pausado"
        } else {
            MediaRepository.localEngine.startOrResume()
            MagicMediaBridgeService.startOrUpdate(ctx)
            _feedbackMessage.value = "Motor Hi-Res activo • Listo para probar widgets y pantalla de bloqueo"
        }
        MediaRepository.rebuildState()
    }

    // Session priority & filtering preferences
    fun setSessionPriorityRule(rule: SessionPriorityRule) {
        viewModelScope.launch {
            prefsRepo.updateMediaSessionPreferences { it.copy(priorityRule = rule) }
        }
    }

    fun setPreferredServicePackage(packageName: String) {
        viewModelScope.launch {
            prefsRepo.setPreferredServicePackage(packageName)
            _feedbackMessage.value = "Reproductor preferido actualizado"
        }
    }

    fun toggleIgnoreVideoSessions(ignore: Boolean) {
        viewModelScope.launch {
            prefsRepo.updateMediaSessionPreferences { it.copy(ignoreVideoSessions = ignore) }
        }
    }

    fun toggleManagedPackageEnabled(packageName: String, enabled: Boolean) {
        viewModelScope.launch {
            prefsRepo.updateMediaSessionPreferences { prefs ->
                val updated = prefs.disabledPackages.toMutableSet()
                if (enabled) updated.remove(packageName) else updated.add(packageName)
                prefs.copy(disabledPackages = updated)
            }
        }
    }

    // Widget studio & template mutations
    fun setWidgetStyle(style: WidgetStyle) {
        viewModelScope.launch {
            prefsRepo.updateWidgetConfig { it.copy(style = style) }
        }
    }

    fun setCornerRadius(dp: Int) {
        viewModelScope.launch {
            prefsRepo.updateWidgetConfig { it.copy(cornerRadiusDp = dp.coerceIn(12, 32)) }
        }
    }

    fun setGlassOpacity(opacity: Float) {
        viewModelScope.launch {
            prefsRepo.updateWidgetConfig { it.copy(glassOpacity = opacity.coerceIn(0.45f, 1.0f)) }
        }
    }

    fun toggleShowProgressBar(show: Boolean) {
        viewModelScope.launch {
            prefsRepo.updateWidgetConfig { it.copy(showProgressBar = show) }
        }
    }

    fun toggleShowServiceBadge(show: Boolean) {
        viewModelScope.launch {
            prefsRepo.updateWidgetConfig { it.copy(showServiceBadge = show) }
        }
    }

    fun toggleCompactHeader(show: Boolean) {
        viewModelScope.launch {
            prefsRepo.updateWidgetConfig { it.copy(showCompactHeader = show) }
        }
    }

    fun setLeftCustomAction(action: CustomControlAction) {
        viewModelScope.launch {
            prefsRepo.updateWidgetConfig { it.copy(leftCustomAction = action) }
        }
    }

    fun setRightCustomAction(action: CustomControlAction) {
        viewModelScope.launch {
            prefsRepo.updateWidgetConfig { it.copy(rightCustomAction = action) }
        }
    }

    fun saveWidgetTemplate(name: String, format: WidgetFormat) {
        viewModelScope.launch {
            prefsRepo.saveWidgetTemplate(name, format, uiState.value.widgetConfig)
            _feedbackMessage.value = "Plantilla guardada"
        }
    }

    fun duplicateWidgetTemplate(templateId: String) {
        viewModelScope.launch {
            prefsRepo.duplicateWidgetTemplate(templateId)
            _feedbackMessage.value = "Plantilla duplicada"
        }
    }

    fun deleteWidgetTemplate(templateId: String) {
        viewModelScope.launch {
            prefsRepo.deleteWidgetTemplate(templateId)
            _feedbackMessage.value = "Plantilla eliminada"
        }
    }

    fun applyWidgetTemplate(template: WidgetTemplate) {
        viewModelScope.launch {
            prefsRepo.applyWidgetTemplate(template)
            _feedbackMessage.value = "Plantilla '${template.name}' aplicada"
        }
    }

    fun exportWidgetTemplatesJson(onResult: (String) -> Unit) {
        viewModelScope.launch {
            onResult(prefsRepo.exportWidgetTemplatesJson())
        }
    }

    fun importWidgetTemplatesJson(raw: String) {
        viewModelScope.launch {
            val count = prefsRepo.importWidgetTemplatesJson(raw)
            _feedbackMessage.value = if (count > 0) {
                "Se importaron $count plantillas"
            } else {
                "No se reconocieron plantillas en el texto JSON"
            }
        }
    }

    fun exportFullBackupJson(onResult: (String) -> Unit) {
        viewModelScope.launch {
            onResult(prefsRepo.exportFullBackupJson())
        }
    }

    fun importFullBackupJson(raw: String) {
        viewModelScope.launch {
            val ok = prefsRepo.importFullBackupJson(raw)
            _feedbackMessage.value = if (ok) {
                "Copia de seguridad restaurada correctamente"
            } else {
                "Formato de copia no válido"
            }
        }
    }

    fun pinWidget(format: WidgetFormat) {
        val ctx = getApplication<Application>()
        val cls = when (format) {
            WidgetFormat.CAPSULE_4X1 -> MagicWidgetCapsuleProvider::class.java
            WidgetFormat.CARD_4X2 -> MagicWidgetCardProvider::class.java
            WidgetFormat.SQUARE_2X2 -> MagicWidgetSquareProvider::class.java
            WidgetFormat.MINI_2X1 -> MagicWidgetMiniProvider::class.java
        }
        val provider = ComponentName(ctx, cls)
        val successMsg = "Confirma para añadir el widget ${format.title} (${format.gridLabel}) a tu escritorio"
        when (val result = AndroidVersionCompat.requestPinAppWidgetSafe(ctx, provider, successMsg)) {
            is CompatActionResult.Success -> _feedbackMessage.value = result.message
            is CompatActionResult.AlreadyConfigured -> _feedbackMessage.value = result.message
            is CompatActionResult.FallbackManualGuide -> _feedbackMessage.value = result.message
            is CompatActionResult.Error -> _feedbackMessage.value = result.message
        }
    }

    // Lock screen & Quick Settings mutations
    fun toggleBridgeService(enable: Boolean) {
        val ctx = getApplication<Application>()
        viewModelScope.launch {
            prefsRepo.updateLockAndQsConfig { it.copy(bridgeEnabled = enable) }
        }
        if (enable) {
            MagicMediaBridgeService.startOrUpdate(ctx)
            _feedbackMessage.value = "Puente de pantalla de bloqueo activado"
        } else {
            MagicMediaBridgeService.stop(ctx)
            _feedbackMessage.value = "Puente de pantalla de bloqueo desactivado"
        }
    }

    fun toggleShowOnLockScreen(show: Boolean) {
        viewModelScope.launch {
            prefsRepo.updateLockAndQsConfig { it.copy(showOnLockScreen = show) }
        }
    }

    fun toggleShowArtworkOnLockScreen(show: Boolean) {
        viewModelScope.launch {
            prefsRepo.updateLockAndQsConfig { it.copy(showArtworkOnLockScreen = show) }
        }
    }

    fun toggleRequireUnlockToOpenApp(req: Boolean) {
        viewModelScope.launch {
            prefsRepo.updateLockAndQsConfig { it.copy(requireUnlockToOpenApp = req) }
        }
    }

    fun toggleRequireUnlockToSwitchSource(req: Boolean) {
        viewModelScope.launch {
            prefsRepo.updateLockAndQsConfig { it.copy(requireUnlockToSwitchSource = req) }
        }
    }

    fun toggleRequireUnlockForCustomActions(req: Boolean) {
        viewModelScope.launch {
            prefsRepo.updateLockAndQsConfig { it.copy(requireUnlockForCustomActions = req) }
        }
    }

    fun setLockAction1(action: CustomControlAction) {
        viewModelScope.launch {
            prefsRepo.updateLockAndQsConfig { it.copy(lockScreenAction1 = action) }
        }
    }

    fun setLockAction2(action: CustomControlAction) {
        viewModelScope.launch {
            prefsRepo.updateLockAndQsConfig { it.copy(lockScreenAction2 = action) }
        }
    }

    fun setQsTileMode(mode: QsTileMode) {
        viewModelScope.launch {
            prefsRepo.updateLockAndQsConfig { it.copy(qsTileMode = mode) }
        }
    }

    fun setLockPrivacyLevel(level: LockPrivacyLevel) {
        viewModelScope.launch {
            prefsRepo.updateLockAndQsConfig { it.copy(lockPrivacyLevel = level) }
        }
    }

    fun setLockAutoCloseSeconds(seconds: Int) {
        viewModelScope.launch {
            prefsRepo.updateLockAndQsConfig { it.copy(lockAutoCloseSeconds = seconds) }
        }
    }

    fun toggleCompactActionIndex(index: Int) {
        viewModelScope.launch {
            prefsRepo.updateLockAndQsConfig { cfg ->
                val list = cfg.compactActionIndices.toMutableList()
                if (list.contains(index)) {
                    list.remove(index)
                } else {
                    list.add(index)
                }
                cfg.copy(compactActionIndices = list.take(3))
            }
        }
    }

    fun dismissNotifyChannelGuide() {
        viewModelScope.launch {
            prefsRepo.updateLockAndQsConfig { it.copy(notifyChannelGuideShown = true) }
        }
    }

    fun requestAddQuickTile(type: QuickTileType) {
        val isPlay = type == QuickTileType.PLAY_PAUSE
        requestAddQuickSettingsTile(isPlay)
        viewModelScope.launch {
            prefsRepo.setQuickTileAdded(type.key, true)
        }
    }

    fun requestAddQuickSettingsTile(isPlayPause: Boolean) {
        val ctx = getApplication<Application>()
        val component = ComponentName(
            ctx,
            if (isPlayPause) MagicPlayPauseTileService::class.java else MagicNextTrackTileService::class.java,
        )
        val label = if (isPlayPause) {
            ctx.getString(R.string.tile_play_pause_label)
        } else {
            ctx.getString(R.string.tile_next_label)
        }
        val iconRes = if (isPlayPause) R.drawable.ic_magic_music_note else R.drawable.ic_magic_next

        AndroidVersionCompat.requestAddTileServiceSafe(
            context = ctx,
            tileComponent = component,
            label = label,
            iconResId = iconRes,
        ) { result ->
            if (result is CompatActionResult.Success || result is CompatActionResult.AlreadyConfigured) {
                MediaRepository.setTileAddedStatus(isPlayPause, true)
            }
            _feedbackMessage.value = when (result) {
                is CompatActionResult.Success -> result.message
                is CompatActionResult.AlreadyConfigured -> result.message
                is CompatActionResult.FallbackManualGuide -> result.message
                is CompatActionResult.Error -> result.message
            }
        }
    }

    fun fixHealthCheck(id: HealthCheckId, onRequestPostNotifications: () -> Unit) {
        when (id) {
            HealthCheckId.NOTIFICATION_LISTENER -> openNotificationListenerSettings()
            HealthCheckId.POST_NOTIFICATIONS -> onRequestPostNotifications()
            HealthCheckId.BATTERY_OPTIMIZATION -> openBatteryOptimizationSettings()
            HealthCheckId.WIDGET_PINNED -> pinWidget(WidgetFormat.CARD_4X2)
            HealthCheckId.QUICK_TILE_ADDED -> requestAddQuickSettingsTile(true)
        }
    }

    fun openLockScreenDeck() {
        val ctx = getApplication<Application>()
        ctx.startActivity(
            Intent(ctx, LockScreenDeckActivity::class.java).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            },
        )
    }

    fun openNotificationListenerSettings() {
        val ctx = getApplication<Application>()
        val res = OemCompat.openNotificationListenerSettingsSafe(ctx)
        if (res is CompatActionResult.FallbackManualGuide) {
            _feedbackMessage.value = res.message
        }
    }

    fun openBatteryOptimizationSettings() {
        val ctx = getApplication<Application>()
        val res = OemCompat.openBatterySettingsSafe(ctx)
        if (res is CompatActionResult.FallbackManualGuide) {
            _feedbackMessage.value = res.message
        }
    }

    fun launchMusicService(packageName: String) {
        val ctx = getApplication<Application>()
        MediaRepository.launchMusicApp(ctx, packageName)
    }
}
