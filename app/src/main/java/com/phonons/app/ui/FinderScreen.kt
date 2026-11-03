package com.phonons.app.ui

import com.phonons.app.R
import androidx.compose.ui.res.stringResource
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.phonons.app.data.AppState
import com.phonons.app.data.BuiltIns
import com.phonons.app.data.Choice
import com.phonons.app.data.Preset
import com.phonons.app.data.Profiles
import com.phonons.app.data.Repo
import com.phonons.app.data.SoundFinder
import com.phonons.app.data.Trait
import java.util.Locale

private enum class FinderStep { INTRO, COMPARE, FINAL, RESULT }

/**
 * Guided listening test. It changes the live sound while it runs, and puts everything back exactly
 * as it was unless the result is saved.
 */
@Composable
fun FinderScreen(onSetEnabled: (Boolean) -> Unit, onClose: () -> Unit) {
    val state by Repo.state.collectAsStateWithLifecycle()
    val output by Repo.output.collectAsStateWithLifecycle()
    // Captured once, so leaving the test can restore precisely what was there before.
    val before = remember { Repo.state.value }
    var step by remember { mutableStateOf(FinderStep.INTRO) }
    var finder by remember { mutableStateOf<SoundFinder?>(null) }
    var start by remember { mutableStateOf(before.current) }
    var finding by remember { mutableStateOf(false) }
    var progress by remember { mutableIntStateOf(0) }

    fun cancel() {
        Repo.update { it.copy(current = before.current, bypass = before.bypass) }
        if (!before.enabled) onSetEnabled(false)
        Repo.update { it.copy(guidedSetupSeen = true) }
        onClose()
    }

    fun begin(from: Preset) {
        if (!state.enabled) onSetEnabled(true)
        Repo.update { it.copy(bypass = false, guidedSetupSeen = true, current = from) }
        start = from
        finder = SoundFinder(from.tone)
        progress = 0
        step = FinderStep.COMPARE
    }

    BackHandler { cancel() }

    Column(
        Modifier
            .fillMaxSize()
            .background(Palette.Background)
            .windowInsetsPadding(WindowInsets.statusBars)
            .windowInsetsPadding(WindowInsets.navigationBars)
            .imePadding(),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Column(
            Modifier
                .widthIn(max = 560.dp)
                .fillMaxWidth()
                .weight(1f)
                .verticalScroll(rememberScrollState())
                .padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            when (step) {
                FinderStep.INTRO -> Intro(
                    outputLabel = output.label,
                    // Read directly, because the service's output isn't known while Phonons is off.
                    correction = rememberConnectedOutputNames().firstNotNullOfOrNull(BuiltIns::correctionFor),
                    current = before.current,
                    onStart = ::begin,
                    onFind = { finding = true },
                    onCancel = ::cancel,
                )
                FinderStep.COMPARE -> finder?.let { active ->
                    Compare(
                        finder = active,
                        progress = progress,
                        start = start,
                        onAnswered = {
                            progress++
                            if (active.isDone) step = FinderStep.FINAL
                        },
                        onCancel = ::cancel,
                    )
                }
                FinderStep.FINAL -> finder?.let { active ->
                    FinalChoice(
                        previous = before.current,
                        result = start.copy(tone = active.result()),
                        autoHeadroom = state.autoHeadroom,
                        onKeepPrevious = ::cancel,
                        onUseNew = {
                            Repo.edit { start.copy(tone = active.result()) }
                            step = FinderStep.RESULT
                        },
                    )
                }
                FinderStep.RESULT -> finder?.let { active ->
                    Result(
                        finder = active,
                        state = state,
                        start = start,
                        outputKey = output.key,
                        onDone = {
                            Repo.commit()
                            onClose()
                        },
                        onDiscard = ::cancel,
                    )
                }
            }
        }
    }

    if (finding) {
        AutoEqDialog(onDismiss = { finding = false }) {
            finding = false
            begin(Repo.state.value.current)
        }
    }
}

