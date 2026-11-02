package com.phonons.app.dsp

import kotlin.math.floor
import kotlin.math.log10
import kotlin.math.max

/** Turns a Visualizer FFT into log-spaced bands from 0 (silent) to 1 (full scale). */
object Spectrum {
    private const val FLOOR_DB = -72.0

    /**
     * [fft] is in the Visualizer format: real DC, real Nyquist, then real and imaginary byte pairs.
     * Bands narrower than one FFT bin read the nearest bin, so the lowest bands look coarse.
     */
    fun bands(fft: ByteArray, sampleRate: Double, bandCount: Int): FloatArray {
        val bins = fft.size / 2
        val out = FloatArray(bandCount)
        if (bins < 2 || sampleRate <= 0) return out
        val binHz = sampleRate / fft.size
        for (band in 0 until bandCount) {
            val lowHz = Eq.logFreq(band.toDouble() / bandCount)
            val highHz = Eq.logFreq((band + 1.0) / bandCount)
            val first = floor(lowHz / binHz).toInt().coerceIn(1, bins - 1)
            val last = floor(highHz / binHz).toInt().coerceIn(first, bins - 1)
            var peak = 0.0
            for (bin in first..last) {
                val re = fft[bin * 2].toDouble()
                val im = fft[bin * 2 + 1].toDouble()
                peak = max(peak, re * re + im * im)
            }
            val db = if (peak > 0) 10 * log10(peak / (128.0 * 128.0)) else FLOOR_DB
            out[band] = ((db - FLOOR_DB) / -FLOOR_DB).coerceIn(0.0, 1.0).toFloat()
        }
        return out
    }

    /** Rises instantly and falls gradually, so the display reads smoothly. */
    fun smooth(previous: FloatArray, next: FloatArray, fall: Float = 0.82f): FloatArray {
        if (previous.size != next.size) return next
        return FloatArray(next.size) { i -> max(next[i], previous[i] * fall) }
    }
}
