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

object Palette {
    val Background = Color(0xFF0F1113)
    val Surface = Color(0xFF181B1F)
    val SurfaceHigh = Color(0xFF22262B)
    val Line = Color(0xFF2D3238)
    val Text = Color(0xFFECEEF0)
    val TextDim = Color(0xFF9AA2AB)
    val Amber = Color(0xFFFFB454)
    val OnAmber = Color(0xFF1E1404)
    val Danger = Color(0xFFFF7A70)
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
    surfaceVariant = Palette.Surface,
    onSurfaceVariant = Palette.TextDim,
    surfaceContainer = Palette.Surface,
    surfaceContainerHigh = Palette.SurfaceHigh,
    surfaceContainerHighest = Palette.SurfaceHigh,
    outline = Palette.Line,
    outlineVariant = Palette.Line,
    error = Palette.Danger,
)

private val Type = Typography(
    headlineSmall = TextStyle(fontSize = 24.sp, fontWeight = FontWeight.SemiBold),
    titleMedium = TextStyle(fontSize = 16.sp, fontWeight = FontWeight.Medium),
    bodyMedium = TextStyle(fontSize = 14.sp, lineHeight = 20.sp),
    bodySmall = TextStyle(fontSize = 12.sp, lineHeight = 16.sp),
    labelMedium = TextStyle(fontSize = 12.sp, fontWeight = FontWeight.Medium),
)

/** Tabular digits, so readouts stay still while their values change. */
val Readout = TextStyle(fontFamily = FontFamily.Monospace, fontSize = 13.sp)

@Composable
fun PhononsTheme(content: @Composable () -> Unit) {
    MaterialTheme(colorScheme = Colors, typography = Type, content = content)
}
