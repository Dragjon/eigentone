package com.phonons.app.ui

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
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
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableDoubleStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.phonons.app.data.PeakFinder
import com.phonons.app.data.Processing
import com.phonons.app.data.Repo
import com.phonons.app.data.SidePick
import com.phonons.app.data.SpotAnswer
import com.phonons.app.data.Sweep
import com.phonons.app.data.SweepMark
import com.phonons.app.dsp.GraphicEq
import com.phonons.app.engine.SweepPlayer
import java.util.Locale
import kotlinx.coroutines.delay

private enum class FinderStage { INTRO, QUESTIONS, RESULT }

private const val NOTE_MS = 450L
private const val GAP_MS = 190L
private const val MANY_MARKS = 6
private const val RESULT_GRAPH_RANGE_DB = 10.0

/**
 * Guided search for peaks and dips. It plays the notes and only asks about them. The EQ steps aside for
 * the questions, through a flag that isn't saved, and nothing is kept unless the result is added.
 */
@Composable
fun PeakFinderScreen(onClose: () -> Unit) {
    val context = LocalContext.current
    val before = remember { Repo.state.value.current }
    val player = remember { SweepPlayer(context) }
    val finder = remember { PeakFinder() }
    var stage by remember { mutableStateOf(FinderStage.INTRO) }
    var marks by remember { mutableStateOf(emptyList<SweepMark>()) }
    var answers by remember { mutableIntStateOf(0) }
    var levelDb by remember { mutableDoubleStateOf(Sweep.DEFAULT_LEVEL_DB) }
    var ready by remember { mutableStateOf(false) }
    var note by remember { mutableIntStateOf(0) }
    var failed by remember { mutableStateOf(false) }
    var finished by remember { mutableStateOf(false) }

    DisposableEffect(stage) {
        val asking = stage == FinderStage.QUESTIONS
        Repo.hearingCheckRunning.value = asking
        if (asking && !player.start()) {
            failed = true
            stage = FinderStage.INTRO
        }
        onDispose {
            if (asking) player.stop()
            Repo.hearingCheckRunning.value = false
        }
    }
    // Puts everything back unless the result was added, including if the screen is closed some other way.
    DisposableEffect(Unit) {
        Repo.comparing.value = true
        onDispose {
            Repo.comparing.value = false
            if (!finished) Repo.edit { before }
        }
    }
    LaunchedEffect(levelDb) { player.levelDb = levelDb }

    LaunchedEffect(stage, answers) {
        if (stage != FinderStage.QUESTIONS) return@LaunchedEffect
        val tones = finder.question?.tones ?: return@LaunchedEffect
        ready = false
        var played = 0
        while (true) {
            player.muted = true
            delay(GAP_MS)
            player.hz = tones[played % tones.size]
            note = played % tones.size
            player.muted = false
            delay(NOTE_MS)
            played++
            // Every note has to be heard at least once before answering.
            if (played >= tones.size) ready = true
        }
    }

    fun afterAnswer() {
        answers++
        if (finder.isDone) {
            marks = finder.marks().take(Sweep.roomFor(before.filters))
            player.muted = false
            stage = FinderStage.RESULT
        }
    }

    BackHandler { onClose() }

    Column(
        Modifier
            .fillMaxSize()
            .background(Palette.Background)
            .windowInsetsPadding(WindowInsets.statusBars)
            .windowInsetsPadding(WindowInsets.navigationBars),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Column(
            Modifier
                .widthIn(max = 560.dp)
                .fillMaxWidth()
                .weight(1f)
                .verticalScroll(rememberScrollState())
                .padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            when (stage) {
                FinderStage.INTRO -> Intro(failed, onStart = { stage = FinderStage.QUESTIONS }, onCancel = onClose)
                FinderStage.QUESTIONS -> finder.question?.let { question ->
                    Questions(
                        finder = finder,
                        question = question,
                        answers = answers,
                        note = note,
                        ready = ready,
                        levelDb = levelDb,
                        onLevel = { levelDb = it },
                        onSpot = { answer ->
                            finder.answer(answer)
                            afterAnswer()
                        },
                        onPick = { side ->
                            finder.pick(side)
                            afterAnswer()
                        },
                        onStop = onClose,
                    )
                }
                FinderStage.RESULT -> Result(
                    marks = marks,
                    onHear = { withFilters ->
                        Repo.edit { if (withFilters) it.copy(filters = Sweep.addTo(before.filters, marks), tone = before.tone) else before }
                    },
                    onAdd = {
                        finished = true
                        Repo.edit { before.copy(filters = Sweep.addTo(before.filters, marks)) }
                        Repo.commit()
                        onClose()
                    },
                    onClose = onClose,
                )
            }
        }
    }
}

