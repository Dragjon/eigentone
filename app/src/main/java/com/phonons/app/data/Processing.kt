package com.phonons.app.data

import com.phonons.app.dsp.Eq
import com.phonons.app.dsp.Filter
import com.phonons.app.dsp.FilterType
import com.phonons.app.dsp.GraphicEq
import kotlin.math.log10
import kotlin.math.min
import kotlin.math.pow

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
)

/** Turns the user's settings into the final processing. Pure, so it can be unit tested. */
object Processing {
    const val MAX_TONE_DB = 12.0

    // Compensation starts below this much volume attenuation, and is full at REFERENCE + RANGE.
    private const val LOUDNESS_REFERENCE_DB = 10.0
    private const val LOUDNESS_RANGE_DB = 40.0
    private const val LOUDNESS_BASS_DB = 12.0
    private const val LOUDNESS_TREBLE_DB = 4.0
    private const val SHELF_Q = 0.71
    private const val VOCALS_HZ = 2500.0
    private const val VOCALS_Q = 0.7

    fun toneFilters(bassDb: Double, trebleDb: Double, vocalsDb: Double = 0.0): List<Filter> = listOfNotNull(
        if (bassDb != 0.0) Filter(-1, FilterType.LOW_SHELF, 105.0, bassDb, SHELF_Q) else null,
        if (trebleDb != 0.0) Filter(-2, FilterType.HIGH_SHELF, 8000.0, trebleDb, SHELF_Q) else null,
        if (vocalsDb != 0.0) Filter(-6, FilterType.BELL, VOCALS_HZ, vocalsDb, VOCALS_Q) else null,
    )

    /**
     * Ears lose sensitivity to bass and treble at low levels. This is a simplified shape inspired
     * by equal-loudness contours, not an ISO 226 calculation.
     */
    fun loudnessFilters(attenuationDb: Double, strength: Double): List<Filter> {
        val amount = ((attenuationDb - LOUDNESS_REFERENCE_DB) / LOUDNESS_RANGE_DB).coerceIn(0.0, 1.0) *
            strength.coerceIn(0.0, 1.0)
        if (amount <= 0.0) return emptyList()
        return listOf(
            Filter(-3, FilterType.LOW_SHELF, 120.0, LOUDNESS_BASS_DB * amount, SHELF_Q),
            Filter(-4, FilterType.HIGH_SHELF, 9000.0, LOUDNESS_TREBLE_DB * amount, SHELF_Q),
        )
    }

    /** A shelf with an adjustable corner, for shaping the low end independently of the preset. */
    fun bassTunerFilters(gainDb: Double, freqHz: Double): List<Filter> =
        if (gainDb != 0.0) listOf(Filter(-5, FilterType.LOW_SHELF, freqHz, gainDb, SHELF_Q)) else emptyList()

    /** Everything a preset contributes: its filters and its tone adjustments. */
    fun presetFilters(preset: Preset): List<Filter> {
        val tone = preset.tone
        val scale = tone.strength
        val filterScale = scale * tone.correction
        return preset.filters.map { filter ->
            if (filterScale != 1.0 && filter.type.hasGain) filter.copy(gainDb = filter.gainDb * filterScale) else filter
        }.filterNot { tone.correction == 0.0 && !it.type.hasGain } +
            curveFilter(IMPORTED_CURVE_ID, tone.importedCurve, filterScale) +
            GraphicEq.filters(
                GraphicEq.normalized(tone.graphicDb, tone.graphicBands).map { it * scale },
                tone.graphicBands,
            ) +
            bassTunerFilters(tone.bassTunerDb * scale, tone.bassTunerHz) +
            toneFilters(tone.bassDb * scale, tone.trebleDb * scale, tone.vocalsDb * scale)
    }

    /** The preset plus loudness compensation, which follows the volume rather than the preset. */
    fun effectiveFilters(state: AppState, attenuationDb: Double): List<Filter> =
        presetFilters(state.current) +
            if (state.loudness) loudnessFilters(attenuationDb, state.loudnessStrength) else emptyList()

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

    /** Each ear's filters: the same sound, plus that ear's own correction. */
    fun earCurves(filters: List<Filter>, ears: EarCorrection): Pair<List<Filter>, List<Filter>> =
        (filters + curveFilter(EAR_CURVE_ID, ears.left)) to (filters + curveFilter(EAR_CURVE_ID, ears.right))

    fun target(
        state: AppState,
        attenuationDb: Double,
        scheduledNight: Boolean = false,
        outputKey: String = "",
    ): ProcessingTarget {
        val filters = effectiveFilters(state, attenuationDb)
        val input = inputOf(state.current, filters, state.autoHeadroom)
        if (!state.bypass) {
            val channels = channelFilters(state, outputKey, filters)
            val (left, right) = channels ?: (filters to filters)
            // One level for both ears. Separate levels would shift the balance across every frequency.
            val shared = if (channels == null) {
                input
            } else {
                min(inputOf(state.current, left, state.autoHeadroom), inputOf(state.current, right, state.autoHeadroom))
            }
            val night = if (scheduledNight) maxOf(state.nightMode, state.nightSchedule.amount) else state.nightMode
            return ProcessingTarget(
                left, shared, state.limiter,
                nightMode = night,
                effectEnabled = true,
                limiterThresholdDb = state.limiterThresholdDb,
                // Only the phone's own effect needs this. Stereo width is already in the channels' filters.
                surround = if (state.useSystemSurround) state.current.tone.surround else 0.0,
                leveller = state.leveller,
                rightFilters = right,
                dynamics = Dynamics.resolve(night, state.leveller, state.current.tone.compressor),
            )
        }
        if (!state.matchBypass) {
            return ProcessingTarget(emptyList(), 0.0, limiter = false, nightMode = 0.0, effectEnabled = false)
        }
        // Flat sound at about the same loudness as the EQ, so comparisons aren't swayed by volume.
        // Never boosted, because a flat signal already peaks at full scale.
        return ProcessingTarget(
            filters = emptyList(),
            inputDb = min(0.0, estimatedLoudnessDb(filters, input)),
            limiter = state.limiter,
            nightMode = 0.0,
            effectEnabled = true,
            limiterThresholdDb = state.limiterThresholdDb,
        )
    }

    /** Approximate perceived level change, using A-weighted pink noise as a stand-in for music. */
    fun estimatedLoudnessDb(filters: List<Filter>, inputDb: Double): Double {
        var weighted = 0.0
        var total = 0.0
        for (i in 0..LOUDNESS_STEPS) {
            val freq = Eq.logFreq(i.toDouble() / LOUDNESS_STEPS)
            val weight = 10.0.pow(aWeightDb(freq) / 10)
            weighted += weight * 10.0.pow((Eq.curveDb(filters, freq) + inputDb) / 10)
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

    private const val LOUDNESS_STEPS = 120
    private const val EXPORT_POINTS = 127
    private const val IMPORTED_CURVE_ID = -101
    private const val EAR_CURVE_ID = -102
    private const val WIDTH_CURVE_ID = -103
}
