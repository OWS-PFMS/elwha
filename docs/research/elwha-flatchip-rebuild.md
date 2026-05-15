# ElwhaChip Token-Native Rebuild — Locked Decisions

**Status:** LOCKED, EXECUTED (epic #31, 2026-05-15). The plan landed end-to-end across sub-issues #36 / #37 / #38 / #39 in one bundled PR; the visual §9 Q2 risk surfaced two collapses during smoketest — the preemptive `PRIMARY` border swap on selected handles `OUTLINED` cleanly, but exposed that `GHOST + SELECTED` collapses with `OUTLINED + SELECTED`. Resolved per the §9 Q2 amendment ([#50](https://github.com/OWS-PFMS/elwha/issues/50)): `GHOST` does not participate in selection rendering, aligning with M3's treatment of its Text-button emphasis level.

**Drafted:** 2026-05-13 (as a narrow "retrofit") · **Re-scoped + locked:** 2026-05-14 · **Amended:** 2026-05-14 (SurfacePainter extraction folded into #37 — see Amendment below) · **Executed:** 2026-05-15

**Author:** Charles Bryan (`cfb3@uw.edu`), via design conversation with Claude.

**Parents:**
- [`elwha-design-direction.md`](elwha-design-direction.md) — the design-system stance.
- [`elwha-token-taxonomy.md`](elwha-token-taxonomy.md) — the **locked** 79-token surface.
- [`elwha-theme-install-api.md`](elwha-theme-install-api.md) — the **locked** install API.

This document supersedes the narrower "retrofit" framing it began as. **`ElwhaChip` has zero consumers**, so its styling API is rebuilt outright in the token vocabulary rather than preserved. ElwhaChip's component *behavior* — interaction, slots, accessibility, the paint-time-resolution architecture — is good and is kept. Its *styling API* — raw `Color` / `Insets` / `Integer` setters, the `ElwhaChip.*` escape-hatch namespace, the `WARM_ACCENT` variant — is replaced.

This is design-direction §15 step 4, and the first work that touches component code.

---

## Amendment — 2026-05-14

**Change:** Sub-issue #37 ("rewire ElwhaChip styling to tokens") expands to include extracting a `SurfacePainter` internal helper, and routing ElwhaChip's background painting (round-rect fill + border + state-layer overlay) through it from day one. New §4.7 added below; corresponding row added to §8.

**Rationale:** The original rebuild plan kept Chip's paint code in-class (§3, "What is kept"). After the locked plan was drafted, the design review identified `ElwhaSurface` as a primitive the library needs (issue #43), and `ElwhaCard` V2 will compose Surface as its background. All three components — Chip, Surface, Card V2 — paint the same round-rect token-resolved surface. Putting that paint logic in exactly one place from day one (via #37) is meaningfully cheaper than rewriting Chip's paint code twice: once here, once when #43 extracts it later. Doing the extraction here also shapes the helper API around the harder consumer (Chip — with state-layer overlays, focus rings, variant-driven borders) rather than retrofitting it from a simpler one.

**Resolved-decision impact:** None of the locked design decisions change. §3 "What is kept" still applies — paint-time resolution, the binding rule, the slot / interaction / accessibility surface — only the *where* of the paint *implementation* moves from in-class to a delegated helper. The component still owns its `paintComponent`; it just calls into `SurfacePainter` for the surface background.

---

## 1. Scope

**In scope:** rebuild `ElwhaChip`'s styling API as token-native — variants as treatment-only role declarations, M3 chip *types* as factory presets, typed `ColorRole` / `ShapeScale` / `SpaceScale` setters, color/foreground/state resolved from the token system.

**Out of scope:** `ElwhaChip`'s interaction model, slot system, accessibility, and paint architecture — these are kept (§3). `ElwhaCard` V2 is held entirely (operator decision, 2026-05-14).

**Why a rebuild, not a retrofit:** `ElwhaChip` has no consumers, so API breakage carries no cost. The operator's priority for this phase is design correctness over stability; the lib is pre-1.0. There is no reason to preserve a styling API the token system makes redundant or wrong.

## 2. Prerequisite — token layer in code first

The rebuild *consumes* the token facade. Before it can start:

1. The token facade enums — `ColorRole`, `ShapeScale`, `SpaceScale`, `TypeRole`, `StateLayer` — with their `resolve()` / `.px()` lookups.
2. `ElwhaTheme.install(Config)` and the `Palette` / `Theme` / `Config` types.
3. `MaterialPalettes.baseline()` — at least the baseline `Theme` to install and validate against.

**Two derived facade requirements** this rebuild surfaces (additive, consistent with the locked install-API design — note them in that doc when the facade is implemented):

- **`on`-pairing lookup** — `ColorRole` must expose the pairing `PRIMARY_CONTAINER → ON_PRIMARY_CONTAINER`, `SURFACE → ON_SURFACE`, etc. This is what makes the foreground a total, correct lookup and lets auto-contrast be deleted (§4.4).
- **Baseline fallback** — `ColorRole.resolve()` must fall back to the M3 baseline value when `UIManager` has no entry, so components are robust before any theme is installed (§7).

## 3. What is kept

`ElwhaChip` V1's component engineering is sound. Untouched by this rebuild:

- **Paint-time resolution.** All color resolution already runs inside `paintComponent`; `updateUI()` rebuilds the padding border. Already binding-rule-compliant (taxonomy Conventions).
- **Interaction** — `ChipInteractionMode` (the behavioral axis), hover-poll timer, focus handling, press/click/activate semantics.
- **Slots** — leading icon, leading affordance, trailing action button, context menu.
- **Selection** state + `PROPERTY_SELECTED` + listeners.
- **Accessibility** — `AccessibleElwhaChip`, the role mapping.
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

Material 3's chip *types* — assist / filter / input / suggestion — are bundles of *interaction semantics + typical slots*, not visual styles. `ElwhaChip` already expresses all four by composing its orthogonal axes. Rather than re-bundle them into a rigid `ChipType` enum or a widget hierarchy (which would repeat the `WARM_ACCENT` mistake — bundling orthogonal things into rigid buckets — and contradict the "Material-flavored, not spec-compliant" stance), they ship as **factory-method presets** over the existing axes:

| Factory | Pre-configures | Returns |
|---|---|---|
| `ElwhaChip.assistChip(text)` | `CLICKABLE` interaction, `OUTLINED` treatment | a fully further-configurable `ElwhaChip` |
| `ElwhaChip.filterChip(text)` | `SELECTABLE` interaction, `OUTLINED` treatment | " |
| `ElwhaChip.inputChip(text, onRemove)` | `CLICKABLE` interaction, `OUTLINED` treatment, trailing remove affordance wired to `onRemove` | " |
| `ElwhaChip.suggestionChip(text)` | `CLICKABLE` interaction, `OUTLINED` treatment | " |

The preset is *sugar over composition* — it sets sensible defaults; everything stays overridable via the normal setters. `OUTLINED` is the default treatment because M3 chips are outlined in their resting state. A bare `new ElwhaChip()` keeps `FILLED` as the Elwha default.

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

### 4.6 The `ElwhaChip.*` UIManager namespace is removed

All ~12 `ElwhaChip.*` keys and their `K_*` public constants are **deleted**. Nothing is left for them to do:

- `ElwhaChip.hoverBackground` / `pressedBackground` / `selectedBackground` / `selectedBorderColor` — state is now uniform `StateLayer` overlays over tokens.
- `ElwhaChip.warmAccent` — `WARM_ACCENT` is gone.
- `ElwhaChip.background` / `borderColor` / `foreground` — replaced by `setSurfaceRole` + variant roles + `on`-pairing.
- `ElwhaChip.arc` / `padding` — replaced by `setShape` / `setPadding` and the `Elwha.shape.*` / `Elwha.space.*` tokens.
- `ElwhaChip.focusColor` / `disabledBackground` — focus is the `PRIMARY` role; disabled is the taxonomy's disabled opacity treatment (§5.1).

App-wide chip theming now happens through the palette + variant roles; per-instance happens through the typed setters. There is no remaining gap a `ElwhaChip.*` key would fill.

### 4.7 Paint implementation — `SurfacePainter` helper (amendment 2026-05-14)

The round-rect token-fill + border + state-layer overlay logic moves into an internal `SurfacePainter` helper, called from `ElwhaChip.paintComponent`. The helper's responsibility is the *surface* portion of the paint:

- Round-rect fill from a `ColorRole` (resolved at call time per the binding rule).
- Round-rect border from a `ColorRole` + stroke width.
- State-layer overlay (a `StateLayer` opacity tinted by the `on`-pair of the surface role), composited between fill and border.

`ElwhaChip.paintComponent` keeps everything chip-specific — content layout, icon, label, trailing affordance, focus ring. Only the surface background delegates. The same helper is then used by `ElwhaSurface` (#43) and `ElwhaCard` V2 (#253), so round-rect token-resolved surface painting lives in exactly one place across the library.

**Where it lives:** `com.owspfm.elwha.theme.SurfacePainter`. Package-private or limited visibility — it is an internal helper, not part of the public API.

**Removed:** `setSurfaceColor(Color)`, `setForegroundColor(Color)`, `setCornerRadius(Integer)`, `setBorderColor(Color)`, `setPadding(Insets)`, `setPadding(int,int)`, the entire `ElwhaChip.*` UIManager namespace + all `K_*` constants, `ChipVariant.WARM_ACCENT`, the auto-contrast machinery.

**Added:** `setSurfaceRole(ColorRole)`, `setShape(ShapeScale)`, `setPadding(SpaceScale, SpaceScale)`, role fields on `ChipVariant`, the four `ElwhaChip.*Chip(...)` factory methods.

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

If `ElwhaChip` is used before `ElwhaTheme.install(...)` runs, the `Elwha.*` keys are absent. **Resolved (was Q3):** the `ColorRole` facade itself falls back to the M3 baseline value — decided once, every component robust, no per-component hardcoded fallbacks (which would re-scatter the magic numbers the system exists to remove). This is the second derived facade requirement from §2.

## 8. Locked decisions

| Topic | Decision |
|---|---|
| Variant axis | Treatment-only: `FILLED` / `OUTLINED` / `GHOST`. `WARM_ACCENT` removed. |
| M3 chip types | Factory-method presets (`assistChip` / `filterChip` / `inputChip` / `suggestionChip`) over the orthogonal axes — not a rigid `ChipType` enum or widget hierarchy. |
| Color override | `setSurfaceRole(ColorRole)` — typed role selector, not raw `Color`. |
| Foreground (was Q1) | Always the `on`-pair of the effective surface role. Auto-contrast **deleted entirely**. |
| State layers (was Q2) | Uniform `StateLayer` opacities; per-variant `selectedFillStrength` dropped. Validate in playground (§9). |
| No-theme fallback (was Q3) | Baseline fallback lives in the `ColorRole` facade. |
| `ElwhaChip.*` keys (was Q4) | Entire namespace + `K_*` constants removed. |
| `WARM_ACCENT` (was Q5) | Removed (collapsed into `FILLED` + `setSurfaceRole`). |
| Shape | Typed `setShape(ShapeScale)`; default `SM`, `FULL` available. |
| Border color | Variant-derived; no `setBorderRole` for v1 (deliberate omission). |
| Paint implementation (amendment 2026-05-14) | Surface painting (fill + border + state-layer overlay) extracted to internal `SurfacePainter` helper as part of #37; `ElwhaChip.paintComponent` delegates the background to it. Same helper used by `ElwhaSurface` (#43) and `ElwhaCard` V2 (#253). See §4.7. |

## 9. Validation

No JUnit infrastructure exists (per `CLAUDE.md`) — validation is visual via `ElwhaChipPlayground`. The rebuild must be exercised across:

- Every `ChipVariant` × every `ChipInteractionMode` × {idle, hover, pressed, selected, focused, disabled}.
- All four factory presets.
- Light **and** dark mode, plus a runtime theme switch (confirms the binding rule end-to-end).
- At least one non-baseline palette (confirms variants are genuinely palette-driven).
- `setSurfaceRole` with a non-default role (e.g. `TERTIARY_CONTAINER`) — confirm the foreground re-pairs correctly.

**Specific thing to eyeball (the Q2 risk):** selected `OUTLINED` under the uniform 12% `SELECTED` overlay. V1 deliberately kept it mostly-transparent so it didn't read as a generic filled chip. If the uniform overlay loses that distinction, the likely fix is to let selection preserve `OUTLINED`'s identity via the *border* (swap to the accent role) rather than by a weaker fill — uniform fill, distinct border. Decide that on observed rendering, not preemptively.

**§9 Q2 amendment (2026-05-15, issue [#50](https://github.com/OWS-PFMS/elwha/issues/50)).** Observed-rendering check on PR #46 surfaced a second collapse the preemptive border swap didn't cover: `GHOST + SELECTED` and `OUTLINED + SELECTED` rendered identically (both produce no fill + 12 % overlay + 1 px `PRIMARY` border under the accent-border rule). Two of three variants lost their identity in the selected state, and `GHOST + SELECTED` additionally read as a faded `FILLED` chip — defeating GHOST's "no surface unless interacting" intent. M3 doesn't define a ghost-style chip variant; its closest analog is the Text button (lowest emphasis level), and the spec doesn't document a selected state on that emphasis level. **Resolution:** `GHOST` does not participate in selection rendering. The `selected` field and `PROPERTY_SELECTED` events remain on the API surface (so `setVariant(...)` later to a non-GHOST variant resumes rendering the selected state), but the rendering pipeline ignores `selected` while the variant is `GHOST` — no `SELECTED` state-layer overlay, no border swap. Consumers needing a togglable chip with a visible selected state should use `OUTLINED` (M3's own default treatment for filter chips). `ChipVariant × ChipInteractionMode` orthogonality is preserved at the API level — only the visual contract changes.
