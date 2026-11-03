# Phonons

A system-wide parametric equaliser for Android 14 and newer, built for personal sideloading. It runs in the background using Android's `DynamicsProcessing` effect.

## Install

Copy the latest `Phonons-<version>.apk` to the phone and open it, or use USB debugging:

```powershell
& "$env:LOCALAPPDATA\Android\Sdk\platform-tools\adb.exe" install -r Phonons-0.9.2.apk
```

## Build

```powershell
$env:JAVA_HOME = "C:\Program Files\Android\Android Studio\jbr"
.\gradlew.bat :app:testDebugUnitTest :app:assembleRelease
```

The APK is written to `app\build\outputs\apk\release\app-release.apk`.

Back up `phonons-release.jks` and `keystore.properties`. They are excluded from version control, and Android refuses updates signed with a different key, so losing them means uninstalling (and losing presets) to update.

## Layout

- `dsp/`: filter maths, the curve-to-band mapping and the AutoEq parser. Pure Kotlin with unit tests.
- `engine/`: the `DynamicsProcessing` wrapper, foreground service and Quick Settings tile.
- `data/`: saved state, presets and undo.
- `ui/`: the Compose interface.

## Credits

The Liberty 4 NC presets use [AutoEq](https://github.com/jaakkopasanen/AutoEq) results (MIT License) from DHRME and Rtings measurements. They only appear in the app while those earbuds are connected.

## How the sound is built

A preset holds a whole sound: parametric filters, graphic EQ, tone, surround, strength and preamp. Balance, night mode, loudness compensation and limiter settings stay global.

The graphic EQ defines its curve directly from the faders rather than using overlapping bell filters. Bells of every width measured strayed by up to 3.3 dB between faders. This works because the audio engine takes a curve sampled on 128 bands.

The **shape library** (`dsp/ShapeLibrary.kt`, `dsp/ShapeData.kt`) has 596 graphic EQ curves: 331 hand-written across music, instruments, speech and media, situations, problems, gear and character, plus 265 generated building blocks. They can be searched and filtered by category, or by traits calculated from each curve. They are designed starting points, not measurements, and they don't reproduce any artist's or manufacturer's sound. The one exception is `My soundcore curve`, which the owner read from their own soundcore app settings.

**Find my sound** narrows bass, vocals, treble and space through level-matched A/B choices. Its ranges follow Harman's published headphone preference research. It finds a preference for this music and these headphones, not an objectively best sound.

## Known limits

- **3D surround** is Android's virtualizer forced to binaural mode. It widens stereo, with no head tracking or real surround channels. On a Galaxy A54 it reads `mode=0` while audibly working.
- **Reverb** was removed. On session 0 the public API only provides an auxiliary reverb, which receives no audio, and Android 16 blocks the method needed to control the insert version.
- **Noise-mode profiles** are chosen by hand, because Android can't read the earbuds' mode.

## Shelved: Studio mode

Playback capture plus custom DSP (convolution, crossfeed). `capturelab/` holds the experiment. On a Galaxy A54 running Android 16, it worked without root by muting the music stream and playing through the alarm stream to a preferred device, but:

- Spotify cannot be captured, and it is the main listening app.
- Volume buttons unmute the music stream.
- The small test buffer stuttered, and the estimated delay was 140-340 ms before Bluetooth.
