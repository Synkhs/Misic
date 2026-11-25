package com.example.designsystem

import androidx.compose.material3.ColorScheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Immutable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import com.materialkolor.contrast.Contrast
import com.materialkolor.dynamiccolor.ColorSpec
import com.materialkolor.dynamiccolor.MaterialDynamicColors
import com.materialkolor.hct.Hct
import com.materialkolor.scheme.DynamicScheme
import com.materialkolor.scheme.SchemeContent
import com.materialkolor.scheme.SchemeExpressive
import com.materialkolor.scheme.SchemeFidelity
import com.materialkolor.scheme.SchemeFruitSalad
import com.materialkolor.scheme.SchemeMonochrome
import com.materialkolor.scheme.SchemeNeutral
import com.materialkolor.scheme.SchemeRainbow
import com.materialkolor.scheme.SchemeTonalSpot
import com.materialkolor.scheme.SchemeVibrant
import kotlin.math.max
import kotlin.math.min
import kotlin.math.pow

enum class McuSchemeVariant(
    val label: String,
    val subtitle: String,
) {
    TONAL_SPOT("Tonal Spot", "Equilibrado Material 3 clásico"),
    VIBRANT("Vibrant", "Acentos vivos y saturados"),
    EXPRESSIVE("Expressive", "Contraste armónico expresivo"),
    FIDELITY("Fidelity", "Fiel al color semilla exacto"),
    CONTENT("Content", "Derivado de carátula o contenido"),
    NEUTRAL("Neutral", "Superficies limpias de baja saturación"),
    MONOCHROME("Monochrome", "Escala tonal monocromática"),
    RAINBOW("Rainbow", "Acentos secundarios cromáticos"),
    FRUIT_SALAD("Fruit Salad", "Combinación lúdica multicolor"),
}

enum class ContrastPreset(
    val label: String,
    val level: Double,
    val minAaTextRatio: Double,
) {
    STANDARD("Estándar", 0.0, 4.5),
    MEDIUM("Medio", 0.5, 5.5),
    HIGH("Alto", 1.0, 7.0),
}

@Immutable
data class ColorRoleOverrides(
    val primaryArgb: Long? = null,
    val secondaryArgb: Long? = null,
    val tertiaryArgb: Long? = null,
    val surfaceArgb: Long? = null,
)

@Immutable
data class SavedColorProfile(
    val id: String,
    val name: String,
    val seedColorArgb: Long,
    val variant: McuSchemeVariant = McuSchemeVariant.TONAL_SPOT,
    val themeMode: ThemeMode = ThemeMode.DARK,
    val surfaceToneStyle: SurfaceToneStyle = SurfaceToneStyle.TONAL,
    val contrastPreset: ContrastPreset = ContrastPreset.STANDARD,
    val pureAmoled: Boolean = false,
    val overrides: ColorRoleOverrides = ColorRoleOverrides(),
)

/**
 * Full Material Color Utilities engine generating AA/AAA compliant Material 3 schemes
 * with HCT math, all 9 dynamic scheme variants, contrast curves, and optional role overrides.
 */
object MaterialColorUtilitiesEngine {

    private val mdc = MaterialDynamicColors()

    fun seedFromHueSaturation(hueDegrees: Float, saturationFraction: Float, lightnessTone: Double = 55.0): Long {
        val h = hueDegrees.toDouble().coerceIn(0.0, 360.0)
        val c = (saturationFraction.coerceIn(0f, 1f) * 84.0).coerceIn(4.0, 84.0)
        val argb = Hct.from(h, c, lightnessTone).toInt()
        return (argb.toLong() and 0xFFFFFFFFL) or 0xFF000000L
    }

    fun extractHueAndSaturation(seedArgb: Long): Pair<Float, Float> {
        val hct = Hct.fromInt(seedArgb.toInt())
        val hue = hct.hue.toFloat().coerceIn(0f, 360f)
        val sat = (hct.chroma / 84.0).toFloat().coerceIn(0f, 1f)
        return hue to sat
    }

