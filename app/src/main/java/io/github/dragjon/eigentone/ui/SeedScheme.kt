package io.github.dragjon.eigentone.ui

import androidx.compose.material3.ColorScheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import kotlin.math.atan2
import kotlin.math.cbrt
import kotlin.math.cos
import kotlin.math.pow
import kotlin.math.sin

/**
 * Material You's colours made from one colour, for phones whose Android is too old to take them from the
 * wallpaper, and for anyone who picks a colour of their own. Laid out as Android's own "tonal spot" scheme:
 * a vivid primary, a quieter secondary, a tertiary turned 60° round the wheel, and greys with a hint of
 * the colour. Tones are CIE lightness, so a tone reads equally light whatever the hue.
 */
object SeedScheme {
    /** Eigentone's blue, the colour the app wears when no other is chosen. */
    const val DEFAULT_SEED: Long = 0xFF1A73E8

    /** The colours offered to pick from, round the wheel. */
    val SWATCHES: List<Long> = listOf(
        0xFF1A73E8, // blue
        0xFF00897B, // teal
        0xFF2E7D32, // green
        0xFFF9A825, // amber
        0xFFEF6C00, // orange
        0xFFD32F2F, // red
        0xFFD81B60, // pink
        0xFF7B1FA2, // purple
    )

    fun scheme(seed: Long, dark: Boolean): ColorScheme {
        val hue = hueOf(Color(seed.toInt()))
        val p = Tonal(hue, PRIMARY_CHROMA)
        val s = Tonal(hue, SECONDARY_CHROMA)
        val t = Tonal(hue + 60.0, TERTIARY_CHROMA)
        val n = Tonal(hue, NEUTRAL_CHROMA)
        val nv = Tonal(hue, NEUTRAL_VARIANT_CHROMA)
        val error = Tonal(25.0, 60.0)
        return if (dark) darkColorScheme(
            primary = p(80), onPrimary = p(20), primaryContainer = p(30), onPrimaryContainer = p(90), inversePrimary = p(40),
            secondary = s(80), onSecondary = s(20), secondaryContainer = s(30), onSecondaryContainer = s(90),
            tertiary = t(80), onTertiary = t(20), tertiaryContainer = t(30), onTertiaryContainer = t(90),
            background = n(6), onBackground = n(90), surface = n(6), onSurface = n(90),
            surfaceVariant = nv(30), onSurfaceVariant = nv(80), surfaceTint = p(80),
            inverseSurface = n(90), inverseOnSurface = n(20),
            error = error(80), onError = error(20), errorContainer = error(30), onErrorContainer = error(90),
            outline = nv(60), outlineVariant = nv(30), scrim = Color.Black,
            surfaceBright = n(24), surfaceDim = n(6),
            surfaceContainerLowest = n(4), surfaceContainerLow = n(10), surfaceContainer = n(12),
            surfaceContainerHigh = n(17), surfaceContainerHighest = n(22),
        ) else lightColorScheme(
            primary = p(40), onPrimary = p(100), primaryContainer = p(90), onPrimaryContainer = p(10), inversePrimary = p(80),
            secondary = s(40), onSecondary = s(100), secondaryContainer = s(90), onSecondaryContainer = s(10),
            tertiary = t(40), onTertiary = t(100), tertiaryContainer = t(90), onTertiaryContainer = t(10),
            background = n(98), onBackground = n(10), surface = n(98), onSurface = n(10),
            surfaceVariant = nv(90), onSurfaceVariant = nv(30), surfaceTint = p(40),
            inverseSurface = n(20), inverseOnSurface = n(95),
            error = error(40), onError = error(100), errorContainer = error(90), onErrorContainer = error(10),
            outline = nv(50), outlineVariant = nv(80), scrim = Color.Black,
            surfaceBright = n(98), surfaceDim = n(87),
            surfaceContainerLowest = n(100), surfaceContainerLow = n(96), surfaceContainer = n(94),
            surfaceContainerHigh = n(92), surfaceContainerHighest = n(90),
        )
    }

