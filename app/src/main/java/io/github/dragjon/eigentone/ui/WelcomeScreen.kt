package io.github.dragjon.eigentone.ui

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Rect
import androidx.compose.animation.core.LinearEasing
import kotlinx.coroutines.launch
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.runtime.rememberCoroutineScope
import kotlin.math.sin
import kotlin.math.PI
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.Density
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.Outline
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.animation.core.tween
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.Animatable
import android.Manifest
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.annotation.DrawableRes
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Icon
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
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import io.github.dragjon.eigentone.R
import io.github.dragjon.eigentone.data.AppState
import io.github.dragjon.eigentone.data.BuiltIns
import io.github.dragjon.eigentone.data.Profiles
import io.github.dragjon.eigentone.data.Repo
import io.github.dragjon.eigentone.data.ThemeMode

private const val STEPS = 5

/** Set as the welcome hands over, so the app opens out of the line the welcome closed into. */
internal val openingFromWelcome = mutableStateOf(false)

private const val CLOSE_MS = 420

/**
 * The first run, in five short steps: what Eigentone is, turning it on, the headphones, finding a sound, and
 * how much control to show. Every step can be skipped, the place is saved if the app closes, and Settings,
 * Help, opens it again.
 */
@Composable
fun WelcomeScreen(onSetEnabled: (Boolean) -> Unit, onFindSound: () -> Unit) {
    val state by Repo.state.collectAsStateWithLifecycle()
    val step = state.welcomeStep.coerceIn(0, STEPS - 1)
    fun go(to: Int) = Repo.update { it.copy(welcomeStep = to.coerceIn(0, STEPS - 1)) }
    fun finish() = Repo.update { it.copy(welcomeDone = true, welcomeStep = 0, guidedSetupSeen = true) }

    BackHandler(enabled = step > 0) { go(step - 1) }

    // Start listening closes the page into a single lit line, the one the app then opens out of.
    val closing = remember { Animatable(0f) }
    val scope = rememberCoroutineScope()
    val haptics = LocalHapticFeedback.current
    fun startListening() {
        if (closing.value > 0f) return
        scope.launch {
            closing.animateTo(1f, tween(CLOSE_MS, easing = FastOutSlowInEasing))
            haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove)
            openingFromWelcome.value = true
            finish()
        }
    }

    // Changing step sweeps the page away fader by fader to show the next: left to right going forward, right to
    // left going back, so the direction always matches the way through.
    var shown by remember { mutableIntStateOf(step) }
    var tearing by remember { mutableStateOf<Int?>(null) }
    var backward by remember { mutableStateOf(false) }
    val tear = remember { Animatable(0f) }
    LaunchedEffect(step) {
        if (step == shown) return@LaunchedEffect
        backward = step < shown
        tearing = shown
        shown = step
        try {
            tear.snapTo(0f)
            tear.animateTo(1f, tween(SWEEP_MS, easing = LinearEasing))
        } finally {
            tearing = null
        }
    }

    @Composable
    fun Page(page: Int, modifier: Modifier) {
        Box(modifier.verticalScroll(rememberScrollState()), contentAlignment = Alignment.TopCenter) {
            Column(
                Modifier
                    .widthIn(max = 560.dp)
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp, vertical = 8.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp),
            ) {
                when (page) {
                    0 -> Hello()
                    1 -> TurnOn(state, onSetEnabled)
                    2 -> YourHeadphones(state)
                    3 -> FindYourSound {
                        // Comes back to the last step once the test is done or left.
                        go(4)
                        onFindSound()
                    }
                    else -> HandsOn(state)
                }
            }
        }
    }

    Column(
        Modifier
            .fillMaxSize()
            .drawWithContent {
                val c = closing.value
                drawRect(Palette.Background)
                drawContent()
                if (c > 0f) {
                    // The line grows out from the middle as the page flattens onto it.
                    val half = size.width * (0.15f + 0.85f * c) / 2f
                    val y = size.height / 2f
                    drawLine(
                        Palette.Lit.copy(alpha = c),
                        Offset(size.width / 2f - half, y),
                        Offset(size.width / 2f + half, y),
                        strokeWidth = 3.dp.toPx(),
                        cap = StrokeCap.Round,
                    )
                }
            }
            .graphicsLayer {
                val c = closing.value
                scaleY = 1f - 0.994f * c
                scaleX = 1f - 0.06f * c
                alpha = 1f - ((c - 0.6f) / 0.4f).coerceIn(0f, 1f)
            }
            .background(Palette.Background)
            .windowInsetsPadding(WindowInsets.safeDrawing),
    ) {
        Row(
            Modifier
                .fillMaxWidth()
                .heightIn(min = 56.dp)
                .padding(start = 24.dp, end = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            StepDots(step, Modifier.weight(1f))
            if (step < STEPS - 1) TextButton(onClick = ::finish) { Text(stringResource(R.string.welcome_skip)) }
        }
        Box(
            Modifier
                .weight(1f)
                .fillMaxWidth(),
        ) {
            Page(shown, Modifier.matchParentSize())
            tearing?.let { from ->
                for (strip in 0 until STRIPS) {
                    FaderStrip(if (backward) STRIPS - 1 - strip else strip, strip, tear.value, Modifier.matchParentSize()) { Page(from, Modifier.fillMaxSize()) }
                }
                SweepLine(tear.value, backward, Modifier.matchParentSize())
            }
        }
        Row(
            Modifier
                .fillMaxWidth()
                .background(Palette.Bar)
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            if (step > 0) TextButton(onClick = { go(step - 1) }) { Text(stringResource(R.string.ui_back)) }
            Spacer(Modifier.weight(1f))
            Button(
                onClick = { if (step == STEPS - 1) startListening() else go(step + 1) },
                modifier = Modifier.heightIn(min = 48.dp),
            ) {
                Text(
                    stringResource(
                        when (step) {
                            0 -> R.string.welcome_get_started
                            STEPS - 1 -> R.string.welcome_finish
                            else -> R.string.welcome_continue
                        },
                    ),
                )
            }
        }
    }
}

