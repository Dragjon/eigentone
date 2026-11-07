package io.github.dragjon.eigentone.ui

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import io.github.dragjon.eigentone.R
import io.github.dragjon.eigentone.data.CurveDifference
import io.github.dragjon.eigentone.data.Preset
import io.github.dragjon.eigentone.data.Processing
import io.github.dragjon.eigentone.data.Repo
import io.github.dragjon.eigentone.dsp.GraphicEq

/**
 * Two presets' curves on one graph, and what sets the first apart from the second in plain words, such as
 * "more bass" or "less presence". It only looks: nothing about the sound changes while it's open.
 */
@Composable
fun CurveCompareScreen(onClose: () -> Unit) {
    val state by Repo.state.collectAsStateWithLifecycle()
    val presets = remember(state.presets) { Repo.allPresets(state) }
    // The one in use first, against the one most likely to be compared with it: the next of your own.
    var firstId by rememberSaveable { mutableStateOf(state.current.id) }
    var secondId by rememberSaveable {
        mutableStateOf(state.presets.firstOrNull { it.id != state.current.id }?.id ?: presets.first { it.id != state.current.id }.id)
    }
    // The one in use is compared as it sounds now, edits included.
    fun find(id: String): Preset = if (id == state.current.id) state.current else presets.firstOrNull { it.id == id } ?: state.current
    val first = find(firstId)
    val second = find(secondId)
    // Read as the first against the second, so the rows say what the first has more or less of.
    val summary = remember(first, second) { CurveDifference.compare(second, first) }

    BackHandler(onBack = onClose)
    ToolScreen(title = stringResource(R.string.ui_compare_curves), onBack = onClose) {
        Text(stringResource(R.string.ui_compare_curves_intro), style = MaterialTheme.typography.bodyLarge, color = Palette.TextDim)
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            PresetPicker(stringResource(R.string.ui_curve_first), first, presets, solid = true, Modifier.weight(1f)) { firstId = it }
            PresetPicker(stringResource(R.string.ui_curve_second), second, presets, solid = false, Modifier.weight(1f)) { secondId = it }
        }
        EqGraph(
            filters = emptyList(),
            curveFilters = remember(first) { Processing.presetFilters(first) },
            spectrum = { null },
            selectedId = null,
            dimmed = false,
            onSelect = {},
            onDrag = { _, _, _ -> },
            onDragEnd = {},
            modifier = Modifier
                .clip(PanelShape)
                .background(Palette.Surface),
            heightDp = 220,
            overlay = remember(second) { Processing.presetFilters(second) },
        )
        // A one-line verdict first, then each range that differs, biggest first, as rows to scan.
        Text(headline(subject = first.name, against = second.name, summary), style = MaterialTheme.typography.titleMedium, color = Palette.Text)
        SettingsSection(stringResource(R.string.ui_curve_differences)) {
            // Names the preset the rows describe, so "Less presence" plainly means this one has less.
            Row(
                Modifier.padding(start = 16.dp, end = 16.dp, top = 12.dp, bottom = 4.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                LineKey(solid = true)
                Text(
                    first.name,
                    style = MaterialTheme.typography.titleSmall,
                    color = Palette.Text,
                    modifier = Modifier.padding(start = 8.dp),
                )
            }
            RowDivider()
            if (summary.parts.isEmpty()) {
                Text(
                    stringResource(if (summary.nearlyTheSame) R.string.ui_curve_same_short else R.string.ui_curve_small_short),
                    style = MaterialTheme.typography.bodyLarge,
                    color = Palette.TextDim,
                    modifier = Modifier.padding(16.dp),
                )
            }
            summary.parts.take(MAX_LINES).forEachIndexed { index, part ->
                if (index > 0) RowDivider()
                DifferenceRow(part)
            }
        }
        Text(stringResource(R.string.ui_curve_differences_note), style = MaterialTheme.typography.bodySmall, color = Palette.TextDim)
    }
}

/** What sets the second apart, in one line: its overall balance when it leans, or plainly that it differs. */
@Composable
private fun headline(subject: String, against: String, summary: CurveDifference.Summary): String = when {
    summary.nearlyTheSame -> stringResource(R.string.ui_curve_same, subject, against)
    summary.tilt >= CurveDifference.WARMER_DB -> stringResource(R.string.ui_curve_warmer, subject, against)
    summary.tilt <= -CurveDifference.WARMER_DB -> stringResource(R.string.ui_curve_brighter, subject, against)
    else -> stringResource(R.string.ui_curve_headline, subject, against)
}

