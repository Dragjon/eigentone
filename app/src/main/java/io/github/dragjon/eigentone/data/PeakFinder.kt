package io.github.dragjon.eigentone.data

import kotlin.math.abs
import kotlin.math.log2
import kotlin.math.pow
import kotlin.random.Random

enum class SpotAnswer { STANDS_OUT, SAME, WEAKER }

enum class SidePick { FIRST, SECOND, SAME }

/**
 * Finds peaks and dips by asking simple questions, so nobody has to hunt for a frequency. It compares
 * spots with their neighbours in a random order, then asks again about anything flagged, because a single
 * answer can be chance. Anything that holds up is then narrowed down. It finds what the listener hears,
 * and isn't a measurement. Pure, so it can be tested with a simulated listener.
 */
class PeakFinder(random: Random = Random.Default) {
    sealed interface Question {
        val hz: Double
        val tones: List<Double>

        /** Does this spot stand out from the notes beside it? */
        data class Spot(override val hz: Double, val rechecking: Boolean) : Question {
            override val tones get() = Sweep.nearbyTones(hz)
        }

        /** Which side of this spot is the stronger or weaker point? */
        data class Narrow(override val hz: Double, val answer: SpotAnswer) : Question {
            override val tones get() = listOf(hz / NARROW_STEP, hz * NARROW_STEP)
        }
    }

    private val random = random
    private val survey = ArrayDeque(spots().shuffled(random))
    private val flagged = mutableListOf<Pair<Double, SpotAnswer>>()
    private val recheck = ArrayDeque<Pair<Double, SpotAnswer>>()
    private val confirmed = mutableListOf<Pair<Double, SpotAnswer>>()
    private val narrow = ArrayDeque<Pair<Double, SpotAnswer>>()
    private val found = mutableListOf<Pair<Double, SpotAnswer>>()

    /** How many questions have been answered. */
    var answered = 0
        private set

    /** The questions asked and known about so far. This grows when something gets flagged. */
    val total: Int get() = answered + survey.size + recheck.size + narrow.size + flagged.size * 2 + confirmed.size

    var question: Question? = nextQuestion()
        private set

    val isDone: Boolean get() = question == null

    /** Answers a [Question.Spot]. */
    fun answer(answer: SpotAnswer) {
        val asked = question as? Question.Spot ?: return
        answered++
        if (asked.rechecking) {
            val first = recheck.removeFirst()
            if (first.second == answer) confirmed += first
        } else {
            survey.removeFirst()
            if (answer != SpotAnswer.SAME) flagged += asked.hz to answer
        }
        question = nextQuestion()
    }

    /** Answers a [Question.Narrow]. */
    fun pick(side: SidePick) {
        val asked = question as? Question.Narrow ?: return
        answered++
        narrow.removeFirst()
        val hz = when (side) {
            SidePick.FIRST -> asked.hz / NARROW_STEP
            SidePick.SECOND -> asked.hz * NARROW_STEP
            SidePick.SAME -> asked.hz
        }
        found += hz to asked.answer
        question = nextQuestion()
    }

    private fun nextQuestion(): Question? {
        survey.firstOrNull()?.let { return Question.Spot(it, rechecking = false) }
        if (flagged.isNotEmpty()) {
            recheck.addAll(flagged.shuffled(random))
            flagged.clear()
        }
        recheck.firstOrNull()?.let { return Question.Spot(it.first, rechecking = true) }
        if (confirmed.isNotEmpty()) {
            narrow.addAll(confirmed)
            confirmed.clear()
        }
        narrow.firstOrNull()?.let { return Question.Narrow(it.first, it.second) }
        return null
    }

    /** What was found, as gentle marks. Two answers at practically the same spot count once. */
    fun marks(): List<SweepMark> {
        val kept = mutableListOf<SweepMark>()
        for ((hz, answer) in found.sortedBy { it.first }) {
            if (kept.any { abs(log2(it.hz / hz)) < SAME_SPOT_OCTAVES }) continue
            kept += SweepMark(hz, if (answer == SpotAnswer.STANDS_OUT) Sweep.TOO_LOUD_DB else Sweep.TOO_QUIET_DB)
        }
        return kept
    }

    companion object {
        /**
         * Below this, fit and hearing vary too much for a comparison to mean anything. Above it, tones
         * get hard to judge and shift as headphones move on the head.
         */
        const val LOWEST_HZ = 200.0
        const val HIGHEST_HZ = 12000.0
        private const val SPOT_SPACING_OCTAVES = 0.25
        private const val SAME_SPOT_OCTAVES = 1.0 / 6.0
        private val NARROW_STEP = 2.0.pow(1.0 / 8.0)

        fun spots(): List<Double> {
            val count = (log2(HIGHEST_HZ / LOWEST_HZ) / SPOT_SPACING_OCTAVES).toInt()
            return List(count + 1) { LOWEST_HZ * 2.0.pow(it * SPOT_SPACING_OCTAVES) }
        }
    }
}
