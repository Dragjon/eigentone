package io.github.dragjon.eigentone.ui

import androidx.compose.ui.graphics.Color
import io.github.dragjon.eigentone.data.AppState
import io.github.dragjon.eigentone.data.ThemeMode
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
        for (item in listOf(t.stripText, t.stripOn, t.stripDim, t.stripDanger)) {
            check("strip", item, t.strip, 4.5)
            check("opened strip", item, t.stripPanel, 4.5)
        }
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
    fun theStripIsGreenInTheDarkAndWarmInDaylight() {
        assertEquals(Color(0xFF174035), DarkColors.strip)
        // In daylight it's a warm band a shade deeper than the page, with no green or blue cast.
        val light = LightColors.strip
        assertTrue(light.red >= light.green && light.green >= light.blue)
        assertTrue(luminance(light) < luminance(LightColors.background))
    }

    @Test
    fun amberIsTheLitColourAndTheEarsKeepTheirs() {
        assertEquals(Color(0xFFFFC24A), DarkColors.lit)
        assertEquals(Color(0xFF62B2F2), DarkColors.leftEar)
        assertEquals(Color(0xFFF06B5C), DarkColors.rightEar)
        assertEquals(Color(0xFFFF9A5C), DarkColors.danger)
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
