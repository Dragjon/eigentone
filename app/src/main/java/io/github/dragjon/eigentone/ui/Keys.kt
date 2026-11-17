package io.github.dragjon.eigentone.ui

import androidx.compose.ui.draw.shadow
import androidx.compose.ui.draw.drawBehind
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.verticalScroll

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
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInParent
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import io.github.dragjon.eigentone.R
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import io.github.dragjon.eigentone.data.Preset
import io.github.dragjon.eigentone.data.Repo

/** One option in a [SlidingToggle]: words, an icon, or both. An icon alone is still named for screen readers. */
data class ToggleOption(val label: String, val icon: Int? = null, val showLabel: Boolean = true, val description: String? = null)

/** A choice between a few words, as a [SlidingToggle]. Longer words get wider keys, so none of them is cut off. */
@Composable
fun WordSegments(labels: List<String>, selected: Int, onSelect: (Int) -> Unit, modifier: Modifier = Modifier) {
    SlidingToggle(labels.map { ToggleOption(it) }, selected, onSelect, modifier.fillMaxWidth(), weighted = true)
}

/** The icon-and-word form, kept for the places that already pass pairs. */
@Composable
fun SlidingToggle(
    options: List<Pair<Int, String>>,
    selected: Int,
    onSelect: (Int) -> Unit,
    modifier: Modifier = Modifier,
) = SlidingToggle(options.map { (icon, label) -> ToggleOption(label, icon) }, selected, onSelect, modifier, weighted = false)

/**
 * Every few-way choice in the app (user decision): a casing with a thumb that slides to the chosen option and takes
 * its width, so it reads as one control rather than separate buttons. Quiet, as the graph is: a thin outline, a
 * faint tint of the theme's colour on the choice and its words lit, rather than a solid fill (user decision). The
 * press flash has the thumb's own shape, so a tap never shows a second, differently rounded box.
 */
@Composable
fun SlidingToggle(
    options: List<ToggleOption>,
    selected: Int,
    onSelect: (Int) -> Unit,
    modifier: Modifier = Modifier,
    /** Wider keys for longer words, rather than equal shares. */
    weighted: Boolean = false,
    height: androidx.compose.ui.unit.Dp = 48.dp,
) {
    val density = androidx.compose.ui.platform.LocalDensity.current
    // Where each option sits, learnt as they're laid out, so the thumb can travel to any of them.
    val spans = remember(options.size) { androidx.compose.runtime.mutableStateListOf<Pair<Float, Float>>().apply { repeat(options.size) { add(0f to 0f) } } }
    val target = spans.getOrElse(selected) { 0f to 0f }
    // On the same curve and time as the pages and panels the toggle changes, so they all move as one.
    val thumbX by androidx.compose.animation.core.animateFloatAsState(target.first, tween(TOGGLE_MS, easing = androidx.compose.animation.core.LinearOutSlowInEasing), label = "toggleX")
    val thumbW by androidx.compose.animation.core.animateFloatAsState(target.second, tween(TOGGLE_MS, easing = androidx.compose.animation.core.LinearOutSlowInEasing), label = "toggleWidth")
    Box(
        modifier
            .height(height)
            .clip(KeyShape)
            .border(1.dp, Palette.KeyBorder, KeyShape)
            .padding(TOGGLE_INSET_DP.dp)
            .selectableGroup(),
    ) {
        if (thumbW > 0f) {
            Box(
                Modifier
                    .offset { androidx.compose.ui.unit.IntOffset(thumbX.toInt(), 0) }
                    .width(with(density) { thumbW.toDp() })
                    .fillMaxHeight()
                    // Its corners follow the casing's, less the gap between them, so the two curves run parallel.
                    .clip(ToggleThumbShape)
                    .background(Palette.LitFill.copy(alpha = QUIET_TINT))
                    .border(1.dp, Palette.LitFill.copy(alpha = QUIET_EDGE), ToggleThumbShape),
            )
        }
        Row(Modifier.fillMaxSize()) {
            options.forEachIndexed { i, option ->
                val on = i == selected
                Row(
                    Modifier
                        .weight(if (weighted) option.label.length + 4f else 1f)
                        .fillMaxHeight()
                        .onGloballyPositioned { spans[i] = it.positionInParent().x to it.size.width.toFloat() }
                        .clip(ToggleThumbShape)
                        .selectable(selected = on, role = Role.RadioButton, onClick = { onSelect(i) })
                        .then(if (option.showLabel && option.description == null) Modifier else Modifier.semantics { contentDescription = option.description ?: option.label })
                        .padding(horizontal = if (option.showLabel) 8.dp else 0.dp),
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    option.icon?.let { icon ->
                        androidx.compose.material3.Icon(
                            androidx.compose.ui.res.painterResource(icon),
                            contentDescription = null,
                            tint = if (on) Palette.LitBright else Palette.TextDim,
                            modifier = Modifier.size(18.dp),
                        )
                    }
                    if (option.showLabel) {
                        WholeWordText(
                            option.label,
                            style = MaterialTheme.typography.labelLarge,
                            color = if (on) Palette.LitBright else Palette.TextDim,
                            textAlign = TextAlign.Center,
                            maxLines = 2,
                            modifier = Modifier.padding(start = if (option.icon != null) 6.dp else 0.dp),
                        )
                    }
                }
            }
        }
    }
}

