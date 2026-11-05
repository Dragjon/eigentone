package io.github.dragjon.eigentone.dsp

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.math.abs
import kotlin.math.max

class EqTest {
    private fun bell(freq: Double, gain: Double, q: Double = 1.0) =
        Filter(1, FilterType.BELL, freq, gain, q)

    @Test
    fun bellHitsItsGainAtCentreAndIsFlatFarAway() {
        val f = bell(1000.0, 6.0)
        assertEquals(6.0, Eq.filterDb(f, 1000.0), 0.01)
        assertTrue(abs(Eq.filterDb(f, 20.0)) < 0.05)
    }

    @Test
    fun cutIsTheExactInverseOfBoost() {
        for (freq in listOf(100.0, 1000.0, 8000.0)) {
            val sum = Eq.filterDb(bell(1000.0, 9.0), freq) + Eq.filterDb(bell(1000.0, -9.0), freq)
            assertEquals(0.0, sum, 1e-9)
        }
    }

    @Test
    fun shelvesReachFullGainOnTheirSide() {
        val low = Filter(1, FilterType.LOW_SHELF, 200.0, 8.0, 0.71)
        assertEquals(8.0, Eq.filterDb(low, 20.0), 0.1)
        assertEquals(0.0, Eq.filterDb(low, 15000.0), 0.1)
        val high = Filter(1, FilterType.HIGH_SHELF, 5000.0, -6.0, 0.71)
        assertEquals(0.0, Eq.filterDb(high, 50.0), 0.1)
        assertEquals(-6.0, Eq.filterDb(high, 20000.0), 0.3)
    }

    @Test
    fun passFiltersAreMinusThreeDbAtCutoffWithButterworthQ() {
        val q = 0.7071
        assertEquals(-3.01, Eq.filterDb(Filter(1, FilterType.LOW_PASS, 1000.0, q = q), 1000.0), 0.02)
        assertEquals(-3.01, Eq.filterDb(Filter(1, FilterType.HIGH_PASS, 1000.0, q = q), 1000.0), 0.02)
        assertTrue(Eq.filterDb(Filter(1, FilterType.HIGH_PASS, 1000.0, q = q), 100.0) < -35.0)
    }

    @Test
    fun disabledFilterHasNoEffect() {
        assertEquals(0.0, Eq.filterDb(bell(1000.0, 12.0).copy(enabled = false), 1000.0), 0.0)
    }

    @Test
    fun filtersStackInDecibels() {
        val a = bell(100.0, 3.0)
        val b = bell(5000.0, 4.0).copy(id = 2)
        val expected = Eq.filterDb(a, 700.0) + Eq.filterDb(b, 700.0)
        assertEquals(expected, Eq.curveDb(listOf(a, b), 700.0), 1e-12)
    }

    @Test
    fun autoHeadroomLeavesNoRoomForClipping() {
        val filters = listOf(bell(1000.0, 8.0))
        val input = Eq.inputGainDb(filters, preampDb = 0.0, autoHeadroom = true)
        assertEquals(-8.0, input, 0.05)
        assertEquals(0.0, Eq.inputGainDb(filters, 0.0, autoHeadroom = false), 0.0)
        // A preamp that already provides enough headroom is left alone.
        assertEquals(-10.0, Eq.inputGainDb(filters, -10.0, autoHeadroom = true), 0.0)
        // No boost means no reduction.
        assertEquals(0.0, Eq.inputGainDb(listOf(bell(1000.0, -8.0)), 0.0, autoHeadroom = true), 0.05)
    }

    @Test
    fun bandsAreOrderedAndCoverTheWholeSpectrum() {
        val mapper = CurveMapper(64)
        for (i in 1 until 64) assertTrue(mapper.cutoffs[i] > mapper.cutoffs[i - 1])
        assertEquals(CurveMapper.HIGH_EDGE, mapper.cutoffs.last().toDouble(), 1.0)
    }

    /** How closely the sampled bands follow the true curve, away from the steep extremes. */
    private fun worstError(bandCount: Int, filters: List<Filter>): Double {
        val mapper = CurveMapper(bandCount)
        val gains = mapper.gains(filters)
        var worst = 0.0
        for (i in 0..2000) {
            val freq = Eq.logFreq(i / 2000.0)
            val band = mapper.cutoffs.indexOfFirst { freq <= it }
            worst = max(worst, abs(gains[band] - Eq.curveDb(filters, freq)))
        }
        return worst
    }

    @Test
    fun bandCountsTrackTypicalCurvesClosely() {
        val typical = listOf(
            Filter(1, FilterType.LOW_SHELF, 105.0, 5.0, 0.7),
            bell(250.0, -3.0, 1.2).copy(id = 2),
            bell(3000.0, 4.0, 2.0).copy(id = 3),
            Filter(4, FilterType.HIGH_SHELF, 9000.0, -2.5, 0.7),
        )
        val errors = listOf(16, 32, 64, 128, 256).associateWith { worstError(it, typical) }
        println("worst band error dB: $errors")
        // Worst case is at a band edge on the steepest slope, and halves as bands double.
        assertTrue("128 bands: ${errors[128]}", errors.getValue(128) < 0.3)
        assertTrue("64 bands: ${errors[64]}", errors.getValue(64) < 0.6)
    }

