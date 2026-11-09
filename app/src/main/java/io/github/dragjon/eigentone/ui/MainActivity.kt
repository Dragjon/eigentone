package io.github.dragjon.eigentone.ui

import io.github.dragjon.eigentone.R
import androidx.compose.ui.res.stringResource
import android.Manifest
import android.app.UiModeManager
import android.content.Intent
import android.graphics.Color
import android.media.AudioDeviceCallback
import android.media.AudioDeviceInfo
import android.media.AudioManager
import android.os.Build
import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.SystemBarStyle
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.activity.result.contract.ActivityResultContracts
import io.github.dragjon.eigentone.data.Repo
import io.github.dragjon.eigentone.data.ThemeMode
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import io.github.dragjon.eigentone.engine.AudioService
import io.github.dragjon.eigentone.engine.OutputDetector
import io.github.dragjon.eigentone.engine.PresetShortcuts
import io.github.dragjon.eigentone.engine.isBackgroundStartRefused

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
        // Android's light or dark choice for the app is set before anything draws, so Device always follows the phone
        // from the first frame, even when an earlier Light or Dark was left in place.
        matchWindowToTheme(Repo.state.value.themeMode)
        AppText.context = applicationContext
        // On a first run the welcome asks, with an explanation, when the EQ is turned on.
        // Android 12 and older show the notification without asking.
        if (Repo.state.value.welcomeDone && Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) notificationPermission.launch(Manifest.permission.POST_NOTIFICATIONS)
        setContent {
            val modes = remember { Repo.state.map { it.themeMode }.distinctUntilChanged() }
            val mode by modes.collectAsStateWithLifecycle(Repo.state.value.themeMode)
            val dark = isDarkTheme(mode)
            LaunchedEffect(dark) { matchSystemBars(dark) }
            // Only the launch window needs telling, and doing it now rebuilds the whole app like a change of
            // configuration, so a theme switch lagged. It's told when the app goes into the background instead.
            // Device needs it at once, or the app would keep answering with the last Light or Dark. Light and Dark
            // wait until the app is left, since setting it now rebuilds the whole app.
            LaunchedEffect(mode) {
                if (mode == ThemeMode.DEVICE) {
                    matchWindowToTheme(mode)
                    pendingWindowTheme = null
                } else {
                    pendingWindowTheme = mode
                }
            }
            EigentoneTheme(dark) { MenuHost { EigentoneApp(onSetEnabled = ::setEnabled) } }
        }
        // Otherwise reopening from recents after Android closed the app would load the preset again.
        if (savedInstanceState == null) handleShortcut(intent)
    }

    /** Dark icons on a light bar and light icons on a dark one, whatever the phone itself is set to. */
    private fun matchSystemBars(dark: Boolean) {
        val clear = Color.TRANSPARENT
        val style = if (dark) SystemBarStyle.dark(clear) else SystemBarStyle.light(clear, clear)
        enableEdgeToEdge(statusBarStyle = style, navigationBarStyle = style)
    }

    /** Android remembers this, so the window behind the app opens in the right colour next time, with no flash. */
    private fun matchWindowToTheme(mode: ThemeMode) {
        // Only from Android 12. Before that the window takes the phone's own light or dark setting.
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.S) return
        getSystemService(UiModeManager::class.java).setApplicationNightMode(
            when (mode) {
                ThemeMode.DEVICE -> UiModeManager.MODE_NIGHT_AUTO
                ThemeMode.LIGHT -> UiModeManager.MODE_NIGHT_NO
                ThemeMode.DARK -> UiModeManager.MODE_NIGHT_YES
            },
        )
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
        // An open listening tool puts its own sound back when it closes, which would undo the switch.
        if (preset == null || Repo.toolBusy) return
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

    /** The theme the window behind the app should open in next time, set on the way out. */
    private var pendingWindowTheme: ThemeMode? = null

    override fun onStop() {
        pendingWindowTheme?.let(::matchWindowToTheme)
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
        } catch (e: IllegalStateException) {
            if (!e.isBackgroundStartRefused()) throw e
            Log.w("Eigentone", "Could not start engine", e)
            Repo.status.value = Repo.status.value.copy(error = appText(R.string.ui_android_blocked_the_background_service, e.message))
        }
    }
}
