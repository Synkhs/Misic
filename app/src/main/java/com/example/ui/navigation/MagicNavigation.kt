package com.example.ui.navigation

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.QueueMusic
import androidx.compose.material.icons.automirrored.rounded.QueueMusic
import androidx.compose.material.icons.outlined.GraphicEq
import androidx.compose.material.icons.outlined.LibraryMusic
import androidx.compose.material.icons.outlined.Palette
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material.icons.rounded.GraphicEq
import androidx.compose.material.icons.rounded.LibraryMusic
import androidx.compose.material.icons.rounded.Palette
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.ui.graphics.vector.ImageVector
import com.example.designsystem.expressive.MagicNavChromeItem
import com.example.ui.components.NavItemUiModel

/**
 * 5 top-level destinations in pure M3 Expressive navigation:
 * Biblioteca, Listas, Ecualizador, Estilo, Ajustes.
 */
enum class MagicNavTab(
    val routePattern: String,
    val label: String,
    val selectedIcon: ImageVector,
    val unselectedIcon: ImageVector,
    val testTag: String,
) {
    LIBRARY("library", "Biblioteca", Icons.Rounded.LibraryMusic, Icons.Outlined.LibraryMusic, "nav_tab_library"),
    PLAYLISTS("playlists", "Listas", Icons.AutoMirrored.Rounded.QueueMusic, Icons.AutoMirrored.Outlined.QueueMusic, "nav_tab_playlists"),
    EQUALIZER("equalizer", "Ecualizador", Icons.Rounded.GraphicEq, Icons.Outlined.GraphicEq, "nav_tab_equalizer"),
    STYLE("style", "Estilo", Icons.Rounded.Palette, Icons.Outlined.Palette, "nav_tab_style"),
    SETTINGS("settings", "Ajustes", Icons.Rounded.Settings, Icons.Outlined.Settings, "nav_tab_settings");

    fun baseRoute(): String = routePattern

    fun toNavItemUiModel(): NavItemUiModel = NavItemUiModel(
        id = routePattern,
        label = label,
        selectedIcon = selectedIcon,
        unselectedIcon = unselectedIcon,
        testTag = testTag,
    )

    fun toExpressiveChromeItem(): MagicNavChromeItem = MagicNavChromeItem(
        id = routePattern,
        label = label,
        selectedIcon = selectedIcon,
        unselectedIcon = unselectedIcon,
        testTag = testTag,
    )

    companion object {
        fun fromRoute(route: String?): MagicNavTab? {
            if (route.isNullOrBlank()) return LIBRARY
            val base = route.substringBefore("?").substringBefore("/")
            return entries.firstOrNull { it.routePattern.substringBefore("?") == base }
        }
    }
}

sealed class MagicRoute(val pattern: String) {
    data object Library : MagicRoute("library")
    data object Playlists : MagicRoute("playlists")
    data object Equalizer : MagicRoute("equalizer")
    data object Style : MagicRoute("style")
    data object Settings : MagicRoute("settings")

    // Secondary / modal / deep destinations
    data object NowPlaying : MagicRoute("now_playing")
    data object Lyrics : MagicRoute("lyrics")
    data object Queue : MagicRoute("queue")
    data object Services : MagicRoute("services")
    data object Controls : MagicRoute("controls?tab={tab}") {
        fun create(tabIndex: Int = 0): String = "controls?tab=${tabIndex.coerceIn(0, 1)}"
    }
    data object WidgetStudio : MagicRoute("widget_studio")
    data object SystemHealth : MagicRoute("system_health")
    data object Stats : MagicRoute("stats")
    data object Onboarding : MagicRoute("onboarding")
    data object Developer : MagicRoute("developer?dest={dest}") {
        fun create(destName: String = "GALLERY"): String = "developer?dest=$destName"
    }

    companion object {
        private val topLevelBases = setOf("library", "playlists", "equalizer", "style", "settings")

        fun isTopLevel(route: String?): Boolean {
            if (route.isNullOrBlank()) return true
            val base = route.substringBefore("?").substringBefore("/")
            return base in topLevelBases
        }
    }
}
