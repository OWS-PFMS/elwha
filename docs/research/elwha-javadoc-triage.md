# Javadoc doclint triage (#529 Phase 0)

**Status:** COMPLETE · **Date:** 2026-08-06 · **Baseline:** 7 errors / 925 warnings over 554
sources (`javadoc -Xdoclint:all`, JDK 21, full dependency classpath — reproduces the epic's
measured baseline exactly).

## The headline: the epic is an order of magnitude smaller than filed

The epic body estimated **580 missing-comment sites in real public API**. That figure was
path-filtered (excluding demos/smokes/`card/v1`) but never *visibility*-classified. Classifying
every `no comment` site by reading its declaration:

| `no comment` sites (765 total) | count |
|---|---|
| **Private fields flagged as undocumented serialized-form** | **518** |
| Out of scope (demos / smokes / guards / playgrounds / Showcase / `card/v1` / `card/fixes`) | 185 |
| List-family classes (all private fields too — and #70 deletes these classes) | 58 |
| **Genuine public/protected API gaps** | **4** |

The 518 exist because every Swing component is `Serializable`, so javadoc treats every private
field as an undocumented serialized-form entry. Even the best-documented classes in the library
bleed these — `ElwhaSwitch` (fully documented) carried ~18, `ElwhaSurface` 12. **Verified fix:**
`@serial exclude` in the class doc removes a class's serialized-form section and with it every such
warning (ElwhaSurface: 13 → 0 in this pass). Elwha does not support Java serialization, so the tag
is also semantically honest.

The genuine in-scope debt, all warning types:

| type | in scope | where |
|---|---|---|
| `no comment` (real API) | 4 | slider ×2, overlay ×1, button ×1 |
| `no main description` (`@return`-first comments) | 68 | card family 36, sidesheet 23, menu 8, surface 1 *(fixed here)* |
| `no @return` | 61 | **MaterialIcons 56** (factory methods), navrail 5 |
| implicit default constructors | 14 | one per component's `Accessible*` inner class |
| `@value` on a non-constant | 3 sites | sidesheet *(all fixed here)* |
| errors | 7 | *(all fixed here)* |

**≈ 147 mechanical fixes** + a ~50-class `@serial exclude` sweep. Not a 580-site campaign.

## What Phase 0 shipped (this branch)

1. **Errors 7 → 0.** Raw `&` in slider's class doc; `{@linkplain}`s to private fields in method
   syntax (Size enum); `{@value}` on the token-derived `DETACHED_MARGIN_PX`
   (= `SpaceScale.LG.px()`, not a constant expression — now `{@link}`) in the sheet + two demos;
   unresolvable short-name `{@link}`s in a demo without the import (now fully qualified).
2. **Enforcement flipped.** `<doclint>all</doclint>` + `<failOnError>true</failOnError>` — doclint
   errors now fail `mvn package`, hence the required `build` check. Warnings print but don't fail;
   the ratchet's endgame is `failOnWarnings` when the debt hits zero.
3. **The exemplar.** `ElwhaSurface` is the library's first doclint-clean class (`@serial exclude`
   + main descriptions on the `@return`-first getters). `docs/development/javadoc-style.md`
   codifies the house style from it and `ElwhaSwitch`.
4. This triage. Full doclint output regenerable via the epic's command; warning count after this
   branch: **907** (the 518 false positives dominate until S1).

## Story plan (replaces the per-package-batch sketch)

- **S1 — mechanical sweep to doclint-zero + the warnings ratchet.** `@serial exclude` across the
  component classes; the 68 main descriptions; MaterialIcons' 56 `@return`s (formulaic); the 14
  explicit `Accessible*` constructors; the 4 real gaps; then flip `failOnWarnings`. One PR or two;
  precise per-entity `@version` bumps throughout. Skips: `card/v1` (#96 deletes), the list
  families (#70 deletes — their replacement `ElwhaItemList` is documented to the house style as
  new code under #69).

  **S1 executed (2026-08-07) — amendment:** the sweep went wider than planned (demos/smokes
  included — 33 cheap sites) and landed at **zero doclint warnings outside `card/v1`** (122
  remain there). The `failOnWarnings` flip therefore **rides #96's deletion PR** — it cannot flip
  while `card/v1` exists, and once v1 is gone the flip is a two-line pom change against an
  already-clean tree. The list families were swept too (they were in the serialized-form bucket;
  the tags die with #70's deletions — no cost). Surprise from the sweep: the true remaining
  no-comment API gaps were `ElwhaButton.startWidthBorrow` and `AbstractElwhaOverlay.relayout`, not
  the four sites the first classification estimated — same order of magnitude, different names.
- **S2 — consumer usability.** `package-info.java` for `theme` / `tabs` / `icons`; the overview
  page decision for the published javadoc jar; cross-reference audit (anchors ↔ hosts, list
  families ↔ `ElwhaList<T>`, `ElwhaSurface` ↔ subclasses); class-doc spot-audit against the style
  guide's section anatomy for the oldest components (the pre-Switch generations).

#440's scan keeps filing Javadoc findings against this epic, per the standing de-confliction.
