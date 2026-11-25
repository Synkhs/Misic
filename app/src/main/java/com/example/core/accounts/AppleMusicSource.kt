package com.example.core.accounts

import android.content.Context
import android.content.Intent
import android.net.Uri
import com.example.BuildConfig
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * Apple Music source behind feature flag ENABLE_APPLE_MUSIC.
 * Apple Music on Android requires MusicKit Android SDK + Developer Token signed by an Apple Developer team.
 * Without a MusicKit Developer Token, MagicMusic exposes honest MediaSession control and official app deep-links.
 */
class AppleMusicSource(
    private val context: Context,
) : MusicSource {

    override val sourceId: MusicSourceId = MusicSourceId.APPLE_MUSIC

    override val capabilities: SourceCapabilities = SourceCapabilities(
        supportsNativePlayback = false,
        supportsLibraryRead = false,
        supportsPlaylistRead = false,
        supportsRemoteSearch = false,
        supportsOAuthConnect = BuildConfig.ENABLE_APPLE_MUSIC,
        requiresOfficialExternalPlayback = true,
    )

    private val _state = MutableStateFlow(
        SourceAccountSnapshot(
            sourceId = sourceId,
            capabilities = capabilities,
            connectionState = computeState(),
        ),
    )
    override val state: StateFlow<SourceAccountSnapshot> = _state.asStateFlow()

    private fun computeState(): AccountConnectionState {
        if (!BuildConfig.ENABLE_APPLE_MUSIC) {
            return AccountConnectionState.FeatureDisabled(
                "Desactivado por configuración de compilación (ENABLE_APPLE_MUSIC = false).",
            )
        }
        val installed = context.packageManager.getLaunchIntentForPackage(sourceId.officialPackageName) != null
        return if (installed) {
            AccountConnectionState.NeedsConfig(
                "Apple Music instalada (control por MediaSession activo). El catálogo en la nube requiere MusicKit Android SDK + Developer Token.",
            )
        } else {
            AccountConnectionState.NeedsConfig(
                "Instala Apple Music para Android o configura MusicKit Developer Token para sincronizar catálogo.",
            )
        }
    }

    override suspend fun restoreSessionIfValid() {
        _state.value = _state.value.copy(connectionState = computeState())
    }

    override suspend fun syncLibrary() {
        _state.value = _state.value.copy(connectionState = computeState())
    }

    override suspend fun search(query: String): List<SourceTrackSummary> = emptyList()

    override suspend fun disconnect() {
        _state.value = SourceAccountSnapshot(
            sourceId = sourceId,
            capabilities = capabilities,
            connectionState = computeState(),
        )
    }

    override fun launchExternalUri(context: Context, uri: String?): Boolean {
        val target = uri?.takeIf { it.isNotBlank() } ?: "https://music.apple.com/"
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
}
