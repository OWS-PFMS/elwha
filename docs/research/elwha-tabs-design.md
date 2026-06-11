# ElwhaTabs — Phase 0 Design

**Status:** LOCKED (S1 spike confirms §2). **Drafted:** 2026-06-10. **Author:** Charles Bryan (`cfb3@uw.edu`).
**Parents:** epic [#425](https://github.com/OWS-PFMS/elwha/issues/425). **Milestone:** `v0.4.0`.
**Research:** [`elwha-tabs-research.md`](elwha-tabs-research.md) — the captured spec; this doc holds only the decisions.

---

## TL;DR — the locked decisions

1. **Two dedicated primitives:** `ElwhaTabs extends JComponent` (the bar/container) + `ElwhaTab extends JComponent implements IconBearing` (the tab) in `com.owspfm.elwha.tabs` — **not** a styled `JTabbedPane`/`TabbedPaneUI`. The M3 component is a *tab bar*; content panels are consumer composition. [RECOMMENDED; locked via S1 spike — §2]
2. **The container owns the chrome that spans tabs:** custom layout (fixed/scrollable + scroll offset), the full-width `OUTLINE_VARIANT` divider, and the **single animated active indicator**. Tabs own their content paint (icon/label), state layers, ripple, focus ring, and leaf a11y. §2.
3. **Variant lives on the bar** (`TabsVariant.PRIMARY` / `SECONDARY`, per-variant static factories), stamped onto tabs — M3: "Use the same type of tab in a tab bar." §3.
4. **Zero new theme tokens** — colors map onto `ColorRole`, opacities onto `StateLayer`, type onto `TypeRole.TITLE_SMALL` (research §Tokens). §4.
5. **Geometry (px=dp):** bar height 48 (inline) / 64 (primary stacked icon+label); tab padding 0×16; inline gap 8 / stacked gap 2; icon 24; primary indicator 3px tall top-rounded 3px hugging **content width**; secondary indicator 2px square spanning **full tab width**; divider 1px. Scrollable tab width clamps [72, 264]. §5.
6. **Selection = exactly-one-mandatory** (ChipList `SINGLE_MANDATORY` semantics): auto-activate first tab (silently), click-active no-op, active-removal re-activates first. M3 noun **active** (`activeTabIndex`/`activeTab`; material-web deprecates `selected`). `ChangeListener` fires on any active-tab change; per-tab `ActionListener` fires on user activation only (the Switch listener split). §6, §7.
7. **Indicator motion:** one container-level `MorphAnimator` (250ms, `Easing.EMPHASIZED`) interpolating the indicator rect (x + width together) from the previous tab to the new one; reduced-motion / not-displayable snap. §6.
8. **Scrollable mode is hand-rolled** in the container (scroll offset + clip, no `JScrollPane`): wheel scrolls, activation/focus auto-scrolls with the 48px margin via a 300ms `STANDARD` tween. §7.
9. **Keyboard = the material-web tablist contract:** roving tab stop, Left/Right wrap-around (RTL-aware), Home/End, Enter/Space activate, optional `autoActivate`, focus-leave restores the tab stop to the active tab. §7.
10. **Badges day-one:** `ElwhaTab implements IconBearing`; `setBadge(ElwhaBadge)` mirrors `ElwhaNavRailDestination` (icon tabs anchor `ICON_CORNER`; label-only tabs `TRAILING_EDGE`). §3.
11. **a11y:** bar = `AccessibleRole.PAGE_TAB_LIST` + `AccessibleSelection`; tab = `PAGE_TAB` + `AccessibleState.SELECTED` + one "click" `AccessibleAction`; icon-only tabs require `setAccessibleLabel`. RTL mirrors layout, arrows, indicator, and scroll math via `ComponentOrientation`. §8.
12. **V1 = one phase, seven stories** (S1 spike/static → S2 interaction → S3 motion → S4 icons+badges → S5 scrollable → S6 keyboard/a11y/RTL → S7 Showcase). §11.

## §1. Scope — what V1 ships

- ✅ Both M3 variants (primary/secondary), both tab modes (fixed/scrollable), icons (stacked / inline / icon-only), badges, the animated indicator, divider, full keyboard + a11y + RTL, light/dark, reduced motion.
- ✅ Showcase Workbench + Gallery + sidebar entry + headless smokes; CHANGELOG entry.
- ❌ Content-panel management (`CardLayout` binding, pager sync) — consumer composition, recipe documented in Javadoc + demoed in the Showcase. §10.
- ❌ Per-tab disabled; `OnSurface`-style alternate container colors. §10.
- **No excluded M3 variants exist** — Expressive published no tabs respec (research §E); nothing is stubbed or cut.

## §2. Architecture — the load-bearing decision [RECOMMENDED; locked via S1 spike]

**`ElwhaTabs extends JComponent`** is a real Swing container holding **`ElwhaTab extends JComponent`** children, with a custom `doLayout()`. The bar paints, in order: container fill (`SURFACE`), children (Swing), then divider + the **one** active indicator in `paint()` after children (`paintChildren` → divider/indicator overlay — exact hook chosen in S1: the indicator must paint *over* tab state layers and the divider, matching web z-order).

**Why not `JTabbedPane` + custom UI?** `JTabbedPane` is bar+pages welded together; `BasicTabbedPaneUI`'s run-wrapping multi-row tab layout, content-border painting, and placement machinery all fight the M3 anatomy (single row, scrolling strip, content-width indicator, per-tab badges). The M3 component is *just the bar* — and the bar's interaction surface is finite and fully captured (research §I): click, arrows/Home/End/Enter/Space, wheel scroll. Mirrors `ElwhaNavRailDestination`/`ElwhaButtonGroup` precedent: dedicated `JComponent` primitives when M3 anatomy is involved.

**Why the container paints the indicator** (not the tabs): during the slide the indicator is *between* two tabs — web fakes this with two indicators and z-index games (research §I); a Swing container painting one rect in bar coordinates is strictly simpler and can never double-paint. Tabs expose their content bounds (`indicatorSpan()`, package-private) so the bar can compute the primary variant's content-hugging width.

**S1 spike proves:** both variants' static chrome paints token-correct light+dark; bar+tabs lay out at 48/64 heights in `BorderLayout.NORTH` and `FlowLayout` hosts; the indicator rect computes correctly for content-width (primary) and full-width (secondary) at rest; selection API round-trips with `ChangeListener`. Fallback documented here if the spike fails (none anticipated — this is the nav-rail's proven path).

