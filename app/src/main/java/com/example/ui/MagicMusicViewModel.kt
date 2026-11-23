package com.example.ui

import android.app.Application
import android.app.StatusBarManager
import android.appwidget.AppWidgetManager
import android.content.ComponentName
import android.content.Intent
import android.graphics.drawable.Icon
import android.os.Build
import android.provider.Settings
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.R
import com.example.data.CustomControlAction
import com.example.data.InstalledServiceStatus
import com.example.data.LockAndQsConfig
import com.example.data.PreferencesRepository
import com.example.data.QsTileMode
import com.example.data.UnifiedMediaState
import com.example.data.WidgetConfig
import com.example.data.WidgetStyle
import com.example.media.MagicMediaBridgeService
import com.example.media.MediaHubController
import com.example.quicksettings.LockScreenDeckActivity
import com.example.quicksettings.MagicNextTrackTileService
import com.example.quicksettings.MagicPlayPauseTileService
import com.example.widget.MagicWidgetCapsuleProvider
import com.example.widget.MagicWidgetCardProvider
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class MagicMusicUiState(
    val media: UnifiedMediaState = UnifiedMediaState(),
    val widgetConfig: WidgetConfig = WidgetConfig(),
    val lockAndQsConfig: LockAndQsConfig = LockAndQsConfig(),
    val hasListenerPermission: Boolean = false,
    val hasPostNotificationsPermission: Boolean = true,
    val installedServices: List<InstalledServiceStatus> = emptyList(),
    val bridgeRunning: Boolean = false,
    val feedbackMessage: String? = null,
)

class MagicMusicViewModel(application: Application) : AndroidViewModel(application) {

    private val prefsRepo = PreferencesRepository(application)
    private val _feedbackMessage = MutableStateFlow<String?>(null)

    init {
        MediaHubController.init(application)
    }

    private data class SystemSnapshot(
        val hasListener: Boolean,
        val hasNotifications: Boolean,
        val services: List<InstalledServiceStatus>,
        val bridgeRunning: Boolean,
        val feedback: String?,
    )

    private val systemSnapshotFlow = combine(
        MediaHubController.hasListenerPermission,
        MediaHubController.hasPostNotificationsPermission,
        MediaHubController.installedServices,
        MediaHubController.bridgeActive,
        _feedbackMessage,
    ) { hasListener, hasNotif, services, bridge, feedback ->
        SystemSnapshot(hasListener, hasNotif, services, bridge, feedback)
    }

