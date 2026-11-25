package com.example.data

import android.graphics.Bitmap
import android.graphics.drawable.Drawable
import androidx.compose.runtime.Immutable
import androidx.compose.ui.graphics.Color
import com.example.designsystem.ColorRoleOverrides
import com.example.designsystem.ContrastPreset
import com.example.designsystem.DEFAULT_VIOLET_SEED_ARGB
import com.example.designsystem.McuSchemeVariant
import com.example.designsystem.ThemeMode

enum class MusicServiceApp(
    val id: String,
    val displayName: String,
    val subtitle: String,
    val packageName: String,
    val knownPackages: List<String>,
    val accentColor: Color,
    val storeUrl: String = "",
) {
    SPOTIFY(
        id = "spotify",
        displayName = "Spotify",
        subtitle = "Streaming y biblioteca oficial",
        packageName = "com.spotify.music",
        knownPackages = listOf("com.spotify.music", "com.spotify.lite"),
        accentColor = Color(0xFF1DB954),
        storeUrl = "https://play.google.com/store/apps/details?id=com.spotify.music",
    ),
    YOUTUBE_MUSIC(
        id = "youtube_music",
        displayName = "YouTube Music",
        subtitle = "Streaming de YouTube Music y listas",
        packageName = "com.google.android.apps.youtube.music",
        knownPackages = listOf("com.google.android.apps.youtube.music"),
        accentColor = Color(0xFFFF3D57),
        storeUrl = "https://play.google.com/store/apps/details?id=com.google.android.apps.youtube.music",
    ),
    AMAZON_MUSIC(
        id = "amazon_music",
        displayName = "Amazon Music",
        subtitle = "Control universal MediaSession",
        packageName = "com.amazon.mp3",
        knownPackages = listOf("com.amazon.mp3"),
        accentColor = Color(0xFF25D1DA),
        storeUrl = "https://play.google.com/store/apps/details?id=com.amazon.mp3",
    ),
    APPLE_MUSIC(
        id = "apple_music",
        displayName = "Apple Music",
        subtitle = "Control multimedia en Android",
        packageName = "com.apple.android.music",
        knownPackages = listOf("com.apple.android.music"),
        accentColor = Color(0xFFFA4A66),
        storeUrl = "https://play.google.com/store/apps/details?id=com.apple.android.music",
    ),
    TIDAL(
        id = "tidal",
        displayName = "TIDAL",
        subtitle = "Audio sin pérdida y MediaSession",
        packageName = "com.aspiro.tidal",
        knownPackages = listOf("com.aspiro.tidal"),
        accentColor = Color(0xFF00D7D7),
        storeUrl = "https://play.google.com/store/apps/details?id=com.aspiro.tidal",
    ),
    DEEZER(
        id = "deezer",
        displayName = "Deezer",
        subtitle = "Streaming de alta fidelidad",
        packageName = "deezer.android.app",
        knownPackages = listOf("deezer.android.app"),
        accentColor = Color(0xFFA238FF),
        storeUrl = "https://play.google.com/store/apps/details?id=deezer.android.app",
    ),
    SOUNDCLOUD(
        id = "soundcloud",
        displayName = "SoundCloud",
        subtitle = "Pistas independientes y sesiones",
        packageName = "com.soundcloud.android",
        knownPackages = listOf("com.soundcloud.android"),
        accentColor = Color(0xFFFF6A00),
        storeUrl = "https://play.google.com/store/apps/details?id=com.soundcloud.android",
    ),
    YOUTUBE(
        id = "youtube",
        displayName = "YouTube",
        subtitle = "Sesión multimedia de vídeo",
        packageName = "com.google.android.youtube",
        knownPackages = listOf("com.google.android.youtube"),
        accentColor = Color(0xFFFF4444),
        storeUrl = "https://play.google.com/store/apps/details?id=com.google.android.youtube",
    ),
    LOCAL_ENGINE(
        id = "local_engine",
        displayName = "Motor Local Hi-Res",
        subtitle = "Motor nativo Media3 ExoPlayer de MagicMusic",
        packageName = "com.example",
        knownPackages = listOf("com.example", "com.aistudio.magicmusic.hnm10x"),
        accentColor = Color(0xFFC0C1FF),
    ),
    OTHER(
        id = "other",
        displayName = "Reproductor del sistema",
        subtitle = "Control MediaSession compatible",
        packageName = "",
        knownPackages = emptyList(),
        accentColor = Color(0xFF9EA6FF),
    );

    companion object {
        val supportedThirdParty: List<MusicServiceApp> = listOf(
            SPOTIFY,
            YOUTUBE_MUSIC,
            AMAZON_MUSIC,
            APPLE_MUSIC,
            TIDAL,
            DEEZER,
            SOUNDCLOUD,
        )

        fun fromPackageName(pkg: String?): MusicServiceApp {
            if (pkg.isNullOrBlank()) return OTHER
            return entries.firstOrNull { app ->
                app.knownPackages.any { it.equals(pkg, ignoreCase = true) } ||
                    app.packageName.equals(pkg, ignoreCase = true)
            } ?: OTHER
        }

        fun fromId(id: String?): MusicServiceApp {
            if (id.isNullOrBlank()) return OTHER
            return entries.firstOrNull { it.id.equals(id, ignoreCase = true) } ?: OTHER
        }
    }
}

