package com.phonons.app.ui

import com.phonons.app.R
import androidx.compose.ui.res.stringResource
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
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
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
import com.phonons.app.data.HeadphoneTargets
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
            .clip(PanelShape)
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
        targetPrompt(output.key) !in state.dismissedPrompts &&
            HeadphoneTargets.worthSuggesting(state.current.tone, state.current.id) -> TargetPrompt(output)
        output.key.startsWith("Bluetooth") && EARBUD_EQ_PROMPT !in state.dismissedPrompts -> {
            PromptCard(
                stringResource(R.string.ui_your_headphones_may_have_their_own_eq),
                stringResource(R.string.ui_eq_in_a_headphone_app_such),
            ) {
                Button(onClick = { Repo.dismissPrompt(EARBUD_EQ_PROMPT) }) { Text(stringResource(R.string.ui_got_it)) }
            }
        }
    }
}

/** Corrections are applied as published, so this says when that leaves far less bass than most people prefer. */
@Composable
private fun TargetPrompt(output: OutputInfo) {
    var tuning by remember { mutableStateOf(false) }
    PromptCard(stringResource(R.string.ui_target_prompt_title), stringResource(R.string.ui_target_prompt_body)) {
        Button(onClick = { tuning = true }) { Text(stringResource(R.string.ui_target_prompt_action)) }
        TextButton(onClick = { Repo.dismissPrompt(targetPrompt(output.key)) }) { Text(stringResource(R.string.ui_not_now)) }
    }
    if (tuning) CorrectionTuner(onDismiss = { tuning = false })
}

private fun targetPrompt(outputKey: String) = "target:$outputKey"

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
                    Triple(entry, parsed, AutoEqCatalog.loadCorrectionInfo(entry))
                }
            }
            busy = false
            found.fold(
                onSuccess = { match ->
                    if (match == null) {
                        searching = true
                    } else {
                        Repo.importPreset(match.first.title, match.second, match.third)
                        Repo.assignToOutput(output.key, Repo.state.value.current.id)
                    }
                },
                onFailure = { problem = appText(R.string.ui_couldn_t_get_the_headphone_list) },
            )
        }
    }

    PromptCard(
        stringResource(R.string.ui_correct, output.label),
        problem ?: stringResource(R.string.ui_a_correction_evens_out_these_headphones),
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
        ) { Text(if (busy) stringResource(R.string.ui_looking) else stringResource(R.string.ui_find_a_correction)) }
        TextButton(enabled = !busy, onClick = { Repo.dismissPrompt(correctionPrompt(output.key)) }) { Text(stringResource(R.string.ui_not_now)) }
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
    ActionRow(stringResource(R.string.ui_share_this_sound), Help.share) {
        val preset = Repo.state.value.current
        val send = Intent(Intent.ACTION_SEND).setType("text/plain")
            .putExtra(Intent.EXTRA_SUBJECT, "${preset.name} for Eigentone")
            .putExtra(Intent.EXTRA_TEXT, ShareCode.encode(preset))
        context.startActivity(Intent.createChooser(send, appText(R.string.ui_share, preset.name)))
    }
    RowDivider()
    ActionRow(stringResource(R.string.ui_add_a_shared_sound), Help.addShared) { pasting = true }
    if (pasting) {
        var code by remember { mutableStateOf("") }
        var failed by remember { mutableStateOf(false) }
        AlertDialog(
            onDismissRequest = { pasting = false },
            containerColor = Palette.Tooltip,
            title = { Text(stringResource(R.string.ui_add_a_shared_sound)) },
            text = {
                OutlinedTextField(
                    value = code,
                    onValueChange = {
                        code = it
                        failed = false
                    },
                    label = { Text(stringResource(R.string.ui_share_code)) },
                    isError = failed,
                    supportingText = if (failed) ({ Text(stringResource(R.string.ui_that_isn_t_a_valid_share_code)) }) else null,
                    maxLines = 5,
                )
            },
            confirmButton = {
                TextButton(enabled = code.isNotBlank(), onClick = {
                    if (Repo.importShareCode(code)) {
                        pasting = false
                        snack(appText(R.string.ui_added, Repo.state.value.current.name))
                    } else {
                        failed = true
                    }
                }) { Text(stringResource(R.string.ui_add)) }
            },
            dismissButton = { TextButton(onClick = { pasting = false }) { Text(stringResource(R.string.ui_cancel)) } },
        )
    }
}

