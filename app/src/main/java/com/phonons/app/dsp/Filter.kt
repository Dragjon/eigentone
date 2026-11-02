package com.phonons.app.dsp

import kotlinx.serialization.Serializable
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.log10
import kotlin.math.max
import kotlin.math.min
import kotlin.math.pow
import kotlin.math.sin
import kotlin.math.sqrt

@Serializable
enum class FilterType(val label: String, val hasGain: Boolean) {
    BELL("Bell", true),
    LOW_SHELF("Low shelf", true),
    HIGH_SHELF("High shelf", true),
    LOW_PASS("Low pass", false),
    HIGH_PASS("High pass", false),
}

@Serializable
data class Filter(
    val id: Int,
    val type: FilterType = FilterType.BELL,
    val freq: Double = 1000.0,
    val gainDb: Double = 0.0,
    val q: Double = 1.0,
    val enabled: Boolean = true,
)

/** Normalised biquad coefficients (a0 == 1), from the RBJ Audio EQ Cookbook. */
class Biquad(val b0: Double, val b1: Double, val b2: Double, val a1: Double, val a2: Double) {
    fun magnitudeDb(freq: Double, sampleRate: Double): Double {
        val w = 2 * PI * freq / sampleRate
        val c1 = cos(w)
        val c2 = cos(2 * w)
        val num = b0 * b0 + b1 * b1 + b2 * b2 + 2 * (b0 * b1 + b1 * b2) * c1 + 2 * b0 * b2 * c2
        val den = 1 + a1 * a1 + a2 * a2 + 2 * (a1 + a1 * a2) * c1 + 2 * a2 * c2
        return 10 * log10(max(num, 1e-30) / max(den, 1e-30))
    }
}

object Eq {
    const val SAMPLE_RATE = 48_000.0
    const val MIN_FREQ = 20.0
    const val MAX_FREQ = 20_000.0
    const val MIN_Q = 0.1
    const val MAX_Q = 12.0
    const val MAX_GAIN = 18.0

    fun biquad(f: Filter, sampleRate: Double = SAMPLE_RATE): Biquad {
        val freq = min(f.freq, sampleRate * 0.49)
        val w0 = 2 * PI * freq / sampleRate
        val c = cos(w0)
        val alpha = sin(w0) / (2 * f.q.coerceAtLeast(MIN_Q))
        val a = 10.0.pow(f.gainDb / 40)
        val sq = 2 * sqrt(a) * alpha

        val b0: Double
        val b1: Double
        val b2: Double
        val a0: Double
        val a1: Double
        val a2: Double
        when (f.type) {
            FilterType.BELL -> {
                b0 = 1 + alpha * a; b1 = -2 * c; b2 = 1 - alpha * a
                a0 = 1 + alpha / a; a1 = -2 * c; a2 = 1 - alpha / a
            }
            FilterType.LOW_SHELF -> {
                b0 = a * ((a + 1) - (a - 1) * c + sq)
                b1 = 2 * a * ((a - 1) - (a + 1) * c)
                b2 = a * ((a + 1) - (a - 1) * c - sq)
                a0 = (a + 1) + (a - 1) * c + sq
                a1 = -2 * ((a - 1) + (a + 1) * c)
                a2 = (a + 1) + (a - 1) * c - sq
            }
            FilterType.HIGH_SHELF -> {
                b0 = a * ((a + 1) + (a - 1) * c + sq)
                b1 = -2 * a * ((a - 1) + (a + 1) * c)
                b2 = a * ((a + 1) + (a - 1) * c - sq)
                a0 = (a + 1) - (a - 1) * c + sq
                a1 = 2 * ((a - 1) - (a + 1) * c)
                a2 = (a + 1) - (a - 1) * c - sq
            }
            FilterType.LOW_PASS -> {
                b0 = (1 - c) / 2; b1 = 1 - c; b2 = (1 - c) / 2
                a0 = 1 + alpha; a1 = -2 * c; a2 = 1 - alpha
            }
            FilterType.HIGH_PASS -> {
                b0 = (1 + c) / 2; b1 = -(1 + c); b2 = (1 + c) / 2
                a0 = 1 + alpha; a1 = -2 * c; a2 = 1 - alpha
            }
        }
        return Biquad(b0 / a0, b1 / a0, b2 / a0, a1 / a0, a2 / a0)
    }

    fun filterDb(f: Filter, freq: Double): Double =
        if (f.enabled) biquad(f).magnitudeDb(freq, SAMPLE_RATE) else 0.0

    /** Combined response of the whole filter stack, excluding preamp. */
    fun curveDb(filters: List<Filter>, freq: Double): Double =
        filters.sumOf { filterDb(it, freq) }

    /** Highest boost anywhere in the audible range, used for automatic headroom. */
    fun peakDb(filters: List<Filter>): Double {
        var peak = Double.NEGATIVE_INFINITY
        val steps = 720
        for (i in 0..steps) {
            peak = max(peak, curveDb(filters, logFreq(i.toDouble() / steps)))
        }
        return peak
    }

    /** Maps 0..1 onto the log frequency axis. */
    fun logFreq(t: Double): Double = MIN_FREQ * (MAX_FREQ / MIN_FREQ).pow(t)

    fun logPosition(freq: Double): Double =
        log10(freq / MIN_FREQ) / log10(MAX_FREQ / MIN_FREQ)

    /** Input gain so that the loudest boost cannot exceed 0 dB when headroom is automatic. */
    fun inputGainDb(filters: List<Filter>, preampDb: Double, autoHeadroom: Boolean): Double {
        if (!autoHeadroom) return preampDb
        return preampDb - max(0.0, peakDb(filters) + preampDb)
    }
}
