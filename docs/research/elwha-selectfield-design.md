# ElwhaSelectField — Design Decisions

**Status:** **Phase 0 — design draft for review.** Decisions below are proposed; the load-bearing **composition-boundary** call (§2) is locked by the first implementation story's spike. No stories built until this doc is approved (the Phase-1 stories are filed against it).

**Drafted:** 2026-06-07. **Author:** Charles Bryan (`cfb3@uw.edu`).

**Parents:**
- [`elwha-selectfield-research.md`](elwha-selectfield-research.md) — the M3 exposed-dropdown composition spec this doc decides against. **Read it for any anatomy/behavior/a11y detail; this doc references rather than duplicates it.**
- [`elwha-textfield-design.md`](elwha-textfield-design.md) — the field this composes (V1 shipped); the design-doc shape + the **S1-spike-locks-the-architecture** precedent this epic mirrors.
- [`elwha-menu-design.md`](elwha-menu-design.md) — the menu this composes (V1 shipped, incl. `SelectionMode`).
- [M3 Menus → exposed dropdown](https://m3.material.io/components/menus/guidelines) + Material Web `select.md`.

**Epic:** [#331](https://github.com/OWS-PFMS/elwha/issues/331) (V1). **Composes:** [#286](https://github.com/OWS-PFMS/elwha/issues/286) (`ElwhaTextField`) + [#298](https://github.com/OWS-PFMS/elwha/issues/298) (`ElwhaMenu`) — **both V1-complete on `main`**, so this epic is unblocked.

**Milestone:** v0.4.0 (the active dev milestone).

---

## TL;DR — the locked decisions

1. **What it is:** `ElwhaSelectField<T>` — the M3 **exposed dropdown menu**: a text field with a trailing dropdown-arrow that opens an anchored menu of typed options; choosing one writes back to the field. The first-class *select* Elwha lacks. **Pure composition** of two shipped primitives — it paints nothing of its own.
2. **Architecture (load-bearing, §2):** a **new dedicated `ElwhaSelectField<T> extends JComponent`** that **composes (HAS-A)** an embedded `ElwhaTextField` (chrome/label/slots/states/a11y) and **builds an `ElwhaMenu` on open** for the option list. **Not** a mode bolted onto `ElwhaTextField`, **not** a subclass of it. Mirrors the lib's dedicated-primitive doctrine. **Locked by the §11 S1 spike.**
3. **Generic over the option type `T`** — `setOptions(List<T>)` + a `Function<T,String>` display renderer, `getSelectedValue()` / `setSelectedValue(T)`. Mirrors the `ElwhaList<T>` / `ElwhaCardList<T>` generic-container doctrine. Non-`String` options (enums, domain records) are the norm; string-only would be a regression.
4. **Zero new theme tokens (LOCKED).** Both primitives are already token-native and paint themselves; the select field adds **no paint**. The only new visual asset is the **dropdown-arrow glyph** (a bundled Material Symbol, `arrow_drop_down` / `arrow_drop_up`) and its open/closed rotation — an icon, not a token.
5. **V1 = non-editable (pure select).** The embedded field is **read-only** (`setReadOnly(true)`); the user cannot type — the field + arrow are the open/close affordance, and picking a menu item is the only way to set the value. **Editable filter-as-you-type combo and multi-select are later phases** (§10/§11).
6. **The trailing affordance is an `ElwhaIconButton`** (the shipped `setTrailingIconButton` slot) carrying the **dropdown arrow**, which **rotates down→up when the menu opens** (reduced-motion → instant). It brings free Button a11y and is the keyboard/click toggle.
7. **Selection write-back (LOCKED):** the menu is built with `SelectionMode.SINGLE`; choosing an item fires `Builder.onSelectionChange(...)` → the select field sets the field text to the option's display string, closes the menu, marks the item `selected` for the next open, and fires the select field's own `SelectionChangeListener`. Single source of truth = the selected `T`.
8. **Anchored popup (LOCKED):** `ElwhaMenu.open(anchor)` with the **field as the anchor** (menu hangs below-start, flips above near the viewport edge, light-dismisses, restores focus to the field) — all shipped behavior, reused as-is. Menu width tracks the field width.
9. **States (LOCKED):** the field's full set — enabled / hover / focus / error / disabled / read-only — all **delegate to the embedded `ElwhaTextField`**, plus the **expanded vs collapsed** menu state (arrow direction + a11y). `setError`/`setEnabled` propagate to the whole control.
10. **A11y (LOCKED):** the M3/ARIA **combobox** pattern, approximated in Swing — the field stays a read-only text component, the **arrow `ElwhaIconButton`** carries the expanded/collapsed state in its accessible name + fires an accessible state-change announcement on open/close (the one Swing gap, mirroring how `ElwhaTextField` engineered error→alert); the option items keep their `MENU_ITEM` roles. (research §A11y)
11. **V/Phase split:** **V1 (#331)** = the non-editable select (Phase 1) + editable combo (Phase 2) + multi-select (Phase 3). No follow-on epic foreseen. See §11.

---

## §2. Architecture — the load-bearing decision [RECOMMENDED; lock via S1 spike]

**`ElwhaSelectField<T> extends JComponent` that COMPOSES an embedded `ElwhaTextField` + an on-demand `ElwhaMenu`.**

- **Composition, not inheritance.** The select field **owns** an `ElwhaTextField` (added as its single child, filling its bounds) and **builds an `ElwhaMenu` when opened**. It does *not* extend `ElwhaTextField` (which would inherit a typing surface a pure-select must suppress) and is *not* a `selectMode` flag on `ElwhaTextField` (which would bloat the field's API with menu/option concerns and a generic `<T>` it doesn't want). The field stays a clean single-responsibility primitive; the select field is a clean composition.
- **The field is read-only.** `embeddedField.setReadOnly(true)` — non-dimmed chrome, text selectable/copyable, not editable. The select field intercepts mouse-press on the field and the trailing arrow to toggle the menu.
- **The arrow is the field's trailing icon button.** `embeddedField.setTrailingIconButton(arrowButton)` where `arrowButton` is an `ElwhaIconButton(MaterialIcons.arrowDropDown())`. Its action toggles the menu; the select field rotates/swaps the glyph on open/close.
- **The menu is built per-open** from the current options (`SelectionMode.SINGLE`, the field as anchor), or built once and reused — the **S1 spike decides** build-per-open vs cache-and-rebuild-on-options-change (lean: rebuild only when options change, to preserve the `selected` marks cheaply).

**Why not extend `ElwhaTextField`?** A subclass inherits the editing/IME surface a non-editable select must fight, and couples the select's `<T>` option model into the field. **Why not a from-scratch field?** Re-deriving the field chrome/label/a11y is exactly what #286 already solved — compose it. **Why a dedicated class at all (not a `JComboBox`)?** `JComboBox` fights its UI delegate for the M3 chrome and can't host the `ElwhaMenu`/`ElwhaTextField` pairing cleanly — the same reasoning that made `ElwhaMenu` a dedicated primitive over `JPopupMenu`.

**S1 spike (first story):** prototype `ElwhaSelectField<T>` — embed a read-only `ElwhaTextField`, attach the arrow `ElwhaIconButton`, open an anchored `ElwhaMenu` of options on click/keyboard, prove (a) the **write-back round-trip** (pick item → field shows it → reopen shows it `selected`), (b) the menu **anchors + flips + light-dismisses** off the field, (c) **focus returns** to the field on close, (d) the **arrow toggles + rotates**, (e) it composes under both field `Variant`s. Documented fallback: if the trailing-button slot can't host an arrow that both toggles and rotates cleanly, paint the arrow in the select field and reserve the slot (heavier, documented). Mirrors the dialog/textfield S1-locks-the-architecture precedent.

## §3. Anatomy / slots

The exposed-dropdown anatomy = **the text-field anatomy + one trailing arrow + the popup** (research §"What M3 specifies"). The select field exposes the field's slots by delegation — `setLabel` (via constructor), `setLeadingIcon`, `setSupportingText`, `setPlaceholder`, `setError`/`setErrorText` — and **owns** the trailing slot (the arrow; a consumer-set trailing icon is rejected/ignored, documented). Options are a typed model, not a slot.

## §4. Tokens & color

**Zero new tokens** (research §"Composing API"). The embedded `ElwhaTextField` and the `ElwhaMenu` resolve every color/type/shape/space themselves. The arrow glyph auto-themes via `MaterialIcons` (the shared `Label.foreground` filter). The only additions to the repo are the bundled `arrow_drop_down` Material Symbol SVG(s) and a `MaterialIcons.arrowDropDown()` accessor (icon plumbing, not a token).

## §5. Layout / measurements

The control's preferred size **is the embedded field's** (the field fills the select field's bounds; the arrow lives in the field's reserved trailing slot — no extra width). The menu **matches the field width** and opens below-start per `ElwhaMenu` anchoring (research §M: the 32 dp option-row is the menu's own metric). No new redlines — the field and menu carry theirs.

## §6. States & motion

States delegate to the embedded field (§TL;DR 9). The one select-specific motion is the **arrow rotation** (down ↔ up over the menu's open/close), honoring reduced-motion (instant swap). The menu's entrance/exit motion is its own.

## §7. Selection & value model

**Single source of truth = the selected `T`.** `setOptions(List<T>)` + `setDisplayFunction(Function<T,String>)` (default `String::valueOf`); `getSelectedValue()` / `setSelectedValue(T)` (sets the field text + the menu's `selected` mark); `addSelectionChangeListener(Consumer<T>)`. Internally the menu is built `SelectionMode.SINGLE` and its `onSelectionChange` is the write-back path. No validation engine — a select is inherently constrained to its options.

## §8. Accessibility

The ARIA **combobox** pattern (research §A11y), approximated in Swing: the embedded field stays a read-only `Textbox` whose accessible name is the label; the **arrow `ElwhaIconButton`** is the interactive node — its accessible name encodes the **expanded/collapsed** state (e.g. *"Open options"* / *"Close options"*) and it fires an accessible state-change announcement on toggle (the one Swing gap to engineer, mirroring the field's error→alert). The option list keeps the menu's `MENU_ITEM` roles + `selected` state. Keyboard parity (§ below) makes it operable without a pointer.

**Keyboard (field focused, menu closed):** **Down / Enter / Space / Alt+Down** open the menu and move focus into it; **Esc** closes + returns focus to the field; **type-ahead** matches an option. Open menu = the shipped `ElwhaMenu` keyboard map; commit closes + refocuses the field.

## §9. Showcase pattern

A select field **is** an embeddable surface (the popup aside), so it fits the standard `ComponentWorkbench` + `Gallery`: a live select on the stage with variant / option-count / error / supporting / placeholder controls + a live "selected value" readout, and a Gallery matrix (variant × state, a long list that scrolls, a pre-selected value). **Dogfood Elwha components** in the controls ([[feedback_dogfood_elwha_components]]).

## §10. Out of scope (filed, not silently cut)

- **Editable / filter-as-you-type combo** — typing filters the option list (`listbox`/`option`); **Phase 2**, not cut.
- **Multi-select** — `SelectionMode.MULTI` with a chip/summary display in the field; **Phase 3**, not cut.
- **Async / remote option loading**, **formatted/typed-value parsing**, **grouped/sectioned options beyond the menu's `addGroup`** — documented deferrals, consumer- or future-owned.
- **`JComboBox` drop-in compatibility** — out; this is a dedicated M3 primitive.

## §11. Phasing → stories

- **S1 — Spike + composition skeleton (§2).** `ElwhaSelectField<T>` embedding a read-only `ElwhaTextField`, the arrow `ElwhaIconButton`, an anchored `ElwhaMenu` on open; prove write-back round-trip + anchor/flip/dismiss + focus-return + arrow-toggle, both variants. *Locks §2.*
- **S2 — Option model + single-select write-back (§7).** `setOptions(List<T>)` + display function, `SelectionMode.SINGLE`, choose→set-value→close→`selected`-on-reopen; `getSelectedValue`/`setSelectedValue` + `SelectionChangeListener`.
- **S3 — Keyboard + expanded/collapsed a11y (§8).** Down/Enter/Space/Alt+Down open, Esc close + focus-return, type-ahead; arrow rotation (reduced-motion); the combobox a11y wiring (expanded-state name + announcement).
- **S4 — Variant delegation + state propagation + arrow polish (§3/§6).** `FILLED`/`OUTLINED` delegation, `setError`/`setEnabled`/`setReadOnly` + label/supporting/placeholder/leading-icon passthrough, the open/closed arrow affordance.
- **S5 — Showcase leaf + docs (§9).** Workbench + Gallery; `selectfield/README.md`, CHANGELOG, Javadoc (`@author`/`@version`/`@since`), dogfood pass (a genuine Showcase/playground select site).

**Phases:**
- **Phase 1 = S1–S5** → ships the complete **non-editable select** (typed options, both variants, write-back, keyboard + a11y, Showcase, docs).
- **Phase 2 = editable / filter-as-you-type combo** (file when Phase 1 lands).
- **Phase 3 = multi-select** (`SelectionMode.MULTI` + summary display; file when Phase 2 lands).

## §12. Open for the S1 spike / Phase 0 sign-off
- The **menu lifecycle** — build-per-open vs cache-and-rebuild-on-options-change (lean: rebuild on options change; preserves `selected` marks cheaply). S1 decides.
- The **arrow mechanism** — rotate-in-the-trailing-button vs paint-in-the-select-field (lean: the trailing `ElwhaIconButton`, swapping `arrow_drop_down`↔`arrow_drop_up`). S1 confirms.
- **Name** — `ElwhaSelectField` (recommended) vs `ElwhaDropdownField` / `ElwhaExposedDropdown`. Confirm at sign-off.

## S1 spike outcome (Phase 1, #374 — 2026-06-07) — architecture LOCKED

The §2 composition is proven and locked. `ElwhaSelectField<T> extends JComponent` owns a read-only `ElwhaTextField` (added `BorderLayout.CENTER`, so `getPreferredSize` delegates to the field) and a trailing-slot `ElwhaIconButton` carrying the dropdown arrow; it builds an `ElwhaMenu` (`SelectionMode.SINGLE`, the field as anchor) for the options. No subclassing, no `selectMode` flag. The §12 questions resolved:

1. **Name = `ElwhaSelectField`** (confirmed) — package `com.owspfm.elwha.selectfield`.
2. **Menu lifecycle = cache-and-rebuild-on-options-change** (the lean). The built menu is cached; `setOptions` / `setDisplayFunction` null it so the next open rebuilds. Picking via the menu's `SelectionMode.SINGLE` keeps the `selected` mark on the cached items across reopens — no rebuild on a plain pick.
3. **Arrow = the trailing `ElwhaIconButton`, glyph-swap** `arrow_drop_down` ↔ `arrow_drop_up` (the lean). Rotation + reduced-motion is **S3** (#376); S1 ships the swap. Sized `IconButtonSize.S` (32 dp container, 20 dp glyph) in the field's trailing slot.
4. **New assets (sanctioned by §4):** bundled `arrow_drop_down.svg` + `arrow_drop_up.svg` Material Symbols + `MaterialIcons.arrowDropDown()` / `arrowDropUp()` accessors. **Zero new theme tokens** (held).

**Toggle-reopen guard (finding).** The shared overlay's outside-press listener treats the trigger (field + arrow) as *outside* the surface, so pressing the trigger while open begins a light-dismiss. An `expanded` flag — set on open, cleared in the menu's `onClose` (which fires after the animated teardown, so it stays `true` through the dismissing click) — makes the same click read as a close, not a re-toggle. The whole field body and the arrow both toggle (a `mousePressed` on the field + its editor, plus the arrow's action).

**Menu width-tracking (deferred, not cut).** `ElwhaMenu`/`Builder` exposes no width hook today, so the popup opens at its intrinsic content width rather than matching the field (design §5). Not forced in S1; revisit in S4 (arrow/affordance polish) or as a small `ElwhaMenu` enhancement if the visual gap warrants — recorded here rather than silently dropped.

**Headless testability (finding).** `ElwhaMenuItem.activate(int)` is package-private to `menu`, and the popup needs a window — so the round-trip is driven headlessly through a package-private `ElwhaSelectField.selectIndex(int)` (the shared selection seam; S2's public `setSelectedValue` builds on it). Window-dependent behavior (anchor/flip/light-dismiss/focus-return/arrow flip) is exercised by the interactive `SelectFieldS1SpikeDemo`.

## Phase 1 complete (#374–#378 — 2026-06-07) — non-editable select shipped

All five stories built on one stacked branch: S1 skeleton (#374) → S2 typed value model + listeners (#375) → S3 combobox keyboard + expanded/collapsed a11y + arrow rotation (#376) → S4 variant delegation + state propagation + owned trailing slot (#377) → S5 Showcase leaf + docs (#378). Five fresh demos + five headless guards (66 checks total). Zero new theme tokens held; the only new assets are the two arrow glyphs + their `MaterialIcons` accessors.

**Dogfood pass (S5) — documented skip.** The story asked to swap a genuine Showcase/playground `JComboBox`/hand-rolled select onto `ElwhaSelectField`. None is a natural fit, so it is **skipped, not forced** (per the story's explicit allowance): the prominent labeled select in the Showcase is the **toolbar palette picker**, but it is load-bearing chrome with a custom `ListCellRenderer`, a dynamic tier-switch `setModel`, and a `pickerAdjusting` re-entry guard, and it sits in a compact toolbar where an `ElwhaSelectField`'s floating-label + reserved-supporting-row anatomy (~56 dp) would break the toolbar height. The remaining Showcase combos are dense control-panel enum pickers in tight control columns — the same form-field-too-tall mismatch. The Select Field leaf's own Workbench instead dogfoods Elwha controls (button-group variant picker, `SELECTABLE` toggles, icon-button steppers, text-field inputs). The editable-combo Phase 2 (a toolbar-friendly filter-as-you-type form) is the natural future home for a real swap.

**Later phases (progressive filing):** Phase 2 (editable / filter-as-you-type combo) and Phase 3 (multi-select + summary display) are filed when Phase 1 lands. Epic #331 stays open until the final V1 phase ships.

## Phase 2 complete (#391–#394 — 2026-06-10) — editable combo shipped

All four stories built on one stacked branch: S1 editable spike (#391) → S2 filter-as-you-type (#392, `ElwhaMenu.setVisibleItems` live in-place filter + the "No matches" placeholder) → S3 value model + keyboard + a11y (#393, free-text policy, Enter/Esc/focus-loss commit semantics, `moveHighlight`/`activateHighlighted` routing, editor-side combobox a11y) → S4 Showcase + docs (#394). Four fresh demos + four headless guards. Zero new theme tokens held; zero new assets (Phase 2 is pure behavior).

**Dogfood pass (Phase 2 S4) — documented skip, again.** The story asked to revisit swapping the Showcase toolbar palette picker (`JComboBox`) now that an editable combo exists. Re-evaluated and **still skipped, not forced**: the Phase-1 blockers were never about typeability — they are (1) the picker's per-option **swatch icons** (custom `ListCellRenderer`); `ElwhaSelectField` has no per-option icon API (its display function is `Function<T,String>`), so the swap would silently drop the palette swatches or require inventing a per-option-icon API outside this story's scope; and (2) the form-field anatomy (floating label + reserved supporting row, ~56 dp) still breaks the compact toolbar height — editable mode does not change the chrome. The natural unlock is a future per-option leading-icon hook + a dense/toolbar density variant; until then the Showcase's Select Field leaf and the Phase-2 filtering gallery are the genuine in-repo dogfood sites.

**Phase 3 (final V1 phase, files when Phase 2 lands):** multi-select — `SelectionMode.MULTI` + a summary display in the field; its PR `Closes #331`.

## Phase 2 S1 spike outcome (#391 — 2026-06-10) — editable architecture LOCKED

`setEditable(boolean)` (default `false` = the shipped pure select) lifts the embedded field's read-only behind the opt-in; the select-level `setReadOnly` re-imposes it in either mode (`field.setReadOnly(!editable || readOnly)`). The coexistence questions the story asked to decide:

1. **Focus model = focus stays in the editor (ARIA combobox).** The menu gains a `Builder.focusHome(Component)` axis: when set, opening does not move focus onto the menu surface (`initialFocusTarget()` → the focus home) and focus changes / mouse presses within the focus home's hierarchy do not light-dismiss. Implemented as a new `ownsFocus(Component)` strategy hook on `AbstractElwhaOverlay` (default = descends-from-surface; `ElwhaMenu` widens it with the focus home) consulted by both the focus-escape and outside-press listeners. The select field passes `field.getEditor()` as the focus home when editable.
2. **Open gestures (editable):** typing a letter/digit opens (the character lands in the editor — no consume, no type-ahead forwarding), Down / Alt+Down opens, the trailing arrow toggles. Enter / Space / Up do **not** open — they are text keys in a typeable editor. A press in the field places the caret (a press on the field chrome focuses the editor); only the arrow toggles by pointer. Pure-select gestures are untouched.
3. **Keystroke routing while open:** printable keys keep landing in the editor (S2 turns them into the live filter). The menu surface never holds focus, so its own Up/Down/Enter bindings are inert; S3 routes highlight navigation from the editor explicitly. Esc already closes via the surface's `WHEN_IN_FOCUSED_WINDOW` binding.
4. **Arrow close = the Phase-1 light-dismiss path.** The arrow sits outside the focus home (the editor), so pressing it while open still outside-press-dismisses and the existing toggle-reopen guard makes the same click read as a close — no new mechanism. Presses on the field chrome (outside the editor) likewise still light-dismiss; recorded as acceptable combobox behavior (the editor is the input surface).
5. **`ElwhaMenu.close()`** (public, `PROGRAMMATIC` cause) added for owners that manage the menu lifecycle — a mode flip closes an open menu, and S3's free-text Enter commit needs it.
6. **Zero new theme tokens (held).** All Phase 2 additions are behavior; no new paint, no new assets.

## Phase 3 S1 spike outcome (#397 — 2026-06-10) — multi-select architecture LOCKED

`setMultiSelect(boolean)` (default `false` = the shipped single select / editable combo) lifts the single-selection constraint behind the opt-in. The menu side needed **nothing new**: `SelectionMode.MULTI` (toggle-without-closing, reserved check column, checkbox `CHECKED` a11y) shipped with epic #298 and is reused as-is — the spike is select-field-side model + wiring. The coexistence questions the story asked to decide:

1. **Opt-in shape = `setMultiSelect(boolean)` / `isMultiSelect()`** (the story's lean, adopted). When on, `rebuildMenu()` builds `SelectionMode.MULTI` and routes `onSelectionChange` to the multi toggle path; toggling never closes, Esc / outside-press / the arrow close (all existing menu paths — `keepOpen` is the MULTI mode's own semantic).
2. **Value model:** `getSelectedValues()` / `setSelectedValues(Collection<T>)`. The canonical store is an **option-ordered** list, re-derived through one invariant funnel (`reorderMultiValues`): always a subset of the current options, option order regardless of toggle/collection order, duplicates collapsed, unknowns ignored (the lib's lenient house style — no exceptions). `setOptions` prunes the selection to the surviving options.
3. **Single-value API in multi mode (decided + recorded):** `getSelectedValue()` returns the **first selected value in option order** (mirroring `JList.getSelectedValue`), `null` when empty — the internal `selectedValue` field always tracks `multiValues[0]`, so every Phase-1 read path stays meaningful. `setSelectedValue(v)` delegates to `setSelectedValues(List.of(v))` (null → clear) — the value becomes the entire selection. Symmetrically, `getSelectedValues()` works in single mode (empty/singleton mirror) and `setSelectedValues` in single mode leniently applies the first recognized value.
4. **Editable + multi = mutually exclusive in V1 (decided + recorded).** A filterable multi-select is a real combinatorial surface (filter + toggle + summary + free text interactions) deferred per §10's discipline — documented, not silently cut. The exclusion **forces** rather than throws (lenient house style): `setMultiSelect(true)` on an editable combo first runs `setEditable(false)`, and `setEditable(true)` on a multi-select first runs `setMultiSelect(false)`.
5. **Mode flips preserve what they can:** single→multi seeds the selection with the current single value; multi→single collapses to the first value in option order. Neither fires change listeners — `getSelectedValue()` is unchanged by either flip.
6. **Provisional summary (S1):** display strings joined `", "` in option order, empty selection resting the floating label. The real summary format + overflow policy is S2 (#398).
7. **Zero new theme tokens (held); zero new assets** — Phase 3 S1 is pure behavior over the shipped MULTI menu.
