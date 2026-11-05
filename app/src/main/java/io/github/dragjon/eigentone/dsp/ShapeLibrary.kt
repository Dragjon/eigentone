package io.github.dragjon.eigentone.dsp

import java.util.Locale
import kotlin.math.abs
import kotlin.math.log2

/** The top-level classes of the library, in the order they're browsed. */
enum class ShapeCategory(val label: String, val examples: String) {
    REFERENCE("Research & targets", "Equal loudness, Harman, speech"),
    FIX("Fix a problem", "Muddy, harsh, thin, dull"),
    MUSIC("Music", "Pop, rock, hip-hop, jazz, classical"),
    INSTRUMENT("Instruments", "Vocals, guitar, drums, piano"),
    MEDIA("Speech & media", "Podcasts, films, games, calls"),
    SITUATION("Situations", "Commute, gym, sleep, low volume"),
    GEAR("Headphones & speakers", "Earbuds, speakers, car"),
    CHARACTER("Character", "Warm, bright, fun, effects"),
    BLOCKS("Building blocks", "Shelves, peaks, tilts"),
    YOURS("Your shapes", "Ones you've made or saved"),
}

/** What a curve actually does. Worked out from the curve itself, so it can't disagree with it. */
enum class ShapeTrait(val label: String) {
    MORE_BASS("More bass"),
    LESS_BASS("Less bass"),
    MORE_MIDS("More mids"),
    SCOOPED("Scooped mids"),
    MORE_TREBLE("More treble"),
    LESS_TREBLE("Less treble"),
    GENTLE("Gentle"),
    STRONG("Strong"),
}

data class LibraryShape(
    val name: String,
    val category: ShapeCategory,
    val group: String,
    val tags: String,
    val description: String,
    val points: List<Pair<Double, Double>>,
    private val fixedId: String? = null,
    /** Where the numbers come from. Null means a designed starting point rather than published data. */
    val source: String? = null,
) {
    val id: String = fixedId ?: name.lowercase(Locale.US).replace(Regex("[^a-z0-9+-]+"), "-").trim('-')
    val shape: GraphicShape get() = GraphicShape(name, points)

    /** The curve on the finest fader layout, for traits and thumbnails. */
    val curve: List<Double> by lazy(LazyThreadSafetyMode.PUBLICATION) { GraphicEq.apply(shape, 31) }
    val traits: Set<ShapeTrait> by lazy(LazyThreadSafetyMode.PUBLICATION) { ShapeLibrary.traitsOf(curve) }

    internal val searchText: String by lazy(LazyThreadSafetyMode.PUBLICATION) {
        ShapeLibrary.normalise(
            "$name $group ${category.label} $tags $description ${source.orEmpty()} ${traits.joinToString(" ") { it.label }}",
        )
    }
}

/**
 * A searchable collection of starting curves for the graphic EQ.
 *
 * Shapes with a [LibraryShape.source] are worked out from published data. The rest are designed starting
 * points based on general mixing practice about which frequencies do what. They aren't measurements, and
 * they don't reproduce any artist's or manufacturer's sound. Listening tests found preferences move only
 * about 1 dB between genres, and common genre presets disagree with each other, so genre shapes stay gentle.
 * Headphone corrections from real measurements are a separate feature (AutoEq).
 */
object ShapeLibrary {
    val all: List<LibraryShape> by lazy(LazyThreadSafetyMode.PUBLICATION) {
        ShapeData.blocks.flatMap { (category, text) -> parse(category, text) } + buildingBlocks()
    }

    fun find(id: String, yours: List<LibraryShape> = emptyList()): LibraryShape? =
        yours.firstOrNull { it.id == id } ?: all.firstOrNull { it.id == id }

    /** A shape the user owns, in the form the library works with. */
    fun yours(id: String, name: String, points: List<Pair<Double, Double>>): LibraryShape =
        LibraryShape(name, ShapeCategory.YOURS, "Your shapes", "mine custom personal", "A shape you saved.", points, id)

    /**
     * Every word must match somewhere. Matches in the name come first, and otherwise the library's
     * order holds, so hand-written shapes stay ahead of the generated building blocks.
     */
    fun search(
        query: String,
        category: ShapeCategory? = null,
        trait: ShapeTrait? = null,
        only: Set<String>? = null,
        yours: List<LibraryShape> = emptyList(),
    ): List<LibraryShape> {
        val words = normalise(query).split(' ').filter { it.isNotEmpty() }
        val matches = (yours + all).filter { shape ->
            (category == null || shape.category == category) &&
                (trait == null || trait in shape.traits) &&
                (only == null || shape.id in only) &&
                words.all { matches(it, shape) }
        }
        if (words.isEmpty()) return matches
        return matches.sortedWith(
            compareBy<LibraryShape> { it.category == ShapeCategory.BLOCKS }.thenBy { shape ->
                val name = normalise(shape.name)
                when {
                    name == words.joinToString(" ") -> 0
                    words.all { word -> name.split(' ').any { it.startsWith(word) } } -> 1
                    words.all { it in name } -> 2
                    else -> 3
                }
            },
        )
    }

    /** Numbers match whole, so "100" doesn't find 1000 Hz. "4" finds either sign, and "+4" only that one. */
    private fun matches(word: String, shape: LibraryShape): Boolean {
        if (!NUMBER.matches(word)) return word in shape.searchText
        val words = shape.searchText.split(' ')
        return word in words || (word[0].isDigit() && ("+$word" in words || "-$word" in words))
    }

