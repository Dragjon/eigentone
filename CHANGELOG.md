# Changelog

[← Back to the README](README.md)

What changed in each release since 0.33.0, newest first. Every release is published on [GitHub Releases](https://github.com/Dragjon/eigentone/releases), with its APK.

> [!NOTE]
> Every release is a pre-release until the first official one.

**Releases:** [0.43.1](#0431) · [0.43.0](#0430) · [0.42.0](#0420) · [0.41.3](#0413) · [0.41.2](#0412) · [0.41.1](#0411) · [0.41.0](#0410) · [0.40.2](#0402) · [0.40.1](#0401) · [0.40.0](#0400) · [0.39.1](#0391) · [0.39.0](#0390) · [0.38.1](#0381) · [0.38.0](#0380) · [0.37.0](#0370) · [0.36.0](#0360) · [0.35.0](#0350) · [0.34.0](#0340)

## 0.43.1

- **New tab icons.** Every tab has an outlined icon, and a filled one while it's the tab you're on, drawn as one set at the same size and weight. The Settings gear fills while Settings is open.
- **Tabs fill in as you switch.** The lit pill grows out from the middle of the new tab while the old one shrinks away, and the icon fills in with a small settle.
- **A roomier tab bar**, with more space above and below the icons.
- **Messages line up with the save bar**, at the same width and the same distance above it.
- **Fix:** the small curve in the status strip slides out in one smooth step when you return to the Equaliser, instead of a slide and then a jump.

## 0.43.0

- **Floating, frosted tabs.** The tabs sit in a rounded bar that floats above the bottom of the screen, with the page scrolling on beneath it. From Android 12 the bar is frosted, blurring the page behind it; before that it's a tint.
- **The page fades into the bars** at the top and bottom instead of stopping at a hard edge, and the tab bar is shorter.
- **EQ precision.** Under Settings, with Advanced controls on, choose Standard (128 bands) or Fine (256 bands). Fine follows narrow parametric filters about twice as closely, but changes to the whole sound, such as Bypass and switching presets, take about twice as long to apply. A phone that refuses Fine uses Standard, and Status shows the bands in use.
- The small curve in the status strip slides in and out as you move to and from the Equaliser, instead of popping.
- **Settings fades in** from the gear rather than sliding in like a tab, and fades back out.
- The device name in the status strip reveals smoothly as space opens up, with a soft fade where it's cut off instead of "…".
- Unsaved-changes bars and messages sit just above the tabs.

## 0.42.0

- **Notches on every slider**, at round steps, so a setting can be read and matched by eye. The equaliser's faders read every 5 dB, with the scale marked out to ±15.
- **Sliders move only from their handle**, so a finger landing elsewhere on the track can't change a setting by accident. A double tap still resets.
- **Faders reach their full range on any preset.** On a preset that already cuts or boosts a band, its fader used to stop short of the top or bottom.
- The faders' dB scale stays solid while they scroll, the levels and frequencies sit clear of the handles, no fader is chosen until you touch one, and tapping elsewhere lets it go.
- **Save or discard on the device tab.** The assigned preset, noise mode, switching, ear correction and quiet listening apply at once so you can hear them, then wait to be saved like a preset's edits.
- **Clearer messages.** A new one replaces the last instead of stacking, they sit above the save bars and glide with them, and every Undo offers Redo.
- The bass frequency and character rows appear and go half a second after the bass settles, so sliding through zero doesn't jolt the page.
- **A floating status strip**, rounded and over the page, which scrolls on up beneath it.
- **Pick the app's colour.** Material You colours on every Android version: your phone's own from Android 12, a swatch, or any hue from a rainbow slider that recolours the app as you slide.
- **Accessibility settings:** larger text, higher contrast, vibration on or off, and messages that stay longer.
- **Fits small screens.** On narrow phones the fader options and landscape key all fit, and labels shrink a little rather than splitting a word.
- Opening a folded section scrolls the page with it, so its contents come into view.

## 0.41.3

- **The welcome flows into the app.** Swiping on from the last step slides it away like any other page, and the app's opening starts playing as you swipe, carrying straight on into the app with no plain line, pause or jump. The arrow after the dots does the same when tapped, and lights up in a circle ringed in a paler blue.
- **Lighter swiping:** a page turns after a short swipe, as in other Android apps, and there's one quiet hint on the first page.
- **The status strip's drawer** is the strip's own colour, set apart by a faint line, so the open strip reads as one bar.
- **Fix:** the Device theme follows the phone again, after Light or Dark had been chosen before.
- The output level meter beside the spectrum uses the same track as the sliders and a solid fill.

## 0.41.2

- **The welcome feels like any other app:** pages follow your finger as you swipe. On the last step the arrow after the dots lights up; tap it, or swipe on, to start.
- **A shorter, smoother opening:** the page flattens onto a line that runs in from the edge, and the app opens out of it in a second.
- **The notification uses the new blue.** On Android 13 and older it still showed the old amber behind the icon.
- **The status strip's drawer** uses a softer tone of the strip's own colour, with text made for it, instead of a greyed shade.
- The selected fader no longer grows in landscape, where its halo is enough.
- The - and + keys in landscape are visible again.

## 0.41.1

- In dark mode, the current preset, its tick and the favourite stars in the preset menu use a brighter blue, so they stand out from the menu. Compare curves' menu does the same.

## 0.41.0

**A new look, in Material You.**

- **Colours from your wallpaper** on Android 12 and newer, in light and dark. Android 11 gets a fixed neutral palette. The ear colours and the warning orange stay the same, so they always mean the same thing.
- **Rounded and softer:** rounded keys, panels, menus and dialogs, Material sliders that fill from the left, and no more glow behind lit controls.
- **New faders:** rounded sliders with a round handle. The selected fader gets a soft halo and grows a little where there's room. Faders are selected the moment you touch them, are easier to grab, and fade out at the edges of the bank.
- **One selector for every choice:** Graphic or Parametric, Touch and View, theme, audio route, bass character and more all use the same sliding toggle.
- **A white light theme** with white menus, and darker bars at night. The status strip is in your theme's colour.
- **Swipe through the welcome.** A wave follows your finger, and swiping past the last step starts listening as the wave falls into the line the app opens from.
- **A new logo:** a blue helix on white.
- **Smoother:** switching theme no longer rebuilds the app, and switching from Parametric to Graphic moves as one.

**Fixes**

- The welcome no longer flashes the window behind it when it closes.
- Turning the spectrum analyser off always clears the bars and the level meter.
- The tour's outline stays on screen, and "Phone's virtual surround" fits on one line.
- Refine my sound suggests listening to each version for a few seconds before switching.

## 0.40.2

- **Possible fix for Android 11:** after picking headphones under Find headphones, Tune correction or Compare measurements could close the moment it opened. Each now opens once the previous dialog has gone, and a dialog ignores being closed in its first moment. If it still happens, Settings, Troubleshooting, Debug console shows what closed it.
- **Tabs switch without a hitch, even the first time.** Pages out of view stay ready just past the edge of the screen, so switching only slides them in.
- **The preset menu and the status strip's drawer open smoothly the first time,** since both are prepared shortly after the app opens.
- **Compiled ahead of time.** Eigentone now tells Android which of its code to compile in advance, which Android does while the phone is idle, so nothing stutters the first time it's used.

## 0.40.1

- **Dropdown menus are smooth.** The preset menu, Compare curves and the shape library's filter used to open in a window of their own, which made each open and close stutter. They're now drawn inside the app, so they unfold and fold back without a hitch.
- A tap outside an open menu closes it and still does what you tapped, so switching tab with the menu open takes one tap instead of two.

## 0.40.0

- **Now installs on Android 11 and 12.** Until now Eigentone needed Android 13. It hasn't been tested on an Android 11 or 12 phone yet, so please report anything that goes wrong. On these versions the system's own spatial audio always reads as off, and with two outputs connected at once Eigentone picks the most likely one rather than asking Android.
- **Tabs switch without a pause.** Every tab stays ready once it has been opened, so the slide starts the moment you tap.
- **The status strip slides over the page** instead of pushing it down, which makes opening and closing it much smoother.
- **Menus close with a fold** instead of vanishing, including the preset menu at the top left.
- Tapping a row in the status strip now scrolls to that setting, not just its tab.
- Refine my sound and Find my sound say **"I'm not sure"** instead of "I can't hear a difference", which fits hearing a difference without knowing which you prefer.
- Compare curves only offers headphone-specific presets when those headphones are connected.

## 0.39.1

- **Compare curves.** Under Presets, draw two presets on one graph and see in plain words where the first differs from the second, such as less presence or more deep bass, with how much and where.
- **Refine my sound finds your sound more closely.** It can now offer broad changes as large as 7 dB apart, which are easy to hear. On simulated listeners this cut the error left after refining by about an eighth.
- Compare two sounds now sits on the Presets tab beside Compare curves, and the Equaliser offers Refine this sound under the faders.
- Long preset menus scroll instead of running under the navigation bar.

## 0.39.0

- **Motion throughout.** Tabs slide towards where they sit, tools and Settings slide in and back out, folding sections open and close smoothly, dialogs shrink away instead of vanishing, and each step of a listening test slides to the next.
- **A better status strip.** The device name shows once and grows when the strip opens, each effect's icon moves down into its own row, every row opens its setting, and a tap anywhere else closes it.
- **Haptics** when a slider or fader passes zero or reaches its end, and for the power switch, Bypass and Save.
- **A crisper light theme** with white panels, darker text and an orange status strip, and a little more depth between panels in the dark one.
- A soft fill under the EQ curve, a gentle glow on lit controls, and a star beside favourites in the preset menu.
- After the welcome, the app fades in behind the opening line instead of squeezing open.
- **Duplicate a preset** from its edit dialog.
- Saved presets are called by their own name above the faders.
- Undo after Discard, Reset tone, deleting a filter, removing ear correction and restoring a backup now works and stays on screen.
- Long help opens in a scrolling dialog, and the tour points at the save bar and fits the Settings gear.
- **Fixes:** Compare targets no longer closes itself; Compare plays what you chose and puts the sound back when you go back; test tones stop when you leave the app; Find my sound matches loudness properly; an unreadable save file is kept rather than wiping your presets; renaming or reloading a preset keeps its edits; with two outputs connected, the one actually playing is used; EQ files with comma decimals import correctly; and many smaller layout, wording and accessibility fixes.

## 0.38.1

- More space between the version and the Bypass button, and the version still shows in full.

## 0.38.0

- **Refine my EQ.** The device tab can fine-tune your current sound by asking which of two versions you prefer. Each comparison is chosen to teach the most about your taste, a few come back with sides swapped to check your answers hold, and it stops once it's sure. A blind final A/B against your starting sound decides whether to keep the result.
- **Switching band counts keeps detail.** Going from 31 to 10 bands and back restores the 31-band curve exactly; only parts you moved while on fewer bands take the coarser shape.
- Each fader shows its level above it in the scrolling view and in landscape.
- Find peaks and the frequency sweep only appear for headphones and earbuds.
- Discarding changes says what will be lost, and can be undone.
- Pressing Bypass no longer shifts the top bar.

## 0.37.0

- **Measure a speaker with a microphone.** For a Bluetooth speaker or a car, the device tab can play a set of tones, listen with the microphone from three spots around where you sit, and save a gentle correction as a preset that loads whenever that speaker connects. It cuts peaks by up to 12 dB, lifts dips by no more than 4 dB, and leaves alone what the speaker can't really play. A USB measurement microphone is used when one is plugged in. Nothing is recorded or kept: only each tone's level is read.
- The version now shows beside the app's name at the top, so it's clear which build is running.
- The spectrum's info button sits clear of the panel's edge, centred over the dB labels.

## 0.36.0

- **New filter types:** tilt, notch and band pass.
- **Slopes for high and low pass**, from a gentle 6 to a steep 48 dB per octave. Existing filters stay at 12, so presets sound the same.
- **Left, right or both:** each parametric filter can shape one channel only, for headphones whose sides differ.
- **Dynamic filters:** a bell or shelf can also turn its range down while it gets loud, like a compressor for just that part of the sound.
- **A tab for what's playing**, named and drawn for it: Phone, Headphones, Earbuds, Speaker or Car. It animates when the device changes, and its page offers what suits that device.
- **Output level meter** beside the spectrum, which turns red when peaks reach full scale. Tap it to read about it.
- Discarding changes no longer leaves the filter editor empty.
- Filter type names no longer get cut off.

## 0.35.0

- **The graphic EQ faders show the whole sound.** Picking a preset moves them to its curve, so each fader reads the real dB at its band. Only your difference from the preset is saved, so presets sound exactly as before.
- Reset puts the faders back on the preset's curve.
- Where a preset cuts deeper than a fader reaches, the fader rests at the bottom and the readout gives the real figure.

## 0.34.0

- The code uses the Eigentone name throughout, with the package `io.github.dragjon.eigentone`.
- **After updating from 0.33.0 or earlier:** add the Quick Settings tile and home-screen widget again, grant notification access again if you use per-app presets, and re-pin preset shortcuts. Your presets and settings are kept.
- Share codes from before the name change no longer work. Share the preset again for a new code.
- "Report a problem" in Settings fits on one line.
