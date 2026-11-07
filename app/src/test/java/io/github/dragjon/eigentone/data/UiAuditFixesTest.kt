package io.github.dragjon.eigentone.data

import io.github.dragjon.eigentone.dsp.AutoEqParser
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class UiAuditFixesTest {
    @Test
    fun reloadingThePresetInUseKeepsItsEdits() {
        val warm = Repo.findPreset("builtin:warm")!!
        Repo.update { AppState(current = warm) }
        Repo.edit { it.copy(preampDb = -4.0) }
        // Such as "Next preset" with a single favourite, or that preset's own app icon shortcut.
        Repo.loadPreset(warm)
        assertEquals(-4.0, Repo.state.value.current.preampDb, 0.0)
    }

    @Test
    fun renamingAPresetWithWaitingEditsKeepsTheNewName() {
        val mine = Preset("user:rock", "Rock", 0.0, emptyList())
        Repo.update { AppState(current = mine, presets = listOf(mine)) }
        Repo.edit { it.copy(preampDb = -2.0) }
        Repo.loadPreset(BuiltIns.flat)
        Repo.renamePreset(mine.id, "Rock live")
        Repo.loadPreset(Repo.findPreset(mine.id)!!)
        assertEquals("Rock live", Repo.state.value.current.name)
        assertEquals(-2.0, Repo.state.value.current.preampDb, 0.0)
    }

    @Test
    fun deletingAPresetLeavesNoLinksToIt() {
        val mine = Preset("user:gone", "Gone", 0.0, emptyList())
        Repo.update {
            AppState(
                current = BuiltIns.flat,
                presets = listOf(mine),
                favorites = setOf(mine.id),
                appProfiles = mapOf("com.example.music" to mine.id),
            )
        }
        Repo.deletePreset(mine.id)
        val state = Repo.state.value
        assertFalse(mine.id in state.favorites)
        assertTrue(state.appProfiles.isEmpty())
    }

    @Test
    fun duplicatingCopiesTheSavedPresetJustBelowIt() {
        val a = Preset("user:a", "A", -2.0, emptyList(), folder = "Mine")
        val b = Preset("user:b", "B", 0.0, emptyList())
        Repo.update { AppState(current = a, presets = listOf(a, b)) }
        // Edits waiting on the original aren't copied.
        Repo.edit { it.copy(preampDb = -9.0) }
        val copy = Repo.duplicatePreset(a.id, PresetOrganiser.freeName(Repo.state.value, "A copy"))!!
        val presets = Repo.state.value.presets
        assertEquals(listOf("A", "A copy", "B"), presets.map { it.name })
        assertEquals(-2.0, copy.preampDb, 0.0)
        assertEquals("Mine", copy.folder)
        assertTrue(copy.id != a.id)
        // The one in use stays the original.
        assertEquals(a.id, Repo.state.value.current.id)
    }

    @Test
    fun savingNeverKeepsAPreviewTrim() {
        val mine = Preset("user:trim", "Trim", 0.0, emptyList())
        Repo.update { AppState(current = mine, presets = listOf(mine)) }
        Repo.edit { it.copy(tone = it.tone.copy(trimDb = -3.0, bassDb = 2.0)) }
        Repo.overwriteCurrent()
        val saved = Repo.state.value.presets.first { it.id == mine.id }
        assertEquals(0.0, saved.tone.trimDb, 0.0)
        assertEquals(2.0, saved.tone.bassDb, 0.0)
    }

    @Test
    fun importsReadCommaDecimalsAndSlopes() {
        val parsed = AutoEqParser.parse(
            """
            Preamp: -6,5 dB
            Filter 1: ON PK Fc 105,5 Hz Gain -1,6 dB Q 0,7
            Filter 2: ON LS 12dB Fc 1,000 Hz Gain 3.0 dB
            """.trimIndent(),
        )!!
        assertEquals(-6.5, parsed.preampDb, 1e-9)
        assertEquals(105.5, parsed.filters[0].freq, 1e-9)
        assertEquals(-1.6, parsed.filters[0].gainDb, 1e-9)
        assertEquals(0.7, parsed.filters[0].q, 1e-9)
        assertEquals(2, parsed.filters.size)
        assertEquals(1000.0, parsed.filters[1].freq, 1e-9)
        assertEquals(3.0, parsed.filters[1].gainDb, 1e-9)
    }
}
