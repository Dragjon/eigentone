---
name: Eigentone
description: A precision equaliser instrument, tuned to one person's ears.
colors:
  graphite-page: "#141516"
  graphite-bar: "#0B0C0D"
  enamel-panel: "#1E1F20"
  raised-key: "#2B2D2E"
  overlay: "#333536"
  hairline: "#323435"
  control-outline: "#8A8C8E"
  scale-track: "#6C6F71"
  text: "#EEEEEA"
  text-dim: "#B0B2B2"
  lit-ivory: "#F2E8CF"
  lit-pressed: "#D6CCB4"
  on-lit: "#1E1A10"
  green-strip: "#174035"
  green-dim: "#BFD3C9"
  danger-orange: "#FF9A5C"
  left-ear: "#62B2F2"
  right-ear: "#F06B5C"
typography:
  display:
    fontFamily: "Barlow Semi Condensed"
    fontSize: "38sp"
    fontWeight: 500
    lineHeight: "44sp"
  headline-large:
    fontFamily: "Barlow Semi Condensed"
    fontSize: "30sp"
    fontWeight: 600
    lineHeight: "36sp"
  headline:
    fontFamily: "Barlow Semi Condensed"
    fontSize: "26sp"
    fontWeight: 600
    lineHeight: "32sp"
  title-large:
    fontFamily: "Barlow Semi Condensed"
    fontSize: "22sp"
    fontWeight: 600
    lineHeight: "28sp"
  title:
    fontFamily: "Barlow"
    fontSize: "16sp"
    fontWeight: 600
    lineHeight: "24sp"
  title-small:
    fontFamily: "Barlow"
    fontSize: "14sp"
    fontWeight: 600
    lineHeight: "20sp"
  body-large:
    fontFamily: "Barlow"
    fontSize: "16sp"
    fontWeight: 400
    lineHeight: "22sp"
  body:
    fontFamily: "Barlow"
    fontSize: "14sp"
    fontWeight: 400
    lineHeight: "20sp"
  body-small:
    fontFamily: "Barlow"
    fontSize: "12sp"
    fontWeight: 400
    lineHeight: "16sp"
  label:
    fontFamily: "Barlow"
    fontSize: "14sp"
    fontWeight: 600
    lineHeight: "20sp"
  label-nav:
    fontFamily: "Barlow Semi Condensed"
    fontSize: "12sp"
    fontWeight: 500
    lineHeight: "16sp"
  panel-legend:
    fontFamily: "Barlow Semi Condensed"
    fontSize: "13sp"
    fontWeight: 600
    lineHeight: "18sp"
    letterSpacing: "1.2sp"
  strip-tag:
    fontFamily: "Barlow"
    fontSize: "11sp"
    fontWeight: 400
    lineHeight: "14sp"
  readout:
    fontFamily: "JetBrains Mono"
    fontSize: "13sp"
    fontWeight: 400
    lineHeight: "18sp"
  scale-label:
    fontFamily: "JetBrains Mono"
    fontSize: "10sp"
    fontWeight: 400
rounded:
  cap: "2dp"
  key: "4dp"
  panel: "4dp"
  chip: "6dp"
  tooltip: "8dp"
  dialog: "12dp"
spacing:
  xs: "4dp"
  sm: "8dp"
  md: "12dp"
  lg: "16dp"
