# ElwhaTooltip — Phase 0 Design

**Status:** LOCKED pending the S1 spike (§2). **Drafted:** 2026-06-11. **Author:** Charles Bryan (`cfb3@uw.edu`).
**Parents:** epic [#445](https://github.com/OWS-PFMS/elwha/issues/445). **Milestone:** `v0.4.0`.
**Research:** [`elwha-tooltip-research.md`](elwha-tooltip-research.md) — the captured spec; this doc holds only the decisions.

---

## TL;DR — the locked decisions

1. **One public class, two variants:** `ElwhaTooltip extends AbstractElwhaOverlay` in `com.owspfm.elwha.tooltip` — `TooltipVariant.PLAIN` / `RICH`, per-variant factories (`ElwhaTooltip.plain(String)` / `ElwhaTooltip.rich()` builder). The surface is a package-private `JComponent`; the trigger machinery is a package-private `TooltipTrigger`. [RECOMMENDED; locked via S1 spike — §2]
2. **First passive-focus overlay:** add a `protected boolean takesFocus()` hook (default `true`) to `AbstractElwhaOverlay`; `false` skips the initial-focus grab, the focus-escape listener, and focus restore. **Tooltips never steal focus** — the load-bearing divergence from menu/dialog. §2.
3. **Zero new theme tokens** — plain = `INVERSE_SURFACE` / `INVERSE_ON_SURFACE` / `BODY_SMALL` / `ShapeScale.XS`, flat; rich = `SURFACE_CONTAINER` / elevation 2 (`ShadowPainter`) / `ShapeScale.MD` / `TITLE_SMALL` + `BODY_MEDIUM` in `ON_SURFACE_VARIANT`; actions are real `ElwhaButton.textButton`s (the TEXT variant *is* the M3 action token row). §4.
4. **Geometry (px=dp):** 4 anchor gap; plain pads 8×4, min 40×24, wraps at max width 200; rich pads 16 horizontal with baseline rhythm 28 (top→subhead) / 24 (subhead→text) / 16 (text→bottom, no actions) / action row min 36 + 8 bottom, wraps at max width 320. Hand-rolled `FontMetrics` word wrap — no HTML views (#305 doctrine). §5.
5. **Placement:** prefer **above**, flip below on collision, clamp to the pane with an 8 px edge margin; horizontal alignment `START` / `CENTER` / `END` flush with the anchor (plain defaults `CENTER`, rich defaults `END`), RTL-mirrored. `POPUP_LAYER` (tops dialogs, like menus). §5.
6. **Triggers (the `attach` machinery):** hover dwell **500 ms** / hide linger **600 ms** (both settable); keyboard-focus shows immediately (`FocusVisible.isKeyboardCause` gating); pointer over the **anchor ∪ tooltip surface** keeps it open (WCAG 1.4.13 hoverable — the submenu hover-watch idiom); no auto-timeout while hovered; one tooltip visible at a time globally; Esc dismisses; wheel + anchor-removal dismiss. §7.
7. **Rich behavioral split:** **default** rich shows/hides like plain (and hides on click inside contents); **persistent** rich (`setPersistent(true)`, rich-only — throws on plain, the `ButtonVariant.TEXT` precedent) is toggled by anchor click (mousePressed — macOS drop doctrine) / Enter / Space, ignores hover, dismisses on outside-press / Esc / re-toggle / action click. §7.
8. **Action clicks always dismiss** then fire the consumer listener — both rich flavors. §7.
9. **Motion:** fade-only via `motionProgress` alpha — **150 ms in / 75 ms out**, `Easing.STANDARD`; reduced-motion snaps (overlay host already handles). No scale transform (live `ElwhaButton` children — `isPaintingOrigin` trap not worth it for a tooltip). §6.
10. **a11y:** surface role `AccessibleRole.TOOL_TIP` + accessible name = the text; `attach` writes the anchor's `AccessibleContext.setAccessibleDescription` (the `aria-describedby` analogue, same wiring Swing's own `setToolTipText` performs); Esc dismissal; no focus theft. §8.
11. **V1 = one phase, six stories** (S1 spike/plain chrome → S2 triggers → S3 rich → S4 persistent → S5 motion/RTL/a11y → S6 Showcase). §11.

## §1. Scope — what V1 ships

- ✅ Both M3 variants (plain / rich), both rich flavors (default / persistent), full desktop trigger machinery, above/below flip + clamped alignment, actions as `ElwhaButton.textButton`s, fade motion, Esc + WCAG 1.4.13 conformance, light/dark, RTL, reduced motion.
- ✅ Showcase Workbench + Gallery + sidebar entry + headless smokes; CHANGELOG entry.
- ❌ Caret/arrow, touch long-press + auto-timeout, Compose side-placement — platform extras, parked with breadcrumbs (research §G), not M3 anatomy; **no excluded M3 variants exist** (research §E — nothing stubbed, nothing cut).
- ❌ Lib-wide migration of existing `setToolTipText` call sites — that's the dogfood-sweep epic #424's turf; this epic ships the primitive + Showcase page only.

## §2. Architecture — the load-bearing decision [RECOMMENDED; locked via S1 spike]

**`ElwhaTooltip extends AbstractElwhaOverlay`**, mounting on `POPUP_LAYER` with `lightDismiss() = true`, **plus a new `takesFocus()` strategy hook on the host** (default `true`; tooltip returns `false`):

- `takesFocus() == false` ⇒ `show(...)` skips `focusInitial()` and never installs the focus-escape listener; teardown skips focus restore (`restoreFocusOnClose()` also `false`). The anchor keeps focus the whole time — a hover tooltip that yanks focus would break typing.
- Everything else is exactly what a tooltip needs and already exists: host resolution off the anchor, layered-pane mount, `MorphAnimator` entrance/exit with `motionProgress`, relayout-on-resize, the outside-press AWT listener (persistent rich), idempotent teardown.
- The hook is **additive** on a library-internal base (`AbstractElwhaOverlay` is non-public-API by contract) — menu/dialog behavior is untouched at `true`.

**Why not a bare `JLayeredPane.add` (badge/FAB-anchor style)?** Tooltips need the overlay lifecycle — entrance/exit motion, resize relayout, outside-press routing, teardown ordering — all of which the host already owns and the menu already proved at `POPUP_LAYER`. Re-rolling it would be the third copy of the plumbing #298 extracted the host to kill.

**Why not Swing's `ToolTipManager`/`JToolTip`?** Single global manager with legacy delays, heavyweight-window popups that escape theming, no rich anatomy, no persistent flavor, and `ToolTipUI` paints outside the Elwha token pipeline. Dedicated primitives over styled Swing widgets — the standing doctrine.

**S1 spike proves:** the `takesFocus()` opt-out keeps focus on the anchor across show/dismiss (caret keeps blinking in a focused text field while a tooltip shows over it); plain chrome paints token-correct light+dark; placement (above/flip/clamp) lands correctly in pane coordinates; programmatic `show`/`dismiss` round-trips; teardown leaves no listeners behind. Fallback if the host fights the opt-out: a slim dedicated mount in `tooltip/` copying only attach/motion (not anticipated — the hook touches three call sites).

## §3. Anatomy / API

**`ElwhaTooltip`** (public, not a `JComponent` — an overlay handle, the `ElwhaMenu` shape):

- Factories (component-api doctrine): `ElwhaTooltip.plain(String text)`; `ElwhaTooltip.rich()` → `RichBuilder` with `.subhead(String)` (optional), `.supportingText(String)` (required), `.action(String, ActionListener)` (repeatable), `.persistent(boolean)` (optional), `.build()`.
- `attach(JComponent anchor)` — installs the trigger machinery; one anchor per tooltip (re-attach throws `IllegalStateException`; `detach()` first). `detach()` removes every listener and dismisses if showing.
- `show(Component anchor)` (inherited, public) — programmatic show without/with attach; `dismiss()` — programmatic dismiss; `isTooltipShowing()`.
- Config setters: `setText` (plain), `setSubhead`/`setSupportingText` (rich), `setPersistent(boolean)` (rich-only; **throws** on plain — the `ButtonVariant.TEXT` "M3 prohibits" precedent), `setShowDelayMs(int)`, `setHideDelayMs(int)`, `setPreferredPlacement(TooltipPlacement.ABOVE/BELOW)`, `setAlignment(TooltipAlignment.START/CENTER/END)`, getters for all.
- `getVariant()`, plus `TooltipVariant { PLAIN, RICH }`, `TooltipPlacement { ABOVE, BELOW }`, `TooltipAlignment { START, CENTER, END }` enums in the package.

**`TooltipSurface`** (package-private `JComponent`): paints the container (round-rect fill; rich adds the elevation-2 shadow via `ShadowPainter` + shadow insets) and the text block(s) with hand-rolled `FontMetrics` word wrap; rich hosts the action row as real `ElwhaButton.textButton` children (start-aligned, 8 gap). Non-opaque; alpha-composited by `motionProgress`.

**`TooltipTrigger`** (package-private): the attach machinery — anchor `MouseListener` (enter/exit/press), `KeyListener` (Enter/Space for persistent), `FocusListener` (keyboard-cause gated), the two `javax.swing.Timer`s (dwell + linger, `setRepeats(false)` — the `ElwhaSubMenuItem` idiom), the global hover-watch `AWTEventListener` while shown, the Esc + wheel watch while shown, the anchor `HierarchyListener` (dismiss on removal/hide), and the static one-at-a-time registry.

## §4. Tokens & color [zero new tokens — LOCKED]

Full mapping: research §T; load-bearing rows:

| Part | PLAIN | RICH |
|---|---|---|
| Container | `INVERSE_SURFACE`, `ShapeScale.XS`, flat | `SURFACE_CONTAINER`, `ShapeScale.MD`, elevation 2 (`ShadowPainter`) |
| Subhead | — | `ON_SURFACE_VARIANT`, `TITLE_SMALL` |
| Text | `INVERSE_ON_SURFACE`, `BODY_SMALL` | `ON_SURFACE_VARIANT`, `BODY_MEDIUM` |
| Actions | — | `ElwhaButton.textButton` (TEXT variant = `PRIMARY` `LABEL_LARGE` + its own state layers) |

No state layers on the surface (non-interactive container); the action buttons bring their own. All colors resolved **at paint time** (`ColorRole.resolve()` — the binding rule, no caching).

## §5. Measurements, placement & geometry

| Quantity | Value |
|---|---|
| Anchor gap | 4 (both variants, both placements) |
| Plain padding | 8 horizontal × 4 vertical; min 40×24; **max width 200** (label wraps, centered single-line by default) |
| Rich padding | 16 horizontal; **max width 320** |
| Rich vertical rhythm | container top → subhead first **baseline** 28; subhead baseline → supporting first baseline 24; no subhead ⇒ container top → supporting first baseline 24; supporting last baseline → bottom 16 (no actions); with actions: supporting last baseline → action row, row min-height 36, row → bottom 8 |
| Action row | start-aligned, 8 gap between buttons, 16 side padding (8 effective to the button's own label inset — noted, accepted) |
| Edge clamp margin | 8 to every pane edge (MDC's 32 is a browser-viewport rule; in-window panes are tighter — research §M) |
| Placement | prefer `ABOVE`, flip to `BELOW` when the gap+height collides with the top margin (and vice versa for an explicit `BELOW` preference); alignment `START`/`CENTER`/`END` aligns the tooltip edge flush with the same anchor edge, then clamps |
| Defaults | plain `CENTER`; rich `END` (research §Open-3) |
| RTL | `START`/`END` resolve through the anchor's `ComponentOrientation`; rich shadow insets participate in the box math (`ShadowBearing` doctrine) |

Anchor bounds convert to pane coordinates via `SwingUtilities.convertRectangle`; `layoutSurface` re-derives placement on host resize. A **`ShadowBearing` anchor's halo reserve is backed out** of the anchor rect before placing — the shadow band is not visual anchor, and leaving it in reads as a too-wide gap (added at smoke; pinned in `TooltipPlainChromeSmoke`). Residual, accepted: placement measures from component bounds, so a component stretched by a fill layout beyond its preferred size anchors at its stretched edge, not its centered body — only the component knows its body rect (no public accessor today). The surface's preferred size is content-driven (wrap math), never `getMaximumSize`-overridden (#199/#200 doctrine).

## §6. States & motion

- **No interactive states on the surface** — tooltips have no hover/press/focus treatment of their own; the action `ElwhaButton`s carry the only state layers.
- **Entrance:** fade 0→1 over **150 ms**, `Easing.STANDARD` (override `motionDurationMs()`/`easing()`); the surface paints itself and children through an `AlphaComposite(motionProgress)` — fade-only, no scale (live button children make transform tricks an `isPaintingOrigin` liability for zero spec mandate; research §I motion is fade-dominant).
- **Exit:** **75 ms** — `dismiss()` retunes the animator duration before `beginClose()`.
- **Reduced motion:** `MorphAnimator.setReducedMotion` snaps both ways (host behavior, free).
- Re-show while exiting: a fresh `show()` after teardown; mid-exit hover re-entry within the linger window cancels the hide timer before `beginClose()` is ever reached (the timer is the gate).

## §7. Behaviors — triggers & dismissal

**The attach machinery (per research §I, MDC desktop model):**

- **Hover:** pointer enters the anchor → dwell timer (**500 ms** default, `setShowDelayMs`); leaves the **anchor ∪ surface** union → linger timer (**600 ms**, `setHideDelayMs`) → dismiss. While shown, a global mouse-motion watch (the `ElwhaSubMenuItem` hover-watch idiom) arms/disarms the linger as the pointer crosses the 4 px gap onto the tooltip — hovering the tooltip keeps it open (WCAG 1.4.13 *hoverable*). **No auto-timeout while hovered** (*persistent*).
- **Keyboard focus:** anchor `focusGained` with `FocusVisible.isKeyboardCause()` → show immediately (no dwell); `focusLost` → dismiss (no linger). Mouse-click focus does not trigger (the gating exists precisely for this).
- **Programmatic:** `show(anchor)` / `dismiss()` at any time; programmatic show skips timers.
- **One at a time:** a static current-tooltip slot; showing any tooltip dismisses the incumbent immediately.
- **Esc** while shown dismisses (passive AWT key watch — focus may be anywhere; WCAG 1.4.13 *dismissible*). **Wheel** events dismiss (anchor may scroll out from under the surface). Anchor removed from the hierarchy / hidden → dismiss.
- **Default rich:** identical show/hide to plain, plus a click inside the contents dismisses (MDC). Action click ⇒ dismiss **then** fire the listener (consumer code may open dialogs — the tooltip must already be gone).
- **Persistent rich** (`setPersistent(true)`): hover/focus triggers are **disarmed**; anchor `mousePressed` toggles (mousePressed, not clicked — the macOS rapid-click drop, #299 doctrine); Enter/Space on a focused anchor toggles; dismiss on outside-press (host's light-dismiss routing), Esc, wheel, action click, or re-toggle. Clicking inside the contents does *not* dismiss. `[DOC]` MDC's pairing warning (persistent anchors shouldn't carry their own click actions) goes in the `setPersistent` Javadoc verbatim.
- **Anchor press dismisses (non-persistent)** — pressing the anchor means the user is acting on the control; the tooltip is noise from that instant (Swing/browser desktop convention; MDC's touch model re-shows on tap, which is meaningless under a mouse). Added during S2; persistent rich replaces this path with the click toggle. `[CODE — desktop adaptation]`
- **Swing tooltip interop:** `attach` neither reads nor clears an existing `setToolTipText` — consumers must not double-book an anchor (Javadoc on `attach`). `[DOC]`

## §8. Accessibility

- Surface: `AccessibleRole.TOOL_TIP`; accessible name = plain text / rich subhead+supporting concatenation (the host's `accessibleName()` hook).
- Anchor: `attach` sets `AccessibleContext.setAccessibleDescription` to the same string — the `aria-describedby` analogue and exactly what Swing's own `setToolTipText` wires; `detach` clears it (only if still ours).
- No focus theft (§2); Esc dismissal; WCAG 1.4.13 hoverable/dismissible/persistent per §7.
- Persistent-rich action buttons are ordinary focusable `ElwhaButton`s — reachable by the root pane's geometric traversal while mounted; V1 adds no forced focus move into the tooltip (MDC `tabindex="-1"` parity; research §A). Focus that does wander in doesn't dismiss (no focus-escape listener) — outside-press/Esc remain the exits. `[DOC]`

## §9. Showcase pattern

`TooltipShowcasePanels` (showcase package) + registration in `ElwhaShowcase` (the checkbox `LeafEntry` shape) as **"Tooltip"**:

- **Workbench:** stage hosts a row of live anchors (`ElwhaIconButton`s + an `ElwhaButton` — dogfood) with tooltips attached; controls — Variant (plain/default rich/persistent rich), Text/Subhead/Supporting fields, Action count, Placement, Alignment, Show/Hide delay spinners, RTL, programmatic Show now; code panel re-renders the construction snippet.
- **Gallery:** static matrix of pre-laid-out surfaces (bypassing triggers — surfaces constructed directly): plain short / plain wrapped-at-200 / rich subhead+text / rich no-subhead / rich with one and two actions / dark-mode row.
- `ElwhaTooltipShowcaseSmoke` headless guard: panels construct, gallery renders to a `BufferedImage`, workbench controls apply.

## §10. Out of scope (documented, not cut)

- **Caret/arrow, touch long-press + 1.5 s timeout, side placement** — platform extras outside the M3 anatomy/placement language; parked with breadcrumbs (research §G). No stub epics — they are not M3 variants (research §E).
- **`setToolTipText` call-site migration** across the lib/Showcase — epic #424 (dogfood sweep) owns sweeps; the Workbench/Gallery here dogfood the new primitive day-one.
- **Forced keyboard focus into persistent-rich actions** — MDC parity says don't; revisit on a real a11y consumer need.
- **Multi-anchor sharing** (one tooltip instance serving many anchors) — `ToolTipManager`-style pooling adds registry complexity for no current consumer; one tooltip per anchor, `detach`/re-`attach` to move.

## §11. Phasing → stories (Phase 1 = V1, single phase)

- **S1 — overlay passive-focus spike + plain chrome & placement** ([447](https://github.com/OWS-PFMS/elwha/issues/447)) — `takesFocus()` hook on `AbstractElwhaOverlay`; `ElwhaTooltip.plain` + `TooltipSurface` static chrome (tokens, wrap at 200, min sizes, 8×4 padding); placement engine (above/flip/clamp, START/CENTER/END, 4 gap, 8 margin); programmatic `show`/`dismiss`; light+dark. *Locks §2.* Demo: `TooltipPlainChromeDemo`; guard: `TooltipPlainChromeSmoke`.
- **S2 — trigger machinery** ([448](https://github.com/OWS-PFMS/elwha/issues/448)) — `attach`/`detach`, hover dwell/linger timers, hover-watch union (anchor ∪ surface), keyboard-focus trigger with `FocusVisible` gating, one-at-a-time, Esc + wheel + hierarchy dismissal, delay setters. §7. Demo: `TooltipTriggerDemo`; guard: `TooltipTriggerSmoke`.
- **S3 — rich tooltip anatomy + default-rich behavior** ([449](https://github.com/OWS-PFMS/elwha/issues/449)) — `rich()` builder, subhead/supporting baseline rhythm, 320 wrap, elevation-2 shadow + insets, `END` corner alignment, action row of `textButton`s, click-inside + action-click dismissal. §3/§4/§5. Demo: `TooltipRichDemo`; guard: `TooltipRichSmoke`.
- **S4 — persistent rich** ([450](https://github.com/OWS-PFMS/elwha/issues/450)) — `setPersistent` (rich-only throw), click/Enter/Space toggle, hover disarm, outside-press routing, inside-click immunity. §7. Demo: `TooltipPersistentDemo`; guard: `TooltipPersistentSmoke`.
- **S5 — motion, RTL & a11y** ([451](https://github.com/OWS-PFMS/elwha/issues/451)) — 150/75 fade via `motionProgress` alpha + retuned exit, reduced-motion snap, RTL mirror of START/END + rich corner default, `TOOL_TIP` role + accessible name + anchor description wiring. §6/§8. Demo: `TooltipMotionRtlDemo`; guard: `TooltipA11ySmoke`.
- **S6 — Showcase + CHANGELOG** ([452](https://github.com/OWS-PFMS/elwha/issues/452)) — §9 panels + registration + `ElwhaTooltipShowcaseSmoke`; CHANGELOG `[Unreleased]` entry. *Completes V1; closes the epic.*

### S1 spike outcome (2026-06-11)

Confirmed — §2 locked as built. The `takesFocus()` opt-out short-circuits exactly the three expected host call sites (`focusInitial` scheduling, `installFocusListener`, teardown restore — §12-1; the chain-parent refocus never fires for chainless overlays) plus a defensive `takesFocus() && restoreFocusOnClose()` at teardown. A re-entry guard was added to `show()` while in there (double-show would strand the first surface and its listeners on the pane — always a caller bug, now a no-op). `TooltipPlainChromeSmoke` asserts the contract end to end on a real pane: focus-owner identity unchanged across show/dismiss, `POPUP_LAYER` mount, 4 px placement in pane coordinates, double-show no-op, teardown clean — 28/28. One smoke-side correction: a top-of-frame anchor *legitimately* flips below, so the fixture centers the anchor before asserting the ABOVE case.

## §12. Open for the S1 spike

1. Exactly which host call sites the `takesFocus()` hook short-circuits — `focusInitial` scheduling, `installFocusListener`, and the teardown restore are the expected three; verify no fourth focus path (the chain-parent refocus only fires for chained overlays — tooltips never chain).
2. Whether the fade composite must wrap `paintChildren` too (rich action buttons) or the surface's own `paint` override suffices — verify the buttons don't pop in at full alpha mid-entrance.
3. Plain wrapped-label line layout: centered lines vs leading-aligned — M3 renders centered single-line; multi-line plain labels read leading-aligned in Compose. Verify visually at 200 px.
