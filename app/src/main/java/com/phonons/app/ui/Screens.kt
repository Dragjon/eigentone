package com.phonons.app.ui

import androidx.compose.ui.platform.LocalDensity
import androidx.compose.foundation.gestures.animateScrollBy
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.animation.core.LinearEasing
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
import com.phonons.app.data.NightSchedule
import androidx.compose.ui.graphics.graphicsLayer
import com.phonons.app.data.ClipRisk
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Search
import com.phonons.app.data.PresetOrganiser
import com.phonons.app.engine.PresetShortcuts
import androidx.compose.animation.Crossfade
import androidx.compose.animation.core.tween
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
import com.phonons.app.R
import com.phonons.app.data.AppState
import com.phonons.app.data.Disclosure
import com.phonons.app.data.AutoEqCatalog
import com.phonons.app.data.BackupCodec
import com.phonons.app.data.BuiltIns
import com.phonons.app.data.CatalogEntry
import com.phonons.app.data.EngineMode
import com.phonons.app.data.EngineStatus
import com.phonons.app.data.ListeningMode
import com.phonons.app.data.Preset
import com.phonons.app.data.Processing
import com.phonons.app.data.Profiles
import com.phonons.app.data.Repo
import com.phonons.app.data.ReverbRoom
import com.phonons.app.data.EqualLoudness
import com.phonons.app.dsp.GraphicEq
import com.phonons.app.data.LimiterSettings
import com.phonons.app.data.ShapeText
import com.phonons.app.data.ThemeMode
import com.phonons.app.data.Tone
import com.phonons.app.dsp.AutoEqParser
import com.phonons.app.dsp.Eq
import com.phonons.app.dsp.Filter
import com.phonons.app.dsp.FilterType
import com.phonons.app.engine.SpectrumAnalyser
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.Locale
import kotlin.math.log10
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

/** Opens the guided listening test from anywhere in the main screens. */
private val LocalOpenFinder = compositionLocalOf<() -> Unit> { error("No finder") }

private val LocalOpenHearing = compositionLocalOf<() -> Unit> { error("No hearing check") }

private const val SEARCH_FROM_PRESETS = 6
private const val TAB_FADE_MS = 140
private const val OPEN_MS = 1500
/** When, as a share of the opening, the app starts to unfold out of the line. */
private const val OPEN_UNFOLD_AT = 0.52f
private const val OPEN_BARS = 41

/** How far through the window from [start] to [end] the progress is, from 0 to 1. */
private fun phase(progress: Float, start: Float, end: Float) = ((progress - start) / (end - start)).coerceIn(0f, 1f)

/**
 * The power-on drawn over the app as it opens: the lit line, two pulses ringing out from its middle, and a
 * spectrum that bursts out of the line from the centre outward, moves like live sound, then settles back
 * into the line, edges first, as the app unfolds.
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

private val LocalOpenSweep = compositionLocalOf<() -> Unit> { error("No sweep") }

private val LocalOpenCompare = compositionLocalOf<() -> Unit> { error("No comparison") }

/** Opens the faders across the whole screen. */
private val LocalOpenFaders = compositionLocalOf<() -> Unit> { error("No expanded faders") }

/** Opens the shape library from the graphic EQ. */
private val LocalOpenShapes = compositionLocalOf<() -> Unit> { error("No shape library") }

