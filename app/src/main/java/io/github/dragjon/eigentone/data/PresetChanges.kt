package io.github.dragjon.eigentone.data

/**
 * What differs between a preset as saved and as it is now, for saying what Discard would drop. Pure, so
 * it can be tested. The wording is the screen's, from [Part].
 */
object PresetChanges {
    enum class Part { ADDED_FILTERS, REMOVED_FILTERS, CHANGED_FILTERS, PREAMP, FADERS, BASS, TREBLE, VOCALS, STRENGTH, CORRECTION, WIDTH, COMPRESSOR, CLARITY, REVERB, OTHER }

    /** Each kind of change, with how many filters it covers where that applies, in a steady order. */
    fun of(saved: Preset, current: Preset): List<Pair<Part, Int>> {
        val parts = mutableListOf<Pair<Part, Int>>()
        val before = saved.filters.associateBy { it.id }
        val now = current.filters.associateBy { it.id }
        val added = (now.keys - before.keys).size
        val removed = (before.keys - now.keys).size
        val changed = now.keys.intersect(before.keys).count { now[it] != before[it] }
        if (added > 0) parts += Part.ADDED_FILTERS to added
        if (removed > 0) parts += Part.REMOVED_FILTERS to removed
        if (changed > 0) parts += Part.CHANGED_FILTERS to changed
        if (saved.preampDb != current.preampDb) parts += Part.PREAMP to 0

        val a = saved.tone
        val b = current.tone.copy(trimDb = a.trimDb)
        fun differs(vararg pick: (Tone) -> Any?) = pick.any { it(a) != it(b) }
        if (differs(Tone::graphicDb, Tone::graphicBands)) parts += Part.FADERS to 0
        if (differs(Tone::bassDb, Tone::bassHz, Tone::bassTunerDb, Tone::bassTunerHz, Tone::bassCharacter)) parts += Part.BASS to 0
        if (differs(Tone::trebleDb)) parts += Part.TREBLE to 0
        if (differs(Tone::vocalsDb)) parts += Part.VOCALS to 0
        if (differs(Tone::strength)) parts += Part.STRENGTH to 0
        if (differs(Tone::correction, Tone::importedCurve, Tone::targetId, Tone::correctionInfo)) parts += Part.CORRECTION to 0
        if (differs(Tone::surround)) parts += Part.WIDTH to 0
        if (differs(Tone::compressor)) parts += Part.COMPRESSOR to 0
        if (differs(Tone::harshTamer, Tone::boomTamer)) parts += Part.CLARITY to 0
        if (differs(Tone::reverb, Tone::reverbAmount)) parts += Part.REVERB to 0

        // Anything else, such as the name a shape left on the faders, counts only if nothing above did.
        val named = parts.isNotEmpty()
        if (!named && (saved.copy(tone = a) != current.copy(tone = b))) parts += Part.OTHER to 0
        return parts
    }
}
