# ElwhaFab — Design Decisions

**Status:** LOCKED for the Phase 1 build. This doc fixes the `ElwhaFab` API, size + color axes, content rules, state model, accessibility surface, and the Standard↔Extended morph spec for epic #160.

**Drafted:** 2026-05-25

**Author:** Charles Bryan (`cfb3@uw.edu`).

**Parents:**
- [`elwha-button-design.md`](elwha-button-design.md) — the reference primitive; FAB borrows the orthogonal-axis enum structure (`Size` / `Color`), the `AbstractButton` posture, and the §10 divergence-table format.
- [`elwha-button-anim-design.md`](elwha-button-anim-design.md) — locks the shared morph kit (`ShapeMorphPainter`, `MorphAnimator`, `Easing`) that this epic consumes for the Standard↔Extended morph. No new motion infra needed.
- [`elwha-design-direction.md`](elwha-design-direction.md) §9 — doctrine bar (raw Swing + tokens can't express).
- [`elwha-v1-component-scope.md`](elwha-v1-component-scope.md) — catalogs the FAB primitive.
- [M3 Floating Action Button (Expressive)](https://m3.material.io/components/floating-action-button/overview) — the Expressive variant of the spec, including the May 2025 update that added the Medium FAB / dropped Small-and-Surface / added the Extended FAB size axis aligned with Standard.

**Epic:** [#160](https://github.com/OWS-PFMS/elwha/issues/160) — `epic: M3 FAB component (soft spec)`.

**Related stub:** [#185](https://github.com/OWS-PFMS/elwha/issues/185) — `epic: M3 FAB Menu component (soft spec)`. Filed during this capture pass; not scheduled.

**Blocks:** Navigation Rail epic (#159) — Rail drives Standard↔Extended FAB transform via §8.

---

## TL;DR

1. **What it is:** a single `ElwhaFab` primitive that covers both **Standard** (icon-only) and **Extended** (icon + label) M3 Expressive FAB forms across three sizes (Small / Medium / Large) and six color styles (Primary / Secondary / Tertiary, each in both *tonal* and *container* tiers).
2. **Why one class, not two:** at any given size tier, Standard and Extended share container height, icon size, corner radius, color treatment, elevation, and states. Extended layers label-related tokens on top. M3 explicitly states the size axes align ("for easier transition between FABs") — making `size` a single contract and `standard`-vs-`extended` a content/form switch. The Nav Rail (#159) Standard↔Extended morph also requires one component to preserve identity.
3. **Posture:** tracks **M3 Expressive** post-May-2025 update — drops baseline Small FAB (40dp), Surface FAB, and the legacy Baseline Extended FAB. Adopts the new aligned-sizes vocabulary verbatim except for the smallest-tier enum value (see §5).
4. **Form enforcement:** per-variant static factories — `ElwhaFab.standard(Icon)`, `ElwhaFab.extended(String)`, `ElwhaFab.extended(Icon, String)`. The absence of `ElwhaFab.extended(Icon)` makes M3's "Don't have an icon without text" rule unrepresentable at the API level.
5. **Paint:** reuses `SurfacePainter` (round-rect + state-layer overlays + border) and `RipplePainter` (press ripple). The Standard↔Extended morph reuses `ShapeMorphPainter` + `MorphAnimator` from the shared theme kit (no new motion infra). No new theme tokens — color styles, shape tokens, elevation tokens already exist on the facade.
6. **Out of scope:** placement / positioning logic (consumer-driven), drag state (desktop-only library), FAB Menu transitions (deferred to #185), container-transform expansion patterns (motion infra well beyond Elwha's stance), enter/exit "appearing" animation (deferred), scroll-collapse utility (deferred).

---

## §0. Posture: M3 Expressive baseline, post-May-2025

Per [`elwha-design-direction.md`](elwha-design-direction.md) Elwha tracks M3 Expressive. The May 2025 FAB update is load-bearing for this design:

**Adopted from M3 Expressive (not present in baseline M3):**
- **Three-size axis** — Regular (56dp) / Medium (80dp) / Large (96dp) for Standard; Small (56dp) / Medium (80dp) / Large (96dp) for Extended. The two families share heights at every tier.
- **Six color styles** — three *tonal* (Primary / Secondary / Tertiary) and three *container* (Primary container / Secondary container / Tertiary container), each a `{container, on-container}` `ColorRole` pair. Container tier is the default. (Pre-May-2025 "Primary FAB" was renamed to "Primary Container FAB"; new "Primary FAB" is the tonal style — naming trap to watch.)
- **Aligned Extended size axis** — M3 May 2025 explicitly says: *"These align with the FAB sizes for an easier transition between FABs."* Drives the unified `Size` enum (§5 / §7).
- **Updated Extended typography** — Small Extended uses Roboto 500 with +0.15pt tracking; Medium and Large use Roboto 400 with 0 tracking. Maps onto Inter Medium + Inter Regular via `Typography.defaults()`.

**Dropped from M3 (not built; no deprecation shim, pre-1.0 per CLAUDE.md semver policy):**
- **Small FAB (40dp Standard)** — "no longer recommended" per May 2025.
- **Surface FAB / Surface Extended FAB** — "no longer recommended" per May 2025.
- **Baseline Extended FAB (single-size)** — replaced by Small Extended FAB; same 56dp value.
- **Lowered FAB variant** — not present on Variants page, Color styles legend, or token panel. Treated as deprecated alongside the above.

§10 is reserved for divergences from M3 Expressive, not from baseline M3.

---

## §1. Scope decisions — Elwha adaptation

- ✅ **Three sizes** — Small (56dp) / Medium (80dp) / Large (96dp), unified enum `Size`.
- ✅ **Six color styles** — three tonal + three container; default = Primary container.
- ✅ **Two forms via factory methods** — Standard (icon-only) and Extended (text required, icon optional).
- ✅ **Standard ↔ Extended morph** — bidirectional, via the shared `ShapeMorphPainter` + `MorphAnimator` kit.
- ❌ **Placement / positioning / FAB-menu / Speed-dial behaviors** — out of scope. `ElwhaFab` is the component; consumers position. Matches Elwha's library-not-framework stance.
- ❌ **Drag state** — desktop Swing library; no touch-reorder.
- ❌ **Enter/exit animation** — deferred to a future motion epic.
- ❌ **Scroll-collapse utility** (Extended → Standard on scroll, per M3 G14b / G33 / G34) — deferred; the morph API supports it once a scroll-source helper exists.
- ❌ **Container-transform expansion pattern** (M3 G13 / G32) — out of scope.

---

## §2. Component model — one component, not two

**Decision:** Single `ElwhaFab` class covers both Standard and Extended forms.

M3 documents them as two components, but at the same size tier they share:
- Container height (56 / 80 / 96 dp)
- Icon size (24 / 28 / 36 dp)
- Corner radius (16 / 20 / 28 dp)
- Color treatment (six identical color styles)
- Resting elevation + state-layer model
- Focus ring + ripple
- Accessibility surface (extends `AbstractButton`)

Extended adds *only*: a label, leading/trailing/icon-label padding, and per-size label typography. Standard ≈ Extended minus a label, at the same size.

**Rationale for one class:**
- Mirrors `ElwhaButton` (one class, label optional).
- Nav Rail (#159) drives a Standard↔Extended morph; one component → property/morph call; two components → instance swap with identity loss.
- M3's own May 2025 alignment statement signals this is the right grain.

---

## §3. Content rules

Source: M3 "Icon (optional)" Do/Don't card.

| Form | Icon | Text |
|---|---|---|
| **Standard FAB** | required | forbidden |
| **Extended FAB** | optional | required |

**Concretely:**
- Standard FAB: icon-only, always. No text mode.
- Extended FAB: text-only is valid (M3 Do); icon-only is not (M3 Don't — that's the Standard FAB).
- Extended FAB with icon + text is the common case.

**Enforcement** via per-variant static factories — invalid combinations are unrepresentable:

```java
ElwhaFab.standard(Icon icon)                    // Standard FAB — icon-only
ElwhaFab.extended(String text)                  // Extended FAB — text-only
ElwhaFab.extended(Icon icon, String text)       // Extended FAB — icon + text
// (no extended(Icon) factory — Standard FAB exists for that)
```

Null icon on Standard or null text on Extended produces `NullPointerException` at construction (fail-fast per Elwha convention).

---

## §4. Size axis (M3 Expressive, May 2025)

Both Standard and Extended families use the same height tiers. The Extended family also defines per-size label typography and internal padding.

| Tier (`Size` enum) | Container H | Icon | Corner radius | M3 Standard name | M3 Extended name |
|---|---|---|---|---|---|
| **SMALL** *(default)* | 56 dp | 24 dp | 16 dp | "FAB" (also called "Regular FAB") | "Small Extended FAB" |
| **MEDIUM** | 80 dp | 28 dp | 20 dp | "Medium FAB" | "Medium Extended FAB" |
| **LARGE** | 96 dp | 36 dp | 28 dp | "Large FAB" | "Large Extended FAB" |

**Naming note:** M3 calls the smallest Standard FAB "Regular FAB" and the smallest Extended FAB "Small Extended FAB" — same 56dp container, different label. Elwha unifies under `Size.SMALL` for cross-form symmetry; a Javadoc footnote on `Size` cross-references M3's mixed naming. The decision history (Appendix A) captures why this supersedes an earlier provisional `REGULAR` value.

### §4.1 Standard FAB layout

Icon is centered in the container. Effective inset (container edge → icon bounding box):

| Size | Container | Icon | Inset all around |
|---|---|---|---|
| Small | 56 dp | 24 dp | 16 dp |
| Medium | 80 dp | 28 dp | 26 dp |
| Large | 96 dp | 36 dp | 30 dp |

### §4.2 Extended FAB layout

Icon (optional) is leading; label is trailing; padding and gaps scale with size. M3 token names noted parenthetically.

| Size | Leading (`leading space`) | Icon→label gap (`icon label space`) | Trailing (`trailing space`) | Label typography (`label text`) |
|---|---|---|---|---|
| Small | 16 dp | 8 dp | 16 dp | Roboto **500** / 16 pt / **+0.15 pt** track / 24 pt LH |
| Medium | 26 dp | 12 dp | 26 dp | Roboto **400** / 22 pt / 0 / 28 pt LH |
| Large | 28 dp | 16 dp | 28 dp | Roboto **400** / 24 pt / 0 / 32 pt LH |

**Notable:** Small Extended uses a heavier weight (500 vs 400) and tracked letters for compact-form presence. Maps onto Elwha's bundled Inter Medium for Small; Inter Regular for Medium / Large.

**Font substitution:** M3 spec uses Roboto; Elwha uses Inter per `Typography.defaults()`. Mapping: Roboto 500 → Inter Medium, Roboto 400 → Inter Regular. Consumers may override via theme.

### §4.3 Extended FAB width

Width is **dynamic** (label-driven, content-sized) with a **minimum of 80 dp** (per M3 placement diagram annotation "Dynamic, min 80"). The component never imposes a max-width — implementation rule: no `JLabel.setMaximumSize(...)`, no text wrapping, no ellipsis. Label paints at intrinsic preferred width; container grows to fit.

### §4.4 Placement padding (out of scope, documented only)

M3 also specifies screen-edge margins (16 dp uniform) and per-size adjacent-UI gaps (20 dp for Medium, 28 dp for Large). All **out of scope** — consumers position their own FABs per Elwha's library-not-framework stance.

---

## §5. Color axis

Two color families, six total styles. Each style is a `{container, on-container}` `ColorRole` pair. Container drives background; on-container drives icon tint, label tint, and state-layer color.

| `Color` enum | Container role | On-container role | M3 name |
|---|---|---|---|
| **PRIMARY_CONTAINER** *(default)* | `primaryContainer` | `onPrimaryContainer` | Primary container & On primary container |
| SECONDARY_CONTAINER | `secondaryContainer` | `onSecondaryContainer` | Secondary container & On secondary container |
| TERTIARY_CONTAINER | `tertiaryContainer` | `onTertiaryContainer` | Tertiary container & On tertiary container |
| PRIMARY | `primary` | `onPrimary` | Primary & On primary |
| SECONDARY | `secondary` | `onSecondary` | Secondary & On secondary |
| TERTIARY | `tertiary` | `onTertiary` | Tertiary & On tertiary |

**No new theme tokens.** All six roles exist on the `ColorRole` facade today. Dark mode is automatic via the theme infrastructure; the same role names resolve to dark-scheme values.

**Color is independent of size and form.** The Extended FAB Color page and Color styles legend show the identical six styles with the identical Primary container default — one Color enum, one implementation path for both forms.

**Default = `PRIMARY_CONTAINER`** per the M3 Color styles legend (both Standard and Extended pages).

---

## §6. State model

Source: M3 FAB States page (intro + states diagram, light + dark) — identical for Standard and Extended.

| State | State layer α | Elevation | Extras |
|---|---|---|---|
| Enabled (resting) | — | FAB default (theme token) | — |
| Hovered | **8%** | **Level 4** (explicit M3 token) | — |
| Focused | **10%** | Resting | **visible focus ring outline** |
| Pressed | **10%** | Resting | **ripple** |

### §6.1 Elevation rule

M3 surfaces *only one* per-state elevation token for FAB: the hover bump to level 4. Resting, focused, and pressed all share the FAB's component-default elevation (typically M3 level 3 — defer to the theme's FAB elevation token). The visual "lower" appearance of non-hover states in the M3 states diagram is the *absence* of the hover bump, not a separate per-state token.

**Implementation:** one component-level elevation token (resolved through the theme) plus a hardcoded hover delta to level 4. No per-state elevation table needed.

### §6.2 Ripple

Pressed state shows an ink ripple. **Reuses the existing shared `RipplePainter`** (already in `theme/`). No new ripple infra.

### §6.3 Focus ring

Focused state has a visible outline around the container *in addition to* the 10% state layer. Light mode = dark outline; dark mode = light outline. Exact token/width verified against the existing focus-indicator pattern at implementation; reuses the `ElwhaButton` focus-ring approach where possible.

### §6.4 State layer color rule

Direct quote from M3 States intro (identical wording on Standard and Extended pages):

> "When using a non-default color mapping for FABs, make sure the state layer color is the same as the icon color."

**The first-sentence rule is load-bearing:** state layer color = icon color = on-container role for the active style.

| Style | State layer color (role) |
|---|---|
| Primary container *(default)* | `onPrimaryContainer` |
| Secondary container | `onSecondaryContainer` |
| Tertiary container | `onTertiaryContainer` |
| Primary | `onPrimary` |
| Secondary | `onSecondary` |
| Tertiary | `onTertiary` |

**M3 documentation imprecision flag:** the example sentence that follows ("For example, the state layer color for primary mapping should be md.sys.color.primary") appears verbatim on both Standard and Extended pages and is internally inconsistent with the first-sentence rule. Treat the first-sentence rule as authoritative; the example is M3 doc imprecision.

### §6.5 Drag state — out of scope

M3 baseline FAB had a Drag state for touch reordering. Out of scope for Elwha — desktop Swing library, no drag-to-reorder for FABs.

---

## §7. Anatomy

### §7.1 Standard FAB

1. **Container** — rounded-rect surface (color + shape + elevation).
2. **Icon** — centered glyph.

### §7.2 Extended FAB

1. **Container** — rounded-rect surface, wider than Standard, content-sized.
2. **Label text** — required.
3. **Icon** — optional.

**Note on M3 numbering:** M3's anatomy diagram numbers Label as part 2 and Icon as part 3 — reflecting priority in the Extended form (Label required, Icon optional), not visual placement. Visually the icon appears leading and label trailing.

---

## §8. API design (LOCKED)

```java
package com.owspfm.elwha.fab;

public final class ElwhaFab extends AbstractButton {

  /** Three sizes that scale across both Standard and Extended forms. */
  public enum Size { SMALL, MEDIUM, LARGE }

  /** Six color styles — three tonal + three container. */
  public enum Color {
    PRIMARY_CONTAINER,    // default — Primary container & On primary container
    SECONDARY_CONTAINER,
    TERTIARY_CONTAINER,
    PRIMARY,
    SECONDARY,
    TERTIARY
  }

  // Standard form — icon-only
  public static ElwhaFab standard(Icon icon)               { ... }

  // Extended form — text required, icon optional
  public static ElwhaFab extended(String text)             { ... }
  public static ElwhaFab extended(Icon icon, String text)  { ... }

  // Orthogonal axes
  public ElwhaFab setSize(Size size)   { ... }
  public ElwhaFab setColor(Color color){ ... }
  public Size getSize()                { ... }
  public Color getColor()              { ... }

  // Standard ↔ Extended morph (see §9)
  public void morphTo(/* form switch — see §9 */);
}
```

### §8.1 Default values

| Property | Default |
|---|---|
| `Size` | `SMALL` (56 dp) |
| `Color` | `PRIMARY_CONTAINER` |
| `Icon` | `null` for `extended(text)`; required for `standard(icon)` and `extended(icon, text)` |
| Tooltip | none — consumer-set via inherited `setToolTipText(...)` |

### §8.2 Convention adherence

Follows [`docs/development/component-api-conventions.md`](../development/component-api-conventions.md):
- Per-variant static factories (`standard` / `extended`).
- Single-axis fluent setters (`setSize` / `setColor` return `ElwhaFab`).
- Getter naming `getX()` only (no `getEffectiveX`).
- Extends `AbstractButton` (matches `ElwhaButton`, `ElwhaIconButton`) — Tab focus, Space/Enter activation, and `AccessibleRole.PUSH_BUTTON` come free.

### §8.3 Name-mapping footnote

The `Size.SMALL` value corresponds to what M3 calls "Regular FAB" on the Standard side and "Small Extended FAB" on the Extended side. Both are 56 dp containers; only the M3 documentation label differs. The Elwha API uses the May-2025 vocabulary (`SMALL` / `MEDIUM` / `LARGE`) consistently across both forms. Javadoc on `Size` calls out this mapping.

---

## §9. Standard ↔ Extended morph

The morph is bidirectional and driven by a single progress parameter `[0, 1]`. Used by Navigation Rail (#159) when its collapsed↔expanded state changes; also designed to support a future scroll-collapse utility (deferred per §1).

### §9.1 Animation breakdown (LOCKED)

Per M3's "FAB → Extended FAB" transition diagram, three transitions run in parallel:

| Step | Implementation |
|---|---|
| **Container width** | `ContentMorphPainter.containerWidth(standardW, extendedW, eased)` — drives both layout-time preferred size and paint-time body geometry that `ShapeMorphPainter` / `SurfacePainter` / `ShadowPainter` render into |
| **Container shape change** | `ShapeMorphPainter.interpolate(standardRadii, extendedRadii, progress, easing)` — circular ↔ rounded-rect (today both forms share the same per-size radius, so the visible "shape morph" is the container-width morph above plus the painter's `min(arc, min(w, h))` clamp keeping the ends well-formed) |
| **Icon translation** | `ContentMorphPainter.iconX(standardX, extendedX, eased)` — center (Standard) ↔ leading inset (Extended) positional interpolation |
| **Label opacity** | `ContentMorphPainter.labelAlpha(eased)` — `AlphaComposite` fade-in (forward) / fade-out (reverse) cross-faded across the 0.5 inflection: label invisible while the body expands through the first half, fades in across the second half (and vice versa in reverse) |

Reverse direction (Extended → Standard) inverts each step — `MorphAnimator.reverse()` runs progress backwards; all four transitions naturally play in reverse.

### §9.2 Motion kit reuse

Reuses the shared theme kit (no new infra):

- **`com.owspfm.elwha.theme.ShapeMorphPainter`** — stateless static `interpolate(from, to, progress, easing)` returning `CornerRadii`. Already designed for non-button consumers ("composing a shape morph with its own glyph paint" per its Javadoc).
- **`com.owspfm.elwha.theme.ContentMorphPainter`** — stateless static primitives for the content-side half of the morph: `iconX(from, to, progress)`, `labelAlpha(progress)` (with the 0.5 cross-fade inflection baked in plus an inflection-parameter overload for non-FAB consumers), and `containerWidth(from, to, progress)`. Parallels `ShapeMorphPainter` and is intended for future consumers (Navigation Rail expand / collapse) — primitives take an already-eased `progress` so a single `MorphAnimator` can drive multiple parallel transitions through one shared eased value.
- **`com.owspfm.elwha.theme.MorphAnimator`** — time-driven progress source. `new MorphAnimator(host, durationMs)` with `start()` / `reverse()` / `progress()`. Use `MorphAnimator.MEDIUM2_MS` (300 ms) for the form morph.
- **`com.owspfm.elwha.theme.Easing`** — pluggable curves.

### §9.3 API shape (provisional)

The exact `morphTo(...)` signature is settled during Phase 3 implementation. The motion contract above is the locked half; the API surface tracks how Nav Rail (#159) wants to drive it. Provisional shape:

```java
public void morphTo(Form target);   // Form = STANDARD | EXTENDED enum (internal)
```

with reciprocal forward/reverse motion managed by `MorphAnimator`. Final shape may extend to morph + size simultaneously if Nav Rail benefits.

---

## §10. Accessibility

### §10.1 Architectural choice — extend `AbstractButton`

Most of the M3 a11y contract lands for free:

| M3 requirement | Mechanism |
|---|---|
| Tab focus + Space/Enter activation | Inherited from `AbstractButton` |
| Role = Button | `AccessibleJButton` reports `AccessibleRole.PUSH_BUTTON` automatically |
| Tooltip-as-name fallback (Standard) | `AccessibleJComponent.getAccessibleName()` falls back to tooltip text when no accessible name is set |
| Visible label = accessible name (Extended) | `AbstractButton.getText()` is the default accessible name |

This matches `ElwhaButton` and `ElwhaIconButton`.

### §10.2 Use cases (per M3)

**Standard FAB:**
1. Navigate to and activate the FAB.
2. Perform an action with the FAB.
3. Expand and minimize an extended FAB (covered by §9 morph).

**Extended FAB:**
1. Navigate to and activate the extended FAB.

(Extended drops "perform action" — folded into activate — and "expand/minimize" — Extended is the destination form from the user's POV.)

### §10.3 Keyboard navigation

| Keys | Action |
|---|---|
| **Tab** | Focus lands on the FAB |
| **Space** or **Enter** | Perform the default action |

CODE — inherited from `AbstractButton`. No FAB-specific keyboard handling.

### §10.4 Labeling

**Standard FAB:** icon-only — accessible name is required. Two routes:
- `setToolTipText("Compose")` — doubles as M3 hover tooltip (G9b) *and* accessible-name fallback.
- `getAccessibleContext().setAccessibleName("Compose a new message")` — explicit override for a longer descriptive name.

Recommended: `setToolTipText(...)` for the simple case (one call covers both behaviors).

**Extended FAB:** label text is automatically the accessible name via `AbstractButton.getText()`. No additional code required. Tooltip not required for a11y.

**Same-first-word convention** (DOCS recommendation for consumer overrides): when overriding the accessible name to be more descriptive than the visible label, start with the same word. Example: visible "Create" → accessible "Create a new invite."

**Icon decorative for Extended:** per M3, the icon in an Extended FAB is "Decorative" — it carries no independent accessibility meaning (the label tells the story). Naturally satisfied because icons in `AbstractButton` are not separately focusable or announced.

### §10.5 Other a11y rules — all DOCS

| Rule | Disposition |
|---|---|
| Don't disable the FAB (hide instead) | DOCS — `setEnabled` stays inherited; M3 guidance in Javadoc |
| 3:1 icon-container contrast | DOCS — met by default via M3 `{container, on-container}` color pairs |
| Focus priority in tab order | DOCS — composition concern, consumer-managed |
| Tooltip on focus | DOCS — supported via standard Swing tooltip patterns |
| Layout placement (upper-left expanded, lower-right compact) | DOCS — placement out of scope per §1 |
| Don't obscure focus indicators of other elements | DOCS — composition concern |
| Treat label + icon as one focusable element | CODE-IMPLICIT — automatic from `AbstractButton` |

### §10.6 Drag a11y

Not applicable — drag state out of scope per §6.5.

---

## §11. RTL mirroring

**CODE rule** (M3 Extended FAB Right-to-left languages section): Extended FAB icon/label order swaps based on language direction.

| Orientation | Layout |
|---|---|
| LTR | Icon leading (left), label trailing (right) |
| RTL | Icon trailing (right), label leading (left) |

**Implementation rule:** internal layout queries `Component.getComponentOrientation().isLeftToRight()` to decide icon-label order. No hardcoded "icon left."

Standard FAB is icon-only and trivially symmetric — RTL is a no-op there.

---

## §12. Guidelines reference

35 M3 guidelines were captured during the spec pass. The full table lives in Appendix B. Summary:

| Category | Count | Disposition |
|---|---|---|
| CODE (locked or enforced) | 5 | §3 content rules; §6 states; §8 API; §9 morph; §11 RTL |
| CODE-IMPLICIT (falls out of design) | 6 | §4.3 dynamic width; §8 a11y inheritance; §10.4 label-as-name; etc. |
| DOCS (Javadoc / design-doc only) | 22 | Usage, placement, sizing, content semantics, etc. |
| OUT OF SCOPE | 2 | G13 / G32 container-transform pattern |

Most guidelines are placement, sibling-awareness, or composition-time decisions that a leaf component can't enforce.

---

## §13. Story breakdown (Phases 1–3)

Each story is a sub-issue of #160, added to Project #5. Each story produces a fresh demo class per the per-story-demo cadence; PRs land at phase boundaries, not per story.

### Phase 1 — Standard FAB

- **S1.** `ElwhaFab` skeleton + `Size`/`Color` enums + `standard(Icon)` factory + container rendering at all 3 sizes (default color). Demo: 3-size visual matrix.
- **S2.** Apply all 6 color styles across light/dark themes. Demo: 6 × 2 grid.
- **S3.** States — hover/focus/press state layers + hover elevation bump to level 4 + ripple via `RipplePainter` + focus ring. Demo: 4-state visual + interactive.

**Phase 1 PR:** Standard FAB visually complete in all states.

### Phase 2 — Extended FAB

- **S4.** `extended(String)` + `extended(Icon, String)` factories + label rendering (Inter Medium / Regular per size) + internal layout (leading / trailing / icon-label gap). Demo: 3-size variants with sample labels.
- **S5.** RTL mirroring + dynamic width + min 80 dp + no truncation/wrap. Demo: LTR/RTL toggle + long-label sizing.

**Phase 2 PR:** Extended FAB visually complete; states inherit from Phase 1.

### Phase 3 — Standard ↔ Extended morph

- **S6.** `morphTo(...)` API + bidirectional shape morph (via `ShapeMorphPainter`) + icon translation + label fade. All three animations as functions of one progress parameter `[0, 1]` per §9.1. Demo: button-driven Standard↔Extended toggle.
- **S7.** Bidirectional verification + cross-size morph + smoketest. (Light Nav Rail #159 integration demo if ready; standalone otherwise.) Demo: morph matrix.

**Phase 3 PR:** Morph complete, ready for Nav Rail (#159) consumption.

**Milestone:** v0.3.0 across all three phases.

---

## §14. Future work (deferred / out of scope)

Filed for posterity; not in this epic.

| Item | Disposition | Reference |
|---|---|---|
| Enter/exit "appearing" animation | DEFERRED | G12a / G31 |
| Scroll-collapse utility (`FabScrollCollapser`?) | DEFERRED | G14b / G33 / G34 — bidirectional morph already supports this once a scroll-source helper exists |
| Container-transform expansion pattern | OUT OF SCOPE | G13 / G32 — major motion infra well beyond Elwha's stance |
| `ElwhaFabMenu` | FUTURE EPIC | #185 — soft-spec stub filed; not scheduled |
| Window-class-driven auto-size switching | NOT IN SCOPE | Library-not-framework; consumer composition decision |
| Floating-toolbar coexistence rules | Cross-component | G26e — surface in eventual `ElwhaFloatingToolbar` Javadoc when filed |

---

## §15. Floating FAB composition (consumer recipe)

§4.4 punts placement out of scope: the primitive renders correctly wherever the parent layout places it; *making it float above content* is a Swing composition concern the lib doesn't bake into the FAB itself.

**Recommended: `ElwhaFabAnchor`.** As of [#205](https://github.com/OWS-PFMS/elwha/issues/205) Phase 1, the library ships a placement primitive that absorbs this glue. Wrap your content and FAB and drop the anchor in where a single component is expected:

```java
final ElwhaFab fab =
    ElwhaFab.extended(MaterialIcons.editFilled(ElwhaFab.Size.SMALL.iconPx()), "Compose");
frame.setContentPane(new ElwhaFabAnchor(content, fab));   // BOTTOM_TRAILING, 16 dp, RTL-aware
```

`ElwhaFabAnchor` owns z-order, corner placement (configurable `Corner` + inset), resize-pinning, and RTL — and pins the FAB's *visible body* to the spec margin (it backs out the shadow-halo reserve, which the raw recipe below does not). Scroll-aware hide/shrink (G14b / G33 / G34) is its Phase 2.

**Under the hood (manual recipe).** The recipe below is what the anchor does internally — documented for the case where you're floating a FAB on an *existing* layered pane (as the navigation rail does on the frame's own pane) rather than wrapping a content region. Every consumer of the manual path assembles the same four pieces:

1. **Z-order — `JLayeredPane`.** Every `JFrame` already has one (`frame.getLayeredPane()`). Add the FAB to `JLayeredPane.PALETTE_LAYER` (or higher); the content pane is on `FRAME_CONTENT_LAYER` and paints below. The FAB now floats above whatever the content does — scrolling, repaint, tab swap, anything.

2. **Position — bottom-trailing corner, 16 dp inset.** The layered pane has no layout manager, so the FAB needs explicit bounds. Set them from `getPreferredSize()` plus the M3 16 dp inset.

3. **Resize handling — `ComponentListener`.** When the frame resizes, the layered pane resizes too. Re-compute the FAB's bounds on every `componentResized` so the anchor stays glued to the corner.

4. **RTL — `getComponentOrientation()`.** Flip the X anchor when the orientation is right-to-left. The Y anchor doesn't change.

```java
final ElwhaFab fab =
    ElwhaFab.extended(MaterialIcons.editFilled(ElwhaFab.Size.SMALL.iconPx()), "Compose");

final JLayeredPane layeredPane = frame.getLayeredPane();
layeredPane.add(fab, JLayeredPane.PALETTE_LAYER);

final Runnable position = () -> {
  final Dimension pref = fab.getPreferredSize();
  final int inset = 16; // dp; M3 placement diagram
  final boolean ltr = layeredPane.getComponentOrientation().isLeftToRight();
  final int x = ltr
      ? layeredPane.getWidth() - pref.width - inset
      : inset;
  final int y = layeredPane.getHeight() - pref.height - inset;
  fab.setBounds(x, y, pref.width, pref.height);
};
layeredPane.addComponentListener(new ComponentAdapter() {
  @Override
  public void componentResized(final ComponentEvent e) {
    position.run();
  }
});
position.run();
```

**Where to put the listener.** On the *layered pane*, not the frame — the layered pane's bounds are what the FAB anchors to.

**Initial position.** The recipe above calls `position.run()` once at the end so the FAB is placed correctly before its first paint, not just after the first resize event. Without it, the FAB initially renders at `(0, 0)` and flickers into position on first resize.

**Scroll-aware behavior** (`G14b` / `G33` / `G34`). Not in this recipe, and not in `ElwhaFabAnchor` Phase 1. M3's hide-on-scroll-down and shrink-to-Standard-on-scroll-down patterns require a `JScrollPane` hook; they are `ElwhaFabAnchor`'s Phase 2 ([#205](https://github.com/OWS-PFMS/elwha/issues/205)).

**Working example.** The Elwha Showcase mounts its floating FAB via `ElwhaFabAnchor` — visible on every tab, clicks navigate the sidebar to the FAB Workbench entry. See `ElwhaShowcase.buildFloatingFabAnchor(JComponent)` for the live wiring.

---

## Appendix A — Decision history

Decisions captured during the spec pass on 2026-05-25. Earlier decisions superseded by later ones are listed with their replacement noted.

| Decision | Resolution |
|---|---|
| **One component vs two** | One `ElwhaFab` covers Standard + Extended. Drives §2. |
| **Content rules** | Standard: icon req, text forbidden. Extended: text req, icon optional. Enforced via §3 factories. |
| **Factory-method API** | Per-variant statics make invalid combinations unrepresentable. |
| **Small FAB + Surface FAB dropped** | M3 Expressive May 2025 deprecation + Elwha's no-baseline-deprecated stance. |
| **Lowered FAB dropped** | Not present on May 2025 Variants / Color / token panel. |
| **Baseline Extended FAB dropped** | Replaced by Small Extended FAB in May 2025. |
| **Size is the variant axis, color independent** | Per M3 Expressive framing. |
| **Six color styles, Primary container default** | Per M3 Color styles legend (both Standard and Extended). |
| **All six color roles exist on `ColorRole` facade** | No new theme tokens for FAB color. |
| **Size dp values** | Container 56 / 80 / 96, icon 24 / 28 / 36, corner 16 / 20 / 28. Captured from M3 token panel. |
| **State layer percentages** | Hover 8%, Focus 10%, Press 10%. |
| **Elevation per state** | Resting = FAB default; hover = level 4 (only state with explicit M3 token); focused/pressed = resting. |
| **Ripple in Pressed state** | Reuse shared `RipplePainter`. No new ripple infra. |
| **State layer color = on-container role** | Per first-sentence rule of M3 States intro. M3 example sentence flagged as doc imprecision. |
| **Drag state out of scope** | Desktop library. |
| **Placement padding out of scope** | Consumer positions. |
| **Extended FAB supports 3 sizes** | Small / Medium / Large; heights align with Standard at 56 / 80 / 96 dp. |
| **Extended FAB measurements per size** | Container H, icon, corner, leading, icon-label gap, trailing, label typography — all locked from M3 token panel. |
| **Extended FAB typography** | Small: Roboto 500 / 16pt / +0.15pt tracking; Medium: 400 / 22pt / 0; Large: 400 / 24pt / 0. Maps to Inter Medium + Inter Regular. |
| **Extended FAB width** | Dynamic, min 80 dp; no max-width, no truncation, no wrap. |
| **Extended FAB color identical to Standard** | Same 6 styles, same default, same role pairs. |
| **Extended FAB states identical to Standard** | Same 4 states, same elevation rule, ripple on press, focus ring. |
| **Standard ↔ Extended morph is bidirectional** | Nav Rail #159 + G14b both demand it. |
| **§9 morph animation steps locked from G35** | Shape change + icon translation + label fade. Three parallel transitions on one progress parameter. |
| **`AbstractButton` posture** | Inherits keyboard nav + a11y role + accessible-name fallback. |
| **`setToolTipText` doubles as a11y name for Standard** | One call covers both M3 hover tooltip and accessible-name fallback. |
| **Default size = SMALL (56 dp)** | Unified `Size { SMALL, MEDIUM, LARGE }` matches M3 May 2025 vocabulary. Supersedes an earlier provisional `Size.REGULAR` value (same 56 dp). |
| **`ElwhaFabMenu` filed as soft-spec stub** | Issue #185; not scheduled. |
| **RTL mirroring is a CODE rule** | Internal layout respects `Component.getComponentOrientation()`. Standard FAB unaffected (icon-only). |
| **No max-width constraint on Extended FAB label** | Implementation rule from G25. |

---

## Appendix B — M3 guideline reference table

Captured 2026-05-25. Each row maps an M3 guideline to its Elwha disposition.

| # | Source page | Verdict | Notes |
|---|---|---|---|
| G1 | Guidelines — size scales with window | DOCS | Composition decision; library-not-framework. |
| G2 | Usage philosophy (Standard) | DOCS | Semantic guidance; placement is out of scope. |
| G3 | Three sizes; Medium "most recommended" | DOCS | Recommendation lives in Javadoc + design doc. |
| G4 | Size selection per window class (Standard) | DOCS | Consumer composition. |
| G5 | Do / Don't (Standard) | DOCS | Semantic guidance. |
| G6a | Standard ↔ Extended transform | CODE | §9. |
| G6b | FAB → FAB Menu pattern | OUT OF SCOPE | Filed as #185. |
| G7 | Action semantics | DOCS | "Promotes important constructive actions." |
| G8 | Container guidelines | DOCS + CODE-IMPLICIT | Square container enforced via size dims. |
| G9 | Icon guidelines (filled icon, tooltip) | DOCS | Tooltip via inherited `setToolTipText`; fill axis is a future MaterialIcons enhancement. |
| G10 | Adaptive design | DOCS | Placement; Nav Rail Endorsed. |
| G11 | Within / outside nested contexts | DOCS | No on-card FABs (G11b) etc. |
| G12 | Behaviors — Appearing / Reappearance | DOCS / DEFERRED | G12a (enter animation) deferred. |
| G13 | Behaviors — Expanding (container transform) | OUT OF SCOPE | Motion infra beyond Elwha. |
| G14 | Behaviors — Scrolling | DOCS / DEFERRED | G14b (extended→standard on scroll) deferred. |
| G15 | Behaviors — Moving across tabs (Do) | DOCS | Visibility on tab change is router concern. |
| G16 | Behaviors — Moving across tabs (Don't) | DOCS | Paired with G15. |
| G17 | Extended FAB usage philosophy | DOCS | Long scrolling views, persistent access. |
| G18 | Extended FAB additional emphasis | DOCS | Reinforces §3 content rules. |
| G19 | One Extended FAB per screen | DOCS | Restates G5b / G11b for Extended. |
| G20 | Don't use Extended FAB as one of a set | DOCS | NEW — Extended-specific; reach for `ElwhaButton` Filled instead. |
| G21 | Choosing an Extended FAB size | DOCS | Slight nuance — Extended Large fits compact windows. |
| G22 | Container hugs contents (dynamic width) | CODE-IMPLICIT | §4.3. |
| G23 | Icon optional; text required (Extended) | CODE-ENFORCED | §3 factories. |
| G24 | Label clarity / 1–2 words / localization | DOCS | Consumer semantic decision. |
| G25 | Don't wrap or truncate label | CODE-IMPLICIT | No `setMaximumSize` on label. |
| G26 | Extended FAB placement | DOCS | Out of scope per §1. G26e cross-references future `ElwhaFloatingToolbar`. |
| G27 | Standard ↔ Extended responsive morph | CODE | Reinforces §9 with Nav Rail use case. |
| G28 | RTL language mirroring | CODE | §11. |
| G29 | Window-size placement (compact / medium) | DOCS | Out of scope. |
| G30 | Window-size placement (expanded) | DOCS | Restates G10b; Nav Rail integration. |
| G31 | Extended FAB Appearing animation | DEFERRED | Mirrors G12a. |
| G32 | Extended FAB Expanding (container transform) | OUT OF SCOPE | Mirrors G13. |
| G33 | Extended → FAB on scroll (Transforming) | DEFERRED | Mirrors G14b. |
| G34 | Extended ↔ FAB on scroll (Scrolling, bidirectional) | DEFERRED | Reinforces bidirectional requirement in §9. |
| G35 | FAB → Extended FAB transition animation steps | CODE | §9.1 motion spec — shape + icon + label parallel transitions. |
