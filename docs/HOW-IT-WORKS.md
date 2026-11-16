# How the sound is built

[← Back to the README](../README.md)

The decisions behind how Eigentone processes sound, for anyone curious or changing the code.

A preset holds a whole sound: parametric filters, graphic EQ, tone, surround, compressor, strength, correction amount and preamp. Balance, ear correction, night mode, the leveller, loudness compensation and limiter settings stay global.

Preset changes morph rather than jump (`data/GainRamp.kt`). Bands and the input level move together in steps of at most 4 dB, so if the old and new sounds are each safe from clipping, so is every step between them. The compressor fades along with it, in level, ratio and crossovers. Bypass, comparisons and the hearing check still switch at once.

Loudness matching for comparisons and Bypass includes an estimate of what the compressor does at a typical music level. It's close rather than exact, because real music varies.

The phone has one compressor, so night mode, manual settings and the leveller share it, in that order of priority (`data/Dynamics.kt`).

The graphic EQ defines its curve directly from the faders rather than using overlapping bell filters. Bells of every width measured strayed by up to 3.3 dB between faders. This works because the audio engine takes a curve sampled on 128 bands.

The **shape library** (`dsp/ShapeLibrary.kt`, `dsp/ShapeData.kt`) has 350 graphic EQ curves: 13 worked out from published data, 245 hand-written across problems, music, instruments, speech and media, situations, gear and character, plus 92 generated building blocks. They can be searched and filtered by category, or by traits calculated from each curve. Shapes with a source come from published data. The rest are designed starting points, not measurements, and they don't reproduce any artist's or manufacturer's sound. Shapes less than a useful difference apart were merged, and their names still find them in search. Genre shapes stay within 4 dB, because listening tests found preferences move only about 1 dB between genres, and the genre presets in Android and VLC disagree with each other.

**Find my sound** narrows bass, vocals, treble and space through level-matched A/B choices. Its ranges follow Harman's published headphone preference research. It finds a preference for this music and these headphones, not an objectively best sound.

The **hearing check** finds a threshold per frequency and ear with a "down 10, up 5" method, capped at -15 dBFS. Levels are relative to the phone's volume, so only the differences between them are used, and the curve corrects half of those.

**Ear correction** uses the same results, which are always drawn as a curve for each ear. It's recommended when the ears differ by 10 dB or more at any frequency, because the check moves in 5 dB steps, and it's available below that as a choice. It adds a separate curve to each audio channel that closes half of the gap. Both ears share one input level, because separate levels would shift the balance across every frequency. It stays on across presets, but a check also measures the headphones, so each output keeps its own correction and latest results, and it turns on when that output connects. The phone speaker never gets one, because it can't reach each ear separately.
