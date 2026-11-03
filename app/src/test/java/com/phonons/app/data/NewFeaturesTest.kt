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

    @Test
    fun compressorCurveFollowsThresholdAndRatio() {
        val band = CompressorBand(thresholdDb = -20.0, ratio = 4.0, kneeDb = 0.0, makeupDb = 0.0)
        assertEquals(-40.0, band.outputDb(-40.0), 0.0001)
        assertEquals(-20.0, band.outputDb(-20.0), 0.0001)
        assertEquals(-15.0, band.outputDb(0.0), 0.0001)
        assertEquals(-12.0, band.copy(makeupDb = 3.0).outputDb(0.0), 0.0001)
        assertEquals(0.0, band.copy(enabled = false).outputDb(0.0), 0.0)
    }

    @Test
    fun compressorKneeIsSmoothAndNeverBoosts() {
        val band = CompressorBand(thresholdDb = -20.0, ratio = 4.0, kneeDb = 12.0)
        // Meets the straight lines at both edges of the knee.
        assertEquals(-26.0, band.outputDb(-26.0), 0.0001)
        assertEquals(-20.0 + 6.0 / 4, band.outputDb(-14.0), 0.0001)
        var previous = band.outputDb(-60.0)
        for (step in 1..600) {
            val input = -60.0 + step / 10.0
            val output = band.outputDb(input)
            assertTrue(output <= input + 0.0001)
            assertTrue("Should only rise at $input", output >= previous)
            assertTrue("Should have no jumps at $input", output - previous < 0.11)
            previous = output
        }
    }

    @Test
    fun compressorStartsOffAndDoesNothing() {
        val target = Processing.target(AppState(), 0.0)
        assertFalse(target.dynamics.enabled)
    }

    @Test
    fun nightModeWinsThenManualThenLeveller() {
        val manual = Dynamics.styles.first { it.name == "Punchy" }.compressor
        assertEquals(manual, Dynamics.resolve(0.0, 0.0, manual))
        assertEquals(manual, Dynamics.resolve(0.0, 0.5, manual))

        val night = Dynamics.resolve(0.5, 0.5, manual)
        assertTrue(night.enabled)
        assertEquals(-15.0, night.low.thresholdDb, 0.0)

        val leveller = Dynamics.resolve(0.0, 0.5, manual.copy(enabled = false))
        assertTrue(leveller.enabled)
        assertEquals(-12.0, leveller.mid.thresholdDb, 0.0)

        assertFalse(Dynamics.resolve(0.0, 0.0, manual.copy(enabled = false)).enabled)
    }

    /** Night mode and the leveller moved into shared code, and must sound exactly as they did before. */
    @Test
    fun nightModeAndLevellerKeepTheirOriginalSettings() {
        val night = Dynamics.resolve(1.0, 0.0, Compressor()).high
        assertEquals(CompressorBand(thresholdDb = -30.0, ratio = 4.0, attackMs = 8.0, releaseMs = 120.0, kneeDb = 6.0, makeupDb = 9.0), night)
        val leveller = Dynamics.resolve(0.0, 1.0, Compressor()).low
        assertEquals(CompressorBand(thresholdDb = -24.0, ratio = 3.0, attackMs = 50.0, releaseMs = 1000.0, kneeDb = 6.0, makeupDb = 6.0), leveller)
    }

    @Test
    fun scheduledNightOverridesTheManualCompressor() {
        val manual = Dynamics.styles.first().compressor
        val state = AppState(compressor = manual, nightSchedule = NightSchedule(enabled = true, amount = 0.6))
        assertEquals(manual, Processing.target(state, 0.0).dynamics)
        assertEquals(-18.0, Processing.target(state, 0.0, scheduledNight = true).dynamics.low.thresholdDb, 0.0001)
    }

    @Test
    fun bypassAndTheHearingCheckTurnTheCompressorOff() {
        val state = AppState(compressor = Dynamics.styles.first().compressor)
        assertTrue(Processing.target(state, 0.0).dynamics.enabled)
        for (match in listOf(true, false)) {
            assertFalse(Processing.target(state.copy(bypass = true, matchBypass = match), 0.0).dynamics.enabled)
        }
    }

    @Test
    fun everyCompressorStyleIsValid() {
        assertEquals(Dynamics.styles.size, Dynamics.styles.map { it.name }.distinct().size)
        for (style in Dynamics.styles) {
            assertTrue(style.name, style.compressor.enabled)
            assertTrue(style.name, style.compressor.bands.any { it.enabled })
            assertEquals(style.name, style.compressor, style.compressor.clamped())
            assertTrue(style.name, style.compressor.lowCrossoverHz < style.compressor.highCrossoverHz)
        }
    }

    @Test
    fun crossoverLimitsKeepBandsInOrder() {
        assertTrue(Dynamics.LOW_CROSSOVER_HZ.endInclusive < Dynamics.HIGH_CROSSOVER_HZ.start)
        val wild = Compressor(lowCrossoverHz = 99999.0, highCrossoverHz = 1.0).clamped()
        assertTrue(wild.lowCrossoverHz < wild.highCrossoverHz)
    }

    @Test
    fun compressorSurvivesBackupAndIsChecked() {
        val punchy = Dynamics.styles.first { it.name == "Punchy" }.compressor
        val restored = BackupCodec.merge(AppState(), BackupCodec.decode(BackupCodec.encode(AppState(compressor = punchy)))!!)
        assertEquals(punchy, restored.compressor)
        // An older backup leaves the current settings alone.
        assertEquals(punchy, BackupCodec.merge(AppState(compressor = punchy), BackupCodec.decode("{}")!!).compressor)

        val edited = """{"compressor":{"enabled":true,"low":{"ratio":500.0,"makeupDb":90.0,"thresholdDb":-500.0}}}"""
        val low = BackupCodec.decode(edited)!!.compressor!!.low
        assertEquals(Dynamics.RATIO.endInclusive, low.ratio, 0.0)
        assertEquals(Dynamics.MAKEUP_DB.endInclusive, low.makeupDb, 0.0)
        assertEquals(Dynamics.THRESHOLD_DB.start, low.thresholdDb, 0.0)
    }

    @Test
    fun resetTurnsTheCompressorOff() {
        assertFalse(AppState(compressor = Dynamics.styles.first().compressor).resetSound().compressor.enabled)
    }

    private val warmFaders = List(10) { if (it < 3) 3.0 else 0.0 }

    @Test
    fun faderLabelNamesTheAppliedShape() {
        assertEquals("Flat", Tone().faderLabel())
        assertEquals("Warm", Tone().withShape("Warm", warmFaders, 10).faderLabel())
        // Faders with no name stay honest about it.
        assertEquals("Custom", Tone(graphicDb = warmFaders, graphicBands = 10).faderLabel())
    }

    @Test
    fun faderLabelShowsAnEditOnceAFaderMoves() {
        val tone = Tone().withShape("Warm", warmFaders, 10)
        val moved = warmFaders.toMutableList().also { it[5] = 2.0 }
        assertEquals("Warm, edited", tone.copy(graphicDb = moved).faderLabel())
        // Putting it back exactly means it's the shape again.
        assertEquals("Warm", tone.copy(graphicDb = moved).copy(graphicDb = warmFaders).faderLabel())
    }

    @Test
    fun faderLabelFollowsLayers() {
        val warm = Tone().withShape("Warm", warmFaders, 10)
        val both = warm.withShape("Bright at 60%", warmFaders.map { it + 1.0 }, 10, layered = true)
        assertEquals("Warm + Bright at 60%", both.faderLabel())

        val edited = warm.copy(graphicDb = warmFaders.map { it + 2.0 })
        val onEdited = edited.withShape("Bright", warmFaders.map { it + 3.0 }, 10, layered = true)
        assertEquals("Warm, edited + Bright", onEdited.faderLabel())

        // Layering onto nothing is just the shape.
        assertEquals("Bright", Tone().withShape("Bright", warmFaders, 10, layered = true).faderLabel())
        val long = Tone().withShape("x".repeat(500), warmFaders, 10)
        assertEquals(Tone.MAX_SHAPE_LABEL, long.faderLabel().length)
    }

    @Test
    fun faderLabelRecognisesSoundsSavedBeforeNamesWere() {
        val shape = com.phonons.app.dsp.GraphicEq.shapes.first { it.name == "Warm" }
        val saved = Tone(graphicDb = com.phonons.app.dsp.GraphicEq.apply(shape, 15), graphicBands = 15)
        assertEquals("Warm", saved.faderLabel(com.phonons.app.dsp.GraphicEq.shapes))
    }

    @Test
    fun aFlatShapeHasNoName() {
        assertNull(Tone().withShape("Flat", List(10) { 0.0 }, 10).shape)
    }

    @Test
    fun theShapeNameTravelsWithThePreset() {
        val tone = Tone().withShape("Warm", warmFaders, 10)
        val preset = Preset("user:x", "X", tone = tone)
        assertEquals("Warm", ShareCode.decode(ShareCode.encode(preset))!!.tone.faderLabel())

        val backup = BackupCodec.decode(BackupCodec.encode(AppState(presets = listOf(preset))))!!
        assertEquals("Warm", backup.presets.single().tone.faderLabel())

        val longName = Preset("user:x", "X", tone = tone.copy(shape = AppliedShape("y".repeat(500), warmFaders)))
        assertEquals(Tone.MAX_SHAPE_LABEL, ShareCode.decode(ShareCode.encode(longName))!!.tone.shape!!.name.length)
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
