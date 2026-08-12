# Elwha desktop component gap analysis

**Date:** 2026-08-12
**Status:** Locked into release planning 2026-08-12. Tier 1 is the **v1.2.0 desktop-workbench
wave** — Toolbar #454, tooltip region anchors #804, StatusBar #805, SplitPane #806, Tree #807,
Canvas #808, Table #809. Tier 2 is the **v1.3.0 M3/M3E completeness wave** — FAB menu #185,
Snackbar #810, SplitButton #811, SearchField #812 (+ #453 search app bar; date/time pickers
scheduled last, revisited when that wave is planned). Individual candidates still graduate to
build-ready epics via their own spec/design docs (`/spec-epic` flow).
**Driver:** With v1.1.0/v1.1.1 shipped, the next release theme is new components — specifically
components that are *desktop-specific* but still follow M3 Expressive design principles. The
motivating consumer is the OWS analysis tool, which today hand-rolls its data tables, its
visualizations, and its bottom status bar in raw Swing.

---

## 1. Evidence: what the OWS analysis tool builds by hand

Screenshots of the OWS analysis tool (2026-08-12) show the following built from raw Swing:

1. **Two data tables** — a frequency table with sortable columns (sort indicator on the active
   header), right-aligned numerics, and an "Export Table" action; and a wider factor table with
   zebra striping, a narrow abbreviation column, and per-column numeric formatting.
2. **A tree view** — a "List" panel with a project root, factor nodes, and *relationship*
   children (arrow-icon leaves), plus a mini-toolbar of tree operations (select/deselect,
   expand-all, collapse-all).
3. **A custom-rendered visualization** — an Influence/Dependence quadrant scatter plot, with a
   zoom slider + percentage readout, marker-shape pickers, grid and label toggles, and a
   "Default Chart" reset action.
4. **A bottom status bar** — info icon + pipe-separated live segments
   (`Time Elapsed | Loops found | Loops/Sec`).
5. **Shell furniture around all of it** — icon toolbars above the table and chart, collapsible
   side panels (edge chevrons), and resizable split regions.

The pattern: the tool's *content* components are M3-covered by Elwha today, but its *workbench*
components — what makes it a desktop analysis app rather than a phone app — are all hand-rolled.

## 2. Where Elwha stands

The current 25 components cover the M3 core well: actions (button, icon button, button group,
FAB/anchor), selection controls (checkbox, radio, switch, chip), fields (text, select, slider,
color picker), containers (card, item list, side sheet), overlays (dialog ×2, menu, tooltip),
navigation (app bar, nav rail, tabs), and feedback (badge, progress, loading indicator), all over
the token foundation.

The holes fall into three distinct buckets:

