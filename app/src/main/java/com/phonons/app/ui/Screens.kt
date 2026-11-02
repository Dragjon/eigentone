package com.phonons.app.ui

import android.annotation.SuppressLint
import android.content.Intent
import android.net.Uri
import android.os.PowerManager
import android.provider.OpenableColumns
import android.provider.Settings
import android.Manifest
import android.content.pm.PackageManager
import androidx.annotation.DrawableRes
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Slider
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
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
import com.phonons.app.engine.SpectrumAnalyser
import com.phonons.app.dsp.AutoEqParser
import com.phonons.app.dsp.Eq
import com.phonons.app.dsp.Filter
import com.phonons.app.dsp.FilterType
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

@Composable
fun PhononsApp(onSetEnabled: (Boolean) -> Unit) {
    val state by Repo.state.collectAsStateWithLifecycle()
    val status by Repo.status.collectAsStateWithLifecycle()
    var tab by rememberSaveable { mutableStateOf(Tab.EQ) }

    Scaffold(
        containerColor = Palette.Background,
        bottomBar = {
            NavigationBar(containerColor = Palette.Surface) {
                Tab.entries.forEach { t ->
                    NavigationBarItem(
                        selected = tab == t,
                        onClick = { tab = t },
                        // The label below already names the tab for screen readers.
                        icon = { Icon(painterResource(t.icon), contentDescription = null) },
                        label = { Text(t.label) },
                    )
                }
            }
        },
    ) { padding ->
        Column(
            Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            Header(state, status, onSetEnabled)
            when (tab) {
                Tab.EQ -> EqTab(state)
                Tab.SOUND -> SoundTab(state)
                Tab.PRESETS -> PresetsTab(state)
                Tab.SYSTEM -> SystemTab(state, status)
            }
        }
    }
}

