package com.phonons.app.ui

import com.phonons.app.R
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
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.phonons.app.data.CustomShape
import com.phonons.app.data.HearingRecord
import com.phonons.app.data.HearingResult
import com.phonons.app.data.HearingTest
import com.phonons.app.data.Profiles
import com.phonons.app.data.Repo
import com.phonons.app.dsp.GraphicEq
import com.phonons.app.dsp.GraphicShape
import com.phonons.app.engine.TonePlayer
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

    DisposableEffect(step) {
        val testing = step == HearingStep.TONES
        Repo.hearingCheckRunning.value = testing
        if (testing) player.begin()
        onDispose {
            if (testing) player.end()
            Repo.hearingCheckRunning.value = false
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
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            when (step) {
                HearingStep.INTRO -> Intro(onStart = { step = HearingStep.TONES }, onCancel = onClose)
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
private fun Intro(onStart: () -> Unit, onCancel: () -> Unit) {
    Text(stringResource(R.string.ui_hearing_check), style = MaterialTheme.typography.headlineSmall, color = Palette.Text)
    Text(
        stringResource(R.string.ui_you_ll_hear_quiet_tones_in_one_ear_at_a),
        style = MaterialTheme.typography.bodyLarge,
        color = Palette.TextDim,
    )
    SettingsSection(stringResource(R.string.ui_before_you_start)) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            listOf(
                "Wear the headphones you want this for, and go somewhere quiet.",
                "Turn on noise cancelling if you have it, and set the headphones' own EQ to flat.",
                "Set your usual music volume, and don't change it during the check.",
                "Stop if a tone is ever uncomfortable.",
            ).forEach { Text("• $it", style = MaterialTheme.typography.bodyMedium, color = Palette.Text) }
        }
    }
    Text(
        stringResource(R.string.ui_this_isn_t_a_medical_hearing_test_and_ca),
        style = MaterialTheme.typography.bodySmall,
        color = Palette.TextDim,
    )
    Button(onClick = onStart, modifier = Modifier.fillMaxWidth().heightIn(min = 52.dp)) { Text(stringResource(R.string.ui_start)) }
    TextButton(onClick = onCancel, modifier = Modifier.heightIn(min = 48.dp)) { Text(stringResource(R.string.ui_not_now)) }
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
    val progress = test.progress
    Text("Listen with your ${ear.label} ear", style = MaterialTheme.typography.titleLarge, color = Palette.Text)
    LinearProgressIndicator(
        progress = { progress },
        modifier = Modifier.fillMaxWidth(),
        color = Palette.Amber,
        trackColor = Palette.Track,
        drawStopIndicator = {},
    )
    Text(
        "${GraphicEq.formatFrequency(hz)}, tone ${test.attempt}. Part ${test.stepNumber} of ${test.stepCount}, after $answers ${if (answers == 1) "answer" else "answers"}.",
        style = MaterialTheme.typography.bodyMedium,
        color = Palette.TextDim,
    )
    Text(
        when (phase) {
            TonePhase.WAITING -> "Next tone coming…"
            TonePhase.PLAYING -> "Listen now: three short beeps…"
            TonePhase.ASKING -> "Did you hear it?"
            TonePhase.FAILED -> "This phone wouldn't play the tone. Try Play again."
        },
        style = MaterialTheme.typography.titleMedium,
        color = when (phase) {
            TonePhase.PLAYING -> Palette.Amber
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
            "This lifts the frequencies you heard least well, by half the difference. These headphones' own tuning is part of the result, so it suits them best."
        } else {
            "Your results were about even across frequencies, so there's no shared curve to add."
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
                        color = if (point.db != 0.0) Palette.Amber else Palette.TextDim,
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
                "Your ears differed by up to ${result.largestEarDifferenceDb().toInt()} dB at some frequencies, which can also come from fit. Eigentone can close half of that gap, frequency by frequency, with a separate EQ for each ear. It's saved for ${output.label}, and you can also do this later from the Sound tab, under Space."
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
                        earsApplied -> "Each ear is now corrected"
                        recommended -> "Correct each ear"
                        else -> "Correct each ear anyway"
                    },
                )
            }
        }
    }
    TextButton(onClick = onClose, modifier = Modifier.heightIn(min = 48.dp)) { Text(if (hasCurve) "Discard" else "Done") }
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
                .clip(RoundedCornerShape(16.dp))
                .background(Palette.Surface),
            rangeDb = HEARING_GRAPH_RANGE_DB,
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
