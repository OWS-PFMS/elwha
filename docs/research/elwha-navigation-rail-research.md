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

## 12. Decisions (post-capture)

All previously-open questions have been resolved during design-doc drafting on 2026-05-27. Final shapes live in `elwha-navigation-rail-design.md`; this section is the audit trail.

1. **Prerequisite sequencing** — *Resolved:* FAB & Badge sequenced before the rail; both shipped before Phase 1 of #159 starts.
2. **Icon fill** — *Resolved:* `MaterialIcons` fill-0→fill-1 axis as the primary path (single Material symbol resolves both states); two-icon factory as an escape hatch for custom non-Material glyphs. Filed as Phase 1 story under #159.
3. **Destination component shape** — *Resolved:* dedicated `ElwhaNavRailDestination` (final name) rather than extending `ElwhaIconButton` / `ElwhaButton` with rail-specific modes. Triggered by reviewing the screenshots: destinations have stacked-vs-inline label layout, selection-pill scope that changes between modes, icon-anchored badges, and single-mandatory selection — none of which fit a general-purpose button without bloating it for one consumer. Shared scaffolding stays at the painter/theme layer; the destination composes those primitives.
4. **Collapsed↔Expanded morph** — *Resolved:* the destination's morph is a **subset** of FAB's choreography plus an active-indicator dimension/shape interpolation. The label *cross-fades* between two discrete anchor positions (stacked-below in Collapsed, inline-beside in Expanded) — it does not translate along a path. Verified against the M3 reference animation video. `ContentMorphPainter` (#223) covers the rail's needs without extension. (Initial draft modeled the label as path-interpolated; the video correction simplified this substantially.)
5. **Animation scope** — *Resolved:* core Collapsed↔Expanded morph in Phase 3; indicator grow-from-center on selection deferred to a separate Phase 5 (polish).
6. **Component naming** — *Resolved:* `ElwhaNavigationRail` for the container; `ElwhaNavRailDestination` for the slot. M3's `Collapsed` / `Expanded` variant naming retained (no Elwha rename to "Compact").
7. **Expanded width API** — *Resolved:* `setExpandedWidth(int)` clamped to [220, 360], default per a sensible midpoint (TBD on Phase 3 smoke).
8. **Active-indicator `Fill` option** in Expanded — *Resolved:* out of scope; ship `Hug` (default) only. M3 frames `Fill` as a "consider modifying" customization. File a follow-up if a consumer needs it.

Additional decisions made during design-doc drafting:

9. **Active-indicator dimensions (Collapsed)** — locked from M3 tokens: 32dp tall × 56dp wide, 16dp leading/trailing horizontal pad, 4dp icon-label space. Expanded: 56dp tall, 8dp icon-label horizontal space, `Hug` width.
10. **Morph duration** — `MorphAnimator.MEDIUM3_MS` (350 ms) placeholder; smoke-test confirms.
11. **Easing curve** — match FAB's curve.
12. **State-layer overlay shape** — pill-shaped (follows the active indicator), not full-row. Hit target is still full-row.
13. **`AccessibleRole`** — `PAGE_TAB_LIST` (container) + `PAGE_TAB` (destination). Matches ARIA `tablist`/`tab` for navigation rails.
14. **`Escape` in Expanded** — not handled by the lib. Consumer-controlled; rail is non-modal, doesn't claim global Escape.
15. **Trailing actions slot (Elwha extension)** — *Resolved 2026-05-28:* add an optional `List<ElwhaIconButton>` slot anchored to the bottom of the rail surface, below the destinations. The formal M3 nav rail token spec doesn't enumerate this, but m3.material.io itself renders rails with bottom-anchored utility buttons (theme toggle, playground launcher). Elwha follows the demonstrated pattern. See design doc §3.
16. **No standalone Navigation Drawer** — *Resolved 2026-05-28:* M3 Expressive explicitly deprecates the Navigation Drawer in favor of the Expanded rail variant ([9to5google 2025-05-14](https://9to5google.com/2025/05/14/material-3-expressive-navigation/)). Elwha follows Expressive — the Expanded rail's section support covers the drawer's old use case.
17. **Hover-flyout submenu (deferred)** — *Resolved 2026-05-28:* the m3.material.io docs-site pattern of hover-revealing a contextual sub-list of pages is a desktop convention, not an M3 component. If a use-case emerges, file as a separate generic `ElwhaHoverFlyout` affordance — decoupled from the rail — not as a rail feature. Out of scope for the rail epic. See design doc §14.

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