@Composable
private fun Intro(
    outputLabel: String,
    correction: Preset?,
    current: Preset,
    onStart: (Preset) -> Unit,
    onFind: () -> Unit,
    onCancel: () -> Unit,
) {
    Text(stringResource(R.string.ui_find_my_sound), style = MaterialTheme.typography.headlineSmall, color = Palette.Text)
    Text(
        stringResource(R.string.ui_you_ll_compare_two_versions_of_your_musi),
        style = MaterialTheme.typography.bodyLarge,
        color = Palette.TextDim,
    )
    Column(
        Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(Palette.Surface)
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Text(stringResource(R.string.ui_before_you_start), style = MaterialTheme.typography.titleSmall, color = Palette.Text)
        Text(stringResource(R.string.ui_play_a_song_you_know_well_and_keep_it_pl), style = MaterialTheme.typography.bodyMedium, color = Palette.Text)
        Text(stringResource(R.string.ui_set_a_comfortable_volume_and_leave_it_th), style = MaterialTheme.typography.bodyMedium, color = Palette.Text)
        Text("• Listening on: $outputLabel", style = MaterialTheme.typography.bodyMedium, color = Palette.Text)
    }
    Text(stringResource(R.string.ui_start_from), style = MaterialTheme.typography.titleSmall, color = Palette.TextDim)
    // A sound the listener has already shaped is a better base than a fresh correction it may replace.
    val hasOwnSound = current.filters.any { it.gainDb != 0.0 } || current.tone != com.phonons.app.data.Tone()
    if (hasOwnSound) {
        Button(onClick = { onStart(current) }, modifier = Modifier.fillMaxWidth()) {
            Text("My current sound: ${current.name} (recommended)")
        }
    }
    if (correction != null) {
        val recommended = if (hasOwnSound) "" else " (recommended)"
        if (hasOwnSound) {
            FilledTonalButton(onClick = { onStart(correction) }, modifier = Modifier.fillMaxWidth()) {
                Text("${correction.name} correction")
            }
        } else {
            Button(onClick = { onStart(correction) }, modifier = Modifier.fillMaxWidth()) {
                Text("${correction.name} correction$recommended")
            }
        }
    } else if (hasOwnSound) {
        FilledTonalButton(onClick = onFind, modifier = Modifier.fillMaxWidth()) { Text(stringResource(R.string.ui_find_my_headphones_first)) }
    } else {
        Button(onClick = onFind, modifier = Modifier.fillMaxWidth()) { Text(stringResource(R.string.ui_find_my_headphones_first_recommended)) }
    }
    if (!hasOwnSound) {
        FilledTonalButton(onClick = { onStart(current) }, modifier = Modifier.fillMaxWidth()) {
            Text("My current sound: ${current.name}")
        }
    }
    Text(
        stringResource(R.string.ui_a_headphone_correction_evens_out_your_mo),
        style = MaterialTheme.typography.bodySmall,
        color = Palette.TextDim,
    )
    TextButton(onClick = onCancel, modifier = Modifier.heightIn(min = 48.dp)) { Text(stringResource(R.string.ui_not_now)) }
}

