package io.github.dragjon.eigentone.ui

import io.github.dragjon.eigentone.data.HeadphoneTargets
import io.github.dragjon.eigentone.data.HeadphoneTarget
import io.github.dragjon.eigentone.data.BuiltIns
import androidx.compose.ui.Alignment
import io.github.dragjon.eigentone.R
import androidx.compose.ui.res.stringResource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import io.github.dragjon.eigentone.data.Preset
import io.github.dragjon.eigentone.data.PresetOrganiser
import io.github.dragjon.eigentone.data.Processing
import io.github.dragjon.eigentone.data.Repo
import java.util.Locale
import kotlin.math.round

/** Renames a preset and files it in a folder. */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun PresetDetailsDialog(preset: Preset, folders: List<String>, onDismiss: () -> Unit) {
    var name by remember { mutableStateOf(preset.name) }
    var folder by remember { mutableStateOf(preset.folder.orEmpty()) }
    MotionDialog(
        onDismissRequest = onDismiss,
        containerColor = Palette.Tooltip,
        title = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(stringResource(R.string.ui_edit_preset), modifier = Modifier.weight(1f))
                // A copy to try changes on, keeping the original as it is.
                val copyName = PresetOrganiser.freeName(Repo.state.value, stringResource(R.string.ui_preset_copy, preset.name))
                androidx.compose.material3.IconButton(onClick = {
                    Repo.duplicatePreset(preset.id, copyName)
                    onDismiss()
                }) {
                    androidx.compose.material3.Icon(
                        androidx.compose.ui.res.painterResource(R.drawable.ic_copy),
                        contentDescription = stringResource(R.string.ui_duplicate_preset_named, preset.name),
                        tint = Palette.Text,
                    )
                }
            }
        },
        text = {
            Column(
                Modifier.verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                val taken = io.github.dragjon.eigentone.data.PresetOrganiser.nameTaken(Repo.state.value, name, preset.id)
                OutlinedTextField(colors = fieldColors(), 
                    value = name,
                    onValueChange = { name = it },
                    singleLine = true,
                    label = { Text(stringResource(R.string.ui_name)) },
                    isError = taken,
                    supportingText = if (taken) ({ Text(stringResource(R.string.ui_name_taken, name.trim())) }) else null,
                )
                OutlinedTextField(colors = fieldColors(), value = folder, onValueChange = { folder = it }, singleLine = true, label = { Text(stringResource(R.string.ui_folder_optional)) })
                if (folders.isNotEmpty()) {
                    FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        folders.forEach { existing ->
                            FilterChip(
                                selected = folder.trim() == existing,
                                onClick = { folder = if (folder.trim() == existing) "" else existing },
                                label = { Text(existing, maxLines = 1) },
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = Palette.LitFill,
                                    selectedLabelColor = Palette.OnLit,
                                    labelColor = Palette.Text,
                                ),
                            )
                        }
                    }
                }

            }
        },
        confirmButton = {
            TextButton(
                enabled = name.isNotBlank() && !io.github.dragjon.eigentone.data.PresetOrganiser.nameTaken(Repo.state.value, name, preset.id),
                onClick = {
                    Repo.renamePreset(preset.id, name.trim())
                    Repo.moveToFolder(preset.id, folder)
                    onDismiss()
                },
            ) { Text(stringResource(R.string.ui_save)) }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text(stringResource(R.string.ui_cancel)) } },
    )
}

/**
 * Adjusts a correction to taste: how much of it to use, and how much bass and treble to add back. It
 * changes the live sound while open, and puts everything back unless kept.
 */
