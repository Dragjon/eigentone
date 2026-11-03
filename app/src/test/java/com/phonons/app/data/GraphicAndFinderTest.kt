package com.phonons.app.data

import com.phonons.app.dsp.Eq
import com.phonons.app.dsp.GraphicEq
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.math.abs
import kotlin.math.max

class GraphicAndFinderTest {
    /** The largest gap between what the faders show and the real combined response. */
    private fun worstError(gains: List<Double>, bands: Int): Double {
        val filters = GraphicEq.filters(gains, bands)
        return GraphicEq.frequencies(bands).indices.maxOf { i ->
            abs(Eq.curveDb(filters, GraphicEq.frequencies(bands)[i]) - gains[i])
        }
    }

    @Test
    fun bandLayoutsHaveTheAdvertisedCounts() {
        for (bands in GraphicEq.BAND_OPTIONS) {
            val freqs = GraphicEq.frequencies(bands)
            assertEquals(bands, freqs.size)
            assertEquals(freqs.sorted(), freqs)
        }
    }

    @Test
    fun allFadersUpLandsOnThatLevelRatherThanOvershooting() {
        for (bands in GraphicEq.BAND_OPTIONS) {
            val error = worstError(List(bands) { 6.0 }, bands)
            println("all faders +6 dB, $bands bands: worst error $error dB")
            assertTrue("$bands bands: $error", error < 0.6)
        }
    }

    @Test
    fun realisticCurvesLandCloseToTheFaders() {
        for (bands in GraphicEq.BAND_OPTIONS) {
            for (shape in GraphicEq.shapes.drop(1)) {
                val error = worstError(GraphicEq.apply(shape, bands), bands)
                println("${shape.name}, $bands bands: worst error $error dB")
                assertTrue("${shape.name} at $bands bands: $error", error < 0.6)
            }
        }
    }

    @Test
    fun theCurveFollowsTheFadersBetweenThemAndHoldsBeyondTheEnds() {
        val gains = listOf(9.0, 7.0, 5.0, 3.0, 1.0, -1.0, -2.0, -3.0, -4.0, -5.0)
        val filters = GraphicEq.filters(gains, 10)
        val freqs = GraphicEq.frequencies(10)
        // Halfway between faders on the log axis is halfway between their levels.
        val middle = kotlin.math.sqrt(freqs[2] * freqs[3])
        assertEquals((gains[2] + gains[3]) / 2, Eq.curveDb(filters, middle), 0.01)
        // No hook back towards zero past the first or last fader.
        assertEquals(gains.first(), Eq.curveDb(filters, 20.0), 0.0)
        assertEquals(gains.last(), Eq.curveDb(filters, 20000.0), 0.0)
    }

    @Test
    fun theCurveIsNeverSavedInsideAPresetsFilters() {
        val json = kotlinx.serialization.json.Json { encodeDefaults = true }
        val curve = GraphicEq.filters(List(10) { 2.0 }, 10).single()
        val text = json.encodeToString(com.phonons.app.dsp.Filter.serializer(), curve)
        assertFalse(text, text.contains("curve"))
    }

    @Test
    fun presetStrengthScalesTheWholePreset() {
        val tone = Tone(bassDb = 4.0, graphicDb = List(10) { 2.0 })
        val filter = com.phonons.app.dsp.Filter(1, com.phonons.app.dsp.FilterType.BELL, 1000.0, 6.0, 1.0)
        val preset = Preset("user:s", "S", filters = listOf(filter), tone = tone)
        val full = Eq.curveDb(Processing.presetFilters(preset), 1000.0)
        val half = Eq.curveDb(Processing.presetFilters(preset.copy(tone = tone.copy(strength = 0.5))), 1000.0)
        val off = Processing.presetFilters(preset.copy(tone = tone.copy(strength = 0.0)))
        assertEquals(full / 2, half, 0.05)
        assertEquals(0.0, Eq.curveDb(off, 1000.0), 0.0)
        assertEquals(0.0, Eq.curveDb(off, 40.0), 0.0)
    }

    @Test
    fun presetStrengthLeavesCutoffFiltersAlone() {
        val highPass = com.phonons.app.dsp.Filter(1, com.phonons.app.dsp.FilterType.HIGH_PASS, 100.0, q = 0.7)
        val preset = Preset("user:s", "S", filters = listOf(highPass), tone = Tone(strength = 0.0))
        assertEquals(Eq.curveDb(listOf(highPass), 30.0), Eq.curveDb(Processing.presetFilters(preset), 30.0), 0.0)
    }

