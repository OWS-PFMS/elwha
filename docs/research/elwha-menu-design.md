# ElwhaMenu — Design Decisions

**Status:** **Phase 0 — design draft for review.** Decisions below are proposed; the load-bearing host call (§2) is locked by the first implementation story's spike. No stories filed until this doc is approved.

**Drafted:** 2026-06-04. **Author:** Charles Bryan (`cfb3@uw.edu`).

**Parents:**
- [`elwha-menu-research.md`](elwha-menu-research.md) — the full M3 spec capture this doc decides against. **Read it for any anatomy/token/measurement detail; this doc references rather than duplicates it.**
- [`elwha-design-direction.md`](elwha-design-direction.md) — Expressive-first doctrine.
- [`elwha-dialog-design.md`](elwha-dialog-design.md) §2 + `AbstractElwhaDialog` (#254/#271) — the overlay-host precedent this epic extends.
- [M3 Menus](https://m3.material.io/components/menus) + Material Web `menu.md` + MDC-Android `Menu.md` — token/anatomy/behavior source.

**Epic:** [#298](https://github.com/OWS-PFMS/elwha/issues/298) (V1). **Follow-on:** [#322](https://github.com/OWS-PFMS/elwha/issues/322) (V2 submenus), [#323](https://github.com/OWS-PFMS/elwha/issues/323) (baseline stub).

**Milestone:** v0.4.0 (the active dev milestone).

---

## TL;DR

1. **What it is:** `ElwhaMenu` — the M3 **Expressive vertical menu**. A temporary, light-dismissed surface that tops all content, anchored to a trigger, listing dedicated `ElwhaMenuItem` primitives. The general-purpose popover Elwha lacks; unblocks the Nav-Rail overflow (#238).
2. **Posture:** Expressive vertical menu **only**. Baseline (square) menu is excluded (#323); the "horizontal menu" tokens are a token-table ghost / future Toolbar (research §I).
3. **Host (load-bearing, §2):** render as an **in-window overlay on the host frame's layered pane** at `JLayeredPane.POPUP_LAYER` (300) — above dialogs (`MODAL_LAYER`) and the Elwha overlay band (`ElwhaLayers.OVERLAY_LAYER`, 190), so a menu opened from within a dialog correctly tops it. **Extract a shared lightweight overlay host** from the dialog's `AbstractElwhaDialog` (mount + focus-trap + focus-restore + entrance); the menu uses it in **light-dismiss + anchored** mode (no scrim). Coordinates with side-sheet #308, the other pending consumer. Final extraction boundary locked by the §12 S1 spike.
4. **`ElwhaMenuItem` = dedicated `JComponent` primitive** (container-with-swappable-slot), mirroring `ElwhaNavRailDestination` — *not* `JMenuItem`, *not* a Button. Slots: leading icon · label · supporting text · trailing text/shortcut · trailing icon · badge · swappable content slot (research §J/§Q/§Q′).
5. **Tokens — zero new ones (LOCKED).** Item **44dp** visual / **48dp** touch target, **16/8/12dp** insets, **20dp** icons, **2dp** gap/group-padding, Level-3 shadow. Label `LABEL_LARGE`, supporting `BODY_SMALL`, trailing/shortcut `LABEL_LARGE`. Focus ring `SECONDARY` 3dp inset (−3dp). Container `SURFACE_CONTAINER_LOW`. (research §I/§K)
6. **Two config axes (LOCKED):** `ColorStyle{STANDARD, VIBRANT}` (surface vs tertiary) × `Layout{STANDARD, GROUPED}`; Grouped separators `Separator{GAP, DIVIDER}` (gap = expressive default; divider forced when scrollable — gaps unsupported on scroll). (research §H/§K/§N/§U)
7. **Selection (LOCKED):** selected = `TERTIARY_CONTAINER` fill **+ ✓ checkmark** (3:1 + non-color cue per a11y); Vibrant uses bold `TERTIARY`. `SelectionMode{NONE, SINGLE, MULTI}` — MULTI stays open on select. (research §K/§X/§U)
8. **Placement (LOCKED):** anchor below/leading-aligned (`anchorCorner=END_START`/`menuCorner=START_START`); **flip left/right/above** on window-edge clip. (research §R)
9. **Motion (LOCKED):** short scale/fade entrance via shared `MorphAnimator`; **desktop instant-open is on-spec** and reduced-motion-aligned — low bar for v1. Item-select ripple via `RipplePainter`; trigger shows pressed while open. (research §U/§L)
10. **A11y (LOCKED):** `POPUP_MENU`/`MENU_ITEM` roles, initial focus = first item, focus-trap + restore-to-trigger, disabled items stay focusable, separators non-focusable, leading icon decorative, full key map. (research §X)
11. **V/Phase split:** **V1 (#298)** = the menu incl. Vibrant + SelectionMode across internal phases; **V2 (#322)** = submenus. See §12.
12. **Out of scope:** submenus (V2), baseline menu (#323), exposed-dropdown/filtering, context-menu right-click helper, density levels, horizontal/Toolbar.

---

## §1. Scope — what V1 ships

✅ Flat (non-nested) Expressive vertical menu · dedicated `ElwhaMenuItem` (+ slot) · anchored overlay host with flip · light-dismiss (outside-click / Esc / focusout) · full keyboard + a11y · `ColorStyle` Standard **and** Vibrant · `Layout` Standard **and** Grouped (gap/divider) · `SelectionMode` NONE/SINGLE/MULTI · default token theming + dark mode · Showcase leaf.

❌ Submenus → **V2 #322** · baseline square menu → **#323** · exposed-dropdown/filtering/autocomplete (future assembly) · context-menu right-click binding helper · density levels · horizontal menu (Toolbar).

*(No invented cuts: every exclusion above is either a filed epic or a research-documented deferral — research §0/§S/§U/§N/§W.)*

## §2. Host & z-band — the load-bearing decision [RECOMMENDED; lock via S1 spike]

**Recommendation:** render `ElwhaMenu` as an **in-window overlay** on the host frame's `JLayeredPane` at **`JLayeredPane.POPUP_LAYER` (300)**, consisting of just the **menu surface** (no scrim/backdrop — menus light-dismiss, they don't block input). Modality = none; dismissal = outside-click / Esc / focusout.

**Why POPUP_LAYER (300):** a menu can open from inside a dialog (a select control in a dialog body). Dialogs sit at `MODAL_LAYER` (200); badges/side-sheets at `ElwhaLayers.OVERLAY_LAYER` (190, #221). The menu must top all of them → the standard Swing popup band (300) is purpose-built and already above them. No new layer constant needed.

**Why a shared overlay-host extraction (not subclassing the dialog):** the dialog's `AbstractElwhaDialog` already owns the reusable plumbing — layered-pane mount, focus-trap, focus-restore-to-trigger, `MorphAnimator` entrance, Esc handling. But it assumes a **scrim + centered surface** (modality), which is the opposite of a menu's **light-dismiss + anchored** model. So:
- **Extract** a lightweight `AbstractElwhaOverlay` (working name) base holding mount + focus-trap + focus-restore + entrance, with strategy hooks for *dismiss policy* (modal-scrim vs light) and *placement* (centered vs anchored).
- `AbstractElwhaDialog` becomes a modal-scrim + centered subclass; the new menu host is a light-dismiss + anchored subclass.
- **This is the third consumer** of that plumbing (dialog, full-screen dialog, and side-sheet #308 all want it) — extraction pays for itself; coordinate the boundary with #308.

**S1 spike (first story):** prototype the extraction + an anchored light-dismiss menu on POPUP_LAYER, confirming (a) outside-click/Esc/focusout dismissal, (b) focus-trap + restore, (c) opening from inside a live `ElwhaDialog` tops it, (d) flip near a window edge. Mirrors the dialog epic's `DialogModalityDemo` S1 lock. If extraction proves too invasive, fall back to a standalone menu host that copies the plumbing (documented fallback).

## §3. Anatomy & the `ElwhaMenuItem` primitive [LOCKED]

`ElwhaMenuItem extends JComponent` — a dedicated primitive mirroring `ElwhaNavRailDestination` (bespoke `AccessibleJComponent`, own state painting, 48dp target), **not** `JMenuItem`/Button. Slot model (research §J/§P):

`[leadingIcon?] · label (+ supportingText?) · [badge?] · [trailingText?] · [trailingIcon?]` inside the menu Container, with optional inter-item `Separator{GAP|DIVIDER}` for grouping. `setSlot(JComponent)` swaps the label region for image/progress/swatch content (display-only — no nested interactive controls, research §Q). Disabled items dim but remain (research §O) and stay focusable (research §X).

**Item family:** one `ElwhaMenuItem` + `setSlot`. The only siblings: `ElwhaSubMenuItem` (V2 #322) and a lightweight divider/separator. (research §Q′, §E Q7.)

## §4. Tokens [LOCKED — zero new]
Per research §I/§K — all roles/scales exist today. Item 44dp visual / 48dp target; 16/8/12dp insets; 20dp icons; 2dp gap + group-padding; Level-3 `ShadowPainter`; focus ring `SECONDARY` 3dp / −3dp inset; label `LABEL_LARGE`, supporting `BODY_SMALL`, trailing `LABEL_LARGE`.

## §5. Color styles [LOCKED]
`ColorStyle.STANDARD` (default): container `SURFACE_CONTAINER_LOW`, label `ON_SURFACE`, icons/trailing/supporting `ON_SURFACE_VARIANT`, selected `TERTIARY_CONTAINER`/`ON_TERTIARY_CONTAINER`. `ColorStyle.VIBRANT`: surface tints `TERTIARY_CONTAINER`/`ON_TERTIARY_CONTAINER` throughout, selected bold `TERTIARY`/`ON_TERTIARY`. Light/dark = same roles → dark mode free. (research §K) Vibrant ships in V1 but a **later phase** (§12).

## §6. Layout & grouping [LOCKED]
`Layout.STANDARD` (flat) | `Layout.GROUPED` (partitioned). Grouped separator `Separator.GAP` (expressive default, segmented rounded cards) | `Separator.DIVIDER` (subtle line). **Scrollable menu ⇒ gaps unsupported ⇒ force DIVIDER/none** (research §N/§U). Cap gaps at 1–2 groups (doc guidance). Per-position corner rounding via the §I `first/last child shape` + `inner corner` tokens.

## §7. States & motion [LOCKED]
Six states (research §L): Enabled / Disabled (38% dim) / Hovered (`ON_SURFACE` state layer) / Focused (3dp inset ring) / Pressed (ripple) / Active (submenu — **V2**). Entrance = short scale/fade via shared `MorphAnimator`; **instant-open sanctioned on desktop** and under reduced motion. The Active corner shape-morph (focused rounds more / unfocused squares off) reuses `ShapeMorphPainter` (#176) and lands with **V2** (research §V).

## §8. Placement [LOCKED]
Anchor below the trigger, leading-aligned (`anchorCorner=END_START`/`menuCorner=START_START`); auto-flip to **left/right/above** when the window edge would clip; viewport = frame/layered-pane bounds. (research §R)

## §9. Selection [LOCKED]
`SelectionMode{NONE, SINGLE, MULTI}`. NONE = action menu (#238's case). SINGLE = one selected, auto-deselect prior, close on select. MULTI = many selected, **stays open** until dismissed (`keepOpen`). Selected visual = `TERTIARY_CONTAINER`/Vibrant `TERTIARY` fill **+ ✓ checkmark** (3:1 + non-color cue). Coordinate enums with the cross-cutting selection surface (#252). SINGLE/MULTI ship in a **later V1 phase** (§12). (research §U/§K/§X)

## §10. Accessibility [LOCKED — code rule]
Container `AccessibleRole.POPUP_MENU`, items `AccessibleRole.MENU_ITEM` (bespoke `AccessibleJComponent`). Initial focus = first item; focus-trap while open; restore to trigger on close. Disabled items **focusable-but-inert**; separators **non-focusable**. Accessible name = item text; leading icon **decorative**. Key map (research §X): Tab focuses · Space/Enter opens-then-selects · Up/Down moves (+ Home/End) · Letters type-ahead · Esc closes · Left/Right submenu (V2). Honors reduced motion.

## §11. Showcase pattern
Menus are transient popovers, not embeddable surfaces — so the Showcase leaf mirrors the **dialog** pattern (research §U/§N): a **Workbench** of trigger buttons (`ElwhaIconButton` ⋮, split button) that open live menus configured by surrounding controls (color style, layout, separator, selection mode), plus a **Gallery** of static rendered snapshots (Standard/Vibrant × flat/grouped). Dogfood Elwha controls per [[feedback_dogfood_elwha_components]].

## §12. Phasing → stories (V1 = #298)

**Proposed story breakdown (file on approval):**
- **S1 — Host spike + extraction (§2).** Shared overlay-host extraction; anchored light-dismiss menu on POPUP_LAYER; dismissal/focus/flip/opens-over-dialog proven. *Locks §2.*
- **S2 — `ElwhaMenuItem` primitive (§3).** Dedicated `JComponent`, slot anatomy, token painting, states, a11y role, 48dp target.
- **S3 — `ElwhaMenu` container + Standard color + Standard/Grouped layout + placement/flip (§4/§6/§8).** `open(anchor)`, gap/divider, scroll→divider rule.
- **S4 — Keyboard nav + light-dismiss + focus (§10).** Full flat key map (no Left/Right), type-ahead, restore.
- **S5 — Vibrant color style (§5).**
- **S6 — `SelectionMode` SINGLE/MULTI (§9).**
- **S7 — Showcase leaf (§11).**
- **S8 — Docs:** `menu/README.md`, CHANGELOG, Javadoc, dogfood pass.

**Phase grouping (handoff at phase boundaries per [[feedback_phase_handoff_cadence]]):**
- **Phase 1 = S1–S4 + S7/S8 partial** → ships the flat menu that **unblocks #238** (NONE selection, Standard+Vibrant-deferred, Standard/Grouped).
- **Phase 2 = S5** (Vibrant).
- **Phase 3 = S6** (SelectionMode SINGLE/MULTI).

**V2 (#322)** = submenus (`ElwhaSubMenuItem`, side-placement, active shape-morph, Left/Right nav) — separate epic, after V1.

## §13. Open for the S1 spike / Phase 0 sign-off
- §2 host extraction boundary (shared base vs standalone fallback) — the one genuine unknown; spike resolves it.
- Q4 (research §E) — relationship to FAB Menu #185: does ElwhaMenu become the content host a future FAB Menu composes? **Defer** — note as a forward dependency; doesn't affect V1.

## §14. S1 spike outcome (2026-06-05, #325) — **the extraction lands; host LOCKED**

The §2 `RECOMMENDED` extraction was prototyped and **adopted** — no fallback needed. Shape as built:

- **`com.owspfm.elwha.overlay.AbstractElwhaOverlay`** (new package; `public` to cross the package
  boundary, documented library-internal like `ShadowPainter`/`RipplePainter`). Owns the shared
  lifecycle: layered-pane mount at a subclass-chosen `overlayLayer()`, `MorphAnimator`
  entrance/exit, relayout-on-resize, the focus listener, the re-entry-guarded teardown, and the
  `firstFocusable`/`action` helpers. Two strategy axes via overridable hooks: **dismiss policy**
  (`lightDismiss()` → false=modal-trap / true=light-dismiss; `onFocusEscaped()`, `onOutsidePress()`)
  and **placement** (the abstract `layoutSurface(w, h)`). The cause/`onClose` reporting stays in the
  subclass (`onClosed()` hook), so `DismissCause` never leaked out of `dialog/`.
- **`AbstractElwhaDialog`** is now a thin modal subclass (`extends AbstractElwhaOverlay`): pins
  `MODAL_LAYER` + scrim + focus-trap, keeps its `DismissCause` plumbing. `ElwhaDialog` /
  `ElwhaFullScreenDialog` are **unchanged** (same abstract hooks, same `dismiss(...)` surface) — the
  refactor is non-breaking.
- **`menu/AbstractElwhaMenuOverlay`** (package-private) is the light-dismiss + anchored host
  `ElwhaMenu` (S3) extends: `POPUP_LAYER` (300, tops dialogs/overlays), light dismiss (outside-press
  / focus-loss / Esc), and the anchored placement (`placeAnchored` — leading-aligned below, flips
  above on bottom-clip, shifts on right-clip, RTL trailing-align). Focus restores to the trigger
  only on intentional close (Esc/selection/programmatic), not on focus-loss/outside-press. Public
  `MenuDismissCause` carries the close reason.
- **Proven** by `MenuHostSmoke` (9 headless checks + a windowed mount/POPUP_LAYER proof) and the
  visual `MenuHostSpikeDemo` (below/flip/shift triggers + open-over-a-live-`ElwhaDialog`).
- **Carried to S3:** `placeAnchored` is the placement seam S3 refines (left/right flip is still
  TODO — S1 ships vertical flip + horizontal shift, sufficient for the "minimal flip" acceptance).

## §15. S5 outcome (2026-06-06, #355) — Vibrant ships per §5/§K, no deviation

- **`ColorStyle.VIBRANT`** added (the Phase-1-deferred constant; a source/binary-compatible enum
  add). The container and item color logic became `(colorStyle, selected)`-aware off the **research
  §K** role tables — no role substitutions, no new tokens. Standard is byte-for-byte unchanged.
- **Container** (`ElwhaMenu.containerColor()`): Standard `SURFACE_CONTAINER_LOW` / Vibrant
  `TERTIARY_CONTAINER`, applied to both the slab and the per-group GAP cards.
- **Item** (`ElwhaMenuItem`): Vibrant non-selected content/icons/trailing/supporting/state-layer →
  `ON_TERTIARY_CONTAINER`; selected fill → bold `TERTIARY` with `ON_TERTIARY` content (incl. the ✓
  checkmark, themed live through the existing `iconColor()` color filter). The focus ring stays
  `SECONDARY` in both styles (a focus-visible affordance, outside the §K anatomy table).
- **Proven** by `MenuVibrantSmoke` (10 headless checks — a render-and-count role matrix asserting
  Standard fills `SURFACE_CONTAINER_LOW` and never the bold `TERTIARY`, Vibrant fills
  `TERTIARY_CONTAINER` + the bold `TERTIARY` selection and never `SURFACE_CONTAINER_LOW`, light +
  dark) and the visual `ElwhaMenuVibrantDemo` (Standard vs Vibrant columns, live + preview, one
  selected item). Showcase Menu leaf gains a Color control + a Vibrant gallery section.
- **Phase 3 — S6 `SelectionMode` SINGLE/MULTI** (§6/§U) files when this lands.

## §16. S6 outcome (2026-06-06, #365) — SelectionMode ships per §9, V1 feature-complete

- **`SelectionMode{NONE, SINGLE, MULTI}`** added (the M3 nouns, §P / §9); `builder().selectionMode(...)`
  default `NONE`. No deviation from §9, **zero new tokens** — the selected visual (fill + ✓) already
  existed from Phases 1–2.
- **Behavior** (`ElwhaMenu.onItemActivated`): `NONE` fires + closes (Phase-1 action menu, byte-for-byte
  unchanged); `SINGLE` selects the activated item, deselects all others, and closes with
  `MenuDismissCause.SELECTION` (focus restores to the trigger via the host's existing intentional-close
  path); `MULTI` toggles the activated item's `selected` and stays open. Mouse and keyboard share the
  path — the container's Enter/Space routing already funnels through `ElwhaMenuItem.activate(...)`.
- **Read-back / observe:** `getSelectedItems()` + per-item `isSelected()` + `getSelectionMode()`;
  `builder().onSelectionChange(Consumer<ElwhaMenuItem>)` fires per toggle (never in `NONE`). Initial
  selection set by the consumer via the existing public `ElwhaMenuItem.setSelected(true)`.
- **Layout stability (decision):** in a selection mode the container pushes
  `ElwhaMenuItem.setReserveLeadingColumn(true)` onto **every** item (M3 checkable-menu behavior), so a
  no-icon item reserves the check-column up front and toggling never reflows the row. `NONE` keeps the
  tight Phase-1 layout.
- **A11y:** selected items expose `AccessibleState.SELECTED`; `MULTI` items additionally expose
  `CHECKED` (checkbox-like) via a pushed `setCheckable(true)` — so the ✓ is never the only signal.
- **Proven** by `MenuSelectionSmoke` (14 headless checks — the NONE/SINGLE/MULTI behavior matrix +
  auto-deselect + accumulate/toggle + change-callback counts + the SELECTED/CHECKED a11y split + the
  check-column-reserve / no-reflow width invariants) and the visual `ElwhaMenuSelectionDemo` (three
  live columns, one per mode). Showcase Menu Workbench gains a Selection control.
- **V1 (#298) is feature-complete with this story** — its PR `Closes #298`. Submenus are the separate
  **V2 epic #322**.
