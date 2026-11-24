package com.example.core.media

import androidx.compose.runtime.Immutable

enum class DiagnosticCategory {
    MEDIA_SESSION,
    WIDGETS,
    LOCK_SCREEN,
    QUICK_SETTINGS,
    HIRES_OUTPUT,
}

@Immutable
data class DiagnosticCheckItem(
    val category: DiagnosticCategory,
    val title: String,
    val detail: String,
    val passed: Boolean,
)

@Immutable
data class HiResDiagnosticsReport(
    val items: List<DiagnosticCheckItem> = emptyList(),
) {
    val passedCount: Int get() = items.count { it.passed }
    val totalCount: Int get() = items.size
}

object HiResDiagnosticsCalculator {

    fun evaluate(
        hasListenerPermission: Boolean,
        listenerConnected: Boolean,
        hasPinnedWidget: Boolean,
        hasPostNotifications: Boolean,
        bridgeOrLockReady: Boolean,
        hasQuickTileAdded: Boolean,
        hardwareReport: HiResHardwareReport,
    ): HiResDiagnosticsReport {
        val items = listOf(
            DiagnosticCheckItem(
                category = DiagnosticCategory.MEDIA_SESSION,
                title = "MediaSession",
                detail = when {
                    hasListenerPermission && listenerConnected -> "Conectado y leyendo reproductores externos"
                    hasListenerPermission -> "Permiso concedido • Esperando enlace del sistema"
                    else -> "Falta permiso de acceso a notificaciones"
                },
                passed = hasListenerPermission && listenerConnected,
            ),
            DiagnosticCheckItem(
                category = DiagnosticCategory.WIDGETS,
                title = "Widgets",
                detail = if (hasPinnedWidget) {
                    "Al menos un widget activo en el escritorio"
                } else {
                    "Ningún widget fijado todavía en el escritorio"
                },
                passed = hasPinnedWidget,
            ),
            DiagnosticCheckItem(
                category = DiagnosticCategory.LOCK_SCREEN,
                title = "Pantalla de bloqueo",
                detail = if (hasPostNotifications && bridgeOrLockReady) {
                    "Notificaciones y controles en bloqueo habilitados"
                } else {
                    "Revisa el permiso de notificaciones y la visibilidad en bloqueo"
                },
                passed = hasPostNotifications && bridgeOrLockReady,
            ),
            DiagnosticCheckItem(
                category = DiagnosticCategory.QUICK_SETTINGS,
                title = "Ajustes Rápidos",
                detail = if (hasQuickTileAdded) {
                    "Mosaico añadido a los Ajustes Rápidos del sistema"
                } else {
                    "Añade al menos un mosaico desde Controles > Ajustes Rápidos"
                },
                passed = hasQuickTileAdded,
            ),
            DiagnosticCheckItem(
                category = DiagnosticCategory.HIRES_OUTPUT,
                title = "Salida Hi-Res / DAC",
                detail = "${hardwareReport.activeDeviceName} • ${hardwareReport.nativeSampleRateHz / 1000} kHz (${if (hardwareReport.isUsbDacConnected) "USB DAC" else "Salida local"})",
                passed = hardwareReport.supportsFloatPcm || hardwareReport.isUsbDacConnected,
            ),
        )
        return HiResDiagnosticsReport(items = items)
    }
}
