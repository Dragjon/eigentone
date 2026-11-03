package com.phonons.app.dsp

import kotlin.math.abs
import kotlin.math.ln
import kotlin.math.round

/** A named starting shape, as points that can be applied at any band count. */
data class GraphicShape(val name: String, val points: List<Pair<Double, Double>>)

/**
 * Fixed-band equaliser.
 *
 * Traditional graphic EQs use overlapping bell filters, whose curves add up and ripple between the
 * faders. The audio engine here takes a sampled curve rather than real filters, so the faders define
 * the curve directly as straight lines between them. Measured against bells of every width tried,
 * which strayed by up to 3.3 dB, this has no bell ripple and is cheaper to calculate.
 */
object GraphicEq {
    const val MAX_DB = 15.0
    val BAND_OPTIONS = listOf(10, 15, 31)
    const val DEFAULT_BANDS = 10

    private val octave = listOf(31.25, 62.5, 125.0, 250.0, 500.0, 1000.0, 2000.0, 4000.0, 8000.0, 16000.0)
    private val twoThirds = listOf(
        25.0, 40.0, 63.0, 100.0, 160.0, 250.0, 400.0, 630.0, 1000.0, 1600.0, 2500.0, 4000.0, 6300.0, 10000.0, 16000.0,
    )
    private val third = listOf(
        20.0, 25.0, 31.5, 40.0, 50.0, 63.0, 80.0, 100.0, 125.0, 160.0, 200.0, 250.0, 315.0, 400.0, 500.0, 630.0,
        800.0, 1000.0, 1250.0, 1600.0, 2000.0, 2500.0, 3150.0, 4000.0, 5000.0, 6300.0, 8000.0, 10000.0, 12500.0,
        16000.0, 20000.0,
    )

    /** The 9-band layout from before band counts could be chosen. Only used to read old settings. */
    private val legacyNine = listOf(62.5, 125.0, 250.0, 500.0, 1000.0, 2000.0, 4000.0, 8000.0, 16000.0)

    val shapes = listOf(
        GraphicShape("Flat", listOf(1000.0 to 0.0)),
        GraphicShape("Bass boost", listOf(40.0 to 6.0, 100.0 to 5.0, 200.0 to 2.0, 400.0 to 0.0)),
        GraphicShape("Warm", listOf(60.0 to 3.5, 200.0 to 2.0, 1000.0 to 0.0, 4000.0 to -1.5, 12000.0 to -3.0)),
        GraphicShape("Vocals", listOf(100.0 to -2.0, 300.0 to -1.0, 1000.0 to 1.0, 2500.0 to 3.5, 5000.0 to 2.0, 12000.0 to 0.0)),
        GraphicShape("Bright", listOf(1000.0 to 0.0, 3000.0 to 1.5, 8000.0 to 3.5, 16000.0 to 5.0)),
        GraphicShape("V-shape", listOf(60.0 to 5.0, 200.0 to 2.0, 1000.0 to -2.5, 4000.0 to 1.0, 10000.0 to 4.5)),
        GraphicShape("Speech", listOf(60.0 to -8.0, 150.0 to -3.0, 400.0 to 0.0, 2500.0 to 4.0, 8000.0 to 0.0, 16000.0 to -3.0)),
    )

    fun frequencies(bandCount: Int): List<Double> = when (bandCount) {
        9 -> legacyNine
        15 -> twoThirds
        31 -> third
        else -> octave
    }

    fun formatFrequency(hz: Double): String = when {
        hz >= 1000 -> {
            val k = hz / 1000
            if (k == round(k)) "${k.toInt()} kHz" else "${"%.1f".format(java.util.Locale.US, k)} kHz"
        }
        hz == round(hz) -> "${hz.toInt()} Hz"
        else -> "${"%.1f".format(java.util.Locale.US, hz)} Hz"
    }

    /** Half-dB steps within the fader range, which also makes it easy to land on exactly zero. */
    fun snap(db: Double): Double = (round(db * 2) / 2).coerceIn(-MAX_DB, MAX_DB)

