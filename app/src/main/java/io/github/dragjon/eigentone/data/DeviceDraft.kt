package io.github.dragjon.eigentone.data

/**
 * The settings the device tab changes that live outside presets: which preset each output loads, the
 * earbuds' noise mode, switching automatically, each ear's correction and quiet listening. They take
 * effect at once, so they can be heard, but wait to be saved or discarded as a preset's edits do.
 */
data class DeviceSettings(
    val outputProfiles: Map<String, String>,
    val listeningMode: ListeningMode,
    val autoSwitch: Boolean,
    val earCorrections: Map<String, EarCorrection>,
    val loudness: Boolean,
    val loudnessUsual: Map<String, Double>,
    val loudnessStrengths: Map<String, Double>,
) {
    /** [state] with these settings in place of its own. */
    fun applyTo(state: AppState): AppState = state.copy(
        outputProfiles = outputProfiles,
        listeningMode = listeningMode,
        autoSwitch = autoSwitch,
        earCorrections = earCorrections,
        loudness = loudness,
        loudnessUsual = loudnessUsual,
        loudnessStrengths = loudnessStrengths,
    )

    /** What differs from [other], for saying what Discard would drop. */
    fun changedParts(other: DeviceSettings): List<Part> = buildList {
        if (outputProfiles != other.outputProfiles) add(Part.PRESET)
        if (listeningMode != other.listeningMode) add(Part.MODE)
        if (autoSwitch != other.autoSwitch) add(Part.AUTO_SWITCH)
        if (earCorrections != other.earCorrections) add(Part.EARS)
        if (loudness != other.loudness || loudnessUsual != other.loudnessUsual || loudnessStrengths != other.loudnessStrengths) add(Part.QUIET)
    }

    enum class Part { PRESET, MODE, AUTO_SWITCH, EARS, QUIET }

    companion object {
        fun of(state: AppState) = DeviceSettings(
            outputProfiles = state.outputProfiles,
            listeningMode = state.listeningMode,
            autoSwitch = state.autoSwitch,
            earCorrections = state.earCorrections,
            loudness = state.loudness,
            loudnessUsual = state.loudnessUsual,
            loudnessStrengths = state.loudnessStrengths,
        )
    }
}
