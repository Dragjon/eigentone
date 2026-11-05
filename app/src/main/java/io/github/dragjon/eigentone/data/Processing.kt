package io.github.dragjon.eigentone.data

import io.github.dragjon.eigentone.dsp.Eq
import io.github.dragjon.eigentone.dsp.Filter
import io.github.dragjon.eigentone.dsp.FilterType
import io.github.dragjon.eigentone.dsp.GraphicEq
import kotlin.math.log10
import kotlin.math.min
import kotlin.math.pow

/** What full-scale music could do with these settings. */
enum class ClipRisk {
    NONE,

    /** Peaks go over, and the limiter holds them back, which can flatten them slightly. */
    HELD,

    /** Peaks go over with nothing to stop them, so they can distort. */
    CLIPPING,
}

data class EffectState(val name: String, val on: Boolean)

data class PeakEstimate(val peakDb: Double, val risk: ClipRisk)

/** What the audio effect should do right now. */
data class ProcessingTarget(
    val filters: List<Filter>,
    val inputDb: Double,
    val limiter: Boolean,
    val nightMode: Double,
    val effectEnabled: Boolean,
    val limiterThresholdDb: Double = -1.0,
    val surround: Double = 0.0,
    val leveller: Double = 0.0,
    /** Differs from [filters], which is then the left ear, only while ear correction is on. */
    val rightFilters: List<Filter> = filters,
    /** What the compressor does, once night mode, the leveller and manual settings are resolved. */
    val dynamics: Compressor = Compressor(),
    val tamers: Tamers = Tamers(),
    val limiterSettings: LimiterSettings = LimiterSettings(),
)

/** Turns the user's settings into the final processing. Pure, so it can be unit tested. */
object Processing {
    const val MAX_TONE_DB = 12.0

    private const val SHELF_Q = 0.71
    private const val VOCALS_HZ = 2500.0
    private const val VOCALS_Q = 0.7

    fun toneFilters(
        bassDb: Double,
        trebleDb: Double,
        vocalsDb: Double = 0.0,
        bassHz: Double = Tone.DEFAULT_BASS_HZ,
    ): List<Filter> = listOfNotNull(
        if (bassDb != 0.0) Filter(-1, FilterType.LOW_SHELF, bassHz, bassDb, SHELF_Q) else null,
        if (trebleDb != 0.0) Filter(-2, FilterType.HIGH_SHELF, 8000.0, trebleDb, SHELF_Q) else null,
        if (vocalsDb != 0.0) Filter(-6, FilterType.BELL, VOCALS_HZ, vocalsDb, VOCALS_Q) else null,
    )

    /**
     * Equal-loudness compensation for this volume, as one curve, from the ISO 226 contours. [usualDb] is the
     * volume, in dB below full, where nothing is added.
     */
    fun loudnessFilters(attenuationDb: Double, strength: Double, usualDb: Double = EqualLoudness.DEFAULT_USUAL_DB): List<Filter> =
        curveFilter(LOUDNESS_CURVE_ID, EqualLoudness.correction(EqualLoudness.quieter(attenuationDb, usualDb), strength))

    /** A shelf with an adjustable corner, for shaping the low end independently of the preset. */
    fun bassTunerFilters(gainDb: Double, freqHz: Double): List<Filter> =
        if (gainDb != 0.0) listOf(Filter(-5, FilterType.LOW_SHELF, freqHz, gainDb, SHELF_Q)) else emptyList()

    /** How much to turn the left channel down. Balance towards the right (above 0) makes the left quieter. */
    fun leftTrimDb(balance: Double): Double = sideTrimDb(balance)

    /** How much to turn the right channel down. Balance towards the left (below 0) makes the right quieter. */
    fun rightTrimDb(balance: Double): Double = sideTrimDb(-balance)

    private fun sideTrimDb(amount: Double): Double =
        if (amount <= 0) 0.0 else 20 * log10(kotlin.math.max(1.0 - amount, MIN_SIDE_LEVEL))

