package com.phonons.app.data

import com.phonons.app.dsp.Eq
import com.phonons.app.dsp.Filter
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

@Serializable
data class Backup(
    val version: Int = 1,
    val presets: List<Preset> = emptyList(),
    val outputProfiles: Map<String, String> = emptyMap(),
    val favorites: Set<String> = emptySet(),
)

/** Reads and writes preset backups. Restored files are sanitised, since they could be edited. */
object BackupCodec {
    private const val MAX_FILTERS = 20
    private const val MAX_NAME = 80
    private const val MAX_IMPORT_GAIN = 30.0
    private const val MAX_PREAMP = 30.0

    private val json = Json { ignoreUnknownKeys = true; encodeDefaults = true; prettyPrint = true }

    fun encode(state: AppState): String = json.encodeToString(
        Backup.serializer(),
        Backup(presets = state.presets, outputProfiles = state.outputProfiles, favorites = state.favorites),
    )

    /** Returns null if the text is not a valid backup. */
    fun decode(text: String): Backup? = try {
        val backup = json.decodeFromString(Backup.serializer(), text)
        backup.copy(
            presets = backup.presets
                // Only user presets can be restored, so a backup can't replace a built-in.
                .filter { it.id.startsWith("user:") && it.name.isNotBlank() }
                .map(::sanitise),
        )
    } catch (e: Exception) {
        null
    }

    /** Backed-up presets replace ones with the same id, and everything else is kept. */
    fun merge(state: AppState, backup: Backup): AppState {
        val incoming = backup.presets.associateBy { it.id }
        val kept = state.presets.filterNot { it.id in incoming }
        val known = (BuiltIns.all + kept + backup.presets).map { it.id }.toSet()
        return state.copy(
            presets = kept + backup.presets,
            outputProfiles = state.outputProfiles + backup.outputProfiles.filterValues { it in known },
            favorites = state.favorites + backup.favorites.filter { it in known },
        )
    }

    private fun sanitise(preset: Preset) = preset.copy(
        name = preset.name.take(MAX_NAME),
        preampDb = safe(preset.preampDb, 0.0).coerceIn(-MAX_PREAMP, MAX_PREAMP),
        filters = preset.filters.take(MAX_FILTERS).map(::sanitise),
    )

    private fun sanitise(f: Filter) = f.copy(
        freq = safe(f.freq, 1000.0).coerceIn(Eq.MIN_FREQ, Eq.MAX_FREQ),
        gainDb = safe(f.gainDb, 0.0).coerceIn(-MAX_IMPORT_GAIN, MAX_IMPORT_GAIN),
        q = safe(f.q, 1.0).coerceIn(Eq.MIN_Q, Eq.MAX_Q),
    )

    private fun safe(value: Double, fallback: Double) = if (value.isFinite()) value else fallback
}
