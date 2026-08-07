# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## What this repo is

**Elwha** — a Swing component library built on [FlatLaf](https://www.formdev.com/flatlaf/), implementing **Material 3 Expressive** as a design system for desktop Java: a design-token foundation plus ~25 components and the containers, overlays, and anchors around them. Apache 2.0, JDK 21. API is not stable pre-1.0.

**Version, carefully:** `pom.xml` reads `0.1.0`, which is the last *published* release (2026-05-12, the only tag). It is not a description of `main`. `v0.2.0` was cancelled outright, `v0.3.0` / `v0.4.0` were planning waves that were never cut, and **1.0.0 is the next publish** — and OWS's initial Elwha release. Treat `main` as several unreleased waves ahead of the version string. See *Milestones* under Conventions and *The road to 1.0.0* below.

`groupId = com.owspfm`, `artifactId = elwha`. Single maintainer: Charles Bryan (`cfb3@uw.edu`). GitHub org: `OWS-PFMS`.

**Provenance:** extracted from [OWS-PFMS/OWS-Local-Search-GUI](https://github.com/OWS-PFMS/OWS-Local-Search-GUI) on 2026-05-12 via epic #231 with `git filter-repo --subdirectory-filter` so pre-extraction history is preserved. Rationale and the coupling audit that confirmed extraction-readiness live in `docs/research/elwha-extraction-decisions.md` and `docs/research/elwha-coupling-audit.md` — **read those before making architectural decisions; don't re-litigate them.**

## Build & run

```bash
mvn clean package                                                    # → target/elwha-<version>.jar + sources + javadoc jars

# The Elwha Showcase — the primary visual harness; every shipped component has a leaf
mvn compile exec:java -Dexec.mainClass="com.owspfm.elwha.showcase.ElwhaShowcase"

# Component playgrounds — ~30 more live under <component>/playground/
mvn compile exec:java -Dexec.mainClass="com.owspfm.elwha.chip.ElwhaChipPlayground"
mvn compile exec:java -Dexec.mainClass="com.owspfm.elwha.card.playground.ElwhaCardPlayground"
mvn compile exec:java -Dexec.mainClass="com.owspfm.elwha.theme.playground.ThemePlayground"
```

**No tests yet.** The lib has zero JUnit tests and no `src/test` tree. Validation is three things, none of them automated in CI: **The Elwha Showcase** (the storefront — one leaf per component, and the first place to check a change), the per-component **playgrounds**, and the per-story headless **`*Smoke` / `*Guard` mains** that each epic left behind. Those smokes prove the story that shipped them; they are not a maintained suite and nothing runs them together. Building a real regression suite is epic **#438**, which natively blocks the lib-level review **#440**. Until it lands: if you change behavior, exercise the Showcase leaf and the component's smokes by hand.

### The build runs on JDK 21 — not optional

All four CI workflows pin temurin 21, so a local build on anything else is testing something other than what gates merges. Concretely, Spotless' google-java-format (1.27.0, the version Spotless 2.46.1 resolves) calls javac internals, and on **JDK 25** it dies with `NoSuchMethodError: Log$DeferredDiagnosticHandler.getDiagnostics()`. That's a *signature* change, not module encapsulation — `--add-exports` in `.mvn/jvm.config` cannot fix it. Plain `mvn compile` works on any JDK; only the Spotless step breaks, which makes the failure look unrelated to your change.

`.envrc` handles this: with [direnv](https://direnv.net) installed (`brew install direnv` + `eval "$(direnv hook zsh)"` last in your shell rc), entering the repo exports `JAVA_HOME` for JDK 21 and leaving reverts it, so a newer JDK can stay your global default. First time in a fresh clone, run `direnv allow .`.

Without direnv, prefix Maven invocations manually:

```bash
JAVA_HOME=$(/usr/libexec/java_home -v 21) mvn verify
```

**Agents and scripts:** direnv hooks fire on shell prompt / `chpwd`, so non-interactive shells do **not** pick this up. Export `JAVA_HOME` explicitly or wrap with `direnv exec . mvn …`.

This is independent of `maven.compiler.release=21`, which governs bytecode output and holds regardless of which JVM runs Maven — see the JDK target convention below.

## Source layout

Java sources follow the standard Maven layout: code under `src/main/java/com/owspfm/...`, bundled resources under `src/main/resources/com/owspfm/...`. `pom.xml` uses Maven's default `<sourceDirectory>` — no override. (The tree was migrated from a flat `src/` layout in [#60](https://github.com/OWS-PFMS/elwha/issues/60); `git log --follow` traverses the move, so blame is preserved.)

Component packages under `src/main/java/com/owspfm/elwha/`:

Nearly every component package also carries `*Demo` / `*Smoke` / `*Guard` mains and a `playground/` subpackage; those are story-time artifacts, not API, and are excluded from the test-suite (#438), review (#440), and Javadoc (#529) epics.

**Foundation**

| Package | What it is |
|---|---|
| `theme/` | The design-token foundation (epic #30): facade enums (`ColorRole` / `ShapeScale` / `SpaceScale` / `TypeRole` / `StateLayer`), the `ElwhaTheme` static install API (`Palette` / `Theme` / `Mode` / `Typography` / `Config`), `FlatLafKeyMapping` (the curated FlatLaf-native key → role bridge), `MaterialPalettes` (`baseline()` + the directory-discovered `primary()` / `secondary()` tier sets) + `PaletteLoader`, the bundled Inter font, and shared paint/motion helpers (`RipplePainter`, `ShapeMorphPainter`, `ContentMorphPainter`, `ScrollSourceBinding`). `TypeRole` carries the full M3 15-role scale incl. `DISPLAY_*` |
| `surface/` | `ElwhaSurface` — the rounded, token-resolved painted panel most other components extend |
| `icons/` | `MaterialIcons` — wraps `FlatSVGIcon` over the bundled Material Symbols (Rounded / 400 / fill 0; 20-dp optical-size axis, rendered at 24px by default with sized overloads), auto-themed via a shared `Label.foreground` color filter |

**Actions**

| Package | What it is |
|---|---|
| `button/` | `ElwhaButton` — variants, sizes, shape morph, ripple |
| `iconbutton/` | `ElwhaIconButton` — per-instance variants, toggle icon swap; implements `IconBearing` |
| `buttongroup/` | `ElwhaButtonGroup` — M3 Expressive button group (connected/positional treatment) |
| `fab/` | `ElwhaFab` (standard + extended, shape morph) and `ElwhaFabAnchor` — the wrapper container that floats a FAB over content, scroll-aware |

**Selection controls**

| Package | What it is |
|---|---|
| `checkbox/` | `ElwhaCheckbox` — tri-state, hand-stroked marks |
| `radio/` | `ElwhaRadioButton` — roving tab stop, press-swap; matches the checkbox label/geometry/focus contract |
| `switches/` | `ElwhaSwitch` — M3 switch with icon modes |
| `chip/` | `ElwhaChip` + variants + interaction modes |

**Fields**

| Package | What it is |
|---|---|
| `textfield/` | `ElwhaTextField` — filled/outlined, floating label, supporting text, counter, multiline |
| `selectfield/` | `ElwhaSelectField<T>` — typed combo over `ElwhaMenu`; editable and multi-select modes (mutually exclusive) |
| `slider/` | `ElwhaSlider` — one `JComponent` over `BoundedRangeModel` (**not** `JSlider`/`SliderUI`); standard / centered / range, sizes, horizontal + vertical |
| `colorpicker/` | `ElwhaColorPicker` + `ElwhaColorPickerDialog` / `ElwhaColorPickerPopover` — spectrum, wheel, sliders, swatch tiers, eyedropper |

**Containers & surfaces**

| Package | What it is |
|---|---|
| `card/` | **V3** `ElwhaCard` — chrome-only primitive extending `ElwhaSurface`, plus the composition primitives added via `card.add(...)`: `ElwhaCardTitle` / `Subtitle` / `SupportingText` / `LeadingIcon` / `Thumbnail` / `Header` / `Media` / `Actions` / `Divider` / `Chevron` / `ExpandLink`. Variants `ELEVATED` / `FILLED` / `OUTLINED`. Architecture and token bindings: `docs/research/elwha-card-v3-spec.md` |
| `card/list/` | `ElwhaCardList<T>` + `CardListModel` + `CardSelectionModel` |
| `chip/list/` | `ElwhaChipList<T>` + `ChipListModel` + `ChipSelectionModel` (incl. `SINGLE_MANDATORY` tab-strip semantics) + `MovementMode` + `IconAffordance` |
| `list/` | `ElwhaList<T>` — the narrow cross-cutting interface both list families implement (orientation / gap / padding / empty / loading / filter / sort). **Does not include selection or drag-reorder by design** — those are family-specific today; unifying them is epic **#67** |
| `sidesheet/` | `ElwhaSideSheet` — `DOCKED` / `DETACHED` postures, modal + standard, opt-in drag gestures |

**Overlays**

| Package | What it is |
|---|---|
| `overlay/` | `AbstractElwhaOverlay` — the shared layered-pane host every overlay component builds on (placement, focus, dismissal, parent–child chaining) |
| `dialog/` | `AbstractElwhaDialog` + `ElwhaDialog` (M3 basic) + `ElwhaFullScreenDialog` |
| `menu/` | `ElwhaMenu` + `ElwhaSubMenuItem` — Expressive vertical menu, selection modes, submenu chain, hover-driven corner morph |
| `tooltip/` | `ElwhaTooltip` — role-colored M3 tooltip with passive-focus opt-out |

**Navigation**

| Package | What it is |
|---|---|
| `appbar/` | `ElwhaAppBar` — `SMALL` / `MEDIUM_FLEXIBLE` / `LARGE_FLEXIBLE`, tonal lift, scroll-driven collapse |
| `navrail/` | `ElwhaNavigationRail` + `ElwhaNavRailDestination` — collapsed/expanded; the rail owns lateral nav (see the ☰ note in `docs/research/elwha-appbar-design.md`) |
| `tabs/` | `ElwhaTabs` + `ElwhaTab` — M3 tab bar, fixed + scrollable, indicator overlay |

**Feedback**

| Package | What it is |
|---|---|
| `badge/` | `ElwhaBadge` + `ElwhaBadgeAnchor` (+ the `IconBearing` contract) — dot/count overlay attached to a host component |
| `progress/` | `AbstractElwhaProgressIndicator` → `ElwhaLinearProgressIndicator` / `ElwhaCircularProgressIndicator`, incl. the wavy Expressive tracks |
| `loading/` | `ElwhaLoadingIndicator` — the M3 shape-morph spinner (distinct from `progress/`) |

**Legacy & harness**

| Package | What it is |
|---|---|
| `card/v1/` | The pre-Elwha-theme card extracted from OWS. **Frozen, not a design reference.** Deleted before 1.0.0 by **#96** — but note the V3 list currently imports its `Cursors` class (**#531**) |
| `card/fixes/` | Diagnostic harnesses for historical card bugs. Advisory-only in reviews |
| `showcase/` | `ElwhaShowcase` — the storefront, plus `ComponentWorkbench` / `ContainerWorkbench` / `CodeView` and the per-component `*ShowcasePanels`. Review/reorder pass is **#441**; dogfood sweep is **#424** |

Bundled resources:
- `src/main/resources/com/owspfm/elwha/card/v1/list/cursors/` — grab / grabbing cursors, light + dark, 16/32px (Capitaine). **The license is in dispute in our own metadata** — the bundled `LICENSE-capitaine.txt` is LGPL-3.0 while `NOTICE` claims CC BY-SA 4.0, and `NOTICE`'s path predates the #83 rename. Tracked in **#531**; don't restate a license for these assets until it resolves. Note the path: they live under `card/v1/`, and the V3 list still imports the V1 `Cursors` class
- `src/main/resources/com/owspfm/icons/material/` — 73 Material Symbol SVGs (Apache 2.0 from Google; attribution in `NOTICE`)
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
- **Milestones:** the title is consumed **verbatim** as the expected `@version` — `validate-versions` passes it straight to `scripts/update_javadoc_version.py --expected`, which string-compares it against the tag. So every milestone title must be a real version string (`vX.Y.Z`, no `-alpha` / `-beta` suffix, and never a label like `v1.x` or `backlog`). Set one on every PR before review. Three are live: **`v0.5.0`** the current wave, **`v1.0.0`** the freeze + first publish, **`v1.1.0`** the post-1.0 parking lot for deliberately deferred epics. `v0.1.0`–`v0.4.0` are closed; **`v0.4.0` is unreachable, not merely stale** — `@version` never moves backwards, and files on `main` already carry `v0.5.0`, so a PR milestoned `v0.4.0` fails the gate outright. Note `mvn verify` does **not** run this gate (it's a workflow, not a Maven plugin); check it locally with `python3 scripts/update_javadoc_version.py --check --changed-only --expected v0.5.0`.
- **No backwards-compat shims pre-1.0.** Format-breaking changes are free until 1.0.0; don't add deprecation layers or legacy aliases.
- **No code comments by default.** Add one only when the *why* is non-obvious (hidden constraint, subtle invariant, workaround). Don't explain *what* — identifiers cover that.
- **Material Symbols icon house style:** Rounded / weight 400 / fill 0 / 20px. Override only when fill1 is semantically needed for a "selected/active" state. Source from `gstatic` for crisp variants. Use `MaterialIcons` helper, not raw `FlatSVGIcon`.
- **JDK target:** 21 (`maven.compiler.release=21`). Stays at 21 until OWS-tool migrates off 21 — bumping prematurely cuts off consumers. This governs *bytecode output* and is unaffected by which JVM runs Maven; the separate requirement to *build* on JDK 21 is a Spotless constraint — see "The build runs on JDK 21" above.
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

## The road to 1.0.0 — expect breaking changes until these land

The component catalog is essentially complete; what stands between `main` and 1.0.0 is one refactor epic plus a self-imposed quality gate. **Two earlier framings are dead — don't work from them:** the old consumer-repo epics `OWS-Local-Search-GUI#252` / `#253` (absorbed by #67 and superseded by #80 respectively), and `docs/research/elwha-v1-component-scope.md`, which never locked and is marked superseded.

**On the `v1.0.0` milestone:**

- **[#67](https://github.com/OWS-PFMS/elwha/issues/67) — `ElwhaItemList<T>`.** Collapse the two parallel list families into one generic implementation behind the existing `ElwhaList<T>` interface. Absorbs `OWS-Local-Search-GUI#252`. Locked principle: `max(funcA, funcB)` — where card and chip differ, the richer side wins, no feature loss. Stories #68 (spec) → #69 (build) → #70 (migrate + delete). **Not started; the spec is unwritten.** The last genuine architectural work before the freeze.
- **[#80](https://github.com/OWS-PFMS/elwha/issues/80) — Card V3.** Effectively **done** — #81–#95 all closed, V3 is what `card/` ships. Remaining tail: **#96** (delete `card/v1`; gated on the `Cursors` question in #531) and **#97** (the release chore).
- **[#530](https://github.com/OWS-PFMS/elwha/issues/530) — consumer adoption & publishing notes.** Install/auth, a Quick start that compiles, theming guide, component index, stability policy. Owns the README work #97 used to claim.

**On `v0.5.0` — the pre-V1 quality gate:**

- **[#438](https://github.com/OWS-PFMS/elwha/issues/438) — regression test suite.** The big unknown: zero tests today across ~124k LOC. Only S1 (**#439**, framework research) is filed; every other story files after it. **Natively blocks #440 and #441.**
- **[#440](https://github.com/OWS-PFMS/elwha/issues/440) — full lib review.** Scan → file every finding → fix in batches, suite green per batch.
- **[#441](https://github.com/OWS-PFMS/elwha/issues/441) — Showcase review**, and **[#424](https://github.com/OWS-PFMS/elwha/issues/424) — dogfood sweep**.
- **[#529](https://github.com/OWS-PFMS/elwha/issues/529) — full Javadoc review.** Measured `-Xdoclint:all` baseline: 7 errors, 925 warnings, 580 missing-comment sites in real public API — invisible to the normal build because `pom.xml` sets both `<doclint>none</doclint>` and `<failOnError>false</failOnError>`. **The only pre-V1 quality epic not blocked by #438**, since comment-only work can't regress behavior.

**Completed renames, for context:** `FlatPill` → `ElwhaChip` (epic [#27](https://github.com/OWS-PFMS/elwha/issues/27)) is complete in the lib; `FlatComp` → `Elwha` (issue #42) executed 2026-05-15 in PR #44.

Org-level project board: **Project #5 — Material Flat Component Library** at `https://github.com/orgs/OWS-PFMS/projects/5`. **Every new issue gets added to it as part of filing.**

Known consumer: **OWS-PFMS/OWS-Local-Search-GUI** (via PR #266 swap-out); its issues #243 / #244 are the OWS-side `FactorPill` / `InnerViewTabStrip` migration onto `ElwhaChip`. Note that **1.0.0 is OWS's *initial* Elwha release** — it never adopted a published V1 card, so there is no consumer mid-migration to protect.

## Working-style preferences (from the operator)

These carry forward unless overridden in conversation:

- **Clarifying questions in chat as plain-text numbered lists.** Don't use UI pickers.
- **Don't auto-merge PRs.** After CI green, hand off — merge only on explicit go.
- **Preserve `Closes #N` keywords** in squash-merge commit bodies. Custom `--body` flags can overwrite the PR description; sub-issues silently stay open otherwise.
- **No interactive Git commands** (`-i` flags) — no rebase -i, no add -i.
- **Set the PR milestone at creation** (required for the `@version` validation workflow).
