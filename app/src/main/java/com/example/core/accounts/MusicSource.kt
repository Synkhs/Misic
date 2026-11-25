package com.example.core.accounts

import android.content.Context
import android.content.Intent
import androidx.compose.runtime.Immutable
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

enum class MusicSourceId(
    val id: String,
    val displayName: String,
    val officialPackageName: String,
    val ownershipDisclaimer: String,
) {
    LOCAL(
        id = "local",
        displayName = "Archivos Locales",
        officialPackageName = "com.example",
        ownershipDisclaimer = "Reproducción local directa mediante MediaStore y ExoPlayer Hi-Res.",
    ),
    SESSION(
        id = "session",
        displayName = "Sesión Activa",
        officialPackageName = "",
        ownershipDisclaimer = "Control estándar Android MediaSession sin acceder a credenciales.",
    ),
    SPOTIFY(
        id = "spotify",
        displayName = "Spotify",
        officialPackageName = "com.spotify.music",
        ownershipDisclaimer = "Conexión oficial mediante OAuth PKCE y Spotify Web API. MagicMusic no está afiliado ni patrocinado por Spotify AB. La reproducción completa se abre en la app oficial de Spotify.",
    ),
    APPLE_MUSIC(
        id = "apple_music",
        displayName = "Apple Music",
        officialPackageName = "com.apple.android.music",
        ownershipDisclaimer = "Integración de estado vía MediaSession y apertura en Apple Music para Android. Requiere MusicKit SDK y clave de desarrollador para catálogo completo.",
    ),
    YOUTUBE_MUSIC(
        id = "youtube_music",
        displayName = "YouTube Music",
        officialPackageName = "com.google.android.apps.youtube.music",
        ownershipDisclaimer = "Inicio oficial con cuenta Google (Credential Manager) + YouTube Data API v3. La reproducción multimedia se abre en YouTube Music o YouTube.",
    ),
}

@Immutable
data class SourceCapabilities(
    val supportsNativePlayback: Boolean = false,
    val supportsLibraryRead: Boolean = false,
    val supportsPlaylistRead: Boolean = false,
    val supportsRemoteSearch: Boolean = false,
    val supportsOAuthConnect: Boolean = false,
    val requiresOfficialExternalPlayback: Boolean = true,
)

@Immutable
data class SourcePlaylistSummary(
    val id: String,
    val sourceId: MusicSourceId,
    val title: String,
    val subtitle: String,
    val trackCount: Int,
    val artworkUrl: String? = null,
    val externalUri: String? = null,
)

@Immutable
data class SourceTrackSummary(
    val id: String,
    val sourceId: MusicSourceId,
    val title: String,
    val artist: String,
    val album: String,
    val artworkUrl: String? = null,
    val externalUri: String? = null,
)

sealed class AccountConnectionState {
    data object Disconnected : AccountConnectionState()
    data class NeedsConfig(val message: String) : AccountConnectionState()
    data object Connecting : AccountConnectionState()
    data class Connected(
        val displayName: String,
        val handleOrPlan: String,
        val playlistCount: Int = 0,
        val savedTrackCount: Int = 0,
        val lastSyncedAtMs: Long = System.currentTimeMillis(),
    ) : AccountConnectionState()

    data class FeatureDisabled(val reason: String) : AccountConnectionState()
    data class Error(val message: String) : AccountConnectionState()
}

@Immutable
data class SourceAccountSnapshot(
    val sourceId: MusicSourceId,
    val capabilities: SourceCapabilities,
    val connectionState: AccountConnectionState = AccountConnectionState.Disconnected,
    val playlists: List<SourcePlaylistSummary> = emptyList(),
    val savedTracks: List<SourceTrackSummary> = emptyList(),
    val searchResults: List<SourceTrackSummary> = emptyList(),
)

interface MusicSource {
    val sourceId: MusicSourceId
    val capabilities: SourceCapabilities
    val state: StateFlow<SourceAccountSnapshot>

    suspend fun restoreSessionIfValid()
    suspend fun syncLibrary()
    suspend fun search(query: String): List<SourceTrackSummary>
    suspend fun disconnect()
    fun launchExternalUri(context: Context, uri: String?): Boolean
}

class MusicAccountsCoordinator(context: Context) {
    private val appCtx = context.applicationContext
    private val tokenStore = SecureTokenStore(appCtx)

    val spotify = SpotifySource(appCtx, tokenStore)
    val appleMusic = AppleMusicSource(appCtx)
    val youtube = YouTubeSource(appCtx, tokenStore)

    private val _lastAccountMessage = MutableStateFlow<String?>(null)
    val lastAccountMessage: StateFlow<String?> = _lastAccountMessage.asStateFlow()

    suspend fun restoreAll() {
        spotify.restoreSessionIfValid()
        appleMusic.restoreSessionIfValid()
        youtube.restoreSessionIfValid()
    }

    fun buildSpotifyAuthIntent(): Intent? {
        val intent = spotify.createAuthorizationIntent()
        if (intent == null) {
            _lastAccountMessage.value = "Configura SPOTIFY_CLIENT_ID para iniciar OAuth PKCE oficial."
        }
        return intent
    }

    suspend fun handleSpotifyAuthResult(data: Intent?): Boolean {
        val ok = spotify.handleAuthorizationResponse(data)
        _lastAccountMessage.value = if (ok) {
            "Spotify conectado de forma segura"
        } else {
            (spotify.state.value.connectionState as? AccountConnectionState.Error)?.message
                ?: "No se completó el acceso con Spotify"
        }
        return ok
    }

    suspend fun connectYouTubeWithGoogle(activityContext: Context): Boolean {
        val ok = youtube.signInWithGoogle(activityContext)
        _lastAccountMessage.value = if (ok) {
            "Cuenta Google vinculada para YouTube Music"
        } else {
            (youtube.state.value.connectionState as? AccountConnectionState.Error)?.message
                ?: "No se vinculó la cuenta Google"
        }
        return ok
    }

    suspend fun syncSource(sourceId: MusicSourceId) {
        when (sourceId) {
            MusicSourceId.SPOTIFY -> spotify.syncLibrary()
            MusicSourceId.YOUTUBE_MUSIC -> youtube.syncLibrary()
            MusicSourceId.APPLE_MUSIC -> appleMusic.syncLibrary()
            else -> Unit
        }
    }

    suspend fun disconnectSource(sourceId: MusicSourceId) {
        when (sourceId) {
            MusicSourceId.SPOTIFY -> {
                spotify.disconnect()
                _lastAccountMessage.value = "Spotify desconectado y tokens cifrados eliminados"
            }
            MusicSourceId.YOUTUBE_MUSIC -> {
                youtube.disconnect()
                _lastAccountMessage.value = "YouTube desconectado y sesión local eliminada"
            }
            MusicSourceId.APPLE_MUSIC -> {
                appleMusic.disconnect()
                _lastAccountMessage.value = "Apple Music desvinculado"
            }
            else -> Unit
        }
    }

    fun clearMessage() {
        _lastAccountMessage.value = null
    }
}
