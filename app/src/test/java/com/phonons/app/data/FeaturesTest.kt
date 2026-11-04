package com.phonons.app.data

import com.phonons.app.dsp.Eq
import com.phonons.app.dsp.Filter
import com.phonons.app.dsp.FilterType
import com.phonons.app.dsp.Spectrum
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.math.abs

class FeaturesTest {
    private val boost = Preset("user:a", "A", 0.0, listOf(Filter(1, FilterType.BELL, 1000.0, 6.0, 1.0)))

    @Test
    fun toneDialsAddToThePresetWithoutChangingIt() {
        val state = AppState(current = boost.copy(tone = Tone(bassDb = 4.0, trebleDb = -2.0)))
        val filters = Processing.effectiveFilters(state, 0.0)
        assertEquals(3, filters.size)
        assertEquals(4.0, Eq.curveDb(filters, 20.0), 0.2)
        assertEquals(-2.0, Eq.curveDb(filters, 20000.0), 0.2)
        assertEquals(1, state.current.filters.size)
    }

    @Test
    fun zeroToneAddsNoFilters() {
        assertTrue(Processing.toneFilters(0.0, 0.0).isEmpty())
    }

    private fun at(filters: List<Filter>, hz: Double) = Eq.curveDb(filters, hz)

    @Test
    fun loudnessFollowsTheIsoContoursBelowYourUsualVolume() {
        // At or above the usual volume (10 dB below full by default), nothing is added.
        assertTrue(Processing.loudnessFilters(5.0, 1.0).isEmpty())
        assertTrue(Processing.loudnessFilters(10.0, 1.0).isEmpty())
        // 10 dB quieter is 70 phon instead of 80. ISO 226 gives about +3 dB at 125 Hz and nothing at 1 kHz.
        val tenQuieter = Processing.loudnessFilters(20.0, 1.0)
        assertEquals(3.0, at(tenQuieter, 125.0), 0.4)
        assertEquals(0.0, at(tenQuieter, 1000.0), 0.05)
        // 30 dB quieter needs much more bass.
        assertEquals(8.1, at(Processing.loudnessFilters(40.0, 1.0), 125.0), 0.4)
        // Very low volume stops at the safety limit.
        assertTrue(at(Processing.loudnessFilters(90.0, 1.0), 20.0) <= EqualLoudness.MAX_BOOST_DB + 1e-9)
    }

    @Test
    fun loudnessStrengthScalesAndZeroDisables() {
        val full = at(Processing.loudnessFilters(30.0, 1.0), 63.0)
        assertEquals(full / 2, at(Processing.loudnessFilters(30.0, 0.5), 63.0), 0.1)
        assertTrue(Processing.loudnessFilters(30.0, 0.0).isEmpty())
    }

    @Test
    fun eachOutputKeepsItsOwnUsualVolume() {
        val state = AppState(loudness = true, loudnessUsual = mapOf("Bluetooth:Buds" to 25.0))
        assertEquals(25.0, state.usualVolumeDb("Bluetooth:Buds"), 0.0)
        assertEquals(EqualLoudness.DEFAULT_USUAL_DB, state.usualVolumeDb("speaker"), 0.0)
        // At 20 dB below full, the buds are above their usual volume, and the speaker is 10 dB below its own.
        assertEquals(1, Processing.effectiveFilters(state.copy(current = boost), 20.0, "Bluetooth:Buds").size)
        assertEquals(2, Processing.effectiveFilters(state.copy(current = boost), 20.0, "speaker").size)
    }

    @Test
    fun loudnessIsIgnoredWhenSwitchedOff() {
        val off = AppState(current = boost, loudness = false, loudnessStrength = 1.0)
        assertEquals(1, Processing.effectiveFilters(off, 50.0).size)
        assertEquals(2, Processing.effectiveFilters(off.copy(loudness = true), 50.0).size)
    }

    @Test
    fun flatProcessingEstimatesNoLoudnessChange() {
        assertEquals(0.0, Processing.estimatedLoudnessDb(emptyList(), 0.0), 1e-6)
        assertEquals(-6.0, Processing.estimatedLoudnessDb(emptyList(), -6.0), 1e-6)
    }

    @Test
    fun loudnessEstimateWeighsMidrangeAboveDeepBass() {
        val mid = listOf(Filter(1, FilterType.BELL, 2000.0, 6.0, 1.0))
        val sub = listOf(Filter(1, FilterType.BELL, 40.0, 6.0, 1.0))
        assertTrue(Processing.estimatedLoudnessDb(mid, 0.0) > Processing.estimatedLoudnessDb(sub, 0.0))
    }

    @Test
    fun matchedBypassIsFlatQuieterAndStillRunning() {
        val state = AppState(current = boost, bypass = true, matchBypass = true, autoHeadroom = true)
        val target = Processing.target(state, 0.0)
        assertTrue(target.filters.isEmpty())
        assertTrue(target.effectEnabled)
        assertEquals(0.0, target.nightMode, 0.0)
        assertTrue("expected a reduction, got ${target.inputDb}", target.inputDb < 0.0)
        // The headroom cut is -6 dB and the boost only covers part of the spectrum, so quieter overall.
        assertTrue(target.inputDb > -6.0)
    }

