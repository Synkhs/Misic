package com.example

import android.Manifest
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Album
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.Lock
import androidx.compose.material.icons.outlined.Widgets
import androidx.compose.material.icons.rounded.Album
import androidx.compose.material.icons.rounded.Home
import androidx.compose.material.icons.rounded.Lock
import androidx.compose.material.icons.rounded.Widgets
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.NavigationRail
import androidx.compose.material3.NavigationRailItem
import androidx.compose.material3.NavigationRailItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.example.ui.MagicMusicViewModel
import com.example.ui.components.MagicCapsuleBanner
import com.example.ui.screens.HomeDeckScreen
import com.example.ui.screens.LockAndQuickSettingsScreen
import com.example.ui.screens.ServicesScreen
import com.example.ui.screens.WidgetStudioScreen
import com.example.ui.theme.HonorCyan
import com.example.ui.theme.MagicDeepNavy
import com.example.ui.theme.MagicObsidian
import com.example.ui.theme.MyApplicationTheme

private enum class MagicNavTab(
    val route: String,
    val label: String,
    val selectedIcon: ImageVector,
    val unselectedIcon: ImageVector,
    val testTag: String,
) {
    HOME("home", "Inicio", Icons.Rounded.Home, Icons.Outlined.Home, "nav_tab_home"),
    WIDGETS("widgets", "Widgets", Icons.Rounded.Widgets, Icons.Outlined.Widgets, "nav_tab_widgets"),
    LOCK_QS("lock_qs", "Bloqueo", Icons.Rounded.Lock, Icons.Outlined.Lock, "nav_tab_lock_qs"),
    SERVICES("services", "Servicios", Icons.Rounded.Album, Icons.Outlined.Album, "nav_tab_services"),
}

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            MyApplicationTheme(darkTheme = true) {
                MagicMusicApp()
            }
        }
    }
}

