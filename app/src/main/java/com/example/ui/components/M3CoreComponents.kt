package com.example.ui.components

import android.graphics.Bitmap
import android.graphics.drawable.Drawable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.OpenInNew
import androidx.compose.material.icons.automirrored.rounded.VolumeOff
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.Favorite
import androidx.compose.material.icons.rounded.Forward10
import androidx.compose.material.icons.rounded.Mic
import androidx.compose.material.icons.rounded.Pause
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.Repeat
import androidx.compose.material.icons.rounded.Replay10
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material.icons.rounded.Shuffle
import androidx.compose.material.icons.rounded.SkipNext
import androidx.compose.material.icons.rounded.SwapHoriz
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.core.graphics.drawable.toBitmap
import coil.compose.AsyncImage
import com.example.R
import com.example.core.artwork.LocalArtworkPipeline
import com.example.data.CustomControlAction
import com.example.data.MusicServiceApp
import com.example.data.UnifiedMediaState
import com.example.designsystem.MagicTheme
import com.example.designsystem.expressive.MagicLinearWavyProgress
import com.example.designsystem.expressive.MagicNavChromeItem
import com.example.designsystem.expressive.MagicShortNavBar
import com.example.designsystem.expressive.MagicWideNavRail
import com.example.designsystem.expressive.MiniPlayerStyle
import com.example.designsystem.expressive.resolveArtworkShape

@Immutable
data class NavItemUiModel(
    val id: String,
    val label: String,
    val selectedIcon: ImageVector,
    val unselectedIcon: ImageVector,
    val testTag: String,
) {
    fun toChromeItem(): MagicNavChromeItem = MagicNavChromeItem(
        id = id,
        label = label,
        selectedIcon = selectedIcon,
        unselectedIcon = unselectedIcon,
        testTag = testTag,
    )
}

@Immutable
data class CoverGridItem(
    val id: String,
    val title: String,
    val subtitle: String,
    val accentTint: Color,
    val bitmap: Bitmap? = null,
    val packageName: String = "",
    val artworkModel: Any? = null,
)

@Composable
fun MagicSurfaceCard(
    modifier: Modifier = Modifier,
    cornerRadius: Dp = MagicTheme.tokens.radii.card,
    highlightBorder: Boolean = false,
    containerColor: Color = MagicTheme.tokens.semantic.cardSurface,
    contentPadding: Dp = MagicTheme.tokens.spacing.md,
    content: @Composable ColumnScope.() -> Unit,
) {
    val tokens = MagicTheme.tokens
    val colors = MaterialTheme.colorScheme
    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(cornerRadius),
        color = containerColor,
        border = BorderStroke(
            width = if (highlightBorder) 1.5.dp else 1.dp,
            color = if (highlightBorder) colors.primary.copy(alpha = 0.6f) else tokens.semantic.divider,
        ),
        tonalElevation = tokens.elevations.card,
    ) {
        Column(
            modifier = Modifier.padding(contentPadding),
            content = content,
        )
    }
}

