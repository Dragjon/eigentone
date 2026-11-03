package com.phonons.app.ui

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.phonons.app.data.EarCorrection
import com.phonons.app.data.Processing
import com.phonons.app.dsp.Eq
import com.phonons.app.dsp.Filter
import kotlin.math.hypot

private const val RANGE_DB = 18.0
private const val CURVE_POINTS = 300
private val GRID_FREQS = listOf(20, 50, 100, 200, 500, 1000, 2000, 5000, 10000, 20000)
private val LABEL_FREQS = setOf(100, 1000, 10000)
private val GRID_DB = listOf(-12, -6, 0, 6, 12)

private fun xOf(freq: Double, width: Float) = (Eq.logPosition(freq) * width).toFloat()
private fun yOf(db: Double, height: Float, range: Double = RANGE_DB) =
    ((range - db.coerceIn(-range, range)) / (2 * range) * height).toFloat()

private fun freqAt(x: Float, width: Float) =
    Eq.logFreq((x / width).toDouble().coerceIn(0.0, 1.0))

private fun dbAt(y: Float, height: Float) =
    (RANGE_DB - y / height * 2 * RANGE_DB).coerceIn(-Eq.MAX_GAIN, Eq.MAX_GAIN)

/**
 * The filter curve with draggable nodes. Sliders below the graph offer the same control, which is
 * what makes this usable with a screen reader.
 */
