# Elwha artifact split — Phase 0 design (#779)

Phase 0 of epic #779: split the single `elwha` artifact into a two-module Maven reactor — `elwha`
(the library consumers import and semver governs) and `elwha-showcase` (the runnable storefront:
the Showcase, every playground, every story-time main). The epic locked the shape with the operator
on 2026-08-10: two modules, shared version from a parent pom, wholesale move with no pruning, both
publish channels (GitHub Packages for both jars, plus the runnable jar attached to the GitHub
Release), minor bump licensed by `docs/consumer/stability.md`'s harness carve-out. This doc does
not reopen any of that. What it does is make the split executable: the exact layout, the exact move
inventory (measured, not estimated), the testkit strategy, what each of the six CI workflows needs,
and the release plumbing for a two-artifact tag.

Every count and every claim below was taken from the tree at the time of writing (main at 1.0.1,
2026-08-10) by running the globs and reading the workflows and scripts — not from CLAUDE.md's
approximations.

## TL;DR — the decisions this doc locks

- **Layout:** root parent pom (`com.owspfm:elwha-parent`, packaging `pom`) + two module
  directories named after their artifactIds: `elwha/` and `elwha-showcase/`. The
  library-stays-at-root alternative is rejected (§1.1 — Maven forbids it cleanly).
- **Move inventory:** **355 of 547** main-tree files move; **192 stay**. All 301 `main(...)`
  methods in `src/main` are inside the moved set — after the split, *nothing runnable-for-humans
  remains in `elwha`*, mechanically verified (§2.3). Two name patterns the epic didn't list —
  the six `*Diag` classes and the stray top-level `ElwhaChipPlayground` — are included in the
  move (§2.2, operator sign-off requested).
- **Reference safety:** a full token-level scan of every staying file against every moved class
  name found **zero compile-level references** from library code (main or test) to moved classes.
  The five hits are comments/Javadoc prose (§2.4). **No Phase-1 pre-story is needed.**
- **Testkit:** `elwha` publishes a `tests`-classifier **test-jar restricted to
  `com/owspfm/elwha/testkit/**`**; `elwha-showcase` consumes it test-scope. Promoting testkit to a
  third module is rejected because it creates a reactor cycle: testkit imports
  `com.owspfm.elwha.theme.*`, and `elwha`'s own tests need testkit (§3.2).
- **Runnable jar:** maven-shade with `shadedArtifactAttached=true`, classifier **`app`** — the
  *plain* `elwha-showcase` jar (with `Main-Class`) deploys to Packages so Maven consumers get
  clean transitive deps; the *shaded* `elwha-showcase-<v>-app.jar` is attached to the GitHub
  Release so evaluators run it with zero Packages auth and zero classpath assembly (§5.2).
- **CI:** all five branch-protection contexts keep their exact names — no required-check rename.
  Four workflows need path or flag edits; `build.yml` and the Checkstyle half of
  `validate-style.yml` need pom-side changes only. **`validate-versions` does not survive the
  reactor as-is, and it fails *open***: the script's `git diff -- src/ test/` pathspec matches
  nothing under module directories, so it reports "no Java files changed" and exits 0 on every PR.
  The script fix rides Phase 1, not Phase 2, to close that window (§4.4).
- **Invocation continuity:** with `exec.mainClass` wired as a showcase-pom property (§1.5), every
  `mvn compile exec:java -Dexec.mainClass="…"` one-liner in CLAUDE.md keeps working **verbatim**
  from the repo root.
- **Split packages, accepted:** the wholesale move leaves e.g. `com.owspfm.elwha.progress` present
  in both jars. `elwha-showcase` therefore gets **no** `Automatic-Module-Name` and is documented
  as classpath-only; `elwha`'s JPMS story (#774) is untouched (§2.6).

---

## §1. Module layout & the parent pom

### §1.1 Directory shape [LOCKED: root parent + two subdirectories]

```
elwha/                          (repo root — the parent)
├── pom.xml                     com.owspfm:elwha-parent, packaging pom, <modules>
├── .mvn/maven.config           (new, empty — anchors maven.multiModuleProjectDirectory, §1.3)
├── config/checkstyle/          (stays at root, shared)
├── scripts/                    (stays at root)
├── docs/                       (stays at root)
├── CHANGELOG.md / README.md / LICENSE / NOTICE / CLAUDE.md / .envrc / cspell.json
├── elwha/
│   ├── pom.xml                 com.owspfm:elwha (jar)
│   └── src/main/java, src/main/resources, src/main/javadoc, src/test/java
└── elwha-showcase/
    ├── pom.xml                 com.owspfm:elwha-showcase (jar)
    └── src/main/java, src/test/java
```

The alternative the epic asked to weigh — keep the library at the repo root and add only a
`elwha-showcase/` module — does not survive contact with Maven. A project that declares
`<modules>` must have packaging `pom`; the root pom cannot be both the aggregator and the library
jar. The only way to fake it is a module pom that reaches up (`<sourceDirectory>../src/main/java`),
which breaks IDE imports, the Spotless `src/main/java/**` includes, the resource convention, and
the "Maven default layout, no override" rule CLAUDE.md records from #60. Rejected.

Module directories are named after their artifactIds (`elwha/`, `elwha-showcase/`) rather than
`lib/`/`showcase/`. The `elwha/elwha/src/…` path stutter is real but the dir-equals-artifactId
convention is what FlatLaf itself and most multi-module Swing projects use, and it keeps
`-pl elwha` / `-pl elwha-showcase` self-describing.

### §1.2 The three poms — what hoists, what stays

