package io.github.dragjon.eigentone.data

import io.github.dragjon.eigentone.dsp.Eq
import io.github.dragjon.eigentone.dsp.Filter
import io.github.dragjon.eigentone.dsp.FilterChannel
import io.github.dragjon.eigentone.dsp.FilterType
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Test

class ChannelFilterTest {
    private val both = Filter(1, FilterType.BELL, 1000.0, 4.0, 1.0)
    private val leftOnly = Filter(2, FilterType.BELL, 3000.0, -5.0, 2.0, channel = FilterChannel.LEFT)
    private val rightOnly = Filter(3, FilterType.HIGH_SHELF, 8000.0, 3.0, 0.7, channel = FilterChannel.RIGHT)

    private fun state(vararg filters: Filter) =
        AppState(enabled = true, current = Preset("user:x", "X", filters = filters.toList()))

    @Test
    fun aFilterOnOneSideOnlyShapesThatSide() {
        val target = Processing.target(state(both, leftOnly, rightOnly), 0.0, outputKey = "Wired:Wired")
        listOf(200.0, 1000.0, 3000.0, 9000.0).forEach { hz ->
            assertEquals(Eq.curveDb(listOf(both, leftOnly), hz), Eq.curveDb(target.filters, hz), 1e-9)
            assertEquals(Eq.curveDb(listOf(both, rightOnly), hz), Eq.curveDb(target.rightFilters, hz), 1e-9)
        }
    }

    @Test
    fun bothSidesAloneNeedNoSplit() {
        assertNull(Processing.channelFilters(state(both), "Wired:Wired", Processing.presetFilters(state(both).current)))
        assertNotNull(Processing.channelFilters(state(both, leftOnly), "Wired:Wired", Processing.presetFilters(state(both, leftOnly).current)))
    }

    @Test
    fun oneSidedFiltersFollowTheStrength() {
        val s = state(leftOnly).let { it.copy(current = it.current.copy(tone = Tone(strength = 0.5))) }
        val (left, right) = Processing.sidedFilters(s.current)
        assertEquals(-2.5, left.single().gainDb, 1e-9)
        assertEquals(0, right.size)
    }

    @Test
    fun aPresetWithOneSidedFiltersIsntExportedAsFilters() {
        assertNull(Processing.exportFilters(state(both, leftOnly).current))
        assertNotNull(Processing.exportFilters(state(both).current))
    }

    @Test
    fun theSharedSoundLeavesOneSidedFiltersOut() {
        val shared = Processing.presetFilters(state(both, leftOnly, rightOnly).current)
        assertEquals(listOf(both), shared)
    }
}