enum class WidgetFormat(
    val title: String,
    val gridLabel: String,
    val description: String,
) {
    CARD_4X2("Tarjeta Completa", "4×2", "Carátula grande, controles configurables y barra de progreso"),
    CAPSULE_4X1("Cápsula Compacta", "4×1", "Cabecera horizontal compacta con reproducción instantánea"),
    SQUARE_2X2("Cuadrado Portada", "2×2", "Portada protagonista con título y botón play/pausa"),
    MINI_2X1("Mini Rápido", "2×1 / 1×1", "Botón compacto adaptable para huecos pequeños"),
}

enum class WidgetStyle(
    val title: String,
    val subtitle: String,
) {
    SURFACE("Superficie Material 3", "Contenedor tonal sólido surfaceContainerHigh"),
    TONAL("Tonal Vibrante", "Acentos primaryContainer del color semilla o dinámico"),
    ARTWORK("Carátula Adaptable", "Colores extraídos del álbum con texto de alto contraste"),
    TRANSPARENT("Contorno Ligero", "Contenedor translúcido limpio sin gradientes"),
}

enum class WidgetColorSource(val label: String) {
    FOLLOW_APP("Seguir color de la app"),
    SYSTEM_DYNAMIC("Color del sistema (Material You)"),
    ARTWORK("Tono de la carátula"),
    CUSTOM_SEED("Color personalizado del widget"),
}

enum class CustomControlAction(
    val label: String,
    val shortLabel: String,
) {
    REWIND_15("Retroceder 15s", "−15 s"),
    FORWARD_15("Avanzar 15s", "+15 s"),
    SWITCH_SOURCE("Cambiar reproductor", "Cambiar app"),
    OPEN_PLAYER("Abrir reproductor", "Abrir app"),
    MUTE_AUDIO("Silenciar audio", "Silenciar"),
    LIKE_TRACK("Me gusta", "Me gusta"),
    SHUFFLE("Aleatorio", "Aleatorio"),
    REPEAT("Repetir", "Repetir"),
}

@Immutable
data class ActiveSessionInfo(
    val packageName: String,
    val service: MusicServiceApp,
    val title: String,
    val artist: String,
    val album: String = "",
    val isPlaying: Boolean,
    val isSelected: Boolean,
    val appLabel: String = service.displayName,
    val isLocalEngine: Boolean = false,
)

@Immutable
data class LyricsLine(
    val timestampMs: Long,
    val text: String,
)

@Immutable
data class LyricsData(
    val lines: List<LyricsLine>,
    val isSynced: Boolean = true,
    val sourceNote: String = "",
    val trackKey: String = "",
)

@Immutable
data class QueueTrackItem(
    val queueId: Long,
    val title: String,
    val artist: String,
    val durationMs: Long,
    val isCurrent: Boolean,
)

