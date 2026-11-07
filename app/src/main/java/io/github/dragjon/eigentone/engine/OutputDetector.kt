package io.github.dragjon.eigentone.engine

import android.content.Context
import android.media.AudioDeviceInfo
import android.media.AudioManager
import io.github.dragjon.eigentone.data.OutputInfo
import io.github.dragjon.eigentone.data.OutputKind

/** Works out which output the sound is going to. The app needs this even while the EQ is off. */
object OutputDetector {
    class Detected(val info: OutputInfo, val deviceType: Int)

    private val PRIORITY = listOf(
        AudioDeviceInfo.TYPE_USB_HEADSET,
        AudioDeviceInfo.TYPE_USB_DEVICE,
        AudioDeviceInfo.TYPE_WIRED_HEADPHONES,
        AudioDeviceInfo.TYPE_WIRED_HEADSET,
        AudioDeviceInfo.TYPE_BLE_HEADSET,
        AudioDeviceInfo.TYPE_BLE_SPEAKER,
        AudioDeviceInfo.TYPE_BLUETOOTH_A2DP,
    )

    fun current(context: Context): Detected {
        val audio = context.getSystemService(AudioManager::class.java)
        // Where Android is sending music right now, which matters when two outputs are connected at once.
        val routed = if (android.os.Build.VERSION.SDK_INT < android.os.Build.VERSION_CODES.TIRAMISU) emptyList() else runCatching {
            audio.getAudioDevicesForAttributes(
                android.media.AudioAttributes.Builder().setUsage(android.media.AudioAttributes.USAGE_MEDIA).build(),
            )
        }.getOrNull().orEmpty()
        val routedBest = PRIORITY.firstNotNullOfOrNull { type -> routed.firstOrNull { it.type == type } }
        val routedToPhone = routedBest == null && routed.any { it.type == AudioDeviceInfo.TYPE_BUILTIN_SPEAKER }
        // Otherwise, or on a phone that doesn't say, the most likely of everything connected.
        val devices = audio.getDevices(AudioManager.GET_DEVICES_OUTPUTS)
        val best = when {
            routedBest != null -> routedBest
            routedToPhone -> null
            else -> PRIORITY.firstNotNullOfOrNull { type -> devices.firstOrNull { it.type == type } }
        }
        val speaker = OutputInfo("speaker", "Phone speaker")
        if (best == null) return Detected(speaker, AudioDeviceInfo.TYPE_BUILTIN_SPEAKER)
        val family = when (best.type) {
            AudioDeviceInfo.TYPE_BLUETOOTH_A2DP,
            AudioDeviceInfo.TYPE_BLE_HEADSET,
            AudioDeviceInfo.TYPE_BLE_SPEAKER -> "Bluetooth"
            AudioDeviceInfo.TYPE_USB_HEADSET,
            AudioDeviceInfo.TYPE_USB_DEVICE -> "USB"
            else -> "Wired"
        }
        val name = best.productName?.toString().orEmpty().ifBlank { family }
        val key = "$family:$name"
        val kind = OutputKind.guess(key, name, speakerType = best.type == AudioDeviceInfo.TYPE_BLE_SPEAKER)
        val label = if (name != family) name else when (kind) {
            OutputKind.SPEAKER -> "$family speaker"
            else -> "$family headphones"
        }
        return Detected(OutputInfo(key, label, kind), best.type)
    }
}
