package io.github.dragjon.eigentone.data

import io.github.dragjon.eigentone.dsp.GraphicEq
import io.github.dragjon.eigentone.ui.faderValue
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertNull
import org.junit.Test

class BandSwitchTest {
    /** Jagged on purpose, so any resampling would visibly blur it. */
    private fun jagged(bands: Int) = List(bands) { if (it % 2 == 0) 4.5 else -3.0 }
    private fun tone(bands: Int) = Tone(graphicDb = jagged(bands), graphicBands = bands)

    @Test
    fun everyFinerLayoutSurvivesARoundTripUnchanged() {
        for ((fine, coarse) in listOf(31 to 10, 31 to 15, 15 to 10)) {
            val back = BandSwitch.switch(BandSwitch.switch(tone(fine), coarse), fine)
            assertEquals("$fine→$coarse→$fine", jagged(fine), back.graphicDb)
            assertNull(back.graphicDetail)
        }
    }

    @Test
    fun aChainOfSwitchesStillComesBack() {
        val t = tone(31)
        val back = listOf(15, 10, 15, 31).fold(t) { acc, n -> BandSwitch.switch(acc, n) }
        assertEquals(t.graphicDb, back.graphicDb)
        assertEquals(t, back)
    }

    @Test
    fun onlyTheMovedPartLosesDetail() {
        val coarse = BandSwitch.switch(tone(31), 10)
        // Raise the top coarse fader (16 kHz) only.
        val edited = coarse.copy(graphicDb = coarse.graphicDb.toMutableList().also { it[9] = 10.0 })
        val back = BandSwitch.switch(edited, 31)
        val fine = jagged(31)
        val freqs = GraphicEq.frequencies(31)
        // Everything well below the moved fader is exactly as it was.
        freqs.indices.filter { freqs[it] <= 4000.0 }.forEach { assertEquals(fine[it], back.graphicDb[it], 0.0) }
        // The top is the coarse fader's new level.
        assertEquals(10.0, back.graphicDb.last(), 0.0)
        assertNotEquals(fine.last(), back.graphicDb.last())
    }

    @Test
    fun aResetBringsNothingBack() {
        val coarse = BandSwitch.switch(tone(31), 10)
        val back = BandSwitch.switch(coarse.copy(graphicDb = List(10) { 0.0 }), 31)
        assertEquals(List(31) { 0.0 }, back.graphicDb)
    }

    @Test
    fun aNewShapeDropsTheDetail() {
        val coarse = BandSwitch.switch(tone(31), 10)
        assertNull(coarse.withShape("Warm", List(10) { 1.0 }, 10).graphicDetail)
    }

    @Test
    fun faderValuesAreShort() {
        assertEquals("+3.5", faderValue(3.5, true))
        assertEquals("-2", faderValue(-2.0, true))
        assertEquals("0", faderValue(0.2, true))
        assertEquals("+4", faderValue(3.5, false))
        assertEquals("0", faderValue(-0.4, false))
    }
}