@Composable
fun PhononsApp(onSetEnabled: (Boolean) -> Unit) {
    val state by Repo.state.collectAsStateWithLifecycle()
    val status by Repo.status.collectAsStateWithLifecycle()
    var tab by rememberSaveable { mutableStateOf(Tab.EQ) }
    // Settings opens from the top bar, so Back returns to whichever tab it was opened from.
    var beforeSettings by rememberSaveable { mutableStateOf(Tab.EQ) }
    val toggleSettings = {
        if (tab == Tab.SYSTEM) {
            tab = beforeSettings
        } else {
            beforeSettings = tab
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
    // Each tab keeps its own position, so switching never lands partway down another page.
    val scrolls: Map<Tab, ScrollState> = Tab.entries.associateWith { rememberScrollState() }
    var finding by rememberSaveable { mutableStateOf(false) }
    var browsingShapes by rememberSaveable { mutableStateOf(false) }
    var checkingHearing by rememberSaveable { mutableStateOf(false) }
    var comparing by rememberSaveable { mutableStateOf(false) }
    var sweeping by rememberSaveable { mutableStateOf(false) }
    var findingPeaks by rememberSaveable { mutableStateOf(false) }
    var expandingFaders by rememberSaveable { mutableStateOf(false) }

    if (expandingFaders) {
        ExpandedFaders(onClose = { expandingFaders = false })
        return
    }

    if (findingPeaks) {
        PeakFinderScreen(onClose = { findingPeaks = false })
        return
    }

    if (sweeping) {
        SweepScreen(onClose = { sweeping = false })
        return
    }

    if (comparing) {
        CompareScreen(onClose = { comparing = false })
        return
    }

    if (checkingHearing) {
        HearingScreen(onClose = { checkingHearing = false })
        return
    }

    if (finding) {
        FinderScreen(onSetEnabled = onSetEnabled, onClose = { finding = false })
        return
    }
    if (browsingShapes) {
        ShapeBrowser(onClose = { browsingShapes = false })
        return
    }
    // After the tools above, so Find your sound opens over the welcome and returns to it.
    if (!state.welcomeDone) {
        WelcomeScreen(onSetEnabled = onSetEnabled, onFindSound = { finding = true })
        return
    }

    // The tour runs on the Equaliser tab, scrolled so the control it points at is in view.
    val tourStop = Tour.stop
    val screenHeight = with(LocalDensity.current) { androidx.compose.ui.platform.LocalConfiguration.current.screenHeightDp.dp.toPx() }
    val navRoom = with(LocalDensity.current) { 140.dp.toPx() }
    LaunchedEffect(tourStop) {
        if (tourStop == null) return@LaunchedEffect
        if (tab != Tab.EQ) tab = Tab.EQ
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
    // a spectrum bursts from the line, and the app unfolds as the bars settle back into it.
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
    Box(
        Modifier
            .fillMaxSize()
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
                // Opens quickly and settles gently.
                val eased = 1f - (1f - unfold) * (1f - unfold) * (1f - unfold)
                scaleY = 0.006f + 0.994f * eased
                alpha = (unfold * 3f).coerceIn(0f, 1f)
            },
    ) {
    CompositionLocalProvider(
        LocalSnackbar provides snackbar,
        LocalOpenFinder provides { finding = true },
        LocalOpenShapes provides { browsingShapes = true },
        LocalOpenHearing provides { checkingHearing = true },
        LocalOpenCompare provides { comparing = true },
        LocalOpenSweep provides { sweeping = true },
        LocalOpenPeaks provides { findingPeaks = true },
        LocalOpenFaders provides { expandingFaders = true },
        LocalOpenPresets provides { tab = Tab.PRESETS },
    ) {
        Scaffold(
            containerColor = Palette.Background,
            topBar = { TopBar(state, status, showCurve = tab != Tab.EQ, settingsOpen = tab == Tab.SYSTEM, onSettings = toggleSettings, onSetEnabled) },
            snackbarHost = { SnackbarHost(snackbar) },
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
                    Tab.entries.filter { it != Tab.SYSTEM }.forEach { t ->
                        NavItem(t, selected = tab == t, modifier = Modifier.weight(1f)) { tab = t }
                    }
                }
            },
        ) { padding ->
            androidx.activity.compose.BackHandler(enabled = tab == Tab.SYSTEM) { tab = beforeSettings }
            Box(
                Modifier
                    .fillMaxSize()
                    .padding(padding),
                contentAlignment = Alignment.TopCenter,
            ) {
                Crossfade(targetState = tab, animationSpec = tween(TAB_FADE_MS), label = "tab") { shownTab ->
                    // The whole width scrolls, so the space beside the page works too, as on a tablet or in landscape.
                    Box(
                        Modifier
                            .fillMaxSize()
                            .verticalScroll(scrolls.getValue(shownTab)),
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
                            when (shownTab) {
                                Tab.EQ -> EqTab(state)
                                Tab.SOUND -> SoundTab(state)
                                Tab.PRESETS -> PresetsTab(state)
                                Tab.HEADPHONES -> HeadphonesTab(state)
                                Tab.SYSTEM -> SystemTab(state, status)
                            }
                        }
                    }
                }
                // Floats over the page rather than sitting in it, so appearing can never move anything,
                // such as a fader under a finger that's still dragging it.
                val savedVersion = Repo.allPresets(state).firstOrNull { it.id == state.current.id }
                if (savedVersion != null && savedVersion != state.current) {
                    UnsavedBar(
                        state,
                        savedVersion,
                        Modifier
                            .align(Alignment.BottomCenter)
                            .widthIn(max = 640.dp)
                            .padding(horizontal = 12.dp, vertical = 8.dp),
                    )
                }
            }
        }
    }
    TourOverlay()
    }
}

