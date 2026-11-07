package io.github.dragjon.eigentone.ui

import androidx.compose.runtime.withFrameNanos
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.foundation.layout.size
import androidx.compose.ui.semantics.Role
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.selection.selectable
import androidx.compose.runtime.mutableStateOf
import io.github.dragjon.eigentone.data.FaderView
import io.github.dragjon.eigentone.data.BandSwitch
import io.github.dragjon.eigentone.data.FaderLayer
import io.github.dragjon.eigentone.data.Preset
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.ui.platform.LocalContext
import androidx.compose.runtime.DisposableEffect
import androidx.activity.compose.BackHandler
import android.content.pm.ActivityInfo
import android.app.Activity
import kotlinx.coroutines.launch
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.foundation.ScrollState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.background
import kotlin.math.abs
import io.github.dragjon.eigentone.data.Disclosure
import io.github.dragjon.eigentone.data.FaderMode
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.Path
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
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
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
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.runtime.remember
import androidx.compose.ui.text.style.TextOverflow
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import io.github.dragjon.eigentone.dsp.GraphicShape
import io.github.dragjon.eigentone.R
import io.github.dragjon.eigentone.data.Repo
import io.github.dragjon.eigentone.data.Tone
import io.github.dragjon.eigentone.dsp.GraphicEq
import io.github.dragjon.eigentone.dsp.ShapeLibrary
import java.util.Locale

private const val STEP_DB = 0.5

/** A small two-way switch shown as icons, each named for screen readers. */
@Composable
private fun IconPill(options: List<Pair<Int, String>>, selected: Int, onSelect: (Int) -> Unit) {
    Row(
        Modifier
            .clip(KeyShape)
            .background(Palette.SurfaceHigh)
            .selectableGroup(),
    ) {
        options.forEachIndexed { i, (icon, name) ->
            val on = i == selected
            Box(
                Modifier
                    .size(width = PILL_WIDTH_DP.dp, height = PILL_HEIGHT_DP.dp)
                    .background(if (on) Palette.Lit else Palette.SurfaceHigh)
                    .selectable(selected = on, role = Role.RadioButton, onClick = { onSelect(i) })
                    .semantics { contentDescription = name },
                contentAlignment = Alignment.Center,
            ) {
                Icon(painterResource(icon), contentDescription = null, tint = if (on) Palette.OnLit else Palette.TextDim, modifier = Modifier.size(18.dp))
            }
        }
    }
}

/** Names a pill, so its icons have something to explain them. Screen readers already hear each option's name. */
@Composable
private fun PillLabel(label: String) {
    Text(
        label.uppercase(),
        style = PanelLegend.copy(fontSize = 11.sp),
        color = Palette.TextDim,
        modifier = Modifier.clearAndSetSemantics { },
    )
}

private const val PILL_WIDTH_DP = 44
private const val PILL_HEIGHT_DP = 32

