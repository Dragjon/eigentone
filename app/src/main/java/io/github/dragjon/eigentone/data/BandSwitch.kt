package io.github.dragjon.eigentone.data

import io.github.dragjon.eigentone.dsp.GraphicEq
import kotlinx.serialization.Serializable
import kotlin.math.abs

/** The faders at a finer layout than the one showing, kept so switching back to it loses nothing. */
@Serializable
data class FaderDetail(val bands: Int, val gains: List<Double>)

/**
 * Changing how many faders there are. Going to fewer keeps the finer faders aside. Coming back restores
 * them wherever the coarse faders weren't touched, and only the parts that were moved take the coarse shape.
 */
object BandSwitch {
    /** A fader closer than this to where the switch left it counts as untouched. */
    private const val TOUCHED_DB = 0.05

    fun switch(tone: Tone, to: Int): Tone {
        val from = tone.graphicBands
        if (from == to) return tone
        val gains = GraphicEq.normalized(tone.graphicDb, from)
        val finest = finest(tone.graphicDetail, gains, from)
        val moved = if (finest.bands == to) finest.gains else GraphicEq.resample(finest.gains, finest.bands, to)
        // Changing the layout isn't an edit, so an unedited shape stays that way.
        val kept = tone.shape?.let { shape -> if (shape.faders == gains) shape.copy(faders = moved) else shape }
        return tone.copy(
            graphicDb = moved,
            graphicBands = to,
            shape = kept,
            graphicDetail = finest.takeIf { it.bands > to && !GraphicEq.isFlat(it.gains) },
        )
    }

    /**
     * The most detailed version of the faders there is: the kept finer faders, with any coarse fader that
     * was moved since replacing the detail around it, blending into the detail kept on either side.
     */
    fun finest(detail: FaderDetail?, gains: List<Double>, bands: Int): FaderDetail {
        if (detail == null || detail.bands <= bands || detail.gains.size != detail.bands) return FaderDetail(bands, gains)
        // Flat everywhere means a reset, which shouldn't bring any of the old detail back.
        if (GraphicEq.isFlat(gains)) return FaderDetail(detail.bands, List(detail.bands) { 0.0 })
        val left = GraphicEq.resample(detail.gains, detail.bands, bands)
        val touched = gains.indices.map { if (abs(gains[it] - left[it]) > TOUCHED_DB) 1.0 else 0.0 }
        if (touched.all { it == 0.0 }) return detail
        val weight = GraphicEq.interpolated(touched, bands, detail.bands)
        val coarse = GraphicEq.resample(gains, bands, detail.bands)
        return FaderDetail(
            detail.bands,
            detail.gains.indices.map { i ->
                val w = weight[i].coerceIn(0.0, 1.0)
                GraphicEq.snap(detail.gains[i] * (1 - w) + coarse[i] * w)
            },
        )
    }
}
