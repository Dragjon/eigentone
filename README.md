# Eigentone

*Eigentones are the natural resonant tones of an enclosed space. "Eigen" is German for "own", so the name reads as "your own tone".*

A free system-wide equaliser for Android 14 and newer. No ads, no account, and no tracking. It runs in the background using Android's `DynamicsProcessing` effect, so it works with Spotify, YouTube and most other apps without root.

## What it does

- **Graphic EQ** with 10, 15 or 31 faders. Drag them or draw a curve with one sweep.
- **Parametric EQ** with up to 20 filters.
- **Shape library** of 595 curves, searchable by genre, instrument, situation or problem. Hear each on your music before keeping it, and layer them.
- **Your own shapes**, typed in from any other app's EQ.
- **Headphone correction** from AutoEq's published measurements, with a strength control, and a way to compare reviewers' measurements by ear. Eigentone offers one when headphones connect.
- **Compare two sounds**, switching instantly at matched loudness, with an optional blind test.
- **Find my sound**, a guided A/B listening test that has to beat your current sound before you keep it.
- **Hearing check** that builds a gentle curve for how you hear, with a separate EQ for each ear if they differ, saved per pair of headphones and drawn as a curve for each ear. It isn't a medical test.
- **EQ files**: import ParametricEQ and GraphicEQ files, and export any sound as a 127-point GraphicEQ file. Imported GraphicEQ curves keep every point rather than being rounded to faders.
- **Find peaks for me**, a guided listening test that finds peaks and dips in your headphones and suggests narrow filters.
- **Frequency sweep**, a climbing tone that lets you mark peaks and dips by ear and turns them into narrow filters.
- **Presets** that load automatically per output, per earbud noise mode and per app.
- **Dynamic EQ**, with a harshness tamer and a boom tamer that only act while those ranges get loud.
- **Multiband compressor**, free and fully adjustable and saved per preset, with three bands, adjustable crossovers, a live response curve and starting styles.
- **Volume leveller**, night mode with a schedule, loudness compensation, limiter, balance, stereo width and virtual surround.
- **Undo and redo**, a saved-curve overlay while editing and a warning when settings could clip.
- **Preset search and folders**, app icon shortcuts to your favourites, and a tuner that adds bass or treble back to a correction.
- **Share codes**, backups, a Quick Settings tile and a home-screen widget.

## Privacy

Eigentone has no ads, analytics or account, and it sends nothing about you anywhere.

| Permission | Why | When |
|---|---|---|
| Internet | Downloads headphone profiles from AutoEq's public GitHub repository | Only when you ask for a correction |
| Microphone | Android requires it for the spectrum analyser, which reads the level of playing audio and doesn't record the microphone | Only if you turn the analyser on |
| Notification access | The only way Android reveals which app is playing, for per-app presets. Notifications are ignored | Only if you grant it |
| Notifications, background and start at boot | Keep the EQ running, with a visible notification | While Eigentone is on |

Crash reports stay on the phone. You choose whether to share one from the System tab.

## Install

Copy the latest `Eigentone-<version>.apk` to the phone and open it, or use USB debugging:

```powershell
& "$env:LOCALAPPDATA\Android\Sdk\platform-tools\adb.exe" install -r Eigentone-0.22.1.apk
```

## Build

```powershell
$env:JAVA_HOME = "C:\Program Files\Android\Android Studio\jbr"
.\gradlew.bat :app:testDebugUnitTest :app:assembleRelease
```

The APK is written to `app\build\outputs\apk\release\app-release.apk`.

Back up `phonons-release.jks` and `keystore.properties`. They are excluded from version control, and Android refuses updates signed with a different key, so losing them means uninstalling (and losing presets) to update.

## Translating

Most text is in `app/src/main/res/values/strings.xml` and `strings_ui.xml`. To add a language, copy those to `values-<code>/` and translate them. Text with values inserted, such as "Deleted Main preset", and text chosen by a condition is still in the Kotlin code, and needs moving before a translation can be complete.

## Layout

- `dsp/`: filter maths, the curve-to-band mapping, the AutoEq parser and the shape library. Pure Kotlin with unit tests.
- `engine/`: the `DynamicsProcessing` wrapper, foreground service, tile, widget, hearing check tones and playing-app detection.
- `data/`: saved state, presets, undo, share codes and the listening test logic.
- `ui/`: the Compose interface.

