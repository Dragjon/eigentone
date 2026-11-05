package io.github.dragjon.eigentone.data

import io.github.dragjon.eigentone.dsp.Eq
import io.github.dragjon.eigentone.dsp.GraphicEq

/**
 * The faders show the whole sound at each band, the preset's own curve plus the change saved in the
 * faders, so a fader at +3 means +3 dB overall. Only the change is stored, so the preset still sounds
 * exactly as designed and a preset with no filters behaves as before.
 *
 * A preset can cut further than a fader reaches, such as a phone speaker's high-pass. Those faders rest
 * at the bottom of their travel, and the readout gives the real figure.
 */
object FaderLayer {
    /** The preset's own level at each band's centre, in dB, without the faders. Can be beyond their travel. */
    fun base(preset: Preset, bands: Int): List<Double> {
        val fixed = Processing.fixedFilters(preset)
        if (fixed.isEmpty()) return List(bands) { 0.0 }
        return GraphicEq.frequencies(bands).map { Eq.curveDb(fixed, it) }
    }

    /** Where each fader sits: the preset plus the change, kept within the faders' travel. */
    fun shown(base: List<Double>, change: List<Double>): List<Double> =
        base.indices.map { i -> (rest(base[i]) + change.getOrElse(i) { 0.0 }).coerceIn(-GraphicEq.MAX_DB, GraphicEq.MAX_DB) }

    /** The real level at a band, preset and change together, for the readout. */
    fun actual(base: List<Double>, change: List<Double>, band: Int): Double =
        base.getOrElse(band) { 0.0 } + change.getOrElse(band) { 0.0 }

    /**
     * Turns faders that were moved to [moved] back into the change to store. Faders that didn't move keep
     * their change exactly, so one held at the end of its travel doesn't lose what's under it. A fader put
     * back on the preset's own level stores no change at all.
     */
    fun change(base: List<Double>, before: List<Double>, current: List<Double>, moved: List<Double>): List<Double> =
        moved.indices.map { i ->
            val kept = current.getOrElse(i) { 0.0 }
            if (moved[i] == before.getOrElse(i) { Double.NaN }) return@map kept
            val rest = rest(base.getOrElse(i) { 0.0 })
            if (moved[i] == GraphicEq.snap(rest)) 0.0
            else (moved[i] - rest).coerceIn(-GraphicEq.MAX_DB, GraphicEq.MAX_DB)
        }

    /** Whether the preset itself shapes the sound, so the faders don't start flat. */
    fun isShaped(base: List<Double>): Boolean = base.any { kotlin.math.abs(it) >= 0.05 }

    /** Where a fader with no change rests: the preset's level, within the faders' travel. */
    private fun rest(db: Double) = db.coerceIn(-GraphicEq.MAX_DB, GraphicEq.MAX_DB)
}
