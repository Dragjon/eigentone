package io.github.dragjon.eigentone.dsp

import kotlinx.serialization.json.Json
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.math.abs

class FilterTypesTest {
    private fun pass(type: FilterType, slope: Int, q: Double = Eq.BUTTERWORTH_Q) = Filter(1, type, 1000.0, q = q, slope = slope)

    @Test
    fun twelveDbSlopeIsUnchanged() {
        val f = Filter(1, FilterType.HIGH_PASS, 120.0, q = 0.9)
        listOf(20.0, 60.0, 120.0, 500.0, 5000.0).forEach { hz ->
            assertEquals(Eq.biquad(f).magnitudeDb(hz, Eq.SAMPLE_RATE), Eq.filterDb(f, hz), 1e-9)
        }
    }

    @Test
    fun everySlopeIsThreeDbDownAtItsCorner() {
        for (slope in Eq.SLOPES) for (type in listOf(FilterType.HIGH_PASS, FilterType.LOW_PASS)) {
            assertEquals("$type $slope", -3.01, Eq.filterDb(pass(type, slope), 1000.0), 0.05)
        }
    }

    @Test
    fun eachSlopeFallsAsSteeplyAsItSays() {
        for (slope in Eq.SLOPES) {
            val f = pass(FilterType.HIGH_PASS, slope)
            // Two to three octaves below the corner, where the fall has settled.
            val perOctave = Eq.filterDb(f, 250.0) - Eq.filterDb(f, 125.0)
            assertEquals("slope $slope", slope.toDouble(), perOctave, 0.6)
            assertTrue(abs(Eq.filterDb(f, 16000.0)) < 0.05)
        }
    }

    @Test
    fun qAddsAPeakAtTheCornerOfASteepSlope() {
        val smooth = Eq.filterDb(pass(FilterType.LOW_PASS, 24), 900.0)
        val peaky = Eq.filterDb(pass(FilterType.LOW_PASS, 24, q = 3.0), 900.0)
        assertTrue(peaky > smooth + 3)
    }

    @Test
    fun notchRemovesItsFrequencyAndLeavesTheRest() {
        val f = Filter(1, FilterType.NOTCH, 1000.0, q = 5.0)
        assertTrue(Eq.filterDb(f, 1000.0) < -40)
        assertTrue(abs(Eq.filterDb(f, 200.0)) < 0.1)
        assertTrue(abs(Eq.filterDb(f, 5000.0)) < 0.1)
    }

    @Test
    fun bandPassKeepsOnlyItsRange() {
        val f = Filter(1, FilterType.BAND_PASS, 1000.0, q = 1.0)
        assertEquals(0.0, Eq.filterDb(f, 1000.0), 0.01)
        assertTrue(Eq.filterDb(f, 100.0) < -15)
        assertTrue(Eq.filterDb(f, 10000.0) < -15)
    }

    @Test
    fun tiltTurnsTheEndsOppositeWaysAndLeavesTheMiddle() {
        val f = Filter(1, FilterType.TILT, 1000.0, gainDb = 4.0, q = 0.5)
        assertEquals(0.0, Eq.filterDb(f, 1000.0), 0.01)
        assertEquals(4.0, Eq.filterDb(f, 20000.0), 0.3)
        assertEquals(-4.0, Eq.filterDb(f, 20.0), 0.3)
    }

    @Test
    fun steepSlopesAndTiltsExportAsFiltersThatSoundTheSame() {
        val filters = listOf(
            Filter(1, FilterType.HIGH_PASS, 40.0, q = 1.2, slope = 48),
            Filter(2, FilterType.TILT, 1500.0, gainDb = -3.0, q = 0.6),
            Filter(3, FilterType.NOTCH, 3000.0, q = 8.0),
            Filter(4, FilterType.BAND_PASS, 2000.0, q = 0.4),
            Filter(5, FilterType.LOW_PASS, 15000.0, q = 0.71, slope = 24),
        )
        assertTrue(filters.all(Eq::canExport))
        val parsed = AutoEqParser.parse(AutoEqParser.format(0.0, filters))!!
        listOf(25.0, 40.0, 100.0, 1500.0, 2900.0, 3000.0, 12000.0, 18000.0).forEach { hz ->
            assertEquals("at $hz Hz", Eq.curveDb(filters, hz), Eq.curveDb(parsed.filters, hz), 0.05)
        }
    }

    @Test
    fun oddSlopesCantBeWrittenToAFiltersFile() {
        assertFalse(Eq.canExport(pass(FilterType.HIGH_PASS, 6)))
        assertFalse(Eq.canExport(pass(FilterType.LOW_PASS, 18)))
        assertTrue(Eq.canExport(pass(FilterType.LOW_PASS, 36)))
    }

    @Test
    fun filtersSavedBeforeSlopesKeepTheirSound() {
        val old = Json { ignoreUnknownKeys = true }.decodeFromString(Filter.serializer(), """{"id":1,"type":"HIGH_PASS","freq":80.0,"q":0.7}""")
        assertEquals(12, old.slope)
    }
}
