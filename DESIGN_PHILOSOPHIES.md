# Design Philosophies

The interaction and UX practices behind Eigentone, written so the next app starts where this one ended rather than where it began. Colours, fonts and theming are left out on purpose; this is about structure, behaviour, wording and process.

Every rule below was paid for. Each comes with **what went wrong** in Eigentone (found by the owner, a tester, or an audit) and **how to apply it** next time.

---

## 1. Product stance

### 1.1 Simple by default, powerful when needed
Borrowed from KDE Plasma's motto, and adopted after the tester said the app had "a lot all around" and "isn't exactly intuitive".

- **What went wrong:** features were added one after another, each placed wherever it fitted at the time. After about twenty features, the Sound page had too many settings everywhere, and a new user faced a compressor's attack and knee before finding presets.
- **How to apply:**
  - Decide the beginner's surface on day one, before the feature list grows.
  - Hide expert controls behind a single **Advanced controls** switch, not scattered "more" links.
  - **An advanced control that has been changed from its default is always shown**, even with Advanced off, so nothing in use is ever hidden.
  - People updating from an older version keep everything visible. Never take away a control someone already uses.

### 1.2 Design for the daily task, not the feature list
- The daily tasks were: switch presets, nudge the sound, check it's working, leave. Tools and tests are occasional.
- **What went wrong:** preset switching started as a row of keys inside a tab. The owner asked for it in the top bar, on every tab.
- **How to apply:** write down the three things people do on most visits, and make each reachable from every screen in one tap. Everything else can be two taps away.

### 1.3 Offer the option, don't make the decision
- **What went wrong:** per-ear correction was only offered when the ears differed by 10 dB or more. The owner's didn't, and they wanted it anyway: "having an option is always better than not".
- **How to apply:** when the app has a recommendation, show the recommendation *and* the way around it ("Correct anyway"). Recommend; don't gatekeep.

### 1.4 Only show what's true
- Nothing is drawn as a live measurement unless it is one. The compressor shows "on", not an invented gain-reduction meter, because Android doesn't report one.
- Estimates are labelled as estimates (clip risk is "could clip", from a worst case).
- **What went wrong:** a reverb shipped that received no audio at all, and the virtualizer reported "off" while working. Both were only caught by listening and measuring.
- **How to apply:** verify every effect end-to-end on a device, with a measurement if possible, before giving it UI. If it can't be made to work, remove it rather than leave a placebo control.

### 1.5 Fair comparisons
For an app where the user judges by ear or eye, the comparison is the product.
- Comparisons are level-matched (louder always sounds "better"), optionally blind, and sides swap at random.
- Every tool ends by comparing the result with what the user had, and "keep what you had" is presented as a perfectly good answer.

---

## 2. Information architecture

### 2.1 List every feature before organising any of them
- **What went wrong:** the first reorganisation moved things around without a full inventory, so overlaps survived. There was a Bass dial *and* a Bass tuner, compressor styles *and* tamers doing the same job.
- **How to apply:** before a redesign, write every feature and setting in one table: what it does, how often it's used, who it's for, and what it overlaps with. Merge overlaps first, then group by task, then decide placement.

### 2.2 One home for each thing
- Each setting lives in exactly one place. If it must be reachable elsewhere, link to that place rather than duplicating the control.
- Saved-with-the-preset settings and global settings live in different places, and the UI says which is which. Eigentone split them into the preset's Sound and app-wide Settings after users couldn't tell what a preset would remember.

### 2.3 Navigation that holds up
- **What went wrong:**
  - The bottom bar had five tabs and felt crowded. Settings moved to a gear at the top right.
  - Tabs felt too far apart, and the selected highlight looked "small and squished".
- **How to apply:**
  - Four bottom tabs at most, with labels of similar length.
  - Give the selected indicator a generous size (Eigentone settled on 64 × 32 dp) and let it hold the press ripple.
  - Each tab keeps its own scroll position. Tapping the tab you're already on scrolls it to the top.
  - Back from a secondary page (Settings) returns to the tab it was opened from.

