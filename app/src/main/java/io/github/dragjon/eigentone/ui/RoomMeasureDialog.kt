package io.github.dragjon.eigentone.ui

import android.Manifest
import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import io.github.dragjon.eigentone.R
import io.github.dragjon.eigentone.data.OutputInfo
import io.github.dragjon.eigentone.data.Repo
import io.github.dragjon.eigentone.data.RoomMeasurement
import io.github.dragjon.eigentone.dsp.Eq
import io.github.dragjon.eigentone.engine.RoomMeasurer
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch

private sealed interface Step {
    data object Intro : Step
    data class Measuring(val spot: Int) : Step
    data class Between(val done: Int) : Step
    data class Result(val reading: RoomMeasurement.Reading) : Step
    data class Problem(val message: Int) : Step
}

/**
 * Measures a speaker where you listen, with a microphone, and saves a gentle correction for it. Three
 * spots a little apart are averaged, because one exact spot has dips the next one over doesn't. The EQ
 * steps aside while tones play, so the speaker itself is measured.
 */
@Composable
fun RoomMeasureDialog(output: OutputInfo, onDismiss: () -> Unit, onSaved: (String) -> Unit) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val measurer = remember { RoomMeasurer(context) }
    var step by remember { mutableStateOf<Step>(Step.Intro) }
    val progress = remember { mutableFloatStateOf(0f) }
    val readings = remember { mutableListOf<RoomMeasurement.Reading>() }
    var job by remember { mutableStateOf<Job?>(null) }
    val usb = remember { measurer.microphone() != null }

    DisposableEffect(Unit) {
        onDispose {
            job?.cancel()
            Repo.hearingCheckRunning.value = false
        }
    }

    fun measureSpot(spot: Int) {
        step = Step.Measuring(spot)
        progress.floatValue = 0f
        job = scope.launch {
            Repo.hearingCheckRunning.value = true
            val recording = try {
                measurer.measure { progress.floatValue = it }
            } finally {
                Repo.hearingCheckRunning.value = false
            }
            val delay = recording?.let(RoomMeasurement::delayFrames)
            if (recording == null) {
                step = Step.Problem(R.string.ui_measure_failed)
                return@launch
            }
            if (delay == null) {
                step = Step.Problem(R.string.ui_measure_not_heard)
                return@launch
            }
            val reading = RoomMeasurement.read(recording, delay)
            if (RoomMeasurement.tooQuiet(reading)) {
                step = Step.Problem(R.string.ui_measure_too_quiet)
                return@launch
            }
            readings += reading
            step = if (readings.size < SPOTS) Step.Between(readings.size) else Step.Result(RoomMeasurement.combine(readings))
        }
    }

    val permission = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
        if (granted) measureSpot(1) else step = Step.Problem(R.string.ui_measure_needs_mic)
    }

    fun start() {
        readings.clear()
        if (context.checkSelfPermission(Manifest.permission.RECORD_AUDIO) == PackageManager.PERMISSION_GRANTED) {
            measureSpot(1)
        } else {
            permission.launch(Manifest.permission.RECORD_AUDIO)
        }
    }

    AlertDialog(
        onDismissRequest = { if (step !is Step.Measuring) onDismiss() },
        containerColor = Palette.Tooltip,
        title = { Text(stringResource(R.string.ui_measure_title, output.label)) },
        text = {
            Column(Modifier.verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                when (val s = step) {
                    Step.Intro -> {
                        Body(stringResource(R.string.ui_measure_intro))
                        Body(stringResource(if (usb) R.string.ui_measure_usb_mic else R.string.ui_measure_phone_mic))
                    }
                    is Step.Measuring -> {
                        Body(stringResource(R.string.ui_measure_playing, s.spot, SPOTS))
                        LinearProgressIndicator(
                            progress = { progress.floatValue },
                            modifier = Modifier.fillMaxWidth(),
                            color = Palette.Lit,
                            trackColor = Palette.SurfaceHigh,
                        )
                    }
                    is Step.Between -> Body(stringResource(R.string.ui_measure_move, s.done, SPOTS))
                    is Step.Result -> {
                        Body(stringResource(R.string.ui_measure_result))
                        ResultGraph(s.reading)
                        Legend()
                    }
                    is Step.Problem -> Text(stringResource(s.message), style = MaterialTheme.typography.bodyMedium, color = Palette.Danger)
                }
            }
        },
        confirmButton = {
            when (val s = step) {
                Step.Intro -> TextButton(onClick = ::start) { Text(stringResource(R.string.ui_start)) }
                is Step.Between -> TextButton(onClick = { measureSpot(s.done + 1) }) { Text(stringResource(R.string.ui_measure_next_spot)) }
                is Step.Result -> TextButton(onClick = {
                    val name = context.getString(R.string.ui_measure_preset_name, output.label)
                    Repo.importCurve(name, RoomMeasurement.correction(s.reading))
                    Repo.assignToOutput(output.key, Repo.state.value.current.id)
                    onSaved(name)
                }) { Text(stringResource(R.string.ui_measure_use)) }
                is Step.Problem -> TextButton(onClick = ::start) { Text(stringResource(R.string.ui_try_again)) }
                is Step.Measuring -> Unit
            }
        },
        dismissButton = {
            TextButton(onClick = {
                job?.cancel()
                onDismiss()
            }) { Text(stringResource(R.string.ui_cancel)) }
        },
    )
}

