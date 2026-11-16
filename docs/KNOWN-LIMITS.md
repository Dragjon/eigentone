# Known limits

[← Back to the README](../README.md)

What Android doesn't let an app do without root, and how Eigentone works around it where it can.

## At a glance

| Limit | Why | What Eigentone does |
|---|---|---|
| EQ in your headphones adds to Eigentone's | It runs on the headphones themselves | Reminds you to set it flat |
| Stereo width is subtle | Android won't let an app mix one channel into the other | Leans ranges left and right with a separate EQ per channel |
| No full replacement for `Virtualizer` | Its successor needs a system permission | Keeps it as an opt-in, with stereo width as the default |
| Reverb only reaches some apps | The system-wide reverb receives no audio | Adds reverb to apps that announce their audio |
| Noise modes are chosen by hand | Android can't read the earbuds' mode | A preset per noise mode, switched in the app |
| Session 0 varies by phone | Android has deprecated it | Falls back to per-app mode |

## In detail

### EQ in your headphones

EQ in the headphones adds to Eigentone, because it runs on the headphones themselves. Eigentone reminds you to set it flat, and it can't read or change it.

> [!TIP]
> If your headphones' own app has an EQ, set it to flat or off before tuning in Eigentone, or the two will stack.

### Stereo width

**Stereo width** (`data/StereoWidth.kt`) leans frequency ranges left and right using a separate EQ per channel, cutting the side a range leans away from rather than boosting the other, so it never lowers the overall volume. It's subtle, because Android doesn't let an app mix one channel into the other, so it can't do crossfeed or real surround.

### Virtual surround

Android has no full replacement for `Virtualizer`. It's deprecated in favour of `Spatializer`, but that only describes an app's own playback, and turning it on requires a system permission.

- `Virtualizer` remains as an opt-in, forced to binaural mode, and stereo width takes over if a phone refuses it.
- It's audibly stronger, but stereo width is the default because it sounded better in listening tests on a Galaxy A54.
- It has no head tracking, and on a Galaxy A54 it reads `mode=0` while audibly working.

### Reverb

Reverb only reaches apps that announce their audio.

- On session 0 the public API only provides an auxiliary reverb, which receives no audio, so a reverb for all sound isn't possible without root.
- `PresetReverb` on an app's own announced session does give the working insert version, and that runs beside the global EQ.
- Eigentone uses `EnvironmentalReverb`, because `PresetReverb`'s fixed levels leave Large hall's echo about 38 dB below the music.

The Space section on the Effects tab names the apps it reaches.

### Noise-mode profiles

Noise-mode profiles are chosen by hand, because Android can't read the earbuds' mode.

### Session 0

Session 0 is deprecated by Android and varies by phone. Eigentone falls back to per-app mode, and the Settings tab shows what the phone accepted.

## Shelved: Studio mode

Studio mode would have used playback capture plus custom DSP, such as convolution and crossfeed. `capturelab/` holds the experiment.

<details>
<summary>Why it was shelved</summary>

On a Galaxy A54 running Android 16, it worked without root by muting the music stream and playing through the alarm stream to a preferred device, but:

- Spotify cannot be captured, and it is the main listening app.
- Volume buttons unmute the music stream.
- The small test buffer stuttered, and the estimated delay was 140-340 ms before Bluetooth.

</details>
