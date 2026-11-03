package com.phonons.app.data

import com.phonons.app.dsp.Filter
import com.phonons.app.dsp.FilterType
import com.phonons.app.dsp.GraphicEq
import com.phonons.app.dsp.GraphicShape
import kotlinx.serialization.Serializable

/** The adjustments layered over a preset's filters. They're saved with it, so a preset is a whole sound. */
@Serializable
data class Tone(
    val bassDb: Double = 0.0,
    val trebleDb: Double = 0.0,
    val vocalsDb: Double = 0.0,
    val bassTunerDb: Double = 0.0,
    val bassTunerHz: Double = 80.0,
    val graphicDb: List<Double> = emptyList(),
    val graphicBands: Int = GraphicEq.DEFAULT_BANDS,
    /** 0 is off, 1 is the strongest. */
    val surround: Double = 0.0,
    /** Scales every EQ change in the preset: 0 is flat, 1 is as designed, 1.5 is stronger. */
    val strength: Double = 1.0,
    /** Scales the preset's own filters, such as a headphone correction: 0 removes them, 1 is as designed. */
    val correction: Double = 1.0,
    /** A level change for fair comparisons while previewing. Never saved as part of a preset. */
    val trimDb: Double = 0.0,
    /** A curve imported at full detail, such as a GraphicEQ file. Scaled by [correction], like filters. */
    val importedCurve: List<ShapePoint> = emptyList(),
    /** What was last applied to the faders, for showing its name. */
    val shape: AppliedShape? = null,
) {
    /**
     * Names what the faders hold. Moving a fader afterwards shows as edited, because the name no longer
     * tells the whole story. Sounds set before names were saved are matched against [known] shapes.
     */
    fun faderLabel(known: List<GraphicShape> = emptyList()): String {
        val gains = GraphicEq.normalized(graphicDb, graphicBands)
        return when {
            GraphicEq.isFlat(gains) -> FLAT_LABEL
            shape == null -> known.firstOrNull { GraphicEq.apply(it, graphicBands) == gains }?.name ?: CUSTOM_LABEL
            shape.faders == gains -> shape.name
            else -> "${shape.name}, edited"
        }
    }

    /** Sets the faders from a named shape. When layering, the name also says what it was added to. */
    fun withShape(name: String, faders: List<Double>, bands: Int, layered: Boolean = false): Tone {
        val before = GraphicEq.normalized(graphicDb, graphicBands)
        val label = if (layered && !GraphicEq.isFlat(before)) "${faderLabel()} + $name" else name
        return copy(
            graphicDb = faders,
            graphicBands = bands,
            shape = if (GraphicEq.isFlat(faders)) null else AppliedShape(label.take(MAX_SHAPE_LABEL), faders),
        )
    }

    companion object {
        const val FLAT_LABEL = "Flat"
        const val CUSTOM_LABEL = "Custom"
        const val MAX_SHAPE_LABEL = 80
    }
}

/** A shape's name, with the faders as it left them, which shows whether they've been moved since. */
@Serializable
data class AppliedShape(val name: String, val faders: List<Double>)

@Serializable
data class ShapePoint(val hz: Double, val db: Double)

/** A curve the user made, typed in, or got from the hearing check. */
@Serializable
data class CustomShape(val id: String, val name: String, val points: List<ShapePoint>)

/**
 * Separate changes for each ear, added to the preset's sound. These come from the hearing check and
 * belong to the listener, so they stay in place when the preset changes.
 */
@Serializable
data class EarCorrection(
    val enabled: Boolean = true,
    val left: List<ShapePoint> = emptyList(),
    val right: List<ShapePoint> = emptyList(),
) {
    /** Whether this would change anything. Small gaps can round down to no change at all. */
    val hasChanges: Boolean get() = (left + right).any { it.db != 0.0 }
}