@Composable
private fun Header(state: AppState, status: EngineStatus, onSetEnabled: (Boolean) -> Unit) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Column(Modifier.weight(1f)) {
            Text("Phonons", style = MaterialTheme.typography.headlineSmall)
            val problem = state.enabled && (status.error != null || !status.hasControl)
            Text(
                statusLine(state, status),
                style = MaterialTheme.typography.bodySmall,
                color = if (problem) Palette.Danger else Palette.TextDim,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
        if (state.enabled) {
            TextButton(onClick = { Repo.update { it.copy(bypass = !it.bypass) } }) {
                Text(if (state.bypass) "Resume" else "Bypass")
            }
        }
        Switch(checked = state.enabled, onCheckedChange = onSetEnabled)
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
        state.bypass -> "Bypassed. $name"
        else -> "${status.mode}. $name"
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
            .clip(RoundedCornerShape(12.dp))
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

    Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
        OutlinedButton(
            enabled = filters.size < MAX_FILTERS,
            onClick = {
                val id = (filters.maxOfOrNull { it.id } ?: 0) + 1
                Repo.edit { it.copy(filters = it.filters + Filter(id)) }
                Repo.commit()
                selectedId = id
            },
        ) { Text("Add filter") }
        OutlinedButton(enabled = canUndo, onClick = Repo::undo) { Text("Undo") }
        Spacer(Modifier.weight(1f))
        Text("${filters.size} filters", style = MaterialTheme.typography.bodySmall, color = Palette.TextDim)
    }
    AnalyserRow(state)

    if (selected == null) {
        Text(
            if (filters.isEmpty()) "No filters yet. Add one to start shaping the sound."
            else "Tap a point on the curve to edit it.",
            color = Palette.TextDim,
        )
    } else {
        FilterEditor(selected, onDelete = {
            Repo.edit { p -> p.copy(filters = p.filters.filterNot { it.id == selected.id }) }
            Repo.commit()
            selectedId = Repo.state.value.current.filters.firstOrNull()?.id
        })
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

@Composable
private fun AnalyserRow(state: AppState) {
    val permission = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
        if (granted) Repo.update { it.copy(analyser = true) }
    }
    val context = LocalContext.current
    SwitchRow(
        "Spectrum analyser",
        when {
            analyserUnavailable.value -> "This phone refused the analyser."
            state.analyser -> "Shows live levels behind the curve. The lowest bands are coarse."
            else -> "Android asks for microphone permission, but only levels of playing audio are read."
        },
        state.analyser,
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
private fun FilterEditor(filter: Filter, onDelete: () -> Unit) {
    Card {
        Row(
            Modifier
                .fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text("Filter", style = MaterialTheme.typography.titleMedium, modifier = Modifier.weight(1f))
            Text("On", style = MaterialTheme.typography.bodySmall, color = Palette.TextDim)
            Spacer(Modifier.width(8.dp))
            Switch(checked = filter.enabled, onCheckedChange = { on ->
                updateFilter(filter.id) { it.copy(enabled = on) }
                Repo.commit()
            })
        }

        Row(horizontalArrangement = Arrangement.spacedBy(6.dp), modifier = Modifier.fillMaxWidth()) {
            TypeChip(filter, FilterType.BELL, "Bell", Modifier.weight(1f))
            TypeChip(filter, FilterType.LOW_SHELF, "Lo shelf", Modifier.weight(1.1f))
            TypeChip(filter, FilterType.HIGH_SHELF, "Hi shelf", Modifier.weight(1.1f))
        }
        Row(horizontalArrangement = Arrangement.spacedBy(6.dp), modifier = Modifier.fillMaxWidth()) {
            TypeChip(filter, FilterType.HIGH_PASS, "High pass", Modifier.weight(1f))
            TypeChip(filter, FilterType.LOW_PASS, "Low pass", Modifier.weight(1f))
        }

        ValueSlider(
            label = "Frequency",
            readout = formatHz(filter.freq),
            value = Eq.logPosition(filter.freq).toFloat(),
            range = 0f..1f,
            onChange = { t -> updateFilter(filter.id) { it.copy(freq = Eq.logFreq(t.toDouble())) } },
        )
        if (filter.type.hasGain) {
            ValueSlider(
                label = "Gain",
                readout = formatDb(filter.gainDb),
                value = filter.gainDb.toFloat(),
                range = -Eq.MAX_GAIN.toFloat()..Eq.MAX_GAIN.toFloat(),
                onChange = { g -> updateFilter(filter.id) { it.copy(gainDb = g.toDouble()) } },
            )
        }
        ValueSlider(
            label = "Q",
            readout = String.format(Locale.US, "%.2f", filter.q),
            value = qPosition(filter.q),
            range = 0f..1f,
            onChange = { t -> updateFilter(filter.id) { it.copy(q = qFromPosition(t)) } },
        )
        TextButton(onClick = onDelete) { Text("Delete filter", color = Palette.Danger) }
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
        label = { Text(label, maxLines = 1) },
        colors = FilterChipDefaults.filterChipColors(
            selectedContainerColor = Palette.Amber,
            selectedLabelColor = Palette.OnAmber,
        ),
    )
}

private fun qPosition(q: Double): Float =
    (log10(q / Eq.MIN_Q) / log10(Eq.MAX_Q / Eq.MIN_Q)).toFloat()

private fun qFromPosition(t: Float): Double = Eq.MIN_Q * (Eq.MAX_Q / Eq.MIN_Q).pow(t.toDouble())

private fun formatHz(hz: Double): String =
    if (hz >= 1000) String.format(Locale.US, "%.2f kHz", hz / 1000) else String.format(Locale.US, "%.0f Hz", hz)

private fun formatDb(db: Double): String = String.format(Locale.US, "%+.1f dB", db)

@Composable
private fun SoundTab(state: AppState) {
    val attenuation by Repo.attenuationDb.collectAsStateWithLifecycle()
    val effective = Processing.effectiveFilters(state, attenuation)
    val input = Eq.inputGainDb(effective, state.current.preampDb, state.autoHeadroom)
    var showGraphic by rememberSaveable { mutableStateOf(false) }
    Card {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text("Tone", style = MaterialTheme.typography.titleMedium, modifier = Modifier.weight(1f))
            if (state.bassDb != 0.0 || state.trebleDb != 0.0) {
                TextButton(onClick = { Repo.update { it.copy(bassDb = 0.0, trebleDb = 0.0) } }) { Text("Reset") }
            }
        }
        ValueSlider(
            label = "Bass",
            readout = formatDb(state.bassDb),
            value = state.bassDb.toFloat(),
            range = -Processing.MAX_TONE_DB.toFloat()..Processing.MAX_TONE_DB.toFloat(),
            onChange = { v -> Repo.update { it.copy(bassDb = snapTone(v)) } },
            commit = false,
        )
        ValueSlider(
            label = "Treble",
            readout = formatDb(state.trebleDb),
            value = state.trebleDb.toFloat(),
            range = -Processing.MAX_TONE_DB.toFloat()..Processing.MAX_TONE_DB.toFloat(),
            onChange = { v -> Repo.update { it.copy(trebleDb = snapTone(v)) } },
            commit = false,
        )
        Text(
            "Adds to every preset, without changing the preset itself.",
            style = MaterialTheme.typography.bodySmall,
            color = Palette.TextDim,
        )
    }
    SurroundCard(state)
    Card {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text("Graphic EQ", style = MaterialTheme.typography.titleMedium, modifier = Modifier.weight(1f))
            if (state.graphicDb.any { it != 0.0 }) {
                TextButton(onClick = { Repo.update { it.copy(graphicDb = emptyList()) } }) { Text("Reset") }
            }
            TextButton(onClick = { showGraphic = !showGraphic }) { Text(if (showGraphic) "Hide" else "Show") }
        }
        if (showGraphic) {
            Processing.GRAPHIC_BANDS.forEachIndexed { i, freq ->
                val gain = state.graphicDb.getOrElse(i) { 0.0 }
                ValueSlider(
                    label = formatHz(freq),
                    readout = formatDb(gain),
                    value = gain.toFloat(),
                    range = -Processing.MAX_TONE_DB.toFloat()..Processing.MAX_TONE_DB.toFloat(),
                    onChange = { v ->
                        Repo.update { s ->
                            val gains = List(Processing.GRAPHIC_BANDS.size) { s.graphicDb.getOrElse(it) { 0.0 } }
                            s.copy(graphicDb = gains.toMutableList().also { it[i] = snapTone(v) })
                        }
                    },
                    commit = false,
                )
            }
        }
    }
    Card {
        Text("Bass tuner", style = MaterialTheme.typography.titleMedium)
        ValueSlider(
            label = "Amount",
            readout = formatDb(state.bassTunerDb),
            value = state.bassTunerDb.toFloat(),
            range = -Processing.MAX_TONE_DB.toFloat()..Processing.MAX_TONE_DB.toFloat(),
            onChange = { v -> Repo.update { it.copy(bassTunerDb = snapTone(v)) } },
            commit = false,
        )
        ValueSlider(
            label = "Up to",
            readout = formatHz(state.bassTunerHz),
            value = state.bassTunerHz.toFloat(),
            range = 30f..250f,
            onChange = { v -> Repo.update { it.copy(bassTunerHz = v.toDouble()) } },
            commit = false,
        )
    }
    Card {
        Text("Loudness compensation", style = MaterialTheme.typography.titleMedium)
        SwitchRow(
            "Adapt to volume",
            "Adds bass and treble as the volume goes down, because ears hear less of them when it's quiet.",
            state.loudness,
        ) { on -> Repo.update { it.copy(loudness = on) } }
        if (state.loudness) {
            ValueSlider(
                label = "Strength",
                readout = "${(state.loudnessStrength * 100).toInt()}%",
                value = state.loudnessStrength.toFloat(),
                range = 0f..1f,
                onChange = { v -> Repo.update { it.copy(loudnessStrength = v.toDouble()) } },
                commit = false,
            )
            val added = Processing.loudnessFilters(attenuation, state.loudnessStrength)
            Text(
                if (added.isEmpty()) {
                    "Nothing added at this volume."
                } else {
                    "Now adding ${formatDb(added[0].gainDb)} bass and ${formatDb(added[1].gainDb)} treble."
                },
                style = MaterialTheme.typography.bodySmall,
                color = Palette.TextDim,
            )
        }
    }
    Card {
        Text("Level", style = MaterialTheme.typography.titleMedium)
        ValueSlider(
            label = "Preamp",
            readout = formatDb(state.current.preampDb),
            value = state.current.preampDb.toFloat(),
            range = -24f..12f,
            onChange = { v -> Repo.edit { it.copy(preampDb = v.toDouble()) } },
        )
        SwitchRow(
            "Automatic headroom",
            "Lowers the level by your largest boost to prevent clipping. Applied now: ${formatDb(input)}",
            state.autoHeadroom,
        ) { on -> Repo.update { it.copy(autoHeadroom = on) } }
        SwitchRow(
            "Limiter",
            "Catches peaks above ${String.format(Locale.US, "%.1f", state.limiterThresholdDb)} dB.",
            state.limiter,
        ) { on -> Repo.update { it.copy(limiter = on) } }
        if (state.limiter) {
            ValueSlider(
                label = "Limiter threshold",
                readout = formatDb(state.limiterThresholdDb),
                value = state.limiterThresholdDb.toFloat(),
                range = -12f..0f,
                onChange = { v -> Repo.update { it.copy(limiterThresholdDb = snapTone(v)) } },
                commit = false,
            )
        }
        SwitchRow(
            "Level-matched Bypass",
            "Keeps Bypass at about the same loudness, so comparisons are about tone. This is an estimate.",
            state.matchBypass,
        ) { on -> Repo.update { it.copy(matchBypass = on) } }
    }
    Card {
        Text("Dynamics", style = MaterialTheme.typography.titleMedium)
        ValueSlider(
            label = "Night mode",
            readout = if (state.nightMode <= 0.001) "Off" else "${(state.nightMode * 100).toInt()}%",
            value = state.nightMode.toFloat(),
            range = 0f..1f,
            onChange = { v -> Repo.update { it.copy(nightMode = v.toDouble()) } },
            commit = false,
        )
        Text(
            "Brings quiet and loud parts closer together, so you can listen at low volume.",
            style = MaterialTheme.typography.bodySmall,
            color = Palette.TextDim,
        )
    }
    Card {
        Text("Stereo", style = MaterialTheme.typography.titleMedium)
        ValueSlider(
            label = "Balance",
            readout = when {
                state.balance < -0.005 -> "L ${(-state.balance * 100).toInt()}%"
                state.balance > 0.005 -> "R ${(state.balance * 100).toInt()}%"
                else -> "Centre"
            },
            value = state.balance.toFloat(),
            range = -1f..1f,
            onChange = { v -> Repo.update { it.copy(balance = v.toDouble()) } },
            commit = false,
        )
    }
}

@Composable
private fun SurroundCard(state: AppState) {
    val status by Repo.status.collectAsStateWithLifecycle()
    Card {
        Text("3D surround", style = MaterialTheme.typography.titleMedium)
        ValueSlider(
            label = "Virtual surround",
            readout = if (state.surround <= 0.0) "Off" else "${(state.surround * 100).toInt()}%",
            value = state.surround.toFloat(),
            range = 0f..1f,
            onChange = { v -> Repo.update { it.copy(surround = if (v < 0.02f) 0.0 else v.toDouble()) } },
            commit = false,
        )
        Text(
            "Widens the stereo image. It doesn't track your head or create real surround channels.",
            style = MaterialTheme.typography.bodySmall,
            color = Palette.TextDim,
        )
        status.spatialError?.let {
            Text(it, style = MaterialTheme.typography.bodySmall, color = Palette.Danger)
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
                                .padding(vertical = 10.dp),
                            style = MaterialTheme.typography.bodyMedium,
                        )
                    }
                }
            }
        },
        confirmButton = { TextButton(onClick = onDismiss) { Text("Close") } },
    )
}

