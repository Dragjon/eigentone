@file:OptIn(androidx.compose.animation.ExperimentalSharedTransitionApi::class)

package io.github.dragjon.eigentone.ui

import androidx.compose.ui.semantics.Role
import androidx.compose.runtime.setValue
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.clickable
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.SharedTransitionLayout
import androidx.compose.animation.SharedTransitionScope
import androidx.compose.animation.ExperimentalSharedTransitionApi
import androidx.compose.animation.expandVertically
import androidx.compose.animation.shrinkVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.LinearOutSlowInEasing
import androidx.compose.ui.layout.layout
import kotlin.math.roundToInt
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.foundation.layout.Box
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.layout.boundsInRoot
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import io.github.dragjon.eigentone.R
import io.github.dragjon.eigentone.data.AppState
import io.github.dragjon.eigentone.data.ClipRisk
import io.github.dragjon.eigentone.data.EffectState
import io.github.dragjon.eigentone.data.OutputInfo
import io.github.dragjon.eigentone.data.Processing
import io.github.dragjon.eigentone.data.Repo
import io.github.dragjon.eigentone.dsp.Eq

private const val MINI_POINTS = 48
private const val STRIP_HEIGHT_DP = 38
private const val MINI_BARS = 24
private const val MINI_RANGE_DB = 18.0

private fun outputIcon(output: OutputInfo): Int = when {
    output.key.startsWith("Bluetooth") -> R.drawable.ic_output_bluetooth
    output.key.startsWith("USB") || output.key.startsWith("Wired") -> R.drawable.ic_output_headphones
    else -> R.drawable.ic_output_phone
}

/**
 * What's happening right now, at a glance. Everything here is real: Android doesn't report how much a
 * compressor is compressing, for example, so it's shown as on rather than with an invented meter.
 */
