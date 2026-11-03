package com.phonons.app.data

import com.phonons.app.dsp.Eq
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.math.abs

class NewFeaturesTest {
    private val libertyBass = BuiltIns.all.first { it.id == "builtin:liberty4nc-bass" }

    @Test
    fun correctionStrengthScalesOnlyThePresetsOwnFilters() {
        val withFaders = libertyBass.copy(tone = Tone(graphicDb = List(10) { 2.0 }, graphicBands = 10))
        val off = withFaders.copy(tone = withFaders.tone.copy(correction = 0.0))
        val filters = Processing.presetFilters(off)
        assertEquals(2.0, Eq.curveDb(filters, 7400.0), 0.01)
        assertEquals(2.0, Eq.curveDb(filters, 2400.0), 0.01)
        assertEquals(0.0, Processing.preampOf(off), 0.0)

        val half = withFaders.copy(tone = withFaders.tone.copy(correction = 0.5))
        val full = Eq.curveDb(Processing.presetFilters(withFaders), 7369.0) - 2.0
        assertEquals(full / 2, Eq.curveDb(Processing.presetFilters(half), 7369.0) - 2.0, 0.01)
        assertEquals(libertyBass.preampDb / 2, Processing.preampOf(half), 0.0)
    }

    @Test
    fun backupKeepsShapesAndPerAppPresets() {
        val preset = Preset("user:mine", "Mine", 0.0, emptyList())
        val shape = CustomShape("custom:mine", "My hearing", listOf(ShapePoint(100.0, 4.0), ShapePoint(1000.0, -2.0)))
        val state = AppState(
            presets = listOf(preset),
            customShapes = listOf(shape),
            favoriteShapes = setOf("custom:mine"),
            appProfiles = mapOf("com.spotify.music" to "user:mine", "com.gone" to "user:deleted"),
        )
        val restored = BackupCodec.merge(AppState(), BackupCodec.decode(BackupCodec.encode(state))!!)
        assertEquals(listOf(shape), restored.customShapes)
        assertEquals(setOf("custom:mine"), restored.favoriteShapes)
        assertEquals(mapOf("com.spotify.music" to "user:mine"), restored.appProfiles)

        // Restoring over the same shape replaces it instead of adding a copy.
        val again = BackupCodec.merge(restored, BackupCodec.decode(BackupCodec.encode(state))!!)
        assertEquals(listOf(shape), again.customShapes)
    }

    @Test
    fun backupWithoutTheNewFieldsStillRestores() {
        val backup = BackupCodec.decode("""{"version":1,"presets":[{"id":"user:old","name":"Old","preampDb":0.0,"filters":[]}]}""")!!
        val restored = BackupCodec.merge(AppState(), backup)
        assertEquals(listOf("user:old"), restored.presets.map { it.id })
        assertTrue(restored.customShapes.isEmpty())
    }

    @Test
    fun restoredShapesAreChecked() {
        val text = """{"customShapes":[
            {"id":"custom:ok","name":"Ok","points":[{"hz":5.0,"db":1.0},{"hz":100.0,"db":99.0},{"hz":100.0,"db":1.0}]},
            {"id":"builtin:bad","name":"Wrong id","points":[{"hz":100.0,"db":1.0}]},
            {"id":"custom:blank","name":" ","points":[{"hz":100.0,"db":1.0}]},
            {"id":"custom:empty","name":"Empty","points":[{"hz":1.0,"db":1.0}]}
        ]}"""
        val shape = BackupCodec.decode(text)!!.customShapes.single()
        assertEquals("custom:ok", shape.id)
        assertEquals(listOf(ShapePoint(100.0, com.phonons.app.dsp.GraphicEq.MAX_DB)), shape.points)
    }

    @Test
    fun removingACorrectionAlsoRemovesItsHighPass() {
        val clearVocals = BuiltIns.all.first { it.id == "builtin:vocal" }
        val off = clearVocals.copy(tone = Tone(correction = 0.0))
        val filters = Processing.presetFilters(off)
        for (hz in listOf(25.0, 300.0, 2800.0, 14000.0)) assertEquals(0.0, Eq.curveDb(filters, hz), 0.001)
    }

