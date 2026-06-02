# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## What this repo is

**Elwha** — a Swing component library built on [FlatLaf](https://www.formdev.com/flatlaf/) providing `ElwhaCard`, `ElwhaChip`, their list containers, a shared `ElwhaList<T>` contract, and a `MaterialIcons` helper. Apache 2.0, JDK 21, currently `0.1.0` (pre-1.0 — API not stable).

`groupId = com.owspfm`, `artifactId = elwha`. Single maintainer: Charles Bryan (`cfb3@uw.edu`). GitHub org: `OWS-PFMS`.

**Provenance:** extracted from [OWS-PFMS/OWS-Local-Search-GUI](https://github.com/OWS-PFMS/OWS-Local-Search-GUI) on 2026-05-12 via epic #231 with `git filter-repo --subdirectory-filter` so pre-extraction history is preserved. Rationale and the coupling audit that confirmed extraction-readiness live in `docs/research/elwha-extraction-decisions.md` and `docs/research/elwha-coupling-audit.md` — **read those before making architectural decisions; don't re-litigate them.**

## Build & run

```bash
mvn clean package                                                    # → target/elwha-<version>.jar + sources + javadoc jars
mvn compile exec:java -Dexec.mainClass="com.owspfm.elwha.chip.ElwhaChipPlayground"
mvn compile exec:java -Dexec.mainClass="com.owspfm.elwha.card.playground.ElwhaCardPlayground"
```

**No tests yet.** The lib has zero JUnit tests today — components are validated visually via the two playground apps above. There's no test infrastructure to lean on; if you change behavior, exercise the playground.

## Source layout

Java sources follow the standard Maven layout: code under `src/main/java/com/owspfm/...`, bundled resources under `src/main/resources/com/owspfm/...`. `pom.xml` uses Maven's default `<sourceDirectory>` — no override. (The tree was migrated from a flat `src/` layout in [#60](https://github.com/OWS-PFMS/elwha/issues/60); `git log --follow` traverses the move, so blame is preserved.)

Component packages under `src/main/java/com/owspfm/elwha/`:

| Package | What it is |
|---|---|
| `card/` | `ElwhaCard` primitive + variants (`FILLED` / `OUTLINED` / `GHOST` / `WARM_ACCENT`) + interaction modes |
| `card/list/` | `ElwhaCardList<T>` + `CardListModel` + `CardSelectionModel` + drag-handle cursor PNGs |
| `card/playground/` | `ElwhaCardPlayground` interactive demo (embeds `ElwhaCardListShowcase` + `GalleryPanel` panels) |
| `chip/` | `ElwhaChip` + variants + interaction modes |
| `chip/list/` | `ElwhaChipList<T>` + `ChipListModel` + `ChipSelectionModel` (including `SINGLE_MANDATORY` tab-strip semantics) + `MovementMode` + `IconAffordance` |
| `list/` | `ElwhaList<T>` — the narrow cross-cutting interface both list families implement (orientation / gap / padding / empty / loading / filter / sort). **Does not include selection or drag-reorder by design** — those are family-specific today (see epic #252) |
| `icons/` | `MaterialIcons` — wraps `FlatSVGIcon` over 17 bundled Material Symbols (Rounded / 400 / fill 0; 20-dp optical-size axis, rendered at 24px by default with sized overloads), auto-themed via a shared `Label.foreground` color filter |
| `theme/` | The design-token foundation (Epic #30): facade enums (`ColorRole` / `ShapeScale` / `SpaceScale` / `TypeRole` / `StateLayer`), the `ElwhaTheme` static install API (`Palette` / `Theme` / `Mode` / `Typography` / `Config`), `FlatLafKeyMapping` (the curated FlatLaf-native key → role bridge), `MaterialPalettes` (`baseline()` + the directory-discovered `primary()` / `secondary()` tier sets) + `PaletteLoader`, and the bundled Inter font |
| `theme/playground/` | `ThemePlayground` — visual harness for the token foundation (color swatches, type scale, components gallery + light/dark/system mode toggle) |

Bundled resources:
- `src/main/resources/com/owspfm/elwha/card/list/cursors/` — grab / grabbing cursors, light + dark, 16/32px (Capitaine, CC BY-SA 4.0; attribution in `NOTICE`)
- `src/main/resources/com/owspfm/icons/material/` — 17 Material Symbol SVGs (Apache 2.0 from Google; attribution in `NOTICE`)
- `src/main/resources/com/owspfm/elwha/theme/fonts/` — Inter Regular + Medium TTFs for `Typography.defaults()` (SIL OFL 1.1; attribution in `NOTICE`)
- `src/main/resources/com/owspfm/elwha/theme/palettes/` — bundled demo palettes in two directory-derived tiers: `primary/` (`baseline.json` — the M3 baseline scheme, `MaterialPalettes.baseline()` — plus the ROYGBIV set `red` / `orange` / `yellow` / `green` / `blue` / `indigo` / `deep-purple`) and `secondary/` (10 Material Theme Builder palettes — the colors not in the primary tier; the two tiers are disjoint). `MaterialPalettes.primary()` / `secondary()` discover every `*.json` in their subdirectory at runtime and return it in spectral (hue) order — directory-derived, not a hardcoded list, so dropping a new Elwha-format palette JSON into a tier surfaces it in The Elwha Showcase's palette picker with no code change. Raw M3 Theme Builder exports are archived under `docs/research/themes/`; `scripts/convert_mtb_palette.py` automates the M3-export → Elwha-palette conversion. Consumers may still ship their own palettes via `PaletteLoader`.

## Coupling stance (defend this)

The pre-extraction audit confirmed **zero coupling sites** between these components and the OWS-tool app they came from. Keep it that way: depend on **Swing + FlatLaf only**, never on app-specific code, domain types, or consumer assumptions. The transitive deps in `pom.xml` are `flatlaf`, `flatlaf-extras`, `flatlaf-intellij-themes` — don't add to that list without a strong reason.

## Conventions

- **Code style:** Google Java Style, enforced via Spotless (`googleJavaFormat`) and Checkstyle. Run `mvn spotless:apply` to fix formatting; `mvn verify` runs both checkers. No `my*` / `the*` identifier prefixes — qualify with `this.` when a parameter shadows a field. Full convention: `docs/development/code-style.md`.
- **Component API doctrine:** getter naming (`getX()` only — no `getEffectiveX()`), per-variant static factories, single-arg convenience constructors, border-role exposure rule, symmetric border-width getter/setter. Canonical source: `docs/development/component-api-conventions.md`. New components are expected to match.
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

The workflow validates the version matches `CHANGELOG.md` and publishes the jar + sources + javadoc to `https://maven.pkg.github.com/OWS-PFMS/elwha`.

## Open epics — expect breaking changes until these land

The `FlatPill` → `ElwhaChip` rename (epic [#27](https://github.com/OWS-PFMS/elwha/issues/27) — mirrored from the closed OWS-Local-Search-GUI epic #251) is **complete in the lib**; consumer-side migration is tracked in **OWS-PFMS/OWS-Local-Search-GUI#258**. The FlatComp → Elwha rename (issue #42) executed on 2026-05-15 in PR #44. The remaining open epics are queued and still filed on the consumer repo (the lib didn't exist when they were opened). They'll move here over time. **1.0.0 ships only after both complete.**

- **OWS-Local-Search-GUI#252 — Extend `ElwhaList<T>`** to share selection + drag-reorder surface across both list families. Today the surface is family-specific (`CardSelectionMode` vs `ChipSelectionMode`, `CardSelectionModel` vs `ChipSelectionModel`); the chip side has richer semantics (`SINGLE_MANDATORY`, toggleable `SINGLE`, deferred drag-vs-click) that the card side lacks.
- **OWS-Local-Search-GUI#253 — `ElwhaCard` next-gen API** replacing accumulated escape-hatches (raw label getters, `setSurfaceColor` bolt-on, `setKeepSummaryWhenExpanded`, `setHeader` overload pattern). Filed as "V2 API" pre-extraction; the lib has since shipped the **V3** Card architecture (`card/ElwhaCard.java`, "V3 onward"), with **V1** retained under `card/v1/` for the OWS regression and **V2 retired**. So this consumer epic is really the migration onto the current V3 card — not a fresh "V2".

Org-level project board tracking this work: **Project #5 — Material Flat Component Library** at `https://github.com/orgs/OWS-PFMS/projects/5`.

Known consumer: **OWS-PFMS/OWS-Local-Search-GUI** (via PR #266 swap-out). That repo's issues #243 / #244 are the OWS-tool-side migration of `FactorPill` / `InnerViewTabStrip` onto `ElwhaChip` (originally written against the pre-rename `FlatPill` name).

## Working-style preferences (from the operator)

These carry forward unless overridden in conversation:

- **Clarifying questions in chat as plain-text numbered lists.** Don't use UI pickers.
- **Don't auto-merge PRs.** After CI green, hand off — merge only on explicit go.
- **Preserve `Closes #N` keywords** in squash-merge commit bodies. Custom `--body` flags can overwrite the PR description; sub-issues silently stay open otherwise.
- **No interactive Git commands** (`-i` flags) — no rebase -i, no add -i.
- **Set the PR milestone at creation** (required for the `@version` validation workflow).
