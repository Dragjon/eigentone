package com.phonons.app.engine

import android.content.Context
import android.media.AudioAttributes
import android.media.AudioFocusRequest
import android.media.AudioFormat
import android.media.AudioManager
import android.media.AudioTrack
import android.util.Log
import com.phonons.app.data.Sweep
import com.phonons.app.data.SweepOscillator

/** Plays the sweep tool's tone on its own thread, and asks other audio to pause meanwhile. */
class SweepPlayer(context: Context) {
    private val audio = context.getSystemService(AudioManager::class.java)
    private val attributes = AudioAttributes.Builder()
        .setUsage(AudioAttributes.USAGE_MEDIA)
        .setContentType(AudioAttributes.CONTENT_TYPE_UNKNOWN)
        .build()
    private val focus = AudioFocusRequest.Builder(AudioManager.AUDIOFOCUS_GAIN_TRANSIENT_EXCLUSIVE)
        .setAudioAttributes(attributes)
        .build()

    @Volatile var hz = Sweep.hzAt(0.0)
    @Volatile var levelDb = Sweep.DEFAULT_LEVEL_DB
    @Volatile private var sounding = false
    private var thread: Thread? = null

    /** Returns false if the phone wouldn't play it, so the screen can say so. */
    fun start(): Boolean {
        if (thread != null) return true
        val track = try {
            val format = AudioFormat.Builder()
                .setEncoding(AudioFormat.ENCODING_PCM_FLOAT)
                .setSampleRate(SAMPLE_RATE)
                .setChannelMask(AudioFormat.CHANNEL_OUT_STEREO)
                .build()
            AudioTrack.Builder()
                .setAudioAttributes(attributes)
                .setAudioFormat(format)
                .setTransferMode(AudioTrack.MODE_STREAM)
                .setBufferSizeInBytes(BUFFER_FRAMES * 2 * Float.SIZE_BYTES * 2)
                .build()
        } catch (e: Exception) {
            Log.w(TAG, "Could not create the tone", e)
            return false
        }
        audio.requestAudioFocus(focus)
        sounding = true
        thread = Thread({ run(track) }, "SweepTone").also { it.start() }
        return true
    }

    /** Fades out, then lets go of the audio. Takes a fraction of a second. */
    fun stop() {
        sounding = false
        thread?.let { runCatching { it.join(STOP_WAIT_MS) } }
        thread = null
        audio.abandonAudioFocusRequest(focus)
    }

    private fun run(track: AudioTrack) {
        val oscillator = SweepOscillator(SAMPLE_RATE)
        val buffer = FloatArray(BUFFER_FRAMES * 2)
        try {
            track.play()
            while (sounding || !oscillator.isSilent) {
                oscillator.fill(buffer, hz, levelDb, sounding)
                if (track.write(buffer, 0, buffer.size, AudioTrack.WRITE_BLOCKING) < 0) break
            }
            track.stop()
        } catch (e: Exception) {
            Log.w(TAG, "The tone stopped", e)
        } finally {
            runCatching { track.release() }
        }
    }

    private companion object {
        const val TAG = "PhononsSweep"
        const val SAMPLE_RATE = 48000
        const val BUFFER_FRAMES = 1024
        const val STOP_WAIT_MS = 500L
    }
}