@Composable
fun EqGraph(
    filters: List<Filter>,
    /** The full processing curve: the preset's filters plus tone dials and loudness. */
    curveFilters: List<Filter>,
    /** The right ear's curve while each ear differs, when [curveFilters] is then the left ear's. */
    rightCurveFilters: List<Filter>? = null,
    /** Read while drawing, so spectrum frames redraw the canvas without recomposing the screen. */
    spectrum: () -> FloatArray?,
    selectedId: Int?,
    dimmed: Boolean,
    onSelect: (Int) -> Unit,
    onDrag: (id: Int, freq: Double, gainDb: Double) -> Unit,
    onDragEnd: () -> Unit,
    modifier: Modifier = Modifier,
    heightDp: Int = 260,
    rangeDb: Double = RANGE_DB,
) {
    val currentFilters by rememberUpdatedState(filters)
    val measurer = rememberTextMeasurer()
    val labelStyle = TextStyle(color = Palette.TextDim, fontSize = 11.sp)
    val accent = if (dimmed) Palette.TextDim else Palette.Amber
    // Curves only change when filters do, so spectrum redraws don't recalculate them.
    val curve = remember(curveFilters) {
        DoubleArray(CURVE_POINTS + 1) { i -> Eq.curveDb(curveFilters, Eq.logFreq(i.toDouble() / CURVE_POINTS)) }
    }
    val rightCurve = remember(rightCurveFilters) {
        rightCurveFilters?.let { right ->
            DoubleArray(CURVE_POINTS + 1) { i -> Eq.curveDb(right, Eq.logFreq(i.toDouble() / CURVE_POINTS)) }
        }
    }
    val rightColour = if (dimmed) Palette.TextDim else Palette.RightEar
    val selectedFilter = filters.firstOrNull { it.id == selectedId && it.enabled }
    val selectedCurve = remember(selectedFilter) {
        selectedFilter?.let { f ->
            DoubleArray(CURVE_POINTS + 1) { i -> Eq.filterDb(f, Eq.logFreq(i.toDouble() / CURVE_POINTS)) }
        }
    }

    Canvas(
        modifier
            .fillMaxWidth()
            .height(heightDp.dp)
            .semantics {
                contentDescription = if (rightCurveFilters == null) {
                    "Equaliser curve. Use the filter controls below to adjust it."
                } else {
                    "Equaliser curves, with a separate curve for each ear."
                }
            }
            .pointerInput(Unit) {
                val touchRadius = 32.dp.toPx()
                awaitEachGesture {
                    val down = awaitFirstDown()
                    val w = size.width.toFloat()
                    val h = size.height.toFloat()
                    val hit = currentFilters
                        .map { f ->
                            val fx = xOf(f.freq, w)
                            val fy = yOf(if (f.type.hasGain) f.gainDb else 0.0, h)
                            f to hypot(fx - down.position.x, fy - down.position.y)
                        }
                        .filter { it.second <= touchRadius }
                        .minByOrNull { it.second }
                        ?.first
                        ?: return@awaitEachGesture

                    down.consume()
                    onSelect(hit.id)
                    var moved = false
                    while (true) {
                        val event = awaitPointerEvent()
                        val change = event.changes.firstOrNull { it.id == down.id } ?: break
                        // Apply before checking for lift: on a slow frame the final move and the
                        // lift arrive together, and skipping it would lose the end of the drag.
                        if (change.position != change.previousPosition) {
                            moved = true
                            onDrag(hit.id, freqAt(change.position.x, w), dbAt(change.position.y, h))
                        }
                        change.consume()
                        if (!change.pressed) break
                    }
                    if (moved) onDragEnd()
                }
            },
    ) {
        val w = size.width
        val h = size.height

        spectrum()?.let { bands ->
            val barWidth = w / bands.size
            bands.forEachIndexed { i, level ->
                if (level > 0f) {
                    val barHeight = level * h
                    drawRect(
                        Palette.TextDim.copy(alpha = 0.22f),
                        topLeft = Offset(i * barWidth, h - barHeight),
                        size = Size(barWidth * 0.8f, barHeight),
                    )
                }
            }
        }

        GRID_FREQS.forEach { freq ->
            val x = xOf(freq.toDouble(), w)
            drawLine(Palette.Line, Offset(x, 0f), Offset(x, h), strokeWidth = 1f)
        }
        val gridDb = GRID_DB.filter { kotlin.math.abs(it) < rangeDb }
        gridDb.forEach { db ->
            val y = yOf(db.toDouble(), h, rangeDb)
            drawLine(
                if (db == 0) Palette.Outline else Palette.Line,
                Offset(0f, y), Offset(w, y), strokeWidth = 1f,
            )
        }

        val zeroY = yOf(0.0, h, rangeDb)
        val curvePath = Path()
        val fill = Path().apply { moveTo(0f, zeroY) }
        curve.forEachIndexed { i, db ->
            val x = w * i / CURVE_POINTS
            val y = yOf(db, h, rangeDb)
            if (i == 0) curvePath.moveTo(x, y) else curvePath.lineTo(x, y)
            fill.lineTo(x, y)
        }
        fill.lineTo(w, zeroY)
        fill.close()

        // Filling under one ear's curve would make it look more important than the other's.
        if (rightCurve == null) {
            drawPath(
                fill,
                Brush.verticalGradient(
                    0f to accent.copy(alpha = 0.28f),
                    0.5f to accent.copy(alpha = 0.06f),
                    1f to accent.copy(alpha = 0.28f),
                ),
            )
        }

        selectedCurve?.let { points ->
            val single = Path()
            points.forEachIndexed { i, db ->
                val x = w * i / CURVE_POINTS
                val y = yOf(db, h)
                if (i == 0) single.moveTo(x, y) else single.lineTo(x, y)
            }
            drawPath(single, Palette.TextDim, style = Stroke(width = 1.5.dp.toPx()))
        }

        drawPath(curvePath, accent, style = Stroke(width = 2.5.dp.toPx()))

        rightCurve?.let { points ->
            val right = Path()
            points.forEachIndexed { i, db ->
                val x = w * i / CURVE_POINTS
                val y = yOf(db, h, rangeDb)
                if (i == 0) right.moveTo(x, y) else right.lineTo(x, y)
            }
            val dash = 6.dp.toPx()
            drawPath(
                right,
                rightColour,
                style = Stroke(width = 2.5.dp.toPx(), pathEffect = PathEffect.dashPathEffect(floatArrayOf(dash, dash))),
            )
        }

        // Labels go over the curve on a backing, so the curve can never make them unreadable.
        fun label(text: String, x: Float, y: Float, style: TextStyle = labelStyle) {
            val measured = measurer.measure(text, style)
            val pad = 3.dp.toPx()
            val left = x.coerceIn(0f, w - measured.size.width - pad * 2)
            val top = y.coerceIn(0f, h - measured.size.height - pad)
            drawRoundRect(
                Palette.Surface.copy(alpha = 0.85f),
                topLeft = Offset(left, top),
                size = Size(measured.size.width + pad * 2, measured.size.height + pad),
                cornerRadius = CornerRadius(pad),
            )
            drawText(measured, topLeft = Offset(left + pad, top + pad / 2))
        }
        LABEL_FREQS.forEach { freq ->
            label(if (freq >= 1000) "${freq / 1000} kHz" else "$freq Hz", xOf(freq.toDouble(), w) + 2f, h)
        }
        gridDb.filter { it != 0 }.forEach { db ->
            label(if (db > 0) "+$db" else "$db", 4f, yOf(db.toDouble(), h, rangeDb) + 2f)
        }
        if (rightCurve != null) {
            val rightStyle = labelStyle.copy(color = rightColour)
            val rightLabel = measurer.measure("Right ear", rightStyle).size.width + 8.dp.toPx()
            val leftLabel = measurer.measure("Left ear", labelStyle).size.width + 8.dp.toPx()
            label("Right ear", w - rightLabel, 4f, rightStyle)
            label("Left ear", w - rightLabel - leftLabel - 4.dp.toPx(), 4f, labelStyle.copy(color = accent))
        }

        filters.forEach { f ->
            val center = Offset(xOf(f.freq, w), yOf(if (f.type.hasGain) f.gainDb else 0.0, h))
            val selected = f.id == selectedId
            val radius = (if (selected) 9.dp else 6.dp).toPx()
            when {
                !f.enabled -> drawCircle(Palette.TextDim, radius, center, style = Stroke(1.5.dp.toPx()))
                selected -> {
                    drawCircle(accent.copy(alpha = 0.25f), radius * 2f, center)
                    drawCircle(accent, radius, center)
                }
                else -> {
                    drawCircle(Palette.Background, radius, center)
                    drawCircle(accent, radius, center, style = Stroke(2.dp.toPx()))
                }
            }
        }
    }
}

/** What ear correction changes, on its own. Zoomed in, because these changes are small next to a whole sound. */
@Composable
fun EarCurvesGraph(ears: EarCorrection, modifier: Modifier = Modifier) {
    val (left, right) = remember(ears) { Processing.earCurves(emptyList(), ears) }
    EqGraph(
        filters = emptyList(),
        curveFilters = left,
        rightCurveFilters = right,
        spectrum = { null },
        selectedId = null,
        dimmed = !ears.enabled,
        onSelect = {},
        onDrag = { _, _, _ -> },
        onDragEnd = {},
        modifier = modifier,
        heightDp = 130,
        rangeDb = EAR_GRAPH_RANGE_DB,
    )
}

private const val EAR_GRAPH_RANGE_DB = 8.0