    val uiState: StateFlow<MagicMusicUiState> = combine(
        MediaHubController.mediaState,
        prefsRepo.widgetConfigFlow,
        prefsRepo.lockAndQsConfigFlow,
        systemSnapshotFlow,
    ) { media, widgetCfg, lockCfg, sys ->
        MagicMusicUiState(
            media = media,
            widgetConfig = widgetCfg,
            lockAndQsConfig = lockCfg,
            hasListenerPermission = sys.hasListener,
            hasPostNotificationsPermission = sys.hasNotifications,
            installedServices = sys.services,
            bridgeRunning = sys.bridgeRunning,
            feedbackMessage = sys.feedback,
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = MagicMusicUiState(),
    )

    fun refreshSystemState() {
        MediaHubController.refreshPermissionsAndServices()
    }

    fun clearFeedback() {
        _feedbackMessage.value = null
    }

    fun showFeedback(msg: String) {
        _feedbackMessage.value = msg
    }

    // Playback controls
    fun playPause() = MediaHubController.playPause()
    fun next() = MediaHubController.next()
    fun previous() = MediaHubController.previous()
    fun seekTo(posMs: Long) = MediaHubController.seekTo(posMs)
    fun selectSession(pkg: String) = MediaHubController.selectSession(pkg)
    fun executeCustomAction(action: CustomControlAction) = MediaHubController.executeCustomAction(action)

    fun toggleLocalAudioEngine() {
        val ctx = getApplication<Application>()
        val local = MediaHubController.localEngine.state.value
        if (local.isActive && local.isPlaying) {
            MediaHubController.localEngine.pause()
            _feedbackMessage.value = "Motor local pausado"
        } else {
            MediaHubController.localEngine.startOrResume()
            MagicMediaBridgeService.startOrUpdate(ctx)
            _feedbackMessage.value = "Motor Hi-Res activo • Ya puedes probar widgets, pantalla de bloqueo y ajustes rápidos"
        }
        MediaHubController.rebuildUnifiedState()
    }

    // Widget studio mutations
    fun setWidgetStyle(style: WidgetStyle) {
        viewModelScope.launch {
            prefsRepo.updateWidgetConfig { it.copy(style = style) }
        }
    }

    fun setCornerRadius(dp: Int) {
        viewModelScope.launch {
            prefsRepo.updateWidgetConfig { it.copy(cornerRadiusDp = dp.coerceIn(12, 34)) }
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

    fun toggleMagicCapsuleHeader(show: Boolean) {
        viewModelScope.launch {
            prefsRepo.updateWidgetConfig { it.copy(showMagicCapsuleHeader = show) }
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

    fun pinWidget(isCapsule: Boolean) {
        val ctx = getApplication<Application>()
        val awm = AppWidgetManager.getInstance(ctx)
        if (awm == null) {
            _feedbackMessage.value = "Gestor de widgets no disponible en este dispositivo"
            return
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O && awm.isRequestPinAppWidgetSupported) {
            val provider = ComponentName(
                ctx,
                if (isCapsule) MagicWidgetCapsuleProvider::class.java else MagicWidgetCardProvider::class.java,
            )
            val requested = awm.requestPinAppWidget(provider, null, null)
            _feedbackMessage.value = if (requested) {
                if (isCapsule) {
                    "Confirma para fijar el widget Cápsula Viva 4x1 en tu pantalla"
                } else {
                    "Confirma para fijar la Tarjeta Control 4x2 en tu pantalla"
                }
            } else {
                "Tu launcher no permitió fijar automáticamente; añádelo desde Widgets"
            }
        } else {
            _feedbackMessage.value = "Mantén pulsado el escritorio y añade el widget MagicMusic"
        }
    }

    // Lock screen & Quick Settings mutations
    fun toggleBridgeService(enable: Boolean) {
        val ctx = getApplication<Application>()
        viewModelScope.launch {
            prefsRepo.updateLockAndQsConfig { it.copy(bridgeEnabled = enable) }
        }
        if (enable) {
            MagicMediaBridgeService.startOrUpdate(ctx)
            _feedbackMessage.value = "Puente de pantalla de bloqueo Android 16 activado"
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

    fun requestAddQuickSettingsTile(isPlayPause: Boolean) {
        val ctx = getApplication<Application>()
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            val sbm = ctx.getSystemService(StatusBarManager::class.java)
            if (sbm == null) {
                _feedbackMessage.value = "StatusBarManager no disponible"
                return
            }
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
            sbm.requestAddTileService(
                component,
                label,
                Icon.createWithResource(ctx, iconRes),
                ctx.mainExecutor,
            ) { resultCode ->
                val msg = when (resultCode) {
                    StatusBarManager.TILE_ADD_REQUEST_RESULT_TILE_ADDED ->
                        "Acceso directo '$label' añadido a Ajustes Rápidos"
                    StatusBarManager.TILE_ADD_REQUEST_RESULT_TILE_ALREADY_ADDED ->
                        "El acceso '$label' ya está en Ajustes Rápidos"
                    StatusBarManager.TILE_ADD_REQUEST_RESULT_TILE_NOT_ADDED ->
                        "Cancelado por el usuario"
                    else ->
                        "Añade '$label' editando el panel de Ajustes Rápidos"
                }
                _feedbackMessage.value = msg
            }
        } else {
            _feedbackMessage.value = "Desliza dos veces los Ajustes Rápidos, pulsa Editar y arrastra MagicMusic"
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
        val intent = Intent(Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        runCatching { ctx.startActivity(intent) }
    }

    fun launchMusicService(packageName: String) {
        val ctx = getApplication<Application>()
        MediaHubController.launchMusicApp(ctx, packageName)
    }
}
