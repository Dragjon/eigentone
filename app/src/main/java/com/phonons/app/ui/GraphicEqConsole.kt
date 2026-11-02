package com.phonons.app.ui

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.KeyboardArrowLeft
import androidx.compose.material.icons.filled.KeyboardArrowRight
import androidx.compose.material3.FilledTonalIconButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.phonons.app.R
import com.phonons.app.data.Repo
import com.phonons.app.data.Tone
import com.phonons.app.dsp.GraphicEq
import java.util.Locale

private const val STEP_DB = 0.5

/**
 * Vertical faders you can set one at a time, or sweep a finger across to draw a whole curve. The
 * band readout and buttons below offer the same control for precise changes and screen readers.
 */
@Composable
fun GraphicEqConsole(tone: Tone) {
    val bands = tone.graphicBands
    val gains = GraphicEq.normalized(tone.graphicDb, bands)
    var selected by rememberSaveable(bands) { mutableIntStateOf(bands / 2) }
    val band = selected.coerceIn(0, bands - 1)
    val freqs = GraphicEq.frequencies(bands)

    fun setGains(new: List<Double>) = Repo.editTone { it.copy(graphicDb = new, graphicBands = bands) }

    SettingsSection(
        "Graphic EQ",
        trailing = {
            InfoButton(Help.graphic)
            if (!GraphicEq.isFlat(gains)) {
                TextButton(onClick = {
                    setGains(emptyList())
                    Repo.commit()
                }) { Text("Reset") }
            }
        },
    ) {
        Faders(
            gains = gains,
            selected = band,
            onSelect = { selected = it },
            onChange = ::setGains,
            onFinished = Repo::commit,
        )

        Row(
            Modifier
                .fillMaxWidth()
                .padding(horizontal = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            IconButton(enabled = band > 0, onClick = { selected = band - 1 }) {
                Icon(Icons.Filled.KeyboardArrowLeft, contentDescription = "Previous band")
            }
            Column(Modifier.weight(1f), horizontalAlignment = Alignment.CenterHorizontally) {
                Text(GraphicEq.formatFrequency(freqs[band]), style = MaterialTheme.typography.bodyMedium, color = Palette.Text)
                Text(
                    String.format(Locale.US, "%+.1f dB", gains[band]),
                    style = Readout,
                    color = if (gains[band] != 0.0) Palette.Amber else Palette.TextDim,
                )
            }
            IconButton(enabled = band < bands - 1, onClick = { selected = band + 1 }) {
                Icon(Icons.Filled.KeyboardArrowRight, contentDescription = "Next band")
            }
            Spacer(Modifier.weight(0.15f))
            fun nudge(by: Double) {
                setGains(gains.toMutableList().also { it[band] = GraphicEq.snap(it[band] + by) })
                Repo.commit()
            }
            FilledTonalIconButton(onClick = { nudge(-STEP_DB) }) {
                Icon(painterResource(R.drawable.ic_minus), contentDescription = "Lower ${GraphicEq.formatFrequency(freqs[band])}")
            }
            Spacer(Modifier.height(0.dp).padding(start = 8.dp))
            FilledTonalIconButton(onClick = { nudge(STEP_DB) }) {
                Icon(Icons.Filled.Add, contentDescription = "Raise ${GraphicEq.formatFrequency(freqs[band])}")
            }
        }
        RowDivider()

        Column(Modifier.padding(start = 16.dp, end = 16.dp, top = 4.dp, bottom = 12.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("Bands", style = MaterialTheme.typography.bodyLarge, color = Palette.Text)
                InfoButton(Help.graphicBands)
            }
            SingleChoiceSegmentedButtonRow(Modifier.fillMaxWidth()) {
                GraphicEq.BAND_OPTIONS.forEachIndexed { i, option ->
                    SegmentedButton(
                        selected = bands == option,
                        onClick = {
                            if (bands != option) {
                                Repo.editTone {
                                    it.copy(graphicDb = GraphicEq.resample(gains, bands, option), graphicBands = option)
                                }
                                Repo.commit()
                            }
                        },
                        shape = SegmentedButtonDefaults.itemShape(i, GraphicEq.BAND_OPTIONS.size),
                        colors = segmentColors(),
                        icon = {},
                    ) { Text("$option", maxLines = 1) }
                }
            }
        }
        RowDivider()

        Column(Modifier.padding(top = 4.dp, bottom = 12.dp)) {
            Row(Modifier.padding(start = 16.dp, end = 8.dp), verticalAlignment = Alignment.CenterVertically) {
                Text("Shapes", style = MaterialTheme.typography.bodyLarge, color = Palette.Text)
                InfoButton(Help.graphicShapes)
                Spacer(Modifier.weight(1f))
                TextButton(enabled = !GraphicEq.isFlat(gains), onClick = {
                    setGains(GraphicEq.smooth(gains))
                    Repo.commit()
                }) { Text("Smooth") }
                InfoButton(Help.graphicSmooth)
            }
            LazyRow(
                contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 16.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                items(GraphicEq.shapes) { shape ->
                    val shapeGains = GraphicEq.apply(shape, bands)
                    FilterChip(
                        selected = shapeGains == gains,
                        onClick = {
                            setGains(shapeGains)
                            Repo.commit()
                        },
                        label = { Text(shape.name, maxLines = 1) },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = Palette.Amber,
                            selectedLabelColor = Palette.OnAmber,
                            labelColor = Palette.Text,
                        ),
                    )
                }
            }
        }
    }
}

