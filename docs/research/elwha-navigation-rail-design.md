# ElwhaNavigationRail — Design Decisions

**Status:** DRAFT for epic [#159](https://github.com/OWS-PFMS/elwha/issues/159), milestone v0.3.0. Mirrors the structure of `elwha-fab-design.md` and `elwha-badge-design.md`. Sections marked **[LOCKED]** are not expected to move during implementation. Seeded from `elwha-navigation-rail-research.md` (25-screenshot M3 capture + M3 token tables for Vertical/Horizontal nav rail items).

## TL;DR

Two new public types compose the rail:

- **`ElwhaNavigationRail`** — the container. Two variants (`COLLAPSED` / `EXPANDED`, matching M3 naming), optional menu toggle, optional anchored `ElwhaFab`, primary destinations always, expanded-only secondary destinations grouped under section headers, animated `morphTo(Variant)` between variants.
- **`ElwhaNavRailDestination`** — the rail slot (the "rail button"). Dedicated component, **not** an extended mode on `ElwhaIconButton` / `ElwhaButton`. Composes shared painters (`SurfacePainter`, `RipplePainter`, `ShapeMorphPainter`, `ContentMorphPainter`, `MorphAnimator`) plus `ElwhaBadge` via `ElwhaBadgeAnchor`. Handles its own Collapsed↔Expanded morph driven by its parent rail's variant.

Single-mandatory selection lives on the container, conceptually parallel to chip-list `SINGLE_MANDATORY` (pattern reuse, not code reuse — the rail is not an `ElwhaList<T>`).

The destination morph is a **subset** of FAB's choreography plus an active-indicator dimension/shape morph. The label *cross-fades* between two discrete anchor positions (stacked-below in Collapsed, inline-beside in Expanded) — it does not translate along a path. This means `ContentMorphPainter` (#223) covers the rail's needs without extension.

Hard dependencies, both shipped: `ElwhaFab` ([#160](https://github.com/OWS-PFMS/elwha/issues/160) ✅), `ElwhaBadge` ([#209](https://github.com/OWS-PFMS/elwha/issues/209) ✅). Foundation prereq open: `ContentMorphPainter` extraction ([#223](https://github.com/OWS-PFMS/elwha/issues/223)). Small prereq open: `MaterialIcons` fill-0→fill-1 axis (to be filed as a Phase 1 story).

---

## §0. Posture: M3 Expressive baseline, post-May-2025

Same posture as the FAB and Badge designs. The M3 Expressive update deprecated the baseline navigation rail; only the Expressive `Collapsed` and `Expanded` variants are built. No baseline fallback, no deprecation shim — pre-1.0, no compat layers.

Follows [`elwha-design-direction.md`](elwha-design-direction.md) §9 (raw Swing + tokens can't express this) and the `m3-morph-is-multi-component` doctrine — the Collapsed↔Expanded transition's primitives come from the shared morph kit.

## §1. Scope decisions — Elwha adaptation of the M3 spec

Out of scope (from `elwha-navigation-rail-research.md` §1):

- Baseline rail variant.
- Phone / compact window size classes.
- Modal expanded layout. Expanded is always non-modal ("Standard" in M3 terms).
- Breakpoint-driven auto-switching between variants — the consumer flips the variant explicitly.
- Page-content transitions — Elwha fires the selection event; the consumer swaps content.

In scope: `Collapsed` ↔ `Expanded` variants and the animated transition between them. The header chrome (menu toggle, FAB) is hosted, not owned — the rail accepts an `ElwhaIconButton` for the menu and an `ElwhaFab` for the action slot.

**Naming:** Elwha retains M3's `Collapsed` / `Expanded` terminology rather than overlaying its own (e.g., "Compact"). The destination's two layouts use the same Collapsed / Expanded names.

## §2. Component model — two components, one epic

The rail is two cooperating components — split for the reasons enumerated below — not one monolithic class.

| Component | Owns |
|---|---|
| `ElwhaNavigationRail` | container chrome, variant state, destination list, section grouping, selection model, morph orchestration (drives every destination's morph in lock-step) |
| `ElwhaNavRailDestination` | one slot's icon + label + active indicator + badge anchor, the Collapsed↔Expanded layout transition, hit target, focus + ripple |

**Why split:**

1. The destination has its own paint contract that differs from the container's (its body paints the active-indicator pill, ripple, state layer; the container paints the rail surface + optional divider). Composing them as separate `JComponent`s lets each own its paint cleanly without conditionals.
2. The destination's API (label, icon, selected, badge) maps to a per-slot consumer call. The container's API (variant, menu, fab, primary[], sections[]) maps to a global call. One class with both surfaces would conflate.
3. Section grouping introduces a non-flat structure in Expanded (header → secondary destinations). A list of `ElwhaNavRailDestination` instances + section-header markers is easier to reason about than per-slot conditionals.
4. The destination is the rail's "rail button" — naming it gives the consumer a clear handle for keyboard navigation, focus traversal, and testing.

**Why NOT extend `ElwhaIconButton` / `ElwhaButton` for the destination:** decided 2026-05-27. The destination has (a) stacked-vs-inline label layout, (b) selection-pill scope that changes between modes (icon-only pill in Collapsed, full-row pill in Expanded), (c) icon-anchored badges, (d) single-mandatory selection driven by a parent container, and (e) a per-mode hit-target invariant (full rail width regardless of indicator shape). None of those fit a general-purpose button; adding them as modes would bloat Button / IconButton for one consumer. Shared scaffolding lives at the painter/theme layer instead.

## §3. Content rules

Per-destination:

- **Label text** — required. One word ideal. Never truncated, ellipsized, or shrunk (M3 G21). Wraps to ≤ 2 lines.
- **Icon** — required. Resolved via the `MaterialIcons` fill axis (consumer passes a single Material symbol; the destination resolves the fill-1 form for the selected state). A two-icon escape hatch accepts arbitrary `Icon` instances for consumers using custom (non-Material) glyphs. See §8 for factories.
- **Badge** — optional. Either `ElwhaBadge.small()` (dot, no content) or `ElwhaBadge.large(...)` (numeric or capped-count). Attached via `ElwhaBadgeAnchor.attach(this, badge)`; the destination implements `IconBearing` for the anchor's positioning.
- **Selected** — destination doesn't carry its own selected boolean publicly; the container's selection model is authoritative. The destination paints from a state set by the rail.

Per-container:

- **Primary destinations** — 3–7 (M3-recommended range; not enforced, but a `Logger.warning` is filed if outside).
- **Sections** — zero or more. Each section is a header label + a list of secondary destinations. Sections are shown only when `variant == EXPANDED`.
- **Menu button slot** — optional `ElwhaIconButton`; if absent, the rail is fixed-state (consumer must drive variant changes via API).
- **Anchored action slot** — optional `ElwhaFab`. If present, the rail orchestrates the FAB's Standard↔Extended form to track its own Collapsed↔Expanded variant.
- **Trailing actions slot** — optional `List<ElwhaIconButton>`, anchored to the bottom of the rail surface, below the destination stack. Typically holds utility/system actions (theme toggle, settings, help, playground launcher) — not destinations. *Elwha extension beyond the M3 token tables; rationale below.*

> **Note on the trailing actions slot (Elwha extension):** the formal M3 nav rail token spec doesn't enumerate a trailing/footer slot. However, m3.material.io itself renders a rail with bottom-anchored utility buttons (theme toggle, playground launcher), so the pattern is demonstrated by the spec's own home site even though not formally documented. Elwha follows the demonstrated pattern: an optional list-of-actions slot below the destinations, treated as "rail-hosted but not a destination" content. Consumers that don't need it pass nothing.

## §4. Size axis (M3 token-locked)

### §4.1 Token reference [LOCKED]

From the M3 Specs tab tokens (`Nav rail item - Vertical` / `Nav rail item - Horizontal`):

| Token | Collapsed (Vertical) | Expanded (Horizontal) |
|---|---|---|
| Active indicator height | 32dp | 56dp |
| Active indicator width | 56dp (icon pill) | row-content-width (`Hug`) |
| Icon-label space | 4dp (vertical, after indicator) | 8dp (horizontal, after icon) |
| Leading space | 16dp | 16dp |
| Trailing space | 16dp | 16dp |
| Icon glyph | 24dp | 24dp |

### §4.2 Derived destination geometry

Inside the Collapsed active indicator: 4dp pad above + 24dp icon + 4dp pad below = 32dp tall; 16dp pad left + 24dp icon + 16dp pad right = 56dp wide.

**Collapsed destination content** (top of indicator to bottom of label-area):

```
32 (indicator) + 4 (icon-label space) + ~14 (label text) + 6 (below-label padding, visual)
= 56dp content height
```

Plus 4dp inter-destination gap = **60dp Collapsed pitch.**

**Expanded destination content:** 56dp (the indicator IS the row at full Hug width). **Expanded pitch = 56dp** (rows visually contiguous; verify against M3 specs for any inter-row gap).

Neat coincidence: Collapsed destination content height (top of indicator to bottom of label-area) and Expanded destination row height are both **56dp**. The destination's vertical *footprint* is the same in both variants; only its internal layout (stacked vs inline) changes.

### §4.3 Variant comparison

| Aspect | Collapsed | Expanded |
|---|---|---|
| Container width | 96dp fixed | 220–360dp configurable |
| Destination row footprint | 56dp content + 4dp inter-row | 56dp (contiguous) |
| Destination layout | icon-over-label stacked | icon-beside-label inline |
| Active indicator | 32×56 icon pill | 56-tall row pill, `Hug` width |
| Anchored FAB | Standard (icon only) | Extended (icon + text) |
| Destinations shown | primary only (3–7) | primary + sectioned secondary |
| Badge placement | upper-right of icon | beside the label |
| Menu icon | ☰ ("expand") | collapse glyph ("collapse") |

`Fill`-width indicator mode in Expanded is **out of scope** (§14); file a follow-up if a consumer needs it.

## §5. Color axis

Color roles from the theme `ColorRole` facade (all eight needed roles confirmed present on `ColorRole.java`):

| Part | Role |
|---|---|
| Container | `SurfaceContainer` (optional fill) |
| Active item icon | `OnSecondaryContainer` |
| Active indicator | `SecondaryContainer` |
| Active item label | `Secondary` (Collapsed) · `OnSecondaryContainer` (Expanded) |
| Inactive item icon | `OnSurfaceVariant` |
| Inactive item label | `OnSurfaceVariant` |
| Large badge (delegated) | owned by `ElwhaBadge` |
| Small badge (delegated) | owned by `ElwhaBadge` |
| Divider | `OutlineVariant` |

Active-label color is variant-dependent — see §11 footnote on why.

## §6. State model

Per-destination states: `Enabled` (default), `Hovered`, `Focused`, `Pressed`, `Disabled`. Mapped via `theme/StateLayer`. Selected is orthogonal — a destination can be `Hovered + Selected` simultaneously.

**Invariant (LOCKED):** the destination hit target spans the **full rail width** in both Collapsed and Expanded, regardless of the active-indicator pill's shape (icon-only pill in Collapsed still gets a row-wide hit). Reconfirmed across three M3 screenshots.

**State-layer overlay (LOCKED):** pill-shaped, aligned with the active-indicator pill — matches M3 reference visuals. In Collapsed this means hover/focus/press feedback paints under a 32×56 puck behind the icon; in Expanded it paints under the row-wide pill. The hit target is full-row in both, but the visual affordance is the pill. (Pattern matches Button / Chip: state layer follows the surface, not the hit area.)

Ripple originates from the click point and uses `RipplePainter`, clipped to the pill shape (matches state-layer scope).

Focus ring: standard Elwha focus treatment (matches Button / Chip). No M3-specific deviation expected.

## §7. Anatomy

### §7.1 Collapsed destination

```
    +------------------+
    |    +--------+    |   ← 32×56 active indicator (visible
    |    |  icon  |    |     under select, hover, focus, press)
    |    +--------+    |
    +------------------+
         (4dp gap)
         (label)              ← ~14dp Inter Medium
         (6dp below)
     ─ ─ ─ ─ ─ ─ ─ ─ ─
         (4dp inter-destination gap)
         next destination's indicator…
```

### §7.2 Expanded destination

```
+--------------------------------------+   ← 56dp tall pill (active
|   +----+                             |     indicator at Hug width)
|   | ic |   Label             [+3]    |
|   +----+                             |
+--------------------------------------+
  (16dp leading)(8dp gap)(label)(badge slot)(16dp trailing)
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
|        |               |                 |
|        |               |                 |   ← trailing actions
| (●)    |               | (●)             |     anchored to bottom
| (☾)    |               | (☾)             |     (icon-only buttons)
+--------+               +-----------------+
   COLLAPSED                  EXPANDED
   96dp                       220–360dp
```

## §8. API design [DRAFT — settle in Phase 1]

Sketch only; concrete signatures land during Phase 1 implementation review.

**`ElwhaNavigationRail`** [LOCKED name]

```java
public final class ElwhaNavigationRail extends JComponent {

  // Construction
  public static ElwhaNavigationRail collapsed();
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

  // Trailing actions (Elwha extension — see §3)
  public void setTrailingActions(List<ElwhaIconButton>);   // null/empty = no actions
  public List<ElwhaIconButton> getTrailingActions();

  // Selection (single-mandatory)
  public ElwhaNavRailDestination getSelected();
  public void setSelected(ElwhaNavRailDestination);
  public void addSelectionListener(NavRailSelectionListener);

  // Configuration
  public void setExpandedWidth(int px);        // clamped [220, 360]
  public void setDivider(boolean);
  public void setElevation(int level);         // 0 or 1
}

public enum Variant { COLLAPSED, EXPANDED }
```

**`ElwhaNavRailDestination`** [LOCKED name]

```java
public final class ElwhaNavRailDestination extends JComponent implements IconBearing {

  // Construction — fill axis primary path
  public static ElwhaNavRailDestination of(MaterialIcons.Symbol icon, String label);

  // Construction — escape hatch for custom (non-Material) glyphs
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

- per-variant static factories (`collapsed()` / `expanded()` for the rail);
- `getX()` only, no `getEffectiveX()`;
- single-arg convenience constructors where the M3 default makes sense;
- Javadoc `@author` / `@version` / `@since` on every public class + method, bumped each touch (verified by `validate-versions`).

## §9. Collapsed ↔ Expanded morph [LOCKED motion contract]

The morph is the rail's central animation. It runs in two coordinated tiers.

### §9.1 Container morph

`ElwhaNavigationRail.morphTo(Variant)` drives a `MorphAnimator` at duration `MorphAnimator.MEDIUM3_MS` (**350 ms** — placeholder, smoke-test confirms; longer than FAB's `MEDIUM2_MS` 300 ms to account for the wider distance the container travels). The container animates its preferred width from 96dp to `expandedWidthPx` (or in reverse).

When the rail hosts an `ElwhaFab`, the container `morphTo` simultaneously calls `fab.morphTo(EXTENDED | STANDARD)` so the FAB's Standard↔Extended choreography stays phase-locked with the rail's variant change. The FAB morph contract from `elwha-fab-design.md` §9 covers the FAB's own internals.

Easing matches FAB: the curve `MorphAnimator` selects for its 350 ms tier (currently the same family FAB uses for its 300 ms tier; verify on smoke).

### §9.2 Per-destination morph

Every destination runs in lock-step with the container's progress (it doesn't own a `MorphAnimator` — the container pushes its progress in). The rail's destination morph is a *subset of FAB's choreography* plus an active-indicator dimension/shape interpolation:

| Transition | Implementation |
|---|---|
| Active-indicator width | `lerp(56, rowContentWidth, progress)` |
| Active-indicator height | `lerp(32, 56, progress)` |
| Active-indicator shape (corner-radius) | `ShapeMorphPainter.interpolate(collapsedRadii, expandedRadii, progress, easing)` — pill in both, but the corner-radius:height ratio shifts |
| Label paint position | **Discrete switch** at `progress = 0.5`: stacked-below anchor for `[0, 0.5)`, inline-beside anchor for `[0.5, 1.0]`. No translation along a path. |
| Label alpha | `ContentMorphPainter.labelAlpha(progress)` — cross-fade with 0.5 inflection (fades out at the stacked anchor, fades in at the inline anchor). Existing FAB choreography, no new primitives. |
| Label color | Token interpolation — `OnSurfaceVariant` ↔ `OnSecondaryContainer` (selected only) |
| Badge position | delegated to `ElwhaBadgeAnchor` — the anchor reads the host's `getIconBounds()` each frame and tracks the icon position automatically |

**Why the label is a discrete-switch + cross-fade and not a translation:** verified against the M3 reference animation — the label disappears in its stacked position before reappearing in its inline position, rather than sliding between them. Implementation-wise this is two `Point` anchors and one `labelAlpha(...)` call, no path interpolation.

### §9.3 Motion kit reuse

| Helper | Use |
|---|---|
| `MorphAnimator` (container-owned) | Drives every destination + the FAB in lock-step |
| `Easing` | FAB-matching curve; smoke-test confirms |
| `ShapeMorphPainter` | Active-indicator corner-radius shape morph (existing helper) |
| `ContentMorphPainter` | `labelAlpha` primitive (NEW — [#223](https://github.com/OWS-PFMS/elwha/issues/223)). Width and icon-X primitives are unused by the rail (the rail does its own indicator-dimension lerps; the icon doesn't translate). |
| `SurfacePainter`, `RipplePainter`, `StateLayer` | Per-frame surface paint, ripple, hover/press overlays |
| `ElwhaBadgeAnchor` | Badge position tracks the icon glyph automatically (existing) |

No new motion infra beyond #223. The rail consumes a strict subset of `ContentMorphPainter`'s API; #223's scope as filed remains correct.

## §10. Accessibility

### §10.1 Architectural choice [LOCKED]

The container is a `JComponent` with `AccessibleRole.PAGE_TAB_LIST`. The destination is a `JComponent` with `AccessibleRole.PAGE_TAB`. This matches the ARIA `tablist` + `tab` pattern that is the documented standard for navigation rails — nav destinations are page tabs that switch views, not form-input controls.

Rationale: extending `AbstractButton` for the destination was tempting (free Space/Enter + tab focus) but conflicts with the rail's container-driven selection model (each destination's `selected` is downstream of the container, not click-toggle-owned). A plain `JComponent` with explicit focus + keybinding wiring is cleaner; ripple-on-press is per-component anyway. `RADIO_BUTTON` was considered but rejected — semantically wrong (radio buttons are form-input controls) and would confuse screen-reader users into thinking they're filling out a form.

### §10.2 Keyboard navigation

| Key | Behavior |
|---|---|
| `Tab` | Container focus → menu button → FAB → first destination → next focusable outside the rail |
| `Shift+Tab` | Reverse |
| `↑ / ↓` | Move focus between destinations within the rail (focus, not selection) |
| `Space / Enter` | Select the focused destination |
| `Home / End` | First / last destination |

Selection moves only on explicit activation (Space / Enter / click), not on focus traversal. Matches `aria-activedescendant`-style tab lists.

`Escape` is intentionally **not** handled by the rail. The expanded rail is non-modal (§1), so claiming `Escape` would steal it from whatever the host app uses it for (closing dialogs that opened from a destination, dismissing menus, etc.). Consumers that want Escape-to-collapse can wire it with three lines of `InputMap` on the rail; the lib does not default it.

### §10.3 Labeling

- **Container** — accessible name from `setAccessibleName(...)` (consumer-supplied, e.g., "Primary navigation"). Default is empty; flag a `Logger.warning` if unset at first paint.
- **Destination** — accessible name = `label`. The badge appends a count fragment via `ElwhaBadgeAnchor`'s push-model name splicing (existing #209 behavior).
- **Selected announcement** — `AccessibleSelection`-based; the container fires a selection event the screen reader picks up.

### §10.4 Other a11y rules — DOCS

| M3 requirement | Mechanism |
|---|---|
| 4.5:1 contrast on all labels | Token system enforces; verify on every Material palette before release |
| 24×24px min tap target | Collapsed destination is rail-width × 60dp; passes |
| Reduced-motion preference | `MorphAnimator` already respects the OS reduced-motion hint (see [[m3-morph-is-multi-component]]); verification matrix in §13 Phase 5 |

## §11. RTL mirroring

The whole rail mirrors under `ComponentOrientation.RIGHT_TO_LEFT`: container docks to the right of the content area, destinations lay out icon-on-right + label-on-left in Expanded, active-indicator pill mirrors, badge position mirrors (via `ElwhaBadgeAnchor` RTL support from #209).

Active-label color stays variant-dependent: in Collapsed the label is *below* the indicator (so `Secondary` for contrast against the rail surface); in Expanded the label sits *inside* the `SecondaryContainer` pill (so `OnSecondaryContainer` for contrast against the pill). The orientation of the label relative to the pill is the deciding factor, not LTR/RTL.

## §12. Guidelines reference

- M3 Navigation rail — overview / specs / guidelines tabs (capture 2026-05-20/21).
- `elwha-navigation-rail-research.md` — the 25-screenshot M3 capture this design is seeded from.
- `elwha-fab-design.md` §9 — the morph kit the rail composes with.
- `elwha-badge-design.md` §5 / §9 / §10 / §11 — the badge anchor contract the destination depends on.

## §13. Story breakdown (Phases 1–5)

Story numbers TBD; filed under epic #159 once this design doc is reviewed.

### Phase 1 — `ElwhaNavRailDestination` (Collapsed form)

1. `MaterialIcons` fill-0→fill-1 axis story — adds a `MaterialIcons.filled(...)` resolver (or analogous) so a single symbol-handle resolves both states. Selected glyphs needed for the destination.
2. `ElwhaNavRailDestination` skeleton — class shell, factories (fill-axis primary + two-icon escape hatch), `IconBearing` impl, Collapsed layout (icon-over-label, 32×56 indicator, 4 / 16 / 16 paddings), state layer (pill-shaped), ripple, focus, basic paint. No selected state, no badge yet.
3. Destination selected state in Collapsed — active-indicator pill around the icon, color shift, fill-0→fill-1 swap. No animation yet.
4. Destination badge slot — `setBadge` + `ElwhaBadgeAnchor.attach(this, badge)` integration. Verify badge tracks icon position via `IconBearing`.
5. Destination playground + Showcase Gallery panel — visual smoke-test artifacts (per `fresh-demo-per-story`).

### Phase 2 — `ElwhaNavigationRail` (Collapsed only)

6. Rail container skeleton — `collapsed()` factory, surface paint, divider, elevation, header chrome slots (menu button, FAB), trailing-actions slot (§3).
7. Primary destinations + single-mandatory selection model — container holds the list, drives `selected` push to each destination, fires selection events.
8. Keyboard navigation (per §10.2) — Tab in/out, ↑/↓ within, Space/Enter to select.
9. Rail playground + Showcase Workbench entry — interactive demo for a static Collapsed rail.

### Phase 3 — Expanded variant + Collapsed↔Expanded morph

10. Expanded layout: destination inline form (icon-beside-label, 56-tall row pill at `Hug` width) — static, no morph yet.
11. Expanded layout: container width range + section headers + secondary destinations — static.
12. `ContentMorphPainter` consumer wiring — destination composes `labelAlpha` for the cross-fade; rail-local lerps drive indicator height/width.
13. `ElwhaNavigationRail.morphTo(Variant)` — orchestrates every destination + the FAB in lock-step. Discrete label-anchor switch at 0.5; corner-radius via `ShapeMorphPainter`; smoke-confirm 350 ms `MEDIUM3_MS` duration.
14. Expanded keyboard navigation + secondary-destination focus traversal.

### Phase 4 — Showcase integration + placement

15. Showcase Workbench: Navigation Rail entry, variant toggle, all-knobs configuration.
16. Showcase Gallery panel: side-by-side Collapsed + Expanded reference, selected/unselected/badge variants.
17. Real placement on the Showcase frame (replaces the temporary sidebar) — analogous to the FAB Phase 5 floating-FAB placement on the layered pane.

### Phase 5 — Polish (separate)

18. Active-indicator grow-from-center animation on selection. **Shipped** — per-destination `MorphAnimator` at `MEDIUM2_MS`, indicator pill grows from icon-center outward to the full Collapsed `32×56` / Expanded full-row size. Icon fill-0→fill-1 swap and label color shift land discretely at progress 0.5.
19. Full `ShadowPainter`-driven drop shadow when the rail sits on a layered pane (replaces the Phase 2 placeholder trailing-edge gradient). **Shipped** — `paintComponent` calls `ShadowPainter.paint(g, bodyWidth, h, 0, elevation)`; the rail exposes `trailingShadowReserve()` so a layered-pane host can size bounds to `pref.width + reserve` and let the halo land outside the body silhouette. The Showcase calls `setElevation(1)` on its floating rail. Layout-managed hosts that don't widen bounds see the halo clip on the body's trailing edge — the documented trade-off.
20. Reduced-motion fallback verification. **Shipped** — `MorphAnimator`'s static `reducedMotion` flag is the single source of truth, auto-detected at class-load (macOS `apple.awt.reduceMotion`, Windows `win.text.animationsEnabled`, GNOME `gsettings enable-animations`) and overridable via `ElwhaTheme.config(...).reducedMotion(...)` and The Elwha Showcase's Animation control. Verification matrix below.

#### §13.1 Reduced-motion verification matrix

Every per-tick animation path the rail participates in honors the global `MorphAnimator.isReducedMotion()` flag — `start()` / `reverse()` snap to the destination value and the underlying `Timer` never schedules ticks. Manual smoke pass on `ElwhaShowcase` with Reduced motion ON:

| Path | Animator | Result |
|---|---|---|
| Rail `Collapsed↔Expanded` morph (variant + container width + per-destination indicator) | Rail-owned `MorphAnimator(MEDIUM3_MS)` | ✅ Snaps in one paint cycle; no in-between progress |
| Destination grow-from-center selected-indicator | Per-destination `MorphAnimator(MEDIUM2_MS)` | ✅ Indicator paints at full pill size on click; no grow |
| Slotted FAB Standard↔Extended transform driven by the rail | `ElwhaFab`-owned `MorphAnimator` | ✅ FAB snaps in lock-step with the rail variant change |
| Showcase JLayeredPane 60-Hz morph-tracker timer | n/a — bounds re-position only fires while `rail.isMorphing()` returns true | ✅ Becomes a single bounds-set on the snap tick; no slow-pan |

Regression check (Reduced motion OFF) confirms all four paths animate normally — the new per-destination animator hasn't broken the existing rail or FAB orchestration.

## §14. Out of scope (LOCKED)

- Baseline (non-Expressive) navigation rail.
- Modal expanded layout.
- Breakpoint-driven auto-switching between variants.
- Page-content transitions ("top level transition pattern").
- Phone / compact window size classes.
- **Active-indicator `Fill` mode in Expanded.** M3 frames `Hug` as default and `Fill` as a "consider modifying" customization. Skipped; file a follow-up if a consumer needs it.
- Built-in `Escape`-to-collapse keybinding (consumer-controlled, §10.2).
- Multi-select destinations.
- Drag-reorder of destinations.
- Custom destination layouts (icon-only, label-only, icon+label-stacked horizontally) — the two M3 layouts are the contract.
- **Navigation Drawer** (separate component) — explicitly deprecated by M3 Expressive in favor of the Expanded rail variant. We do not build a standalone `ElwhaNavigationDrawer`; the Expanded rail's section support covers the same use cases.
- **Hover-flyout contextual submenu** (the m3.material.io docs-site pattern where hovering a rail destination reveals a sub-list of pages). DOM inspection of m3.material.io confirms it's implemented as **two sibling components** — `<mio-left-nav-rail>` + `<mio-navigation-drawer>` — not as a rail extension. The drawer is what shows the sub-list; the rail just triggers it on hover. This is the structural argument for keeping it out of the rail epic: even M3's own site composes a rail and a separate Nav-Drawer-shaped component at the consumer level. Whether Elwha ships an `ElwhaNavigationDrawer` (given M3 Expressive deprecates the baseline Nav Drawer) is an open library-roadmap decision tracked separately from #159; the rail does not need to wait on it.

## §15. Resolved decisions

All previously-flagged `[OPEN]` items from prior drafts of this doc, with the resolution recorded:

| # | Topic | Resolution |
|---|---|---|
| 1 | Variant naming | M3's `Collapsed` / `Expanded` retained (§1) |
| 2 | Icon API | Fill axis on `MaterialIcons` primary + two-icon escape hatch (§3, §8) |
| 3 | Active-indicator `Fill` mode | Out of scope; ship `Hug` only (§4.3, §14) |
| 4 | Collapsed dimensions | Locked from M3 tokens: 32×56 indicator, 16 leading/trailing, 4 icon-label (§4.1) |
| 5 | State-layer overlay shape | Pill-shaped, follows active indicator (§6) |
| 6 | `ColorRole` coverage | Confirmed — all 8 roles present (§5) |
| 7 | Class naming | `ElwhaNavigationRail` + `ElwhaNavRailDestination` (§8) |
| 8 | Morph duration | `MEDIUM3_MS` 350 ms placeholder; smoke-test confirms (§9.1) |
| 9 | Label relocation math | Moot — no translation, only cross-fade between discrete anchors (§9.2) |
| 10 | Easing curve | Match FAB's curve (§9.1) |
| 11 | `AccessibleRole` | `PAGE_TAB_LIST` + `PAGE_TAB` (§10.1) |
| 12 | `Escape` in Expanded | Consumer-controlled; not handled by the lib (§10.2) |
| 13 | Phase 5 cadence | Separate phase (§13) |
