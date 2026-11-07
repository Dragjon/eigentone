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
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import io.github.dragjon.eigentone.data.CustomShape
import io.github.dragjon.eigentone.data.HearingRecord
import io.github.dragjon.eigentone.data.HearingResult
import io.github.dragjon.eigentone.data.HearingTest
import io.github.dragjon.eigentone.data.Profiles
import io.github.dragjon.eigentone.data.Repo
import io.github.dragjon.eigentone.dsp.GraphicEq
import io.github.dragjon.eigentone.dsp.GraphicShape
import io.github.dragjon.eigentone.engine.TonePlayer
import java.util.Locale

private enum class HearingStep { INTRO, TONES, RESULT }

/** Where the current tone is up to. Shown on screen, because a quiet tone can be impossible to hear. */
private enum class TonePhase { WAITING, PLAYING, ASKING, FAILED }

/**
 * A listening check that builds a gentle curve from which frequencies are heard least well. The EQ
 * steps aside while it runs, through a flag that isn't saved, so nothing can stay switched off.
 */
@Composable
fun HearingScreen(onClose: () -> Unit) {
    val context = LocalContext.current
    val player = remember { TonePlayer(context) }
    var step by remember { mutableStateOf(HearingStep.INTRO) }
    val test = remember { HearingTest() }
    var answers by remember { mutableIntStateOf(0) }
    var replays by remember { mutableIntStateOf(0) }
    var phase by remember { mutableStateOf(TonePhase.WAITING) }
    var result by remember { mutableStateOf<HearingResult?>(null) }

    val testProgress = remember(answers, step) { if (step == HearingStep.TONES) test.progress else null }

    DisposableEffect(step) {
        val testing = step == HearingStep.TONES
        Repo.hearingCheckRunning.value = testing
        if (testing) player.begin()
        onDispose {
            if (testing) player.end()
            Repo.hearingCheckRunning.value = false
        }
    }

    // A back swipe partway through the tones would lose every answer, so it asks first.
    var confirmingStop by remember { mutableStateOf(false) }
    fun back() {
        if (step == HearingStep.TONES && answers > 0) confirmingStop = true else onClose()
    }
    BackHandler { back() }
    if (confirmingStop) {
        StopTestDialog(onStop = { confirmingStop = false; onClose() }, onKeepGoing = { confirmingStop = false })
    }

    ToolScreen(title = stringResource(R.string.ui_hearing_check), onBack = ::back, spacing = 16.dp, progress = testProgress) {
        // Each step slides in as the last one leaves.
        ToolSteps(step) { shown ->
        when (shown) {
            HearingStep.INTRO -> Intro(onStart = { step = HearingStep.TONES })
            HearingStep.TONES -> {
                LaunchedEffect(answers, replays) {
                    phase = TonePhase.WAITING
                    // A short pause, so a tone can't be mistaken for the sound of the tap.
                    kotlinx.coroutines.delay(TONE_GAP_MS)
                    val hz = test.hz ?: return@LaunchedEffect
                    if (!player.play(hz, test.levelDb, test.ear!!)) {
                        phase = TonePhase.FAILED
                        return@LaunchedEffect
                    }
                    phase = TonePhase.PLAYING
                    kotlinx.coroutines.delay(TonePlayer.TONE_MS)
                    phase = TonePhase.ASKING
                }
                Tones(
                    test = test,
                    answers = answers,
                    phase = phase,
                    onPlayAgain = {
                        player.stop()
                        replays++
                    },
                    onAnswer = { heard ->
                        player.stop()
                        test.answer(heard)
                        answers++
                        if (test.isDone) {
                            val finished = test.result()
                            result = finished
                            // Saved now, so ear correction can still be chosen after leaving this screen.
                            val output = Repo.output.value
                            val record = HearingRecord.of(finished, output.label, System.currentTimeMillis())
                            Repo.update { it.copy(hearings = it.hearings + (output.key to record)) }
                            step = HearingStep.RESULT
                        }
                    },
                    onCancel = onClose,
                )
            }
            HearingStep.RESULT -> result?.let { Result(it, onClose) }
        }
        }
    }
}

