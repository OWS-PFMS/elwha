# ElwhaDialog — Design Decisions

**Status:** Phase 0 spec capture. Tokens, anatomy, action-row order, scrim, dismiss semantics, motion contract, and accessibility are **LOCKED**. The Java API surface (§8) and the modality mechanism (§2) are **DRAFT — settle in Phase 1** because the custom-dialog path (forced by `ElwhaButton extends JComponent`) has a genuine fork that wants a prototype before it locks.

**Drafted:** 2026-05-29

**Author:** Charles Bryan (`cfb3@uw.edu`).

**Parents:**
- [`elwha-design-direction.md`](elwha-design-direction.md) — doctrine bar (raw Swing + tokens can't express it).
- [`elwha-fab-design.md`](elwha-fab-design.md) §15 + `ElwhaFabAnchor` (#205) — the layered-pane overlay placement precedent this epic reuses for the in-window scrim.
- [`elwha-badge-design.md`](elwha-badge-design.md) — design-doc shape and the push-model a11y precedent.
- [M3 Dialogs](https://m3.material.io/components/dialogs/overview) + [MDC-Android `Dialog.md`](https://github.com/material-components/material-components-android/blob/master/docs/components/Dialog.md) + [Material Web `dialog.md`](https://github.com/material-components/material-web/blob/main/docs/components/dialog.md) — the token / anatomy / dismiss source.

**Epic:** [#254](https://github.com/OWS-PFMS/elwha/issues/254). Phasing + child stories filed after this doc lands.

**Milestone:** v0.3.0.

---

## TL;DR

1. **What it is:** `ElwhaDialog` — the M3 **Basic Dialog** primitive. A token-themed modal surface with typed anatomy slots (icon → headline → supporting text → content → actions) and a typed action-row API that takes `ElwhaButton`. Formalizes and templates the hand-rolled About-dialog chrome from #252.
2. **Why it can't wrap `JOptionPane`:** `ElwhaButton extends JComponent`, not `JButton`. `JRootPane.setDefaultButton(JButton)` rejects a `JComponent`, so `JOptionPane` is fully closed to Elwha actions. A custom dialog path is mandatory — there is no shortcut. The Enter-to-confirm and Esc-to-dismiss key wiring is done by hand (the About dialog already proves the Esc half).
3. **Modality mechanism (the central decision, DRAFT):** render the dialog as an **in-window overlay on the host frame's layered pane** with a scrim that consumes input — *not* a separate top-level `JDialog`. Only the overlay path can paint the M3 scrim over the app content and run the scale-in entrance in the same window. Reuses the `ElwhaFabAnchor` (#205) layered-pane precedent. JDialog is the documented fallback if input-blocking proves leaky.
4. **Tokens (LOCKED, all exist today):** container `SURFACE_CONTAINER_HIGH`, shape `ShapeScale.XL` (28px — its own Javadoc says "containers and dialogs"), elevation Level 3 via the shared `ShadowPainter`, headline `ON_SURFACE` / `TypeRole.HEADLINE_SMALL`, supporting text `ON_SURFACE_VARIANT` / `BODY_MEDIUM`, icon tint `SECONDARY`, scrim `SCRIM` @ 32% opacity. **No new theme tokens.**
5. **Action row (LOCKED):** typed API taking `ElwhaButton`, right-aligned (trailing-justified), M3 order **cancel (leading) → alt → confirm (trailing)**. Default style is text buttons; the confirming action may be promoted to filled/tonal. 8px inter-button gap.
6. **Dismiss (LOCKED):** Esc dismisses (cancel semantics); scrim click dismisses when `dismissibleByScrim` (default `true`); confirming/dismissing actions dismiss by firing their listener then closing. A destructive/required-decision dialog can set `dismissibleByScrim(false)`.
7. **Motion (LOCKED):** entrance = scrim fade-in + container scale-in (0.80→1.0) + fade, via the shared `MorphAnimator`; exit reverses. Honors reduced motion (`MorphAnimator` already auto-detects the OS signal and exposes `ElwhaTheme.config(...).reducedMotion(...)`) by snapping with no scale.
8. **A11y (LOCKED):** focus moves into the dialog on open (initial focus = confirming action, or first focusable), Tab is trapped within the dialog while modal, focus restores to the trigger on close. `AccessibleRole.DIALOG`; headline is the accessible name.
9. **Showcase pattern (different from every other leaf):** dialogs are modal overlays, not embeddable surfaces. Workbench = a panel of **trigger buttons** that open live dialogs, configured by surrounding controls. Gallery = **static rendered snapshots**, not a live matrix (live dialogs would stack onto the frame).
10. **Out of scope this epic:** the **Full-screen Dialog** variant (documented in §14, separate phase/epic), nested dialogs, non-modal/modeless dialogs, snackbars/bottom-sheets, and localization of default strings beyond per-instance overrides.

---

## §0. Posture: M3 Expressive baseline, post-May-2025

Per [`elwha-design-direction.md`](elwha-design-direction.md) Elwha tracks M3 Expressive. The dialog spec is stable between baseline M3 and Expressive — same anatomy, same scrim, same action-row order. Expressive-specific notes:

- **Boxier shape, more padding** — Expressive dialogs use the extra-large corner (`ShapeScale.XL`, 28px) and generous 24px content padding for a more prominent headline read. Elwha adopts both.
- **Surface tier** — container is `SURFACE_CONTAINER_HIGH` (one tier above the page surface), giving the dialog visible separation from the scrimmed app content beneath it without relying on the shadow alone.

§10 (a11y), §13 motion reduced-motion matrix are CODE rules per M3.

---

## §1. Scope decisions — Elwha adaptation

- ✅ **Basic Dialog** — the one primitive this epic ships. Icon (optional) → headline → supporting text → content (optional arbitrary component) → actions.
- ✅ **Typed action-row API** taking `ElwhaButton`, enforcing M3 order and spacing.
- ✅ **In-window scrim overlay** with configurable click-to-dismiss + Esc-to-dismiss.
- ✅ **Scale-in entrance motion** honoring reduced motion.
- ✅ **Default token theming** — auto-themes with the rest of the library; dark mode automatic.
- ❌ **Full-screen Dialog variant** — documented (§14) but deferred to a later phase/epic. Longer-form input flows; different anatomy (top app bar with close + confirm, no scrim, edge-to-edge).
- ❌ **`JOptionPane` parity helpers** (`showMessageDialog`-style static convenience) — not in Phase 1. A typed builder is the API; static shorthands can layer later if a consumer asks.
- ❌ **Non-modal / modeless dialogs** — M3 dialogs are modal by definition. Out of scope.
- ❌ **Nested / stacked dialogs** — one dialog at a time per host frame. A second `show()` while one is open is a consumer error (documented; not defended at runtime in Phase 1).
- ❌ **Snackbars, tooltips, bottom sheets, menus** — separate primitives, separate epics.
- ❌ **Localization of default announcement / button strings** — consumer supplies all visible text; no hardcoded English to localize. Reduced-motion + a11y strings are the only defaults and they reuse existing infra.

---

## §2. Modality mechanism — in-window overlay, not `JDialog` [DRAFT — settle in Phase 1]

This is the load-bearing architectural decision and the one thing worth prototyping before locking.

**Decision (recommended):** render `ElwhaDialog` as an **overlay component installed on the host frame's `JLayeredPane`** at a high z-band (the `PALETTE_LAYER` convention coordinated with #205 / #221), consisting of a full-bounds **scrim** layer plus the **dialog surface** centered on top. Modality is achieved by the scrim consuming all mouse and key events that don't land on the dialog surface, plus a focus trap (§10).

**Why not a real `JDialog`:**

| Concern | `JDialog` (own top-level window) | In-window layered-pane overlay |
|---|---|---|
| Scrim over app content | Can't — a separate window can't dim the parent | Native — scrim paints over the frame's content |
| Scale-in entrance motion | Awkward — animating a top-level window's bounds is janky and flickers | Clean — it's just a repaint of a child component |
| M3 fidelity | Partial | Full |
| True input blocking | Free (OS-modal) | Must be implemented (scrim consumes events + focus trap) |
| Multi-monitor / off-frame | OS-managed | Bounded to the frame (acceptable — M3 dialogs are in-app) |

The overlay path matches M3 visuals exactly and reuses the `ElwhaFabAnchor` (#205) layered-pane glue the library already has. Its one cost — we implement input blocking ourselves — is the same scrim-consumes-events pattern every web dialog uses, and is well-understood.

**Fallback (documented, not chosen):** if the overlay's input blocking proves leaky against FlatLaf/Swing focus edge cases during Phase 1 prototyping, fall back to a real modal `JDialog` and accept the motion/scrim compromise. The decision locks at the end of Phase 1's first story after a prototype, not now.

**Host resolution:** the dialog attaches to `SwingUtilities.getWindowAncestor(parent).getLayeredPane()` (an `RootPaneContainer`). The `show(Component parent)` call resolves the host frame from any component in the tree, mirroring `JOptionPane.showXxx(parentComponent, ...)`.

---

## §3. Component model — one component, builder-configured

**Decision:** a single `ElwhaDialog` class with a fluent builder. Not per-variant factories (badge/FAB used those because their variants are visually distinct primitives; a basic dialog has one form whose parts are present-or-absent).

The anatomy parts are **optional slots**:

- Icon — optional. Present ⇒ icon + headline + supporting text center-aligned; absent ⇒ start-aligned (M3 rule, §7).
- Headline — effectively required (the dialog's accessible name); a dialog with no headline is a degenerate case we don't optimize for.
- Supporting text — optional. Short prose.
- Content — optional arbitrary `JComponent` (a form, a list). Mutually useful with supporting text or instead of it.
- Actions — 0–3 `ElwhaButton`s via the typed action-row API (§5).

A "destructive confirm" or "basic alert" is the same class with different slots filled — the builder, not a subclass, expresses that.

---

## §4. Sizing

| Property | Value | Source |
|---|---|---|
| Min width | 280 dp | M3 dialog spec |
| Max width | 560 dp | M3 dialog spec |
| Content padding | 24 px (`SpaceScale.XL`) all sides | M3 / MDC |
| Window inset (overlay margin from frame edge) | 24 px sides, 80 px top/bottom min | MDC `Dialog.md` |
| Corner radius | 28 px (`ShapeScale.XL`) | MDC `shapeAppearanceCornerExtraLarge` |
| Inter-element vertical gaps | icon→headline 16px, headline→supporting 16px, supporting/content→actions 24px | M3 reference |
| Action-row inter-button gap | 8 px (`SpaceScale.SM`) | M3 |

**Height** is content-driven. When content exceeds the available frame height (minus the 80px top/bottom inset), the **content slot scrolls** inside a viewport while icon/headline pin to the top and the action row pins to the bottom (M3 scrollable-content behavior). Headline and actions never scroll out.

---

## §5. Action row [LOCKED]

The typed action-row API takes `ElwhaButton` instances and lays them out per M3.

- **Alignment:** right-justified (trailing-justified) within the dialog width. RTL flips to leading-justified (§11).
- **Order (left → right in LTR):** **cancel/dismissive (leading) → alternate → confirm/primary (trailing).** The confirming action is always the trailing-most (rightmost) button. Stated trailing-first per the issue: *trailing primary → alt → cancel.*
- **Default style:** M3 dialog actions are **text buttons** (`ElwhaButton.textButton(...)`). The confirming action *may* be promoted to filled or tonal for emphasis — the API allows it; the default does not force it.
- **Count:** 0–3 actions. Most basic dialogs have 1–2 (confirm + cancel). The typed API assigns roles, not raw positions, so the component places them in M3 order regardless of add order:

```java
.confirmAction(ElwhaButton)     // trailing-most
.alternateAction(ElwhaButton)   // middle (optional)
.cancelAction(ElwhaButton)      // leading-most
```

- **Stacking:** when the buttons don't fit the dialog width on one row, M3 stacks them vertically (confirm on top, full-width, right-aligned labels). Phase 1 may defer stacking to DOCS if no consumer hits it; flagged as CODE-if-needed.
- **Gap:** 8px between buttons; 24px between the content/supporting text and the action row.

**Enter-to-confirm wiring:** because `ElwhaButton` is not a `JButton`, `JRootPane.setDefaultButton` is unavailable. The dialog binds **Enter → confirmAction.doClick()** on the dialog's input/action maps, the symmetric twin of the **Esc → cancelAction** binding the About dialog already demonstrates. This hand-wiring is exactly the workaround #254 exists to formalize.

---

## §6. Color axis [LOCKED]

| Anatomy part | Color role | MDC token |
|---|---|---|
| Container | `SURFACE_CONTAINER_HIGH` | `colorSurfaceContainerHigh` |
| Headline text | `ON_SURFACE` | `colorOnSurface` |
| Supporting text | `ON_SURFACE_VARIANT` | `colorOnSurfaceVariant` |
| Icon tint | `SECONDARY` | `colorSecondary` |
| Scrim | `SCRIM` @ **32%** opacity | MDC scrim dim 32% |
| Elevation shadow | `SHADOW` via `ShadowPainter` | M3 elevation Level 3 |
| Divider (optional) | `OUTLINE_VARIANT` | — |

**No new theme tokens** — every role above exists on the `ColorRole` facade today (verified against `ColorRole.java`). Action-button colors come from the `ElwhaButton`s the consumer passes; the dialog doesn't recolor them.

**Dark mode** is automatic via the theme infrastructure. **Scrim** is the `SCRIM` role painted at 32% alpha over the full frame content, matching MDC's documented dim amount.

---

## §7. Anatomy

Source: MDC `Dialog.md` numbered anatomy.

1. **Scrim** — full-frame `SCRIM` @ 32%, behind the surface, consumes input.
2. **Container** — `SURFACE_CONTAINER_HIGH`, 28px corners, Level-3 shadow, 24px interior padding, 280–560px wide.
3. **Icon** (optional) — `SECONDARY` tint, centered when present.
4. **Headline** — `ON_SURFACE`, `HEADLINE_SMALL`. Centered when an icon is present, start-aligned otherwise.
5. **Supporting text** (optional) — `ON_SURFACE_VARIANT`, `BODY_MEDIUM`.
6. **Content** (optional) — arbitrary consumer `JComponent`; scrolls when tall.
7. **Divider** (optional) — `OUTLINE_VARIANT`, shown above the action row only when content scrolls (M3 scroll affordance).
8. **Action row** — text buttons, trailing-justified, M3 order (§5).

**Icon-present alignment rule (CODE):** the single conditional in the layout. Icon present ⇒ icon, headline, and supporting text are horizontally centered. Icon absent ⇒ headline and supporting text are start-aligned (leading). This is the M3 "with-icon dialogs center their content" rule.

---

## §8. API design [DRAFT — settle in Phase 1]

Fluent builder; `show(parent)` resolves the host frame and runs the entrance motion.

```java
package com.owspfm.elwha.dialog;

public final class ElwhaDialog {

  public static Builder builder() { ... }

  /** Show the dialog as a modal overlay on the resolved host frame; runs entrance motion. */
  public void show(Component parent) { ... }

  /** Programmatic dismiss (cancel semantics). */
  public void dismiss() { ... }

  public static final class Builder {
    public Builder icon(Icon icon)                       { ... }   // optional; centers content
    public Builder headline(String text)                 { ... }
    public Builder supportingText(String text)           { ... }
    public Builder content(JComponent content)           { ... }   // optional arbitrary body

    // Typed action row — roles, not positions (component enforces M3 order)
    public Builder confirmAction(ElwhaButton button)     { ... }   // trailing-most
    public Builder alternateAction(ElwhaButton button)   { ... }   // middle (optional)
    public Builder cancelAction(ElwhaButton button)      { ... }   // leading-most

    // Dismiss configuration
    public Builder dismissibleByScrim(boolean v)         { ... }   // default true
    public Builder dismissibleByEsc(boolean v)           { ... }   // default true

    // Lifecycle hook (fires after the dialog closes, with the dismiss cause)
    public Builder onClose(Consumer<DismissCause> cb)    { ... }

    public ElwhaDialog build()                           { ... }
  }

  /** Why the dialog closed — reported to onClose(...). */
  public enum DismissCause { CONFIRM, CANCEL, ALTERNATE, SCRIM, ESC, PROGRAMMATIC }
}
```

**Open API questions to settle in Phase 1 (after the §2 prototype):**
- Does `show()` block (pseudo-modal, like `JOptionPane`) or return immediately and report via `onClose`? Overlay path leans **non-blocking + callback**; JDialog path could block. The `DismissCause` callback works for both.
- Whether `confirmAction` auto-receives the Enter binding or it's opt-in.
- Whether the action buttons are passed pre-built (consumer wires their own listeners) or the dialog wraps them to inject the close-after-fire. Pre-built + dialog-attaches-a-trailing-listener is the leaning design.

### §8.1 Default values

| Property | Default |
|---|---|
| `icon` | none (⇒ start-aligned content) |
| `dismissibleByScrim` | `true` |
| `dismissibleByEsc` | `true` |
| Container color | `SURFACE_CONTAINER_HIGH` |
| Corner radius | `ShapeScale.XL` (28px) |
| Action style | consumer's `ElwhaButton` (M3 default = text) |
| Modality | modal (scrim consumes input) |

### §8.2 Convention adherence

Follows [`docs/development/component-api-conventions.md`](../development/component-api-conventions.md): fluent builder returns `Builder`; `getX()` getter naming on any exposed state; depends on Swing + FlatLaf + Elwha theme only. The builder pattern (rather than per-variant static factories) is the right fit here because the variation is which optional slots are filled, not distinct visual variants.

---

## §9. Scrim & dismiss semantics [LOCKED]

- **Scrim paint:** `SCRIM` role @ 32% alpha over the full frame content, beneath the dialog surface.
- **Scrim click:** dismisses with `DismissCause.SCRIM` **iff** `dismissibleByScrim` (default `true`). When `false` (destructive / required-decision dialogs), a scrim click is consumed but does nothing — M3's "dialogs requiring a decision aren't scrim-dismissible."
- **Esc:** dismisses with `DismissCause.ESC` iff `dismissibleByEsc` (default `true`). Bound on the dialog root's `WHEN_IN_FOCUSED_WINDOW` input map (the About-dialog pattern, formalized).
- **Action buttons:** firing `confirmAction` / `cancelAction` / `alternateAction` closes the dialog with the corresponding `DismissCause` after the consumer's own listener runs.
- **Input blocking:** while shown, the scrim consumes every mouse event not over the dialog surface, and the focus trap (§10) keeps keyboard focus inside. This is what makes the overlay "modal" without an OS window.

---

## §10. Accessibility [LOCKED]

- **Role:** the dialog surface reports `AccessibleRole.DIALOG`; the headline string is its accessible name (and `aria-label`-equivalent). A dialog with content but no headline must be given an explicit accessible name (DOCS).
- **Initial focus (on open):** focus moves into the dialog — to the **confirming action** if present, else the first focusable descendant (a content field), else the dialog surface itself. Never leaves focus on the now-inert background.
- **Focus trap:** while the dialog is shown, Tab / Shift-Tab cycle within the dialog's focusable descendants only. The scrimmed background is not tab-reachable. (On the JDialog fallback this is free; on the overlay path it's an explicit `FocusTraversalPolicy` + a focus listener that pulls focus back.)
- **Focus restore (on close):** focus returns to the component that triggered `show()` (captured at show time), mirroring `JDialog`'s native behavior.
- **Keyboard:** Esc = cancel (§9); Enter = confirm (§5 wiring). Tab order follows visual order (icon/headline are non-focusable; content fields, then action row leading→trailing).
- **Screen reader:** on open, the dialog announces its role + headline. Supporting text is read as the dialog's description where the platform AT supports it.

### §10.1 Other a11y rules — DOCS

| Rule | Disposition |
|---|---|
| ≥3:1 contrast for any consumer-supplied content colors | DOCS — trust the consumer; tokens already satisfy it |
| Don't trap focus with no escape (must always have a dismiss path) | CODE-IMPLICIT — Esc default-on; a no-cancel dialog still has Esc unless the consumer disables both, which is documented as discouraged |
| Reduced motion | CODE — §13, via `MorphAnimator` |

---

## §11. RTL mirroring [LOCKED]

**CODE rule.** Driven by `Component.getComponentOrientation()`.

| Orientation | Headline/supporting alignment (no icon) | Action row justification |
|---|---|---|
| LTR | leading = left | trailing = right; order cancel→…→confirm left→right |
| RTL | leading = right | trailing = left; order mirrors (confirm leftmost) |

Icon-present centering is orientation-neutral. The action-row layout and start-aligned text are the only places that branch on orientation; standard Swing `ComponentOrientation` propagation from the parent frame applies.

---

## §12. Guidelines reference

Captured from the M3 Dialogs spec + MDC docs during this pass.

| Category | Disposition |
|---|---|
| CODE (locked / enforced) | §4 sizes; §5 action order + Enter wiring; §6 color; §7 icon-centering; §9 scrim/dismiss; §10 focus mgmt; §11 RTL; §13 motion |
| CODE-IMPLICIT (falls out of design) | Modal-by-construction (§2); no scrim-dismiss when required-decision (§9); always-has-a-dismiss-path (§10.1) |
| DOCS | Action stacking (if deferred); contrast on consumer content; no-headline accessible-name requirement; one-dialog-at-a-time |
| OUT OF SCOPE | Full-screen variant (§14); modeless; nested dialogs; static `JOptionPane`-style helpers |

---

## §13. Motion contract [LOCKED] + reduced-motion matrix

**Entrance (M3 "dialog enters"):**
1. Scrim fades 0 → 32% alpha.
2. Container fades 0 → 1 and scales **0.80 → 1.0** about its center.
3. Duration / easing from the M3 emphasized-decelerate token; resolved against `MorphAnimator`'s existing curve at implementation.

> **Provenance note:** the `0.80 → 1.0` scale and the duration/easing are the only spec values *not* pinned by component source (all tokens + dimensions are cross-verified against Compose Material3 — see Appendix B). M3 motion is defined in the motion spec, not in component constants, and the curve is tuned against `MorphAnimator` at implementation regardless. Treat the `0.80` start scale as a sound M3-conventional default, not a locked spec figure.

**Exit:** reverses (scale 1.0 → ~0.90, fade to 0, scrim fades out), then the overlay detaches from the layered pane.

**Reuse:** the shared `MorphAnimator` (`theme/`) drives the tween; no new animation engine. This mirrors how FAB / Button / Nav Rail reuse the morph infrastructure rather than each rolling its own.

### §13.1 Reduced-motion verification matrix

`MorphAnimator` already auto-detects the OS reduced-motion signal at class load (mac/Windows/Linux) and honors `ElwhaTheme.config(...).reducedMotion(...)`. The dialog inherits that.

| Reduced motion | Entrance | Exit |
|---|---|---|
| OFF | scrim fade + container scale-in (0.80→1.0) + fade | reverse |
| ON | scrim + container appear at final state instantly (no scale, no fade) | instant removal |

Verify both states in the Phase 2 demo and in the Showcase Workbench (a motion-preset control).

---

## §14. Full-screen Dialog variant — deferred (documented)

M3's second dialog type, for longer-form input flows (e.g., "create event"). **Not in this epic;** captured so the namespace and anatomy are on record.

- **Anatomy:** top app bar (close/back affordance leading, headline, confirm text button trailing) + edge-to-edge content. **No scrim** — it fills the frame. Optional divider under the app bar.
- **Measurements** (captured 2026-05-29 from the M3 "Full-screen dialog padding and size measurements" spec table, for when this variant is built): container shape **0dp** corners (square — fills the frame), container height dynamic, max width 560dp; **header height 56dp**, headline **start-aligned**, close-affordance icon 24dp, divider height 1dp; **bottom action bar height 56dp**; top/left/right padding 24dp; 8dp between elements.
- **When:** content too large for a basic dialog, multi-field forms, or mobile-narrow frames where a centered dialog would be cramped.
- **Likely shape:** either a `Variant.FULL_SCREEN` on `ElwhaDialog` or a sibling `ElwhaFullScreenDialog` — decide when the first consumer flow needs it. The overlay/host-resolution machinery from Phase 1 is reusable either way.
- **Tracking:** file as a follow-up story/epic after Basic Dialog Phase 1 lands and a consumer flow is identified in OWS-Local-Search-GUI.

---

## §15. Showcase integration — trigger-button Workbench + static Gallery

Per #254: dialogs don't fit the standard `ComponentWorkbench` (which embeds the live component into a stage area). They're modal overlays, not embeddable surfaces.

- **Workbench** — a control panel of **trigger buttons** ("Open basic dialog", "Open dialog with icon", "Open destructive confirm", "Open scrollable-content dialog", and — once it lands — "Open full-screen dialog"). Surrounding controls configure what the buttons open: icon present, action count (1/2/3), `dismissibleByScrim`, `dismissibleByEsc`, motion preset (normal / reduced). Clicking a trigger opens a live dialog on the Showcase frame — which doubles as the real-world overlay-on-frame smoke test.
- **Gallery** — **static rendered snapshots** of the variants (basic, with-icon, destructive, scrollable) rendered as non-modal surface previews, *not* a live matrix. Live dialogs would stack onto the frame and break the gallery's "all variants at once" read. The snapshots render the dialog surface (container + slots) without the scrim/modality.
- **Sidebar entry** under Components, mirroring the FAB Phase 4 (#204) / Badge Phase 2 (#217) Gallery+sidebar pattern.

---

## §16. Story breakdown (provisional — filed after this doc lands)

Phasing is provisional per the stub epic. PRs land at **phase boundaries** per [[feedback_phase_handoff_cadence]]; each story gets a **fresh demo class** per [[feedback_fresh_demo_per_story]]; every new issue is added to org **Project #5** per [[feedback_issues_to_project_board]].

### Phase 1 — Core primitive + modality + a11y (no motion)
- **S1.** Modality-mechanism prototype + decision lock (§2): overlay-on-layered-pane scrim vs. JDialog fallback. Deliverable: a thrown-together scrim overlay on the host frame that blocks input, with the §2 table resolved to a chosen path. **Gates the rest of Phase 1.**
- **S2.** `ElwhaDialog` + `Builder` skeleton: container surface (token color, 28px shape, Level-3 `ShadowPainter` shadow, 24px padding, 280–560px width), headline + supporting-text slots, icon slot with the icon-present centering rule (§7).
- **S3.** Typed action row (§5): `confirmAction` / `alternateAction` / `cancelAction`, M3 trailing-justified order, 8px gap, Enter→confirm + Esc→cancel wiring (§5/§9).
- **S4.** Scrim + dismiss semantics (§9): scrim paint @ 32%, `dismissibleByScrim` / `dismissibleByEsc`, `DismissCause` + `onClose`, content slot + scroll-when-tall + scroll divider.
- **S5.** A11y (§10): `AccessibleRole.DIALOG`, initial focus, focus trap, focus restore, RTL (§11).

**Phase 1 PR:** basic dialog fully functional and accessible, sans entrance motion.

### Phase 2 — Motion
- **S6.** Entrance/exit motion (§13) via `MorphAnimator`: scrim fade + container scale-in, reduced-motion snap. Demo: motion-preset toggle.

### Phase 3 — Showcase integration
- **S7.** Showcase Workbench (trigger-button panel + config controls, §15).
- **S8.** Showcase Gallery (static snapshots) + sidebar entry.

**Future (separate epic):** Full-screen Dialog variant (§14); a consumer-side migration tracking issue on OWS-Local-Search-GUI once the Phase 1 API stabilizes.

---

## Appendix A — Decision history

Decisions captured during the spec pass on 2026-05-29.

| Decision | Resolution |
|---|---|
| **Can't wrap `JOptionPane`** | `ElwhaButton extends JComponent`; `setDefaultButton(JButton)` rejects it. Custom dialog path mandatory. |
| **Modality mechanism** | Recommend in-window layered-pane scrim overlay (reuses #205); JDialog documented as fallback. DRAFT — prototype in Phase 1 S1 before locking. |
| **One class + builder, not per-variant factories** | Variation is which optional slots are filled, not distinct visual variants. |
| **Typed action roles, not positions** | `confirmAction`/`alternateAction`/`cancelAction`; component enforces M3 trailing-justified order regardless of add order. |
| **Action order** | cancel (leading) → alt → confirm (trailing); trailing-most is confirming. |
| **Default action style = text** | M3 default; confirm may be promoted to filled/tonal, not forced. |
| **Enter→confirm hand-wired** | Symmetric twin of the existing About-dialog Esc binding; the workaround #254 formalizes. |
| **Tokens** | Container `SURFACE_CONTAINER_HIGH`, shape `ShapeScale.XL` (28px), headline `ON_SURFACE`/`HEADLINE_SMALL`, supporting `ON_SURFACE_VARIANT`/`BODY_MEDIUM`, icon `SECONDARY`, scrim `SCRIM`@32%, shadow `SHADOW` via `ShadowPainter`. No new tokens. |
| **Icon-present centering** | The single layout conditional; matches M3. |
| **Scrim dismiss configurable** | `dismissibleByScrim` default true; false for required-decision dialogs. |
| **Esc dismiss configurable** | `dismissibleByEsc` default true. |
| **Sizing 280–560px** | M3 dialog width bounds; height content-driven with scrollable content slot. |
| **Motion** | scrim fade + container scale-in 0.80→1.0 via shared `MorphAnimator`; reduced-motion snaps. |
| **A11y** | `AccessibleRole.DIALOG`, headline = name, initial focus on confirm, focus trap, restore to trigger. |
| **Full-screen variant deferred** | Documented §14; separate phase/epic when a consumer flow needs it. |
| **No `JOptionPane`-style static helpers Phase 1** | Builder is the API; static shorthands can layer later. |
| **Showcase = trigger buttons + static gallery** | Dialogs aren't embeddable; per #254. |

---

## Appendix B — M3 / MDC token reference table

Color / type / shape / scrim roles captured 2026-05-29 from the MDC-Android and Material-Web component docs. **Every dimensional and elevation value is dual-sourced** — cross-verified verbatim against *both* (a) the canonical Jetpack Compose Material3 source (`DialogTokens.kt` / `AlertDialog.kt` on `androidx-main`, which hard-codes them as constants) *and* (b) the m3.material.io "Basic dialog padding and size measurements" spec table (captured 2026-05-29). The two agree on every value. The only value *not* pinned by either source is the entrance-motion curve — it lives in the motion spec, not the component spec, and is tuned at implementation (see §13).

**One reconciled discrepancy:** the M3 spec table specifies uniform **24dp padding on all sides** with no exception. Compose additionally drops to 20dp in the icon-present variant — that's a Compose implementation detail, *not* M3 spec. The spec is authoritative here: **Elwha uses 24dp uniformly** (T10).

| # | Source attribute | Value | Compose constant | Elwha mapping |
|---|---|---|---|---|
| T1 | Container color | `colorSurfaceContainerHigh` | `ContainerColor` | `ColorRole.SURFACE_CONTAINER_HIGH` |
| T2 | Container shape | `shapeAppearanceCornerExtraLarge` (28dp) | `ContainerShape = CornerExtraLarge` | `ShapeScale.XL` (28px) |
| T3 | Container elevation | Level 3 | `ContainerElevation = ElevationTokens.Level3` | `ShadowPainter` (shared) |
| T4 | Headline color | `colorOnSurface` | `HeadlineColor` | `ColorRole.ON_SURFACE` |
| T5 | Headline typography | `textAppearanceHeadlineSmall` | `HeadlineFont = HeadlineSmall` | `TypeRole.HEADLINE_SMALL` |
| T6 | Icon tint | `colorSecondary` | `IconColor = Secondary` | `ColorRole.SECONDARY` |
| T7 | Supporting text color | `colorOnSurfaceVariant` | `SupportingTextColor` | `ColorRole.ON_SURFACE_VARIANT` |
| T8 | Supporting text typography | `textAppearanceBodyMedium` | `SupportingTextFont = BodyMedium` | `TypeRole.BODY_MEDIUM` |
| T9 | Scrim dim amount | 32% | (MDC) | `ColorRole.SCRIM` @ 0.32 alpha |
| T10 | Content padding (all sides) | 24dp | `dialogPadding` (spec: 24dp uniform; Compose impl uses 20dp icon-present) | `SpaceScale.XL` (24px) |
| T11 | Window inset | 24dp sides / 80dp top-bottom | (MDC) | overlay margin |
| T12 | Action button style | TextButton.Dialog | `ActionLabelTextColor = Primary` | `ElwhaButton.textButton(...)` |
| T13 | Divider color | `colorOutlineVariant` | (MDC) | `ColorRole.OUTLINE_VARIANT` |
| T14 | Min / max width | 280 / 560 dp | `DialogMinWidth` / `DialogMaxWidth` | layout bounds |
| T15 | Esc / scrim dismiss | `cancel` event | (Material-Web) | `DismissCause.ESC` / `.SCRIM` |
| T16 | Icon→headline / headline→supporting gap | 16dp each | `IconPadding` / `TitlePadding` (bottom=16dp) | `SpaceScale.LG` (16px) |
| T17 | Action-row inter-button gap | 8dp | `ButtonsMainAxisSpacing = 8.dp` | `SpaceScale.SM` (8px) |
| T18 | Action label typography | `textAppearanceLabelLarge` | `ActionLabelTextFont = LabelLarge` | `TypeRole.LABEL_LARGE` |
| T19 | Body→actions gap | 24dp | (spec table) | `SpaceScale.XL` (24px) |
| T20 | Divider height (scroll divider) | 1dp | (spec table) | 1px line |
| T21 | Alignment: with icon / without | center / start | (spec table) | §7 centering rule |
