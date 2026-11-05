package io.github.dragjon.eigentone.ui

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.Typography
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import io.github.dragjon.eigentone.R
import io.github.dragjon.eigentone.data.ThemeMode

/** Every colour role, so a theme can only be complete. */
data class ThemeColors(
    val background: Color,
    /** The top and bottom bars, a step darker than the page so they read as fixed. */
    val bar: Color,
    /** Behind the status strip, so it stands apart from both the bars and the page. */
    val strip: Color,
    /** Things that are off, on the strip's green. */
    val stripDim: Color,
    /** Plain text on the strip's green, which stays dark in every theme. */
    val stripText: Color,
    /** Things that are on, on the strip's green. */
    val stripOn: Color,
    /** A warning on the strip's green. */
    val stripDanger: Color,
    /** A softer green under the strip when it's opened, so the details read as a drawer pulled out of it. */
    val stripPanel: Color,
    val surface: Color,
    val surfaceHigh: Color,
    val tooltip: Color,
    /** Hairlines between rows. Decorative, so they sit below control contrast on purpose. */
    val line: Color,
    /** Control boundaries such as off switches and segments. At least 3:1 against every surface. */
    val outline: Color,
    val track: Color,
    val text: Color,
    val textDim: Color,
    val lit: Color,
    val litPressed: Color,
    val onLit: Color,
    /** Orange rather than red, so a problem can't be mistaken for the right ear. */
    val danger: Color,
    val leftEar: Color,
    /** Also dashed on graphs, so colour isn't the only difference between the ears. */
    val rightEar: Color,
)

/** Warm graphite, where amber means one thing: lit or active, like a glowing meter. */
val DarkColors = ThemeColors(
    background = Color(0xFF16140F),
    bar = Color(0xFF0D0C09),
    strip = Color(0xFF174035),
    stripDim = Color(0xFFBFD3C9),
    stripText = Color(0xFFEEEEEA),
    stripOn = Color(0xFFFFC24A),
    stripDanger = Color(0xFFFF9A5C),
    stripPanel = Color(0xFF1F4B3F),
    surface = Color(0xFF211E18),
    surfaceHigh = Color(0xFF2F2B23),
    tooltip = Color(0xFF38332A),
    line = Color(0xFF36322A),
    outline = Color(0xFF958D7E),
    track = Color(0xFF7A7363),
    text = Color(0xFFF3EFE6),
    textDim = Color(0xFFBBB3A5),
    lit = Color(0xFFFFC24A),
    litPressed = Color(0xFFE0A53A),
    onLit = Color(0xFF2A1C00),
    danger = Color(0xFFFF9A5C),
    leftEar = Color(0xFF62B2F2),
    rightEar = Color(0xFFF06B5C),
)

/**
 * The same instrument in daylight: a warm putty faceplate. Amber can't be read on a light page, so lit
 * controls are a deep burnt amber carrying cream legends. The strip's green stays, and the ear colours
 * deepen so they read as ink on a chart, as an audiogram does on paper.
 */
val LightColors = ThemeColors(
    background = Color(0xFFEDE7DA),
    bar = Color(0xFFDED6C5),
    strip = DarkColors.strip,
    stripDim = DarkColors.stripDim,
    stripText = DarkColors.stripText,
    stripOn = DarkColors.stripOn,
    stripDanger = DarkColors.stripDanger,
    stripPanel = DarkColors.stripPanel,
    surface = Color(0xFFF8F3E8),
    surfaceHigh = Color(0xFFE6DDCB),
    tooltip = Color(0xFFFFFDF7),
    line = Color(0xFFDAD1BF),
    outline = Color(0xFF7A7160),
    track = Color(0xFF857B67),
    text = Color(0xFF1D1A14),
    textDim = Color(0xFF574F42),
    lit = Color(0xFF8A4F00),
    litPressed = Color(0xFF6E3F00),
    onLit = Color(0xFFFFF4DC),
    danger = Color(0xFF8F3F00),
    leftEar = Color(0xFF0A5FAD),
    rightEar = Color(0xFFC4231A),
)

/**
 * The colours in use. These are read everywhere, including while drawing graphs, and reading one here
 * means that a theme change redraws whatever used it.
 */
object Palette {
    internal var colors by mutableStateOf(DarkColors)

    val Background get() = colors.background
    val Bar get() = colors.bar
    val Strip get() = colors.strip
    val StripDim get() = colors.stripDim
    val StripText get() = colors.stripText
    val StripOn get() = colors.stripOn
    val StripDanger get() = colors.stripDanger
    val StripPanel get() = colors.stripPanel
    val Surface get() = colors.surface
    val SurfaceHigh get() = colors.surfaceHigh
    val Tooltip get() = colors.tooltip
    val Line get() = colors.line
    val Outline get() = colors.outline
    val Track get() = colors.track
    val Text get() = colors.text
    val TextDim get() = colors.textDim
    val Lit get() = colors.lit
    val LitPressed get() = colors.litPressed
    val OnLit get() = colors.onLit
    val Danger get() = colors.danger
    val LeftEar get() = colors.leftEar
    val RightEar get() = colors.rightEar
}

