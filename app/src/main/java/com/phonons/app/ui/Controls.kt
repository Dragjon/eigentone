package com.phonons.app.ui

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.awaitHorizontalTouchSlopOrCancellation
import androidx.compose.foundation.gestures.horizontalDrag
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RichTooltip
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TooltipBox
import androidx.compose.material3.TooltipDefaults
import androidx.compose.material3.rememberTooltipState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.input.pointer.positionChange
import androidx.compose.ui.semantics.ProgressBarRangeInfo
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.progressBarRangeInfo
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.setProgress
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.launch
import kotlin.math.abs

private const val DOUBLE_TAP_MS = 300L

/** A heading above a group of settings on one surface. */
@Composable
fun SettingsSection(title: String, trailing: @Composable (() -> Unit)? = null, content: @Composable ColumnScope.() -> Unit) {
    Column(Modifier.fillMaxWidth()) {
        Row(
            Modifier
                .fillMaxWidth()
                .heightIn(min = 40.dp)
                .padding(start = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                title,
                style = MaterialTheme.typography.titleSmall,
                color = Palette.TextDim,
                modifier = Modifier.weight(1f),
            )
            trailing?.invoke()
        }
        Column(
            Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(16.dp))
                .background(Palette.Surface),
            content = content,
        )
    }
}

@Composable
fun RowDivider() {
    HorizontalDivider(Modifier.padding(horizontal = 16.dp), color = Palette.Line)
}

/** Opens an explanation next to the setting. Tapping anywhere else closes it. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun InfoButton(tip: Tip) {
    val state = rememberTooltipState(isPersistent = true)
    val scope = rememberCoroutineScope()
    TooltipBox(
        positionProvider = TooltipDefaults.rememberRichTooltipPositionProvider(),
        tooltip = {
            RichTooltip(
                title = { Text(tip.title, style = MaterialTheme.typography.titleSmall) },
                colors = TooltipDefaults.richTooltipColors(
                    containerColor = Palette.Tooltip,
                    contentColor = Palette.Text,
                    titleContentColor = Palette.Text,
                ),
            ) { Text(tip.body, style = MaterialTheme.typography.bodyMedium) }
        },
        state = state,
        enableUserInput = false,
    ) {
        IconButton(onClick = { scope.launch { state.show() } }) {
            Icon(
                Icons.Outlined.Info,
                contentDescription = "About ${tip.title}",
                tint = Palette.TextDim,
                modifier = Modifier.size(18.dp),
            )
        }
    }
}

/** Label, info button and an optional current value. Shared by every setting. */
@Composable
private fun SettingLabel(title: String, tip: Tip?, modifier: Modifier = Modifier) {
    Row(modifier, verticalAlignment = Alignment.CenterVertically) {
        Text(
            title,
            style = MaterialTheme.typography.bodyLarge,
            color = Palette.Text,
            modifier = Modifier.weight(1f, fill = false),
        )
        if (tip != null) InfoButton(tip)
    }
}