/**
 * Vertical faders you can set one at a time, or sweep a finger across to draw a whole curve. The
 * band readout and buttons below offer the same control for precise changes and screen readers.
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun GraphicEqConsole(tone: Tone, presetId: String, onBrowseShapes: () -> Unit, onExpand: () -> Unit) {
    val bands = tone.graphicBands
    val gains = GraphicEq.normalized(tone.graphicDb, bands)
    var selected by rememberSaveable(bands) { mutableIntStateOf(bands / 2) }
    val band = selected.coerceIn(0, bands - 1)
    val freqs = GraphicEq.frequencies(bands)

    fun setGains(new: List<Double>) = Repo.editTone {
        it.copy(graphicDb = new, graphicBands = bands, shape = it.shape.takeUnless { GraphicEq.isFlat(new) })
    }

    val appState by Repo.state.collectAsStateWithLifecycle()
    val base = rememberFaderBase(appState.current, bands)
    val faders = FaderLayer.shown(base, gains)
    // Remembered because matching older sounds against the whole library is too slow to do on every frame.
    // A saved preset is called by its own name: what it was built from only matters while it's being edited.
    val savedUnchanged = Repo.allPresets(appState).firstOrNull { it.id == presetId } == appState.current
    val shapeLabel = remember(gains, bands, tone.shape, appState.customShapes, base, appState.current.name, savedUnchanged) {
        if (savedUnchanged) return@remember appState.current.name
        val yours = appState.customShapes.map { custom -> GraphicShape(custom.name, custom.points.map { it.hz to it.db }) }
        val own = tone.faderLabel(yours + GraphicEq.shapes + ShapeLibrary.all.map { it.shape })
        val name = appState.current.name
        when {
            !FaderLayer.isShaped(base) -> own
            own == Tone.FLAT_LABEL -> name
            // Worded as the top bar words it, so an edited preset reads the same everywhere.
            own == Tone.CUSTOM_LABEL -> appText(R.string.ui_edited, name)
            else -> appText(R.string.ui_shape_on_preset, name, own)
        }
    }

    val showFaderOptions = Disclosure.faderOptions(appState)
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
                    .semantics { contentDescription = appText(R.string.ui_current_shape, shapeLabel) },
            )
            UndoRedoButtons()
            // Without the fader options row, the landscape key joins this one rather than sitting alone.
            if (!showFaderOptions) LandscapeButton(onExpand)
            InfoButton(Help.graphic)
            // Always there, and dimmed when there's nothing to reset, so the keys beside it don't shift.
            TextButton(enabled = !GraphicEq.isFlat(gains), onClick = {
                setGains(emptyList())
                Repo.commit()
            }) { Text(stringResource(R.string.ui_reset)) }
        }
        // Small and right above the faders, so how they behave can be changed without leaving them.
        if (showFaderOptions) Row(
            Modifier
                .fillMaxWidth()
                .padding(start = 12.dp, end = 4.dp, bottom = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp, Alignment.End),
        ) {
            PillLabel(stringResource(R.string.ui_pill_touch))
            IconPill(
                options = listOf(
                    R.drawable.ic_touch_precise to stringResource(R.string.ui_fader_precision),
                    R.drawable.ic_touch_smooth to stringResource(R.string.ui_fader_smooth),
                ),
                selected = FaderMode.entries.indexOf(appState.faderMode),
                onSelect = { i -> Repo.update { it.copy(faderMode = FaderMode.entries[i]) } },
            )
            Spacer(Modifier.width(6.dp))
            PillLabel(stringResource(R.string.ui_pill_view))
            IconPill(
                options = listOf(
                    R.drawable.ic_view_scroll to stringResource(R.string.ui_fader_view_scroll),
                    R.drawable.ic_view_fit to stringResource(R.string.ui_fader_view_full),
                ),
                selected = FaderView.entries.indexOf(appState.faderView),
                onSelect = { i -> Repo.update { it.copy(faderView = FaderView.entries[i]) } },
            )
            LandscapeButton(onExpand)
        }
        Faders(
            gains = faders,
            saved = FaderLayer.shown(base, savedFaders(appState, presetId, bands)),
            area = Modifier.tourTarget(TourStop.FADERS),
            smooth = appState.faderMode == FaderMode.SMOOTH,
            scrollable = appState.faderView == FaderView.SCROLL,
            shown = rememberMorphed(faders.toDoubleArray(), presetId to tone.shape?.name),
            selected = band,
            onSelect = { selected = it },
            // Takes the band count from the faders themselves. A reference to setGains can be remembered
            // across recompositions, which kept the first band count and reset the layout to it.
            onChange = { before, moved -> editFaders(before, moved) },
            onFinished = Repo::commit,
        )

        Row(
            Modifier
                .fillMaxWidth()
                .padding(horizontal = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            IconButton(enabled = band > 0, onClick = { selected = band - 1 }) {
                Icon(Icons.AutoMirrored.Filled.KeyboardArrowLeft, contentDescription = stringResource(R.string.ui_previous_band))
            }
            Column(Modifier.weight(1f), horizontalAlignment = Alignment.CenterHorizontally) {
                Text(GraphicEq.formatFrequency(freqs[band]), style = Readout, color = Palette.Text)
                val actual = FaderLayer.actual(base, gains, band)
                Text(
                    formatLevel(actual),
                    style = Readout,
                    color = if (formatLevel(actual) != formatLevel(0.0)) Palette.Lit else Palette.TextDim,
                )
            }
            IconButton(enabled = band < bands - 1, onClick = { selected = band + 1 }) {
                Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, contentDescription = stringResource(R.string.ui_next_band))
            }
            Spacer(Modifier.weight(0.15f))
            fun nudge(by: Double) {
                editFaders(faders, faders.toMutableList().also { it[band] = GraphicEq.snap(it[band] + by) })
                Repo.commit()
            }
            // Each key is dimmed at the end of the fader's travel, where a press would do nothing.
            FilledTonalIconButton(enabled = faders[band] > -GraphicEq.MAX_DB, onClick = { nudge(-STEP_DB) }) {
                Icon(painterResource(R.drawable.ic_minus), contentDescription = stringResource(R.string.ui_lower, GraphicEq.formatFrequency(freqs[band])))
            }
            Spacer(Modifier.width(8.dp))
            FilledTonalIconButton(enabled = faders[band] < GraphicEq.MAX_DB, onClick = { nudge(STEP_DB) }) {
                Icon(Icons.Filled.Add, contentDescription = stringResource(R.string.ui_raise, GraphicEq.formatFrequency(freqs[band])))
            }
        }
        RowDivider()

        if (Disclosure.bandCount(appState)) {
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
                                Repo.editTone { BandSwitch.switch(it, option) }
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
        }

        Column(Modifier.padding(top = 4.dp, bottom = 12.dp)) {
            Row(Modifier.padding(start = 16.dp, end = 8.dp), verticalAlignment = Alignment.CenterVertically) {
                Text(stringResource(R.string.ui_shapes), style = MaterialTheme.typography.bodyLarge, color = Palette.Text)
                InfoButton(Help.graphicShapes)
                Spacer(Modifier.weight(1f))
                if (Disclosure.smoothing(appState)) {
                    TextButton(enabled = !GraphicEq.isFlat(gains), onClick = {
                        setGains(GraphicEq.smooth(gains))
                        Repo.commit()
                    }) { Text(stringResource(R.string.ui_smooth)) }
                    InfoButton(Help.graphicSmooth)
                }
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
            ) { Text(stringResource(R.string.ui_browse_all_shapes, ShapeLibrary.all.size)) }
        }
    }
}

/** Opens the faders across the whole screen in landscape. */
@Composable
private fun LandscapeButton(onExpand: () -> Unit) {
    IconButton(onClick = onExpand) {
        Icon(painterResource(R.drawable.ic_landscape), contentDescription = stringResource(R.string.ui_expand_faders), tint = Palette.TextDim)
    }
}

