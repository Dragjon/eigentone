package io.github.dragjon.eigentone.engine

import android.media.AudioAttributes
import android.media.AudioFocusRequest
import android.media.AudioFormat
import android.media.AudioManager
import android.media.AudioTrack
import android.util.Log
import io.github.dragjon.eigentone.data.Ear
import io.github.dragjon.eigentone.data.HearingTest
import kotlin.math.PI
import kotlin.math.min
import kotlin.math.pow
import kotlin.math.sin

/** Plays short tones in one ear for the hearing check, and asks other audio to pause meanwhile. */
class TonePlayer(context: android.content.Context) {
    private val audio = context.getSystemService(AudioManager::class.java)
    private val attributes = AudioAttributes.Builder()
        .setUsage(AudioAttributes.USAGE_MEDIA)
        .setContentType(AudioAttributes.CONTENT_TYPE_UNKNOWN)
        .build()
    private val focus = AudioFocusRequest.Builder(AudioManager.AUDIOFOCUS_GAIN_TRANSIENT_EXCLUSIVE)
        .setAudioAttributes(attributes)
        .build()
    private var track: AudioTrack? = null

    fun begin() {
        audio.requestAudioFocus(focus)
    }

    fun end() {
        stop()
        audio.abandonAudioFocusRequest(focus)
    }

    fun stop() {
        track?.let {
            runCatching {
                it.stop()
                it.release()
            }
        }
        track = null
    }

    /**
     * Three short pulses, which are easier to notice at low level than one steady tone. Returns false
     * if the phone wouldn't play it, so the listener isn't asked about a tone that never happened.
     */
    fun play(hz: Double, levelDb: Double, ear: Ear): Boolean = try {
        start(hz, levelDb, ear)
        true
    } catch (e: Exception) {
        Log.w(TAG, "Could not play a tone", e)
        false
    }

    private fun start(hz: Double, levelDb: Double, ear: Ear) {
        stop()
        val samples = pulses(hz, levelDb.coerceAtMost(HearingTest.CEILING_DB), ear)
        val format = AudioFormat.Builder()
            .setEncoding(AudioFormat.ENCODING_PCM_FLOAT)
            .setSampleRate(SAMPLE_RATE)
            .setChannelMask(AudioFormat.CHANNEL_OUT_STEREO)
            .build()
        val next = AudioTrack.Builder()
            .setAudioAttributes(attributes)
            .setAudioFormat(format)
            .setTransferMode(AudioTrack.MODE_STATIC)
            .setBufferSizeInBytes(samples.size * Float.SIZE_BYTES)
            .build()
        next.write(samples, 0, samples.size, AudioTrack.WRITE_BLOCKING)
        next.play()
        track = next
    }

    private fun pulses(hz: Double, levelDb: Double, ear: Ear): FloatArray {
        val amplitude = 10.0.pow(levelDb / 20)
        val pulse = SAMPLE_RATE * PULSE_MS / 1000
        val gap = SAMPLE_RATE * GAP_MS / 1000
        val fade = SAMPLE_RATE * FADE_MS / 1000
        val frames = PULSES * (pulse + gap)
        val out = FloatArray(frames * 2)
        val channel = if (ear == Ear.LEFT) 0 else 1
        for (i in 0 until frames) {
            val position = i % (pulse + gap)
            if (position >= pulse) continue
            // Fading in and out avoids a click, which would be audible even when the tone isn't.
            val envelope = min(1.0, min(position, pulse - position).toDouble() / fade)
            out[i * 2 + channel] = (amplitude * envelope * sin(2 * PI * hz * i / SAMPLE_RATE)).toFloat()
        }
        return out
    }

    companion object {
        private const val TAG = "EigentoneTone"
        private const val SAMPLE_RATE = 48000
        private const val PULSES = 3
        private const val PULSE_MS = 300
        private const val GAP_MS = 150
        private const val FADE_MS = 20

        /** How long one tone takes to play. */
        const val TONE_MS = PULSES * (PULSE_MS + GAP_MS).toLong()
    }
}
