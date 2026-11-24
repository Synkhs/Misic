package com.example.core.compat

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.PowerManager
import android.provider.Settings
import java.util.Locale

enum class OemFamily(
    val displayName: String,
    val uiLayerName: String,
) {
    SAMSUNG("Samsung", "One UI"),
    XIAOMI("Xiaomi / Redmi / POCO", "HyperOS / MIUI"),
    OPPO_REALME("OPPO / realme", "ColorOS / realme UI"),
    ONEPLUS("OnePlus", "OxygenOS"),
    HONOR("HONOR", "Honor"),
    HUAWEI("Huawei", "EMUI / HarmonyOS"),
    VIVO("vivo / iQOO", "Funtouch OS / OriginOS"),
    MOTOROLA("Motorola", "MyUX / Hello UI"),
    GOOGLE_PIXEL("Google Pixel", "Pixel UI"),
    SONY("Sony", "Xperia UI"),
    NOTHING("Nothing", "Nothing OS"),
    GENERIC("Android", "Sistema estándar"),
}

data class OemBatteryGuide(
    val family: OemFamily,
    val manufacturerRaw: String,
    val isIgnoringBatteryOptimizations: Boolean,
    val summary: String,
    val steps: List<String>,
)

object OemCompat {

    fun detectOemFamily(
        manufacturer: String = Build.MANUFACTURER.orEmpty(),
        brand: String = Build.BRAND.orEmpty(),
    ): OemFamily {
        val token = "${manufacturer.lowercase(Locale.ROOT)} ${brand.lowercase(Locale.ROOT)}"
        return when {
            token.contains("samsung") -> OemFamily.SAMSUNG
            token.contains("xiaomi") || token.contains("redmi") || token.contains("poco") -> OemFamily.XIAOMI
            token.contains("oneplus") -> OemFamily.ONEPLUS
            token.contains("oppo") || token.contains("realme") -> OemFamily.OPPO_REALME
            token.contains("honor") -> OemFamily.HONOR
            token.contains("huawei") -> OemFamily.HUAWEI
            token.contains("vivo") || token.contains("iqoo") -> OemFamily.VIVO
            token.contains("motorola") || token.contains("moto") -> OemFamily.MOTOROLA
            token.contains("google") || token.contains("pixel") -> OemFamily.GOOGLE_PIXEL
            token.contains("sony") -> OemFamily.SONY
            token.contains("nothing") -> OemFamily.NOTHING
            else -> OemFamily.GENERIC
        }
    }

    fun isIgnoringBatteryOptimizations(context: Context): Boolean {
        val pm = context.getSystemService(Context.POWER_SERVICE) as? PowerManager ?: return false
        return runCatching { pm.isIgnoringBatteryOptimizations(context.packageName) }.getOrDefault(false)
    }

