package com.phonons.app.data

import kotlin.math.abs
import kotlin.math.ceil
import kotlin.math.max

/**
 * Moves the sound towards a new setting in small steps, so a preset change is a quick morph rather than
 * a jump. All bands and the input level arrive together, so if the old and new settings are each safe
 * from clipping, so is every step between them. Pure, so it can be unit tested.
 */
object GainRamp {
    /** Changes up to this size go straight through, so dragging a fader doesn't lag behind the finger. */
    const val MAX_STEP_DB = 4f

    class Step(val left: FloatArray, val right: FloatArray, val inputDb: Float, val done: Boolean)

    /**
     * The next values to send. Works from what was last sent, so a new target arriving halfway through
     * simply continues from where the sound is. Anything not sent yet is NaN, which goes straight to its
     * target, as there's no earlier sound to move from.
     */
    fun next(
        sentLeft: FloatArray,
        sentRight: FloatArray,
        sentInputDb: Float,
        left: FloatArray,
        right: FloatArray,
        inputDb: Float,
        instant: Boolean = false,
    ): Step {
        val unsent = sentInputDb.isNaN() || sentLeft.any { it.isNaN() } || sentRight.any { it.isNaN() }
        if (instant || unsent) return Step(left, right, inputDb, done = true)

        var largest = abs(inputDb - sentInputDb)
        for (i in left.indices) {
            largest = max(largest, abs(left[i] - sentLeft[i]))
            largest = max(largest, abs(right[i] - sentRight[i]))
        }
        val steps = ceil(largest / MAX_STEP_DB).toInt()
        if (steps <= 1) return Step(left, right, inputDb, done = true)

        fun move(from: Float, to: Float) = from + (to - from) / steps
        return Step(
            left = FloatArray(left.size) { move(sentLeft[it], left[it]) },
            right = FloatArray(right.size) { move(sentRight[it], right[it]) },
            inputDb = move(sentInputDb, inputDb),
            done = false,
        )
    }
}
