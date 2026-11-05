package io.github.dragjon.eigentone.ui

import android.os.SystemClock
import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutLinearInEasing
import androidx.compose.animation.core.LinearOutSlowInEasing
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import io.github.dragjon.eigentone.R
import io.github.dragjon.eigentone.data.OutputKind
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.launch
import kotlin.math.cos
import kotlin.math.sin

@DrawableRes
fun OutputKind.icon(): Int = when (this) {
    OutputKind.PHONE -> R.drawable.ic_tab_phone
    OutputKind.HEADPHONES -> R.drawable.ic_tab_headphones
    OutputKind.EARBUDS -> R.drawable.ic_tab_earbuds
    OutputKind.SPEAKER -> R.drawable.ic_tab_speaker
    OutputKind.CAR -> R.drawable.ic_tab_car
}

@StringRes
fun OutputKind.tabLabel(): Int = when (this) {
    OutputKind.PHONE -> R.string.ui_tab_phone
    OutputKind.HEADPHONES -> R.string.ui_tab_headphones
    OutputKind.EARBUDS -> R.string.ui_tab_earbuds
    OutputKind.SPEAKER -> R.string.ui_tab_speaker
    OutputKind.CAR -> R.string.ui_tab_car
}

/**
 * The device tab's icon. When a different kind of device takes over, the old icon circles inward and
 * collapses to a point, then the new one springs out of it, while a ring spreads from the pill to say
 * something changed. Changes in the first moments after opening just swap, since the app is still
 * finding out what's connected.
 */
@Composable
fun DeviceTabIcon(kind: OutputKind, tint: Color, ring: Color) {
    var shown by remember { mutableStateOf(kind) }
    val opened = remember { SystemClock.uptimeMillis() }
    val scale = remember { Animatable(1f) }
    val orbit = remember { Animatable(0f) }
    val pulse = remember { Animatable(0f) }

    LaunchedEffect(kind) {
        if (kind == shown) return@LaunchedEffect
        if (SystemClock.uptimeMillis() - opened < SETTLE_MS) {
            shown = kind
            return@LaunchedEffect
        }
        coroutineScope {
            launch {
                pulse.snapTo(0f)
                pulse.animateTo(1f, tween(PULSE_MS, easing = LinearOutSlowInEasing))
            }
            // Out: one turn around a shrinking circle, ending at the centre.
            launch { orbit.animateTo(1f, tween(OUT_MS, easing = FastOutLinearInEasing)) }
            scale.animateTo(0f, tween(OUT_MS, easing = FastOutLinearInEasing))
            shown = kind
            orbit.snapTo(0f)
            // In: grows from the point with a little overshoot.
            scale.animateTo(1f, spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessMediumLow))
        }
    }

    Box(contentAlignment = Alignment.Center) {
        Canvas(Modifier.size(width = 64.dp, height = 32.dp)) {
            val p = pulse.value
            if (p > 0f && p < 1f) {
                drawRoundRect(
                    ring.copy(alpha = (1f - p) * 0.8f),
                    topLeft = androidx.compose.ui.geometry.Offset(-p * 10.dp.toPx(), -p * 10.dp.toPx()),
                    size = androidx.compose.ui.geometry.Size(size.width + p * 20.dp.toPx(), size.height + p * 20.dp.toPx()),
                    cornerRadius = androidx.compose.ui.geometry.CornerRadius(16.dp.toPx() + p * 10.dp.toPx()),
                    style = Stroke(width = 2.dp.toPx() * (1f - p) + 0.5f),
                )
            }
        }
        Icon(
            painterResource(shown.icon()),
            contentDescription = null,
            tint = tint,
            modifier = Modifier.graphicsLayer {
                val t = orbit.value
                // The radius shrinks as it turns, so the path is an inward spiral.
                val radius = ORBIT_DP.dp.toPx() * sin(t * Math.PI).toFloat()
                val angle = t * 2 * Math.PI - Math.PI / 2
                translationX = radius * cos(angle).toFloat()
                translationY = radius * sin(angle).toFloat()
                rotationZ = t * 360f
                scaleX = scale.value
                scaleY = scale.value
            },
        )
    }
}

private const val SETTLE_MS = 1500L
private const val OUT_MS = 380
private const val PULSE_MS = 900
private const val ORBIT_DP = 9
