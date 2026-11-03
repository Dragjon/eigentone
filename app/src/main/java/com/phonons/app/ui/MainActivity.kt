package com.phonons.app.ui

import com.phonons.app.R
import androidx.compose.ui.res.stringResource
import android.Manifest
import android.app.ForegroundServiceStartNotAllowedException
import android.content.Intent
import android.media.AudioDeviceCallback
import android.media.AudioDeviceInfo
import android.media.AudioManager
import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import com.phonons.app.data.Repo
import com.phonons.app.engine.AudioService
import com.phonons.app.engine.OutputDetector
import com.phonons.app.engine.PresetShortcuts

class MainActivity : ComponentActivity() {

    /** Also runs while the EQ is off, when no service is watching, so screens always name the right output. */
    private val outputs = object : AudioDeviceCallback() {
        override fun onAudioDevicesAdded(added: Array<out AudioDeviceInfo>) {
            Repo.output.value = OutputDetector.current(this@MainActivity).info
        }

        override fun onAudioDevicesRemoved(removed: Array<out AudioDeviceInfo>) {
            Repo.output.value = OutputDetector.current(this@MainActivity).info
        }
    }

    private val notificationPermission =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) { /* Optional. */ }

    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)
        AppText.context = applicationContext
        notificationPermission.launch(Manifest.permission.POST_NOTIFICATIONS)
        setContent { PhononsTheme { PhononsApp(onSetEnabled = ::setEnabled) } }
        // Otherwise reopening from recents after Android closed the app would load the preset again.
        if (savedInstanceState == null) handleShortcut(intent)
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        handleShortcut(intent)
    }

    /** Loads the preset chosen from the app icon. Only once, so rotating the phone doesn't undo later changes. */
    private fun handleShortcut(intent: Intent?) {
        if (intent?.action != PresetShortcuts.ACTION_LOAD_PRESET) return
        if (intent.flags and Intent.FLAG_ACTIVITY_LAUNCHED_FROM_HISTORY != 0) return
        val preset = intent.getStringExtra(PresetShortcuts.EXTRA_PRESET_ID)?.let(Repo::findPreset)
        intent.action = null
        if (preset == null) return
        Repo.loadPreset(preset)
        if (!Repo.state.value.enabled) setEnabled(true)
    }

    override fun onStart() {
        super.onStart()
        // Picks up a tile press Android refused to start in the background.
        if (Repo.state.value.enabled) startEngine()
        Repo.output.value = OutputDetector.current(this).info
        getSystemService(AudioManager::class.java).registerAudioDeviceCallback(outputs, null)
    }

    override fun onStop() {
        getSystemService(AudioManager::class.java).unregisterAudioDeviceCallback(outputs)
        super.onStop()
    }

    private fun setEnabled(on: Boolean) {
        Repo.update { it.copy(enabled = on) }
        if (on) startEngine()
        AudioService.requestTileRefresh(this)
    }

    private fun startEngine() {
        try {
            AudioService.start(this)
        } catch (e: ForegroundServiceStartNotAllowedException) {
            Log.w("Phonons", "Could not start engine", e)
            Repo.status.value = Repo.status.value.copy(error = appText(R.string.ui_android_blocked_the_background_service, e.message))
        }
    }
}
