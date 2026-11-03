package com.phonons.app.ui

import com.phonons.app.R
import androidx.compose.ui.res.stringResource
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.phonons.app.data.AbComparison
import com.phonons.app.data.BuiltIns
import com.phonons.app.data.Preset
import com.phonons.app.data.Repo
import kotlin.random.Random

private enum class CompareStep { CHOOSE, LISTEN, REVEAL }

/** Id standing for the sound in use when the screen opened, which may be unsaved. */
private const val CURRENT = "current"

/**
 * Switches between two sounds at matched loudness, optionally without knowing which is which. It changes
 * the live sound while it's open and puts everything back unless one is chosen, including if the screen
 * is rebuilt, when the sound is applied again from the saved choices.
 */
@Composable
fun CompareScreen(onClose: () -> Unit) {
    val state by Repo.state.collectAsStateWithLifecycle()
    val before = remember { Repo.state.value }
    var finished by remember { mutableStateOf(false) }

    var step by rememberSaveable { mutableStateOf(CompareStep.CHOOSE) }
    var firstId by rememberSaveable { mutableStateOf(CURRENT) }
    var secondId by rememberSaveable { mutableStateOf<String?>(null) }
    var choosingFirst by rememberSaveable { mutableStateOf(false) }
    var blind by rememberSaveable { mutableStateOf(false) }
    var flipped by rememberSaveable { mutableStateOf(false) }
    var playing by rememberSaveable { mutableIntStateOf(0) }
    var preferred by rememberSaveable { mutableIntStateOf(0) }

    fun find(id: String?): Preset? = if (id == CURRENT) before.current else id?.let(Repo::findPreset)
    val first = find(firstId)
    val second = find(secondId)
    val comparison = if (first != null && second != null) AbComparison(first, second, blind, flipped, before.current) else null

    DisposableEffect(Unit) {
        Repo.comparing.value = true
        onDispose {
            Repo.comparing.value = false
            if (!finished) Repo.update { it.copy(current = before.current, bypass = before.bypass) }
        }
    }

    LaunchedEffect(step, playing, comparison, state.autoHeadroom) {
        if (step == CompareStep.CHOOSE || comparison == null) return@LaunchedEffect
        val sound = comparison.soundFor(playing, state.autoHeadroom)
        Repo.update { it.copy(current = sound, bypass = false) }
    }

    fun cancel() {
        onClose()
    }

    /** Leaves the chosen sound in place as one undo step, without the comparison's level change. */
    fun use(preset: Preset) {
        finished = true
        Repo.update { it.copy(current = before.current, bypass = before.bypass) }
        if (preset != before.current) Repo.loadPreset(preset)
        onClose()
    }

    BackHandler {
        if (step == CompareStep.CHOOSE) cancel() else step = CompareStep.CHOOSE
    }

    ToolScreen(title = stringResource(R.string.ui_compare_two_sounds), onBack = { if (step == CompareStep.CHOOSE) cancel() else step = CompareStep.CHOOSE }, spacing = 16.dp) {
        when (step) {
            CompareStep.CHOOSE -> Choose(
                current = before.current,
                firstId = firstId,
                secondId = secondId,
                choosingFirst = choosingFirst,
                blind = blind,
                onChooseSlot = { choosingFirst = it },
                onPick = { id ->
                    if (choosingFirst) {
                        firstId = id
                        choosingFirst = false
                    } else {
                        secondId = id
                    }
                },
                onBlind = { blind = it },
                canStart = comparison != null && first != second,
                onStart = {
                    flipped = Random.nextBoolean()
                    playing = 0
                    step = CompareStep.LISTEN
                },
                onCancel = ::cancel,
            )
            CompareStep.LISTEN -> comparison?.let { active ->
                Listen(
                    comparison = active,
                    playing = playing,
                    silent = !state.enabled,
                    onPlay = { playing = it },
                    onPrefer = { button ->
                        if (active.blind) {
                            preferred = button
                            playing = button
                            step = CompareStep.REVEAL
                        } else {
                            use(active.presetFor(button))
                        }
                    },
                    onCancel = ::cancel,
                )
            }
            CompareStep.REVEAL -> comparison?.let { active ->
                Reveal(
                    chosen = active.nameOf(active.presetFor(preferred)),
                    other = active.nameOf(active.presetFor(1 - preferred)),
                    wasCurrent = active.presetFor(preferred) == before.current,
                    onUse = { use(active.presetFor(preferred)) },
                    onCancel = ::cancel,
                )
            }
        }
    }
}

