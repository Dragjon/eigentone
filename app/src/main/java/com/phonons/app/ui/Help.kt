package com.phonons.app.ui

/** The explanation behind a setting's info button. */
data class Tip(val title: String, val body: String)

/** Every explanation in one place, so the wording stays consistent. */
object Help {
    val power = Tip(
        "Power and Bypass",
        "The switch turns Eigentone on or off, and it keeps running in the background until you turn it off. Bypass temporarily removes the EQ and effects so you can compare the sound. It doesn't change your settings.",
    )

    val analyser = Tip(
        "Spectrum analyser",
        "Shows the level of each frequency behind the curve, only while this screen is open. Android requires microphone permission for this, but only levels of playing audio are read. The lowest bands look coarse.",
    )
    val filterEnabled = Tip("Filter on or off", "Turns this one filter off without deleting it, so you can hear what it was doing.")
    val filterType = Tip(
        "Filter type",
        "Bell changes a range around one frequency. Shelves change everything below or above it. High pass removes what's below it, and low pass removes what's above it.",
    )
    val frequency = Tip("Frequency", "The pitch this filter works on. Lower numbers are bass, and higher numbers are treble.")
    val gain = Tip("Gain", "How much this filter boosts or cuts, in decibels. Around 3 dB is a clear but gentle change.")
    val q = Tip(
        "Q",
        "How narrow the filter is. Low values change a wide range and sound gentle. High values target a small range, which suits fixing one problem frequency.",
    )

    val bass = Tip("Bass", "Raises or lowers everything below about 105 Hz. This is saved with the preset when you save or update it.")
    val treble = Tip("Treble", "Raises or lowers everything above about 8 kHz. This is saved with the preset when you save or update it.")
    val vocals = Tip("Vocals", "Raises or lowers the range around 2.5 kHz, where voices and many instruments are easiest to pick out. This is saved with the preset.")
    val strength = Tip(
        "Preset strength",
        "More or less of everything this preset does to the tone. 100% is as saved, 0% is flat, and 150% exaggerates it. An easy way to soften a sound that's too much. Surround and preamp aren't affected.",
    )
    val guide = Tip("Getting the best sound", "A few short tips, in the order that makes the most difference.")
    val reset = Tip(
        "Reset sound settings",
        "Returns to Flat and clears night mode, balance, loudness and level settings. Your saved presets and output choices are kept.",
    )
    val finder = Tip(
        "Find my sound",
        "A short listening test. You pick your favourite of two versions a few times, and Eigentone builds a preset from your answers. Nothing is kept unless you save it.",
    )
    val bassTunerAmount = Tip(
        "Bass tuner amount",
        "Raises or lowers the low end up to the frequency you choose below. Use it when the fixed Bass control isn't the right range.",
    )
    val bassTunerFrequency = Tip(
        "Bass tuner frequency",
        "Where the bass tuner's change fades out. Low values affect only deep rumble, and higher values reach into punch and warmth.",
    )
    val eqMode = Tip(
        "Parametric or Graphic",
        "Both shape the same sound and work together. Graphic gives you fixed faders, which is the quick way. Parametric lets you choose each filter's exact frequency and width, which suits fixing a specific problem.",
    )
    val graphic = Tip(
        "Graphic EQ",
        "Fixed faders from deep bass to high treble. Drag a fader, or sweep your finger across to draw a curve. The sound follows the faders directly, and it's saved with the preset.",
    )
    val graphicBands = Tip(
        "Bands",
        "More bands allow finer changes. 10 is quickest, and 31 is the most precise but harder to set by finger. Switching keeps your curve's overall shape.",
    )
    val graphicShapes = Tip("Shapes", "Ready-made curves to start from. Tap one, then adjust it to taste. It replaces the current faders, and your preset's headphone correction stays underneath, so it adds to that.")
    val shapeLibrary = Tip(
        "Shape library",
        "Search by genre, instrument, situation or problem, or filter by what a curve does, such as More bass. Tap a shape to hear it on your music, matched in loudness so the comparison is fair, and nothing is kept until you choose Use this. The level may change slightly when you do. These are designed starting points, not measurements. Thumbnails run from 20 Hz on the left to 20 kHz on the right.",
    )
    val shapeAmount = Tip("Amount", "How strongly the shape is applied. 100% is as designed, lower is more subtle, and higher is stronger. Double-tap to return to 100%.")
    val shapeMode = Tip(
        "Replace or Add",
        "Replace swaps your faders for this shape. Add puts it on top of your current faders, so you can layer shapes, such as a genre plus a fix for something too harsh.",
    )
    val correction = Tip(
        "Headphone correction",
        "How much of this preset's own filters to apply, such as a headphone correction. 100% is as designed, and 0% removes them while keeping your faders and tone. If a sound seems muted or dull, try turning this down.",
    )
    val leveller = Tip(
        "Volume leveller",
        "Slowly evens out loud and quiet passages, which helps when apps and tracks differ in level. Higher values sound flatter. Night mode and the compressor take priority while they're on.",
    )
    val nightSchedule = Tip(
        "Night mode schedule",
        "Turns night mode on at the times you choose. It only works while Eigentone is running, and it won't lower a stronger setting you've chosen above.",
    )
    val nightScheduleAmount = Tip("Scheduled strength", "How strong night mode is during the scheduled hours.")
    val nightScheduleStart = Tip("Starts at", "The time night mode turns on. Move this in half-hour steps.")
    val nightScheduleEnd = Tip("Ends at", "The time night mode turns off. It can be after midnight.")
    val share = Tip("Share this sound", "Creates a text code holding this whole sound, which you can send to someone. It contains EQ settings only, and nothing about you.")
    val addShared = Tip("Add a shared sound", "Paste a code someone shared with you. It's added as a new preset, and your others are kept.")
    val compare = Tip(
        "Compare two sounds",
        "Switch instantly between two presets, or a preset and your current sound, at about the same loudness. Nothing changes unless you choose one.",
    )
    val compareBlind = Tip(
        "Blind comparison",
        "Hides which sound is which until you've chosen, in a random order, so a name can't sway you. Your choice is revealed at the end.",
    )
    val hearing = Tip(
        "Hearing check",
        "Plays quiet tones in each ear to build a gentle curve for what you hear least well, and can correct each ear separately. It takes about 3 minutes. It isn't a medical test.",
    )
    val perApp = Tip(
        "Per-app presets",
        "Loads a preset when a chosen app starts playing, such as one sound for Spotify and another for YouTube.",
    )
    val perAppAccess = Tip(
        "Why this access is needed",
        "Android only reveals which app is playing to apps with notification access. Eigentone ignores your notifications and only checks the playing app, and that stays on your phone. Without it, everything else still works.",
    )
    val diagnostics = Tip("Status details", "Copies technical details about your phone and the audio engine, for reporting a problem. Nothing is sent anywhere unless you paste it somewhere.")
    val crashReport = Tip("Crash report", "A technical record of the last crash, kept only on this phone. You choose whether to share it, and deleting it removes it.")
    val graphicSmooth = Tip("Smooth", "Softens sharp steps between neighbouring faders, which often sounds more natural. Tap it again for a stronger effect.")

