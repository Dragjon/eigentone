package io.github.dragjon.eigentone.ui

import androidx.activity.compose.BackHandler
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutLinearInEasing
import androidx.compose.animation.core.LinearOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.PointerEventType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.Layout
import androidx.compose.ui.layout.boundsInRoot
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.unit.dp

private const val MENU_IN_MS = 180
private const val MENU_OUT_MS = 150
private const val MENU_EDGE_DP = 8
private const val MENU_SHADOW_ROOM_DP = 4

/** One menu, kept built while its control is on screen so that opening it only has to show it. */
private class MenuEntry {
    var expanded by mutableStateOf(false)
    var content by mutableStateOf<@Composable ColumnScope.() -> Unit>({})
    var dismiss: () -> Unit = {}
    val open = Animatable(0f)

    // Read while placing and drawing, not composing, so they're plain values.
    var anchor = Rect.Zero
    var bounds = Rect.Zero
    var below = true
}

private class MenuLayer {
    val entries = mutableStateListOf<MenuEntry>()
}

private val LocalMenuLayer = staticCompositionLocalOf<MenuLayer?> { null }

/**
 * Draws every [MotionMenu] over the app, inside its own window. A popup window would cost a new window and a
 * change of focus on each open and close, which on a phone shows as a frame or two of stutter.
 */
@Composable
fun MenuHost(content: @Composable () -> Unit) {
    val layer = remember { MenuLayer() }
    Box(
        Modifier
            .fillMaxSize()
            // Seen before anything under it, so a tap outside an open menu closes it and still does what it was for.
            .pointerInput(layer) {
                awaitPointerEventScope {
                    while (true) {
                        val event = awaitPointerEvent(PointerEventPass.Initial)
                        if (event.type != PointerEventType.Press) continue
                        val at = event.changes.first().position
                        layer.entries.filter { it.expanded && !it.bounds.contains(at) }.forEach { menu ->
                            menu.dismiss()
                            // A tap on the control that opened it only closes it, rather than opening it again.
                            if (menu.anchor.contains(at)) event.changes.forEach { it.consume() }
                        }
                    }
                }
            },
    ) {
        CompositionLocalProvider(LocalMenuLayer provides layer) { content() }
        layer.entries.forEach { menu -> key(menu) { MenuSurface(menu) } }
    }
}

@Composable
private fun BoxScope.MenuSurface(menu: MenuEntry) {
    Layout(
        content = {
            Column(
                Modifier
                    .graphicsLayer {
                        val p = menu.open.value
                        // Grows out of the control's corner, mostly towards the free space, and fades as it goes.
                        transformOrigin = TransformOrigin(0f, if (menu.below) 0f else 1f)
                        scaleX = 0.92f + 0.08f * p
                        scaleY = 0.8f + 0.2f * p
                        alpha = (p * 1.6f).coerceIn(0f, 1f)
                    }
                    .onGloballyPositioned { menu.bounds = it.boundsInRoot() }
                    // Touches on the menu stay on the menu, even between its items.
                    .pointerInput(Unit) { awaitPointerEventScope { while (true) awaitPointerEvent() } }
                    .padding(MENU_SHADOW_ROOM_DP.dp)
                    .shadow(8.dp, PanelShape)
                    .clip(PanelShape)
                    .background(Palette.Tooltip)
                    .heightIn(max = MENU_MAX_DP.dp)
                    .widthIn(min = 112.dp, max = 280.dp)
                    .width(IntrinsicSize.Max)
                    .verticalScroll(rememberScrollState())
                    .padding(vertical = 8.dp),
                content = menu.content,
            )
        },
        modifier = Modifier.matchParentSize(),
    ) { measurables, constraints ->
        val menuSize = measurables.first().measure(constraints.copy(minWidth = 0, minHeight = 0))
        layout(constraints.maxWidth, constraints.maxHeight) {
            // Closed menus stay built but aren't placed, so they draw nothing, can't be touched and aren't read out.
            if (!menu.expanded && menu.open.value == 0f) return@layout
            val margin = MENU_EDGE_DP.dp.roundToPx()
            val room = MENU_SHADOW_ROOM_DP.dp.roundToPx()
            val a = menu.anchor
            val w = menuSize.width
            val h = menuSize.height
            // Under the control where there's room, over it where there isn't, and never off the side of the screen.
            val x = (a.left.toInt() - room).coerceIn(0, (constraints.maxWidth - w).coerceAtLeast(0))
            val fitsBelow = a.bottom + h <= constraints.maxHeight - margin
            val fitsAbove = a.top - h >= margin
            menu.below = fitsBelow || !fitsAbove
            val y = if (menu.below) a.bottom.toInt().coerceAtMost((constraints.maxHeight - h - margin).coerceAtLeast(0))
            else a.top.toInt() - h
            menuSize.place(x, y)
        }
    }
}

/**
 * A list menu under the control that opens it, placed inside that control's Box. It unfolds from the control
 * and folds back into it, and scrolls once it reaches [MENU_MAX_DP]. Needs a [MenuHost] above it.
 */
@Composable
fun MotionMenu(
    expanded: Boolean,
    onDismissRequest: () -> Unit,
    content: @Composable ColumnScope.() -> Unit,
) {
    val layer = checkNotNull(LocalMenuLayer.current) { "MotionMenu needs a MenuHost" }
    val menu = remember { MenuEntry() }
    SideEffect {
        menu.content = content
        menu.dismiss = onDismissRequest
        menu.expanded = expanded
    }
    DisposableEffect(layer) {
        layer.entries.add(menu)
        onDispose { layer.entries.remove(menu) }
    }
    LaunchedEffect(expanded) {
        if (expanded) menu.open.animateTo(1f, tween(MENU_IN_MS, easing = LinearOutSlowInEasing))
        else menu.open.animateTo(0f, tween(MENU_OUT_MS, easing = FastOutLinearInEasing))
    }
    BackHandler(enabled = expanded) { onDismissRequest() }
    // Takes no space; it only learns where the control that holds it is.
    Spacer(Modifier.onGloballyPositioned { it.parentLayoutCoordinates?.let { parent -> menu.anchor = parent.boundsInRoot() } })
}