private val DarkScheme = darkColorScheme(
    primary = DarkColors.lit,
    onPrimary = DarkColors.onLit,
    secondaryContainer = DarkColors.surfaceHigh,
    onSecondaryContainer = DarkColors.text,
    background = DarkColors.background,
    onBackground = DarkColors.text,
    surface = DarkColors.background,
    onSurface = DarkColors.text,
    surfaceVariant = DarkColors.surfaceHigh,
    onSurfaceVariant = DarkColors.textDim,
    surfaceContainer = DarkColors.surface,
    surfaceContainerLow = DarkColors.surface,
    surfaceContainerHigh = DarkColors.surfaceHigh,
    surfaceContainerHighest = DarkColors.surfaceHigh,
    inverseSurface = DarkColors.text,
    inverseOnSurface = DarkColors.background,
    inversePrimary = Color(0xFF7A5A1C),
    outline = DarkColors.outline,
    outlineVariant = DarkColors.line,
    error = DarkColors.danger,
)

private val LightScheme = lightColorScheme(
    primary = LightColors.lit,
    onPrimary = LightColors.onLit,
    secondaryContainer = LightColors.surfaceHigh,
    onSecondaryContainer = LightColors.text,
    background = LightColors.background,
    onBackground = LightColors.text,
    surface = LightColors.background,
    onSurface = LightColors.text,
    surfaceVariant = LightColors.surfaceHigh,
    onSurfaceVariant = LightColors.textDim,
    surfaceContainer = LightColors.surface,
    surfaceContainerLow = LightColors.surface,
    surfaceContainerHigh = LightColors.surfaceHigh,
    surfaceContainerHighest = LightColors.surfaceHigh,
    inverseSurface = LightColors.text,
    inverseOnSurface = LightColors.background,
    inversePrimary = Color(0xFFFFC24A),
    outline = LightColors.outline,
    outlineVariant = LightColors.line,
    error = LightColors.danger,
)

/** Whether this choice means dark right now. Follows the device live, including a dark mode schedule. */
@Composable
fun isDarkTheme(mode: ThemeMode): Boolean = when (mode) {
    ThemeMode.DEVICE -> isSystemInDarkTheme()
    ThemeMode.LIGHT -> false
    ThemeMode.DARK -> true
}

private val Body = FontFamily(
    Font(R.font.barlow_regular, FontWeight.Normal),
    Font(R.font.barlow_medium, FontWeight.Medium),
    Font(R.font.barlow_semibold, FontWeight.SemiBold),
)

private val Engraved = FontFamily(
    Font(R.font.barlow_semicondensed_medium, FontWeight.Medium),
    Font(R.font.barlow_semicondensed_semibold, FontWeight.SemiBold),
)

val ReadoutFamily = FontFamily(Font(R.font.jetbrains_mono))

private val Type = Typography(
    displaySmall = TextStyle(fontFamily = Engraved, fontSize = 38.sp, lineHeight = 44.sp, fontWeight = FontWeight.Medium),
    headlineMedium = TextStyle(fontFamily = Engraved, fontSize = 30.sp, lineHeight = 36.sp, fontWeight = FontWeight.SemiBold),
    headlineSmall = TextStyle(fontFamily = Engraved, fontSize = 26.sp, lineHeight = 32.sp, fontWeight = FontWeight.SemiBold),
    titleLarge = TextStyle(fontFamily = Engraved, fontSize = 22.sp, lineHeight = 28.sp, fontWeight = FontWeight.SemiBold),
    titleMedium = TextStyle(fontFamily = Body, fontSize = 16.sp, lineHeight = 24.sp, fontWeight = FontWeight.SemiBold),
    titleSmall = TextStyle(fontFamily = Body, fontSize = 14.sp, lineHeight = 20.sp, fontWeight = FontWeight.SemiBold),
    bodyLarge = TextStyle(fontFamily = Body, fontSize = 16.sp, lineHeight = 22.sp),
    bodyMedium = TextStyle(fontFamily = Body, fontSize = 14.sp, lineHeight = 20.sp),
    bodySmall = TextStyle(fontFamily = Body, fontSize = 12.sp, lineHeight = 16.sp),
    labelLarge = TextStyle(fontFamily = Body, fontSize = 14.sp, lineHeight = 20.sp, fontWeight = FontWeight.SemiBold),
    labelMedium = TextStyle(fontFamily = Engraved, fontSize = 12.sp, lineHeight = 16.sp, fontWeight = FontWeight.Medium),
    labelSmall = TextStyle(fontFamily = Engraved, fontSize = 11.sp, lineHeight = 14.sp, fontWeight = FontWeight.Medium),
)

/** A panel's engraved legend. */
val PanelLegend = TextStyle(
    fontFamily = Engraved,
    fontSize = 13.sp,
    lineHeight = 18.sp,
    fontWeight = FontWeight.SemiBold,
    letterSpacing = 1.2.sp,
)

/** A tool's main measurement, large enough to read at arm's length. */
val BigReadout = TextStyle(fontFamily = ReadoutFamily, fontSize = 38.sp, lineHeight = 44.sp)

/** Monospaced digits, so measurements stay still while their values change. */
val Readout = TextStyle(fontFamily = ReadoutFamily, fontSize = 13.sp, lineHeight = 18.sp)

private val Corners = Shapes(
    extraSmall = RoundedCornerShape(4.dp),
    small = RoundedCornerShape(6.dp),
    medium = RoundedCornerShape(8.dp),
    large = RoundedCornerShape(10.dp),
    extraLarge = RoundedCornerShape(12.dp),
)

@Composable
fun EigentoneTheme(dark: Boolean, content: @Composable () -> Unit) {
    val colors = if (dark) DarkColors else LightColors
    // Set before anything below reads it, so a change can't show one frame of the old theme.
    if (Palette.colors != colors) Palette.colors = colors
    MaterialTheme(colorScheme = if (dark) DarkScheme else LightScheme, typography = Type, shapes = Corners, content = content)
}
