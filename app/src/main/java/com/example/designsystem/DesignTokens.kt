package com.example.designsystem

import androidx.compose.material3.Shapes
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.example.designsystem.expressive.ArtworkShapeStyle
import com.example.designsystem.expressive.GeometryPreset
import com.example.designsystem.expressive.MiniPlayerStyle
import com.example.designsystem.expressive.NavLabelVisibility
import com.example.designsystem.expressive.buildShapesForPreset

@Immutable
data class MagicSpacing(
    val xxs: Dp = 4.dp,
    val xs: Dp = 8.dp,
    val sm: Dp = 12.dp,
    val md: Dp = 16.dp,
    val screenMargin: Dp = 20.dp,
    val lg: Dp = 24.dp,
    val xl: Dp = 32.dp,
)

@Immutable
data class MagicRadii(
    val extraSmall: Dp = 8.dp,
    val listItem: Dp = 16.dp,
    val miniPlayer: Dp = 18.dp,
    val medium: Dp = 20.dp,
    val card: Dp = 24.dp,
    val artworkMedium: Dp = 20.dp,
    val artworkLarge: Dp = 28.dp,
    val sheet: Dp = 28.dp,
)

@Immutable
data class MagicElevations(
    val none: Dp = 0.dp,
    val card: Dp = 1.dp,
    val miniPlayer: Dp = 3.dp,
    val sheet: Dp = 6.dp,
)

@Immutable
data class MagicMotion(
    val reduceAnimations: Boolean = false,
    val quickDurationMs: Int = 150,
    val standardDurationMs: Int = 240,
    val emphasizedDurationMs: Int = 340,
) {
    fun durationOrZero(ms: Int): Int = if (reduceAnimations) 0 else ms
}

@Immutable
data class SemanticColors(
    val success: Color,
    val onSuccess: Color,
    val warning: Color,
    val onWarning: Color,
    val info: Color,
    val cardSurface: Color,
    val elevatedCardSurface: Color,
    val selectedContainer: Color,
    val onSelectedContainer: Color,
    val divider: Color,
    val mutedText: Color,
    val miniPlayerSurface: Color,
    val navBarContainer: Color,
    val navPillIndicator: Color,
    val playButtonSurface: Color,
    val onPlayButtonSurface: Color,
)

@Immutable
data class ArtworkPaletteColors(
    val dominant: Color,
    val vibrantAccent: Color,
    val darkTintSurface: Color,
    val onArtworkText: Color,
)

@Immutable
data class MagicThemeTokens(
    val theme: AppTheme,
    val isDark: Boolean,
    val pureAmoled: Boolean,
    val usesDynamicColor: Boolean,
    val colorFromArtwork: Boolean,
    val useExpressiveM3: Boolean = true,
    val hapticFeedbackEnabled: Boolean = true,
    val geometryPreset: GeometryPreset = GeometryPreset.STANDARD_M3,
    val artworkShapeStyle: ArtworkShapeStyle = ArtworkShapeStyle.ROUNDED,
    val navLabelVisibility: NavLabelVisibility = NavLabelVisibility.ALWAYS,
    val miniPlayerStyle: MiniPlayerStyle = MiniPlayerStyle.CARD,
    val libraryGridColumns: Int = 2,
    val lyricsAutoScroll: Boolean = true,
    val lyricsFontScale: Float = 1f,
    val spacing: MagicSpacing = MagicSpacing(),
    val radii: MagicRadii = MagicRadii(),
    val elevations: MagicElevations = MagicElevations(),
    val motion: MagicMotion = MagicMotion(),
    val semantic: SemanticColors,
    val backgroundColor: Color,
    val shapes: Shapes,
)

fun radiiForPreset(preset: GeometryPreset): MagicRadii = MagicRadii(
    extraSmall = 8.dp,
    listItem = preset.listItemRadiusDp.dp,
    miniPlayer = (preset.cardRadiusDp - 6).coerceAtLeast(12).dp,
    medium = preset.artworkMediumDp.dp,
    card = preset.cardRadiusDp.dp,
    artworkMedium = preset.artworkMediumDp.dp,
    artworkLarge = preset.artworkLargeDp.dp,
    sheet = preset.sheetRadiusDp.dp,
)

val DefaultMagicThemeTokens: MagicThemeTokens by lazy {
    val preset = GeometryPreset.STANDARD_M3
    val radii = radiiForPreset(preset)
    MagicThemeTokens(
        theme = AppTheme.Material3Static(),
        isDark = true,
        pureAmoled = false,
        usesDynamicColor = false,
        colorFromArtwork = true,
        useExpressiveM3 = true,
        radii = radii,
        semantic = buildSemanticColorsForM3(
            colorScheme = VioletDarkColorScheme,
            isDark = true,
            pureAmoled = false,
        ),
        backgroundColor = buildFlatBackgroundColor(
            colorScheme = VioletDarkColorScheme,
            isDark = true,
            pureAmoled = false,
        ),
        shapes = buildShapesForPreset(preset),
    )
}

val LocalMagicTokens = compositionLocalOf { DefaultMagicThemeTokens }
