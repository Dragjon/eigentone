package io.github.dragjon.eigentone.data

import io.github.dragjon.eigentone.dsp.Eq
import io.github.dragjon.eigentone.dsp.GraphicEq
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class FaderLayerTest {
    private val speaker = BuiltIns.all.first { it.id == "builtin:speaker" }

    @Test
    fun aPresetWithNoFiltersStartsFlat() {
        val base = FaderLayer.base(BuiltIns.flat, 10)
        assertFalse(FaderLayer.isShaped(base))
        assertEquals(List(10) { 0.0 }, FaderLayer.shown(base, emptyList()))
    }

    @Test
    fun thePresetsCurveShowsOnTheFaders() {
        val base = FaderLayer.base(speaker, 10)
        assertTrue(FaderLayer.isShaped(base))
        val freqs = GraphicEq.frequencies(10)
        val fixed = Processing.fixedFilters(speaker)
        val shown = FaderLayer.shown(base, emptyList())
        freqs.forEachIndexed { i, hz ->
            assertEquals(Eq.curveDb(fixed, hz).coerceIn(-GraphicEq.MAX_DB, GraphicEq.MAX_DB), shown[i], 1e-9)
        }
        // The high-pass cuts the lowest band further than a fader reaches, so it rests at the bottom.
        assertEquals(-GraphicEq.MAX_DB, shown[0], 0.0)
        assertTrue(FaderLayer.actual(base, emptyList(), 0) < -GraphicEq.MAX_DB)
    }

    @Test
    fun movingAFaderStoresOnlyTheChange() {
        val base = FaderLayer.base(speaker, 10)
        val before = FaderLayer.shown(base, emptyList())
        val moved = before.toMutableList().also { it[5] = GraphicEq.snap(before[5] + 3.0) }
        val change = FaderLayer.change(base, before, List(10) { 0.0 }, moved)
        assertEquals(moved[5], FaderLayer.shown(base, change)[5], 1e-9)
        assertEquals(List(9) { 0.0 }, change.filterIndexed { i, _ -> i != 5 })
    }

    @Test
    fun aFaderPutBackOnThePresetStoresNoChange() {
        val base = FaderLayer.base(speaker, 10)
        val raised = List(10) { if (it == 5) 2.0 else 0.0 }
        val before = FaderLayer.shown(base, raised)
        val back = before.toMutableList().also { it[5] = GraphicEq.snap(base[5]) }
        assertTrue(GraphicEq.isFlat(FaderLayer.change(base, before, raised, back)))
    }

    @Test
    fun faderAtTheEndOfItsTravelKeepsItsChange() {
        val base = FaderLayer.base(speaker, 10)
        val current = List(10) { if (it == 0) 4.0 else 0.0 }
        val before = FaderLayer.shown(base, current)
        val moved = before.toMutableList().also { it[6] = GraphicEq.snap(before[6] - 2.0) }
        assertEquals(4.0, FaderLayer.change(base, before, current, moved)[0], 0.0)
    }

    @Test
    fun thePresetStillSoundsTheSame() {
        val withFaders = Processing.presetFilters(speaker)
        val fixed = Processing.fixedFilters(speaker)
        listOf(50.0, 500.0, 3000.0, 12000.0).forEach { hz ->
            assertEquals(Eq.curveDb(fixed, hz), Eq.curveDb(withFaders, hz), 1e-9)
        }
    }
}
