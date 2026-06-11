# ElwhaRadioButton — M3 Spec Capture (research scratch)

**Status:** CAPTURE COMPLETE — M3 source material for epic [#416](https://github.com/OWS-PFMS/elwha/issues/416) (ElwhaRadioButton + ElwhaRadioGroup, `v0.4.0`). Companion research dump (mirrors [`elwha-switch-research.md`](elwha-switch-research.md) / [`elwha-slider-research.md`](elwha-slider-research.md)); the decisions live in [`elwha-radiobutton-design.md`](elwha-radiobutton-design.md).

**Captured:** 2026-06-10. **Author:** Charles Bryan (`cfb3@uw.edu`).

**Capture method note:** specced web-first in a single autonomous run (the switch-epic method). The token sheet below is the **complete `md-comp-radio-button` v0.192 values file** from the material-web repo (the same generated set m3.material.io renders), the geometry comes from material-web's internal SVG + style sheets, and the press-swap rationale is confirmed by MDC-Android's tint selectors **with their in-source comments**. No operator screenshot session ran; §F lists raw-file URLs instead of a screenshot log. The m3.material.io radio-button spec page is JS-only (WebFetch returns the page title), per the established pattern.

**Consumers / related:**
- **OWS-PFMS/OWS-Local-Search-GUI** — any "pick one of N" option row (sort order, view mode, filter presets) migrates onto `ElwhaRadioButton` once Phase 1 ships.
- [`ElwhaSwitch`](../../src/main/java/com/owspfm/elwha/switches/ElwhaSwitch.java) (epic #401, in-flight branch) — the sibling selection control and the architecture template: dedicated `JComponent`, plain `selected` boolean, `ChangeListener`/`ActionListener` split, gallery hooks, `RetargetTween` motion idiom. ⚠️ #401 is **not on `main` yet** — this epic duplicates the private `RetargetTween` idiom rather than depending on the unmerged branch; extraction to `theme/` is a noted follow-up once both land (design §14).
- [`MorphAnimator`](../../src/main/java/com/owspfm/elwha/theme/MorphAnimator.java) + [`Easing`](../../src/main/java/com/owspfm/elwha/theme/Easing.java) — `EMPHASIZED_DECELERATE` is already a named constant; the dot-grow needs nothing new.
- [`RipplePainter`](../../src/main/java/com/owspfm/elwha/theme/RipplePainter.java) + `StateLayer` — the 40dp state layer maps onto the existing enum; press ripple bounded to the circle (material-web attaches `md-ripple` to the same control).

---

## §TL;DR — synthesis (read this first)

**What the M3 Radio button is**, distilled from the capture below:

1. **One component, no axes.** The M3 radio-button spec defines **no variants and no sizes** — one 20dp form. The only configurations: selected/unselected and enabled/disabled. (§A, §C.)
2. **Zero new theme tokens.** Every color maps onto existing `ColorRole`; opacities onto `StateLayer`; the icon is geometry, not a glyph. (§Tokens.)
3. **Anatomy (§C, §G):** a 20dp **icon** — a **2dp ring** (material-web renders an r=10 circle masked by an r=8 hole in a 20dp viewBox) plus a **10dp inner dot** (r=5) shown while selected — and the 40dp **state layer**. That's the whole component; the label is external.
4. **Color (§T):** unselected ring `ON_SURFACE_VARIANT`, shifting to `ON_SURFACE` under hover/focus/press; selected ring **and** dot `PRIMARY` in every interactive state; disabled `ON_SURFACE` @ 0.38 on both sides (symmetric — simpler than the switch's ⚠ asymmetry).
5. **The press swap (§T, confirmed §C′):** state layers are `ON_SURFACE`-tinted unselected and `PRIMARY`-tinted selected for hover/focus — **but pressed flips both**: pressing an *unselected* radio paints a `PRIMARY` layer ("tapping an unchecked radiobutton will turn it blue" — MDC-Android's comment, verbatim), pressing a *selected* one paints `ON_SURFACE` ("tapping a checked radiobutton keeps it checked").
6. **Motion (§Mo):** select = inner dot **grows 0→1 over 300ms `easing-emphasized-decelerate`** (`inner-circle-grow` keyframes, verbatim); deselect = the dot **fades** (`opacity 50ms linear` — no shrink); ring color = `fill 50ms linear`. Disabled: all durations 0.
7. **Interaction (§B):** "clicking on a radio input **always selects it**" — a user gesture never deselects. Space selects. No drag gesture exists.
8. **Grouping (§B′):** material-web's `SingleSelectionController` is the normative group contract — mutual exclusion, **arrow keys move selection** (selection follows focus, wraps, skips disabled, direction-aware in RTL), and a **roving tab stop** via three focusable rules: a checked member is the stop; a focused member is the stop; none checked/focused → all are stops.
9. **A11y (§X):** web `role="radio"` / `role="radiogroup"`; Swing natively has **`AccessibleRole.RADIO_BUTTON`** + `AccessibleState.CHECKED`, plus `AccessibleRelation.MEMBER_OF` for group membership. External label always required ("radios … always need an `aria-label`").
10. **Touch target:** 48px wrapper, state layer 40dp, web focus ring 44px — desktop pointer precision makes 48 a [DOC] guidance; Elwha paints focus as the `StateLayer.FOCUS` circle (the switch precedent), not a web-style ring.
11. **Expressive status (§E):** the May-2025 M3 Expressive update did **not** publish a revised radio-button spec (not in the "14 new or updated components" list); the current spec is operative. **No expressive delta to wait for; no excluded-variant stub epics to file.**

### Reading-order TOC
§A material-web overview/API → §C MDC-Android anatomy → §C′ MDC-Android tint selectors (the press-swap proof) → §T token sheet (complete v0.192) → §G geometry internals → §Mo motion internals → §S states → §B behaviors → §B′ group controller contract → §X a11y → §E Expressive status → §Tokens Elwha mapping → §P terminology lock → §Open → §F capture log.

---

## §A. Material Web overview + API (authoritative text)

Source: <https://raw.githubusercontent.com/material-components/material-web/main/docs/components/radio.md> (fetched 2026-06-10).

> Radio buttons let users **select one option from a set of options**.

```html
<form>
  <md-radio name="animals" value="cats"></md-radio>
  <md-radio name="animals" value="dogs" checked></md-radio>
</form>
```

| Property | Type | Default | Notes |
|---|---|---|---|
| `checked` | boolean | `false` | the selection state — HTML's noun; M3 prose says *selected* `[CODE]` |
| `disabled` | boolean | `false` | `[CODE]` |
| `name` / `value` / `required` / validity surface | — | — | HTML-form plumbing; the *name-groups-radios* mechanism becomes `ElwhaRadioGroup` membership `[CODE]` (mechanism) / `[DOC]` (form fields) |

Events: **`change`** + **`input`**, both fire **on user interaction only** → Swing: `ActionListener` for user selection commits; `ChangeListener` for any state change (the switch/slider precedent). `[CODE]`

A11y guidance `[CODE]`: wrap groups in `role="radiogroup"` with a group label; "do **not** wrap radios inside of a `<label>`, which stops screen readers from correctly announcing the number of radios in a group"; **"Radios are not automatically labelled by `<label>` elements and always need an `aria-label`."** → `setLabel(String)` is load-bearing, not optional.

Web theming surface (maps in §Tokens): `--md-radio-icon-color` (= `on-surface-variant`), `--md-radio-selected-icon-color` (= `primary`), `--md-radio-icon-size` (= `20px`).

## §C. MDC-Android anatomy + attributes (authoritative text)

Source: <https://raw.githubusercontent.com/material-components/material-components-android/master/docs/components/RadioButton.md> (fetched 2026-06-10).

**Anatomy:** **1. Selected icon** (ring + filled inner dot) · **2. Label text** (adjacent) · **3. Unselected icon** (empty ring). Radio buttons "display as circles that **fill with an inset when selected**." `[CODE]` for the icons; the label is `CompoundButton` heritage (`android:text`, `textAppearanceBodyMedium`) — **no label tokens exist in the M3 radio token set (§T)**, so labels are external in Elwha, exactly as the switch resolved (§Open-2). `[DOC]`

| Element | Default | Notes |
|---|---|---|
| Button tint | `?attr/colorOnSurface` unchecked / `?attr/colorPrimary` checked | the color selector resolves the §T per-state set `[CODE]` |
| Min width/height | `?attr/minTouchTargetSize` (48dp) | touch floor — [DOC] on desktop |
| States | "enabled, disabled, hover, focused, and pressed — each in both selected and unselected" | the full 10-cell matrix `[CODE]` |

Grouping: "Use within **`RadioGroup`** to automatically deselect other buttons" — "selecting a button in a group deselects all others." `[CODE]` → the `ElwhaRadioGroup` noun.

## §C′. MDC-Android tint selectors — the press-swap proof (verbatim)

Sources: `radiobutton/res/color/m3_radiobutton_button_tint.xml` + `m3_radiobutton_ripple_tint.xml` (fetched 2026-06-10). The ripple selector carries the rationale in comments:

> `<!-- Uses the primary state layer since tapping an unchecked radiobutton will turn it blue (checked). -->` → `unselected-pressed-state-layer-color = primary` `[CODE]`
>
> `<!-- Uses the primary state layer since tapping a checked radiobutton keeps it checked. -->` *(block header for the checked side, whose pressed entry resolves on-surface)* → `selected-pressed-state-layer-color = on-surface` `[CODE]`

The button tint confirms the icon side: disabled entries at `disabled_*_icon_opacity` over `on-surface`; checked entries `selected_{pressed,focus,hover,}_icon_color`; unchecked entries `unselected_*_icon_color`. (The selector resolves the same §T values.)

## §T. Token sheet — complete `md-comp-radio-button` v0.192 (verbatim)

Source: <https://raw.githubusercontent.com/material-components/material-web/main/tokens/versions/v0_192/_md-comp-radio-button.scss> (fetched 2026-06-10). This is the generated component-token set the m3.material.io spec page renders. (The live `tokens/_md-comp-radio.scss` wrapper renames the eight `unselected-*` icon/state-layer tokens to unprefixed forms per their b/292244480 — values identical.)

**Dimensions + shape `[CODE]`:**

| Token | Value |
|---|---|
| `icon-size` | **20px** |
| `state-layer-size` | **40px** |
| icon/state-layer shape | `corner-full` (circles) |
| touch target | 48px wrapper; web focus ring 44px (consumer sheet, not component tokens) `[DOC]` |

**Unselected colors `[CODE]`:**

| Token | Role |
|---|---|
| `unselected-icon-color` | `on-surface-variant` |
| `unselected-hover-icon-color` | `on-surface` |
| `unselected-focus-icon-color` | `on-surface` |
| `unselected-pressed-icon-color` | `on-surface` |
| `unselected-hover-state-layer-color` | `on-surface` @ `hover-state-layer-opacity` |
| `unselected-focus-state-layer-color` | `on-surface` @ `focus-state-layer-opacity` |
| `unselected-pressed-state-layer-color` | **`primary`** @ `pressed-state-layer-opacity` ← the swap |

**Selected colors `[CODE]`:**

| Token | Role |
|---|---|
| `selected-icon-color` | `primary` |
| `selected-hover-icon-color` | `primary` |
| `selected-focus-icon-color` | `primary` |
| `selected-pressed-icon-color` | `primary` |
| `selected-hover-state-layer-color` | `primary` @ `hover-state-layer-opacity` |
| `selected-focus-state-layer-color` | `primary` @ `focus-state-layer-opacity` |
| `selected-pressed-state-layer-color` | **`on-surface`** @ `pressed-state-layer-opacity` ← the swap |

State-layer opacities are the `md-sys-state` values → **Elwha's `StateLayer` enum governs** (HOVER 0.08 / FOCUS 0.10 / PRESSED 0.10, resolved from UIManager at paint time).

**Disabled `[CODE]`:**

| Token | Value |
|---|---|
| `disabled-selected-icon-color` | `on-surface` @ **0.38** |
| `disabled-unselected-icon-color` | `on-surface` @ **0.38** |

Symmetric — unlike the switch's opaque-`SURFACE` disabled-selected handle. No disabled state layers, no transitions.

## §G. Geometry internals (material-web SVG, verbatim)

Source: `radio/internal/radio.ts` (fetched 2026-06-10). The icon is an SVG in `viewBox="0 0 20 20"`:

| Part | Element | Derived geometry |
|---|---|---|
| Outer ring | `<circle cx="10" cy="10" r="10">` masked by `<circle cx="10" cy="10" r="8" fill="black">` | a **filled ring**, outer Ø20, inner Ø16 ⇒ **2dp ring width** — mask-built, not stroked `[CODE]` |
| Inner dot | `<circle cx="10" cy="10" r="5">` | **Ø10 dot**, concentric `[CODE]` |

The ring being mask-built (not a stroke) lands exactly on Elwha's switch-S1 lesson: paint the ring as an `Area` subtraction so there is no half-pixel stroke seam and translucent disabled fills never double-blend. Derived: state-layer overhang past the icon = `(40 − 20) / 2` = **10px** per side.

## §Mo. Motion internals (material-web style sheet, verbatim)

Source: `radio/internal/_radio.scss` (fetched 2026-06-10).

| What | Property | Duration | Easing |
|---|---|---|---|
| Inner dot **grow** (on check) | `animation: inner-circle-grow` — `from { transform: scale(0); } to { transform: scale(1); }`, `transform-origin: center` | **300ms** | `easing-emphasized-decelerate` `[CODE]` |
| Inner dot **fade** (on uncheck) | `transition: opacity` (`0` unchecked → `1` checked) | **50ms** | `linear` `[CODE]` |
| Outer ring **color** | `transition: fill` | **50ms** | `linear` `[CODE]` |
| **Disabled** | everything | `animation-duration: 0s; transition-duration: 0s` (snap) `[CODE]` |

So: select = dot springs out (300ms decelerate) while the ring color arrives in 50ms; deselect = the dot fades at full size in 50ms — **there is no shrink animation**. Press feedback is the state layer + ripple only — the icon never changes size under press (no switch-style press morph).

## §S. States

From §C ("enabled, disabled, hover, focused, and pressed — each in both selected and unselected") + the §T layers:

- **Hover / focus:** static 40dp state layer centered on the icon; tint `ON_SURFACE` unselected / `PRIMARY` selected; the unselected ring simultaneously darkens `ON_SURFACE_VARIANT → ON_SURFACE`. `[CODE]`
- **Pressed:** state layer with the **§T swap** (`PRIMARY` unselected / `ON_SURFACE` selected) + ripple bounded to the 40dp circle (material-web bounds `md-ripple` to it); unselected ring darkens as under hover. `[CODE]`
- **No dragged state** exists for radio. `[CODE]`
- **Disabled:** both icons `ON_SURFACE` @ 0.38, no layers, no motion. `[CODE]`

## §B. Behaviors

- **"Clicking on a radio input always selects it"** (radio.ts, verbatim) — a click on the already-selected radio re-affirms it: no state change, no `change` event. A user gesture **never deselects**. `[CODE]`
- **Space** selects (same path as click). "Other keys are ignored" at the single-radio level — arrows belong to the group controller (§B′). Enter is not bound. `[CODE]`
- **Programmatic `checked` writes** never fire `change` → `setSelected` fires `ChangeListener` only, never `ActionListener`. `[CODE]`
- Click also focuses the radio (material-web focuses on activation click). `[CODE]`
- **No drag gesture** exists (unlike switch/slider). `[CODE]`

## §B′. Group controller contract (material-web `SingleSelectionController`, authoritative)

Source: `radio/internal/single-selection-controller.ts` (fetched 2026-06-10).

- **Membership:** "all single selection elements in the host element's root with the same `name` attribute" → Elwha: explicit `ElwhaRadioGroup.add(...)`; membership order = navigation order. `[CODE]`
- **Mutual exclusion:** on check, `uncheckSiblings()` — every other member's `checked` drops to false. Adding an already-checked member when a selection exists normalizes (the controller unchecks on attach). `[CODE]`
- **Arrow keys:** `ArrowUp`/`ArrowDown`/`ArrowLeft`/`ArrowRight`; **selection follows focus** — the arrow moves to the next/previous **non-disabled** sibling and **immediately checks it**, firing `change` ("Fire a change event since the change is triggered by a user action"); **wraps** at both ends ("if we return to the host index, there is nothing to select"); **direction-aware**: in RTL, left/down move forward; in LTR, right/down do. `[CODE]`
- **Roving tab stop — three rules, verbatim:** `[CODE]`
  1. "If any are checked, that element is focusable" (the others are not);
  2. "If an element is focused, the others are no longer focusable";
  3. "If none are checked or focused, all are focusable."

## §X. Accessibility

- Web: `role="radio"` per control + `role="radiogroup"` wrapper with `aria-label`/`aria-labelledby`. Android: framework checkable semantics via Talkback, label text read automatically. `[CODE]`
- Swing has the native vocabulary — **`AccessibleRole.RADIO_BUTTON`** (no role mapping compromise, unlike the switch) + **`AccessibleState.CHECKED`** while selected + one "click" `AccessibleAction` (the user-gesture select) + `AccessibleValue` 0/1. `[CODE]`
- **`AccessibleRelation.MEMBER_OF`** exists in Swing precisely for this ("indicates an object is a member of a group") — the group maintains the relation across members so assistive tech can announce group membership. `[CODE]`
- Accessible name: `setLabel(String)` + `JLabel.setLabelFor(radio)` fallback (switch/slider precedent); a radio **always** needs a label (§A). `[CODE]`
- Contrast: the 2dp `ON_SURFACE_VARIANT` ring carries the unselected affordance against `SURFACE` backgrounds — don't thin it. `[DOC]`

## §E. M3 Expressive status

Searched 2026-06-10. The May-2025 Expressive update's "14 new or updated components" list (button groups, FAB menu, split button, loading indicator, toolbars, …) does **not** include the radio button; m3.material.io publishes a single current radio-button spec and no expressive-variant page. The dot-grow on `emphasized-decelerate` *is* the radio's expressive motion story, consistent with [[project_elwha_m3_expressive]]. Sources: <https://m3.material.io/components/radio-button/specs>, <https://supercharge.design/blog/material-3-expressive>. **No expressive delta to wait for; no excluded-variant stub epics to file.**

## §Tokens. Elwha token mapping — zero new tokens ✅

| M3 need | Elwha token | Notes |
|---|---|---|
| unselected ring | `ColorRole.ON_SURFACE_VARIANT` | |
| unselected hover/focus/pressed ring | `ColorRole.ON_SURFACE` | |
| selected ring + dot (all interactive states) | `ColorRole.PRIMARY` | |
| unselected hover/focus state-layer tint | `ColorRole.ON_SURFACE` | |
| unselected **pressed** state-layer tint | `ColorRole.PRIMARY` | the §T swap |
| selected hover/focus state-layer tint | `ColorRole.PRIMARY` | |
| selected **pressed** state-layer tint | `ColorRole.ON_SURFACE` | the §T swap |
| disabled icon (both sides) | `ColorRole.ON_SURFACE` @ `StateLayer.disabledContentOpacity()` | exact 0.38 match, symmetric |
| hover/focus/pressed opacities | `StateLayer.HOVER/FOCUS/PRESSED` | library-pinned values |
| icon/state-layer shape | `ShapeScale.FULL` | circles |
| dot grow | `MorphAnimator` (300ms = `MEDIUM2_MS`) + `Easing.EMPHASIZED_DECELERATE` | both already exist — not even a component-local bezier needed |
| deselect fade / ring color | 50ms linear | component-local `COLOR_FADE_MS = 50` constant (like the switch's 250/100) |
| press ripple | `RipplePainter` | bounded to the 40dp circle |

## §P. Terminology → API lock (M3 nouns mirror rule)

| M3 noun | Elwha API |
|---|---|
| radio button | `ElwhaRadioButton` — package `com.owspfm.elwha.radio` (no reserved-word dodge needed) |
| selected | `isSelected()` / `setSelected(boolean)` (M3 prose noun; matches `ElwhaSwitch`/`ElwhaChip`) |
| radio group / `RadioGroup` (Android) | `ElwhaRadioGroup` — non-visual controller, `add`/`remove`/`getSelected`/`setSelected`/`clearSelection` |
| icon / ring / dot | internal paint vocabulary + Javadoc nouns ("ring", "dot" — MDC's "circles that fill with an inset") |
| `change` event (user commit) | `addActionListener(ActionListener)` — command `"selected"` |
| any state change | `addChangeListener(ChangeListener)`; group-level `ElwhaRadioGroup.addChangeListener` |
| label (a11y name) | `setLabel(String)` / `getLabel()` + `labelFor` association |

## §Open — questions carried into the design doc

1. **Architecture** — dedicated `JComponent` vs styled `JRadioButton`+`ButtonUI`: resolved RECOMMENDED-custom in design §2 (the switch §2 calculus, *plus* the roving-focus requirement that `ButtonGroup` has no vocabulary for), locked via the S1 spike.
2. **Roving-focus mechanics in Swing** — material-web's three tabindex rules map to `setFocusable` flag management by the group; the risk point is focus-loss ordering when the flags flip (design §9; tuned in S5).
3. **Group event surface** — one group `ChangeListener` (selection changed) chosen over an `ItemListener`-style two-event protocol; the deselected member's own `ChangeListener` already tells per-member subscribers (design §8).
4. **`RetargetTween` duplication** — the switch's motion idiom is on the unmerged #401 branch; this epic re-implements it privately rather than coupling branches. Extraction follow-up filed at merge time (design §14).
5. **Text label slot** — no label tokens exist in `md-comp-radio-button`; Android's label is `CompoundButton` heritage. V1 = external labels + `setLabel` a11y name (the switch §Open-3 resolution). A labeled radio-row composite is an M3 *list item* pattern — documented out-of-scope (design §12), no stub epic.

## §F. Capture log (web-first run — raw URLs, no screenshots)

| # | Source | What it contributed |
|---|---|---|
| 1 | `material-web/main/docs/components/radio.md` | §A API + a11y labelling rules + theming surface |
| 2 | `material-components-android/master/docs/components/RadioButton.md` | §C anatomy, tint defaults, the 10-state matrix, `RadioGroup` semantics |
| 3 | `material-components-android/.../color/m3_radiobutton_button_tint.xml` + `m3_radiobutton_ripple_tint.xml` | §C′ press-swap proof with in-source rationale comments |
| 4 | `material-web/main/tokens/versions/v0_192/_md-comp-radio-button.scss` | §T complete token sheet (the authoritative values) |
| 5 | `material-web/main/tokens/_md-comp-radio.scss` | §T note — the `unselected-*` prefix rename (b/292244480), supported/unsupported token split |
| 6 | `material-web/main/radio/internal/radio.ts` | §G SVG geometry (r=10/r=8 mask ring, r=5 dot), click-always-selects, Space, focus-on-click |
| 7 | `material-web/main/radio/internal/_radio.scss` | §Mo dot grow 300ms emphasized-decelerate, 50ms fades, disabled snap; 44px focus ring + 48px touch target |
| 8 | `material-web/main/radio/internal/single-selection-controller.ts` | §B′ arrows / selection-follows-focus / wrap / RTL / the three roving-tabindex rules |
| 9 | Web search (M3 Expressive radio button, 2026-06-10) | §E — no expressive radio delta published |
