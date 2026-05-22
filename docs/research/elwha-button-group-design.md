# ElwhaButtonGroup — Design Decisions

**Status:** LOCKED — scope fixed, all §18 decisions resolved. Ready for the Phase 1 / Phase 2
build. Epic [#170](https://github.com/OWS-PFMS/elwha/issues/170); Phase stories
[#171](https://github.com/OWS-PFMS/elwha/issues/171)–[#176](https://github.com/OWS-PFMS/elwha/issues/176).

**Drafted:** 2026-05-21

**Author:** Charles Bryan (`cfb3@uw.edu`).

**Parents:**
- [`elwha-button-design.md`](elwha-button-design.md) — the `ElwhaButton` primitive `ElwhaButtonGroup`
  composes; Phase 1 extends its corner rendering.
- [`elwha-icon-button-design.md`](elwha-icon-button-design.md) — the `ElwhaIconButton` primitive,
  likewise composed and extended.
- [`elwha-showcase-design.md`](elwha-showcase-design.md) — Phase 5 wires the connected group into
  The Elwha Showcase.
- [M3 Button groups (Expressive)](https://m3.material.io/components/button-groups/overview) — the
  spec being adapted.
- [M3 Segmented buttons](https://m3.material.io/components/segmented-buttons/overview) — the
  deprecated component the connected variant replaces.

**Epic:** [#170](https://github.com/OWS-PFMS/elwha/issues/170) — `epic: M3 Button Group component
(ElwhaButtonGroup)`.

**Target milestone:** v0.3.0.

---

## TL;DR

1. **What it is:** `ElwhaButtonGroup` — the M3 Expressive **Button group**: a visual + layout +
   selection container that composes `ElwhaButton` / `ElwhaIconButton`. Two variants — **standard**
   (gapped toolbar cluster) and **connected** (butted segments, a selection control).
2. **Why it earns v0.3.0:** the connected variant replaces M3's deprecated Segmented Button, and
   OWS has identified concrete uses (the showcase light/dark/system toggle and primary/secondary
   theme-tier picker). Library-completeness work — not release-driving.
3. **Posture:** tracks **M3 Expressive**. Button groups are a net-new Expressive component (added
   May 2025); Segmented Buttons are deprecated and not built. Desktop scope — window-size-class
   behavior and the overflow-menu collapse are out of scope.
4. **Naming:** the new visual container is `ElwhaButtonGroup`. The pre-existing `ButtonGroup` /
   `IconButtonGroup` are selection-mutex helpers (the `javax.swing.ButtonGroup` analog) — they keep
   their names and are reused internally.
5. **Build shape:** one rendering extension to the button primitives (Phase 1) + the net-new
   container (Phases 2–3) + workbench (Phase 4) + showcase wire-in (Phase 5). Animated shape morph
   is a separate polish epic (#176), not a v1 blocker. Full roadmap in §17.

## Source

M3 Expressive "Button groups" documentation at m3.material.io, captured 2026-05-21 from ~28
screenshots (Overview / Specs / Guidelines / Accessibility tabs). The M3 site is a JS SPA, so this
is a screenshot-based capture; the low-res measurement screenshots were re-captured at full
resolution 2026-05-21 — all values in §11 are confirmed.

**Provenance note:** the capture pass started on the M3 "Segmented buttons" page. That page's own
callout states segmented buttons are **no longer recommended in M3 Expressive** — use the
**connected button group** instead. Per Elwha's locked M3-Expressive posture, Elwha builds the
Expressive component (Button groups), not the deprecated Segmented Buttons. Same precedent as the
nav rail dropping the baseline rail / navigation drawer.

---

## §1. Scope decisions — Elwha adaptation

Elwha is a desktop Swing/FlatLaf library. Out of scope:

- **Segmented Buttons** — deprecated by M3 Expressive; not built, no compat shim.
- **Window-size-class guidance** — M3's compact / large / XL "use smaller buttons in compact
  windows" guidance. Elwha is desktop-only; no breakpoint reactivity. (Same call as the nav rail.)
- **Overflow menu / trailing-edge collapse** — M3's Presentation section collapses trailing buttons
  into an overflow menu at small window sizes. Window-size-driven; **parked, not built in this
  epic** (decided §18.5) — a static, developer-controlled overflow may be revisited after v1.

In scope: the M3 Expressive **standard** and **connected** variants, all selection modes, the
XS–XL size axis, round/square shape, and the static selected-shape inversion. Animated shape morph
is a separate polish epic (§13, §16).

## §2. Component overview

New component — **`ElwhaButtonGroup`**. Two variants:

- **Standard** — buttons sit with visible **gaps**; each keeps its own rounded shape. A toolbar /
  action cluster. New in M3 Expressive (no baseline equivalent).
- **Connected** — segments **butted together** sharing edges, uniform sizing. A selection control.
  Replaces the M3 baseline Segmented Button.

**Naming collision (resolved):** Elwha already has `ButtonGroup` / `IconButtonGroup` — those are
*selection-mutex helpers* (the `javax.swing.ButtonGroup` analog, pure logic, no layout/paint). The
new **visual + layout + selection container** is `ElwhaButtonGroup`. The mutex helpers keep their
names and are reused internally by `ElwhaButtonGroup` (§5).

`ElwhaButtonGroup` is a container that **composes** `ElwhaButton` / `ElwhaIconButton` — it owns
layout, padding, group-wide shape/size, width behavior, and selection presentation. It does **not**
render buttons itself.

## §3. Anatomy

| # | Part | Notes |
|---|---|---|
| 1 | Container | The only part. Invisible — adds inter-button padding (standard) or butts segments (connected) and applies group shape. Holds no buttons by default; developer supplies them. |

M3: "Button groups are invisible containers that add padding between buttons and modify button
shape. They don't contain any buttons by default."

## §4. Variant comparison

| Aspect | Standard | Connected |
|---|---|---|
| Inter-button spacing | visible gaps (size-dependent padding) | 2dp inner padding, butted |
| Button shape | each keeps own shape | shared group shape, outer corners rounded / inner ~square |
| Width behavior | **hugs** content | **fills** container width, segments grow; optional maxWidth |
| On select | width + shape change, **ripples to neighbors** | shape change on selected segment **only** |
| Content | mix of label buttons + icon buttons | mix of label + icon segments |
| Replaces | — (net-new) | the baseline Segmented Button |

## §5. Selection model

Three M3 selection modes — **all API-mandatory**:

- **Single-select** — at most one selected.
- **Multi-select** — any number selected independently.
- **Selection-required** — exactly one selected at all times (M3 "selection-required"; equivalent
  to the chip-list `SINGLE_MANDATORY` and the existing `ButtonGroup` mandatory mode).

Reuse: `ButtonGroup` / `IconButtonGroup` mutex helpers already implement single-select + mandatory
(= selection-required). Multi-select uses a container-level tracker (decided §18.2).
`ElwhaButtonGroup` exposes a `SelectionMode` enum (`SINGLE` / `MULTI` / `REQUIRED`, decided §18.3)
and wires the appropriate mechanism internally.

M3 usage guidance: connected groups should be used for single/multi-select toggle patterns; avoid a
connected group where no button can be toggled.

## §6. Configuration surface

- Variant: `Standard` / `Connected`
- Selection mode: a `SelectionMode` enum — `SINGLE` / `MULTI` / `REQUIRED` (§18.3)
- Size: XS / S / M / L / XL (group-wide; applied to all member buttons)
- Default shape: `Round` / `Square` (group-wide; sets the resting shape of all segments)
- Resize mode: `Fixed` / `Flexible` (Fixed = manual width; Flexible = auto-grow — standard hugs,
  connected fills)
- Connected max-width: optional clamp for wide windows
- Color style: per §8 — connected enforces **one** style across all segments

## §7. States

`Enabled`, `Hovered`, `Focused`, `Pressed`, `Disabled` — maps to the theme `StateLayer` facade.

Tracked separately for **selected** vs **unselected** segments:
- Unselected: Enabled · Disabled · Hovered · Focused · Pressed (5)
- Selected: Enabled · Hovered · Focused · Pressed (4 — no disabled-selected shown by M3)

`Focused` shows a distinct focus ring on the segment.

## §8. Color

- **The button group itself has no color properties** — colour comes from the contained buttons.
- Buttons use default button / toggle-button colour styles: **Filled, Tonal, Outlined**.
- **Elevated — "not recommended"** in a group. **Text buttons excluded** — no container treatment.
- Elwha `ButtonVariant` already has `FILLED`, `FILLED_TONAL`, `OUTLINED` (+ `ELEVATED`, `TEXT`) —
  the group offers the first three; elevated/text simply aren't surfaced for connected groups.
- **Connected groups: do not mix colour styles** — M3 "Don't". **Enforced** by having the connected
  group take one colour style applied to all segments (mixing not expressible). Standard groups may
  mix freely — that is their purpose.

## §9. Density

Button groups have **no density property** — they adapt to the height of the buttons inside (M3
density scale 0 / −1 / −2 / −3). Whether Elwha exposes a desktop density axis at all is a
library-wide question, **out of scope for this epic** (decided §18.6); the group does not own it
either way.

## §10. Behavior — the shape morph

- **Pressed:** a pressed button changes **width and shape**. Standard → the change **ripples to
  adjacent buttons' widths**. Connected → **only the pressed button's shape** changes, neighbors
  untouched.
- **Selected:** a selected button **flips shape — round↔square** — taking the *opposite* of the
  group default. Round group → selected segment renders square; square group → selected renders
  round. This is the core visual signal of selection.

Morph model:

| Trigger | What morphs | Neighbors |
|---|---|---|
| Pressed | width + shape | standard: width ripple · connected: untouched |
| Selected | shape flips round↔square (opposite of group default) | untouched |

**Split for implementation (§16):** the *static* selected-shape inversion (selected segment paints
the inverted shape) needs only per-state corner-radius rendering — no animation — and is v1
spec-mandatory. The *animated* morph transition + the standard width-ripple is a separate polish
epic (#176).

## §11. Measurements

All values confirmed against the M3 Specs tab (capture 2026-05-21) — no remaining low-res gaps.

- **Connected inner padding: 2dp between segments, at every size** (both round and square forms).
- **Square connected — outer corner radius:** XS 4dp · S 8dp · M 8dp · L 16dp · XL 20dp.
- **Round connected:** outer shape fully round (pill); the **inner** shape stays square with
  per-size corner sizes — **XS 4dp · S 8dp · M 8dp · L 16dp · XL 20dp**. Same numbers as the square
  variant's outer corners, applied to the inner corners instead.
- **Standard group inner padding** (between buttons; varies by size to keep the 48dp target):
  **XS 18dp · S 12dp · M 8dp · L 8dp · XL 8dp.**
- **Minimum widths:** XS and S connected groups have **48dp target areas and 48dp minimum width**
  per segment.
- Size axis container heights are already in Elwha's `ButtonSize` (XS 32 / S 40 / M 56 / L 96 /
  XL 136 dp) and `IconButtonSize`.

## §12. Width / layout behavior

- **Single line always — never wraps to a second line.** Multiple groups may be stacked by the
  consumer; groups don't interact vertically.
- **Standard group: hugs** the width of the buttons inside (content-sized).
- **Connected group: fills** the container width, segments grow to share it; M3 recommends an
  optional **max-width** clamp in larger windows.
- **Fixed vs Flexible resize:** Fixed = button width/size/padding manually defined; Flexible =
  buttons + group auto-grow until all flexible buttons reach their largest width.

## §13. Accessibility — all Enforce (hard requirements)

- **Container is not focusable** and **does not need an accessible name.**
- **Initial focus lands on the first button**, then steps through each button.
- **Each segment is individually focusable and operable** — the group must not collapse into one
  tab stop.
- **Keyboard:** `Tab` → next button — **each segment is its own tab stop** (M3-literal model,
  decided §18.9; not Swing arrow-key roving-focus). `Space` / `Enter` → activate the focused button.
- **Each segment exposes role `Button`** + its own accessible name; icon-only segments must carry
  an accessible name (can't rely on a visible label). In Swing: correct `AccessibleContext`,
  `AccessibleRole`, and `AccessibleState.SELECTED`.
- Selected state must be exposed to assistive tech, not signalled by shape/colour alone.
- 48×48dp minimum target per segment — XS/S `ButtonSize.minimumTargetPx()` already inflates to 48;
  the small sizes' larger inner padding is an a11y floor, not cosmetic — don't allow it below the
  48dp-guaranteeing value.

## §14. Enforce vs Document (Usage-page guidance)

**Enforce** (API/impl makes misuse hard or impossible):
- Single-line layout, never wraps.
- Standard hugs content / connected fills container.
- Connected: one colour style across all segments.
- Group-wide size + shape applied uniformly.
- Accessibility: §13 in full.
- Selected-shape inversion automatic.

**Document** (Javadoc / workbench notes; consumer misuse is "oh well"):
- Use for 2–5 simple choices (more / complex → chips).
- Only use multiple sizes for "hero moments"; avoid mixing sizes frequently.
- Maintain visual hierarchy when scaling; primary action stays most prominent.
- Use connected when content is related and buttons can be toggled.
- Don't stretch icon buttons beyond the wide setting.

## §15. Existing-code reuse audit (Elwha as of the v0.2.0 button work)

Already present and sufficient — **no extension needed:**
- `ButtonSize` / `IconButtonSize` — XS–XL, exact match, with per-size corner + 48dp target.
- `ButtonVariant` — FILLED / FILLED_TONAL / OUTLINED (+ ELEVATED / TEXT).
- `ButtonInteractionMode.SELECTABLE` — persistent toggle/selected state.
- `ButtonGroup` / `IconButtonGroup` — mutex single-select + mandatory mode (= selection-required).

## §16. Required button extensions

Today `ElwhaButton.cornerRadiusPx()` returns **one uniform radius** and `paintComponent` paints a
single round-rect (`arc = cornerRadiusPx()`). `ButtonShape` is a 2-value enum, static for the
component lifetime; its javadoc explicitly defers shape morph to "a future animation epic" per
[`elwha-button-design.md`](elwha-button-design.md) §10.

Extensions needed on `ElwhaButton` **and** `ElwhaIconButton` (Phase 1):
1. **Per-corner radius** — connected segments render outer corners rounded, inner corners ~square
   (2dp). The per-corner radius API lives **on the button**, not the group (decided §18.1).
2. **Per-state shape** — the selected segment renders the inverted (round↔square) shape. Static,
   no animation.

Separate (polish epic #176): **animated** morph transition + the standard-variant width ripple.

## §17. Order of operations (phase roadmap)

```
PHASE 0  Spec & Epic — DONE: this doc; epic #170 + stories #171–#176 filed; §18 decisions resolved
   │
   ├─ TRACK A ─ PHASE 1 (#171)  Button rendering extension  (HARD PREREQ for connected)
   │              per-corner radius + per-state shape flip; ElwhaButton + ElwhaIconButton
   │
   └─ TRACK B ─ PHASE 2 (#172)  ElwhaButtonGroup container + STANDARD variant
                  row layout, group size/shape, standard gaps, hug-content, a11y traversal
   (Tracks A & B are parallel — standard variant needs no rendering extension)
   │
PHASE 3 (#173)  CONNECTED variant — needs Phase 1 AND Phase 2
   2dp inner padding, butted segments, fill-width + maxWidth, selection modes, one colour style
   │
PHASE 4 (#174)  Workbench — ElwhaButtonGroupWorkbench
   │
PHASE 5 (#175)  Showcase integration — light/dark/system toggle + primary/secondary theme picker
   ┄┄┄┄
PHASE 6 (#176)  Morph polish — SEPARATE epic — animated press/select morph + standard width ripple
```

## §18. Decisions — all resolved

_All 9 resolved 2026-05-21 (operator + capture). Numbering is stable so existing issue references
(§18.1, §18.2, …) hold._

1. **Connected corner rendering** → **per-corner radius API on `ElwhaButton` / `ElwhaIconButton`.**
   The button owns its own pixels; the group does not reach into child painting. Sets Phase 1
   (#171) scope.
2. **Multi-select wiring** → **container-level multi-select tracker** on `ElwhaButtonGroup`. All
   three modes expose a uniform selection surface + one unified change event — not bare independent
   buttons.
3. **Selection-mode API** → **a single `SelectionMode` enum (`SINGLE` / `MULTI` / `REQUIRED`)** on
   `ElwhaButtonGroup`. The group wires the right helper internally; the `ButtonGroup` /
   `IconButtonGroup` mutex helpers stay an implementation detail.
4. **Morph in v1** → **static selected-shape inversion in v1**; animated morph + the standard
   width-ripple stay in the Phase 6 epic #176. #176 does **not** block v1.
5. **Overflow menu** → **parked** — not built in this epic. Revisit a static, developer-controlled
   overflow after v1.
6. **Desktop density axis** → **out of scope for this epic** — a library-wide question, not a
   button-group one. `ElwhaButtonGroup` just adapts to button height (§9).
7. **Round-connected inner corner sizes** → XS 4 · S 8 · M 8 · L 16 · XL 20 dp (identical to the
   square variant's outer corners). See §11.
8. **Standard-group inner padding** → XS 18 · S 12 · M 8 · L 8 · XL 8 dp. See §11.
9. **Keyboard model** → **follow the M3 spec literally: `Tab` moves between every segment**, each
   segment its own tab stop. Not Swing arrow-key roving-focus. Operator call.

## §19. Screenshot index (~28 captures)

Overview: Segmented Buttons (deprecation context) · Button groups Overview · M3 Expressive update ·
Variants · Configurations. Specs: Anatomy · Common layouts · Color · Selection & activation ·
States (standard) · Connected unselected states · Connected selected states · Measurements
(standard) · Connected measurements · Square connected measurements · Minimum widths · Density.
Guidelines/Usage: Usage intro (variants + standard) · Connected button groups · Color · Anatomy /
Container · Adaptive design (Resizing) · Adaptive design (Presentation) · Behavior (Pressed) ·
Behavior (Selected). Accessibility: Use cases · Interaction & style / Initial focus / keyboard ·
Keyboard navigation / Labeling elements. Plus: shape-morph crop (per-corner asymmetry).
