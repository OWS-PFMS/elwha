# ElwhaFullScreenDialog — Design Decisions

**Status:** **Phase 0 (spec capture).** This doc settles the architecture and anatomy for the M3 **Full-screen Dialog** — the second M3 dialog type, deferred out of the Basic Dialog epic ([#254](https://github.com/OWS-PFMS/elwha/issues/254)) and tracked as epic [#271](https://github.com/OWS-PFMS/elwha/issues/271). Phasing + child stories filed after this doc lands. Sections marked **[LOCKED]** are decided; **[PROTOTYPE — S1/S2]** lock at the first implementation story (mirroring how #254 prototyped modality in S1).

**Drafted:** 2026-05-29

**Author:** Charles Bryan (`cfb3@uw.edu`).

**Parents:**
- [`elwha-dialog-design.md`](elwha-dialog-design.md) — the Basic Dialog spec. **This epic reuses its overlay/host machinery** (§2 modality, §10 a11y, §13 motion plumbing) and carries forward its §14 measurement capture. Read it first.
- [`elwha-design-direction.md`](elwha-design-direction.md) — doctrine bar (raw Swing + tokens can't express it).
- [`elwha-fab-design.md`](elwha-fab-design.md) §15 + `ElwhaFabAnchor` (#205) — the layered-pane overlay precedent both dialogs sit on.
- [M3 Dialogs](https://m3.material.io/components/dialogs/guidelines) + [MDC-Android `Dialog.md`](https://github.com/material-components/material-components-android/blob/master/docs/components/Dialog.md) — anatomy / token / dismiss source. (Full-screen-specific dp values were captured in `elwha-dialog-design.md` §14 on 2026-05-29 from the M3 "Full-screen dialog padding and size measurements" spec table; carried into Appendix B here.)

**Epic:** [#271](https://github.com/OWS-PFMS/elwha/issues/271).

**Milestone:** v0.3.0.

---

## TL;DR

1. **What it is:** `ElwhaFullScreenDialog` — the M3 **Full-screen Dialog** primitive for **longer-form input flows** (multi-field forms, "create event"–style tasks) and narrow frames where a centered Basic Dialog would be cramped. A modal overlay that **fills the host frame edge-to-edge** with a **top app bar** (leading close affordance → start-aligned headline → trailing confirm text button) over edge-to-edge content, optionally a bottom action bar. **No scrim** — it fills the frame.
2. **Architecture (LOCKED — sibling, not a variant flag):** a **separate `ElwhaFullScreenDialog` class**, *not* a `Variant.FULL_SCREEN` on `ElwhaDialog`. The anatomy diverges too far (top app bar vs. centered icon/headline; no scrim; edge-to-edge; 0dp corners) for a variant flag to bolt on cleanly — the builders would carry mutually-exclusive fields and `populateSurface` would fork hard. Instead, the ~250 lines of **overlay-host lifecycle** that's identical between the two is **extracted from `ElwhaDialog` into a package-private base** (`AbstractElwhaDialog`), and both dialogs extend it. (§2.)
3. **Reuse, not reinvention:** the extracted base owns host resolution (`getLayeredPane` / `getWindowAncestor` / focus-owner capture), `MODAL_LAYER` attach, the dismiss/teardown lifecycle, the `KeyboardFocusManager` focus trap + restore, the relayout-on-resize listener, and the `MorphAnimator` motion plumbing. Subclasses supply anatomy (`populateSurface`), the backdrop (scrim vs. none), layout (centered band vs. fill), motion (scale-in vs. slide-up), and key bindings.
4. **Tokens (LOCKED, all exist today):** container `ColorRole.SURFACE` (M3 full-screen container is the base surface, *not* `SURFACE_CONTAINER_HIGH` — it fills the frame, so it reads as the page), shape `ShapeScale.NONE` (0dp — square, fills frame), **no elevation shadow** (flush to the frame), headline `ON_SURFACE` / `TypeRole.TITLE_LARGE` (top-app-bar headline, *not* `HEADLINE_SMALL`), close-affordance tint `ON_SURFACE_VARIANT`, divider `OUTLINE_VARIANT`. Confirm/action colors come from the consumer's `ElwhaButton`s. **No new theme tokens.**
5. **Top app bar (LOCKED anatomy):** 56px tall; leading **close affordance** (`ElwhaIconButton`, 24px close glyph), start-aligned **headline** (`TITLE_LARGE`), trailing **confirm text button**; optional **1dp divider** under the bar. Built **inline** in the dialog — *not* a new shared `ElwhaTopAppBar` primitive (that's its own epic; flagged as a future extraction candidate, §7).
6. **Sizing (LOCKED):** container **fills the layered pane** edge-to-edge; the **content column is max 560dp wide, centered**, with 24px top/side padding and 8px inter-element gaps. Filling the frame makes input-blocking trivial (the surface covers everything — no scrim event-consumer needed) and is faithful to "fills the frame." (§4.)
7. **Dismiss (LOCKED):** **no scrim** (nothing to click). The **close affordance** dismisses with cancel semantics; the **confirm** action dismisses with confirm semantics; **Esc** dismisses (cancel semantics) when `dismissibleByEsc` (default `true`). Reuses the shared `DismissCause`.
8. **Motion (LOCKED contract, value tuned at impl):** entrance = **slide-up + fade** (the container translates up from a small offset and fades in) over `MorphAnimator.MEDIUM2_MS` (300ms), `Easing.EMPHASIZED_DECELERATE`; exit reverses (slide-down + fade). Reduced motion snaps. The slide is the full-screen analogue of the Basic Dialog's scale-in — the *direction* is M3-conventional; the *offset* is a tuned default (the M3 component source does not pin full-screen motion, same situation as the Basic Dialog's 0.80 scale; see §13 / Appendix B).
9. **A11y (LOCKED):** `AccessibleRole.DIALOG`, headline = accessible name; initial focus into the dialog (first content field, else the close affordance); focus trap while shown; restore to the trigger on close. Close affordance keyboard-reachable; Esc dismisses. RTL mirrors the app bar (close trailing / confirm leading).
10. **`DismissCause` promotion (LOCKED — pre-1.0 break):** `ElwhaDialog.DismissCause` becomes a top-level `com.owspfm.elwha.dialog.DismissCause` shared by both dialogs. Pre-1.0, no compat shim (CLAUDE.md); documented under `[Unreleased]` in `CHANGELOG.md`. The close affordance maps to `DismissCause.CANCEL` (it's the full-screen analogue of cancel — dismiss without committing). No new cause.
11. **Lib enhancement:** `MaterialIcons.close()` + bundled `close.svg` (Rounded / 400 / fill 0 / 20px optical, per the icon house style) — there is no close glyph today, and the leading affordance needs one. Mirrors how #254 added `ElwhaButton.doClick()`.
12. **Showcase:** the existing "Dialog" Components leaf gains an **"Open full-screen dialog"** trigger in its Workbench and a **static `renderPreview()` snapshot** in its Gallery (the leaf's §15 design already reserved this slot). Same trigger-button + static-snapshot shape as the Basic Dialog — a full-screen dialog is still a modal overlay, just edge-to-edge.
13. **Out of scope this epic:** a shared `ElwhaTopAppBar` primitive (built inline here; extract later if a second consumer needs it), a back-arrow leading affordance (close is the default; back is DOCS/future config), nested/stacked dialogs, modeless dialogs, and `JOptionPane`-style static helpers.

---

## §0. Posture: M3 Expressive baseline

Per [`elwha-design-direction.md`](elwha-design-direction.md) Elwha tracks M3 Expressive. The full-screen dialog spec is stable between baseline M3 and Expressive — same anatomy (top app bar + edge-to-edge content), same dismiss model, same "no scrim, fills the frame." Expressive notes:

- **0dp container corners** — the dialog is flush with the frame; there is no rounded card to express a corner on. (`ShapeScale.NONE`.)
- **Base surface tier** — because it fills the frame, the container is `ColorRole.SURFACE` (the page surface), not the elevated `SURFACE_CONTAINER_HIGH` the Basic Dialog floats on. A full-screen dialog *replaces* the page rather than floating above it.

§9 (a11y), §13 reduced-motion are CODE rules per M3.

---

## §1. Scope decisions — Elwha adaptation

- ✅ **Full-screen Dialog** — the one primitive this epic ships. Top app bar (close → headline → confirm) + edge-to-edge content + optional bottom action bar.
- ✅ **Sibling class** `ElwhaFullScreenDialog` with a fluent builder, sharing the extracted overlay host with `ElwhaDialog`.
- ✅ **Shared-host extraction** — pull the basic-agnostic overlay lifecycle out of `ElwhaDialog` into a package-private `AbstractElwhaDialog`.
- ✅ **Top app bar built inline** — close affordance (`ElwhaIconButton` + new close glyph), start-aligned headline, trailing confirm text button, optional divider.
- ✅ **Slide-up entrance motion** honoring reduced motion, via the shared `MorphAnimator`.
- ✅ **`MaterialIcons.close()`** lib enhancement (+ `close.svg`).
- ❌ **`Variant.FULL_SCREEN` flag on `ElwhaDialog`** — rejected (§2). Anatomy diverges too far for a flag.
- ❌ **Shared `ElwhaTopAppBar` primitive** — the bar is built inline here. A reusable top-app-bar component is a separate epic; extract if a second consumer needs it.
- ❌ **Back-arrow leading affordance** — close (X) is the default and only Phase-1 affordance. M3 allows a back arrow for nested flows; offered later as a builder option (DOCS/future), needs an `arrow_back` glyph.
- ❌ **Scrim** — a full-screen dialog fills the frame; there is nothing behind it to dim.
- ❌ **Nested / stacked / modeless dialogs**, `JOptionPane`-style static helpers — same exclusions as #254.

---

## §2. Architecture — sibling class + extracted shared host [LOCKED]

The load-bearing decision. **Decided (operator, 2026-05-29):** a separate **`ElwhaFullScreenDialog`** class, with the overlay-host lifecycle **extracted from `ElwhaDialog`** into a package-private base both extend. *Not* a `Variant.FULL_SCREEN` flag.

**Why a sibling, not a variant flag:**

| Concern | `Variant.FULL_SCREEN` on `ElwhaDialog` | Sibling `ElwhaFullScreenDialog` |
|---|---|---|
| Builder surface | Carries mutually-exclusive fields (`icon` / `supportingText` / centering apply only to basic; `closeAffordance` / app-bar / bottom-bar only to full-screen) | Each builder is clean and anatomy-appropriate |
| `populateSurface` | Forks hard on the variant — two layouts in one method | Each class owns its own layout |
| Tokens | Container color / shape / elevation all branch on variant | Each class sets its own |
| Motion | Scale-in vs. slide-up branch | Each class supplies its own |
| API read | "a dialog that's sometimes a totally different thing" | Two honest primitives |
| Code duplication | None (but at the cost of all the forks above) | **Solved by extraction** — the identical lifecycle moves to a shared base |

The anatomy genuinely diverges (a top app bar is not a centered headline), so two surfaces is the honest model. The duplication risk a variant flag would avoid is instead removed by **extracting the shared lifecycle**.

### §2.1 What extracts vs. what stays [PROTOTYPE — S1]

The exact base shape is **prototyped and locked in Phase 1 S1** (mirroring how #254 S1 prototyped + locked modality). Provisional split, from a read of `ElwhaDialog.java`:

**Extract to `AbstractElwhaDialog` (base — basic-agnostic):**
- Host-resolution fields + capture: `layeredPane`, `hostWindow`, `orientation`, `focusOwnerBeforeShow`, `closing`, `exitCause`, `motionProgress`.
- `show(Component)` as a **template method**: resolve host → build surface (abstract `createSurface()` / `populateSurface()`) → build backdrop (abstract `installBackdrop()` — scrim for basic, none for full-screen) → attach to `MODAL_LAYER` → install relayout listener + focus trap → run entrance (abstract `entranceMotion()`) → `focusInitial()`.
- `dismiss()` / `dismiss(cause)` / `onMotionTick()` / `performTeardown()` — the whole close lifecycle.
- `installFocusTrap()` / `focusInitial()` / `firstFocusable()` — focus management (full-screen's initial-focus target differs → small overridable hook).
- `relayout()` skeleton + abstract `layoutSurface(layeredPane)` hook (basic: center in 280–560 band w/ 80px vertical inset; full-screen: fill the pane, center the 560 content column).
- Shared helpers: `action(Runnable)`, `wrapHtml(...)`, the `MorphAnimator` wiring.
- **`DismissCause`** promoted to a top-level shared enum (TL;DR #10).

**Stays in `ElwhaDialog` (basic-specific):**
- `populateSurface` / `buildHeader` / `headerLabel` / `buildContentScroll` / `buildSouth` / `addAction`, the `DialogSurface` (28px / `SURFACE_CONTAINER_HIGH` / Level-3 shadow / 280–560 clamp / scale-in `paint()`), the `Scrim`, the `ScrollDivider`, scale-in motion, `availableContentWidth()`, the icon-centering rule, the typed three-action row.

**New in `ElwhaFullScreenDialog` (full-screen-specific):**
- The fill-the-frame surface (`SURFACE` / 0dp / no shadow), the inline top app bar, edge-to-edge scrollable content, optional bottom action bar, slide-up motion, the close-affordance + single-confirm action model.

**Risk:** the extraction refactors a **just-merged primitive** (`ElwhaDialog`, `2d25ccd`). S1's acceptance must include re-smoking the Basic Dialog (Showcase Dialog leaf + the dogfooded About dialog) to prove the refactor is behavior-preserving. This gates the rest of Phase 1.

**Host resolution** is unchanged from #254 §2: attach to `SwingUtilities.getWindowAncestor(parent).getLayeredPane()` at `MODAL_LAYER`; `show(Component parent)` resolves the host frame from any component in the tree.

---

## §3. Component model — one class, builder-configured

A single `ElwhaFullScreenDialog` with a fluent builder. The anatomy parts:

- **Headline** — effectively required (the dialog's accessible name + the app-bar title).
- **Close affordance** — always present (the leading dismiss path); the glyph is fixed (close), keyboard-reachable.
- **Confirm action** — optional trailing **text button** in the app bar (e.g. "Save"). When absent, the app bar is close + headline only.
- **Content** — the primary slot: an arbitrary consumer `JComponent` (a form), edge-to-edge, scrollable when tall.
- **Bottom action bar** — optional; 56px; for flows that want their primary action pinned at the bottom instead of (or in addition to) the app-bar confirm. Phase 1 ships the app-bar confirm; the bottom bar is **CODE-if-needed** (flagged, see §5).

---

## §4. Sizing [LOCKED]

| Property | Value | Source |
|---|---|---|
| Container | **fills the layered pane** (edge-to-edge) | M3 "fills the frame" |
| Container corners | **0dp** (`ShapeScale.NONE`) | M3 full-screen spec table (§14 capture) |
| Content column max width | **560 dp**, centered | M3 spec table |
| Top app bar height | **56 px** | M3 spec table |
| Bottom action bar height (when used) | **56 px** | M3 spec table |
| Top / left / right padding | **24 px** (`SpaceScale.XL`) | M3 spec table |
| Inter-element gap | **8 px** (`SpaceScale.SM`) | M3 spec table |
| Divider (under app bar / scroll) | **1 px** | M3 spec table |
| Headline | start-aligned (LTR) | M3 spec table |
| Close affordance icon | **24 px** | M3 spec table |

**Wide-frame behavior (the one genuinely-open sizing call — LOCKED to "fill + centered column"):** on a frame wider than 560dp, the **container still fills the pane** (`SURFACE`, edge-to-edge, no gutters), and the **content column** (and the app-bar contents) are clamped to 560dp and centered within it. The rejected alternative — clamp the whole container to 560 and leave undimmed side gutters — looks broken on a wide desktop frame and complicates input-blocking. Filling the frame keeps modality trivial: the surface covers everything, so **no scrim event-consumer is needed** (unlike the Basic Dialog, where the scrim does the blocking). *(M3 guidance is that full-screen dialogs suit compact widths and a Basic Dialog is preferred on expanded widths; Elwha leaves that choice to the consumer and renders correctly at any width.)*

**Height** is the full frame height; the **content slot scrolls** inside a viewport when the form is taller than the space between the (pinned) app bar and the (pinned) bottom bar / frame edge. The app bar and bottom bar never scroll.

---

## §5. Top app bar + actions [LOCKED anatomy; bottom bar CODE-if-needed]

The app bar is built **inline** (not a shared primitive). Left → right in LTR:

1. **Close affordance** (leading) — an `ElwhaIconButton` carrying the 24px close glyph, tinted `ON_SURFACE_VARIANT`. Dismisses with `DismissCause.CANCEL`. Keyboard-reachable; Esc is its twin.
2. **Headline** (start-aligned, immediately after the close affordance) — `ON_SURFACE` / `TypeRole.TITLE_LARGE`.
3. **Confirm text button** (trailing, optional) — a consumer-supplied `ElwhaButton` (M3 default: text button). Fires the consumer's listener, then dismisses with `DismissCause.CONFIRM`. Receives the **Enter** binding (the `ElwhaButton.doClick()` hand-wire #254 added).
4. **Divider** (optional) — 1px `OUTLINE_VARIANT` line under the bar; also shown when content scrolls (scroll affordance, mirroring the Basic Dialog's scroll divider).

**Bottom action bar (CODE-if-needed):** M3 allows a 56px bottom action bar for the primary commit action as an alternative to the app-bar confirm. Phase 1 ships the **app-bar confirm** (the M3 default for full-screen). The bottom bar is captured here and built **only if a Phase-1 consumer flow needs it**; otherwise DOCS. Flagged so it isn't an invented cut — it's M3-real, just demand-gated.

**Enter / Esc wiring:** same hand-wiring as #254 (`ElwhaButton` is not a `JButton`): **Enter → confirm.doClick()**, **Esc → close** (dismiss `CANCEL`). Bound on the surface's `WHEN_IN_FOCUSED_WINDOW` maps by the shared base scaffold.

---

## §6. Color axis [LOCKED]

| Anatomy part | Color role | Note |
|---|---|---|
| Container | `ColorRole.SURFACE` | base surface — it *replaces* the page, doesn't float over it |
| Headline text | `ColorRole.ON_SURFACE` | `TITLE_LARGE` |
| Close affordance tint | `ColorRole.ON_SURFACE_VARIANT` | 24px close glyph |
| Confirm / actions | consumer's `ElwhaButton` | dialog doesn't recolor |
| Divider | `ColorRole.OUTLINE_VARIANT` | app-bar + scroll divider |
| Scrim | — | **none** |
| Elevation shadow | — | **none** (flush to frame) |

**No new theme tokens** — every role above exists on `ColorRole` today (verified). **Dark mode** automatic via the theme infrastructure.

---

## §7. Anatomy

1. **Container** — `SURFACE`, 0dp corners, no shadow, fills the layered pane.
2. **Top app bar** (56px, pinned) — close affordance (leading) → headline (`TITLE_LARGE`, start) → confirm text button (trailing, optional).
3. **Divider** (optional, 1px `OUTLINE_VARIANT`) — under the app bar and/or above scrolled content.
4. **Content** — arbitrary consumer `JComponent`, edge-to-edge within the 560dp centered column + 24px padding; scrolls when tall.
5. **Bottom action bar** (optional, 56px, pinned) — CODE-if-needed (§5).

**Future extraction candidate:** the top app bar built here is a strong seed for a shared `ElwhaTopAppBar` primitive (M3 has top-app-bar variants of its own). Out of scope for #271 — extract when a second consumer (a real app bar, not a dialog header) needs it, the same way this epic extracts the dialog host from #254. Noted so the inline build is a deliberate, revisitable choice, not an oversight.

---

## §8. API design [PROTOTYPE — S2]

Fluent builder; `show(parent)` resolves the host frame and runs the slide-up entrance. Mirrors `ElwhaDialog`'s builder shape.

```java
package com.owspfm.elwha.dialog;

public final class ElwhaFullScreenDialog {

  public static Builder builder() { ... }

  /** Show as a modal, frame-filling overlay on the resolved host frame; runs entrance motion. */
  public void show(Component parent) { ... }

  /** Programmatic dismiss (cancel semantics). */
  public void dismiss() { ... }

  /** Non-modal static render of the surface for galleries/docs (no overlay/focus/motion). */
  public JComponent renderPreview() { ... }

  public static final class Builder {
    public Builder headline(String text)                 { ... }   // app-bar title + accessible name
    public Builder content(JComponent content)           { ... }   // the form / body
    public Builder confirmAction(ElwhaButton button)     { ... }   // trailing app-bar text button (optional)
    public Builder showDivider(boolean v)                { ... }   // 1px under the app bar; default false
    public Builder dismissibleByEsc(boolean v)           { ... }   // default true
    public Builder onClose(Consumer<DismissCause> cb)    { ... }
    public ElwhaFullScreenDialog build()                 { ... }
  }
}
```

`DismissCause` is the shared top-level enum (TL;DR #10). There is **no `dismissibleByScrim`** (no scrim). The close affordance is always present (it's the leading dismiss path) — not a builder toggle.

### §8.1 Default values

| Property | Default |
|---|---|
| Close affordance | always present (leading) |
| `confirmAction` | none (app bar = close + headline only) |
| `showDivider` | `false` |
| `dismissibleByEsc` | `true` |
| Container color / shape / elevation | `SURFACE` / `ShapeScale.NONE` / none |
| Headline typography | `TITLE_LARGE` |
| Modality | modal (surface fills the frame; focus trapped) |

### §8.2 Convention adherence

Follows [`docs/development/component-api-conventions.md`](../development/component-api-conventions.md): fluent builder returns `Builder`; `getX()` getter naming; Swing + FlatLaf + Elwha-theme deps only. Builder (not per-variant factories) for the same reason as #254 — variation is which optional slots are filled.

---

## §9. Dismiss & a11y [LOCKED]

**Dismiss:**
- **Close affordance** → `DismissCause.CANCEL` (always available).
- **Confirm action** → consumer listener runs, then `DismissCause.CONFIRM`.
- **Esc** → close (`CANCEL`) when `dismissibleByEsc` (default true).
- **No scrim** → no scrim-dismiss path.
- **Input blocking** — the surface fills the layered pane, so it physically covers all app content; the focus trap keeps keyboard focus inside. No scrim event-consumer needed.

**A11y (reuses #254 §10 machinery via the shared base):**
- **Role:** surface reports `AccessibleRole.DIALOG`; headline is the accessible name.
- **Initial focus:** into the dialog — the **first focusable content field** (full-screen dialogs are input flows; focus belongs in the form), else the **close affordance**, else the surface. *(Differs from the Basic Dialog, which focuses the confirm action; a small overridable hook on the base.)*
- **Focus trap:** Tab / Shift-Tab cycle within the dialog only (shared `KeyboardFocusManager` listener).
- **Focus restore:** to the trigger on close.
- **Keyboard:** Esc = close; Enter = confirm (§5). Close affordance is in the tab order (leading).

---

## §10. RTL mirroring [LOCKED]

CODE rule, driven by `Component.getComponentOrientation()`.

| Orientation | App-bar layout | Headline alignment | Content column |
|---|---|---|---|
| LTR | close (left) → headline → confirm (right) | leading = left | centered |
| RTL | close (right) → headline → confirm (left) | leading = right | centered |

The 560dp content column stays centered (orientation-neutral). The app-bar order and headline alignment are the only places that branch on orientation; standard Swing `ComponentOrientation` propagation applies.

---

## §11. Motion contract [LOCKED contract; offset tuned at impl] + reduced-motion matrix

**Entrance (M3 "full-screen dialog enters"):**
1. Container translates **up** from a small downward offset (slide-up) to its resting position.
2. Container fades **0 → 1**.
3. Eased `Easing.EMPHASIZED_DECELERATE` over `MorphAnimator.MEDIUM2_MS` (300ms).

**Exit:** symmetric reverse (slide-down + fade-out), then the overlay detaches.

**Mechanics:** reuse the Basic Dialog's `MorphAnimator`-on-the-surface plumbing (the shared base already wires `motionProgress` + the progress listener + teardown-at-0). The full-screen surface's `paint()` translates + alpha-composites the subtree (instead of the Basic Dialog's scale + alpha). The full-size-snapshot optimization from `DialogSurface.paint()` (rasterize once, reuse across the ~18 tween frames) applies equally and should be carried over.

> **Provenance:** like the Basic Dialog's 0.80 scale, the full-screen **slide direction** is M3-conventional (full-screen dialogs enter from the bottom) but the **offset distance** is not pinned by the M3 component source (full-screen motion lives in the motion spec, not component constants — Appendix B). Tune it in `S6`'s demo; a sound default is a small fraction of the frame height (e.g. 32–48px or ~8% of height, capped).

### §11.1 Reduced-motion matrix

`MorphAnimator` auto-detects the OS reduced-motion signal and honors `ElwhaTheme.config(...).reducedMotion(...)`. The dialog inherits it.

| Reduced motion | Entrance | Exit |
|---|---|---|
| OFF | slide-up + fade | reverse (slide-down + fade) |
| ON | appears at final position instantly (no slide, no fade) | instant removal |

Verify both states in the S6 demo and the Showcase Workbench motion-preset control.

### §11.2 Known limitation — action-button ripple freezes during the exit fade

The exit fade composites a **one-time snapshot** of the surface at uniform alpha (a live subtree can't be faded — each child resets the alpha composite). Consequently, when an action button (close / confirm) is clicked, its press ripple is captured into that snapshot and **freezes mid-stroke** as the dialog fades out, instead of completing. This is inherent to the snapshot-based fade and is **not** fixable by keeping the ripple live (breaks the fade) or by timing tricks (a delayed close feels laggy). It is addressed in a **separate epic — [#288](https://github.com/OWS-PFMS/elwha/issues/288)** — which adds `ElwhaButton.setRippleEnabled(boolean)` and has the dialogs suppress the press ripple on dismiss action buttons (the exit motion is the dismiss feedback; "no ripple > frozen ripple"). #288 also fixes the same latent behavior in the Basic Dialog. Until #288 lands, the full-screen dialog ships with this minor cosmetic on a departing surface.

(The distinct *at-rest* ripple freeze — a child animating while the dialog is open and not closing — was a separate bug, fixed by declaring the surface a painting origin so descendant repaints route through the motion `paint()`.)

---

## §12. Showcase integration — extend the existing Dialog leaf [S7/S8]

The "Dialog" Components leaf (`buildDialogComponent()` in `ElwhaShowcase.java`, from #254 S7/S8) already pairs a trigger-button **Workbench** with a static-snapshot **Gallery**, and its §15 design reserved an "Open full-screen dialog" trigger slot. This epic fills it:

- **Workbench** — add an **"Open full-screen dialog"** trigger that `show(...)`s a live full-screen dialog on the Showcase frame (the real overlay-on-frame smoke test). Config controls: confirm action present/absent, divider on/off, motion preset (normal / reduced). The dialog's content is a small sample form (a few fields) so the longer-form-input use case reads.
- **Gallery** — a **static `ElwhaFullScreenDialog.renderPreview()` snapshot** alongside the Basic Dialog snapshots (rendered non-modal — no overlay, focus, or motion). A live full-screen dialog would cover the whole frame and break the "all variants at once" read, so the snapshot is essential here.
- No new sidebar entry — it lives under the existing Dialog leaf (it's the same component family, a second dialog type).

---

## §13. Guidelines reference

| Category | Disposition |
|---|---|
| CODE (locked / enforced) | §4 sizes; §5 app-bar order + Enter/Esc wiring; §6 color; §9 dismiss + focus; §10 RTL; §11 motion |
| CODE-IMPLICIT (falls out of design) | Modal by construction — surface fills the frame (§4/§9); no scrim (§6); always-has-a-dismiss-path — close affordance + Esc (§9) |
| CODE-IF-NEEDED | Bottom action bar (§5); content-scroll divider (§5) |
| DOCS | Back-arrow affordance variant; one-dialog-at-a-time; contrast on consumer content; prefer Basic Dialog on expanded widths |
| OUT OF SCOPE | Shared `ElwhaTopAppBar` primitive (§7); `Variant.FULL_SCREEN` flag (§2); modeless / nested dialogs; `JOptionPane`-style helpers |

---

## §14. Story breakdown (provisional — filed after this doc lands)

PRs land at **phase boundaries** per [[feedback_phase_handoff_cadence]] (cadence for this epic set in conversation — see the epic memory); each story gets a **fresh demo class** per [[feedback_fresh_demo_per_story]]; every new issue is added to org **Project #5** per [[feedback_issues_to_project_board]].

### Phase 1 — Host extraction + core full-screen primitive + a11y (no motion)
- **S1.** **Extract the shared overlay host** from `ElwhaDialog` into a package-private `AbstractElwhaDialog` (§2.1); promote `DismissCause` to a top-level shared enum; refactor `ElwhaDialog` to extend the base. **Acceptance includes re-smoking the Basic Dialog** (Showcase Dialog leaf + About dialog) to prove behavior-preservation. **Gates the rest.**
- **S2.** `ElwhaFullScreenDialog` + `Builder` skeleton: fill-the-frame surface (`SURFACE` / `ShapeScale.NONE` / no shadow), 560dp centered content column, 24px padding, headline + content slots. `show()` / `dismiss()` / `renderPreview()` over the shared base.
- **S3.** Top app bar (§5): inline bar — close affordance (`ElwhaIconButton` + **new `MaterialIcons.close()` + `close.svg`**), start-aligned `TITLE_LARGE` headline, trailing confirm text button, optional 1dp divider. Enter→confirm / Esc→close wiring.
- **S4.** Content slot edge-to-edge + scroll-when-tall + scroll divider; dismiss semantics (close / confirm / Esc) + shared `DismissCause` + `onClose`. (Bottom action bar CODE-if-needed.)
- **S5.** A11y (§9) + RTL (§10): `AccessibleRole.DIALOG`, initial focus into the form, focus trap, restore, close-affordance keyboard reach, app-bar RTL mirroring.

**Phase 1 PR boundary:** full-screen dialog functional + accessible, sans entrance motion; Basic Dialog re-verified.

### Phase 2 — Motion
- **S6.** Slide-up entrance/exit (§11) via `MorphAnimator`, snapshot-reuse paint, reduced-motion snap. Fresh demo with a motion-preset toggle.

### Phase 3 — Showcase integration
- **S7.** Extend the Dialog Workbench: "Open full-screen dialog" trigger + sample form + config controls (§12).
- **S8.** Extend the Dialog Gallery: static `renderPreview()` snapshot of the full-screen variant.

**Lib enhancements landed along the way:** `MaterialIcons.close()` + `close.svg` (S3); `DismissCause` promoted to top-level (S1).

---

## Appendix A — Decision history

| Decision | Resolution |
|---|---|
| **Sibling class vs. `Variant.FULL_SCREEN` flag** | **LOCKED — sibling `ElwhaFullScreenDialog`** (operator, 2026-05-29). Anatomy diverges too far for a flag; duplication solved by extracting the shared host instead. |
| **Shared-host extraction** | Extract the basic-agnostic overlay lifecycle from `ElwhaDialog` → package-private `AbstractElwhaDialog`; both dialogs extend it. Exact shape prototyped + locked in S1. |
| **`DismissCause` promotion** | Promote `ElwhaDialog.DismissCause` → top-level `com.owspfm.elwha.dialog.DismissCause`. Pre-1.0 break, no shim, CHANGELOG `[Unreleased]`. Close affordance → `CANCEL`; no new cause. |
| **Container token** | `ColorRole.SURFACE` (base surface), not `SURFACE_CONTAINER_HIGH` — it fills/replaces the page. |
| **Shape / elevation** | `ShapeScale.NONE` (0dp), no shadow — flush to the frame. |
| **Headline typography** | `TITLE_LARGE` (top-app-bar headline), not `HEADLINE_SMALL`. |
| **Wide-frame behavior** | Container fills the pane; content column max 560dp, centered; no gutters. Makes modality scrim-free (surface covers all). |
| **No scrim** | Full-screen fills the frame — nothing to dim. Input blocking is physical coverage + focus trap. |
| **Top app bar inline** | Built inline, not a shared `ElwhaTopAppBar` primitive (future extraction candidate). |
| **Close affordance** | `ElwhaIconButton` + new 24px close glyph; always present (leading); not a builder toggle. |
| **Confirm in app bar** | Optional trailing text button; receives Enter; M3 default = text. |
| **Bottom action bar** | CODE-if-needed — M3-real, demand-gated to a Phase-1 consumer flow; not an invented cut. |
| **Back-arrow affordance** | Out of Phase 1 — close is default; back is a future builder option (needs `arrow_back` glyph). |
| **Motion** | Slide-up + fade, `EMPHASIZED_DECELERATE` 300ms via shared `MorphAnimator`; offset tuned at impl; reduced-motion snaps. |
| **A11y initial focus** | First content field (it's an input flow), not the confirm action — an overridable hook on the base. |
| **Showcase** | Extend the existing Dialog leaf's Workbench + Gallery; no new sidebar entry. |
| **Lib enhancement** | `MaterialIcons.close()` + `close.svg` (mirrors #254's `ElwhaButton.doClick()`). |

---

## Appendix B — M3 / MDC token reference table

Full-screen dimensional values were captured 2026-05-29 from the M3 "Full-screen dialog padding and size measurements" spec table (recorded in `elwha-dialog-design.md` §14) and are carried here. M3 ships **no full-screen-dialog composable** in Jetpack Compose Material3, so — unlike the Basic Dialog — these are **single-sourced from the M3 spec table** (the Basic Dialog's values were dual-sourced against Compose constants; that cross-check isn't available for this type). The motion curve is not pinned by any component source and is tuned at implementation (§11).

| # | Source attribute | Value | Elwha mapping |
|---|---|---|---|
| F1 | Container color | `colorSurface` | `ColorRole.SURFACE` |
| F2 | Container shape | 0dp (square) | `ShapeScale.NONE` |
| F3 | Container elevation | flat (fills frame) | none |
| F4 | Container max width | 560 dp (content column) | layout clamp, centered |
| F5 | Container height | dynamic (fills frame) | full layered-pane height |
| F6 | Top app bar height | 56 dp | 56px literal |
| F7 | Headline color | `colorOnSurface` | `ColorRole.ON_SURFACE` |
| F8 | Headline typography | top-app-bar title | `TypeRole.TITLE_LARGE` |
| F9 | Headline alignment | start | §10 |
| F10 | Close-affordance icon size | 24 dp | `ElwhaIconButton` + 24px close glyph |
| F11 | Close-affordance tint | `colorOnSurfaceVariant` | `ColorRole.ON_SURFACE_VARIANT` |
| F12 | Confirm action | trailing text button | `ElwhaButton.textButton(...)` |
| F13 | Divider height | 1 dp | 1px line |
| F14 | Divider color | `colorOutlineVariant` | `ColorRole.OUTLINE_VARIANT` |
| F15 | Bottom action bar height | 56 dp | 56px literal (CODE-if-needed) |
| F16 | Top/left/right padding | 24 dp | `SpaceScale.XL` (24px) |
| F17 | Inter-element gap | 8 dp | `SpaceScale.SM` (8px) |
| F18 | Scrim | none | — |
| F19 | Esc / close dismiss | `cancel` event | `DismissCause.CANCEL` |
| F20 | Motion (enter/exit) | slide-up + fade (not source-pinned) | `MorphAnimator` + `EMPHASIZED_DECELERATE`, offset tuned (§11) |
