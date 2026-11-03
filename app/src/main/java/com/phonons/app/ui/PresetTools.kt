package com.phonons.app.ui

import com.phonons.app.R
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
import com.phonons.app.data.Preset
import com.phonons.app.data.PresetOrganiser
import com.phonons.app.data.Processing
import com.phonons.app.data.Repo
import java.util.Locale
import kotlin.math.round

/** Renames a preset and files it in a folder. */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun PresetDetailsDialog(preset: Preset, folders: List<String>, onDismiss: () -> Unit) {
    var name by remember { mutableStateOf(preset.name) }
    var folder by remember { mutableStateOf(preset.folder.orEmpty()) }
    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = Palette.Tooltip,
        title = { Text(stringResource(R.string.ui_edit_preset)) },
        text = {
            Column(
                Modifier.verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                OutlinedTextField(value = name, onValueChange = { name = it }, singleLine = true, label = { Text(stringResource(R.string.ui_name)) })
                OutlinedTextField(value = folder, onValueChange = { folder = it }, singleLine = true, label = { Text(stringResource(R.string.ui_folder_optional)) })
                if (folders.isNotEmpty()) {
                    FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        folders.forEach { existing ->
                            FilterChip(
                                selected = folder.trim() == existing,
                                onClick = { folder = if (folder.trim() == existing) "" else existing },
                                label = { Text(existing, maxLines = 1) },
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = Palette.Lit,
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
                enabled = name.isNotBlank(),
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

    fun change(transform: (com.phonons.app.data.Tone) -> com.phonons.app.data.Tone) {
        tuned = transform(tuned)
        hearingBefore = false
        play(true)
    }

    fun db(value: Double) = String.format(Locale.US, "%+.1f dB", value)
    val range = -Processing.MAX_TONE_DB.toFloat()..Processing.MAX_TONE_DB.toFloat()

    AlertDialog(
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
                    }) { Text(if (hearingBefore) "Hear tuned" else "Hear before") }
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