@Composable
private fun Intro(failed: Boolean, onStart: () -> Unit, onCancel: () -> Unit) {
    Text("Find peaks for me", style = MaterialTheme.typography.headlineSmall, color = Palette.Text)
    Text(
        "You'll hear a note alternating with the notes just beside it, and say whether it stands out. There are about ${PeakFinder.spots().size} short questions, then a few more about anything you flagged. It takes around 3 minutes.",
        style = MaterialTheme.typography.bodyLarge,
        color = Palette.TextDim,
    )
    SettingsSection("Before you start") {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            listOf(
                "Wear the headphones you want this for, and go somewhere quiet.",
                "Keep your volume low, and stop if a tone is ever uncomfortable.",
                "If you're unsure, answer Same. Only clear differences are worth fixing.",
            ).forEach { Text("• $it", style = MaterialTheme.typography.bodyMedium, color = Palette.Text) }
        }
    }
    Text(
        "This finds what you hear on these headphones. It isn't a measurement.",
        style = MaterialTheme.typography.bodySmall,
        color = Palette.TextDim,
    )
    if (failed) Text("This phone wouldn't play the tone.", style = MaterialTheme.typography.bodyMedium, color = Palette.Danger)
    Button(onClick = onStart, modifier = Modifier.fillMaxWidth().heightIn(min = 52.dp)) { Text("Start") }
    TextButton(onClick = onCancel, modifier = Modifier.heightIn(min = 48.dp)) { Text("Not now") }
}

@Composable
private fun Questions(
    finder: PeakFinder,
    question: PeakFinder.Question,
    answers: Int,
    note: Int,
    ready: Boolean,
    levelDb: Double,
    onLevel: (Double) -> Unit,
    onSpot: (SpotAnswer) -> Unit,
    onPick: (SidePick) -> Unit,
    onStop: () -> Unit,
) {
    // PeakFinder isn't observable, so everything shown is read here, where each answer redraws it.
    val progress = answers.toFloat() / finder.total.coerceAtLeast(1)
    Text("Step ${finder.answered + 1} of ${finder.total}", style = MaterialTheme.typography.titleLarge, color = Palette.Text)
    LinearProgressIndicator(
        progress = { progress },
        modifier = Modifier.fillMaxWidth(),
        color = Palette.Amber,
        trackColor = Palette.Track,
        drawStopIndicator = {},
    )
    Text(
        GraphicEq.formatFrequency(kotlin.math.round(question.hz)),
        style = MaterialTheme.typography.displaySmall,
        color = Palette.Amber,
    )
    when (question) {
        is PeakFinder.Question.Spot -> {
            val mainNote = note % 2 == 0
            Text(
                if (question.rechecking) "Checking again: does this note stand out from the ones beside it?" else "Does this note stand out from the ones beside it?",
                style = MaterialTheme.typography.titleMedium,
                color = Palette.Text,
            )
            Text(
                if (mainNote) "Playing: this note" else "Playing: the note beside it",
                style = MaterialTheme.typography.bodyMedium,
                color = if (mainNote) Palette.Amber else Palette.TextDim,
            )
            AnswerButton("Stands out", ready) { onSpot(SpotAnswer.STANDS_OUT) }
            AnswerButton("Same", ready) { onSpot(SpotAnswer.SAME) }
            AnswerButton("Weaker", ready) { onSpot(SpotAnswer.WEAKER) }
        }
        is PeakFinder.Question.Narrow -> {
            val word = if (question.answer == SpotAnswer.STANDS_OUT) "stronger" else "weaker"
            Text("Which of these two notes is $word?", style = MaterialTheme.typography.titleMedium, color = Palette.Text)
            Text(
                if (note % 2 == 0) "Playing: first note" else "Playing: second note",
                style = MaterialTheme.typography.bodyMedium,
                color = if (note % 2 == 0) Palette.Amber else Palette.RightEar,
            )
            AnswerButton("First", ready) { onPick(SidePick.FIRST) }
            AnswerButton("Second", ready) { onPick(SidePick.SECOND) }
            AnswerButton("About the same", ready) { onPick(SidePick.SAME) }
        }
    }
    SliderSetting(
        title = "Tone level",
        tip = Help.sweepLevel,
        readout = String.format(Locale.US, "%.0f dB", levelDb),
        value = levelDb.toFloat(),
        range = Sweep.MIN_LEVEL_DB.toFloat()..Sweep.MAX_LEVEL_DB.toFloat(),
        default = Sweep.DEFAULT_LEVEL_DB.toFloat(),
        onChange = { onLevel(it.toDouble()) },
    )
    TextButton(onClick = onStop, modifier = Modifier.heightIn(min = 48.dp)) { Text("Stop") }
}

