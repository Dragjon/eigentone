package com.phonons.app.ui

import android.Manifest
import android.app.ForegroundServiceStartNotAllowedException
import android.content.Intent
import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import com.phonons.app.data.Repo
import com.phonons.app.engine.AudioService
import com.phonons.app.engine.PresetShortcuts

class MainActivity : ComponentActivity() {

    private val notificationPermission =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) { /* Optional. */ }

    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)
        notificationPermission.launch(Manifest.permission.POST_NOTIFICATIONS)
        setContent { PhononsTheme { PhononsApp(onSetEnabled = ::setEnabled) } }
        handleShortcut(intent)
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        handleShortcut(intent)
    }

    /** Loads the preset chosen from the app icon. Only once, so rotating the phone doesn't undo later changes. */
    private fun handleShortcut(intent: Intent?) {
        if (intent?.action != PresetShortcuts.ACTION_LOAD_PRESET) return
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
            Repo.status.value = Repo.status.value.copy(error = "Android blocked the background service: ${e.message}")
        }
    }
}
