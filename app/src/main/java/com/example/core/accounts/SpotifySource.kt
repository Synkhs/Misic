package com.example.core.accounts

import android.content.Context
import android.content.Intent
import android.net.Uri
import com.example.BuildConfig
import com.squareup.moshi.Json
import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import net.openid.appauth.AuthorizationException
import net.openid.appauth.AuthorizationRequest
import net.openid.appauth.AuthorizationResponse
import net.openid.appauth.AuthorizationService
import net.openid.appauth.AuthorizationServiceConfiguration
import net.openid.appauth.CodeVerifierUtil
import net.openid.appauth.ResponseTypeValues
import okhttp3.OkHttpClient
import retrofit2.Retrofit
import retrofit2.converter.moshi.MoshiConverterFactory
import retrofit2.http.GET
import retrofit2.http.Header
import retrofit2.http.Query
import kotlin.coroutines.resume

private interface SpotifyWebApi {
    @GET("v1/me")
    suspend fun getCurrentProfile(@Header("Authorization") bearer: String): SpotifyProfileDto

    @GET("v1/me/playlists")
    suspend fun getUserPlaylists(
        @Header("Authorization") bearer: String,
        @Query("limit") limit: Int = 25,
    ): SpotifyPagingDto<SpotifyPlaylistDto>

    @GET("v1/me/tracks")
    suspend fun getSavedTracks(
        @Header("Authorization") bearer: String,
        @Query("limit") limit: Int = 25,
    ): SpotifyPagingDto<SpotifySavedTrackWrapperDto>

    @GET("v1/search")
    suspend fun searchTracks(
        @Header("Authorization") bearer: String,
        @Query("q") query: String,
        @Query("type") type: String = "track",
        @Query("limit") limit: Int = 20,
    ): SpotifySearchResponseDto
}

private data class SpotifyProfileDto(
    val id: String = "",
    @Json(name = "display_name") val displayName: String? = null,
    val product: String? = null,
)

private data class SpotifyImageDto(val url: String = "")

private data class SpotifyExternalUrlsDto(val spotify: String? = null)

private data class SpotifyPagingDto<T>(
    val items: List<T> = emptyList(),
    val total: Int = 0,
)

private data class SpotifyTracksCountDto(val total: Int = 0)

private data class SpotifyPlaylistDto(
    val id: String = "",
    val name: String = "",
    val images: List<SpotifyImageDto> = emptyList(),
    val tracks: SpotifyTracksCountDto? = null,
    @Json(name = "external_urls") val externalUrls: SpotifyExternalUrlsDto? = null,
)

private data class SpotifyArtistDto(val name: String = "")

private data class SpotifyAlbumDto(
    val name: String = "",
    val images: List<SpotifyImageDto> = emptyList(),
)

private data class SpotifyTrackDto(
    val id: String = "",
    val name: String = "",
    val artists: List<SpotifyArtistDto> = emptyList(),
    val album: SpotifyAlbumDto? = null,
    @Json(name = "external_urls") val externalUrls: SpotifyExternalUrlsDto? = null,
)

private data class SpotifySavedTrackWrapperDto(val track: SpotifyTrackDto? = null)

private data class SpotifySearchResponseDto(
    val tracks: SpotifyPagingDto<SpotifyTrackDto>? = null,
)

/**
 * Official Spotify Web API + AppAuth OAuth 2.0 PKCE connector.
 * Never handles passwords, never uses client_secret, and launches playback in official Spotify.
 */
