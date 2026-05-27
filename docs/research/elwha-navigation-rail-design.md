# ElwhaNavigationRail — Design Decisions

**Status:** DRAFT for epic [#159](https://github.com/OWS-PFMS/elwha/issues/159), milestone v0.3.0. Sections marked **[OPEN]** are decisions to settle before phase breakdown. Sections marked **[LOCKED]** are not expected to move during implementation. Seeded from `elwha-navigation-rail-research.md` (25-screenshot M3 capture). Mirrors the structure of `elwha-fab-design.md` and `elwha-badge-design.md`.

## TL;DR

Two new public types compose the rail:

- **`ElwhaNavigationRail`** — the container. Two variants (`COMPACT` / `EXPANDED`), optional menu toggle, optional anchored `ElwhaFab`, primary destinations always, expanded-only secondary destinations grouped under section headers, animated `morphTo(Variant)` between variants.
- **`ElwhaNavRailDestination`** (working name; final name §8) — the rail slot. Dedicated component, **not** an extended mode on `ElwhaIconButton` / `ElwhaButton`. Composes shared painters (`SurfacePainter`, `RipplePainter`, `ShapeMorphPainter`, `ContentMorphPainter`, `MorphAnimator`) plus `ElwhaBadge` via `ElwhaBadgeAnchor`. Handles its own Compact↔Expanded morph (icon-over-label-stacked ↔ icon-beside-label-inline-in-pill) driven by its parent rail's variant.

Single-mandatory selection lives on the container, conceptually parallel to chip-list `SINGLE_MANDATORY` (pattern reuse, not code reuse — the rail is not an `ElwhaList<T>`).

Hard dependencies, both shipped: `ElwhaFab` ([#160](https://github.com/OWS-PFMS/elwha/issues/160) ✅), `ElwhaBadge` ([#209](https://github.com/OWS-PFMS/elwha/issues/209) ✅). Foundation prereq open: `ContentMorphPainter` extraction ([#223](https://github.com/OWS-PFMS/elwha/issues/223)). Small prereq open: `MaterialIcons` fill-0→fill-1 axis (to be filed as a Phase 1 story).

---

## §0. Posture: M3 Expressive baseline, post-May-2025

Same posture as the FAB and Badge designs. The M3 Expressive update deprecated the baseline navigation rail; only the Expressive `Collapsed` and `Expanded` (renamed `Compact` / `Expanded` here, §1) variants are built. No baseline fallback, no deprecation shim — pre-1.0, no compat layers.

Follows [`elwha-design-direction.md`](elwha-design-direction.md) §9 (raw Swing + tokens can't express this) and the `m3-morph-is-multi-component` doctrine — the Compact↔Expanded transition's primitives come from the shared morph kit.

## §1. Scope decisions — Elwha adaptation of the M3 spec

Out of scope (from `elwha-navigation-rail-research.md` §1):

- Baseline rail variant.
- Phone / compact window size classes.
- Modal expanded layout. Expanded is always non-modal ("Standard" in M3 terms).
- Breakpoint-driven auto-switching between variants — the consumer flips the variant explicitly.
- Page-content transitions — Elwha fires the selection event; the consumer swaps content.

In scope: `Compact` ↔ `Expanded` variants and the animated transition between them. The header chrome (menu toggle, FAB) is hosted, not owned — the rail accepts an `ElwhaIconButton` for the menu and an `ElwhaFab` for the action slot.

**Naming note (M3 → Elwha):** the M3 spec uses "Collapsed" / "Expanded." Elwha uses **Compact** / **Expanded** to align with M3 Expressive component-size terminology elsewhere in the lib (Card, Button group). The destination's two layouts use the same Compact / Expanded names. **[OPEN]** if reviewer prefers staying with M3's "Collapsed" naming.

## §2. Component model — two components, one epic

The rail is two cooperating components — split for the reasons enumerated below — not one monolithic class.

| Component | Owns |
|---|---|
| `ElwhaNavigationRail` | container chrome, variant state, destination list, section grouping, selection model, morph orchestration (drives every destination's morph in lock-step) |
| `ElwhaNavRailDestination` | one slot's icon + label + active indicator + badge anchor, the Compact↔Expanded layout transition, hit target, focus + ripple |

**Why split:**

1. The destination has its own paint contract that differs from the container's (its body paints the active-indicator pill, ripple, state layer; the container paints the rail surface + optional divider). Composing them as separate `JComponent`s lets each own its paint cleanly without conditionals.
2. The destination's API (label, icon, selected, badge) maps to a per-slot consumer call. The container's API (variant, menu, fab, primary[], sections[]) maps to a global call. One class with both surfaces would conflate.
3. Section grouping introduces a non-flat structure in expanded mode (header → secondary destinations). A list of `ElwhaNavRailDestination` instances + section-header markers is easier to reason about than per-slot conditionals.
4. The destination is the rail's "rail button" — naming it gives the consumer a clear handle for keyboard navigation, focus traversal, and testing.

**Why NOT extend `ElwhaIconButton` / `ElwhaButton` for the destination:** decided 2026-05-27. The destination has (a) stacked-vs-inline label layout, (b) selection-pill scope that changes between modes (icon-only in Compact, full-row in Expanded), (c) icon-anchored badges, (d) single-mandatory selection driven by a parent container, and (e) a per-mode hit-target invariant (full rail width regardless of indicator shape). None of those fit a general-purpose button; adding them as modes would bloat Button / IconButton for one consumer. Shared scaffolding lives at the painter/theme layer instead.

## §3. Content rules

Per-destination:

- **Label text** — required. One word ideal. Never truncated, ellipsized, or shrunk (M3 G21). Wraps to ≤ 2 lines.
- **Icon** — required; pair of glyphs for the fill axis (fill-0 unselected, fill-1 selected). **[OPEN]** API shape: two-icon param (`destination(Icon unsel, Icon sel, String label)`) vs one-glyph + `MaterialIcons` fill axis. Leaning fill axis so other icon-bearing components benefit.
- **Badge** — optional. Either `ElwhaBadge.small()` (dot, no content) or `ElwhaBadge.large(...)` (numeric or capped-count). Attached via `ElwhaBadgeAnchor.attach(this, badge)`; the destination implements `IconBearing` for the anchor's positioning.
- **Selected** — destination doesn't carry its own selected boolean publicly; the container's selection model is authoritative. The destination paints from a state set by the rail.

Per-container:

- **Primary destinations** — 3–7 (M3-recommended range; not enforced, but a `Logger.warning` is filed if outside).
- **Sections** — zero or more. Each section is a header label + a list of secondary destinations. Sections are shown only when `variant == EXPANDED`.
- **Menu button slot** — optional `ElwhaIconButton`; if absent, the rail is fixed-state (consumer must drive variant changes via API).
- **Anchored action slot** — optional `ElwhaFab`. If present, the rail orchestrates the FAB's Standard↔Extended form to track its own Compact↔Expanded variant.

## §4. Variant axis (M3 Expressive)

| Aspect | Compact | Expanded |
|---|---|---|
| Container width | 96dp fixed | 220–360dp configurable |
| Destination layout | icon-over-label stacked | icon-beside-label inline |
| Active indicator | pill on icon | horizontal pill (`Hug` default / `Fill` **[OPEN]**) |
| Anchored FAB | Standard (icon only) | Extended (icon + text) |
| Destinations shown | primary only | primary + sectioned secondary |
| Badge placement | upper-right of icon | beside the label **[OPEN]** — verify against #209 anchor's current capability |
| Menu icon | ☰ ("expand") | collapse glyph ("collapse") |

Destination height in Expanded: **~56dp**. Compact destination height **[OPEN]** — verify from M3 specs tab.

## §5. Color axis

Color roles from the theme `ColorRole` facade:

| Part | Role |
|---|---|
| Container | `SurfaceContainer` (optional fill) |
| Active item icon | `OnSecondaryContainer` |
| Active indicator | `SecondaryContainer` |
| Active item label | `Secondary` (Compact) · `OnSecondaryContainer` (Expanded) |
| Inactive item icon | `OnSurfaceVariant` |
| Inactive item label | `OnSurfaceVariant` |
| Large badge (delegated) | owned by `ElwhaBadge` |
| Small badge (delegated) | owned by `ElwhaBadge` |
| Divider | `OutlineVariant` |

Active-label color is variant-dependent — see §11 footnote on why. Pre-flight check: confirm `ColorRole` exposes every role above. (Spot-check expected to pass; flag if not.)

## §6. State model

Per-destination states: `Enabled` (default), `Hovered`, `Focused`, `Pressed`, `Disabled`. Mapped via `theme/StateLayer`. Selected is orthogonal — a destination can be `Hovered + Selected` simultaneously.

**Invariant (LOCKED):** the destination hit target spans the **full rail width** in both Compact and Expanded, regardless of the active-indicator pill's shape (icon-only pill in Compact still gets a row-wide hit). Reconfirmed across three M3 screenshots.

Ripple originates from the click point and uses `RipplePainter`. State layer overlay sits on the active-indicator pill *and* the hit area — **[OPEN]** verify M3 reference for hover-over-unselected behavior (is the overlay drawn under the indicator shape or under the full row?).

Focus ring: standard Elwha focus treatment (matches Button / Chip). No M3-specific deviation expected.

## §7. Anatomy

### §7.1 Compact destination

```
+----------------------+   ← full-width hit target
|        +----+        |
|        | ic |        |   ← icon, optional badge anchored UR
|        +----+        |
|        Label         |   ← label below icon
+----------------------+

(selected: pill behind icon only; label sits unstyled below)
```

### §7.2 Expanded destination

```
+--------------------------------------+   ← full-width hit target
|   +----+                             |
|   | ic |  Label              [+3]    |   ← inline, badge optional
|   +----+                             |
+--------------------------------------+

(selected: pill wraps the entire row)
```

### §7.3 Container

```
+--------+               +-----------------+
| ☰      |               | ☰<              |
|        |               |                 |
| +-+    |               | +--------+      |
| |F|    |     ===>      | |  F Label |    |   ← FAB orchestrated
| +-+    |   morphTo     | +--------+      |
|        |               |                 |
| +-+    |               | +-----------+   |
| |i|    |               | | i Inbox  |◀── selected (full-row pill)
| Inbx   |               | +-----------+   |
|        |               |                 |
| +-+    |               | i Outbox  [3]   |
| |i|[3] |               |                 |
| Outbx  |               | ─── Section ──  |
|        |               |                 |
| ...    |               | i Secondary     |
+--------+               +-----------------+
   COMPACT                    EXPANDED
   96dp                       220–360dp
```

## §8. API design [DRAFT — settle in Phase 1]

Sketch only; concrete signatures land during Phase 1 implementation review.

**`ElwhaNavigationRail`** *(naming [OPEN]: `ElwhaNavigationRail` vs `ElwhaNavRail`)*

```java
public final class ElwhaNavigationRail extends JComponent {

  // Construction
  public static ElwhaNavigationRail compact();
  public static ElwhaNavigationRail expanded();

  // Variant + morph
  public Variant getVariant();
  public void setVariant(Variant);             // snap, no animation
  public void morphTo(Variant);                // animated
  public boolean isMorphing();

  // Header chrome (both optional)
  public void setMenuButton(ElwhaIconButton);  // null = no menu
  public void setFab(ElwhaFab);                // null = no FAB; the rail orchestrates Form

  // Destinations
  public void setPrimary(List<ElwhaNavRailDestination>);
  public void addSection(String header, List<ElwhaNavRailDestination>);
  public void clearSections();

  // Selection (single-mandatory)
  public ElwhaNavRailDestination getSelected();
  public void setSelected(ElwhaNavRailDestination);
  public void addSelectionListener(NavRailSelectionListener);

  // Configuration
  public void setExpandedWidth(int px);        // clamped [220, 360]
  public void setDivider(boolean);
  public void setElevation(int level);         // 0 or 1
}

public enum Variant { COMPACT, EXPANDED }
```

**`ElwhaNavRailDestination`** *(naming [OPEN]: `ElwhaNavRailDestination` vs `ElwhaNavRailButton`)*

```java
public final class ElwhaNavRailDestination extends JComponent implements IconBearing {

  // Construction — see §3 [OPEN] on icon API
  public static ElwhaNavRailDestination of(Icon unselected, Icon selected, String label);

  // Identity
  public String getLabel();
  public Icon getIconUnselected();
  public Icon getIconSelected();

  // Badge (optional)
  public void setBadge(ElwhaBadge);   // attaches via ElwhaBadgeAnchor
  public ElwhaBadge getBadge();

  // IconBearing — for ElwhaBadgeAnchor positioning
  @Override public Rectangle getIconBounds();

  // The variant + selected state are pushed from the parent rail; not part of the
  // destination's public surface to keep the container the single source of truth.
}
```

### §8.1 Convention adherence

Follows `component-api-conventions.md` and `code-style.md`:

- per-variant static factories (`compact()` / `expanded()` for the rail);
- `getX()` only, no `getEffectiveX()`;
- single-arg convenience constructors where the M3 default makes sense;
- Javadoc `@author` / `@version` / `@since` on every public class + method, bumped each touch (verified by `validate-versions`).

## §9. Compact ↔ Expanded morph

The morph is the rail's central animation. It runs in two coordinated tiers:

### §9.1 Container morph

`ElwhaNavigationRail.morphTo(Variant)` drives a `MorphAnimator` (duration **[OPEN]** — likely `MEDIUM2_MS` 300 ms matching FAB, possibly `MEDIUM3_MS` 350 ms for the wider distance). The container animates its preferred width from 96dp to `expandedWidthPx` (or in reverse).

When the rail hosts an `ElwhaFab`, the container `morphTo` simultaneously calls `fab.morphTo(EXTENDED | STANDARD)` so the FAB's Standard↔Extended choreography stays phase-locked with the rail's variant change. The FAB morph contract from `elwha-fab-design.md` §9 covers the FAB's own internals.

### §9.2 Per-destination morph (LOCKED motion contract)

Every destination runs in lock-step with the container's progress (it doesn't own a `MorphAnimator` — the container's progress is pushed in). Each frame composes:

| Transition | Implementation |
|---|---|
| Container width | `ContentMorphPainter.containerWidth(compactW, expandedW, progress, easing)` |
| Active-indicator shape | `ShapeMorphPainter.interpolate(compactRadii, expandedRadii, progress, easing)` — circular icon pill ↔ horizontal row pill |
| Icon X translation | `ContentMorphPainter.iconX(centerX, leadingX, progress, easing)` — centered ↔ leading inset |
| Label position | **stacked-below ↔ inline-beside** — composed on top of `ContentMorphPainter` primitives, lives in the destination's paint method (rail-specific, see §9.3) |
| Label alpha | `ContentMorphPainter.labelAlpha(progress, inflection)` — fade across the position transition |
| Label color | `ColorRole` interpolation — `OnSurfaceVariant` ↔ `OnSecondaryContainer` if selected (Expanded label moves inside the pill) |
| Badge position | delegated to `ElwhaBadgeAnchor` — the anchor reads the host's `getIconBounds()` each frame and tracks the icon translation automatically |

### §9.3 The stacked↔inline relocation

This is the "superset" piece — the choreography FAB doesn't have. In Compact, the label is rendered **below** the icon, horizontally centered. In Expanded, the label is rendered **beside** the icon, on a horizontal pill. The relocation interpolates:

- Label X: `iconBottomCenterX` → `iconRightEdgeX + leadingGap`
- Label Y: `iconBottomY + verticalGap` → `iconCenterY` (vertically centered with icon)
- Label baseline: vertically-aligned-with-icon-bottom in Compact ↔ vertically-aligned-with-icon-center in Expanded

**Implementation choice [OPEN]:** inline this math in `ElwhaNavRailDestination.paintComponent`, or extend `ContentMorphPainter` with a `labelOrigin(...)` primitive. Leaning inline for now (rail-specific math; not yet a multi-consumer pattern); extract later if a third consumer needs it.

### §9.4 Motion kit reuse

| Helper | Use |
|---|---|
| `MorphAnimator` (container-owned) | Drives every destination + the FAB in lock-step |
| `Easing` | `easeInOutCubic` default; **[OPEN]** confirm against M3 motion tokens |
| `ShapeMorphPainter` | Active-indicator shape morph (existing helper) |
| `ContentMorphPainter` | Container width + icon X + label alpha primitives (NEW — [#223](https://github.com/OWS-PFMS/elwha/issues/223)) |
| `SurfacePainter`, `RipplePainter`, `StateLayer` | Per-frame surface paint, ripple, hover/press overlays |
| `ElwhaBadgeAnchor` | Badge position tracks the icon glyph automatically (existing) |

No new motion infra beyond #223.

## §10. Accessibility

### §10.1 Architectural choice

The container is a `JComponent` with `AccessibleRole.PAGE_TAB_LIST` (or `LIST` — **[OPEN]**, evaluate which JAWS/NVDA narrate more usefully for rails). The destination is a `JComponent` with `AccessibleRole.PAGE_TAB` (or `RADIO_BUTTON` to encode the single-mandatory exclusivity — **[OPEN]**).

Rationale: extending `AbstractButton` for the destination was tempting (free Space/Enter + tab focus) but conflicts with the rail's container-driven selection model (each destination's `selected` is downstream of the container, not click-toggle-owned). A plain `JComponent` with explicit focus + keybinding wiring is cleaner; ripple-on-press is per-component anyway.

### §10.2 Keyboard navigation

| Key | Behavior |
|---|---|
| `Tab` | Container focus → menu button → FAB → first destination → next focusable outside the rail |
| `Shift+Tab` | Reverse |
| `↑ / ↓` | Move focus between destinations within the rail (focus, not selection) |
| `Space / Enter` | Select the focused destination |
| `Home / End` | First / last destination |
| `Escape` | If Expanded with menu, collapse (rail consumer can suppress) — **[OPEN]** |

Selection moves only on explicit activation (Space / Enter / click), not on focus traversal. Matches `aria-activedescendant`-style tab lists.

### §10.3 Labeling

- **Container** — accessible name from `setAccessibleName(...)` (consumer-supplied, e.g., "Primary navigation"). Default is empty; flag a `Logger.warning` if unset at first paint.
- **Destination** — accessible name = `label`. The badge appends a count fragment via `ElwhaBadgeAnchor`'s push-model name splicing (existing #209 behavior).
- **Selected announcement** — `AccessibleSelection`-based; the container fires a selection event the screen reader picks up.

### §10.4 Other a11y rules — DOCS

| M3 requirement | Mechanism |
|---|---|
| 4.5:1 contrast on all labels | Token system enforces; verify on every Material palette before release |
| 24×24px min tap target | Compact destination is row-width × ~56dp; passes |
| Reduced-motion preference | `MorphAnimator` already respects the OS reduced-motion hint (see [[m3-morph-is-multi-component]]) |

## §11. RTL mirroring

The whole rail mirrors under `ComponentOrientation.RIGHT_TO_LEFT`: container docks to the right of the content area, destinations lay out icon-on-right + label-on-left in Expanded, active-indicator pill mirrors, badge position mirrors (via `ElwhaBadgeAnchor` RTL support from #209).

Active-label color stays variant-dependent: in Compact the label is *below* the indicator (so `Secondary` for contrast against the rail surface); in Expanded the label sits *inside* the `SecondaryContainer` pill (so `OnSecondaryContainer` for contrast against the pill). The orientation of the label relative to the pill is the deciding factor, not LTR/RTL.

## §12. Guidelines reference

- M3 Navigation rail — overview / specs / guidelines tabs (capture 2026-05-20/21).
- `elwha-navigation-rail-research.md` — the 25-screenshot M3 capture this design is seeded from.
- `elwha-fab-design.md` §9 — the morph kit the rail composes with.
- `elwha-badge-design.md` §5 / §9 / §10 / §11 — the badge anchor contract the destination depends on.

## §13. Story breakdown (Phases 1–4)

Story numbers TBD; filed under epic #159 once this design doc is reviewed.

### Phase 1 — `ElwhaNavRailDestination` (Compact form)

1. `MaterialIcons` fill-0→fill-1 axis story — adds an optional `Icon iconSelected(...)` resolver or a `MaterialIcons.filled(...)` overload (decision in story). Selected glyphs needed for the destination.
2. `ElwhaNavRailDestination` skeleton — class shell, factories, `IconBearing` impl, Compact layout (icon-over-label), state layer, ripple, focus, basic paint. No selected state, no badge yet.
3. Destination selected state in Compact — active-indicator pill around the icon, color shift, fill-0→fill-1 swap. No animation yet.
4. Destination badge slot — `setBadge` + `ElwhaBadgeAnchor.attach(this, badge)` integration. Verify badge tracks icon position via `IconBearing`.
5. Destination playground + Showcase Gallery panel — visual smoke-test artifacts (per `fresh-demo-per-story`).

### Phase 2 — `ElwhaNavigationRail` (Compact only)

6. Rail container skeleton — `compact()` factory, surface paint, divider, elevation, header chrome slots (menu button, FAB).
7. Primary destinations + single-mandatory selection model — container holds the list, drives `selected` push to each destination, fires selection events.
8. Keyboard navigation (per §10.2) — Tab in/out, ↑/↓ within, Space/Enter to select.
9. Rail playground + Showcase Workbench entry — interactive demo for a static Compact rail.

### Phase 3 — Expanded variant + Compact↔Expanded morph

10. Expanded layout: destination inline form (icon-beside-label, full-row pill) — static, no morph yet.
11. Expanded layout: container width range + section headers + secondary destinations — static.
12. `ContentMorphPainter` consumer wiring — destination composes the primitives for the icon-X + label-alpha + width transitions.
13. Stacked↔inline label-relocation morph (§9.3) — the rail-specific math layered on top of `ContentMorphPainter`.
14. `ElwhaNavigationRail.morphTo(Variant)` — orchestrates every destination + the FAB in lock-step.
15. Expanded keyboard navigation + secondary-destination focus traversal.

### Phase 4 — Showcase integration + placement

16. Showcase Workbench: Navigation Rail entry, variant toggle, all-knobs configuration.
17. Showcase Gallery panel: side-by-side Compact + Expanded reference, selected/unselected/badge variants.
18. Real placement on the Showcase frame (replaces the temporary sidebar) — analogous to the FAB Phase 5 floating-FAB placement on the layered pane.

### Phase 5 — Polish

19. Active-indicator grow-from-center animation on selection (deferred from §10 if Phase 3 ships static-indicator-swap).
20. Reduced-motion fallback verification.
21. CHANGELOG `[Unreleased]` entry curation + `@version` audit.

**[OPEN]** is Phase 5 phased separately or folded into Phase 4 — depends on whether the indicator-grow animation lands in Phase 3 implementation.

## §14. Out of scope (LOCKED)

- Baseline (non-Expressive) navigation rail.
- Modal expanded layout.
- Breakpoint-driven auto-switching between variants.
- Page-content transitions ("top level transition pattern").
- Phone / compact window size classes.
- Multi-select destinations.
- Drag-reorder of destinations.
- Custom destination layouts (icon-only, label-only, icon+label-stacked horizontally) — the two M3 layouts are the contract.

## §15. Open decisions, indexed

The DRAFT decisions to settle before Phase 1 starts (cross-references back to where each is flagged):

1. §1 — "Compact" vs M3's "Collapsed" naming.
2. §3 — Icon API: two-icon param vs `MaterialIcons` fill axis.
3. §4 — Active-indicator `Fill` mode in Expanded: include or skip.
4. §4 — Compact destination height (verify against M3 Specs tab).
5. §6 — State-layer overlay shape on hover (under-indicator vs full-row).
6. §6 — Confirm `ColorRole` exposes every role in the §5 table.
7. §8 — Naming: `ElwhaNavigationRail` vs `ElwhaNavRail`; `ElwhaNavRailDestination` vs `ElwhaNavRailButton`.
8. §9.1 — Morph duration: 300 ms (MEDIUM2) or 350 ms (MEDIUM3).
9. §9.3 — Stacked↔inline label-relocation math: inline in destination vs extend `ContentMorphPainter`.
10. §9.4 — Easing curve confirmation against M3 motion tokens.
11. §10.1 — `AccessibleRole` choice: `PAGE_TAB_LIST + PAGE_TAB` vs `LIST + RADIO_BUTTON`.
12. §10.2 — `Escape` in Expanded: collapse vs consumer-controlled.
13. §13 — Phase 5 separate or folded.
