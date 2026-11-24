package com.example.ui

import android.app.Application
import android.content.ComponentName
import android.content.Intent
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.R
import com.example.core.compat.AndroidVersionCompat
import com.example.core.compat.CompatActionResult
import com.example.core.compat.OemBatteryGuide
import com.example.core.compat.OemCompat
import com.example.core.media.HealthCheckId
import com.example.core.media.ListenerConnectionState
import com.example.core.media.MediaRepository
import com.example.core.media.PlayerState
import com.example.core.media.SleepTimerState
import com.example.core.media.SystemHealthSummary
import com.example.data.AudioOutputDevice
import com.example.data.CustomControlAction
import com.example.data.EqualizerConfig
import com.example.data.InstalledServiceStatus
import com.example.data.LocalListeningStats
import com.example.data.LockAndQsConfig
import com.example.data.MediaSessionPreferences
import com.example.data.PreferencesRepository
import com.example.data.QsTileMode
import com.example.data.QuickTileType
import com.example.data.LockPrivacyLevel
import com.example.data.SessionPriorityRule
import com.example.data.UnifiedMediaState
import com.example.data.WidgetConfig
import com.example.data.WidgetFormat
import com.example.data.WidgetStyle
import com.example.data.WidgetTemplate
import com.example.designsystem.ArtworkPaletteColors
import com.example.designsystem.ArtworkPaletteExtractor
import com.example.designsystem.SurfaceToneStyle
import com.example.designsystem.ThemeMode
import com.example.designsystem.ThemePreferences
import com.example.designsystem.VioletSeedColor
import com.example.media.MagicMediaBridgeService
import com.example.quicksettings.LockScreenDeckActivity
import com.example.quicksettings.MagicNextTrackTileService
import com.example.quicksettings.MagicPlayPauseTileService
import com.example.ui.components.EqualizerPreset
import com.example.widget.MagicWidgetCapsuleProvider
import com.example.widget.MagicWidgetCardProvider
import com.example.widget.MagicWidgetMiniProvider
import com.example.widget.MagicWidgetSquareProvider
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.mapLatest
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class MagicMusicUiState(
    val playerState: PlayerState = PlayerState(),
    val media: UnifiedMediaState = UnifiedMediaState(),
    val widgetConfig: WidgetConfig = WidgetConfig(),
    val lockAndQsConfig: LockAndQsConfig = LockAndQsConfig(),
    val sessionPreferences: MediaSessionPreferences = MediaSessionPreferences(),
    val equalizerConfig: EqualizerConfig = EqualizerConfig(),
    val listeningStats: LocalListeningStats = LocalListeningStats(),
    val widgetTemplates: List<WidgetTemplate> = emptyList(),
    val sleepTimer: SleepTimerState = SleepTimerState(),
    val systemHealth: SystemHealthSummary = SystemHealthSummary(),
    val audioOutputs: List<AudioOutputDevice> = emptyList(),
    val hasListenerPermission: Boolean = false,
    val hasPostNotificationsPermission: Boolean = true,
    val onboardingCompleted: Boolean = false,
    val listenerState: ListenerConnectionState = ListenerConnectionState.PERMISSION_MISSING,
    val installedServices: List<InstalledServiceStatus> = emptyList(),
    val oemBatteryGuide: OemBatteryGuide? = null,
    val bridgeRunning: Boolean = false,
    val searchQuery: String = "",
    val feedbackMessage: String? = null,
)

class MagicMusicViewModel(application: Application) : AndroidViewModel(application) {

    private val prefsRepo = PreferencesRepository(application)
    private val _feedbackMessage = MutableStateFlow<String?>(null)
    private val _searchQuery = MutableStateFlow("")

    init {
        MediaRepository.init(application)
    }

