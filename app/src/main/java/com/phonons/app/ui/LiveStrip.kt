package com.phonons.app.ui

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
import com.phonons.app.R
import com.phonons.app.data.AppState
import com.phonons.app.data.ClipRisk
import com.phonons.app.data.EffectState
import com.phonons.app.data.OutputInfo
import com.phonons.app.data.Processing
import com.phonons.app.data.Repo
import com.phonons.app.dsp.Eq

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

    Row(
        Modifier
            .fillMaxWidth()
            .background(Palette.Strip)
            .heightIn(min = STRIP_HEIGHT_DP.dp)
            .padding(horizontal = 16.dp, vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        Icon(
            painterResource(outputIcon(output)),
            contentDescription = null,
            tint = if (working) Palette.StripOn else Palette.StripDim,
            modifier = Modifier.size(16.dp),
        )
        Text(
            output.label,
            style = MaterialTheme.typography.bodySmall,
            color = Palette.StripText,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.weight(1f),
        )
        if (clipping) EffectPill(stringResource(R.string.ui_clip_risk), Palette.StripDanger)
        effects.forEach { EffectTag(it) }
        PlayingDot()
        if (showCurve) MiniCurve(state, output.key, working)
    }
}

/** A tick or a cross as well as the colour, so the state doesn't rely on colour alone. */
@Composable
private fun EffectTag(effect: EffectState) {
    val colour = if (effect.on) Palette.StripOn else Palette.StripDim
    val description = stringResource(if (effect.on) R.string.ui_effect_on else R.string.ui_effect_off, effect.name)
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier.semantics(mergeDescendants = true) { contentDescription = description },
    ) {
        Icon(
            if (effect.on) Icons.Filled.Check else Icons.Filled.Close,
            contentDescription = null,
            tint = colour,
            modifier = Modifier.size(12.dp),
        )
        Text(effect.name, fontSize = 11.sp, lineHeight = 14.sp, color = colour, maxLines = 1)
    }
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
    val filters = remember(state, attenuation) { Processing.effectiveFilters(state, attenuation) }
    val channels = remember(filters, state.earCorrections, outputKey, state.useSystemSurround) {
        Processing.channelFilters(state, outputKey, filters)
    }
    fun sample(list: List<com.phonons.app.dsp.Filter>) =
        DoubleArray(MINI_POINTS + 1) { i -> Eq.curveDb(list, Eq.logFreq(i.toDouble() / MINI_POINTS)) }
    val left = remember(filters, channels) { sample(channels?.first ?: filters) }
    val right = remember(channels) { channels?.second?.let(::sample) }
    val leftColour = when {
        !working -> Palette.TextDim
        right != null -> Palette.LeftEar
        else -> Palette.Lit
    }
    val rightColour = if (working) Palette.RightEar else Palette.TextDim

    Canvas(
        Modifier
            .size(width = 52.dp, height = 26.dp)
            .clip(RoundedCornerShape(4.dp))
            .background(Palette.Bar)
            .semantics { contentDescription = appText(R.string.ui_current_sound_curve) },
    ) {
        spectrum.value?.let { bands -> drawBars(bands) }
        val zero = size.height / 2
        drawLine(Palette.Line, Offset(0f, zero), Offset(size.width, zero), strokeWidth = 1f)
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
                Palette.TextDim.copy(alpha = 0.35f),
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
