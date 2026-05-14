# FlatComp Token Taxonomy — Locked Decisions

**Status:** LOCKED. This is the authoritative token surface for FlatComp v1. Decisions are not to be re-debated during execution — any change requires reopening this document with rationale.

**Drafted:** 2026-05-13 · **Locked:** 2026-05-14 · **Amended:** 2026-05-14 (color role set 16 → 49 — see Amendment below)

**Author:** Charles Bryan (`cfb3@uw.edu`), via design conversation with Claude.

**Parent:** [`flatcomp-design-direction.md`](flatcomp-design-direction.md) — the design-system stance this taxonomy implements. Read that first.

This document enumerates the **exact token surface** for FlatComp v1: every color role, type role, shape step, spacing step, and state layer, with M3-derived values and the trim/resolution decisions behind each. The `FlatCompTheme` install API and all component variant work are designed against this fixed surface.

---

## Amendment — 2026-05-14

**Change:** `ColorRole` expanded from 16 → 49 — the full standard Material 3 color scheme — replacing the original trimmed subset (which had cut tertiary, inverse, the error-container pair, the surface-container ladder, the utility colors, and the fixed accents).

**Rationale:** the original trim conflated two different things. *Components* should be trimmed aggressively — YAGNI applies, since a component is expensive to build and cheap to add later. *Token vocabulary* is the opposite: the M3 theme builder generates the entire role set from one source color, so capturing it is **free**, while adding a role later is **expensive** — it reopens this locked doc, forces a backfill of every existing palette, and in the gap leaves consumers with no token for the missing role (so they hardcode a color — the exact drift the token system exists to prevent). Principle: **trim components, not vocabulary.**

**Resolved-decision impact:** Q5 (background) and Q6 (errorContainer) are superseded — both are now captured roles. Usage guidance still prefers `surface` over `background`, but that is a usage note, not a vocabulary omission. See §8.

---

## Conventions

- **Storage:** every token lives in `UIManager` under the `FlatComp.*` namespace. Components resolve through a typed facade (enums), never raw string lookups.
- **Key format:** `FlatComp.<category>.<camelCaseName>` — e.g. `FlatComp.color.onPrimaryContainer`, `FlatComp.shape.md`, `FlatComp.type.bodyMedium`, `FlatComp.state.hover`.
- **Enum → key mapping:** `ColorRole.ON_PRIMARY_CONTAINER` → `onPrimaryContainer`. Mechanical `SCREAMING_SNAKE` → `camelCase`.
- **Values below are the M3 *baseline* reference palette** — they ship as the default `MaterialPalettes.baseline()` light/dark schemes and exist to validate the pipeline end-to-end. An OWS-branded palette is a separate, consumer-side artifact (see design-direction §13) and is not in scope here.

### Binding rule — resolve at paint time, never cache

**Every FlatComp component MUST resolve tokens at paint time (or re-resolve on `updateUI()`) — never cache a resolved `Color` / `Font` / radius in the constructor or a field.**

**Why:** runtime theme switching works by re-writing the `FlatComp.*` and FlatLaf-native `UIManager` keys, then calling `SwingUtilities.updateComponentTreeUI(...)` on every live window. A component that resolved `ColorRole.PRIMARY_CONTAINER` once at construction holds a stale `Color` and won't re-skin — producing a "theme switch doesn't update chips" bug that is invisible until someone hits it.

**How to apply:** resolve inside `paintComponent` / `paintBorder`, or override `updateUI()` to re-resolve any token-derived state into fields. Memoizing *within a single paint pass* is fine; memoizing *across* paints is the violation. This rule is enforced by convention/review for v1; an ArchUnit or custom Checkstyle rule could mechanize it later.

---

## 1. Color roles — 49 (the full standard M3 scheme)

Semantic roles, not literal colors. Components reference roles; the palette supplies values. The set below is the **complete standard Material 3 color scheme** — every role the M3 theme builder emits from a single source color. See the Amendment for why v1 captures the whole scheme rather than a trimmed subset.

### 1.1 The role set

Grouped by family. The **v1 use** column flags whether a shipped FlatComp v1 component actively consumes the role, or whether it's captured for vocabulary completeness — themable and resolvable today, just not yet consumed by a component.

**Accent groups — primary / secondary / tertiary (12)**

