package com.example.ui.theme

import androidx.compose.runtime.Composable
import com.example.designsystem.MagicMusicTheme
import com.example.designsystem.SurfaceToneStyle
import com.example.designsystem.ThemeMode
import com.example.designsystem.ThemePreferences

@Composable
fun MyApplicationTheme(
    darkTheme: Boolean? = null,
    dynamicColor: Boolean = false,
    content: @Composable () -> Unit,
) {
    val resolvedMode = when (darkTheme) {
        true -> ThemeMode.DARK
        false -> ThemeMode.LIGHT
        null -> ThemeMode.SYSTEM
    }
    val prefs = ThemePreferences(
        useDynamicColor = dynamicColor,
        themeMode = resolvedMode,
        surfaceToneStyle = if (resolvedMode == ThemeMode.LIGHT) SurfaceToneStyle.LIGHT else SurfaceToneStyle.TONAL,
    )
    MagicMusicTheme(themePreferences = prefs, content = content)
}
