package com.phonons.app.engine

import android.media.audiofx.DynamicsProcessing
import android.media.audiofx.PresetReverb
import android.media.audiofx.Virtualizer
import android.util.Log
import com.phonons.app.data.AppState
import com.phonons.app.data.EngineMode
import com.phonons.app.data.EngineStatus
import com.phonons.app.data.Processing
import com.phonons.app.data.ProcessingTarget
import com.phonons.app.data.ReverbChoice
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
        var reverb: PresetReverb? = null
        var lastSurround = Double.NaN
        var lastReverb: ReverbChoice? = null
        var lastLimiterThreshold = Double.NaN
        val lastGains = FloatArray(bandCount) { Float.NaN }
        var lastInput = Float.NaN
        var lastBalance = Double.NaN
        var lastNight = Double.NaN
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

    /** Set by the service before [apply]. */
    var attenuationDb = 0.0

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
            chain.lastInput = Float.NaN
            chain.lastBalance = Double.NaN
            chain.lastNight = Double.NaN
            chain.lastLimiter = null
            chain.lastEnabled = null
            chain.lastLimiterThreshold = Double.NaN
            chain.lastSurround = Double.NaN
            chain.lastReverb = null
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
        releaseReverb(chain)
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
        val target = Processing.target(state, attenuationDb)
        try {
            val gains = map.gains(target.filters)
            for (i in gains.indices) {
                val last = chain.lastGains[i]
                if (last.isNaN() || abs(last - gains[i]) >= GAIN_EPSILON) {
                    dp.setPreEqBandAllChannelsTo(
                        i, DynamicsProcessing.EqBand(true, map.cutoffs[i], gains[i]),
                    )
                    chain.lastGains[i] = gains[i]
                }
            }

            if (!chain.verified) {
                chain.verified = true
                verifyBands(chain, map, gains)
            }

            val input = target.inputDb.toFloat()
            if (input != chain.lastInput || state.balance != chain.lastBalance) {
                dp.setInputGainbyChannel(0, input + channelTrimDb(-state.balance))
                dp.setInputGainbyChannel(1, input + channelTrimDb(state.balance))
                chain.lastInput = input
                chain.lastBalance = state.balance
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

            if (target.nightMode != chain.lastNight) {
                pushNightMode(dp, target.nightMode)
                chain.lastNight = target.nightMode
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
        pushReverb(chain, target)
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
            Log.i(
                TAG,
                "Surround on ${chain.label}: strength=${virtualizer.roundedStrength} " +
                    "supported=${virtualizer.strengthSupported} mode=${virtualizer.virtualizationMode} " +
                    "enabled=${virtualizer.enabled} control=${virtualizer.hasControl()}",
            )
        } catch (e: Exception) {
            Log.w(TAG, "Surround unavailable on ${chain.label}", e)
            spatialError = "This phone refused 3D surround: ${e.message}"
            releaseVirtualizer(chain)
        }
    }

    private fun pushReverb(chain: Chain, target: ProcessingTarget) {
        if (target.reverb == chain.lastReverb) return
        chain.lastReverb = target.reverb
        if (target.reverb == ReverbChoice.OFF) {
            releaseReverb(chain)
            return
        }
        try {
            val reverb = chain.reverb ?: PresetReverb(Int.MAX_VALUE, chain.session).also {
                chain.reverb = it
                spatialError = null
            }
            reverb.preset = target.reverb.androidPreset
            reverb.enabled = true
            Log.i(TAG, "Reverb on ${chain.label}: preset=${reverb.preset} enabled=${reverb.enabled} control=${reverb.hasControl()}")
        } catch (e: Exception) {
            Log.w(TAG, "Reverb unavailable on ${chain.label}", e)
            spatialError = "This phone refused reverb: ${e.message}"
            releaseReverb(chain)
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

    private fun releaseReverb(chain: Chain) {
        chain.reverb?.let { r ->
            runCatching {
                r.enabled = false
                r.release()
            }
        }
        chain.reverb = null
    }

    /** Reads bands back once, so a device that silently ignores them shows up as a visible error. */
    private fun verifyBands(chain: Chain, map: CurveMapper, gains: FloatArray) {
        val wrong = gains.indices.count { i ->
            val band = chain.dp.getPreEqBandByChannelIndex(0, i)
            abs(band.cutoffFrequency - map.cutoffs[i]) > 1f || abs(band.gain - gains[i]) > 0.1f
        }
        Log.i(TAG, "Band readback on ${chain.label}: ${gains.size - wrong}/${gains.size} match")
        if (wrong > 0) error = "${chain.label} did not accept $wrong of ${gains.size} EQ bands"
    }

    /** Evens out loud and quiet parts, with makeup gain so overall level stays similar. */
    private fun pushNightMode(dp: DynamicsProcessing, amount: Double) {
        val on = amount > 0.001
        val threshold = (-30.0 * amount).toFloat()
        val ratio = (1.0 + 3.0 * amount).toFloat()
        val makeup = (9.0 * amount).toFloat()
        val mbc = DynamicsProcessing.Mbc(true, on, MBC_BANDS)
        MBC_CUTOFFS.forEachIndexed { i, cutoff ->
            mbc.setBand(
                i,
                DynamicsProcessing.MbcBand(
                    on, cutoff, /* attack */ 8f, /* release */ 120f, ratio, threshold,
                    /* knee */ 6f, /* gate */ -90f, /* expander */ 1f, /* pre */ 0f, makeup,
                ),
            )
        }
        dp.setMbcAllChannelsTo(mbc)
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
        private val MBC_CUTOFFS = floatArrayOf(200f, 2500f, 24000f)
    }
}