@Composable
fun MagicFilterChip(
    label: String,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    leadingIcon: ImageVector? = null,
) {
    val colors = MaterialTheme.colorScheme
    FilterChip(
        selected = selected,
        onClick = onClick,
        label = {
            Text(
                text = label,
                style = MaterialTheme.typography.labelLarge,
                fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Medium,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        },
        leadingIcon = if (leadingIcon != null) {
            {
                Icon(
                    imageVector = leadingIcon,
                    contentDescription = null,
                    modifier = Modifier.size(16.dp),
                )
            }
        } else {
            null
        },
        shape = CircleShape,
        colors = FilterChipDefaults.filterChipColors(
            containerColor = colors.surfaceContainerHigh,
            labelColor = colors.onSurface,
            iconColor = colors.onSurfaceVariant,
            selectedContainerColor = colors.primaryContainer,
            selectedLabelColor = colors.onPrimaryContainer,
            selectedLeadingIconColor = colors.onPrimaryContainer,
        ),
        modifier = modifier.heightIn(min = 40.dp),
    )
}

@Composable
fun MagicSearchBarPill(
    query: String,
    placeholder: String,
    onClick: () -> Unit,
    onMicClick: () -> Unit,
    modifier: Modifier = Modifier,
    onQueryChange: ((String) -> Unit)? = null,
) {
    val colors = MaterialTheme.colorScheme
    val tokens = MagicTheme.tokens
    Surface(
        onClick = if (onQueryChange == null) onClick else ({}),
        shape = CircleShape,
        color = colors.surfaceContainerHigh,
        border = BorderStroke(1.dp, tokens.semantic.divider),
        modifier = modifier
            .fillMaxWidth()
            .heightIn(min = 52.dp),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = 16.dp, end = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(
                imageVector = Icons.Rounded.Search,
                contentDescription = "Buscar",
                tint = colors.onSurfaceVariant,
                modifier = Modifier.size(22.dp),
            )
            Spacer(modifier = Modifier.width(12.dp))
            if (onQueryChange != null) {
                Box(
                    modifier = Modifier.weight(1f),
                    contentAlignment = Alignment.CenterStart,
                ) {
                    if (query.isEmpty()) {
                        Text(
                            text = placeholder,
                            style = MaterialTheme.typography.bodyLarge,
                            color = colors.onSurfaceVariant,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                    }
                    BasicTextField(
                        value = query,
                        onValueChange = onQueryChange,
                        singleLine = true,
                        textStyle = MaterialTheme.typography.bodyLarge.copy(color = colors.onSurface),
                        cursorBrush = SolidColor(colors.primary),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("search_input_field"),
                    )
                }
            } else {
                Text(
                    text = query.ifBlank { placeholder },
                    style = MaterialTheme.typography.bodyLarge,
                    color = if (query.isBlank()) colors.onSurfaceVariant else colors.onSurface,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f),
                )
            }
            if (query.isNotEmpty() && onQueryChange != null) {
                IconButton(onClick = { onQueryChange("") }) {
                    Icon(
                        imageVector = Icons.Rounded.Close,
                        contentDescription = "Limpiar búsqueda",
                        tint = colors.onSurfaceVariant,
                    )
                }
            } else {
                IconButton(onClick = onMicClick) {
                    Icon(
                        imageVector = Icons.Rounded.Mic,
                        contentDescription = "Búsqueda rápida",
                        tint = colors.onSurfaceVariant,
                    )
                }
            }
        }
    }
}

@Composable
fun MagicAppIconBadge(
    drawable: Drawable?,
    fallbackTint: Color,
    contentDescription: String?,
    modifier: Modifier = Modifier,
    size: Dp = 44.dp,
    cornerRadius: Dp = 14.dp,
    service: MusicServiceApp? = null,
    serviceName: String = service?.displayName ?: contentDescription.orEmpty(),
    isDarkTheme: Boolean = MagicTheme.tokens.isDark,
) {
    val imageBitmap = remember(drawable) {
        drawable?.let { runCatching { it.toBitmap(96, 96).asImageBitmap() }.getOrNull() }
    }
    val spec = remember(imageBitmap, service, serviceName, isDarkTheme, size) {
        ServiceIconFallbackSelector.select(
            hasPackageDrawable = imageBitmap != null,
            service = service,
            serviceName = serviceName,
            isDarkTheme = isDarkTheme,
            sizeDp = size.value,
        )
    }

    val shape = if (spec.kind == ServiceBadgeFallbackKind.INITIAL_CIRCLE) {
        CircleShape
    } else {
        RoundedCornerShape(cornerRadius)
    }

    Box(
        modifier = modifier
            .size(size)
            .clip(shape)
            .background(
                when (spec.kind) {
                    ServiceBadgeFallbackKind.APPLE_MUSIC_PNG -> Color.Transparent
                    else -> fallbackTint.copy(alpha = 0.18f)
                },
            ),
        contentAlignment = Alignment.Center,
    ) {
        when (spec.kind) {
            ServiceBadgeFallbackKind.PACKAGE_DRAWABLE -> {
                if (imageBitmap != null) {
                    Image(
                        bitmap = imageBitmap,
                        contentDescription = contentDescription ?: spec.contentDescription,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxSize(),
                    )
                }
            }
            ServiceBadgeFallbackKind.APPLE_MUSIC_PNG -> {
                Image(
                    painter = painterResource(id = spec.appleMusicDrawableRes),
                    contentDescription = "Apple Music",
                    contentScale = ContentScale.Fit,
                    modifier = Modifier.fillMaxSize(),
                )
            }
            ServiceBadgeFallbackKind.INITIAL_CIRCLE -> {
                Text(
                    text = spec.initialLetter,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = fallbackTint,
                )
            }
        }
    }
}

@Composable
fun MagicWidePrimaryButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    icon: ImageVector? = Icons.Rounded.Shuffle,
) {
    val colors = MaterialTheme.colorScheme
    Button(
        onClick = onClick,
        shape = CircleShape,
        colors = ButtonDefaults.buttonColors(
            containerColor = colors.primary,
            contentColor = colors.onPrimary,
        ),
        modifier = modifier
            .fillMaxWidth()
            .height(52.dp),
    ) {
        if (icon != null) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                modifier = Modifier.size(20.dp),
            )
            Spacer(modifier = Modifier.width(8.dp))
        }
        Text(
            text = text,
            style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.SemiBold,
        )
    }
}

