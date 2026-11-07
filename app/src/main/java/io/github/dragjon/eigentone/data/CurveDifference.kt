package io.github.dragjon.eigentone.data

import io.github.dragjon.eigentone.dsp.Eq
import kotlin.math.abs
import kotlin.math.ln
import kotlin.math.round

/**
 * How two presets' curves differ, put the way people talk about sound: more bass, less presence, a
 * brighter overall balance. Pure, so the wording can be unit tested.
 */
object CurveDifference {
    /** The ranges people name, with where each starts and ends in Hz. */
    enum class Region(val lowHz: Double, val highHz: Double) {
        DEEP_BASS(20.0, 60.0),
        BASS(60.0, 250.0),
        LOWER_MIDS(250.0, 500.0),
        MIDS(500.0, 2_000.0),
        PRESENCE(2_000.0, 5_000.0),
        TREBLE(5_000.0, 10_000.0),
        AIR(10_000.0, 20_000.0),
    }

    /** One range where the second curve is [db] higher (or lower, when negative) than the first. */
    data class Part(val region: Region, val db: Double)

    /** [parts] from largest to smallest; [tilt] is how much more the second leans to the bass than to the treble. */
    data class Summary(val parts: List<Part>, val tilt: Double, val largestDb: Double) {
        val nearlyTheSame: Boolean get() = parts.isEmpty() && abs(tilt) < WARMER_DB
    }

    /** A difference smaller than this in a range isn't worth mentioning; most people can't hear it. */
    const val NOTICEABLE_DB = 1.0

    /** A lean of this much between bass and treble reads as warmer or brighter overall. */
    const val WARMER_DB = 1.5

    private const val POINTS = 96

    /** The curve a preset applies, in dB, at [POINTS] points spread evenly in octaves from 20 Hz to 20 kHz. */
    fun curve(preset: Preset): DoubleArray {
        val filters = Processing.presetFilters(preset)
        return DoubleArray(POINTS + 1) { i -> Eq.curveDb(filters, Eq.logFreq(i.toDouble() / POINTS)) }
    }

    private fun freqAt(i: Int) = Eq.logFreq(i.toDouble() / POINTS)

    /** How [second] differs from [first], range by range. Overall level is left out: it's matched when listening. */
    fun compare(first: DoubleArray, second: DoubleArray): Summary {
        val diff = DoubleArray(first.size) { second[it] - first[it] }
        // Level-free, as a listening comparison would be: the middle of the difference counts as zero.
        val offset = diff.sorted()[diff.size / 2]
        fun mean(low: Double, high: Double): Double {
            val inside = diff.indices.filter { freqAt(it) >= low && freqAt(it) <= high }
            return if (inside.isEmpty()) 0.0 else inside.sumOf { diff[it] - offset } / inside.size
        }
        val parts = Region.entries
            .map { Part(it, halfDb(mean(it.lowHz, it.highHz))) }
            .filter { abs(it.db) >= NOTICEABLE_DB }
            .sortedByDescending { abs(it.db) }
        val tilt = halfDb(mean(20.0, 250.0) - mean(2_000.0, 20_000.0))
        val largest = halfDb(diff.maxOf { abs(it - offset) })
        return Summary(parts, tilt, largest)
    }

    fun compare(first: Preset, second: Preset): Summary = compare(curve(first), curve(second))

    private fun halfDb(db: Double) = round(db * 2) / 2

    /** Octaves between two frequencies, for checking ranges. */
    internal fun octaves(low: Double, high: Double) = ln(high / low) / ln(2.0)
}
