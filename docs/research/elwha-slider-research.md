# ElwhaSlider — M3 Spec Capture (research scratch)

**Status:** RAW CAPTURE — accumulating M3 source material for epic [#340](https://github.com/OWS-PFMS/elwha/issues/340) (ElwhaSlider stub, `v0.4.0`). Not a design doc yet; this is the companion research dump (mirrors [`elwha-textfield-research.md`](elwha-textfield-research.md) / [`elwha-menu-research.md`](elwha-menu-research.md) / [`elwha-navigation-rail-research.md`](elwha-navigation-rail-research.md)). Promote a real `elwha-slider-design.md` when Phase 0 runs.

**Captured:** 2026-06-04. **Author:** Charles Bryan (`cfb3@uw.edu`).

**Consumers / related:**
- **OWS-PFMS/OWS-Local-Search-GUI** — any range/threshold control would migrate onto `ElwhaSlider` once Phase 1 ships (file a consumer-side migration tracker when the API stabilises).
- **7 raw `JSlider` sites inside this repo** (`card/v1/playground/LiveConfigPanel.java`, `ElwhaCardListShowcase.java`) — the dogfood target ([[feedback_dogfood_elwha_components]]).
- [`ShapeMorphPainter`](../../src/main/java/com/owspfm/elwha/theme/ShapeMorphPainter.java) (#176) — handle press squish, if M3 Expressive calls for it.
- [`RipplePainter`](../../src/main/java/com/owspfm/elwha/theme/RipplePainter.java) + `StateLayer` (`HOVER`/`FOCUS`/`PRESSED`/`DRAGGED`) — the handle state layer set maps **exactly** to the existing enum.
- [`ElwhaNavRailDestination`](../../src/main/java/com/owspfm/elwha/navrail/ElwhaNavRailDestination.java) — the `extends JComponent implements <Bearing>` dedicated-primitive template.

---

## §TL;DR — synthesis (read this first)

**What M3 Expressive Slider is**, distilled from the full capture below:

1. **One component, two orthogonal axes.** **Variant** (mutually exclusive): `STANDARD` · `CENTERED` · `RANGE`. **Configurations** (independent): inset icon · orientation (H/V) · size (XS–XL) · **stops** (formerly "discrete") · value indicator. (§Cfg, §V — M3 Expressive renamed *continuous→standard*, *discrete→stops*; mirror those nouns, §P.)
2. **Zero new theme tokens.** Every color/shape/type need maps onto existing `ColorRole` / `StateLayer` / `ShapeScale` / `TypeRole`. (§Tokens.)
3. **Color (light + dark, role-named §Color):** active track + handle = **PRIMARY**; inactive track = **SECONDARY_CONTAINER**; stop on active = **ON_PRIMARY**, stop on inactive = **ON_SECONDARY_CONTAINER**; value bubble = **INVERSE_SURFACE** + **INVERSE_ON_SURFACE**; disabled = **ON_SURFACE** @ 0.38 (active/handle) / 0.12 (inactive).
4. **Geometry, XS default (§M, §T):** track **16dp**; handle **44×4dp** pill, **flat — no shadow**; **6dp** handle↔track gap; stop **4dp**; track outer corner full, **inner (gap-side) corner ~2dp**; value bubble **44×48dp**, **12dp** above the handle.
5. **Interaction (§S, §TS, §B):** handle **narrows 4dp→2dp on focus/press** (height constant 44dp); **pressed = `RipplePainter` ripple**; hover/focus = static `StateLayer` (0.08 / 0.10); **value bubble shows on focus + press**; live value updates during drag.
6. **Behaviors + keyboard (§B, §X):** drag (smooth / snap-to-stop); **click-to-jump**; Tab→handle, Arrows ±step/stop, Space+Arrows ±interval, **Home/End → min/max**. **RTL mirrors** the fill direction (§GD).
7. **A11y (§X):** role = **slider** (`AccessibleRole.SLIDER` + `AccessibleValue`); name = adjacent label; external +/− = `ElwhaIconButton` (Button role). End stops exist for the **≥3:1 contrast** requirement.
8. **Architecture (RECOMMENDED, S1-spike-locked, §0 + §X-arch):** one **`ElwhaSlider extends JComponent`** backed by a `BoundedRangeModel` (single) / two-value model (range); keymap + a11y hand-wired once (finite + fully captured). Range forces custom regardless, so a unified custom path beats `SliderUI`-for-single + separate-range. Build **RTL/orientation-aware from S1**.
9. **Sizes (§M, §GD5):** five (XS 16 / S 24 / M 40 / L 56 / XL 96 track dp). **XS is M3's only off-Android code preset** → XS Phase 1, the `Size` enum a later phase is M3-faithful, not a cut.
10. **No silent cuts:** vertical+range = doc-warn (§G); external value-field = Showcase recipe composing **ElwhaTextField #286** (§GD2); inset icon couples to size (M/L/XL only, §GD4).

**The open calls for Phase C** (V1/phase split + architecture lock + a few design toggles) are in §Open — surfaced to the operator as a numbered list.

### Reading-order TOC
*(Sections accrued in capture order; logical order:)* §0 architecture fork → §A overview → §V/§V2 variants → §Cfg variant/config split → §An anatomy → §Color roles → §M measurements → §T/§TS token sheets → §S states → §B behaviors → §G/§GD/§GD2–5 guidelines → §X accessibility → §Tokens map → §P terminology → §Open questions → §F screenshot log.

---

## §0. Scope decision + the load-bearing question

**The central Phase-0 fork (mirrors the text-field wrap-vs-extend call, but lands differently):**

A slider carries non-trivial behavior worth not reinventing — keyboard arrow/page stepping, the `BoundedRangeModel` value math, `AccessibleValue` a11y, focus traversal — all of which `JSlider` already provides. **But** unlike the text field (where `JTextField`'s *chrome* was close enough to decorate), M3 Expressive slider chrome diverges **completely** from `JSlider`'s painted form: pill handle, split active/inactive track with an end-gap, stop-indicator dots, a value-label bubble. A thin decorator buys far less here because almost nothing of `JSlider`'s paint survives.

Two candidate architectures (decide in Phase 0, lock via S1 spike):

- **(A) Custom `JComponent` + `BoundedRangeModel`** — paint all M3 chrome ourselves; borrow only the value/step model and wire keyboard + `AccessibleValue` by hand. Mirrors `ElwhaNavRailDestination`'s dedicated-primitive path. Full control over the Expressive geometry; we own ~all the a11y/keyboard glue.
- **(B) `JSlider` + custom `SliderUI` delegate** — keep `JSlider`'s model + keyboard + accessibility, replace only the painted UI. Less glue, but the `BasicSliderUI` track/thumb layout model fights the M3 split-track-with-gap geometry, and range (two-thumb) isn't a `JSlider` concept at all → range would need a custom component regardless.

**Provisional lean (now informed by §B behaviors + §X a11y):** the **range** variant forces a custom component regardless — two thumbs are not a `JSlider` concept, and two-handle a11y/keyboard is custom either way (§X-arch). So maintaining *both* a `SliderUI` (for single) *and* a separate custom range component = two code paths for one component family. **Cleaner: one dedicated `ElwhaSlider extends JComponent`, backed by a `BoundedRangeModel` (single) / two-value model (range), with the keymap (§49: arrows / Space+arrows / Home-End) and `AccessibleRole.SLIDER` + `AccessibleValue` hand-wired once.** The keymap + a11y surface is **finite and fully captured** (§B, §X) — not the open-ended reimplementation that made the *text* field lean the other way. **RECOMMENDED (A); lock in the S1 spike.** (The spike should sanity-check whether borrowing `JSlider`'s model+keyboard *internally* for the single case saves enough to justify the second code path — default to the unified custom path if not.)

---

## §B. Material Web API (authoritative text)

Source: <https://raw.githubusercontent.com/material-components/material-web/main/docs/components/slider.md> (fetched 2026-06-04).

| Property | Type | Default | Notes |
|---|---|---|---|
| `min` | number | 0 | range lower bound |
| `max` | number | 100 | range upper bound |
| `value` | number | undefined | single-slider value |
| `valueStart` | number | undefined | **range** lower thumb |
| `valueEnd` | number | undefined | **range** upper thumb |
| `valueLabel` | string | '' | single value-label text |
| `valueLabelStart` / `valueLabelEnd` | string | '' | range value-label texts |
| `step` | number | 1 | discrete snap increment |
| `ticks` | boolean | false | show tick / stop marks |
| `labeled` | boolean | false | show the value-label bubble |
| `range` | boolean | false | single ↔ two-thumb |
| `disabled` | boolean | undefined | |
| `name` / `form` / `labels` | — | — | HTML-form plumbing (N/A to Swing) |

A11y props: `ariaLabelStart`, `ariaValueTextStart`, `ariaLabelEnd`, `ariaValueTextEnd`.

Events: **`change`** (commit) and **`input`** (live drag). → Swing: a `ChangeListener` on the model covers both; may want a separate "value adjusting finished" signal (cf. `JSlider.getValueIsAdjusting()`).

Web theming tokens (for reference; we map to Elwha roles below): `--md-slider-active-track-color`, `--md-slider-inactive-track-color`, `--md-slider-handle-color`, plus `*-track-shape` / `*-handle-shape`.

## §C. MDC-Android anatomy + measurements (authoritative text)

Source: <https://raw.githubusercontent.com/material-components/material-components-android/master/docs/components/Slider.md> (fetched 2026-06-04). ⚠️ These are the **Android** redlines — **cross-check against the operator's M3 Tokens/Measurements screenshots before treating any dp as final** (the skill's measurement-cross-check rule).

**Anatomy (6 parts):** 1 value indicator (optional) · 2 stop indicators (optional) · 3 active track · 4 handle/thumb · 5 inactive track · 6 inset icon (optional).

**Measurements (Android defaults — VERIFY against screenshots):**

| Element | Attr | Default |
|---|---|---|
| Track thickness | `trackHeight` | **16dp** |
| Thumb width | `thumbWidth` | **4dp** (the tall pill) |
| Thumb height | `thumbHeight` | **44dp** |
| Thumb elevation | `thumbElevation` | 2dp |
| Thumb↔track gap | `thumbTrackGapSize` | **6dp** |
| Stop-indicator size | `trackStopIndicatorSize` | **4dp** |
| Track corner radius | `trackCornerSize` | trackHeight/2 (→ fully round) |
| Inside corner size | `trackInsideCornerSize` | 2dp |
| Min touch target | — | **48dp** |

**Color roles (Android):**

| Part | Android attr | → Elwha `ColorRole` |
|---|---|---|
| Active track | `colorPrimary` | `PRIMARY` |
| Inactive track | `colorSurfaceContainerHighest` | ⚠️ **CORRECTED → `SECONDARY_CONTAINER`** (M3 token sheet §T3 = `#E8DEF8`, not surface-container-highest; the Android attr disagrees with the M3 design token — trust the token sheet) |
| Handle/thumb | `colorPrimary` | `PRIMARY` |
| Tick — on active track | `colorSurfaceContainerHighest` | ⚠️ **superseded** → stop indicator (active) = `ON_PRIMARY` `#FFFFFF` (§T) |
| Tick — on inactive track | `colorPrimary` | ⚠️ **superseded** → stop indicator (inactive) = `ON_SECONDARY_CONTAINER` `#4A4458` (§Color) |
| Value label | `Widget.Material3.Tooltip` | ✅ **RESOLVED** (§TS4) → `INVERSE_SURFACE` `#322F35` container + `INVERSE_ON_SURFACE` `#F5EFF7` text, 12dp above handle (the deprecated PRIMARY label-container token is superseded) |

**Variants (Android):** Standard (single) · Centered (fill from zero-origin) · Range (two-thumb). Discrete via `stepSize`.

---

## §A. Overview (M3 page — screenshot #1)

Hero: *"Sliders let users make selections from a range of values."* Key bullets (read off the render, not just captions):

- `[DOC]` **Three variants: Standard, centered, range.**
- `[CODE]` **Five sizes, vertical *and* horizontal orientation, and an optional inset icon.** → sizes + orientation + inset-icon are real config axes, not just guidance.
- `[DOC]` Sliders should present the **full range** of available values.
- `[DOC]` The slider value should **take effect immediately** (live `input`, not deferred to commit).

Render (vertical example, "Bedroom Lights"): a **vertical** standard slider — tall rounded track, **inset bulb icon at the track top**, `PRIMARY` active fill at the **bottom** (vertical fills bottom-up), pill handle, inactive container above. Confirms: vertical orientation is first-class, and the inset icon sits **inside** the track at the far (max) end.

## §V. Variants & M3 Expressive naming (screenshot #2 — "M3 Expressive update", May 2025)

> *"The slider includes expressive configurations for orientation, shape sizes, and an inset icon. Updated on Android Views (MDC-Android) and Jetpack Compose."*

⚠️ **NAMING — load-bearing for the API (terminology must mirror M3's nouns):**

- `[CODE]` **`continuous` slider → renamed `standard` slider.**
- `[CODE]` **`discrete` slider → renamed the `stops` configuration.** (So "stops" is the noun for snap-to-step mode; the dots are "stop indicators".)

New M3 Expressive configurations:

- `[CODE]` **Orientation:** Horizontal, vertical.
- `[CODE]` **Optional inset icon — standard slider *only*.** (Not on centered/range.)
- `[CODE]` **Sizes:** **XS (existing default), S, M, L, XL** — five track-thickness sizes.

The three **variants** (numbered in the diagram):

| # | Variant | Fill behavior | Handles | Stop dots |
|---|---|---|---|---|
| ① | **Standard** | from leading edge | one | at trailing end |
| ② | **Centered** | from the **middle** (zero-origin) | one | both ends |
| ③ | **Range** | **between** the two handles | **two** | both ends |

## §V2. Variant render detail + availability table (screenshots #5, #6)

**Render (screenshot #5 — read off the pixels):** each variant shows a **gap on *both* sides of the handle** (the inactive→handle and handle→active breaks — the 6dp `thumbTrackGapSize`), and a **stop-indicator dot at the inactive far end(s)**:

- ① **Standard** — `PRIMARY` fill from the **left**, handle (vertical pill) mid-track, inactive container to its right, **stop dot at the right end**.
- ② **Centered** — handle near the midpoint, `PRIMARY` fill spanning from the **center** to the handle, inactive on both sides, **stop dots at both ends**.
- ③ **Range** — **two** handles, `PRIMARY` fill **between** them, inactive outside, **stop dots at both ends**.

**Availability table (screenshot #6 — verbatim):**

| Variant | M3 (baseline) | M3 Expressive |
|---|---|---|
| Standard | Available as **"continuous"** slider | Available |
| Centered | Available **(web only)** | Available |
| Range | Available | Available |
| Discrete | Available | Available as **"stops"** configuration |

Confirms the §V renames and adds: Centered was **web-only** in baseline M3 (now general in Expressive — fine for us, we follow Expressive); baseline "continuous" = Expressive "standard".

⚠️ **API-shape tension (flagged here, RESOLVED in §Cfg below):** this table lists **Discrete as a fourth row *alongside* Standard/Centered/Range**, but the May-2025 update (§V) called **"stops" a *configuration***. The **Configurations** page (§Cfg, screenshots #7–#9) settles it: M3 cleanly separates **3 variants** (Standard/Centered/Range) from **5 configuration axes** — stops/discrete is a **configuration**, not a variant. So the API models `stops`/`step` as a mode on every variant. ✅

## §R. Visual-refresh behaviors / motion (screenshot #3 — Dec 2023 "Previous updates")

Caption: *"Sliders have a stop indicator, larger label text, and a vertical handle that narrows when pressed. Centered sliders start from the middle instead of the leading edge."*

- `[CODE]` **Shape:** new track + handle shape; **slider elements change shape when selected** (handle/track morph on interaction).
- `[CODE]` **Motion — handle adjusts width on selection**; **the vertical pill handle narrows when pressed.** → this is the `ShapeMorphPainter` (#176) candidate: handle-width morph on press/drag.
- `[CODE]` **Motion — tracks adjust shape when the handle slides to the edge** (the active/inactive end caps reshape near the extremes).
- `[CODE]` **Value label:** larger text, bubble above the handle on press (render shows `-25`). Centered render confirms fill-from-middle.
- `[DOC]` Color mappings refreshed (the role set captured in §C / §Tokens).

**Architecture-insight running note:** the handle **narrows when pressed** (not the usual "grows") — opposite of the FAB/Button press-squish direction. Whatever drives it (ShapeMorphPainter or a bespoke width tween) must support shrink-on-press. Confirm exact resting vs pressed handle width on the Measurements/Tokens screenshots.

## §P. Terminology → API lock (running)

Operator rule: the API must mirror M3's exact nouns.

| M3 noun | Elwha API surface | Notes |
|---|---|---|
| **Standard** slider | `ElwhaSlider` variant `STANDARD` | was "continuous" |
| **Centered** slider | variant `CENTERED` | fill from origin |
| **Range** slider | variant `RANGE` | two handles |
| **Stops** configuration | `setStops(...)` / a `stops` mode (NOT "discrete") | snap-to-step; dots = "stop indicators" |
| **Stop indicator** | the dot primitive | — |
| **Value label** | `valueLabel` / `labeled` | bubble on press |
| **Inset icon** | `setInsetIcon(...)` (standard only) | inside track, max end |
| **Size** XS/S/M/L/XL | `ElwhaSlider.Size` enum, default `XS` | track-thickness scale |
| **Orientation** | `HORIZONTAL` / `VERTICAL` | M3 Expressive adds vertical |

## §Cfg. Configurations — the variant/config split (screenshots #7, #8, #9)

M3 separates two orthogonal axes. **This is the spine of the API.**

**Axis 1 — Variant (3, mutually exclusive):** `STANDARD` · `CENTERED` · `RANGE` (§V/§V2).

**Axis 2 — Configurations (5 independent toggles/scales), per the availability table:**

| Config | Values | Default | Expressive-only? | Notes |
|---|---|---|---|---|
| **Inset icon** | No / **Yes** | No | **Yes** | Standard-slider-only (§V). Render #8: glyph inset at the **leading edge inside the active track** (horizontal); at the max end for vertical (§A). `[CODE]` |
| **Orientation** | **Horizontal** / Vertical | Horizontal | **Vertical is Expressive-only** | Render #7. `[CODE]` |
| **Size** | **XS** / S / M / L / XL | XS | **S–XL Expressive-only** | Render #7: five track thicknesses; handle + corner scale with it. ⚠️ Off-Android the non-XS sizes are **token-swaps with no code preset** — we paint ourselves so we *can* expose all five, but XS is the canonical default. `[CODE]` |
| **Stop indicators** | No / Yes | No | — (was "discrete" in baseline) | Render #8: tick dots. = the `stops` mode. `[CODE]` |
| **Value indicator** | No / Yes | No | — | Render #8: "50" bubble above handle on press. = web `labeled`. `[CODE]` |

`[DOC]` Footnote (verbatim): *"Configurations only available using tokens don't have implemented presets in code. To change the size, swap the default size tokens md.comp.slider.xsmall.[…] with those of the desired size."* → off-Android, M3 itself ships only the XS preset; S–XL are token recipes. **Phasing signal:** XS-only is a legitimate Phase-1 surface; the size enum can be a later phase without diverging from M3's own code-level support.

**Inset-icon position lock:** inside the **active** track, at the track's **leading/origin end** (the end the fill grows *from*) — horizontal = left/start, vertical = the icon end shown in §A. Uses `MaterialIcons`, standard variant only.

## §T. Token sheet — Enabled (Default, Light) — VERBATIM (screenshots #10–#15)

> ⚠️ **Operator note: the caution-triangle icon = a DEPRECATED token.** Deprecated rows are the *old* discrete-slider / tick-mark / elevation styling, superseded by the current **stop-indicator** + flat-handle tokens. We follow the **current** (non-deprecated) set; deprecated values are recorded only for provenance.

### §T1 — Enabled / Stop indicator (current)
| Token | Value | → Elwha |
|---|---|---|
| Slider stop indicator size | **4dp** | — |
| Slider stop indicator shape | full (circle) | `ShapeScale.FULL` |
| Slider stop indicator trailing space | **4dp** | — |
| Slider stop indicator color | `#4A4458` | `ON_SECONDARY_CONTAINER` (⚠️ corrected from ON_SURFACE_VARIANT per §Color) |
| Slider stop indicator color **selected** | `#FFFFFF` | `ON_PRIMARY` |

### §T2 — Enabled / Container
**Current (no ⚠️):**
| Token | Value | → Elwha |
|---|---|---|
| Slider **active** stop indicator container color | `#FFFFFF` | `ON_PRIMARY` (white dot on PRIMARY fill) |
| Slider active stop indicator container opacity | **1** | — |
| Slider **inactive** stop indicator container color | `#4A4458` | `ON_SECONDARY_CONTAINER` (dark dot on SECONDARY_CONTAINER track; ⚠️ corrected per §Color) |
| Slider inactive stop indicator container opacity | **1** | — |

**⚠️ Deprecated (old "tick marks" / container — provenance only):**
- Slider with tick marks container size `2dp` · shape full · active container color `#FFFFFF` opacity `0.38` · inactive container color `#49454F` opacity `0.38`.
- Slider label container height `28dp` · label container color `#6750A4` (= **PRIMARY**) · label container elevation (none).
- Slider active container opacity `1` · inactive container opacity `1`.

> ⚠️ **Do NOT use the deprecated label-container `#6750A4`/PRIMARY for the value bubble.** The **current** value-indicator tokens (§TS4, "Pressed / Value indicator") are **`INVERSE_SURFACE` container + `INVERSE_ON_SURFACE` text** — the M3 tooltip styling. The deprecated 28dp/PRIMARY label-container token is superseded. See §TS4.

### §T3 — Enabled / Track (current)
| Token | Value | → Elwha |
|---|---|---|
| Slider active track height | **16dp** | — |
| Slider inactive track height | **16dp** | — |
| Slider active track shape | full | `ShapeScale.FULL` |
| Slider active track **outer** corner size | full (round) | the track's outer ends |
| Slider active track **inner** corner size | small (≈2dp rectangle) | the gap-side corner (squared) — matches §C `trackInsideCornerSize` 2dp |
| Slider inactive track shape | full | `ShapeScale.FULL` |
| Slider active track color | `#6750A4` | **`PRIMARY`** ✓ |
| Slider inactive track color | `#E8DEF8` | **`SECONDARY_CONTAINER`** ⚠️ (see correction) |
| *Slider track elevation* | *(none)* ⚠️DEP | flat |

### §T4 — Enabled / Handle (current)
| Token | Value | → Elwha |
|---|---|---|
| Slider handle height | **44dp** | — |
| Slider handle width | **4dp** | the resting pill |
| Slider handle shape | full (pill) | `ShapeScale.FULL` |
| Slider handle color | `#6750A4` | **`PRIMARY`** ✓ |
| Slider active handle color | `#6750A4` | **`PRIMARY`** ✓ |
| Slider active handle height | **44dp** | — |
| Slider active handle width | **4dp** | (= resting; see §T-note on narrows-on-press) |
| Slider active handle shape | full | `ShapeScale.FULL` |
| Slider active handle leading space | **6dp** | the handle↔track gap (= §C `thumbTrackGapSize`) |
| Slider active handle trailing space | **6dp** | same, other side |
| Slider active handle padding | **6dp** | gap around handle |
| *Slider handle elevation* | *(none)* ⚠️DEP | **flat handle** — no shadow |
| *Slider handle shadow color* | `#000000` ⚠️DEP | n/a (elevation deprecated) |

### §T-notes
- **⚠️ CORRECTION (inactive track):** earlier I mapped inactive track → `SURFACE_CONTAINER_HIGHEST` from the MDC-Android attr (`colorSurfaceContainerHighest`, §C). The **M3 design-token sheet says `#E8DEF8` = `SECONDARY_CONTAINER`.** Trust the token sheet → **inactive track = `SECONDARY_CONTAINER`.** Fixed in §C/§Tokens. (Cross-check artifact: Android theme attr ≠ M3 design token here.)
- **Stop-indicator color `#4A4458`:** ⚠️ **corrected** — the §Color role page names it `ON_SECONDARY_CONTAINER` (not `ON_SURFACE_VARIANT`, my hex-proximity guess). Symmetric with the track it sits on. Active stop dot `#FFFFFF` = `ON_PRIMARY` (clean).
- **Handle "narrows when pressed" (§R) not in this Enabled sheet** — resting AND "active" handle width are both **4dp** here. The narrower pressed width (if any) lives in a **Pressed** token group not yet captured. Flag: confirm resting-vs-pressed handle width when the states/pressed sheet arrives; provisional resting = 4dp×44dp.
- **Flat handle:** elevation + shadow tokens are deprecated → the M3 Expressive handle is **flat** (no `ShadowPainter`). Simplifies the primitive (no shadow-reserve insets needed for the handle).

## §TS. Token sheet — States (Default, Light) — VERBATIM (screenshots #16–#23)

> Same deprecation rule: ⚠️ rows are deprecated. The state-layer + stop tokens are flagged `[Deprecated]` in-sheet because pressed feedback is now a **ripple** (group "Pressed (ripple)") and tick→stop renamed — but the **opacity values are still the live numbers** and match our `StateLayer` enum.

### §TS1 — Hovered
| Token | Value | → Elwha |
|---|---|---|
| Slider hover state layer color | `#6750A4` ⚠️DEP | `PRIMARY` |
| Slider hover **state layer opacity** | **0.08** ⚠️DEP | `StateLayer.HOVER` (0.08 ✓) |
| Slider hover handle color | `#6750A4` ⚠️DEP | `PRIMARY` |
| Slider hover **handle width** | **4dp** | unchanged from rest |
| *Slider hover stop color* | `#6750A4` ⚠️DEP | — |

### §TS2 — Focused
| Token | Value | → Elwha |
|---|---|---|
| Slider focus active track color | `#6750A4` | `PRIMARY` |
| Slider focus inactive track color | `#E8DEF8` | `SECONDARY_CONTAINER` ✓ |
| Slider focus state layer opacity | **0.1** ⚠️DEP | `StateLayer.FOCUS` (0.10 ✓) |
| Slider focus handle color | `#6750A4` ⚠️DEP | `PRIMARY` |
| Slider focus **handle width** | **2dp** | ⬅ **narrows from 4dp** |
| *Slider focus stop color* | `#6750A4` ⚠️DEP | — |

### §TS3 — Pressed (ripple)
| Token | Value | → Elwha |
|---|---|---|
| Slider pressed state layer opacity | **0.1** ⚠️DEP | `StateLayer.PRESSED` (0.10) — **but feedback is now a `RipplePainter` ripple** |
| Slider pressed handle color | `#6750A4` | `PRIMARY` |
| Slider pressed **handle width** | **2dp** | ⬅ **narrows from 4dp** |
| Slider pressed active track color | `#6750A4` | `PRIMARY` |
| Slider pressed inactive track color | `#E8DEF8` | `SECONDARY_CONTAINER` ✓ |
| *Slider pressed stop color* | `#6750A4` ⚠️DEP | — |

### §TS4 — Pressed / Value indicator (CURRENT — the live value-bubble spec)
| Token | Value | → Elwha |
|---|---|---|
| Slider value indicator **container color** | `#322F35` | **`INVERSE_SURFACE`** (the tooltip look) |
| Slider value indicator label font | Roboto | → Elwha Inter (lib default) |
| Slider value indicator label **font color** | `#F5EFF7` | **`INVERSE_ON_SURFACE`** |
| Slider value indicator label line height | **20pt** | type metric |
| Slider value indicator label size | **14pt** | type metric |
| Slider value indicator label tracking | 0.5pt | type metric |
| Slider value indicator label weight | 400 | type metric |
| Slider value indicator **active bottom space** | **12dp** | gap between bubble and handle |

> Metrics 14/20/0.5/400 ≈ `TypeRole.BODY_MEDIUM`-ish (M3 calls slider value label a label style; closest Elwha role = `LABEL_LARGE`/`BODY_MEDIUM` — map to the nearest existing `TypeRole`, **no new type token**). Bubble = `INVERSE_SURFACE`/`INVERSE_ON_SURFACE`, 12dp above the handle.

### §TS5 — Disabled
| Token | Value | → Elwha |
|---|---|---|
| Slider disabled active track color | `#1D1B20` | `ON_SURFACE` |
| Slider disabled active track **opacity** | **0.38** | M3 disabled content |
| Slider disabled inactive track color | `#1D1B20` | `ON_SURFACE` |
| Slider disabled inactive track **opacity** | **0.12** | M3 disabled container |
| Slider disabled handle color | `#1D1B20` | `ON_SURFACE` |
| Slider disabled handle **opacity** | **0.38** | — |
| Slider disabled handle width | **4dp** | (not narrowed when disabled) |
| Slider disabled active stop indicator container color | `#F5EFF7` | light dot (≈`INVERSE_ON_SURFACE`/surface) on disabled active track |
| Slider disabled inactive stop indicator container color | `#1D1B20` | `ON_SURFACE` dark dot |
| *Slider disabled stop color active/inactive track* | `#F5EFF7` / `#1D1B20` ⚠️DEP | provenance |
| *Slider disabled handle elevation* | (none) ⚠️DEP | flat |

### §TS-summary — handle width by state (the morph spec)
| State | Handle width |
|---|---|
| Enabled (rest) | **4dp** |
| Hover | **4dp** |
| Focus | **2dp** |
| Pressed | **2dp** |
| Disabled | 4dp |

→ The handle **narrows 4dp→2dp on focus/press** (active interaction), returns to 4dp otherwise. Height stays 44dp throughout. This is the §R "narrows when pressed" behavior, quantified — drive it with a width tween (reduced-motion: snap, no tween).

## §An. Anatomy (screenshot #24) — VERBATIM part numbering

M3 numbers the six parts (matches §C):

| # | Part | Optional? | Elwha note |
|---|---|---|---|
| 1 | **Value indicator** | optional | the bubble (§TS4 inverse-surface), points **down onto the handle**, 12dp above |
| 2 | **Stop indicators** | optional | dots; on active track = `ON_PRIMARY`, on inactive = `ON_SECONDARY_CONTAINER` (§Color) — the "stops" config |
| 3 | **Active track** | — | `PRIMARY`, fills from origin to handle |
| 4 | **Handle** | — | 44×4dp pill (`PRIMARY`), narrows to 2dp on focus/press |
| 5 | **Inactive track** | — | `SECONDARY_CONTAINER` |
| 6 | **Inset icon** | optional | inside the active track at the **leading edge**; standard-variant only; `MaterialIcons` |

Render confirms: value indicator (#1) sits above and points at the handle; stop dots (#2) sit on **both** active and inactive segments; the inset icon (#6) is inside the active fill at the leading edge, with the handle just after it. **`[CODE]` all six** (the four non-optional are intrinsic; #1/#2/#6 are config toggles per §Cfg).

## §Color. Color overview — authoritative role names (screenshot #25)

> *"Slider color roles used for light and dark schemes"* — this page names the **roles** (the token sheets gave hexes; this is the semantic source of truth). Same roles light + dark.

| # | Role | Applied to | Elwha `ColorRole` |
|---|---|---|---|
| 1 | **Inverse surface** | value-indicator container | `INVERSE_SURFACE` |
| 2 | **Inverse on surface** | value-indicator label text | `INVERSE_ON_SURFACE` |
| 3 | **Primary** | active track | `PRIMARY` |
| 4 | **On primary** | stop indicator on **active** track | `ON_PRIMARY` |
| 5 | **Primary** | handle | `PRIMARY` |
| 6 | **Secondary container** | inactive track | `SECONDARY_CONTAINER` |
| 7 | **On secondary container** | stop indicator on **inactive** track | `ON_SECONDARY_CONTAINER` |
| 8 | **On secondary container** | (inactive stop, alt fill scenario) | `ON_SECONDARY_CONTAINER` |
| 9 | **On primary** | (active stop, alt fill scenario) | `ON_PRIMARY` |

**⚠️ CORRECTION (inactive stop indicator):** §T approximated the inactive stop dot (`#4A4458`) as `ON_SURFACE_VARIANT` from hex proximity to `#49454F`. **This page authoritatively names it `ON_SECONDARY_CONTAINER`** — which is the *semantically correct, symmetric* answer (dots are the "on" color of the track they sit on: active=PRIMARY→ON_PRIMARY, inactive=SECONDARY_CONTAINER→ON_SECONDARY_CONTAINER). `#4A4458` is just M3's generated on-secondary-container value in this scheme. **Fixed in §T/§Tokens.**

**Full role set is 7 distinct roles** — all existing Elwha `ColorRole`s: `PRIMARY`, `ON_PRIMARY`, `SECONDARY_CONTAINER`, `ON_SECONDARY_CONTAINER`, `INVERSE_SURFACE`, `INVERSE_ON_SURFACE` (+ `ON_SURFACE` for disabled). **Zero new tokens. Confirmed light + dark.**

## §S. States gallery (screenshots #26–#27) — light + dark

Five states, numbered legend verbatim:

| # | State | Render details (read off pixels) |
|---|---|---|
| 1 | **Enabled** | PRIMARY active / SECONDARY_CONTAINER inactive, 4dp handle, **no** value label |
| 2 | **Disabled** | both tracks greyed (`ON_SURFACE` 0.38/0.12), handle greyed, no label |
| 3 | **Hovered** | **pointer (hand) cursor**; handle + state layer (HOVER 0.08); no value label |
| 4 | **Focused** | **value label "50" bubble** above handle; handle **narrowed (2dp)** with a focus ring/outline around it |
| 5 | **Pressed** | **value label "50"**; **grab/fist cursor**; handle **narrowed (2dp)**; ripple |

`[CODE]` **Value label shows on focus + pressed**, hidden on enabled/hover/disabled (when the value-indicator config is on). `[CODE]` Cursor: pointer on hover, grab on press. Confirmed identical role behavior light + dark (§Color).

## §M. Measurements — VERBATIM size table (screenshots #28–#30)

Caption: *"Padding and size measurements for XS, S, M, L, and XL sliders."* The authoritative per-size table:

| Attribute | XS | S | M | L | XL |
|---|---|---|---|---|---|
| **Track height** | **16dp** | 24dp | 40dp | 56dp | 96dp |
| **Handle height** | **44dp** | 44dp | 52dp | 68dp | 108dp |
| **Handle width** | **4dp** | 4dp | 4dp | 4dp | 4dp |
| **Track shape** (outer corner radius) | **8dp** | 8dp | 12dp | 16dp | 28dp |
| **Inset icon size** | — | — | 24dp | 24dp | 32dp |
| **Label container height** | **44dp** (all sizes) | | | | |
| **Label container width** | **48dp** (all sizes) | | | | |

**Redline cross-checks (screenshots #28–#29) — all agree with §T:**
- Track height XS = **16dp** ✓ (= §T3). Handle width = **4dp** ✓ (= §T4). Handle↔track gap = **6dp** ✓ (= §T4 leading/trailing). Value-indicator bottom space = **12dp** ✓ (= §TS4).

**Notes / corrections:**
- ⚠️ **Value-indicator bubble = 44dp tall × 48dp wide** (constant across sizes). This **supersedes the deprecated `label container height 28dp`** token (§T2) — trust the measurements table (current). Handle morph never changes the bubble.
- **Inset icon only exists M/L/XL** (—for XS/S) — too small to inset below 40dp track. Standard-variant-only still holds (§Cfg).
- **"Track shape" is the outer corner radius, NOT always height/2**: XS 8dp (=full round on 16dp), but S–XL stay relatively smaller than half-height (24→8, 40→12, 56→16, 96→28) → larger sizes read as rounded-rectangles, not pills. Paint the corner per the table, not as `height/2`.
- **XS is the Phase-1 size** (track 16 / handle 44×4 / corner 8); the `Size` enum (S–XL) is the later-phase scale (§Open Q5).

## §G. Guidelines (screenshots #31–#33)

### Usage
- `[DOC]` Sliders select values along a track — ideal for **volume, brightness, image-filter intensity** (settings you adjust continuously).
- `[DOC]` Sliders can use **icons or labels** to represent a numeric or relative scale (the inset icon / value indicator).

### Immediacy (behavioral contract)
- `[CODE]` **"Changes made with sliders must take effect immediately, so people can understand the effects of their selection as they're moving the slider."** → fire **live updates continuously during drag** (web `input`), not only on release. The model's `ChangeListener` must fire on every value change mid-drag. A committed/`valueIsAdjusting=false` signal is *additional*, not a replacement (§Open Q9).

### When to use each variant
- `[DOC]` **Standard** — select one value from a range. Use when the slider **starts from zero or the beginning of a sequence**. (Horizontal + vertical both shown.)
- `[DOC]` **Centered** — select a value from a **positive/negative range**. Use when **zero (or the default) is in the middle**.
- `[DOC]` **Range** — select **two values** (a min + max). Use when defining a minimum and maximum.

### Orientation × variant constraint (Do / Don't)
- `[DOC]` ✓ **Do** — horizontal range slider.
- `[CODE]`-relevant `[DOC]` ✗ **Don't** — **avoid range sliders in vertical orientation** ("additional cognitive load … people are used to most sliders being horizontal"). Standard/centered are fine vertical; **range + vertical is discouraged**.
  - **Elwha stance (recommend):** **doc-warn, don't hard-block** — allow the combination but call it out in Javadoc/README (matches Elwha's no-nanny API doctrine). Since vertical is a later phase anyway (§Open Q6), this only bites when both vertical *and* range ship. Revisit at that phase.

## §GD. Element guidelines — Track & Handle (screenshots #34–#36)

*(Screenshot #34 re-shows the §An anatomy diagram — no new content.)*

### Track (screenshot #35)
- `[DOC]` The track shows the full range; two sections: **active** + **inactive**.
- `[CODE]` **Active** = from the **min value to the handle**; for **range**, active = **between the two handles**.
- `[CODE]` **Inactive** = from the handle to the max (or **outside** the two handles for range).
- `[CODE]` ⚠️ **RTL: "For LTR languages values increase left→right; for RTL this is reversed."** The slider must **mirror for right-to-left** locales — origin/active-fill direction flips. Swing: honor `ComponentOrientation.isLeftToRight()`. New implementation consideration (added to §Open).

### Handle (screenshot #36)
- `[DOC]` The handle moves along the track to choose a value.
- `[DOC]` **Two handles** → choose the **min and max** of a range (range variant).
- `[CODE]` **"The handle changes shape to indicate when it's pressed"** — confirms the narrows-on-press morph (§TS-summary: 4dp→2dp). Render caption: *"A handle changes shape when it's being pressed or dragged"* → applies to **press AND drag** (DRAGGED state too).

## §GD2. Value indicator config (screenshot #37)
- `[DOC]` The value displays the specific value at the handle's placement; appears while **pressing/dragging**.
- `[CODE]` **Range: only ONE value bubble at a time** — shows on the handle currently being interacted with.
- `[DOC]` If the value is shown elsewhere, the built-in indicator isn't required.
- `[CODE]` **External value field**: a separate text input can sit outside the slider; if so, **slider ↔ field auto-sync both ways**, and **Tab must reach that field directly after the slider**. → composes with **ElwhaTextField (#286)**; a Showcase recipe, and a focus-order contract.

## §GD3. Stop indicators config (screenshot #38)
- `[CODE]` Stop indicators mark predetermined values; **the handle snaps to the closest stop** (the "stops" config).
- `[DOC]` Avoid too many stops (visually crowded, hard to adjust).
- `[CODE]`/`[DOC]` **End stops exist to guarantee ≥3:1 contrast** of the inactive track vs background; **if the track already has 3:1, the end stops can be removed.** → an option: `endStopsVisible`/auto.
- `[DOC]` External **+/− icons or text** (left/right of the track) can indicate range — an accessibility aid, usable instead of stops.

## §GD4. Orientation & Inset icon configs (screenshots #39–#40)
- `[DOC]` **Orientation** — horizontal or vertical, per use case (standard shown both ways).
- `[CODE]` **Inset icon = standard sliders sized M/L/XL only.** *"Avoid adding inset icons to XS or S"* / Don't: **track thickness < 40dp** (so XS 16 / S 24 excluded; M 40 is the floor). Confirms §M (inset-icon size only M/L/XL).
- `[CODE]` **Icon repositions active→inactive track at low values** when there isn't room on the active side.
- `[DOC]` Consider **swapping the icon at zero** (volume → mute).
- `[CODE]` **Don't use inset icons on centered or range** ("unclear where the start is"). Standard-variant-only — enforce/doc.

## §GD5. Size guidance (screenshots #41–#42)
- `[DOC]` Sizes XS/S/M/L/XL — **larger = bigger target + more visual emphasis.**
- `[CODE]` **Active and inactive tracks are always the same height** (confirms §T3: both 16dp at XS).
- `[CODE]` Track heights: **XS 16 · S 24 · M 40 · L 56 · XL 96** (confirms §M).
- `[DOC]` **XL is for hero moments** — when the slider is the most important element on the page.

## §B. Behaviors — the interaction contract (screenshots #43–#44)
**These are all `[CODE]`.** Most are `JSlider`/`BoundedRangeModel`-native (architecture signal, §0):

| Gesture | Standard | With stops |
|---|---|---|
| **Select & drag** (drag the handle) | handle drags smoothly | snaps to closest stop while dragging |
| **Select jump** (click part of the track) | handle **moves to the clicked location** | handle moves to the **closest stop** |
| **Keyboard — Tab** | focus lands on the handle | same |
| **Keyboard — Arrows** | value ±**one step** | value ±**one stop** |
| **Keyboard — Space + Arrows** | value ±**a larger interval** (page step) | ±a larger stop interval |

→ **Click-to-jump, arrow-stepping, and page-stepping are exactly `BoundedRangeModel` + `JSlider` keyboard semantics.** Reinforces borrowing the model/keyboard even under architecture (A) (custom paint, `BoundedRangeModel` backing). Snap-to-stop = `JSlider` `snapToTicks`. RTL flips arrow direction (§GD).

## §X. Accessibility (screenshots #45–#50)

### Use cases (#45)
`[DOC]` Via assistive tech, people must be able to: **navigate to a slider** · **control a handle along a track to select a value/range** · **get appropriate feedback based on input type.**

### Interaction feedback (#46)
- `[CODE]` On press/drag the **handle shrinks in width AND the value appears** — the dual cue that the handle is active. (Touch: tap/drag → shrink + value. Cursor: hover → cursor change; click+drag → shrink + value.) Matches §TS-summary (4dp→2dp) + §S (value on focus/press).

### Focus & navigation (#47)
- `[CODE]` **Initial focus lands directly on the handle** (the primary interactive element). Then arrows/keyboard adjust.

### Keyboard navigation (#49) — VERBATIM table (authoritative; extends §B)
| Keys | Action |
|---|---|
| **Tab** | Moves focus to the slider handle |
| **Arrows** | ± the value by **one value or one stop indicator** |
| **Space & Arrows** | ± the value by **one interval or one stop indicator** (larger step) |
| **Home / End** | **Set the slider to the first / last value** (min / max) ← *new vs §B* |

→ This is the **exact `JSlider` / `BasicSliderUI` keymap** (arrows, Home/End, block increment). Swing gives it free for a single slider.

### Color contrast (#48)
- `[CODE]`/`[DOC]` The **end of the inactive track must have ≥3:1 contrast with the background.** The **end stop indicator** guarantees this on low-contrast backgrounds; **external +/− icons** with 3:1 are an alternative. (This is the *raison d'être* of end stops — §GD3.)

### Labeling & roles (#50)
- `[CODE]` The slider's accessible **role = "slider"** (Swing: `AccessibleRole.SLIDER`).
- `[CODE]` Accessible **name = the adjacent UI label text** (e.g. "Brightness"); screenreader reads *label → role*. Expose a `setLabel`/`labelFor`-style hook or honor an associated `JLabel`.
- `[CODE]` **External icon buttons** (the +/− affordances) get the **Button role** — they're `ElwhaIconButton`s, not part of the slider's a11y node.
- `[CODE]` Expose `AccessibleValue` (current / min / max) for "feedback based on input type."

### §X-arch — the a11y verdict for the architecture fork (feeds §0)
**Single slider:** `JSlider` already provides `AccessibleRole.SLIDER`, `AccessibleValue`, the full keymap (#49), and focus-on-handle — **all free.** **Range slider:** two thumbs are *not* a `JSlider` concept, so range a11y (two foci, per-handle value text, web's `aria-*-start/end`) is **custom regardless.** → Mirrors the text-field lesson: backing the single slider with `JSlider`/`BoundedRangeModel` buys a lot; range forces custom either way. **Recommend: single = JSlider-or-BoundedRangeModel-backed; range = custom two-value model with hand-wired a11y.** Lock in the S1 spike.

## §Tokens. Elwha zero-new-tokens mapping (running)

Goal per Elwha doctrine: **map every M3 need onto an existing token — zero new theme tokens.** Status so far — **looks fully coverable**:

| M3 need | Elwha token | Confidence |
|---|---|---|
| Active track fill | `ColorRole.PRIMARY` (`#6750A4`) | **confirmed §T3** |
| Handle fill | `ColorRole.PRIMARY` (`#6750A4`) | **confirmed §T4** |
| Inactive track fill | ⚠️ `ColorRole.SECONDARY_CONTAINER` (`#E8DEF8`) | **CORRECTED §T3** (was surface-container-highest) |
| Stop indicator — on active track | `ColorRole.ON_PRIMARY` (`#FFFFFF`) | **confirmed §T1/§T2** |
| Stop indicator — on inactive track | `ColorRole.ON_SECONDARY_CONTAINER` (`#4A4458`) | **confirmed §Color** (corrected from ON_SURFACE_VARIANT) |
| Handle hover/focus/press/drag state layer | `StateLayer.HOVER` / `FOCUS` / `PRESSED` / `DRAGGED` | **exact match** |
| Handle/track shape | `ShapeScale.FULL` (pill + round track ends) | confirmed §T |
| Track inner (gap-side) corner | ≈2dp (small) — not a clean ShapeScale step | §T3; paint as ~2dp literal |
| Value-label bubble container | ⚠️ `ColorRole.INVERSE_SURFACE` (`#322F35`) | **CORRECTED §TS4** (was PRIMARY/deprecated) |
| Value-label bubble text | `ColorRole.INVERSE_ON_SURFACE` (`#F5EFF7`), ~`BODY_MEDIUM` | **§TS4**; bubble 44×48dp, 12dp above handle (§M) |
| Disabled active track / handle | `ON_SURFACE` @ **0.38** | **confirmed §TS5** |
| Disabled inactive track | `ON_SURFACE` @ **0.12** | **confirmed §TS5** |
| Handle width morph | 4dp→**2dp** on focus/press, bespoke width tween | **confirmed §TS-summary** |
| Hover/focus/press state-layer opacity | `StateLayer` HOVER 0.08 / FOCUS 0.10 / PRESSED 0.10 (press = ripple) | **confirmed §TS** |
| Handle shadow | **none** — flat (elevation deprecated §T4) | confirmed |

**Zero new tokens holds.** Every role lands on an existing `ColorRole` (`PRIMARY`/`ON_PRIMARY`/`SECONDARY_CONTAINER`/`ON_SURFACE_VARIANT`/`ON_SURFACE`) + `StateLayer` + `ShapeScale.FULL`. The only non-token literal is the ~2dp track inner-corner radius (a paint constant, not a theme token).

---

## §Open. Open questions

> **✅ RESOLVED in Phase C (2026-06-05) — operator accepted all recommendations.** Locks: (A1) unified `ElwhaSlider extends JComponent` + `BoundedRangeModel`, S1-spike-locked; (A2) Standard P1 · Centered P2 · Range P3, all V1; (A3) P1 = horizontal, XS, continuous+stops, value indicator; inset icon / sizes / vertical / centered / range = later phases; (B4) vertical+range doc-warn; (B5) live `ChangeListener` + `getValueIsAdjusting()`; (C6) terminology lock per §P; (C7) v0.4.0. Decisions live in [`elwha-slider-design.md`](elwha-slider-design.md). Original questions retained below for provenance.

1. **Architecture (A) custom-`JComponent` vs (B) `JSlider`+`SliderUI`** — provisional (A); lock via S1 spike. *(See §0.)*
2. ✅ **RESOLVED — Value-label bubble** = `INVERSE_SURFACE` container + `INVERSE_ON_SURFACE` text, ~`BODY_MEDIUM`, 12dp above handle (§TS4). The "tooltip style" = inverse-surface. No new token.
3. **V1 variant split — Standard / Centered / Range.** All three are first-class M3 variants (§V). Decide the V1/phase/V2 split: likely **Standard V1 Phase 1**, **Centered + Range later V1 phases** (Range forces two-thumb model → bigger lift). Get the operator's explicit split (V ≠ Phase).
4. **Stops configuration (formerly "discrete") in V1?** — core M3 anatomy (stop indicators); likely yes, confirm phase.
5. ✅ **Sizes XS/S/M/L/XL — exact dp now captured** (§M table: track 16/24/40/56/96, handle 44/44/52/68/108, corner 8/8/12/16/28, inset icon —/—/24/24/32). XS = the Enabled default. **Recommend XS-only Phase 1 + a `Size` enum later phase** (M3-faithful: only XS has a code preset off-Android). Still *open only on the phasing decision* (operator confirms split).
6. **Orientation Horizontal/vertical** — vertical is **Expressive-only but first-class** (§A render, §Cfg). Decide V1-Phase-1 (horizontal) vs vertical a later phase. Architecture (A) makes vertical our own paint either way; horizontal-first is a reasonable Phase 1.
7. **Inset icon** — **position resolved** (§Cfg: leading/origin end inside the active track; standard-only, `MaterialIcons`). Open only on *phasing* (V1 later phase vs deferred stub).
8. ✅ **RESOLVED — Handle width morph** = 4dp→2dp on focus/press, height constant 44dp (§TS-summary). Bespoke width tween (not `ShapeMorphPainter`, which morphs *corner shape* not width); reduced-motion snaps.
9. **`getValueIsAdjusting()` equivalent** — expose a "drag in progress" signal distinct from committed change? (web's `input` vs `change`). Still open.
10. **Pressed = ripple** (§TS3 group "Pressed (ripple)") — handle press shows a `RipplePainter` ripple, hover/focus use static `StateLayer`. Confirm ripple bounds (handle-sized vs touch-target-sized) on a behavior/states render.
11. **RTL mirroring** (§GD Track) — values increase L→R in LTR, reversed in RTL. Honor `ComponentOrientation` in the paint + hit-testing. In scope for V1 (cheap if designed in from the start; expensive to retrofit). Recommend: build RTL-aware from S1.

---

## §F. Screenshot log

| # | M3 page | Captured | Section appended |
|---|---|---|---|
| 1 | Overview (hero; Sound-settings example: Call/Alarm/Ring/Media volume, single-point standard, primary active track + pill handle + end stop-dot) | 2026-06-04 | header / §0 |
| 2 | Overview key points + **vertical** "Bedroom Lights" render (inset bulb icon, bottom-up fill) | 2026-06-04 | §A |
| 3 | "M3 Expressive update" May 2025 — variants/naming (continuous→standard, discrete→stops), orientation, inset icon, sizes XS–XL; ①②③ variant diagram | 2026-06-04 | §V, §P |
| 4 | "Previous updates" Dec 2023 refresh — stop indicator, larger label, handle narrows-on-press, centered-from-middle ("-25" render) | 2026-06-04 | §R |
| 5 | **Variants** page render (①②③ Standard/Centered/Range, both-side handle gaps + stop dots) | 2026-06-04 | §V2 |
| 6 | Variant **availability table** (M3 vs M3 Expressive; continuous→standard, discrete→stops, centered web-only→general) | 2026-06-04 | §V2 |
| 7 | **Configurations** page — orientation (H/V) + size (XS–XL, five thicknesses) renders | 2026-06-04 | §Cfg |
| 8 | **Configurations** page — inset icon / stops (ticks) / value indicator ("50") renders | 2026-06-04 | §Cfg |
| 9 | **Configurations availability table** (verbatim: inset/orientation/size/stops/value-indicator × M3 vs Expressive + size-token footnote) | 2026-06-04 | §Cfg |
| 10–15 | **Tokens / Enabled** — Stop indicator · Container (+deprecated tick-marks/label) · Track · Handle (2 sheets) | 2026-06-05 | §T1–§T4 |
| 16–17 | **Tokens / Disabled** — Stop · Track · Handle | 2026-06-05 | §TS5 |
| 18 | **Tokens / Hovered** — deprecated stop + state layer (0.08) + handle (4dp) | 2026-06-05 | §TS1 |
| 19–20 | **Tokens / Focused** — track + state layer (0.1) + handle (**2dp**) | 2026-06-05 | §TS2 |
| 21–23 | **Tokens / Pressed (ripple)** — state layer + handle (**2dp**) + track + stop + **Value indicator** (inverse-surface bubble) | 2026-06-05 | §TS3, §TS4 |
| 24 | **Anatomy** — 6-part numbered diagram (value indicator / stop indicators / active track / handle / inactive track / inset icon) | 2026-06-05 | §An |
| 25 | **Color** — 9 role callouts (light + dark): inverse-surface bubble, primary track/handle, on-primary + on-secondary-container stops, secondary-container inactive track | 2026-06-05 | §Color |
| 26–27 | **States** gallery + legend (Enabled/Disabled/Hovered/Focused/Pressed; value label on focus+pressed; cursors) light + dark | 2026-06-05 | §S |
| 28–29 | **Measurements** redlines (track 16 / handle 4 / gap 6 / bottom-space 12) + per-size XS–XL render | 2026-06-05 | §M |
| 30 | **Measurements table** (verbatim: track/handle/corner/inset-icon/label-container by XS–XL) | 2026-06-05 | §M |
| 31 | **Usage** guidelines (what sliders are for; icons/labels) | 2026-06-05 | §G |
| 32 | **Immediacy** + variant intro (standard, live updates, H/V standard) | 2026-06-05 | §G |
| 33 | **Centered/Range** guidance + Do/Don't (no vertical range) | 2026-06-05 | §G |
| 34 | **Anatomy** (repeat of #24 — no new content) | 2026-06-05 | §GD (noted) |
| 35 | **Track** element guideline (active/inactive sections + **RTL** mirroring) | 2026-06-05 | §GD |
| 36 | **Handle** element guideline (move/two-handle/changes-shape-on-press) | 2026-06-05 | §GD |
| 37 | **Value** config (one-bubble-at-a-time, external text-field sync + tab order) | 2026-06-05 | §GD2 |
| 38 | **Stop indicators** config (snap, crowding, end-stops-for-contrast, external +/−) | 2026-06-05 | §GD3 |
| 39 | **Orientation + Inset icon** (M/L/XL only, <40dp don't, repositioning, swap-at-zero) | 2026-06-05 | §GD4 |
| 40 | **Inset icon Don'ts** (not on centered/range) | 2026-06-05 | §GD4 |
| 41 | **Size** guidance (larger=emphasis; active==inactive height; XS–XL track heights) | 2026-06-05 | §GD5 |
| 42 | **XL hero** moments | 2026-06-05 | §GD5 |
| 43 | **Behaviors** — Select & drag / Select jump (click-to-jump, snap) | 2026-06-05 | §B |
| 44 | **Behaviors** — Select & arrow (Tab/Arrows/Space+Arrows keyboard) | 2026-06-05 | §B |
| 45 | **A11y — Use cases** (navigate / control handle / feedback by input type) | 2026-06-05 | §X |
| 46 | **A11y — Interaction & style** (handle shrinks + value appears; touch/cursor) | 2026-06-05 | §X |
| 47 | **A11y — Focus & navigation** (focus on handle, arrows adjust) | 2026-06-05 | §X |
| 48 | **A11y — Color contrast** (≥3:1 end via stop indicator / external icons) | 2026-06-05 | §X |
| 49 | **A11y — Keyboard table** verbatim (Tab/Arrows/Space+Arrows/**Home-End**) | 2026-06-05 | §X |
| 50 | **A11y — Labeling** (role=slider, name=adjacent label, external buttons=Button role) | 2026-06-05 | §X |

> **Awaiting operator screenshots** for: Accessibility. Phase B transcribes verbatim. *(Everything else — tokens, anatomy, color, states, measurements, guidelines — is captured. Only a11y remains before Phase C synthesis.)*
