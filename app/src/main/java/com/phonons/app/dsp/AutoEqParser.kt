package com.phonons.app.dsp

import java.util.Locale

data class ParsedEq(val preampDb: Double, val filters: List<Filter>)

/** Parses the Equalizer APO / AutoEq "ParametricEQ.txt" format. */
object AutoEqParser {
    const val MAX_FILTERS = 20

    private val preampRegex = Regex("""^\s*Preamp:\s*([-+]?[\d.]+)\s*dB""", RegexOption.IGNORE_CASE)
    private val filterRegex = Regex(
        """^\s*Filter(?:\s+\d+)?:\s*(ON|OFF)\s+(PK|PEQ|LSC|LS|HSC|HS|LPQ|LP|HPQ|HP)\s+Fc\s+([\d.,]+)\s*Hz""" +
            """(?:\s+Gain\s+([-+]?[\d.]+)\s*dB)?(?:\s+Q\s+([\d.]+))?""",
        RegexOption.IGNORE_CASE,
    )

    /** Writes filters in the same format, which AutoEq-compatible apps and Equalizer APO can read. */
    fun format(preampDb: Double, filters: List<Filter>): String = buildString {
        appendLine(String.format(Locale.US, "Preamp: %.1f dB", preampDb))
        filters.forEachIndexed { i, f ->
            val type = when (f.type) {
                FilterType.BELL -> "PK"
                FilterType.LOW_SHELF -> "LSC"
                FilterType.HIGH_SHELF -> "HSC"
                FilterType.LOW_PASS -> "LPQ"
                FilterType.HIGH_PASS -> "HPQ"
                FilterType.CURVE -> error("A curve can't be written as a filter")
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

    /** Returns null when the text holds no recognisable filters. */
    fun parse(text: String): ParsedEq? {
        var preamp = 0.0
        val filters = mutableListOf<Filter>()
        for (line in text.lineSequence()) {
            preampRegex.find(line)?.let { preamp = (it.groupValues[1].toDoubleOrNull() ?: preamp).coerceIn(-MAX_IMPORT_GAIN, MAX_IMPORT_GAIN) }
            val m = filterRegex.find(line) ?: continue
            if (filters.size == MAX_FILTERS) break
            val type = when (m.groupValues[2].uppercase()) {
                "PK", "PEQ" -> FilterType.BELL
                "LSC", "LS" -> FilterType.LOW_SHELF
                "HSC", "HS" -> FilterType.HIGH_SHELF
                "LPQ", "LP" -> FilterType.LOW_PASS
                else -> FilterType.HIGH_PASS
            }
            val freq = m.groupValues[3].replace(",", "").toDoubleOrNull() ?: continue
            filters += Filter(
                id = filters.size + 1,
                type = type,
                freq = freq.coerceIn(Eq.MIN_FREQ, Eq.MAX_FREQ),
                gainDb = (m.groupValues[4].toDoubleOrNull() ?: 0.0).coerceIn(-MAX_IMPORT_GAIN, MAX_IMPORT_GAIN),
                q = (m.groupValues[5].toDoubleOrNull() ?: DEFAULT_Q).coerceIn(Eq.MIN_Q, Eq.MAX_Q),
                enabled = m.groupValues[1].equals("ON", ignoreCase = true),
            )
        }
        return if (filters.isEmpty()) null else ParsedEq(preamp, filters)
    }

    private const val DEFAULT_Q = 0.71

    /**
     * Wider than the UI's editing range. AutoEq files can pair large opposing filters that net out
     * to something gentle, so clamping one of them to the UI range would change the curve.
     */
    private const val MAX_IMPORT_GAIN = 30.0
}