    /**
     * The preset's own sound, which the faders sit on: its filters, an imported curve and a headphone
     * target, at the preset's strength. Not the faders or the tone controls, which are changes on top.
     */
    fun fixedFilters(preset: Preset): List<Filter> {
        val tone = preset.tone
        val filterScale = tone.strength * tone.correction
        return preset.filters.map { filter ->
            if (filterScale != 1.0 && filter.type.hasGain) filter.copy(gainDb = filter.gainDb * filterScale) else filter
        }.filterNot { tone.correction == 0.0 && !it.type.hasGain } +
            curveFilter(IMPORTED_CURVE_ID, tone.importedCurve, filterScale) +
            curveFilter(TARGET_CURVE_ID, HeadphoneTargets.change(HeadphoneTarget.of(tone.targetId), tone.correctionInfo), filterScale)
    }

    /** Everything a preset contributes: its filters and its tone adjustments. */
    fun presetFilters(preset: Preset): List<Filter> {
        val tone = preset.tone
        val scale = tone.strength
        return fixedFilters(preset) +
            GraphicEq.filters(
                GraphicEq.normalized(tone.graphicDb, tone.graphicBands).map { it * scale },
                tone.graphicBands,
            ) +
            bassTunerFilters(tone.bassTunerDb * scale, tone.bassTunerHz) +
            toneFilters(tone.bassDb * scale, tone.trebleDb * scale, tone.vocalsDb * scale, tone.bassHz)
    }

    /** The room to use, which steps aside for Bypass and for tests that need the plain sound. */
    fun reverbFor(state: AppState, suspended: Boolean): ReverbRoom =
        if (!state.enabled || state.bypass || suspended) ReverbRoom.OFF else state.current.tone.reverb

    /** The preset plus loudness compensation, which follows the volume and the output rather than the preset. */
    fun effectiveFilters(state: AppState, attenuationDb: Double, outputKey: String = ""): List<Filter> =
        presetFilters(state.current) +
            if (state.loudness) loudnessFilters(attenuationDb, state.loudnessStrengthFor(outputKey), state.usualVolumeDb(outputKey)) else emptyList()

    /**
     * A preset's level belongs to its correction, so turning the correction down takes the matching
     * level change with it. Otherwise removing a correction would leave the sound needlessly quiet.
     */
    fun preampOf(preset: Preset): Double = preset.preampDb * preset.tone.correction

    /** The level going into the EQ for this preset, including any preview trim. */
    fun inputOf(preset: Preset, filters: List<Filter>, autoHeadroom: Boolean): Double =
        Eq.inputGainDb(filters, preampOf(preset), autoHeadroom) + preset.tone.trimDb

    private fun curveFilter(id: Int, points: List<ShapePoint>, scale: Double = 1.0): List<Filter> =
        if (points.isEmpty() || scale == 0.0) {
            emptyList()
        } else {
            listOf(Filter(id, FilterType.CURVE, curve = points.map { it.hz to it.db * scale }))
        }

    /** The whole preset as one curve including its level, in the form other EQ apps can read. */
    fun exportCurve(preset: Preset): List<ShapePoint> {
        val filters = presetFilters(preset)
        val level = preampOf(preset)
        return List(EXPORT_POINTS) { i ->
            val hz = kotlin.math.round(Eq.logFreq(i.toDouble() / (EXPORT_POINTS - 1)))
            ShapePoint(hz, Eq.curveDb(filters, hz) + level)
        }
    }

    /**
     * Each channel's filters, when they differ: ear correction for this output and stereo width. Null
     * when both channels get the same sound.
     */
    fun channelFilters(state: AppState, outputKey: String, filters: List<Filter>): Pair<List<Filter>, List<Filter>>? {
        val ears = state.earsFor(outputKey)?.takeIf { it.enabled }
        val width = state.current.tone.surround.takeIf { it > 0.0 && !state.useSystemSurround }
        if (ears == null && width == null) return null
        val (left, right) = if (ears == null) filters to filters else earCurves(filters, ears)
        if (width == null) return left to right
        val (leanLeft, leanRight) = StereoWidth.curves(width)
        return (left + curveFilter(WIDTH_CURVE_ID, leanLeft)) to (right + curveFilter(WIDTH_CURVE_ID, leanRight))
    }

