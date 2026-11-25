package com.example.widget

import android.appwidget.AppWidgetManager
import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.Palette
import androidx.compose.material3.Button
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Slider
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.core.media.MediaRepository
import com.example.data.CustomControlAction
import com.example.data.PreferencesRepository
import com.example.data.WidgetColorSource
import com.example.data.WidgetInstanceConfig
import com.example.data.WidgetStyle
import com.example.designsystem.ContrastPreset
import com.example.designsystem.MaterialColorUtilitiesEngine
import com.example.designsystem.McuSchemeVariant
import com.example.designsystem.ThemeMode
import com.example.designsystem.ThemePreferences
import com.example.designsystem.MagicMusicTheme
import kotlinx.coroutines.launch
import kotlin.math.roundToInt

/**
 * Reconfigurable per-instance AppWidget customization activity (independent colors, contrast,
 * style, corner radius, and custom action slots for every pinned widgetId).
 */
class WidgetConfigureActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)

        val appWidgetId = intent?.extras?.getInt(
            AppWidgetManager.EXTRA_APPWIDGET_ID,
            AppWidgetManager.INVALID_APPWIDGET_ID,
        ) ?: AppWidgetManager.INVALID_APPWIDGET_ID

        if (appWidgetId == AppWidgetManager.INVALID_APPWIDGET_ID) {
            setResult(RESULT_CANCELED)
            finish()
            return
        }

        // Safe default canceled unless user saves
        setResult(
            RESULT_CANCELED,
            Intent().putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, appWidgetId),
        )

        MediaRepository.init(applicationContext)
        val prefsRepo = PreferencesRepository(applicationContext)

        setContent {
            val themePrefs by prefsRepo.themePreferencesFlow.collectAsStateWithLifecycle(initialValue = ThemePreferences())
            var config by remember { mutableStateOf<WidgetInstanceConfig?>(null) }
            val scope = rememberCoroutineScope()

            LaunchedEffect(appWidgetId) {
                config = prefsRepo.getWidgetInstanceConfig(appWidgetId)
            }

            MagicMusicTheme(themePreferences = themePrefs) {
                val activeCfg = config
                if (activeCfg != null) {
                    WidgetConfigureScreen(
                        appWidgetId = appWidgetId,
                        config = activeCfg,
                        appTheme = themePrefs,
                        onUpdate = { config = it },
                        onCancel = { finish() },
                        onSave = {
                            scope.launch {
                                prefsRepo.updateWidgetInstanceConfig(appWidgetId) { activeCfg }
                                MagicWidgetUpdater.updateAllWidgets(applicationContext, force = true)
                                val resultIntent = Intent().putExtra(
                                    AppWidgetManager.EXTRA_APPWIDGET_ID,
                                    appWidgetId,
                                )
                                setResult(RESULT_OK, resultIntent)
                                finish()
                            }
                        },
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
private fun WidgetConfigureScreen(
    appWidgetId: Int,
    config: WidgetInstanceConfig,
    appTheme: ThemePreferences,
    onUpdate: (WidgetInstanceConfig) -> Unit,
    onCancel: () -> Unit,
    onSave: () -> Unit,
) {
    val colors = MaterialTheme.colorScheme
    val (hue, sat) = remember(config.seedColorArgb) {
        MaterialColorUtilitiesEngine.extractHueAndSaturation(config.seedColorArgb)
    }
    val previewPalette = remember(config, appTheme) {
        WidgetRenderModel.resolvePaletteForInstance(
            instance = config,
            appTheme = appTheme,
            dominantArtworkArgb = null,
            serviceAccentArgb = null,
        )
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Configurar Widget #$appWidgetId") },
                navigationIcon = {
                    IconButton(onClick = onCancel, modifier = Modifier.testTag("widget_cfg_cancel_btn")) {
                        Icon(Icons.AutoMirrored.Rounded.ArrowBack, contentDescription = "Cancelar")
                    }
                },
                actions = {
                    Button(
                        onClick = onSave,
                        modifier = Modifier
                            .padding(end = 12.dp)
                            .testTag("widget_cfg_save_btn"),
                    ) {
                        Icon(Icons.Rounded.Check, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.size(6.dp))
                        Text("Aplicar")
                    }
                },
            )
        },
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(colors.background)
                .padding(innerPadding),
            contentAlignment = Alignment.TopCenter,
        ) {
            Column(
                modifier = Modifier
                    .widthIn(max = 600.dp)
                    .verticalScroll(rememberScrollState())
                    .navigationBarsPadding()
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp),
            ) {
                // Live M3 Preview Card
                ElevatedCard(modifier = Modifier.fillMaxWidth()) {
                    Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        Text("Vista previa independiente", style = MaterialTheme.typography.labelLarge, color = colors.onSurfaceVariant)
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(config.cornerRadiusDp.dp))
                                .background(Color(previewPalette.bgStartArgb))
                                .padding(16.dp),
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                                Box(
                                    modifier = Modifier
                                        .size(48.dp)
                                        .clip(RoundedCornerShape(12.dp))
                                        .background(Color(previewPalette.accentArgb)),
                                    contentAlignment = Alignment.Center,
                                ) {
                                    Icon(Icons.Rounded.Palette, contentDescription = null, tint = Color(previewPalette.playBtnIconArgb))
                                }
                                Column(modifier = Modifier.weight(1f)) {
                                    if (config.showServiceBadge) {
                                        Text(
                                            text = if (config.showCompactHeader) "MAGICMUSIC • SPOTIFY" else "SPOTIFY",
                                            style = MaterialTheme.typography.labelSmall,
                                            color = Color(previewPalette.badgeTextArgb),
                                        )
                                    }
                                    Text("Nocturne en Mi Menor", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold, color = Color(previewPalette.titleArgb))
                                    Text("Estudio Acústico", style = MaterialTheme.typography.bodySmall, color = Color(previewPalette.subtitleArgb))
                                }
                            }
                        }
                    }
                }

                Text("Origen de color del widget", style = MaterialTheme.typography.titleMedium)
                FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    WidgetColorSource.entries.forEach { src ->
                        FilterChip(
                            selected = config.colorSource == src,
                            onClick = { onUpdate(config.copy(colorSource = src)) },
                            label = { Text(src.label) },
                        )
                    }
                }

                if (config.colorSource != WidgetColorSource.FOLLOW_APP) {
                    if (config.colorSource == WidgetColorSource.CUSTOM_SEED) {
                        Text("Matiz semilla (${hue.roundToInt()}°) y saturación (${(sat * 100).roundToInt()}%)", style = MaterialTheme.typography.titleSmall)
                        Slider(
                            value = hue,
                            onValueChange = { nextHue ->
                                onUpdate(config.copy(seedColorArgb = MaterialColorUtilitiesEngine.seedFromHueSaturation(nextHue, sat)))
                            },
                            valueRange = 0f..360f,
                        )
                        Slider(
                            value = sat,
                            onValueChange = { nextSat ->
                                onUpdate(config.copy(seedColorArgb = MaterialColorUtilitiesEngine.seedFromHueSaturation(hue, nextSat)))
                            },
                            valueRange = 0.05f..1f,
                        )
                    }

                    Text("Modo y contraste del widget", style = MaterialTheme.typography.titleSmall)
                    FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        ThemeMode.entries.forEach { mode ->
                            FilterChip(
                                selected = config.themeMode == mode,
                                onClick = { onUpdate(config.copy(themeMode = mode)) },
                                label = { Text(mode.label) },
                            )
                        }
                    }
                    FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        ContrastPreset.entries.forEach { c ->
                            FilterChip(
                                selected = config.contrastLevel == c,
                                onClick = { onUpdate(config.copy(contrastLevel = c)) },
                                label = { Text("Contraste ${c.label}") },
                            )
                        }
                    }
                    FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        listOf(
                            McuSchemeVariant.TONAL_SPOT,
                            McuSchemeVariant.VIBRANT,
                            McuSchemeVariant.EXPRESSIVE,
                            McuSchemeVariant.MONOCHROME,
                        ).forEach { variant ->
                            FilterChip(
                                selected = config.schemeVariant == variant,
                                onClick = { onUpdate(config.copy(schemeVariant = variant)) },
                                label = { Text(variant.label) },
                            )
                        }
                    }
                }

                Text("Estilo Material 3 y radios", style = MaterialTheme.typography.titleMedium)
                FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    WidgetStyle.entries.forEach { style ->
                        FilterChip(
                            selected = config.style == style,
                            onClick = { onUpdate(config.copy(style = style)) },
                            label = { Text(style.title) },
                        )
                    }
                }
                Text("Radio de esquina: ${config.cornerRadiusDp} dp", style = MaterialTheme.typography.bodyMedium)
                Slider(
                    value = config.cornerRadiusDp.toFloat(),
                    onValueChange = { onUpdate(config.copy(cornerRadiusDp = it.roundToInt().coerceIn(12, 32))) },
                    valueRange = 12f..32f,
                )

                Text("Opciones de visualización", style = MaterialTheme.typography.titleMedium)
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                    Text("Mostrar barra de progreso")
                    Switch(checked = config.showProgressBar, onCheckedChange = { onUpdate(config.copy(showProgressBar = it)) })
                }
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                    Text("Mostrar etiqueta de servicio")
                    Switch(checked = config.showServiceBadge, onCheckedChange = { onUpdate(config.copy(showServiceBadge = it)) })
                }

                Text("Botón izquierdo personalizado", style = MaterialTheme.typography.titleSmall)
                FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    CustomControlAction.entries.forEach { action ->
                        FilterChip(
                            selected = config.leftCustomAction == action,
                            onClick = { onUpdate(config.copy(leftCustomAction = action)) },
                            label = { Text(action.shortLabel) },
                        )
                    }
                }
                Text("Botón derecho personalizado", style = MaterialTheme.typography.titleSmall)
                FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    CustomControlAction.entries.forEach { action ->
                        FilterChip(
                            selected = config.rightCustomAction == action,
                            onClick = { onUpdate(config.copy(rightCustomAction = action)) },
                            label = { Text(action.shortLabel) },
                        )
                    }
                }
                Spacer(Modifier.height(24.dp))
            }
        }
    }
}
