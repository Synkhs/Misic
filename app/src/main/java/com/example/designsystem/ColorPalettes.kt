package com.example.designsystem

import androidx.compose.material3.ColorScheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.lerp

// Calibrated tokens matching the reference video: #121218 bg, #1C1C24/#23232C containers,
// #C0C1FF lavender primary, #3A47B5 indigo primaryContainer, tertiaryContainer format badges.
val VioletSeedColor = Color(DEFAULT_VIOLET_SEED_ARGB)
val ReferenceBgDark = Color(0xFF121218)
val ReferenceSurfaceLowDark = Color(0xFF171720)
val ReferenceSurfaceTonalDark = Color(0xFF1C1C24)
val ReferenceSurfaceHighDark = Color(0xFF23232C)
val ReferenceSurfaceHighestDark = Color(0xFF2B2B36)
val ReferenceSelectedPillDark = Color(0xFF3A47B5)
val ReferencePrimaryLilac = Color(0xFFC0C1FF)
val ReferenceOnPrimaryPlum = Color(0xFF1A216C)
val ReferenceTextPrimaryDark = Color(0xFFE9E9F4)
val ReferenceTextSecondaryDark = Color(0xFFC4C5D6)
val ReferenceDividerDark = Color(0xFF343442)

val SemanticSuccessLight = Color(0xFF0E8A5D)
val SemanticSuccessDark = Color(0xFF4ADE80)
val SemanticWarningLight = Color(0xFFB45309)
val SemanticWarningDark = Color(0xFFFBBF24)

val VioletDarkColorScheme: ColorScheme = darkColorScheme(
    primary = ReferencePrimaryLilac,
    onPrimary = ReferenceOnPrimaryPlum,
    primaryContainer = ReferenceSelectedPillDark,
    onPrimaryContainer = Color(0xFFE7E8FF),
    secondary = Color(0xFFC5C4DD),
    onSecondary = Color(0xFF2E2F42),
    secondaryContainer = Color(0xFF32365E),
    onSecondaryContainer = Color(0xFFE2E2F9),
    tertiary = Color(0xFFE8B9D5),
    onTertiary = Color(0xFF46263C),
    tertiaryContainer = Color(0xFF4F384C),
    onTertiaryContainer = Color(0xFFFFD8EE),
    background = ReferenceBgDark,
    onBackground = ReferenceTextPrimaryDark,
    surface = ReferenceBgDark,
    onSurface = ReferenceTextPrimaryDark,
    surfaceVariant = ReferenceSurfaceTonalDark,
    onSurfaceVariant = ReferenceTextSecondaryDark,
    surfaceContainerLowest = Color(0xFF0D0D12),
    surfaceContainerLow = ReferenceSurfaceLowDark,
    surfaceContainer = ReferenceSurfaceTonalDark,
    surfaceContainerHigh = ReferenceSurfaceHighDark,
    surfaceContainerHighest = ReferenceSurfaceHighestDark,
    outline = Color(0xFF8F90A2),
    outlineVariant = ReferenceDividerDark,
    error = Color(0xFFFFB4AB),
    onError = Color(0xFF690005),
    errorContainer = Color(0xFF93000A),
    onErrorContainer = Color(0xFFFFDAD6),
)

val VioletLightColorScheme: ColorScheme = lightColorScheme(
    primary = Color(0xFF3A47B5),
    onPrimary = Color.White,
    primaryContainer = Color(0xFFE0E2FF),
    onPrimaryContainer = Color(0xFF0F1866),
    secondary = Color(0xFF5B5D72),
    onSecondary = Color.White,
    secondaryContainer = Color(0xFFE0E0F9),
    onSecondaryContainer = Color(0xFF181A2C),
    tertiary = Color(0xFF77536D),
    onTertiary = Color.White,
    tertiaryContainer = Color(0xFFFFD8EE),
    onTertiaryContainer = Color(0xFF2D1227),
    background = Color(0xFFFAF9FF),
    onBackground = Color(0xFF191922),
    surface = Color(0xFFFAF9FF),
    onSurface = Color(0xFF191922),
    surfaceVariant = Color(0xFFE4E4EE),
    onSurfaceVariant = Color(0xFF464654),
    surfaceContainerLowest = Color(0xFFFFFFFF),
    surfaceContainerLow = Color(0xFFF3F3FA),
    surfaceContainer = Color(0xFFECECF5),
    surfaceContainerHigh = Color(0xFFE5E5F0),
    surfaceContainerHighest = Color(0xFFDFDFEA),
    outline = Color(0xFF767686),
    outlineVariant = Color(0xFFC7C7D4),
    error = Color(0xFFBA1A1A),
    onError = Color.White,
    errorContainer = Color(0xFFFFDAD6),
    onErrorContainer = Color(0xFF410002),
)

