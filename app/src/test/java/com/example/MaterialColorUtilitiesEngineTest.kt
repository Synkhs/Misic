package com.example

import androidx.compose.ui.graphics.toArgb
import com.example.designsystem.ColorRoleOverrides
import com.example.designsystem.ContrastPreset
import com.example.designsystem.DEFAULT_VIOLET_SEED_ARGB
import com.example.designsystem.MaterialColorUtilitiesEngine
import com.example.designsystem.McuSchemeVariant
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class MaterialColorUtilitiesEngineTest {

    @Test
    fun allVariantsAndContrastPresets_satisfyMinimumContrastRatios() {
        val seeds = listOf(
            DEFAULT_VIOLET_SEED_ARGB,
            0xFF006D40L,
            0xFFB3261EL,
            0xFFF59E0BL,
        )
        seeds.forEach { seed ->
            McuSchemeVariant.entries.forEach { variant ->
                ContrastPreset.entries.forEach { contrast ->
                    listOf(true, false).forEach { isDark ->
                        val scheme = MaterialColorUtilitiesEngine.generateColorScheme(
                            seedColorArgb = seed,
                            variant = variant,
                            isDark = isDark,
                            contrastPreset = contrast,
                        )
                        val primaryRatio = MaterialColorUtilitiesEngine.contrastRatioArgb(
                            scheme.onPrimary.toArgb(),
                            scheme.primary.toArgb(),
                        )
                        val surfaceRatio = MaterialColorUtilitiesEngine.contrastRatioArgb(
                            scheme.onSurface.toArgb(),
                            scheme.surface.toArgb(),
                        )
                        assertTrue(
                            "Primary contrast too low ($primaryRatio) for $variant/$contrast dark=$isDark",
                            primaryRatio >= 4.5,
                        )
                        assertTrue(
                            "Surface contrast too low ($surfaceRatio) for $variant/$contrast dark=$isDark",
                            surfaceRatio >= 4.5,
                        )
                    }
                }
            }
        }
    }

    @Test
    fun hctHueSaturationRoundTripAndRoleOverrides_applyAndAutoContrast() {
        val seed = MaterialColorUtilitiesEngine.seedFromHueSaturation(hueDegrees = 215f, saturationFraction = 0.72f)
        val (hue, sat) = MaterialColorUtilitiesEngine.extractHueAndSaturation(seed)
        assertTrue(hue in 190f..240f)
        assertTrue(sat > 0.3f)

        val overridden = MaterialColorUtilitiesEngine.generateColorScheme(
            seedColorArgb = seed,
            variant = McuSchemeVariant.EXPRESSIVE,
            isDark = true,
            contrastPreset = ContrastPreset.MEDIUM,
            overrides = ColorRoleOverrides(primaryArgb = 0xFFFFEB3BL),
        )
        assertNotEquals(overridden.primary, overridden.onPrimary)
        val ratio = MaterialColorUtilitiesEngine.contrastRatioArgb(
            overridden.onPrimary.toArgb(),
            overridden.primary.toArgb(),
        )
        assertTrue("Expected overridden ink contrast >= 4.5, got $ratio", ratio >= 4.5)
    }
}
