<p align="center">
  <img src="docs/logo-blue.svg" width="112" alt="Eigentone logo: a strand of DNA in blue on white">
</p>

<h1 align="center">Eigentone</h1>

<p align="center">
  <b>A free, open-source equaliser for Android that tunes every app to your headphones and your ears.</b><br>
  No root. No ads. No account. No tracking.
</p>

<p align="center">
  <a href="https://github.com/Dragjon/eigentone/releases"><img alt="Latest release" src="https://img.shields.io/github/v/release/Dragjon/eigentone?include_prereleases&label=release&color=1A73E8"></a>
  <a href="LICENSE"><img alt="Licence: GPL v3" src="https://img.shields.io/badge/licence-GPL--3.0-0D0C09"></a>
  <img alt="Android 11 and newer" src="https://img.shields.io/badge/Android-11%2B-3DDC84">
  <a href="https://github.com/Dragjon/eigentone/releases"><img alt="Downloads" src="https://img.shields.io/github/downloads/Dragjon/eigentone/total?color=1A73E8"></a>
</p>

<p align="center">
  <img alt="No root needed" src="https://img.shields.io/badge/root-not%20needed-EEEEEA">
  <img alt="No ads or tracking" src="https://img.shields.io/badge/ads%20%26%20tracking-none-EEEEEA">
  <img alt="AutoEq profiles" src="https://img.shields.io/badge/AutoEq-supported-1A73E8">
  <img alt="Kotlin and Jetpack Compose" src="https://img.shields.io/badge/Kotlin-Jetpack%20Compose-7F52FF?logo=kotlin&logoColor=white">
</p>

<p align="center">
  <img src="fastlane/metadata/android/en-US/images/phoneScreenshots/01-equaliser-0.42.png" width="19%" alt="Equaliser tab: the EQ curve over the live spectrum, and the graphic EQ faders with their dB scale">
  <img src="fastlane/metadata/android/en-US/images/phoneScreenshots/02-effects-0.42.png" width="19%" alt="Effects tab: preset strength, preamp and tone controls">
  <img src="fastlane/metadata/android/en-US/images/phoneScreenshots/03-presets-0.42.png" width="19%" alt="Presets tab: search, and your presets with favourites starred">
  <img src="fastlane/metadata/android/en-US/images/phoneScreenshots/04-headphones-0.42.png" width="19%" alt="Earbuds tab, named after what is connected: headphone correction and the preset that loads automatically">
  <img src="fastlane/metadata/android/en-US/images/phoneScreenshots/05-settings-0.42.png" width="19%" alt="Settings: volume, level, stereo, theme, the colour picker and Advanced controls">
</p>

*Eigentones are the natural resonant tones of an enclosed space. "Eigen" is German for "own", so the name reads as "your own tone".*

Eigentone sits behind Spotify, YouTube and most other apps, and shapes their sound system-wide using Android's `DynamicsProcessing` effect. Connect your headphones and it offers a correction measured for them. Take the short hearing check and it fits the sound to each of your ears. Everything else, from a 31-band graphic EQ to a multiband compressor, is there when you want it and out of the way when you don't. We take inspiration from KDE Plasma's motto: **simple by default, powerful when needed**.

## Why Eigentone

- **Everything is free.** There's no paid tier and nothing locked. Reverb, surround and the compressor are free like the rest.
- **It's open source.** The code is GPL v3, so anyone can check what it does with their audio, build it themselves, or improve it.
- **It fits you, not just your headphones.** AutoEq corrections fix the headphones. The hearing check, a separate EQ for each ear, and *Find my sound*, a level-matched A/B listening test, fit the sound to you.
- **It's honest about what it changes.** Comparisons and Bypass are loudness-matched, so louder never wins by accident, and it warns when a setting could clip.

## Highlights

- **Graphic EQ** with 10, 15 or 31 faders, and a **parametric EQ** with up to 20 filters, including dynamic ones.
- **Headphone correction** from AutoEq's measurements, offered when your headphones connect.
- **Fits your ears**: a hearing check with a separate EQ for each ear, and *Find my sound*, a level-matched A/B listening test.
- **350-shape library**, EQ file import and export, and share codes.
- **Presets that load by themselves** for each output, earbud noise mode and app.
- **Multiband compressor, dynamic EQ, leveller, night mode, limiter**, surround and reverb, all free.
- **Your colours and your size**: Material You on every Android version, larger text and higher contrast.

The full list is in [Features](docs/FEATURES.md), and how it stacks up against the best-known alternative is in [Eigentone and Wavelet](docs/COMPARISON.md).

## Install

Download the APK from [Releases](https://github.com/Dragjon/eigentone/releases) and open it on your phone. It needs Android 11 or newer and no root. Eigentone is a pre-release, tested on three phones so far, so reports from yours are very welcome.

[Installing and updating](docs/INSTALL.md) has the details, and what changed in each release is in the [changelog](CHANGELOG.md).

## Documentation

| | |
|---|---|
| [Features](docs/FEATURES.md) | Everything Eigentone does |
| [Eigentone and Wavelet](docs/COMPARISON.md) | A side-by-side comparison |
| [Privacy](docs/PRIVACY.md) | What each permission is for, and what's never collected |
| [Installing and updating](docs/INSTALL.md) | Install, update, and the phones it's been tested on |
| [How the sound is built](docs/HOW-IT-WORKS.md) | The audio decisions behind the app |
| [Known limits](docs/KNOWN-LIMITS.md) | What Android doesn't allow without root, and the workarounds |
| [Building Eigentone](docs/BUILDING.md) | Build from source, and where things are in the code |
| [Contributing](CONTRIBUTING.md) | How to report bugs, suggest ideas, translate, or send code |
| [Design philosophies](docs/DESIGN-PHILOSOPHIES.md) | The interaction rules the app follows, and why |
| [Changelog](CHANGELOG.md) | What changed in each release |

## Contributing

Contributions of every size are welcome, from testing on your phone and reporting bugs to translations and code. Start with [Contributing](CONTRIBUTING.md), or open an [issue](https://github.com/Dragjon/eigentone/issues).

## Privacy

No ads, no analytics, no account, and nothing about you is sent anywhere. Each permission and why it's asked for is in [Privacy](docs/PRIVACY.md).

## License

Eigentone is free software under the [GNU General Public License v3.0](LICENSE). Third-party material keeps its own terms, listed in [THIRD_PARTY_NOTICES.txt](THIRD_PARTY_NOTICES.txt).

## Credits

HUGE THANKS to the goated [Natcuber](https://github.com/Natcuber) for suggesting many of the improvements in Eigentone and testing the app extensively!!! 

Barlow and JetBrains Mono are used under the SIL Open Font License 1.1.

The Liberty 4 NC presets, headphone corrections and target curves use [AutoEq](https://github.com/jaakkopasanen/AutoEq) results and targets (MIT License). The Liberty presets only appear while those earbuds are connected.

Research shapes and targets use figures from ISO 226:2003 (equal-loudness contours), ANSI S3.5 band weights, and Sean Olive and colleagues' Harman listening research, including "The Perception and Measurement of Headphone Sound Quality" (Acoustics Today, 2022).
