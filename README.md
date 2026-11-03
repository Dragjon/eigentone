# Phonons

A free system-wide equaliser for Android 14 and newer. No ads, no account, and no tracking. It runs in the background using Android's `DynamicsProcessing` effect, so it works with Spotify, YouTube and most other apps without root.

## What it does

- **Graphic EQ** with 10, 15 or 31 faders. Drag them or draw a curve with one sweep.
- **Parametric EQ** with up to 20 filters.
- **Shape library** of 595 curves, searchable by genre, instrument, situation or problem. Hear each on your music before keeping it, and layer them.
- **Your own shapes**, typed in from any other app's EQ.
- **Headphone correction** from AutoEq's published measurements, with a strength control. Phonons offers one when headphones connect.
- **Find my sound**, a guided A/B listening test that has to beat your current sound before you keep it.
- **Hearing check** that builds a gentle curve for how you hear, with a separate EQ for each ear if they differ. It isn't a medical test.
- **EQ files**: import ParametricEQ and GraphicEQ files, and export any sound as a 127-point GraphicEQ file. Imported GraphicEQ curves keep every point rather than being rounded to faders.
- **Presets** that load automatically per output, per earbud noise mode and per app.
- **Volume leveller**, night mode with a schedule, loudness compensation, limiter, balance and virtual surround.
- **Share codes**, backups, a Quick Settings tile and a home-screen widget.

## Privacy

Phonons has no ads, analytics or account, and it sends nothing about you anywhere.

| Permission | Why | When |
|---|---|---|
| Internet | Downloads headphone profiles from AutoEq's public GitHub repository | Only when you ask for a correction |
| Microphone | Android requires it for the spectrum analyser, which reads the level of playing audio and doesn't record the microphone | Only if you turn the analyser on |
| Notification access | The only way Android reveals which app is playing, for per-app presets. Notifications are ignored | Only if you grant it |
| Notifications, background and start at boot | Keep the EQ running, with a visible notification | While Phonons is on |

Crash reports stay on the phone. You choose whether to share one from the System tab.

## Install

Copy the latest `Phonons-<version>.apk` to the phone and open it, or use USB debugging:

```powershell
& "$env:LOCALAPPDATA\Android\Sdk\platform-tools\adb.exe" install -r Phonons-0.11.0.apk
```

## Build

```powershell
$env:JAVA_HOME = "C:\Program Files\Android\Android Studio\jbr"
.\gradlew.bat :app:testDebugUnitTest :app:assembleRelease
```

The APK is written to `app\build\outputs\apk\release\app-release.apk`.

Back up `phonons-release.jks` and `keystore.properties`. They are excluded from version control, and Android refuses updates signed with a different key, so losing them means uninstalling (and losing presets) to update.

## Layout

- `dsp/`: filter maths, the curve-to-band mapping, the AutoEq parser and the shape library. Pure Kotlin with unit tests.
- `engine/`: the `DynamicsProcessing` wrapper, foreground service, tile, widget, hearing check tones and playing-app detection.
- `data/`: saved state, presets, undo, share codes and the listening test logic.
- `ui/`: the Compose interface.

## Credits

The Liberty 4 NC presets and headphone corrections use [AutoEq](https://github.com/jaakkopasanen/AutoEq) results (MIT License). The Liberty presets only appear while those earbuds are connected.

## How the sound is built

A preset holds a whole sound: parametric filters, graphic EQ, tone, surround, strength, correction amount and preamp. Balance, night mode, the leveller, loudness compensation and limiter settings stay global.

The graphic EQ defines its curve directly from the faders rather than using overlapping bell filters. Bells of every width measured strayed by up to 3.3 dB between faders. This works because the audio engine takes a curve sampled on 128 bands.

The **shape library** (`dsp/ShapeLibrary.kt`, `dsp/ShapeData.kt`) has 595 graphic EQ curves: 330 hand-written across music, instruments, speech and media, situations, problems, gear and character, plus 265 generated building blocks. They can be searched and filtered by category, or by traits calculated from each curve. They are designed starting points, not measurements, and they don't reproduce any artist's or manufacturer's sound.

**Find my sound** narrows bass, vocals, treble and space through level-matched A/B choices. Its ranges follow Harman's published headphone preference research. It finds a preference for this music and these headphones, not an objectively best sound.

The **hearing check** finds a threshold per frequency and ear with a "down 10, up 5" method, capped at -15 dBFS. Levels are relative to the phone's volume, so only the differences between them are used, and the curve corrects half of those.

**Ear correction** uses the same results. If the ears differ by 10 dB or more at any frequency, it can add a separate curve to each audio channel that closes half of the gap. Both ears share one input level, because separate levels would shift the balance across every frequency. It belongs to the listener rather than the sound, so it stays on across presets.

## Known limits

- **EQ in the headphones adds to Phonons**, because it runs on the headphones themselves. Phonons reminds you to set it flat, and it can't read or change it.
- **3D surround** is Android's virtualizer forced to binaural mode. It widens stereo, with no head tracking or real surround channels. On a Galaxy A54 it reads `mode=0` while audibly working.
- **Reverb** was removed. On session 0 the public API only provides an auxiliary reverb, which receives no audio, and Android 16 blocks the method needed to control the insert version.
- **Noise-mode profiles** are chosen by hand, because Android can't read the earbuds' mode.
- **Session 0 is deprecated** by Android and varies by phone. Phonons falls back to per-app mode, and the System tab shows what the phone accepted.

## Shelved: Studio mode

Playback capture plus custom DSP (convolution, crossfeed). `capturelab/` holds the experiment. On a Galaxy A54 running Android 16, it worked without root by muting the music stream and playing through the alarm stream to a preferred device, but:

- Spotify cannot be captured, and it is the main listening app.
- Volume buttons unmute the music stream.
- The small test buffer stuttered, and the estimated delay was 140-340 ms before Bluetooth.
