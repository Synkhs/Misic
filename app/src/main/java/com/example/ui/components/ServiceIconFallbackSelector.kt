package com.example.ui.components

import androidx.annotation.DrawableRes
import com.example.R
import com.example.data.MusicServiceApp

enum class ServiceBadgeFallbackKind {
    PACKAGE_DRAWABLE,
    APPLE_MUSIC_PNG,
    INITIAL_CIRCLE,
}

data class ServiceBadgeVisualSpec(
    val kind: ServiceBadgeFallbackKind,
    @DrawableRes val appleMusicDrawableRes: Int = 0,
    val contentDescription: String = "",
    val initialLetter: String = "M",
)

object ServiceIconFallbackSelector {

    fun select(
        hasPackageDrawable: Boolean,
        service: MusicServiceApp?,
        serviceName: String,
        isDarkTheme: Boolean,
        sizeDp: Float,
    ): ServiceBadgeVisualSpec {
        val displayTitle = serviceName.ifBlank { service?.displayName.orEmpty() }
        if (hasPackageDrawable) {
            return ServiceBadgeVisualSpec(
                kind = ServiceBadgeFallbackKind.PACKAGE_DRAWABLE,
                contentDescription = displayTitle.ifBlank { null } ?: "Reproductor",
            )
        }

        if (service == MusicServiceApp.APPLE_MUSIC) {
            // Never upscale 192px above 24.dp: use 192px only for <= 24dp, otherwise 512px
            val useCompact192 = sizeDp <= 24f
            val resId = when {
                isDarkTheme && useCompact192 -> R.drawable.ic_service_apple_music_white_192
                isDarkTheme -> R.drawable.ic_service_apple_music_white_512
                useCompact192 -> R.drawable.ic_service_apple_music_color_192
                else -> R.drawable.ic_service_apple_music_color_512
            }
            return ServiceBadgeVisualSpec(
                kind = ServiceBadgeFallbackKind.APPLE_MUSIC_PNG,
                appleMusicDrawableRes = resId,
                contentDescription = "Apple Music",
            )
        }

        val initial = displayTitle.trim()
            .firstOrNull { it.isLetterOrDigit() }
            ?.uppercaseChar()
            ?.toString()
            ?: "M"

        return ServiceBadgeVisualSpec(
            kind = ServiceBadgeFallbackKind.INITIAL_CIRCLE,
            contentDescription = displayTitle.ifBlank { "Reproductor" },
            initialLetter = initial,
        )
    }
}
