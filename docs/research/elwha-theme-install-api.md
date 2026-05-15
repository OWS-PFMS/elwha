# FlatComp Theme Install API — Locked Decisions

**Status:** LOCKED. Authoritative theme-install API for FlatComp v1. Decisions are not to be re-debated during execution — any change requires reopening this document with rationale.

**Drafted:** 2026-05-14 · **Locked:** 2026-05-14

**Author:** Charles Bryan (`cfb3@uw.edu`), drafted via design conversation with Claude.

**Parents:**
- [`elwha-design-direction.md`](elwha-design-direction.md) — the design-system stance.
- [`elwha-token-taxonomy.md`](elwha-token-taxonomy.md) — the **locked** 79-token surface this API installs.

This document specifies how a palette becomes a live theme: the `Palette` / `Theme` data types, the `ElwhaTheme` install entry point, what install does to `UIManager`, the state-layer compute-and-bake step, runtime switching, and where palettes come from. It is designed against the fixed token surface — it does not revisit which tokens exist.

---

## 1. The data types

Five types, only one of which is mode-aware.

| Type | What it is | Mutability |
|---|---|---|
| `ColorRole` / `ShapeScale` / `SpaceScale` / `TypeRole` / `StateLayer` | The **vocabulary** — locked enums from the taxonomy. Mode-agnostic. | enum |
| `Palette` | One **complete** role→value map for **one mode**. All 49 color roles present. | immutable value object |
| `Theme` | A named `{ light: Palette, dark: Palette }` pair. The thing a user "picks." | immutable value object |
| `Typography` | Font family + the 12 `TypeRole`→`Font` resolutions. Mode-agnostic. | immutable value object |
| `Config` | `{ theme, mode, typography }` — the complete input to `install()`. | immutable value object |

### 1.1 `Palette`

- Holds the 49 `ColorRole` values — **color only**. Shape and spacing are not palette data (§1.4).
- **Completeness is validated at construction.** A `Palette` missing any role throws — partial palettes produce `null` resolves and silent paint bugs. Palettes come from the M3 builder export (always complete), so fail-fast costs nothing and catches transcription errors early.
- The 12 mode-invariant **fixed** roles are stored in *both* the light and dark palettes with identical values (the "complete-map redundancy" option from the light/dark discussion) — keeps `Palette` self-contained; 12 redundant entries cost nothing.

### 1.2 `Theme`

- `{ name, lightPalette, darkPalette }`.
- Does **not** carry a FlatLaf base-LAF class. The mode→LAF mapping (`LIGHT`→`FlatLightLaf`, `DARK`→`FlatDarkLaf`) is owned by the installer, because the base-LAF choice is a property of the *mode*, not the *theme* — every theme uses `FlatLightLaf` for its light palette. Custom base LAFs (e.g. a FlatLaf IntelliJ theme as substrate) are a possible v2 extension; out of scope for v1.

### 1.3 `Mode`

`LIGHT` / `DARK` / `SYSTEM`. `SYSTEM` resolves to a concrete `LIGHT` / `DARK` at install time via FlatLaf's OS-appearance detection. v1 detects at install time only — see §6.

### 1.4 Shape and spacing — resolved, not palette data

`ShapeScale` and `SpaceScale` are **not** carried by `Palette` or `Theme`. They are fixed enums whose values do not vary by theme or mode in v1.

They are still **resolved from `UIManager` at paint time**, exactly like color and type — `SpaceScale.MD.px()` does a `UIManager.getInt("Elwha.space.md")` lookup, *not* a return of a compiled-in constant. `install()` writes the fixed v1 defaults into those keys.

**Why resolve, when the v1 values are fixed?** Two reasons. (1) **Consistency** — one uniform resolution model across all five token categories; a value-holder approach would make shape/spacing the odd one out. (2) **It keeps themeable geometry cheap later.** If a future "compact" theme needs to vary spacing, the lift is fully contained in the theme module: add an optional geometry block to `Theme` / the palette JSON, have `install()` read from it (falling back to the v1 defaults when absent). **No component code changes** — components already resolve from `UIManager` — and **no taxonomy reopen**, because the *set* of steps (`xs…xxl`) is unchanged; only whether their values vary. The value-holder alternative would have made that future change invasive: it alters the enum's contract and forces re-validation of every call site.

---

## 2. The install entry point

A single operation — "install this `(theme, mode, typography)` combination" — used for both startup and runtime switching.

