package io.github.dragjon.eigentone.ui

import androidx.compose.ui.draw.shadow
import androidx.compose.ui.draw.drawBehind

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.border
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import io.github.dragjon.eigentone.R
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import io.github.dragjon.eigentone.data.Preset
import io.github.dragjon.eigentone.data.Repo

/** A choice between a few words. Longer words get wider keys, so none of them is cut off. */
@Composable
fun WordSegments(labels: List<String>, selected: Int, onSelect: (Int) -> Unit, modifier: Modifier = Modifier) {
    Row(
        modifier
            .fillMaxWidth()
            .height(IntrinsicSize.Min)
            .selectableGroup(),
        horizontalArrangement = Arrangement.spacedBy(2.dp),
    ) {
        labels.forEachIndexed { i, label ->
            val on = i == selected
            Box(
                Modifier
                    .weight(label.length + 4f)
                    .fillMaxHeight()
                    .heightIn(min = 48.dp)
                    .clip(KeyShape)
                    .background(if (on) Palette.Lit else Palette.SurfaceHigh)
                    .selectable(selected = on, role = Role.RadioButton, onClick = { onSelect(i) })
                    .padding(horizontal = 6.dp, vertical = 4.dp),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    label,
                    style = MaterialTheme.typography.labelLarge,
                    color = if (on) Palette.OnLit else Palette.Text,
                    textAlign = TextAlign.Center,
                    maxLines = 2,
                )
            }
        }
    }
}

/**
 * One switch between two or three choices: a track with a lit key that slides to the chosen one, so
 * it reads as a single control rather than separate buttons.
 */
@Composable
fun SlidingToggle(
    options: List<Pair<Int, String>>,
    selected: Int,
    onSelect: (Int) -> Unit,
    modifier: Modifier = Modifier,
) {
    androidx.compose.foundation.layout.BoxWithConstraints(
        modifier
            // A full touch height, with the same square-shouldered corners as every other key.
            .height(48.dp)
            .clip(KeyShape)
            .background(Palette.SurfaceHigh)
            .border(1.dp, Palette.Line, KeyShape)
            .padding(3.dp)
            .selectableGroup(),
    ) {
        val share = maxWidth / options.size
        val thumbX by androidx.compose.animation.core.animateDpAsState(share * selected, label = "toggle")
        Box(
            Modifier
                .offset(x = thumbX)
                .width(share)
                .fillMaxHeight()
                .clip(KeyShape)
                .background(Palette.Lit),
        )
        Row(Modifier.fillMaxSize()) {
            options.forEachIndexed { i, (icon, label) ->
                val on = i == selected
                Row(
                    Modifier
                        .weight(1f)
                        .fillMaxHeight()
                        .clip(KeyShape)
                        .selectable(selected = on, role = Role.RadioButton, onClick = { onSelect(i) })
                        .padding(horizontal = 10.dp),
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    androidx.compose.material3.Icon(
                        androidx.compose.ui.res.painterResource(icon),
                        contentDescription = null,
                        tint = if (on) Palette.OnLit else Palette.TextDim,
                        modifier = Modifier.size(18.dp),
                    )
                    Text(
                        label,
                        style = MaterialTheme.typography.labelLarge,
                        color = if (on) Palette.OnLit else Palette.Text,
                        maxLines = 1,
                        modifier = Modifier.padding(start = 6.dp),
                    )
                }
            }
        }
    }
}

val KeyShape = RoundedCornerShape(4.dp)
val PanelShape = RoundedCornerShape(4.dp)


/** A lit key: the primary action on a screen. */
@Composable
fun Button(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    contentPadding: PaddingValues = ButtonDefaults.ContentPadding,
    content: @Composable RowScope.() -> Unit,
) {
    val source = remember { MutableInteractionSource() }
    val pressed by source.collectIsPressedAsState()
    androidx.compose.material3.Button(
        onClick = onClick,
        // A lit key gives off a soft amber light, as a backlit key on a panel does.
        modifier = modifier.then(
            if (enabled) {
                Modifier.shadow(KEY_GLOW_DP.dp, KeyShape, clip = false, ambientColor = Palette.Glow, spotColor = Palette.Glow)
            } else {
                Modifier
            },
        ),
        enabled = enabled,
        shape = KeyShape,
        interactionSource = source,
        colors = ButtonDefaults.buttonColors(
            containerColor = if (pressed) Palette.LitPressed else Palette.Lit,
            contentColor = Palette.OnLit,
            disabledContainerColor = Palette.SurfaceHigh,
            disabledContentColor = Palette.TextDim,
        ),
        contentPadding = contentPadding,
        content = content,
    )
}