fun applyAmoledBlack(scheme: ColorScheme): ColorScheme = scheme.copy(
    background = Color.Black,
    surface = Color.Black,
    surfaceContainerLowest = Color.Black,
    surfaceContainerLow = Color(0xFF0A0A10),
    surfaceContainer = Color(0xFF12121A),
    surfaceContainerHigh = Color(0xFF1A1A24),
    surfaceContainerHighest = Color(0xFF232330),
    surfaceVariant = Color(0xFF15151E),
    outlineVariant = Color(0xFF2B2B38),
)

fun applyFlatDarkSurface(scheme: ColorScheme): ColorScheme = scheme.copy(
    surfaceContainerLow = lerp(scheme.background, scheme.surfaceContainerLow, 0.7f),
    surfaceContainer = lerp(scheme.background, scheme.surfaceContainer, 0.65f),
    surfaceContainerHigh = lerp(scheme.background, scheme.surfaceContainerHigh, 0.65f),
)

fun generateTonalColorScheme(
    seedColorArgb: Long,
    isDark: Boolean,
    surfaceToneStyle: SurfaceToneStyle,
    pureAmoled: Boolean,
    schemeVariant: McuSchemeVariant = McuSchemeVariant.TONAL_SPOT,
    contrastPreset: ContrastPreset = ContrastPreset.STANDARD,
    roleOverrides: ColorRoleOverrides = ColorRoleOverrides(),
): ColorScheme {
    val hasOverrides = roleOverrides.primaryArgb != null ||
        roleOverrides.secondaryArgb != null ||
        roleOverrides.tertiaryArgb != null ||
        roleOverrides.surfaceArgb != null

    val base = if (
        seedColorArgb == DEFAULT_VIOLET_SEED_ARGB &&
        schemeVariant == McuSchemeVariant.TONAL_SPOT &&
        contrastPreset == ContrastPreset.STANDARD &&
        !hasOverrides
    ) {
        val calibrated = if (isDark) VioletDarkColorScheme else VioletLightColorScheme
        if (isDark && pureAmoled) applyAmoledBlack(calibrated) else calibrated
    } else {
        MaterialColorUtilitiesEngine.generateColorScheme(
            seedColorArgb = seedColorArgb,
            variant = schemeVariant,
            isDark = isDark,
            contrastPreset = contrastPreset,
            pureAmoled = pureAmoled,
            overrides = roleOverrides,
        )
    }

    return if (isDark && surfaceToneStyle == SurfaceToneStyle.DARK && !pureAmoled) {
        applyFlatDarkSurface(base)
    } else {
        base
    }
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
    cardSurface = if (pureAmoled && isDark) Color(0xFF101018) else colorScheme.surfaceContainer,
    elevatedCardSurface = if (pureAmoled && isDark) Color(0xFF1A1A24) else colorScheme.surfaceContainerHigh,
    selectedContainer = colorScheme.primaryContainer,
    onSelectedContainer = colorScheme.onPrimaryContainer,
    divider = colorScheme.outlineVariant,
    mutedText = colorScheme.onSurfaceVariant,
    miniPlayerSurface = if (pureAmoled && isDark) Color(0xFF161620) else colorScheme.surfaceContainerHigh,
    navBarContainer = if (pureAmoled && isDark) Color(0xFF0A0A10) else colorScheme.surfaceContainerLow,
    navPillIndicator = colorScheme.primaryContainer,
    playButtonSurface = colorScheme.primary,
    onPlayButtonSurface = colorScheme.onPrimary,
)

fun buildFlatBackgroundColor(
    colorScheme: ColorScheme,
    isDark: Boolean,
    pureAmoled: Boolean,
): Color = if (isDark && pureAmoled) Color.Black else colorScheme.background

fun buildArtworkTonalSurface(
    artworkColors: ArtworkPaletteColors,
    baseBackground: Color,
    isDark: Boolean,
): Color = if (isDark) {
    lerp(baseBackground, artworkColors.darkTintSurface, 0.45f)
} else {
    lerp(baseBackground, artworkColors.dominant, 0.08f)
}
