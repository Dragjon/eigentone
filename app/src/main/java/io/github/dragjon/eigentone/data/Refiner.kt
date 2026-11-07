package io.github.dragjon.eigentone.data

import io.github.dragjon.eigentone.dsp.GraphicEq
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.exp
import kotlin.math.ln
import kotlin.math.max
import kotlin.math.sqrt
import kotlin.random.Random

/** Two versions to compare, as changes in dB at [Refiner.FREQS] on top of the sound being refined. */
data class RefinePair(val a: List<Double>, val b: List<Double>)

/**
 * Fine-tunes a sound by asking which of two versions is better, many times over.
 *
 * Single answers are unreliable: people often pick differently when the same pair comes round again. So
 * nothing here follows one answer. Every answer goes into one model of what the listener prefers, and
 * each next pair is chosen from that model. This is Bayesian preference learning with pairwise
 * comparisons, as used for fitting hearing aids, with a small parametric model, which needs fewer
 * answers than the heavier alternatives.
 *
 * The model: the listener has a preferred change [m] at each of ten octave points, and likes a version
 * less the further it is from that, as a squared distance. Answers are noisy, with "can't tell" when two
 * versions are close (an ordered logistic choice). Before any answers, the preferred change is no change,
 * and neighbouring octaves are expected to want similar changes, so an answer about one region also
 * says a little about the ones beside it.
 *
 * The next pair is the one whose answer would teach the model the most (BALD). Pairs are built around the
 * current best guess, so that already spends the questions near the preferred sound.
 *
 * Two alternatives are kept behind [Options] for comparison, and are off because simulated listeners
 * showed no gain from them: choosing the pair whose better version is expected to be best (EUBO), and
 * weighing several guesses at how sharply the listener hears and how readily they say "can't tell".
 * RefinerBenchmarkTest runs the comparison.
 */
class Refiner(seed: Long = 1L, private val options: Options = Options()) {
    /** Which ways of choosing pairs and modelling the listener are on. The defaults are what the app uses, and won on simulated listeners. */
    data class Options(
        val choice: PairChoice = PairChoice.MOST_INFORMATIVE,
        val learnListener: Boolean = false,
        /**
         * Half the difference between the two versions at the centre of a change, in dB, so pairs differ by
         * 3, 5 or 7 dB there. The largest makes broad changes easy to hear, which on simulated listeners
         * cut the error left after refining by about an eighth.
         */
        val steps: List<Double> = listOf(1.5, 2.5, 3.5),
    )

    enum class PairChoice {
        /** The pair whose answer would teach the most about the whole preference (BALD). */
        MOST_INFORMATIVE,

        /** The pair whose better version is expected to be the best (EUBO). */
        BEST_OPTION,
    }

    /** One guess at how the listener hears, with what's been worked out about their preference under it. */
    private class Listener(val sharpness: Double, val tie: Double, val logPrior: Double) {
        var mean = DoubleArray(N)
        var covariance = prior()
        var evidence = logPrior
        var weight = 0.0
    }

    private val random = Random(seed)
    private val observations = mutableListOf<Triple<RefinePair, Choice, Int>>()
    private val asked = mutableListOf<RefinePair>()
    private val listeners: List<Listener> =
        if (options.learnListener) {
            SHARPNESS.flatMap { (s, ps) -> TIES.map { (t, pt) -> Listener(CURVATURE * s, t, ps + pt) } }
        } else {
            listOf(Listener(CURVATURE, TIE, 0.0))
        }
    private var lastInfo = Double.MAX_VALUE
    private val repeats = mutableListOf<Boolean>()

    /** For a repeated pair, which earlier answer it repeats and whether its sides were swapped. */
    private var repeating: Pair<Int, Boolean>? = null

    init {
        weigh()
    }

    /** The pair being asked about, or null once it's finished. */
    var current: RefinePair? = next()
        private set

    val answered: Int get() = observations.size
    val isDone: Boolean get() = current == null

    /** Repeated pairs answered the same way, and how many were repeated, as a check on how steady the answers were. */
    val consistency: Pair<Int, Int> get() = repeats.count { it } to repeats.size

    fun choose(choice: Choice) {
        val pair = current ?: return
        repeating?.let { (index, swapped) ->
            val earlier = observations[index].second
            val same = when {
                earlier == Choice.NO_PREFERENCE || choice == Choice.NO_PREFERENCE -> earlier == choice
                swapped -> earlier != choice
                else -> earlier == choice
            }
            repeats += same
        }
        repeating = null
        observations += Triple(pair, choice, answered)
        listeners.forEach(::fit)
        weigh()
        current = next()
    }

