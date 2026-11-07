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
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
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
import io.github.dragjon.eigentone.data.Repo
import io.github.dragjon.eigentone.data.Sweep
import io.github.dragjon.eigentone.data.SweepMark
import io.github.dragjon.eigentone.dsp.GraphicEq
import io.github.dragjon.eigentone.engine.SweepPlayer
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
    var heardTone by remember { mutableDoubleStateOf(Sweep.hzAt(position)) }
    val hz = Sweep.hzAt(position)
    val room = Sweep.roomFor(state.current.filters)

    DisposableEffect(hearEq) {
        Repo.hearingCheckRunning.value = !hearEq
        onDispose { Repo.hearingCheckRunning.value = false }
    }
    // Leaving the app stops the tone, rather than letting it play on behind everything else.
    androidx.lifecycle.compose.LifecycleEventEffect(androidx.lifecycle.Lifecycle.Event.ON_STOP) { playing = false }
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

    ToolScreen(title = stringResource(R.string.ui_frequency_sweep), onBack = ::leave, spacing = 12.dp) {
        Text(
            stringResource(R.string.ui_a_tone_climbs_from_deep_bass_to_high_tre),
            style = MaterialTheme.typography.bodyMedium,
            color = Palette.TextDim,
        )
        Text(
            GraphicEq.formatFrequency(if (hz >= 1000) kotlin.math.round(hz / 10) * 10 else kotlin.math.round(hz)),
            style = BigReadout,
            color = if (playing) Palette.Lit else Palette.Text,
        )
        ValueTrack(
            label = stringResource(R.string.ui_frequency_sweep),
            readout = GraphicEq.formatFrequency(kotlin.math.round(hz)),
            value = position.toFloat(),
            range = 0f..1f,
            origin = 0f,
            default = 0f,
            onChange = {
                climbing = false
                heardTone = Sweep.hzAt(it.toDouble())
                position = it.toDouble()
            },
            onFinished = {},
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
            ) { Text(if (playing) stringResource(R.string.ui_stop_tone) else stringResource(R.string.ui_start_tone)) }
            FilledTonalButton(
                enabled = playing && position < 1.0,
                onClick = {
                    comparing = false
                    climbing = !climbing
                },
                modifier = Modifier.weight(1f).heightIn(min = 52.dp),
            ) { Text(if (climbing) stringResource(R.string.ui_pause_climb) else stringResource(R.string.ui_climb)) }
        }
        FilledTonalButton(
            enabled = playing,
            onClick = {
                climbing = false
                comparing = !comparing
            },
            modifier = Modifier.fillMaxWidth().heightIn(min = 52.dp),
        ) { Text(if (comparing) stringResource(R.string.ui_stop_comparing) else stringResource(R.string.ui_compare_with_nearby_notes)) }
        if (comparing && playing) {
            Text(
                stringResource(if (heardTone == hz) R.string.ui_sweep_now_your_spot else R.string.ui_sweep_now, GraphicEq.formatFrequency(kotlin.math.round(heardTone))),
                style = MaterialTheme.typography.bodyMedium,
                color = Palette.Text,
            )
        }
        if (failed) {
            Text(stringResource(R.string.ui_this_phone_wouldn_t_play_the_tone), style = MaterialTheme.typography.bodyMedium, color = Palette.Danger)
        }
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            FilledTonalButton(
                enabled = playing,
                onClick = { marks = Sweep.addMark(marks, hz, Sweep.TOO_LOUD_DB) },
                modifier = Modifier.weight(1f).heightIn(min = 64.dp),
            ) { Text(stringResource(R.string.ui_too_loud_here)) }
            FilledTonalButton(
                enabled = playing,
                onClick = { marks = Sweep.addMark(marks, hz, Sweep.TOO_QUIET_DB) },
                modifier = Modifier.weight(1f).heightIn(min = 64.dp),
            ) { Text(stringResource(R.string.ui_too_quiet_here)) }
        }
        SliderSetting(
            title = stringResource(R.string.ui_tone_level),
            tip = Help.sweepLevel,
            readout = String.format(Locale.US, "%.0f dB", levelDb),
            value = levelDb.toFloat(),
            range = Sweep.MIN_LEVEL_DB.toFloat()..Sweep.MAX_LEVEL_DB.toFloat(),
            default = Sweep.DEFAULT_LEVEL_DB.toFloat(),
            onChange = { levelDb = it.toDouble() },
        )
        SwitchSetting(stringResource(R.string.ui_hear_through_my_eq), Help.sweepHearEq, hearEq) { hearEq = it }

        if (marks.isNotEmpty()) {
            SettingsSection(stringResource(R.string.ui_your_marks)) {
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
                        Text(String.format(Locale.US, "%+.0f dB", mark.gainDb), style = Readout, color = Palette.Lit)
                        TextButton(onClick = { marks = marks - mark }) { Text(stringResource(R.string.ui_remove)) }
                    }
                }
            }
            if (marks.size > room) {
                Text(
                    appPlural(R.plurals.ui_room_for_filters, room, room),
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
            ) { Text(stringResource(R.string.ui_add_to_my_sound_2, minOf(marks.size, room))) }
        }
        TextButton(onClick = { leave() }, modifier = Modifier.heightIn(min = 48.dp)) {
            Text(if (marks.isEmpty()) stringResource(R.string.ui_close) else stringResource(R.string.ui_discard_marks))
        }
    }
}
