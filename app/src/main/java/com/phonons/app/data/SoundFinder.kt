package com.phonons.app.data

/** One quality the listening test asks about. Ranges follow published preference research. */
enum class Trait(val title: String, val question: String) {
    BASS("Bass", "Which has the right amount of bass?"),
    VOCALS("Vocals", "Which makes voices and instruments clearer?"),
    TREBLE("Treble", "Which has the right amount of detail, without being sharp?"),
    SPACE("Space", "Which feels better: more focused, or more spacious?"),
}

enum class Choice { A, B, NO_PREFERENCE }

/** The two versions being compared. */
data class Round(val trait: Trait, val a: Double, val b: Double)

/**
 * A short listening test that narrows each [Trait] by repeated A or B choices, in the style of an
 * eye test. The range halves after each answer, so a few answers cover it. It finds a preference
 * for these headphones and this music, not an objectively best sound.
 *
 * "No preference" always moves towards the smaller change, because no audible benefit means no reason
 * to alter the sound.
 */
class SoundFinder(private val base: Tone) {
    private val ladders = mapOf(
        // Harman's over-ear preference study found groups wanting about 2-3 dB less and 3-6 dB more
        // bass than its target. This range covers those, with room either side.
        Trait.BASS to listOf(-6.0, -4.5, -3.0, -1.5, 0.0, 1.5, 3.0, 4.5, 6.0),
        Trait.VOCALS to listOf(-3.0, -2.25, -1.5, -0.75, 0.0, 0.75, 1.5, 2.25, 3.0),
        Trait.TREBLE to listOf(-4.0, -3.0, -2.0, -1.0, 0.0, 1.0, 2.0, 3.0, 4.0),
    )
    private val traits = Trait.entries
    private var traitIndex = 0
    private var low = 0
    private var high = LADDER_TOP
    private var spaceStep = 0
    private var spaceWide = false
    private val answers = mutableMapOf<Trait, Double>()
    private var answered = 0

    val totalRounds: Int = LADDER_ROUNDS * ladders.size + SPACE_ROUNDS
    val roundNumber: Int get() = answered + 1
    val isDone: Boolean get() = traitIndex >= traits.size

    val current: Round?
        get() {
            val trait = traits.getOrNull(traitIndex) ?: return null
            if (trait == Trait.SPACE) {
                return if (spaceStep == 0) Round(trait, 0.0, SPACE_FIRST) else Round(trait, SPACE_LOW, SPACE_HIGH)
            }
            val ladder = ladders.getValue(trait)
            val middle = (low + high) / 2
            return Round(trait, ladder[(low + middle) / 2], ladder[(middle + high) / 2])
        }

    fun choose(choice: Choice) {
        val round = current ?: return
        answered++
        if (round.trait == Trait.SPACE) {
            chooseSpace(choice)
            return
        }
        val ladder = ladders.getValue(round.trait)
        val middle = (low + high) / 2
        when (choice) {
            Choice.A -> high = middle
            Choice.B -> low = middle
            Choice.NO_PREFERENCE -> {
                // Narrow towards no change, so hearing no difference never alters the sound.
                val zeroIndex = ladder.indexOfFirst { it >= 0.0 }.coerceIn(low, high)
                low = (low + zeroIndex) / 2
                high = (high + zeroIndex + 1) / 2
            }
        }
        if (high - low <= FINISHED_SPAN) {
            answers[round.trait] = ladder[(low + high) / 2]
            traitIndex++
            low = 0
            high = LADDER_TOP
        }
    }

    private fun chooseSpace(choice: Choice) {
        if (spaceStep == 0) {
            if (choice == Choice.B) {
                spaceWide = true
                spaceStep = 1
            } else {
                // Skips the follow-up, which is why the test can end a question early.
                answered = totalRounds
                answers[Trait.SPACE] = 0.0
                traitIndex++
            }
            return
        }
        answers[Trait.SPACE] = when (choice) {
            Choice.A -> SPACE_LOW
            Choice.B -> SPACE_HIGH
            Choice.NO_PREFERENCE -> SPACE_FIRST
        }
        traitIndex++
    }

    /** The sound with every answer so far, without the comparison in progress. */
    fun result(): Tone = tone(emptyMap())

    /** The sound to play for one side of the current comparison. */
    fun preview(side: Choice): Tone {
        val round = current ?: return result()
        val value = if (side == Choice.B) round.b else round.a
        return tone(mapOf(round.trait to value))
    }

    /** What each answer changed, for the summary. */
    fun answer(trait: Trait): Double = answers[trait] ?: 0.0

    private fun tone(overrides: Map<Trait, Double>): Tone {
        fun value(trait: Trait) = overrides[trait] ?: answers[trait] ?: 0.0
        return base.copy(
            bassDb = (base.bassDb + value(Trait.BASS)).coerceIn(-Processing.MAX_TONE_DB, Processing.MAX_TONE_DB),
            vocalsDb = (base.vocalsDb + value(Trait.VOCALS)).coerceIn(-Processing.MAX_TONE_DB, Processing.MAX_TONE_DB),
            trebleDb = (base.trebleDb + value(Trait.TREBLE)).coerceIn(-Processing.MAX_TONE_DB, Processing.MAX_TONE_DB),
            surround = if (Trait.SPACE in overrides || Trait.SPACE in answers) value(Trait.SPACE) else base.surround,
        )
    }

    companion object {
        private const val LADDER_TOP = 8
        private const val LADDER_ROUNDS = 2
        private const val SPACE_ROUNDS = 2
        private const val FINISHED_SPAN = 2
        private const val SPACE_FIRST = 0.6
        private const val SPACE_LOW = 0.35
        private const val SPACE_HIGH = 0.85

        /**
         * Adjusts the level so both sides of a comparison sound about equally loud. Otherwise the
         * louder one tends to win, whatever its tone. Compared in full against [reference].
         */
        fun levelMatched(preset: Preset, tone: Tone, reference: Preset): Preset {
            val candidate = preset.copy(tone = tone)
            val difference = Processing.estimatedLoudnessDb(Processing.presetFilters(candidate), candidate.preampDb) -
                Processing.estimatedLoudnessDb(Processing.presetFilters(reference), reference.preampDb)
            return candidate.copy(preampDb = candidate.preampDb - difference)
        }
    }
}
