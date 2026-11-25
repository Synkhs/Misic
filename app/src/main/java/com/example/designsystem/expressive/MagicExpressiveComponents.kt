package com.example.designsystem.expressive

import androidx.compose.animation.core.animateDpAsState
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.KeyboardArrowDown
import androidx.compose.material.icons.rounded.Pause
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ButtonGroupDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.FilledIconButton
import androidx.compose.material3.FilledIconToggleButton
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.FloatingActionButtonMenu
import androidx.compose.material3.FloatingActionButtonMenuItem
import androidx.compose.material3.FloatingToolbarDefaults
import androidx.compose.material3.HorizontalFloatingToolbar
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.LargeFlexibleTopAppBar
import androidx.compose.material3.LargeTopAppBar
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.LinearWavyProgressIndicator
import androidx.compose.material3.LoadingIndicator
import androidx.compose.material3.MaterialShapes
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.MediumFlexibleTopAppBar
import androidx.compose.material3.MediumTopAppBar
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.NavigationRail
import androidx.compose.material3.NavigationRailItem
import androidx.compose.material3.NavigationRailItemDefaults
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.ShortNavigationBar
import androidx.compose.material3.ShortNavigationBarDefaults
import androidx.compose.material3.ShortNavigationBarItem
import androidx.compose.material3.ShortNavigationBarItemDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.SplitButtonDefaults
import androidx.compose.material3.SplitButtonLayout
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.ToggleButtonDefaults
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.TopAppBarScrollBehavior
import androidx.compose.material3.WavyProgressIndicatorDefaults
import androidx.compose.material3.WideNavigationRail
import androidx.compose.material3.WideNavigationRailDefaults
import androidx.compose.material3.WideNavigationRailItem
import androidx.compose.material3.WideNavigationRailItemDefaults
import androidx.compose.material3.rememberWideNavigationRailState
import androidx.compose.material3.toShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

enum class NavLabelVisibility(val label: String) {
    ALWAYS("Siempre"),
    ACTIVE_ONLY("Solo activo"),
    ICONS_ONLY("Solo iconos"),
}

enum class MiniPlayerStyle(val label: String) {
    CARD("Tarjeta flotante"),
    DOCKED("Barra acoplada"),
}

@Immutable
data class MagicNavChromeItem(
    val id: String,
    val label: String,
    val selectedIcon: ImageVector,
    val unselectedIcon: ImageVector,
    val testTag: String,
)

@Immutable
data class MagicFabMenuAction(
    val label: String,
    val icon: ImageVector,
    val testTag: String,
    val onClick: () -> Unit,
)

/**
 * Flexible large top app bar isolating Expressive LargeFlexibleTopAppBar with standard fallback.
 */