fun appLabel(context: Context, packageName: String): String = runCatching {
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

    SettingsSection(stringResource(R.string.ui_per_app_presets), trailing = { InfoButton(Help.perApp) }) {
        if (!allowed) {
            ActionRow(
                stringResource(R.string.ui_allow_per_app_presets),
                Help.perAppAccess,
                detail = stringResource(R.string.ui_android_lists_this_under_notification_ac),
            ) { context.startActivity(Intent(Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS)) }
            return@SettingsSection
        }
        val app = playing
        val canAssign = Repo.findPreset(state.current.id) != null
        Column(Modifier.padding(start = 16.dp, end = 16.dp, top = 12.dp, bottom = 12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(
                if (app == null) stringResource(R.string.ui_play_something_to_set_up_its) else stringResource(R.string.ui_last_playing, appLabel(context, app)),
                style = MaterialTheme.typography.bodyMedium,
                color = Palette.Text,
            )
            if (app != null) {
                FilledTonalButton(
                    enabled = canAssign && state.appProfiles[app] != state.current.id,
                    onClick = { Repo.assignToApp(app, state.current.id) },
                ) { Text(stringResource(R.string.ui_use_for_this_app, state.current.name), maxLines = 1, overflow = TextOverflow.Ellipsis) }
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
                        Repo.findPreset(presetId)?.name ?: stringResource(R.string.ui_preset_was_deleted),
                        style = MaterialTheme.typography.bodySmall,
                        color = Palette.TextDim,
                    )
                }
                TextButton(onClick = { Repo.assignToApp(packageName, null) }) { Text(stringResource(R.string.ui_clear)) }
            }
        }
    }
}

/** What to send when something goes wrong, and anything else that might be interfering. */
@Composable
fun DiagnosticsSection(status: EngineStatus, output: OutputInfo, onConsole: () -> Unit) {
    val context = LocalContext.current
    var crash by remember { mutableStateOf(CrashLog.read(context)) }
    val rivals = remember { otherEffectApps(context) }
    var copied by remember { mutableStateOf(false) }

    SettingsSection(stringResource(R.string.ui_troubleshooting)) {
        if (!status.hasControl && rivals.isNotEmpty()) {
            Text(
                stringResource(R.string.ui_another_audio_app_has_control_try, rivals.joinToString()),
                style = MaterialTheme.typography.bodySmall,
                color = Palette.Danger,
                modifier = Modifier.padding(16.dp),
            )
            RowDivider()
        }
        ActionRow(
            if (copied) stringResource(R.string.ui_copied) else stringResource(R.string.ui_copy_status_details),
            Help.diagnostics,
        ) {
            val text = buildList {
                add(CrashLog.deviceSummary(context))
                add("Running: ${status.running}, mode: ${status.mode}, bands: ${status.bandCount}, accepted: ${status.readback ?: "not checked"}")
                add("In control: ${status.hasControl}, repairs: ${status.rebuilds}")
                add("Output: ${output.key}")
                status.error?.let { add("Error: $it") }
                status.spatialError?.let { add("Surround: $it") }
                if (rivals.isNotEmpty()) add(appText(R.string.ui_other_audio_effect_apps, rivals.joinToString()))
            }.joinToString("\n")
            context.getSystemService(ClipboardManager::class.java)
                .setPrimaryClip(ClipData.newPlainText("Eigentone status", text))
            copied = true
        }
        RowDivider()
        ActionRow(stringResource(R.string.ui_debug_console), Help.console, detail = stringResource(R.string.ui_debug_console_detail), onClick = onConsole)
        crash?.let { report ->
            RowDivider()
            ActionRow(stringResource(R.string.ui_share_crash_report), Help.crashReport, detail = stringResource(R.string.ui_eigentone_crashed_recently_nothing_is_se)) {
                val send = Intent(Intent.ACTION_SEND).setType("text/plain")
                    .putExtra(Intent.EXTRA_SUBJECT, "Eigentone crash report")
                    .putExtra(Intent.EXTRA_TEXT, report)
                context.startActivity(Intent.createChooser(send, appText(R.string.ui_share_crash_report)))
            }
            RowDivider()
            ActionRow(stringResource(R.string.ui_delete_crash_report), Help.crashReport) {
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
