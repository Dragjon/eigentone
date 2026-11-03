package com.phonons.app.ui

import androidx.compose.ui.graphics.Color
import com.phonons.app.data.AppState
import com.phonons.app.data.ThemeMode
import kotlin.math.max
import kotlin.math.min
import kotlin.math.pow
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class ThemeColorsTest {
    private fun channel(value: Float): Double =
        if (value <= 0.03928f) value / 12.92 else ((value + 0.055) / 1.055).pow(2.4)

    private fun luminance(c: Color) = 0.2126 * channel(c.red) + 0.7152 * channel(c.green) + 0.0722 * channel(c.blue)

    private fun contrast(a: Color, b: Color): Double {
        val x = luminance(a)
        val y = luminance(b)
        return (max(x, y) + 0.05) / (min(x, y) + 0.05)
    }

    private fun check(name: String, foreground: Color, background: Color, needed: Double) {
        val got = contrast(foreground, background)
        assertTrue("$name is only ${"%.2f".format(got)}:1, needs $needed", got >= needed)
    }

    private fun checkTheme(t: ThemeColors) {
        val surfaces = listOf(t.background, t.bar, t.surface, t.surfaceHigh, t.tooltip)
        for (surface in surfaces) {
            check("text", t.text, surface, 4.5)
            check("dim text", t.textDim, surface, 4.5)
            check("lit readout", t.lit, surface, 4.5)
            check("danger", t.danger, surface, 4.5)
        }
        for (surface in listOf(t.surface, t.background, t.bar)) {
            check("left ear", t.leftEar, surface, 3.0)
            check("right ear", t.rightEar, surface, 3.0)
        }
        for (surface in listOf(t.background, t.surface)) {
            check("rail", t.track, surface, 3.0)
            check("outline", t.outline, surface, 3.0)
        }
        check("key legend", t.onLit, t.lit, 4.5)
        check("pressed key legend", t.onLit, t.litPressed, 4.5)
        for (item in listOf(t.stripText, t.stripOn, t.stripDim, t.stripDanger)) check("strip", item, t.strip, 4.5)
    }

    @Test
    fun theDarkThemeIsReadable() = checkTheme(DarkColors)

    @Test
    fun theLightThemeIsReadable() = checkTheme(LightColors)

    @Test
    fun railsCanBeSeenInLightDialogs() {
        check("rail", LightColors.track, LightColors.tooltip, 3.0)
    }

    @Test
    fun barsAreDarkerThanThePageInBothThemes() {
        for (t in listOf(DarkColors, LightColors)) assertTrue(luminance(t.bar) < luminance(t.background))
    }

    @Test
    fun theStripKeepsItsGreenInBothThemes() {
        assertEquals(Color(0xFF174035), DarkColors.strip)
        assertEquals(DarkColors.strip, LightColors.strip)
    }

    @Test
    fun theDarkThemeKeepsItsOriginalColours() {
        val t = DarkColors
        assertEquals(
            listOf(0xFF141516, 0xFF0B0C0D, 0xFF1E1F20, 0xFF2B2D2E, 0xFF333536, 0xFF323435, 0xFF8A8C8E, 0xFF6C6F71, 0xFFEEEEEA, 0xFFB0B2B2, 0xFFF2E8CF, 0xFFD6CCB4, 0xFF1E1A10, 0xFFFF9A5C, 0xFF62B2F2, 0xFFF06B5C).map { Color(it) },
            listOf(t.background, t.bar, t.surface, t.surfaceHigh, t.tooltip, t.line, t.outline, t.track, t.text, t.textDim, t.lit, t.litPressed, t.onLit, t.danger, t.leftEar, t.rightEar),
        )
    }

    @Test
    fun followingTheDeviceIsTheDefault() {
        assertEquals(ThemeMode.DEVICE, AppState().themeMode)
    }

    @Test
    fun resettingTheSoundLeavesTheThemeAlone() {
        assertEquals(ThemeMode.LIGHT, AppState(themeMode = ThemeMode.LIGHT).resetSound().themeMode)
    }
}
