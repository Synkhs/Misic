package com.example

import com.example.core.media.HealthCheckId
import com.example.core.media.HealthLight
import com.example.core.media.SleepTimerManager
import com.example.core.media.SleepTimerMode
import com.example.core.media.SleepTimerState
import com.example.core.media.SystemHealthCalculator
import com.example.data.CustomControlAction
import com.example.data.TrackHistoryEntry
import com.example.data.UnifiedMediaState
import com.example.data.WidgetConfig
import com.example.data.WidgetFormat
import com.example.data.WidgetStyle
import com.example.data.WidgetTemplate
import com.example.data.WidgetTemplateSerializer
import com.example.widget.WidgetRenderModel
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class Phase3And4UnitTest {

    @Test
    fun widgetRenderModel_enforcesWcagAaContrastAcrossAllStylesAndOpacities() {
        val styles = WidgetStyle.entries
        val opacities = listOf(0.45f, 0.75f, 0.94f, 1.0f)

        styles.forEach { style ->
            opacities.forEach { opacity ->
                val palette = WidgetRenderModel.resolvePalette(
                    style = style,
                    glassOpacity = opacity,
                    dominantArtworkArgb = 0xFFE9D5FF.toInt(), // bright artwork edge case
                    serviceAccentArgb = 0xFF1DB954.toInt(),
                )
                // Badge and Play button always use opaque high-contrast backgrounds
                val badgeContrast = WidgetRenderModel.contrastRatio(palette.badgeTextArgb, palette.badgeBgArgb)
                val playContrast = WidgetRenderModel.contrastRatio(palette.playBtnIconArgb, palette.playBtnBgArgb)
                assertTrue("Badge contrast failed for $style ($badgeContrast)", badgeContrast >= 4.5)
                assertTrue("Play button contrast failed for $style ($playContrast)", playContrast >= 4.5)

                // Composite title & subtitle on dark wallpaper reference
                val blendedBg = WidgetRenderModel.blendArgb(
                    0xFF141118.toInt(),
                    palette.bgStartArgb or 0xFF000000.toInt(),
                    ((palette.bgStartArgb ushr 24) and 0xFF) / 255f,
                )
                val titleContrast = WidgetRenderModel.contrastRatio(palette.titleArgb, blendedBg)
                val subtitleContrast = WidgetRenderModel.contrastRatio(palette.subtitleArgb, blendedBg)
                assertTrue("Title contrast failed for $style @ $opacity ($titleContrast)", titleContrast >= 4.5)
                assertTrue("Subtitle contrast failed for $style @ $opacity ($subtitleContrast)", subtitleContrast >= 4.5)
            }
        }
    }

    @Test
    fun widgetRenderModel_mapsSizesToResponsiveFormats() {
        assertEquals(
            WidgetFormat.MINI_2X1,
            WidgetRenderModel.resolveFormatForSizeDp(140, 70, WidgetFormat.CARD_4X2),
        )
        assertEquals(
            WidgetFormat.CAPSULE_4X1,
            WidgetRenderModel.resolveFormatForSizeDp(320, 72, WidgetFormat.CARD_4X2),
        )
        assertEquals(
            WidgetFormat.SQUARE_2X2,
            WidgetRenderModel.resolveFormatForSizeDp(170, 170, WidgetFormat.CARD_4X2),
        )
        assertEquals(
            WidgetFormat.CARD_4X2,
            WidgetRenderModel.resolveFormatForSizeDp(320, 160, WidgetFormat.CAPSULE_4X1),
        )
    }

    @Test
    fun widgetRenderModel_resolvesMeaningfulEmptyStateCopy() {
        val noHistoryCopy = WidgetRenderModel.resolveDisplayCopy(
            media = UnifiedMediaState(),
            config = WidgetConfig(),
            lastHistoryTrack = null,
            preferredPackage = "com.spotify.music",
        )
        assertTrue(noHistoryCopy.isEmptyState)
        assertEquals("Sin reproducción activa", noHistoryCopy.title)
        assertTrue(noHistoryCopy.subtitle.contains("Spotify"))

        val withHistoryCopy = WidgetRenderModel.resolveDisplayCopy(
            media = UnifiedMediaState(),
            config = WidgetConfig(),
            lastHistoryTrack = TrackHistoryEntry(
                id = "t1",
                title = "Marea Alta",
                artist = "Brisa de Mar",
                album = "Verano",
                packageName = "com.spotify.music",
                serviceId = "spotify",
                lastPlayedMs = 1000L,
            ),
            preferredPackage = "com.spotify.music",
        )
        assertTrue(withHistoryCopy.isEmptyState)
        assertEquals("Marea Alta", withHistoryCopy.title)
        assertTrue(withHistoryCopy.subtitle.startsWith("Última"))
    }

    @Test
    fun widgetTemplateSerializer_roundTripAndMalformedResilience() {
        val templates = listOf(
            WidgetTemplate(
                id = "custom_1",
                name = "Violeta \"Studio\"\n2x2",
                format = WidgetFormat.SQUARE_2X2,
                config = WidgetConfig(
                    style = WidgetStyle.ARTWORK,
                    cornerRadiusDp = 28,
                    glassOpacity = 0.82f,
                    showProgressBar = false,
                    showServiceBadge = true,
                    showCompactHeader = false,
                    leftCustomAction = CustomControlAction.LIKE_TRACK,
                    rightCustomAction = CustomControlAction.SHUFFLE,
                ),
            ),
            WidgetTemplate(
                id = "custom_2",
                name = "Mini Nocturno",
                format = WidgetFormat.MINI_2X1,
                config = WidgetConfig(style = WidgetStyle.SURFACE),
            ),
        )

        val encoded = WidgetTemplateSerializer.encodeTemplates(templates)
        val decoded = WidgetTemplateSerializer.decodeTemplates(encoded)
        assertEquals(2, decoded.size)
        assertEquals("Violeta \"Studio\"\n2x2", decoded[0].name)
        assertEquals(WidgetFormat.SQUARE_2X2, decoded[0].format)
        assertEquals(WidgetStyle.ARTWORK, decoded[0].config.style)
        assertEquals(28, decoded[0].config.cornerRadiusDp)
        assertFalse(decoded[0].config.showProgressBar)
        assertEquals(CustomControlAction.LIKE_TRACK, decoded[0].config.leftCustomAction)
        assertEquals(CustomControlAction.SHUFFLE, decoded[0].config.rightCustomAction)

        // Legacy style migration check inside template import
        assertEquals(WidgetStyle.SURFACE, WidgetTemplateSerializer.migrateWidgetStyleRaw("CAPSULE_DARK"))
        assertEquals(WidgetStyle.TONAL, WidgetTemplateSerializer.migrateWidgetStyleRaw("FROSTED_GLASS"))

        // Malformed JSON resilience
        assertTrue(WidgetTemplateSerializer.decodeTemplates("not valid json").isEmpty())
        assertNull(WidgetTemplateSerializer.decodeTemplate("{{{"))
        assertNotNull(WidgetTemplateSerializer.decodeTemplate("{\"name\":\"Solo\"}"))
    }

    @Test
    fun systemHealthCalculator_evaluatesGreenAmberRedCorrectly() {
        val allGreen = SystemHealthCalculator.evaluate(
            hasListenerPermission = true,
            listenerConnected = true,
            hasPostNotificationsPermission = true,
            isIgnoringBatteryOptimizations = true,
            hasPinnedWidget = true,
            hasQuickTileAdded = true,
        )
        assertEquals(HealthLight.GREEN, allGreen.overallLight)
        assertFalse(allGreen.hasPendingIssues)
        assertEquals(5, allGreen.greenCount)

        val criticalRed = SystemHealthCalculator.evaluate(
            hasListenerPermission = false,
            listenerConnected = false,
            hasPostNotificationsPermission = true,
            isIgnoringBatteryOptimizations = true,
            hasPinnedWidget = true,
            hasQuickTileAdded = true,
        )
        assertEquals(HealthLight.RED, criticalRed.overallLight)
        assertTrue(criticalRed.hasPendingIssues)
        assertEquals(HealthCheckId.NOTIFICATION_LISTENER, criticalRed.items.first { it.light == HealthLight.RED }.id)

        val warningAmber = SystemHealthCalculator.evaluate(
            hasListenerPermission = true,
            listenerConnected = true,
            hasPostNotificationsPermission = true,
            isIgnoringBatteryOptimizations = false,
            hasPinnedWidget = true,
            hasQuickTileAdded = true,
        )
        assertEquals(HealthLight.AMBER, warningAmber.overallLight)
        assertEquals(1, warningAmber.pendingCount)
    }

    @Test
    fun sleepTimerManager_fadeAndEndOfTrackTriggerCorrectly() {
        assertEquals(1.0f, SleepTimerManager.computeFadeVolumeMultiplier(15_000L), 0.001f)
        assertEquals(0.5f, SleepTimerManager.computeFadeVolumeMultiplier(5_000L), 0.001f)
        assertEquals(0.0f, SleepTimerManager.computeFadeVolumeMultiplier(0L), 0.001f)

        val armed = SleepTimerState(
            mode = SleepTimerMode.END_OF_TRACK,
            trackKeyAtArm = "pkg|Track A|Artist",
        )
        assertFalse(SleepTimerManager.shouldTriggerOnTrackChange(armed, "pkg|Track A|Artist", 30_000L, 120_000L))
        assertTrue(SleepTimerManager.shouldTriggerOnTrackChange(armed, "pkg|Track B|Artist", 1_000L, 120_000L))
        assertTrue(SleepTimerManager.shouldTriggerOnTrackChange(armed, "pkg|Track A|Artist", 119_500L, 120_000L))
        assertEquals("05:00", SleepTimerManager.formatRemaining(300_000L))
    }
}
