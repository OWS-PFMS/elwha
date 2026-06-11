# ElwhaRadioButton — Phase 0 Design

**Status:** LOCKED (S1 spike confirms §2) + **smoke iterate 1 applied (§15 — operator-driven `ElwhaCheckbox` API alignment, 2026-06-11; supersedes parts of §3/§5/§6/§12)**. **Drafted:** 2026-06-10. **Author:** Charles Bryan (`cfb3@uw.edu`).
**Parents:** epic [#416](https://github.com/OWS-PFMS/elwha/issues/416). **Milestone:** `v0.4.0`.
**Research:** [`elwha-radiobutton-research.md`](elwha-radiobutton-research.md) — the captured spec; this doc holds only the decisions.

---

## TL;DR — the locked decisions

1. **One dedicated `ElwhaRadioButton extends JComponent`** — custom paint, hand-wired input + a11y; **not** a styled `JRadioButton`/`ButtonUI`. [RECOMMENDED; locked via S1 spike — §2]
2. **Package `com.owspfm.elwha.radio`** — two classes: the radio and the non-visual `ElwhaRadioGroup`. §2, §8.
3. **No variant / size enums.** The M3 radio spec defines none — one 20dp form. Configurations: selected/unselected × enabled/disabled. §1.
4. **Zero new theme tokens** — full color/opacity/shape map onto `ColorRole` / `StateLayer` / `ShapeScale.FULL`, including the **press swap** (research §T). §4.
5. **Geometry (px=dp):** icon 20 — ring outer Ø20, **2dp filled ring** (`Area` subtraction, the switch-S1 lesson; material-web mask-builds it too), inner dot Ø10; state layer 40 circle concentric. Preferred size **40×40** (state-layer-inclusive box, icon centered). **No `getMaximumSize` override** (the #199 trap). §5.
6. **State boolean, not a Swing model.** `selected` is a plain field + `ChangeListener` (any change) + `ActionListener` (user selection commits only — command `"selected"`). No `ButtonModel`. §2.
7. **A user gesture never deselects** ("clicking on a radio input always selects it" — research §B). Programmatic `setSelected(false)` is allowed (clears a group selection). §7.
8. **Motion (M3 verbatim, no deviation):** dot grows 0→1 over 300ms `Easing.EMPHASIZED_DECELERATE`; deselect fades the dot (no shrink) and ring color crossfades, both 50ms linear (component-local `COLOR_FADE_MS = 50`). Reduced-motion snaps globally via `MorphAnimator`. §6.
9. **`ElwhaRadioGroup` is a non-visual controller** (the `ButtonGroup` shape, Elwha-native): explicit `add`/`remove`, mutual exclusion, `getSelected`/`setSelected`/`clearSelection`, one group `ChangeListener`. First-selected-wins on conflicting adds (the `ButtonGroup` precedent). §8.
10. **Group keyboard = material-web's controller, verbatim:** arrows move **and select** (selection follows focus), wrap, skip disabled, RTL flips horizontal; **roving tab stop** via the three focusable rules. §9.
11. **a11y is Swing-native:** `AccessibleRole.RADIO_BUTTON` + `AccessibleState.CHECKED` + `AccessibleAction` + `AccessibleValue(0/1)` + **`AccessibleRelation.MEMBER_OF`** across group members; name via `setLabel` / `labelFor`. §10.
12. **V1 = one phase, seven stories** (S1 spike → interaction → motion → group → keyboard → a11y → Showcase). §13.
13. **Smoke iterate 1 (§15):** the radio carries the `ElwhaCheckbox` label/geometry/focus contract — attached clickable `setLabel` text, `setAccessibleLabel` override, 48px leading touch-target block in the preferred size, `FocusVisible`-gated single state layer, `doClick()`. Locked by the operator against PR #435.

## §1. Scope — what V1 ships

- ✅ The complete M3 radio button: anatomy, all ten state cells (enabled/hover/focus/pressed × selected/unselected, disabled × selected/unselected), motion, group semantics, group keyboard contract, a11y, RTL, dark mode.
- ✅ `ElwhaRadioGroup` with the full §B′ contract (mutual exclusion + arrows + roving tab stop).
- ✅ Showcase Workbench + Gallery + sidebar entry + headless smoke; CHANGELOG entry.
- ❌ Labeled radio-row composite (M3 *list item* pattern, not radio anatomy) — §12.
- ❌ `javax.swing.ButtonGroup`/`ButtonModel` interop (structurally impossible — `ButtonGroup` takes `AbstractButton`) — §12.
- ❌ Error/invalid styling — no such tokens exist for radio (unlike checkbox) — §12.
- **No excluded M3 variants exist** — the spec has exactly one radio; nothing is stubbed or cut (research §E).

## §2. Architecture — the load-bearing decision [RECOMMENDED; locked via S1 spike]

**One dedicated `ElwhaRadioButton extends JComponent` in `com.owspfm.elwha.radio`,** painting all chrome, with input (click, Space) and a11y hand-wired; **`ElwhaRadioGroup`** is a plain object (not a `JComponent`) coordinating members.

**Why not `JRadioButton` + custom `ButtonUI`?** The switch §2 calculus applies wholesale — `BasicRadioButtonUI`'s text/icon layout model fights the icon-only anatomy, FlatLaf styling would need active suppression — *plus* two radio-specific nails: (a) `ButtonGroup` mutual exclusion gives no hook for **selection-follows-focus arrow navigation or roving focusability** — the group contract that is most of this epic's behavioral value — so it would have to be fought rather than reused; (b) `ButtonModel`'s armed/pressed lifecycle still buys nothing over the hand-wired press tracking the switch/slider already proved. Mirrors `ElwhaSwitch` / `ElwhaSlider` precedent.

**S1 spike proves:** static chrome paints token-correct in light+dark for all four static cells (selected/unselected × enabled/disabled), the 40×40 halo-inclusive preferred size lays out in `FlowLayout`/`GridLayout` without clipping the state layer, and the selected API round-trips with `ChangeListener` events. Fallback documented here if the spike fails (none anticipated — third pass down the slider/switch path).

## §3. Anatomy / primitive

Two painted parts (M3 nouns — research §C/§G; "ring" and "dot", MDC's "circles that fill with an inset"):

1. **Ring** — Ø20 outer circle minus Ø16 hole = a 2dp filled ring (`Area` subtraction; no stroke, no seam, disabled translucency never double-blends). Always painted.
2. **Dot** — Ø10 filled circle, concentric, painted while selected (scaled by the grow tween mid-flight).

Plus the non-anatomy **state layer**: 40px circle concentric with the icon, and the press **ripple** bounded to the same circle via `RipplePainter`. There is no visible label slot — labels are external (§10, §12).

## §4. Tokens & color [zero new tokens — LOCKED]

The full mapping is research §Tokens; the load-bearing rows:

| Part | Unselected | Selected |
|---|---|---|
| Ring | `ON_SURFACE_VARIANT` (hover/focus/pressed → `ON_SURFACE`) | `PRIMARY` (all states) |
| Dot | — | `PRIMARY` (all states) |
| Hover/focus layer tint | `ON_SURFACE` | `PRIMARY` |
| **Pressed layer tint** | **`PRIMARY`** (anticipates selection — research §C′) | **`ON_SURFACE`** |

Disabled (no layers, no motion): ring — and dot, when selected — `ON_SURFACE` @ `StateLayer.disabledContentOpacity()` (0.38), **symmetric** across both sides (no switch-style asymmetry). All roles resolved **at paint time** (`ColorRole.resolve()` / `StateLayer.opacity()` — the binding rule, no caching).

## §5. Measurements & geometry

| Quantity | Value |
|---|---|
| Icon (ring outer) | Ø20, corner-full |
| Ring width | 2, painted as a filled ring inside Ø20 |
| Dot | Ø10, concentric |
| State layer | Ø40, concentric |
| Halo overhang past the icon | (40 − 20) / 2 = 10 per side |
| Preferred size | **40×40** — the state-layer-inclusive box; icon centered |
| Touch target | 48 — [DOC] guidance; whole component bounds are interactive |

`getPreferredSize`/`getMinimumSize` return 40×40; **never** override `getMaximumSize` (#199/#200 doctrine). The chrome centers itself in whatever bounds the layout grants.

## §6. States & motion

**Static layers:** hover (`StateLayer.HOVER` 0.08) and focus (`StateLayer.FOCUS` 0.10, painted while `isFocusOwner()` — the switch precedent) fill the 40px circle in the state's tint role and can stack independently. Pressed paints `StateLayer.PRESSED` (0.10) **in the swapped tint** + the `RipplePainter` ripple from the press point, clipped to the circle. Gallery hooks `setHovered(boolean)` / `setPressed(boolean)` force the treatments for static rendering. The pressed/hover/focus ring shift (`ON_SURFACE_VARIANT → ON_SURFACE`) applies while any interactive treatment is active and the radio is unselected.

**Motion (research §Mo — M3 verbatim, no deviation this time):**

| Animation | Driver | Duration / easing |
|---|---|---|
| Dot grow (select) | `dotTween` — retargeting tween over dot scale 0→1 | `MEDIUM2_MS` 300ms, `Easing.EMPHASIZED_DECELERATE` |
| Dot fade (deselect) | dot alpha 1→0 at full scale — **no shrink** | `COLOR_FADE_MS` 50ms, `Easing.LINEAR` |
| Ring color (both directions) | `colorTween` — crossfade between the two ring roles | `COLOR_FADE_MS` 50ms, `Easing.LINEAR` |
| Ripple | `Timer` per `RipplePainter` contract | 400ms (painter's curve) |

- Mid-flight retarget (deselect during the grow) continues from the current scale/alpha — the switch's `RetargetTween` idiom, re-implemented privately (§14-2).
- Disabled or not-displayable ⇒ snap. Reduced-motion: `MorphAnimator` global snap behavior, free.
- No press size morph — the icon never resizes (research §Mo); press feedback is layer + ripple only.

## §7. Behaviors & keyboard (single radio)

- **Click** (press+release within bounds): **selects**; a click on the already-selected radio changes nothing and fires nothing (research §B). Press focuses the radio (material-web parity).
- **Space:** key-press shows the pressed treatment + ripple, key-release selects (Swing button semantics). Enter not bound.
- **`setSelected(boolean)`:** both directions allowed programmatically; animates when displayable+enabled, snaps otherwise; fires `ChangeListener` only on actual change; **never** fires `ActionListener`. When grouped, `setSelected(true)` routes through the group's exclusion (§8); `setSelected(false)` leaves the group empty.
- **`addActionListener`:** user selection commits only (click, Space, arrow arrival, a11y "click" action) — command `"selected"`; there is no `"deselected"` command because no user gesture deselects.
- Disabled: no listeners react, default cursor, no layers; `HAND_CURSOR` on hover while enabled.

## §8. `ElwhaRadioGroup` — mutual exclusion + selection model

A **non-visual controller** (plain class; layout stays free-form — members can sit in any container/geometry):

- `add(ElwhaRadioButton)` / `remove(ElwhaRadioButton)`; **membership order = arrow-navigation order.** A radio belongs to at most one group — adding to a second group removes it from the first.
- **Exclusion:** any member reaching `selected == true` (user gesture or programmatic) deselects the previously selected member. Adding an already-selected radio to a group that has a selection **deselects the incoming radio** (first-selected-wins — the `javax.swing.ButtonGroup` precedent).
- `getSelected()` → the selected member or `null`; `setSelected(ElwhaRadioButton)` (must be a member); `clearSelection()`.
- `remove` of the selected member clears the selection.
- **Events:** group `addChangeListener` fires once per selection change, including to/from empty. Members' own `ChangeListener`s still fire per-member (the deselected one tells its subscribers itself — no `ItemListener`-style two-event protocol; research §Open-3).

## §9. Group keyboard nav & roving tab stop [the S5 risk point]

Material-web's `SingleSelectionController` contract, verbatim (research §B′):

- **Arrows:** Up/Left = previous, Down/Right = next, in membership order; **wraps**; **skips disabled members**; horizontal arrows flip under the focused member's right-to-left `ComponentOrientation` (Up/Down unaffected). Arrival **focuses and selects** (selection follows focus) and fires the user-gesture events.
- **Roving tab stop — the three rules:** (1) a selected member is the group's only focusable member; (2) while a member is focused, the others are not focusable; (3) none selected or focused → all enabled members are focusable. The group enforces this by managing `setFocusable` flags on selection change, focus gained/lost, membership change, and enabled change.
- Bindings are installed by the group on `add` and removed on `remove` — an ungrouped radio has no arrow behavior and plain focusability.
- **Risk point (tune in S5):** flag-flips during focus transit can bounce focus in Swing — the group must set the new stop focusable *before* clearing the old, and `requestFocusInWindow` before re-applying rule 2. The S5 smoke walks Tab/Shift-Tab/arrow sequences headlessly to pin this.

## §10. Accessibility

`AccessibleElwhaRadioButton extends AccessibleJComponent implements AccessibleAction, AccessibleValue` — Swing-native vocabulary, no role compromise:

- Role **`AccessibleRole.RADIO_BUTTON`**; `AccessibleState.CHECKED` while selected; state-change `PropertyChangeEvent` (`ACCESSIBLE_STATE_PROPERTY`) on toggle.
- `AccessibleAction`: one action, "click", performs the user-gesture **select** (fires both listener kinds; no-ops when already selected — §7).
- `AccessibleValue`: 0/1; `setCurrentAccessibleValue` maps to `setSelected` (programmatic semantics).
- **`AccessibleRelation.MEMBER_OF`** — the group maintains the relation on every member, targeting all current members, updated on add/remove.
- Name: `setLabel(String)` → `getAccessibleName()`, falling back to a `JLabel.setLabelFor(radio)` association. Class Javadoc states a radio **always** needs a label (research §A).

## §11. Showcase pattern

`RadioButtonShowcasePanels` (showcase package) + registration in `ElwhaShowcase` under Components as **"Radio button"**, the `LeafEntry` + Workbench/Gallery `JTabbedPane` shape the switch uses:

- **Workbench:** stage hosts a live 3-member `ElwhaRadioGroup`; controls — selected member, per-member enabled, label text, RTL, reduced motion; code panel re-renders the construction snippet per state. Workbench controls remain plain `J*` per the established Workbench convention.
- **Gallery:** matrix rows = Basic / Grouped (3-member strip); columns = Unselected, Selected, Hover, Focused, **Pressed-unselected, Pressed-selected** (the §4 swap needs both cells), Disabled-unselected, Disabled-selected — via the `setHovered`/`setPressed` hooks.
- `ElwhaRadioButtonShowcaseSmoke` headless guard: panels construct, gallery renders to a `BufferedImage` without throwing, workbench controls apply.

## §12. Out of scope (documented, not cut)

- **Labeled radio row** (text + leading radio line item) — M3 *list item* pattern; consumers compose with a `JLabel` + `setLabelFor`. Same resolution as the switch §10.
- **`javax.swing.ButtonGroup` / `ButtonModel` / `ItemListener` interop** — `ButtonGroup` requires `AbstractButton`, structurally unavailable to a dedicated `JComponent`; `ElwhaRadioGroup` is the grouping surface.
- **Error/invalid styling** — `md-comp-radio-button` defines no error tokens (checkbox has them; radio does not).
- **Materialised 48px touch-target padding** — [DOC] guidance on desktop; revisit if a touch consumer appears.

## §13. Phasing → stories (Phase 1 = V1, single phase)

- **S1 — architecture spike + static chrome** ([#417](https://github.com/OWS-PFMS/elwha/issues/417)) — `ElwhaRadioButton` skeleton, §2/§3/§4/§5 static paint, selected API + `ChangeListener`, 40×40 preferred size, light+dark token-correct. *Locks §2.* Demo: `ElwhaRadioButtonChromeDemo`; guard: `ElwhaRadioButtonChromeSmoke`.
- **S2 — interaction & state layers** ([#418](https://github.com/OWS-PFMS/elwha/issues/418)) — click/Space select-only semantics, hover/focus/pressed layers **with the press swap**, ring shift, press ripple, `ActionListener` semantics, gallery hooks, cursor. §6-static/§7. Demo: `ElwhaRadioButtonInteractionDemo`; guard: `ElwhaRadioButtonInteractionSmoke`.
- **S3 — motion** ([#419](https://github.com/OWS-PFMS/elwha/issues/419)) — dot-grow tween (300ms emphasized-decelerate), deselect fade + ring crossfade (50ms linear), retargeting, reduced-motion + non-displayable snap. §6. Demo: `ElwhaRadioButtonMotionDemo`; guard: `ElwhaRadioButtonMotionSmoke`.
- **S4 — `ElwhaRadioGroup`** ([#420](https://github.com/OWS-PFMS/elwha/issues/420)) — §8 in full: exclusion, selection API, group events, membership edge rules. Demo: `ElwhaRadioGroupDemo`; guard: `ElwhaRadioGroupSmoke`.
- **S5 — group keyboard nav** ([#421](https://github.com/OWS-PFMS/elwha/issues/421)) — §9 in full: arrows + selection-follows-focus + wrap + disabled skip + RTL + the three roving rules. Demo: `ElwhaRadioGroupKeyboardDemo`; guard: `ElwhaRadioGroupKeyboardSmoke`.
- **S6 — a11y, label & RTL** ([#422](https://github.com/OWS-PFMS/elwha/issues/422)) — §10 in full + disabled-guards audit. Demo: `ElwhaRadioButtonA11yRtlDemo`; guard: `ElwhaRadioButtonA11ySmoke`.
- **S7 — Showcase + CHANGELOG** ([#423](https://github.com/OWS-PFMS/elwha/issues/423)) — §11 panels + registration + `ElwhaRadioButtonShowcaseSmoke`; CHANGELOG `[Unreleased]` entry. *Completes V1; closes the epic.*

## §14. Open for the spikes / follow-ups

1. Whether the 2dp ring at Ø20 renders crisply at 1× via `Area` subtraction in both light/dark (expected yes — the switch ring proved the technique at capsule scale; this is the same move on circles). [S1]
2. **`RetargetTween` duplication** — the idiom lives privately in `ElwhaSwitch` on the unmerged #401 branch; this epic re-implements it privately in `ElwhaRadioButton`. **File a `theme/` extraction follow-up issue once both epics are on `main`** — flagged here so the duplication is a recorded decision, not an accident. [merge time]
3. Focus-transit ordering for the roving-stop flag flips (§9 risk point) — pin with the S5 smoke's Tab/arrow walk. [S5]
4. Whether arrow-arrival selection should animate the dot grow (it should — it's a user gesture like click; confirm the tween isn't starved by rapid arrow repeats). [S5]

## §15. Smoke iterate 1 — `ElwhaCheckbox` API alignment (2026-06-11, operator-driven)

`ElwhaCheckbox` (epic #410, PR #435) merged while this epic's PR was in smoke, shipping the sibling
selection control **with an attached clickable label**. The operator ruled the pair must share one
API ("we have different APIs for checkbox radio — that's probably wrong"). Changes applied on the
epic branch, superseding the original locks where named:

1. **`setLabel(String)` is the visible label** (supersedes §3's icon-only anatomy and §12's
   labeled-row exclusion): paints after the touch target in `TypeRole.BODY_MEDIUM` with a 4px
   trailing pad, widens the preferred size, **extends the click target**, truncates with an
   ellipsis, and provides the accessible name. Constructors `ElwhaRadioButton(String)` and
   `(String, boolean)` added. `setAccessibleLabel(String)` is the label-less override (the
   `aria-label` analogue); the name chain is explicit → accessibleLabel → label → `labelFor`.
2. **48px leading touch-target block** (supersedes §5's 40×40 state-layer box): the icon centers in
   a `TOUCH_TARGET` (48px) block pinned to the leading edge (mirrored under RTL); preferred size is
   the block, widened by the label. The 40px state layer is unchanged and stays concentric.
3. **`FocusVisible`-gated, single state layer** (supersedes §6's stacking + any-focus rule):
   pressed > hovered > focus-visible, one layer at a time — the checkbox/button-family policy.
   Group arrow navigation requests focus with a traversal cause so keyboard arrival lights the
   focus treatment.
4. **`doClick()`** — the user-equivalent select for tests/automation (checkbox parity).
5. **Not aligned, operator ruling requested:** the event idiom — checkbox fires
   `PROPERTY_CHECK_STATE` property changes where radio/slider/switch fire `ChangeListener`s
   (checkbox is the lib-wide outlier); and the merged `ElwhaSwitch`'s `setLabel`, which remains
   accessible-only — both flagged at handoff as potential follow-up issues.
