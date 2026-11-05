package io.github.dragjon.eigentone.data

import io.github.dragjon.eigentone.dsp.Filter
import io.github.dragjon.eigentone.dsp.FilterChannel
import io.github.dragjon.eigentone.dsp.FilterDynamics
import io.github.dragjon.eigentone.dsp.FilterType
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class DynamicFilterTest {
    private val sibilance = Filter(1, FilterType.BELL, 6000.0, -2.0, 2.0, dynamic = FilterDynamics(-36.0, 4.0))
    private val boom = Filter(2, FilterType.LOW_SHELF, 90.0, 0.0, 0.7, dynamic = FilterDynamics(-20.0, 2.0))

    private fun tamersFor(vararg filters: Filter, tone: Tone = Tone()) =
        Processing.target(AppState(enabled = true, current = Preset("user:x", "X", filters = filters.toList(), tone = tone)), 0.0).tamers

    @Test
    fun aDynamicBellCompressesOnlyAroundItsFrequency() {
        val plan = Dynamics.plan(Compressor(), tamersFor(sibilance))
        assertEquals(Dynamics.FULL_BANDS, plan.size)
        val range = sibilance.dynamicRange()
        val active = plan.withIndex().filter { it.value.active }
        assertEquals(1, active.size)
        val (i, band) = active.single()
        val bottom = if (i == 0) 20.0 else plan[i - 1].cutoffHz
        assertEquals(range.start, bottom, 0.01)
        assertEquals(range.endInclusive, band.cutoffHz, 0.01)
        assertEquals(-36.0, band.band.thresholdDb, 0.0)
        assertEquals(4.0, band.band.ratio, 0.0)
    }

    @Test
    fun cutoffsStayInRisingOrder() {
        val plan = Dynamics.plan(Compressor(), tamersFor(boom, sibilance))
        assertTrue(plan.zipWithNext().all { (a, b) -> a.cutoffHz < b.cutoffHz })
        assertEquals(setOf(1, 2), Dynamics.placedIds(Compressor(), tamersFor(boom, sibilance)))
    }

    @Test
    fun aDynamicFilterWaitsWhenTheBandsAreTaken() {
        val busy = tamersFor(sibilance, tone = Tone(harshTamer = 0.5, boomTamer = 0.5))
        val compressor = Compressor(enabled = true)
        assertFalse(1 in Dynamics.placedIds(compressor, busy))
        // Without the compressor running, its crossovers free up room.
        assertTrue(1 in Dynamics.placedIds(Compressor(), busy))
    }

    @Test
    fun onlyBellsAndShelvesOnBothChannelsCanBeDynamic() {
        assertFalse(sibilance.copy(channel = FilterChannel.LEFT).canBeDynamic)
        assertFalse(sibilance.copy(type = FilterType.NOTCH).canBeDynamic)
        assertTrue(Processing.dynamicRanges(Preset("user:x", "X", filters = listOf(sibilance.copy(channel = FilterChannel.LEFT)))).isEmpty())
    }

    @Test
    fun aPhoneWithOnlyBasicBandsRunsNone() {
        assertTrue(Dynamics.plan(Compressor(), tamersFor(sibilance), Dynamics.BASIC_BANDS).none { it.active })
    }

    @Test
    fun anOffOrFixedFilterAddsNothing() {
        assertTrue(Dynamics.plan(Compressor(), tamersFor(sibilance.copy(enabled = false))).none { it.active })
        assertTrue(Dynamics.plan(Compressor(), tamersFor(sibilance.copy(dynamic = null))).none { it.active })
    }
}
