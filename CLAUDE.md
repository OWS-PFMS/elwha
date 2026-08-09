# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## What this repo is

**Elwha** — a Swing component library built on [FlatLaf](https://www.formdev.com/flatlaf/), implementing **Material 3 Expressive** as a design system for desktop Java: a design-token foundation plus ~25 components and the containers, overlays, and anchors around them. Apache 2.0, JDK 21. ~126k LOC across 547 source files, with a ~4,570-test suite behind it.

**Version, carefully:** `pom.xml` reads `0.1.0`, which is the last *published* release (2026-05-12, the only tag). It is not a description of `main`. `v0.2.0` was cancelled outright, `v0.3.0` / `v0.4.0` were planning waves that were never cut, and **1.0.0 is the next publish** — and OWS's initial Elwha release. Treat `main` as four unreleased waves ahead of the version string; the number moves in the release commit itself, not before. 1.0.0 is also the API freeze: pre-1.0 the library broke API freely, and after the tag semver applies. See *Milestones* under Conventions, *Release process* below, and `docs/development/release-runbook.md`.

`groupId = com.owspfm`, `artifactId = elwha`. Single maintainer: Charles Bryan (`cfb3@uw.edu`). GitHub org: `OWS-PFMS`.

**Provenance:** extracted from [OWS-PFMS/OWS-Local-Search-GUI](https://github.com/OWS-PFMS/OWS-Local-Search-GUI) on 2026-05-12 via epic #231 with `git filter-repo --subdirectory-filter` so pre-extraction history is preserved. Rationale and the coupling audit that confirmed extraction-readiness live in `docs/research/elwha-extraction-decisions.md` and `docs/research/elwha-coupling-audit.md` — **read those before making architectural decisions; don't re-litigate them.**

## Build & run

```bash
mvn clean package                                                    # → target/elwha-<version>.jar + sources + javadoc jars
mvn verify                                                           # the above + the test suite + Spotless + Checkstyle

# The Elwha Showcase — the primary visual harness; every shipped component has a leaf
mvn compile exec:java -Dexec.mainClass="com.owspfm.elwha.showcase.ElwhaShowcase"

# Component playgrounds — 14 packages, ~79 mains, under <component>/playground/
mvn compile exec:java -Dexec.mainClass="com.owspfm.elwha.chip.ElwhaChipPlayground"
mvn compile exec:java -Dexec.mainClass="com.owspfm.elwha.card.playground.ElwhaCardPlayground"
mvn compile exec:java -Dexec.mainClass="com.owspfm.elwha.theme.playground.ThemePlayground"
```

### The build runs on JDK 21 — not optional

All six CI workflows (`build`, `test`, `validate-style`, `validate-versions`, `pages`, `publish`) pin temurin 21, so a local build on anything else is testing something other than what gates merges. Concretely, Spotless' google-java-format (1.27.0, the version Spotless 2.46.1 resolves) calls javac internals, and on **JDK 25** it dies with `NoSuchMethodError: Log$DeferredDiagnosticHandler.getDiagnostics()`. That's a *signature* change, not module encapsulation — `--add-exports` in `.mvn/jvm.config` cannot fix it. Plain `mvn compile` works on any JDK; only the Spotless step breaks, which makes the failure look unrelated to your change.

`.envrc` handles this: with [direnv](https://direnv.net) installed (`brew install direnv` + `eval "$(direnv hook zsh)"` last in your shell rc), entering the repo exports `JAVA_HOME` for JDK 21 and leaving reverts it, so a newer JDK can stay your global default. First time in a fresh clone, run `direnv allow .`.

Without direnv, prefix Maven invocations manually:

```bash
JAVA_HOME=$(/usr/libexec/java_home -v 21) mvn verify
```

**Agents and scripts:** direnv hooks fire on shell prompt / `chpwd`, so non-interactive shells do **not** pick this up. Export `JAVA_HOME` explicitly or wrap with `direnv exec . mvn …`.

This is independent of `maven.compiler.release=21`, which governs bytecode output and holds regardless of which JVM runs Maven — see the JDK target convention below.

## Tests

Two kinds of validation, and a behavior change generally wants both.

**The suite — `mvn verify`.** ~4,570 tests under `src/test/java/com/owspfm/elwha/`, one package per component, on JUnit 6 + AssertJ. Built in epic #438; it is a **required check on `main`** (`Test (components + Showcase)`) and runs in two tiers:

| | Tier A — headless, the default | Tier B — `@Tag("gui")` |
|---|---|---|
| Count | ~4,470 | ~100 |
| JVM | `java.awt.headless=true` | headless=false, its own forked JVM |
| Display | none — offscreen `BufferedImage` | Cacio virtual toolkit locally, native under Xvfb on Linux CI |
| Covers | construction, geometry, pixel probes in both modes, a11y shapes, event contracts, InputMap wiring, synthetic input dispatch | real focus *ownership*, `Robot` input, window realization, overlay placement, drag gestures |

Put a test in Tier B only when headless cannot represent what it asserts. The split is load-bearing — mixing Cacio with an already-initialized JDK toolkit segfaults — so the tiers run in separate surefire executions and separate JVMs. Shared fixtures live in `src/test/java/com/owspfm/elwha/testkit/` (`EdtInterceptor`, `ThemeExtension`, `Pixels`, `Input`, `WaitFor`, `GuiToolkit`, `GuiSteps`, `PaintLog`, `HeadlessHost`). **Read `docs/development/testing.md` before writing one** — its determinism rules (reduced motion pinned, assert resolved roles never hex, never probe pixels inside a glyph box, pace `Robot` input) are what holds the flake budget at zero. JaCoCo is report-only; there is no coverage threshold.

**By eye.** Nothing in the suite renders to a screen a human looks at, so a visual change still needs a human. **The Elwha Showcase** is the storefront — one leaf per component, grouped by the same families the package tables below use — and it is the first place to check anything visual. The per-component `playground/` packages go deeper on one component. The per-story `*Demo` / `*Smoke` / `*Guard` mains scattered through `src/main` are story-time artifacts: each proves the story that shipped it, they are not a maintained suite, and nothing runs them together. The one exception is the six `J*SweepGuard` classes in `showcase/`, which a real test now drives (#424).

## Source layout

Java sources follow the standard Maven layout: code under `src/main/java/com/owspfm/...`, bundled resources under `src/main/resources/com/owspfm/...`. `pom.xml` uses Maven's default `<sourceDirectory>` — no override. (The tree was migrated from a flat `src/` layout in [#60](https://github.com/OWS-PFMS/elwha/issues/60); `git log --follow` traverses the move, so blame is preserved.)

Component packages under `src/main/java/com/owspfm/elwha/`:

Nearly every component package also carries `*Demo` / `*Smoke` / `*Guard` mains and a `playground/` subpackage. Those are story-time artifacts, not API: the test-suite (#438), lib-review (#440) and Javadoc (#529) epics all scoped them out, and the tables below name only what a consumer imports.

**Foundation**

| Package | What it is |
|---|---|
| `theme/` | The design-token foundation (epic #30): the facade enums (`ColorRole` / `ShapeScale` / `SpaceScale` / `TypeRole` / `StateLayer`), the `ElwhaTheme` static install API (`Palette` / `Theme` / `Mode` / `Typography` / `Config` — all four value types now compare by value), `MaterialPalettes` (`baseline()` + the directory-discovered `primary()` / `secondary()` tier sets) + `PaletteLoader`, and the bundled Inter font. Also the shared paint/motion machinery the components draw on: `RipplePainter`, `ShadowPainter`, `SurfacePainter`, `ShapeMorphPainter`, `ContentMorphPainter`, `MorphAnimator`, `RetargetTween`, `RtlMirror`, `HoverTracker`, `FocusVisible`, `ScrollSourceBinding`, `ElwhaLayers`, `CornerRadii`, `Easing` — plus the two cross-cutting geometry contracts, `ShadowBearing` (`getShadowInsets()`) and `BodyBearing` (`getBodyRect()`, so overlays anchor to the painted body and not to stretched bounds). `TypeRole` carries the full M3 15-role scale incl. `DISPLAY_*`. FlatLaf-native keys are bridged to roles at install time by the package-private `FlatLafKeyMapping` — a mechanism, not a type you call |
| `surface/` | `ElwhaSurface` — the rounded, token-resolved painted panel most other components extend |
| `icons/` | `MaterialIcons` — wraps `FlatSVGIcon` over the bundled Material Symbols (Rounded / 400 / fill 0; 20-dp optical-size axis, rendered at 24px by default with sized overloads), auto-themed via a shared `Label.foreground` color filter |

**Actions**

| Package | What it is |
|---|---|
| `button/` | `ElwhaButton` — variants, sizes, shape morph, ripple; `ElwhaButtonSelectionGroup` for radio-style mutex across buttons |
| `iconbutton/` | `ElwhaIconButton` — per-instance variants, toggle icon swap, `doClick()`; implements `IconBearing`. `ElwhaIconButtonSelectionGroup` is the icon-button mutex |
| `buttongroup/` | `ElwhaButtonGroup` — M3 Expressive button group (connected/positional treatment) |
| `fab/` | `ElwhaFab` (standard + extended, shape morph) and `ElwhaFabAnchor` — the wrapper container that floats a FAB over content, scroll-aware |

**Selection controls**

| Package | What it is |
|---|---|
| `checkbox/` | `ElwhaCheckbox` — tri-state, hand-stroked marks |
| `radio/` | `ElwhaRadioButton` + `ElwhaRadioGroup` — roving tab stop, press-swap; matches the checkbox label/geometry/focus contract |
| `switches/` | `ElwhaSwitch` — M3 switch with icon modes |
| `chip/` | `ElwhaChip` + variants + interaction modes; implements `ElwhaListItemView`, so chips are hostable in `ElwhaItemList<T>` |

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
| `card/` | **V3** `ElwhaCard` — chrome-only primitive extending `ElwhaSurface`, plus the composition primitives added via `card.add(...)`: `ElwhaCardTitle` / `Subtitle` / `SupportingText` / `LeadingIcon` / `Thumbnail` / `Header` / `Media` / `Actions` / `Divider` / `Chevron` / `ExpandLink`. Variants `ELEVATED` / `FILLED` / `OUTLINED`; VERTICAL orientation only. Implements `ElwhaListItemView`. Architecture and token bindings: `docs/research/elwha-card-v3-spec.md` |
| `list/` | `ElwhaItemList<T>` — the **one** list container, behind the `ElwhaList<T>` interface (orientation / gap / padding / empty / loading / filter / sort), plus `DefaultElwhaListModel` / `DefaultElwhaSelectionModel`, the `ElwhaListItemView` capability a hosted component implements, `ElwhaItemAdapter` for item→view mapping, the reorder + selection event types, and `SelectionMode` / `MovementMode` / `ReorderAffordance` / `IconAffordance`. Epic **#67** collapsed the twin `card/list/` and `chip/list/` families into this on the locked `max(funcA, funcB)` principle, so selection *and* drag-reorder now live here rather than being per-family. `T` is the item type, not the view type. The package-private `ReorderCursors` loads the bundled grab cursors |
| `sidesheet/` | `ElwhaSideSheet` — `DOCKED` / `DETACHED` postures, modal + standard, opt-in drag gestures |

**Overlays**

| Package | What it is |
|---|---|
| `overlay/` | `AbstractElwhaOverlay` — the shared layered-pane host every overlay component builds on (placement, focus, dismissal, parent–child chaining) |
| `dialog/` | `AbstractElwhaDialog` + `ElwhaDialog` (M3 basic) + `ElwhaFullScreenDialog`, sharing the top-level `DismissCause` |
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
| `card/fixes/` | Diagnostic harnesses for historical card bugs. Advisory-only in reviews |
| `showcase/` | `ElwhaShowcase` — the storefront, plus `ComponentWorkbench` / `ContainerWorkbench` / `CodeView` and the per-component `*ShowcasePanels`. Three areas (Foundations / Components / Containers); the Components landing groups its 25 leaves under the **same family headings the tables above use**, and `ElwhaShowcase.GROUP_ORDER` is that taxonomy in code — reorder these tables and the storefront's landing pages go with them (#441). Also holds the six `J*SweepGuard` raw-Swing sweeps a test drives (#424) |

Bundled resources:
- `src/main/resources/com/owspfm/elwha/list/cursors/` — grab / grabbing reorder cursors, light + dark, 16/32px, loaded by the package-private `list/ReorderCursors`. First-party artwork under the repo's own Apache-2.0; the 32px hand outline derives from the Material Symbols `back_hand` glyph, which `NOTICE` already attributes to Google. **#531 is resolved** — these replaced the third-party Capitaine set whose bundled license file (LGPL-3.0) contradicted `NOTICE` (CC BY-SA 4.0). Generated, not hand-drawn: the vector source and rasteriser are on `design/531-cursor-redesign` (`docs/research/elwha-cursor-redesign-531.md`), which is also where the two conventions live — both states share hotspot `(16, 14)`, and `-light-`/`-dark-` name the *theme* an asset serves, so a `-light-` asset is a **dark** hand
- `src/main/resources/com/owspfm/icons/material/` — 76 Material Symbol SVGs (Apache 2.0 from Google; attribution in `NOTICE`)
- `src/main/resources/com/owspfm/elwha/theme/fonts/` — Inter Regular + Medium TTFs for `Typography.defaults()` (SIL OFL 1.1; attribution in `NOTICE`)
- `src/main/resources/com/owspfm/elwha/theme/palettes/` — bundled demo palettes in two directory-derived tiers: `primary/` (`baseline.json` — the M3 baseline scheme, `MaterialPalettes.baseline()` — plus the ROYGBIV set `red` / `orange` / `yellow` / `green` / `blue` / `indigo` / `deep-purple`) and `secondary/` (10 Material Theme Builder palettes — the colors not in the primary tier; the two tiers are disjoint). `MaterialPalettes.primary()` / `secondary()` discover every `*.json` in their subdirectory at runtime and return it in spectral (hue) order — directory-derived, not a hardcoded list, so dropping a new Elwha-format palette JSON into a tier surfaces it in The Elwha Showcase's palette picker with no code change. Raw M3 Theme Builder exports are archived under `docs/research/themes/`; `scripts/convert_mtb_palette.py` automates the M3-export → Elwha-palette conversion. Consumers may still ship their own palettes via `PaletteLoader`.

## Coupling stance (defend this)

The pre-extraction audit confirmed **zero coupling sites** between these components and the OWS-tool app they came from. Keep it that way: depend on **Swing + FlatLaf only**, never on app-specific code, domain types, or consumer assumptions. The compile-scope deps in `pom.xml` are `flatlaf`, `flatlaf-extras`, `flatlaf-intellij-themes` and nothing else — that is what a consumer inherits, and adding to it needs a strong reason. Test scope is separate and does not reach consumers: `junit-jupiter`, `assertj-core`, `cacio-tta-jdk21`.

## Conventions

- **Code style:** Google Java Style, enforced via Spotless (`googleJavaFormat`) and Checkstyle. Run `mvn spotless:apply` to fix formatting; `mvn verify` runs both checkers. No `my*` / `the*` identifier prefixes — qualify with `this.` when a parameter shadows a field. Full convention: `docs/development/code-style.md`.
- **Component API doctrine:** getter naming (`getX()` only — no `getEffectiveX()`), per-variant static factories, single-arg convenience constructors, border-role exposure rule, symmetric border-width getter/setter. Canonical source: `docs/development/component-api-conventions.md`. New components are expected to match.
- **Javadoc:** every public class / method has `@author`, `@version`, `@since`. `@version` is bumped on every change that touches the entity — **the `validate-versions` workflow runs `scripts/update_javadoc_version.py --check --changed-only` against the PR's milestone and hard-fails on missed bumps.** `@since` is set once and never moves. Bump `@version` in the same commit as the code change. Since #529 the javadoc build runs `-Xdoclint:all` with `<failOnError>true</failOnError>`, so a broken `@link` or an undocumented public member now fails `mvn package` — it is not advisory. House style: `docs/development/javadoc-style.md`. Full convention: `docs/development/versioning.md`; playbook: `docs/development/versioning-playbook.md`. `pages.yml` renders the same doc set to GitHub Pages on every push to `main`.
- **Tests:** a behavior change lands with a test. Two tiers, headless by default; `docs/development/testing.md` is the contract and its determinism rules are non-negotiable — see *Tests* above.
- **Commits:** Conventional Commits (`feat:`, `fix:`, `refactor:`, `docs:`, `chore:`, `ci:`, `test:`).
- **Semver:** through `0.x.y` a minor bump could break API and only patch was bug-fix-only. **1.0.0 ends that** — after the tag, breaking API changes need a major bump, minors are additive, patches are fixes. Document every change under `## [Unreleased]` in `CHANGELOG.md`, breaking ones first within their category (policy: `docs/development/changelog-policy.md`).
- **Milestones:** the title is consumed **verbatim** as the expected `@version` — `validate-versions` passes it straight to `scripts/update_javadoc_version.py --expected`, which string-compares it against the tag. So every milestone title must be a real version string (`vX.Y.Z`, no `-alpha` / `-beta` suffix, and never a label like `v1.x` or `backlog`). Set one on every PR before review. Two are live: **`v1.0.0`**, the freeze + first publish, holding only the release tail; and **`v1.1.0`**, the post-1.0 parking lot for deliberately deferred work. `v0.1.0`–`v0.5.0` are all closed — **`v0.5.0` completed the pre-V1 quality gate** — so milestone new work `v1.0.0` or `v1.1.0`.

  Know the mechanism before you fight the gate. It compares the milestone title against the *first* `@version` in each Java file the PR changed, so the milestone you pick is the version every touched file must carry. **The high-water mark on `main` is currently `v0.5.0`** (377 files; nothing carries `v1.0.0` yet, and the tail runs back through `v0.4.0` on 127 files to `v0.1.0` on 11) — so a `v1.0.0`-milestoned PR bumps every file it touches, and that is expected, not a mistake. A milestone *below* the mark is genuinely unreachable rather than merely closed, because `@version` never moves backwards; that is why nothing can be milestoned `v0.4.0` or lower. Note `mvn verify` does **not** run this gate (it's a workflow, not a Maven plugin); check it locally with `python3 scripts/update_javadoc_version.py --check --changed-only --expected v1.0.0`.
- **Backwards-compat shims: free to skip until the 1.0.0 tag, not after.** Format-breaking changes were unconstrained pre-1.0 and no deprecation layers or legacy aliases were added. Once 1.0.0 publishes, that licence expires and the semver rule above governs.
- **No code comments by default.** Add one only when the *why* is non-obvious (hidden constraint, subtle invariant, workaround). Don't explain *what* — identifiers cover that.
- **Material Symbols icon house style:** Rounded / weight 400 / fill 0 / 20px. Override only when fill1 is semantically needed for a "selected/active" state. Source from `gstatic` for crisp variants. Use `MaterialIcons` helper, not raw `FlatSVGIcon`.
- **JDK target:** 21 (`maven.compiler.release=21`). Stays at 21 until OWS-tool migrates off 21 — bumping prematurely cuts off consumers. This governs *bytecode output* and is unaffected by which JVM runs Maven; the separate requirement to *build* on JDK 21 is a Spotless constraint — see "The build runs on JDK 21" above.
- **PRs need a milestone at creation.** The Validate `@version` workflow hard-fails without one.
- **Branch protection:** `main` requires `build`, `Test (components + Showcase)`, `Validate @version and @since tags`, `Validate formatting (Spotless)`, and `Validate naming (Checkstyle)`; force-push and deletion are blocked.

## Release process

Tag-driven publish to GitHub Packages (`.github/workflows/publish.yml` does the validation + publish):

1. Bump `<version>` in `pom.xml`
2. Move `[Unreleased]` → `[X.Y.Z]` in `CHANGELOG.md`
3. `git commit -m "chore: release X.Y.Z"`
4. `git tag -a vX.Y.Z -m "Release X.Y.Z"`
5. `git push origin vX.Y.Z`

The workflow validates that the tag matches `pom.xml` and that `CHANGELOG.md` has a matching `## [X.Y.Z]` heading, then publishes the jar + sources + javadoc to `https://maven.pkg.github.com/OWS-PFMS/elwha`.

**`docs/development/release-runbook.md` is the executable version of this** — pre-flight checklist, the exact commands, and what to verify after the workflow goes green. Use it rather than the five lines above when actually cutting a release.

## Version state & release

The pre-1.0 roadmap is **complete** — every epic that gated the freeze is closed, and the full account of what changed lives in `CHANGELOG.md`, not here. `v1.0.0` holds only the publishing tail: #97 (and epic #80, which closes with it) stay open until the tag publishes; the procedure is `docs/development/release-runbook.md`. `v1.1.0` is the post-1.0 parking lot for deliberately deferred work.

**Superseded plans — don't work from them:** `docs/research/elwha-v1-component-scope.md` (never locked), `docs/handoff/elwha-v1-roadmap-handoff.md` (historical; its §11 definition of done was met), and the old consumer-repo epics `OWS-Local-Search-GUI#252` / `#253`.

Org-level project board: **Project #5 — Material Flat Component Library** at `https://github.com/orgs/OWS-PFMS/projects/5`. **Every new issue gets added to it as part of filing.**

Known consumer: **OWS-PFMS/OWS-Local-Search-GUI** (via PR #266 swap-out); its issues #243 / #244 are the OWS-side `FactorPill` / `InnerViewTabStrip` migration onto `ElwhaChip`. Both are still open and still written against the pre-rename `FlatPill` / `FlatPillList(WRAP)` API — the target is now `ElwhaChip` inside `ElwhaItemList<T>`, so re-read them against 1.0.0 before picking either up. Note that **1.0.0 is OWS's *initial* Elwha release** — it never adopted a published V1 card, so there is no consumer mid-migration to protect.

## Working-style preferences (from the operator)

These carry forward unless overridden in conversation:

- **Clarifying questions in chat as plain-text numbered lists.** Don't use UI pickers.
- **Don't auto-merge PRs.** After CI green, hand off — merge only on explicit go.
- **Preserve `Closes #N` keywords** in squash-merge commit bodies. Custom `--body` flags can overwrite the PR description; sub-issues silently stay open otherwise.
- **No interactive Git commands** (`-i` flags) — no rebase -i, no add -i.
- **Set the PR milestone at creation** (required for the `@version` validation workflow).
