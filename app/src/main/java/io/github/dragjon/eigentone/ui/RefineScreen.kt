package io.github.dragjon.eigentone.ui

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
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
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import io.github.dragjon.eigentone.R
import io.github.dragjon.eigentone.data.Choice
import io.github.dragjon.eigentone.data.Preset
import io.github.dragjon.eigentone.data.Refiner
import io.github.dragjon.eigentone.data.Repo
import io.github.dragjon.eigentone.data.SoundFinder
import io.github.dragjon.eigentone.dsp.GraphicEq
import java.util.Locale
import kotlin.math.ln
import kotlin.random.Random

private enum class RefineStep { INTRO, COMPARE, FINAL, NOTHING, KEPT, APPLIED }

/**
 * Fine-tunes the current sound from many A or B answers, see [Refiner]. The live sound changes while it
 * runs, and goes back exactly as it was unless the refined sound wins a blind comparison at the end.
 */
@Composable
fun RefineScreen(onSetEnabled: (Boolean) -> Unit, onClose: () -> Unit) {
    val state by Repo.state.collectAsStateWithLifecycle()
    val output by Repo.output.collectAsStateWithLifecycle()
    // Captured once, so leaving can restore precisely what was there before.
    val before = remember { Repo.state.value }
    val start = before.current
    val refiner = remember { Refiner(System.nanoTime()) }
    var step by remember { mutableStateOf(RefineStep.INTRO) }
    var progress by remember { mutableIntStateOf(0) }
    var kept by remember { mutableStateOf(false) }

    DisposableEffect(step) {
        Repo.comparing.value = step == RefineStep.COMPARE || step == RefineStep.FINAL
        onDispose { Repo.comparing.value = false }
    }
    DisposableEffect(Unit) {
        onDispose { if (!kept) Repo.update { it.copy(current = before.current, bypass = before.bypass) } }
    }

    fun restore() {
        Repo.update { it.copy(current = before.current, bypass = before.bypass) }
        if (!before.enabled) onSetEnabled(false)
    }

    fun cancel() {
        if (step == RefineStep.APPLIED) {
            onClose()
            return
        }
        restore()
        onClose()
    }

    /** Plays [change] on top of the starting sound, at the starting sound's loudness. */
    fun hear(change: List<Double>) {
        val tone = Refiner.apply(start.tone, change)
        Repo.edit { SoundFinder.levelMatched(start, tone, start, before.autoHeadroom) }
    }

    fun finishComparing() {
        val result = refiner.result()
        step = if (result.all { it == 0.0 }) {
            restore()
            RefineStep.NOTHING
        } else {
            RefineStep.FINAL
        }
    }

    BackHandler { cancel() }

    val compareProgress = if (step == RefineStep.COMPARE) progress.toFloat() / Refiner.MAX_ROUNDS else null
    ToolScreen(title = stringResource(R.string.ui_refine), onBack = ::cancel, progress = compareProgress) {
        when (step) {
            RefineStep.INTRO -> {
                Text(stringResource(R.string.ui_refine_intro, start.name), style = MaterialTheme.typography.bodyLarge, color = Palette.TextDim)
                Column(
                    Modifier
                        .fillMaxWidth()
                        .clip(PanelShape)
                        .background(Palette.Surface)
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    Text(stringResource(R.string.ui_before_you_start), style = MaterialTheme.typography.titleSmall, color = Palette.Text)
                    Text(stringResource(R.string.ui_play_a_song_you_know_well_and_keep_it_pl), style = MaterialTheme.typography.bodyMedium, color = Palette.Text)
                    Text(stringResource(R.string.ui_set_a_comfortable_volume_and_leave_it_th), style = MaterialTheme.typography.bodyMedium, color = Palette.Text)
                    Text(stringResource(R.string.ui_listening_on, output.label), style = MaterialTheme.typography.bodyMedium, color = Palette.Text)
                }
                Button(
                    onClick = {
                        if (!state.enabled) onSetEnabled(true)
                        Repo.update { it.copy(bypass = false) }
                        step = RefineStep.COMPARE
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(min = 52.dp),
                ) { Text(stringResource(R.string.ui_refine_start)) }
            }

            RefineStep.COMPARE -> refiner.current?.let { pair ->
                var side by remember(progress) { mutableStateOf<Choice?>(null) }
                fun play(choice: Choice) {
                    side = choice
                    hear(if (choice == Choice.B) pair.b else pair.a)
                }
                LaunchedEffect(progress) { play(Choice.A) }
                fun answer(choice: Choice) {
                    refiner.choose(choice)
                    progress++
                    if (refiner.isDone) finishComparing()
                }

                Text(
                    stringResource(R.string.ui_refine_round, refiner.answered + 1, Refiner.MAX_ROUNDS),
                    style = MaterialTheme.typography.bodySmall,
                    color = Palette.TextDim,
                )
                Text(stringResource(R.string.ui_refine_question), style = MaterialTheme.typography.titleLarge, color = Palette.Text)
                Text(stringResource(R.string.ui_refine_hint), style = MaterialTheme.typography.bodyMedium, color = Palette.TextDim)
                NothingPlayingHint()
                val musicActive = rememberMusicActive()
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    SideCard("A", side == Choice.A, musicActive, Modifier.weight(1f)) { play(Choice.A) }
                    SideCard("B", side == Choice.B, musicActive, Modifier.weight(1f)) { play(Choice.B) }
                }
                // One answer button that follows the card being heard, so the choice is always about what's playing.
                val pick = side ?: Choice.A
                Button(
                    onClick = { answer(pick) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(min = 52.dp),
                ) { Text(stringResource(if (pick == Choice.B) R.string.ui_i_prefer_b else R.string.ui_i_prefer_a)) }
                FilledTonalButton(
                    onClick = { answer(Choice.NO_PREFERENCE) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(min = 48.dp),
                ) { Text(stringResource(R.string.ui_i_can_t_hear_a_difference)) }
                TextButton(onClick = ::cancel, modifier = Modifier.heightIn(min = 48.dp)) { Text(stringResource(R.string.ui_stop_and_undo)) }
            }

            RefineStep.FINAL -> {
                val result = remember { refiner.result() }
                // Which side is the refined one is decided once, at random, and never shown.
                val refinedIsA = remember { Random.nextBoolean() }
                var side by remember { mutableStateOf(Choice.A) }
                fun play(choice: Choice) {
                    side = choice
                    val refined = (choice == Choice.A) == refinedIsA
                    hear(if (refined) result else List(result.size) { 0.0 })
                }
                LaunchedEffect(Unit) { play(Choice.A) }
                fun pick(choice: Choice) {
                    if ((choice == Choice.A) == refinedIsA) {
                        // An ordinary edit at the starting sound's level, so the bar can save it or throw it away.
                        Repo.update { it.copy(current = before.current, bypass = before.bypass) }
                        Repo.edit { start.copy(tone = Refiner.apply(start.tone, result)) }
                        Repo.commit()
                        kept = true
                        step = RefineStep.APPLIED
                    } else {
                        restore()
                        step = RefineStep.KEPT
                    }
                }

                Text(stringResource(R.string.ui_refine_final), style = MaterialTheme.typography.headlineSmall, color = Palette.Text)
                Text(stringResource(R.string.ui_refine_final_body), style = MaterialTheme.typography.bodyLarge, color = Palette.TextDim)
                NothingPlayingHint()
                val musicActive = rememberMusicActive()
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    SideCard("A", side == Choice.A, musicActive, Modifier.weight(1f)) { play(Choice.A) }
                    SideCard("B", side == Choice.B, musicActive, Modifier.weight(1f)) { play(Choice.B) }
                }
                Button(
                    onClick = { pick(side) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(min = 52.dp),
                ) { Text(stringResource(if (side == Choice.B) R.string.ui_i_prefer_b else R.string.ui_i_prefer_a)) }
                TextButton(onClick = ::cancel, modifier = Modifier.heightIn(min = 48.dp)) { Text(stringResource(R.string.ui_stop_and_undo)) }
            }

            RefineStep.NOTHING -> Outcome(
                title = stringResource(R.string.ui_refine_nothing),
                body = stringResource(R.string.ui_refine_nothing_body, start.name),
                refiner = refiner,
                change = null,
                onDone = onClose,
            )

            RefineStep.KEPT -> Outcome(
                title = stringResource(R.string.ui_refine_kept),
                body = stringResource(R.string.ui_refine_kept_body),
                refiner = refiner,
                change = null,
                onDone = onClose,
            )

            RefineStep.APPLIED -> Outcome(
                title = stringResource(R.string.ui_refine_applied),
                body = stringResource(R.string.ui_refine_applied_body, start.name),
                refiner = refiner,
                change = refiner.result(),
                onDone = onClose,
            )
        }
    }
}

@Composable
private fun Outcome(title: String, body: String, refiner: Refiner, change: List<Double>?, onDone: () -> Unit) {
    Text(title, style = MaterialTheme.typography.headlineSmall, color = Palette.Text)
    Text(body, style = MaterialTheme.typography.bodyLarge, color = Palette.TextDim)
    if (change != null) {
        Text(stringResource(R.string.ui_refine_change), style = MaterialTheme.typography.titleSmall, color = Palette.TextDim)
        ChangeCurve(change)
    }
    val (same, repeated) = refiner.consistency
    if (repeated > 0) {
        Text(
            if (same == repeated) {
                stringResource(R.string.ui_refine_steady, same, repeated)
            } else {
                stringResource(R.string.ui_refine_unsteady, repeated - same, repeated)
            },
            style = MaterialTheme.typography.bodyMedium,
            color = Palette.TextDim,
        )
    }
    Button(
        onClick = onDone,
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = 52.dp),
    ) { Text(stringResource(R.string.ui_done)) }
}

/** The refinement as a curve over frequency, with its biggest changes labelled. */
@Composable
private fun ChangeCurve(change: List<Double>) {
    val measurer = rememberTextMeasurer()
    val labelStyle = TextStyle(color = Palette.TextDim, fontSize = 10.sp, fontFamily = ReadoutFamily)
    val freqs = Refiner.FREQS
    val description = freqs.indices.filter { change[it] != 0.0 }.joinToString {
        "${GraphicEq.formatFrequency(freqs[it])} ${String.format(Locale.US, "%+.1f dB", change[it])}"
    }
    Canvas(
        Modifier
            .fillMaxWidth()
            .height(140.dp)
            .clip(PanelShape)
            .background(Palette.Surface)
            .semantics { contentDescription = description },
    ) {
        val pad = 12.dp.toPx()
        val bottomLabels = 16.dp.toPx()
        val w = size.width - pad * 2
        val h = size.height - pad * 2 - bottomLabels
        val range = 6.0
        fun xOf(hz: Double) = (pad + ln(hz / 20.0) / ln(20000.0 / 20.0) * w).toFloat()
        fun yOf(db: Double) = (pad + (1 - (db.coerceIn(-range, range) + range) / (2 * range)) * h).toFloat()
        drawLine(Palette.Outline, Offset(pad, yOf(0.0)), Offset(size.width - pad, yOf(0.0)), strokeWidth = 1f)
        val points = (0..120).map { i ->
            val hz = 20.0 * Math.pow(1000.0, i / 120.0)
            val db = run {
                // Straight lines on a log axis between the ten points, held flat beyond them.
                val pts = freqs.zip(change)
                when {
                    hz <= pts.first().first -> pts.first().second
                    hz >= pts.last().first -> pts.last().second
                    else -> {
                        val upper = pts.indexOfFirst { p -> p.first >= hz }
                        val (f0, g0) = pts[upper - 1]
                        val (f1, g1) = pts[upper]
                        g0 + (g1 - g0) * ln(hz / f0) / ln(f1 / f0)
                    }
                }
            }
            Offset(xOf(hz), yOf(db))
        }
        val curve = Path().apply {
            moveTo(points.first().x, points.first().y)
            points.drop(1).forEach { lineTo(it.x, it.y) }
        }
        drawPath(curve, Palette.Lit, style = Stroke(width = 2.dp.toPx(), cap = StrokeCap.Round))
        listOf(100.0 to "100", 1000.0 to "1k", 10000.0 to "10k").forEach { (hz, label) ->
            val measured = measurer.measure(label, labelStyle)
            drawText(measured, topLeft = Offset(xOf(hz) - measured.size.width / 2, size.height - pad - measured.size.height + 4.dp.toPx()))
        }
        // The largest boost and cut, so the size of the change can be read.
        listOfNotNull(
            change.indices.maxByOrNull { change[it] }?.takeIf { change[it] > 0 },
            change.indices.minByOrNull { change[it] }?.takeIf { change[it] < 0 },
        ).forEach { i ->
            val measured = measurer.measure(String.format(Locale.US, "%+.1f", change[i]), labelStyle.copy(color = Palette.Lit))
            val y = yOf(change[i]) + if (change[i] > 0) -measured.size.height - 2.dp.toPx() else 2.dp.toPx()
            drawText(measured, topLeft = Offset((xOf(freqs[i]) - measured.size.width / 2).coerceIn(0f, size.width - measured.size.width), y.coerceIn(0f, size.height - measured.size.height)))
        }
    }
}
