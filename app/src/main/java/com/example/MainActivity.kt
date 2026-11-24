package com.example

import android.Manifest
import android.content.Intent
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedContentTransitionScope
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.example.data.ActiveSessionInfo
import com.example.data.MusicServiceApp
import com.example.designsystem.MagicMusicTheme
import com.example.designsystem.MagicTheme
import com.example.designsystem.ThemePreferences
import com.example.ui.MagicMusicViewModel
import com.example.ui.components.MagicBottomNavBar
import com.example.ui.components.MagicContextMenuSheet
import com.example.ui.components.MagicMiniPlayer
import com.example.ui.components.MagicNavRail
import com.example.ui.navigation.MagicNavTab
import com.example.ui.navigation.MagicRoute
import com.example.ui.screens.AppearanceScreen
import com.example.ui.screens.AudioQualityScreen
import com.example.ui.screens.ControlsScreen
import com.example.ui.screens.DesignShowcaseHostScreen
import com.example.ui.screens.HomeDeckScreen
import com.example.ui.screens.LocalStatsScreen
import com.example.ui.screens.LyricsScreen
import com.example.ui.screens.NowPlayingScreen
import com.example.ui.screens.OnboardingScreen
import com.example.ui.screens.QueueScreen
import com.example.ui.screens.ServicesScreen
import com.example.ui.screens.SettingsScreen
import com.example.ui.screens.ShowcaseDestination
import com.example.ui.screens.SystemHealthScreen
import com.example.ui.screens.WidgetStudioScreen
import com.example.ui.screens.LibraryScreen

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            MagicMusicRoot()
        }
    }
}

@Composable
fun MagicMusicRoot(
    viewModel: MagicMusicViewModel = viewModel(),
) {
    val themePreferences by viewModel.themePreferences.collectAsStateWithLifecycle()

    MagicMusicTheme(themePreferences = themePreferences) {
        MagicMusicApp(
            viewModel = viewModel,
            themePreferences = themePreferences,
        )
    }
}

