package com.phonons.lab

import android.annotation.SuppressLint
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.media.AudioAttributes
import android.media.AudioDeviceInfo
import android.media.AudioFormat
import android.media.AudioManager
import android.media.AudioPlaybackCaptureConfiguration
import android.media.AudioRecord
import android.media.AudioTimestamp
import android.media.AudioTrack
import android.media.projection.MediaProjection
import android.media.projection.MediaProjectionManager
import android.os.IBinder
import android.os.Process
import android.util.Log
import kotlinx.coroutines.flow.MutableStateFlow
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.log10
import kotlin.math.max
import kotlin.math.sin

enum class OutputRoute(val label: String, val usage: Int, val personalOutputOnly: Boolean = false) {
    MEDIA("Media", AudioAttributes.USAGE_MEDIA),
    ALARM("Alarm", AudioAttributes.USAGE_ALARM),
    ALARM_PERSONAL("Alarm, earbuds only", AudioAttributes.USAGE_ALARM, personalOutputOnly = true),
    ACCESSIBILITY("Accessibility", AudioAttributes.USAGE_ASSISTANCE_ACCESSIBILITY),
    VOICE("Voice", AudioAttributes.USAGE_VOICE_COMMUNICATION),
}

data class LabStats(
    val running: Boolean = false,
    val capturedPeakDb: Double = -120.0,
    val heardSignal: Boolean = false,
    val musicActive: Boolean = false,
    val seconds: Int = 0,
    val underruns: Int = 0,
    val latencyMs: Int = 0,
    val routeApplied: String = "",
    val error: String? = null,
)

/** Controls read by the audio thread. */
object LabControls {
    @Volatile var route = OutputRoute.ALARM
    @Volatile var muffle = true
    @Volatile var outputLevel = 0.1f
    val stats = MutableStateFlow(LabStats())
}

