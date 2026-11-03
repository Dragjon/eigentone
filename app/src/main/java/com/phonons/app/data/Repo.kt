package com.phonons.app.data

import android.content.Context
import android.util.AtomicFile
import android.util.Log
import com.phonons.app.dsp.ParsedEq
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.drop
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.update
import kotlinx.serialization.json.Json
import java.io.File
import java.util.UUID

/** Single source of truth shared by the UI, the service and the Quick Settings tile. */
object Repo {
    private const val TAG = "PhononsRepo"
    private const val MAX_UNDO = 50

    private val json = Json { ignoreUnknownKeys = true; encodeDefaults = true }
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    private val _state = MutableStateFlow(AppState())
    val state: StateFlow<AppState> = _state.asStateFlow()

    val status = MutableStateFlow(EngineStatus())
    val output = MutableStateFlow(OutputInfo("speaker", "Phone speaker"))

    /** How far the music volume is below full, in dB. Drives loudness compensation. */
    val attenuationDb = MutableStateFlow(0.0)
    val spectrum = MutableStateFlow<FloatArray?>(null)

    /** The hearing check is running, so the EQ steps aside. Never saved, so it can't get stuck on. */
    val hearingCheckRunning = MutableStateFlow(false)

    /** Two sounds are being compared, so switching between them skips the morph. Never saved. */
    val comparing = MutableStateFlow(false)

    /** Whether the time of day is inside the night mode schedule. Set by the service. */
    val scheduledNight = MutableStateFlow(false)

    /** The app playing audio, when notification access has been given. Set by the service. */
    val playingApp = MutableStateFlow<String?>(null)

    private val _canUndo = MutableStateFlow(false)
    val canUndo: StateFlow<Boolean> = _canUndo.asStateFlow()

    private val undoStack = ArrayDeque<Preset>()
    private val redoStack = ArrayDeque<Preset>()

    private val _canRedo = MutableStateFlow(false)
    val canRedo: StateFlow<Boolean> = _canRedo.asStateFlow()
    private var committed: Preset = BuiltIns.flat
    private var file: AtomicFile? = null

    @OptIn(FlowPreview::class)
    fun init(context: Context) {
        if (file != null) return
        val atomic = AtomicFile(File(context.filesDir, "state.json"))
        file = atomic
        val saved = load(atomic)
        _state.value = saved?.migrated() ?: AppState(stateVersion = AppState.CURRENT_STATE_VERSION)
        committed = _state.value.current
        _state.drop(1).debounce(400).onEach { save(it) }.launchIn(scope)
    }

    private fun load(atomic: AtomicFile): AppState? = try {
        if (atomic.baseFile.exists()) {
            json.decodeFromString<AppState>(atomic.readFully().decodeToString())
        } else {
            null
        }
    } catch (e: Exception) {
        Log.w(TAG, "Saved state unreadable, using defaults", e)
        null
    }

    private fun save(value: AppState) {
        val atomic = file ?: return
        val stream = try {
            atomic.startWrite()
        } catch (e: Exception) {
            Log.w(TAG, "Could not open state file", e)
            return
        }
        try {
            stream.write(json.encodeToString(AppState.serializer(), value).encodeToByteArray())
            atomic.finishWrite(stream)
        } catch (e: Exception) {
            Log.w(TAG, "Could not save state", e)
            atomic.failWrite(stream)
        }
    }

    fun update(transform: (AppState) -> AppState) = _state.update(transform)

    /** Live edit of the working preset, e.g. during a drag. Call [commit] when the gesture ends. */
    fun edit(transform: (Preset) -> Preset) = _state.update { it.copy(current = transform(it.current)) }

    /** Live edit of the tone adjustments saved with the current preset. */
    fun editTone(transform: (Tone) -> Tone) = edit { it.copy(tone = transform(it.tone)) }

    /** Records one undo step for everything edited since the previous commit. */
    @Synchronized
    fun commit() {
        val now = _state.value.current
        if (now == committed) return
        undoStack.addLast(committed)
        if (undoStack.size > MAX_UNDO) undoStack.removeFirst()
        committed = now
        _canUndo.value = true
        // A new change starts a new history, so what was undone can no longer be brought back.
        redoStack.clear()
        _canRedo.value = false
    }

    @Synchronized
    fun undo() {
        commit()
        val previous = undoStack.removeLastOrNull() ?: return
        redoStack.addLast(committed)
        committed = previous
        _state.update { it.copy(current = previous) }
        _canUndo.value = undoStack.isNotEmpty()
        _canRedo.value = true
    }

    @Synchronized
    fun redo() {
        // Anything edited since the undo starts a new history, which clears what could be redone.
        commit()
        val next = redoStack.removeLastOrNull() ?: return
        undoStack.addLast(committed)
        committed = next
        _state.update { it.copy(current = next) }
        _canUndo.value = true
        _canRedo.value = redoStack.isNotEmpty()
    }

    fun loadPreset(preset: Preset) {
        commit()
        _state.update { it.copy(current = preset) }
        commit()
    }

    fun allPresets(state: AppState = _state.value): List<Preset> = BuiltIns.all + state.presets

    fun findPreset(id: String): Preset? = allPresets().firstOrNull { it.id == id }

    fun saveCurrentAs(name: String) {
        val preset = _state.value.current.copy(id = "user:${UUID.randomUUID()}", name = name)
        _state.update { it.copy(current = preset, presets = it.presets + preset) }
        commit()
    }

