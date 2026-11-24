package com.example.ui.navigation

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.LibraryMusic
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material.icons.outlined.Tune
import androidx.compose.material.icons.outlined.Widgets
import androidx.compose.material.icons.rounded.Home
import androidx.compose.material.icons.rounded.LibraryMusic
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.material.icons.rounded.Tune
import androidx.compose.material.icons.rounded.Widgets
import androidx.compose.ui.graphics.vector.ImageVector
import com.example.ui.components.NavItemUiModel

/**
 * 5 top-level destinations including Biblioteca.
 */
enum class MagicNavTab(
    val routePattern: String,
    val label: String,
    val selectedIcon: ImageVector,
    val unselectedIcon: ImageVector,
    val testTag: String,
) {
    HOME("home", "Inicio", Icons.Rounded.Home, Icons.Outlined.Home, "nav_tab_home"),
    LIBRARY("library", "Biblioteca", Icons.Rounded.LibraryMusic, Icons.Outlined.LibraryMusic, "nav_tab_library"),
    WIDGETS("widgets", "Widgets", Icons.Rounded.Widgets, Icons.Outlined.Widgets, "nav_tab_widgets"),
    CONTROLS("controls?tab={tab}", "Controles", Icons.Rounded.Tune, Icons.Outlined.Tune, "nav_tab_controls"),
    SETTINGS("settings", "Ajustes", Icons.Rounded.Settings, Icons.Outlined.Settings, "nav_tab_settings");

    fun baseRoute(): String = when (this) {
        CONTROLS -> "controls?tab=0"
        else -> routePattern
    }

    fun toNavItemUiModel(): NavItemUiModel = NavItemUiModel(
        id = routePattern,
        label = label,
        selectedIcon = selectedIcon,
        unselectedIcon = unselectedIcon,
        testTag = testTag,
    )

    companion object {
        fun fromRoute(route: String?): MagicNavTab? {
            if (route.isNullOrBlank()) return HOME
            val base = route.substringBefore("?").substringBefore("/")
            return entries.firstOrNull { it.routePattern.substringBefore("?") == base }
        }
    }
}

sealed class MagicRoute(val pattern: String) {
    data object Home : MagicRoute("home")
    data object Library : MagicRoute("library")
    data object Widgets : MagicRoute("widgets")
    data object Controls : MagicRoute("controls?tab={tab}") {
        fun create(tabIndex: Int = 0): String = "controls?tab=${tabIndex.coerceIn(0, 1)}"
    }
    data object Settings : MagicRoute("settings")
    data object NowPlaying : MagicRoute("now_playing")
    data object Lyrics : MagicRoute("lyrics")
    data object Queue : MagicRoute("queue")
    data object Services : MagicRoute("services")
    data object Appearance : MagicRoute("appearance")
    data object SystemHealth : MagicRoute("system_health")
    data object AudioQuality : MagicRoute("audio_quality")
    data object Stats : MagicRoute("stats")
    data object Onboarding : MagicRoute("onboarding")
    data object Developer : MagicRoute("developer?dest={dest}") {
        fun create(destName: String = "GALLERY"): String = "developer?dest=$destName"
    }

    companion object {
        private val topLevelBases = setOf("home", "library", "widgets", "controls", "settings")

        fun isTopLevel(route: String?): Boolean {
            if (route.isNullOrBlank()) return true
            val base = route.substringBefore("?").substringBefore("/")
            return base in topLevelBases
        }
    }
}