@Composable
private fun Faders(
    gains: List<Double>,
    selected: Int,
    onSelect: (Int) -> Unit,
    onChange: (List<Double>) -> Unit,
    onFinished: () -> Unit,
) {
    val latest by rememberUpdatedState(gains)
    val change by rememberUpdatedState(onChange)
    val select by rememberUpdatedState(onSelect)
    val finished by rememberUpdatedState(onFinished)
    val measurer = rememberTextMeasurer()
    val labelStyle = TextStyle(color = Palette.TextDim, fontSize = 11.sp)
    val bands = gains.size
    val freqs = GraphicEq.frequencies(bands)
    val labelledBands = freqs.indices.filter { freqs[it] in LABEL_FREQS }

    Canvas(
        Modifier
            .fillMaxWidth()
            .heightIn(min = 240.dp)
            .height(240.dp)
            .padding(horizontal = 12.dp)
            .semantics {
                contentDescription =
                    "$bands band faders. Use the band controls below to change them one at a time."
            }
            .pointerInput(bands) {
                val bottomLabels = LABEL_SPACE_DP.dp.toPx()
                fun bandAt(x: Float) = ((x / size.width) * bands).toInt().coerceIn(0, bands - 1)
                fun dbAt(y: Float): Double {
                    val height = size.height - bottomLabels
                    return GraphicEq.snap(GraphicEq.MAX_DB * (1 - 2 * y.coerceIn(0f, height) / height))
                }
                awaitEachGesture {
                    val down = awaitFirstDown()
                    down.consume()
                    var lastBand = bandAt(down.position.x)
                    var lastDb = dbAt(down.position.y)
                    select(lastBand)
                    change(GraphicEq.drawLine(latest, lastBand, lastDb, lastBand, lastDb))
                    while (true) {
                        val event = awaitPointerEvent()
                        val pointer = event.changes.firstOrNull { it.id == down.id } ?: break
                        if (pointer.position != pointer.previousPosition) {
                            val band = bandAt(pointer.position.x)
                            val db = dbAt(pointer.position.y)
                            select(band)
                            change(GraphicEq.drawLine(latest, lastBand, lastDb, band, db))
                            lastBand = band
                            lastDb = db
                        }
                        pointer.consume()
                        if (!pointer.pressed) break
                    }
                    finished()
                }
            },
    ) {
        val bottomLabels = LABEL_SPACE_DP.dp.toPx()
        val height = size.height - bottomLabels
        val slot = size.width / bands
        val barWidth = (slot * 0.5f).coerceIn(3.dp.toPx(), 12.dp.toPx())
        val yOf = { db: Double -> ((1 - db / GraphicEq.MAX_DB) / 2 * height).toFloat() }
        val zero = yOf(0.0)

        listOf(-10.0, -5.0, 5.0, 10.0).forEach { db ->
            drawLine(Palette.Line, Offset(0f, yOf(db)), Offset(size.width, yOf(db)), strokeWidth = 1f)
        }
        drawLine(Palette.Outline, Offset(0f, zero), Offset(size.width, zero), strokeWidth = 1f)

        gains.forEachIndexed { i, gain ->
            val x = slot * i + (slot - barWidth) / 2
            val isSelected = i == selected
            val corner = CornerRadius(barWidth / 2)
            drawRoundRect(
                if (isSelected) Palette.Track else Palette.SurfaceHigh,
                topLeft = Offset(x, 0f),
                size = Size(barWidth, height),
                cornerRadius = corner,
            )
            val top = minOf(zero, yOf(gain))
            val bottom = maxOf(zero, yOf(gain))
            if (bottom - top > 0.5f) {
                drawRoundRect(
                    Palette.Amber,
                    topLeft = Offset(x, top),
                    size = Size(barWidth, bottom - top),
                    cornerRadius = corner,
                )
            }
            drawCircle(
                if (isSelected) Palette.Text else Palette.Amber,
                radius = (barWidth / 2 + 1.dp.toPx()).coerceAtMost(7.dp.toPx()),
                center = Offset(x + barWidth / 2, yOf(gain)),
            )
        }

        labelledBands.forEach { i ->
            val text = GraphicEq.formatFrequency(freqs[i]).replace(" kHz", "k").replace(" Hz", "")
            val measured = measurer.measure(text, labelStyle)
            val centre = slot * i + slot / 2
            drawText(
                measured,
                topLeft = Offset(
                    (centre - measured.size.width / 2).coerceIn(0f, size.width - measured.size.width),
                    height + 4.dp.toPx(),
                ),
            )
        }
    }
}

private const val LABEL_SPACE_DP = 22
private val LABEL_FREQS = setOf(62.5, 63.0, 250.0, 1000.0, 4000.0, 16000.0)
