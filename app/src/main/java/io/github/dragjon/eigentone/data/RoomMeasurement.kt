package io.github.dragjon.eigentone.data

import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.log10
import kotlin.math.max
import kotlin.math.pow
import kotlin.math.sqrt

/**
 * Measures a speaker in its room with a microphone, and works out a gentle correction. A sequence of
 * steady tones plays through the speaker while the microphone records. Each tone's level is read with a
 * Goertzel filter, which only hears that one frequency, so room noise at other frequencies doesn't count.
 *
 * Pure, so it can be tested with made-up recordings. The playing and recording are in RoomMeasurer.
 */
object RoomMeasurement {
    const val SAMPLE_RATE = 48000

    /** Third-octave steps from 25 Hz to 16 kHz, enough detail for a room without taking long. */
    val FREQS: List<Double> = (0..27).map { 25.0 * 2.0.pow(it / 3.0) }

    /** How loud each tone is, below full scale, so it's clear of noise without straining the speaker. */
    const val TONE_DB = -14.0

    // The sequence: quiet to hear the room, a marker tone to find the speaker's delay, then the tones.
    const val QUIET_MS = 700
    const val MARKER_HZ = 1000.0
    const val MARKER_MS = 400
    const val AFTER_MARKER_MS = 300
    const val TONE_MS = 420
    const val GAP_MS = 80

    /** Room for a Bluetooth speaker's delay, which can be a few hundred milliseconds. */
    const val TAIL_MS = 900

    /** The part of each tone that's read, after it has settled in the room and before it stops. */
    private const val READ_FROM_MS = 120
    private const val READ_MS = 240

    /** A tone has to stand this far above the room's own noise at its frequency to be trusted. */
    const val MIN_SNR_DB = 10.0

    // How far the correction may go. Cuts fix peaks, which rooms are full of. Boosts are kept small,
    // since filling a dip in a room usually just costs headroom without being heard.
    const val MAX_CUT_DB = -12.0
    const val MAX_BOOST_DB = 4.0

    /** Below this level, relative to the middle, the speaker isn't really playing, so it isn't boosted. */
    private const val RANGE_EDGE_DB = -12.0

    fun msToFrames(ms: Int) = SAMPLE_RATE * ms / 1000

    /** Where each tone starts in the sequence, in frames. */
    fun toneStart(index: Int): Int =
        msToFrames(QUIET_MS + MARKER_MS + AFTER_MARKER_MS) + index * msToFrames(TONE_MS + GAP_MS)

    val totalFrames: Int get() = toneStart(FREQS.size) + msToFrames(TAIL_MS)

    /** The test signal, mono, from -1 to 1. Each tone fades in and out so it doesn't click. */
    fun signal(): FloatArray {
        val out = FloatArray(totalFrames)
        val amplitude = 10.0.pow(TONE_DB / 20)
        fun tone(start: Int, frames: Int, hz: Double) {
            val fade = msToFrames(10)
            for (i in 0 until frames) {
                val envelope = minOf(1.0, minOf(i, frames - i).toDouble() / fade)
                out[start + i] = (amplitude * envelope * kotlin.math.sin(2 * PI * hz * i / SAMPLE_RATE)).toFloat()
            }
        }
        tone(msToFrames(QUIET_MS), msToFrames(MARKER_MS), MARKER_HZ)
        FREQS.forEachIndexed { i, hz -> tone(toneStart(i), msToFrames(TONE_MS), hz) }
        return out
    }

    /** The level of one frequency in a stretch of recording, in dB of full scale. */
    fun levelDb(samples: FloatArray, from: Int, frames: Int, hz: Double): Double {
        val start = from.coerceIn(0, samples.size)
        val end = (start + frames).coerceAtMost(samples.size)
        val n = end - start
        if (n <= 0) return -200.0
        val w = 2 * PI * hz / SAMPLE_RATE
        val coefficient = 2 * cos(w)
        var s1 = 0.0
        var s2 = 0.0
        for (i in start until end) {
            // A Hann window keeps a strong neighbouring frequency from leaking in.
            val window = 0.5 - 0.5 * cos(2 * PI * (i - start) / (n - 1).coerceAtLeast(1))
            val s0 = samples[i] * window + coefficient * s1 - s2
            s2 = s1
            s1 = s0
        }
        val power = s1 * s1 + s2 * s2 - coefficient * s1 * s2
        // Scaled so a full-scale sine reads 0 dB. The Hann window passes half the amplitude.
        val amplitude = 2 * sqrt(max(power, 0.0)) / (n * 0.5)
        return 20 * log10(max(amplitude, 1e-10))
    }

