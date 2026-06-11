# ElwhaMenu V2 — Submenus · Phase 0 design

> **Decisions, not a catalog.** Captured spec detail lives in `elwha-menu-v2-research.md` (+ the V1
> `elwha-menu-research.md` §S/§V/§L/§X). This doc locks the architecture and the story breakdown.
>
> **Epic:** [#322](https://github.com/OWS-PFMS/elwha/issues/322) (ElwhaMenu V2 — submenus). **Builds
> on:** [#298](https://github.com/OWS-PFMS/elwha/issues/298) (V1, shipped). **Milestone:** v0.4.0.

## TL;DR — the locks

1. **One new capability:** a menu item can host a **nested `ElwhaMenu`** that opens to the side. V2
   composes entirely on top of the shipped V1 flat menu — V1 is untouched API-wise (additive only).
2. **New primitive:** **`ElwhaSubMenuItem extends ElwhaMenuItem`** — auto trailing `›` caret +
   `setSubMenu(ElwhaMenu)`; opens on hover (400 ms), click, and Right-arrow. (Research §Q′ named this
   the one genuine `ElwhaMenuItem` sibling.)
3. **Host = a parent–child overlay *chain*** [RECOMMENDED; **locked by the S1 spike**]. A submenu is
   its own `ElwhaMenu` overlay on `POPUP_LAYER`, registered as a *child* of its opener in the existing
   `AbstractElwhaOverlay` `OPEN` registry. The parent **stays open** while the child is up; light
   dismissal closes from the topmost level inward; a press **outside the whole chain** closes all.
4. **Placement:** side-anchored `anchorCorner = START_END` (open trailing, top-aligned), flipping to
   the opposite side and shifting vertically on viewport clip — an extension of V1's pure
   `placeAnchored(...)`.
5. **Active-state corner shape-morph** (the M3 "dynamic quality"): the focused container **rounds
   more**, the unfocused sibling **squares off**, animating the swap as focus crosses levels — driven
   by the shared **`ShapeMorphPainter`** (#176), reduced-motion aware via `MorphAnimator`. Start pair
   **focused `ShapeScale.LG` / unfocused `ShapeScale.SM`**, finalized visually in the spike.
6. **Keyboard:** Right/Enter/Space open a submenu and move focus in; Left/Esc close it and move focus
   back to the opener. V1 **already reserved Left/Right** (left them unbound) for exactly this.
7. **A11y:** each level is its own `AccessibleRole.POPUP_MENU`; the submenu trigger exposes an
   **expandable/expanded** state; focus moves into the submenu and restores to the opener on close.
8. **Zero new theme tokens, zero new design tokens.** The only asset add is **one icon SVG** — a
   right-chevron (`chevron_right`, Rounded/400/20 dp) via a new `MaterialIcons.chevronRight()` — an
   icon resource, not a theme token.
9. **Scope = a single phase** (stories S1–S5). Submenus are one cohesive arc, so the final story's PR
   `Closes #322`. No later phases planned.

## §1. Scope

✅ `ElwhaSubMenuItem` (caret + nested `ElwhaMenu`) · open on hover/click/Right · close on
Left/Esc/outside · side placement + flip · parent-child overlay chain · active-state shape-morph ·
arbitrary nesting depth · nested `POPUP_MENU` a11y + focus chain · Showcase submenu sample · the
right-chevron icon asset.

❌ **Out of scope:** everything in V1 #298 (flat menu, `ElwhaMenuItem`, color styles, layout/grouping,
selection modes, base placement-flip, light-dismiss, base keyboard/a11y — all reused as-is) · a
right-click context-menu binding helper (V1 deferral, still deferred) · the baseline square menu
(#323) · exposed-dropdown/filtering · horizontal/Toolbar.

## §2. Host & overlay chain [RECOMMENDED — lock via the S1 spike]

**The load-bearing call.** V1's host (`AbstractElwhaOverlay`) keeps a static `OPEN` deque and lets
**only the topmost overlay** react to focus escapes / outside-presses (the #298 F1/F7/F8 fixes), so
"one menu at a time" falls out of natural light-dismiss. **Submenus are the deliberate exception:** a
submenu opening must *not* dismiss its parent.

**Recommended model — an overlay chain:**
- A submenu is a full `ElwhaMenu` overlay on `POPUP_LAYER`, **anchored to its opener
  `ElwhaSubMenuItem`** (not to a trigger outside the menu).
- It registers in `OPEN` as a **child of its opener's menu**, forming a chain
  (root → submenu → sub-submenu …). The chain is the unit of "one menu open."
- **Dismissal walks the chain from the top:** Esc / Left-arrow / hover-away (after `hoverCloseDelay`)
  closes only the **leaf**; an outside-press *outside every level in the chain* closes the **whole
  chain**; selecting a leaf item closes the whole chain (the V1 `SELECTION` close, now chain-wide).
- **Focus** lives in the topmost open level; the opener item paints the **active** state (§5) while
  its child is up. Focus restores **to the opener item** (not the root trigger) when a leaf closes.
- An outside-press *on a parent level's surface* (e.g. clicking a different item in the root while a
  submenu is open) closes the leaf and re-activates that level — not a full dismiss.

**Why not** mount the submenu inside the parent surface (Material-Web's `has-overflow` DOM approach):
Elwha overlays are separate layered-pane components, so there is no scroll-clip to fight — a sibling
overlay is simpler and reuses all of V1's mount/motion/focus plumbing. **The spike (S1) proves the
chain registry + parent-stays-open + chain-dismiss before the rest builds on it**, mirroring the V1
S1 host spike and the dialog epic's S1.

Extraction note: the chain logic lands in `AbstractElwhaOverlay` (a general parent/child link +
chain-aware dismissal), so the pending side-sheet (#308) and any future nested overlay inherit it —
coordinate the shape with that consumer, same as V1.

## §3. `ElwhaSubMenuItem` primitive

`public final class ElwhaSubMenuItem extends ElwhaMenuItem` (mirrors how V1 made `ElwhaMenuItem` a
dedicated `JComponent`; this is its one sanctioned sibling, §Q′). It **adds**, on top of the inherited
slot anatomy / state painting / a11y:
- a **trailing `›` caret** auto-placed in the trailing-icon slot (consumer-supplied trailing icon is
  disallowed/overridden — the caret is the submenu signifier);
- `setSubMenu(ElwhaMenu submenu)` / `getSubMenu()` — the nested menu it opens;
- open/close wiring: hover-intent timer (`hoverOpenDelay`/`hoverCloseDelay` = 400 ms), click, and the
  Right/Left keyboard routed from the parent menu's roving focus;
- the **active** state (§5) while its submenu is open;
- a11y: `AccessibleState.EXPANDABLE` always, `EXPANDED` while open; the nested menu is its
  `POPUP_MENU` child.

Construction mirrors `ElwhaMenuItem` (label / leading-icon factories) plus the submenu, e.g.
`ElwhaSubMenuItem.of(icon, "Share", shareSubMenu)`. Added to a menu via the existing
`ElwhaMenu.Builder.addItem(...)` (an `ElwhaSubMenuItem` *is an* `ElwhaMenuItem`).

## §4. Tokens, color, shape [LOCKED — zero new theme tokens]

- **Surface / elevation / corners / color:** a submenu is an `ElwhaMenu`, so it inherits V1's
  container token mapping verbatim — `SURFACE_CONTAINER_LOW` (Standard) / `TERTIARY_CONTAINER`
  (Vibrant), Level-2 shadow, `ShapeScale.MD` rest corners, the §K role tables. A submenu **inherits
  its parent's `ColorStyle`** by default (consumer may override per nested menu).
- **Caret icon:** new bundled SVG `chevron_right` (Rounded/400/fill0/20 dp, gstatic), surfaced as
  `MaterialIcons.chevronRight()` — themed via the shared `Label.foreground` color filter like every
  other `MaterialIcons` glyph. **Icon asset, not a theme token.**
- **Active/inactive container shape:** the morph end-states (§5) are `ShapeScale` values
  (LG focused / SM unfocused, to confirm in the spike) fed to `ShapeMorphPainter` — existing
  `ShapeScale`/`CornerRadii`, no new shape token.

## §5. Active state & motion [LOCKED]

The V1-deferred **Active** state (research §L #6 / §V). While a submenu is open:
- the **opener level's container morphs to the unfocused (squared, `SM`) shape** and the **open
  submenu morphs to the focused (rounded, `LG`) shape**; as focus crosses levels the two **animate the
  swap** (the M3 "dynamic quality").
- driven by **`ShapeMorphPainter`** (#176) — the same engine as Button/IconButton/FAB; **no bespoke
  animator**. The container's `paintComponent` already owns its rounded-rect fill + shadow (V1
  `MenuSurface`); V2 makes that corner radius a morph-driven `CornerRadii` instead of a constant.
- **reduced motion:** `MorphAnimator` auto-snaps to the end-state (no animation) — same as FAB/dialog.
- **submenu entrance:** the nested menu uses the same short scale/fade `MorphAnimator` entrance as V1,
  anchored toward the opener item; instant-open still sanctioned on desktop / reduced motion.

## §6. Placement [LOCKED]

Extend V1's pure `AbstractElwhaMenuOverlay.placeAnchored(...)` with a **side-anchor mode**:
- default `anchorCorner = START_END` — the submenu's top-leading corner meets the opener item's
  top-trailing corner (opens to the **trailing** side, top-aligned to the item, with the M3 small
  offset);
- **flip** to the opposite (leading) side when the trailing side would clip the viewport;
- **shift vertically** to stay in the viewport (as V1 already does for the below-anchor case);
- **RTL** mirrors leading/trailing. Pure function → unit-testable without a realized window, like V1.

## §7. Keyboard & focus [LOCKED]

Layered on V1's flat key map (which deliberately left Left/Right unbound):
| Key | On a submenu trigger / within a submenu |
|---|---|
| **Right** | open the submenu (if closed) and move focus to its first item |
| **Left** | close the current submenu and move focus back to its opener item |
| **Enter / Space** | on a submenu trigger: open + focus in (same as Right) |
| **Esc** | close the current (leaf) submenu, focus back to opener; at root, close the menu |
| **Up / Down / Home / End / type-ahead** | scoped to the **currently focused level** |
Type-ahead and roving focus are **per-level** (the active level owns them; parents are suspended) —
the Material-Web `deactivate-items`/`activate-typeahead` coordination, realized via the chain's
topmost-owns-focus rule.

## §8. Accessibility [LOCKED — code rule]

- Each menu level is its own `AccessibleRole.POPUP_MENU`; items stay `MENU_ITEM`.
- The submenu trigger (`ElwhaSubMenuItem`) reports `AccessibleState.EXPANDABLE` always and `EXPANDED`
  while its submenu is open (the M3 `aria-expanded` analog); its `AccessibleContext` exposes the
  nested menu as a child.
- Focus moves **into** the submenu on open and **restores to the opener item** on close (not the root
  trigger). Disabled submenu triggers are focusable-but-inert (V1 rule). Honors reduced motion.

## §9. Showcase pattern

Extend the existing **Menu** leaf (don't add a new leaf): the Workbench gains a trigger whose menu
contains an `ElwhaSubMenuItem` (one or two nested levels — e.g. *Share ›* → *Email / Link / Embed*,
and a second level), exercising hover/click/keyboard open, the side-flip, and the active morph; the
Gallery gains a static rendered submenu-open snapshot (Standard + Vibrant). Dogfood Elwha controls
([[feedback_dogfood_elwha_components]]).

## §10. Phasing → stories (V2 = #322, single phase)

Submenus are one cohesive arc → **one phase**, stories S1–S5; the final story's PR `Closes #322`.

- **S1 — Overlay-chain host spike + minimal `ElwhaSubMenuItem` (§2/§3).** Extend
  `AbstractElwhaOverlay` with the parent-child chain + chain-aware dismissal; a minimal
  `ElwhaSubMenuItem` that opens its nested `ElwhaMenu` to the side on Right/click; prove **parent
  stays open**, outside-the-chain press closes all, Left/Esc unwind one level, focus moves in/out.
  **Locks §2.** Spike demo + headless chain-dismiss guard.
- **S2 — `ElwhaSubMenuItem` full primitive + side placement/flip (§3/§6).** The caret asset
  (`MaterialIcons.chevronRight()`), `setSubMenu`/`getSubMenu`, hover-intent timers (400 ms), the
  `placeAnchored` side-anchor mode + opposite-side flip + vertical shift + RTL. Fresh demo + placement
  unit guard (pure `placeAnchored`).
- **S3 — Active-state shape-morph (§5).** Container corner radius becomes a `ShapeMorphPainter`-driven
  `CornerRadii`; focused-rounds-more / unfocused-squares-off as focus crosses levels; reduced-motion
  snap. Fresh demo (+ headless morph-state guard).
- **S4 — Keyboard + hover-intent completeness + nested a11y (§7/§8).** Full Left/Right/Esc chain nav,
  per-level type-ahead/roving scoping, `POPUP_MENU` nesting, `EXPANDABLE`/`EXPANDED` on the trigger,
  focus-restore-to-opener. Fresh demo + headless a11y/keyboard guard.
- **S5 — Showcase submenu sample + docs (§9).** Extend the Menu leaf (Workbench submenu trigger +
  Gallery snapshot); `menu/README.md` + CHANGELOG + Javadoc; dogfood pass. **Final story → PR
  `Closes #322`.**

## §11. Open for the S1 spike / Phase 0 sign-off

- §2 chain-dismissal semantics (close-leaf vs close-all on a parent-surface press) — the spike pins
  the exact rule.
- §5 exact focused/unfocused corner radii (`LG`/`SM` starting pair) — pin visually in the spike;
  optional M3 *Specs*-page re-cap if exact dp is wanted.
- Nesting-depth stress (3+ levels) is supported by the recursion but only smoke-tested at 2 in the
  Showcase.

## §12. S1 spike outcome (locked)

The host architecture (§2) is implemented and proven by `MenuChainDismissSmoke` (12 checks). The
chain primitive lives in `AbstractElwhaOverlay`, reusable by the side-sheet (#308):

- **Linear chain links** `chainParent`/`chainChild` on `AbstractElwhaOverlay`; a child mounts via
  `showInChain(anchor, parent)` and is the topmost overlay (so only it reacts to focus escapes — the
  parent stays open). One open child per level.
- **Dismissal rule (pinned):** the **chain leaf** owns the outside-press decision
  (`handleChainOutsidePress`) — a press inside the leaf is ignored; inside an ancestor closes only
  the levels above it (that ancestor stays — *clicking a sibling item in the parent collapses just
  the submenu*); outside every level closes the whole chain from the root. An ancestor's teardown
  **cascades down** to its descendants; a leaf closing on its own **unlinks** and notifies the parent
  via `onChainChildClosed()` (which re-arms the parent's roving focus on the opener item).
- **Selection is chain-wide:** `AbstractElwhaMenuOverlay.closeChain(cause)` closes from the root, so
  picking a leaf action item (or a fully-outside press) dismisses every level. For a chainless menu
  it is exactly `close(cause)` — V1 behavior is unchanged (verified: all V1 menu smokes + the
  motion-on `MenuDismissDiag`/`MenuOverDialogDiag` still pass).
- **Esc scoping (pinned):** the menu host binds Escape `WHEN_FOCUSED` on the surface (not
  `WHEN_IN_FOCUSED_WINDOW`), so in a chain only the focused (leaf) level's Esc fires — closing one
  level at a time instead of an ambiguous window-wide binding collapsing the wrong level.
- **`ElwhaMenuItem` is now `sealed permitting ElwhaSubMenuItem`** — the one sanctioned sibling
  (was `final`).
- **Keyboard (minimal in S1):** Right opens the focused item's submenu + focus in; Left closes a
  submenu level back to its opener; Esc closes the leaf. Full per-level type-ahead/roving scoping is
  S4.
