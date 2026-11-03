package com.phonons.app.ui

import androidx.compose.ui.res.stringResource
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
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.KeyboardArrowLeft
import androidx.compose.material.icons.filled.KeyboardArrowRight
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Text
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
import androidx.compose.runtime.remember
import androidx.compose.ui.text.style.TextOverflow
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.phonons.app.dsp.GraphicShape
import com.phonons.app.R
import com.phonons.app.data.Repo
import com.phonons.app.data.Tone
import com.phonons.app.dsp.GraphicEq
import com.phonons.app.dsp.ShapeLibrary
import java.util.Locale

private const val STEP_DB = 0.5

/**
 * Vertical faders you can set one at a time, or sweep a finger across to draw a whole curve. The
 * band readout and buttons below offer the same control for precise changes and screen readers.
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun GraphicEqConsole(tone: Tone, onBrowseShapes: () -> Unit) {
    val bands = tone.graphicBands
    val gains = GraphicEq.normalized(tone.graphicDb, bands)
    var selected by rememberSaveable(bands) { mutableIntStateOf(bands / 2) }
    val band = selected.coerceIn(0, bands - 1)
    val freqs = GraphicEq.frequencies(bands)

    fun setGains(new: List<Double>) = Repo.editTone {
        it.copy(graphicDb = new, graphicBands = bands, shape = it.shape.takeUnless { GraphicEq.isFlat(new) })
    }

    val appState by Repo.state.collectAsStateWithLifecycle()
    // Remembered because matching older sounds against the whole library is too slow to do on every frame.
    val shapeLabel = remember(gains, bands, tone.shape, appState.customShapes) {
        val yours = appState.customShapes.map { custom -> GraphicShape(custom.name, custom.points.map { it.hz to it.db }) }
        tone.faderLabel(yours + GraphicEq.shapes + ShapeLibrary.all.map { it.shape })
    }

    SettingsSection(null) {
        Row(
            Modifier
                .fillMaxWidth()
                .heightIn(min = 48.dp)
                .padding(start = 16.dp, end = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                shapeLabel,
                style = MaterialTheme.typography.titleSmall,
                color = if (shapeLabel == Tone.FLAT_LABEL || shapeLabel == Tone.CUSTOM_LABEL) Palette.TextDim else Palette.Lit,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier
                    .weight(1f)
                    .semantics { contentDescription = "Current shape: $shapeLabel" },
            )
            UndoRedoButtons()
            InfoButton(Help.graphic)
            if (!GraphicEq.isFlat(gains)) {
                TextButton(onClick = {
                    setGains(emptyList())
                    Repo.commit()
                }) { Text(stringResource(R.string.ui_reset)) }
            }
        }
        Faders(
            gains = gains,
            selected = band,
            onSelect = { selected = it },
            // Takes the band count from the faders themselves. A reference to setGains can be remembered
            // across recompositions, which kept the first band count and reset the layout to it.
            onChange = { new ->
                Repo.editTone { it.copy(graphicDb = new, graphicBands = new.size, shape = it.shape.takeUnless { GraphicEq.isFlat(new) }) }
            },
            onFinished = Repo::commit,
        )

        Row(
            Modifier
                .fillMaxWidth()
                .padding(horizontal = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            IconButton(enabled = band > 0, onClick = { selected = band - 1 }) {
                Icon(Icons.Filled.KeyboardArrowLeft, contentDescription = stringResource(R.string.ui_previous_band))
            }
            Column(Modifier.weight(1f), horizontalAlignment = Alignment.CenterHorizontally) {
                Text(GraphicEq.formatFrequency(freqs[band]), style = MaterialTheme.typography.bodyMedium, color = Palette.Text)
                Text(
                    String.format(Locale.US, "%+.1f dB", gains[band]),
                    style = Readout,
                    color = if (gains[band] != 0.0) Palette.Lit else Palette.TextDim,
                )
            }
            IconButton(enabled = band < bands - 1, onClick = { selected = band + 1 }) {
                Icon(Icons.Filled.KeyboardArrowRight, contentDescription = stringResource(R.string.ui_next_band))
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
                Text(stringResource(R.string.ui_bands), style = MaterialTheme.typography.bodyLarge, color = Palette.Text)
                InfoButton(Help.graphicBands)
            }
            SingleChoiceSegmentedButtonRow(Modifier.fillMaxWidth()) {
                GraphicEq.BAND_OPTIONS.forEachIndexed { i, option ->
                    SegmentedButton(
                        selected = bands == option,
                        onClick = {
                            if (bands != option) {
                                val moved = GraphicEq.resample(gains, bands, option)
                                Repo.editTone {
                                    // Changing the layout isn't an edit, so an unedited shape stays that way.
                                    val kept = it.shape?.let { shape -> if (shape.faders == gains) shape.copy(faders = moved) else shape }
                                    it.copy(graphicDb = moved, graphicBands = option, shape = kept)
                                }
                                Repo.commit()
                            }
                        },
                        shape = SegmentedButtonDefaults.itemShape(i, GraphicEq.BAND_OPTIONS.size, KeyShape),
                        colors = segmentColors(),
                        icon = {},
                    ) { Text("$option", maxLines = 1) }
                }
            }
        }
        RowDivider()

        Column(Modifier.padding(top = 4.dp, bottom = 12.dp)) {
            Row(Modifier.padding(start = 16.dp, end = 8.dp), verticalAlignment = Alignment.CenterVertically) {
                Text(stringResource(R.string.ui_shapes), style = MaterialTheme.typography.bodyLarge, color = Palette.Text)
                InfoButton(Help.graphicShapes)
                Spacer(Modifier.weight(1f))
                TextButton(enabled = !GraphicEq.isFlat(gains), onClick = {
                    setGains(GraphicEq.smooth(gains))
                    Repo.commit()
                }) { Text(stringResource(R.string.ui_smooth)) }
                InfoButton(Help.graphicSmooth)
            }
            FlowRow(
                Modifier.padding(horizontal = 16.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                GraphicEq.shapes.forEach { shape ->
                    val shapeGains = GraphicEq.apply(shape, bands)
                    FilterChip(
                        selected = shapeGains == gains,
                        onClick = {
                            Repo.editTone { it.withShape(shape.name, shapeGains, bands) }
                            Repo.commit()
                        },
                        label = { Text(shape.name, maxLines = 1) },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = Palette.Lit,
                            selectedLabelColor = Palette.OnLit,
                            labelColor = Palette.Text,
                        ),
                    )
                }
            }
            FilledTonalButton(
                onClick = onBrowseShapes,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(start = 16.dp, end = 16.dp, top = 8.dp),
            ) { Text("Browse all ${ShapeLibrary.all.size} shapes") }
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
    val labelStyle = TextStyle(color = Palette.TextDim, fontSize = 10.sp, fontFamily = ReadoutFamily)
    val bands = gains.size
    val freqs = GraphicEq.frequencies(bands)
    val labelledBands = freqs.indices.filter { freqs[it] in LABEL_FREQS }

    Canvas(
        Modifier
            .fillMaxWidth()
            .heightIn(min = 196.dp)
            .height(196.dp)
            .padding(horizontal = 12.dp)
            .semantics {
                contentDescription =
                    "$bands band faders. Use the band controls below to change them one at a time."
            }
            .pointerInput(bands) {
                val bottomLabels = LABEL_SPACE_DP.dp.toPx()
                val gutter = DB_GUTTER_DP.dp.toPx()
                fun bandAt(x: Float) = (((x - gutter) / (size.width - gutter)) * bands).toInt().coerceIn(0, bands - 1)
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
        val gutter = DB_GUTTER_DP.dp.toPx()
        val slot = (size.width - gutter) / bands
        val rail = 2.dp.toPx()
        // Caps are as wide as the layout allows, so they read as faders even with 31 bands.
        val capWidth = (slot * 0.76f).coerceAtMost(22.dp.toPx())
        val capHeight = 12.dp.toPx()
        val yOf = { db: Double -> ((1 - db / GraphicEq.MAX_DB) / 2 * height).toFloat() }
        val zero = yOf(0.0)

        listOf(-10.0, -5.0, 5.0, 10.0).forEach { db ->
            drawLine(Palette.Line, Offset(gutter, yOf(db)), Offset(size.width, yOf(db)), strokeWidth = 1f)
        }
        drawLine(Palette.Outline, Offset(gutter, zero), Offset(size.width, zero), strokeWidth = 1f)

        gains.forEachIndexed { i, gain ->
            val centre = gutter + slot * i + slot / 2
            val isSelected = i == selected
            drawRect(
                if (isSelected) Palette.Outline else Palette.Track,
                topLeft = Offset(centre - rail / 2, 0f),
                size = Size(rail, height),
            )
            val top = minOf(zero, yOf(gain))
            val bottom = maxOf(zero, yOf(gain))
            if (bottom - top > 0.5f) {
                drawRect(
                    Palette.Lit,
                    topLeft = Offset(centre - rail / 2, top),
                    size = Size(rail, bottom - top),
                )
            }
            val width = if (isSelected) capWidth.coerceAtLeast(14.dp.toPx()) else capWidth
            val tall = if (isSelected) capHeight + 6.dp.toPx() else capHeight
            val y = yOf(gain)
            if (isSelected) {
                drawRoundRect(
                    Palette.Surface,
                    topLeft = Offset(centre - width / 2 - 2.dp.toPx(), y - tall / 2 - 2.dp.toPx()),
                    size = Size(width + 4.dp.toPx(), tall + 4.dp.toPx()),
                    cornerRadius = CornerRadius(3.dp.toPx()),
                )
            }
            drawRoundRect(
                if (isSelected) Palette.Text else Palette.Lit,
                topLeft = Offset(centre - width / 2, y - tall / 2),
                size = Size(width, tall),
                cornerRadius = CornerRadius(2.dp.toPx()),
            )
            drawRect(
                Palette.OnLit,
                topLeft = Offset(centre - width / 2 + 1.dp.toPx(), y - 0.5.dp.toPx()),
                size = Size(width - 2.dp.toPx(), 1.dp.toPx()),
            )
        }

        listOf(10.0, 0.0, -10.0).forEach { db ->
            val text = if (db > 0) "+${db.toInt()}" else "${db.toInt()}"
            val measured = measurer.measure(text, labelStyle)
            drawText(measured, topLeft = Offset(0f, yOf(db) - measured.size.height / 2))
        }

        labelledBands.forEach { i ->
            val text = GraphicEq.formatFrequency(freqs[i]).replace(" kHz", "k").replace(" Hz", "")
            val measured = measurer.measure(text, labelStyle)
            val centre = gutter + slot * i + slot / 2
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
private const val DB_GUTTER_DP = 24
private val LABEL_FREQS = setOf(62.5, 63.0, 250.0, 1000.0, 4000.0, 16000.0)
