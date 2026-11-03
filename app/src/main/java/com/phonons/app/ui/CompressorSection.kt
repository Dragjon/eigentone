package com.phonons.app.ui

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
import androidx.compose.material3.TextButton
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

private val BAND_NAMES = listOf("Bass", "Mids", "Treble")

/** The manual compressor: a starting point, then each band's settings, with the details tucked away. */
@OptIn(ExperimentalLayoutApi::class, ExperimentalMaterial3Api::class)
@Composable
fun CompressorSection(state: AppState) {
    val scheduledNight by Repo.scheduledNight.collectAsStateWithLifecycle()
    val compressor = state.compressor
    val nightActive = state.nightMode > 0.001 || scheduledNight
    var selected by remember { mutableIntStateOf(0) }
    var details by remember { mutableStateOf(false) }
    fun change(transform: (Compressor) -> Compressor) = Repo.update { it.copy(compressor = transform(it.compressor)) }

    SettingsSection("Compressor") {
        SwitchSetting(
            "Multiband compressor",
            Help.compressor,
            compressor.enabled,
            detail = if (compressor.enabled && nightActive) "Night mode is on, so it's used instead." else null,
        ) { on -> change { it.copy(enabled = on) } }
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
                    onClick = { change { style.compressor } },
                    label = { Text(style.name) },
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = Palette.Amber,
                        selectedLabelColor = Palette.OnAmber,
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
                    shape = SegmentedButtonDefaults.itemShape(index, BAND_NAMES.size),
                    colors = segmentColors(),
                    icon = {},
                ) { Text(name, maxLines = 1) }
            }
        }
        val band = compressor.bands[selected]
        fun changeBand(transform: (CompressorBand) -> CompressorBand) = change { it.withBand(selected, transform(it.bands[selected])) }

        SwitchSetting("Compress ${BAND_NAMES[selected].lowercase()}", Help.compressorBand, band.enabled) { on ->
            changeBand { it.copy(enabled = on) }
        }
        if (band.enabled) {
            TransferCurve(band)
            SliderSetting(
                title = "Threshold",
                tip = Help.compressorThreshold,
                readout = formatLevel(band.thresholdDb, sign = false),
                value = band.thresholdDb.toFloat(),
                range = Dynamics.THRESHOLD_DB.start.toFloat()..Dynamics.THRESHOLD_DB.endInclusive.toFloat(),
                default = CompressorBand().thresholdDb.toFloat(),
                onChange = { v -> changeBand { it.copy(thresholdDb = halfStep(v)) } },
            )
            LogSlider(
                title = "Ratio",
                tip = Help.compressorRatio,
                readout = String.format(Locale.US, "%.1f : 1", band.ratio),
                value = band.ratio,
                range = Dynamics.RATIO,
                default = CompressorBand().ratio,
                onChange = { v -> changeBand { it.copy(ratio = round(v * 10) / 10) } },
            )
            SliderSetting(
                title = "Makeup",
                tip = Help.compressorMakeup,
                readout = formatLevel(band.makeupDb, sign = true),
                value = band.makeupDb.toFloat(),
                range = Dynamics.MAKEUP_DB.start.toFloat()..Dynamics.MAKEUP_DB.endInclusive.toFloat(),
                default = 0f,
                detail = if (band.makeupDb > 0 && !state.limiter) "The limiter is off, so a lot of makeup can distort." else null,
                onChange = { v -> changeBand { it.copy(makeupDb = halfStep(v)) } },
            )
        }
        TextButton(
            onClick = { details = !details },
            modifier = Modifier
                .padding(horizontal = 8.dp)
                .heightIn(min = 48.dp),
        ) { Text(if (details) "Hide timing and crossovers" else "Timing and crossovers") }
        if (details) {
            if (band.enabled) {
                LogSlider(
                    title = "Attack",
                    tip = Help.compressorAttack,
                    readout = formatMs(band.attackMs),
                    value = band.attackMs,
                    range = Dynamics.ATTACK_MS,
                    default = CompressorBand().attackMs,
                    onChange = { v -> changeBand { it.copy(attackMs = round(v * 10) / 10) } },
                )
                LogSlider(
                    title = "Release",
                    tip = Help.compressorRelease,
                    readout = formatMs(band.releaseMs),
                    value = band.releaseMs,
                    range = Dynamics.RELEASE_MS,
                    default = CompressorBand().releaseMs,
                    onChange = { v -> changeBand { it.copy(releaseMs = round(v)) } },
                )
                SliderSetting(
                    title = "Knee",
                    tip = Help.compressorKnee,
                    readout = formatLevel(band.kneeDb, sign = false),
                    value = band.kneeDb.toFloat(),
                    range = Dynamics.KNEE_DB.start.toFloat()..Dynamics.KNEE_DB.endInclusive.toFloat(),
                    default = CompressorBand().kneeDb.toFloat(),
                    onChange = { v -> changeBand { it.copy(kneeDb = halfStep(v)) } },
                )
            }
            RowDivider()
            LogSlider(
                title = "Bass to mids",
                tip = Help.compressorCrossover,
                readout = GraphicEq.formatFrequency(compressor.lowCrossoverHz),
                value = compressor.lowCrossoverHz,
                range = Dynamics.LOW_CROSSOVER_HZ,
                default = Compressor().lowCrossoverHz,
                onChange = { v -> change { it.copy(lowCrossoverHz = round(v)) } },
            )
            LogSlider(
                title = "Mids to treble",
                tip = Help.compressorCrossover,
                readout = GraphicEq.formatFrequency(compressor.highCrossoverHz),
                value = compressor.highCrossoverHz,
                range = Dynamics.HIGH_CROSSOVER_HZ,
                default = Compressor().highCrossoverHz,
                onChange = { v -> change { it.copy(highCrossoverHz = round(v / 10) * 10) } },
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
) {
    SliderSetting(
        title = title,
        tip = tip,
        readout = readout,
        value = ln(value).toFloat(),
        range = ln(range.start).toFloat()..ln(range.endInclusive).toFloat(),
        default = ln(default).toFloat(),
        onChange = { v -> onChange(exp(v.toDouble()).coerceIn(range)) },
    )
}

/** Level in against level out. The straight line is no change, and the coloured line is this setting. */
@Composable
private fun TransferCurve(band: CompressorBand) {
    val description = "Compression curve. Levels above ${band.thresholdDb.toInt()} dB are reduced at ${String.format(Locale.US, "%.1f", band.ratio)} to 1."
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
            drawPath(path, Palette.Amber, style = Stroke(width = 2.dp.toPx()))
        }
        Text("Level in, left to right. Level out, bottom to top.", style = MaterialTheme.typography.bodySmall, color = Palette.TextDim)
    }
}

private fun halfStep(value: Float): Double = round(value * 2.0) / 2

private fun formatLevel(db: Double, sign: Boolean): String =
    String.format(Locale.US, if (sign) "%+.1f dB" else "%.1f dB", db)

private fun formatMs(ms: Double): String =
    if (ms < 10) String.format(Locale.US, "%.1f ms", ms) else "${ms.toInt()} ms"

private const val CURVE_FLOOR_DB = -60.0
private const val CURVE_STEPS = 60