components:
  key-lit:
    backgroundColor: "{colors.lit-ivory}"
    textColor: "{colors.on-lit}"
    typography: "{typography.label}"
    rounded: "{rounded.key}"
  key-lit-pressed:
    backgroundColor: "{colors.lit-pressed}"
    textColor: "{colors.on-lit}"
  key-lit-disabled:
    backgroundColor: "{colors.raised-key}"
    textColor: "{colors.text-dim}"
  key-unlit:
    backgroundColor: "{colors.raised-key}"
    textColor: "{colors.text}"
    typography: "{typography.label}"
    rounded: "{rounded.key}"
  key-unlit-pressed:
    backgroundColor: "{colors.enamel-panel}"
    textColor: "{colors.text}"
  segment-active:
    backgroundColor: "{colors.lit-ivory}"
    textColor: "{colors.on-lit}"
    rounded: "{rounded.key}"
  segment-inactive:
    backgroundColor: "{colors.raised-key}"
    textColor: "{colors.text}"
    rounded: "{rounded.key}"
  chip-selected:
    backgroundColor: "{colors.lit-ivory}"
    textColor: "{colors.on-lit}"
    rounded: "{rounded.chip}"
  settings-panel:
    backgroundColor: "{colors.enamel-panel}"
    textColor: "{colors.text}"
    rounded: "{rounded.panel}"
  setting-row:
    textColor: "{colors.text}"
    typography: "{typography.body-large}"
    padding: "4dp 16dp"
    height: "56dp"
  status-strip:
    backgroundColor: "{colors.green-strip}"
    textColor: "{colors.text}"
    typography: "{typography.body-small}"
    padding: "4dp 16dp"
    height: "38dp"
  bar:
    backgroundColor: "{colors.graphite-bar}"
    textColor: "{colors.text}"
  nav-indicator:
    backgroundColor: "{colors.lit-ivory}"
    textColor: "{colors.on-lit}"
  preset-menu:
    backgroundColor: "{colors.overlay}"
    textColor: "{colors.text}"
  value-track-cap:
    backgroundColor: "{colors.lit-ivory}"
    rounded: "{rounded.cap}"
    width: "12dp"
    height: "24dp"
  fader-cap:
    backgroundColor: "{colors.lit-ivory}"
    rounded: "{rounded.cap}"
    width: "22dp"
    height: "12dp"
  fader-cap-selected:
    backgroundColor: "{colors.text}"
    rounded: "{rounded.cap}"
    height: "18dp"
---

# Design System: Eigentone

## Overview

**Creative North Star: "The Audiometer"**