@Composable
private fun Intro(onStart: () -> Unit) {
    Text(
        stringResource(R.string.ui_you_ll_hear_quiet_tones_in_one_ear_at_a),
        style = MaterialTheme.typography.bodyLarge,
        color = Palette.TextDim,
    )
    SettingsSection(stringResource(R.string.ui_before_you_start)) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            listOf(
                stringResource(R.string.ui_wear_the_headphones_you_want_this),
                stringResource(R.string.ui_turn_on_noise_cancelling_if_you),
                stringResource(R.string.ui_set_your_usual_music_volume_and),
                stringResource(R.string.ui_stop_if_a_tone_is_ever),
            ).forEach { Text("• $it", style = MaterialTheme.typography.bodyMedium, color = Palette.Text) }
        }
    }
    Text(
        stringResource(R.string.ui_this_isn_t_a_medical_hearing_test_and_ca),
        style = MaterialTheme.typography.bodySmall,
        color = Palette.TextDim,
    )
    Button(onClick = onStart, modifier = Modifier.fillMaxWidth().heightIn(min = 52.dp)) { Text(stringResource(R.string.ui_start)) }
}

@Composable
private fun Tones(
    test: HearingTest,
    answers: Int,
    phase: TonePhase,
    onPlayAgain: () -> Unit,
    onAnswer: (Boolean) -> Unit,
    onCancel: () -> Unit,
) {
    val ear = test.ear ?: return
    val hz = test.hz ?: return
    // HearingTest isn't observable, so everything shown is read here, where each answer redraws it.
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        EarMark(left = ear == io.github.dragjon.eigentone.data.Ear.LEFT)
        Text(stringResource(R.string.ui_listen_with_your_ear, ear.label), style = MaterialTheme.typography.titleLarge, color = Palette.Text)
    }
    Text(GraphicEq.formatFrequency(hz), style = BigReadout, color = Palette.Lit)
    if (phase != TonePhase.FAILED) Lamps(count = 3, lit = phase.ordinal)
    Text(
        appPlural(R.plurals.ui_hearing_status, answers, GraphicEq.formatFrequency(hz), test.attempt, test.stepNumber, test.stepCount, answers),
        style = MaterialTheme.typography.bodyMedium,
        color = Palette.TextDim,
    )
    Text(
        when (phase) {
            TonePhase.WAITING -> stringResource(R.string.ui_next_tone_coming)
            TonePhase.PLAYING -> stringResource(R.string.ui_listen_now_three_short_beeps)
            TonePhase.ASKING -> stringResource(R.string.ui_did_you_hear_it)
            TonePhase.FAILED -> stringResource(R.string.ui_this_phone_wouldn_t_play_the)
        },
        style = MaterialTheme.typography.titleMedium,
        color = when (phase) {
            TonePhase.PLAYING -> Palette.Lit
            TonePhase.FAILED -> Palette.Danger
            else -> Palette.Text
        },
    )
    Text(
        stringResource(R.string.ui_after_you_hear_one_the_next_is_quieter_s),
        style = MaterialTheme.typography.bodyMedium,
        color = Palette.TextDim,
    )
    // Hearing it can be answered as soon as it starts, but not hearing it only once it has finished.
    Button(
        enabled = phase == TonePhase.PLAYING || phase == TonePhase.ASKING,
        onClick = { onAnswer(true) },
        modifier = Modifier.fillMaxWidth().heightIn(min = 64.dp),
    ) { Text(stringResource(R.string.ui_i_heard_it)) }
    FilledTonalButton(
        enabled = phase == TonePhase.ASKING,
        onClick = { onAnswer(false) },
        modifier = Modifier.fillMaxWidth().heightIn(min = 64.dp),
    ) { Text(stringResource(R.string.ui_i_didn_t_hear_it)) }
    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        TextButton(onClick = onPlayAgain, modifier = Modifier.heightIn(min = 48.dp)) { Text(stringResource(R.string.ui_play_again)) }
        TextButton(onClick = onCancel, modifier = Modifier.heightIn(min = 48.dp)) { Text(stringResource(R.string.ui_stop)) }
    }
}