@OptIn(ExperimentalMaterial3Api::class, ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun MagicFlexibleLargeTopBar(
    title: String,
    subtitle: String?,
    useExpressiveM3: Boolean,
    scrollBehavior: TopAppBarScrollBehavior? = null,
    navigationIcon: @Composable () -> Unit = {},
    actions: @Composable RowScope.() -> Unit = {},
    modifier: Modifier = Modifier,
) {
    val colors = MaterialTheme.colorScheme
    val barColors = TopAppBarDefaults.topAppBarColors(
        containerColor = colors.background,
        scrolledContainerColor = colors.surfaceContainerLow,
        titleContentColor = colors.onBackground,
        subtitleContentColor = colors.onSurfaceVariant,
        actionIconContentColor = colors.onSurfaceVariant,
        navigationIconContentColor = colors.onBackground,
    )
    if (useExpressiveM3) {
        LargeFlexibleTopAppBar(
            title = {
                Text(
                    text = title,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            },
            subtitle = if (!subtitle.isNullOrBlank()) {
                {
                    Text(
                        text = subtitle,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
            } else {
                null
            },
            navigationIcon = navigationIcon,
            actions = actions,
            colors = barColors,
            scrollBehavior = scrollBehavior,
            windowInsets = WindowInsets(0, 0, 0, 0),
            modifier = modifier,
        )
    } else {
        LargeTopAppBar(
            title = {
                Column {
                    Text(text = title, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    if (!subtitle.isNullOrBlank()) {
                        Text(
                            text = subtitle,
                            style = MaterialTheme.typography.bodySmall,
                            color = colors.onSurfaceVariant,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                    }
                }
            },
            navigationIcon = navigationIcon,
            actions = actions,
            colors = barColors,
            scrollBehavior = scrollBehavior,
            windowInsets = WindowInsets(0, 0, 0, 0),
            modifier = modifier,
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun MagicFlexibleMediumTopBar(
    title: String,
    subtitle: String? = null,
    useExpressiveM3: Boolean,
    scrollBehavior: TopAppBarScrollBehavior? = null,
    navigationIcon: @Composable () -> Unit = {},
    actions: @Composable RowScope.() -> Unit = {},
    modifier: Modifier = Modifier,
) {
    val colors = MaterialTheme.colorScheme
    val barColors = TopAppBarDefaults.topAppBarColors(
        containerColor = colors.background,
        scrolledContainerColor = colors.surfaceContainerLow,
        titleContentColor = colors.onBackground,
        subtitleContentColor = colors.onSurfaceVariant,
    )
    if (useExpressiveM3) {
        MediumFlexibleTopAppBar(
            title = { Text(text = title, maxLines = 1, overflow = TextOverflow.Ellipsis) },
            subtitle = if (!subtitle.isNullOrBlank()) {
                { Text(text = subtitle, maxLines = 1, overflow = TextOverflow.Ellipsis) }
            } else {
                null
            },
            navigationIcon = navigationIcon,
            actions = actions,
            colors = barColors,
            scrollBehavior = scrollBehavior,
            windowInsets = WindowInsets(0, 0, 0, 0),
            modifier = modifier,
        )
    } else {
        MediumTopAppBar(
            title = { Text(text = title, maxLines = 1, overflow = TextOverflow.Ellipsis) },
            navigationIcon = navigationIcon,
            actions = actions,
            colors = barColors,
            scrollBehavior = scrollBehavior,
            windowInsets = WindowInsets(0, 0, 0, 0),
            modifier = modifier,
        )
    }
}

/**
 * Mobile navigation chrome: Expressive ShortNavigationBar when enabled, NavigationBar otherwise.
 */
@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun MagicShortNavBar(
    items: List<MagicNavChromeItem>,
    selectedId: String,
    labelVisibility: NavLabelVisibility,
    useExpressiveM3: Boolean,
    onSelect: (MagicNavChromeItem) -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = MaterialTheme.colorScheme
    if (useExpressiveM3) {
        val itemColors = ShortNavigationBarItemDefaults.colors(
            selectedIconColor = colors.onPrimaryContainer,
            selectedTextColor = colors.primary,
            selectedIndicatorColor = colors.primaryContainer,
            unselectedIconColor = colors.onSurfaceVariant,
            unselectedTextColor = colors.onSurfaceVariant,
        )
        ShortNavigationBar(
            containerColor = colors.surfaceContainerLow,
            contentColor = colors.onSurface,
            windowInsets = WindowInsets.navigationBars,
            arrangement = ShortNavigationBarDefaults.arrangement,
            modifier = modifier,
        ) {
            items.forEach { item ->
                val selected = item.id == selectedId
                val showLabel = when (labelVisibility) {
                    NavLabelVisibility.ALWAYS -> true
                    NavLabelVisibility.ACTIVE_ONLY -> selected
                    NavLabelVisibility.ICONS_ONLY -> false
                }
                ShortNavigationBarItem(
                    selected = selected,
                    onClick = { onSelect(item) },
                    icon = {
                        Icon(
                            imageVector = if (selected) item.selectedIcon else item.unselectedIcon,
                            contentDescription = item.label,
                        )
                    },
                    label = {
                        if (showLabel) {
                            Text(
                                text = item.label,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                            )
                        }
                    },
                    colors = itemColors,
                    modifier = Modifier.testTag(item.testTag),
                )
            }
        }
    } else {
        NavigationBar(
            containerColor = colors.surfaceContainerLow,
            windowInsets = WindowInsets.navigationBars,
            modifier = modifier,
        ) {
            items.forEach { item ->
                val selected = item.id == selectedId
                val showLabel = when (labelVisibility) {
                    NavLabelVisibility.ALWAYS -> true
                    NavLabelVisibility.ACTIVE_ONLY -> selected
                    NavLabelVisibility.ICONS_ONLY -> false
                }
                NavigationBarItem(
                    selected = selected,
                    onClick = { onSelect(item) },
                    icon = {
                        Icon(
                            imageVector = if (selected) item.selectedIcon else item.unselectedIcon,
                            contentDescription = item.label,
                        )
                    },
                    label = if (showLabel) {
                        { Text(text = item.label, maxLines = 1, overflow = TextOverflow.Ellipsis) }
                    } else {
                        null
                    },
                    alwaysShowLabel = labelVisibility == NavLabelVisibility.ALWAYS,
                    colors = NavigationBarItemDefaults.colors(
                        selectedIconColor = colors.onPrimaryContainer,
                        selectedTextColor = colors.primary,
                        indicatorColor = colors.primaryContainer,
                        unselectedIconColor = colors.onSurfaceVariant,
                        unselectedTextColor = colors.onSurfaceVariant,
                    ),
                    modifier = Modifier.testTag(item.testTag),
                )
            }
        }
    }
}

/**
 * Wide/foldable navigation rail: Expressive WideNavigationRail when enabled, standard NavigationRail otherwise.
 */
@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun MagicWideNavRail(
    items: List<MagicNavChromeItem>,
    selectedId: String,
    useExpressiveM3: Boolean,
    expanded: Boolean = false,
    onSelect: (MagicNavChromeItem) -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = MaterialTheme.colorScheme
    if (useExpressiveM3) {
        val railState = rememberWideNavigationRailState()
        val itemColors = WideNavigationRailItemDefaults.colors(
            selectedIconColor = colors.onPrimaryContainer,
            selectedTextColor = colors.primary,
            selectedIndicatorColor = colors.primaryContainer,
            unselectedIconColor = colors.onSurfaceVariant,
            unselectedTextColor = colors.onSurfaceVariant,
        )
        WideNavigationRail(
            state = railState,
            colors = WideNavigationRailDefaults.colors(
                containerColor = colors.surfaceContainerLow,
                contentColor = colors.onSurface,
            ),
            modifier = modifier
                .fillMaxHeight()
                .statusBarsPadding(),
        ) {
            items.forEach { item ->
                val selected = item.id == selectedId
                WideNavigationRailItem(
                    selected = selected,
                    onClick = { onSelect(item) },
                    icon = {
                        Icon(
                            imageVector = if (selected) item.selectedIcon else item.unselectedIcon,
                            contentDescription = item.label,
                        )
                    },
                    label = { Text(item.label, maxLines = 1) },
                    railExpanded = expanded,
                    colors = itemColors,
                    modifier = Modifier.testTag(item.testTag),
                )
            }
        }
    } else {
        NavigationRail(
            containerColor = colors.surfaceContainerLow,
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
                        selectedIconColor = colors.onPrimaryContainer,
                        selectedTextColor = colors.primary,
                        indicatorColor = colors.primaryContainer,
                        unselectedIconColor = colors.onSurfaceVariant,
                        unselectedTextColor = colors.onSurfaceVariant,
                    ),
                    modifier = Modifier.testTag(item.testTag),
                )
            }
        }
    }
}

/**
 * Expressive SplitButton for primary actions (e.g., "Aleatorio" + source picker menu).
 */
@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun MagicSplitActionButton(
    label: String,
    leadingIcon: ImageVector,
    useExpressiveM3: Boolean,
    menuExpanded: Boolean,
    onPrimaryClick: () -> Unit,
    onToggleMenu: () -> Unit,
    modifier: Modifier = Modifier,
    primaryTestTag: String? = null,
    trailingTestTag: String? = null,
) {
    val colors = MaterialTheme.colorScheme
    if (useExpressiveM3) {
        SplitButtonLayout(
            modifier = modifier,
            leadingButton = {
                SplitButtonDefaults.LeadingButton(
                    onClick = onPrimaryClick,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = colors.primaryContainer,
                        contentColor = colors.onPrimaryContainer,
                    ),
                    modifier = if (primaryTestTag != null) Modifier.testTag(primaryTestTag) else Modifier,
                ) {
                    Icon(
                        imageVector = leadingIcon,
                        contentDescription = null,
                        modifier = Modifier.size(SplitButtonDefaults.LeadingIconSize),
                    )
                    Spacer(Modifier.size(ButtonDefaults.IconSpacing))
                    Text(
                        text = label,
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.SemiBold,
                        maxLines = 1,
                    )
                }
            },
            trailingButton = {
                SplitButtonDefaults.TrailingButton(
                    checked = menuExpanded,
                    onCheckedChange = { onToggleMenu() },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = colors.primaryContainer,
                        contentColor = colors.onPrimaryContainer,
                    ),
                    modifier = if (trailingTestTag != null) Modifier.testTag(trailingTestTag) else Modifier,
                ) {
                    Icon(
                        imageVector = Icons.Rounded.KeyboardArrowDown,
                        contentDescription = "Opciones de $label",
                        modifier = Modifier.size(SplitButtonDefaults.TrailingIconSize),
                    )
                }
            },
        )
    } else {
        Row(
            modifier = modifier,
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(2.dp),
        ) {
            Button(
                onClick = onPrimaryClick,
                colors = ButtonDefaults.buttonColors(
                    containerColor = colors.primaryContainer,
                    contentColor = colors.onPrimaryContainer,
                ),
                shape = RoundedCornerShape(topStart = 20.dp, bottomStart = 20.dp, topEnd = 6.dp, bottomEnd = 6.dp),
                modifier = if (primaryTestTag != null) Modifier.testTag(primaryTestTag) else Modifier,
            ) {
                Icon(leadingIcon, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(Modifier.width(6.dp))
                Text(label, maxLines = 1)
            }
            FilledIconButton(
                onClick = onToggleMenu,
                shape = RoundedCornerShape(topStart = 6.dp, bottomStart = 6.dp, topEnd = 20.dp, bottomEnd = 20.dp),
                modifier = if (trailingTestTag != null) Modifier.testTag(trailingTestTag) else Modifier,
            ) {
                Icon(Icons.Rounded.KeyboardArrowDown, contentDescription = "Opciones de $label")
            }
        }
    }
}

/**
 * Connected button group using Expressive FilledTonalToggleButton connected shapes,
 * falling back to SingleChoiceSegmentedButtonRow when Expressive is disabled.
 */
@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun <T> MagicConnectedButtonGroup(
    options: List<T>,
    selectedIndex: Int,
    labelProvider: (T) -> String,
    useExpressiveM3: Boolean,
    onSelect: (Int, T) -> Unit,
    modifier: Modifier = Modifier,
    itemTestTagPrefix: String? = null,
) {
    val colors = MaterialTheme.colorScheme
    if (options.isEmpty()) return
    if (useExpressiveM3) {
        Row(
            modifier = modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(ButtonGroupDefaults.ConnectedSpaceBetween),
        ) {
            options.forEachIndexed { idx, item ->
                val checked = idx == selectedIndex
                val shape = when {
                    options.size == 1 -> RoundedCornerShape(20.dp)
                    idx == 0 -> RoundedCornerShape(topStart = 20.dp, bottomStart = 20.dp, topEnd = 6.dp, bottomEnd = 6.dp)
                    idx == options.lastIndex -> RoundedCornerShape(topStart = 6.dp, bottomStart = 6.dp, topEnd = 20.dp, bottomEnd = 20.dp)
                    else -> RoundedCornerShape(6.dp)
                }
                FilledTonalButton(
                    onClick = { onSelect(idx, item) },
                    shape = shape,
                    colors = ButtonDefaults.filledTonalButtonColors(
                        containerColor = if (checked) colors.secondaryContainer else colors.surfaceContainerHighest,
                        contentColor = if (checked) colors.onSecondaryContainer else colors.onSurfaceVariant,
                    ),
                    modifier = Modifier
                        .weight(1f)
                        .let { m -> if (itemTestTagPrefix != null) m.testTag("${itemTestTagPrefix}_$idx") else m },
                ) {
                    Text(
                        text = labelProvider(item),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
            }
        }
    } else {
        SingleChoiceSegmentedButtonRow(modifier = modifier.fillMaxWidth()) {
            options.forEachIndexed { idx, item ->
                SegmentedButton(
                    selected = idx == selectedIndex,
                    onClick = { onSelect(idx, item) },
                    shape = SegmentedButtonDefaults.itemShape(index = idx, count = options.size),
                    modifier = if (itemTestTagPrefix != null) Modifier.testTag("${itemTestTagPrefix}_$idx") else Modifier,
                ) {
                    Text(
                        text = labelProvider(item),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
            }
        }
    }
}

/**
 * Hero Play/Pause button that morphs between Circle and Expressive Cookie/RoundedSquare when toggled.
 */
@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun MagicPlayPauseMorphButton(
    isPlaying: Boolean,
    useExpressiveM3: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    size: Dp = 76.dp,
    containerColor: Color = MaterialTheme.colorScheme.primary,
    contentColor: Color = MaterialTheme.colorScheme.onPrimary,
) {
    if (useExpressiveM3) {
        val shapes = IconButtonDefaults.toggleableShapes(
            shape = CircleShape,
            pressedShape = MaterialShapes.Square.toShape(),
            checkedShape = MaterialShapes.Cookie9Sided.toShape(),
        )
        FilledIconToggleButton(
            checked = isPlaying,
            onCheckedChange = { onClick() },
            shapes = shapes,
            colors = IconButtonDefaults.filledIconToggleButtonColors(
                containerColor = containerColor,
                contentColor = contentColor,
                checkedContainerColor = containerColor,
                checkedContentColor = contentColor,
            ),
            modifier = modifier.size(size),
        ) {
            Icon(
                imageVector = if (isPlaying) Icons.Rounded.Pause else Icons.Rounded.PlayArrow,
                contentDescription = if (isPlaying) "Pausar" else "Reproducir",
                modifier = Modifier.size(size * 0.48f),
            )
        }
    } else {
        val corner by animateDpAsState(
            targetValue = if (isPlaying) 22.dp else size / 2,
            label = "play_corner",
        )
        Surface(
            onClick = onClick,
            shape = RoundedCornerShape(corner),
            color = containerColor,
            contentColor = contentColor,
            modifier = modifier.size(size),
        ) {
            Box(contentAlignment = Alignment.Center) {
                Icon(
                    imageVector = if (isPlaying) Icons.Rounded.Pause else Icons.Rounded.PlayArrow,
                    contentDescription = if (isPlaying) "Pausar" else "Reproducir",
                    modifier = Modifier.size(size * 0.48f),
                )
            }
        }
    }
}

/**
 * Wavy progress bar for MiniPlayer / inline track indicators (flat when paused or reduced motion).
 */
@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun MagicLinearWavyProgress(
    progress: Float,
    isPlaying: Boolean,
    useExpressiveM3: Boolean,
    reduceAnimations: Boolean,
    modifier: Modifier = Modifier,
    color: Color = MaterialTheme.colorScheme.primary,
    trackColor: Color = MaterialTheme.colorScheme.surfaceContainerHighest,
) {
    val clamped = progress.coerceIn(0f, 1f)
    if (useExpressiveM3 && isPlaying && !reduceAnimations) {
        val strokePx = with(LocalDensity.current) { 3.dp.toPx() }
        LinearWavyProgressIndicator(
            progress = { clamped },
            color = color,
            trackColor = trackColor,
            stroke = Stroke(width = strokePx, cap = StrokeCap.Round),
            trackStroke = Stroke(width = strokePx, cap = StrokeCap.Round),
            amplitude = { if (it > 0.02f) 1f else 0f },
            wavelength = WavyProgressIndicatorDefaults.LinearDeterminateWavelength,
            modifier = modifier.height(10.dp),
        )
    } else {
        LinearProgressIndicator(
            progress = { clamped },
            color = color,
            trackColor = trackColor,
            modifier = modifier.height(3.dp),
        )
    }
}

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun MagicExpressiveLoading(
    useExpressiveM3: Boolean,
    modifier: Modifier = Modifier,
    color: Color = MaterialTheme.colorScheme.primary,
) {
    if (useExpressiveM3) {
        LoadingIndicator(
            color = color,
            modifier = modifier.size(40.dp),
        )
    } else {
        CircularProgressIndicator(
            color = color,
            strokeWidth = 3.dp,
            modifier = modifier.size(32.dp),
        )
    }
}

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun MagicFloatingToolbarBar(
    useExpressiveM3: Boolean,
    modifier: Modifier = Modifier,
    containerColor: Color = MaterialTheme.colorScheme.surfaceContainerHigh,
    content: @Composable RowScope.() -> Unit,
) {
    val colors = MaterialTheme.colorScheme
    if (useExpressiveM3) {
        HorizontalFloatingToolbar(
            expanded = true,
            colors = FloatingToolbarDefaults.standardFloatingToolbarColors(
                toolbarContainerColor = containerColor,
                toolbarContentColor = colors.onSurface,
            ),
            contentPadding = PaddingValues(horizontal = 10.dp, vertical = 6.dp),
            modifier = modifier,
            content = content,
        )
    } else {
        Surface(
            shape = CircleShape,
            color = containerColor,
            border = BorderStroke(1.dp, colors.outlineVariant),
            modifier = modifier,
        ) {
            Row(
                modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(4.dp),
                content = content,
            )
        }
    }
}

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun MagicFabMenu(
    expanded: Boolean,
    useExpressiveM3: Boolean,
    fabIcon: ImageVector,
    fabContentDescription: String,
    actions: List<MagicFabMenuAction>,
    onToggle: () -> Unit,
    modifier: Modifier = Modifier,
    fabShape: Shape = CircleShape,
) {
    val colors = MaterialTheme.colorScheme
    if (useExpressiveM3) {
        FloatingActionButtonMenu(
            expanded = expanded,
            button = {
                FloatingActionButton(
                    onClick = onToggle,
                    shape = fabShape,
                    containerColor = colors.primaryContainer,
                    contentColor = colors.onPrimaryContainer,
                    modifier = Modifier.testTag("fab_menu_toggle"),
                ) {
                    Icon(imageVector = fabIcon, contentDescription = fabContentDescription)
                }
            },
            modifier = modifier,
        ) {
            actions.forEach { action ->
                FloatingActionButtonMenuItem(
                    onClick = action.onClick,
                    text = { Text(action.label) },
                    icon = { Icon(action.icon, contentDescription = null) },
                    containerColor = colors.secondaryContainer,
                    contentColor = colors.onSecondaryContainer,
                    modifier = Modifier.testTag(action.testTag),
                )
            }
        }
    } else {
        Column(
            horizontalAlignment = Alignment.End,
            verticalArrangement = Arrangement.spacedBy(8.dp),
            modifier = modifier,
        ) {
            if (expanded) {
                actions.forEach { action ->
                    Surface(
                        onClick = action.onClick,
                        shape = CircleShape,
                        color = colors.secondaryContainer,
                        contentColor = colors.onSecondaryContainer,
                        modifier = Modifier.testTag(action.testTag),
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                        ) {
                            Icon(action.icon, contentDescription = null, modifier = Modifier.size(18.dp))
                            Text(action.label, style = MaterialTheme.typography.labelLarge)
                        }
                    }
                }
            }
            FloatingActionButton(
                onClick = onToggle,
                shape = fabShape,
                containerColor = colors.primaryContainer,
                contentColor = colors.onPrimaryContainer,
                modifier = Modifier.testTag("fab_menu_toggle"),
            ) {
                Icon(imageVector = fabIcon, contentDescription = fabContentDescription)
            }
        }
    }
}