- **Desktop workhorses M3 has never specced** — table, tree, status bar, split pane. These are
  not "missing M3 components"; they are components Material never defined because Material is
  phone-first. M3 has no data-table spec at all — the spec died with M2, and business-app
  developers have been asking for it since ([material-web #4052](https://github.com/material-components/material-web/issues/4052),
  [M2 data tables](https://m2.material.io/components/data-tables)). Filling these means Elwha
  *defines* the M3E treatment itself — precedent: `ElwhaSlider` was built from
  `BoundedRangeModel` up rather than wrapping `JSlider`.
- **M3 Expressive components that exist but are not in Elwha yet** — of the five new components
  M3E introduced in May 2025 (button groups, FAB menu, loading indicator, split button,
  toolbars — [overview](https://supercharge.design/blog/material-3-expressive)), Elwha shipped
  two (button group, loading indicator). **Toolbar, split button, and FAB menu are
  specced-and-waiting.** Notably, [the docked toolbar is M3E's replacement for the deprecated
  bottom app bar](https://9to5google.com/2025/05/18/material-3-expressive-toolbars/), so it
  doubles as the M3E-legitimate anchor for a status-bar-like bottom strip.
- **M3 core components skipped so far** — snackbar, date/time pickers, search field. Not
  desktop-specific, but real holes for any form-heavy consumer.

## 3. Cross-library survey

Every mature desktop toolkit converges on the same supplemental set beyond what Material specs.
This convergence is the strongest evidence of what a desktop component library must have:

| Component | Swing | JavaFX (+ControlsFX) | Qt | WPF / WinUI | Compose Desktop |
|---|---|---|---|---|---|
| Data table/grid | `JTable` | `TableView` | `QTableView` | DataGrid (Community Toolkit — WinUI itself has none) | community `compose-data-table` |
| Tree | `JTree` | `TreeView` | `QTreeView` | `TreeView` | — (gap) |
| Tree-table | `JXTreeTable` | `TreeTableView` | `QTreeView` | community | — |
| Status bar | hand-rolled (SwingX `JXStatusBar`) | ControlsFX `StatusBar` | `QStatusBar` | WPF `StatusBar` | — |
| Toolbar | `JToolBar` | `ToolBar` | `QToolBar` | CommandBar | — |
| Split pane | `JSplitPane` | `SplitPane` | `QSplitter` | `SplitView`/GridSplitter | JetBrains `SplitPane` component |
| Expander/collapsible | SwingX `JXTaskPane` | `TitledPane`/Accordion | — | `Expander` | — |
| Number spinner | `JSpinner` | `Spinner` | `QSpinBox` | `NumberBox` | — |
| Context menu | `JPopupMenu` wiring | `ContextMenu` | built-in | `MenuFlyout` | dedicated desktop context-menu API |
| Toast/notification | hand-rolled | ControlsFX `Notifications` | — | `InfoBar`/toast | M3 Snackbar |

Compose Multiplatform is the most instructive comparable — it is literally Material 3 on the
desktop, and JetBrains had to ship [desktop-only components outside the M3 spec](https://kotlinlang.org/docs/multiplatform/compose-desktop-components.html)
(split pane, [context menus](https://kotlinlang.org/docs/multiplatform/compose-desktop-context-menus.html),
scrollbars, tooltips) to make it viable. Elwha is walking the same road, with the advantage that
FlatLaf already covers scrollbars and base tooltips.

## 4. Candidate list

### Tier 1 — the desktop workbench set (OWS-driven, highest value)

| # | Candidate | M3E anchor | Evidence & scope notes |
|---|---|---|---|
| 1 | **`ElwhaTable`** — data table | None — Elwha defines it (M2 spec + M3 tokens as the base) | The flagship. Screenshots give the spec floor: sortable columns with indicator, resizable/reorderable columns, zebra striping option, per-column alignment/formatting, sticky header, row selection, cell renderers that host Elwha components, export hook. Decide early: virtualization vs pagination, and whether it reuses the `ElwhaList<T>` model/selection-model pattern from #67. Biggest epic of the wave, easily multi-phase. |
| 2 | **`ElwhaTree`** — tree view | None — Elwha defines it | Node icons, expand chevrons, typed children, multi-select, expand/collapse-all operations. Same model/adapter philosophy as `ElwhaItemList` (`T` = item, adapter → view). Leave `TreeTable` out of V1 but keep the door open — it is the classic follow-on. |
| 3 | **`ElwhaToolbar`** | **Real M3E spec** (docked + floating) | The OWS screens show ad-hoc icon strips everywhere. M3E specced this in May 2025 and Elwha already has the ingredients (icon buttons, button groups, menus). Medium scope, high leverage — it becomes the container idiom for table/chart/tree headers. |
| 4 | **`ElwhaStatusBar`** | None — anchor to the docked-toolbar treatment + surface-container tokens | Segmented API (leading icon area, message segments, separator treatment, trailing area), live-update friendly, optional inline progress on the trailing edge. Small epic; disproportionate "desktop-native" payoff. |
| 5 | **`ElwhaSplitPane`** | None — M3E extrapolation (drag-handle treatment; Compose has a `VerticalDragHandle` precedent) | Horizontal/vertical, token-styled divider + hover/drag states, collapse-to-edge affordance (which also covers the edge-chevron panel-collapse behavior in the OWS screens). Medium scope. |
| 6 | **`ElwhaCanvas`** — zoomable drawing viewport | None — token-driven by design | Reframed (2026-08-12) from an earlier "plot foundation" idea: the OWS scatter is a *custom drawing*, not a chart, and a charting library is its own product — permanently out of library scope. What Elwha ships instead is the desktop primitive underneath: a pan/zoom canvas where the library owns the **camera** and the consumer owns the **drawing**. Precedents: Qt `QGraphicsView`/`QGraphicsScene`, Piccolo2D, WinUI Community Toolkit `InfiniteCanvas`. Shape: one component over a viewport model (zoom + pan center + limits — the `ElwhaSlider`-over-`BoundedRangeModel` doctrine); programmatic API as the contract (`zoomIn()`/`zoomOut()`/`setZoom()`, `zoomToFit()`, `resetView()`, `pan(dx, dy)`, `worldToScreen`/`screenToWorld` for consumer hit-testing); viewport-change events so external controls bind to it — hooks for controls to hit, not built-in chrome (the OWS zoom slider / % readout / reset button, or any Elwha control, wire to the model). Painting contract in world coordinates with a **screen-space overlay layer** for labels/handles that must not scale with zoom (the QGraphicsView/Piccolo convergence). Gestures (cursor-anchored wheel zoom, drag-to-pan) opt-in OFF, per the side-sheet precedent. Also needs a **hover hit-testing hook** (hover point in world coordinates → consumer resolves the mark under it) feeding region-anchored tooltips — OWS shows mouseover tooltips on its custom-drawn marks; see the cross-cutting note below. Chrome/content line: the **background is chrome** — token-resolved via `ColorRole` (surface-role default, matching `ElwhaSurface`) so themes/modes flip correctly, never left to the custom drawing; the drawing is the consumer's, but the paint hook receives a **paint context exposing resolved token colors** so consumer-drawn content (gridlines, quadrant lines, marks) can be theme-correct without the canvas knowing what it depicts. No axes, marks, or legends — ever. Medium scope. |

### Cross-cutting consideration: region-anchored tooltips

`ElwhaTooltip.attach(JComponent)` anchors to a whole component — correct for buttons and chips,
but every Tier-1 heavyweight breaks that assumption identically: a canvas is *one* component
with many hoverable marks (OWS shows mouseover tooltips on its custom-drawn scatter marks), a
table is one component with many cells (truncated-cell tooltips are a desktop-table staple —
`JTable.getToolTipText(MouseEvent)` is Swing's classic answer), and a tree has many nodes. The
shared enabler is a **region anchor** for `ElwhaTooltip`: attach to a component *plus* a
hover-resolved sub-rect, so the existing rich-tooltip machinery (placement, delays, persistent
mode, passive-focus opt-out) works unchanged over sub-component regions. Elwha already has the
conceptual hook — `BodyBearing` anchors overlays to a painted body rect rather than full bounds;
a region anchor generalizes the same idea. This should be specced once (likely as tooltip/overlay
work) *before* Table/Tree/Canvas each invent their own hover-tooltip plumbing.

### Tier 2 — M3/M3E completeness (specs exist; fill the checklist)

| # | Candidate | Notes |
|---|---|---|
| 7 | **`ElwhaSnackbar`** | The one glaring M3-core absence in the Feedback family. Desktop apps need transient notifications too (ControlsFX ships `Notifications` for exactly this). Builds on the overlay host. Small-medium. |
| 8 | **`ElwhaSplitButton`** | M3E May-2025 component; slots naturally into the existing button family + `ElwhaMenu`. Small. |
| 9 | **FAB menu** | M3E May-2025 component; extends the existing FAB/anchor pair. Small-medium. |
| 10 | **Date picker / time picker** | M3-specced, form-staple, every toolkit has one. Real scope (calendar grid, input mode) — a candidate for the wave after next unless OWS needs it. |
| 11 | **`ElwhaSearchField`** | M3 Search spec, adapted to desktop as a field variant (leading icon, clear affordance, suggestion menu via `ElwhaMenu`). Pairs well with table/tree filtering (`ElwhaList` already has filter hooks). |

### Tier 3 — desktop patterns to keep on the radar (not this wave)

- **`ElwhaExpander` / collapsible section** — WinUI `Expander`, JavaFX `TitledPane`; useful for
  inspector panels like the OWS chart-settings rail.
- **`ElwhaNumberField`** — spinner/stepper (`JSpinner`, WinUI `NumberBox`); as an
  `ElwhaTextField` variant.
- **Context-menu wiring** — not a new visual (it is `ElwhaMenu`) but a first-class right-click
  attach API; Compose Desktop treats this as its own desktop API for good reason.
- **Menu bar** — the one big classic-desktop item deliberately *held*: M3 apps do not use menu
  bars, OWS does not appear to, and it drags in accelerator/mnemonic infrastructure. Skip until
  a consumer demands it.
- **Tree-table, breadcrumb bar, info banner, empty-state page** — all with strong precedent
  (ControlsFX/WinUI), all downstream of Tier 1 pieces.

### Explicitly out of scope

Docking-window frameworks, wizard/assistant flows, and bottom sheets (side sheet owns that role
on desktop). Also note: the open carousel/grid "aggregate split" architectural question
intersects with candidate #1 — the Table model/selection design should resurface that decision
rather than preempt it.

## 5. Suggested shape of the wave

If the release theme is "OWS can build its workbench entirely in Elwha," the coherent core is
**Table + Tree + Toolbar + StatusBar + SplitPane + Canvas** — with Toolbar first (M3E-specced,
small, and the other four consume it), the region-anchored tooltip enabler early (three of the
six consume it), Table as the flagship epic, and
Snackbar/SplitButton/FAB-menu as small fillers that also close out the M3E checklist. The
canvas's camera-not-drawing reframing makes it a normal medium epic rather than the open-ended
"charting product" it first appeared to be, so it joins the wave outright.

## Sources

- [Material 3 Expressive: New Components, Motion, Shapes, and More](https://supercharge.design/blog/material-3-expressive)
- [Android 16's Material 3 Expressive redesign (9to5Google, May 2025)](https://9to5google.com/2025/05/13/android-16-material-3-expressive-redesign/)
- [M3E toolbars replace the bottom app bar (9to5Google)](https://9to5google.com/2025/05/18/material-3-expressive-toolbars/)
- [Are Data Tables coming to Material 3? — material-web #4052](https://github.com/material-components/material-web/issues/4052)
- [M2 data tables spec](https://m2.material.io/components/data-tables)
- [compose-data-table (community M3 data table for Compose)](https://github.com/sproctor/compose-data-table)
- [Compose Multiplatform desktop-only components](https://kotlinlang.org/docs/multiplatform/compose-desktop-components.html)
- [Compose Desktop context menus](https://kotlinlang.org/docs/multiplatform/compose-desktop-context-menus.html)
- [JetBrains SplitPane component demo](https://github.com/JetBrains/compose-multiplatform/blob/master/components/SplitPane/demo/src/jvmMain/kotlin/org/jetbrains/compose/splitpane/demo/Main.kt)
- [ControlsFX features](https://github.com/controlsfx/controlsfx/wiki/ControlsFX-Features)
- [Qt `QGraphicsView` (viewport/scene split — canvas precedent)](https://doc.qt.io/qt-6/qgraphicsview.html)
- [Piccolo2D (Java zoomable-UI toolkit — canvas precedent)](https://github.com/piccolo2d/piccolo2d.java)
- [Windows Community Toolkit `InfiniteCanvas` (canvas precedent)](https://learn.microsoft.com/en-us/windows/communitytoolkit/controls/infinitecanvas)
- [WinUI DataGrid via Windows Community Toolkit](https://platform.uno/blog/uno-platform-and-windows-community-toolkit-datagrid-tabview-and-expander-with-sample-code)
