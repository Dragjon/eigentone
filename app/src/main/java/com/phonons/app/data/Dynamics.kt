package com.phonons.app.data

import kotlin.math.abs
import kotlin.math.pow
import kotlinx.serialization.Serializable

/** One frequency range of the compressor. */
@Serializable
data class CompressorBand(
    val enabled: Boolean = true,
    val thresholdDb: Double = -24.0,
    val ratio: Double = 2.0,
    val attackMs: Double = 20.0,
    val releaseMs: Double = 200.0,
    val kneeDb: Double = 6.0,
    val makeupDb: Double = 0.0,
) {
    /**
     * The level that comes out for a steady level going in, using the standard soft-knee curve. It's
     * for showing the setting. Real audio also depends on attack and release.
     */
    fun outputDb(inputDb: Double): Double {
        if (!enabled) return inputDb
        val over = inputDb - thresholdDb
        val compressed = when {
            kneeDb > 0 && 2 * abs(over) <= kneeDb -> {
                val into = over + kneeDb / 2
                inputDb + (1 / ratio - 1) * into * into / (2 * kneeDb)
            }
            over <= 0 -> inputDb
            else -> thresholdDb + over / ratio
        }
        return compressed + makeupDb
    }

    /** Keeps values from a backup, or anywhere else outside the app, in the range the controls allow. */
    fun clamped() = copy(
        thresholdDb = limit(thresholdDb, CompressorBand().thresholdDb, Dynamics.THRESHOLD_DB),
        ratio = limit(ratio, CompressorBand().ratio, Dynamics.RATIO),
        attackMs = limit(attackMs, CompressorBand().attackMs, Dynamics.ATTACK_MS),
        releaseMs = limit(releaseMs, CompressorBand().releaseMs, Dynamics.RELEASE_MS),
        kneeDb = limit(kneeDb, CompressorBand().kneeDb, Dynamics.KNEE_DB),
        makeupDb = limit(makeupDb, CompressorBand().makeupDb, Dynamics.MAKEUP_DB),
    )

    private fun limit(value: Double, fallback: Double, range: ClosedFloatingPointRange<Double>) =
        if (value.isFinite()) value.coerceIn(range) else fallback
}

/** A three-band compressor. The crossovers set where the bands meet. */
@Serializable
data class Compressor(
    val enabled: Boolean = false,
    val lowCrossoverHz: Double = 200.0,
    val highCrossoverHz: Double = 2500.0,
    val low: CompressorBand = CompressorBand(),
    val mid: CompressorBand = CompressorBand(),
    val high: CompressorBand = CompressorBand(),
) {
    val bands: List<CompressorBand> get() = listOf(low, mid, high)

    fun withBand(index: Int, band: CompressorBand) = when (index) {
        0 -> copy(low = band)
        1 -> copy(mid = band)
        else -> copy(high = band)
    }

    fun clamped() = copy(
        lowCrossoverHz = lowCrossoverHz.takeIf { it.isFinite() }?.coerceIn(Dynamics.LOW_CROSSOVER_HZ) ?: 200.0,
        highCrossoverHz = highCrossoverHz.takeIf { it.isFinite() }?.coerceIn(Dynamics.HIGH_CROSSOVER_HZ) ?: 2500.0,
        low = low.clamped(),
        mid = mid.clamped(),
        high = high.clamped(),
    )
}

data class CompressorStyle(val name: String, val compressor: Compressor)

/** How strongly to hold back each range, from 0 for off to 1. */
data class Tamers(val harsh: Double = 0.0, val boom: Double = 0.0)

/** One band as the audio effect receives it. */
data class PlannedBand(val cutoffHz: Double, val band: CompressorBand, val active: Boolean)

object Dynamics {
    val THRESHOLD_DB = -60.0..0.0
    val RATIO = 1.0..20.0
    val ATTACK_MS = 0.5..200.0
    val RELEASE_MS = 10.0..2000.0
    val KNEE_DB = 0.0..24.0
    val MAKEUP_DB = 0.0..12.0

    // These can't meet, so the bands always stay in order, which the audio engine requires.
    val LOW_CROSSOVER_HZ = 40.0..800.0
    val HIGH_CROSSOVER_HZ = 1000.0..12000.0

    private const val ACTIVE = 0.001

    /**
     * What a band that should do nothing receives. Android's effect keeps each band's on or off flag but
     * never checks it, so once any band is active it compresses all of them with whatever they hold. At a
     * ratio of 1:1 with no makeup, the result is the same as the input at every level.
     */
    private val UNTOUCHED = CompressorBand(enabled = false, thresholdDb = 0.0, ratio = 1.0, makeupDb = 0.0)

    /** The full set of bands, which leaves room for the tamers beside the compressor's three. */
    const val FULL_BANDS = 6

    /** For a phone that refuses more, which keeps the compressor but can't offer the tamers. */
    const val BASIC_BANDS = 3

