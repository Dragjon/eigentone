package com.phonons.app.data

import com.phonons.app.dsp.Filter
import com.phonons.app.dsp.FilterType
import kotlinx.serialization.Serializable

@Serializable
data class Preset(
    val id: String,
    val name: String,
    val preampDb: Double = 0.0,
    val filters: List<Filter> = emptyList(),
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
    val bassDb: Double = 0.0,
    val trebleDb: Double = 0.0,
    val matchBypass: Boolean = true,
    val loudness: Boolean = false,
    val loudnessStrength: Double = 0.5,
    val startOnBoot: Boolean = true,
    val favorites: Set<String> = emptySet(),
    val listeningMode: ListeningMode = ListeningMode.NOISE_CANCELLING,
    val analyser: Boolean = false,
    /** Gains for [Processing.GRAPHIC_BANDS]. */
    val graphicDb: List<Double> = emptyList(),
    val bassTunerDb: Double = 0.0,
    val bassTunerHz: Double = 80.0,
    val limiterThresholdDb: Double = -1.0,
    /** 0 is off, 1 is the strongest. */
    val surround: Double = 0.0,
    val reverb: ReverbChoice = ReverbChoice.OFF,
)

/** [androidPreset] matches the values of android.media.audiofx.PresetReverb. */
@Serializable
enum class ReverbChoice(val label: String, val androidPreset: Short) {
    OFF("Off", 0),
    SMALL_ROOM("Small room", 1),
    MEDIUM_ROOM("Medium room", 2),
    LARGE_ROOM("Large room", 3),
    MEDIUM_HALL("Medium hall", 4),
    LARGE_HALL("Large hall", 5),
    PLATE("Plate", 6),
}

/**
 * The earbuds' noise control mode. Android can't read this from the earbuds, so it is set by hand
 * in the app and only decides which preset to use.
 */
@Serializable
enum class ListeningMode(val label: String) {
    NOISE_CANCELLING("Noise cancelling"),
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
    /** Surround and reverb failures are separate, because the EQ keeps working without them. */
    val spatialError: String? = null,
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
