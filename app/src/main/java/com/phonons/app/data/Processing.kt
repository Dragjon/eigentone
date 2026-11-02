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
        return preset.filters.map { filter ->
            if (scale != 1.0 && filter.type.hasGain) filter.copy(gainDb = filter.gainDb * scale) else filter
        } +
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

    fun target(state: AppState, attenuationDb: Double): ProcessingTarget {
        val filters = effectiveFilters(state, attenuationDb)
        val input = Eq.inputGainDb(filters, state.current.preampDb, state.autoHeadroom)
        if (!state.bypass) {
            return ProcessingTarget(
                filters, input, state.limiter, state.nightMode, effectEnabled = true,
                limiterThresholdDb = state.limiterThresholdDb,
                surround = state.current.tone.surround,
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
}