| Enum | Key | v1 use |
|---|---|---|
| `PRIMARY` | `primary` | active |
| `ON_PRIMARY` | `onPrimary` | active |
| `PRIMARY_CONTAINER` | `primaryContainer` | active — `FILLED` variant |
| `ON_PRIMARY_CONTAINER` | `onPrimaryContainer` | active — `FILLED` variant |
| `SECONDARY` | `secondary` | active |
| `ON_SECONDARY` | `onSecondary` | active |
| `SECONDARY_CONTAINER` | `secondaryContainer` | active — `WARM_ACCENT` variant |
| `ON_SECONDARY_CONTAINER` | `onSecondaryContainer` | active — `WARM_ACCENT` variant |
| `TERTIARY` | `tertiary` | completeness |
| `ON_TERTIARY` | `onTertiary` | completeness |
| `TERTIARY_CONTAINER` | `tertiaryContainer` | completeness |
| `ON_TERTIARY_CONTAINER` | `onTertiaryContainer` | completeness |

**Error group (4)**

| Enum | Key | v1 use |
|---|---|---|
| `ERROR` | `error` | active |
| `ON_ERROR` | `onError` | active |
| `ERROR_CONTAINER` | `errorContainer` | completeness |
| `ON_ERROR_CONTAINER` | `onErrorContainer` | completeness |

**Surface family (11)**

| Enum | Key | v1 use |
|---|---|---|
| `SURFACE` | `surface` | active |
| `ON_SURFACE` | `onSurface` | active |
| `SURFACE_VARIANT` | `surfaceVariant` | active |
| `ON_SURFACE_VARIANT` | `onSurfaceVariant` | active |
| `SURFACE_DIM` | `surfaceDim` | completeness |
| `SURFACE_BRIGHT` | `surfaceBright` | completeness |
| `SURFACE_CONTAINER_LOWEST` | `surfaceContainerLowest` | completeness — elevation system (§6) |
| `SURFACE_CONTAINER_LOW` | `surfaceContainerLow` | completeness — elevation system (§6) |
| `SURFACE_CONTAINER` | `surfaceContainer` | completeness — elevation system (§6) |
| `SURFACE_CONTAINER_HIGH` | `surfaceContainerHigh` | completeness — elevation system (§6) |
| `SURFACE_CONTAINER_HIGHEST` | `surfaceContainerHighest` | completeness — elevation system (§6) |

**Outline (2)**

| Enum | Key | v1 use |
|---|---|---|
| `OUTLINE` | `outline` | active — `OUTLINED` variant stroke |
| `OUTLINE_VARIANT` | `outlineVariant` | active — subtle dividers |

**Inverse (3)**

| Enum | Key | v1 use |
|---|---|---|
| `INVERSE_SURFACE` | `inverseSurface` | completeness |
| `INVERSE_ON_SURFACE` | `inverseOnSurface` | completeness |
| `INVERSE_PRIMARY` | `inversePrimary` | completeness |

**Utility (3)**

| Enum | Key | v1 use |
|---|---|---|
| `SHADOW` | `shadow` | completeness — elevation system (§6) |
| `SCRIM` | `scrim` | completeness — modal scrims |
| `SURFACE_TINT` | `surfaceTint` | completeness — elevation system (§6) |

**Background (2)**

| Enum | Key | v1 use |
|---|---|---|
| `BACKGROUND` | `background` | completeness — components prefer `surface`; retained for M3 completeness |
| `ON_BACKGROUND` | `onBackground` | completeness |

**Fixed accents — mode-invariant (12)**

These hold the **same value in light and dark** (see §1.2) — for UI that must stay visually constant across a mode toggle.

| Enum | Key | v1 use |
|---|---|---|
| `PRIMARY_FIXED` | `primaryFixed` | completeness |
| `PRIMARY_FIXED_DIM` | `primaryFixedDim` | completeness |
| `ON_PRIMARY_FIXED` | `onPrimaryFixed` | completeness |
| `ON_PRIMARY_FIXED_VARIANT` | `onPrimaryFixedVariant` | completeness |
| `SECONDARY_FIXED` | `secondaryFixed` | completeness |
| `SECONDARY_FIXED_DIM` | `secondaryFixedDim` | completeness |
| `ON_SECONDARY_FIXED` | `onSecondaryFixed` | completeness |
| `ON_SECONDARY_FIXED_VARIANT` | `onSecondaryFixedVariant` | completeness |
| `TERTIARY_FIXED` | `tertiaryFixed` | completeness |
| `TERTIARY_FIXED_DIM` | `tertiaryFixedDim` | completeness |
| `ON_TERTIARY_FIXED` | `onTertiaryFixed` | completeness |
| `ON_TERTIARY_FIXED_VARIANT` | `onTertiaryFixedVariant` | completeness |