/** One range: which way it goes, what it's called and where it is, how much, and a bar to show the size at a glance. */
@Composable
private fun DifferenceRow(part: CurveDifference.Part) {
    val more = part.db > 0
    val colour = if (more) Palette.Lit else Palette.Text
    Row(
        Modifier
            .fillMaxWidth()
            .heightIn(min = 64.dp)
            .padding(start = 12.dp, end = 16.dp, top = 8.dp, bottom = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(
            if (more) Icons.Filled.KeyboardArrowUp else Icons.Filled.KeyboardArrowDown,
            contentDescription = null,
            tint = colour,
            modifier = Modifier.size(28.dp),
        )
        Column(Modifier.weight(1f).padding(start = 8.dp)) {
            Text(
                stringResource(if (more) R.string.ui_curve_row_more else R.string.ui_curve_row_less, stringResource(regionName(part.region))),
                style = MaterialTheme.typography.bodyLarge,
                color = Palette.Text,
            )
            Text(
                stringResource(R.string.ui_curve_range, GraphicEq.formatFrequency(part.region.lowHz), GraphicEq.formatFrequency(part.region.highHz)),
                style = MaterialTheme.typography.bodySmall,
                color = Palette.TextDim,
            )
        }
        Column(horizontalAlignment = Alignment.End, verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Text(String.format(java.util.Locale.US, "%+.1f dB", part.db), style = Readout, color = colour)
            SizeBar(part.db, colour)
        }
    }
}

/** A short scale with its zero in the middle, filled left for less and right for more, up to 6 dB either way. */
@Composable
private fun SizeBar(db: Double, colour: Color) {
    val track = Palette.Track
    Canvas(Modifier.size(width = 72.dp, height = 6.dp)) {
        val mid = size.width / 2
        drawRect(track, topLeft = Offset(0f, size.height / 2 - 0.5.dp.toPx()), size = androidx.compose.ui.geometry.Size(size.width, 1.dp.toPx()))
        val reach = (kotlin.math.abs(db) / 6.0).coerceAtMost(1.0).toFloat() * mid
        val left = if (db > 0) mid else mid - reach
        drawRect(colour, topLeft = Offset(left, 0f), size = androidx.compose.ui.geometry.Size(reach, size.height))
        drawRect(track, topLeft = Offset(mid - 0.5.dp.toPx(), 0f), size = androidx.compose.ui.geometry.Size(1.dp.toPx(), size.height))
    }
}

private const val MAX_LINES = 6

private fun regionName(region: CurveDifference.Region): Int = when (region) {
    CurveDifference.Region.DEEP_BASS -> R.string.ui_region_deep_bass
    CurveDifference.Region.BASS -> R.string.ui_region_bass
    CurveDifference.Region.LOWER_MIDS -> R.string.ui_region_lower_mids
    CurveDifference.Region.MIDS -> R.string.ui_region_mids
    CurveDifference.Region.PRESENCE -> R.string.ui_region_presence
    CurveDifference.Region.TREBLE -> R.string.ui_region_treble
    CurveDifference.Region.AIR -> R.string.ui_region_air
}
/** Chooses one preset, showing which line on the graph is its: solid amber or dashed. */
@Composable
private fun PresetPicker(label: String, chosen: Preset, presets: List<Preset>, solid: Boolean, modifier: Modifier, onPick: (String) -> Unit) {
    var open by remember { mutableStateOf(false) }
    Box(modifier) {
        Column(
            Modifier
                .fillMaxWidth()
                .clip(PanelShape)
                .background(Palette.SurfaceHigh)
                .clickable(role = Role.DropdownList) { open = true }
                .heightIn(min = 64.dp)
                .padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                LineKey(solid)
                Text(label, style = MaterialTheme.typography.labelMedium, color = Palette.TextDim, modifier = Modifier.padding(start = 8.dp))
            }
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    chosen.name,
                    style = MaterialTheme.typography.bodyMedium,
                    color = Palette.Text,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f),
                )
                Icon(Icons.Filled.ArrowDropDown, contentDescription = null, tint = Palette.TextDim)
            }
        }
        // Capped, so a long list scrolls rather than running under the navigation bar out of reach.
        DropdownMenu(expanded = open, onDismissRequest = { open = false }, containerColor = Palette.Tooltip, modifier = Modifier.heightIn(max = MENU_MAX_DP.dp)) {
            presets.forEach { preset ->
                DropdownMenuItem(
                    text = { Text(preset.name, color = if (preset.id == chosen.id) Palette.Lit else Palette.Text, maxLines = 1, overflow = TextOverflow.Ellipsis) },
                    onClick = {
                        open = false
                        onPick(preset.id)
                    },
                )
            }
        }
    }
}

/** A short sample of a curve's line, so the pickers say which line is which. */
@Composable
private fun LineKey(solid: Boolean) {
    val colour: Color = if (solid) Palette.Lit else Palette.Text
    Canvas(Modifier.width(22.dp).size(width = 22.dp, height = 10.dp)) {
        val y = size.height / 2
        drawLine(
            colour,
            Offset(0f, y),
            Offset(size.width, y),
            strokeWidth = 2.dp.toPx(),
            pathEffect = if (solid) null else PathEffect.dashPathEffect(floatArrayOf(4.dp.toPx(), 3.dp.toPx())),
        )
    }
}
