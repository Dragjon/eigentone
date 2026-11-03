package com.phonons.app.ui

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.provider.Settings
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.LifecycleResumeEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.phonons.app.data.AppState
import com.phonons.app.data.AutoEqCatalog
import com.phonons.app.data.BuiltIns
import com.phonons.app.data.CrashLog
import com.phonons.app.data.EngineStatus
import com.phonons.app.data.OutputInfo
import com.phonons.app.data.Profiles
import com.phonons.app.data.Repo
import com.phonons.app.data.ShareCode
import com.phonons.app.dsp.AutoEqParser
import com.phonons.app.engine.MediaWatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

private const val EARBUD_EQ_PROMPT = "earbud-eq"
private fun correctionPrompt(outputKey: String) = "correction:$outputKey"

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun PromptCard(title: String, body: String, actions: @Composable () -> Unit) {
    Column(
        Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(Palette.SurfaceHigh)
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Text(title, style = MaterialTheme.typography.titleMedium, color = Palette.Text)
        Text(body, style = MaterialTheme.typography.bodyMedium, color = Palette.TextDim)
        FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) { actions() }
    }
}

/**
 * Offers help that depends on what's connected, one card at a time: a correction for these headphones,
 * then a reminder about EQ built into the headphones themselves.
 */
@Composable
fun OutputPrompts(state: AppState) {
    val output by Repo.output.collectAsStateWithLifecycle()
    if (!state.enabled || !Profiles.supportsModes(output.key)) return
    val needsCorrection = correctionPrompt(output.key) !in state.dismissedPrompts &&
        Profiles.presetIdFor(state, output.key) == null &&
        !state.current.filters.any { it.gainDb != 0.0 } && state.current.tone.importedCurve.isEmpty()
    when {
        needsCorrection -> CorrectionPrompt(output)
        output.key.startsWith("Bluetooth") && EARBUD_EQ_PROMPT !in state.dismissedPrompts -> {
            PromptCard(
                "Your headphones may have their own EQ",
                "EQ in a headphone app, such as soundcore, Sony or Galaxy Wearable, adds to this one, so changes can apply twice. For the clearest results, set it to flat or default, and turn off any bass boost.",
            ) {
                Button(onClick = { Repo.dismissPrompt(EARBUD_EQ_PROMPT) }) { Text("Got it") }
            }
        }
    }
}

@Composable
private fun CorrectionPrompt(output: OutputInfo) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var busy by remember { mutableStateOf(false) }
    var problem by remember { mutableStateOf<String?>(null) }
    var searching by remember { mutableStateOf(false) }
    val builtIn = BuiltIns.correctionFor(output.key)

    fun useCorrection() {
        busy = true
        problem = null
        scope.launch {
            val found = withContext(Dispatchers.IO) {
                runCatching {
                    val entries = AutoEqCatalog.loadIndex(context.cacheDir)
                    val entry = AutoEqCatalog.bestMatch(entries, output.label) ?: return@runCatching null
                    val parsed = AutoEqParser.parse(AutoEqCatalog.loadProfile(entry)) ?: return@runCatching null
                    entry to parsed
                }
            }
            busy = false
            found.fold(
                onSuccess = { match ->
                    if (match == null) {
                        searching = true
                    } else {
                        Repo.importPreset(match.first.title, match.second)
                        Repo.assignToOutput(output.key, Repo.state.value.current.id)
                    }
                },
                onFailure = { problem = "Couldn't get the headphone list. Check your connection and try again." },
            )
        }
    }

    PromptCard(
        "Correct ${output.label}?",
        problem ?: "A correction evens out these headphones' sound, using published measurements. You can undo it or turn it down at any time.",
    ) {
        Button(
            enabled = !busy,
            onClick = {
                if (builtIn != null) {
                    Repo.loadPreset(builtIn)
                    Repo.assignToOutput(output.key, builtIn.id)
                } else {
                    useCorrection()
                }
            },
        ) { Text(if (busy) "Looking…" else "Find a correction") }
        TextButton(enabled = !busy, onClick = { Repo.dismissPrompt(correctionPrompt(output.key)) }) { Text("Not now") }
    }
    if (searching) {
        AutoEqDialog(onDismiss = { searching = false }) {
            searching = false
            Repo.assignToOutput(output.key, Repo.state.value.current.id)
        }
    }
}

/** Sharing, and adding a sound someone else shared. */
@Composable
fun ShareRows(snack: (String) -> Unit) {
    val context = LocalContext.current
    var pasting by remember { mutableStateOf(false) }
    RowDivider()
    ActionRow("Share this sound", Help.share) {
        val preset = Repo.state.value.current
        val send = Intent(Intent.ACTION_SEND).setType("text/plain")
            .putExtra(Intent.EXTRA_SUBJECT, "${preset.name} for Eigentone")
            .putExtra(Intent.EXTRA_TEXT, ShareCode.encode(preset))
        context.startActivity(Intent.createChooser(send, "Share ${preset.name}"))
    }
    RowDivider()
    ActionRow("Add a shared sound", Help.addShared) { pasting = true }
    if (pasting) {
        var code by remember { mutableStateOf("") }
        var failed by remember { mutableStateOf(false) }
        AlertDialog(
            onDismissRequest = { pasting = false },
            containerColor = Palette.Tooltip,
            title = { Text("Add a shared sound") },
            text = {
                OutlinedTextField(
                    value = code,
                    onValueChange = {
                        code = it
                        failed = false
                    },
                    label = { Text("Share code") },
                    isError = failed,
                    supportingText = if (failed) ({ Text("That isn't a valid share code.") }) else null,
                    maxLines = 5,
                )
            },
            confirmButton = {
                TextButton(enabled = code.isNotBlank(), onClick = {
                    if (Repo.importShareCode(code)) {
                        pasting = false
                        snack("Added ${Repo.state.value.current.name}.")
                    } else {
                        failed = true
                    }
                }) { Text("Add") }
            },
            dismissButton = { TextButton(onClick = { pasting = false }) { Text("Cancel") } },
        )
    }
}

