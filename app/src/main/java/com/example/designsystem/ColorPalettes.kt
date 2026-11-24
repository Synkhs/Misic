package com.example.designsystem

import androidx.compose.material3.ColorScheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.graphics.luminance

// Calibrated tokens matching the reference dark violet/lilac Material 3 system
val VioletSeedColor = Color(DEFAULT_VIOLET_SEED_ARGB)
val ReferenceBgDark = Color(0xFF16131A)
val ReferenceSurfaceLowDark = Color(0xFF211C27)
val ReferenceSurfaceTonalDark = Color(0xFF2B2630)
val ReferenceSurfaceHighDark = Color(0xFF352F3C)
val ReferenceSurfaceHighestDark = Color(0xFF403949)
val ReferenceSelectedPillDark = Color(0xFF4A3F66)
val ReferencePrimaryLilac = Color(0xFFCDA9FF)
val ReferenceOnPrimaryPlum = Color(0xFF2A1A4A)
val ReferenceTextPrimaryDark = Color(0xFFF2EAFB)
val ReferenceTextSecondaryDark = Color(0xFFCAC0D6)
val ReferenceDividerDark = Color(0xFF3D3646)

val SemanticSuccessLight = Color(0xFF0E8A5D)
val SemanticSuccessDark = Color(0xFF4ADE80)
val SemanticWarningLight = Color(0xFFB45309)
val SemanticWarningDark = Color(0xFFFBBF24)

val VioletDarkColorScheme: ColorScheme = darkColorScheme(
    primary = ReferencePrimaryLilac,
    onPrimary = ReferenceOnPrimaryPlum,
    primaryContainer = ReferenceSelectedPillDark,
    onPrimaryContainer = ReferenceTextPrimaryDark,
    secondary = Color(0xFFD5C1ED),
    onSecondary = Color(0xFF38294E),
    secondaryContainer = ReferenceSelectedPillDark,
    onSecondaryContainer = ReferenceTextPrimaryDark,
    tertiary = Color(0xFFF3B8CE),
    onTertiary = Color(0xFF4B2537),
    tertiaryContainer = Color(0xFF643B4E),
    onTertiaryContainer = Color(0xFFFFD8E6),
    background = ReferenceBgDark,
    onBackground = ReferenceTextPrimaryDark,
    surface = ReferenceBgDark,
    onSurface = ReferenceTextPrimaryDark,
    surfaceVariant = ReferenceSurfaceTonalDark,
    onSurfaceVariant = ReferenceTextSecondaryDark,
    surfaceContainerLowest = Color(0xFF100E14),
    surfaceContainerLow = ReferenceSurfaceLowDark,
    surfaceContainer = ReferenceSurfaceTonalDark,
    surfaceContainerHigh = ReferenceSurfaceHighDark,
    surfaceContainerHighest = ReferenceSurfaceHighestDark,
    outline = Color(0xFF938A9E),
    outlineVariant = ReferenceDividerDark,
    error = Color(0xFFFFB4AB),
    onError = Color(0xFF690005),
    errorContainer = Color(0xFF93000A),
    onErrorContainer = Color(0xFFFFDAD6),
)

val VioletLightColorScheme: ColorScheme = lightColorScheme(
    primary = Color(0xFF6A45B5),
    onPrimary = Color.White,
    primaryContainer = Color(0xFFEADCFF),
    onPrimaryContainer = Color(0xFF24095A),
    secondary = Color(0xFF625677),
    onSecondary = Color.White,
    secondaryContainer = Color(0xFFE9DEF8),
    onSecondaryContainer = Color(0xFF1E1630),
    tertiary = Color(0xFF834C66),
    onTertiary = Color.White,
    tertiaryContainer = Color(0xFFFFD8E7),
    onTertiaryContainer = Color(0xFF340B21),
    background = Color(0xFFFCF8FF),
    onBackground = Color(0xFF1D1A22),
    surface = Color(0xFFFCF8FF),
    onSurface = Color(0xFF1D1A22),
    surfaceVariant = Color(0xFFE9E2F1),
    onSurfaceVariant = Color(0xFF4A4453),
    surfaceContainerLowest = Color(0xFFFFFFFF),
    surfaceContainerLow = Color(0xFFF6F0FA),
    surfaceContainer = Color(0xFFEFE8F5),
    surfaceContainerHigh = Color(0xFFE8E0EF),
    surfaceContainerHighest = Color(0xFFE0D7E9),
    outline = Color(0xFF7B7484),
    outlineVariant = Color(0xFFCCC4D4),
    error = Color(0xFFBA1A1A),
    onError = Color.White,
    errorContainer = Color(0xFFFFDAD6),
    onErrorContainer = Color(0xFF410002),
)

