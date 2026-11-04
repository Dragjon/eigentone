package com.phonons.app.ui

import com.phonons.app.R
import androidx.compose.ui.res.stringResource
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.phonons.app.data.AppState
import com.phonons.app.data.Compressor
import com.phonons.app.data.CompressorBand
import com.phonons.app.data.Dynamics
import com.phonons.app.data.Repo
import com.phonons.app.dsp.GraphicEq
import java.util.Locale
import kotlin.math.exp
import kotlin.math.ln
import kotlin.math.round

private val BAND_NAMES = listOf(R.string.ui_band_bass, R.string.ui_band_mids, R.string.ui_band_treble)

/** The manual compressor: a starting point, then each band's settings, with the details tucked away. */
@OptIn(ExperimentalLayoutApi::class, ExperimentalMaterial3Api::class)
@Composable
fun CompressorSection(state: AppState) {
    val scheduledNight by Repo.scheduledNight.collectAsStateWithLifecycle()
    val compressor = state.current.tone.compressor
    val nightActive = state.nightMode > 0.001 || scheduledNight
    var selected by remember { mutableIntStateOf(0) }
    var details by remember { mutableStateOf(false) }
    fun change(transform: (Compressor) -> Compressor) = Repo.editTone { it.copy(compressor = transform(it.compressor)) }

    val status by Repo.status.collectAsStateWithLifecycle()
    val tamerParts = buildList {
        val tone = state.current.tone
        if (tone.harshTamer > 0.0) add(stringResource(R.string.ui_harshness_tamer) + " " + "${round(tone.harshTamer * 100).toInt()}%")
        if (tone.boomTamer > 0.0) add(stringResource(R.string.ui_boom_tamer) + " " + "${round(tone.boomTamer * 100).toInt()}%")
    }
    SettingsSection(stringResource(R.string.ui_dynamic_eq), trailing = { InfoButton(Help.dynamicEq) }, summary = foldSummary(tamerParts)) {
        val tone = state.current.tone
        val off = stringResource(R.string.ui_off)
        fun amount(value: Double) = if (value <= 0.0) off else "${round(value * 100).toInt()}%"
        SliderSetting(
            title = stringResource(R.string.ui_harshness_tamer),
            tip = Help.harshTamer,
            readout = amount(tone.harshTamer),
            value = tone.harshTamer.toFloat(),
            range = 0f..1f,
            default = 0f,
            detail = if (status.tamersUnavailable) stringResource(R.string.ui_tamers_unavailable) else null,
            onChange = { v -> Repo.editTone { it.copy(harshTamer = if (v < 0.02f) 0.0 else v.toDouble()) } },
            onFinished = Repo::commit,
        )
        RowDivider()
        SliderSetting(
            title = stringResource(R.string.ui_boom_tamer),
            tip = Help.boomTamer,
            readout = amount(tone.boomTamer),
            value = tone.boomTamer.toFloat(),
            range = 0f..1f,
            default = 0f,
            onChange = { v -> Repo.editTone { it.copy(boomTamer = if (v < 0.02f) 0.0 else v.toDouble()) } },
            onFinished = Repo::commit,
        )
    }
    val compressorParts = if (compressor.enabled) {
        listOf(Dynamics.styles.firstOrNull { it.compressor == compressor }?.name ?: stringResource(R.string.ui_state_on))
    } else {
        emptyList()
    }
    SettingsSection(stringResource(R.string.ui_compressor), summary = foldSummary(compressorParts)) {
        SwitchSetting(
            stringResource(R.string.ui_multiband_compressor),
            Help.compressor,
            compressor.enabled,
            detail = if (compressor.enabled && nightActive) stringResource(R.string.ui_night_mode_is_on_so_it) else null,
        ) { on ->
            change { it.copy(enabled = on) }
            Repo.commit()
        }
        if (!compressor.enabled) return@SettingsSection

        RowDivider()
        FlowRow(
            Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Dynamics.styles.forEach { style ->
                FilterChip(
                    selected = compressor == style.compressor,
                    onClick = {
                        change { style.compressor }
                        Repo.commit()
                    },
                    label = { Text(style.name) },
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = Palette.Lit,
                        selectedLabelColor = Palette.OnLit,
                        labelColor = Palette.Text,
                    ),
                )
            }
        }
        RowDivider()
        SingleChoiceSegmentedButtonRow(
            Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 8.dp),
        ) {
            BAND_NAMES.forEachIndexed { index, name ->
                SegmentedButton(
                    selected = selected == index,
                    onClick = { selected = index },
                    shape = SegmentedButtonDefaults.itemShape(index, BAND_NAMES.size, KeyShape),
                    colors = segmentColors(),
                    icon = {},
                ) { Text(stringResource(name), maxLines = 1) }
            }
        }
        val band = compressor.bands[selected]
        fun changeBand(transform: (CompressorBand) -> CompressorBand) = change { it.withBand(selected, transform(it.bands[selected])) }

        SwitchSetting(stringResource(R.string.ui_compress, stringResource(BAND_NAMES[selected]).lowercase()), Help.compressorBand, band.enabled) { on ->
            changeBand { it.copy(enabled = on) }
            Repo.commit()
        }
        if (band.enabled) {
            TransferCurve(band)
            SliderSetting(
                title = stringResource(R.string.ui_threshold),
                tip = Help.compressorThreshold,
                readout = formatLevel(band.thresholdDb, sign = false),
                value = band.thresholdDb.toFloat(),
                range = Dynamics.THRESHOLD_DB.start.toFloat()..Dynamics.THRESHOLD_DB.endInclusive.toFloat(),
                default = CompressorBand().thresholdDb.toFloat(),
                onChange = { v -> changeBand { it.copy(thresholdDb = halfStep(v)) } },
                onFinished = Repo::commit,
            )
            LogSlider(
                title = stringResource(R.string.ui_ratio),
                tip = Help.compressorRatio,
                readout = String.format(Locale.US, "%.1f : 1", band.ratio),
                value = band.ratio,
                range = Dynamics.RATIO,
                default = CompressorBand().ratio,
                onChange = { v -> changeBand { it.copy(ratio = round(v * 10) / 10) } },
                onFinished = Repo::commit,
            )
            SliderSetting(
                title = stringResource(R.string.ui_makeup),
                tip = Help.compressorMakeup,
                readout = formatLevel(band.makeupDb, sign = true),
                value = band.makeupDb.toFloat(),
                range = Dynamics.MAKEUP_DB.start.toFloat()..Dynamics.MAKEUP_DB.endInclusive.toFloat(),
                default = 0f,
                detail = if (band.makeupDb > 0 && !state.limiter) stringResource(R.string.ui_the_limiter_is_off_so_a) else null,
                onChange = { v -> changeBand { it.copy(makeupDb = halfStep(v)) } },
                onFinished = Repo::commit,
            )
        }
        TextButton(
            onClick = { details = !details },
            modifier = Modifier
                .padding(horizontal = 8.dp)
                .heightIn(min = 48.dp),
        ) { Text(if (details) stringResource(R.string.ui_hide_timing_and_crossovers) else stringResource(R.string.ui_timing_and_crossovers)) }
        if (details) {
            if (band.enabled) {
                LogSlider(
                    title = stringResource(R.string.ui_attack),
                    tip = Help.compressorAttack,
                    readout = formatMs(band.attackMs),
                    value = band.attackMs,
                    range = Dynamics.ATTACK_MS,
                    default = CompressorBand().attackMs,
                    onChange = { v -> changeBand { it.copy(attackMs = round(v * 10) / 10) } },
                    onFinished = Repo::commit,
                )
                LogSlider(
                    title = stringResource(R.string.ui_release),
                    tip = Help.compressorRelease,
                    readout = formatMs(band.releaseMs),
                    value = band.releaseMs,
                    range = Dynamics.RELEASE_MS,
                    default = CompressorBand().releaseMs,
                    onChange = { v -> changeBand { it.copy(releaseMs = round(v)) } },
                    onFinished = Repo::commit,
                )
                SliderSetting(
                    title = stringResource(R.string.ui_knee),
                    tip = Help.compressorKnee,
                    readout = formatLevel(band.kneeDb, sign = false),
                    value = band.kneeDb.toFloat(),
                    range = Dynamics.KNEE_DB.start.toFloat()..Dynamics.KNEE_DB.endInclusive.toFloat(),
                    default = CompressorBand().kneeDb.toFloat(),
                    onChange = { v -> changeBand { it.copy(kneeDb = halfStep(v)) } },
                    onFinished = Repo::commit,
                )
            }
            RowDivider()
            LogSlider(
                title = stringResource(R.string.ui_bass_to_mids),
                tip = Help.compressorCrossover,
                readout = GraphicEq.formatFrequency(compressor.lowCrossoverHz),
                value = compressor.lowCrossoverHz,
                range = Dynamics.LOW_CROSSOVER_HZ,
                default = Compressor().lowCrossoverHz,
                onChange = { v -> change { it.copy(lowCrossoverHz = round(v)) } },
                onFinished = Repo::commit,
            )
            LogSlider(
                title = stringResource(R.string.ui_mids_to_treble),
                tip = Help.compressorCrossover,
                readout = GraphicEq.formatFrequency(compressor.highCrossoverHz),
                value = compressor.highCrossoverHz,
                range = Dynamics.HIGH_CROSSOVER_HZ,
                default = Compressor().highCrossoverHz,
                onChange = { v -> change { it.copy(highCrossoverHz = round(v / 10) * 10) } },
                onFinished = Repo::commit,
            )
        }
    }
}

