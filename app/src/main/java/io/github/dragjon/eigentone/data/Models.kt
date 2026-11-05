package io.github.dragjon.eigentone.data

import io.github.dragjon.eigentone.dsp.Filter
import io.github.dragjon.eigentone.dsp.FilterType
import io.github.dragjon.eigentone.dsp.GraphicEq
import io.github.dragjon.eigentone.dsp.GraphicShape
import kotlinx.serialization.Serializable

/**
 * A room for the reverb, based on Android's own presets. Their mix levels aren't used, because those leave
 * Large hall's echo about 38 dB below the music, which can't be heard. Decays are longer than theirs too,
 * because a measured 1.8 second tail stayed hidden under loud modern masters, which have few gaps.
 */
@Serializable
enum class ReverbRoom(val roomHfLevel: Short, val decayMs: Int, val decayHfRatio: Short, val diffusion: Short, val density: Short) {
    OFF(0, 1000, 1000, 1000, 1000),
    SMALL_ROOM(-600, 1000, 830, 1000, 1000),
    MEDIUM_ROOM(-600, 1500, 830, 1000, 1000),
    LARGE_ROOM(-600, 2000, 830, 1000, 1000),
    MEDIUM_HALL(-600, 2800, 700, 1000, 1000),
    LARGE_HALL(-600, 4000, 700, 1000, 1000),
    PLATE(-200, 2200, 900, 1000, 750),
    ;

    companion object {
        /** The quietest and loudest the echo can be against the music, in hundredths of a dB. */
        private const val QUIETEST_MB = -3000.0
        private const val LOUDEST_MB = 0.0

        /** Android subtracts this from the level it's given, so it's added back here. */
        private const val ANDROID_OFFSET_MB = 2000.0

        /**
         * The reverb level to send for an amount from 0 to 1, with the room level left at 0. Rooms differ by
         * a little at the same amount, as a small room leaves less echo than a large hall.
         */
        fun reverbLevel(amount: Double): Short =
            (ANDROID_OFFSET_MB + QUIETEST_MB + (LOUDEST_MB - QUIETEST_MB) * amount.coerceIn(0.0, 1.0)).toInt().toShort()
    }
}

/** The adjustments layered over a preset's filters. They're saved with it, so a preset is a whole sound. */
@Serializable
data class Tone(
    val bassDb: Double = 0.0,
    /** Where the bass change fades out. */
    val bassHz: Double = DEFAULT_BASS_HZ,
    val trebleDb: Double = 0.0,
    val vocalsDb: Double = 0.0,
    /** The separate bass tuner from earlier versions. Only still used by sounds that had both, see [withMergedBass]. */
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
    val compressor: Compressor = Compressor(),
    /** Only turns down harsh treble when it gets loud, from 0 to 1. */
    val harshTamer: Double = 0.0,
    /** Only turns down deep bass when it gets loud, from 0 to 1. */
    val boomTamer: Double = 0.0,
    /** How the added bass behaves over time. */
    val bassCharacter: BassCharacter = BassCharacter.NATURAL,
    /** Only reaches apps that announce their audio, because Android offers no reverb for all audio. */
    val reverb: ReverbRoom = ReverbRoom.OFF,
    /** How loud the echo is, from 0 to 1. */
    val reverbAmount: Double = 0.6,
    /** A sound signature for a measured correction to aim for, from [HeadphoneTarget]. Null keeps the published one. */
    val targetId: String? = null,
    /** What a measured correction was published to aim for, which [targetId] works from. */
    val correctionInfo: CorrectionInfo? = null,
    /** The faders at a finer layout, kept while fewer show, so switching back to it loses nothing. See [BandSwitch]. */
    val graphicDetail: FaderDetail? = null,
) {
    /**
     * Folds the old bass tuner into the bass control. A sound that used both keeps them separate, because
     * two shelves at different frequencies can't become one without changing how it sounds.
     */
    fun withMergedBass(): Tone = when {
        bassTunerDb == 0.0 -> copy(bassTunerHz = DEFAULT_TUNER_HZ)
        bassDb == 0.0 -> copy(bassDb = bassTunerDb, bassHz = bassTunerHz, bassTunerDb = 0.0, bassTunerHz = DEFAULT_TUNER_HZ)
        else -> this
    }

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
            // A new shape replaces the faders outright, so no finer detail from before carries over.
            graphicDetail = null,
            shape = if (GraphicEq.isFlat(faders)) null else AppliedShape(label.take(MAX_SHAPE_LABEL), faders),
        )
    }

    companion object {
        const val DEFAULT_BASS_HZ = 105.0
        private const val DEFAULT_TUNER_HZ = 80.0
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

    companion object {
        /** Shows minutes after midnight as a time, such as 22:00 or 10:00 PM. */
        fun formatTime(minuteOfDay: Int, is24Hour: Boolean): String {
            val hour = minuteOfDay / 60
            val minute = (minuteOfDay % 60).toString().padStart(2, '0')
            if (is24Hour) return "${hour.toString().padStart(2, '0')}:$minute"
            val hour12 = if (hour % 12 == 0) 12 else hour % 12
            return "$hour12:$minute ${if (hour < 12) "AM" else "PM"}"
        }
    }
}