**Colour update (user decision):** the lit colour is now Amber (#FFC24A dark, burnt amber #8A4F00 light, with On Lit #2A1C00 and cream #FFF4DC), and the graphite carries a faint warm tint. Where this file says ivory, read amber. The ear colours, Danger Orange and the green strip are unchanged.

Eigentone is a precision instrument the listener owns: a graphite enamel faceplate, ivory that lights up, engraved condensed legends, a fine graph grid and readouts that hold still. Screens are dense because their users know Q and dB, and because a visit is short and happens with music playing.

Colour always carries meaning. Blue and red belong to the left and right ear, as on an audiogram, and every per-ear graph marks the left with X and the right with O. The system refuses the category's glossy dark app with coloured pill sliders and rounded cards.

Six decisions came directly from the user and override the original direction contract. They are binding: no horizontal scrolling; preset switching by dropdown under the app name; a deep green status strip; effects in the strip with a drawn tick or cross; bars darker than the page with no scroll fades; ivory filter points on graphs.

**Known limits:** No authored motion beyond the playing pulse; anything else that moves is stock Material behaviour. Left and right are only partly mirrored, in graph legends and curves. Tool and test screens (hearing check, sweep, peak finder, Find my sound, A/B compare, shape browser) inherit these tokens but were not redesigned. No tablet layout: content is simply capped at 640dp and centred.

**Key Characteristics:**
- Graphite surfaces separated by tone, with no shadows on panels or keys
- Ivory for what is lit, active or grippable; ear colours only for the ears
- Barlow Semi Condensed legends, Barlow body, JetBrains Mono digits
- Rectangular fader caps with a centre line, on thin rails with engraved scales
- Everything fits the width: wrap or drop down, never scroll sideways

## Colors

Near-neutral graphite with one warm ivory, two ear colours, one warning orange and one green band.

### Primary
- **Lit Ivory** (#F2E8CF, `Palette.Lit`): lit keys, active segments, selected chips, the nav indicator, fader and track caps, filled rail sections, filter points, the single-curve graph line, and readouts that differ from their default.
- **Pressed Ivory** (#D6CCB4, `LitPressed` in `Keys.kt`): a lit key while held.
- **On Lit** (#1E1A10, `Palette.OnLit`): text, icons and cap centre lines on ivory (14.2:1).

### Secondary
- **Left Ear Blue** (#62B2F2, `Palette.LeftEar`): left-ear curve, X markers and legend. Solid line.
- **Right Ear Red** (#F06B5C, `Palette.RightEar`): right-ear curve, O markers and legend. Always dashed, so colour is not the only difference.

### Tertiary
- **Deep Green** (#174035, `Palette.Strip`): the status strip, and nothing else. The user chose it after rejecting purple, which clashed with the graph, and brass, which looked muddy.
- **Green Dim** (#BFD3C9, `Palette.StripDim`): things that are off, on the strip only (7.3:1).
- **Danger Orange** (#FF9A5C, `Palette.Danger`): errors, clip risk and destructive actions. Orange so that a problem cannot be mistaken for the right ear.

### Neutral
- **Bar Graphite** (#0B0C0D, `Palette.Bar`): top bar, bottom bar and the strip's mini-curve well.
- **Page Graphite** (#141516, `Palette.Background`): the page, also the window background.
- **Enamel** (#1E1F20, `Palette.Surface`): panels that hold settings and graphs.
- **Raised Key** (#2B2D2E, `Palette.SurfaceHigh`): unlit keys, inactive segments and prompt panels.
- **Overlay** (#333536, `Palette.Tooltip`): tooltips and the preset menu.
- **Hairline** (#323435, `Palette.Line`): row dividers and minor grid lines. Decorative.
- **Control Outline** (#8A8C8E, `Palette.Outline`): off switches, unselected comparison cards, graph zero lines, the saved-curve ghost.
- **Scale Track** (#6C6F71, `Palette.Track`): rails, ticks and decade grid lines.
- **Text** (#EEEEEA, `Palette.Text`) and **Dim Text** (#B0B2B2, `Palette.TextDim`): primary and secondary text. Dim Text also draws inactive graphs and is at least 6.5:1 on the bars, page, panels and keys.

### Named Rules
**The Ear Colour Rule.** Blue and red mean left and right ear, and nothing else. Any other second state uses ivory, dim text or a shape.

**The Ivory Rule.** Ivory means lit, active, or a handle you can grip. Plain text stays Text, including text buttons and the app name, so that ivory keeps its meaning. Filter points are ivory because they apply to both ears (user decision).

**The Green Strip Rule.** Green is the one exception to "the ears are the only chroma" besides Danger Orange, and it belongs to the status strip alone (user decision). Do not reuse it for accents, and keep the band's hue clear of the ear colours.

### Light Theme: The Audiometer in Daylight

Selected in **System → Appearance**: Device (default), Light or Dark. `DarkColors` and `LightColors` in `Theme.kt` hold every role, and `Palette` reads whichever is active. The dark theme's values are locked by a test.

Light is composed, not inverted: a warm putty faceplate, as on lab equipment. A lit control can't glow in daylight, so it becomes a dark inked key carrying the same ivory as its legend, which keeps ivory meaning "active".

| Role | Token | Light |
|---|---|---|
| Page | `Background` | #E9E6DC |
| Bars, still darker than the page | `Bar` | #D9D5C8 |
| Panels | `Surface` | #F5F3EC |
| Unlit keys | `SurfaceHigh` | #DFDBCF |
| Menus and dialogs | `Tooltip` | #FFFEFA |
| Hairlines | `Line` | #D3CFC3 |
| Control outline | `Outline` | #77746A |
| Rails | `Track` | #838075 |
| Text and dim text | `Text`, `TextDim` | #1B1A17, #55534B |
| Inked key, and its pressed state | `Lit`, `LitPressed` | #2C2718, #4A4331 |
| Legend on an inked key | `OnLit` | #F2E8CF |
| Warning | `Danger` | #8F3F00 |
| Left and right ear | `LeftEar`, `RightEar` | #0A5FAD, #C4231A |

**The Fixed Green Rule.** The status strip stays Deep Green in both themes, so everything on it uses `StripText`, `StripOn`, `StripDim` and `StripDanger`, never the page's colours (user decision).

**Not themed:** the home-screen widget and launcher icon stay dark.

## Typography

**Display Font:** Barlow Semi Condensed (bundled)
**Body Font:** Barlow (bundled)
**Label/Mono Font:** JetBrains Mono (bundled), for numbers that change

**Character:** Condensed legends read as engraving on a faceplate, plain Barlow keeps long explanations easy to read, and the mono digits behave like a meter.

### Hierarchy
- **Display** (500, 38sp, 44sp): the large frequency on the sweep and peak finder.
- **Headline Large** (600, 30sp, 36sp): the labels on comparison cards.
- **Headline** (600, 26sp, 32sp): tool and test screen headings.
- **Title Large** (600, 22sp, 28sp): the app name and full-screen tool titles.
- **Title** and **Title Small** (Barlow 600, 16sp/24sp and 14sp/20sp): prompt titles, tooltip titles, the current shape name.
- **Body Large** (400, 16sp, 22sp): setting names.
- **Body** (400, 14sp, 20sp): explanations and dialog text.
- **Body Small** (400, 12sp, 16sp): setting details, the status line, the output name in the strip.
- **Label** (Barlow 600, 14sp, 20sp): key, segment and chip text.
- **Label Nav** (Semi Condensed 500, 12sp, 16sp): bottom navigation labels.
- **Panel Legend** (Semi Condensed 600, 13sp, 18sp, +1.2sp, uppercase, Dim Text): the heading of a group of settings.
- **Strip Tag** (400, 11sp, 14sp): effect names in the strip.
- **Readout** (JetBrains Mono, 13sp, 18sp): values such as "+1.5 dB" or "30%".
- **Scale Label** (JetBrains Mono, 10sp): axis labels and ear legends drawn on canvases.

### Named Rules
**The Still Digits Rule.** Any number that changes while you watch it is set in JetBrains Mono, so that it does not shift as its value changes.

**The Engraved Legend Rule.** Uppercase tracked type is only the heading of a group of rows. It never sits above another heading as a kicker.

## Layout

One vertical column, capped at 640dp and centred, with 16dp side padding, 12dp above and 16dp below. Sections are 12dp apart (`SectionSpacing`). Spacing steps are 4, 8, 12 and 16dp, with 16dp dominant. Each tab keeps its own scroll position.

The frame is fixed: top bar (name, preset dropdown, Bypass while the engine is on, info, power), status strip, scrolling page, five-tab bottom bar (EQ, Sound, Presets, Tune, System). Minimum heights: setting rows 56dp, value tracks and the preset switcher 48dp, section headers 40dp.

**The No Sideways Rule.** Nothing scrolls horizontally, except the precision fader bank, which the user asked for, under an overview of the whole curve. Short sets wrap with `FlowRow` (8dp gaps), and long lists use dropdowns (user decision).

## Elevation & Depth

Depth is tonal: Bar, Page, Enamel, Raised Key, Overlay, from lowest to highest. Panels and keys carry no shadows, and pressing a key moves it down one tone. Stock Material menus, tooltips and dialogs keep their default overlay shadows; these are inherited rather than designed.

**The Tonal Ladder Rule.** Surface height is shown by tone. A pressed unlit key becomes Enamel, and a pressed lit key becomes Pressed Ivory.

**The Fixed Bars Rule.** Top and bottom bars are darker than the page, with no scroll fades or gradients (user decision). The step is deliberately quiet (1.07:1), so the strip (1.67:1 against the page) marks the top edge.

## Shapes

Small, square-shouldered corners: 2dp on fader and track caps, 4dp on keys, segments, panels and text fields, 6dp on chips, 8dp on tooltips, 12dp on dialogs. Graph points are circles. Rails, ticks and grid lines are square-ended rectangles.

## Components

Built to feel like hardware controls: rectangular, labelled and precise.

### Buttons
- **Lit key** (`Button`): the screen's primary action, or a held state such as Bypassed.
- **Unlit key** (`FilledTonalButton`, `FilledTonalIconButton`): everything else. Raised Key fill, with a 1dp Hairline border on text keys.
- **Text button:** Text colour, never ivory. Destructive text is Danger Orange.

### Chips and Segments
- **Segments:** a joined row with 4dp outer corners and no icon. Active is ivory; inactive is Raised Key.
- **Chips:** used for short shape sets that wrap. Selected is ivory.

### Cards / Containers
- **Settings panel** (`SettingsSection`): Enamel, 4dp, no border, with an optional Panel Legend above and 16dp-inset Hairline dividers between rows.
- **Prompt panel:** Raised Key, 16dp padding, 8dp internal gaps.
- **Comparison card** (`SideCard`): 140dp tall with a 1dp border. Selected fills with ivory.

### Inputs / Fields
- **Text fields:** stock outlined fields on the theme's colours, with 4dp corners. Errors use Danger Orange.
- **Switches:** stock Material, ivory when on and outlined when off. The whole setting row is the touch target.

### Navigation
- **Bottom bar:** Bar background, with no tonal elevation. Selected tab has On Lit icon on an ivory indicator and a Text label; others are Dim Text.
- **Preset switcher:** the app name and status line form one 48dp target with a drop arrow. The menu is Overlay, lists quick presets with a tick and ivory on the current one, then "All presets" below a Hairline. The status line turns Danger Orange on a problem (user decision, replaces the contract's row of keys).

### Status Strip
A full-width green band, at least 38dp tall, with 6dp gaps: output icon, output name (truncates), clip risk if present, effects, playing dot, and a 52 × 26dp mini curve on every tab except EQ.

- **Expanding:** tapping the strip opens a drawer in a softer green (#1F4B3F, `Palette.StripPanel`) listing every effect by its full name with a tick or cross and On or Off, and tapping again closes it.
- **Effects:** dynamics (Comp, Night or Level), space (Width or Surround), Ears and Reverb always appear as a 16dp icon followed by a 10dp tick when on or cross when off, ivory when on and Green Dim when off (user decision). Tame appears only while active. Each is announced by name with its state.
- **Playing dot:** 8dp ivory, present only while Android reports music. Its alpha pulses 0.35 to 1 over 900ms, linear, reversing. This is the only authored motion.

**The Drawn State Rule.** On and off are shown by a drawn mark as well as a colour, and each tag is announced with its state.

**The True Reading Rule.** Nothing is drawn as a live measurement unless it is one. The compressor shows as on rather than with a meter, because Android does not report gain reduction.

### Response Graph
A log-frequency plot inside an Enamel panel, ±18 dB by default and zoomed for ear graphs. Height is 170dp above the faders, 260dp in parametric mode and 130dp for ear curves.

- **Grid:** 1px Hairline at every decade step and at ±6 and ±12 dB; Scale Track at 100 Hz, 1 kHz and 10 kHz; Control Outline at 0 dB. Scale Labels sit on an 85% Enamel backing so a curve cannot obscure them.
- **Curves:** 2dp. Ivory for one curve; Left Ear solid and Right Ear dashed (6dp) when the ears differ; Dim Text when off or bypassed. The saved preset is a 1.5dp Control Outline ghost. The live spectrum sits behind as Dim Text bars at 22%.
- **Audiogram markers:** left X and right O, 5dp arm or radius, 2dp stroke, in ear colours, and repeated in the corner legends with the left ear on the left.
- **Filter points:** 6dp radius ivory rings with a page-coloured centre; 9dp solid with a 25% halo when selected; Dim Text when disabled or dimmed.

### Fader Bank
10, 15 or 31 vertical faders in a 196dp canvas. Two 44 × 32dp icon pills right-aligned above the bank, each after a dim 11sp engraved label (TOUCH, VIEW) (crosshair or wave for **Fader touch**, two-way arrow or fit for **Fader view**, ivory when chosen) choose Precision (the default, where a finger stays on the fader it pressed and drags it relative to its current level, so a tap never jumps it; faders are 44dp wide and scroll sideways when they don't fit, under a 44dp overview strip of the whole curve with the visible window outlined, and the dB scale stays pinned, unless **Fader view** is Fit all; a Smooth sweep held within 48dp of an edge slides the bank; a sideways-phone key beside the pills always opens `ExpandedFaders`, the bank full-screen in landscape) or Smooth (sweeping across draws a curve, drawn as a continuous Catmull-Rom line through every band with a 16% ivory fill to zero, 1dp Hairline band guides, 3dp ivory handles and a 6dp Text handle on the selected band); band arrows, readout and − / + keys beside it give the same control to screen readers. Rails are 2dp, ivory between zero and the value. Caps are up to 22 × 12dp with a 1dp On Lit centre line. The selected cap is at least 14 × 18dp, Text colour, with a 2dp Enamel surround.

### Value Track
Replaces Material's slider: a 2dp rail with 20 engraved ticks, a long tick at the default, and a 12 × 24dp ivory cap with an On Lit grip line. It fills from its origin, with a notch when that origin is centred. Only a sideways drag changes it, and a double tap resets it. It keeps slider semantics for screen readers.

## Do's and Don'ts

### Do:
- **Do** use `Palette`, `KeyShape`, `PanelShape` and the type styles in `Theme.kt` rather than new literals.
- **Do** draw right-ear lines dashed and mark ears with X and O wherever a graph is per-ear.
- **Do** set changing numbers in Readout, in ivory when they differ from the default and Dim Text when they do not.
- **Do** give every graph or fader gesture a button or slider alternative for screen readers.
- **Do** keep the deep green strip as the only green surface, with drawn ticks and crosses for effects.
- **Do** wrap short sets and put long lists in dropdowns.

### Don't:
- **Don't** scroll anything horizontally.
- **Don't** use blue or red for anything except the ears, or use red for errors.
- **Don't** use ear colours for filter points; they apply to both ears.
- **Don't** add scroll fades or gradients to the bars.
- **Don't** bring back the rejected purple strip, or a row of preset keys in the first viewport.
- **Don't** add coloured pill sliders, rounded cards or shadows on panels and keys.
- **Don't** draw an invented meter or present an estimate as a measurement.
