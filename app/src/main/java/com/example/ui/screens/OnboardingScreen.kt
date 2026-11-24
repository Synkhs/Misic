package com.example.ui.screens

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
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.BatteryChargingFull
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.Headset
import androidx.compose.material.icons.rounded.NotificationsActive
import androidx.compose.material.icons.rounded.Widgets
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.example.R
import com.example.core.media.HealthCheckId
import com.example.core.media.HealthLight
import com.example.designsystem.MagicTheme
import com.example.ui.MagicMusicUiState
import com.example.ui.components.MagicSurfaceCard

@Composable
fun OnboardingScreen(
    state: MagicMusicUiState,
    onBack: () -> Unit,
    onFixStep: (HealthCheckId) -> Unit,
    onComplete: () -> Unit,
) {
    val tokens = MagicTheme.tokens
    val colors = MaterialTheme.colorScheme
    val summary = state.systemHealth
    val completedSteps = summary.greenCount.coerceAtMost(4)
    val progress = (completedSteps.toFloat() / 4f).coerceIn(0.15f, 1f)

    val byId = summary.items.associateBy { it.id }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(tokens.backgroundBrush)
            .statusBarsPadding()
            .navigationBarsPadding(),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = tokens.spacing.sm, vertical = tokens.spacing.xs),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            IconButton(onClick = onBack) {
                Icon(Icons.AutoMirrored.Rounded.ArrowBack, contentDescription = stringResource(R.string.action_back))
            }
            Text(
                text = "Configuración inicial",
                style = MaterialTheme.typography.titleMedium,
                color = colors.onSurfaceVariant,
            )
        }

        LazyColumn(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .testTag("onboarding_list"),
            contentPadding = PaddingValues(
                horizontal = tokens.spacing.screenMargin,
                vertical = tokens.spacing.sm,
            ),
            verticalArrangement = Arrangement.spacedBy(tokens.spacing.md),
        ) {
            item {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        text = stringResource(R.string.onboarding_title),
                        style = MaterialTheme.typography.headlineMedium,
                        color = colors.onBackground,
                    )
                    Text(
                        text = "Conecta sesiones multimedia, pantalla de bloqueo, batería sin cierres y tu primer widget.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = colors.onSurfaceVariant,
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    LinearProgressIndicator(
                        progress = { progress },
                        color = colors.primary,
                        trackColor = tokens.semantic.divider,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(6.dp)
                            .clip(CircleShape),
                    )
                }
            }

            item {
                val listenerReady = byId[HealthCheckId.NOTIFICATION_LISTENER]?.light == HealthLight.GREEN
                OnboardingStepCard(
                    icon = Icons.Rounded.Headset,
                    title = "1. Acceso a sesiones de música",
                    description = "Detecta automáticamente Spotify, YouTube Music, Amazon Music o Apple Music.",
                    isDone = listenerReady,
                    actionLabel = if (listenerReady) "Activo" else "Conceder",
                    onAction = { onFixStep(HealthCheckId.NOTIFICATION_LISTENER) },
                )
            }

            item {
                val notifReady = byId[HealthCheckId.POST_NOTIFICATIONS]?.light == HealthLight.GREEN
                OnboardingStepCard(
                    icon = Icons.Rounded.NotificationsActive,
                    title = "2. Controles en pantalla de bloqueo",
                    description = "Muestra botones personalizados y progreso sobre el bloqueo de Android.",
                    isDone = notifReady,
                    actionLabel = if (notifReady) "Activo" else "Activar",
                    onAction = { onFixStep(HealthCheckId.POST_NOTIFICATIONS) },
                )
            }

            item {
                val batteryReady = byId[HealthCheckId.BATTERY_OPTIMIZATION]?.light == HealthLight.GREEN
                OnboardingStepCard(
                    icon = Icons.Rounded.BatteryChargingFull,
                    title = "3. Estabilidad en segundo plano",
                    description = "Evita que el sistema pause los widgets al apagar la pantalla.",
                    isDone = batteryReady,
                    actionLabel = if (batteryReady) "Activo" else "Revisar",
                    onAction = { onFixStep(HealthCheckId.BATTERY_OPTIMIZATION) },
                )
            }

            item {
                val widgetReady = byId[HealthCheckId.WIDGET_PINNED]?.light == HealthLight.GREEN
                OnboardingStepCard(
                    icon = Icons.Rounded.Widgets,
                    title = "4. Añadir Widget al escritorio",
                    description = "Fija tu Tarjeta Control 4×2 o Cápsula 4×1 con un toque.",
                    isDone = widgetReady,
                    actionLabel = if (widgetReady) "Añadido" else "Añadir widget",
                    onAction = { onFixStep(HealthCheckId.WIDGET_PINNED) },
                )
            }
        }

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = tokens.spacing.screenMargin, vertical = tokens.spacing.md),
        ) {
            Button(
                onClick = onComplete,
                shape = CircleShape,
                colors = ButtonDefaults.buttonColors(
                    containerColor = colors.primary,
                    contentColor = colors.onPrimary,
                ),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(54.dp)
                    .testTag("onboarding_continue_btn"),
            ) {
                Text(
                    text = stringResource(R.string.action_continue),
                    style = MaterialTheme.typography.titleMedium,
                )
            }
        }
    }
}

@Composable
private fun OnboardingStepCard(
    icon: ImageVector,
    title: String,
    description: String,
    isDone: Boolean,
    actionLabel: String,
    onAction: () -> Unit,
) {
    val tokens = MagicTheme.tokens
    val colors = MaterialTheme.colorScheme
    MagicSurfaceCard(highlightBorder = isDone) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Box(
                modifier = Modifier
                    .size(44.dp)
                    .clip(CircleShape)
                    .background(
                        if (isDone) tokens.semantic.success.copy(alpha = 0.16f) else colors.primary.copy(alpha = 0.16f),
                    ),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    imageVector = if (isDone) Icons.Rounded.CheckCircle else icon,
                    contentDescription = null,
                    tint = if (isDone) tokens.semantic.success else colors.primary,
                )
            }
            Spacer(modifier = Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleSmall,
                    color = colors.onSurface,
                )
                Text(
                    text = description,
                    style = MaterialTheme.typography.bodySmall,
                    color = colors.onSurfaceVariant,
                )
            }
            Spacer(modifier = Modifier.width(8.dp))
            FilledTonalButton(
                onClick = onAction,
                enabled = !isDone,
            ) {
                Text(actionLabel)
            }
        }
    }
}
