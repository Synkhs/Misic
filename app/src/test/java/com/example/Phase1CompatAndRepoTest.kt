package com.example

import android.content.Context
import android.media.session.PlaybackState
import androidx.test.core.app.ApplicationProvider
import com.example.core.compat.InstalledPlayersScanner
import com.example.core.compat.OemCompat
import com.example.core.compat.OemFamily
import com.example.core.media.MediaRepository
import com.example.core.media.PlaybackCapabilities
import com.example.core.media.SessionSnapshot
import com.example.data.MediaSessionPreferences
import com.example.data.MusicServiceApp
import com.example.data.PreferencesRepository
import com.example.data.SessionPriorityRule
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class Phase1CompatAndRepoTest {

    @Test
    fun oemDetection_identifiesAllMajorManufacturers() {
        assertEquals(OemFamily.SAMSUNG, OemCompat.detectOemFamily("samsung", "samsung"))
        assertEquals(OemFamily.XIAOMI, OemCompat.detectOemFamily("Xiaomi", "Redmi"))
        assertEquals(OemFamily.OPPO_REALME, OemCompat.detectOemFamily("realme", "realme"))
        assertEquals(OemFamily.ONEPLUS, OemCompat.detectOemFamily("OnePlus", "OnePlus"))
        assertEquals(OemFamily.HONOR, OemCompat.detectOemFamily("HONOR", "HONOR"))
        assertEquals(OemFamily.MOTOROLA, OemCompat.detectOemFamily("motorola", "moto"))
        assertEquals(OemFamily.GOOGLE_PIXEL, OemCompat.detectOemFamily("Google", "google"))
        assertEquals(OemFamily.NOTHING, OemCompat.detectOemFamily("Nothing", "Nothing"))
        assertEquals(OemFamily.GENERIC, OemCompat.detectOemFamily("CustomMaker", "Unknown"))
    }

    @Test
    fun oemBatteryGuide_returnsTailoredStepsForContext() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val guide = OemCompat.getBatteryAndBackgroundGuide(context, "Xiaomi", "POCO")
        assertEquals(OemFamily.XIAOMI, guide.family)
        assertTrue(guide.steps.isNotEmpty())
        assertTrue(guide.summary.contains("HyperOS"))
    }

    @Test
    fun videoFilter_ignoresYouTubeVideoButKeepsYouTubeMusic() {
        assertTrue(InstalledPlayersScanner.isLikelyVideoApp("com.google.android.youtube"))
        assertTrue(InstalledPlayersScanner.isLikelyVideoApp("com.netflix.mediaclient"))
        assertFalse(InstalledPlayersScanner.isLikelyVideoApp("com.google.android.apps.youtube.music"))
        assertFalse(InstalledPlayersScanner.isLikelyVideoApp("com.spotify.music"))
    }

    @Test
    fun sessionPriorityRules_selectExpectedSession() {
        val spotify = sampleSnapshot("com.spotify.music", isPlaying = false, lastActive = 5000L)
        val ytMusic = sampleSnapshot("com.google.android.apps.youtube.music", isPlaying = true, lastActive = 2000L)
        val amazon = sampleSnapshot("com.amazon.mp3", isPlaying = false, lastActive = 9000L)
        val list = listOf(spotify, ytMusic, amazon)

        val playingFirst = MediaRepository.selectBestSession(
            sessions = list,
            manualSelectedPackage = null,
            prefs = MediaSessionPreferences(priorityRule = SessionPriorityRule.PLAYING_FIRST),
        )
        assertEquals("com.google.android.apps.youtube.music", playingFirst?.packageName)

        val lastActive = MediaRepository.selectBestSession(
            sessions = list,
            manualSelectedPackage = null,
            prefs = MediaSessionPreferences(priorityRule = SessionPriorityRule.LAST_ACTIVE),
        )
        assertEquals("com.amazon.mp3", lastActive?.packageName)

        val pinned = MediaRepository.selectBestSession(
            sessions = list,
            manualSelectedPackage = null,
            prefs = MediaSessionPreferences(
                priorityRule = SessionPriorityRule.PINNED_PREFERRED,
                preferredServicePackage = "com.spotify.music",
            ),
        )
        assertEquals("com.spotify.music", pinned?.packageName)
    }

    @Test
    fun positionInterpolation_advancesWhenPlayingAndStopsWhenPaused() {
        val playing = sampleSnapshot(
            pkg = "com.spotify.music",
            isPlaying = true,
            lastActive = 1000L,
            positionMs = 10_000L,
            lastUpdateElapsed = 1000L,
            durationMs = 60_000L,
        )
        assertEquals(15_000L, playing.computeInterpolatedPosition(nowElapsedMs = 6000L))

        val paused = playing.copy(isPlaying = false)
        assertEquals(10_000L, paused.computeInterpolatedPosition(nowElapsedMs = 6000L))
    }

    @Test
    fun playbackCapabilities_parsesActionsBitmask() {
        val pb = PlaybackState.Builder()
            .setActions(PlaybackState.ACTION_PLAY_PAUSE or PlaybackState.ACTION_SKIP_TO_NEXT)
            .setState(PlaybackState.STATE_PLAYING, 1200L, 1.0f)
            .build()

        val caps = PlaybackCapabilities.fromPlaybackState(pb)
        assertTrue(caps.canPlayPause)
        assertTrue(caps.canSkipNext)
        assertFalse(caps.canSkipPrevious)
        assertFalse(caps.canSeek)
    }

    @Test
    fun preferencesRepository_persistsMediaSessionPreferences() = runTest {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val repo = PreferencesRepository(context)
        repo.updateMediaSessionPreferences {
            it.copy(
                priorityRule = SessionPriorityRule.PINNED_PREFERRED,
                ignoreVideoSessions = true,
                disabledPackages = setOf("com.google.android.youtube"),
                preferredServicePackage = "com.amazon.mp3",
            )
        }
        val saved = repo.getMediaSessionPreferencesSnapshot()
        assertNotNull(saved)
        assertEquals(SessionPriorityRule.PINNED_PREFERRED, saved.priorityRule)
        assertTrue(saved.ignoreVideoSessions)
        assertTrue(saved.disabledPackages.contains("com.google.android.youtube"))
        assertEquals("com.amazon.mp3", saved.preferredServicePackage)
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
            playbackStateCode = if (isPlaying) PlaybackState.STATE_PLAYING else PlaybackState.STATE_PAUSED,
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