/** For settings where small values need as much room as large ones, such as attack time. */
@Composable
private fun LogSlider(
    title: String,
    tip: Tip,
    readout: String,
    value: Double,
    range: ClosedFloatingPointRange<Double>,
    default: Double,
    onChange: (Double) -> Unit,
    onFinished: () -> Unit,
) {
    SliderSetting(
        title = title,
        tip = tip,
        readout = readout,
        value = ln(value).toFloat(),
        range = ln(range.start).toFloat()..ln(range.endInclusive).toFloat(),
        default = ln(default).toFloat(),
        onChange = { v -> onChange(exp(v.toDouble()).coerceIn(range)) },
        onFinished = onFinished,
    )
}

/** Level in against level out. The straight line is no change, and the coloured line is this setting. */
@Composable
private fun TransferCurve(band: CompressorBand) {
    val description = stringResource(R.string.ui_compression_curve_description, band.thresholdDb.toInt(), String.format(Locale.US, "%.1f", band.ratio))
    Column(Modifier.padding(horizontal = 16.dp, vertical = 8.dp)) {
        Canvas(
            Modifier
                .fillMaxWidth()
                .height(120.dp)
                .semantics { contentDescription = description },
        ) {
            fun x(db: Double) = ((db - CURVE_FLOOR_DB) / -CURVE_FLOOR_DB * size.width).toFloat()
            fun y(db: Double) = ((1 - (db - CURVE_FLOOR_DB) / -CURVE_FLOOR_DB).coerceIn(0.0, 1.0) * size.height).toFloat()
            drawLine(Palette.Line, Offset(0f, size.height), Offset(size.width, 0f), strokeWidth = 1.dp.toPx())
            drawLine(Palette.Track, Offset(x(band.thresholdDb), 0f), Offset(x(band.thresholdDb), size.height), strokeWidth = 1.dp.toPx())
            val path = Path()
            for (step in 0..CURVE_STEPS) {
                val input = CURVE_FLOOR_DB + -CURVE_FLOOR_DB * step / CURVE_STEPS
                val output = y(band.outputDb(input))
                if (step == 0) path.moveTo(x(input), output) else path.lineTo(x(input), output)
            }
            drawPath(path, Palette.Lit, style = Stroke(width = 2.dp.toPx()))
        }
        Text(stringResource(R.string.ui_level_in_left_to_right_level_out_bottom), style = MaterialTheme.typography.bodySmall, color = Palette.TextDim)
    }
}

private fun halfStep(value: Float): Double = round(value * 2.0) / 2

private fun formatLevel(db: Double, sign: Boolean): String =
    String.format(Locale.US, if (sign) "%+.1f dB" else "%.1f dB", db)

private fun formatMs(ms: Double): String =
    if (ms < 10) String.format(Locale.US, "%.1f ms", ms) else "${ms.toInt()} ms"

private const val CURVE_FLOOR_DB = -60.0
private const val CURVE_STEPS = 60
