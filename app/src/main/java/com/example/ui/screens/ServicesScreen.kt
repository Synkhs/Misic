package com.example.ui.screens

import androidx.compose.foundation.BorderStroke
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
import androidx.compose.material.icons.rounded.Album
import androidx.compose.material.icons.automirrored.rounded.OpenInNew
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import com.example.data.InstalledServiceStatus
import com.example.ui.MagicMusicUiState
import com.example.ui.components.MagicGlassCard
import com.example.ui.theme.HonorCobalt
import com.example.ui.theme.HonorCyan
import com.example.ui.theme.HonorMint

@Composable
fun ServicesScreen(
    state: MagicMusicUiState,
    onLaunchService: (String) -> Unit,
    onRefreshServices: () -> Unit,
    onGrantListenerPermission: () -> Unit,
) {
    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .testTag("services_list"),
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        item {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        text = "Servicios Compatibles",
                        style = MaterialTheme.typography.headlineMedium,
                        color = MaterialTheme.colorScheme.onBackground,
                    )
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

                MagicGlassCard(highlightBorder = !state.hasListenerPermission) {
                    Text(
                        text = "Conexión Universal Android MediaSession",
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onSurface,
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "MagicMusic detecta automáticamente Spotify, YouTube Music, Amazon Music o cualquier otro reproductor multimedia activo sin necesitar inicios de sesión ni APIs cerradas.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    if (!state.hasListenerPermission) {
                        Spacer(modifier = Modifier.height(10.dp))
                        Button(
                            onClick = onGrantListenerPermission,
                            colors = ButtonDefaults.buttonColors(
                                containerColor = HonorCobalt,
                                contentColor = Color.White,
                            ),
                            modifier = Modifier.fillMaxWidth(),
                        ) {
                            Text("Conceder Acceso a Sesiones Multimedia")
                        }
                    }
                }
            }
        }

        items(state.installedServices, key = { it.service.id }) { status ->
            ServiceCard(
                status = status,
                onLaunch = { onLaunchService(status.service.packageName) },
            )
        }
    }
}

@Composable
private fun ServiceCard(
    status: InstalledServiceStatus,
    onLaunch: () -> Unit,
) {
    val svc = status.service
    Surface(
        shape = RoundedCornerShape(24.dp),
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.9f),
        border = BorderStroke(
            1.dp,
            if (status.isActiveSession) svc.accentColor.copy(alpha = 0.7f) else Color.White.copy(alpha = 0.14f),
        ),
        modifier = Modifier
            .fillMaxWidth()
            .testTag("service_card_${svc.id}"),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .clip(RoundedCornerShape(16.dp))
                    .background(svc.accentColor.copy(alpha = 0.18f)),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    imageVector = Icons.Rounded.Album,
                    contentDescription = null,
                    tint = svc.accentColor,
                    modifier = Modifier.size(26.dp),
                )
            }

            Spacer(modifier = Modifier.width(14.dp))

            Column(modifier = Modifier.weight(1f)) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    Text(
                        text = svc.displayName,
                        style = MaterialTheme.typography.titleMedium,
                        color = Color.White,
                    )
                    if (status.isActiveSession) {
                        Surface(
                            shape = CircleShape,
                            color = HonorMint.copy(alpha = 0.18f),
                        ) {
                            Text(
                                text = "ACTIVA",
                                style = MaterialTheme.typography.labelSmall,
                                color = HonorMint,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp),
                            )
                        }
                    }
                }
                Text(
                    text = svc.subtitle,
                    style = MaterialTheme.typography.bodyMedium,
                    color = Color(0xFF94A3B8),
                )
                Text(
                    text = if (status.isInstalled) "Instalada en el dispositivo" else "Disponible en Google Play",
                    style = MaterialTheme.typography.labelSmall,
                    color = if (status.isInstalled) HonorCyan else Color(0xFF64748B),
                )
            }

            Spacer(modifier = Modifier.width(8.dp))

            FilledTonalButton(
                onClick = onLaunch,
                modifier = Modifier.testTag("launch_service_${svc.id}"),
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Rounded.OpenInNew,
                    contentDescription = null,
                    modifier = Modifier.size(16.dp),
                )
                Spacer(modifier = Modifier.width(4.dp))
                Text(if (status.isInstalled) "Abrir" else "Instalar")
            }
        }
    }
}
