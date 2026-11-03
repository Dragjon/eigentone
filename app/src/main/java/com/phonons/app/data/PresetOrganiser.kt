package com.phonons.app.data

/** Finding and organising presets, and choosing which reach the app icon. Pure, so it can be unit tested. */
object PresetOrganiser {
    const val MAX_FOLDER_NAME = 40
    const val MAX_SHORTCUTS = 4

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

    /** Favourites first, since those are the ones chosen for quick access, then the user's own presets. */
    fun shortcuts(state: AppState, available: List<Preset>): List<Preset> {
        val favourites = available.filter { it.id in state.favorites }
        val own = available.filter { it.id.startsWith("user:") && it.id !in state.favorites }
        return (favourites + own).take(MAX_SHORTCUTS)
    }
}
