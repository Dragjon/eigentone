package io.github.dragjon.eigentone.ui

import androidx.compose.ui.platform.LocalDensity
import androidx.compose.foundation.gestures.animateScrollBy
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.LinearOutSlowInEasing
import androidx.compose.animation.core.Animatable
import androidx.compose.foundation.indication
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.ui.graphics.Color
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.selection.selectable
import androidx.compose.ui.res.stringResource
import android.Manifest
import android.annotation.SuppressLint
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.PowerManager
import android.provider.OpenableColumns
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import androidx.compose.foundation.ScrollState
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.KeyboardArrowLeft
import androidx.compose.material.icons.filled.KeyboardArrowRight
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarResult
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.TimePicker
import androidx.compose.material3.rememberTimePickerState
import io.github.dragjon.eigentone.data.NightSchedule
import androidx.compose.ui.graphics.graphicsLayer
import io.github.dragjon.eigentone.data.ClipRisk
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Search
import io.github.dragjon.eigentone.data.PresetOrganiser
import io.github.dragjon.eigentone.engine.PresetShortcuts
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.animation.core.animateFloat
import androidx.compose.ui.layout.layout
import kotlinx.coroutines.flow.first
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.ui.text.drawText
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.foundation.layout.height
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LifecycleResumeEffect
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import io.github.dragjon.eigentone.R
import io.github.dragjon.eigentone.data.AppState
import io.github.dragjon.eigentone.data.Disclosure
import io.github.dragjon.eigentone.data.AutoEqCatalog
import io.github.dragjon.eigentone.data.BackupCodec
import io.github.dragjon.eigentone.data.BuiltIns
import io.github.dragjon.eigentone.data.CatalogEntry
import io.github.dragjon.eigentone.data.EngineMode
import io.github.dragjon.eigentone.data.EngineStatus
import io.github.dragjon.eigentone.data.ListeningMode
import io.github.dragjon.eigentone.data.Preset
import io.github.dragjon.eigentone.data.Processing
import io.github.dragjon.eigentone.data.Profiles
import io.github.dragjon.eigentone.data.Repo
import io.github.dragjon.eigentone.data.ReverbRoom
import io.github.dragjon.eigentone.data.EqualLoudness
import io.github.dragjon.eigentone.dsp.GraphicEq
import io.github.dragjon.eigentone.data.LimiterSettings
import io.github.dragjon.eigentone.data.ShapeText
import io.github.dragjon.eigentone.data.ThemeMode
import io.github.dragjon.eigentone.data.Tone
import io.github.dragjon.eigentone.dsp.AutoEqParser
import io.github.dragjon.eigentone.dsp.Eq
import io.github.dragjon.eigentone.dsp.Filter
import io.github.dragjon.eigentone.dsp.FilterType
import io.github.dragjon.eigentone.engine.SpectrumAnalyser
import io.github.dragjon.eigentone.engine.readAtMost
import androidx.compose.ui.semantics.clearAndSetSemantics
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.Locale
import io.github.dragjon.eigentone.data.PresetChanges
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.draw.alpha
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.requiredWidth
import androidx.compose.foundation.layout.fillMaxHeight
import io.github.dragjon.eigentone.data.OutputKind
import io.github.dragjon.eigentone.data.OutputLevel
import androidx.compose.animation.togetherWith
import kotlin.math.log10
import kotlin.math.roundToInt
import androidx.compose.runtime.mutableIntStateOf
import kotlin.math.pow

private enum class Tab(@StringRes val label: Int, @DrawableRes val icon: Int) {
    EQ(R.string.ui_tab_eq, R.drawable.ic_tab_eq),
    SOUND(R.string.ui_tab_sound, R.drawable.ic_tab_sound),
    PRESETS(R.string.ui_tab_presets, R.drawable.ic_tab_presets),
    HEADPHONES(R.string.ui_tab_headphones, R.drawable.ic_tab_headphones),
    SYSTEM(R.string.ui_tab_system, R.drawable.ic_tab_system),
}

@Composable
private fun listeningModeLabel(mode: ListeningMode) = stringResource(
    when (mode) {
        ListeningMode.NOISE_CANCELLING -> R.string.ui_mode_cancelling
        ListeningMode.TRANSPARENCY -> R.string.ui_mode_transparency
        ListeningMode.NORMAL -> R.string.ui_mode_off
    },
)

@Composable
private fun engineModeLabel(mode: EngineMode) = stringResource(
    when (mode) {
        EngineMode.AUTO -> R.string.ui_route_auto
        EngineMode.GLOBAL -> R.string.ui_route_global
        EngineMode.SESSIONS -> R.string.ui_route_per_app
    },
)

private const val MAX_FILTERS = 20
private const val MAX_IMPORT_BYTES = 256 * 1024
private const val MAX_BACKUP_BYTES = 1024 * 1024

/** Brief feedback for actions, shown by the screen's snackbar. */
private val LocalSnackbar = compositionLocalOf<SnackbarHostState> { error("No snackbar host") }

/** Messages run on the app's scope, so one shown by a bar that then goes away, such as Undo after Discard, stays. */
private val LocalSnackScope = compositionLocalOf<CoroutineScope> { error("No snackbar scope") }

/** Opens the guided listening test from anywhere in the main screens. */
private val LocalOpenFinder = compositionLocalOf<() -> Unit> { error("No finder") }

private val LocalOpenHearing = compositionLocalOf<() -> Unit> { error("No hearing check") }

private val LocalOpenRefine = compositionLocalOf<() -> Unit> { error("No refine") }

private const val SEARCH_FROM_PRESETS = 6

/** Just after the theme toggle's thumb has finished sliding, so recolouring the app never stalls it midway. */
private const val THEME_SWITCH_AFTER_MS = 260L
private const val TAB_SLIDE_MS = 240

/** How long after the app appears the tabs not yet visited are built, so the opening isn't slowed by them. */
private const val TAB_PREBUILD_DELAY_MS = 400L
private const val OPEN_MS = 1500
/** When, as a share of the opening, the app starts to fade in behind the line. */
private const val OPEN_UNFOLD_AT = 0.52f
private const val OPEN_BARS = 41

/** How far through the window from [start] to [end] the progress is, from 0 to 1. */
private fun phase(progress: Float, start: Float, end: Float) = ((progress - start) / (end - start)).coerceIn(0f, 1f)

/**
 * The power-on drawn over the app as it opens: the lit line, two pulses ringing out from its middle, and a
 * spectrum that bursts out of the line from the centre outward, moves like live sound, then settles back
 * into the line, edges first, as the app fades in.
 */
private fun androidx.compose.ui.graphics.drawscope.DrawScope.drawPowerOn(p: Float) {
    val centre = androidx.compose.ui.geometry.Offset(size.width / 2f, size.height / 2f)
    val reach = kotlin.math.hypot(size.width, size.height) / 2f
    // Pulses.
    for (k in 0..1) {
        val r = phase(p, 0.02f + k * 0.13f, 0.6f + k * 0.13f)
        if (r > 0f && r < 1f) {
            drawCircle(
                Palette.Lit.copy(alpha = (1f - r) * 0.55f),
                radius = r * reach * 1.1f,
                center = centre,
                style = androidx.compose.ui.graphics.drawscope.Stroke(width = (1f + 3f * (1f - r)) * density),
            )
        }
    }
    // The line, brightest at the start and gone once the app is open.
    val lineAlpha = 1f - phase(p, OPEN_UNFOLD_AT, OPEN_UNFOLD_AT + 0.25f)
    drawLine(
        Palette.Lit.copy(alpha = lineAlpha),
        androidx.compose.ui.geometry.Offset(0f, centre.y),
        androidx.compose.ui.geometry.Offset(size.width, centre.y),
        strokeWidth = 3f * density,
    )
    // The spectrum.
    val rise = phase(p, 0.06f, 0.42f)
    val settle = phase(p, OPEN_UNFOLD_AT - 0.06f, 0.9f)
    val slot = size.width / OPEN_BARS
    val barWidth = slot * 0.55f
    val tallest = size.height * 0.26f
    for (i in 0 until OPEN_BARS) {
        // 0 in the middle, 1 at the edges.
        val distance = kotlin.math.abs(i - (OPEN_BARS - 1) / 2f) / ((OPEN_BARS - 1) / 2f)
        val up = ((rise * 1.5f - distance * 0.5f)).coerceIn(0f, 1f)
        val down = ((settle * 1.5f - (1f - distance) * 0.5f)).coerceIn(0f, 1f)
        val upEased = 1f - (1f - up) * (1f - up)
        // Moves like music: two waves at different speeds, louder in the middle, as bass and mids are.
        val motion = 0.55f + 0.45f * kotlin.math.sin(i * 0.9f + p * 22f) * kotlin.math.sin(i * 0.31f - p * 9f)
        val height = tallest * upEased * (1f - down) * motion * (1f - 0.55f * distance)
        if (height < 0.5f) continue
        val x = slot * i + (slot - barWidth) / 2f
        drawRoundRect(
            Palette.Lit.copy(alpha = 0.9f * (1f - down * 0.5f)),
            topLeft = androidx.compose.ui.geometry.Offset(x, centre.y - height),
            size = androidx.compose.ui.geometry.Size(barWidth, height * 2f),
            cornerRadius = androidx.compose.ui.geometry.CornerRadius(barWidth / 2f),
        )
    }
}
private const val UNSAVED_BAR_ROOM_DP = 80
private const val NAV_EDGE_DP = 8

private val LocalOpenPresets = compositionLocalOf<() -> Unit> { error("No presets tab") }

private val LocalOpenPeaks = compositionLocalOf<() -> Unit> { error("No peak finder") }
private val LocalOpenCurves = compositionLocalOf<() -> Unit> { error("No curve comparison") }

private val LocalOpenConsole = compositionLocalOf<() -> Unit> { error("No debug console") }

private const val SOURCE_URL = "https://github.com/Dragjon/eigentone"

private val LocalOpenSweep = compositionLocalOf<() -> Unit> { error("No sweep") }

private val LocalOpenCompare = compositionLocalOf<() -> Unit> { error("No comparison") }

/** Opens the faders across the whole screen. */
private val LocalOpenFaders = compositionLocalOf<() -> Unit> { error("No expanded faders") }

/** Opens the shape library from the graphic EQ. */
private val LocalOpenShapes =compositionLocalOf<() -> Unit> { error("No shape library") }

@Composable
fun EigentoneApp(onSetEnabled: (Boolean) -> Unit) {
    val state by Repo.state.collectAsStateWithLifecycle()
    val status by Repo.status.collectAsStateWithLifecycle()
    var tab by rememberSaveable { mutableStateOf(Tab.EQ) }
    // Settings opens from the top bar, so Back returns to whichever tab it was opened from.
    var beforeSettings by rememberSaveable { mutableStateOf(Tab.EQ) }
    // Each tab has its own position. Moving to one starts it at the top; Back from Settings returns to where you were.
    val scrolls: Map<Tab, ScrollState> = Tab.entries.associateWith { rememberScrollState() }
    val tabScope = rememberCoroutineScope()
    val toggleSettings = {
        if (tab == Tab.SYSTEM) {
            tab = beforeSettings
        } else {
            beforeSettings = tab
            tabScope.launch { scrolls.getValue(Tab.SYSTEM).scrollTo(0) }
            tab = Tab.SYSTEM
        }
    }
    val snackbar = remember { SnackbarHostState() }
    val appContext = LocalContext.current
    LaunchedEffect(state.favorites, state.presets) {
        val available = BuiltIns.all + state.presets
        withContext(Dispatchers.Default) {
            PresetShortcuts.update(appContext, PresetOrganiser.shortcuts(state, available))
        }
    }
    var finding by rememberSaveable { mutableStateOf(false) }
    var refining by rememberSaveable { mutableStateOf(false) }
    var browsingShapes by rememberSaveable { mutableStateOf(false) }
    var checkingHearing by rememberSaveable { mutableStateOf(false) }
    var comparing by rememberSaveable { mutableStateOf(false) }
    var sweeping by rememberSaveable { mutableStateOf(false) }
    var findingPeaks by rememberSaveable { mutableStateOf(false) }
    var consoleOpen by rememberSaveable { mutableStateOf(false) }
    var expandingFaders by rememberSaveable { mutableStateOf(false) }
    var comparingCurves by rememberSaveable { mutableStateOf(false) }

    // Which full screen is open over the app, if any. Each slides in from the right and back out the way it came.
    val screen = when {
        expandingFaders -> Overlay.FADERS
        findingPeaks -> Overlay.PEAKS
        consoleOpen -> Overlay.CONSOLE
        sweeping -> Overlay.SWEEP
        comparing -> Overlay.COMPARE
        checkingHearing -> Overlay.HEARING
        finding -> Overlay.FINDER
        refining -> Overlay.REFINE
        browsingShapes -> Overlay.SHAPES
        comparingCurves -> Overlay.CURVES
        // After the tools above, so Find your sound opens over the welcome and returns to it.
        !state.welcomeDone -> Overlay.WELCOME
        else -> null
    }
    AnimatedContent(
        targetState = screen,
        // The app's own page behind every change of screen, so a cross-fade never shows the window beneath, which
        // follows the phone's light or dark setting rather than Eigentone's and flashed through (user decision).
        modifier = Modifier.background(Palette.Background),
        transitionSpec = {
            // The landscape faders turn the phone, which a slide would only make look broken. The welcome closes
            // into the line the app then opens out of, so that change needs no transition of its own either.
            if (initialState == Overlay.FADERS || targetState == Overlay.FADERS ||
                (initialState == Overlay.WELCOME && targetState == null)
            ) {
                EnterTransition.None togetherWith ExitTransition.None
            } else if (targetState != null && initialState == null || targetState == Overlay.WELCOME) {
                (slideInHorizontally(screenSlide()) { it } + fadeIn(screenFade())) togetherWith
                    (slideOutHorizontally(screenSlide()) { -it / 4 } + fadeOut(screenFade()))
            } else {
                (slideInHorizontally(screenSlide()) { -it / 4 } + fadeIn(screenFade())) togetherWith
                    (slideOutHorizontally(screenSlide()) { it } + fadeOut(screenFade()))
            }
        },
        label = "screen",
    ) { shown ->
    when (shown) {
        Overlay.FADERS -> ExpandedFaders(onClose = { expandingFaders = false })
        Overlay.PEAKS -> PeakFinderScreen(onClose = { findingPeaks = false })
        Overlay.CONSOLE -> DebugConsoleScreen(onClose = { consoleOpen = false })
        Overlay.SWEEP -> SweepScreen(onClose = { sweeping = false })
        Overlay.COMPARE -> CompareScreen(onSetEnabled = onSetEnabled, onClose = { comparing = false })
        Overlay.HEARING -> HearingScreen(onClose = { checkingHearing = false })
        Overlay.FINDER -> FinderScreen(onSetEnabled = onSetEnabled, onClose = { finding = false })
        Overlay.REFINE -> RefineScreen(onSetEnabled = onSetEnabled, onClose = { refining = false })
        Overlay.SHAPES -> ShapeBrowser(onClose = { browsingShapes = false })
        Overlay.CURVES -> CurveCompareScreen(onClose = { comparingCurves = false })
        Overlay.WELCOME -> WelcomeScreen(onSetEnabled = onSetEnabled, onFindSound = { finding = true })
        null -> AppScreen(state, status, tab, { tab = it }, beforeSettings, { beforeSettings = it }, scrolls, tabScope, toggleSettings, snackbar, onSetEnabled,
            open = OverlayOpeners(
                finder = { finding = true },
                refine = { refining = true },
                shapes = { browsingShapes = true },
                hearing = { checkingHearing = true },
                compare = { comparing = true },
                sweep = { sweeping = true },
                peaks = { findingPeaks = true },
                console = { consoleOpen = true },
                faders = { expandingFaders = true },
                curves = { comparingCurves = true },
            ),
        )
    }
    }
}

private enum class Overlay { FADERS, PEAKS, CONSOLE, SWEEP, COMPARE, HEARING, FINDER, REFINE, SHAPES, CURVES, WELCOME }

/** Opens each full screen; passed down, since the screens are chosen above the app's own content. */
private class OverlayOpeners(
    val finder: () -> Unit,
    val refine: () -> Unit,
    val shapes: () -> Unit,
    val hearing: () -> Unit,
    val compare: () -> Unit,
    val sweep: () -> Unit,
    val peaks: () -> Unit,
    val console: () -> Unit,
    val faders: () -> Unit,
    val curves: () -> Unit,
)

// Decelerating from full speed, so a page moves on the very first frame after the tap rather than easing in.
private fun <T> screenSlide() = tween<T>(SCREEN_MS, easing = androidx.compose.animation.core.LinearOutSlowInEasing)
private fun <T> screenFade() = tween<T>(SCREEN_MS / 2)
private const val SCREEN_MS = 300

