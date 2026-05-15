# ElwhaChip / ElwhaChipList

A reusable, FlatLaf-aware chip primitive — text + leading icon + optional trailing icon-button — plus a model-driven list-of-chips container supporting four orientations, selection, drag-to-reorder, filter, sort, empty / loading state, keyboard navigation, and accessibility.

Mirrors the structure of the sibling [`ElwhaCard` + `ElwhaCardList`](../card/README.md) package, sharing the cross-cutting [`ElwhaList<T>`](../list/ElwhaList.java) abstraction.

---

## Quick start

```java
// 1. Build a model
DefaultChipListModel<Factor> model = new DefaultChipListModel<>(factors);

// 2. Define how each item becomes a chip
ChipAdapter<Factor> adapter = (factor, idx) ->
    new ElwhaChip(factor.name())
        .setLeadingIcon(factor.icon())
        .setVariant(ChipVariant.OUTLINED);

// 3. Build the list and wire it up
ElwhaChipList<Factor> list = new ElwhaChipList<>(model, adapter)
    .setOrientation(ElwhaListOrientation.WRAP)
    .setSelectionMode(ChipSelectionMode.MULTIPLE)
    .setReorderable(true);

list.getSelectionModel().addSelectionListener(evt ->
    System.out.println("Selected: " + evt.getSelected()));
list.addReorderListener(evt ->
    System.out.println(evt.getItem() + ": " + evt.getFromIndex() + " → " + evt.getToIndex()));
```

---

## ElwhaChip — the primitive

A single-row capsule containing optional leading icon, text, and optional Action-bound trailing icon-button.

### Variants

| Variant         | Description                                                                       |
| --------------- | --------------------------------------------------------------------------------- |
| `FILLED`        | Tinted background; the workhorse for chip / tag rows                              |
| `OUTLINED`      | Hairline border, transparent fill; for dense rows that need to read as "lighter" |
| `GHOST`         | No fill, no border until hovered / pressed / selected; ideal for tab strips       |
| `WARM_ACCENT`   | Tinted with the application's warm accent (gold/amber); for emphasis              |

### Interaction modes

| Mode          | Behavior                                                                          |
| ------------- | --------------------------------------------------------------------------------- |
| `STATIC`      | Non-interactive (no mouse / keyboard response)                                    |
| `HOVERABLE`   | Hover feedback only                                                               |
| `CLICKABLE`   | Push-button: fires `ActionEvent` on click / Space / Enter                         |
| `SELECTABLE`  | Toggle: persistent `selected` state + `"selected"` `PropertyChangeEvent`          |

### Context menus

```java
chip.attachContextMenu(jPopupMenu);
// or
chip.attachContextMenu(() -> buildPopupForCurrentState());
// or full control:
chip.setContextMenuCallback(evt -> popup.show(evt.getComponent(), evt.getX(), evt.getY()));
```

Right-click is detected on both `mousePressed` (Mac) and `mouseReleased` (Windows). `VK_CONTEXT_MENU` and `Shift+F10` keyboard accelerators invoke the same callback.

### Trailing icon-button

```java
chip.setTrailingAction(action);                            // Action-bound (uses SMALL_ICON or NAME)
chip.setTrailingIcon(closeIcon, "Remove", () -> remove(it)); // convenience for icon + tooltip + click
```

The trailing button has its own hover / press states and **does not bubble** clicks to the chip's own action listeners.

---

## Three-layer styling

Every visual property is resolved through three layers, last-wins:

