package com.example.designsystem.expressive

import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.MaterialShapes
import androidx.compose.material3.Shapes
import androidx.compose.material3.toShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

enum class GeometryPreset(
    val label: String,
    val description: String,
    val cardRadiusDp: Int,
    val listItemRadiusDp: Int,
    val artworkMediumDp: Int,
    val artworkLargeDp: Int,
    val sheetRadiusDp: Int,
) {
    STANDARD_M3(
        label = "Estándar M3",
        description = "Tokens Material 3 equilibrados (24–28 dp)",
        cardRadiusDp = 24,
        listItemRadiusDp = 16,
        artworkMediumDp = 20,
        artworkLargeDp = 28,
        sheetRadiusDp = 28,
    ),
    COMPACT(
        label = "Compacto",
        description = "Bordes precisos y mayor densidad en listas",
        cardRadiusDp = 16,
        listItemRadiusDp = 12,
        artworkMediumDp = 14,
        artworkLargeDp = 20,
        sheetRadiusDp = 24,
    ),
    SOFT_EXPRESSIVE(
        label = "Suave Expressive",
        description = "Superficies amplias y contornos orgánicos",
        cardRadiusDp = 28,
        listItemRadiusDp = 20,
        artworkMediumDp = 24,
        artworkLargeDp = 32,
        sheetRadiusDp = 32,
    ),
}

enum class ArtworkShapeStyle(
    val label: String,
    val isExpressivePolygon: Boolean,
) {
    ROUNDED("Redondeada", false),
    SOFT("Suave", false),
    SQUARE("Cuadrada", false),
    COOKIE("Galleta", true),
    CLOVER("Trébol", true),
    PENTAGON("Pentágono", true),
    PILL("Píldora", true),
}

@Immutable
data class MagicShapeTokens(
    val geometryPreset: GeometryPreset = GeometryPreset.STANDARD_M3,
    val artworkShapeStyle: ArtworkShapeStyle = ArtworkShapeStyle.ROUNDED,
    val useExpressiveShapes: Boolean = true,
)

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun resolveArtworkShape(
    style: ArtworkShapeStyle,
    useExpressiveM3: Boolean,
    fallbackCornerRadius: Dp = 28.dp,
): Shape {
    if (!useExpressiveM3 || !style.isExpressivePolygon) {
        return when (style) {
            ArtworkShapeStyle.SQUARE -> RoundedCornerShape(12.dp)
            ArtworkShapeStyle.SOFT -> RoundedCornerShape((fallbackCornerRadius.value * 1.15f).coerceAtMost(36f).dp)
            ArtworkShapeStyle.COOKIE,
            ArtworkShapeStyle.CLOVER,
            ArtworkShapeStyle.PENTAGON,
            ArtworkShapeStyle.ROUNDED,
            -> RoundedCornerShape(fallbackCornerRadius)
            ArtworkShapeStyle.PILL -> CircleShape
        }
    }
    return when (style) {
        ArtworkShapeStyle.COOKIE -> MaterialShapes.Cookie9Sided.toShape()
        ArtworkShapeStyle.CLOVER -> MaterialShapes.Clover4Leaf.toShape()
        ArtworkShapeStyle.PENTAGON -> MaterialShapes.Pentagon.toShape()
        ArtworkShapeStyle.PILL -> MaterialShapes.Pill.toShape()
        ArtworkShapeStyle.SOFT -> MaterialShapes.SoftBurst.toShape()
        ArtworkShapeStyle.SQUARE -> RoundedCornerShape(12.dp)
        ArtworkShapeStyle.ROUNDED -> RoundedCornerShape(fallbackCornerRadius)
    }
}

fun buildShapesForPreset(preset: GeometryPreset): Shapes = Shapes(
    extraSmall = RoundedCornerShape(8.dp),
    small = RoundedCornerShape(preset.listItemRadiusDp.dp),
    medium = RoundedCornerShape(preset.artworkMediumDp.dp),
    large = RoundedCornerShape(preset.cardRadiusDp.dp),
    extraLarge = RoundedCornerShape(preset.artworkLargeDp.dp),
)