    /** Straight-line interpolation on a log frequency axis, held flat beyond either end. */
    internal fun interpolate(points: List<Pair<Double, Double>>, freq: Double): Double {
        if (points.isEmpty()) return 0.0
        if (freq <= points.first().first) return points.first().second
        if (freq >= points.last().first) return points.last().second
        val upper = points.indexOfFirst { it.first >= freq }
        val (f0, g0) = points[upper - 1]
        val (f1, g1) = points[upper]
        val t = ln(freq / f0) / ln(f1 / f0)
        return g0 + (g1 - g0) * t
    }

    /** Moves a curve to a different band count while keeping its shape. */
    fun resample(gains: List<Double>, fromBands: Int, toBands: Int): List<Double> {
        val points = frequencies(fromBands).zip(List(fromBands) { gains.getOrElse(it) { 0.0 } })
        return frequencies(toBands).map { snap(interpolate(points, it)) }
    }

    /** Returns exactly [bandCount] gains, adapting anything saved with a different layout. */
    fun normalized(gains: List<Double>, bandCount: Int): List<Double> = when {
        gains.size == bandCount -> gains
        gains.isEmpty() -> List(bandCount) { 0.0 }
        else -> resample(gains, gains.size, bandCount)
    }

    fun apply(shape: GraphicShape, bandCount: Int, amount: Double = 1.0): List<Double> =
        frequencies(bandCount).map { snap(interpolate(shape.points, it) * amount) }

    /** Adds a shape to existing faders, so shapes can be layered. */
    fun layered(base: List<Double>, shape: GraphicShape, bandCount: Int, amount: Double = 1.0): List<Double> {
        val existing = normalized(base, bandCount)
        return apply(shape, bandCount, amount).mapIndexed { i, gain -> snap(existing[i] + gain) }
    }

    /**
     * The largest gap in dB between a shape and what [bandCount] faders can make of it. Measured on the
     * finest fader layout, because faders that miss a shape's points blur its detail.
     */
    fun followError(shape: GraphicShape, bandCount: Int): Double {
        val faders = frequencies(bandCount).zip(apply(shape, bandCount))
        return frequencies(31).maxOf { hz -> abs(interpolate(shape.points, hz) - interpolate(faders, hz)) }
    }

    /** Softens sharp steps between neighbouring faders. */
    fun smooth(gains: List<Double>): List<Double> = gains.indices.map { i ->
        val before = gains.getOrElse(i - 1) { gains[i] }
        val after = gains.getOrElse(i + 1) { gains[i] }
        snap(before * 0.25 + gains[i] * 0.5 + after * 0.25)
    }

    fun isFlat(gains: List<Double>): Boolean = gains.all { it == 0.0 }

    /** Sets the faders between two touch points, so a fast swipe doesn't skip any. */
    fun drawLine(gains: List<Double>, fromBand: Int, fromDb: Double, toBand: Int, toDb: Double): List<Double> {
        val out = gains.toMutableList()
        val span = abs(toBand - fromBand)
        for (step in 0..span) {
            val t = if (span == 0) 1.0 else step.toDouble() / span
            val band = if (toBand >= fromBand) fromBand + step else fromBand - step
            if (band in out.indices) out[band] = snap(fromDb + (toDb - fromDb) * t)
        }
        return out
    }

    private data class Solved(val gains: List<Double>, val bandCount: Int, val filters: List<Filter>)

    @Volatile private var lastSolved: Solved? = null

    /** The faders as a curve, or nothing when they're flat. */
    fun filters(gains: List<Double>, bandCount: Int): List<Filter> {
        val targets = normalized(gains, bandCount)
        if (isFlat(targets)) return emptyList()
        lastSolved?.let { if (it.bandCount == bandCount && it.gains == targets) return it.filters }

        val result = listOf(
            Filter(FILTER_ID, FilterType.CURVE, curve = frequencies(bandCount).zip(targets)),
        )
        lastSolved = Solved(targets, bandCount, result)
        return result
    }

    private const val FILTER_ID = -100

    private const val FILTER_ID_BASE = -100
}
