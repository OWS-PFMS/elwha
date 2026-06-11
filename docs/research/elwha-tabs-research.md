# ElwhaTabs — M3 Spec Capture (research scratch)

**Status:** CAPTURE COMPLETE — M3 source material for epic [#425](https://github.com/OWS-PFMS/elwha/issues/425) (ElwhaTabs, `v0.4.0`). Companion research dump (mirrors [`elwha-switch-research.md`](elwha-switch-research.md) / [`elwha-menu-research.md`](elwha-menu-research.md)); the decisions live in [`elwha-tabs-design.md`](elwha-tabs-design.md).

**Captured:** 2026-06-10. **Author:** Charles Bryan (`cfb3@uw.edu`).

**Capture method note:** this epic was specced web-first in a single autonomous run (the ElwhaSwitch #401 pattern) — the token sheets below are the **complete `md-comp-primary-navigation-tab` + `md-comp-secondary-navigation-tab` v0.192 values files** pulled from the material-web repo (the same generated token sets m3.material.io renders), plus the material-web `tabs/internal/` implementation sources for layout, motion, keyboard, and selection truth. No operator screenshot session ran; there is therefore no screenshot log, and the §F capture log lists raw-file URLs instead. The m3.material.io Tabs spec pages are JS-only (WebFetch returns the page title), per the established pattern.

**Consumers / related:**
- **The Elwha Showcase itself** — Workbench/Gallery switchers are `JTabbedPane` today; the long-term dogfood target ([[feedback_dogfood_elwha_components]]). **Not** migrated in this epic.
- **OWS-PFMS/OWS-Local-Search-GUI#244** — `InnerViewTabStrip` currently targets `ElwhaChip` `SINGLE_MANDATORY` tab-strip semantics; a true M3 tab bar is the better destination once this ships.
- [`ChipSelectionMode.SINGLE_MANDATORY`](../../src/main/java/com/owspfm/elwha/chip/list/ChipSelectionMode.java) — prior art for exactly-one-mandatory selection semantics (click-active = no-op, auto-select-first).
- [`ElwhaNavRailDestination`](../../src/main/java/com/owspfm/elwha/navrail/ElwhaNavRailDestination.java) — the dedicated-primitive template (`extends JComponent implements IconBearing`): state layers, ripple, focus ring, badge hosting, TOGGLE_BUTTON a11y.
- [`ElwhaButtonGroup`](../../src/main/java/com/owspfm/elwha/buttongroup/ElwhaButtonGroup.java) — container-of-children precedent: roving focus, group-stamped child config.
- [`MorphAnimator`](../../src/main/java/com/owspfm/elwha/theme/MorphAnimator.java) + [`Easing`](../../src/main/java/com/owspfm/elwha/theme/Easing.java) — drive the indicator slide and the scroll-to-tab tween.
- [`ElwhaBadge`](../../src/main/java/com/owspfm/elwha/badge/ElwhaBadge.java) + [`ElwhaBadgeAnchor`](../../src/main/java/com/owspfm/elwha/badge/ElwhaBadgeAnchor.java) + `IconBearing` — the badge in tab anatomy item 2 maps directly onto the existing anchor primitive.

---

## §TL;DR — synthesis (read this first)

**What M3 Tabs are**, distilled from the capture below:

1. **Two variants, one anatomy family.** **Primary tabs** (main content destinations, under a top app bar) and **Secondary tabs** (within a content area, for hierarchy). "Use the same type of tab in a tab bar" — the variant is a property of the *bar*, not of individual tabs. (§A, §C.)
2. **Zero new theme tokens.** Every color maps onto existing `ColorRole`; opacities onto `StateLayer`; typography is `TypeRole.TITLE_SMALL`; shapes are corner-none + the indicator's 3px top-rounding. (§Tokens.)
3. **Anatomy (§C):** **container** · optional **badge** · optional **icon** (primary only, per MDC anatomy — but see §A: material-web renders inline icons in secondary tabs too) · **label** · **divider** (full-width, bottom) · **active indicator**.
4. **The indicator is the variant fork (§T):** primary = **3px tall, top corners rounded 3px, hugs the content width**; secondary = **2px tall, square, spans the full tab width**. Both `PRIMARY` color.
5. **Geometry (§T, §I):** bar height **48** (inline content) or **64** (primary stacked icon+label); tab horizontal padding **16**; inline icon↔label gap **8**; stacked gap **2**; icon **24**; container `SURFACE`, elevation 0, shape none. Scrollable-mode tab min-width **72**, max-width **264** (MDC).
6. **Color (§T):** active label/icon = `PRIMARY` in primary tabs, `ON_SURFACE` in secondary tabs; inactive = `ON_SURFACE_VARIANT` (hover/focus/pressed lift to `ON_SURFACE`). State layers: active = `PRIMARY` tint; inactive = `ON_SURFACE` — **except primary-variant inactive-pressed, which is `PRIMARY`** (verbatim sheet quirk, §T).
7. **Indicator motion (§I):** **250ms, emphasized easing**, FLIP-style translate+scale from the previous tab's indicator rect to the new one — i.e. **x and width interpolate together**. Reduced-motion → opacity snap. (MDC-Android's default is a fancier `elastic` mode, 250ms — noted as a deviation option, not adopted.)
8. **Selection model (§I):** **exactly one tab is always active** — first tab auto-activates when none is (no change event for that initial auto-activation); clicking the active tab is a no-op; removing the active tab re-activates the first. `change` fires on **user** selection only. M3's noun is **`active`** — material-web explicitly deprecates `selected` for tabs.
9. **Keyboard (§I):** bar = `tablist`, tabs = `tab` roles. **Roving focus** with wrap-around: Left/Right (RTL-aware) move focus, Home/End jump, Enter/Space activate, optional **`autoActivate`** selects on focus move; on focus leaving the bar, the tab stop returns to the active tab.
10. **Scrollable (§A, §I):** fixed mode = equal-width tabs filling the bar (material-web `flex: 1`); scrollable mode = content-width tabs, overflow scrolls with **scroll-to-tab keeping a 48px margin**, smooth-scrolled, active tab kept visible on child mutations.
11. **Badges (§C):** tabs are a badge host (anatomy item 2) — MDC attaches `BadgeDrawable` per tab. Maps onto `ElwhaBadgeAnchor` + `IconBearing` day-one.
12. **No disabled tokens (§T, §Open):** the M3 tab token sheets define **no disabled states** and material-web's Tab has no `disabled` property. MDC supports disabling but M3 guidance avoids it.
13. **Expressive status (§E):** the May-2025 M3 Expressive update did **not** publish a revised Tabs spec — no new variants, no shape morphs. The current m3.material.io Tabs spec is the operative one.

### Reading-order TOC
§A material-web overview/API → §C MDC-Android anatomy + attributes → §T token sheets (complete v0.192, both variants) → §I implementation internals (layout / motion / keyboard / selection / scroll) → §E Expressive status → §Tokens Elwha mapping → §P terminology lock → §Open → §F capture log.

---

## §A. Material Web overview + API (authoritative text)

Source: <https://raw.githubusercontent.com/material-components/material-web/main/docs/components/tabs.md> (fetched 2026-06-10).

> Tabs **organize groups of related content that are at the same level of hierarchy**.

> **Primary tabs** are placed at the top of the content pane under a top app bar. They display the main content destinations. **Secondary tabs** are used within a content area to further separate related content and establish hierarchy.

> Tabs contain multiple primary or secondary tab children. **Use the same type of tab in a tab bar.** `[CODE]` — the variant belongs to the bar; Elwha stamps it from the container.

```html
<md-tabs>
  <md-primary-tab>Video</md-primary-tab>
  <md-primary-tab active>Photos</md-primary-tab>
  <md-primary-tab>Audio</md-primary-tab>
</md-tabs>
```

**`<md-tabs>` API:**

| Property | Type | Default | Notes |
|---|---|---|---|
| `activeTabIndex` / `activeTab` | number / Tab | — | the selection surface — M3's noun is **active** `[CODE]` |
| `autoActivate` | boolean | `false` | select a tab when it is focused (keyboard) `[CODE]` |
| `tabs` | Tab[] | — | the tab children `[CODE]` |
| `scrollToTab(tab?)` | method | — | scrolls the toolbar, if overflowing, to the active (or given) tab `[CODE]` |

Events: **`change`** — fired when the selected tab changes **via user interaction** (click / Space / Enter / auto-activate focus move); cancellable (preventDefault on the triggering event reverts). Not fired for the initial auto-activation of the first child. `[CODE]` → Swing mapping in design §6/§7.

**`<md-primary-tab>` / `<md-secondary-tab>` API:**

| Property | Type | Default | Notes |
|---|---|---|---|
| `active` | boolean | `false` | **`selected` is explicitly deprecated in favor of `active`** `[CODE]` — terminology lock §P |
| `inline-icon` | boolean | `false` | primary tabs only: show the icon inline (like secondary) instead of stacked `[CODE]` |
| `has-icon` / `icon-only` | boolean | auto | derived from slotted content `[CODE]` (Elwha: derived from configured icon/label) |

**Icons:** "Tabs may optionally show an icon… Icons communicate the type of content within a tab. Icons should be simple and recognizable." `[DOC]` Tabs may be **icon-only** (no label). `[CODE]` The secondary-tab usage example in the doc shows **icons in secondary tabs** (flight/hotel/hiking) — rendered **inline** — even though MDC's secondary anatomy (§C) omits the icon item. ⚠️ See §Open-1.

**A11y (web):** add `aria-label` to `<md-tabs>` and to any tab whose label needs to be more descriptive, **such as icon-only tabs** `[CODE]` (accessible-name API required). Every tab must reference a `role="tabpanel"` element with `aria-controls`; panels labelled via `aria-labelledby` pointing back at the tab `[DOC]` — panel wiring is consumer-side composition on the web too; Swing mapping in design §8.

**Web theming surface (the 5 headline tokens, both variants identical shape):** `container-color` (= `surface`), `label-text-font` (= `title-small`), `active-indicator-color` (= `primary`), `icon-color` (= `on-surface-variant`), `container-shape` (= `corner-none`).

## §C. MDC-Android anatomy + attributes (authoritative text)

Source: <https://raw.githubusercontent.com/material-components/material-components-android/master/docs/components/Tabs.md> (fetched 2026-06-10).

**Anatomy — primary tabs:** 1. **Container** · 2. **Badge** (optional) · 3. **Icon** (optional) · 4. **Label** · 5. **Divider** · 6. **Active indicator**. `[CODE]`

**Anatomy — secondary tabs:** 1. **Container** · 2. **Badge** (optional) · 3. **Label** · 4. **Divider** · 5. **Active indicator**. `[CODE]` — ⚠️ note: **no icon item** in the secondary anatomy, but material-web's secondary examples render inline icons (§A). §Open-1.

**The divider is anatomy** (item 5/4) — the full-width hairline under the bar, with the active indicator painted over it. `[CODE]`

Attribute highlights (defaults are M3 spec values):

| Element | Default | Notes |
|---|---|---|
| Container height | `48dp` (inline text) or `72dp` (non-inline text and icon) | ⚠️ MDC's 72dp is the **M2-era** stacked height; the M3 v0.192 token is **64px** (§T) — token sheet wins `[CODE]` |
| Tab mode | `fixed` (vs `scrollable`) | `[CODE]` |
| Icon tint | `colorOnSurfaceVariant`, `colorPrimary` activated | `[CODE]` |
| Label color | `colorOnSurfaceVariant`, `colorPrimary` activated | `[CODE]` |
| Typography | `textAppearanceTitleSmall` | `[CODE]` |
| Inline label | `tabInlineLabel` false | the stacked↔inline axis `[CODE]` |
| Ripple | `colorOnSurfaceVariant` 16% / `colorPrimary` 16% activated | MDC folds hover+press into one 16%; Elwha uses the per-state `StateLayer` opacities from the token sheet instead `[CODE]` |
| Min width | `72dp` (scrollable) or wrap | `[CODE]` scrollable-mode floor |
| Max width | `264dp` | `[CODE]` cap in both modes |
| Padding | start/end `12dp` | ⚠️ material-web's implementation uses `padding: 0 16px` (§I) — 16 adopted; MDC's 12 is the M2 holdover `[CODE]` |
| Indicator color | `colorPrimary` | `[CODE]` |
| Indicator drawable | `m3_tabs_rounded_line_indicator` | the rounded-line primary indicator `[CODE]` |
| Indicator height | `2dp` | MDC's shared default; v0.192 splits 3px primary / 2px secondary (§T) `[CODE]` |
| Indicator full width | `false` | primary hugs content; secondary spans (§T shape note) `[CODE]` |
| Indicator animation mode | **`elastic`**, duration **250** | leading-edge-faster stretch; material-web does plain translate+scale at 250ms (§I). Elwha adopts material-web's. `[CODE]` |
| Indicator gravity | `bottom` | `[CODE]` |

**Badges:** `tab.getOrCreateBadge()` / `tab.removeBadge()` — tabs are first-class badge hosts, with numberless / count / text content and content-description setters. `[CODE]` → `ElwhaBadgeAnchor`.

**Fixed tabs:** "display all tabs on one screen, with each tab at a fixed width. The width of each tab is determined by dividing the number of tabs by the screen width." `[CODE]`

**Scrollable tabs:** "displayed without fixed widths. They are scrollable, such that some tabs will remain off-screen until scrolled." `[CODE]`

**A11y:** content descriptions on the `TabLayout` and per tab; badge content descriptions. `[CODE]`

**ViewPager mediators** (`TabLayoutMediator`, swipe-sync): Android pager plumbing — N/A to Swing; content switching is consumer-side. `[DOC]`

## §T. Token sheets — complete v0.192 (verbatim)

Sources: <https://raw.githubusercontent.com/material-components/material-web/main/tokens/versions/v0_192/_md-comp-primary-navigation-tab.scss> and `_md-comp-secondary-navigation-tab.scss` (fetched 2026-06-10). These are the generated component-token sets the m3.material.io spec pages render. Note the component's formal token name: **`primary-navigation-tab`** / **`secondary-navigation-tab`**.

### Primary navigation tab

**Dimensions + shape `[CODE]`:**

| Token | Value |
|---|---|
| `container-height` | **48px** |
| `with-icon-and-label-text-container-height` | **64px** |
| `container-shape` | `corner-none` |
| `container-elevation` | level0 |
| `active-indicator-height` | **3px** |
| `active-indicator-shape` | **3px 3px 0px 0px** (top corners rounded) |
| `with-icon-icon-size` | **24px** |

**Typography `[CODE]`:** `label-text` = `title-small` (font/size/line-height/weight/tracking).

**Color `[CODE]`:**

| Token | Role |
|---|---|
| `container-color` | `surface` |
| `active-indicator-color` | `primary` |
| `with-label-text-inactive-label-text-color` | `on-surface-variant` |
| `with-label-text-inactive-{hover,focus,pressed}-label-text-color` | `on-surface` |
| `with-label-text-active-label-text-color` (incl. hover/focus/pressed) | `primary` |
| `with-icon-inactive-icon-color` | `on-surface-variant` |
| `with-icon-inactive-{hover,focus,pressed}-icon-color` | `on-surface` |
| `with-icon-active-icon-color` (incl. hover/focus/pressed) | `primary` |

**State layers `[CODE]`:**

| Token | Role @ opacity |
|---|---|
| `inactive-hover-state-layer` | `on-surface` @ hover (0.08) |
| `inactive-focus-state-layer` | `on-surface` @ focus (0.10) |
| `inactive-pressed-state-layer` | ⚠️ **`primary`** @ pressed (0.10) — the press flashes the destination color even on inactive tabs (verbatim) |
| `active-hover-state-layer` | `primary` @ hover |
| `active-focus-state-layer` | `primary` @ focus |
| `active-pressed-state-layer` | `primary` @ pressed |

**No disabled tokens exist in the sheet.** §Open-2.

### Secondary navigation tab

**Dimensions + shape `[CODE]`:**

| Token | Value |
|---|---|
| `container-height` | **48px** |
| `container-shape` | `corner-none` |
| `container-elevation` | level0 |
| `active-indicator-height` | **2px** |
| `active-indicator-shape` | **0** (square; synthesized in the consumer sheet — the versioned sheet omits it) |
| `with-icon-icon-size` | **24px** |

**No `with-icon-and-label-text-container-height` token exists for secondary** — a comment in the consumer sheet's unsupported-tokens block claims "height is 48 and it's 64 with icon", but no such token or rendering exists anywhere (material-web renders secondary icons inline at 48px). **Token-table ghost — parked.** Secondary = 48px always. §Open-1.

**Typography `[CODE]`:** `label-text` = `title-small`.

**Color `[CODE]`:**

| Token | Role |
|---|---|
| `container-color` | `surface` |
| `active-indicator-color` | `primary` |
| `inactive-label-text-color` | `on-surface-variant` |
| `active-label-text-color` | **`on-surface`** (the variant's signature: active text does *not* go primary) |
| `{hover,focus,pressed}-label-text-color` | `on-surface` |
| `with-icon-inactive-icon-color` | `on-surface-variant` |
| `with-icon-{active,hover,focus,pressed}-icon-color` | `on-surface` |

**State layers `[CODE]`:** hover/focus/pressed all `on-surface` at the standard opacities; the consumer sheet synthesizes the `active-*` variants to the same values (no active/inactive fork for secondary).

## §I. Implementation internals (material-web `tabs/internal/`)

Sources: `_tabs.scss`, `_tab.scss`, `_primary-tab.scss`, `_secondary-tab.scss`, `tabs.ts`, `tab.ts` (fetched 2026-06-10).

**Layout `[CODE]`:**
- Tab: `padding: 0 16px`; content row centered; `gap: 8px` between inline icon and label; `white-space: nowrap`.
- Primary stacked content: `flex-direction: column; gap: 2px`; with icon+label, content height = 64px.
- Bar: `align-items: end` (inline-height tabs bottom-align in a 64px bar), tabs get `flex: 1` (equal widths — the fixed-mode behavior), `overflow: auto` with hidden scrollbars + smooth scroll (the scrollable behavior; material-web folds both modes into CSS, MDC's explicit fixed/scrollable fork is the better API surface).
- Container color paints **per tab** (`.button::before`, full tab bounds, `z-index: -1`); divider is a separate full-width element below.

**Indicator `[CODE]`:**
- Primary: indicator div lives **inside `.content`** → spans the content width (icon/label cluster + its own bounds), bottom-aligned.
- Secondary: `fullWidthIndicator` → indicator div spans the **full tab** width.
- Hidden (`opacity: 0`) unless the tab is active.

**Indicator motion (`tab.ts [ANIMATE_INDICATOR]`) `[CODE]`:**
- On activation with a previous tab: FLIP — measure previous indicator rect and new indicator rect, animate `translateX(fromPos − toPos) scaleX(fromWidth/toWidth)` → `transform: none`, **duration 250ms, `EASING.EMPHASIZED`**.
- Reduced motion (`prefers-reduced-motion`): opacity snap, no slide.
- The newly-active tab is raised (`z-index: 1`) so its indicator animates over the previous one — a web-compositing detail; a Swing container painting **one** indicator avoids the entire two-indicator dance.

**Selection (`tabs.ts activateTab`) `[CODE]`:**
- Exactly-one semantics: activating sets `active` on one tab, clears the rest.
- `handleSlotChange`: if no tab is active and children exist → **activate the first** (no `change` event); also re-scrolls to keep the active tab visible when children shift.
- Click on the already-active tab → ignored.
- `change` fires only with a previous tab (user interactions); cancellable → revert.
- After activation: focus bookkeeping (`updateFocusableTab`) + `scrollToTab(activeTab)`.

**Keyboard (`tabs.ts handleKeydown/handleKeyup/handleFocusout`) `[CODE]`:**
- Bar role `tablist`; tab role `tab`; `aria-selected` reflects active.
- Roving tabIndex: only one tab is the tab stop (the focused one; restored to the active tab when focus leaves the bar).
- ArrowLeft/ArrowRight move focus with **wrap-around**, **RTL-aware** (direction `rtl` flips which arrow is "forwards"); Home/End jump to first/last; if no tab is focused yet, forwards starts at 0, backwards at last.
- `autoActivate`: focus move also activates; otherwise Enter/Space (tab.ts keydown) activate the focused tab.
- On keyup: scroll the focused (else active) tab into view.

**Scroll-to-tab (`tabs.ts scrollToTab`) `[CODE]`:**
- Target scroll keeps the tab visible with a **48px `scrollMargin`** on either side: `to = min(offset − 48, max(offset + extent − hostExtent + 48, scroll))`.
- Smooth-scrolled (CSS `scroll-behavior: smooth`) when focus is in the bar; instant otherwise.

**Focus ring `[CODE]`:** inward, 8px shape; on the active tab its bottom edge sits above the indicator (`margin-bottom: indicator-height + 1`).

## §E. M3 Expressive status

The May-2025 M3 Expressive update (button groups, FAB menu, split button, toolbar, loading indicator + refreshed buttons/FABs/navigation) **did not publish a revised Tabs spec** — no Expressive variants, sizes, or shape-morph behaviors for tabs; 2025–2026 coverage of "expressive tabs" is all about Chrome's tab-grid UI, not the Tabs component. The operative spec is the current m3.material.io Tabs page (= the v0.192 token sheets above). Nothing is being skipped: there is no deprecated-baseline/Expressive fork for this component.

## §Tokens — Elwha mapping (zero new tokens)

| M3 token | Elwha |
|---|---|
| `surface` (container) | `ColorRole.SURFACE` |
| `primary` (indicator; primary-variant active content; inactive-pressed layer) | `ColorRole.PRIMARY` |
| `on-surface` (secondary active content; lifted inactive content; layers) | `ColorRole.ON_SURFACE` |
| `on-surface-variant` (inactive content) | `ColorRole.ON_SURFACE_VARIANT` |
| divider color (M3 divider = `outline-variant`) | `ColorRole.OUTLINE_VARIANT` |
| hover/focus/pressed layer opacities | `StateLayer.HOVER` (0.08) / `FOCUS` (0.10) / `PRESSED` (0.10) + `StateLayer.over(...)` |
| `title-small` label | `TypeRole.TITLE_SMALL.resolve()` |
| `corner-none` container | no rounding (plain rect) |
| indicator 3px top-round | component-local geometry (a 3px arc on a painted shape — not a `ShapeScale` value, same class of local constant as the switch's overshoot bezier) |
| icon 24px | `MaterialIcons` default 24px render |
| emphasized 250ms indicator slide | `MorphAnimator` (250ms) + `Easing.EMPHASIZED` |
| smooth scroll-to-tab | `MorphAnimator` (`MEDIUM2_MS` 300) + `Easing.STANDARD` |
| focus ring 8px inward | the established Elwha focus-ring treatment + `ShapeScale.SM` (8) |

## §P. Terminology → API lock (M3 nouns, exactly)

| M3 noun | Elwha API |
|---|---|
| Tabs (the component/bar) | `ElwhaTabs` |
| Primary tabs / Secondary tabs (variants of the bar) | `TabsVariant.PRIMARY` / `TabsVariant.SECONDARY` |
| Tab (the item) | `ElwhaTab` |
| **active** (material-web deprecates `selected` for tabs) | `activeTabIndex` / `activeTab` / `ElwhaTab.isActive()` |
| auto-activate | `setAutoActivate(boolean)` |
| inline icon | `setInlineIcon(boolean)` |
| Active indicator | "indicator" in identifiers/docs |
| Divider | "divider" |
| Fixed / Scrollable tabs | `TabMode.FIXED` / `TabMode.SCROLLABLE` |
| scrollToTab | `scrollToTab(ElwhaTab)` |

## §Open — resolved in the design doc

1. **Secondary tabs + icons:** MDC anatomy omits the icon; material-web's docs/styles render secondary icons inline at 48px; the "64 with icon" comment in the consumer sheet is a token ghost with no token behind it. → **Resolution:** secondary tabs support inline icons (material-web parity), height stays 48; design §3.
2. **Disabled:** no disabled tokens for tabs in v0.192; material-web Tab has no `disabled`; MDC allows it; M3 guidance avoids disabled tabs. → **Resolution:** whole-bar `setEnabled(false)` honored with the standard library disabled treatment (content @ 0.38, interaction off) as a Swing-table-stakes extension; **no per-tab disabled API** in V1; design §10.
3. **Indicator animation mode:** MDC `elastic` (leading edge stretches ahead) vs material-web linear translate+scale, both 250ms. → **Resolution:** material-web's translate+scale (x+width interpolate together) — design §6; elastic noted as possible future polish.
4. **Tab padding 12 vs 16:** MDC table says 12dp, material-web paints 16px. → **Resolution:** 16 (the rendered truth); design §5.
5. **MDC 72dp stacked height:** M2 holdover; v0.192 says 64. → **Resolution:** 64; design §5.

## §F. Capture log (raw-file URLs, fetched 2026-06-10)

- <https://raw.githubusercontent.com/material-components/material-web/main/docs/components/tabs.md>
- <https://raw.githubusercontent.com/material-components/material-components-android/master/docs/components/Tabs.md>
- <https://raw.githubusercontent.com/material-components/material-web/main/tokens/_md-comp-primary-tab.scss> (+ `_md-comp-secondary-tab.scss`) — consumer sheets: supported-token lists, renames, secondary synthesized actives
- <https://raw.githubusercontent.com/material-components/material-web/main/tokens/versions/v0_192/_md-comp-primary-navigation-tab.scss> (+ `_md-comp-secondary-navigation-tab.scss`) — verbatim values
- <https://raw.githubusercontent.com/material-components/material-web/main/tabs/internal/_tabs.scss> / `_tab.scss` / `_primary-tab.scss` / `_secondary-tab.scss` / `tabs.ts` / `tab.ts` — layout, motion, keyboard, selection, scroll truth
- WebSearch sweep for "M3 Expressive tabs" (2026-06-10) — confirms no Expressive tabs respec (§E)