/** The app itself: the bars, the strip, and the tab that's open. */
@Composable
private fun AppScreen(
    state: AppState,
    status: EngineStatus,
    tab: Tab,
    setTab: (Tab) -> Unit,
    beforeSettings: Tab,
    setBeforeSettings: (Tab) -> Unit,
    scrolls: Map<Tab, ScrollState>,
    tabScope: CoroutineScope,
    toggleSettings: () -> Unit,
    snackbar: SnackbarHostState,
    onSetEnabled: (Boolean) -> Unit,
    open: OverlayOpeners,
) {

    // The tour runs on the Equaliser tab, scrolled so the control it points at is in view.
    val tourStop = Tour.stop
    val screenHeight = with(LocalDensity.current) { androidx.compose.ui.platform.LocalConfiguration.current.screenHeightDp.dp.toPx() }
    val navRoom = with(LocalDensity.current) { 140.dp.toPx() }
    LaunchedEffect(tourStop) {
        if (tourStop == null) return@LaunchedEffect
        if (tab != Tab.EQ) setTab(Tab.EQ)
        when (tourStop) {
            TourStop.FADERS -> {
                kotlinx.coroutines.delay(60)
                val r = Tour.anchors[tourStop] ?: return@LaunchedEffect
                val over = r.bottom - (screenHeight - navRoom)
                if (over > 0f) scrolls.getValue(Tab.EQ).animateScrollBy(over)
            }
            TourStop.POWER -> scrolls.getValue(Tab.EQ).animateScrollTo(0)
            else -> Unit
        }
    }

    // Straight after the welcome, the app powers on out of the lit line it closed into: a pulse rings out,
    // a spectrum bursts from the line, and the app fades in as the bars settle back into it.
    val opening = remember { Animatable(if (openingFromWelcome.value) 0f else 1f) }
    val haptics = androidx.compose.ui.platform.LocalHapticFeedback.current
    LaunchedEffect(Unit) {
        if (openingFromWelcome.value) {
            launch {
                kotlinx.coroutines.delay((OPEN_MS * OPEN_UNFOLD_AT).toLong())
                haptics.performHapticFeedback(androidx.compose.ui.hapticfeedback.HapticFeedbackType.LongPress)
            }
            opening.animateTo(1f, tween(OPEN_MS, easing = LinearEasing))
            openingFromWelcome.value = false
            // A new user goes straight on to the tour of the screen they've just arrived at.
            if (!Repo.state.value.tourSeen) Tour.start()
        }
    }
    // Back, or a tap anywhere outside the strip, closes its drawer. The tap still does what it was for,
    // such as switching tab, so closing the drawer never costs an extra tap.
    androidx.activity.compose.BackHandler(enabled = StripDrawer.open) { StripDrawer.open = false }
    Box(
        Modifier
            .fillMaxSize()
            .pointerInput(Unit) {
                awaitEachGesture {
                    val down = awaitFirstDown(requireUnconsumed = false, pass = PointerEventPass.Initial)
                    if (!StripDrawer.open || StripDrawer.bounds.contains(down.position)) return@awaitEachGesture
                    StripDrawer.open = false
                }
            }
            .drawWithContent {
                val p = opening.value
                if (p >= 1f) {
                    drawContent()
                    return@drawWithContent
                }
                drawRect(Palette.Background)
                drawContent()
                drawPowerOn(p)
            }
            .graphicsLayer {
                val unfold = phase(opening.value, OPEN_UNFOLD_AT, 1f)
                // The app fades up in place as the line and bars die away, quickly at first and settling gently.
                alpha = 1f - (1f - unfold) * (1f - unfold) * (1f - unfold)
            },
    ) {
    CompositionLocalProvider(
        LocalSnackbar provides snackbar,
        LocalSnackScope provides tabScope,
        LocalOpenFinder provides open.finder,
        LocalOpenRefine provides open.refine,
        LocalOpenShapes provides open.shapes,
        LocalOpenHearing provides open.hearing,
        LocalOpenCompare provides open.compare,
        LocalOpenSweep provides open.sweep,
        LocalOpenPeaks provides open.peaks,
        LocalOpenConsole provides open.console,
        LocalOpenFaders provides open.faders,
        LocalOpenCurves provides open.curves,
        LocalOpenPresets provides {
            tabScope.launch { scrolls.getValue(Tab.PRESETS).scrollTo(0) }
            setTab(Tab.PRESETS)
        },
        // A row in the strip's drawer goes to the tab and section where that effect is set.
        LocalOpenEffect provides { name ->
            val (target, section) = when (name) {
                "Night", "Level" -> Tab.SYSTEM to R.string.ui_dynamics
                "Surround" -> Tab.SYSTEM to R.string.ui_stereo
                "Ears" -> Tab.HEADPHONES to R.string.ui_your_ears
                "Comp" -> Tab.SOUND to R.string.ui_compressor
                "Tame" -> Tab.SOUND to R.string.ui_dynamic_eq
                else -> Tab.SOUND to R.string.ui_space
            }
            if (target == Tab.SYSTEM && tab != Tab.SYSTEM) setBeforeSettings(tab)
            setTab(target)
            SectionFocus.request = appText(section)
        },
    ) {
        Scaffold(
            containerColor = Palette.Background,
            topBar = { TopBar(state, status, showCurve = tab != Tab.EQ, settingsOpen = tab == Tab.SYSTEM, onSettings = toggleSettings, onSetEnabled) },
            // Messages wear the same card as the unsaved bar, so the two floating notes look like one family.
            snackbarHost = {
                SnackbarHost(snackbar) { data ->
                    androidx.compose.material3.Snackbar(
                        snackbarData = data,
                        shape = PanelShape,
                        containerColor = Palette.Note,
                        contentColor = Palette.NoteText,
                        actionColor = Palette.NoteText,
                        dismissActionContentColor = Palette.TextDim,
                    )
                }
            },
            bottomBar = {
                // Settings lives in the top bar, which leaves the bottom for the four everyday tabs. They share
                // the width equally, with labels of similar length, so the spacing is even edge to edge.
                Row(
                    Modifier
                        .fillMaxWidth()
                        .background(Palette.Bar)
                        .windowInsetsPadding(WindowInsets.navigationBars)
                        .padding(start = NAV_EDGE_DP.dp, end = NAV_EDGE_DP.dp, top = 12.dp, bottom = 12.dp)
                        .tourTarget(TourStop.TABS)
                        .selectableGroup(),
                ) {
                    val navOutput by Repo.output.collectAsStateWithLifecycle()
                    Tab.entries.filter { it != Tab.SYSTEM }.forEach { t ->
                        NavItem(t, selected = tab == t, device = navOutput.kind.takeIf { t == Tab.HEADPHONES }, modifier = Modifier.weight(1f)) {
                            // Moving to a tab starts it at the top, and tapping the one already open scrolls it back up.
                            if (tab == t) {
                                tabScope.launch { scrolls.getValue(t).animateScrollTo(0) }
                            } else {
                                tabScope.launch { scrolls.getValue(t).scrollTo(0) }
                                setTab(t)
                            }
                        }
                    }
                }
            },
        ) { padding ->
            androidx.activity.compose.BackHandler(enabled = tab == Tab.SYSTEM) { setTab(beforeSettings) }
            Box(
                Modifier
                    .fillMaxSize()
                    .padding(padding),
                contentAlignment = Alignment.TopCenter,
            ) {
                // Every tab stays built once made, so a switch only slides pages rather than first building one.
                // The rest are built shortly after the app appears, one at a time, so opening stays quick.
                var built by remember { mutableStateOf(setOf(tab)) }
                LaunchedEffect(Unit) {
                    androidx.compose.runtime.snapshotFlow { opening.value >= 1f }.first { it }
                    kotlinx.coroutines.delay(TAB_PREBUILD_DELAY_MS)
                    Tab.entries.forEach { t ->
                        if (t in built) return@forEach
                        built = built + t
                        repeat(2) { androidx.compose.runtime.withFrameNanos { } }
                    }
                }
                // What each page last showed while in view. A page out of view keeps it, so changes elsewhere,
                // such as a fader being dragged, don't make the hidden pages redo their work.
                val lastSeen = remember { HashMap<Tab, Pair<AppState, EngineStatus>>() }
                // Tabs slide towards where they sit in the bar, so moving right slides the page left, and back.
                val pages = androidx.compose.animation.core.updateTransition(tab, label = "tab")
                Tab.entries.forEach { t ->
                    if (t !in built && t != tab) return@forEach
                    androidx.compose.runtime.key(t) {
                        val presence = pages.animateFloat(
                            transitionSpec = { tween(TAB_SLIDE_MS, easing = LinearOutSlowInEasing) },
                            label = "page",
                        ) { if (it == t) 1f else 0f }
                        val seen = if (t == tab) (state to status).also { lastSeen[t] = it } else lastSeen.getOrPut(t) { state to status }
                        val shownNow = remember(t) { { pages.targetState == t } }
                        CompositionLocalProvider(LocalPageShown provides shownNow) {
                            // The whole width scrolls, so the space beside the page works too, as on a tablet or in landscape.
                            Box(
                                Modifier
                                    .fillMaxSize()
                                    // Out of view, the page isn't read out.
                                    .then(if (t == tab) Modifier else Modifier.clearAndSetSemantics { })
                                    // A page out of view stays laid out and drawn just past the edge of the screen, where it
                                    // can't be touched, so bringing it into view only slides it rather than drawing it anew.
                                    .layout { measurable, constraints ->
                                        val placeable = measurable.measure(constraints)
                                        layout(placeable.width, placeable.height) {
                                            val inView = t == pages.targetState || presence.value > 0f
                                            placeable.place(if (inView) 0 else placeable.width * 2, 0)
                                        }
                                    }
                                    .graphicsLayer {
                                        val p = presence.value
                                        val entering = t == pages.targetState
                                        val sign = if (pages.targetState.ordinal >= pages.currentState.ordinal) 1f else -1f
                                        translationX = (if (entering) sign else -sign) * (1f - p) * size.width / 3f
                                        // Fades over the first half: the new page arrives quickly, the old one leaves quickly.
                                        alpha = if (entering) (p * 2f).coerceAtMost(1f) else ((p - 0.5f) * 2f).coerceIn(0f, 1f)
                                    }
                                    .verticalScroll(scrolls.getValue(t)),
                                contentAlignment = Alignment.TopCenter,
                            ) {
                                Column(
                                    Modifier
                                        .widthIn(max = 640.dp)
                                        .fillMaxWidth()
                                        // Room at the end, so the last setting can scroll clear of the floating save bar.
                                        .padding(start = 16.dp, end = 16.dp, top = 12.dp, bottom = UNSAVED_BAR_ROOM_DP.dp),
                                    verticalArrangement = SectionSpacing,
                                ) {
                                    when (t) {
                                        Tab.EQ -> EqTab(seen.first)
                                        Tab.SOUND -> SoundTab(seen.first)
                                        Tab.PRESETS -> PresetsTab(seen.first)
                                        Tab.HEADPHONES -> HeadphonesTab(seen.first)
                                        Tab.SYSTEM -> SystemTab(seen.first, seen.second)
                                    }
                                }
                            }
                        }
                    }
                }
                // Floats over the page rather than sitting in it, so appearing can never move anything,
                // such as a fader under a finger that's still dragging it.
                val savedVersion = Repo.allPresets(state).firstOrNull { it.id == state.current.id }
                // Shown as an example while the tour explains it, so that stop has something to point at.
                val touring = Tour.stop == TourStop.SAVE
                val unsaved = (savedVersion != null && savedVersion != state.current) || touring
                // Kept while the bar slides away, so it leaves showing what it said rather than going blank.
                var lastSaved by remember { mutableStateOf(savedVersion) }
                if (unsaved) lastSaved = savedVersion ?: state.current
                // Rises from the bottom when there's something to save, and sinks back the same way.
                AnimatedVisibility(
                    visible = unsaved,
                    enter = slideInVertically(tween(SCREEN_MS, easing = FastOutSlowInEasing)) { it } + fadeIn(tween(SCREEN_MS)),
                    exit = slideOutVertically(tween(SCREEN_MS, easing = FastOutSlowInEasing)) { it } + fadeOut(tween(SCREEN_MS)),
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .widthIn(max = 640.dp)
                        .padding(horizontal = 12.dp, vertical = 8.dp)
                        .tourTarget(TourStop.SAVE),
                ) {
                    lastSaved?.let { UnsavedBar(state, it) }
                }
            }
        }
    }
    TourOverlay()
    }
}