/** Half-dB steps, which also makes it easy to land on exactly zero. */
private fun snapTone(value: Float): Double = kotlin.math.round(value * 2.0) / 2.0

@Composable
private fun PresetsTab(state: AppState) {
    val context = LocalContext.current
    val output by Repo.output.collectAsStateWithLifecycle()
    val scope = rememberCoroutineScope()
    var saving by remember { mutableStateOf(false) }
    var finding by remember { mutableStateOf(false) }
    var message by remember { mutableStateOf<String?>(null) }
    val isUser = state.presets.any { it.id == state.current.id }
    val isDirty = isUser && state.presets.first { it.id == state.current.id } != state.current

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
                message = "Could not read that file: ${it.message}"
                return@launch
            }
            if (parsed == null) {
                message = "No EQ filters found. Expected an AutoEq or Equalizer APO ParametricEQ.txt file."
            } else {
                val title = name?.substringBeforeLast('.')?.removeSuffix(" ParametricEQ")?.ifBlank { null }
                Repo.importPreset(title ?: "Imported EQ", parsed)
                message = "Imported ${parsed.filters.size} filters."
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
            message = if (written.isSuccess) "Backup saved." else "Could not save backup: ${written.exceptionOrNull()?.message}"
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
            message = when {
                text == null -> "Could not read that file."
                Repo.restoreBackup(text) -> "Backup restored."
                else -> "That isn't a Phonons backup."
            }
        }
    }

    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        Button(onClick = { saving = true }) { Text("Save as new") }
        if (isDirty) OutlinedButton(onClick = Repo::overwriteCurrent) { Text("Update") }
        OutlinedButton(onClick = { importer.launch(arrayOf("text/*", "application/octet-stream")) }) { Text("Import") }
    }
    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        OutlinedButton(onClick = { finding = true }) { Text("Find") }
        OutlinedButton(onClick = { exporter.launch("phonons-backup.json") }) { Text("Back up") }
        OutlinedButton(onClick = { restorer.launch(arrayOf("application/json", "text/*", "application/octet-stream")) }) {
            Text("Restore")
        }
    }
    message?.let { Text(it, style = MaterialTheme.typography.bodySmall, color = Palette.TextDim) }

    Card {
        Text("For this output", style = MaterialTheme.typography.titleMedium)
        val supportsModes = Profiles.supportsModes(output.key)
        val assignKey = if (supportsModes) Profiles.modeKey(output.key, state.listeningMode) else output.key
        val assigned = Profiles.presetIdFor(state, output.key)?.let(Repo::findPreset)
        if (supportsModes) {
            Text("Earbud noise mode", style = MaterialTheme.typography.bodyMedium)
            val modes = ListeningMode.entries
            SingleChoiceSegmentedButtonRow(Modifier.fillMaxWidth()) {
                modes.forEachIndexed { i, mode ->
                    SegmentedButton(
                        selected = state.listeningMode == mode,
                        onClick = { Repo.setListeningMode(mode) },
                        shape = SegmentedButtonDefaults.itemShape(i, modes.size),
                        icon = {},
                    ) { Text(mode.label, maxLines = 1, overflow = TextOverflow.Ellipsis, style = MaterialTheme.typography.labelMedium) }
                }
            }
            Text(
                "Phonons can't read this from your earbuds, so set it to match. A mode with no preset of its own uses the output's main one.",
                style = MaterialTheme.typography.bodySmall,
                color = Palette.TextDim,
            )
        }
        Text(
            "${output.label}: ${assigned?.name ?: "no preset assigned"}",
            style = MaterialTheme.typography.bodyMedium,
            color = Palette.TextDim,
        )
        val correction = BuiltIns.correctionFor(output.key)
        if (assigned == null && correction != null) {
            Button(onClick = {
                Repo.assignToOutput(output.key, correction.id)
                Repo.loadPreset(correction)
            }) { Text("Use ${correction.name} correction") }
        }
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            OutlinedButton(
                enabled = Repo.findPreset(state.current.id) != null,
                onClick = { Repo.assignToOutput(assignKey, state.current.id) },
            ) { Text("Use ${state.current.name}", maxLines = 1, overflow = TextOverflow.Ellipsis) }
            if (assigned != null) {
                // Clears the mode's own choice first, then the output's main one.
                val clearKey = if (assignKey in state.outputProfiles) assignKey else output.key
                TextButton(onClick = { Repo.assignToOutput(clearKey, null) }) { Text("Clear") }
            }
        }
        SwitchRow("Switch automatically", "Loads the assigned preset when this output connects.", state.autoSwitch) { on ->
            Repo.update { it.copy(autoSwitch = on) }
        }
    }

    Column {
        Repo.allPresets(state).forEachIndexed { i, preset ->
            if (i > 0) HorizontalDivider(color = Palette.Line)
            PresetRow(preset, selected = preset.id == state.current.id, favorite = preset.id in state.favorites)
        }
    }
    Text(
        "The notification's Next preset button moves through your starred presets, or all of them if none are starred.",
        style = MaterialTheme.typography.bodySmall,
        color = Palette.TextDim,
    )

    if (saving) SaveDialog(onDismiss = { saving = false }) { name ->
        Repo.saveCurrentAs(name)
        saving = false
    }
    if (finding) {
        AutoEqDialog(onDismiss = { finding = false }) { result ->
            message = result
            finding = false
        }
    }
}

