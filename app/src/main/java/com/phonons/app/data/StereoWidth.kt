package com.phonons.app.data

import kotlin.math.PI
import kotlin.math.log2
import kotlin.math.pow
import kotlin.math.sin

/**
 * Widens stereo using only a separate EQ for each channel, so it works wherever the EQ does and doesn't
 * rely on Android's deprecated Virtualizer. Neighbouring frequency ranges lean slightly left and right
 * in turn, which spreads sounds that would otherwise sit in the centre. Each channel's rise is matched
 * by the other's fall, so the overall tone barely changes, and bass stays centred, where it belongs.
 *
 * It can't move sound between channels, which Android doesn't allow here, so this is a gentle widening
 * rather than a surround effect. Pure, so it can be unit tested.
 */
object StereoWidth {
    const val MAX_LEAN_DB = 6.0

    /** How wide each left and right pair of ranges is. Narrow enough to spread a voice, wide enough to not sound hollow. */
    private const val CYCLE_OCTAVES = 2.0 / 3.0

    private const val FADE_IN_START_HZ = 250.0
    private const val FADE_IN_END_HZ = 500.0
    private const val FADE_OUT_START_HZ = 10000.0
    private const val FADE_OUT_END_HZ = 16000.0
    private const val POINTS_PER_OCTAVE = 12

    private val frequencies: List<Double> = run {
        val octaves = log2(FADE_OUT_END_HZ / FADE_IN_START_HZ)
        val count = (octaves * POINTS_PER_OCTAVE).toInt()
        List(count + 1) { i -> FADE_IN_START_HZ * 2.0.pow(octaves * i / count) }
    }

    private data class Cached(val amount: Double, val left: List<ShapePoint>, val right: List<ShapePoint>)

    @Volatile private var cached: Cached? = null

    /** Moves between 0 and 1 across a range, on a log frequency axis. */
    private fun fade(hz: Double, from: Double, to: Double): Double =
        (log2(hz / from) / log2(to / from)).coerceIn(0.0, 1.0)

    /** How far towards the left channel this frequency leans at full strength, in dB. Negative leans right. */
    fun leanDb(hz: Double): Double {
        val edges = fade(hz, FADE_IN_START_HZ, FADE_IN_END_HZ) * (1 - fade(hz, FADE_OUT_START_HZ, FADE_OUT_END_HZ))
        return MAX_LEAN_DB * edges * sin(2 * PI * log2(hz / FADE_IN_START_HZ) / CYCLE_OCTAVES)
    }

    /** Each channel's curve for an amount from 0 to 1. Remembered, as the engine asks for this very often. */
    fun curves(amount: Double): Pair<List<ShapePoint>, List<ShapePoint>> {
        cached?.takeIf { it.amount == amount }?.let { return it.left to it.right }
        val left = frequencies.map { ShapePoint(it, leanDb(it) * amount) }
        val right = left.map { it.copy(db = -it.db) }
        cached = Cached(amount, left, right)
        return left to right
    }
}