fun applyAmoledBlack(scheme: ColorScheme): ColorScheme = scheme.copy(
    background = Color.Black,
    surface = Color.Black,
    surfaceContainerLowest = Color.Black,
    surfaceContainerLow = Color(0xFF0A090D),
    surfaceContainer = Color(0xFF121017),
    surfaceContainerHigh = Color(0xFF1B1822),
    surfaceContainerHighest = Color(0xFF24202D),
    surfaceVariant = Color(0xFF15121B),
    outlineVariant = Color(0xFF2A2533),
)

fun applyFlatDarkSurface(scheme: ColorScheme): ColorScheme = scheme.copy(
    surfaceContainerLow = lerp(scheme.background, scheme.surfaceContainerLow, 0.6f),
    surfaceContainer = lerp(scheme.background, scheme.surfaceContainer, 0.55f),
    surfaceContainerHigh = lerp(scheme.background, scheme.surfaceContainerHigh, 0.55f),
)

fun generateTonalColorScheme(
    seedColorArgb: Long,
    isDark: Boolean,
    surfaceToneStyle: SurfaceToneStyle,
    pureAmoled: Boolean,
): ColorScheme {
    val base = if (seedColorArgb == DEFAULT_VIOLET_SEED_ARGB) {
        if (isDark) VioletDarkColorScheme else VioletLightColorScheme
    } else {
        val seed = Color(seedColorArgb)
        if (isDark) {
            val darkPrimary = lerp(seed, Color.White, 0.28f)
            val onDarkPrimary = lerp(seed, Color.Black, 0.78f)
            val bg = lerp(Color(0xFF121216), seed, 0.06f)
            val low = lerp(Color(0xFF1B1A20), seed, 0.08f)
            val tonal = lerp(Color(0xFF24222B), seed, 0.10f)
            val high = lerp(Color(0xFF2E2B36), seed, 0.12f)
            val highest = lerp(Color(0xFF383442), seed, 0.14f)
            val pill = lerp(Color(0xFF322D40), seed, 0.30f)
            val onSurface = lerp(Color(0xFFF3EFF8), seed, 0.08f)
            val onVariant = lerp(Color(0xFFC8C2D2), seed, 0.12f)
            val divider = lerp(Color(0xFF383440), seed, 0.12f)

            darkColorScheme(
                primary = darkPrimary,
                onPrimary = onDarkPrimary,
                primaryContainer = pill,
                onPrimaryContainer = onSurface,
                secondary = lerp(darkPrimary, Color(0xFFCAC0D6), 0.35f),
                onSecondary = onDarkPrimary,
                secondaryContainer = pill,
                onSecondaryContainer = onSurface,
                tertiary = lerp(seed, Color(0xFFFFB8CE), 0.45f),
                onTertiary = Color(0xFF3E1828),
                background = bg,
                onBackground = onSurface,
                surface = bg,
                onSurface = onSurface,
                surfaceVariant = tonal,
                onSurfaceVariant = onVariant,
                surfaceContainerLowest = lerp(Color(0xFF0C0C10), seed, 0.04f),
                surfaceContainerLow = low,
                surfaceContainer = tonal,
                surfaceContainerHigh = high,
                surfaceContainerHighest = highest,
                outline = onVariant.copy(alpha = 0.72f),
                outlineVariant = divider,
            )
        } else {
            val lightPrimary = lerp(seed, Color(0xFF1A1033), 0.44f)
            val bg = lerp(Color(0xFFFCFAFF), seed, 0.04f)
            val low = lerp(Color(0xFFF6F3FA), seed, 0.06f)
            val tonal = lerp(Color(0xFFEFEAF6), seed, 0.09f)
            val high = lerp(Color(0xFFE7E1F0), seed, 0.11f)
            val highest = lerp(Color(0xFFDFD7EA), seed, 0.13f)
            val container = lerp(Color.White, seed, 0.25f)
            val onContainer = lerp(seed, Color.Black, 0.76f)
            val onSurface = Color(0xFF1B1920)
            val onVariant = Color(0xFF494550)
            val divider = lerp(Color(0xFFCCC6D4), seed, 0.15f)

            lightColorScheme(
                primary = lightPrimary,
                onPrimary = if (lightPrimary.luminance() > 0.45f) Color.Black else Color.White,
                primaryContainer = container,
                onPrimaryContainer = onContainer,
                secondary = lerp(lightPrimary, Color(0xFF5D576B), 0.4f),
                onSecondary = Color.White,
                secondaryContainer = container,
                onSecondaryContainer = onContainer,
                background = bg,
                onBackground = onSurface,
                surface = bg,
                onSurface = onSurface,
                surfaceVariant = tonal,
                onSurfaceVariant = onVariant,
                surfaceContainerLowest = Color.White,
                surfaceContainerLow = low,
                surfaceContainer = tonal,
                surfaceContainerHigh = high,
                surfaceContainerHighest = highest,
                outline = Color(0xFF7A7582),
                outlineVariant = divider,
            )
        }
    }

    val adjusted = if (isDark && surfaceToneStyle == SurfaceToneStyle.DARK) {
        applyFlatDarkSurface(base)
    } else {
        base
    }
    return if (isDark && pureAmoled) applyAmoledBlack(adjusted) else adjusted
}

