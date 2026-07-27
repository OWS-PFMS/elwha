# ElwhaProgressIndicator — M3 Spec Capture (research scratch)

Status: **CAPTURED** — feeds `elwha-progress-indicator-design.md`. Epic: [#467](https://github.com/OWS-PFMS/elwha/issues/467).
Companion stub (separate component, not covered here): ElwhaLoadingIndicator [#468](https://github.com/OWS-PFMS/elwha/issues/468).

## §TL;DR — synthesis (read this first)

1. **Two variants, one anatomy language.** Linear and circular progress indicators share three parts: **active indicator** (`primary`), **track** (`secondaryContainer`), and — linear only — a **stop indicator** dot (`primary`, 4dp) at the track's trailing end. A **4dp gap** (`TrackActiveSpace`) separates the active indicator's head from the track in *both* variants; the circular shows the gap at *both* arc ends. All shapes are corner-full (round caps).
2. **The updated M3 spec supersedes the web baseline.** material-web's progress (track = `surfaceContainerHighest`, no rounding, no gap, `buffer`, `four-color`, transparent circular track) is the *legacy* design. The current spec (Compose tokens + MDC-Android + the m3.material.io renders) has the gap + stop-indicator anatomy, a visible circular track, and drops buffer/four-color. Elwha implements the current spec.
3. **Wavy is the Expressive addition, expressed as amplitude > 0.** The active indicator (only — the track stays flat, per the spec renders) becomes a sine wave. Linear: amplitude 3dp, wavelength **40dp determinate / 20dp indeterminate**, container height 10dp (14dp at 8dp thickness). Circular: amplitude 1.6dp, wavelength 15dp, container 48dp Ø (52 at 8dp). Wave phase travels at **one wavelength per second** by default.
4. **Determinate wavy ramps its amplitude**: zero at ≤ 10% progress and ≥ 95%, full between — transitions animated (500ms; standard easing in, emphasized-accelerate out). The wave flattens as the bar approaches done.
5. **Thickness is a knob, not a separate component.** Default 4dp; the spec redlines an 8dp "thick" reference (linear total wave height 14; circular Ø 44 flat / 52 wavy). Outer ends are full-round; the **inner ends at the gap hold a 2dp radius** (full-round at 4dp thickness coincidentally = 2; the 8dp redline pins 2 explicitly).
6. **Indeterminate motion is the current Compose choreography** (the expressive refresh, not the classic 1800ms loop): linear = **1750ms** cycle, two lines with head/tail keyframes (see §C), `emphasized-accelerate`; circular = **6000ms** progress cycle, **1080°** global rotation per cycle, plus an extra **360°** advance every 1500ms over 300ms, `standard` easing.
7. **Non-interactive.** No hover/focus/pressed/disabled states, not focusable. A11y is `PROGRESS_BAR` role + value reporting (none when indeterminate) + a developer-supplied accessible name.
8. **Zero new Elwha theme tokens** — color roles, easings, and the Swing `Timer` clock all exist; geometry dp values become component constants (slider precedent).

### Reading-order TOC

§A MDC-Android capture → §B Compose token sheets (verbatim) → §C Compose animation internals → §D material-web legacy delta → §S Operator screenshot capture → §Tokens Elwha mapping → §P terminology→API lock → §Open → §F capture log.

## §A. MDC-Android anatomy + attributes (authoritative text)

Two variants: **linear** ("animates along a fixed horizontal track") and **circular** ("animates along an invisible circular track in a clockwise direction"). Modes: **determinate** ("fill from 0% to 100%... progress and wait time is known") and **indeterminate** ("move along a fixed track, growing and shrinking in size... unknown").

Anatomy: 1 active indicator · 2 track · 3 stop indicator (linear only). `[CODE]`

Common attributes (defaults):

| Attribute | Default | Note |
|---|---|---|
| `trackThickness` | **4dp** | one knob drives indicator + track thickness `[CODE]` |
| `indicatorColor` | `colorPrimary` | `[CODE]` |
| `trackColor` | (see §B — current token is `secondaryContainer`) | MDC doc text lags; Compose tokens authoritative `[CODE]` |
| `trackCornerRadius` | 50% (full) | `[CODE]` |
| `indicatorTrackGapSize` | **4dp** | the active↔track gap `[CODE]` |
| `trackStopIndicatorSize` | **4dp** | 0 hides it `[CODE]` |
| `showAnimationBehavior` / `hideAnimationBehavior` | `none` | enter/exit transitions (`outward`/`inward`/`escape`) `[DOC — deferred, see design §10]` |
| `showDelay` / `minHideDelay` | 0 | flash-prevention timers `[DOC — deferred]` |

Wave attributes (Expressive): `wavelength` (+ `wavelengthDeterminate` / `wavelengthIndeterminate` overrides), `waveAmplitude` (default 0 = flat), `waveSpeed` (default 0 in MDC; Compose defaults to one wavelength/s — we follow Compose, the renders animate), `waveAmplitudeRampProgressMin/Max` (0.1 / 0.9 in MDC; Compose uses 0.1 / 0.95 — we follow Compose). `[CODE]`

Linear-specific: `indeterminateAnimationType` `disjoint` (two same-color segments; `contiguous` needs ≥ 3 colors — legacy, out of scope), `indicatorDirectionLinear` `leftToRight` (RTL-aware), `trackInnerCornerRadius` (inner-end radius at the gap). `[CODE]`
Circular-specific: `indicatorSize` **40dp** (the Ø), `indicatorInset` 4dp, `indicatorDirectionCircular` `clockwise`, indeterminate type `advance`. Thick recommendation: **44dp Ø for 8dp thickness**. `[CODE]`

A11y: inherits `ProgressBar` semantics; set a content description. Components ≤ 4dp tall can have focus-bounds issues with screen readers — pad the *bounds*, not the chrome. `[CODE]`

## §B. Compose Material3 token sheets (verbatim — generated from the token DB; authoritative values)

`ProgressIndicatorTokens.kt`:

| Token | Value |
|---|---|
| ActiveIndicatorColor | `Primary` |
| ActiveShape | `CornerFull` |
| StopColor | `Primary` |
| StopShape | `CornerFull` |
| TrackColor | **`SecondaryContainer`** |
| TrackShape | `CornerFull` |

`LinearProgressIndicatorTokens.kt`:

| Token | Value |
|---|---|
| ActiveThickness | 4.0dp |
| ActiveWaveAmplitude | **3.0dp** |
| ActiveWaveWavelength | **40.0dp** (determinate) |
| IndeterminateActiveWaveWavelength | **20.0dp** |
| Height | 4.0dp |
| StopSize | 4.0dp |
| StopTrailingSpace | 0.0dp |
| TrackActiveSpace | **4.0dp** |
| TrackThickness | 4.0dp |
| WaveHeight | **10.0dp** |

`CircularProgressIndicatorTokens.kt`:

| Token | Value |
|---|---|
| ActiveThickness | 4.0dp |
| ActiveWaveAmplitude | **1.6dp** |
| ActiveWaveWavelength | **15.0dp** |
| Size | **40.0dp** |
| TrackActiveSpace | 4.0dp |
| TrackThickness | 4.0dp |
| WaveSize | **48.0dp** |

`WavyProgressIndicatorDefaults` (from `WavyProgressIndicator.kt`): linear default *width* 240dp; stroke caps `Round` everywhere; wave speed defaults to **the wavelength per second** (determinate and indeterminate each use their own wavelength); indeterminate amplitude fixed at 1.0; determinate amplitude lambda → **0.0 for progress ≤ 0.1 or ≥ 0.95, else 1.0**; progress value animation `DurationLong2` (500ms) `EasingLinear`; amplitude transitions `DurationLong2` with `Standard` (increasing) / `EmphasizedAccelerate` (decreasing).

## §C. Compose animation internals (current `ProgressIndicator.kt` — the Expressive refresh)

Linear indeterminate — **two lines**, cycle `LinearAnimationDuration = 1750ms`, all four channels eased `EasingEmphasizedAccelerateCubicBezier`:

| Channel | Delay | Duration |
|---|---|---|
| FirstLineHead | 0 | 1000ms |
| FirstLineTail | 250 | 1000ms |
| SecondLineHead | 650 | 850ms |
| SecondLineTail | 900 | 850ms |

(Each channel animates a 0→1 fraction across the bar; a line is the [tail, head] span. Track paints in the remaining spans with the 4dp gap on both sides of each line; **no stop indicator in indeterminate**.)

Circular indeterminate: `CircularAnimationProgressDuration = 6000ms`, `CircularGlobalRotationDegreesTarget = 1080f`, `CircularAdditionalRotationDelay = 1500ms`, `CircularAdditionalRotationDuration = 300ms`, `CircularAdditionalRotationDegreesTarget = 360f`, easing `EasingStandardCubicBezier`. Interpretation (validated visually at smoke): the arc sweep grows/shrinks over the 6s cycle while the whole figure rotates 1080° per cycle, with a 360° `advance` kick every 1.5s over 300ms — the MDC `advance` indeterminate type. `[CODE — S4 implementation note]`

A11y in Compose: `progressBarRangeInfo = ProgressBarRangeInfo(progress, 0f..1f)`; indeterminate omits the value.

## §D. material-web — legacy delta (what we are NOT building)

`md-linear-progress` / `md-circular-progress`: `value`/`max`/`indeterminate` survive conceptually. Superseded/legacy, **out of scope** with rationale (design §10): `buffer` (web-only dual-progress affordance, absent from the current M3 spec + Compose + the renders), `fourColor` (legacy indeterminate color cycling), track = `surfaceContainerHighest` + zero corner rounding + no gap/stop (the old anatomy), circular track transparent (current spec renders a visible circular track). a11y text carries over: name the indicator (`aria-label` ≈ accessible name), `role=progressbar`. `[CODE for the a11y line; rest DOC]`

## §S. Operator screenshot capture (m3.material.io renders, 2026-06-11)

Two screen captures pasted in-session (temp paths, not committed; transcribed here — **findings read from the renders, not captions**):

**S-1 — variants overview (anatomy render).** ① Linear, two rows: *top* — flat determinate: solid `primary` active bar on the left, lighter `secondaryContainer` track to the right, **clear gap** between them, **small `primary` dot at the track's far right end** (stop indicator). *Bottom* — wavy determinate: the **active portion only** is a sine wave; the remaining track is **flat**, same gap + stop dot. ② Circular, two figures: *left* — flat: thick `primary` arc (~120°) over a lighter full-circle track, **gaps at both ends** of the active arc. *Right* — wavy: the active arc is **scalloped/wavy**; the track remains a **flat** circle. → Locks: wave applies to the active indicator only; circular has a visible track + double gap; stop indicator is linear-only.

**S-2 — measurements redline.** Linear (4 rows):

| Row | Thickness | Gap | Stop | Wavelength | Amplitude | Total height | Extra |
|---|---|---|---|---|---|---|---|
| flat default | 4 | 4 | 4 | — | — | **4** | |
| flat thick | 8 | 4 | 4 | — | — | **8** | inner-corner radius **2** at the gap ends |
| wavy default | 4 | 4 | 4 | **40** | **3** | **10** | (4 + 2·3 ✓) |
| wavy thick | 8 | 4 | 4 | **40** | **3** | **14** | (8 + 2·3 ✓); inner-corner **2** |

Circular (4 figures): **40 Ø / 4** thick (flat) · **44 Ø / 8** (flat thick) · **48 Ø / 4**, amplitude **1.6**, wavelength **15** (wavy) · **52 Ø / 8**, amplitude 1.6, wavelength 15 (wavy thick); each carries a **4** annotation at the arc gap (= TrackActiveSpace 4 ✓ §B). Caption fragment: thicker variants are "sample measurement" guidance — adjust from the default per use case. → Locks: Ø grows **+4 for 8dp thickness, +8 for wavy** (44 = 40+4; 48 = 40+8; 52 = 40+4+8); redline wavelength/amplitude match §B tokens exactly (cross-check ✓, no mismatches).

⚠️ Reading note: the linear wavy redlines show the determinate **40dp** wavelength; the **20dp** indeterminate wavelength appears only in the Compose token sheet (§B) — not a ghost, it's mode-dependent, both kept.

## §Tokens — Elwha mapping (zero new tokens ✓)

| M3 need | Elwha |
|---|---|
| active indicator + stop color (`primary`) | `ColorRole.PRIMARY` |
| track color (`secondaryContainer`) | `ColorRole.SECONDARY_CONTAINER` |
| corner-full shapes | round stroke caps / capsule paths (geometry, not `ShapeScale`) |
| 4dp gap / 4dp stop / thickness / wavelength / amplitude / Ø | component constants (px = dp at 1×; slider precedent) |
| `EasingStandardCubicBezier` | `Easing.STANDARD` ✓ exists |
| `EasingEmphasizedAccelerateCubicBezier` | `Easing.EMPHASIZED_ACCELERATE` ✓ exists |
| `EasingLinear` | `Easing.LINEAR` ✓ exists |
| `DurationLong2` 500ms | component constant |
| animation clock | `javax.swing.Timer` (lib-established) |

## §P. Terminology → API lock (M3 nouns, exactly)

| M3 / MDC noun | Elwha API |
|---|---|
| Progress indicator (component) | package `progress/`, base `AbstractElwhaProgressIndicator` |
| Linear / Circular progress indicator | `ElwhaLinearProgressIndicator` / `ElwhaCircularProgressIndicator` (MDC class-split precedent) |
| determinate / indeterminate | `setIndeterminate(boolean)` (JProgressBar-coincident) |
| active indicator / track / stop indicator | painted parts; `setIndicatorColorRole` / `setTrackColorRole` / `setStopIndicatorColorRole` |
| track thickness | `setTrackThickness(int)` |
| indicator–track gap (`TrackActiveSpace`) | `setIndicatorTrackGapSize(int)` |
| stop indicator size (`StopSize`) | `setTrackStopIndicatorSize(int)` (0 hides) |
| wavy (Expressive shape) | `setWavy(boolean)` + wave knobs `setWaveAmplitude(float)` / `setWavelength(int)` (+Determinate/Indeterminate) / `setWaveSpeed(float)` |
| indicator size (circular Ø) | `setIndicatorSize(int)` |

## §Open — resolved in the design doc

1. One class vs two → **two + abstract base** (design §2).
2. Value model → **`BoundedRangeModel`** (design §2, slider precedent).
3. Circular indeterminate choreography interpretation → design §6, tuned at smoke.
4. Inner-corner-2 rendering approach → design §5.
5. Show/hide behaviors + delays → out of scope V1 (design §10).

## §F. Capture log (fetched 2026-06-11)

- `https://raw.githubusercontent.com/material-components/material-components-android/master/docs/components/ProgressIndicator.md`
- `https://raw.githubusercontent.com/androidx/androidx/androidx-main/compose/material3/material3/src/commonMain/kotlin/androidx/compose/material3/WavyProgressIndicator.kt`
- `https://raw.githubusercontent.com/androidx/androidx/androidx-main/compose/material3/material3/src/commonMain/kotlin/androidx/compose/material3/ProgressIndicator.kt`
- `https://raw.githubusercontent.com/androidx/androidx/androidx-main/compose/material3/material3/src/commonMain/kotlin/androidx/compose/material3/tokens/{ProgressIndicatorTokens,LinearProgressIndicatorTokens,CircularProgressIndicatorTokens}.kt`
- `https://raw.githubusercontent.com/material-components/material-web/main/docs/components/progress.md` (legacy delta)
- Operator screenshots: m3.material.io progress-indicators specs pages (variants render + measurements redline), pasted 2026-06-11 — transcribed in §S. (m3.material.io is JS-only; WebFetch returns the page title.)