    const val BOOM_TOP_HZ = 110.0
    const val HARSH_LOW_HZ = 4000.0
    const val HARSH_HIGH_HZ = 9000.0
    private const val TOP_HZ = 24000.0
    private const val SPARE_FROM_HZ = 20000.0

    /** Low thresholds, because a single narrow range holds much less level than the whole of the music. */
    fun harshBand(amount: Double) = CompressorBand(
        thresholdDb = -22.0 - 14.0 * amount,
        ratio = 1.5 + 3.5 * amount,
        attackMs = 3.0,
        releaseMs = 80.0,
    )

    fun boomBand(amount: Double) = CompressorBand(
        thresholdDb = -14.0 - 12.0 * amount,
        ratio = 1.5 + 2.5 * amount,
        attackMs = 15.0,
        releaseMs = 150.0,
    )

    /**
     * Combines the compressor and the tamers into the bands the effect has. Where both apply, the band
     * does the stronger of the two, and keeps the compressor's makeup. Always gives exactly [slots] bands
     * in rising order, because the effect's band count is fixed when it's created and can't have gaps.
     */
    fun plan(compressor: Compressor, tamers: Tamers, slots: Int = FULL_BANDS): List<PlannedBand> {
        val harsh = if (slots >= FULL_BANDS) tamers.harsh else 0.0
        val boom = if (slots >= FULL_BANDS) tamers.boom else 0.0
        val edges = sortedSetOf(compressor.lowCrossoverHz, compressor.highCrossoverHz)
        if (boom > ACTIVE) edges += BOOM_TOP_HZ
        if (harsh > ACTIVE) {
            edges += HARSH_LOW_HZ
            edges += HARSH_HIGH_HZ
        }
        // Spare slots go above hearing. Splitting an audible band would change how it compresses, because
        // a narrower band carries less level and so crosses the threshold less often.
        val spare = slots - edges.size - 1
        val cutoffs = edges.toList() +
            List(spare) { SPARE_FROM_HZ * (TOP_HZ / SPARE_FROM_HZ).pow(it.toDouble() / spare) } +
            TOP_HZ
        return cutoffs.mapIndexed { i, top ->
            val bottom = if (i == 0) LOWEST_HZ else cutoffs[i - 1]
            val own = bandAt(compressor, kotlin.math.sqrt(bottom * top))
            val ownActive = compressor.enabled && own.enabled
            val tamer = when {
                boom > ACTIVE && top <= BOOM_TOP_HZ -> boomBand(boom)
                harsh > ACTIVE && bottom >= HARSH_LOW_HZ && top <= HARSH_HIGH_HZ -> harshBand(harsh)
                else -> null
            }
            val band = when {
                tamer == null -> own
                !ownActive -> tamer
                else -> own.copy(
                    thresholdDb = minOf(own.thresholdDb, tamer.thresholdDb),
                    ratio = maxOf(own.ratio, tamer.ratio),
                    attackMs = minOf(own.attackMs, tamer.attackMs),
                    releaseMs = minOf(own.releaseMs, tamer.releaseMs),
                )
            }
            val active = ownActive || tamer != null
            PlannedBand(top, if (active) band else UNTOUCHED, active)
        }
    }

    private const val LOWEST_HZ = 20.0

    /** A typical level for music in a band, used to estimate what compression does to loudness. */
    private const val TYPICAL_LEVEL_DB = -18.0

    private const val MAX_MAKEUP_STEP_DB = 1.5
    private const val MAX_THRESHOLD_STEP_DB = 6.0
    private const val MAX_RATIO_STEP = 1.5
    private const val MAX_CROSSOVER_STEP = 1.5

    /**
     * Roughly how much the compressor changes the level at this frequency, for music at a typical level.
     * Real music varies, so this is only good enough for matching loudness, not for measuring it.
     */
    fun gainDb(compressor: Compressor, hz: Double, levelDb: Double): Double {
        if (!compressor.enabled) return 0.0
        val band = bandAt(compressor, hz)
        val input = TYPICAL_LEVEL_DB + levelDb
        return band.outputDb(input) - input
    }

    fun bandAt(compressor: Compressor, hz: Double): CompressorBand = when {
        hz <= compressor.lowCrossoverHz -> compressor.low
        hz <= compressor.highCrossoverHz -> compressor.mid
        else -> compressor.high
    }

    class Step(val compressor: Compressor, val done: Boolean)

