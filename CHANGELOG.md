# Changelog

All notable changes to FlatComp are documented in this file.

The format is based on [Keep a Changelog](https://keepachangelog.com/en/1.1.0/), and this project adheres to [Semantic Versioning](https://semver.org/spec/v2.0.0.html).

## [Unreleased]

### Changed

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
- **`FlatCardListShowcase`** — interactive playground for the card list family.
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