/** An unlit key: raised above the faceplate, for everything that isn't the primary action. */
@Composable
fun FilledTonalButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    contentPadding: PaddingValues = ButtonDefaults.ContentPadding,
    content: @Composable RowScope.() -> Unit,
) {
    val source = remember { MutableInteractionSource() }
    val pressed by source.collectIsPressedAsState()
    androidx.compose.material3.FilledTonalButton(
        onClick = onClick,
        modifier = modifier,
        enabled = enabled,
        shape = KeyShape,
        interactionSource = source,
        colors = ButtonDefaults.filledTonalButtonColors(
            containerColor = if (pressed) Palette.Surface else Palette.SurfaceHigh,
            contentColor = Palette.Text,
            disabledContainerColor = Palette.Surface,
            disabledContentColor = Palette.TextDim,
        ),
        border = BorderStroke(1.dp, Palette.Line),
        contentPadding = contentPadding,
        content = content,
    )
}

/** Plain text, because ivory is kept for what's lit or active. */
@Composable
fun TextButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    /** For actions that remove something, which DESIGN.md draws in Danger Orange. */
    danger: Boolean = false,
    content: @Composable RowScope.() -> Unit,
) {
    androidx.compose.material3.TextButton(
        onClick = onClick,
        modifier = modifier,
        enabled = enabled,
        shape = KeyShape,
        colors = ButtonDefaults.textButtonColors(contentColor = if (danger) Palette.Danger else Palette.Text),
        content = content,
    )
}

@Composable
fun FilledTonalIconButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    content: @Composable () -> Unit,
) {
    val source = remember { MutableInteractionSource() }
    val pressed by source.collectIsPressedAsState()
    androidx.compose.material3.FilledTonalIconButton(
        onClick = onClick,
        modifier = modifier,
        enabled = enabled,
        shape = KeyShape,
        interactionSource = source,
        colors = IconButtonDefaults.filledTonalIconButtonColors(
            containerColor = if (pressed) Palette.Surface else Palette.SurfaceHigh,
            contentColor = Palette.Text,
            disabledContainerColor = Palette.Surface,
            disabledContentColor = Palette.TextDim,
        ),
        content = content,
    )
}

/**
 * The current sound, which opens a list of presets from any tab, since switching is the most common
 * reason to open the app.
 */
@Composable
fun PresetSwitcher(
    label: String,
    problem: Boolean,
    presets: List<Preset>,
    currentId: String,
    favorites: Set<String> = emptySet(),
    onPick: (Preset) -> Unit,
    onAll: () -> Unit,
    modifier: Modifier = Modifier,
    header: @Composable () -> Unit,
) {
    var open by remember { mutableStateOf(false) }
    Box(modifier) {
        Column(
            Modifier
                .fillMaxWidth()
                .heightIn(min = 48.dp)
                .clip(KeyShape)
                .clickable(role = Role.DropdownList, onClickLabel = stringResource(R.string.ui_change_preset)) { open = true },
        ) {
            header()
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    label,
                    style = MaterialTheme.typography.bodySmall,
                    color = if (problem) Palette.Danger else Palette.TextDim,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f, fill = false),
                )
                Icon(
                    Icons.Filled.ArrowDropDown,
                    contentDescription = null,
                    tint = Palette.TextDim,
                    modifier = Modifier.size(20.dp),
                )
            }
        }
        DropdownMenu(expanded = open, onDismissRequest = { open = false }, containerColor = Palette.Tooltip) {
            presets.forEach { preset ->
                val current = preset.id == currentId
                DropdownMenuItem(
                    text = {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                preset.name,
                                color = if (current) Palette.Lit else Palette.Text,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                                modifier = Modifier.weight(1f, fill = false),
                            )
                            // Favourites carry the same star as in the preset list.
                            if (preset.id in favorites) {
                                Icon(
                                    androidx.compose.ui.res.painterResource(R.drawable.ic_star),
                                    contentDescription = stringResource(R.string.ui_favourite),
                                    tint = Palette.Lit,
                                    modifier = Modifier
                                        .padding(start = 6.dp)
                                        .size(16.dp),
                                )
                            }
                        }
                    },
                    leadingIcon = {
                        if (current) Icon(Icons.Filled.Check, contentDescription = stringResource(R.string.ui_selected), tint = Palette.Lit)
                    },
                    onClick = {
                        open = false
                        onPick(preset)
                    },
                )
            }
            HorizontalDivider(color = Palette.Line)
            DropdownMenuItem(
                text = { Text(stringResource(R.string.ui_all_presets)) },
                onClick = {
                    open = false
                    onAll()
                },
            )
        }
    }
}