/** One bottom tab: a lit pill behind the icon when chosen, and the name below. */
@Composable
private fun NavItem(tab: Tab, selected: Boolean, device: OutputKind? = null, modifier: Modifier = Modifier, onClick: () -> Unit) {
    // The whole tab takes the touch, but the press ripples inside the rounded pill, as Android's own tabs do,
    // rather than as a tight box around the narrow tab.
    val presses = remember { androidx.compose.foundation.interaction.MutableInteractionSource() }
    Column(
        modifier
            .selectable(selected = selected, interactionSource = presses, indication = null, role = Role.Tab, onClick = onClick)
            .padding(vertical = 2.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        val tint = if (selected) Palette.OnLit else Palette.TextDim
        // The device tab's icon sits over the pill rather than in it, so its change animation isn't clipped.
        Box(contentAlignment = Alignment.Center) {
            Box(
                Modifier
                    .size(width = 64.dp, height = 32.dp)
                    .clip(androidx.compose.foundation.shape.RoundedCornerShape(16.dp))
                    .background(if (selected) Palette.LitFill else Color.Transparent)
                    .indication(presses, androidx.compose.material3.ripple()),
                contentAlignment = Alignment.Center,
            ) {
                // The label below already names the tab for screen readers.
                if (device == null) Icon(painterResource(tab.icon), contentDescription = null, tint = tint)
            }
            if (device != null) DeviceTabIcon(device, tint = tint, ring = Palette.Lit)
        }
        val label = stringResource(device?.tabLabel() ?: tab.label)
        androidx.compose.animation.AnimatedContent(
            targetState = label,
            transitionSpec = {
                (androidx.compose.animation.fadeIn(tween(220, delayMillis = 200)) + androidx.compose.animation.slideInVertically(tween(260, delayMillis = 200)) { it / 2 })
                    .togetherWith(androidx.compose.animation.fadeOut(tween(180)) + androidx.compose.animation.slideOutVertically(tween(200)) { -it / 2 })
            },
            label = "deviceLabel",
        ) { text ->
        Text(
            text,
            style = MaterialTheme.typography.labelMedium,
            color = if (selected) Palette.Text else Palette.TextDim,
            maxLines = 1,
            softWrap = false,
            modifier = Modifier.padding(top = 4.dp),
        )
        }
    }
}

/** Stays on screen, so power and Bypass are always one tap away. */
@Composable
private fun TopBar(
    state: AppState,
    status: EngineStatus,
    showCurve: Boolean,
    settingsOpen: Boolean,
    onSettings: () -> Unit,
    onSetEnabled: (Boolean) -> Unit,
) {
    // Here because the strip below always shows the live spectrum. It still stops when the app is closed.
    AnalyserEffect(state.analyser)
    Column(
        Modifier
            .fillMaxWidth()
            .background(Palette.Bar)
            .windowInsetsPadding(WindowInsets.statusBars),
    ) {
    Row(
        Modifier
            .fillMaxWidth()
            .padding(start = 16.dp, end = 4.dp, top = 8.dp, bottom = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        val connectedNames = rememberConnectedOutputNames()
        val quickPresets = remember(state.current.id, state.favorites, state.presets, connectedNames) {
            PresetOrganiser.quickKeys(state, BuiltIns.availableFor(connectedNames) + state.presets)
        }
        PresetSwitcher(
            label = statusLine(state, status),
            problem = state.enabled && (status.error != null || !status.hasControl),
            presets = quickPresets,
            currentId = state.current.id,
            favorites = state.favorites,
            onPick = Repo::loadPreset,
            onAll = LocalOpenPresets.current,
            modifier = Modifier
                .weight(1f)
                .tourTarget(TourStop.PRESETS),
        ) {
            // The version beside the name, small and quiet, so which build is running is plain at a glance.
            val context = LocalContext.current
            val version = remember {
                runCatching { context.packageManager.getPackageInfo(context.packageName, 0).versionName }.getOrNull().orEmpty()
            }
            // Neither wraps: when the bar is tight the version is clipped rather than pushed into the name.
            Row {
                Text(
                    stringResource(R.string.app_name),
                    style = MaterialTheme.typography.titleLarge,
                    color = Palette.Text,
                    maxLines = 1,
                    softWrap = false,
                    modifier = Modifier.alignByBaseline(),
                )
                if (version.isNotEmpty()) {
                    Text(
                        version,
                        style = Readout.copy(fontSize = MaterialTheme.typography.labelMedium.fontSize),
                        color = Palette.TextDim,
                        maxLines = 1,
                        softWrap = false,
                        overflow = TextOverflow.Clip,
                        modifier = Modifier
                            .alignByBaseline()
                            .weight(1f, fill = false)
                            .padding(start = 6.dp),
                    )
                }
            }
        }
        // Even gaps: version, Bypass, switch and settings sit 12 dp apart (the settings icon carries its own 12 dp).
        Row(Modifier.padding(start = 8.dp).tourTarget(TourStop.POWER), verticalAlignment = Alignment.CenterVertically) {
            if (state.enabled) BypassButton(state.bypass)
            val view = androidx.compose.ui.platform.LocalView.current
            Switch(
                colors = switchColors(),
                checked = state.enabled,
                // Felt as well as seen, since it's the switch that matters most.
                onCheckedChange = { on ->
                    Haptics.toggle(view, on)
                    onSetEnabled(on)
                },
                modifier = Modifier
                    .litGlow(state.enabled && !state.bypass)
                    .padding(start = 8.dp)
                    .semantics { contentDescription = "Eigentone" },
            )
        }
        IconButton(onClick = onSettings) {
            Icon(
                Icons.Outlined.Settings,
                contentDescription = stringResource(R.string.ui_tab_system),
                tint = if (settingsOpen) Palette.Lit else Palette.TextDim,
                // The tour outlines the gear itself rather than its whole touch area, which would reach the switch.
                modifier = Modifier.tourTarget(TourStop.SETTINGS),
            )
        }
    }
    Box(Modifier.tourTarget(TourStop.STRIP)) { LiveStrip(state, showCurve) }
    }
}

// Room for "Bypassed" clear of the pill's rounded ends.
private val BypassPadding = PaddingValues(horizontal = 12.dp, vertical = 8.dp)

/** Distinct from the power switch: neutral until engaged, when it's clearly a held state. */
@Composable
private fun BypassButton(bypassed: Boolean) {
    val view = androidx.compose.ui.platform.LocalView.current
    val toggle = {
        Haptics.toggle(view, !bypassed)
        Repo.update { it.copy(bypass = !it.bypass) }
    }
    // Both words are laid out and only one shows, so the button keeps one width and nothing beside it moves.
    val label: @Composable () -> Unit = {
        Box(contentAlignment = Alignment.Center) {
            Text(stringResource(R.string.ui_bypassed), maxLines = 1, modifier = Modifier.alpha(if (bypassed) 1f else 0f))
            Text(stringResource(R.string.ui_bypass), maxLines = 1, modifier = Modifier.alpha(if (bypassed) 0f else 1f))
        }
    }
    if (bypassed) {
        // Engaged, it's a warning: the EQ is silently off. Orange, since red belongs to the right ear.
        androidx.compose.material3.Button(
            onClick = { toggle() },
            shape = KeyShape,
            contentPadding = BypassPadding,
            colors = androidx.compose.material3.ButtonDefaults.buttonColors(
                containerColor = androidx.compose.ui.graphics.lerp(Palette.Surface, Palette.Danger, 0.3f),
                contentColor = Palette.Danger,
            ),
            border = androidx.compose.foundation.BorderStroke(1.dp, Palette.Danger.copy(alpha = 0.6f)),
        ) { label() }
    } else {
        FilledTonalButton(onClick = { toggle() }, contentPadding = BypassPadding) { label() }
    }
}

private fun statusLine(state: AppState, status: EngineStatus): String {
    val saved = Repo.allPresets(state).firstOrNull { it.id == state.current.id }
    // A preset deleted while in use isn't "edited": there's nothing left to compare it with.
    val name = when (saved) {
        null -> appText(R.string.ui_not_saved, state.current.name)
        state.current -> state.current.name
        else -> appText(R.string.ui_edited, state.current.name)
    }
    return when {
        !state.enabled -> appText(R.string.ui_mode_off)
        status.error != null -> appText(R.string.ui_problem_see_the_system_tab)
        !status.running -> appText(R.string.ui_starting)
        !status.hasControl -> appText(R.string.ui_another_audio_app_has_taken_control)
        else -> name
    }
}

@Composable
private fun EqTab(state: AppState) {
    val filters = state.current.filters
    var selectedId by rememberSaveable { mutableStateOf<Int?>(filters.firstOrNull()?.id) }
    var graphicMode by rememberSaveable { mutableStateOf(true) }
    val openFinder = LocalOpenFinder.current
    // The chosen filter can stop existing without a tap here, as when Discard, undo or switching preset
    // removes it. Falling back to the first filter keeps the editor open rather than leaving nothing chosen.
    val selected = filters.firstOrNull { it.id == selectedId } ?: filters.firstOrNull()
    LaunchedEffect(selected?.id) { if (selected?.id != selectedId) selectedId = selected?.id }
    val showParametric = Disclosure.parametric(state)
    val inGraphic = graphicMode || !showParametric
    // The graph grows for parametric editing and shrinks back above the faders, rather than jumping.
    val graphHeight = androidx.compose.animation.core.animateDpAsState(if (inGraphic) 170.dp else 260.dp, tween(TAB_SLIDE_MS, easing = LinearOutSlowInEasing), label = "graphHeight")
    val attenuation by Repo.attenuationDb.collectAsStateWithLifecycle()
    val curveFilters = remember(state, attenuation, Repo.output.value.key) { Processing.effectiveFilters(state, attenuation, Repo.output.value.key) }
    val graphOutput by Repo.output.collectAsStateWithLifecycle()
    val earCurves = remember(curveFilters, state.current.filters, state.earCorrections, graphOutput.key, state.useSystemSurround, state.bypass) {
        if (state.bypass) null else Processing.channelFilters(state, graphOutput.key, curveFilters)
    }
    // The measured points of the ear correction, sitting on each ear's own curve.
    val earMarkers = remember(earCurves, state.earCorrections, graphOutput.key) {
        val ears = state.earsFor(graphOutput.key)?.takeIf { it.enabled }
        if (ears == null || earCurves == null) {
            null
        } else {
            io.github.dragjon.eigentone.data.EarCorrection(
                left = ears.left.map { io.github.dragjon.eigentone.data.ShapePoint(it.hz, Eq.curveDb(earCurves.first, it.hz)) },
                right = ears.right.map { io.github.dragjon.eigentone.data.ShapePoint(it.hz, Eq.curveDb(earCurves.second, it.hz)) },
            )
        }
    }
    val savedPreset = Repo.allPresets(state).firstOrNull { it.id == state.current.id }
    val savedCurve = remember(savedPreset, state.current, state.loudness, state.loudnessStrength, attenuation, state.bypass) {
        savedPreset?.takeIf { it != state.current && !state.bypass }
            ?.let { Processing.effectiveFilters(state.copy(current = it), attenuation, Repo.output.value.key) }
    }
    val spectrumState = Repo.spectrum.collectAsStateWithLifecycle()
    val pageShown = LocalPageShown.current

    if (!state.tourSeen && Tour.stop == null) {
        TourOffer()
    } else if (!state.guidedSetupSeen) {
        Column(
            Modifier
                .fillMaxWidth()
                .clip(PanelShape)
                .background(Palette.SurfaceHigh)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Text(stringResource(R.string.ui_new_here_find_your_sound), style = MaterialTheme.typography.titleMedium, color = Palette.Text)
            Text(
                stringResource(R.string.ui_compare_two_versions_of_a_song_a_few_tim),
                style = MaterialTheme.typography.bodyMedium,
                color = Palette.TextDim,
            )
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Button(onClick = openFinder) { Text(stringResource(R.string.ui_start)) }
                TextButton(onClick = { Repo.update { it.copy(guidedSetupSeen = true) } }) { Text(stringResource(R.string.ui_not_now)) }
            }
        }
    } else {
        OutputPrompts(state)
    }

    Row(
        Modifier
            .fillMaxWidth()
            .clip(PanelShape)
            .background(Palette.Surface)
            // The graph sits inset in its rounded panel, so its labels and the info icon keep clear of the corners.
            .padding(start = GRAPH_INSET_DP.dp, top = GRAPH_INSET_DP.dp, bottom = GRAPH_INSET_DP.dp)
            .height(androidx.compose.foundation.layout.IntrinsicSize.Min),
    ) {
        Box(Modifier.weight(1f)) {
        EqGraph(
            legendInsetDp = if (state.analyser) SPECTRUM_INFO_INSET_DP else 0,
            // Graphic mode has its own faders, so the parametric nodes would only get in the way.
            filters = if (inGraphic) emptyList() else filters,
            height = { graphHeight.value },
            curveFilters = earCurves?.first ?: curveFilters,
            rightCurveFilters = earCurves?.second,
            ghostFilters = savedCurve,
            morphKey = state.current.id,
            earMarkers = earMarkers,
            // Not read while the page is out of view, so the graph isn't redrawn off screen with every reading.
            spectrum = { if (pageShown() && state.analyser) spectrumState.value else null },
            selectedId = selectedId,
            dimmed = !state.enabled || state.bypass,
            onSelect = { selectedId = it },
            onDrag = { id, freq, gain ->
                updateFilter(id) { f -> f.copy(freq = freq, gainDb = if (f.type.hasGain) gain else f.gainDb) }
            },
            onDragEnd = Repo::commit,
        )
            // Explains the spectrum while it shows, faint so it doesn't compete with the curve.
            if (state.analyser) {
                Box(
                    Modifier
                        .align(Alignment.TopStart)
                        // Centres the icon over the column of dB labels below it, clear of the panel's edge,
                        // and in line with the ear legend's row.
                        .offset(x = (-10).dp, y = (-10).dp)
                        .alpha(SPECTRUM_INFO_ALPHA),
                ) { InfoButton(Help.analyser) }
            }
        }
        // The output level beside the spectrum it comes from, at the graph's right edge.
        // Tapping it explains it. The touch area is wider than the bar, so it's easy to hit.
        if (state.analyser) TipOnTap(Help.meter, Modifier.requiredWidth(28.dp).fillMaxHeight()) {
            OutputMeterBar(Modifier.padding(top = 12.dp, bottom = 12.dp))
        }
    }
    if (state.analyser && analyserUnavailable.value) {
        Text(stringResource(R.string.ui_this_phone_refused_the_analyser), style = MaterialTheme.typography.bodySmall, color = Palette.TextDim)
    }

    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.End,
        modifier = Modifier.fillMaxWidth(),
    ) {
        // Without the mode switch beside it, the analyser is a labelled chip, so it doesn't sit alone as a bare icon.
        if (!showParametric) {
            AnalyserChip(state)
        } else SlidingToggle(
            options = listOf(ToggleOption(stringResource(R.string.ui_graphic)), ToggleOption(stringResource(R.string.ui_parametric))),
            selected = if (graphicMode) 0 else 1,
            onSelect = { graphicMode = it == 0 },
            modifier = Modifier.weight(1f),
        )
        // Beside the mode rather than on the graph, where the ear legends sit.
        if (showParametric) AnalyserToggle(state, Modifier)
        if (showParametric) InfoButton(Help.eqMode)
    }

    // The faders and the filter list swap with a slide that follows the switch above: Graphic is on the
    // left, so it comes in from the left, and Parametric from the right.
    AnimatedContent(
        targetState = inGraphic,
        transitionSpec = {
            val sign = if (targetState) -1 else 1
            (slideInHorizontally(tween(TAB_SLIDE_MS, easing = LinearOutSlowInEasing)) { sign * it / 4 } + fadeIn(tween(TAB_SLIDE_MS / 2))) togetherWith
                (slideOutHorizontally(tween(TAB_SLIDE_MS, easing = LinearOutSlowInEasing)) { -sign * it / 4 } + fadeOut(tween(TAB_SLIDE_MS / 2))) using
                // The panel's height changes on the same curve and time as the slide, so everything moves together.
                androidx.compose.animation.SizeTransform(clip = false) { _, _ -> tween(TAB_SLIDE_MS, easing = LinearOutSlowInEasing) }
        },
        label = "eqMode",
    ) { graphic ->
    Column(verticalArrangement = SectionSpacing) {
    if (graphic) {
        GraphicEqConsole(state.current.tone, state.current.id, onBrowseShapes = LocalOpenShapes.current, onExpand = LocalOpenFaders.current)
        // A shortcut to Refine, which is also on the device tab with the other listening tools.
        SettingsSection(null) {
            ActionRow(stringResource(R.string.ui_refine_this), Help.refine, detail = stringResource(R.string.ui_refine_this_detail), onClick = LocalOpenRefine.current)
        }
        return@Column
    }

    Row(
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier.fillMaxWidth(),
    ) {
        FilledTonalButton(
            enabled = filters.size < MAX_FILTERS,
            onClick = {
                // The id comes from the filters at the moment of adding, so two quick taps can't share one.
                var id = 0
                Repo.edit { p ->
                    id = (p.filters.maxOfOrNull { it.id } ?: 0) + 1
                    p.copy(filters = p.filters + Filter(id))
                }
                Repo.commit()
                selectedId = id
            },
            contentPadding = ButtonDefaults.ButtonWithIconContentPadding,
        ) {
            Icon(Icons.Filled.Add, contentDescription = null, modifier = Modifier.size(18.dp))
            Spacer(Modifier.size(8.dp))
            Text(stringResource(R.string.ui_add_filter))
        }
        UndoRedoButtons()
    }

    if (selected == null) {
        Text(
            if (filters.isEmpty()) stringResource(R.string.ui_no_filters_yet_add_one_to)
            else stringResource(R.string.ui_tap_a_point_on_the_curve),
            color = Palette.TextDim,
            style = MaterialTheme.typography.bodyMedium,
        )
    } else {
        val snackbar = LocalSnackbar.current
        val snackScope = LocalSnackScope.current
        val undoLabel = stringResource(R.string.ui_undo)
        val filterDeleted = stringResource(R.string.ui_filter_deleted)
        val dynamicPlaced = remember(state) {
            val target = Processing.target(state.copy(bypass = false), 0.0)
            io.github.dragjon.eigentone.data.Dynamics.placedIds(target.dynamics, target.tamers)
        }
        // Moving to another filter slides its editor in from the side it sits on, by its place in the list.
        AnimatedContent(
            targetState = selected.id,
            transitionSpec = {
                val forward = filters.indexOfFirst { it.id == targetState } >= filters.indexOfFirst { it.id == initialState }
                val sign = if (forward) 1 else -1
                (slideInHorizontally(tween(TAB_SLIDE_MS, easing = LinearOutSlowInEasing)) { sign * it / 5 } + fadeIn(tween(TAB_SLIDE_MS / 2))) togetherWith
                    (slideOutHorizontally(tween(TAB_SLIDE_MS, easing = LinearOutSlowInEasing)) { -sign * it / 5 } + fadeOut(tween(TAB_SLIDE_MS / 2))) using
                    androidx.compose.animation.SizeTransform(clip = false)
            },
            label = "filter",
        ) { shownId ->
        val shown = filters.firstOrNull { it.id == shownId } ?: return@AnimatedContent
        FilterEditor(
            filter = shown,
            filters = filters,
            dynamicPlaced = dynamicPlaced,
            onSelect = { selectedId = it },
            onDelete = {
                val index = filters.indexOf(shown)
                val deletedId = shown.id
                Repo.commit()
                Repo.edit { p -> p.copy(filters = p.filters.filterNot { it.id == deletedId }) }
                Repo.commit()
                val remaining = Repo.state.value.current.filters
                selectedId = remaining.getOrNull(index.coerceAtMost(remaining.lastIndex))?.id
                // Deleting a preset offers Undo, and so does deleting a filter.
                snackScope.launch {
                    val result = snackbar.showSnackbar(
                        filterDeleted,
                        actionLabel = undoLabel,
                        withDismissAction = true,
                        duration = SnackbarDuration.Short,
                    )
                    if (result == SnackbarResult.ActionPerformed) {
                        Repo.undo()
                        selectedId = deletedId
                    }
                }
            },
        )
        }
    }
    }
    }
}

/** The unsaved changes in a few words, such as "1 added filter, faders and bass". */
@Composable
private fun changeSummary(parts: List<Pair<PresetChanges.Part, Int>>): String {
    val words = parts.map { (part, count) ->
        when (part) {
            PresetChanges.Part.ADDED_FILTERS -> pluralStringResource(R.plurals.ui_changes_added_filters, count, count)
            PresetChanges.Part.REMOVED_FILTERS -> pluralStringResource(R.plurals.ui_changes_removed_filters, count, count)
            PresetChanges.Part.CHANGED_FILTERS -> pluralStringResource(R.plurals.ui_changes_changed_filters, count, count)
            PresetChanges.Part.PREAMP -> stringResource(R.string.ui_changes_preamp)
            PresetChanges.Part.FADERS -> stringResource(R.string.ui_changes_faders)
            PresetChanges.Part.BASS -> stringResource(R.string.ui_changes_bass)
            PresetChanges.Part.TREBLE -> stringResource(R.string.ui_changes_treble)
            PresetChanges.Part.VOCALS -> stringResource(R.string.ui_changes_vocals)
            PresetChanges.Part.STRENGTH -> stringResource(R.string.ui_changes_strength)
            PresetChanges.Part.CORRECTION -> stringResource(R.string.ui_changes_correction)
            PresetChanges.Part.WIDTH -> stringResource(R.string.ui_changes_width)
            PresetChanges.Part.COMPRESSOR -> stringResource(R.string.ui_changes_compressor)
            PresetChanges.Part.CLARITY -> stringResource(R.string.ui_changes_clarity)
            PresetChanges.Part.REVERB -> stringResource(R.string.ui_changes_reverb)
            PresetChanges.Part.OTHER -> stringResource(R.string.ui_changes_other)
        }
    }
    return when (words.size) {
        0 -> ""
        1 -> words[0]
        else -> stringResource(R.string.ui_changes_and, words.dropLast(1).joinToString(", "), words.last())
    }
}

