package com.example

import com.example.core.compat.InstalledPlayersScanner
import com.example.core.compat.OemCompat
import com.example.core.compat.OemFamily
import com.example.core.media.PlaybackCapabilities
import com.example.core.media.PlayerState
import com.example.core.media.SessionSelector
import com.example.core.media.SessionSnapshot
import com.example.data.MediaSessionPreferences
import com.example.data.MusicServiceApp
import com.example.data.SessionPriorityRule
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class Phase1LogicUnitTest {

    @Test
    fun oemDetection_identifiesAllMajorManufacturers() {
        assertEquals(OemFamily.SAMSUNG, OemCompat.detectOemFamily("samsung", "samsung"))
        assertEquals(OemFamily.XIAOMI, OemCompat.detectOemFamily("Xiaomi", "Redmi"))
        assertEquals(OemFamily.XIAOMI, OemCompat.detectOemFamily("Xiaomi", "POCO"))
        assertEquals(OemFamily.OPPO_REALME, OemCompat.detectOemFamily("OPPO", "OPPO"))
        assertEquals(OemFamily.OPPO_REALME, OemCompat.detectOemFamily("realme", "realme"))
        assertEquals(OemFamily.ONEPLUS, OemCompat.detectOemFamily("OnePlus", "OnePlus"))
        assertEquals(OemFamily.HONOR, OemCompat.detectOemFamily("HONOR", "HONOR"))
        assertEquals(OemFamily.HUAWEI, OemCompat.detectOemFamily("HUAWEI", "HUAWEI"))
        assertEquals(OemFamily.VIVO, OemCompat.detectOemFamily("vivo", "iQOO"))
        assertEquals(OemFamily.MOTOROLA, OemCompat.detectOemFamily("motorola", "moto"))
        assertEquals(OemFamily.GOOGLE_PIXEL, OemCompat.detectOemFamily("Google", "google"))
        assertEquals(OemFamily.SONY, OemCompat.detectOemFamily("Sony", "Sony"))
        assertEquals(OemFamily.NOTHING, OemCompat.detectOemFamily("Nothing", "Nothing"))
        assertEquals(OemFamily.GENERIC, OemCompat.detectOemFamily("CustomMaker", "Unknown"))
    }

    @Test
    fun videoFilter_ignoresVideoAppsButKeepsMusicApps() {
        assertTrue(InstalledPlayersScanner.isLikelyVideoApp("com.google.android.youtube"))
        assertTrue(InstalledPlayersScanner.isLikelyVideoApp("com.netflix.mediaclient"))
        assertTrue(InstalledPlayersScanner.isLikelyVideoApp("tv.twitch.android.app"))
        assertFalse(InstalledPlayersScanner.isLikelyVideoApp("com.google.android.apps.youtube.music"))
        assertFalse(InstalledPlayersScanner.isLikelyVideoApp("com.spotify.music"))
        assertFalse(InstalledPlayersScanner.isLikelyVideoApp("com.amazon.mp3"))
        assertFalse(InstalledPlayersScanner.isLikelyVideoApp("com.soundcloud.android"))
    }

    @Test
    fun musicServiceMapping_supportsThirdPartyAndGenericPlayers() {
        assertEquals(MusicServiceApp.SPOTIFY, MusicServiceApp.fromPackageName("com.spotify.music"))
        assertEquals(
            MusicServiceApp.YOUTUBE_MUSIC,
            MusicServiceApp.fromPackageName("com.google.android.apps.youtube.music"),
        )
        assertEquals(MusicServiceApp.AMAZON_MUSIC, MusicServiceApp.fromPackageName("com.amazon.mp3"))
        assertEquals(MusicServiceApp.SOUNDCLOUD, MusicServiceApp.fromPackageName("com.soundcloud.android"))
        assertEquals(MusicServiceApp.OTHER, MusicServiceApp.fromPackageName("in.krosbits.musicolet"))
        assertFalse(MusicServiceApp.entries.any { it.name.contains("HONOR") })
    }

    @Test
    fun sessionPriorityRules_selectExpectedSession() {
        val spotify = sampleSnapshot("com.spotify.music", isPlaying = false, lastActive = 5000L)
        val ytMusic = sampleSnapshot("com.google.android.apps.youtube.music", isPlaying = true, lastActive = 2000L)
        val amazon = sampleSnapshot("com.amazon.mp3", isPlaying = false, lastActive = 9000L)
        val list = listOf(spotify, ytMusic, amazon)

        val playingFirst = SessionSelector.selectBestSession(
            sessions = list,
            manualSelectedPackage = null,
            prefs = MediaSessionPreferences(priorityRule = SessionPriorityRule.PLAYING_FIRST),
        )
        assertEquals("com.google.android.apps.youtube.music", playingFirst?.packageName)

        val lastActive = SessionSelector.selectBestSession(
            sessions = list,
            manualSelectedPackage = null,
            prefs = MediaSessionPreferences(priorityRule = SessionPriorityRule.LAST_ACTIVE),
        )
        assertEquals("com.amazon.mp3", lastActive?.packageName)

        val pinned = SessionSelector.selectBestSession(
            sessions = list,
            manualSelectedPackage = null,
            prefs = MediaSessionPreferences(
                priorityRule = SessionPriorityRule.PINNED_PREFERRED,
                preferredServicePackage = "com.spotify.music",
            ),
        )
        assertEquals("com.spotify.music", pinned?.packageName)

        val manual = SessionSelector.selectBestSession(
            sessions = list,
            manualSelectedPackage = "com.amazon.mp3",
            prefs = MediaSessionPreferences(priorityRule = SessionPriorityRule.PLAYING_FIRST),
        )
        assertEquals("com.amazon.mp3", manual?.packageName)
    }

    @Test
    fun positionInterpolation_advancesWhenPlayingAndClampsToDuration() {
        val playing = sampleSnapshot(
            pkg = "com.spotify.music",
            isPlaying = true,
            lastActive = 1000L,
            positionMs = 10_000L,
            lastUpdateElapsed = 1000L,
            durationMs = 60_000L,
        )
        assertEquals(15_000L, playing.computeInterpolatedPosition(nowElapsedMs = 6000L))
        assertEquals(60_000L, playing.computeInterpolatedPosition(nowElapsedMs = 99_000L))

        val paused = playing.copy(isPlaying = false)
        assertEquals(10_000L, paused.computeInterpolatedPosition(nowElapsedMs = 6000L))
    }

    @Test
    fun playerState_mapsCapabilitiesToUnifiedMediaState() {
        val snap = sampleSnapshot(
            pkg = "com.spotify.music",
            isPlaying = true,
            lastActive = 1000L,
            durationMs = 100_000L,
        ).copy(
            isSelected = true,
            capabilities = PlaybackCapabilities(
                canPlayPause = true,
                canSkipNext = false,
                canSkipPrevious = true,
                canSeek = false,
            ),
        )
        val state = PlayerState(
            activeSession = snap,
            sessions = listOf(snap),
            interpolatedPositionMs = 25_000L,
        )
        val unified = state.toUnifiedMediaState()
        assertEquals(25_000L, unified.positionMs)
        assertEquals(0.25f, unified.progressFraction, 0.001f)
        assertTrue(unified.canPlayPause)
        assertFalse(unified.canSkipNext)
        assertTrue(unified.canSkipPrevious)
        assertFalse(unified.canSeek)
    }

    private fun sampleSnapshot(
        pkg: String,
        isPlaying: Boolean,
        lastActive: Long,
        positionMs: Long = 0L,
        lastUpdateElapsed: Long = 0L,
        durationMs: Long = 180_000L,
    ): SessionSnapshot {
        val svc = MusicServiceApp.fromPackageName(pkg)
        return SessionSnapshot(
            packageName = pkg,
            appName = svc.displayName,
            service = svc,
            title = "Track",
            artist = "Artist",
            album = "Album",
            isPlaying = isPlaying,
            playbackStateCode = if (isPlaying) 3 else 2,
            positionMs = positionMs,
            lastPositionUpdateElapsedMs = lastUpdateElapsed,
            playbackSpeed = 1.0f,
            durationMs = durationMs,
            albumArt = null,
            albumArtUri = null,
            capabilities = PlaybackCapabilities(canPlayPause = true, canSkipNext = true),
            isLocalEngine = false,
            isSelected = false,
            lastActiveTimestampMs = lastActive,
        )
    }
}
