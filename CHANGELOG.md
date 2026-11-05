# Changelog

Changes since 0.33.0. Every release is a pre-release on [GitHub](https://github.com/Dragjon/eigentone/releases) until the first official one.

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