    @Test
    fun renamingUpdatesTheSavedPresetAndTheWorkingCopy() {
        val preset = Preset("user:r", "Old")
        val state = AppState(current = preset, presets = listOf(preset)).renamedPreset("user:r", "New")
        assertEquals("New", state.presets.single().name)
        assertEquals("New", state.current.name)
        val other = AppState(presets = listOf(preset)).renamedPreset("user:r", "New")
        assertEquals("Flat", other.current.name)
    }

    @Test
    fun resetKeepsPresetsAndOutputsButClearsTheSound() {
        val preset = Preset("user:r", "Mine", tone = Tone(bassDb = 5.0))
        val before = AppState(
            enabled = true,
            current = preset,
            presets = listOf(preset),
            outputProfiles = mapOf("speaker" to "user:r"),
            nightMode = 0.8,
            balance = 0.4,
            loudness = true,
            bypass = true,
        )
        val after = before.resetSound()
        assertEquals(BuiltIns.flat, after.current)
        assertEquals(0.0, after.nightMode, 0.0)
        assertEquals(0.0, after.balance, 0.0)
        assertFalse(after.loudness)
        assertFalse(after.bypass)
        assertTrue(after.enabled)
        assertEquals(listOf(preset), after.presets)
        assertEquals("user:r", after.outputProfiles["speaker"])
    }

    @Test
    fun flatFadersAddNoFilters() {
        assertTrue(GraphicEq.filters(List(31) { 0.0 }, 31).isEmpty())
        assertTrue(GraphicEq.filters(emptyList(), 10).isEmpty())
    }

    @Test
    fun resamplingKeepsTheShape() {
        val ten = GraphicEq.apply(GraphicEq.shapes.first { it.name == "Bass boost" }, 10)
        val thirtyOne = GraphicEq.resample(ten, 10, 31)
        assertEquals(31, thirtyOne.size)
        assertTrue(thirtyOne.first() > 3.0)
        assertEquals(0.0, thirtyOne[GraphicEq.frequencies(31).indexOf(1000.0)], 0.0)
    }

    @Test
    fun oldNineBandSettingsAreRead() {
        val nine = List(9) { if (it == 4) 6.0 else 0.0 }
        val ten = GraphicEq.normalized(nine, 10)
        assertEquals(10, ten.size)
        assertEquals(6.0, ten[GraphicEq.frequencies(10).indexOf(1000.0)], 0.0)
    }

    @Test
    fun fadersSnapAndStayInRange() {
        assertEquals(3.5, GraphicEq.snap(3.4), 0.0)
        assertEquals(GraphicEq.MAX_DB, GraphicEq.snap(40.0), 0.0)
        assertEquals(-GraphicEq.MAX_DB, GraphicEq.snap(-40.0), 0.0)
    }

    @Test
    fun aFastSwipeFillsTheBandsBetweenTouches() {
        val drawn = GraphicEq.drawLine(List(10) { 0.0 }, 2, 0.0, 6, 8.0)
        assertEquals(listOf(0.0, 2.0, 4.0, 6.0, 8.0), drawn.subList(2, 7))
        assertEquals(0.0, drawn[1], 0.0)
        assertEquals(0.0, drawn[7], 0.0)
        val backwards = GraphicEq.drawLine(List(10) { 0.0 }, 6, 8.0, 2, 0.0)
        assertEquals(drawn, backwards)
    }

    @Test
    fun smoothingSoftensAStepWithoutChangingFlatAreas() {
        val out = GraphicEq.smooth(listOf(0.0, 0.0, 8.0, 0.0, 0.0))
        assertEquals(4.0, out[2], 0.0)
        assertEquals(2.0, out[1], 0.0)
        assertEquals(0.0, out[0], 0.0)
    }

    @Test
    fun aPresetSavesItsWholeSound() {
        val tone = Tone(bassDb = 3.0, graphicDb = List(10) { 1.0 }, surround = 0.5)
        val preset = Preset("user:a", "A", tone = tone)
        val target = Processing.target(AppState(current = preset, useSystemSurround = true), 0.0)
        assertEquals(0.5, target.surround, 0.0)
        // The +1 dB graphic curve and the +3 dB bass both reach the final sound at 20 Hz.
        assertEquals(4.0, Eq.curveDb(Processing.presetFilters(preset), 20.0), 0.2)
    }

