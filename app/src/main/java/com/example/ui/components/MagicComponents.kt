package com.example.ui.components

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
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
import androidx.compose.material.icons.rounded.Forward10
import androidx.compose.material.icons.rounded.GraphicEq
import androidx.compose.material.icons.automirrored.rounded.OpenInNew
import androidx.compose.material.icons.rounded.Pause
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.Replay10
import androidx.compose.material.icons.rounded.SkipNext
import androidx.compose.material.icons.rounded.SkipPrevious
import androidx.compose.material.icons.rounded.SwapHoriz
import androidx.compose.material.icons.automirrored.rounded.VolumeOff
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.example.R
import com.example.data.CustomControlAction
import com.example.data.UnifiedMediaState
import com.example.data.WidgetConfig
import com.example.data.WidgetStyle
import com.example.ui.theme.HonorCobalt
import com.example.ui.theme.HonorCyan

@Composable
fun MagicGlassCard(
    modifier: Modifier = Modifier,
    cornerRadius: Dp = 26.dp,
    highlightBorder: Boolean = false,
    content: @Composable ColumnScope.() -> Unit,
) {
    val borderBrush = if (highlightBorder) {
        Brush.linearGradient(listOf(HonorCyan.copy(alpha = 0.65f), HonorCobalt.copy(alpha = 0.35f)))
    } else {
        Brush.linearGradient(
            listOf(
                Color.White.copy(alpha = 0.16f),
                Color.White.copy(alpha = 0.04f),
            ),
        )
    }

    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(cornerRadius),
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.88f),
        border = BorderStroke(1.dp, borderBrush),
        tonalElevation = 4.dp,
    ) {
        Column(
            modifier = Modifier.padding(18.dp),
            content = content,
        )
    }
}

@Composable
fun MagicCapsuleBanner(
    media: UnifiedMediaState,
    onPlayPause: () -> Unit,
    onBannerClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Surface(
        modifier = modifier
            .fillMaxWidth()
            .clip(CircleShape)
            .clickable(onClick = onBannerClick)
            .testTag("magic_capsule_banner"),
        shape = CircleShape,
        color = Color(0xFF070C18),
        border = BorderStroke(
            width = 1.dp,
            brush = Brush.horizontalGradient(
                listOf(
                    if (media.isPlaying) HonorCyan.copy(alpha = 0.75f) else Color.White.copy(alpha = 0.16f),
                    media.service.accentColor.copy(alpha = 0.55f),
                ),
            ),
        ),
        shadowElevation = 10.dp,
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Box(
                modifier = Modifier
                    .size(34.dp)
                    .clip(CircleShape)
                    .background(media.service.accentColor.copy(alpha = 0.18f)),
                contentAlignment = Alignment.Center,
            ) {
                if (media.isPlaying) {
                    AnimatedEqualizerBars(color = HonorCyan)
                } else {
                    Icon(
                        imageVector = Icons.Rounded.GraphicEq,
                        contentDescription = null,
                        tint = media.service.accentColor,
                        modifier = Modifier.size(18.dp),
                    )
                }
            }

            Spacer(modifier = Modifier.width(10.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = if (media.hasActiveMedia) media.title else "Magic Capsule lista",
                    style = MaterialTheme.typography.labelLarge,
                    color = Color.White,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Text(
                    text = if (media.hasActiveMedia) {
                        "${media.service.displayName} • ${media.artist}"
                    } else {
                        "Spotify • YT Music • Amazon Music"
                    },
                    style = MaterialTheme.typography.labelSmall,
                    color = HonorCyan.copy(alpha = 0.9f),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }

            Box(
                modifier = Modifier
                    .size(38.dp)
                    .clip(CircleShape)
                    .background(Brush.linearGradient(listOf(HonorCyan, HonorCobalt)))
                    .clickable(onClick = onPlayPause)
                    .testTag("capsule_play_pause_btn"),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    imageVector = if (media.isPlaying) Icons.Rounded.Pause else Icons.Rounded.PlayArrow,
                    contentDescription = "Alternar reproducción en cápsula",
                    tint = Color.White,
                    modifier = Modifier.size(20.dp),
                )
            }
        }
    }
}

