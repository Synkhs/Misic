package com.example.designsystem

import android.app.Activity
import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat

object MagicTheme {
    val tokens: MagicThemeTokens
        @Composable
        @ReadOnlyComposable
        get() = LocalMagicTokens.current
}

@Composable
fun MagicMusicTheme(
    themePreferences: ThemePreferences = ThemePreferences(),
    content: @Composable () -> Unit,
) {
    val context = LocalContext.current
    val systemDark = isSystemInDarkTheme()
    val isDark = themePreferences.resolveDarkTheme(systemDark)
    val dynamicSupported = Build.VERSION.SDK_INT >= Build.VERSION_CODES.S
    val appTheme = remember(themePreferences.useDynamicColor, themePreferences.seedColorArgb) {
        themePreferences.toAppTheme()
    }

    val resolvedColorScheme: ColorScheme = remember(
        appTheme,
        isDark,
        themePreferences.surfaceToneStyle,
        themePreferences.pureAmoledBlack,
        dynamicSupported,
        context,
    ) {
        when (appTheme) {
            AppTheme.Material3Dynamic -> {
                if (dynamicSupported) {
                    val dyn = if (isDark) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
                    val adjusted = if (isDark && themePreferences.surfaceToneStyle == SurfaceToneStyle.DARK) {
                        applyFlatDarkSurface(dyn)
                    } else {
                        dyn
                    }
                    if (isDark && themePreferences.pureAmoledBlack) applyAmoledBlack(adjusted) else adjusted
                } else {
                    generateTonalColorScheme(
                        seedColorArgb = themePreferences.seedColorArgb,
                        isDark = isDark,
                        surfaceToneStyle = themePreferences.surfaceToneStyle,
                        pureAmoled = themePreferences.pureAmoledBlack,
                    )
                }
            }
            is AppTheme.Material3Static -> {
                generateTonalColorScheme(
                    seedColorArgb = appTheme.seedColorArgb,
                    isDark = isDark,
                    surfaceToneStyle = themePreferences.surfaceToneStyle,
                    pureAmoled = themePreferences.pureAmoledBlack,
                )
            }
        }
    }

    val tokens = remember(
        appTheme,
        resolvedColorScheme,
        isDark,
        themePreferences.pureAmoledBlack,
        themePreferences.colorFromArtwork,
        themePreferences.reduceAnimations,
        dynamicSupported,
    ) {
        val radii = MagicRadii()
        MagicThemeTokens(
            theme = appTheme,
            isDark = isDark,
            pureAmoled = isDark && themePreferences.pureAmoledBlack,
            usesDynamicColor = appTheme is AppTheme.Material3Dynamic && dynamicSupported,
            colorFromArtwork = themePreferences.colorFromArtwork,
            radii = radii,
            motion = MagicMotion(reduceAnimations = themePreferences.reduceAnimations),
            semantic = buildSemanticColorsForM3(
                colorScheme = resolvedColorScheme,
                isDark = isDark,
                pureAmoled = themePreferences.pureAmoledBlack,
            ),
            backgroundBrush = buildBackgroundBrush(
                colorScheme = resolvedColorScheme,
                isDark = isDark,
                pureAmoled = themePreferences.pureAmoledBlack,
            ),
            shapes = buildMagicShapes(radii),
        )
    }

    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as? Activity)?.window
            if (window != null) {
                val controller = WindowCompat.getInsetsController(window, view)
                controller.isAppearanceLightStatusBars = !isDark
                controller.isAppearanceLightNavigationBars = !isDark
            }
        }
    }

    CompositionLocalProvider(LocalMagicTokens provides tokens) {
        MaterialTheme(
            colorScheme = resolvedColorScheme,
            typography = MagicTypography,
            shapes = tokens.shapes,
            content = content,
        )
    }
}
