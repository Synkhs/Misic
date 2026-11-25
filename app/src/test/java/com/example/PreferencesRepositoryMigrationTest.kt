package com.example

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.example.data.PreferencesRepository
import com.example.data.WidgetColorSource
import com.example.designsystem.ContrastPreset
import com.example.designsystem.McuSchemeVariant
import com.example.designsystem.ThemePreferences
import com.example.designsystem.expressive.ArtworkShapeStyle
import com.example.designsystem.expressive.GeometryPreset
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class PreferencesRepositoryMigrationTest {

    @Test
    fun colorProfilesAndPerWidgetColorConfigs_persistAndRoundTripJson() = runTest {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val repo = PreferencesRepository(context)

        val sampleTheme = ThemePreferences(
            seedColorArgb = 0xFF3A47B5L,
            schemeVariant = McuSchemeVariant.EXPRESSIVE,
            contrastLevel = ContrastPreset.HIGH,
            geometryPreset = GeometryPreset.SOFT_EXPRESSIVE,
            artworkShape = ArtworkShapeStyle.SOFT,
        )
        repo.saveColorProfile("Lavanda Índigo Test", sampleTheme)
        val exported = repo.exportColorProfilesJson()
        val importedCount = repo.importColorProfilesJson(exported)
        assertTrue(importedCount >= 1)

        repo.updateWidgetInstanceConfig(77) { cur ->
            cur.copy(
                colorSource = WidgetColorSource.CUSTOM_SEED,
                seedColorArgb = 0xFF006D40L,
                schemeVariant = McuSchemeVariant.VIBRANT,
                contrastLevel = ContrastPreset.MEDIUM,
            )
        }
        val instances = repo.widgetInstancesFlow.first()
        val widget77 = instances[77]
        assertEquals(WidgetColorSource.CUSTOM_SEED, widget77?.colorSource)
        assertEquals(0xFF006D40L, widget77?.seedColorArgb)
        assertEquals(McuSchemeVariant.VIBRANT, widget77?.schemeVariant)
    }
}