@Composable
fun MagicMusicApp(
    viewModel: MagicMusicViewModel = viewModel(),
    themePreferences: ThemePreferences = ThemePreferences(),
) {
    val context = LocalContext.current
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val artworkColors by viewModel.artworkColors.collectAsStateWithLifecycle()
    val navController = rememberNavController()
    val backStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = backStackEntry?.destination?.route ?: MagicRoute.Home.pattern
    val selectedTabId = MagicNavTab.fromRoute(currentRoute)?.routePattern ?: MagicNavTab.HOME.routePattern
    val showMainChrome = MagicRoute.isTopLevel(currentRoute)

    val snackbarHostState = remember { SnackbarHostState() }
    var sessionForContextMenu by remember { mutableStateOf<ActiveSessionInfo?>(null) }

    val tokens = MagicTheme.tokens
    val animDuration = tokens.motion.durationOrZero(240)

    val lifecycleOwner = LocalLifecycleOwner.current
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                viewModel.refreshSystemState()
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
        }
    }

    val notificationPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission(),
    ) { granted ->
        viewModel.refreshSystemState()
        viewModel.showFeedback(
            if (granted) {
                "Permiso de notificaciones concedido para pantalla de bloqueo"
            } else {
                "Activa notificaciones en Ajustes para ver controles en pantalla de bloqueo"
            },
        )
    }

    val requestPostNotificationsAction = remember(notificationPermissionLauncher) {
        {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
            } else {
                viewModel.refreshSystemState()
            }
        }
    }

    LaunchedEffect(state.feedbackMessage) {
        val msg = state.feedbackMessage
        if (!msg.isNullOrBlank()) {
            snackbarHostState.showSnackbar(msg)
            viewModel.clearFeedback()
        }
    }

    val navItems = remember {
        MagicNavTab.entries.map { it.toNavItemUiModel() }
    }

    val navigateToTab: (MagicNavTab) -> Unit = remember(navController) {
        { tab ->
            navController.navigate(tab.baseRoute()) {
                popUpTo(navController.graph.findStartDestination().id) {
                    saveState = true
                }
                launchSingleTop = true
                restoreState = true
            }
        }
    }

    BoxWithConstraints(
        modifier = Modifier
            .fillMaxSize()
            .background(tokens.backgroundBrush),
    ) {
        val useRail = maxWidth >= 720.dp

        Scaffold(
            containerColor = Color.Transparent,
            snackbarHost = { SnackbarHost(hostState = snackbarHostState) },
            contentWindowInsets = WindowInsets(0, 0, 0, 0),
            bottomBar = {
                if (!useRail && showMainChrome) {
                    Column(modifier = Modifier.fillMaxWidth()) {
                        MagicMiniPlayer(
                            media = state.media,
                            onPlayPause = viewModel::playPause,
                            onNext = viewModel::next,
                            onClick = { navController.navigate(MagicRoute.NowPlaying.pattern) },
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                        )
                        MagicBottomNavBar(
                            items = navItems,
                            selectedId = selectedTabId,
                            onSelect = { item ->
                                MagicNavTab.entries.firstOrNull { it.routePattern == item.id }?.let(navigateToTab)
                            },
                        )
                    }
                }
            },
        ) { innerPadding ->
            Row(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding),
            ) {
                if (useRail && showMainChrome) {
                    MagicNavRail(
                        items = navItems,
                        selectedId = selectedTabId,
                        onSelect = { item ->
                            MagicNavTab.entries.firstOrNull { it.routePattern == item.id }?.let(navigateToTab)
                        },
                    )
                }

                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxHeight(),
                    contentAlignment = Alignment.TopCenter,
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .let { m -> if (showMainChrome) m.widthIn(max = 680.dp).statusBarsPadding() else m },
                    ) {
                        if (useRail && showMainChrome) {
                            MagicMiniPlayer(
                                media = state.media,
                                onPlayPause = viewModel::playPause,
                                onNext = viewModel::next,
                                onClick = { navController.navigate(MagicRoute.NowPlaying.pattern) },
                                modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                            )
                        }

                        NavHost(
                            navController = navController,
                            startDestination = MagicRoute.Home.pattern,
                            enterTransition = { fadeIn(animationSpec = tween(animDuration)) },
                            exitTransition = { fadeOut(animationSpec = tween(animDuration)) },
                            popEnterTransition = { fadeIn(animationSpec = tween(animDuration)) },
                            popExitTransition = { fadeOut(animationSpec = tween(animDuration)) },
                            modifier = Modifier
                                .weight(1f)
                                .fillMaxWidth(),
                        ) {
                            composable(MagicRoute.Home.pattern) {
                                HomeDeckScreen(
                                    state = state,
                                    onSearchQueryChange = viewModel::setSearchQuery,
                                    onSelectSession = viewModel::selectSession,
                                    onOpenSessionMenu = { session -> sessionForContextMenu = session },
                                    onToggleLocalEngine = viewModel::toggleLocalAudioEngine,
                                    onOpenNowPlaying = { navController.navigate(MagicRoute.NowPlaying.pattern) },
                                    onOpenSystemHealth = { navController.navigate(MagicRoute.SystemHealth.pattern) },
                                    onOpenServices = { navController.navigate(MagicRoute.Services.pattern) },
                                    onOpenActiveApp = viewModel::launchMusicService,
                                    onPlayRecentTrack = { entry ->
                                        if (entry.service == MusicServiceApp.LOCAL_ENGINE) {
                                            viewModel.toggleLocalAudioEngine()
                                        } else if (entry.packageName.isNotBlank()) {
                                            viewModel.launchMusicService(entry.packageName)
                                        }
                                    },
                                )
                            }

                            composable(MagicRoute.Library.pattern) {
                                LibraryScreen(
                                    state = state,
                                    onSearchQueryChange = viewModel::setSearchQuery,
                                    onShowFeedback = viewModel::showFeedback,
                                )
                            }

                            composable(MagicRoute.Widgets.pattern) {
                                WidgetStudioScreen(
                                    state = state,
                                    artworkColors = artworkColors,
                                    onPlayPause = viewModel::playPause,
                                    onPrev = viewModel::previous,
                                    onNext = viewModel::next,
                                    onCustomAction = viewModel::executeCustomAction,
                                    onOpenSource = {
                                        if (state.media.packageName.isNotBlank() && !state.media.isLocalEngine) {
                                            viewModel.launchMusicService(state.media.packageName)
                                        } else {
                                            navController.navigate(MagicRoute.Services.pattern)
                                        }
                                    },
                                    onStyleSelected = viewModel::setWidgetStyle,
                                    onCornerRadiusChange = viewModel::setCornerRadius,
                                    onOpacityChange = viewModel::setGlassOpacity,
                                    onToggleProgressBar = viewModel::toggleShowProgressBar,
                                    onToggleServiceBadge = viewModel::toggleShowServiceBadge,
                                    onToggleCompactHeader = viewModel::toggleCompactHeader,
                                    onLeftCustomSelected = viewModel::setLeftCustomAction,
                                    onRightCustomSelected = viewModel::setRightCustomAction,
                                    onPinWidget = { fmt -> viewModel.pinWidget(fmt) },
                                    onSaveTemplate = viewModel::saveWidgetTemplate,
                                    onApplyTemplate = viewModel::applyWidgetTemplate,
                                    onDuplicateTemplate = viewModel::duplicateWidgetTemplate,
                                    onDeleteTemplate = viewModel::deleteWidgetTemplate,
                                    onExportTemplatesJson = viewModel::exportWidgetTemplatesJson,
                                    onImportTemplatesJson = viewModel::importWidgetTemplatesJson,
                                    onCopyFeedback = viewModel::showFeedback,
                                )
                            }

                            composable(
                                route = MagicRoute.Controls.pattern,
                                arguments = listOf(
                                    navArgument("tab") {
                                        type = NavType.IntType
                                        defaultValue = 0
                                    },
                                ),
                            ) { entry ->
                                val initialTab = entry.arguments?.getInt("tab") ?: 0
                                ControlsScreen(
                                    state = state,
                                    initialTab = initialTab,
                                    onToggleBridge = viewModel::toggleBridgeService,
                                    onToggleLockVisibility = viewModel::toggleShowOnLockScreen,
                                    onToggleLockArtwork = viewModel::toggleShowArtworkOnLockScreen,
                                    onSelectLockPrivacyLevel = viewModel::setLockPrivacyLevel,
                                    onSelectLockAutoCloseSeconds = viewModel::setLockAutoCloseSeconds,
                                    onToggleCompactActionIndex = viewModel::toggleCompactActionIndex,
                                    onDismissNotifyChannelGuide = viewModel::dismissNotifyChannelGuide,
                                    onToggleRequireUnlockToOpenApp = viewModel::toggleRequireUnlockToOpenApp,
                                    onToggleRequireUnlockToSwitchSource = viewModel::toggleRequireUnlockToSwitchSource,
                                    onToggleRequireUnlockForCustomActions = viewModel::toggleRequireUnlockForCustomActions,
                                    onSelectLockAction1 = viewModel::setLockAction1,
                                    onSelectLockAction2 = viewModel::setLockAction2,
                                    onSelectQsTileMode = viewModel::setQsTileMode,
                                    onRequestAddQuickTile = viewModel::requestAddQuickTile,
                                    onOpenLockDeck = viewModel::openLockScreenDeck,
                                    onPlayPause = viewModel::playPause,
                                    onNext = viewModel::next,
                                    onPrevious = viewModel::previous,
                                )
                            }

                            composable(MagicRoute.Settings.pattern) {
                                SettingsScreen(
                                    state = state,
                                    onOpenAppearance = { navController.navigate(MagicRoute.Appearance.pattern) },
                                    onOpenServices = { navController.navigate(MagicRoute.Services.pattern) },
                                    onOpenAudioQuality = { navController.navigate(MagicRoute.AudioQuality.pattern) },
                                    onOpenStats = { navController.navigate(MagicRoute.Stats.pattern) },
                                    onOpenSystemHealth = { navController.navigate(MagicRoute.SystemHealth.pattern) },
                                    onOpenOnboarding = { navController.navigate(MagicRoute.Onboarding.pattern) },
                                    onOpenDeveloperShowcase = {
                                        navController.navigate(MagicRoute.Developer.create(ShowcaseDestination.GALLERY.name))
                                    },
                                    onToggleBridge = viewModel::toggleBridgeService,
                                    onToggleLockVisibility = viewModel::toggleShowOnLockScreen,
                                    onToggleLockArtwork = viewModel::toggleShowArtworkOnLockScreen,
                                    onToggleRequireUnlockToOpenApp = viewModel::toggleRequireUnlockToOpenApp,
                                    onExportBackupJson = viewModel::exportFullBackupJson,
                                    onImportBackupJson = viewModel::importFullBackupJson,
                                    onShowFeedback = viewModel::showFeedback,
                                )
                            }

                            composable(
                                route = MagicRoute.NowPlaying.pattern,
                                enterTransition = {
                                    slideIntoContainer(
                                        towards = AnimatedContentTransitionScope.SlideDirection.Up,
                                        animationSpec = tween(animDuration),
                                    ) + fadeIn(animationSpec = tween(animDuration))
                                },
                                popExitTransition = {
                                    slideOutOfContainer(
                                        towards = AnimatedContentTransitionScope.SlideDirection.Down,
                                        animationSpec = tween(animDuration),
                                    ) + fadeOut(animationSpec = tween(animDuration))
                                },
                            ) {
                                NowPlayingScreen(
                                    state = state,
                                    artworkColors = artworkColors,
                                    onBack = { navController.popBackStack() },
                                    onPlayPause = viewModel::playPause,
                                    onNext = viewModel::next,
                                    onPrevious = viewModel::previous,
                                    onSeekTo = viewModel::seekTo,
                                    onSeekBy = viewModel::seekBy,
                                    onToggleFavorite = viewModel::toggleFavorite,
                                    onToggleShuffle = viewModel::toggleShuffle,
                                    onToggleRepeat = viewModel::toggleRepeat,
                                    onOpenLyrics = { navController.navigate(MagicRoute.Lyrics.pattern) },
                                    onOpenQueue = { navController.navigate(MagicRoute.Queue.pattern) },
                                    onStartSleepTimerCountdown = viewModel::startSleepTimerCountdown,
                                    onStartSleepTimerEndOfTrack = viewModel::startSleepTimerEndOfTrack,
                                    onToggleSleepFade = viewModel::setSleepTimerFadeOut,
                                    onCancelSleepTimer = viewModel::cancelSleepTimer,
                                    onSetVolumePercent = viewModel::setVolumePercent,
                                    onRefreshOutputs = viewModel::refreshAudioOutputs,
                                    onSelectOutput = viewModel::selectAudioOutput,
                                    onAddToQuickPlaylist = viewModel::addTrackToQuickPlaylist,
                                    onViewArtist = { artist ->
                                        viewModel.setSearchQuery(artist)
                                        navigateToTab(MagicNavTab.HOME)
                                    },
                                    onOpenSourceApp = viewModel::launchMusicService,
                                )
                            }

                            composable(MagicRoute.Lyrics.pattern) {
                                LyricsScreen(
                                    state = state,
                                    artworkColors = artworkColors,
                                    onBack = { navController.popBackStack() },
                                    onPlayPause = viewModel::playPause,
                                    onStartLocalEngine = viewModel::toggleLocalAudioEngine,
                                )
                            }

                            composable(MagicRoute.Queue.pattern) {
                                QueueScreen(
                                    state = state,
                                    onBack = { navController.popBackStack() },
                                    onPlayPause = viewModel::playPause,
                                    onNext = viewModel::next,
                                    onSkipToQueueItem = viewModel::skipToQueueItem,
                                    onReorderLocalQueue = viewModel::reorderLocalQueue,
                                    onOpenNowPlaying = { navController.popBackStack() },
                                    onStartLocalEngine = viewModel::toggleLocalAudioEngine,
                                )
                            }

                            composable(MagicRoute.Services.pattern) {
                                ServicesScreen(
                                    state = state,
                                    onLaunchService = viewModel::launchMusicService,
                                    onRefreshServices = viewModel::refreshSystemState,
                                    onGrantListenerPermission = viewModel::openNotificationListenerSettings,
                                    onPriorityRuleSelected = viewModel::setSessionPriorityRule,
                                    onToggleIgnoreVideo = viewModel::toggleIgnoreVideoSessions,
                                    onToggleManagedPackage = viewModel::toggleManagedPackageEnabled,
                                    onSetPreferredPackage = viewModel::setPreferredServicePackage,
                                    onOpenBatterySettings = viewModel::openBatteryOptimizationSettings,
                                    onBack = { navController.popBackStack() },
                                )
                            }

                            composable(MagicRoute.Appearance.pattern) {
                                AppearanceScreen(
                                    themePreferences = themePreferences,
                                    onSelectThemeMode = viewModel::setThemeMode,
                                    onSelectSurfaceTone = viewModel::setSurfaceToneStyle,
                                    onSelectSeedSwatch = { swatch -> viewModel.setSeedColor(swatch.argb) },
                                    onToggleDynamicColor = viewModel::setUseDynamicColor,
                                    onTogglePureAmoled = viewModel::setPureAmoledBlack,
                                    onToggleColorFromArtwork = viewModel::setColorFromArtwork,
                                    onToggleReduceAnimations = viewModel::setReduceAnimations,
                                    onBack = { navController.popBackStack() },
                                )
                            }

                            composable(MagicRoute.SystemHealth.pattern) {
                                SystemHealthScreen(
                                    state = state,
                                    onBack = { navController.popBackStack() },
                                    onFixItem = { id ->
                                        viewModel.fixHealthCheck(id, requestPostNotificationsAction)
                                    },
                                )
                            }

                            composable(MagicRoute.AudioQuality.pattern) {
                                AudioQualityScreen(
                                    state = state,
                                    onBack = { navController.popBackStack() },
                                    onToggleEqEnabled = viewModel::setEqualizerEnabled,
                                    onBandChange = viewModel::setEqualizerBand,
                                    onSelectPreset = viewModel::setEqualizerPreset,
                                    onModeChange = viewModel::setEqualizerAdvanced,
                                    onToggleHiResBuffer = viewModel::setHiResBufferHigh,
                                    onRefreshOutputs = viewModel::refreshAudioOutputs,
                                    onSelectOutput = viewModel::selectAudioOutput,
                                )
                            }

                            composable(MagicRoute.Stats.pattern) {
                                LocalStatsScreen(
                                    state = state,
                                    onBack = { navController.popBackStack() },
                                    onClearHistory = viewModel::clearLocalStats,
                                    onOpenActiveApp = viewModel::launchMusicService,
                                )
                            }

                            composable(MagicRoute.Onboarding.pattern) {
                                OnboardingScreen(
                                    state = state,
                                    onBack = { navController.popBackStack() },
                                    onFixStep = { id ->
                                        viewModel.fixHealthCheck(id, requestPostNotificationsAction)
                                    },
                                    onComplete = {
                                        viewModel.completeOnboarding()
                                        navController.popBackStack()
                                    },
                                )
                            }

                            if (BuildConfig.DEBUG) {
                                composable(
                                    route = MagicRoute.Developer.pattern,
                                    arguments = listOf(
                                        navArgument("dest") {
                                            type = NavType.StringType
                                            defaultValue = ShowcaseDestination.GALLERY.name
                                        },
                                    ),
                                ) { entry ->
                                    val rawDest = entry.arguments?.getString("dest")
                                    var activeDest by remember(rawDest) {
                                        mutableStateOf(
                                            runCatching { ShowcaseDestination.valueOf(rawDest.orEmpty()) }
                                                .getOrDefault(ShowcaseDestination.GALLERY),
                                        )
                                    }
                                    Box(
                                        modifier = Modifier
                                            .fillMaxSize()
                                            .statusBarsPadding(),
                                    ) {
                                        DesignShowcaseHostScreen(
                                            destination = activeDest,
                                            appThemePreferences = themePreferences,
                                            media = state.media,
                                            artworkColors = artworkColors,
                                            onPlayPause = viewModel::playPause,
                                            onNext = viewModel::next,
                                            onPrevious = viewModel::previous,
                                            onSeek = viewModel::seekTo,
                                            onCustomAction = viewModel::executeCustomAction,
                                            onSelectSession = viewModel::selectSession,
                                            onOpenActiveApp = viewModel::launchMusicService,
                                            onDestinationChange = { activeDest = it },
                                            onBack = { navController.popBackStack() },
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

    sessionForContextMenu?.let { session ->
        MagicContextMenuSheet(
            title = session.title.ifBlank { session.appLabel },
            artist = session.artist.ifBlank { session.service.displayName },
            bitmap = if (session.packageName == state.media.packageName) state.media.albumArt else null,
            onDismiss = { sessionForContextMenu = null },
            onActionSelected = { label ->
                when {
                    label.contains("playlist", ignoreCase = true) || label.contains("lista", ignoreCase = true) -> {
                        viewModel.selectSession(session.packageName)
                        viewModel.addTrackToQuickPlaylist()
                    }
                    label.contains("Compartir", ignoreCase = true) -> {
                        val shareIntent = Intent(Intent.ACTION_SEND).apply {
                            type = "text/plain"
                            putExtra(
                                Intent.EXTRA_TEXT,
                                "Escuchando ${session.title} — ${session.artist} en ${session.appLabel}",
                            )
                        }
                        context.startActivity(Intent.createChooser(shareIntent, "Compartir pista"))
                    }
                    label.contains("artista", ignoreCase = true) -> {
                        if (session.artist.isNotBlank()) {
                            viewModel.setSearchQuery(session.artist)
                        }
                    }
                    label.contains("Abrir", ignoreCase = true) && session.packageName.isNotBlank() -> {
                        viewModel.launchMusicService(session.packageName)
                    }
                }
            },
        )
    }
}
