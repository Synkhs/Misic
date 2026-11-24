package com.example.media

import android.content.Context
import com.example.core.media.MediaRepository
import com.example.core.media.SleepTimerState
import com.example.core.media.SystemHealthSummary
import com.example.data.AudioOutputDevice
import com.example.data.CustomControlAction
import com.example.data.InstalledServiceStatus
import com.example.data.UnifiedMediaState
import kotlinx.coroutines.flow.StateFlow

/**
 * Compatibility facade forwarding calls to the single source of truth [MediaRepository].
 */
object MediaHubController {

    val localEngine: LocalAmbientAudioEngine
        get() = MediaRepository.localEngine

    val mediaState: StateFlow<UnifiedMediaState>
        get() = MediaRepository.mediaState

    val sleepTimerState: StateFlow<SleepTimerState>
        get() = MediaRepository.sleepTimerState

    val systemHealth: StateFlow<SystemHealthSummary>
        get() = MediaRepository.systemHealth

    val audioOutputs: StateFlow<List<AudioOutputDevice>>
        get() = MediaRepository.audioOutputs

    val hasListenerPermission: StateFlow<Boolean>
        get() = MediaRepository.hasListenerPermission

    val hasPostNotificationsPermission: StateFlow<Boolean>
        get() = MediaRepository.hasPostNotificationsPermission

    val installedServices: StateFlow<List<InstalledServiceStatus>>
        get() = MediaRepository.installedServices

    val bridgeActive: StateFlow<Boolean>
        get() = MediaRepository.bridgeActive

    fun init(context: Context) = MediaRepository.init(context)

    fun setBridgeRunning(running: Boolean) = MediaRepository.setBridgeRunning(running)

    fun refreshPermissionsAndServices() = MediaRepository.refreshPermissionsAndServices()

    fun rebuildUnifiedState() = MediaRepository.rebuildState()

    fun selectSession(packageName: String) = MediaRepository.selectSession(packageName)

    fun playPause() = MediaRepository.playPause()

    fun next() = MediaRepository.next()

    fun previous() = MediaRepository.previous()

    fun seekTo(positionMs: Long) = MediaRepository.seekTo(positionMs)

    fun seekBy(deltaMs: Long) = MediaRepository.seekBy(deltaMs)

    fun toggleFavorite() = MediaRepository.toggleFavorite()

    fun toggleShuffle() = MediaRepository.toggleShuffle()

    fun toggleRepeat() = MediaRepository.toggleRepeat()

    fun rotateActiveSource() = MediaRepository.rotateActiveSource()

    fun toggleMuteAudio() = MediaRepository.toggleMuteAudio()

    fun executeCustomAction(action: CustomControlAction) = MediaRepository.executeCustomAction(action)

    fun openActiveOrPreferredApp(ctx: Context) = MediaRepository.openActiveOrPreferredApp(ctx)

    fun launchMusicApp(ctx: Context, packageName: String) = MediaRepository.launchMusicApp(ctx, packageName)
}
