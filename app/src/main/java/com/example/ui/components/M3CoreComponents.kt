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
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.OpenInNew
import androidx.compose.material.icons.automirrored.rounded.VolumeOff
import androidx.compose.material.icons.rounded.Album
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
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.NavigationRail
import androidx.compose.material3.NavigationRailItem
import androidx.compose.material3.NavigationRailItemDefaults
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
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.core.graphics.drawable.toBitmap
import coil.compose.AsyncImage
import com.example.R
import com.example.data.CustomControlAction
import com.example.data.MusicServiceApp
import com.example.data.UnifiedMediaState
import com.example.designsystem.MagicTheme

@Immutable
data class NavItemUiModel(
    val id: String,
    val label: String,
    val selectedIcon: ImageVector,
    val unselectedIcon: ImageVector,
    val testTag: String,
)

@Immutable
data class CoverGridItem(
    val id: String,
    val title: String,
    val subtitle: String,
    val accentTint: Color,
    val bitmap: Bitmap? = null,
    val packageName: String = "",
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
            width = if (highlightBorder) 1.2.dp else 1.dp,
            color = if (highlightBorder) colors.primary.copy(alpha = 0.55f) else tokens.semantic.divider,
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
    val tokens = MagicTheme.tokens
    Surface(
        onClick = onClick,
        shape = CircleShape,
        color = if (selected) colors.primary else tokens.semantic.cardSurface,
        border = if (selected) null else BorderStroke(1.dp, tokens.semantic.divider),
        modifier = modifier.heightIn(min = 38.dp),
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 9.dp),
        ) {
            if (leadingIcon != null) {
                Icon(
                    imageVector = leadingIcon,
                    contentDescription = null,
                    tint = if (selected) colors.onPrimary else colors.onSurfaceVariant,
                    modifier = Modifier.size(16.dp),
                )
            }
            Text(
                text = label,
                style = MaterialTheme.typography.labelLarge,
                fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Medium,
                color = if (selected) colors.onPrimary else colors.onSurface,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
    }
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
        color = tokens.semantic.cardSurface,
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
) {
    val shape = RoundedCornerShape(cornerRadius)
    val tokens = MagicTheme.tokens
    Box(
        modifier = modifier
            .clip(shape)
            .background(fallbackTint?.copy(alpha = 0.28f) ?: tokens.semantic.elevatedCardSurface),
        contentAlignment = Alignment.Center,
    ) {
        AsyncImage(
            model = bitmap ?: R.drawable.img_magic_album_art,
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
) {
    val tokens = MagicTheme.tokens
    val colors = MaterialTheme.colorScheme
    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(tokens.radii.miniPlayer),
        color = tokens.semantic.miniPlayerSurface,
        border = BorderStroke(1.dp, tokens.semantic.divider),
        tonalElevation = tokens.elevations.miniPlayer,
        modifier = modifier
            .fillMaxWidth()
            .testTag("magic_mini_player"),
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 10.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                MagicAlbumArtwork(
                    bitmap = media.albumArt,
                    contentDescription = "Carátula actual",
                    cornerRadius = 10.dp,
                    fallbackTint = media.service.accentColor,
                    modifier = Modifier.size(44.dp),
                )
                Spacer(modifier = Modifier.width(12.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = if (media.hasActiveMedia) media.title else "Sin música activa",
                        style = MaterialTheme.typography.titleSmall,
                        color = colors.onSurface,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                    Text(
                        text = if (media.hasActiveMedia) {
                            "${media.artist} • ${media.service.displayName}"
                        } else {
                            "Conecta Spotify, YT Music o Motor Local"
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
                        tint = colors.onSurface,
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
            LinearProgressIndicator(
                progress = { if (media.hasActiveMedia) media.progressFraction else 0f },
                color = colors.primary,
                trackColor = tokens.semantic.divider.copy(alpha = 0.45f),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(3.dp),
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
    val colors = MaterialTheme.colorScheme
    NavigationBar(
        containerColor = tokens.semantic.navBarContainer,
        windowInsets = WindowInsets.navigationBars,
        modifier = modifier,
    ) {
        items.forEach { item ->
            val selected = item.id == selectedId
            NavigationBarItem(
                selected = selected,
                onClick = { onSelect(item) },
                icon = {
                    Icon(
                        imageVector = if (selected) item.selectedIcon else item.unselectedIcon,
                        contentDescription = item.label,
                    )
                },
                label = {
                    Text(
                        text = item.label,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                },
                colors = NavigationBarItemDefaults.colors(
                    selectedIconColor = colors.onSecondaryContainer,
                    selectedTextColor = colors.onSurface,
                    indicatorColor = tokens.semantic.navPillIndicator,
                    unselectedIconColor = colors.onSurfaceVariant,
                    unselectedTextColor = colors.onSurfaceVariant,
                ),
                modifier = Modifier.testTag(item.testTag),
            )
        }
    }
}

@Composable
fun MagicNavRail(
    items: List<NavItemUiModel>,
    selectedId: String,
    onSelect: (NavItemUiModel) -> Unit,
    modifier: Modifier = Modifier,
) {
    val tokens = MagicTheme.tokens
    val colors = MaterialTheme.colorScheme
    NavigationRail(
        containerColor = tokens.semantic.navBarContainer,
        modifier = modifier
            .fillMaxHeight()
            .statusBarsPadding(),
    ) {
        items.forEach { item ->
            val selected = item.id == selectedId
            NavigationRailItem(
                selected = selected,
                onClick = { onSelect(item) },
                icon = {
                    Icon(
                        imageVector = if (selected) item.selectedIcon else item.unselectedIcon,
                        contentDescription = item.label,
                    )
                },
                label = { Text(item.label, maxLines = 1) },
                colors = NavigationRailItemDefaults.colors(
                    selectedIconColor = colors.onSecondaryContainer,
                    selectedTextColor = colors.onSurface,
                    indicatorColor = tokens.semantic.navPillIndicator,
                    unselectedIconColor = colors.onSurfaceVariant,
                    unselectedTextColor = colors.onSurfaceVariant,
                ),
                modifier = Modifier.testTag(item.testTag),
            )
        }
    }
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
