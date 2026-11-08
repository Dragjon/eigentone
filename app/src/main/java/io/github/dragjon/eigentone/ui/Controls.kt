package io.github.dragjon.eigentone.ui

import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.shrinkVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.foundation.relocation.bringIntoViewRequester
import io.github.dragjon.eigentone.R
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
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.graphics.Color
import androidx.lifecycle.repeatOnLifecycle
import androidx.compose.material3.AlertDialog
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
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
@OptIn(androidx.compose.foundation.ExperimentalFoundationApi::class)
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
    // The chevron turns rather than swapping, so it's plain which way the section is going.
    val turn by animateFloatAsState(if (open) 180f else 0f, tween(FOLD_MS, easing = FastOutSlowInEasing), label = "chevron")
    // Asked for from elsewhere, such as a row in the strip's drawer: it opens and scrolls into view.
    val requester = remember { androidx.compose.foundation.relocation.BringIntoViewRequester() }
    // Every tab stays built, so only a section on the page in view answers, not one of the same name elsewhere.
    val pageShown = LocalPageShown.current
    LaunchedEffect(title) {
        if (title == null) return@LaunchedEffect
        androidx.compose.runtime.snapshotFlow { pageShown() && SectionFocus.request == title }.collect { wanted ->
            if (!wanted) return@collect
            open = true
            // Waits for the tab to slide in and the section to unfold, then brings all of it into view.
            delay(FOLD_MS + 120L)
            requester.bringIntoView()
            SectionFocus.request = null
        }
    }
    Column(Modifier.fillMaxWidth().bringIntoViewRequester(requester)) {
        if (title != null) {
            Row(
                Modifier
                    .fillMaxWidth()
                    .heightIn(min = if (folds) 48.dp else 40.dp)
                    // The press highlight has the same rounded corners as the panels.
                    .clip(KeyShape)
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
                    // Folds away under the heading as the section opens, and back out as it closes, so the
                    // heading glides into place rather than jumping.
                    AnimatedVisibility(
                        visible = folds && !open,
                        enter = expandVertically(tween(FOLD_MS, easing = FastOutSlowInEasing), expandFrom = Alignment.Top) +
                            fadeIn(tween(FOLD_MS)),
                        exit = shrinkVertically(tween(FOLD_MS, easing = FastOutSlowInEasing), shrinkTowards = Alignment.Top) +
                            fadeOut(tween(FOLD_MS / 2)),
                    ) {
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
                        Icons.Filled.KeyboardArrowDown,
                        contentDescription = null,
                        tint = Palette.TextDim,
                        modifier = Modifier
                            .padding(horizontal = 8.dp)
                            .graphicsLayer { rotationZ = turn },
                    )
                }
            }
        }
        // Opens downwards and closes back up the same way.
        AnimatedVisibility(
            visible = shown,
            enter = expandVertically(tween(FOLD_MS, easing = FastOutSlowInEasing), expandFrom = Alignment.Top) + fadeIn(tween(FOLD_MS)),
            exit = shrinkVertically(tween(FOLD_MS, easing = FastOutSlowInEasing), shrinkTowards = Alignment.Top) + fadeOut(tween(FOLD_MS / 2)),
        ) {
            Column(
                Modifier
                    .fillMaxWidth()
                    .clip(PanelShape)
                    .background(Palette.Surface)
                    // Rows appearing or going inside a panel, such as a setting that only shows when it applies,
                    // grow and shrink it smoothly.
                    .animateContentSize(tween(FOLD_MS, easing = FastOutSlowInEasing)),
                content = content,
            )
        }
    }
}

/** How long a section takes to fold open or closed. */
internal const val FOLD_MS = 260

/** A section another part of the app wants shown, by its title. The section clears it once it has opened. */
object SectionFocus {
    var request by mutableStateOf<String?>(null)
}

/**
 * Whether the page this is on is the tab in view. Pages out of view stay built but shouldn't act. It's asked
 * rather than handed over as a value, so a tab switch only reaches the few places that ask, at the moment they
 * ask, instead of rebuilding the page.
 */
val LocalPageShown = androidx.compose.runtime.staticCompositionLocalOf<() -> Boolean> { { true } }

/** Joins what's on into one short line for a folded section, or says that nothing is. */
@Composable
fun foldSummary(parts: List<String>): String = parts.joinToString(" · ").ifEmpty { stringResource(R.string.ui_all_off) }

@Composable
fun RowDivider() {
    HorizontalDivider(Modifier.padding(horizontal = 16.dp), color = Palette.Line)
}

/** Past this many characters a tip opens as a dialog that scrolls, since a tooltip would run off the screen. */
private const val LONG_TIP_CHARS = 300

