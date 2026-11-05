package io.github.dragjon.eigentone.ui

import io.github.dragjon.eigentone.R
import androidx.compose.ui.res.stringResource
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
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
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
import io.github.dragjon.eigentone.data.PeakFinder
import io.github.dragjon.eigentone.data.Processing
import io.github.dragjon.eigentone.data.Repo
import io.github.dragjon.eigentone.data.SidePick
import io.github.dragjon.eigentone.data.SpotAnswer
import io.github.dragjon.eigentone.data.Sweep
import io.github.dragjon.eigentone.data.SweepMark
import io.github.dragjon.eigentone.dsp.GraphicEq
import io.github.dragjon.eigentone.engine.SweepPlayer
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

    val questionProgress = remember(answers, stage) {
        if (stage == FinderStage.QUESTIONS) answers.toFloat() / finder.total.coerceAtLeast(1) else null
    }

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

    ToolScreen(title = stringResource(R.string.ui_find_peaks_for_me), onBack = onClose, spacing = 14.dp, progress = questionProgress) {
        when (stage) {
            FinderStage.INTRO -> Intro(failed, onStart = { stage = FinderStage.QUESTIONS })
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

@Composable
private fun Intro(failed: Boolean, onStart: () -> Unit) {
    Text(
        stringResource(R.string.ui_you_ll_hear_a_note_alternating, PeakFinder.spots().size),
        style = MaterialTheme.typography.bodyLarge,
        color = Palette.TextDim,
    )
    SettingsSection(stringResource(R.string.ui_before_you_start)) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            listOf(
                stringResource(R.string.ui_wear_the_headphones_you_want_this),
                stringResource(R.string.ui_keep_your_volume_low_and_stop),
                stringResource(R.string.ui_if_you_re_unsure_answer_same),
            ).forEach { Text("• $it", style = MaterialTheme.typography.bodyMedium, color = Palette.Text) }
        }
    }
    Text(
        stringResource(R.string.ui_this_finds_what_you_hear_on_these_headph),
        style = MaterialTheme.typography.bodySmall,
        color = Palette.TextDim,
    )
    if (failed) Text(stringResource(R.string.ui_this_phone_wouldn_t_play_the_tone), style = MaterialTheme.typography.bodyMedium, color = Palette.Danger)
    Button(onClick = onStart, modifier = Modifier.fillMaxWidth().heightIn(min = 52.dp)) { Text(stringResource(R.string.ui_start)) }
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
    Text(stringResource(R.string.ui_step_of, finder.answered + 1, finder.total), style = MaterialTheme.typography.titleLarge, color = Palette.Text)
    Text(
        GraphicEq.formatFrequency(kotlin.math.round(question.hz)),
        style = BigReadout,
        color = Palette.Lit,
    )
    Lamps(count = question.tones.size, lit = note)
    when (question) {
        is PeakFinder.Question.Spot -> {
            val mainNote = note % 2 == 0
            Text(
                if (question.rechecking) stringResource(R.string.ui_checking_again_does_this_note_stand) else stringResource(R.string.ui_does_this_note_stand_out_from),
                style = MaterialTheme.typography.titleMedium,
                color = Palette.Text,
            )
            Text(
                if (mainNote) stringResource(R.string.ui_playing_this_note) else stringResource(R.string.ui_playing_the_note_beside_it),
                style = MaterialTheme.typography.bodyMedium,
                color = if (mainNote) Palette.Lit else Palette.TextDim,
            )
            AnswerButton(stringResource(R.string.ui_stands_out), ready) { onSpot(SpotAnswer.STANDS_OUT) }
            AnswerButton(stringResource(R.string.ui_same), ready) { onSpot(SpotAnswer.SAME) }
            AnswerButton(stringResource(R.string.ui_weaker), ready) { onSpot(SpotAnswer.WEAKER) }
        }
        is PeakFinder.Question.Narrow -> {
            Text(stringResource(if (question.answer == SpotAnswer.STANDS_OUT) R.string.ui_which_note_is_stronger else R.string.ui_which_note_is_weaker), style = MaterialTheme.typography.titleMedium, color = Palette.Text)
            Text(
                if (note % 2 == 0) stringResource(R.string.ui_playing_first_note) else stringResource(R.string.ui_playing_second_note),
                style = MaterialTheme.typography.bodyMedium,
                color = if (note % 2 == 0) Palette.Lit else Palette.Text,
            )
            AnswerButton(stringResource(R.string.ui_first), ready) { onPick(SidePick.FIRST) }
            AnswerButton(stringResource(R.string.ui_second), ready) { onPick(SidePick.SECOND) }
            AnswerButton(stringResource(R.string.ui_about_the_same), ready) { onPick(SidePick.SAME) }
        }
    }
    SliderSetting(
        title = stringResource(R.string.ui_tone_level),
        tip = Help.sweepLevel,
        readout = String.format(Locale.US, "%.0f dB", levelDb),
        value = levelDb.toFloat(),
        range = Sweep.MIN_LEVEL_DB.toFloat()..Sweep.MAX_LEVEL_DB.toFloat(),
        default = Sweep.DEFAULT_LEVEL_DB.toFloat(),
        onChange = { onLevel(it.toDouble()) },
    )
    TextButton(onClick = onStop, modifier = Modifier.heightIn(min = 48.dp)) { Text(stringResource(R.string.ui_stop)) }
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
        Text(stringResource(R.string.ui_nothing_stood_out), style = MaterialTheme.typography.headlineSmall, color = Palette.Text)
        Text(
            stringResource(R.string.ui_nothing_held_up_when_checked_twice_so_th),
            style = MaterialTheme.typography.bodyLarge,
            color = Palette.TextDim,
        )
        Button(onClick = onClose, modifier = Modifier.fillMaxWidth().heightIn(min = 52.dp)) { Text(stringResource(R.string.ui_done)) }
        return
    }

    var hearingNew by remember { mutableStateOf(true) }
    LaunchedEffect(hearingNew) { onHear(hearingNew) }
    val filters = remember(marks) { Sweep.addTo(emptyList(), marks) }

    Text(stringResource(R.string.ui_what_you_found), style = MaterialTheme.typography.headlineSmall, color = Palette.Text)
    Text(
        stringResource(R.string.ui_these_narrow_filters_would_even_out_what),
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
            .clip(PanelShape)
            .background(Palette.Surface),
        heightDp = 150,
        rangeDb = RESULT_GRAPH_RANGE_DB,
    )
    SettingsSection(stringResource(R.string.ui_changes)) {
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
                Text(String.format(Locale.US, "%+.0f dB", mark.gainDb), style = Readout, color = Palette.Lit)
            }
        }
    }
    if (marks.size >= MANY_MARKS) {
        Text(
            stringResource(R.string.ui_that_s_a_lot_of_spots_for_a_single_pair),
            style = MaterialTheme.typography.bodySmall,
            color = Palette.TextDim,
        )
    }
    NothingPlayingHint()
    val musicActive = rememberMusicActive()
    Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
        SideCard(stringResource(R.string.ui_before), !hearingNew, musicActive, Modifier.weight(1f)) { hearingNew = false }
        SideCard(stringResource(R.string.ui_with_filters), hearingNew, musicActive, Modifier.weight(1f)) { hearingNew = true }
    }
    Button(onClick = onAdd, modifier = Modifier.fillMaxWidth().heightIn(min = 52.dp)) { Text(stringResource(R.string.ui_add_to_my_sound)) }
    TextButton(onClick = onClose, modifier = Modifier.heightIn(min = 48.dp)) { Text(stringResource(R.string.ui_discard)) }
}
