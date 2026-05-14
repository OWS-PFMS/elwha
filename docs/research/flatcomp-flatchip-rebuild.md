# FlatChip Token-Native Rebuild — Locked Decisions

**Status:** LOCKED. Authoritative plan for rebuilding `FlatChip`'s styling API on the token system. Decisions are not to be re-debated during execution — any change requires reopening this document with rationale.

**Drafted:** 2026-05-13 (as a narrow "retrofit") · **Re-scoped + locked:** 2026-05-14

**Author:** Charles Bryan (`cfb3@uw.edu`), via design conversation with Claude.

**Parents:**
- [`flatcomp-design-direction.md`](flatcomp-design-direction.md) — the design-system stance.
- [`flatcomp-token-taxonomy.md`](flatcomp-token-taxonomy.md) — the **locked** 79-token surface.
- [`flatcomp-theme-install-api.md`](flatcomp-theme-install-api.md) — the **locked** install API.

This document supersedes the narrower "retrofit" framing it began as. **`FlatChip` has zero consumers**, so its styling API is rebuilt outright in the token vocabulary rather than preserved. FlatChip's component *behavior* — interaction, slots, accessibility, the paint-time-resolution architecture — is good and is kept. Its *styling API* — raw `Color` / `Insets` / `Integer` setters, the `FlatChip.*` escape-hatch namespace, the `WARM_ACCENT` variant — is replaced.

This is design-direction §15 step 4, and the first work that touches component code.

---

## 1. Scope

**In scope:** rebuild `FlatChip`'s styling API as token-native — variants as treatment-only role declarations, M3 chip *types* as factory presets, typed `ColorRole` / `ShapeScale` / `SpaceScale` setters, color/foreground/state resolved from the token system.

**Out of scope:** `FlatChip`'s interaction model, slot system, accessibility, and paint architecture — these are kept (§3). `FlatCard` V2 is held entirely (operator decision, 2026-05-14).

**Why a rebuild, not a retrofit:** `FlatChip` has no consumers, so API breakage carries no cost. The operator's priority for this phase is design correctness over stability; the lib is pre-1.0. There is no reason to preserve a styling API the token system makes redundant or wrong.

## 2. Prerequisite — token layer in code first

The rebuild *consumes* the token facade. Before it can start:

1. The token facade enums — `ColorRole`, `ShapeScale`, `SpaceScale`, `TypeRole`, `StateLayer` — with their `resolve()` / `.px()` lookups.
2. `FlatCompTheme.install(Config)` and the `Palette` / `Theme` / `Config` types.
3. `MaterialPalettes.baseline()` — at least the baseline `Theme` to install and validate against.

**Two derived facade requirements** this rebuild surfaces (additive, consistent with the locked install-API design — note them in that doc when the facade is implemented):

- **`on`-pairing lookup** — `ColorRole` must expose the pairing `PRIMARY_CONTAINER → ON_PRIMARY_CONTAINER`, `SURFACE → ON_SURFACE`, etc. This is what makes the foreground a total, correct lookup and lets auto-contrast be deleted (§4.4).
- **Baseline fallback** — `ColorRole.resolve()` must fall back to the M3 baseline value when `UIManager` has no entry, so components are robust before any theme is installed (§7).

## 3. What is kept

`FlatChip` V1's component engineering is sound. Untouched by this rebuild:

- **Paint-time resolution.** All color resolution already runs inside `paintComponent`; `updateUI()` rebuilds the padding border. Already binding-rule-compliant (taxonomy Conventions).
- **Interaction** — `ChipInteractionMode` (the behavioral axis), hover-poll timer, focus handling, press/click/activate semantics.
- **Slots** — leading icon, leading affordance, trailing action button, context menu.
- **Selection** state + `PROPERTY_SELECTED` + listeners.
- **Accessibility** — `AccessibleFlatChip`, the role mapping.
- The `ChipTextLabel` dynamic-`getForeground()` trick — still the mechanism, just resolving a token-derived color now.

## 4. The rebuilt styling model

### 4.1 Variants = treatments only

`ChipVariant` becomes **treatment-only** — `FILLED` / `OUTLINED` / `GHOST`. `WARM_ACCENT` is **removed**: it was never a Material 3 concept, it bundled a *color choice* into what should be a *treatment* axis, and its name baked in a single-app warm-gold palette assumption. The secondary-container look is now `FILLED` + `setSurfaceRole(SECONDARY_CONTAINER)` — color and treatment cleanly orthogonal.

Each variant carries the token roles it represents:

```java
public enum ChipVariant {
  FILLED  (ColorRole.PRIMARY_CONTAINER, ColorRole.OUTLINE_VARIANT), // surfaceRole, borderRole
  OUTLINED(ColorRole.SURFACE,           ColorRole.OUTLINE),
  GHOST   (null,                        null);                      // transparent resting surface, no border
}
```

