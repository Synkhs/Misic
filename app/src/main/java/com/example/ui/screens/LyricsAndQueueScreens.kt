package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.automirrored.rounded.QueueMusic
import androidx.compose.material.icons.rounded.ArrowDownward
import androidx.compose.material.icons.rounded.ArrowUpward
import androidx.compose.material.icons.rounded.Lyrics
import androidx.compose.material.icons.rounded.Pause
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.R
import com.example.designsystem.ArtworkPaletteColors
import com.example.designsystem.MagicTheme
import com.example.designsystem.buildArtworkTonalSurface
import com.example.ui.MagicMusicUiState
import com.example.ui.components.MagicMiniPlayer
import com.example.ui.components.MagicSurfaceCard
import com.example.ui.components.MagicTrackListItem
import com.example.ui.components.TrackItemVariant
import com.example.ui.components.TrackListItemModel
import com.example.ui.components.formatTimeMs

@Composable
fun LyricsScreen(
    state: MagicMusicUiState,
    artworkColors: ArtworkPaletteColors,
    positionMs: Long = state.media.positionMs,
    onBack: () -> Unit,
    onPlayPause: () -> Unit,
    onStartLocalEngine: () -> Unit,
) {
    val tokens = MagicTheme.tokens
    val colors = MaterialTheme.colorScheme
    val media = state.media
    val lyricsData = media.lyrics
    val listState = rememberLazyListState()

    val activeIndex = if (lyricsData != null && lyricsData.lines.isNotEmpty()) {
        if (lyricsData.isSynced) {
            lyricsData.lines.indexOfLast { it.timestampMs <= positionMs }.coerceAtLeast(0)
        } else {
            0
        }
    } else {
        -1
    }

    LaunchedEffect(activeIndex, tokens.lyricsAutoScroll) {
        if (tokens.lyricsAutoScroll && activeIndex >= 0 && !tokens.motion.reduceAnimations) {
            runCatching { listState.animateScrollToItem((activeIndex - 1).coerceAtLeast(0)) }
        }
    }

    val bgColor = if (tokens.colorFromArtwork) {
        buildArtworkTonalSurface(
            artworkColors = artworkColors,
            baseBackground = colors.background,
            isDark = tokens.isDark,
        )
    } else {
        tokens.backgroundColor
    }

    val baseLineSizeSp = MaterialTheme.typography.headlineSmall.fontSize.value * tokens.lyricsFontScale

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(bgColor)
            .statusBarsPadding()
            .navigationBarsPadding(),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = tokens.spacing.sm, vertical = tokens.spacing.xs),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            IconButton(
                onClick = onBack,
                modifier = Modifier.testTag("lyrics_back_btn"),
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Rounded.ArrowBack,
                    contentDescription = stringResource(R.string.action_back),
                    tint = colors.onBackground,
                )
            }
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = stringResource(R.string.action_lyrics),
                    style = MaterialTheme.typography.titleLarge,
                    color = colors.onBackground,
                )
                Text(
                    text = "${media.title} • ${media.artist}",
                    style = MaterialTheme.typography.bodySmall,
                    color = colors.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
            IconButton(onClick = onPlayPause) {
                Icon(
                    imageVector = if (media.isPlaying) Icons.Rounded.Pause else Icons.Rounded.PlayArrow,
                    contentDescription = stringResource(R.string.action_play_pause),
                    tint = colors.primary,
                )
            }
        }

        if (lyricsData != null && lyricsData.lines.isNotEmpty()) {
            LazyColumn(
                state = listState,
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .testTag("lyrics_lines_list"),
                contentPadding = PaddingValues(
                    horizontal = tokens.spacing.lg,
                    vertical = tokens.spacing.md,
                ),
                verticalArrangement = Arrangement.spacedBy(18.dp),
            ) {
                itemsIndexed(lyricsData.lines) { idx, line ->
                    val isActive = idx == activeIndex
                    Text(
                        text = line.text,
                        style = MaterialTheme.typography.headlineSmall.copy(
                            fontSize = baseLineSizeSp.sp,
                            lineHeight = (baseLineSizeSp * 1.32f).sp,
                        ),
                        fontWeight = if (isActive) FontWeight.Bold else FontWeight.Medium,
                        color = if (isActive) colors.onBackground else colors.onSurfaceVariant.copy(alpha = 0.48f),
                    )
                }
                if (lyricsData.sourceNote.isNotBlank()) {
                    item {
                        Spacer(modifier = Modifier.height(12.dp))
                        Text(
                            text = lyricsData.sourceNote,
                            style = MaterialTheme.typography.bodySmall,
                            color = colors.onSurfaceVariant,
                        )
                    }
                }
            }
        } else {
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .padding(horizontal = tokens.spacing.lg),
                contentAlignment = Alignment.Center,
            ) {
                MagicSurfaceCard(modifier = Modifier.testTag("lyrics_empty_card")) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(10.dp),
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        Box(
                            modifier = Modifier
                                .size(56.dp)
                                .clip(CircleShape)
                                .background(colors.primaryContainer),
                            contentAlignment = Alignment.Center,
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.Lyrics,
                                contentDescription = null,
                                tint = colors.onPrimaryContainer,
                                modifier = Modifier.size(28.dp),
                            )
                        }
                        Text(
                            text = stringResource(R.string.lyrics_empty_title),
                            style = MaterialTheme.typography.titleMedium,
                            color = colors.onSurface,
                        )
                        Text(
                            text = "La app de origen (${media.service.displayName}) no publica letras sincronizadas en su sesión MediaSession. Puedes probar las letras sincronizadas con el Motor Local Hi-Res.",
                            style = MaterialTheme.typography.bodySmall,
                            color = colors.onSurfaceVariant,
                        )
                        Button(onClick = onStartLocalEngine) {
                            Text("Probar letras en Motor Local")
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun QueueScreen(
    state: MagicMusicUiState,
    positionMs: Long = state.media.positionMs,
    durationMs: Long = state.media.durationMs,
    onBack: () -> Unit,
    onPlayPause: () -> Unit,
    onNext: () -> Unit,
    onSkipToQueueItem: (Long) -> Unit,
    onReorderLocalQueue: (Int, Int) -> Unit,
    onOpenNowPlaying: () -> Unit,
    onStartLocalEngine: () -> Unit,
) {
    val tokens = MagicTheme.tokens
    val colors = MaterialTheme.colorScheme
    val media = state.media
    val queue = media.queue
    val canReorder = media.isLocalEngine && queue.size > 1

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(tokens.backgroundColor)
            .statusBarsPadding()
            .navigationBarsPadding(),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = tokens.spacing.sm, vertical = tokens.spacing.xs),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            IconButton(
                onClick = onBack,
                modifier = Modifier.testTag("queue_back_btn"),
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Rounded.ArrowBack,
                    contentDescription = stringResource(R.string.action_back),
                    tint = colors.onBackground,
                )
            }
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = stringResource(R.string.queue_title),
                    style = MaterialTheme.typography.titleLarge,
                    color = colors.onBackground,
                )
                Text(
                    text = stringResource(R.string.queue_next_header),
                    style = MaterialTheme.typography.bodySmall,
                    color = colors.onSurfaceVariant,
                )
            }
        }

        if (queue.isNotEmpty()) {
            LazyColumn(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .testTag("queue_items_list"),
                contentPadding = PaddingValues(
                    horizontal = tokens.spacing.screenMargin,
                    vertical = tokens.spacing.sm,
                ),
                verticalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                itemsIndexed(queue, key = { index, item -> "${item.queueId}_$index" }) { index, item ->
                    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        MagicTrackListItem(
                            item = TrackListItemModel(
                                id = item.queueId.toString(),
                                title = item.title,
                                artist = item.artist,
                                durationLabel = if (item.durationMs > 0L) formatTimeMs(item.durationMs) else "",
                                trackNumber = index + 1,
                                bitmap = if (item.isCurrent) media.albumArt else null,
                                accentTint = media.service.accentColor,
                            ),
                            variant = when {
                                item.isCurrent -> TrackItemVariant.HIGHLIGHTED_CURRENT
                                canReorder -> TrackItemVariant.QUEUE_DRAG
                                else -> TrackItemVariant.NUMBERED
                            },
                            onClick = { onSkipToQueueItem(item.queueId) },
                        )
                        if (canReorder) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 8.dp),
                                horizontalArrangement = Arrangement.End,
                            ) {
                                IconButton(
                                    enabled = index > 0,
                                    onClick = { onReorderLocalQueue(index, index - 1) },
                                    modifier = Modifier.size(34.dp),
                                ) {
                                    Icon(
                                        imageVector = Icons.Rounded.ArrowUpward,
                                        contentDescription = "Subir pista",
                                        modifier = Modifier.size(18.dp),
                                    )
                                }
                                IconButton(
                                    enabled = index < queue.lastIndex,
                                    onClick = { onReorderLocalQueue(index, index + 1) },
                                    modifier = Modifier.size(34.dp),
                                ) {
                                    Icon(
                                        imageVector = Icons.Rounded.ArrowDownward,
                                        contentDescription = "Bajar pista",
                                        modifier = Modifier.size(18.dp),
                                    )
                                }
                            }
                        }
                    }
                }
            }
        } else {
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .padding(horizontal = tokens.spacing.lg),
                contentAlignment = Alignment.Center,
            ) {
                MagicSurfaceCard(modifier = Modifier.testTag("queue_empty_card")) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(10.dp),
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        Box(
                            modifier = Modifier
                                .size(56.dp)
                                .clip(CircleShape)
                                .background(colors.primaryContainer),
                            contentAlignment = Alignment.Center,
                        ) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Rounded.QueueMusic,
                                contentDescription = null,
                                tint = colors.onPrimaryContainer,
                                modifier = Modifier.size(28.dp),
                            )
                        }
                        Text(
                            text = "La cola no está expuesta por ${media.service.displayName}",
                            style = MaterialTheme.typography.titleMedium,
                            color = colors.onSurface,
                        )
                        Text(
                            text = "Cuando el reproductor externo publica su lista siguiente mediante MediaSession aparecerá aquí. El Motor Local Hi-Res sí incluye cola completa y reordenación.",
                            style = MaterialTheme.typography.bodySmall,
                            color = colors.onSurfaceVariant,
                        )
                        Button(onClick = onStartLocalEngine) {
                            Text("Ver cola del Motor Local")
                        }
                    }
                }
            }
        }

        MagicMiniPlayer(
            media = media,
            positionMs = positionMs,
            durationMs = durationMs,
            onPlayPause = onPlayPause,
            onNext = onNext,
            onClick = onOpenNowPlaying,
            modifier = Modifier.padding(horizontal = tokens.spacing.screenMargin, vertical = 12.dp),
        )
    }
}