@OptIn(androidx.compose.foundation.layout.ExperimentalLayoutApi::class)
@Composable
fun CorrectionTuner(onDismiss: () -> Unit) {
    val state by Repo.state.collectAsStateWithLifecycle()
    val before = remember { Repo.state.value.current }
    var kept by remember { mutableStateOf(false) }
    var tuned by remember { mutableStateOf(before.tone) }
    var hearingBefore by remember { mutableStateOf(false) }

    DisposableEffect(Unit) {
        Repo.comparing.value = true
        onDispose {
            Repo.comparing.value = false
            if (!kept) Repo.edit { before }
        }
    }
    fun play(useTuned: Boolean) = Repo.edit { if (useTuned) before.copy(tone = tuned) else before }

    fun change(transform: (io.github.dragjon.eigentone.data.Tone) -> io.github.dragjon.eigentone.data.Tone) {
        tuned = transform(tuned)
        hearingBefore = false
        play(true)
    }

    fun db(value: Double) = String.format(Locale.US, "%+.1f dB", value)
    val range = -Processing.MAX_TONE_DB.toFloat()..Processing.MAX_TONE_DB.toFloat()

    MotionDialog(
        onDismissRequest = onDismiss,
        containerColor = Palette.Tooltip,
        title = { Text(stringResource(R.string.ui_tune_correction)) },
        text = {
            Column(Modifier.verticalScroll(rememberScrollState())) {
                Text(
                    stringResource(R.string.ui_a_correction_aims_for_a_neutral_sound_wh),
                    style = MaterialTheme.typography.bodyMedium,
                    color = Palette.TextDim,
                    modifier = Modifier.padding(bottom = 8.dp),
                )
                SliderSetting(
                    title = stringResource(R.string.ui_amount),
                    tip = Help.correction,
                    readout = "${round(tuned.correction * 100).toInt()}%",
                    value = tuned.correction.toFloat(),
                    range = 0f..1f,
                    default = 1f,
                    onChange = { v -> change { it.copy(correction = round(v * 20.0) / 20.0) } },
                )
                val info = before.tone.correctionInfo ?: BuiltIns.correctionInfo(before.id)
                if (info != null) {
                    val chosen = HeadphoneTarget.of(tuned.targetId)
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(stringResource(R.string.ui_target), style = MaterialTheme.typography.bodyLarge, color = Palette.Text)
                        InfoButton(Help.target)
                    }
                    androidx.compose.foundation.layout.FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        (listOf<HeadphoneTarget?>(null) + HeadphoneTarget.entries).forEach { option ->
                            FilterChip(
                                selected = chosen == option,
                                onClick = { change { it.copy(targetId = option?.id, correctionInfo = info) } },
                                label = { Text(stringResource(targetName(option)), maxLines = 1) },
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = Palette.LitFill,
                                    selectedLabelColor = Palette.OnLit,
                                    labelColor = Palette.Text,
                                ),
                            )
                        }
                    }
                    val below = kotlin.math.round(HeadphoneTargets.bassBelowHarman(info)).toInt()
                    Text(
                        if (chosen == null && below >= NOTABLE_BASS_GAP_DB) {
                            stringResource(R.string.ui_target_published_below_harman, below)
                        } else {
                            stringResource(targetDetail(chosen))
                        },
                        style = MaterialTheme.typography.bodySmall,
                        color = Palette.TextDim,
                        modifier = Modifier.padding(bottom = 8.dp),
                    )
                }
                SliderSetting(
                    title = stringResource(R.string.ui_bass),
                    tip = Help.tunerBass,
                    readout = db(tuned.bassDb),
                    value = tuned.bassDb.toFloat(),
                    range = range,
                    default = 0f,
                    origin = 0f,
                    onChange = { v -> change { it.copy(bassDb = round(v * 2.0) / 2.0) } },
                )
                SliderSetting(
                    title = stringResource(R.string.ui_treble),
                    tip = Help.tunerTreble,
                    readout = db(tuned.trebleDb),
                    value = tuned.trebleDb.toFloat(),
                    range = range,
                    default = 0f,
                    origin = 0f,
                    onChange = { v -> change { it.copy(trebleDb = round(v * 2.0) / 2.0) } },
                )
                if (!state.enabled || state.bypass) {
                    Text(
                        stringResource(R.string.ui_you_won_t_hear_this_while_eigentone_is_o),
                        style = MaterialTheme.typography.bodySmall,
                        color = Palette.TextDim,
                    )
                } else {
                    NothingPlayingHint()
                }
                Row(Modifier.fillMaxWidth()) {
                    TextButton(onClick = {
                        hearingBefore = !hearingBefore
                        play(!hearingBefore)
                    }) { Text(if (hearingBefore) stringResource(R.string.ui_hear_tuned) else stringResource(R.string.ui_hear_before)) }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = {
                kept = true
                Repo.edit { before.copy(tone = tuned) }
                Repo.commit()
                onDismiss()
            }) { Text(stringResource(R.string.ui_keep)) }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text(stringResource(R.string.ui_cancel)) } },
    )
}

private const val NOTABLE_BASS_GAP_DB = 2

private fun targetName(target: HeadphoneTarget?): Int = when (target) {
    null -> R.string.ui_target_published
    HeadphoneTarget.HARMAN -> R.string.ui_target_harman
    HeadphoneTarget.LESS_BASS -> R.string.ui_target_less_bass
    HeadphoneTarget.MORE_BASS -> R.string.ui_target_more_bass
    HeadphoneTarget.NO_BASS_LIFT -> R.string.ui_target_no_bass_lift
    HeadphoneTarget.DIFFUSE_FIELD -> R.string.ui_target_diffuse
}

private fun targetDetail(target: HeadphoneTarget?): Int = when (target) {
    null -> R.string.ui_target_published_detail
    HeadphoneTarget.HARMAN -> R.string.ui_target_harman_detail
    HeadphoneTarget.LESS_BASS -> R.string.ui_target_less_bass_detail
    HeadphoneTarget.MORE_BASS -> R.string.ui_target_more_bass_detail
    HeadphoneTarget.NO_BASS_LIFT -> R.string.ui_target_no_bass_lift_detail
    HeadphoneTarget.DIFFUSE_FIELD -> R.string.ui_target_diffuse_detail
}
