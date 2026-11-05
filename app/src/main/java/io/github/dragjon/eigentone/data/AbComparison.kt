package io.github.dragjon.eigentone.data

/**
 * Two sounds to switch between. The second is matched to the first's loudness, because the louder one
 * tends to win whatever its tone. In a blind comparison the buttons don't say which is which, and
 * [flipped] decides which button plays which sound. Pure, so it can be unit tested.
 */
data class AbComparison(
    val a: Preset,
    val b: Preset,
    val blind: Boolean = false,
    val flipped: Boolean = false,
    /** The working copy, when it's being compared with the saved preset it came from. */
    val edited: Preset? = null,
) {
    private val swapped get() = blind && flipped

    /** The saved sound behind a button, without any temporary level change. */
    fun presetFor(button: Int): Preset = if ((button == 0) != swapped) a else b

    /** What to play for a button. */
    fun soundFor(button: Int, autoHeadroom: Boolean): Preset {
        val preset = presetFor(button)
        return if (preset === a) preset else SoundFinder.levelMatched(preset, preset.tone, a, autoHeadroom)
    }

    /** A button's label. Blind labels say nothing about the sound behind them. */
    fun label(button: Int): String = if (blind) "${button + 1}" else if (button == 0) "A" else "B"

    /** A button's preset name, which stays hidden during a blind comparison. */
    fun nameFor(button: Int): String? = if (blind) null else nameOf(presetFor(button))

    /** Names a sound. An edited preset and its saved version share a name, so they need telling apart. */
    fun nameOf(preset: Preset): String = when {
        a.id != b.id -> preset.name
        preset == edited -> "${preset.name} (edited)"
        else -> "${preset.name} (saved)"
    }
}