    val surround = Tip(
        "Virtual surround",
        "Makes the sound feel wider than your head. It doesn't track head movement or create real surround channels. Higher values can make vocals feel less centred. This is saved with the preset.",
    )
    val balance = Tip("Balance", "Shifts sound towards the left or right ear. Useful if one side sounds quieter to you.")

    val loudness = Tip(
        "Adapt to volume",
        "Ears hear less bass and treble at low volume, so this adds more of both as you turn the volume down. It's a simplified curve, not a measured one.",
    )
    val loudnessStrength = Tip("Strength", "How much bass and treble are added at low volume. Lower it if quiet listening sounds too boomy.")
    val nightMode = Tip(
        "Night mode",
        "Brings quiet and loud parts closer together, so you can hear details at low volume without sudden loud moments. High values can make music sound flatter.",
    )

    val preamp = Tip(
        "Preamp",
        "Raises or lowers the overall level before the EQ. It's saved with the preset, so changing it marks the preset as edited.",
    )
    val headroom = Tip(
        "Automatic headroom",
        "Boosting can push audio past its maximum and cause distortion. This turns the level down by your largest boost, so the sound may get quieter as you add EQ.",
    )
    val limiter = Tip("Limiter", "A safety net that holds back sudden peaks so they don't distort. It rarely needs turning off.")
    val limiterThreshold = Tip(
        "Limiter threshold",
        "The level where the limiter starts working. Keep it near -1 dB. Much lower values make loud parts quieter and flatter.",
    )
    val matchBypass = Tip(
        "Level-matched Bypass",
        "Louder usually sounds better, which makes comparisons unfair. This keeps Bypass at about the same loudness, so you're comparing tone. The match is an estimate.",
    )

