package com.example

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import com.example.data.PreferencesRepository
import com.example.data.WidgetStyle
import com.example.designsystem.AppTheme
import com.example.designsystem.ArtworkPaletteExtractor
import com.example.designsystem.DEFAULT_VIOLET_SEED_ARGB
import com.example.designsystem.DefaultSeedSwatches
import com.example.designsystem.ReferenceBgDark
import com.example.designsystem.ReferenceDividerDark
import com.example.designsystem.ReferenceOnPrimaryPlum
import com.example.designsystem.ReferencePrimaryLilac
import com.example.designsystem.ReferenceSelectedPillDark
import com.example.designsystem.ReferenceSurfaceHighDark
import com.example.designsystem.ReferenceSurfaceTonalDark
import com.example.designsystem.ReferenceTextPrimaryDark
import com.example.designsystem.ReferenceTextSecondaryDark
import com.example.designsystem.SurfaceToneStyle
import com.example.designsystem.ThemeMode
import com.example.designsystem.ThemePreferences
import com.example.designsystem.VioletDarkColorScheme
import com.example.designsystem.VioletLightColorScheme
import com.example.designsystem.VioletSeedColor
import com.example.designsystem.applyAmoledBlack
import com.example.designsystem.buildArtworkTonalSurface
import com.example.designsystem.buildFlatBackgroundColor
import com.example.designsystem.buildSemanticColorsForM3
import com.example.designsystem.generateTonalColorScheme
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class Phase2ThemeUnitTest {

    @Test
    fun themePreferences_defaultToDarkTonalLavenderExpressive() {
        val defaults = ThemePreferences()
        assertFalse(defaults.useDynamicColor)
        assertEquals(ThemeMode.DARK, defaults.themeMode)
        assertEquals(SurfaceToneStyle.TONAL, defaults.surfaceToneStyle)
        assertEquals(DEFAULT_VIOLET_SEED_ARGB, defaults.seedColorArgb)
        assertFalse(defaults.pureAmoledBlack)
        assertTrue(defaults.colorFromArtwork)
        assertFalse(defaults.reduceAnimations)
        assertTrue(defaults.useExpressiveM3)
        assertEquals(AppTheme.Material3Static(DEFAULT_VIOLET_SEED_ARGB), defaults.toAppTheme())
    }

    @Test
    fun violetDarkScheme_matchesCalibratedReferenceHexTokens() {
        assertEquals(ReferenceBgDark, VioletDarkColorScheme.background)
        assertEquals(ReferenceSurfaceTonalDark, VioletDarkColorScheme.surfaceContainer)
        assertEquals(ReferenceSurfaceHighDark, VioletDarkColorScheme.surfaceContainerHigh)
        assertEquals(ReferenceSelectedPillDark, VioletDarkColorScheme.primaryContainer)
        assertEquals(ReferencePrimaryLilac, VioletDarkColorScheme.primary)
        assertEquals(ReferenceOnPrimaryPlum, VioletDarkColorScheme.onPrimary)
        assertEquals(ReferenceTextPrimaryDark, VioletDarkColorScheme.onBackground)
        assertEquals(ReferenceTextSecondaryDark, VioletDarkColorScheme.onSurfaceVariant)
        assertEquals(ReferenceDividerDark, VioletDarkColorScheme.outlineVariant)
    }

    @Test
    fun resolveDarkTheme_respectsSystemDarkAndLightModes() {
        val systemPrefs = ThemePreferences(themeMode = ThemeMode.SYSTEM)
        assertFalse(systemPrefs.resolveDarkTheme(systemIsDark = false))
        assertTrue(systemPrefs.resolveDarkTheme(systemIsDark = true))

        val forcedLight = ThemePreferences(themeMode = ThemeMode.LIGHT)
        assertFalse(forcedLight.resolveDarkTheme(systemIsDark = true))

        val forcedDark = ThemePreferences(themeMode = ThemeMode.DARK)
        assertTrue(forcedDark.resolveDarkTheme(systemIsDark = false))
    }

    @Test
    fun generateTonalColorScheme_supportsAllSwatchesLightAndAmoled() {
        assertTrue(VioletLightColorScheme.background.luminance() > 0.7f)
        assertTrue(VioletDarkColorScheme.background.luminance() < 0.1f)

        DefaultSeedSwatches.forEach { swatch ->
            val darkScheme = generateTonalColorScheme(
                seedColorArgb = swatch.argb,
                isDark = true,
                surfaceToneStyle = SurfaceToneStyle.TONAL,
                pureAmoled = false,
            )
            val lightScheme = generateTonalColorScheme(
                seedColorArgb = swatch.argb,
                isDark = false,
                surfaceToneStyle = SurfaceToneStyle.LIGHT,
                pureAmoled = false,
            )
            assertNotEquals(darkScheme.background, lightScheme.background)
            assertNotEquals(Color.Unspecified, darkScheme.primary)
            assertNotEquals(Color.Unspecified, lightScheme.primary)
        }

        val amoled = applyAmoledBlack(VioletDarkColorScheme)
        assertEquals(Color.Black, amoled.background)
        assertEquals(Color.Black, amoled.surface)
        assertEquals(Color.Black, amoled.surfaceContainerLowest)
        assertEquals(VioletDarkColorScheme.primary, amoled.primary)
    }

    @Test
    fun semanticColorsAndFlatTonalSurfaces_buildWithoutGradients() {
        val lightSemantic = buildSemanticColorsForM3(
            colorScheme = VioletLightColorScheme,
            isDark = false,
            pureAmoled = false,
        )
        val darkAmoledSemantic = buildSemanticColorsForM3(
            colorScheme = applyAmoledBlack(VioletDarkColorScheme),
            isDark = true,
            pureAmoled = true,
        )
        assertNotEquals(lightSemantic.cardSurface, darkAmoledSemantic.cardSurface)
        assertEquals(Color(0xFF0A0A10), darkAmoledSemantic.navBarContainer)

        val artworkColors = ArtworkPaletteExtractor.deriveFromFallbackSeed(VioletSeedColor)
        val tintedSurface = buildArtworkTonalSurface(
            artworkColors = artworkColors,
            baseBackground = VioletDarkColorScheme.background,
            isDark = true,
        )
        val flatBg = buildFlatBackgroundColor(
            colorScheme = VioletDarkColorScheme,
            isDark = true,
            pureAmoled = false,
        )
        assertNotEquals(Color.Unspecified, tintedSurface)
        assertEquals(VioletDarkColorScheme.background, flatBg)
        assertEquals(Color.Black, buildFlatBackgroundColor(VioletDarkColorScheme, isDark = true, pureAmoled = true))
    }

    @Test
    fun legacyDataStorePreferences_migrateCleanlyToMaterial3() {
        assertEquals(WidgetStyle.SURFACE, PreferencesRepository.migrateWidgetStyle("CAPSULE_DARK"))
        assertEquals(WidgetStyle.TONAL, PreferencesRepository.migrateWidgetStyle("FROSTED_GLASS"))
        assertEquals(WidgetStyle.ARTWORK, PreferencesRepository.migrateWidgetStyle("PORCELAIN_LIGHT"))
        assertEquals(WidgetStyle.TRANSPARENT, PreferencesRepository.migrateWidgetStyle("DYNAMIC_NEON"))
        assertEquals(WidgetStyle.TONAL, PreferencesRepository.migrateWidgetStyle("UNKNOWN_STYLE"))

        assertEquals(
            ThemeMode.LIGHT,
            PreferencesRepository.migrateThemeMode(
                rawMode = "SYSTEM",
                legacyThemeId = "MAGIC_OS_10",
                legacyFinish = "PORCELANA_CLARA",
            ),
        )
        assertEquals(
            ThemeMode.DARK,
            PreferencesRepository.migrateThemeMode(
                rawMode = null,
                legacyThemeId = "MAGIC_OS_10",
                legacyFinish = "CAPSULE_VIVA",
            ),
        )
        assertTrue(
            PreferencesRepository.migrateUseDynamicColor(
                explicitUseDynamic = null,
                legacyThemeId = "MATERIAL_3_DYNAMIC",
            ),
        )
        assertFalse(
            PreferencesRepository.migrateUseDynamicColor(
                explicitUseDynamic = null,
                legacyThemeId = "MAGIC_OS_10",
            ),
        )
    }
}