/** Shown while the preset has changes, so they can be kept or dropped without leaving the EQ. */
@Composable
private fun UnsavedBar(state: AppState, saved: Preset, modifier: Modifier = Modifier) {
    val scope = LocalSnackScope.current
    val snackbar = LocalSnackbar.current
    var naming by remember { mutableStateOf(false) }
    val own = state.presets.any { it.id == saved.id }
    Row(
        modifier
            .fillMaxWidth()
            .shadow(6.dp, PanelShape)
            .clip(PanelShape)
            .background(Palette.Note)
            .padding(start = 16.dp, end = 8.dp, top = 4.dp, bottom = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        // Says what's unsaved, so it's plain what Discard would drop before it's tapped.
        val changes = changeSummary(remember(saved, state.current) { PresetChanges.of(saved, state.current) })
        Column(Modifier.weight(1f)) {
            Text(
                stringResource(R.string.ui_unsaved_changes, saved.name),
                style = MaterialTheme.typography.bodyMedium,
                color = Palette.Text,
                // Two lines, so the preset's name isn't cut to its first letter beside the two keys.
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )
            if (changes.isNotEmpty()) {
                Text(
                    changes,
                    style = MaterialTheme.typography.bodySmall,
                    color = Palette.TextDim,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }
        val undoLabel = stringResource(R.string.ui_undo)
        TextButton(onClick = {
            Repo.discardEdits()
            // Discard is a step in the undo history, so the message can offer it straight back.
            scope.launch {
                val result = snackbar.showSnackbar(
                    appText(R.string.ui_edits_discarded, saved.name),
                    actionLabel = undoLabel,
                    withDismissAction = true,
                    duration = SnackbarDuration.Long,
                )
                if (result == androidx.compose.material3.SnackbarResult.ActionPerformed) Repo.undo()
            }
        }) { Text(stringResource(R.string.ui_discard)) }
        val view = androidx.compose.ui.platform.LocalView.current
        Button(onClick = {
            if (own) {
                Haptics.confirm(view)
                Repo.overwriteCurrent()
                scope.notify(snackbar, appText(R.string.ui_updated, saved.name))
            } else {
                naming = true
            }
        }) { Text(stringResource(if (own) R.string.ui_save else R.string.ui_save_as_new), maxLines = 1) }
    }
    if (naming) {
        SaveDialog(initial = saved.name, onDismiss = { naming = false }) { name ->
            Repo.saveCurrentAs(name)
            naming = false
            scope.notify(snackbar, appText(R.string.ui_saved_as, name))
        }
    }
}

/** Runs the analyser only while the EQ tab is visible, and only if it has been turned on. */
@Composable
private fun AnalyserEffect(enabled: Boolean) {
    val context = LocalContext.current
    val lifecycle = LocalLifecycleOwner.current.lifecycle
    DisposableEffect(enabled, lifecycle) {
        // A reading already on its way when the analyser stops is dropped, so turning it off always clears the
        // bars rather than sometimes leaving the last ones behind.
        val live = java.util.concurrent.atomic.AtomicBoolean(true)
        val analyser = SpectrumAnalyser(
            onBands = { if (live.get()) Repo.spectrum.value = it },
            onLevel = { peak, rms -> if (live.get()) Repo.updateLevel(peak, rms) },
        )
        val observer = LifecycleEventObserver { _, event ->
            val allowed = enabled && context.checkSelfPermission(Manifest.permission.RECORD_AUDIO) ==
                PackageManager.PERMISSION_GRANTED
            when (event) {
                Lifecycle.Event.ON_START -> if (allowed) {
                    live.set(true)
                    analyserUnavailable.value = !analyser.start()
                }
                Lifecycle.Event.ON_STOP -> {
                    live.set(false)
                    analyser.stop()
                    Repo.spectrum.value = null
                    Repo.level.value = null
                }
                else -> Unit
            }
        }
        lifecycle.addObserver(observer)
        onDispose {
            live.set(false)
            lifecycle.removeObserver(observer)
            analyser.stop()
            Repo.spectrum.value = null
            Repo.level.value = null
        }
    }
}

private val analyserUnavailable = mutableStateOf(false)

/** Turns the live spectrum on or off, first asking for the microphone permission Android requires to read it. */
@Composable
private fun rememberAnalyserSwitch(): (Boolean) -> Unit {
    val permission = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
        if (granted) Repo.update { it.copy(analyser = true) }
    }
    val context = LocalContext.current
    return { on ->
        val granted = context.checkSelfPermission(Manifest.permission.RECORD_AUDIO) == PackageManager.PERMISSION_GRANTED
        if (on && !granted) permission.launch(Manifest.permission.RECORD_AUDIO) else Repo.update { it.copy(analyser = on) }
    }
}

/** The analyser as a labelled chip, for when it stands on its own row. */
@Composable
private fun AnalyserChip(state: AppState) {
    val switch = rememberAnalyserSwitch()
    FilterChip(
        selected = state.analyser,
        onClick = { switch(!state.analyser) },
        label = { Text(stringResource(R.string.ui_live_spectrum), maxLines = 1) },
        leadingIcon = { Icon(painterResource(R.drawable.ic_spectrum), contentDescription = null, modifier = Modifier.size(18.dp)) },
        colors = FilterChipDefaults.filterChipColors(
            selectedContainerColor = Palette.LitFill,
            selectedLabelColor = Palette.OnLit,
            selectedLeadingIconColor = Palette.OnLit,
            labelColor = Palette.Text,
            iconColor = Palette.TextDim,
        ),
    )
}

/** Shows the live spectrum behind the curve, as an icon beside the EQ mode switch. */
@Composable
private fun AnalyserToggle(state: AppState, modifier: Modifier) {
    val switch = rememberAnalyserSwitch()
    androidx.compose.material3.IconToggleButton(
        checked = state.analyser,
        onCheckedChange = { on -> switch(on) },
        modifier = modifier,
    ) {
        Icon(
            painterResource(R.drawable.ic_spectrum),
            contentDescription = stringResource(R.string.ui_spectrum_analyser),
            tint = if (state.analyser) Palette.Lit else Palette.TextDim,
        )
    }
}

private const val SPECTRUM_INFO_INSET_DP = 24

/** The graph's room inside its rounded panel. */
private const val GRAPH_INSET_DP = 8
private const val SPECTRUM_INFO_ALPHA = 0.7f

private fun updateFilter(id: Int, transform: (Filter) -> Filter) {
    Repo.edit { p -> p.copy(filters = p.filters.map { if (it.id == id) transform(it) else it }) }
}

@Composable
private fun FilterEditor(filter: Filter, filters: List<Filter>, dynamicPlaced: Set<Int>, onSelect: (Int) -> Unit, onDelete: () -> Unit) {
    val index = filters.indexOf(filter)
    SettingsSection(
        stringResource(R.string.ui_filter_of, index + 1, filters.size),
        trailing = {
            // Nodes can overlap on the graph, so every filter is also reachable from here.
            IconButton(enabled = index > 0, onClick = { onSelect(filters[index - 1].id) }) {
                Icon(Icons.Filled.KeyboardArrowLeft, contentDescription = stringResource(R.string.ui_previous_filter))
            }
            IconButton(enabled = index < filters.lastIndex, onClick = { onSelect(filters[index + 1].id) }) {
                Icon(Icons.Filled.KeyboardArrowRight, contentDescription = stringResource(R.string.ui_next_filter))
            }
            IconButton(onClick = onDelete) {
                Icon(painterResource(R.drawable.ic_delete), contentDescription = stringResource(R.string.ui_delete_filter), tint = Palette.TextDim)
            }
        },
    ) {
        SwitchSetting(stringResource(R.string.ui_enabled), Help.filterEnabled, filter.enabled) { on ->
            updateFilter(filter.id) { it.copy(enabled = on) }
            Repo.commit()
        }
        RowDivider()
        Column(
            Modifier.padding(start = 16.dp, end = 16.dp, top = 4.dp, bottom = 8.dp),
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(stringResource(R.string.ui_type), style = MaterialTheme.typography.bodyLarge, color = Palette.Text)
                InfoButton(Help.filterType)
            }
            // Each chip takes the width its name needs and the rows wrap, so no name is ever cut short.
            @OptIn(androidx.compose.foundation.layout.ExperimentalLayoutApi::class)
            androidx.compose.foundation.layout.FlowRow(
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                modifier = Modifier.fillMaxWidth(),
            ) {
                TypeChip(filter, FilterType.BELL, stringResource(R.string.ui_bell), Modifier)
                TypeChip(filter, FilterType.LOW_SHELF, stringResource(R.string.ui_low_shelf), Modifier)
                TypeChip(filter, FilterType.HIGH_SHELF, stringResource(R.string.ui_high_shelf), Modifier)
                TypeChip(filter, FilterType.TILT, stringResource(R.string.ui_tilt), Modifier)
                TypeChip(filter, FilterType.HIGH_PASS, stringResource(R.string.ui_high_pass), Modifier)
                TypeChip(filter, FilterType.LOW_PASS, stringResource(R.string.ui_low_pass), Modifier)
                TypeChip(filter, FilterType.NOTCH, stringResource(R.string.ui_notch), Modifier)
                TypeChip(filter, FilterType.BAND_PASS, stringResource(R.string.ui_band_pass), Modifier)
            }
        }
        RowDivider()
        Column(Modifier.padding(start = 16.dp, end = 16.dp, top = 4.dp, bottom = 12.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(stringResource(R.string.ui_channel), style = MaterialTheme.typography.bodyLarge, color = Palette.Text)
                InfoButton(Help.channel)
            }
            val channels = listOf(
                io.github.dragjon.eigentone.dsp.FilterChannel.BOTH to stringResource(R.string.ui_channel_both),
                io.github.dragjon.eigentone.dsp.FilterChannel.LEFT to stringResource(R.string.ui_channel_left),
                io.github.dragjon.eigentone.dsp.FilterChannel.RIGHT to stringResource(R.string.ui_channel_right),
            )
            SlidingToggle(
                options = channels.map { (_, name) -> ToggleOption(name) },
                selected = channels.indexOfFirst { it.first == filter.channel }.coerceAtLeast(0),
                onSelect = { i ->
                    val channel = channels[i].first
                    if (filter.channel != channel) {
                        updateFilter(filter.id) { it.copy(channel = channel) }
                        Repo.commit()
                    }
                },
                modifier = Modifier.fillMaxWidth(),
            )
        }
        if (filter.type.hasSlope) {
            RowDivider()
            Column(Modifier.padding(start = 16.dp, end = 16.dp, top = 4.dp, bottom = 12.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(stringResource(R.string.ui_slope), style = MaterialTheme.typography.bodyLarge, color = Palette.Text)
                    InfoButton(Help.slope)
                    Spacer(Modifier.weight(1f))
                    Text(stringResource(R.string.ui_db_per_octave), style = MaterialTheme.typography.bodySmall, color = Palette.TextDim)
                }
                val current = Eq.slopeOf(filter)
                SlidingToggle(
                    options = Eq.SLOPES.map { ToggleOption("$it", description = appText(R.string.ui_slope_option, it)) },
                    selected = Eq.SLOPES.indexOf(current).coerceAtLeast(0),
                    onSelect = { i ->
                        val option = Eq.SLOPES[i]
                        if (current != option) {
                            updateFilter(filter.id) { it.copy(slope = option) }
                            Repo.commit()
                        }
                    },
                    modifier = Modifier.fillMaxWidth(),
                )
            }
        }
        RowDivider()
        SliderSetting(
            title = stringResource(R.string.ui_frequency),
            tip = Help.frequency,
            readout = formatHz(filter.freq),
            value = Eq.logPosition(filter.freq).toFloat(),
            range = 0f..1f,
            // Frequency has no "off" value, so every position reads as a plain value.
            default = Eq.logPosition(filter.freq).toFloat(),
            onChange = { t -> updateFilter(filter.id) { it.copy(freq = Eq.logFreq(t.toDouble())) } },
            onFinished = Repo::commit,
        )
        if (filter.type.hasGain) {
            RowDivider()
            SliderSetting(
                title = stringResource(R.string.ui_gain),
                tip = Help.gain,
                readout = formatDb(filter.gainDb),
                value = filter.gainDb.toFloat(),
                range = -Eq.MAX_GAIN.toFloat()..Eq.MAX_GAIN.toFloat(),
                default = 0f,
                origin = 0f,
                onChange = { g -> updateFilter(filter.id) { it.copy(gainDb = g.toDouble()) } },
                onFinished = Repo::commit,
            )
        }
        // A 6 dB slope has a single gentle bend, so there's no Q to set.
        if (!(filter.type.hasSlope && Eq.slopeOf(filter) == 6)) {
        RowDivider()
        SliderSetting(
            title = stringResource(R.string.ui_q),
            tip = Help.q,
            readout = String.format(Locale.US, "%.2f", filter.q),
            value = qPosition(filter.q),
            range = 0f..1f,
            default = qPosition(1.0),
            onChange = { t -> updateFilter(filter.id) { it.copy(q = qFromPosition(t)) } },
            onFinished = Repo::commit,
        )
        }
        if (filter.canBeDynamic) DynamicFilterRows(filter, placed = filter.id in dynamicPlaced)
    }
}

/**
 * Turns a bell or shelf into a dynamic one, which also turns its range down while that range is loud. It
 * uses a band of Android's multiband compressor, which the compressor and Clarity share, so it says when
 * there's no band left for it.
 */
@Composable
private fun DynamicFilterRows(filter: Filter, placed: Boolean) {
    val dynamic = filter.dynamic
    RowDivider()
    SwitchSetting(
        stringResource(R.string.ui_dynamic),
        Help.dynamicFilter,
        dynamic != null,
        detail = when {
            dynamic == null -> stringResource(R.string.ui_dynamic_detail)
            placed -> null
            else -> stringResource(R.string.ui_dynamic_no_room)
        },
    ) { on ->
        updateFilter(filter.id) { it.copy(dynamic = if (on) it.dynamic ?: io.github.dragjon.eigentone.dsp.FilterDynamics() else null) }
        Repo.commit()
    }
    if (dynamic == null) return
    RowDivider()
    SliderSetting(
        title = stringResource(R.string.ui_threshold),
        tip = Help.dynamicFilter,
        readout = String.format(Locale.US, "%.0f dB", dynamic.thresholdDb),
        value = dynamic.thresholdDb.toFloat(),
        range = -60f..0f,
        default = -30f,
        onChange = { v -> updateFilter(filter.id) { it.copy(dynamic = dynamic.copy(thresholdDb = kotlin.math.round(v.toDouble()))) } },
        onFinished = Repo::commit,
    )
    RowDivider()
    SliderSetting(
        title = stringResource(R.string.ui_ratio),
        tip = Help.dynamicFilter,
        readout = String.format(Locale.US, "%.1f:1", dynamic.ratio),
        value = dynamic.ratio.toFloat(),
        range = 1f..10f,
        default = 3f,
        onChange = { v -> updateFilter(filter.id) { it.copy(dynamic = dynamic.copy(ratio = kotlin.math.round(v * 2.0) / 2.0)) } },
        onFinished = Repo::commit,
    )
}

@Composable
private fun TypeChip(filter: Filter, type: FilterType, label: String, modifier: Modifier) {
    FilterChip(
        modifier = modifier,
        selected = filter.type == type,
        onClick = {
            updateFilter(filter.id) { it.copy(type = type) }
            Repo.commit()
        },
        label = { Text(label, maxLines = 1, overflow = TextOverflow.Ellipsis) },
        colors = FilterChipDefaults.filterChipColors(
            selectedContainerColor = Palette.LitFill,
            selectedLabelColor = Palette.OnLit,
            labelColor = Palette.Text,
        ),
    )
}

private fun qPosition(q: Double): Float =
    (log10(q / Eq.MIN_Q) / log10(Eq.MAX_Q / Eq.MIN_Q)).toFloat()

private fun qFromPosition(t: Float): Double = Eq.MIN_Q * (Eq.MAX_Q / Eq.MIN_Q).pow(t.toDouble())

private fun formatHz(hz: Double): String =
    if (hz >= 1000) String.format(Locale.US, "%.2f kHz", hz / 1000) else String.format(Locale.US, "%.0f Hz", hz)

private fun formatDb(db: Double): String = String.format(Locale.US, "%+.1f dB", db)

private fun formatPercent(amount: Double): String = "${(amount * 100).roundToInt()}%"

/** Half-dB steps, which also makes it easy to land on exactly zero. */
private fun snapTone(value: Float): Double = kotlin.math.round(value * 2.0) / 2.0

private val toneRange = -Processing.MAX_TONE_DB.toFloat()..Processing.MAX_TONE_DB.toFloat()

/** The preset as last saved, where a double tap on one of its settings returns it. */
@Composable
private fun savedPreset(state: AppState): Preset? =
    remember(state.presets, state.current.id) { Repo.allPresets(state).firstOrNull { it.id == state.current.id } }

/** Says which settings a tab changes: the preset in use, or every preset. */
@Composable
private fun ScopeNote(text: String) {
    Text(
        text,
        style = MaterialTheme.typography.bodySmall,
        color = Palette.TextDim,
        modifier = Modifier.padding(horizontal = 4.dp),
    )
}

/** Everything here is part of the preset in use, saved and shared with it. */
@Composable
private fun SoundTab(state: AppState) {
    val saved = savedPreset(state)?.tone
    ScopeNote(stringResource(R.string.ui_sound_scope, state.current.name))
    // The note above already names the preset, so the legend says what the group is.
    if (Disclosure.presetLevel(state)) SettingsSection(stringResource(R.string.ui_preset_level)) {
        SliderSetting(
            title = stringResource(R.string.ui_preset_strength),
            tip = Help.strength,
            readout = formatPercent(state.current.tone.strength),
            value = state.current.tone.strength.toFloat(),
            range = 0f..1.5f,
            default = 1f,
            origin = 1f,
            onChange = { v -> Repo.editTone { it.copy(strength = kotlin.math.round(v * 20.0) / 20.0) } },
            onFinished = Repo::commit,
            resetTo = (saved?.strength ?: 1.0).toFloat(),
        )
        RowDivider()
        SliderSetting(
            title = stringResource(R.string.ui_preamp),
            tip = Help.preamp,
            readout = formatDb(state.current.preampDb),
            value = state.current.preampDb.toFloat(),
            range = -24f..12f,
            default = 0f,
            origin = 0f,
            onChange = { v -> Repo.edit { it.copy(preampDb = v.toDouble()) } },
            onFinished = Repo::commit,
            resetTo = (savedPreset(state)?.preampDb ?: 0.0).toFloat(),
        )
    }
    ToneSection(state, saved)
    SpaceSection(state, saved)
    CompressorSection(state)
}

/** What applies whichever preset is in use, at the top of Settings. */
@Composable
private fun EveryPresetSections(state: AppState) {
    val attenuation by Repo.attenuationDb.collectAsStateWithLifecycle()
    ScopeNote(stringResource(R.string.ui_settings_scope))
    DynamicsSection(state, attenuation)
    LevelSection(state, attenuation)
    StereoSection(state)
}

@Composable
private fun ToneSection(state: AppState, saved: Tone?) {
    val tone = state.current.tone
    val modified = tone.bassDb != 0.0 || tone.trebleDb != 0.0 || tone.vocalsDb != 0.0 || tone.bassTunerDb != 0.0 ||
        tone.bassHz != Tone.DEFAULT_BASS_HZ
    SettingsSection(
        stringResource(R.string.ui_tone),
        trailing = {
            if (modified) {
                val snackbar = LocalSnackbar.current
                val snackScope = LocalSnackScope.current
                TextButton(onClick = {
                    Repo.commit()
                    Repo.editTone {
                        it.copy(
                            bassDb = 0.0,
                            bassHz = Tone.DEFAULT_BASS_HZ,
                            trebleDb = 0.0,
                            vocalsDb = 0.0,
                            bassTunerDb = 0.0,
                            bassCharacter = Tone().bassCharacter,
                        )
                    }
                    Repo.commit()
                    // This tab has no undo key of its own, so the reset offers one.
                    snackScope.launch {
                        val result = snackbar.showSnackbar(
                            appText(R.string.ui_tone_reset),
                            actionLabel = appText(R.string.ui_undo),
                            withDismissAction = true,
                            duration = SnackbarDuration.Short,
                        )
                        if (result == SnackbarResult.ActionPerformed) Repo.undo()
                    }
                }) { Text(stringResource(R.string.ui_reset_tone)) }
            }
        },
    ) {
        ToneSlider(stringResource(R.string.ui_bass), Help.bass, tone.bassDb, saved?.bassDb ?: 0.0) { v -> Repo.editTone { it.copy(bassDb = v) } }
        // Only means something once there's a bass change to place.
        if (tone.bassDb != 0.0 && Disclosure.bassDetail(state)) {
            RowDivider()
            SliderSetting(
                title = stringResource(R.string.ui_bass_frequency),
                tip = Help.bassFrequency,
                readout = formatHz(tone.bassHz),
                value = tone.bassHz.toFloat(),
                range = 30f..250f,
                default = Tone.DEFAULT_BASS_HZ.toFloat(),
                onChange = { v -> Repo.editTone { it.copy(bassHz = v.toDouble()) } },
                onFinished = Repo::commit,
                resetTo = (saved?.bassHz ?: Tone.DEFAULT_BASS_HZ).toFloat(),
            )
        }
        // Only shapes bass that's been added, so it only shows then.
        if (tone.bassDb > 0.0 && Disclosure.bassDetail(state)) {
            RowDivider()
            Column(Modifier.padding(start = 16.dp, end = 16.dp, top = 4.dp, bottom = 12.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(stringResource(R.string.ui_bass_character), style = MaterialTheme.typography.bodyLarge, color = Palette.Text)
                    InfoButton(Help.bassCharacter)
                }
                WordSegments(
                    labels = listOf(
                        stringResource(R.string.ui_bass_natural),
                        stringResource(R.string.ui_bass_punch),
                        stringResource(R.string.ui_bass_sustain),
                    ),
                    selected = io.github.dragjon.eigentone.data.BassCharacter.entries.indexOf(tone.bassCharacter),
                    onSelect = { i ->
                        Repo.editTone { it.copy(bassCharacter = io.github.dragjon.eigentone.data.BassCharacter.entries[i]) }
                        Repo.commit()
                    },
                )
            }
        }
        RowDivider()
        ToneSlider(stringResource(R.string.help_vocals_title), Help.vocals, tone.vocalsDb, saved?.vocalsDb ?: 0.0) { v -> Repo.editTone { it.copy(vocalsDb = v) } }
        RowDivider()
        ToneSlider(stringResource(R.string.ui_treble), Help.treble, tone.trebleDb, saved?.trebleDb ?: 0.0) { v -> Repo.editTone { it.copy(trebleDb = v) } }
        // Only on a sound from an earlier version that used both bass controls. Setting it to zero retires it.
        if (tone.bassTunerDb != 0.0) {
            RowDivider()
            ToneSlider(stringResource(R.string.ui_extra_bass, formatHz(tone.bassTunerHz)), Help.extraBass, tone.bassTunerDb) { v ->
                Repo.editTone { it.copy(bassTunerDb = v) }
            }
        }
    }
}

@Composable
private fun ToneSlider(title: String, tip: Tip, value: Double, saved: Double = 0.0, onChange: (Double) -> Unit) {
    SliderSetting(
        title = title,
        tip = tip,
        readout = formatDb(value),
        value = value.toFloat(),
        range = toneRange,
        default = 0f,
        origin = 0f,
        onChange = { onChange(snapTone(it)) },
        onFinished = Repo::commit,
        resetTo = saved.toFloat(),
    )
}

@Composable
private fun SpaceSection(state: AppState, saved: Tone?) {
    val status by Repo.status.collectAsStateWithLifecycle()
    val systemSpatialAudio = rememberSystemSpatialAudio()
    val spaceParts = buildList {
        val surround = state.current.tone.surround
        if (surround > 0.0) {
            add(stringResource(if (state.useSystemSurround) R.string.help_surround_title else R.string.help_stereo_width_title) + " " + formatPercent(surround))
        }
        if (state.current.tone.reverb != ReverbRoom.OFF) add(stringResource(R.string.ui_reverb) + " " + stringResource(reverbName(state.current.tone.reverb)).lowercase())
    }
    SettingsSection(stringResource(R.string.ui_space), summary = foldSummary(spaceParts)) {
        SliderSetting(
            title = if (state.useSystemSurround) stringResource(R.string.help_surround_title) else stringResource(R.string.help_stereo_width_title),
            tip = if (state.useSystemSurround) Help.surround else Help.stereoWidth,
            readout = if (state.current.tone.surround <= 0.0) stringResource(R.string.ui_mode_off) else formatPercent(state.current.tone.surround),
            value = state.current.tone.surround.toFloat(),
            range = 0f..1f,
            default = 0f,
            detail = when {
                state.useSystemSurround && status.spatialError != null -> status.spatialError
                state.current.tone.surround > 0.0 && systemSpatialAudio ->
                    stringResource(R.string.ui_android_s_own_spatial_audio_is)
                state.useSystemSurround -> stringResource(R.string.ui_surround_from_settings)
                else -> null
            },
            onChange = { v -> Repo.editTone { it.copy(surround = if (v < 0.02f) 0.0 else v.toDouble()) } },
            onFinished = Repo::commit,
            resetTo = (saved?.surround ?: 0.0).toFloat(),
        )
        RowDivider()
        ReverbRow(state, status, saved)
    }
}

/** Stereo settings, which belong to the phone rather than to a preset. */
@Composable
private fun StereoSection(state: AppState) {
    val parts = buildList {
        if (state.useSystemSurround) add(stringResource(R.string.ui_virtual_surround_short))
        if (kotlin.math.abs(state.balance) > 0.005) add(stringResource(R.string.ui_balance))
    }
    SettingsSection(stringResource(R.string.ui_stereo), summary = foldSummary(parts)) {
        SwitchSetting(stringResource(R.string.ui_use_the_phone_s_virtual_surround), Help.systemSurround, state.useSystemSurround) { on ->
            Repo.update { it.copy(useSystemSurround = on) }
        }
        RowDivider()
        SliderSetting(
            title = stringResource(R.string.ui_balance),
            tip = Help.balance,
            readout = when {
                state.balance < -0.005 -> stringResource(R.string.ui_balance_left, formatPercent(-state.balance))
                state.balance > 0.005 -> stringResource(R.string.ui_balance_right, formatPercent(state.balance))
                else -> stringResource(R.string.ui_centre)
            },
            value = state.balance.toFloat(),
            range = -1f..1f,
            default = 0f,
            origin = 0f,
            onChange = { v -> Repo.update { it.copy(balance = v.toDouble()) } },
        )
    }
}

/** Says which apps it really reaches, because Android only allows a reverb on apps that announce their audio. */
@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun ReverbRow(state: AppState, status: EngineStatus, saved: Tone?) {
    val context = LocalContext.current
    val chosen = state.current.tone.reverb
    Column(Modifier.padding(start = 16.dp, end = 16.dp, top = 4.dp, bottom = 12.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(stringResource(R.string.ui_reverb), style = MaterialTheme.typography.bodyLarge, color = Palette.Text)
            InfoButton(Help.reverb)
        }
        FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            ReverbRoom.entries.forEach { room ->
                FilterChip(
                    selected = chosen == room,
                    onClick = {
                        Repo.editTone { it.copy(reverb = room) }
                        Repo.commit()
                    },
                    label = { Text(stringResource(reverbName(room)), maxLines = 1) },
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = Palette.LitFill,
                        selectedLabelColor = Palette.OnLit,
                        labelColor = Palette.Text,
                    ),
                )
            }
        }
        fun names(packages: List<String>) = packages.joinToString { appLabel(context, it) }
        Text(
            when {
                !state.enabled -> stringResource(R.string.ui_reverb_detail_off)
                status.reverbApps.isNotEmpty() -> stringResource(R.string.ui_reverb_reaching, names(status.reverbApps))
                chosen != ReverbRoom.OFF -> stringResource(R.string.ui_reverb_waiting)
                status.announcedApps.isNotEmpty() -> stringResource(R.string.ui_reverb_can_reach, names(status.announcedApps))
                else -> stringResource(R.string.ui_reverb_detail_off)
            },
            style = MaterialTheme.typography.bodySmall,
            color = Palette.TextDim,
            modifier = Modifier.padding(top = 8.dp),
        )
    }
    // Inset like every other slider, rather than twice over inside the chips' column.
    if (chosen != ReverbRoom.OFF) {
        RowDivider()
        SliderSetting(
            title = stringResource(R.string.ui_reverb_amount),
            tip = Help.reverbAmount,
            readout = formatPercent(state.current.tone.reverbAmount),
            value = state.current.tone.reverbAmount.toFloat(),
            range = 0f..1f,
            default = 0.6f,
            onChange = { v -> Repo.editTone { it.copy(reverbAmount = v.toDouble()) } },
            onFinished = Repo::commit,
            resetTo = (saved?.takeIf { it.reverb != ReverbRoom.OFF }?.reverbAmount ?: 0.6).toFloat(),
        )
    }
}

