package com.phonons.app.data

import com.phonons.app.dsp.AutoEqParser
import com.phonons.app.dsp.Eq
import com.phonons.app.dsp.Filter
import com.phonons.app.dsp.FilterType
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.log2
import kotlin.math.max
import kotlin.math.pow
import kotlin.math.sin

/** A spot in a sweep that stuck out or sounded weak, as the filter gain that would even it out. */
data class SweepMark(val hz: Double, val gainDb: Double)

/** The sums behind the frequency sweep tool. Pure, so they can be unit tested. */
object Sweep {
    const val MIN_LEVEL_DB = -50.0
    const val DEFAULT_LEVEL_DB = -32.0
    const val MAX_LEVEL_DB = HearingTest.CEILING_DB
    const val SECONDS = 60.0

    const val TOO_LOUD_DB = -3.0
    const val TOO_QUIET_DB = 2.0

    /** Headphone peaks and dips are often narrow, so a filter this narrow treats one without its neighbours. */
    private const val MARK_Q = 4.0

    /** Marks closer together than this are the same spot, and go further rather than stacking filters. */
    private const val SAME_SPOT_OCTAVES = 1.0 / 6.0
    private const val MAX_MARK_DB = 9.0

    /** Position along the sweep from 0 to 1, spaced the way ears hear pitch. */
    fun hzAt(position: Double): Double = Eq.logFreq(position.coerceIn(0.0, 1.0))

    /** Adds a mark, or moves an existing one at the same spot further in that direction. */
    fun addMark(marks: List<SweepMark>, hz: Double, gainDb: Double): List<SweepMark> {
        val same = marks.indexOfFirst { abs(log2(it.hz / hz)) < SAME_SPOT_OCTAVES }
        if (same < 0) return (marks + SweepMark(hz, gainDb)).sortedBy { it.hz }
        val merged = (marks[same].gainDb + gainDb).coerceIn(-MAX_MARK_DB, MAX_MARK_DB)
        val updated = if (merged == 0.0) {
            marks.filterIndexed { i, _ -> i != same }
        } else {
            marks.mapIndexed { i, mark -> if (i == same) mark.copy(gainDb = merged) else mark }
        }
        return updated.sortedBy { it.hz }
    }

    /** How many marks a preset can take, since a preset has a limit on its filters. */
    fun roomFor(filters: List<Filter>): Int = max(0, AutoEqParser.MAX_FILTERS - filters.size)

    /** Adds the marks to a preset's filters as narrow bells, leaving the existing ones alone. */
    fun addTo(filters: List<Filter>, marks: List<SweepMark>): List<Filter> {
        val firstId = (filters.maxOfOrNull { it.id } ?: 0) + 1
        return filters + marks.take(roomFor(filters)).mapIndexed { i, mark ->
            Filter(
                id = firstId + i,
                type = FilterType.BELL,
                freq = mark.hz.coerceIn(Eq.MIN_FREQ, Eq.MAX_FREQ),
                gainDb = mark.gainDb,
                q = MARK_Q,
            )
        }
    }
}

/**
 * Makes a steady tone that can change pitch and level while it plays. Both change gradually and the wave
 * carries on from where it was, because a sudden jump in either makes a click that isn't in the headphones.
 */
class SweepOscillator(private val sampleRate: Int) {
    private var phase = 0.0
    private var hz = Sweep.hzAt(0.0)
    private var amplitude = 0.0

    /** Fills stereo samples, heading for this pitch and level. The level can't exceed the safety limit. */
    fun fill(buffer: FloatArray, targetHz: Double, targetDb: Double, sounding: Boolean) {
        val goal = if (sounding) 10.0.pow(targetDb.coerceAtMost(Sweep.MAX_LEVEL_DB) / 20) else 0.0
        val hzStep = (targetHz / hz).pow(1.0 / (GLIDE_SECONDS * sampleRate))
        val amplitudeStep = 1.0 / (FADE_SECONDS * sampleRate)
        for (frame in 0 until buffer.size / 2) {
            hz = if (abs(log2(hz / targetHz)) < PITCH_EPSILON) targetHz else hz * hzStep
            amplitude = when {
                amplitude < goal -> minOf(goal, amplitude + amplitudeStep * MAX_LEVEL)
                amplitude > goal -> maxOf(goal, amplitude - amplitudeStep * MAX_LEVEL)
                else -> amplitude
            }
            phase += 2 * PI * hz / sampleRate
            if (phase > 2 * PI) phase -= 2 * PI
            val sample = (amplitude * sin(phase)).toFloat()
            buffer[frame * 2] = sample
            buffer[frame * 2 + 1] = sample
        }
    }

    /** Whether it has faded fully out, so stopping won't click. */
    val isSilent: Boolean get() = amplitude == 0.0

    private companion object {
        const val GLIDE_SECONDS = 0.1
        const val FADE_SECONDS = 0.05
        const val PITCH_EPSILON = 0.0005
        val MAX_LEVEL = 10.0.pow(Sweep.MAX_LEVEL_DB / 20)
    }
}
