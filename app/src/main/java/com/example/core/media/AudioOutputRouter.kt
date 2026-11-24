package com.example.core.media

import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.media.AudioDeviceInfo
import android.media.AudioManager
import android.media.MediaRoute2Info
import android.media.MediaRouter2
import android.os.Build
import android.provider.Settings
import com.example.data.AudioOutputDevice

object AudioOutputRouter {

    @SuppressLint("NewApi")
    fun listOutputDevices(context: Context): List<AudioOutputDevice> {
        val result = mutableListOf<AudioOutputDevice>()

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            runCatching {
                val router2 = MediaRouter2.getInstance(context)
                val activeRoutes = router2.systemController.selectedRoutes
                val activeIds = activeRoutes.map { it.id }.toSet()
                val allRoutes = router2.routes
                allRoutes.forEach { route ->
                    val type = route.type
                    val isBt = type == MediaRoute2Info.TYPE_BLUETOOTH_A2DP ||
                        type == MediaRoute2Info.TYPE_BLE_HEADSET ||
                        type == MediaRoute2Info.TYPE_HEARING_AID
                    val typeLabel = when (type) {
                        MediaRoute2Info.TYPE_BUILTIN_SPEAKER -> "Altavoz del dispositivo"
                        MediaRoute2Info.TYPE_WIRED_HEADSET,
                        MediaRoute2Info.TYPE_WIRED_HEADPHONES,
                        MediaRoute2Info.TYPE_USB_HEADSET,
                        MediaRoute2Info.TYPE_USB_DEVICE -> "Audio por cable / USB"
                        MediaRoute2Info.TYPE_BLUETOOTH_A2DP,
                        MediaRoute2Info.TYPE_BLE_HEADSET -> "Bluetooth Audio"
                        MediaRoute2Info.TYPE_REMOTE_SPEAKER,
                        MediaRoute2Info.TYPE_REMOTE_AUDIO_VIDEO_RECEIVER -> "Dispositivo externo"
                        else -> "Salida de audio"
                    }
                    result.add(
                        AudioOutputDevice(
                            id = "mr2_${route.id}",
                            name = route.name.toString().ifBlank { typeLabel },
                            typeLabel = typeLabel,
                            isActive = activeIds.contains(route.id),
                            isBluetooth = isBt,
                            routeId = route.id,
                        ),
                    )
                }
            }
        }

        if (result.isEmpty()) {
            val am = context.getSystemService(Context.AUDIO_SERVICE) as? AudioManager
            val devices = am?.getDevices(AudioManager.GET_DEVICES_OUTPUTS).orEmpty()
            devices.forEach { dev ->
                val isBt = dev.type == AudioDeviceInfo.TYPE_BLUETOOTH_A2DP ||
                    dev.type == AudioDeviceInfo.TYPE_BLUETOOTH_SCO
                val label = when (dev.type) {
                    AudioDeviceInfo.TYPE_BUILTIN_SPEAKER -> "Altavoz"
                    AudioDeviceInfo.TYPE_WIRED_HEADPHONES,
                    AudioDeviceInfo.TYPE_WIRED_HEADSET,
                    AudioDeviceInfo.TYPE_USB_HEADSET -> "Auriculares con cable"
                    AudioDeviceInfo.TYPE_BLUETOOTH_A2DP -> "Bluetooth A2DP"
                    else -> "Salida multimedia"
                }
                val name = dev.productName?.toString()?.takeIf { it.isNotBlank() } ?: label
                if (result.none { it.name == name && it.typeLabel == label }) {
                    result.add(
                        AudioOutputDevice(
                            id = "am_${dev.id}",
                            name = name,
                            typeLabel = label,
                            isActive = isBt || dev.type == AudioDeviceInfo.TYPE_BUILTIN_SPEAKER,
                            isBluetooth = isBt,
                            routeId = null,
                        ),
                    )
                }
            }
        }

        if (result.isEmpty()) {
            result.add(
                AudioOutputDevice(
                    id = "default_speaker",
                    name = "Altavoz del teléfono",
                    typeLabel = "Salida estándar",
                    isActive = true,
                    isBluetooth = false,
                ),
            )
        }
        return result.distinctBy { "${it.name}_${it.typeLabel}" }
    }

    fun selectOutputRoute(context: Context, device: AudioOutputDevice): Boolean {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R && !device.routeId.isNullOrBlank()) {
            val transferred = runCatching {
                val router2 = MediaRouter2.getInstance(context)
                val target = router2.routes.firstOrNull { it.id == device.routeId }
                if (target != null) {
                    router2.transferTo(target)
                    true
                } else {
                    false
                }
            }.getOrDefault(false)
            if (transferred) return true
        }
        return openSystemOutputOrBluetoothSettings(context)
    }

    fun openSystemOutputOrBluetoothSettings(context: Context): Boolean {
        val candidates = listOf(
            Intent("com.android.settings.panel.action.VOLUME"),
            Intent(Settings.ACTION_BLUETOOTH_SETTINGS),
            Intent(Settings.ACTION_SOUND_SETTINGS),
        )
        for (intent in candidates) {
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            val ok = runCatching {
                if (intent.resolveActivity(context.packageManager) != null) {
                    context.startActivity(intent)
                    true
                } else {
                    false
                }
            }.getOrDefault(false)
            if (ok) return true
        }
        return false
    }
}