/** The faders as saved in the preset, where a double tap returns one. Flat when it was never saved. */
@Composable
private fun savedFaders(state: io.github.dragjon.eigentone.data.AppState, presetId: String, bands: Int): List<Double> =
    remember(state.presets, presetId, bands) {
        val saved = Repo.allPresets(state).firstOrNull { it.id == presetId }?.tone ?: return@remember List(bands) { 0.0 }
        val gains = GraphicEq.normalized(saved.graphicDb, saved.graphicBands)
        if (saved.graphicBands == bands) gains else GraphicEq.resample(gains, saved.graphicBands, bands)
    }

@Composable
private fun Faders(
    gains: List<Double>,
    /** What a double tap returns each fader to. */
    saved: List<Double>,
    /** Draws a curve across the faders. Otherwise a touch stays on the fader it started on. */
    smooth: Boolean,
    /** Precision faders get a finger's width each and scroll when they don't fit. */
    scrollable: Boolean,
    /** Where the bank sits. The full-screen view gives it all the room left. */
    area: Modifier = Modifier,
    /** Fills the height it's given rather than the usual 196dp. */
    fill: Boolean = false,
    /** What to draw, which glides while the sound changes. Touches always work from [gains]. */
    shown: DoubleArray,
    selected: Int,
    onSelect: (Int) -> Unit,
    /** The faders before and after a move, in what they show. */
    onChange: (before: List<Double>, moved: List<Double>) -> Unit,
    onFinished: () -> Unit,
) {
    val latest by rememberUpdatedState(gains)
    val savedGains by rememberUpdatedState(saved)
    val changeOnly by rememberUpdatedState(onChange)
    val view = androidx.compose.ui.platform.LocalView.current
    // A tick when a fader passes 0 dB and a firmer one at the top or bottom of its travel, at most once a move.
    val change: (List<Double>, List<Double>) -> Unit = { before, moved ->
        val limit = GraphicEq.MAX_DB.toFloat()
        val felt = moved.indices.firstOrNull { i ->
            val from = before.getOrElse(i) { 0.0 }
            from != moved[i]
        }
        if (felt != null) {
            Haptics.crossing(view, before.getOrElse(felt) { 0.0 }.toFloat(), moved[felt].toFloat(), 0f, -limit..limit)
        }
        changeOnly(before, moved)
    }
    val select by rememberUpdatedState(onSelect)
    val finished by rememberUpdatedState(onFinished)
    val measurer = rememberTextMeasurer()
    val labelStyle = TextStyle(color = Palette.TextDim, fontSize = 10.sp, fontFamily = ReadoutFamily)
    val bands = gains.size
    val freqs = GraphicEq.frequencies(bands)
    val density = LocalDensity.current

    BoxWithConstraints(area.fillMaxWidth().padding(horizontal = 12.dp)) {
        // Precision gives every fader a finger's width and scrolls sideways when they don't all fit.
        val wide = DB_GUTTER_DP.dp + PRECISION_SLOT_DP.dp * bands
        val scrolling = scrollable && wide > maxWidth
        val scroll = rememberScrollState()
        // Set while a smooth sweep is drawing, so the view doesn't chase the band under the finger.
        var sweeping by remember { mutableStateOf(false) }
        // How fast to slide while a sweep holds near an edge, in pixels a frame. Negative goes left.
        var edgePush by remember { mutableFloatStateOf(0f) }
        val pushing = edgePush != 0f
        LaunchedEffect(pushing) {
            while (edgePush != 0f) {
                withFrameNanos { }
                scroll.dispatchRawDelta(edgePush)
            }
        }
        val contentWidth = if (scrolling) wide else maxWidth
        val contentPx = with(density) { contentWidth.toPx() }
        val viewPx = with(density) { maxWidth.toPx() }
        val labelledBands = if (scrolling || (fill && bands <= FULL_LABEL_BANDS)) freqs.indices.toList() else freqs.indices.filter { freqs[it] in LABEL_FREQS }

        // A band chosen with the arrows below comes into view.
        LaunchedEffect(selected, scrolling, bands) {
            if (!scrolling || sweeping) return@LaunchedEffect
            val gutter = with(density) { DB_GUTTER_DP.dp.toPx() }
            val slot = (contentPx - gutter) / bands
            val left = gutter + slot * selected
            val right = left + slot
            when {
                left - gutter < scroll.value -> scroll.animateScrollTo((left - gutter).toInt())
                right > scroll.value + viewPx -> scroll.animateScrollTo((right - viewPx).toInt())
            }
        }

        // Each fader's level sits above it. With one decimal when it fits, whole dB when it doesn't, and in
        // two staggered rows when even that is wider than a fader.
        val slotPx = (contentPx - with(density) { DB_GUTTER_DP.dp.toPx() }) / bands
        val gap = with(density) { 3.dp.toPx() }
        val fitsDecimal = measurer.measure("+10.5", labelStyle).size.width + gap
        val fitsWhole = measurer.measure("-10", labelStyle).size.width + gap
        // The small fit-to-screen bank has no room for them; the selected band's level shows below it anyway.
        val showValues = scrolling || fill
        val valueRows = if (!showValues) 0 else if (slotPx >= fitsWhole) 1 else 2
        val decimals = slotPx * valueRows >= fitsDecimal
        val valueSpace = VALUE_ROW_DP.dp * valueRows
        val railTopDp = RAIL_TOP_DP.dp + valueSpace

        val bankHeight = if (fill) (maxHeight - if (scrolling) OVERVIEW_HEIGHT_DP.dp else 0.dp).coerceAtLeast(196.dp) else 212.dp + valueSpace
        Column {
            if (scrolling) FaderOverview(shown, selected, scroll, contentPx, viewPx)
            Box(Modifier.fillMaxWidth().then(if (scrolling) Modifier.horizontalScroll(scroll, enabled = !smooth) else Modifier)) {
                Canvas(
                    Modifier
                        .width(contentWidth)
                        .height(bankHeight)
                        .semantics {
                            contentDescription =
                                appText(R.string.ui_band_faders_use_the_band_controls, bands)
                        }
                        .pointerInput(bands, smooth, scrolling, viewPx, railTopDp) {
                            val bottomLabels = LABEL_SPACE_DP.dp.toPx()
                            val gutter = DB_GUTTER_DP.dp.toPx()
                            fun bandAt(x: Float) = (((x - gutter) / (size.width - gutter)) * bands).toInt().coerceIn(0, bands - 1)
                            fun dbAt(y: Float): Double {
                                val top = railTopDp.toPx()
                                val height = size.height - bottomLabels - top
                                return GraphicEq.snap(GraphicEq.MAX_DB * (1 - 2 * (y - top).coerceIn(0f, height) / height))
                            }
                            // A second tap on the same fader soon after the first returns it to the preset.
                            var lastTapBand = -1
                            var lastTapAt = 0L
                            fun tapped(band: Int, at: Long): Boolean {
                                val double = band == lastTapBand && at - lastTapAt < viewConfiguration.doubleTapTimeoutMillis
                                lastTapBand = if (double) -1 else band
                                lastTapAt = at
                                if (double) {
                                    val to = savedGains.getOrElse(band) { 0.0 }
                                    change(latest, latest.toMutableList().also { if (band in it.indices) it[band] = to })
                                }
                                return double
                            }
                            awaitEachGesture {
                                val down = awaitFirstDown()
                                if (smooth) {
                                    // Smooth sets each fader to wherever the finger is, across all of them.
                                    down.consume()
                                    sweeping = true
                                    var lastBand = bandAt(down.position.x)
                                    var lastDb = dbAt(down.position.y)
                                    select(lastBand)
                                    change(latest, GraphicEq.drawLine(latest, lastBand, lastDb, lastBand, lastDb))
                                    var moved = false
                                    // Cleared however the sweep ends, so a cancelled one can't leave the bank sliding.
                                    try {
                                    while (true) {
                                        val pointer = awaitPointerEvent().changes.firstOrNull { it.id == down.id } ?: break
                                        if (scrolling) {
                                            // Near an edge of the view, slide that way, so a sweep can carry on past it.
                                            val edge = EDGE_PAN_DP.dp.toPx()
                                            val inView = pointer.position.x - scroll.value
                                            edgePush = when {
                                                inView < edge && scroll.value > 0 -> -(edge - inView) / edge * MAX_PAN_PX
                                                inView > viewPx - edge && scroll.value < scroll.maxValue -> (inView - (viewPx - edge)) / edge * MAX_PAN_PX
                                                else -> 0f
                                            }
                                        }
                                        if ((pointer.position - down.position).getDistance() > viewConfiguration.touchSlop) moved = true
                                        if (pointer.position != pointer.previousPosition) {
                                            val band = bandAt(pointer.position.x)
                                            val db = dbAt(pointer.position.y)
                                            select(band)
                                            change(latest, GraphicEq.drawLine(latest, lastBand, lastDb, band, db))
                                            lastBand = band
                                            lastDb = db
                                        }
                                        pointer.consume()
                                        if (!pointer.pressed) break
                                    }
                                    } finally {
                                        sweeping = false
                                        edgePush = 0f
                                    }
                                    if (!moved) tapped(lastBand, down.uptimeMillis) else lastTapBand = -1
                                    finished()
                                } else {
                                    // Precision moves one fader from where it already is, so a tap never makes it
                                    // jump. A sideways swipe is left for scrolling, an upward or downward one moves it.
                                    val band = bandAt(down.position.x)
                                    val startDb = latest.getOrElse(band) { 0.0 }
                                    val railHeight = size.height - bottomLabels - railTopDp.toPx()
                                    val capY = (railTopDp.toPx() + (1 - startDb / GraphicEq.MAX_DB) / 2 * railHeight).toFloat()
                                    // Only a touch on the cap moves it, so a finger running along the rails does
                                    // nothing, and an up or down swipe there scrolls the page instead.
                                    val onCap = abs(down.position.y - capY) <= CAP_GRAB_DP.dp.toPx()
                                    val startX = down.position.x
                                    var startY = down.position.y
                                    var dragging = false
                                    val slop = viewConfiguration.touchSlop
                                    while (true) {
                                        val pointer = awaitPointerEvent().changes.firstOrNull { it.id == down.id } ?: break
                                        if (!pointer.pressed) {
                                            if (!dragging && !pointer.isConsumed) {
                                                select(band)
                                                if (tapped(band, down.uptimeMillis)) finished()
                                            }
                                            break
                                        }
                                        if (!dragging) {
                                            val dx = abs(pointer.position.x - startX)
                                            val dy = abs(pointer.position.y - startY)
                                            if (onCap && dy >= slop && dy > dx) {
                                                dragging = true
                                                startY = pointer.position.y
                                                select(band)
                                            } else if (dx >= slop || dy >= slop || pointer.isConsumed) {
                                                break
                                            }
                                        }
                                        if (dragging) {
                                            val db = GraphicEq.snap(startDb - (pointer.position.y - startY) / railHeight * 2 * GraphicEq.MAX_DB)
                                            change(latest, GraphicEq.drawLine(latest, band, db, band, db))
                                            pointer.consume()
                                        }
                                    }
                                    if (dragging) finished()
                                }
                            }
                        },
                ) {
                    val bottomLabels = LABEL_SPACE_DP.dp.toPx()
                    // Room above the rails, so a fader at the top keeps clear of the overview above it.
                    val railTop = railTopDp.toPx()
                    val height = size.height - bottomLabels - railTop
                    val gutter = DB_GUTTER_DP.dp.toPx()
                    val slot = (size.width - gutter) / bands
                    val rail = 2.dp.toPx()
                    // Caps are as wide as the layout allows, so they read as faders even with 31 bands.
                    val capWidth = (slot * 0.76f).coerceAtMost((if (scrolling) 30 else 22).dp.toPx())
                    val capHeight = (if (scrolling) 14 else 12).dp.toPx()
                    val yOf = { db: Double -> (railTop + (1 - db / GraphicEq.MAX_DB) / 2 * height).toFloat() }
                    val zero = yOf(0.0)

                    listOf(-10.0, -5.0, 5.0, 10.0).forEach { db ->
                        drawLine(Palette.Line, Offset(gutter, yOf(db)), Offset(size.width, yOf(db)), strokeWidth = 1f)
                    }
                    drawLine(Palette.Outline, Offset(gutter, zero), Offset(size.width, zero), strokeWidth = 1f)

                    if (smooth) {
                        drawSmoothCurve(shown, selected, gutter, slot, height, zero, yOf)
                    } else shown.forEachIndexed { i, gain ->
                        val centre = gutter + slot * i + slot / 2
                        val isSelected = i == selected
                        drawRect(
                            if (isSelected) Palette.Outline else Palette.Track,
                            topLeft = Offset(centre - rail / 2, railTop),
                            size = Size(rail, height),
                        )
                        val top = minOf(zero, yOf(gain))
                        val bottom = maxOf(zero, yOf(gain))
                        if (bottom - top > 0.5f) {
                            drawRect(Palette.Lit, topLeft = Offset(centre - rail / 2, top), size = Size(rail, bottom - top))
                        }
                        val width = if (isSelected) capWidth.coerceAtLeast(14.dp.toPx()) else capWidth
                        val tall = if (isSelected) capHeight + 6.dp.toPx() else capHeight
                        val y = yOf(gain)
                        // Each lit cap gives off a little amber light, and the one in hand a little more.
                        val glow = Palette.Glow
                        if (glow.alpha > 0f) {
                            val reach = width * if (isSelected) 1.4f else 1f
                            drawCircle(
                                androidx.compose.ui.graphics.Brush.radialGradient(
                                    listOf(glow.copy(alpha = if (isSelected) 0.16f else 0.07f), androidx.compose.ui.graphics.Color.Transparent),
                                    center = Offset(centre, y),
                                    radius = reach,
                                ),
                                radius = reach,
                                center = Offset(centre, y),
                            )
                        }
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

                    labelledBands.forEach { i ->
                        val label = GraphicEq.formatFrequency(freqs[i]).replace(" kHz", "k").replace(" Hz", "")
                        val measured = measurer.measure(label, labelStyle)
                        val centre = gutter + slot * i + slot / 2
                        drawText(
                            measured,
                            topLeft = Offset(
                                (centre - measured.size.width / 2).coerceIn(0f, size.width - measured.size.width),
                                railTop + height + 4.dp.toPx(),
                            ),
                        )
                    }

                    // Each fader's level above its line: brighter when it's set, brightest when selected.
                    if (showValues) shown.forEachIndexed { i, gain ->
                        val text = faderValue(gain, decimals)
                        val style = labelStyle.copy(
                            color = when {
                                i == selected -> Palette.Text
                                text == "0" -> Palette.TextDim.copy(alpha = 0.55f)
                                else -> Palette.Lit
                            },
                        )
                        val measured = measurer.measure(text, style)
                        val centre = gutter + slot * i + slot / 2
                        val row = if (valueRows > 1) i % valueRows else 0
                        drawText(
                            measured,
                            topLeft = Offset(
                                (centre - measured.size.width / 2).coerceIn(gutter, size.width - measured.size.width),
                                VALUE_ROW_DP.dp.toPx() * row,
                            ),
                        )
                    }

                    // The dB scale stays put while the faders scroll beneath it.
                    val pinned = if (scrolling) scroll.value.toFloat() else 0f
                    if (scrolling) drawRect(Palette.Surface, topLeft = Offset(pinned, 0f), size = Size(gutter, size.height))
                    listOf(10.0, 0.0, -10.0).forEach { db ->
                        val label = if (db > 0) "+${db.toInt()}" else "${db.toInt()}"
                        val measured = measurer.measure(label, labelStyle)
                        drawText(measured, topLeft = Offset(pinned, yOf(db) - measured.size.height / 2))
                    }
                }
            }
        }
    }
}

/**
 * The whole fader curve at a glance while the faders scroll, with the part in view outlined. Tapping or
 * dragging on it scrolls there.
 */
@Composable
private fun FaderOverview(shown: DoubleArray, selected: Int, scroll: ScrollState, contentPx: Float, viewPx: Float) {
    val scope = rememberCoroutineScope()
    Canvas(
        Modifier
            .fillMaxWidth()
            .height(OVERVIEW_HEIGHT_DP.dp)
            .padding(bottom = 4.dp)
            .clip(KeyShape)
            .background(Palette.SurfaceHigh)
            .semantics { contentDescription = appText(R.string.ui_fader_overview) }
            .pointerInput(contentPx, viewPx) {
                awaitEachGesture {
                    val down = awaitFirstDown()
                    fun go(x: Float) {
                        val target = (x / size.width * contentPx - viewPx / 2).toInt().coerceIn(0, scroll.maxValue)
                        scope.launch { scroll.scrollTo(target) }
                    }
                    down.consume()
                    go(down.position.x)
                    while (true) {
                        val pointer = awaitPointerEvent().changes.firstOrNull { it.id == down.id } ?: break
                        if (!pointer.pressed) break
                        pointer.consume()
                        go(pointer.position.x)
                    }
                }
            },
    ) {
        val inset = 4.dp.toPx()
        val h = size.height - inset * 2
        fun yOf(db: Double) = (inset + (1 - db / GraphicEq.MAX_DB) / 2 * h).toFloat()
        drawLine(Palette.Outline, Offset(0f, yOf(0.0)), Offset(size.width, yOf(0.0)), strokeWidth = 1f)
        val step = size.width / shown.size
        val curve = Path()
        shown.forEachIndexed { i, db ->
            val x = step * i + step / 2
            if (i == 0) curve.moveTo(0f, yOf(db))
            curve.lineTo(x, yOf(db))
            if (i == shown.lastIndex) curve.lineTo(size.width, yOf(db))
        }
        drawPath(curve, Palette.Lit, style = Stroke(width = 1.5.dp.toPx(), cap = StrokeCap.Round))
        drawCircle(Palette.Text, 3.dp.toPx(), Offset(step * selected + step / 2, yOf(shown.getOrElse(selected) { 0.0 })))
        // The window in view, read here so it follows the scroll as it moves.
        val left = scroll.value / contentPx * size.width
        val width = viewPx / contentPx * size.width
        drawRect(Palette.Text, topLeft = Offset(left, 1f), size = Size(width, size.height - 2f), style = Stroke(width = 1.5.dp.toPx()))
    }
}

/**
 * Smooth mode shows the faders as one continuous curve, since that's what a sweep draws: thin band guides,
 * a filled curve through every band, and small handles on it.
 */
private fun androidx.compose.ui.graphics.drawscope.DrawScope.drawSmoothCurve(
    shown: DoubleArray,
    selected: Int,
    gutter: Float,
    slot: Float,
    height: Float,
    zero: Float,
    yOf: (Double) -> Float,
) {
    val points = shown.mapIndexed { i, gain -> Offset(gutter + slot * i + slot / 2, yOf(gain)) }
    points.forEachIndexed { i, p ->
        val guide = 1.dp.toPx()
        drawRect(if (i == selected) Palette.Outline else Palette.Line, topLeft = Offset(p.x - guide / 2, yOf(GraphicEq.MAX_DB)), size = Size(guide, height))
    }
    val curve = Path().apply {
        moveTo(gutter, points.first().y)
        lineTo(points.first().x, points.first().y)
        for (i in 0 until points.size - 1) {
            // Catmull-Rom through each fader, so the line passes exactly through every value.
            val p0 = points[maxOf(i - 1, 0)]
            val p1 = points[i]
            val p2 = points[i + 1]
            val p3 = points[minOf(i + 2, points.lastIndex)]
            fun clampY(y: Float) = y.coerceIn(yOf(GraphicEq.MAX_DB), yOf(-GraphicEq.MAX_DB))
            cubicTo(
                p1.x + (p2.x - p0.x) / 6, clampY(p1.y + (p2.y - p0.y) / 6),
                p2.x - (p3.x - p1.x) / 6, clampY(p2.y - (p3.y - p1.y) / 6),
                p2.x, p2.y,
            )
        }
        lineTo(size.width, points.last().y)
    }
    val fill = Path().apply {
        addPath(curve)
        lineTo(size.width, zero)
        lineTo(gutter, zero)
        close()
    }
    drawPath(fill, Palette.Lit.copy(alpha = SMOOTH_FILL_ALPHA))
    drawPath(curve, Palette.Lit, style = Stroke(width = 2.dp.toPx(), cap = StrokeCap.Round))
    points.forEachIndexed { i, p ->
        if (i == selected) {
            drawCircle(Palette.Surface, 8.dp.toPx(), p)
            drawCircle(Palette.Text, 6.dp.toPx(), p)
        } else {
            drawCircle(Palette.Lit, 3.dp.toPx(), p)
        }
    }
}

private const val SMOOTH_FILL_ALPHA = 0.16f

private const val LABEL_SPACE_DP = 22
/** How wide each fader is in precision mode, so a finger lands on the right one. */
private const val PRECISION_SLOT_DP = 44
private const val OVERVIEW_HEIGHT_DP = 44
/** How far from the cap's centre a touch still grabs it, which makes the target 48dp tall. */
private const val CAP_GRAB_DP = 24
/** Clear space above the rails, a cap's grab reach, so a fader at the top never reaches the overview. */
private const val RAIL_TOP_DP = 16
/** How close to the edge a sweep has to get before the faders slide, and how fast they go at the very edge. */
private const val EDGE_PAN_DP = 48
private const val MAX_PAN_PX = 22f
private const val FULL_LABEL_BANDS = 15

/**
 * The faders across the whole screen, turned sideways, so each one is wide and tall enough to set exactly.
 * It changes the same sound as the console, and undo works the same way.
 */
@Composable
fun ExpandedFaders(onClose: () -> Unit) {
    val state by Repo.state.collectAsStateWithLifecycle()
    val tone = state.current.tone
    val bands = tone.graphicBands
    val gains = GraphicEq.normalized(tone.graphicDb, bands)
    val base = rememberFaderBase(state.current, bands)
    val faders = FaderLayer.shown(base, gains)
    val freqs = GraphicEq.frequencies(bands)
    var selected by rememberSaveable(bands) { mutableIntStateOf(bands / 2) }
    val band = selected.coerceIn(0, bands - 1)
    val activity = LocalContext.current as? Activity

    DisposableEffect(activity) {
        val before = activity?.requestedOrientation
        activity?.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_SENSOR_LANDSCAPE
        onDispose { if (activity != null && before != null) activity.requestedOrientation = before }
    }
    BackHandler { onClose() }

    Column(
        Modifier
            .fillMaxSize()
            .background(Palette.Background)
            .windowInsetsPadding(WindowInsets.safeDrawing),
    ) {
        Row(
            Modifier
                .fillMaxWidth()
                .background(Palette.Bar)
                .heightIn(min = 56.dp)
                .padding(start = 4.dp, end = 12.dp, top = 8.dp, bottom = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            IconButton(onClick = onClose) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = stringResource(R.string.ui_back), tint = Palette.Text)
            }
            // The same band arrows and nudge keys as below the faders in portrait, so this view has a
            // screen reader alternative and fine steps too.
            IconButton(enabled = band > 0, onClick = { selected = band - 1 }) {
                Icon(Icons.AutoMirrored.Filled.KeyboardArrowLeft, contentDescription = stringResource(R.string.ui_previous_band), tint = Palette.Text)
            }
            Text(GraphicEq.formatFrequency(freqs[band]), style = Readout, color = Palette.Text)
            IconButton(enabled = band < bands - 1, onClick = { selected = band + 1 }) {
                Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, contentDescription = stringResource(R.string.ui_next_band), tint = Palette.Text)
            }
            val actual = FaderLayer.actual(base, gains, band)
            Text(
                formatLevel(actual),
                style = Readout,
                color = if (formatLevel(actual) != formatLevel(0.0)) Palette.Lit else Palette.TextDim,
                modifier = Modifier.weight(1f),
            )
            fun nudge(by: Double) {
                editFaders(faders, faders.toMutableList().also { it[band] = GraphicEq.snap(it[band] + by) })
                Repo.commit()
            }
            FilledTonalIconButton(enabled = faders[band] > -GraphicEq.MAX_DB, onClick = { nudge(-STEP_DB) }) {
                Icon(painterResource(R.drawable.ic_minus), contentDescription = stringResource(R.string.ui_lower, GraphicEq.formatFrequency(freqs[band])))
            }
            Spacer(Modifier.width(8.dp))
            FilledTonalIconButton(enabled = faders[band] < GraphicEq.MAX_DB, onClick = { nudge(STEP_DB) }) {
                Icon(Icons.Filled.Add, contentDescription = stringResource(R.string.ui_raise, GraphicEq.formatFrequency(freqs[band])))
            }
            Spacer(Modifier.width(8.dp))
            UndoRedoButtons()
            Spacer(Modifier.width(8.dp))
            SlidingToggle(
                options = listOf(
                    R.drawable.ic_touch_precise to stringResource(R.string.ui_fader_precision),
                    R.drawable.ic_touch_smooth to stringResource(R.string.ui_fader_smooth),
                ),
                selected = FaderMode.entries.indexOf(state.faderMode),
                onSelect = { i -> Repo.update { it.copy(faderMode = FaderMode.entries[i]) } },
                modifier = Modifier.width(260.dp),
            )
        }
        Faders(
            gains = faders,
            saved = FaderLayer.shown(base, savedFaders(state, state.current.id, gains.size)),
            smooth = state.faderMode == FaderMode.SMOOTH,
            scrollable = false,
            area = Modifier
                .weight(1f)
                .padding(top = 12.dp, bottom = 8.dp),
            fill = true,
            shown = rememberMorphed(faders.toDoubleArray(), state.current.id to tone.shape?.name),
            selected = band,
            onSelect = { selected = it },
            onChange = { before, moved -> editFaders(before, moved) },
            onFinished = Repo::commit,
        )
    }
}

