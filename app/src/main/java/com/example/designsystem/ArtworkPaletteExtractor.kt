package com.example.designsystem

import android.graphics.Bitmap
import android.util.LruCache
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.graphics.toArgb
import androidx.palette.graphics.Palette
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

object ArtworkPaletteExtractor {

    private val cache = LruCache<String, ArtworkPaletteColors>(64)

    fun deriveFromFallbackSeed(
        seed: Color,
        baseBackground: Color = ReferenceBgDark,
    ): ArtworkPaletteColors {
        val darkTint = lerp(baseBackground, seed, 0.24f)
        val vibrant = lerp(seed, Color.White, 0.20f)
        val onText = MaterialColorUtilitiesEngine.bestInkFor(darkTint, 4.5)
        return ArtworkPaletteColors(
            dominant = seed,
            vibrantAccent = vibrant,
            darkTintSurface = darkTint,
            onArtworkText = onText,
        )
    }

    suspend fun extractCached(
        cacheKey: String,
        bitmap: Bitmap?,
        fallbackColor: Color,
    ): ArtworkPaletteColors {
        if (bitmap == null || bitmap.isRecycled) {
            return deriveFromFallbackSeed(fallbackColor)
        }
        val key = "${cacheKey}_${bitmap.width}x${bitmap.height}_${bitmap.generationId}"
        cache.get(key)?.let { return it }

        val extracted = withContext(Dispatchers.Default) {
            runCatching {
                val sample = if (bitmap.width > 96 || bitmap.height > 96) {
                    Bitmap.createScaledBitmap(bitmap, 72, 72, true)
                } else {
                    bitmap
                }
                val palette = Palette.from(sample)
                    .maximumColorCount(12)
                    .generate()
                val dominantInt = palette.getDominantColor(fallbackColor.toArgb())
                val vibrantInt = palette.getVibrantColor(
                    palette.getLightVibrantColor(dominantInt),
                )
                val darkMutedInt = palette.getDarkMutedColor(
                    palette.getDarkVibrantColor(dominantInt),
                )
                val dominant = Color(dominantInt)
                val vibrant = lerp(Color(vibrantInt), Color.White, 0.16f)
                val darkSurface = lerp(ReferenceBgDark, Color(darkMutedInt), 0.55f)
                val safeText = MaterialColorUtilitiesEngine.bestInkFor(darkSurface, 4.5)
                ArtworkPaletteColors(
                    dominant = dominant,
                    vibrantAccent = vibrant,
                    darkTintSurface = darkSurface,
                    onArtworkText = safeText,
                )
            }.getOrElse {
                deriveFromFallbackSeed(fallbackColor)
            }
        }
        cache.put(key, extracted)
        return extracted
    }
}
