package io.github.dragjon.eigentone.data

import io.github.dragjon.eigentone.dsp.GraphicEq

/**
 * Simple by default, powerful when needed. Each rule says whether an advanced control shows. With Advanced
 * controls off, a control still shows while its setting differs from the default, so nothing in use is ever
 * out of sight or out of reach.
 */
object Disclosure {
    /** Only filters that change the sound count. Flat keeps its filters at 0 dB, ready to move. */
    fun parametric(s: AppState) = s.advancedControls || s.current.filters.any { it.gainDb != 0.0 || !it.type.hasGain }

    fun faderOptions(s: AppState) = s.advancedControls || s.faderMode != FaderMode.PRECISION || s.faderView != FaderView.SCROLL

    fun bandCount(s: AppState) = s.advancedControls || s.current.tone.graphicBands != GraphicEq.DEFAULT_BANDS

    /** Smoothing is an action rather than a setting, so there's nothing to keep in view. */
    fun smoothing(s: AppState) = s.advancedControls

    fun presetLevel(s: AppState) = s.advancedControls || s.current.tone.strength != 1.0 || s.current.preampDb != 0.0

    fun bassDetail(s: AppState) = s.advancedControls ||
        s.current.tone.bassHz != Tone.DEFAULT_BASS_HZ || s.current.tone.bassCharacter != BassCharacter.NATURAL

    /** The styles cover most needs. Hand-set bands keep their controls in view. */
    fun compressorBands(s: AppState): Boolean {
        val compressor = s.current.tone.compressor
        return s.advancedControls || (compressor.enabled && Dynamics.styles.none { it.compressor == compressor })
    }

    /** The threshold is always shown, so only the timing and gain count here. */
    fun limiterDetail(s: AppState): Boolean {
        val now = s.limiterSettings()
        val usual = LimiterSettings()
        return s.advancedControls || now.attackMs != usual.attackMs || now.releaseMs != usual.releaseMs ||
            now.ratio != usual.ratio || now.postGainDb != usual.postGainDb
    }

    fun matchBypass(s: AppState) = s.advancedControls || !s.matchBypass

    fun engine(s: AppState) = s.advancedControls || s.engineMode != EngineMode.AUTO || s.lowLatency

    fun status(s: AppState) = s.advancedControls

    fun perApp(s: AppState) = s.advancedControls || s.appProfiles.isNotEmpty()

    fun measureTools(s: AppState) = s.advancedControls
}