    /** A colour of the given [hue] in degrees, as vivid as a swatch, for a colour picked by hue alone. */
    fun fromHue(hue: Float): Long = Tonal(hue.toDouble(), PRIMARY_CHROMA)(50).argbLong()

    fun hueOf(color: Color): Double {
        val (_, a, b) = lab(color)
        return (Math.toDegrees(atan2(b, a)) + 360.0) % 360.0
    }

    /** One hue at one chroma, at any tone from 0 (black) to 100 (white). */
    private class Tonal(private val hue: Double, private val chroma: Double) {
        operator fun invoke(tone: Int): Color = fromLch(tone.toDouble(), chroma, hue)
    }

    /**
     * The colour at this lightness and hue with as much of [chroma] as sRGB can show, found by easing the
     * chroma off until it fits, so a tone keeps its lightness exactly rather than clipping to something else.
     */
    private fun fromLch(l: Double, chroma: Double, hue: Double): Color {
        if (l <= 0.0) return Color.Black
        if (l >= 100.0) return Color.White
        val h = Math.toRadians(hue)
        var lo = 0.0
        var hi = chroma
        var best = rgbOf(l, 0.0, 0.0)!!
        rgbOf(l, chroma * cos(h), chroma * sin(h))?.let { return it.toColor() }
        repeat(16) {
            val c = (lo + hi) / 2
            val rgb = rgbOf(l, c * cos(h), c * sin(h))
            if (rgb != null) {
                best = rgb
                lo = c
            } else {
                hi = c
            }
        }
        return best.toColor()
    }

    private fun DoubleArray.toColor() = Color(this[0].toFloat(), this[1].toFloat(), this[2].toFloat())

    /** Lab to sRGB, or null when it falls outside what sRGB can show. */
    private fun rgbOf(l: Double, a: Double, b: Double): DoubleArray? {
        val fy = (l + 16) / 116
        val fx = fy + a / 500
        val fz = fy - b / 200
        fun inv(t: Double) = if (t > 6.0 / 29) t * t * t else 3 * (6.0 / 29).pow(2) * (t - 4.0 / 29)
        val x = 0.95047 * inv(fx)
        val y = 1.0 * inv(fy)
        val z = 1.08883 * inv(fz)
        val lin = doubleArrayOf(
            3.2404542 * x - 1.5371385 * y - 0.4985314 * z,
            -0.9692660 * x + 1.8760108 * y + 0.0415560 * z,
            0.0556434 * x - 0.2040259 * y + 1.0572252 * z,
        )
        if (lin.any { it < -1e-4 || it > 1.0001 }) return null
        return DoubleArray(3) { i ->
            val v = lin[i].coerceIn(0.0, 1.0)
            if (v <= 0.0031308) 12.92 * v else 1.055 * v.pow(1 / 2.4) - 0.055
        }
    }

    private fun lab(color: Color): Triple<Double, Double, Double> {
        fun lin(c: Float): Double = if (c <= 0.04045f) c / 12.92 else ((c + 0.055) / 1.055).pow(2.4)
        val r = lin(color.red)
        val g = lin(color.green)
        val b = lin(color.blue)
        val x = (0.4124564 * r + 0.3575761 * g + 0.1804375 * b) / 0.95047
        val y = 0.2126729 * r + 0.7151522 * g + 0.0721750 * b
        val z = (0.0193339 * r + 0.1191920 * g + 0.9503041 * b) / 1.08883
        fun f(t: Double) = if (t > (6.0 / 29).pow(3)) cbrt(t) else t / (3 * (6.0 / 29).pow(2)) + 4.0 / 29
        val l = 116 * f(y) - 16
        return Triple(l, 500 * (f(x) - f(y)), 200 * (f(y) - f(z)))
    }

    // Lab chroma, a little above Material's CAM16 figures, which run lower for the same vividness.
    private const val PRIMARY_CHROMA = 48.0
    private const val SECONDARY_CHROMA = 18.0
    private const val TERTIARY_CHROMA = 28.0
    private const val NEUTRAL_CHROMA = 4.0
    private const val NEUTRAL_VARIANT_CHROMA = 8.0
}

private fun Color.argbLong(): Long = toArgb().toLong() and 0xFFFFFFFFL
