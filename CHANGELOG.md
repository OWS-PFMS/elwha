# Changelog

All notable changes to FlatComp are documented in this file.

The format is based on [Keep a Changelog](https://keepachangelog.com/en/1.1.0/), and this project adheres to [Semantic Versioning](https://semver.org/spec/v2.0.0.html).

## [Unreleased]

### Changed

- **`FlatPill` renamed to `FlatChip`** ([epic #27](https://github.com/OWS-PFMS/flatcomp/issues/27)) — aligns the component name with Material Design's chip taxonomy. **Breaking change** for any consumer importing the old names, even though no consumer has shipped yet against this lib. The rename is mechanical and 1:1; no behavior changes.
    - **Package:** `com.owspfm.ui.components.pill` → `com.owspfm.ui.components.chip`; `pill.list` → `chip.list`.
    - **Classes:** every `FlatPill*` and `Pill*` type renamed (`FlatPill` → `FlatChip`, `FlatPillList` → `FlatChipList`, `FlatPillPlayground` → `FlatChipPlayground`, `FlatPillDemo` → `FlatChipDemo`, `PillVariant` → `ChipVariant`, `PillInteractionMode` → `ChipInteractionMode`, `PillListModel` / `DefaultPillListModel` → `ChipListModel` / `DefaultChipListModel`, `PillSelectionModel` / `DefaultPillSelectionModel` → `ChipSelectionModel` / `DefaultChipSelectionModel`, `PillSelectionMode` → `ChipSelectionMode`, `PillSelectionEvent` / `PillSelectionListener` → `ChipSelectionEvent` / `ChipSelectionListener`, `PillAdapter` → `ChipAdapter`, `PillReorderEvent` / `PillReorderListener` → `ChipReorderEvent` / `ChipReorderListener`, `PillListDataEvent` / `PillListDataListener` → `ChipListDataEvent` / `ChipListDataListener`).
    - **UIManager keys:** every `"FlatPill.*"` string value renamed to `"FlatChip.*"` (`FlatChip.background`, `FlatChip.hoverBackground`, `FlatChip.padding`, `FlatChip.arc`, `FlatChip.warmAccent`, `FlatChip.foreground`, …). Java constant names (`K_*`, `STYLE_PROPERTY`) are unchanged; only the string values move. The client-property key `"FlatPill.style"` is now `"FlatChip.style"`, and the ad-hoc `"FlatPill.removeIcon"` override slot used by the playground is now `"FlatChip.removeIcon"`.
    - **Consumer migration:** any consumer that previously overrode `UIManager.put("FlatPill.*", ...)` at app startup must update its overrides to the `FlatChip.*` namespace. Imports of `com.owspfm.ui.components.pill.*` must update to `com.owspfm.ui.components.chip.*`. Tracked downstream in [OWS-PFMS/OWS-Local-Search-GUI#258](https://github.com/OWS-PFMS/OWS-Local-Search-GUI/issues/258).
    - **History:** the component was originally named `FlatPill` because its shape is a capsule ("pill"); the Material industry-standard name is "chip" for interactive token elements (input / choice / filter / action chips). Pre-flight inventory of every renamed touchpoint lives at `docs/research/flatpill-to-flatchip-rename-inventory.md`.
- **Naming convention switched to Google Java Style** ([epic #5](https://github.com/OWS-PFMS/flatcomp/issues/5)). Every `my*`-prefixed instance field and every `the*`-prefixed parameter / local was renamed to its unprefixed equivalent across the source tree. Public API parameter names move with this change — Java does not bind to parameter names at the call site, but Javadoc `@param` references and consumers reading reflection metadata will see the new names. No method signatures, class names, package names, or behavior changed.

### Tooling

- **Spotless** (`spotless-maven-plugin` 2.46.1 + `googleJavaFormat`) wired into the Maven build as `mvn spotless:check` at the `verify` phase. Run `mvn spotless:apply` locally to fix formatting. Note: requires JDK 21 — Spotless + google-java-format hits a binary-API drift on JDK 22+ (tracked upstream at [diffplug/spotless#2468](https://github.com/diffplug/spotless/issues/2468)).
- **Checkstyle** (`maven-checkstyle-plugin` 3.6.0 + Checkstyle 10.21.0) wired with a project-local config (`config/checkstyle/checkstyle.xml`) that enforces Google naming patterns plus a project-specific ban on `my*` / `the*` identifier prefixes. Currently in **advisory mode**; will flip to fail-on-violation in [#9](https://github.com/OWS-PFMS/flatcomp/issues/9).

## [0.1.0] — 2026-05-12

Initial release. Library extracted from the [OWS-Local-Search-GUI](https://github.com/OWS-PFMS/OWS-Local-Search-GUI) project ([epic #231](https://github.com/OWS-PFMS/OWS-Local-Search-GUI/issues/231)).

### Added

- **`FlatCard`** — theme-aware card primitive with header, body, surface variants, hover/pressed/selected states, optional collapse/expand, leading icon, trailing actions.
- **`FlatCardList<T>`** — list of `FlatCard` items: selection, drag-to-reorder, filter, sort, orientation modes (vertical / horizontal / wrap / grid).
- **`FlatPill`** — compact pill primitive: text + optional leading icon + optional trailing action button, themeable via UIManager keys (`FlatPill.*`), auto-contrast foreground via BT.601 luma.
- **`FlatPillList<T>`** — list of `FlatPill` items with selection modes (`NONE` / `SINGLE` / `SINGLE_MANDATORY` / `MULTIPLE`), drag-to-reorder, pinned-partition + anchored modes (`MovementMode` enum), icon affordances (`IconAffordance` enum).
- **`FlatList<T>`** — shared cross-cutting interface implemented by both `FlatCardList` and `FlatPillList` (orientation, gap, padding, empty / loading state, filter, sort).
- **`MaterialIcons`** — helper that exposes 17 Material Symbols SVGs (Rounded / 400 / fill0 / 20px) via `FlatSVGIcon` with a theme-aware color filter.
- **`FlatCardPlayground`** — interactive playground for the card family (embeds `FlatCardListShowcase` + `GalleryPanel` panels for the list view, single-card variants, and cycle/cursor examples).
- **`FlatPillPlayground`** — interactive playground for the pill list family, including a LAF tweak panel for live UIManager-key experimentation.

### Notes

- API is **not yet stable** at 0.1.0. Breaking changes between minor versions are expected until 1.0.0.
- Two breaking-change refactors are tracked and gated on this release:
  - `FlatPill` → `FlatChip` rename (taxonomy alignment with Material)
  - `FlatList<T>` extension to share selection + drag-reorder surface across both list families
- 1.0.0 will follow those landings.

### Source history

Pre-extraction commit history has been preserved via `git filter-repo --subdirectory-filter`. The components evolved through six OWS-Local-Search-GUI PRs:

- [#153](https://github.com/OWS-PFMS/OWS-Local-Search-GUI/pull/153) — `FlatCard` + playground
- [#158](https://github.com/OWS-PFMS/OWS-Local-Search-GUI/pull/158) — `FlatCardList`
- [#170](https://github.com/OWS-PFMS/OWS-Local-Search-GUI/pull/170) — cycle-viewer integration (drove `FlatCard` evolution)
- [#175](https://github.com/OWS-PFMS/OWS-Local-Search-GUI/pull/175) — cycle-card polish (further `FlatCard` evolution)
- [#179](https://github.com/OWS-PFMS/OWS-Local-Search-GUI/pull/179) — multi-factor selection (drove `FlatCard` selection surface)
- [#250](https://github.com/OWS-PFMS/OWS-Local-Search-GUI/pull/250) — `FlatPill` + `FlatPillList` epic

Cross-reference these via `git log` in this repo — file paths and blame archeology are preserved.

[Unreleased]: https://github.com/OWS-PFMS/flatcomp/compare/v0.1.0...HEAD
[0.1.0]: https://github.com/OWS-PFMS/flatcomp/releases/tag/v0.1.0