private fun appLabel(context: Context, packageName: String): String = runCatching {
    val manager = context.packageManager
    manager.getApplicationLabel(manager.getApplicationInfo(packageName, 0)).toString()
}.getOrDefault(packageName)

/** Lets each music or video app have its own preset. Needs access that Android treats as sensitive. */
@Composable
fun PerAppSection(state: AppState) {
    val context = LocalContext.current
    var allowed by remember { mutableStateOf(MediaWatcher.isAllowed(context)) }
    LifecycleResumeEffect(Unit) {
        allowed = MediaWatcher.isAllowed(context)
        onPauseOrDispose { }
    }
    val playing by Repo.playingApp.collectAsStateWithLifecycle()

    SettingsSection("Per-app presets", trailing = { InfoButton(Help.perApp) }) {
        if (!allowed) {
            ActionRow(
                "Allow per-app presets",
                Help.perAppAccess,
                detail = "Android lists this under notification access. Eigentone only checks which app is playing.",
            ) { context.startActivity(Intent(Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS)) }
            return@SettingsSection
        }
        val app = playing
        val canAssign = Repo.findPreset(state.current.id) != null
        Column(Modifier.padding(start = 16.dp, end = 16.dp, top = 12.dp, bottom = 12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(
                if (app == null) "Play something to set up its preset." else "Last playing: ${appLabel(context, app)}",
                style = MaterialTheme.typography.bodyMedium,
                color = Palette.Text,
            )
            if (app != null) {
                FilledTonalButton(
                    enabled = canAssign && state.appProfiles[app] != state.current.id,
                    onClick = { Repo.assignToApp(app, state.current.id) },
                ) { Text("Use ${state.current.name} for this app", maxLines = 1, overflow = TextOverflow.Ellipsis) }
            }
        }
        state.appProfiles.forEach { (packageName, presetId) ->
            RowDivider()
            Row(
                Modifier
                    .fillMaxWidth()
                    .heightIn(min = 56.dp)
                    .padding(start = 16.dp, end = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Column(Modifier.weight(1f)) {
                    Text(appLabel(context, packageName), style = MaterialTheme.typography.bodyLarge, color = Palette.Text)
                    Text(
                        Repo.findPreset(presetId)?.name ?: "Preset was deleted",
                        style = MaterialTheme.typography.bodySmall,
                        color = Palette.TextDim,
                    )
                }
                TextButton(onClick = { Repo.assignToApp(packageName, null) }) { Text("Clear") }
            }
        }
    }
}

/** What to send when something goes wrong, and anything else that might be interfering. */
@Composable
fun DiagnosticsSection(status: EngineStatus, output: OutputInfo) {
    val context = LocalContext.current
    var crash by remember { mutableStateOf(CrashLog.read(context)) }
    val rivals = remember { otherEffectApps(context) }
    var copied by remember { mutableStateOf(false) }

    SettingsSection("Troubleshooting") {
        if (!status.hasControl && rivals.isNotEmpty()) {
            Text(
                "Another audio app has control. Try closing or turning off: ${rivals.joinToString()}.",
                style = MaterialTheme.typography.bodySmall,
                color = Palette.Danger,
                modifier = Modifier.padding(16.dp),
            )
            RowDivider()
        }
        ActionRow(
            if (copied) "Copied" else "Copy status details",
            Help.diagnostics,
        ) {
            val text = buildList {
                add(CrashLog.deviceSummary(context))
                add("Running: ${status.running}, mode: ${status.mode}, bands: ${status.bandCount}, accepted: ${status.readback ?: "not checked"}")
                add("In control: ${status.hasControl}, repairs: ${status.rebuilds}")
                add("Output: ${output.key}")
                status.error?.let { add("Error: $it") }
                status.spatialError?.let { add("Surround: $it") }
                if (rivals.isNotEmpty()) add("Other audio effect apps: ${rivals.joinToString()}")
            }.joinToString("\n")
            context.getSystemService(ClipboardManager::class.java)
                .setPrimaryClip(ClipData.newPlainText("Eigentone status", text))
            copied = true
        }
        crash?.let { report ->
            RowDivider()
            ActionRow("Share crash report", Help.crashReport, detail = "Eigentone crashed recently. Nothing is sent unless you choose to.") {
                val send = Intent(Intent.ACTION_SEND).setType("text/plain")
                    .putExtra(Intent.EXTRA_SUBJECT, "Eigentone crash report")
                    .putExtra(Intent.EXTRA_TEXT, report)
                context.startActivity(Intent.createChooser(send, "Share crash report"))
            }
            RowDivider()
            ActionRow("Delete crash report", Help.crashReport) {
                CrashLog.clear(context)
                crash = null
            }
        }
    }
}

private fun otherEffectApps(context: Context): List<String> {
    val manager = context.packageManager
    val panels = manager.queryIntentActivities(
        Intent("android.media.action.DISPLAY_AUDIO_EFFECT_CONTROL_PANEL"),
        PackageManager.MATCH_ALL,
    )
    return panels.map { it.activityInfo.packageName }
        .distinct()
        .filter { it != context.packageName }
        .map { appLabel(context, it) }
}
