package com.example.ui.screens

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.res.Configuration
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.AutoAwesome
import androidx.compose.material.icons.rounded.BookmarkAdd
import androidx.compose.material.icons.rounded.Contrast
import androidx.compose.material.icons.rounded.DeleteOutline
import androidx.compose.material.icons.rounded.Download
import androidx.compose.material.icons.rounded.Palette
import androidx.compose.material.icons.rounded.RestartAlt
import androidx.compose.material.icons.rounded.RoundedCorner
import androidx.compose.material.icons.rounded.Upload
import androidx.compose.material.icons.rounded.Widgets
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Slider
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.example.R
import com.example.core.compat.AndroidVersionCompat
import com.example.data.WidgetInstanceConfig
import com.example.designsystem.ColorRoleOverrides
import com.example.designsystem.ContrastPreset
import com.example.designsystem.DefaultSeedSwatches
import com.example.designsystem.MagicMusicTheme
import com.example.designsystem.MagicTheme
import com.example.designsystem.MaterialColorUtilitiesEngine
import com.example.designsystem.McuSchemeVariant
import com.example.designsystem.SavedColorProfile
import com.example.designsystem.SeedColorSwatch
import com.example.designsystem.SurfaceToneStyle
import com.example.designsystem.ThemeMode
import com.example.designsystem.ThemePreferences
import com.example.designsystem.expressive.ArtworkShapeStyle
import com.example.designsystem.expressive.GeometryPreset
import com.example.designsystem.expressive.MagicConnectedButtonGroup
import com.example.designsystem.expressive.MagicFlexibleLargeTopBar
import com.example.designsystem.expressive.MiniPlayerStyle
import com.example.designsystem.expressive.NavLabelVisibility
import com.example.ui.components.MagicColorSwatchGrid
import com.example.ui.components.MagicModeThumbnailsSelector
import com.example.ui.components.MagicSettingsRowCard
import com.example.ui.components.MagicSettingsSwitchRow
import com.example.ui.components.MagicSurfaceCard
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun AppearanceScreen(
    themePreferences: ThemePreferences,
    savedProfiles: List<SavedColorProfile> = emptyList(),
    widgetInstances: Map<Int, WidgetInstanceConfig> = emptyMap(),
    onSelectThemeMode: (ThemeMode) -> Unit,
    onSelectSurfaceTone: (SurfaceToneStyle) -> Unit,
    onSelectSeedSwatch: (SeedColorSwatch) -> Unit,
    onSelectCustomSeedArgb: (Long) -> Unit = {},
    onSelectSchemeVariant: (McuSchemeVariant) -> Unit = {},
    onSelectContrastPreset: (ContrastPreset) -> Unit = {},
    onUpdateRoleOverrides: (ColorRoleOverrides) -> Unit = {},
    onResetRoleOverrides: () -> Unit = {},
    onToggleDynamicColor: (Boolean) -> Unit,
    onTogglePureAmoled: (Boolean) -> Unit,
    onToggleColorFromArtwork: (Boolean) -> Unit,
    onToggleReduceAnimations: (Boolean) -> Unit,
    onToggleExpressiveM3: (Boolean) -> Unit = {},
    onToggleHaptics: (Boolean) -> Unit = {},
    onSelectGeometryPreset: (GeometryPreset) -> Unit = {},
    onSelectArtworkShape: (ArtworkShapeStyle) -> Unit = {},
    onSelectNavLabelVisibility: (NavLabelVisibility) -> Unit = {},
    onSelectMiniPlayerStyle: (MiniPlayerStyle) -> Unit = {},
    onSelectLibraryGridColumns: (Int) -> Unit = {},
    onToggleLyricsAutoScroll: (Boolean) -> Unit = {},
    onLyricsFontScaleChange: (Float) -> Unit = {},
    onSaveColorProfile: (String) -> Unit = {},
    onApplyColorProfile: (SavedColorProfile) -> Unit = {},
    onDeleteColorProfile: (String) -> Unit = {},
    onExportProfilesJson: ((String) -> Unit) -> Unit = {},
    onImportProfilesJson: (String) -> Unit = {},
    onResetWidgetToApp: (Int) -> Unit = {},
    onOpenWidgetStudio: (() -> Unit)? = null,
    onShowFeedback: (String) -> Unit = {},
    onBack: (() -> Unit)? = null,
) {
    val context = LocalContext.current
    val tokens = MagicTheme.tokens
    val colors = MaterialTheme.colorScheme
    val supportsDynamic = AndroidVersionCompat.supportsDynamicColor()
    val scrollBehavior = TopAppBarDefaults.exitUntilCollapsedScrollBehavior()

    val (initHue, initSat) = remember(themePreferences.seedColorArgb) {
        MaterialColorUtilitiesEngine.extractHueAndSaturation(themePreferences.seedColorArgb)
    }
    var hueSlider by remember(themePreferences.seedColorArgb) { mutableFloatStateOf(initHue) }
    var satSlider by remember(themePreferences.seedColorArgb) { mutableFloatStateOf(initSat) }
    var hexDraft by remember(themePreferences.seedColorArgb) {
        mutableStateOf(String.format(Locale.US, "#%06X", themePreferences.seedColorArgb.toInt() and 0xFFFFFF))
    }

    var showSaveDialog by remember { mutableStateOf(false) }
    var profileNameDraft by remember { mutableStateOf("") }
    var showImportDialog by remember { mutableStateOf(false) }
    var importDraft by remember { mutableStateOf("") }

    val primaryContrastRatio = remember(colors.primary, colors.onPrimary) {
        MaterialColorUtilitiesEngine.contrastRatioArgb(colors.onPrimary.toArgb(), colors.primary.toArgb())
    }
    val surfaceContrastRatio = remember(colors.surface, colors.onSurface) {
        MaterialColorUtilitiesEngine.contrastRatioArgb(colors.onSurface.toArgb(), colors.surface.toArgb())
    }

    Scaffold(
        modifier = Modifier
            .fillMaxSize()
            .nestedScroll(scrollBehavior.nestedScrollConnection),
        containerColor = Color.Transparent,
        topBar = {
            MagicFlexibleLargeTopBar(
                title = "Estilo",
                subtitle = if (themePreferences.useExpressiveM3) {
                    "Material 3 Expressive • ${themePreferences.schemeVariant.label}"
                } else {
                    "Material Design 3 • ${themePreferences.schemeVariant.label}"
                },
                useExpressiveM3 = themePreferences.useExpressiveM3,
                scrollBehavior = scrollBehavior,
                navigationIcon = {
                    if (onBack != null) {
                        IconButton(
                            onClick = onBack,
                            modifier = Modifier.testTag("appearance_back_btn"),
                        ) {
                            Icon(Icons.AutoMirrored.Rounded.ArrowBack, contentDescription = stringResource(R.string.action_back))
                        }
                    }
                },
            )
        },
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .testTag("appearance_list"),
            contentPadding = PaddingValues(
                start = tokens.spacing.screenMargin,
                end = tokens.spacing.screenMargin,
                top = tokens.spacing.xs,
                bottom = 96.dp,
            ),
            verticalArrangement = Arrangement.spacedBy(tokens.spacing.md),
        ) {
            // 1. Material 3 Expressive & Motion / Haptics
            item {
                MagicSurfaceCard {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Icon(Icons.Rounded.AutoAwesome, contentDescription = null, tint = colors.primary)
                        Text("Material 3 Expressive", style = MaterialTheme.typography.titleMedium, color = colors.onSurface)
                    }
                    Spacer(Modifier.height(6.dp))
                    MagicSettingsSwitchRow(
                        title = "Material 3 Expressive",
                        subtitle = "Barras flexibles, botón play morfable, barra ondulada y formas expresivas",
                        checked = themePreferences.useExpressiveM3,
                        onCheckedChange = onToggleExpressiveM3,
                        switchTestTag = "expressive_m3_switch",
                    )
                    MagicSettingsSwitchRow(
                        title = "Respuesta háptica",
                        subtitle = "Vibración sutil al pulsar controles y pestañas",
                        checked = themePreferences.hapticFeedbackEnabled,
                        onCheckedChange = onToggleHaptics,
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

            // 2. Geometry, Artwork Shape, Navigation, MiniPlayer & Lyrics UI controls
            item {
                MagicSurfaceCard {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Icon(Icons.Rounded.RoundedCorner, contentDescription = null, tint = colors.primary)
                        Text("Geometría, Carátula y Diseño", style = MaterialTheme.typography.titleMedium, color = colors.onSurface)
                    }
                    Spacer(Modifier.height(10.dp))

                    Text("Geometría de superficies", style = MaterialTheme.typography.labelLarge, color = colors.onSurfaceVariant)
                    Spacer(Modifier.height(6.dp))
                    val geomEntries = GeometryPreset.entries
                    MagicConnectedButtonGroup(
                        options = geomEntries,
                        selectedIndex = geomEntries.indexOf(themePreferences.geometryPreset).coerceAtLeast(0),
                        labelProvider = { it.label },
                        useExpressiveM3 = themePreferences.useExpressiveM3,
                        onSelect = { _, preset -> onSelectGeometryPreset(preset) },
                        itemTestTagPrefix = "geometry_preset",
                    )

                    Spacer(Modifier.height(12.dp))
                    Text("Forma de la carátula", style = MaterialTheme.typography.labelLarge, color = colors.onSurfaceVariant)
                    Spacer(Modifier.height(6.dp))
                    FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp),
                    ) {
                        ArtworkShapeStyle.entries.forEach { shapeStyle ->
                            FilterChip(
                                selected = themePreferences.artworkShape == shapeStyle,
                                onClick = { onSelectArtworkShape(shapeStyle) },
                                label = { Text(shapeStyle.label) },
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = colors.primaryContainer,
                                    selectedLabelColor = colors.onPrimaryContainer,
                                ),
                            )
                        }
                    }

                    Spacer(Modifier.height(12.dp))
                    Text("Etiquetas en barra de navegación", style = MaterialTheme.typography.labelLarge, color = colors.onSurfaceVariant)
                    Spacer(Modifier.height(6.dp))
                    val navModes = NavLabelVisibility.entries
                    MagicConnectedButtonGroup(
                        options = navModes,
                        selectedIndex = navModes.indexOf(themePreferences.navLabelVisibility).coerceAtLeast(0),
                        labelProvider = { it.label },
                        useExpressiveM3 = themePreferences.useExpressiveM3,
                        onSelect = { _, mode -> onSelectNavLabelVisibility(mode) },
                    )

                    Spacer(Modifier.height(12.dp))
                    Text("Mini-reproductor", style = MaterialTheme.typography.labelLarge, color = colors.onSurfaceVariant)
                    Spacer(Modifier.height(6.dp))
                    val miniStyles = MiniPlayerStyle.entries
                    MagicConnectedButtonGroup(
                        options = miniStyles,
                        selectedIndex = miniStyles.indexOf(themePreferences.miniPlayerStyle).coerceAtLeast(0),
                        labelProvider = { it.label },
                        useExpressiveM3 = themePreferences.useExpressiveM3,
                        onSelect = { _, st -> onSelectMiniPlayerStyle(st) },
                    )

                    Spacer(Modifier.height(12.dp))
                    Text("Columnas en cuadrícula de álbumes", style = MaterialTheme.typography.labelLarge, color = colors.onSurfaceVariant)
                    Spacer(Modifier.height(6.dp))
                    MagicConnectedButtonGroup(
                        options = listOf(2, 3),
                        selectedIndex = if (themePreferences.libraryGridColumns == 3) 1 else 0,
                        labelProvider = { "$it columnas" },
                        useExpressiveM3 = themePreferences.useExpressiveM3,
                        onSelect = { _, cols -> onSelectLibraryGridColumns(cols) },
                    )

                    Spacer(Modifier.height(12.dp))
                    MagicSettingsSwitchRow(
                        title = "Desplazamiento automático en Letras",
                        subtitle = "Sigue la estrofa sincronizada activa",
                        checked = themePreferences.lyricsAutoScroll,
                        onCheckedChange = onToggleLyricsAutoScroll,
                    )
                    Text(
                        text = "Tamaño de texto en Letras (${(themePreferences.lyricsFontScale * 100).toInt()}%)",
                        style = MaterialTheme.typography.labelLarge,
                        color = colors.onSurfaceVariant,
                    )
                    Slider(
                        value = themePreferences.lyricsFontScale,
                        onValueChange = onLyricsFontScaleChange,
                        valueRange = 0.85f..1.4f,
                    )
                }
            }

            // 3. Surface Mode & Contrast Level
            item {
                MagicSurfaceCard {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Icon(Icons.Rounded.Contrast, contentDescription = null, tint = colors.primary)
                        Text("Superficie y Contraste WCAG", style = MaterialTheme.typography.titleMedium, color = colors.onSurface)
                    }
                    Spacer(Modifier.height(10.dp))
                    MagicModeThumbnailsSelector(
                        selectedTone = themePreferences.surfaceToneStyle,
                        onSelectTone = onSelectSurfaceTone,
                    )
                    Spacer(Modifier.height(12.dp))
                    MagicConnectedButtonGroup(
                        options = ThemeMode.entries,
                        selectedIndex = ThemeMode.entries.indexOf(themePreferences.themeMode).coerceAtLeast(0),
                        labelProvider = { it.label },
                        useExpressiveM3 = themePreferences.useExpressiveM3,
                        onSelect = { _, mode -> onSelectThemeMode(mode) },
                        itemTestTagPrefix = "theme_mode",
                    )
                    Spacer(Modifier.height(12.dp))
                    Text("Nivel de contraste (mínimo AA 4.5:1)", style = MaterialTheme.typography.labelLarge, color = colors.onSurfaceVariant)
                    Spacer(Modifier.height(6.dp))
                    MagicConnectedButtonGroup(
                        options = ContrastPreset.entries,
                        selectedIndex = ContrastPreset.entries.indexOf(themePreferences.contrastLevel).coerceAtLeast(0),
                        labelProvider = { it.label },
                        useExpressiveM3 = themePreferences.useExpressiveM3,
                        onSelect = { _, preset -> onSelectContrastPreset(preset) },
                        itemTestTagPrefix = "contrast_preset",
                    )
                    Spacer(Modifier.height(8.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        ContrastBadge("Botón ${"%.1f".format(primaryContrastRatio)}:1")
                        ContrastBadge("Texto ${"%.1f".format(surfaceContrastRatio)}:1")
                    }
                }
            }

            // 4. Material Color Utilities Studio
            item {
                MagicSurfaceCard {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Icon(Icons.Rounded.Palette, contentDescription = null, tint = colors.primary)
                        Text("Material Color Utilities Studio", style = MaterialTheme.typography.titleMedium, color = colors.onSurface)
                    }
                    Spacer(Modifier.height(10.dp))

                    MagicColorSwatchGrid(
                        swatches = DefaultSeedSwatches,
                        selectedArgb = themePreferences.seedColorArgb,
                        dynamicEnabled = themePreferences.useDynamicColor && supportsDynamic,
                        onSelectSwatch = onSelectSeedSwatch,
                    )

                    Spacer(Modifier.height(12.dp))
                    Text("Variante de esquema dinámico (9 algoritmos)", style = MaterialTheme.typography.labelLarge, color = colors.onSurfaceVariant)
                    Spacer(Modifier.height(6.dp))
                    FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp),
                    ) {
                        McuSchemeVariant.entries.forEach { variant ->
                            FilterChip(
                                selected = themePreferences.schemeVariant == variant,
                                onClick = { onSelectSchemeVariant(variant) },
                                label = { Text(variant.label) },
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = colors.primaryContainer,
                                    selectedLabelColor = colors.onPrimaryContainer,
                                ),
                            )
                        }
                    }

                    Spacer(Modifier.height(12.dp))
                    Text("Matiz HCT libre (${hueSlider.toInt()}°)", style = MaterialTheme.typography.labelLarge, color = colors.onSurfaceVariant)
                    Slider(
                        value = hueSlider,
                        onValueChange = { h ->
                            hueSlider = h
                            onSelectCustomSeedArgb(MaterialColorUtilitiesEngine.seedFromHueSaturation(h, satSlider))
                        },
                        valueRange = 0f..360f,
                    )
                    Text("Croma / Saturación (${(satSlider * 100).toInt()}%)", style = MaterialTheme.typography.labelLarge, color = colors.onSurfaceVariant)
                    Slider(
                        value = satSlider,
                        onValueChange = { s ->
                            satSlider = s
                            onSelectCustomSeedArgb(MaterialColorUtilitiesEngine.seedFromHueSaturation(hueSlider, s))
                        },
                        valueRange = 0.05f..1f,
                    )
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        OutlinedTextField(
                            value = hexDraft,
                            onValueChange = { hexDraft = it },
                            label = { Text("Hex semilla (#RRGGBB)") },
                            singleLine = true,
                            modifier = Modifier.weight(1f),
                        )
                        FilledTonalButton(
                            onClick = {
                                val parsed = parseHexColorOrNull(hexDraft)
                                if (parsed != null) {
                                    onSelectCustomSeedArgb(parsed)
                                    onShowFeedback("Color semilla aplicado")
                                } else {
                                    onShowFeedback("Formato inválido (usa #RRGGBB)")
                                }
                            },
                        ) {
                            Text("Aplicar")
                        }
                    }

                    Spacer(Modifier.height(10.dp))
                    MagicSettingsSwitchRow(
                        title = stringResource(R.string.appearance_dynamic_color),
                        subtitle = if (supportsDynamic) {
                            stringResource(R.string.appearance_dynamic_color_desc)
                        } else {
                            "No disponible en Android 8–11: usando paleta tonal HCT"
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

                    Spacer(Modifier.height(12.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Text("Anular roles (Primary / Secondary / Tertiary)", style = MaterialTheme.typography.labelLarge, color = colors.onSurfaceVariant)
                        TextButton(onClick = onResetRoleOverrides) {
                            Icon(Icons.Rounded.RestartAlt, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(Modifier.width(4.dp))
                            Text("Restablecer")
                        }
                    }
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        val curOverrides = themePreferences.roleOverrides
                        RoleDotButton(
                            label = "Primary",
                            color = colors.primary,
                            onCycle = {
                                val next = DefaultSeedSwatches.random().argb
                                onUpdateRoleOverrides(curOverrides.copy(primaryArgb = next))
                            },
                            modifier = Modifier.weight(1f),
                        )
                        RoleDotButton(
                            label = "Secondary",
                            color = colors.secondary,
                            onCycle = {
                                val next = DefaultSeedSwatches.random().argb
                                onUpdateRoleOverrides(curOverrides.copy(secondaryArgb = next))
                            },
                            modifier = Modifier.weight(1f),
                        )
                        RoleDotButton(
                            label = "Tertiary",
                            color = colors.tertiary,
                            onCycle = {
                                val next = DefaultSeedSwatches.random().argb
                                onUpdateRoleOverrides(curOverrides.copy(tertiaryArgb = next))
                            },
                            modifier = Modifier.weight(1f),
                        )
                    }
                }
            }

            // 5. Saved Color Profiles (Export / Import JSON)
            item {
                MagicSurfaceCard {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Text("Colección de Paletas", style = MaterialTheme.typography.titleMedium, color = colors.onSurface)
                        FilledTonalButton(onClick = {
                            profileNameDraft = ""
                            showSaveDialog = true
                        }) {
                            Icon(Icons.Rounded.BookmarkAdd, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(Modifier.width(6.dp))
                            Text("Guardar")
                        }
                    }
                    if (savedProfiles.isNotEmpty()) {
                        Spacer(Modifier.height(10.dp))
                        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            savedProfiles.forEach { profile ->
                                Surface(
                                    onClick = { onApplyColorProfile(profile) },
                                    shape = RoundedCornerShape(tokens.radii.listItem),
                                    color = colors.surfaceContainerLow,
                                    border = BorderStroke(1.dp, tokens.semantic.divider),
                                    modifier = Modifier.fillMaxWidth(),
                                ) {
                                    Row(
                                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
                                        verticalAlignment = Alignment.CenterVertically,
                                    ) {
                                        Box(
                                            modifier = Modifier
                                                .size(28.dp)
                                                .clip(CircleShape)
                                                .background(Color(profile.seedColorArgb)),
                                        )
                                        Spacer(Modifier.width(10.dp))
                                        Column(modifier = Modifier.weight(1f)) {
                                            Text(profile.name, style = MaterialTheme.typography.titleSmall, color = colors.onSurface)
                                            Text(
                                                "${profile.variant.label} • ${profile.contrastPreset.label}",
                                                style = MaterialTheme.typography.bodySmall,
                                                color = colors.onSurfaceVariant,
                                            )
                                        }
                                        IconButton(onClick = { onDeleteColorProfile(profile.id) }) {
                                            Icon(Icons.Rounded.DeleteOutline, contentDescription = "Borrar", tint = colors.error)
                                        }
                                    }
                                }
                            }
                        }
                    }
                    Spacer(Modifier.height(10.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                    ) {
                        OutlinedButton(
                            onClick = {
                                onExportProfilesJson { json ->
                                    val cm = context.getSystemService(Context.CLIPBOARD_SERVICE) as? ClipboardManager
                                    cm?.setPrimaryClip(ClipData.newPlainText("MagicMusic Palettes", json))
                                    onShowFeedback("Paletas JSON copiadas al portapapeles")
                                }
                            },
                            modifier = Modifier.weight(1f),
                        ) {
                            Icon(Icons.Rounded.Upload, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(Modifier.width(6.dp))
                            Text("Exportar JSON")
                        }
                        OutlinedButton(
                            onClick = { showImportDialog = true },
                            modifier = Modifier.weight(1f),
                        ) {
                            Icon(Icons.Rounded.Download, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(Modifier.width(6.dp))
                            Text("Importar JSON")
                        }
                    }
                }
            }

            // 6. Widgets Studio shortcut & per-instance color configuration
            if (onOpenWidgetStudio != null) {
                item {
                    MagicSettingsRowCard(
                        icon = Icons.Rounded.Widgets,
                        title = "Estudio de Widgets",
                        subtitle = "Diseña tarjetas 4×2, cápsulas 4×1, cuadrados 2×2 y pines directos",
                        onClick = onOpenWidgetStudio,
                        modifier = Modifier.testTag("style_open_widget_studio"),
                    )
                }
            }

            if (widgetInstances.isNotEmpty()) {
                item {
                    MagicSurfaceCard {
                        Text(
                            text = "Widgets fijados (${widgetInstances.size})",
                            style = MaterialTheme.typography.titleMedium,
                            color = colors.onSurface,
                        )
                        Spacer(Modifier.height(8.dp))
                        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            widgetInstances.values.forEach { inst ->
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically,
                                ) {
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = "Widget #${inst.appWidgetId} • ${inst.format.gridLabel}",
                                            style = MaterialTheme.typography.titleSmall,
                                            color = colors.onSurface,
                                        )
                                        Text(
                                            text = "${inst.colorSource.label} • ${inst.schemeVariant.label}",
                                            style = MaterialTheme.typography.bodySmall,
                                            color = colors.onSurfaceVariant,
                                        )
                                    }
                                    TextButton(onClick = { onResetWidgetToApp(inst.appWidgetId) }) {
                                        Text("Sincronizar")
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    if (showSaveDialog) {
        AlertDialog(
            onDismissRequest = { showSaveDialog = false },
            title = { Text("Guardar paleta") },
            text = {
                OutlinedTextField(
                    value = profileNameDraft,
                    onValueChange = { profileNameDraft = it },
                    label = { Text("Nombre de la paleta") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                )
            },
            confirmButton = {
                Button(
                    enabled = profileNameDraft.isNotBlank(),
                    onClick = {
                        onSaveColorProfile(profileNameDraft.trim())
                        showSaveDialog = false
                    },
                ) {
                    Text("Guardar")
                }
            },
            dismissButton = {
                TextButton(onClick = { showSaveDialog = false }) { Text("Cancelar") }
            },
        )
    }

    if (showImportDialog) {
        AlertDialog(
            onDismissRequest = { showImportDialog = false },
            title = { Text("Importar paletas JSON") },
            text = {
                OutlinedTextField(
                    value = importDraft,
                    onValueChange = { importDraft = it },
                    label = { Text("Pega aquí el JSON de paletas") },
                    minLines = 3,
                    maxLines = 6,
                    modifier = Modifier.fillMaxWidth(),
                )
            },
            confirmButton = {
                Button(
                    enabled = importDraft.isNotBlank(),
                    onClick = {
                        onImportProfilesJson(importDraft)
                        importDraft = ""
                        showImportDialog = false
                    },
                ) {
                    Text("Importar")
                }
            },
            dismissButton = {
                TextButton(onClick = { showImportDialog = false }) { Text("Cancelar") }
            },
        )
    }
}

@Composable
private fun ContrastBadge(text: String) {
    val colors = MaterialTheme.colorScheme
    Surface(
        shape = RoundedCornerShape(8.dp),
        color = colors.tertiaryContainer,
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.labelSmall,
            fontWeight = FontWeight.SemiBold,
            color = colors.onTertiaryContainer,
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
        )
    }
}

@Composable
private fun RoleDotButton(
    label: String,
    color: Color,
    onCycle: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Surface(
        onClick = onCycle,
        shape = RoundedCornerShape(14.dp),
        color = MaterialTheme.colorScheme.surfaceContainerLow,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
        modifier = modifier,
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            Box(
                modifier = Modifier
                    .size(16.dp)
                    .clip(CircleShape)
                    .background(color)
                    .border(1.dp, MaterialTheme.colorScheme.outline, CircleShape),
            )
            Text(label, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurface)
        }
    }
}

private fun parseHexColorOrNull(raw: String): Long? {
    val cleaned = raw.trim().removePrefix("#")
    if (cleaned.length != 6 && cleaned.length != 8) return null
    val parsed = cleaned.toLongOrNull(16) ?: return null
    return if (cleaned.length == 6) (parsed or 0xFF000000L) else parsed
}

@Preview(name = "Estilo - Tonal", showBackground = true, uiMode = Configuration.UI_MODE_NIGHT_YES)
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
