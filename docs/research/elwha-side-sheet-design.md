# ElwhaSideSheet — Design Decisions

**Status:** LOCKED (Phase 0) — the build contract for epic [#308](https://github.com/OWS-PFMS/elwha/issues/308), `v0.5.0`. Captured detail lives in [`elwha-side-sheet-research.md`](elwha-side-sheet-research.md); this doc records the decisions. Mirrors [`elwha-dialog-design.md`](elwha-dialog-design.md).

**Drafted:** 2026-06-11. **Author:** Charles Bryan (`cfb3@uw.edu`).
**Parents:** decision #232 (generic side sheet, not NavigationDrawer) · z-band #221 (`ElwhaLayers.OVERLAY_LAYER` = 190) · host extraction #325 (`AbstractElwhaOverlay`, which names #308 as its second consumer).

---

## TL;DR

1. **One component, two presentation paths.** `ElwhaSideSheet` is a single surface component (`com.owspfm.elwha.sidesheet`). **Standard** = the consumer embeds it in their layout and toggles `open()`/`close()` (content reflows — Swing embedding gives M3's *coplanar* behavior by construction). **Modal** = `showModal(parent)` mounts it through an internal `AbstractElwhaOverlay` host with scrim, slide-in, focus trap. (§2, §3)
2. **Modal z-band = `ElwhaLayers.OVERLAY_LAYER` (190)** — below dialogs (200), below menus (300): a dialog or menu opened *from* a sheet stacks above it. Pre-decided by #221 + menu design doc §12. (§2)
3. **Type-derived chrome, zero new tokens.** `SheetType.STANDARD`: `SURFACE`, square corners, optional 1px `OUTLINE_VARIANT` edge divider. `SheetType.MODAL`: `SURFACE_CONTAINER_LOW`, `ShapeScale.LG` (16px) on the **content-facing corners only**. **Neither type paints a drop shadow** — the spec renders show the modal sheet flat over its scrim; the `container.elevation level1` token is not expressed as a shadow (research §B correction, locked in the 2026-06-11 smoke loop). (§6, §7)
4. **Anatomy slots:** header (optional back affordance · `TITLE_LARGE` headline · optional close affordance), content slot, optional footer (divider + `ElwhaButton` actions). All M3 nouns. (§7, §8)
5. **Width 256px default** (`docked.container.width`), `setSheetWidth(int)`; modal clamps to the window, spec max 400 documented not enforced. (§4)
6. **Edge anchoring:** `SheetEdge.TRAILING` (default) / `LEADING`, resolved against `ComponentOrientation` — drives corner asymmetry, divider edge, slide direction. (§11)
7. **Motion:** slide-translate from the anchored edge + scrim fade, `MorphAnimator` `MEDIUM2_MS` (300ms), `Easing.EMPHASIZED` family (spec's MDC hardcode is 275ms — see §13 adaptation note); standard open/close animates width with the same contract; reduced-motion snaps (free via `MorphAnimator`). (§13, §14)
8. **Dismiss semantics (modal):** Esc + scrim-click toggles (`setDismissibleByEsc`/`setDismissibleByScrim`, default true), close/back affordances, `dismiss()`; `onClose(Consumer<SheetDismissCause>)` with its own cause enum (menu precedent — not the dialog's). Footer actions do **not** auto-dismiss. (§9)
9. **V1 = docked standard + modal** (this epic, single phase, stories S1–S5). **V2 epic = detached variants + drag/swipe gestures** (filed — no silent cut). Coplanar needs no story: it falls out of embedding. (§1, §16)
10. **Showcase = Dialog-pattern leaf**: Workbench with a live embedded standard sheet on a stage + modal trigger buttons; Gallery embeds configured instances statically (the sheet *is* a plain component, so no `renderPreview()` shim is needed). (§15)

## §0. Posture: M3 Expressive, post-May-2025

The side sheet is unchanged by the Expressive pass (no deprecated baseline form, no Expressive variant fork — the canonical `md.comp.sheet.side` token set is current). The M3 deprecation that matters here already drove #232: the *Navigation Drawer* name is retired; the side-panel primitive is not. `ElwhaSideSheet` is that primitive; nav affordances compose on top.

## §1. Scope decisions — Elwha adaptation

**In (V1, this epic):**
- Docked **standard** sheet (embed + reflow) and docked **modal** sheet (overlay + scrim).
- Header (back/headline/close), content slot, footer (divider + actions).
- Leading/trailing edge anchoring, LTR/RTL aware.
- Slide motion + scrim fade + reduced-motion.
- Esc/scrim dismissibility, dismiss-cause reporting, focus trap/restore.
- Showcase leaf (Workbench + Gallery) + `arrow_back` glyph addition to `MaterialIcons`.

**Out (V2 epic, filed):** detached (floating, 16dp margin, fully-rounded) standard + modal variants; drag/swipe-to-dismiss and drag-to-resize gestures (touch-first; desktop V1 dismisses via Esc/scrim/affordances/API).

**Out (by construction):** a separate *coplanar* variant — M3's coplanar sheet "squashes" sibling content, which is exactly what embedding a component in a Swing layout does. Documented, not built.

**Out (not this component):** nav-drawer affordances (destination lists) — compose `ElwhaNavRailDestination`-style content *into* the sheet at consumer level (rail design doc §14); the FAB-menu/sheet hybrid; bottom sheets.

## §2. Presentation architecture [LOCKED]

**One surface component + an internal modal overlay host.**

- `ElwhaSideSheet extends JComponent` — the sheet surface itself: chrome painting (container color, asymmetric corners, edge divider) + header/content/footer assembly. It is a real component, so the **standard** path is plain Swing embedding (`BorderLayout.LINE_END`, a split layout, etc.).
- `SideSheetOverlay` (package-private) `extends AbstractElwhaOverlay` — the **modal** path. Pins: `overlayLayer() = ElwhaLayers.OVERLAY_LAYER` (190); `lightDismiss() = false` (focus-trap posture; scrim-click dismissal is an explicit backdrop listener honoring `isDismissibleByScrim()`, dialog precedent); `createBackdrop()` = the 32% `SCRIM` veil reusing the dialog's scrim treatment; `createSurface()` = the sheet instance; `layoutSurface()` = full-height band hugged to the resolved edge, width = `min(sheetWidth, paneWidth)`; Esc binding honoring `isDismissibleByEsc()`.
- Why not extend `AbstractElwhaDialog`: it's package-private in `dialog/` and contributes only the modal posture + the *dialog's* cause vocabulary — everything load-bearing already lives in `AbstractElwhaOverlay`, whose own Javadoc names #308 as its second consumer. The sheet host re-pins the same posture in ~the same line count without a cross-package dependency.
- Why 190 not 200: a modal sheet is less interruptive than a dialog — M3 lets a dialog (confirmation) open *from* a sheet (filters). 190/200/300 gives sheet < dialog < menu, and `AbstractElwhaOverlay`'s topmost-overlay focus arbitration handles the rest.

## §3. Component model — container family, mutable type

- `SheetType { STANDARD, MODAL }` — **mutable** (`setSheetType`), set by the per-variant factories. Chrome (color/elevation/corners/divider) derives from it. `showModal(...)` on a `STANDARD`-typed sheet **forces** the type to `MODAL` (SelectField "setters force, not throw" precedent); embedding a `MODAL`-typed sheet is harmless (it just wears modal chrome) and documented.
- Container per the conventions doc: chrome root + typed slots; the consumer supplies content (`setContent(JComponent)`) and actions (`ElwhaButton`s). No raw-Swing children in Elwha-owned chrome (header label, affordances, dividers are Elwha-painted/`ElwhaIconButton`).
- Scrolling content is consumer-owned (wrap your content in a `JScrollPane` if it scrolls) — the sheet doesn't second-guess. [DOC]

## §4. Sizing

- `setSheetWidth(int)` — default **256** (`docked.container.width`). Preferred size: `sheetWidth × parent-determined height` (standard, when open); the modal host stretches the sheet full pane height.
- Standard closed = width 0 (the open/close animation interpolates preferred width; the host layout reflows each tick — that *is* the M3 coplanar squash).
- Modal width = `min(sheetWidth, paneWidth)` — graceful on narrow windows (the spec's full-width-on-compact behavior falls out). The spec's 400 modal max is documented in the width setter's Javadoc, **not** clamped (desktop detail panes legitimately run wider; pre-1.0, no invented constraints). [DOC]
- Height: always fills its slot (standard: whatever the consumer's layout gives it; modal: pane height). No max-height surface.

## §5. Header & footer

- **Header row** (always present — the headline is the sheet's accessible name): optional back affordance (leading) · headline (`TITLE_LARGE`, `ON_SURFACE`, single line, ellipsized) · optional close affordance (trailing). Padding: `SpaceScale.XL` (24) horizontal, dropping to `LG` (16) on a side occupied by an icon affordance (the 48px target supplies the rest of the optical gap); `MD` (12) gap between affordance and headline; `LG` (16) below the header row.
- Affordances are `ElwhaIconButton` standard variant, `MaterialIcons.close()` / `MaterialIcons.arrowBack()` (the latter is the one new glyph — gstatic, Rounded/400/fill0/20dp axis, house style). 48px targets out of the box.
- Close affordance: visible by default. Standard: `close()`es the sheet. Modal: dismisses with `CLOSE_AFFORDANCE`.
- Back affordance: hidden by default; `setOnBack(Runnable)` for multi-step flows. With no `onBack` set, it dismisses/closes with `BACK_AFFORDANCE` (Flutter-package behavior, sensible default).
- **Footer** (modal anatomy item 6, available on both types): `setActions(ElwhaButton...)` — rendered bottom-pinned, leading-aligned (confirm first, M3 sheet renders), `MD` (12) gap, `XL` (24) horizontal / `LG` (16) top / `XL` (24) bottom padding. Empty actions ⇒ no footer. `setFooterDividerVisible(boolean)` default **true** (painted only when the footer exists), full sheet width, 1px `OUTLINE_VARIANT`.
- Actions do **not** auto-dismiss — a filter sheet's "Apply" may legitimately keep the sheet open; consumers call `close()`/`dismiss()` in their listener. (Divergence from the dialog, where actions auto-dismiss with a cause — a dialog *is* a question; a sheet is a workspace.) [LOCKED]

## §6. Color & elevation (canonical token set — research §B)

| | STANDARD | MODAL |
|---|---|---|
| Container | `ColorRole.SURFACE` | `ColorRole.SURFACE_CONTAINER_LOW` |
| Elevation | flat — no shadow | flat — no shadow (the `level1` token is not expressed as a drop shadow in the spec renders; research §B correction) |
| Corners | `ShapeScale.NONE` (square) | `ShapeScale.LG` (16px) on the two content-facing corners; square on the window-edge corners |
| Edge divider | 1px `OUTLINE_VARIANT` on the content-facing edge, default **visible** (`setEdgeDividerVisible`) — square-cornered surface-on-surface needs the boundary | none (corners + scrim carry the boundary) |
| Scrim | — | `ColorRole.SCRIM` @ **0.32f** (identical to the dialog scrim — reuse, don't re-derive) |

Headline `ON_SURFACE`; affordance glyphs `ON_SURFACE_VARIANT` (the `ElwhaIconButton` standard-variant default).

## §7. Anatomy

```
┌─────────────────────────────┐ ◄ container (surface role, type-derived corners)
│ [←]  Headline          [✕]  │ ◄ header: back? · TITLE_LARGE · close?
│ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ │
│                             │
│         content slot        │ ◄ consumer-supplied JComponent
│                             │
│ ─────────────────────────── │ ◄ footer divider (when footer present)
│  (Action)  (Action)         │ ◄ ElwhaButton actions
└─────────────────────────────┘
  ▲ standard: 1px edge divider on the content-facing edge; modal: scrim behind
```

## §8. API design [LOCKED]

```java
// com.owspfm.elwha.sidesheet
public class ElwhaSideSheet extends JComponent {
  public enum SheetType { STANDARD, MODAL }
  public enum SheetEdge { LEADING, TRAILING }

  public ElwhaSideSheet(String headline)                 // convenience: STANDARD
  public static ElwhaSideSheet standardSheet(String headline)
  public static ElwhaSideSheet modalSheet(String headline)

  // anatomy
  public void setHeadline(String) / String getHeadline()
  public void setContent(JComponent) / JComponent getContent()
  public void setActions(ElwhaButton...) / List<ElwhaButton> getActions()
  public void setCloseAffordanceVisible(boolean) / isCloseAffordanceVisible()   // default true
  public void setBackAffordanceVisible(boolean) / isBackAffordanceVisible()     // default false
  public void setOnBack(Runnable) / Runnable getOnBack()
  public void setEdgeDividerVisible(boolean) / isEdgeDividerVisible()           // default true; paints on STANDARD only
  public void setFooterDividerVisible(boolean) / isFooterDividerVisible()       // default true; paints when footer exists

  // chrome axes
  public void setSheetType(SheetType) / SheetType getSheetType()
  public void setSheetEdge(SheetEdge) / SheetEdge getSheetEdge()                // default TRAILING
  public void setSheetWidth(int) / int getSheetWidth()                          // default 256

  // standard presentation (embedded)
  public void open() / close() / setOpen(boolean) / boolean isOpen()            // animated; default open

  // modal presentation (overlay)
  public void showModal(Component parent)        // forces MODAL type; mounts the overlay host
  public void dismiss()                          // PROGRAMMATIC; no-op when not shown
  public boolean isModalShowing()
  public void setDismissibleByEsc(boolean) / isDismissibleByEsc()               // default true
  public void setDismissibleByScrim(boolean) / isDismissibleByScrim()           // default true
  public void setOnClose(Consumer<SheetDismissCause>)                           // fired after teardown
}

public enum SheetDismissCause { CLOSE_AFFORDANCE, BACK_AFFORDANCE, SCRIM, ESC, PROGRAMMATIC }
```

### §8.1 Default values
`STANDARD` · `TRAILING` · width 256 · open · close affordance on · back affordance off · dividers on (where applicable) · Esc + scrim dismissible · no actions · no content.

### §8.2 Convention adherence
Effective-value getters only; per-variant static factories + single-arg convenience constructor; no `setBorderRole` (chrome is type-derived); no `ShadowBearing` (both types render flat — research §B correction); no `getMaximumSize = getPreferredSize` override (the #199 trap); `@author/@version v0.4.0/@since v0.4.0` Javadoc throughout; no comments unless a *why* demands it.

## §9. Scrim & dismiss semantics (modal) [LOCKED]

- Scrim click → `dismiss(SCRIM)` iff `isDismissibleByScrim()`; otherwise consumed (still blocks the UI behind).
- Esc → `dismiss(ESC)` iff `isDismissibleByEsc()`.
- Close affordance → `CLOSE_AFFORDANCE`; back affordance (no `onBack`) → `BACK_AFFORDANCE`; `dismiss()` → `PROGRAMMATIC`.
- Re-entry guarded by the base (`isClosing()`); `onClose` fires once, after teardown + focus restore, with the recorded cause.
- Standard sheets have **no** Esc/scrim semantics — they're page furniture, dismissed by whatever UI the consumer wires to `close()`. [DOC]

## §10. Accessibility [LOCKED]

- Modal: focus moves into the sheet on show (`initialFocusTarget()` → first focusable in content, else the close affordance), is trapped while open (base behavior), and restores to the prior owner on close (base behavior).
- `accessibleName()` = headline; `AccessibleRole.PANEL` for the surface (Swing has no side-sheet role; the modal host's trap + name carry the semantics).
- Affordances: 48px targets (free via `ElwhaIconButton`), accessible names "Back" / "Close".
- Standard: normal tab-order membership; no trap.
- Tooltips on the affordances mirror their accessible names. [DOC]

## §11. RTL mirroring & edge anchoring [LOCKED]

- `SheetEdge.TRAILING` default (MDC `layout_gravity=end`). Resolution: TRAILING = right in LTR, left in RTL (via the surface's `ComponentOrientation`, menu/dialog precedent).
- The resolved edge drives: which corners round (modal — content-facing corners only), which edge wears the standard divider, the slide direction (enter from the resolved edge), and the modal host's docking side.
- Standard sheets: the consumer places the component in their layout; the edge property drives *chrome only*. Mismatched placement (TRAILING chrome docked LINE_START) is a consumer bug — documented, not detected. [DOC]

## §12. Guidelines reference [DOC]

Side sheets carry supporting content/tasks for the main view (filters, detail panes, settings, secondary lists) — not primary navigation (rail), not interruptive confirmation (dialog), not transient command lists (menu). Keep headlines short; prefer one sheet at a time per edge. These land as class-level Javadoc, not enforcement.

## §13. Motion contract [LOCKED] + reduced motion

- **Modal enter:** sheet translates from fully-offscreen at the resolved edge to docked, scrim fades 0→0.32, **300ms** (`MorphAnimator.MEDIUM2_MS`), overlay-default emphasized-decelerate. **Exit:** reverse, emphasized family (base `MorphAnimator.reverse()` contract, dialog-identical).
- **Adaptation note:** MDC hardcodes 275ms (between `medium1` 250 and `medium2` 300, referencing neither token). Elwha pins the lib-wide overlay duration `MEDIUM2_MS` = 300 — one motion vocabulary across dialog/menu/sheet beats chasing a hardcode by 25ms. Revisit in smoke if the slide feels slow.
- **Standard open/close:** preferred-width interpolation 0↔`sheetWidth`, same 300ms/emphasized, `revalidate()` per tick so siblings reflow (the coplanar squash).
- **Reduced motion:** `MorphAnimator` snaps to the target — modal appears docked with full scrim; standard snaps open/closed with a single reflow. No per-component handling needed.

## §14. Standard-mode embedding semantics

- The sheet participates in the consumer's layout; `open()/close()` animate `getPreferredSize().width` and call `revalidate()` on each tick. Works under `BorderLayout` (LINE_START/LINE_END), `GridBagLayout`, and box-style layouts — the Workbench stage demos BorderLayout, the canonical M3 arrangement.
- While closed, the component stays in the hierarchy at width 0 (not `setVisible(false)` — keeps the animation symmetric and the consumer's layout stable).
- Content is clipped during the animation (no relayout-thrash inside the sheet: children keep their open-width layout and the surface clips — matches how M3 renders the squash).

## §15. Showcase integration — Dialog-pattern leaf [LOCKED]

- **Workbench tab:** a stage panel hosting fake main content + a live embedded **standard** sheet (BorderLayout.LINE_END), with controls: open/close toggle, edge, width, type preview, back/close affordance toggles, action count, divider toggles — every control re-applies live (dogfooding `ElwhaButton`/`ElwhaCheckbox`/`ElwhaSelectField`-style controls per the showcase's established control idiom). Plus **"Open modal side sheet"** trigger buttons (trailing + leading) opening the real overlay on the Showcase frame — the live smoke path.
- **Gallery tab:** static embedded instances — standard-with-divider, modal chrome (no scrim; the surface embeds directly, no `renderPreview()` shim needed), leading-edge, footer-actions, back-affordance configurations.
- Sidebar `LeafEntry` under Components: "Side sheet — M3 side sheet: standard (docked, reflowing) + modal (scrim overlay) supplementary surface."

## §16. Story breakdown (V1 = one phase, S1–S5; single PR, tabs/checkbox cadence)

- **S1 — Surface primitive:** `ElwhaSideSheet` chrome + anatomy (header/content/footer slots, type-derived color/corner/elevation/dividers), `MaterialIcons.arrowBack()` glyph, conventions + Javadoc. Demo: static sheet configurations.
- **S2 — Standard presentation:** open/close width animation + reflow, edge divider behavior, embedding contract (§14). Demo: BorderLayout stage with toggle.
- **S3 — Modal presentation:** `SideSheetOverlay` host (scrim, slide+fade motion, Esc/scrim toggles, `SheetDismissCause`, focus trap/restore/initial-focus, `OVERLAY_LAYER`). Demo: trigger-button frame exercising every dismiss path.
- **S4 — Edge anchoring + RTL:** `SheetEdge` resolution across chrome/slide/divider/corners, LTR+RTL. Demo: edge × orientation matrix.
- **S5 — Showcase leaf:** Workbench + Gallery + sidebar entry (§15).

Each story = fresh demo class ([[feedback_fresh_demo_per_story]]); a11y acceptance lives inside S1–S4 (no filler story). V2 epic (detached + gestures) filed at the same time as these stories.

## Appendix A — Decision history

- **Single class vs. surface+overlay pair exposed separately:** rejected a public two-class split (`ElwhaSideSheet` + `ElwhaModalSideSheet`) — the M3 spec treats standard/modal as *types of one component*, and the surface anatomy is identical; only the presentation differs. The overlay host stays package-private exactly like the menu's.
- **Builder API (dialog-style) rejected:** the dialog is fire-and-forget (build → show → gone); a sheet is long-lived page furniture that consumers reconfigure — a mutable component matches, and the standard path *requires* a real embeddable component anyway.
- **Reusing `DismissCause` (dialog) rejected:** wrong vocabulary (`CONFIRM/CANCEL/ALTERNATE` are dialog-action causes; sheets need `CLOSE_AFFORDANCE/BACK_AFFORDANCE`) and a needless cross-package coupling. Menu precedent (own `MenuDismissCause`) followed.
- **Hard 400px modal clamp rejected:** spec range documented instead — desktop detail panes legitimately exceed it; pre-1.0 no invented constraints; clamping to the window is the only structural limit.
- **`setVisible(false)` for closed standard sheets rejected:** width-0 keeps animation symmetric and consumer layout code branch-free.
- **275ms motion rejected** in favor of the lib-wide 300ms (`MEDIUM2_MS`) — see §13.
- **Painted level-1 modal shadow removed (2026-06-11 smoke loop):** the initial build expressed the `container.elevation level1` token as a `ShadowPainter` shadow; the operator's spec-render screenshots show the modal sheet flat over its scrim — no shadow in any render. The shadow, the reserve machinery, and `ShadowBearing` were removed; separation is scrim + corners, per spec.
- **Auto-dismiss on footer actions rejected** — see §5.

## Appendix B — Token reference

See research doc §B (canonical `md.comp.sheet.side` verbatim) and §F (the zero-new-tokens Elwha mapping).