    /**
     * The main effects and whether each is on, for the top of the screen. The compressor's place shows
     * whichever of night mode, the compressor or the leveller is really running.
     */
    fun effectStates(state: AppState, outputKey: String, scheduledNight: Boolean = false): List<EffectState> {
        val active = activeEffects(state, outputKey, scheduledNight)
        val dynamics = active.firstOrNull { it == "Night" || it == "Comp" || it == "Level" }
        val space = active.firstOrNull { it == "Width" || it == "Surround" }
        return buildList {
            add(EffectState(dynamics ?: "Comp", dynamics != null))
            add(EffectState(space ?: if (state.useSystemSurround) "Surround" else "Width", space != null))
            add(EffectState("Ears", "Ears" in active))
            add(EffectState("Reverb", "Reverb" in active))
            if ("Tame" in active) add(EffectState("Tame", true))
        }
    }

    /**
     * Short names for what's changing the sound right now, for the top of the screen. The compressor
     * entries follow [Dynamics.resolve], so something that's on but overridden isn't listed.
     */
    fun activeEffects(state: AppState, outputKey: String, scheduledNight: Boolean = false): List<String> {
        if (!state.enabled || state.bypass) return emptyList()
        val tone = state.current.tone
        return buildList {
            when {
                state.nightMode > 0.001 || scheduledNight -> add("Night")
                tone.compressor.enabled -> add("Comp")
                state.leveller > 0.001 -> add("Level")
            }
            if (tone.harshTamer > 0.0 || tone.boomTamer > 0.0) add("Tame")
            if (tone.surround > 0.0) add(if (state.useSystemSurround) "Surround" else "Width")
            if (state.earsFor(outputKey)?.enabled == true) add("Ears")
            if (tone.reverb != ReverbRoom.OFF) add("Reverb")
        }
    }

    /**
     * The highest level these settings could reach with music at full scale. It's worked out from the
     * settings, because Android doesn't report levels from inside the effect, so it's a worst case
     * rather than a measurement of what's playing.
     */
    fun peakEstimate(state: AppState, attenuationDb: Double, outputKey: String, scheduledNight: Boolean): PeakEstimate {
        val target = target(state, attenuationDb, scheduledNight, outputKey)
        if (state.bypass || !target.effectEnabled) return PeakEstimate(Double.NEGATIVE_INFINITY, ClipRisk.NONE)
        var peak = Double.NEGATIVE_INFINITY
        for (filters in listOf(target.filters, target.rightFilters)) {
            for (i in 0..PEAK_STEPS) {
                val hz = Eq.logFreq(i.toDouble() / PEAK_STEPS)
                val level = Eq.curveDb(filters, hz) + target.inputDb
                val out = if (target.dynamics.enabled) Dynamics.bandAt(target.dynamics, hz).outputDb(level) else level
                peak = maxOf(peak, out)
            }
        }
        val risk = when {
            peak <= OVER_MARGIN_DB -> ClipRisk.NONE
            target.limiter -> ClipRisk.HELD
            else -> ClipRisk.CLIPPING
        }
        return PeakEstimate(peak, risk)
    }

    /** The preset as ordinary filters, or null if it uses faders or a curve, which filters can't express. */
    fun exportFilters(preset: Preset): List<Filter>? =
        presetFilters(preset).takeIf { list -> list.isNotEmpty() && list.none { it.type == FilterType.CURVE } }

    /** Each ear's filters: the same sound, plus that ear's own correction. */
    fun earCurves(filters: List<Filter>, ears: EarCorrection): Pair<List<Filter>, List<Filter>> =
        (filters + curveFilter(EAR_CURVE_ID, ears.left)) to (filters + curveFilter(EAR_CURVE_ID, ears.right))