1. **Variant defaults** — chosen by `setVariant(ChipVariant)`. Each variant pre-fills a coherent palette derived from `UIManager` keys.
2. **`UIManager` overrides** — drop a FlatLaf properties file into your app's classpath to theme every chip at once. Public keys:

   | Key                              | Type           | Default                           |
   | -------------------------------- | -------------- | --------------------------------- |
   | `ElwhaChip.background`            | `Color`        | (variant default)                 |
   | `ElwhaChip.borderColor`           | `Color`        | (variant default)                 |
   | `ElwhaChip.arc`                   | `Integer`      | `999` (capsule)                   |
   | `ElwhaChip.padding`               | `Insets`       | `Insets(4, 10, 4, 10)`            |
   | `ElwhaChip.hoverBackground`       | `Color`        | foreground-tinted panel           |
   | `ElwhaChip.pressedBackground`     | `Color`        | foreground-tinted panel (heavier) |
   | `ElwhaChip.selectedBackground`    | `Color`        | accent-tinted panel               |
   | `ElwhaChip.selectedBorderColor`   | `Color`        | accent                            |
   | `ElwhaChip.focusColor`            | `Color`        | `Component.focusColor`            |
   | `ElwhaChip.disabledBackground`    | `Color`        | (variant default at low contrast) |
   | `ElwhaChip.warmAccent`            | `Color`        | `Color(248, 226, 165)` (gold)     |

3. **Per-instance overrides** — call setters on a specific chip:
   ```java
   chip.setCornerRadius(8)
       .setPadding(new Insets(2, 6, 2, 6))
       .setBorderColor(Color.RED)
       .setSurfaceColor(customFill);
   ```
   Or use the `"ElwhaChip.style"` client property for a FlatLaf-style key=value string.

---

## ElwhaChipList — the container

### Orientations

| Orientation  | Layout                                                                                |
| ------------ | ------------------------------------------------------------------------------------- |
| `VERTICAL`   | Single-column stack, chips sized to preferred height                                  |
| `HORIZONTAL` | Single row, clip+scroll overflow (wrap in `JScrollPane`)                              |
| `WRAP`       | Multi-row `FlowLayout`-derivative — wraps to the next row when container width fills  |
| `GRID`       | N-column uniform cell grid (set columns via `setColumns(int)`)                        |

### Selection

`ChipSelectionMode.{NONE, SINGLE, MULTIPLE}`. Multi-selection supports Shift-click for range, Cmd / Ctrl-click for toggle, and `Cmd/Ctrl+A` for select-all.

The selection model operates on **item identity** rather than indices, so selection survives filter / sort changes.

### Drag-to-reorder

```java
list.setReorderable(true);
```

Works across all four orientations. Backed by a 16ms animation timer at 30%-per-tick easing. Requires a mutable model (`DefaultChipListModel`) — non-mutable models log a one-shot warning and ignore the drop. Drag is silently disabled while a sort comparator is active.

### Filter / sort

```java
list.setFilter(factor -> factor.weight() > threshold);
list.setSortOrder(Comparator.comparing(Factor::name));
```

### Empty / loading state

```java
list.setEmptyState(emptyComponent);
list.setLoadingComponent(spinner);
list.setLoading(true);
```

Both fall back to a built-in placeholder when `null`.

### Keyboard navigation

| Key                       | Action                                          |
| ------------------------- | ----------------------------------------------- |
| Arrow keys                | Move focus between chips                        |
| Home / End                | Jump to first / last chip                       |
| Space / Enter             | Activate focused chip (clicks / toggles)        |
| `Cmd/Ctrl+A`              | Select all (multi-selection only)               |

---

## Demos

| Class                 | Purpose                                                                       |
| --------------------- | ----------------------------------------------------------------------------- |
| `ElwhaChipDemo`        | Minimal smoke test — variant × state matrix + one interactive sample          |
| `ElwhaChipPlayground`  | Full interactive playground: variant gallery, live list with all orientations, and a **live LAF tweak panel** with sliders / color pickers for every `ElwhaChip.*` `UIManager` key |

Run either via:
```
mvn -q exec:java -Dexec.mainClass=com.owspfm.elwha.chip.ElwhaChipDemo
mvn -q exec:java -Dexec.mainClass=com.owspfm.elwha.chip.ElwhaChipPlayground
```

---

## Independence

This package has **no dependencies on application code**. It depends only on FlatLaf and standard Swing. The `chip/`, `chip/list/`, and `list/` directories together can be lifted into a standalone library.
