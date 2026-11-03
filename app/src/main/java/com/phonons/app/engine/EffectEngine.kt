package com.phonons.app.engine

import android.media.AudioFormat
import android.media.audiofx.DynamicsProcessing
import android.media.audiofx.Virtualizer
import android.util.Log
import com.phonons.app.data.AppState
import com.phonons.app.data.Compressor
import com.phonons.app.data.EngineMode
import com.phonons.app.data.EngineStatus
import com.phonons.app.data.GainRamp
import com.phonons.app.data.Processing
import com.phonons.app.data.ProcessingTarget
import com.phonons.app.dsp.CurveMapper
import kotlin.math.abs
import kotlin.math.log10
import kotlin.math.max

/**
 * Drives android.media.audiofx.DynamicsProcessing.
 *
 * Global mode attaches to session 0, which is deprecated and varies by device. Per-app mode attaches
 * to the sessions music players announce, and exists as the fallback. They never run together,
 * because stacking them would apply the EQ twice.
 *
 * Not thread safe: all calls must come from the service's single engine dispatcher.
 */
class EffectEngine(private val onStatus: (EngineStatus) -> Unit) {

    private class Chain(val dp: DynamicsProcessing, val session: Int, val label: String, bandCount: Int) {
        var virtualizer: Virtualizer? = null
        var lastSurround = Double.NaN
        var lastLimiterThreshold = Double.NaN
        val lastGains = FloatArray(bandCount) { Float.NaN }
        val lastRightGains = FloatArray(bandCount) { Float.NaN }
        var lastInput = Float.NaN
        var lastBalance = Double.NaN
        var lastDynamics: Compressor? = null
        var lastOff: Boolean? = null
        var moving = false
        var movingSince = 0L
        var lastLimiter: Boolean? = null
        var lastEnabled: Boolean? = null
        var verified = false
    }

    private val chains = LinkedHashMap<Int, Chain>()
    private val pendingSessions = LinkedHashMap<Int, String>()
    private var mapper: CurveMapper? = null
    private var activeMode: EngineMode? = null
    private var builtLowLatency: Boolean? = null
    private var builtMode: EngineMode? = null
    private var error: String? = null
    private var spatialError: String? = null
    private var latest: AppState? = null

    private var rebuilds = 0
    private var readback: String? = null

    /** Set by the service before [apply]. */
    var attenuationDb = 0.0

    /** Set by the service before [apply]. Whether the time of day is inside the night mode schedule. */
    var scheduledNight = false

    /** Set by the service before [apply]. Takes the sound out of the path during the hearing check. */
    var suspended = false

    /** Whether the sound is still moving to its target, so [apply] needs calling again shortly. */
    val settling: Boolean get() = chains.values.any { it.moving }

    fun apply(state: AppState) {
        latest = state
        if (builtMode != state.engineMode || builtLowLatency != state.lowLatency) rebuild(state)
        chains.values.forEach { push(it, state) }
        publish()
    }

    /** A music player opened a session. Remembered even in global mode, so switching works. */
    fun sessionOpened(session: Int, packageName: String) {
        if (session <= 0) return
        pendingSessions[session] = packageName
        if (activeMode == EngineMode.SESSIONS && session !in chains) attach(session, packageName)
        latest?.let { state -> chains[session]?.let { push(it, state) } }
        publish()
    }

    fun sessionClosed(session: Int) {
        pendingSessions.remove(session)
        if (activeMode == EngineMode.SESSIONS) {
            chains.remove(session)?.let { releaseChain(it) }
            // A failure for a session that has since gone is no longer worth showing.
            error = null
        }
        publish()
    }

    /** Another app took or returned control of an effect, so resend everything. */
    fun refresh() {
        chains.values.forEach { chain ->
            chain.lastGains.fill(Float.NaN)
            chain.lastRightGains.fill(Float.NaN)
            chain.lastInput = Float.NaN
            chain.lastBalance = Double.NaN
            chain.lastDynamics = null
            chain.moving = false
            chain.lastLimiter = null
            chain.lastEnabled = null
            chain.lastLimiterThreshold = Double.NaN
            chain.lastSurround = Double.NaN
        }
        latest?.let { apply(it) }
    }

