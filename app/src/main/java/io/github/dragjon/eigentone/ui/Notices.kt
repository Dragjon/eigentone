package io.github.dragjon.eigentone.ui

import androidx.compose.material.icons.filled.Close

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.snap
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Snackbar
import androidx.compose.material3.SnackbarData
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarResult
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalAccessibilityManager
import androidx.compose.ui.unit.dp
import io.github.dragjon.eigentone.R
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/**
 * Shows one notice at a time, just above the save bars. A new notice takes the old one's place at once
 * rather than waiting behind it or fading in over it, so two are never seen stacked.
 */
@Composable
fun NoticeHost(state: SnackbarHostState, modifier: Modifier = Modifier, longer: Boolean = false) {
    val data = state.currentSnackbarData
    val accessibility = LocalAccessibilityManager.current
    if (data != null) {
        LaunchedEffect(data) {
            val base = when (data.visuals.duration) {
                SnackbarDuration.Short -> NOTICE_SHORT_MS
                SnackbarDuration.Long -> NOTICE_LONG_MS
                SnackbarDuration.Indefinite -> return@LaunchedEffect
            } * if (longer) 2 else 1
            // Longer for people who've asked Android for more time to read and act.
            val shown = accessibility?.calculateRecommendedTimeoutMillis(
                base,
                containsIcons = true,
                containsText = true,
                containsControls = data.visuals.actionLabel != null,
            ) ?: base
            delay(shown)
            data.dismiss()
        }
    }
    AnimatedContent(
        targetState = data,
        transitionSpec = {
            // A notice replaced by another goes at once; only the last one fades away.
            val out = if (targetState != null) fadeOut(snap()) else fadeOut(tween(NOTICE_FADE_MS))
            (fadeIn(tween(NOTICE_FADE_MS)) + slideInVertically(tween(NOTICE_FADE_MS, easing = FastOutSlowInEasing)) { it / 3 })
                .togetherWith(out)
                // Settles to its new size at once and grows from the bottom, so a message rises into place
                // rather than first popping up from the top of a growing box and then sinking.
                .using(androidx.compose.animation.SizeTransform(clip = false) { _, _ -> snap() })
        },
        contentAlignment = androidx.compose.ui.Alignment.BottomCenter,
        modifier = modifier.fillMaxWidth(),
        label = "notice",
    ) { shown: SnackbarData? ->
        // Material's ready-made snackbar adds its own 12 dp margin, which left messages narrower than the save bars and
        // higher above them. Built from its plain layout instead, a message lines up with the save bars exactly.
        if (shown != null) Box {
            Snackbar(
                shape = PanelShape,
                containerColor = Palette.Note,
                contentColor = Palette.NoteText,
                actionContentColor = Palette.LitBright,
                dismissActionContentColor = Palette.TextDim,
                action = shown.visuals.actionLabel?.let { label ->
                    {
                        androidx.compose.material3.TextButton(
                            onClick = { shown.performAction() },
                            colors = androidx.compose.material3.ButtonDefaults.textButtonColors(contentColor = Palette.LitBright),
                        ) { androidx.compose.material3.Text(label) }
                    }
                },
                dismissAction = if (shown.visuals.withDismissAction) {
                    {
                        androidx.compose.material3.IconButton(onClick = { shown.dismiss() }) {
                            androidx.compose.material3.Icon(
                                androidx.compose.material.icons.Icons.Filled.Close,
                                contentDescription = androidx.compose.ui.res.stringResource(R.string.ui_dismiss),
                            )
                        }
                    }
                } else {
                    null
                },
            ) { androidx.compose.material3.Text(shown.visuals.message) }
        }
    }
}

/** Shows a message in place of any notice still showing. */
fun CoroutineScope.notify(snackbar: SnackbarHostState, message: String) {
    launch {
        snackbar.currentSnackbarData?.dismiss()
        snackbar.showSnackbar(message, withDismissAction = true, duration = SnackbarDuration.Short)
    }
}

/**
 * Says something was done and offers to take it back. Undoing says so and offers Redo, and redoing offers
 * Undo again, so a change can be flipped back and forth while the notice is up.
 */
fun CoroutineScope.offerUndo(
    snackbar: SnackbarHostState,
    message: String,
    undo: () -> Unit,
    redo: () -> Unit,
) {
    launch {
        snackbar.currentSnackbarData?.dismiss()
        var done = true
        while (true) {
            val result = snackbar.showSnackbar(
                if (done) message else appText(R.string.ui_notice_undone, message),
                actionLabel = appText(if (done) R.string.ui_undo else R.string.ui_redo),
                withDismissAction = true,
                duration = SnackbarDuration.Long,
            )
            if (result != SnackbarResult.ActionPerformed) break
            if (done) undo() else redo()
            done = !done
        }
    }
}

private const val NOTICE_SHORT_MS = 4000L
private const val NOTICE_LONG_MS = 7000L
private const val NOTICE_FADE_MS = 180
