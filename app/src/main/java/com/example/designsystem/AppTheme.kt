package com.example.designsystem

import androidx.compose.runtime.Immutable
import androidx.compose.ui.graphics.Color
import com.example.designsystem.expressive.ArtworkShapeStyle
import com.example.designsystem.expressive.GeometryPreset
import com.example.designsystem.expressive.MiniPlayerStyle
import com.example.designsystem.expressive.NavLabelVisibility

// Calibrated to the reference video: dark #121218 surface + #C0C1FF lavender primary + #3A47B5 indigo container
const val DEFAULT_VIOLET_SEED_ARGB: Long = 0xFF5C6BFFL

@Immutable
data class SeedColorSwatch(
    val id: String,
    val label: String,
    val argb: Long,
) {
    val color: Color get() = Color(argb)
}

val DefaultSeedSwatches: List<SeedColorSwatch> = listOf(
    SeedColorSwatch("indigo_lavender", "Lavanda", 0xFF5C6BFFL),
    SeedColorSwatch("violet", "Violeta", 0xFF7B5CFFL),
    SeedColorSwatch("cobalt", "Cobalto", 0xFF3E63DDL),
    SeedColorSwatch("cyan", "Cian", 0xFF00A7C7L),
    SeedColorSwatch("emerald", "Esmeralda", 0xFF1E9E68L),
    SeedColorSwatch("lime", "Lima", 0xFF7CB342L),
    SeedColorSwatch("amber", "Ámbar", 0xFFE69F00L),
    SeedColorSwatch("coral", "Coral", 0xFFE55950L),
    SeedColorSwatch("rose", "Rosa", 0xFFD94A8CL),
    SeedColorSwatch("plum", "Ciruela", 0xFF9C4DCCL),
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
        title = "Material 3 Personalizado",
        subtitle = "Paleta tonal Material Color Utilities",
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
    val schemeVariant: McuSchemeVariant = McuSchemeVariant.TONAL_SPOT,
    val contrastLevel: ContrastPreset = ContrastPreset.STANDARD,
    val pureAmoledBlack: Boolean = false,
    val colorFromArtwork: Boolean = true,
    val reduceAnimations: Boolean = false,
    val useExpressiveM3: Boolean = true,
    val hapticFeedbackEnabled: Boolean = true,
    val geometryPreset: GeometryPreset = GeometryPreset.STANDARD_M3,
    val artworkShape: ArtworkShapeStyle = ArtworkShapeStyle.ROUNDED,
    val navLabelVisibility: NavLabelVisibility = NavLabelVisibility.ALWAYS,
    val miniPlayerStyle: MiniPlayerStyle = MiniPlayerStyle.CARD,
    val libraryGridColumns: Int = 2,
    val lyricsAutoScroll: Boolean = true,
    val lyricsFontScale: Float = 1.0f,
    val roleOverrides: ColorRoleOverrides = ColorRoleOverrides(),
) {
    fun toAppTheme(): AppTheme = AppTheme.fromPreferences(this)

    fun resolveDarkTheme(systemIsDark: Boolean): Boolean = when (themeMode) {
        ThemeMode.SYSTEM -> systemIsDark
        ThemeMode.LIGHT -> false
        ThemeMode.DARK -> true
    }

    fun isTonalSurface(): Boolean = surfaceToneStyle == SurfaceToneStyle.TONAL
}