@Composable
fun MagicAlbumArtwork(
    bitmap: Bitmap?,
    contentDescription: String?,
    modifier: Modifier = Modifier,
    cornerRadius: Dp = MagicTheme.tokens.radii.artworkMedium,
    fallbackTint: Color? = null,
    artworkModel: Any? = null,
    shapeOverride: Shape? = null,
) {
    val context = LocalContext.current
    val tokens = MagicTheme.tokens
    val imageLoader = remember(context) {
        LocalArtworkPipeline.getOrCreateImageLoader(context)
    }
    val resolvedShape = shapeOverride ?: resolveArtworkShape(
        style = tokens.artworkShapeStyle,
        useExpressiveM3 = tokens.useExpressiveM3,
        fallbackCornerRadius = cornerRadius,
    )
    val modelToLoad: Any = artworkModel ?: bitmap ?: R.drawable.img_magic_album_art
    Box(
        modifier = modifier
            .clip(resolvedShape)
            .background(fallbackTint?.copy(alpha = 0.22f) ?: tokens.semantic.elevatedCardSurface),
        contentAlignment = Alignment.Center,
    ) {
        AsyncImage(
            model = modelToLoad,
            imageLoader = imageLoader,
            placeholder = painterResource(id = R.drawable.img_magic_album_art),
            error = painterResource(id = R.drawable.img_magic_album_art),
            contentDescription = contentDescription,
            contentScale = ContentScale.Crop,
            modifier = Modifier.fillMaxSize(),
        )
    }
}