@Composable
fun LiveStrip(state: AppState, showCurve: Boolean) {
    val output by Repo.output.collectAsStateWithLifecycle()
    val scheduledNight by Repo.scheduledNight.collectAsStateWithLifecycle()
    val effects = remember(state, output.key, scheduledNight) { Processing.effectStates(state, output.key, scheduledNight) }
    val working = state.enabled && !state.bypass
    val attenuationNow by Repo.attenuationDb.collectAsStateWithLifecycle()
    val clipping = remember(state, attenuationNow, output.key, scheduledNight) {
        state.enabled && Processing.peakEstimate(state, attenuationNow, output.key, scheduledNight).risk == ClipRisk.CLIPPING
    }

    val expanded = StripDrawer.open
    val openEffect = LocalOpenEffect.current
    // 0 closed, 1 open. The name grows into the room the drawer makes, while the small icons slide down
    // and out, since the drawer lists each of them in full.
    // Read only while laying out and drawing, never while composing, so the strip isn't rebuilt every frame
    // of the animation: the name and icon are scaled rather than given a new size each frame.
    val grow = animateFloatAsState(if (expanded) 1f else 0f, tween(STRIP_MS, easing = LinearOutSlowInEasing), label = "strip")
    val turn by animateFloatAsState(if (expanded) 180f else 0f, tween(STRIP_MS, easing = LinearOutSlowInEasing), label = "stripChevron")
    val nameScale = { STRIP_NAME_CLOSED + (1f - STRIP_NAME_CLOSED) * grow.value }
    // Each small effect icon in the bar travels down to its own row in the drawer as it opens, and back up
    // into the bar as it closes, so it's plain which row is which.
    // The open drawer lies over the page rather than pushing it down, so the page holds still and isn't
    // laid out and redrawn on every frame while the drawer moves. The strip only ever takes its closed height.
    val closedHeight = remember { intArrayOf(0) }
    SharedTransitionLayout(
        Modifier
            .fillMaxWidth()
            .layout { measurable, constraints ->
                val placeable = measurable.measure(constraints)
                val taken = if (closedHeight[0] > 0) closedHeight[0].coerceAtMost(placeable.height) else placeable.height
                layout(placeable.width, taken) { placeable.place(0, 0) }
            }
            // Lifts off the page as it opens, so it reads as lying over it.
            .graphicsLayer {
                shadowElevation = STRIP_DRAWER_SHADOW_DP.dp.toPx() * grow.value
                clip = false
            },
    ) {
    val shared = this
    Column(
        Modifier
            .fillMaxWidth()
            .background(Palette.Strip)
            // Where the strip and its drawer are, so a tap anywhere else can close the drawer.
            .onGloballyPositioned { StripDrawer.bounds = it.boundsInRoot() },
    ) {
    // Ripples and icons stay light on the green whichever theme the page uses.
    androidx.compose.runtime.CompositionLocalProvider(androidx.compose.material3.LocalContentColor provides Palette.StripText) {
    Row(
        Modifier
            .fillMaxWidth()
            .layout { measurable, constraints ->
                val least = (STRIP_HEIGHT_DP + (STRIP_OPEN_HEIGHT_DP - STRIP_HEIGHT_DP) * grow.value).dp.roundToPx()
                val placeable = measurable.measure(constraints.copy(minHeight = least.coerceIn(constraints.minHeight, constraints.maxHeight)))
                // The height the strip keeps on the page, taken while shut, so a larger font still fits.
                if (grow.value == 0f) closedHeight[0] = placeable.height
                layout(placeable.width, placeable.height) { placeable.place(0, 0) }
            }
            .clickable(
                role = Role.Button,
                onClickLabel = stringResource(if (expanded) R.string.ui_strip_hide else R.string.ui_strip_show),
            ) { StripDrawer.open = !expanded }
            .padding(start = 16.dp, end = 8.dp, top = 4.dp, bottom = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        Icon(
            painterResource(outputIcon(output)),
            contentDescription = null,
            tint = if (working) Palette.StripOn else Palette.StripDim,
            modifier = Modifier
                .grownBy(nameScale)
                .size(22.dp),
        )
        Text(
            output.label,
            style = MaterialTheme.typography.bodySmall.copy(fontSize = 16.sp, lineHeight = 22.sp),
            color = Palette.StripText,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier
                .weight(1f)
                .grownBy(nameScale),
        )
        AnimatedVisibility(
            visible = !expanded,
            enter = fadeIn(tween(STRIP_MS)),
            exit = fadeOut(tween(STRIP_MS / 2)),
        ) {
            val bar = this
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                if (clipping) EffectPill(stringResource(R.string.ui_clip_risk), Palette.StripDanger)
                effects.forEach { effect ->
                    EffectTag(
                        effect,
                        iconModifier = with(shared) {
                            Modifier.sharedElement(rememberSharedContentState(effectKey(effect.name)), animatedVisibilityScope = bar, boundsTransform = stripBounds)
                        },
                    )
                }
            }
        }
        PlayingDot()
        if (showCurve) MiniCurve(state, output.key, working)
        // Says the strip opens, and turns to say it closes again.
        Icon(
            Icons.Filled.KeyboardArrowDown,
            contentDescription = null,
            tint = Palette.StripDim,
            modifier = Modifier
                .size(18.dp)
                .graphicsLayer { rotationZ = turn },
        )
    }
    // A copy of the drawer is built and laid out once, out of sight, soon after launch, so the first real open
    // isn't also the first time its rows are ever made.
    var rehearse by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) {
        kotlinx.coroutines.delay(STRIP_REHEARSE_DELAY_MS)
        rehearse = true
        repeat(3) { androidx.compose.runtime.withFrameNanos { } }
        rehearse = false
    }
    if (rehearse && !expanded) {
        Box(
            Modifier
                .clearAndSetSemantics { }
                // Takes no room and sits far off the screen.
                .layout { measurable, constraints ->
                    val placeable = measurable.measure(constraints)
                    layout(0, 0) { placeable.place(-placeable.width * 3, 0) }
                },
        ) {
            SharedTransitionLayout {
                val copy = this
                androidx.compose.animation.AnimatedVisibility(visible = true) {
                    StripDetails(effects, output, clipping, state, scheduledNight, copy) { }
                }
            }
        }
    }
    AnimatedVisibility(
        visible = expanded,
        enter = expandVertically(tween(STRIP_MS, easing = LinearOutSlowInEasing), expandFrom = Alignment.Top) + fadeIn(tween(STRIP_MS)),
        exit = shrinkVertically(tween(STRIP_MS, easing = LinearOutSlowInEasing), shrinkTowards = Alignment.Top) + fadeOut(tween(STRIP_MS)),
    ) {
        StripDetails(effects, output, clipping, state, scheduledNight, shared) { name ->
            StripDrawer.open = false
            openEffect(name)
        }
    }
    }
    }
    }
}

