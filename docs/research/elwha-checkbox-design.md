# ElwhaCheckbox — Phase 0 design

**Status:** LOCKED 2026-06-10 · Epic [#410](https://github.com/OWS-PFMS/elwha/issues/410) ·
research capture: `elwha-checkbox-research.md`

## TL;DR — the locks

1. **One `ElwhaCheckbox extends JComponent`** in `com.owspfm.elwha.checkbox` — dedicated primitive
   (the `ElwhaSlider` template), never a styled `JCheckBox`.
2. **Tri-state enum** `CheckState { UNCHECKED, CHECKED, INDETERMINATE }` + boolean conveniences;
   API mirrors M3 nouns (`checked` / `indeterminate` / `errorShown` / `label`).
3. **Zero new theme tokens.** Colors/opacities/type map onto existing roles; 18/2/2/40/48
   geometry is component constants (slider precedent).
4. **Hand-painted marks** — checkmark + indeterminate dash are stroked `Path2D`s, enabling the
   draw-in animation and per-state coloring without the `ColorFilter` mutation trap (#197).
5. **Token-exact state layers including the cross-color pressed rule** (unselected-pressed
   `PRIMARY`, selected-pressed `ON_SURFACE`).
6. **Focus = focus state layer only** (keyboard-only focus-visible, slider pattern); no outward
   focus ring in V1.
7. **Ripple centered in the 40px circle** via shared `RipplePainter` (selection-control
   convention: center-origin, not press-point).
8. **`ActionListener` on user toggle + `PropertyChangeEvent("checkState")` on every change** —
   the `ElwhaButton` event pattern.
9. **Single phase, five stories (#411–#415), single stacked PR.** S5 is the Showcase
   `JCheckBox` dogfood sweep the library has owed since the dogfood rule landed.
10. **No S1 architecture spike needed** — the slider already litigated custom-`JComponent`-vs-
    Swing-subclass for stateful controls; checkbox is strictly simpler. S1 is a plain build story.

## §1 Scope

V1 = the complete M3 checkbox: tri-state, error flag, label, full interaction/motion/a11y,
Showcase leaf, dogfood sweep. M3 Expressive has no checkbox variant fork (research §TL;DR-1), so
there is no excluded-variant stub to file. Radio and Switch are sibling components → future epics.

## §2 Architecture

`extends JComponent`. State is four fields: `checkState`, `errorShown`, `label`, plus transient
interaction state (`hovered`, `pressed`, `focusVisible`, ripple progress, mark-motion progress).
No Swing `ButtonModel` — its armed/pressed semantics buy nothing here and drag in `ButtonGroup`
coupling we don't want (mutex is a radio concern). Repaint-on-set for every visual property;
all tokens resolved at paint time, never cached.

## §3 Anatomy

```
          ┌────────────────────────────── touch target ≥ 48×48
          │   ┌────────────────────────── state-layer circle 40×40 (hover/focus/press/ripple)
          │   │     ┌──────────────────── container 18×18, r=2, centered in the circle
          │   │     │      ┌───────────── mark: checkmark / dash, stroked, ON_PRIMARY
          ▼   ▼     ▼      ▼
        [ ( [ ✓ ] ) ]   Label text      ← optional, BODY_MEDIUM, ON_SURFACE
```

Parts: (1) touch target, (2) state-layer circle, (3) container (outline when unchecked, fill when
checked/indeterminate), (4) mark, (5) optional label.

## §4 Tokens (zero new)

Full mapping in research §C. Summary: `PRIMARY`/`ON_PRIMARY` selected, `ON_SURFACE_VARIANT`
outline, `ERROR`/`ON_ERROR` error, `StateLayer.HOVER/FOCUS/PRESSED` opacities,
`StateLayer.disabledContentOpacity()` (0.38), `TypeRole.BODY_MEDIUM` label.

## §5 Geometry & layout (component constants, px)

| Constant | Value | Source |
|---|---|---|
| `CONTAINER_SIZE` | 18 | token container-size |
| `CONTAINER_ARC` | 4 | token container-shape 2dp radius → arc-diameter convention ([[project_corner_radius_convention]]) |
| `OUTLINE_WIDTH` | 2 | token outline-width |
| `STATE_LAYER_SIZE` | 40 | token state-layer-size |
| `TOUCH_TARGET` | 48 | M3 target / WCAG, matches `ElwhaButton` XS inflation |
| `LABEL_GAP` | label leading edge sits at x=48 (the touch-target edge) | Android `CompoundButton` 48dp-min precedent; ~15px visual gap from the container edge |
| `LABEL_TRAILING_PAD` | 4 (`SpaceScale.XS`) | breathing room in preferred width |

Preferred size: `48×48` label-less; with label `48 + labelWidth + 4` × `max(48, labelHeight)`.
Box block is leading-aligned, label vertically centered on the container center; `getBaseline`
returns the label baseline when present. RTL: box trails right, label leftward — V1 honors
`ComponentOrientation` by mirroring the x-layout.

**Marks** (in 18×18 container coords): checkmark = polyline (4.4, 9.3) → (7.4, 12.3) →
(13.6, 6.1), stroke 2, round cap/join — start/short-arm/long-arm proportions match the M3
glyph. Indeterminate dash = (4.5, 9) → (13.5, 9), stroke 2, round caps. Exact coordinates are
eye-tuned in S1's demo; the doc records the shipped values if they shift.

## §6 Colors & states

The research §B tables are the contract, verbatim. The two rules implementations get wrong:

- **Pressed cross-color:** the press layer previews the *destination* state — unselected-pressed
  paints `PRIMARY`, selected-pressed paints `ON_SURFACE`. Hover/focus use the *current* state's
  color (`ON_SURFACE` when unchecked, `PRIMARY` when selected).
- **Disabled-selected mark is `SURFACE`** (not `ON_SURFACE`, not alpha'd) over the 0.38
  `ON_SURFACE` container.

Error replaces the chromatic role wholesale (`ERROR` outline/fill/layers, `ON_ERROR` mark);
disabled wins over error. Indeterminate colors identically to checked everywhere.

Focus: keyboard-only `focusVisible` (gained via Tab ⇒ show layer; mouse press takes focus without
the layer — slider pattern). No outward focus ring in V1; if a later epic adds a lib-wide
focus-ring affordance the checkbox adopts it then (deferral noted, not silent).

## §7 Motion

- **Ripple:** shared `RipplePainter`, component-owned 400ms timer, **origin = container center**,
  clipped to the 40px circle (`cornerRadiusPx = STATE_LAYER_SIZE` ⇒ circular clip).
- **Mark draw-in:** `MorphAnimator`-driven progress 0→1. Checkmark reveals by stroke length
  (`BasicStroke` dash array sized to path length, dash phase by progress) — the M3 "draw" gesture.
  Dash grows from center outward. Mark-out reverses the reveal. Container fill fades with the
  same progress. Programmatic `setCheckState` outside a realized/showing hierarchy snaps without
  animating.
- Honors the lib's reduced-motion behavior to the extent `MorphAnimator` provides it.

## §8 Behavior & events

- Click cycle `UNCHECKED→CHECKED→UNCHECKED`; `INDETERMINATE→CHECKED`. Press arms on
  `mousePressed` (the macOS rapid-click lesson, [[project_card_supporting_text_repaint_storm]]),
  toggles on release inside.
- Whole component (incl. label) is the click target.
- Space toggles when focused. Tab/Shift-Tab traversal native.
- `addActionListener` — fires on **user-driven** toggles only. `PropertyChangeEvent
  "checkState"` — fires on every change including programmatic (group/parent-child wiring hook).
- Disabled: no listeners fire, hover/press/ripple cleared and suppressed.

## §9 Accessibility

`AccessibleRole.CHECK_BOX`; state set gains `AccessibleState.CHECKED` when checked and
`AccessibleState.INDETERMINATE` when indeterminate (both standard `javax.accessibility` states).
Accessible name from the label; `setAccessibleLabel(String)` hook for label-less boxes (slider's
`setLabel` precedent, renamed to avoid colliding with the visual `setLabel`). Error state surfaces
via accessible description (`"Error"` prefix + optional custom text — MDC's
`errorAccessibilityLabel` analogue folded into `setAccessibleLabel`'s sibling
`setErrorAccessibleText(String)` if needed; V1 ships description = "error" when flagged).

## §10 Label

`BODY_MEDIUM`, `ON_SURFACE` (0.38 content opacity when disabled). Single-line, no wrapping —
checkbox labels are short by guideline; long text truncates with ellipsis at the component's
given width (no HTML, avoids the WrappingLabel storm class, [[project_card_supporting_text_repaint_storm]]).

## §11 Showcase

Standard leaf: `CheckboxShowcasePanels.buildWorkbench()` (live checkbox; controls: tri-state
selector, label text, error, enabled; synced code view) + `buildGallery()` (states ×
plain/labeled/error/disabled matrix) + `LeafEntry` in `populateCatalog()`. Then the **S5 dogfood
sweep**: every raw `JCheckBox` in Showcase/workbench/playground control panels swaps to
`ElwhaCheckbox` — the library finally satisfies its own dogfood rule for boolean controls.

## §12 Phasing → stories (single phase)

| Story | # | Ships |
|---|---|---|
| S1 | #411 | Core primitive: geometry, tri-state model, painting, disabled · `CheckboxStateMatrixDemo` |
| S2 | #412 | Interaction & motion: state layers, ripple, keyboard, draw-in · `CheckboxInteractionDemo` |
| S3 | #413 | Label, error, a11y, baseline · `CheckboxLabelErrorDemo` |
| S4 | #414 | Showcase workbench + gallery + sidebar leaf · `CheckboxShowcaseSmoke` |
| S5 | #415 | Dogfood sweep `JCheckBox`→`ElwhaCheckbox` · `CheckboxDogfoodSweepSmoke` |

## §13 Out of scope (deferrals, not cuts)

- **Radio / Switch** — sibling selection controls, future epics (checkbox API sets their template).
- **Parent–child tri-state wiring** — app logic everywhere upstream; ships as a Javadoc recipe on
  `setIndeterminate`.
- **Web constraint-validation API** (`required`/`checkValidity`) — no Swing analogue (research §A).
- **Outward focus ring** — adopted if/when a lib-wide focus-ring affordance lands (§6).
- **Checkbox-in-list selection patterns** — belongs to the `ElwhaList` families (epic #252 turf).
