# ElwhaSwitch — Phase 0 Design

**Status:** LOCKED (S1 spike confirms §2). **Drafted:** 2026-06-10. **Author:** Charles Bryan (`cfb3@uw.edu`).
**Parents:** epic [#401](https://github.com/OWS-PFMS/elwha/issues/401). **Milestone:** `v0.4.0`.
**Research:** [`elwha-switch-research.md`](elwha-switch-research.md) — the captured spec; this doc holds only the decisions.

---

## TL;DR — the locked decisions

1. **One dedicated `ElwhaSwitch extends JComponent`** — custom paint, hand-wired input + a11y; **not** a styled `JToggleButton`/`ButtonUI`. [RECOMMENDED; locked via S1 spike — §2]
2. **Package `com.owspfm.elwha.switches`** — `switch` is a Java reserved word (MDC-Android precedent: `materialswitch`). §2.
3. **No variant / size enums.** The M3 switch spec defines none — one 52×32 form. Configurations: icons (none / both / selected-only) + enabled/disabled. §1.
4. **Zero new theme tokens** — full color/opacity/shape map onto `ColorRole` / `StateLayer` / `ShapeScale.FULL` (research §Tokens). §4.
5. **Geometry (px=dp):** track 52×32 corner-full; handle 16 → 24 (selected) → 28 (pressed) → 24 (icons shown); icon 16; state layer 40 circle riding the handle; handle-center travel 16→36. Preferred size **60×40** (state-layer-inclusive box, track centered). **No `getMaximumSize` override** (the #199 trap). §5.
6. **State boolean, not a Swing model.** `selected` is a plain field + `ChangeListener` (any change) + `ActionListener` (user commits only — click/Space/drag release). No `ButtonModel`, no `ButtonGroup` interop. §2.
7. **Motion:** one selection `MorphAnimator` (300ms, overshoot bezier `(0.175, 0.885, 0.32, 1.275)` as a component-local `Easing` constant) drives slide + color crossfade + icon fade; a second retargetable size animator (250ms standard / 100ms linear when press-driven) drives handle diameter. Reduced-motion snaps globally via `MorphAnimator`. §6.
8. **Color crossfade tied to slide progress** — accepted deviation from M3's 67ms color snap (research §Open-2); revisit at smoke.
9. **Drag-to-toggle** ships in V1: handle scrubs the 20px run with the pointer, commit by nearest half on release. §7.
10. **Icons:** `setIconsVisible` / `setShowOnlySelectedIcon` / custom `setSelectedIcon`/`setUnselectedIcon`, defaults `MaterialIcons.check(16)` / `close(16)`; check rotates −45°→0 into view in selected-only mode. §3, §6.
11. **a11y = the `JToggleButton` shape:** `AccessibleRole.TOGGLE_BUTTON` + `AccessibleState.CHECKED` + `AccessibleAction` + `AccessibleValue(0/1)`; name via `setLabel` / `labelFor`. RTL mirrors via `ComponentOrientation`. §8.
12. **V1 = one phase, six stories** (S1 spike → interaction → motion → icons → a11y/RTL → Showcase). §11.

## §1. Scope — what V1 ships

- ✅ The complete M3 switch: anatomy, all states (enabled/hover/focus/pressed × selected/unselected, disabled × selected/unselected), motion, icons modes, drag, keyboard, a11y, RTL, dark mode.
- ✅ Showcase Workbench + Gallery + sidebar entry + headless smoke; CHANGELOG entry.
- ❌ Labeled switch-row composite (M3 *list item* pattern, not switch anatomy) — §10.
- ❌ `ItemListener`/`ButtonModel`/`ButtonGroup` interop — §10.
- **No excluded M3 variants exist** — the spec has exactly one switch; nothing is stubbed or cut (research §E).

## §2. Architecture — the load-bearing decision [RECOMMENDED; locked via S1 spike]

**One dedicated `ElwhaSwitch extends JComponent` in `com.owspfm.elwha.switches`,** painting all chrome, with input (mouse click/drag, Space) and a11y hand-wired. State is a plain `boolean selected` + listener lists.

**Why not `JToggleButton` + custom `ButtonUI`?** The only thing `ButtonModel` would buy is armed/pressed bookkeeping and `ButtonGroup` interop — and switches are never radio-grouped. Against that: `BasicToggleButtonUI`'s text/icon layout model fights the track-and-riding-handle anatomy completely (nothing of the button's paint survives), the handle **drag** gesture has no `ButtonModel` vocabulary at all, and FlatLaf's button styling would have to be actively suppressed. The interaction surface is *finite and fully captured* (research §B): click, drag, Space — the same calculus that locked the slider's §2. Mirrors `ElwhaSlider` / `ElwhaNavRailDestination` precedent.

**S1 spike proves:** static chrome paints token-correct in light+dark for all four static state cells (selected/unselected × enabled/disabled), the 60×40 halo-inclusive preferred size lays out in `FlowLayout`/`GridBagLayout` without clipping the state layer, and the selected-state API round-trips with `ChangeListener` events. Fallback documented here if the spike fails (none anticipated — this is the slider's proven path).

## §3. Anatomy / primitive

Three painted parts (M3 nouns — research §C; "handle", never "thumb"):

1. **Track** — 52×32 corner-full capsule. Unselected: `SURFACE_CONTAINER_HIGHEST` fill + **2px `OUTLINE` border painted inside the bounds**. Selected: `PRIMARY` fill, no border.
2. **Handle** — circle riding the track's vertical center, diameter per state (§5), corner-full.
3. **Icon** (optional) — 16px, centered on the handle. Defaults: `MaterialIcons.check(16)` selected / `MaterialIcons.close(16)` unselected; consumer-replaceable per side.

Plus the non-anatomy **state layer**: 40px circle concentric with the handle (paints under it), and the press **ripple** bounded to the same circle via `RipplePainter`.

## §4. Tokens & color [zero new tokens — LOCKED]

The full mapping is research §Tokens / §T; load-bearing rows:

| Part | Unselected | Selected |
|---|---|---|
| Track | `SURFACE_CONTAINER_HIGHEST` + 2px `OUTLINE` | `PRIMARY` |
| Handle | `OUTLINE` (hover/focus/pressed → `ON_SURFACE_VARIANT`) | `ON_PRIMARY` (hover/focus/pressed → `PRIMARY_CONTAINER`) |
| Icon | `SURFACE_CONTAINER_HIGHEST` | `ON_PRIMARY_CONTAINER` |
| State-layer tint | `ON_SURFACE` | `PRIMARY` |

Disabled (no layers, no motion): track = (`SURFACE_CONTAINER_HIGHEST` unsel / `ON_SURFACE` sel) @ `StateLayer.disabledContainerOpacity()` (0.12, outline included); handle = `ON_SURFACE` @ `disabledContentOpacity()` (0.38) unselected / **opaque `SURFACE`** selected (the ⚠️ asymmetry — gallery shows both); icons @ 0.38 over their state's base role.

All resolved **at paint time** (`ColorRole.resolve()` / `StateLayer.opacity()` — the binding rule, no caching).

## §5. Measurements & geometry

| Quantity | Value |
|---|---|
| Track | 52×32, corner-full |
| Track outline (unselected only) | 2px, inside |
| Handle diameter | 16 unselected · 24 selected · **28 pressed (either side)** · 24 whenever its icon shows |
| Icon | 16 |
| State layer | 40 circle, concentric with handle |
| Handle-center x (LTR) | 16 unselected → 36 selected (travel 20) |
| Preferred size | **60×40** — the state-layer-inclusive box; track centered at (4,4) |
| Touch target | 48 — [DOC] guidance; whole component bounds are interactive |

`getPreferredSize`/`getMinimumSize` return 60×40; **never** override `getMaximumSize` to preferred (#199/#200 doctrine). The component paints centered within whatever bounds the layout grants (track block centered both axes).

## §6. States & motion

**Static layers:** hover (`StateLayer.HOVER`) and focus (`StateLayer.FOCUS`) paint the 40px circle in the state's tint role; both can stack (max-opacity rule: paint hover and focus independently like the slider does). Pressed paints `StateLayer.PRESSED` + the `RipplePainter` ripple from the press point, clipped to the circle. Gallery hooks `setHovered(boolean)` / `setPressed(boolean)` (slider precedent) force the treatments for static rendering.

**Motion (research §Mo):**

| Animation | Driver | Duration / easing |
|---|---|---|
| Handle slide (+ color crossfade + icon fade) | `slideAnimator` — selection `MorphAnimator`, 0=unselected→1=selected | `MEDIUM2_MS` 300ms; position eased by component-local `OVERSHOOT = Easing.cubicBezier(0.175f, 0.885f, 0.32f, 1.275f)`; color/icon alpha by `Easing.LINEAR` on the same progress |
| Handle diameter | `sizeAnimator` — retargeting lerp `fromDiameter→toDiameter` | 250ms `Easing.STANDARD` for selection-driven changes; **100ms `Easing.LINEAR`** for press-grow/release |
| Ripple | `Timer` per `RipplePainter` contract | 400ms (painter's curve) |

- A drag in flight **owns the slide position** (progress = pointer fraction of the 20px run, clamped); release hands back to `slideAnimator` from the current progress.
- Disabled or not-displayable ⇒ `snapTo` (no tween). Reduced-motion: `MorphAnimator` global snap behavior, free.
- Icon motion: both-icons mode = plain crossfade (alpha by progress). Selected-only mode = check fades **and rotates −45°→0** as progress rises.
- **Deviation [accepted]:** M3 webs snap colors in 67ms; we crossfade along the slide so a scrubbed drag previews color continuously. Revisit at smoke.

## §7. Behaviors & keyboard

- **Click** (press+release within bounds, no drag): toggle, animate, fire `ActionListener` + `ChangeListener`.
- **Drag:** press on/near the handle then move ⇒ scrub; release commits to the nearest half (handle-center vs track midpoint). A release that lands back on the starting side still fires `ChangeListener`? — no: if the state didn't change, no events fire; the handle animates home.
- **Space:** key-press shows pressed treatment (grow to 28), key-release toggles + fires (Swing button semantics). Enter not bound (research §B).
- **`setSelected(boolean)`:** animates when showing, snaps when not displayable; fires `ChangeListener` only on actual change; **never** fires `ActionListener` (user-gesture event only — material-web parity).
- **RTL** (`ComponentOrientation` not left-to-right): selected rests left; travel, drag math, and halves all mirror.
- Disabled: no listeners react, cursor default, no layers.

## §8. Accessibility

`AccessibleElwhaSwitch extends AccessibleJComponent implements AccessibleAction, AccessibleValue` — the `JToggleButton` accessible shape (Swing has no SWITCH role):

- Role `AccessibleRole.TOGGLE_BUTTON`; `AccessibleState.CHECKED` in `getAccessibleStateSet()` while selected; state-change `PropertyChangeEvent` (`ACCESSIBLE_STATE_PROPERTY`) on toggle.
- `AccessibleAction`: one action, "click", performs the user-gesture toggle (fires both listener kinds).
- `AccessibleValue`: 0/1, min 0, max 1; `setCurrentAccessibleValue` maps to `setSelected`.
- Name: `setLabel(String)` (mirrors slider) → `getAccessibleName()`; a `JLabel.setLabelFor(theSwitch)` association is honored as the fallback name source. Research §A: a switch **always** needs a label — Javadoc says so on the class.

## §9. Showcase pattern

`SwitchShowcasePanels` (showcase package) + registration in `ElwhaShowcase` under Components as **"Switch"**, the `LeafEntry` + Workbench/Gallery `JTabbedPane` shape the slider uses:

- **Workbench:** stage hosts one live `ElwhaSwitch`; controls — Selected, Icons visible, Show only selected icon, Custom icons (star/star-filled demo pair), Label text, Enabled, RTL; code panel re-renders the construction snippet per state. Dogfood note: Workbench controls remain plain `J*` per the established Workbench convention.
- **Gallery:** matrix rows = Basic / Icons / Selected-icon-only / Custom icons; columns = Unselected, Selected, Hover, Focused (tab-to), Pressed, Disabled unselected, Disabled selected — via the `setHovered`/`setPressed` hooks. The two disabled columns exist because of the §4 ⚠️ asymmetry.
- `ElwhaSwitchShowcaseSmoke` headless guard: panels construct, gallery renders to a `BufferedImage` without throwing, workbench controls apply.

## §10. Out of scope (documented, not cut)

- **Labeled switch row** (text + trailing switch line item) — M3 *list item* pattern; consumers compose. Becomes interesting when an Elwha List epic exists.
- **`ItemListener` / `ButtonModel` / `ButtonGroup` interop** — no M3 or consumer need; `ChangeListener`+`ActionListener` are the event surface.
- **Materialised 48px touch-target padding** — [DOC] guidance on desktop; revisit if a touch consumer appears.

## §11. Phasing → stories (Phase 1 = V1, single phase)

- **S1 — architecture spike + static chrome** (#402) — `ElwhaSwitch` skeleton, §2/§3/§4/§5 static paint (selected/unselected × enabled/disabled, no icons), selected API + `ChangeListener`, 60×40 preferred size, light+dark token-correct. *Locks §2.* Demo: `ElwhaSwitchChromeDemo`; guard: `ElwhaSwitchChromeSmoke`.
- **S2 — interaction & state layers** (#403) — click toggle, Space press/release, drag-to-toggle commit, hover/focus/pressed layers + press ripple, pressed 28px (static snap this story), `ActionListener` semantics, `setHovered`/`setPressed` gallery hooks. §6-static/§7. Demo: `ElwhaSwitchInteractionDemo`; guard: `ElwhaSwitchInteractionSmoke`.
- **S3 — motion** (#404) — slide animator + overshoot, retargeting size animator (250/100ms), color crossfade, drag scrubbing→handoff, reduced-motion + non-displayable snap. §6. Demo: `ElwhaSwitchMotionDemo`; guard: `ElwhaSwitchMotionSmoke`.
- **S4 — icons** (#405) — `setIconsVisible` / `setShowOnlySelectedIcon` / custom icon slots, 24px with-icon handle, icon crossfade + −45° rotate-in, disabled icon treatments. §3/§6. Demo: `ElwhaSwitchIconsDemo`; guard: `ElwhaSwitchIconsSmoke`.
- **S5 — a11y, label & RTL** (#406) — `AccessibleElwhaSwitch` (§8), `setLabel`/`labelFor` naming, RTL mirroring incl. drag math, focus traversal polish, disabled guards audit. Demo: `ElwhaSwitchA11yRtlDemo`; guard: `ElwhaSwitchA11ySmoke`.
- **S6 — Showcase + CHANGELOG** (#407) — §9 panels + registration + `ElwhaSwitchShowcaseSmoke`; CHANGELOG `[Unreleased]` entry. *Completes V1; closes the epic.*

### S1 spike outcome (2026-06-10)

Confirmed — §2 locked as built. `ElwhaSwitch extends JComponent` with a plain `selected` boolean paints all four static cells token-correct in light **and** dark (27-check `ElwhaSwitchChromeSmoke`, pixel-asserted over a `SURFACE` ground including the disabled blends and the opaque-`SURFACE` disabled-selected handle). §12-1 resolved: the unselected outline is a **filled ring via `Area` subtraction**, not a stroke — ring and interior tile exactly, no seam, and the two disabled 12% fills never overlap so nothing double-blends. The 60×40 halo-inclusive preferred size lays out cleanly in `FlowLayout`/`GridLayout` rows (`ElwhaSwitchChromeDemo`). No fallback needed.

## §12. Open for the S1 spike

1. Whether the 2px outline renders crisply inside the capsule at 1× without a half-pixel seam (stroke-inside vs inset-fill — pick whichever survives both light/dark).
2. Whether the overshoot bezier's >1 excursion needs clamping at the travel ends (the handle may overshoot past x=36 by design — confirm it stays inside the track visually; M3's overshoot does).
3. Exact drag threshold (px of movement before a press becomes a drag rather than a click) — propose 4px, tune at smoke.