private fun reverbName(room: ReverbRoom): Int = when (room) {
    ReverbRoom.OFF -> R.string.ui_mode_off
    ReverbRoom.SMALL_ROOM -> R.string.ui_reverb_small_room
    ReverbRoom.MEDIUM_ROOM -> R.string.ui_reverb_medium_room
    ReverbRoom.LARGE_ROOM -> R.string.ui_reverb_large_room
    ReverbRoom.MEDIUM_HALL -> R.string.ui_reverb_medium_hall
    ReverbRoom.LARGE_HALL -> R.string.ui_reverb_large_hall
    ReverbRoom.PLATE -> R.string.ui_reverb_plate
}

/** This output's correction, or the offer to make one from its latest hearing check. */
@Composable
private fun EarCorrectionRows(state: AppState) {
    val output by Repo.output.collectAsStateWithLifecycle()
    val key = output.key
    val ears = state.earsFor(key)
    val record = state.hearings[key]
    val measured = remember(record) { record?.result() }
    fun setEars(new: io.github.dragjon.eigentone.data.EarCorrection?) = Repo.update {
        it.copy(earCorrections = if (new == null) it.earCorrections - key else it.earCorrections + (key to new))
    }
    val snackbar = LocalSnackbar.current
    val snackScope = LocalSnackScope.current
    val undoLabel = stringResource(R.string.ui_undo)

    /** Replacing or removing a correction acts at once, so it can be taken back for a moment. */
    fun replaceEars(new: io.github.dragjon.eigentone.data.EarCorrection?, message: String) {
        val old = state.earCorrections[key]
        setEars(new)
        snackScope.launch {
            val result = snackbar.showSnackbar(message, actionLabel = undoLabel, withDismissAction = true, duration = SnackbarDuration.Short)
            if (result == SnackbarResult.ActionPerformed) setEars(old)
        }
    }

    if (Profiles.supportsModes(key)) {
        if (ears != null) {
            RowDivider()
            SwitchSetting(stringResource(R.string.ui_ear_correction), Help.earCorrection, ears.enabled, detail = stringResource(R.string.ui_a_separate_eq_for_each_ear, output.label)) { on ->
                setEars(ears.copy(enabled = on))
            }
            EarCurvesGraph(ears, Modifier.padding(horizontal = 16.dp, vertical = 8.dp), markers = true)
            // The latest check is always shown, so a new one replaces the old graph even with a correction in use.
            if (measured != null && record != null) {
                val latest = remember(measured) { measured.earCorrection() }
                val date = java.text.DateFormat.getDateInstance(java.text.DateFormat.MEDIUM).format(java.util.Date(record.measuredAt))
                RowDivider()
                HearingGraph(measured, Modifier.padding(16.dp))
                if (latest.hasChanges && (latest.left != ears.left || latest.right != ears.right)) {
                    RowDivider()
                    ActionRow(
                        stringResource(R.string.ui_use_latest_check),
                        Help.earCorrection,
                        detail = stringResource(R.string.ui_correction_from_check_on, date),
                        opens = false,
                    ) { replaceEars(latest.copy(enabled = ears.enabled), appText(R.string.ui_ear_correction_updated)) }
                }
            }
            RowDivider()
            ActionRow(stringResource(R.string.ui_remove_ear_correction), Help.earCorrection, opens = false, danger = true) {
                replaceEars(null, appText(R.string.ui_ear_correction_removed))
            }
        } else if (measured != null && record != null) {
            // Offered until it's used, so leaving the hearing check without choosing doesn't lose it.
            val correction = remember(measured) { measured.earCorrection() }
            val recommended = measured.earCorrectionIsRecommended()
            val date = java.text.DateFormat.getDateInstance(java.text.DateFormat.MEDIUM).format(java.util.Date(record.measuredAt))
            RowDivider()
            HearingGraph(measured, Modifier.padding(16.dp))
            RowDivider()
            if (correction.hasChanges) {
                ActionRow(
                    if (recommended) stringResource(R.string.ui_correct_each_ear) else stringResource(R.string.ui_correct_each_ear_anyway),
                    Help.earCorrection,
                    detail = if (recommended) {
                        stringResource(R.string.ui_for_from_your_hearing_check_on, output.label, date)
                    } else {
                        stringResource(R.string.ui_for_from_your_check_on, output.label, date, earsCloseNote(measured))
                    },
                    opens = false,
                ) { setEars(correction) }
            } else {
                Text(
                    earsCloseNote(measured),
                    style = MaterialTheme.typography.bodyMedium,
                    color = Palette.TextDim,
                    modifier = Modifier.padding(16.dp),
                )
            }
        }
    }
    val others = (state.earCorrections.keys - key).map { state.hearings[it]?.headphones ?: it.substringAfter(':') }
    if (others.isNotEmpty()) {
        RowDivider()
        Text(
            stringResource(R.string.ui_ear_correction_is_also_saved_for, others.joinToString()),
            style = MaterialTheme.typography.bodySmall,
            color = Palette.TextDim,
            modifier = Modifier.padding(16.dp),
        )
    }
}

@Composable
private fun DynamicsSection(state: AppState, attenuation: Double) {
    val scheduledNight by Repo.scheduledNight.collectAsStateWithLifecycle()
    val volumeParts = buildList {
        if (state.loudness) add(stringResource(R.string.ui_adapt_to_volume))
        if (state.nightMode > 0.001) add(stringResource(R.string.ui_night_mode) + " " + formatPercent(state.nightMode))
        if (state.leveller > 0.001) add(stringResource(R.string.ui_volume_leveller) + " " + formatPercent(state.leveller))
        if (state.nightSchedule.enabled) add(stringResource(R.string.ui_night_mode_schedule))
    }
    SettingsSection(stringResource(R.string.ui_dynamics), summary = foldSummary(volumeParts)) {
        SwitchSetting(
            stringResource(R.string.ui_adapt_to_volume),
            Help.loudness,
            state.loudness,
            detail = if (state.loudness) stringResource(R.string.ui_loudness_tuned_per_output) else null,
        ) { on ->
            Repo.update { it.copy(loudness = on) }
        }
        RowDivider()
        SliderSetting(
            title = stringResource(R.string.ui_night_mode),
            tip = Help.nightMode,
            readout = if (state.nightMode <= 0.001) stringResource(R.string.ui_mode_off) else formatPercent(state.nightMode),
            value = state.nightMode.toFloat(),
            range = 0f..1f,
            default = 0f,
            onChange = { v -> Repo.update { it.copy(nightMode = v.toDouble()) } },
        )
        RowDivider()
        SliderSetting(
            title = stringResource(R.string.ui_volume_leveller),
            tip = Help.leveller,
            readout = if (state.leveller <= 0.001) stringResource(R.string.ui_mode_off) else formatPercent(state.leveller),
            value = state.leveller.toFloat(),
            range = 0f..1f,
            default = 0f,
            detail = when {
                state.leveller <= 0.001 -> null
                state.nightMode > 0.001 || scheduledNight -> stringResource(R.string.ui_night_mode_is_on_so_it)
                state.current.tone.compressor.enabled -> stringResource(R.string.ui_this_preset_s_compressor_is_on)
                else -> null
            },
            onChange = { v -> Repo.update { it.copy(leveller = if (v < 0.02f) 0.0 else v.toDouble()) } },
        )
        RowDivider()
        val schedule = state.nightSchedule
        fun updateSchedule(change: (io.github.dragjon.eigentone.data.NightSchedule) -> io.github.dragjon.eigentone.data.NightSchedule) =
            Repo.update { it.copy(nightSchedule = change(it.nightSchedule)) }
        SwitchSetting(stringResource(R.string.ui_night_mode_schedule), Help.nightSchedule, schedule.enabled) { on -> updateSchedule { it.copy(enabled = on) } }
        if (schedule.enabled) {
            TimeSetting(stringResource(R.string.help_night_schedule_start_title), Help.nightScheduleStart, schedule.startMinute) { m -> updateSchedule { it.copy(startMinute = m) } }
            TimeSetting(stringResource(R.string.help_night_schedule_end_title), Help.nightScheduleEnd, schedule.endMinute) { m -> updateSchedule { it.copy(endMinute = m) } }
            if (schedule.startMinute == schedule.endMinute) {
                Text(
                    stringResource(R.string.ui_the_start_and_end_are_the_same_so_night),
                    style = MaterialTheme.typography.bodySmall,
                    color = Palette.Danger,
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp),
                )
            }
            SliderSetting(
                title = stringResource(R.string.ui_scheduled_strength),
                tip = Help.nightScheduleAmount,
                readout = formatPercent(schedule.amount),
                value = schedule.amount.toFloat(),
                range = 0.1f..1f,
                default = 0.6f,
                onChange = { v -> updateSchedule { it.copy(amount = v.toDouble()) } },
            )
        }
    }
}

/**
 * Equal loudness: what's being added right now, drawn live, and the usual volume it works from, which each
 * pair of headphones keeps separately because they play at different levels.
 */