## §3. Anatomy / primitives

**`ElwhaTab`** — the dedicated tab primitive (M3 anatomy: badge · icon · label):

- Static factories (component-api doctrine): `ElwhaTab.of(String label)`, `of(MaterialIcons.Symbol icon, String label)`, `of(Icon icon, String label)`, `iconOnly(MaterialIcons.Symbol icon, String accessibleLabel)`.
- Content forms: label-only · icon+label (primary: **stacked** by default, `setInlineIcon(true)` for inline; secondary: always inline) · icon-only.
- `setBadge(ElwhaBadge)` / `getBadge()` — anchors via `ElwhaBadgeAnchor` (`ICON_CORNER` when an icon exists, `TRAILING_EDGE` for label-only); `implements IconBearing.getIconBounds()` (label bounds stand in when no icon — S4 verifies the anchor math).
- `isActive()` — read-only public; activation flows through the bar (`setActive` is package-private, stamped by `ElwhaTabs`).
- `addActionListener(ActionListener)` — fires on **user** activation of this tab (click / Space / Enter / auto-activate focus), never programmatic.
- Variant + bar-derived config (variant, mode) are stamped by the container on add — a tab is meaningless outside a bar.

**`ElwhaTabs`** — the bar:

- `ElwhaTabs()` (PRIMARY) / `ElwhaTabs(TabsVariant)`; factories `ElwhaTabs.primary()` / `ElwhaTabs.secondary()`.
- `addTab(ElwhaTab)` (+ `addTab(String label)` / varargs convenience), `removeTab(ElwhaTab)`, `getTabCount()`, `getTabAt(int)`.
- `getActiveTabIndex()` / `setActiveTabIndex(int)` / `getActiveTab()` / `setActiveTab(ElwhaTab)`; `addChangeListener(ChangeListener)`.
- `setAutoActivate(boolean)`, `setTabMode(TabMode)`, `scrollToTab(ElwhaTab)`.
- Paints `SURFACE` fill, the 1px `OUTLINE_VARIANT` divider along the bottom, and the active indicator (over the divider).

