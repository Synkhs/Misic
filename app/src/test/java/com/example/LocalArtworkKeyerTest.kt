package com.example

import com.example.core.artwork.LocalArtworkPipeline
import com.example.core.artwork.LocalArtworkRequest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Test

class LocalArtworkKeyerTest {

    @Test
    fun cacheKeyFor_distinguishesTrackIdDateModifiedAndTargetSize() {
        val req128 = LocalArtworkRequest(
            trackId = 42L,
            albumId = 10L,
            contentUri = "content://media/external/audio/media/42",
            albumArtUri = "content://media/external/audio/albumart/10",
            filePath = "/storage/emulated/0/Music/track.flac",
            dateModifiedSec = 1700000000L,
            targetSizePx = 128,
        )
        val req256 = req128.copy(targetSizePx = 256)
        val reqModified = req128.copy(dateModifiedSec = 1700000999L)
        val reqOtherTrack = req128.copy(trackId = 43L)

        val k128 = LocalArtworkPipeline.cacheKeyFor(req128)
        assertEquals("42_1700000000_128", k128)
        assertNotEquals(k128, LocalArtworkPipeline.cacheKeyFor(req256))
        assertNotEquals(k128, LocalArtworkPipeline.cacheKeyFor(reqModified))
        assertNotEquals(k128, LocalArtworkPipeline.cacheKeyFor(reqOtherTrack))
    }
}
