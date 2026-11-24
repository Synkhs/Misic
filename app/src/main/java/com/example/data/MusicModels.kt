package com.example.data

import android.graphics.Bitmap
import android.graphics.drawable.Drawable
import androidx.compose.runtime.Immutable
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
        subtitle = "Streaming y listas",
    ),
    YOUTUBE_MUSIC(
        id = "yt_music",
        displayName = "YouTube Music",
        packageName = "com.google.android.apps.youtube.music",
        accentColor = Color(0xFFFF3D57),
        subtitle = "Catálogo YouTube y radios",
    ),
    AMAZON_MUSIC(
        id = "amazon_music",
        displayName = "Amazon Music",
        packageName = "com.amazon.mp3",
        accentColor = Color(0xFF00A8E1),
        subtitle = "Ultra HD y Audio Espacial",
    ),
    APPLE_MUSIC(
        id = "apple_music",
        displayName = "Apple Music",
        packageName = "com.apple.android.music",
        accentColor = Color(0xFFFA243C),
        subtitle = "Lossless y Dolby Atmos",
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
        accentColor = Color(0xFF00E5FF),
        subtitle = "Max FLAC 24-bit",
    ),
    SOUNDCLOUD(
        id = "soundcloud",
        displayName = "SoundCloud",
        packageName = "com.soundcloud.android",
        accentColor = Color(0xFFFF5500),
        subtitle = "Audio independiente",
    ),
    LOCAL_ENGINE(
        id = "local_engine",
        displayName = "Motor Local Hi-Res",
        packageName = "com.aistudio.magicmusic.hnm10x",
        accentColor = Color(0xFFB38CFF),
        subtitle = "Reproductor local integrado",
    ),
    OTHER(
        id = "other",
        displayName = "Reproductor Activo",
        packageName = "",
        accentColor = Color(0xFFCDA9FF),
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
            SOUNDCLOUD,
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
                pkg.contains("soundcloud", ignoreCase = true) -> SOUNDCLOUD
                else -> OTHER
            }
        }
    }
}

enum class SessionPriorityRule(
    val label: String,
    val description: String,
) {
    PLAYING_FIRST("Reproduciendo primero", "Prioriza la app que está sonando actualmente"),
    LAST_ACTIVE("Última activa", "Mantiene la última sesión con actividad reciente"),
    PINNED_PREFERRED("App preferida", "Prioriza tu servicio fijado cuando esté abierto"),
}

enum class WidgetFormat(
    val title: String,
    val gridLabel: String,
    val description: String,
) {
    CARD_4X2("Tarjeta Control", "4×2", "Carátula, insignia, barra de progreso y 5 controles"),
    CAPSULE_4X1("Cápsula", "4×1", "Barra horizontal compacta con carátula y controles base"),
    SQUARE_2X2("Cuadrado", "2×2", "Carátula prominente con barra de progreso y pausa"),
    MINI_2X1("Mini", "2×1", "Diseño mínimo adaptable 2×1 o 1×1 con pausa rápida"),
}

enum class WidgetStyle(
    val title: String,
    val subtitle: String,
) {
    TONAL("Tonal", "Colores tonales del tema Material 3"),
    SURFACE("Superficie", "Contenedor oscuro neutro con contraste nítido"),
    ARTWORK("Carátula", "Colores extraídos dinámicamente de la pista"),
    TRANSPARENT("Transparente", "Superficie translúcida que deja ver tu fondo"),
}

enum class CustomControlAction(
    val label: String,
    val shortLabel: String,
) {
    REWIND_15("Retroceder 15 s", "-15 s"),
    FORWARD_15("Avanzar 15 s", "+15 s"),
    SWITCH_SOURCE("Cambiar app", "Fuente"),
    OPEN_PLAYER("Abrir reproductor", "Abrir"),
    MUTE_AUDIO("Silenciar", "Silencio"),
    LIKE_TRACK("Me gusta", "Me gusta"),
    SHUFFLE("Aleatorio", "Aleatorio"),
    REPEAT("Repetir", "Repetir"),
}

