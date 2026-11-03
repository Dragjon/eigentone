package com.phonons.app.data

import kotlin.math.abs
import kotlinx.serialization.Serializable

enum class Ear(val label: String) { LEFT("left"), RIGHT("right") }

/**
 * A quick listening check that finds how quiet a tone can be before each ear stops hearing it, using
 * the common "down 10, up 5" method. Levels are relative to full scale and the phone's volume, so they
 * only mean something compared with each other. It isn't a hearing test in the medical sense.
 */
class HearingTest(
    private val frequencies: List<Double> = DEFAULT_FREQUENCIES,
) {
    private data class Step(val ear: Ear, val hz: Double)

    private val steps = Ear.entries.flatMap { ear -> frequencies.map { Step(ear, it) } }
    private var stepIndex = 0
    private var level = START_DB
    private var quietestHeard: Double? = null
    private var misses = 0
    private var answersThisStep = 0
    private val thresholds = mutableMapOf<Pair<Ear, Double>, Double>()

    val isDone: Boolean get() = stepIndex >= steps.size
    val ear: Ear? get() = steps.getOrNull(stepIndex)?.ear
    val hz: Double? get() = steps.getOrNull(stepIndex)?.hz

    /** The level to play, which can never go above [CEILING_DB]. */
    val levelDb: Double get() = level
    val progress: Float get() = stepIndex.toFloat() / steps.size

    /** Which ear and frequency this is, counting from 1, out of [stepCount]. */
    val stepNumber: Int get() = (stepIndex + 1).coerceAtMost(steps.size)
    val stepCount: Int get() = steps.size

    /** Which tone this is for the current ear and frequency, counting from 1. */
    val attempt: Int get() = answersThisStep + 1

    fun answer(heard: Boolean) {
        val step = steps.getOrNull(stepIndex) ?: return
        answersThisStep++
        val atFloor = heard && level <= FLOOR_DB
        if (heard) {
            quietestHeard = minOf(quietestHeard ?: level, level)
            misses = 0
            level = (level - STEP_DOWN_DB).coerceAtLeast(FLOOR_DB)
        } else {
            misses++
            level = (level + STEP_UP_DB).coerceAtMost(CEILING_DB)
        }
        val heardThenLost = quietestHeard != null && misses >= MISSES_TO_FINISH
        val outOfRange = !heard && quietestHeard == null && misses >= MISSES_TO_GIVE_UP
        if (heardThenLost || atFloor || outOfRange || answersThisStep >= MAX_ANSWERS) {
            thresholds[step.ear to step.hz] = quietestHeard ?: CEILING_DB
            stepIndex++
            level = START_DB
            quietestHeard = null
            misses = 0
            answersThisStep = 0
        }
    }

    fun result(): HearingResult = HearingResult(
        Ear.entries.associateWith { ear -> frequencies.associateWith { hz -> thresholds[ear to hz] ?: CEILING_DB } },
    )

    companion object {
        val DEFAULT_FREQUENCIES = listOf(250.0, 500.0, 1000.0, 2000.0, 4000.0, 8000.0)
        const val START_DB = -40.0

        /** Well below full scale, so a tone can't be uncomfortably loud at a normal music volume. */
        const val CEILING_DB = -15.0
        const val FLOOR_DB = -90.0
        private const val STEP_DOWN_DB = 10.0
        private const val STEP_UP_DB = 5.0
        private const val MISSES_TO_FINISH = 2
        private const val MISSES_TO_GIVE_UP = 6
        private const val MAX_ANSWERS = 14
    }
}

/**
 * A finished check, kept because the curve saved from one only holds the average of both ears. Each
 * point's level is the quietest tone heard at that frequency, not a change to the sound.
 */
@Serializable
data class HearingRecord(
    val left: List<ShapePoint> = emptyList(),
    val right: List<ShapePoint> = emptyList(),
    val headphones: String = "",
    val measuredAt: Long = 0,
) {
    /** Null if the two ears don't cover the same frequencies, which could only come from an edited backup. */
    fun result(): HearingResult? {
        if (left.isEmpty() || left.map { it.hz }.toSet() != right.map { it.hz }.toSet()) return null
        return HearingResult(
            mapOf(
                Ear.LEFT to left.associate { it.hz to it.db },
                Ear.RIGHT to right.associate { it.hz to it.db },
            ),
        )
    }

    companion object {
        fun of(result: HearingResult, headphones: String, measuredAt: Long): HearingRecord {
            fun points(ear: Ear) = result.thresholds[ear].orEmpty().map { (hz, level) -> ShapePoint(hz, level) }
            return HearingRecord(points(Ear.LEFT), points(Ear.RIGHT), headphones, measuredAt)
        }
    }
}

data class HearingResult(val thresholds: Map<Ear, Map<Double, Double>>) {

    /**
     * A gentle curve that lifts the frequencies heard least well, relative to the listener's own
     * typical level. It only corrects half the difference, because full correction sounds unnatural.
     * The Bluetooth earbuds' own tuning is part of the measurement, so this suits those earbuds.
     */
    fun shapePoints(): List<ShapePoint> {
        val frequencies = thresholds.values.first().keys.sorted()
        val averages = frequencies.map { hz -> thresholds.values.map { it.getValue(hz) }.average() }
        val typical = averages.sorted().let { (it[(it.size - 1) / 2] + it[it.size / 2]) / 2 }
        return frequencies.zip(averages).map { (hz, level) ->
            ShapePoint(hz, (((level - typical) * CORRECTION).coerceIn(-MAX_LIFT_DB, MAX_LIFT_DB) * 2).let { kotlin.math.round(it) / 2 })
        }
    }

    /** The largest gap between the two ears at any one frequency. */
    fun largestEarDifferenceDb(): Double {
        val left = thresholds[Ear.LEFT] ?: return 0.0
        val right = thresholds[Ear.RIGHT] ?: return 0.0
        return left.keys.maxOfOrNull { hz -> abs(left.getValue(hz) - (right[hz] ?: left.getValue(hz))) } ?: 0.0
    }

    /**
     * Moves each ear towards the middle of the two, frequency by frequency, closing half of the gap.
     * Null when the ears are too close for it to matter: one 5 dB step could just be a missed beep.
     */
    fun earCorrection(): EarCorrection? {
        if (largestEarDifferenceDb() < MIN_EAR_DIFFERENCE_DB) return null
        val left = thresholds.getValue(Ear.LEFT)
        val right = thresholds.getValue(Ear.RIGHT)
        fun offsets(mine: Map<Double, Double>, other: Map<Double, Double>) = mine.keys.sorted().map { hz ->
            val offset = (mine.getValue(hz) - other.getValue(hz)) / 2 * CORRECTION
            ShapePoint(hz, kotlin.math.round(offset.coerceIn(-MAX_LIFT_DB, MAX_LIFT_DB) * 2) / 2)
        }
        return EarCorrection(left = offsets(left, right), right = offsets(right, left))
    }

    companion object {
        private const val CORRECTION = 0.5
        private const val MAX_LIFT_DB = 6.0
        private const val MIN_EAR_DIFFERENCE_DB = 10.0
    }
}