    @Test
    fun levelMatchUsesATrimThatsNeverSavedAndEvensLoudness() {
        val loud = BuiltIns.flat.copy(tone = Tone(bassDb = 8.0, vocalsDb = 3.0))
        val matched = SoundFinder.levelMatched(BuiltIns.flat, loud.tone, BuiltIns.flat)
        assertEquals(BuiltIns.flat.preampDb, matched.preampDb, 0.0)
        assertTrue(matched.tone.trimDb != 0.0)
        fun loudness(p: Preset): Double {
            val f = Processing.presetFilters(p)
            return Processing.estimatedLoudnessDb(f, Processing.inputOf(p, f, true))
        }
        assertEquals(loudness(BuiltIns.flat), loudness(matched), 0.2)
        val code = ShareCode.encode(matched)
        assertEquals(0.0, ShareCode.decode(code)!!.tone.trimDb, 0.0)
    }

    @Test
    fun migrationAddsTheOwnersCurveOnceAndNeverToNewInstalls() {
        val old = AppState(stateVersion = 0).migrated()
        assertEquals(listOf("My soundcore curve"), old.customShapes.map { it.name })
        assertEquals(AppState.CURRENT_STATE_VERSION, old.stateVersion)
        assertEquals(old, old.migrated())

        val deletedLater = old.copy(customShapes = emptyList()).migrated()
        assertTrue(deletedLater.customShapes.isEmpty())

        val fresh = AppState(stateVersion = AppState.CURRENT_STATE_VERSION).migrated()
        assertTrue(fresh.customShapes.isEmpty())
    }

    @Test
    fun shapeTextReadsWhatPeopleType() {
        val parsed = ShapeText.parse("100 +4\n1.6k 4\n12.8 kHz: -2 dB; 200Hz, 1") as ShapeText.Parsed.Points
        assertEquals(
            listOf(ShapePoint(100.0, 4.0), ShapePoint(200.0, 1.0), ShapePoint(1600.0, 4.0), ShapePoint(12800.0, -2.0)),
            parsed.points,
        )
        assertEquals(parsed, ShapeText.parse(ShapeText.format(parsed.points)))
    }

    @Test
    fun shapeTextExplainsProblems() {
        for (bad in listOf("", "banana", "100", "5 3", "100 99", "100 1\n100 2", "100 0\n200 0")) {
            assertTrue(bad, ShapeText.parse(bad) is ShapeText.Parsed.Problem)
        }
    }

    @Test
    fun shareCodesRoundTripAndGetANewId() {
        val preset = libertyBass.copy(id = "user:abc", name = "Mine", tone = Tone(bassDb = 3.0, correction = 0.5, graphicDb = List(10) { 1.0 }))
        val decoded = ShareCode.decode("  " + ShareCode.encode(preset) + "\n")!!
        assertEquals("Mine", decoded.name)
        assertEquals(preset.filters, decoded.filters)
        assertEquals(preset.tone, decoded.tone)
        assertTrue(decoded.id.startsWith("user:") && decoded.id != preset.id)
    }

    @Test
    fun shareCodesRejectJunkAndLimitWildValues() {
        for (bad in listOf("", "hello", "phonons1:", "phonons1:!!!!", "phonons1:AAAA", "phonons1:" + "A".repeat(70000))) {
            assertNull(bad.take(20), ShareCode.decode(bad))
        }
        val wild = libertyBass.copy(name = "x", preampDb = 500.0, tone = Tone(bassDb = 500.0, correction = 9.0))
        val decoded = ShareCode.decode(ShareCode.encode(wild))!!
        assertEquals(30.0, decoded.preampDb, 0.0)
        assertEquals(Processing.MAX_TONE_DB, decoded.tone.bassDb, 0.0)
        assertEquals(1.0, decoded.tone.correction, 0.0)
    }

    @Test
    fun nightScheduleHandlesMidnight() {
        val overnight = NightSchedule(enabled = true, startMinute = 22 * 60, endMinute = 7 * 60)
        assertTrue(overnight.isActive(23 * 60))
        assertTrue(overnight.isActive(0))
        assertTrue(overnight.isActive(6 * 60 + 59))
        assertFalse(overnight.isActive(7 * 60))
        assertFalse(overnight.isActive(12 * 60))
        val daytime = NightSchedule(enabled = true, startMinute = 9 * 60, endMinute = 17 * 60)
        assertTrue(daytime.isActive(12 * 60))
        assertFalse(daytime.isActive(20 * 60))
        assertFalse(overnight.copy(enabled = false).isActive(23 * 60))
    }

