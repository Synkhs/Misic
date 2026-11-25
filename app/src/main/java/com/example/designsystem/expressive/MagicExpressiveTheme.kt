package com.example.designsystem.expressive

import androidx.compose.material3.ColorScheme
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.MaterialExpressiveTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.MotionScheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.Typography
import androidx.compose.runtime.Composable

/**
 * Isolation wrapper around MaterialExpressiveTheme (material3 1.5.0).
 * When [useExpressiveM3] is disabled, falls back cleanly to standard M3 MaterialTheme.
 */
@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun MagicExpressiveRootTheme(
    colorScheme: ColorScheme,
    shapes: Shapes,
    typography: Typography,
    useExpressiveM3: Boolean,
    reduceAnimations: Boolean,
    content: @Composable () -> Unit,
) {
    if (useExpressiveM3) {
        val motion = if (reduceAnimations) {
            MotionScheme.standard()
        } else {
            MotionScheme.expressive()
        }
        MaterialExpressiveTheme(
            colorScheme = colorScheme,
            motionScheme = motion,
            shapes = shapes,
            typography = typography,
            content = content,
        )
    } else {
        MaterialTheme(
            colorScheme = colorScheme,
            shapes = shapes,
            typography = typography,
            content = content,
        )
    }
}
