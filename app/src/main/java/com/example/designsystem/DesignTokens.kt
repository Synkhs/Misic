package com.example.designsystem

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Shapes
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

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
    val miniPlayer: Dp = 16.dp,
    val medium: Dp = 20.dp,
    val card: Dp = 24.dp,
    val artworkMedium: Dp = 24.dp,
    val artworkLarge: Dp = 28.dp,
    val sheet: Dp = 28.dp,
)

@Immutable
data class MagicElevations(
    val none: Dp = 0.dp,
    val card: Dp = 2.dp,
    val miniPlayer: Dp = 6.dp,
    val sheet: Dp = 12.dp,
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
    val spacing: MagicSpacing = MagicSpacing(),
    val radii: MagicRadii = MagicRadii(),
    val elevations: MagicElevations = MagicElevations(),
    val motion: MagicMotion = MagicMotion(),
    val semantic: SemanticColors,
    val backgroundBrush: Brush,
    val shapes: Shapes,
)

fun buildMagicShapes(radii: MagicRadii): Shapes = Shapes(
    extraSmall = RoundedCornerShape(radii.extraSmall),
    small = RoundedCornerShape(radii.listItem),
    medium = RoundedCornerShape(radii.medium),
    large = RoundedCornerShape(radii.card),
    extraLarge = RoundedCornerShape(radii.artworkLarge),
)

val DefaultMagicThemeTokens: MagicThemeTokens by lazy {
    val radii = MagicRadii()
    MagicThemeTokens(
        theme = AppTheme.Material3Static(),
        isDark = true,
        pureAmoled = false,
        usesDynamicColor = false,
        colorFromArtwork = true,
        radii = radii,
        semantic = buildSemanticColorsForM3(
            colorScheme = VioletDarkColorScheme,
            isDark = true,
            pureAmoled = false,
        ),
        backgroundBrush = buildBackgroundBrush(
            colorScheme = VioletDarkColorScheme,
            isDark = true,
            pureAmoled = false,
        ),
        shapes = buildMagicShapes(radii),
    )
}

val LocalMagicTokens = compositionLocalOf { DefaultMagicThemeTokens }