    @Test
    fun scheduledNightModeAppliesOnlyWhileActiveAndNeverWeakensTheSetting() {
        val state = AppState(nightMode = 0.2, nightSchedule = NightSchedule(enabled = true, amount = 0.6))
        assertEquals(0.2, Processing.target(state, 0.0).nightMode, 0.0)
        assertEquals(0.6, Processing.target(state, 0.0, scheduledNight = true).nightMode, 0.0)
        assertEquals(0.9, Processing.target(state.copy(nightMode = 0.9), 0.0, scheduledNight = true).nightMode, 0.0)
        assertEquals(0.0, Processing.target(state.copy(bypass = true), 0.0, scheduledNight = true).nightMode, 0.0)
    }

    @Test
    fun deviceNamesFindTheRightHeadphones() {
        val entries = listOf(
            CatalogEntry("Sony WH-1000XM5", "Rtings", "a"),
            CatalogEntry("Sony WH-1000XM5", "oratory1990", "b"),
            CatalogEntry("Sony WH-1000XM4", "oratory1990", "c"),
            CatalogEntry("Anker Soundcore Liberty 4 NC", "DHRME", "d"),
            CatalogEntry("Anker Soundcore Liberty 4", "DHRME", "e"),
            CatalogEntry("Samsung Galaxy Buds2 Pro", "crinacle", "f"),
            CatalogEntry("Apple AirPods Pro (ANC on)", "Rtings", "g"),
        )
        fun match(name: String) = AutoEqCatalog.bestMatch(entries, name)?.path
        assertEquals("b", match("WH-1000XM5"))
        assertEquals("d", match("soundcore Liberty 4 NC"))
        assertEquals("d", match("Ethan's soundcore Liberty 4 NC"))
        assertEquals("e", match("soundcore Liberty 4"))
        assertEquals("f", match("Galaxy Buds2 Pro"))
        assertEquals("g", match("AirPods Pro"))
        assertNull(match("Buds"))
        assertNull(match("Pro"))
        assertNull(match("My Speaker"))
        assertNull(match("Liberty"))
    }

    /** Answers as a listener whose threshold is known, so the method's accuracy can be measured. */
    private fun run(test: HearingTest, thresholdFor: (Ear, Double) -> Double): HearingResult {
        var answers = 0
        while (!test.isDone) {
            val heard = test.levelDb >= thresholdFor(test.ear!!, test.hz!!)
            assertTrue(test.levelDb <= HearingTest.CEILING_DB)
            test.answer(heard)
            assertTrue("never finishes", ++answers < 500)
        }
        return test.result()
    }

    @Test
    fun hearingCheckFindsThresholdsWithinOneStep() {
        val truth = mapOf(250.0 to -52.0, 500.0 to -60.0, 1000.0 to -66.0, 2000.0 to -68.0, 4000.0 to -61.0, 8000.0 to -43.0)
        val result = run(HearingTest()) { _, hz -> truth.getValue(hz) }
        for ((hz, level) in truth) {
            val found = result.thresholds.getValue(Ear.LEFT).getValue(hz)
            assertTrue("$hz found $found for $level", found >= level && found - level <= 10.0)
        }
    }

    @Test
    fun hearingCheckStaysShortEnough() {
        var answers = 0
        val test = HearingTest()
        while (!test.isDone) {
            test.answer(test.levelDb >= -55.0)
            answers++
        }
        assertTrue("took $answers answers", answers <= 72)
    }

    @Test
    fun hearingCheckHandlesExtremes() {
        val silent = run(HearingTest()) { _, _ -> 0.0 }
        assertEquals(HearingTest.CEILING_DB, silent.thresholds.getValue(Ear.RIGHT).getValue(1000.0), 0.0)
        assertTrue(silent.shapePoints().all { it.db == 0.0 })
        val perfect = run(HearingTest()) { _, _ -> -200.0 }
        assertEquals(HearingTest.FLOOR_DB, perfect.thresholds.getValue(Ear.LEFT).getValue(1000.0), 0.0)
    }

