package io.github.dragjon.eigentone.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import io.github.dragjon.eigentone.R
import io.github.dragjon.eigentone.data.AutoEqCatalog
import io.github.dragjon.eigentone.data.CatalogEntry
import io.github.dragjon.eigentone.data.Preset
import io.github.dragjon.eigentone.data.Repo
import io.github.dragjon.eigentone.data.CorrectionInfo
import io.github.dragjon.eigentone.data.SoundFinder
import io.github.dragjon.eigentone.dsp.AutoEqParser
import io.github.dragjon.eigentone.dsp.ParsedEq
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

private const val WHAT_I_HAD = -1

/**
 * Different reviewers measure the same headphones differently, and the results can sound quite different.
 * This plays each at about the same loudness as the current sound, and only keeps the one chosen.
 */
@Composable
fun MeasurementCompare(options: List<CatalogEntry>, onDismiss: () -> Unit, onChosen: (String) -> Unit) {
    val before = remember { Repo.state.value }
    var loaded by remember { mutableStateOf<List<Pair<CatalogEntry, ParsedEq>>?>(null) }
    var infos by remember { mutableStateOf<Map<CatalogEntry, CorrectionInfo?>>(emptyMap()) }
    var selected by remember { mutableIntStateOf(WHAT_I_HAD) }
    var kept by remember { mutableStateOf(false) }

    DisposableEffect(Unit) {
        Repo.comparing.value = true
        onDispose {
            Repo.comparing.value = false
            if (!kept) Repo.edit { before.current }
        }
    }

    LaunchedEffect(options) {
        val profiles = withContext(Dispatchers.IO) {
            options.mapNotNull { entry ->
                runCatching { AutoEqCatalog.loadProfile(entry) }.getOrNull()
                    ?.let(AutoEqParser::parse)
                    ?.let { entry to it }
            }
        }
        infos = withContext(Dispatchers.IO) { profiles.associate { (entry, _) -> entry to AutoEqCatalog.loadCorrectionInfo(entry) } }
        loaded = profiles
        if (profiles.isNotEmpty()) selected = 0
    }

    LaunchedEffect(selected, loaded) {
        val profiles = loaded ?: return@LaunchedEffect
        val choice = profiles.getOrNull(selected)
        Repo.edit {
            if (choice == null) {
                before.current
            } else {
                val preview = Preset("preview:$selected", choice.first.title, choice.second.preampDb, choice.second.filters)
                SoundFinder.levelMatched(preview, preview.tone, before.current, before.autoHeadroom)
            }
        }
    }

    MotionDialog(
        onDismissRequest = onDismiss,
        containerColor = Palette.Tooltip,
        title = { Text(stringResource(R.string.ui_compare_measurements)) },
        text = {
            Column(Modifier.verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                val profiles = loaded
                when {
                    profiles == null -> Text(stringResource(R.string.ui_downloading), color = Palette.TextDim)
                    profiles.isEmpty() -> Text(stringResource(R.string.ui_could_not_download_measurements), color = Palette.Danger)
                    else -> {
                        Text(
                            stringResource(R.string.ui_compare_measurements_intro),
                            style = MaterialTheme.typography.bodyMedium,
                            color = Palette.TextDim,
                        )
                        NothingPlayingHint()
                        val labels = AutoEqCatalog.measurementLabels(profiles.map { it.first })
                        profiles.forEachIndexed { index, _ ->
                            Option(labels[index], selected == index) { selected = index }
                        }
                        Option(stringResource(R.string.ui_what_i_had), selected == WHAT_I_HAD) { selected = WHAT_I_HAD }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(
                enabled = loaded != null,
                onClick = {
                    val choice = loaded?.getOrNull(selected)
                    if (choice == null) {
                        onDismiss()
                    } else {
                        // Back to the real sound first, so undo returns there rather than to a preview.
                        Repo.edit { before.current }
                        kept = true
                        Repo.importPreset(choice.first.title, choice.second, infos[choice.first])
                        onChosen(appText(R.string.ui_added, choice.first.title))
                    }
                },
            ) { Text(stringResource(R.string.ui_keep)) }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text(stringResource(R.string.ui_cancel)) } },
    )
}

@Composable
private fun Option(name: String, selected: Boolean, onClick: () -> Unit) {
    Row(
        Modifier
            .fillMaxWidth()
            .heightIn(min = 48.dp)
            .selectable(selected = selected, role = Role.RadioButton, onClick = onClick),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        RadioButton(selected = selected, onClick = null, modifier = Modifier.padding(8.dp))
        Text(name, style = MaterialTheme.typography.bodyLarge, color = Palette.Text)
    }
}
