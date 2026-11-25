package com.example.core.accounts

import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.credentials.ClearCredentialStateRequest
import androidx.credentials.CredentialManager
import androidx.credentials.CustomCredential
import androidx.credentials.GetCredentialRequest
import com.google.android.libraries.identity.googleid.GetGoogleIdOption
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * Official Google Identity (Credential Manager) + external YouTube Music launch connector.
 * Avoids any private/unofficial scraping or cookie extraction. Replays playlists/tracks natively
 * inside YouTube Music / YouTube via official Intent URIs.
 */
class YouTubeSource(
    private val context: Context,
    private val tokenStore: SecureTokenStore,
) : MusicSource {

    override val sourceId: MusicSourceId = MusicSourceId.YOUTUBE_MUSIC

    override val capabilities: SourceCapabilities = SourceCapabilities(
        supportsNativePlayback = false,
        supportsLibraryRead = false,
        supportsPlaylistRead = true,
        supportsRemoteSearch = false,
        supportsOAuthConnect = true,
        requiresOfficialExternalPlayback = true,
    )

    private val _state = MutableStateFlow(
        SourceAccountSnapshot(
            sourceId = sourceId,
            capabilities = capabilities,
            connectionState = AccountConnectionState.Disconnected,
        ),
    )
    override val state: StateFlow<SourceAccountSnapshot> = _state.asStateFlow()

    override suspend fun restoreSessionIfValid() {
        val stored = tokenStore.readToken(SOURCE_KEY) ?: return
        _state.value = _state.value.copy(
            connectionState = AccountConnectionState.Connected(
                displayName = stored.accountLabel.ifBlank { "Cuenta Google" },
                handleOrPlan = stored.accountHandle.ifBlank { "Credential Manager • Apertura externa en YT Music" },
            ),
        )
    }

    suspend fun signInWithGoogle(activityContext: Context, serverClientId: String = ""): Boolean {
        if (serverClientId.isBlank()) {
            _state.value = _state.value.copy(
                connectionState = AccountConnectionState.NeedsConfig(
                    "El acceso Google para YouTube Data API requiere OAuth Web Client ID de tu proyecto Google Cloud. Control vía MediaSession ya está disponible.",
                ),
            )
            return false
        }
        _state.value = _state.value.copy(connectionState = AccountConnectionState.Connecting)
        return runCatching {
            val credentialManager = CredentialManager.create(activityContext)
            val googleIdOption = GetGoogleIdOption.Builder()
                .setFilterByAuthorizedAccounts(false)
                .setServerClientId(serverClientId)
                .setAutoSelectEnabled(false)
                .build()
            val request = GetCredentialRequest.Builder()
                .addCredentialOption(googleIdOption)
                .build()
            val response = credentialManager.getCredential(activityContext, request)
            val credential = response.credential
            if (credential is CustomCredential &&
                credential.type == GoogleIdTokenCredential.TYPE_GOOGLE_ID_TOKEN_CREDENTIAL
            ) {
                val googleCred = GoogleIdTokenCredential.createFrom(credential.data)
                tokenStore.saveToken(
                    SOURCE_KEY,
                    StoredOAuthToken(
                        accessToken = googleCred.idToken,
                        accountLabel = googleCred.displayName ?: googleCred.id,
                        accountHandle = googleCred.id,
                    ),
                )
                _state.value = _state.value.copy(
                    connectionState = AccountConnectionState.Connected(
                        displayName = googleCred.displayName ?: googleCred.id,
                        handleOrPlan = "Google Sign-In • YouTube Music externo",
                    ),
                )
                true
            } else {
                _state.value = _state.value.copy(
                    connectionState = AccountConnectionState.Error("Credencial Google no compatible."),
                )
                false
            }
        }.getOrElse { err ->
            _state.value = _state.value.copy(
                connectionState = AccountConnectionState.Error(
                    err.message ?: "No se completó el inicio de sesión Google.",
                ),
            )
            false
        }
    }

    override suspend fun syncLibrary() {
        restoreSessionIfValid()
    }

    override suspend fun search(query: String): List<SourceTrackSummary> = emptyList()

    override suspend fun disconnect() {
        tokenStore.deleteToken(SOURCE_KEY)
        runCatching {
            CredentialManager.create(context).clearCredentialState(ClearCredentialStateRequest())
        }
        _state.value = SourceAccountSnapshot(
            sourceId = sourceId,
            capabilities = capabilities,
            connectionState = AccountConnectionState.Disconnected,
        )
    }

    override fun launchExternalUri(context: Context, uri: String?): Boolean {
        val target = uri?.takeIf { it.isNotBlank() } ?: "https://music.youtube.com/"
        val pkg = if (context.packageManager.getLaunchIntentForPackage(sourceId.officialPackageName) != null) {
            sourceId.officialPackageName
        } else {
            "com.google.android.youtube"
        }
        val intent = Intent(Intent.ACTION_VIEW, Uri.parse(target)).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            if (context.packageManager.getLaunchIntentForPackage(pkg) != null) {
                setPackage(pkg)
            }
        }
        return runCatching {
            context.startActivity(intent)
            true
        }.getOrDefault(false)
    }

    companion object {
        private const val SOURCE_KEY = "youtube_google_id"
    }
}