private fun effectKey(name: String) = "strip-effect-$name"

/** The icons travel on the same curve and timing as the drawer that carries them. */
@OptIn(ExperimentalSharedTransitionApi::class)
private val stripBounds = androidx.compose.animation.BoundsTransform { _, _ -> tween(STRIP_MS, easing = LinearOutSlowInEasing) }

/** Whether the strip's drawer is open, and where the strip sits, shared so the rest of the app can close it. */
object StripDrawer {
    var open by mutableStateOf(false)
    var bounds by mutableStateOf(androidx.compose.ui.geometry.Rect.Zero)
}

/** Opens the setting behind an effect, by its name, from the strip's drawer. */
val LocalOpenEffect = androidx.compose.runtime.compositionLocalOf<(String) -> Unit> { {} }

private const val STRIP_MS = 280

/** After the app has opened and its tabs have been built. */
private const val STRIP_REHEARSE_DELAY_MS = 1800L
private const val STRIP_OPEN_HEIGHT_DP = 52
private const val STRIP_DRAWER_SHADOW_DP = 8

/** The name and its icon are drawn at their open size and shrunk by this while the drawer is shut (12 sp of 16). */
private const val STRIP_NAME_CLOSED = 0.75f

/**
 * Lays its content out at full size, then shrinks it by [scale] and takes only the room it then needs, so a
 * growing label costs a cheap re-layout each frame rather than a rebuild at a new font size. Text still
 * ellipsises at the width it ends up with.
 */
private fun Modifier.grownBy(scale: () -> Float): Modifier = layout { measurable, constraints ->
    val s = scale().coerceAtLeast(0.01f)
    val inner = constraints.copy(
        minWidth = 0,
        maxWidth = if (constraints.hasBoundedWidth) (constraints.maxWidth / s).toInt() else constraints.maxWidth,
        minHeight = 0,
        maxHeight = if (constraints.hasBoundedHeight) (constraints.maxHeight / s).toInt() else constraints.maxHeight,
    )
    val placeable = measurable.measure(inner)
    val width = (placeable.width * s).roundToInt().coerceIn(constraints.minWidth, constraints.maxWidth)
    val height = (placeable.height * s).roundToInt().coerceIn(constraints.minHeight, constraints.maxHeight)
    layout(width, height) {
        placeable.placeWithLayer(0, 0) {
            scaleX = s
            scaleY = s
            transformOrigin = androidx.compose.ui.graphics.TransformOrigin(0f, 0f)
        }
    }
}

