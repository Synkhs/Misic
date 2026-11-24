package com.example.data

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.floatPreferencesKey
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.core.stringSetPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.example.designsystem.DEFAULT_VIOLET_SEED_ARGB
import com.example.designsystem.SurfaceToneStyle
import com.example.designsystem.ThemeMode
import com.example.designsystem.ThemePreferences
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import java.util.Calendar

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
        val SHOW_COMPACT_HEADER = booleanPreferencesKey("widget_show_compact_header")
        val LEGACY_SHOW_CAPSULE_HEADER = booleanPreferencesKey("widget_show_capsule_header")
        val LEFT_CUSTOM_ACTION = stringPreferencesKey("widget_left_custom_action")
        val RIGHT_CUSTOM_ACTION = stringPreferencesKey("widget_right_custom_action")
        val WIDGET_TEMPLATES_JSON = stringPreferencesKey("widget_templates_json")

        val BRIDGE_ENABLED = booleanPreferencesKey("lock_bridge_enabled")
        val SHOW_ON_LOCK_SCREEN = booleanPreferencesKey("lock_show_on_lock_screen")
        val SHOW_ARTWORK_ON_LOCK = booleanPreferencesKey("lock_show_artwork")
        val REQ_UNLOCK_OPEN_APP = booleanPreferencesKey("lock_req_unlock_open_app")
        val REQ_UNLOCK_SWITCH = booleanPreferencesKey("lock_req_unlock_switch")
        val REQ_UNLOCK_CUSTOM = booleanPreferencesKey("lock_req_unlock_custom")
        val LOCK_ACTION_1 = stringPreferencesKey("lock_action_1")
        val LOCK_ACTION_2 = stringPreferencesKey("lock_action_2")
        val QS_TILE_MODE = stringPreferencesKey("qs_tile_mode")
        val HIGH_CONTRAST_DECK = booleanPreferencesKey("high_contrast_capsule")
        val QS_PLAY_TILE_ADDED = booleanPreferencesKey("qs_play_tile_added")
        val QS_NEXT_TILE_ADDED = booleanPreferencesKey("qs_next_tile_added")
        val QS_ADDED_TILE_KEYS = stringSetPreferencesKey("qs_added_tile_keys")
        val LOCK_PRIVACY_LEVEL = stringPreferencesKey("lock_privacy_level")
        val LOCK_AUTO_CLOSE_SECONDS = intPreferencesKey("lock_auto_close_seconds")
        val COMPACT_ACTION_INDICES_CSV = stringPreferencesKey("compact_action_indices_csv")
        val NOTIFY_CHANNEL_GUIDE_SHOWN = booleanPreferencesKey("notify_channel_guide_shown")

        val PRIORITY_RULE = stringPreferencesKey("session_priority_rule")
        val PREFERRED_SERVICE_PKG = stringPreferencesKey("preferred_service_pkg")
        val IGNORE_VIDEO_SESSIONS = booleanPreferencesKey("ignore_video_sessions")
        val DISABLED_PACKAGES = stringSetPreferencesKey("disabled_media_packages")

        // Material 3 ThemePreferences keys
        val USE_DYNAMIC_COLOR = booleanPreferencesKey("app_use_dynamic_color")
        val THEME_MODE = stringPreferencesKey("app_theme_mode")
        val SURFACE_TONE_STYLE = stringPreferencesKey("app_surface_tone_style")
        val SEED_COLOR_ARGB = longPreferencesKey("app_seed_color_argb")
        val PURE_AMOLED_BLACK = booleanPreferencesKey("app_pure_amoled_black")
        val COLOR_FROM_ARTWORK = booleanPreferencesKey("app_color_from_artwork")
        val REDUCE_ANIMATIONS = booleanPreferencesKey("app_reduce_animations")

        // Equalizer & Hi-Res
        val EQ_ENABLED = booleanPreferencesKey("eq_enabled")
        val EQ_PRESET_INDEX = intPreferencesKey("eq_preset_index")
        val EQ_BANDS_CSV = stringPreferencesKey("eq_bands_csv")
        val EQ_ADVANCED_MODE = booleanPreferencesKey("eq_advanced_mode")
        val EQ_HIRES_BUFFER = booleanPreferencesKey("eq_hires_buffer")
        val EQ_BASS_BOOST = intPreferencesKey("eq_bass_boost")
        val EQ_LOUDNESS_GAIN = intPreferencesKey("eq_loudness_gain")
        val HIRES_GAPLESS = booleanPreferencesKey("hires_gapless")
        val HIRES_CROSSFADE_SEC = intPreferencesKey("hires_crossfade_sec")
        val HIRES_REPLAY_GAIN = booleanPreferencesKey("hires_replay_gain")
        val HIRES_FLOAT_24BIT = booleanPreferencesKey("hires_float_24bit")
        val EQ_ACTIVE_OUTPUT_CATEGORY = stringPreferencesKey("eq_active_output_category")
        val EQ_DEVICE_PROFILES_JSON = stringPreferencesKey("eq_device_profiles_blob")

        // Local stats & history & favorites & playlists
        val STATS_TOTAL_SECONDS = longPreferencesKey("stats_total_seconds")
        val STATS_DAILY_MINUTES_CSV = stringPreferencesKey("stats_daily_minutes_csv")
        val RECENT_TRACKS_JSON = stringPreferencesKey("recent_tracks_json")
        val FAVORITE_TRACK_IDS = stringSetPreferencesKey("favorite_track_ids")
        val PLAYLIST_TRACK_IDS = stringSetPreferencesKey("playlist_track_ids")

        // Onboarding
        val ONBOARDING_COMPLETED = booleanPreferencesKey("onboarding_completed")

        // Legacy keys read-only for seamless migration
        val LEGACY_THEME_ID = stringPreferencesKey("app_theme_id")
        val LEGACY_FINISH = stringPreferencesKey("app_magic_os_finish")
    }

    val widgetConfigFlow: Flow<WidgetConfig> = context.magicMusicDataStore.data
        .map { prefs -> mapWidgetConfig(prefs) }
        .distinctUntilChanged()

    val widgetTemplatesFlow: Flow<List<WidgetTemplate>> = context.magicMusicDataStore.data
        .map { prefs -> mapWidgetTemplates(prefs) }
        .distinctUntilChanged()

    val lockAndQsConfigFlow: Flow<LockAndQsConfig> = context.magicMusicDataStore.data
        .map { prefs -> mapLockConfig(prefs) }
        .distinctUntilChanged()

    val mediaSessionPreferencesFlow: Flow<MediaSessionPreferences> = context.magicMusicDataStore.data
        .map { prefs -> mapMediaSessionPreferences(prefs) }
        .distinctUntilChanged()

    val themePreferencesFlow: Flow<ThemePreferences> = context.magicMusicDataStore.data
        .map { prefs -> mapThemePreferences(prefs) }
        .distinctUntilChanged()

    val equalizerConfigFlow: Flow<EqualizerConfig> = context.magicMusicDataStore.data
        .map { prefs -> mapEqualizerConfig(prefs) }
        .distinctUntilChanged()

    val localStatsFlow: Flow<LocalListeningStats> = context.magicMusicDataStore.data
        .map { prefs -> mapLocalStats(prefs) }
        .distinctUntilChanged()

    val onboardingCompletedFlow: Flow<Boolean> = context.magicMusicDataStore.data
        .map { prefs -> prefs[Keys.ONBOARDING_COMPLETED] ?: false }
        .distinctUntilChanged()

    val quickTileAddedFlow: Flow<Boolean> = context.magicMusicDataStore.data
        .map { prefs ->
            (prefs[Keys.QS_PLAY_TILE_ADDED] ?: false) ||
                (prefs[Keys.QS_NEXT_TILE_ADDED] ?: false) ||
                !prefs[Keys.QS_ADDED_TILE_KEYS].isNullOrEmpty()
        }
        .distinctUntilChanged()

    val preferredServiceFlow: Flow<String> = context.magicMusicDataStore.data
        .map { prefs -> prefs[Keys.PREFERRED_SERVICE_PKG] ?: MusicServiceApp.SPOTIFY.packageName }
        .distinctUntilChanged()

    suspend fun getWidgetConfigSnapshot(): WidgetConfig =
        mapWidgetConfig(context.magicMusicDataStore.data.first())

    suspend fun getLockConfigSnapshot(): LockAndQsConfig =
        mapLockConfig(context.magicMusicDataStore.data.first())

    suspend fun getMediaSessionPreferencesSnapshot(): MediaSessionPreferences =
        mapMediaSessionPreferences(context.magicMusicDataStore.data.first())

    suspend fun getThemePreferencesSnapshot(): ThemePreferences =
        mapThemePreferences(context.magicMusicDataStore.data.first())

    suspend fun getEqualizerConfigSnapshot(): EqualizerConfig =
        mapEqualizerConfig(context.magicMusicDataStore.data.first())

    suspend fun isAnyQuickTileAddedSnapshot(): Boolean {
        val prefs = context.magicMusicDataStore.data.first()
        return (prefs[Keys.QS_PLAY_TILE_ADDED] ?: false) ||
            (prefs[Keys.QS_NEXT_TILE_ADDED] ?: false) ||
            !prefs[Keys.QS_ADDED_TILE_KEYS].isNullOrEmpty()
    }

    suspend fun setQuickTileAdded(isPlayPause: Boolean, added: Boolean) {
        val key = if (isPlayPause) QuickTileType.PLAY_PAUSE.key else QuickTileType.NEXT.key
        setQuickTileAdded(key, added)
    }

    suspend fun setQuickTileAdded(tileKey: String, added: Boolean) {
        context.magicMusicDataStore.edit { prefs ->
            val cur = (prefs[Keys.QS_ADDED_TILE_KEYS] ?: emptySet()).toMutableSet()
            if (added) cur.add(tileKey) else cur.remove(tileKey)
            prefs[Keys.QS_ADDED_TILE_KEYS] = cur
            if (tileKey == QuickTileType.PLAY_PAUSE.key) {
                prefs[Keys.QS_PLAY_TILE_ADDED] = added
            } else if (tileKey == QuickTileType.NEXT.key) {
                prefs[Keys.QS_NEXT_TILE_ADDED] = added
            }
        }
    }

    suspend fun setOnboardingCompleted(completed: Boolean) {
        context.magicMusicDataStore.edit { prefs ->
            prefs[Keys.ONBOARDING_COMPLETED] = completed
        }
    }

    suspend fun updateWidgetConfig(transform: (WidgetConfig) -> WidgetConfig) {
        context.magicMusicDataStore.edit { prefs ->
            val updated = transform(mapWidgetConfig(prefs))
            prefs[Keys.WIDGET_STYLE] = updated.style.name
            prefs[Keys.CORNER_RADIUS] = updated.cornerRadiusDp
            prefs[Keys.GLASS_OPACITY] = updated.glassOpacity
            prefs[Keys.SHOW_PROGRESS] = updated.showProgressBar
            prefs[Keys.SHOW_SERVICE_BADGE] = updated.showServiceBadge
            prefs[Keys.SHOW_COMPACT_HEADER] = updated.showCompactHeader
            prefs.remove(Keys.LEGACY_SHOW_CAPSULE_HEADER)
            prefs[Keys.LEFT_CUSTOM_ACTION] = updated.leftCustomAction.name
            prefs[Keys.RIGHT_CUSTOM_ACTION] = updated.rightCustomAction.name
        }
    }

    suspend fun saveWidgetTemplate(name: String, format: WidgetFormat, config: WidgetConfig) {
        context.magicMusicDataStore.edit { prefs ->
            val current = mapWidgetTemplates(prefs).toMutableList()
            val tpl = WidgetTemplate(
                id = "tpl_${System.currentTimeMillis()}",
                name = name.trim().ifBlank { "Plantilla ${current.size + 1}" },
                format = format,
                config = config,
            )
            current.add(0, tpl)
            prefs[Keys.WIDGET_TEMPLATES_JSON] = WidgetTemplateSerializer.encodeTemplates(current.take(20))
        }
    }

    suspend fun duplicateWidgetTemplate(templateId: String) {
        context.magicMusicDataStore.edit { prefs ->
            val current = mapWidgetTemplates(prefs).toMutableList()
            val source = current.firstOrNull { it.id == templateId } ?: return@edit
            val copy = source.copy(
                id = "tpl_${System.currentTimeMillis()}",
                name = "${source.name} (Copia)",
            )
            val idx = current.indexOfFirst { it.id == templateId }
            current.add((idx + 1).coerceAtMost(current.size), copy)
            prefs[Keys.WIDGET_TEMPLATES_JSON] = WidgetTemplateSerializer.encodeTemplates(current.take(20))
        }
    }

    suspend fun deleteWidgetTemplate(templateId: String) {
        context.magicMusicDataStore.edit { prefs ->
            val current = mapWidgetTemplates(prefs).filterNot { it.id == templateId }
            prefs[Keys.WIDGET_TEMPLATES_JSON] = WidgetTemplateSerializer.encodeTemplates(current)
        }
    }

    suspend fun applyWidgetTemplate(template: WidgetTemplate) {
        updateWidgetConfig { template.config }
    }

    suspend fun exportWidgetTemplatesJson(): String {
        val templates = mapWidgetTemplates(context.magicMusicDataStore.data.first())
        return WidgetTemplateSerializer.encodeTemplates(templates)
    }

    suspend fun importWidgetTemplatesJson(rawJson: String): Int {
        val decoded = WidgetTemplateSerializer.decodeTemplates(rawJson)
        if (decoded.isEmpty()) return 0
        context.magicMusicDataStore.edit { prefs ->
            val current = mapWidgetTemplates(prefs).toMutableList()
            val existingIds = current.map { it.id }.toMutableSet()
            decoded.forEach { item ->
                val unique = if (existingIds.contains(item.id)) {
                    item.copy(id = "tpl_imp_${System.currentTimeMillis()}_${existingIds.size}")
                } else {
                    item
                }
                existingIds.add(unique.id)
                current.add(0, unique)
            }
            prefs[Keys.WIDGET_TEMPLATES_JSON] = WidgetTemplateSerializer.encodeTemplates(current.take(20))
        }
        return decoded.size
    }

    suspend fun updateLockAndQsConfig(transform: (LockAndQsConfig) -> LockAndQsConfig) {
        context.magicMusicDataStore.edit { prefs ->
            val updated = transform(mapLockConfig(prefs))
            prefs[Keys.BRIDGE_ENABLED] = updated.bridgeEnabled
            prefs[Keys.SHOW_ON_LOCK_SCREEN] = updated.showOnLockScreen
            prefs[Keys.SHOW_ARTWORK_ON_LOCK] = updated.showArtworkOnLockScreen
            prefs[Keys.LOCK_PRIVACY_LEVEL] = updated.lockPrivacyLevel.name
            prefs[Keys.LOCK_AUTO_CLOSE_SECONDS] = updated.lockAutoCloseSeconds
            prefs[Keys.COMPACT_ACTION_INDICES_CSV] = sanitizeCompactIndices(updated.compactActionIndices).joinToString(",")
            prefs[Keys.NOTIFY_CHANNEL_GUIDE_SHOWN] = updated.notifyChannelGuideShown
            prefs[Keys.REQ_UNLOCK_OPEN_APP] = updated.requireUnlockToOpenApp
            prefs[Keys.REQ_UNLOCK_SWITCH] = updated.requireUnlockToSwitchSource
            prefs[Keys.REQ_UNLOCK_CUSTOM] = updated.requireUnlockForCustomActions
            prefs[Keys.LOCK_ACTION_1] = updated.lockScreenAction1.name
            prefs[Keys.LOCK_ACTION_2] = updated.lockScreenAction2.name
            prefs[Keys.QS_TILE_MODE] = updated.qsTileMode.name
            prefs[Keys.HIGH_CONTRAST_DECK] = updated.preferHighContrastDeck
            prefs[Keys.QS_ADDED_TILE_KEYS] = updated.qsAddedTileKeys
        }
    }

    suspend fun updateMediaSessionPreferences(transform: (MediaSessionPreferences) -> MediaSessionPreferences) {
        context.magicMusicDataStore.edit { prefs ->
            val updated = transform(mapMediaSessionPreferences(prefs))
            prefs[Keys.PRIORITY_RULE] = updated.priorityRule.name
            prefs[Keys.PREFERRED_SERVICE_PKG] = updated.preferredServicePackage
            prefs[Keys.IGNORE_VIDEO_SESSIONS] = updated.ignoreVideoSessions
            prefs[Keys.DISABLED_PACKAGES] = updated.disabledPackages
        }
    }

    suspend fun updateThemePreferences(transform: (ThemePreferences) -> ThemePreferences) {
        context.magicMusicDataStore.edit { prefs ->
            val updated = transform(mapThemePreferences(prefs))
            prefs[Keys.USE_DYNAMIC_COLOR] = updated.useDynamicColor
            prefs[Keys.THEME_MODE] = updated.themeMode.name
            prefs[Keys.SURFACE_TONE_STYLE] = updated.surfaceToneStyle.name
            prefs[Keys.SEED_COLOR_ARGB] = updated.seedColorArgb
            prefs[Keys.PURE_AMOLED_BLACK] = updated.pureAmoledBlack
            prefs[Keys.COLOR_FROM_ARTWORK] = updated.colorFromArtwork
            prefs[Keys.REDUCE_ANIMATIONS] = updated.reduceAnimations
            prefs.remove(Keys.LEGACY_THEME_ID)
            prefs.remove(Keys.LEGACY_FINISH)
        }
    }

    suspend fun updateEqualizerConfig(transform: (EqualizerConfig) -> EqualizerConfig) {
        context.magicMusicDataStore.edit { prefs ->
            val updated = transform(mapEqualizerConfig(prefs))
            val updatedProfiles = updated.deviceProfiles.toMutableMap()
            updatedProfiles[updated.activeOutputCategory] = DeviceEqProfile(
                category = updated.activeOutputCategory,
                enabled = updated.enabled,
                presetIndex = updated.presetIndex,
                bandGainsDb = updated.bandGainsDb,
                bassBoostStrength = updated.bassBoostStrength,
                loudnessGainMb = updated.loudnessGainMb,
            )
            prefs[Keys.EQ_ENABLED] = updated.enabled
            prefs[Keys.EQ_PRESET_INDEX] = updated.presetIndex
            prefs[Keys.EQ_BANDS_CSV] = updated.bandGainsDb.joinToString(",")
            prefs[Keys.EQ_ADVANCED_MODE] = updated.isAdvancedMode
            prefs[Keys.EQ_HIRES_BUFFER] = updated.hiResBufferHigh
            prefs[Keys.EQ_BASS_BOOST] = updated.bassBoostStrength.coerceIn(0, 1000)
            prefs[Keys.EQ_LOUDNESS_GAIN] = updated.loudnessGainMb.coerceIn(0, 1000)
            prefs[Keys.HIRES_GAPLESS] = updated.gaplessEnabled
            prefs[Keys.HIRES_CROSSFADE_SEC] = updated.crossfadeSeconds.coerceIn(0, 12)
            prefs[Keys.HIRES_REPLAY_GAIN] = updated.replayGainEnabled
            prefs[Keys.HIRES_FLOAT_24BIT] = updated.float24BitOutput
            prefs[Keys.EQ_ACTIVE_OUTPUT_CATEGORY] = updated.activeOutputCategory.name
            prefs[Keys.EQ_DEVICE_PROFILES_JSON] = encodeDeviceEqProfiles(updatedProfiles)
        }
    }

    suspend fun switchEqualizerOutputCategory(category: AudioOutputCategory) {
        context.magicMusicDataStore.edit { prefs ->
            val cur = mapEqualizerConfig(prefs)
            if (cur.activeOutputCategory == category) return@edit
            val savedCurrent = DeviceEqProfile(
                category = cur.activeOutputCategory,
                enabled = cur.enabled,
                presetIndex = cur.presetIndex,
                bandGainsDb = cur.bandGainsDb,
                bassBoostStrength = cur.bassBoostStrength,
                loudnessGainMb = cur.loudnessGainMb,
            )
            val allProfiles = cur.deviceProfiles.toMutableMap()
            allProfiles[cur.activeOutputCategory] = savedCurrent
            val target = allProfiles[category] ?: DeviceEqProfile(category = category)
            prefs[Keys.EQ_ACTIVE_OUTPUT_CATEGORY] = category.name
            prefs[Keys.EQ_ENABLED] = target.enabled
            prefs[Keys.EQ_PRESET_INDEX] = target.presetIndex
            prefs[Keys.EQ_BANDS_CSV] = target.bandGainsDb.joinToString(",")
            prefs[Keys.EQ_BASS_BOOST] = target.bassBoostStrength
            prefs[Keys.EQ_LOUDNESS_GAIN] = target.loudnessGainMb
            prefs[Keys.EQ_DEVICE_PROFILES_JSON] = encodeDeviceEqProfiles(allProfiles)
        }
    }

    suspend fun setPreferredServicePackage(packageName: String) {
        updateMediaSessionPreferences { it.copy(preferredServicePackage = packageName) }
    }

    suspend fun recordTrackPlay(
        trackKey: String,
        title: String,
        artist: String,
        album: String,
        packageName: String,
        serviceId: String,
        durationMs: Long,
    ) {
        if (title.isBlank() || title == "Sin música activa") return
        context.magicMusicDataStore.edit { prefs ->
            val list = WidgetTemplateSerializer.decodeHistory(prefs[Keys.RECENT_TRACKS_JSON].orEmpty()).toMutableList()
            val idx = list.indexOfFirst { it.id == trackKey }
            val now = System.currentTimeMillis()
            val updatedEntry = if (idx >= 0) {
                val prev = list.removeAt(idx)
                prev.copy(
                    title = title,
                    artist = artist,
                    album = album,
                    packageName = packageName,
                    serviceId = serviceId,
                    lastPlayedMs = now,
                    playCount = prev.playCount + 1,
                    durationMs = durationMs.coerceAtLeast(prev.durationMs),
                )
            } else {
                TrackHistoryEntry(
                    id = trackKey,
                    title = title,
                    artist = artist,
                    album = album,
                    packageName = packageName,
                    serviceId = serviceId,
                    lastPlayedMs = now,
                    playCount = 1,
                    durationMs = durationMs,
                )
            }
            list.add(0, updatedEntry)
            prefs[Keys.RECENT_TRACKS_JSON] = WidgetTemplateSerializer.encodeHistory(list.take(30))
        }
    }

    suspend fun addListeningSeconds(seconds: Int) {
        if (seconds <= 0) return
        context.magicMusicDataStore.edit { prefs ->
            val prevSecs = prefs[Keys.STATS_TOTAL_SECONDS] ?: 0L
            val nextSecs = prevSecs + seconds
            prefs[Keys.STATS_TOTAL_SECONDS] = nextSecs

            val daily = parseDailyCsv(prefs[Keys.STATS_DAILY_MINUTES_CSV]).toMutableList()
            val dayIdx = currentDayOfWeekMonFirstIndex()
            // Add 1 minute every 60 accumulated seconds
            if ((nextSecs / 60L) > (prevSecs / 60L)) {
                val deltaMin = ((nextSecs / 60L) - (prevSecs / 60L)).toInt()
                daily[dayIdx] = (daily[dayIdx] + deltaMin).coerceAtLeast(0)
                prefs[Keys.STATS_DAILY_MINUTES_CSV] = daily.joinToString(",")
            }
        }
    }

    suspend fun toggleFavoriteTrackId(trackKey: String): Boolean {
        var added = false
        context.magicMusicDataStore.edit { prefs ->
            val cur = (prefs[Keys.FAVORITE_TRACK_IDS] ?: emptySet()).toMutableSet()
            added = if (cur.contains(trackKey)) {
                cur.remove(trackKey)
                false
            } else {
                cur.add(trackKey)
                true
            }
            prefs[Keys.FAVORITE_TRACK_IDS] = cur
        }
        return added
    }

    suspend fun addTrackToQuickPlaylist(trackKey: String): Boolean {
        var added = false
        context.magicMusicDataStore.edit { prefs ->
            val cur = (prefs[Keys.PLAYLIST_TRACK_IDS] ?: emptySet()).toMutableSet()
            added = cur.add(trackKey)
            prefs[Keys.PLAYLIST_TRACK_IDS] = cur
        }
        return added
    }

    suspend fun clearLocalStats() {
        context.magicMusicDataStore.edit { prefs ->
            prefs[Keys.STATS_TOTAL_SECONDS] = 0L
            prefs[Keys.STATS_DAILY_MINUTES_CSV] = "0,0,0,0,0,0,0"
            prefs[Keys.RECENT_TRACKS_JSON] = "[]"
        }
    }

    suspend fun exportFullBackupJson(): String {
        val prefs = context.magicMusicDataStore.data.first()
        val w = mapWidgetConfig(prefs)
        val t = mapThemePreferences(prefs)
        val l = mapLockConfig(prefs)
        val templatesJson = WidgetTemplateSerializer.encodeTemplates(mapWidgetTemplates(prefs))
        val activeTpl = WidgetTemplate(
            id = "active_config",
            name = "Configuración Activa",
            format = WidgetFormat.CARD_4X2,
            config = w,
        )
        return buildString {
            append("MagicMusicBackup|v1|")
            append("themeMode=").append(t.themeMode.name).append(";")
            append("surfaceTone=").append(t.surfaceToneStyle.name).append(";")
            append("dynamic=").append(t.useDynamicColor).append(";")
            append("seed=").append(t.seedColorArgb).append(";")
            append("amoled=").append(t.pureAmoledBlack).append(";")
            append("artworkTint=").append(t.colorFromArtwork).append(";")
            append("bridge=").append(l.bridgeEnabled).append(";")
            append("lockVisible=").append(l.showOnLockScreen).append(";")
            append("lockArt=").append(l.showArtworkOnLockScreen).append(";")
            append("activeWidget=").append(WidgetTemplateSerializer.encodeTemplate(activeTpl)).append(";")
            append("templates=").append(templatesJson)
        }
    }

    suspend fun importFullBackupJson(payload: String): Boolean {
        val trimmed = payload.trim()
        if (trimmed.startsWith("[") || trimmed.startsWith("{")) {
            return importWidgetTemplatesJson(trimmed) > 0
        }
        if (!trimmed.startsWith("MagicMusicBackup|v1|")) return false
        val body = trimmed.removePrefix("MagicMusicBackup|v1|")
        val parts = body.split(";").mapNotNull { kv ->
            val idx = kv.indexOf('=')
            if (idx <= 0) null else kv.substring(0, idx) to kv.substring(idx + 1)
        }.toMap()

        parts["activeWidget"]?.let { activeJson ->
            WidgetTemplateSerializer.decodeTemplate(activeJson)?.let { tpl ->
                updateWidgetConfig { tpl.config }
            }
        }
        parts["templates"]?.let { tJson ->
            if (tJson.isNotBlank()) importWidgetTemplatesJson(tJson)
        }
        updateThemePreferences { cur ->
            cur.copy(
                themeMode = enumValueOrDefault(parts["themeMode"], cur.themeMode),
                surfaceToneStyle = enumValueOrDefault(parts["surfaceTone"], cur.surfaceToneStyle),
                useDynamicColor = parts["dynamic"]?.toBooleanStrictOrNull() ?: cur.useDynamicColor,
                seedColorArgb = parts["seed"]?.toLongOrNull() ?: cur.seedColorArgb,
                pureAmoledBlack = parts["amoled"]?.toBooleanStrictOrNull() ?: cur.pureAmoledBlack,
                colorFromArtwork = parts["artworkTint"]?.toBooleanStrictOrNull() ?: cur.colorFromArtwork,
            )
        }
        updateLockAndQsConfig { cur ->
            cur.copy(
                bridgeEnabled = parts["bridge"]?.toBooleanStrictOrNull() ?: cur.bridgeEnabled,
                showOnLockScreen = parts["lockVisible"]?.toBooleanStrictOrNull() ?: cur.showOnLockScreen,
                showArtworkOnLockScreen = parts["lockArt"]?.toBooleanStrictOrNull() ?: cur.showArtworkOnLockScreen,
            )
        }
        return true
    }

    private fun mapWidgetConfig(prefs: Preferences): WidgetConfig {
        val style = migrateWidgetStyle(prefs[Keys.WIDGET_STYLE])
        val left = enumValueOrDefault(prefs[Keys.LEFT_CUSTOM_ACTION], CustomControlAction.REWIND_15)
        val right = enumValueOrDefault(prefs[Keys.RIGHT_CUSTOM_ACTION], CustomControlAction.FORWARD_15)
        val showHeader = prefs[Keys.SHOW_COMPACT_HEADER]
            ?: prefs[Keys.LEGACY_SHOW_CAPSULE_HEADER]
            ?: true
        return WidgetConfig(
            style = style,
            cornerRadiusDp = prefs[Keys.CORNER_RADIUS] ?: 24,
            glassOpacity = prefs[Keys.GLASS_OPACITY] ?: 0.94f,
            showProgressBar = prefs[Keys.SHOW_PROGRESS] ?: true,
            showServiceBadge = prefs[Keys.SHOW_SERVICE_BADGE] ?: true,
            showCompactHeader = showHeader,
            leftCustomAction = left,
            rightCustomAction = right,
        )
    }

    private fun mapWidgetTemplates(prefs: Preferences): List<WidgetTemplate> {
        val raw = prefs[Keys.WIDGET_TEMPLATES_JSON]
        if (raw.isNullOrBlank()) {
            return defaultWidgetTemplates()
        }
        val decoded = WidgetTemplateSerializer.decodeTemplates(raw)
        return if (decoded.isEmpty()) defaultWidgetTemplates() else decoded
    }

    private fun mapLockConfig(prefs: Preferences): LockAndQsConfig {
        val a1 = enumValueOrDefault(prefs[Keys.LOCK_ACTION_1], CustomControlAction.REWIND_15)
        val a2 = enumValueOrDefault(prefs[Keys.LOCK_ACTION_2], CustomControlAction.FORWARD_15)
        val tileMode = enumValueOrDefault(prefs[Keys.QS_TILE_MODE], QsTileMode.PLAY_PAUSE)
        val privacy = enumValueOrDefault(prefs[Keys.LOCK_PRIVACY_LEVEL], LockPrivacyLevel.PUBLIC)
        val addedKeys = (prefs[Keys.QS_ADDED_TILE_KEYS] ?: emptySet()).toMutableSet()
        if (prefs[Keys.QS_PLAY_TILE_ADDED] == true) addedKeys.add(QuickTileType.PLAY_PAUSE.key)
        if (prefs[Keys.QS_NEXT_TILE_ADDED] == true) addedKeys.add(QuickTileType.NEXT.key)
        val compactRaw = prefs[Keys.COMPACT_ACTION_INDICES_CSV]
        val compactIndices = if (compactRaw.isNullOrBlank()) {
            listOf(1, 2, 3)
        } else {
            sanitizeCompactIndices(compactRaw.split(",").mapNotNull { it.toIntOrNull() })
        }
        return LockAndQsConfig(
            bridgeEnabled = prefs[Keys.BRIDGE_ENABLED] ?: true,
            showOnLockScreen = prefs[Keys.SHOW_ON_LOCK_SCREEN] ?: true,
            showArtworkOnLockScreen = prefs[Keys.SHOW_ARTWORK_ON_LOCK] ?: true,
            lockPrivacyLevel = privacy,
            lockAutoCloseSeconds = prefs[Keys.LOCK_AUTO_CLOSE_SECONDS] ?: 30,
            compactActionIndices = compactIndices,
            notifyChannelGuideShown = prefs[Keys.NOTIFY_CHANNEL_GUIDE_SHOWN] ?: false,
            requireUnlockToOpenApp = prefs[Keys.REQ_UNLOCK_OPEN_APP] ?: true,
            requireUnlockToSwitchSource = prefs[Keys.REQ_UNLOCK_SWITCH] ?: false,
            requireUnlockForCustomActions = prefs[Keys.REQ_UNLOCK_CUSTOM] ?: false,
            lockScreenAction1 = a1,
            lockScreenAction2 = a2,
            qsTileMode = tileMode,
            preferHighContrastDeck = prefs[Keys.HIGH_CONTRAST_DECK] ?: true,
            qsAddedTileKeys = addedKeys,
        )
    }

    private fun mapMediaSessionPreferences(prefs: Preferences): MediaSessionPreferences {
        val rule = enumValueOrDefault(prefs[Keys.PRIORITY_RULE], SessionPriorityRule.PLAYING_FIRST)
        return MediaSessionPreferences(
            priorityRule = rule,
            preferredServicePackage = prefs[Keys.PREFERRED_SERVICE_PKG] ?: MusicServiceApp.SPOTIFY.packageName,
            ignoreVideoSessions = prefs[Keys.IGNORE_VIDEO_SESSIONS] ?: false,
            disabledPackages = prefs[Keys.DISABLED_PACKAGES] ?: emptySet(),
        )
    }

    private fun mapThemePreferences(prefs: Preferences): ThemePreferences {
        val legacyThemeId = prefs[Keys.LEGACY_THEME_ID]
        val legacyFinish = prefs[Keys.LEGACY_FINISH]
        val explicitDynamic = prefs[Keys.USE_DYNAMIC_COLOR]

        val themeMode = migrateThemeMode(
            rawMode = prefs[Keys.THEME_MODE],
            legacyThemeId = legacyThemeId,
            legacyFinish = legacyFinish,
        )
        val surfaceTone = migrateSurfaceToneStyle(
            rawSurfaceTone = prefs[Keys.SURFACE_TONE_STYLE],
            themeMode = themeMode,
        )
        return ThemePreferences(
            useDynamicColor = migrateUseDynamicColor(explicitDynamic, legacyThemeId),
            themeMode = themeMode,
            surfaceToneStyle = surfaceTone,
            seedColorArgb = prefs[Keys.SEED_COLOR_ARGB] ?: DEFAULT_VIOLET_SEED_ARGB,
            pureAmoledBlack = prefs[Keys.PURE_AMOLED_BLACK] ?: false,
            colorFromArtwork = prefs[Keys.COLOR_FROM_ARTWORK] ?: true,
            reduceAnimations = prefs[Keys.REDUCE_ANIMATIONS] ?: false,
        )
    }

    private fun mapEqualizerConfig(prefs: Preferences): EqualizerConfig {
        val bandsRaw = prefs[Keys.EQ_BANDS_CSV].orEmpty()
        val parsedBands = bandsRaw.split(",")
            .mapNotNull { it.toFloatOrNull()?.coerceIn(-12f, 12f) }
        val finalBands = if (parsedBands.size == 5) parsedBands else listOf(0f, 0f, 0f, 0f, 0f)
        val activeCategory = enumValueOrDefault(
            prefs[Keys.EQ_ACTIVE_OUTPUT_CATEGORY],
            AudioOutputCategory.SPEAKER,
        )
        val profiles = decodeDeviceEqProfiles(prefs[Keys.EQ_DEVICE_PROFILES_JSON].orEmpty())
        return EqualizerConfig(
            enabled = prefs[Keys.EQ_ENABLED] ?: true,
            presetIndex = (prefs[Keys.EQ_PRESET_INDEX] ?: 0).coerceAtLeast(0),
            bandGainsDb = finalBands,
            isAdvancedMode = prefs[Keys.EQ_ADVANCED_MODE] ?: false,
            hiResBufferHigh = prefs[Keys.EQ_HIRES_BUFFER] ?: true,
            bassBoostStrength = (prefs[Keys.EQ_BASS_BOOST] ?: 0).coerceIn(0, 1000),
            loudnessGainMb = (prefs[Keys.EQ_LOUDNESS_GAIN] ?: 0).coerceIn(0, 1000),
            gaplessEnabled = prefs[Keys.HIRES_GAPLESS] ?: true,
            crossfadeSeconds = (prefs[Keys.HIRES_CROSSFADE_SEC] ?: 0).coerceIn(0, 12),
            replayGainEnabled = prefs[Keys.HIRES_REPLAY_GAIN] ?: true,
            float24BitOutput = prefs[Keys.HIRES_FLOAT_24BIT] ?: true,
            activeOutputCategory = activeCategory,
            deviceProfiles = profiles,
        )
    }

    private fun sanitizeCompactIndices(raw: List<Int>): List<Int> {
        val clean = raw.filter { it in 0..4 }.distinct().sorted().take(3)
        return clean.ifEmpty { listOf(1, 2, 3) }
    }

    private fun encodeDeviceEqProfiles(profiles: Map<AudioOutputCategory, DeviceEqProfile>): String {
        return profiles.values.joinToString("|") { p ->
            listOf(
                p.category.name,
                p.enabled.toString(),
                p.presetIndex.toString(),
                p.bandGainsDb.joinToString(","),
                p.bassBoostStrength.toString(),
                p.loudnessGainMb.toString(),
            ).joinToString(";")
        }
    }

    private fun decodeDeviceEqProfiles(blob: String): Map<AudioOutputCategory, DeviceEqProfile> {
        if (blob.isBlank()) return emptyMap()
        return blob.split("|").mapNotNull { record ->
            val cols = record.split(";")
            if (cols.size < 6) return@mapNotNull null
            val cat = runCatching { AudioOutputCategory.valueOf(cols[0]) }.getOrNull() ?: return@mapNotNull null
            val bands = cols[3].split(",").mapNotNull { it.toFloatOrNull()?.coerceIn(-12f, 12f) }
            val fixedBands = if (bands.size == 5) bands else listOf(0f, 0f, 0f, 0f, 0f)
            cat to DeviceEqProfile(
                category = cat,
                enabled = cols[1].toBooleanStrictOrNull() ?: true,
                presetIndex = cols[2].toIntOrNull() ?: 0,
                bandGainsDb = fixedBands,
                bassBoostStrength = (cols[4].toIntOrNull() ?: 0).coerceIn(0, 1000),
                loudnessGainMb = (cols[5].toIntOrNull() ?: 0).coerceIn(0, 1000),
            )
        }.toMap()
    }

    private fun mapLocalStats(prefs: Preferences): LocalListeningStats {
        val totalMin = ((prefs[Keys.STATS_TOTAL_SECONDS] ?: 0L) / 60L).toInt()
        val daily = parseDailyCsv(prefs[Keys.STATS_DAILY_MINUTES_CSV])
        val recent = WidgetTemplateSerializer.decodeHistory(prefs[Keys.RECENT_TRACKS_JSON].orEmpty())
        return LocalListeningStats(
            totalMinutes = totalMin,
            dailyMinutes = daily,
            recentTracks = recent,
            favoriteTrackIds = prefs[Keys.FAVORITE_TRACK_IDS] ?: emptySet(),
            playlistTrackIds = prefs[Keys.PLAYLIST_TRACK_IDS] ?: emptySet(),
        )
    }

    private fun parseDailyCsv(csv: String?): List<Int> {
        val parsed = csv.orEmpty().split(",").mapNotNull { it.toIntOrNull()?.coerceAtLeast(0) }
        return if (parsed.size == 7) parsed else listOf(0, 0, 0, 0, 0, 0, 0)
    }

    private fun currentDayOfWeekMonFirstIndex(): Int {
        val cal = Calendar.getInstance()
        return when (cal.get(Calendar.DAY_OF_WEEK)) {
            Calendar.MONDAY -> 0
            Calendar.TUESDAY -> 1
            Calendar.WEDNESDAY -> 2
            Calendar.THURSDAY -> 3
            Calendar.FRIDAY -> 4
            Calendar.SATURDAY -> 5
            else -> 6
        }
    }

    private inline fun <reified T : Enum<T>> enumValueOrDefault(raw: String?, default: T): T {
        if (raw.isNullOrBlank()) return default
        return runCatching { enumValueOf<T>(raw) }.getOrDefault(default)
    }

    companion object {
        fun defaultWidgetTemplates(): List<WidgetTemplate> = listOf(
            WidgetTemplate(
                id = "preset_tonal_card",
                name = "Violeta Tonal 4×2",
                format = WidgetFormat.CARD_4X2,
                config = WidgetConfig(style = WidgetStyle.TONAL, cornerRadiusDp = 24, glassOpacity = 0.96f),
            ),
            WidgetTemplate(
                id = "preset_artwork_capsule",
                name = "Carátula Dinámica 4×1",
                format = WidgetFormat.CAPSULE_4X1,
                config = WidgetConfig(
                    style = WidgetStyle.ARTWORK,
                    cornerRadiusDp = 28,
                    leftCustomAction = CustomControlAction.LIKE_TRACK,
                    rightCustomAction = CustomControlAction.SWITCH_SOURCE,
                ),
            ),
            WidgetTemplate(
                id = "preset_transparent_square",
                name = "Cristal Limpio 2×2",
                format = WidgetFormat.SQUARE_2X2,
                config = WidgetConfig(style = WidgetStyle.TRANSPARENT, cornerRadiusDp = 22, glassOpacity = 0.78f),
            ),
        )

        fun migrateWidgetStyle(raw: String?): WidgetStyle {
            if (raw.isNullOrBlank()) return WidgetStyle.TONAL
            return when (raw) {
                "CAPSULE_DARK" -> WidgetStyle.SURFACE
                "FROSTED_GLASS" -> WidgetStyle.TONAL
                "PORCELAIN_LIGHT" -> WidgetStyle.ARTWORK
                "DYNAMIC_NEON" -> WidgetStyle.TRANSPARENT
                else -> runCatching { WidgetStyle.valueOf(raw) }.getOrDefault(WidgetStyle.TONAL)
            }
        }

        fun migrateUseDynamicColor(
            explicitUseDynamic: Boolean?,
            legacyThemeId: String?,
        ): Boolean {
            if (explicitUseDynamic != null) return explicitUseDynamic
            return legacyThemeId == "MATERIAL_3_DYNAMIC"
        }

        fun migrateThemeMode(
            rawMode: String?,
            legacyThemeId: String?,
            legacyFinish: String?,
        ): ThemeMode {
            if (!rawMode.isNullOrBlank()) {
                val parsed = runCatching { ThemeMode.valueOf(rawMode) }.getOrNull()
                if (parsed != null) {
                    if (legacyThemeId == "MAGIC_OS_10" && legacyFinish == "PORCELANA_CLARA") {
                        return ThemeMode.LIGHT
                    }
                    return parsed
                }
            }
            return if (legacyThemeId == "MAGIC_OS_10" && legacyFinish == "PORCELANA_CLARA") {
                ThemeMode.LIGHT
            } else {
                ThemeMode.DARK
            }
        }

        fun migrateSurfaceToneStyle(
            rawSurfaceTone: String?,
            themeMode: ThemeMode,
        ): SurfaceToneStyle {
            if (!rawSurfaceTone.isNullOrBlank()) {
                val parsed = runCatching { SurfaceToneStyle.valueOf(rawSurfaceTone) }.getOrNull()
                if (parsed != null) return parsed
            }
            return if (themeMode == ThemeMode.LIGHT) SurfaceToneStyle.LIGHT else SurfaceToneStyle.TONAL
        }
    }
}
