package com.example.ui.screens

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.provider.Settings
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.Album
import androidx.compose.material.icons.rounded.Backup
import androidx.compose.material.icons.rounded.BarChart
import androidx.compose.material.icons.rounded.BugReport
import androidx.compose.material.icons.rounded.DeleteOutline
import androidx.compose.material.icons.rounded.Equalizer
import androidx.compose.material.icons.rounded.HealthAndSafety
import androidx.compose.material.icons.rounded.Info
import androidx.compose.material.icons.rounded.ImageSearch
import androidx.compose.material.icons.rounded.Lock
import androidx.compose.material.icons.rounded.Notifications
import androidx.compose.material.icons.rounded.Palette
import androidx.compose.material.icons.rounded.RocketLaunch
import androidx.compose.material.icons.rounded.Shield
import androidx.compose.material.icons.rounded.SpeakerGroup
import androidx.compose.material.icons.rounded.Widgets
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.BuildConfig
import com.example.R
import com.example.core.media.HealthCheckId
import com.example.core.media.HealthLight
import com.example.data.AudioOutputDevice
import com.example.designsystem.MagicTheme
import com.example.designsystem.expressive.MagicFlexibleMediumTopBar
import com.example.ui.MagicMusicUiState
import com.example.ui.components.EqualizerBand
import com.example.ui.components.EqualizerPreset
import com.example.ui.components.MagicEqualizerPanel
import com.example.ui.components.MagicSettingsRowCard
import com.example.ui.components.MagicSettingsSwitchRow
import com.example.ui.components.MagicSurfaceCard
import com.example.ui.components.MagicTrackListItem
import com.example.ui.components.TrackItemVariant
import com.example.ui.components.TrackListItemModel
import com.example.ui.components.formatTimeMs

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    state: MagicMusicUiState,
    onOpenAppearance: () -> Unit,
    onOpenServices: () -> Unit,
    onOpenAudioQuality: () -> Unit,
    onOpenControls: () -> Unit = {},
    onOpenWidgetStudio: () -> Unit = {},
    onOpenStats: () -> Unit,
    onOpenSystemHealth: () -> Unit,
    onOpenOnboarding: () -> Unit,
    onOpenDeveloperShowcase: () -> Unit,
    onOpenArtworkInspector: () -> Unit = {},
    onToggleBridge: (Boolean) -> Unit,
    onToggleLockVisibility: (Boolean) -> Unit,
    onToggleLockArtwork: (Boolean) -> Unit,
    onToggleRequireUnlockToOpenApp: (Boolean) -> Unit,
    onExportBackupJson: ((String) -> Unit) -> Unit,
    onImportBackupJson: (String) -> Unit,
    onShowFeedback: (String) -> Unit,
) {
    val context = LocalContext.current
    val tokens = MagicTheme.tokens
    val colors = MaterialTheme.colorScheme
    val scrollBehavior = TopAppBarDefaults.exitUntilCollapsedScrollBehavior()

    var showNotificationsDialog by remember { mutableStateOf(false) }
    var showPrivacyDialog by remember { mutableStateOf(false) }
    var showBackupDialog by remember { mutableStateOf(false) }
    var showAboutDialog by remember { mutableStateOf(false) }
    var backupDraft by remember { mutableStateOf("") }

    Scaffold(
        modifier = Modifier
            .fillMaxSize()
            .nestedScroll(scrollBehavior.nestedScrollConnection),
        containerColor = Color.Transparent,
        topBar = {
            MagicFlexibleMediumTopBar(
                title = stringResource(R.string.nav_settings),
                subtitle = "Cuentas, bloqueo, widgets, privacidad y diagnóstico",
                useExpressiveM3 = tokens.useExpressiveM3,
                scrollBehavior = scrollBehavior,
            )
        },
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .testTag("settings_hub_list"),
            contentPadding = PaddingValues(
                start = tokens.spacing.screenMargin,
                end = tokens.spacing.screenMargin,
                top = tokens.spacing.sm,
                bottom = tokens.spacing.lg,
            ),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            item {
                MagicSettingsRowCard(
                    icon = Icons.Rounded.Album,
                    title = "Cuentas y Reproductores",
                    subtitle = "Spotify OAuth, YouTube Google, Apple Music y MediaSession",
                    onClick = onOpenServices,
                    modifier = Modifier.testTag("settings_row_services"),
                )
            }

            item {
                MagicSettingsRowCard(
                    icon = Icons.Rounded.Palette,
                    title = "Estilo y Material 3 Expressive",
                    subtitle = "Estudio de color MCU, contraste WCAG, formas y tipografía",
                    onClick = onOpenAppearance,
                    modifier = Modifier.testTag("settings_row_appearance"),
                )
            }

            item {
                MagicSettingsRowCard(
                    icon = Icons.Rounded.Widgets,
                    title = stringResource(R.string.nav_widgets),
                    subtitle = "4 formatos adaptables, color propio por widget y plantillas",
                    onClick = onOpenWidgetStudio,
                    modifier = Modifier.testTag("settings_row_widgets"),
                )
            }

            item {
                MagicSettingsRowCard(
                    icon = Icons.Rounded.Lock,
                    title = "Pantalla de bloqueo y Ajustes Rápidos",
                    subtitle = "Privacidad MediaStyle, botones rápidos y 7 mosaicos QS",
                    onClick = onOpenControls,
                    modifier = Modifier.testTag("settings_row_controls"),
                )
            }

            item {
                MagicSettingsRowCard(
                    icon = Icons.Rounded.Equalizer,
                    title = stringResource(R.string.eq_title),
                    subtitle = "Bandas dB, DSP local, perfiles Bluetooth y crossfade",
                    onClick = onOpenAudioQuality,
                    modifier = Modifier.testTag("settings_row_audio"),
                )
            }

            item {
                MagicSettingsRowCard(
                    icon = Icons.Rounded.BarChart,
                    title = stringResource(R.string.stats_title),
                    subtitle = "${state.listeningStats.totalMinutes} min registrados en el dispositivo",
                    onClick = onOpenStats,
                    modifier = Modifier.testTag("settings_row_stats"),
                )
            }

            item {
                val pendingText = if (state.systemHealth.hasPendingIssues) {
                    "${state.systemHealth.pendingCount} revisiones pendientes"
                } else {
                    "Todos los permisos y componentes activos"
                }
                MagicSettingsRowCard(
                    icon = Icons.Rounded.HealthAndSafety,
                    title = stringResource(R.string.system_health_title),
                    subtitle = pendingText,
                    onClick = onOpenSystemHealth,
                    modifier = Modifier.testTag("settings_row_health"),
                )
            }

            item {
                MagicSettingsRowCard(
                    icon = Icons.Rounded.RocketLaunch,
                    title = "Asistente de configuración",
                    subtitle = "Repasa los permisos, widget y mosaico paso a paso",
                    onClick = onOpenOnboarding,
                    modifier = Modifier.testTag("settings_row_onboarding"),
                )
            }

            item {
                MagicSettingsRowCard(
                    icon = Icons.Rounded.Notifications,
                    title = "Notificaciones",
                    subtitle = "Controles MediaStyle y canales del sistema",
                    onClick = { showNotificationsDialog = true },
                )
            }

            item {
                MagicSettingsRowCard(
                    icon = Icons.Rounded.Shield,
                    title = "Qué datos usa MagicMusic",
                    subtitle = "Privacidad local, permisos mínimos y cifrado Keystore AES256",
                    onClick = { showPrivacyDialog = true },
                    modifier = Modifier.testTag("settings_row_privacy"),
                )
            }

            item {
                MagicSettingsRowCard(
                    icon = Icons.Rounded.Backup,
                    title = "Copia de seguridad",
                    subtitle = "Exporta o restaura tus widgets, paletas y preferencias en JSON",
                    onClick = { showBackupDialog = true },
                )
            }

            item {
                MagicSettingsRowCard(
                    icon = Icons.Rounded.Info,
                    title = "Acerca de MagicMusic",
                    subtitle = "Versión ${BuildConfig.VERSION_NAME} • Control universal MediaSession",
                    onClick = { showAboutDialog = true },
                )
            }

            if (BuildConfig.DEBUG) {
                item {
                    MagicSettingsRowCard(
                        icon = Icons.Rounded.ImageSearch,
                        title = "Inspector de carátulas (Debug)",
                        subtitle = "Estado de Coil, MediaStore loadThumbnail y SessionArtworkStabilizer",
                        onClick = onOpenArtworkInspector,
                        modifier = Modifier.testTag("settings_row_artwork_inspector"),
                    )
                }
                item {
                    MagicSettingsRowCard(
                        icon = Icons.Rounded.BugReport,
                        title = "Galería Material 3 Expressive (Debug)",
                        subtitle = "Componentes Material 3, pruebas visuales y variantes tonales",
                        onClick = onOpenDeveloperShowcase,
                        modifier = Modifier.testTag("settings_row_developer"),
                    )
                }
            }
        }
    }

    if (showNotificationsDialog) {
        AlertDialog(
            onDismissRequest = { showNotificationsDialog = false },
            title = { Text("Notificaciones") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    MagicSettingsSwitchRow(
                        title = "Puente de notificación activo",
                        subtitle = "Mantiene controles MediaStyle en barra y bloqueo",
                        checked = state.lockAndQsConfig.bridgeEnabled,
                        onCheckedChange = onToggleBridge,
                    )
                    MagicSettingsSwitchRow(
                        title = "Visible en bloqueo",
                        subtitle = "Muestra botones multimedia con la pantalla bloqueada",
                        checked = state.lockAndQsConfig.showOnLockScreen,
                        onCheckedChange = onToggleLockVisibility,
                    )
                    OutlinedButton(
                        onClick = {
                            val intent = Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS).apply {
                                putExtra(Settings.EXTRA_APP_PACKAGE, context.packageName)
                                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                            }
                            runCatching { context.startActivity(intent) }
                        },
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        Text("Abrir ajustes del sistema")
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showNotificationsDialog = false }) { Text("Listo") }
            },
        )
    }

    if (showPrivacyDialog) {
        AlertDialog(
            onDismissRequest = { showPrivacyDialog = false },
            title = { Text("Qué datos usa MagicMusic") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        text = "• Biblioteca local: lee solo tus archivos de audio locales (READ_MEDIA_AUDIO / READ_EXTERNAL_STORAGE en Android <=12) mediante MediaStore y Room sin subir nada a servidores.\n" +
                            "• Sesiones activas: lee metadatos de MediaSession únicamente para mostrar títulos, carátulas y controles.\n" +
                            "• Cuentas oficiales: los tokens OAuth de Spotify o Google/YouTube se cifran en tu dispositivo con Android Keystore + Tink AES256-GCM. MagicMusic no recolecta analíticas ni telemetría.",
                        style = MaterialTheme.typography.bodySmall,
                        color = colors.onSurfaceVariant,
                    )
                    MagicSettingsSwitchRow(
                        title = "Mostrar carátula en bloqueo",
                        subtitle = "Oculta la portada del álbum cuando el móvil está bloqueado",
                        checked = state.lockAndQsConfig.showArtworkOnLockScreen,
                        onCheckedChange = onToggleLockArtwork,
                    )
                    MagicSettingsSwitchRow(
                        title = "Pedir desbloqueo al abrir app",
                        subtitle = "Evita abrir reproductores desde la pantalla bloqueada",
                        checked = state.lockAndQsConfig.requireUnlockToOpenApp,
                        onCheckedChange = onToggleRequireUnlockToOpenApp,
                    )
                }
            },
            confirmButton = {
                TextButton(onClick = { showPrivacyDialog = false }) { Text("Cerrar") }
            },
        )
    }

    if (showBackupDialog) {
        AlertDialog(
            onDismissRequest = { showBackupDialog = false },
            title = { Text("Copia de seguridad") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text(
                        text = "Exporta tu configuración (tema, widgets, bloqueo y plantillas) o pega una copia previa para restaurarla.",
                        style = MaterialTheme.typography.bodySmall,
                        color = colors.onSurfaceVariant,
                    )
                    FilledTonalButton(
                        onClick = {
                            onExportBackupJson { payload ->
                                backupDraft = payload
                                val cm = context.getSystemService(Context.CLIPBOARD_SERVICE) as? ClipboardManager
                                cm?.setPrimaryClip(ClipData.newPlainText("MagicMusic Backup", payload))
                                onShowFeedback("Copia de seguridad copiada al portapapeles")
                            }
                        },
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        Text("Exportar y copiar copia")
                    }
                    OutlinedTextField(
                        value = backupDraft,
                        onValueChange = { backupDraft = it },
                        label = { Text("Texto de copia o JSON") },
                        maxLines = 5,
                        modifier = Modifier.fillMaxWidth(),
                    )
                }
            },
            confirmButton = {
                Button(
                    enabled = backupDraft.isNotBlank(),
                    onClick = {
                        onImportBackupJson(backupDraft)
                        showBackupDialog = false
                    },
                ) {
                    Text("Restaurar")
                }
            },
            dismissButton = {
                TextButton(onClick = { showBackupDialog = false }) { Text("Cancelar") }
            },
        )
    }

    if (showAboutDialog) {
        AlertDialog(
            onDismissRequest = { showAboutDialog = false },
            title = { Text("MagicMusic") },
            text = {
                Text(
                    "Versión ${BuildConfig.VERSION_NAME}\n\nReproductor Hi-Res y centro musical Material Design 3 Expressive con biblioteca local indexada, widgets personalizables, pantalla de bloqueo, Ajustes Rápidos y conectores oficiales.\n\n${stringResource(R.string.trademark_disclaimer)}",
                    style = MaterialTheme.typography.bodyMedium,
                )
            },
            confirmButton = {
                TextButton(onClick = { showAboutDialog = false }) { Text("Aceptar") }
            },
        )
    }
}

