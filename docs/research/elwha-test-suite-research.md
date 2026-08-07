# Elwha regression-suite framework selection (#439 → epic #438)

**Status:** RECOMMENDED · **Story:** #439 (S1 of epic #438) · **Date:** 2026-08-06

The lib has zero test infrastructure; this doc picks the foundation the suite is built on, records
the evidence, and proposes the remaining #438 stories. The selections were validated by a worked
spike in this branch — `src/test/java` exists as of this PR and `mvn test` runs both tiers green.

**The selection in one line:** JUnit 6 + AssertJ Core on a **two-tier execution split** — pure
`java.awt.headless=true` for the bulk of the suite, and a display-bearing `gui` tier in a separate
forked JVM for the minority of tests that need real focus, real input, or real windows. The gui
tier's display stack is **per-platform behind one switch** (`testkit/GuiToolkit`): the **Cacio
virtual toolkit** on macOS/Windows dev machines, the **native toolkit under Xvfb** on Linux CI —
same tests, zero per-platform test code. No AssertJ-Swing, no golden-image backbone. (Why not
Cacio everywhere: the JDK 21 X11 font natives make Cacio structurally crash on Linux the moment a
font registers — the full derivation is §5b, paid for in hs_err logs.)

---

## 1. Constraints that shaped the selection

- **CI is headless Linux; dev is macOS.** All four workflows run on `ubuntu-latest` with temurin 21
  and no display. Anything display-dependent must either work headless or bring its own display.
- **`build` is a required check running `mvn -B clean package`** — the moment a test tree exists,
  the suite is a merge gate. Flakiness is therefore a day-one design constraint, not a
  nice-to-have: every pattern below is chosen for determinism first.
- **Coupling stance:** runtime deps stay Swing + FlatLaf only. Everything here is `<scope>test</scope>`.
- **The test oracles already exist** — the `docs/research/elwha-*-design.md` token sheets, geometry
  tables, state matrices, and event contracts. The suite asserts against *resolved roles*
  (`ColorRole.PRIMARY.resolve()`), not hardcoded hex, so palette changes don't invalidate tests.

## 2. The incumbent: what the `*Smoke` mains prove (and where they stop)

Surveyed in full; the numbers matter for sizing #438.

**Inventory.** 123 `*Smoke`/`*Guard` mains, 20,553 LOC, all in `src/main/java` in the same package
as the component under test. 113 are headless (literal first statement:
`System.setProperty("java.awt.headless", "true")`); the tooltip/sidesheet families self-skip their
display-dependent halves. Nothing runs them in CI; invocation is one JVM per smoke via `exec:java`.

**Empirical run (2026-08-06, macOS, JDK 21): 112/113 pass, 196s serial.** The one failure —
`progress/ElwhaLinearProgressIndeterminateSmoke` — is a reproducible timing flake (fails ~3/5 runs:
it samples a live wall-clock animation for 1900ms and allows only 3 dropped frames). The incumbent
is green, slow to run serially (JVM startup dominates at ~1.7s/smoke), and already demonstrates the
flake class the suite must design out.

**What the smokes proved that the suite keeps:**

- **Headless render + pixel probe works.** Offscreen `BufferedImage` + `paint(g)` on an unrealized,
  manually-sized component (`setSize` + `doLayout`, no peer, no EDT contention), opaque
  `ColorRole.SURFACE` ground pre-filled so translucent state layers composite predictably,
  per-channel ±10 tolerance (`near`), and source-over blend math (`mix`) to predict state-layer
  results. This is Tier A's render path, verbatim.
- **The theme bootstraps headless in two lines** — `ElwhaTheme.install(...)` with the baseline
  palette; Inter registers via `GraphicsEnvironment.registerFont` with zero headless issues across
  all 113 runs. Mode switching mid-test is just a second `install()`.
- **A11y is assertable headless** — 32 smokes already assert roles, state sets,
  `AccessibleSelection`, and accessible-event counts with no realized peer.
- **Same-package placement is load-bearing** — smokes use package-private geometry seams (e.g.
  `ElwhaTabs.currentIndicatorRect()`); `src/test/java` mirrors of the same packages keep that
  access without widening any API.