@Composable
fun SwitchSetting(title: String, tip: Tip, checked: Boolean, detail: String? = null, onChange: (Boolean) -> Unit) {
    Row(
        Modifier
            .fillMaxWidth()
            .heightIn(min = 56.dp)
            .toggleable(value = checked, role = Role.Switch, onValueChange = onChange)
            .padding(start = 16.dp, end = 16.dp, top = 4.dp, bottom = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(Modifier.weight(1f)) {
            SettingLabel(title, tip)
            if (detail != null) {
                Text(
                    detail,
                    style = MaterialTheme.typography.bodySmall,
                    color = Palette.TextDim,
                    modifier = Modifier.padding(bottom = 8.dp),
                )
            }
        }
        // The whole row toggles and announces itself, so the switch only draws the state.
        Switch(checked = checked, onCheckedChange = null, modifier = Modifier.padding(start = 12.dp))
    }
}

/** A row that opens something or runs an action. */
@Composable
fun ActionRow(title: String, tip: Tip, detail: String? = null, enabled: Boolean = true, onClick: () -> Unit) {
    Row(
        Modifier
            .fillMaxWidth()
            .heightIn(min = 56.dp)
            .clickable(enabled = enabled, role = Role.Button, onClick = onClick)
            .padding(start = 16.dp, end = 8.dp, top = 4.dp, bottom = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(Modifier.weight(1f)) {
            SettingLabel(title, tip)
            if (detail != null) {
                Text(
                    detail,
                    style = MaterialTheme.typography.bodySmall,
                    color = Palette.TextDim,
                    modifier = Modifier.padding(bottom = 8.dp),
                )
            }
        }
        Icon(
            Icons.AutoMirrored.Filled.KeyboardArrowRight,
            contentDescription = null,
            tint = Palette.TextDim,
        )
    }
}

/** Reads a value out. Amber means it differs from the default, so changes are easy to spot. */
@Composable
fun SliderSetting(
    title: String,
    tip: Tip,
    readout: String,
    value: Float,
    range: ClosedFloatingPointRange<Float>,
    default: Float,
    origin: Float = range.start,
    detail: String? = null,
    onChange: (Float) -> Unit,
    onFinished: () -> Unit = {},
) {
    val changed = abs(value - default) > (range.endInclusive - range.start) * 0.005f
    Column(
        Modifier
            .fillMaxWidth()
            .padding(start = 16.dp, end = 16.dp, top = 4.dp, bottom = 8.dp),
    ) {
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            SettingLabel(title, tip, Modifier.weight(1f))
            Text(
                readout,
                style = Readout,
                color = if (changed) Palette.Amber else Palette.TextDim,
                modifier = Modifier.padding(start = 8.dp),
            )
        }
        ValueTrack(
            label = title,
            readout = readout,
            value = value,
            range = range,
            origin = origin,
            default = default,
            onChange = onChange,
            onFinished = onFinished,
        )
        if (detail != null) {
            Text(detail, style = MaterialTheme.typography.bodySmall, color = Palette.TextDim)
        }
    }
}

/**
 * Replaces Material's Slider, which jumps to wherever it's touched and fills from the left only.
 *
 * - Only a sideways drag changes the value, so a vertical scroll that starts here can't alter a
 *   setting, and a stray tap does nothing.
 * - The value moves relative to the finger rather than jumping to it.
 * - It fills from [origin], so a centred control at 0 dB looks untouched.
 * - A double tap returns to [default].
 *
 * It stays a real slider for screen readers through its progress semantics.
 */
@Composable
private fun ValueTrack(
    label: String,
    readout: String,
    value: Float,
    range: ClosedFloatingPointRange<Float>,
    origin: Float,
    default: Float,
    onChange: (Float) -> Unit,
    onFinished: () -> Unit,
) {
    val current by rememberUpdatedState(value)
    val change by rememberUpdatedState(onChange)
    val finished by rememberUpdatedState(onFinished)
    val resetTo by rememberUpdatedState(default)
    val span = range.endInclusive - range.start
    val clamped = value.coerceIn(range)

    Canvas(
        Modifier
            .fillMaxWidth()
            .height(40.dp)
            .semantics {
                contentDescription = label
                stateDescription = readout
                progressBarRangeInfo = ProgressBarRangeInfo(clamped, range)
                setProgress { target ->
                    change(target.coerceIn(range))
                    finished()
                    true
                }
            }
            .pointerInput(span) {
                var lastTapAt = 0L
                awaitEachGesture {
                    val down = awaitFirstDown(requireUnconsumed = false)
                    val drag = awaitHorizontalTouchSlopOrCancellation(down.id) { event, _ -> event.consume() }
                    if (drag == null) {
                        // Released without dragging sideways, or a vertical scroll took over.
                        val up = currentEvent.changes.firstOrNull { it.id == down.id }
                        val isTap = up != null && !up.pressed && !up.isConsumed
                        if (isTap) {
                            val now = up!!.uptimeMillis
                            if (now - lastTapAt < DOUBLE_TAP_MS) {
                                change(resetTo)
                                finished()
                                lastTapAt = 0L
                            } else {
                                lastTapAt = now
                            }
                        }
                        return@awaitEachGesture
                    }
                    val width = size.width.toFloat().coerceAtLeast(1f)
                    var running = current
                    horizontalDrag(drag.id) { event ->
                        val moved = event.positionChange().x
                        running = (running + moved / width * span).coerceIn(range)
                        change(running)
                        event.consume()
                    }
                    finished()
                }
            },
    ) {
        val thumbRadius = 9.dp.toPx()
        val usable = size.width - thumbRadius * 2
        val xOf = { v: Float -> thumbRadius + (v - range.start) / span * usable }
        val centre = size.height / 2
        val trackHeight = 4.dp.toPx()
        val corner = CornerRadius(trackHeight / 2)
        drawRoundRect(
            Palette.Track,
            topLeft = Offset(thumbRadius, centre - trackHeight / 2),
            size = Size(usable, trackHeight),
            cornerRadius = corner,
        )
        val from = xOf(origin.coerceIn(range))
        val to = xOf(clamped)
        val left = minOf(from, to)
        drawRoundRect(
            Palette.Amber,
            topLeft = Offset(left, centre - trackHeight / 2),
            size = Size(maxOf(from, to) - left, trackHeight),
            cornerRadius = corner,
        )
        if (origin > range.start) {
            // A notch marks the zero point on centred controls.
            drawRect(
                Palette.TextDim,
                topLeft = Offset(from - 1.dp.toPx() / 2, centre - 6.dp.toPx()),
                size = Size(1.dp.toPx(), 12.dp.toPx()),
            )
        }
        drawCircle(Palette.Surface, thumbRadius, Offset(to, centre))
        drawCircle(Palette.Amber, thumbRadius - 1.dp.toPx(), Offset(to, centre))
        drawCircle(Palette.OnAmber, 2.5.dp.toPx(), Offset(to, centre))
    }
}

/** The gaps between sections, kept in one place. */
val SectionSpacing = Arrangement.spacedBy(12.dp)