@Serializable
data class Preset(
    val id: String,
    val name: String,
    val preampDb: Double = 0.0,
    val filters: List<Filter> = emptyList(),
    val tone: Tone = Tone(),
    /** Where it's filed in the preset list, or null for none. */
    val folder: String? = null,
)

/** How a finger moves the faders: one fader at a time, or a curve drawn across them. */
@Serializable
enum class FaderMode { PRECISION, SMOOTH }

/** How precision faders fit: wide ones that scroll sideways, or all on screen with a full-screen view to open. */
@Serializable
enum class FaderView { SCROLL, FULL_SCREEN }

/** Which colours the app uses. Device follows the phone's own light or dark setting. */
@Serializable
enum class ThemeMode { DEVICE, LIGHT, DARK }

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
    /** 100% is the full correction from the equal-loudness contours. */
    val loudnessStrength: Double = 1.0,
    /** Output key to its usual volume, in dB below full, where nothing is added. Each pair of headphones differs. */
    val loudnessUsual: Map<String, Double> = emptyMap(),
    /** Output key to its own strength, since a speaker and headphones can each want a different amount. */
    val loudnessStrengths: Map<String, Double> = emptyMap(),
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
    /** From before each preset had its own. Moved by [withLegacyCompressorMoved]. */
    val compressor: Compressor = Compressor(),
    /** Uses the phone's own Virtualizer for the Space amount, rather than Eigentone's stereo width. */
    val useSystemSurround: Boolean = false,
    /** Which one-time updates this saved state has had. A fresh install starts at the latest. */
    val stateVersion: Int = 0,
    val listeningMode: ListeningMode = ListeningMode.NOISE_CANCELLING,
    val analyser: Boolean = false,
    val limiterThresholdDb: Double = -1.0,
    val limiterAttackMs: Double = 1.0,
    val limiterReleaseMs: Double = 60.0,
    val limiterRatio: Double = 10.0,
    /** Lifts the level after limiting. It can't push peaks past full scale. */
    val limiterPostGainDb: Double = 0.0,
    val guidedSetupSeen: Boolean = false,
    /** Shows every control. Off, the app shows what most people need, plus anything already changed. */
    val advancedControls: Boolean = false,
    /** Whether the welcome has been finished or skipped. Settings, Help, shows it again. */
    val welcomeDone: Boolean = false,
    /** Where the welcome was left, so closing the app partway through resumes there. */
    val welcomeStep: Int = 0,
    /** Whether the tour of the main screen has been taken or turned down. Offered once until then. */
    val tourSeen: Boolean = false,
    val themeMode: ThemeMode = ThemeMode.DEVICE,
    val faderMode: FaderMode = FaderMode.PRECISION,
    val faderView: FaderView = FaderView.SCROLL,
    /** The output last acted on, so restarting the EQ on the same output leaves the chosen preset alone. */
    val lastOutputKey: String? = null,
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

    /** The limiter as it's really used, with every value kept in range. */
    fun limiterSettings(): LimiterSettings =
        LimiterSettings(limiterAttackMs, limiterReleaseMs, limiterRatio, limiterThresholdDb, limiterPostGainDb).clamped()

    /** Where equal loudness starts for this output: its usual volume, in dB below full. */
    fun usualVolumeDb(outputKey: String): Double = loudnessUsual[outputKey] ?: EqualLoudness.DEFAULT_USUAL_DB

    /** How much equal loudness to use on this output, falling back to the setting from before it was per output. */
    fun loudnessStrengthFor(outputKey: String): Double = (loudnessStrengths[outputKey] ?: loudnessStrength).coerceIn(0.0, 1.0)

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

    /**
     * The compressor used to apply to every preset, so one that was on moves into each of them, which
     * means nobody's sound changes. A preset that already has its own settings keeps them.
     */
    fun withLegacyCompressorMoved(): AppState {
        val none = Compressor()
        if (compressor == none) return this
        if (!compressor.enabled) return copy(compressor = none)
        fun moved(preset: Preset) =
            if (preset.tone.compressor == none) preset.copy(tone = preset.tone.copy(compressor = compressor)) else preset
        return copy(current = moved(current), presets = presets.map(::moved), compressor = none)
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
            limiterAttackMs = defaults.limiterAttackMs,
            limiterReleaseMs = defaults.limiterReleaseMs,
            limiterRatio = defaults.limiterRatio,
            limiterPostGainDb = defaults.limiterPostGainDb,
            nightMode = defaults.nightMode,
            nightSchedule = defaults.nightSchedule,
            leveller = defaults.leveller,
            balance = defaults.balance,
            earCorrections = defaults.earCorrections,
            earCorrection = defaults.earCorrection,
            compressor = defaults.compressor,
            useSystemSurround = defaults.useSystemSurround,
            loudness = defaults.loudness,
            loudnessStrength = defaults.loudnessStrength,
            loudnessStrengths = defaults.loudnessStrengths,
            loudnessUsual = defaults.loudnessUsual,
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
        // Anyone updating keeps every control they had and skips the welcome. Only new installs start simple.
        return withCurve.copy(stateVersion = CURRENT_STATE_VERSION, current = clean, advancedControls = advancedControls || stateVersion < 3, welcomeDone = welcomeDone || stateVersion < 3)
            .withoutLegacyTone()
            .withLegacyCompressorMoved()
            .let { state ->
                fun merged(preset: Preset) = preset.copy(tone = preset.tone.withMergedBass())
                state.copy(current = merged(state.current), presets = state.presets.map(::merged))
            }
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
        const val CURRENT_STATE_VERSION = 3

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

    /**
     * The preset to load when the output changes, or null to leave the sound alone. A correction tuned for
     * one output is wrong on another, so it isn't carried across, unless the user has edited it.
     */
    fun presetForNewOutput(state: AppState, previousKey: String?, newKey: String, find: (String) -> Preset?): Preset? {
        val assigned = presetIdFor(state, newKey)?.let(find)
        if (assigned != null) return assigned.takeIf { state.current.id != it.id }
        val cameFromAssigned = previousKey != null && isAssigned(state, previousKey, state.current.id)
        val isUnedited = find(state.current.id) == state.current
        return if (cameFromAssigned && isUnedited) BuiltIns.flat else null
    }

    /** Whether [presetId] is assigned to this output under any mode. */
    fun isAssigned(state: AppState, outputKey: String, presetId: String): Boolean =
        state.outputProfiles.any { (key, id) ->
            id == presetId && (key == outputKey || key.startsWith("$outputKey#"))
        }
}

