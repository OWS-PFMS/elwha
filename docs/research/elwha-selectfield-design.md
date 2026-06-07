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