private const val SWEEP_MS = 700
private const val STRIPS = 10

/** How much later each strip starts than the one to its left, as a share of the sweep. */
private const val STRIP_STAGGER = 0.06f

/** How far through its own fall a strip is, given the whole sweep's progress. */
private fun stripProgress(strip: Int, progress: Float): Float =
    ((progress - strip * STRIP_STAGGER) / (1f - (STRIPS - 1) * STRIP_STAGGER)).coerceIn(0f, 1f)

/** One fader-wide strip of the page, from top to bottom, reaching past both ends so nothing shows between. */
private class StripShape(private val strip: Int) : Shape {
    override fun createOutline(size: Size, layoutDirection: LayoutDirection, density: Density): Outline {
        val width = size.width / STRIPS
        // A hair wider than its share, so neighbours overlap and no seam shows before they move.
        return Outline.Rectangle(Rect(width * strip, -size.height, width * (strip + 1) + 1f, size.height * 2))
    }
}

/**
 * A strip of the page just finished. Like a fader pulled down, it drops and fades, a moment after the one to
 * its left, so the page leaves as a sweep from low to high.
 */
@Composable
private fun FaderStrip(order: Int, strip: Int, progress: Float, modifier: Modifier, content: @Composable () -> Unit) {
    val shape = remember(strip) { StripShape(strip) }
    Box(
        modifier
            .graphicsLayer {
                val local = stripProgress(order, progress)
                translationY = local * local * size.height * 0.45f
                alpha = 1f - local
                this.shape = shape
                clip = true
            }
            .background(Palette.Background),
    ) { content() }
}

/** The lit line leading the sweep, like the cursor of a frequency sweep. */
@Composable
private fun SweepLine(progress: Float, backward: Boolean, modifier: Modifier) {
    Box(
        modifier.drawBehind {
            val along = (progress / (1f - (STRIPS - 1) * STRIP_STAGGER * 0.5f)).coerceIn(0f, 1f)
            val x = size.width * if (backward) 1f - along else along
            val fade = sin(PI * progress).toFloat()
            drawRect(Palette.Lit.copy(alpha = 0.12f * fade), topLeft = Offset(x - 8.dp.toPx(), 0f), size = Size(16.dp.toPx(), size.height))
            drawLine(Palette.Lit.copy(alpha = fade), Offset(x, 0f), Offset(x, size.height), strokeWidth = 2.dp.toPx())
        },
    )
}

/** Where the welcome is, as dots: lit up to the current step. */
@Composable
private fun StepDots(step: Int, modifier: Modifier) {
    val label = stringResource(R.string.welcome_step_of, step + 1, STEPS)
    Row(modifier.semantics { contentDescription = label }, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        repeat(STEPS) { i ->
            Box(
                Modifier
                    .size(width = if (i == step) 24.dp else 8.dp, height = 8.dp)
                    .clip(CircleShape)
                    .background(if (i <= step) Palette.Lit else Palette.Track),
            )
        }
    }
}

@Composable
private fun Heading(title: String, body: String) {
    Text(title, style = MaterialTheme.typography.headlineSmall, color = Palette.Text)
    Text(body, style = MaterialTheme.typography.bodyLarge, color = Palette.TextDim)
}

/** A tip or result set apart from the step's explanation. */
@Composable
private fun Note(text: String, lit: Boolean = false) {
    Text(
        text,
        style = MaterialTheme.typography.bodyMedium,
        color = if (lit) Palette.Lit else Palette.Text,
        modifier = Modifier
            .fillMaxWidth()
            .clip(PanelShape)
            .background(Palette.Surface)
            .padding(16.dp),
    )
}

