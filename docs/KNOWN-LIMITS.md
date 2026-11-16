# Known limits

[← Back to the README](../README.md)

What Android doesn't let an app do without root, and how Eigentone works around it where it can.

- **EQ in the headphones adds to Eigentone**, because it runs on the headphones themselves. Eigentone reminds you to set it flat, and it can't read or change it.
- **Stereo width** (`data/StereoWidth.kt`) leans frequency ranges left and right using a separate EQ per channel, cutting the side a range leans away from rather than boosting the other, so it never lowers the overall volume. It's subtle, because Android doesn't let an app mix one channel into the other, so it can't do crossfeed or real surround.
- **Android has no full replacement for `Virtualizer`.** It's deprecated in favour of `Spatializer`, but that only describes an app's own playback, and turning it on requires a system permission. It remains as an opt-in, forced to binaural mode, and stereo width takes over if a phone refuses it. It's audibly stronger, but stereo width is the default because it sounded better in listening tests on a Galaxy A54. It has no head tracking, and on a Galaxy A54 it reads `mode=0` while audibly working.
- **Reverb only reaches apps that announce their audio.** On session 0 the public API only provides an auxiliary reverb, which receives no audio, so a reverb for all sound isn't possible without root. `PresetReverb` on an app's own announced session does give the working insert version, and that runs beside the global EQ. It uses `EnvironmentalReverb`, because `PresetReverb`'s fixed levels leave Large hall's echo about 38 dB below the music. The Space section on the Effects tab names the apps it reaches.
- **Noise-mode profiles** are chosen by hand, because Android can't read the earbuds' mode.
- **Session 0 is deprecated** by Android and varies by phone. Eigentone falls back to per-app mode, and the Settings tab shows what the phone accepted.

## Shelved: Studio mode

Playback capture plus custom DSP (convolution, crossfeed). `capturelab/` holds the experiment. On a Galaxy A54 running Android 16, it worked without root by muting the music stream and playing through the alarm stream to a preferred device, but:

- Spotify cannot be captured, and it is the main listening app.
- Volume buttons unmute the music stream.
- The small test buffer stuttered, and the estimated delay was 140-340 ms before Bluetooth.