/** One bottom tab: a lit pill behind the icon when chosen, and the name below. */
@Composable
private fun NavItem(tab: Tab, selected: Boolean, modifier: Modifier = Modifier, onClick: () -> Unit) {
    // The whole tab takes the touch, but the press ripples inside the rounded pill, as Android's own tabs do,
    // rather than as a tight box around the narrow tab.
    val presses = remember { androidx.compose.foundation.interaction.MutableInteractionSource() }
    Column(
        modifier
            .selectable(selected = selected, interactionSource = presses, indication = null, role = Role.Tab, onClick = onClick)
            .padding(vertical = 2.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Box(
            Modifier
                .size(width = 64.dp, height = 32.dp)
                .clip(androidx.compose.foundation.shape.RoundedCornerShape(16.dp))
                .background(if (selected) Palette.Lit else Color.Transparent)
                .indication(presses, androidx.compose.material3.ripple()),
            contentAlignment = Alignment.Center,
        ) {
            // The label below already names the tab for screen readers.
            Icon(painterResource(tab.icon), contentDescription = null, tint = if (selected) Palette.OnLit else Palette.TextDim)
        }
        Text(
            stringResource(tab.label),
            style = MaterialTheme.typography.labelMedium,
            color = if (selected) Palette.Text else Palette.TextDim,
            maxLines = 1,
            softWrap = false,
            modifier = Modifier.padding(top = 4.dp),
        )
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
            .padding(start = 16.dp, end = 16.dp, top = 8.dp, bottom = 8.dp),
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
            onPick = Repo::loadPreset,
            onAll = LocalOpenPresets.current,
            modifier = Modifier
                .weight(1f)
                .tourTarget(TourStop.PRESETS),
        ) {
            Text(stringResource(R.string.app_name), style = MaterialTheme.typography.titleLarge, color = Palette.Text)
        }
        Row(Modifier.tourTarget(TourStop.POWER), verticalAlignment = Alignment.CenterVertically) {
            if (state.enabled) BypassButton(state.bypass)
            Switch(
                checked = state.enabled,
                onCheckedChange = onSetEnabled,
                modifier = Modifier
                    .padding(start = 8.dp)
                    .semantics { contentDescription = "Eigentone" },
            )
        }
        IconButton(onClick = onSettings, modifier = Modifier.tourTarget(TourStop.SETTINGS)) {
            Icon(
                Icons.Outlined.Settings,
                contentDescription = stringResource(R.string.ui_tab_system),
                tint = if (settingsOpen) Palette.Lit else Palette.TextDim,
            )
        }
    }
    Box(Modifier.tourTarget(TourStop.STRIP)) { LiveStrip(state, showCurve) }
    }
}

private val BypassPadding = PaddingValues(horizontal = 14.dp, vertical = 8.dp)

