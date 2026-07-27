# ElwhaColorPicker V2 — wheel · eyedropper · swatch tiers · popover (research)

> **Status: CONSOLIDATION → synthesis.** Backing research for epic
> [#482](https://github.com/OWS-PFMS/elwha/issues/482) (ElwhaColorPicker V2), the follow-on to the
> shipped **V1** epic [#481](https://github.com/OWS-PFMS/elwha/issues/481) (PR #492). Phase 0
> design lives in `elwha-color-picker-v2-design.md`.

**The V2 feature set was already surveyed during the #481 V1 pass** — the cross-API survey in
`elwha-color-picker-research.md` §X recorded the wheel, eyedropper, favorites-bar, and popover
precedents and §D captured the M3 *docked* date-picker variant. This doc **consolidates** that
capture into a V2-focused synthesis, adds the per-feature mechanics V1 didn't need (wheel
geometry/paint, `java.awt.Robot` screen-sampling constraints, theme-role sourcing, favorites
persistence contracts, popover commit models), maps every need onto existing Elwha primitives +
tokens (goal: **zero new theme tokens**), and lists the open questions for Phase 0. It does **not**
re-transcribe V1's M3 picker-grammar capture — it references it.

## Sources

- **Already-captured spec + survey** (V1 pass, 2026-06-11) → `elwha-color-picker-research.md`:
  §D docked variant · §P picker grammar · §X cross-API survey (wheel/eyedropper/favorites rows) ·
  §M color math · §E token mapping.
- **Reuse targets in-tree:** `colorpicker/` V1 as shipped (`ElwhaColorPicker` single-commit path +
  recent MRU, `SwatchesPane.CellStrip` chassis, `ColorTrackSlider`, `SpectrumPane`'s cached-image
  SV box, `ColorPickerHeader`, `ElwhaColorPickerDialog` composition pattern),
  `overlay/AbstractElwhaOverlay` (public shared host: light-dismiss policy, `POPUP_LAYER` band,
  chain registry), `theme/ColorRole` (49 roles, live `resolve()`), `buttongroup/ElwhaButtonGroup`
  (the SLIDERS RGB/HSV sub-toggle precedent), `menu/ElwhaMenu` (context-menu affordance),
  `icons/MaterialIcons` (`colorize`, `star`/`starFilled` already bundled + factoried).
- **Platform references:** macOS `NSColorPanel` (Wheel mode, pipette, favorites bar) ·
  `NSColorSampler` (10.15+) · Chrome color popup + web `EyeDropper` API · Flutter
  `flex_color_picker` (wheel + `pickersEnabled`) · `java.awt.Robot` capture semantics
  (macOS Screen Recording permission, JDK 21).

---

## §A. What V2 adds (scope)

V1 shipped the closed three-mode picker (SWATCHES / SPECTRUM / SLIDERS), the modal + full-screen
dialog, alpha, keyboard/a11y, and the Showcase leaf. V2 adds **five capabilities**, all additive:

1. **WHEEL mode** — a fourth `PickerMode`: hue/saturation disc + value track (macOS Color Wheel /
   `flex_color_picker` precedent). The closed-enum contract holds: it ships in the lib, never as a
   client panel.
2. **Eyedropper** — screen color sampling (opt-in header affordance, `colorize` glyph), built on a
   frozen `java.awt.Robot` screen capture with a magnifier loupe.
3. **Theme-palette swatch tier** — a SWATCHES source showing the **live Elwha theme's 49
   `ColorRole`s**, so apps pick theme-consistent colors that match the installed palette + mode.
4. **Saved swatch tier (favorites)** — persistent user swatches (NSColorPanel favorites-bar
   precedent) with a client-owned persistence contract.
5. **Docked popover presentation** — `ElwhaColorPickerPopover`, the anchored light-dismiss analog
   of the M3 *docked* date picker (V1 research §D: "outlined text field + dropdown calendar —
   anchors like a menu").

**Everything in V1 is out of scope** (modes/grammar/dialog/alpha/hex/recent — all reused as-is);
HSL/CMYK slider sub-modes stay **not planned at all** (V1 design §14).

---

## §B. Per-feature mechanics

### §B.1 Wheel — anatomy precedents

| Implementation | Geometry | Value channel | Notes |
|---|---|---|---|
| macOS NSColorPanel "Color Wheel" | disc: hue = angle, saturation = radius | separate brightness slider | the mainstream desktop wheel |
| Flutter `flex_color_picker` wheel | hue ring + square shade box inside | inside the box | hybrid |
| GIMP / Krita | hue ring + SV triangle | inside the triangle | pro-niche; rotating-triangle hit-testing |
| Photoshop (optional UI) | hue ring + SV square | inside the square | |

**Consensus for a 328px M3-grammar pane:** the **disc + value slider** (macOS/Flutter lineage) —
it mirrors V1 SPECTRUM's structure exactly (one 2D surface + one `ColorTrackSlider`), so the pane
reuses the V1 chassis, height budget, and keyboard grammar. Ring+triangle is the pro-niche outlier
with rotating hit-testing for no added picking power.

**Disc math:** pointer→(dx,dy) from center; `saturation = min(1, dist/R)`, `hue = atan2(dy,dx)`
normalized to 0–360°. **Paint trick:** render the disc once per size at `value = 1` into a cached
`BufferedImage`, then composite **translucent black at alpha (1 − value)** over it — exact for HSV
because RGB(h,s,v) = v · RGB(h,s,1), so a src-over black layer scales every channel by v. No
re-render per value step. AA rule from V1 applies: the disc edge is drawn into the image / fills,
**never produced by `clip()`** (clip is never antialiased — V1 smoke finding).

**Hue preservation** (V1 design §6 invariant) extends verbatim: the wheel pane owns float `h,s`
while it is the commit source; external syncs keep `h` at `s = 0` (disc center) and keep `h,s` at
`v = 0`.

**Tab icon:** Material Symbols **`colors`** — verified available on gstatic
(Rounded/400/fill0/20px, default + fill1) 2026-06-11. House pipeline: bundle SVG + a
`MaterialIcons.colors()` factory, same as V1's `gradient`/`tune`.

### §B.2 Eyedropper — sampling mechanics survey

| Platform | Mechanism | Permission story |
|---|---|---|
| Web `EyeDropper` API (Chrome 95+) | `new EyeDropper().open()` → Promise of `{sRGBHex}`; browser draws the loupe; Esc cancels | user-gesture gated |
| macOS `NSColorSampler` / NSColorPanel pipette | system magnifier loupe | no extra permission (system API) |
| Windows PowerToys Color Picker | global hook + magnifier | none |
| **Java (our lane)** | `java.awt.Robot` — `createScreenCapture` / `getPixelColor` | **macOS 10.15+: Screen Recording permission**; unpermitted captures silently return wallpaper-only frames (no exception). X11/Windows: unrestricted. Wayland: portal-mediated, may fail. |

**The Java-realizable pattern is the frozen-capture overlay** (the IntelliJ-lineage approach): on
activation, `Robot.createScreenCapture` of **each `GraphicsDevice`'s bounds**, shown in a
full-screen undecorated always-on-top window per device; the pointer drives a **magnifier loupe**
(scaled pixel grid around the cursor + center-pixel hex label); click or Enter samples the frozen
image and commits; Esc cancels; arrow keys nudge by one pixel. Sampling reads **the captured
image**, not live `getPixelColor` polling — one capture, no global-hook requirement, works for
every pixel on screen including outside the host window.

Key constraints to carry into design:
- **No reliable permission probe on macOS** — denial yields a valid-but-wallpaper image, not an
  error. So the permission note is a `[DOC]` rule (Javadoc + README), not a `[CODE]` detection.
  What *is* `[CODE]`-detectable: `GraphicsEnvironment.isHeadless()` and `AWTException` from Robot
  construction → the affordance disables/hides.
- HiDPI: device-bounds captures are logical-resolution; the loupe magnifies logical pixels —
  acceptable and what `getPixelColor` would report anyway. `[DOC]` note, no retina pass in V2.
- The eyedropper is an **affordance, not a mode**: every surveyed picker (Chrome, macOS, Figma)
  places it as a small button beside the preview/header, never as a tab.

### §B.3 Theme-palette tier — sourcing options

| Source | What it yields | Verdict |
|---|---|---|
| **Live theme roles** — `ColorRole.resolve()` × 49 | the colors the running app is actually themed with; tracks palette + light/dark mode automatically (resolve at paint time) | **the tier** — this is what "theme-consistent pick" means |
| `MaterialPalettes.primary()/secondary()` bundled `Theme`s | 17 whole demo palettes (each a full scheme) | a *palette chooser*, not a color picker — wrong granularity for picking one color; **dropped, documented** |

Mechanics: cells render `ColorRole.resolve()` **at paint time** (live — a theme switch repaints
correctly with no listener plumbing); commits hand the picker a **plain `Color` copy**, never the
`ColorUIResource` that `resolve()` returns — the #495 lesson (UIResource values set onto Swing
children get clobbered by `installDefaults`) applied at the source. Accessible names prettify the
enum constants: "Primary container · #EADDFF". 49 roles in the V1 hue-grid geometry = a 10-column,
5-row grid (last row partial).

### §B.4 Saved swatches (favorites) — persistence survey

| Precedent | Model | Persistence |
|---|---|---|
| NSColorPanel favorites bar | drag-in/drag-out swatch strip | OS-owned (`~/Library/Colors`) |
| JColorChooser recent grid | MRU only, no pinning | none |
| Figma styles | named saved styles | document/cloud-owned |

**Elwha's lane:** the *component* owns the in-memory model + change events; the *client* owns
persistence — the lib's zero-coupling stance (Swing + FlatLaf only) rules out file/Preferences I/O
in the component. That is exactly the WinUI/Flutter pattern: expose
`getFavorites()/setFavorites(...)` + add/remove + a listener, and the consumer round-trips the list
through whatever store it already has. Drag-in/drag-out (NSColorPanel) is heavy Swing DnD for V2's
value — replaced by an explicit save-current affordance + Delete-key / context-menu removal
(`ElwhaMenu` — dogfood).

### §B.5 Popover — commit-model survey

| Precedent | Commit model | Action row |
|---|---|---|
| M3 **docked** date picker (V1 research §D) | "anchors like a menu"; field + dropdown surface | the docked variant is the lightweight sibling of the modal |
| Chrome `input[type=color]` popup | **live** `input` events while open; `change` on dismiss | none — dismissal is "done" |
| macOS NSColorPanel | live continuous callbacks | none |
| ElwhaMenu / Elwha overlay family | light-dismiss on outside-press / focus-escape / Esc | n/a |

**Consensus:** an anchored picker popover commits **live** (it wraps the inline `ElwhaColorPicker`,
whose contract is already live) and **light-dismisses** — no Cancel/OK row. Pending-until-OK
semantics remain the dialog's job (V1 §8); offering both presentations mirrors M3 offering modal
*and* docked date pickers. Host: `overlay/AbstractElwhaOverlay` with `lightDismiss() = true` at
`POPUP_LAYER` (300) — the menu's band; a picker popover is a transient popup, not a side-sheet
(190). The package-boundary pattern is settled precedent: a **package-private host subclass**
inside `colorpicker/` (the `AbstractElwhaMenuOverlay`-in-`menu/` arrangement) wrapped by a public
class — `AbstractElwhaOverlay` stays out of the public API surface per its own contract note.
Placement is a **pure function** (unit-testable, the V1 menu's `placeAnchored` doctrine): below the
anchor, top-aligned to the anchor's leading edge, flipping above on viewport clip, shifting
horizontally to stay on-screen, RTL-mirrored.

---

## §C. The already-captured grammar (pointers — do not re-litigate)

- **§P picker grammar** — container/header/picking-surface/mode-switch contract: WHEEL is just a
  fourth picking surface behind the existing `ElwhaTabs` switch; the popover reuses the container
  tokens the dialog already applies.
- **§X survey** — "a wheel is a *fourth* freeform variant (macOS/Flutter), redundant with
  SV-square for V1 → V2 #482" · "NSColorPanel's favorites bar ≈ recent row — defer persistent
  favorites (#482)" · "Eyedropper is universal *and* universally OS-entangled → V2 #482,
  `colorize` glyph already bundled".
- **§D docked variant** — the anchored presentation precedent.
- **§M color math** — `Color.RGBtoHSB`/`HSBtoRGB` cover the wheel; hex stays `#RRGGBBAA` web order.
- **V1 smoke-iterate findings that bind V2 geometry:** the pane **height budget** — cards equalize
  under `CardLayout`, and the operator iterated the dead band out of V1 (SV box trimmed to 146px so
  the tallest card ≈ 230px with alpha). **New V2 views must not exceed that budget**, or every
  other pane regrows the dead band. Also: clip() is never antialiased; three inline-icon tabs
  truncate at 328px (stacked icon-over-label is the V1 fix — a fourth tab must be re-verified).

---

## §D. Token mapping — zero new theme tokens

| V2 need | Existing Elwha vehicle | New token? |
|---|---|---|
| Wheel disc thumb / border / focus | V1 SV-box treatment verbatim (16px SURFACE+OUTLINE ring thumb, OUTLINE_VARIANT border, `StateLayer.FOCUS`) | **no** |
| Wheel value/alpha tracks | `ColorTrackSlider` (V1 primitive) | **no** |
| Wheel tab icon | new bundled SVG **`colors`** + `MaterialIcons.colors()` | **no** (icon asset, not a token) |
| Eyedropper affordance | `ElwhaIconButton` (standard) + bundled `colorize` glyph (factory exists) | **no** |
| Sampler loupe chrome | OUTLINE / SURFACE strokes + LABEL_MEDIUM hex label; frozen screenshot is content, not surface | **no** |
| Swatch-source sub-toggle | `ElwhaButtonGroup` connected single-mandatory (the V1 RGB/HSV precedent) | **no** |
| Theme tier cells | `ColorRole.resolve()` (all 49) painted via the V1 `CellStrip` chassis | **no** |
| Saved tier save/remove | `ElwhaButton` text + `star` glyph (factory exists) · `ElwhaMenu` context menu · Delete key | **no** |
| Popover container | the dialog's surface treatment (SURFACE_CONTAINER_HIGH, XL corners, level-3 shadow via `ShadowPainter`) on an `AbstractElwhaOverlay` light-dismiss host | **no** |
| Popover band | `JLayeredPane.POPUP_LAYER` (300) — menu precedent | **no** |

→ **Goal holds: zero new theme tokens.** The only asset add is the `colors` icon SVG.

---

## §E. Open questions for Phase 0 (carried to the design doc)

1. **[LOAD-BEARING] Where do the new swatch tiers live?** A `SwatchSource { MATERIAL, THEME,
   SAVED }` sub-toggle inside the SWATCHES pane (the RGB/HSV `ElwhaButtonGroup` precedent) vs. two
   new top-level `PickerMode`s. (Recommend: **sub-toggle** — they are all "pick from fixed
   swatches"; six top-level tabs cannot fit 328px; mirrors `setModes` with a closed
   `setSwatchSources` subset contract.)
2. **Height budget reconciliation** — the toggle row + 5-row theme grid + favorites grid must each
   stay ≤ the ~230px tallest-card budget. (Recommend: recent row stays in the MATERIAL view only;
   SAVED carries its own action row instead; THEME is toggle + grid. All three land ≈ 218–222px.)
3. **Default mode set** — does WHEEL join the default four, or stay opt-in? (Recommend: default
   **all four** — the closed set *is* the offering, JColorChooser/NSColorPanel show all modes;
   verify four stacked tabs render un-truncated at 328px in the smoke loop.)
4. **Eyedropper default** — on or off? (Recommend: **opt-in**, `setEyedropperEnabled(false)`
   default — the macOS permission prompt should be a consumer decision, the WinUI
   `IsAlphaEnabled`-style gate V1 already established for alpha.)
5. **Popover initial-color/revert semantics** — live commits mean Esc does not revert. Acceptable?
   (Recommend: yes — Chrome behavior; revert belongs to the dialog. Document loudly.)
6. **Favorites capacity** — unbounded vs. capped. (Recommend: cap at 30 = three grid rows, the
   height budget; `setFavorites` truncates + dedupes, documented.)

---

## §F. Screenshot log

- **No new M3 captures needed** — M3 defines no color picker (V1 finding); the docked-variant
  grammar was captured 2026-06-11 in the V1 pass (`elwha-color-picker-research.md` §D/§F). Platform
  mechanics above are from vendor docs/JDK semantics, logged 2026-06-11.
- *(append if any capture is pasted)*