/** Every effect by its full name, opened by tapping the strip. Each row leads to its setting. */
@Composable
private fun androidx.compose.animation.AnimatedVisibilityScope.StripDetails(
    effects: List<EffectState>,
    output: OutputInfo,
    clipping: Boolean,
    state: AppState,
    scheduledNight: Boolean,
    shared: SharedTransitionScope,
    onOpen: (String) -> Unit,
) {
    val drawer = this
    val status by Repo.status.collectAsStateWithLifecycle()
    val context = androidx.compose.ui.platform.LocalContext.current
    androidx.compose.runtime.CompositionLocalProvider(androidx.compose.material3.LocalContentColor provides Palette.StripText) {
    Column(
        Modifier
            .fillMaxWidth()
            // The strip's own colour, a shade lighter, so the drawer belongs to the strip yet reads apart from its bar (user decision).
            .background(Palette.StripPanel)
            .padding(start = 8.dp, end = 8.dp, top = 4.dp, bottom = 8.dp),
    ) {
        if (clipping) {
            Text(
                stringResource(R.string.ui_clip_risk),
                style = MaterialTheme.typography.bodySmall,
                color = Palette.StripDanger,
                modifier = Modifier.padding(horizontal = 8.dp, vertical = 6.dp),
            )
        }
        effects.forEachIndexed { index, effect ->
            val colour = if (effect.on) Palette.StripOn else Palette.StripDim
            // Each row drops in just after the one above, and lifts out the same way when the drawer closes.
            val stagger = index * ROW_STAGGER_MS
            Row(
                Modifier
                    .fillMaxWidth()
                    .heightIn(min = 52.dp)
                    .animateEnterExit(
                        enter = slideInVertically(tween(STRIP_MS, delayMillis = stagger, easing = LinearOutSlowInEasing)) { -it / 2 } +
                            fadeIn(tween(STRIP_MS, delayMillis = stagger)),
                        exit = slideOutVertically(tween(STRIP_MS, easing = LinearOutSlowInEasing)) { -it / 2 } + fadeOut(tween(STRIP_MS / 2)),
                    )
                    .clip(KeyShape)
                    .clickable(role = Role.Button, onClickLabel = stringResource(R.string.ui_strip_open_setting)) { onOpen(effect.name) }
                    .padding(horizontal = 8.dp, vertical = 6.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Icon(
                    painterResource(effectIcon(effect.name)),
                    contentDescription = null,
                    tint = colour,
                    modifier = with(shared) {
                        Modifier.sharedElement(rememberSharedContentState(effectKey(effect.name)), animatedVisibilityScope = drawer, boundsTransform = stripBounds)
                    }.size(18.dp),
                )
                Spacer(Modifier.width(12.dp))
                Column(Modifier.weight(1f)) {
                    Text(stringResource(effectLabel(effect.name)), style = MaterialTheme.typography.bodyMedium, color = Palette.StripText)
                    val detail = effectDetail(effect, state, output, scheduledNight, status.reverbApps.map { appLabel(context, it) })
                    if (detail != null) Text(detail, style = MaterialTheme.typography.bodySmall, color = Palette.StripDim)
                }
                Icon(if (effect.on) Icons.Filled.Check else Icons.Filled.Close, contentDescription = null, tint = colour, modifier = Modifier.size(14.dp))
                Spacer(Modifier.width(4.dp))
                Text(stringResource(if (effect.on) R.string.ui_state_on else R.string.ui_mode_off), style = Readout, color = colour)
                Icon(
                    Icons.AutoMirrored.Filled.KeyboardArrowRight,
                    contentDescription = null,
                    tint = Palette.StripDim,
                    modifier = Modifier.padding(start = 4.dp).size(18.dp),
                )
            }
        }
    }
}
}

private const val ROW_STAGGER_MS = 25
/** What an effect is doing right now, in a few words. */
@Composable
private fun effectDetail(effect: EffectState, state: AppState, output: OutputInfo, scheduledNight: Boolean, reverbApps: List<String>): String? {
    val tone = state.current.tone
    fun pct(value: Double) = "${kotlin.math.round(value * 100).toInt()}%"
    return when (effect.name) {
        "Night" -> {
            val amount = if (scheduledNight) maxOf(state.nightMode, state.nightSchedule.amount) else state.nightMode
            stringResource(if (scheduledNight && state.nightMode < state.nightSchedule.amount) R.string.ui_strip_scheduled else R.string.ui_strip_at, pct(amount))
        }
        "Level" -> stringResource(R.string.ui_strip_at, pct(state.leveller))
        "Comp" -> if (effect.on) {
            io.github.dragjon.eigentone.data.Dynamics.styles.firstOrNull { it.compressor == tone.compressor }?.name
                ?: stringResource(R.string.ui_strip_comp_custom)
        } else {
            stringResource(R.string.ui_strip_comp_off)
        }
        "Width" -> if (effect.on) stringResource(R.string.ui_strip_width, pct(tone.surround)) else stringResource(R.string.ui_strip_width_off)
        "Surround" -> if (effect.on) stringResource(R.string.ui_strip_at, pct(tone.surround)) else stringResource(R.string.ui_strip_surround_off)
        "Ears" -> when {
            effect.on -> stringResource(R.string.ui_strip_ears_on, output.label)
            state.hearings.containsKey(output.key) -> stringResource(R.string.ui_strip_ears_saved)
            else -> stringResource(R.string.ui_strip_ears_none)
        }
        "Reverb" -> when {
            !effect.on -> stringResource(R.string.ui_strip_reverb_off)
            reverbApps.isEmpty() -> stringResource(R.string.ui_strip_reverb_waiting, stringResource(reverbRoomName(tone.reverb)), pct(tone.reverbAmount))
            else -> stringResource(R.string.ui_strip_reverb_on, stringResource(reverbRoomName(tone.reverb)), pct(tone.reverbAmount), reverbApps.joinToString())
        }
        "Tame" -> stringResource(
            R.string.ui_strip_tame,
            if (tone.harshTamer > 0.0) pct(tone.harshTamer) else stringResource(R.string.ui_mode_off).lowercase(),
            if (tone.boomTamer > 0.0) pct(tone.boomTamer) else stringResource(R.string.ui_mode_off).lowercase(),
        )
        else -> null
    }
}

private fun reverbRoomName(room: io.github.dragjon.eigentone.data.ReverbRoom): Int = when (room) {
    io.github.dragjon.eigentone.data.ReverbRoom.OFF -> R.string.ui_mode_off
    io.github.dragjon.eigentone.data.ReverbRoom.SMALL_ROOM -> R.string.ui_reverb_small_room
    io.github.dragjon.eigentone.data.ReverbRoom.MEDIUM_ROOM -> R.string.ui_reverb_medium_room
    io.github.dragjon.eigentone.data.ReverbRoom.LARGE_ROOM -> R.string.ui_reverb_large_room
    io.github.dragjon.eigentone.data.ReverbRoom.MEDIUM_HALL -> R.string.ui_reverb_medium_hall
    io.github.dragjon.eigentone.data.ReverbRoom.LARGE_HALL -> R.string.ui_reverb_large_hall
    io.github.dragjon.eigentone.data.ReverbRoom.PLATE -> R.string.ui_reverb_plate
}

private fun effectLabel(name: String): Int = when (name) {
    "Night" -> R.string.ui_fx_night
    "Level" -> R.string.ui_fx_level
    "Width" -> R.string.ui_fx_width
    "Surround" -> R.string.ui_fx_surround
    "Ears" -> R.string.ui_fx_ears
    "Tame" -> R.string.ui_fx_tame
    "Reverb" -> R.string.ui_fx_reverb
    else -> R.string.ui_fx_comp
}

/** An icon for the effect, then a tick or a cross, so the state doesn't rely on colour alone. */
@Composable
private fun EffectTag(effect: EffectState, iconModifier: Modifier = Modifier) {
    val colour = if (effect.on) Palette.StripOn else Palette.StripDim
    // Announced by the name shown in the drawer, not the short code used inside the app.
    val description = stringResource(if (effect.on) R.string.ui_effect_on else R.string.ui_effect_off, stringResource(effectLabel(effect.name)))
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier.semantics(mergeDescendants = true) { contentDescription = description },
    ) {
        Icon(painterResource(effectIcon(effect.name)), contentDescription = null, tint = colour, modifier = iconModifier.size(16.dp))
        Icon(
            if (effect.on) Icons.Filled.Check else Icons.Filled.Close,
            contentDescription = null,
            tint = colour,
            modifier = Modifier.size(10.dp),
        )
    }
}