/** Keys, segments and pressed rows: soft and nearly pill-shaped, as in Material You (user decision). */
val KeyShape = Corner.large

/** Panels and cards, rounded like Material You's large cards, and gentle enough that content near a corner sits clear of it. */
val PanelShape = Corner.medium

/** How strongly the theme's colour tints a chosen option, and the edge around it: faint, so the words carry it. */
internal const val QUIET_TINT = 0.16f
internal const val QUIET_EDGE = 0.45f

/** The gap between a sliding toggle's casing and its thumb. */
private const val TOGGLE_INSET_DP = 3

/** The same as a tab's slide, so a toggle and what it changes move together. */
private const val TOGGLE_MS = 240

/** A sliding toggle's thumb, whose corners run parallel to its casing's. */
private val ToggleThumbShape = RoundedCornerShape((20 - TOGGLE_INSET_DP).dp)


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
            containerColor = if (pressed) Palette.LitPressed else Palette.LitFill,
            contentColor = Palette.OnLit,
            disabledContainerColor = Palette.SurfaceHigh,
            disabledContentColor = Palette.TextDim,
        ),
        contentPadding = contentPadding,
        content = content,
    )
}

/**
 * Everything that isn't a screen's main action: a thin outline on the panel, with no fill, so only the one main
 * action is solid (user decision).
 */
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
            containerColor = if (pressed) Palette.LitFill.copy(alpha = QUIET_TINT) else Color.Transparent,
            contentColor = Palette.Text,
            disabledContainerColor = Color.Transparent,
            disabledContentColor = Palette.TextDim,
        ),
        border = BorderStroke(1.dp, if (enabled) Palette.KeyBorder else Palette.Line),
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
        enabled = enabled,
        shape = KeyShape,
        interactionSource = source,
        colors = IconButtonDefaults.filledTonalIconButtonColors(
            containerColor = if (pressed) Palette.LitFill.copy(alpha = QUIET_TINT) else Color.Transparent,
            contentColor = Palette.Text,
            disabledContainerColor = Color.Transparent,
            disabledContentColor = Palette.TextDim,
        ),
        modifier = modifier.border(1.dp, if (enabled) Palette.KeyBorder else Palette.Line, KeyShape),
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
                // Smaller corners than a key, so the press highlight never clips the name at its edge.
                .clip(Corner.extraSmall)
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
        // Capped, so a long list scrolls rather than running under the navigation bar out of reach.
        // Opens on the status strip, its corner on the strip's, so the strip's rounded end doesn't poke out beside it
        // (tester feedback).
        MotionMenu(expanded = open, onDismissRequest = { open = false }, pin = { StripDrawer.bounds }) {
            presets.forEach { preset ->
                val current = preset.id == currentId
                DropdownMenuItem(
                    text = {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                preset.name,
                                color = if (current) Palette.LitBright else Palette.Text,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                                modifier = Modifier.weight(1f, fill = false),
                            )
                            // Favourites carry the same star as in the preset list.
                            if (preset.id in favorites) {
                                Icon(
                                    androidx.compose.ui.res.painterResource(R.drawable.ic_star),
                                    contentDescription = stringResource(R.string.ui_favourite),
                                    tint = Palette.LitBright,
                                    modifier = Modifier
                                        .padding(start = 6.dp)
                                        .size(16.dp),
                                )
                            }
                        }
                    },
                    leadingIcon = {
                        if (current) Icon(Icons.Filled.Check, contentDescription = stringResource(R.string.ui_selected), tint = Palette.LitBright)
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
    // A dialog opened as another one closes can be handed that one's last back press or outside tap, or the
    // keyboard closing, which on Android 11 shut it the moment it appeared. Nobody means to close a dialog
    // that fast, so a request that early is ignored, and logged so the debug console shows it happened.
    val openedAt = remember { android.os.SystemClock.uptimeMillis() }
    val dismiss by androidx.compose.runtime.rememberUpdatedState(onDismissRequest)
    val guardedDismiss: () -> Unit = remember {
        {
            val age = android.os.SystemClock.uptimeMillis() - openedAt
            if (age < DIALOG_SETTLE_MS) {
                android.util.Log.w("EigentoneDialog", "Ignored a request to close a dialog $age ms after it opened")
            } else {
                dismiss()
            }
            Unit
        }
    }
    androidx.compose.material3.AlertDialog(
        onDismissRequest = guardedDismiss,
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

/** How soon after opening a dialog ignores being asked to close, and how long one takes to close. */
const val DIALOG_SETTLE_MS = 400L

/** A little longer than a dialog takes to animate away, so the next one opens after it rather than over it. */
const val DIALOG_GAP_MS = 200L

/** Gives the dialog window this is drawn in the app's opening and closing animation. */
@Composable
fun DialogMotionEffect() {
    val view = androidx.compose.ui.platform.LocalView.current
    // Set once, since each change makes Android lay the window out again.
    androidx.compose.runtime.DisposableEffect(view) {
        val provider = view as? androidx.compose.ui.window.DialogWindowProvider
            ?: view.parent as? androidx.compose.ui.window.DialogWindowProvider
        provider?.window?.setWindowAnimations(io.github.dragjon.eigentone.R.style.DialogMotion)
        onDispose { }
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

/** The tallest a list menu grows before it scrolls, short enough to stay clear of the navigation bar. */
internal const val MENU_MAX_DP = 480

/**
 * Text that wraps only between words. Where a word won't fit its line, as on a narrow phone or with large
 * text, the type steps down a little until it does, rather than splitting the word (tester feedback:
 * "Parametri / c" on a 720p screen).
 */
@Composable
fun WholeWordText(
    text: String,
    style: androidx.compose.ui.text.TextStyle,
    color: androidx.compose.ui.graphics.Color,
    modifier: Modifier = Modifier,
    textAlign: TextAlign? = null,
    maxLines: Int = 1,
) {
    var scale by androidx.compose.runtime.remember(text) { androidx.compose.runtime.mutableFloatStateOf(1f) }
    Text(
        text,
        style = style.copy(fontSize = style.fontSize * scale, lineHeight = style.lineHeight * scale),
        color = color,
        textAlign = textAlign,
        maxLines = maxLines,
        modifier = modifier,
        onTextLayout = { layout ->
            val splitsWord = (0 until layout.lineCount - 1).any { line ->
                val end = layout.getLineEnd(line)
                end in 1 until text.length && !text[end - 1].isWhitespace() && !text[end].isWhitespace()
            }
            if ((splitsWord || layout.hasVisualOverflow) && scale > MIN_TEXT_SCALE) scale = (scale - 0.06f).coerceAtLeast(MIN_TEXT_SCALE)
        },
    )
}

private const val MIN_TEXT_SCALE = 0.7f