private const val MAX_IMPORT_BYTES = 256 * 1024
private const val MAX_BACKUP_BYTES = 1024 * 1024

@Composable
private fun PresetRow(preset: Preset, selected: Boolean, favorite: Boolean) {
    val isBuiltIn = BuiltIns.all.any { it.id == preset.id }
    Row(
        Modifier
            .fillMaxWidth()
            .clickable(role = Role.Button) { Repo.loadPreset(preset) }
            .padding(vertical = 14.dp, horizontal = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            Modifier
                .size(8.dp)
                .clip(RoundedCornerShape(4.dp))
                .background(if (selected) Palette.Amber else Color.Transparent),
        )
        Spacer(Modifier.width(12.dp))
        Text(preset.name, Modifier.weight(1f), maxLines = 1, overflow = TextOverflow.Ellipsis)
        TextButton(onClick = { Repo.toggleFavorite(preset.id) }) {
            Text(
                if (favorite) "★" else "☆",
                color = if (favorite) Palette.Amber else Palette.TextDim,
                modifier = Modifier.semantics {
                    contentDescription = if (favorite) "Remove ${preset.name} from favourites" else "Add ${preset.name} to favourites"
                },
            )
        }
        if (!isBuiltIn) {
            TextButton(onClick = { Repo.deletePreset(preset.id) }) { Text("Delete", color = Palette.Danger) }
        }
    }
}