    /**
     * The next settings to send when moving to [target], so a change is a quick fade rather than a jump
     * in level. Works from what was last sent, so a new target simply continues from where the sound is.
     * Timing settings change at once, because they make no sound by themselves.
     */
    fun next(sent: Compressor?, target: Compressor, instant: Boolean = false): Step {
        if (instant || sent == null || sent == target) return Step(target, done = true)

        // A band that's off behaves like one that changes nothing, which gives it something to fade from.
        fun active(compressor: Compressor, band: CompressorBand) = compressor.enabled && band.enabled
        val pairs = sent.bands.zip(target.bands)
        fun ratioOf(compressor: Compressor, band: CompressorBand) = if (active(compressor, band)) band.ratio else 1.0
        fun makeupOf(compressor: Compressor, band: CompressorBand) = if (active(compressor, band)) band.makeupDb else 0.0

        var steps = 1.0
        fun need(value: Double) {
            steps = maxOf(steps, kotlin.math.ceil(value))
        }
        fun logSteps(from: Double, to: Double, limit: Double) = kotlin.math.abs(kotlin.math.ln(to / from)) / kotlin.math.ln(limit)
        for ((a, b) in pairs) {
            need(kotlin.math.abs(makeupOf(target, b) - makeupOf(sent, a)) / MAX_MAKEUP_STEP_DB)
            need(logSteps(ratioOf(sent, a), ratioOf(target, b), MAX_RATIO_STEP))
            if (active(sent, a) && active(target, b)) need(kotlin.math.abs(b.thresholdDb - a.thresholdDb) / MAX_THRESHOLD_STEP_DB)
        }
        need(logSteps(sent.lowCrossoverHz, target.lowCrossoverHz, MAX_CROSSOVER_STEP))
        need(logSteps(sent.highCrossoverHz, target.highCrossoverHz, MAX_CROSSOVER_STEP))
        if (steps <= 1.0) return Step(target, done = true)

        fun move(from: Double, to: Double) = from + (to - from) / steps
        fun moveLog(from: Double, to: Double) = from * Math.pow(to / from, 1 / steps)
        val moved = pairs.map { (a, b) ->
            val on = active(target, b)
            val shape = if (on) b else a
            shape.copy(
                enabled = true,
                ratio = moveLog(ratioOf(sent, a), ratioOf(target, b)),
                makeupDb = move(makeupOf(sent, a), makeupOf(target, b)),
                thresholdDb = if (on && active(sent, a)) move(a.thresholdDb, b.thresholdDb) else shape.thresholdDb,
            )
        }
        return Step(
            Compressor(
                enabled = true,
                lowCrossoverHz = moveLog(sent.lowCrossoverHz, target.lowCrossoverHz),
                highCrossoverHz = moveLog(sent.highCrossoverHz, target.highCrossoverHz),
                low = moved[0],
                mid = moved[1],
                high = moved[2],
            ),
            done = false,
        )
    }

    /**
     * What the phone's single compressor should do. Night mode wins, because it protects against
     * sudden loud moments, then the manual compressor, then the leveller.
     */
    fun resolve(night: Double, leveller: Double, compressor: Compressor): Compressor = when {
        night > ACTIVE -> amountBased(night, threshold = -30.0, ratio = 3.0, makeup = 9.0, attack = 8.0, release = 120.0)
        compressor.enabled -> compressor.clamped()
        leveller > ACTIVE -> amountBased(leveller, threshold = -24.0, ratio = 2.0, makeup = 6.0, attack = 50.0, release = 1000.0)
        else -> Compressor()
    }

    /** Night mode reacts quickly to tame sudden moments. The leveller is slow, so it evens out whole passages. */
    private fun amountBased(amount: Double, threshold: Double, ratio: Double, makeup: Double, attack: Double, release: Double): Compressor {
        val band = CompressorBand(
            thresholdDb = threshold * amount,
            ratio = 1.0 + ratio * amount,
            attackMs = attack,
            releaseMs = release,
            makeupDb = makeup * amount,
        )
        return Compressor(enabled = true, low = band, mid = band, high = band)
    }

    /** Designed starting points to adjust by ear. They aren't measurements or anyone's known settings. */
    val styles = listOf(
        CompressorStyle(
            "Gentle",
            CompressorBand(thresholdDb = -24.0, ratio = 2.0, attackMs = 20.0, releaseMs = 200.0, makeupDb = 2.0).let {
                Compressor(enabled = true, low = it, mid = it, high = it)
            },
        ),
        CompressorStyle(
            "Punchy",
            Compressor(
                enabled = true,
                low = CompressorBand(thresholdDb = -20.0, ratio = 3.0, attackMs = 30.0, releaseMs = 120.0, makeupDb = 3.0),
                mid = CompressorBand(thresholdDb = -22.0, ratio = 2.5, attackMs = 30.0, releaseMs = 150.0, makeupDb = 2.0),
                high = CompressorBand(thresholdDb = -24.0, ratio = 2.0, attackMs = 15.0, releaseMs = 150.0, makeupDb = 1.5),
            ),
        ),
        CompressorStyle(
            "Clear speech",
            Compressor(
                enabled = true,
                low = CompressorBand(thresholdDb = -30.0, ratio = 4.0, attackMs = 10.0, releaseMs = 150.0),
                mid = CompressorBand(thresholdDb = -28.0, ratio = 3.0, attackMs = 5.0, releaseMs = 120.0, makeupDb = 5.0),
                high = CompressorBand(thresholdDb = -30.0, ratio = 2.0, attackMs = 5.0, releaseMs = 120.0, makeupDb = 3.0),
            ),
        ),
    )
}
