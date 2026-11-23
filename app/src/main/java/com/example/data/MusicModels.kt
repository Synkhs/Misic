package com.example.data

import android.graphics.Bitmap
import androidx.compose.ui.graphics.Color

enum class MusicServiceApp(
    val id: String,
    val displayName: String,
    val packageName: String,
    val accentColor: Color,
    val subtitle: String,
) {
    SPOTIFY(
        id = "spotify",
        displayName = "Spotify",
        packageName = "com.spotify.music",
        accentColor = Color(0xFF1DB954),
        subtitle = "Streaming & Podcasts",
    ),
    YOUTUBE_MUSIC(
        id = "yt_music",
        displayName = "YouTube Music",
        packageName = "com.google.android.apps.youtube.music",
        accentColor = Color(0xFFFF3D57),
        subtitle = "Catálogo YouTube & Mixes",
    ),
    AMAZON_MUSIC(
        id = "amazon_music",
        displayName = "Amazon Music",
        packageName = "com.amazon.mp3",
        accentColor = Color(0xFF00A8E1),
        subtitle = "Ultra HD & Spatial Audio",
    ),
    APPLE_MUSIC(
        id = "apple_music",
        displayName = "Apple Music",
        packageName = "com.apple.android.music",
        accentColor = Color(0xFFFA243C),
        subtitle = "Lossless & Dolby Atmos",
    ),
    DEEZER(
        id = "deezer",
        displayName = "Deezer",
        packageName = "deezer.android.app",
        accentColor = Color(0xFFA238FF),
        subtitle = "HiFi FLAC Streaming",
    ),
    TIDAL(
        id = "tidal",
        displayName = "TIDAL",
        packageName = "com.aspiro.tidal",
        accentColor = Color(0xFF00FFFF),
        subtitle = "Max FLAC 24-bit",
    ),
    HONOR_MUSIC(
        id = "honor_music",
        displayName = "HONOR Música",
        packageName = "com.hihonor.cloudmusic",
        accentColor = Color(0xFF00D2FF),
        subtitle = "Reproductor nativo MagicOS",
    ),
    LOCAL_ENGINE(
        id = "local_engine",
        displayName = "Puente MagicOS",
        packageName = "com.aistudio.magicmusic.hnm10x",
        accentColor = Color(0xFF00D2FF),
        subtitle = "Audio Hi-Res de prueba local",
    ),
    OTHER(
        id = "other",
        displayName = "Multimedia Activa",
        packageName = "",
        accentColor = Color(0xFF38BDF8),
        subtitle = "Cualquier sesión MediaSession",
    );

    companion object {
        val supportedThirdParty: List<MusicServiceApp> = listOf(
            SPOTIFY,
            YOUTUBE_MUSIC,
            AMAZON_MUSIC,
            APPLE_MUSIC,
            DEEZER,
            TIDAL,
            HONOR_MUSIC,
        )

        fun fromPackageName(pkg: String?): MusicServiceApp {
            if (pkg.isNullOrBlank()) return OTHER
            return entries.firstOrNull { it.packageName == pkg } ?: when {
                pkg.contains("spotify", ignoreCase = true) -> SPOTIFY
                pkg.contains("youtube.music", ignoreCase = true) -> YOUTUBE_MUSIC
                pkg.contains("amazon", ignoreCase = true) -> AMAZON_MUSIC
                pkg.contains("apple", ignoreCase = true) -> APPLE_MUSIC
                pkg.contains("deezer", ignoreCase = true) -> DEEZER
                pkg.contains("tidal", ignoreCase = true) -> TIDAL
                pkg.contains("honor", ignoreCase = true) || pkg.contains("mediacenter", ignoreCase = true) -> HONOR_MUSIC
                else -> OTHER
            }
        }
    }
}

enum class WidgetStyle(
    val title: String,
    val subtitle: String,
) {
    CAPSULE_DARK("Cápsula Viva", "Estilo isla dinámica MagicOS 10"),
    FROSTED_GLASS("Cristal Ahumado", "Vidrio esmerilado oscuro Honor"),
    PORCELAIN_LIGHT("Porcelana Clara", "Superficie luminosa diurna"),
    DYNAMIC_NEON("Neón Zafiro", "Borde iridiscente Honor Cyan"),
}

enum class CustomControlAction(
    val label: String,
    val shortLabel: String,
) {
    REWIND_15("Retroceder 15s", "-15s"),
    FORWARD_15("Avanzar 15s", "+15s"),
    SWITCH_SOURCE("Cambiar App", "Fuente"),
    OPEN_PLAYER("Abrir Reproductor", "Abrir"),
    MUTE_AUDIO("Silenciar Audio", "Silencio"),
}

enum class QsTileMode(
    val label: String,
    val description: String,
) {
    PLAY_PAUSE("Reproducir / Pausar", "Alterna la reproducción del servicio activo"),
    OPEN_DECK("Panel Pantalla Bloqueo", "Abre controles completos sobre el bloqueo"),
    SWITCH_SOURCE("Rotar Servicio", "Cambia entre Spotify, YT Music y Amazon"),
}

data class WidgetConfig(
    val style: WidgetStyle = WidgetStyle.FROSTED_GLASS,
    val cornerRadiusDp: Int = 26,
    val glassOpacity: Float = 0.90f,
    val showProgressBar: Boolean = true,
    val showServiceBadge: Boolean = true,
    val showMagicCapsuleHeader: Boolean = true,
    val leftCustomAction: CustomControlAction = CustomControlAction.REWIND_15,
    val rightCustomAction: CustomControlAction = CustomControlAction.FORWARD_15,
)

data class LockAndQsConfig(
    val bridgeEnabled: Boolean = true,
    val showOnLockScreen: Boolean = true,
    val lockScreenAction1: CustomControlAction = CustomControlAction.REWIND_15,
    val lockScreenAction2: CustomControlAction = CustomControlAction.FORWARD_15,
    val qsTileMode: QsTileMode = QsTileMode.PLAY_PAUSE,
    val preferHighContrastCapsule: Boolean = true,
)

data class ActiveSessionInfo(
    val packageName: String,
    val service: MusicServiceApp,
    val title: String,
    val artist: String,
    val isPlaying: Boolean,
    val isSelected: Boolean,
)

data class InstalledServiceStatus(
    val service: MusicServiceApp,
    val isInstalled: Boolean,
    val isActiveSession: Boolean,
)

data class UnifiedMediaState(
    val title: String = "Sin música activa",
    val artist: String = "Abre Spotify, YT Music o Amazon Music",
    val album: String = "MagicOS 10 Audio Hub",
    val packageName: String = "",
    val service: MusicServiceApp = MusicServiceApp.OTHER,
    val isPlaying: Boolean = false,
    val positionMs: Long = 0L,
    val durationMs: Long = 0L,
    val albumArt: Bitmap? = null,
    val isLocalEngine: Boolean = false,
    val activeSessions: List<ActiveSessionInfo> = emptyList(),
    val lastUpdatedMs: Long = 0L,
) {
    val hasActiveMedia: Boolean
        get() = packageName.isNotBlank() || isLocalEngine

    val progressFraction: Float
        get() = if (durationMs > 0L) (positionMs.toFloat() / durationMs.toFloat()).coerceIn(0f, 1f) else 0f
}
