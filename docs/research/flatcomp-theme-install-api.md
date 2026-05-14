# FlatComp Theme Install API — Locked Decisions

**Status:** LOCKED. Authoritative theme-install API for FlatComp v1. Decisions are not to be re-debated during execution — any change requires reopening this document with rationale.

**Drafted:** 2026-05-14 · **Locked:** 2026-05-14

**Author:** Charles Bryan (`cfb3@uw.edu`), drafted via design conversation with Claude.

**Parents:**
- [`flatcomp-design-direction.md`](flatcomp-design-direction.md) — the design-system stance.
- [`flatcomp-token-taxonomy.md`](flatcomp-token-taxonomy.md) — the **locked** 79-token surface this API installs.

This document specifies how a palette becomes a live theme: the `Palette` / `Theme` data types, the `FlatCompTheme` install entry point, what install does to `UIManager`, the state-layer compute-and-bake step, runtime switching, and where palettes come from. It is designed against the fixed token surface — it does not revisit which tokens exist.

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

They are still **resolved from `UIManager` at paint time**, exactly like color and type — `SpaceScale.MD.px()` does a `UIManager.getInt("FlatComp.space.md")` lookup, *not* a return of a compiled-in constant. `install()` writes the fixed v1 defaults into those keys.

**Why resolve, when the v1 values are fixed?** Two reasons. (1) **Consistency** — one uniform resolution model across all five token categories; a value-holder approach would make shape/spacing the odd one out. (2) **It keeps themeable geometry cheap later.** If a future "compact" theme needs to vary spacing, the lift is fully contained in the theme module: add an optional geometry block to `Theme` / the palette JSON, have `install()` read from it (falling back to the v1 defaults when absent). **No component code changes** — components already resolve from `UIManager` — and **no taxonomy reopen**, because the *set* of steps (`xs…xxl`) is unchanged; only whether their values vary. The value-holder alternative would have made that future change invasive: it alters the enum's contract and forces re-validation of every call site.

---

## 2. The install entry point

A single operation — "install this `(theme, mode, typography)` combination" — used for both startup and runtime switching.

```java
// Startup
FlatCompTheme.install(
    FlatCompTheme.config()
        .theme(MaterialPalettes.baseline())   // a Theme
        .mode(Mode.SYSTEM)                    // follow the OS
        .typography(Typography.defaults())    // bundled Inter
        .build());

// Runtime switch — same call, cheap derivation
FlatCompTheme.install(currentConfig.withMode(Mode.DARK));
FlatCompTheme.install(currentConfig.withTheme(themes.get("forest")));
```

- `FlatCompTheme.config()` → a `Config.Builder`. `theme(...)` is required; `mode(...)` defaults to `SYSTEM`; `typography(...)` defaults to the bundled Inter resolution.
- `install(Config)` is **idempotent and re-callable** — it is the only entry point. There is no separate "switch" API; switching *is* re-installing.
- `Config` is immutable with `withMode()` / `withTheme()` / `withTypography()` for cheap derivation on switch.
- `FlatCompTheme` holds the last-installed `Config` as `current()` so callers can derive from it without threading it through their own state.

---

## 3. What `install(Config)` does

An ordered sequence:

1. **Resolve mode.** If `SYSTEM`, detect OS appearance → concrete `LIGHT` / `DARK`.
2. **Install the base LAF.** `LIGHT` → `FlatLightLaf.setup()`, `DARK` → `FlatDarkLaf.setup()`. This gives every raw Swing component its modern baseline (scrollbars, focus rings, defaults for components FlatComp does not theme).
3. **Select the palette.** `theme.light()` or `theme.dark()` for the resolved mode.
4. **Write the `FlatComp.*` keys.** For every token, `UIManager.put("FlatComp.<category>.<key>", value)` — all 49 colors, 7 shapes, 6 spacings, 12 type fonts, 5 state opacities, 2 disabled constants. This is what FlatComp's own components resolve against.
5. **Write the FlatLaf-native keys.** Map roles onto FlatLaf's component keys so **raw Swing inherits the design language** (§4). This is the step that makes a plain `JButton` look like it belongs next to a `FlatChip`.
6. **Compute-and-bake state layers.** FlatLaf models hover/pressed as separate colors; M3 models them as opacity overlays. Blend the overlay over the role color and write the *result* into FlatLaf's `*hoverBackground` / `*pressedBackground` keys (§5).
7. **Apply typography.** Register the bundled Inter font; write `defaultFont` and the `FlatComp.type.*` fonts.
8. **Repaint.** `SwingUtilities.updateComponentTreeUI(w)` for every `Window` in `Window.getWindows()`. Components re-resolve tokens per the taxonomy's binding rule and re-skin live.

Steps 4–7 are pure `UIManager` population; step 8 is the only one that touches live components.

---

## 4. The FlatLaf-native key mapping

The bridge that makes raw Swing inherit the theme. FlatLaf exposes hundreds of `UIManager` keys; FlatComp maps a curated subset onto roles. Representative sketch — **not exhaustive**:

| FlatLaf key | Role | Notes |
|---|---|---|
| `Button.background` | `surface` | default (non-emphasis) button |
| `Button.foreground` | `onSurface` | |
| `Button.focusedBackground` | `SELECTED` over `surface` | baked, see §5 |
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
| Q5 | Is `FlatCompTheme` a static facade or an instantiable object? | **Static facade** — one LAF per JVM (design-direction §12), so a global static API matches physical reality. |

With the theme API fixed, the next deliverables are the `FlatChip` variant retrofit (design-direction §15 step 4 — variants expressed as token role-sets) and the FlatLaf-native key-mapping appendix (Q1).