    @Test
    fun hearingShapeLiftsWhatsHeardLeastWellGently() {
        val result = HearingResult(
            Ear.entries.associateWith {
                mapOf(1000.0 to -60.0, 2000.0 to -60.0, 4000.0 to -60.0, 8000.0 to -40.0, 500.0 to -60.0, 250.0 to -60.0)
            },
        )
        val points = result.shapePoints().associate { it.hz to it.db }
        assertEquals(6.0, points.getValue(8000.0), 0.0)
        assertEquals(0.0, points.getValue(1000.0), 0.0)
    }

    private fun earResult(left: Double, right: Double) =
        HearingResult(mapOf(Ear.LEFT to mapOf(1000.0 to left), Ear.RIGHT to mapOf(1000.0 to right)))

    @Test
    fun earCorrectionOnlyAppearsForARealDifference() {
        // One 5 dB step could just be a missed beep.
        assertNull(earResult(-60.0, -55.0).earCorrection())
        assertNotNull(earResult(-60.0, -50.0).earCorrection())
    }

    @Test
    fun earCorrectionClosesHalfTheGapAtEachFrequency() {
        // The right ear needs the tone 20 dB louder, so it gets lifted and the left comes down.
        val ears = earResult(-60.0, -40.0).earCorrection()!!
        assertEquals(5.0, ears.right.single().db, 0.0)
        assertEquals(-5.0, ears.left.single().db, 0.0)
        // Far apart readings still stay gentle.
        val extreme = earResult(-90.0, -15.0).earCorrection()!!
        assertEquals(6.0, extreme.right.single().db, 0.0)
    }

    private val ears = EarCorrection(
        left = listOf(ShapePoint(1000.0, -3.0)),
        right = listOf(ShapePoint(1000.0, 3.0)),
    )

    @Test
    fun earCorrectionGivesEachChannelItsOwnCurveAndOneLevel() {
        val state = AppState(earCorrection = ears, autoHeadroom = true)
        val target = Processing.target(state, 0.0)
        assertEquals(-3.0, Eq.curveDb(target.filters, 1000.0), 0.001)
        assertEquals(3.0, Eq.curveDb(target.rightFilters, 1000.0), 0.001)
        // Headroom comes from the ear with the larger boost, and applies to both.
        assertEquals(-3.0, target.inputDb, 0.01)
    }

    @Test
    fun earCorrectionStepsAsideWhenOffOrBypassed() {
        val plain = Processing.target(AppState(), 0.0)
        assertTrue(plain.filters === plain.rightFilters)

        val disabled = Processing.target(AppState(earCorrection = ears.copy(enabled = false)), 0.0)
        assertTrue(disabled.filters === disabled.rightFilters)

        for (match in listOf(true, false)) {
            val bypassed = Processing.target(AppState(earCorrection = ears, bypass = true, matchBypass = match), 0.0)
            assertTrue(bypassed.filters.isEmpty())
            assertTrue(bypassed.rightFilters.isEmpty())
        }
    }

    @Test
    fun earCorrectionSurvivesPresetChangesButNotAReset() {
        val state = AppState(earCorrection = ears)
        assertEquals(ears, state.copy(current = libertyBass).earCorrection)
        assertNull(state.resetSound().earCorrection)
    }

    @Test
    fun backupKeepsEarCorrectionAndChecksIt() {
        val restored = BackupCodec.merge(AppState(), BackupCodec.decode(BackupCodec.encode(AppState(earCorrection = ears)))!!)
        assertEquals(ears, restored.earCorrection)
        // A backup without one doesn't remove the one already in use.
        assertEquals(ears, BackupCodec.merge(AppState(earCorrection = ears), BackupCodec.decode("{}")!!).earCorrection)

        val edited = """{"earCorrection":{"left":[{"hz":1000.0,"db":90.0}],"right":[]}}"""
        assertEquals(6.0, BackupCodec.decode(edited)!!.earCorrection!!.left.single().db, 0.0)
    }

