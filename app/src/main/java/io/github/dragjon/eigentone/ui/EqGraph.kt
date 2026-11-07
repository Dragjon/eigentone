package io.github.dragjon.eigentone.ui


import io.github.dragjon.eigentone.R
import androidx.compose.ui.res.stringResource
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
import io.github.dragjon.eigentone.data.EarCorrection
import io.github.dragjon.eigentone.data.Processing
import io.github.dragjon.eigentone.dsp.Eq
import io.github.dragjon.eigentone.dsp.Filter
import kotlin.math.hypot

private const val RANGE_DB = 18.0
private const val GHOST_MIN_DIFFERENCE_DB = 0.05
private const val CURVE_POINTS = 300
/** Every step of each decade, as on a published response chart. */
private val GRID_FREQS = listOf(20, 30, 40, 50, 60, 70, 80, 90, 100, 200, 300, 400, 500, 600, 700, 800, 900, 1000, 2000, 3000, 4000, 5000, 6000, 7000, 8000, 9000, 10000, 20000)
private val MAJOR_FREQS = setOf(100, 1000, 10000)
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
    /** The preset as saved, drawn faintly behind the current sound while it's being edited. */
    ghostFilters: List<Filter>? = null,
    /** Measured points to mark as on an audiogram: an X for the left ear and an O for the right. */
    earMarkers: EarCorrection? = null,
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
    /** When this changes, such as the preset, the curves glide to their new shape. */
    morphKey: Any? = null,
    /** Room kept clear at the top left, such as for an info button over the graph, which the ear legend moves past. */
    legendInsetDp: Int = 0,
    /** Another sound's curve to compare with, drawn dashed in plain text colour so it can't be taken for an ear. */
    overlay: List<Filter>? = null,
) {
    val currentFilters by rememberUpdatedState(filters)
    val measurer = rememberTextMeasurer()
    val labelStyle = TextStyle(color = Palette.TextDim, fontSize = 10.sp, fontFamily = ReadoutFamily)
    val accent = when {
        dimmed -> Palette.TextDim
        rightCurveFilters != null -> Palette.LeftEar
        else -> Palette.Lit
    }
    // Curves only change when filters do, so spectrum redraws don't recalculate them.
    val curve = remember(curveFilters) {
        DoubleArray(CURVE_POINTS + 1) { i -> Eq.curveDb(curveFilters, Eq.logFreq(i.toDouble() / CURVE_POINTS)) }
    }
    val rightCurve = remember(rightCurveFilters) {
        rightCurveFilters?.let { right ->
            DoubleArray(CURVE_POINTS + 1) { i -> Eq.curveDb(right, Eq.logFreq(i.toDouble() / CURVE_POINTS)) }
        }
    }
    val shownCurve = rememberMorphed(curve, morphKey)
    val shownRight = rememberMorphed(rightCurve ?: curve, morphKey)
    val rightColour = if (dimmed) Palette.TextDim else Palette.RightEar
    val nodeColour = if (dimmed) Palette.TextDim else Palette.Lit
    // Some edits don't change the curve, such as the compressor, and then there's nothing to show.
    val ghost = remember(ghostFilters, curve, rightCurve) {
        ghostFilters?.let { saved ->
            DoubleArray(CURVE_POINTS + 1) { i -> Eq.curveDb(saved, Eq.logFreq(i.toDouble() / CURVE_POINTS)) }
        }?.takeIf { saved ->
            saved.indices.any { i ->
                kotlin.math.abs(saved[i] - curve[i]) > GHOST_MIN_DIFFERENCE_DB ||
                    (rightCurve != null && kotlin.math.abs(saved[i] - rightCurve[i]) > GHOST_MIN_DIFFERENCE_DB)
            }
        }
    }
    val overlayCurve = remember(overlay) {
        overlay?.let { other -> DoubleArray(CURVE_POINTS + 1) { i -> Eq.curveDb(other, Eq.logFreq(i.toDouble() / CURVE_POINTS)) } }
    }
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
                    appText(R.string.ui_equaliser_curve_use_the_filter_controls)
                } else {
                    appText(R.string.ui_equaliser_curves_with_a_separate_curve)
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
            // Quiet grey, so the spectrum reads as one moving shape behind the curve rather than competing with it.
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
            val major = freq in MAJOR_FREQS
            drawLine(if (major) Palette.Track else Palette.Line, Offset(x, 0f), Offset(x, h), strokeWidth = 1f)
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
        shownCurve.forEachIndexed { i, db ->
            val x = w * i / CURVE_POINTS
            val y = yOf(db, h, rangeDb)
            if (i == 0) curvePath.moveTo(x, y) else curvePath.lineTo(x, y)
            fill.lineTo(x, y)
        }
        fill.lineTo(w, zeroY)
        fill.close()

        ghost?.let { points ->
            val saved = Path()
            points.forEachIndexed { i, db ->
                val x = w * i / CURVE_POINTS
                val y = yOf(db, h, rangeDb)
                if (i == 0) saved.moveTo(x, y) else saved.lineTo(x, y)
            }
            drawPath(saved, Palette.Outline, style = Stroke(width = 1.5.dp.toPx()))
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

        // A faint wash between the curve and 0 dB, strongest far from the line and fading towards it, so how
        // much the EQ changes reads at a glance. Left out when the ears differ, to keep their colours apart.
        if (rightCurve == null) {
            // Full strength by FILL_FULL_DB either way, which everyday curves reach, rather than at the
            // graph's edges, which they rarely do.
            drawPath(
                fill,
                Brush.verticalGradient(
                    0f to accent.copy(alpha = FILL_ALPHA),
                    0.5f to accent.copy(alpha = FILL_ZERO_ALPHA),
                    1f to accent.copy(alpha = FILL_ALPHA),
                    startY = yOf(FILL_FULL_DB, h, rangeDb),
                    endY = yOf(-FILL_FULL_DB, h, rangeDb),
                ),
            )
        }
        overlayCurve?.let { points ->
            val other = Path()
            points.forEachIndexed { i, db ->
                val x = w * i / CURVE_POINTS
                val y = yOf(db, h, rangeDb)
                if (i == 0) other.moveTo(x, y) else other.lineTo(x, y)
            }
            val dash = 5.dp.toPx()
            drawPath(other, Palette.Text, style = Stroke(width = 2.dp.toPx(), pathEffect = PathEffect.dashPathEffect(floatArrayOf(dash, dash * 0.7f))))
        }
        drawPath(curvePath, accent, style = Stroke(width = 2.dp.toPx()))

        if (rightCurve != null) shownRight.let { points ->
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
                style = Stroke(width = 2.dp.toPx(), pathEffect = PathEffect.dashPathEffect(floatArrayOf(dash, dash))),
            )
        }

        earMarkers?.let { ears ->
            val mark = 5.dp.toPx()
            val stroke = 2.dp.toPx()
            ears.left.forEach { point ->
                val c = Offset(xOf(point.hz, w), yOf(point.db, h, rangeDb))
                drawLine(Palette.LeftEar, Offset(c.x - mark, c.y - mark), Offset(c.x + mark, c.y + mark), stroke)
                drawLine(Palette.LeftEar, Offset(c.x - mark, c.y + mark), Offset(c.x + mark, c.y - mark), stroke)
            }
            ears.right.forEach { point ->
                drawCircle(Palette.RightEar, mark, Offset(xOf(point.hz, w), yOf(point.db, h, rangeDb)), style = Stroke(stroke))
            }
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
        if (ghost != null) label(appText(R.string.ui_saved_2), w * 0.42f, 4f)
        if (rightCurve != null) {
            val rightStyle = labelStyle.copy(color = rightColour)
            val rightLabel = measurer.measure(appText(R.string.ui_right_ear), rightStyle).size.width + 8.dp.toPx()
            val mark = 4.dp.toPx()
            val stroke = 1.5.dp.toPx()
            val markY = 12.dp.toPx()
            val inset = legendInsetDp.dp.toPx()
            val leftX = 10.dp.toPx() + inset
            drawLine(accent, Offset(leftX - mark, markY - mark), Offset(leftX + mark, markY + mark), stroke)
            drawLine(accent, Offset(leftX - mark, markY + mark), Offset(leftX + mark, markY - mark), stroke)
            label(appText(R.string.ui_left_ear), 18.dp.toPx() + inset, 4f, labelStyle.copy(color = accent))
            label(appText(R.string.ui_right_ear), w - rightLabel - 18.dp.toPx(), 4f, rightStyle)
            drawCircle(rightColour, mark, Offset(w - 10.dp.toPx(), markY), style = Stroke(stroke))
        }

        filters.forEach { f ->
            val center = Offset(xOf(f.freq, w), yOf(if (f.type.hasGain) f.gainDb else 0.0, h))
            val selected = f.id == selectedId
            val radius = (if (selected) 9.dp else 6.dp).toPx()
            when {
                !f.enabled -> drawCircle(Palette.TextDim, radius, center, style = Stroke(1.5.dp.toPx()))
                selected -> {
                    drawCircle(nodeColour.copy(alpha = 0.25f), radius * 2f, center)
                    drawCircle(nodeColour, radius, center)
                }
                else -> {
                    drawCircle(Palette.Background, radius, center)
                    drawCircle(nodeColour, radius, center, style = Stroke(2.dp.toPx()))
                }
            }
            // A filter on one channel only says which, beside its node.
            if (f.channel != io.github.dragjon.eigentone.dsp.FilterChannel.BOTH) {
                val side = measurer.measure(
                    appText(if (f.channel == io.github.dragjon.eigentone.dsp.FilterChannel.LEFT) R.string.ui_channel_left_short else R.string.ui_channel_right_short),
                    labelStyle.copy(color = nodeColour),
                )
                drawText(side, topLeft = Offset(center.x + radius + 2.dp.toPx(), center.y - radius - side.size.height / 2))
            }
        }
    }
}

/** What ear correction changes, on its own. Zoomed in, because these changes are small next to a whole sound. */
@Composable
fun EarCurvesGraph(ears: EarCorrection, modifier: Modifier = Modifier, rangeDb: Double = EAR_GRAPH_RANGE_DB, markers: Boolean = false) {
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
        rangeDb = rangeDb,
        earMarkers = if (markers) ears else null,
    )
}

private const val EAR_GRAPH_RANGE_DB = 8.0

/** How strong the wash under the main curve is at the graph's top and bottom edges. */
private const val FILL_ALPHA = 0.32f
private const val FILL_ZERO_ALPHA = 0.04f
private const val FILL_FULL_DB = 8.0