@Composable
fun MagicMusicApp(
    viewModel: MagicMusicViewModel = viewModel(),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val navController = rememberNavController()
    val backStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = backStackEntry?.destination?.route ?: MagicNavTab.HOME.route
    val snackbarHostState = remember { SnackbarHostState() }

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

    val requestPostNotificationsAction = {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
        } else {
            viewModel.refreshSystemState()
        }
    }

    LaunchedEffect(state.feedbackMessage) {
        val msg = state.feedbackMessage
        if (!msg.isNullOrBlank()) {
            snackbarHostState.showSnackbar(msg)
            viewModel.clearFeedback()
        }
    }

    val navigateToTab: (MagicNavTab) -> Unit = { tab ->
        navController.navigate(tab.route) {
            popUpTo(navController.graph.findStartDestination().id) {
                saveState = true
            }
            launchSingleTop = true
            restoreState = true
        }
    }

    BoxWithConstraints(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    listOf(MagicObsidian, MagicDeepNavy, MagicObsidian),
                ),
            ),
    ) {
        val useRail = maxWidth >= 720.dp

        Scaffold(
            containerColor = Color.Transparent,
            snackbarHost = { SnackbarHost(hostState = snackbarHostState) },
            contentWindowInsets = WindowInsets(0, 0, 0, 0),
            bottomBar = {
                if (!useRail) {
                    NavigationBar(
                        containerColor = Color(0xEB0A101F),
                        windowInsets = WindowInsets.navigationBars,
                    ) {
                        MagicNavTab.entries.forEach { tab ->
                            val selected = currentRoute == tab.route
                            NavigationBarItem(
                                selected = selected,
                                onClick = { navigateToTab(tab) },
                                icon = {
                                    Icon(
                                        imageVector = if (selected) tab.selectedIcon else tab.unselectedIcon,
                                        contentDescription = tab.label,
                                    )
                                },
                                label = { Text(tab.label) },
                                colors = NavigationBarItemDefaults.colors(
                                    selectedIconColor = HonorCyan,
                                    selectedTextColor = HonorCyan,
                                    indicatorColor = HonorCyan.copy(alpha = 0.16f),
                                    unselectedIconColor = Color(0xFF94A3B8),
                                    unselectedTextColor = Color(0xFF94A3B8),
                                ),
                                modifier = Modifier.testTag(tab.testTag),
                            )
                        }
                    }
                }
            },
        ) { innerPadding ->
            Row(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding),
            ) {
                if (useRail) {
                    NavigationRail(
                        containerColor = Color(0xEB0A101F),
                        modifier = Modifier
                            .fillMaxHeight()
                            .statusBarsPadding(),
                    ) {
                        MagicNavTab.entries.forEach { tab ->
                            val selected = currentRoute == tab.route
                            NavigationRailItem(
                                selected = selected,
                                onClick = { navigateToTab(tab) },
                                icon = {
                                    Icon(
                                        imageVector = if (selected) tab.selectedIcon else tab.unselectedIcon,
                                        contentDescription = tab.label,
                                    )
                                },
                                label = { Text(tab.label) },
                                colors = NavigationRailItemDefaults.colors(
                                    selectedIconColor = HonorCyan,
                                    selectedTextColor = HonorCyan,
                                    indicatorColor = HonorCyan.copy(alpha = 0.16f),
                                    unselectedIconColor = Color(0xFF94A3B8),
                                    unselectedTextColor = Color(0xFF94A3B8),
                                ),
                                modifier = Modifier.testTag(tab.testTag),
                            )
                        }
                    }
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
                            .widthIn(max = 680.dp)
                            .statusBarsPadding(),
                    ) {
                        MagicCapsuleBanner(
                            media = state.media,
                            onPlayPause = viewModel::playPause,
                            onBannerClick = { navigateToTab(MagicNavTab.HOME) },
                            modifier = Modifier.padding(horizontal = 16.dp, vertical = 10.dp),
                        )

                        NavHost(
                            navController = navController,
                            startDestination = MagicNavTab.HOME.route,
                            modifier = Modifier
                                .weight(1f)
                                .fillMaxWidth(),
                        ) {
                            composable(MagicNavTab.HOME.route) {
                                HomeDeckScreen(
                                    state = state,
                                    onPlayPause = viewModel::playPause,
                                    onNext = viewModel::next,
                                    onPrevious = viewModel::previous,
                                    onSeek = viewModel::seekTo,
                                    onCustomAction = viewModel::executeCustomAction,
                                    onSelectSession = viewModel::selectSession,
                                    onToggleLocalEngine = viewModel::toggleLocalAudioEngine,
                                    onGrantListenerPermission = viewModel::openNotificationListenerSettings,
                                    onRequestPostNotifications = requestPostNotificationsAction,
                                    onPinWidget = { viewModel.pinWidget(isCapsule = false) },
                                    onAddQuickTile = { viewModel.requestAddQuickSettingsTile(isPlayPause = true) },
                                    onOpenActiveApp = viewModel::launchMusicService,
                                    onNavigateToWidgets = { navigateToTab(MagicNavTab.WIDGETS) },
                                )
                            }

                            composable(MagicNavTab.WIDGETS.route) {
                                WidgetStudioScreen(
                                    state = state,
                                    onPlayPause = viewModel::playPause,
                                    onPrev = viewModel::previous,
                                    onNext = viewModel::next,
                                    onCustomAction = viewModel::executeCustomAction,
                                    onOpenSource = {
                                        if (state.media.packageName.isNotBlank() && !state.media.isLocalEngine) {
                                            viewModel.launchMusicService(state.media.packageName)
                                        } else {
                                            navigateToTab(MagicNavTab.SERVICES)
                                        }
                                    },
                                    onStyleSelected = viewModel::setWidgetStyle,
                                    onCornerRadiusChange = viewModel::setCornerRadius,
                                    onOpacityChange = viewModel::setGlassOpacity,
                                    onToggleProgressBar = viewModel::toggleShowProgressBar,
                                    onToggleServiceBadge = viewModel::toggleShowServiceBadge,
                                    onToggleCapsuleHeader = viewModel::toggleMagicCapsuleHeader,
                                    onLeftCustomSelected = viewModel::setLeftCustomAction,
                                    onRightCustomSelected = viewModel::setRightCustomAction,
                                    onPinWidget = viewModel::pinWidget,
                                )
                            }

                            composable(MagicNavTab.LOCK_QS.route) {
                                LockAndQuickSettingsScreen(
                                    state = state,
                                    onToggleBridge = viewModel::toggleBridgeService,
                                    onToggleLockVisibility = viewModel::toggleShowOnLockScreen,
                                    onSelectLockAction1 = viewModel::setLockAction1,
                                    onSelectLockAction2 = viewModel::setLockAction2,
                                    onSelectQsTileMode = viewModel::setQsTileMode,
                                    onRequestAddTile = viewModel::requestAddQuickSettingsTile,
                                    onOpenLockDeck = viewModel::openLockScreenDeck,
                                    onPlayPause = viewModel::playPause,
                                    onNext = viewModel::next,
                                    onPrevious = viewModel::previous,
                                )
                            }

                            composable(MagicNavTab.SERVICES.route) {
                                ServicesScreen(
                                    state = state,
                                    onLaunchService = viewModel::launchMusicService,
                                    onRefreshServices = viewModel::refreshSystemState,
                                    onGrantListenerPermission = viewModel::openNotificationListenerSettings,
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}