    val outputPreset = Tip(
        "Preset for this output",
        "Choose the preset that loads for what's connected now, such as your earbuds or the phone speaker.",
    )
    val noiseMode = Tip(
        "Earbud noise mode",
        "Earbuds can sound different in each mode, so each can have its own preset. Eigentone can't read the mode from your earbuds, so set it here to match. A mode with no preset uses this output's main one.",
    )
    val autoSwitch = Tip("Switch automatically", "Loads the assigned preset whenever this output connects.")
    val favorites = Tip(
        "Favourites",
        "Star the presets you switch between. The notification's Next preset button moves through your starred presets, or all of them if none are starred.",
    )
    val saveNew = Tip("Save as new", "Saves the current sound as a new preset of your own: filters, graphic EQ, tone, surround and preamp.")
    val update = Tip("Update preset", "Replaces your saved preset with the edits you've just made.")
    val find = Tip(
        "Find headphones",
        "Searches AutoEq's published correction profiles and adds the one you pick. It needs internet access. Measurements are a starting point, and your ears and fit still matter.",
    )
    val compressor = Tip(
        "Multiband compressor",
        "Brings loud and quiet parts closer together, separately for bass, mids and treble. Start from a style and adjust by ear. Night mode takes over while it's on, and this replaces the volume leveller.",
    )
    val compressorBand = Tip("Compress this band", "Turn this off to leave this range of frequencies untouched.")
    val compressorThreshold = Tip(
        "Threshold",
        "Anything louder than this gets turned down. Lower values compress more of the music. Phone volume doesn't change this, because it happens before the volume control.",
    )
    val compressorRatio = Tip(
        "Ratio",
        "How strongly levels above the threshold are turned down. At 4 : 1, going 4 dB over the threshold only comes out 1 dB over. Around 2 : 1 is subtle.",
    )
    val compressorMakeup = Tip("Makeup", "Turns this band back up afterwards, to make up for the level that compression took away.")
    val compressorAttack = Tip(
        "Attack",
        "How quickly it reacts once a sound crosses the threshold. Slower values let the start of drum hits through, which keeps them punchy.",
    )
    val compressorRelease = Tip(
        "Release",
        "How quickly it lets go afterwards. Very short values can make the sound flutter, and long values make it steadier.",
    )
    val compressorKnee = Tip("Knee", "How gradually compression starts around the threshold. Higher values are smoother and less noticeable.")
    val compressorCrossover = Tip("Crossover", "The frequency where one band ends and the next begins.")
    val importFile = Tip(
        "Import EQ file",
        "Adds a preset from an AutoEq or Equalizer APO file, either ParametricEQ or GraphicEQ. A GraphicEQ file keeps every point, with no rounding to faders.",
    )
    val exportFile = Tip(
        "Export EQ file",
        "Saves this preset's sound as a GraphicEQ text file, which AutoEq-compatible apps and Equalizer APO can read. Surround and settings outside the preset aren't included.",
    )
    val earCorrection = Tip(
        "Ear correction",
        "A separate EQ for each ear, from the hearing check. It closes half of the gap between your ears at each frequency, and stays on when you change preset. A check also measures the headphones, so each pair keeps its own, which turns on when they connect. Fit can affect the result, so repeat the check if it sounds off. Each pair's latest check is kept, so you can turn this on later or again after removing it.",
    )
    val backup = Tip(
        "Back up presets",
        "Saves your presets, your shapes, output and per-app choices, and favourites to a file. Ear correction, hearing checks and compressor settings are included. Other settings that aren't part of a preset, such as balance and night mode, aren't.",
    )
    val restore = Tip(
        "Restore presets",
        "Adds presets, your shapes, per-app choices, ear correction, hearing checks and compressor settings from a backup file. Anything with the same identity is replaced, and the rest is kept.",
    )

    val engineMode = Tip(
        "Engine",
        "Global covers every app Android allows, and Auto tries it first. If the EQ makes no difference, try Per app, which only covers players that announce their audio. Those players must start after Eigentone.",
    )
    val lowLatency = Tip("Low latency", "Reduces processing delay for games and video, but bass control gets less precise.")
    val startOnBoot = Tip("Start automatically", "Turns the EQ back on after a phone restart or app update, if it was on before.")
    val battery = Tip("Background use", "Battery optimisation can stop apps in the background. Allowing background use helps keep the EQ running when the phone is locked.")

    val statusRunning = Tip("Running", "Whether the audio effect is currently loaded.")
    val statusMode = Tip("Mode", "Which audio route is in use. Global applies to all apps it can reach, and Per app applies to individual players.")
    val statusBands = Tip("EQ bands", "How finely your curve is reproduced. More is more accurate, and 128 is the most Eigentone asks for.")
    val statusControl = Tip("In control", "Whether Eigentone controls the effect. If another audio app takes over, Eigentone's settings may stop applying.")
    val statusReadback = Tip("Bands accepted", "Eigentone reads its settings back from Android to confirm they were applied. All bands should match, such as 128/128.")
    val statusRepairs = Tip("Repairs", "How many times Eigentone has rebuilt the effect after losing it since it last started, such as when Android's audio system restarts. Occasional repairs are fine, but a rising number suggests a problem on this phone.")
    val statusOutput = Tip("Output", "Where Eigentone thinks audio is going. This decides which preset loads.")
    val statusApps = Tip("Attached apps", "The players Eigentone is processing in Per app mode.")
}
