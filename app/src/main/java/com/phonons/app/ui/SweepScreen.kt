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
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableDoubleStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.Saver
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.phonons.app.data.Repo
import com.phonons.app.data.Sweep
import com.phonons.app.data.SweepMark
import com.phonons.app.dsp.GraphicEq
import com.phonons.app.engine.SweepPlayer
import java.util.Locale
import kotlinx.coroutines.delay

private const val TICK_MS = 50L
private const val NOTE_MS = 450L
private const val NOTE_GAP_MS = 80L
private const val NOTE_GLIDE_MS = 110L

private val MarksSaver = Saver<List<SweepMark>, DoubleArray>(
    save = { marks -> marks.flatMap { listOf(it.hz, it.gainDb) }.toDoubleArray() },
    restore = { saved -> saved.toList().chunked(2).map { SweepMark(it[0], it[1]) } },
)

/**
 * Plays a tone that climbs through the frequencies, so peaks and dips in the headphones can be heard and
 * marked, then adds filters for them. The EQ steps aside by default, through a flag that isn't saved.
 */
@Composable
fun SweepScreen(onClose: () -> Unit) {
    val context = LocalContext.current
    val state by Repo.state.collectAsStateWithLifecycle()
    val player = remember { SweepPlayer(context) }
    var playing by rememberSaveable { mutableStateOf(false) }
    var climbing by rememberSaveable { mutableStateOf(false) }
    var position by rememberSaveable { mutableDoubleStateOf(0.0) }
    var levelDb by rememberSaveable { mutableDoubleStateOf(Sweep.DEFAULT_LEVEL_DB) }
    var hearEq by rememberSaveable { mutableStateOf(false) }
    var marks by rememberSaveable(stateSaver = MarksSaver) { mutableStateOf(emptyList<SweepMark>()) }
    var failed by remember { mutableStateOf(false) }
    var comparing by rememberSaveable { mutableStateOf(false) }
    var heardTone by remember { mutableDoubleStateOf(0.0) }
    val hz = Sweep.hzAt(position)
    val room = Sweep.roomFor(state.current.filters)

    DisposableEffect(hearEq) {
        Repo.hearingCheckRunning.value = !hearEq
        onDispose { Repo.hearingCheckRunning.value = false }
    }
    DisposableEffect(playing) {
        if (playing && !player.start()) {
            failed = true
            playing = false
        }
        onDispose { if (playing) player.stop() }
    }
    LaunchedEffect(hz, comparing) { if (!comparing) player.hz = hz }
    // Separate notes with short gaps, with the spot itself every other note.
    LaunchedEffect(playing, comparing, hz) {
        if (!playing || !comparing) {
            player.muted = false
            return@LaunchedEffect
        }
        try {
            val tones = Sweep.nearbyTones(hz)
            var next = 0
            while (true) {
                val tone = tones[next % tones.size]
                player.muted = true
                delay(NOTE_GAP_MS)
                player.hz = tone
                delay(NOTE_GLIDE_MS)
                player.muted = false
                heardTone = tone
                delay(NOTE_MS)
                next++
            }
        } finally {
            player.muted = false
        }
    }
    LaunchedEffect(levelDb) { player.levelDb = levelDb }
    LaunchedEffect(playing, climbing) {
        while (playing && climbing) {
            delay(TICK_MS)
            position = (position + TICK_MS / 1000.0 / Sweep.SECONDS).coerceAtMost(1.0)
            if (position >= 1.0) climbing = false
        }
    }

    fun leave() {
        playing = false
        player.stop()
        onClose()
    }
    BackHandler { leave() }

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
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Text("Frequency sweep", style = MaterialTheme.typography.headlineSmall, color = Palette.Text)
            Text(
                "A tone climbs from deep bass to high treble. It should stay about equally loud, so mark anywhere it jumps out or nearly disappears. Keep your volume low, and stop if it's ever uncomfortable.",
                style = MaterialTheme.typography.bodyMedium,
                color = Palette.TextDim,
            )
            Text(
                GraphicEq.formatFrequency(if (hz >= 1000) kotlin.math.round(hz / 10) * 10 else kotlin.math.round(hz)),
                style = MaterialTheme.typography.displaySmall,
                color = if (playing) Palette.Amber else Palette.Text,
            )
            Slider(
                value = position.toFloat(),
                onValueChange = {
                    climbing = false
                    heardTone = Sweep.hzAt(it.toDouble())
                    position = it.toDouble()
                },
                modifier = Modifier.fillMaxWidth(),
            )
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Button(
                    onClick = {
                        failed = false
                        if (playing) {
                            player.stop()
                            climbing = false
                            comparing = false
                        }
                        playing = !playing
                    },
                    modifier = Modifier.weight(1f).heightIn(min = 52.dp),
                ) { Text(if (playing) "Stop tone" else "Start tone") }
                FilledTonalButton(
                    enabled = playing && position < 1.0,
                    onClick = {
                        comparing = false
                        climbing = !climbing
                    },
                    modifier = Modifier.weight(1f).heightIn(min = 52.dp),
                ) { Text(if (climbing) "Pause climb" else "Climb") }
            }
            FilledTonalButton(
                enabled = playing,
                onClick = {
                    climbing = false
                    comparing = !comparing
                },
                modifier = Modifier.fillMaxWidth().heightIn(min = 52.dp),
            ) { Text(if (comparing) "Stop comparing" else "Compare with nearby notes") }
            if (comparing && playing) {
                Text(
                    "Now: ${GraphicEq.formatFrequency(kotlin.math.round(heardTone))}${if (heardTone == hz) ", your spot" else ""}. Your spot plays every other note. If it stands out from the notes beside it, mark it too loud. If it sinks below them, mark it too quiet.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = Palette.Text,
                )
            }
            if (failed) {
                Text("This phone wouldn't play the tone.", style = MaterialTheme.typography.bodyMedium, color = Palette.Danger)
            }
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                FilledTonalButton(
                    enabled = playing,
                    onClick = { marks = Sweep.addMark(marks, hz, Sweep.TOO_LOUD_DB) },
                    modifier = Modifier.weight(1f).heightIn(min = 64.dp),
                ) { Text("Too loud here") }
                FilledTonalButton(
                    enabled = playing,
                    onClick = { marks = Sweep.addMark(marks, hz, Sweep.TOO_QUIET_DB) },
                    modifier = Modifier.weight(1f).heightIn(min = 64.dp),
                ) { Text("Too quiet here") }
            }
            SliderSetting(
                title = "Tone level",
                tip = Help.sweepLevel,
                readout = String.format(Locale.US, "%.0f dB", levelDb),
                value = levelDb.toFloat(),
                range = Sweep.MIN_LEVEL_DB.toFloat()..Sweep.MAX_LEVEL_DB.toFloat(),
                default = Sweep.DEFAULT_LEVEL_DB.toFloat(),
                onChange = { levelDb = it.toDouble() },
            )
            SwitchSetting("Hear through my EQ", Help.sweepHearEq, hearEq) { hearEq = it }

            if (marks.isNotEmpty()) {
                SettingsSection("Your marks") {
                    marks.forEachIndexed { index, mark ->
                        if (index > 0) RowDivider()
                        Row(
                            Modifier
                                .fillMaxWidth()
                                .heightIn(min = 48.dp)
                                .padding(start = 16.dp, end = 8.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Text(
                                GraphicEq.formatFrequency(kotlin.math.round(mark.hz)),
                                style = MaterialTheme.typography.bodyMedium,
                                color = Palette.Text,
                                modifier = Modifier.weight(1f),
                            )
                            Text(String.format(Locale.US, "%+.0f dB", mark.gainDb), style = Readout, color = Palette.Amber)
                            TextButton(onClick = { marks = marks - mark }) { Text("Remove") }
                        }
                    }
                }
                if (marks.size > room) {
                    Text(
                        "This preset only has room for $room more ${if (room == 1) "filter" else "filters"}, so the rest won't be added.",
                        style = MaterialTheme.typography.bodySmall,
                        color = Palette.Danger,
                    )
                }
                Button(
                    enabled = room > 0,
                    onClick = {
                        Repo.edit { it.copy(filters = Sweep.addTo(it.filters, marks)) }
                        Repo.commit()
                        leave()
                    },
                    modifier = Modifier.fillMaxWidth().heightIn(min = 52.dp),
                ) { Text("Add ${minOf(marks.size, room)} to my sound") }
            }
            TextButton(onClick = { leave() }, modifier = Modifier.heightIn(min = 48.dp)) {
                Text(if (marks.isEmpty()) "Close" else "Discard marks")
            }
        }
    }
}
