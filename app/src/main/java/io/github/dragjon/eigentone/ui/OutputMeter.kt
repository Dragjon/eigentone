package io.github.dragjon.eigentone.ui

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import io.github.dragjon.eigentone.R
import io.github.dragjon.eigentone.data.Repo
import java.util.Locale

/**
 * A slim level meter for everything playing, standing at the graph's right edge like an EQ's output
 * meter. The fill is the average (RMS) level and the tick the peak, held for a moment. It stays quiet
 * until peaks reach full scale, where it turns red, since that's when sound can clip. Empty while the
 * analyser is off, so the space doesn't jump when it turns on.
 */
@Composable
fun OutputMeterBar(modifier: Modifier = Modifier) {
    val level = Repo.level.collectAsStateWithLifecycle()
    // Out of view it isn't read, so a page kept ready off screen doesn't redraw with every reading.
    val shown = LocalPageShown.current
    val now = if (shown()) level.value else null
    val track = Palette.Track2
    Canvas(
        modifier
            .width(METER_WIDTH_DP.dp)
            .fillMaxHeight()
            .semantics {
                contentDescription = if (now == null) "" else appText(R.string.ui_output_meter_description, formatLevelDb(now.rmsDb), formatLevelDb(now.heldPeakDb))
            },
    ) {
        if (now == null) return@Canvas
        val radius = CornerRadius(size.width / 2)
        fun yOf(db: Float) = (1f - ((db - MIN_DB) / -MIN_DB).coerceIn(0f, 1f)) * size.height
        val hot = now.heldPeakDb > CLIP_DB
        // The same track as every slider and fader, and a solid fill, so it reads as part of the set rather than
        // a washed-out grey with a faint fill (user decision).
        drawRoundRect(track, size = size, cornerRadius = radius)
        val top = yOf(now.rmsDb)
        drawRoundRect(
            if (hot) Palette.Danger else Palette.LitFill,
            topLeft = Offset(0f, top),
            size = Size(size.width, size.height - top),
            cornerRadius = radius,
        )
        val peak = yOf(now.heldPeakDb)
        drawRect(if (hot) Palette.Danger else Palette.LitBright, topLeft = Offset(0f, peak - 1.dp.toPx()), size = Size(size.width, 2.dp.toPx()))
    }
}

private fun formatLevelDb(db: Float): String =
    if (db <= MIN_DB) "−∞ dB" else String.format(Locale.US, "%.1f dB", db).replace("-", "−")

private const val MIN_DB = -60f
private const val CLIP_DB = -0.5f
const val METER_WIDTH_DP = 6