    fun generateColorScheme(
        seedColorArgb: Long,
        variant: McuSchemeVariant,
        isDark: Boolean,
        contrastPreset: ContrastPreset,
        pureAmoled: Boolean = false,
        overrides: ColorRoleOverrides = ColorRoleOverrides(),
    ): ColorScheme {
        val hct = Hct.fromInt(seedColorArgb.toInt())
        val contrastLevel = contrastPreset.level
        val spec = ColorSpec.SpecVersion.SPEC_2025
        val platform = DynamicScheme.Platform.PHONE

        val scheme: DynamicScheme = when (variant) {
            McuSchemeVariant.TONAL_SPOT -> SchemeTonalSpot(hct, isDark, contrastLevel, spec, platform)
            McuSchemeVariant.VIBRANT -> SchemeVibrant(hct, isDark, contrastLevel, spec, platform)
            McuSchemeVariant.EXPRESSIVE -> SchemeExpressive(hct, isDark, contrastLevel, spec, platform)
            McuSchemeVariant.FIDELITY -> SchemeFidelity(hct, isDark, contrastLevel, spec, platform)
            McuSchemeVariant.CONTENT -> SchemeContent(hct, isDark, contrastLevel, spec, platform)
            McuSchemeVariant.NEUTRAL -> SchemeNeutral(hct, isDark, contrastLevel, spec, platform)
            McuSchemeVariant.MONOCHROME -> SchemeMonochrome(hct, isDark, contrastLevel, spec, platform)
            McuSchemeVariant.RAINBOW -> SchemeRainbow(hct, isDark, contrastLevel, spec, platform)
            McuSchemeVariant.FRUIT_SALAD -> SchemeFruitSalad(hct, isDark, contrastLevel, spec, platform)
        }

        fun c(dc: com.materialkolor.dynamiccolor.DynamicColor): Color = Color(scheme.getArgb(dc))

        var generated = if (isDark) {
            darkColorScheme(
                primary = c(mdc.primary()),
                onPrimary = c(mdc.onPrimary()),
                primaryContainer = c(mdc.primaryContainer()),
                onPrimaryContainer = c(mdc.onPrimaryContainer()),
                secondary = c(mdc.secondary()),
                onSecondary = c(mdc.onSecondary()),
                secondaryContainer = c(mdc.secondaryContainer()),
                onSecondaryContainer = c(mdc.onSecondaryContainer()),
                tertiary = c(mdc.tertiary()),
                onTertiary = c(mdc.onTertiary()),
                tertiaryContainer = c(mdc.tertiaryContainer()),
                onTertiaryContainer = c(mdc.onTertiaryContainer()),
                background = c(mdc.background()),
                onBackground = c(mdc.onBackground()),
                surface = c(mdc.surface()),
                onSurface = c(mdc.onSurface()),
                surfaceVariant = c(mdc.surfaceVariant()),
                onSurfaceVariant = c(mdc.onSurfaceVariant()),
                surfaceContainerLowest = c(mdc.surfaceContainerLowest()),
                surfaceContainerLow = c(mdc.surfaceContainerLow()),
                surfaceContainer = c(mdc.surfaceContainer()),
                surfaceContainerHigh = c(mdc.surfaceContainerHigh()),
                surfaceContainerHighest = c(mdc.surfaceContainerHighest()),
                inverseSurface = c(mdc.inverseSurface()),
                inverseOnSurface = c(mdc.inverseOnSurface()),
                inversePrimary = c(mdc.inversePrimary()),
                outline = c(mdc.outline()),
                outlineVariant = c(mdc.outlineVariant()),
                error = c(mdc.error()),
                onError = c(mdc.onError()),
                errorContainer = c(mdc.errorContainer()),
                onErrorContainer = c(mdc.onErrorContainer()),
            )
        } else {
            lightColorScheme(
                primary = c(mdc.primary()),
                onPrimary = c(mdc.onPrimary()),
                primaryContainer = c(mdc.primaryContainer()),
                onPrimaryContainer = c(mdc.onPrimaryContainer()),
                secondary = c(mdc.secondary()),
                onSecondary = c(mdc.onSecondary()),
                secondaryContainer = c(mdc.secondaryContainer()),
                onSecondaryContainer = c(mdc.onSecondaryContainer()),
                tertiary = c(mdc.tertiary()),
                onTertiary = c(mdc.onTertiary()),
                tertiaryContainer = c(mdc.tertiaryContainer()),
                onTertiaryContainer = c(mdc.onTertiaryContainer()),
                background = c(mdc.background()),
                onBackground = c(mdc.onBackground()),
                surface = c(mdc.surface()),
                onSurface = c(mdc.onSurface()),
                surfaceVariant = c(mdc.surfaceVariant()),
                onSurfaceVariant = c(mdc.onSurfaceVariant()),
                surfaceContainerLowest = c(mdc.surfaceContainerLowest()),
                surfaceContainerLow = c(mdc.surfaceContainerLow()),
                surfaceContainer = c(mdc.surfaceContainer()),
                surfaceContainerHigh = c(mdc.surfaceContainerHigh()),
                surfaceContainerHighest = c(mdc.surfaceContainerHighest()),
                inverseSurface = c(mdc.inverseSurface()),
                inverseOnSurface = c(mdc.inverseOnSurface()),
                inversePrimary = c(mdc.inversePrimary()),
                outline = c(mdc.outline()),
                outlineVariant = c(mdc.outlineVariant()),
                error = c(mdc.error()),
                onError = c(mdc.onError()),
                errorContainer = c(mdc.errorContainer()),
                onErrorContainer = c(mdc.onErrorContainer()),
            )
        }

        // Apply optional per-role overrides with automatic AA contrast validation
        overrides.primaryArgb?.let { pArgb ->
            val p = Color(pArgb)
            val onP = bestInkFor(p, contrastPreset.minAaTextRatio)
            generated = generated.copy(primary = p, onPrimary = onP)
        }
        overrides.secondaryArgb?.let { sArgb ->
            val s = Color(sArgb)
            val onS = bestInkFor(s, contrastPreset.minAaTextRatio)
            generated = generated.copy(secondary = s, onSecondary = onS)
        }
        overrides.tertiaryArgb?.let { tArgb ->
            val t = Color(tArgb)
            val onT = bestInkFor(t, contrastPreset.minAaTextRatio)
            generated = generated.copy(tertiary = t, onTertiary = onT)
        }
        overrides.surfaceArgb?.let { surfArgb ->
            val surf = Color(surfArgb)
            val onSurf = bestInkFor(surf, contrastPreset.minAaTextRatio)
            val surfHct = Hct.fromInt(surfArgb.toInt())
            val darkSurface = surfHct.tone < 50.0
            val containerStep = if (darkSurface) 6.0 else -4.0
            fun shifted(delta: Double): Color {
                val nextTone = (surfHct.tone + delta).coerceIn(0.0, 100.0)
                return Color(Hct.from(surfHct.hue, surfHct.chroma, nextTone).toInt())
            }
            generated = generated.copy(
                background = surf,
                surface = surf,
                onBackground = onSurf,
                onSurface = onSurf,
                surfaceContainerLowest = shifted(-containerStep),
                surfaceContainerLow = shifted(containerStep * 0.5),
                surfaceContainer = shifted(containerStep),
                surfaceContainerHigh = shifted(containerStep * 1.6),
                surfaceContainerHighest = shifted(containerStep * 2.2),
            )
        }

        val withContrast = enforceSchemeContrastAA(generated, contrastPreset.minAaTextRatio)
        return if (isDark && pureAmoled) applyAmoledBlack(withContrast) else withContrast
    }