@Composable
private fun Result(result: HearingResult, onClose: () -> Unit) {
    val points = remember(result) { result.shapePoints() }
    // Kept from when the check finished, in case the headphones are disconnected on this screen.
    val output = remember(result) { Repo.output.value }
    val ears = remember(result) { result.earCorrection() }
    val recommended = remember(result) { result.earCorrectionIsRecommended() }
    var earsApplied by remember { mutableStateOf(false) }
    val hasCurve = points.any { it.db != 0.0 }
    fun save(): CustomShape {
        val existing = Repo.state.value.customShapes.count { it.name.startsWith(NAME) }
        val shape = CustomShape(
            "custom:hearing-${System.currentTimeMillis()}",
            if (existing == 0) NAME else "$NAME ${existing + 1}",
            points,
        )
        Repo.saveShape(shape)
        return shape
    }

    Text(stringResource(R.string.ui_your_curve), style = MaterialTheme.typography.headlineSmall, color = Palette.Text)
    Text(
        if (hasCurve) {
            stringResource(R.string.ui_this_lifts_the_frequencies_you_heard)
        } else {
            stringResource(R.string.ui_your_results_were_about_even_across)
        },
        style = MaterialTheme.typography.bodyLarge,
        color = Palette.TextDim,
    )
    if (hasCurve) {
        SettingsSection(stringResource(R.string.ui_changes)) {
            points.forEach { point ->
                Row(
                    Modifier
                        .fillMaxWidth()
                        .heightIn(min = 40.dp)
                        .padding(horizontal = 16.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        GraphicEq.formatFrequency(point.hz),
                        style = MaterialTheme.typography.bodyMedium,
                        color = Palette.Text,
                        modifier = Modifier.weight(1f),
                    )
                    Text(
                        String.format(Locale.US, "%+.1f dB", point.db),
                        style = Readout,
                        color = if (point.db != 0.0) Palette.Lit else Palette.TextDim,
                    )
                }
            }
        }
        Button(
            onClick = {
                val shape = save()
                val graphic = GraphicShape(shape.name, shape.points.map { it.hz to it.db })
                Repo.editTone { tone ->
                    tone.withShape(
                        shape.name,
                        GraphicEq.layered(tone.graphicDb, graphic, tone.graphicBands),
                        tone.graphicBands,
                        layered = true,
                    )
                }
                Repo.commit()
                onClose()
            },
            modifier = Modifier.fillMaxWidth().heightIn(min = 52.dp),
        ) { Text(stringResource(R.string.ui_add_to_my_sound)) }
        FilledTonalButton(
            onClick = {
                save()
                onClose()
            },
            modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp),
        ) { Text(stringResource(R.string.ui_only_save_it_to_your_shapes)) }
    }
    if (Profiles.supportsModes(output.key)) {
        HearingGraph(result)
        Text(
            if (recommended) {
                stringResource(R.string.ui_your_ears_differed_by_up_to, result.largestEarDifferenceDb().toInt(), output.label)
            } else {
                earsCloseNote(result)
            },
            style = MaterialTheme.typography.bodyMedium,
            color = Palette.Text,
        )
        if (ears.hasChanges) {
            FilledTonalButton(
                enabled = !earsApplied,
                onClick = {
                    Repo.update { it.copy(earCorrections = it.earCorrections + (output.key to ears)) }
                    earsApplied = true
                },
                modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp),
            ) {
                Text(
                    when {
                        earsApplied -> stringResource(R.string.ui_each_ear_is_now_corrected)
                        recommended -> stringResource(R.string.ui_correct_each_ear)
                        else -> stringResource(R.string.ui_correct_each_ear_anyway)
                    },
                )
            }
        }
    }
    TextButton(onClick = onClose, modifier = Modifier.heightIn(min = 48.dp)) { Text(if (hasCurve) stringResource(R.string.ui_not_now) else stringResource(R.string.ui_done)) }
}

/** What each ear measured, side by side, so they can be compared whether or not they need correcting. */
@Composable
fun HearingGraph(result: HearingResult, modifier: Modifier = Modifier) {
    val levels = remember(result) { result.earLevels() }
    Column(modifier, verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Text(stringResource(R.string.ui_how_each_ear_heard), style = MaterialTheme.typography.titleSmall, color = Palette.Text)
        EarCurvesGraph(
            levels,
            Modifier
                .clip(PanelShape)
                .background(Palette.Surface),
            rangeDb = HEARING_GRAPH_RANGE_DB,
            markers = true,
        )
        Text(
            stringResource(R.string.ui_higher_means_that_ear_needed_a_louder_to),
            style = MaterialTheme.typography.bodySmall,
            color = Palette.TextDim,
        )
    }
}

/** Explains why a correction isn't suggested, while leaving the choice to the listener. */
fun earsCloseNote(result: HearingResult): String {
    val gap = result.largestEarDifferenceDb().toInt()
    return if (gap == 0) {
        "Your ears matched at every frequency, so there's nothing to correct."
    } else {
        "Your ears are within $gap dB, which is close to what this check can tell apart, so no correction is needed. You can still try one."
    }
}

private const val HEARING_GRAPH_RANGE_DB = 20.0
private const val NAME = "My hearing"
private const val TONE_GAP_MS = 400L
