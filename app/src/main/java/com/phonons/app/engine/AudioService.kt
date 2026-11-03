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
import com.phonons.app.data.Profiles
import com.phonons.app.data.Repo
import com.phonons.app.ui.MainActivity
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.conflate
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch

/** Keeps the effect alive in the background and reacts to players and output changes. */
class AudioService : Service() {

    private data class Inputs(
        val state: AppState,
        val attenuation: Double,
        val night: Boolean,
        val suspended: Boolean,
        val comparing: Boolean,
        val outputKey: String,
    )

    @OptIn(ExperimentalCoroutinesApi::class)
    private val engineDispatcher = Dispatchers.Default.limitedParallelism(1)
    private val scope = CoroutineScope(SupervisorJob() + engineDispatcher)

    private val engine = EffectEngine { Repo.status.value = it }
    private lateinit var audioManager: AudioManager
    private val mediaWatcher by lazy { MediaWatcher(this, Repo::onPlayingAppChanged) }
    private var lastNotified: Pair<Boolean, String>? = null
    private var settleJob: Job? = null
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
        onOutputChanged()
        scope.launch {
            combine(Repo.comparing, Repo.hearingCheckRunning) { comparing, checking -> comparing || checking }
                .distinctUntilChanged()
                .collect { busy -> if (!busy) onOutputChanged() }
        }

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
            // Restarts when the schedule is edited, so a change is heard at once rather than at the next check.
            Repo.state.map { it.nightSchedule }.distinctUntilChanged().collectLatest { schedule ->
                while (true) {
                    val now = java.time.LocalTime.now()
                    Repo.scheduledNight.value = schedule.isActive(now.hour * 60 + now.minute)
                    delay(SCHEDULE_POLL_MS)
                }
            }
        }
        scope.launch {
            while (true) {
                mediaWatcher.attachIfAllowed()
                delay(MEDIA_ACCESS_POLL_MS)
            }
        }
        scope.launch {
            combine(
                Repo.state, Repo.attenuationDb, Repo.scheduledNight, Repo.hearingCheckRunning,
                combine(Repo.comparing, Repo.output) { comparing, output -> comparing to output.key },
            ) { state, attenuation, night, suspended, (comparing, outputKey) ->
                Inputs(state, attenuation, night, suspended, comparing, outputKey)
            }
                .conflate()
                .collect { (state, attenuation, night, suspended, comparing, outputKey) ->
                    if (!state.enabled) {
                        stopSelf()
                        return@collect
                    }
                    engine.attenuationDb = attenuation
                    engine.scheduledNight = night
                    engine.suspended = suspended
                    engine.comparing = comparing
                    engine.outputKey = outputKey
                    engine.apply(state)
                    keepSettling()
                    updateNotification(state)
                    delay(APPLY_INTERVAL_MS)
                }
        }
    }

    /** Finishes a move to a new sound. Only runs during one, so an idle service has no timer to wake for. */
    private fun keepSettling() {
        if (!engine.settling || settleJob?.isActive == true) return
        settleJob = scope.launch {
            while (engine.settling) {
                delay(APPLY_INTERVAL_MS)
                engine.apply(Repo.state.value)
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
        mediaWatcher.release()
        PhononsWidget.refresh(this)
        engine.refreshRequested = null
        // Release on the engine dispatcher so it cannot overlap an apply in progress.
        CoroutineScope(engineDispatcher).launch { engine.release() }
        scope.cancel()
        requestTileRefresh(this)
        super.onDestroy()
    }

    override fun onBind(intent: Intent?): IBinder? = null

    /**
     * Compares with the output last acted on, which is saved, so turning the EQ off and on keeps a preset
     * picked by hand, while headphones changed during that time are still noticed. Waits for an open tool,
     * which is using the sound and is checked again when it closes.
     */
    @Synchronized
    private fun onOutputChanged() {
        val detected = OutputDetector.current(this)
        val info = detected.info
        outputDeviceType = detected.deviceType
        Repo.output.value = info
        Repo.update { it.withLegacyEarsMovedTo(info) }
        if (Repo.toolBusy) return
        val state = Repo.state.value
        if (state.lastOutputKey == info.key) return
        Repo.update { it.copy(lastOutputKey = info.key) }
        if (!state.autoSwitch) return
        Profiles.presetForNewOutput(state, state.lastOutputKey, info.key, Repo::findPreset)?.let(Repo::loadPreset)
    }

    private fun createChannel() {
        // Low importance files it under silent notifications at the bottom. This ranks it with ordinary
        // ones, and it still makes no sound. A channel's importance can't be raised later, hence a new one.
        val channel = NotificationChannel(CHANNEL_ID, getString(R.string.engine_channel), NotificationManager.IMPORTANCE_DEFAULT)
        channel.setShowBadge(false)
        channel.setSound(null, null)
        channel.enableVibration(false)
        val manager = getSystemService(NotificationManager::class.java)
        manager.deleteNotificationChannel(OLD_CHANNEL_ID)
        manager.createNotificationChannel(channel)
    }

    private fun startInForeground(state: AppState) {
        startForeground(NOTIFICATION_ID, buildNotification(state), ServiceInfo.FOREGROUND_SERVICE_TYPE_SPECIAL_USE)
    }

    private fun updateNotification(state: AppState) {
        // A blind comparison's sound is named after its preset, which would give the answer away.
        if (Repo.comparing.value) return
        val key = state.bypass to state.current.name
        if (key == lastNotified) return
        lastNotified = key
        getSystemService(NotificationManager::class.java).notify(NOTIFICATION_ID, buildNotification(state))
        requestTileRefresh(this)
        PhononsWidget.refresh(this)
    }

    private fun buildNotification(state: AppState): Notification {
        val open = PendingIntent.getActivity(
            this, 0, Intent(this, MainActivity::class.java), PendingIntent.FLAG_IMMUTABLE,
        )
        val bypassLabel = getString(if (state.bypass) R.string.engine_resume else R.string.widget_bypass)
        return Notification.Builder(this, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_phonons)
            .setContentTitle(state.current.name)
            .setContentText(getString(if (state.bypass) R.string.ui_bypassed else R.string.engine_on))
            .setContentIntent(open)
            .setOngoing(true)
            .setOnlyAlertOnce(true)
            .setCategory(Notification.CATEGORY_SERVICE)
            .setColor(NOTIFICATION_COLOUR)
            .addAction(Notification.Action.Builder(null, getString(R.string.engine_next_preset), servicePending(ACTION_NEXT_PRESET, 3)).build())
            .addAction(Notification.Action.Builder(null, bypassLabel, servicePending(ACTION_TOGGLE_BYPASS, 1)).build())
            .addAction(Notification.Action.Builder(null, getString(R.string.engine_turn_off), servicePending(ACTION_TURN_OFF, 2)).build())
            .build()
    }

    private fun servicePending(action: String, code: Int): PendingIntent = PendingIntent.getService(
        this, code,
        Intent(this, AudioService::class.java).setAction(action),
        PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT,
    )

    companion object {
        private const val CHANNEL_ID = "engine_status"
        private const val OLD_CHANNEL_ID = "engine"
        private const val NOTIFICATION_ID = 1
        private const val NOTIFICATION_COLOUR = 0xFFFFB454.toInt()
        private const val APPLY_INTERVAL_MS = 30L
        private const val WATCHDOG_INTERVAL_MS = 10_000L
        private const val VOLUME_POLL_MS = 1_000L
        private const val SCHEDULE_POLL_MS = 30_000L
        private const val MEDIA_ACCESS_POLL_MS = 15_000L
        private const val FALLBACK_RANGE_DB = 45.0
        private const val MAX_ATTENUATION_DB = 60.0
        private const val ACTION_NEXT_PRESET = "com.phonons.app.NEXT_PRESET"
        private const val ACTION_TURN_OFF = "com.phonons.app.TURN_OFF"
        private const val ACTION_TOGGLE_BYPASS = "com.phonons.app.TOGGLE_BYPASS"

        /** Throws ForegroundServiceStartNotAllowedException if the app may not start it right now. */
        fun start(context: Context) {
            context.startForegroundService(Intent(context, AudioService::class.java))
        }

        fun requestTileRefresh(context: Context) {
            TileService.requestListeningState(context, ComponentName(context, PhononsTile::class.java))
        }
    }
}
