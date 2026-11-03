package com.phonons.app.data

import com.phonons.app.dsp.AutoEqParser
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class AuditFixesTest {
    private val buds = "Bluetooth:Buds"
    private val mine = Preset("user:mine", "Mine")
    private val other = Preset("user:other", "Other")
    private val all = listOf(mine, other, BuiltIns.flat)
    private fun find(id: String) = all.firstOrNull { it.id == id }

    @Test
    fun connectingAnOutputLoadsItsPreset() {
        val state = AppState(current = other, presets = all, outputProfiles = mapOf(buds to mine.id))
        assertEquals(mine, Profiles.presetForNewOutput(state, "speaker", buds, ::find))
    }

    @Test
    fun anOutputAlreadyOnItsPresetChangesNothing() {
        val state = AppState(current = mine, presets = all, outputProfiles = mapOf(buds to mine.id))
        assertNull(Profiles.presetForNewOutput(state, "speaker", buds, ::find))
    }

    @Test
    fun leavingAnOutputDropsItsUneditedCorrection() {
        val state = AppState(current = mine, presets = all, outputProfiles = mapOf(buds to mine.id))
        assertEquals(BuiltIns.flat, Profiles.presetForNewOutput(state, buds, "speaker", ::find))
    }

    @Test
    fun leavingAnOutputKeepsEdits() {
        val edited = mine.copy(preampDb = -2.0)
        val state = AppState(current = edited, presets = all, outputProfiles = mapOf(buds to mine.id))
        assertNull(Profiles.presetForNewOutput(state, buds, "speaker", ::find))
    }

    @Test
    fun aFirstRunHasNoPreviousOutputToLeave() {
        val state = AppState(current = mine, presets = all, outputProfiles = mapOf(buds to mine.id))
        assertNull(Profiles.presetForNewOutput(state, null, "speaker", ::find))
    }

    private fun low(tone: Tone) = com.phonons.app.dsp.Eq.curveDb(Processing.presetFilters(Preset("user:b", "B", tone = tone)), 40.0)
    private fun mid(tone: Tone) = com.phonons.app.dsp.Eq.curveDb(Processing.presetFilters(Preset("user:b", "B", tone = tone)), 150.0)

    @Test
    fun theBassFrequencyMovesTheBassChange() {
        assertEquals(low(Tone(bassDb = 6.0)), low(Tone(bassDb = 6.0, bassHz = 105.0)), 0.0)
        val deep = Tone(bassDb = 6.0, bassHz = 40.0)
        val wide = Tone(bassDb = 6.0, bassHz = 250.0)
        org.junit.Assert.assertTrue(mid(wide) > mid(deep))
    }

    @Test
    fun anOldBassTunerMovesIntoBassWithoutChangingTheSound() {
        val old = Tone(bassTunerDb = 5.0, bassTunerHz = 60.0)
        val merged = old.withMergedBass()
        assertEquals(5.0, merged.bassDb, 0.0)
        assertEquals(60.0, merged.bassHz, 0.0)
        assertEquals(0.0, merged.bassTunerDb, 0.0)
        assertEquals(low(old), low(merged), 1e-9)
        assertEquals(mid(old), mid(merged), 1e-9)
    }

    @Test
    fun aSoundUsingBothBassControlsKeepsThem() {
        val both = Tone(bassDb = 3.0, bassTunerDb = 5.0, bassTunerHz = 60.0)
        assertEquals(both, both.withMergedBass())
    }

    @Test
    fun startingTheNewVersionKeepsSavedPresetsMatchingTheWorkingCopy() {
        val preset = Preset("user:t", "T", tone = Tone(bassTunerDb = 4.0, bassTunerHz = 70.0))
        val state = AppState(stateVersion = AppState.CURRENT_STATE_VERSION, current = preset, presets = listOf(preset)).migrated()
        assertEquals(state.presets.single(), state.current)
        assertEquals(4.0, state.current.tone.bassDb, 0.0)
        assertEquals(state, state.migrated())
    }

    @Test
    fun aBackupFromBeforeTheMergeIsMergedToo() {
        val preset = Preset("user:t", "T", tone = Tone(bassTunerDb = 4.0, bassTunerHz = 70.0))
        assertEquals(70.0, BackupCodec.sanitise(preset).tone.bassHz, 0.0)
    }

    @Test
    fun theStylesTheTamersReplacedAreGone() {
        val names = Dynamics.styles.map { it.name }
        assertEquals(listOf("Gentle", "Punchy", "Clear speech"), names)
    }

    @Test
    fun anImportedPreampStaysInASafeRange() {
        assertEquals(30.0, AutoEqParser.parse("Preamp: 9999 dB\nFilter 1: ON PK Fc 1000 Hz Gain 1 dB Q 1")!!.preampDb, 0.0)
        assertEquals(-30.0, AutoEqParser.parse("Preamp: -9999 dB\nFilter 1: ON PK Fc 1000 Hz Gain 1 dB Q 1")!!.preampDb, 0.0)
    }
}
