# Elwha 1.1.0 — Consumer Smoke Test Report

> **Provenance:** external consumer smoke-test report against the published `1.1.0` artifacts,
> received 2026-08-11 and archived verbatim below (from the report's own H1 onward). The test
> harness it describes — a standalone Maven consumer project with 66 JUnit tests — and the
> `shots/` screenshots it references live outside this repository. Findings were triaged the
> same day: F2 → [#797](https://github.com/OWS-PFMS/elwha/issues/797), F1 →
> [#798](https://github.com/OWS-PFMS/elwha/issues/798) (both milestoned `v1.1.1`), F3 →
> [#799](https://github.com/OWS-PFMS/elwha/issues/799), F4 →
> [#800](https://github.com/OWS-PFMS/elwha/issues/800), behavior notes →
> [#801](https://github.com/OWS-PFMS/elwha/issues/801) (all `v1.2.0`).

---

# Elwha 1.1.0 — Consumer Smoke Test Report

- **Date:** 2026-08-11
- **Artifacts under test:** `com.owspfm:elwha:1.1.0` (+ `tests` and `javadoc` classifiers, `elwha-parent` pom), `com.owspfm:elwha-showcase:1.1.0`, and the GitHub Release asset `elwha-showcase-1.1.0-app.jar` — all published earlier today (registry lastUpdated 2026‑08‑11 19:02 UTC)
- **Method:** pure external-client posture. Only the published jars, poms, GitHub Release assets, and consumer documentation (`README`, `CHANGELOG`, `docs/consumer/*`) were used. No implementation source was read; API discovery was via `javap` on the shipped binaries and the published Javadoc.
- **Consumer environment:** macOS (Darwin 25.5.0), JDK 25 (floor is 21), Maven 3.9.11, auth via `~/.m2/settings.xml` server id `github-flatcomp` (this machine's legacy id; the id only has to match the consumer pom's repository id, which it does)
- **Harness:** this directory — a Maven consumer project with 8 JUnit 5 classes / **66 tests, all passing**, plus JPMS/compat/showcase scripts. Screenshots in `shots/`.

## Verdict

**1.1.0 is in very good shape for consumers.** Every load-bearing claim in the consumer docs checked out: install and auth flow, the two-artifact split, the semver/source/binary compatibility promise, the theming contract, the 1.0.1 visual fixes, the new 1.1.0 fail-fast validation, the accessibility advisory, JPMS behavior in both directions, and the no-auth showcase evaluation path. Four findings below — one worth fixing in the artifact (testkit's hidden AssertJ requirement), one API/docs footgun (silent broken icons for unbundled glyph names, present in the docs' own example), and two low-severity behavior notes.

---

## 1. Distribution & packaging — all verified

| Check | Result |
|---|---|
| `mvn dependency:resolve` for `elwha:1.1.0` from GitHub Packages | ✅ resolves with credentials |
| install.md verify command `mvn -q dependency:get -Dartifact=com.owspfm:elwha:1.1.0` | ✅ silent exit, as documented |
| Anonymous (no-token) request to Packages | ✅ HTTP 401 — matches "authenticated request for every download" |
| Compile-scope dependency set | ✅ exactly `flatlaf 3.2.5`, `flatlaf-extras 3.2.5` (+ runtime `jsvg 1.2.0`); `flatlaf-intellij-themes` gone (1.1.0 “Removed” entry confirmed) |
| Bytecode level | ✅ major 65 (JDK 21), manifest `Build-Jdk-Spec: 21` |
| `Automatic-Module-Name: com.owspfm.elwha` in lib jar | ✅ present; jar describes as automatic module `com.owspfm.elwha@1.1.0` |
| `META-INF/LICENSE` + `NOTICE` in lib jar | ✅ present |
| Artifact split (#786): no Showcase/`playground`/`*Demo`/`*Smoke`/`*Guard`/`*Diag`/`card.fixes` in lib jar | ✅ zero matches; 29 packages, all API |
| `elwha-parent:1.1.0` pom resolves through same repo/credentials | ✅ |
| `tests` classifier (`test-jar`) resolves; contents restricted to `testkit/**` | ✅ (see finding F4 nit) |
| `javadoc` classifier resolves; library-only (no showcase/playground pages) | ✅ includes new 1.1.0 classes (`AccessibleNameAdvisory`) |
| `elwha-showcase:1.1.0` resolves from Packages | ✅ |
| Release asset `elwha-showcase-1.1.0-app.jar` downloads **without any auth** | ✅ 4.3 MB via plain `curl` |
| App jar: `Main-Class: com.owspfm.elwha.showcase.ElwhaShowcase` | ✅ |
| App jar shade hygiene: no `.SF/.DSA/.RSA`, no dependency `module-info.class` | ✅ zero matches |
| App jar licenses: Elwha LICENSE/NOTICE + `META-INF/licenses/jsvg-LICENSE` (MIT) | ✅ all present |
| Showcase jars declare **no** `Automatic-Module-Name` (deliberate) | ✅ both plain and app jar |
| Javadoc site `ows-pfms.github.io/elwha` | ✅ live, shows 1.1.0, library packages only |

## 2. Quick start & theming contract — all verified

- **QuickStart.java compiled verbatim** (extracted from `quick-start.md`) against 1.0.0, 1.0.1, **and** 1.1.0 — the page's "compile proof" holds.
- `ElwhaTheme.install` **from a non-EDT thread** returns with the theme fully live (documented dispatch-and-block). Idempotent re-install verified.
- **All 49 `ColorRole`s** resolve and are stored as `ColorUIResource`; every `TypeRole` (15) is a `FontUIResource` — the live re-skin prerequisite the docs call out.
- Token scales match the documented values exactly — ShapeScale 0/4/8/12/16/28(+FULL), SpaceScale 4/8/12/16/24/32, StateLayer blend math (HOVER = 8% tint over base) verified numerically.
- **Mode:** LIGHT→DARK swaps `SURFACE` (darker, equals `theme.dark()` palette); `Mode.SYSTEM.resolved()` returns a concrete mode; `isConcrete()` correct; `current()` round-trips the installed config; `with*` derivations don't mutate the source.
- **Bundled palettes:** `primary()` = 8 (baseline + ROYGBIV), `secondary()` = 10, tiers disjoint, `baseline()` is identity-equal to the instance inside `primary()` (the documented picker-matching contract). Recorded spectral order: Red, Orange, Yellow, Green, Blue, Indigo, **Baseline**, Deep Purple (baseline sorts by its purple primary hue).
- **`Palette.builder()`** fails at build time on incomplete palettes with a message naming every missing role (`IllegalStateException`, 48 roles listed for a 1-role palette).
- **`PaletteLoader`**: a generated 49-role JSON loads and installs; missing role → `IllegalArgumentException` naming the resource **and** the missing role; malformed JSON → IAE with parse position; absent resource → IAE naming the classpath path. No silent nulls anywhere.
- **Typography:** `defaults()` is Inter and installs as the `Button.font` family (FlatLaf bridge); `ofFamily("Helvetica Neue")` builds all 15 roles.
- **Raw Swing bridge:** `Button.background`, `TextField.background`, `ScrollBar.thumb` written; a rendered `JButton` paints exactly the bridged background; LAF is FlatLaf.
- **Spot override:** writing `Elwha.color.primary` after install is visible through `ColorRole.resolve()` and is overwritten by the next install, as documented.
- `reducedMotion` defaults to `null` (defer-to-OS) and round-trips through config.

## 3. Components — constructed and exercised (all families)

All component families from `components.md` were constructed and their core behaviors verified — variants/factories, mutual-exclusion groups (button, icon-button, radio), tri-state checkbox, switch drag-free toggle + action events, chip types (input chip's trailing remove affordance present; filter chip SELECTABLE), text field anatomy (label, placeholder, supporting/error text, prefix/suffix, max length, read-only, typed `getEditor()`), typed select field (display function write-back verified), slider (clamping, range lower/upper, variants/sizes/orientations/stops), color picker (SWATCHES/SPECTRUM/WHEEL/SLIDERS modes, alpha, favorites; eyedropper off by default), card chrome + companion primitives (chevron toggles collapse; selection; actionability), generic item list (filter/sort/empty/loading/selection model), tabs (activation events, badges, icon tabs), navigation rail (destinations, selection listener, FAB/menu slots, expanded>collapsed widths), app bar (all three variants, nav icon, actions, lift), menu/dialog/tooltip builders (+`renderPreview()`), side sheet (standard/modal, actions, width), badges (dot vs pill; `large(1042)` → `"999+"`), progress indicators (determinate fraction, indeterminate, wavy) and the loading indicator (contained/determinate forms).

**Synthetic interaction** (via the published testkit's `HeadlessHost` + `Input`): click toggles switch and fires its listener; button fires on press+release inside and **not** on release outside; filter chip selects on click; checkbox checks; clicking the second tab activates it; an actionable card fires. All passed.

**1.0.1 regression fixes verified against 1.1.0:**
- **#766** FIXED-mode tabs preferred width: exactly count × widest tab (648 = 3 × 216 in our probe) — no truncation.
- **#767** disabled treatment: a disabled FILLED button's container measured `#e2dce4` vs the M3-documented ON_SURFACE@12%-over-surface blend `#e2dde4` — within 1/255 per channel; nowhere near the old faded-PRIMARY failure.

**1.1.0 fail-fast contract (#776/#768) verified:**
- All five `ElwhaButton` variant factories: NPE naming `label`; constructors keep the null-as-empty path.
- All four `ElwhaChip` factories: NPE naming `text`; constructor keeps null-as-empty.
- `ElwhaTabs.setActiveTabIndex(5)` → `IndexOutOfBoundsException` "activeTabIndex 5 out of range [0, 2)"; `setActiveTab(nonMember)` still the documented ignore.
- `ElwhaCardHeader.setTitle/setSubtitle(null)` → NPE; `header.add(...)` → IAE: "ElwhaCardHeader is slot-based; add() is not part of its API…".
- `ElwhaSelectField.setOptions([… null …])` → NPE "options must not contain null elements".
- `MaterialIcons.themed(null)` → NPE naming `icon`.
- Bonus guards found: `ElwhaIconButtonSelectionGroup.add` rejects CLICKABLE buttons (IAE with both modes named); a label-less FAB refuses `morphTo(EXTENDED)` with a constructive message.

**Accessibility (#777):** `AccessibleNameAdvisory` fires at first paint for name-less `ElwhaIconButton`, `ElwhaFab`, `ElwhaSwitch` — one WARNING per instance, each with component-appropriate remediation wording. `ElwhaCardChevron` seeds its documented default accessible name ("Expand or collapse the card").

## 4. Compatibility & JPMS — all verified

- **Source compat:** a probe written to 1.0.0-era API compiles unchanged against 1.0.0 and 1.1.0.
- **Binary compat:** the same probe **compiled against 1.0.0** runs against the 1.1.0 jar (theme install, buttons, cards, chips, tabs) — `PROBE-OK`.
- **JPMS (lib):** a real module with `requires com.owspfm.elwha;` compiles and runs on the module path (elwha as automatic module alongside FlatLaf's explicit modules).
- **JPMS (showcase):** putting the showcase jar on the module path with the lib fails resolution exactly as stability.md warns: `ResolutionException: Module elwha.showcase contains package com.owspfm.elwha.navrail…` — classpath-only, as documented.

## 5. Showcase (evaluator path) — verified

Downloaded the app jar with plain `curl` (no token), launched with `java` on the app-jar classpath: "The Elwha Showcase" opens (1320×860), renders the documented three areas (Foundations / Components / Containers), palette tier picker (Primary/Secondary), light/dark/auto controls, and the FAB Workbench. **Zero accessibility advisories on its own console** — consistent with #769. Capture: `shots/showcase-main.png`.

---

## Findings

### F1 (medium) — Testkit silently requires AssertJ; not documented
`com.owspfm:elwha:1.1.0:tests` (`Pixels.assertPixelNear` and friends) throws `NoClassDefFoundError: org/assertj/core/api/Assertions` at runtime unless the consumer adds AssertJ themselves. A classifier jar can't carry its own dependencies, and install.md's testkit section doesn't mention the requirement. A consumer following the docs verbatim gets a hard error on first use of the pixel assertions.
**Suggested fix:** document the AssertJ requirement in install.md (with a version), or re-implement the assertions on JUnit's `Assertions` so the testkit is dependency-free.

### F2 (medium) — `MaterialIcons.get/pair/symbol` fail silently for unbundled names, and the docs' own example hits it
The bundle ships 76 SVGs (~40 distinct glyph names). `get("…")`/`symbol("…")` with any other name returns a normal-looking icon object whose SVG was never found (`FlatSVGIcon.hasFound()==false`) — no exception, no log — and it **paints as a solid filled square** (see `shots/probe-unbundled-symbol-tab.png`). This contradicts the library's own 1.1.0 fail-fast conventions (#776) at exactly the API the docs recommend "for names chosen at runtime". Compounding it, **theming.md's example is `ElwhaNavRailDestination.of(MaterialIcons.symbol("inbox"), "Inbox")` — and `inbox` is not in the bundle** ("send"/"draft" are also absent), so the doc's own snippet renders a broken icon.
**Suggested fix:** throw IAE naming the glyph and the bundle (or at least emit an advisory-style WARNING once per name), and switch the doc examples to bundled glyphs (`home`, `star`, `palette`, …) or bundle the example glyphs.

### F3 (low, cosmetic) — Pre-filled text fields animate the label float on first paint
`setText(...)` before first display leaves the floating label at its resting position for the first frame(s); it animates up once timers tick (settled render is correct — `shots/probe-textfield-immediate.png` vs `probe-textfield-settled.png`). In a real app a pre-filled form flashes label-over-text on open. Snapping the label when the float state changes while not displayed would remove the flash.

### F4 (nit) — `PixelsRenderTest` rides inside the testkit jar
The `tests` classifier jar is documented as "restricted to the shared `testkit/**` fixtures", and it is path-wise — but it includes `PixelsRenderTest(+$InkLeaf)`, which looks like a test of the fixtures rather than a fixture. Harmless; slightly untidy.

### Behavior notes (not defects)
- `ElwhaSelectField`: `setEditable(true)` on a multi-select field silently turns multi-select off (last-wins). Docs say the modes are "mutually exclusive" without specifying mechanics; under the #776 conventions an `IllegalStateException` — or a documented last-wins rule — would be clearer.
- `ElwhaBadge.setContent("12345")` (5 chars, docs say 1–4) is accepted and clamps to `999+`; `large(1042)` → `999+`. Reasonable, just undocumented.
- Checkbox `doClick()` from INDETERMINATE lands on CHECKED (recorded; M3 leaves this open).
- `MaterialIcons.DEFAULT_SIZE` is 24 (house no-arg size); the "20px optical size" in components.md refers to the Material Symbols optical-size axis, not the accessor default — could confuse a reader.

---

## Coverage map

| Area | How tested |
|---|---|
| Install/auth/resolution | Maven resolve, verify command, anonymous 401, all 5 coordinates/classifiers |
| Packaging/split/licenses/JPMS metadata | jar/manifest inspection, module describe |
| Theme/tokens/palettes/typography | `T01_ThemeTest` (15 tests) |
| 1.1.0 fail-fast contract | `T02_FailFastTest` (8 tests) |
| Component construction/behavior | `T03_ComponentTest` (26 tests) |
| Pixel-level rendering + 1.0.1 fixes | `T04_RenderTest` (8 tests) |
| Synthetic input | `T05_InteractionTest` (6 tests) |
| Visual gallery light/dark/alt palette | `T06_GalleryTest` → `shots/gallery-*.png` |
| Regression probes (icons, label float) | `T07`/`T08` → `shots/probe-*.png` |
| Semver source/binary compat | scripted javac/java across 1.0.0→1.1.0 |
| QuickStart compile proof | verbatim extraction, 3 versions |
| JPMS positive + negative | scripted module build/run |
| Showcase evaluator path | anonymous download + launch + capture |
| Javadoc | site fetch + classifier jar contents |

*Suite:* `mvn test` in this directory — 66/66 green on JDK 25.
