package io.github.dragjon.eigentone.data

/** Finding and organising presets, and choosing which reach the app icon. Pure, so it can be unit tested. */
object PresetOrganiser {
    const val MAX_FOLDER_NAME = 40
    const val MAX_SHORTCUTS = 4
    const val MAX_QUICK_KEYS = 12

    /** Every word must match, in any order, so "bass lib" finds "Liberty 4 NC + bass". Folders count too. */
    fun matches(preset: Preset, query: String): Boolean {
        val words = query.trim().lowercase().split(Regex("\\s+")).filter { it.isNotEmpty() }
        val text = (preset.name + " " + preset.folder.orEmpty()).lowercase()
        return words.all { it in text }
    }

    /** Tidies a typed folder name. Blank means no folder. */
    fun cleanFolder(name: String?): String? = name?.trim()?.take(MAX_FOLDER_NAME)?.ifEmpty { null }

    /** Folder names in alphabetical order. A folder exists while something is in it. */
    fun folders(presets: List<Preset>): List<String> =
        presets.mapNotNull { it.folder }.distinct().sortedBy { it.lowercase() }

    /**
     * The presets in the switcher under the app name: the one in use, favourites, your own, then the rest
     * that suit what's connected, up to [MAX_QUICK_KEYS]. Always filled the same way, so which preset is in
     * use never changes which others are offered.
     */
    /** Whether another preset already has this name, ignoring case and spaces at the ends. */
    fun nameTaken(state: AppState, name: String, exceptId: String? = null): Boolean {
        val wanted = name.trim()
        return (BuiltIns.all + state.presets).any { it.id != exceptId && it.name.trim().equals(wanted, ignoreCase = true) }
    }

    /** The name, or the name with the next free number after it, such as "Flat 2". */
    fun freeName(state: AppState, name: String): String {
        val wanted = name.trim()
        if (!nameTaken(state, wanted)) return wanted
        var n = 2
        while (nameTaken(state, "$wanted $n")) n++
        return "$wanted $n"
    }

    fun quickKeys(state: AppState, available: List<Preset>): List<Preset> {
        val favourites = available.filter { it.id in state.favorites }
        val own = available.filter { it.id.startsWith("user:") && it.id !in state.favorites }
        val current = available.filter { it.id == state.current.id }
        return (current + favourites + own + available).distinctBy { it.id }.take(MAX_QUICK_KEYS)
    }

    /** Favourites first, since those are the ones chosen for quick access, then the user's own presets. */
    fun shortcuts(state: AppState, available: List<Preset>): List<Preset> {
        val favourites = available.filter { it.id in state.favorites }
        val own = available.filter { it.id.startsWith("user:") && it.id !in state.favorites }
        return (favourites + own).take(MAX_SHORTCUTS)
    }
}