The app was first called Phonons. The package name (`com.phonons.app`) and class names still use it, because Android treats a new application ID as a different app. It should change once, just before the first public release.

## Credits

The Liberty 4 NC presets and headphone corrections use [AutoEq](https://github.com/jaakkopasanen/AutoEq) results (MIT License). The Liberty presets only appear while those earbuds are connected.

## How the sound is built

A preset holds a whole sound: parametric filters, graphic EQ, tone, surround, compressor, strength, correction amount and preamp. Balance, ear correction, night mode, the leveller, loudness compensation and limiter settings stay global.

Preset changes morph rather than jump (`data/GainRamp.kt`). Bands and the input level move together in steps of at most 4 dB, so if the old and new sounds are each safe from clipping, so is every step between them. The compressor fades along with it, in level, ratio and crossovers. Bypass, comparisons and the hearing check still switch at once.

Loudness matching for comparisons and Bypass includes an estimate of what the compressor does at a typical music level. It's close rather than exact, because real music varies.

The phone has one compressor, so night mode, manual settings and the leveller share it, in that order of priority (`data/Dynamics.kt`).

The graphic EQ defines its curve directly from the faders rather than using overlapping bell filters. Bells of every width measured strayed by up to 3.3 dB between faders. This works because the audio engine takes a curve sampled on 128 bands.

The **shape library** (`dsp/ShapeLibrary.kt`, `dsp/ShapeData.kt`) has 595 graphic EQ curves: 330 hand-written across music, instruments, speech and media, situations, problems, gear and character, plus 265 generated building blocks. They can be searched and filtered by category, or by traits calculated from each curve. They are designed starting points, not measurements, and they don't reproduce any artist's or manufacturer's sound.

**Find my sound** narrows bass, vocals, treble and space through level-matched A/B choices. Its ranges follow Harman's published headphone preference research. It finds a preference for this music and these headphones, not an objectively best sound.

The **hearing check** finds a threshold per frequency and ear with a "down 10, up 5" method, capped at -15 dBFS. Levels are relative to the phone's volume, so only the differences between them are used, and the curve corrects half of those.

**Ear correction** uses the same results, which are always drawn as a curve for each ear. It's recommended when the ears differ by 10 dB or more at any frequency, because the check moves in 5 dB steps, and it's available below that as a choice. It adds a separate curve to each audio channel that closes half of the gap. Both ears share one input level, because separate levels would shift the balance across every frequency. It stays on across presets, but a check also measures the headphones, so each output keeps its own correction and latest results, and it turns on when that output connects. The phone speaker never gets one, because it can't reach each ear separately.

## Known limits

- **EQ in the headphones adds to Eigentone**, because it runs on the headphones themselves. Eigentone reminds you to set it flat, and it can't read or change it.
- **Stereo width** (`data/StereoWidth.kt`) leans frequency ranges left and right using a separate EQ per channel. It's subtle, because Android doesn't let an app mix one channel into the other, so it can't do crossfeed or real surround.
- **Android has no full replacement for `Virtualizer`.** It's deprecated in favour of `Spatializer`, but that only describes an app's own playback, and turning it on requires a system permission. It remains as an opt-in, forced to binaural mode, and stereo width takes over if a phone refuses it. It's audibly stronger, but stereo width is the default because it sounded better in listening tests on a Galaxy A54. It has no head tracking, and on a Galaxy A54 it reads `mode=0` while audibly working.
- **Reverb** was removed. On session 0 the public API only provides an auxiliary reverb, which receives no audio, and Android 16 blocks the method needed to control the insert version.
- **Noise-mode profiles** are chosen by hand, because Android can't read the earbuds' mode.
- **Session 0 is deprecated** by Android and varies by phone. Eigentone falls back to per-app mode, and the System tab shows what the phone accepted.

## Shelved: Studio mode

Playback capture plus custom DSP (convolution, crossfeed). `capturelab/` holds the experiment. On a Galaxy A54 running Android 16, it worked without root by muting the music stream and playing through the alarm stream to a preferred device, but:

- Spotify cannot be captured, and it is the main listening app.
- Volume buttons unmute the music stream.
- The small test buffer stuttered, and the estimated delay was 140-340 ms before Bluetooth.