## §4. Tokens & color [zero new tokens — LOCKED]

Full mapping: research §Tokens / §T; load-bearing rows:

| Part | PRIMARY variant | SECONDARY variant |
|---|---|---|
| Container | `SURFACE` | `SURFACE` |
| Divider | `OUTLINE_VARIANT` | `OUTLINE_VARIANT` |
| Indicator | `PRIMARY` | `PRIMARY` |
| Active label+icon | `PRIMARY` (all states) | `ON_SURFACE` (all states) |
| Inactive label+icon | `ON_SURFACE_VARIANT`; hover/focus/pressed → `ON_SURFACE` | same |
| State layer, active | `PRIMARY` | `ON_SURFACE` |
| State layer, inactive | `ON_SURFACE` — **except pressed → `PRIMARY`** (research §T quirk, kept verbatim) | `ON_SURFACE` |

Layer opacities: `StateLayer.HOVER`/`FOCUS`/`PRESSED`; press ripple via `RipplePainter` clipped to the tab rect, tinted with the pressed layer color. Bar-level `setEnabled(false)`: content @ `StateLayer.disabledContentOpacity()` (0.38), divider/indicator unchanged, interaction off (§10 rationale). All colors resolved **at paint time** (`ColorRole.resolve()` — the binding rule, no caching).

## §5. Measurements & geometry

| Quantity | Value |
|---|---|
| Bar height | 48 — or 64 when PRIMARY and any tab shows stacked icon+label |
| Tab padding | 0 vertical, 16 horizontal |
| Inline icon↔label gap | 8 |
| Stacked icon↔label gap | 2 |
| Icon | 24 (`MaterialIcons` default render) |
| Label type | `TypeRole.TITLE_SMALL`, single line, no wrap |
| Primary indicator | 3px tall, top corners rounded 3px, width = the tab's **content span** (icon/label cluster), bottom-aligned |
| Secondary indicator | 2px tall, square, full tab width |
| Divider | 1px, full bar width, bottom edge (indicator paints over it) |
| FIXED mode | tabs share the bar width equally |
| SCROLLABLE mode | tab width = preferred (content + 32 padding) clamped to [72, 264]; label ellipsizes at the cap |
| Scroll margin | 48 (scroll-to-tab keeps this much of the neighbor visible) |
| Focus ring | inward rounded rect, `ShapeScale.SM` (8); on the active tab the ring bottom sits above the indicator (+1) |

