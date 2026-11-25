package com.example.designsystem

import android.app.Activity
import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.ColorScheme
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
import com.example.designsystem.expressive.MagicExpressiveRootTheme
import com.example.designsystem.expressive.buildShapesForPreset

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
        themePreferences.schemeVariant,
        themePreferences.contrastLevel,
        themePreferences.roleOverrides,
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
                    val contrasted = MaterialColorUtilitiesEngine.enforceSchemeContrastAA(
                        adjusted,
                        themePreferences.contrastLevel.minAaTextRatio,
                    )
                    if (isDark && themePreferences.pureAmoledBlack) applyAmoledBlack(contrasted) else contrasted
                } else {
                    generateTonalColorScheme(
                        seedColorArgb = themePreferences.seedColorArgb,
                        isDark = isDark,
                        surfaceToneStyle = themePreferences.surfaceToneStyle,
                        pureAmoled = themePreferences.pureAmoledBlack,
                        schemeVariant = themePreferences.schemeVariant,
                        contrastPreset = themePreferences.contrastLevel,
                        roleOverrides = themePreferences.roleOverrides,
                    )
                }
            }
            is AppTheme.Material3Static -> {
                generateTonalColorScheme(
                    seedColorArgb = appTheme.seedColorArgb,
                    isDark = isDark,
                    surfaceToneStyle = themePreferences.surfaceToneStyle,
                    pureAmoled = themePreferences.pureAmoledBlack,
                    schemeVariant = themePreferences.schemeVariant,
                    contrastPreset = themePreferences.contrastLevel,
                    roleOverrides = themePreferences.roleOverrides,
                )
            }
        }
    }

    val tokens = remember(
        appTheme,
        resolvedColorScheme,
        isDark,
        themePreferences,
        dynamicSupported,
    ) {
        val radii = radiiForPreset(themePreferences.geometryPreset)
        MagicThemeTokens(
            theme = appTheme,
            isDark = isDark,
            pureAmoled = isDark && themePreferences.pureAmoledBlack,
            usesDynamicColor = appTheme is AppTheme.Material3Dynamic && dynamicSupported,
            colorFromArtwork = themePreferences.colorFromArtwork,
            useExpressiveM3 = themePreferences.useExpressiveM3,
            hapticFeedbackEnabled = themePreferences.hapticFeedbackEnabled,
            geometryPreset = themePreferences.geometryPreset,
            artworkShapeStyle = themePreferences.artworkShape,
            navLabelVisibility = themePreferences.navLabelVisibility,
            miniPlayerStyle = themePreferences.miniPlayerStyle,
            libraryGridColumns = themePreferences.libraryGridColumns.coerceIn(1, 3),
            lyricsAutoScroll = themePreferences.lyricsAutoScroll,
            lyricsFontScale = themePreferences.lyricsFontScale,
            radii = radii,
            motion = MagicMotion(reduceAnimations = themePreferences.reduceAnimations),
            semantic = buildSemanticColorsForM3(
                colorScheme = resolvedColorScheme,
                isDark = isDark,
                pureAmoled = themePreferences.pureAmoledBlack,
            ),
            backgroundColor = buildFlatBackgroundColor(
                colorScheme = resolvedColorScheme,
                isDark = isDark,
                pureAmoled = themePreferences.pureAmoledBlack,
            ),
            shapes = buildShapesForPreset(themePreferences.geometryPreset),
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
        MagicExpressiveRootTheme(
            colorScheme = resolvedColorScheme,
            shapes = tokens.shapes,
            typography = MagicTypography,
            useExpressiveM3 = themePreferences.useExpressiveM3,
            reduceAnimations = themePreferences.reduceAnimations,
            content = content,
        )
    }
}