fun buildSemanticColorsForM3(
    colorScheme: ColorScheme,
    isDark: Boolean,
    pureAmoled: Boolean,
): SemanticColors = SemanticColors(
    success = if (isDark) SemanticSuccessDark else SemanticSuccessLight,
    onSuccess = if (isDark) Color(0xFF042613) else Color.White,
    warning = if (isDark) SemanticWarningDark else SemanticWarningLight,
    onWarning = if (isDark) Color(0xFF2B1600) else Color.White,
    info = colorScheme.primary,
    cardSurface = if (pureAmoled && isDark) Color(0xFF0E0C12) else colorScheme.surfaceContainer,
    elevatedCardSurface = if (pureAmoled && isDark) Color(0xFF181520) else colorScheme.surfaceContainerHigh,
    selectedContainer = colorScheme.secondaryContainer,
    onSelectedContainer = colorScheme.onSecondaryContainer,
    divider = colorScheme.outlineVariant,
    mutedText = colorScheme.onSurfaceVariant,
    miniPlayerSurface = if (pureAmoled && isDark) Color(0xFF14111A) else colorScheme.surfaceContainerHigh,
    navBarContainer = if (pureAmoled && isDark) Color(0xFF09080C) else colorScheme.surfaceContainerLow,
    navPillIndicator = colorScheme.secondaryContainer,
    playButtonSurface = if (isDark) ReferenceTextPrimaryDark else colorScheme.primary,
    onPlayButtonSurface = if (isDark) ReferenceOnPrimaryPlum else colorScheme.onPrimary,
)

fun buildBackgroundBrush(
    colorScheme: ColorScheme,
    isDark: Boolean,
    pureAmoled: Boolean,
): Brush {
    if (isDark && pureAmoled) {
        return Brush.verticalGradient(listOf(Color.Black, Color.Black))
    }
    return if (isDark) {
        Brush.verticalGradient(
            listOf(
                lerp(colorScheme.background, colorScheme.primaryContainer, 0.22f),
                colorScheme.background,
                colorScheme.background,
            ),
        )
    } else {
        Brush.verticalGradient(
            listOf(
                colorScheme.background,
                colorScheme.surfaceContainerLow,
            ),
        )
    }
}

fun buildArtworkTintedBrush(
    artworkColors: ArtworkPaletteColors,
    baseBackground: Color,
    isDark: Boolean,
): Brush {
    val topTint = if (isDark) {
        lerp(artworkColors.darkTintSurface, artworkColors.dominant, 0.42f)
    } else {
        lerp(baseBackground, artworkColors.dominant, 0.22f)
    }
    val midTint = if (isDark) {
        lerp(baseBackground, artworkColors.darkTintSurface, 0.55f)
    } else {
        lerp(baseBackground, artworkColors.dominant, 0.10f)
    }
    return Brush.verticalGradient(
        listOf(
            topTint,
            midTint,
            baseBackground,
        ),
    )
}
