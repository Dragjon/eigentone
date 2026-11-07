package io.github.dragjon.eigentone.data

import io.github.dragjon.eigentone.dsp.GraphicEq
import kotlinx.serialization.Serializable
import kotlin.math.ln

@Serializable
enum class HeadphoneForm { IN_EAR, OVER_EAR }

/**
 * What a measured correction is aiming for, read from its AutoEq result. Published corrections don't all
 * aim at the same bass: among in-ear results, some carry the Harman lift and some have none.
 */
@Serializable
data class CorrectionInfo(val form: HeadphoneForm, val aim: List<ShapePoint>)

/** Sound signatures a measured correction can aim for instead of the one it was published with. */
enum class HeadphoneTarget(val id: String) {
    /** The curve most listeners preferred in Harman's tests. */
    HARMAN("harman"),

    /** 2 dB less bass than Harman, preferred by 21% of those listeners. */
    LESS_BASS("harman-less-bass"),

    /** 5 dB more bass than Harman, within the 4 to 6 dB preferred by 15% of them. */
    MORE_BASS("harman-more-bass"),

    /** Harman's target with its bass lift taken out. */
    NO_BASS_LIFT("harman-no-bass"),

    /** The older reference target: lean bass and brighter treble. */
    DIFFUSE_FIELD("diffuse-field"),
    ;

    companion object {
        fun of(id: String?): HeadphoneTarget? = entries.firstOrNull { it.id == id }
    }
}

/**
 * Works out the change that moves a correction to a chosen target. Below 500 Hz this compares the target
 * with what the correction actually aims for, so it's right whichever bass it was published with. Target
 * files come from AutoEq (MIT License) and the preference amounts from Olive et al. at Harman.
 */
object HeadphoneTargets {
    private val LOW = listOf(20.0, 31.5, 50.0, 80.0, 125.0, 200.0, 315.0, 500.0)
    private val HIGH = listOf(800.0, 1000.0, 1600.0, 2500.0, 4000.0, 6300.0, 8000.0, 10000.0, 12500.0, 16000.0)

    // Levels relative to 1 kHz, from AutoEq's target files.
    private val HARMAN_OVER = listOf(3.9, 3.9, 3.3, 2.0, -0.1, -2.0, -1.5, -0.8)
    private val HARMAN_IN = listOf(7.6, 7.7, 6.7, 4.2, 1.4, -1.0, -1.7, -1.4)
    private val NO_BASS_OVER = listOf(-2.1, -2.0, -2.4, -2.5, -2.1, -2.4, -1.6, -0.9)
    private val NO_BASS_IN = listOf(-1.4, -1.2, -1.8, -2.4, -1.6, -1.7, -1.8, -1.4)
    private val DIFFUSE_LOW_OVER = listOf(-7.7, -7.7, -7.1, -5.8, -3.8, -1.8, -1.8, -0.9)
    private val DIFFUSE_LOW_IN = listOf(-11.4, -11.5, -10.5, -8.0, -5.3, -2.8, -1.6, -0.3)
    private val DIFFUSE_HIGH = listOf(-0.1, 0.0, 1.2, 3.7, 1.2, 0.4, 3.0, 3.9, 6.4, 6.5)

    /** Results compare with the target fully up to here, then fade, because the midrange is left alone. */
    private const val FULL_UNTIL_HZ = 200.0
    private const val FADED_AT_HZ = 500.0
    private const val MORE_BASS_DB = 5.0
    private const val LESS_BASS_DB = -2.0

    private fun harman(form: HeadphoneForm) = if (form == HeadphoneForm.OVER_EAR) HARMAN_OVER else HARMAN_IN