Total: 12 + 4 + 11 + 2 + 3 + 3 + 2 + 12 = **49**.

### 1.2 M3 baseline reference values

The M3 *baseline* scheme — the default the M3 theme builder produces before a custom source color is chosen. Ships as `MaterialPalettes.baseline()`.

> **Verification note:** the hex values below are the M3 baseline scheme recorded for reference. Before they become `MaterialPalettes.baseline()` in code, transcribe them from a current M3 theme builder export — that export, not this table, is the authoritative source. The locked decision here is the **role set** (§1.1); these values are reference data pending generator verification.

**Mode-varying roles (37)**

| Role | Light | Dark |
|---|---|---|
| `primary` | `#6750A4` | `#D0BCFF` |
| `onPrimary` | `#FFFFFF` | `#381E72` |
| `primaryContainer` | `#EADDFF` | `#4F378B` |
| `onPrimaryContainer` | `#4F378B` | `#EADDFF` |
| `secondary` | `#625B71` | `#CCC2DC` |
| `onSecondary` | `#FFFFFF` | `#332D41` |
| `secondaryContainer` | `#E8DEF8` | `#4A4458` |
| `onSecondaryContainer` | `#4A4458` | `#E8DEF8` |
| `tertiary` | `#7D5260` | `#EFB8C8` |
| `onTertiary` | `#FFFFFF` | `#492532` |
| `tertiaryContainer` | `#FFD8E4` | `#633B48` |
| `onTertiaryContainer` | `#633B48` | `#FFD8E4` |
| `error` | `#B3261E` | `#F2B8B5` |
| `onError` | `#FFFFFF` | `#601410` |
| `errorContainer` | `#F9DEDC` | `#8C1D18` |
| `onErrorContainer` | `#8C1D18` | `#F9DEDC` |
| `surface` | `#FEF7FF` | `#141218` |
| `onSurface` | `#1D1B20` | `#E6E0E9` |
| `surfaceVariant` | `#E7E0EC` | `#49454F` |
| `onSurfaceVariant` | `#49454F` | `#CAC4D0` |
| `surfaceDim` | `#DED8E1` | `#141218` |
| `surfaceBright` | `#FEF7FF` | `#3B383E` |
| `surfaceContainerLowest` | `#FFFFFF` | `#0F0D13` |
| `surfaceContainerLow` | `#F7F2FA` | `#1D1B20` |
| `surfaceContainer` | `#F3EDF7` | `#211F26` |
| `surfaceContainerHigh` | `#ECE6F0` | `#2B2930` |
| `surfaceContainerHighest` | `#E6E0E9` | `#36343B` |
| `outline` | `#79747E` | `#938F99` |
| `outlineVariant` | `#CAC4D0` | `#49454F` |
| `inverseSurface` | `#322F35` | `#E6E0E9` |
| `inverseOnSurface` | `#F5EFF7` | `#322F35` |
| `inversePrimary` | `#D0BCFF` | `#6750A4` |
| `shadow` | `#000000` | `#000000` |
| `scrim` | `#000000` | `#000000` |
| `surfaceTint` | `#6750A4` | `#D0BCFF` |
| `background` | `#FEF7FF` | `#141218` |
| `onBackground` | `#1D1B20` | `#E6E0E9` |

**Fixed roles (12)** — single value, identical in light and dark:

| Role | Value |
|---|---|
| `primaryFixed` | `#EADDFF` |
| `primaryFixedDim` | `#D0BCFF` |
| `onPrimaryFixed` | `#21005D` |
| `onPrimaryFixedVariant` | `#4F378B` |
| `secondaryFixed` | `#E8DEF8` |
| `secondaryFixedDim` | `#CCC2DC` |
| `onSecondaryFixed` | `#1D192B` |
| `onSecondaryFixedVariant` | `#4A4458` |
| `tertiaryFixed` | `#FFD8E4` |
| `tertiaryFixedDim` | `#EFB8C8` |
| `onTertiaryFixed` | `#31111D` |
| `onTertiaryFixedVariant` | `#633B48` |

### 1.3 What is and isn't deferred

**Nothing in the M3 color scheme is omitted.** All 49 standard roles are captured as vocabulary.

What *is* deferred is **behavior, not vocabulary**:

