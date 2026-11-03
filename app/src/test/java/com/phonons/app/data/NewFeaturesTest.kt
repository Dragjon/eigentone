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
    fun theSweepCoversTheWholeRange() {
        assertEquals(Eq.MIN_FREQ, Sweep.hzAt(0.0), 0.001)
        assertEquals(Eq.MAX_FREQ, Sweep.hzAt(1.0), 0.001)
        assertEquals(Eq.MAX_FREQ, Sweep.hzAt(5.0), 0.001)
        // Each step along it is the same musical distance.
        assertEquals(Sweep.hzAt(0.5) / Sweep.hzAt(0.25), Sweep.hzAt(0.75) / Sweep.hzAt(0.5), 0.001)
    }

    /** Runs the whole test with a listener who hears these spots as different, and returns what it found. */
    private fun listen(
        finder: PeakFinder,
        hears: (Double) -> SpotAnswer,
        narrow: (PeakFinder.Question.Narrow) -> SidePick = { SidePick.SAME },
    ): List<SweepMark> {
        var questions = 0
        while (!finder.isDone) {
            when (val question = finder.question!!) {
                is PeakFinder.Question.Spot -> finder.answer(hears(question.hz))
                is PeakFinder.Question.Narrow -> finder.pick(narrow(question))
            }
            assertTrue("The test must end", ++questions < 500)
        }
        return finder.marks()
    }

    private fun near(hz: Double, target: Double) = abs(kotlin.math.log2(hz / target)) < 0.13

    @Test
    fun theFinderCoversTheUsefulRange() {
        val spots = PeakFinder.spots()
        assertEquals(PeakFinder.LOWEST_HZ, spots.first(), 0.0)
        assertTrue(spots.last() <= PeakFinder.HIGHEST_HZ)
        assertTrue(spots.size in 20..30)
    }

    @Test
    fun aListenerHearingNothingGetsNoFilters() {
        assertTrue(listen(PeakFinder(kotlin.random.Random(1)), { SpotAnswer.SAME }).isEmpty())
    }

    @Test
    fun aConsistentPeakAndDipAreFoundWithTheRightDirection() {
        val marks = listen(PeakFinder(kotlin.random.Random(2)), { hz ->
            when {
                near(hz, 3400.0) -> SpotAnswer.STANDS_OUT
                near(hz, 800.0) -> SpotAnswer.WEAKER
                else -> SpotAnswer.SAME
            }
        })
        assertEquals(2, marks.size)
        assertTrue(near(marks[0].hz, 800.0))
        assertEquals(Sweep.TOO_QUIET_DB, marks[0].gainDb, 0.0)
        assertTrue(near(marks[1].hz, 3400.0))
        assertEquals(Sweep.TOO_LOUD_DB, marks[1].gainDb, 0.0)
    }

    @Test
    fun aChanceAnswerIsDroppedWhenCheckedAgain() {
        var firstTime = true
        val marks = listen(PeakFinder(kotlin.random.Random(3)), { hz ->
            if (near(hz, 2000.0) && firstTime) {
                firstTime = false
                SpotAnswer.STANDS_OUT
            } else {
                SpotAnswer.SAME
            }
        })
        assertTrue(marks.isEmpty())
    }

    @Test
    fun changingDirectionWhenCheckedAgainIsDropped() {
        val answers = mutableMapOf<Double, Int>()
        val marks = listen(PeakFinder(kotlin.random.Random(4)), { hz ->
            val times = answers.getOrDefault(hz, 0)
            answers[hz] = times + 1
            if (times == 0) SpotAnswer.STANDS_OUT else SpotAnswer.WEAKER
        })
        assertTrue(marks.isEmpty())
    }

    @Test
    fun narrowingMovesTheMarkToThePickedSide() {
        val hears = { hz: Double -> if (near(hz, 2000.0)) SpotAnswer.STANDS_OUT else SpotAnswer.SAME }
        val same = listen(PeakFinder(kotlin.random.Random(5)), hears).single().hz
        val first = listen(PeakFinder(kotlin.random.Random(5)), hears, { SidePick.FIRST }).single().hz
        val second = listen(PeakFinder(kotlin.random.Random(5)), hears, { SidePick.SECOND }).single().hz
        assertTrue(first < same)
        assertTrue(second > same)
    }

    @Test
    fun theOrderIsShuffledSoItCantBePredicted() {
        fun order(seed: Int): List<Double> {
            val finder = PeakFinder(kotlin.random.Random(seed))
            val heard = mutableListOf<Double>()
            while (!finder.isDone) {
                heard += finder.question!!.hz
                finder.answer(SpotAnswer.SAME)
            }
            return heard
        }
        assertTrue(order(1) != order(2))
        assertEquals(PeakFinder.spots().sorted(), order(1).sorted())
    }

    @Test
    fun theStepCountOnlyGrowsWhenSomethingIsFlagged() {
        val finder = PeakFinder(kotlin.random.Random(6))
        val start = finder.total
        assertEquals(PeakFinder.spots().size, start)
        finder.answer(SpotAnswer.SAME)
        assertEquals(start, finder.total)
        finder.answer(SpotAnswer.STANDS_OUT)
        assertEquals(start + 2, finder.total)
    }

    @Test
    fun answeringTheWrongKindOfQuestionDoesNothing() {
        val finder = PeakFinder(kotlin.random.Random(7))
        val question = finder.question
        finder.pick(SidePick.FIRST)
        assertEquals(question, finder.question)
        assertEquals(0, finder.answered)
    }

    @Test
    fun nearbyMarksFromTheFinderDontStack() {
        val marks = listen(PeakFinder(kotlin.random.Random(8)), { SpotAnswer.STANDS_OUT })
        for (i in 1 until marks.size) {
            assertTrue(kotlin.math.log2(marks[i].hz / marks[i - 1].hz) >= 1.0 / 6.0)
        }
        assertTrue(marks.all { it.gainDb == Sweep.TOO_LOUD_DB })
    }

    @Test
    fun nearbyNotesAlternateAroundTheSpot() {
        val tones = Sweep.nearbyTones(1000.0)
        assertEquals(4, tones.size)
        assertEquals(1000.0, tones[0], 0.0)
        assertEquals(1000.0, tones[2], 0.0)
        assertEquals(1000.0 / Math.pow(2.0, 1.0 / 3), tones[1], 0.01)
        assertEquals(1000.0 * Math.pow(2.0, 1.0 / 3), tones[3], 0.01)
    }

    @Test
    fun nearbyNotesStayInsideTheRange() {
        assertEquals(Eq.MIN_FREQ, Sweep.nearbyTones(Eq.MIN_FREQ)[1], 0.0)
        assertEquals(Eq.MAX_FREQ, Sweep.nearbyTones(Eq.MAX_FREQ)[3], 0.0)
    }

    @Test
    fun marksAtDifferentSpotsStaySeparateAndInOrder() {
        val marks = Sweep.addMark(Sweep.addMark(emptyList(), 6000.0, -3.0), 200.0, 2.0)
        assertEquals(listOf(200.0, 6000.0), marks.map { it.hz })
    }

    @Test
    fun markingTheSameSpotGoesFurther() {
        var marks = Sweep.addMark(emptyList(), 6000.0, -3.0)
        marks = Sweep.addMark(marks, 6100.0, -3.0)
        assertEquals(1, marks.size)
        assertEquals(-6.0, marks.single().gainDb, 0.0)
        repeat(10) { marks = Sweep.addMark(marks, 6000.0, -3.0) }
        assertEquals(-9.0, marks.single().gainDb, 0.0)
    }

    @Test
    fun oppositeMarksAtTheSameSpotCancelOut() {
        val marks = Sweep.addMark(Sweep.addMark(emptyList(), 1000.0, 2.0), 1000.0, -2.0)
        assertTrue(marks.isEmpty())
    }

    @Test
    fun marksBecomeNarrowFiltersBesideTheOnesAlreadyThere() {
        val existing = listOf(com.phonons.app.dsp.Filter(7, com.phonons.app.dsp.FilterType.BELL, 100.0, 1.0, 1.0))
        val filters = Sweep.addTo(existing, listOf(SweepMark(6000.0, -3.0), SweepMark(9000.0, 2.0)))
        assertEquals(3, filters.size)
        assertEquals(existing.single(), filters[0])
        assertEquals(listOf(7, 8, 9), filters.map { it.id })
        assertEquals(-3.0, Eq.curveDb(listOf(filters[1]), 6000.0), 0.01)
        // Narrow, so it barely touches a frequency an octave away.
        assertTrue(abs(Eq.curveDb(listOf(filters[1]), 3000.0)) < 0.3)
    }

    @Test
    fun marksStopAtThePresetsFilterLimit() {
        val full = List(19) { com.phonons.app.dsp.Filter(it + 1, com.phonons.app.dsp.FilterType.BELL, 100.0 + it, 0.0, 1.0) }
        assertEquals(1, Sweep.roomFor(full))
        assertEquals(20, Sweep.addTo(full, listOf(SweepMark(500.0, 2.0), SweepMark(900.0, 2.0))).size)
        assertEquals(0, Sweep.roomFor(full + full))
    }

    private fun tone(oscillator: SweepOscillator, hz: Double, db: Double, sounding: Boolean = true, frames: Int = 4800): FloatArray {
        val buffer = FloatArray(frames * 2)
        oscillator.fill(buffer, hz, db, sounding)
        return buffer
    }

    @Test
    fun theToneNeverGoesAboveTheSafetyLimit() {
        val oscillator = SweepOscillator(48000)
        repeat(10) {
            val limit = Math.pow(10.0, Sweep.MAX_LEVEL_DB / 20)
            assertTrue(tone(oscillator, 1000.0, 20.0).all { abs(it) <= limit + 0.0001 })
        }
    }

    @Test
    fun theToneFadesInAndOutWithoutClicks() {
        val oscillator = SweepOscillator(48000)
        val start = tone(oscillator, 1000.0, -30.0)
        assertTrue(abs(start[0]) < 0.001)
        assertFalse(oscillator.isSilent)
        val end = tone(oscillator, 1000.0, -30.0, sounding = false)
        assertTrue(oscillator.isSilent)
        assertTrue(abs(end[end.size - 2]) < 0.0001)
    }

    /** Counts upward zero crossings, which gives the pitch of a tone. */
    private fun measuredHz(samples: FloatArray, sampleRate: Int): Double {
        var crossings = 0
        for (i in 2 until samples.size step 2) if (samples[i - 2] < 0f && samples[i] >= 0f) crossings++
        return crossings * sampleRate.toDouble() / (samples.size / 2)
    }

    @Test
    fun aNewNoteStartsOnItsPitchAfterAGap() {
        val oscillator = SweepOscillator(48000)
        tone(oscillator, 500.0, -30.0)
        tone(oscillator, 500.0, -30.0, sounding = false)
        assertTrue(oscillator.isSilent)
        // Only the first tenth of a second, which is when a slide would be heard.
        val note = tone(oscillator, 4000.0, -30.0, frames = 4800)
        assertEquals(4000.0, measuredHz(note, 48000), 40.0)
    }

    @Test
    fun theToneMovesSmoothlyWhenThePitchJumps() {
        val oscillator = SweepOscillator(48000)
        tone(oscillator, 500.0, -30.0)
        val jumped = tone(oscillator, 8000.0, -30.0)
        val amplitude = Math.pow(10.0, -30.0 / 20)
        // No sample leaps further than even the highest pitch here could move in one step.
        val largestAllowed = amplitude * 2 * Math.PI * 8000.0 / 48000 * 1.05
        for (i in 2 until jumped.size step 2) {
            assertTrue(abs(jumped[i] - jumped[i - 2]) <= largestAllowed)
        }
    }

    private val boosted = Preset("user:b", "Boost", tone = Tone(bassDb = 6.0))

    @Test
    fun peaksAreFlaggedOnlyWhenTheyCanGoOver() {
        val noRoom = AppState(current = boosted, autoHeadroom = false, limiter = false)
        val clipping = Processing.peakEstimate(noRoom, 0.0, BUDS, false)
        assertEquals(ClipRisk.CLIPPING, clipping.risk)
        assertTrue(clipping.peakDb > 4.0)

        assertEquals(ClipRisk.HELD, Processing.peakEstimate(noRoom.copy(limiter = true), 0.0, BUDS, false).risk)
        assertEquals(ClipRisk.NONE, Processing.peakEstimate(noRoom.copy(autoHeadroom = true), 0.0, BUDS, false).risk)
        assertEquals(ClipRisk.NONE, Processing.peakEstimate(noRoom.copy(bypass = true), 0.0, BUDS, false).risk)
    }

    @Test
    fun compressorMakeupCanGoOverDespiteAutomaticHeadroom() {
        val makeup = CompressorBand(ratio = 1.0, makeupDb = 6.0)
        val preset = Preset("user:m", "M", tone = Tone(compressor = Compressor(enabled = true, low = makeup, mid = makeup, high = makeup)))
        val estimate = Processing.peakEstimate(AppState(current = preset, autoHeadroom = true, limiter = false), 0.0, BUDS, false)
        assertEquals(ClipRisk.CLIPPING, estimate.risk)
        assertEquals(6.0, estimate.peakDb, 0.1)
    }

    @Test
    fun compressionThatLowersPeaksIsAllowedFor() {
        val hard = CompressorBand(thresholdDb = -30.0, ratio = 8.0, makeupDb = 3.0)
        val preset = boosted.copy(tone = boosted.tone.copy(compressor = Compressor(enabled = true, low = hard, mid = hard, high = hard)))
        val estimate = Processing.peakEstimate(AppState(current = preset, autoHeadroom = false, limiter = false), 0.0, BUDS, false)
        assertEquals(ClipRisk.NONE, estimate.risk)
    }

    @Test
    fun peaksAreCheckedInBothEars() {
        val loudRight = EarCorrection(left = listOf(ShapePoint(1000.0, 0.0)), right = listOf(ShapePoint(1000.0, 6.0)))
        val state = AppState(earCorrections = mapOf(BUDS to loudRight), autoHeadroom = false, limiter = false)
        assertEquals(ClipRisk.CLIPPING, Processing.peakEstimate(state, 0.0, BUDS, false).risk)
    }

    @Test
    fun filtersExportAndReadBackTheSame() {
        val filters = listOf(
            com.phonons.app.dsp.Filter(1, com.phonons.app.dsp.FilterType.BELL, 1000.0, -3.5, 1.41),
            com.phonons.app.dsp.Filter(2, com.phonons.app.dsp.FilterType.LOW_SHELF, 105.0, 4.0, 0.7),
            com.phonons.app.dsp.Filter(3, com.phonons.app.dsp.FilterType.HIGH_SHELF, 9000.0, -2.0, 0.7, enabled = false),
            com.phonons.app.dsp.Filter(4, com.phonons.app.dsp.FilterType.HIGH_PASS, 30.0, 0.0, 0.71),
        )
        val text = com.phonons.app.dsp.AutoEqParser.format(-4.5, filters)
        val parsed = com.phonons.app.dsp.AutoEqParser.parse(text)!!
        assertEquals(-4.5, parsed.preampDb, 0.0)
        assertEquals(filters.map { it.type }, parsed.filters.map { it.type })
        assertEquals(filters.map { it.enabled }, parsed.filters.map { it.enabled })
        for ((a, b) in filters.zip(parsed.filters)) {
            assertEquals(a.freq, b.freq, 0.01)
            assertEquals(a.gainDb, b.gainDb, 0.01)
            assertEquals(a.q, b.q, 0.001)
        }
    }

    @Test
    fun onlyPresetsMadeOfFiltersCanExportThem() {
        assertNull(Processing.exportFilters(Preset("user:g", "G", tone = Tone(graphicDb = List(10) { 2.0 }, graphicBands = 10))))
        assertNull(Processing.exportFilters(Preset("user:c", "C", tone = Tone(importedCurve = listOf(ShapePoint(100.0, 2.0))))))
        assertNotNull(Processing.exportFilters(libertyBass.copy(tone = Tone())))
    }

    @Test
    fun undoAndRedoWalkThroughHistory() {
        val a = Preset("user:ha", "A")
        val b = Preset("user:hb", "B")
        val c = Preset("user:hc", "C")
        Repo.loadPreset(a)
        Repo.loadPreset(b)
        Repo.undo()
        assertEquals(a, Repo.state.value.current)
        assertTrue(Repo.canRedo.value)
        Repo.redo()
        assertEquals(b, Repo.state.value.current)
        assertFalse(Repo.canRedo.value)

        // A new change after undoing means the undone step can't come back.
        Repo.undo()
        Repo.loadPreset(c)
        assertFalse(Repo.canRedo.value)
        Repo.redo()
        assertEquals(c, Repo.state.value.current)
        Repo.undo()
        assertEquals(a, Repo.state.value.current)
    }

    @Test
    fun theTopStripListsWhatsActuallyRunning() {
        val comp = Dynamics.styles.first().compressor
        val full = AppState(
            enabled = true,
            current = Preset("user:a", "A", tone = Tone(surround = 0.3, compressor = comp)),
            earCorrections = mapOf(BUDS to ears),
        )
        assertEquals(listOf("Comp", "Width", "Ears"), Processing.activeEffects(full, BUDS))
        assertEquals(listOf("Comp", "Surround", "Ears"), Processing.activeEffects(full.copy(useSystemSurround = true), BUDS))
        // Ear correction belongs to its own headphones.
        assertEquals(listOf("Comp", "Width"), Processing.activeEffects(full, "Bluetooth:Other"))
        assertEquals(listOf("Comp", "Width"), Processing.activeEffects(full.copy(earCorrections = mapOf(BUDS to ears.copy(enabled = false))), BUDS))
    }

    @Test
    fun theTopStripOnlyShowsTheCompressorEffectThatWon() {
        val comp = Dynamics.styles.first().compressor
        val state = AppState(enabled = true, current = Preset("user:a", "A", tone = Tone(compressor = comp)), leveller = 0.5)
        assertEquals(listOf("Comp"), Processing.activeEffects(state, BUDS))
        assertEquals(listOf("Night"), Processing.activeEffects(state, BUDS, scheduledNight = true))
        assertEquals(listOf("Night"), Processing.activeEffects(state.copy(nightMode = 0.4), BUDS))
        assertEquals(listOf("Level"), Processing.activeEffects(state.copy(current = quiet), BUDS))
    }

    @Test
    fun theTopStripIsEmptyWhenNothingIsChangingTheSound() {
        val state = AppState(enabled = true, nightMode = 0.5, current = Preset("user:a", "A", tone = Tone(surround = 0.3)))
        assertTrue(Processing.activeEffects(state.copy(enabled = false), BUDS).isEmpty())
        assertTrue(Processing.activeEffects(state.copy(bypass = true), BUDS).isEmpty())
        assertTrue(Processing.activeEffects(AppState(enabled = true), BUDS).isEmpty())
    }

    @Test
    fun scheduleTimesReadLikeAClock() {
        assertEquals("22:00", NightSchedule.formatTime(22 * 60, is24Hour = true))
        assertEquals("07:05", NightSchedule.formatTime(7 * 60 + 5, is24Hour = true))
        assertEquals("00:00", NightSchedule.formatTime(0, is24Hour = true))
        assertEquals("10:00 PM", NightSchedule.formatTime(22 * 60, is24Hour = false))
        assertEquals("7:05 AM", NightSchedule.formatTime(7 * 60 + 5, is24Hour = false))
        assertEquals("12:00 AM", NightSchedule.formatTime(0, is24Hour = false))
        assertEquals("12:30 PM", NightSchedule.formatTime(12 * 60 + 30, is24Hour = false))
    }

    @Test
    fun anyMinuteWorksInTheSchedule() {
        val schedule = NightSchedule(enabled = true, startMinute = 22 * 60 + 17, endMinute = 6 * 60 + 43)
        assertFalse(schedule.isActive(22 * 60 + 16))
        assertTrue(schedule.isActive(22 * 60 + 17))
        assertTrue(schedule.isActive(6 * 60 + 42))
        assertFalse(schedule.isActive(6 * 60 + 43))
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

    private fun compressed(c: Compressor) = Preset("user:c", "Compressed", tone = Tone(compressor = c))

    @Test
    fun scheduledNightOverridesTheManualCompressor() {
        val manual = Dynamics.styles.first().compressor
        val state = AppState(current = compressed(manual), nightSchedule = NightSchedule(enabled = true, amount = 0.6))
        assertEquals(manual, Processing.target(state, 0.0).dynamics)
        assertEquals(-18.0, Processing.target(state, 0.0, scheduledNight = true).dynamics.low.thresholdDb, 0.0001)
    }

    @Test
    fun eachPresetUsesItsOwnCompressor() {
        val punchy = Dynamics.styles.first { it.name == "Punchy" }.compressor
        val state = AppState(current = compressed(punchy))
        assertEquals(punchy, Processing.target(state, 0.0).dynamics)
        assertFalse(Processing.target(state.copy(current = BuiltIns.flat), 0.0).dynamics.enabled)
    }

    @Test
    fun presetsAreSavedWithTheirCompressor() {
        val punchy = Dynamics.styles.first { it.name == "Punchy" }.compressor
        val preset = compressed(punchy)
        assertEquals(punchy, ShareCode.decode(ShareCode.encode(preset))!!.tone.compressor)
        val backup = BackupCodec.decode(BackupCodec.encode(AppState(presets = listOf(preset))))!!
        assertEquals(punchy, backup.presets.single().tone.compressor)
    }

    @Test
    fun sharedCompressorSettingsAreChecked() {
        val wild = compressed(Compressor(enabled = true, low = CompressorBand(ratio = 500.0, makeupDb = 90.0)))
        val low = ShareCode.decode(ShareCode.encode(wild))!!.tone.compressor.low
        assertEquals(Dynamics.RATIO.endInclusive, low.ratio, 0.0)
        assertEquals(Dynamics.MAKEUP_DB.endInclusive, low.makeupDb, 0.0)
    }

    @Test
    fun aGlobalCompressorThatWasOnMovesIntoEveryPreset() {
        val punchy = Dynamics.styles.first { it.name == "Punchy" }.compressor
        val own = Dynamics.styles.first { it.name == "Gentle" }.compressor
        val old = AppState(
            stateVersion = 1,
            compressor = punchy,
            current = quiet,
            presets = listOf(boomy, compressed(own)),
        ).migrated()
        assertEquals(punchy, old.current.tone.compressor)
        assertEquals(punchy, old.presets[0].tone.compressor)
        // Already has its own settings, so they're left alone.
        assertEquals(own, old.presets[1].tone.compressor)
        assertEquals(Compressor(), old.compressor)
        assertEquals(old, old.migrated())
    }

    @Test
    fun aGlobalCompressorThatWasOffChangesNothing() {
        val leftover = Dynamics.styles.first { it.name == "Punchy" }.compressor.copy(enabled = false)
        val old = AppState(stateVersion = 1, compressor = leftover, current = quiet).migrated()
        assertEquals(Compressor(), old.current.tone.compressor)
        assertEquals(Compressor(), old.compressor)
    }

    @Test
    fun bypassAndTheHearingCheckTurnTheCompressorOff() {
        val state = AppState(current = compressed(Dynamics.styles.first().compressor))
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
    fun anOlderBackupsCompressorMovesIntoItsPresets() {
        val text = """{"presets":[{"id":"user:a","name":"A"}],"compressor":{"enabled":true,"low":{"ratio":500.0,"makeupDb":90.0,"thresholdDb":-500.0}}}"""
        val restored = BackupCodec.merge(AppState(), BackupCodec.decode(text)!!)
        val low = restored.presets.single().tone.compressor.low
        assertEquals(Dynamics.RATIO.endInclusive, low.ratio, 0.0)
        assertEquals(Dynamics.MAKEUP_DB.endInclusive, low.makeupDb, 0.0)
        assertEquals(Dynamics.THRESHOLD_DB.start, low.thresholdDb, 0.0)
        assertEquals(Compressor(), restored.compressor)
    }

    @Test
    fun resetTurnsTheCompressorOff() {
        val state = AppState(current = compressed(Dynamics.styles.first().compressor))
        assertFalse(state.resetSound().current.tone.compressor.enabled)
    }

    private val unevenEars = HearingResult(
        mapOf(
            Ear.LEFT to mapOf(1000.0 to -60.0, 4000.0 to -60.0),
            Ear.RIGHT to mapOf(1000.0 to -60.0, 4000.0 to -40.0),
        ),
    )

    @Test
    fun aSavedHearingCheckGivesTheSameEarCorrectionLater() {
        val record = HearingRecord.of(unevenEars, "Liberty 4 NC", 1234L)
        assertEquals(unevenEars.earCorrection(), record.result()!!.earCorrection())
        assertEquals("Liberty 4 NC", record.headphones)
    }

    @Test
    fun aSavedHearingCheckSurvivesRestarts() {
        val json = kotlinx.serialization.json.Json { ignoreUnknownKeys = true }
        val state = AppState(hearings = mapOf(BUDS to HearingRecord.of(unevenEars, "Buds", 1L)))
        val loaded = json.decodeFromString(AppState.serializer(), json.encodeToString(AppState.serializer(), state))
        assertNotNull(loaded.hearings.getValue(BUDS).result()!!.earCorrection())
        assertTrue(json.decodeFromString(AppState.serializer(), "{}").hearings.isEmpty())
    }

    @Test
    fun evenEarsHaveNothingToCorrect() {
        val even = HearingResult(Ear.entries.associateWith { mapOf(1000.0 to -60.0) })
        val result = HearingRecord.of(even, "Buds", 1L).result()!!
        assertFalse(result.earCorrectionIsRecommended())
        assertFalse(result.earCorrection().hasChanges)
    }

    @Test
    fun resettingTheSoundKeepsTheMeasurement() {
        val record = HearingRecord.of(unevenEars, "Buds", 1L)
        assertEquals(record, AppState(hearings = mapOf(BUDS to record)).resetSound().hearings[BUDS])
    }

    @Test
    fun aBackupKeepsTheHearingCheckAndCantBreakIt() {
        val record = HearingRecord.of(unevenEars, "Buds", 5L)
        val state = AppState(hearings = mapOf(BUDS to record))
        val restored = BackupCodec.merge(AppState(), BackupCodec.decode(BackupCodec.encode(state))!!)
        assertEquals(record, restored.hearings[BUDS])

        // Ears that don't match would otherwise crash when the correction is worked out.
        val edited = """{"hearings":{"Bluetooth:Buds":{"left":[{"hz":1000.0,"db":-60.0}],"right":[{"hz":2000.0,"db":-40.0}]}}}"""
        assertNull(BackupCodec.decode(edited)!!.hearings.getValue(BUDS).result())
        val empty = """{"hearings":{"Bluetooth:Buds":{"left":[],"right":[]}}}"""
        assertNull(BackupCodec.decode(empty)!!.hearings.getValue(BUDS).result())
    }

    @Test
    fun hearingCheckCountsEveryAnswerSoTheScreenCanShowIt() {
        val test = HearingTest(listOf(1000.0, 2000.0))
        assertEquals(4, test.stepCount)
        assertEquals(1, test.stepNumber)
        assertEquals(1, test.attempt)

        test.answer(heard = true)
        assertEquals(2, test.attempt)
        assertEquals(1, test.stepNumber)
        test.answer(heard = false)
        assertEquals(3, test.attempt)

        // Two misses in a row after hearing one finishes this frequency, and counting starts again.
        test.answer(heard = false)
        assertEquals(2, test.stepNumber)
        assertEquals(1, test.attempt)
    }

    @Test
    fun hearingCheckFinishesWithoutTheCountsOverrunning() {
        val test = HearingTest(listOf(1000.0))
        repeat(500) { if (!test.isDone) test.answer(heard = true) }
        assertTrue(test.isDone)
        assertEquals(test.stepCount, test.stepNumber)
    }

    private val quiet = Preset("user:quiet", "Quiet", tone = Tone(trebleDb = -3.0))
    private val boomy = Preset("user:boomy", "Boomy", tone = Tone(bassDb = 8.0, vocalsDb = 3.0))

    private fun comparedLoudness(preset: Preset): Double {
        val filters = Processing.presetFilters(preset)
        return Processing.estimatedLoudnessDb(filters, Processing.inputOf(preset, filters, true))
    }

    @Test
    fun comparedSoundsPlayAtMatchedLoudness() {
        val comparison = AbComparison(quiet, boomy)
        assertEquals(
            comparedLoudness(comparison.soundFor(0, true)),
            comparedLoudness(comparison.soundFor(1, true)),
            0.05,
        )
    }

    @Test
    fun theFirstSoundPlaysUntouchedAndChoicesStayClean() {
        val comparison = AbComparison(quiet, boomy)
        assertEquals(quiet, comparison.soundFor(0, true))
        assertTrue(comparison.soundFor(1, true).tone.trimDb != 0.0)
        // What gets kept is the saved sound, not the temporary level change.
        assertEquals(boomy, comparison.presetFor(1))
    }

    @Test
    fun openComparisonsNameBothSounds() {
        val comparison = AbComparison(quiet, boomy, blind = false, flipped = true)
        assertEquals(quiet, comparison.presetFor(0))
        assertEquals("A", comparison.label(0))
        assertEquals("B", comparison.label(1))
        assertEquals("Quiet", comparison.nameFor(0))
        assertEquals("Boomy", comparison.nameFor(1))
    }

    @Test
    fun blindComparisonsHideWhichIsWhich() {
        for (flipped in listOf(false, true)) {
            val comparison = AbComparison(quiet, boomy, blind = true, flipped = flipped)
            assertEquals("1", comparison.label(0))
            assertEquals("2", comparison.label(1))
            assertNull(comparison.nameFor(0))
            assertNull(comparison.nameFor(1))
            assertEquals(if (flipped) boomy else quiet, comparison.presetFor(0))
            assertEquals(if (flipped) quiet else boomy, comparison.presetFor(1))
        }
    }

    private val widePreset = Preset("user:w", "Wide", tone = Tone(surround = 1.0))

    @Test
    fun widthKeepsBassCentredAndLeansTheRest() {
        for (hz in listOf(20.0, 60.0, 100.0, 250.0)) assertEquals(0.0, StereoWidth.leanDb(hz), 0.0)
        assertEquals(0.0, StereoWidth.leanDb(16000.0), 0.0)
        val leans = (500..9000 step 50).map { StereoWidth.leanDb(it.toDouble()) }
        assertTrue(leans.any { it > 2.0 })
        assertTrue(leans.any { it < -2.0 })
        assertTrue(leans.all { abs(it) <= StereoWidth.MAX_LEAN_DB })
    }

    @Test
    fun widthMirrorsTheChannelsAndFollowsTheAmount() {
        val (left, right) = StereoWidth.curves(0.5)
        assertEquals(left.size, right.size)
        for (i in left.indices) {
            assertEquals(left[i].hz, right[i].hz, 0.0)
            assertEquals(-left[i].db, right[i].db, 0.0)
            assertTrue(abs(left[i].db) <= StereoWidth.MAX_LEAN_DB / 2 + 0.0001)
        }
        assertEquals(left.map { it.hz }.sorted(), left.map { it.hz })
        // Asking for another amount doesn't get the remembered one.
        assertEquals(left[10].db * 2, StereoWidth.curves(1.0).first[10].db, 0.0001)
    }

    @Test
    fun widthGoesThroughTheChannelsNotThePhonesEffect() {
        val target = Processing.target(AppState(useSystemSurround = false, current = widePreset), 0.0)
        assertEquals(0.0, target.surround, 0.0)
        assertTrue(target.filters !== target.rightFilters)
        // Somewhere between the fades, the two channels really do differ.
        val leaning = listOf(600.0, 800.0, 1200.0, 3000.0).maxOf {
            abs(Eq.curveDb(target.filters, it) - Eq.curveDb(target.rightFilters, it))
        }
        assertTrue(leaning > 2.0)
    }

    @Test
    fun thePhonesEffectReplacesWidthWhenChosen() {
        val target = Processing.target(AppState(current = widePreset, useSystemSurround = true), 0.0)
        assertEquals(1.0, target.surround, 0.0)
        assertTrue(target.filters === target.rightFilters)
    }

    @Test
    fun widthStepsAsideInBypassAndAtZero() {
        for (match in listOf(true, false)) {
            val target = Processing.target(AppState(useSystemSurround = false, current = widePreset, bypass = true, matchBypass = match), 0.0)
            assertTrue(target.rightFilters.isEmpty())
        }
        val off = Processing.target(AppState(useSystemSurround = false, current = widePreset.copy(tone = Tone(surround = 0.0))), 0.0)
        assertTrue(off.filters === off.rightFilters)
    }

    @Test
    fun widthAndEarCorrectionAddTogether() {
        val state = AppState(useSystemSurround = false, current = widePreset, earCorrections = mapOf(BUDS to ears))
        val target = Processing.target(state, 0.0, outputKey = BUDS)
        val (lean, _) = StereoWidth.curves(1.0)
        val point = lean.first { it.hz > 900 }
        assertEquals(point.db - 3.0, Eq.curveDb(target.filters, point.hz), 0.01)
        assertEquals(-point.db + 3.0, Eq.curveDb(target.rightFilters, point.hz), 0.01)
    }

    @Test
    fun widthLeavesRoomSoNeitherChannelClips() {
        val target = Processing.target(AppState(useSystemSurround = false, current = widePreset, autoHeadroom = true), 0.0)
        assertTrue(Eq.peakDb(target.filters) + target.inputDb <= 0.001)
        assertTrue(Eq.peakDb(target.rightFilters) + target.inputDb <= 0.001)
    }

    @Test
    fun widthBarelyChangesTheOverallTone() {
        val target = Processing.target(AppState(useSystemSurround = false, current = widePreset), 0.0)
        for (hz in listOf(600.0, 1000.0, 2000.0, 5000.0)) {
            val sum = Eq.curveDb(target.filters, hz) + Eq.curveDb(target.rightFilters, hz)
            assertEquals(0.0, sum, 0.001)
        }
    }

    @Test
    fun widthIsTheDefaultAndResetReturnsToIt() {
        assertFalse(AppState().useSystemSurround)
        assertFalse(AppState(useSystemSurround = true).resetSound().useSystemSurround)
    }

    private val punchyBand = CompressorBand(thresholdDb = -24.0, ratio = 3.0, makeupDb = 6.0)
    private val loudCompressor = Compressor(enabled = true, low = punchyBand, mid = punchyBand, high = punchyBand)

    private fun fullLoudness(preset: Preset): Double {
        val filters = Processing.presetFilters(preset)
        return Processing.estimatedLoudnessDb(filters, Processing.inputOf(preset, filters, true), preset.tone.compressor)
    }

    @Test
    fun makeupRaisesAndCompressionLowersTheEstimate() {
        val plain = Preset("user:p", "Plain")
        val base = fullLoudness(plain)
        val makeupOnly = CompressorBand(ratio = 1.0, makeupDb = 6.0)
        assertEquals(
            base + 6.0,
            fullLoudness(plain.copy(tone = Tone(compressor = Compressor(enabled = true, low = makeupOnly, mid = makeupOnly, high = makeupOnly)))),
            0.01,
        )
        val hard = CompressorBand(thresholdDb = -40.0, ratio = 8.0, makeupDb = 0.0)
        assertTrue(fullLoudness(plain.copy(tone = Tone(compressor = Compressor(enabled = true, low = hard, mid = hard, high = hard)))) < base)
    }

    @Test
    fun anOffCompressorDoesntAffectTheEstimate() {
        val plain = Preset("user:p", "Plain", tone = Tone(bassDb = 3.0))
        assertEquals(fullLoudness(plain), fullLoudness(plain.copy(tone = plain.tone.copy(compressor = loudCompressor.copy(enabled = false)))), 0.0)
    }

    @Test
    fun aCompressedPresetIsMatchedInAComparison() {
        val compressedSound = Preset("user:c", "C", tone = Tone(compressor = loudCompressor))
        for ((a, b) in listOf(quiet to compressedSound, compressedSound to quiet)) {
            val comparison = AbComparison(a, b)
            assertEquals(fullLoudness(comparison.soundFor(0, true)), fullLoudness(comparison.soundFor(1, true)), 0.05)
        }
        // What's kept is still the saved sound, with no temporary level.
        assertEquals(0.0, AbComparison(quiet, compressedSound).presetFor(1).tone.trimDb, 0.0)
    }

    @Test
    fun bypassMatchesACompressorThatLowersTheLevel() {
        val hard = CompressorBand(thresholdDb = -40.0, ratio = 8.0, makeupDb = 0.0)
        val preset = Preset("user:h", "H", tone = Tone(compressor = Compressor(enabled = true, low = hard, mid = hard, high = hard)))
        val bypassed = Processing.target(AppState(current = preset, bypass = true, matchBypass = true), 0.0)
        assertTrue(bypassed.inputDb < -3.0)
        assertFalse(bypassed.dynamics.enabled)
    }

    private fun fadeSteps(from: Compressor?, to: Compressor): List<Dynamics.Step> {
        val steps = mutableListOf<Dynamics.Step>()
        var sent = from
        do {
            val step = Dynamics.next(sent, to)
            steps += step
            sent = step.compressor
            assertTrue("A fade must finish", steps.size < 100)
        } while (!step.done)
        return steps
    }

    @Test
    fun theCompressorFadesInWithoutALevelJump() {
        val steps = fadeSteps(Compressor(), loudCompressor)
        assertTrue(steps.size > 2)
        assertEquals(loudCompressor, steps.last().compressor)
        var makeup = 0.0
        for (step in steps) {
            assertTrue(step.compressor.enabled)
            assertTrue(step.compressor.low.makeupDb - makeup <= 1.5001)
            makeup = step.compressor.low.makeupDb
        }
        // The first step barely compresses at all.
        assertTrue(steps.first().compressor.low.ratio < 1.6)
    }

    @Test
    fun theCompressorFadesOutThenSwitchesOff() {
        val steps = fadeSteps(loudCompressor, Compressor())
        assertEquals(Compressor(), steps.last().compressor)
        assertTrue(steps.dropLast(1).all { it.compressor.enabled })
        assertTrue(steps.dropLast(1).last().compressor.low.makeupDb <= 1.5001)
    }

    @Test
    fun aBandSwitchedOffFadesWhileTheOthersStay() {
        val target = loudCompressor.copy(mid = loudCompressor.mid.copy(enabled = false))
        val steps = fadeSteps(loudCompressor, target)
        assertTrue(steps.size > 1)
        assertEquals(punchyBand.makeupDb, steps.first().compressor.low.makeupDb, 0.0001)
        assertTrue(steps.first().compressor.mid.makeupDb < punchyBand.makeupDb)
        assertEquals(target, steps.last().compressor)
    }

    @Test
    fun smallCompressorChangesGoStraightThrough() {
        val target = loudCompressor.copy(low = punchyBand.copy(thresholdDb = -22.0))
        val step = Dynamics.next(loudCompressor, target)
        assertTrue(step.done)
        assertEquals(target, step.compressor)
    }

    @Test
    fun comparisonsAndAFreshStartSkipTheFade() {
        assertTrue(Dynamics.next(Compressor(), loudCompressor, instant = true).done)
        assertEquals(loudCompressor, Dynamics.next(null, loudCompressor).compressor)
        assertTrue(Dynamics.next(loudCompressor, loudCompressor).done)
    }

    @Test
    fun crossoversStayInOrderThroughAFade() {
        val target = loudCompressor.copy(lowCrossoverHz = 800.0, highCrossoverHz = 1000.0)
        for (start in listOf(loudCompressor.copy(lowCrossoverHz = 40.0, highCrossoverHz = 12000.0), loudCompressor)) {
            for (step in fadeSteps(start, target)) {
                assertTrue(step.compressor.lowCrossoverHz < step.compressor.highCrossoverHz)
            }
        }
    }

    @Test
    fun aNewCompressorTargetContinuesFromWhereTheSoundIs() {
        val first = Dynamics.next(Compressor(), loudCompressor).compressor
        val back = Dynamics.next(first, Compressor())
        assertTrue(back.compressor.low.makeupDb <= first.low.makeupDb)
    }

    @Test
    fun shareCodesFromTheOldNameStillWork() {
        val preset = Preset("user:x", "Mine", tone = Tone(bassDb = 3.0))
        val code = ShareCode.encode(preset)
        assertTrue(code.startsWith("eigentone1:"))
        val old = code.replace("eigentone1:", "phonons1:")
        assertEquals("Mine", ShareCode.decode(old)!!.name)
        assertEquals(3.0, ShareCode.decode("Try this: $old")!!.tone.bassDb, 0.0)
        assertNull(ShareCode.decode("phonons1:"))
        assertNull(ShareCode.decode("no code here"))
    }

    @Test
    fun anEditedPresetAndItsSavedVersionAreToldApart() {
        val saved = Preset("user:main", "Main", tone = Tone(bassDb = 2.0))
        val edited = saved.copy(tone = Tone(bassDb = 6.0))
        val comparison = AbComparison(edited, saved, edited = edited)
        assertEquals("Main (edited)", comparison.nameFor(0))
        assertEquals("Main (saved)", comparison.nameFor(1))
        // Keeping the saved one really does bring back the saved sound.
        assertEquals(saved, comparison.presetFor(1))
        assertTrue(comparison.soundFor(1, true).tone.bassDb == 2.0)
    }

    @Test
    fun differentPresetsKeepTheirPlainNames() {
        val comparison = AbComparison(quiet, boomy, edited = quiet)
        assertEquals("Quiet", comparison.nameFor(0))
        assertEquals("Boomy", comparison.nameFor(1))
    }

    @Test
    fun blindComparisonsStayMatchedInEitherOrder() {
        val comparison = AbComparison(quiet, boomy, blind = true, flipped = true)
        assertEquals(comparedLoudness(comparison.soundFor(0, true)), comparedLoudness(comparison.soundFor(1, true)), 0.05)
        assertEquals(quiet, comparison.soundFor(1, true))
    }

    private fun floats(vararg values: Float) = values

    /** Runs a move to the end, the way the engine does, and returns every step it took. */
    private fun rampSteps(fromBands: FloatArray, fromInput: Float, toBands: FloatArray, toInput: Float): List<GainRamp.Step> {
        val steps = mutableListOf<GainRamp.Step>()
        var bands = fromBands
        var input = fromInput
        do {
            val step = GainRamp.next(bands, bands, input, toBands, toBands, toInput)
            steps += step
            bands = step.left
            input = step.inputDb
            assertTrue("A move must finish", steps.size < 100)
        } while (!step.done)
        return steps
    }

    @Test
    fun smallChangesGoStraightThrough() {
        val step = GainRamp.next(floats(1f, 2f), floats(1f, 2f), -3f, floats(3f, 2f), floats(3f, 2f), -3.5f)
        assertTrue(step.done)
        assertEquals(3f, step.left[0], 0f)
    }

    @Test
    fun largeChangesMoveInStepsAndEndExactly() {
        val steps = rampSteps(floats(0f, 0f), 0f, floats(12f, -6f), -12f)
        assertEquals(3, steps.size)
        assertEquals(4f, steps[0].left[0], 0.001f)
        assertEquals(-2f, steps[0].left[1], 0.001f)
        assertEquals(-4f, steps[0].inputDb, 0.001f)
        assertEquals(12f, steps.last().left[0], 0f)
        assertEquals(-6f, steps.last().left[1], 0f)
        assertEquals(-12f, steps.last().inputDb, 0f)
    }

    @Test
    fun noStepChangesMoreThanTheLimit() {
        var bands = floats(-20f, 15f)
        var input = 0f
        for (step in rampSteps(bands, input, floats(20f, -15f), -20f)) {
            for (i in bands.indices) assertTrue(abs(step.left[i] - bands[i]) <= GainRamp.MAX_STEP_DB + 0.001f)
            assertTrue(abs(step.inputDb - input) <= GainRamp.MAX_STEP_DB + 0.001f)
            bands = step.left
            input = step.inputDb
        }
    }

    /** The reason for moving the level with the bands: nothing louder than either end along the way. */
    @Test
    fun aMoveBetweenTwoSafeSoundsStaysSafe() {
        val quiet = floats(0f, 0f, 12f)
        val boosted = floats(15f, -6f, 0f)
        for ((from, to) in listOf(quiet to boosted, boosted to quiet)) {
            val fromInput = -from.max()
            val toInput = -to.max()
            for (step in rampSteps(from, fromInput, to, toInput)) {
                assertTrue(step.left.max() + step.inputDb <= 0.001f)
            }
        }
    }

    @Test
    fun aNewTargetContinuesFromWhereTheSoundIs() {
        val first = GainRamp.next(floats(0f), floats(0f), 0f, floats(12f), floats(12f), 0f)
        assertEquals(4f, first.left[0], 0.001f)
        val turned = GainRamp.next(first.left, first.right, first.inputDb, floats(-4f), floats(-4f), 0f)
        assertEquals(0f, turned.left[0], 0.001f)
        assertFalse(turned.done)
    }

    @Test
    fun bothEarsFinishTogether() {
        val step = GainRamp.next(floats(0f), floats(0f), 0f, floats(8f), floats(-2f), 0f)
        assertEquals(4f, step.left[0], 0.001f)
        assertEquals(-1f, step.right[0], 0.001f)
    }

    @Test
    fun nothingToMoveFromGoesStraightThere() {
        val fresh = GainRamp.next(floats(Float.NaN), floats(Float.NaN), Float.NaN, floats(12f), floats(12f), -12f)
        assertTrue(fresh.done)
        assertEquals(12f, fresh.left[0], 0f)
    }

    @Test
    fun instantSkipsTheMove() {
        val step = GainRamp.next(floats(0f), floats(0f), 0f, floats(12f), floats(12f), -12f, instant = true)
        assertTrue(step.done)
        assertEquals(12f, step.left[0], 0f)
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
    fun earCorrectionIsOnlyRecommendedForARealDifference() {
        // One 5 dB step could just be a missed beep.
        assertFalse(earResult(-60.0, -55.0).earCorrectionIsRecommended())
        assertTrue(earResult(-60.0, -50.0).earCorrectionIsRecommended())
    }

    @Test
    fun aCloseResultCanStillBeCorrected() {
        val ears = earResult(-60.0, -55.0).earCorrection()
        assertTrue(ears.hasChanges)
        assertTrue(ears.right.single().db > 0.0)
        assertTrue(ears.left.single().db < 0.0)
    }

    @Test
    fun earLevelsShowHowEachEarComparedAtEveryFrequency() {
        val levels = HearingResult(
            mapOf(
                Ear.LEFT to mapOf(4000.0 to -60.0, 1000.0 to -50.0),
                Ear.RIGHT to mapOf(1000.0 to -50.0, 4000.0 to -40.0),
            ),
        ).earLevels()
        // The typical level is -50 dB, and points come out in frequency order.
        assertEquals(listOf(ShapePoint(1000.0, 0.0), ShapePoint(4000.0, -10.0)), levels.left)
        assertEquals(listOf(ShapePoint(1000.0, 0.0), ShapePoint(4000.0, 10.0)), levels.right)
    }

    @Test
    fun theNoteAboutCloseEarsSaysHowClose() {
        assertTrue(com.phonons.app.ui.earsCloseNote(earResult(-60.0, -55.0)).contains("within 5 dB"))
        assertTrue(com.phonons.app.ui.earsCloseNote(earResult(-60.0, -60.0)).contains("matched"))
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

    private val BUDS = "Bluetooth:Buds"

    private val ears = EarCorrection(
        left = listOf(ShapePoint(1000.0, -3.0)),
        right = listOf(ShapePoint(1000.0, 3.0)),
    )

    @Test
    fun earCorrectionGivesEachChannelItsOwnCurveAndOneLevel() {
        val state = AppState(earCorrections = mapOf(BUDS to ears), autoHeadroom = true)
        val target = Processing.target(state, 0.0, outputKey = BUDS)
        assertEquals(-3.0, Eq.curveDb(target.filters, 1000.0), 0.001)
        assertEquals(3.0, Eq.curveDb(target.rightFilters, 1000.0), 0.001)
        // Headroom comes from the ear with the larger boost, and applies to both.
        assertEquals(-3.0, target.inputDb, 0.01)
    }

    @Test
    fun earCorrectionOnlyAppliesToItsOwnHeadphones() {
        val state = AppState(earCorrections = mapOf(BUDS to ears))
        val other = Processing.target(state, 0.0, outputKey = "Bluetooth:Other")
        assertTrue(other.filters === other.rightFilters)
        assertEquals(0.0, other.inputDb, 0.0)
    }

    @Test
    fun aPhoneSpeakerNeverGetsEarCorrection() {
        val state = AppState(earCorrections = mapOf("speaker" to ears))
        assertNull(state.earsFor("speaker"))
        val speaker = Processing.target(state, 0.0, outputKey = "speaker")
        assertTrue(speaker.filters === speaker.rightFilters)
    }

    @Test
    fun earCorrectionStepsAsideWhenOffOrBypassed() {
        val plain = Processing.target(AppState(), 0.0)
        assertTrue(plain.filters === plain.rightFilters)

        val off = AppState(earCorrections = mapOf(BUDS to ears.copy(enabled = false)))
        val disabled = Processing.target(off, 0.0, outputKey = BUDS)
        assertTrue(disabled.filters === disabled.rightFilters)

        for (match in listOf(true, false)) {
            val state = AppState(earCorrections = mapOf(BUDS to ears), bypass = true, matchBypass = match)
            val bypassed = Processing.target(state, 0.0, outputKey = BUDS)
            assertTrue(bypassed.filters.isEmpty())
            assertTrue(bypassed.rightFilters.isEmpty())
        }
    }

    @Test
    fun earCorrectionSurvivesPresetChangesButNotAReset() {
        val state = AppState(earCorrections = mapOf(BUDS to ears))
        assertEquals(ears, state.copy(current = libertyBass).earsFor(BUDS))
        assertNull(state.resetSound().earsFor(BUDS))
    }

    @Test
    fun aCorrectionFromBeforeHeadphonesWereTrackedMovesToTheFirstHeadphones() {
        val old = AppState(earCorrection = ears)
        // The speaker can't use it, so it waits.
        assertEquals(old, old.withLegacyEarsMovedTo(OutputInfo("speaker", "Phone speaker")))

        val moved = old.withLegacyEarsMovedTo(OutputInfo(BUDS, "Buds"))
        assertEquals(ears, moved.earsFor(BUDS))
        assertNull(moved.earCorrection)
        assertEquals(moved, moved.withLegacyEarsMovedTo(OutputInfo("Bluetooth:Other", "Other")))
    }

    @Test
    fun aCorrectionFromAnOlderCheckWaitsForTheHeadphonesItNames() {
        val record = HearingRecord.of(unevenEars, "Buds", 1L)
        val old = AppState(earCorrection = ears, hearing = record)
        assertEquals(old, old.withLegacyEarsMovedTo(OutputInfo("Bluetooth:Other", "Other")))

        val moved = old.withLegacyEarsMovedTo(OutputInfo(BUDS, "Buds"))
        assertEquals(ears, moved.earsFor(BUDS))
        assertEquals(record, moved.hearings[BUDS])
        assertNull(moved.hearing)
    }

    @Test
    fun movingAnOldCorrectionNeverReplacesANewerOne() {
        val newer = ears.copy(left = listOf(ShapePoint(500.0, 1.0)))
        val state = AppState(earCorrection = ears, earCorrections = mapOf(BUDS to newer))
        assertEquals(newer, state.withLegacyEarsMovedTo(OutputInfo(BUDS, "Buds")).earsFor(BUDS))
    }

    @Test
    fun backupKeepsEarCorrectionAndChecksIt() {
        val state = AppState(earCorrections = mapOf(BUDS to ears))
        val restored = BackupCodec.merge(AppState(), BackupCodec.decode(BackupCodec.encode(state))!!)
        assertEquals(ears, restored.earsFor(BUDS))
        // A backup without one doesn't remove the one already in use.
        assertEquals(ears, BackupCodec.merge(state, BackupCodec.decode("{}")!!).earsFor(BUDS))

        val edited = """{"earCorrections":{"Bluetooth:Buds":{"left":[{"hz":1000.0,"db":90.0}],"right":[]}}}"""
        assertEquals(6.0, BackupCodec.decode(edited)!!.earCorrections.getValue(BUDS).left.single().db, 0.0)

        // A backup from before these were per headphone still works.
        val older = """{"earCorrection":{"left":[{"hz":1000.0,"db":90.0}],"right":[]}}"""
        val merged = BackupCodec.merge(AppState(), BackupCodec.decode(older)!!)
        assertEquals(6.0, merged.withLegacyEarsMovedTo(OutputInfo(BUDS, "Buds")).earsFor(BUDS)!!.left.single().db, 0.0)
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
