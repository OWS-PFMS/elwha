# ElwhaColorPicker — Phase 0 design

**Status:** LOCKED (epic #481 · V2 stub #482) · 2026-06-11 · research: `elwha-color-picker-research.md`
**Operator constraints:** several fixed ways to pick/define colors (JColorChooser lineage); **no client-pluggable panels**; UI/UX designed in-house from the synthesized M3 picker grammar; dogfood Elwha components throughout.

## TL;DR — locked decisions

1. **Two public classes**: `ElwhaColorPicker` (inline composite, live selection) + `ElwhaColorPickerDialog` (modal wrapper, pending-until-OK), package `com.owspfm.elwha.colorpicker`.
2. **Closed mode set**: `ElwhaColorPicker.PickerMode { SWATCHES, SPECTRUM, SLIDERS }`. `setModes(PickerMode...)` may subset/reorder, never extend. Single mode ⇒ tab bar hidden.
3. **Mode switch = `ElwhaTabs`** (primary variant, inline icon + label) — M3's own 3+-view switcher and the JColorChooser lineage. Icons: `palette` / `gradient`(new) / `tune`(new).
4. **M3 picker grammar header**: supporting text (LABEL_LARGE, ON_SURFACE_VARIANT, default "Select color", `setSupportingText(null)` hides) + headline row = 40×40 preview swatch (SM corners, OUTLINE_VARIANT 1px border) + hex readout (HEADLINE_LARGE, ON_SURFACE) + divider (OUTLINE_VARIANT).
5. **Gradient tracks are a dedicated package-private primitive** (`ColorTrackSlider`, M3 slider geometry: 16px FULL-corner track, 6px FULL-corner bar handle in SURFACE with 1px OUTLINE border) — **not** an `ElwhaSlider` extension. Said loudly: this is the one place dogfooding yields to anatomy; gradient-track paint stays off the finished shared primitive.
6. **Selection model**: `getColor()/setColor(Color)` (never null), `addChangeListener`, `isAdjusting()` during drags (ElwhaSlider precedent). Dialog stages and commits on OK; Cancel/Esc discards.
7. **Alpha is opt-in** (`setAlphaEnabled`, WinUI precedent): alpha track (checkerboard backing) in SPECTRUM + SLIDERS, `#RRGGBBAA` web-order hex, checkerboard preview, headline drops to HEADLINE_MEDIUM.
8. **Zero new theme tokens** (research §E table). Checkerboard greys deliberately untokenized.
9. **Hue-preservation invariant**: panes that edit in HSV keep float h/s/v as source of truth while active — RGB roundtrips at s=0/v=0 must not snap hue to 0 (classic picker bug, see §6).
10. **V2 (#482)**: wheel mode, eyedropper, theme-palette swatches, favorites persistence, docked/popover presentation. HSL/CMYK slider sub-modes: not planned.

## §1 Scope

**V1 ships ✅:** inline picker (header + tabs + 3 panes) · Material swatch catalog with hue grid → shade strip → recent row · SV-square + hue slider spectrum · RGB/HSV channel sliders + validated hex field · modal dialog with pending semantics · opt-in alpha · keyboard + a11y · Showcase Workbench/Gallery · CHANGELOG.
**Not V1 ❌ (filed #482, not silent):** wheel mode · eyedropper · theme-palette/`MaterialPalettes` swatch tiers · persistent favorites · docked/popover (anchored) presentation.

## §2 Architecture (load-bearing; S1 spike locks)

- `ElwhaColorPicker extends JComponent` — a **composite container** (SelectField precedent), not a painted leaf: BorderLayout of header / `ElwhaTabs` / CardLayout pane host. Background = transparent (host surface shows through); the **dialog** supplies SURFACE_CONTAINER_HIGH + XL shape + elevation; inline embeds sit on whatever surface the app provides.
- Package-private sub-primitives: `ColorPickerHeader`, `SwatchesPane`, `SpectrumPane`, `SlidersPane`, `ColorTrackSlider`, `MaterialSwatchCatalog`, `Checkerboard` (static painter util).
- **Single commit path**: every pane edit funnels through one internal `commit(Color, boolean adjusting, Object source)`; a reentrancy guard suppresses echo while `setColor` fans back out to panes. Panes other than `source` resync silently.
- `ElwhaColorPickerDialog` **composes** the public `ElwhaDialog` (has-a). RECOMMENDED, locked by S5: `AbstractElwhaDialog` is package-private in `dialog/` — composition avoids widening a shared host. Dialog headline carries the task title; the picker's own supporting text is suppressed to avoid double titles.
- Disabled: `setEnabled(false)` cascades; all interaction (incl. keyboard) gated through outer-qualified `ElwhaColorPicker.this.isEnabled()` (lesson #432); content at `StateLayer.disabledContentOpacity()`.

## §3 Mode host

- `ElwhaTabs`, primary variant, inline icon+label tabs in `setModes` order: Swatches (`palette`) / Spectrum (`gradient`) / Sliders (`tune`). Two new Material Symbols (gstatic, Rounded/400/fill0/opsz20) + `MaterialIcons` factories — house pipeline.
- `getMode()/setMode(PickerMode)` reflect the active tab; switching never mutates the color (state preserved per pane — M3 date picker preserves across calendar↔input).
- Default mode = first of `setModes` (default order SWATCHES, SPECTRUM, SLIDERS — quick-pick first, JColorChooser/Chrome convention).

## §4 Header

- Padding `SpaceScale.LG`; supporting text row; headline row gap `SpaceScale.MD`; divider below.
- Preview swatch 40×40, `ShapeScale.SM`, 1px OUTLINE_VARIANT border; checkerboard behind when `alphaEnabled && alpha<255`.
- Hex readout: uppercase `#RRGGBB` / `#RRGGBBAA` (web byte order — research §M), HEADLINE_LARGE → HEADLINE_MEDIUM when alpha enabled (9 glyphs in 328px). Readout is display-only; *entry* lives in SLIDERS (M3 separates display headline from input mode).

## §5 SWATCHES pane (S2)

- `MaterialSwatchCatalog`: the 19 Material-2014 hues + a 10-step monochrome ramp (white→black) = 20 hue entries × 10 shades (50–900), hardcoded constants; catalog is package-private.
- Layout (top→bottom, `SpaceScale.MD` gaps): **hue grid** — 20 circular cells (32px ∅ on a 40px pitch; whole cell is the hit target), 10 per row × 2 rows, each showing shade 500; **shade strip** — the active hue's 10 shades as one connected FULL-corner segmented strip (segments ~28px tall, hairline gaps); **recent row** — label "Recent" (LABEL_MEDIUM, ON_SURFACE_VARIANT) + up to 10 24px circles, MRU, deduped, fed by *non-adjusting* commits from any pane.
- Hue cell click → commits that hue's 500 + repopulates the shade strip. Shade click → commits exactly. Cell equal (ARGB, alpha-ignored when alpha disabled) to the current color → selected indicator: 2px PRIMARY ring + luminance-picked black/white `check` glyph. Swatch picks **preserve current alpha** when alpha is enabled (WinUI behavior).
- Design decision (not a cut): the date-picker "today" thin-ring analog for the *initial* color is dropped — two ring weights on colored cells read as noise; the dialog's Cancel covers "get back".
- Keyboard: hue grid, shade strip, recent row = three tab stops; arrows move a painted focus cursor (`StateLayer.FOCUS`), Space/Enter commits; Up/Down cross grid rows.

## §6 SPECTRUM pane (S3)

- **SV box**: full pane width, 168px tall, SM-corner clip, 1px OUTLINE_VARIANT border. x = saturation 0→1, y = value 1→0. Backing `BufferedImage` cached per (hue, size). Thumb = 16px ring (2px SURFACE ring + 1px OUTLINE inner ring) centered on (s,v); press/drag anywhere repositions (adjusting during drag).
- **Hue `ColorTrackSlider`** below (0–360, six-stop rainbow gradient). Alpha track appended when enabled (S6).
- **Hue preservation**: pane owns float `h,s,v` while it is the commit source; external `setColor` resyncs h/s/v from the Color (s=0 keeps previous h; v=0 keeps previous h,s). The SV box hue never snaps to red when dragging through greys.
- Keyboard: SV box focusable — arrows ±0.01 s/v, PgUp/PgDn ±0.10; sliders per §7 bindings.

## §7 SLIDERS pane (S4)

- Sub-toggle: connected `ElwhaButtonGroup`, segments "RGB" / "HSV", single-mandatory, default RGB.
- `ColorTrackSlider` rows: label (LABEL_LARGE, 16px col) + track + value (BODY_MEDIUM, right-aligned 40px col). RGB: R/G/B 0–255, each track sweeps its channel with the others held current. HSV: H 0–360 rainbow, S 0–100 (grey→full at current h,v), V 0–100 (black→full at current h,s).
- Slider bindings: Left/Right ±1 · PgUp/PgDn ±10 · Home/End min/max. Drag = adjusting; release commits.
- **Hex `ElwhaTextField`** (outlined, label "Hex"): accepts `#RRGGBB`, `#RGB` (expanded), + `#RRGGBBAA`/`#RGBA` when alpha enabled; '#' optional on entry, normalized uppercase-with-# on commit. Commit on Enter and focus-loss; invalid → error state + supporting text (`Use #RRGGBB` / `Use #RRGGBB or #RRGGBBAA`), reverts on focus-loss if still invalid.

## §8 `ElwhaColorPickerDialog` (S5)

- Has-a `ElwhaDialog`: headline = title (default "Select color"), content = an `ElwhaColorPicker` (supporting text suppressed), actions = **Cancel** / **OK** text actions (trailing, OK last — M3 action row), Esc enabled ⇒ cancel path.
- Pending semantics: edits stay in the embedded picker; **OK** invokes `Consumer<Color> onConfirm` with the final color; Cancel/Esc invokes optional `Runnable onCancel`, no color callback. Reopening with `setInitialColor` re-stages.
- Construction: `new ElwhaColorPickerDialog()` + setters (`setTitle`, `setInitialColor`, `setModes`, `setAlphaEnabled`, `onConfirm`, `onCancel`) + `show(Component parent)`. Static convenience `ElwhaColorPickerDialog.show(Component parent, String title, Color initial, Consumer<Color> onConfirm)` — the `JColorChooser.showDialog` ergonomic, **non-blocking** (in-window overlay; callbacks, not return values).
- Width: picker's 328px content + dialog's own padding; height per pane.

## §9 Alpha (S6)

- `setAlphaEnabled(boolean)`, default false. On: alpha `ColorTrackSlider` (0–255, transparent→opaque sweep over `Checkerboard`) appended in SPECTRUM and SLIDERS (both sub-modes); hex grammar grows per §7; preview + relevant tracks get checkerboard backing; headline per §4. Off: `setColor` strips alpha to 255.
- `Checkerboard.paint(g, w, h, 8)` — 8px `0xFFFFFF`/`0xCCCCCC` squares (untokenized by design, research §E).

## §10 Keyboard + a11y (S7)

- Tab order: tabs → active-pane stops (per §5–§7) → (dialog) action buttons. Focus visuals via `StateLayer.FOCUS` cursors/rings, matching checkbox/radio/tab precedent.
- `AccessibleRole.COLOR_CHOOSER` on `ElwhaColorPicker`; accessible name = supporting text; hex readout exposed as the picker's `AccessibleValue`-adjacent description (name carries current hex). Swatch cells: names "Red 500 · #F44336" (catalog) / hex (recent). `ColorTrackSlider`: `AccessibleRole.SLIDER` + `AccessibleValue` + channel name. SV box: role PANEL with name "Saturation and value" + value summary.
- ChangeListener contract documented: read `getColor()` + `isAdjusting()`; non-adjusting commit ⇒ recent-row update.
- RTL: rows/grids follow `ComponentOrientation` (ElwhaTabs/ButtonGroup already do); track gradients render in value order (min at leading edge), matching ElwhaSlider's RTL behavior.

## §11 States & motion

- Hover/focus/pressed `StateLayer` over swatch cells, strip segments, recent chips; handles/thumbs have no extra halo in V1 (the 2px SURFACE ring is the affordance).
- Tab-switch motion = ElwhaTabs' indicator slide (free). No new morphs/tweens in V1; spectrums repaint direct. Reduced-motion: nothing extra needed.
- Disabled: tracks/cells at `disabledContainerOpacity()`-style muting via 38% content alpha; no state layers; keyboard inert (§2).

## §12 Showcase (S8)

- `ColorPickerShowcasePanels` + `LeafEntry("Color picker", …, AREA_COMPONENTS, …)`.
- **Workbench**: stage = live inline `ElwhaColorPicker` + live hex/RGB readout label; controls = mode subset (`ElwhaCheckbox` ×3), alpha (`ElwhaCheckbox`), enabled (`ElwhaSwitch`), supporting-text (`ElwhaTextField`), preset initial colors (`ElwhaSelectField`), and an `ElwhaButton` "Open dialog…" launching `ElwhaColorPickerDialog` (confirm writes the workbench picker). Generated-code panel mirrors knobs. **All knobs are Elwha components** — dogfood.
- **Gallery**: single-mode pickers (each pane standalone), alpha-enabled spectrum, disabled state, recent-row populated example.
- Headless `ColorPickerShowcaseSmoke` (panels build, knobs apply, no NPE).

## §13 Phasing → stories (V1, single phase)

| S | Story | Locks |
|---|---|---|
| S1 | spike — shell, header, `PickerMode`/`setModes`, ElwhaTabs host, selection model, new icons | architecture §2–§4 |
| S2 | SWATCHES pane + `MaterialSwatchCatalog` + recent row | §5 |
| S3 | SPECTRUM pane + `ColorTrackSlider` primitive | §6 |
| S4 | SLIDERS pane + RGB/HSV toggle + hex field | §7 |
| S5 | `ElwhaColorPickerDialog` | §8 |
| S6 | alpha channel | §9 |
| S7 | keyboard + a11y sweep | §10 |
| S8 | Showcase + CHANGELOG (closes epic) | §12 |

Each story: fresh demo class + headless smoke guard, `@version v0.5.0`, one commit.

## §14 Out of scope (→ #482 unless noted)

Wheel mode · eyedropper (`Robot` + macOS screen-recording permission) · theme-palette swatch tiers (`Palette`/49 roles, `MaterialPalettes` tiers) · persistent favorites · docked/popover presentation (anchored, date-picker-docked analog) · HSL/CMYK sub-modes (not planned at all — survey shows no leading API leads with them).
