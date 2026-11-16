# Building Eigentone

[← Back to the README](../README.md)

How to build the app from source, and where things are in the code. To send a change back, see [Contributing](../CONTRIBUTING.md).

## What you need

- [Android Studio](https://developer.android.com/studio), which brings the JDK 17 the build uses and the Android SDK (compile SDK 36). Or a JDK 17 and the SDK command-line tools on their own.
- A phone running Android 11 or newer with USB or wireless debugging on. An emulator runs the interface, but it can't show what the audio engine really does.

Gradle, the Android Gradle Plugin and Kotlin are fetched by the wrapper on the first build.

## Build and install a debug copy

This is the build to use while working on Eigentone. It needs no signing key of your own.

```powershell
# Windows (PowerShell), using Android Studio's JDK
$env:JAVA_HOME = "C:\Program Files\Android\Android Studio\jbr"
.\gradlew.bat :app:testDebugUnitTest :app:installDebug
```

```sh
# macOS or Linux
./gradlew :app:testDebugUnitTest :app:installDebug
```

A debug copy can't update a release copy from GitHub, because they're signed with different keys. Android will ask you to uninstall the release first, which loses its presets. A backup from Settings can be restored afterwards.

## Build a release

```powershell
$env:JAVA_HOME = "C:\Program Files\Android\Android Studio\jbr"
.\gradlew.bat :app:testDebugUnitTest :app:assembleRelease
```

The APK is written to `app\build\outputs\apk\release\app-release.apk`.

Release builds are signed only when `keystore.properties` and the key it names are present. Back up `eigentone-release.jks` and `keystore.properties`. They are excluded from version control, and Android refuses updates signed with a different key, so losing them means uninstalling (and losing presets) to update.

## Code layout

Everything lives under `app/src/main/java/io/github/dragjon/eigentone/`:

- `dsp/`: filter maths, the curve-to-band mapping, the AutoEq parser and the shape library. Pure Kotlin with unit tests.
- `engine/`: the `DynamicsProcessing` wrapper, foreground service, tile, widget, hearing check tones and playing-app detection.
- `data/`: saved state, presets, undo, share codes and the listening test logic.
- `ui/`: the Compose interface.

Unit tests are in `app/src/test/`, mostly for `data/` and `dsp/`. `capturelab/` is a shelved experiment, described in [Known limits](KNOWN-LIMITS.md).

The Kotlin package and the application ID are both `io.github.dragjon.eigentone`.
