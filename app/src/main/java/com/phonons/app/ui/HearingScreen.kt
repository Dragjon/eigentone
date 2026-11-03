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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.phonons.app.data.CustomShape
import com.phonons.app.data.HearingResult
import com.phonons.app.data.HearingTest
import com.phonons.app.data.Repo
import com.phonons.app.dsp.GraphicEq
import com.phonons.app.dsp.GraphicShape
import com.phonons.app.engine.TonePlayer
import java.util.Locale

private enum class HearingStep { INTRO, TONES, RESULT }

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
                    LaunchedEffect(answers) {
                        // A short pause, so a tone can't be mistaken for the sound of the tap.
                        kotlinx.coroutines.delay(TONE_GAP_MS)
                        test.hz?.let { hz -> player.play(hz, test.levelDb, test.ear!!) }
                    }
                    Tones(
                        test = test,
                        onPlayAgain = { test.hz?.let { hz -> player.play(hz, test.levelDb, test.ear!!) } },
                        onAnswer = { heard ->
                            player.stop()
                            test.answer(heard)
                            answers++
                            if (test.isDone) {
                                result = test.result()
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
    Text("Hearing check", style = MaterialTheme.typography.headlineSmall, color = Palette.Text)
    Text(
        "You'll hear quiet tones in one ear at a time and say whether you heard them. It takes about 3 minutes, and it makes a gentle curve that lifts what you hear least well.",
        style = MaterialTheme.typography.bodyLarge,
        color = Palette.TextDim,
    )
    SettingsSection("Before you start") {
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
        "This isn't a medical hearing test and can't diagnose anything. If you're worried about your hearing, see a professional.",
        style = MaterialTheme.typography.bodySmall,
        color = Palette.TextDim,
    )
    Button(onClick = onStart, modifier = Modifier.fillMaxWidth().heightIn(min = 52.dp)) { Text("Start") }
    TextButton(onClick = onCancel, modifier = Modifier.heightIn(min = 48.dp)) { Text("Not now") }
}

@Composable
private fun Tones(test: HearingTest, onPlayAgain: () -> Unit, onAnswer: (Boolean) -> Unit, onCancel: () -> Unit) {
    val ear = test.ear ?: return
    Text("Listen with your ${ear.label} ear", style = MaterialTheme.typography.titleLarge, color = Palette.Text)
    LinearProgressIndicator(
        progress = { test.progress },
        modifier = Modifier.fillMaxWidth(),
        color = Palette.Amber,
        trackColor = Palette.Track,
        drawStopIndicator = {},
    )
    Text(
        "Three short beeps just played. They can be very faint, and it's normal to miss many of them. Answer honestly rather than guessing.",
        style = MaterialTheme.typography.bodyMedium,
        color = Palette.TextDim,
    )
    Button(onClick = { onAnswer(true) }, modifier = Modifier.fillMaxWidth().heightIn(min = 64.dp)) { Text("I heard it") }
    FilledTonalButton(onClick = { onAnswer(false) }, modifier = Modifier.fillMaxWidth().heightIn(min = 64.dp)) { Text("I didn't hear it") }
    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        TextButton(onClick = onPlayAgain, modifier = Modifier.heightIn(min = 48.dp)) { Text("Play again") }
        TextButton(onClick = onCancel, modifier = Modifier.heightIn(min = 48.dp)) { Text("Stop") }
    }
}

@Composable
private fun Result(result: HearingResult, onClose: () -> Unit) {
    val points = remember(result) { result.shapePoints() }
    val ears = remember(result) { result.earCorrection() }
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

    Text("Your curve", style = MaterialTheme.typography.headlineSmall, color = Palette.Text)
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
        SettingsSection("Changes") {
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
                    tone.copy(graphicDb = GraphicEq.layered(tone.graphicDb, graphic, tone.graphicBands))
                }
                Repo.commit()
                onClose()
            },
            modifier = Modifier.fillMaxWidth().heightIn(min = 52.dp),
        ) { Text("Add to my sound") }
        FilledTonalButton(
            onClick = {
                save()
                onClose()
            },
            modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp),
        ) { Text("Only save it to Your shapes") }
    }
    if (ears != null) {
        Text(
            "Your ears differed by up to ${result.largestEarDifferenceDb().toInt()} dB at some frequencies, which can also come from fit. Phonons can close half of that gap, frequency by frequency, with a separate EQ for each ear.",
            style = MaterialTheme.typography.bodyMedium,
            color = Palette.Text,
        )
        FilledTonalButton(
            enabled = !earsApplied,
            onClick = {
                Repo.update { it.copy(earCorrection = ears) }
                earsApplied = true
            },
            modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp),
        ) { Text(if (earsApplied) "Each ear is now corrected" else "Correct each ear") }
    }
    TextButton(onClick = onClose, modifier = Modifier.heightIn(min = 48.dp)) { Text(if (hasCurve) "Discard" else "Done") }
}

private const val NAME = "My hearing"
private const val TONE_GAP_MS = 400L