@Composable
private fun Hello() {
    // The mark in full colour: a double helix untwisting into one sound wave.
    androidx.compose.foundation.Image(
        painterResource(R.drawable.ic_logo),
        contentDescription = null,
        modifier = Modifier
            .padding(top = 16.dp)
            .size(88.dp)
            .clip(RoundedCornerShape(20.dp))
            // The icon's own black, in either theme, so it looks the same as on the home screen.
            .background(androidx.compose.ui.graphics.Color(0xFF0D0C09))
            // A white rim, so the black tile stands out from the dark page as it does on a wallpaper.
            .border(2.dp, androidx.compose.ui.graphics.Color.White, RoundedCornerShape(20.dp))
            .padding(4.dp),
    )
    Heading(stringResource(R.string.welcome_hello_title), stringResource(R.string.welcome_hello_body))
    Feature(R.drawable.ic_tab_eq, R.string.welcome_feature_eq)
    Feature(R.drawable.ic_tab_headphones, R.string.welcome_feature_headphones)
    Feature(R.drawable.ic_tab_presets, R.string.welcome_feature_presets)
    Text(stringResource(R.string.welcome_hello_note), style = MaterialTheme.typography.bodySmall, color = Palette.TextDim)
}

@Composable
private fun Feature(@DrawableRes icon: Int, text: Int) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Box(
            Modifier
                .size(40.dp)
                .clip(KeyShape)
                .background(Palette.SurfaceHigh),
            contentAlignment = Alignment.Center,
        ) {
            Icon(painterResource(icon), contentDescription = null, tint = Palette.Lit)
        }
        Text(stringResource(text), style = MaterialTheme.typography.bodyLarge, color = Palette.Text, modifier = Modifier.padding(start = 16.dp))
    }
}

@Composable
private fun TurnOn(state: AppState, onSetEnabled: (Boolean) -> Unit) {
    val status by Repo.status.collectAsStateWithLifecycle()
    // The notification is optional, so the EQ starts whatever the answer.
    val permission = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { onSetEnabled(true) }
    Heading(stringResource(R.string.welcome_on_title), stringResource(R.string.welcome_on_body))
    if (!state.enabled) {
        Button(
            onClick = {
                // Android 12 and older show the notification without asking.
                if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.TIRAMISU) {
                    permission.launch(Manifest.permission.POST_NOTIFICATIONS)
                } else {
                    onSetEnabled(true)
                }
            },
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = 52.dp),
        ) { Text(stringResource(R.string.welcome_turn_on)) }
    } else {
        Note(
            when {
                status.error != null -> status.error.orEmpty()
                !status.running -> stringResource(R.string.ui_starting)
                !status.hasControl -> stringResource(R.string.ui_another_audio_app_has_taken_control)
                else -> stringResource(R.string.welcome_on_done)
            },
            lit = status.running && status.hasControl && status.error == null,
        )
    }
    BackgroundUse()
    Text(stringResource(R.string.ui_use_one_eq_at_a_time), style = MaterialTheme.typography.titleSmall, color = Palette.Text)
    Text(stringResource(R.string.ui_eq_in_a_headphone_app_such_2), style = MaterialTheme.typography.bodyMedium, color = Palette.TextDim)
}

/**
 * Asks to be left out of battery optimisation, which otherwise can stop the EQ once the phone is locked. Rechecked
 * on every return, since the system prompt can be declined.
 */
@android.annotation.SuppressLint("BatteryLife")
@Composable
private fun BackgroundUse() {
    val context = androidx.compose.ui.platform.LocalContext.current
    var exempt by remember { mutableStateOf(true) }
    androidx.lifecycle.compose.LifecycleResumeEffect(Unit) {
        exempt = context.getSystemService(android.os.PowerManager::class.java).isIgnoringBatteryOptimizations(context.packageName)
        onPauseOrDispose { }
    }
    Text(stringResource(R.string.welcome_background_title), style = MaterialTheme.typography.titleSmall, color = Palette.Text)
    if (exempt) {
        Note(stringResource(R.string.welcome_background_done), lit = true)
    } else {
        Text(stringResource(R.string.welcome_background_body), style = MaterialTheme.typography.bodyMedium, color = Palette.TextDim)
        FilledTonalButton(
            onClick = {
                context.startActivity(
                    android.content.Intent(
                        android.provider.Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS,
                        android.net.Uri.parse("package:${context.packageName}"),
                    ),
                )
            },
            modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp),
        ) { Text(stringResource(R.string.ui_allow_background_use)) }
    }
}

