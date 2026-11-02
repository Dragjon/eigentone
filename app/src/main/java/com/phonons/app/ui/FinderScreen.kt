package com.phonons.app.ui

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

private enum class FinderStep { INTRO, COMPARE, RESULT }

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
                    correction = BuiltIns.correctionFor(output.key),
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
                            if (active.isDone) {
                                Repo.edit { start.copy(tone = active.result()) }
                                step = FinderStep.RESULT
                            }
                        },
                        onCancel = ::cancel,
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
    Text("Find my sound", style = MaterialTheme.typography.headlineSmall, color = Palette.Text)
    Text(
        "You'll compare two versions of your music a few times and choose the one you prefer. It takes about two minutes, and there are no wrong answers.",
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
        Text("Before you start", style = MaterialTheme.typography.titleSmall, color = Palette.Text)
        Text("• Play a song you know well, and keep it playing.", style = MaterialTheme.typography.bodyMedium, color = Palette.Text)
        Text("• Set a comfortable volume, and leave it there.", style = MaterialTheme.typography.bodyMedium, color = Palette.Text)
        Text("• Listening on: $outputLabel", style = MaterialTheme.typography.bodyMedium, color = Palette.Text)
    }
    Text("Start from", style = MaterialTheme.typography.titleSmall, color = Palette.TextDim)
    if (correction != null) {
        Button(onClick = { onStart(correction) }, modifier = Modifier.fillMaxWidth()) {
            Text("${correction.name} correction (recommended)")
        }
    } else {
        Button(onClick = onFind, modifier = Modifier.fillMaxWidth()) { Text("Find my headphones first (recommended)") }
    }
    FilledTonalButton(onClick = { onStart(current) }, modifier = Modifier.fillMaxWidth()) {
        Text("My current sound: ${current.name}")
    }
    Text(
        "A headphone correction evens out your model's sound first, so your choices start from a balanced base.",
        style = MaterialTheme.typography.bodySmall,
        color = Palette.TextDim,
    )
    TextButton(onClick = onCancel, modifier = Modifier.heightIn(min = 48.dp)) { Text("Not now") }
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
        "Switch between them as often as you like while the music plays. Both are matched in loudness.",
        style = MaterialTheme.typography.bodyMedium,
        color = Palette.TextDim,
    )
    Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
        SideCard("A", side == Choice.A, Modifier.weight(1f)) { play(Choice.A) }
        SideCard("B", side == Choice.B, Modifier.weight(1f)) { play(Choice.B) }
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
    ) { Text("I can't hear a difference") }
    TextButton(onClick = onCancel, modifier = Modifier.heightIn(min = 48.dp)) { Text("Stop and undo") }
}

@Composable
private fun SideCard(label: String, selected: Boolean, modifier: Modifier, onClick: () -> Unit) {
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
                if (selected) "Playing" else "Tap to hear",
                style = MaterialTheme.typography.bodySmall,
                color = if (selected) Palette.OnAmber else Palette.TextDim,
            )
        }
    }
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

    Text("Your sound", style = MaterialTheme.typography.headlineSmall, color = Palette.Text)
    Text(
        "This is what you preferred with this music and these headphones. Other songs may suit it differently, and you can run the test again at any time.",
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
    Text("Compare", style = MaterialTheme.typography.titleSmall, color = Palette.TextDim)
    Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
        SideCard("Before", comparingOriginal, Modifier.weight(1f)) { compare(true) }
        SideCard("After", !comparingOriginal, Modifier.weight(1f)) { compare(false) }
    }
    OutlinedTextField(
        value = name,
        onValueChange = { name = it },
        singleLine = true,
        label = { Text("Preset name") },
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
    ) { Text("Save and use") }
    TextButton(onClick = onDiscard, modifier = Modifier.heightIn(min = 48.dp)) { Text("Discard") }
    if (state.bypass) Text("Bypass is on, so you won't hear the changes.", color = Palette.Danger)
}
