# Installing Eigentone

[← Back to the README](../README.md)

## Install

1. On your phone, open [Releases](https://github.com/Dragjon/eigentone/releases) and download the newest `Eigentone-<version>.apk`.
2. Open the downloaded file. If Android asks, allow your browser or file manager to install apps.
3. Open Eigentone and follow the short welcome.

| Requirement | Detail |
|---|---|
| Android version | 11 or newer |
| Root | Not needed |
| Account | None |

> [!NOTE]
> Eigentone is a pre-release. It may behave differently on phones it hasn't been tested on, and reports from yours are very welcome.

### Install from a computer

With USB or wireless debugging on, install from a computer with:

```powershell
& "$env:LOCALAPPDATA\Android\Sdk\platform-tools\adb.exe" install -r Eigentone-<version>.apk
```

## Tested phones

| Phone | Android |
|---|---|
| Samsung Galaxy A54 | 16 |
| Vivo Y33s | 13 |
| Oppo A9 2020 | 11 |

Tried it on another phone? Tell us how it went in an [issue](https://github.com/Dragjon/eigentone/issues), whether it worked or not.

## If something goes wrong

> [!TIP]
> Settings → Troubleshooting has a **debug console**. Copy its log into an [issue](https://github.com/Dragjon/eigentone/issues), along with your phone model and Android version, and we can usually see what happened.

## Updating

Install a newer APK over the old one, and your presets and settings are kept. Releases are signed with the same key, so Android accepts each as an update. What changed in each release is in the [changelog](../CHANGELOG.md).

> [!WARNING]
> A copy you built yourself is signed with a different key, so it can't update a release from GitHub, or the other way round. Android will ask you to uninstall first, which loses your presets. Back them up from the app before switching.

### From 0.33.0 or earlier

0.34.0 renamed the app's internal classes, so Android forgets a few things tied to the old names. Your presets and settings are kept, but:

- Add the Quick Settings tile and home-screen widget again.
- Grant notification access again if you use per-app presets.
- Re-pin any preset shortcuts on your home screen.
- Share codes made before 0.34.0 no longer work, so share the preset again for a new code.