- **The elevation system** — *which* component maps to *which* `surfaceContainer*` tone, plus the tonal-lift logic — is deferred to v2 (§6, design-direction §6). The surface-container, `surfaceTint`, and `shadow` *tokens* are captured here; only their systematic *use* waits.
- Roles marked **completeness** in §1.1 are themable and resolvable today; they simply aren't consumed by a shipped v1 component yet. A consumer that needs one (e.g. an `inverseSurface` snackbar) can use it immediately — no reopening of this doc required.

This is the "trim components, not vocabulary" principle in practice — see the Amendment.

## 2. Type roles — 12 for v1

Each role resolves to a `java.awt.Font`. M3's full scale is 15 (5 sizes × display/headline/title/body/label); the `display` tier is dropped.

| Enum | Key | Size (pt) | Weight | FlatComp components that use it |
|---|---|---|---|---|
| `HEADLINE_LARGE` | `headlineLarge` | 32 | Regular | — (consumer pages) |
| `HEADLINE_MEDIUM` | `headlineMedium` | 28 | Regular | — (consumer pages) |
| `HEADLINE_SMALL` | `headlineSmall` | 24 | Regular | — (consumer pages) |
| `TITLE_LARGE` | `titleLarge` | 22 | Regular | `FlatCard` header (large) |
| `TITLE_MEDIUM` | `titleMedium` | 16 | Medium | `FlatCard` header (default) |
| `TITLE_SMALL` | `titleSmall` | 14 | Medium | `FlatCard` sub-header |
| `BODY_LARGE` | `bodyLarge` | 16 | Regular | `FlatCard` body |
| `BODY_MEDIUM` | `bodyMedium` | 14 | Regular | `FlatCard` body (compact), default |
| `BODY_SMALL` | `bodySmall` | 12 | Regular | `FlatCard` summary / metadata |
| `LABEL_LARGE` | `labelLarge` | 14 | Medium | `FlatChip` label (default) |
| `LABEL_MEDIUM` | `labelMedium` | 12 | Medium | `FlatChip` label (compact) |
| `LABEL_SMALL` | `labelSmall` | 11 | Medium | badge / caption text |

The headline tiers are kept even though no FlatComp component consumes them — the token layer is universal, and consumer pages need a headline scale to stay cohesive with the components.

### 2.1 Deferred / cut

| M3 | Decision | Rationale |
|---|---|---|
| `displayLarge` / `displayMedium` / `displaySmall` (3) | **Cut for v1** | 36–57pt display type is for marketing / splash surfaces a desktop tool rarely has. Trivial to add later. |
| Letter-spacing (tracking) per role | **Cut for v1** | Swing supports it via `TextAttribute.TRACKING` but it's fiddly and low-payoff. Capture family + weight + size only. |
| Line-height per role | **Cut for v1** | Swing has no clean line-height primitive (`JLabel` ignores it). Revisit only if a multi-line text component is built. |

### 2.2 Known rough edge — the Medium weight

M3 uses Regular (400) and Medium (500). `java.awt.Font` has only `PLAIN` / `BOLD` natively; true 500-weight needs either a font family with a Medium face or `TextAttribute.WEIGHT_MEDIUM` (honored only if the family supplies the glyphs). **Locked (Q3):** FlatComp bundles **Inter** as the default family via FlatLaf's preferred-family support — Inter ships a real Medium face, so the 400/500 distinction renders correctly. A consumer-supplied family without a Medium face falls back to `TextAttribute.WEIGHT_MEDIUM` (best-effort), then `BOLD`.

## 3. Shape scale — 7 steps

Corner radius by role. Lifted directly from M3; values are radius in px.

| Enum | Key | Radius (px) | Typical use |
|---|---|---|---|
| `NONE` | `none` | 0 | Square — full-bleed surfaces |
| `XS` | `xs` | 4 | Subtle rounding — inputs, small affordances |
| `SM` | `sm` | 8 | Chips (M3 spec value), small buttons |
| `MD` | `md` | 12 | **`FlatCard` default** |
| `LG` | `lg` | 16 | Large cards, sheets |
| `XL` | `xl` | 28 | Prominent containers, dialogs |
| `FULL` | `full` | 9999 | Pill / fully-rounded — capsule chips, FABs |

**Locked (Q1):** `FlatChip` default shape is `SM` (8px), matching the M3 chip spec. `FULL` (capsule) remains available as a per-instance shape override for consumers who want the old `FlatPill` look. The default deliberately moves chips off their capsule heritage onto the M3 baseline.

## 4. Spacing scale — 6 steps

4px base unit. Used for padding, gaps, insets. M3 has no formal spacing *token* scale (it uses an informal 8px/4px grid), so this is FlatComp's own ladder on that grid.