@Composable
private fun Choose(
    current: Preset,
    firstId: String,
    secondId: String?,
    choosingFirst: Boolean,
    blind: Boolean,
    onChooseSlot: (Boolean) -> Unit,
    onPick: (String) -> Unit,
    onBlind: (Boolean) -> Unit,
    canStart: Boolean,
    onStart: () -> Unit,
    onCancel: () -> Unit,
) {
    val state by Repo.state.collectAsStateWithLifecycle()
    val connected = rememberConnectedOutputNames()
    val presets = remember(connected, state.presets) { BuiltIns.availableFor(connected) + state.presets }
    /** The saved version of a preset that's been edited sits beside the edited one, so it says which it is. */
    fun label(preset: Preset) = if (preset.id == current.id) appText(R.string.ui_saved, preset.name) else preset.name
    fun nameOf(id: String?) = when (id) {
        null -> appText(R.string.ui_choose_below)
        CURRENT -> appText(R.string.ui_my_current_sound)
        else -> presets.firstOrNull { it.id == id }?.let(::label) ?: appText(R.string.ui_choose_below)
    }
    // Comparing something with itself wouldn't tell anyone anything, so neither slot offers the other's sound.
    val otherId = if (choosingFirst) secondId else firstId

    Text(
        stringResource(R.string.ui_switch_between_them_while_your_music_pla),
        style = MaterialTheme.typography.bodyLarge,
        color = Palette.TextDim,
    )
    Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
        Slot("A", nameOf(firstId), choosingFirst, Modifier.weight(1f)) { onChooseSlot(true) }
        Slot("B", nameOf(secondId), !choosingFirst, Modifier.weight(1f)) { onChooseSlot(false) }
    }
    SettingsSection(stringResource(if (choosingFirst) R.string.ui_choose_a else R.string.ui_choose_b)) {
        @Composable
        fun Option(id: String, name: String) {
            Row(
                Modifier
                    .fillMaxWidth()
                    .heightIn(min = 52.dp)
                    .selectable(
                        selected = (if (choosingFirst) firstId else secondId) == id,
                        role = Role.RadioButton,
                        onClick = { onPick(id) },
                    )
                    .padding(horizontal = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                RadioButton(selected = (if (choosingFirst) firstId else secondId) == id, onClick = null, modifier = Modifier.padding(8.dp))
                Text(name, style = MaterialTheme.typography.bodyLarge, color = Palette.Text, maxLines = 1, overflow = TextOverflow.Ellipsis)
            }
        }
        if (otherId != CURRENT) Option(CURRENT, stringResource(R.string.ui_my_current_sound_2, current.name))
        presets.filter { it.id != otherId && (otherId != CURRENT || it != current) }.forEachIndexed { index, preset ->
            if (index > 0 || otherId != CURRENT) RowDivider()
            Option(preset.id, label(preset))
        }
    }
    SettingsSection(stringResource(R.string.ui_options)) {
        SwitchSetting(stringResource(R.string.ui_blind_comparison), Help.compareBlind, blind, onChange = onBlind)
    }
    Button(enabled = canStart, onClick = onStart, modifier = Modifier.fillMaxWidth().heightIn(min = 52.dp)) { Text(stringResource(R.string.ui_start_comparing)) }
    TextButton(onClick = onCancel, modifier = Modifier.heightIn(min = 48.dp)) { Text(stringResource(R.string.ui_cancel)) }
}

@Composable
private fun Slot(label: String, name: String, active: Boolean, modifier: Modifier, onClick: () -> Unit) {
    Column(
        modifier
            .clip(PanelShape)
            .background(if (active) Palette.SurfaceHigh else Palette.Surface)
            .clickable(role = Role.Button, onClick = onClick)
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        Text(label, style = MaterialTheme.typography.titleMedium, color = if (active) Palette.Lit else Palette.TextDim)
        Text(name, style = MaterialTheme.typography.bodyMedium, color = Palette.Text, maxLines = 2, overflow = TextOverflow.Ellipsis)
    }
}

@Composable
private fun Listen(
    comparison: AbComparison,
    playing: Int,
    silent: Boolean,
    onPlay: (Int) -> Unit,
    onPrefer: (Int) -> Unit,
    onCancel: () -> Unit,
) {
    Text(
        if (comparison.blind) stringResource(R.string.ui_which_do_you_prefer) else stringResource(R.string.ui_compare),
        style = MaterialTheme.typography.headlineSmall,
        color = Palette.Text,
    )
    Text(
        if (comparison.blind) {
            stringResource(R.string.ui_the_buttons_are_in_a_random)
        } else {
            stringResource(R.string.ui_switch_back_and_forth_ideally_on)
        },
        style = MaterialTheme.typography.bodyLarge,
        color = Palette.TextDim,
    )
    if (silent) {
        Text(stringResource(R.string.ui_eigentone_is_off_so_both_will_sound_the), style = MaterialTheme.typography.bodyMedium, color = Palette.Danger)
    } else {
        NothingPlayingHint()
    }
    val musicActive = rememberMusicActive()
    Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
        for (button in 0..1) {
            SideCard(comparison.label(button), playing == button, musicActive, Modifier.weight(1f)) { onPlay(button) }
        }
    }
    for (button in 0..1) {
        val name = comparison.nameFor(button)
        FilledTonalButton(
            onClick = { onPrefer(button) },
            modifier = Modifier.fillMaxWidth().heightIn(min = 52.dp),
        ) {
            Text(
                if (name != null) stringResource(R.string.ui_use, comparison.label(button), name) else stringResource(R.string.ui_i_prefer, comparison.label(button)),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
    }
    TextButton(onClick = onCancel, modifier = Modifier.heightIn(min = 48.dp)) { Text(stringResource(R.string.ui_keep_what_i_had)) }
}

@Composable
private fun Reveal(chosen: String, other: String, wasCurrent: Boolean, onUse: () -> Unit, onCancel: () -> Unit) {
    Text(stringResource(R.string.ui_you_preferred, chosen), style = MaterialTheme.typography.headlineSmall, color = Palette.Text)
    Text(
        stringResource(R.string.ui_the_other_one_was_a_single, other),
        style = MaterialTheme.typography.bodyLarge,
        color = Palette.TextDim,
    )
    if (wasCurrent) {
        Button(onClick = onCancel, modifier = Modifier.fillMaxWidth().heightIn(min = 52.dp)) { Text(stringResource(R.string.ui_done)) }
    } else {
        Button(onClick = onUse, modifier = Modifier.fillMaxWidth().heightIn(min = 52.dp)) { Text(stringResource(R.string.ui_use_2, chosen), maxLines = 1, overflow = TextOverflow.Ellipsis) }
        TextButton(onClick = onCancel, modifier = Modifier.heightIn(min = 48.dp)) { Text(stringResource(R.string.ui_keep_what_i_had)) }
    }
}
