package com.phonons.app.ui

import android.Manifest
import android.app.ForegroundServiceStartNotAllowedException
import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import com.phonons.app.data.Repo
import com.phonons.app.engine.AudioService

class MainActivity : ComponentActivity() {

    private val notificationPermission =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) { /* Optional. */ }

    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)
        notificationPermission.launch(Manifest.permission.POST_NOTIFICATIONS)
        setContent { PhononsTheme { PhononsApp(onSetEnabled = ::setEnabled) } }
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