/** Applies night mode automatically during part of the day. Times are minutes after midnight. */
@Serializable
data class NightSchedule(
    val enabled: Boolean = false,
    val startMinute: Int = 22 * 60,
    val endMinute: Int = 7 * 60,
    val amount: Double = 0.6,
) {
    /** Handles periods that run past midnight. */
    fun isActive(minuteOfDay: Int): Boolean = when {
        !enabled || startMinute == endMinute -> false
        startMinute < endMinute -> minuteOfDay in startMinute until endMinute
        else -> minuteOfDay >= startMinute || minuteOfDay < endMinute
    }
}

@Serializable
data class Preset(
    val id: String,
    val name: String,
    val preampDb: Double = 0.0,
    val filters: List<Filter> = emptyList(),
    val tone: Tone = Tone(),
)

@Serializable
enum class EngineMode(val label: String) {
    AUTO("Auto"),
    GLOBAL("Global"),
    SESSIONS("Per app"),
}

/** Everything the user controls. Saved to disk as JSON. */
@Serializable
data class AppState(
    val enabled: Boolean = false,
    val bypass: Boolean = false,
    val current: Preset = BuiltIns.flat,
    val presets: List<Preset> = emptyList(),
    val autoHeadroom: Boolean = true,
    val limiter: Boolean = true,
    val nightMode: Double = 0.0,
    val balance: Double = 0.0,
    val lowLatency: Boolean = false,
    val engineMode: EngineMode = EngineMode.AUTO,
    val autoSwitch: Boolean = true,
    /** Output key to preset id. See [Profiles] for per-mode keys. */
    val outputProfiles: Map<String, String> = emptyMap(),
    val matchBypass: Boolean = true,
    val loudness: Boolean = false,
    val loudnessStrength: Double = 0.5,
    val startOnBoot: Boolean = true,
    val favorites: Set<String> = emptySet(),
    val favoriteShapes: Set<String> = emptySet(),
    val customShapes: List<CustomShape> = emptyList(),
    /** App package to preset id. */
    val appProfiles: Map<String, String> = emptyMap(),
    /** One-off prompts already answered, such as the correction offer for one output. */
    val dismissedPrompts: Set<String> = emptySet(),
    val nightSchedule: NightSchedule = NightSchedule(),
    /** Evens out loud and quiet tracks, from 0 to 1. Night mode takes priority when both are on. */
    val leveller: Double = 0.0,
    /** Output key to its correction. A hearing check measures the headphones too, so each has its own. */
    val earCorrections: Map<String, EarCorrection> = emptyMap(),
    /** Output key to its latest finished check, so a correction can still be turned on after leaving it. */
    val hearings: Map<String, HearingRecord> = emptyMap(),
    /** From before these were per headphone. Moved by [withLegacyEarsMovedTo] when the headphones are known. */
    val earCorrection: EarCorrection? = null,
    val hearing: HearingRecord? = null,
    val compressor: Compressor = Compressor(),
    /** Which one-time updates this saved state has had. A fresh install starts at the latest. */
    val stateVersion: Int = 0,
    val listeningMode: ListeningMode = ListeningMode.NOISE_CANCELLING,
    val analyser: Boolean = false,
    val limiterThresholdDb: Double = -1.0,
    val guidedSetupSeen: Boolean = false,
    // Older versions kept these outside the preset. They're only read once, by migrated().
    val bassDb: Double = 0.0,
    val trebleDb: Double = 0.0,
    val graphicDb: List<Double> = emptyList(),
    val bassTunerDb: Double = 0.0,
    val bassTunerHz: Double = 80.0,
    val surround: Double = 0.0,
) {
    fun renamedPreset(id: String, name: String): AppState = copy(
        presets = presets.map { if (it.id == id) it.copy(name = name) else it },
        current = if (current.id == id) current.copy(name = name) else current,
    )

    /** This output's correction. A phone speaker can't reach each ear separately, so it never has one. */
    fun earsFor(outputKey: String): EarCorrection? =
        if (Profiles.supportsModes(outputKey)) earCorrections[outputKey] else null

    /**
     * Files a correction from before these were per headphone under the output it belongs to. A saved
     * check names its headphones, so it waits for them. Without one, the first headphones get it, since
     * the correction was already in use on whatever was connected.
     */
    fun withLegacyEarsMovedTo(output: OutputInfo): AppState {
        if (earCorrection == null && hearing == null) return this
        val belongsHere = if (hearing != null) hearing.headphones == output.label else Profiles.supportsModes(output.key)
        if (!belongsHere) return this
        return copy(
            earCorrections = earCorrection?.let { mapOf(output.key to it) + earCorrections } ?: earCorrections,
            hearings = hearing?.let { mapOf(output.key to it) + hearings } ?: hearings,
            earCorrection = null,
            hearing = null,
        )
    }

    /** Keeps presets, output choices and power, and clears everything that shapes the sound. */
    fun resetSound(): AppState {
        val defaults = AppState()
        return copy(
            current = BuiltIns.flat,
            bypass = false,
            autoHeadroom = defaults.autoHeadroom,
            limiter = defaults.limiter,
            limiterThresholdDb = defaults.limiterThresholdDb,
            nightMode = defaults.nightMode,
            nightSchedule = defaults.nightSchedule,
            leveller = defaults.leveller,
            balance = defaults.balance,
            earCorrections = defaults.earCorrections,
            earCorrection = defaults.earCorrection,
            compressor = defaults.compressor,
            loudness = defaults.loudness,
            loudnessStrength = defaults.loudnessStrength,
            matchBypass = defaults.matchBypass,
        )
    }

    /**
     * Updates a state saved by an older version. The curve below moved out of the shared library when
     * it became the owner's own shape, and it's added here so that install keeps it. New installs start
     * at [CURRENT_STATE_VERSION], so they never get it.
     */
    fun migrated(): AppState {
        val withCurve = if (stateVersion < 1 && customShapes.none { it.id == OWNER_CURVE.id }) {
            copy(customShapes = customShapes + OWNER_CURVE)
        } else {
            this
        }
        // A preview's temporary level can only be here if the app was closed during one.
        val clean = current.copy(tone = current.tone.copy(trimDb = 0.0))
        return withCurve.copy(stateVersion = CURRENT_STATE_VERSION, current = clean).withoutLegacyTone()
    }

    /** Moves settings saved by older versions into the current preset, so nobody's sound changes. */
    private fun withoutLegacyTone(): AppState {
        val hasLegacy = bassDb != 0.0 || trebleDb != 0.0 || graphicDb.isNotEmpty() || bassTunerDb != 0.0 ||
            bassTunerHz != 80.0 || surround != 0.0
        if (!hasLegacy) return this
        val tone = current.tone.copy(
            bassDb = bassDb,
            trebleDb = trebleDb,
            bassTunerDb = bassTunerDb,
            bassTunerHz = bassTunerHz,
            graphicDb = GraphicEq.normalized(graphicDb, GraphicEq.DEFAULT_BANDS).takeIf { graphicDb.isNotEmpty() } ?: emptyList(),
            surround = surround,
        )
        return copy(
            current = current.copy(tone = tone),
            bassDb = 0.0,
            trebleDb = 0.0,
            graphicDb = emptyList(),
            bassTunerDb = 0.0,
            bassTunerHz = 80.0,
            surround = 0.0,
        )
    }

    companion object {
        const val CURRENT_STATE_VERSION = 1

        private val OWNER_CURVE = CustomShape(
            "custom:my-soundcore-curve",
            "My soundcore curve",
            listOf(
                ShapePoint(100.0, 4.0), ShapePoint(200.0, 1.0), ShapePoint(400.0, 2.0), ShapePoint(800.0, 2.0),
                ShapePoint(1600.0, 4.0), ShapePoint(3200.0, 4.0), ShapePoint(6400.0, 4.0), ShapePoint(12800.0, 2.0),
            ),
        )
    }
}

