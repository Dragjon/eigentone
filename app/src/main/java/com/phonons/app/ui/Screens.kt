package com.phonons.app.ui

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
import androidx.compose.foundation.ScrollState
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowLeft
import androidx.compose.material.icons.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
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
import androidx.compose.material3.TextButton
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
import androidx.compose.ui.Modifier
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

private enum class Tab(val label: String, @DrawableRes val icon: Int) {
    EQ("EQ", R.drawable.ic_tab_eq),
    SOUND("Sound", R.drawable.ic_tab_sound),
    PRESETS("Presets", R.drawable.ic_tab_presets),
    SYSTEM("System", R.drawable.ic_tab_system),
}

private const val MAX_FILTERS = 20
private const val MAX_IMPORT_BYTES = 256 * 1024
private const val MAX_BACKUP_BYTES = 1024 * 1024

/** Brief feedback for actions, shown by the screen's snackbar. */
private val LocalSnackbar = compositionLocalOf<SnackbarHostState> { error("No snackbar host") }

@Composable
fun PhononsApp(onSetEnabled: (Boolean) -> Unit) {
    val state by Repo.state.collectAsStateWithLifecycle()
    val status by Repo.status.collectAsStateWithLifecycle()
    var tab by rememberSaveable { mutableStateOf(Tab.EQ) }
    val snackbar = remember { SnackbarHostState() }
    // Each tab keeps its own position, so switching never lands partway down another page.
    val scrolls: Map<Tab, ScrollState> = Tab.entries.associateWith { rememberScrollState() }

    CompositionLocalProvider(LocalSnackbar provides snackbar) {
        Scaffold(
            containerColor = Palette.Background,
            topBar = { TopBar(state, status, onSetEnabled) },
            snackbarHost = { SnackbarHost(snackbar) },
            bottomBar = {
                NavigationBar(containerColor = Palette.Surface) {
                    Tab.entries.forEach { t ->
                        NavigationBarItem(
                            selected = tab == t,
                            onClick = { tab = t },
                            // The label below already names the tab for screen readers.
                            icon = { Icon(painterResource(t.icon), contentDescription = null) },
                            label = { Text(t.label) },
                            colors = NavigationBarItemDefaults.colors(
                                selectedIconColor = Palette.OnAmber,
                                selectedTextColor = Palette.Text,
                                indicatorColor = Palette.Amber,
                                unselectedIconColor = Palette.TextDim,
                                unselectedTextColor = Palette.TextDim,
                            ),
                        )
                    }
                }
            },
        ) { padding ->
            Box(
                Modifier
                    .fillMaxSize()
                    .padding(padding),
                contentAlignment = Alignment.TopCenter,
            ) {
                Column(
                    Modifier
                        .widthIn(max = 640.dp)
                        .fillMaxSize()
                        .verticalScroll(scrolls.getValue(tab))
                        .padding(start = 16.dp, end = 16.dp, top = 4.dp, bottom = 16.dp),
                    verticalArrangement = SectionSpacing,
                ) {
                    when (tab) {
                        Tab.EQ -> EqTab(state)
                        Tab.SOUND -> SoundTab(state)
                        Tab.PRESETS -> PresetsTab(state)
                        Tab.SYSTEM -> SystemTab(state, status)
                    }
                }
            }
        }
    }
}

