package com.phonons.app.data

import com.phonons.app.dsp.GraphicEq
import java.util.Locale

/** Reads an EQ curve typed by hand, such as one copied from another app's screen. */
object ShapeText {
    const val MIN_HZ = 10.0
    const val MAX_HZ = 40000.0
    const val MAX_POINTS = 128
    private const val MAX_CURVE_POINTS = 512
    private const val MAX_CURVE_DB = 30.0
    private const val GRAPHIC_PREFIX = "GraphicEQ:"

    private val entry = Regex(
        """(\d+(?:\.\d+)?)\s*(k)?\s*(?:hz)?\s*[:=,\s]\s*([+-]?\d+(?:\.\d+)?)\s*(?:db)?""",
        RegexOption.IGNORE_CASE,
    )

    sealed interface Parsed {
        data class Points(val points: List<ShapePoint>) : Parsed
        data class Problem(val message: String) : Parsed
    }

    /** Accepts lines like "100 +4", "1.6k 4", "12.8 kHz: -2 dB", one per line or separated by semicolons. */
    fun parse(text: String): Parsed {
        val parts = text.trim().removePrefix(GRAPHIC_PREFIX).split('\n', ';').map { it.trim() }.filter { it.isNotEmpty() }
        if (parts.isEmpty()) return Parsed.Problem("Add at least one frequency and level.")
        if (parts.size > MAX_POINTS) return Parsed.Problem("Use $MAX_POINTS points or fewer.")
        val points = mutableListOf<ShapePoint>()
        for (part in parts) {
            val match = entry.matchEntire(part) ?: return Parsed.Problem("Couldn't read \"$part\". Try \"100 +4\".")
            val hz = match.groupValues[1].toDouble() * if (match.groupValues[2].isNotEmpty()) 1000 else 1
            val db = match.groupValues[3].toDouble()
            if (hz !in MIN_HZ..MAX_HZ) return Parsed.Problem("${format(hz)} Hz is out of range.")
            if (db !in -GraphicEq.MAX_DB..GraphicEq.MAX_DB) {
                return Parsed.Problem("${format(db)} dB is out of range. Use up to ${GraphicEq.MAX_DB.toInt()} dB either way.")
            }
            points += ShapePoint(hz, db)
        }
        if (points.map { it.hz }.distinct().size != points.size) return Parsed.Problem("The same frequency appears twice.")
        if (points.all { it.db == 0.0 }) return Parsed.Problem("Every level is 0, so this wouldn't change the sound.")
        return Parsed.Points(points.sortedBy { it.hz })
    }

    /**
     * Reads the single-line "GraphicEQ: 20 -3; 25 1.5" format from AutoEq and Equalizer APO, at full
     * detail. Returns null if the text isn't in that format or any part of it can't be read.
     */
    fun parseGraphicEq(text: String): List<ShapePoint>? {
        val line = text.lineSequence().map { it.trim() }.firstOrNull { it.startsWith(GRAPHIC_PREFIX, ignoreCase = true) }
            ?: return null
        val parts = line.substring(GRAPHIC_PREFIX.length).split(';').map { it.trim() }.filter { it.isNotEmpty() }
        if (parts.size > MAX_CURVE_POINTS) return null
        val points = parts.map { part ->
            val match = entry.matchEntire(part) ?: return null
            val hz = match.groupValues[1].toDouble() * if (match.groupValues[2].isNotEmpty()) 1000 else 1
            ShapePoint(hz, match.groupValues[3].toDouble())
        }
        return cleanCurve(points).takeIf { it.isNotEmpty() }
    }

    /** Makes a curve from outside the app safe to use: sorted, in range, with no repeated frequencies. */
    fun cleanCurve(points: List<ShapePoint>, maxDb: Double = MAX_CURVE_DB): List<ShapePoint> = points
        .filter { it.hz in MIN_HZ..MAX_HZ && it.db.isFinite() }
        .distinctBy { it.hz }
        .take(MAX_CURVE_POINTS)
        .map { it.copy(db = it.db.coerceIn(-maxDb, maxDb)) }
        .sortedBy { it.hz }

    fun graphicEq(points: List<ShapePoint>): String = GRAPHIC_PREFIX + " " + points.joinToString("; ") { point ->
        "${format(point.hz)} ${String.format(Locale.US, "%.1f", point.db)}"
    }

    fun format(points: List<ShapePoint>): String = points.joinToString("\n") { point ->
        val sign = if (point.db > 0) "+" else ""
        "${format(point.hz)} $sign${format(point.db)}"
    }

    private fun format(value: Double): String =
        if (value == kotlin.math.round(value)) value.toInt().toString() else String.format(Locale.US, "%.1f", value)
}
