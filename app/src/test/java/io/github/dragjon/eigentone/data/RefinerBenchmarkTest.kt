package io.github.dragjon.eigentone.data

import org.junit.Assume.assumeTrue
import org.junit.Test
import kotlin.math.cos
import kotlin.math.exp
import kotlin.math.ln
import kotlin.math.sqrt
import kotlin.random.Random

/**
 * Compares ways of running Refine on simulated listeners who differ in taste, in how sharply they hear,
 * and in how readily they say "can't tell". Slow, so it only runs when REFINE_BENCH is set.
 */
class RefinerBenchmarkTest {
    private class Listener(val taste: List<Double>, val sharpness: Double, val tie: Double)

    private fun gaussian(r: Random) = sqrt(-2 * ln(r.nextDouble().coerceAtLeast(1e-12))) * cos(2 * Math.PI * r.nextDouble())

    /** A smooth taste like the ones people have: broad tilts and bumps, drawn at random. */
    private fun taste(r: Random): List<Double> {
        val n = 10
        val cov = Array(n) { i -> DoubleArray(n) { j -> 9.0 * exp(-((i - j) * (i - j)) / 2.0) + if (i == j) 0.05 else 0.0 } }
        val l = Array(n) { DoubleArray(n) }
        for (i in 0 until n) for (j in 0..i) {
            val s = cov[i][j] - (0 until j).sumOf { l[i][it] * l[j][it] }
            l[i][j] = if (i == j) sqrt(s) else s / l[j][j]
        }
        val z = DoubleArray(n) { gaussian(r) }
        return List(n) { i -> (0..i).sumOf { k -> l[i][k] * z[k] }.coerceIn(-6.0, 6.0) }
    }

    private fun run(listener: Listener, seed: Int, options: Refiner.Options): Triple<Double, Int, Double> {
        val refiner = Refiner(seed.toLong(), options)
        val random = Random(seed * 7919 + 13)
        var early = Double.NaN
        fun error(result: List<Double>) = sqrt(result.indices.sumOf { (result[it] - listener.taste[it]) * (result[it] - listener.taste[it]) } / result.size)
        while (!refiner.isDone) {
            if (refiner.answered == 12) early = error(refiner.result())
            val pair = refiner.current!!
            fun score(x: List<Double>) = -0.08 * listener.sharpness * x.indices.sumOf { (x[it] - listener.taste[it]) * (x[it] - listener.taste[it]) }
            val p = Refiner.probabilities(score(pair.a) - score(pair.b), listener.tie)
            val r = random.nextDouble()
            refiner.choose(if (r < p[0]) Choice.A else if (r < p[0] + p[1]) Choice.B else Choice.NO_PREFERENCE)
        }
        val rms = error(refiner.result())
        return Triple(rms, refiner.answered, if (early.isNaN()) rms else early)
    }

    @Test
    fun compare() {
        assumeTrue(System.getenv("REFINE_BENCH") != null)
        val r = Random(System.getenv("REFINE_BENCH")!!.toIntOrNull() ?: 2026)
        val listeners = buildList {
            repeat(20) {
                val t = taste(r)
                for (s in listOf(0.5, 1.0, 2.0)) for (tie in listOf(0.3, 0.6, 1.0)) add(Listener(t, s, tie))
            }
        }
        val variants = listOf(
            "Current (informative, fixed listener)" to Refiner.Options(Refiner.PairChoice.MOST_INFORMATIVE, learnListener = false),
            "Fixed listener, steps 1.5-3.5" to Refiner.Options(Refiner.PairChoice.MOST_INFORMATIVE, learnListener = false, steps = listOf(1.5, 2.5, 3.5)),
            "Learns listener, steps 1.5-3.5" to Refiner.Options(Refiner.PairChoice.MOST_INFORMATIVE, learnListener = true, steps = listOf(1.5, 2.5, 3.5)),
            "Learns listener, steps 1-3.5" to Refiner.Options(Refiner.PairChoice.MOST_INFORMATIVE, learnListener = true, steps = listOf(1.0, 1.5, 2.5, 3.5)),
        )
        val untouched = listeners.map { l -> sqrt(l.taste.sumOf { it * it } / l.taste.size) }.average()
        println("Untouched error: %.2f dB over %d listeners".format(untouched, listeners.size))
        for ((name, options) in variants) {
            val results = listeners.mapIndexed { i, l -> run(l, i + 1, options) }
            fun bySharpness(s: Double) = listeners.indices.filter { listeners[it].sharpness == s }.map { results[it].first }.average()
            println(
                "%-42s error %.2f dB (%.2f after 12), %.1f rounds | dull %.2f, typical %.2f, keen %.2f".format(
                    name,
                    results.map { it.first }.average(),
                    results.map { it.third }.average(),
                    results.map { it.second.toDouble() }.average(),
                    bySharpness(0.5), bySharpness(1.0), bySharpness(2.0),
                ),
            )
        }
    }
}