Foreground is **not** stored on the variant — it is *derived* from the effective surface role via the facade's `on`-pairing: `onPair(effectiveSurfaceRole ?? SURFACE)`. `GHOST` (null surface) pairs its foreground against `SURFACE` → `ON_SURFACE`.

### 4.2 M3 chip types = factory-method presets

Material 3's chip *types* — assist / filter / input / suggestion — are bundles of *interaction semantics + typical slots*, not visual styles. `FlatChip` already expresses all four by composing its orthogonal axes. Rather than re-bundle them into a rigid `ChipType` enum or a widget hierarchy (which would repeat the `WARM_ACCENT` mistake — bundling orthogonal things into rigid buckets — and contradict the "Material-flavored, not spec-compliant" stance), they ship as **factory-method presets** over the existing axes:

| Factory | Pre-configures | Returns |
|---|---|---|
| `FlatChip.assistChip(text)` | `CLICKABLE` interaction, `OUTLINED` treatment | a fully further-configurable `FlatChip` |
| `FlatChip.filterChip(text)` | `SELECTABLE` interaction, `OUTLINED` treatment | " |
| `FlatChip.inputChip(text, onRemove)` | `CLICKABLE` interaction, `OUTLINED` treatment, trailing remove affordance wired to `onRemove` | " |
| `FlatChip.suggestionChip(text)` | `CLICKABLE` interaction, `OUTLINED` treatment | " |

The preset is *sugar over composition* — it sets sensible defaults; everything stays overridable via the normal setters. `OUTLINED` is the default treatment because M3 chips are outlined in their resting state. A bare `new FlatChip()` keeps `FILLED` as the FlatComp default.

### 4.3 Typed styling setters

Raw-typed escape-hatch setters become typed token selectors. You can pick any *role* or *scale step*; you cannot pick an arbitrary `Color` / `Insets` / pixel value — that is the cohesion guarantee.

| Removed (raw) | Added (typed) | Notes |
|---|---|---|
| `setSurfaceColor(Color)` | `setSurfaceRole(ColorRole)` | overrides the variant's default surface role; foreground re-pairs automatically |
| `setCornerRadius(Integer)` | `setShape(ShapeScale)` | `setShape(ShapeScale.FULL)` gives the old capsule (taxonomy Q1) |
| `setPadding(Insets)` / `setPadding(int,int)` | `setPadding(SpaceScale horizontal, SpaceScale vertical)` | typed to the scale ladder |
| `setForegroundColor(Color)` | *(none)* | foreground is always the `on`-pair of the effective surface role — an independent override would re-introduce unpaired surface/foreground |
| `setBorderColor(Color)` | *(none — deliberate omission)* | border color is variant-derived; `setBorderRole(ColorRole)` is intentionally not added (YAGNI — add only if a real need surfaces) |

`setBorderWidth(int)` is **kept** — stroke width is genuine geometry with no token in the taxonomy; it is not an escape hatch.

### 4.4 Color & foreground resolution

- **Surface** — `effectiveSurfaceRole()?.resolve()`, where the effective role is the per-instance `setSurfaceRole` override if set, else the variant's default. `GHOST` resolves to `null` (transparent until interacted — unchanged behavior).
- **Border** — the variant's `borderRole()?.resolve()`. `selected` → accent (`PRIMARY`) border; `focused` → accent border at the thicker stroke. (State precedence unchanged from V1.)
- **Foreground** — `onPair(effectiveSurfaceRole ?? SURFACE).resolve()`. Always correct by construction.
- **Auto-contrast is deleted entirely.** V1's `resolveForegroundColor()` luma branch, `AUTO_FG_DARK` / `AUTO_FG_LIGHT`, `isLight()`, `effectiveSurfaceForContrast()` — all removed. They existed only because V1 had no `on*` pairs and a caller could set an arbitrary off-role surface. Neither is true anymore: every surface is a role, every role has its `on`-pair.

### 4.5 State layers = uniform `StateLayer` opacities

V1's per-state, per-variant blend ratios (`composeHoverBackground` at 0.18/0.45, `selectedFillStrength()` at 1.0/0.22/0.6 per variant) are replaced by the locked uniform `StateLayer` model: `HOVER` 8% / `PRESSED` 10% / `SELECTED` 12% / `DRAGGED` 16%, composited over the resolved surface, tinted by the surface's `on`-role. `selectedFillStrength()`'s per-variant character is dropped — uniformity is the locked design (taxonomy §5). See §9 for the validation this needs.

### 4.6 The `FlatChip.*` UIManager namespace is removed

All ~12 `FlatChip.*` keys and their `K_*` public constants are **deleted**. Nothing is left for them to do:

