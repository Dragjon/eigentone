package io.github.dragjon.eigentone.dsp

import kotlinx.serialization.Serializable
import kotlinx.serialization.Transient
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
    /** A response defined by points, used by the graphic EQ. Never part of a saved preset's filters. */
    CURVE("Curve", false),
    /** Removes a narrow range around the frequency completely. Q sets how narrow. */
    NOTCH("Notch", false),
    /** Keeps only a range around the frequency. Q sets how narrow. */
    BAND_PASS("Band pass", false),
    /** Tips the whole balance around the frequency: the top end goes up by the gain and the bass down by the same, or the other way round. */
    TILT("Tilt", true);

    /** High and low pass, the only types whose steepness can be chosen. */
    val hasSlope: Boolean get() = this == LOW_PASS || this == HIGH_PASS
}

@Serializable
enum class FilterChannel { BOTH, LEFT, RIGHT }

/** How a dynamic filter reacts: above the threshold, each dB louder comes out 1/ratio dB louder. */
@Serializable
data class FilterDynamics(val thresholdDb: Double = -30.0, val ratio: Double = 3.0) {
    fun clamped() = FilterDynamics(
        thresholdDb.takeIf { it.isFinite() }?.coerceIn(-60.0, 0.0) ?: -30.0,
        ratio.takeIf { it.isFinite() }?.coerceIn(1.0, 10.0) ?: 3.0,
    )
}