/**
 * The earbuds' noise control mode. Android can't read this from the earbuds, so it is set by hand
 * in the app and only decides which preset to use.
 */
@Serializable
enum class ListeningMode(val label: String) {
    NOISE_CANCELLING("Cancelling"),
    TRANSPARENCY("Transparency"),
    NORMAL("Off"),
}

/** Output presets: a per-mode choice wins, then the output's general choice. */
object Profiles {
    private const val SPEAKER = "speaker"

    fun modeKey(outputKey: String, mode: ListeningMode) = "$outputKey#${mode.name}"

    /** Noise control modes only mean something on headphones. */
    fun supportsModes(outputKey: String) = outputKey != SPEAKER

    fun presetIdFor(state: AppState, outputKey: String): String? {
        val forMode = if (supportsModes(outputKey)) state.outputProfiles[modeKey(outputKey, state.listeningMode)] else null
        return forMode ?: state.outputProfiles[outputKey]
    }

    /** Whether [presetId] is assigned to this output under any mode. */
    fun isAssigned(state: AppState, outputKey: String, presetId: String): Boolean =
        state.outputProfiles.any { (key, id) ->
            id == presetId && (key == outputKey || key.startsWith("$outputKey#"))
        }
}

data class OutputInfo(val key: String, val label: String)

