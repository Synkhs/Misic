package com.example.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
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
import androidx.compose.material.icons.automirrored.rounded.OpenInNew
import androidx.compose.material.icons.rounded.BatteryChargingFull
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material.icons.rounded.Star
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.example.R
import com.example.data.InstalledServiceStatus
import com.example.data.SessionPriorityRule
import com.example.designsystem.MagicTheme
import com.example.ui.MagicMusicUiState
import com.example.ui.components.MagicAppIconBadge
import com.example.ui.components.MagicSurfaceCard

@Composable
fun ServicesScreen(
    state: MagicMusicUiState,
    onLaunchService: (String) -> Unit,
    onRefreshServices: () -> Unit,
    onGrantListenerPermission: () -> Unit,
    onPriorityRuleSelected: (SessionPriorityRule) -> Unit,
    onToggleIgnoreVideo: (Boolean) -> Unit,
    onToggleManagedPackage: (packageName: String, enabled: Boolean) -> Unit,
    onSetPreferredPackage: (String) -> Unit,
    onOpenBatterySettings: () -> Unit,
    onBack: (() -> Unit)? = null,
) {
    val colors = MaterialTheme.colorScheme
    val tokens = MagicTheme.tokens

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .testTag("services_list"),
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
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.weight(1f),
                ) {
                    if (onBack != null) {
                        IconButton(
                            onClick = onBack,
                            modifier = Modifier.testTag("services_back_btn"),
                        ) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Rounded.ArrowBack,
                                contentDescription = stringResource(R.string.action_back),
                                tint = colors.onBackground,
                            )
                        }
                    }
                    Text(
                        text = stringResource(R.string.nav_services),
                        style = MaterialTheme.typography.titleLarge,
                        color = colors.onBackground,
                    )
                }
                FilledTonalButton(
                    onClick = onRefreshServices,
                    modifier = Modifier.testTag("refresh_services_btn"),
                ) {
                    Icon(
                        imageVector = Icons.Rounded.Refresh,
                        contentDescription = "Actualizar servicios",
                        modifier = Modifier.size(18.dp),
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Escanear")
                }
            }
        }

        item {
            MagicSurfaceCard(highlightBorder = !state.hasListenerPermission) {
                Text(
                    text = "Conexión Universal MediaSession",
                    style = MaterialTheme.typography.titleMedium,
                    color = colors.onSurface,
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "Detecta automáticamente cualquier reproductor compatible en tu Android (Pixel, Samsung, Xiaomi, Honor, OPPO, Motorola, etc.) sin cuentas ni servidores.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = colors.onSurfaceVariant,
                )
                if (!state.hasListenerPermission) {
                    Spacer(modifier = Modifier.height(10.dp))
                    Button(
                        onClick = onGrantListenerPermission,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = colors.primary,
                            contentColor = colors.onPrimary,
                        ),
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        Text("Conceder Acceso a Sesiones Multimedia")
                    }
                }
            }
        }

        item {
            MagicSurfaceCard {
                Text(
                    text = "Prioridad de Sesiones y Filtros",
                    style = MaterialTheme.typography.titleMedium,
                    color = colors.onSurface,
                )
                Spacer(modifier = Modifier.height(8.dp))

                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    SessionPriorityRule.entries.forEach { rule ->
                        val selected = state.sessionPreferences.priorityRule == rule
                        Surface(
                            shape = RoundedCornerShape(tokens.radii.listItem),
                            color = if (selected) tokens.semantic.selectedContainer.copy(alpha = 0.65f) else colors.surfaceContainerLow,
                            border = BorderStroke(
                                1.dp,
                                if (selected) colors.primary else tokens.semantic.divider,
                            ),
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { onPriorityRuleSelected(rule) }
                                .testTag("priority_rule_${rule.name.lowercase()}"),
                        ) {
                            Column(modifier = Modifier.padding(12.dp)) {
                                Text(rule.label, style = MaterialTheme.typography.labelLarge, color = colors.onSurface)
                                Text(
                                    rule.description,
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = colors.onSurfaceVariant,
                                )
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Column(modifier = Modifier.weight(1f).padding(end = 8.dp)) {
                        Text(
                            text = "Ignorar apps de vídeo",
                            style = MaterialTheme.typography.labelLarge,
                            color = colors.onSurface,
                        )
                        Text(
                            text = "Evita que YouTube o streaming de vídeo reemplacen tu música",
                            style = MaterialTheme.typography.bodyMedium,
                            color = colors.onSurfaceVariant,
                        )
                    }
                    Switch(
                        checked = state.sessionPreferences.ignoreVideoSessions,
                        onCheckedChange = onToggleIgnoreVideo,
                        modifier = Modifier.testTag("ignore_video_switch"),
                    )
                }
            }
        }

        state.oemBatteryGuide?.let { guide ->
            item {
                MagicSurfaceCard(highlightBorder = !guide.isIgnoringBatteryOptimizations) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.BatteryChargingFull,
                            contentDescription = null,
                            tint = if (guide.isIgnoringBatteryOptimizations) {
                                tokens.semantic.success
                            } else {
                                tokens.semantic.warning
                            },
                        )
                        Text(
                            text = "Compatibilidad ${guide.family.displayName}",
                            style = MaterialTheme.typography.titleMedium,
                            color = colors.onSurface,
                        )
                    }
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = guide.summary,
                        style = MaterialTheme.typography.bodyMedium,
                        color = colors.onSurfaceVariant,
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    guide.steps.forEach { step ->
                        Text(
                            text = "• $step",
                            style = MaterialTheme.typography.bodyMedium,
                            color = colors.onSurface,
                            modifier = Modifier.padding(vertical = 2.dp),
                        )
                    }
                    Spacer(modifier = Modifier.height(10.dp))
                    FilledTonalButton(
                        onClick = onOpenBatterySettings,
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("oem_battery_settings_btn"),
                    ) {
                        Text("Abrir Ajustes de Batería del Dispositivo")
                    }
                }
            }
        }

        items(state.installedServices, key = { it.packageName.ifBlank { it.service.id } }) { status ->
            ServiceCard(
                status = status,
                isPreferred = status.packageName == state.sessionPreferences.preferredServicePackage,
                onLaunch = { onLaunchService(status.packageName) },
                onToggleManaged = { enabled -> onToggleManagedPackage(status.packageName, enabled) },
                onSetPreferred = { onSetPreferredPackage(status.packageName) },
            )
        }
    }
}

