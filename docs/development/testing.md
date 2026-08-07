# Testing conventions

**Owner epic:** #438 · **Foundation:** `docs/research/elwha-test-suite-research.md` (#439 — the
framework selection, the two-tier rationale, and the Linux font-native derivation §5b). This page
is the working contract for writing suite tests; the research doc is the why.

## The two tiers

| | Tier A — headless (default) | Tier B — `@Tag("gui")` |
|---|---|---|
| JVM | `java.awt.headless=true`, no extra toolkit | headless=false, own forked JVM (`reuseForks=false`) |
| Display | none — offscreen `BufferedImage` | Cacio virtual toolkit (dev) / native under Xvfb (Linux CI), via `testkit/GuiToolkit` |
| Covers | construction, geometry, pixel probes light+dark, a11y shapes, event contracts, InputMap wiring, `dispatchEvent` input, traversal *policy* | real focus *ownership*, Robot input, window realization, overlay placement, drag gestures |
| Threading | everything on the EDT via `EdtInterceptor` | Robot off-EDT; component reads through `WaitFor.onEdt` |

Put a test in Tier B only when headless cannot represent what it asserts. The tag split is
load-bearing: mixing Cacio with an initialized JDK toolkit segfaults, so `gui` tests run in their
own surefire execution and JVM.

## Layout and naming

```
src/test/java/com/owspfm/elwha/
  testkit/                          shared fixtures (public; @version convention applies)
  <component>/                      same package as the component — package-private seams stay
      <Component><Aspect>Test.java  Tier A
      <Component>GuiTest.java       Tier B
```

Test classes and methods are package-private. Assertion messages are plain-English contract
statements ("Space release commits the toggle") so a CI failure reads as a spec violation.

**The `@version`/`@since` convention applies to `testkit/` only** — per-test classes are exempt
(the gate script scopes `src/test` to the testkit package; decision recorded in the #529 triage).

## The testkit

- **`EdtInterceptor`** — runs every Tier A test + lifecycle method on the EDT. Not for gui tests.
- **`ThemeExtension`** — installs baseline LIGHT before each test and pins reduced motion
  (`MorphAnimator` otherwise auto-detects the host OS accessibility setting — an unpinned run is
  machine-dependent). Mid-test mode switches go through `ThemeExtension.install(Mode)`.
- **`Pixels`** — offscreen render + `assertPixelNear` (±10/channel, hex-diff failures) + `mix`
  (state-layer compositing math).
- **`Input`** — mouse through real `dispatchEvent` (explicit-abs-coords ctor — the `(x,y)`-only
  one NPEs on peerless components); `pressBoundKey` asserts the `WHEN_FOCUSED` binding *and*
  invokes the action (never invoke actions by bare name — the binding is part of the contract).
- **`WaitFor`** — poll-with-deadline on the EDT, the only sanctioned wait in Tier B. No bare
  sleeps; no wall-clock sampling of live animations (the incumbent's one flake was exactly that).
- **`GuiToolkit`** — the per-platform display-stack switch; test code never references Cacio
  directly.

## Determinism rules (zero flake budget — the suite gates merges)

1. Reduced motion pinned always (ThemeExtension does it); animation-frame assertions inject
   progress into the stateless painters instead of racing a Timer.
2. Assert against **resolved roles** (`ColorRole.PRIMARY.resolve()`), never hardcoded hex.
3. Never probe pixels inside a text glyph's box; assert chrome, or assert text *geometry*
   (FontMetrics), not text pixels. Fonts: only the theme's bundled Inter — never logical names.
4. Robot events are paced (`setAutoDelay(50)` + `setAutoWaitForIdle(true)`) — unthrottled XTEST
   delivery outran event processing on CI.
5. Both surefire executions set `java.awt.headless` explicitly — JDK patch releases have changed
   auto-detection before (21.0.4).

## CI

Two workflows run the suite: `build` (packaging gate) and `Test (components + Showcase)`
(`test.yml` — uploads `surefire-reports` + `hs_err_pid*` on failure and the JaCoCo report on
success). JaCoCo is **report-only**; no coverage threshold while the suite grows from zero. The
gui-tier surefire `argLine` chains `@{argLine}` so JaCoCo's agent injection survives — hardcoding
that line silently zeroes coverage for the tier.

Reproducing CI-only crashes locally: Docker `maven:3.9-eclipse-temurin-21` + X client libs +
fontconfig + xvfb, mount the worktree, `xvfb-run -a -s "-screen 0 1920x1080x24" mvn -B clean
test`; `hs_err_pid*` lands in the mounted tree.
