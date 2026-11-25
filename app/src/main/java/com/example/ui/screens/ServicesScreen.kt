package com.example.ui.screens

import android.content.Context
import android.content.Intent
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
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
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.automirrored.rounded.OpenInNew
import androidx.compose.material.icons.rounded.AccountCircle
import androidx.compose.material.icons.rounded.BatteryChargingFull
import androidx.compose.material.icons.rounded.CloudDone
import androidx.compose.material.icons.rounded.Info
import androidx.compose.material.icons.rounded.LinkOff
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material.icons.rounded.Star
import androidx.compose.material.icons.rounded.Sync
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
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.R
import com.example.core.accounts.AccountConnectionState
import com.example.core.accounts.MusicSourceId
import com.example.core.accounts.SourceAccountSnapshot
import com.example.core.accounts.SourceTrackSummary
import com.example.data.InstalledServiceStatus
import com.example.data.SessionPriorityRule
import com.example.designsystem.MagicTheme
import com.example.designsystem.expressive.MagicFlexibleMediumTopBar
import com.example.ui.MagicMusicUiState
import com.example.ui.components.MagicAppIconBadge
import com.example.ui.components.MagicSurfaceCard

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun ServicesScreen(
    state: MagicMusicUiState,
    spotifySnapshot: SourceAccountSnapshot,
    appleMusicSnapshot: SourceAccountSnapshot,
    youtubeSnapshot: SourceAccountSnapshot,
    spotifySearchResults: List<SourceTrackSummary> = emptyList(),
    onBuildSpotifyAuthIntent: () -> Intent?,
    onHandleSpotifyAuthResult: (Intent?) -> Unit,
    onConnectYouTubeWithGoogle: (Context) -> Unit,
    onSyncAccountSource: (MusicSourceId) -> Unit,
    onDisconnectAccountSource: (MusicSourceId) -> Unit,
    onSearchSpotifyTracks: (String) -> Unit,
    onLaunchAccountExternalUri: (MusicSourceId, String?) -> Unit,
    onLaunchService: (String) -> Unit,
    onRefreshServices: () -> Unit,
    onGrantListenerPermission: () -> Unit,
    onPriorityRuleSelected: (SessionPriorityRule) -> Unit,
    onToggleIgnoreVideo: (Boolean) -> Unit,
    onToggleManagedPackage: (packageName: String, enabled: Boolean) -> Unit,
    onSetPreferredPackage: (String) -> Unit,
    onOpenBatterySettings: () -> Unit,
    onShowFeedback: (String) -> Unit = {},
    onBack: (() -> Unit)? = null,
) {
    val context = LocalContext.current
    val colors = MaterialTheme.colorScheme
    val tokens = MagicTheme.tokens
    val scrollBehavior = TopAppBarDefaults.exitUntilCollapsedScrollBehavior()

    var spotifySearchQuery by remember { mutableStateOf("") }

    val spotifyAuthLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult(),
    ) { result ->
        onHandleSpotifyAuthResult(result.data)
    }

    Scaffold(
        modifier = Modifier
            .fillMaxSize()
            .nestedScroll(scrollBehavior.nestedScrollConnection),
        containerColor = Color.Transparent,
        topBar = {
            MagicFlexibleMediumTopBar(
                title = stringResource(R.string.nav_services),
                subtitle = "Cuentas oficiales, MediaSession y compatibilidad",
                useExpressiveM3 = tokens.useExpressiveM3,
                scrollBehavior = scrollBehavior,
                navigationIcon = {
                    if (onBack != null) {
                        IconButton(
                            onClick = onBack,
                            modifier = Modifier.testTag("services_back_btn"),
                        ) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Rounded.ArrowBack,
                                contentDescription = stringResource(R.string.action_back),
                                tint = colors.onSurface,
                            )
                        }
                    }
                },
                actions = {
                    FilledTonalButton(
                        onClick = onRefreshServices,
                        modifier = Modifier
                            .padding(end = 8.dp)
                            .testTag("refresh_services_btn"),
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.Refresh,
                            contentDescription = "Actualizar servicios",
                            modifier = Modifier.size(18.dp),
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Escanear")
                    }
                },
            )
        },
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .testTag("services_list"),
            contentPadding = PaddingValues(
                start = tokens.spacing.screenMargin,
                end = tokens.spacing.screenMargin,
                top = tokens.spacing.sm,
                bottom = tokens.spacing.lg,
            ),
            verticalArrangement = Arrangement.spacedBy(tokens.spacing.md),
        ) {
            // Section: Official Connected Music Accounts
            item {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        text = "Cuentas oficiales (OAuth / SDK)",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = colors.onBackground,
                    )
                    Text(
                        text = stringResource(R.string.trademark_disclaimer),
                        style = MaterialTheme.typography.bodySmall,
                        color = colors.onSurfaceVariant,
                    )
                }
            }

            // Spotify OAuth PKCE Card
            item {
                AccountConnectionCard(
                    snapshot = spotifySnapshot,
                    connectLabel = "Conectar con Spotify",
                    onConnect = {
                        val intent = onBuildSpotifyAuthIntent()
                        if (intent != null) {
                            spotifyAuthLauncher.launch(intent)
                        } else {
                            onShowFeedback("Configura SPOTIFY_CLIENT_ID para habilitar OAuth PKCE de Spotify")
                        }
                    },
                    onSync = { onSyncAccountSource(MusicSourceId.SPOTIFY) },
                    onDisconnect = { onDisconnectAccountSource(MusicSourceId.SPOTIFY) },
                    onOpenOfficialApp = {
                        onLaunchAccountExternalUri(MusicSourceId.SPOTIFY, null)
                    },
                    extraContent = {
                        if (spotifySnapshot.connectionState is AccountConnectionState.Connected) {
                            Spacer(modifier = Modifier.height(10.dp))
                            OutlinedTextField(
                                value = spotifySearchQuery,
                                onValueChange = { spotifySearchQuery = it },
                                label = { Text("Buscar en Spotify Web API") },
                                singleLine = true,
                                trailingIcon = {
                                    IconButton(
                                        onClick = { onSearchSpotifyTracks(spotifySearchQuery) },
                                        modifier = Modifier.testTag("spotify_search_submit_btn"),
                                    ) {
                                        Icon(Icons.Rounded.Search, contentDescription = "Buscar en Spotify")
                                    }
                                },
                                modifier = Modifier.fillMaxWidth(),
                            )
                            if (spotifySearchResults.isNotEmpty()) {
                                Spacer(modifier = Modifier.height(8.dp))
                                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                    spotifySearchResults.take(5).forEach { track ->
                                        Surface(
                                            onClick = { onLaunchAccountExternalUri(MusicSourceId.SPOTIFY, track.externalUri) },
                                            shape = RoundedCornerShape(tokens.radii.listItem),
                                            color = colors.surfaceContainerHigh,
                                            modifier = Modifier.fillMaxWidth(),
                                        ) {
                                            Row(
                                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
                                                verticalAlignment = Alignment.CenterVertically,
                                            ) {
                                                Column(modifier = Modifier.weight(1f)) {
                                                    Text(
                                                        text = track.title,
                                                        style = MaterialTheme.typography.labelLarge,
                                                        color = colors.onSurface,
                                                        maxLines = 1,
                                                        overflow = TextOverflow.Ellipsis,
                                                    )
                                                    Text(
                                                        text = "${track.artist} • ${track.album}",
                                                        style = MaterialTheme.typography.bodySmall,
                                                        color = colors.onSurfaceVariant,
                                                        maxLines = 1,
                                                        overflow = TextOverflow.Ellipsis,
                                                    )
                                                }
                                                Icon(
                                                    imageVector = Icons.AutoMirrored.Rounded.OpenInNew,
                                                    contentDescription = "Abrir en Spotify",
                                                    tint = colors.primary,
                                                    modifier = Modifier.size(18.dp),
                                                )
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    },
                )
            }

            // YouTube Music Google Credential Manager + API Card
            item {
                AccountConnectionCard(
                    snapshot = youtubeSnapshot,
                    connectLabel = "Conectar con cuenta Google",
                    onConnect = { onConnectYouTubeWithGoogle(context) },
                    onSync = { onSyncAccountSource(MusicSourceId.YOUTUBE_MUSIC) },
                    onDisconnect = { onDisconnectAccountSource(MusicSourceId.YOUTUBE_MUSIC) },
                    onOpenOfficialApp = {
                        onLaunchAccountExternalUri(MusicSourceId.YOUTUBE_MUSIC, null)
                    },
                )
            }

            // Apple Music Card (honest MusicKit SDK status + MediaSession bridge)
            item {
                AccountConnectionCard(
                    snapshot = appleMusicSnapshot,
                    connectLabel = "Abrir Apple Music",
                    onConnect = { onLaunchAccountExternalUri(MusicSourceId.APPLE_MUSIC, null) },
                    onSync = { onSyncAccountSource(MusicSourceId.APPLE_MUSIC) },
                    onDisconnect = { onDisconnectAccountSource(MusicSourceId.APPLE_MUSIC) },
                    onOpenOfficialApp = {
                        onLaunchAccountExternalUri(MusicSourceId.APPLE_MUSIC, null)
                    },
                )
            }

            // MediaSession Universal Bridge
            item {
                MagicSurfaceCard(highlightBorder = !state.hasListenerPermission) {
                    Text(
                        text = "Conexión Universal MediaSession",
                        style = MaterialTheme.typography.titleMedium,
                        color = colors.onSurface,
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "Detecta automáticamente cualquier reproductor compatible en tu Android (Pixel, Samsung, Xiaomi, Honor, OPPO, Motorola, etc.) sin necesidad de iniciar sesión.",
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
                                color = if (selected) colors.primaryContainer else colors.surfaceContainerLow,
                                border = BorderStroke(
                                    1.dp,
                                    if (selected) colors.primary else colors.outlineVariant,
                                ),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable { onPriorityRuleSelected(rule) }
                                    .testTag("priority_rule_${rule.name.lowercase()}"),
                            ) {
                                Column(modifier = Modifier.padding(12.dp)) {
                                    Text(
                                        text = rule.label,
                                        style = MaterialTheme.typography.labelLarge,
                                        color = if (selected) colors.onPrimaryContainer else colors.onSurface,
                                    )
                                    Text(
                                        text = rule.description,
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = if (selected) colors.onPrimaryContainer.copy(alpha = 0.8f) else colors.onSurfaceVariant,
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
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun AccountConnectionCard(
    snapshot: SourceAccountSnapshot,
    connectLabel: String,
    onConnect: () -> Unit,
    onSync: () -> Unit,
    onDisconnect: () -> Unit,
    onOpenOfficialApp: () -> Unit,
    extraContent: @Composable () -> Unit = {},
) {
    val colors = MaterialTheme.colorScheme
    val tokens = MagicTheme.tokens
    val conn = snapshot.connectionState
    val isConnected = conn is AccountConnectionState.Connected

    MagicSurfaceCard(
        highlightBorder = isConnected,
        containerColor = if (isConnected) colors.surfaceContainerHigh else colors.surfaceContainer,
        modifier = Modifier.testTag("account_card_${snapshot.sourceId.id}"),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Surface(
                shape = CircleShape,
                color = if (isConnected) colors.primaryContainer else colors.surfaceContainerHighest,
                modifier = Modifier.size(44.dp),
            ) {
                androidx.compose.foundation.layout.Box(contentAlignment = Alignment.Center) {
                    Icon(
                        imageVector = if (isConnected) Icons.Rounded.CloudDone else Icons.Rounded.AccountCircle,
                        contentDescription = snapshot.sourceId.displayName,
                        tint = if (isConnected) colors.onPrimaryContainer else colors.onSurfaceVariant,
                    )
                }
            }

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = snapshot.sourceId.displayName,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = colors.onSurface,
                )
                val statusText = when (conn) {
                    is AccountConnectionState.Connected ->
                        "${conn.displayName} • ${conn.handleOrPlan} (${conn.playlistCount} listas)"
                    is AccountConnectionState.Connecting -> "Conectando de forma segura…"
                    is AccountConnectionState.NeedsConfig -> conn.message
                    is AccountConnectionState.FeatureDisabled -> conn.reason
                    is AccountConnectionState.Error -> conn.message
                    AccountConnectionState.Disconnected -> "Desconectado • Toca para vincular cuenta oficial"
                }
                Text(
                    text = statusText,
                    style = MaterialTheme.typography.bodySmall,
                    color = if (conn is AccountConnectionState.Error) colors.error else colors.onSurfaceVariant,
                )
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        Row(
            verticalAlignment = Alignment.Top,
            horizontalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            Icon(
                imageVector = Icons.Rounded.Info,
                contentDescription = null,
                tint = colors.onSurfaceVariant,
                modifier = Modifier.size(15.dp).padding(top = 2.dp),
            )
            Text(
                text = snapshot.sourceId.ownershipDisclaimer,
                style = MaterialTheme.typography.labelSmall,
                color = colors.onSurfaceVariant,
            )
        }

        Spacer(modifier = Modifier.height(10.dp))

        FlowRow(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            if (!isConnected && conn !is AccountConnectionState.FeatureDisabled) {
                Button(
                    onClick = onConnect,
                    modifier = Modifier.testTag("account_connect_${snapshot.sourceId.id}"),
                ) {
                    Text(connectLabel)
                }
            }
            if (isConnected) {
                FilledTonalButton(
                    onClick = onSync,
                    modifier = Modifier.testTag("account_sync_${snapshot.sourceId.id}"),
                ) {
                    Icon(Icons.Rounded.Sync, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Sincronizar")
                }
                OutlinedButton(
                    onClick = onDisconnect,
                    modifier = Modifier.testTag("account_disconnect_${snapshot.sourceId.id}"),
                ) {
                    Icon(Icons.Rounded.LinkOff, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Desconectar")
                }
            }
            OutlinedButton(
                onClick = onOpenOfficialApp,
                modifier = Modifier.testTag("account_open_app_${snapshot.sourceId.id}"),
            ) {
                Icon(Icons.AutoMirrored.Rounded.OpenInNew, contentDescription = null, modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(6.dp))
                Text("App oficial")
            }
        }

        extraContent()
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
        color = colors.surfaceContainer,
        border = BorderStroke(
            1.dp,
            if (status.isActiveSession) colors.primary else colors.outlineVariant,
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
                                color = colors.tertiaryContainer,
                            ) {
                                Text(
                                    text = "ACTIVA",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = colors.onTertiaryContainer,
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
                        tint = if (isPreferred) colors.tertiary else colors.onSurfaceVariant,
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