```java
// Startup
ElwhaTheme.install(
    ElwhaTheme.config()
        .theme(MaterialPalettes.baseline())   // a Theme
        .mode(Mode.SYSTEM)                    // follow the OS
        .typography(Typography.defaults())    // bundled Inter
        .build());

// Runtime switch — same call, cheap derivation
ElwhaTheme.install(currentConfig.withMode(Mode.DARK));
ElwhaTheme.install(currentConfig.withTheme(themes.get("forest")));
```

- `ElwhaTheme.config()` → a `Config.Builder`. `theme(...)` is required; `mode(...)` defaults to `SYSTEM`; `typography(...)` defaults to the bundled Inter resolution.
- `install(Config)` is **idempotent and re-callable** — it is the only entry point. There is no separate "switch" API; switching *is* re-installing.
- `Config` is immutable with `withMode()` / `withTheme()` / `withTypography()` for cheap derivation on switch.
- `ElwhaTheme` holds the last-installed `Config` as `current()` so callers can derive from it without threading it through their own state.

---

## 3. What `install(Config)` does

An ordered sequence:

1. **Resolve mode.** If `SYSTEM`, detect OS appearance → concrete `LIGHT` / `DARK`.
2. **Install the base LAF.** `LIGHT` → `FlatLightLaf.setup()`, `DARK` → `FlatDarkLaf.setup()`. This gives every raw Swing component its modern baseline (scrollbars, focus rings, defaults for components FlatComp does not theme).
3. **Select the palette.** `theme.light()` or `theme.dark()` for the resolved mode.
4. **Write the `Elwha.*` keys.** For every token, `UIManager.put("Elwha.<category>.<key>", value)` — all 49 colors, 7 shapes, 6 spacings, 12 type fonts, 5 state opacities, 2 disabled constants. This is what FlatComp's own components resolve against.
5. **Write the FlatLaf-native keys.** Map roles onto FlatLaf's component keys so **raw Swing inherits the design language** (§4). This is the step that makes a plain `JButton` look like it belongs next to a `FlatChip`.
6. **Compute-and-bake state layers.** FlatLaf models hover/pressed as separate colors; M3 models them as opacity overlays. Blend the overlay over the role color and write the *result* into FlatLaf's `*hoverBackground` / `*pressedBackground` keys (§5).
7. **Apply typography.** Register the bundled Inter font; write `defaultFont` and the `Elwha.type.*` fonts.
8. **Repaint.** `SwingUtilities.updateComponentTreeUI(w)` for every `Window` in `Window.getWindows()`. Components re-resolve tokens per the taxonomy's binding rule and re-skin live.

Steps 4–7 are pure `UIManager` population; step 8 is the only one that touches live components.

---

## 4. The FlatLaf-native key mapping

The bridge that makes raw Swing inherit the theme. FlatLaf exposes hundreds of `UIManager` keys; FlatComp maps a curated subset onto roles. Representative sketch — **not exhaustive**:

| FlatLaf key | Role | Notes |
|---|---|---|
| `Button.background` | `surface` | default (non-emphasis) button |
| `Button.foreground` | `onSurface` | |
| `Button.hoverBackground` | `HOVER` over `surfaceContainerLow` | baked, see §5 |
| `Component.focusColor` | `primary` | focus ring |
| `Component.borderColor` | `outline` | |
| `Component.arc` | `ShapeScale.SM.px()` | global default corner radius |
| `TextField.background` | `surface` | |
| `TextField.foreground` | `onSurface` | |
| `TextField.placeholderForeground` | `onSurfaceVariant` | |
| `ScrollBar.thumb` | `outlineVariant` | |
| `ToolTip.background` | `inverseSurface` | M3 tooltips use the inverse surface |
| `ToolTip.foreground` | `inverseOnSurface` | |
| `Component.error.focusedBorderColor` | `error` | |
| `defaultFont` | `Typography` → `bodyMedium` | |