@Serializable
data class Filter(
    val id: Int,
    val type: FilterType = FilterType.BELL,
    val freq: Double = 1000.0,
    val gainDb: Double = 0.0,
    val q: Double = 1.0,
    val enabled: Boolean = true,
    @Transient val curve: List<Pair<Double, Double>> = emptyList(),
    /** How steep a high or low pass is, in dB per octave. 12 is a single section, as before slopes could be chosen. */
    val slope: Int = Eq.DEFAULT_SLOPE,
    /** Which channel it shapes. Most filters shape both; one side only is for evening out an uneven pair. */
    val channel: FilterChannel = FilterChannel.BOTH,
    /** Also turns this filter's range down while it's louder than a threshold. Null for a fixed filter. */
    val dynamic: FilterDynamics? = null,
) {
    /** Bells and shelves can be dynamic, on both channels, because the compressor bands shape both at once. */
    val canBeDynamic: Boolean get() =
        channel == FilterChannel.BOTH && (type == FilterType.BELL || type == FilterType.LOW_SHELF || type == FilterType.HIGH_SHELF)

    /** The range a dynamic filter watches and turns down: around a bell's centre, or a shelf's side. */
    fun dynamicRange(): ClosedFloatingPointRange<Double> {
        val half = 2.0.pow(Eq.bandwidthOctaves(q).coerceAtLeast(Eq.MIN_DYNAMIC_OCTAVES) / 2)
        return when (type) {
            FilterType.LOW_SHELF -> Eq.MIN_FREQ..(freq * half).coerceAtMost(Eq.MAX_FREQ)
            FilterType.HIGH_SHELF -> (freq / half).coerceAtLeast(Eq.MIN_FREQ)..Eq.MAX_FREQ
            else -> (freq / half).coerceAtLeast(Eq.MIN_FREQ)..(freq * half).coerceAtMost(Eq.MAX_FREQ)
        }
    }
    /** Built once, because curves are sampled thousands of times while settings change. */
    val sections: List<Biquad> by lazy(LazyThreadSafetyMode.PUBLICATION) { Eq.sections(this) }
}

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
    const val DEFAULT_SLOPE = 12

    /** Compressor bands narrower than this can't be split cleanly, so a narrow dynamic bell watches this much. */
    const val MIN_DYNAMIC_OCTAVES = 2.0 / 3.0

    /** A bell's width in octaves for its Q. */
    fun bandwidthOctaves(q: Double): Double {
        val x = 1 / (2 * q.coerceAtLeast(MIN_Q))
        return 2 / kotlin.math.ln(2.0) * kotlin.math.ln(x + sqrt(x * x + 1))
    }
    val SLOPES = listOf(6, 12, 18, 24, 36, 48)
    /** The Q of a 12 dB pass with no peak at the corner. */
    const val BUTTERWORTH_Q = 0.7071067811865476

    /**
     * The sections that make up a filter. A steeper pass is a Butterworth cascade, with a first-order
     * section for slopes that aren't a multiple of 12, and Q scales the sharpest section, so 0.71 is flat
     * at the corner and higher values add a resonant peak there, as on a 12 dB slope.
     */
    fun sections(f: Filter, sampleRate: Double = SAMPLE_RATE): List<Biquad> = when (f.type) {
        FilterType.LOW_PASS, FilterType.HIGH_PASS -> {
            val order = (slopeOf(f) / 6).coerceAtLeast(1)
            val qs = butterworthQs(order)
            val scale = f.q / BUTTERWORTH_Q
            val sharpest = qs.indices.maxByOrNull { qs[it] }
            buildList {
                if (order % 2 == 1) add(firstOrder(f, sampleRate))
                qs.forEachIndexed { i, q -> add(biquad(f, sampleRate, q = if (i == sharpest) q * scale else q)) }
            }
        }
        FilterType.TILT -> listOf(
            biquad(f.copy(type = FilterType.LOW_SHELF, gainDb = -f.gainDb), sampleRate),
            biquad(f.copy(type = FilterType.HIGH_SHELF, gainDb = f.gainDb), sampleRate),
        )
        else -> listOf(biquad(f, sampleRate))
    }

    /** The slope actually used: one of [SLOPES], and 12 for anything else. */
    fun slopeOf(f: Filter): Int = if (f.slope in SLOPES) f.slope else DEFAULT_SLOPE

    /** The Q of each second-order section of a Butterworth filter of this order. */
    fun butterworthQs(order: Int): List<Double> =
        if (order % 2 == 0) {
            (1..order / 2).map { k -> 1 / (2 * cos((2 * k - 1) * PI / (2 * order))) }
        } else {
            // Odd orders have a real pole for the first-order section, so the pairs sit at k·π/n.
            (1..order / 2).map { k -> 1 / (2 * cos(k * PI / order)) }
        }

    /** A first-order low or high pass, from the bilinear transform, falling 6 dB per octave. */
    private fun firstOrder(f: Filter, sampleRate: Double): Biquad {
        val k = kotlin.math.tan(PI * min(f.freq, sampleRate * 0.49) / sampleRate)
        val a1 = (k - 1) / (k + 1)
        return if (f.type == FilterType.LOW_PASS) {
            Biquad(k / (1 + k), k / (1 + k), 0.0, a1, 0.0)
        } else {
            Biquad(1 / (1 + k), -1 / (1 + k), 0.0, a1, 0.0)
        }
    }

    /**
     * Whether a filter can be written to a ParametricEQ file. Those have no first-order sections, so
     * 6 and 18 dB slopes can't be.
     */
    fun canExport(f: Filter): Boolean = when {
        f.type == FilterType.CURVE -> false
        f.channel != FilterChannel.BOTH -> false
        f.type.hasSlope -> (slopeOf(f) / 6) % 2 == 0
        else -> true
    }

    fun biquad(f: Filter, sampleRate: Double = SAMPLE_RATE, q: Double = f.q): Biquad {
        val freq = min(f.freq, sampleRate * 0.49)
        val w0 = 2 * PI * freq / sampleRate
        val c = cos(w0)
        val alpha = sin(w0) / (2 * q.coerceAtLeast(MIN_Q))
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
            FilterType.NOTCH -> {
                b0 = 1.0; b1 = -2 * c; b2 = 1.0
                a0 = 1 + alpha; a1 = -2 * c; a2 = 1 - alpha
            }
            // Constant 0 dB at the peak, so it only takes away.
            FilterType.BAND_PASS -> {
                b0 = alpha; b1 = 0.0; b2 = -alpha
                a0 = 1 + alpha; a1 = -2 * c; a2 = 1 - alpha
            }
            // Handled in filterDb and sections. A unit response here keeps this function total.
            FilterType.CURVE, FilterType.TILT -> {
                b0 = 1.0; b1 = 0.0; b2 = 0.0
                a0 = 1.0; a1 = 0.0; a2 = 0.0
            }
        }
        return Biquad(b0 / a0, b1 / a0, b2 / a0, a1 / a0, a2 / a0)
    }

    fun filterDb(f: Filter, freq: Double): Double =
        when {
            !f.enabled -> 0.0
            f.type == FilterType.CURVE -> GraphicEq.interpolate(f.curve, freq)
            else -> f.sections.sumOf { it.magnitudeDb(freq, SAMPLE_RATE) }
        }

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