@Composable
private fun EqualLoudnessRows(state: AppState, attenuation: Double) {
    val output by Repo.output.collectAsStateWithLifecycle()
    val usual = state.usualVolumeDb(output.key)
    val quieter = EqualLoudness.quieter(attenuation, usual)
    val strength = state.loudnessStrengthFor(output.key)
    val added = remember(quieter, strength) { EqualLoudness.correction(quieter, strength) }
    fun at(hz: Double) = if (added.isEmpty()) 0.0 else GraphicEq.interpolate(added.map { it.hz to it.db }, hz)
    fun setUsual(db: Double) = Repo.update { it.copy(loudnessUsual = it.loudnessUsual + (output.key to db.coerceIn(0.0, USUAL_MAX_DB))) }

    LoudnessGraph(added, Modifier.padding(start = 16.dp, end = 16.dp, top = 4.dp, bottom = 4.dp))
    Text(
        if (added.isEmpty()) {
            stringResource(R.string.ui_loudness_none)
        } else {
            stringResource(R.string.ui_loudness_now, formatAmount(quieter), formatDb(at(LOUDNESS_LOW_HZ)), formatDb(at(LOUDNESS_HIGH_HZ)))
        },
        style = MaterialTheme.typography.bodySmall,
        color = if (added.isEmpty()) Palette.TextDim else Palette.Text,
        modifier = Modifier.padding(horizontal = 16.dp),
    )
    RowDivider()
    SliderSetting(
        title = stringResource(R.string.ui_usual_volume),
        tip = Help.usualVolume,
        readout = stringResource(R.string.ui_db_below_full, formatAmount(usual)),
        value = usual.toFloat(),
        range = 0f..USUAL_MAX_DB.toFloat(),
        default = EqualLoudness.DEFAULT_USUAL_DB.toFloat(),
        detail = stringResource(R.string.ui_usual_volume_for, output.label, formatAmount(attenuation)),
        onChange = { v -> setUsual(kotlin.math.round(v * 2.0) / 2.0) },
    )
    // A row like the others, and only usable when it would change something.
    RowDivider()
    ActionRow(
        stringResource(R.string.ui_use_current_volume),
        Help.usualVolume,
        detail = stringResource(R.string.ui_db_below_full, formatAmount(attenuation)),
        enabled = kotlin.math.abs(usual - attenuation) > 0.25,
        opens = false,
    ) { setUsual(kotlin.math.round(attenuation * 2.0) / 2.0) }
    RowDivider()
    SliderSetting(
        title = stringResource(R.string.ui_strength),
        tip = Help.loudnessStrength,
        readout = formatPercent(strength),
        value = strength.toFloat(),
        range = 0f..1f,
        default = 1f,
        detail = stringResource(R.string.ui_strength_for, output.label),
        onChange = { v -> Repo.update { it.copy(loudnessStrengths = it.loudnessStrengths + (output.key to kotlin.math.round(v * 20.0) / 20.0)) } },
    )
}

/** The correction being added right now, from 20 Hz to 20 kHz, on a 0 to 15 dB scale. */
@Composable
private fun LoudnessGraph(points: List<io.github.dragjon.eigentone.data.ShapePoint>, modifier: Modifier) {
    val measurer = androidx.compose.ui.text.rememberTextMeasurer()
    val labelStyle = androidx.compose.ui.text.TextStyle(color = Palette.TextDim, fontSize = androidx.compose.ui.unit.TextUnit(10f, androidx.compose.ui.unit.TextUnitType.Sp), fontFamily = ReadoutFamily)
    androidx.compose.foundation.Canvas(
        modifier
            .fillMaxWidth()
            .height(96.dp)
            .clip(PanelShape)
            .background(Palette.SurfaceHigh)
            .semantics { contentDescription = appText(R.string.ui_loudness_graph) },
    ) {
        val pad = 6.dp.toPx()
        val h = size.height - pad * 2
        fun x(hz: Double) = (Eq.logPosition(hz) * size.width).toFloat()
        fun y(db: Double) = (pad + (1 - (db + LOUDNESS_GRAPH_FLOOR) / (EqualLoudness.MAX_BOOST_DB + LOUDNESS_GRAPH_FLOOR)) * h).toFloat()
        for (hz in listOf(100.0, 1000.0, 10000.0)) {
            drawLine(Palette.Line, androidx.compose.ui.geometry.Offset(x(hz), 0f), androidx.compose.ui.geometry.Offset(x(hz), size.height), strokeWidth = 1f)
        }
        drawLine(Palette.Outline, androidx.compose.ui.geometry.Offset(0f, y(0.0)), androidx.compose.ui.geometry.Offset(size.width, y(0.0)), strokeWidth = 1f)
        val curve = androidx.compose.ui.graphics.Path()
        val samples = 64
        for (i in 0..samples) {
            val hz = Eq.logFreq(i.toDouble() / samples)
            val db = if (points.isEmpty()) 0.0 else GraphicEq.interpolate(points.map { it.hz to it.db }, hz)
            if (i == 0) curve.moveTo(x(hz), y(db)) else curve.lineTo(x(hz), y(db))
        }
        drawPath(curve, if (points.isEmpty()) Palette.TextDim else Palette.Lit, style = androidx.compose.ui.graphics.drawscope.Stroke(width = 2.dp.toPx()))
        val top = measurer.measure("+${EqualLoudness.MAX_BOOST_DB.toInt()}", labelStyle)
        drawText(top, topLeft = androidx.compose.ui.geometry.Offset(4.dp.toPx(), y(EqualLoudness.MAX_BOOST_DB)))
        val zero = measurer.measure("0", labelStyle)
        drawText(zero, topLeft = androidx.compose.ui.geometry.Offset(4.dp.toPx(), y(0.0) - zero.size.height))
    }
}

private const val USUAL_MAX_DB = 45.0
private const val LOUDNESS_LOW_HZ = 50.0
private const val LOUDNESS_HIGH_HZ = 10000.0
private const val LOUDNESS_GRAPH_FLOOR = 3.0

private fun formatAmount(db: Double): String = String.format(Locale.US, "%.0f", db)

/** One-tap limiter settings. Each is a designed starting point, adjustable afterwards. */
private enum class LimiterStyle(val label: Int, val settings: LimiterSettings) {
    SAFE(R.string.ui_limiter_safe, LimiterSettings()),
    TRANSPARENT(R.string.ui_limiter_transparent, LimiterSettings(attackMs = 5.0, releaseMs = 250.0, ratio = 10.0, thresholdDb = -1.0)),
    BRICKWALL(R.string.ui_limiter_brickwall, LimiterSettings(attackMs = 0.5, releaseMs = 40.0, ratio = 50.0, thresholdDb = -1.0)),
    LOUD(R.string.ui_limiter_loud, LimiterSettings(attackMs = 1.0, releaseMs = 80.0, ratio = 20.0, thresholdDb = -6.0, postGainDb = 5.0)),
}

private fun AppState.withLimiter(l: LimiterSettings) = copy(
    limiterAttackMs = l.attackMs,
    limiterReleaseMs = l.releaseMs,
    limiterRatio = l.ratio,
    limiterThresholdDb = l.thresholdDb,
    limiterPostGainDb = l.postGainDb,
)