Inline-height tabs **bottom-align** in a 64 bar (web `align-items: end`). Preferred bar size: height per above; width = sum of tab preferred widths (SCROLLABLE) or the same sum as a sane minimum (FIXED fills whatever the layout grants). No `getMaximumSize` override (#199/#200 doctrine).

## §6. States & motion

**Static layers (per tab):** hover paints the tint role @ `HOVER` over the full tab rect; keyboard focus paints @ `FOCUS` + the focus ring (`FocusVisible.isKeyboardCause` gating, the established convention); press paints @ `PRESSED` + `RipplePainter` ripple from the press point clipped to the tab. Gallery hooks `setHovered(boolean)` / `setPressed(boolean)` (slider/switch precedent) force treatments for static rendering.

**Indicator slide:** the bar keeps `indicatorRect` (bar coordinates). On activation change while displayable: `fromRect` = current animated rect, `toRect` = new active tab's rest rect; one `MorphAnimator(bar, 250)` + `Easing.EMPHASIZED` interpolates x and width together (material-web's translate+scale, research §I; MDC's `elastic` mode explicitly not adopted — research §Open-3). Reduced motion or not displayable → snap. Activation with no previous tab (initial auto-activate) → snap, no event. Scroll offset shifts merely translate the painted rect — a scroll during a slide composes.

**Scroll-to-tab tween:** 300ms (`MorphAnimator.MEDIUM2_MS`) + `Easing.STANDARD` on the scroll offset; wheel input retargets/cancels the tween.

**Color is not tweened** — active/inactive content colors switch with the activation (web behavior: CSS state colors snap; only the indicator slides).

## §7. Behaviors, keyboard & scrolling

- **Click** an inactive tab → activate: stamp actives, fire bar `ChangeListener` + that tab's `ActionListener`, animate indicator, scroll-to-tab. Click the active tab → no-op (research §I).
- **Programmatic** `setActiveTabIndex`/`setActiveTab` → activate: fire `ChangeListener` (any-change semantics), animate, scroll; **no** `ActionListener` (user-gesture only — the Switch split).
- **Mandatory selection:** first `addTab` auto-activates silently (no events — material-web parity); removing the active tab re-activates the first (fires `ChangeListener` — the selection did change).
- **Keyboard (bar-level bindings):** Left/Right move the roving focus with wrap-around, RTL-aware; Home/End jump; Enter/Space activate the focused tab; `autoActivate` makes focus moves activate. Focus arriving at the bar lands on the active tab; focus leaving restores the roving tab stop to the active tab. Only one tab is focus-traversable at a time (roving `setFocusable` bookkeeping — exact mechanism mirrors `ElwhaButtonGroup`, verified in S6).
- **SCROLLABLE:** wheel (and trackpad shift-wheel/horizontal wheel) scrolls the strip; offset clamps to content; activation/focus auto-scrolls per the 48 margin rule; child add/remove keeps the active tab visible (research §I). No scrollbars, no edge fades (M3 shows none).
- **RTL:** tab order, arrow directionality, indicator geometry, and scroll math all mirror via `ComponentOrientation`.

## §8. Accessibility

- **Bar:** `AccessibleElwhaTabs extends AccessibleJComponent implements AccessibleSelection` — role `PAGE_TAB_LIST`; selection size 1; `getAccessibleSelection(i)` = the active tab; `addAccessibleSelection(i)` activates (the `JTabbedPane` shape).
- **Tab:** `AccessibleElwhaTab extends AccessibleJComponent implements AccessibleAction` — role `PAGE_TAB`; `AccessibleState.SELECTED` while active (+ state-change `PropertyChangeEvent` on flip); one "click" action performing user-gesture activation.
- **Names:** label text is the accessible name; `setAccessibleLabel(String)` overrides — **required for icon-only tabs** (research §A); Javadoc says so on `iconOnly(...)`.
- Badge a11y composes via `ElwhaBadgeAnchor`'s push-model name splicing (existing behavior).

## §9. Showcase pattern

`TabsShowcasePanels` (showcase package) + registration in `ElwhaShowcase` under **Containers** as **"Tabs"** (it's a container-of-children, like Button Group), the `LeafEntry` + Workbench/Gallery shape the switch uses:

- **Workbench:** stage hosts one live `ElwhaTabs` over a demo content panel (the `CardLayout` recipe, dogfooding the consumer story); controls — Variant, Tab mode, Icons (none/stacked/inline/icon-only), Badge on tab 2, Auto-activate, Tab count, RTL, Enabled; code panel re-renders the construction snippet.
- **Gallery:** static matrix — rows: Primary / Primary stacked / Primary inline / Secondary / Secondary+icons / icon-only / badged; columns: rest, hover, focus, pressed (via `setHovered`/`setPressed` hooks), + a scrollable-mode row.
- `ElwhaTabsShowcaseSmoke` headless guard: panels construct, gallery renders to a `BufferedImage` without throwing, workbench controls apply.

## §10. Out of scope (documented, not cut)

- **Content-panel management** — M3 ships the bar; panel switching is consumer composition (web: consumer `aria-controls` + hidden panels; Android: ViewPager mediators). Javadoc carries the `CardLayout` recipe; the Workbench demos it live.
- **Per-tab disabled** — no M3 tokens, no material-web API (research §Open-2); bar-level `setEnabled` only. Revisit only on a real consumer need.
- **Alternate container colors** (MDC `OnSurface` style) — `SURFACE` fixed in V1; no `setContainerColor` escape hatch (the Card V1 lesson).
- **MDC `elastic` indicator mode** — possible future polish atop the same animator (research §Open-3).
- **Swipeable/pager content sync** — platform pattern, N/A.

## §11. Phasing → stories (Phase 1 = V1, single phase)

- **S1 — architecture spike + static chrome** (#426) — `ElwhaTabs` + `ElwhaTab` skeletons, §2 paint pipeline, both variants' static chrome label-only (active/inactive colors, divider, at-rest indicator both shapes), FIXED layout, 48px bar, selection API + silent auto-activate-first + `ChangeListener`, light+dark token-correct. *Locks §2.* Demo: `ElwhaTabsChromeDemo`; guard: `ElwhaTabsChromeSmoke`.
- **S2 — interaction & state layers** (#427) — click activation (click-active no-op), hover/focus/pressed layers incl. the primary inactive-pressed→`PRIMARY` quirk, press ripple, per-tab `ActionListener` user-gesture semantics, `setHovered`/`setPressed` gallery hooks, bar `setEnabled` treatment. §6-static/§7. Demo: `ElwhaTabsInteractionDemo`; guard: `ElwhaTabsInteractionSmoke`.
- **S3 — indicator slide motion** (#428) — the 250ms `EMPHASIZED` x+width slide, snap on reduced-motion/not-displayable/initial activation, slide correctness across variant indicator widths. §6. Demo: `ElwhaTabsMotionDemo`; guard: `ElwhaTabsMotionSmoke`.
- **S4 — icons & badges** (#429) — stacked (64 bar, gap 2) / `setInlineIcon` / secondary inline (48) / icon-only; icon state colors; bottom-alignment of inline tabs in a 64 bar; primary content-hugging indicator span with icons; `setBadge` + `IconBearing` anchor (icon-corner + trailing-edge label-only). §3/§5. Demo: `ElwhaTabsIconsBadgesDemo`; guard: `ElwhaTabsIconsSmoke`.
- **S5 — scrollable mode** (#430) — `TabMode.SCROLLABLE`: preferred-width tabs clamped [72,264] + ellipsis, hand-rolled offset + clip, wheel scrolling, 300ms scroll-to-tab with the 48 margin, active-visible on child mutations, indicator/scroll composition. §5/§7. Demo: `ElwhaTabsScrollableDemo`; guard: `ElwhaTabsScrollableSmoke`.
- **S6 — keyboard, a11y & RTL** (#431) — roving focus + wrap + RTL arrows, Home/End, Enter/Space, `autoActivate`, focus-restore-to-active, focus ring (+1 above indicator), `PAGE_TAB_LIST`/`PAGE_TAB`/`AccessibleSelection`/`SELECTED`/action, `setAccessibleLabel`, full RTL mirror (layout/indicator/scroll). §7/§8. Demo: `ElwhaTabsKeyboardA11yDemo`; guard: `ElwhaTabsA11ySmoke`.
- **S7 — Showcase + CHANGELOG** (#433) — §9 panels + registration + `ElwhaTabsShowcaseSmoke`; CHANGELOG `[Unreleased]` entry. *Completes V1; closes the epic.*

## §12. Open for the S1 spike

1. The indicator/divider paint hook: override `paint(g)` calling `super.paint` then overlay, vs `paintChildren` override — pick whichever keeps tab repaints (ripples) from erasing the indicator; remember `isPaintingOrigin()` if any child-transform tricks appear ([[ref_painting_origin_for_transformed_children]] — not expected here).
2. Whether the primary content-hugging indicator measures the *content cluster* (icon∪label) or the label alone for stacked tabs — material-web spans the `.content` box (cluster); verify it looks right at 16px padding with short labels.
3. FIXED-mode rounding: distributing `width % count` leftover pixels across tabs so the last tab doesn't visibly differ.