data class OutputInfo(val key: String, val label: String, val kind: OutputKind = OutputKind.guess(key, label))

/**
 * Peak and average (RMS) level of everything playing, in dB below full scale, with the highest recent
 * peak held for a moment so a short one can be read.
 */
data class OutputLevel(val peakDb: Float, val rmsDb: Float, val heldPeakDb: Float = peakDb) {
    /** The next reading, keeping the held peak until a higher one comes or it has been held long enough. */
    fun next(peak: Float, rms: Float, heldFor: Long): Pair<OutputLevel, Boolean> {
        val renew = peak >= heldPeakDb || heldFor > HOLD_MS
        return OutputLevel(peak, rms, if (renew) peak else heldPeakDb) to renew
    }

    companion object {
        const val HOLD_MS = 1500L
    }
}

/** What kind of thing is playing, which names the device tab and decides what it offers. */
enum class OutputKind {
    PHONE, HEADPHONES, EARBUDS, SPEAKER, CAR;

    /** Worn on the head, so each ear can be reached separately and headphone corrections apply. */
    val isWorn: Boolean get() = this == HEADPHONES || this == EARBUDS

    companion object {
        private val CAR_WORDS = Regex(
            """\b(car|carkit|car kit|auto|android auto|carplay|sync|uconnect|mbux|idrive|mylink|bmw|audi|toyota|honda|ford|vw|volkswagen|kia|hyundai|mazda|nissan|tesla|subaru|lexus|mercedes|peugeot|renault|skoda|volvo|seat|opel|chevrolet|jeep|porsche)\b""",
            RegexOption.IGNORE_CASE,
        )
        private val SPEAKER_WORDS = Regex(
            """(speaker|soundbar|sound bar|soundlink|soundcore motion|partybox|\bflip\b|\bcharge\b|\bboom\b|megaboom|wonderboom|xtreme|\bpulse\b|\becho\b|\bnest\b|sonos|homepod|stanmore|emberton|acton|marshall woburn)""",
            RegexOption.IGNORE_CASE,
        )
        private val EARBUD_WORDS = Regex(
            """(buds|pods|earbud|\biem\b|\btws\b|\bwf-|nothing ear|jabra elite|soundcore liberty|momentum true|\bfreebuds|\bear \()""",
            RegexOption.IGNORE_CASE,
        )

        /**
         * A best guess from the output and its name, since Android only says a Bluetooth device plays audio,
         * not whether it's headphones, a speaker or a car. Names like "Galaxy Buds" or "JBL Flip" say the rest.
         */
        fun guess(key: String, name: String, speakerType: Boolean = false): OutputKind = when {
            key == "speaker" -> PHONE
            speakerType -> SPEAKER
            CAR_WORDS.containsMatchIn(name) -> CAR
            SPEAKER_WORDS.containsMatchIn(name) -> SPEAKER
            EARBUD_WORDS.containsMatchIn(name) -> EARBUDS
            else -> HEADPHONES
        }
    }
}

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
    /** This phone refused the extra bands the harshness and boom tamers need. */
    val tamersUnavailable: Boolean = false,
    /** Package names of the apps that have announced their audio since the EQ started. */
    val announcedApps: List<String> = emptyList(),
    /** Package names of the apps the reverb is really attached to right now. */
    val reverbApps: List<String> = emptyList(),
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

    /** What the built-in corrections aim for, from their AutoEq results. The bass version has had its aim changed. */
    fun correctionInfo(presetId: String): CorrectionInfo? = when (presetId) {
        liberty4Nc.id -> CorrectionInfo(HeadphoneForm.IN_EAR, listOf(ShapePoint(20.0, 6.4), ShapePoint(31.5, 6.3), ShapePoint(50.0, 5.9), ShapePoint(80.0, 4.3), ShapePoint(125.0, 1.2), ShapePoint(200.0, -0.9), ShapePoint(315.0, -1.4), ShapePoint(500.0, -1.2)))
        liberty4NcRtings.id -> CorrectionInfo(HeadphoneForm.IN_EAR, listOf(ShapePoint(20.0, 2.8), ShapePoint(31.5, 2.7), ShapePoint(50.0, 2.4), ShapePoint(80.0, 1.2), ShapePoint(125.0, -0.9), ShapePoint(200.0, -2.2), ShapePoint(315.0, -2.2), ShapePoint(500.0, -1.5)))
        else -> null
    }

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
