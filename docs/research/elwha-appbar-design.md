# ElwhaAppBar — Phase 0 Design

**Status:** PROPOSED (architecture locks via the S1 spike). **Drafted:** 2026-06-11. **Author:** Charles Bryan (`cfb3@uw.edu`).
**Parents:** epic [#287](https://github.com/OWS-PFMS/elwha/issues/287). **Milestone:** `v0.4.0`.
**Research:** [`elwha-appbar-research.md`](elwha-appbar-research.md) — the captured spec; this doc holds only the decisions.

---

## TL;DR — the locked decisions

1. **Name: `ElwhaAppBar`** in `com.owspfm.elwha.appbar` — the M3 Expressive rename ("top app bar" → "app bar") is the spec Elwha targets; the deprecated bottom bar no longer exists to collide with. The stub's `ElwhaTopAppBar` predates the respec. §3.
2. **One dedicated primitive: `ElwhaAppBar extends JComponent`**, a real Swing container with a custom `doLayout()` hosting the consumer's nav/action `ElwhaIconButton`s and painting title/subtitle text itself — **not** a `JToolBar`, **not** a `JPanel` assembly. The flexible variants are the *same component* with a two-row layout, not subclasses. [RECOMMENDED; locked via S1 spike — §2]
3. **Variants (Expressive set): `AppBarVariant.SMALL / MEDIUM_FLEXIBLE / LARGE_FLEXIBLE`** + per-variant static factories. Baseline Medium/Large are deprecated upstream — never shipped. Center-aligned is **not a variant**: `setTitleCentered(boolean)` works on every bar. **Search** is excluded to a stub epic (needs text-field anatomy, #286); **toolbars** (docked/floating — the bottom-bar successors) are a separate stubbed family. §3, §12.
4. **Tokens: zero new color/shape/space/state tokens.** One deliberate type-surface change: **add the display tier to `TypeRole`** (`DISPLAY_LARGE` 57 / `DISPLAY_MEDIUM` 45 / `DISPLAY_SMALL` 36, regular weight) — completes the M3 15-role scale; the large-flexible expanded title is `DisplaySmall` and v1's "desktop tools don't need display" rationale just met its counterexample. `Typography` iterates `TypeRole.values()`, so fonts derive with no further code. §4.
5. **Geometry (px=dp, verbatim v14.0.0):** small 64; medium-flexible 112/136 (no-sub/sub); large-flexible 120/152; flexibles collapse to 64. Edge spaces 4/4, action gap 0, icon 24, title inset 16 (no nav icon), expanded headline margins 16, title↔subtitle gap 0. §5.
6. **Color:** container `SURFACE` → lift `SURFACE_CONTAINER` when content scrolls under (tonal only — **no drop shadow**, Compose parity; the `Level2` token manifests as the color change). Title `ON_SURFACE`, subtitle `ON_SURFACE_VARIANT`; nav icon `ON_SURFACE`, actions `ON_SURFACE_VARIANT` — both fall out of `ElwhaIconButton.STANDARD` defaults. §4.
7. **Typography forks by variant + collapse state:** small (and any collapsed flexible) = `TITLE_LARGE`/`LABEL_MEDIUM`; medium-flexible expanded = `HEADLINE_MEDIUM`/`LABEL_LARGE`; large-flexible expanded = `DISPLAY_SMALL`/`TITLE_MEDIUM`. §4.
8. **Scroll model:** `setScrollSource(JScrollPane)` + `setLiftOnScroll(boolean)` (default `true`) — **no behavior enum**. Pinned-with-lift is the universal M3 default; flexible variants additionally collapse `exitUntilCollapsed`-style, **scroll-position-driven** (`collapsedFraction = clamp(scrollY / (expandedH − 64))`), re-expanding symmetrically. `enterAlways` deferred (documented, §12). §8.
9. **Scroll plumbing is extracted, responses are not:** new **`theme/ScrollSourceBinding`** (attach/detach to the vertical `BoundedRangeModel`, value+delta callback, source-swap lifecycle) shared machinery à la `RipplePainter`; **`ElwhaFabAnchor` refactors onto it** in the same story. Direction semantics (FAB) and offset semantics (app bar) stay per-component — that's the response, not the plumbing. Settles the stub's #269 cross-component note. §8.
10. **Collapse rendering = the Compose two-row model:** permanent 64 top strip (nav/actions/collapsed-title) + expanded headline block below (margins 16, bottom-anchored); expanded block fades out with the shrinking region, collapsed title fades in near full collapse — **crossfade, no text scaling**. Lift color fades over ~200ms `STANDARD` via `MorphAnimator` on the overlap flip; collapse itself is position-driven (no timer, inherently reduced-motion-safe; the lift fade snaps under reduced motion). §6, §7.
11. **Slots:** `setNavigationIcon(…)` (leading, `ElwhaIconButton` or `MaterialIcons.Symbol` + listener convenience) · `setTitle(String)` / `setSubtitle(String)` (bar-painted text, single line + ellipsis in V1) · `addAction(…)` (trailing `ElwhaIconButton`s; `Symbol` + listener convenience returns the created button) · `addTrailingElement(JComponent)` escape hatch for the Expressive imagery/filled-button/avatar allowance. Action overflow is consumer composition with `ElwhaMenu` — recipe in Javadoc, demoed in the Showcase. §3.
12. **a11y:** the bar is a structural surface — `AccessiblePANEL`-role container whose accessible name is the title (+ subtitle appended); interaction a11y lives on the hosted `ElwhaIconButton`s (already done). Icon-only nav/action buttons require accessible labels (Javadoc on the conveniences). Full RTL mirror via `ComponentOrientation` (slot order, title alignment, insets). §9.
13. **V1 = one phase, six stories** (S1 spike/small-static → S2 flexible-static + display tier → S3 scroll binding + lift → S4 collapse motion → S5 keyboard/a11y/RTL → S6 Showcase + CHANGELOG). §11.

## §1. Scope — what V1 ships

- ✅ `SMALL` / `MEDIUM_FLEXIBLE` / `LARGE_FLEXIBLE`, subtitle on all, `titleCentered` on all, nav-icon + actions + generic trailing elements, lift-on-scroll, flexible collapse, light/dark, reduced motion, RTL, a11y.
- ✅ `TypeRole` display tier (the one token-surface change). `ScrollSourceBinding` extraction + `ElwhaFabAnchor` refactor onto it.
- ✅ Showcase Workbench + Gallery + sidebar entry + headless smokes; CHANGELOG entry.
- ❌ **Search app bar** → stub epic (blocked by #286 text-field anatomy). §12.
- ❌ **Toolbars family** (docked/floating; bottom-bar successor) → stub epic. §12.
- ❌ `enterAlways`, expanded-headline wrapping, action auto-overflow → documented deferrals (wrap gets a follow-up issue). §12.

## §2. Architecture — the load-bearing decision [RECOMMENDED; locked via S1 spike]

**`ElwhaAppBar extends JComponent`** — one class, all variants. A real container: the nav button, action buttons, and trailing elements are Swing children positioned by a custom `doLayout()`; **title and subtitle are painted by the bar itself** (not `JLabel` children) so the collapse crossfade, alignment, ellipsis, and type-role switching are single-owner paint logic, exactly like every other Elwha primitive paints its own text.

**Why not `JToolBar`?** Float/dock machinery, `ToolBarUI` chrome, and separator semantics all fight the M3 anatomy; we'd disable more than we'd use. **Why not a `JPanel` + `JLabel` assembly?** The collapse needs fraction-driven layout + two title layers crossfading under one painter — component-per-text-run turns that into label-juggling; bar-painted text mirrors how `ElwhaCard`/`ElwhaTabs` own their text runs.

**Why one class, not small/collapsing subclasses?** The flexibles' collapsed state *is* the small bar (same 64 strip, same type roles, same slot layout — research §C: Compose's `TwoRowsTopAppBar` embeds the small-bar composition). A variant enum + conditional second row keeps that identity literal: the collapsed rendering shares the small bar's code path, so they can't drift.

**Hosting model:** the bar is an in-flow component — consumers drop it in `BorderLayout.NORTH` above their content/scroll pane. Collapse = `getPreferredSize().height` change + `revalidate()` on the host; no layered-pane tricks, no overlay band (it's not an overlay — contrast `ElwhaFabAnchor`).

**S1 spike proves:** slot layout + bar-painted title render token-correct light+dark in a `BorderLayout.NORTH` host; preferred-height contract (64) behaves under `revalidate()`; `titleCentered` centers over the *container* (M3 renders center-aligned titles centered in the bar, clipped by the button slots — verify the visual against the small-bar render and ellipsize against the slot edges); RTL flips the row. Fallback if bar-painted text fights baselines: degrade title/subtitle to internal `JLabel`s with a shared painter — documented here, not anticipated (Tabs/Card precedent says paint-time text is fine).

## §3. Anatomy, variants & slots

**`AppBarVariant { SMALL, MEDIUM_FLEXIBLE, LARGE_FLEXIBLE }`** — constructor `ElwhaAppBar(AppBarVariant)`, no-arg = `SMALL`; factories `ElwhaAppBar.small()`, `.mediumFlexible()`, `.largeFlexible()` (component-api doctrine: per-variant static factories, single-arg convenience constructors).

| Slot | API | Notes |
|---|---|---|
| Leading button | `setNavigationIcon(ElwhaIconButton)` · convenience `setNavigationIcon(MaterialIcons.Symbol, ActionListener)` (creates a STANDARD icon button, returns it) · `getNavigationIcon()` · `null` clears | one max; absent ⇒ title takes the 16 inset |
| Headline | `setTitle(String)` / `getTitle()` | bar-painted; single line, ellipsis (V1) |
| Subtitle | `setSubtitle(String)` / `getSubtitle()` | all variants; `null`/empty ⇒ absent (flexible expanded height drops to the no-subtitle token) |
| Centering | `setTitleCentered(boolean)` / `isTitleCentered()` | MDC noun; one knob covers title+subtitle (Compose model); applies to small row *and* expanded headline block |
| Trailing actions | `addAction(ElwhaIconButton)` · convenience `addAction(MaterialIcons.Symbol, String accessibleName, ActionListener)` returns the button · `removeAction(…)` · `getActions()` | order = add order, laid trailing-to-lead… laid left→right ending 4 from the trailing edge; gap 0 |
| Trailing escape | `addTrailingElement(JComponent)` / `removeTrailingElement(…)` | the Expressive imagery/avatar/filled-button allowance (`AvatarSize 32` parked — no avatar primitive); vertically centered in the 64 strip |

Actions are `ElwhaIconButton`s — `STANDARD` variant is the M3 anatomy and its `IconBearing` means `ElwhaBadge` rides actions with zero new code (dogfood win). The bar never restyles hosted buttons beyond layout.

**Overflow:** M3 guidance shows up to ~3 actions and an optional `more_vert` overflow — the overflow *menu* is consumer composition with `ElwhaMenu` (recipe in the class Javadoc, live in the Showcase Workbench). No auto-overflow in V1 (§12).

## §4. Tokens & color [zero new tokens, one TypeRole addition — LOCKED]

Full mapping: research §Tokens. Load-bearing rows:

| Part | Role |
|---|---|
| Container at rest | `SURFACE` (shape `NONE`, no shadow) |
| Container lifted (content scrolled under) | `SURFACE_CONTAINER` — tonal only, **no `ShadowPainter` call** (Compose parity; research §I) |
| Title | `ON_SURFACE` |
| Subtitle | `ON_SURFACE_VARIANT` |
| Nav icon / actions | `ON_SURFACE` / `ON_SURFACE_VARIANT` — `ElwhaIconButton.STANDARD` defaults, verified in S1 |

| Variant · state | Title | Subtitle |
|---|---|---|
| SMALL (and any collapsed flexible) | `TITLE_LARGE` | `LABEL_MEDIUM` |
| MEDIUM_FLEXIBLE expanded | `HEADLINE_MEDIUM` | `LABEL_LARGE` |
| LARGE_FLEXIBLE expanded | **`DISPLAY_SMALL`** | `TITLE_MEDIUM` |

**The TypeRole change (deliberate, loud):** add `DISPLAY_LARGE("displayLarge", 57, false)`, `DISPLAY_MEDIUM("displayMedium", 45, false)`, `DISPLAY_SMALL("displaySmall", 36, false)` — the M3 display tier, regular weight, completing the 15-role scale. `TypeRole`'s class Javadoc ("Elwha v1 drops the display tier") is updated to record the app bar as the forcing consumer. `Typography.defaults()` iterates `TypeRole.values()` — Inter derives the three fonts automatically; `ThemePlayground`'s type-scale panel picks them up by the same iteration (verify, S2). This is an *additive* enum change — no consumer breaks. Rejected alternative: mapping onto `HEADLINE_LARGE` (32pt) bends the captured spec (36pt) for no structural saving.

All colors/fonts resolve **at paint time** (`ColorRole.resolve()` / `TypeRole.resolve()` — the binding rule, no caching). Bar-level `setEnabled(false)`: title/subtitle paint at `StateLayer.disabledContentOpacity()` and hosted buttons get `setEnabled(false)` propagated (the slider RANGE-proxy precedent — outer-qualified per #432).

## §5. Measurements & geometry (px=dp)

| Quantity | Value |
|---|---|
| SMALL container height | **64** (subtitle present or not) |
| MEDIUM_FLEXIBLE expanded | **112** / **136** with subtitle |
| LARGE_FLEXIBLE expanded | **120** / **152** with subtitle |
| Flexible collapsed height | **64** |
| Leading space (edge → nav button) | **4** |
| Trailing space (last element → edge) | **4** |
| Gap between trailing icon buttons | **0** (48 targets abut) |
| Icon render | **24** (`MaterialIcons` default) in the 48 `ElwhaIconButton` target |
| Title inset, no nav button | **16** from container edge |
| Title start, nav button present | nav button's right edge (visual gap = the button's internal centering) |
| Expanded headline block margins | **16** start/end |
| Title ↔ subtitle gap | **0** (line metrics carry the separation) |
| Expanded headline bottom padding | start at **24** (medium) / **28** (large) — Compose internals, *tuned visually in S2*, not token-locked |
| Small-row text block | vertically centered; title+subtitle stack centered as a unit |

Preferred size: height per variant/subtitle/collapse state; width = slots + title minimum (title ellipsizes first). No `getMaximumSize` override (#199/#200 doctrine). The bar fills whatever width `BorderLayout.NORTH` grants.

## §6. States & motion

- **Lift:** a single boolean — *content scrolled under the bar* (`scrollY > 0`). On flip, `MorphAnimator` (~200ms, `Easing.STANDARD`) fades container `SURFACE ↔ SURFACE_CONTAINER`; reduced motion snaps. Applies to **every** variant with a scroll source while `liftOnScroll` (default `true`).
- **Collapse (flexibles with a scroll source):** `collapsedFraction = clamp(scrollY / (expandedHeight − 64))`. Height lerps; the expanded headline block's region shrinks and its alpha falls off (most fade in the back half); the collapsed title's alpha rises only near 1.0 — **crossfade, never scaling text** (research §I). Fraction changes ⇒ `revalidate()` + repaint; position-driven means scrubbing the scrollbar scrubs the bar — no timers, no reduced-motion concern for the collapse itself.
- **No state layers on the bar surface** — hover/focus/press belong to the hosted buttons (the token sheet defines none for the container; research §T).
- Gallery hooks: `setLifted(boolean)` / `setCollapsedFraction(float)` force treatments for static rendering, following the slider/switch `setHovered`/`setPressed` hook precedent (exact form matched to that convention in S6); a live scroll source overrides forced values on its next event.

## §7. Collapse rendering (the two-row model)

Layout regions (flexible, fraction *f*):

```
┌──────────────────────────────────────────────┐  ─┐
│ [4][nav 48]  (collapsed title, α rises ~f→1) │   │ 64 — permanent strip
│              [actions…][4]                   │  ─┘
│  [16] Expanded Headline  (α falls with f)    │  ─┐ (expandedH − 64)·(1−f)
│  [16] subtitle                               │  ─┘ bottom-anchored block
└──────────────────────────────────────────────┘
```

- The 64 strip is *literally the small-bar layout/paint path* (variant identity, §2) — nav/actions never move during collapse.
- The headline block is bottom-anchored against the *current* collapse height, so the text slides up as the bar shrinks. ⚠️ S4 correction: it is **not** rectangle-clipped at the strip boundary — the geometry makes that impossible even at rest (the large-flexible 36pt headline tops out above y=64 at 120px), and Compose doesn't clip either; the linear alpha fade carries the transition (Compose parity).
- At `f = 1` the flexible bar **is** a lifted small bar with the collapsed title visible.
- `getCollapsedFraction()` public read-only (smokes, demos, consumer curiosity).

## §8. Scroll behavior & the shared binding

**API:** `setScrollSource(JScrollPane)` / `getScrollSource()` (null clears; re-attach on swap), `setLiftOnScroll(boolean)` / `isLiftOnScroll()` (default `true`). No behavior enum — pinned+lift is M3's universal default; collapse joins automatically for flexible variants. With no scroll source the bar is static-expanded (galleries, dialogs, simple frames).

**`theme/ScrollSourceBinding` (new, the stub's #269 decision):** the extracted *plumbing* — bind to a `JScrollPane`'s vertical scrollbar `BoundedRangeModel`, manage attach/detach across source swaps and `addNotify`/`removeNotify`, deliver `(value, delta)` to one callback. **`ElwhaFabAnchor` refactors onto it in S3** (its `attachedModel`/`prevScrollValue`/threshold code collapses into the binding; `ScrollResponse` direction logic stays put), FAB demos re-smoked. Responses stay per-component by design: FAB = direction+threshold, app bar = absolute offset — sharing those would be abstraction theater. Mirrors the `RipplePainter`/`MorphAnimator` shared-machinery precedent; also the landing zone the radio epic's `RetargetTween` extraction expects.

**Desktop adaptation (documented honestly):** Swing's plain `JScrollPane` can't *consume* scroll the way nested-scroll mobile does — content moves the full `scrollY` while the bar collapses over the first `(expandedH − 64)` px. The visual reads correctly (bar shrinking as content slides under); the Javadoc states the adaptation.

## §9. Accessibility & RTL

- `AccessibleElwhaAppBar extends AccessibleJComponent` — `AccessibleRole.PANEL`; accessible name = title (+ ", " + subtitle when present), live-updated on setters. The bar adds no interactive a11y of its own.
- Hosted `ElwhaIconButton`s carry their own roles/actions (done). The `setNavigationIcon`/`addAction` conveniences take/require accessible-name input (`addAction(Symbol, String accessibleName, listener)`); Javadoc flags icon-only buttons per the MDC content-description requirement.
- Focus traversal: natural child order (nav → trailing elements in layout order) — no roving-focus machinery; these are independent buttons, not a selection group.
- **RTL:** full mirror via `ComponentOrientation` — the nav slot, action order, title alignment (START tracks reading direction; CENTER is symmetric), insets, and the expanded block margins all flip. Verified in S5 with the FabAnchor `componentOrientation` listener precedent.

## §10. Showcase pattern

`AppBarShowcasePanels` + registration under **Components** as **"App Bar"** (`LeafEntry` + Workbench/Gallery, the Tabs shape — ⚠️ corrected at smoke: the app bar is a single-instance primitive like Tabs/Switch/Checkbox, not a multi-instance container like Chip List or the mutex Groups; the Showcase's *Containers* area is reserved for the latter):

- **Workbench:** stage hosts a live `ElwhaAppBar` in `BorderLayout.NORTH` over a **scrollable content stub** (the stub epic's requirement) wired as scroll source. Controls: Variant · Subtitle on/off · Title centered · Nav icon on/off · Action count 0–4 · Overflow-menu demo action (dogfoods `ElwhaMenu`) · Lift on scroll · Enabled · RTL. Code panel re-renders the construction snippet.
- **Gallery:** static matrix — rows: Small / Small+subtitle / Small centered / Medium flexible expanded / +subtitle / Large flexible expanded / +subtitle / collapsed flexible / lifted small (via the gallery hooks); columns capture rest vs lifted where meaningful.
- `ElwhaAppBarShowcaseSmoke` headless guard: panels construct, gallery renders to `BufferedImage`, workbench controls apply, collapse fraction scrubs.

## §11. Phasing → stories (Phase 1 = V1, single phase)

- **S1 — architecture spike + small bar static chrome** — `ElwhaAppBar` skeleton, `AppBarVariant`, slot layout (4/48/title/actions·0/4 + 16 inset), bar-painted title+subtitle (`TITLE_LARGE`/`LABEL_MEDIUM`, ellipsis), `titleCentered`, nav/action/trailing APIs + conveniences, token-correct light+dark, `BorderLayout.NORTH` behavior. *Locks §2.* Demo + smoke.
- **S2 — TypeRole display tier + flexible variants static** — the three `DISPLAY_*` roles (+ Javadoc update + `ThemePlayground` type-scale verification); `MEDIUM_FLEXIBLE`/`LARGE_FLEXIBLE` expanded chrome at 112/136/120/152, expanded headline block (margins 16, bottom padding tune, expanded type roles, subtitle stack, centered option). §4/§5/§7-static. Demo + smoke.
- **S3 — `ScrollSourceBinding` + lift-on-scroll** — the `theme/` binding; `ElwhaFabAnchor` refactor onto it (FAB demos re-smoked); `setScrollSource`/`setLiftOnScroll`; the ~200ms lift fade both directions, reduced-motion snap, all variants. §8/§6. Demo + smoke.
- **S4 — flexible collapse motion** — fraction-driven height + region shrink, headline-block fade-out / collapsed-title fade-in crossfade, scrubbing correctness (drag up/down), `getCollapsedFraction()`, collapse+lift composition at `f=1`. §6/§7. Demo + smoke.
- **S5 — a11y, RTL & enabled** — `AccessibleElwhaAppBar` (PANEL + name splicing), accessible-name conveniences + Javadoc requirements, full RTL mirror (slots, alignment, expanded block, collapse), `setEnabled` propagation + disabled text opacity. §9. Demo + smoke.
- **S6 — Showcase + CHANGELOG** — §10 panels + registration + overflow-`ElwhaMenu` recipe (Javadoc + Workbench) + `ElwhaAppBarShowcaseSmoke`; CHANGELOG `[Unreleased]` entry. *Completes V1; closes the epic.*

## §11.1 S1 spike outcome (2026-06-11)

Confirmed — §2 locked as built. One class, all variants; the nav/trailing children lay out in 48px slots (4px edge spaces, zero gap) with the default `IconButtonSize.M` (40px) button centered in its slot, which lands the 24px glyph exactly 16px from the container edge — the Compose render falls out of the existing icon-button sizing with no restyling. Bar-painted title/subtitle stack centers as a unit in the 64 strip; ellipsis + centered-clamping verified pixel-level in `ElwhaAppBarChromeSmoke` (light + dark). One adaptation from the §3 sketch: `MaterialIcons` exposes per-glyph static methods, not a `Symbol` enum, so the conveniences take `(Icon, String accessibleName, ActionListener)` — the accessible name is required at the convenience layer, which also satisfies the §9 requirement earlier than planned. No fallback needed.

## §12. Out of scope (every cut filed or documented)

- **Search app bar** → **stub epic filed** (V2): the headline slot hosts the M3 search-field anatomy ("icons inside and outside the search bar, centered text") — blocked by `ElwhaTextField` #286 maturity. Not silently cut.
- **Toolbars family** (docked + floating) → **stub epic filed**: the Expressive successor to the deprecated bottom app bar (research §E) — the #287 "bottom app bar sibling" note lands there. A different component family, not an app-bar variant.
- **`enterAlways`** — whole-bar hide on scroll-down; mobile-estate pattern, low desktop value, host-layout churn. Documented deferral (a future behavior flag; `ScrollSourceBinding` already carries the delta it would need).
- **Expanded-headline text wrapping** (Expressive flexibility) → **follow-up issue filed at S6**: needs a multi-line height model on top of the fixed height tokens. V1 is single-line + ellipsis everywhere.
- **Action auto-overflow** — consumer composition with `ElwhaMenu` (Javadoc recipe + Workbench demo); no auto-collapse in V1.
- **Avatar primitive** (`AvatarSize 32`) — `addTrailingElement(JComponent)` carries imagery; a dedicated avatar is its own future discussion.
- **Compress scroll effect** (MDC `layout_scrollEffect="compress"`) — Android-specific polish; not adopted.
