package io.github.dragjon.eigentone.data

import io.github.dragjon.eigentone.dsp.GraphicEq
import io.github.dragjon.eigentone.ui.SeedScheme
import io.github.dragjon.eigentone.ui.notchStep
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class Release042Test {
    @Test
    fun `a fader on a preset that cuts can still reach the top`() {
        val base = listOf(-10.0)
        val change = FaderLayer.change(base, before = listOf(-10.0), current = listOf(0.0), moved = listOf(GraphicEq.MAX_DB))
        assertEquals(25.0, change[0], 0.0)
        assertEquals(GraphicEq.MAX_DB, FaderLayer.shown(base, change)[0], 0.0)
    }

    @Test
    fun `a fader on a preset that boosts can still reach the bottom`() {
        val base = listOf(8.0)
        val change = FaderLayer.change(base, before = listOf(8.0), current = listOf(0.0), moved = listOf(-GraphicEq.MAX_DB))
        assertEquals(-GraphicEq.MAX_DB, FaderLayer.shown(base, change)[0], 0.0)
    }

    @Test
    fun `slider notches fall on round steps`() {
        assertEquals(5f, notchStep(-12f..12f), 1e-6f)
        assertEquals(0.2f, notchStep(0f..1f), 1e-6f)
        assertEquals(50f, notchStep(30f..250f), 1e-6f)
    }

    @Test
    fun `device settings say what changed and put it back`() {
        val before = AppState()
        val after = before.copy(autoSwitch = false, loudness = true)
        val saved = DeviceSettings.of(before)
        assertEquals(listOf(DeviceSettings.Part.AUTO_SWITCH, DeviceSettings.Part.QUIET), DeviceSettings.of(after).changedParts(saved))
        assertEquals(saved, DeviceSettings.of(saved.applyTo(after)))
    }

    @Test
    fun `seed schemes keep text readable on the page`() {
        for (seed in SeedScheme.SWATCHES) {
            for (dark in listOf(false, true)) {
                val s = SeedScheme.scheme(seed, dark)
                assertTrue("seed ${seed.toString(16)} dark=$dark", contrast(s.onSurface, s.surface) >= 7.0)
                assertTrue("seed ${seed.toString(16)} dark=$dark", contrast(s.onPrimary, s.primary) >= 4.5)
            }
        }
    }

    private fun contrast(a: androidx.compose.ui.graphics.Color, b: androidx.compose.ui.graphics.Color): Double {
        val la = a.luminance() + 0.05
        val lb = b.luminance() + 0.05
        return maxOf(la, lb) / minOf(la, lb)
    }

    private fun androidx.compose.ui.graphics.Color.luminance(): Double {
        fun lin(c: Float) = if (c <= 0.03928f) c / 12.92 else Math.pow((c + 0.055) / 1.055, 2.4)
        return 0.2126 * lin(red) + 0.7152 * lin(green) + 0.0722 * lin(blue)
    }
}
