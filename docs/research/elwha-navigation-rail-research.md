# Elwha Navigation Rail — M3 Expressive Spec Capture (Research)

> **Status:** Research notes. Captures the M3 Expressive Navigation Rail specification (from m3.material.io) plus the Elwha-side adaptation decisions made during capture and follow-up review. The locked API + motion + a11y contract for the lib lives in `elwha-navigation-rail-design.md`; this file is the source material.
>
> **GitHub epic:** [#159](https://github.com/OWS-PFMS/elwha/issues/159) — `epic: M3 Navigation Rail component (soft spec)`. **Target milestone:** v0.3.0.

## Source

M3 Expressive Navigation Rail documentation at m3.material.io, captured 2026-05-20/21 from 25 screenshots (Overview / Specs intro / Guidelines tabs). The M3 site is a JS SPA, so this capture is screenshot-based — the **M3 Specs tab should be consulted to confirm exact dp values** flagged in §9 before they are treated as final.

---

## 1. Scope decisions — Elwha adaptation of the M3 spec

Elwha is a desktop Swing/FlatLaf library. The following M3 guidance is deliberately **out of scope**:

- **Baseline navigation rail** — deprecated by M3 Expressive; not built. No `Baseline` variant, no deprecation shim (consistent with the pre-1.0 "no compat shims" rule).
- **Phone / compact window size classes** — Elwha targets desktop windows only.
- **Modal expanded layout** — modality was the phone behavior. Elwha's expanded rail is always non-modal (M3 calls this "Standard").
- **Breakpoint-driven auto-switching** — collapsed/expanded is a developer/user-controlled state, not window-class-reactive.
- **Page-content transitions** ("top level transition pattern") — the consumer swaps page content; Elwha only fires the selection event.

In scope: the M3 Expressive `Collapsed` and `Expanded` variants and the collapsed↔expanded transition. Elwha follows M3 Expressive throughout — see `elwha-design-direction.md`.

## 2. Component overview

One container — working name `ElwhaNavigationRail` (naming open, §12) — with two variants:

- **Collapsed** — narrow rail, fixed width. Replaces the M3 baseline rail.
- **Expanded** — wide rail. Replaces the M3 navigation drawer.

Both are the M3 Expressive "wider" rails; the container transitions between them.

Each rail slot is its own component (the destination / "rail button"); the container hosts a list of them plus the optional header chrome (menu toggle, FAB).

## 3. Anatomy

| # | Part | Optional? | Notes |
|---|---|---|---|
| 1 | Container | — | optional fill, optional elevation, optional divider |
| 2 | Menu button | optional | toggles collapsed/expanded; absent ⇒ fixed-state rail |
| 3 | FAB / Extended FAB | optional | standard when collapsed, extended when expanded |
| 4 | Destination icon | — | fill-0 unselected, fill-1 selected |
| 5 | Active indicator | — | pill behind the selected item |
| 6 | Label text | required | ≤2 lines; never truncate / ellipsize / shrink |
| 7 | Large badge | optional | numeric or capped-count |
| 8 | Large badge label | optional | content inside the large badge |
| 9 | Small badge | optional | dot, no content |
| + | Section header | optional | expanded-only; heads a group of secondary destinations |
| + | Divider | optional | vertical, on the content-facing edge |

Header composition order, top to bottom: **menu button → FAB → destinations.**

## 4. Variant comparison

| Aspect | Collapsed | Expanded |
|---|---|---|
| Width | 96dp fixed | 220–360dp range |
| Item layout | icon-over-label | icon-beside-label |
| Active indicator | pill on icon | horizontal pill — `Hug` (default) or `Fill` |
| FAB | standard (icon only) | extended (icon + text) |
| Destinations shown | primary only (3–7) | primary + sectioned secondary |
| Badge placement | upper-right of icon | beside the label |
| Menu icon | ☰ ("expand") | collapse glyph ("collapse") |

## 5. Destination data model

- **Primary destinations** — 3–7. Shown in both variants.
- **Sections** — zero or more. Each has a header label + its own secondary destinations. Shown **only when expanded**.
- Each destination: required label, icon (fill-0 + fill-1), optional badge, selected state.
- **Selection: single-select, mandatory** — exactly one destination active at all times (it reflects the current page). Conceptually equivalent to the chip-list `SINGLE_MANDATORY` semantics; likely pattern-reuse, not code-reuse (the rail is not an `ElwhaList<T>`).

## 6. Configuration surface

- Variant: `Collapsed` / `Expanded`
- Expanded layout: `Standard` only (Modal dropped, §1)
- Expanded behavior: `TransitionToCollapsed` (default) / `HideWhenCollapsed`
- Expanded active indicator: `Hug` (default) / `Fill`
- Expanded width: configurable, range 220–360dp
- Menu button: present / absent (absent ⇒ fixed-state rail)
- FAB: present / absent
- Divider: on / off
- Elevation: 0 / level 1

The four "common layouts" are the menu × FAB matrix — both are independent booleans.

## 7. States

`Enabled`, `Hovered`, `Focused`, `Pressed` — maps to the theme `StateLayer` facade. `Disabled` is not shown in the M3 states page but is likely needed for completeness.

**Invariant:** the destination hit target spans the **full rail width**, regardless of the active-indicator shape. Reconfirmed across three screenshots (states collapsed, states expanded, active-indicator page).

## 8. Color roles — theme `ColorRole` facade

| Part | Role |
|---|---|
| Container | `SurfaceContainer` (optional fill) |
| Active item icon | `OnSecondaryContainer` |
| Active indicator | `SecondaryContainer` |
| Active item label | `Secondary` (collapsed) · `OnSecondaryContainer` (expanded) |
| Inactive item icon | `OnSurfaceVariant` |
| Inactive item label | `OnSurfaceVariant` |
| Large badge | `Error` |
| Large badge label | `OnError` |
| Small badge | `Error` |

Active-label color is **orientation-dependent**: collapsed (vertical) = `Secondary`; expanded (horizontal) = `OnSecondaryContainer`, because the label sits inside the `SecondaryContainer` indicator pill. **Verification item:** confirm `ColorRole` exposes all seven roles, particularly `Error` / `OnError`.

## 9. Measurements

- Collapsed container width: **96dp** (fixed)
- Expanded container width: **220–360dp** (range)
- Expanded item / active-indicator height: **~56dp**
- Smaller padding values (4 / 6 / 8 / 12 / 16 / 24 / 36) annotated in the Measurements screenshot — read off a low-res capture; **confirm against the M3 Specs tab before final.**

## 10. Behavior

- Vertical body scroll: rail stays fixed (inherent in a Swing `BorderLayout.WEST` placement).
- Menu icon swaps with state: ☰ (collapsed / "expand") ↔ collapse glyph (expanded / "collapse").
- Selection: destination icon fills (0→1), color shifts to the prominent role, active indicator appears.
- Animation: the active indicator **expands from the center of the icon** on selection; the collapsed↔expanded change is an animated transition. (Animation scope — v1 vs polish follow-up — open, §12.)
- Labels: never truncate, ellipsize, or shrink; wrap to a max of 2 lines.

## 11. Prerequisites / dependencies

| Item | Relationship | State |
|---|---|---|
| `ElwhaFab` | **Hard dependency** | ✅ shipped 2026-05-26 (epic [#160](https://github.com/OWS-PFMS/elwha/issues/160) — 5 phases) |
| `ElwhaBadge` | Soft integration only — rail positions, doesn't own appearance | ✅ shipped 2026-05-27 (epic [#209](https://github.com/OWS-PFMS/elwha/issues/209)) |
| `ContentMorphPainter` extraction | Foundation for the destination's Compact↔Expanded morph | 🟡 filed as [#223](https://github.com/OWS-PFMS/elwha/issues/223); not implemented |
| `MaterialIcons` fill-0→fill-1 axis | Selected-destination glyph state | 🟡 not filed; planned as Phase 1 story under #159 |

## 12. Open decisions (post-capture)

The following were carried forward as open questions at capture time; some have since been resolved. Resolved items are folded into `elwha-navigation-rail-design.md`.

1. **Prerequisite sequencing** — *Resolved:* FAB & Badge sequenced before the rail; both shipped before Phase 1 of #159 starts.
2. **Icon fill** — two-icon destination API vs a `MaterialIcons` fill axis. *Resolution pending* — leaning fill axis on `MaterialIcons` so existing icon-bearing components benefit, but final call is a Phase 1 design item.
3. **Destination component shape** — *Resolved 2026-05-27:* dedicated rail-destination component (working name `ElwhaNavRailDestination` / `ElwhaNavRailButton` — final name TBD in the design doc) rather than extending `ElwhaIconButton` / `ElwhaButton` with rail-specific modes. Triggered by reviewing the screenshots: destinations have stacked-vs-inline label layout, selection-pill scope that changes between modes, icon-anchored badges, and single-mandatory selection — none of which fit a general-purpose button without bloating it for one consumer. Shared scaffolding stays at the painter/theme layer; the destination composes those primitives.
4. **Compact↔Expanded morph** — *Resolved 2026-05-27:* the destination's morph is a *superset* of FAB's. FAB does icon-only → icon+label-inline (label fades in as the container widens). The rail destination does icon-over-label-stacked → icon+label-inline-in-pill — FAB's choreography *plus* a stacked↔inline label-position morph FAB doesn't have. `ContentMorphPainter` (#223) extracts the FAB-shaped primitives (icon-X interpolation, label alpha, container width); the relocation half is designed during the rail Phase 3 implementation, composed on top of those primitives.
5. **Animation scope** — collapsed↔expanded transition + indicator grow-from-center: v1 or polish follow-up. *Open.*
6. **Component naming** — `ElwhaNavigationRail` vs `ElwhaNavRail`; destination component `ElwhaNavRailDestination` vs `ElwhaNavRailButton`. *Open* — settle in the design doc.
7. **Expanded width API** — `setExpandedWidth(int)` clamped to [220, 360] vs a fixed default. *Open.*
8. **Active-indicator `Fill` option** in expanded — include or skip (M3 frames it as "consider modifying… to fill the container"). *Open.*

## 13. Screenshot index (25 captures)

1. Overview header — window sizes, 3–7 destinations + optional FAB, consistent placement.
2–3. Collapsed vs expanded; transition works on any device (tablet + phone — phones in scope for M3, out of scope for Elwha).
4. M3 Expressive update changelog — variant naming, configurations, color change.
5–7. Variants — Collapsed / Expanded only; baseline not built (availability table).
8. Configurations — Standard vs Modal expanded layout; expanded behavior.
9. Anatomy — the 9-part list.
10–11. Color — role mapping, light + dark.
12–13. States — Enabled / Hovered / Focused / Pressed; full-width hit target.
14. Measurements — 96dp / 220–360dp / ~56dp + paddings.
15–16. Common layouts — menu × FAB matrix.
17. Baseline anatomy — reference only; not built.
18. Menu (optional) — expand reveals secondary destinations; menu icon swaps.
19. Active indicator — single-select; Hug vs Fill in expanded.
20. Icons — selected icon fills (0→1) + color + indicator.
21–22. Label text — required, one word ideal, ≤2-line wrap, no truncate/ellipsis/shrink.
23. Badges (small / large-numeric / large-capped) + Divider (optional).
24–25. Behavior: scrolling (rail fixed) + Selection (icon fill, indicator grows from center).