    /**
     * Rebuilds the effect if it died, for example when Android's audio server restarted, or retries
     * global attachment if it failed earlier. Without this, the EQ would silently stay off.
     */
    fun watchdog() {
        val state = latest ?: return
        val broken = chains.values.any { chain ->
            runCatching { chain.dp.getPreEqBandByChannelIndex(0, 0) }.isFailure
        }
        val shouldBeAttached = activeMode != EngineMode.SESSIONS && chains.isEmpty()
        if (broken || shouldBeAttached) {
            Log.w(TAG, "Watchdog rebuilding effect: broken=$broken unattached=$shouldBeAttached")
            rebuilds++
            builtMode = null
            apply(state)
        }
    }

    fun release() {
        releaseAll()
        mapper = null
        activeMode = null
        builtMode = null
        builtLowLatency = null
        onStatus(EngineStatus())
    }

    private fun releaseAll() {
        chains.values.forEach { releaseChain(it) }
        chains.clear()
    }

    private fun releaseChain(chain: Chain) {
        releaseVirtualizer(chain)
        runCatching {
            chain.dp.enabled = false
            chain.dp.release()
        }
    }

    private fun rebuild(state: AppState) {
        releaseAll()
        error = null
        builtMode = state.engineMode
        builtLowLatency = state.lowLatency
        activeMode = null

        if (state.engineMode != EngineMode.SESSIONS) {
            if (attach(GLOBAL_SESSION, "All apps")) {
                activeMode = EngineMode.GLOBAL
                return
            }
            if (state.engineMode == EngineMode.GLOBAL) return
        }
        // Per-app mode. Any error from a failed Auto attempt stays visible in status.
        activeMode = EngineMode.SESSIONS
        pendingSessions.forEach { (session, name) -> attach(session, name) }
    }

    /** Tries the finest band count first, because support varies between devices. */
    private fun attach(session: Int, label: String): Boolean {
        val lowLatency = builtLowLatency == true
        val counts = mapper?.let { listOf(it.bandCount) } ?: BAND_COUNTS
        var failure: Exception? = null
        for (count in counts) {
            try {
                val config = DynamicsProcessing.Config.Builder(
                    DynamicsProcessing.VARIANT_FAVOR_FREQUENCY_RESOLUTION,
                    CHANNELS,
                    /* preEqInUse = */ true, count,
                    /* mbcInUse = */ true, MBC_BANDS,
                    /* postEqInUse = */ false, 0,
                    /* limiterInUse = */ true,
                )
                    .setPreferredFrameDuration(if (lowLatency) FRAME_MS_LOW_LATENCY else FRAME_MS_PRECISE)
                    .build()
                val dp = DynamicsProcessing(Int.MAX_VALUE, session, config)
                dp.setControlStatusListener { _, _ -> refreshRequested?.invoke() }
                if (mapper?.bandCount != count) mapper = CurveMapper(count)
                chains[session] = Chain(dp, session, label, count)
                Log.i(TAG, "Attached to session $session with $count bands")
                return true
            } catch (e: Exception) {
                Log.w(TAG, "Session $session refused $count bands: ${e.message}")
                failure = e
            }
        }
        error = "Could not attach to $label: ${failure?.message ?: "unknown error"}"
        return false
    }

    /** Set by the service so a control change hops back onto the engine dispatcher. */
    var refreshRequested: (() -> Unit)? = null

