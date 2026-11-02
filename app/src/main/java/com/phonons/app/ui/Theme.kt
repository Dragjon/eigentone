package com.phonons.app.ui

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp

/**
 * Amber means one thing: active. It marks selected items, changed values and primary actions, so it
 * stays meaningful. Neutral text and controls carry everything else.
 */
object Palette {
    val Background = Color(0xFF0F1113)
    val Surface = Color(0xFF191C20)
    val SurfaceHigh = Color(0xFF262A2F)
    val Tooltip = Color(0xFF2E3238)
    /** Hairlines between rows. Decorative, so they sit below control contrast on purpose. */
    val Line = Color(0xFF2D3238)
    /** Control boundaries such as off switches and segments. At least 3:1 against every surface. */
    val Outline = Color(0xFF7D8690)
    val Track = Color(0xFF4A5159)
    val Text = Color(0xFFECEEF0)
    val TextDim = Color(0xFFA3ABB4)
    val Amber = Color(0xFFFFB454)
    val OnAmber = Color(0xFF1E1404)
    val Danger = Color(0xFFFF8A80)
}

private val Colors = darkColorScheme(
    primary = Palette.Amber,
    onPrimary = Palette.OnAmber,
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
    inversePrimary = Color(0xFF8A5300),
    outline = Palette.Outline,
    outlineVariant = Palette.Line,
    error = Palette.Danger,
)

private val Type = Typography(
    titleLarge = TextStyle(fontSize = 22.sp, lineHeight = 28.sp, fontWeight = FontWeight.SemiBold),
    titleMedium = TextStyle(fontSize = 16.sp, lineHeight = 24.sp, fontWeight = FontWeight.Medium),
    titleSmall = TextStyle(fontSize = 14.sp, lineHeight = 20.sp, fontWeight = FontWeight.Medium),
    bodyLarge = TextStyle(fontSize = 16.sp, lineHeight = 22.sp),
    bodyMedium = TextStyle(fontSize = 14.sp, lineHeight = 20.sp),
    bodySmall = TextStyle(fontSize = 12.sp, lineHeight = 16.sp),
    labelLarge = TextStyle(fontSize = 14.sp, lineHeight = 20.sp, fontWeight = FontWeight.Medium),
    labelMedium = TextStyle(fontSize = 12.sp, lineHeight = 16.sp, fontWeight = FontWeight.Medium),
)

/** Monospaced digits, so measurements stay still while their values change. */
val Readout = TextStyle(fontFamily = FontFamily.Monospace, fontSize = 13.sp, lineHeight = 18.sp)

@Composable
fun PhononsTheme(content: @Composable () -> Unit) {
    MaterialTheme(colorScheme = Colors, typography = Type, content = content)
}
