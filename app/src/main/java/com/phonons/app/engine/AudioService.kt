package com.phonons.app.engine

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.ServiceInfo
import android.media.AudioDeviceCallback
import android.media.AudioDeviceInfo
import android.media.AudioManager
import android.media.audiofx.AudioEffect
import android.os.IBinder
import android.service.quicksettings.TileService
import android.content.ComponentName
import com.phonons.app.R
import com.phonons.app.data.AppState
import com.phonons.app.data.BuiltIns
import com.phonons.app.data.OutputInfo
import com.phonons.app.data.Profiles
import com.phonons.app.data.Repo
import com.phonons.app.ui.MainActivity
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.conflate
import kotlinx.coroutines.launch

/** Keeps the effect alive in the background and reacts to players and output changes. */
class AudioService : Service() {

    @OptIn(ExperimentalCoroutinesApi::class)
    private val engineDispatcher = Dispatchers.Default.limitedParallelism(1)
    private val scope = CoroutineScope(SupervisorJob() + engineDispatcher)

    private val engine = EffectEngine { Repo.status.value = it }
    private lateinit var audioManager: AudioManager
    private var lastNotified: Pair<Boolean, String>? = null
    @Volatile private var outputDeviceType = AudioDeviceInfo.TYPE_BUILTIN_SPEAKER

