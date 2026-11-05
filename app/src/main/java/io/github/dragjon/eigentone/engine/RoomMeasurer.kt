package io.github.dragjon.eigentone.engine

import android.annotation.SuppressLint
import android.content.Context
import android.media.AudioAttributes
import android.media.AudioDeviceInfo
import android.media.AudioFocusRequest
import android.media.AudioFormat
import android.media.AudioManager
import android.media.AudioRecord
import android.media.AudioTrack
import android.media.MediaRecorder
import android.util.Log
import io.github.dragjon.eigentone.data.RoomMeasurement
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/**
 * Plays the measurement tones through whatever is connected and records them with the microphone at the
 * same time. A USB microphone is used when one is plugged in, since a measurement mic is far more even
 * than a phone's own. The raw microphone source is used where the phone has one, because the usual one
 * shapes sound for speech.
 */
class RoomMeasurer(context: Context) {
    private val audio = context.getSystemService(AudioManager::class.java)
    private val attributes = AudioAttributes.Builder()
        .setUsage(AudioAttributes.USAGE_MEDIA)
        .setContentType(AudioAttributes.CONTENT_TYPE_UNKNOWN)
        .build()
    private val focus = AudioFocusRequest.Builder(AudioManager.AUDIOFOCUS_GAIN_TRANSIENT_EXCLUSIVE)
        .setAudioAttributes(attributes)
        .build()

    /** The microphone that will be used: a USB one if plugged in, otherwise the phone's own. */
    fun microphone(): AudioDeviceInfo? =
        audio.getDevices(AudioManager.GET_DEVICES_INPUTS).firstOrNull {
            it.type == AudioDeviceInfo.TYPE_USB_DEVICE || it.type == AudioDeviceInfo.TYPE_USB_HEADSET
        }

    private val rawSource: Boolean
        get() = audio.getProperty(AudioManager.PROPERTY_SUPPORT_AUDIO_SOURCE_UNPROCESSED) == "true"

    /**
     * Plays the sequence once and returns the recording, or null if the phone wouldn't record or play.
     * [onProgress] goes from 0 to 1 as it plays. Needs the record permission, which the caller checks.
     */
    @SuppressLint("MissingPermission")
    suspend fun measure(onProgress: (Float) -> Unit): FloatArray? = withContext(Dispatchers.IO) {
        val signal = RoomMeasurement.signal()
        val rate = RoomMeasurement.SAMPLE_RATE
        val inFormat = AudioFormat.Builder()
            .setEncoding(AudioFormat.ENCODING_PCM_16BIT)
            .setSampleRate(rate)
            .setChannelMask(AudioFormat.CHANNEL_IN_MONO)
            .build()
        val minIn = AudioRecord.getMinBufferSize(rate, AudioFormat.CHANNEL_IN_MONO, AudioFormat.ENCODING_PCM_16BIT)
        val recorder = try {
            AudioRecord.Builder()
                .setAudioSource(if (rawSource) MediaRecorder.AudioSource.UNPROCESSED else MediaRecorder.AudioSource.MIC)
                .setAudioFormat(inFormat)
                .setBufferSizeInBytes(maxOf(minIn, rate) * 2)
                .build()
        } catch (e: Exception) {
            Log.w(TAG, "Could not record", e)
            return@withContext null
        }
        microphone()?.let { recorder.setPreferredDevice(it) }
        val outFormat = AudioFormat.Builder()
            .setEncoding(AudioFormat.ENCODING_PCM_FLOAT)
            .setSampleRate(rate)
            .setChannelMask(AudioFormat.CHANNEL_OUT_MONO)
            .build()
        val minOut = AudioTrack.getMinBufferSize(rate, AudioFormat.CHANNEL_OUT_MONO, AudioFormat.ENCODING_PCM_FLOAT)
        val player = try {
            AudioTrack.Builder()
                .setAudioAttributes(attributes)
                .setAudioFormat(outFormat)
                .setTransferMode(AudioTrack.MODE_STREAM)
                .setBufferSizeInBytes(maxOf(minOut, rate / 4 * Float.SIZE_BYTES))
                .build()
        } catch (e: Exception) {
            Log.w(TAG, "Could not play", e)
            recorder.release()
            return@withContext null
        }
        audio.requestAudioFocus(focus)
        val recording = FloatArray(signal.size)
        try {
            if (recorder.state != AudioRecord.STATE_INITIALIZED) return@withContext null
            recorder.startRecording()
            player.play()
            coroutineScope {
                // Plays on its own, so writing never waits on reading.
                launch {
                    var written = 0
                    val chunk = rate / 20
                    while (written < signal.size) {
                        ensureActive()
                        val n = player.write(signal, written, minOf(chunk, signal.size - written), AudioTrack.WRITE_BLOCKING)
                        if (n <= 0) break
                        written += n
                    }
                }
                val buffer = ShortArray(rate / 20)
                var read = 0
                while (read < recording.size) {
                    ensureActive()
                    val n = recorder.read(buffer, 0, minOf(buffer.size, recording.size - read))
                    if (n <= 0) break
                    for (i in 0 until n) recording[read + i] = buffer[i] / 32768f
                    read += n
                    onProgress(read.toFloat() / recording.size)
                }
            }
            recording
        } catch (e: Exception) {
            Log.w(TAG, "Measurement stopped", e)
            null
        } finally {
            runCatching { player.stop() }
            player.release()
            runCatching { recorder.stop() }
            recorder.release()
            audio.abandonAudioFocusRequest(focus)
        }
    }

    private companion object {
        const val TAG = "EigentoneMeasure"
    }
}