private fun effectIcon(name: String): Int = when (name) {
    "Night" -> R.drawable.ic_fx_night
    "Level" -> R.drawable.ic_fx_level
    "Width" -> R.drawable.ic_fx_width
    "Surround" -> R.drawable.ic_fx_surround
    "Ears" -> R.drawable.ic_fx_ears
    "Tame" -> R.drawable.ic_fx_tame
    "Reverb" -> R.drawable.ic_fx_reverb
    else -> R.drawable.ic_fx_comp
}

@Composable
private fun EffectPill(name: String, colour: androidx.compose.ui.graphics.Color = Palette.StripOn) {
    Text(
        name,
        fontSize = 11.sp,
        lineHeight = 14.sp,
        color = colour,
        maxLines = 1,
    )
}

/** Pulses while Android reports music playing. Absent otherwise, so it can't be read as a marker. */
@Composable
private fun PlayingDot() {
    val playing = rememberMusicActive()
    if (playing) {
        val modifier = Modifier
            .size(8.dp)
            .semantics { contentDescription = appText(R.string.ui_music_is_playing) }
        val pulse by rememberInfiniteTransition(label = "playing").animateFloat(
            initialValue = 0.35f,
            targetValue = 1f,
            animationSpec = infiniteRepeatable(tween(PULSE_MS, easing = LinearEasing), RepeatMode.Reverse),
            label = "pulse",
        )
        Canvas(modifier) { drawCircle(Palette.StripOn.copy(alpha = pulse)) }
    }
}