/** Redo is the undo arrow mirrored. */
@Composable
fun UndoRedoButtons() {
    val canUndo by Repo.canUndo.collectAsStateWithLifecycle()
    val canRedo by Repo.canRedo.collectAsStateWithLifecycle()
    Row {
        // Coloured here rather than inherited, so they show on any surface, such as the landscape faders' bar.
        IconButton(enabled = canUndo, onClick = Repo::undo) {
            Icon(
                painterResource(R.drawable.ic_undo),
                contentDescription = stringResource(R.string.ui_undo),
                tint = if (canUndo) Palette.Text else Palette.TextDim.copy(alpha = 0.4f),
                modifier = Modifier.size(20.dp),
            )
        }
        IconButton(enabled = canRedo, onClick = Repo::redo) {
            Icon(
                painterResource(R.drawable.ic_undo),
                contentDescription = stringResource(R.string.ui_redo),
                tint = if (canRedo) Palette.Text else Palette.TextDim.copy(alpha = 0.4f),
                modifier = Modifier
                    .size(20.dp)
                    .graphicsLayer(scaleX = -1f),
            )
        }
    }
}

/**
 * Material's alert dialog, opening and closing with the same motion as every other dialog in the app,
 * so it shrinks and fades away rather than vanishing.
 */
@Composable
fun MotionDialog(
    onDismissRequest: () -> Unit,
    confirmButton: @Composable () -> Unit,
    modifier: Modifier = Modifier,
    dismissButton: (@Composable () -> Unit)? = null,
    title: (@Composable () -> Unit)? = null,
    text: (@Composable () -> Unit)? = null,
    containerColor: androidx.compose.ui.graphics.Color = Palette.Tooltip,
    titleContentColor: androidx.compose.ui.graphics.Color = Palette.Text,
    textContentColor: androidx.compose.ui.graphics.Color = Palette.Text,
    properties: androidx.compose.ui.window.DialogProperties = androidx.compose.ui.window.DialogProperties(),
) {
    androidx.compose.material3.AlertDialog(
        onDismissRequest = onDismissRequest,
        confirmButton = {
            DialogMotionEffect()
            confirmButton()
        },
        modifier = modifier,
        dismissButton = dismissButton,
        title = title,
        text = text,
        containerColor = containerColor,
        titleContentColor = titleContentColor,
        textContentColor = textContentColor,
        properties = properties,
    )
}

/** Gives the dialog window this is drawn in the app's opening and closing animation. */
@Composable
fun DialogMotionEffect() {
    val view = androidx.compose.ui.platform.LocalView.current
    androidx.compose.runtime.SideEffect {
        val provider = view as? androidx.compose.ui.window.DialogWindowProvider
            ?: view.parent as? androidx.compose.ui.window.DialogWindowProvider
        provider?.window?.setWindowAnimations(io.github.dragjon.eigentone.R.style.DialogMotion)
    }
}

private const val KEY_GLOW_DP = 4

/**
 * A soft round glow behind something lit, such as the power switch while it's on. Draws nothing in
 * daylight, or when [on] is false.
 */
fun Modifier.litGlow(on: Boolean, strength: Float = 0.16f): Modifier = this.drawBehind {
    val glow = Palette.Glow
    if (!on || glow.alpha == 0f) return@drawBehind
    // Kept close, so it lights the switch without reaching the keys beside it.
    val radius = size.maxDimension * 0.5f
    drawCircle(
        androidx.compose.ui.graphics.Brush.radialGradient(listOf(glow.copy(alpha = strength), androidx.compose.ui.graphics.Color.Transparent), center = center, radius = radius),
        radius = radius,
        center = center,
    )
}
