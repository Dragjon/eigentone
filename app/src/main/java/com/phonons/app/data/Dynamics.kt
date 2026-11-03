package com.phonons.app.data

import kotlin.math.abs
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

    private val OFF = CompressorBand(enabled = false)

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
            "Tame bass",
            Compressor(
                enabled = true,
                low = CompressorBand(thresholdDb = -28.0, ratio = 4.0, attackMs = 10.0, releaseMs = 150.0, makeupDb = 2.0),
                mid = OFF,
                high = OFF,
            ),
        ),
        CompressorStyle(
            "Tame harshness",
            Compressor(
                enabled = true,
                highCrossoverHz = 4000.0,
                low = OFF,
                mid = OFF,
                high = CompressorBand(thresholdDb = -28.0, ratio = 3.0, attackMs = 3.0, releaseMs = 80.0),
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
