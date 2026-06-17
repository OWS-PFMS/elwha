# ElwhaColorPicker — research capture

**Status:** SYNTHESIS (epic #481 · V2 stub #482) · captured 2026-06-11
**Method:** M3 defines **no** Color Picker. This capture therefore has two halves: (1) the two pickers M3 *does* define — Date Picker and Time Picker — captured from the MDC-Android GitHub raw docs plus m3.material.io spec values, distilled into the common **M3 picker grammar**; (2) a cross-API survey of how mature toolkits shape color pickers. The design doc (`elwha-color-picker-design.md`) synthesizes the two into one component.
**Sources:**
- https://raw.githubusercontent.com/material-components/material-components-android/master/docs/components/DatePicker.md
- https://raw.githubusercontent.com/material-components/material-components-android/master/docs/components/TimePicker.md
- https://m3.material.io/components/date-pickers/specs · https://m3.material.io/components/time-pickers/specs (JS-only; values cross-checked against MDC attrs + Compose/Flutter implementations)
- JColorChooser (JDK 21 javax.swing) · WinUI 3 `ColorPicker` (learn.microsoft.com) · macOS `NSColorPanel` · Chrome `input[type=color]` popup · Flutter `flex_color_picker` (rydmike)

---

## §TL;DR synthesis

1. **M3 picker grammar** (shared by Date + Time Picker): `surface-container-high` container at corner-extra-large (28dp) and elevation 3 → header = *supporting text* (label-large, on-surface-variant) over a *large headline showing the pending selection* (on-surface) → divider (outline-variant) → **picking surface** → trailing **text-button action row** (Cancel / OK) → a **mode-switch affordance** for alternate input methods.
2. **Pending-until-OK**: both M3 pickers stage the selection in the headline and commit only on the confirm button. Dialog form must do the same; the inline form is live.
3. **Mode-switch shape depends on mode count**: M3 uses an *icon button* to flip between exactly two modes (calendar↔input, dial↔keyboard). With three modes, the M3-native generalization is **tabs** ("organize views of related content at the same hierarchy") — which is also the `JColorChooser` lineage.
4. **Selection-indicator colors**: selected = `primary` family (selected day = primary circle/on-primary; selected time field = primary-container/on-primary-container); "current value" = 1dp `primary` outline (today's date). The color-picker analog: selected swatch gets the primary ring treatment; the picker's *initial* color gets the today-style thin ring.
5. **Cross-API consensus**: every mature color picker converges on the same three panes — **swatch grid** (quick pick), **SV-square + hue slider** (freeform), **channel sliders / hex** (precise) — plus an eyedropper (OS-permission-gated; deferred to V2 #482).
6. **Fixed mode sets are the norm.** NSColorPanel (5 modes), WinUI, Chrome, Flutter's `pickersEnabled` flags — all closed sets. `JColorChooser.setChooserPanels` (client-pluggable panels) is the outlier and the operator explicitly excluded it. → closed `PickerMode` enum; `setModes(...)` can subset, never extend.
7. **Color math is free**: `java.awt.Color.RGBtoHSB/HSBtoRGB` covers HSV↔RGB (HSB ≡ HSV); hex is trivial. Zero new dependencies, zero new theme tokens.

Reading order: §D date picker → §T time picker → §P picker grammar → §X cross-API survey → §M color math → §E Elwha token mapping → §L terminology lock → §O open questions → §F capture log.

---

## §D — M3 Date Picker capture

### Variants
- **Docked**: outlined text field + dropdown calendar (anchors like a menu). Month/year menu buttons paginate.
- **Modal**: dialog; calendar grid; header headline shows pending date; pagination icon buttons.
- **Modal input**: same dialog, picking surface replaced by an outlined text field.

### Anatomy (modal) `[CODE unless noted]`
| Part | Spec |
|---|---|
| Container | `colorSurfaceContainerHigh`, `shapeAppearanceCornerExtraLarge` (28dp), elevation level 3, width 328dp |
| Header supporting text | "Select date" — label-large, `colorOnSurfaceVariant` |
| Header headline | pending selection ("Mon, Aug 17") — headline-large, `colorOnSurface`; header block 120dp tall |
| Mode-switch | icon button (edit/calendar pencil), `colorOnSurfaceVariant`, trailing edge of header |
| Divider | under header, outline-variant |
| Day cell | 40×40dp circle; **selected** = `colorPrimary` fill + `colorOnPrimary` text, stroke 0dp; **today** = 1dp `colorPrimary` stroke + `colorPrimary` text; plain day = on-surface text |
| Range fill | MDC attr says `colorSurfaceVariant`; m3.material.io tokens say secondary-container — ⚠ unresolved discrepancy, **irrelevant to color picker** `[DOC]` |
| Action row | text buttons (Cancel, OK), trailing-aligned, bottom of container |
| A11y | dialog title announced on launch; "use a descriptive title for the task" `[CODE — accessible name]` |

### Behaviors
- Tap day → headline updates (pending); OK commits; Cancel/scrim discards. `[CODE]`
- Header icon button toggles calendar ↔ text-input modes; state is preserved across the toggle. `[CODE]`

## §T — M3 Time Picker capture

### Variants
- **Dial**: drag a handle around a clock face. **Input**: keyboard entry fields. Toggle via icon button (keyboard/clock glyph) at the container's bottom-leading corner.

### Anatomy `[CODE]`
| Part | Spec |
|---|---|
| Container | `colorSurfaceContainerHigh`, corner-extra-large, elevation 3 |
| Headline | "Select time" — label-medium, on-surface-variant |
| Time selector fields | 96×80dp each, display-large text, corner-small (8dp); **selected** = primary-container fill / on-primary-container text; **unselected** = `colorSurfaceContainerHighest` fill / on-surface text |
| Separator | ":" display-large, on-surface, between fields |
| Period selector | stacked AM/PM, 52dp wide, 1dp outline border, corner-small; **selected** = tertiary-container / on-tertiary-container; unselected = transparent / on-surface-variant |
| Clock dial | 256dp circle, `colorSurfaceContainerHighest` (MDC `clockFaceBackgroundColor` confirms); 36dp side padding |
| Clock hand | `colorPrimary` center dot + 2dp track + 48dp selector circle; selected label `colorOnPrimary`; labels on-surface |
| Mode-switch | icon button bottom-leading, on-surface-variant |
| Action row | text buttons trailing-aligned |

### Behaviors
- The *active* selector field (hour vs minute) is the primary-container one; dial edits route to it. Field activation is itself a selection state. `[CODE — same "active sub-target" idea as the sliders-pane active channel]`

## §P — The synthesized M3 picker grammar

What Date + Time share — this is the contract the color picker must satisfy:

1. **Container**: surface-container-high · corner XL (28dp) · elevation 3 · ~328dp wide.
2. **Header**: supporting text naming the task (label-large/medium, on-surface-variant) + **large headline rendering the pending value** (on-surface) + divider. *The headline is the preview.* For a color that means: preview swatch + hex readout.
3. **Picking surface**: the interactive area (grid / dial / spectrum), cells sized ≥40dp with 48dp touch targets.
4. **Mode switch**: 2 modes → icon-button toggle; 3+ modes → tabs (M3's own content-switcher; JColorChooser precedent).
5. **Action row** (modal only): trailing text buttons; **pending-until-OK** commit; Esc/Cancel restores the entry value.
6. **Selection colors**: selected indicator = primary family; current/initial reference value = thin primary outline ("today" treatment).
7. **A11y**: container announces a descriptive task title; every cell/handle keyboard-reachable.

## §X — Cross-API color picker survey

| API | Modes (fixed?) | Freeform | Precise | Quick | Alpha | Eyedropper | Commit model |
|---|---|---|---|---|---|---|---|
| `JColorChooser` (Swing) | 5 tabs; **pluggable** via `setChooserPanels` (outlier — excluded by operator) | HSV/HSL tabs (square+slider) | RGB/CMYK slider+spinner tabs | Swatches tab + recent grid | yes (slider tabs) | no | `showDialog` blocks → returns Color or null; live `ColorSelectionModel`+`ChangeListener` otherwise |
| WinUI 3 `ColorPicker` | one surface, knobs gated by bool props (`IsMoreButtonVisible`, …) — **fixed** | spectrum square/ring + value slider | channel sliders + text boxes + hex | n/a | `IsAlphaEnabled` gates alpha slider + textbox | no | live `ColorChanged`; "more" button expands precision inputs |
| macOS `NSColorPanel` | 5 **fixed** modes in a toolbar: Wheel · Sliders · Palettes · Image · Pencils | wheel + brightness | sliders (Gray/RGB/CMYK/HSB) + hex field | palettes/pencils + favorites bar | yes (opacity slider) | yes (pipette) | live continuous `setAction` callbacks |
| Chrome `input[type=color]` | one popup — **fixed** | SV square + hue slider | format toggle hex/RGB/HSL + fields | swatch row | alpha slider (with `alpha` attr) | yes (EyeDropper API) | live `input` events + `change` on dismiss |
| Flutter `flex_color_picker` | `pickersEnabled` flag map — **fixed set, subsettable** | wheel + shade box | hex/copy-paste | Material primary/accent swatches → shade row · recent colors | optional | no | live `onColorChanged` + `onColorChangeStart/End` (adjusting brackets) |
| Figma/Photoshop (pattern ref) | format dropdown | SV square + hue + alpha | hex + per-channel | saved styles | yes | yes | live |

**Consensus extracted:**
- The three universal panes: **swatch grid**, **SV square + hue slider**, **channel sliders + hex**. A wheel is a *fourth* freeform variant (macOS/Flutter), redundant with SV-square for V1 → V2 #482.
- Swatch organization: Flutter's two-stage **hue grid → shade row for the active hue** scales the 2014 Material palette (19 hues × 10 shades) into a 328dp surface without 190 tiny cells. NSColorPanel's favorites bar ≈ JColorChooser's **recent row** — keep recent, defer persistent favorites (#482).
- **Adjusting semantics**: Flutter's `onColorChangeStart/End` and Swing's `getValueIsAdjusting` agree: continuous events during a drag, with a bracket so listeners can defer expensive work. ElwhaSlider already models this.
- Alpha is everywhere but **opt-in** (WinUI `IsAlphaEnabled` is the cleanest precedent) — checkerboard backing under gradient tracks + preview, 8-digit hex.
- Eyedropper is universal *and* universally OS-entangled (macOS screen-recording permission for `java.awt.Robot.getPixelColor`) → V2 #482, `colorize` glyph already bundled.

## §M — Color math

- `Color.RGBtoHSB(r,g,b,float[])` / `Color.HSBtoRGB(h,s,b)` — JDK-native HSV↔RGB (Java's HSB ≡ HSV). No HSL needed for V1 (HSL adds a second lightness model for marginal value; none of WinUI/macOS/Flutter lead with it).
- Hex: `#RRGGBB` canonical; `#AARRGGBB`? — ⚠ web order is `#RRGGBBAA`, AWT int order is `0xAARRGGBB`. **Lock: display/parse `#RRGGBBAA` (web order)** — every survey API with hex-alpha (Chrome, Figma, Flutter) uses it; AWT byte order is an implementation detail. `[CODE]`
- SV square render: horizontal = saturation 0→1 (white→hue), vertical = value 1→0 (→black); composite via two `GradientPaint`s or per-row `HSBtoRGB` into a cached `BufferedImage` keyed by hue.

## §E — Elwha token mapping (zero new tokens)

| Need | Elwha token |
|---|---|
| Container fill / shape / elevation | `ColorRole.SURFACE_CONTAINER_HIGH` · `ShapeScale.XL` · dialog host's existing elevation (ShadowPainter level 3) |
| Supporting text | `TypeRole.LABEL_LARGE` + `ColorRole.ON_SURFACE_VARIANT` |
| Headline (hex readout) | `TypeRole.HEADLINE_LARGE` + `ColorRole.ON_SURFACE` (alpha-on: `HEADLINE_MEDIUM` for 9 glyphs — design §4) |
| Preview swatch border | `ColorRole.OUTLINE_VARIANT` 1px (keeps near-surface colors visible) |
| Divider | `ColorRole.OUTLINE_VARIANT` 1px |
| Mode switch | `ElwhaTabs` (primary variant, inline icon+label) — its own token mapping |
| Swatch selected ring | `ColorRole.PRIMARY` 2px ring + luminance-picked check glyph (`MaterialIcons.check`) |
| Initial-color ring ("today" analog) | `ColorRole.PRIMARY` 1px ring |
| Swatch hover/focus/pressed | `StateLayer.HOVER/FOCUS/PRESSED` over the cell |
| Gradient track geometry | M3 slider grammar: 16px track height, `ShapeScale.FULL`; handle = 6px bar, `ShapeScale.FULL`, `SURFACE` fill + `OUTLINE` 1px border (theme-neutral over arbitrary gradients) |
| Spectrum thumb | 16px ring: `SURFACE` ring 2px + `OUTLINE` 1px inner contrast |
| Channel labels / values | `TypeRole.LABEL_LARGE` / `TypeRole.BODY_MEDIUM`, `ON_SURFACE_VARIANT` / `ON_SURFACE` |
| Hex entry / errors | `ElwhaTextField` (outlined) + its error supporting-text system |
| RGB ↔ HSV toggle | `ElwhaButtonGroup` (connected, single-mandatory) |
| Dialog host / action row | `ElwhaDialog` (text-button actions trailing — already M3-correct) |
| Checkerboard (alpha) | literal `0xFFFFFF`/`0xCCCCCC` 8px squares — physical transparency depiction, deliberately not tokenized (same in light/dark; matches every survey API) `[DOC]` |

## §L — Terminology lock (API mirrors the source nouns)

| API name | Source |
|---|---|
| `PickerMode.SWATCHES / SPECTRUM / SLIDERS` | JColorChooser "Swatches" · Chrome/WinUI "spectrum" · universal "sliders" |
| `setModes(PickerMode...)` | Flutter `pickersEnabled` subset semantics |
| `getColor()/setColor(Color)` | JColorChooser/AWT convention |
| `isAdjusting` on change events | `BoundedRangeModel.getValueIsAdjusting` / ElwhaSlider precedent |
| "supporting text", "headline", "action row" | M3 picker anatomy nouns |
| `setAlphaEnabled` | WinUI `IsAlphaEnabled` |
| `ElwhaColorPickerDialog.show…` | M3 "modal" picker + `JColorChooser.showDialog` lineage |

## §O — Open questions → resolved in design doc

1. Tabs vs connected button group for the 3-mode switch → **tabs** (design §3).
2. Extend `ElwhaSlider` with gradient tracks vs dedicated primitive → **dedicated `ColorTrackSlider`** (design §2, surfaced loudly).
3. Hex field placement (all panes vs SLIDERS pane) → **SLIDERS pane**; header carries a read-only hex readout (design §4).
4. Headline type role with 9-glyph alpha hex → **HEADLINE_MEDIUM when alpha enabled** (design §4).
5. Wheel mode / eyedropper / theme-palette swatches → **V2 #482**, not silent cuts.

## §F — Capture log

- 2026-06-11 — MDC-Android `DatePicker.md` + `TimePicker.md` fetched (raw GitHub). m3.material.io spec pages are JS-only (established); dimension values (328dp container, 120dp header, 40dp day cells, 256dp dial, 96×80dp fields, 36dp dial padding) cross-confirmed via web search of the spec + Compose/Flutter issue trackers. Range-fill color discrepancy noted §D.
- 2026-06-11 — Cross-API survey via vendor docs/search: WinUI ColorPicker (learn.microsoft.com), NSColorPanel modes + eyedropper, Chrome popup anatomy, flex_color_picker feature set (pub.dev/rydmike), JColorChooser from JDK knowledge (Swatches/HSV/HSL/RGB/CMYK tabs, `setChooserPanels`, `showDialog`).
- Operator constraints recorded: several fixed ways to pick/define colors; **no client-made custom picker panels**; dogfood Elwha components; UI/UX defined by us against Elwha best practices.