@Immutable
data class UnifiedMediaState(
    val title: String = "Sin reproducción activa",
    val artist: String = "Toca para abrir o inicia el Motor Local",
    val album: String = "MagicMusic",
    val packageName: String = "",
    val service: MusicServiceApp = MusicServiceApp.LOCAL_ENGINE,
    val isPlaying: Boolean = false,
    val positionMs: Long = 0L,
    val durationMs: Long = 0L,
    val albumArt: Bitmap? = null,
    val albumArtUri: String? = null,
    val isLocalEngine: Boolean = false,
    val activeSessions: List<ActiveSessionInfo> = emptyList(),
    val lastUpdatedMs: Long = System.currentTimeMillis(),
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
    val isMuted: Boolean = false,
    val streamFormatBadge: String = "Hi-Res Local",
    val queue: List<QueueTrackItem> = emptyList(),
    val lyrics: LyricsData? = null,
    val localTrackId: Long? = null,
) {
    val hasActiveMedia: Boolean
        get() = activeSessions.isNotEmpty() || isLocalEngine || (title.isNotBlank() && title != "Sin reproducción activa")

    val trackKey: String
        get() = "${packageName}|${title.trim().lowercase()}|${artist.trim().lowercase()}|${album.trim().lowercase()}"

    val progressFraction: Float
        get() = if (durationMs > 0L) (positionMs.toFloat() / durationMs.toFloat()).coerceIn(0f, 1f) else 0f
}

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
data class WidgetInstanceConfig(
    val appWidgetId: Int,
    val format: WidgetFormat = WidgetFormat.CARD_4X2,
    val style: WidgetStyle = WidgetStyle.SURFACE,
    val colorSource: WidgetColorSource = WidgetColorSource.FOLLOW_APP,
    val seedColorArgb: Long = DEFAULT_VIOLET_SEED_ARGB,
    val schemeVariant: McuSchemeVariant = McuSchemeVariant.TONAL_SPOT,
    val themeMode: ThemeMode = ThemeMode.SYSTEM,
    val contrastLevel: ContrastPreset = ContrastPreset.STANDARD,
    val roleOverrides: ColorRoleOverrides = ColorRoleOverrides(),
    val cornerRadiusDp: Int = 24,
    val containerOpacity: Float = 0.94f,
    val showProgressBar: Boolean = true,
    val showServiceBadge: Boolean = true,
    val showCompactHeader: Boolean = true,
    val leftCustomAction: CustomControlAction = CustomControlAction.REWIND_15,
    val rightCustomAction: CustomControlAction = CustomControlAction.FORWARD_15,
) {
    fun toWidgetConfig(): WidgetConfig = WidgetConfig(
        style = style,
        cornerRadiusDp = cornerRadiusDp,
        glassOpacity = containerOpacity,
        showProgressBar = showProgressBar,
        showServiceBadge = showServiceBadge,
        showCompactHeader = showCompactHeader,
        leftCustomAction = leftCustomAction,
        rightCustomAction = rightCustomAction,
    )
}

@Immutable
data class WidgetTemplate(
    val id: String,
    val name: String,
    val format: WidgetFormat,
    val config: WidgetConfig,
    val createdAtMs: Long = System.currentTimeMillis(),
)

enum class SessionPriorityRule(
    val label: String,
    val description: String,
) {
    PLAYING_FIRST("Última en reproducción", "Prioriza la app que está sonando activamente"),
    LAST_ACTIVE("Más reciente", "Prioriza la sesión multimedia con interacción más reciente"),
    PINNED_PREFERRED("App preferida fijada", "Da prioridad a tu reproductor favorito cuando esté abierto"),
}

@Immutable
data class MediaSessionPreferences(
    val priorityRule: SessionPriorityRule = SessionPriorityRule.PLAYING_FIRST,
    val ignoreVideoSessions: Boolean = false,
    val preferredServicePackage: String = MusicServiceApp.SPOTIFY.packageName,
    val disabledPackages: Set<String> = emptySet(),
)

enum class QsTileMode(
    val label: String,
    val description: String,
) {
    PLAY_PAUSE("Reproducir / Pausar", "Alterna reproducción de la sesión activa"),
    SWITCH_SOURCE("Cambiar reproductor", "Abre selector de sesión activa o rota fuente"),
    OPEN_DECK("Abrir panel bloqueado", "Abre el deck de control en pantalla completa"),
}