    @Test
    fun parsesAutoEqFile() {
        val text = """
            Preamp: -6.1 dB
            Filter 1: ON LSC Fc 105 Hz Gain 2.5 dB Q 0.70
            Filter 2: ON PK Fc 1,250 Hz Gain -3.2 dB Q 1.41
            Filter 3: OFF HSC Fc 10000 Hz Gain -1.0 dB Q 0.70
            Filter 4: ON HP Fc 30 Hz
            # a comment that should be ignored
        """.trimIndent()
        val parsed = AutoEqParser.parse(text)
        assertNotNull(parsed)
        assertEquals(-6.1, parsed!!.preampDb, 0.0)
        assertEquals(4, parsed.filters.size)
        assertEquals(FilterType.LOW_SHELF, parsed.filters[0].type)
        assertEquals(1250.0, parsed.filters[1].freq, 0.0)
        assertEquals(-3.2, parsed.filters[1].gainDb, 0.0)
        assertEquals(false, parsed.filters[2].enabled)
        assertEquals(FilterType.HIGH_PASS, parsed.filters[3].type)
        assertEquals(0.71, parsed.filters[3].q, 0.0)
    }

    @Test
    fun rejectsTextWithNoFilters() {
        assertNull(AutoEqParser.parse("Preamp: -3 dB\nhello"))
        assertNull(AutoEqParser.parse(""))
    }

    @Test
    fun parserClampsOutOfRangeValues() {
        val parsed = AutoEqParser.parse("Filter 1: ON PK Fc 90000 Hz Gain 99 dB Q 500")!!
        val f = parsed.filters.single()
        assertEquals(Eq.MAX_FREQ, f.freq, 0.0)
        assertEquals(30.0, f.gainDb, 0.0)
        assertEquals(Eq.MAX_Q, f.q, 0.0)
    }

    @Test
    fun largeOpposingImportedFiltersKeepTheirNetCurve() {
        val parsed = AutoEqParser.parse(
            """
            Filter 1: ON PK Fc 5504 Hz Gain -20.0 dB Q 0.77
            Filter 2: ON PK Fc 4778 Hz Gain 17.4 dB Q 0.67
            """.trimIndent(),
        )!!
        assertEquals(-20.0, parsed.filters[0].gainDb, 0.0)
        assertEquals(17.4, parsed.filters[1].gainDb, 0.0)
        // Matches the published profile's net response, which an 18 dB clamp shifted by 2 dB.
        assertTrue(abs(Eq.curveDb(parsed.filters, 5508.0)) < 4.0)
    }

    @Test
    fun liberty4NcPresetsMatchTheirSourceProfiles() {
        val byId = io.github.dragjon.eigentone.data.BuiltIns.all.associateBy { it.id }
        val main = byId.getValue("builtin:liberty4nc")
        val bass = byId.getValue("builtin:liberty4nc-bass")
        val rtings = byId.getValue("builtin:liberty4nc-rtings")
        assertEquals(10, main.filters.size)
        assertEquals(-6.3, Eq.curveDb(main.filters, 100.0), 0.1)
        assertEquals(3.3, Eq.curveDb(main.filters, 1500.0), 0.1)
        assertEquals(-3.6, Eq.curveDb(rtings.filters, 5500.0), 0.1)
        // The bass variant keeps the upper-range correction while leaving the low end alone.
        assertTrue(abs(Eq.curveDb(bass.filters, 60.0)) < 1.0)
        assertEquals(Eq.curveDb(main.filters, 3500.0), Eq.curveDb(bass.filters, 3500.0), 0.3)
    }

    @Test
    fun libertyPresetsOnlyAppearWhenThoseEarbudsAreConnected() {
        val builtIns = io.github.dragjon.eigentone.data.BuiltIns
        fun libertyCount(names: List<String>) = builtIns.availableFor(names).count { "Liberty" in it.name }

        assertEquals(3, libertyCount(listOf("SM-A546E", "soundcore Liberty 4 NC")))
        assertEquals(3, libertyCount(listOf("Bluetooth:soundcore Liberty 4 NC")))
        assertEquals(0, libertyCount(listOf("SM-A546E", "Other Buds")))
        assertEquals(0, libertyCount(listOf("speaker")))
        assertEquals(0, libertyCount(emptyList()))
        assertEquals(builtIns.all.size - 3, builtIns.availableFor(emptyList()).size)
        // Hidden isn't deleted, so an output already assigned to one can still load it.
        assertEquals(3, builtIns.all.count { "Liberty" in it.name })
    }

    @Test
    fun outputNamesFindTheirCorrectionPreset() {
        val find = io.github.dragjon.eigentone.data.BuiltIns::correctionFor
        assertEquals("builtin:liberty4nc", find("Bluetooth:soundcore Liberty 4 NC")?.id)
        assertNull(find("Bluetooth:Other Buds"))
        assertNull(find("speaker"))
    }
}
