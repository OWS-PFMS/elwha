# ElwhaSlider — Design Decisions

**Status:** **Phase 0 — design draft for review.** Decisions below are proposed; the load-bearing **model/paint-boundary** call (§2) is locked by the first implementation story's spike. No stories filed until this doc is approved.

**Drafted:** 2026-06-05. **Author:** Charles Bryan (`cfb3@uw.edu`).

**Parents:**
- [`elwha-slider-research.md`](elwha-slider-research.md) — the full M3 spec capture this doc decides against. **Read it for any anatomy/token/measurement/behavior/a11y detail; this doc references rather than duplicates it.**
- [`elwha-dialog-design.md`](elwha-dialog-design.md) / [`elwha-textfield-design.md`](elwha-textfield-design.md) — the design-doc shape + the S1-spike-locks-the-architecture precedent this epic mirrors.
- [`ElwhaNavRailDestination`](../../src/main/java/com/owspfm/elwha/navrail/ElwhaNavRailDestination.java) — the `extends JComponent` dedicated-primitive template.
- [M3 Sliders](https://m3.material.io/components/sliders) + Material Web `slider.md` + MDC-Android `Slider.md`.

**Epic:** [#340](https://github.com/OWS-PFMS/elwha/issues/340) (V1). **Milestone:** v0.4.0 (the active dev milestone).

---

## TL;DR — the locked decisions

1. **What it is:** `ElwhaSlider` — the M3 **Expressive slider**: a token-themed range input with a pill handle, split active/inactive track, optional stop indicators + value bubble + inset icon. The range-input primitive Elwha lacks (7 internal raw-`JSlider` sites + the OWS consumer).
2. **One component, two orthogonal axes (§Cfg):** **Variant** (mutually exclusive) `STANDARD` / `CENTERED` / `RANGE`; **configurations** (independent) inset icon · orientation (H/V) · size (XS–XL) · **stops** · value indicator. M3 Expressive nouns (continuous→**standard**, discrete→**stops**) — the API mirrors them ([[project_elwha_m3_expressive]]; research §P).
3. **Architecture (load-bearing, §2):** **one `ElwhaSlider extends JComponent`** painting all M3 chrome, backed by a `BoundedRangeModel` (single) / two-value model (range); keyboard + `AccessibleRole.SLIDER` + `AccessibleValue` **hand-wired once**. *Not* a `JSlider` subclass, *not* a `SliderUI` delegate. Justified because **range forces a custom component regardless** and the keyboard/a11y surface is **finite and fully captured** (research §B/§X). **Final boundary locked by the §11 S1 spike.**
4. **Zero new theme tokens (LOCKED).** Every color/shape/type need maps onto existing `ColorRole`/`StateLayer`/`ShapeScale`/`TypeRole` — table in research §Tokens/§Color.
5. **No overlay host.** Like the text field (and unlike `ElwhaMenu`/`ElwhaDialog`), the slider is an **embeddable inline component** — no `JLayeredPane`, no z-band. The **value bubble** is painted by the component within its own bounds (reserve space above the track), not a popup.
6. **Color (LOCKED, research §Color):** active track + handle = **PRIMARY**; inactive track = **SECONDARY_CONTAINER**; stop-on-active = **ON_PRIMARY**, stop-on-inactive = **ON_SECONDARY_CONTAINER**; value bubble = **INVERSE_SURFACE** + **INVERSE_ON_SURFACE**; disabled = **ON_SURFACE** @ 0.38 (active/handle) / 0.12 (inactive). Confirmed light + dark.
7. **Geometry XS (LOCKED, research §M/§T):** track **16dp**; handle **44×4dp** pill, **flat (no shadow)**; **6dp** handle↔track gap; stop **4dp**; track outer corner full / **inner ~2dp**; value bubble **44×48dp**, **12dp** above the handle. Active and inactive track heights are always equal.
8. **Interaction (LOCKED, research §S/§TS/§B):** handle **narrows 4dp→2dp on focus/press** (44dp height constant); **pressed = `RipplePainter` ripple**; hover/focus = static `StateLayer` (0.08 / 0.10); **value bubble on focus + press**; **live value updates during drag**.
9. **Behaviors + keyboard (LOCKED, research §B/§X):** drag (smooth / snap-to-stop); **click-to-jump**; Tab→handle, Arrows ±step/stop, Space+Arrows ±interval, **Home/End → min/max**. **RTL mirrors** the fill direction — built in from S1.
10. **A11y (LOCKED, §8):** role **slider**, `AccessibleValue` (current/min/max), name = adjacent label; external +/− = `ElwhaIconButton` (Button role). End stops satisfy the **≥3:1 contrast** requirement.
11. **V/Phase split (LOCKED, §11):** **V1 (#340)** = Standard (P1) → Centered (P2) → Range (P3) + the config/size/orientation phases. Each variant/axis is a phase *within* V1.
12. **Out of scope / deferrals (no silent cuts):** **vertical + range** = doc-warn only (research §G); **external value-field** = a Showcase recipe composing **ElwhaTextField #286** (research §GD2); inset icon couples to size (M/L/XL only). Centered/origin beyond zero, custom tick labels → documented deferrals.

---

## §1. Scope — what V1 ships (across phases)

✅ `STANDARD` · `CENTERED` · `RANGE` variants · horizontal + vertical orientation · sizes XS–XL · continuous + **stops** (snap) · **value indicator** bubble · **inset icon** (standard, M/L/XL) · stop indicators incl. contrast end-stops · full token theming + dark mode · handle narrow-on-press morph + ripple + state layers · click-to-jump + full keyboard (arrows/Space/Home/End) · RTL · `AccessibleValue`/slider role · Showcase leaf.

❌ **Vertical range** → doc-warn (allowed, discouraged) · **external value-field sync** → Showcase recipe (composes #286), not a built-in · custom tick/value-label formatting beyond a `valueFormatter` hook → documented deferral · density → out (M3 says don't-apply-by-default, research §Cfg footnote).

**Phase-1 surface specifically:** `STANDARD`, **horizontal**, **XS**, continuous **+ stops**, **value indicator**. Everything else is a later V1 phase (§11).

## §2. Architecture — the load-bearing decision [RECOMMENDED; lock via S1 spike]

**One `ElwhaSlider extends JComponent`** that paints all M3 chrome and owns interaction, backed by a value model:
- **Single (`STANDARD`/`CENTERED`)** — a `javax.swing.BoundedRangeModel` (`DefaultBoundedRangeModel`) holds value/min/max/extent; we add `step` (snap) on top.
- **Range** — a small two-value model (`lowerValue`/`upperValue` over the same min/max); **not** a `JSlider` concept.

Elwha owns: **chrome paint** (active/inactive track with the 6dp gap + 2dp inner corner, pill handle, stop dots, inset icon, value bubble) · **state layers + ripple** (`StateLayer` + `RipplePainter`) · **the handle width morph** (4dp→2dp tween) · **keyboard** (arrows/Space+arrows/Home/End, RTL-aware) · **mouse** (drag, click-to-jump, snap) · **a11y** (`AccessibleRole.SLIDER`, `AccessibleValue`, name).

**Why not `JSlider` + `SliderUI`?** `BasicSliderUI`'s track/thumb layout fights the M3 split-track-with-gap geometry, and **range is not a `JSlider` concept** — so a `SliderUI` path still needs a separate custom range component (two code paths for one family). **Why not from-scratch with no model?** `BoundedRangeModel` is the natural, reusable value/step/clamp holder and costs nothing to adopt. The unified custom path keeps single + range on one paint/interaction codebase.

**S1 spike (first story):** prototype the single horizontal standard slider — `BoundedRangeModel`-backed, paint the M3 chrome (track + gap + pill handle + stop dots), and prove (a) token theming + dark mode, (b) hover/focus state layers + **press ripple** + **handle 4→2dp morph**, (c) drag + **click-to-jump** + **keyboard** (arrows/Home/End) drive the model, (d) `AccessibleRole.SLIDER` + `AccessibleValue` read correctly under a screen reader, (e) **RTL** mirrors fill + arrows. Mirrors the dialog/text-field S1 precedent. Documented fallback: if hand-wiring keyboard/a11y proves heavier than expected for the *single* case, evaluate borrowing `JSlider` internally for single only — but default to the unified path.

## §3. Anatomy / primitive

Per research §An (6 parts): value indicator (1) · stop indicators (2) · active track (3) · handle (4) · inactive track (5) · inset icon (6). The component paints all six within its own bounds; reserve vertical space above the track for the value bubble (44×48dp, 12dp gap). Inset icon (`MaterialIcons`, 24/32dp) lives inside the active track at the leading/origin end (standard variant, M/L/XL only) and **repositions to the inactive track at low values** (research §GD4). Leading/origin end + fill direction respect `ComponentOrientation` (RTL).

## §4. Tokens & color [zero new tokens — LOCKED]

Full role table in research §Color / §Tokens. Summary: PRIMARY (active track, handle), SECONDARY_CONTAINER (inactive track), ON_PRIMARY (stop on active), ON_SECONDARY_CONTAINER (stop on inactive), INVERSE_SURFACE + INVERSE_ON_SURFACE (value bubble), ON_SURFACE @ 0.38/0.12 (disabled), `StateLayer.HOVER` 0.08 / `FOCUS` 0.10 (handle), `RipplePainter` (press), `ShapeScale.FULL` (pill + track ends; inner corner ~2dp painted literal). Value-label text ≈ `TypeRole.BODY_MEDIUM` (nearest existing role — no new type token).

## §5. Measurements & sizes

XS is the Phase-1 size (research §M): track 16 · handle 44×4 · gap 6 · stop 4 · corner 8(outer)/2(inner) · bubble 44×48 / 12dp. The `Size` enum {`XS`,`S`,`M`,`L`,`XL`} (track 16/24/40/56/96, handle-H 44/44/52/68/108, corner 8/8/12/16/28, inset-icon —/—/24/24/32) is a **later phase** (M3 ships only the XS code preset off-Android — research §Cfg). Active and inactive track heights are always equal.

## §6. States & motion

enabled · hover · focus · pressed/dragged · disabled (research §S/§TS). Handle **narrows 4dp→2dp on focus + press** via a short width tween (reduced-motion: snap). Hover/focus = static `StateLayer`; **press = `RipplePainter` ripple** on the handle. Value bubble fades/scales in on focus+press (reduced-motion: snap). Flat handle — no `ShadowPainter`.

## §7. Behaviors & keyboard

Per research §B/§X (authoritative keymap in §X #49): **drag** (smooth; snap-to-stop in stops mode) · **click-to-jump** (handle moves to click, or nearest stop) · **Tab**→handle · **Arrows** ±one step/stop · **Space+Arrows** ±interval (block increment) · **Home/End**→min/max. Live `ChangeListener` fires on every value change during drag; **`getValueIsAdjusting()`** distinguishes drag-in-progress from commit (mirrors `JSlider` + web `input`/`change`). All horizontal directions mirror under RTL.

## §8. Accessibility

Role **`AccessibleRole.SLIDER`**; expose **`AccessibleValue`** (current/min/max). Accessible **name = adjacent label** (a `setLabel`/`labelFor`-style hook or associated `JLabel`); screen reader reads label → role → value. **Range:** two foci (one per handle) with per-handle value text (web `aria-*-start/end` analogue) — custom, built in the Range phase. **External +/− affordances** are `ElwhaIconButton`s (Button role), outside the slider's a11y node. **End stop indicators** guarantee the inactive-track end ≥3:1 contrast (research §X #48).

## §9. Showcase pattern

A slider **is** an embeddable surface → standard `ComponentWorkbench` + `Gallery` (research §9-style). Stage controls: variant (standard/centered/range) · orientation · size · stops on/off + step · value-indicator on/off · inset-icon on/off · disabled. Gallery: variant × state matrix. **Dogfood** ([[feedback_dogfood_elwha_components]]): migrate the 7 internal raw-`JSlider` sites (`card/v1/playground/LiveConfigPanel`, `ElwhaCardListShowcase`) onto `ElwhaSlider` as the dogfood pass. The **external value-field recipe** (slider ↔ `ElwhaTextField` #286 two-way sync, Tab-after-slider) is a Showcase demo, not a built-in.

## §10. Out of scope (filed/deferred, not silently cut)

- **Vertical + range** — allowed but **doc-warned** (M3 cognitive-load guidance, research §G).
- **External value-field sync** — Showcase recipe composing #286, not a library API.
- **Custom value/tick label formatting** beyond a `valueFormatter` hook — documented deferral.
- **Density** — out (M3 says don't-apply-by-default).
- No new follow-on epic anticipated (unlike the text field's exposed-dropdown) — the slider is self-contained.

## §11. Phasing → stories

All phases are **V1 (epic #340)**; hand off at phase boundaries ([[feedback_phase_handoff_cadence]]). **Default to filing Phase-1 stories only**; later-phase stories filed when Phase 1 lands.

**Phase 1 — single standard horizontal XS slider (the critical path):**
- **S1 — architecture spike + chrome skeleton** (`JComponent` + `BoundedRangeModel`, paint track+gap+pill handle+stops, token theme + dark mode). *Locks §2.*
- **S2 — interaction & motion** (drag, click-to-jump, state layers, **press ripple**, **handle 4→2dp morph**, reduced-motion).
- **S3 — keyboard + a11y** (arrows/Space/Home/End, RTL, `AccessibleRole.SLIDER` + `AccessibleValue`, name hook).
- **S4 — stops config + value indicator** (snap-to-stop, stop dots incl. contrast end-stops, value bubble on focus/press).
- **S5 — Showcase leaf** (Workbench + Gallery; dogfood the 7 raw-`JSlider` sites).
- **S6 — docs** (README + CHANGELOG + Javadoc `@author/@version/@since`; terminology mirrors §P).

**Later V1 phases (file when P1 lands):**
- **Phase 2 — Centered variant** (origin-from-middle fill).
- **Phase 3 — Range variant** (two-value model, two-thumb a11y, one-bubble-at-a-time, vertical-range doc-warn).
- **Phase 4 — Sizes** (`Size` enum S–XL) **+ inset icon** (M/L/XL, repositioning, swap-at-zero) — coupled (inset icon needs ≥40dp track).
- **Phase 5 — Vertical orientation** (standard/centered; range stays doc-warned).

## §12. Open for the S1 spike

- Whether to borrow `JSlider` internally for the **single** case (vs fully hand-wired) — default unified custom; spike confirms.
- Ripple bounds on the handle (handle-sized vs a touch-target halo) — research §Open Q10.
- Exact handle width-morph easing/duration (eye-confirm in the spike, research §TS-summary).
