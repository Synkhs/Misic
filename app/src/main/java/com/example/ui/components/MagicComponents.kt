package com.example.ui.components

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
import androidx.compose.material.icons.automirrored.rounded.OpenInNew
import androidx.compose.material.icons.rounded.Pause
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.SkipNext
import androidx.compose.material.icons.rounded.SkipPrevious
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.example.data.CustomControlAction
import com.example.data.TrackHistoryEntry
import com.example.data.UnifiedMediaState
import com.example.data.WidgetConfig
import com.example.data.WidgetFormat
import com.example.data.WidgetInstanceConfig
import com.example.designsystem.MagicTheme
import com.example.designsystem.ThemePreferences
import com.example.widget.WidgetRenderModel

@Composable
fun MagicGlassCard(
    modifier: Modifier = Modifier,
    cornerRadius: Dp = MagicTheme.tokens.radii.card,
    highlightBorder: Boolean = false,
    content: @Composable ColumnScope.() -> Unit,
) {
    MagicSurfaceCard(
        modifier = modifier,
        cornerRadius = cornerRadius,
        highlightBorder = highlightBorder,
        content = content,
    )
}

@Composable
fun WidgetPreviewCard(
    media: UnifiedMediaState,
    config: WidgetConfig,
    format: WidgetFormat,
    dominantArtworkColor: Color?,
    lastHistoryTrack: TrackHistoryEntry?,
    preferredPackage: String,
    onPlayPause: () -> Unit,
    onPrev: () -> Unit,
    onNext: () -> Unit,
    onCustomAction: (CustomControlAction) -> Unit,
    onOpenSource: () -> Unit,
    instanceConfig: WidgetInstanceConfig? = null,
) {
    val colors = MaterialTheme.colorScheme
    val radius = (instanceConfig?.cornerRadiusDp ?: config.cornerRadiusDp).dp
    val palette = if (instanceConfig != null) {
        WidgetRenderModel.resolvePaletteForInstance(
            instance = instanceConfig,
            appTheme = ThemePreferences(),
            dominantArtworkArgb = dominantArtworkColor?.toArgb(),
            serviceAccentArgb = if (media.hasActiveMedia) media.service.accentColor.toArgb() else null,
            systemDynamicPrimaryArgb = colors.primary.toArgb(),
            systemIsDark = MagicTheme.tokens.isDark,
        )
    } else {
        WidgetRenderModel.resolvePalette(
            style = config.style,
            glassOpacity = config.glassOpacity,
            dominantArtworkArgb = dominantArtworkColor?.toArgb(),
            serviceAccentArgb = if (media.hasActiveMedia) media.service.accentColor.toArgb() else null,
            themePrimaryArgb = colors.primary.toArgb(),
            themeOnPrimaryArgb = colors.onPrimary.toArgb(),
        )
    }
    val effectiveConfig = instanceConfig?.toWidgetConfig() ?: config
    val copy = WidgetRenderModel.resolveDisplayCopy(
        media = media,
        config = effectiveConfig,
        lastHistoryTrack = lastHistoryTrack,
        preferredPackage = preferredPackage,
    )

    val bgColor = Color(palette.bgStartArgb)
    val borderColor = Color(palette.borderArgb)
    val titleColor = Color(palette.titleArgb)
    val subtitleColor = Color(palette.subtitleArgb)
    val accentColor = Color(palette.accentArgb)
    val playBgColor = Color(palette.playBtnBgArgb)
    val playIconColor = Color(palette.playBtnIconArgb)

    val boxModifier = Modifier
        .let { base ->
            when (format) {
                WidgetFormat.SQUARE_2X2 -> base.width(210.dp)
                WidgetFormat.MINI_2X1 -> base.width(220.dp)
                WidgetFormat.CAPSULE_4X1, WidgetFormat.CARD_4X2 -> base.fillMaxWidth()
            }
        }
        .clip(RoundedCornerShape(radius))
        .background(bgColor)
        .border(1.dp, borderColor, RoundedCornerShape(radius))
        .padding(
            when (format) {
                WidgetFormat.MINI_2X1 -> 12.dp
                WidgetFormat.CAPSULE_4X1 -> 12.dp
                WidgetFormat.SQUARE_2X2 -> 14.dp
                WidgetFormat.CARD_4X2 -> 16.dp
            },
        )

    Box(
        modifier = Modifier.fillMaxWidth(),
        contentAlignment = Alignment.Center,
    ) {
        Box(modifier = boxModifier) {
            when (format) {
                WidgetFormat.CAPSULE_4X1 -> {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        WidgetArtworkThumb(media = media, size = 46.dp, radius = 14.dp)
                        Spacer(modifier = Modifier.width(10.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            if (effectiveConfig.showServiceBadge) {
                                Text(
                                    text = copy.badgeText,
                                    style = MaterialTheme.typography.labelSmall,
                                    color = accentColor,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                )
                            }
                            Text(
                                text = copy.title,
                                style = MaterialTheme.typography.titleMedium,
                                color = titleColor,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                            )
                            Text(
                                text = copy.subtitle,
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
                                .background(playBgColor)
                                .clickable(onClick = onPlayPause),
                            contentAlignment = Alignment.Center,
                        ) {
                            Icon(
                                imageVector = if (media.isPlaying) Icons.Rounded.Pause else Icons.Rounded.PlayArrow,
                                contentDescription = "Reproducir o pausar",
                                tint = playIconColor,
                            )
                        }
                        IconButton(onClick = onNext) {
                            Icon(Icons.Rounded.SkipNext, contentDescription = "Siguiente", tint = titleColor)
                        }
                    }
                }

                WidgetFormat.CARD_4X2 -> {
                    Column(modifier = Modifier.fillMaxWidth()) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.fillMaxWidth(),
                        ) {
                            WidgetArtworkThumb(media = media, size = 56.dp, radius = 16.dp)
                            Spacer(modifier = Modifier.width(12.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                if (effectiveConfig.showServiceBadge) {
                                    Surface(
                                        shape = CircleShape,
                                        color = Color(palette.badgeBgArgb),
                                    ) {
                                        Text(
                                            text = copy.badgeText,
                                            style = MaterialTheme.typography.labelSmall,
                                            color = Color(palette.badgeTextArgb),
                                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp),
                                        )
                                    }
                                    Spacer(modifier = Modifier.height(4.dp))
                                }
                                Text(
                                    text = copy.title,
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = titleColor,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                )
                                Text(
                                    text = copy.subtitle,
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

                        if (effectiveConfig.showProgressBar) {
                            Spacer(modifier = Modifier.height(12.dp))
                            LinearProgressIndicator(
                                progress = { if (media.hasActiveMedia) media.progressFraction else 0.35f },
                                color = accentColor,
                                trackColor = titleColor.copy(alpha = 0.16f),
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
                            IconButton(onClick = { onCustomAction(effectiveConfig.leftCustomAction) }) {
                                Icon(
                                    imageVector = customActionIcon(effectiveConfig.leftCustomAction),
                                    contentDescription = effectiveConfig.leftCustomAction.label,
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
                                    .background(playBgColor)
                                    .clickable(onClick = onPlayPause),
                                contentAlignment = Alignment.Center,
                            ) {
                                Icon(
                                    imageVector = if (media.isPlaying) Icons.Rounded.Pause else Icons.Rounded.PlayArrow,
                                    contentDescription = "Reproducir o pausar",
                                    tint = playIconColor,
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
                            IconButton(onClick = { onCustomAction(effectiveConfig.rightCustomAction) }) {
                                Icon(
                                    imageVector = customActionIcon(effectiveConfig.rightCustomAction),
                                    contentDescription = effectiveConfig.rightCustomAction.label,
                                    tint = subtitleColor,
                                )
                            }
                        }
                    }
                }

                WidgetFormat.SQUARE_2X2 -> {
                    Column(modifier = Modifier.fillMaxWidth()) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.Top,
                        ) {
                            WidgetArtworkThumb(media = media, size = 56.dp, radius = 14.dp)
                            Column(horizontalAlignment = Alignment.End) {
                                IconButton(onClick = onOpenSource, modifier = Modifier.size(34.dp)) {
                                    Icon(
                                        imageVector = Icons.AutoMirrored.Rounded.OpenInNew,
                                        contentDescription = "Abrir reproductor",
                                        tint = accentColor,
                                        modifier = Modifier.size(18.dp),
                                    )
                                }
                                if (effectiveConfig.showServiceBadge) {
                                    Text(
                                        text = media.service.displayName.uppercase(),
                                        style = MaterialTheme.typography.labelSmall,
                                        color = accentColor,
                                        maxLines = 1,
                                    )
                                }
                            }
                        }
                        Spacer(modifier = Modifier.height(10.dp))
                        Text(
                            text = copy.title,
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold,
                            color = titleColor,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                        Text(
                            text = copy.subtitle,
                            style = MaterialTheme.typography.bodySmall,
                            color = subtitleColor,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                        if (effectiveConfig.showProgressBar) {
                            Spacer(modifier = Modifier.height(8.dp))
                            LinearProgressIndicator(
                                progress = { if (media.hasActiveMedia) media.progressFraction else 0.35f },
                                color = accentColor,
                                trackColor = titleColor.copy(alpha = 0.16f),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(3.dp)
                                    .clip(CircleShape),
                            )
                        }
                        Spacer(modifier = Modifier.height(8.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceEvenly,
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            IconButton(onClick = onPrev, modifier = Modifier.size(38.dp)) {
                                Icon(Icons.Rounded.SkipPrevious, contentDescription = "Anterior", tint = titleColor)
                            }
                            Box(
                                modifier = Modifier
                                    .size(42.dp)
                                    .clip(CircleShape)
                                    .background(playBgColor)
                                    .clickable(onClick = onPlayPause),
                                contentAlignment = Alignment.Center,
                            ) {
                                Icon(
                                    imageVector = if (media.isPlaying) Icons.Rounded.Pause else Icons.Rounded.PlayArrow,
                                    contentDescription = "Reproducir o pausar",
                                    tint = playIconColor,
                                )
                            }
                            IconButton(onClick = onNext, modifier = Modifier.size(38.dp)) {
                                Icon(Icons.Rounded.SkipNext, contentDescription = "Siguiente", tint = titleColor)
                            }
                        }
                    }
                }

                WidgetFormat.MINI_2X1 -> {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        WidgetArtworkThumb(media = media, size = 40.dp, radius = 12.dp)
                        Spacer(modifier = Modifier.width(8.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = copy.title,
                                style = MaterialTheme.typography.labelLarge,
                                fontWeight = FontWeight.Bold,
                                color = titleColor,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                            )
                            Text(
                                text = copy.subtitle,
                                style = MaterialTheme.typography.labelSmall,
                                color = subtitleColor,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                            )
                        }
                        Spacer(modifier = Modifier.width(8.dp))
                        Box(
                            modifier = Modifier
                                .size(40.dp)
                                .clip(CircleShape)
                                .background(playBgColor)
                                .clickable(onClick = onPlayPause),
                            contentAlignment = Alignment.Center,
                        ) {
                            Icon(
                                imageVector = if (media.isPlaying) Icons.Rounded.Pause else Icons.Rounded.PlayArrow,
                                contentDescription = "Reproducir o pausar",
                                tint = playIconColor,
                            )
                        }
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
    MagicAlbumArtwork(
        bitmap = media.albumArt,
        artworkModel = media.albumArt ?: media.albumArtUri,
        contentDescription = "Carátula del álbum",
        cornerRadius = radius,
        fallbackTint = media.service.accentColor,
        modifier = Modifier
            .size(size)
            .border(1.dp, MagicTheme.tokens.semantic.divider, RoundedCornerShape(radius)),
    )
}
