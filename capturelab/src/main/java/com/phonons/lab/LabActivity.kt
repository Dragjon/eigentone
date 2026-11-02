package com.phonons.lab

import android.Manifest
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.pm.PackageManager
import android.media.AudioManager
import android.media.projection.MediaProjectionConfig
import android.media.projection.MediaProjectionManager
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Slider
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import java.util.Locale

class LabActivity : ComponentActivity() {

    private val projectionRequest =
        registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
            val data = result.data
            if (result.resultCode == RESULT_OK && data != null) {
                LabService.start(this, result.resultCode, data)
            } else {
                LabControls.stats.value = LabStats(error = "Capture permission was declined.")
            }
        }

    private val permissions =
        registerForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) { granted ->
            if (granted[Manifest.permission.RECORD_AUDIO] == true) {
                askForCapture()
            } else {
                LabControls.stats.value = LabStats(error = "Audio recording permission is required for capture.")
            }
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)
        // Recovers from a crash or kill that happened while the music stream was muted.
        restoreMusicIfNeeded()
        setContent {
            MaterialTheme(colorScheme = darkColorScheme()) {
                Surface(Modifier.fillMaxSize()) { LabScreen() }
            }
        }
    }

    private fun startTest() {
        if (checkSelfPermission(Manifest.permission.RECORD_AUDIO) == PackageManager.PERMISSION_GRANTED) {
            askForCapture()
        } else {
            permissions.launch(arrayOf(Manifest.permission.RECORD_AUDIO, Manifest.permission.POST_NOTIFICATIONS))
        }
    }

    private fun askForCapture() {
        val manager = getSystemService(MediaProjectionManager::class.java)
        // Whole-screen consent, because audio capture does not apply to a single-app selection.
        projectionRequest.launch(
            manager.createScreenCaptureIntent(MediaProjectionConfig.createConfigForDefaultDisplay()),
        )
    }

    private fun setMusicMuted(muted: Boolean) {
        getSystemService(AudioManager::class.java).adjustStreamVolume(
            AudioManager.STREAM_MUSIC,
            if (muted) AudioManager.ADJUST_MUTE else AudioManager.ADJUST_UNMUTE,
            0,
        )
        getSharedPreferences(PREFS, Context.MODE_PRIVATE).edit().putBoolean(KEY_MUTED, muted).apply()
    }

    private fun restoreMusicIfNeeded() {
        if (getSharedPreferences(PREFS, Context.MODE_PRIVATE).getBoolean(KEY_MUTED, false)) setMusicMuted(false)
    }

    private fun copy(text: String) {
        getSystemService(ClipboardManager::class.java).setPrimaryClip(ClipData.newPlainText("Capture Lab report", text))
    }

    @OptIn(ExperimentalLayoutApi::class)
    @Composable
    private fun LabScreen() {
        val stats by LabControls.stats.collectAsStateWithLifecycle()
        var route by remember { mutableStateOf(LabControls.route) }
        var muffle by remember { mutableStateOf(LabControls.muffle) }
        var level by remember { mutableStateOf(LabControls.outputLevel) }
        var muted by remember { mutableStateOf(false) }
        var appName by remember { mutableStateOf("") }
        var copied by remember { mutableStateOf(false) }
        val log = remember { mutableStateListOf<String>() }

        // The service restores sound when capture ends, so keep the switch in step with it.
        LaunchedEffect(stats.running) {
            if (!stats.running) muted = false
        }

        fun snapshot() = "route=${route.label} muted=$muted muffle=$muffle " +
            "peak=${String.format(Locale.US, "%.0f", stats.capturedPeakDb)}dB musicActive=${stats.musicActive} " +
            "latency=${stats.latencyMs}ms underruns=${stats.underruns}"

        Column(
            Modifier
                .fillMaxSize()
                .windowInsetsPadding(WindowInsets.safeDrawing)
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Text("Capture Lab", style = MaterialTheme.typography.headlineSmall)
            Text("Tests whether Studio mode can work on this phone. Use headphones at low volume.")

            Card(Modifier.fillMaxWidth()) {
                Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text("1. Capture", style = MaterialTheme.typography.titleMedium)
                    if (stats.running) {
                        OutlinedButton(onClick = {
                            if (muted) { setMusicMuted(false); muted = false }
                            LabService.stop(this@LabActivity)
                        }) { Text("Stop and restore sound") }
                    } else {
                        Button(onClick = { startTest() }) { Text("Start test") }
                    }
                    Text(verdict(stats), color = MaterialTheme.colorScheme.primary)
                    Text(
                        "Level ${String.format(Locale.US, "%.0f", stats.capturedPeakDb)} dB · " +
                            "delay about ${stats.latencyMs} ms · dropouts ${stats.underruns} · ${stats.seconds}s",
                        style = MaterialTheme.typography.bodySmall,
                    )
                    stats.error?.let { Text(it, color = MaterialTheme.colorScheme.error) }
                }
            }

            Card(Modifier.fillMaxWidth()) {
                Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("2. Apps", style = MaterialTheme.typography.titleMedium)
                    Text("Play something in an app, return here, type its name, then log it.")
                    OutlinedTextField(
                        value = appName,
                        onValueChange = { appName = it },
                        singleLine = true,
                        label = { Text("App name") },
                        modifier = Modifier.fillMaxWidth(),
                    )
                    Button(enabled = stats.running && appName.isNotBlank(), onClick = {
                        log += "APP ${appName.trim()}: ${verdict(stats)} | ${snapshot()}"
                        appName = ""
                    }) { Text("Log this app") }
                }
            }

            Card(Modifier.fillMaxWidth()) {
                Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("3. Muting", style = MaterialTheme.typography.titleMedium)
                    Text("Play music, then turn on Mute original. The test sound is muffled on purpose.")
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text("Mute original", Modifier.weight(1f))
                        Switch(checked = muted, enabled = stats.running, onCheckedChange = {
                            setMusicMuted(it)
                            muted = it
                        })
                    }
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text("Muffle test sound", Modifier.weight(1f))
                        Switch(checked = muffle, onCheckedChange = {
                            muffle = it
                            LabControls.muffle = it
                        })
                    }
                    Text("Play the test sound through")
                    FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutputRoute.entries.forEach { option ->
                            FilterChip(
                                selected = route == option,
                                onClick = {
                                    route = option
                                    LabControls.route = option
                                },
                                label = { Text(option.label) },
                            )
                        }
                    }
                    Text("Test sound level ${(level * 100).toInt()}%")
                    Slider(value = level, onValueChange = {
                        level = it
                        LabControls.outputLevel = it
                    })
                    Text("What do you hear?")
                    FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        listOf("Only muffled", "Only clear", "Both", "Nothing").forEach { heard ->
                            OutlinedButton(enabled = stats.running, onClick = {
                                log += "HEARD \"$heard\": ${snapshot()}"
                            }) { Text(heard) }
                        }
                    }
                }
            }

            Card(Modifier.fillMaxWidth()) {
                Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("4. Report", style = MaterialTheme.typography.titleMedium)
                    log.forEach { Text(it, style = MaterialTheme.typography.bodySmall) }
                    if (log.isEmpty()) Text("Nothing logged yet.", style = MaterialTheme.typography.bodySmall)
                    Button(onClick = {
                        copy(report(stats, log))
                        copied = true
                    }) { Text(if (copied) "Copied" else "Copy report") }
                }
            }
        }
    }

    private fun verdict(stats: LabStats): String = when {
        !stats.running -> "Not running"
        stats.capturedPeakDb > SILENCE_DB -> "Capturing audio"
        stats.musicActive -> "Music is playing but capture is silent: this app blocks capture"
        else -> "Waiting for audio. Play something."
    }

    private fun report(stats: LabStats, log: List<String>): String = buildString {
        appendLine("Capture Lab 0.1.0")
        appendLine("${Build.MANUFACTURER} ${Build.MODEL}, Android ${Build.VERSION.RELEASE} (API ${Build.VERSION.SDK_INT})")
        appendLine(
            "Last: running=${stats.running} heardSignalEver=${stats.heardSignal} " +
                "latency=${stats.latencyMs}ms underruns=${stats.underruns} seconds=${stats.seconds} " +
                "route=${stats.routeApplied} error=${stats.error ?: "none"}",
        )
        log.forEach { appendLine(it) }
    }

    companion object {
        const val PREFS = "lab"
        const val KEY_MUTED = "music_muted"
        private const val SILENCE_DB = -80.0
    }
}
