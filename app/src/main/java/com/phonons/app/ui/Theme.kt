package com.phonons.app.ui

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.phonons.app.R

/**
 * An instrument panel: graphite enamel, with ivory that means one thing, lit or active. Blue and red
 * are reserved for the left and right ear, as on an audiogram, so colour always carries meaning.
 */
object Palette {
    val Background = Color(0xFF141516)
    /** The top and bottom bars, a step darker than the page so they read as fixed. */
    val Bar = Color(0xFF0B0C0D)
    /** Behind the status strip, so it stands apart from both the bars and the page. */
    val Strip = Color(0xFF174035)
    /** Things that are off, on the strip's green. */
    val StripDim = Color(0xFFBFD3C9)
    val Surface = Color(0xFF1E1F20)
    val SurfaceHigh = Color(0xFF2B2D2E)
    val Tooltip = Color(0xFF333536)
    /** Hairlines between rows. Decorative, so they sit below control contrast on purpose. */
    val Line = Color(0xFF323435)
    /** Control boundaries such as off switches and segments. At least 3:1 against every surface. */
    val Outline = Color(0xFF8A8C8E)
    val Track = Color(0xFF6C6F71)
    val Text = Color(0xFFEEEEEA)
    val TextDim = Color(0xFFB0B2B2)
    val Lit = Color(0xFFF2E8CF)
    val OnLit = Color(0xFF1E1A10)
    /** Orange rather than red, so a problem can't be mistaken for the right ear. */
    val Danger = Color(0xFFFF9A5C)
    val LeftEar = Color(0xFF62B2F2)
    /** Also dashed on graphs, so colour isn't the only difference between the ears. */
    val RightEar = Color(0xFFF06B5C)
}

private val Colors = darkColorScheme(
    primary = Palette.Lit,
    onPrimary = Palette.OnLit,
    secondaryContainer = Palette.SurfaceHigh,
    onSecondaryContainer = Palette.Text,
    background = Palette.Background,
    onBackground = Palette.Text,
    surface = Palette.Background,
    onSurface = Palette.Text,
    surfaceVariant = Palette.SurfaceHigh,
    onSurfaceVariant = Palette.TextDim,
    surfaceContainer = Palette.Surface,
    surfaceContainerLow = Palette.Surface,
    surfaceContainerHigh = Palette.SurfaceHigh,
    surfaceContainerHighest = Palette.SurfaceHigh,
    inverseSurface = Palette.Text,
    inverseOnSurface = Palette.Background,
    inversePrimary = Color(0xFF6B6147),
    outline = Palette.Outline,
    outlineVariant = Palette.Line,
    error = Palette.Danger,
)

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
fun PhononsTheme(content: @Composable () -> Unit) {
    MaterialTheme(colorScheme = Colors, typography = Type, shapes = Corners, content = content)
}
