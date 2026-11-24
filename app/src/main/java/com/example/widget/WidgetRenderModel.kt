package com.example.widget

import com.example.data.CustomControlAction
import com.example.data.MusicServiceApp
import com.example.data.TrackHistoryEntry
import com.example.data.UnifiedMediaState
import com.example.data.WidgetConfig
import com.example.data.WidgetFormat
import com.example.data.WidgetStyle
import kotlin.math.max
import kotlin.math.min
import kotlin.math.pow

data class WidgetPaletteArgb(
    val bgStartArgb: Int,
    val bgEndArgb: Int,
    val borderArgb: Int,
    val titleArgb: Int,
    val subtitleArgb: Int,
    val badgeBgArgb: Int,
    val badgeTextArgb: Int,
    val playBtnBgArgb: Int,
    val playBtnIconArgb: Int,
    val accentArgb: Int,
)

data class WidgetDisplayCopy(
    val title: String,
    val subtitle: String,
    val badgeText: String,
    val isEmptyState: Boolean,
)

/**
 * Shared Material 3 rendering & WCAG AA contrast model used by both
 * real AppWidgets (RemoteViews) and the live Compose preview in WidgetStudioScreen.
 */
object WidgetRenderModel {

    fun resolveDisplayCopy(
        media: UnifiedMediaState,
        config: WidgetConfig,
        lastHistoryTrack: TrackHistoryEntry?,
        preferredPackage: String,
    ): WidgetDisplayCopy {
        val badgeSource = if (media.hasActiveMedia) {
            media.service.displayName.uppercase()
        } else {
            val preferred = MusicServiceApp.fromPackageName(preferredPackage)
            if (preferred != MusicServiceApp.OTHER) preferred.displayName.uppercase() else "AUDIO"
        }
        val badgeText = if (config.showCompactHeader) {
            "MAGICMUSIC • $badgeSource"
        } else {
            badgeSource
        }

        if (media.hasActiveMedia) {
            return WidgetDisplayCopy(
                title = media.title.ifBlank { media.service.displayName },
                subtitle = media.artist.ifBlank { media.service.displayName },
                badgeText = badgeText,
                isEmptyState = false,
            )
        }

        if (lastHistoryTrack != null) {
            return WidgetDisplayCopy(
                title = lastHistoryTrack.title,
                subtitle = "Última • ${lastHistoryTrack.artist.ifBlank { lastHistoryTrack.service.displayName }}",
                badgeText = badgeText,
                isEmptyState = true,
            )
        }

        val prefApp = MusicServiceApp.fromPackageName(preferredPackage)
        val prefName = if (prefApp != MusicServiceApp.OTHER) prefApp.displayName else "reproductor"
        return WidgetDisplayCopy(
            title = "Sin reproducción activa",
            subtitle = "Toca para abrir $prefName",
            badgeText = badgeText,
            isEmptyState = true,
        )
    }

    fun resolvePalette(
        style: WidgetStyle,
        glassOpacity: Float,
        dominantArtworkArgb: Int?,
        serviceAccentArgb: Int?,
        themePrimaryArgb: Int = 0xFFCDA9FF.toInt(),
        themeOnPrimaryArgb: Int = 0xFF2A1A4A.toInt(),
    ): WidgetPaletteArgb {
        val alphaFactor = glassOpacity.coerceIn(0.45f, 1.0f)
        val rawStart: Int
        val rawEnd: Int
        val rawBorder: Int
        val accentCandidate = themePrimaryArgb

        when (style) {
            WidgetStyle.TONAL -> {
                rawStart = applyAlpha(0xFF2B2630.toInt(), alphaFactor)
                rawEnd = applyAlpha(0xFF352F3C.toInt(), alphaFactor)
                rawBorder = applyAlpha(0xFF4A4256.toInt(), 0.72f)
            }
            WidgetStyle.SURFACE -> {
                rawStart = applyAlpha(0xFF16131A.toInt(), alphaFactor)
                rawEnd = applyAlpha(0xFF211C27.toInt(), alphaFactor)
                rawBorder = applyAlpha(0xFF3D3646.toInt(), 0.80f)
            }
            WidgetStyle.ARTWORK -> {
                val artTint = dominantArtworkArgb ?: serviceAccentArgb ?: 0xFF5D468A.toInt()
                rawStart = applyAlpha(blendArgb(0xFF1A1523.toInt(), artTint, 0.36f), alphaFactor)
                rawEnd = applyAlpha(blendArgb(0xFF2B2435.toInt(), artTint, 0.18f), alphaFactor)
                rawBorder = applyAlpha(ensureContrastOnBackground(artTint, 0xFF1A1523.toInt(), 3.2), 0.75f)
            }
            WidgetStyle.TRANSPARENT -> {
                val glassAlpha = (alphaFactor * 0.74f).coerceIn(0.40f, 0.85f)
                rawStart = applyAlpha(0xFF18141E.toInt(), glassAlpha)
                rawEnd = applyAlpha(0xFF25202E.toInt(), glassAlpha)
                rawBorder = applyAlpha(0xFFCDA9FF.toInt(), 0.38f)
            }
        }

        // Composite background over a dark home-screen baseline when computing WCAG AA contrast
        val effectiveBg = compositeOverDarkWallpaper(rawStart)
        val titleColor = ensureContrastOnBackground(0xFFF2EAFB.toInt(), effectiveBg, 4.5)
        val subtitleColor = ensureContrastOnBackground(0xFFD5CCE2.toInt(), effectiveBg, 4.5)
        val badgeBg = 0xFF4A3F66.toInt()
        val badgeText = ensureContrastOnBackground(0xFFEDE2FF.toInt(), badgeBg, 4.5)
        val playBg = 0xFFCDA9FF.toInt()
        val playIcon = ensureContrastOnBackground(themeOnPrimaryArgb, playBg, 4.5)
        val safeAccent = ensureContrastOnBackground(accentCandidate, effectiveBg, 4.5)

        return WidgetPaletteArgb(
            bgStartArgb = rawStart,
            bgEndArgb = rawEnd,
            borderArgb = rawBorder,
            titleArgb = titleColor,
            subtitleArgb = subtitleColor,
            badgeBgArgb = badgeBg,
            badgeTextArgb = badgeText,
            playBtnBgArgb = playBg,
            playBtnIconArgb = playIcon,
            accentArgb = safeAccent,
        )
    }

