package com.example.core.compat

import android.app.ForegroundServiceStartNotAllowedException
import android.app.Notification
import android.app.Service
import android.app.StatusBarManager
import android.appwidget.AppWidgetManager
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.graphics.drawable.Icon
import android.os.Build
import android.os.PowerManager

sealed interface CompatActionResult {
    data class Success(val message: String) : CompatActionResult
    data class AlreadyConfigured(val message: String) : CompatActionResult
    data class FallbackManualGuide(val message: String) : CompatActionResult
    data class Error(val message: String) : CompatActionResult
}

object AndroidVersionCompat {

    fun currentApiLevel(): Int = Build.VERSION.SDK_INT

    fun supportsDynamicColor(): Boolean = Build.VERSION.SDK_INT >= Build.VERSION_CODES.S

    fun supportsRenderEffectBlur(): Boolean = Build.VERSION.SDK_INT >= Build.VERSION_CODES.S

    fun supportsRequestAddTileService(): Boolean = Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU

    fun supportsPostNotificationsRuntime(): Boolean = Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU

    fun supportsPredictiveBack(): Boolean = Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU

    fun supportsForegroundServiceTypes(): Boolean = Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE

    fun supportsGeneratedWidgetPreview(): Boolean = Build.VERSION.SDK_INT >= Build.VERSION_CODES.VANILLA_ICE_CREAM

    fun supportsTileSubtitle(): Boolean = Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q

    fun supportsRequestPinAppWidget(context: Context): Boolean {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return false
        val awm = AppWidgetManager.getInstance(context) ?: return false
        return runCatching { awm.isRequestPinAppWidgetSupported }.getOrDefault(false)
    }

    fun requestPinAppWidgetSafe(
        context: Context,
        provider: ComponentName,
        successMessage: String,
    ): CompatActionResult {
        val awm = AppWidgetManager.getInstance(context)
            ?: return CompatActionResult.Error("AppWidgetManager no disponible en este dispositivo.")

        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O || !supportsRequestPinAppWidget(context)) {
            return CompatActionResult.FallbackManualGuide(
                "Tu lanzador no admite fijación directa. Mantén pulsada la pantalla de inicio, entra en Widgets y añade MagicMusic.",
            )
        }

        return runCatching {
            if (awm.requestPinAppWidget(provider, null, null)) {
                CompatActionResult.Success(successMessage)
            } else {
                CompatActionResult.FallbackManualGuide(
                    "El lanzador rechazó la solicitud automática. Añade el widget manualmente desde el menú de Widgets.",
                )
            }
        }.getOrElse { err ->
            CompatActionResult.Error("No se pudo solicitar el widget: ${err.localizedMessage ?: "error desconocido"}")
        }
    }

    fun requestAddTileServiceSafe(
        context: Context,
        tileComponent: ComponentName,
        label: CharSequence,
        iconResId: Int,
        onResult: (CompatActionResult) -> Unit,
    ) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) {
            onResult(
                CompatActionResult.FallbackManualGuide(
                    "Disponible desde Android 13 con un toque. En esta versión, desliza Ajustes Rápidos > Editar y arrastra '$label'.",
                ),
            )
            return
        }

        val sbm = context.getSystemService(StatusBarManager::class.java)
        if (sbm == null) {
            onResult(
                CompatActionResult.FallbackManualGuide(
                    "Desliza dos veces la barra superior, pulsa Editar y arrastra '$label' a tus Ajustes Rápidos.",
                ),
            )
            return
        }

        runCatching {
            sbm.requestAddTileService(
                tileComponent,
                label,
                Icon.createWithResource(context, iconResId),
                context.mainExecutor,
            ) { code ->
                val result = when (code) {
                    StatusBarManager.TILE_ADD_REQUEST_RESULT_TILE_ADDED ->
                        CompatActionResult.Success("Acceso rápido '$label' añadido correctamente.")
                    StatusBarManager.TILE_ADD_REQUEST_RESULT_TILE_ALREADY_ADDED ->
                        CompatActionResult.AlreadyConfigured("El acceso '$label' ya está en Ajustes Rápidos.")
                    StatusBarManager.TILE_ADD_REQUEST_RESULT_TILE_NOT_ADDED ->
                        CompatActionResult.FallbackManualGuide("No se añadió '$label'. Puedes activarlo desde Ajustes Rápidos > Editar.")
                    else ->
                        CompatActionResult.FallbackManualGuide("Añade '$label' manualmente editando tus Ajustes Rápidos.")
                }
                onResult(result)
            }
        }.onFailure { err ->
            onResult(
                CompatActionResult.Error("Error al solicitar el acceso rápido: ${err.localizedMessage ?: "desconocido"}"),
            )
        }
    }

    fun startForegroundServiceSafe(
        context: Context,
        intent: Intent,
    ): Boolean {
        return try {
            context.startForegroundService(intent)
            true
        } catch (e: SecurityException) {
            false
        } catch (e: IllegalStateException) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S && e is ForegroundServiceStartNotAllowedException) {
                false
            } else {
                false
            }
        } catch (_: Throwable) {
            false
        }
    }

    fun startMediaPlaybackForegroundCompat(
        service: Service,
        notificationId: Int,
        notification: Notification,
    ): Boolean {
        return try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                service.startForeground(
                    notificationId,
                    notification,
                    ServiceInfo.FOREGROUND_SERVICE_TYPE_MEDIA_PLAYBACK,
                )
            } else {
                service.startForeground(notificationId, notification)
            }
            true
        } catch (e: SecurityException) {
            false
        } catch (e: IllegalStateException) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S && e is ForegroundServiceStartNotAllowedException) {
                false
            } else {
                false
            }
        } catch (_: Throwable) {
            false
        }
    }

    fun isInteractiveScreenOn(context: Context): Boolean {
        val pm = context.getSystemService(Context.POWER_SERVICE) as? PowerManager ?: return true
        return pm.isInteractive
    }
}