/** Distinct from the power switch: neutral until engaged, when it's clearly a held state. */
@Composable
private fun BypassButton(bypassed: Boolean) {
    val toggle = { Repo.update { it.copy(bypass = !it.bypass) } }
    if (bypassed) {
        Button(onClick = { toggle() }, contentPadding = BypassPadding) { Text(stringResource(R.string.ui_bypassed)) }
    } else {
        FilledTonalButton(onClick = { toggle() }, contentPadding = BypassPadding) { Text(stringResource(R.string.ui_bypass)) }
    }
}

private fun statusLine(state: AppState, status: EngineStatus): String {
    val saved = Repo.allPresets(state).firstOrNull { it.id == state.current.id }
    val name = if (saved == state.current) state.current.name else appText(R.string.ui_edited, state.current.name)
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
    val selected = filters.firstOrNull { it.id == selectedId }
    val showParametric = Disclosure.parametric(state)
    val inGraphic = graphicMode || !showParametric
    val attenuation by Repo.attenuationDb.collectAsStateWithLifecycle()
    val curveFilters = remember(state, attenuation, Repo.output.value.key) { Processing.effectiveFilters(state, attenuation, Repo.output.value.key) }
    val graphOutput by Repo.output.collectAsStateWithLifecycle()
    val earCurves = remember(curveFilters, state.earCorrections, graphOutput.key, state.useSystemSurround, state.bypass) {
        if (state.bypass) null else Processing.channelFilters(state, graphOutput.key, curveFilters)
    }
    // The measured points of the ear correction, sitting on each ear's own curve.
    val earMarkers = remember(earCurves, state.earCorrections, graphOutput.key) {
        val ears = state.earsFor(graphOutput.key)?.takeIf { it.enabled }
        if (ears == null || earCurves == null) {
            null
        } else {
            com.phonons.app.data.EarCorrection(
                left = ears.left.map { com.phonons.app.data.ShapePoint(it.hz, Eq.curveDb(earCurves.first, it.hz)) },
                right = ears.right.map { com.phonons.app.data.ShapePoint(it.hz, Eq.curveDb(earCurves.second, it.hz)) },
            )
        }
    }
    val savedPreset = Repo.allPresets(state).firstOrNull { it.id == state.current.id }
    val savedCurve = remember(savedPreset, state.current, state.loudness, state.loudnessStrength, attenuation, state.bypass) {
        savedPreset?.takeIf { it != state.current && !state.bypass }
            ?.let { Processing.effectiveFilters(state.copy(current = it), attenuation, Repo.output.value.key) }
    }
    val spectrumState = Repo.spectrum.collectAsStateWithLifecycle()

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

    Box(
        Modifier
            .fillMaxWidth()
            .clip(PanelShape)
            .background(Palette.Surface),
    ) {
        EqGraph(
            // Graphic mode has its own faders, so the parametric nodes would only get in the way.
            filters = if (inGraphic) emptyList() else filters,
            heightDp = if (inGraphic) 170 else 260,
            curveFilters = earCurves?.first ?: curveFilters,
            rightCurveFilters = earCurves?.second,
            ghostFilters = savedCurve,
            morphKey = state.current.id,
            earMarkers = earMarkers,
            spectrum = { spectrumState.value },
            selectedId = selectedId,
            dimmed = !state.enabled || state.bypass,
            onSelect = { selectedId = it },
            onDrag = { id, freq, gain ->
                updateFilter(id) { f -> f.copy(freq = freq, gainDb = if (f.type.hasGain) gain else f.gainDb) }
            },
            onDragEnd = Repo::commit,
        )
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
        } else SingleChoiceSegmentedButtonRow(Modifier.weight(1f)) {
            SegmentedButton(
                selected = graphicMode,
                onClick = { graphicMode = true },
                shape = SegmentedButtonDefaults.itemShape(0, 2, KeyShape),
                colors = segmentColors(),
                icon = {},
            ) { Text(stringResource(R.string.ui_graphic), maxLines = 1) }
            SegmentedButton(
                selected = !graphicMode,
                onClick = { graphicMode = false },
                shape = SegmentedButtonDefaults.itemShape(1, 2, KeyShape),
                colors = segmentColors(),
                icon = {},
            ) { Text(stringResource(R.string.ui_parametric), maxLines = 1) }
        }
        // Beside the mode rather than on the graph, where the ear legends sit.
        if (showParametric) AnalyserToggle(state, Modifier)
        if (showParametric) InfoButton(Help.eqMode)
    }

    if (inGraphic) {
        GraphicEqConsole(state.current.tone, state.current.id, onBrowseShapes = LocalOpenShapes.current, onExpand = LocalOpenFaders.current)
        return
    }

    Row(
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier.fillMaxWidth(),
    ) {
        FilledTonalButton(
            enabled = filters.size < MAX_FILTERS,
            onClick = {
                val id = (filters.maxOfOrNull { it.id } ?: 0) + 1
                Repo.edit { it.copy(filters = it.filters + Filter(id)) }
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
        FilterEditor(
            filter = selected,
            filters = filters,
            onSelect = { selectedId = it },
            onDelete = {
                val index = filters.indexOf(selected)
                Repo.edit { p -> p.copy(filters = p.filters.filterNot { it.id == selected.id }) }
                Repo.commit()
                val remaining = Repo.state.value.current.filters
                selectedId = remaining.getOrNull(index.coerceAtMost(remaining.lastIndex))?.id
            },
        )
    }
}

/** Shown while the preset has changes, so they can be kept or dropped without leaving the EQ. */
@Composable
private fun UnsavedBar(state: AppState, saved: Preset, modifier: Modifier = Modifier) {
    val scope = rememberCoroutineScope()
    val snackbar = LocalSnackbar.current
    var naming by remember { mutableStateOf(false) }
    val own = state.presets.any { it.id == saved.id }
    Row(
        modifier
            .fillMaxWidth()
            .shadow(6.dp, PanelShape)
            .clip(PanelShape)
            .background(Palette.Tooltip)
            .padding(start = 16.dp, end = 8.dp, top = 4.dp, bottom = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            stringResource(R.string.ui_unsaved_changes, saved.name),
            style = MaterialTheme.typography.bodyMedium,
            color = Palette.Text,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.weight(1f),
        )
        TextButton(onClick = {
            Repo.discardEdits()
            scope.notify(snackbar, appText(R.string.ui_edits_discarded, saved.name))
        }) { Text(stringResource(R.string.ui_discard)) }
        Button(onClick = {
            if (own) {
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
        val analyser = SpectrumAnalyser { Repo.spectrum.value = it }
        val observer = LifecycleEventObserver { _, event ->
            val allowed = enabled && context.checkSelfPermission(Manifest.permission.RECORD_AUDIO) ==
                PackageManager.PERMISSION_GRANTED
            when (event) {
                Lifecycle.Event.ON_START -> if (allowed) analyserUnavailable.value = !analyser.start()
                Lifecycle.Event.ON_STOP -> {
                    analyser.stop()
                    Repo.spectrum.value = null
                }
                else -> Unit
            }
        }
        lifecycle.addObserver(observer)
        onDispose {
            lifecycle.removeObserver(observer)
            analyser.stop()
            Repo.spectrum.value = null
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
            selectedContainerColor = Palette.Lit,
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

private fun updateFilter(id: Int, transform: (Filter) -> Filter) {
    Repo.edit { p -> p.copy(filters = p.filters.map { if (it.id == id) transform(it) else it }) }
}

@Composable
private fun FilterEditor(filter: Filter, filters: List<Filter>, onSelect: (Int) -> Unit, onDelete: () -> Unit) {
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
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp), modifier = Modifier.fillMaxWidth()) {
                TypeChip(filter, FilterType.BELL, stringResource(R.string.ui_bell), Modifier.weight(1f))
                TypeChip(filter, FilterType.LOW_SHELF, stringResource(R.string.ui_low_shelf), Modifier.weight(1.2f))
                TypeChip(filter, FilterType.HIGH_SHELF, stringResource(R.string.ui_high_shelf), Modifier.weight(1.2f))
            }
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp), modifier = Modifier.fillMaxWidth()) {
                TypeChip(filter, FilterType.HIGH_PASS, stringResource(R.string.ui_high_pass), Modifier.weight(1f))
                TypeChip(filter, FilterType.LOW_PASS, stringResource(R.string.ui_low_pass), Modifier.weight(1f))
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
            selectedContainerColor = Palette.Lit,
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

private fun formatPercent(amount: Double): String = "${(amount * 100).toInt()}%"

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
    if (Disclosure.presetLevel(state)) SettingsSection(state.current.name) {
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
                TextButton(onClick = {
                    Repo.editTone { it.copy(bassDb = 0.0, bassHz = Tone.DEFAULT_BASS_HZ, trebleDb = 0.0, vocalsDb = 0.0, bassTunerDb = 0.0) }
                    Repo.commit()
                }) { Text(stringResource(R.string.ui_reset_tone)) }
            }
        },
    ) {
        ToneSlider(stringResource(R.string.ui_bass), Help.bass, tone.bassDb, saved?.bassDb ?: 0.0) { v -> Repo.editTone { it.copy(bassDb = v) } }
        // Only means something once there's a bass change to place.
        if (tone.bassDb != 0.0 && Disclosure.bassDetail(state)) {
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
                    selected = com.phonons.app.data.BassCharacter.entries.indexOf(tone.bassCharacter),
                    onSelect = { i ->
                        Repo.editTone { it.copy(bassCharacter = com.phonons.app.data.BassCharacter.entries[i]) }
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
                state.balance < -0.005 -> "L ${formatPercent(-state.balance)}"
                state.balance > 0.005 -> "R ${formatPercent(state.balance)}"
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
                        selectedContainerColor = Palette.Lit,
                        selectedLabelColor = Palette.OnLit,
                        labelColor = Palette.Text,
                    ),
                )
            }
        }
        if (chosen != ReverbRoom.OFF) {
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
    fun setEars(new: com.phonons.app.data.EarCorrection?) = Repo.update {
        it.copy(earCorrections = if (new == null) it.earCorrections - key else it.earCorrections + (key to new))
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
                    ) { setEars(latest.copy(enabled = ears.enabled)) }
                }
            }
            RowDivider()
            ActionRow(stringResource(R.string.ui_remove_ear_correction), Help.earCorrection) { setEars(null) }
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
        fun updateSchedule(change: (com.phonons.app.data.NightSchedule) -> com.phonons.app.data.NightSchedule) =
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
    Row(Modifier.padding(horizontal = 16.dp)) {
        FilledTonalButton(onClick = { setUsual(attenuation) }) { Text(stringResource(R.string.ui_use_current_volume)) }
    }
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
private fun LoudnessGraph(points: List<com.phonons.app.data.ShapePoint>, modifier: Modifier) {
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
                    selectedContainerColor = Palette.Lit,
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
        AlertDialog(
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
private fun CoroutineScope.notify(snackbar: SnackbarHostState, message: String) {
    launch { snackbar.showSnackbar(message, withDismissAction = true, duration = SnackbarDuration.Short) }
}

/**
 * Everything about what's connected: its correction, the preset it switches to, your ears, and how it adapts
 * to quiet listening. Each output keeps its own, so this page follows whatever is plugged in.
 */
@Composable
private fun HeadphonesTab(state: AppState) {
    val scope = rememberCoroutineScope()
    val snackbar = LocalSnackbar.current
    val output by Repo.output.collectAsStateWithLifecycle()
    val attenuation by Repo.attenuationDb.collectAsStateWithLifecycle()
    var finding by remember { mutableStateOf(false) }
    var tuning by remember { mutableStateOf(false) }
    val preset = state.current
    val saved = savedPreset(state)?.tone

    Column(Modifier.padding(horizontal = 4.dp)) {
        Text(output.label, style = MaterialTheme.typography.titleLarge, color = Palette.Text, maxLines = 2, overflow = TextOverflow.Ellipsis)
        Text(stringResource(R.string.ui_hp_scope), style = MaterialTheme.typography.bodySmall, color = Palette.TextDim)
    }

    SettingsSection(stringResource(R.string.ui_hp_correction)) {
        ActionRow(stringResource(R.string.ui_find_headphones), Help.find) { finding = true }
        if (preset.filters.any { it.gainDb != 0.0 || !it.type.hasGain } || preset.tone.importedCurve.isNotEmpty()) {
            RowDivider()
            SliderSetting(
                title = stringResource(R.string.ui_headphone_correction),
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
        ActionRow(stringResource(R.string.ui_hearing_check), Help.hearing, detail = stringResource(R.string.ui_builds_a_curve_for_how_you_hear), onClick = LocalOpenHearing.current)
        EarCorrectionRows(state)
    }

    SettingsSection(stringResource(R.string.ui_quiet_listening)) {
        if (state.loudness) {
            EqualLoudnessRows(state, attenuation)
        } else {
            Text(
                stringResource(R.string.ui_quiet_off, output.label),
                style = MaterialTheme.typography.bodyMedium,
                color = Palette.TextDim,
                modifier = Modifier.padding(16.dp),
            )
        }
    }

    SettingsSection(stringResource(R.string.ui_listening_tools)) {
        ActionRow(stringResource(R.string.ui_find_my_sound), Help.finder, detail = stringResource(R.string.ui_a_short_guided_listening_test), onClick = LocalOpenFinder.current)
        RowDivider()
        ActionRow(stringResource(R.string.ui_compare_two_sounds), Help.compare, detail = stringResource(R.string.ui_switch_instantly_with_an_optional_blind), onClick = LocalOpenCompare.current)
        if (Disclosure.measureTools(state)) {
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
            // A correction is the moment people find it too neutral, so offer this straight away.
            tuning = true
        }
    }
    if (tuning) CorrectionTuner(onDismiss = { tuning = false })
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
    activeContainerColor = Palette.Lit,
    activeContentColor = Palette.OnLit,
    activeBorderColor = Palette.Lit,
    inactiveContainerColor = Palette.SurfaceHigh,
    inactiveContentColor = Palette.Text,
    inactiveBorderColor = Palette.Line,
)

@Composable
private fun PresetListSection(state: AppState) {
    val snackbar = LocalSnackbar.current
    val scope = rememberCoroutineScope()
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
            colors = OutlinedTextFieldDefaults.colors(unfocusedBorderColor = Palette.Track),
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
    val scope = rememberCoroutineScope()
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
                        s.readNBytes(MAX_IMPORT_BYTES).decodeToString()
                    }
                    val name = resolver.query(uri, arrayOf(OpenableColumns.DISPLAY_NAME), null, null, null)
                        ?.use { c -> if (c.moveToFirst()) c.getString(0) else null }
                    Triple(AutoEqParser.parse(text), ShapeText.parseGraphicEq(text), name)
                }
            }
            val (parsed, curve, name) = result.getOrElse {
                notify(snackbar, appText(R.string.ui_could_not_read_that_file, it.message))
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
            notify(snackbar, if (written.isSuccess) appText(R.string.ui_backup_saved) else appText(R.string.ui_could_not_save_backup, written.exceptionOrNull()?.message))
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
            notify(snackbar, if (written.isSuccess) appText(R.string.ui_eq_file_saved) else appText(R.string.ui_could_not_save_file, written.exceptionOrNull()?.message))
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
            notify(snackbar, if (written.isSuccess) appText(R.string.ui_eq_file_saved) else appText(R.string.ui_could_not_save_file, written.exceptionOrNull()?.message))
        }
    }
    val restorer = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri: Uri? ->
        if (uri == null) return@rememberLauncherForActivityResult
        scope.launch {
            val text = withContext(Dispatchers.IO) {
                runCatching {
                    context.contentResolver.openInputStream(uri)!!.use { it.readNBytes(MAX_BACKUP_BYTES).decodeToString() }
                }.getOrNull()
            }
            notify(
                snackbar,
                when {
                    text == null -> appText(R.string.ui_could_not_read_that_file_2)
                    Repo.restoreBackup(text) -> appText(R.string.ui_backup_restored)
                    else -> appText(R.string.ui_that_isn_t_an_eigentone_backup)
                },
            )
        }
    }

    SettingsSection(stringResource(R.string.ui_this_preset)) {
        ActionRow(stringResource(R.string.ui_save_as_new), Help.saveNew) { saving = true }
        ShareRows { message -> scope.notify(snackbar, message) }
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
        SaveDialog(onDismiss = { saving = false }) { name ->
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
    var query by remember { mutableStateOf("") }
    var busy by remember { mutableStateOf(false) }
    var measurements by remember { mutableStateOf<List<CatalogEntry>?>(null) }

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

    LaunchedEffect(Unit) {
        val loaded = withContext(Dispatchers.IO) { runCatching { AutoEqCatalog.loadIndex(context.cacheDir) } }
        entries = loaded.getOrNull()
        problem = loaded.exceptionOrNull()?.let { appText(R.string.ui_could_not_load_the_headphone_list, it.message) }
    }

    val results = remember(entries, query) { AutoEqCatalog.search(entries.orEmpty(), query) }
    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = Palette.Tooltip,
        title = { Text(stringResource(R.string.ui_find_headphones)) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(
                    value = query,
                    onValueChange = { query = it },
                    singleLine = true,
                    label = { Text(stringResource(R.string.ui_headphone_model)) },
                    enabled = entries != null && !busy,
                )
                when {
                    problem != null -> Text(problem.orEmpty(), color = Palette.Danger, style = MaterialTheme.typography.bodySmall)
                    entries == null -> Text(stringResource(R.string.ui_loading_the_list), color = Palette.TextDim)
                    busy -> Text(stringResource(R.string.ui_downloading), color = Palette.TextDim)
                    query.isNotBlank() && results.isEmpty() -> Text(stringResource(R.string.ui_no_matches), color = Palette.TextDim)
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
                                        measurements = same
                                        return@clickable
                                    }
                                    busy = true
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
    var name by remember { mutableStateOf(com.phonons.app.data.PresetOrganiser.freeName(state, initial).takeIf { initial.isNotBlank() } ?: "") }
    val taken = com.phonons.app.data.PresetOrganiser.nameTaken(state, name)
    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = Palette.Tooltip,
        title = { Text(title) },
        text = {
            OutlinedTextField(
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
            val modes = ThemeMode.entries
            WordSegments(
                labels = listOf(
                    stringResource(R.string.ui_theme_device),
                    stringResource(R.string.ui_theme_light),
                    stringResource(R.string.ui_theme_dark),
                ),
                selected = modes.indexOf(state.themeMode),
                onSelect = { i -> Repo.update { it.copy(themeMode = modes[i]) } },
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
            SingleChoiceSegmentedButtonRow(Modifier.fillMaxWidth()) {
                modes.forEachIndexed { i, mode ->
                    SegmentedButton(
                        selected = state.engineMode == mode,
                        onClick = { Repo.update { it.copy(engineMode = mode) } },
                        shape = SegmentedButtonDefaults.itemShape(i, modes.size, KeyShape),
                        colors = segmentColors(),
                        icon = {},
                    ) { Text(engineModeLabel(mode), maxLines = 1) }
                }
            }
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
                context.startActivity(
                    Intent(
                        Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS,
                        Uri.parse("package:${context.packageName}"),
                    ),
                )
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
        AlertDialog(
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

    DiagnosticsSection(status, output)

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
                color = Palette.Lit,
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
    AlertDialog(
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
            .heightIn(min = 48.dp)
            .padding(start = 16.dp, end = 16.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(label, style = MaterialTheme.typography.bodyMedium, color = Palette.Text)
        InfoButton(tip)
        Text(
            value,
            Modifier
                .weight(1f)
                .padding(start = 8.dp),
            style = Readout,
            color = Palette.TextDim,
            textAlign = androidx.compose.ui.text.style.TextAlign.End,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
        )
    }
}