    @Test
    fun matchedBypassNeverBoosts() {
        val loud = boost.copy(preampDb = 12.0)
        val state = AppState(current = loud, bypass = true, autoHeadroom = false)
        assertEquals(0.0, Processing.target(state, 0.0).inputDb, 0.0)
    }

    @Test
    fun plainBypassTurnsTheEffectOff() {
        val target = Processing.target(AppState(current = boost, bypass = true, matchBypass = false), 0.0)
        assertFalse(target.effectEnabled)
    }

    @Test
    fun normalProcessingKeepsTheSettings() {
        val target = Processing.target(AppState(current = boost, nightMode = 0.5, limiter = false), 0.0)
        assertTrue(target.effectEnabled)
        assertEquals(0.5, target.nightMode, 0.0)
        assertFalse(target.limiter)
        assertEquals(-6.0, target.inputDb, 0.05)
    }

    @Test
    fun aModesOwnPresetWinsThenTheOutputsMainOne() {
        val key = "Bluetooth:Buds"
        val state = AppState(
            listeningMode = ListeningMode.TRANSPARENCY,
            outputProfiles = mapOf(key to "main", Profiles.modeKey(key, ListeningMode.TRANSPARENCY) to "aware"),
        )
        assertEquals("aware", Profiles.presetIdFor(state, key))
        assertEquals("main", Profiles.presetIdFor(state.copy(listeningMode = ListeningMode.NORMAL), key))
        assertNull(Profiles.presetIdFor(state, "Bluetooth:Other"))
    }

    @Test
    fun speakerIgnoresEarbudModes() {
        val state = AppState(
            listeningMode = ListeningMode.TRANSPARENCY,
            outputProfiles = mapOf("speaker" to "main", Profiles.modeKey("speaker", ListeningMode.TRANSPARENCY) to "x"),
        )
        assertEquals("main", Profiles.presetIdFor(state, "speaker"))
    }

    @Test
    fun assignmentCheckCoversEveryModeButNotOtherOutputs() {
        val state = AppState(outputProfiles = mapOf(Profiles.modeKey("Bluetooth:Buds", ListeningMode.NORMAL) to "p"))
        assertTrue(Profiles.isAssigned(state, "Bluetooth:Buds", "p"))
        assertFalse(Profiles.isAssigned(state, "Bluetooth:Buds 2", "p"))
    }

    @Test
    fun backupRoundTrips() {
        val state = AppState(presets = listOf(boost), outputProfiles = mapOf("speaker" to "user:a"), favorites = setOf("user:a"))
        val restored = BackupCodec.merge(AppState(), BackupCodec.decode(BackupCodec.encode(state))!!)
        assertEquals(listOf(boost), restored.presets)
        assertEquals("user:a", restored.outputProfiles["speaker"])
        assertEquals(setOf("user:a"), restored.favorites)
    }

    @Test
    fun backupRejectsInvalidFiles() {
        assertNull(BackupCodec.decode("not json"))
        assertNull(BackupCodec.decode("[1,2,3]"))
    }

    @Test
    fun restoredPresetsAreSanitised() {
        val text = """
            {"presets":[{"id":"user:x","name":"X","preampDb":999,"filters":[
              {"id":1,"freq":0,"gainDb":500,"q":0}
            ]}]}
        """.trimIndent()
        val filter = BackupCodec.decode(text)!!.presets.single().filters.single()
        assertEquals(Eq.MIN_FREQ, filter.freq, 0.0)
        assertEquals(30.0, filter.gainDb, 0.0)
        assertEquals(Eq.MIN_Q, filter.q, 0.0)
    }

    @Test
    fun backupsCannotReplaceBuiltInsOrPointAtUnknownPresets() {
        val text = """
            {"presets":[{"id":"builtin:flat","name":"Evil","filters":[]}],
             "outputProfiles":{"speaker":"user:missing","Bluetooth:Buds":"builtin:flat"}}
        """.trimIndent()
        val backup = BackupCodec.decode(text)
        assertNotNull(backup)
        assertTrue(backup!!.presets.isEmpty())
        val merged = BackupCodec.merge(AppState(), backup)
        assertNull(merged.outputProfiles["speaker"])
        assertEquals("builtin:flat", merged.outputProfiles["Bluetooth:Buds"])
    }

    @Test
    fun oldSavedStateStillLoads() {
        val json = kotlinx.serialization.json.Json { ignoreUnknownKeys = true }
        val state = json.decodeFromString(AppState.serializer(), """{"enabled":true,"limiter":false}""")
        assertTrue(state.enabled)
        assertFalse(state.limiter)
        assertFalse(state.loudness)
        assertTrue(state.startOnBoot)
    }