    fun getBatteryAndBackgroundGuide(
        context: Context,
        manufacturer: String = Build.MANUFACTURER.orEmpty(),
        brand: String = Build.BRAND.orEmpty(),
    ): OemBatteryGuide {
        val family = detectOemFamily(manufacturer, brand)
        val ignoring = isIgnoringBatteryOptimizations(context)
        val steps = when (family) {
            OemFamily.SAMSUNG -> listOf(
                "Abre Información de la app > Batería y selecciona 'No restringido'.",
                "En Ajustes > Cuidado del dispositivo > Batería > Límites de uso en segundo plano, añade MagicMusic a 'Aplicaciones nunca inactivas'.",
            )
            OemFamily.XIAOMI -> listOf(
                "Ve a Ajustes > Aplicaciones > MagicMusic > Ahorro de batería y elige 'Sin restricciones'.",
                "Activa 'Inicio automático en segundo plano' para evitar que HyperOS/MIUI detenga el widget al limpiar memoria.",
            )
            OemFamily.OPPO_REALME, OemFamily.ONEPLUS -> listOf(
                "Entra en Información de la app > Uso de batería y activa 'Permitir actividad en segundo plano'.",
                "Activa 'Permitir inicio automático' para que la sincronización multimedia reconecte tras reiniciar.",
            )
            OemFamily.HONOR, OemFamily.HUAWEI -> listOf(
                "Ve a Ajustes > Batería > Inicio de aplicaciones, busca MagicMusic y cambia a 'Gestionar manualmente'.",
                "Deja activos: Inicio automático, Inicio secundario y Ejecutar en segundo plano.",
            )
            OemFamily.VIVO -> listOf(
                "En Información de la app > Batería selecciona 'Uso en segundo plano alto'.",
                "Habilita 'Inicio automático' en el administrador del sistema para mantener vivos los widgets.",
            )
            OemFamily.MOTOROLA, OemFamily.GOOGLE_PIXEL, OemFamily.SONY, OemFamily.NOTHING, OemFamily.GENERIC -> listOf(
                "Abre Información de la app > Uso de batería de la app y elige 'Sin restricciones'.",
                "Comprueba que las notificaciones no estén silenciadas si usas controles en pantalla de bloqueo.",
            )
        }

        val summary = if (ignoring) {
            "${family.displayName} (${family.uiLayerName}): optimización de batería desactivada correctamente."
        } else {
            "${family.displayName} (${family.uiLayerName}): recomienda configurar batería en 'Sin restricciones' para mantener widgets y bloqueo siempre actualizados."
        }

        return OemBatteryGuide(
            family = family,
            manufacturerRaw = manufacturer.ifBlank { "Android" },
            isIgnoringBatteryOptimizations = ignoring,
            summary = summary,
            steps = steps,
        )
    }

    fun openBatterySettingsSafe(context: Context): CompatActionResult {
        val candidates = listOf(
            Intent(Settings.ACTION_IGNORE_BATTERY_OPTIMIZATION_SETTINGS),
            Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
                data = Uri.fromParts("package", context.packageName, null)
            },
        )
        return launchFirstResolvableIntent(
            context = context,
            intents = candidates,
            fallbackError = "No se pudo abrir la configuración de batería en este dispositivo.",
        )
    }

    fun openAppNotificationSettingsSafe(context: Context): CompatActionResult {
        val candidates = listOf(
            Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS).apply {
                putExtra(Settings.EXTRA_APP_PACKAGE, context.packageName)
            },
            Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
                data = Uri.fromParts("package", context.packageName, null)
            },
        )
        return launchFirstResolvableIntent(
            context = context,
            intents = candidates,
            fallbackError = "No se pudo abrir la configuración de notificaciones.",
        )
    }

    fun openNotificationListenerSettingsSafe(context: Context): CompatActionResult {
        val candidates = listOf(
            Intent(Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS),
            Intent(Settings.ACTION_SETTINGS),
        )
        return launchFirstResolvableIntent(
            context = context,
            intents = candidates,
            fallbackError = "No se pudo abrir el acceso a notificaciones del sistema.",
        )
    }

    fun openAppDetailsSettingsSafe(context: Context): CompatActionResult {
        val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
            data = Uri.fromParts("package", context.packageName, null)
        }
        return launchFirstResolvableIntent(
            context = context,
            intents = listOf(intent),
            fallbackError = "No se pudieron abrir los detalles de la aplicación.",
        )
    }

    private fun launchFirstResolvableIntent(
        context: Context,
        intents: List<Intent>,
        fallbackError: String,
    ): CompatActionResult {
        val pm = context.packageManager
        for (intent in intents) {
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            val resolvable = runCatching { intent.resolveActivity(pm) != null }.getOrDefault(false)
            if (resolvable) {
                val launched = runCatching {
                    context.startActivity(intent)
                    true
                }.getOrDefault(false)
                if (launched) {
                    return CompatActionResult.Success("Ajustes del sistema abiertos.")
                }
            }
        }
        return CompatActionResult.FallbackManualGuide(fallbackError)
    }
}