class SpotifySource(
    private val context: Context,
    private val tokenStore: SecureTokenStore,
) : MusicSource {

    override val sourceId: MusicSourceId = MusicSourceId.SPOTIFY

    override val capabilities: SourceCapabilities = SourceCapabilities(
        supportsNativePlayback = false,
        supportsLibraryRead = true,
        supportsPlaylistRead = true,
        supportsRemoteSearch = true,
        supportsOAuthConnect = true,
        requiresOfficialExternalPlayback = true,
    )

    private val clientId: String
        get() = BuildConfig.SPOTIFY_CLIENT_ID.trim()

    private val initialConnectionState: AccountConnectionState =
        if (clientId.isBlank()) {
            AccountConnectionState.NeedsConfig(
                "Configura SPOTIFY_CLIENT_ID en Secrets o Gradle para habilitar OAuth PKCE oficial con Spotify.",
            )
        } else {
            AccountConnectionState.Disconnected
        }

    private val _state = MutableStateFlow(
        SourceAccountSnapshot(
            sourceId = sourceId,
            capabilities = capabilities,
            connectionState = initialConnectionState,
        ),
    )
    override val state: StateFlow<SourceAccountSnapshot> = _state.asStateFlow()

    private val serviceConfig = AuthorizationServiceConfiguration(
        Uri.parse("https://accounts.spotify.com/authorize"),
        Uri.parse("https://accounts.spotify.com/api/token"),
    )

    private val redirectUri: Uri =
        Uri.parse("${BuildConfig.APPLICATION_ID}://oauth2redirect/spotify")

    private val api: SpotifyWebApi by lazy {
        val moshi = Moshi.Builder().addLast(KotlinJsonAdapterFactory()).build()
        Retrofit.Builder()
            .baseUrl("https://api.spotify.com/")
            .client(OkHttpClient.Builder().build())
            .addConverterFactory(MoshiConverterFactory.create(moshi))
            .build()
            .create(SpotifyWebApi::class.java)
    }

    fun isConfigured(): Boolean = clientId.isNotBlank()

    fun createAuthorizationIntent(): Intent? {
        if (clientId.isBlank()) {
            _state.value = _state.value.copy(
                connectionState = AccountConnectionState.NeedsConfig(
                    "Falta SPOTIFY_CLIENT_ID. Decláralo en tu entorno para conectar tu cuenta oficial.",
                ),
            )
            return null
        }
        val verifier = CodeVerifierUtil.generateRandomCodeVerifier()
        val request = AuthorizationRequest.Builder(
            serviceConfig,
            clientId,
            ResponseTypeValues.CODE,
            redirectUri,
        )
            .setCodeVerifier(verifier)
            .setScopes(
                "user-read-private",
                "user-library-read",
                "playlist-read-private",
                "playlist-read-collaborative",
            )
            .build()
        val authService = AuthorizationService(context)
        return runCatching {
            authService.getAuthorizationRequestIntent(request)
        }.also {
            authService.dispose()
        }.getOrNull()
    }

    suspend fun handleAuthorizationResponse(data: Intent?): Boolean {
        if (data == null) {
            _state.value = _state.value.copy(
                connectionState = AccountConnectionState.Error("Autenticación cancelada por el usuario."),
            )
            return false
        }
        val resp = AuthorizationResponse.fromIntent(data)
        val ex = AuthorizationException.fromIntent(data)
        if (resp == null) {
            _state.value = _state.value.copy(
                connectionState = AccountConnectionState.Error(
                    ex?.errorDescription ?: "Spotify rechazó o canceló la autorización.",
                ),
            )
            return false
        }
        _state.value = _state.value.copy(connectionState = AccountConnectionState.Connecting)
        val tokenReq = resp.createTokenExchangeRequest()
        val authService = AuthorizationService(context)
        val exchanged = suspendCancellableCoroutine<StoredOAuthToken?> { cont ->
            authService.performTokenRequest(tokenReq) { tokenResponse, tokenEx ->
                authService.dispose()
                if (tokenResponse?.accessToken != null) {
                    cont.resume(
                        StoredOAuthToken(
                            accessToken = tokenResponse.accessToken.orEmpty(),
                            refreshToken = tokenResponse.refreshToken.orEmpty(),
                            expiresAtMs = tokenResponse.accessTokenExpirationTime ?: (System.currentTimeMillis() + 3600_000L),
                            scope = tokenResponse.scope.orEmpty(),
                        ),
                    )
                } else {
                    _state.value = _state.value.copy(
                        connectionState = AccountConnectionState.Error(
                            tokenEx?.errorDescription ?: "No se pudo canjear el código PKCE.",
                        ),
                    )
                    cont.resume(null)
                }
            }
        } ?: return false

        tokenStore.saveToken(SOURCE_KEY, exchanged)
        syncLibrary()
        return true
    }

    override suspend fun restoreSessionIfValid() {
        if (clientId.isBlank()) {
            _state.value = _state.value.copy(connectionState = initialConnectionState)
            return
        }
        val stored = tokenStore.readToken(SOURCE_KEY)
        if (stored == null) {
            _state.value = _state.value.copy(connectionState = AccountConnectionState.Disconnected)
            return
        }
        if (stored.expiresAtMs > System.currentTimeMillis() + 30_000L) {
            _state.value = _state.value.copy(
                connectionState = AccountConnectionState.Connected(
                    displayName = stored.accountLabel.ifBlank { "Cuenta Spotify" },
                    handleOrPlan = stored.accountHandle.ifBlank { "Sesión restaurada" },
                ),
            )
            syncLibrary()
        } else {
            _state.value = _state.value.copy(
                connectionState = AccountConnectionState.Error("Sesión de Spotify expirada. Vuelve a conectar."),
            )
        }
    }

    override suspend fun syncLibrary() {
        val stored = tokenStore.readToken(SOURCE_KEY) ?: return
        val bearer = "Bearer ${stored.accessToken}"
        _state.value = _state.value.copy(connectionState = AccountConnectionState.Connecting)
        runCatching {
            withContext(Dispatchers.IO) {
                val me = api.getCurrentProfile(bearer)
                val playlistsResp = api.getUserPlaylists(bearer)
                val savedResp = api.getSavedTracks(bearer)
                Triple(me, playlistsResp, savedResp)
            }
        }.onSuccess { (me, playlistsResp, savedResp) ->
            val label = me.displayName?.takeIf { it.isNotBlank() } ?: me.id
            val plan = me.product?.replaceFirstChar { it.uppercase() } ?: "Spotify Web API"
            tokenStore.saveToken(SOURCE_KEY, stored.copy(accountLabel = label, accountHandle = plan))
            val playlists = playlistsResp.items.map { dto ->
                SourcePlaylistSummary(
                    id = dto.id,
                    sourceId = sourceId,
                    title = dto.name,
                    subtitle = "${dto.tracks?.total ?: 0} pistas",
                    trackCount = dto.tracks?.total ?: 0,
                    artworkUrl = dto.images.firstOrNull()?.url,
                    externalUri = dto.externalUrls?.spotify,
                )
            }
            val savedTracks = savedResp.items.mapNotNull { it.track }.map { t ->
                SourceTrackSummary(
                    id = t.id,
                    sourceId = sourceId,
                    title = t.name,
                    artist = t.artists.joinToString(", ") { a -> a.name }.ifBlank { "Spotify" },
                    album = t.album?.name.orEmpty(),
                    artworkUrl = t.album?.images?.firstOrNull()?.url,
                    externalUri = t.externalUrls?.spotify,
                )
            }
            _state.value = _state.value.copy(
                connectionState = AccountConnectionState.Connected(
                    displayName = label,
                    handleOrPlan = plan,
                    playlistCount = playlists.size,
                    savedTrackCount = savedTracks.size,
                ),
                playlists = playlists,
                savedTracks = savedTracks,
            )
        }.onFailure { err ->
            _state.value = _state.value.copy(
                connectionState = AccountConnectionState.Error("Error al sincronizar Spotify: ${err.message ?: "red"}"),
            )
        }
    }

    override suspend fun search(query: String): List<SourceTrackSummary> {
        val clean = query.trim()
        if (clean.isBlank()) return emptyList()
        val stored = tokenStore.readToken(SOURCE_KEY) ?: return emptyList()
        val bearer = "Bearer ${stored.accessToken}"
        val results = runCatching {
            withContext(Dispatchers.IO) {
                api.searchTracks(bearer, clean).tracks?.items.orEmpty().map { t ->
                    SourceTrackSummary(
                        id = t.id,
                        sourceId = sourceId,
                        title = t.name,
                        artist = t.artists.joinToString(", ") { a -> a.name },
                        album = t.album?.name.orEmpty(),
                        artworkUrl = t.album?.images?.firstOrNull()?.url,
                        externalUri = t.externalUrls?.spotify,
                    )
                }
            }
        }.getOrDefault(emptyList())
        _state.value = _state.value.copy(searchResults = results)
        return results
    }

    override suspend fun disconnect() {
        tokenStore.deleteToken(SOURCE_KEY)
        _state.value = SourceAccountSnapshot(
            sourceId = sourceId,
            capabilities = capabilities,
            connectionState = initialConnectionState,
        )
    }

    override fun launchExternalUri(context: Context, uri: String?): Boolean {
        val target = uri?.takeIf { it.isNotBlank() } ?: "https://open.spotify.com/"
        val intent = Intent(Intent.ACTION_VIEW, Uri.parse(target)).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            if (context.packageManager.getLaunchIntentForPackage(sourceId.officialPackageName) != null) {
                setPackage(sourceId.officialPackageName)
            }
        }
        return runCatching {
            context.startActivity(intent)
            true
        }.getOrDefault(false)
    }

    companion object {
        private const val SOURCE_KEY = "spotify_oauth"
    }
}
