package com.phonons.app.dsp

import kotlin.math.pow
import kotlin.math.sqrt

/**
 * DynamicsProcessing EQ bands only have a cutoff and a gain, with no Q. To offer real parametric
 * filters, the combined filter curve is sampled onto many narrow log-spaced bands.
 */
class CurveMapper(val bandCount: Int) {
    /** Upper edge of each band. The last one reaches Nyquist so nothing is left unprocessed. */
    val cutoffs: FloatArray = FloatArray(bandCount) { i ->
        (LOW_EDGE * (HIGH_EDGE / LOW_EDGE).pow((i + 1.0) / bandCount)).toFloat()
    }

    private val centres: DoubleArray = DoubleArray(bandCount) { i ->
        val low = if (i == 0) LOW_EDGE else cutoffs[i - 1].toDouble()
        sqrt(low * cutoffs[i])
    }

    fun gains(filters: List<Filter>): FloatArray = FloatArray(bandCount) { i ->
        Eq.curveDb(filters, centres[i]).coerceIn(MIN_BAND_DB, MAX_BAND_DB).toFloat()
    }

    companion object {
        const val LOW_EDGE = 20.0
        const val HIGH_EDGE = 24_000.0
        const val MIN_BAND_DB = -60.0
        const val MAX_BAND_DB = 36.0
    }
}