@Composable
fun MagicCoverCardItem(
    item: CoverGridItem,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    size: Dp = 132.dp,
) {
    val colors = MaterialTheme.colorScheme
    val tokens = MagicTheme.tokens
    Column(
        modifier = modifier
            .width(size)
            .clip(RoundedCornerShape(tokens.radii.card))
            .clickable(onClick = onClick)
            .padding(bottom = 4.dp),
    ) {
        MagicAlbumArtwork(
            bitmap = item.bitmap,
            artworkModel = item.artworkModel ?: item.bitmap,
            contentDescription = item.title,
            cornerRadius = tokens.radii.artworkMedium,
            fallbackTint = item.accentTint,
            modifier = Modifier.size(size),
        )
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = item.title,
            style = MaterialTheme.typography.titleSmall,
            color = colors.onBackground,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
        Text(
            text = item.subtitle,
            style = MaterialTheme.typography.bodySmall,
            color = colors.onSurfaceVariant,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
    }
}

@Composable
fun MagicHorizontalCoverGrid(
    items: List<CoverGridItem>,
    onItemClick: (CoverGridItem) -> Unit,
    modifier: Modifier = Modifier,
    horizontalPadding: Dp = MagicTheme.tokens.spacing.screenMargin,
) {
    LazyRow(
        modifier = modifier.fillMaxWidth(),
        contentPadding = PaddingValues(horizontal = horizontalPadding),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        items(items, key = { it.id }) { cover ->
            MagicCoverCardItem(
                item = cover,
                onClick = { onItemClick(cover) },
            )
        }
    }
}

@Composable
fun MagicMiniPlayer(
    media: UnifiedMediaState,
    onPlayPause: () -> Unit,
    onNext: () -> Unit,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    positionMs: Long = media.positionMs,
    durationMs: Long = media.durationMs,
) {
    val tokens = MagicTheme.tokens
    val colors = MaterialTheme.colorScheme
    val isDocked = tokens.miniPlayerStyle == MiniPlayerStyle.DOCKED
    val shape = if (isDocked) RoundedCornerShape(0.dp) else RoundedCornerShape(tokens.radii.miniPlayer)
    val progress = if (durationMs > 0L) {
        (positionMs.toFloat() / durationMs.toFloat()).coerceIn(0f, 1f)
    } else {
        media.progressFraction
    }

    Surface(
        onClick = onClick,
        shape = shape,
        color = colors.surfaceContainerHigh,
        border = if (isDocked) null else BorderStroke(1.dp, colors.outlineVariant),
        tonalElevation = tokens.elevations.miniPlayer,
        modifier = modifier
            .fillMaxWidth()
            .let { m -> if (isDocked) m else m }
            .testTag("magic_mini_player"),
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                MagicAlbumArtwork(
                    bitmap = media.albumArt,
                    artworkModel = media.albumArt ?: media.albumArtUri,
                    contentDescription = "Carátula actual",
                    cornerRadius = 12.dp,
                    fallbackTint = media.service.accentColor,
                    modifier = Modifier.size(44.dp),
                )
                Spacer(modifier = Modifier.width(12.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = if (media.hasActiveMedia) media.title else "Sin música activa",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.SemiBold,
                        color = colors.onSurface,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                    Text(
                        text = if (media.hasActiveMedia) {
                            "${media.artist} • ${media.service.displayName}"
                        } else {
                            "Explora tu biblioteca o conecta Spotify / YouTube"
                        },
                        style = MaterialTheme.typography.bodySmall,
                        color = colors.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
                IconButton(
                    onClick = onPlayPause,
                    modifier = Modifier.testTag("mini_player_play_pause_btn"),
                ) {
                    Icon(
                        imageVector = if (media.isPlaying) Icons.Rounded.Pause else Icons.Rounded.PlayArrow,
                        contentDescription = "Reproducir o pausar",
                        tint = colors.primary,
                    )
                }
                IconButton(
                    onClick = onNext,
                    modifier = Modifier.testTag("mini_player_next_btn"),
                ) {
                    Icon(
                        imageVector = Icons.Rounded.SkipNext,
                        contentDescription = "Siguiente pista",
                        tint = colors.onSurface,
                    )
                }
            }
            MagicLinearWavyProgress(
                progress = if (media.hasActiveMedia) progress else 0f,
                isPlaying = media.isPlaying && media.hasActiveMedia,
                useExpressiveM3 = tokens.useExpressiveM3,
                reduceAnimations = tokens.motion.reduceAnimations,
                color = colors.primary,
                trackColor = colors.surfaceContainerHighest,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = if (isDocked) 0.dp else 12.dp)
                    .padding(bottom = 2.dp),
            )
        }
    }
}

@Composable
fun MagicBottomNavBar(
    items: List<NavItemUiModel>,
    selectedId: String,
    onSelect: (NavItemUiModel) -> Unit,
    modifier: Modifier = Modifier,
) {
    val tokens = MagicTheme.tokens
    val chromeItems = remember(items) { items.map { it.toChromeItem() } }
    MagicShortNavBar(
        items = chromeItems,
        selectedId = selectedId,
        labelVisibility = tokens.navLabelVisibility,
        useExpressiveM3 = tokens.useExpressiveM3,
        onSelect = { chosen ->
            items.firstOrNull { it.id == chosen.id }?.let(onSelect)
        },
        modifier = modifier,
    )
}

@Composable
fun MagicNavRail(
    items: List<NavItemUiModel>,
    selectedId: String,
    onSelect: (NavItemUiModel) -> Unit,
    modifier: Modifier = Modifier,
    expanded: Boolean = false,
) {
    val tokens = MagicTheme.tokens
    val chromeItems = remember(items) { items.map { it.toChromeItem() } }
    MagicWideNavRail(
        items = chromeItems,
        selectedId = selectedId,
        useExpressiveM3 = tokens.useExpressiveM3,
        expanded = expanded,
        onSelect = { chosen ->
            items.firstOrNull { it.id == chosen.id }?.let(onSelect)
        },
        modifier = modifier,
    )
}

@Composable
fun AnimatedEqualizerBars(
    color: Color,
    modifier: Modifier = Modifier,
) {
    val reduceAnimations = MagicTheme.tokens.motion.reduceAnimations
    val fractions = if (reduceAnimations) {
        listOf(0.6f, 0.85f, 0.5f)
    } else {
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
        listOf(bar1, bar2, bar3)
    }

    Row(
        modifier = modifier.height(16.dp),
        horizontalArrangement = Arrangement.spacedBy(2.5.dp),
        verticalAlignment = Alignment.Bottom,
    ) {
        fractions.forEach { heightFraction ->
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

fun customActionIcon(action: CustomControlAction): ImageVector = when (action) {
    CustomControlAction.REWIND_15 -> Icons.Rounded.Replay10
    CustomControlAction.FORWARD_15 -> Icons.Rounded.Forward10
    CustomControlAction.SWITCH_SOURCE -> Icons.Rounded.SwapHoriz
    CustomControlAction.OPEN_PLAYER -> Icons.AutoMirrored.Rounded.OpenInNew
    CustomControlAction.MUTE_AUDIO -> Icons.AutoMirrored.Rounded.VolumeOff
    CustomControlAction.LIKE_TRACK -> Icons.Rounded.Favorite
    CustomControlAction.SHUFFLE -> Icons.Rounded.Shuffle
    CustomControlAction.REPEAT -> Icons.Rounded.Repeat
}

fun formatTimeMs(ms: Long): String {
    val totalSec = (ms / 1000L).coerceAtLeast(0L)
    val min = totalSec / 60L
    val sec = totalSec % 60L
    return "%d:%02d".format(min, sec)
}
