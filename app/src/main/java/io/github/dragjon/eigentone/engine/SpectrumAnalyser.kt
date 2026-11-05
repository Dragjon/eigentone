package io.github.dragjon.eigentone.engine

import android.media.audiofx.Visualizer
import android.util.Log
import io.github.dragjon.eigentone.dsp.Spectrum

/**
 * Reads the output mix through Android's Visualizer. It only runs while the app is on screen, so it
 * costs nothing in the background. Android requires the record permission for this, even though
 * only frequency levels are read and nothing is stored.
 */
class SpectrumAnalyser(private val onBands: (FloatArray) -> Unit) {
    private var visualizer: Visualizer? = null
    private var last = FloatArray(BAND_COUNT)

    /** Returns false if this phone refused to provide the analyser. */
    fun start(): Boolean = try {
        val v = Visualizer(0)
        v.enabled = false
        v.captureSize = Visualizer.getCaptureSizeRange()[1].coerceAtMost(MAX_CAPTURE)
        v.scalingMode = Visualizer.SCALING_MODE_NORMALIZED
        v.setDataCaptureListener(
            object : Visualizer.OnDataCaptureListener {
                override fun onWaveFormDataCapture(vis: Visualizer, data: ByteArray, rate: Int) = Unit

                override fun onFftDataCapture(vis: Visualizer, fft: ByteArray, rateMilliHz: Int) {
                    last = Spectrum.smooth(last, Spectrum.bands(fft, rateMilliHz / 1000.0, BAND_COUNT))
                    onBands(last)
                }
            },
            Visualizer.getMaxCaptureRate().coerceAtMost(MAX_RATE_MILLIHZ),
            false,
            true,
        )
        v.enabled = true
        visualizer = v
        true
    } catch (e: Exception) {
        Log.w(TAG, "Analyser unavailable", e)
        stop()
        false
    }

    fun stop() {
        visualizer?.let { v ->
            runCatching {
                v.enabled = false
                v.release()
            }
        }
        visualizer = null
    }

    private companion object {
        const val TAG = "EigentoneAnalyser"
        const val BAND_COUNT = 64
        const val MAX_CAPTURE = 1024
        const val MAX_RATE_MILLIHZ = 20_000
    }
}