Root pom `com.owspfm:elwha-parent` (packaging `pom`, version `1.1.0` at split time). Both module
poms name it in their `<parent>` block and omit their own `<groupId>`/`<version>`.

| Config | Where it lands | Notes |
|---|---|---|
| `<properties>` (compiler release, encoding, all dep versions, `cacio.argline`, `guitier.toolkit`) | parent | verbatim hoist |
| `linux-gui-native` profile | parent | inherited; the toolkit selection is per-JVM, not per-module |
| `<dependencyManagement>` (junit-bom import) | parent | verbatim hoist |
| Test-scope deps (`junit-jupiter`, `assertj-core`, `cacio-tta-jdk21`) | parent `<dependencies>` | both modules have test trees; forcing the trio on both is correct, not sloppy — and it sidesteps the test-jar's no-transitive-test-deps caveat (§3.2) |
| Compile deps (`flatlaf`, `flatlaf-extras`, `flatlaf-intellij-themes`) | `elwha` module | the consumer-inherited set stays exactly where stability.md documents it |
| `elwha` + `flatlaf-extras` compile deps | `elwha-showcase` module | §2.7 |
| maven-compiler-plugin | parent `<build><plugins>` | identical for both |
| maven-surefire-plugin (both tier executions) | parent `<build><plugins>` | the two-tier split applies per module unchanged (§3.3) |
| jacoco-maven-plugin | parent `<build><plugins>` | per-module report, still report-only |
| spotless-maven-plugin | parent `<build><plugins>` | its includes (`src/main/java/com/owspfm/**`) are module-relative and hoist verbatim |
| maven-checkstyle-plugin | parent `<build><plugins>` | `configLocation` re-anchored, §1.3 |
| maven-source-plugin, maven-javadoc-plugin | parent `<build><plugins>` | **both modules** attach sources + javadoc — see below |
| javadoc `<overview>` | `elwha` module override | the file moves to `elwha/src/main/javadoc/overview.html`; showcase has none |
| maven-jar-plugin | parent `pluginManagement` (version), per-module `<configuration>` | `elwha`: `Automatic-Module-Name: com.owspfm.elwha` (#774). `elwha-showcase`: `Main-Class: com.owspfm.elwha.showcase.ElwhaShowcase`, **no** module name (§2.6) |
| LICENSE/NOTICE `<resource>` (#774) | parent | path re-anchored (§1.3); both jars carry them — the release-attached showcase jar needs them at least as much as the library jar |
| maven-shade-plugin | `elwha-showcase` module only | §5.2 |
| exec-maven-plugin skip wiring | parent `pluginManagement` + showcase override | §1.5 |
| `<distributionManagement>` | parent | inherited; the URL (`maven.pkg.github.com/OWS-PFMS/elwha`) is repo-scoped and serves all three coordinates |

**Javadoc stays symmetric deliberately.** The 355 harness files are doclint-clean today because
`failOnWarnings` gates them; if `elwha-showcase` stopped building a javadoc jar, that discipline
would silently lapse on exactly the files least likely to get review attention. The cost is
seconds of build time; keep the ratchet on both modules. (Cross-module `{@link}`s from showcase
into library classes resolve against the classpath — doclint accepts them.) `legacyMode` (#774)
hoists with the plugin config; it is only *needed* by `elwha`'s `Automatic-Module-Name`, and is
inert for the showcase module.

### §1.3 Path re-anchoring — `${maven.multiModuleProjectDirectory}` + the `.mvn` marker

Two configs currently resolve against `${project.basedir}` and must not, because under the reactor
each module's basedir is the module directory:

1. Checkstyle `configLocation` → `${maven.multiModuleProjectDirectory}/config/checkstyle/checkstyle.xml`
2. The #774 LICENSE/NOTICE `<resource>` directory → `${maven.multiModuleProjectDirectory}`

`maven.multiModuleProjectDirectory` is set by the `mvn` launcher to the nearest ancestor directory
containing `.mvn`; without one it degrades to the invocation directory, so `cd elwha && mvn verify`
would break both paths. The repo has no `.mvn` today — Phase 1 adds `.mvn/maven.config` (empty) as
the anchor. This is the same commit as the pom split, per the §6 rule that path config moves with
the files it points at.

Everything else already survives: Spotless includes and the surefire config are module-relative;
`scripts/`, `config/`, `docs/`, `.envrc`, `cspell.json` stay at root untouched.

### §1.4 Versions and the release bump [explicit parent version; flatten/`${revision}` rejected]

The shared version lives in exactly three places: the parent's `<version>` and each module's
`<parent><version>`. The release-commit edit becomes:

```bash
mvn -q versions:set -DnewVersion=1.2.0 -DgenerateBackupPoms=false
```

which updates all three atomically. The CI-friendly alternative (`${revision}` +
flatten-maven-plugin) buys a one-line bump at the price of a new plugin whose flattening rewrites
the installed pom — machinery this repo doesn't otherwise need, and a trap for anyone comparing
the deployed pom to the tree. `versions:set` is zero-infrastructure and the runbook already treats
the bump as a scripted mechanical act. Rejected in favor of `versions:set`.

`publish.yml`'s tag check reads `${project.version}` with `--non-recursive` — under the reactor
that is the parent's version, which is the shared version. It keeps working unchanged.

### §1.5 How invocations change — mostly, they don't

The trap: in a reactor, a CLI goal like `exec:java` runs on *every* module (including the parent),
and `mvn -pl elwha-showcase -am compile exec:java` is not an escape — `-am` applies the goals to
the depended-on modules too, so `exec:java` would run on `elwha`, fail to find the main class, and
kill the build. The fix is pom-side, once:

- parent `pluginManagement`: exec-maven-plugin pinned, `<skip>true</skip>` — so the parent and
  `elwha` skip it silently;
- `elwha-showcase` `<build><plugins>`: declares the plugin with `<skip>false</skip>` and
  `<mainClass>${exec.mainClass}</mainClass>`, plus a module property
  `<exec.mainClass>com.owspfm.elwha.showcase.ElwhaShowcase</exec.mainClass>`.

Routing `mainClass` through the property (rather than hardcoding it) matters: pom configuration
outranks CLI user properties, so a hardcoded `<mainClass>` would *ignore* `-Dexec.mainClass`.
With the property indirection, `-D` overrides the default and every existing one-liner keeps
working verbatim from the root:

| Today | Under the reactor |
|---|---|
| `mvn clean package` | unchanged — jars land at `elwha/target/` and `elwha-showcase/target/` |
| `mvn verify` | unchanged — the reactor root runs everything the single module ran (epic invariant) |
| `mvn compile exec:java -Dexec.mainClass="com.owspfm.elwha.showcase.ElwhaShowcase"` | unchanged verbatim; also just `mvn compile exec:java` (the property default) |
| `mvn compile exec:java -Dexec.mainClass="com.owspfm.elwha.chip.ElwhaChipPlayground"` | unchanged verbatim (any playground/demo main) |
| `python3 scripts/update_javadoc_version.py --check --changed-only …` | unchanged command, **fixed script** (§4.4) |
| — | new: `java -jar elwha-showcase/target/elwha-showcase-<v>-app.jar` (§5.2) |

## §2. The move inventory — measured

### §2.1 The buckets

547 main-tree Java files today. **355 move, 192 stay.** Bucket precedence (a file counts once, in
the first bucket that claims it):

| Bucket | Files | Definition |
|---|---|---|
| `showcase/` | 51 | the whole package, incl. the six `J*SweepGuard` sweeps and `RawSwingSweep` |
| `playground/` subpackages | 109 | all 14 of them (badge, button, buttongroup, card, chip, dialog, fab, iconbutton, icons, navrail, selectfield, surface, textfield, theme) |
| `card/fixes/` | 3 | `CardFixesDemo`, `SinglePassCornerAaDemo`, `package-info` |
| `*Demo` | 91 | story-time mains in component packages |
| `*Smoke` | 92 | " |
| `*Guard` | 2 | `progress/ElwhaProgressBarParityGuard`, `slider/SliderDisabledKeyGuard` |
| `*Diag` | 6 | §2.2 |
| `*Playground` outside a `playground/` dir | 1 | `chip/ElwhaChipPlayground` — §2.2 |
| **Total moved** | **355** | |

Per-package, so Phase 1's move script has a checkable target:

| Package | Moves | Breakdown |
|---|---|---|
| showcase | 51 | the package |
| appbar | 12 | 7 Demo, 5 Smoke |
| badge | 9 | playground |
| button | 6 | playground |
| buttongroup | 4 | playground |
| card | 12 | 9 playground, 3 card/fixes |
| checkbox | 4 | 4 Demo |
| chip | 3 | 2 playground, 1 `ElwhaChipPlayground` |
| colorpicker | 29 | 14 Demo, 14 Smoke, 1 Diag |
| dialog | 13 | playground |
| fab | 14 | playground |
| iconbutton | 4 | playground |
| icons | 2 | playground |
| list | 1 | 1 Demo |
| loading | 10 | 5 Demo, 5 Smoke |
| menu | 25 | 11 Demo, 10 Smoke, 4 Diag |
| navrail | 10 | 9 playground, 1 Demo |
| progress | 12 | 6 Demo, 5 Smoke, 1 Guard |
| radio | 12 | 6 Demo, 6 Smoke |
| selectfield | 22 | 11 playground, 10 Smoke, 1 Diag |
| sidesheet | 16 | 8 Demo, 8 Smoke |
| slider | 25 | 12 Demo, 12 Smoke, 1 Guard |
| surface | 4 | playground |
| switches | 10 | 5 Demo, 5 Smoke |
| tabs | 12 | 6 Demo, 6 Smoke |
| textfield | 16 | 15 playground, 1 Smoke |
| theme | 7 | playground |
| tooltip | 10 | 5 Demo, 5 Smoke |

(The epic's "471 of ~900 classes" counts nested and anonymous classes; the 355 above are files,
which is the unit `git mv` and the version gate operate on. Same population.)

### §2.2 Two patterns the epic didn't name [RECOMMENDED — operator sign-off in §10]

The epic's list ("the Showcase, all 14 `playground/` packages, every `*Demo`/`*Smoke`/`*Guard`
main") leaves seven runnable mains behind in `elwha`:

- **Six `*Diag` classes** — `colorpicker/SliderPressDiag`, `menu/MenuDismissDiag`,
  `menu/MenuOverDialogDiag`, `menu/MenuStaleHoverDiag`, `menu/MenuSubmenuFocusDiag`,
  `selectfield/SelectFieldEditableTypingDiag`. Same genre as the smokes: story-time diagnostic
  mains (the menu ones are the epic-#298/#322 focus-bounce probes).
- **`chip/ElwhaChipPlayground`** — the one playground that predates the `playground/`
  subpackage convention and sits at the package top level (it is the example CLAUDE.md's build
  section runs).

Recommendation: **move all seven.** The alternative leaves `elwha` shipping seven runnable mains,
which contradicts the epic's own definition of the library module ("nothing runnable-for-humans").
One honesty note for the semver ruling: stability.md's carve-out names `*Demo`/`*Smoke`/`*Guard`,
"every `playground` subpackage", and `card.fixes` — it does not literally name `*Diag` or a
top-level `*Playground`. The intent plainly covers them (they are the same "build-time artifacts
left behind by the story"), but Phase 3 should tighten the carve-out wording so the licence and
the move agree to the letter.

### §2.3 What stays — and the mechanical proof

192 files stay: the ~25 components, the theme foundation, `ElwhaSurface`, `MaterialIcons`, the
list machinery, the overlay host — exactly the package tables in CLAUDE.md minus their harness
rows. Two mechanical checks, both run for this doc and both to be re-run by Phase 1's move script:

1. **Zero mains stay.** 301 files in `src/main` contain `public static void main`; all 301 are in
   the moved set.
2. **Reference safety** — §2.4.

### §2.4 Reference-safety audit: the split is compile-clean

Method: collect the 339 distinct moved class names, then scan every *staying* file — all 192
main-tree files and all 201 staying test files — for word-boundary occurrences of any of them.
Result: **five hits, all comment-level prose, zero code references.**

| Staying file | Mentions | Kind |
|---|---|---|
| `elwha/fab/ElwhaFabAnchor.java:419` | `ElwhaShowcase` | `//` comment ("mirrors ElwhaShowcase.mountRailOnLayeredPane") — stays true post-split; leave it |
| `test …/progress/ElwhaLinearProgressIndeterminateTest.java` | `ElwhaLinearProgressIndeterminateSmoke` | `{@code}` ×2 — inert |
| `test …/progress/PinnedClock.java` | " | `{@code}` — inert |
| `test …/switches/ElwhaSwitchIconsTest.java` | `ElwhaSwitchIconsSmoke` | `{@code}` — inert |
| `test …/switches/ElwhaSwitchInteractionTest.java` | `ElwhaSwitchInteractionSmoke` | **`{@link}`** — flip to `{@code}` in the move PR |

Nothing here blocks Phase 1: test sources are never processed by the javadoc plugin, so even the
one real `{@link}` cannot fail a build — flipping it to `{@code}` is IDE hygiene, done in the move
PR at zero gate cost (test files outside `testkit/` are exempt from the `@version` gate).
**No Phase-1 pre-story is needed.** `ElwhaFabAnchor` is deliberately left untouched so the move PR
modifies zero library files (§6).

The reverse direction needs no audit: moved code referencing library code is the entire point of
the `elwha-showcase → elwha` dependency.

### §2.5 Resources: nothing moves

A scan of all 355 moved files found **zero** `getResource`/`getResourceAsStream` calls. The
harness surface loads cursors, icons, fonts and palettes exclusively through library APIs
(`MaterialIcons`, `MaterialPalettes`, the package-private `ReorderCursors`), so all of
`src/main/resources` — cursors, the 76 SVGs, the Inter TTFs, both palette tiers — stays in
`elwha`, and the Showcase's palette picker keeps its directory-discovery behavior through
`MaterialPalettes.primary()`/`secondary()` unchanged.

### §2.6 Split packages and JPMS — accept, document, don't repackage

The wholesale move keeps every moved class's package name (`slider/SliderSizesDemo` stays
`com.owspfm.elwha.slider.SliderSizesDemo`, now in the showcase jar). Consequence: ~20 packages
exist in *both* jars. On the classpath that is legal and invisible — and classpath is the only
supported mode for the showcase artifact. On the JPMS module path it is a split-package error.

Decision: **accept it.** `elwha-showcase` gets no `Automatic-Module-Name`; its jar is documented
(stability.md, Phase 3) as a classpath-only application artifact. `elwha` alone keeps its #774
module name and remains module-path-clean — the split actually *improves* the JPMS story for the
only artifact that has one, since the harness classes leave `com.owspfm.elwha`'s module.

The alternative — repackaging moved classes under, say, `com.owspfm.elwha.showcase.*` — is
rejected three times over: it violates the epic's "wholesale, no pruning/no editing" instruction;
it would break any same-package package-private access a smoke relies on; and it turns 355 pure
renames (R100, exempt from the `@version` gate — §6) into 355 content edits, each demanding a
`@version` bump and a review of an import block.

### §2.7 `elwha-showcase`'s dependencies

Measured direct usage: exactly **three** moved files import `com.formdev` types —
`fab/playground/IconSharingColorDemo`, `icons/playground/MaterialIconsSymbolPlayground`,
`theme/playground/FoundationsPanels`, all importing `flatlaf-extras`' `FlatSVGIcon`. No moved file
(including all 51 showcase files) imports anything else from FlatLaf directly.

So the showcase pom declares: `elwha` (compile), `flatlaf-extras` (compile — used directly, don't
lean on transitivity), the `elwha` test-jar (test, §3.2); the junit/assertj/cacio trio arrives
from the parent (§1.2). `flatlaf` and `flatlaf-intellij-themes` flow transitively through `elwha`.

**Incidental finding, out of scope:** `com.formdev.flatlaf.intellijthemes` is imported by *zero*
files in the entire tree (main and test). `flatlaf-intellij-themes` is a compile-scope dep that
nothing compiles against. Removing it would shrink the consumer-inherited set stability.md
documents — a separate decision for a separate issue, not this epic (§9).

## §3. Tests and the testkit

### §3.1 What moves: the 11 showcase-facing tests

`src/test/java` holds 212 files; **11 move** to `elwha-showcase/src/test/java`, all of them the
`com/owspfm/elwha/showcase/` test package: `ComponentWorkbenchTest`, `RawSwingSweepGuardTest`
(the #424 test driving the six `J*SweepGuard.ALLOWLIST`s — it references them by class, so it
must follow them), `ShowcaseCatalogTest`, `ShowcaseFixture`, `ShowcaseLandingTest`,
`ShowcaseLeafConstructionTest`, `ShowcaseNavigationTest`, `ShowcaseRegistryTest`,
`SurfaceControlPanelTest`, `WorkbenchControlApplyTest`, `WorkbenchScaffoldTest`.

The other 201 — the per-component suites and `testkit/` — stay in `elwha`. The §2.4 scan confirms
none of them touches a moved class in code.

### §3.2 Testkit strategy [LOCKED: test-jar, restricted to `testkit/**`]

All 11 moving tests use the testkit (`ThemeExtension` on every fixture, `EdtInterceptor`, `Input`),
so `elwha-showcase` must reach `elwha/src/test/java/com/owspfm/elwha/testkit/`. Two candidates,
per the epic:

**Test-jar dependency — chosen.** `elwha` adds a `maven-jar-plugin` `test-jar` execution with
`<includes>com/owspfm/elwha/testkit/**</includes>`; `elwha-showcase` depends on
`com.owspfm:elwha` with `<type>test-jar</type><scope>test</scope>`. The include restriction keeps
the ~190 per-component test classes out of the showcase test classpath (surefire only discovers a
module's own `testClasses`, so leaking them would be inert but sloppy — restrict anyway). The
test-jar's one classic caveat — it carries no transitive test-scope dependencies — is already
neutralized by declaring junit/assertj/cacio in the parent (§1.2). Fixtures travel cleanly: the
testkit is ordinary classes and JUnit extensions; `@Tag("gui")` lives on test classes in each
module's own tree, not in the testkit, so the tag/tier machinery is untouched by the packaging.

**Third `elwha-testkit` module — rejected on a hard cycle.** The testkit imports
`com.owspfm.elwha.theme.*` (`ElwhaTheme`, `MaterialPalettes`, `ColorRole`, `Mode`,
`MorphAnimator` — measured), so an `elwha-testkit` module needs `elwha` at compile scope. But
`elwha`'s own 190 test classes need the testkit, so `elwha` needs `elwha-testkit` at test scope.
Test-scope edges still count in the reactor graph: that is a cyclic module reference and Maven
refuses to build it. Breaking the cycle would mean evicting `elwha`'s tests into a fourth
module — an absurd price for avoiding one classifier. (A testkit module *without* the `elwha`
dependency is not on the table; `ThemeExtension` is the testkit's core and it installs real
themes.)

One consequence to carry into Phase 1: the `@version` gate's testkit carve-in
(`"/elwha/testkit/" in path`) keeps matching under the new path
`elwha/src/test/java/com/owspfm/elwha/testkit/…` — the substring appears in the package segment
regardless of the module prefix. Verified against the script's `in_scope` logic; §4.4's rewrite
must preserve it.

### §3.3 The two-tier split under the reactor

The surefire config hoists to the parent verbatim and applies per module: each module gets a
Tier A execution (headless, `excludedGroups=gui`) and a Tier B execution (`groups=gui`, forked
JVM, Cacio/native). The load-bearing isolation property — Cacio never sharing a JVM with an
initialized JDK toolkit — is per-JVM and unaffected by module count. Two notes:

- All 11 moving tests are Tier A today (none carries `@Tag("gui")`), so `elwha-showcase`'s Tier B
  execution currently matches zero tests. Surefire treats a filtered-to-empty execution as a pass;
  the tier stays configured so the first gui-tagged showcase test needs no build work.
- JaCoCo stays per-module and report-only. `test.yml`'s artifact upload globs widen (§4.2); a
  cross-module `report-aggregate` is a possible later nicety, not part of this epic.

### §3.4 Fixture path assumptions — checked one by one

Surefire's working directory is the *module* basedir, which is what saves most of this:

- `icons/MaterialIconsInventoryDocTest` (stays in `elwha`) reads
  `Path.of("src","main","resources",…)` and `…/icons/package-info.java` — both resolve under
  `elwha/` as cwd. **Survives.**
- `showcase/ShowcaseFixture.classesRoot()` resolves the compiled-classes root from
  `ElwhaShowcase`'s code source and walks `com/owspfm/elwha` under it, seeding its traversal from
  `com.owspfm.elwha.showcase.*` and expanding only through `.playground.`/`.showcase.` refs.
  Post-split that code source is `elwha-showcase/target/classes`, which contains exactly that
  population; refs into library classes fall out via its `Files.exists` guard, as they do today
  for JDK refs. **Survives; its "the library's own tree hangs off com/owspfm/elwha" comment goes
  stale and gets rewritten in Phase 1** (a modified file → one `@version` bump in the test tree's
  exempt zone — no gate cost).
- `showcase/RawSwingSweep.SOURCE_ROOT = Path.of("src/main/java/com/owspfm/elwha")` — cwd-relative.
  Post-split, run from `elwha-showcase/`, it scans **only the showcase module's main tree**. That
  narrowing is by design defensible: #424's dogfood rule guards the *storefront* surface, all of
  which moves, and every `ALLOWLIST` entry across the six guards names moved files
  (`ThemePlayground.java`, `DialogAccessibilityDemo.java`, `FullScreenDialogA11yDemo.java` — no
  library file is allowlisted, so no entry goes dead). What is lost is incidental scanning of the
  library tree, which today reports zero sites. **Accept the narrowed surface**; if the operator
  wants the library tree kept under sweep, the follow-up is a second root
  (`../elwha/src/main/java/…`) — noted in §9, not recommended.

## §4. CI — all six workflows under the reactor

Headline: **every branch-protection context keeps its exact name** — `build`,
`Test (components + Showcase)` (a name the split makes *more* literal, not less),
`Validate @version and @since tags`, `Validate formatting (Spotless)`, `Validate naming
(Checkstyle)`. No required-check rename, no settings change on `main`.

| Workflow | Verdict | Changes |
|---|---|---|
| `build.yml` | works as-is | `mvn -B clean package -DskipTests` at the root builds the reactor. No file edit needed |
| `test.yml` | path edits | command unchanged (`xvfb-run … mvn -B verify`); artifact globs widen: `target/surefire-reports/**` → `**/target/surefire-reports/**`, `target/site/jacoco/**` → `**/target/site/jacoco/**` |
| `validate-style.yml` | works as-is | `spotless:check` and `checkstyle:check` at the root run reactor-wide; the Checkstyle change is pom-side (§1.3), not workflow-side |
| `validate-versions.yml` | **workflow as-is, script broken — fails open** | §4.4 |
| `pages.yml` | scope + path edits | build only the library's docs: `mvn -B -pl elwha javadoc:javadoc` (no `-am` needed — `elwha`'s deps are all external); upload path → `elwha/target/reports/apidocs`. Publishing library-only javadoc is the point of the epic: the storefront classes leave the API reference |
| `publish.yml` | extended | §5.3 |

### §4.4 `validate-versions`: the script does not survive, and it fails open

`scripts/update_javadoc_version.py` has three path assumptions, read directly:

1. `changed_java_files()` runs `git diff --name-status … -- src/ test/`. Git pathspecs are
   root-anchored: `src/` matches nothing once sources live at `elwha/src/…` and
   `elwha-showcase/src/…`. The diff comes back empty, the script prints
   `INFO: No Java files modified or added … skipping validation.` and **exits 0**. Every
   post-split PR would pass the required check without a single file being validated — a green
   gate validating nothing.
2. `in_scope()` tests `path.startswith("src/test/")` for the test-exemption — same root-anchoring
   problem; under the reactor *no* path starts with `src/`, so (once the pathspec is fixed) every
   test file would be treated as main-tree and demanded a `@version`.
3. The legacy tree-wide mode walks `Path('src')` / `Path('test')` — finds nothing at the root.

Fix (small, mechanical): pathspec → `elwha/src/ elwha-showcase/src/` (explicit module list, matching
the repo's preference for greppable literals over glob magic); `in_scope` → exempt any path
containing `/src/test/` unless it contains `/elwha/testkit/` (which keeps matching the testkit's
package segment, §3.2); legacy mode walks `<module>/src` for both modules. The `since_on_base`
`git show base:path` calls already use repo-relative diff paths and survive untouched, as does the
R100 pure-rename exclusion the move itself depends on (§6).

**Phasing correction:** the epic slotted "version-gate" into Phase 2, but a fail-open required
check is not a window to leave ajar between phases — the script fix rides the Phase-1 move PR
(it is a script edit, not a workflow edit, so Phase 2's workflow scope is untouched). Flagged in
§10.

## §5. Release & publish — the two-artifact tag

### §5.1 What one `mvn -B clean deploy` from the root now publishes

Seven artifacts across three coordinates, one workflow run:

| Coordinate | Artifacts |
|---|---|
| `com.owspfm:elwha-parent` | pom (consumers of `elwha` need it resolvable — module poms reference it) |
| `com.owspfm:elwha` | jar, `-sources`, `-javadoc`, `-tests` (the §3.2 test-jar) |
| `com.owspfm:elwha-showcase` | jar (plain, `Main-Class` manifest), `-sources`, `-javadoc` (§1.2), `-app` (shaded, §5.2) |

### §5.2 The runnable jar [LOCKED: shade, attached classifier `app`]

The showcase needs `elwha` + FlatLaf (+ jsvg) on its classpath; a bare `java -jar` on the plain
jar dies at the first missing class. Three candidates:

- **Class-Path manifest + `lib/` directory** — the evaluator downloads a zip, extracts, and must
  keep the layout intact. The epic's evaluation story is "download `elwha-showcase.jar`, run it";
  a zip-with-folder is a worse story for zero implementation savings. Rejected.
- **assembly `jar-with-dependencies`** — works, but shade supersedes it: no resource
  transformers, clumsy naming, and no relocation escape hatch if it's ever needed. Rejected.
- **maven-shade-plugin — chosen**, with `shadedArtifactAttached=true` and
  `shadedClassifierName=app`, so the *plain* jar remains the module's main artifact. This is the
  detail that makes both publish channels honest at once: Maven consumers resolving
  `com.owspfm:elwha-showcase` get a normal jar with normal transitive deps (no duplicated FlatLaf
  classes on their classpath), while evaluators get `elwha-showcase-<v>-app.jar` — one file,
  double-clickable via `java -jar`, no Packages auth.

Shade config: `ManifestResourceTransformer` restating
`Main-Class: com.owspfm.elwha.showcase.ElwhaShowcase`; `ApacheLicenseResourceTransformer` +
`ApacheNoticeResourceTransformer` so FlatLaf's and jsvg's notices merge into the fat jar's
`META-INF` next to Elwha's own (#774 already rides LICENSE/NOTICE into every module jar, §1.2);
`ServicesResourceTransformer` for safety (FlatLaf ships service files). No relocation — the fat
jar is an application, not a dependency.

Size sanity: today's single jar is ~2.8 MB with all deps external; the fat jar adds FlatLaf
(~1 MB), extras, themes and jsvg — call it 6–8 MB. Fine for a Release asset.

### §5.3 `publish.yml` changes

1. `permissions: contents: read` → **`contents: write`** (required to create the Release and
   upload the asset) — the one permission change in the whole epic; packages: write stays.
2. Tag/pom check and CHANGELOG check: unchanged (§1.4).
3. `mvn -B clean deploy -DskipTests`: unchanged text, now deploys §5.1's seven artifacts.
4. New final step — create the Release and attach the runnable jar:

```yaml
- name: Create GitHub Release and attach the runnable Showcase
  env:
    GH_TOKEN: ${{ secrets.GITHUB_TOKEN }}
  run: |
    TAG="${GITHUB_REF#refs/tags/}"
    VERSION="${TAG#v}"
    gh release create "$TAG" --verify-tag --title "$VERSION" \
      --notes "See CHANGELOG.md §[$VERSION]." || true
    gh release upload "$TAG" \
      "elwha-showcase/target/elwha-showcase-${VERSION}-app.jar" --clobber
```

(`|| true` on create keeps a workflow re-run idempotent when the Release already exists;
`--clobber` makes the upload idempotent. Release-notes curation stays a human act per the
runbook — the workflow only guarantees the asset is there.)

Remember the 1.0.0 lesson recorded in the runbook: **a tag runs the workflow at its own ref**, so
these publish.yml changes must be merged to `main` before the first post-split tag — which
Phase 2 does by construction.

### §5.4 Release-runbook changes (`docs/development/release-runbook.md`)

- §3 "The release commit": the pom edit becomes `mvn versions:set -DnewVersion=X.Y.Z
  -DgenerateBackupPoms=false` touching three poms (§1.4); the "There is no parent POM, so it is
  the first `<version>`…" paragraph is deleted — it becomes false the day Phase 1 merges.
- §2 pre-flight: the Showcase-launch check gains the second form (`java -jar
  elwha-showcase/target/elwha-showcase-<v>-app.jar` after `mvn -q package -DskipTests`) — the
  release now ships that exact jar, so pre-flight should run what ships.
- §5 "What the publish workflow checks": table gains the Release-create/upload step and the
  seven-artifact list (§5.1).
- §6 post-publish verification: the Packages check widens to all three coordinates
  (`com.owspfm.elwha-parent`, `com.owspfm.elwha`, `com.owspfm.elwha-showcase`); the "A release on
  GitHub" paragraph inverts — the workflow now creates it, the human verifies the `-app.jar`
  asset downloads and launches; `mvn dependency:get` check unchanged (plus optionally the
  showcase coordinate).

### §5.5 Semver

Restating the epic's ruling for the record: removing the 355 harness files from the `elwha` jar is
exactly what stability.md's carve-out reserved the right to do ("free to change or disappear in
any release"); the known consumer imports none of them (§2.4 proves nothing *could* be silently
load-bearing — no staying code references them either). `elwha-showcase` is a new artifact.
**Minor bump** — the split ships in `1.1.0` (or whatever minor v1.1.0's wave becomes), not a
major. The seven §2.2 files ride the same licence in spirit; Phase 3 tightens the wording
(§7, §10).

## §6. History preservation

The #60 precedent (`481fa0f` — the flat-`src/` → Maven-layout migration) is the playbook:
directory-level `git mv`, one commit, `git log --follow` traverses. The Phase-1 move commit:

1. Add the root parent pom, both module poms, `.mvn/maven.config`.
2. `git mv src elwha/src` — the bulk hop.
3. `git mv` the 355 main files + 11 test files from `elwha/src/…` to `elwha-showcase/src/…`,
   preserving package paths (script-generated from §2.1's bucket rules; the script also re-runs
   §2.3's zero-mains and §2.4's zero-references checks as assertions).
4. The pom-side path re-anchors (§1.3) and the exec wiring (§1.5) — same commit, because a commit
   where Checkstyle's config path or the LICENSE resource dangles is a commit where `mvn verify`
   is red.

Both hops for a twice-moved file collapse into a single rename within the one commit. Because file
*content* is untouched, every move is R100 — and the version-gate script **explicitly excludes
pure renames** (its docstring cites #60 as the reason), so the move PR demands zero `@version`
bumps. That exclusion is load-bearing for this epic and is preserved verbatim by the §4.4 script
fix. The only content edits riding the PR are in the `@version`-exempt test zone: the
`{@link}`→`{@code}` flip (§2.4) and the `ShowcaseFixture` comment refresh (§3.4).

Post-merge spot-check (goes in the PR description): `git log --follow
elwha-showcase/src/main/java/com/owspfm/elwha/showcase/ElwhaShowcase.java` reaches pre-split and
pre-#60 history; same for one moved smoke and one staying component.

## §7. Docs & CLAUDE.md

**CLAUDE.md** (Phase 2, with the plumbing it describes):

- *What this repo is*: two-artifact framing, per-module file counts, `elwha-parent` coordinates.
- *Build & run*: jar paths (`elwha/target/…`), the `java -jar …-app.jar` line; the `exec:java`
  one-liners survive verbatim (§1.5) — the section gains a sentence saying *why* they still work.
- *Source layout*: paths gain the module prefix; the package tables split cleanly — the harness
  row of the story ("Nearly every component package also carries `*Demo`…") moves from a
  disclaimer into the `elwha-showcase` framing; the *Legacy & harness* table (card/fixes,
  showcase) is re-headed as the showcase module's contents.
- *Tests*: testkit path gains the module prefix; the test-jar arrangement gets one line.
- *Release process*: `versions:set`, seven artifacts, Release auto-created.
- *Conventions → Milestones*: unchanged mechanics; one note that the gate's pathspec is now
  module-aware.

**`docs/development/`**: `release-runbook.md` per §5.4; `testing.md` §CI and §testkit paths;
`versioning.md`/`versioning-playbook.md` wherever they cite `src/` paths or the script invocation.

**`docs/consumer/` (Phase 3 — scoped here, written there):**

- `stability.md`: the carve-out section inverts — harness classes are no longer "not API despite
  being in the jar", they are *not in the jar*; name `*Diag`/`*Playground` explicitly while
  rewriting (§2.2); the "What counts as public API" note about Javadoc documenting harness
  packages dies (the published API reference becomes library-only, §4's pages change); the
  dependency-stance section gains the `elwha-showcase` artifact story (classpath-only, split
  packages, not covered by the API promise — evaluate with it, don't build on it).
- `install.md`/`quick-start.md` + `README.md`: the evaluation story leads with "download
  `elwha-showcase-<v>-app.jar` from the Release, `java -jar` it" — no Packages PAT needed just to
  *look* at the library; the Maven install path is unchanged for actually adopting it.
- `components.md`: unaffected (it documents library classes only).

## §8. Phasing → stories (Phases 1–3, one PR each)

- **P1-S1 — the reactor, library-only** (`refactor(#nnn)`): root parent pom + `elwha` module pom;
  `git mv src elwha/src`; `.mvn` marker; §1.3 re-anchors; §1.5 exec wiring (parent half);
  minimal CI path edits that Phase 1 itself breaks — `pages.yml` (`-pl elwha` + upload path),
  `test.yml` artifact globs, and the §4.4 script fix. Reactor of one module + parent; `mvn verify`
  green; required contexts green. *(The epic's invariant — contexts stay green throughout — is
  why these three CI touches cannot wait for Phase 2.)*
- **P1-S2 — the wholesale move** (`refactor(#nnn)`): `elwha-showcase` module pom (deps §2.7,
  `Main-Class`, exec override, test-jar consumption); `elwha`'s test-jar execution; script-driven
  `git mv` of 355 + 11 files per §2.1/§3.1 with the §2.3/§2.4 assertions re-run; the two exempt
  test-zone comment edits; suite green in both modules. *(S1+S2 can collapse into one PR if the
  operator prefers a single restructure commit — the two-step split exists for reviewability:
  S1 is poms-and-paths, S2 is a move manifest.)*
- **P2-S3 — publish plumbing** (`ci(#nnn)`): shade config (§5.2); `publish.yml` §5.3
  (permissions, Release create/attach); `release-runbook.md` §5.4. Dry-run: `mvn -q clean package`
  then launch the `-app.jar` by hand.
- **P2-S4 — developer docs & gate polish** (`docs(#nnn)`): CLAUDE.md §7 updates; `testing.md`,
  `versioning.md`/`-playbook.md` path refresh; the script's legacy tree-wide mode made
  module-aware (the PR-scoped mode already fixed in P1-S1).
- **P3-S5 — the storefront docs** (`docs(#nnn)`): README + `docs/consumer/` per §7, including the
  stability.md carve-out/artifact rewrite. Lands before the v1.1.0 tag so the Release the
  evaluation story points at actually carries the jar it promises.

Stories get filed when this doc lands (progressive filing, house convention), every one milestoned
`v1.1.0` and added to Project #5.

## §9. Out of scope (every cut named)

- **Pruning dead demos/smokes** — the epic explicitly defers it; the move is wholesale.
- **`flatlaf-intellij-themes` removal** (§2.7) — zero usage tree-wide, but it changes the
  documented consumer dependency set; file separately if wanted.
- **Re-widening `RawSwingSweep` to scan the library tree** (§3.4) — the narrowed surface is the
  #424 intent; a two-root scan is possible later if wanted.
- **JaCoCo cross-module aggregation** (§3.3) — report-only stays report-only.
- **Repackaging moved classes / a showcase `Automatic-Module-Name`** (§2.6) — rejected, not
  deferred.
- **CHANGELOG mechanics** — unchanged; one root file, publish.yml's grep untouched.

## §10. Open questions for the operator

1. **§2.2 — the seven unlisted files** (six `*Diag` + `ElwhaChipPlayground`): confirm they move
   with the rest. Recommendation: yes — leaving them makes `elwha` "runnable-for-humans" again.
2. **§8 — Phase 1 as two PRs (S1 scaffolding, S2 move) or one?** Recommendation: two, for
   reviewability; both leave `main` green.
3. **§4.4 — the version-gate script fix riding Phase 1** instead of the epic's Phase-2 slot:
   confirm. Recommendation: yes — the check fails open the moment sources move.
4. **§1.2 — symmetric sources/javadoc jars for `elwha-showcase`**: keep the doclint ratchet on
   the harness files (recommended), or jar-only and let the discipline lapse?
5. **§5.3 — `publish.yml` gains `contents: write`** and creates the Release itself: confirm the
   permission change.
6. **§2.7 — `flatlaf-intellij-themes`**: file the removal candidate as its own issue?

## Related

- Epic #779; this doc is story #783 (Phase 0)
- `docs/consumer/stability.md` — the carve-out that licenses the minor bump (§5.5)
- `docs/development/release-runbook.md` — rewritten by P2-S3 (§5.4)
- `scripts/update_javadoc_version.py` — the §4.4 fix; its R100 exclusion is what makes §6 cheap
- `481fa0f` / #60 — the source-layout migration this epic's `git mv` strategy repeats
- `docs/research/elwha-dogfood-sweep-424.md` — the sweep whose guarded surface §3.4 re-scopes
- #774 — the jar-metadata patch whose `Automatic-Module-Name`, LICENSE/NOTICE resource and
  `legacyMode` settings this design re-homes