/** Live engine facts, never saved. */
data class EngineStatus(
    val running: Boolean = false,
    val mode: String = "Off",
    val bandCount: Int = 0,
    val hasControl: Boolean = true,
    val sessions: List<String> = emptyList(),
    val error: String? = null,
    /** Surround failures are separate, because the EQ keeps working without it. */
    val spatialError: String? = null,
    /** How many times the watchdog has had to rebuild the effect since the service started. */
    val rebuilds: Int = 0,
    /** Bands the phone accepted when read back, as "accepted/total", or null before the first check. */
    val readback: String? = null,
)

object BuiltIns {
    private fun f(id: Int, type: FilterType, freq: Double, gain: Double, q: Double) =
        Filter(id, type, freq, gain, q)

    val flat = Preset(
        "builtin:flat", "Flat", 0.0,
        listOf(
            f(1, FilterType.LOW_SHELF, 80.0, 0.0, 0.71),
            f(2, FilterType.BELL, 250.0, 0.0, 1.0),
            f(3, FilterType.BELL, 1000.0, 0.0, 1.0),
            f(4, FilterType.BELL, 4000.0, 0.0, 1.0),
            f(5, FilterType.HIGH_SHELF, 10000.0, 0.0, 0.71),
        ),
    )

    /** AutoEq: DHRME measurements. Close to the middle of the three published profiles. */
    private val liberty4Nc = Preset(
        "builtin:liberty4nc", "Liberty 4 NC", -3.6,
        listOf(
            f(1, FilterType.LOW_SHELF, 105.0, -1.6, 0.70),
            f(2, FilterType.BELL, 2383.0, 4.0, 0.26),
            f(3, FilterType.BELL, 79.0, -4.7, 0.79),
            f(4, FilterType.BELL, 264.0, -1.8, 0.35),
            f(5, FilterType.BELL, 7369.0, -6.0, 1.88),
            f(6, FilterType.HIGH_SHELF, 10000.0, -3.0, 0.70),
            f(7, FilterType.BELL, 4874.0, 1.1, 3.01),
            f(8, FilterType.BELL, 2945.0, -1.0, 3.83),
            f(9, FilterType.BELL, 89.0, -1.0, 6.00),
            f(10, FilterType.BELL, 8013.0, 1.3, 5.99),
        ),
    )

    /** The same correction without its bass cuts, for listeners who find the full version thin. */
    private val liberty4NcBass = liberty4Nc.copy(
        id = "builtin:liberty4nc-bass",
        name = "Liberty 4 NC + bass",
        filters = liberty4Nc.filters.filterNot { it.id in setOf(1, 3, 4, 9) },
    )