    /** Sends only what changed, because every parameter is a separate call into the audio system. */
    private fun push(chain: Chain, state: AppState) {
        val map = mapper ?: return
        val dp = chain.dp
        val target = Processing.target(
            if (suspended) state.copy(bypass = true, matchBypass = false) else state,
            attenuationDb,
            scheduledNight,
        )
        try {
            val gains = map.gains(target.filters)
            val rightGains = if (target.rightFilters === target.filters) gains else map.gains(target.rightFilters)
            // Comparing with Bypass needs a clean switch, and the hearing check needs the EQ gone at once.
            val off = suspended || state.bypass
            val step = GainRamp.next(
                chain.lastGains, chain.lastRightGains, chain.lastInput,
                gains, rightGains, target.inputDb.toFloat(),
                instant = suspended || chain.lastOff != off,
            )
            chain.lastOff = off

            // Each band is a separate call, so a boost could be heard before the level comes down for it.
            // Lowering first and raising last keeps every moment during the update free of clipping.
            val goingDown = chain.lastInput.isNaN() || step.inputDb < chain.lastInput
            if (goingDown) pushInput(chain, step.inputDb, state.balance)
            pushBands(dp, map, /* channel = */ 0, step.left, chain.lastGains)
            pushBands(dp, map, /* channel = */ 1, step.right, chain.lastRightGains)
            if (!goingDown) pushInput(chain, step.inputDb, state.balance)
            logMove(chain, step.done)

            if (!chain.verified) {
                chain.verified = true
                verifyBands(chain, map, gains, rightGains)
            }

            if (target.limiter != chain.lastLimiter || target.limiterThresholdDb != chain.lastLimiterThreshold) {
                dp.setLimiterAllChannelsTo(
                    DynamicsProcessing.Limiter(
                        true, target.limiter, /* linkGroup = */ 0,
                        LIMITER_ATTACK_MS, LIMITER_RELEASE_MS, LIMITER_RATIO, target.limiterThresholdDb.toFloat(), 0f,
                    ),
                )
                chain.lastLimiter = target.limiter
                chain.lastLimiterThreshold = target.limiterThresholdDb
            }

            if (target.dynamics != chain.lastDynamics) {
                pushDynamics(chain, target.dynamics)
                chain.lastDynamics = target.dynamics
            }

            val wanted = target.effectEnabled
            if (chain.lastEnabled != wanted) {
                dp.enabled = wanted
                chain.lastEnabled = wanted
            }
        } catch (e: Exception) {
            Log.w(TAG, "Could not update ${chain.label}", e)
            error = "Lost connection to ${chain.label}: ${e.message}"
        }
        pushSurround(chain, target)
    }

    private fun pushInput(chain: Chain, input: Float, balance: Double) {
        if (input == chain.lastInput && balance == chain.lastBalance) return
        chain.dp.setInputGainbyChannel(0, input + channelTrimDb(-balance))
        chain.dp.setInputGainbyChannel(1, input + channelTrimDb(balance))
        chain.lastInput = input
        chain.lastBalance = balance
    }

    /** Timed in the log, because how long a move takes depends on how fast the phone accepts bands. */
    private fun logMove(chain: Chain, done: Boolean) {
        if (!done && !chain.moving) chain.movingSince = System.nanoTime()
        if (done && chain.moving) {
            Log.i(TAG, "Sound moved on ${chain.label} in ${(System.nanoTime() - chain.movingSince) / 1_000_000} ms")
        }
        chain.moving = !done
    }

    private fun pushBands(dp: DynamicsProcessing, map: CurveMapper, channel: Int, gains: FloatArray, sent: FloatArray) {
        for (i in gains.indices) {
            val last = sent[i]
            if (last.isNaN() || abs(last - gains[i]) >= GAIN_EPSILON) {
                dp.setPreEqBandByChannelIndex(channel, i, DynamicsProcessing.EqBand(true, map.cutoffs[i], gains[i]))
                sent[i] = gains[i]
            }
        }
    }

    /** Only exists while in use, so turning surround off fully removes it from the audio path. */
    private fun pushSurround(chain: Chain, target: ProcessingTarget) {
        if (target.surround == chain.lastSurround) return
        chain.lastSurround = target.surround
        if (target.surround <= 0.0) {
            releaseVirtualizer(chain)
            return
        }
        try {
            val virtualizer = chain.virtualizer ?: Virtualizer(Int.MAX_VALUE, chain.session).also {
                chain.virtualizer = it
                spatialError = null
            }
            virtualizer.setStrength((target.surround * MAX_EFFECT_STRENGTH).toInt().toShort())
            virtualizer.enabled = true
            // Automatic mode can leave it inactive on Bluetooth, so ask for headphone processing.
            val stereo = AudioFormat.CHANNEL_OUT_STEREO
            val canBinaural = virtualizer.canVirtualize(stereo, Virtualizer.VIRTUALIZATION_MODE_BINAURAL)
            val forced = virtualizer.forceVirtualizationMode(Virtualizer.VIRTUALIZATION_MODE_BINAURAL)
            val mode = virtualizer.virtualizationMode
            Log.i(
                TAG,
                "Surround on ${chain.label}: strength=${virtualizer.roundedStrength} canBinaural=$canBinaural " +
                    "forced=$forced mode=$mode enabled=${virtualizer.enabled} control=${virtualizer.hasControl()}",
            )
            // Don't treat mode 0 as failure: on a Galaxy A54 it reads 0 while audibly working.
        } catch (e: Exception) {
            Log.w(TAG, "Surround unavailable on ${chain.label}", e)
            spatialError = "This phone refused 3D surround: ${e.message}"
            releaseVirtualizer(chain)
        }
    }