    @Test
    fun graphicEqHitsItsFaderAndFlatAddsNothing() {
        val gains = List(10) { if (it == 5) 6.0 else 0.0 }
        val tone = Tone(graphicDb = gains, graphicBands = 10)
        val filters = Processing.presetFilters(Preset("user:g", "G", tone = tone))
        assertEquals(6.0, Eq.curveDb(filters, 1000.0), 0.3)
        val flat = boost.copy(tone = Tone(graphicDb = List(10) { 0.0 }))
        assertEquals(1, Processing.effectiveFilters(AppState(current = flat), 0.0).size)
    }

    @Test
    fun bassTunerShapesOnlyTheLowEnd() {
        val filters = Processing.bassTunerFilters(6.0, 80.0)
        assertEquals(6.0, Eq.curveDb(filters, 20.0), 0.3)
        assertEquals(0.0, Eq.curveDb(filters, 5000.0), 0.05)
        assertTrue(Processing.bassTunerFilters(0.0, 80.0).isEmpty())
    }

    @Test
    fun surroundFollowsItsSettingAndStopsInEitherBypass() {
        val state = AppState(useSystemSurround = true, current = boost.copy(tone = Tone(surround = 0.6)), limiterThresholdDb = -3.0)
        val on = Processing.target(state, 0.0)
        assertEquals(0.6, on.surround, 0.0)
        assertEquals(-3.0, on.limiterThresholdDb, 0.0)
        for (match in listOf(true, false)) {
            assertEquals(0.0, Processing.target(state.copy(bypass = true, matchBypass = match), 0.0).surround, 0.0)
        }
    }

    @Test
    fun stateSavedWithTheRemovedReverbSettingStillLoads() {
        val json = kotlinx.serialization.json.Json { ignoreUnknownKeys = true }
        val state = json.decodeFromString(AppState.serializer(), "{\"surround\":0.5,\"reverb\":\"LARGE_HALL\"}")
        assertEquals(0.5, state.surround, 0.0)
    }

    @Test
    fun catalogParsesRealIndexLinesAndRejectsPathEscapes() {
        val entries = AutoEqCatalog.parseIndex(
            """
            # Index
            - [Anker Soundcore Liberty 4 NC](./DHRME/in-ear/Anker%20Soundcore%20Liberty%204%20NC) by DHRME
            - [Anker Soundcore Liberty 4 NC](./Rtings/Bruel%20&%20Kjaer%205128%20in-ear/Anker%20Soundcore%20Liberty%204%20NC) by Rtings on Bruel & Kjaer 5128
            - [Sony (Rev 2)](./oratory1990/over-ear/Sony%20(Rev%202)) by oratory1990
            - [Bad](./../../secrets) by Nobody
            not a list line
            """.trimIndent(),
        )
        assertEquals(3, entries.size)
        assertEquals("Rtings on Bruel & Kjaer 5128", entries[1].source)
        assertEquals("oratory1990/over-ear/Sony%20(Rev%202)", entries[2].path)
        assertEquals(
            "https://raw.githubusercontent.com/jaakkopasanen/AutoEq/master/results/" +
                "DHRME/in-ear/Anker%20Soundcore%20Liberty%204%20NC/Anker%20Soundcore%20Liberty%204%20NC%20ParametricEQ.txt",
            AutoEqCatalog.profileUrl(entries[0]),
        )
    }

    @Test
    fun catalogSearchMatchesAllWordsInAnyOrder() {
        val entries = listOf(
            CatalogEntry("Anker Soundcore Liberty 4 NC", "DHRME", "a"),
            CatalogEntry("Anker Soundcore Liberty 4", "Rtings", "b"),
            CatalogEntry("Sony WH-1000XM5", "oratory1990", "c"),
        )
        assertEquals(listOf("a"), AutoEqCatalog.search(entries, "nc liberty").map { it.path })
        assertEquals(2, AutoEqCatalog.search(entries, "LIBERTY 4").size)
        assertTrue(AutoEqCatalog.search(entries, "   ").isEmpty())
        assertEquals(1, AutoEqCatalog.search(entries, "anker", limit = 1).size)
    }

    @Test
    fun spectrumFindsAToneInTheRightBand() {
        val size = 1024
        val fft = ByteArray(size)
        val binHz = 48000.0 / size
        val bin = (1000.0 / binHz).toInt()
        fft[bin * 2] = 127
        val bands = Spectrum.bands(fft, 48000.0, 64)
        val loudest = bands.indices.maxByOrNull { bands[it] }!!
        val centre = Eq.logFreq((loudest + 0.5) / 64)
        assertTrue("tone landed at $centre Hz", abs(centre - 1000.0) < 120.0)
        assertTrue(bands[loudest] > 0.9f)
    }

    @Test
    fun spectrumOfSilenceIsZero() {
        assertTrue(Spectrum.bands(ByteArray(1024), 48000.0, 64).all { it == 0f })
    }

    @Test
    fun spectrumSmoothingRisesAtOnceAndFallsGradually() {
        val result = Spectrum.smooth(floatArrayOf(1f, 0f), floatArrayOf(0f, 1f))
        assertEquals(0.82f, result[0], 1e-4f)
        assertEquals(1f, result[1], 0f)
    }
}
