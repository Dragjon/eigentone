package io.github.dragjon.eigentone.data

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.pow
import kotlin.math.sin

class RoomMeasurementTest {
    private val freqs = RoomMeasurement.FREQS

    /** A made-up speaker: weak below 80 Hz, a 6 dB peak around 1.6 kHz, otherwise flat. */
    private fun speakerDb(hz: Double): Double = when {
        hz < 80.0 -> -20.0
        abs(kotlin.math.log2(hz / 1600.0)) < 0.2 -> 6.0
        else -> 0.0
    }

    /** What the microphone would record: the signal, shaped by the speaker, late by [delayMs], plus noise. */
    private fun record(delayMs: Int, gainDb: Double = -6.0, noise: Double = 0.0005): FloatArray {
        val signal = RoomMeasurement.signal()
        val out = FloatArray(signal.size)
        val delay = RoomMeasurement.msToFrames(delayMs)
        val random = java.util.Random(7)
        // Rebuild each tone at the speaker's level for its frequency, which is what a real room would do.
        val marker = RoomMeasurement.msToFrames(RoomMeasurement.QUIET_MS)
        fun put(start: Int, frames: Int, hz: Double, db: Double) {
            val amplitude = 10.0.pow((RoomMeasurement.TONE_DB + gainDb + db) / 20)
            for (i in 0 until frames) {
                val at = start + i + delay
                if (at < out.size) out[at] += (amplitude * sin(2 * PI * hz * i / RoomMeasurement.SAMPLE_RATE)).toFloat()
            }
        }
        put(marker, RoomMeasurement.msToFrames(RoomMeasurement.MARKER_MS), RoomMeasurement.MARKER_HZ, 0.0)
        freqs.forEachIndexed { i, hz -> put(RoomMeasurement.toneStart(i), RoomMeasurement.msToFrames(RoomMeasurement.TONE_MS), hz, speakerDb(hz)) }
        for (i in out.indices) out[i] += (random.nextGaussian() * noise).toFloat()
        return out
    }

    @Test
    fun aFullScaleSineReadsZeroDb() {
        val n = 4800
        val sine = FloatArray(n) { sin(2 * PI * 1000.0 * it / RoomMeasurement.SAMPLE_RATE).toFloat() }
        assertEquals(0.0, RoomMeasurement.levelDb(sine, 0, n, 1000.0), 0.2)
        assertTrue(RoomMeasurement.levelDb(sine, 0, n, 2000.0) < -40)
    }

    @Test
    fun findsTheSpeakersDelay() {
        for (delayMs in listOf(0, 40, 250)) {
            val found = RoomMeasurement.delayFrames(record(delayMs))
            assertNotNull(found)
            assertEquals(delayMs.toDouble(), found!! * 1000.0 / RoomMeasurement.SAMPLE_RATE, 25.0)
        }
    }

    @Test
    fun cutsThePeakAndLeavesWhatTheSpeakerCantPlay() {
        val recording = record(200)
        val reading = RoomMeasurement.read(recording, RoomMeasurement.delayFrames(recording)!!)
        assertFalse(RoomMeasurement.tooQuiet(reading))
        val correction = RoomMeasurement.correction(reading)
        val at1600 = correction.minBy { abs(it.hz - 1600.0) }
        assertTrue("peak cut was ${at1600.db}", at1600.db < -2.5)
        // Deep bass the speaker barely plays isn't boosted.
        correction.filter { it.hz < 60.0 }.forEach { assertEquals(0.0, it.db, 0.0) }
        // The flat middle stays nearly untouched.
        correction.filter { it.hz in 300.0..800.0 }.forEach { assertTrue(abs(it.db) < 1.5) }
        // Nothing beyond the limits.
        correction.forEach { assertTrue(it.db in RoomMeasurement.MAX_CUT_DB..RoomMeasurement.MAX_BOOST_DB) }
    }

    @Test
    fun aSilentRecordingIsTooQuietToUse() {
        val random = java.util.Random(3)
        val silent = FloatArray(RoomMeasurement.totalFrames) { (random.nextGaussian() * 0.0005).toFloat() }
        assertNull(RoomMeasurement.delayFrames(silent))
        assertTrue(RoomMeasurement.tooQuiet(RoomMeasurement.read(silent, 0)))
    }

    @Test
    fun combiningSpotsAveragesPowerNotDecibels() {
        val a = RoomMeasurement.Reading(freqs.map { -20.0 }, freqs.map { -80.0 })
        val b = RoomMeasurement.Reading(freqs.map { -60.0 }, freqs.map { -70.0 })
        val both = RoomMeasurement.combine(listOf(a, b))
        // Power average of -20 and -60 dB is about -23 dB, not the -40 a plain average would give.
        assertEquals(-23.0, both.levels.first(), 0.1)
        assertEquals(-70.0, both.noise.first(), 0.0)
    }
}