/** Styles, and the timing and gain controls tucked away until wanted. */
@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun LimiterDetails(state: AppState) {
    val current = state.limiterSettings()
    var open by rememberSaveable { mutableStateOf(false) }
    FlowRow(
        Modifier.padding(horizontal = 16.dp, vertical = 4.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        LimiterStyle.entries.forEach { style ->
            FilterChip(
                selected = current == style.settings.clamped(),
                onClick = { Repo.update { it.withLimiter(style.settings) } },
                label = { Text(stringResource(style.label), maxLines = 1) },
                colors = FilterChipDefaults.filterChipColors(
                    selectedContainerColor = Palette.LitFill,
                    selectedLabelColor = Palette.OnLit,
                    labelColor = Palette.Text,
                ),
            )
        }
    }
    Text(
        stringResource(R.string.ui_limiter_ceiling, formatDb(current.ceilingDb)),
        style = MaterialTheme.typography.bodySmall,
        color = Palette.TextDim,
        modifier = Modifier.padding(horizontal = 16.dp),
    )
    DisclosureRow(stringResource(R.string.ui_limiter_more), open) { open = !open }
    if (!open) return
    SliderSetting(
        title = stringResource(R.string.ui_limiter_attack),
        tip = Help.limiterAttack,
        readout = String.format(Locale.US, "%.1f ms", current.attackMs),
        value = current.attackMs.toFloat(),
        range = 0.1f..50f,
        default = 1f,
        onChange = { v -> Repo.update { it.copy(limiterAttackMs = kotlin.math.round(v * 10.0) / 10.0) } },
    )
    SliderSetting(
        title = stringResource(R.string.ui_limiter_release),
        tip = Help.limiterRelease,
        readout = String.format(Locale.US, "%.0f ms", current.releaseMs),
        value = current.releaseMs.toFloat(),
        range = 10f..1000f,
        default = 60f,
        onChange = { v -> Repo.update { it.copy(limiterReleaseMs = kotlin.math.round(v).toDouble()) } },
    )
    SliderSetting(
        title = stringResource(R.string.ui_limiter_ratio),
        tip = Help.limiterRatio,
        readout = String.format(Locale.US, "%.0f : 1", current.ratio),
        value = current.ratio.toFloat(),
        range = 1f..50f,
        default = 10f,
        onChange = { v -> Repo.update { it.copy(limiterRatio = kotlin.math.round(v).toDouble()) } },
    )
    SliderSetting(
        title = stringResource(R.string.ui_limiter_post_gain),
        tip = Help.limiterPostGain,
        readout = formatDb(current.postGainDb),
        value = current.postGainDb.toFloat(),
        range = 0f..12f,
        default = 0f,
        detail = if (state.limiterPostGainDb > current.postGainDb + 0.01) stringResource(R.string.ui_limiter_post_gain_capped) else null,
        onChange = { v -> Repo.update { it.copy(limiterPostGainDb = snapTone(v)) } },
    )
}

/** Opens a clock, which follows the phone's 12 or 24 hour setting. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun TimeSetting(title: String, tip: Tip, minute: Int, onChange: (Int) -> Unit) {
    val context = LocalContext.current
    val is24Hour = android.text.format.DateFormat.is24HourFormat(context)
    var picking by remember { mutableStateOf(false) }
    ActionRow(title, tip, detail = NightSchedule.formatTime(minute, is24Hour)) { picking = true }
    if (picking) {
        val clock = rememberTimePickerState(initialHour = minute / 60, initialMinute = minute % 60, is24Hour = is24Hour)
        MotionDialog(
            onDismissRequest = { picking = false },
            containerColor = Palette.Tooltip,
            title = { Text(title) },
            text = { TimePicker(state = clock) },
            confirmButton = {
                TextButton(onClick = {
                    onChange(clock.hour * 60 + clock.minute)
                    picking = false
                }) { Text(stringResource(R.string.ui_set)) }
            },
            dismissButton = { TextButton(onClick = { picking = false }) { Text(stringResource(R.string.ui_cancel)) } },
        )
    }
}

/** Says when these settings could push peaks over, which they can't do while automatic headroom has room. */
@Composable
private fun ClipWarning(state: AppState, attenuation: Double) {
    val output by Repo.output.collectAsStateWithLifecycle()
    val scheduledNight by Repo.scheduledNight.collectAsStateWithLifecycle()
    val estimate = remember(state, attenuation, output.key, scheduledNight) {
        Processing.peakEstimate(state, attenuation, output.key, scheduledNight)
    }
    if (estimate.risk == ClipRisk.NONE) return
    val peak = formatDb(estimate.peakDb)
    Text(
        if (estimate.risk == ClipRisk.CLIPPING) {
            stringResource(R.string.ui_loud_music_could_reach_which_can, peak)
        } else {
            stringResource(R.string.ui_loud_music_could_reach_the_limiter, peak)
        },
        style = MaterialTheme.typography.bodySmall,
        color = if (estimate.risk == ClipRisk.CLIPPING) Palette.Danger else Palette.TextDim,
        modifier = Modifier.padding(start = 16.dp, end = 16.dp, top = 8.dp),
    )
}

@Composable
private fun LevelSection(state: AppState, attenuation: Double) {
    val effective = Processing.effectiveFilters(state, attenuation, Repo.output.value.key)
    val input = Processing.inputOf(state.current, effective, state.autoHeadroom)
    val levelParts = buildList {
        if (!state.autoHeadroom) add(stringResource(R.string.ui_automatic_headroom) + " " + stringResource(R.string.ui_mode_off).lowercase())
        add(
            if (state.limiter) stringResource(R.string.ui_limiter) + " " + formatDb(state.limiterSettings().ceilingDb)
            else stringResource(R.string.ui_limiter) + " " + stringResource(R.string.ui_mode_off).lowercase(),
        )
    }
    SettingsSection(stringResource(R.string.ui_level), summary = foldSummary(levelParts)) {
        SwitchSetting(
            stringResource(R.string.ui_automatic_headroom),
            Help.headroom,
            state.autoHeadroom,
            detail = stringResource(R.string.ui_level_going_into_the_eq, formatDb(input)),
        ) { on -> Repo.update { it.copy(autoHeadroom = on) } }
        RowDivider()
        ClipWarning(state, attenuation)
        SwitchSetting(stringResource(R.string.ui_limiter), Help.limiter, state.limiter) { on -> Repo.update { it.copy(limiter = on) } }
        if (state.limiter) {
            SliderSetting(
                title = stringResource(R.string.ui_limiter_threshold),
                tip = Help.limiterThreshold,
                readout = formatDb(state.limiterThresholdDb),
                value = state.limiterThresholdDb.toFloat(),
                range = -12f..0f,
                default = -1f,
                origin = 0f,
                onChange = { v -> Repo.update { it.copy(limiterThresholdDb = snapTone(v)) } },
            )
            if (Disclosure.limiterDetail(state)) LimiterDetails(state)
        }
        if (Disclosure.matchBypass(state)) {
            RowDivider()
            SwitchSetting(stringResource(R.string.ui_level_matched_bypass), Help.matchBypass, state.matchBypass) { on ->
                Repo.update { it.copy(matchBypass = on) }
            }
        }
    }
}

/** Shows a message through the screen's snackbar. */
/** Why something failed, in words, never "null" when an error carries no message. */
private fun reason(error: Throwable?): String = error?.message ?: error?.javaClass?.simpleName ?: "unknown error"

/** Opens another app's screen, without crashing when no app on this phone can handle it. */
private fun android.content.Context.openSafely(intent: Intent): Boolean =
    runCatching { startActivity(intent) }.isSuccess

private fun CoroutineScope.notify(snackbar: SnackbarHostState, message: String) {
    launch { snackbar.showSnackbar(message, withDismissAction = true, duration = SnackbarDuration.Short) }
}

/**
 * Everything about what's connected: its correction, the preset it switches to, your ears, and how it adapts
 * to quiet listening. Each output keeps its own, so this page follows whatever is plugged in.
 */
@Composable
private fun HeadphonesTab(state: AppState) {
    val scope = LocalSnackScope.current
    val snackbar = LocalSnackbar.current
    val output by Repo.output.collectAsStateWithLifecycle()
    val attenuation by Repo.attenuationDb.collectAsStateWithLifecycle()
    var finding by remember { mutableStateOf(false) }
    var tuning by remember { mutableStateOf(false) }
    var measuringRoom by remember { mutableStateOf(false) }
    val preset = state.current
    val saved = savedPreset(state)?.tone

    val kind = output.kind
    val hasCorrection = preset.filters.any { it.gainDb != 0.0 || !it.type.hasGain } || preset.tone.importedCurve.isNotEmpty()

    Row(Modifier.padding(horizontal = 4.dp), verticalAlignment = Alignment.CenterVertically) {
        Icon(painterResource(kind.icon()), contentDescription = null, tint = Palette.Lit, modifier = Modifier.padding(end = 12.dp).size(28.dp))
        Column {
            Text(output.label, style = MaterialTheme.typography.titleLarge, color = Palette.Text, maxLines = 2, overflow = TextOverflow.Ellipsis)
            Text(
                stringResource(
                    when (kind) {
                        OutputKind.PHONE -> R.string.ui_device_scope_phone
                        OutputKind.SPEAKER -> R.string.ui_device_scope_speaker
                        OutputKind.CAR -> R.string.ui_device_scope_car
                        else -> R.string.ui_hp_scope
                    },
                ),
                style = MaterialTheme.typography.bodySmall,
                color = Palette.TextDim,
            )
        }
    }

    // Headphone corrections are measured on heads, so finding one only makes sense for something worn.
    // Anything else still shows the strength of a correction its preset already has.
    // A speaker or a car can be measured where you listen. The phone's own speaker can't, since its
    // microphone sits right beside it and would hear the phone rather than the room.
    val canMeasure = kind == OutputKind.SPEAKER || kind == OutputKind.CAR
    if (kind.isWorn || hasCorrection || canMeasure) SettingsSection(stringResource(R.string.ui_hp_correction)) {
        if (kind.isWorn) {
            ActionRow(stringResource(if (kind == OutputKind.EARBUDS) R.string.ui_find_earbuds else R.string.ui_find_headphones), Help.find) { finding = true }
        }
        if (canMeasure) {
            ActionRow(stringResource(R.string.ui_measure_action), Help.measure, detail = stringResource(R.string.ui_measure_action_detail)) { measuringRoom = true }
        }
        if (hasCorrection) {
            if (kind.isWorn || canMeasure) RowDivider()
            SliderSetting(
                title = stringResource(if (kind.isWorn) R.string.ui_headphone_correction else R.string.ui_hp_correction),
                tip = Help.correction,
                readout = if (preset.tone.correction <= 0.0) stringResource(R.string.ui_mode_off) else formatPercent(preset.tone.correction),
                value = preset.tone.correction.toFloat(),
                range = 0f..1f,
                default = 1f,
                detail = stringResource(R.string.ui_saved_with, preset.name),
                onChange = { v -> Repo.editTone { it.copy(correction = kotlin.math.round(v * 20.0) / 20.0) } },
                onFinished = Repo::commit,
                resetTo = (saved?.correction ?: 1.0).toFloat(),
            )
        }
        if (preset.filters.any { it.gainDb != 0.0 } || preset.tone.importedCurve.isNotEmpty()) {
            RowDivider()
            ActionRow(stringResource(R.string.ui_tune_correction), Help.tuner, detail = stringResource(R.string.ui_use_less_of_it_or_add_bass_or_treble_bac)) { tuning = true }
        }
    }

    OutputSection(state, output.key, output.label)

    SettingsSection(stringResource(R.string.ui_your_ears)) {
        if (kind.isWorn) {
            ActionRow(stringResource(R.string.ui_hearing_check), Help.hearing, detail = stringResource(R.string.ui_builds_a_curve_for_how_you_hear), onClick = LocalOpenHearing.current)
            EarCorrectionRows(state)
        } else {
            // A speaker reaches both ears at once, and the room changes what each one hears.
            Text(
                stringResource(R.string.ui_ears_need_headphones, output.label),
                style = MaterialTheme.typography.bodyMedium,
                color = Palette.TextDim,
                modifier = Modifier.padding(16.dp),
            )
        }
    }

    SettingsSection(stringResource(R.string.ui_quiet_listening)) {
        if (state.loudness) {
            EqualLoudnessRows(state, attenuation)
        } else {
            // The switch itself, rather than directions to it in a folded section of Settings.
            SwitchSetting(
                stringResource(R.string.ui_adapt_to_volume),
                Help.loudness,
                false,
                detail = stringResource(R.string.ui_quiet_turn_on, output.label),
            ) { on -> Repo.update { it.copy(loudness = on) } }
        }
    }

    SettingsSection(stringResource(R.string.ui_listening_tools)) {
        ActionRow(stringResource(R.string.ui_find_my_sound), Help.finder, detail = stringResource(R.string.ui_a_short_guided_listening_test), onClick = LocalOpenFinder.current)
        RowDivider()
        ActionRow(stringResource(R.string.ui_refine), Help.refine, detail = stringResource(R.string.ui_refine_detail), onClick = LocalOpenRefine.current)
        // Both play tones into each ear and ask what you hear, so they're for headphones and earbuds.
        // A speaker's peaks are mostly the room's, which the microphone measurement above handles.
        if (kind.isWorn && Disclosure.measureTools(state)) {
            RowDivider()
            ActionRow(stringResource(R.string.ui_find_peaks_for_me), Help.peakFinder, detail = stringResource(R.string.ui_a_guided_test_that_finds_peaks_and_dips), onClick = LocalOpenPeaks.current)
            RowDivider()
            ActionRow(stringResource(R.string.ui_frequency_sweep), Help.sweep, detail = stringResource(R.string.ui_find_and_fix_peaks_in_your_headphones_by), onClick = LocalOpenSweep.current)
        }
    }

    if (finding) {
        AutoEqDialog(onDismiss = { finding = false }) { result ->
            scope.notify(snackbar, result)
            finding = false
            // A correction is the moment people find it too neutral, so offer this straight away, once the
            // search has finished closing rather than over it.
            scope.launch {
                kotlinx.coroutines.delay(DIALOG_GAP_MS)
                tuning = true
            }
        }
    }
    if (tuning) CorrectionTuner(onDismiss = { tuning = false })
    if (measuringRoom) {
        RoomMeasureDialog(output, onDismiss = { measuringRoom = false }) { name ->
            measuringRoom = false
            scope.notify(snackbar, appText(R.string.ui_added, name))
        }
    }
}

@Composable
private fun PresetsTab(state: AppState) {
    PresetListSection(state)
    if (Disclosure.perApp(state)) PerAppSection(state)
    ManageSection(state)
}

@Composable
private fun OutputSection(state: AppState, outputKey: String, outputLabel: String) {
    val supportsModes = Profiles.supportsModes(outputKey)
    val assignKey = if (supportsModes) Profiles.modeKey(outputKey, state.listeningMode) else outputKey
    val assigned = Profiles.presetIdFor(state, outputKey)?.let(Repo::findPreset)
    val correction = BuiltIns.correctionFor(outputKey)
    val canAssign = Repo.findPreset(state.current.id) != null
    val alreadyAssigned = assigned?.id == state.current.id

    SettingsSection(stringResource(R.string.ui_hp_preset)) {
        Column(Modifier.padding(start = 16.dp, end = 16.dp, top = 4.dp, bottom = 12.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(stringResource(R.string.ui_assigned_preset), style = MaterialTheme.typography.bodyLarge, color = Palette.Text)
                InfoButton(Help.outputPreset)
            }
            Text(
                assigned?.name ?: stringResource(R.string.ui_none),
                style = MaterialTheme.typography.bodyMedium,
                color = if (assigned != null) Palette.Lit else Palette.TextDim,
                modifier = Modifier.padding(bottom = 10.dp),
            )
            if (assigned == null && correction != null) {
                Button(
                    onClick = {
                        Repo.assignToOutput(outputKey, correction.id)
                        Repo.loadPreset(correction)
                    },
                    modifier = Modifier.padding(bottom = 8.dp),
                ) { Text(stringResource(R.string.ui_use_correction, correction.name)) }
            }
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                FilledTonalButton(
                    enabled = canAssign && !alreadyAssigned,
                    onClick = { Repo.assignToOutput(assignKey, state.current.id) },
                    modifier = Modifier.weight(1f, fill = false),
                ) {
                    Text(
                        if (alreadyAssigned) stringResource(R.string.ui_current_preset_assigned) else stringResource(R.string.ui_assign, state.current.name),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
                if (assigned != null) {
                    // Clears the mode's own choice first, then the output's main one.
                    val clearKey = if (assignKey in state.outputProfiles) assignKey else outputKey
                    TextButton(onClick = { Repo.assignToOutput(clearKey, null) }) { Text(stringResource(R.string.ui_clear)) }
                }
            }
        }
        if (supportsModes) {
            RowDivider()
            Column(Modifier.padding(start = 16.dp, end = 16.dp, top = 4.dp, bottom = 12.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(stringResource(R.string.ui_earbud_noise_mode), style = MaterialTheme.typography.bodyLarge, color = Palette.Text)
                    InfoButton(Help.noiseMode)
                }
                val modes = ListeningMode.entries
                WordSegments(
                    labels = modes.map { listeningModeLabel(it) },
                    selected = modes.indexOf(state.listeningMode),
                    onSelect = { Repo.setListeningMode(modes[it]) },
                )
            }
        }
        RowDivider()
        SwitchSetting(stringResource(R.string.ui_switch_automatically), Help.autoSwitch, state.autoSwitch) { on ->
            Repo.update { it.copy(autoSwitch = on) }
        }
    }
}

@Composable
fun segmentColors() = SegmentedButtonDefaults.colors(
    activeContainerColor = Palette.LitFill,
    activeContentColor = Palette.OnLit,
    activeBorderColor = Palette.LitFill,
    inactiveContainerColor = Palette.SurfaceHigh,
    inactiveContentColor = Palette.Text,
    inactiveBorderColor = Palette.Line,
)

@Composable
private fun PresetListSection(state: AppState) {
    val snackbar = LocalSnackbar.current
    val scope = LocalSnackScope.current
    val connected = rememberConnectedOutputNames()
    val editedIds by Repo.editedIds.collectAsStateWithLifecycle()
    var query by rememberSaveable { mutableStateOf("") }
    val allFolders = PresetOrganiser.folders(state.presets)
    fun shown(presets: List<Preset>) = presets.filter { PresetOrganiser.matches(it, query) }
    val (headphones, general) = shown(BuiltIns.availableFor(connected)).partition(BuiltIns::isForSpecificHeadphones)
    val own = shown(state.presets)

    // Only worth the space once there's a list to search.
    if (state.presets.size + general.size + headphones.size > SEARCH_FROM_PRESETS || query.isNotEmpty()) {
        OutlinedTextField(
            value = query,
            onValueChange = { query = it },
            singleLine = true,
            label = { Text(stringResource(R.string.ui_search_presets)) },
            leadingIcon = { Icon(Icons.Filled.Search, contentDescription = null) },
            colors = fieldColors(unfocusedBorder = Palette.Track),
            trailingIcon = if (query.isEmpty()) null else ({
                IconButton(onClick = { query = "" }) { Icon(Icons.Filled.Close, contentDescription = stringResource(R.string.ui_clear_search)) }
            }),
            modifier = Modifier.fillMaxWidth(),
        )
    }

    @Composable
    fun Group(title: String, presets: List<Preset>, deletable: Boolean, tip: Tip? = null) {
        if (presets.isEmpty()) return
        SettingsSection(title, trailing = tip?.let { { InfoButton(it) } }) {
            presets.forEachIndexed { i, preset ->
                if (i > 0) RowDivider()
                PresetRow(
                    preset = preset,
                    selected = preset.id == state.current.id,
                    // The list holds the saved version, so the one in use differs from it as soon as it's edited.
                    edited = preset.id in editedIds || (preset.id == state.current.id && preset != state.current),
                    favorite = preset.id in state.favorites,
                    folders = allFolders,
                    onDelete = if (deletable) {
                        {
                            val before = Repo.state.value
                            Repo.deletePreset(preset.id)
                            scope.launch {
                                val result = snackbar.showSnackbar(
                                    appText(R.string.ui_deleted, preset.name),
                                    actionLabel = appText(R.string.ui_undo),
                                    duration = SnackbarDuration.Short,
                                )
                                if (result == SnackbarResult.ActionPerformed) {
                                    Repo.update {
                                        it.copy(presets = before.presets, outputProfiles = before.outputProfiles)
                                    }
                                }
                            }
                        }
                    } else {
                        null
                    },
                )
            }
        }
    }

    Group(stringResource(R.string.ui_your_presets), own.filter { it.folder == null }, deletable = true, tip = Help.favorites)
    PresetOrganiser.folders(own).forEach { folder ->
        Group(folder, own.filter { it.folder == folder }, deletable = true)
    }
    Group(stringResource(R.string.ui_liberty_4_nc_correction), headphones, deletable = false, tip = if (state.presets.isEmpty()) Help.favorites else null)
    Group(stringResource(R.string.ui_general), general, deletable = false)
    if (query.isNotBlank() && own.isEmpty() && headphones.isEmpty() && general.isEmpty()) {
        Text(stringResource(R.string.ui_no_presets_match_that), style = MaterialTheme.typography.bodyMedium, color = Palette.TextDim)
    }
}

@Composable
private fun PresetRow(preset: Preset, selected: Boolean, edited: Boolean, favorite: Boolean, folders: List<String>, onDelete: (() -> Unit)?) {
    var renaming by remember { mutableStateOf(false) }
    if (renaming) {
        PresetDetailsDialog(preset, folders, onDismiss = { renaming = false })
    }
    Row(
        Modifier
            .fillMaxWidth()
            .heightIn(min = 56.dp)
            .clickable(role = Role.RadioButton) { Repo.loadPreset(preset) }
            .padding(start = 16.dp, end = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(Modifier.size(24.dp), contentAlignment = Alignment.CenterStart) {
            if (selected) {
                Icon(Icons.Filled.Check, contentDescription = stringResource(R.string.ui_selected), tint = Palette.Lit, modifier = Modifier.size(20.dp))
            }
        }
        // Padded so a second line keeps clear of the row's edges rather than pushing the name to the top.
        Column(Modifier.weight(1f).padding(vertical = 10.dp)) {
            Text(
                preset.name,
                style = MaterialTheme.typography.bodyLarge,
                color = if (selected) Palette.Lit else Palette.Text,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )
            // Edits from earlier that come back when this preset is chosen again.
            if (edited) {
                Text(
                    stringResource(R.string.ui_has_unsaved_edits),
                    style = MaterialTheme.typography.bodySmall,
                    color = Palette.TextDim,
                    modifier = Modifier.padding(top = 2.dp),
                )
            }
        }
        IconButton(onClick = { Repo.toggleFavorite(preset.id) }) {
            Icon(
                // Filled against outlined, so the state doesn't rely on colour alone.
                if (favorite) painterResource(R.drawable.ic_star) else painterResource(R.drawable.ic_star_border),
                contentDescription = if (favorite) stringResource(R.string.ui_remove_from_favourites, preset.name) else stringResource(R.string.ui_add_to_favourites, preset.name),
                tint = if (favorite) Palette.Lit else Palette.TextDim,
            )
        }
        if (onDelete != null) {
            IconButton(onClick = { renaming = true }) {
                Icon(painterResource(R.drawable.ic_edit), contentDescription = stringResource(R.string.ui_edit, preset.name), tint = Palette.TextDim)
            }
            IconButton(onClick = onDelete) {
                Icon(painterResource(R.drawable.ic_delete), contentDescription = stringResource(R.string.ui_delete, preset.name), tint = Palette.TextDim)
            }
        }
    }
}

@Composable
private fun ManageSection(state: AppState) {
    val context = LocalContext.current
    val scope = LocalSnackScope.current
    val snackbar = LocalSnackbar.current
    var saving by remember { mutableStateOf(false) }
    val saved = state.presets.firstOrNull { it.id == state.current.id }

    val importer = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri: Uri? ->
        if (uri == null) return@rememberLauncherForActivityResult
        scope.launch {
            val resolver = context.contentResolver
            val result = withContext(Dispatchers.IO) {
                runCatching {
                    val text = resolver.openInputStream(uri)!!.use { s ->
                        // Real EQ files are tiny. The limit protects against opening the wrong file.
                        s.readAtMost(MAX_IMPORT_BYTES).decodeToString()
                    }
                    val name = resolver.query(uri, arrayOf(OpenableColumns.DISPLAY_NAME), null, null, null)
                        ?.use { c -> if (c.moveToFirst()) c.getString(0) else null }
                    Triple(AutoEqParser.parse(text), ShapeText.parseGraphicEq(text), name)
                }
            }
            val (parsed, curve, name) = result.getOrElse {
                notify(snackbar, appText(R.string.ui_could_not_read_that_file, reason(it)))
                return@launch
            }
            val title = name?.substringBeforeLast('.')?.removeSuffix(" ParametricEQ")?.removeSuffix(" GraphicEQ")?.ifBlank { null }
            when {
                parsed != null -> {
                    Repo.importPreset(title ?: appText(R.string.ui_imported_eq), parsed)
                    notify(snackbar, appText(R.string.ui_imported_filters, parsed.filters.size))
                }
                curve != null -> {
                    Repo.importCurve(title ?: appText(R.string.ui_imported_eq), curve)
                    notify(snackbar, appText(R.string.ui_imported_a_curve_with_points, curve.size))
                }
                else -> notify(snackbar, appText(R.string.ui_no_eq_found_choose_an_autoeq))
            }
        }
    }
    val exporter = rememberLauncherForActivityResult(
        ActivityResultContracts.CreateDocument("application/json"),
    ) { uri: Uri? ->
        if (uri == null) return@rememberLauncherForActivityResult
        val text = BackupCodec.encode(Repo.state.value)
        scope.launch {
            val written = withContext(Dispatchers.IO) {
                runCatching { context.contentResolver.openOutputStream(uri)!!.use { it.write(text.encodeToByteArray()) } }
            }
            notify(snackbar, if (written.isSuccess) appText(R.string.ui_backup_saved) else appText(R.string.ui_could_not_save_backup, reason(written.exceptionOrNull())))
        }
    }
    val eqExporter = rememberLauncherForActivityResult(
        ActivityResultContracts.CreateDocument("text/plain"),
    ) { uri: Uri? ->
        if (uri == null) return@rememberLauncherForActivityResult
        val text = ShapeText.graphicEq(Processing.exportCurve(Repo.state.value.current))
        scope.launch {
            val written = withContext(Dispatchers.IO) {
                runCatching { context.contentResolver.openOutputStream(uri)!!.use { it.write(text.encodeToByteArray()) } }
            }
            notify(snackbar, if (written.isSuccess) appText(R.string.ui_eq_file_saved) else appText(R.string.ui_could_not_save_file, reason(written.exceptionOrNull())))
        }
    }
    val parametricExporter = rememberLauncherForActivityResult(
        ActivityResultContracts.CreateDocument("text/plain"),
    ) { uri: Uri? ->
        if (uri == null) return@rememberLauncherForActivityResult
        val preset = Repo.state.value.current
        val filters = Processing.exportFilters(preset) ?: return@rememberLauncherForActivityResult
        val text = AutoEqParser.format(Processing.preampOf(preset), filters)
        scope.launch {
            val written = withContext(Dispatchers.IO) {
                runCatching { context.contentResolver.openOutputStream(uri)!!.use { it.write(text.encodeToByteArray()) } }
            }
            notify(snackbar, if (written.isSuccess) appText(R.string.ui_eq_file_saved) else appText(R.string.ui_could_not_save_file, reason(written.exceptionOrNull())))
        }
    }
    val restorer = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri: Uri? ->
        if (uri == null) return@rememberLauncherForActivityResult
        scope.launch {
            val text = withContext(Dispatchers.IO) {
                runCatching {
                    context.contentResolver.openInputStream(uri)!!.use { it.readAtMost(MAX_BACKUP_BYTES).decodeToString() }
                }.getOrNull()
            }
            // A restore replaces presets with the same names, so it can be taken back for a while.
            val before = Repo.state.value
            when {
                text == null -> notify(snackbar, appText(R.string.ui_could_not_read_that_file_2))
                Repo.restoreBackup(text) -> {
                    val result = snackbar.showSnackbar(
                        appText(R.string.ui_backup_restored),
                        actionLabel = appText(R.string.ui_undo),
                        withDismissAction = true,
                        duration = SnackbarDuration.Long,
                    )
                    if (result == SnackbarResult.ActionPerformed) Repo.update { before }
                }
                else -> notify(snackbar, appText(R.string.ui_that_isn_t_an_eigentone_backup))
            }
        }
    }

    SettingsSection(stringResource(R.string.ui_this_preset)) {
        ActionRow(stringResource(R.string.ui_save_as_new), Help.saveNew) { saving = true }
        ShareRows { message -> scope.notify(snackbar, message) }
    }
    // Choosing between presets, by ear and by eye, side by side.
    SettingsSection(stringResource(R.string.ui_compare_presets)) {
        ActionRow(stringResource(R.string.ui_compare_two_sounds), Help.compare, detail = stringResource(R.string.ui_switch_instantly_with_an_optional_blind), onClick = LocalOpenCompare.current)
        RowDivider()
        ActionRow(stringResource(R.string.ui_compare_curves), Help.compareCurves, detail = stringResource(R.string.ui_compare_curves_detail), onClick = LocalOpenCurves.current)
    }
    SettingsSection(stringResource(R.string.ui_files_and_backup)) {
        ActionRow(stringResource(R.string.ui_import_eq_file), Help.importFile) {
            importer.launch(arrayOf("text/*", "application/octet-stream"))
        }
        RowDivider()
        ActionRow(stringResource(R.string.ui_export_eq_file), Help.exportFile) { eqExporter.launch("${state.current.name} GraphicEQ.txt") }
        RowDivider()
        val canExportFilters = remember(state.current) { Processing.exportFilters(state.current) != null }
        ActionRow(
            stringResource(R.string.ui_export_filters_file),
            Help.exportParametric,
            detail = if (canExportFilters) null else stringResource(R.string.ui_this_preset_uses_faders_or_a),
            enabled = canExportFilters,
        ) { parametricExporter.launch("${state.current.name} ParametricEQ.txt") }
        RowDivider()
        ActionRow(stringResource(R.string.ui_back_up_presets), Help.backup) { exporter.launch("eigentone-backup.json") }
        RowDivider()
        ActionRow(stringResource(R.string.ui_restore_presets), Help.restore) {
            restorer.launch(arrayOf("application/json", "text/*", "application/octet-stream"))
        }
    }

    if (saving) {
        SaveDialog(initial = state.current.name, onDismiss = { saving = false }) { name ->
            Repo.saveCurrentAs(name)
            saving = false
        }
    }
}

/** Finds and imports a headphone profile from AutoEq's published results. */
@Composable
fun AutoEqDialog(onDismiss: () -> Unit, onImported: (String) -> Unit) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var entries by remember { mutableStateOf<List<CatalogEntry>?>(null) }
    var problem by remember { mutableStateOf<String?>(null) }
    var query by rememberSaveable { mutableStateOf("") }
    var busy by remember { mutableStateOf(false) }
    var attempt by remember { mutableIntStateOf(0) }
    var measurements by remember { mutableStateOf<List<CatalogEntry>?>(null) }
    // The connected headphones, found by their name, so the search opens on them. Kept above the steps below,
    // so coming back from comparing measurements still says where the search came from.
    val connected = rememberConnectedOutputNames()
    var detected by remember { mutableStateOf<Pair<String, CatalogEntry>?>(null) }
    // Chosen, but waiting for the search to finish closing, so the comparison opens after it rather than over it.
    var nextMeasurements by remember { mutableStateOf<List<CatalogEntry>?>(null) }
    nextMeasurements?.let { waiting ->
        LaunchedEffect(waiting) {
            kotlinx.coroutines.delay(DIALOG_GAP_MS)
            measurements = waiting
            nextMeasurements = null
        }
        return
    }

    measurements?.let { options ->
        MeasurementCompare(
            options,
            onDismiss = { measurements = null },
            onChosen = { message ->
                measurements = null
                onImported(message)
            },
        )
        return
    }

    // Runs again from Try again, so a failed download isn't a dead end.
    LaunchedEffect(attempt) {
        problem = null
        val loaded = withContext(Dispatchers.IO) { runCatching { AutoEqCatalog.loadIndex(context.cacheDir) } }
        entries = loaded.getOrNull()
        problem = loaded.exceptionOrNull()?.let { appText(R.string.ui_could_not_load_the_headphone_list, reason(it)) }
    }

    LaunchedEffect(entries) {
        val list = entries ?: return@LaunchedEffect
        if (query.isNotBlank() || detected != null) return@LaunchedEffect
        // The phone's own speaker reports the phone's model, which is never a pair of headphones.
        val found = connected
            .filter { it.isNotBlank() && it != android.os.Build.MODEL }
            .firstNotNullOfOrNull { name -> AutoEqCatalog.bestMatch(list, name)?.let { name to it } }
            ?: return@LaunchedEffect
        detected = found
        query = found.second.name
    }
    val results = remember(entries, query, detected) {
        val found = AutoEqCatalog.search(entries.orEmpty(), query)
        // The best measurement of the connected pair comes first.
        val best = detected?.second?.takeIf { it in found }
        if (best == null) found else listOf(best) + (found - best)
    }
    MotionDialog(
        onDismissRequest = onDismiss,
        containerColor = Palette.Tooltip,
        title = { Text(stringResource(R.string.ui_find_headphones)) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(colors = fieldColors(), 
                    value = query,
                    onValueChange = {
                        query = it
                        // A failed download belongs to the earlier search, so it goes once the search changes.
                        if (entries != null) problem = null
                    },
                    singleLine = true,
                    label = { Text(stringResource(R.string.ui_headphone_model)) },
                    enabled = entries != null && !busy,
                )
                when {
                    problem != null -> Column {
                        Text(problem.orEmpty(), color = Palette.Danger, style = MaterialTheme.typography.bodySmall)
                        if (entries == null) TextButton(onClick = { attempt++ }) { Text(stringResource(R.string.ui_try_again)) }
                    }
                    entries == null -> Text(stringResource(R.string.ui_loading_the_list), color = Palette.TextDim)
                    busy -> Text(stringResource(R.string.ui_downloading), color = Palette.TextDim)
                    query.isNotBlank() && results.isEmpty() -> Text(stringResource(R.string.ui_no_matches), color = Palette.TextDim)
                    detected?.takeIf { it.second.name == query } != null -> Text(
                        stringResource(R.string.ui_autoeq_detected, detected!!.first),
                        style = MaterialTheme.typography.bodySmall,
                        color = Palette.Lit,
                    )
                }
                LazyColumn(Modifier.heightIn(max = 320.dp)) {
                    items(results) { entry ->
                        Text(
                            entry.title,
                            modifier = Modifier
                                .fillMaxWidth()
                                .heightIn(min = 48.dp)
                                .clickable(enabled = !busy, role = Role.Button) {
                                    // Several measurements are worth hearing before choosing one.
                                    val same = AutoEqCatalog.measurementsOf(entries.orEmpty(), entry)
                                    if (same.size > 1) {
                                        nextMeasurements = same
                                        return@clickable
                                    }
                                    busy = true
                                    problem = null
                                    scope.launch {
                                        val text = withContext(Dispatchers.IO) {
                                            runCatching { AutoEqCatalog.loadProfile(entry) }
                                        }
                                        val parsed = text.getOrNull()?.let(AutoEqParser::parse)
                                        val info = if (parsed == null) null else withContext(Dispatchers.IO) { AutoEqCatalog.loadCorrectionInfo(entry) }
                                        busy = false
                                        if (parsed == null) {
                                            problem = appText(R.string.ui_could_not_download_that_profile_it)
                                        } else {
                                            Repo.importPreset(entry.title, parsed, info)
                                            onImported(appText(R.string.ui_added, entry.title))
                                        }
                                    }
                                }
                                .padding(vertical = 12.dp),
                            style = MaterialTheme.typography.bodyMedium,
                        )
                    }
                }
            }
        },
        confirmButton = { TextButton(onClick = onDismiss) { Text(stringResource(R.string.ui_close)) } },
    )
}