    /**
     * Lower case with punctuation removed, so "hip-hop", "hiphop" and "R&B" all find their shapes.
     * A minus sign in front of a number stays, because -4 dB and +4 dB are different shapes.
     */
    internal fun normalise(text: String): String = text.lowercase(Locale.US)
        .replace("&", "")
        .replace(Regex("(?<=\\S)-|-(?!\\d)"), "")
        .replace(Regex("[^a-z0-9+. -]"), " ")
        .replace(Regex(" +"), " ")
        .trim()

    internal fun traitsOf(curve: List<Double>): Set<ShapeTrait> {
        val freqs = GraphicEq.frequencies(curve.size)
        fun average(range: ClosedFloatingPointRange<Double>): Double =
            freqs.indices.filter { freqs[it] in range }.map { curve[it] }.average()

        val bass = average(20.0..150.0)
        val mids = average(300.0..2000.0)
        val treble = average(5000.0..20000.0)
        val peak = curve.maxOf { abs(it) }
        return buildSet {
            if (bass >= NOTABLE_DB && bass > mids) add(ShapeTrait.MORE_BASS)
            if (bass <= -NOTABLE_DB && bass < mids) add(ShapeTrait.LESS_BASS)
            if (treble >= NOTABLE_DB && treble > mids) add(ShapeTrait.MORE_TREBLE)
            if (treble <= -NOTABLE_DB && treble < mids) add(ShapeTrait.LESS_TREBLE)
            if (mids <= minOf(bass, treble) - NOTABLE_DB) add(ShapeTrait.SCOOPED)
            if (mids >= maxOf(bass, treble) + NOTABLE_DB) add(ShapeTrait.MORE_MIDS)
            if (peak <= GENTLE_DB) add(ShapeTrait.GENTLE)
            if (peak >= STRONG_DB) add(ShapeTrait.STRONG)
        }
    }

    /** Each line reads: name | group | search words | description | hz:db hz:db ... */
    private fun parse(category: ShapeCategory, text: String): List<LibraryShape> =
        text.lineSequence().map { it.trim() }.filter { it.isNotEmpty() }.map { line ->
            val parts = line.split('|').map { it.trim() }
            require(parts.size in 5..6) { "Bad shape line: $line" }
            LibraryShape(
                name = parts[0],
                category = category,
                group = parts[1],
                tags = parts[2],
                description = parts[3],
                points = parts[4].split(' ').map { point ->
                    val (hz, db) = point.split(':')
                    hz.toDouble() to db.toDouble()
                },
                source = parts.getOrNull(5),
            )
        }.toList()

    private fun signed(db: Int) = if (db > 0) "+$db" else "$db"

    private fun hz(value: Int) = if (value >= 1000) "${value / 1000} kHz" else "$value Hz"

    /** Single-purpose curves at set corners and amounts, for building a sound one step at a time. */
    private fun buildingBlocks(): List<LibraryShape> = buildList {
        val cat = ShapeCategory.BLOCKS

        for (corner in listOf(60, 100, 200)) {
            for (db in listOf(8, 6, 4, 2, -2, -4, -6)) {
                add(
                    LibraryShape(
                        "Bass ${signed(db)} dB below ${hz(corner)}", cat, "Bass shelves", "low shelf",
                        "Changes everything below ${hz(corner)}, and leaves the rest alone.",
                        listOf(corner / 2.0 to db.toDouble(), corner * 2.0 to 0.0),
                    ),
                )
            }
        }
        for (corner in listOf(4000, 8000, 12000)) {
            for (db in listOf(6, 4, 2, -2, -4, -6)) {
                add(
                    LibraryShape(
                        "Treble ${signed(db)} dB above ${hz(corner)}", cat, "Treble shelves", "high shelf",
                        "Changes everything above ${hz(corner)}, and leaves the rest alone.",
                        listOf(corner / 2.0 to 0.0, corner * 2.0 to db.toDouble()),
                    ),
                )
            }
        }
        val centres = listOf(63, 125, 250, 500, 1000, 2000, 4000, 8000, 16000)
        for (centre in centres) {
            val label = hz(centre)
            for (db in listOf(4, 2, -2, -4)) {
                add(
                    LibraryShape(
                        "Peak ${signed(db)} dB at $label", cat, "Peaks and dips", "bell boost cut notch",
                        "A one-octave ${if (db > 0) "lift" else "dip"} centred on $label.",
                        listOf(centre / 2.0 to 0.0, centre.toDouble() to db.toDouble(), centre * 2.0 to 0.0),
                    ),
                )
            }
        }
        for (pivot in listOf(1000)) {
            for (quarter in listOf(1, 2, 3, 4)) {
                val slope = quarter / 4.0
                for (sign in listOf(1, -1)) {
                    val perOctave = slope * sign
                    add(
                        LibraryShape(
                            "Tilt ${if (sign > 0) "brighter" else "darker"} ${slope} dB per octave at ${hz(pivot)}",
                            cat, "Tilts", "slope balance warm",
                            "Leans the whole balance around ${hz(pivot)} without any single peak.",
                            listOf(
                                20.0 to perOctave * log2(20.0 / pivot),
                                20000.0 to perOctave * log2(20000.0 / pivot),
                            ),
                        ),
                    )
                }
            }
        }
        for (low in listOf(2, 4, 6)) {
            for (high in listOf(2, 4, 6)) {
                add(
                    LibraryShape(
                        "Smile bass +$low treble +$high", cat, "Smile curves", "v shape loudness",
                        "Lifts both ends by these amounts and leaves the mids untouched.",
                        listOf(50.0 to low.toDouble(), 250.0 to 0.0, 2000.0 to 0.0, 12000.0 to high.toDouble()),
                    ),
                )
            }
        }
    }

    private val NUMBER = Regex("[+-]?\\d+(\\.\\d+)?")
    private const val NOTABLE_DB = 1.5
    private const val GENTLE_DB = 3.0
    private const val STRONG_DB = 7.0
}