- **The peerless-`MouseEvent` trap:** the `(x,y)`-only ctor calls `getLocationOnScreen` and NPEs on
  an unrealized component; the explicit-absolute-coords ctor is required.

**What the smokes lack, which the framework supplies:** one assertion contract (five hand-rolled
`check` signatures exist, two with *reversed argument order*), full-run tallies instead of
fail-fast `System.exit(1)`, machine-readable reports, CI execution, shared fixtures (102 duplicated
`check`s, 11 `near`s, 6 `mix`es), and single-JVM amortization (JUnit runs the spike's 12 tests in
~1s where 12 smoke mains would cost ~20s).

## 3. Candidate matrix

Verified against central.sonatype.com artifact pages and upstream repos on 2026-08-06. (Caveat for
future re-checks: the `search.maven.org` solrsearch index was stale by ~15 months in this
environment; don't trust it for "latest version" claims.)

| Candidate | Version | License | Maintained | JDK 21 | Headless CI | Verdict |
|---|---|---|---|---|---|---|
| JUnit (Jupiter) | 6.1.2 (2026-07) | EPL-2.0 | active | yes (needs 17+) | n/a | **adopt** |
| AssertJ Core | 3.27.7 (2026-01) | Apache-2.0 | active | yes | n/a | **adopt** |
| Cacio `cacio-tta-jdk21` | 2.0 (2026-05) | GPL-2.0 + classpath exception | active | yes (dedicated artifact) | **yes — virtual toolkit** | **adopt (Tier B)** |
| maven-surefire | 3.5.4 | Apache-2.0 | active | yes | n/a | **adopt** |
| JaCoCo | 0.8.15 (2026-06) | EPL-2.0 | active | yes | n/a | adopt later, report-only (S2) |
| image-comparison | 4.5.0 (2024-06) | Apache-2.0 | quiet, low-risk | yes | yes | optional, golden tier only |
| AssertJ-Swing upstream | 3.17.1 (2020) | Apache-2.0 | **abandoned 2020** | unverified | **no — Robot-bound** | reject |
| AssertJ-Swing fork (`tokyo.northside`) | 4.0.0-beta-3 | Apache-2.0 | active-ish | docs say 11/17 only | **no — Robot-bound** | reject (revisit if fixture ergonomics ever needed) |
| Jemmy v2 | dormant | **GPL-2.0 (no exception)** | dormant | unverified | no | reject |
| Xvfb (+ fluxbox) on Actions | preinstalled | — | n/a | n/a | Linux-only | fallback only (§10) |

License note: Cacio's POM declares "GPL2 with classpath exception" (openjdk.java.net/legal/gplv2+ce)
— the same license as OpenJDK itself — and it is test-scoped, so nothing GPL touches the shipped
jar. Jemmy has no such exception, which alone disqualifies it.

## 4. The two-tier architecture

The requirements don't all live at the same level, and serving them from one JVM config is the main
way this goes wrong:

- **Tier A — headless (default, untagged, the bulk of the suite).** `java.awt.headless=true`, no
  extra toolkit, no flags. Covers: EDT-disciplined construction/mutation, model/listener/selection
  contracts, geometry and `getPreferredSize`, shadow-inset/body-rect contracts, offscreen pixel
  probes light *and* dark, a11y shapes, focus-traversal *policy* order
  (`FocusTraversalPolicy.getComponentAfter` is a plain object-graph query — headless-safe), InputMap
  binding checks, synthetic `dispatchEvent` input. Identical semantics on macOS and Linux.
- **Tier B — `@Tag("gui")` (deliberate minority).** A real display stack: real window peers, real
  `KeyboardFocusManager` arbitration, real `java.awt.Robot` input, screen capture. Covers what
  headless *cannot represent*: focus **ownership** (roving tab stops, `ownsFocus`/`takesFocus()`
  opt-outs, overlay focus handoff), Robot-driven gestures, window realization, overlay placement.
  Tests are written toolkit-agnostically (plain Robot + Swing APIs); which stack hosts them is
  decided per-platform by `testkit/GuiToolkit` reading `elwha.guiTier.toolkit`:
  - **`cacio` (default — macOS/Windows dev):** Cacio's virtual toolkit. Deterministic screen
    (`cacio.managed.screensize`), no display theft (native Robot would drag the developer's real
    cursor around), zero setup.
  - **`native` (Linux, selected by the `linux-gui-native` Maven profile):** the platform toolkit
    with a real display — Xvfb on CI (`xvfb-run -a mvn …` in `build.yml`), the desktop on a Linux
    dev machine. Forced by the JDK 21 font-native constraint derived in §5b.

Two hard rules make the split load-bearing rather than stylistic:

1. **Separate forked JVMs per tier** (two surefire executions; Tier B with `reuseForks=false`).
   The AWT toolkit is a per-JVM singleton, and Cacio documents a segfault when it races an
   already-initialized JDK toolkit. Fork separation makes that structurally impossible.
2. **Dispatched `FocusEvent`s are banned as a focus-testing idiom.** They flip painted focus rings
   while `KeyboardFocusManager.getFocusOwner()` stays null — the test then verifies its own event
   construction, not the component. Every focus bug this lib has actually shipped (RadioButton
   roving stop, SelectField `focusInitial`, the ColorPicker sampler JWindow-focus Esc bug, the Menu
   V2 focus-bounce cascade) lived in the machinery synthetic FocusEvents bypass. Focus *ownership*
   assertions go to Tier B; focus *policy* assertions stay in Tier A.

Keyboard input splits the same way: Tier A asserts the `WHEN_FOCUSED` InputMap binding and invokes
the mapped action (covering the wiring the smokes skipped — they called actions by name); Tier B
presses the real key.

## 5. The spike (in this branch, all green)

`mvn test` on JDK 21 runs both tiers; the PR's own CI run is the ubuntu-latest proof.

- **`switches/ElwhaSwitchInteractionTest`** (Tier A, 10 tests, ~0.4s): the
  `ElwhaSwitchInteractionSmoke` §7 contract as JUnit — click/drag/cancel event contracts through
  real `Component.dispatchEvent` (an upgrade over the smokes' direct listener iteration — it
  exercises the component's own listener registration and event masks), InputMap-verified Space
  semantics, disabled guards, the full a11y shape (role/name/CHECKED/value/action), the hover
  state-layer pixel probe via the shared `mix` math, and a `@ParameterizedTest` over
  `Mode.LIGHT/DARK` proving the mode-matrix shape costs one annotation.
- **`switches/ElwhaSwitchGuiTest`** (Tier B, 2 tests, ~0.5s Cacio / ~3.8s Xvfb): FlatLaf +
  `ElwhaTheme` install, a `JFrame` realizes and *gains focus*, Robot Space toggles the focused
  switch, **Tab moves real focus** (asserted via `isFocusOwner` *and* KFM agreement), Robot click
  toggles through the real pipeline, and `Robot.createScreenCapture` shows the selected track at
  resolved `PRIMARY` within ±10/channel. Verified green on **both** hosting stacks: Cacio on
  macOS, and the native toolkit under bare Xvfb (no window manager — frame focus and Tab traversal
  held without one) in a Linux container matching the CI runner.
- **`testkit/`** seeds the shared fixture library: `EdtInterceptor` (the JUnit user guide's
  documented EDT interceptor, extended to lifecycle + `@ParameterizedTest` templates) and `Pixels`
  (the smokes' render/`near`/`mix` idiom, shared once, with hex-diff failure messages).

Spike-time findings worth recording: `dispatchEvent` works headless on unrealized components (the
listener-iteration workaround is unnecessary); JUnit 6.1.2 + surefire 3.5.4 resolve and run with no
provider friction; Cacio 2.0's `@ExtendWith(CacioExtension.class)` needs *no* manual
`awt.toolkit`/`java.awt.graphicsenv` properties (most tutorials are stale — they describe 1.18);
counterintuitively Tier B requires `java.awt.headless=false`; and Cacio pulls the abandoned
`assertj-swing-junit:3.17.1` transitively — harmless at test scope, and excluding it would break
`@CacioTest`'s screenshot path, so it stays.

### 5b. The Linux font-native crash — why the gui tier is per-platform

The first CI run went 10/10 on Tier A and **SIGABRT'd the Tier B fork** (exit 134, "Aborted (core
dumped)", zero tests reported) — after both tiers had passed on macOS. The diagnosis is recorded
here in full because every intermediate hypothesis *looked* fixable and wasn't; the next person
should not re-walk this path.

**Repro methodology (reusable):** the runner's crash dump was inaccessible, so the environment was
rebuilt locally in Docker (`maven:3.9-eclipse-temurin-21` + X client libraries + fontconfig, **no
X server** — the runner's exact shape). That converted the crash into local `hs_err_pid` logs with
full native frames. `build.yml` now uploads `surefire-reports` + `hs_err_pid*` on failure so future
CI-only crashes are diagnosable directly.

**Root cause, from the hs_err frames:** Cacio needs `java.awt.headless=false`; with that set on
Linux, `Toolkit.<clinit>` binds AWT's native backend to **`libawt_xawt`** (per-JVM, one-time), and
the JDK's hard-wired `X11FontManager` answers font-path queries through xawt natives that call
back through JNI state which **only real X11 display initialization registers**. Under Cacio, X11
display init never runs, so the first `SunFontManager` construction — triggered by
`Typography.defaults()` registering Inter, i.e. by any `ElwhaTheme.install` — dies inside
`Java_sun_awt_FcFontManager_getFontPathNative` (a null callback through
`jni_CallStaticVoidMethod`).

**Dead ends, so nobody retries them:**
1. `-Dsun.font.fontmanager=sun.awt.FcFontManager` — **the property no longer exists.** JDK 21's
   `FontManagerFactory` delegates to `PlatformFontInfo.createFontManager()`, a hard-wired platform
   switch (probe-confirmed).
2. Pre-warming the font caches under a temporarily-headless environment (LauncherSessionListener +
   reflective `GraphicsEnvironment.headless` flip) — the enumeration still crashed identically,
   and the ordering games it forces (which backend `libawt` binds first vs. `AWTEvent.initIDs`
   registration, each one-shot and mutually exclusive) make every fix create the next crash.
3. `-Dsun.java2d.fontpath=…` — `SunFontManager`'s constructor calls the native platform-path
   lookup regardless (hs_err shows it from `SunFontManager$2.run`).
4. Xvfb *underneath Cacio* — a display exists, but Cacio still owns the toolkit, X11 display init
   still never runs, same crash. The display alone is not the missing piece; the init is.

**Conclusion:** Cacio + JDK 21 + anything that constructs the font manager (any FlatLaf/Elwha
theme install) is structurally broken on Linux. Not a flag, not an ordering trick. Hence
`GuiToolkit`: same gui tests, Cacio where it shines (dev machines), native-toolkit-under-Xvfb
where Linux forces it (CI) — where X11 init runs for real and the font natives are on home turf.

## 6. Determinism rules (the flake budget is zero)

**Fonts.**
- Never let a logical font (`Dialog`, `SansSerif`…) into an assertion path — logical-name
  resolution goes through the platform's fontconfig and shifts with runner-image contents. Elwha
  bundles Inter and `Typography.defaults()` registers it; tests use the theme's fonts, full stop.
- Never probe pixels inside a text glyph's box. Assert *chrome* (surfaces, borders, indicators,
  silhouettes — Elwha's own painting) or assert *text geometry* (FontMetrics widths, label bounds,
  baselines — which is what would have caught the #305 repaint storm, a width mismatch).
- Pin `KEY_TEXT_ANTIALIASING`/`KEY_ANTIALIASING` explicitly when a raster will be probed near text;
  desktop-derived hints differ per OS and sub-pixel LCD modes produce colored fringes.
- macOS and Linux rasterize identically-versioned fonts differently. Cross-OS pixel identity is
  not a goal; per-OS determinism is.

**Time.**
- No wall-clock sampling of live animations — that is exactly the incumbent's one flake.
  `MorphAnimator.setReducedMotion(true)` in fixtures (it also *auto-detects the host OS
  accessibility setting at class load*, so an unpinned run can behave differently on a dev's
  machine than CI — pin it always). Stateless painters (`RipplePainter` et al.) take injected
  progress; animation-frame assertions inject the clock value instead of racing a Timer.
- Where real asynchrony exists (Tier B), poll-with-deadline on the EDT (`waitFor`), never sleep-and-hope.
- The 21 per-component `javax.swing.Timer`s and the `isDisplayable()` animation gate are the
  incumbent reality the suite works *around* pre-#440 (reduced-motion + progress injection cover
  it); an injectable clock is a #440-era refactor candidate, not this epic's scope.

**JDK.** CI pins temurin `21` (latest patch). Patch releases have changed AWT behavior before
(21.0.4's headless auto-detection change, JDK-8336862); both surefire executions therefore set
`java.awt.headless` *explicitly* rather than trusting detection. If patch drift ever bites anyway,
pinning the full patch version in CI is the escalation.

**Golden images** are deliberately *not* the backbone: a probe against a resolved role survives an
intentional 1px padding change and fails readably; a bitmap does neither. If shape-silhouette cases
(shape-morph paths, body/shadow agreement — the FAB Phase 3 class of bug) later justify a golden
tier, the rules are: `image-comparison` (Apache-2.0) with small non-zero tolerance, text regions
masked, baselines generated *by CI on Linux only*, never on a dev machine.

## 7. Suite layout and conventions

```
src/test/java/com/owspfm/elwha/
  testkit/                    ← shared fixtures (this PR: EdtInterceptor, Pixels; S2 grows it)
  <component>/                ← mirrors main packages; same-package = package-private seams stay
      <Component><Aspect>Test.java     (Tier A, untagged)
      <Component>GuiTest.java          (Tier B, @Tag("gui"))
```

- Test classes and methods are package-private (JUnit 6 idiom); `testkit` classes are public.
- House Javadoc convention (`@author`/`@version`/`@since`) applies to test files — the `@version`
  gate already rglobs all of `src/`, so this PR complies. **Open question for S2 (operator call):**
  keep that discipline suite-wide, or scope `scripts/update_javadoc_version.py` to `src/main` +
  `testkit` so per-test files stop paying the re-tag tax on every touch.
- Style gates now cover the test tree (this PR widens Spotless includes and Checkstyle
  `sourceDirectories` to `src/test/java`) — test code is house-style code.
- Assertion messages follow the smokes' plain-English contract style ("Space release commits the
  toggle") so a CI failure reads like a spec violation, not a stack trace.

## 8. CI-job shape

Already gated: `build` runs `xvfb-run -a mvn -B clean package` (the wrapper hosts the gui tier per
§5b; Xvfb is preinstalled on ubuntu runners) and now uploads `surefire-reports` + `hs_err_pid*` as
a `test-failure-diagnostics` artifact on failure. This PR's own CI run is the first Linux
execution of the suite. S2 adds the deliberate version:

- A dedicated `test` job (name: `Test (components + Showcase)`) running `mvn -B verify`, uploading
  `target/surefire-reports` on failure, becoming a required check alongside build/format/naming.
- JaCoCo 0.8.15 wired report-only (no threshold gate while coverage grows from zero). One known
  trap: JaCoCo injects via `argLine`, and the Tier B execution *sets* `argLine` — S2 must use
  `@{argLine}`-style property chaining or coverage silently drops to zero for that tier.
- The `*Smoke` mains stay untouched and un-run, per the epic's scope line.

## 9. Proposed #438 stories (file at Phase 0, per the epic)

The per-family split the epic anticipated, ordered so shared fixtures exist before the families
that need them; each family story owns its components' Tier A matrix (render/geometry/input/a11y/
light+dark/RTL/reduced-motion/re-skin dimensions from the epic) plus its Tier B focus/gesture cases.

- **S2 — harness & CI:** grow `testkit` (theme-install extension, input helpers, `waitFor`, a
  `ColorAssert`), dedicated `test` workflow + required check, JaCoCo report-only, surefire-report
  artifacts, the `@version`-gate scoping decision, `docs/development/testing.md` conventions page.
- **S3 — theme/foundation:** token resolution, `FlatLafKeyMapping`, `MaterialPalettes` discovery +
  `PaletteLoader`, Typography, mode switch + re-skin, `surface`/`icons`.
- **S4 — actions:** button, iconbutton, buttongroup, fab + anchor (incl. morph via injected progress).
- **S5 — selection controls:** checkbox, radio (roving stop → Tier B), switches, chip.
- **S6 — fields:** textfield, selectfield (focusInitial → Tier B), slider, colorpicker.
- **S7 — containers & lists:** card + card/list, chip/list, `list/` contract. (Coordinates with
  #67/#69 — if `ElwhaItemList<T>` lands first, S7 tests the unified class instead of two families;
  either way S7's model/selection tests become #69's regression net.)
- **S8 — overlays:** overlay host chain, menu (+submenus), tooltip, dialog + fullscreen, sidesheet
  — the family where Tier B earns its keep (placement, focus handoff, outside-press routing).
- **S9 — navigation & status:** navrail, appbar, tabs, badge + anchor, progress + loading — and the
  suite-ified, *deterministic* replacement for the indeterminate-progress flake as the showcase case.
- **S10 — Showcase:** registration completeness, workbench/gallery construction, control apply
  paths (per #441's seeds).

Sizing signal for the operator: the spike ported ~60% of one smoke's assertions in ~180 LOC of
test at roughly 1:1 effort with writing the smoke would have been; 123 smokes exist but the suite
is *not* a smoke port — it tests contracts from the design docs, reusing smoke assertions where
they encode those contracts.

## 10. Rejected alternatives, for the record

- **AssertJ-Swing (either lineage):** upstream abandoned since 2020 with no Jupiter module; the
  live fork documents JDK 11/17 only and is beta. Both are `Robot`-bound — headless-incompatible —
  and their fixture-lookup ergonomics solve an app-testing problem a component library doesn't
  have (tests hold direct references). Revisit only if Tier B ergonomics ever hurt.
- **Xvfb as the *universal* Tier B host:** rejected as universal, adopted for Linux (§5b forced
  it). What survives of the original objections: it's Linux-only (macOS dev keeps Cacio — which
  also avoids native Robot dragging the developer's real cursor), and X's async rendering can make
  read-back racy — mitigated because every Tier B pixel assertion goes through the poll-with-
  deadline `waitFor` idiom, never a single read. What did *not* survive: the "needs a WM" claim —
  frame focus and Tab traversal held under bare Xvfb in the spike; fluxbox remains the documented
  escalation if a future test needs real WM protocols (activation ordering, iconify, stacking).
- **Jemmy:** GPL-2.0 without the classpath exception, dormant, Robot-bound.
- **Golden images as the primary oracle:** brittleness economics (§6). Capability retained as an
  optional tier, never the backbone.

## 11. Risks & open questions

1. **Cacio bus factor** — active but single-maintainer. Now a small risk: it's test-scope, it only
   hosts the *dev-machine* side of Tier B (CI runs native+Xvfb), and dropping it entirely would
   cost dev convenience, not CI coverage — `-Delwha.guiTier.toolkit=native` on a dev desktop runs
   the same tests on the real display today.
2. **JUnit 6 is young** (6.x line since 2025-09). It unified the 5.x platform rather than rewriting
   it; the spike surfaced no friction. Downgrade path to 5.13.x exists if surefire integration
   regresses.
3. **The `@version` tax on test files** (§7) — needs the S2 ruling before the suite grows to
   hundreds of files.
4. **The gui tier runs on two display stacks** (Cacio on dev, Xvfb on CI) — deliberate, but it
   means a dev-green gui test can in principle differ on CI. Mitigations: both stacks run the
   identical tests, both were green in the spike, and CI is the gate. Operator smokes remain the
   human net for platform-*native* behavior (real macOS focus quirks), per the epic.
5. **JDK patch drift in the font natives** — §5b's crash lives in JDK internals that patch
   releases have touched before. The failure-diagnostics artifact upload in `build.yml` exists so
   any recurrence is diagnosable from the run page directly.