enum class QsTileMode(
    val label: String,
    val description: String,
) {
    PLAY_PAUSE("Reproducir / Pausar", "Alterna la reproducción del servicio activo"),
    OPEN_DECK("Panel Pantalla Bloqueo", "Abre controles completos sobre el bloqueo"),
    SWITCH_SOURCE("Rotar Servicio", "Cambia entre sesiones activas disponibles"),
}

enum class LockPrivacyLevel(
    val label: String,
    val description: String,
) {
    PUBLIC("Pública", "Muestra carátula, título, artista y controles sobre el bloqueo"),
    HIDE_SENSITIVE("Privada (ocultar título y carátula)", "Oculta portada e información de la pista mientras el móvil está bloqueado"),
    SECRET("Secreta (no mostrar en bloqueo)", "Mantiene el reproductor oculto en la pantalla de bloqueo"),
}

enum class QuickTileType(
    val key: String,
    val label: String,
    val supportsToggle: Boolean,
) {
    PLAY_PAUSE("play_pause", "Música", true),
    NEXT("next", "Siguiente", false),
    PREVIOUS("previous", "Anterior", false),
    SHUFFLE("shuffle", "Aleatorio", true),
    REPEAT("repeat", "Repetir", true),
    FAVORITE("favorite", "Me gusta", true),
    SLEEP_TIMER("sleep_timer", "Temporizador", true),
}

enum class AudioOutputCategory(
    val label: String,
) {
    SPEAKER("Altavoz"),
    WIRED_HEADSET("Cable"),
    BLUETOOTH("Bluetooth"),
    USB_DAC("USB DAC"),
}

@Immutable
data class DeviceEqProfile(
    val category: AudioOutputCategory = AudioOutputCategory.SPEAKER,
    val enabled: Boolean = true,
    val presetIndex: Int = 0,
    val bandGainsDb: List<Float> = listOf(0f, 0f, 0f, 0f, 0f),
    val bassBoostStrength: Int = 0,
    val loudnessGainMb: Int = 0,
)

@Immutable
data class WidgetConfig(
    val style: WidgetStyle = WidgetStyle.TONAL,
    val cornerRadiusDp: Int = 24,
    val glassOpacity: Float = 0.94f,
    val showProgressBar: Boolean = true,
    val showServiceBadge: Boolean = true,
    val showCompactHeader: Boolean = true,
    val leftCustomAction: CustomControlAction = CustomControlAction.REWIND_15,
    val rightCustomAction: CustomControlAction = CustomControlAction.FORWARD_15,
)

@Immutable
data class WidgetTemplate(
    val id: String,
    val name: String,
    val format: WidgetFormat = WidgetFormat.CARD_4X2,
    val config: WidgetConfig = WidgetConfig(),
)

@Immutable
data class LockAndQsConfig(
    val bridgeEnabled: Boolean = true,
    val showOnLockScreen: Boolean = true,
    val showArtworkOnLockScreen: Boolean = true,
    val lockPrivacyLevel: LockPrivacyLevel = LockPrivacyLevel.PUBLIC,
    val lockAutoCloseSeconds: Int = 30,
    val compactActionIndices: List<Int> = listOf(1, 2, 3),
    val notifyChannelGuideShown: Boolean = false,
    val requireUnlockToOpenApp: Boolean = true,
    val requireUnlockToSwitchSource: Boolean = false,
    val requireUnlockForCustomActions: Boolean = false,
    val lockScreenAction1: CustomControlAction = CustomControlAction.REWIND_15,
    val lockScreenAction2: CustomControlAction = CustomControlAction.FORWARD_15,
    val qsTileMode: QsTileMode = QsTileMode.PLAY_PAUSE,
    val preferHighContrastDeck: Boolean = true,
    val qsAddedTileKeys: Set<String> = emptySet(),
)

@Immutable
data class MediaSessionPreferences(
    val priorityRule: SessionPriorityRule = SessionPriorityRule.PLAYING_FIRST,
    val preferredServicePackage: String = MusicServiceApp.SPOTIFY.packageName,
    val ignoreVideoSessions: Boolean = false,
    val disabledPackages: Set<String> = emptySet(),
)