/** The curve in use, for each ear when they differ, over the live spectrum if the analyser is on. */
@Composable
private fun MiniCurve(state: AppState, outputKey: String, working: Boolean) {
    val attenuation by Repo.attenuationDb.collectAsStateWithLifecycle()
    val spectrum = Repo.spectrum.collectAsStateWithLifecycle()
    val filters = remember(state, attenuation, outputKey) { Processing.effectiveFilters(state, attenuation, outputKey) }
    val channels = remember(filters, state.current.filters, state.earCorrections, outputKey, state.useSystemSurround) {
        Processing.channelFilters(state, outputKey, filters)
    }
    fun sample(list: List<io.github.dragjon.eigentone.dsp.Filter>) =
        DoubleArray(MINI_POINTS + 1) { i -> Eq.curveDb(list, Eq.logFreq(i.toDouble() / MINI_POINTS)) }
    val left = remember(filters, channels) { sample(channels?.first ?: filters) }
    val right = remember(channels) { channels?.second?.let(::sample) }
    // Drawn in the theme's own colours, which the strip follows.
    val strip = Palette.colors
    val leftColour = when {
        !working -> strip.textDim
        right != null -> strip.leftEar
        else -> strip.lit
    }
    val rightColour = if (working) strip.rightEar else strip.textDim

    Canvas(
        Modifier
            .size(width = 52.dp, height = 26.dp)
            .clip(RoundedCornerShape(4.dp))
            .background(strip.bar)
            .semantics { contentDescription = appText(R.string.ui_current_sound_curve) },
    ) {
        spectrum.value?.let { bands -> drawBars(bands) }
        val zero = size.height / 2
        drawLine(strip.line, Offset(0f, zero), Offset(size.width, zero), strokeWidth = 1f)
        drawMiniLine(left, leftColour, dashed = false)
        right?.let { drawMiniLine(it, rightColour, dashed = true) }
    }
}

private fun DrawScope.drawBars(bands: FloatArray) {
    if (bands.isEmpty()) return
    val barWidth = size.width / MINI_BARS
    for (bar in 0 until MINI_BARS) {
        val from = bar * bands.size / MINI_BARS
        val to = ((bar + 1) * bands.size / MINI_BARS).coerceAtLeast(from + 1).coerceAtMost(bands.size)
        val level = (from until to).maxOf { bands[it] }
        if (level > 0f) {
            val height = level * size.height
            drawRect(
                Palette.colors.textDim.copy(alpha = 0.35f),
                topLeft = Offset(bar * barWidth, size.height - height),
                size = Size(barWidth * 0.8f, height),
            )
        }
    }
}

private fun DrawScope.drawMiniLine(points: DoubleArray, colour: androidx.compose.ui.graphics.Color, dashed: Boolean) {
    val path = Path()
    points.forEachIndexed { i, db ->
        val x = size.width * i / MINI_POINTS
        val y = ((MINI_RANGE_DB - db.coerceIn(-MINI_RANGE_DB, MINI_RANGE_DB)) / (2 * MINI_RANGE_DB) * size.height).toFloat()
        if (i == 0) path.moveTo(x, y) else path.lineTo(x, y)
    }
    val dash = 2.dp.toPx()
    drawPath(
        path,
        colour,
        style = Stroke(
            width = 1.5.dp.toPx(),
            pathEffect = if (dashed) PathEffect.dashPathEffect(floatArrayOf(dash, dash)) else null,
        ),
    )
}

private const val PULSE_MS = 900
