package com.phonons.app.engine

import android.content.Context
import android.media.AudioDeviceInfo
import android.media.AudioManager
import com.phonons.app.data.OutputInfo

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
        val devices = context.getSystemService(AudioManager::class.java).getDevices(AudioManager.GET_DEVICES_OUTPUTS)
        val best = PRIORITY.firstNotNullOfOrNull { type -> devices.firstOrNull { it.type == type } }
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
        return Detected(OutputInfo("$family:$name", if (name == family) "$family headphones" else name), best.type)
    }
}
