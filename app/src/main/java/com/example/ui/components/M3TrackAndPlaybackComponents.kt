package com.example.ui.components

import android.graphics.Bitmap
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.OpenInNew
import androidx.compose.material.icons.automirrored.rounded.PlaylistAdd
import androidx.compose.material.icons.automirrored.rounded.VolumeUp
import androidx.compose.material.icons.rounded.Bedtime
import androidx.compose.material.icons.rounded.DragHandle
import androidx.compose.material.icons.rounded.Forward10
import androidx.compose.material.icons.rounded.MoreVert
import androidx.compose.material.icons.rounded.Person
import androidx.compose.material.icons.rounded.Repeat
import androidx.compose.material.icons.rounded.Replay10
import androidx.compose.material.icons.rounded.Share
import androidx.compose.material.icons.rounded.Shuffle
import androidx.compose.material.icons.rounded.SkipNext
import androidx.compose.material.icons.rounded.SkipPrevious
import androidx.compose.material.icons.rounded.SpeakerGroup
import androidx.compose.material3.BottomSheetDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.designsystem.MagicTheme
import com.example.designsystem.expressive.MagicLinearWavyProgress
import com.example.designsystem.expressive.MagicPlayPauseMorphButton

enum class TrackItemVariant {
    DEFAULT,
    HIGHLIGHTED_CURRENT,
    NUMBERED,
    QUEUE_DRAG,
}

@Immutable
data class TrackListItemModel(
    val id: String,
    val title: String,
    val artist: String,
    val durationLabel: String = "",
    val trackNumber: Int = 1,
    val bitmap: Bitmap? = null,
    val artworkModel: Any? = null,
    val formatBadge: String = "",
    val accentTint: Color? = null,
)

@Composable
fun MagicTrackListItem(
    item: TrackListItemModel,
    variant: TrackItemVariant,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    onMoreClick: (() -> Unit)? = null,
) {
    val tokens = MagicTheme.tokens
    val colors = MaterialTheme.colorScheme
    val isCurrent = variant == TrackItemVariant.HIGHLIGHTED_CURRENT

    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(tokens.radii.listItem),
        color = if (isCurrent) colors.primaryContainer.copy(alpha = 0.72f) else colors.surfaceContainer,
        border = if (isCurrent) BorderStroke(1.5.dp, colors.primary) else null,
        modifier = modifier.fillMaxWidth(),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            when (variant) {
                TrackItemVariant.NUMBERED -> {
                    Text(
                        text = item.trackNumber.toString(),
                        style = MaterialTheme.typography.titleSmall,
                        color = colors.onSurfaceVariant,
                        modifier = Modifier.width(28.dp),
                    )
                }
                TrackItemVariant.QUEUE_DRAG -> {
                    Icon(
                        imageVector = Icons.Rounded.DragHandle,
                        contentDescription = "Reordenar pista",
                        tint = colors.onSurfaceVariant,
                        modifier = Modifier
                            .padding(end = 10.dp)
                            .size(20.dp),
                    )
                }
                else -> Unit
            }

            MagicAlbumArtwork(
                bitmap = item.bitmap,
                artworkModel = item.artworkModel ?: item.bitmap,
                contentDescription = item.title,
                cornerRadius = 12.dp,
                fallbackTint = item.accentTint,
                modifier = Modifier.size(48.dp),
            )

            Spacer(modifier = Modifier.width(12.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = item.title,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = if (isCurrent) FontWeight.Bold else FontWeight.SemiBold,
                    color = if (isCurrent) colors.onPrimaryContainer else colors.onSurface,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                ) {
                    Text(
                        text = if (item.durationLabel.isNotBlank()) {
                            "${item.artist} • ${item.durationLabel}"
                        } else {
                            item.artist
                        },
                        style = MaterialTheme.typography.bodySmall,
                        color = if (isCurrent) colors.onPrimaryContainer.copy(alpha = 0.8f) else colors.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f, fill = false),
                    )
                    if (item.formatBadge.isNotBlank()) {
                        Surface(
                            shape = RoundedCornerShape(6.dp),
                            color = colors.tertiaryContainer,
                        ) {
                            Text(
                                text = item.formatBadge,
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.SemiBold,
                                color = colors.onTertiaryContainer,
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 1.dp),
                            )
                        }
                    }
                }
            }

            if (isCurrent) {
                AnimatedEqualizerBars(
                    color = colors.primary,
                    modifier = Modifier.padding(horizontal = 8.dp),
                )
            }

            if (onMoreClick != null) {
                IconButton(onClick = onMoreClick) {
                    Icon(
                        imageVector = Icons.Rounded.MoreVert,
                        contentDescription = "Más opciones",
                        tint = if (isCurrent) colors.onPrimaryContainer else colors.onSurfaceVariant,
                    )
                }
            }
        }
    }
}

