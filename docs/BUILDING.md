# Building Eigentone

[← Back to the README](../README.md)

How to build the app from source, and where things are in the code. To send a change back, see [Contributing](../CONTRIBUTING.md).

## What you need

| Tool | Version | Notes |
|---|---|---|
| [Android Studio](https://developer.android.com/studio) | Any recent version | Brings the JDK and Android SDK. Or install them on their own. |
| JDK | 17 | Android Studio's own `jbr` works |
| Android SDK | Compile SDK 36 | Installed from Android Studio's SDK Manager |
| A phone | Android 11 or newer | With USB or wireless debugging on |

Gradle 8.13, the Android Gradle Plugin and Kotlin 2.0 are fetched by the wrapper on the first build.

> [!NOTE]
> An emulator runs the interface, but it can't show what the audio engine really does. Test sound changes on a real phone.

## Build and install a debug copy

This is the build to use while working on Eigentone. It needs no signing key of your own.

**Windows (PowerShell), using Android Studio's JDK:**

```powershell
$env:JAVA_HOME = "C:\Program Files\Android\Android Studio\jbr"
.\gradlew.bat :app:testDebugUnitTest :app:installDebug
```

**macOS or Linux:**

```sh
./gradlew :app:testDebugUnitTest :app:installDebug
```

> [!WARNING]
> A debug copy can't update a release copy from GitHub, because they're signed with different keys. Android will ask you to uninstall the release first, which loses its presets. Back them up from the app first, and restore them afterwards.

## Build a release

```powershell
$env:JAVA_HOME = "C:\Program Files\Android\Android Studio\jbr"
.\gradlew.bat :app:testDebugUnitTest :app:assembleRelease
```

The APK is written to `app\build\outputs\apk\release\app-release.apk`.

Release builds are signed only when `keystore.properties` and the key it names are present.

> [!CAUTION]
> For maintainers: back up `eigentone-release.jks` and `keystore.properties`. They are excluded from version control, and Android refuses updates signed with a different key, so losing them means everyone uninstalling (and losing presets) to update.

## Code layout

```text
app/src/main/java/io/github/dragjon/eigentone/
├── dsp/      Filter maths, the curve-to-band mapping, the AutoEq parser and the shape library
├── engine/   The DynamicsProcessing wrapper, foreground service, tile, widget,
│             hearing check tones and playing-app detection
├── data/     Saved state, presets, undo, share codes and the listening test logic
└── ui/       The Compose interface

app/src/main/res/values/   strings.xml and strings_ui.xml, the text people read
app/src/test/              Unit tests, mostly for data/ and dsp/
capturelab/                A shelved experiment, described in Known limits
fastlane/                  Store listing, screenshots and per-release changelogs
```

| Folder | Pure Kotlin? | Tested by |
|---|---|---|
| `dsp/` | Yes | Unit tests |
| `data/` | Mostly | Unit tests |
| `engine/` | No, uses Android's audio APIs | A real phone |
| `ui/` | No, Jetpack Compose | A real phone |

The Kotlin package and the application ID are both `io.github.dragjon.eigentone`. How the audio chain fits together is in [How the sound is built](HOW-IT-WORKS.md), and `capturelab/` is described in [Known limits](KNOWN-LIMITS.md#shelved-studio-mode).
