# ElwhaCheckbox — M3 spec research capture

**Status:** CAPTURED 2026-06-10 · Epic [#410](https://github.com/OWS-PFMS/elwha/issues/410) · feeds `elwha-checkbox-design.md`

Unlike prior captures, this one is **web-sourced end-to-end** (no operator screenshot pass): the
token values come from Material Web's published token sets, which are the same generated values the
`m3.material.io` spec pages render. Sources:

- Material Web component doc — `material-components/material-web/docs/components/checkbox.md`
- Material Web token set — `material-web/tokens/versions/v0_192/_md-comp-checkbox.scss` (concrete values)
- MDC-Android component doc — `material-components-android/docs/components/Checkbox.md`
- M3 spec pages (`m3.material.io/components/checkbox`) — JS-only, used as cross-check via search only

## §TL;DR

1. **No M3 Expressive fork.** The May-2025 Expressive update touched buttons/FABs/groups/sliders/
   progress; the checkbox keeps its baseline component spec. Expressive contributes only the
   spring **motion** vocabulary, which Elwha already encodes (`MorphAnimator`).
2. **Geometry:** container 18×18dp, corner radius 2dp, outline 2dp, icon fills the container,
   state layer 40dp circle, touch target 48dp.
3. **Three check states** (`unchecked` / `checked` / `indeterminate`) × orthogonal **error** flag ×
   enabled/hover/focus/pressed/disabled.
4. **Cross-color pressed state layers** — the spec detail easiest to miss: unselected-pressed uses
   `primary` while selected-pressed uses `on-surface` (each press previews the *destination*
   state's layer color). Hover/focus use the *current* state's color.
5. **Zero new Elwha tokens needed** — every color/type/opacity maps onto existing roles;
   sizes are component geometry constants (the `ElwhaSlider` precedent).

## §A — API shape (web + Android)

Material Web `<md-checkbox>`:

| Property | Type | Default | Notes |
|---|---|---|---|
| `checked` | boolean | false | |
| `indeterminate` | boolean | false | visual: dash; clicking an indeterminate box → **checked** |
| `disabled` | boolean | false | |
| `required` / `value` / `name` | form plumbing | — | web-form constraint validation (`checkValidity()` etc.) — **no Swing analogue** |

Events: `change` / `input` on user toggle (not on programmatic set).

MDC-Android `MaterialCheckBox`:

| Surface | Notes |
|---|---|
| `CheckedState` | `STATE_UNCHECKED` / `STATE_CHECKED` / `STATE_INDETERMINATE` — tri-state enum, parallel boolean conveniences |
| `app:errorShown` | **error is an orthogonal flag**, not a fourth check state; maps colors to `colorError`/`colorOnError`; `errorAccessibilityLabel` for announcements |
| `android:text` | built-in label, `textAppearanceBodyMedium` |
| `minWidth/minHeight` | `?attr/minTouchTargetSize` (48dp) |
| `centerIfNoTextEnabled` | label-less box centers in its bounds |

[CODE] Tri-state enum + boolean conveniences + orthogonal `errorShown` + optional label.
[DOC] Parent–child tri-state wiring (a parent checkbox reflecting children) is *app logic* in every
implementation surveyed — none of MDC/material-web automates the relationship. Javadoc recipe only.
[DROP] Web constraint-validation API (`required`/`checkValidity`) — form-submission concept with no
Swing analogue; dropped with this rationale rather than silently.

## §B — Token table (verbatim, material-web v0_192)

Sizing / shape:

| Token | Value |
|---|---|
| container-size | 18dp |
| container-shape | 2dp (corner radius) |
| icon-size | 18dp |
| state-layer-size | 40dp |
| state-layer-shape | corner-full (circle) |
| unselected outline-width | 2dp (all interactive states) |
| selected outline-width | 0dp (fill replaces outline) |

Colors — **unselected**:

| State | Outline | State layer |
|---|---|---|
| enabled | `on-surface-variant` | — |
| hover | `on-surface` | `on-surface` @ hover (0.08) |
| focus | `on-surface` | `on-surface` @ focus (0.10) |
| pressed | `on-surface` | **`primary`** @ pressed (0.10) ← cross-color |
| disabled | `on-surface` @ 0.38 | — |
| error (enabled/hover/focus/pressed) | `error` | `error` @ state opacity |

Colors — **selected** (checked *and* indeterminate — indeterminate is visually "selected"):

| State | Container | Icon | State layer |
|---|---|---|---|
| enabled | `primary` | `on-primary` | — |
| hover | `primary` | `on-primary` | `primary` @ 0.08 |
| focus | `primary` | `on-primary` | `primary` @ 0.10 |
| pressed | `primary` | `on-primary` | **`on-surface`** @ 0.10 ← cross-color |
| disabled | `on-surface` @ 0.38 | `surface` | — |
| error | `error` | `on-error` | `error` @ state opacity |

[CODE] All of the above. Note disabled-selected icon = `surface` (punches a "hole" through the
38% fill), not `on-surface`.

## §C — Elwha token mapping (zero new tokens)

| M3 need | Elwha token | Note |
|---|---|---|
| primary / on-primary | `ColorRole.PRIMARY` / `ON_PRIMARY` | |
| on-surface / on-surface-variant / surface | `ColorRole.ON_SURFACE` / `ON_SURFACE_VARIANT` / `SURFACE` | |
| error / on-error | `ColorRole.ERROR` / `ON_ERROR` | |
| hover/focus/pressed opacities | `StateLayer.HOVER` (0.08) / `FOCUS` (0.10) / `PRESSED` (0.10) | exact match |
| disabled 0.38 | `StateLayer.disabledContentOpacity()` | exact match |
| label type | `TypeRole.BODY_MEDIUM` | MDC `textAppearanceBodyMedium` |
| 18/2/2/40/48 geometry | component constants | slider precedent — geometry is not themable |
| ripple | shared `RipplePainter` | clipped to the 40dp circle |
| mark draw-in motion | `MorphAnimator` spring vocabulary | Expressive contribution |

`ShapeScale` deliberately **not** used for the 2dp container radius: `XS` (4px) is the smallest
step and would visibly over-round an 18px box. 2dp is spec-fixed component geometry.

## §D — Terminology → API lock (M3 nouns mirror rule)

| M3/MDC noun | Elwha API |
|---|---|
| checked | `isChecked()` / `setChecked(boolean)` |
| indeterminate | `isIndeterminate()` / `setIndeterminate(boolean)` |
| checked state (tri-state) | `CheckState { UNCHECKED, CHECKED, INDETERMINATE }` + `getCheckState()/setCheckState()` |
| error shown | `isErrorShown()` / `setErrorShown(boolean)` |
| label | `getLabel()` / `setLabel(String)` |

## §E — Behavior notes

- [CODE] Click cycle: `UNCHECKED→CHECKED`, `CHECKED→UNCHECKED`, `INDETERMINATE→CHECKED`
  (material-web behavior; indeterminate is exited by user interaction, entered only
  programmatically).
- [CODE] Space toggles when focused; full touch target (incl. label) is clickable.
- [CODE] The checkmark is **not a font/SVG glyph** in any first-party implementation — it's a
  stroked path so it can draw in/out (MDC animates stroke; material-web animates clip). Elwha
  paints a `Path2D` with round caps; `MaterialIcons.check()` rejected (would need per-state
  `ColorFilter` mutation — the #197 trap — and can't draw-in).
- [DOC] a11y: checkbox role, checked/indeterminate states announced; label provides the name;
  label-less boxes need an explicit accessible label (web `aria-label` ≙ Swing accessible name).

## §F — Open questions → resolved in design doc

1. Label gap geometry (M3 spec pages don't redline label placement) → §5 of design doc.
2. Focus indicator: state-layer only vs material-web's outward `md-focus-ring` → §6 of design doc.
3. Event surface: `ActionListener` vs typed listener → §8 of design doc.

## §Screenshot log

None — web-sourced capture (see header). If operator screenshots arrive later, append here and
reconcile; token values above are from the generated source of truth and should not drift.
