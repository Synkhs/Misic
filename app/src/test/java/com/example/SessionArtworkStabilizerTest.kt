package com.example

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import com.example.core.artwork.ArtworkResolutionStatus
import com.example.core.artwork.SessionArtworkStabilizer
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class SessionArtworkStabilizerTest {

    private fun createDetailedBitmap(size: Int = 128): Bitmap {
        val bmp = Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bmp)
        val p1 = Paint().apply { color = Color.rgb(180, 160, 255) }
        val p2 = Paint().apply { color = Color.rgb(30, 35, 90) }
        canvas.drawRect(0f, 0f, size / 2f, size.toFloat(), p1)
        canvas.drawRect(size / 2f, 0f, size.toFloat(), size.toFloat(), p2)
        return bmp
    }

    private fun createUniformBitmap(size: Int = 128, argb: Int = Color.BLACK): Bitmap {
        return Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888).apply {
            eraseColor(argb)
        }
    }

    @Test
    fun stabilizer_preservesLastGoodBitmapDuringNullOrUniformFlicker() {
        val stabilizer = SessionArtworkStabilizer(graceWindowMs = 2_000L, minValidDimensionPx = 32)
        val validArt = createDetailedBitmap()

        // 1. Valid bitmap arrives for Track A
        val first = stabilizer.resolve(
            trackKey = "spotify|Track A|Artist",
            incomingBitmap = validArt,
            incomingUri = null,
            nowMs = 1_000L,
        )
        assertEquals(ArtworkResolutionStatus.DIRECT_BITMAP, first.status)
        assertNotNull(first.bitmap)

        // 2. Partial MediaController tick emits null bitmap -> must KEEP_LAST_GOOD
        val nullTick = stabilizer.resolve(
            trackKey = "spotify|Track A|Artist",
            incomingBitmap = null,
            incomingUri = null,
            nowMs = 1_500L,
        )
        assertEquals(ArtworkResolutionStatus.KEEP_LAST_GOOD, nullTick.status)
        assertNotNull(nullTick.bitmap)

        // 3. Uniform black frame arrives -> rejected and KEEP_LAST_GOOD preserved
        val uniformTick = stabilizer.resolve(
            trackKey = "spotify|Track A|Artist",
            incomingBitmap = createUniformBitmap(),
            incomingUri = null,
            nowMs = 1_800L,
        )
        assertEquals(ArtworkResolutionStatus.KEEP_LAST_GOOD, uniformTick.status)
        assertNotNull(uniformTick.bitmap)
        assertTrue(stabilizer.currentDebugState().rejectedUniformFrames >= 1)
    }

    @Test
    fun stabilizer_holdsGraceWindowBrieflyOnTrackSwitchThenExpiresToFallback() {
        val stabilizer = SessionArtworkStabilizer(graceWindowMs = 2_000L, minValidDimensionPx = 32)
        val trackAArt = createDetailedBitmap()

        stabilizer.resolve(
            trackKey = "spotify|Track A|Artist",
            incomingBitmap = trackAArt,
            incomingUri = null,
            nowMs = 1_000L,
        )

        // Switch to Track B while metadata bitmap hasn't arrived yet (within 2s grace window)
        val grace = stabilizer.resolve(
            trackKey = "spotify|Track B|Artist",
            incomingBitmap = null,
            incomingUri = null,
            nowMs = 2_000L,
        )
        assertEquals(ArtworkResolutionStatus.GRACE_HOLDING_PREVIOUS, grace.status)
        assertNotNull(grace.bitmap)

        // After grace window expires (now - changeAt > 2000ms), fallback cleanly without sticking Track A's art forever
        val expired = stabilizer.resolve(
            trackKey = "spotify|Track B|Artist",
            incomingBitmap = null,
            incomingUri = null,
            nowMs = 5_000L,
        )
        assertEquals(ArtworkResolutionStatus.FALLBACK_PLACEHOLDER, expired.status)
        assertNull(expired.bitmap)
    }
}
