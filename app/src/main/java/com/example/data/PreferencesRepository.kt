package com.example.data

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.floatPreferencesKey
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map

private val Context.magicMusicDataStore: DataStore<Preferences> by preferencesDataStore(
    name = "magic_music_prefs",
)

class PreferencesRepository(private val context: Context) {

    private object Keys {
        val WIDGET_STYLE = stringPreferencesKey("widget_style")
        val CORNER_RADIUS = intPreferencesKey("widget_corner_radius")
        val GLASS_OPACITY = floatPreferencesKey("widget_glass_opacity")
        val SHOW_PROGRESS = booleanPreferencesKey("widget_show_progress")
        val SHOW_SERVICE_BADGE = booleanPreferencesKey("widget_show_service_badge")
        val SHOW_CAPSULE_HEADER = booleanPreferencesKey("widget_show_capsule_header")
        val LEFT_CUSTOM_ACTION = stringPreferencesKey("widget_left_custom_action")
        val RIGHT_CUSTOM_ACTION = stringPreferencesKey("widget_right_custom_action")

        val BRIDGE_ENABLED = booleanPreferencesKey("lock_bridge_enabled")
        val SHOW_ON_LOCK_SCREEN = booleanPreferencesKey("lock_show_on_lock_screen")
        val LOCK_ACTION_1 = stringPreferencesKey("lock_action_1")
        val LOCK_ACTION_2 = stringPreferencesKey("lock_action_2")
        val QS_TILE_MODE = stringPreferencesKey("qs_tile_mode")
        val HIGH_CONTRAST_CAPSULE = booleanPreferencesKey("high_contrast_capsule")
        val PREFERRED_SERVICE_PKG = stringPreferencesKey("preferred_service_pkg")
    }

    val widgetConfigFlow: Flow<WidgetConfig> = context.magicMusicDataStore.data.map { prefs ->
        mapWidgetConfig(prefs)
    }

    val lockAndQsConfigFlow: Flow<LockAndQsConfig> = context.magicMusicDataStore.data.map { prefs ->
        mapLockConfig(prefs)
    }

    val preferredServiceFlow: Flow<String> = context.magicMusicDataStore.data.map { prefs ->
        prefs[Keys.PREFERRED_SERVICE_PKG] ?: MusicServiceApp.SPOTIFY.packageName
    }

    suspend fun getWidgetConfigSnapshot(): WidgetConfig =
        mapWidgetConfig(context.magicMusicDataStore.data.first())

    suspend fun getLockConfigSnapshot(): LockAndQsConfig =
        mapLockConfig(context.magicMusicDataStore.data.first())

    suspend fun updateWidgetConfig(transform: (WidgetConfig) -> WidgetConfig) {
        context.magicMusicDataStore.edit { prefs ->
            val updated = transform(mapWidgetConfig(prefs))
            prefs[Keys.WIDGET_STYLE] = updated.style.name
            prefs[Keys.CORNER_RADIUS] = updated.cornerRadiusDp
            prefs[Keys.GLASS_OPACITY] = updated.glassOpacity
            prefs[Keys.SHOW_PROGRESS] = updated.showProgressBar
            prefs[Keys.SHOW_SERVICE_BADGE] = updated.showServiceBadge
            prefs[Keys.SHOW_CAPSULE_HEADER] = updated.showMagicCapsuleHeader
            prefs[Keys.LEFT_CUSTOM_ACTION] = updated.leftCustomAction.name
            prefs[Keys.RIGHT_CUSTOM_ACTION] = updated.rightCustomAction.name
        }
    }

    suspend fun updateLockAndQsConfig(transform: (LockAndQsConfig) -> LockAndQsConfig) {
        context.magicMusicDataStore.edit { prefs ->
            val updated = transform(mapLockConfig(prefs))
            prefs[Keys.BRIDGE_ENABLED] = updated.bridgeEnabled
            prefs[Keys.SHOW_ON_LOCK_SCREEN] = updated.showOnLockScreen
            prefs[Keys.LOCK_ACTION_1] = updated.lockScreenAction1.name
            prefs[Keys.LOCK_ACTION_2] = updated.lockScreenAction2.name
            prefs[Keys.QS_TILE_MODE] = updated.qsTileMode.name
            prefs[Keys.HIGH_CONTRAST_CAPSULE] = updated.preferHighContrastCapsule
        }
    }

    suspend fun setPreferredServicePackage(packageName: String) {
        context.magicMusicDataStore.edit { prefs ->
            prefs[Keys.PREFERRED_SERVICE_PKG] = packageName
        }
    }

    private fun mapWidgetConfig(prefs: Preferences): WidgetConfig {
        val style = enumValueOrDefault(prefs[Keys.WIDGET_STYLE], WidgetStyle.FROSTED_GLASS)
        val left = enumValueOrDefault(prefs[Keys.LEFT_CUSTOM_ACTION], CustomControlAction.REWIND_15)
        val right = enumValueOrDefault(prefs[Keys.RIGHT_CUSTOM_ACTION], CustomControlAction.FORWARD_15)
        return WidgetConfig(
            style = style,
            cornerRadiusDp = prefs[Keys.CORNER_RADIUS] ?: 26,
            glassOpacity = prefs[Keys.GLASS_OPACITY] ?: 0.90f,
            showProgressBar = prefs[Keys.SHOW_PROGRESS] ?: true,
            showServiceBadge = prefs[Keys.SHOW_SERVICE_BADGE] ?: true,
            showMagicCapsuleHeader = prefs[Keys.SHOW_CAPSULE_HEADER] ?: true,
            leftCustomAction = left,
            rightCustomAction = right,
        )
    }

    private fun mapLockConfig(prefs: Preferences): LockAndQsConfig {
        val a1 = enumValueOrDefault(prefs[Keys.LOCK_ACTION_1], CustomControlAction.REWIND_15)
        val a2 = enumValueOrDefault(prefs[Keys.LOCK_ACTION_2], CustomControlAction.FORWARD_15)
        val tileMode = enumValueOrDefault(prefs[Keys.QS_TILE_MODE], QsTileMode.PLAY_PAUSE)
        return LockAndQsConfig(
            bridgeEnabled = prefs[Keys.BRIDGE_ENABLED] ?: true,
            showOnLockScreen = prefs[Keys.SHOW_ON_LOCK_SCREEN] ?: true,
            lockScreenAction1 = a1,
            lockScreenAction2 = a2,
            qsTileMode = tileMode,
            preferHighContrastCapsule = prefs[Keys.HIGH_CONTRAST_CAPSULE] ?: true,
        )
    }

    private inline fun <reified T : Enum<T>> enumValueOrDefault(raw: String?, default: T): T {
        if (raw.isNullOrBlank()) return default
        return runCatching { enumValueOf<T>(raw) }.getOrDefault(default)
    }
}
