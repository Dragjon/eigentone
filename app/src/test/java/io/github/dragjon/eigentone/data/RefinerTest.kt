package io.github.dragjon.eigentone.data

import io.github.dragjon.eigentone.dsp.GraphicEq
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.math.exp
import kotlin.math.sqrt
import kotlin.random.Random

class RefinerTest {
    private fun rms(v: List<Double>) = sqrt(v.sumOf { it * it } / v.size)

    /** A listener who wants [taste] and answers with the same kind of noise people do, sometimes worse. */
    private fun run(taste: List<Double>, seed: Int, noise: Double = 1.0): Refiner {
        val refiner = Refiner(seed.toLong())
        val random = Random(seed)
        while (!refiner.isDone) {
            val pair = refiner.current!!
            fun score(x: List<Double>) = -0.08 * x.indices.sumOf { (x[it] - taste[it]) * (x[it] - taste[it]) }
            val z = (score(pair.a) - score(pair.b)) / noise
            val p = Refiner.probabilities(z)
            val r = random.nextDouble()
            refiner.choose(if (r < p[0]) Choice.A else if (r < p[0] + p[1]) Choice.B else Choice.NO_PREFERENCE)
        }
        return refiner
    }

    private val bassAndDarker = List(10) { i -> 4 * exp(-(i * i) / 4.0) - 3 * exp(-((i - 9) * (i - 9)) / 4.0) }

    @Test
    fun findsMostOfAListenersTaste() {
        // Over several listeners, the result ends up much closer to what they want than leaving it alone.
        val errors = (1..8).map { seed ->
            val result = run(bassAndDarker, seed).result()
            rms(result.indices.map { result[it] - bassAndDarker[it] })
        }
        val untouched = rms(bassAndDarker)
        assertTrue("average error ${errors.average()} vs $untouched", errors.average() < 0.5 * untouched)
    }

    @Test
    fun aListenerWhoHearsNoDifferenceGetsNoChange() {
        val refiner = Refiner(3)
        while (!refiner.isDone) refiner.choose(Choice.NO_PREFERENCE)
        assertEquals(Refiner.MIN_ROUNDS, refiner.answered)
        assertTrue(refiner.result().all { kotlin.math.abs(it) <= 0.5 })
    }

    @Test
    fun aListenerWithNoTasteGetsLittleChange() {
        val results = (1..6).map { rms(run(List(10) { 0.0 }, it).result()) }
        assertTrue("${results.average()}", results.average() < 1.0)
    }

    @Test
    fun takesAReasonableNumberOfAnswersAndChecksConsistency() {
        val refiner = run(bassAndDarker, 11)
        assertTrue(refiner.answered in Refiner.MIN_ROUNDS..Refiner.MAX_ROUNDS)
        assertTrue(refiner.consistency.second >= 1)
    }

    @Test
    fun pairsAreClearlyDifferent() {
        val refiner = Refiner(5)
        repeat(10) {
            val pair = refiner.current!!
            val gap = pair.a.indices.maxOf { kotlin.math.abs(pair.a[it] - pair.b[it]) }
            assertTrue("gap $gap", gap >= 2.9)
            refiner.choose(if (it % 2 == 0) Choice.A else Choice.B)
        }
    }

    @Test
    fun appliesToTheFadersAtAnyLayout() {
        val change = List(10) { if (it == 0) 3.0 else 0.0 }
        val ten = Refiner.apply(Tone(), change)
        assertEquals(3.0, ten.graphicDb.first(), 0.0)
        val tone31 = Tone(graphicDb = List(31) { 1.0 }, graphicBands = 31)
        val applied = Refiner.apply(tone31, change)
        assertEquals(4.0, applied.graphicDb.first(), 0.0)
        assertEquals(1.0, applied.graphicDb.last(), 0.0)
        assertEquals(31, applied.graphicDb.size)
        assertEquals(GraphicEq.snap(applied.graphicDb[5]), applied.graphicDb[5], 0.0)
    }
}
