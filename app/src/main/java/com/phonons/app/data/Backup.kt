package com.phonons.app.data

import com.phonons.app.dsp.Eq
import com.phonons.app.dsp.Filter
import com.phonons.app.dsp.GraphicEq
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

@Serializable
data class Backup(
    val version: Int = 1,
    val presets: List<Preset> = emptyList(),
    val outputProfiles: Map<String, String> = emptyMap(),
    val favorites: Set<String> = emptySet(),
    val customShapes: List<CustomShape> = emptyList(),
    val favoriteShapes: Set<String> = emptySet(),
    val appProfiles: Map<String, String> = emptyMap(),
    val earCorrections: Map<String, EarCorrection> = emptyMap(),
    val hearings: Map<String, HearingRecord> = emptyMap(),
    val earCorrection: EarCorrection? = null,
    val hearing: HearingRecord? = null,
    val compressor: Compressor? = null,
)

/** Reads and writes preset backups. Restored files are sanitised, since they could be edited. */
object BackupCodec {
    private const val MAX_FILTERS = 20
    private const val MAX_NAME = 80
    private const val MAX_IMPORT_GAIN = 30.0
    private const val MAX_PREAMP = 30.0
    private const val MAX_EAR_DB = 6.0
    private const val MAX_HEARING_DB = 120.0

    private val json = Json { ignoreUnknownKeys = true; encodeDefaults = true; prettyPrint = true }

    fun encode(state: AppState): String = json.encodeToString(
        Backup.serializer(),
        Backup(
            presets = state.presets,
            outputProfiles = state.outputProfiles,
            favorites = state.favorites,
            customShapes = state.customShapes,
            favoriteShapes = state.favoriteShapes,
            appProfiles = state.appProfiles,
            earCorrections = state.earCorrections,
            hearings = state.hearings,
            earCorrection = state.earCorrection,
            hearing = state.hearing,
        ),
    )

    /** Returns null if the text is not a valid backup. */
    fun decode(text: String): Backup? = try {
        val backup = json.decodeFromString(Backup.serializer(), text)
        backup.copy(
            presets = backup.presets
                // Only user presets can be restored, so a backup can't replace a built-in.
                .filter { it.id.startsWith("user:") && it.name.isNotBlank() }
                .map(::sanitise),
            customShapes = backup.customShapes.mapNotNull(::sanitise),
            hearing = backup.hearing?.let(::sanitise),
            compressor = backup.compressor?.clamped(),
            earCorrections = backup.earCorrections.mapValues { sanitise(it.value) },
            hearings = backup.hearings.mapValues { sanitise(it.value) },
            earCorrection = backup.earCorrection?.let(::sanitise),
        )
    } catch (e: Exception) {
        null
    }

    /** Backed-up presets and shapes replace ones with the same id, and everything else is kept. */
    fun merge(state: AppState, backup: Backup): AppState {
        val incoming = backup.presets.associateBy { it.id }
        val kept = state.presets.filterNot { it.id in incoming }
        val known = (BuiltIns.all + kept + backup.presets).map { it.id }.toSet()
        val incomingShapes = backup.customShapes.map { it.id }.toSet()
        return state.copy(
            presets = kept + backup.presets,
            outputProfiles = state.outputProfiles + backup.outputProfiles.filterValues { it in known },
            favorites = state.favorites + backup.favorites.filter { it in known },
            customShapes = state.customShapes.filterNot { it.id in incomingShapes } + backup.customShapes,
            favoriteShapes = state.favoriteShapes + backup.favoriteShapes,
            appProfiles = state.appProfiles + backup.appProfiles.filterValues { it in known },
            earCorrections = state.earCorrections + backup.earCorrections,
            hearings = state.hearings + backup.hearings,
            earCorrection = backup.earCorrection ?: state.earCorrection,
            hearing = backup.hearing ?: state.hearing,
            compressor = backup.compressor ?: state.compressor,
        ).withLegacyCompressorMoved()
    }

    private fun sanitise(ears: EarCorrection) =
        ears.copy(left = ShapeText.cleanCurve(ears.left, MAX_EAR_DB), right = ShapeText.cleanCurve(ears.right, MAX_EAR_DB))

    private fun sanitise(record: HearingRecord) = record.copy(
        left = ShapeText.cleanCurve(record.left, MAX_HEARING_DB),
        right = ShapeText.cleanCurve(record.right, MAX_HEARING_DB),
        headphones = record.headphones.take(MAX_NAME),
    )

    /** Returns null if nothing usable is left, as a shape with no points can't be shown or applied. */
    private fun sanitise(shape: CustomShape): CustomShape? {
        val points = ShapeText.cleanCurve(shape.points, GraphicEq.MAX_DB).take(ShapeText.MAX_POINTS)
        if (!shape.id.startsWith("custom:") || shape.name.isBlank() || points.isEmpty()) return null
        return shape.copy(name = shape.name.take(MAX_NAME), points = points)
    }

    fun sanitise(preset: Preset) = preset.copy(
        name = preset.name.take(MAX_NAME),
        preampDb = safe(preset.preampDb, 0.0).coerceIn(-MAX_PREAMP, MAX_PREAMP),
        filters = preset.filters.take(MAX_FILTERS).map(::sanitise),
        tone = sanitise(preset.tone),
    )

    private fun sanitise(tone: Tone): Tone {
        val toneLimit = Processing.MAX_TONE_DB
        val bands = tone.graphicBands.takeIf { it in GraphicEq.BAND_OPTIONS } ?: GraphicEq.DEFAULT_BANDS
        return Tone(
            bassDb = safe(tone.bassDb, 0.0).coerceIn(-toneLimit, toneLimit),
            trebleDb = safe(tone.trebleDb, 0.0).coerceIn(-toneLimit, toneLimit),
            vocalsDb = safe(tone.vocalsDb, 0.0).coerceIn(-toneLimit, toneLimit),
            bassTunerDb = safe(tone.bassTunerDb, 0.0).coerceIn(-toneLimit, toneLimit),
            bassTunerHz = safe(tone.bassTunerHz, 80.0).coerceIn(30.0, 250.0),
            graphicDb = tone.graphicDb.take(bands).map { GraphicEq.snap(safe(it, 0.0)) },
            graphicBands = bands,
            surround = safe(tone.surround, 0.0).coerceIn(0.0, 1.0),
            strength = safe(tone.strength, 1.0).coerceIn(0.0, 1.5),
            correction = safe(tone.correction, 1.0).coerceIn(0.0, 1.0),
            importedCurve = ShapeText.cleanCurve(tone.importedCurve),
            compressor = tone.compressor.clamped(),
            shape = tone.shape?.takeIf { it.name.isNotBlank() }?.let { shape ->
                AppliedShape(shape.name.take(Tone.MAX_SHAPE_LABEL), shape.faders.take(bands).map { GraphicEq.snap(safe(it, 0.0)) })
            },
        )
    }

    private fun sanitise(f: Filter) = f.copy(
        freq = safe(f.freq, 1000.0).coerceIn(Eq.MIN_FREQ, Eq.MAX_FREQ),
        gainDb = safe(f.gainDb, 0.0).coerceIn(-MAX_IMPORT_GAIN, MAX_IMPORT_GAIN),
        q = safe(f.q, 1.0).coerceIn(Eq.MIN_Q, Eq.MAX_Q),
    )

    private fun safe(value: Double, fallback: Double) = if (value.isFinite()) value else fallback
}