@Immutable
data class EqualizerConfig(
    val enabled: Boolean = true,
    val presetIndex: Int = 0,
    val bandGainsDb: List<Float> = listOf(0f, 0f, 0f, 0f, 0f),
    val isAdvancedMode: Boolean = false,
    val hiResBufferHigh: Boolean = true,
    val bassBoostStrength: Int = 0,
    val loudnessGainMb: Int = 0,
    val gaplessEnabled: Boolean = true,
    val crossfadeSeconds: Int = 0,
    val replayGainEnabled: Boolean = true,
    val float24BitOutput: Boolean = true,
    val activeOutputCategory: AudioOutputCategory = AudioOutputCategory.SPEAKER,
    val deviceProfiles: Map<AudioOutputCategory, DeviceEqProfile> = emptyMap(),
)

@Immutable
data class ActiveSessionInfo(
    val packageName: String,
    val service: MusicServiceApp,
    val title: String,
    val artist: String,
    val isPlaying: Boolean,
    val isSelected: Boolean,
    val appLabel: String = service.displayName,
)

data class InstalledServiceStatus(
    val service: MusicServiceApp,
    val isInstalled: Boolean,
    val isActiveSession: Boolean,
    val packageName: String = service.packageName,
    val realAppName: String = service.displayName,
    val appIcon: Drawable? = null,
    val isManagedEnabled: Boolean = true,
)

@Immutable
data class QueueTrackItem(
    val queueId: Long,
    val title: String,
    val artist: String,
    val durationMs: Long = 0L,
    val isCurrent: Boolean = false,
)

@Immutable
data class LyricsLine(
    val timestampMs: Long,
    val text: String,
)

@Immutable
data class LyricsData(
    val lines: List<LyricsLine> = emptyList(),
    val isSynced: Boolean = false,
    val sourceNote: String = "",
)

@Immutable
data class TrackHistoryEntry(
    val id: String,
    val title: String,
    val artist: String,
    val album: String,
    val packageName: String,
    val serviceId: String,
    val lastPlayedMs: Long,
    val playCount: Int = 1,
    val durationMs: Long = 0L,
) {
    val service: MusicServiceApp
        get() = MusicServiceApp.entries.firstOrNull { it.id == serviceId }
            ?: MusicServiceApp.fromPackageName(packageName)
}

@Immutable
data class LocalListeningStats(
    val totalMinutes: Int = 0,
    val dailyMinutes: List<Int> = listOf(0, 0, 0, 0, 0, 0, 0),
    val recentTracks: List<TrackHistoryEntry> = emptyList(),
    val favoriteTrackIds: Set<String> = emptySet(),
    val playlistTrackIds: Set<String> = emptySet(),
)

@Immutable
data class AudioOutputDevice(
    val id: String,
    val name: String,
    val typeLabel: String,
    val isActive: Boolean,
    val isBluetooth: Boolean,
    val routeId: String? = null,
)

data class UnifiedMediaState(
    val title: String = "Sin música activa",
    val artist: String = "Abre tu reproductor de música favorito",
    val album: String = "MagicMusic",
    val packageName: String = "",
    val service: MusicServiceApp = MusicServiceApp.OTHER,
    val isPlaying: Boolean = false,
    val positionMs: Long = 0L,
    val durationMs: Long = 0L,
    val albumArt: Bitmap? = null,
    val isLocalEngine: Boolean = false,
    val activeSessions: List<ActiveSessionInfo> = emptyList(),
    val lastUpdatedMs: Long = 0L,
    val canPlayPause: Boolean = true,
    val canSkipNext: Boolean = true,
    val canSkipPrevious: Boolean = true,
    val canSeek: Boolean = true,
    val canFavorite: Boolean = false,
    val isFavorite: Boolean = false,
    val canShuffle: Boolean = false,
    val isShuffleEnabled: Boolean = false,
    val canRepeat: Boolean = false,
    val isRepeatEnabled: Boolean = false,
    val queue: List<QueueTrackItem> = emptyList(),
    val lyrics: LyricsData? = null,
) {
    val hasActiveMedia: Boolean
        get() = packageName.isNotBlank() || isLocalEngine

    val progressFraction: Float
        get() = if (durationMs > 0L) (positionMs.toFloat() / durationMs.toFloat()).coerceIn(0f, 1f) else 0f

    val trackKey: String
        get() = "${packageName}|${title}|${artist}".lowercase()
}
