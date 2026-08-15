# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## What this repo is

**Elwha** — a Swing component library built on [FlatLaf](https://www.formdev.com/flatlaf/), implementing **Material 3 Expressive** as a design system for desktop Java: a design-token foundation plus ~25 components and the containers, overlays, and anchors around them. Apache 2.0, JDK 21. Since the #779 artifact split (PRs #787/#788) the repo is a **two-module Maven reactor** shipping two artifacts: **`elwha`** — the library consumers import and semver governs (193 source files) — and **`elwha-showcase`** — the runnable storefront (355 files: the Showcase, every playground, every story-time main). ~127k LOC of main-tree code, with a ~4,640-test suite behind it.

**Version:** the poms read `1.1.2` — the current release (2026-08-15: the public `ElwhaCursors` accessor for the grab / grabbing drag pointers, restoring surface lost in the flatcomp→elwha rename, plus the card-migration doc correction; shipped as a patch on regression grounds — see the note at the head of its `CHANGELOG.md` section); `1.1.1` (2026-08-11) was the consumer-report patch — `MaterialIcons` fail-fast for unbundled glyphs + testkit dependency docs; `1.1.0` (also 2026-08-11) was the two-artifact split (epic #779) plus the field-report burn-down, `1.0.1` (2026-08-10) the field-report patch, and `1.0.0` (2026-08-09) the API-stability milestone and OWS's initial Elwha adoption. `main` and the version string agree; the shared version lives in exactly three places — the parent's `<version>` and each module's `<parent><version>` — and moves only in a release commit, never in content PRs (see *Release process* below and `docs/development/release-runbook.md`). From the `v1.0.0` tag onward semver governs: breaking changes need a major bump, minors are additive only, patches are fixes — the pre-1.0 licence to break API freely, and the no-backwards-compat-shims rule that went with it, both expired at that tag. See *Milestones* under Conventions.

`groupId = com.owspfm`; artifactIds `elwha` (the library), `elwha-showcase` (the storefront app), `elwha-parent` (the reactor pom — published so module poms resolve). Single maintainer: Charles Bryan (`cfb3@uw.edu`). GitHub org: `OWS-PFMS`.

**Provenance:** extracted from [OWS-PFMS/OWS-Local-Search-GUI](https://github.com/OWS-PFMS/OWS-Local-Search-GUI) on 2026-05-12 via epic #231 with `git filter-repo --subdirectory-filter` so pre-extraction history is preserved. Rationale and the coupling audit that confirmed extraction-readiness live in `docs/research/elwha-extraction-decisions.md` and `docs/research/elwha-coupling-audit.md` — **read those before making architectural decisions; don't re-litigate them.**

## Build & run

```bash
mvn clean package    # → elwha/target/elwha-<v>.jar + elwha-showcase/target/elwha-showcase-<v>.jar
                     #   (each with sources + javadoc jars; elwha also attaches the testkit `tests` jar)
mvn verify           # the above + the test suite + Spotless + Checkstyle, reactor-wide

# The Elwha Showcase — the primary visual harness; every shipped component has a leaf
mvn compile exec:java -Dexec.mainClass="com.owspfm.elwha.showcase.ElwhaShowcase"
mvn compile exec:java    # same thing — the Showcase is the exec.mainClass default

# Component playgrounds — 14 packages, ~79 mains, under <component>/playground/
mvn compile exec:java -Dexec.mainClass="com.owspfm.elwha.chip.ElwhaChipPlayground"
mvn compile exec:java -Dexec.mainClass="com.owspfm.elwha.card.playground.ElwhaCardPlayground"
mvn compile exec:java -Dexec.mainClass="com.owspfm.elwha.theme.playground.ThemePlayground"

# Evaluator path — the self-contained shaded Showcase jar (also attached to each GitHub Release)
java -jar elwha-showcase/target/elwha-showcase-<version>-app.jar
```

The `exec:java` one-liners are unchanged **verbatim** from the single-module days, by design: the parent pom skips exec reactor-wide and `elwha-showcase` re-enables it, with `mainClass` routed through the parent-level `exec.mainClass` property so `-Dexec.mainClass=…` still overrides (artifact-split design §1.5). Every moved main kept its package name, so no class name in a muscle-memory command changed.

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

**The suite — `mvn verify`.** ~4,640 tests on JUnit 6 + AssertJ, split with the modules: ~4,135 under `elwha/src/test/java/com/owspfm/elwha/` (one package per component, plus `testkit/`) and 503 under `elwha-showcase/src/test/java/` (the storefront suite). Built in epic #438; it is a **required check on `main`** (`Test (components + Showcase)`) and runs in two tiers, configured once in the parent pom and applied per module:

| | Tier A — headless, the default | Tier B — `@Tag("gui")` |
|---|---|---|
| Count | ~4,540 (4,035 library + 503 showcase) | ~100 (all in `elwha` today; the showcase execution is configured but matches zero tests) |
| JVM | `java.awt.headless=true` | headless=false, its own forked JVM |
| Display | none — offscreen `BufferedImage` | Cacio virtual toolkit locally, native under Xvfb on Linux CI |
| Covers | construction, geometry, pixel probes in both modes, a11y shapes, event contracts, InputMap wiring, synthetic input dispatch | real focus *ownership*, `Robot` input, window realization, overlay placement, drag gestures |

Put a test in Tier B only when headless cannot represent what it asserts. The split is load-bearing — mixing Cacio with an already-initialized JDK toolkit segfaults — so the tiers run in separate surefire executions and separate JVMs. Shared fixtures live in `elwha/src/test/java/com/owspfm/elwha/testkit/` (`EdtInterceptor`, `ThemeExtension`, `Pixels`, `Input`, `WaitFor`, `GuiToolkit`, `GuiSteps`, `PaintLog`, `HeadlessHost`). `elwha-showcase` consumes the testkit by **source inclusion** — build-helper adds the library's test tree, compiler `testIncludes` restrict it to `testkit/**` plus its own tests (a test-jar *dependency* would break cold-tree `mvn compile exec:java`); the `tests`-classifier jar `elwha` ships is the path for consumers outside the reactor. **Read `docs/development/testing.md` before writing one** — its determinism rules (reduced motion pinned, assert resolved roles never hex, never probe pixels inside a glyph box, pace `Robot` input) are what holds the flake budget at zero. JaCoCo is report-only; there is no coverage threshold.

**By eye.** Nothing in the suite renders to a screen a human looks at, so a visual change still needs a human. **The Elwha Showcase** is the storefront — one leaf per component, grouped by the same families the package tables below use — and it is the first place to check anything visual. The per-component `playground/` packages go deeper on one component. The per-story `*Demo` / `*Smoke` / `*Guard` mains scattered through the `elwha-showcase` module are story-time artifacts: each proves the story that shipped it, they are not a maintained suite, and nothing runs them together. The one exception is the six `J*SweepGuard` classes in `showcase/`, which a real test now drives (#424).

## Source layout

Two modules under the reactor root, each on the standard Maven layout with no `<sourceDirectory>` override: the library at `elwha/src/main/java/com/owspfm/...` (bundled resources under `elwha/src/main/resources/com/owspfm/...`), the demo surface at `elwha-showcase/src/main/java/com/owspfm/...`. (The tree was migrated from a flat `src/` layout in [#60](https://github.com/OWS-PFMS/elwha/issues/60) and into the modules by #785/#786; `git log --follow` traverses both moves, so blame is preserved.)

Component packages under `elwha/src/main/java/com/owspfm/elwha/`:

The story-time harness that used to ride along in these packages — the `*Demo` / `*Smoke` / `*Guard` / `*Diag` mains and the 14 `playground/` subpackages — now lives in the **`elwha-showcase` module**, keeping its original package names (split packages by design; the showcase jar is classpath-only and declares no `Automatic-Module-Name`). The tables below name only what a consumer imports — which is exactly what the `elwha` jar now contains.

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
| `list/` | `ElwhaItemList<T>` — the **one** list container, behind the `ElwhaList<T>` interface (orientation / gap / padding / empty / loading / filter / sort), plus `DefaultElwhaListModel` / `DefaultElwhaSelectionModel`, the `ElwhaListItemView` capability a hosted component implements, `ElwhaItemAdapter` for item→view mapping, the reorder + selection event types, and `SelectionMode` / `MovementMode` / `ReorderAffordance` / `IconAffordance`. Epic **#67** collapsed the twin `card/list/` and `chip/list/` families into this on the locked `max(funcA, funcB)` principle, so selection *and* drag-reorder now live here rather than being per-family. `T` is the item type, not the view type. The package-private `ReorderCursors` loads the bundled grab cursors; `ElwhaCursors` is the public two-method facade over it (`grab()` / `grabbing()`), added in 1.1.2 so consumers can dress their own draggable surfaces — the cache-invalidation machinery stays package-private |
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

**The `elwha-showcase` module** — the demo surface. These live at `elwha-showcase/src/main/java/com/owspfm/elwha/…`, alongside the per-component demo/smoke/playground files, and ship in the showcase jar, not the library:

| Package | What it is |
|---|---|
| `card/fixes/` | Diagnostic harnesses for historical card bugs. Advisory-only in reviews |
| `showcase/` | `ElwhaShowcase` — the storefront, plus `ComponentWorkbench` / `ContainerWorkbench` / `CodeView` and the per-component `*ShowcasePanels`. Three areas (Foundations / Components / Containers); the Components landing groups its 25 leaves under the **same family headings the tables above use**, and `ElwhaShowcase.GROUP_ORDER` is that taxonomy in code — reorder these tables and the storefront's landing pages go with them (#441). Also holds the six `J*SweepGuard` raw-Swing sweeps a test drives (#424) |

Bundled resources — all in the library module; the showcase loads them through library APIs, resolved cross-module over the classpath:
- `elwha/src/main/resources/com/owspfm/elwha/list/cursors/` — grab / grabbing reorder cursors, black-bodied + white-bodied, 16/32px, loaded by the package-private `list/ReorderCursors` and reachable by consumers through `list/ElwhaCursors`. First-party artwork under the repo's own Apache-2.0; the 32px hand outline derives from the Material Symbols `back_hand` glyph, which `NOTICE` already attributes to Google. **#531 is resolved** — these replaced the third-party Capitaine set whose bundled license file (LGPL-3.0) contradicted `NOTICE` (CC BY-SA 4.0). Generated, not hand-drawn: the vector source and rasteriser are on `design/531-cursor-redesign` (`docs/research/elwha-cursor-redesign-531.md`). Two conventions: both states share hotspot `(16, 14)`, and the body color is selected by **platform, never theme** (#762 — no OS flips its pointer with the theme): `-black-` on macOS/Linux matching their black system arrows, `-white-` on Windows matching its white arrow, with the halo carrying opposite-ground legibility the way the OS outline does
- `elwha/src/main/resources/com/owspfm/icons/material/` — 76 Material Symbol SVGs (Apache 2.0 from Google; attribution in `NOTICE`)
- `elwha/src/main/resources/com/owspfm/elwha/theme/fonts/` — Inter Regular + Medium TTFs for `Typography.defaults()` (SIL OFL 1.1; attribution in `NOTICE`)
- `elwha/src/main/resources/com/owspfm/elwha/theme/palettes/` — bundled demo palettes in two directory-derived tiers: `primary/` (`baseline.json` — the M3 baseline scheme, `MaterialPalettes.baseline()` — plus the ROYGBIV set `red` / `orange` / `yellow` / `green` / `blue` / `indigo` / `deep-purple`) and `secondary/` (10 Material Theme Builder palettes — the colors not in the primary tier; the two tiers are disjoint). `MaterialPalettes.primary()` / `secondary()` discover every `*.json` in their subdirectory at runtime and return it in spectral (hue) order — directory-derived, not a hardcoded list, so dropping a new Elwha-format palette JSON into a tier surfaces it in The Elwha Showcase's palette picker with no code change. Raw M3 Theme Builder exports are archived under `docs/research/themes/`; `scripts/convert_mtb_palette.py` automates the M3-export → Elwha-palette conversion. Consumers may still ship their own palettes via `PaletteLoader`.

## Coupling stance (defend this)

The pre-extraction audit confirmed **zero coupling sites** between these components and the OWS-tool app they came from. Keep it that way: depend on **Swing + FlatLaf only**, never on app-specific code, domain types, or consumer assumptions. The compile-scope deps in `elwha/pom.xml` are `flatlaf`, `flatlaf-extras` and nothing else — that is what a consumer inherits, and adding to it needs a strong reason (`flatlaf-intellij-themes` was extraction-era dead weight, removed in #775). Test scope is separate — declared once in the parent pom — and does not reach consumers: `junit-jupiter`, `assertj-core`, `cacio-tta-jdk21`. (`elwha-showcase`'s own deps, `elwha` + `flatlaf-extras`, are an application's and reach no consumer either.)

## Conventions

- **Code style:** Google Java Style, enforced via Spotless (`googleJavaFormat`) and Checkstyle. Run `mvn spotless:apply` to fix formatting; `mvn verify` runs both checkers. No `my*` / `the*` identifier prefixes — qualify with `this.` when a parameter shadows a field. Full convention: `docs/development/code-style.md`.
- **Component API doctrine:** getter naming (`getX()` only — no `getEffectiveX()`), per-variant static factories, single-arg convenience constructors, border-role exposure rule, symmetric border-width getter/setter. Canonical source: `docs/development/component-api-conventions.md`. New components are expected to match.
- **Javadoc:** every public class / method has `@author`, `@version`, `@since`. `@version` is bumped on every change that touches the entity — **the `validate-versions` workflow runs `scripts/update_javadoc_version.py --check --changed-only` against the PR's milestone and hard-fails on missed bumps.** `@since` is set once and never moves. Bump `@version` in the same commit as the code change. Since #529 the javadoc build runs `-Xdoclint:all` with `<failOnError>true</failOnError>`, so a broken `@link` or an undocumented public member now fails `mvn package` — it is not advisory. House style: `docs/development/javadoc-style.md`. Full convention: `docs/development/versioning.md`; playbook: `docs/development/versioning-playbook.md`. `pages.yml` renders the same doc set to GitHub Pages on every push to `main`.
- **Tests:** a behavior change lands with a test. Two tiers, headless by default; `docs/development/testing.md` is the contract and its determinism rules are non-negotiable — see *Tests* above.
- **Commits:** Conventional Commits (`feat:`, `fix:`, `refactor:`, `docs:`, `chore:`, `ci:`, `test:`).
- **Semver:** through `0.x.y` a minor bump could break API and only patch was bug-fix-only. **1.0.0 ends that** — after the tag, breaking API changes need a major bump, minors are additive, patches are fixes. Document every change under `## [Unreleased]` in `CHANGELOG.md`, breaking ones first within their category (policy: `docs/development/changelog-policy.md`).
- **Milestones:** the title is consumed **verbatim** as the expected `@version` — `validate-versions` passes it straight to `scripts/update_javadoc_version.py --expected`, which string-compares it against the tag. So every milestone title must be a real version string (`vX.Y.Z`, no `-alpha` / `-beta` suffix, and never a label like `v1.x` or `backlog`). Set one on every PR before review. Two are live: **`v1.2.0`**, the wave in flight; and **`v1.3.0`**, the parking lot for deliberately deferred work. Everything else — `v0.1.0`–`v0.5.0` (with **`v0.5.0`** completing the pre-V1 quality gate), `v1.0.0` (shipped 2026-08-09), `v1.0.1` (2026-08-10), `v1.1.0` / `v1.1.1` (both 2026-08-11) and `v1.1.2` (2026-08-15) — is closed, so milestone new work `v1.2.0` or `v1.3.0`.

  Know the mechanism before you fight the gate. It compares the milestone title against the *first* `@version` in each Java file the PR changed, so the milestone you pick is the version every touched file must carry. **The high-water mark on `main` is currently `v1.1.2`** (2 main-tree files, from the #814 cursor release; `v1.1.1` is on 6 more; `v1.1.0` is on 62 more, put there by the #784 field-report wave and the #779 split, while the bulk of the tree still reads `v0.5.0` — 1,222 tagged entities — with the tail running back through `v0.4.0` to `v0.1.0`, and `v1.0.0` / `v1.0.1` on 4 each) — so a `v1.2.0`-milestoned PR bumps every `v0.5.0`-era file it touches across the gap, and a wide PR bumping every file it touches is expected, not a mistake. A milestone *below* the mark is genuinely unreachable rather than merely closed, because `@version` never moves backwards; that is why nothing can be milestoned `v1.0.1` or lower. Note `mvn verify` does **not** run this gate (it's a workflow, not a Maven plugin); check it locally with `python3 scripts/update_javadoc_version.py --check --changed-only --expected v1.2.0`. Since the #779 split the gate is module-aware: it diffs **without** a git pathspec (a module-root pathspec breaks rename pairing and degrades moves to spurious adds) and scopes to `elwha/src/` + `elwha-showcase/src/` in code, with test trees exempt outside `testkit/` and pure renames (R100) excluded.
- **Backwards-compat shims: free to skip until the 1.0.0 tag, not after.** Format-breaking changes were unconstrained pre-1.0 and no deprecation layers or legacy aliases were added. Once 1.0.0 publishes, that licence expires and the semver rule above governs.
- **No code comments by default.** Add one only when the *why* is non-obvious (hidden constraint, subtle invariant, workaround). Don't explain *what* — identifiers cover that.
- **Material Symbols icon house style:** Rounded / weight 400 / fill 0 / 20px. Override only when fill1 is semantically needed for a "selected/active" state. Source from `gstatic` for crisp variants. Use `MaterialIcons` helper, not raw `FlatSVGIcon`.
- **JDK target:** 21 (`maven.compiler.release=21`). Stays at 21 until OWS-tool migrates off 21 — bumping prematurely cuts off consumers. This governs *bytecode output* and is unaffected by which JVM runs Maven; the separate requirement to *build* on JDK 21 is a Spotless constraint — see "The build runs on JDK 21" above.
- **PRs need a milestone at creation.** The Validate `@version` workflow hard-fails without one.
- **Branch protection:** `main` requires `build`, `Test (components + Showcase)`, `Validate @version and @since tags`, `Validate formatting (Spotless)`, and `Validate naming (Checkstyle)`; force-push and deletion are blocked.

## Release process

Tag-driven publish to GitHub Packages (`.github/workflows/publish.yml` does the validation + publish): bump the shared `<version>` across the reactor poms, move `[Unreleased]` → `[X.Y.Z]` in `CHANGELOG.md`, commit `chore: release X.Y.Z`, tag `vX.Y.Z`, push the tag. The workflow validates the tag against the poms and the `CHANGELOG.md` heading, then publishes both modules' artifacts to `https://maven.pkg.github.com/OWS-PFMS/elwha` — the library jar (+ sources, javadoc, and the testkit `tests` jar), the showcase jar (+ sources, javadoc), and the parent pom. A release also ships the self-contained **`elwha-showcase-<v>-app.jar`** (the shaded evaluator jar) as a GitHub Release asset.

**`docs/development/release-runbook.md` is canonical** — the pre-flight checklist, the exact commands (including the multi-pom version bump), and what to verify after the workflow goes green. Use it, not the summary above, when actually cutting a release.

## Version state & release

The pre-1.0 roadmap is **complete and shipped**: tag `v1.0.0` published 2026-08-09 (#97 and epic #80 closed with it), the `v1.0.1` field-report patch followed 2026-08-10, and the full account of what changed lives in `CHANGELOG.md`, not here. The release procedure is `docs/development/release-runbook.md`. `v1.1.0` is the wave in flight; `v1.2.0` is the parking lot for deliberately deferred work.

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