@Composable
fun AnimatedEqualizerBars(
    color: Color,
    modifier: Modifier = Modifier,
) {
    val transition = rememberInfiniteTransition(label = "eq_bars")
    val bar1 by transition.animateFloat(
        initialValue = 0.3f,
        targetValue = 1.0f,
        animationSpec = infiniteRepeatable(
            tween(420, easing = FastOutSlowInEasing),
            RepeatMode.Reverse,
        ),
        label = "b1",
    )
    val bar2 by transition.animateFloat(
        initialValue = 0.9f,
        targetValue = 0.35f,
        animationSpec = infiniteRepeatable(
            tween(540, easing = FastOutSlowInEasing),
            RepeatMode.Reverse,
        ),
        label = "b2",
    )
    val bar3 by transition.animateFloat(
        initialValue = 0.45f,
        targetValue = 0.95f,
        animationSpec = infiniteRepeatable(
            tween(360, easing = FastOutSlowInEasing),
            RepeatMode.Reverse,
        ),
        label = "b3",
    )

    Row(
        modifier = modifier.height(16.dp),
        horizontalArrangement = Arrangement.spacedBy(2.5.dp),
        verticalAlignment = Alignment.Bottom,
    ) {
        listOf(bar1, bar2, bar3).forEach { heightFraction ->
            Box(
                modifier = Modifier
                    .width(3.dp)
                    .height((14f * heightFraction).dp)
                    .clip(RoundedCornerShape(2.dp))
                    .background(color),
            )
        }
    }
}