- `FlatChip.hoverBackground` / `pressedBackground` / `selectedBackground` / `selectedBorderColor` — state is now uniform `StateLayer` overlays over tokens.
- `FlatChip.warmAccent` — `WARM_ACCENT` is gone.
- `FlatChip.background` / `borderColor` / `foreground` — replaced by `setSurfaceRole` + variant roles + `on`-pairing.
- `FlatChip.arc` / `padding` — replaced by `setShape` / `setPadding` and the `FlatComp.shape.*` / `FlatComp.space.*` tokens.
- `FlatChip.focusColor` / `disabledBackground` — focus is the `PRIMARY` role; disabled is the taxonomy's disabled opacity treatment (§5.1).

App-wide chip theming now happens through the palette + variant roles; per-instance happens through the typed setters. There is no remaining gap a `FlatChip.*` key would fill.

## 5. API delta

**Removed:** `setSurfaceColor(Color)`, `setForegroundColor(Color)`, `setCornerRadius(Integer)`, `setBorderColor(Color)`, `setPadding(Insets)`, `setPadding(int,int)`, the entire `FlatChip.*` UIManager namespace + all `K_*` constants, `ChipVariant.WARM_ACCENT`, the auto-contrast machinery.

**Added:** `setSurfaceRole(ColorRole)`, `setShape(ShapeScale)`, `setPadding(SpaceScale, SpaceScale)`, role fields on `ChipVariant`, the four `FlatChip.*Chip(...)` factory methods.

**Changed:** `getEffectiveCornerRadius()` → `getEffectiveShape()` (returns `ShapeScale`); default shape capsule → `SM` (taxonomy Q1).

**Kept:** `setVariant`, `setInteractionMode`, `setBorderWidth(int)`, all slot setters (`setLeadingIcon`, `setLeadingAffordance`, `setTrailingAction`, `setTrailingIcon`, context-menu API), `setSelected` + listeners, all interaction and accessibility surface.

## 6. What visibly changes from V1

| Change | Why |
|---|---|
| Default corner radius: capsule (999) → `SM` (8px) | Taxonomy Q1 |
| State-layer feel: per-variant blend ratios → uniform M3 opacities | Locked `StateLayer` model |
| Variant colors come from the palette, not blends off `Panel.background` | The point of the rebuild |
| `WARM_ACCENT` removed | Not an M3 concept; color/treatment now orthogonal |
| Styling is role/scale-typed, not raw `Color`/`Insets`/`Integer` | Cohesion guarantee — design-direction §11 |

## 7. Graceful degradation — no theme installed

If `FlatChip` is used before `FlatCompTheme.install(...)` runs, the `FlatComp.*` keys are absent. **Resolved (was Q3):** the `ColorRole` facade itself falls back to the M3 baseline value — decided once, every component robust, no per-component hardcoded fallbacks (which would re-scatter the magic numbers the system exists to remove). This is the second derived facade requirement from §2.

## 8. Locked decisions

| Topic | Decision |
|---|---|
| Variant axis | Treatment-only: `FILLED` / `OUTLINED` / `GHOST`. `WARM_ACCENT` removed. |
| M3 chip types | Factory-method presets (`assistChip` / `filterChip` / `inputChip` / `suggestionChip`) over the orthogonal axes — not a rigid `ChipType` enum or widget hierarchy. |
| Color override | `setSurfaceRole(ColorRole)` — typed role selector, not raw `Color`. |
| Foreground (was Q1) | Always the `on`-pair of the effective surface role. Auto-contrast **deleted entirely**. |
| State layers (was Q2) | Uniform `StateLayer` opacities; per-variant `selectedFillStrength` dropped. Validate in playground (§9). |
| No-theme fallback (was Q3) | Baseline fallback lives in the `ColorRole` facade. |
| `FlatChip.*` keys (was Q4) | Entire namespace + `K_*` constants removed. |
| `WARM_ACCENT` (was Q5) | Removed (collapsed into `FILLED` + `setSurfaceRole`). |
| Shape | Typed `setShape(ShapeScale)`; default `SM`, `FULL` available. |
| Border color | Variant-derived; no `setBorderRole` for v1 (deliberate omission). |

## 9. Validation

No JUnit infrastructure exists (per `CLAUDE.md`) — validation is visual via `FlatChipPlayground`. The rebuild must be exercised across:

- Every `ChipVariant` × every `ChipInteractionMode` × {idle, hover, pressed, selected, focused, disabled}.
- All four factory presets.
- Light **and** dark mode, plus a runtime theme switch (confirms the binding rule end-to-end).
- At least one non-baseline palette (confirms variants are genuinely palette-driven).
- `setSurfaceRole` with a non-default role (e.g. `TERTIARY_CONTAINER`) — confirm the foreground re-pairs correctly.

**Specific thing to eyeball (the Q2 risk):** selected `OUTLINED` under the uniform 12% `SELECTED` overlay. V1 deliberately kept it mostly-transparent so it didn't read as a generic filled chip. If the uniform overlay loses that distinction, the likely fix is to let selection preserve `OUTLINED`'s identity via the *border* (swap to the accent role) rather than by a weaker fill — uniform fill, distinct border. Decide that on observed rendering, not preemptively.