@Composable
fun MagicPlaybackControls(
    positionMs: Long,
    durationMs: Long,
    isPlaying: Boolean,
    shuffleEnabled: Boolean,
    repeatEnabled: Boolean,
    onSeekFraction: (Float) -> Unit,
    onPlayPause: () -> Unit,
    onPrevious: () -> Unit,
    onNext: () -> Unit,
    onToggleShuffle: () -> Unit,
    onToggleRepeat: () -> Unit,
    modifier: Modifier = Modifier,
    onRewind15: (() -> Unit)? = null,
    onForward15: (() -> Unit)? = null,
    onVolumeClick: (() -> Unit)? = null,
    onOutputClick: (() -> Unit)? = null,
) {
    val tokens = MagicTheme.tokens
    val colors = MaterialTheme.colorScheme
    val progress = if (durationMs > 0L) {
        (positionMs.toFloat() / durationMs.toFloat()).coerceIn(0f, 1f)
    } else {
        0f
    }

    Column(modifier = modifier.fillMaxWidth()) {
        if (tokens.useExpressiveM3 && isPlaying && !tokens.motion.reduceAnimations) {
            MagicLinearWavyProgress(
                progress = progress,
                isPlaying = true,
                useExpressiveM3 = true,
                reduceAnimations = false,
                color = colors.primary,
                trackColor = colors.surfaceContainerHighest,
                modifier = Modifier.fillMaxWidth(),
            )
        }
        Slider(
            value = progress,
            onValueChange = onSeekFraction,
            colors = SliderDefaults.colors(
                thumbColor = colors.primary,
                activeTrackColor = colors.primary,
                inactiveTrackColor = colors.surfaceContainerHighest,
            ),
            modifier = Modifier.fillMaxWidth(),
        )

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 4.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Text(
                text = formatTimeMs(positionMs),
                style = MaterialTheme.typography.labelMedium,
                color = colors.onSurfaceVariant,
            )
            Text(
                text = if (durationMs > 0L) formatTimeMs(durationMs) else "0:00",
                style = MaterialTheme.typography.labelMedium,
                color = colors.onSurfaceVariant,
            )
        }

        Spacer(modifier = Modifier.height(14.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            IconButton(onClick = onToggleShuffle) {
                Icon(
                    imageVector = Icons.Rounded.Shuffle,
                    contentDescription = "Aleatorio",
                    tint = if (shuffleEnabled) colors.primary else colors.onSurfaceVariant,
                )
            }
            IconButton(
                onClick = onPrevious,
                modifier = Modifier.size(48.dp),
            ) {
                Icon(
                    imageVector = Icons.Rounded.SkipPrevious,
                    contentDescription = "Anterior",
                    tint = colors.onSurface,
                    modifier = Modifier.size(34.dp),
                )
            }
            MagicPlayPauseMorphButton(
                isPlaying = isPlaying,
                useExpressiveM3 = tokens.useExpressiveM3,
                onClick = onPlayPause,
                size = 76.dp,
                containerColor = colors.primary,
                contentColor = colors.onPrimary,
                modifier = Modifier.testTag("playback_main_play_pause"),
            )
            IconButton(
                onClick = onNext,
                modifier = Modifier.size(48.dp),
            ) {
                Icon(
                    imageVector = Icons.Rounded.SkipNext,
                    contentDescription = "Siguiente",
                    tint = colors.onSurface,
                    modifier = Modifier.size(34.dp),
                )
            }
            IconButton(onClick = onToggleRepeat) {
                Icon(
                    imageVector = Icons.Rounded.Repeat,
                    contentDescription = "Repetir",
                    tint = if (repeatEnabled) colors.primary else colors.onSurfaceVariant,
                )
            }
        }

        if (onRewind15 != null || onForward15 != null || onVolumeClick != null || onOutputClick != null) {
            Spacer(modifier = Modifier.height(16.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                if (onRewind15 != null) {
                    QuickDeckActionChip(
                        icon = Icons.Rounded.Replay10,
                        label = "-10s",
                        onClick = onRewind15,
                    )
                }
                if (onForward15 != null) {
                    QuickDeckActionChip(
                        icon = Icons.Rounded.Forward10,
                        label = "+10s",
                        onClick = onForward15,
                    )
                }
                if (onVolumeClick != null) {
                    QuickDeckActionChip(
                        icon = Icons.AutoMirrored.Rounded.VolumeUp,
                        label = "Volumen",
                        onClick = onVolumeClick,
                    )
                }
                if (onOutputClick != null) {
                    QuickDeckActionChip(
                        icon = Icons.Rounded.SpeakerGroup,
                        label = "Salida",
                        onClick = onOutputClick,
                    )
                }
            }
        }
    }
}

@Composable
private fun QuickDeckActionChip(
    icon: ImageVector,
    label: String,
    onClick: () -> Unit,
) {
    val colors = MaterialTheme.colorScheme
    Surface(
        onClick = onClick,
        shape = CircleShape,
        color = colors.surfaceContainerHigh,
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
        ) {
            Icon(
                imageVector = icon,
                contentDescription = label,
                tint = colors.onSurfaceVariant,
                modifier = Modifier.size(16.dp),
            )
            Text(
                text = label,
                style = MaterialTheme.typography.labelMedium,
                color = colors.onSurface,
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MagicContextMenuSheet(
    title: String,
    artist: String,
    bitmap: Bitmap?,
    onDismiss: () -> Unit,
    onActionSelected: (String) -> Unit,
    artworkModel: Any? = null,
) {
    val tokens = MagicTheme.tokens
    val colors = MaterialTheme.colorScheme
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        shape = RoundedCornerShape(topStart = tokens.radii.sheet, topEnd = tokens.radii.sheet),
        containerColor = colors.surfaceContainerHigh,
        dragHandle = { BottomSheetDefaults.DragHandle(color = colors.onSurfaceVariant.copy(alpha = 0.5f)) },
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = tokens.spacing.screenMargin)
                .padding(bottom = 28.dp),
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                MagicAlbumArtwork(
                    bitmap = bitmap,
                    artworkModel = artworkModel ?: bitmap,
                    contentDescription = title,
                    cornerRadius = 14.dp,
                    modifier = Modifier.size(54.dp),
                )
                Spacer(modifier = Modifier.width(14.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = title,
                        style = MaterialTheme.typography.titleMedium,
                        color = colors.onSurface,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                    Text(
                        text = artist,
                        style = MaterialTheme.typography.bodyMedium,
                        color = colors.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))
            HorizontalDivider(color = tokens.semantic.divider)
            Spacer(modifier = Modifier.height(8.dp))

            val actions = listOf(
                Icons.AutoMirrored.Rounded.PlaylistAdd to "Añadir a playlist",
                Icons.Rounded.Share to "Compartir pista",
                Icons.Rounded.Person to "Ver artista",
                Icons.AutoMirrored.Rounded.OpenInNew to "Abrir en app de origen",
            )
            actions.forEach { (icon, label) ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(tokens.radii.listItem))
                        .clickable {
                            onActionSelected(label)
                            onDismiss()
                        }
                        .padding(horizontal = 8.dp, vertical = 14.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Icon(
                        imageVector = icon,
                        contentDescription = null,
                        tint = colors.primary,
                        modifier = Modifier.size(22.dp),
                    )
                    Spacer(modifier = Modifier.width(16.dp))
                    Text(
                        text = label,
                        style = MaterialTheme.typography.bodyLarge,
                        color = colors.onSurface,
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MagicLyricsSheet(
    title: String,
    artist: String,
    lines: List<String>,
    activeLineIndex: Int,
    onDismiss: () -> Unit,
) {
    val tokens = MagicTheme.tokens
    val colors = MaterialTheme.colorScheme
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        shape = RoundedCornerShape(topStart = tokens.radii.sheet, topEnd = tokens.radii.sheet),
        containerColor = colors.surfaceContainer,
        dragHandle = { BottomSheetDefaults.DragHandle(color = colors.onSurfaceVariant.copy(alpha = 0.5f)) },
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = tokens.spacing.lg)
                .padding(bottom = 32.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Text(
                text = "Letras • $title",
                style = MaterialTheme.typography.titleMedium,
                color = colors.primary,
            )
            Text(
                text = artist,
                style = MaterialTheme.typography.bodySmall,
                color = colors.onSurfaceVariant,
            )
            Spacer(modifier = Modifier.height(6.dp))
            lines.forEachIndexed { idx, line ->
                val isActive = idx == activeLineIndex
                Text(
                    text = line,
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = if (isActive) FontWeight.Bold else FontWeight.Medium,
                    color = if (isActive) colors.onSurface else colors.onSurfaceVariant.copy(alpha = 0.65f),
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MagicSleepTimerSheet(
    selectedMinutes: Int,
    fadeOutEnabled: Boolean,
    onSelectMinutes: (Int) -> Unit,
    onToggleFadeOut: (Boolean) -> Unit,
    onDismiss: () -> Unit,
) {
    val tokens = MagicTheme.tokens
    val colors = MaterialTheme.colorScheme
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        shape = RoundedCornerShape(topStart = tokens.radii.sheet, topEnd = tokens.radii.sheet),
        containerColor = colors.surfaceContainerHigh,
        dragHandle = { BottomSheetDefaults.DragHandle(color = colors.onSurfaceVariant.copy(alpha = 0.5f)) },
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
                Icon(
                    imageVector = Icons.Rounded.Bedtime,
                    contentDescription = null,
                    tint = colors.primary,
                )
                Text(
                    text = "Temporizador de apagado",
                    style = MaterialTheme.typography.titleMedium,
                    color = colors.onSurface,
                )
            }
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                text = if (selectedMinutes > 0) "$selectedMinutes min" else "Desactivado",
                style = MaterialTheme.typography.displayMedium,
                fontWeight = FontWeight.Bold,
                color = colors.primary,
            )
            Spacer(modifier = Modifier.height(14.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                listOf(15, 30, 45, 60).forEach { min ->
                    MagicFilterChip(
                        label = "${min}m",
                        selected = selectedMinutes == min,
                        onClick = { onSelectMinutes(min) },
                    )
                }
            }
            Spacer(modifier = Modifier.height(16.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = "Fundido de volumen suave",
                    style = MaterialTheme.typography.bodyLarge,
                    color = colors.onSurface,
                )
                Switch(
                    checked = fadeOutEnabled,
                    onCheckedChange = onToggleFadeOut,
                )
            }
        }
    }
}
