package com.example.core.media

import androidx.compose.runtime.Immutable

enum class HealthLight {
    GREEN,
    AMBER,
    RED,
}

enum class HealthCheckId {
    NOTIFICATION_LISTENER,
    POST_NOTIFICATIONS,
    BATTERY_OPTIMIZATION,
    WIDGET_PINNED,
    QUICK_TILE_ADDED,
}

@Immutable
data class HealthItemStatus(
    val id: HealthCheckId,
    val title: String,
    val subtitle: String,
    val light: HealthLight,
    val actionLabel: String,
)

@Immutable
data class SystemHealthSummary(
    val items: List<HealthItemStatus> = emptyList(),
    val greenCount: Int = 0,
    val amberCount: Int = 0,
    val redCount: Int = 0,
    val overallLight: HealthLight = HealthLight.GREEN,
) {
    val hasPendingIssues: Boolean
        get() = redCount > 0 || amberCount > 0

    val pendingCount: Int
        get() = amberCount + redCount
}

object SystemHealthCalculator {

    fun evaluate(
        hasListenerPermission: Boolean,
        listenerConnected: Boolean,
        hasPostNotificationsPermission: Boolean,
        isIgnoringBatteryOptimizations: Boolean,
        hasPinnedWidget: Boolean,
        hasQuickTileAdded: Boolean,
    ): SystemHealthSummary {
        val listenerItem = when {
            !hasListenerPermission -> HealthItemStatus(
                id = HealthCheckId.NOTIFICATION_LISTENER,
                title = "Acceso a notificaciones",
                subtitle = "Necesario para detectar Spotify, YouTube Music y sesiones activas",
                light = HealthLight.RED,
                actionLabel = "Arreglar",
            )
            !listenerConnected -> HealthItemStatus(
                id = HealthCheckId.NOTIFICATION_LISTENER,
                title = "Acceso a notificaciones",
                subtitle = "Permiso concedido • Reconectando puente multimedia",
                light = HealthLight.AMBER,
                actionLabel = "Arreglar",
            )
            else -> HealthItemStatus(
                id = HealthCheckId.NOTIFICATION_LISTENER,
                title = "Acceso a notificaciones",
                subtitle = "Activo y sincronizando sesiones en tiempo real",
                light = HealthLight.GREEN,
                actionLabel = "Listo",
            )
        }

        val postNotifItem = if (hasPostNotificationsPermission) {
            HealthItemStatus(
                id = HealthCheckId.POST_NOTIFICATIONS,
                title = "Notificaciones de control",
                subtitle = "Permitido para mostrar reproductor en pantalla de bloqueo",
                light = HealthLight.GREEN,
                actionLabel = "Listo",
            )
        } else {
            HealthItemStatus(
                id = HealthCheckId.POST_NOTIFICATIONS,
                title = "Notificaciones de control",
                subtitle = "Concede notificaciones para usar controles en pantalla de bloqueo",
                light = HealthLight.RED,
                actionLabel = "Arreglar",
            )
        }

        val batteryItem = if (isIgnoringBatteryOptimizations) {
            HealthItemStatus(
                id = HealthCheckId.BATTERY_OPTIMIZATION,
                title = "Batería sin restricciones",
                subtitle = "El sistema no cerrará tus widgets ni el puente en segundo plano",
                light = HealthLight.GREEN,
                actionLabel = "Listo",
            )
        } else {
            HealthItemStatus(
                id = HealthCheckId.BATTERY_OPTIMIZATION,
                title = "Batería sin restricciones",
                subtitle = "Recomendado desactivar ahorro de energía para evitar pausas en el widget",
                light = HealthLight.AMBER,
                actionLabel = "Arreglar",
            )
        }

        val widgetItem = if (hasPinnedWidget) {
            HealthItemStatus(
                id = HealthCheckId.WIDGET_PINNED,
                title = "Widget en escritorio",
                subtitle = "Widget activo añadido a tu pantalla de inicio",
                light = HealthLight.GREEN,
                actionLabel = "Listo",
            )
        } else {
            HealthItemStatus(
                id = HealthCheckId.WIDGET_PINNED,
                title = "Widget en escritorio",
                subtitle = "Aún no has añadido ningún widget de MagicMusic al escritorio",
                light = HealthLight.AMBER,
                actionLabel = "Arreglar",
            )
        }

        val tileItem = if (hasQuickTileAdded) {
            HealthItemStatus(
                id = HealthCheckId.QUICK_TILE_ADDED,
                title = "Mosaico en Ajustes Rápidos",
                subtitle = "Acceso rápido musical disponible al deslizar la barra superior",
                light = HealthLight.GREEN,
                actionLabel = "Listo",
            )
        } else {
            HealthItemStatus(
                id = HealthCheckId.QUICK_TILE_ADDED,
                title = "Mosaico en Ajustes Rápidos",
                subtitle = "Añade el mosaico Música para controlar reproducción con un toque",
                light = HealthLight.AMBER,
                actionLabel = "Arreglar",
            )
        }

        val items = listOf(listenerItem, postNotifItem, batteryItem, widgetItem, tileItem)
        val greens = items.count { it.light == HealthLight.GREEN }
        val ambers = items.count { it.light == HealthLight.AMBER }
        val reds = items.count { it.light == HealthLight.RED }
        val overall = when {
            reds > 0 -> HealthLight.RED
            ambers > 0 -> HealthLight.AMBER
            else -> HealthLight.GREEN
        }
        return SystemHealthSummary(
            items = items,
            greenCount = greens,
            amberCount = ambers,
            redCount = reds,
            overallLight = overall,
        )
    }
}
