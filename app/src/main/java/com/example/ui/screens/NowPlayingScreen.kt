package com.example.ui.screens

import androidx.compose.runtime.Composable
import com.example.data.AudioOutputDevice
import com.example.designsystem.ArtworkPaletteColors
import com.example.ui.MagicMusicUiState
import com.example.ui.components.NowPlayingDeckContent

@Composable
fun NowPlayingScreen(
    state: MagicMusicUiState,
    artworkColors: ArtworkPaletteColors,
    onBack: () -> Unit,
    onPlayPause: () -> Unit,
    onNext: () -> Unit,
    onPrevious: () -> Unit,
    onSeekTo: (Long) -> Unit,
    onSeekBy: (Long) -> Unit,
    onToggleFavorite: () -> Unit,
    onToggleShuffle: () -> Unit,
    onToggleRepeat: () -> Unit,
    onOpenLyrics: () -> Unit,
    onOpenQueue: () -> Unit,
    onStartSleepTimerCountdown: (Int, Boolean) -> Unit,
    onStartSleepTimerEndOfTrack: (Boolean) -> Unit,
    onToggleSleepFade: (Boolean) -> Unit,
    onCancelSleepTimer: () -> Unit,
    onSetVolumePercent: (Int) -> Unit,
    onRefreshOutputs: () -> Unit,
    onSelectOutput: (AudioOutputDevice) -> Unit,
    onAddToQuickPlaylist: () -> Unit,
    onViewArtist: (String) -> Unit,
    onOpenSourceApp: (String) -> Unit,
) {
    NowPlayingDeckContent(
        media = state.media,
        sleepTimer = state.sleepTimer,
        audioOutputs = state.audioOutputs,
        artworkColors = artworkColors,
        isLockScreenMode = false,
        initialVolumePercent = state.playerState.volumePercent,
        onBackOrClose = onBack,
        onPlayPause = onPlayPause,
        onNext = onNext,
        onPrevious = onPrevious,
        onSeekTo = onSeekTo,
        onSeekBy = onSeekBy,
        onToggleFavorite = onToggleFavorite,
        onToggleShuffle = onToggleShuffle,
        onToggleRepeat = onToggleRepeat,
        onOpenLyrics = onOpenLyrics,
        onOpenQueue = onOpenQueue,
        onStartSleepTimerCountdown = onStartSleepTimerCountdown,
        onStartSleepTimerEndOfTrack = onStartSleepTimerEndOfTrack,
        onToggleSleepFade = onToggleSleepFade,
        onCancelSleepTimer = onCancelSleepTimer,
        onSetVolumePercent = onSetVolumePercent,
        onRefreshOutputs = onRefreshOutputs,
        onSelectOutput = onSelectOutput,
        onAddToQuickPlaylist = onAddToQuickPlaylist,
        onViewArtist = onViewArtist,
        onOpenSourceApp = onOpenSourceApp,
    )
}