**Locked (Q1):** the *full* mapping table is a separate sub-deliverable — likely 40–80 keys — shipped as its own locked appendix to this doc. It is enumerated carefully (FlatLaf's key list is large and partially undocumented) and validated visually in the playground apps. It does **not** block the install-API shape: the API only ever says "write the native keys" — it does not depend on the table's contents.

---

## 5. State-layer compute-and-bake

From design-direction §8: FlatLaf wants discrete colors for interaction states; M3 defines them as opacity overlays. Resolution — bake at install time.

For each FlatLaf state key that needs it:

```
bakedColor = alphaBlend(
    base   = <role color, e.g. surface>,
    overlay = <tint role, e.g. onSurface>,
    alpha  = StateLayer.HOVER.opacity()   // 0.08
)
// → write bakedColor into UIManager "Button.hoverBackground" etc.
```

- Runs **once per install**, not per paint — negligible cost.
- The blend is straightforward `Color` interpolation in sRGB for v1. (M3 technically composites in a perceptual space; sRGB is close enough at these low opacities and avoids pulling in a color-science dependency. Revisit if it looks off.)
- FlatComp's **own** components (`FlatChip`, `FlatCard`) do *not* use baked keys — they own their paint and apply the overlay live at paint time, reading `StateLayer.*.opacity()` directly. Baking is only for the raw-Swing bridge.

---

## 6. Runtime switching

- **Mechanism:** call `install(newConfig)` again. Steps 2–8 re-run; the binding rule (taxonomy doc) guarantees components re-resolve. No component-facing switch API.
- **Theme switch** (`withTheme`) and **mode switch** (`withMode`) are the same operation — different `Config` field changed.
- **`SYSTEM` mode (locked Q2):** v1 detects OS appearance *at install time only*. A *listener* that re-fires `install()` on OS light/dark changes is a fast follow — it is purely additive (no API-surface change), so it is deliberately deferred to keep v1's lifecycle surface minimal (a live listener carries an unregister/ownership concern not worth taking on before the basics are proven).
- **Persistence is the consumer's job.** FlatComp does not touch `Preferences`; OWS-Local-Search-GUI persists the chosen `(themeName, mode)` and replays it into a `Config` at startup.

---

## 7. Where palettes come from

Per design-direction §13, the *vocabulary* is FlatComp's and the *values* are the consumer's. Concretely:

- **FlatComp ships** `MaterialPalettes.baseline()` (a `Theme`) for pipeline validation, and *optionally* a small curated set (`MaterialPalettes.indigo()`, etc.).
- **Palettes are JSON resources, not hand-written Java.** A `Palette` / `Theme` is loaded from a JSON file shaped like the M3 theme builder's export, via a `PaletteLoader`. This means a consumer (OWS) drops an M3-builder export into their own resources and loads it — no transcription, no FlatComp code change.
- **Locked (Q4):** the palette JSON uses a thin **FlatComp-normalized schema** that maps 1:1 to the 49 `ColorRole` keys, with a documented (and automatable) conversion from the M3 theme builder's export. FlatComp owns the contract — it is not hostage to the builder's format churn.
- The in-code M3-algorithm generator (source color → full `Palette` via HCT tonal palettes) remains a **v2** nicety, per design-direction §4 / the taxonomy. v1 is explicit-palette-only.

---

## 8. Resolved decisions

All five questions that gated the lock, resolved 2026-05-14:

| # | Question | Locked decision |
|---|---|---|
| Q1 | The full FlatLaf-native key mapping (§4) — 40–80 keys. | **Separate locked appendix**; does not block the API shape. Enumerated + validated in the playground. |
| Q2 | `SYSTEM` mode — install-time detection only, or also a live OS-change listener? | **Detect-at-install only** for v1; OS-change listener is a purely-additive fast follow. |
| Q3 | Do shape/spacing values live inside `Palette`, or in a separate object? | **Neither** — they are fixed enums, not palette data. But they resolve from `UIManager` at paint time like every other token (§1.4), which keeps themeable geometry a cheap, component-free change later. |
| Q4 | Palette JSON schema — M3 builder's native export shape, or a FlatComp-normalized shape? | **FlatComp-normalized schema** (1:1 with `ColorRole`), with a documented, automatable conversion from the M3 export. |
| Q5 | Is `ElwhaTheme` a static facade or an instantiable object? | **Static facade** — one LAF per JVM (design-direction §12), so a global static API matches physical reality. |

With the theme API fixed, the next deliverable is the `FlatChip` variant retrofit (design-direction §15 step 4 — variants expressed as token role-sets). The FlatLaf-native key-mapping appendix (Q1) and the palette JSON schema (Q4) are delivered below as Appendix A and Appendix B.

---

## Appendix A — FlatLaf-native key mapping (locked)

**Status:** LOCKED. The curated FlatLaf `UIManager` key → token role mapping resolving Q1. Implemented in `FlatLafKeyMapping` and applied by `install()` as steps 5 and 6. **Visual validation in the playground (`ThemePlayground`) is still pending an operator eyeball — the table below is the locked intent; a role reassignment after visual review is an appendix amendment, not an API change.**

The mapping is deliberately a curated subset, not exhaustive — FlatLaf exposes hundreds of keys; FlatComp maps the ones that make raw Swing read as coherent next to a `FlatChip`.

**Focus model — a focus ring.** Focus is indicated by a `PRIMARY`-colored ring, not by a fill or border swap. `Component.focusWidth` (FlatLaf-default `0`) is raised to `2`, and `Component.focusColor` is `PRIMARY`, so any focused component draws a consistent ring. Every `*.focused*` background / border key resolves to its *resting* equivalent — focus changes neither the fill nor the border, only adds the ring. This keeps one unambiguous, accessible focus cue across buttons, fields, combos, and lists, and avoids the "focused button looks stuck-pressed" reading that a `focusedBackground` fill produces. (M3 also defines a subtle focus *state layer*; v1 uses the ring alone and may layer the state-layer tint in later.)

**Focus-key completeness within the curated scope.** An unmapped `*.focused*` or `*.hover*` key does *not* mean "no effect" — FlatLaf falls back to its built-in blue accent default. So for every component in the curated scope, *all* of its focus/hover keys are mapped (to their resting equivalents, per the focus model above), not just the resting-state ones. Components deliberately left out of v1 scope — `HelpButton` (the `?` button) and tab-style toggle buttons (`ToggleButton.tab.*`) — keep FlatLaf's defaults; a consumer using those will see FlatLaf's blue focus styling until they are brought into scope.

**Shared icon palette for checkbox and radio.** `FlatRadioButtonIcon` extends `FlatCheckBoxIcon` and reads its color fields from the literal `CheckBox.icon.*` keys — only `RadioButton.icon.style` and `RadioButton.icon.centerDiameter` are radio-specific. So the icon-color palette below is mapped once on `CheckBox.icon.*` and serves both controls; the separate `JCheckBox` / `JRadioButton` label background/foreground keys still exist for the label colors.

**`focusedSelectedBackground` must be set explicitly.** Its FlatLaf fallback is `focusedBackground` (not `selectedBackground`). With the focus-ring model, `focusedBackground` resolves to the unchecked-resting `background`, so an unset `focusedSelectedBackground` makes a focused, *checked* checkbox / radio render as empty. The mapping below sets it explicitly.

### A.1 Static keys — direct role assignments (`applyStaticKeys`)

| FlatLaf key | Role / token |
|---|---|
| `Component.focusColor` | `PRIMARY` — the focus-ring color |
| `Component.focusWidth` | `2` — focus-ring width (FlatLaf default `0`; integer key, so non-integer values truncate) |
| `Component.focusedBorderColor` | `OUTLINE` — equal to the resting border; the ring is the cue |
| `Component.borderColor` | `OUTLINE` |
| `Component.disabledBorderColor` | `OUTLINE_VARIANT` |
| `Component.arc` | `ShapeScale.SM` |
| `Component.error.borderColor`, `Component.error.focusedBorderColor` | `ERROR` |
| `Panel.background`, `Viewport.background`, `RootPane.background` | `SURFACE` |
| `Panel.foreground`, `Label.foreground` | `ON_SURFACE` |
| `Label.disabledForeground` | `ON_SURFACE_VARIANT` |
| `Separator.foreground` | `OUTLINE_VARIANT` |
| `Button.background`, `ToggleButton.background` | `SURFACE_CONTAINER_LOW` |
| `Button.foreground`, `ToggleButton.foreground` | `ON_SURFACE` |
| `Button.borderColor` | `OUTLINE` |
| `Button.disabledBorderColor` | `OUTLINE_VARIANT` |
| `Button.default.background`, `Button.default.borderColor` | `PRIMARY` |
| `Button.default.foreground` | `ON_PRIMARY` |
| `Button.focusedBackground` | `SURFACE_CONTAINER_LOW` — equal to `Button.background` (focus = ring only) |
| `Button.focusedBorderColor`, `Button.hoverBorderColor` | `OUTLINE` — equal to `Button.borderColor` |
| `Button.default.focusedBackground` | `PRIMARY` — equal to `Button.default.background` |
| `Button.default.focusedBorderColor`, `Button.default.focusColor`, `Button.default.hoverBorderColor` | `PRIMARY` — equal to `Button.default.borderColor` |
| `ToggleButton.selectedBackground` | `PRIMARY_CONTAINER` |
| `ToggleButton.selectedForeground` | `ON_PRIMARY_CONTAINER` |
| `TextField.background`, `FormattedTextField.background`, `PasswordField.background`, `TextArea.background`, `EditorPane.background` | `SURFACE` |
| `TextField.foreground`, `FormattedTextField.foreground`, `PasswordField.foreground`, `TextArea.foreground`, `EditorPane.foreground` | `ON_SURFACE` |
| `TextField.placeholderForeground` | `ON_SURFACE_VARIANT` |
| `TextComponent.selectionBackground` | `PRIMARY_CONTAINER` |
| `TextComponent.selectionForeground` | `ON_PRIMARY_CONTAINER` |
| `ComboBox.background`, `Spinner.background` | `SURFACE` |
| `ComboBox.foreground`, `Spinner.foreground` | `ON_SURFACE` |
| `ComboBox.buttonBackground` | `SURFACE_CONTAINER_LOW` |
| `CheckBox.background`, `RadioButton.background` | `SURFACE` — label background |
| `CheckBox.foreground`, `RadioButton.foreground` | `ON_SURFACE` — label foreground |
| `CheckBox.icon.background` | `SURFACE` — icon palette is shared (see note) |
| `CheckBox.icon.borderColor` | `OUTLINE` |
| `CheckBox.icon.selectedBackground`, `CheckBox.icon.selectedBorderColor` | `PRIMARY` |
| `CheckBox.icon.checkmarkColor` | `ON_PRIMARY` |
| `CheckBox.icon.focusedBackground` | `SURFACE` — equal to `background` (focus = ring only) |
| `CheckBox.icon.focusedBorderColor`, `CheckBox.icon.hoverBorderColor`, `CheckBox.icon.pressedBorderColor` | `OUTLINE` — equal to `borderColor` |
| `CheckBox.icon.focusedSelectedBackground`, `CheckBox.icon.focusedSelectedBorderColor` | `PRIMARY` — equal to `selectedBackground` (MUST be set: fallback is `focusedBackground`, which makes a focused-checked icon render empty otherwise) |
| `RadioButton.icon.style` | `"outlined"` — splits the shared icon palette so radio takes the canonical M3 ring look while the checkbox keeps its filled look |
| `CheckBox.icon[outlined].selectedBackground`, `…focusedSelectedBackground` | `SURFACE` — radio interior stays transparent when checked |
| `CheckBox.icon[outlined].focusedSelectedBorderColor` | `PRIMARY` — focused-checked ring stays primary |
| `CheckBox.icon[outlined].checkmarkColor` | `PRIMARY` — radio dot color (the "checkmark" for radio paints as the center dot) |
| `CheckBox.icon[outlined].selectedBorderWidth` | `2` — thicker ring when checked (M3 radio spec) |
| `List.background`, `Table.background`, `Tree.background` | `SURFACE` |
| `List.foreground`, `Table.foreground`, `Tree.foreground` | `ON_SURFACE` |
| `List.selectionBackground`, `Table.selectionBackground`, `Tree.selectionBackground` | `PRIMARY_CONTAINER` |
| `List.selectionForeground`, `Table.selectionForeground`, `Tree.selectionForeground` | `ON_PRIMARY_CONTAINER` |
| `List.cellFocusColor`, `Table.cellFocusColor` | `PRIMARY` |
| `Table.gridColor` | `OUTLINE_VARIANT` |
| `TableHeader.background` | `SURFACE_CONTAINER_HIGH` |
| `TableHeader.foreground` | `ON_SURFACE_VARIANT` |
| `ScrollPane.background`, `ScrollBar.track` | `SURFACE` |
| `ScrollBar.thumb` | `OUTLINE_VARIANT` |
| `ScrollBar.hoverThumbColor` | `OUTLINE` |
| `ScrollBar.pressedThumbColor` | `ON_SURFACE_VARIANT` |
| `TabbedPane.background` | `SURFACE` |
| `TabbedPane.foreground` | `ON_SURFACE_VARIANT` |
| `TabbedPane.selectedForeground` | `ON_SURFACE` |
| `TabbedPane.underlineColor` | `PRIMARY` |
| `ProgressBar.background` | `SURFACE_VARIANT` |
| `ProgressBar.foreground` | `PRIMARY` |
| `Slider.background` | `SURFACE` |
| `Slider.trackColor` | `SURFACE_VARIANT` — inactive (unfilled) track |
| `Slider.trackValueColor` | `PRIMARY` — active (filled) track |
| `Slider.thumbColor`, `Slider.focusedColor` | `PRIMARY` |
| `MenuBar.background` | `SURFACE` |
| `MenuBar.foreground`, `MenuItem.foreground`, `Menu.foreground` | `ON_SURFACE` |
| `PopupMenu.background` | `SURFACE_CONTAINER_HIGH` |
| `ToolTip.background` | `INVERSE_SURFACE` |
| `ToolTip.foreground` | `INVERSE_ON_SURFACE` |

### A.2 State-layer keys — computed and baked (`applyStateLayerKeys`)

Each value is the M3 state-layer overlay alpha-blended over a base role, baked once per install (install-API §5).

| FlatLaf key | Overlay | Base → tint |
|---|---|---|
| `Button.hoverBackground`, `ToggleButton.hoverBackground` | `HOVER` | `SURFACE_CONTAINER_LOW` → `ON_SURFACE` |
| `Button.pressedBackground`, `ToggleButton.pressedBackground` | `PRESSED` | `SURFACE_CONTAINER_LOW` → `ON_SURFACE` |
| `Button.default.hoverBackground` | `HOVER` | `PRIMARY` → `ON_PRIMARY` |
| `Button.default.pressedBackground` | `PRESSED` | `PRIMARY` → `ON_PRIMARY` |
| `List.hoverBackground`, `Table.hoverBackground`, `Tree.hoverBackground`, `MenuItem.hoverBackground`, `TabbedPane.hoverColor` | `HOVER` | `SURFACE` → `ON_SURFACE` |
| `List.selectionInactiveBackground` | `SELECTED` | `SURFACE` → `ON_SURFACE` |
| `TabbedPane.focusColor` | `FOCUS` | `SURFACE` → `PRIMARY` |
| `ComboBox.buttonHoverBackground` | `HOVER` | `SURFACE_CONTAINER_LOW` → `ON_SURFACE` |
| `ComboBox.buttonPressedBackground` | `PRESSED` | `SURFACE_CONTAINER_LOW` → `ON_SURFACE` |
| `MenuItem.selectionBackground` | (direct) | `PRIMARY_CONTAINER` |

---

## Appendix B — Palette JSON schema (locked)

**Status:** LOCKED. The FlatComp-normalized palette JSON schema resolving Q4. Loaded by `PaletteLoader`; the bundled `MaterialPalettes.baseline()` resource is its reference instance.

### B.1 Shape

A single JSON object:

```json
{
  "name": "Material Baseline",
  "description": "optional free text — ignored by the loader",
  "light": { "<roleKey>": "#rrggbb", ... 49 entries },
  "dark":  { "<roleKey>": "#rrggbb", ... 49 entries }
}
```

- `name` — string. The `Theme` name. Falls back to the resource path if absent.
- `description` — optional string, ignored by the loader (provenance / notes only).
- `light` / `dark` — objects, each holding **all 49** `ColorRole` keys in `camelCase` (`primary`, `onPrimaryContainer`, `surfaceContainerHighest`, …) mapped to a `#rrggbb` hex string.
- Completeness is enforced at load: `Palette.Builder.build()` reports every missing role at once. Unknown keys are ignored.
- The 12 mode-invariant **fixed** roles appear in *both* `light` and `dark` with identical values.

### B.2 Conversion from the M3 theme builder export

The M3 builder exports CSS custom properties named `--md-sys-color-<role>-<mode>` (e.g. `--md-sys-color-on-primary-container-light`). The automatable conversion is mechanical:

1. Strip the `--md-sys-color-` prefix and the `-light` / `-dark` suffix; route each into the matching mode object.
2. Convert the hyphenated role name to `camelCase` (`on-primary-container` → `onPrimaryContainer`).
3. Four roles the current M3 builder omits are **derived by canonical M3 rule**: `surfaceTint` = `primary`, `background` = `surface`, `onBackground` = `onSurface`, and `surfaceVariant` from the M3 baseline reference (consistent with the export's `onSurfaceVariant`).

The baseline resource (`src/main/resources/com/owspfm/ui/components/theme/palettes/baseline.json`) was produced this way from an operator-supplied M3 builder export, with the four derived roles filled per step 3.