    private val sessionReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            val session = intent.getIntExtra(AudioEffect.EXTRA_AUDIO_SESSION, AudioEffect.ERROR)
            val pkg = intent.getStringExtra(AudioEffect.EXTRA_PACKAGE_NAME) ?: "Unknown app"
            scope.launch {
                when (intent.action) {
                    AudioEffect.ACTION_OPEN_AUDIO_EFFECT_CONTROL_SESSION ->
                        engine.sessionOpened(session, pkg)
                    AudioEffect.ACTION_CLOSE_AUDIO_EFFECT_CONTROL_SESSION ->
                        engine.sessionClosed(session)
                }
            }
        }
    }

    private val deviceCallback = object : AudioDeviceCallback() {
        override fun onAudioDevicesAdded(added: Array<out AudioDeviceInfo>) = onOutputChanged()
        override fun onAudioDevicesRemoved(removed: Array<out AudioDeviceInfo>) = onOutputChanged()
    }

    override fun onCreate() {
        super.onCreate()
        audioManager = getSystemService(AudioManager::class.java)
        createChannel()
        startInForeground(Repo.state.value)

        registerReceiver(
            sessionReceiver,
            IntentFilter().apply {
                addAction(AudioEffect.ACTION_OPEN_AUDIO_EFFECT_CONTROL_SESSION)
                addAction(AudioEffect.ACTION_CLOSE_AUDIO_EFFECT_CONTROL_SESSION)
            },
            // Players are other apps, so this has to accept external broadcasts.
            RECEIVER_EXPORTED,
        )
        audioManager.registerAudioDeviceCallback(deviceCallback, null)
        onOutputChanged(starting = true)

        engine.refreshRequested = { scope.launch { engine.refresh() } }
        scope.launch {
            while (true) {
                delay(WATCHDOG_INTERVAL_MS)
                engine.watchdog()
            }
        }
        scope.launch {
            while (true) {
                if (Repo.state.value.loudness) Repo.attenuationDb.value = currentAttenuationDb()
                delay(VOLUME_POLL_MS)
            }
        }
        scope.launch {
            combine(Repo.state, Repo.attenuationDb) { state, attenuation -> state to attenuation }
                .conflate()
                .collect { (state, attenuation) ->
                    if (!state.enabled) {
                        stopSelf()
                        return@collect
                    }
                    engine.attenuationDb = attenuation
                    engine.apply(state)
                    updateNotification(state)
                    delay(APPLY_INTERVAL_MS)
                }
        }
    }

    /** How far the music volume is below full. Some Bluetooth setups report 0 dB at every step. */
    private fun currentAttenuationDb(): Double {
        val stream = AudioManager.STREAM_MUSIC
        val index = audioManager.getStreamVolume(stream)
        val max = audioManager.getStreamMaxVolume(stream)
        if (index >= max) return 0.0
        val reported = runCatching {
            audioManager.getStreamVolumeDb(stream, index, outputDeviceType).toDouble()
        }.getOrDefault(0.0)
        val fallback = FALLBACK_RANGE_DB * (1.0 - index.toDouble() / max)
        return if (reported.isFinite() && reported < 0.0) (-reported).coerceAtMost(MAX_ATTENUATION_DB) else fallback
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_TURN_OFF -> Repo.update { it.copy(enabled = false) }
            ACTION_TOGGLE_BYPASS -> Repo.update { it.copy(bypass = !it.bypass) }
            ACTION_NEXT_PRESET -> Repo.nextPreset()
        }
        // A redelivered start after the process was killed must also respect the saved state.
        if (!Repo.state.value.enabled) stopSelf()
        return START_STICKY
    }

    override fun onDestroy() {
        unregisterReceiver(sessionReceiver)
        audioManager.unregisterAudioDeviceCallback(deviceCallback)
        engine.refreshRequested = null
        // Release on the engine dispatcher so it cannot overlap an apply in progress.
        CoroutineScope(engineDispatcher).launch { engine.release() }
        scope.cancel()
        requestTileRefresh(this)
        super.onDestroy()
    }

    override fun onBind(intent: Intent?): IBinder? = null

    /**
     * [starting] also applies the output's preset when nothing appears to have changed, because the
     * output may have changed while the service was not running.
     */
    private fun onOutputChanged(starting: Boolean = false) {
        val previous = Repo.output.value
        val info = currentOutput()
        Repo.output.value = info
        if (!starting && previous == info) return
        val state = Repo.state.value
        if (!state.autoSwitch) return

        val assigned = Profiles.presetIdFor(state, info.key)?.let(Repo::findPreset)
        if (assigned != null) {
            if (state.current.id != assigned.id) Repo.loadPreset(assigned)
            return
        }
        // Correction tuned for one output is wrong on another, so don't carry it across. Anything
        // the user has edited, or loaded by hand, is left alone.
        val cameFromAssigned = Profiles.isAssigned(state, previous.key, state.current.id)
        val isUnedited = Repo.findPreset(state.current.id) == state.current
        if (previous != info && cameFromAssigned && isUnedited) Repo.loadPreset(BuiltIns.flat)
    }

    private fun currentOutput(): OutputInfo {
        val devices = audioManager.getDevices(AudioManager.GET_DEVICES_OUTPUTS)
        val best = OUTPUT_PRIORITY.firstNotNullOfOrNull { type -> devices.firstOrNull { it.type == type } }
        outputDeviceType = best?.type ?: AudioDeviceInfo.TYPE_BUILTIN_SPEAKER
        if (best == null) return OutputInfo("speaker", "Phone speaker")
        val family = when (best.type) {
            AudioDeviceInfo.TYPE_BLUETOOTH_A2DP,
            AudioDeviceInfo.TYPE_BLE_HEADSET,
            AudioDeviceInfo.TYPE_BLE_SPEAKER -> "Bluetooth"
            AudioDeviceInfo.TYPE_USB_HEADSET,
            AudioDeviceInfo.TYPE_USB_DEVICE -> "USB"
            AudioDeviceInfo.TYPE_WIRED_HEADSET,
            AudioDeviceInfo.TYPE_WIRED_HEADPHONES -> "Wired"
            else -> "Speaker"
        }
        if (family == "Speaker") return OutputInfo("speaker", "Phone speaker")
        val name = best.productName?.toString().orEmpty().ifBlank { family }
        return OutputInfo("$family:$name", if (name == family) "$family headphones" else name)
    }

    private fun createChannel() {
        val channel = NotificationChannel(CHANNEL_ID, "Equaliser running", NotificationManager.IMPORTANCE_LOW)
        channel.setShowBadge(false)
        getSystemService(NotificationManager::class.java).createNotificationChannel(channel)
    }

    private fun startInForeground(state: AppState) {
        startForeground(NOTIFICATION_ID, buildNotification(state), ServiceInfo.FOREGROUND_SERVICE_TYPE_SPECIAL_USE)
    }

    private fun updateNotification(state: AppState) {
        val key = state.bypass to state.current.name
        if (key == lastNotified) return
        lastNotified = key
        getSystemService(NotificationManager::class.java).notify(NOTIFICATION_ID, buildNotification(state))
        requestTileRefresh(this)
    }

    private fun buildNotification(state: AppState): Notification {
        val open = PendingIntent.getActivity(
            this, 0, Intent(this, MainActivity::class.java), PendingIntent.FLAG_IMMUTABLE,
        )
        val bypassLabel = if (state.bypass) "Resume" else "Bypass"
        return Notification.Builder(this, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_phonons)
            .setContentTitle(state.current.name)
            .setContentText(if (state.bypass) "Bypassed" else "Equaliser on")
            .setContentIntent(open)
            .setOngoing(true)
            .setOnlyAlertOnce(true)
            .addAction(Notification.Action.Builder(null, "Next preset", servicePending(ACTION_NEXT_PRESET, 3)).build())
            .addAction(Notification.Action.Builder(null, bypassLabel, servicePending(ACTION_TOGGLE_BYPASS, 1)).build())
            .addAction(Notification.Action.Builder(null, "Turn off", servicePending(ACTION_TURN_OFF, 2)).build())
            .build()
    }

    private fun servicePending(action: String, code: Int): PendingIntent = PendingIntent.getService(
        this, code,
        Intent(this, AudioService::class.java).setAction(action),
        PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT,
    )

    companion object {
        private const val CHANNEL_ID = "engine"
        private const val NOTIFICATION_ID = 1
        private const val APPLY_INTERVAL_MS = 30L
        private const val WATCHDOG_INTERVAL_MS = 10_000L
        private const val VOLUME_POLL_MS = 1_000L
        private const val FALLBACK_RANGE_DB = 45.0
        private const val MAX_ATTENUATION_DB = 60.0
        private const val ACTION_NEXT_PRESET = "com.phonons.app.NEXT_PRESET"
        private const val ACTION_TURN_OFF = "com.phonons.app.TURN_OFF"
        private const val ACTION_TOGGLE_BYPASS = "com.phonons.app.TOGGLE_BYPASS"

        private val OUTPUT_PRIORITY = listOf(
            AudioDeviceInfo.TYPE_USB_HEADSET,
            AudioDeviceInfo.TYPE_USB_DEVICE,
            AudioDeviceInfo.TYPE_WIRED_HEADPHONES,
            AudioDeviceInfo.TYPE_WIRED_HEADSET,
            AudioDeviceInfo.TYPE_BLE_HEADSET,
            AudioDeviceInfo.TYPE_BLE_SPEAKER,
            AudioDeviceInfo.TYPE_BLUETOOTH_A2DP,
        )

        /** Throws ForegroundServiceStartNotAllowedException if the app may not start it right now. */
        fun start(context: Context) {
            context.startForegroundService(Intent(context, AudioService::class.java))
        }

        fun requestTileRefresh(context: Context) {
            TileService.requestListeningState(context, ComponentName(context, PhononsTile::class.java))
        }
    }
}