    // Isolated from media ticks so theme switches never re-emit playback/service state
    val themePreferences: StateFlow<ThemePreferences> = prefsRepo.themePreferencesFlow.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = ThemePreferences(),
    )

    @Suppress("OPT_IN_USAGE")
    val artworkColors: StateFlow<ArtworkPaletteColors> = MediaRepository.mediaState
        .mapLatest { media ->
            ArtworkPaletteExtractor.extractCached(
                cacheKey = "${media.packageName}_${media.album}",
                bitmap = media.albumArt,
                fallbackColor = if (media.hasActiveMedia) media.service.accentColor else VioletSeedColor,
            )
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = ArtworkPaletteExtractor.deriveFromFallbackSeed(VioletSeedColor),
        )

    private data class SystemSnapshot(
        val hasListener: Boolean,
        val hasNotifications: Boolean,
        val services: List<InstalledServiceStatus>,
        val oemGuide: OemBatteryGuide?,
        val bridgeRunning: Boolean,
        val systemHealth: SystemHealthSummary,
        val audioOutputs: List<AudioOutputDevice>,
        val sleepTimer: SleepTimerState,
        val searchQuery: String,
        val feedback: String?,
    )

    private data class ConfigSnapshot(
        val widgetConfig: WidgetConfig,
        val lockConfig: LockAndQsConfig,
        val sessionPreferences: MediaSessionPreferences,
        val equalizerConfig: EqualizerConfig,
        val listeningStats: LocalListeningStats,
        val widgetTemplates: List<WidgetTemplate>,
        val onboardingCompleted: Boolean,
    )

    private val permissionsAndServicesFlow = combine(
        MediaRepository.hasListenerPermission,
        MediaRepository.hasPostNotificationsPermission,
        MediaRepository.installedServices,
        MediaRepository.oemBatteryGuide,
        MediaRepository.bridgeActive,
    ) { hasListener, hasNotif, services, oemGuide, bridge ->
        listOf(hasListener, hasNotif, services, oemGuide, bridge)
    }

    private val extrasFlow = combine(
        MediaRepository.systemHealth,
        MediaRepository.audioOutputs,
        MediaRepository.sleepTimerState,
        _searchQuery,
        _feedbackMessage,
    ) { health, outputs, timer, query, feedback ->
        listOf(health, outputs, timer, query, feedback)
    }

    @Suppress("UNCHECKED_CAST")
    private val systemSnapshotFlow = combine(
        permissionsAndServicesFlow,
        extrasFlow,
    ) { base, ext ->
        SystemSnapshot(
            hasListener = base[0] as Boolean,
            hasNotifications = base[1] as Boolean,
            services = base[2] as List<InstalledServiceStatus>,
            oemGuide = base[3] as OemBatteryGuide?,
            bridgeRunning = base[4] as Boolean,
            systemHealth = ext[0] as SystemHealthSummary,
            audioOutputs = ext[1] as List<AudioOutputDevice>,
            sleepTimer = ext[2] as SleepTimerState,
            searchQuery = ext[3] as String,
            feedback = ext[4] as String?,
        )
    }

    private val configBaseFlow = combine(
        prefsRepo.widgetConfigFlow,
        prefsRepo.lockAndQsConfigFlow,
        prefsRepo.mediaSessionPreferencesFlow,
        prefsRepo.equalizerConfigFlow,
    ) { w, l, s, eq ->
        listOf(w, l, s, eq)
    }

    private val configUserFlow = combine(
        prefsRepo.localStatsFlow,
        prefsRepo.widgetTemplatesFlow,
        prefsRepo.onboardingCompletedFlow,
    ) { stats, templates, onboardingDone ->
        Triple(stats, templates, onboardingDone)
    }

    private val configSnapshotFlow = combine(
        configBaseFlow,
        configUserFlow,
    ) { base, usr ->
        ConfigSnapshot(
            widgetConfig = base[0] as WidgetConfig,
            lockConfig = base[1] as LockAndQsConfig,
            sessionPreferences = base[2] as MediaSessionPreferences,
            equalizerConfig = base[3] as EqualizerConfig,
            listeningStats = usr.first,
            widgetTemplates = usr.second,
            onboardingCompleted = usr.third,
        )
    }

    val uiState: StateFlow<MagicMusicUiState> = combine(
        MediaRepository.playerState,
        MediaRepository.mediaState,
        configSnapshotFlow,
        systemSnapshotFlow,
    ) { player, media, cfg, sys ->
        MagicMusicUiState(
            playerState = player,
            media = media,
            widgetConfig = cfg.widgetConfig,
            lockAndQsConfig = cfg.lockConfig,
            sessionPreferences = cfg.sessionPreferences,
            equalizerConfig = cfg.equalizerConfig,
            listeningStats = cfg.listeningStats,
            widgetTemplates = cfg.widgetTemplates,
            sleepTimer = sys.sleepTimer,
            systemHealth = sys.systemHealth,
            audioOutputs = sys.audioOutputs,
            hasListenerPermission = sys.hasListener,
            hasPostNotificationsPermission = sys.hasNotifications,
            onboardingCompleted = cfg.onboardingCompleted,
            listenerState = player.listenerState,
            installedServices = sys.services,
            oemBatteryGuide = sys.oemGuide,
            bridgeRunning = sys.bridgeRunning,
            searchQuery = sys.searchQuery,
            feedbackMessage = sys.feedback ?: player.lastError,
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = MagicMusicUiState(),
    )

    fun refreshSystemState() {
        MediaRepository.refreshPermissionsAndServices()
        MediaRepository.refreshAudioOutputs()
    }

    fun clearFeedback() {
        _feedbackMessage.value = null
        MediaRepository.clearError()
    }

    fun showFeedback(msg: String) {
        _feedbackMessage.value = msg
    }

    fun setSearchQuery(query: String) {
        _searchQuery.value = query
    }

    fun completeOnboarding() {
        viewModelScope.launch {
            prefsRepo.setOnboardingCompleted(true)
        }
    }

    // Material 3 Theme Preferences (real-time updates)
    fun setUseDynamicColor(enabled: Boolean) {
        viewModelScope.launch {
            prefsRepo.updateThemePreferences { it.copy(useDynamicColor = enabled) }
        }
    }

    fun setThemeMode(themeMode: ThemeMode) {
        viewModelScope.launch {
            prefsRepo.updateThemePreferences {
                val syncedTone = when (themeMode) {
                    ThemeMode.LIGHT -> SurfaceToneStyle.LIGHT
                    ThemeMode.DARK -> if (it.surfaceToneStyle == SurfaceToneStyle.LIGHT) SurfaceToneStyle.TONAL else it.surfaceToneStyle
                    ThemeMode.SYSTEM -> it.surfaceToneStyle
                }
                it.copy(themeMode = themeMode, surfaceToneStyle = syncedTone)
            }
        }
    }

    fun setSurfaceToneStyle(style: SurfaceToneStyle) {
        viewModelScope.launch {
            prefsRepo.updateThemePreferences {
                val syncedMode = when (style) {
                    SurfaceToneStyle.LIGHT -> ThemeMode.LIGHT
                    SurfaceToneStyle.DARK, SurfaceToneStyle.TONAL -> ThemeMode.DARK
                }
                it.copy(surfaceToneStyle = style, themeMode = syncedMode)
            }
        }
    }

    fun setSeedColor(argb: Long) {
        viewModelScope.launch {
            prefsRepo.updateThemePreferences {
                it.copy(
                    useDynamicColor = false,
                    seedColorArgb = argb,
                )
            }
        }
    }

    fun setPureAmoledBlack(enabled: Boolean) {
        viewModelScope.launch {
            prefsRepo.updateThemePreferences { it.copy(pureAmoledBlack = enabled) }
        }
    }

    fun setColorFromArtwork(enabled: Boolean) {
        viewModelScope.launch {
            prefsRepo.updateThemePreferences { it.copy(colorFromArtwork = enabled) }
        }
    }

    fun setReduceAnimations(enabled: Boolean) {
        viewModelScope.launch {
            prefsRepo.updateThemePreferences { it.copy(reduceAnimations = enabled) }
        }
    }

    // Playback controls
    fun playPause() = MediaRepository.playPause()
    fun next() = MediaRepository.next()
    fun previous() = MediaRepository.previous()
    fun seekTo(posMs: Long) = MediaRepository.seekTo(posMs)
    fun seekBy(deltaMs: Long) = MediaRepository.seekBy(deltaMs)
    fun selectSession(pkg: String) = MediaRepository.selectSession(pkg)
    fun toggleFavorite() = MediaRepository.toggleFavorite()
    fun toggleShuffle() = MediaRepository.toggleShuffle()
    fun toggleRepeat() = MediaRepository.toggleRepeat()
    fun skipToQueueItem(queueId: Long) = MediaRepository.skipToQueueItem(queueId)
    fun reorderLocalQueue(fromIndex: Int, toIndex: Int) = MediaRepository.reorderLocalQueue(fromIndex, toIndex)
    fun executeCustomAction(action: CustomControlAction) = MediaRepository.executeCustomAction(action)
    fun setVolumePercent(percent: Int) = MediaRepository.setMusicVolumePercent(percent)

    fun addTrackToQuickPlaylist() {
        val m = uiState.value.media
        if (!m.hasActiveMedia) return
        viewModelScope.launch {
            val added = prefsRepo.addTrackToQuickPlaylist(m.trackKey)
            _feedbackMessage.value = if (added) {
                "Pista guardada en tu lista rápida"
            } else {
                "La pista ya estaba en tu lista rápida"
            }
        }
    }

    // Sleep timer
    fun startSleepTimerCountdown(minutes: Int, fadeOut: Boolean) {
        MediaRepository.startSleepTimerCountdown(minutes, fadeOut)
        _feedbackMessage.value = "Temporizador programado para $minutes min"
    }

    fun startSleepTimerEndOfTrack(fadeOut: Boolean) {
        MediaRepository.startSleepTimerEndOfTrack(fadeOut)
        _feedbackMessage.value = "Se detendrá al terminar esta pista"
    }

    fun setSleepTimerFadeOut(enabled: Boolean) = MediaRepository.setSleepTimerFadeOut(enabled)

    fun cancelSleepTimer() {
        MediaRepository.cancelSleepTimer()
        _feedbackMessage.value = "Temporizador desactivado"
    }

    // Audio outputs & Equalizer
    fun refreshAudioOutputs() = MediaRepository.refreshAudioOutputs()

    fun selectAudioOutput(device: AudioOutputDevice) {
        val ok = MediaRepository.selectAudioOutput(device)
        _feedbackMessage.value = if (ok) {
            "Salida ajustada: ${device.name}"
        } else {
            "Abre el selector de salida del sistema para conectar tu dispositivo"
        }
    }

    fun setEqualizerEnabled(enabled: Boolean) {
        viewModelScope.launch {
            prefsRepo.updateEqualizerConfig { it.copy(enabled = enabled) }
        }
    }

    fun setEqualizerPreset(presetIndex: Int, preset: EqualizerPreset) {
        viewModelScope.launch {
            prefsRepo.updateEqualizerConfig {
                it.copy(
                    presetIndex = presetIndex,
                    bandGainsDb = preset.gains,
                )
            }
        }
    }

    fun setEqualizerBand(bandIndex: Int, gainDb: Float) {
        viewModelScope.launch {
            prefsRepo.updateEqualizerConfig { cur ->
                val list = cur.bandGainsDb.toMutableList()
                while (list.size < 5) list.add(0f)
                if (bandIndex in list.indices) {
                    list[bandIndex] = gainDb.coerceIn(-12f, 12f)
                }
                cur.copy(bandGainsDb = list)
            }
        }
    }

    fun setEqualizerAdvanced(advanced: Boolean) {
        viewModelScope.launch {
            prefsRepo.updateEqualizerConfig { it.copy(isAdvancedMode = advanced) }
        }
    }

    fun setHiResBufferHigh(high: Boolean) {
        viewModelScope.launch {
            prefsRepo.updateEqualizerConfig { it.copy(hiResBufferHigh = high) }
        }
    }

    fun clearLocalStats() {
        viewModelScope.launch {
            prefsRepo.clearLocalStats()
            _feedbackMessage.value = "Historial y estadísticas locales borrados"
        }
    }

    fun toggleLocalAudioEngine() {
        val ctx = getApplication<Application>()
        val local = MediaRepository.localEngine.state.value
        if (local.isActive && local.isPlaying) {
            MediaRepository.localEngine.pause()
            _feedbackMessage.value = "Motor local pausado"
        } else {
            MediaRepository.localEngine.startOrResume()
            MagicMediaBridgeService.startOrUpdate(ctx)
            _feedbackMessage.value = "Motor Hi-Res activo • Listo para probar widgets y pantalla de bloqueo"
        }
        MediaRepository.rebuildState()
    }

    // Session priority & filtering preferences
    fun setSessionPriorityRule(rule: SessionPriorityRule) {
        viewModelScope.launch {
            prefsRepo.updateMediaSessionPreferences { it.copy(priorityRule = rule) }
        }
    }

    fun setPreferredServicePackage(packageName: String) {
        viewModelScope.launch {
            prefsRepo.setPreferredServicePackage(packageName)
            _feedbackMessage.value = "Reproductor preferido actualizado"
        }
    }

    fun toggleIgnoreVideoSessions(ignore: Boolean) {
        viewModelScope.launch {
            prefsRepo.updateMediaSessionPreferences { it.copy(ignoreVideoSessions = ignore) }
        }
    }

    fun toggleManagedPackageEnabled(packageName: String, enabled: Boolean) {
        viewModelScope.launch {
            prefsRepo.updateMediaSessionPreferences { prefs ->
                val updated = prefs.disabledPackages.toMutableSet()
                if (enabled) updated.remove(packageName) else updated.add(packageName)
                prefs.copy(disabledPackages = updated)
            }
        }
    }

    // Widget studio & template mutations
    fun setWidgetStyle(style: WidgetStyle) {
        viewModelScope.launch {
            prefsRepo.updateWidgetConfig { it.copy(style = style) }
        }
    }

    fun setCornerRadius(dp: Int) {
        viewModelScope.launch {
            prefsRepo.updateWidgetConfig { it.copy(cornerRadiusDp = dp.coerceIn(12, 32)) }
        }
    }

    fun setGlassOpacity(opacity: Float) {
        viewModelScope.launch {
            prefsRepo.updateWidgetConfig { it.copy(glassOpacity = opacity.coerceIn(0.45f, 1.0f)) }
        }
    }

    fun toggleShowProgressBar(show: Boolean) {
        viewModelScope.launch {
            prefsRepo.updateWidgetConfig { it.copy(showProgressBar = show) }
        }
    }

    fun toggleShowServiceBadge(show: Boolean) {
        viewModelScope.launch {
            prefsRepo.updateWidgetConfig { it.copy(showServiceBadge = show) }
        }
    }

    fun toggleCompactHeader(show: Boolean) {
        viewModelScope.launch {
            prefsRepo.updateWidgetConfig { it.copy(showCompactHeader = show) }
        }
    }

    fun setLeftCustomAction(action: CustomControlAction) {
        viewModelScope.launch {
            prefsRepo.updateWidgetConfig { it.copy(leftCustomAction = action) }
        }
    }

    fun setRightCustomAction(action: CustomControlAction) {
        viewModelScope.launch {
            prefsRepo.updateWidgetConfig { it.copy(rightCustomAction = action) }
        }
    }

    fun saveWidgetTemplate(name: String, format: WidgetFormat) {
        viewModelScope.launch {
            prefsRepo.saveWidgetTemplate(name, format, uiState.value.widgetConfig)
            _feedbackMessage.value = "Plantilla guardada"
        }
    }

    fun duplicateWidgetTemplate(templateId: String) {
        viewModelScope.launch {
            prefsRepo.duplicateWidgetTemplate(templateId)
            _feedbackMessage.value = "Plantilla duplicada"
        }
    }

    fun deleteWidgetTemplate(templateId: String) {
        viewModelScope.launch {
            prefsRepo.deleteWidgetTemplate(templateId)
            _feedbackMessage.value = "Plantilla eliminada"
        }
    }

    fun applyWidgetTemplate(template: WidgetTemplate) {
        viewModelScope.launch {
            prefsRepo.applyWidgetTemplate(template)
            _feedbackMessage.value = "Plantilla '${template.name}' aplicada"
        }
    }

    fun exportWidgetTemplatesJson(onResult: (String) -> Unit) {
        viewModelScope.launch {
            onResult(prefsRepo.exportWidgetTemplatesJson())
        }
    }

    fun importWidgetTemplatesJson(raw: String) {
        viewModelScope.launch {
            val count = prefsRepo.importWidgetTemplatesJson(raw)
            _feedbackMessage.value = if (count > 0) {
                "Se importaron $count plantillas"
            } else {
                "No se reconocieron plantillas en el texto JSON"
            }
        }
    }

    fun exportFullBackupJson(onResult: (String) -> Unit) {
        viewModelScope.launch {
            onResult(prefsRepo.exportFullBackupJson())
        }
    }

    fun importFullBackupJson(raw: String) {
        viewModelScope.launch {
            val ok = prefsRepo.importFullBackupJson(raw)
            _feedbackMessage.value = if (ok) {
                "Copia de seguridad restaurada correctamente"
            } else {
                "Formato de copia no válido"
            }
        }
    }

    fun pinWidget(format: WidgetFormat) {
        val ctx = getApplication<Application>()
        val cls = when (format) {
            WidgetFormat.CAPSULE_4X1 -> MagicWidgetCapsuleProvider::class.java
            WidgetFormat.CARD_4X2 -> MagicWidgetCardProvider::class.java
            WidgetFormat.SQUARE_2X2 -> MagicWidgetSquareProvider::class.java
            WidgetFormat.MINI_2X1 -> MagicWidgetMiniProvider::class.java
        }
        val provider = ComponentName(ctx, cls)
        val successMsg = "Confirma para añadir el widget ${format.title} (${format.gridLabel}) a tu escritorio"
        when (val result = AndroidVersionCompat.requestPinAppWidgetSafe(ctx, provider, successMsg)) {
            is CompatActionResult.Success -> _feedbackMessage.value = result.message
            is CompatActionResult.AlreadyConfigured -> _feedbackMessage.value = result.message
            is CompatActionResult.FallbackManualGuide -> _feedbackMessage.value = result.message
            is CompatActionResult.Error -> _feedbackMessage.value = result.message
        }
    }

    fun pinWidget(isCapsule: Boolean) {
        pinWidget(if (isCapsule) WidgetFormat.CAPSULE_4X1 else WidgetFormat.CARD_4X2)
    }

    // Lock screen & Quick Settings mutations
    fun toggleBridgeService(enable: Boolean) {
        val ctx = getApplication<Application>()
        viewModelScope.launch {
            prefsRepo.updateLockAndQsConfig { it.copy(bridgeEnabled = enable) }
        }
        if (enable) {
            MagicMediaBridgeService.startOrUpdate(ctx)
            _feedbackMessage.value = "Puente de pantalla de bloqueo activado"
        } else {
            MagicMediaBridgeService.stop(ctx)
            _feedbackMessage.value = "Puente de pantalla de bloqueo desactivado"
        }
    }

    fun toggleShowOnLockScreen(show: Boolean) {
        viewModelScope.launch {
            prefsRepo.updateLockAndQsConfig { it.copy(showOnLockScreen = show) }
        }
    }

    fun toggleShowArtworkOnLockScreen(show: Boolean) {
        viewModelScope.launch {
            prefsRepo.updateLockAndQsConfig { it.copy(showArtworkOnLockScreen = show) }
        }
    }

    fun toggleRequireUnlockToOpenApp(req: Boolean) {
        viewModelScope.launch {
            prefsRepo.updateLockAndQsConfig { it.copy(requireUnlockToOpenApp = req) }
        }
    }

    fun toggleRequireUnlockToSwitchSource(req: Boolean) {
        viewModelScope.launch {
            prefsRepo.updateLockAndQsConfig { it.copy(requireUnlockToSwitchSource = req) }
        }
    }

    fun toggleRequireUnlockForCustomActions(req: Boolean) {
        viewModelScope.launch {
            prefsRepo.updateLockAndQsConfig { it.copy(requireUnlockForCustomActions = req) }
        }
    }

    fun setLockAction1(action: CustomControlAction) {
        viewModelScope.launch {
            prefsRepo.updateLockAndQsConfig { it.copy(lockScreenAction1 = action) }
        }
    }

    fun setLockAction2(action: CustomControlAction) {
        viewModelScope.launch {
            prefsRepo.updateLockAndQsConfig { it.copy(lockScreenAction2 = action) }
        }
    }

    fun setQsTileMode(mode: QsTileMode) {
        viewModelScope.launch {
            prefsRepo.updateLockAndQsConfig { it.copy(qsTileMode = mode) }
        }
    }

    fun setLockPrivacyLevel(level: LockPrivacyLevel) {
        viewModelScope.launch {
            prefsRepo.updateLockAndQsConfig { it.copy(lockPrivacyLevel = level) }
        }
    }

    fun setLockAutoCloseSeconds(seconds: Int) {
        viewModelScope.launch {
            prefsRepo.updateLockAndQsConfig { it.copy(lockAutoCloseSeconds = seconds) }
        }
    }

    fun toggleCompactActionIndex(index: Int) {
        viewModelScope.launch {
            prefsRepo.updateLockAndQsConfig { cfg ->
                val list = cfg.compactActionIndices.toMutableList()
                if (list.contains(index)) {
                    list.remove(index)
                } else {
                    list.add(index)
                }
                cfg.copy(compactActionIndices = list.take(3))
            }
        }
    }

    fun dismissNotifyChannelGuide() {
        viewModelScope.launch {
            prefsRepo.updateLockAndQsConfig { it.copy(notifyChannelGuideShown = true) }
        }
    }

    fun requestAddQuickTile(type: QuickTileType) {
        val isPlay = type == QuickTileType.PLAY_PAUSE
        requestAddQuickSettingsTile(isPlay)
        viewModelScope.launch {
            prefsRepo.setQuickTileAdded(type.key, true)
        }
    }

    fun requestAddQuickSettingsTile(isPlayPause: Boolean) {
        val ctx = getApplication<Application>()
        val component = ComponentName(
            ctx,
            if (isPlayPause) MagicPlayPauseTileService::class.java else MagicNextTrackTileService::class.java,
        )
        val label = if (isPlayPause) {
            ctx.getString(R.string.tile_play_pause_label)
        } else {
            ctx.getString(R.string.tile_next_label)
        }
        val iconRes = if (isPlayPause) R.drawable.ic_magic_music_note else R.drawable.ic_magic_next

        AndroidVersionCompat.requestAddTileServiceSafe(
            context = ctx,
            tileComponent = component,
            label = label,
            iconResId = iconRes,
        ) { result ->
            if (result is CompatActionResult.Success || result is CompatActionResult.AlreadyConfigured) {
                MediaRepository.setTileAddedStatus(isPlayPause, true)
            }
            _feedbackMessage.value = when (result) {
                is CompatActionResult.Success -> result.message
                is CompatActionResult.AlreadyConfigured -> result.message
                is CompatActionResult.FallbackManualGuide -> result.message
                is CompatActionResult.Error -> result.message
            }
        }
    }

    fun fixHealthCheck(id: HealthCheckId, onRequestPostNotifications: () -> Unit) {
        when (id) {
            HealthCheckId.NOTIFICATION_LISTENER -> openNotificationListenerSettings()
            HealthCheckId.POST_NOTIFICATIONS -> onRequestPostNotifications()
            HealthCheckId.BATTERY_OPTIMIZATION -> openBatteryOptimizationSettings()
            HealthCheckId.WIDGET_PINNED -> pinWidget(WidgetFormat.CARD_4X2)
            HealthCheckId.QUICK_TILE_ADDED -> requestAddQuickSettingsTile(true)
        }
    }

    fun openLockScreenDeck() {
        val ctx = getApplication<Application>()
        ctx.startActivity(
            Intent(ctx, LockScreenDeckActivity::class.java).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            },
        )
    }

    fun openNotificationListenerSettings() {
        val ctx = getApplication<Application>()
        val res = OemCompat.openNotificationListenerSettingsSafe(ctx)
        if (res is CompatActionResult.FallbackManualGuide) {
            _feedbackMessage.value = res.message
        }
    }

    fun openBatteryOptimizationSettings() {
        val ctx = getApplication<Application>()
        val res = OemCompat.openBatterySettingsSafe(ctx)
        if (res is CompatActionResult.FallbackManualGuide) {
            _feedbackMessage.value = res.message
        }
    }

    fun launchMusicService(packageName: String) {
        val ctx = getApplication<Application>()
        MediaRepository.launchMusicApp(ctx, packageName)
    }
}
