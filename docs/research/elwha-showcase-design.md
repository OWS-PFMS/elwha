# The Elwha Showcase — Design Decisions

**Deliverable for:** [issue #131](https://github.com/OWS-PFMS/elwha/issues/131) — Story 1 (design) of epic #130.

**Part of epic:** [#130 — Unified Elwha playground with dynamic multi-theme switching](https://github.com/OWS-PFMS/elwha/issues/130)

**Status:** LOCKED for build. Decided in an interactive design session with the operator on 2026-05-19.

**Author:** Charles Bryan (`cfb3@uw.edu`).

This document is the authoritative source of design decisions for the unified playground. The
remaining epic #130 stories (build work) consume these choices and are not to re-debate them —
any change requires reopening this document with rationale.

---

## TL;DR

1. **What it is:** **The Elwha Showcase** (`ElwhaShowcase`) — one curated playground that
   exercises the whole Elwha component set under multiple color themes, replacing the
   append-only `ThemePlayground` tab list.
2. **Layout:** a left **sidebar nav** + content area with a **header bar** (light/dark/system
   mode toggle + palette picker). Three sidebar sections: **Foundations**, **Components**,
   **Containers**. Today's two-level tab nesting flattens to one.
3. **Two canonical templates:** the **Component Workbench** (single-instance components — every
   option → a control, a live stage, an equivalent-Java code view) and the **Container
   Workbench** (multi-instance containers — container + controls + live event log). Every
   component/container uses one of the two; the layout is uniform.
4. **Panels:** everything that exists today **carries** into the initial Showcase, reorganized.
   Nothing is dropped. Visual refinement of each component's surfaces to the new "pro" bar is
   deferred to per-component refine stories.
5. **Themes:** **6 bundled palettes** spanning ROYGBIV — Red / Orange / Yellow / Green / Blue
   (new) plus the existing M3 baseline purple as the Indigo·Violet end. Bundled in the
   published jar as starter "Themes"; consumers can still ship their own.
6. **Output:** this note + the epic #130 story breakdown (§9), filed as sub-issues of #130.

---

## 1. Context — the raw material

`ThemePlayground` is today's de-facto unified playground: a `JFrame` with a light/dark/system
toggle and an 8-tab `JTabbedPane` (Color Roles, Type Scale, Swing Comps, Chip, Icon Button,
Button, Surface, Card). Several tabs nest **another** `JTabbedPane` inside, so the current
structure is **two levels of tabs deep**. It grew tab-by-tab as each component landed; the
layout was never designed, just appended to. It exercises only the one bundled palette
(`MaterialPalettes.baseline()`).

The per-component standalone playgrounds (`ElwhaChipPlayground`, `ElwhaIconButtonPlayground`,
`ElwhaButtonPlayground`, `ElwhaSurfacePlayground`, `ElwhaCardPlayground`) wrap the same panel
builders `ThemePlayground` composes.

What is in good shape and reused as-is:

- The `*PlaygroundPanels` builders (`ChipPlaygroundPanels`, `IconButtonPlaygroundPanels`,
  `ButtonPlaygroundPanels`, `SurfacePlaygroundPanels`) are **static and composable** — they
  drop into the Showcase with no change.
- The Card panels (`GalleryPanel`, `LiveConfigPanel`, `SnippetPanel`, `ElwhaCardListShowcase`,
  `CursorReferencePanel`) are stateful but already wired by plain composition.
- `ElwhaTheme.install(Config)` is idempotent and re-callable — runtime theme switching is
  already a solved mechanism; the light/dark toggle uses it.
- `PaletteLoader.loadTheme(resourcePath)` loads any classpath palette JSON; `MaterialPalettes`
  ships exactly one (`baseline.json`) today.

This epic is therefore an **architecture + theme-picker** job, not a rewrite.

## 2. Name

The unified playground is **The Elwha Showcase**. Main class: `ElwhaShowcase`.

## 3. Layout / information architecture

- **Left sidebar nav + content area.** Replaces the outer `JTabbedPane`. Flattens today's
  two-level tab nesting to one level.
- **Header bar** at the top of the content area: the LIGHT/DARK/SYSTEM mode toggle and the
  palette picker, side by side.
- **Sidebar sections:**

  | Section | Entries |
  |---|---|
  | **Foundations** | Color Roles · Type Scale · Shape & Space scales · Swing Comps |
  | **Components** | Button · Chip · Icon Button · Card · Surface |
  | **Containers** | Chip List · Card List · Button group · Icon Button group |

- Each **Component** entry is one single-level inner `JTabbedPane`: **Workbench** (primary) +
  **Gallery** (secondary). No deeper nesting anywhere in the Showcase.

## 4. Two canonical templates

The Showcase has exactly **two** reusable layout templates. Every component or container uses
one of them — uniformity is the point.

### 4.1 Component Workbench (single-instance components)

For Button, Chip, Icon Button, Card, Surface. Contract:

- **Every option the component class exposes** has an interactive control (combo for enums,
  checkbox for booleans, spinner for ints, etc.).
- A **live instance** reflects the controls immediately, centered on a **configurable surface
  stage** (see §5) — the component is always shown against an `ElwhaSurface`, not the bare panel.
- A **`Component | Surface` switcher** flips the controls column between the component's own
  controls and the surface's controls. The switcher is an `ElwhaChipList` in `SINGLE_MANDATORY`
  mode — the Showcase dogfooding the library's own segmented-control semantics.
- An **equivalent-Java code view** shows the construction code, refreshed on every change, and
  tracks the active switcher segment (component code vs. surface code).
- Same regions, same arrangement, every component.

### 4.2 Container Workbench (multi-instance containers)

For Chip List, Card List, and the Button / Icon Button group demos. These are inherently
multi-instance — a single live instance cannot express list/group behavior. Contract:

- The container with a representative set of children.
- Controls for the container's options.
- A **live event log** (selection, reorder, group-selection changes).
- `ElwhaCardListShowcase` already implements this pattern and is the reference.

## 5. Component Workbench layout — "pro", not POC-ish

The skeleton is today's Button Live view (`ButtonPlaygroundPanels.buildLivePanel()`): a
vertical `BoxLayout` of a cramped `GridBagLayout` control run, a bare floating stage, and a
`JTextArea` under a titled "Equivalent Java" border. It works but reads as a proof-of-concept.

The Showcase's Component Workbench is a deliberate **three-region** layout:

- **Stage** — the live component centered on a **configurable `ElwhaSurface`**. The surface is
  not decorative backdrop: it is a first-class, configurable part of the workbench. Its role,
  shape, border, size, and visibility are all driven from the controls column's `Surface`
  segment, so contrast and elevation are shown honestly and the component can be evaluated on
  any surface — or, with the surface hidden, on the bare stage background. This is the canonical
  `ComponentWorkbench` contract; every component inherits it.
- **Controls** — a `Component | Surface` switcher over a card-swapped controls column. Each
  segment's controls are grouped into labelled sections (not one flat `GridBag` run),
  scrollable, with consistent control widths and alignment.
- **Code view** — monospace, a proper header, and a Copy button. Card's `SnippetPanel` already
  does this; it is generalized into the shared code-view widget (§7).

The exact region split (e.g. stage + controls side by side over a full-width code view) is a
build-story call. The **contract** is: same regions, same arrangement, every component.

## 6. Panel inventory — carry / rework / drop

**Every panel that exists today carries into the initial Showcase. Nothing is dropped.** The
initial Showcase ships *what exists today, reorganized* into the new IA; visual refinement to
the "pro" bar is deferred to the per-component refine stories (§9). This deferral is the
operator's explicit call, not an invented scope cut.

| Existing panel | Disposition | Showcase destination |
|---|---|---|
| `ThemePlayground` Color Roles | Carry as-is | Foundations |
| `ThemePlayground` Type Scale | Carry as-is | Foundations |
| `ThemePlayground` Swing Comps | Carry as-is | Foundations |
| `ChipPlaygroundPanels.buildVariantGallery()` | Carry; refine later | Chip → Gallery |
| `ChipPlaygroundPanels.buildLiveListPanel()` | Carry; refine later | Containers → Chip List |
| `IconButtonPlaygroundPanels` variant gallery / sizes | Carry; refine later | Icon Button → Gallery |
| `IconButtonPlaygroundPanels` toggle examples | Carry; refine later | Containers → Icon Button group |
| `IconButtonPlaygroundPanels.buildLivePanel()` | Carry; refine later | Icon Button → Workbench |
| `ButtonPlaygroundPanels` variant gallery / sizes | Carry; refine later | Button → Gallery |
| `ButtonPlaygroundPanels.buildTogglesPanel()` | Carry; refine later | Containers → Button group |
| `ButtonPlaygroundPanels.buildLivePanel()` | Carry; refine later | Button → Workbench |
| `SurfacePlaygroundPanels.buildMatrixPanel()` | Carry; refine later | Surface → Gallery |
| `SurfacePlaygroundPanels.buildLivePanel()` | Carry; refine later | Surface → Workbench |
| Card `GalleryPanel` | Carry; refine later | Card → Gallery |
| Card `LiveConfigPanel` + `SnippetPanel` | Carry; refine later | Card → Workbench |
| Card `ElwhaCardListShowcase` | Carry; refine later | Containers → Card List |
| Card `CursorReferencePanel` | Carry as-is | Card → Gallery (or a Card reference tab) |

The five standalone playgrounds are **kept** as focused entry points — they wrap the same
builders, so deleting them buys nothing and risks no drift. Whether to retire them later is a
post-epic question, not precommitted here.

## 7. Net-new work

- `ElwhaShowcase` main class — the sidebar-nav shell and header bar.
- The **palette picker** in the header + `MaterialPalettes` bundled-set API + 5 new palette
  JSON resources (§8).
- A **shared code-view widget** — generalize Card's `SnippetPanel` so all Component Workbenches
  use one code view.
- A **shared Component Workbench scaffold** and **Container Workbench scaffold** so all five
  components and four containers use one layout each.
- **Chip has no single-instance Workbench today** — its current tab is Variant gallery + Live
  list (the container). A net-new Component Workbench for `ElwhaChip` is required.
- The Button / Icon Button **group demos are preset galleries today**, not interactive
  Container Workbenches — upgrading them to the Container Workbench pattern is refine work.
- *Optional:* a Foundations **"Icons" gallery** for the 17 bundled `MaterialIcons` symbols.
  Flagged, not committed.

## 8. Theme set

- **6 bundled palettes**, spanning ROYGBIV: **Red, Orange, Yellow, Green, Blue** (new) plus the
  existing **M3 baseline purple** covering the Indigo·Violet end.
- The 5 new palette JSONs are **M3 Theme Builder exports** — the operator supplies them when a
  build story asks. The current exports are acknowledged as rushed-but-adequate for building
  the Showcase and may be tightened later.
- JSON lives alongside `baseline.json` under the theme `palettes/` resource directory
  (`src/main/resources/com/owspfm/elwha/theme/palettes/`).
- The demo palettes **ship in the published jar**, positioned as starter "Themes" master
  colors. Consumers can still ship their own palettes via `PaletteLoader` — the bundled set is
  a starting point, not a constraint.
- **Picker behavior:** a combo / segmented control in the header next to the mode toggle.
  Selecting a palette re-installs the theme via the same `ElwhaTheme.install` re-call mechanism
  the light/dark toggle already uses (`PaletteLoader.loadTheme()` already loads arbitrary
  classpath resources — no loader change). `MaterialPalettes` gains a bundled-set accessor; a
  build story confirms whether any convenience API on `ElwhaTheme` is warranted.
- **Palette doctrine shift.** `CLAUDE.md` currently states Elwha ships only `baseline.json`
  ("the token vocabulary is Elwha's; the palette values are the consumer's"). This epic
  intentionally bundles demo themes. The relevant `CLAUDE.md` line is to be updated when the
  theme story lands.

**Amendment — Primary / Secondary tiers ([#153](https://github.com/OWS-PFMS/elwha/issues/153)).**
The "6 bundled palettes" framing above is superseded. The bundled set is now two
directory-derived tiers: a **primary** tier (the curated baseline + ROYGBIV set, under
`theme/palettes/primary/`) and an additive **secondary** tier of 10 M3 Theme Builder palettes
(`theme/palettes/secondary/`) — the colors not already in the primary tier, so the two tiers are
disjoint. The header carries a `Primary | Secondary` tier switcher (an
`ElwhaChipList` in `SINGLE_MANDATORY` mode); the picker shows one tier at a time, ordered
spectrally by primary-role hue with neutral-family palettes (grey / brown) last. `MaterialPalettes`
exposes `primary()` / `secondary()` in place of the former flat `bundled()`. Each tier stays
directory-derived — a palette JSON dropped into a tier's subdirectory surfaces with no code change.

## 9. Epic #130 story breakdown

To be filed as sub-issues of #130, milestone `v0.3.0`. Order is a recommendation; finalize at
filing time.

- **#131 — Design** (this story). Done.
- **Story 2 — Showcase shell.** `ElwhaShowcase` main class, sidebar nav, header bar, mode
  toggle ported. Foundations / Components / Containers wired with the existing panels
  reorganized into the new IA (no refinement). Ships a working unified playground.
- **Story 3 — Multi-theme palettes & picker.** 5 new palette JSON resources, `MaterialPalettes`
  bundled-set API, the header palette picker, runtime palette swap. Update the `CLAUDE.md`
  palette doctrine line.
- **Story 4 — Shared Workbench scaffolds.** Generalize `SnippetPanel` into the shared code-view
  widget; build the canonical Component Workbench and Container Workbench scaffolds.
- **Stories 5–9 — Per-component refine.** One story each for **Button, Chip, Icon Button,
  Surface, Card** — clean and refine that component's Workbench + Gallery to the "pro" bar with
  every option wired. Chip's story also builds its net-new single-instance Workbench (§7). Card
  is the largest: media slot (image / rendered / none), header slot (icon / avatar / none),
  body slots, actions, collapse, disabled — every axis a control.
- Container refine (Chip List, Card List, Button / Icon Button groups → Container Workbench)
  may fold into the per-component refine stories or stand alone — decide at filing time.

## 10. Open / deferred items

- Retiring the standalone playgrounds — a post-epic question, not decided here.
- Tightening the rushed palette exports — operator may revisit after the Showcase is built.
- The optional Foundations "Icons" gallery — flagged in §7, not committed.
- Screenshot / web visual-regression tooling — explicitly out of scope per epic #130.

## 11. Process

This note formalizes the design parked during the design session while #60 (the
`src/main/java` source-layout migration) was in flight. #60 has landed; this note now lives in
`docs/research/`. Next: file the §9 sub-issues of #130.