    fun target(
        state: AppState,
        attenuationDb: Double,
        scheduledNight: Boolean = false,
        outputKey: String = "",
    ): ProcessingTarget {
        val filters = effectiveFilters(state, attenuationDb, outputKey)
        val input = inputOf(state.current, filters, state.autoHeadroom)
        val night = if (scheduledNight) maxOf(state.nightMode, state.nightSchedule.amount) else state.nightMode
        val dynamics = Dynamics.resolve(night, state.leveller, state.current.tone.compressor)
        if (!state.bypass) {
            val channels = channelFilters(state, outputKey, filters)
            val (left, right) = channels ?: (filters to filters)
            // One level for both ears. Separate levels would shift the balance across every frequency.
            val shared = if (channels == null) {
                input
            } else {
                min(inputOf(state.current, left, state.autoHeadroom), inputOf(state.current, right, state.autoHeadroom))
            }
            return ProcessingTarget(
                left, shared, state.limiter,
                nightMode = night,
                effectEnabled = true,
                limiterThresholdDb = state.limiterThresholdDb,
                limiterSettings = state.limiterSettings(),
                // Only the phone's own effect needs this. Stereo width is already in the channels' filters.
                surround = if (state.useSystemSurround) state.current.tone.surround else 0.0,
                leveller = state.leveller,
                rightFilters = right,
                dynamics = dynamics,
                tamers = Tamers(
                    state.current.tone.harshTamer,
                    state.current.tone.boomTamer,
                    state.current.tone.bassCharacter,
                    (state.current.tone.bassDb * state.current.tone.strength).coerceAtLeast(0.0),
                ),
            )
        }
        if (!state.matchBypass) {
            return ProcessingTarget(emptyList(), 0.0, limiter = false, nightMode = 0.0, effectEnabled = false)
        }
        // Flat sound at about the same loudness as the EQ, so comparisons aren't swayed by volume.
        // Never boosted, because a flat signal already peaks at full scale.
        return ProcessingTarget(
            filters = emptyList(),
            inputDb = min(0.0, estimatedLoudnessDb(filters, input, dynamics)),
            limiter = state.limiter,
            nightMode = 0.0,
            effectEnabled = true,
            limiterThresholdDb = state.limiterThresholdDb,
            limiterSettings = state.limiterSettings(),
        )
    }

    /** Approximate perceived level change, using A-weighted pink noise as a stand-in for music. */
    fun estimatedLoudnessDb(filters: List<Filter>, inputDb: Double, compressor: Compressor = Compressor()): Double {
        var weighted = 0.0
        var total = 0.0
        for (i in 0..LOUDNESS_STEPS) {
            val freq = Eq.logFreq(i.toDouble() / LOUDNESS_STEPS)
            val weight = 10.0.pow(aWeightDb(freq) / 10)
            val level = Eq.curveDb(filters, freq) + inputDb
            weighted += weight * 10.0.pow((level + Dynamics.gainDb(compressor, freq, level)) / 10)
            total += weight
        }
        return 10 * log10(weighted / total)
    }

    private fun aWeightDb(f: Double): Double {
        val f2 = f * f
        val numerator = 12194.0 * 12194.0 * f2 * f2
        val denominator = (f2 + 20.6 * 20.6) *
            kotlin.math.sqrt((f2 + 107.7 * 107.7) * (f2 + 737.9 * 737.9)) *
            (f2 + 12194.0 * 12194.0)
        return 20 * log10(numerator / denominator) + 2.0
    }

    private const val MIN_SIDE_LEVEL = 0.001
    private const val LOUDNESS_STEPS = 120
    private const val EXPORT_POINTS = 127
    private const val PEAK_STEPS = 240
    private const val OVER_MARGIN_DB = 0.5
    private const val IMPORTED_CURVE_ID = -101
    private const val LOUDNESS_CURVE_ID = -3
    private const val TARGET_CURVE_ID = -104
    private const val EAR_CURVE_ID = -102
    private const val WIDTH_CURVE_ID = -103
}