@Composable
private fun SaveDialog(onDismiss: () -> Unit, onSave: (String) -> Unit) {
    var name by remember { mutableStateOf("") }
    AlertDialog(
        onDismissRequest = onDismiss,
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

    Card {
        Text("Engine", style = MaterialTheme.typography.titleMedium)
        val modes = EngineMode.entries
        SingleChoiceSegmentedButtonRow(Modifier.fillMaxWidth()) {
            modes.forEachIndexed { i, mode ->
                SegmentedButton(
                    selected = state.engineMode == mode,
                    onClick = { Repo.update { it.copy(engineMode = mode) } },
                    shape = SegmentedButtonDefaults.itemShape(i, modes.size),
                ) { Text(mode.label) }
            }
        }
        Text(
            "Global covers every app that Android allows. If the EQ makes no difference, try Per app: it covers " +
                "players that announce their audio, and they must start after Phonons.",
            style = MaterialTheme.typography.bodySmall,
            color = Palette.TextDim,
        )
        SwitchRow(
            "Low latency",
            "Reduces delay for games and video, with less precise bass control.",
            state.lowLatency,
        ) { on -> Repo.update { it.copy(lowLatency = on) } }
        SwitchRow(
            "Start automatically",
            "Turns the EQ back on after a phone restart or update, if it was on before.",
            state.startOnBoot,
        ) { on -> Repo.update { it.copy(startOnBoot = on) } }
    }

    Card {
        Text("Status", style = MaterialTheme.typography.titleMedium)
        Fact("Running", if (status.running) "Yes" else "No")
        Fact("Mode", status.mode)
        Fact("EQ bands", if (status.bandCount > 0) "${status.bandCount}" else "None")
        Fact("Has control", if (status.hasControl) "Yes" else "No")
        Fact("Output", output.label)
        if (status.sessions.isNotEmpty()) Fact("Attached apps", status.sessions.joinToString())
        if (status.bandCount in 1 until 64) {
            Text(
                "This phone only allowed ${status.bandCount} bands, so narrow filters will be less accurate.",
                style = MaterialTheme.typography.bodySmall,
                color = Palette.Amber,
            )
        }
        status.error?.let {
            Text(it, style = MaterialTheme.typography.bodySmall, color = Palette.Danger)
        }
    }

    if (!exempt) {
        Card {
            Text("Keep running", style = MaterialTheme.typography.titleMedium)
            Text(
                "Battery optimisation may stop the EQ in the background.",
                style = MaterialTheme.typography.bodyMedium,
                color = Palette.TextDim,
            )
            OutlinedButton(onClick = {
                context.startActivity(
                    Intent(
                        Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS,
                        Uri.parse("package:${context.packageName}"),
                    ),
                )
            }) { Text("Allow background use") }
        }
    }
}

@Composable
private fun Fact(label: String, value: String) {
    Row(Modifier.fillMaxWidth()) {
        Text(label, Modifier.weight(1f), style = MaterialTheme.typography.bodyMedium, color = Palette.TextDim)
        Text(value, style = Readout)
    }
}

@Composable
private fun Card(content: @Composable ColumnScope.() -> Unit) {
    Column(
        Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(Palette.Surface)
            .padding(14.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
        content = content,
    )
}

@Composable
private fun ValueSlider(
    label: String,
    readout: String,
    value: Float,
    range: ClosedFloatingPointRange<Float>,
    onChange: (Float) -> Unit,
    commit: Boolean = true,
) {
    Column {
        Row(Modifier.fillMaxWidth()) {
            Text(label, Modifier.weight(1f), style = MaterialTheme.typography.bodyMedium)
            Text(readout, style = Readout, color = Palette.Amber)
        }
        Slider(
            value = value.coerceIn(range),
            valueRange = range,
            onValueChange = onChange,
            onValueChangeFinished = if (commit) Repo::commit else null,
            modifier = Modifier.height(36.dp),
        )
    }
}

@Composable
private fun SwitchRow(title: String, detail: String, checked: Boolean, onChange: (Boolean) -> Unit) {
    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        Column(Modifier.weight(1f).padding(end = 12.dp)) {
            Text(title, style = MaterialTheme.typography.bodyMedium)
            Text(detail, style = MaterialTheme.typography.bodySmall, color = Palette.TextDim)
        }
        Switch(checked = checked, onCheckedChange = onChange)
    }
}