@Composable
fun SystemHealthScreen(
    state: MagicMusicUiState,
    onBack: () -> Unit,
    onFixItem: (HealthCheckId) -> Unit,
) {
    val tokens = MagicTheme.tokens
    val colors = MaterialTheme.colorScheme
    val summary = state.systemHealth

    val headerColor = when (summary.overallLight) {
        HealthLight.GREEN -> tokens.semantic.success
        HealthLight.AMBER -> tokens.semantic.warning
        HealthLight.RED -> colors.error
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .testTag("system_health_list"),
        contentPadding = PaddingValues(
            start = tokens.spacing.screenMargin,
            end = tokens.spacing.screenMargin,
            top = tokens.spacing.sm,
            bottom = tokens.spacing.lg,
        ),
        verticalArrangement = Arrangement.spacedBy(tokens.spacing.md),
    ) {
        item {
            Row(verticalAlignment = Alignment.CenterVertically) {
                IconButton(
                    onClick = onBack,
                    modifier = Modifier.testTag("system_health_back_btn"),
                ) {
                    Icon(Icons.AutoMirrored.Rounded.ArrowBack, contentDescription = stringResource(R.string.action_back))
                }
                Text(
                    text = stringResource(R.string.system_health_title),
                    style = MaterialTheme.typography.headlineSmall,
                    color = colors.onBackground,
                )
            }
        }

        item {
            MagicSurfaceCard(highlightBorder = summary.hasPendingIssues) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    Box(
                        modifier = Modifier
                            .size(16.dp)
                            .clip(CircleShape)
                            .background(headerColor),
                    )
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = when (summary.overallLight) {
                                HealthLight.GREEN -> "Todo configurado correctamente"
                                HealthLight.AMBER -> "Mejoras recomendadas disponibles"
                                HealthLight.RED -> "Se requieren permisos críticos"
                            },
                            style = MaterialTheme.typography.titleMedium,
                            color = colors.onSurface,
                        )
                        Text(
                            text = "${summary.greenCount} listos • ${summary.amberCount} recomendados • ${summary.redCount} críticos",
                            style = MaterialTheme.typography.bodySmall,
                            color = colors.onSurfaceVariant,
                        )
                    }
                }
            }
        }

        items(summary.items, key = { it.id.name }) { item ->
            val dotColor = when (item.light) {
                HealthLight.GREEN -> tokens.semantic.success
                HealthLight.AMBER -> tokens.semantic.warning
                HealthLight.RED -> colors.error
            }
            MagicSurfaceCard {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Box(
                        modifier = Modifier
                            .size(12.dp)
                            .clip(CircleShape)
                            .background(dotColor),
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = item.title,
                            style = MaterialTheme.typography.titleSmall,
                            color = colors.onSurface,
                        )
                        Text(
                            text = item.subtitle,
                            style = MaterialTheme.typography.bodySmall,
                            color = colors.onSurfaceVariant,
                        )
                    }
                    if (item.light != HealthLight.GREEN) {
                        Spacer(modifier = Modifier.width(8.dp))
                        FilledTonalButton(
                            onClick = { onFixItem(item.id) },
                            modifier = Modifier.testTag("fix_health_${item.id.name.lowercase()}"),
                        ) {
                            Text(item.actionLabel)
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun AudioQualityScreen(
    state: MagicMusicUiState,
    onBack: () -> Unit,
    onToggleEqEnabled: (Boolean) -> Unit,
    onBandChange: (Int, Float) -> Unit,
    onSelectPreset: (Int, EqualizerPreset) -> Unit,
    onModeChange: (Boolean) -> Unit,
    onToggleHiResBuffer: (Boolean) -> Unit,
    onRefreshOutputs: () -> Unit,
    onSelectOutput: (AudioOutputDevice) -> Unit,
) {
    val tokens = MagicTheme.tokens
    val colors = MaterialTheme.colorScheme
    val eqCfg = state.equalizerConfig
    val presets = EqualizerPreset.entries
    val activePreset = presets.getOrElse(eqCfg.presetIndex) { EqualizerPreset.FLAT }
    val freqs = remember { listOf("60Hz", "230Hz", "910Hz", "3.6kHz", "14kHz") }
    val bands = remember(eqCfg.bandGainsDb) {
        freqs.mapIndexed { idx, label ->
            EqualizerBand(freqLabel = label, gainDb = eqCfg.bandGainsDb.getOrElse(idx) { 0f })
        }
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .testTag("audio_quality_list"),
        contentPadding = PaddingValues(
            start = tokens.spacing.screenMargin,
            end = tokens.spacing.screenMargin,
            top = tokens.spacing.sm,
            bottom = tokens.spacing.lg,
        ),
        verticalArrangement = Arrangement.spacedBy(tokens.spacing.md),
    ) {
        item {
            Row(verticalAlignment = Alignment.CenterVertically) {
                IconButton(onClick = onBack) {
                    Icon(Icons.AutoMirrored.Rounded.ArrowBack, contentDescription = stringResource(R.string.action_back))
                }
                Text(
                    text = stringResource(R.string.eq_title),
                    style = MaterialTheme.typography.titleLarge,
                    color = colors.onBackground,
                )
            }
        }

        item {
            MagicSurfaceCard {
                MagicSettingsSwitchRow(
                    title = "Ecualizador DSP (Motor Local)",
                    subtitle = "Aplica la curva de 5 bandas a la sesión local Hi-Res",
                    checked = eqCfg.enabled,
                    onCheckedChange = onToggleEqEnabled,
                    switchTestTag = "eq_enabled_switch",
                )
                MagicSettingsSwitchRow(
                    title = "Buffer Hi-Res de alta estabilidad",
                    subtitle = "Prioriza continuidad frente a cortes en Bluetooth LDAC/AAC",
                    checked = eqCfg.hiResBufferHigh,
                    onCheckedChange = onToggleHiResBuffer,
                )
            }
        }

        item {
            MagicEqualizerPanel(
                bands = bands,
                selectedPreset = activePreset,
                isAdvancedMode = eqCfg.isAdvancedMode,
                onBandChange = onBandChange,
                onSelectPreset = { preset -> onSelectPreset(presets.indexOf(preset), preset) },
                onModeChange = onModeChange,
            )
        }

        item {
            MagicSurfaceCard {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Rounded.SpeakerGroup, contentDescription = null, tint = colors.primary)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Salidas detectadas",
                            style = MaterialTheme.typography.titleMedium,
                            color = colors.onSurface,
                        )
                    }
                    TextButton(onClick = onRefreshOutputs) { Text("Actualizar") }
                }
                Spacer(modifier = Modifier.height(6.dp))
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    state.audioOutputs.forEach { dev ->
                        Surface(
                            onClick = { onSelectOutput(dev) },
                            shape = RoundedCornerShape(tokens.radii.listItem),
                            color = if (dev.isActive) tokens.semantic.selectedContainer else colors.surfaceContainerLow,
                            border = BorderStroke(1.dp, if (dev.isActive) colors.primary else tokens.semantic.divider),
                            modifier = Modifier.fillMaxWidth(),
                        ) {
                            Row(
                                modifier = Modifier.padding(12.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically,
                            ) {
                                Column {
                                    Text(dev.name, style = MaterialTheme.typography.titleSmall, color = colors.onSurface)
                                    Text(dev.typeLabel, style = MaterialTheme.typography.bodySmall, color = colors.onSurfaceVariant)
                                }
                                if (dev.isActive) {
                                    Text("Activa", style = MaterialTheme.typography.labelMedium, color = colors.primary)
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun LocalStatsScreen(
    state: MagicMusicUiState,
    onBack: () -> Unit,
    onClearHistory: () -> Unit,
    onOpenActiveApp: (String) -> Unit,
) {
    val tokens = MagicTheme.tokens
    val colors = MaterialTheme.colorScheme
    val stats = state.listeningStats
    var showConfirmClear by remember { mutableStateOf(false) }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .testTag("local_stats_list"),
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
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Rounded.ArrowBack, contentDescription = stringResource(R.string.action_back))
                    }
                    Text(
                        text = stringResource(R.string.stats_title),
                        style = MaterialTheme.typography.titleLarge,
                        color = colors.onBackground,
                    )
                }
                TextButton(
                    onClick = { showConfirmClear = true },
                    modifier = Modifier.testTag("clear_stats_btn"),
                ) {
                    Icon(Icons.Rounded.DeleteOutline, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Borrar")
                }
            }
        }

        // Total minutes & 7-day bar chart matching reference 19
        item {
            MagicSurfaceCard {
                Text(
                    text = "Tiempo escuchado controlado localmente",
                    style = MaterialTheme.typography.labelLarge,
                    color = colors.onSurfaceVariant,
                )
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = "${stats.totalMinutes} min",
                    style = MaterialTheme.typography.displayMedium,
                    fontWeight = FontWeight.Bold,
                    color = colors.primary,
                )
                Spacer(modifier = Modifier.height(16.dp))

                val days = listOf("L", "M", "X", "J", "V", "S", "D")
                val values = (0 until 7).map { idx -> stats.dailyMinutes.getOrElse(idx) { 0 } }
                val maxVal = (values.maxOrNull() ?: 1).coerceAtLeast(1)

                Canvas(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(130.dp),
                ) {
                    val barCount = 7
                    val slotWidth = size.width / barCount
                    val barWidth = slotWidth * 0.5f
                    values.forEachIndexed { idx, value ->
                        val frac = (value.toFloat() / maxVal.toFloat()).coerceIn(0.08f, 1f)
                        val barHeight = size.height * frac
                        val left = idx * slotWidth + (slotWidth - barWidth) / 2f
                        val top = size.height - barHeight
                        drawRoundRect(
                            color = if (value > 0) colors.primary else tokens.semantic.divider,
                            topLeft = Offset(left, top),
                            size = Size(barWidth, barHeight),
                            cornerRadius = CornerRadius(12f, 12f),
                        )
                    }
                }
                Spacer(modifier = Modifier.height(8.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceAround,
                ) {
                    days.forEach { d ->
                        Text(d, style = MaterialTheme.typography.labelSmall, color = colors.onSurfaceVariant)
                    }
                }
            }
        }

        if (stats.recentTracks.isNotEmpty()) {
            item {
                Text(
                    text = "Pistas recientes y más reproducidas",
                    style = MaterialTheme.typography.titleMedium,
                    color = colors.onBackground,
                )
            }
            items(stats.recentTracks, key = { it.id }) { entry ->
                MagicTrackListItem(
                    item = TrackListItemModel(
                        id = entry.id,
                        title = entry.title,
                        artist = "${entry.artist} • ${entry.service.displayName} (${entry.playCount}×)",
                        durationLabel = if (entry.durationMs > 0L) formatTimeMs(entry.durationMs) else "",
                        accentTint = entry.service.accentColor,
                    ),
                    variant = TrackItemVariant.DEFAULT,
                    onClick = { if (entry.packageName.isNotBlank()) onOpenActiveApp(entry.packageName) },
                )
            }
        }
    }

    if (showConfirmClear) {
        AlertDialog(
            onDismissRequest = { showConfirmClear = false },
            title = { Text("¿Borrar historial local?") },
            text = { Text("Se eliminarán los minutos acumulados y la lista de escuchado recientemente.") },
            confirmButton = {
                Button(
                    onClick = {
                        onClearHistory()
                        showConfirmClear = false
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = colors.error,
                        contentColor = colors.onError,
                    ),
                ) {
                    Text("Borrar historial")
                }
            },
            dismissButton = {
                TextButton(onClick = { showConfirmClear = false }) { Text("Cancelar") }
            },
        )
    }
}
