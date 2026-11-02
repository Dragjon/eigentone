# Phonons

A system-wide parametric equaliser for Android 14 and newer, built for personal sideloading. It runs in the background using Android's `DynamicsProcessing` effect.

## Install

Copy `Phonons-0.1.0.apk` to the phone and open it, or use USB debugging:

```powershell
& "$env:LOCALAPPDATA\Android\Sdk\platform-tools\adb.exe" install -r Phonons-0.1.0.apk
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

The Liberty 4 NC presets use [AutoEq](https://github.com/jaakkopasanen/AutoEq) results (MIT License) from DHRME and Rtings measurements.

## Shelved: Studio mode

Playback capture plus custom DSP (convolution, crossfeed). `capturelab/` holds the experiment. On a Galaxy A54 running Android 16, it worked without root by muting the music stream and playing through the alarm stream to a preferred device, but:

- Spotify cannot be captured, and it is the main listening app.
- Volume buttons unmute the music stream.
- The small test buffer stuttered, and the estimated delay was 140-340 ms before Bluetooth.