/**
 * Shows [tip] beside what [anchor] draws: a tooltip for a short explanation, a dialog for a long one.
 * [anchor] gets the function that opens it.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun TipHost(tip: Tip, anchor: @Composable (open: () -> Unit) -> Unit) {
    val body = stringResource(tip.body)
    if (body.length > LONG_TIP_CHARS) {
        var open by remember { mutableStateOf(false) }
        anchor { open = true }
        if (open) {
            MotionDialog(
                onDismissRequest = { open = false },
                containerColor = Palette.Tooltip,
                titleContentColor = Palette.Text,
                textContentColor = Palette.Text,
                title = { Text(stringResource(tip.title), style = MaterialTheme.typography.titleMedium) },
                text = {
                    Text(body, style = MaterialTheme.typography.bodyMedium, modifier = Modifier.verticalScroll(rememberScrollState()))
                },
                confirmButton = { TextButton(onClick = { open = false }) { Text(stringResource(R.string.ui_done)) } },
            )
        }
        return
    }
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
            ) { Text(body, style = MaterialTheme.typography.bodyMedium) }
        },
        state = state,
        enableUserInput = false,
    ) { anchor { scope.launch { state.show() } } }
}

/** Opens an explanation next to the setting. Tapping anywhere else closes it. */
@Composable
fun InfoButton(tip: Tip) {
    TipHost(tip) { open ->
        IconButton(onClick = open) {
            Icon(
                Icons.Outlined.Info,
                contentDescription = stringResource(R.string.ui_about_setting, stringResource(tip.title)),
                tint = Palette.TextDim,
                modifier = Modifier.size(18.dp),
            )
        }
    }
}

/**
 * Something too small or too plain for an info button beside it, such as a meter, which explains itself
 * when tapped instead. The touch area is [modifier]'s, so it can be wider than what it draws.
 */
@Composable
fun TipOnTap(tip: Tip, modifier: Modifier = Modifier, content: @Composable () -> Unit) {
    val about = stringResource(R.string.ui_about_setting, stringResource(tip.title))
    TipHost(tip) { open ->
        androidx.compose.foundation.layout.Box(
            modifier.clickable(onClickLabel = about, role = Role.Button, onClick = open),
            contentAlignment = Alignment.Center,
        ) { content() }
    }
}

