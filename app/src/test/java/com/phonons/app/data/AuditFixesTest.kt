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

    /**
     * What Android's effect does to one band, from DPFrequency.cpp. It ignores the band's on or off flag,
     * so this does too, which is what the real sound depends on.
     */
    private fun androidGainDb(band: CompressorBand, levelDb: Double): Double {
        val halfKnee = band.kneeDb / 2
        val over = levelDb - band.thresholdDb
        val change = when {
            levelDb > band.thresholdDb + halfKnee -> (1 / band.ratio - 1) * over
            levelDb > band.thresholdDb - halfKnee -> (1 / band.ratio - 1) * (over + halfKnee) * (over + halfKnee) / (halfKnee * 4)
            else -> 0.0
        }
        return change + band.makeupDb
    }

    private val loudLevels = listOf(-60.0, -40.0, -24.0, -12.0, -6.0, 0.0)

    @Test
    fun theHarshnessTamerLeavesEveryOtherRangeAlone() {
        for (amount in listOf(0.02, 0.5, 1.0)) {
            for (planned in Dynamics.plan(Compressor(), Tamers(harsh = amount))) {
                val isTamerBand = planned.cutoffHz == Dynamics.HARSH_HIGH_HZ
                assertEquals(isTamerBand, planned.active)
                if (!isTamerBand) for (level in loudLevels) assertEquals(0.0, androidGainDb(planned.band, level), 0.0)
            }
        }
    }

    @Test
    fun theBoomTamerLeavesEveryOtherRangeAlone() {
        for (planned in Dynamics.plan(Compressor(), Tamers(boom = 0.02))) {
            if (planned.cutoffHz != Dynamics.BOOM_TOP_HZ) {
                for (level in loudLevels) assertEquals(0.0, androidGainDb(planned.band, level), 0.0)
            }
        }
    }

    @Test
    fun theTamerStillTurnsDownLoudTreble() {
        val tamed = Dynamics.plan(Compressor(), Tamers(harsh = 0.5)).first { it.cutoffHz == Dynamics.HARSH_HIGH_HZ }
        org.junit.Assert.assertTrue(androidGainDb(tamed.band, -6.0) < -1.0)
        assertEquals(0.0, androidGainDb(tamed.band, -60.0), 0.0)
    }

    @Test
    fun aCompressorBandThatsOffReallyIsOff() {
        val punchy = Dynamics.styles.first { it.name == "Punchy" }.compressor
        val bassOff = punchy.copy(low = punchy.low.copy(enabled = false))
        val bass = Dynamics.plan(bassOff, Tamers()).first()
        org.junit.Assert.assertFalse(bass.active)
        for (level in loudLevels) assertEquals(0.0, androidGainDb(bass.band, level), 0.0)
        // The active ones keep working.
        org.junit.Assert.assertTrue(Dynamics.plan(bassOff, Tamers()).any { it.active && androidGainDb(it.band, -6.0) != 0.0 })
    }

    /** Android's formula, from EffectReverb.cpp, for how loud the echo ends up in dB. */
    private fun echoDb(roomLevel: Int, reverbLevel: Int) = (reverbLevel + roomLevel - 2000) / 100.0

    @Test
    fun androidsOwnLargeHallWouldBeTooQuietToHear() {
        assertEquals(-38.0, echoDb(-400, -1400), 0.0)
    }

    @Test
    fun theReverbAmountSetsAnEchoYouCanHear() {
        assertEquals(-30.0, echoDb(0, ReverbRoom.reverbLevel(0.0).toInt()), 0.0)
        assertEquals(0.0, echoDb(0, ReverbRoom.reverbLevel(1.0).toInt()), 0.0)
        val byDefault = echoDb(0, ReverbRoom.reverbLevel(Tone().reverbAmount).toInt())
        org.junit.Assert.assertTrue("$byDefault dB", byDefault in -18.0..-12.0)
    }

    @Test
    fun biggerRoomsRingForLonger() {
        val rooms = listOf(ReverbRoom.SMALL_ROOM, ReverbRoom.MEDIUM_ROOM, ReverbRoom.LARGE_ROOM, ReverbRoom.MEDIUM_HALL, ReverbRoom.LARGE_HALL)
        assertEquals(rooms, rooms.sortedBy { it.decayMs })
        org.junit.Assert.assertTrue(ReverbRoom.LARGE_HALL.decayMs in 3000..7000)
    }

    @Test
    fun reverbSettingsStayWithinWhatAndroidAccepts() {
        for (amount in listOf(-1.0, 0.0, 0.5, 1.0, 2.0)) {
            org.junit.Assert.assertTrue(ReverbRoom.reverbLevel(amount) in -9000..2000)
        }
        for (room in ReverbRoom.entries) {
            org.junit.Assert.assertTrue(room.name, room.roomHfLevel in -9000..0)
            org.junit.Assert.assertTrue(room.name, room.decayMs in 100..20000)
            org.junit.Assert.assertTrue(room.name, room.decayHfRatio in 100..2000)
            org.junit.Assert.assertTrue(room.name, room.diffusion in 0..1000)
            org.junit.Assert.assertTrue(room.name, room.density in 0..1000)
        }
    }

    @Test
    fun reverbStepsAsideForBypassAndTests() {
        val hall = Preset("user:r", "R", tone = Tone(reverb = ReverbRoom.LARGE_HALL))
        val on = AppState(enabled = true, current = hall)
        assertEquals(ReverbRoom.LARGE_HALL, Processing.reverbFor(on, suspended = false))
        assertEquals(ReverbRoom.OFF, Processing.reverbFor(on.copy(bypass = true), suspended = false))
        assertEquals(ReverbRoom.OFF, Processing.reverbFor(on, suspended = true))
        assertEquals(ReverbRoom.OFF, Processing.reverbFor(on.copy(enabled = false), suspended = false))
    }

    @Test
    fun reverbIsSavedAndSharedWithThePreset() {
        val hall = Preset("user:r", "R", tone = Tone(reverb = ReverbRoom.PLATE))
        assertEquals(ReverbRoom.PLATE, BackupCodec.sanitise(hall).tone.reverb)
        assertEquals(ReverbRoom.PLATE, ShareCode.decode(ShareCode.encode(hall))!!.tone.reverb)
    }

    @Test
    fun presetsSavedBeforeReverbStillLoad() {
        val old = kotlinx.serialization.json.Json { ignoreUnknownKeys = true }.decodeFromString(Tone.serializer(), "{\"bassDb\":2.0}")
        assertEquals(ReverbRoom.OFF, old.reverb)
    }

    @Test
    fun fadersStartInPrecisionModeAndOlderStatesStillLoad() {
        assertEquals(FaderMode.PRECISION, AppState().faderMode)
        assertEquals(FaderView.SCROLL, AppState().faderView)
        val old = kotlinx.serialization.json.Json { ignoreUnknownKeys = true }.decodeFromString(AppState.serializer(), "{\"enabled\":true}")
        assertEquals(FaderMode.PRECISION, old.faderMode)
    }

    @Test
    fun theStripShowsReverbOnlyWhileItsRunning() {
        val hall = Preset("user:r", "R", tone = Tone(reverb = ReverbRoom.LARGE_HALL))
        assertEquals(EffectState("Reverb", true), Processing.effectStates(AppState(enabled = true, current = hall), buds).last())
        assertEquals(EffectState("Reverb", false), Processing.effectStates(AppState(enabled = true, current = hall, bypass = true), buds).last())
        assertEquals(EffectState("Reverb", false), Processing.effectStates(AppState(enabled = true), buds).last())
    }

    @Test
    fun theLimiterCantBeSetToClip() {
        val wild = LimiterSettings(attackMs = -5.0, releaseMs = 9999.0, ratio = 0.0, thresholdDb = -4.0, postGainDb = 10.0).clamped()
        assertEquals(4.0, wild.postGainDb, 0.0)
        assertEquals(0.0, wild.ceilingDb, 0.0)
        org.junit.Assert.assertTrue(wild.attackMs >= 0.1 && wild.releaseMs <= 1000.0 && wild.ratio >= 1.0)
    }

    @Test
    fun theLimiterSettingsReachTheEngineAndBypassDropsThem() {
        val state = AppState(enabled = true, limiterAttackMs = 5.0, limiterRatio = 20.0, limiterThresholdDb = -3.0, limiterPostGainDb = 2.0)
        val target = Processing.target(state, 0.0)
        assertEquals(LimiterSettings(5.0, 60.0, 20.0, -3.0, 2.0), target.limiterSettings)
        org.junit.Assert.assertFalse(Processing.target(state.copy(bypass = true, matchBypass = false), 0.0).limiter)
    }

    @Test
    fun naturalBassAddsNoCompressorBand() {
        val plan = Dynamics.plan(Compressor(), Tamers(bassAmount = 6.0))
        org.junit.Assert.assertTrue(plan.none { it.active })
    }

    @Test
    fun punchAndSustainShapeOnlyTheDeepBass() {
        for (character in listOf(BassCharacter.PUNCH, BassCharacter.SUSTAIN)) {
            val plan = Dynamics.plan(Compressor(), Tamers(bass = character, bassAmount = 6.0))
            assertEquals(Dynamics.FULL_BANDS, plan.size)
            val active = plan.filter { it.active }
            assertEquals(1, active.size)
            assertEquals(Dynamics.BOOM_TOP_HZ, active.single().cutoffHz, 0.0)
            for (other in plan - active.single()) for (level in loudLevels) assertEquals(0.0, androidGainDb(other.band, level), 0.0)
        }
        val punch = Dynamics.characterBand(Tamers(bass = BassCharacter.PUNCH, bassAmount = 6.0))!!
        val sustain = Dynamics.characterBand(Tamers(bass = BassCharacter.SUSTAIN, bassAmount = 6.0))!!
        // Punch waits before catching a hit, and Sustain catches it at once and holds on longer.
        org.junit.Assert.assertTrue(punch.attackMs > sustain.attackMs && sustain.releaseMs > punch.releaseMs)
        org.junit.Assert.assertTrue(punch.makeupDb > 0.0 && sustain.makeupDb > 0.0)
    }

    @Test
    fun aCharacterNeedsAddedBass() {
        org.junit.Assert.assertNull(Dynamics.characterBand(Tamers(bass = BassCharacter.SUSTAIN, bassAmount = 0.0)))
        val cut = Preset("user:c", "C", tone = Tone(bassDb = -4.0, bassCharacter = BassCharacter.PUNCH))
        assertEquals(0.0, Processing.target(AppState(enabled = true, current = cut), 0.0).tamers.bassAmount, 0.0)
    }

    @Test
    fun theBoomTamerKeepsControlAlongsideACharacter() {
        val both = Dynamics.plan(Compressor(), Tamers(boom = 1.0, bass = BassCharacter.SUSTAIN, bassAmount = 6.0)).first { it.active }
        val boom = Dynamics.boomBand(1.0)
        org.junit.Assert.assertTrue(both.band.thresholdDb <= boom.thresholdDb && both.band.ratio >= boom.ratio)
    }

    @Test
    fun theBassCharacterIsSavedWithThePreset() {
        val preset = Preset("user:b", "B", tone = Tone(bassDb = 3.0, bassCharacter = BassCharacter.PUNCH))
        assertEquals(BassCharacter.PUNCH, BackupCodec.sanitise(preset).tone.bassCharacter)
        assertEquals(BassCharacter.PUNCH, ShareCode.decode(ShareCode.encode(preset))!!.tone.bassCharacter)
    }

    @Test
    fun editsToAPresetSurviveSwitchingAwayAndBack() {
        Repo.update { AppState(current = BuiltIns.flat) }
        val warm = Repo.findPreset("builtin:warm")!!
        Repo.loadPreset(warm)
        Repo.edit { it.copy(preampDb = -3.0) }
        Repo.loadPreset(BuiltIns.flat)
        assertEquals(BuiltIns.flat, Repo.state.value.current)
        org.junit.Assert.assertTrue("builtin:warm" in Repo.editedIds.value)
        Repo.loadPreset(warm)
        assertEquals(-3.0, Repo.state.value.current.preampDb, 0.0)
        // Discarding goes back to the saved version, and switching away then leaves nothing kept.
        Repo.discardEdits()
        assertEquals(warm, Repo.state.value.current)
        Repo.loadPreset(BuiltIns.flat)
        Repo.loadPreset(warm)
        assertEquals(warm, Repo.state.value.current)
        org.junit.Assert.assertTrue(Repo.editedIds.value.isEmpty())
    }

    @Test
    fun theSwitcherOffersTheSamePresetsWhicheverIsInUse() {
        val own = List(3) { Preset("user:$it", "P$it") }
        val built = listOf(BuiltIns.flat, Preset("builtin:warm", "Warm"), Preset("builtin:liberty4nc", "Liberty"))
        val available = built + own
        val onFlat = PresetOrganiser.quickKeys(AppState(current = BuiltIns.flat), available).map { it.id }.toSet()
        val onOwn = PresetOrganiser.quickKeys(AppState(current = own[0]), available).map { it.id }.toSet()
        assertEquals(available.map { it.id }.toSet(), onFlat)
        assertEquals(onFlat, onOwn)
    }

    @Test
    fun aPresetNameCanOnlyBeUsedOnce() {
        val state = AppState(presets = listOf(Preset("user:1", "Commute")))
        org.junit.Assert.assertTrue(PresetOrganiser.nameTaken(state, " commute "))
        org.junit.Assert.assertFalse(PresetOrganiser.nameTaken(state, "Commute", exceptId = "user:1"))
        org.junit.Assert.assertTrue(PresetOrganiser.nameTaken(state, BuiltIns.flat.name))
        assertEquals("Commute 2", PresetOrganiser.freeName(state, "Commute"))
        assertEquals("Gym", PresetOrganiser.freeName(state, "Gym"))
    }

    @Test
    fun eachOutputKeepsItsOwnLoudnessStrength() {
        val state = AppState(loudness = true, loudnessStrength = 0.8, loudnessStrengths = mapOf("speaker" to 0.3))
        assertEquals(0.3, state.loudnessStrengthFor("speaker"), 0.0)
        assertEquals(0.8, state.loudnessStrengthFor("Bluetooth:Buds"), 0.0)
        val speaker = com.phonons.app.dsp.Eq.curveDb(Processing.effectiveFilters(state, 40.0, "speaker"), 63.0)
        val buds = com.phonons.app.dsp.Eq.curveDb(Processing.effectiveFilters(state, 40.0, "Bluetooth:Buds"), 63.0)
        org.junit.Assert.assertTrue(buds > speaker * 2)
    }

    @Test
    fun anImportedPreampStaysInASafeRange() {
        assertEquals(30.0, AutoEqParser.parse("Preamp: 9999 dB\nFilter 1: ON PK Fc 1000 Hz Gain 1 dB Q 1")!!.preampDb, 0.0)
        assertEquals(-30.0, AutoEqParser.parse("Preamp: -9999 dB\nFilter 1: ON PK Fc 1000 Hz Gain 1 dB Q 1")!!.preampDb, 0.0)
    }
}
