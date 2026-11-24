package com.example.designsystem

import androidx.compose.runtime.Immutable
import androidx.compose.ui.graphics.Color

const val DEFAULT_VIOLET_SEED_ARGB: Long = 0xFFB38CFFL

@Immutable
data class SeedColorSwatch(
    val id: String,
    val label: String,
    val argb: Long,
) {
    val color: Color get() = Color(argb)
}

val DefaultSeedSwatches: List<SeedColorSwatch> = listOf(
    SeedColorSwatch("violet", "Violeta", 0xFFB38CFFL),
    SeedColorSwatch("lavender", "Lavanda", 0xFFD0BCFFL),
    SeedColorSwatch("indigo", "Índigo", 0xFF7B9CFFL),
    SeedColorSwatch("blue", "Azul", 0xFF67B7FFL),
    SeedColorSwatch("cyan", "Cian", 0xFF4FD8EBL),
    SeedColorSwatch("mint", "Menta", 0xFF64FFDAL),
    SeedColorSwatch("emerald", "Esmeralda", 0xFF5CE0A8L),
    SeedColorSwatch("lime", "Lima", 0xFFAEE571L),
    SeedColorSwatch("amber", "Ámbar", 0xFFFFD54FL),
    SeedColorSwatch("coral", "Coral", 0xFFFF8A80L),
    SeedColorSwatch("rose", "Rosa", 0xFFFF8FB1L),
    SeedColorSwatch("plum", "Ciruela", 0xFFE086FFL),
)

enum class AppThemeId(
    val title: String,
    val subtitle: String,
) {
    MATERIAL_3_DYNAMIC(
        title = "Material 3 Dinámico",
        subtitle = "Extrae colores del fondo de pantalla en Android 12+",
    ),
    MATERIAL_3_STATIC(
        title = "Material 3 Violeta",
        subtitle = "Paleta tonal violeta y lila calibrada",
    ),
}

enum class ThemeMode(val label: String) {
    DARK("Oscuro"),
    SYSTEM("Sistema"),
    LIGHT("Claro"),
}

enum class SurfaceToneStyle(val label: String) {
    DARK("Oscuro"),
    TONAL("Tonal"),
    LIGHT("Claro"),
}

@Immutable
sealed interface AppTheme {
    val id: AppThemeId

    @Immutable
    data object Material3Dynamic : AppTheme {
        override val id: AppThemeId = AppThemeId.MATERIAL_3_DYNAMIC
    }

    @Immutable
    data class Material3Static(
        val seedColorArgb: Long = DEFAULT_VIOLET_SEED_ARGB,
    ) : AppTheme {
        override val id: AppThemeId = AppThemeId.MATERIAL_3_STATIC
    }

    companion object {
        fun fromPreferences(prefs: ThemePreferences): AppTheme = if (prefs.useDynamicColor) {
            Material3Dynamic
        } else {
            Material3Static(prefs.seedColorArgb)
        }
    }
}

@Immutable
data class ThemePreferences(
    val useDynamicColor: Boolean = false,
    val themeMode: ThemeMode = ThemeMode.DARK,
    val surfaceToneStyle: SurfaceToneStyle = SurfaceToneStyle.TONAL,
    val seedColorArgb: Long = DEFAULT_VIOLET_SEED_ARGB,
    val pureAmoledBlack: Boolean = false,
    val colorFromArtwork: Boolean = true,
    val reduceAnimations: Boolean = false,
) {
    fun toAppTheme(): AppTheme = AppTheme.fromPreferences(this)

    fun resolveDarkTheme(systemIsDark: Boolean): Boolean = when (themeMode) {
        ThemeMode.SYSTEM -> systemIsDark
        ThemeMode.LIGHT -> false
        ThemeMode.DARK -> true
    }

    fun isTonalSurface(): Boolean = surfaceToneStyle == SurfaceToneStyle.TONAL
}