| Enum | Key | Value (px) | Typical use |
|---|---|---|---|
| `XS` | `xs` | 4 | Tight gaps — icon-to-label inside a chip |
| `SM` | `sm` | 8 | Default intra-component padding |
| `MD` | `md` | 12 | Chip padding, compact card padding |
| `LG` | `lg` | 16 | Default card padding, comfortable gaps |
| `XL` | `xl` | 24 | Section gaps within a panel |
| `XXL` | `xxl` | 32 | Major section separation |

**Locked (Q2):** no `XXXL`. The scale stops at `XXL` (32px); larger page-section gaps are the layout manager's job, not a token's. Cheap to add later if consumer pages prove it's needed.

## 5. State layers — 5 states

Hover / focus / pressed / dragged / selected expressed as **opacity overlays on a role color**, not as separate colors. This is the M3 model. See design-direction §8 for the FlatLaf impedance mismatch and the "compute-and-bake" resolution.

| Enum | Key | Opacity | Source |
|---|---|---|---|
| `HOVER` | `hover` | 8% | M3 |
| `FOCUS` | `focus` | 10% | M3 |
| `PRESSED` | `pressed` | 10% | M3 |
| `DRAGGED` | `dragged` | 16% | M3 |
| `SELECTED` | `selected` | 12% | **FlatComp-invented** — M3 models selection as a container-color swap, not an overlay |

**Locked (Q4):** the `SELECTED` overlay stays — a FlatComp-specific 12% overlay, applied uniformly. M3 models selection as a container-color swap (unselected chip → `surface`, selected → `secondaryContainer`); FlatComp instead uses one uniform overlay mechanism across chip/card lists to keep per-component selection logic minimal. Revisit if it reads wrong next to M3-styled components.

### 5.1 Disabled is not a state layer

M3 disabled is an *opacity treatment*, not an overlay: content drops to **38%** opacity, container fill to **12%**. Captured here for completeness but it lives outside the `StateLayer` enum — it's applied as a compositing pass, not a tinted overlay. Keys: `FlatComp.state.disabledContent` (0.38), `FlatComp.state.disabledContainer` (0.12).

## 6. Elevation — system deferred to v2

The elevation *system* is deferred to v2. The elevation *tokens* are not — they are part of the locked v1 role set (§1.1).

M3 expresses elevation as **tonal lift** — higher-elevation surfaces shift toward the primary tone via the `surfaceContainerLowest…Highest` ladder — rather than as drop shadows. The *tokens* for this (the surface-container roles, `surfaceTint`, `shadow`) are captured in §1.1. What's deferred is the *system*: deciding which component sits at which elevation tone, and the tonal-lift logic. **v1 ships flat:** components use `surface` / `surfaceVariant` and `outline` for differentiation, no shadow, no tonal ladder. Revisit once the v1 surface is proven.

## 7. Summary — the v1 token surface

| Category | Count | Enum |
|---|---|---|
| Color roles | 49 | `ColorRole` |
| Type roles | 12 | `TypeRole` |
| Shape steps | 7 | `ShapeScale` |
| Spacing steps | 6 | `SpaceScale` |
| State layers | 5 | `StateLayer` |
| **Total tokens** | **79** | + 2 disabled-opacity constants |

## 8. Resolved decisions

All six questions that gated the original lock, resolved 2026-05-14:

| # | Question | Locked decision |
|---|---|---|
| Q1 | `FlatChip` default shape — `SM` (M3 spec) vs `FULL` (pill heritage)? | **`SM`** default; `FULL` available as a per-instance override |
| Q2 | Add `SpaceScale.XXXL` (48px)? | **No** — scale stops at `XXL` (32px) |
| Q3 | Medium (500) font weight — bundle Inter vs synthetic semi-bold? | **Bundle Inter** via FlatLaf; best-effort fallback for consumer families without a Medium face |
| Q4 | `SELECTED` — keep invented 12% overlay vs M3 container-swap? | **Keep the 12% overlay** — uniform mechanism across chip/card lists |
| Q5 | Fold `background` into `surface`? | **Superseded** by the 2026-05-14 amendment — `background` / `onBackground` are captured roles. Usage guidance still prefers `surface`. |
| Q6 | `errorContainer` pair — needed for a v1 error variant? | **Superseded** by the 2026-05-14 amendment — `errorContainer` / `onErrorContainer` are captured roles. |

With the surface fixed, the next deliverable is the `FlatCompTheme` install API (design-direction §15 step 2), designed against these 79 tokens.