/** Captures other apps' audio, optionally muffles it, and plays it back. */
class LabService : Service() {
    private var projection: MediaProjection? = null
    private var worker: Thread? = null
    @Volatile private var keepRunning = false

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (intent?.action == ACTION_STOP) {
            stopSelf()
            return START_NOT_STICKY
        }
        val code = intent?.getIntExtra(EXTRA_CODE, 0) ?: 0
        val data = intent?.getParcelableExtra(EXTRA_DATA, Intent::class.java)
        if (data == null) {
            stopSelf()
            return START_NOT_STICKY
        }
        createChannel()
        // Android requires the foreground service to exist before the projection is opened.
        startForeground(NOTIFICATION_ID, notification(), ServiceInfo.FOREGROUND_SERVICE_TYPE_MEDIA_PROJECTION)
        try {
            val mp = getSystemService(MediaProjectionManager::class.java).getMediaProjection(code, data)
                ?: error("Android returned no capture session")
            mp.registerCallback(object : MediaProjection.Callback() {
                override fun onStop() = stopSelf()
            }, null)
            projection = mp
            startWorker(mp)
        } catch (e: Exception) {
            Log.e(TAG, "Could not start capture", e)
            LabControls.stats.value = LabStats(error = "Could not start capture: ${e.message}")
            stopSelf()
        }
        return START_NOT_STICKY
    }

    private fun startWorker(mp: MediaProjection) {
        keepRunning = true
        worker = Thread({ runLoop(mp) }, "lab-audio").also { it.start() }
    }

    @SuppressLint("MissingPermission") // The activity only starts this after RECORD_AUDIO is granted.
    private fun runLoop(mp: MediaProjection) {
        Process.setThreadPriority(Process.THREAD_PRIORITY_URGENT_AUDIO)
        var record: AudioRecord? = null
        var track: AudioTrack? = null
        try {
            val format = AudioFormat.Builder()
                .setSampleRate(SAMPLE_RATE)
                .setChannelMask(AudioFormat.CHANNEL_IN_STEREO)
                .setEncoding(AudioFormat.ENCODING_PCM_FLOAT)
                .build()
            val capture = AudioPlaybackCaptureConfiguration.Builder(mp)
                .addMatchingUsage(AudioAttributes.USAGE_MEDIA)
                .addMatchingUsage(AudioAttributes.USAGE_GAME)
                .addMatchingUsage(AudioAttributes.USAGE_UNKNOWN)
                .build()
            val minBytes = AudioRecord.getMinBufferSize(
                SAMPLE_RATE, AudioFormat.CHANNEL_IN_STEREO, AudioFormat.ENCODING_PCM_FLOAT,
            )
            val rec = AudioRecord.Builder()
                .setAudioFormat(format)
                .setBufferSizeInBytes(max(minBytes, BLOCK_FRAMES * 2 * 4 * 4))
                .setAudioPlaybackCaptureConfig(capture)
                .build()
            record = rec
            rec.startRecording()

            val buffer = FloatArray(BLOCK_FRAMES * 2)
            val filters = arrayOf(LowPass(), LowPass())
            val audioManager = getSystemService(AudioManager::class.java)
            val recordStamp = AudioTimestamp()
            val trackStamp = AudioTimestamp()
            var activeRoute: OutputRoute? = null
            var framesRead = 0L
            var framesWritten = 0L
            var windowPeak = 0f
            var heard = false
            var windowFrames = 0
            val started = System.nanoTime()

            while (keepRunning) {
                if (activeRoute != LabControls.route) {
                    track?.let { it.pause(); it.flush(); it.release() }
                    activeRoute = LabControls.route
                    track = buildTrack(activeRoute)
                    track.play()
                    framesWritten = 0
                }
                val read = rec.read(buffer, 0, buffer.size, AudioRecord.READ_BLOCKING)
                if (read < 0) error("Capture read failed with code $read")
                var i = 0
                val muffle = LabControls.muffle
                val level = LabControls.outputLevel
                while (i < read) {
                    val channel = i and 1
                    val input = buffer[i]
                    windowPeak = max(windowPeak, abs(input))
                    val shaped = if (muffle) filters[channel].process(input) else input
                    buffer[i] = shaped * level
                    i++
                }
                val outTrack = track ?: continue
                outTrack.write(buffer, 0, read, AudioTrack.WRITE_BLOCKING)
                framesRead += read / 2
                framesWritten += read / 2
                windowFrames += read / 2

                if (windowFrames >= SAMPLE_RATE / STATS_PER_SECOND) {
                    val peakDb = if (windowPeak > 0f) 20 * log10(windowPeak.toDouble()) else -120.0
                    if (peakDb > SILENCE_DB) heard = true
                    var backlog = 0L
                    if (rec.getTimestamp(recordStamp, AudioTimestamp.TIMEBASE_MONOTONIC) == AudioRecord.SUCCESS) {
                        backlog += max(0L, recordStamp.framePosition - framesRead)
                    }
                    if (outTrack.getTimestamp(trackStamp)) {
                        backlog += max(0L, framesWritten - trackStamp.framePosition)
                    }
                    LabControls.stats.value = LabStats(
                        running = true,
                        capturedPeakDb = peakDb,
                        heardSignal = heard,
                        musicActive = audioManager.isMusicActive,
                        seconds = ((System.nanoTime() - started) / 1_000_000_000L).toInt(),
                        underruns = outTrack.underrunCount,
                        latencyMs = (backlog * 1000 / SAMPLE_RATE).toInt(),
                        routeApplied = activeRoute?.label.orEmpty(),
                    )
                    windowPeak = 0f
                    windowFrames = 0
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Capture loop failed", e)
            LabControls.stats.value = LabStats(error = "Capture stopped: ${e.message}")
        } finally {
            runCatching { record?.stop() }
            record?.release()
            runCatching { track?.stop() }
            track?.release()
        }
    }

    private fun buildTrack(route: OutputRoute): AudioTrack = AudioTrack.Builder()
        .setAudioAttributes(
            AudioAttributes.Builder()
                .setUsage(route.usage)
                .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
                // Stops this app capturing its own output, which would cause a feedback loop.
                .setAllowedCapturePolicy(AudioAttributes.ALLOW_CAPTURE_BY_NONE)
                .build(),
        )
        .setAudioFormat(
            AudioFormat.Builder()
                .setSampleRate(SAMPLE_RATE)
                .setChannelMask(AudioFormat.CHANNEL_OUT_STEREO)
                .setEncoding(AudioFormat.ENCODING_PCM_FLOAT)
                .build(),
        )
        .setPerformanceMode(AudioTrack.PERFORMANCE_MODE_LOW_LATENCY)
        .setBufferSizeInBytes(BLOCK_FRAMES * 2 * 4 * 2)
        .setTransferMode(AudioTrack.MODE_STREAM)
        .build()
        .also { track ->
            if (route.personalOutputOnly) {
                val devices = getSystemService(AudioManager::class.java).getDevices(AudioManager.GET_DEVICES_OUTPUTS)
                val personal = PERSONAL_OUTPUTS.firstNotNullOfOrNull { type -> devices.firstOrNull { it.type == type } }
                val accepted = personal != null && track.setPreferredDevice(personal)
                Log.i(TAG, "Preferred output ${personal?.productName} type=${personal?.type} accepted=$accepted")
            }
        }

    override fun onDestroy() {
        keepRunning = false
        worker?.join(1000)
        projection?.stop()
        // However capture ended, never leave the phone's music silently muted.
        getSystemService(AudioManager::class.java).adjustStreamVolume(
            AudioManager.STREAM_MUSIC, AudioManager.ADJUST_UNMUTE, 0,
        )
        getSharedPreferences(LabActivity.PREFS, MODE_PRIVATE).edit().putBoolean(LabActivity.KEY_MUTED, false).apply()
        val last = LabControls.stats.value
        LabControls.stats.value = last.copy(running = false)
        super.onDestroy()
    }

    override fun onBind(intent: Intent?): IBinder? = null

    private fun createChannel() {
        getSystemService(NotificationManager::class.java).createNotificationChannel(
            NotificationChannel(CHANNEL_ID, "Capture test", NotificationManager.IMPORTANCE_LOW),
        )
    }

    private fun notification(): Notification {
        val open = PendingIntent.getActivity(
            this, 0, Intent(this, LabActivity::class.java), PendingIntent.FLAG_IMMUTABLE,
        )
        return Notification.Builder(this, CHANNEL_ID)
            .setSmallIcon(android.R.drawable.ic_btn_speak_now)
            .setContentTitle("Capture test running")
            .setContentIntent(open)
            .setOngoing(true)
            .build()
    }

    /** Second-order 500 Hz low pass, deliberately obvious by ear. */
    private class LowPass {
        private val w = 2 * PI * 500.0 / SAMPLE_RATE
        private val alpha = sin(w) / (2 * 0.707)
        private val a0 = 1 + alpha
        private val b0 = ((1 - cos(w)) / 2 / a0).toFloat()
        private val b1 = ((1 - cos(w)) / a0).toFloat()
        private val a1 = (-2 * cos(w) / a0).toFloat()
        private val a2 = ((1 - alpha) / a0).toFloat()
        private var z1 = 0f
        private var z2 = 0f

        fun process(x: Float): Float {
            val y = b0 * x + z1
            z1 = b1 * x - a1 * y + z2
            z2 = b0 * x - a2 * y
            return y
        }
    }

    companion object {
        private const val TAG = "CaptureLab"
        private const val CHANNEL_ID = "lab"
        private const val NOTIFICATION_ID = 1
        private const val SAMPLE_RATE = 48_000
        private const val BLOCK_FRAMES = 480
        private const val STATS_PER_SECOND = 4
        private const val SILENCE_DB = -80.0
        private const val ACTION_STOP = "stop"
        private const val EXTRA_CODE = "code"
        private const val EXTRA_DATA = "data"
        private val PERSONAL_OUTPUTS = listOf(
            AudioDeviceInfo.TYPE_BLE_HEADSET,
            AudioDeviceInfo.TYPE_BLUETOOTH_A2DP,
            AudioDeviceInfo.TYPE_USB_HEADSET,
            AudioDeviceInfo.TYPE_WIRED_HEADPHONES,
            AudioDeviceInfo.TYPE_WIRED_HEADSET,
        )

        fun start(context: Context, code: Int, data: Intent) {
            context.startForegroundService(
                Intent(context, LabService::class.java).putExtra(EXTRA_CODE, code).putExtra(EXTRA_DATA, data),
            )
        }

        fun stop(context: Context) {
            context.stopService(Intent(context, LabService::class.java))
        }
    }
}
