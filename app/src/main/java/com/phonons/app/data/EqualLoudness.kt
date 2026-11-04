package com.phonons.app.data

import kotlin.math.log10
import kotlin.math.pow

/**
 * Keeps the balance of music the same as the volume goes down. Ears lose bass, and some treble, faster than
 * midrange as sound gets quieter. This uses the ISO 226:2003 equal-loudness contours to work out how much,
 * compared with how it sounds at your usual volume, which is assumed to be about 80 phon, a typical loud
 * listening level. It follows the phone's volume, so it's only an estimate of the real level at your ears.
 */
object EqualLoudness {
    /** How loud your usual volume is taken to be. Music is often mixed and enjoyed around here. */
    const val USUAL_PHON = 80.0

    /** Where to start when a pair of headphones hasn't been set up yet, in dB below full volume. */
    const val DEFAULT_USUAL_DB = 10.0

    /** The contours are measured down to 20 phon, which is about as quiet as music gets. */
    private const val QUIETEST_PHON = 20.0

    /** Large enough to restore bass at very low volume, without asking too much of small drivers. */
    const val MAX_BOOST_DB = 15.0

    /** Smaller differences than this aren't worth a filter. */
    private const val SMALLEST_DB = 0.5

    // ISO 226:2003, one-third octave frequencies and their parameters.
    private val FREQS = doubleArrayOf(20.0, 25.0, 31.5, 40.0, 50.0, 63.0, 80.0, 100.0, 125.0, 160.0, 200.0, 250.0, 315.0, 400.0, 500.0, 630.0, 800.0, 1000.0, 1250.0, 1600.0, 2000.0, 2500.0, 3150.0, 4000.0, 5000.0, 6300.0, 8000.0, 10000.0, 12500.0)
    private val AF = doubleArrayOf(.532, .506, .480, .455, .432, .409, .387, .367, .349, .330, .315, .301, .288, .276, .267, .259, .253, .250, .246, .244, .243, .243, .243, .242, .242, .245, .254, .271, .301)
    private val LU = doubleArrayOf(-31.6, -27.2, -23.0, -19.1, -15.9, -13.0, -10.3, -8.1, -6.2, -4.5, -3.1, -2.0, -1.1, -.4, 0.0, .3, .5, 0.0, -2.7, -4.1, -1.0, 1.7, 2.5, 1.2, -2.1, -7.1, -11.2, -10.7, -3.1)
    private val TF = doubleArrayOf(78.5, 68.7, 59.5, 51.1, 44.0, 37.5, 31.5, 26.5, 22.1, 17.9, 14.4, 11.4, 8.6, 6.2, 4.4, 3.0, 2.2, 2.4, 3.5, 1.7, -1.3, -4.2, -6.0, -5.4, -1.5, 6.0, 12.6, 13.9, 12.3)
    private val ONE_KHZ = FREQS.indexOfFirst { it == 1000.0 }

    /** The sound pressure each frequency needs to sound as loud as [phon] does at 1 kHz. */
    private fun spl(phon: Double): DoubleArray = DoubleArray(FREQS.size) { i ->
        val a = 4.47e-3 * (10.0.pow(0.025 * phon) - 1.15) + (0.4 * 10.0.pow((TF[i] + LU[i]) / 10 - 9)).pow(AF[i])
        10 / AF[i] * log10(a) - LU[i] + 94
    }

    private val usual = spl(USUAL_PHON)

    /**
     * What to add so music [quieterDb] below your usual volume keeps its balance, scaled by [strength].
     * Nothing at or above your usual volume, because louder listening doesn't need less bass.
     */
    fun correction(quieterDb: Double, strength: Double): List<ShapePoint> {
        if (quieterDb < SMALLEST_DB || strength <= 0.0) return emptyList()
        val now = spl((USUAL_PHON - quieterDb).coerceAtLeast(QUIETEST_PHON))
        val atReference = now[ONE_KHZ] - usual[ONE_KHZ]
        val points = FREQS.indices.map { i ->
            val db = ((now[i] - usual[i] - atReference) * strength.coerceIn(0.0, 1.0)).coerceIn(-MAX_BOOST_DB, MAX_BOOST_DB)
            ShapePoint(FREQS[i], Math.round(db * 10) / 10.0)
        }
        return points.takeIf { list -> list.any { kotlin.math.abs(it.db) >= SMALLEST_DB } } ?: emptyList()
    }

    /** How far below your usual volume the phone is now, never negative. */
    fun quieter(attenuationDb: Double, usualDb: Double): Double = (attenuationDb - usualDb).coerceAtLeast(0.0)
}
