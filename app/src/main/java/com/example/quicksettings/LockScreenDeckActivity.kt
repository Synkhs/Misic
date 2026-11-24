package com.example.quicksettings

import android.app.KeyguardManager
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.view.WindowManager
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Lock
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.core.media.MediaRepository
import com.example.core.media.SleepTimerState
import com.example.data.CustomControlAction
import com.example.data.LockAndQsConfig
import com.example.data.LockPrivacyLevel
import com.example.data.PreferencesRepository
import com.example.designsystem.ArtworkPaletteExtractor
import com.example.designsystem.MagicMusicTheme
import com.example.designsystem.ThemePreferences
import com.example.designsystem.VioletSeedColor
import com.example.ui.components.NowPlayingDeckContent
import kotlinx.coroutines.delay

class LockScreenDeckActivity : ComponentActivity() {

    private var allowTurnScreenOn = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        MediaRepository.init(applicationContext)
        MediaRepository.refreshAudioOutputs()
        allowTurnScreenOn = intent?.getBooleanExtra(EXTRA_ALLOW_TURN_SCREEN_ON, false) ?: false

        val prefs = PreferencesRepository(applicationContext)

        setContent {
            val media by MediaRepository.mediaState.collectAsStateWithLifecycle()
            val playerState by MediaRepository.playerState.collectAsStateWithLifecycle()
            val sleepTimer by MediaRepository.sleepTimerState.collectAsStateWithLifecycle(initialValue = SleepTimerState())
            val audioOutputs by MediaRepository.audioOutputs.collectAsStateWithLifecycle()
            val lockConfig by prefs.lockAndQsConfigFlow.collectAsStateWithLifecycle(initialValue = LockAndQsConfig())
            val themePrefs by prefs.themePreferencesFlow.collectAsStateWithLifecycle(initialValue = ThemePreferences())

            val km = remember { getSystemService(Context.KEYGUARD_SERVICE) as? KeyguardManager }
            var isKeyguardLocked by remember { mutableStateOf(km?.isKeyguardLocked == true) }
            var lastInteractionMs by remember { mutableLongStateOf(System.currentTimeMillis()) }

            LaunchedEffect(lockConfig.showOnLockScreen, lockConfig.lockPrivacyLevel, isKeyguardLocked) {
                isKeyguardLocked = km?.isKeyguardLocked == true
                val allowOnLock = lockConfig.showOnLockScreen && lockConfig.lockPrivacyLevel != LockPrivacyLevel.SECRET
                applyLockWindowFlags(
                    showWhenLocked = allowOnLock,
                    turnScreenOn = allowTurnScreenOn && allowOnLock,
                )
            }

            // Inactivity auto-close when keyguard is locked
            LaunchedEffect(lastInteractionMs, lockConfig.lockAutoCloseSeconds, media.isPlaying, isKeyguardLocked) {
                val timeoutSec = lockConfig.lockAutoCloseSeconds
                if (timeoutSec > 0 && isKeyguardLocked) {
                    delay(timeoutSec * 1000L)
                    finish()
                }
            }

            var artworkColors by remember {
                mutableStateOf(ArtworkPaletteExtractor.deriveFromFallbackSeed(VioletSeedColor))
            }
            LaunchedEffect(media.packageName, media.album, media.albumArt, lockConfig.showArtworkOnLockScreen, lockConfig.lockPrivacyLevel) {
                val allowArtwork = lockConfig.showArtworkOnLockScreen &&
                    (!isKeyguardLocked || lockConfig.lockPrivacyLevel == LockPrivacyLevel.PUBLIC)
                artworkColors = ArtworkPaletteExtractor.extractCached(
                    cacheKey = "${media.packageName}_${media.album}",
                    bitmap = if (allowArtwork) media.albumArt else null,
                    fallbackColor = if (media.hasActiveMedia) media.service.accentColor else VioletSeedColor,
                )
            }

            val runProtected: (Boolean, () -> Unit) -> Unit = { requiresUnlock, block ->
                val lockedNow = km?.isKeyguardLocked == true
                isKeyguardLocked = lockedNow
                if (requiresUnlock && lockedNow && km != null) {
                    km.requestDismissKeyguard(
                        this,
                        object : KeyguardManager.KeyguardDismissCallback() {
                            override fun onDismissSucceeded() {
                                isKeyguardLocked = km.isKeyguardLocked
                                block()
                            }
                        },
                    )
                } else {
                    block()
                }
            }

            MagicMusicTheme(themePreferences = themePrefs) {
                val blockedBySecret = isKeyguardLocked &&
                    (!lockConfig.showOnLockScreen || lockConfig.lockPrivacyLevel == LockPrivacyLevel.SECRET)

                if (blockedBySecret) {
                    SecretLockGuardView(
                        onUnlock = {
                            runProtected(true) {
                                isKeyguardLocked = km?.isKeyguardLocked == true
                            }
                        },
                        onClose = { finish() },
                    )
                } else {
                    NowPlayingDeckContent(
                        media = media,
                        sleepTimer = sleepTimer,
                        audioOutputs = audioOutputs,
                        artworkColors = artworkColors,
                        isLockScreenMode = true,
                        isKeyguardLocked = isKeyguardLocked,
                        lockPrivacyLevel = lockConfig.lockPrivacyLevel,
                        showArtworkOnLockScreen = lockConfig.showArtworkOnLockScreen,
                        lockCustomAction1 = lockConfig.lockScreenAction1,
                        lockCustomAction2 = lockConfig.lockScreenAction2,
                        initialVolumePercent = playerState.volumePercent,
                        onBackOrClose = { finish() },
                        onPlayPause = { MediaRepository.playPause() },
                        onNext = { MediaRepository.next() },
                        onPrevious = { MediaRepository.previous() },
                        onSeekTo = { MediaRepository.seekTo(it) },
                        onSeekBy = { MediaRepository.seekBy(it) },
                        onToggleFavorite = { MediaRepository.toggleFavorite() },
                        onToggleShuffle = { MediaRepository.toggleShuffle() },
                        onToggleRepeat = { MediaRepository.toggleRepeat() },
                        onStartSleepTimerCountdown = { mins, fade ->
                            MediaRepository.startSleepTimerCountdown(mins, fade)
                        },
                        onStartSleepTimerEndOfTrack = { fade ->
                            MediaRepository.startSleepTimerEndOfTrack(fade)
                        },
                        onToggleSleepFade = { MediaRepository.setSleepTimerFadeOut(it) },
                        onCancelSleepTimer = { MediaRepository.cancelSleepTimer() },
                        onSetVolumePercent = { MediaRepository.setMusicVolumePercent(it) },
                        onRefreshOutputs = { MediaRepository.refreshAudioOutputs() },
                        onSelectOutput = { MediaRepository.selectAudioOutput(it) },
                        onOpenSourceApp = { pkg ->
                            runProtected(lockConfig.requireUnlockToOpenApp) {
                                MediaRepository.launchMusicApp(this, pkg)
                            }
                        },
                        onCustomAction = { action ->
                            val needsUnlock = when (action) {
                                CustomControlAction.OPEN_PLAYER -> lockConfig.requireUnlockToOpenApp
                                CustomControlAction.SWITCH_SOURCE -> lockConfig.requireUnlockToSwitchSource
                                else -> lockConfig.requireUnlockForCustomActions
                            }
                            runProtected(needsUnlock) {
                                MediaRepository.executeCustomAction(action)
                            }
                        },
                        onRequestUnlock = {
                            runProtected(true) {
                                isKeyguardLocked = km?.isKeyguardLocked == true
                            }
                        },
                        onUserInteraction = {
                            lastInteractionMs = System.currentTimeMillis()
                        },
                    )
                }
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        allowTurnScreenOn = intent.getBooleanExtra(EXTRA_ALLOW_TURN_SCREEN_ON, false)
    }

    @Suppress("DEPRECATION")
    private fun applyLockWindowFlags(showWhenLocked: Boolean, turnScreenOn: Boolean) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O_MR1) {
            setShowWhenLocked(showWhenLocked)
            setTurnScreenOn(turnScreenOn)
        } else {
            if (showWhenLocked) {
                window.addFlags(WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED)
            } else {
                window.clearFlags(WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED)
            }
            if (turnScreenOn) {
                window.addFlags(WindowManager.LayoutParams.FLAG_TURN_SCREEN_ON)
            } else {
                window.clearFlags(WindowManager.LayoutParams.FLAG_TURN_SCREEN_ON)
            }
        }
    }

    companion object {
        const val EXTRA_ALLOW_TURN_SCREEN_ON = "extra_allow_turn_screen_on"
    }
}

@Composable
private fun SecretLockGuardView(
    onUnlock: () -> Unit,
    onClose: () -> Unit,
) {
    val colors = MaterialTheme.colorScheme
    Surface(
        modifier = Modifier.fillMaxSize(),
        color = colors.background,
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(32.dp),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Icon(
                imageVector = Icons.Rounded.Lock,
                contentDescription = null,
                tint = colors.primary,
                modifier = Modifier.size(48.dp),
            )
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                text = "Controles ocultos en bloqueo",
                style = MaterialTheme.typography.titleLarge,
                color = colors.onBackground,
                textAlign = TextAlign.Center,
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = "Tu configuración de privacidad oculta el reproductor mientras el dispositivo esté bloqueado.",
                style = MaterialTheme.typography.bodyMedium,
                color = colors.onSurfaceVariant,
                textAlign = TextAlign.Center,
            )
            Spacer(modifier = Modifier.height(24.dp))
            Button(onClick = onUnlock) {
                Text("Desbloquear para controlar")
            }
            Spacer(modifier = Modifier.height(8.dp))
            TextButton(onClick = onClose) {
                Text("Cerrar")
            }
        }
    }
}