@Composable
private fun Body(text: String) {
    Text(text, style = MaterialTheme.typography.bodyMedium, color = Palette.Text)
}

/** What was measured, faint, and the correction that evens it out, bright, from 20 Hz to 20 kHz. */
@Composable
private fun ResultGraph(reading: RoomMeasurement.Reading) {
    val response = remember(reading) { RoomMeasurement.response(reading) }
    val correction = remember(reading) { RoomMeasurement.correction(reading) }
    val description = stringResource(R.string.ui_measure_graph_description)
    Canvas(
        Modifier
            .fillMaxWidth()
            .height(150.dp)
            .clip(KeyShape)
            .background(Palette.Surface)
            .semantics { contentDescription = description },
    ) {
        val range = 15.0
        fun x(hz: Double) = (Eq.logPosition(hz) * size.width).toFloat()
        fun y(db: Double) = ((1 - (db.coerceIn(-range, range) + range) / (2 * range)) * size.height).toFloat()
        drawLine(Palette.Outline, Offset(0f, y(0.0)), Offset(size.width, y(0.0)), strokeWidth = 1f)
        val measured = Path()
        var started = false
        RoomMeasurement.FREQS.forEachIndexed { i, hz ->
            val db = response[i] ?: run { started = false; return@forEachIndexed }
            if (!started) measured.moveTo(x(hz), y(db)) else measured.lineTo(x(hz), y(db))
            started = true
        }
        drawPath(measured, Palette.TextDim, style = Stroke(width = 1.5.dp.toPx(), cap = StrokeCap.Round))
        val fix = Path()
        correction.forEachIndexed { i, p -> if (i == 0) fix.moveTo(x(p.hz), y(p.db)) else fix.lineTo(x(p.hz), y(p.db)) }
        drawPath(fix, Palette.Lit, style = Stroke(width = 2.dp.toPx(), cap = StrokeCap.Round))
    }
}

@Composable
private fun Legend() {
    Row(horizontalArrangement = Arrangement.spacedBy(16.dp), verticalAlignment = Alignment.CenterVertically) {
        LegendItem(Palette.TextDim, stringResource(R.string.ui_measure_legend_measured))
        LegendItem(Palette.Lit, stringResource(R.string.ui_measure_legend_correction))
    }
}

@Composable
private fun LegendItem(color: androidx.compose.ui.graphics.Color, label: String) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Canvas(Modifier.size(width = 16.dp, height = 2.dp)) { drawRect(color) }
        Text(label, style = MaterialTheme.typography.bodySmall, color = Palette.TextDim, modifier = Modifier.padding(start = 6.dp))
    }
}

private const val SPOTS = 3
