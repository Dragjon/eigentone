package io.github.dragjon.eigentone.ui

import androidx.compose.material3.ColorScheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import kotlin.math.atan2
import kotlin.math.cbrt
import kotlin.math.cos
import kotlin.math.hypot
import kotlin.math.pow
import kotlin.math.roundToInt
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
        // A colour of one's own also carries how vivid and how light it is. The swatches are as designed.
        val own = if (seed in SWATCHES) null else picked(seed)
        val vivid = own?.saturation?.toDouble() ?: 1.0
        // Further towards dark than light, and furthest by day, where a deep colour on white still reads well; at
        // night the colour is drawn light, and too dark it would lose its text (user decision).
        val shift = own?.let {
            val t = (it.lightness - 0.5f) * 2
            (t * if (t >= 0f) MAX_TONE_SHIFT else if (dark) MAX_DARK_SHIFT_NIGHT else MAX_DARK_SHIFT_DAY).roundToInt()
        } ?: 0
        val p = Tonal(hue, PRIMARY_CHROMA * vivid)
        val s = Tonal(hue, SECONDARY_CHROMA * vivid)
        val t = Tonal(hue + 60.0, TERTIARY_CHROMA * vivid)
        val n = Tonal(hue, NEUTRAL_CHROMA * vivid)
        val nv = Tonal(hue, NEUTRAL_VARIANT_CHROMA * vivid)
        val error = Tonal(25.0, 60.0)
        return if (dark) darkColorScheme(
            primary = p(80 + shift), onPrimary = p(20), primaryContainer = p(30), onPrimaryContainer = p(90), inversePrimary = p(40),
            secondary = s(80), onSecondary = s(20), secondaryContainer = s(30), onSecondaryContainer = s(90),
            tertiary = t(80), onTertiary = t(20), tertiaryContainer = t(30), onTertiaryContainer = t(90),
            background = n(6), onBackground = n(90), surface = n(6), onSurface = n(90),
            surfaceVariant = nv(30), onSurfaceVariant = nv(80), surfaceTint = p(80 + shift),
            inverseSurface = n(90), inverseOnSurface = n(20),
            error = error(80), onError = error(20), errorContainer = error(30), onErrorContainer = error(90),
            outline = nv(60), outlineVariant = nv(30), scrim = Color.Black,
            surfaceBright = n(24), surfaceDim = n(6),
            surfaceContainerLowest = n(4), surfaceContainerLow = n(10), surfaceContainer = n(12),
            surfaceContainerHigh = n(17), surfaceContainerHighest = n(22),
        ) else lightColorScheme(
            primary = p(40 + shift), onPrimary = p(100), primaryContainer = p(90), onPrimaryContainer = p(10), inversePrimary = p(80),
            secondary = s(40), onSecondary = s(100), secondaryContainer = s(90), onSecondaryContainer = s(10),
            tertiary = t(40), onTertiary = t(100), tertiaryContainer = t(90), onTertiaryContainer = t(10),
            background = n(98), onBackground = n(10), surface = n(98), onSurface = n(10),
            surfaceVariant = nv(90), onSurfaceVariant = nv(30), surfaceTint = p(40 + shift),
            inverseSurface = n(20), inverseOnSurface = n(95),
            error = error(40), onError = error(100), errorContainer = error(90), onErrorContainer = error(10),
            outline = nv(50), outlineVariant = nv(80), scrim = Color.Black,
            surfaceBright = n(98), surfaceDim = n(87),
            surfaceContainerLowest = n(100), surfaceContainerLow = n(96), surfaceContainer = n(94),
            surfaceContainerHigh = n(92), surfaceContainerHighest = n(90),
        )
    }

    /**
     * The second accent: the hue opposite [primary]'s, at its lightness and about its vividness, so it reads as
     * strongly against the page, and its text colour (user decision). Used sparingly, on the few things that should
     * stand out from the controls in the main colour.
     */
    fun accentFor(primary: Color, dark: Boolean): Pair<Color, Color> {
        // A pair chosen by eye for the main colour's family, as modern apps pair them (blue with coral, purple
        // with pink, amber with violet), rather than a fixed turn of the wheel: the exact opposite gave olive and
        // brown that clashed (tester feedback).
        val hsv = FloatArray(3)
        android.graphics.Color.colorToHSV(primary.toArgb(), hsv)
        val pick = Color(ACCENT_PAIRS.first { (range, _) -> hsv[0] in range }.second.toInt())
        val (pl, pa, pb) = lab(pick)
        val hue = hueOf(pick)
        val chroma = hypot(pa, pb)
        // Lighter at night, as Material's own accents are there, and as picked by day.
        val tone = if (dark) maxOf(pl, NIGHT_ACCENT_TONE) else pl.coerceAtMost(DAY_ACCENT_TONE)
        val accent = fromLch(tone, chroma, hue)
        // Dark text on a light accent and white on a deep one.
        val onAccent = if (tone >= LIGHT_ACCENT_TONE) fromLch(15.0, chroma / 4, hue) else Color.White
        return accent to onAccent
    }

    /**
     * Each main colour family, by hue in degrees, and the accent it pairs with. Cool colours take a warm coral or
     * pink; warm ones a cool violet or blue, so the accent always stands apart.
     */
    private val ACCENT_PAIRS: List<Pair<ClosedFloatingPointRange<Float>, Long>> = listOf(
        0f..20f to 0xFF7C5CFF, // red: violet
        20f..50f to 0xFF4F6BFF, // orange: blue
        50f..80f to 0xFF7C5CFF, // amber and yellow: violet
        80f..165f to 0xFFFF5C8A, // green: pink
        165f..195f to 0xFFFF7A59, // teal: coral
        195f..260f to 0xFFFF6B5A, // blue: coral
        260f..320f to 0xFFFF5CA8, // purple: pink
        320f..360f to 0xFF7C5CFF, // pink: violet
    )
    private const val NIGHT_ACCENT_TONE = 72.0
    // Dark enough by day for the curve to stand 3:1 against the white page; at 62 it was about 2.8:1 (MD3 audit).
    private const val DAY_ACCENT_TONE = 55.0
    private const val LIGHT_ACCENT_TONE = 60.0

    /** A colour of the given [hue] in degrees, as vivid as a swatch, for a colour picked by hue alone. */
    fun fromHue(hue: Float): Long = fromPicked(hue, 1f, 0.5f)

    /**
     * A colour picked by hue, how vivid it is (0 grey to 1 as vivid as a swatch) and how light (0 to 1, a
     * swatch's lightness halfway). Saved as the colour itself, which [picked] reads back.
     */
    fun fromPicked(hue: Float, saturation: Float, lightness: Float): Long {
        val tone = PICK_TONE_LOW + (PICK_TONE_HIGH - PICK_TONE_LOW) * lightness.coerceIn(0f, 1f).toDouble()
        val most = chromaOf(fromLch(tone, PRIMARY_CHROMA, hue.toDouble()))
        return fromLch(tone, most * saturation.coerceIn(0f, 1f), hue.toDouble()).argbLong()
    }

    /** A picked colour: its hue in degrees, and its saturation and lightness from 0 to 1. */
    data class Picked(val hue: Float, val saturation: Float, val lightness: Float)

    /** Reads a colour of one's own back as hue, saturation and lightness, the opposite of [fromPicked]. */
    fun picked(seed: Long): Picked {
        val color = Color(seed.toInt())
        val (l, a, b) = lab(color)
        val hue = hueOf(color)
        val most = chromaOf(fromLch(l, PRIMARY_CHROMA, hue))
        return Picked(
            hue = hue.toFloat(),
            saturation = (if (most > 0.5) hypot(a, b) / most else 0.0).coerceIn(0.0, 1.0).toFloat(),
            lightness = ((l - PICK_TONE_LOW) / (PICK_TONE_HIGH - PICK_TONE_LOW)).coerceIn(0.0, 1.0).toFloat(),
        )
    }

    private fun chromaOf(color: Color): Double = lab(color).let { (_, a, b) -> hypot(a, b) }

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

    /** The lightness range a picked colour spans, a swatch's 50 in the middle. */
    // Down to 25, so the swatch shows how deep the darkest choice goes (user decision).
    private const val PICK_TONE_LOW = 25.0
    private const val PICK_TONE_HIGH = 65.0

    /** How far the accent's tone moves at either end of the lightness slider, kept small so text on it stays readable. */
    private const val MAX_TONE_SHIFT = 10
    /** How far darker the accent can go at the slider's dark end: furthest by day. */
    private const val MAX_DARK_SHIFT_DAY = 20
    private const val MAX_DARK_SHIFT_NIGHT = 14

    // Lab chroma, a little above Material's CAM16 figures, which run lower for the same vividness.
    private const val PRIMARY_CHROMA = 48.0
    private const val SECONDARY_CHROMA = 18.0
    private const val TERTIARY_CHROMA = 28.0
    private const val NEUTRAL_CHROMA = 4.0
    private const val NEUTRAL_VARIANT_CHROMA = 8.0
}

private fun Color.argbLong(): Long = toArgb().toLong() and 0xFFFFFFFFL
