package com.example.ui.components

import android.content.Intent
import android.provider.Settings
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.OpenInNew
import androidx.compose.material.icons.automirrored.rounded.QueueMusic
import androidx.compose.material.icons.automirrored.rounded.VolumeUp
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.ArrowDownward
import androidx.compose.material.icons.rounded.ArrowUpward
import androidx.compose.material.icons.rounded.Bedtime
import androidx.compose.material.icons.rounded.BluetoothAudio
import androidx.compose.material.icons.rounded.Bookmark
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.DeleteOutline
import androidx.compose.material.icons.rounded.Favorite
import androidx.compose.material.icons.rounded.FavoriteBorder
import androidx.compose.material.icons.rounded.KeyboardArrowDown
import androidx.compose.material.icons.rounded.Lock
import androidx.compose.material.icons.rounded.LockOpen
import androidx.compose.material.icons.rounded.Lyrics
import androidx.compose.material.icons.rounded.MoreVert
import androidx.compose.material.icons.rounded.Shuffle
import androidx.compose.material.icons.rounded.Speaker
import androidx.compose.material3.BottomSheetDefaults
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Slider
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.example.R
import com.example.core.library.TrackBookmarkEntity
import com.example.core.media.SleepTimerManager
import com.example.core.media.SleepTimerMode
import com.example.core.media.SleepTimerState
import com.example.data.AudioOutputDevice
import com.example.data.CustomControlAction
import com.example.data.LockPrivacyLevel
import com.example.data.UnifiedMediaState
import com.example.designsystem.ArtworkPaletteColors
import com.example.designsystem.MagicTheme
import com.example.designsystem.buildArtworkTonalSurface
import com.example.designsystem.expressive.MagicFloatingToolbarBar
import kotlinx.coroutines.delay
import kotlin.math.abs
import kotlin.math.sin

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NowPlayingDeckContent(
    media: UnifiedMediaState,
    sleepTimer: SleepTimerState,
    audioOutputs: List<AudioOutputDevice>,
    artworkColors: ArtworkPaletteColors,
    positionMs: Long = media.positionMs,
    durationMs: Long = media.durationMs,
    bookmarks: List<TrackBookmarkEntity> = emptyList(),
    isLockScreenMode: Boolean = false,
    isKeyguardLocked: Boolean = false,
    lockPrivacyLevel: LockPrivacyLevel = LockPrivacyLevel.PUBLIC,
    showArtworkOnLockScreen: Boolean = true,
    lockCustomAction1: CustomControlAction = CustomControlAction.REWIND_15,
    lockCustomAction2: CustomControlAction = CustomControlAction.FORWARD_15,
    initialVolumePercent: Int = 70,
    onBackOrClose: () -> Unit,
    onPlayPause: () -> Unit,
    onNext: () -> Unit,
    onPrevious: () -> Unit,
    onSeekTo: (Long) -> Unit,
    onSeekBy: (Long) -> Unit,
    onToggleFavorite: () -> Unit,
    onToggleShuffle: () -> Unit,
    onToggleRepeat: () -> Unit,
    onOpenLyrics: () -> Unit = {},
    onOpenQueue: () -> Unit = {},
    onSkipToQueueItem: (Long) -> Unit = {},
    onReorderLocalQueue: (Int, Int) -> Unit = { _, _ -> },
    onClearLocalQueue: () -> Unit = {},
    onAddBookmark: (String, Long?) -> Unit = { _, _ -> },
    onDeleteBookmark: (Long) -> Unit = {},
    onStartSleepTimerCountdown: (Int, Boolean) -> Unit,
    onStartSleepTimerEndOfTrack: (Boolean) -> Unit,
    onToggleSleepFade: (Boolean) -> Unit,
    onCancelSleepTimer: () -> Unit,
    onSetVolumePercent: (Int) -> Unit,
    onRefreshOutputs: () -> Unit,
    onSelectOutput: (AudioOutputDevice) -> Unit,
    onAddToQuickPlaylist: () -> Unit = {},
    onViewArtist: (String) -> Unit = {},
    onOpenSourceApp: (String) -> Unit,
    onCustomAction: (CustomControlAction) -> Unit = {},
    onRequestUnlock: () -> Unit = {},
    onUserInteraction: () -> Unit = {},
) {
    val context = LocalContext.current
    val tokens = MagicTheme.tokens
    val colors = MaterialTheme.colorScheme

    var showContextMenu by remember { mutableStateOf(false) }
    var showTimerSheet by remember { mutableStateOf(false) }
    var showVolumeSheet by remember { mutableStateOf(false) }
    var showOutputSheet by remember { mutableStateOf(false) }
    var showBookmarksSheet by remember { mutableStateOf(false) }
    var showQueueSheet by remember { mutableStateOf(false) }

    val hideMetadataForPrivacy = isLockScreenMode &&
        isKeyguardLocked &&
        (lockPrivacyLevel == LockPrivacyLevel.HIDE_SENSITIVE || lockPrivacyLevel == LockPrivacyLevel.SECRET)
    val effectiveArtwork = if (isLockScreenMode && (!showArtworkOnLockScreen || hideMetadataForPrivacy)) {
        null
    } else {
        media.albumArt
    }
    val effectiveArtworkModel = if (isLockScreenMode && (!showArtworkOnLockScreen || hideMetadataForPrivacy)) {
        null
    } else {
        media.albumArt ?: media.albumArtUri
    }
    val displayTitle = if (hideMetadataForPrivacy) "Contenido multimedia privado" else media.title
    val displayArtist = if (hideMetadataForPrivacy) "Desbloquea para ver detalles de la pista" else media.artist
    val displaySubtitleHeader = when {
        hideMetadataForPrivacy -> "Pantalla de bloqueo • Privacidad activa"
        isLockScreenMode -> "${media.service.displayName} • Bloqueo"
        media.hasActiveMedia -> media.service.displayName
        else -> "Sin sesión activa"
    }

    val flatBackgroundColor = if (tokens.colorFromArtwork && !hideMetadataForPrivacy) {
        buildArtworkTonalSurface(
            artworkColors = artworkColors,
            baseBackground = colors.background,
            isDark = tokens.isDark,
        )
    } else {
        tokens.backgroundColor
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(flatBackgroundColor)
            .pointerInput(Unit) {
                awaitPointerEventScope {
                    while (true) {
                        awaitPointerEvent()
                        onUserInteraction()
                    }
                }
            }
            .statusBarsPadding()
            .navigationBarsPadding(),
        contentAlignment = Alignment.TopCenter,
    ) {
        Column(
            modifier = Modifier
                .widthIn(max = 580.dp)
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = tokens.spacing.screenMargin, vertical = tokens.spacing.sm),
            verticalArrangement = Arrangement.SpaceBetween,
        ) {
            // Top bar with M3 Expressive tertiaryContainer audio-quality badge
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                IconButton(
                    onClick = {
                        onUserInteraction()
                        onBackOrClose()
                    },
                    modifier = Modifier.testTag(if (isLockScreenMode) "lock_deck_close" else "now_playing_back_btn"),
                ) {
                    Icon(
                        imageVector = if (isLockScreenMode) Icons.Rounded.Close else Icons.Rounded.KeyboardArrowDown,
                        contentDescription = stringResource(R.string.action_back),
                        tint = colors.onBackground,
                        modifier = Modifier.size(if (isLockScreenMode) 26.dp else 30.dp),
                    )
                }

                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier.weight(1f),
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                    ) {
                        if (isLockScreenMode) {
                            Icon(
                                imageVector = if (isKeyguardLocked) Icons.Rounded.Lock else Icons.Rounded.LockOpen,
                                contentDescription = null,
                                tint = colors.primary,
                                modifier = Modifier.size(14.dp),
                            )
                        }
                        Text(
                            text = displaySubtitleHeader,
                            style = MaterialTheme.typography.labelLarge,
                            color = colors.onSurfaceVariant,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                        if (!hideMetadataForPrivacy && media.streamFormatBadge.isNotBlank()) {
                            Surface(
                                shape = CircleShape,
                                color = colors.tertiaryContainer,
                            ) {
                                Text(
                                    text = media.streamFormatBadge,
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = FontWeight.SemiBold,
                                    color = colors.onTertiaryContainer,
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp),
                                )
                            }
                        }
                    }
                    if (!hideMetadataForPrivacy && media.album.isNotBlank() && media.hasActiveMedia) {
                        Text(
                            text = media.album,
                            style = MaterialTheme.typography.bodySmall,
                            color = colors.onSurfaceVariant.copy(alpha = 0.85f),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                    }
                }

                if (isLockScreenMode) {
                    IconButton(
                        onClick = {
                            onUserInteraction()
                            if (media.packageName.isNotBlank()) {
                                onOpenSourceApp(media.packageName)
                            } else {
                                onRequestUnlock()
                            }
                        },
                        modifier = Modifier.testTag("lock_deck_open_app_btn"),
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Rounded.OpenInNew,
                            contentDescription = stringResource(R.string.menu_open_source),
                            tint = colors.onBackground,
                        )
                    }
                } else {
                    IconButton(
                        onClick = {
                            onUserInteraction()
                            showContextMenu = true
                        },
                        modifier = Modifier.testTag("now_playing_more_btn"),
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.MoreVert,
                            contentDescription = stringResource(R.string.action_more_options),
                            tint = colors.onBackground,
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Large Artwork honoring Estilo's selected ArtworkShapeStyle
            Box(
                modifier = Modifier.fillMaxWidth(),
                contentAlignment = Alignment.Center,
            ) {
                MagicAlbumArtwork(
                    bitmap = effectiveArtwork,
                    artworkModel = effectiveArtworkModel,
                    contentDescription = displayTitle,
                    cornerRadius = tokens.radii.artworkLarge,
                    fallbackTint = if (hideMetadataForPrivacy) colors.primary else artworkColors.dominant,
                    modifier = Modifier
                        .fillMaxWidth(0.84f)
                        .aspectRatio(1f)
                        .let { mod ->
                            if (isLockScreenMode && media.packageName.isNotBlank()) {
                                mod.clickable {
                                    onUserInteraction()
                                    onOpenSourceApp(media.packageName)
                                }
                            } else {
                                mod
                            }
                        },
                )
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Lifecycle-aware flat tonal 14-bar Canvas audio visualizer
            if (!hideMetadataForPrivacy) {
                TonalAudioVisualizerStrip(
                    isPlaying = media.isPlaying && media.hasActiveMedia,
                    reduceAnimations = tokens.motion.reduceAnimations,
                    barColor = colors.primary,
                    mutedColor = colors.outlineVariant,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(24.dp)
                        .padding(horizontal = 12.dp),
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Title + Artist + Favorite
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = displayTitle,
                        style = MaterialTheme.typography.headlineMedium,
                        fontWeight = FontWeight.Bold,
                        color = colors.onBackground,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = displayArtist,
                        style = MaterialTheme.typography.titleMedium,
                        color = colors.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
                if (media.canFavorite && !hideMetadataForPrivacy) {
                    IconButton(
                        onClick = {
                            onUserInteraction()
                            onToggleFavorite()
                        },
                        modifier = Modifier.testTag(if (isLockScreenMode) "lock_deck_favorite_btn" else "now_playing_favorite_btn"),
                    ) {
                        Icon(
                            imageVector = if (media.isFavorite) Icons.Rounded.Favorite else Icons.Rounded.FavoriteBorder,
                            contentDescription = stringResource(R.string.action_favorite),
                            tint = if (media.isFavorite) colors.primary else colors.onSurfaceVariant,
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            MagicPlaybackControls(
                positionMs = positionMs,
                durationMs = durationMs,
                isPlaying = media.isPlaying,
                shuffleEnabled = media.isShuffleEnabled,
                repeatEnabled = media.isRepeatEnabled,
                onSeekFraction = { frac ->
                    onUserInteraction()
                    if (media.canSeek && durationMs > 0L) {
                        onSeekTo((frac * durationMs).toLong())
                    }
                },
                onPlayPause = {
                    onUserInteraction()
                    onPlayPause()
                },
                onPrevious = {
                    onUserInteraction()
                    onPrevious()
                },
                onNext = {
                    onUserInteraction()
                    onNext()
                },
                onToggleShuffle = {
                    onUserInteraction()
                    onToggleShuffle()
                },
                onToggleRepeat = {
                    onUserInteraction()
                    onToggleRepeat()
                },
                onRewind15 = {
                    onUserInteraction()
                    onSeekBy(-10_000L)
                },
                onForward15 = {
                    onUserInteraction()
                    onSeekBy(10_000L)
                },
                onVolumeClick = {
                    onUserInteraction()
                    showVolumeSheet = true
                },
                onOutputClick = {
                    onUserInteraction()
                    onRefreshOutputs()
                    showOutputSheet = true
                },
            )

            Spacer(modifier = Modifier.height(14.dp))

            if (isLockScreenMode) {
                Surface(
                    shape = RoundedCornerShape(tokens.radii.card),
                    color = colors.surfaceContainerHigh,
                    border = BorderStroke(1.dp, tokens.semantic.divider),
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 12.dp, vertical = 10.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween,
                    ) {
                        LockQuickCustomButton(
                            action = lockCustomAction1,
                            onClick = {
                                onUserInteraction()
                                onCustomAction(lockCustomAction1)
                            },
                            testTag = "lock_deck_custom_1",
                            modifier = Modifier.weight(1f),
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        IconButton(
                            onClick = {
                                onUserInteraction()
                                showTimerSheet = true
                            },
                            modifier = Modifier.testTag("lock_deck_timer_btn"),
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.Bedtime,
                                contentDescription = stringResource(R.string.timer_title),
                                tint = if (sleepTimer.isActive) colors.primary else colors.onSurfaceVariant,
                            )
                        }
                        Spacer(modifier = Modifier.width(8.dp))
                        LockQuickCustomButton(
                            action = lockCustomAction2,
                            onClick = {
                                onUserInteraction()
                                onCustomAction(lockCustomAction2)
                            },
                            testTag = "lock_deck_custom_2",
                            modifier = Modifier.weight(1f),
                        )
                    }
                }
            } else {
                // Expressive Floating Toolbar: Letras / Temporizador / Marcas(N) / Cola(N)
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .pointerInput(Unit) {
                            detectVerticalDragGestures { _, dragAmount ->
                                if (dragAmount < -28f) onOpenLyrics()
                            }
                        },
                    contentAlignment = Alignment.Center,
                ) {
                    MagicFloatingToolbarBar(
                        useExpressiveM3 = tokens.useExpressiveM3,
                        containerColor = colors.surfaceContainerHigh,
                    ) {
                        IconButton(
                            onClick = onOpenLyrics,
                            modifier = Modifier.testTag("now_playing_lyrics_btn"),
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.Lyrics,
                                contentDescription = stringResource(R.string.action_lyrics),
                                tint = colors.primary,
                            )
                        }
                        IconButton(
                            onClick = { showTimerSheet = true },
                            modifier = Modifier.testTag("now_playing_timer_btn"),
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.Bedtime,
                                contentDescription = stringResource(R.string.timer_title),
                                tint = if (sleepTimer.isActive) colors.primary else colors.onSurfaceVariant,
                            )
                        }
                        TextButton(
                            onClick = { showBookmarksSheet = true },
                            modifier = Modifier.testTag("now_playing_bookmarks_btn"),
                        ) {
                            Icon(Icons.Rounded.Bookmark, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(Modifier.width(4.dp))
                            Text("Marcas (${bookmarks.size})", maxLines = 1)
                        }
                        TextButton(
                            onClick = {
                                if (media.queue.isNotEmpty()) showQueueSheet = true
                                else onOpenQueue()
                            },
                            modifier = Modifier.testTag("now_playing_queue_btn"),
                        ) {
                            Icon(Icons.AutoMirrored.Rounded.QueueMusic, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(Modifier.width(4.dp))
                            Text("Cola (${media.queue.size})", maxLines = 1)
                        }
                    }
                }
            }
        }
    }

    if (showContextMenu && !isLockScreenMode) {
        MagicContextMenuSheet(
            title = media.title,
            artist = media.artist,
            bitmap = media.albumArt,
            artworkModel = media.albumArt ?: media.albumArtUri,
            onDismiss = { showContextMenu = false },
            onActionSelected = { label ->
                when {
                    label.contains("playlist", ignoreCase = true) || label.contains("lista", ignoreCase = true) ->
                        onAddToQuickPlaylist()
                    label.contains("Compartir", ignoreCase = true) -> {
                        val shareIntent = Intent(Intent.ACTION_SEND).apply {
                            type = "text/plain"
                            putExtra(
                                Intent.EXTRA_TEXT,
                                "Escuchando ${media.title} — ${media.artist} en ${media.service.displayName}",
                            )
                        }
                        context.startActivity(Intent.createChooser(shareIntent, "Compartir pista"))
                    }
                    label.contains("artista", ignoreCase = true) ->
                        onViewArtist(media.artist)
                    label.contains("Abrir", ignoreCase = true) && media.packageName.isNotBlank() ->
                        onOpenSourceApp(media.packageName)
                }
            },
        )
    }

    if (showTimerSheet) {
        SleepTimerModalSheet(
            sleepTimer = sleepTimer,
            onSelectMinutes = { mins, fade ->
                onUserInteraction()
                onStartSleepTimerCountdown(mins, fade)
            },
            onSelectEndOfTrack = { fade ->
                onUserInteraction()
                onStartSleepTimerEndOfTrack(fade)
            },
            onToggleFade = { fade ->
                onUserInteraction()
                onToggleSleepFade(fade)
            },
            onCancel = {
                onUserInteraction()
                onCancelSleepTimer()
            },
            onDismiss = { showTimerSheet = false },
        )
    }

    if (showVolumeSheet) {
        VolumeControlSheet(
            initialPercent = initialVolumePercent,
            onSetVolumePercent = {
                onUserInteraction()
                onSetVolumePercent(it)
            },
            onDismiss = { showVolumeSheet = false },
        )
    }

    if (showOutputSheet) {
        AudioOutputPickerSheet(
            outputs = audioOutputs,
            onSelectOutput = { device ->
                onUserInteraction()
                onSelectOutput(device)
                showOutputSheet = false
            },
            onOpenBluetoothSettings = {
                onUserInteraction()
                context.startActivity(
                    Intent(Settings.ACTION_BLUETOOTH_SETTINGS).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK),
                )
            },
            onDismiss = { showOutputSheet = false },
        )
    }

    if (showBookmarksSheet) {
        BookmarksModalSheet(
            positionMs = positionMs,
            bookmarks = bookmarks,
            onAddBookmark = { label -> onAddBookmark(label, positionMs) },
            onJumpToBookmark = { pos ->
                onSeekTo(pos)
                showBookmarksSheet = false
            },
            onDeleteBookmark = onDeleteBookmark,
            onDismiss = { showBookmarksSheet = false },
        )
    }

    if (showQueueSheet) {
        QueueModalSheet(
            media = media,
            onShuffle = onToggleShuffle,
            onClear = onClearLocalQueue,
            onSkipToItem = { qId ->
                onSkipToQueueItem(qId)
                showQueueSheet = false
            },
            onReorder = onReorderLocalQueue,
            onOpenFullQueue = {
                showQueueSheet = false
                onOpenQueue()
            },
            onDismiss = { showQueueSheet = false },
        )
    }
}

@Composable
private fun TonalAudioVisualizerStrip(
    isPlaying: Boolean,
    reduceAnimations: Boolean,
    barColor: androidx.compose.ui.graphics.Color,
    mutedColor: androidx.compose.ui.graphics.Color,
    modifier: Modifier = Modifier,
) {
    val lifecycleOwner = LocalLifecycleOwner.current
    var isVisible by remember { mutableStateOf(true) }
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_START) isVisible = true
            else if (event == Lifecycle.Event.ON_STOP) isVisible = false
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    var phaseTick by remember { mutableIntStateOf(0) }
    LaunchedEffect(isPlaying, reduceAnimations, isVisible) {
        if (isPlaying && !reduceAnimations && isVisible) {
            while (true) {
                phaseTick++
                delay(90L)
            }
        }
    }

    Canvas(modifier = modifier) {
        val bars = 14
        val slot = size.width / bars
        val barWidth = (slot * 0.56f).coerceAtLeast(4f)
        for (i in 0 until bars) {
            val amp = if (isPlaying && !reduceAnimations) {
                0.28f + 0.68f * abs(sin((phaseTick * 0.35f) + i * 0.65f))
            } else if (isPlaying) {
                0.35f + 0.45f * ((i % 4) / 3f)
            } else {
                0.22f
            }
            val barH = (size.height * amp).coerceAtLeast(4f)
            val left = i * slot + (slot - barWidth) / 2f
            val top = (size.height - barH) / 2f
            drawRoundRect(
                color = if (isPlaying) barColor else mutedColor,
                topLeft = Offset(left, top),
                size = Size(barWidth, barH),
                cornerRadius = CornerRadius(barWidth / 2f, barWidth / 2f),
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun BookmarksModalSheet(
    positionMs: Long,
    bookmarks: List<TrackBookmarkEntity>,
    onAddBookmark: (String) -> Unit,
    onJumpToBookmark: (Long) -> Unit,
    onDeleteBookmark: (Long) -> Unit,
    onDismiss: () -> Unit,
) {
    val tokens = MagicTheme.tokens
    val colors = MaterialTheme.colorScheme
    var labelDraft by remember { mutableStateOf("") }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
        shape = RoundedCornerShape(topStart = tokens.radii.sheet, topEnd = tokens.radii.sheet),
        containerColor = colors.surfaceContainerHigh,
        dragHandle = { BottomSheetDefaults.DragHandle() },
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = tokens.spacing.screenMargin)
                .padding(bottom = 28.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Text(
                text = "Marcas de tiempo",
                style = MaterialTheme.typography.titleLarge,
                color = colors.onSurface,
            )
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                OutlinedTextField(
                    value = labelDraft,
                    onValueChange = { labelDraft = it },
                    label = { Text("Etiqueta en ${formatTimeMs(positionMs)}") },
                    singleLine = true,
                    modifier = Modifier.weight(1f),
                )
                Button(
                    onClick = {
                        onAddBookmark(labelDraft)
                        labelDraft = ""
                    },
                    modifier = Modifier.testTag("bookmark_add_btn"),
                ) {
                    Icon(Icons.Rounded.Add, contentDescription = "Añadir marca", modifier = Modifier.size(18.dp))
                }
            }
            if (bookmarks.isEmpty()) {
                Text(
                    text = "Guarda fragmentos favoritos o solos de guitarra para saltar con un toque.",
                    style = MaterialTheme.typography.bodySmall,
                    color = colors.onSurfaceVariant,
                )
            } else {
                LazyColumn(
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.height(240.dp),
                ) {
                    items(bookmarks, key = { it.id }) { bm ->
                        Surface(
                            onClick = { onJumpToBookmark(bm.positionMs) },
                            shape = RoundedCornerShape(tokens.radii.listItem),
                            color = colors.surfaceContainer,
                            modifier = Modifier.fillMaxWidth(),
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
                                verticalAlignment = Alignment.CenterVertically,
                            ) {
                                Surface(
                                    shape = CircleShape,
                                    color = colors.tertiaryContainer,
                                ) {
                                    Text(
                                        text = formatTimeMs(bm.positionMs),
                                        style = MaterialTheme.typography.labelMedium,
                                        color = colors.onTertiaryContainer,
                                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                                    )
                                }
                                Spacer(Modifier.width(10.dp))
                                Text(
                                    text = bm.label,
                                    style = MaterialTheme.typography.titleSmall,
                                    color = colors.onSurface,
                                    modifier = Modifier.weight(1f),
                                )
                                IconButton(onClick = { onDeleteBookmark(bm.id) }) {
                                    Icon(Icons.Rounded.DeleteOutline, contentDescription = "Borrar marca", tint = colors.error)
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun QueueModalSheet(
    media: UnifiedMediaState,
    onShuffle: () -> Unit,
    onClear: () -> Unit,
    onSkipToItem: (Long) -> Unit,
    onReorder: (Int, Int) -> Unit,
    onOpenFullQueue: () -> Unit,
    onDismiss: () -> Unit,
) {
    val tokens = MagicTheme.tokens
    val colors = MaterialTheme.colorScheme
    val canReorder = media.isLocalEngine && media.queue.size > 1

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
        shape = RoundedCornerShape(topStart = tokens.radii.sheet, topEnd = tokens.radii.sheet),
        containerColor = colors.surfaceContainerHigh,
        dragHandle = { BottomSheetDefaults.DragHandle() },
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = tokens.spacing.screenMargin)
                .padding(bottom = 28.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = "Cola (${media.queue.size})",
                    style = MaterialTheme.typography.titleLarge,
                    color = colors.onSurface,
                )
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    FilledTonalButton(onClick = onShuffle) {
                        Icon(Icons.Rounded.Shuffle, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(Modifier.width(4.dp))
                        Text("Mezclar")
                    }
                    if (media.isLocalEngine) {
                        OutlinedButton(onClick = onClear) {
                            Text("Limpiar")
                        }
                    }
                }
            }
            LazyColumn(
                verticalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.height(320.dp),
            ) {
                itemsIndexed(media.queue, key = { idx, item -> "${item.queueId}_$idx" }) { idx, item ->
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        MagicTrackListItem(
                            item = TrackListItemModel(
                                id = item.queueId.toString(),
                                title = item.title,
                                artist = item.artist,
                                durationLabel = if (item.durationMs > 0L) formatTimeMs(item.durationMs) else "",
                                trackNumber = idx + 1,
                                bitmap = if (item.isCurrent) media.albumArt else null,
                            ),
                            variant = if (item.isCurrent) TrackItemVariant.HIGHLIGHTED_CURRENT else TrackItemVariant.NUMBERED,
                            onClick = { onSkipToItem(item.queueId) },
                            modifier = Modifier.weight(1f),
                        )
                        if (canReorder) {
                            Column {
                                IconButton(
                                    enabled = idx > 0,
                                    onClick = { onReorder(idx, idx - 1) },
                                    modifier = Modifier.size(28.dp),
                                ) {
                                    Icon(Icons.Rounded.ArrowUpward, contentDescription = "Subir", modifier = Modifier.size(16.dp))
                                }
                                IconButton(
                                    enabled = idx < media.queue.lastIndex,
                                    onClick = { onReorder(idx, idx + 1) },
                                    modifier = Modifier.size(28.dp),
                                ) {
                                    Icon(Icons.Rounded.ArrowDownward, contentDescription = "Bajar", modifier = Modifier.size(16.dp))
                                }
                            }
                        }
                    }
                }
            }
            TextButton(onClick = onOpenFullQueue, modifier = Modifier.align(Alignment.End)) {
                Text("Ver pantalla completa de Cola")
            }
        }
    }
}

@Composable
private fun LockQuickCustomButton(
    action: CustomControlAction,
    onClick: () -> Unit,
    testTag: String,
    modifier: Modifier = Modifier,
) {
    val colors = MaterialTheme.colorScheme
    Surface(
        onClick = onClick,
        shape = CircleShape,
        color = colors.surfaceContainerLow,
        modifier = modifier.testTag(testTag),
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center,
        ) {
            Icon(
                imageVector = customActionIcon(action),
                contentDescription = action.label,
                tint = colors.primary,
                modifier = Modifier.size(18.dp),
            )
            Spacer(modifier = Modifier.width(6.dp))
            Text(
                text = action.shortLabel,
                style = MaterialTheme.typography.labelLarge,
                color = colors.onSurface,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SleepTimerModalSheet(
    sleepTimer: SleepTimerState,
    onSelectMinutes: (Int, Boolean) -> Unit,
    onSelectEndOfTrack: (Boolean) -> Unit,
    onToggleFade: (Boolean) -> Unit,
    onCancel: () -> Unit,
    onDismiss: () -> Unit,
) {
    val tokens = MagicTheme.tokens
    val colors = MaterialTheme.colorScheme
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
        shape = RoundedCornerShape(topStart = tokens.radii.sheet, topEnd = tokens.radii.sheet),
        containerColor = colors.surfaceContainerHigh,
        dragHandle = { BottomSheetDefaults.DragHandle() },
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = tokens.spacing.screenMargin)
                .padding(bottom = 28.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                Icon(Icons.Rounded.Bedtime, contentDescription = null, tint = colors.primary)
                Text(
                    text = stringResource(R.string.timer_title),
                    style = MaterialTheme.typography.titleMedium,
                    color = colors.onSurface,
                )
            }
            Spacer(modifier = Modifier.height(14.dp))
            Text(
                text = when (sleepTimer.mode) {
                    SleepTimerMode.COUNTDOWN -> SleepTimerManager.formatRemaining(sleepTimer.remainingMs)
                    SleepTimerMode.END_OF_TRACK -> stringResource(R.string.timer_end_of_track)
                    SleepTimerMode.OFF -> "Desactivado"
                },
                style = MaterialTheme.typography.displayMedium,
                fontWeight = FontWeight.Bold,
                color = colors.primary,
            )
            Spacer(modifier = Modifier.height(14.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                listOf(15 to "15 min", 30 to "30 min", 60 to "1 h").forEach { (mins, label) ->
                    MagicFilterChip(
                        label = label,
                        selected = sleepTimer.mode == SleepTimerMode.COUNTDOWN && sleepTimer.selectedMinutes == mins,
                        onClick = { onSelectMinutes(mins, sleepTimer.fadeOutEnabled) },
                    )
                }
            }
            Spacer(modifier = Modifier.height(8.dp))
            MagicFilterChip(
                label = stringResource(R.string.timer_end_of_track),
                selected = sleepTimer.mode == SleepTimerMode.END_OF_TRACK,
                onClick = { onSelectEndOfTrack(sleepTimer.fadeOutEnabled) },
            )
            Spacer(modifier = Modifier.height(16.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = stringResource(R.string.timer_fade_out),
                    style = MaterialTheme.typography.bodyLarge,
                    color = colors.onSurface,
                )
                Switch(
                    checked = sleepTimer.fadeOutEnabled,
                    onCheckedChange = onToggleFade,
                )
            }
            if (sleepTimer.isActive) {
                Spacer(modifier = Modifier.height(12.dp))
                FilledTonalButton(
                    onClick = {
                        onCancel()
                        onDismiss()
                    },
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Text("Cancelar temporizador")
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun VolumeControlSheet(
    initialPercent: Int = 70,
    onSetVolumePercent: (Int) -> Unit,
    onDismiss: () -> Unit,
) {
    val tokens = MagicTheme.tokens
    val colors = MaterialTheme.colorScheme
    var level by remember(initialPercent) { mutableFloatStateOf(initialPercent.coerceIn(0, 100).toFloat()) }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        shape = RoundedCornerShape(topStart = tokens.radii.sheet, topEnd = tokens.radii.sheet),
        containerColor = colors.surfaceContainerHigh,
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = tokens.spacing.screenMargin)
                .padding(bottom = 28.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.AutoMirrored.Rounded.VolumeUp, contentDescription = null, tint = colors.primary)
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "Volumen multimedia (${level.toInt()}%)",
                    style = MaterialTheme.typography.titleMedium,
                    color = colors.onSurface,
                )
            }
            Slider(
                value = level,
                onValueChange = {
                    level = it
                    onSetVolumePercent(it.toInt())
                },
                valueRange = 0f..100f,
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AudioOutputPickerSheet(
    outputs: List<AudioOutputDevice>,
    onSelectOutput: (AudioOutputDevice) -> Unit,
    onOpenBluetoothSettings: () -> Unit,
    onDismiss: () -> Unit,
) {
    val tokens = MagicTheme.tokens
    val colors = MaterialTheme.colorScheme
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        shape = RoundedCornerShape(topStart = tokens.radii.sheet, topEnd = tokens.radii.sheet),
        containerColor = colors.surfaceContainerHigh,
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = tokens.spacing.screenMargin)
                .padding(bottom = 28.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            Text(
                text = "Salida de audio",
                style = MaterialTheme.typography.titleLarge,
                color = colors.onSurface,
            )
            outputs.forEach { device ->
                Surface(
                    onClick = { onSelectOutput(device) },
                    shape = RoundedCornerShape(tokens.radii.listItem),
                    color = if (device.isActive) tokens.semantic.selectedContainer else tokens.semantic.cardSurface,
                    border = BorderStroke(1.dp, if (device.isActive) colors.primary else tokens.semantic.divider),
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 14.dp, vertical = 12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Icon(
                            imageVector = if (device.isBluetooth) Icons.Rounded.BluetoothAudio else Icons.Rounded.Speaker,
                            contentDescription = null,
                            tint = if (device.isActive) colors.primary else colors.onSurfaceVariant,
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = device.name,
                                style = MaterialTheme.typography.titleSmall,
                                color = colors.onSurface,
                            )
                            Text(
                                text = device.typeLabel,
                                style = MaterialTheme.typography.bodySmall,
                                color = colors.onSurfaceVariant,
                            )
                        }
                        if (device.isActive) {
                            Text(
                                text = "Activa",
                                style = MaterialTheme.typography.labelMedium,
                                color = colors.primary,
                            )
                        }
                    }
                }
            }
            TextButton(
                onClick = onOpenBluetoothSettings,
                modifier = Modifier.align(Alignment.End),
            ) {
                Icon(Icons.AutoMirrored.Rounded.OpenInNew, contentDescription = null, modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(6.dp))
                Text("Conectar Bluetooth en Ajustes")
            }
        }
    }
}
