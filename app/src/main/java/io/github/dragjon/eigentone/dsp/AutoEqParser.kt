package io.github.dragjon.eigentone.dsp

import java.util.Locale

data class ParsedEq(val preampDb: Double, val filters: List<Filter>)

/** Parses the Equalizer APO / AutoEq "ParametricEQ.txt" format. */
object AutoEqParser {
    const val MAX_FILTERS = 20

    private val preampRegex = Regex("""^\s*Preamp:\s*([-+]?[\d.,]+)\s*dB""", RegexOption.IGNORE_CASE)
    private val filterRegex = Regex(
        """^\s*Filter(?:\s+\d+)?:\s*(ON|OFF)\s+(PK|PEQ|LSC|LS|HSC|HS|LPQ|LP|HPQ|HP|NO|BP)(?:\s+\d+\s*dB)?\s+Fc\s+([\d.,]+)\s*Hz""" +
            """(?:\s+Gain\s+([-+]?[\d.,]+)\s*dB)?(?:\s+Q\s+([\d.,]+))?""",
        RegexOption.IGNORE_CASE,
    )

    /** Writes filters in the same format, which AutoEq-compatible apps and Equalizer APO can read. */
    fun format(preampDb: Double, filters: List<Filter>): String = buildString {
        appendLine(String.format(Locale.US, "Preamp: %.1f dB", preampDb))
        filters.flatMap(::lines).forEachIndexed { i, f ->
            val type = when (f.type) {
                FilterType.BELL -> "PK"
                FilterType.LOW_SHELF -> "LSC"
                FilterType.HIGH_SHELF -> "HSC"
                FilterType.LOW_PASS -> "LPQ"
                FilterType.HIGH_PASS -> "HPQ"
                FilterType.NOTCH -> "NO"
                FilterType.BAND_PASS -> "BP"
                FilterType.CURVE, FilterType.TILT -> error("Written as other filters by lines()")
            }
            val gain = if (f.type.hasGain) String.format(Locale.US, " Gain %.2f dB", f.gainDb) else ""
            appendLine(
                String.format(
                    Locale.US, "Filter %d: %s %s Fc %.1f Hz%s Q %.3f",
                    i + 1, if (f.enabled) "ON" else "OFF", type, f.freq, gain, f.q,
                ),
            )
        }
    }

    /**
     * The file lines a filter becomes. The format has no tilt or steep slopes, so a tilt is written as its
     * two shelves and a steep pass as its 12 dB sections, which sound the same.
     */
    private fun lines(f: Filter): List<Filter> = when {
        f.type == FilterType.CURVE -> error("A curve can't be written as a filter")
        f.type == FilterType.TILT -> listOf(
            f.copy(type = FilterType.LOW_SHELF, gainDb = -f.gainDb),
            f.copy(type = FilterType.HIGH_SHELF, gainDb = f.gainDb),
        )
        f.type.hasSlope -> {
            require(Eq.canExport(f)) { "Odd slopes have a first-order section the format can't hold" }
            val qs = Eq.butterworthQs(Eq.slopeOf(f) / 6)
            val sharpest = qs.indices.maxBy { qs[it] }
            qs.mapIndexed { i, q -> f.copy(q = if (i == sharpest) q * f.q / Eq.BUTTERWORTH_Q else q, slope = Eq.DEFAULT_SLOPE) }
        }
        else -> listOf(f)
    }

    /** Returns null when the text holds no recognisable filters. */
    fun parse(text: String): ParsedEq? {
        var preamp = 0.0
        val filters = mutableListOf<Filter>()
        for (line in text.lineSequence()) {
            preampRegex.find(line)?.let { preamp = (number(it.groupValues[1]) ?: preamp).coerceIn(-MAX_IMPORT_GAIN, MAX_IMPORT_GAIN) }
            val m = filterRegex.find(line) ?: continue
            if (filters.size == MAX_FILTERS) break
            val type = when (m.groupValues[2].uppercase()) {
                "PK", "PEQ" -> FilterType.BELL
                "LSC", "LS" -> FilterType.LOW_SHELF
                "HSC", "HS" -> FilterType.HIGH_SHELF
                "LPQ", "LP" -> FilterType.LOW_PASS
                "NO" -> FilterType.NOTCH
                "BP" -> FilterType.BAND_PASS
                else -> FilterType.HIGH_PASS
            }
            val freq = number(m.groupValues[3]) ?: continue
            filters += Filter(
                id = filters.size + 1,
                type = type,
                freq = freq.coerceIn(Eq.MIN_FREQ, Eq.MAX_FREQ),
                gainDb = (number(m.groupValues[4]) ?: 0.0).coerceIn(-MAX_IMPORT_GAIN, MAX_IMPORT_GAIN),
                q = (number(m.groupValues[5]) ?: DEFAULT_Q).coerceIn(Eq.MIN_Q, Eq.MAX_Q),
                enabled = m.groupValues[1].equals("ON", ignoreCase = true),
            )
        }
        return if (filters.isEmpty()) null else ParsedEq(preamp, filters)
    }

    /**
     * Reads a number written either way: a comma before exactly three digits groups thousands, as in
     * "1,000", and any other comma is a decimal point, as files saved in many languages write "105,5".
     */
    private fun number(text: String): Double? {
        val thousands = Regex("""^\d{1,3}(,\d{3})+(\.\d+)?$""").matches(text)
        return (if (thousands) text.replace(",", "") else text.replace(',', '.')).toDoubleOrNull()
    }

    private const val DEFAULT_Q = 0.71

    /**
     * Wider than the UI's editing range. AutoEq files can pair large opposing filters that net out
     * to something gentle, so clamping one of them to the UI range would change the curve.
     */
    private const val MAX_IMPORT_GAIN = 30.0
}