    @Test
    fun oldTopLevelSettingsMoveIntoTheCurrentPreset() {
        val old = AppState(bassDb = 2.0, trebleDb = -1.0, surround = 0.4, graphicDb = List(9) { 1.0 }).migrated()
        assertEquals(2.0, old.current.tone.bassDb, 0.0)
        assertEquals(-1.0, old.current.tone.trebleDb, 0.0)
        assertEquals(0.4, old.current.tone.surround, 0.0)
        assertEquals(10, old.current.tone.graphicDb.size)
        assertEquals(0.0, old.bassDb, 0.0)
        assertTrue(old.graphicDb.isEmpty())
        // Running it again changes nothing, so a saved state can't double up.
        assertEquals(old, old.migrated())
    }

    @Test
    fun migrationLeavesNewStatesAlone() {
        val state = AppState(stateVersion = AppState.CURRENT_STATE_VERSION)
        assertEquals(state, state.migrated())
    }

    @Test
    fun oldSavedPresetsWithoutToneStillLoad() {
        val json = kotlinx.serialization.json.Json { ignoreUnknownKeys = true }
        val preset = json.decodeFromString(Preset.serializer(), """{"id":"user:x","name":"X"}""")
        assertEquals(Tone(), preset.tone)
    }

    private fun runFinder(pick: (Round) -> Choice): SoundFinder {
        val finder = SoundFinder(Tone())
        var guard = 0
        while (!finder.isDone && guard++ < 30) finder.choose(pick(finder.current!!))
        assertTrue(finder.isDone)
        return finder
    }

    @Test
    fun alwaysPickingTheLargerSideEndsNearTheTop() {
        val finder = runFinder { Choice.B }
        assertTrue(finder.answer(Trait.BASS) >= 3.0)
        assertTrue(finder.answer(Trait.TREBLE) >= 2.0)
        assertEquals(0.85, finder.answer(Trait.SPACE), 0.0)
    }

    @Test
    fun alwaysPickingTheSmallerSideEndsNearTheBottom() {
        val finder = runFinder { Choice.A }
        assertTrue(finder.answer(Trait.BASS) <= -3.0)
        assertTrue(finder.answer(Trait.TREBLE) <= -2.0)
        assertEquals(0.0, finder.answer(Trait.SPACE), 0.0)
    }

    @Test
    fun noPreferenceEverywhereChangesNothing() {
        val finder = runFinder { Choice.NO_PREFERENCE }
        val result = finder.result()
        assertEquals(0.0, result.bassDb, 0.0)
        assertEquals(0.0, result.vocalsDb, 0.0)
        assertEquals(0.0, result.trebleDb, 0.0)
        assertEquals(0.0, result.surround, 0.0)
    }

    @Test
    fun theTestAsksFewerThanTenQuestions() {
        var asked = 0
        runFinder {
            asked++
            Choice.B
        }
        assertTrue("asked $asked", asked in 1..9)
    }

    @Test
    fun answersBuildOnTheStartingToneAndKeepItsOtherSettings() {
        val base = Tone(bassDb = 2.0, graphicDb = List(10) { 1.0 })
        val finder = SoundFinder(base)
        while (!finder.isDone) finder.choose(Choice.B)
        val result = finder.result()
        assertTrue(result.bassDb > base.bassDb)
        assertEquals(base.graphicDb, result.graphicDb)
    }

    @Test
    fun previewsOnlyDifferInTheTraitBeingCompared() {
        val finder = SoundFinder(Tone())
        val a = finder.preview(Choice.A)
        val b = finder.preview(Choice.B)
        assertTrue(a.bassDb < b.bassDb)
        assertEquals(a.trebleDb, b.trebleDb, 0.0)
        assertEquals(a.vocalsDb, b.vocalsDb, 0.0)
    }

    @Test
    fun comparisonsAreLevelMatched() {
        val reference = Preset("user:a", "A", filters = emptyList())
        val matched = SoundFinder.levelMatched(reference, Tone(bassDb = 6.0, vocalsDb = 3.0), reference)
        val filters = Processing.presetFilters(matched)
        val loudness = Processing.estimatedLoudnessDb(filters, Processing.inputOf(matched, filters, true))
        assertEquals(0.0, loudness, 0.01)
        assertEquals(0.0, matched.preampDb, 0.0)
    }

    @Test
    fun aDifferentRangeOfAnswersGivesDifferentSounds() {
        val big = runFinder { Choice.B }.result()
        val small = runFinder { Choice.A }.result()
        assertFalse(big == small)
        assertTrue(max(big.bassDb, small.bassDb) > 0.0)
    }
}
