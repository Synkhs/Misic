package com.example.ui.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext

private val MagicDarkColorScheme = darkColorScheme(
    primary = HonorCyan,
    onPrimary = MagicObsidian,
    primaryContainer = Color(0xFF0A3254),
    onPrimaryContainer = Color(0xFFD6F5FF),
    secondary = HonorCobalt,
    onSecondary = Color.White,
    secondaryContainer = MagicSurfaceElevated,
    onSecondaryContainer = Color(0xFFDBEAFE),
    tertiary = HonorElectricBlue,
    onTertiary = MagicObsidian,
    background = MagicObsidian,
    onBackground = MagicTextLight,
    surface = MagicDeepNavy,
    onSurface = MagicTextLight,
    surfaceVariant = MagicSurfaceCard,
    onSurfaceVariant = MagicMutedLight,
    outline = MagicGlassStroke,
    outlineVariant = Color(0x1FFFFFFF),
    error = HonorCoral,
)

private val MagicLightColorScheme = lightColorScheme(
    primary = HonorCobalt,
    onPrimary = Color.White,
    primaryContainer = Color(0xFFDCEBFF),
    onPrimaryContainer = Color(0xFF0A2458),
    secondary = HonorCyan,
    onSecondary = MagicObsidian,
    secondaryContainer = MagicPorcelainContainer,
    onSecondaryContainer = MagicTextDark,
    tertiary = HonorElectricBlue,
    background = MagicPorcelain,
    onBackground = MagicTextDark,
    surface = MagicPorcelainCard,
    onSurface = MagicTextDark,
    surfaceVariant = MagicPorcelainContainer,
    onSurfaceVariant = MagicMutedDark,
    outline = Color(0x260F172A),
    outlineVariant = Color(0x140F172A),
    error = HonorCoral,
)

@Composable
fun MyApplicationTheme(
    darkTheme: Boolean = true,
    dynamicColor: Boolean = false,
    content: @Composable () -> Unit,
) {
    val systemDark = isSystemInDarkTheme()
    val useDark = darkTheme || systemDark
    val colorScheme = when {
        dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            val context = LocalContext.current
            if (useDark) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        }
        useDark -> MagicDarkColorScheme
        else -> MagicLightColorScheme
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content,
    )
}