@Composable
private fun SaveDialog(
    title: String = stringResource(R.string.ui_save_preset),
    initial: String = "",
    onDismiss: () -> Unit,
    onSave: (String) -> Unit,
) {
    val state by Repo.state.collectAsStateWithLifecycle()
    var name by remember { mutableStateOf(io.github.dragjon.eigentone.data.PresetOrganiser.freeName(state, initial).takeIf { initial.isNotBlank() } ?: "") }
    val taken = io.github.dragjon.eigentone.data.PresetOrganiser.nameTaken(state, name)
    MotionDialog(
        onDismissRequest = onDismiss,
        containerColor = Palette.Tooltip,
        title = { Text(title) },
        text = {
            OutlinedTextField(colors = fieldColors(), 
                value = name,
                onValueChange = { name = it },
                singleLine = true,
                label = { Text(stringResource(R.string.ui_name)) },
                isError = taken,
                supportingText = if (taken) ({ Text(stringResource(R.string.ui_name_taken, name.trim())) }) else null,
            )
        },
        confirmButton = {
            TextButton(enabled = name.isNotBlank() && !taken, onClick = { onSave(name.trim()) }) { Text(stringResource(R.string.ui_save)) }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text(stringResource(R.string.ui_cancel)) } },
    )
}

@SuppressLint("BatteryLife")
@Composable
private fun SystemTab(state: AppState, status: EngineStatus) {
    val context = LocalContext.current
    val output by Repo.output.collectAsStateWithLifecycle()
    var exempt by remember { mutableStateOf(true) }
    // Rechecked on every return, since the user can decline the system prompt.
    LifecycleResumeEffect(Unit) {
        exempt = context.getSystemService(PowerManager::class.java).isIgnoringBatteryOptimizations(context.packageName)
        onPauseOrDispose { }
    }

    Text(stringResource(R.string.ui_tab_system), style = MaterialTheme.typography.titleLarge, color = Palette.Text, modifier = Modifier.padding(horizontal = 4.dp))
    EveryPresetSections(state)

    SettingsSection(stringResource(R.string.ui_appearance)) {
        Column(Modifier.padding(start = 16.dp, end = 16.dp, top = 4.dp, bottom = 12.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(stringResource(R.string.ui_theme), style = MaterialTheme.typography.bodyLarge, color = Palette.Text)
                InfoButton(Help.theme)
            }
            // The same joined selector as the audio route, so every few-way choice looks alike (user decision).
            val modes = ThemeMode.entries
            val names = listOf(
                stringResource(R.string.ui_theme_device),
                stringResource(R.string.ui_theme_light),
                stringResource(R.string.ui_theme_dark),
            )
            // Recolouring the whole app is heavy, so the thumb slides first and the theme follows once it's there,
            // rather than the toggle waiting for the recolour before it can move.
            var picked by remember { mutableStateOf<Int?>(null) }
            val themeScope = rememberCoroutineScope()
            SlidingToggle(
                options = names.map { ToggleOption(it) },
                selected = picked ?: modes.indexOf(state.themeMode),
                onSelect = { i ->
                    picked = i
                    themeScope.launch {
                        kotlinx.coroutines.delay(THEME_SWITCH_AFTER_MS)
                        Repo.update { it.copy(themeMode = modes[i]) }
                        picked = null
                    }
                },
                modifier = Modifier.fillMaxWidth(),
            )
        }
        RowDivider()
        SwitchSetting(
            stringResource(R.string.ui_advanced_controls),
            Help.advanced,
            state.advancedControls,
            detail = stringResource(if (state.advancedControls) R.string.ui_advanced_on else R.string.ui_advanced_off),
        ) { on -> Repo.update { it.copy(advancedControls = on) } }
    }


    SettingsSection(stringResource(R.string.ui_engine), trailing = { InfoButton(Help.power) }) {
        if (Disclosure.engine(state)) {
        Column(Modifier.padding(start = 16.dp, end = 16.dp, top = 4.dp, bottom = 12.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(stringResource(R.string.ui_audio_route), style = MaterialTheme.typography.bodyLarge, color = Palette.Text)
                InfoButton(Help.engineMode)
            }
            val modes = EngineMode.entries
            SlidingToggle(
                options = modes.map { ToggleOption(engineModeLabel(it)) },
                selected = modes.indexOf(state.engineMode),
                onSelect = { i -> Repo.update { it.copy(engineMode = modes[i]) } },
                modifier = Modifier.fillMaxWidth(),
            )
        }
        RowDivider()
        SwitchSetting(stringResource(R.string.ui_low_latency), Help.lowLatency, state.lowLatency) { on ->
            Repo.update { it.copy(lowLatency = on) }
        }
        RowDivider()
        }
        SwitchSetting(stringResource(R.string.ui_start_automatically), Help.startOnBoot, state.startOnBoot) { on ->
            Repo.update { it.copy(startOnBoot = on) }
        }
        if (!exempt) {
            RowDivider()
            ActionRow(stringResource(R.string.ui_allow_background_use), Help.battery, detail = stringResource(R.string.ui_battery_optimisation_may_stop_the_eq_whe)) {
                // Some phones don't offer the direct request, so their battery list opens instead.
                context.openSafely(Intent(Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS, Uri.parse("package:${context.packageName}"))) ||
                    context.openSafely(Intent(Settings.ACTION_IGNORE_BATTERY_OPTIMIZATION_SETTINGS))
            }
        }
    }

    var showGuide by remember { mutableStateOf(false) }
    var confirmReset by remember { mutableStateOf(false) }
    SettingsSection(stringResource(R.string.ui_help_and_reset)) {
        ActionRow(stringResource(R.string.tour_take), Help.tour) { Tour.start() }
        RowDivider()
        ActionRow(stringResource(R.string.welcome_show_again), Help.welcome) {
            Repo.update { it.copy(welcomeDone = false, welcomeStep = 0) }
        }
        RowDivider()
        ActionRow(stringResource(R.string.ui_getting_the_best_sound), Help.guide) { showGuide = true }
        RowDivider()
        ActionRow(stringResource(R.string.ui_reset_sound_settings_2), Help.reset) { confirmReset = true }
    }
    if (showGuide) GuideDialog(onDismiss = { showGuide = false })
    if (confirmReset) {
        MotionDialog(
            onDismissRequest = { confirmReset = false },
            containerColor = Palette.Tooltip,
            title = { Text(stringResource(R.string.ui_reset_sound_settings)) },
            text = { Text(stringResource(R.string.ui_the_sound_goes_back_to_flat_your_saved_p)) },
            confirmButton = {
                TextButton(onClick = {
                    Repo.resetSound()
                    confirmReset = false
                }) { Text(stringResource(R.string.ui_reset), color = Palette.Danger) }
            },
            dismissButton = { TextButton(onClick = { confirmReset = false }) { Text(stringResource(R.string.ui_cancel)) } },
        )
    }

    DiagnosticsSection(status, output, onConsole = LocalOpenConsole.current)

    SettingsSection(stringResource(R.string.ui_open_source)) {
        val version = remember {
            runCatching { context.packageManager.getPackageInfo(context.packageName, 0).versionName }.getOrNull().orEmpty()
        }
        Text(
            stringResource(R.string.ui_open_source_body, version),
            style = MaterialTheme.typography.bodyMedium,
            color = Palette.TextDim,
            modifier = Modifier.padding(16.dp),
        )
        RowDivider()
        ActionRow(stringResource(R.string.ui_view_source), Help.openSource, detail = SOURCE_URL.removePrefix("https://")) {
            context.openSafely(Intent(Intent.ACTION_VIEW, Uri.parse(SOURCE_URL)))
        }
        RowDivider()
        ActionRow(stringResource(R.string.ui_report_problem), Help.reportProblem, detail = stringResource(R.string.ui_report_problem_detail)) {
            context.openSafely(Intent(Intent.ACTION_VIEW, Uri.parse("$SOURCE_URL/issues")))
        }
    }

    if (Disclosure.status(state)) SettingsSection(stringResource(R.string.ui_status)) {
        Fact(stringResource(R.string.help_status_running_title), Help.statusRunning, if (status.running) stringResource(R.string.ui_yes) else stringResource(R.string.ui_no))
        RowDivider()
        Fact(stringResource(R.string.help_status_mode_title), Help.statusMode, status.mode)
        RowDivider()
        Fact(stringResource(R.string.help_status_bands_title), Help.statusBands, if (status.bandCount > 0) "${status.bandCount}" else stringResource(R.string.ui_none))
        RowDivider()
        Fact(stringResource(R.string.help_status_control_title), Help.statusControl, if (status.hasControl) stringResource(R.string.ui_yes) else stringResource(R.string.ui_no))
        RowDivider()
        Fact(stringResource(R.string.help_status_readback_title), Help.statusReadback, status.readback ?: stringResource(R.string.ui_not_checked_yet))
        RowDivider()
        Fact(stringResource(R.string.help_status_repairs_title), Help.statusRepairs, "${status.rebuilds}")
        RowDivider()
        Fact(stringResource(R.string.help_status_output_title), Help.statusOutput, output.label)
        if (status.sessions.isNotEmpty()) {
            RowDivider()
            Fact(stringResource(R.string.help_status_apps_title), Help.statusApps, status.sessions.joinToString())
        }
        if (status.bandCount in 1 until 64) {
            RowDivider()
            Text(
                stringResource(R.string.ui_this_phone_only_allowed_bands_so, status.bandCount),
                style = MaterialTheme.typography.bodySmall,
                color = Palette.TextDim,
                modifier = Modifier.padding(16.dp),
            )
        }
        status.error?.let {
            RowDivider()
            Text(
                it,
                style = MaterialTheme.typography.bodySmall,
                color = Palette.Danger,
                modifier = Modifier.padding(16.dp),
            )
        }
    }
}

@Composable
private fun GuideDialog(onDismiss: () -> Unit) {
    val tips = listOf(
        stringResource(R.string.ui_get_a_good_seal) to stringResource(R.string.ui_with_earbuds_fit_matters_more_than),
        stringResource(R.string.ui_use_one_eq_at_a_time) to stringResource(R.string.ui_eq_in_a_headphone_app_such_2),
        stringResource(R.string.ui_start_with_find_my_sound) to stringResource(R.string.ui_it_picks_a_correction_for_your),
        stringResource(R.string.ui_change_a_little_at_a_time) to stringResource(R.string.ui_a_change_of_2_or_3),
        stringResource(R.string.ui_prefer_cuts_to_boosts) to stringResource(R.string.ui_if_something_is_too_loud_cutting),
        stringResource(R.string.ui_compare_with_bypass) to stringResource(R.string.ui_your_ears_adapt_within_minutes_so),
        stringResource(R.string.ui_try_several_songs) to stringResource(R.string.ui_one_track_can_mislead_you_test),
    )
    MotionDialog(
        onDismissRequest = onDismiss,
        containerColor = Palette.Tooltip,
        title = { Text(stringResource(R.string.ui_getting_the_best_sound)) },
        text = {
            Column(
                Modifier.verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                tips.forEach { (title, body) ->
                    Column {
                        Text(title, style = MaterialTheme.typography.titleSmall, color = Palette.Text)
                        Text(body, style = MaterialTheme.typography.bodyMedium, color = Palette.TextDim)
                    }
                }
            }
        },
        confirmButton = { TextButton(onClick = onDismiss) { Text(stringResource(R.string.ui_close)) } },
    )
}

@Composable
private fun Fact(label: String, tip: Tip, value: String) {
    Row(
        Modifier
            .fillMaxWidth()
            .heightIn(min = 56.dp)
            .padding(start = 16.dp, end = 16.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        // The same size and height as every other setting row.
        Text(label, style = MaterialTheme.typography.bodyLarge, color = Palette.Text)
        InfoButton(tip)
        // Mono digits are for numbers that change; words such as a mode or a device name read as text.
        val numeric = value.all { it.isDigit() || it == '/' || it == ' ' || it == '.' }
        Text(
            value,
            Modifier
                .weight(1f)
                .padding(start = 8.dp),
            style = if (numeric) Readout else MaterialTheme.typography.bodyMedium,
            color = Palette.TextDim,
            textAlign = androidx.compose.ui.text.style.TextAlign.End,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
        )
    }
}
