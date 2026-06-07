# ElwhaMenu V2 — Submenus · M3 spec capture (research)

> **Status: RAW CAPTURE → synthesis.** Backing research for epic
> [#322](https://github.com/OWS-PFMS/elwha/issues/322) (ElwhaMenu V2 — submenus + nested menu items),
> the follow-on to the now-shipped **V1** epic [#298](https://github.com/OWS-PFMS/elwha/issues/298).
> Phase 0 design lives in `elwha-menu-v2-design.md`.

**The submenu spec was already captured during the #298 V1 pass** — the operator screen-capped the
M3 *Submenus*, *Focus*, *States*, and *Accessibility* pages on 2026-06-04, and they were transcribed
into `elwha-menu-research.md` §S / §V / §L / §X. This doc **consolidates** that capture into a
submenu-focused synthesis, adds the Material-Web `md-sub-menu` API shape, maps every need onto
existing Elwha primitives + tokens (goal: **zero new theme tokens, zero new design tokens**), and
lists the open architecture questions for Phase 0. It does **not** re-transcribe the V1 token tables —
it references them.

## Sources

- **Already-captured M3 spec** (operator screenshots, 2026-06-04) → `elwha-menu-research.md`:
  §S Submenus · §V Focus & shape morphing · §L States (#6 Active) · §X A11y (keyboard) · §I tokens
  (shape) · §J anatomy (trailing caret) · §P terminology.
- **Material Web** (text API): `https://raw.githubusercontent.com/material-components/material-web/main/docs/components/menu.md`
  — the `md-sub-menu` element.
- **Reuse targets in-tree:** `com.owspfm.elwha.overlay.AbstractElwhaOverlay` (the shared overlay host
  extracted in V1 #325, with the static topmost-overlay `OPEN` registry), `menu/ElwhaMenu` +
  `menu/ElwhaMenuItem`, `menu/AbstractElwhaMenuOverlay` (anchored light-dismiss + `placeAnchored`),
  `theme/ShapeMorphPainter` (#176), `theme/MorphAnimator`, `theme/CornerRadii` / `ShapeScale`.

---

## §A. What V2 adds (scope)

V1 shipped the **flat** vertical menu. V2 adds **one new capability: a menu item can host a nested
menu** that opens to the side. Concretely:

1. **`ElwhaSubMenuItem`** — a menu item with a trailing submenu caret (`›`) that owns a nested
   `ElwhaMenu` and opens/closes it on hover, click, and Right/Left keyboard.
2. **Side placement** — the nested menu opens *beside* the parent item without overlapping it
   (`anchorCorner=START_END`), flipping to the opposite side and shifting vertically on viewport clip.
3. **Active-state corner shape-morph** — the focused menu/submenu container **rounds more**, the
   unfocused sibling **squares off**, animating the swap as focus crosses levels (research §V).
4. **Hover open/close timing** (~400 ms) + **Left/Right** keyboard open/close (the V1 key map already
   reserves Left/Right for exactly this).
5. **Nested a11y** — each menu level is its own `POPUP_MENU`; the submenu-trigger item exposes an
   expanded/collapsed state; focus moves into the submenu and back.

**Everything in V1 is out of scope** (flat menu, `ElwhaMenuItem`, color styles, layout/grouping,
selection modes, placement-flip, light-dismiss, keyboard/a11y) — V2 composes on top of it.

---

## §B. `md-sub-menu` API (Material Web, text source)

| Property | Type | Default | Elwha bearing |
|---|---|---|---|
| `anchorCorner` | `Corner` | **`START_END`** | open to the trailing side, top-aligned to the item |
| `menuCorner` | `Corner` | **`START_START`** | the submenu's own top-leading corner anchors there |
| `hoverOpenDelay` | number (ms) | **400** | debounce before a hovered trigger opens its submenu |
| `hoverCloseDelay` | number (ms) | **400** | debounce before an un-hovered submenu closes |
| `isSubMenu` | boolean | true (read-only) | — |

**Structure:** `<md-sub-menu>` wraps a triggering menu item (slot `item`) + a nested `<md-menu>`
(slot `menu`). **The root menu sets `has-overflow`** to *disable overflow scrolling so nested
submenus display* — in Material Web a submenu is a child DOM node that would otherwise be clipped by
the parent's scroll container.

> **Elwha divergence (important):** in Elwha a submenu is a **separate layered-pane overlay**, not a
> child of the parent's scroll viewport — so the `has-overflow` clipping problem **does not exist**.
> The Elwha equivalent is simply: a submenu is its own `ElwhaMenu` overlay anchored to the parent
> item, mounted on the same `POPUP_LAYER`. No scroll-clip coupling.

**Coordination events** (`deactivate-items`, `request-activation`, `deactivate-typeahead`,
`activate-typeahead`) are Material-Web's way of telling the parent menu to yield active-item state to
the open submenu. **Elwha equivalent:** the parent menu suspends its own roving focus / type-ahead
while a submenu is open and owns focus (the V1 host's topmost-overlay-only focus rule, extended to a
parent-child *chain*).

**Affordance:** a trailing **arrow/caret** icon signals "has submenu" (right caret to open).

---

## §C. The already-captured behavior (consolidated from V1 research)

Verbatim pointers — do not re-litigate, these were transcribed from the M3 pages in #298:

- **§S Submenus** — *"Submenus should open next to the parent menu item without overlapping it"*
  `[CODE]`; *"Position submenu to the side of the parent item"* `[CODE]`; large-screen-only `[DOC]`
  (Elwha is desktop Swing → always satisfied, no mobile fallback).
- **§V Focus & shape morphing** — *"As a person moves from one submenu to the next, the corners of
  the focused submenu become more rounded, while the unfocused submenu becomes less rounded"* `[CODE]`.
  *"Focus follows the current hovered or focused submenu"* `[DOC]`. → a **container corner-radius
  morph between two shape states**, driven by the shared `ShapeMorphPainter` (#176) — **do not roll a
  bespoke animator**; honors reduced motion via `MorphAnimator`.
- **§L #6 Active state** — the parent item is marked active; the submenu opens to the side; the
  focused container rounds more. This is the **only** V1 state deferred to V2 (V1 ships
  Enabled/Disabled/Hovered/Focused/Pressed; Active needs a sibling to morph against).
- **§X Keyboard** — *Left/Right = open/close a submenu* `[CODE]`; Space/Enter on a submenu trigger
  opens it. The V1 flat menu wired Up/Down/Home/End/Enter/Space/Esc/type-ahead and **deliberately
  left Left/Right unbound, reserved for V2** (V1 design §10).
- **§J Anatomy** — the trailing **submenu caret `›`** is the existing trailing-icon slot (anatomy #4);
  no new anatomy part.

---

## §D. Token mapping — zero new tokens

| V2 need | Existing Elwha vehicle | New token? |
|---|---|---|
| Submenu container surface, elevation, corners | same as V1 `ElwhaMenu` (`SURFACE_CONTAINER_LOW`/Vibrant `TERTIARY_CONTAINER`, Level-2 shadow, `ShapeScale.MD`) | **no** |
| Submenu caret `›` | `MaterialIcons` chevron glyph in the trailing-icon slot (20 dp) | **no** (bundled glyph; verify `expandMore`/a right-chevron exists, else add an SVG asset, not a theme token) |
| Active = focused-rounds-more / unfocused-squares-off | `ShapeMorphPainter` (#176) interpolating `CornerRadii` between two `ShapeScale` rest states | **no** |
| Morph timing / reduced motion | `MorphAnimator` (`MEDIUM2_MS`, reduced-motion auto-snap) | **no** |
| Hover open/close debounce (400 ms) | a `javax.swing.Timer` (behavioral constant, not a theme token) | **no** |
| Side placement `START_END` + flip | extend `AbstractElwhaMenuOverlay.placeAnchored(...)` (V1's pure placement fn) with a side-anchor mode | **no** |
| Nested overlay mount | same `POPUP_LAYER` (300) as V1; parent-child chain in the `OPEN` registry | **no** |

→ **Goal holds: zero new theme tokens, zero new design tokens.** The one possible asset add is a
right-chevron **icon SVG** if the bundle lacks one (an icon resource, not a token) — confirm against
`MaterialIcons` in Phase 0.

---

## §E. Open questions for Phase 0 (carry to the design doc)

1. **[LOAD-BEARING] Nested-overlay architecture** — how do a parent menu and its open submenu coexist
   as overlays so the parent does *not* light-dismiss when the child opens, focus moves into the
   child, and an outside-press closes the whole chain? (Recommend: a parent-child **overlay chain**
   extending the V1 `OPEN` registry; lock via the S1 spike.)
2. **`ElwhaSubMenuItem` API** — dedicated `ElwhaSubMenuItem extends ElwhaMenuItem` (auto caret +
   `setSubMenu`/nested `ElwhaMenu`) vs. a `setSubMenu(...)` setter on the base `ElwhaMenuItem`.
   (Recommend: dedicated subclass — §Q′ named it the one genuine sibling.)
3. **Active morph exact radii** — §I shape tokens were swatch-only (no dp). Focused ≈ `ShapeScale.LG`,
   unfocused ≈ `ShapeScale.SM`? (Recommend a starting pair; lock the visual in the spike. Optional:
   re-cap the M3 *Specs* page for exact radii.)
4. **Nesting depth** — arbitrary depth (M3 supports it) vs. single-level. (Recommend: arbitrary depth
   — the overlay-chain recursion is natural once one level works; Showcase demos two levels.)
5. **Hover-intent vs. click** — open on hover (400 ms) AND on click/Right-arrow? (Recommend: all
   three, with the 400 ms hover debounce; click/keyboard are instant.)

---

## §F. Screenshot log

- The submenu spec pages were captured **2026-06-04 during the #298 pass** — see
  `elwha-menu-research.md` §F entries: *Submenus*, *Focus & shape morphing*, *States*, *Keyboard nav
  (Left/Right submenu)*, *Anatomy (trailing caret)*. **No new screenshots needed for V2** unless the
  operator wants the M3 *Specs* page to pin exact active/inactive corner radii (§E Q3).
- *(append if any re-cap is pasted)*