    /**
     * How late the speaker played, in frames, found from when the marker tone first appears in the
     * recording. Null when it never clearly does, such as with the volume all the way down.
     */
    fun delayFrames(recording: FloatArray): Int? {
        val hop = msToFrames(10)
        val window = msToFrames(20)
        val expected = msToFrames(QUIET_MS)
        val levels = (0 until (recording.size - window) / hop).map { levelDb(recording, it * hop, window, MARKER_HZ) }
        // The noise is read the same way as the marker, in short windows, or it would look quieter than it is.
        val noise = levels.take(msToFrames(QUIET_MS - 100) / hop).maxOrNull() ?: return null
        val peak = levels.maxOrNull() ?: return null
        if (peak - noise < MIN_SNR_DB) return null
        val threshold = noise + (peak - noise) / 2
        val onset = levels.indexOfFirst { it >= threshold }.takeIf { it >= 0 } ?: return null
        return (onset * hop + window / 2 - expected).coerceAtLeast(0)
    }

    /** One measurement: each frequency's level and the room's noise there, in dB. */
    data class Reading(val levels: List<Double>, val noise: List<Double>)

    /** Reads every tone, allowing for the speaker's delay, and the room's noise at the same frequency. */
    fun read(recording: FloatArray, delay: Int): Reading {
        val read = msToFrames(READ_MS)
        val levels = FREQS.mapIndexed { i, hz ->
            levelDb(recording, toneStart(i) + delay + msToFrames(READ_FROM_MS), read, hz)
        }
        // Read over the same length as the tones, at a few places in the quiet, keeping the loudest.
        val quietStarts = (0..(msToFrames(QUIET_MS - 100) - read) step (read / 2)).toList()
        val noise = FREQS.map { hz -> quietStarts.maxOf { levelDb(recording, it, read, hz) } }
        return Reading(levels, noise)
    }

    /**
     * Combines readings from a few spots around the listening position, which evens out the dips one exact
     * spot has. Levels are averaged as power, so a deep dip at one spot doesn't drag the result down.
     */
    fun combine(readings: List<Reading>): Reading = Reading(
        FREQS.indices.map { i -> 10 * log10(readings.map { 10.0.pow(it.levels[i] / 10) }.average()) },
        FREQS.indices.map { i -> readings.maxOf { it.noise[i] } },
    )

    /** Which frequencies were heard clearly enough to use. */
    fun trusted(reading: Reading): List<Boolean> =
        FREQS.indices.map { reading.levels[it] - reading.noise[it] >= MIN_SNR_DB }

    /**
     * The measured response, smoothed and set so the middle of the range reads 0 dB. Frequencies that
     * weren't heard clearly are null.
     */
    fun response(reading: Reading): List<Double?> {
        val ok = trusted(reading)
        val raw = FREQS.indices.map { if (ok[it]) reading.levels[it] else null }
        // A light, centre-weighted smoothing: enough to steady the reading without flattening real peaks.
        val smoothed = raw.indices.map { i ->
            val here = raw[i] ?: return@map null
            val sides = listOfNotNull(raw.getOrNull(i - 1), raw.getOrNull(i + 1))
            (here * 2 + sides.sum()) / (2 + sides.size)
        }
        val middle = FREQS.indices.filter { FREQS[it] in 200.0..4000.0 }.mapNotNull { smoothed[it] }
        if (middle.isEmpty()) return smoothed.map { null }
        val reference = middle.sorted()[middle.size / 2]
        return smoothed.map { it?.minus(reference) }
    }

    /** True when too little was heard to correct anything, so the result shouldn't be offered. */
    fun tooQuiet(reading: Reading): Boolean = trusted(reading).count { it } < FREQS.size / 2

    /**
     * A gentle correction towards a flat response. Peaks are cut, dips are lifted only a little, and
     * nothing is boosted where the speaker can't really play, such as deep bass from a small speaker.
     */
    fun correction(reading: Reading): List<ShapePoint> {
        val response = response(reading)
        val inRange = response.map { it != null && it >= RANGE_EDGE_DB }
        val lowest = inRange.indexOfFirst { it }
        val highest = inRange.indexOfLast { it }
        return FREQS.indices.map { i ->
            val r = response[i]
            val db = when {
                r == null || lowest < 0 || i < lowest || i > highest -> 0.0
                else -> (-r).coerceIn(MAX_CUT_DB, MAX_BOOST_DB)
            }
            ShapePoint(FREQS[i], kotlin.math.round(db * 10) / 10)
        }
    }
}