@Composable
private fun AnswerButton(text: String, enabled: Boolean, onClick: () -> Unit) {
    FilledTonalButton(
        enabled = enabled,
        onClick = onClick,
        modifier = Modifier.fillMaxWidth().heightIn(min = 56.dp),
    ) { Text(text) }
}

@Composable
private fun Result(
    marks: List<SweepMark>,
    onHear: (Boolean) -> Unit,
    onAdd: () -> Unit,
    onClose: () -> Unit,
) {
    if (marks.isEmpty()) {
        Text("Nothing stood out", style = MaterialTheme.typography.headlineSmall, color = Palette.Text)
        Text(
            "Nothing held up when checked twice, so there's nothing to add. Your headphones sound even to you, or the differences are too small to be worth fixing.",
            style = MaterialTheme.typography.bodyLarge,
            color = Palette.TextDim,
        )
        Button(onClick = onClose, modifier = Modifier.fillMaxWidth().heightIn(min = 52.dp)) { Text("Done") }
        return
    }

    var hearingNew by remember { mutableStateOf(true) }
    LaunchedEffect(hearingNew) { onHear(hearingNew) }
    val filters = remember(marks) { Sweep.addTo(emptyList(), marks) }

    Text("What you found", style = MaterialTheme.typography.headlineSmall, color = Palette.Text)
    Text(
        "These narrow filters would even out what you noticed. Play some music and compare before keeping them.",
        style = MaterialTheme.typography.bodyLarge,
        color = Palette.TextDim,
    )
    EqGraph(
        filters = emptyList(),
        curveFilters = filters,
        spectrum = { null },
        selectedId = null,
        dimmed = false,
        onSelect = {},
        onDrag = { _, _, _ -> },
        onDragEnd = {},
        modifier = Modifier
            .clip(RoundedCornerShape(16.dp))
            .background(Palette.Surface),
        heightDp = 150,
        rangeDb = RESULT_GRAPH_RANGE_DB,
    )
    SettingsSection("Changes") {
        marks.forEachIndexed { index, mark ->
            if (index > 0) RowDivider()
            Row(
                Modifier
                    .fillMaxWidth()
                    .heightIn(min = 40.dp)
                    .padding(horizontal = 16.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    GraphicEq.formatFrequency(kotlin.math.round(mark.hz)),
                    style = MaterialTheme.typography.bodyMedium,
                    color = Palette.Text,
                    modifier = Modifier.weight(1f),
                )
                Text(String.format(Locale.US, "%+.0f dB", mark.gainDb), style = Readout, color = Palette.Amber)
            }
        }
    }
    if (marks.size >= MANY_MARKS) {
        Text(
            "That's a lot of spots for a single pair of headphones, and some may just be how your ears hear different notes. Compare carefully, and run this again if the result sounds worse.",
            style = MaterialTheme.typography.bodySmall,
            color = Palette.TextDim,
        )
    }
    NothingPlayingHint()
    val musicActive = rememberMusicActive()
    Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
        SideCard("Before", !hearingNew, musicActive, Modifier.weight(1f)) { hearingNew = false }
        SideCard("With filters", hearingNew, musicActive, Modifier.weight(1f)) { hearingNew = true }
    }
    Button(onClick = onAdd, modifier = Modifier.fillMaxWidth().heightIn(min = 52.dp)) { Text("Add to my sound") }
    TextButton(onClick = onClose, modifier = Modifier.heightIn(min = 48.dp)) { Text("Discard") }
}
