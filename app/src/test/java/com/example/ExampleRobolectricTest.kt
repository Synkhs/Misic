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
                style = WidgetStyle.DYNAMIC_NEON,
                leftCustomAction = CustomControlAction.SWITCH_SOURCE,
            )
        }
        val snapshot = repo.getWidgetConfigSnapshot()
        assertEquals(WidgetStyle.DYNAMIC_NEON, snapshot.style)
        assertEquals(CustomControlAction.SWITCH_SOURCE, snapshot.leftCustomAction)
    }

    @Test
    fun navigationTabs_switchBetweenHomeWidgetsLockAndServices() {
        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithTag("magic_capsule_banner").assertIsDisplayed()

        composeTestRule.onNodeWithTag("nav_tab_widgets").performClick()
        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithTag("studio_mode_card").assertIsDisplayed()
        composeTestRule.onNodeWithTag("widget_studio_list").performScrollToNode(hasTestTag("studio_pin_widget_btn"))
        composeTestRule.onNodeWithTag("studio_pin_widget_btn").assertIsDisplayed()

        composeTestRule.onNodeWithTag("nav_tab_lock_qs").performClick()
        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithTag("lock_qs_list").performScrollToNode(hasTestTag("open_lock_deck_preview_btn"))
        composeTestRule.onNodeWithTag("open_lock_deck_preview_btn").assertIsDisplayed()
        composeTestRule.onNodeWithTag("lock_qs_list").performScrollToNode(hasTestTag("add_qs_play_tile_btn"))
        composeTestRule.onNodeWithTag("add_qs_play_tile_btn").assertIsDisplayed()

        composeTestRule.onNodeWithTag("nav_tab_services").performClick()
        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithTag("services_list").performScrollToNode(hasTestTag("service_card_spotify"))
        composeTestRule.onNodeWithTag("service_card_spotify").assertIsDisplayed()
    }
}