/** Stays on screen, so power and Bypass are always one tap away. */
@Composable
private fun TopBar(state: AppState, status: EngineStatus, onSetEnabled: (Boolean) -> Unit) {
    Row(
        Modifier
            .fillMaxWidth()
            .background(Palette.Background)
            .windowInsetsPadding(WindowInsets.statusBars)
            .padding(start = 16.dp, end = 12.dp, top = 8.dp, bottom = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(Modifier.weight(1f)) {
            Text("Phonons", style = MaterialTheme.typography.titleLarge, color = Palette.Text)
            val problem = state.enabled && (status.error != null || !status.hasControl)
            Text(
                statusLine(state, status),
                style = MaterialTheme.typography.bodySmall,
                color = if (problem) Palette.Danger else Palette.TextDim,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
        if (state.enabled) BypassButton(state.bypass)
        InfoButton(Help.power)
        Switch(
            checked = state.enabled,
            onCheckedChange = onSetEnabled,
            modifier = Modifier.semantics { contentDescription = "Phonons" },
        )
    }
}

/** Distinct from the power switch: neutral until engaged, when it's clearly a held state. */
@Composable
private fun BypassButton(bypassed: Boolean) {
    val toggle = { Repo.update { it.copy(bypass = !it.bypass) } }
    if (bypassed) {
        Button(onClick = { toggle() }) { Text("Bypassed") }
    } else {
        FilledTonalButton(onClick = { toggle() }) { Text("Bypass") }
    }
}

private fun statusLine(state: AppState, status: EngineStatus): String {
    val saved = Repo.allPresets(state).firstOrNull { it.id == state.current.id }
    val name = if (saved == state.current) state.current.name else "${state.current.name} (edited)"
    return when {
        !state.enabled -> "Off"
        status.error != null -> "Problem. See the System tab"
        !status.running -> "Starting…"
        !status.hasControl -> "Another audio app has taken control"
        else -> name
    }
}

@Composable
private fun EqTab(state: AppState) {
    val filters = state.current.filters
    var selectedId by rememberSaveable { mutableStateOf<Int?>(filters.firstOrNull()?.id) }
    val canUndo by Repo.canUndo.collectAsStateWithLifecycle()
    val selected = filters.firstOrNull { it.id == selectedId }
    val attenuation by Repo.attenuationDb.collectAsStateWithLifecycle()
    val curveFilters = remember(state, attenuation) { Processing.effectiveFilters(state, attenuation) }
    val spectrumState = Repo.spectrum.collectAsStateWithLifecycle()
    AnalyserEffect(state.analyser)

    Box(
        Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(Palette.Surface),
    ) {
        EqGraph(
            filters = filters,
            curveFilters = curveFilters,
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
            Text("Add filter")
        }
        FilledTonalButton(enabled = canUndo, onClick = Repo::undo) {
            Icon(painterResource(R.drawable.ic_undo), contentDescription = null, modifier = Modifier.size(18.dp))
            Spacer(Modifier.size(8.dp))
            Text("Undo")
        }
    }

    if (selected == null) {
        Text(
            if (filters.isEmpty()) "No filters yet. Add one to start shaping the sound."
            else "Tap a point on the curve to edit it.",
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

    SettingsSection("Display") { AnalyserSetting(state) }
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

@Composable
private fun AnalyserSetting(state: AppState) {
    val permission = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
        if (granted) Repo.update { it.copy(analyser = true) }
    }
    val context = LocalContext.current
    SwitchSetting(
        "Spectrum analyser",
        Help.analyser,
        state.analyser,
        detail = if (analyserUnavailable.value) "This phone refused the analyser." else null,
    ) { on ->
        val granted = context.checkSelfPermission(Manifest.permission.RECORD_AUDIO) ==
            PackageManager.PERMISSION_GRANTED
        if (on && !granted) {
            permission.launch(Manifest.permission.RECORD_AUDIO)
        } else {
            Repo.update { it.copy(analyser = on) }
        }
    }
}

private fun updateFilter(id: Int, transform: (Filter) -> Filter) {
    Repo.edit { p -> p.copy(filters = p.filters.map { if (it.id == id) transform(it) else it }) }
}

@Composable
private fun FilterEditor(filter: Filter, filters: List<Filter>, onSelect: (Int) -> Unit, onDelete: () -> Unit) {
    val index = filters.indexOf(filter)
    SettingsSection(
        "Filter ${index + 1} of ${filters.size}",
        trailing = {
            // Nodes can overlap on the graph, so every filter is also reachable from here.
            IconButton(enabled = index > 0, onClick = { onSelect(filters[index - 1].id) }) {
                Icon(Icons.Filled.KeyboardArrowLeft, contentDescription = "Previous filter")
            }
            IconButton(enabled = index < filters.lastIndex, onClick = { onSelect(filters[index + 1].id) }) {
                Icon(Icons.Filled.KeyboardArrowRight, contentDescription = "Next filter")
            }
            IconButton(onClick = onDelete) {
                Icon(Icons.Filled.Delete, contentDescription = "Delete filter", tint = Palette.Danger)
            }
        },
    ) {
        SwitchSetting("Enabled", Help.filterEnabled, filter.enabled) { on ->
            updateFilter(filter.id) { it.copy(enabled = on) }
            Repo.commit()
        }
        RowDivider()
        Column(
            Modifier.padding(start = 16.dp, end = 16.dp, top = 4.dp, bottom = 8.dp),
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("Type", style = MaterialTheme.typography.bodyLarge, color = Palette.Text)
                InfoButton(Help.filterType)
            }
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp), modifier = Modifier.fillMaxWidth()) {
                TypeChip(filter, FilterType.BELL, "Bell", Modifier.weight(1f))
                TypeChip(filter, FilterType.LOW_SHELF, "Low shelf", Modifier.weight(1.2f))
                TypeChip(filter, FilterType.HIGH_SHELF, "High shelf", Modifier.weight(1.2f))
            }
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp), modifier = Modifier.fillMaxWidth()) {
                TypeChip(filter, FilterType.HIGH_PASS, "High pass", Modifier.weight(1f))
                TypeChip(filter, FilterType.LOW_PASS, "Low pass", Modifier.weight(1f))
            }
        }
        RowDivider()
        SliderSetting(
            title = "Frequency",
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
                title = "Gain",
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
            title = "Q",
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
            selectedContainerColor = Palette.Amber,
            selectedLabelColor = Palette.OnAmber,
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

@Composable
private fun SoundTab(state: AppState) {
    val attenuation by Repo.attenuationDb.collectAsStateWithLifecycle()
    ToneSection(state)
    SpaceSection(state)
    DynamicsSection(state, attenuation)
    LevelSection(state, attenuation)
}

@Composable
private fun ToneSection(state: AppState) {
    var showGraphic by rememberSaveable { mutableStateOf(false) }
    val modified = state.bassDb != 0.0 || state.trebleDb != 0.0 || state.bassTunerDb != 0.0 ||
        state.graphicDb.any { it != 0.0 }
    SettingsSection(
        "Tone",
        trailing = {
            if (modified) {
                TextButton(onClick = {
                    Repo.update { it.copy(bassDb = 0.0, trebleDb = 0.0, bassTunerDb = 0.0, graphicDb = emptyList()) }
                }) { Text("Reset tone") }
            }
        },
    ) {
        SliderSetting(
            title = "Bass",
            tip = Help.bass,
            readout = formatDb(state.bassDb),
            value = state.bassDb.toFloat(),
            range = toneRange,
            default = 0f,
            origin = 0f,
            onChange = { v -> Repo.update { it.copy(bassDb = snapTone(v)) } },
        )
        RowDivider()
        SliderSetting(
            title = "Treble",
            tip = Help.treble,
            readout = formatDb(state.trebleDb),
            value = state.trebleDb.toFloat(),
            range = toneRange,
            default = 0f,
            origin = 0f,
            onChange = { v -> Repo.update { it.copy(trebleDb = snapTone(v)) } },
        )
        RowDivider()
        SliderSetting(
            title = "Bass tuner",
            tip = Help.bassTunerAmount,
            readout = formatDb(state.bassTunerDb),
            value = state.bassTunerDb.toFloat(),
            range = toneRange,
            default = 0f,
            origin = 0f,
            onChange = { v -> Repo.update { it.copy(bassTunerDb = snapTone(v)) } },
        )
        SliderSetting(
            title = "Bass tuner frequency",
            tip = Help.bassTunerFrequency,
            readout = formatHz(state.bassTunerHz),
            value = state.bassTunerHz.toFloat(),
            range = 30f..250f,
            default = 80f,
            onChange = { v -> Repo.update { it.copy(bassTunerHz = v.toDouble()) } },
        )
        RowDivider()
        Row(
            Modifier
                .fillMaxWidth()
                .heightIn(min = 56.dp)
                .clickable(role = Role.Button) { showGraphic = !showGraphic }
                .padding(start = 16.dp, end = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text("Graphic EQ", style = MaterialTheme.typography.bodyLarge, color = Palette.Text)
            InfoButton(Help.graphic)
            Spacer(Modifier.weight(1f))
            Icon(
                if (showGraphic) Icons.Filled.KeyboardArrowUp else Icons.Filled.KeyboardArrowDown,
                contentDescription = if (showGraphic) "Hide graphic EQ" else "Show graphic EQ",
                tint = Palette.TextDim,
            )
        }
        if (showGraphic) {
            Processing.GRAPHIC_BANDS.forEachIndexed { i, freq ->
                val gain = state.graphicDb.getOrElse(i) { 0.0 }
                GraphicBand(i, freq, gain)
            }
        }
    }
}

@Composable
private fun GraphicBand(index: Int, freq: Double, gain: Double) {
    SliderSetting(
        title = formatHz(freq),
        tip = Help.graphic,
        readout = formatDb(gain),
        value = gain.toFloat(),
        range = toneRange,
        default = 0f,
        origin = 0f,
        onChange = { v ->
            Repo.update { s ->
                val gains = List(Processing.GRAPHIC_BANDS.size) { s.graphicDb.getOrElse(it) { 0.0 } }
                s.copy(graphicDb = gains.toMutableList().also { it[index] = snapTone(v) })
            }
        },
    )
}

@Composable
private fun SpaceSection(state: AppState) {
    val status by Repo.status.collectAsStateWithLifecycle()
    SettingsSection("Space") {
        SliderSetting(
            title = "Virtual surround",
            tip = Help.surround,
            readout = if (state.surround <= 0.0) "Off" else formatPercent(state.surround),
            value = state.surround.toFloat(),
            range = 0f..1f,
            default = 0f,
            detail = status.spatialError,
            onChange = { v -> Repo.update { it.copy(surround = if (v < 0.02f) 0.0 else v.toDouble()) } },
        )
        RowDivider()
        SliderSetting(
            title = "Balance",
            tip = Help.balance,
            readout = when {
                state.balance < -0.005 -> "L ${formatPercent(-state.balance)}"
                state.balance > 0.005 -> "R ${formatPercent(state.balance)}"
                else -> "Centre"
            },
            value = state.balance.toFloat(),
            range = -1f..1f,
            default = 0f,
            origin = 0f,
            onChange = { v -> Repo.update { it.copy(balance = v.toDouble()) } },
        )
    }
}

@Composable
private fun DynamicsSection(state: AppState, attenuation: Double) {
    SettingsSection("Dynamics") {
        SwitchSetting("Adapt to volume", Help.loudness, state.loudness) { on ->
            Repo.update { it.copy(loudness = on) }
        }
        if (state.loudness) {
            val added = Processing.loudnessFilters(attenuation, state.loudnessStrength)
            SliderSetting(
                title = "Strength",
                tip = Help.loudnessStrength,
                readout = formatPercent(state.loudnessStrength),
                value = state.loudnessStrength.toFloat(),
                range = 0f..1f,
                default = 0.5f,
                detail = if (added.isEmpty()) {
                    "Nothing added at this volume."
                } else {
                    "Now adding ${formatDb(added[0].gainDb)} bass and ${formatDb(added[1].gainDb)} treble."
                },
                onChange = { v -> Repo.update { it.copy(loudnessStrength = v.toDouble()) } },
            )
        }
        RowDivider()
        SliderSetting(
            title = "Night mode",
            tip = Help.nightMode,
            readout = if (state.nightMode <= 0.001) "Off" else formatPercent(state.nightMode),
            value = state.nightMode.toFloat(),
            range = 0f..1f,
            default = 0f,
            onChange = { v -> Repo.update { it.copy(nightMode = v.toDouble()) } },
        )
    }
}

@Composable
private fun LevelSection(state: AppState, attenuation: Double) {
    val effective = Processing.effectiveFilters(state, attenuation)
    val input = Eq.inputGainDb(effective, state.current.preampDb, state.autoHeadroom)
    SettingsSection("Level") {
        SliderSetting(
            title = "Preamp",
            tip = Help.preamp,
            readout = formatDb(state.current.preampDb),
            value = state.current.preampDb.toFloat(),
            range = -24f..12f,
            default = 0f,
            origin = 0f,
            onChange = { v -> Repo.edit { it.copy(preampDb = v.toDouble()) } },
            onFinished = Repo::commit,
        )
        RowDivider()
        SwitchSetting(
            "Automatic headroom",
            Help.headroom,
            state.autoHeadroom,
            detail = "Level going into the EQ: ${formatDb(input)}",
        ) { on -> Repo.update { it.copy(autoHeadroom = on) } }
        RowDivider()
        SwitchSetting("Limiter", Help.limiter, state.limiter) { on -> Repo.update { it.copy(limiter = on) } }
        if (state.limiter) {
            SliderSetting(
                title = "Limiter threshold",
                tip = Help.limiterThreshold,
                readout = formatDb(state.limiterThresholdDb),
                value = state.limiterThresholdDb.toFloat(),
                range = -12f..0f,
                default = -1f,
                origin = 0f,
                onChange = { v -> Repo.update { it.copy(limiterThresholdDb = snapTone(v)) } },
            )
        }
        RowDivider()
        SwitchSetting("Level-matched Bypass", Help.matchBypass, state.matchBypass) { on ->
            Repo.update { it.copy(matchBypass = on) }
        }
    }
}

/** Shows a message through the screen's snackbar. */
private fun CoroutineScope.notify(snackbar: SnackbarHostState, message: String) {
    launch { snackbar.showSnackbar(message, withDismissAction = true, duration = SnackbarDuration.Short) }
}

@Composable
private fun PresetsTab(state: AppState) {
    val output by Repo.output.collectAsStateWithLifecycle()
    OutputSection(state, output.key, output.label)
    PresetListSection(state)
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

    SettingsSection(outputLabel) {
        Column(Modifier.padding(start = 16.dp, end = 16.dp, top = 4.dp, bottom = 12.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("Assigned preset", style = MaterialTheme.typography.bodyLarge, color = Palette.Text)
                InfoButton(Help.outputPreset)
            }
            Text(
                assigned?.name ?: "None",
                style = MaterialTheme.typography.bodyMedium,
                color = if (assigned != null) Palette.Amber else Palette.TextDim,
                modifier = Modifier.padding(bottom = 10.dp),
            )
            if (assigned == null && correction != null) {
                Button(
                    onClick = {
                        Repo.assignToOutput(outputKey, correction.id)
                        Repo.loadPreset(correction)
                    },
                    modifier = Modifier.padding(bottom = 8.dp),
                ) { Text("Use ${correction.name} correction") }
            }
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                FilledTonalButton(
                    enabled = canAssign && !alreadyAssigned,
                    onClick = { Repo.assignToOutput(assignKey, state.current.id) },
                    modifier = Modifier.weight(1f, fill = false),
                ) {
                    Text(
                        if (alreadyAssigned) "Current preset assigned" else "Assign ${state.current.name}",
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
                if (assigned != null) {
                    // Clears the mode's own choice first, then the output's main one.
                    val clearKey = if (assignKey in state.outputProfiles) assignKey else outputKey
                    TextButton(onClick = { Repo.assignToOutput(clearKey, null) }) { Text("Clear") }
                }
            }
        }
        if (supportsModes) {
            RowDivider()
            Column(Modifier.padding(start = 16.dp, end = 16.dp, top = 4.dp, bottom = 12.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("Earbud noise mode", style = MaterialTheme.typography.bodyLarge, color = Palette.Text)
                    InfoButton(Help.noiseMode)
                }
                val modes = ListeningMode.entries
                SingleChoiceSegmentedButtonRow(Modifier.fillMaxWidth()) {
                    modes.forEachIndexed { i, mode ->
                        SegmentedButton(
                            selected = state.listeningMode == mode,
                            onClick = { Repo.setListeningMode(mode) },
                            shape = SegmentedButtonDefaults.itemShape(i, modes.size),
                            colors = segmentColors(),
                            icon = {},
                        ) { Text(mode.label, maxLines = 1, style = MaterialTheme.typography.labelMedium) }
                    }
                }
            }
        }
        RowDivider()
        SwitchSetting("Switch automatically", Help.autoSwitch, state.autoSwitch) { on ->
            Repo.update { it.copy(autoSwitch = on) }
        }
    }
}

@Composable
private fun segmentColors() = SegmentedButtonDefaults.colors(
    activeContainerColor = Palette.Amber,
    activeContentColor = Palette.OnAmber,
    activeBorderColor = Palette.Amber,
    inactiveContainerColor = Palette.Surface,
    inactiveContentColor = Palette.Text,
    inactiveBorderColor = Palette.Outline,
)

@Composable
private fun PresetListSection(state: AppState) {
    val snackbar = LocalSnackbar.current
    val scope = rememberCoroutineScope()
    val (headphones, general) = BuiltIns.all.partition { it.id.startsWith("builtin:liberty") }

    @Composable
    fun Group(title: String, presets: List<Preset>, deletable: Boolean, tip: Tip? = null) {
        if (presets.isEmpty()) return
        SettingsSection(title, trailing = tip?.let { { InfoButton(it) } }) {
            presets.forEachIndexed { i, preset ->
                if (i > 0) RowDivider()
                PresetRow(
                    preset = preset,
                    selected = preset.id == state.current.id,
                    favorite = preset.id in state.favorites,
                    onDelete = if (deletable) {
                        {
                            val before = Repo.state.value
                            Repo.deletePreset(preset.id)
                            scope.launch {
                                val result = snackbar.showSnackbar(
                                    "Deleted ${preset.name}",
                                    actionLabel = "Undo",
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

    Group("Your presets", state.presets, deletable = true, tip = Help.favorites)
    Group("Liberty 4 NC correction", headphones, deletable = false, tip = if (state.presets.isEmpty()) Help.favorites else null)
    Group("General", general, deletable = false)
}

@Composable
private fun PresetRow(preset: Preset, selected: Boolean, favorite: Boolean, onDelete: (() -> Unit)?) {
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
                Icon(Icons.Filled.Check, contentDescription = "Selected", tint = Palette.Amber, modifier = Modifier.size(20.dp))
            }
        }
        Text(
            preset.name,
            Modifier.weight(1f),
            style = MaterialTheme.typography.bodyLarge,
            color = if (selected) Palette.Amber else Palette.Text,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
        )
        IconButton(onClick = { Repo.toggleFavorite(preset.id) }) {
            Icon(
                // Filled against outlined, so the state doesn't rely on colour alone.
                if (favorite) painterResource(R.drawable.ic_star) else painterResource(R.drawable.ic_star_border),
                contentDescription = if (favorite) "Remove ${preset.name} from favourites" else "Add ${preset.name} to favourites",
                tint = if (favorite) Palette.Amber else Palette.TextDim,
            )
        }
        if (onDelete != null) {
            IconButton(onClick = onDelete) {
                Icon(Icons.Filled.Delete, contentDescription = "Delete ${preset.name}", tint = Palette.Danger)
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
    var finding by remember { mutableStateOf(false) }
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
                    Pair(AutoEqParser.parse(text), name)
                }
            }
            val (parsed, name) = result.getOrElse {
                notify(snackbar, "Could not read that file: ${it.message}")
                return@launch
            }
            if (parsed == null) {
                notify(snackbar, "No EQ filters found. Choose an AutoEq or Equalizer APO ParametricEQ.txt file.")
            } else {
                val title = name?.substringBeforeLast('.')?.removeSuffix(" ParametricEQ")?.ifBlank { null }
                Repo.importPreset(title ?: "Imported EQ", parsed)
                notify(snackbar, "Imported ${parsed.filters.size} filters.")
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
            notify(snackbar, if (written.isSuccess) "Backup saved." else "Could not save backup: ${written.exceptionOrNull()?.message}")
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
                    text == null -> "Could not read that file."
                    Repo.restoreBackup(text) -> "Backup restored."
                    else -> "That isn't a Phonons backup."
                },
            )
        }
    }

    SettingsSection("Add and manage") {
        ActionRow("Save as new", Help.saveNew) { saving = true }
        if (saved != null && saved != state.current) {
            RowDivider()
            ActionRow("Update ${saved.name}", Help.update) {
                Repo.overwriteCurrent()
                scope.notify(snackbar, "Updated ${saved.name}.")
            }
        }
        RowDivider()
        ActionRow("Find headphones", Help.find) { finding = true }
        RowDivider()
        ActionRow("Import EQ file", Help.importFile) {
            importer.launch(arrayOf("text/*", "application/octet-stream"))
        }
        RowDivider()
        ActionRow("Back up presets", Help.backup) { exporter.launch("phonons-backup.json") }
        RowDivider()
        ActionRow("Restore presets", Help.restore) {
            restorer.launch(arrayOf("application/json", "text/*", "application/octet-stream"))
        }
    }

    if (saving) {
        SaveDialog(onDismiss = { saving = false }) { name ->
            Repo.saveCurrentAs(name)
            saving = false
        }
    }
    if (finding) {
        AutoEqDialog(onDismiss = { finding = false }) { result ->
            scope.notify(snackbar, result)
            finding = false
        }
    }
}

/** Finds and imports a headphone profile from AutoEq's published results. */
@Composable
private fun AutoEqDialog(onDismiss: () -> Unit, onImported: (String) -> Unit) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var entries by remember { mutableStateOf<List<CatalogEntry>?>(null) }
    var problem by remember { mutableStateOf<String?>(null) }
    var query by remember { mutableStateOf("") }
    var busy by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        val loaded = withContext(Dispatchers.IO) { runCatching { AutoEqCatalog.loadIndex(context.cacheDir) } }
        entries = loaded.getOrNull()
        problem = loaded.exceptionOrNull()?.let { "Could not load the headphone list: ${it.message}" }
    }

    val results = remember(entries, query) { AutoEqCatalog.search(entries.orEmpty(), query) }
    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = Palette.Tooltip,
        title = { Text("Find headphones") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(
                    value = query,
                    onValueChange = { query = it },
                    singleLine = true,
                    label = { Text("Headphone model") },
                    enabled = entries != null && !busy,
                )
                when {
                    problem != null -> Text(problem.orEmpty(), color = Palette.Danger, style = MaterialTheme.typography.bodySmall)
                    entries == null -> Text("Loading the list…", color = Palette.TextDim)
                    busy -> Text("Downloading…", color = Palette.TextDim)
                    query.isNotBlank() && results.isEmpty() -> Text("No matches.", color = Palette.TextDim)
                }
                LazyColumn(Modifier.heightIn(max = 320.dp)) {
                    items(results) { entry ->
                        Text(
                            entry.title,
                            modifier = Modifier
                                .fillMaxWidth()
                                .heightIn(min = 48.dp)
                                .clickable(enabled = !busy, role = Role.Button) {
                                    busy = true
                                    scope.launch {
                                        val text = withContext(Dispatchers.IO) {
                                            runCatching { AutoEqCatalog.loadProfile(entry) }
                                        }
                                        val parsed = text.getOrNull()?.let(AutoEqParser::parse)
                                        busy = false
                                        if (parsed == null) {
                                            problem = "Could not download that profile. It may have no parametric version."
                                        } else {
                                            Repo.importPreset(entry.title, parsed)
                                            onImported("Added ${entry.title}.")
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
        confirmButton = { TextButton(onClick = onDismiss) { Text("Close") } },
    )
}

@Composable
private fun SaveDialog(onDismiss: () -> Unit, onSave: (String) -> Unit) {
    var name by remember { mutableStateOf("") }
    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = Palette.Tooltip,
        title = { Text("Save preset") },
        text = {
            OutlinedTextField(value = name, onValueChange = { name = it }, singleLine = true, label = { Text("Name") })
        },
        confirmButton = {
            TextButton(enabled = name.isNotBlank(), onClick = { onSave(name.trim()) }) { Text("Save") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } },
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

    SettingsSection("Engine") {
        Column(Modifier.padding(start = 16.dp, end = 16.dp, top = 4.dp, bottom = 12.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("Audio route", style = MaterialTheme.typography.bodyLarge, color = Palette.Text)
                InfoButton(Help.engineMode)
            }
            val modes = EngineMode.entries
            SingleChoiceSegmentedButtonRow(Modifier.fillMaxWidth()) {
                modes.forEachIndexed { i, mode ->
                    SegmentedButton(
                        selected = state.engineMode == mode,
                        onClick = { Repo.update { it.copy(engineMode = mode) } },
                        shape = SegmentedButtonDefaults.itemShape(i, modes.size),
                        colors = segmentColors(),
                        icon = {},
                    ) { Text(mode.label, maxLines = 1) }
                }
            }
        }
        RowDivider()
        SwitchSetting("Low latency", Help.lowLatency, state.lowLatency) { on ->
            Repo.update { it.copy(lowLatency = on) }
        }
        RowDivider()
        SwitchSetting("Start automatically", Help.startOnBoot, state.startOnBoot) { on ->
            Repo.update { it.copy(startOnBoot = on) }
        }
        if (!exempt) {
            RowDivider()
            ActionRow("Allow background use", Help.battery, detail = "Battery optimisation may stop the EQ when locked.") {
                context.startActivity(
                    Intent(
                        Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS,
                        Uri.parse("package:${context.packageName}"),
                    ),
                )
            }
        }
    }

    SettingsSection("Status") {
        Fact("Running", Help.statusRunning, if (status.running) "Yes" else "No")
        RowDivider()
        Fact("Mode", Help.statusMode, status.mode)
        RowDivider()
        Fact("EQ bands", Help.statusBands, if (status.bandCount > 0) "${status.bandCount}" else "None")
        RowDivider()
        Fact("In control", Help.statusControl, if (status.hasControl) "Yes" else "No")
        RowDivider()
        Fact("Output", Help.statusOutput, output.label)
        if (status.sessions.isNotEmpty()) {
            RowDivider()
            Fact("Attached apps", Help.statusApps, status.sessions.joinToString())
        }
        if (status.bandCount in 1 until 64) {
            RowDivider()
            Text(
                "This phone only allowed ${status.bandCount} bands, so narrow filters will be less accurate.",
                style = MaterialTheme.typography.bodySmall,
                color = Palette.Amber,
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
