package com.phonons.app.ui

import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.animation.animateContentSize
import com.phonons.app.R
import androidx.compose.ui.res.stringResource
import android.media.AudioDeviceCallback
import android.media.AudioDeviceInfo
import android.media.AudioManager
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
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.input.pointer.positionChange
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.ProgressBarRangeInfo
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.progressBarRangeInfo
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.setProgress
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.math.abs

private const val DOUBLE_TAP_MS = 300L
private const val MUSIC_CHECK_MS = 1000L
private const val TRACK_TICKS = 20

/**
 * A heading above a group of settings on one surface. With a [summary], the group folds away under its
 * heading, and the summary says what's on while it's closed, so nothing changed is ever hidden from view.
 */
@Composable
fun SettingsSection(
    title: String?,
    trailing: @Composable (() -> Unit)? = null,
    summary: String? = null,
    content: @Composable ColumnScope.() -> Unit,
) {
    val folds = summary != null && title != null
    var open by rememberSaveable(title) { mutableStateOf(false) }
    val shown = !folds || open
    Column(Modifier.fillMaxWidth().animateContentSize()) {
        if (title != null) {
            Row(
                Modifier
                    .fillMaxWidth()
                    .heightIn(min = if (folds) 48.dp else 40.dp)
                    .then(
                        if (folds) {
                            Modifier.clickable(
                                role = Role.Button,
                                onClickLabel = stringResource(if (open) R.string.ui_fold_close else R.string.ui_fold_open),
                            ) { open = !open }
                        } else {
                            Modifier
                        },
                    )
                    .padding(start = 4.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Column(Modifier.weight(1f)) {
                    Text(title.uppercase(), style = PanelLegend, color = Palette.TextDim)
                    if (folds && !open) {
                        Text(
                            summary.orEmpty(),
                            style = MaterialTheme.typography.bodySmall,
                            color = if (summary == stringResource(R.string.ui_all_off)) Palette.TextDim else Palette.Lit,
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis,
                        )
                    }
                }
                trailing?.invoke()
                if (folds) {
                    Icon(
                        if (open) Icons.Filled.KeyboardArrowUp else Icons.Filled.KeyboardArrowDown,
                        contentDescription = null,
                        tint = Palette.TextDim,
                        modifier = Modifier.padding(horizontal = 8.dp),
                    )
                }
            }
        }
        if (shown) {
            Column(
                Modifier
                    .fillMaxWidth()
                    .clip(PanelShape)
                    .background(Palette.Surface),
                content = content,
            )
        }
    }
}

/** Joins what's on into one short line for a folded section, or says that nothing is. */
@Composable
fun foldSummary(parts: List<String>): String = parts.joinToString(" · ").ifEmpty { stringResource(R.string.ui_all_off) }

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
                title = { Text(stringResource(tip.title), style = MaterialTheme.typography.titleSmall) },
                colors = TooltipDefaults.richTooltipColors(
                    containerColor = Palette.Tooltip,
                    contentColor = Palette.Text,
                    titleContentColor = Palette.Text,
                ),
            ) { Text(stringResource(tip.body), style = MaterialTheme.typography.bodyMedium) }
        },
        state = state,
        enableUserInput = false,
    ) {
        IconButton(onClick = { scope.launch { state.show() } }) {
            Icon(
                Icons.Outlined.Info,
                contentDescription = stringResource(R.string.ui_about_setting, stringResource(tip.title)),
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

/** Opens more settings inside a section. Looks and works like a folding section's header, at row size. */
@Composable
fun DisclosureRow(label: String, open: Boolean, onToggle: () -> Unit) {
    Row(
        Modifier
            .fillMaxWidth()
            .heightIn(min = 52.dp)
            .clickable(
                role = Role.Button,
                onClickLabel = stringResource(if (open) R.string.ui_fold_close else R.string.ui_fold_open),
            ) { onToggle() }
            .padding(start = 16.dp, end = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(label, style = MaterialTheme.typography.bodyLarge, color = Palette.Lit, modifier = Modifier.weight(1f))
        Icon(
            if (open) Icons.Filled.KeyboardArrowUp else Icons.Filled.KeyboardArrowDown,
            contentDescription = null,
            tint = Palette.Lit,
            modifier = Modifier.padding(horizontal = 8.dp),
        )
    }
}

/** Reads a value out. Lit means it differs from the default, so changes are easy to spot. */
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
    /** Where a double tap returns it, such as the value saved in the preset. */
    resetTo: Float = default,
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
                color = if (changed) Palette.Lit else Palette.TextDim,
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
            resetTo = resetTo,
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
 * - A double tap returns to [resetTo], which is [default] unless given.
 *
 * It stays a real slider for screen readers through its progress semantics.
 */
@Composable
fun ValueTrack(
    label: String,
    readout: String,
    value: Float,
    range: ClosedFloatingPointRange<Float>,
    origin: Float,
    default: Float,
    onChange: (Float) -> Unit,
    onFinished: () -> Unit,
    resetTo: Float = default,
) {
    val current by rememberUpdatedState(value)
    val change by rememberUpdatedState(onChange)
    val finished by rememberUpdatedState(onFinished)
    val resetValue by rememberUpdatedState(resetTo)
    val span = range.endInclusive - range.start
    val clamped = value.coerceIn(range)

    Canvas(
        Modifier
            .fillMaxWidth()
            .height(48.dp)
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
                                change(resetValue)
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
        // A thin rail with an engraved scale and a rectangular cap, like a fader on a piece of equipment.
        val capWidth = 12.dp.toPx()
        val capHeight = 24.dp.toPx()
        val usable = size.width - capWidth
        val xOf = { v: Float -> capWidth / 2 + (v - range.start) / span * usable }
        val centre = size.height / 2
        val railHeight = 2.dp.toPx()
        val tickHeight = 3.dp.toPx()
        for (tick in 0..TRACK_TICKS) {
            val x = capWidth / 2 + usable * tick / TRACK_TICKS
            val major = tick == 0 || tick == TRACK_TICKS
            val length = if (major) tickHeight * 2 else tickHeight
            drawRect(
                Palette.Track,
                topLeft = Offset(x - 0.5.dp.toPx(), centre + railHeight + 3.dp.toPx()),
                size = Size(1.dp.toPx(), length),
            )
        }
        // The default gets its own long tick, wherever it falls on the scale.
        drawRect(
            Palette.TextDim,
            topLeft = Offset(xOf(default.coerceIn(range)) - 0.5.dp.toPx(), centre + railHeight + 3.dp.toPx()),
            size = Size(1.dp.toPx(), tickHeight * 2),
        )
        drawRect(
            Palette.Track,
            topLeft = Offset(capWidth / 2, centre - railHeight / 2),
            size = Size(usable, railHeight),
        )
        val from = xOf(origin.coerceIn(range))
        val to = xOf(clamped)
        val left = minOf(from, to)
        drawRect(
            Palette.Lit,
            topLeft = Offset(left, centre - railHeight / 2),
            size = Size(maxOf(from, to) - left, railHeight),
        )
        if (origin > range.start) {
            // A notch marks the zero point on centred controls.
            drawRect(
                Palette.TextDim,
                topLeft = Offset(from - 1.dp.toPx() / 2, centre - 7.dp.toPx()),
                size = Size(1.dp.toPx(), 7.dp.toPx()),
            )
        }
        val corner = CornerRadius(2.dp.toPx())
        drawRoundRect(
            Palette.Lit,
            topLeft = Offset(to - capWidth / 2, centre - capHeight / 2),
            size = Size(capWidth, capHeight),
            cornerRadius = corner,
        )
        drawRect(
            Palette.OnLit,
            topLeft = Offset(to - 1.dp.toPx(), centre - capHeight / 2 + 4.dp.toPx()),
            size = Size(2.dp.toPx(), capHeight - 8.dp.toPx()),
        )
    }
}

/** Names of the connected audio outputs, kept current as devices come and go, even while Eigentone is off. */
/** Whether Android's own spatial audio is on. It can only be read, because only system apps can change it. */
@Composable
fun rememberSystemSpatialAudio(): Boolean {
    val context = LocalContext.current
    val spatializer = remember { context.getSystemService(AudioManager::class.java).spatializer }
    return remember { spatializer.isAvailable && spatializer.isEnabled }
}

@Composable
fun rememberConnectedOutputNames(): List<String> {
    val context = LocalContext.current
    val audio = remember { context.getSystemService(AudioManager::class.java) }
    fun read() = audio.getDevices(AudioManager.GET_DEVICES_OUTPUTS).map { it.productName?.toString().orEmpty() }
    var names by remember { mutableStateOf(read()) }
    DisposableEffect(audio) {
        val callback = object : AudioDeviceCallback() {
            override fun onAudioDevicesAdded(added: Array<out AudioDeviceInfo>?) {
                names = read()
            }

            override fun onAudioDevicesRemoved(removed: Array<out AudioDeviceInfo>?) {
                names = read()
            }
        }
        audio.registerAudioDeviceCallback(callback, null)
        onDispose { audio.unregisterAudioDeviceCallback(callback) }
    }
    return names
}

/** Whether any app is playing music right now, checked once a second. */
@Composable
fun rememberMusicActive(): Boolean {
    val context = LocalContext.current
    var active by remember { mutableStateOf(true) }
    LaunchedEffect(Unit) {
        val audio = context.getSystemService(AudioManager::class.java)
        while (true) {
            active = audio.isMusicActive
            delay(MUSIC_CHECK_MS)
        }
    }
    return active
}

/** Eigentone changes another app's sound and plays nothing itself, which isn't obvious in a listening test. */
@Composable
fun NothingPlayingHint(modifier: Modifier = Modifier) {
    if (rememberMusicActive()) return
    Text(
        stringResource(R.string.ui_nothing_is_playing_start_your_music_app),
        style = MaterialTheme.typography.bodySmall,
        color = Palette.Text,
        modifier = modifier,
    )
}

/** The gaps between sections, kept in one place. */
val SectionSpacing = Arrangement.spacedBy(12.dp)