    /** The preferred change at [FREQS], in half-dB steps. No answers, or only "can't tell", leave it flat. */
    fun result(): List<Double> = blendedMean().map { GraphicEq.snap(it.coerceIn(-MAX_DB, MAX_DB)) }

    /** The preference averaged over the guesses about the listener, each by how well it explains the answers. */
    private fun blendedMean(): DoubleArray {
        val m = DoubleArray(N)
        for (l in listeners) for (i in 0 until N) m[i] += l.weight * l.mean[i]
        return m
    }

    private fun next(): RefinePair? {
        if (answered >= MAX_ROUNDS) return null
        if (answered >= MIN_ROUNDS && lastInfo < DONE_INFO) return null
        // Mostly "can't tell" answers: more of the same won't find anything, so stop early.
        if (answered >= MIN_ROUNDS && observations.count { it.second == Choice.NO_PREFERENCE } * 4 >= answered * 3) return null
        // Now and then an earlier pair comes back, sides swapped, to see whether the answer holds.
        if (answered in REPEAT_AT && answered >= REPEAT_GAP) {
            val index = answered - REPEAT_GAP
            val earlier = observations[index].first
            repeating = index to true
            return RefinePair(earlier.b, earlier.a).also { asked += it }
        }
        val centre = result()
        val samples = samples()
        var best: RefinePair? = null
        var bestScore = Double.NEGATIVE_INFINITY
        var mostInfo = 0.0
        for (width in WIDTHS) for (at in 0 until N) for (step in options.steps) {
            val bump = List(N) { j -> exp(-((j - at) * (j - at)) / (2 * width * width)) }
            val a = List(N) { (centre[it] + step * bump[it]).coerceIn(-MAX_DB, MAX_DB) }
            val b = List(N) { (centre[it] - step * bump[it]).coerceIn(-MAX_DB, MAX_DB) }
            val pair = RefinePair(a, b)
            if (pair == asked.lastOrNull()) continue
            val info = information(pair, samples)
            mostInfo = max(mostInfo, info)
            val score = when (options.choice) {
                PairChoice.MOST_INFORMATIVE -> info
                PairChoice.BEST_OPTION -> bestOption(pair, samples)
            }
            if (score > bestScore) {
                bestScore = score
                best = pair
            }
        }
        // Stopping always asks the same question, whatever chooses the pairs: would any answer still teach much?
        lastInfo = mostInfo
        if (answered >= MIN_ROUNDS && mostInfo < DONE_INFO) return null
        // Which side holds the larger change is random, so "A" never comes to mean anything.
        val chosen = best ?: return null
        return (if (random.nextBoolean()) chosen else RefinePair(chosen.b, chosen.a)).also { asked += it }
    }

    /** A likely preference, with the guess about the listener it was drawn under. */
    private class Sample(val m: DoubleArray, val sharpness: Double, val tie: Double)

    /** How much an answer to [pair] is expected to teach the model: the mutual information of answer and preference. */
    private fun information(pair: RefinePair, samples: List<Sample>): Double {
        val average = DoubleArray(3)
        var spread = 0.0
        for (s in samples) {
            val (g, c) = features(pair, s.sharpness)
            val p = probabilities(dot(g, s.m) + c, s.tie)
            for (k in 0..2) average[k] += p[k] / samples.size
            spread += entropy(p) / samples.size
        }
        return entropy(average) - spread
    }

    /**
     * How good the better of the two versions is expected to be, in the model's own units, so the pairs
     * chosen move towards the preferred sound. Scored with one shared sharpness, so the guesses about the
     * listener don't change what "better" means.
     */
    private fun bestOption(pair: RefinePair, samples: List<Sample>): Double {
        var total = 0.0
        for (s in samples) {
            var ua = 0.0
            var ub = 0.0
            for (i in 0 until N) {
                ua -= (pair.a[i] - s.m[i]) * (pair.a[i] - s.m[i])
                ub -= (pair.b[i] - s.m[i]) * (pair.b[i] - s.m[i])
            }
            total += max(ua, ub)
        }
        return total / samples.size
    }

    /**
     * Draws likely preferences from what's known so far, from each guess about the listener in proportion
     * to how well it fits. The same draws each time, for a steady choice.
     */
    private fun samples(): List<Sample> {
        val draws = Random(SAMPLE_SEED)
        val out = ArrayList<Sample>(SAMPLES)
        // Shares of the draws by weight, largest remainders first, so they always add up to SAMPLES.
        val exact = listeners.map { it.weight * SAMPLES }
        val counts = exact.map { it.toInt() }.toMutableList()
        exact.indices.sortedByDescending { exact[it] - counts[it] }.take(SAMPLES - counts.sum()).forEach { counts[it]++ }
        listeners.forEachIndexed { index, l ->
            if (counts[index] == 0) return@forEachIndexed
            val chol = cholesky(l.covariance)
            repeat(counts[index]) {
                val z = DoubleArray(N) { gaussian(draws) }
                out += Sample(DoubleArray(N) { i -> l.mean[i] + (0..i).sumOf { k -> chol[i][k] * z[k] } }, l.sharpness, l.tie)
            }
        }
        return out
    }

