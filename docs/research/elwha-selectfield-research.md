# ElwhaSelectField — M3 exposed-dropdown menu research

**Epic [#331](https://github.com/OWS-PFMS/elwha/issues/331).** The M3 *exposed dropdown menu* — a text field whose trailing dropdown-arrow opens an anchored menu of options; choosing one writes back to the field. Unlike every prior component research pass, this primitive **composes two already-shipped, already-M3-validated Elwha primitives** rather than introducing new chrome — so this capture is a *composition spec* grounded in the two existing research docs, not a fresh 50-screenshot M3 re-capture.

## Source material (already captured)

- **`elwha-textfield-research.md` §GD2** — the dropdown-arrow signifier: *"a dropdown arrow indicates a text field with a **nested selection component** (the exposed-dropdown / menu-backed select) → it would compose `ElwhaTextField` + `ElwhaMenu` (#298)."* The field chrome, label-float, slots, read-only state, error contract, and a11y are all the shipped `ElwhaTextField` V1.
- **`elwha-menu-research.md`** — the option list:
  - §109 menu types: *"dropdown … + **exposed-dropdown (text field + list)**."*
  - §76: *"Menus can open from … **text fields**."*
  - §69 selection state (new in Expressive): the screenshot is literally an exposed-dropdown (*"'Label' field, Item 2 selected"*) — a selected item shows a **leading checkmark + filled highlight**.
  - §103 a11y: container `role=menu` / items `role=menuitem`, **adaptable to `listbox`/`option` for combobox use**; ARIA `aria-expanded`, `aria-activedescendant`, `aria-selected`.
  - §87–88 anchoring: `anchorCorner` default `END_START`, `menuCorner` `START_START` (menu hangs below-start of the anchor) — the shipped `ElwhaMenu.open(anchor)` already does this plus viewport flip.
  - §93 keyboard: Up/Down, Home/End, Enter/Space select, Esc close, type-ahead (200 ms).
  - §M: the *"32"* redline = the exposed-dropdown **field-row height** (32 dp) — flagged "out of core" for the menu itself; it belongs to *this* epic.
  - Q9 (resolved 2026-06-04): **filtering / autocomplete → "out of core (future exposed-dropdown)"** — i.e. *here*, as a later phase.

## What M3 specifies for the exposed dropdown

**Anatomy** = a standard text field (Filled or Outlined, label, supporting text, the full state set) **+ a trailing dropdown-arrow icon** **+** an anchored **menu list** of options. The arrow is the affordance that signifies "this field opens a selection." No new container, label, or supporting-text anatomy — it is the text-field anatomy plus one trailing icon and a popup.

**Two variants:**
- **Non-editable (pure select)** — the field is **read-only**; the user cannot type. The whole field (and the arrow) is the open/close affordance. Picking a menu item is the only way to set the value. This is the V1 target.
- **Editable (combo / autocomplete)** — the user **can type**; keystrokes filter the menu (`listbox`/`option` semantics), and free text may be allowed. This is the M3 "filtering" behavior the menu research routed here as a later phase.

**Selection / write-back** — choosing a menu item **populates the field** with that option's text and **closes** the menu; the field then displays the current selection. The menu's `selected` visual (checkmark + filled highlight) marks the current choice when the menu is reopened.

**The arrow rotates** — the trailing dropdown arrow points **down when closed**, **up when open** (a 180° flip), the standard exposed-dropdown open-state cue. Reduced-motion → instant.

**Keyboard** (field has focus, menu closed): **Down** / **Enter** / **Space** / **Alt+Down** open the menu and move focus into it; **Esc** closes and returns focus to the field; **type-ahead** jumps to a matching option. Once open, the menu owns navigation (its shipped keyboard map). Selecting commits + closes + refocuses the field.

**States** — exactly the text field's: enabled / hover / focus / error / disabled / read-only — plus the **expanded vs collapsed** menu state (arrow direction + `aria-expanded`). Error/disabled propagate to the whole control.

**Accessibility** — the M3/ARIA pattern is a **combobox**: the field exposes the expanded/collapsed state and points at the active option; the popup is a `listbox` of `option`s (the menu's `MENU_ITEM` items, adaptable). In Swing terms the field stays a text component (read-only), the arrow is an `ElwhaIconButton` (free Button role + expanded-state name), and the option items carry their `ElwhaMenuItem` roles. Swing has no native `combobox`/`aria-expanded` role on a text component → approximate via the arrow button's accessible name/state and an accessible state-change announcement, mirroring how `ElwhaTextField` engineered the error→alert gap.

## Composing API already on `main` (nothing to build in the primitives)

| Need | Shipped surface |
|---|---|
| Field chrome / label / supporting / error / states | `ElwhaTextField` V1 ([#286](https://github.com/OWS-PFMS/elwha/issues/286)) |
| Read-only (non-editable select) | `ElwhaTextField.setReadOnly(true)` |
| Trailing dropdown-arrow affordance | `ElwhaTextField.setTrailingIconButton(ElwhaIconButton)` + `MaterialIcons` arrow glyph |
| Anchored popup (below-start, viewport flip, light-dismiss, focus-restore) | `ElwhaMenu.open(Component anchor)` |
| Option list + selection visual | `ElwhaMenu.builder().addItem(ElwhaMenuItem)…` + `SelectionMode.SINGLE` (checkmark + fill) |
| Selection write-back hook | `Builder.onSelectionChange(Consumer<ElwhaMenuItem>)` |
| Per-item activation | `ElwhaMenuItem.addActionListener` / `isSelected()` / `setSelected(boolean)` |
| Editable-mode filtering (later) | live `getEditor().getDocument()` + rebuilding the menu's items |

**Zero new theme tokens** — both primitives are already token-native; the select field paints nothing of its own (the field and menu paint themselves). The only new visual is the arrow glyph (a bundled Material Symbol) and its open/closed rotation.

## Out of scope (V1)

- **Editable / filter-as-you-type combo** → later phase (M3 "filtering"; menu-research Q9).
- **Multi-select** (`SelectionMode.MULTI` with a chip/summary display) → later phase.
- **Async / remote option loading**, validation engines, formatted values → consumer-owned, documented deferrals.