    /** Overwrites the user preset the working copy came from. Built-ins cannot be overwritten. */
    fun overwriteCurrent() {
        _state.update { s ->
            s.copy(presets = s.presets.map { if (it.id == s.current.id) s.current else it })
        }
    }

    /** Renames a user preset, including the working copy if it's the one in use. */
    fun renamePreset(id: String, name: String) {
        _state.update { s -> s.renamedPreset(id, name) }
    }

    /** Files a user preset in a folder, including the working copy if it's the one in use. */
    fun moveToFolder(id: String, folder: String?) {
        val cleaned = PresetOrganiser.cleanFolder(folder)
        _state.update { s ->
            s.copy(
                presets = s.presets.map { if (it.id == id) it.copy(folder = cleaned) else it },
                current = if (s.current.id == id) s.current.copy(folder = cleaned) else s.current,
            )
        }
    }

    /** Returns the sound to a clean start. Presets and output choices are kept. */
    fun resetSound() {
        commit()
        _state.update { it.resetSound() }
        commit()
    }

    fun deletePreset(id: String) {
        _state.update { s ->
            s.copy(
                presets = s.presets.filterNot { it.id == id },
                outputProfiles = s.outputProfiles.filterValues { it != id },
            )
        }
    }

    fun importPreset(name: String, parsed: ParsedEq) {
        val preset = Preset("user:${UUID.randomUUID()}", name, parsed.preampDb, parsed.filters)
        _state.update { it.copy(presets = it.presets + preset) }
        loadPreset(preset)
    }

    /** Adds a preset from a full-detail curve, such as an imported GraphicEQ file. */
    fun importCurve(name: String, points: List<ShapePoint>) {
        val preset = Preset("user:${UUID.randomUUID()}", name, 0.0, emptyList(), tone = Tone(importedCurve = points))
        _state.update { it.copy(presets = it.presets + preset) }
        loadPreset(preset)
    }

    /** Assigns a preset to an output, or clears the assignment when [presetId] is null. */
    fun assignToOutput(outputKey: String, presetId: String?) {
        _state.update { s ->
            s.copy(
                outputProfiles = if (presetId == null) {
                    s.outputProfiles - outputKey
                } else {
                    s.outputProfiles + (outputKey to presetId)
                },
            )
        }
    }

    fun toggleFavorite(id: String) {
        _state.update { s ->
            s.copy(favorites = if (id in s.favorites) s.favorites - id else s.favorites + id)
        }
    }

    fun toggleFavoriteShape(id: String) {
        _state.update { s ->
            s.copy(favoriteShapes = if (id in s.favoriteShapes) s.favoriteShapes - id else s.favoriteShapes + id)
        }
    }

    fun saveShape(shape: CustomShape) {
        _state.update { s ->
            val replaced = s.customShapes.any { it.id == shape.id }
            s.copy(customShapes = if (replaced) s.customShapes.map { if (it.id == shape.id) shape else it } else s.customShapes + shape)
        }
    }

    fun deleteShape(id: String) {
        _state.update { s ->
            s.copy(customShapes = s.customShapes.filterNot { it.id == id }, favoriteShapes = s.favoriteShapes - id)
        }
    }

    fun dismissPrompt(key: String) {
        _state.update { it.copy(dismissedPrompts = it.dismissedPrompts + key) }
    }

    /** Assigns a preset to an app, or clears it when [presetId] is null. */
    fun assignToApp(packageName: String, presetId: String?) {
        _state.update { s ->
            s.copy(appProfiles = if (presetId == null) s.appProfiles - packageName else s.appProfiles + (packageName to presetId))
        }
    }

    /** Adds a preset from a share code and starts using it. Returns false if the code isn't valid. */
    fun importShareCode(code: String): Boolean {
        val preset = ShareCode.decode(code) ?: return false
        _state.update { it.copy(presets = it.presets + preset) }
        loadPreset(preset)
        return true
    }

    /** A different app started playing, so load its preset if it has one. */
    fun onPlayingAppChanged(packageName: String?) {
        if (playingApp.value == packageName) return
        playingApp.value = packageName
        val state = _state.value
        val preset = packageName?.let { state.appProfiles[it] }?.let(::findPreset) ?: return
        if (state.current.id != preset.id) loadPreset(preset)
    }

    /** Moves through favourites, or every preset when there are none. */
    fun nextPreset() {
        val state = _state.value
        val all = BuiltIns.availableFor(listOf(output.value.key)) + state.presets
        val cycle = all.filter { it.id in state.favorites }.ifEmpty { all }
        val index = cycle.indexOfFirst { it.id == state.current.id }
        loadPreset(cycle[(index + 1) % cycle.size])
    }

    /** Changes the noise control mode and loads that mode's preset, if this output has one. */
    fun setListeningMode(mode: ListeningMode) {
        _state.update { it.copy(listeningMode = mode) }
        val state = _state.value
        val preset = Profiles.presetIdFor(state, output.value.key)?.let(::findPreset) ?: return
        if (state.current.id != preset.id) loadPreset(preset)
    }

    /** Returns false if the text was not a valid backup. Tone and effect settings aren't included. */
    fun restoreBackup(text: String): Boolean {
        val backup = BackupCodec.decode(text) ?: return false
        _state.update { BackupCodec.merge(it, backup) }
        return true
    }
}