### 2.4 No sideways scrolling
- **What went wrong:** sets of chips overflowed into horizontal scrollers, which hid options off screen.
- **How to apply:** short sets wrap onto the next line, long lists become dropdowns. The single exception should be a deliberate one (Eigentone's precision fader bank), and then it gets an overview of the whole range so nothing is hidden.

### 2.5 Fold, but summarise what's folded
- Long panels fold under their heading, and while folded they show a one-line summary of what's on ("Compressor, Clarity") or "All off".
- **What went wrong:** "Timings and crossover" was collapsed but didn't look like it could open.
- **How to apply:** a fold needs a chevron and a whole-row tap target, or it shouldn't be folded at all. Short or essential panels stay open.

### 2.6 Context decides what's offered, everywhere at once
- Features only appear where they make sense: peak finding and the sweep only for headphones and earbuds, never for the phone speaker; presets made for specific earbuds only while those earbuds are connected.
- **What went wrong:** the earbuds-only presets were hidden in the preset list but still appeared in Compare curves, so other people saw the owner's Liberty 4 presets.
- **How to apply:** put each filtering rule in one function, and make every list that shows those items call it. When fixing a leak in one place, search for every other list of the same thing.

### 2.7 Keep often-used options next to what they change
- **What went wrong:** the fader touch and view options sat below the faders, so the owner had to scroll down to change how the faders above behaved.
- **How to apply:** mode switches sit directly beside or above the thing they affect, as small labelled icon toggles. The landscape button went next to them too.

---

## 3. Edits, presets and state

### 3.1 Show the real value, not the difference
- **What went wrong:** choosing a preset left the faders flat, with the user's edits layered on top of a hidden preset. The tester asked for the faders to move to the preset's curve, "makes it easier to see how many decibels are being changed".
- **How to apply:** controls always show the absolute, audible value. If the model stores differences, that's an internal detail. Where a value goes beyond the control's range, the control rests at its end and the readout gives the real figure.

### 3.2 Unsaved edits are visible the moment they exist
- **What went wrong:** "Unsaved edits" only appeared after switching presets, so users didn't know they'd changed anything.
- **How to apply:**
  - Mark the preset as edited as soon as anything changes, using the same wording on every surface. "Optimus, edited" in one place and "Optimus (edited)" in another counts as two different states to a user.
  - A floating Save / Discard bar appears with the first edit and leaves when there's nothing to save.
  - A saved preset is called by its own name, not by what it was built from ("Optimus", not "Optimus + Bass lift + …").

### 3.3 Discard means "back to saved", never "delete"
- **What went wrong:** adding a filter and pressing Discard removed *every* filter, including the ones saved with the preset.
- **How to apply:** Discard restores the saved version exactly. Write a test that edits, discards, and compares with the saved copy. The discard prompt says what will be lost, and Discard itself can be undone.

### 3.4 Undo for everything that acts immediately
- Deleting, resetting, removing a correction, restoring a backup, discarding: all offer Undo, and Redo sits beside it.
- **What went wrong:** Undo messages vanished instantly when the user changed tab, so the safety net existed but couldn't be reached.
- **How to apply:** make the Undo message outlive navigation and stay until the user acts or a sensible timeout. A new edit clears the redo stack.

### 3.5 Switching representations must not lose detail
- **What went wrong:** switching from 31 bands to 10 and back degraded the whole curve.
- **How to apply:** keep the finest version and derive coarser views from it. Only the parts actually edited at the coarser level take the coarser shape.

### 3.6 Temporary changes never become permanent by accident
- Previews (a shape being browsed, a comparison side, a tool's test sound) are never saved into a preset.
- Every tool restores the previous sound when it exits, however it exits: back, cancel, rotation, or the app being closed.
- While a tool owns the sound, automatic changes (switching preset when a device connects) wait.
- **What went wrong:** test tones kept playing after leaving the app, and a comparison could apply a different version from the one the user heard.

### 3.7 Never lose user data silently
- **What went wrong:** if the saved file couldn't be read (for example after installing an older version), every preset was silently replaced with defaults.
- **How to apply:** unknown values fall back to defaults field by field. An unreadable file is kept as a backup copy, never overwritten. Deleting something also removes every reference to it (favourites, per-app choices), so nothing points at a ghost.

### 3.8 Names are unique
Two presets called "Bass" make every menu ambiguous. Enforce unique names at save and rename.

---

## 4. Controls and touch

### 4.1 Controls ignore accidental touches
- **What went wrong:**
  - Sliders changed when a scroll gesture or stray tap landed on them.
  - In precision mode, tapping the rail made the cap jump there.
  - A finger sliding sideways across faders grabbed the neighbouring fader.
- **How to apply:**
  - A slider only moves on a deliberate sideways drag, never on a tap or a vertical scroll.
  - Dragging is relative to the current value, so touching never jumps the control.
  - A drag stays locked to the control it started on.
  - Offer a second mode for those who want to sweep or draw (Eigentone's Precision and Smooth).
  - Double-tap returns a control to its saved or default value.

### 4.2 Every gesture has a button alternative
Graph points, faders and drawn curves each have arrows, a readout and − / + keys beside them, so screen-reader users and people with shaky hands can make the same change.

### 4.3 Touch targets of at least 48 dp, and the whole row counts
- A settings row toggles when any part of it is tapped, not just the switch.
- **What went wrong:** the audit found the small mode pills, the output meter and the status strip under 48 dp.

### 4.4 Labels fit their controls, in every orientation
- **What went wrong:** "Parametri", "Band p…" and "Transparency" were cut off. "Report a problem…" wrapped onto two lines.
- **How to apply:**
  - Size keys to their words, or shorten the words. Never let a label truncate.
  - Check with the longest label, the largest system font size and a pseudo-locale (Android's fake languages stretch text by about 30%).

### 4.5 Things don't move when their state changes
- **What went wrong:**
  - Pressing Bypass widened the button and pushed it into the version label.
  - Bypass then sat too close to the power switch, with spacing unlike anywhere else in the bar.
  - A Reset button jumped in and out of a header.
- **How to apply:** controls that change label or state keep a fixed width. Space in a bar follows one rule. Something that comes and goes reserves its space, or fades rather than shifting its neighbours.

### 4.6 Affordances must look like what they do
- **What went wrong:**
  - The settings icon looked like a sun.
  - The tappable status strip gave no hint that it opened.
  - App-icon shortcut icons rendered white and looked broken.
- **How to apply:**
  - Use the platform's standard icon for a standard action (gear, copy, undo).
  - Anything that expands gets a chevron.
  - A copy action uses a copy icon, placed where the platform puts secondary actions (top right of a dialog), not a text button among the main ones.
  - Look at every icon at real size on a real phone.

### 4.7 Use the input that matches the data
- **What went wrong:** night-mode times were set with a slider. The owner called it "hella weird".
- **How to apply:** times use a clock picker, dates a calendar, choices from a list a dropdown, and short sets segments. Validate impossible combinations, such as start equal to end, with a visible warning.

### 4.8 Landscape and rotation are real modes
- **What went wrong:**
  - In landscape, the page couldn't be scrolled while the equaliser filled the middle, because only the sides would have scrolled and they didn't.
  - The landscape toggle sat against the screen edge and looked like two separate buttons.
  - Undo and Redo were missing in landscape.
- **How to apply:**
  - Every scrolling surface scrolls from anywhere, including beside centred content.
  - A two-state toggle looks like one control, a sliding switch rather than two buttons.
  - Every essential action exists in every orientation.
  - Tests and tools survive rotation without losing answers.

### 4.9 Haptics mark meaningful thresholds only
- A light tick when a control crosses zero or its default.
- A firmer tap at either end of its travel.
- Confirmation on power, Bypass and Save.
- Not on every step, which turns feedback into noise.

---

## 5. Overlays, menus and dismissal

### 5.1 Tap outside closes, and the tap still works
- **What went wrong:** with the status drawer open, tapping the Effects tab only closed the drawer, so every action needed two taps.
- **How to apply:** a non-modal panel closes on an outside tap *and* passes that tap through. Only true dialogs block the page.

### 5.2 Back closes the innermost thing first
Order: menu, then drawer, then dialog, then tool, then secondary page, then tab history. Back must never leave the app while something is open.

### 5.3 Overlays appear from where they came from and return there
- Menus unfold from their control and fold back into it.
- Dialogs shrink and fade on close, taking the dim with them.
- Material's stock menu exit was a 75 ms flicker that read as "no close animation". The tester noticed straight away.
- **How to apply:** for each popup, check the close as carefully as the open.

### 5.4 Drawers lie over the page
- **What went wrong:** the status drawer pushed the whole page down as it opened. That was slow, because the whole page re-laid out every frame, and disorienting.
- **How to apply:** a temporary panel overlays the content with a shadow, and the content underneath stays still.

### 5.5 Rows in a summary go somewhere
A summary row (an effect listed in the status drawer) opens its setting: switch tab, unfold the section, scroll it into view. It has to land on the setting itself, not just the tab. Eigentone's first version cleared the request before the scroll ran, so it silently stopped at the tab.

---

## 6. Motion

### 6.1 Every animation has a reverse
- **What went wrong:** the tester asked for "animations that go back the way they came", after menus, dialogs and drawers opened with motion but vanished on close.
- **How to apply:** design open and close as a pair. Leaving reverses entering: a tool slides in from the right and out to the right, and drawer rows drop in one by one and lift out the same way.

### 6.2 Motion carries direction
- Tabs slide towards where they sit in the bar.
- Tool steps slide forward, and Back slides them backward.
- Switching between Graphic and Parametric slides in the direction of the switch.
- Elements that move between states travel. The strip's icons slide down into their own drawer rows rather than fading in and out.

### 6.3 Start moving on the first frame
- **What went wrong:** the owner asked "is there a delay when switching pages before the animation starts?" There was. Ease-in curves spend their first frames barely moving, and building a tab on first visit took a 25–84 ms frame before any motion.
- **How to apply:**
  - Use a decelerate curve (full speed, then settle) for anything that responds to a tap.
  - Build likely destinations ahead of time, after the first screen has finished appearing.
  - Keep hidden pages alive but not drawn.
  - Measure frame times on a real phone (Android's `dumpsys gfxinfo`) instead of trusting the eye.

### 6.4 Nothing jumps
- **What went wrong:** opening a folding section made its heading snap down as the summary line disappeared.
- **How to apply:**
  - Things that appear grow into place, and things that leave shrink away.
  - Layout changes animate their size.
  - Numbers that change while you watch use fixed-width digits.
  - Curves and controls glide between presets rather than teleporting.

### 6.5 Endings fade
The owner preferred a fade for the end of a transition (welcome into the app) over a squeeze or slide. Reserve slides for spatial moves, and use fades for arrivals and endings.

### 6.6 Animation must not cost every frame
Read animated values only where they're drawn (layout and draw phases), so the rest of the screen doesn't rebuild 60 times a second. Profile the heaviest transition before shipping it.

---

## 7. Wording

### 7.1 Explain every setting, including its limits
- Every setting has an info button with a plain explanation, kept in one file so the tone stays consistent.
- Say what something *can't* do ("isn't a medical test", "only clear differences are worth fixing").
- Long help opens in a scrolling dialog, not a tooltip that runs off screen.

### 7.2 Answer options must cover the user's real states
- **What went wrong:** "I can't hear a difference" didn't fit a tester who heard a difference but couldn't choose. It became **"I'm not sure"**.
- **How to apply:** for every question the app asks, list the states a person could actually be in, and make sure one honest answer exists for each. Tell people what to do when unsure ("If you're unsure, answer Same").

### 7.3 Buttons say what they will do
- **What went wrong:**
  - The hearing check's "Discard" didn't discard anything; it meant "Not now".
  - Rows that act immediately showed a "›" as if they opened a page.
- **How to apply:**
  - Verbs match outcomes.
  - "›" only on rows that navigate.
  - Destructive rows look destructive.
  - Unavailable rows look unavailable and say why.

### 7.4 Rename everywhere at once
- **What went wrong:** after "Smooth" became "Smoothen", its info box was still titled "Smooth".
- **How to apply:** search all strings, titles and accessibility labels for the old word whenever something is renamed.

### 7.5 Never show raw internals
Errors never say "…: null". Technical failures become a sentence saying what happened and what to try. Blind comparison names never leak into the notification, tile or widget.

### 7.6 Say what is happening right now
- "Nothing is playing" when a listening tool needs music and none is playing.
- "Bypass is on, so you won't hear this" on screens where an edit wouldn't be heard.
- Progress after every answer in a test.
- A status line that turns into the warning colour on a problem.

---

## 8. Tools, tests and guided flows

### 8.1 One scaffold for every tool
- **What went wrong:** the hearing check, peak finder, sweep, Find my sound and Compare each had their own top bar, intro panel, step counter and heading size.
- **How to apply:** build the tool frame once (back arrow, title, progress scale, intro, steps, result, compare-with-before) and make every tool use it. Comparison screens share one interaction: a single main answer button that follows the card being heard.

### 8.2 Protect work in progress
- Back partway through a test asks before throwing away answers.
- An intro page has no "Not now" dead end. Back is the way out.
- An answer button only becomes active when it's meaningful. "I didn't hear it" waits until the tone has finished.

### 8.3 Keep results so the decision can be made later
- **What went wrong:** finishing the hearing check without pressing the per-ear button lost the chance to apply it. The owner asked if they could still do it.
- **How to apply:** save every result, show it wherever it's relevant, and offer its actions from there later. A new result replaces the old graph straight away. The "how each ear heard" graph didn't update after a repeat check.

### 8.4 Nothing is kept unless the user adds it
Tools suggest; the user accepts. Every suggestion can be compared with music before it's kept, and undone after.

---

## 9. First run and permissions

### 9.1 Welcome, then a tour over the real app
- A short, resumable welcome: what it is, turn it on, your headphones, find your sound, how hands-on you want to be.
- Then a tour that points at real controls in the live app.
- Both can be reopened from Settings.
- People updating are offered the tour once, not forced through the welcome.
- **What went wrong:**
  - Tour "Next" sometimes did nothing.
  - One step pointed at a save bar that wasn't on screen.
  - The Settings step outlined the power switch too.
- **How to apply:** test every tour step on a real device, in both a fresh state and a busy one. Show an example when the real element may be absent.

### 9.2 Ask for permissions in context, with a reason
- Ask for notifications when the user turns the feature on, explaining why.
- Ask to be excluded from battery optimisation during the welcome. The tester pointed out it's essential, because the EQ died on a locked screen without it.
- Only ask on platform versions that need asking (Android 13+ for notifications).

### 9.3 Nothing changes under someone updating
New defaults apply to new installs. Existing users keep their controls visible, their settings, and their saved data in its old form, migrated automatically.

---

## 10. Device and platform awareness

- Know the output while the app is "off". Features and settings depend on it.
- Name things after what's connected (the device tab is called "Earbuds", "Headphones" or "Car"), and change it live when the device changes.
- Only switch automatically on a real change (a new device), never on every app start.
- Support the oldest platform version you claim, with every newer API guarded. Lint (`NewApi`) is a cheap first pass; a real device is the proof.
- Some phones refuse some features. Detect the refusal, say so, and fall back (stereo width when the phone refuses virtual surround) rather than failing silently.

---

## 11. Process

### 11.1 Test on a real phone after every change you can see
Screenshots and recordings caught what code review couldn't: a squished nav highlight, a logo that "looks hellish", an info icon touching the screen edge, a strip tap opening the wrong menu. Claim nothing works until it's been seen on a device. Say clearly what wasn't checked.

### 11.2 Audit regularly, with a score
Run a full audit after each big batch: every screen, every state, accessibility, wording and consistency, scored so improvement is measurable. Eigentone's first full audit scored 13/20 and found 20+ bugs and 15 consistency problems no single feature review had seen.

### 11.3 Feed testers' words straight into the backlog
Most of the best changes came from one tester using the app normally: page slides, haptics, one device name in the strip, menus that close the way they opened, "I'm not sure", and faders that show the preset's real curve. Keep their exact words, act on them quickly, and credit them.

### 11.4 Record decisions so they aren't re-argued
User decisions (no horizontal scrolling, preset dropdown in the top bar) were written into DESIGN.md as binding rules with their reasons. Write every "we decided X because Y" down where the next change will see it.

### 11.5 Fix the class, not the instance
When a bug appears in one list, menu or row, search for every sibling: the earbud preset leak, a missing row divider, an unlabelled icon. Most of Eigentone's repeat bugs were the same mistake in a second place.

### 11.6 Measure, don't guess
Whether reverb was audible, whether the virtualizer worked, whether a refinement algorithm helped, whether an animation was smooth: each was settled with a measurement (recorded output, simulated listeners, frame timings), not opinion.

---

## Checklist for a new screen

Before calling a screen done:

- [ ] Its daily task is reachable in one tap from wherever people usually are.
- [ ] Beginner view and Advanced view both make sense, and changed advanced values are visible.
- [ ] Nothing scrolls sideways, and no label truncates, at the largest font and in a pseudo-locale.
- [ ] Every control shows the absolute value, ignores stray touches, has a button alternative, and is at least 48 dp.
- [ ] Every edit is marked as unsaved immediately, and can be saved, discarded (back to saved) or undone.
- [ ] Previews and tools restore what was there on every exit path, including rotation.
- [ ] Every overlay opens from its source, closes back into it, passes outside taps through, and closes on Back.
- [ ] Every animation reverses, starts on the first frame, and is measured on a real phone.
- [ ] Every setting has a plain explanation, including what it can't do.
- [ ] Every question has an honest answer for each state a person could be in.
- [ ] Context filters (device, version, connection) are applied in every list that shows the same items.
- [ ] Data can't be lost by a bad file, a deletion or an update.
- [ ] It has been used on a real phone, in portrait and landscape, by someone who didn't build it.