    /** AutoEq: Rtings on a Bruel & Kjaer 5128. Brighter at the top than the DHRME version. */
    private val liberty4NcRtings = Preset(
        "builtin:liberty4nc-rtings", "Liberty 4 NC (Rtings)", -2.5,
        listOf(
            f(1, FilterType.LOW_SHELF, 105.0, -6.6, 0.70),
            f(2, FilterType.BELL, 579.0, 2.4, 0.94),
            f(3, FilterType.BELL, 105.0, -2.1, 0.87),
            f(4, FilterType.BELL, 5504.0, -20.0, 0.77),
            f(5, FilterType.BELL, 4778.0, 17.4, 0.67),
            f(6, FilterType.HIGH_SHELF, 10000.0, 2.4, 0.70),
            f(7, FilterType.BELL, 1229.0, 0.3, 2.48),
            f(8, FilterType.BELL, 45.0, 0.3, 2.07),
            f(9, FilterType.BELL, 258.0, -0.1, 1.50),
            f(10, FilterType.BELL, 71.0, -0.3, 2.62),
        ),
    )

    /** Finds a correction preset for a connected output, matched on its name. */
    fun correctionFor(outputKey: String): Preset? =
        if (outputKey.contains("Liberty 4 NC", ignoreCase = true)) liberty4Nc else null

    fun isForSpecificHeadphones(preset: Preset): Boolean = preset.id.startsWith("builtin:liberty")

    /**
     * The built-ins worth offering given what's connected. Corrections only suit their own headphones,
     * so they're left out otherwise. They still exist, so an output assigned to one keeps working.
     */
    fun availableFor(connectedNames: List<String>): List<Preset> =
        if (connectedNames.any { correctionFor(it) != null }) all else all.filterNot(::isForSpecificHeadphones)

    val all = listOf(
        flat,
        liberty4Nc,
        liberty4NcBass,
        liberty4NcRtings,
        Preset(
            "builtin:warm", "Warm bass", 0.0,
            listOf(
                f(1, FilterType.LOW_SHELF, 110.0, 5.0, 0.71),
                f(2, FilterType.BELL, 350.0, -1.5, 1.0),
                f(3, FilterType.HIGH_SHELF, 9000.0, -1.0, 0.71),
            ),
        ),
        Preset(
            "builtin:vocal", "Clear vocals", 0.0,
            listOf(
                f(1, FilterType.HIGH_PASS, 40.0, 0.0, 0.71),
                f(2, FilterType.BELL, 300.0, -2.5, 0.9),
                f(3, FilterType.BELL, 2800.0, 3.5, 1.1),
                f(4, FilterType.HIGH_SHELF, 11000.0, 1.5, 0.71),
            ),
        ),
        Preset(
            "builtin:air", "Bright air", 0.0,
            listOf(
                f(1, FilterType.BELL, 3500.0, -1.0, 2.0),
                f(2, FilterType.HIGH_SHELF, 8000.0, 4.5, 0.71),
            ),
        ),
        Preset(
            "builtin:v", "V-shape", 0.0,
            listOf(
                f(1, FilterType.LOW_SHELF, 90.0, 5.0, 0.71),
                f(2, FilterType.BELL, 900.0, -3.0, 0.6),
                f(3, FilterType.HIGH_SHELF, 7000.0, 4.0, 0.71),
            ),
        ),
        Preset(
            "builtin:speaker", "Phone speaker", 0.0,
            listOf(
                f(1, FilterType.HIGH_PASS, 180.0, 0.0, 0.8),
                f(2, FilterType.BELL, 500.0, 2.5, 0.8),
                f(3, FilterType.BELL, 3000.0, -2.0, 1.2),
                f(4, FilterType.HIGH_SHELF, 9000.0, 2.0, 0.71),
            ),
        ),
    )
}
