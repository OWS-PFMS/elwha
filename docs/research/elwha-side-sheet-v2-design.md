# ElwhaSideSheet V2 — Design Decisions (detached posture + drag gestures)

**Status:** LOCKED (Phase 0) — the build contract for epic [#458](https://github.com/OWS-PFMS/elwha/issues/458), `v0.5.0`. The V2 follow-on to [#308](https://github.com/OWS-PFMS/elwha/issues/308) (ElwhaSideSheet V1). M3 token capture for the detached posture already lives in [`elwha-side-sheet-research.md`](elwha-side-sheet-research.md) §A (variants table) + §B (`detached.container.shape`, `m3_side_sheet_margin_detached`); this doc records the V2 decisions and the **desktop pointer-affordance mapping** of M3's touch-first drag gestures — the part #458 flagged as needing a real design pass.

**Drafted:** 2026-06-17. **Author:** Charles Bryan (`cfb3@uw.edu`).
**Parent epic:** [#458](https://github.com/OWS-PFMS/elwha/issues/458). **Builds on:** V1 (`com.owspfm.elwha.sidesheet`) — `ElwhaSideSheet`, `SideSheetOverlay`, `SheetType`, `SheetEdge`, `SheetDismissCause`, and the shared `AbstractElwhaOverlay` host.

---

## TL;DR

1. **New axis: `SheetPosture { DOCKED, DETACHED }`** — orthogonal to `SheetType`, **default `DOCKED`** so every V1 sheet is byte-for-byte unchanged. `DETACHED` is M3's floating posture: a 16dp margin on all four sides and `ShapeScale.LG` (16px) rounding on **all four** corners (vs the docked sheet's flush edge + content-facing-only corners). Works for both standard and modal. (§1, §2)
2. **The detached margin lives in the sheet, not the host.** A `DETACHED` sheet carries a 16px `EmptyBorder`; the existing inset-aware `getPreferredSize` / `doLayout` / `paintComponent` already inset by `getInsets()`, so the body floats and the corners round with almost no new geometry. The modal overlay just widens its docked band to the sheet's **footprint** (`sheetWidth + 2·margin`) and lets the sheet's own border produce the float — no presentation-state branch inside the sheet's paint. (§2, §3)
3. **Drag-to-dismiss = scrub the existing motion.** Dragging the header toward the anchored edge maps the drag fraction onto the modal host's entrance progress via `entrance.snapTo(1 − fraction)`; release past a 50% threshold continues to 0 with `beginClose()` (one continuous slide, no snap-jump), release under threshold settles back to 1 with `entrance.start()`. The scrim dims with the same `motionProgress` it already tracks. The standard path has no scrim: it translates the body by the drag offset and either `close()`es (past threshold) or animates the offset back. (§4)
4. **Drag-to-resize = a content-facing edge strip.** An 8px hot zone on the sheet's inner (content-facing) edge shows an E/W resize cursor; a press-drag changes `sheetWidth` live, clamped to `[minSheetWidth, maxSheetWidth]`. Standard reflows its host; a shown modal re-docks via the existing `relayoutHost()`. (§5)
5. **Both gestures are opt-in (default `false`).** MDC defaults touch `behavior_draggable` on, but on desktop an unannounced "drag the header to dismiss" surprises upgrading V1 consumers and can fight text selection. V2 ships them **off**, surfaced through `setDragToDismissEnabled` / `setResizable`; the Showcase demos them on. Surfaced decision, not a silent cut. (§4, §6)
6. **One new dismiss cause: `SheetDismissCause.DRAG`** — the modal sheet was flung/dragged past the dismiss threshold. The standard path reuses `close()` (no cause vocabulary). (§4)
7. **Zero new theme tokens.** `SpaceScale.LG` (16) for the margin, `ShapeScale.LG` (16) for the all-corner rounding, the existing `MorphAnimator` motion, the existing scrim — all already in the lib. (§7)
8. **Stories S1–S5, single phase, single stacked PR** — posture+chrome (standard) · detached modal · drag-to-dismiss · drag-to-resize · Showcase. Fresh demo + headless guard per story. (§8)

## §0. Posture vs. type — the four-cell matrix

`SheetType` (standard / modal) and `SheetPosture` (docked / detached) are independent. V1 shipped the left column; V2 fills the right:

| | `DOCKED` (V1) | `DETACHED` (V2) |
|---|---|---|
| `STANDARD` | flush-to-edge, square outer corners, edge divider | 16dp float, all-4 corners 16px, no edge divider |
| `MODAL` | flush-to-edge over scrim, content-facing corners 16px | 16dp float over scrim, all-4 corners 16px |

Drag-to-dismiss and drag-to-resize are behaviors layered on **any** cell (most natural on modal-dismiss and standard-resize respectively), gated by their own flags.

## §1. Scope decisions

**In (V2, this epic):**
- `SheetPosture` axis + detached chrome (margin + all-corner rounding) for standard and modal.
- Detached modal presentation (the overlay floats the sheet off all four edges).
- Drag-to-dismiss (header drag → threshold dismiss/settle), modal + standard, opt-in.
- Drag-to-resize (content-facing edge → live `sheetWidth`), opt-in, min/max bounds.
- Showcase leaf + Gallery coverage of the new axes.

**Out (deliberately, no silent cut):**
- **Drag-to-*move*** a detached sheet around the window (M3 detached sheets do not float free; they stay edge-anchored — only the margin detaches them). Not a thing.
- **Velocity/fling physics** beyond a simple distance threshold — a position threshold (50% of the visible width) is the desktop-legible rule; momentum flinging is a touch refinement deferred unless smoke asks for it. Documented.
- **Predictive-back animation** — already mapped to Esc in V1; no new surface.
- **Bottom sheets / coplanar fork** — out of the whole component (V1 §1).

## §2. Detached chrome [LOCKED]

- **Margin:** `SpaceScale.LG` (16px) on all four sides, the M3 `m3_side_sheet_margin_detached` value. Implemented as an `EmptyBorder(16,16,16,16)` applied in `refreshChrome()` when `posture == DETACHED`, cleared (zero border) when `DOCKED`. The V1 paint/layout/preferred-size code already reads `getInsets()` for the body rect, so the margin falls out — the body, the painted surface, and the dividers all inset together.
- **Corners:** `cornerRadii()` returns `CornerRadii.uniform(ShapeScale.LG.px())` (all four 16px) when `DETACHED`, regardless of type or edge. The docked asymmetry (content-facing only) is a docked-modal concern; a floating card rounds uniformly.
- **Edge divider:** suppressed when detached — a floating card with rounded corners reads as a panel without the 1px boundary line (the divider exists in V1 only because a square, flush, elevation-0 surface needs it). `paintComponent` skips the edge divider when `posture == DETACHED`.
- **Container color / footer divider / no shadow:** unchanged from V1 — posture changes the silhouette, not the fill or the flat-over-scrim rule.
- **Collapse-to-zero:** the standard open/close animation interpolates the *footprint* width (`sheetWidth + 2·margin`) to 0; `getPreferredSize` special-cases a fully-collapsed sheet to width 0 (not `2·margin`) so a closed detached sheet leaves no 32px margin sliver.

## §3. Detached modal presentation [LOCKED]

The margin must not be applied twice (once by the sheet's border, once by the host). The lock: **the host never insets for the margin; the sheet's own border does.**

- `SideSheetOverlay.layoutSurface` sizes the slide band to the sheet's **footprint** — `min(sheet.modalFootprintWidth(), paneWidth)` where `modalFootprintWidth() == sheetWidth + 2·(detached ? 16 : 0)` — kept **full pane height** and flush to the resolved edge, exactly as V1. The sheet fills that band; its 16px border then floats the painted body 16px off the band's outer edge (→ 16px off the window), 16px off top and bottom, and 16px off the band's inner edge (the scrim shows through the rest). One conditional in the band-width calc; the slide-translate, snapshot, scrim, and focus plumbing are untouched.
- A docked modal keeps a zero border and a `sheetWidth` band — V1 verbatim.
- The slide still enters from the resolved window edge and carries the whole floating card (margin included) on/off screen.

## §4. Drag-to-dismiss [LOCKED]

**Affordance.** The drag zone is the **header band** (the title row), excluding the back/close icon buttons (they keep their own hand cursor and click semantics). While `dragToDismissEnabled` and the pointer is over the draggable header, the cursor is `Cursor.MOVE_CURSOR`. This maps M3's touch swipe-to-dismiss to the desktop title-bar-drag idiom without a dedicated glyph (M3 side sheets — unlike bottom sheets — have no drag-handle in the anatomy).

**Direction & fraction.** Only motion *toward the anchored edge* dismisses (dragging away is clamped to 0 offset — a docked sheet can't over-extend). The drag fraction = `min(1, offsetTowardEdge / visibleWidth)`.

**Modal — scrub the entrance motion.** The modal host already animates `motionProgress` 1↔0 (docked↔off-edge) and the scrim alpha tracks it. Drag maps straight onto it:
- On drag: `overlay.dragScrub(offset)` → `entrance.snapTo(1 − fraction)`, which fires the existing motion tick (surface slides, scrim dims) with no new paint path.
- On release ≥ threshold (0.5): `overlay.dismissByDrag()` → records `SheetDismissCause.DRAG`, `beginClose()` continues the already-partway slide from the current progress to 0 (the reverse animates from `entrance.progress()`, so there is **no snap back to docked first**).
- On release < threshold: `overlay.settleOpen()` → `entrance.start()` animates progress back to 1; the sheet re-docks.

This required adding two `protected final` scrub hooks to `AbstractElwhaOverlay` (`scrubMotion(float)` / `settleMotion()`) — interactive analogs of the existing programmatic motion. They are not public API (the base's standing caveat).

**Standard — translate + close.** No scrim, no host. The sheet stores a `dragOffsetPx`, shifts the body by it in `doLayout`/`paintComponent`, and on release either `close()`es (≥ threshold; the width then animates to 0 from the dragged position) or animates `dragOffsetPx` back to 0 (< threshold).

**Re-entrancy & reduced motion.** Drag is ignored while a modal is mid-`beginClose()`; reduced motion still works (snap scrub, snap settle/close). The drag listener is removed/no-ops when `dragToDismissEnabled` is false.

## §5. Drag-to-resize [LOCKED]

- **Hot zone:** an 8px-wide strip along the sheet's **content-facing edge** (the inner edge — opposite the anchored window edge), resolved against orientation. Hovering it sets `Cursor.E_RESIZE_CURSOR` / `W_RESIZE_CURSOR` (by resolved side). Active only when `resizable`.
- **Drag:** changes `sheetWidth` live by the pointer delta (growing when dragged toward the content, shrinking toward the edge), clamped to `[minSheetWidth, maxSheetWidth]`. Standard: `revalidate()` reflows the host each tick (the coplanar squash, driven by the pointer). Modal: `relayoutHost()` re-docks the shown sheet at the new width.
- **Bounds:** `minSheetWidth` default **200** (the spec's narrow end), `maxSheetWidth` default **600** (room for desktop detail panes past the 400 guidance; documented, not the spec max). Setters clamp `sheetWidth` into range.
- **Independence from drag-to-dismiss:** different hot zone (inner edge vs header), different cursor, different flag. A sheet may enable either, both, or neither. When both are on, the header drags to dismiss and the inner edge resizes — no overlap.

## §6. API additions [LOCKED]

```java
// com.owspfm.elwha.sidesheet
public enum SheetPosture { DOCKED, DETACHED }     // new

public final class ElwhaSideSheet extends JComponent {     // additions only — V1 surface unchanged
  public void setSheetPosture(SheetPosture) / SheetPosture getSheetPosture()      // default DOCKED

  public void setDragToDismissEnabled(boolean) / boolean isDragToDismissEnabled() // default false
  public void setResizable(boolean) / boolean isResizable()                       // default false
  public void setMinSheetWidth(int) / int getMinSheetWidth()                      // default 200
  public void setMaxSheetWidth(int) / int getMaxSheetWidth()                      // default 600
}

public enum SheetDismissCause { /* …V1… */, DRAG }   // + DRAG
```

`SheetPosture.DOCKED` + both gesture flags `false` ⇒ a V2 sheet behaves identically to a V1 sheet. Convention adherence mirrors V1 §8.2 (effective-value getters, no chrome setters beyond the axes, no `ShadowBearing`, `@author/@version v0.5.0/@since v0.5.0`).

## §7. Token mapping — zero new tokens

| V2 need | Elwha token | Note |
|---|---|---|
| detached margin 16dp | `SpaceScale.LG` (16) | as an `EmptyBorder` |
| detached corners 16dp (all four) | `ShapeScale.LG` (16) | `CornerRadii.uniform` |
| drag scrub / settle motion | `MorphAnimator` (`snapTo`/`start`/`reverse`) | reuse the entrance animator |
| scrim dim during drag | existing `motionProgress` × 0.32 | no change |
| resize cursors | AWT `E_RESIZE_CURSOR` / `W_RESIZE_CURSOR` | platform cursors, no token |
| drag-handle cursor | AWT `MOVE_CURSOR` | no glyph (M3 side sheets have none) |

## §8. Story breakdown (V1 = S1–S5; single phase, single stacked PR)

- **S1 — Detached posture (chrome + standard):** `SheetPosture` enum; `setSheetPosture`; detached chrome (margin border, all-corner rounding, edge-divider suppression, collapse-to-0); standard embedded float + open/close. Demo: docked vs detached standard, both edges, RTL. Guard: chrome/geometry assertions (margin insets, uniform corners).
- **S2 — Detached modal presentation:** overlay band → footprint width; full-height flush so the sheet's border floats it; all-4 corners over the scrim; slide carries the margin. Demo: modal trigger, docked vs detached, both edges. Guard: display-gated footprint/placement.
- **S3 — Drag-to-dismiss:** header drag zone + MOVE cursor; modal scrub (`entrance.snapTo`) + scrim dim + threshold dismiss (`DRAG`)/settle; standard translate + `close()`/settle; `setDragToDismissEnabled`; `SheetDismissCause.DRAG`; `AbstractElwhaOverlay.scrubMotion/settleMotion`. Demo: drag-to-dismiss modal + standard. Guard: drive the package-private drag hooks, assert dismiss vs settle.
- **S4 — Drag-to-resize:** content-facing edge strip + E/W cursor; live `sheetWidth` clamped to `[min,max]`; standard reflow / modal re-dock; `setResizable` / `setMinSheetWidth` / `setMaxSheetWidth`. Demo: resizable standard docked + detached. Guard: resize-delta clamping.
- **S5 — Showcase leaf + Gallery:** posture toggle + drag-to-dismiss + resizable controls on both Workbench facets; detached gallery cells; sidebar blurb refresh; refreshed `SideSheetShowcaseSmoke`.

Each story = a fresh demo class ([[feedback_fresh_demo_per_story]]); a11y/edge coverage rides inside S1–S4. No follow-on epic anticipated — V2 closes the side-sheet roadmap from V1 §16.

## Appendix A — Decision history

- **Posture as a mutable axis, not new factories:** `SheetPosture` is orthogonal to `SheetType`; four factories (`detachedStandardSheet`, …) would be combinatorial noise. A `setSheetPosture` setter on the existing `standardSheet`/`modalSheet` factories matches the `setSheetType`/`setSheetEdge` precedent.
- **Margin in the sheet, not the host:** the alternative (host insets the surface bounds for detached modal, sheet stays edge-to-edge) needs the sheet to know it's being hosted to avoid double-inset — a fragile `isModalShowing()` check inside paint. Putting the margin in the sheet's border makes one code path serve embedded and modal; the host only adjusts its band width.
- **Drag scrubs the entrance animation rather than a parallel drag animator:** the modal slide + scrim-dim are already a 1↔0 progress curve; `snapTo` lets the pointer drive it and `reverse()`/`start()` continue smoothly from the dragged position. A separate drag-offset transform would duplicate the snapshot/translate/scrim logic and risk a snap-jump at release.
- **Opt-in gestures (default off):** MDC defaults `behavior_draggable=true`, but that's a touch default. On desktop, header-drag-to-dismiss is non-obvious and can fight selection; resize changes layout. Off-by-default preserves V1 behavior on upgrade; consumers opt in. Revisit defaults at 1.0 if a consumer wants them on.
- **Position threshold, not velocity fling:** a 50%-of-visible-width release threshold is legible with a mouse; momentum flinging is a touch nicety deferred (§1) unless smoke disagrees.
- **No drag-handle glyph:** M3 side sheets have no drag handle in the anatomy (bottom sheets do). The MOVE cursor on the header is the desktop affordance.

## Appendix B — Token reference

Detached tokens are in the V1 research doc §B (`detached.container.shape` = Corner.Large all corners; `m3_side_sheet_margin_detached` = 16dp) and §A (the variants table marking detached + drag as the V2 cut). No V2-specific token capture was needed — the gesture work is desktop interaction design, not M3 token capture.