@Composable
fun WidgetPreviewCard(
    media: UnifiedMediaState,
    config: WidgetConfig,
    isCapsuleMode: Boolean,
    onPlayPause: () -> Unit,
    onPrev: () -> Unit,
    onNext: () -> Unit,
    onCustomAction: (CustomControlAction) -> Unit,
    onOpenSource: () -> Unit,
) {
    val isLight = config.style == WidgetStyle.PORCELAIN_LIGHT
    val radius = config.cornerRadiusDp.dp
    val bgBrush = when (config.style) {
        WidgetStyle.CAPSULE_DARK -> Brush.linearGradient(
            listOf(
                Color(0xFF0C1426).copy(alpha = config.glassOpacity),
                Color(0xFF132240).copy(alpha = config.glassOpacity),
            ),
        )
        WidgetStyle.FROSTED_GLASS -> Brush.linearGradient(
            listOf(
                Color(0xFF161F36).copy(alpha = config.glassOpacity),
                Color(0xFF0A1120).copy(alpha = config.glassOpacity),
            ),
        )
        WidgetStyle.PORCELAIN_LIGHT -> Brush.linearGradient(
            listOf(
                Color(0xFFF9FBFF).copy(alpha = config.glassOpacity),
                Color(0xFFE7EFFC).copy(alpha = config.glassOpacity),
            ),
        )
        WidgetStyle.DYNAMIC_NEON -> Brush.linearGradient(
            listOf(
                Color(0xFF08243E).copy(alpha = config.glassOpacity),
                Color(0xFF123C73).copy(alpha = config.glassOpacity),
            ),
        )
    }

    val borderColor = when (config.style) {
        WidgetStyle.DYNAMIC_NEON -> HonorCyan.copy(alpha = 0.72f)
        WidgetStyle.PORCELAIN_LIGHT -> HonorCobalt.copy(alpha = 0.25f)
        else -> Color.White.copy(alpha = 0.18f)
    }

    val titleColor = if (isLight) Color(0xFF0F172A) else Color.White
    val subtitleColor = if (isLight) Color(0xFF475569) else Color(0xFFB4C5E0)
    val accentColor = if (isLight) HonorCobalt else HonorCyan

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(radius))
            .background(bgBrush)
            .border(1.2.dp, borderColor, RoundedCornerShape(radius))
            .padding(if (isCapsuleMode) 12.dp else 16.dp),
    ) {
        if (isCapsuleMode) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth(),
            ) {
                WidgetArtworkThumb(media = media, size = 46.dp, radius = 14.dp)
                Spacer(modifier = Modifier.width(10.dp))
                Column(modifier = Modifier.weight(1f)) {
                    if (config.showServiceBadge) {
                        Text(
                            text = if (config.showMagicCapsuleHeader) {
                                "MAGIC CAPSULE • ${media.service.displayName.uppercase()}"
                            } else {
                                media.service.displayName.uppercase()
                            },
                            style = MaterialTheme.typography.labelSmall,
                            color = accentColor,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                    }
                    Text(
                        text = media.title,
                        style = MaterialTheme.typography.titleMedium,
                        color = titleColor,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                    Text(
                        text = media.artist,
                        style = MaterialTheme.typography.bodyMedium,
                        color = subtitleColor,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
                IconButton(onClick = onPrev) {
                    Icon(Icons.Rounded.SkipPrevious, contentDescription = "Anterior", tint = titleColor)
                }
                Box(
                    modifier = Modifier
                        .size(42.dp)
                        .clip(CircleShape)
                        .background(Brush.linearGradient(listOf(HonorCyan, HonorCobalt)))
                        .clickable(onClick = onPlayPause),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(
                        imageVector = if (media.isPlaying) Icons.Rounded.Pause else Icons.Rounded.PlayArrow,
                        contentDescription = "Reproducir o pausar",
                        tint = Color.White,
                    )
                }
                IconButton(onClick = onNext) {
                    Icon(Icons.Rounded.SkipNext, contentDescription = "Siguiente", tint = titleColor)
                }
            }
        } else {
            Column(modifier = Modifier.fillMaxWidth()) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    WidgetArtworkThumb(media = media, size = 56.dp, radius = 16.dp)
                    Spacer(modifier = Modifier.width(12.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        if (config.showServiceBadge) {
                            Surface(
                                shape = RoundedCornerShape(10.dp),
                                color = accentColor.copy(alpha = 0.14f),
                            ) {
                                Text(
                                    text = if (config.showMagicCapsuleHeader) {
                                        "MAGIC OS 10 • ${media.service.displayName.uppercase()}"
                                    } else {
                                        media.service.displayName.uppercase()
                                    },
                                    style = MaterialTheme.typography.labelSmall,
                                    color = accentColor,
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp),
                                )
                            }
                            Spacer(modifier = Modifier.height(4.dp))
                        }
                        Text(
                            text = media.title,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = titleColor,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                        Text(
                            text = media.artist,
                            style = MaterialTheme.typography.bodyMedium,
                            color = subtitleColor,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                    }
                    IconButton(onClick = onOpenSource) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Rounded.OpenInNew,
                            contentDescription = "Abrir reproductor",
                            tint = accentColor,
                        )
                    }
                }

                if (config.showProgressBar) {
                    Spacer(modifier = Modifier.height(12.dp))
                    LinearProgressIndicator(
                        progress = { if (media.hasActiveMedia) media.progressFraction else 0.35f },
                        color = accentColor,
                        trackColor = titleColor.copy(alpha = 0.14f),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(4.dp)
                            .clip(CircleShape),
                    )
                }

                Spacer(modifier = Modifier.height(10.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    IconButton(onClick = { onCustomAction(config.leftCustomAction) }) {
                        Icon(
                            imageVector = customActionIcon(config.leftCustomAction),
                            contentDescription = config.leftCustomAction.label,
                            tint = subtitleColor,
                        )
                    }
                    IconButton(onClick = onPrev) {
                        Icon(
                            imageVector = Icons.Rounded.SkipPrevious,
                            contentDescription = "Anterior",
                            tint = titleColor,
                            modifier = Modifier.size(28.dp),
                        )
                    }
                    Box(
                        modifier = Modifier
                            .size(48.dp)
                            .clip(CircleShape)
                            .background(Brush.linearGradient(listOf(HonorCyan, HonorCobalt)))
                            .clickable(onClick = onPlayPause),
                        contentAlignment = Alignment.Center,
                    ) {
                        Icon(
                            imageVector = if (media.isPlaying) Icons.Rounded.Pause else Icons.Rounded.PlayArrow,
                            contentDescription = "Reproducir o pausar",
                            tint = Color.White,
                            modifier = Modifier.size(28.dp),
                        )
                    }
                    IconButton(onClick = onNext) {
                        Icon(
                            imageVector = Icons.Rounded.SkipNext,
                            contentDescription = "Siguiente",
                            tint = titleColor,
                            modifier = Modifier.size(28.dp),
                        )
                    }
                    IconButton(onClick = { onCustomAction(config.rightCustomAction) }) {
                        Icon(
                            imageVector = customActionIcon(config.rightCustomAction),
                            contentDescription = config.rightCustomAction.label,
                            tint = subtitleColor,
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun WidgetArtworkThumb(
    media: UnifiedMediaState,
    size: Dp,
    radius: Dp,
) {
    Box(
        modifier = Modifier
            .size(size)
            .clip(RoundedCornerShape(radius))
            .border(1.dp, Color.White.copy(alpha = 0.18f), RoundedCornerShape(radius)),
    ) {
        if (media.albumArt != null) {
            Image(
                bitmap = media.albumArt.asImageBitmap(),
                contentDescription = "Carátula del álbum",
                contentScale = ContentScale.Crop,
                modifier = Modifier.matchParentSize(),
            )
        } else {
            Image(
                painter = painterResource(id = R.drawable.img_magic_album_art),
                contentDescription = "Carátula MagicOS 10",
                contentScale = ContentScale.Crop,
                modifier = Modifier.matchParentSize(),
            )
        }
    }
}

fun customActionIcon(action: CustomControlAction): ImageVector = when (action) {
    CustomControlAction.REWIND_15 -> Icons.Rounded.Replay10
    CustomControlAction.FORWARD_15 -> Icons.Rounded.Forward10
    CustomControlAction.SWITCH_SOURCE -> Icons.Rounded.SwapHoriz
    CustomControlAction.OPEN_PLAYER -> Icons.AutoMirrored.Rounded.OpenInNew
    CustomControlAction.MUTE_AUDIO -> Icons.AutoMirrored.Rounded.VolumeOff
}

fun formatTimeMs(ms: Long): String {
    val totalSec = (ms / 1000L).coerceAtLeast(0L)
    val min = totalSec / 60L
    val sec = totalSec % 60L
    return "%d:%02d".format(min, sec)
}
