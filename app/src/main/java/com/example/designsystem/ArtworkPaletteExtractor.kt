package com.example.designsystem

import android.graphics.Bitmap
import android.util.LruCache
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.lerp
import androidx.palette.graphics.Palette
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

object ArtworkPaletteExtractor {

    private val cache = LruCache<String, ArtworkPaletteColors>(32)

    fun deriveFromFallbackSeed(
        seed: Color,
        baseBackground: Color = ReferenceBgDark,
    ): ArtworkPaletteColors {
        val darkTint = lerp(baseBackground, seed, 0.36f)
        val vibrant = lerp(seed, Color.White, 0.24f)
        return ArtworkPaletteColors(
            dominant = seed,
            vibrantAccent = vibrant,
            darkTintSurface = darkTint,
            onArtworkText = ReferenceTextPrimaryDark,
        )
    }

    suspend fun extractCached(
        cacheKey: String,
        bitmap: Bitmap?,
        fallbackColor: Color,
    ): ArtworkPaletteColors {
        if (bitmap == null) {
            return deriveFromFallbackSeed(fallbackColor)
        }
        val key = "${cacheKey}_${bitmap.width}x${bitmap.height}_${bitmap.generationId}"
        cache.get(key)?.let { return it }

        val extracted = withContext(Dispatchers.Default) {
            runCatching {
                val palette = Palette.from(bitmap)
                    .maximumColorCount(12)
                    .generate()
                val dominantInt = palette.getDominantColor(android.graphics.Color.rgb(92, 65, 138))
                val vibrantInt = palette.getVibrantColor(
                    palette.getLightVibrantColor(dominantInt),
                )
                val darkMutedInt = palette.getDarkMutedColor(
                    palette.getDarkVibrantColor(dominantInt),
                )
                val dominant = Color(dominantInt)
                val vibrant = lerp(Color(vibrantInt), Color.White, 0.18f)
                val darkSurface = lerp(ReferenceBgDark, Color(darkMutedInt), 0.65f)
                ArtworkPaletteColors(
                    dominant = dominant,
                    vibrantAccent = vibrant,
                    darkTintSurface = darkSurface,
                    onArtworkText = ReferenceTextPrimaryDark,
                )
            }.getOrElse {
                deriveFromFallbackSeed(fallbackColor)
            }
        }
        cache.put(key, extracted)
        return extracted
    }
}
