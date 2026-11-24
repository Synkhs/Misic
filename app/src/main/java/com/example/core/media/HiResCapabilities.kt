package com.example.core.media

import android.content.Context
import android.media.AudioDeviceInfo
import android.media.AudioFormat
import android.media.AudioManager
import android.os.Build
import androidx.compose.runtime.Immutable
import com.example.data.AudioOutputCategory

@Immutable
data class HiResHardwareReport(
    val activeCategory: AudioOutputCategory = AudioOutputCategory.SPEAKER,
    val activeDeviceName: String = "Altavoz del dispositivo",
    val nativeSampleRateHz: Int = 48000,
    val nativeFramesPerBuffer: Int = 256,
    val maxReportedSampleRateHz: Int = 48000,
    val supportsFloatPcm: Boolean = true,
    val supports24BitPcm: Boolean = false,
    val isUsbDacConnected: Boolean = false,
    val bitPerfectDirectAvailable: Boolean = false,
    val bitPerfectExplanation: String = "Android mezcla y remuestrea el audio del sistema a la frecuencia nativa de salida.",
)

object HiResCapabilities {

    fun inspectHardware(context: Context): HiResHardwareReport {
        val am = context.getSystemService(Context.AUDIO_SERVICE) as? AudioManager
            ?: return HiResHardwareReport()

        val nativeSr = am.getProperty(AudioManager.PROPERTY_OUTPUT_SAMPLE_RATE)?.toIntOrNull() ?: 48000
        val framesPerBuf = am.getProperty(AudioManager.PROPERTY_OUTPUT_FRAMES_PER_BUFFER)?.toIntOrNull() ?: 256
        val devices = am.getDevices(AudioManager.GET_DEVICES_OUTPUTS).orEmpty()

        val usbDac = devices.firstOrNull {
            it.type == AudioDeviceInfo.TYPE_USB_DEVICE ||
                it.type == AudioDeviceInfo.TYPE_USB_HEADSET ||
                (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S && it.type == AudioDeviceInfo.TYPE_USB_ACCESSORY)
        }
        val bluetooth = devices.firstOrNull {
            it.type == AudioDeviceInfo.TYPE_BLUETOOTH_A2DP ||
                (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S && it.type == AudioDeviceInfo.TYPE_BLE_HEADSET)
        }
        val wired = devices.firstOrNull {
            it.type == AudioDeviceInfo.TYPE_WIRED_HEADPHONES ||
                it.type == AudioDeviceInfo.TYPE_WIRED_HEADSET ||
                it.type == AudioDeviceInfo.TYPE_LINE_ANALOG
        }
        val speaker = devices.firstOrNull { it.type == AudioDeviceInfo.TYPE_BUILTIN_SPEAKER }

        val activeDevice = usbDac ?: bluetooth ?: wired ?: speaker
        val category = when {
            usbDac != null -> AudioOutputCategory.USB_DAC
            bluetooth != null -> AudioOutputCategory.BLUETOOTH
            wired != null -> AudioOutputCategory.WIRED_HEADSET
            else -> AudioOutputCategory.SPEAKER
        }
        val devName = activeDevice?.productName?.toString()?.takeIf { it.isNotBlank() } ?: category.label

        val rates = activeDevice?.sampleRates?.toList().orEmpty()
        val maxRate = (rates.maxOrNull() ?: nativeSr).coerceAtLeast(nativeSr)
        val encodings = activeDevice?.encodings?.toSet().orEmpty()
        val supportsFloat = encodings.contains(AudioFormat.ENCODING_PCM_FLOAT) ||
            Build.VERSION.SDK_INT >= Build.VERSION_CODES.M
        val supports24Bit = encodings.contains(AudioFormat.ENCODING_PCM_24BIT_PACKED) ||
            encodings.contains(AudioFormat.ENCODING_PCM_32BIT) ||
            supportsFloat

        val usbAttached = usbDac != null
        val bitPerfect = usbAttached && Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE
        val explanation = when {
            bitPerfect -> "USB DAC compatible detectado en Android 14+: ruta PCM de alta resolución sin conversión agresiva disponible."
            usbAttached -> "DAC USB detectado (${maxRate / 1000} kHz máx.). Android procesa la mezcla a ${nativeSr / 1000} kHz con punto flotante."
            category == AudioOutputCategory.BLUETOOTH -> "Salida Bluetooth ($devName). El códec inalámbrico aplica su propio muestreo hasta ${maxRate / 1000} kHz."
            category == AudioOutputCategory.WIRED_HEADSET -> "Auriculares por cable. Salida interna a ${nativeSr / 1000} kHz / ${if (supports24Bit) "24-bit flotante" else "16-bit"}."
            else -> "Altavoz integrado a ${nativeSr / 1000} kHz. Conecta unos auriculares o USB DAC para aprovechar mayor rango dinámico."
        }

        return HiResHardwareReport(
            activeCategory = category,
            activeDeviceName = devName,
            nativeSampleRateHz = nativeSr,
            nativeFramesPerBuffer = framesPerBuf,
            maxReportedSampleRateHz = maxRate,
            supportsFloatPcm = supportsFloat,
            supports24BitPcm = supports24Bit,
            isUsbDacConnected = usbAttached,
            bitPerfectDirectAvailable = bitPerfect,
            bitPerfectExplanation = explanation,
        )
    }
}