/** The preset's own curve under the faders, worked out again only when the preset's sound changes. */
@Composable
private fun rememberFaderBase(preset: Preset, bands: Int): List<Double> {
    val tone = preset.tone
    return remember(preset.filters, tone.strength, tone.correction, tone.importedCurve, tone.targetId, tone.correctionInfo, bands) {
        FaderLayer.base(preset, bands)
    }
}

/**
 * Stores a move of the faders, given in what they show, as the change on top of the preset. Reads the
 * preset fresh, so a gesture that outlives a recomposition still works from the current sound.
 */
private fun editFaders(before: List<Double>, moved: List<Double>) {
    val bands = moved.size
    val base = FaderLayer.base(Repo.state.value.current, bands)
    Repo.editTone {
        val current = GraphicEq.normalized(it.graphicDb, it.graphicBands).let { g ->
            if (g.size == bands) g else GraphicEq.resample(g, g.size, bands)
        }
        val new = FaderLayer.change(base, before, current, moved)
        it.copy(graphicDb = new, graphicBands = bands, shape = it.shape.takeUnless { GraphicEq.isFlat(new) })
    }
}

/** A fader's level in the fewest characters: "+3.5", "-2", "0", or whole dB when [decimals] is off. */
internal fun faderValue(db: Double, decimals: Boolean): String {
    val r = if (decimals) kotlin.math.round(db * 2) / 2 else kotlin.math.round(db)
    return when {
        r == 0.0 -> "0"
        r == kotlin.math.floor(r) -> String.format(Locale.US, "%+d", r.toInt())
        else -> String.format(Locale.US, "%+.1f", r)
    }
}

private const val VALUE_ROW_DP = 13

private fun formatLevel(db: Double) = String.format(Locale.US, "%+.1f dB", db).replace("-0.0", "+0.0")
private const val DB_GUTTER_DP = 24
private val LABEL_FREQS = setOf(62.5, 63.0, 250.0, 1000.0, 4000.0, 16000.0)