@Composable
private fun Compare(
    finder: SoundFinder,
    progress: Int,
    start: Preset,
    onAnswered: () -> Unit,
    onCancel: () -> Unit,
) {
    var side by remember(progress) { mutableStateOf<Choice?>(null) }
    val round = finder.current ?: return

    fun play(choice: Choice) {
        side = choice
        // Level matched, so the choice is about tone rather than which one is louder.
        val matched = SoundFinder.levelMatched(start, finder.preview(choice), start)
        Repo.edit { matched }
    }

    // Each new question starts by playing A, so what's heard always matches what's highlighted.
    LaunchedEffect(progress) { play(Choice.A) }

    Row(verticalAlignment = Alignment.CenterVertically) {
        Text(round.trait.title, style = MaterialTheme.typography.titleLarge, color = Palette.Text, modifier = Modifier.weight(1f))
        Text("Question ${finder.roundNumber} of ${finder.totalRounds}", style = MaterialTheme.typography.bodySmall, color = Palette.TextDim)
    }
    LinearProgressIndicator(
        progress = { finder.roundNumber.toFloat() / finder.totalRounds },
        modifier = Modifier.fillMaxWidth(),
        color = Palette.Amber,
        trackColor = Palette.Track,
        drawStopIndicator = {},
    )
    Text(round.trait.question, style = MaterialTheme.typography.bodyLarge, color = Palette.Text)
    Text(
        stringResource(R.string.ui_switch_between_them_as_often_as_you_like),
        style = MaterialTheme.typography.bodyMedium,
        color = Palette.TextDim,
    )
    NothingPlayingHint()
    val musicActive = rememberMusicActive()
    Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
        SideCard("A", side == Choice.A, musicActive, Modifier.weight(1f)) { play(Choice.A) }
        SideCard("B", side == Choice.B, musicActive, Modifier.weight(1f)) { play(Choice.B) }
    }
    val pick = side
    Button(
        enabled = pick != null,
        onClick = {
            finder.choose(pick ?: Choice.A)
            onAnswered()
        },
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = 52.dp),
    ) { Text("I prefer ${if (pick == Choice.B) "B" else "A"}") }
    FilledTonalButton(
        onClick = {
            finder.choose(Choice.NO_PREFERENCE)
            onAnswered()
        },
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = 48.dp),
    ) { Text(stringResource(R.string.ui_i_can_t_hear_a_difference)) }
    TextButton(onClick = onCancel, modifier = Modifier.heightIn(min = 48.dp)) { Text(stringResource(R.string.ui_stop_and_undo)) }
}

@Composable
fun SideCard(label: String, selected: Boolean, musicActive: Boolean, modifier: Modifier, onClick: () -> Unit) {
    Box(
        modifier
            .height(140.dp)
            .clip(RoundedCornerShape(16.dp))
            .background(if (selected) Palette.Amber else Palette.Surface)
            .border(1.dp, if (selected) Palette.Amber else Palette.Outline, RoundedCornerShape(16.dp))
            .semantics { this.selected = selected }
            .clickable(role = Role.RadioButton, onClickLabel = "Play version $label", onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                label,
                style = MaterialTheme.typography.headlineMedium,
                color = if (selected) Palette.OnAmber else Palette.Text,
            )
            Spacer(Modifier.height(4.dp))
            Text(
                when {
                    !selected -> "Tap to hear"
                    musicActive -> "Playing"
                    else -> "Selected"
                },
                style = MaterialTheme.typography.bodySmall,
                color = if (selected) Palette.OnAmber else Palette.TextDim,
            )
        }
    }
}

/**
 * The test's result has to beat what the listener already had, or there's no reason to change. Both
 * play at the same estimated loudness, and neither is presented as the expected answer.
 */
@Composable
private fun FinalChoice(
    previous: Preset,
    result: Preset,
    autoHeadroom: Boolean,
    onKeepPrevious: () -> Unit,
    onUseNew: () -> Unit,
) {
    var hearingNew by remember { mutableStateOf(true) }
    fun hear(new: Boolean) {
        hearingNew = new
        Repo.edit { if (new) SoundFinder.levelMatched(result, result.tone, previous, autoHeadroom) else previous }
    }
    LaunchedEffect(Unit) { hear(true) }

    Text(stringResource(R.string.ui_one_last_comparison), style = MaterialTheme.typography.headlineSmall, color = Palette.Text)
    Text(
        stringResource(R.string.ui_is_the_new_sound_actually_better_than_wh),
        style = MaterialTheme.typography.bodyLarge,
        color = Palette.TextDim,
    )
    NothingPlayingHint()
    val musicActive = rememberMusicActive()
    Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
        SideCard("What I had", !hearingNew, musicActive, Modifier.weight(1f)) { hear(false) }
        SideCard("New", hearingNew, musicActive, Modifier.weight(1f)) { hear(true) }
    }
    FilledTonalButton(
        onClick = onKeepPrevious,
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = 52.dp),
    ) { Text(stringResource(R.string.ui_keep_what_i_had)) }
    FilledTonalButton(
        onClick = onUseNew,
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = 52.dp),
    ) { Text(stringResource(R.string.ui_use_the_new_sound)) }
}

