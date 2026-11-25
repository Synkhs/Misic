package com.example

import android.content.Context
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.hasTestTag
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollToNode
import androidx.test.core.app.ApplicationProvider
import com.example.data.CustomControlAction
import com.example.data.MusicServiceApp
import com.example.data.PreferencesRepository
import com.example.data.WidgetStyle
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36], qualifiers = "w411dp-h915dp-xxhdpi")
class ExampleRobolectricTest {

    @get:Rule
    val composeTestRule = createAndroidComposeRule<MainActivity>()

    @org.junit.Before
    fun setupPermission() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        org.robolectric.Shadows.shadowOf(context).grantPermissions(android.Manifest.permission.READ_MEDIA_AUDIO)
    }

    @Test
    fun appName_matchesMagicMusic() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        assertEquals("MagicMusic", context.getString(R.string.app_name))
    }

    @Test
    fun musicServiceDetection_mapsMajorStreamingApps() {
        assertEquals(MusicServiceApp.SPOTIFY, MusicServiceApp.fromPackageName("com.spotify.music"))
        assertEquals(
            MusicServiceApp.YOUTUBE_MUSIC,
            MusicServiceApp.fromPackageName("com.google.android.apps.youtube.music"),
        )
        assertEquals(MusicServiceApp.AMAZON_MUSIC, MusicServiceApp.fromPackageName("com.amazon.mp3"))
    }

    @Test
    fun preferencesRepository_persistsWidgetAndLockConfig() = runTest {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val repo = PreferencesRepository(context)
        repo.updateWidgetConfig {
            it.copy(
                style = WidgetStyle.ARTWORK,
                leftCustomAction = CustomControlAction.SWITCH_SOURCE,
            )
        }
        val snapshot = repo.getWidgetConfigSnapshot()
        assertEquals(WidgetStyle.ARTWORK, snapshot.style)
        assertEquals(CustomControlAction.SWITCH_SOURCE, snapshot.leftCustomAction)
    }

    @Test
    fun navigationTabs_switchBetweenLibraryPlaylistsEqualizerStyleAndSettings() {
        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithTag("magic_mini_player").assertIsDisplayed()
        composeTestRule.onNodeWithTag("library_main_list").assertIsDisplayed()

        // Listas tab
        composeTestRule.onNodeWithTag("nav_tab_playlists").performClick()
        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithTag("playlists_screen_list").assertIsDisplayed()

        // Ecualizador tab
        composeTestRule.onNodeWithTag("nav_tab_equalizer").performClick()
        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithTag("equalizer_screen_list").assertIsDisplayed()

        // Estilo tab -> Estudio de Widgets
        composeTestRule.onNodeWithTag("nav_tab_style").performClick()
        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithTag("appearance_list").assertIsDisplayed()

        // Ajustes -> Controles (Pantalla de bloqueo & QS) -> add_qs_tile_play_pause
        composeTestRule.onNodeWithTag("nav_tab_settings").performClick()
        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithTag("settings_row_controls").performClick()
        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithTag("open_lock_deck_preview_btn").assertIsDisplayed()
        composeTestRule.onNodeWithTag("controls_tab_qs").performClick()
        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithTag("add_qs_tile_play_pause").assertIsDisplayed()
    }
}
