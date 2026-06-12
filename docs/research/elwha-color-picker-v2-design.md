# ElwhaColorPicker V2 — wheel · eyedropper · swatch tiers · popover · Phase 0 design

> **Decisions, not a catalog.** Mechanics and survey detail live in
> `elwha-color-picker-v2-research.md` (+ the V1 `elwha-color-picker-research.md` §D/§P/§X/§M and
> `elwha-color-picker-design.md`). This doc locks the architecture and the story breakdown.
>
> **Epic:** [#482](https://github.com/OWS-PFMS/elwha/issues/482) (ElwhaColorPicker V2). **Builds
> on:** [#481](https://github.com/OWS-PFMS/elwha/issues/481) (V1, shipped PR #492). **Milestone:**
> v0.5.0.

## TL;DR — the locks

1. **Five additive capabilities, V1 untouched API-wise:** WHEEL mode · eyedropper · theme-palette
   swatch tier · saved swatch tier (favorites) · `ElwhaColorPickerPopover`. HSL/CMYK stay
   not-planned.
2. **WHEEL = hue/saturation disc + value `ColorTrackSlider`** (macOS/Flutter lineage — mirrors
   SPECTRUM's 2D-surface + 1D-track structure). Fourth `PickerMode` constant; **default modes grow
   to all four** (SWATCHES, SPECTRUM, WHEEL, SLIDERS); the closed-set contract is unchanged. New
   bundled icon **`colors`** + `MaterialIcons.colors()`.
3. **Swatch tiers = a `SwatchSource { MATERIAL, THEME, SAVED }` sub-toggle inside the SWATCHES
   pane** (connected single-mandatory `ElwhaButtonGroup` — the V1 RGB/HSV precedent), *not* new
   top-level modes. `setSwatchSources(SwatchSource...)` subsets/reorders with exactly the
   `setModes` contract; single source hides the toggle.
4. **THEME tier = the live theme's 49 `ColorRole`s**, resolved at paint time (tracks palette +
   light/dark automatically); commits hand the picker **plain `Color` copies**, never the
   `ColorUIResource` from `resolve()` (the #495 lesson). `MaterialPalettes` tiers considered and
   dropped — palette-granularity, not color-granularity (research §B.3).
5. **SAVED tier = the favorites model on `ElwhaColorPicker`** — `getFavorites()` /
   `setFavorites(List<Color>)` / `addFavorite` / `removeFavorite` / `addFavoritesListener`,
   **capacity 30** (three grid rows), deduped. **Persistence is client-owned** (zero-coupling
   stance: no file/Preferences I/O in the lib); the listener + setter are the round-trip contract.
6. **Eyedropper = an opt-in header affordance, not a mode**: `setEyedropperEnabled(boolean)`
   (default **false**), surfacing an `ElwhaIconButton` (`colorize`) at the headline row's trailing
   edge. Activation opens the **frozen-capture screen sampler** (per-`GraphicsDevice`
   `Robot.createScreenCapture` in full-screen always-on-top windows) with a magnifier loupe +
   hex label; click/Enter commits (non-adjusting), Esc cancels, arrows nudge 1px. macOS
   Screen Recording permission is a `[DOC]` rule (silent wallpaper-only captures are not
   detectable); headless / `AWTException` → the affordance disables `[CODE]`.
7. **Popover = `ElwhaColorPickerPopover`**, the M3 *docked* analog: a package-private
   `AbstractElwhaOverlay` host (light-dismiss, `POPUP_LAYER` 300, dialog surface treatment) wrapped
   by a public class — the `menu/` package-boundary pattern. **Live commits, no action row**
   (Chrome/NSColorPanel consensus); Esc / outside-press / focus-escape dismisses without revert —
   revert remains the dialog's job. Placement = pure function: below-anchor, flip-above on clip,
   horizontal shift, RTL-mirrored.
8. **Height budget is a hard constraint:** every new pane/tier view stays within the V1
   tallest-card budget (~230px with alpha) so the operator-iterated dead band does not regrow.
   Concretely: **recent row lives in the MATERIAL view only**; THEME is toggle + 49-role grid;
   SAVED is toggle + favorites grid + action row; WHEEL's disc is **146px** tall (the SV-box
   budget).
9. **Zero new theme tokens.** One icon asset (`colors`). No new transitive deps.
10. **Single phase, stories S1–S7**; the final story's PR `Closes #482`.

## §1 Scope

**V2 ships ✅:** WHEEL pane (disc + value track + alpha + keyboard + hue preservation) ·
`SwatchSource` toggle + THEME tier · SAVED tier + favorites model/listener · eyedropper
(`setEyedropperEnabled` + screen sampler) · `ElwhaColorPickerPopover` · keyboard/a11y/RTL across
all of it · Showcase knobs + gallery rows · CHANGELOG.

**Not V2 ❌ (documented, not silent):** HSL/CMYK sub-modes (not planned, V1 §14) · NSColorPanel
drag-in/drag-out favorites DnD (replaced by explicit save/remove affordances) · live-tracking
eyedropper via global hooks (frozen capture only) · retina-resolution loupe sampling (`[DOC]`
note) · `MaterialPalettes` palette-tier swatches (wrong granularity, research §B.3) · Image/Pencils
NSColorPanel modes (no demand signal).

## §2 WHEEL pane (S1)

- `PickerMode.WHEEL("Wheel", "colors")` inserted **after SPECTRUM** in declaration + default order
  (the two freeform modes sit together). Default constructor offers all four; **four stacked
  icon-over-label tabs at 328px must be verified in the smoke loop** (V1 found three *inline* tabs
  truncate; stacked fit — re-check at four).
- Anatomy (top→bottom, V1 pane insets): **hue/sat disc** — 146px diameter, centered; hue = angle
  (0° at 3 o'clock, counter-clockwise positive, the `atan2`/HSB convention), saturation = radius.
  1px OUTLINE_VARIANT circular border. Thumb = the V1 SV-box ring (16px, 2px SURFACE + 1px OUTLINE)
  at (h,s). Press/drag anywhere in the disc repositions (radius clamped to 1.0; adjusting during
  drag). Then the **value `ColorTrackSlider`** (0–100, black→full at current h,s) and the alpha
  track when enabled — SPECTRUM's exact row structure, so the card height matches SPECTRUM
  (≈230 alpha / 198 not).
- Paint: full-saturation disc rendered once per size into a cached `BufferedImage`; current value
  applied by compositing translucent black at alpha (1 − v) — exact for HSV (research §B.1). Disc
  edge AA'd in the image + border stroke; **no `clip()`** (V1 lesson).
- **Hue preservation invariant extends verbatim** (V1 §6): pane owns float `h,s` while commit
  source; external sync keeps `h` at s=0, keeps `h,s` at v=0. The wheel and spectrum panes never
  fight — only the commit source writes.
- Keyboard (disc focusable, polar — the natural wheel axes): Left/Right hue ∓/+1°,
  Up/Down saturation +/−0.01, PgUp/PgDn hue ±10°, Home/End saturation 0/1. `StateLayer.FOCUS`
  treatment on the thumb. **No RTL mirroring of the wheel itself** — hue angle is a fixed
  convention (macOS/GIMP do not mirror); the value/alpha tracks mirror like every
  `ColorTrackSlider`.
- A11y: role PANEL, name "Hue and saturation wheel", description = current hue°/sat% summary —
  the V1 SV-box pattern.

## §3 Swatch tiers (S2 THEME · S3 SAVED)

- **`SwatchSource` top-level enum** in `colorpicker/` (`MATERIAL("Material")`, `THEME("Theme")`,
  `SAVED("Saved")`) — the `PickerMode` house pattern. `setSwatchSources(SwatchSource...)` /
  `getSwatchSources()` / `getSwatchSource()` / `setSwatchSource(SwatchSource)` mirror the
  `setModes` family contract exactly (non-empty, no nulls/duplicates, first = active, single
  hides the toggle). Source switching never mutates the color.
- The toggle row is a connected single-mandatory `ElwhaButtonGroup` at the top of the SWATCHES
  pane (the SLIDERS RGB/HSV precedent), `CardLayout` beneath for the three tier views. The V1
  hue-grid/shade-strip/recent stack becomes the MATERIAL card unchanged.
- **Height budget (locked):** MATERIAL = toggle + hue grid + shade strip + recent ≈ 222px ·
  THEME = toggle + 49-role grid (10 columns × 5 rows at the V1 34px pitch) ≈ 222px · SAVED =
  toggle + favorites grid (3 rows) + action row ≈ 210px. All ≤ the ~230px budget — **the recent
  row stays MATERIAL-only by design** (one toggle-click away; THEME/SAVED need the vertical
  budget). Said out loud: this is a layout decision, not a scope cut.
- **THEME tier:** a `CellStrip` grid of all 49 `ColorRole`s in declaration order (primary family →
  surfaces → outlines → inverse → fixed). Cells resolve at **paint time** (live theming); commit =
  plain copy (`new Color(role.resolve().getRGB(), true)`). Selection indicator = the V1
  ring + luminance check via `matchesCurrent`. Accessible cell names "Primary container ·
  #EADDFF" (prettified constant + live hex).
- **SAVED tier:** favorites grid (`CellStrip`, up to 30 cells; empty state paints "No saved colors
  yet" hint, LABEL_MEDIUM ON_SURFACE_VARIANT) + an action row with an `ElwhaButton` text button
  "Save current" (leading `star` glyph) that appends `getColor()` (deduped — saving an existing
  favorite is a no-op). **Removal:** Delete/Backspace on the focused cell `[CODE]`, and a
  right-click `ElwhaMenu` context menu ("Remove from saved") on cells — dogfood. Favorites store
  **on the picker** (like the recent MRU) so they survive pane rebuilds; `setFavorites` copies,
  dedupes, truncates to 30.
- Favorites events: `addFavoritesListener(ChangeListener)` / `removeFavoritesListener` fire on any
  list mutation (save, remove, `setFavorites`); the Javadoc shows the persistence round-trip
  (read on listener → write to the client's store; restore via `setFavorites` at construction).

## §4 Eyedropper (S4)

- `setEyedropperEnabled(boolean)` / `isEyedropperEnabled()`, default **false** (the alpha-gate
  precedent). On: `ColorPickerHeader` gains its first child — an `ElwhaIconButton`
  (`MaterialIcons.colorize()`, standard variant) docked at the headline row's trailing edge,
  accessible name "Pick color from screen"; header paint reserves its box. Disabled with the
  picker; auto-disabled `[CODE]` when `GraphicsEnvironment.isHeadless()` or Robot construction
  throws.
- **`ScreenSampler`** (package-private): on activate, captures every `GraphicsDevice` via
  `Robot.createScreenCapture(device bounds)` and opens one undecorated always-on-top full-screen
  `JWindow` per device painting its frozen capture. Pointer drives a **loupe**: 11×11 logical-pixel
  grid magnified ~8×, OUTLINE grid lines, SURFACE ring, center cell highlighted, hex label
  (LABEL_MEDIUM on a SURFACE chip) beneath; loupe flips quadrant near screen edges. Crosshair
  cursor.
- Gestures: **press commits** (the V1 press-activation rule — macOS drops MOUSE_CLICKED) as a
  single non-adjusting commit (recent row updates) and closes all sampler windows; **Esc** cancels
  (no commit); **arrows** nudge the sample point 1px (loupe follows); **Enter** commits the
  current point. Alpha: sampled colors are opaque; current alpha is preserved when
  `isAlphaEnabled()` (the V1 swatch-pick rule).
- The sampler is **pure-geometry testable**: sampling/loupe math takes (BufferedImage, Point) — the
  headless smoke drives that seam with a synthetic image; the windowed path is exercised by the
  demo. macOS Screen Recording permission documented in the class + package Javadoc `[DOC]`.
- Dialog/popover interplay: the sampler windows are toplevel and outrank the in-window overlays;
  on commit/cancel, focus returns to the picker. The popover must **not** light-dismiss from the
  sampler's synthetic focus loss — the sampler sets a suppression latch on its owning picker's
  overlay while open (verified in the popover smoke).

## §5 `ElwhaColorPickerPopover` (S5)

- Public final class, **composes** a package-private `PopoverHost extends AbstractElwhaOverlay`
  (the `menu/` boundary pattern; `AbstractElwhaOverlay` stays non-API). Host: `lightDismiss()`
  true, `overlayLayer()` `POPUP_LAYER`, `takesFocus()` true, dialog surface treatment
  (SURFACE_CONTAINER_HIGH fill, XL corners, level-3 `ShadowPainter` shadow) wrapping an embedded
  `ElwhaColorPicker` (supporting text suppressed by default — the anchor names the task).
- API: `new ElwhaColorPickerPopover()` + setters mirroring the dialog (`setInitialColor`,
  `setModes`, `setSwatchSources`, `setAlphaEnabled`, `setEyedropperEnabled`) +
  `addChangeListener` (delegates to the embedded picker — **live commits**) +
  `onDismiss(Runnable)` + `show(Component anchor)` / `close()` / `isShowing()`. Reopening
  re-stages via `setInitialColor` like the dialog.
- **Commit model (locked):** live while open; dismissal commits nothing further and reverts
  nothing (Chrome `input`/`change` consensus, research §B.5). Documented loudly on the class:
  pending-until-OK is `ElwhaColorPickerDialog`.
- Placement (pure, unit-tested): surface below the anchor, leading edges aligned, `SpaceScale.XS`
  gap; flips above when below clips the layered-pane bounds; shifts horizontally to stay inside;
  RTL aligns trailing edges. Width = the picker's 328px + surface padding.
- Esc closes (WHEN_FOCUSED binding scoped to the surface, the menu Esc lesson); outside-press and
  focus-escape close via the host's light-dismiss; `onDismiss` runs once per show, any cause.

## §6 Keyboard + a11y completeness (S6)

- Tab order extensions: SWATCHES = source toggle → tier stops (MATERIAL keeps the V1 three;
  THEME = grid; SAVED = grid → "Save current" button) · WHEEL = tabs → disc → value (→ alpha) ·
  header eyedropper button joins after the tabs when enabled · popover = picker stops, focus
  restored to the anchor on close.
- Roles/names: disc + sampler per §2/§4; tier cells per §3; `SwatchSource` toggle inherits
  `ElwhaButtonGroup` semantics; popover surface announces the picker's accessible name on open
  (overlay host behavior).
- RTL: toggle rows, grids, tracks, and popover placement mirror; the wheel's hue angle does not
  (§2). Disabled: every new interactive surface gates through outer-qualified `isEnabled()`
  (lesson #432) and paints at `disabledContentOpacity()`.

## §7 States & motion

- Hover/focus/pressed `StateLayer` on tier cells and the disc thumb cursor — V1 treatment
  verbatim. Popover entrance/exit = the overlay host's `MorphAnimator` scale/fade (menu timing),
  reduced-motion snap for free. The sampler has no entrance motion (a frozen screen must not
  animate). No new morphs.

## §8 Showcase (S7)

- Workbench: the mode-subset row grows a **Wheel** `ElwhaCheckbox`; new **Eyedropper**
  `ElwhaCheckbox`; swatch-source subset `ElwhaCheckbox` ×3; an `ElwhaButton` "Open popover"
  anchored to itself (live-writes the workbench picker); favorites seeded with two demo colors so
  SAVED renders non-empty. Generated-code panel mirrors all new knobs. All knobs Elwha — dogfood.
- Gallery: WHEEL standalone · THEME tier · SAVED tier (seeded) · eyedropper-enabled header ·
  popover open-state is Workbench-only (overlays don't gallery — V1 dialog precedent).
- Headless `ColorPickerShowcaseSmoke` extended for the new knobs.

## §9 Phasing → stories (V2 = #482, single phase)

| S | Story | Locks |
|---|---|---|
| S1 | WHEEL pane + `colors` icon + default-mode growth | §2 |
| S2 | `SwatchSource` toggle + THEME tier | §3 (architecture: the tier host) |
| S3 | SAVED tier + favorites model/listener/persistence contract | §3 |
| S4 | eyedropper — header affordance + `ScreenSampler` | §4 |
| S5 | `ElwhaColorPickerPopover` + placement fn | §5 |
| S6 | keyboard + a11y + RTL completeness sweep | §6 |
| S7 | Showcase + CHANGELOG (**closes #482**) | §8 |

Dependency order: S3 ← S2 (the toggle host lands in S2); S6 ← S1–S5; S7 ← S6. S1/S2/S4/S5 are
independent. Each story: fresh demo class + headless smoke guard, `@version v0.5.0`, one commit
([[feedback_fresh_demo_per_story]] cadence).

## §10 Open for the build / smoke loop

- Four stacked tabs at 328px (§2) — verified visually in the smoke loop; fallback is
  abbreviating no labels but letting `ElwhaTabs` shrink stacked padding (escalate to the operator
  if anything must truncate).
- Disc keyboard polar-step feel (1°/0.01) — tune in smoke if too fine/coarse.
- Sampler loupe magnification (8×) and grid extent (11×11) — visual smoke knobs.
- Popover-over-dialog stacking (popover from a dialog-hosted picker) — POPUP_LAYER outranks
  MODAL_LAYER by design (the menu-over-dialog precedent); smoke-verify, not redesign.
