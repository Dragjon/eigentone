package io.github.dragjon.eigentone.data

import io.github.dragjon.eigentone.dsp.Filter
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class DisclosureTest {
    private val simple = AppState(advancedControls = false)

    private fun rules(s: AppState) = listOf(
        Disclosure.parametric(s), Disclosure.faderOptions(s), Disclosure.bandCount(s), Disclosure.smoothing(s),
        Disclosure.presetLevel(s), Disclosure.bassDetail(s), Disclosure.compressorBands(s), Disclosure.limiterDetail(s),
        Disclosure.matchBypass(s), Disclosure.engine(s), Disclosure.status(s), Disclosure.perApp(s), Disclosure.measureTools(s),
    )

    @Test
    fun aNewInstallStartsSimpleAndAdvancedShowsEverything() {
        assertTrue(rules(simple).none { it })
        assertTrue(rules(simple.copy(advancedControls = true)).all { it })
    }

    @Test
    fun anythingChangedStaysInViewWhenSimple() {
        val tone = simple.current.tone
        assertTrue(Disclosure.parametric(simple.copy(current = simple.current.copy(filters = listOf(Filter(1, gainDb = 3.0))))))
        assertTrue(Disclosure.faderOptions(simple.copy(faderMode = FaderMode.SMOOTH)))
        assertTrue(Disclosure.faderOptions(simple.copy(faderView = FaderView.FULL_SCREEN)))
        assertTrue(Disclosure.bandCount(simple.copy(current = simple.current.copy(tone = tone.copy(graphicBands = 31)))))
        assertTrue(Disclosure.presetLevel(simple.copy(current = simple.current.copy(preampDb = -3.0))))
        assertTrue(Disclosure.presetLevel(simple.copy(current = simple.current.copy(tone = tone.copy(strength = 0.5)))))
        assertTrue(Disclosure.bassDetail(simple.copy(current = simple.current.copy(tone = tone.copy(bassCharacter = BassCharacter.PUNCH)))))
        assertTrue(Disclosure.bassDetail(simple.copy(current = simple.current.copy(tone = tone.copy(bassHz = 60.0)))))
        val handSet = Dynamics.styles.first().compressor.let { it.withBand(0, it.bands[0].copy(ratio = 7.7)) }
        assertTrue(Disclosure.compressorBands(simple.copy(current = simple.current.copy(tone = tone.copy(compressor = handSet)))))
        assertFalse(Disclosure.compressorBands(simple.copy(current = simple.current.copy(tone = tone.copy(compressor = Dynamics.styles.first().compressor)))))
        assertTrue(Disclosure.limiterDetail(simple.copy(limiterReleaseMs = 200.0)))
        assertTrue(Disclosure.matchBypass(simple.copy(matchBypass = false)))
        assertTrue(Disclosure.engine(simple.copy(engineMode = EngineMode.SESSIONS)))
        assertTrue(Disclosure.engine(simple.copy(lowLatency = true)))
        assertTrue(Disclosure.perApp(simple.copy(appProfiles = mapOf("com.spotify.music" to "builtin:flat"))))
    }

    @Test
    fun flatsUnmovedFiltersDontCountAsParametric() {
        assertFalse(Disclosure.parametric(simple.copy(current = BuiltIns.flat)))
        assertFalse(Disclosure.limiterDetail(simple.copy(limiterThresholdDb = -4.0)))
    }

    @Test
    fun anUpdateKeepsEveryControlButANewInstallDoesNot() {
        assertTrue(AppState(stateVersion = 2).migrated().advancedControls)
        // Someone updating already knows the app, so only new installs see the welcome.
        assertTrue(AppState(stateVersion = 2).migrated().welcomeDone)
        assertFalse(AppState(stateVersion = AppState.CURRENT_STATE_VERSION).migrated().welcomeDone)
        val fresh = AppState(stateVersion = AppState.CURRENT_STATE_VERSION)
        assertFalse(fresh.migrated().advancedControls)
        // Choosing simple after updating sticks.
        val chosen = AppState(stateVersion = 2).migrated().copy(advancedControls = false)
        assertEquals(false, chosen.migrated().advancedControls)
    }
}
