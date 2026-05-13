# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## What this repo is

**FlatComp** — a Swing component library built on [FlatLaf](https://www.formdev.com/flatlaf/) providing `FlatCard`, `FlatChip`, their list containers, a shared `FlatList<T>` contract, and a `MaterialIcons` helper. Apache 2.0, JDK 21, currently `0.1.0` (pre-1.0 — API not stable).

`groupId = com.owspfm`, `artifactId = flatcomp`. Single maintainer: Charles Bryan (`cfb3@uw.edu`). GitHub org: `OWS-PFMS`.

**Provenance:** extracted from [OWS-PFMS/OWS-Local-Search-GUI](https://github.com/OWS-PFMS/OWS-Local-Search-GUI) on 2026-05-12 via epic #231 with `git filter-repo --subdirectory-filter` so pre-extraction history is preserved. Rationale and the coupling audit that confirmed extraction-readiness live in `docs/research/flatcomp-extraction-decisions.md` and `docs/research/flatcomp-coupling-audit.md` — **read those before making architectural decisions; don't re-litigate them.**

## Build & run

```bash
mvn clean package                                                    # → target/flatcomp-<version>.jar + sources + javadoc jars
mvn compile exec:java -Dexec.mainClass="com.owspfm.ui.components.chip.FlatChipPlayground"
mvn compile exec:java -Dexec.mainClass="com.owspfm.ui.components.card.playground.FlatCardListShowcase"
```

**No tests yet.** The lib has zero JUnit tests today — components are validated visually via the two playground apps above. There's no test infrastructure to lean on; if you change behavior, exercise the playground.

## Source layout (non-standard)

`pom.xml` sets `<sourceDirectory>src</sourceDirectory>`, so Java sources live at `src/com/owspfm/...` rather than `src/main/java/com/owspfm/...`. Resources still follow the Maven default at `src/main/resources/`. **This layout is deliberate** — it preserves filename parity with the OWS-tool source the lib was extracted from, keeping `git blame` archeology useful across the two repos. Don't "fix" it.

Component packages under `src/com/owspfm/ui/components/`:

| Package | What it is |
|---|---|
| `card/` | `FlatCard` primitive + variants (`FILLED` / `OUTLINED` / `GHOST` / `WARM_ACCENT`) + interaction modes |
| `card/list/` | `FlatCardList<T>` + `CardListModel` + `CardSelectionModel` + drag-handle cursor PNGs |
| `card/playground/` | `FlatCardListShowcase` interactive demo |
| `chip/` | `FlatChip` + variants + interaction modes |
| `chip/list/` | `FlatChipList<T>` + `ChipListModel` + `ChipSelectionModel` (including `SINGLE_MANDATORY` tab-strip semantics) + `MovementMode` + `IconAffordance` |
| `flatlist/` | `FlatList<T>` — the narrow cross-cutting interface both list families implement (orientation / gap / padding / empty / loading / filter / sort). **Does not include selection or drag-reorder by design** — those are family-specific today (see epic #252) |
| `icons/` | `MaterialIcons` — wraps `FlatSVGIcon` over 17 bundled Material Symbols (Rounded / 400 / fill0 / 20px), auto-themed via a shared `Label.foreground` color filter |

Bundled resources:
- `src/main/resources/com/owspfm/ui/components/card/list/cursors/` — grab / grabbing cursors, light + dark, 16/32px (Capitaine, CC BY-SA 4.0; attribution in `NOTICE`)
- `src/main/resources/com/owspfm/icons/material/` — 17 Material Symbol SVGs (Apache 2.0 from Google; attribution in `NOTICE`)

## Coupling stance (defend this)

The pre-extraction audit confirmed **zero coupling sites** between these components and the OWS-tool app they came from. Keep it that way: depend on **Swing + FlatLaf only**, never on app-specific code, domain types, or consumer assumptions. The transitive deps in `pom.xml` are `flatlaf`, `flatlaf-extras`, `flatlaf-intellij-themes` — don't add to that list without a strong reason.

## Conventions

- **Code style:** Google Java Style, enforced via Spotless (`googleJavaFormat`) and Checkstyle. Run `mvn spotless:apply` to fix formatting; `mvn verify` runs both checkers. No `my*` / `the*` identifier prefixes — qualify with `this.` when a parameter shadows a field. Full convention: `docs/development/code-style.md`.
- **Javadoc:** every public class / method has `@author`, `@version`, `@since`. `@version` is bumped on every change that touches the entity — **the `validate-versions` workflow runs `scripts/update_javadoc_version.py --check --changed-only` against the PR's milestone and hard-fails on missed bumps.** `@since` is set once and never moves. Bump `@version` in the same commit as the code change. Full convention: `docs/development/versioning.md`; playbook: `docs/development/versioning-playbook.md`.
- **Commits:** Conventional Commits (`feat:`, `fix:`, `refactor:`, `docs:`, `chore:`, `ci:`, `test:`).
- **Semver:** `0.x.y` pre-1.0 — minor bumps may break API, patch is bug-fix only. Document every break under `## [Unreleased]` in `CHANGELOG.md` (policy: `docs/development/changelog-policy.md`).
- **Milestones:** `v0.N.0` — no `-alpha` / `-beta` suffix. Set on every PR before review; the `validate-versions` workflow reads the milestone title and uses it as the expected `@version` value.
- **No backwards-compat shims pre-1.0.** Format-breaking changes are free until 1.0.0; don't add deprecation layers or legacy aliases.
- **No code comments by default.** Add one only when the *why* is non-obvious (hidden constraint, subtle invariant, workaround). Don't explain *what* — identifiers cover that.
- **Material Symbols icon house style:** Rounded / weight 400 / fill 0 / 20px. Override only when fill1 is semantically needed for a "selected/active" state. Source from `gstatic` for crisp variants. Use `MaterialIcons` helper, not raw `FlatSVGIcon`.
- **JDK target:** 21 (`maven.compiler.release=21`). Stays at 21 until OWS-tool migrates off 21 — bumping prematurely cuts off consumers.
- **PRs need a milestone at creation.** The Validate `@version` workflow hard-fails without one.
- **Branch protection:** `main` requires `build`, `Validate @version and @since tags`, `Validate formatting (Spotless)`, and `Validate naming (Checkstyle)`; force-push and deletion are blocked.

## Release process

Tag-driven publish to GitHub Packages (`.github/workflows/publish.yml` does the validation + publish):

1. Bump `<version>` in `pom.xml`
2. Move `[Unreleased]` → `[X.Y.Z]` in `CHANGELOG.md`
3. `git commit -m "chore: release X.Y.Z"`
4. `git tag -a vX.Y.Z -m "Release X.Y.Z"`
5. `git push origin vX.Y.Z`

The workflow validates the version matches `CHANGELOG.md` and publishes the jar + sources + javadoc to `https://maven.pkg.github.com/OWS-PFMS/flatcomp`.

## Open epics — expect breaking changes until these land

`FlatPill` → `FlatChip` (epic [#27](https://github.com/OWS-PFMS/flatcomp/issues/27) — mirrored from the closed OWS-Local-Search-GUI epic #251) is **complete in the lib** once the rename PRs land; consumer-side migration is tracked in **OWS-PFMS/OWS-Local-Search-GUI#258**. The remaining open epics are queued and still filed on the consumer repo (the lib didn't exist when they were opened). They'll move here over time. **1.0.0 ships only after both complete.**

- **OWS-Local-Search-GUI#252 — Extend `FlatList<T>`** to share selection + drag-reorder surface across both list families. Today the surface is family-specific (`CardSelectionMode` vs `ChipSelectionMode`, `CardSelectionModel` vs `ChipSelectionModel`); the chip side has richer semantics (`SINGLE_MANDATORY`, toggleable `SINGLE`, deferred drag-vs-click) that the card side lacks.
- **OWS-Local-Search-GUI#253 — `FlatCard` V2 API** replacing accumulated escape-hatches (raw label getters, `setSurfaceColor` bolt-on, `setKeepSummaryWhenExpanded`, `setHeader` overload pattern).

Org-level project board tracking this work: **Project #5 — Material Flat Component Library** at `https://github.com/orgs/OWS-PFMS/projects/5`.

Known consumer: **OWS-PFMS/OWS-Local-Search-GUI** (via PR #266 swap-out). That repo's issues #243 / #244 are the OWS-tool-side migration of `FactorPill` / `InnerViewTabStrip` onto `FlatChip` (originally written against the pre-rename `FlatPill` name).

## Working-style preferences (from the operator)

These carry forward unless overridden in conversation:

- **Clarifying questions in chat as plain-text numbered lists.** Don't use UI pickers.
- **Don't auto-merge PRs.** After CI green, hand off — merge only on explicit go.
- **Preserve `Closes #N` keywords** in squash-merge commit bodies. Custom `--body` flags can overwrite the PR description; sub-issues silently stay open otherwise.
- **No interactive Git commands** (`-i` flags) — no rebase -i, no add -i.
- **Set the PR milestone at creation** (required for the `@version` validation workflow).