private fun describe(trait: Trait, value: Double): String {
    if (trait == Trait.SPACE) return if (value <= 0.0) "Off" else "${(value * 100).toInt()}%"
    return if (value == 0.0) "Unchanged" else String.format(Locale.US, "%+.1f dB", value)
}

@Composable
private fun Result(
    finder: SoundFinder,
    state: AppState,
    start: Preset,
    outputKey: String,
    onDone: () -> Unit,
    onDiscard: () -> Unit,
) {
    var name by remember { mutableStateOf("My sound") }
    var comparingOriginal by remember { mutableStateOf(false) }
    val finished = start.copy(tone = finder.result())

    fun compare(original: Boolean) {
        comparingOriginal = original
        val tone = if (original) start.tone else finished.tone
        Repo.edit { SoundFinder.levelMatched(start, tone, finished) }
    }

    Text(stringResource(R.string.ui_your_sound), style = MaterialTheme.typography.headlineSmall, color = Palette.Text)
    Text(
        stringResource(R.string.ui_this_is_what_you_preferred_with_this_mus),
        style = MaterialTheme.typography.bodyLarge,
        color = Palette.TextDim,
    )
    // Repeat tests build on each other, so these must not read as absolute levels.
    Text("Changes from ${start.name}", style = MaterialTheme.typography.titleSmall, color = Palette.TextDim)
    Column(
        Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(Palette.Surface),
    ) {
        Trait.entries.forEachIndexed { i, trait ->
            if (i > 0) RowDivider()
            Row(
                Modifier
                    .fillMaxWidth()
                    .heightIn(min = 48.dp)
                    .padding(horizontal = 16.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(trait.title, style = MaterialTheme.typography.bodyLarge, color = Palette.Text, modifier = Modifier.weight(1f))
                val value = finder.answer(trait)
                Text(describe(trait, value), style = Readout, color = if (value != 0.0) Palette.Amber else Palette.TextDim)
            }
        }
    }
    Text(stringResource(R.string.ui_compare), style = MaterialTheme.typography.titleSmall, color = Palette.TextDim)
    NothingPlayingHint()
    val musicActive = rememberMusicActive()
    Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
        SideCard("Before", comparingOriginal, musicActive, Modifier.weight(1f)) { compare(true) }
        SideCard("After", !comparingOriginal, musicActive, Modifier.weight(1f)) { compare(false) }
    }
    OutlinedTextField(
        value = name,
        onValueChange = { name = it },
        singleLine = true,
        label = { Text(stringResource(R.string.ui_preset_name)) },
        modifier = Modifier.fillMaxWidth(),
    )
    Button(
        enabled = name.isNotBlank(),
        onClick = {
            Repo.edit { finished }
            Repo.saveCurrentAs(name.trim())
            // The same key the Presets tab uses, so an existing noise-mode choice can't override it.
            val key = if (Profiles.supportsModes(outputKey)) {
                Profiles.modeKey(outputKey, state.listeningMode)
            } else {
                outputKey
            }
            Repo.assignToOutput(key, Repo.state.value.current.id)
            onDone()
        },
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = 52.dp),
    ) { Text(stringResource(R.string.ui_save_and_use)) }
    TextButton(onClick = onDiscard, modifier = Modifier.heightIn(min = 48.dp)) { Text(stringResource(R.string.ui_discard)) }
    if (state.bypass) Text(stringResource(R.string.ui_bypass_is_on_so_you_won_t_hear_the_chang), color = Palette.Danger)
}