@Composable
private fun YourHeadphones(state: AppState) {
    val output by Repo.output.collectAsStateWithLifecycle()
    // Read directly, because the service's output isn't known while Eigentone is off.
    val builtIn = rememberConnectedOutputNames().firstNotNullOfOrNull(BuiltIns::correctionFor)
    var finding by remember { mutableStateOf(false) }
    var tuning by remember { mutableStateOf(false) }
    val corrected = state.current.filters.any { it.gainDb != 0.0 } || state.current.tone.importedCurve.isNotEmpty()

    Heading(stringResource(R.string.welcome_headphones_title), stringResource(R.string.welcome_headphones_body))
    if (!Profiles.supportsModes(output.key) && builtIn == null) {
        Note(stringResource(R.string.welcome_headphones_none, output.label))
        return
    }
    Text(output.label, style = MaterialTheme.typography.titleMedium, color = Palette.Text)
    if (corrected) {
        Note(stringResource(R.string.welcome_headphones_using, state.current.name), lit = true)
        FilledTonalButton(onClick = { tuning = true }, modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp)) {
            Text(stringResource(R.string.welcome_headphones_tune))
        }
        Text(stringResource(R.string.welcome_headphones_tune_note), style = MaterialTheme.typography.bodySmall, color = Palette.TextDim)
    } else if (builtIn != null) {
        Button(
            onClick = {
                Repo.assignToOutput(output.key, builtIn.id)
                Repo.loadPreset(builtIn)
            },
            modifier = Modifier.fillMaxWidth().heightIn(min = 52.dp),
        ) { Text(stringResource(R.string.ui_use_correction, builtIn.name)) }
    } else {
        Button(onClick = { finding = true }, modifier = Modifier.fillMaxWidth().heightIn(min = 52.dp)) {
            Text(stringResource(R.string.welcome_headphones_find))
        }
    }
    Text(stringResource(R.string.welcome_headphones_later), style = MaterialTheme.typography.bodySmall, color = Palette.TextDim)

    if (finding) AutoEqDialog(onDismiss = { finding = false }) { finding = false }
    if (tuning) CorrectionTuner(onDismiss = { tuning = false })
}

@Composable
private fun FindYourSound(onStart: () -> Unit) {
    Heading(stringResource(R.string.welcome_sound_title), stringResource(R.string.ui_compare_two_versions_of_a_song_a_few_tim))
    Note(stringResource(R.string.welcome_sound_note))
    Button(onClick = onStart, modifier = Modifier.fillMaxWidth().heightIn(min = 52.dp)) { Text(stringResource(R.string.welcome_sound_start)) }
    Text(stringResource(R.string.welcome_sound_later), style = MaterialTheme.typography.bodySmall, color = Palette.TextDim)
}

@Composable
private fun HandsOn(state: AppState) {
    Heading(stringResource(R.string.welcome_hands_title), stringResource(R.string.welcome_hands_body))
    Column(Modifier.selectableGroup(), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Choice(
            title = stringResource(R.string.welcome_simple),
            body = stringResource(R.string.welcome_simple_body),
            selected = !state.advancedControls,
        ) { Repo.update { it.copy(advancedControls = false) } }
        Choice(
            title = stringResource(R.string.welcome_full),
            body = stringResource(R.string.welcome_full_body),
            selected = state.advancedControls,
        ) { Repo.update { it.copy(advancedControls = true) } }
    }
    Text(stringResource(R.string.welcome_hands_where), style = MaterialTheme.typography.bodySmall, color = Palette.TextDim)
    Text(stringResource(R.string.ui_theme), style = MaterialTheme.typography.titleSmall, color = Palette.Text, modifier = Modifier.padding(top = 8.dp))
    val modes = ThemeMode.entries
    WordSegments(
        labels = listOf(stringResource(R.string.ui_theme_device), stringResource(R.string.ui_theme_light), stringResource(R.string.ui_theme_dark)),
        selected = modes.indexOf(state.themeMode),
        onSelect = { i -> Repo.update { it.copy(themeMode = modes[i]) } },
    )
    Note(stringResource(R.string.welcome_hands_note))
}

/** A large, plainly selectable card for a choice that sets up the whole app. */
@Composable
private fun Choice(title: String, body: String, selected: Boolean, onSelect: () -> Unit) {
    Column(
        Modifier
            .fillMaxWidth()
            .clip(PanelShape)
            .background(if (selected) Palette.SurfaceHigh else Palette.Surface)
            .border(if (selected) 2.dp else 1.dp, if (selected) Palette.Lit else Palette.Line, PanelShape)
            .selectable(selected = selected, role = Role.RadioButton, onClick = onSelect)
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        Text(title, style = MaterialTheme.typography.titleMedium, color = if (selected) Palette.Lit else Palette.Text)
        Text(body, style = MaterialTheme.typography.bodyMedium, color = Palette.TextDim)
    }
}