    private fun releaseVirtualizer(chain: Chain) {
        chain.virtualizer?.let { v ->
            runCatching {
                v.enabled = false
                v.release()
            }
        }
        chain.virtualizer = null
    }

    /** Reads bands back once, so a device that silently ignores them shows up as a visible error. */
    private fun verifyBands(chain: Chain, map: CurveMapper, left: FloatArray, right: FloatArray) {
        fun wrong(channel: Int, gains: FloatArray) = gains.indices.count { i ->
            val band = chain.dp.getPreEqBandByChannelIndex(channel, i)
            abs(band.cutoffFrequency - map.cutoffs[i]) > 1f || abs(band.gain - gains[i]) > 0.1f
        }
        val total = left.size + right.size
        val rejected = wrong(0, left) + wrong(1, right)
        Log.i(TAG, "Band readback on ${chain.label}: ${total - rejected}/$total match across both channels")
        readback = "${total - rejected}/$total"
        if (rejected > 0) error = "${chain.label} did not accept $rejected of $total EQ bands"
    }

    private fun pushDynamics(chain: Chain, dynamics: Compressor) {
        val cutoffs = floatArrayOf(dynamics.lowCrossoverHz.toFloat(), dynamics.highCrossoverHz.toFloat(), MBC_TOP_HZ)
        val mbc = DynamicsProcessing.Mbc(true, dynamics.enabled, MBC_BANDS)
        dynamics.bands.forEachIndexed { i, band ->
            mbc.setBand(
                i,
                DynamicsProcessing.MbcBand(
                    dynamics.enabled && band.enabled, cutoffs[i], band.attackMs.toFloat(), band.releaseMs.toFloat(),
                    band.ratio.toFloat(), band.thresholdDb.toFloat(), band.kneeDb.toFloat(),
                    /* gate */ -90f, /* expander */ 1f, /* pre */ 0f, band.makeupDb.toFloat(),
                ),
            )
        }
        chain.dp.setMbcAllChannelsTo(mbc)
        if (dynamics.enabled) logDynamics(chain)
    }

    /** Reads back what the phone kept, so a device that changes these values shows up in the log. */
    private fun logDynamics(chain: Chain) {
        val bands = (0 until MBC_BANDS).map { chain.dp.getMbcBandByChannelIndex(1, it) }
        val summary = bands.joinToString { b ->
            "${b.cutoffFrequency.toInt()}Hz on=${b.isEnabled} ${b.threshold}dB ${b.ratio}:1 +${b.postGain}dB"
        }
        Log.i(TAG, "Compressor on ${chain.label}: $summary")
    }

    private fun channelTrimDb(amount: Double): Float {
        if (amount <= 0) return 0f
        return (20 * log10(max(1.0 - amount, 0.001))).toFloat()
    }

    private fun publish() {
        val control = chains.values.all { runCatching { it.dp.hasControl() }.getOrDefault(false) }
        onStatus(
            EngineStatus(
                running = chains.isNotEmpty() || activeMode == EngineMode.SESSIONS,
                mode = when (activeMode) {
                    EngineMode.GLOBAL -> "Global"
                    EngineMode.SESSIONS -> "Per app"
                    else -> "Unavailable"
                },
                bandCount = mapper?.bandCount ?: 0,
                hasControl = control,
                sessions = if (activeMode == EngineMode.SESSIONS) chains.values.map { it.label } else emptyList(),
                error = error,
                spatialError = spatialError,
                rebuilds = rebuilds,
                readback = readback,
            ),
        )
    }

    companion object {
        private const val TAG = "PhononsEngine"
        private const val GLOBAL_SESSION = 0
        private const val CHANNELS = 2
        private const val MBC_BANDS = 3
        private const val GAIN_EPSILON = 0.02f
        private const val FRAME_MS_PRECISE = 20f
        private const val FRAME_MS_LOW_LATENCY = 10f
        private const val LIMITER_ATTACK_MS = 1f
        private const val LIMITER_RELEASE_MS = 60f
        private const val LIMITER_RATIO = 10f
        private const val MAX_EFFECT_STRENGTH = 1000
        private val BAND_COUNTS = listOf(128, 64, 32, 16)
        private const val MBC_TOP_HZ = 24000f
    }
}