enum class QuickTileType(
    val key: String,
    val label: String,
    val subtitle: String,
) {
    PLAY_PAUSE("play_pause", "Música", "Control principal de reproducción"),
    NEXT("next", "Siguiente", "Saltar a la siguiente pista"),
    PREVIOUS("previous", "Anterior", "Volver a la pista anterior"),
    SHUFFLE("shuffle", "Aleatorio", "Alternar modo aleatorio"),
    REPEAT("repeat", "Repetir", "Alternar repetición de lista o pista"),
    FAVORITE("favorite", "Me gusta", "Marcar canción como favorita"),
    SLEEP_TIMER("sleep_timer", "Temporizador", "Temporizador rápido 15m / 30m / Apagar"),
}

enum class LockPrivacyLevel(
    val label: String,
    val description: String,
) {
    PUBLIC("Completa", "Muestra carátula, título, artista y controles en bloqueo"),
    HIDE_SENSITIVE("Ocultar carátula y título", "Muestra controles esenciales sin datos de la pista"),
    SECRET("Ocultar bloqueado", "Bloquea MagicMusic hasta desbloquear el teléfono"),
}

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
data class InstalledServiceStatus(
    val service: MusicServiceApp,
    val packageName: String,
    val realAppName: String,
    val isInstalled: Boolean,
    val isActiveSession: Boolean,
    val isManagedEnabled: Boolean,
    val appIcon: Drawable? = null,
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
        get() = MusicServiceApp.fromId(serviceId)
}

@Immutable
data class LocalListeningStats(
    val totalMinutes: Int = 0,
    val dailyMinutes: List<Int> = listOf(0, 0, 0, 0, 0, 0, 0),
    val recentTracks: List<TrackHistoryEntry> = emptyList(),
    val favoriteTrackIds: Set<String> = emptySet(),
    val playlistTrackIds: Set<String> = emptySet(),
)

enum class AudioOutputCategory(val label: String) {
    SPEAKER("Altavoz"),
    WIRED("Cable / USB-C"),
    BLUETOOTH("Bluetooth"),
}

@Immutable
data class DeviceEqProfile(
    val category: AudioOutputCategory,
    val enabled: Boolean = true,
    val presetIndex: Int = 0,
    val bandGainsDb: List<Float> = listOf(0f, 0f, 0f, 0f, 0f),
    val bassBoostStrength: Int = 0,
    val virtualizerStrength: Int = 0,
    val loudnessGainMb: Int = 0,
    val preampGainDb: Float = 0f,
    val stereoBalance: Float = 0f,
)

@Immutable
data class CustomEqPreset(
    val id: String,
    val name: String,
    val bandGainsDb: List<Float>,
    val bassBoostStrength: Int = 0,
    val virtualizerStrength: Int = 0,
    val loudnessGainMb: Int = 0,
    val preampGainDb: Float = 0f,
)

@Immutable
data class EqualizerConfig(
    val enabled: Boolean = true,
    val presetIndex: Int = 0,
    val customPresetName: String = "",
    val bandGainsDb: List<Float> = listOf(0f, 0f, 0f, 0f, 0f),
    val isAdvancedMode: Boolean = false,
    val hiResBufferHigh: Boolean = true,
    val bassBoostStrength: Int = 0, // 0..1000
    val virtualizerStrength: Int = 0, // 0..1000
    val loudnessGainMb: Int = 0, // 0..1000
    val preampGainDb: Float = 0f, // -6f..+6f
    val stereoBalance: Float = 0f, // -1f..+1f
    val gaplessEnabled: Boolean = true,
    val replayGainEnabled: Boolean = true,
    val float24BitOutput: Boolean = true,
    val crossfadeSeconds: Int = 0, // 0..12
    val activeOutputCategory: AudioOutputCategory = AudioOutputCategory.SPEAKER,
    val deviceProfiles: Map<AudioOutputCategory, DeviceEqProfile> = emptyMap(),
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

enum class LibrarySourceFilter(val label: String) {
    ALL("Todo"),
    LOCAL("Local"),
    SPOTIFY("Spotify"),
    YOUTUBE("YouTube Music"),
    APPLE_MUSIC("Apple Music"),
}

enum class LibrarySortOption(val label: String) {
    TITLE("Título A–Z"),
    ARTIST("Artista"),
    RECENT("Recientes"),
    MOST_PLAYED("Más escuchadas"),
    DURATION("Duración"),
}
