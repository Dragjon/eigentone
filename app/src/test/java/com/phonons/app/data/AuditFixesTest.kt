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
    fun balanceTowardsASideTurnsTheOtherSideDown() {
        org.junit.Assert.assertTrue(Processing.leftTrimDb(0.5) < 0.0)
        assertEquals(0.0, Processing.rightTrimDb(0.5), 0.0)
        org.junit.Assert.assertTrue(Processing.rightTrimDb(-0.5) < 0.0)
        assertEquals(0.0, Processing.leftTrimDb(-0.5), 0.0)
        assertEquals(0.0, Processing.leftTrimDb(0.0), 0.0)
        assertEquals(0.0, Processing.rightTrimDb(0.0), 0.0)
    }

    private val flatBassAim = listOf(20.0, 50.0, 125.0, 200.0, 500.0).map { ShapePoint(it, -1.6) }
    private val inEar = CorrectionInfo(HeadphoneForm.IN_EAR, flatBassAim)

    private fun lift(target: HeadphoneTarget, info: CorrectionInfo, hz: Double) =
        com.phonons.app.dsp.GraphicEq.interpolate(HeadphoneTargets.change(target, info).map { it.hz to it.db }, hz)

    @Test
    fun aCorrectionWithoutABassLiftGetsItFromHarman() {
        assertEquals(8.3, lift(HeadphoneTarget.HARMAN, inEar, 50.0), 0.3)
        assertEquals(0.0, lift(HeadphoneTarget.HARMAN, inEar, 1000.0), 0.0)
        assertEquals(8.0, HeadphoneTargets.bassBelowHarman(inEar), 0.5)
    }

    @Test
    fun aCorrectionAlreadyOnHarmanBarelyMoves() {
        val onHarman = CorrectionInfo(
            HeadphoneForm.IN_EAR,
            listOf(ShapePoint(20.0, 7.6), ShapePoint(50.0, 6.7), ShapePoint(80.0, 4.2), ShapePoint(125.0, 1.4), ShapePoint(200.0, -1.0), ShapePoint(500.0, -1.4)),
        )
        assertEquals(0.0, lift(HeadphoneTarget.HARMAN, onHarman, 50.0), 0.2)
    }

    @Test
    fun theBassChoicesKeepTheirPublishedDistanceFromHarman() {
        val harman = lift(HeadphoneTarget.HARMAN, inEar, 31.5)
        assertEquals(harman + 5.0, lift(HeadphoneTarget.MORE_BASS, inEar, 31.5), 0.2)
        assertEquals(harman - 2.0, lift(HeadphoneTarget.LESS_BASS, inEar, 31.5), 0.2)
        assertEquals(harman - 9.0, lift(HeadphoneTarget.NO_BASS_LIFT, inEar, 31.5), 0.2)
    }

    @Test
    fun onlyTheReferenceTargetChangesTheTreble() {
        assertEquals(0.0, lift(HeadphoneTarget.HARMAN, inEar, 8000.0), 0.0)
        org.junit.Assert.assertTrue(lift(HeadphoneTarget.DIFFUSE_FIELD, inEar, 12500.0) > 3.0)
    }

    @Test
    fun noTargetOrNoInfoChangesNothing() {
        assertEquals(emptyList<ShapePoint>(), HeadphoneTargets.change(null, inEar))
        assertEquals(emptyList<ShapePoint>(), HeadphoneTargets.change(HeadphoneTarget.HARMAN, null))
        val preset = Preset("user:t", "T", tone = Tone(targetId = "harman"))
        assertEquals(emptyList<com.phonons.app.dsp.Filter>(), Processing.presetFilters(preset))
    }

    @Test
    fun harmanIsOnlySuggestedWhenItWouldHelp() {
        assertEquals(true, HeadphoneTargets.worthSuggesting(Tone(correctionInfo = inEar), "user:a"))
        // Already chosen, nothing known about the aim, and already close to Harman.
        assertEquals(false, HeadphoneTargets.worthSuggesting(Tone(correctionInfo = inEar, targetId = "harman-less-bass"), "user:a"))
        assertEquals(false, HeadphoneTargets.worthSuggesting(Tone(), "user:a"))
        assertEquals(false, HeadphoneTargets.worthSuggesting(Tone(), "builtin:liberty4nc"))
        assertEquals(true, HeadphoneTargets.worthSuggesting(Tone(), "builtin:liberty4nc-rtings"))
    }

    @Test
    fun aTargetIsPartOfThePresetsSound() {
        val preset = Preset("user:t", "T", tone = Tone(targetId = "harman", correctionInfo = inEar))
        org.junit.Assert.assertTrue(com.phonons.app.dsp.Eq.curveDb(Processing.presetFilters(preset), 50.0) > 7.0)
        val off = preset.copy(tone = preset.tone.copy(correction = 0.0))
        assertEquals(0.0, com.phonons.app.dsp.Eq.curveDb(Processing.presetFilters(off), 50.0), 0.0)
    }

    @Test
    fun aBackupKeepsOnlyKnownTargets() {
        val good = Preset("user:t", "T", tone = Tone(targetId = "harman", correctionInfo = inEar))
        assertEquals("harman", BackupCodec.sanitise(good).tone.targetId)
        val bad = good.copy(tone = good.tone.copy(targetId = "made up"))
        assertNull(BackupCodec.sanitise(bad).tone.targetId)
    }

    @Test
    fun aResultFileGivesItsAimRelativeTo1kHz() {
        val text = "frequency,raw,target\n20.00,1,8\n1000.00,1,2\n20000.00,1,-3\n"
        val aim = HeadphoneTargets.aimFromResult(text)!!
        assertEquals(6.0, aim.first().db, 0.001)
        assertNull(HeadphoneTargets.aimFromResult("not,a,result\n1,2,3"))
        assertNull(HeadphoneTargets.aimFromResult(""))
    }

    @Test
    fun theHeadphoneFormComesFromTheMeasurementPath() {
        assertEquals(HeadphoneForm.OVER_EAR, CatalogEntry("A", "s", "oratory1990/over-ear/A").form)
        assertEquals(HeadphoneForm.OVER_EAR, CatalogEntry("A", "s", "Rtings/HMS%20II.3%20over-ear/A").form)
        assertEquals(HeadphoneForm.IN_EAR, CatalogEntry("A", "s", "DHRME/in-ear/A").form)
        assertEquals(HeadphoneForm.IN_EAR, CatalogEntry("A", "s", "Regan/earbud/A").form)
    }

    @Test
    fun anImportedPreampStaysInASafeRange() {
        assertEquals(30.0, AutoEqParser.parse("Preamp: 9999 dB\nFilter 1: ON PK Fc 1000 Hz Gain 1 dB Q 1")!!.preampDb, 0.0)
        assertEquals(-30.0, AutoEqParser.parse("Preamp: -9999 dB\nFilter 1: ON PK Fc 1000 Hz Gain 1 dB Q 1")!!.preampDb, 0.0)
    }
}
