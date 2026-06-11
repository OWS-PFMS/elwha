# ElwhaSwitch — M3 Spec Capture (research scratch)

**Status:** CAPTURE COMPLETE — M3 source material for epic [#401](https://github.com/OWS-PFMS/elwha/issues/401) (ElwhaSwitch, `v0.4.0`). Companion research dump (mirrors [`elwha-slider-research.md`](elwha-slider-research.md) / [`elwha-menu-research.md`](elwha-menu-research.md)); the decisions live in [`elwha-switch-design.md`](elwha-switch-design.md).

**Captured:** 2026-06-10. **Author:** Charles Bryan (`cfb3@uw.edu`).

**Capture method note:** this epic was specced web-first in a single autonomous run — the token sheet below is the **complete `md-comp-switch` v0.192 values file** pulled from the material-web repo (the same generated token set m3.material.io renders), plus the material-web internal style sheets for motion. No operator screenshot session ran; there is therefore no screenshot log, and the §F capture log lists raw-file URLs instead. The m3.material.io Switch spec page is JS-only (WebFetch returns the page title), per the established pattern.

**Consumers / related:**
- **OWS-PFMS/OWS-Local-Search-GUI** — any boolean setting / feature-flag row migrates onto `ElwhaSwitch` once Phase 1 ships.
- `JCheckBox` sites inside this repo (Showcase Workbench controls everywhere) — the long-term dogfood target where a switch reads better than a checkbox ([[feedback_dogfood_elwha_components]]); **not** migrated in this epic (Workbench controls are deliberately plain `J*` chrome).
- [`MorphAnimator`](../../src/main/java/com/owspfm/elwha/theme/MorphAnimator.java) + [`Easing`](../../src/main/java/com/owspfm/elwha/theme/Easing.java) — drive the slide / size morphs; `Easing.cubicBezier` accepts material-web's overshoot control points (y > 1 is evaluated, not clamped).
- [`RipplePainter`](../../src/main/java/com/owspfm/elwha/theme/RipplePainter.java) + `StateLayer` — the 40dp handle-centered state layer maps onto the existing enum; press ripple bounded to the same circle (material-web attaches `md-ripple` to the handle).
- [`ElwhaSlider`](../../src/main/java/com/owspfm/elwha/slider/ElwhaSlider.java) — the architecture template: dedicated `JComponent`, paint-time token resolve, halo-inclusive preferred size, gallery `setHovered`/`setPressed` hooks, RTL `mirror()` math.

---

## §TL;DR — synthesis (read this first)

**What the M3 Switch is**, distilled from the capture below:

1. **One component, no axes.** The M3 switch spec defines **no variants and no sizes** — one 52×32 form. The only configurations: **icons** (none / both / selected-only) and the universal enabled/disabled. (§A, §C.)
2. **Zero new theme tokens.** Every color need maps onto existing `ColorRole`; opacities onto `StateLayer`; shapes are corner-full (`ShapeScale.FULL`). (§Tokens.)
3. **Anatomy (§C):** **track** 52×32dp corner-full; **handle** (M3's noun — "thumb" is the legacy name) riding inside it; optional **icon** centered on the handle. Unselected track carries a **2dp `OUTLINE` border**; selected track has none.
4. **The handle is the state display (§T):** diameter **16dp** unselected → **24dp** selected → **28dp** pressed (either side) → **24dp** whenever icons are shown. Handle center travels `trackHeight/2 → trackWidth − trackHeight/2` (16→36dp, a 20dp run).
5. **Color (§T):** selected = track `PRIMARY`, handle `ON_PRIMARY` (hover/focus/pressed handle → `PRIMARY_CONTAINER`), icon `ON_PRIMARY_CONTAINER`. Unselected = track `SURFACE_CONTAINER_HIGHEST` + `OUTLINE` border, handle `OUTLINE` (hover/focus/pressed → `ON_SURFACE_VARIANT`), icon `SURFACE_CONTAINER_HIGHEST`. Disabled = `ON_SURFACE`/`SURFACE` treatments at the `StateLayer` disabled opacities (§T-disabled).
6. **State layer (§S):** **40dp circle centered on the handle**, following it as it slides. Selected tint `PRIMARY`, unselected tint `ON_SURFACE`. Hover/focus static; press = ripple bounded to the same circle (material-web's `md-ripple`).
7. **Motion (§Mo):** slide = **300ms** margin transition on an **overshoot bezier (0.175, 0.885, 0.32, 1.275)**; handle size morph **250ms standard** (press-grow **100ms linear**); track/handle color **67ms linear**; icon opacity **33ms** / transform **167ms** (check rotates **−45°→0** into view in selected-only mode). Disabled: no transitions.
8. **Interaction (§B):** click anywhere toggles; **handle drags** — commit by which half it lands in; **Space** toggles from the keyboard (press = grow, release = commit). Fires `change` on user commit.
9. **A11y (§X):** web role `switch` / Android `Switch` widget semantics → Swing: `AccessibleRole.TOGGLE_BUTTON` + `AccessibleState.CHECKED` + `AccessibleAction` + `AccessibleValue` (the `JToggleButton` accessible shape). External label names the switch — "switches are not automatically labelled… and always need an aria-label."
10. **Touch target:** 48px (`touch-target-size`), state layer 40dp — desktop pointer precision makes 48 a [DOC] guidance, not painted geometry.
11. **Expressive status (§E):** the May-2025 M3 Expressive update did **not** publish a revised switch spec — the current m3.material.io switch spec is the operative one; the press-grow handle morph *is* the switch's expressive shape-change behavior.

### Reading-order TOC
§A material-web overview/API → §C MDC-Android anatomy → §T token sheet (complete v0.192) → §Mo motion internals → §S states → §B behaviors → §X a11y → §E Expressive status → §Tokens Elwha mapping → §P terminology lock → §Open → §F capture log.

---

## §A. Material Web overview + API (authoritative text)

Source: <https://raw.githubusercontent.com/material-components/material-web/main/docs/components/switch.md> (fetched 2026-06-10).

> Switches **toggle the state of an item on or off**.

```html
<md-switch></md-switch>
<md-switch selected></md-switch>
<md-switch icons selected></md-switch>
<md-switch icons show-only-selected-icon selected></md-switch>
```

| Property | Type | Default | Notes |
|---|---|---|---|
| `selected` | boolean | `false` | the switch state — M3's noun, not "checked" `[CODE]` |
| `icons` | boolean | `false` | show icons on the handle in **both** states `[CODE]` |
| `show-only-selected-icon` | boolean | `false` | icon on the **selected** handle only `[CODE]` |
| `disabled` | boolean | `false` | `[CODE]` |
| `required` / `name` / `value` / validity surface | — | — | HTML-form plumbing — N/A to Swing `[DOC]` |

Events: **`change`** + **`input`**, both fire **on user interaction only** (not programmatic `selected` writes) → Swing: `ActionListener` for user commits; `ChangeListener` for any state change (the slider precedent). `[CODE]`

A11y guidance: label via `<label>`/`for`, **but** "switches are not automatically labelled by `<label>` elements and **always need an `aria-label`**" → `setLabel(String)` accessible-name path is load-bearing, not optional. `[CODE]`

Web theming surface (maps to Elwha roles in §Tokens): `--md-switch-handle-color` (= `outline`), `--md-switch-track-color` (= `surface-container-highest`), `--md-switch-selected-handle-color` (= `on-primary`), `--md-switch-selected-track-color` (= `primary`), `*-shape` (= `corner-full`).

## §C. MDC-Android anatomy + attributes (authoritative text)

Source: <https://raw.githubusercontent.com/material-components/material-components-android/master/docs/components/Switch.md> (fetched 2026-06-10).

**Anatomy — three parts:** **1. Track** (the background rail) · **2. Handle** ("formerly *thumb*" — MDC says so explicitly; the M3 noun is **handle**) · **3. Icon** (optional, centered atop the handle, **16dp** default). `[CODE]`

Attribute highlights (defaults are the M3 spec values):

| Element | Default | Notes |
|---|---|---|
| Min height | `?attr/minTouchTargetSize` (48dp) | touch-target floor `[DOC]` on desktop |
| Thumb tint | `colorOutline` unchecked / `colorOnPrimary` checked | `[CODE]` |
| Thumb icon size | **16dp** | `[CODE]` |
| Thumb icon tint | `colorSurfaceContainerHighest` unchecked / `colorOnPrimaryContainer` checked | `[CODE]` |
| Track tint | `colorSurfaceContainerHighest` unchecked / `colorPrimary` checked | `[CODE]` |
| Track decoration (the outline) | `colorOutline` unchecked / **transparent** checked | the 2dp border exists **only unselected** `[CODE]` |
| Text label | optional `android:text`, `textAppearanceBodyMedium`, 16dp `switchPadding` | Android's `CompoundButton` heritage — **not** M3 switch anatomy; external labels in Elwha (§Open-3) `[DOC]` |

States: "Switches can be on or off. Switches have **enabled, hover, focused, and pressed** states." (No dragged state layer named for switch.) `[CODE]`

Implementation note: `MaterialSwitch` lives in package `materialswitch` — Java's reserved word forced the same package-name dodge Elwha needs (§P).

## §T. Token sheet — complete `md-comp-switch` v0.192 (verbatim)

Source: <https://raw.githubusercontent.com/material-components/material-web/main/tokens/versions/v0_192/_md-comp-switch.scss> (fetched 2026-06-10). This is the generated component-token set the m3.material.io spec page renders.

**Dimensions + shape `[CODE]`:**

| Token | Value |
|---|---|
| `track-width` | **52px** |
| `track-height` | **32px** |
| `track-outline-width` | **2px** |
| `unselected-handle-width/height` | **16px** |
| `selected-handle-width/height` | **24px** |
| `pressed-handle-width/height` | **28px** |
| `with-icon-handle-width/height` | **24px** |
| `selected-icon-size` / `unselected-icon-size` | **16px** |
| `state-layer-size` | **40px** |
| `touch-target-size` | 48px (hardcoded in the consumer sheet) |
| `handle-shape` / `track-shape` / `state-layer-shape` | `corner-full` |

**Selected colors `[CODE]`:**

| Token | Role |
|---|---|
| `selected-track-color` (incl. hover/focus/pressed) | `primary` |
| `selected-handle-color` | `on-primary` |
| `selected-hover-handle-color` | `primary-container` |
| `selected-focus-handle-color` | `primary-container` |
| `selected-pressed-handle-color` | `primary-container` |
| `selected-icon-color` (incl. hover/focus/pressed) | `on-primary-container` |
| selected track outline | **none** |

**Unselected colors `[CODE]`:**

| Token | Role |
|---|---|
| `unselected-track-color` (incl. hover/focus/pressed) | `surface-container-highest` |
| `unselected-track-outline-color` (incl. hover/focus/pressed) | `outline` |
| `unselected-handle-color` | `outline` |
| `unselected-hover-handle-color` | `on-surface-variant` |
| `unselected-focus-handle-color` | `on-surface-variant` |
| `unselected-pressed-handle-color` | `on-surface-variant` |
| `unselected-icon-color` (incl. hover/focus/pressed) | `surface-container-highest` |

**State layers `[CODE]`:**

| Token | Value |
|---|---|
| `selected-hover/focus/pressed-state-layer-color` | `primary` |
| `unselected-hover/focus/pressed-state-layer-color` | `on-surface` |
| state-layer opacities | the `md-sys-state` values → **Elwha's `StateLayer` enum governs** (HOVER 0.08 / FOCUS 0.10 / PRESSED 0.10 — the library-pinned set; resolved from UIManager at paint time) |

**Disabled `[CODE]`:**

| Token | Value |
|---|---|
| `disabled-selected-track-color` | `on-surface` @ `disabled-track-opacity` **0.12** |
| `disabled-selected-handle-color` | `surface` @ opacity **1.0** |
| `disabled-selected-icon-color` | `on-surface` @ **0.38** |
| `disabled-unselected-track-color` | `surface-container-highest` @ **0.12** |
| `disabled-unselected-track-outline-color` | `on-surface` (under the same 0.12 track opacity) |
| `disabled-unselected-handle-color` | `on-surface` @ **0.38** |
| `disabled-unselected-icon-color` | `surface-container-highest` @ **0.38** |

⚠️ Note the asymmetry: the **disabled selected handle is opaque `SURFACE`** (a "hole" in the 12% track), while the disabled unselected handle is 38% `ON_SURFACE`. Easy to get backwards; the gallery must show both.

**Derived geometry `[CODE]`:** handle-center travel = `track-width − track-height` = **20px** (centers at x=16 → x=36); state-layer overhang past the track box = `(40 − 32) / 2` = **4px** per side.

## §Mo. Motion internals (material-web style sheets, verbatim)

Sources: `switch/internal/_handle.scss`, `_track.scss`, `_icon.scss` (fetched 2026-06-10).

| What | Property | Duration | Easing |
|---|---|---|---|
| Handle **slide** (margin) | `margin` | **300ms** | `cubic-bezier(0.175, 0.885, 0.32, 1.275)` — **overshoot** (y₂ > 1) `[CODE]` |
| Handle **size** morph | `height, width` | **250ms** | standard easing `[CODE]` |
| Handle size while **pressed** | `height, width` | **100ms** | `linear` `[CODE]` |
| Handle/track **color** | `background-color` / `opacity, background-color` | **67ms** | `linear` `[CODE]` |
| Icon **fade** | `opacity` | **33ms** | `linear` `[CODE]` |
| Icon **transform** | `transform` | **167ms** | standard easing `[CODE]` |
| **Disabled** | everything | — | `transition: none` (snap) `[CODE]` |

- Slide distance: "margin equals track width minus track height" — confirms the 20px run.
- Icon transform detail: "When unselected **without an accompanying [unselected] icon**, the **on icon rotates −45deg into view**" — i.e. the rotation flourish belongs to **show-only-selected-icon** mode; both-icons mode is a plain crossfade. `[CODE]`
- The 67ms color snap is effectively "color arrives immediately" relative to the 300ms slide — see design §6 for the Elwha treatment (crossfade tied to slide progress; deviation accepted and documented).

## §S. States

From §C ("enabled, hover, focused, and pressed") + the §T state-layer block:

- **Hover / focus:** static state layer, 40dp circle **centered on the handle** (it travels with the slide). Tint `PRIMARY` when selected, `ON_SURFACE` when unselected. `[CODE]`
- **Pressed:** handle grows to 28dp + state layer; material-web bounds an `md-ripple` to the handle's 40dp circle → Elwha: `RipplePainter` press ripple clipped to the same circle, plus the static pressed layer fallback for gallery rendering. `[CODE]`
- **No dragged state layer** is specced for switch (unlike slider's `DRAGGED` 0.16) — drag shows the pressed treatment. `[CODE]`
- **Disabled:** §T-disabled colors, no state layers, no transitions. `[CODE]`

## §B. Behaviors

- **Click anywhere** on the switch toggles. `[CODE]`
- **Drag the handle:** the handle follows the pointer along the 20px run; release commits to the nearest side (which half of the track the handle center sits in). Pressed treatment (28dp + layer) holds during the drag. `[CODE]` (Behavior per the Android/web implementations — both `SwitchCompat` and `md-switch` support handle dragging.)
- **Keyboard:** Space toggles — press shows the pressed treatment, release commits (Swing button semantics). Enter is **not** bound (web `role="switch"` responds to Space; Enter is form-submit). Tab focuses the whole switch — there is no inner focus stop. `[CODE]`
- **Programmatic `setSelected`** animates when showing; never fires the user-gesture (`ActionListener`) event — mirrors material-web (`change` fires on user interaction only). `[CODE]`
- **RTL:** the switch mirrors — selected rests the handle at the **left** end under right-to-left `ComponentOrientation` (CSS `margin-inline-*` logical properties in material-web ⇒ direction-aware by construction). `[CODE]`

## §X. Accessibility

- Web: `role="switch"` + `aria-checked`; **always needs an explicit label** (§A). Android: framework `Switch` semantics (a checkable widget). `[CODE]`
- Swing has no SWITCH role → the `JToggleButton` accessible shape is the faithful mapping: **`AccessibleRole.TOGGLE_BUTTON`**, **`AccessibleState.CHECKED`** while selected, **`AccessibleAction`** ("click" → toggle), **`AccessibleValue`** (0/1). `[CODE]`
- Accessible name: `setLabel(String)` (the slider precedent) and `JLabel.setLabelFor(switch)` association. `[CODE]`
- Contrast: the unselected 2dp `OUTLINE` border is what keeps the unselected track ≥3:1 against `SURFACE` family backgrounds — don't drop it. `[DOC]`
- Touch target 48dp: [DOC] guidance on desktop; the component's whole bounds are clickable.

## §E. M3 Expressive status

Searched 2026-06-10. The May-2025 Expressive update's "14 new or updated components" list (button groups, FAB menu, split button, loading indicator, toolbars, …) does **not** include the switch; m3.material.io publishes a single current switch spec and no expressive-variant page. The press-grow / size-morph handle behavior already in the spec *is* the switch's shape-as-state-feedback story, consistent with [[project_elwha_m3_expressive]]. Sources: <https://m3.material.io/components/switch/specs>, <https://supercharge.design/blog/material-3-expressive>, <https://m3.material.io/blog/building-with-m3-expressive>. **No expressive delta to wait for; no excluded-variant stub epics to file.**

## §Tokens. Elwha token mapping — zero new tokens ✅

| M3 need | Elwha token | Notes |
|---|---|---|
| selected track / state-layer tint | `ColorRole.PRIMARY` | |
| selected handle | `ColorRole.ON_PRIMARY` | |
| selected hover/focus/pressed handle | `ColorRole.PRIMARY_CONTAINER` | |
| selected icon | `ColorRole.ON_PRIMARY_CONTAINER` | |
| unselected track / unselected icon | `ColorRole.SURFACE_CONTAINER_HIGHEST` | |
| unselected track outline / unselected handle | `ColorRole.OUTLINE` | |
| unselected hover/focus/pressed handle | `ColorRole.ON_SURFACE_VARIANT` | |
| unselected state-layer tint / disabled bases | `ColorRole.ON_SURFACE` | |
| disabled selected handle | `ColorRole.SURFACE` | opaque — §T ⚠️ |
| hover/focus/pressed opacities | `StateLayer.HOVER/FOCUS/PRESSED` | library-pinned values |
| disabled opacities 0.38 / 0.12 | `StateLayer.disabledContentOpacity()` / `disabledContainerOpacity()` | exact match |
| track/handle/state-layer shape | `ShapeScale.FULL` | corner-full pills/circles |
| slide / size morphs | `MorphAnimator` (300ms = `MEDIUM2_MS`; 250/100ms per-animator) + `Easing` | overshoot bezier as a component-local constant — `Easing.cubicBezier(0.175f, 0.885f, 0.32f, 1.275f)` |
| press ripple | `RipplePainter` | bounded to the 40dp handle circle |
| icons | `MaterialIcons.check()` / `MaterialIcons.close()` | both exist in the bundled 17; 16px via sized overloads |

## §P. Terminology → API lock (M3 nouns mirror rule)

| M3 noun | Elwha API |
|---|---|
| switch | `ElwhaSwitch` — package `com.owspfm.elwha.switches` (`switch` is a Java reserved word; MDC-Android hit the same wall → `materialswitch`. The plural mirrors Material's "Selection controls: switches" page name.) |
| selected | `isSelected()` / `setSelected(boolean)` |
| track / handle / icon | internal paint vocabulary + Javadoc nouns ("handle", not "thumb") |
| icons | `setIconsVisible(boolean)` / `isIconsVisible()` (Swing-idiomatic `Visible` suffix; `isIcons()` is not English) |
| show-only-selected-icon | `setShowOnlySelectedIcon(boolean)` / `isShowOnlySelectedIcon()` |
| (icon slots) | `setSelectedIcon(Icon)` / `getSelectedIcon()`, `setUnselectedIcon(Icon)` / `getUnselectedIcon()` — defaults `check(16)` / `close(16)` |
| `change` event (user commit) | `addActionListener(ActionListener)` |
| any state change | `addChangeListener(ChangeListener)` |
| label (a11y name) | `setLabel(String)` / `getLabel()` + `labelFor` association |

## §Open — questions carried into the design doc

1. **Architecture** — dedicated `JComponent` vs styled `JToggleButton`+`ButtonUI`: resolved RECOMMENDED-(A)-custom in design §2, locked via the S1 spike.
2. **Color motion** — M3's 67ms color snap vs crossfade-tied-to-slide-progress: design §6 picks the crossfade (deviation, documented); revisit at smoke if the read is wrong.
3. **Text label slot** — Android's `CompoundButton` text is heritage, not M3 switch anatomy; web has none. V1 = external labels + `setLabel` a11y name. A labeled switch-row composite is an M3 *list item* pattern — documented out-of-scope (design §10), no stub epic (nothing M3-switch-shaped is being cut).
4. **`ButtonGroup`/`ItemListener` interop** — skipped in V1 (switches are never radio-grouped; `ChangeListener`+`ActionListener` cover the events). Documented in design §10.

## §F. Capture log (web-first run — raw URLs, no screenshots)

| # | Source | What it contributed |
|---|---|---|
| 1 | `material-web/main/docs/components/switch.md` | §A API + a11y labelling rule + theming surface |
| 2 | `material-components-android/master/docs/components/Switch.md` | §C anatomy ("handle, formerly thumb"), attribute defaults, states list |
| 3 | `material-web/main/tokens/versions/v0_192/_md-comp-switch.scss` | §T complete token sheet (the authoritative values) |
| 4 | `material-web/main/switch/internal/_handle.scss` | §Mo slide 300ms overshoot bezier, size 250/100ms, color 67ms |
| 5 | `material-web/main/switch/internal/_track.scss` | §Mo track color/opacity 67ms; per-state track/outline painting |
| 6 | `material-web/main/switch/internal/_icon.scss` | §Mo icon 33/67/167ms + the −45° rotate-in; per-state icon colors |
| 7 | Web search (M3 Expressive switch, 2026-06-10) | §E — no expressive switch delta published |