    @Test
    fun graphicEqFilesAreReadAtFullDetail() {
        val points = ShapeText.parseGraphicEq("Some header\nGraphicEQ: 20 -3.5; 25 1; 1000 0; 20000 -12")!!
        assertEquals(
            listOf(ShapePoint(20.0, -3.5), ShapePoint(25.0, 1.0), ShapePoint(1000.0, 0.0), ShapePoint(20000.0, -12.0)),
            points,
        )
        val detailed = "GraphicEQ: " + (0 until 127).joinToString("; ") { "${20 + it * 150} ${it % 7}" }
        assertEquals(127, ShapeText.parseGraphicEq(detailed)!!.size)
    }

    @Test
    fun badGraphicEqFilesAreRefused() {
        assertNull(ShapeText.parseGraphicEq("Preamp: -3 dB\nFilter 1: ON PK Fc 100 Hz Gain 2 dB Q 1"))
        assertNull(ShapeText.parseGraphicEq("GraphicEQ:"))
        assertNull(ShapeText.parseGraphicEq("GraphicEQ: 20 -3; nonsense"))
        assertNull(ShapeText.parseGraphicEq("GraphicEQ: " + (0 until 513).joinToString("; ") { "${20 + it} 1" }))
        assertEquals(30.0, ShapeText.parseGraphicEq("GraphicEQ: 100 900")!!.single().db, 0.0)
    }

    @Test
    fun exportedSoundReadsBackTheSame() {
        val preset = libertyBass.copy(tone = Tone(graphicDb = List(10) { if (it < 3) 4.0 else 0.0 }, graphicBands = 10, strength = 0.8))
        val exported = Processing.exportCurve(preset)
        assertEquals(127, exported.size)
        assertEquals(127, exported.map { it.hz }.distinct().size)
        assertEquals(20.0, exported.first().hz, 0.0)
        assertEquals(20000.0, exported.last().hz, 0.0)

        val curve = ShapeText.parseGraphicEq(ShapeText.graphicEq(exported))!!
        val imported = Preset("user:x", "Imported", tone = Tone(importedCurve = curve))
        val filters = Processing.presetFilters(imported)
        for (point in exported) {
            assertEquals(point.db, Eq.curveDb(filters, point.hz) + Processing.preampOf(imported), 0.051)
        }
    }

    @Test
    fun importedCurveFollowsCorrectionAndStrength() {
        val curve = listOf(ShapePoint(100.0, 6.0), ShapePoint(1000.0, 6.0))
        fun at(tone: Tone) = Eq.curveDb(Processing.presetFilters(Preset("user:x", "X", tone = tone)), 500.0)
        assertEquals(6.0, at(Tone(importedCurve = curve)), 0.001)
        assertEquals(3.0, at(Tone(importedCurve = curve, correction = 0.5)), 0.001)
        assertEquals(3.0, at(Tone(importedCurve = curve, strength = 0.5)), 0.001)
        assertEquals(0.0, at(Tone(importedCurve = curve, correction = 0.0)), 0.0)
    }

    @Test
    fun importedCurveSurvivesShareCodesAndIsChecked() {
        val curve = listOf(ShapePoint(100.0, 6.0), ShapePoint(1000.0, -2.5))
        val preset = Preset("user:x", "X", tone = Tone(importedCurve = curve))
        assertEquals(curve, ShareCode.decode(ShareCode.encode(preset))!!.tone.importedCurve)

        val edited = Preset("user:x", "X", tone = Tone(importedCurve = listOf(ShapePoint(100.0, 500.0), ShapePoint(1.0, 1.0))))
        assertEquals(listOf(ShapePoint(100.0, 30.0)), ShareCode.decode(ShareCode.encode(edited))!!.tone.importedCurve)
    }

    @Test
    fun oldSavedStatesStillLoad() {
        val json = kotlinx.serialization.json.Json { ignoreUnknownKeys = true }
        val state = json.decodeFromString(AppState.serializer(), """{"enabled":true,"favoriteShapes":["pop"]}""")
        assertEquals(0, state.stateVersion)
        assertEquals(1.0, state.current.tone.correction, 0.0)
        assertNotNull(state.nightSchedule)
    }
}