/** Label, info button and an optional current value. Shared by every setting. */
@Composable
private fun SettingLabel(title: String, tip: Tip?, modifier: Modifier = Modifier, color: Color = Palette.Text) {
    Row(modifier, verticalAlignment = Alignment.CenterVertically) {
        Text(
            title,
            style = MaterialTheme.typography.bodyLarge,
            color = color,
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

/**
 * A row that opens something or runs an action. [opens] draws the chevron, so a row that acts at once,
 * such as copying or deleting, doesn't look like it leads somewhere. [danger] is for removing things.
 */
@Composable
fun ActionRow(
    title: String,
    tip: Tip,
    detail: String? = null,
    enabled: Boolean = true,
    opens: Boolean = true,
    danger: Boolean = false,
    onClick: () -> Unit,
) {
    Row(
        Modifier
            .fillMaxWidth()
            .heightIn(min = 56.dp)
            .clickable(enabled = enabled, role = Role.Button, onClick = onClick)
            .padding(start = 16.dp, end = 8.dp, top = 4.dp, bottom = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(Modifier.weight(1f)) {
            // A row that can't be used now looks it, rather than only ignoring taps.
            val color = when {
                !enabled -> Palette.TextDim.copy(alpha = 0.6f)
                danger -> Palette.Danger
                else -> Palette.Text
            }
            SettingLabel(title, tip, color = color)
            if (detail != null) {
                Text(
                    detail,
                    style = MaterialTheme.typography.bodySmall,
                    color = Palette.TextDim,
                    modifier = Modifier.padding(bottom = 8.dp),
                )
            }
        }
        if (opens) {
            Icon(
                Icons.AutoMirrored.Filled.KeyboardArrowRight,
                contentDescription = null,
                tint = if (enabled) Palette.TextDim else Palette.TextDim.copy(alpha = 0.4f),
            )
        }
    }
}

/** Opens more settings inside a section. Looks and works like a folding section's header, at row size. */
@Composable
fun DisclosureRow(label: String, open: Boolean, onToggle: () -> Unit) {
    val turn by animateFloatAsState(if (open) 180f else 0f, tween(FOLD_MS, easing = FastOutSlowInEasing), label = "disclosure")
    Row(
        Modifier
            .fillMaxWidth()
            .heightIn(min = 56.dp)
            .clip(KeyShape)
            .clickable(
                role = Role.Button,
                onClickLabel = stringResource(if (open) R.string.ui_fold_close else R.string.ui_fold_open),
            ) { onToggle() }
            .padding(start = 16.dp, end = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        // Plain text with a chevron, like every other row; amber stays for what's lit or active.
        Text(label, style = MaterialTheme.typography.bodyLarge, color = Palette.Text, modifier = Modifier.weight(1f))
        Icon(
            Icons.Filled.KeyboardArrowDown,
            contentDescription = null,
            tint = Palette.TextDim,
            modifier = Modifier
                .padding(horizontal = 8.dp)
                .graphicsLayer { rotationZ = turn },
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
    val view = LocalView.current
    val defaultNow by rememberUpdatedState(default)

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
                                Haptics.tick(view)
                                change(resetValue)
                                finished()
                                lastTapAt = 0L
                            } else {
                                lastTapAt = now
                            }
                        }
                        return@awaitEachGesture
                    }
                    // The cap travels the width less its own, so the value keeps pace with the finger.
                    val width = (size.width - 12.dp.toPx()).coerceAtLeast(1f)
                    var running = current
                    horizontalDrag(drag.id) { event ->
                        val moved = event.positionChange().x
                        val before = running
                        running = (running + moved / width * span).coerceIn(range)
                        // A tick passing the default, and a firmer one reaching either end, felt without looking.
                        Haptics.crossing(view, before, running, defaultNow, range)
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

/**
 * Whether Android's own spatial audio is on. It can only be read, because only system apps can change it,
 * and it follows changes made in Quick Settings while the screen is open.
 */
@Composable
fun rememberSystemSpatialAudio(): Boolean {
    // Android's spatial audio arrived in Android 12L.
    if (android.os.Build.VERSION.SDK_INT < android.os.Build.VERSION_CODES.S_V2) return false
    val context = LocalContext.current
    val spatializer = remember { context.getSystemService(AudioManager::class.java).spatializer }
    fun read() = spatializer.isAvailable && spatializer.isEnabled
    var on by remember { mutableStateOf(read()) }
    DisposableEffect(spatializer) {
        val listener = object : android.media.Spatializer.OnSpatializerStateChangedListener {
            override fun onSpatializerEnabledChanged(s: android.media.Spatializer, enabled: Boolean) {
                on = read()
            }

            override fun onSpatializerAvailableChanged(s: android.media.Spatializer, available: Boolean) {
                on = read()
            }
        }
        spatializer.addOnSpatializerStateChangedListener(context.mainExecutor, listener)
        onDispose { spatializer.removeOnSpatializerStateChangedListener(listener) }
    }
    return on
}

/** Names of the connected audio outputs, kept current as devices come and go, even while Eigentone is off. */
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

/** Whether any app is playing music right now, checked once a second while Eigentone is on screen. */
@Composable
fun rememberMusicActive(): Boolean {
    val context = LocalContext.current
    val lifecycle = androidx.lifecycle.compose.LocalLifecycleOwner.current.lifecycle
    var active by remember { mutableStateOf(true) }
    LaunchedEffect(lifecycle) {
        val audio = context.getSystemService(AudioManager::class.java)
        lifecycle.repeatOnLifecycle(androidx.lifecycle.Lifecycle.State.STARTED) {
            while (true) {
                active = audio.isMusicActive
                delay(MUSIC_CHECK_MS)
            }
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

/**
 * Touch feedback for the moments that matter: passing a default, reaching an end, switching something
 * on or off, and confirming. Everyday taps stay silent, so these mean something.
 */
object Haptics {
    fun tick(view: android.view.View) {
        view.performHapticFeedback(android.view.HapticFeedbackConstants.CLOCK_TICK)
    }

    fun limit(view: android.view.View) {
        view.performHapticFeedback(android.view.HapticFeedbackConstants.CONTEXT_CLICK)
    }

    fun toggle(view: android.view.View, on: Boolean) {
        view.performHapticFeedback(
            if (android.os.Build.VERSION.SDK_INT >= 34) {
                if (on) android.view.HapticFeedbackConstants.TOGGLE_ON else android.view.HapticFeedbackConstants.TOGGLE_OFF
            } else {
                android.view.HapticFeedbackConstants.CONTEXT_CLICK
            },
        )
    }

    fun confirm(view: android.view.View) {
        view.performHapticFeedback(android.view.HapticFeedbackConstants.CONFIRM)
    }

    /** Feedback for a value moving from [before] to [after]: past [default], or arriving at an end of [range]. */
    fun crossing(view: android.view.View, before: Float, after: Float, default: Float, range: ClosedFloatingPointRange<Float>) {
        if (before == after) return
        val reachedEnd = (after == range.start || after == range.endInclusive) && before != after
        val passedDefault = (before < default && after >= default) || (before > default && after <= default)
        when {
            reachedEnd -> limit(view)
            passedDefault -> tick(view)
        }
    }
}