    /**
     * How strongly A is favoured is linear in the preference, because each version is scored by its
     * squared distance from it: z = g·m + c.
     */
    private fun features(pair: RefinePair, sharpness: Double): Pair<DoubleArray, Double> {
        val g = DoubleArray(N) { 2 * sharpness * (pair.a[it] - pair.b[it]) }
        val c = -sharpness * (0 until N).sumOf { pair.a[it] * pair.a[it] - pair.b[it] * pair.b[it] }
        return g to c
    }

    /** Newton's method for the most likely preference under [l], its uncertainty, and how well [l] explains the answers. */
    private fun fit(l: Listener) {
        val priorInverse = PRIOR_INVERSE
        val m = l.mean.copyOf()
        var hessian = priorInverse
        for (iteration in 0 until NEWTON_STEPS) {
            val gradient = DoubleArray(N) { i -> -(0 until N).sumOf { j -> priorInverse[i][j] * m[j] } }
            val h = Array(N) { priorInverse[it].copyOf() }
            for ((pair, choice) in observations) {
                val (g, c) = features(pair, l.sharpness)
                val (d1, d2) = slopes(choice, dot(g, m) + c, l.tie)
                for (i in 0 until N) {
                    gradient[i] += d1 * g[i]
                    for (j in 0 until N) h[i][j] -= d2 * g[i] * g[j]
                }
            }
            hessian = h
            val step = solve(h, gradient)
            for (i in 0 until N) m[i] += step[i]
            if (step.maxOf { abs(it) } < 1e-7) break
        }
        l.mean = m
        l.covariance = invert(hessian)
        // The Laplace estimate of how likely the answers are under this guess about the listener.
        var logLikelihood = 0.0
        for ((pair, choice) in observations) {
            val (g, c) = features(pair, l.sharpness)
            val p = probabilities(dot(g, m) + c, l.tie)
            logLikelihood += ln(
                when (choice) {
                    Choice.A -> p[0]
                    Choice.B -> p[1]
                    Choice.NO_PREFERENCE -> p[2]
                }.coerceAtLeast(1e-300),
            )
        }
        val priorTerm = -0.5 * (0 until N).sumOf { i -> m[i] * (0 until N).sumOf { j -> priorInverse[i][j] * m[j] } }
        l.evidence = l.logPrior + logLikelihood + priorTerm - 0.5 * logDet(hessian)
    }

    /** Turns each guess's evidence into a weight, all adding up to one. */
    private fun weigh() {
        if (observations.isEmpty()) listeners.forEach { it.evidence = it.logPrior - 0.5 * logDet(PRIOR_INVERSE) }
        val top = listeners.maxOf { it.evidence }
        val raw = listeners.map { exp(it.evidence - top) }
        val sum = raw.sum()
        listeners.forEachIndexed { i, l -> l.weight = raw[i] / sum }
    }