    fun enforceSchemeContrastAA(scheme: ColorScheme, minRatio: Double = 4.5): ColorScheme {
        return scheme.copy(
            onPrimary = ensureContrastColor(scheme.onPrimary, scheme.primary, minRatio),
            onPrimaryContainer = ensureContrastColor(scheme.onPrimaryContainer, scheme.primaryContainer, minRatio),
            onSecondary = ensureContrastColor(scheme.onSecondary, scheme.secondary, minRatio),
            onSecondaryContainer = ensureContrastColor(scheme.onSecondaryContainer, scheme.secondaryContainer, minRatio),
            onTertiary = ensureContrastColor(scheme.onTertiary, scheme.tertiary, minRatio),
            onTertiaryContainer = ensureContrastColor(scheme.onTertiaryContainer, scheme.tertiaryContainer, minRatio),
            onBackground = ensureContrastColor(scheme.onBackground, scheme.background, minRatio),
            onSurface = ensureContrastColor(scheme.onSurface, scheme.surface, minRatio),
            onSurfaceVariant = ensureContrastColor(scheme.onSurfaceVariant, scheme.surfaceContainer, 4.5),
            onError = ensureContrastColor(scheme.onError, scheme.error, minRatio),
            onErrorContainer = ensureContrastColor(scheme.onErrorContainer, scheme.errorContainer, minRatio),
        )
    }

    fun ensureContrastColor(fg: Color, bg: Color, minRatio: Double = 4.5): Color {
        val fgArgb = fg.toArgb()
        val bgArgb = bg.toArgb()
        if (contrastRatioArgb(fgArgb, bgArgb) >= minRatio) return fg
        val bgHct = Hct.fromInt(bgArgb)
        val fgHct = Hct.fromInt(fgArgb)
        val targetTone = if (bgHct.tone < 50.0) {
            Contrast.lighterUnsafe(bgHct.tone, minRatio)
        } else {
            Contrast.darkerUnsafe(bgHct.tone, minRatio)
        }
        val adjusted = Color(Hct.from(fgHct.hue, fgHct.chroma, targetTone).toInt())
        if (contrastRatioArgb(adjusted.toArgb(), bgArgb) >= minRatio) return adjusted
        return bestInkFor(bg, minRatio)
    }

    fun bestInkFor(bg: Color, minRatio: Double = 4.5): Color {
        val bgArgb = bg.toArgb()
        val darkInk = Color(0xFF101018)
        val lightInk = Color(0xFFF6F6FC)
        val rDark = contrastRatioArgb(darkInk.toArgb(), bgArgb)
        val rLight = contrastRatioArgb(lightInk.toArgb(), bgArgb)
        return when {
            rDark >= minRatio && rDark >= rLight -> darkInk
            rLight >= minRatio -> lightInk
            rDark >= rLight -> Color.Black
            else -> Color.White
        }
    }

    fun contrastRatioArgb(fgArgb: Int, bgArgb: Int): Double {
        val l1 = relativeLuminance(fgArgb)
        val l2 = relativeLuminance(bgArgb)
        return (max(l1, l2) + 0.05) / (min(l1, l2) + 0.05)
    }

    private fun relativeLuminance(argb: Int): Double {
        fun lin(c: Int): Double {
            val v = c / 255.0
            return if (v <= 0.03928) v / 12.92 else ((v + 0.055) / 1.055).pow(2.4)
        }
        val r = lin((argb ushr 16) and 0xFF)
        val g = lin((argb ushr 8) and 0xFF)
        val b = lin(argb and 0xFF)
        return 0.2126 * r + 0.7152 * g + 0.0722 * b
    }
}
