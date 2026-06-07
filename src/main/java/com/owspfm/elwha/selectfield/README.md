# ElwhaSelectField

The M3 **exposed dropdown menu** — a text field whose trailing dropdown-arrow opens an anchored menu of typed options; choosing one writes its display text back into the field. `ElwhaSelectField<T>` is the first-class *select* Elwha lacked, generic over the option value type.

It is **pure composition** of two shipped primitives — it paints nothing of its own. Decisions: [`docs/research/elwha-selectfield-design.md`](../../../../../../../docs/research/elwha-selectfield-design.md) (and the [research capture](../../../../../../../docs/research/elwha-selectfield-research.md)).

---

## Architecture — composition, not inheritance

`ElwhaSelectField<T> extends JComponent` **owns** a read-only [`ElwhaTextField`](../textfield/README.md) (chrome, label, slots, states, a11y) and **builds an [`ElwhaMenu`](../menu/README.md)** for the option list on open. It is deliberately:

- **not a subclass** of `ElwhaTextField` — which would inherit a typing surface a pure select must suppress;
- **not a `selectMode` flag** on the field — which would bloat the field's API with menu/option concerns and a generic `<T>` it doesn't want.

The field stays a clean single-responsibility primitive; the select field is a clean composition. The embedded field is `setReadOnly(true)` (V1 is a non-editable select) and the trailing slot is **owned** by the select field's dropdown-arrow `ElwhaIconButton` — there is no `setTrailingIcon` passthrough, so a consumer cannot displace the arrow. **Zero new theme tokens**; the only new asset is the bundled `arrow_drop_down` / `arrow_drop_up` glyph (`MaterialIcons.arrowDropDown()` / `arrowDropUp()`).

---

## Quick start

```java
ElwhaSelectField<String> planet = ElwhaSelectField.outlined("Planet");
planet.setOptions(List.of("Mercury", "Venus", "Earth", "Mars"));
planet.addSelectionChangeListener(value -> System.out.println("picked " + value));

// Non-String options are the norm — supply a display renderer:
ElwhaSelectField<City> city = ElwhaSelectField.filled("City");
city.setDisplayFunction(c -> c.name() + " (" + c.country() + ")");
city.setOptions(cities);
city.setSelectedValue(cities.get(0));   // programmatic; syncs the field text + menu mark
```

---

## Options & value model

The single source of truth is the selected `T`.

- `setOptions(List<T>)` — the option list. The menu is rebuilt lazily on the next open (rebuild-on-options-change), so changing options is cheap.
- `setDisplayFunction(Function<T,String>)` — value→label renderer (default `String::valueOf`).
- `getSelectedValue()` / `setSelectedValue(T)` — read / programmatically set the selection; `setSelectedValue(null)` clears (empty field, the floating label rests). A value not among the options is ignored — a select is constrained to its options.
- `addSelectionChangeListener(Consumer<T>)` / `removeSelectionChangeListener(...)` — notified with the new value on every change (a menu pick **or** a programmatic set); not fired for a no-op set to the current value.

Internally the menu is built `SelectionMode.SINGLE`; choosing an item is the write-back path (set field text → close → mark `selected` for the next open).

---

## Variants & state

Per-variant factories `ElwhaSelectField.filled(label)` / `outlined(label)` mirror the lib; the variant rides the embedded field. Every field state delegates:

- `setLabel` / `setLeadingIcon` / `setSupportingText` / `setPlaceholder` pass through to the field.
- `setError(boolean)` / `setErrorText(String)` delegate the error chrome (the field's visual-only error contract).
- `setEnabled(boolean)` propagates to the whole control (field + arrow) and blocks opening; **disabled** dims everything.
- `setReadOnly(boolean)` shows the value but blocks opening to change it — the chrome stays normal (not dimmed), unlike disabled.

Plus the **expanded vs collapsed** menu state: the arrow rotates down↔up as the menu opens (reduced-motion → instant), and its accessible name flips "Open options" ↔ "Close options".

---

## Keyboard & accessibility

The ARIA **combobox** pattern, approximated in Swing. With the field focused and the menu closed:

| Key | Action |
| --- | --- |
| **Down / Up / Enter / Space / Alt+Down** | open the menu and move focus into it |
| a printable key | open and **type-ahead** to a matching option |
| **Esc** | close and return focus to the field |

Once open, the shipped `ElwhaMenu` keyboard map owns navigation; committing closes and refocuses the field. The interactive node for screen readers is the arrow `ElwhaIconButton` — its accessible name encodes the expanded/collapsed state and it fires an `ACCESSIBLE_STATE_PROPERTY` expand/collapse announcement on toggle (the one native-Swing gap, mirroring how `ElwhaTextField` engineers error→alert). Option items keep their `MENU_ITEM` roles + `selected` state.

---

## Scope

V1 ships the **non-editable (pure) select** across Phase 1 (`S1`–`S5`). Documented later phases: **editable / filter-as-you-type combo** (Phase 2) and **multi-select** (`SelectionMode.MULTI` + summary display, Phase 3) — filed, not cut. Epic [#331](https://github.com/OWS-PFMS/elwha/issues/331).