    companion object {
        /** Where preferences are modelled: the ten octave faders, 31 Hz to 16 kHz. */
        val FREQS: List<Double> = GraphicEq.frequencies(10)
        private const val N = 10

        /** How far, in dB, a preference is expected to be from the sound as it is. */
        private const val PRIOR_DB = 3.0
        /** How alike neighbouring octaves' preferences are expected to be, in octaves. */
        private const val SMOOTH_OCTAVES = 1.0
        /** How much worse a version sounds per squared dB away from the preference, for a typical listener. */
        private const val CURVATURE = 0.08
        /** How close two versions have to be for "can't tell" to be the likely answer, for a typical listener. */
        private const val TIE = 0.6

        /** Guesses at how sharply a listener hears, as multiples of the typical one, with their log prior. */
        private val SHARPNESS = listOf(0.4, 0.65, 1.0, 1.6, 2.6).map { it to -0.5 * (ln(it) / 0.55) * (ln(it) / 0.55) }
        /** Guesses at how readily a listener says "can't tell", with their log prior. */
        private val TIES = listOf(0.3 to -0.4, 0.6 to 0.0, 1.1 to -0.4)

        private const val MAX_DB = 6.0
        /** How broad each change is, in octaves either side. Broad changes win early, since they're clearest. */
        private val WIDTHS = listOf(2.0, 1.2, 0.6)

        const val MIN_ROUNDS = 12
        const val MAX_ROUNDS = 24
        /** When no pair would teach more than this, more answers aren't worth the listener's time. */
        private const val DONE_INFO = 0.04
        private val REPEAT_AT = setOf(8, 17)
        private const val REPEAT_GAP = 6
        private const val SAMPLES = 48
        private const val SAMPLE_SEED = 7L
        private const val NEWTON_STEPS = 40

        private val PRIOR_INVERSE by lazy { invert(prior()) }

        /** Puts a change at [FREQS] onto the faders, keeping any finer detail they hold in step. */
        fun apply(tone: Tone, change: List<Double>): Tone {
            if (change.all { it == 0.0 }) return tone
            val bands = tone.graphicBands
            val faders = GraphicEq.normalized(tone.graphicDb, bands)
            val added = GraphicEq.interpolated(change, N, bands)
            return tone.copy(
                graphicDb = faders.indices.map { GraphicEq.snap(faders[it] + added[it]) },
                graphicDetail = tone.graphicDetail?.let { detail ->
                    val fine = GraphicEq.interpolated(change, N, detail.bands)
                    FaderDetail(detail.bands, detail.gains.indices.map { GraphicEq.snap(detail.gains[it] + fine[it]) })
                },
            )
        }

        /** Chances of A, B and "can't tell" when A is favoured by [z]. */
        internal fun probabilities(z: Double, tie: Double = TIE): DoubleArray {
            val a = sigmoid(z - tie)
            val b = sigmoid(-z - tie)
            return doubleArrayOf(a, b, (1 - a - b).coerceAtLeast(1e-12))
        }

        /** The first and second derivative of an answer's log-likelihood with respect to z. */
        private fun slopes(choice: Choice, z: Double, tie: Double): Pair<Double, Double> = when (choice) {
            Choice.A -> sigmoid(z - tie).let { p -> (1 - p) to -p * (1 - p) }
            Choice.B -> sigmoid(-z - tie).let { p -> -(1 - p) to -p * (1 - p) }
            Choice.NO_PREFERENCE -> {
                val p1 = sigmoid(z - tie)
                val p2 = sigmoid(-z - tie)
                val q = (1 - p1 - p2).coerceAtLeast(1e-12)
                val q1 = -p1 * (1 - p1) + p2 * (1 - p2)
                val q2 = -p1 * (1 - p1) * (1 - 2 * p1) - p2 * (1 - p2) * (1 - 2 * p2)
                (q1 / q) to ((q2 * q - q1 * q1) / (q * q)).coerceAtMost(0.0)
            }
        }

        private fun prior() = Array(N) { i ->
            DoubleArray(N) { j ->
                val d = (i - j).toDouble()
                PRIOR_DB * PRIOR_DB * exp(-d * d / (2 * SMOOTH_OCTAVES * SMOOTH_OCTAVES)) + if (i == j) 0.05 else 0.0
            }
        }

        private fun sigmoid(x: Double) = 1 / (1 + exp(-x))
        private fun entropy(p: DoubleArray) = -p.sumOf { if (it > 0) it * ln(it) else 0.0 }
        private fun dot(a: DoubleArray, b: DoubleArray) = a.indices.sumOf { a[it] * b[it] }

        private fun gaussian(r: Random): Double {
            val u = r.nextDouble().coerceAtLeast(1e-12)
            return sqrt(-2 * ln(u)) * cos(2 * Math.PI * r.nextDouble())
        }

        private fun cholesky(a: Array<DoubleArray>): Array<DoubleArray> {
            val n = a.size
            val l = Array(n) { DoubleArray(n) }
            for (i in 0 until n) for (j in 0..i) {
                val s = a[i][j] - (0 until j).sumOf { l[i][it] * l[j][it] }
                l[i][j] = if (i == j) sqrt(s.coerceAtLeast(1e-12)) else s / l[j][j]
            }
            return l
        }

        private fun logDet(a: Array<DoubleArray>): Double {
            val l = cholesky(a)
            return 2 * (a.indices).sumOf { ln(l[it][it]) }
        }

        private fun solve(a: Array<DoubleArray>, b: DoubleArray): DoubleArray {
            val l = cholesky(a)
            val n = b.size
            val y = DoubleArray(n)
            for (i in 0 until n) y[i] = (b[i] - (0 until i).sumOf { l[i][it] * y[it] }) / l[i][i]
            val x = DoubleArray(n)
            for (i in n - 1 downTo 0) x[i] = (y[i] - (i + 1 until n).sumOf { l[it][i] * x[it] }) / l[i][i]
            return x
        }

        private fun invert(a: Array<DoubleArray>): Array<DoubleArray> {
            val n = a.size
            val columns = List(n) { c -> solve(a, DoubleArray(n) { if (it == c) 1.0 else 0.0 }) }
            return Array(n) { i -> DoubleArray(n) { j -> columns[j][i] } }
        }
    }
}