@Composable
private fun ServiceCard(
    status: InstalledServiceStatus,
    isPreferred: Boolean,
    onLaunch: () -> Unit,
    onToggleManaged: (Boolean) -> Unit,
    onSetPreferred: () -> Unit,
) {
    val svc = status.service
    val colors = MaterialTheme.colorScheme
    val tokens = MagicTheme.tokens
    Surface(
        shape = RoundedCornerShape(tokens.radii.card),
        color = tokens.semantic.cardSurface,
        border = BorderStroke(
            1.dp,
            if (status.isActiveSession) svc.accentColor.copy(alpha = 0.7f) else tokens.semantic.divider,
        ),
        modifier = Modifier
            .fillMaxWidth()
            .testTag("service_card_${svc.id}"),
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                MagicAppIconBadge(
                    drawable = status.appIcon,
                    fallbackTint = svc.accentColor,
                    contentDescription = status.realAppName,
                    size = 46.dp,
                    cornerRadius = 14.dp,
                    service = svc,
                    serviceName = status.realAppName,
                )

                Spacer(modifier = Modifier.width(12.dp))

                Column(modifier = Modifier.weight(1f)) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        Text(
                            text = status.realAppName,
                            style = MaterialTheme.typography.titleMedium,
                            color = colors.onSurface,
                        )
                        if (status.isActiveSession) {
                            Surface(
                                shape = CircleShape,
                                color = tokens.semantic.success.copy(alpha = 0.18f),
                            ) {
                                Text(
                                    text = "ACTIVA",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = tokens.semantic.success,
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp),
                                )
                            }
                        }
                    }
                    Text(
                        text = if (status.isInstalled) "Instalada en el dispositivo" else "Disponible en tienda",
                        style = MaterialTheme.typography.labelSmall,
                        color = if (status.isInstalled) colors.primary else colors.onSurfaceVariant,
                    )
                }

                IconButton(
                    onClick = onSetPreferred,
                    modifier = Modifier.testTag("prefer_service_${svc.id}"),
                ) {
                    Icon(
                        imageVector = Icons.Rounded.Star,
                        contentDescription = "Marcar como preferida",
                        tint = if (isPreferred) tokens.semantic.warning else colors.onSurfaceVariant,
                    )
                }

                Switch(
                    checked = status.isManagedEnabled,
                    onCheckedChange = onToggleManaged,
                )
            }

            Spacer(modifier = Modifier.height(10.dp))

            FilledTonalButton(
                onClick = onLaunch,
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("launch_service_${svc.id}"),
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Rounded.OpenInNew,
                    contentDescription = null,
                    modifier = Modifier.size(16.dp),
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(if (status.isInstalled) "Abrir ${status.realAppName}" else "Instalar ${status.realAppName}")
            }
        }
    }
}
