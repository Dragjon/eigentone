# Installing Eigentone

[← Back to the README](../README.md)

Download the APK from [Releases](https://github.com/Dragjon/eigentone/releases), and open it on the phone. Eigentone is a pre-release, and has only been tested on three phones: a Samsung Galaxy A54 running Android 16, a Vivo Y33s running Android 13, and an Oppo A9 2020 running Android 11. If something goes wrong on your phone, Settings, Troubleshooting has a debug console whose log you can attach to an [issue](https://github.com/Dragjon/eigentone/issues). Or install with USB debugging:

```powershell
& "$env:LOCALAPPDATA\Android\Sdk\platform-tools\adb.exe" install -r Eigentone-<version>.apk
```

What changed in each release is in the [changelog](../CHANGELOG.md).

## Updating

Install a newer APK over the old one, and your presets and settings are kept. Releases are signed with the same key, so Android accepts each as an update.

### From 0.33.0 or earlier

0.34.0 renames the app's internal classes, so Android forgets a few things tied to the old names. Add the Quick Settings tile and home-screen widget again, grant notification access again if you use per-app presets, and re-pin any preset shortcuts on your home screen. Share codes made before 0.34.0 no longer work, so share the preset again for a new code. Your presets and settings are kept.