    fun resolveFormatForSizeDp(
        widthDp: Int,
        heightDp: Int,
        defaultFormat: WidgetFormat,
    ): WidgetFormat {
        if (widthDp <= 0 || heightDp <= 0) return defaultFormat
        return when {
            widthDp < 190 && heightDp < 110 -> WidgetFormat.MINI_2X1
            widthDp < 210 && heightDp >= 110 -> WidgetFormat.SQUARE_2X2
            widthDp >= 210 && heightDp < 105 -> WidgetFormat.CAPSULE_4X1
            widthDp >= 210 && heightDp >= 105 -> WidgetFormat.CARD_4X2
            else -> defaultFormat
        }
    }

    fun actionAccessibilityDescription(action: CustomControlAction): String = action.label

    /**
     * WCAG 2.1 contrast ratio between two opaque ARGB colors.
     */
    fun contrastRatio(fgArgb: Int, bgArgb: Int): Double {
        val l1 = relativeLuminance(fgArgb)
        val l2 = relativeLuminance(bgArgb)
        val lighter = max(l1, l2)
        val darker = min(l1, l2)
        return (lighter + 0.05) / (darker + 0.05)
    }

    fun ensureContrastOnBackground(
        preferredFgArgb: Int,
        bgArgb: Int,
        minRatio: Double = 4.5,
    ): Int {
        val opaqueFg = preferredFgArgb or 0xFF000000.toInt()
        val opaqueBg = bgArgb or 0xFF000000.toInt()
        if (contrastRatio(opaqueFg, opaqueBg) >= minRatio) return opaqueFg

        val targetExtreme = if (relativeLuminance(opaqueBg) < 0.35) {
            0xFFFFFFFF.toInt()
        } else {
            0xFF110D18.toInt()
        }
        for (step in 1..20) {
            val candidate = blendArgb(opaqueFg, targetExtreme, step / 20f)
            if (contrastRatio(candidate, opaqueBg) >= minRatio) {
                return candidate
            }
        }
        return targetExtreme
    }

    fun relativeLuminance(argb: Int): Double {
        fun linear(channel: Int): Double {
            val c = channel / 255.0
            return if (c <= 0.03928) c / 12.92 else ((c + 0.055) / 1.055).pow(2.4)
        }
        val r = linear((argb ushr 16) and 0xFF)
        val g = linear((argb ushr 8) and 0xFF)
        val b = linear(argb and 0xFF)
        return 0.2126 * r + 0.7152 * g + 0.0722 * b
    }

    fun applyAlpha(argb: Int, alphaFraction: Float): Int {
        val a = (alphaFraction.coerceIn(0f, 1f) * 255f).toInt().coerceIn(0, 255)
        return (a shl 24) or (argb and 0x00FFFFFF)
    }

    fun blendArgb(fromArgb: Int, toArgb: Int, ratio: Float): Int {
        val t = ratio.coerceIn(0f, 1f)
        val a = ((fromArgb ushr 24) and 0xFF) + (((toArgb ushr 24) and 0xFF) - ((fromArgb ushr 24) and 0xFF)) * t
        val r = ((fromArgb ushr 16) and 0xFF) + (((toArgb ushr 16) and 0xFF) - ((fromArgb ushr 16) and 0xFF)) * t
        val g = ((fromArgb ushr 8) and 0xFF) + (((toArgb ushr 8) and 0xFF) - ((fromArgb ushr 8) and 0xFF)) * t
        val b = (fromArgb and 0xFF) + ((toArgb and 0xFF) - (fromArgb and 0xFF)) * t
        return (a.toInt().coerceIn(0, 255) shl 24) or
            (r.toInt().coerceIn(0, 255) shl 16) or
            (g.toInt().coerceIn(0, 255) shl 8) or
            b.toInt().coerceIn(0, 255)
    }

    private fun compositeOverDarkWallpaper(argb: Int): Int {
        val a = ((argb ushr 24) and 0xFF) / 255f
        val bg = 0xFF141118.toInt()
        return blendArgb(bg, argb or 0xFF000000.toInt(), a)
    }
}