    private fun aimed(target: HeadphoneTarget, form: HeadphoneForm): List<Double> {
        val lift = harman(form).zip(if (form == HeadphoneForm.OVER_EAR) NO_BASS_OVER else NO_BASS_IN) { with, without -> with - without }
        val largest = lift.max().takeIf { it > 0.0 } ?: 1.0
        return harman(form).mapIndexed { i, level ->
            when (target) {
                HeadphoneTarget.HARMAN -> level
                HeadphoneTarget.LESS_BASS -> level + lift[i] / largest * LESS_BASS_DB
                HeadphoneTarget.MORE_BASS -> level + lift[i] / largest * MORE_BASS_DB
                HeadphoneTarget.NO_BASS_LIFT -> level - lift[i]
                HeadphoneTarget.DIFFUSE_FIELD -> level + (if (form == HeadphoneForm.OVER_EAR) DIFFUSE_LOW_OVER else DIFFUSE_LOW_IN)[i]
            }
        }
    }

    private fun fade(hz: Double): Double = when {
        hz <= FULL_UNTIL_HZ -> 1.0
        hz >= FADED_AT_HZ -> 0.0
        else -> 1.0 - ln(hz / FULL_UNTIL_HZ) / ln(FADED_AT_HZ / FULL_UNTIL_HZ)
    }

    /** The change to add to a correction, or nothing when it isn't moving. */
    fun change(target: HeadphoneTarget?, info: CorrectionInfo?): List<ShapePoint> {
        if (target == null || info == null || info.aim.isEmpty()) return emptyList()
        val aim = info.aim.map { it.hz to it.db }
        val low = LOW.zip(aimed(target, info.form)) { hz, wanted ->
            ShapePoint(hz, (wanted - GraphicEq.interpolate(aim, hz)) * fade(hz))
        }
        val high = HIGH.mapIndexed { i, hz ->
            ShapePoint(hz, if (target == HeadphoneTarget.DIFFUSE_FIELD) DIFFUSE_HIGH[i] else 0.0)
        }
        return low + high
    }

    /** How far this correction's deep bass sits below the Harman target. Negative means above it. */
    fun bassBelowHarman(info: CorrectionInfo): Double =
        GraphicEq.interpolate(LOW.zip(harman(info.form)), DEEP_BASS_HZ) - GraphicEq.interpolate(info.aim.map { it.hz to it.db }, DEEP_BASS_HZ)

    private const val DEEP_BASS_HZ = 50.0

    /** Below this, Harman would change too little to be worth suggesting. */
    private const val WORTH_SUGGESTING_DB = 2.0

    /** Whether to point out the target choice: a correction with no target chosen, well short of Harman's bass. */
    fun worthSuggesting(tone: Tone, presetId: String): Boolean {
        if (tone.targetId != null) return false
        val info = tone.correctionInfo ?: BuiltIns.correctionInfo(presetId) ?: return false
        return bassBelowHarman(info) >= WORTH_SUGGESTING_DB
    }

    /** Reads an AutoEq result file for its target. Returns null if the file isn't in that form. */
    fun aimFromResult(text: String): List<ShapePoint>? {
        val lines = text.lineSequence().filter { it.isNotBlank() }.toList()
        val header = lines.firstOrNull()?.split(',')?.map { it.trim().lowercase() } ?: return null
        val hzColumn = header.indexOf("frequency")
        val targetColumn = header.indexOf("target")
        if (hzColumn < 0 || targetColumn < 0) return null
        val curve = lines.drop(1).mapNotNull { line ->
            val cells = line.split(',')
            val hz = cells.getOrNull(hzColumn)?.toDoubleOrNull() ?: return@mapNotNull null
            val db = cells.getOrNull(targetColumn)?.toDoubleOrNull() ?: return@mapNotNull null
            // A frequency of zero or below can't sit on a log scale and would turn the curve into NaN.
            if (db.isFinite() && hz.isFinite() && hz > 0) hz to db else null
        }
        if (curve.size < 2 || curve != curve.sortedBy { it.first }) return null
        val reference = GraphicEq.interpolate(curve, REFERENCE_HZ)
        return LOW.map { hz -> ShapePoint(hz, GraphicEq.interpolate(curve, hz) - reference) }
    }

    private const val REFERENCE_HZ = 1000.0

    fun info(form: HeadphoneForm, aim: List<ShapePoint>) = CorrectionInfo(form, aim)
}
