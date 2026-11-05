package io.github.dragjon.eigentone.data

import io.github.dragjon.eigentone.data.PresetChanges.Part
import io.github.dragjon.eigentone.dsp.Filter
import io.github.dragjon.eigentone.dsp.FilterType
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class PresetChangesTest {
    private val saved = Preset(
        "user:x", "Optimus", -3.0,
        listOf(Filter(1, FilterType.BELL, 1000.0, 2.0, 1.0), Filter(2, FilterType.LOW_SHELF, 100.0, -1.0, 0.7)),
    )

    @Test
    fun nothingChangedSaysNothing() {
        assertTrue(PresetChanges.of(saved, saved).isEmpty())
        // A preview's level trim isn't an edit.
        assertTrue(PresetChanges.of(saved, saved.copy(tone = saved.tone.copy(trimDb = -2.0))).isEmpty())
    }

    @Test
    fun countsAddedRemovedAndChangedFilters() {
        val now = saved.copy(
            filters = listOf(saved.filters[0].copy(gainDb = 5.0), Filter(3), Filter(4)),
        )
        assertEquals(
            listOf(Part.ADDED_FILTERS to 2, Part.REMOVED_FILTERS to 1, Part.CHANGED_FILTERS to 1),
            PresetChanges.of(saved, now),
        )
    }

    @Test
    fun namesToneChanges() {
        val now = saved.copy(preampDb = -5.0, tone = saved.tone.copy(graphicDb = List(10) { 1.0 }, bassDb = 3.0, harshTamer = 0.4))
        assertEquals(listOf(Part.PREAMP, Part.FADERS, Part.BASS, Part.CLARITY), PresetChanges.of(saved, now).map { it.first })
    }

    @Test
    fun somethingUnnamedStillShows() {
        assertEquals(listOf(Part.OTHER), PresetChanges.of(saved, saved.copy(folder = "Mine")).map { it.first })
    }
}
