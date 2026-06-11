# ElwhaAppBar — M3 Spec Capture (research scratch)

**Status:** CAPTURE COMPLETE — M3 source material for epic [#287](https://github.com/OWS-PFMS/elwha/issues/287) (ElwhaAppBar, `v0.4.0`). Companion research dump (mirrors [`elwha-tabs-research.md`](elwha-tabs-research.md) / [`elwha-switch-research.md`](elwha-switch-research.md)); the decisions live in [`elwha-appbar-design.md`](elwha-appbar-design.md).

**Captured:** 2026-06-11. **Author:** Charles Bryan (`cfb3@uw.edu`).

**Capture method note:** specced web-first in a single autonomous run (the ElwhaSwitch #401 / ElwhaTabs #425 pattern). The app bar's authoritative machine-readable sources differ from the tabs capture: **material-web never shipped a top-app-bar component**, so the token sheets below are the **complete androidx Compose Material3 `AppBar*Tokens` v14.0.0 files** (the generated token set the M3 Expressive spec renders) plus the **MDC-Android `TopAppBar.md` component doc** for variants/anatomy/attributes and the **Compose Material3 expressive API surface** for behavior truth. No operator screenshot session ran; §F lists raw-file URLs instead. The m3.material.io app-bar spec pages are JS-only (WebFetch returns the page title), per the established pattern.

**⚠️ The stub predates the Expressive respec.** Epic #287's provisional variant list (`SMALL / CENTER_ALIGNED / MEDIUM / LARGE`) is baseline-M3. The May-2025 Expressive update **renamed the component "top app bar" → "app bar"**, **deprecated Medium and Large** in favor of **Medium flexible / Large flexible**, **folded the center-aligned variant into a title-alignment option**, **added subtitle support**, and **added a Search variant**. The capture below is the Expressive spec; the stub's list is superseded (Elwha is Expressive-first — [[project_elwha_m3_expressive]]).

**Consumers / related:**
- **OWS-PFMS/OWS-Local-Search-GUI** — would adopt the app bar for its window header (stub's known-consumer note).
- **The Elwha Showcase itself** — the showcase frame header is a hand-rolled panel today; long-term dogfood target.
- [`ElwhaFabAnchor`](../../src/main/java/com/owspfm/elwha/fab/ElwhaFabAnchor.java) — prior art for the scroll-source pattern (`setScrollSource(JScrollPane)` + `BoundedRangeModel` `ChangeListener`, #269). The stub asks Phase 0 to settle shared-vs-per-component (→ design §8).
- [`ElwhaIconButton`](../../src/main/java/com/owspfm/elwha/iconbutton/ElwhaIconButton.java) — `STANDARD` variant is the M3 anatomy for the leading/trailing icon buttons; `implements IconBearing` so `ElwhaBadge` rides actions for free.
- [`ElwhaTabs`](../../src/main/java/com/owspfm/elwha/tabs/ElwhaTabs.java) — the freshest full-width-bar container precedent (custom `doLayout()`, paint-time token resolution, `paintChildren` overlay).
- [`MorphAnimator`](../../src/main/java/com/owspfm/elwha/theme/MorphAnimator.java) + [`Easing`](../../src/main/java/com/owspfm/elwha/theme/Easing.java) — drive the lift-color fade; collapse is scroll-position-driven (no timer).
- [`MaterialIcons`](../../src/main/java/com/owspfm/elwha/icons/MaterialIcons.java) — icon slots (24px house default = the spec's 24dp icon size).

---

## §TL;DR — synthesis (read this first)

**What the M3 Expressive app bar is**, distilled from the capture below:

1. **One component, three shipping variants + one excluded.** Expressive variants: **Search** · **Small** · **Medium flexible** · **Large flexible**. Search and Small group as *regular* (single-row) bars; the flexibles group as *collapsing* bars. Baseline **Medium / Large are deprecated** ("migrate to flexible"), **Center-aligned is gone as a variant** (now `titleCentered` on any bar). The **bottom app bar is deprecated** in favor of the **docked toolbar** (a different component family — toolbars). (§A, §E.)
2. **Anatomy (§A):** **container** · **leading button** (navigation icon) · **headline** (title) · **subtitle** (new in Expressive, all variants) · **trailing elements** (action icon buttons; Expressive explicitly allows imagery/avatar — `AvatarSize 32` — and filled buttons).
3. **Heights (§T, verbatim v14.0.0):** small **64**; medium flexible **112** expanded (**136** with subtitle); large flexible **120** expanded (**152** with subtitle); all flexibles collapse to the small bar's **64**.
4. **Type roles fork by variant (§T):** small title **Title Large** / subtitle **Label Medium**; medium-flexible expanded title **Headline Medium** / subtitle **Label Large**; large-flexible expanded title **Display Small** / subtitle **Title Medium**. Collapsed flexibles use the small bar's roles. **⚠️ `TypeRole` has no display tier today** (deliberately dropped in v1 per its Javadoc) — large flexible forces the call (→ design §4).
5. **Color (§T):** container `SURFACE` (elevation 0, shape none); **on-scroll container `SURFACE_CONTAINER`** (the "lift"; tonal — paired token `OnScrollContainerElevation Level2` exists but Compose renders the lift as color only, no shadow); leading icon `ON_SURFACE`; trailing icons `ON_SURFACE_VARIANT`; title `ON_SURFACE`; subtitle `ON_SURFACE_VARIANT`. **Zero new color tokens.**
6. **Edge geometry (§T):** leading space **4** (edge → nav button container) · trailing space **4** · icon-button gap **0** (48dp targets abut) · icon **24** · avatar **32**. Title inset 16 from the container edge when no nav button precedes it (Compose internals, §I).
7. **Scroll behaviors (§C, §I):** **pinned** (bar fixed; lift color when content scrolls under — MDC `liftOnScroll` default **true**), **exitUntilCollapsed** (flexibles collapse expanded→64 as content scrolls, re-expand at top), **enterAlways** (whole bar slides away on scroll-down — mobile-screen-estate pattern). Lift color both directions; collapse is scroll-*position*-driven, not a fire-and-forget animation.
8. **Collapse mechanics (§I):** two-row model — the top row is a perpetual small bar (64) whose collapsed title fades in as collapse completes; the bottom row holds the expanded headline block (margins 16, title↔subtitle padding 0) which shrinks away with scroll. Container color crossfades `SURFACE → SURFACE_CONTAINER` as content overlaps.
9. **Alignment (§A, §C):** `titleCentered` / `subtitleCentered` (MDC) ≈ `titleHorizontalAlignment` start|center (Compose) — an option on small *and* flexible bars, not a variant.
10. **Expressive flexibility (§A):** flexible bars allow **text wrapping** for the expanded headline and **larger title text**; Expressive reduced the flexibles' overall heights vs the deprecated baseline medium/large (112 vs 112 / 120 vs 152 — large dropped 32dp unless a subtitle restores it).
11. **A11y (§A):** content descriptions required on the nav button and every action; the headline is the bar's name-giving text. Desktop mapping: the bar itself is structural (no interactive role); the buttons carry the interaction a11y (already handled by `ElwhaIconButton`).
12. **No disabled state, no state layers on the bar itself** (§T): the bar is a non-interactive surface; hover/focus/press belong to the hosted buttons. The token sheet defines no disabled/hover/press rows for the container.

### Reading-order TOC
§A MDC-Android doc (variants / anatomy / attributes / Expressive deltas) → §C Compose Expressive API → §T token sheets (complete v14.0.0) → §I implementation internals (layout / collapse / lift / scroll) → §E Expressive status + adjacent components → §Tokens Elwha mapping → §P terminology lock → §Open → §F capture log.

---

## §A. MDC-Android component doc (authoritative text)

Source: <https://raw.githubusercontent.com/material-components/material-components-android/master/docs/components/TopAppBar.md> (fetched 2026-06-11).

**Variants** (the Expressive set):

> 1. **Search app bar** — emphasizes the search entry-point; includes a search field instead of heading text.
> 2. **Small app bar** — "Use in dense layouts or when a page is scrolled."
> 3. **Medium flexible app bar** — "Use to display a larger headline. It can collapse into a small app bar on scroll."
> 4. **Large flexible app bar** — "Use to emphasize the headline of the page."

> For implementation, "the search and small app bars can be grouped into **regular top app bars**, while the medium flexible and large flexible app bars can be grouped into **collapsing top app bars**." `[CODE]` — the variant fork is single-row vs two-row.

> **Deprecated:** Medium and Large (non-flexible) are deprecated; migrate to the flexible variants. `[CODE]` — Elwha ships no baseline medium/large.

**Anatomy** — five parts: **1 Container · 2 Leading button · 3 Trailing elements · 4 Headline · 5 Subtitle.** `[CODE]`

**Expressive update deltas** (verbatim from the doc's Expressive section):

> - Component officially **renamed from "top app bar" to "app bar"**. `[CODE]` — naming input for the Elwha class (→ §P).
> - New **search app bar**: "supports icons inside and outside the search bar, and centered text."
> - **Flexible improvements** (medium + large flexible): reduced overall height; **larger title text**; **subtitle support**; **left- and center-aligned text options**; **text wrapping** capability; "more flexible elements for imagery and filled buttons."
> - **Small app bar**: added **subtitle** support; **center-aligned text** option; the same flexible-elements allowance.

**Attributes / API** (MDC `MaterialToolbar` / `CollapsingToolbarLayout`):

| Surface | Attribute / method | Default | Notes |
|---|---|---|---|
| Subtitle | `app:subtitle`, `setSubtitle()` / `getSubtitle()` | — | all variants `[CODE]` |
| Centering | `app:titleCentered`, `app:subtitleCentered` | false | per-text booleans `[CODE]` |
| Lift | `app:liftOnScroll` | **true** | on-scroll container-color change `[CODE]` |
| Lift color | — | `?attr/colorSurfaceContainer` | `[CODE]` |
| Scroll flags | `app:layout_scrollFlags` | — | `scroll\|exitUntilCollapsed\|snap` is the canonical collapsing recipe `[CODE]` |
| Compress | `app:layout_scrollEffect="compress"` | — | optional squish effect — not adopted `[DOC]` |
| Expanded title margins | `app:expandedTitleMargin*` | **16dp** | `[CODE]` |
| Expanded title↔subtitle padding | `app:expandedTitlePadding` | **0dp** | `[CODE]` |
| Nav a11y | `app:navigationContentDescription` | — | required `[CODE]` |
| Action a11y | `android:contentDescription` per item | — | required `[CODE]` |

**MDC color attrs** (agree with the §T token sheet): container `?attr/colorSurface`, lift `?attr/colorSurfaceContainer`, title `?attr/colorOnSurface`, subtitle `?attr/colorOnSurfaceVariant`, navigation icon `?attr/colorOnSurface`, action items `?attr/colorOnSurfaceVariant`.

**MDC typography attrs:** title default `?attr/textAppearanceTitleLarge`; expanded title `?attr/textAppearanceHeadlineSmall` (medium) / `?attr/textAppearanceHeadlineMedium` (large) — **note these are the deprecated-baseline values**; the Expressive flexible bars use the larger §T fonts (Headline Medium / Display Small). The doc states existing `collapsingToolbarLayoutMedium/LargeStyle` attrs "automatically adopt flexible behavior" under Material3Expressive themes.

## §C. Compose Material3 Expressive API (behavior truth)

Sources: composables.com API references for `TopAppBar`, `MediumFlexibleTopAppBar`, `LargeFlexibleTopAppBar` (fetched 2026-06-11); androidx `AppBar.kt`.

**Small** — `TopAppBar(title, subtitle?, titleHorizontalAlignment, navigationIcon, actions, expandedHeight = TopAppBarExpandedHeight, colors, scrollBehavior?)`. Subtitle + alignment are `@ExperimentalMaterial3ExpressiveApi`. `[CODE]`

**Flexibles** — `MediumFlexibleTopAppBar` / `LargeFlexibleTopAppBar(title, subtitle?, navigationIcon, actions, titleHorizontalAlignment = Start, collapsedHeight = …AppBarCollapsedHeight, expandedHeight = conditional, colors, scrollBehavior?)`:

- `expandedHeight` defaults fork on subtitle presence: `…FlexibleAppBarWithSubtitleExpandedHeight` vs `…WithoutSubtitleExpandedHeight` → the §T `LargeContainerHeight` vs `ContainerHeight` token pair. `[CODE]`
- Title font: `AppBar<Variant>FlexibleTokens.TitleFont` expanded, **`AppBarSmallTokens.TitleFont` collapsed**; same pattern for subtitle. `[CODE]` — collapsed flexible ≡ small bar typography.
- `titleHorizontalAlignment`: `Start` default, `Center` the spec'd alternative (one knob for title+subtitle, vs MDC's two booleans). `[CODE]`

**`topAppBarColors()`** slots: `containerColor`, `scrolledContainerColor`, `navigationIconContentColor`, `titleContentColor`, `actionIconContentColor`, `subtitleContentColor` — all drawn from `AppBarTokens` (§T agreement). `[CODE]`

**Scroll behaviors** (`TopAppBarDefaults`):

| Behavior | Doc text |
|---|---|
| `pinnedScrollBehavior` | "A pinned app bar will **stay fixed in place** when content is scrolled and will not react to any drag gestures." Colors still transition on overlap. `[CODE]` |
| `enterAlwaysScrollBehavior` | "Will **immediately collapse when the content is pulled up**, and will **immediately appear when the content is pulled down**." (Whole-bar hide/show, direction-driven.) `[CODE]` |
| `exitUntilCollapsedScrollBehavior` | Expanded region collapses as content scrolls up, **until only the collapsed (64) bar remains pinned**; re-expands at top. The canonical flexible-variant behavior. `[CODE]` |

Single-row bars compose via `SingleRowTopAppBar`; flexibles via `TwoRowsTopAppBar(title, smallTitle, …)` — discrete expanded/collapsed title compositions crossfaded/translated by collapse fraction, not one continuously-scaling text. `[CODE]` — sanctions the two-layer title approach in Swing.

## §T. Token sheets — complete, verbatim (androidx Compose Material3, VERSION 14_0_0)

The generated M3 Expressive token set. Sources: §F.

**`AppBarTokens`** (shared, all variants):

| Token | Value |
|---|---|
| `ContainerColor` | `ColorSchemeKeyTokens.Surface` |
| `ContainerElevation` | `ElevationTokens.Level0` |
| `ContainerShape` | `ShapeKeyTokens.CornerNone` |
| `OnScrollContainerColor` | `ColorSchemeKeyTokens.SurfaceContainer` |
| `OnScrollContainerElevation` | `ElevationTokens.Level2` |
| `LeadingIconColor` | `ColorSchemeKeyTokens.OnSurface` |
| `TrailingIconColor` | `ColorSchemeKeyTokens.OnSurfaceVariant` |
| `TitleColor` | `ColorSchemeKeyTokens.OnSurface` |
| `SubtitleColor` | `ColorSchemeKeyTokens.OnSurfaceVariant` |
| `IconSize` | `24.0.dp` |
| `AvatarSize` | `32.0.dp` |
| `LeadingSpace` | `4.0.dp` |
| `TrailingSpace` | `4.0.dp` |
| `IconButtonSpace` | `0.0.dp` |

**`AppBarSmallTokens`:**

| Token | Value |
|---|---|
| `ContainerHeight` | `64.0.dp` |
| `TitleFont` | `TypographyKeyTokens.TitleLarge` |
| `SubtitleFont` | `TypographyKeyTokens.LabelMedium` |

**`AppBarMediumFlexibleTokens`:**

| Token | Value |
|---|---|
| `ContainerHeight` | `112.0.dp` (expanded, no subtitle) |
| `LargeContainerHeight` | `136.0.dp` (expanded, with subtitle) |
| `TitleFont` | `TypographyKeyTokens.HeadlineMedium` |
| `SubtitleFont` | `TypographyKeyTokens.LabelLarge` |

**`AppBarLargeFlexibleTokens`:**

| Token | Value |
|---|---|
| `ContainerHeight` | `120.0.dp` (expanded, no subtitle) |
| `LargeContainerHeight` | `152.0.dp` (expanded, with subtitle) |
| `TitleFont` | `TypographyKeyTokens.DisplaySmall` |
| `SubtitleFont` | `TypographyKeyTokens.TitleMedium` |

**Deprecated-baseline sheets** (`AppBarMediumTokens` 112 / `AppBarLargeTokens` 152, title HeadlineSmall / HeadlineMedium) exist in the same directory — **not captured as scope** (deprecated; ghosts per the token-table-ghost rule, breadcrumb only).

⚠️ **Cross-check, heights:** Expressive *reduced* heights vs baseline — medium flexible 112 = old medium 112 (unchanged), but **large flexible 120 vs old large 152**; the subtitle variants (136/152) recover the lost rows. The MDC doc's "reduced overall height" claim and the token sheet agree. Collapsed height for both flexibles = small's 64 (`TopAppBarDefaults.MediumAppBarCollapsedHeight` / `LargeAppBarCollapsedHeight` → `AppBarSmallTokens.ContainerHeight`).

## §I. Implementation internals (layout / collapse / lift)

From the Compose `AppBar.kt` implementation (single best-documented M3 implementation; material-web has none):

- **Row layout:** `[leading 4][nav button 48?][title block][actions 48×n, gap 0][trailing 4]`. With no nav button the title gets a **16dp inset** from the container edge (`TopAppBarTitleInset`; internal val — Compose pads 4dp on every bar edge + 12dp title-specific). With a nav button, the title starts at the button's container edge (the 48dp target's internal centering yields the visual icon↔title gap). `[CODE]`
- **Vertical:** single-row content is **vertically centered** in the 64 strip (incl. title+subtitle stack). `[CODE]`
- **Two-row (flexible):** top strip is a permanent small bar (64); the **expanded headline block** sits in the bottom region — start/end margins **16** (MDC `expandedTitleMargin`), title↔subtitle gap **0** (MDC `expandedTitlePadding`), block sits toward the container bottom (Compose baseline `MediumTitleBottomPadding 24` / `LargeTitleBottomPadding 28` — internal vals, flexible variants tune visually; treat as start values, not gospel). `[CODE]`
- **Collapse fraction:** `collapsedFraction = consumed scroll / (expandedHeight − collapsedHeight)`, clamped 0..1; container height = lerp. Scroll-*position*-driven — dragging back down re-expands symmetrically. `[CODE]`
- **Title transition:** expanded headline block fades out as its region shrinks (alpha keyed to fraction with most of the fade late); the **collapsed title fades in only near full collapse** (Compose: top-title alpha easing kicks in past ~mid fraction) — they crossfade, the text does not continuously scale. `[CODE]`
- **Lift (on-scroll color):** container color transitions `Surface → SurfaceContainer` once content is scrolled under the bar (overlap > 0), back at top. Compose lerps quickly by overlap fraction; MDC animates the lift on a threshold flip. **No drop shadow is painted by Compose** — the `Level2` elevation token manifests tonally as the color change. `[CODE]`
- **Pinned + lift is the universal default** (MDC `liftOnScroll=true` on every bar incl. small); collapse adds on top for flexibles. `[CODE]`
- **`enterAlways`** hides the *entire* bar on scroll-down / restores on scroll-up — direction-driven like `ElwhaFabAnchor`'s `ScrollResponse.HIDE`. Mobile-estate pattern. `[DOC]` — desktop value low (→ §Open-4).

## §E. Expressive status + adjacent components

- **This capture *is* the Expressive respec** — unlike Tabs (no Expressive delta), the app bar was materially respecced in May 2025: rename, variant reshuffle, subtitle, alignment options, flexible heights/typography. There is no "newer" spec to wait for.
- **Bottom app bar: deprecated** — "should be replaced with the **docked toolbar**, which functions similarly, but is shorter and has more flexibility." The docked toolbar (+ the new **floating toolbar**) belong to the **Toolbars** component family — a *separate future Elwha component*, not an app-bar variant. Supersedes the stub's "bottom app bar sibling later" note. `[DOC]` → excluded-scope stub epic.
- **Search app bar** — embeds a search *field* (the M3 search-bar anatomy) in the headline slot, "icons inside and outside the search bar." Depends on text-field-grade editing anatomy (`ElwhaTextField` #286 is itself a stub). `[DOC]` → excluded-scope stub epic (V2).

## §Tokens — Elwha mapping (goal: zero new tokens)

| M3 token | Elwha | Status |
|---|---|---|
| `ContainerColor` Surface | `ColorRole.SURFACE` | ✅ |
| `OnScrollContainerColor` SurfaceContainer | `ColorRole.SURFACE_CONTAINER` | ✅ |
| `LeadingIconColor` OnSurface | `ColorRole.ON_SURFACE` | ✅ (via `ElwhaIconButton` STANDARD) |
| `TrailingIconColor` OnSurfaceVariant | `ColorRole.ON_SURFACE_VARIANT` | ✅ (IconButton STANDARD default) |
| `TitleColor` OnSurface | `ColorRole.ON_SURFACE` | ✅ |
| `SubtitleColor` OnSurfaceVariant | `ColorRole.ON_SURFACE_VARIANT` | ✅ |
| `ContainerShape` CornerNone | `ShapeScale.NONE` | ✅ |
| `ContainerElevation` Level0 / `OnScrollContainerElevation` Level2 | rendered tonally (color), no `ShadowPainter` call | ✅ decision §I (Compose parity) |
| Small `TitleFont` TitleLarge | `TypeRole.TITLE_LARGE` | ✅ |
| Small `SubtitleFont` LabelMedium | `TypeRole.LABEL_MEDIUM` | ✅ |
| MedFlex `TitleFont` HeadlineMedium | `TypeRole.HEADLINE_MEDIUM` | ✅ |
| MedFlex `SubtitleFont` LabelLarge | `TypeRole.LABEL_LARGE` | ✅ |
| LargeFlex `TitleFont` **DisplaySmall** | **`TypeRole.DISPLAY_SMALL` — does not exist** | ⚠️ the one gap — TypeRole deliberately dropped the display tier in v1 ("a desktop tool rarely needs"); the app bar is the first consumer that does. → design §4 |
| LargeFlex `SubtitleFont` TitleMedium | `TypeRole.TITLE_MEDIUM` | ✅ |
| `IconSize` 24 | `MaterialIcons` default render | ✅ |
| `LeadingSpace`/`TrailingSpace` 4, `IconButtonSpace` 0 | layout constants (px=dp) | ✅ no token needed |
| `AvatarSize` 32 | no avatar primitive; trailing slot accepts any `JComponent` | parked `[DOC]` |
| Heights 64 / 112 / 136 / 120 / 152 | layout constants (px=dp) | ✅ |
| Lift fade / collapse easing | `MorphAnimator` + `Easing` | ✅ |

**Verdict: zero new *color/shape/space/state* tokens; one TypeRole-surface question (display tier).**

## §P. Terminology → API lock

| M3 noun | Source | Elwha API |
|---|---|---|
| **app bar** (post-rename) | MDC Expressive section | `ElwhaAppBar`, package `appbar` — ⚠️ stub says `ElwhaTopAppBar`; rename recommended, → §Open-1 |
| variant set | MDC | `AppBarVariant.SMALL / MEDIUM_FLEXIBLE / LARGE_FLEXIBLE` |
| headline / **title** | anatomy says "headline"; **every Google API ships `title`** (MDC `setTitle`, Compose `title`) | `setTitle` (+ `getTitle`) — implementation-API consensus |
| subtitle | MDC `setSubtitle` / Compose `subtitle` | `setSubtitle` |
| leading button / navigation icon | MDC `navigationIcon` / Compose `navigationIcon` | `setNavigationIcon` |
| trailing elements / actions | Compose `actions` | `addAction` (+ generic trailing-element escape) |
| centered text | MDC `titleCentered` / Compose `titleHorizontalAlignment` | `setTitleCentered(boolean)` (MDC noun; one knob for title+subtitle like Compose) |
| lift on scroll | MDC `liftOnScroll` | `setLiftOnScroll(boolean)` default `true` |
| scroll behavior source | FabAnchor precedent | `setScrollSource(JScrollPane)` |
| collapsed / expanded | Compose `collapsedHeight` / `expandedHeight` | collapse driven by variant + scroll source; `getCollapsedFraction()` observable |

## §Open — questions carried to the design doc

1. **Class name:** `ElwhaAppBar` (Expressive rename, M3-exact) vs `ElwhaTopAppBar` (stub title, disambiguates against the deprecated bottom bar). Recommendation: **`ElwhaAppBar`** — Expressive *is* the spec Elwha targets, and the bottom bar no longer exists to collide with.
2. **TypeRole display tier:** large flexible needs `DISPLAY_SMALL`. Add the full display tier (3 roles, completes the M3 15-role scale; `Typography` iterates `TypeRole.values()` so fonts derive automatically) vs map to `HEADLINE_LARGE` (32pt vs spec 36pt — bends fidelity). Recommendation: **add the tier** — loud, deliberate token-surface change.
3. **Scroll-driver sharing (stub's cross-component note):** FabAnchor's machinery is ~30 lines of `BoundedRangeModel` plumbing with *direction* semantics; the app bar needs *absolute-offset* semantics. Extract a minimal shared `theme/` binding (RipplePainter-style shared machinery; FabAnchor refactors onto it) vs keep per-component. Recommendation: **extract the binding** (lifecycle only — attach/detach/value/delta), responses stay per-component.
4. **`enterAlways` scroll behavior:** mobile-estate pattern, low desktop value, whole-bar hide complicates host layout. Recommendation: **defer from V1** (documented, not stubbed — it's a behavior flag on this component, not an excluded variant).
5. **Expanded-headline text wrapping (Expressive flexibility):** multi-line expanded titles complicate the height/collapse model (height tokens assume one line). Recommendation: **V1 single-line + ellipsis everywhere; file a follow-up issue** for expanded wrap.
6. **Search app bar:** excluded from V1 → **stub epic**, blocked by text-field maturity (#286).
7. **Toolbars family (docked/floating):** the bottom-bar successor → **stub epic** so the #287 "sibling" note lands somewhere real.

## §F. Capture log (sources, fetched 2026-06-11)

- MDC-Android component doc: `https://raw.githubusercontent.com/material-components/material-components-android/master/docs/components/TopAppBar.md`
- Compose token sheets (VERSION 14_0_0): `https://raw.githubusercontent.com/androidx/androidx/androidx-main/compose/material3/material3/src/commonMain/kotlin/androidx/compose/material3/tokens/AppBarTokens.kt` + sibling `AppBarSmallTokens.kt`, `AppBarMediumFlexibleTokens.kt`, `AppBarLargeFlexibleTokens.kt` (deprecated `AppBarMediumTokens.kt` / `AppBarLargeTokens.kt` noted, not scoped)
- Compose API surface: `https://composables.com/docs/androidx.compose.material3/material3/components/TopAppBar` + `MediumFlexibleTopAppBar` + `LargeFlexibleTopAppBar`; androidx `AppBar.kt` (`https://raw.githubusercontent.com/androidx/androidx/androidx-main/compose/material3/material3/src/commonMain/kotlin/androidx/compose/material3/AppBar.kt`)
- Expressive toolbar/bottom-bar status: m3.material.io toolbar specs page + MDC `BottomAppBar.md` deprecation note (via search capture 2026-06-11)
- m3.material.io app-bar pages: JS-only, not fetchable (page-title test confirmed pattern) — no operator screenshot session in this run; Compose token sheets carry the same generated values the spec pages render.
