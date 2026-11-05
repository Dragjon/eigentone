package io.github.dragjon.eigentone.ui

import androidx.activity.compose.BackHandler
import androidx.annotation.StringRes
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.RoundRect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathFillType
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.boundsInRoot
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import io.github.dragjon.eigentone.R
import io.github.dragjon.eigentone.data.Repo
import kotlin.math.roundToInt

/** The stops of the tour, in order. One has nothing to point at, because the save bar only shows after a change. */
enum class TourStop(@StringRes val title: Int, @StringRes val body: Int, val anchored: Boolean = true) {
    POWER(R.string.tour_power_title, R.string.tour_power_body),
    PRESETS(R.string.tour_presets_title, R.string.tour_presets_body),
    STRIP(R.string.tour_strip_title, R.string.tour_strip_body),
    FADERS(R.string.tour_faders_title, R.string.tour_faders_body),
    SAVE(R.string.tour_save_title, R.string.tour_save_body, anchored = false),
    TABS(R.string.tour_tabs_title, R.string.tour_tabs_body),
    SETTINGS(R.string.tour_settings_title, R.string.tour_settings_body),
}

/** Where each stop's control is on screen, and which stop the tour is at, if it's running. */
object Tour {
    val anchors = mutableStateMapOf<TourStop, Rect>()
    var stop by mutableStateOf<TourStop?>(null)
        private set

    fun start() {
        stop = TourStop.entries.first()
    }

    fun next() {
        val now = stop ?: return
        if (now.ordinal == TourStop.entries.lastIndex) end() else stop = TourStop.entries[now.ordinal + 1]
    }

    fun back() {
        val now = stop ?: return
        if (now.ordinal > 0) stop = TourStop.entries[now.ordinal - 1]
    }

    fun end() {
        stop = null
        Repo.update { it.copy(tourSeen = true) }
    }
}

/** Marks a control the tour points at, by recording where it is. */
fun Modifier.tourTarget(stop: TourStop): Modifier = onGloballyPositioned { Tour.anchors[stop] = it.boundsInRoot() }

/**
 * Dims the whole app except the control being explained, which gets a gently pulsing lit outline, with a card
 * beside it saying what it does. The spotlight glides from one stop to the next. Touches stop here, so nothing
 * underneath changes by accident.
 */
@Composable
fun TourOverlay() {
    val stop = Tour.stop ?: return
    BackHandler { if (stop.ordinal > 0) Tour.back() else Tour.end() }
    val density = LocalDensity.current
    val pad = with(density) { 6.dp.toPx() }
    val target = Tour.anchors[stop]?.takeIf { stop.anchored }?.let { Rect(it.left - pad, it.top - pad, it.right + pad, it.bottom + pad) }
    val spring = tween<Float>(TOUR_MOVE_MS)
    // With nothing to point at, the spotlight shrinks to the middle and the card sits there.
    BoxWithConstraints(
        Modifier
            .fillMaxSize()
            // Being hit is enough to keep touches from the app underneath. Consuming them as well would cancel a
            // tap on the card's buttons whenever the finger drifts slightly between down and up.
            .pointerInput(Unit) {
                awaitPointerEventScope {
                    while (true) awaitPointerEvent()
                }
            },
    ) {
        val width = constraints.maxWidth.toFloat()
        val height = constraints.maxHeight.toFloat()
        val left by animateFloatAsState(target?.left ?: width / 2, spring, label = "left")
        val top by animateFloatAsState(target?.top ?: height / 2, spring, label = "top")
        val right by animateFloatAsState(target?.right ?: width / 2, spring, label = "right")
        val bottom by animateFloatAsState(target?.bottom ?: height / 2, spring, label = "bottom")
        val pulse by rememberInfiniteTransition(label = "pulse").animateFloat(
            initialValue = 0.45f,
            targetValue = 1f,
            animationSpec = infiniteRepeatable(tween(900), RepeatMode.Reverse),
            label = "pulse",
        )
        Canvas(Modifier.fillMaxSize()) {
            val corner = CornerRadius(12.dp.toPx())
            val hole = RoundRect(left, top, right, bottom, corner)
            val scrim = Path().apply {
                fillType = PathFillType.EvenOdd
                addRect(Rect(Offset.Zero, size))
                if (right - left > 1f) addRoundRect(hole)
            }
            drawPath(scrim, Color.Black.copy(alpha = 0.74f))
            if (right - left > 1f) {
                drawRoundRect(
                    Palette.Lit.copy(alpha = pulse),
                    topLeft = Offset(left, top),
                    size = Size(right - left, bottom - top),
                    cornerRadius = corner,
                    style = Stroke(width = 2.dp.toPx()),
                )
            }
        }

        var cardHeight by remember { mutableIntStateOf(0) }
        val gap = with(density) { 16.dp.toPx() }
        val cardY = when {
            target == null -> (height - cardHeight) / 2
            // Below the control when it's in the top half, otherwise above it.
            target.center.y < height / 2 -> target.bottom + gap
            else -> target.top - gap - cardHeight
        }.coerceIn(gap, (height - cardHeight - gap).coerceAtLeast(gap))
        Column(
            Modifier
                .align(Alignment.TopCenter)
                .offset { IntOffset(0, cardY.roundToInt()) }
                .padding(horizontal = 16.dp)
                .widthIn(max = 420.dp)
                .fillMaxWidth()
                .onSizeChanged { cardHeight = it.height }
                .clip(RoundedCornerShape(12.dp))
                .background(Palette.Tooltip)
                .padding(16.dp)
                .semantics { liveRegion = LiveRegionMode.Polite },
            verticalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            Text(
                stringResource(R.string.tour_step_of, stop.ordinal + 1, TourStop.entries.size),
                style = MaterialTheme.typography.labelMedium,
                color = Palette.Lit,
            )
            Text(stringResource(stop.title), style = MaterialTheme.typography.titleMedium, color = Palette.Text)
            Text(stringResource(stop.body), style = MaterialTheme.typography.bodyMedium, color = Palette.TextDim)
            Row(Modifier.fillMaxWidth().padding(top = 6.dp), verticalAlignment = Alignment.CenterVertically) {
                if (stop.ordinal < TourStop.entries.lastIndex) {
                    TextButton(onClick = Tour::end) { Text(stringResource(R.string.welcome_skip)) }
                }
                Spacer(Modifier.weight(1f))
                if (stop.ordinal > 0) TextButton(onClick = Tour::back) { Text(stringResource(R.string.ui_back)) }
                Button(onClick = Tour::next, modifier = Modifier.heightIn(min = 44.dp)) {
                    Text(stringResource(if (stop.ordinal == TourStop.entries.lastIndex) R.string.tour_done else R.string.tour_next))
                }
            }
        }
    }
}

/** Offered once to anyone who hasn't taken the tour, such as after updating to the reorganised app. */
@Composable
fun TourOffer() {
    Column(
        Modifier
            .fillMaxWidth()
            .clip(PanelShape)
            .background(Palette.SurfaceHigh)
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Text(stringResource(R.string.tour_offer_title), style = MaterialTheme.typography.titleMedium, color = Palette.Text)
        Text(stringResource(R.string.tour_offer_body), style = MaterialTheme.typography.bodyMedium, color = Palette.TextDim)
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Button(onClick = Tour::start) { Text(stringResource(R.string.tour_offer_start)) }
            TextButton(onClick = { Repo.update { it.copy(tourSeen = true) } }) { Text(stringResource(R.string.ui_not_now)) }
        }
    }
}

private const val TOUR_MOVE_MS = 380
