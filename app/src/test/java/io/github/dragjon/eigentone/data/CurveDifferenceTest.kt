package io.github.dragjon.eigentone.data

import io.github.dragjon.eigentone.dsp.Filter
import io.github.dragjon.eigentone.dsp.FilterType
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class CurveDifferenceTest {
    private fun preset(vararg filters: Filter) = Preset("user:test", "Test", 0.0, filters.toList())

    @Test
    fun aBassBoostReadsAsMoreBassAndWarmer() {
        val flat = preset()
        val boosted = preset(Filter(1, FilterType.LOW_SHELF, freq = 120.0, gainDb = 6.0))
        val summary = CurveDifference.compare(flat, boosted)
        val first = summary.parts.first()
        assertTrue(first.region == CurveDifference.Region.DEEP_BASS || first.region == CurveDifference.Region.BASS)
        assertTrue(first.db > 3.0)
        assertTrue("tilt ${summary.tilt}", summary.tilt >= CurveDifference.WARMER_DB)
    }

    @Test
    fun aPresenceCutReadsAsLessPresence() {
        val flat = preset()
        val cut = preset(Filter(1, FilterType.BELL, freq = 3_000.0, gainDb = -5.0, q = 0.8))
        val summary = CurveDifference.compare(flat, cut)
        assertEquals(CurveDifference.Region.PRESENCE, summary.parts.first().region)
        assertTrue(summary.parts.first().db < -2.0)
    }

    @Test
    fun loudnessAloneIsNotADifference() {
        val flat = preset()
        val louder = preset(Filter(1, FilterType.LOW_SHELF, freq = 10.0, gainDb = 0.0)).copy(preampDb = 4.0)
        assertTrue(CurveDifference.compare(flat, louder).nearlyTheSame)
        assertTrue(CurveDifference.compare(flat, flat).nearlyTheSame)
    }
}
