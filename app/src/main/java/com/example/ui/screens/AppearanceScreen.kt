package com.example.ui.screens

import android.content.res.Configuration
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.Contrast
import androidx.compose.material.icons.rounded.Palette
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.example.R
import com.example.core.compat.AndroidVersionCompat
import com.example.designsystem.DefaultSeedSwatches
import com.example.designsystem.MagicMusicTheme
import com.example.designsystem.MagicTheme
import com.example.designsystem.SeedColorSwatch
import com.example.designsystem.SurfaceToneStyle
import com.example.designsystem.ThemeMode
import com.example.designsystem.ThemePreferences
import com.example.ui.components.MagicColorSwatchGrid
import com.example.ui.components.MagicModeThumbnailsSelector
import com.example.ui.components.MagicSettingsSwitchRow
import com.example.ui.components.MagicSurfaceCard

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun AppearanceScreen(
    themePreferences: ThemePreferences,
    onSelectThemeMode: (ThemeMode) -> Unit,
    onSelectSurfaceTone: (SurfaceToneStyle) -> Unit,
    onSelectSeedSwatch: (SeedColorSwatch) -> Unit,
    onToggleDynamicColor: (Boolean) -> Unit,
    onTogglePureAmoled: (Boolean) -> Unit,
    onToggleColorFromArtwork: (Boolean) -> Unit,
    onToggleReduceAnimations: (Boolean) -> Unit,
    onBack: (() -> Unit)? = null,
) {
    val tokens = MagicTheme.tokens
    val colors = MaterialTheme.colorScheme
    val supportsDynamic = AndroidVersionCompat.supportsDynamicColor()

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .testTag("appearance_list"),
        contentPadding = PaddingValues(
            start = tokens.spacing.screenMargin,
            end = tokens.spacing.screenMargin,
            top = tokens.spacing.sm,
            bottom = tokens.spacing.lg,
        ),
        verticalArrangement = Arrangement.spacedBy(tokens.spacing.md),
    ) {
        item {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth(),
            ) {
                if (onBack != null) {
                    IconButton(
                        onClick = onBack,
                        modifier = Modifier.testTag("appearance_back_btn"),
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Rounded.ArrowBack,
                            contentDescription = stringResource(R.string.action_back),
                            tint = colors.onBackground,
                        )
                    }
                }
                Column(verticalArrangement = Arrangement.spacedBy(tokens.spacing.xxs)) {
                    Text(
                        text = stringResource(R.string.appearance_title),
                        style = MaterialTheme.typography.headlineMedium,
                        color = colors.onBackground,
                    )
                    Text(
                        text = stringResource(R.string.appearance_subtitle),
                        style = MaterialTheme.typography.bodyMedium,
                        color = colors.onSurfaceVariant,
                    )
                }
            }
        }

        // 3 Visual Mode Thumbnails (Oscuro / Tonal / Claro) matching reference screen 12
        item {
            MagicSurfaceCard {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(tokens.spacing.xs),
                ) {
                    Icon(
                        imageVector = Icons.Rounded.Contrast,
                        contentDescription = null,
                        tint = colors.primary,
                    )
                    Text(
                        text = "Superficie e intensidad",
                        style = MaterialTheme.typography.titleMedium,
                        color = colors.onSurface,
                    )
                }
                Spacer(modifier = Modifier.height(tokens.spacing.sm))

                MagicModeThumbnailsSelector(
                    selectedTone = themePreferences.surfaceToneStyle,
                    onSelectTone = onSelectSurfaceTone,
                )

                Spacer(modifier = Modifier.height(tokens.spacing.md))

                SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
                    ThemeMode.entries.forEachIndexed { index, mode ->
                        SegmentedButton(
                            selected = themePreferences.themeMode == mode,
                            onClick = { onSelectThemeMode(mode) },
                            shape = SegmentedButtonDefaults.itemShape(
                                index = index,
                                count = ThemeMode.entries.size,
                            ),
                            modifier = Modifier.testTag("theme_mode_${mode.name.lowercase()}"),
                        ) {
                            Text(mode.label, maxLines = 1)
                        }
                    }
                }
            }
        }

        // Circular Color Swatches & Material You switches
        item {
            MagicSurfaceCard {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(tokens.spacing.xs),
                ) {
                    Icon(
                        imageVector = Icons.Rounded.Palette,
                        contentDescription = null,
                        tint = colors.primary,
                    )
                    Text(
                        text = "Material You y Color Semilla",
                        style = MaterialTheme.typography.titleMedium,
                        color = colors.onSurface,
                    )
                }
                Spacer(modifier = Modifier.height(tokens.spacing.sm))

                MagicColorSwatchGrid(
                    swatches = DefaultSeedSwatches,
                    selectedArgb = themePreferences.seedColorArgb,
                    dynamicEnabled = themePreferences.useDynamicColor && supportsDynamic,
                    onSelectSwatch = onSelectSeedSwatch,
                )

                Spacer(modifier = Modifier.height(tokens.spacing.md))

                MagicSettingsSwitchRow(
                    title = stringResource(R.string.appearance_dynamic_color),
                    subtitle = if (supportsDynamic) {
                        stringResource(R.string.appearance_dynamic_color_desc)
                    } else {
                        "No disponible en Android 8–11: usando paleta tonal calibrada"
                    },
                    checked = themePreferences.useDynamicColor && supportsDynamic,
                    onCheckedChange = onToggleDynamicColor,
                    switchTestTag = "dynamic_color_switch",
                )

                MagicSettingsSwitchRow(
                    title = stringResource(R.string.appearance_amoled),
                    subtitle = stringResource(R.string.appearance_amoled_desc),
                    checked = themePreferences.pureAmoledBlack,
                    onCheckedChange = onTogglePureAmoled,
                    switchTestTag = "amoled_black_switch",
                )

                MagicSettingsSwitchRow(
                    title = stringResource(R.string.appearance_artwork_tint),
                    subtitle = stringResource(R.string.appearance_artwork_tint_desc),
                    checked = themePreferences.colorFromArtwork,
                    onCheckedChange = onToggleColorFromArtwork,
                    switchTestTag = "artwork_tint_switch",
                )

                MagicSettingsSwitchRow(
                    title = stringResource(R.string.appearance_reduce_animations),
                    subtitle = stringResource(R.string.appearance_reduce_animations_desc),
                    checked = themePreferences.reduceAnimations,
                    onCheckedChange = onToggleReduceAnimations,
                    switchTestTag = "reduce_animations_switch",
                )
            }
        }

        // Active token summary
        item {
            MagicSurfaceCard {
                Text(
                    text = "Resumen del tema activo",
                    style = MaterialTheme.typography.titleSmall,
                    color = colors.onSurface,
                )
                Spacer(modifier = Modifier.height(tokens.spacing.xs))
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(tokens.spacing.xs),
                    verticalArrangement = Arrangement.spacedBy(tokens.spacing.xs),
                ) {
                    AssistChip(
                        onClick = {},
                        label = { Text(if (tokens.isDark) "Tema Oscuro" else "Tema Claro") },
                    )
                    AssistChip(
                        onClick = {},
                        label = { Text("Superficie ${themePreferences.surfaceToneStyle.label}") },
                    )
                    AssistChip(
                        onClick = {},
                        label = { Text(if (tokens.usesDynamicColor) "Material You" else "Paleta Semilla") },
                    )
                    if (tokens.pureAmoled) {
                        AssistChip(
                            onClick = {},
                            label = { Text("AMOLED #000000") },
                        )
                    }
                }
            }
        }
    }
}

@Preview(name = "Apariencia - Tonal", showBackground = true, uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
private fun AppearanceScreenTonalPreview() {
    MagicMusicTheme(themePreferences = ThemePreferences()) {
        AppearanceScreen(
            themePreferences = ThemePreferences(),
            onSelectThemeMode = {},
            onSelectSurfaceTone = {},
            onSelectSeedSwatch = {},
            onToggleDynamicColor = {},
            onTogglePureAmoled = {},
            onToggleColorFromArtwork = {},
            onToggleReduceAnimations = {},
        )
    }
}
