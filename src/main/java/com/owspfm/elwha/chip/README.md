# ElwhaChip / ElwhaChipList

A token-native chip primitive — text + leading icon + optional trailing icon-button — plus a model-driven list-of-chips container supporting four orientations, selection, drag-to-reorder, filter, sort, empty / loading state, keyboard navigation, and accessibility.

`ElwhaChip` styling resolves entirely from the [Elwha design tokens](../theme/) — no raw colors / insets / pixel values reach the public setter API. Mirrors the structure of the sibling [`ElwhaCard` + `ElwhaCardList`](../card/README.md) package, sharing the cross-cutting [`ElwhaList<T>`](../list/ElwhaList.java) abstraction.

---

## Quick start

```java
// 1. Build a model
DefaultChipListModel<Factor> model = new DefaultChipListModel<>(factors);

// 2. Define how each item becomes a chip
ChipAdapter<Factor> adapter = (factor, idx) ->
    ElwhaChip.filterChip(factor.name())
        .setLeadingIcon(factor.icon());

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

A single-row capsule containing optional leading icon, text, and optional Action-bound trailing icon-button. **Token-native** — every visual property resolves from the Elwha tokens at paint time.

### Variants (treatment-only)

The variant declares *treatment*. The surface color is independently overridable per instance via `setSurfaceRole(ColorRole)` — color and treatment are orthogonal.

| Variant     | Default surface role          | Default border role          | Description                                                |
| ----------- | ----------------------------- | ---------------------------- | ---------------------------------------------------------- |
| `FILLED`    | `ColorRole.PRIMARY_CONTAINER` | `ColorRole.OUTLINE_VARIANT`  | The M3 default — a distinct cluster against the surrounding surface. |
| `OUTLINED`  | `ColorRole.SURFACE`           | `ColorRole.OUTLINE`          | The M3 resting outlined chip — hairline border, surface fill. |
| `GHOST`     | (none — transparent)          | (none — at rest)             | Text-with-padding until interacted; tab-strip use.         |

### M3 chip-type factory presets

Sugar over the orthogonal axes — everything stays overridable through the normal setters.

```java
ElwhaChip.assistChip("Set reminder");                  // CLICKABLE + OUTLINED
ElwhaChip.filterChip("Demand");                        // SELECTABLE + OUTLINED
ElwhaChip.inputChip("acme.com", () -> remove(it));     // CLICKABLE + OUTLINED + trailing × remove
ElwhaChip.suggestionChip("Try this");                  // CLICKABLE + OUTLINED
```

### Interaction modes

| Mode         | Behavior                                                                  |
| ------------ | ------------------------------------------------------------------------- |
| `STATIC`     | Non-interactive (no mouse / keyboard response)                            |
| `HOVERABLE`  | Hover feedback only                                                       |
| `CLICKABLE`  | Push-button: fires `ActionEvent` on click / Space / Enter                 |
| `SELECTABLE` | Toggle: persistent `selected` state + `"selected"` `PropertyChangeEvent`  |

### Context menus

```java
chip.attachContextMenu(jPopupMenu);
// or
chip.attachContextMenu(() -> buildPopupForCurrentState());
// or full control:
chip.setContextMenuCallback(evt -> popup.show(evt.getComponent(), evt.getX(), evt.getY()));
```

`VK_CONTEXT_MENU` and `Shift+F10` keyboard accelerators invoke the same callback as right-click.

### Trailing icon-button

```java
chip.setTrailingAction(action);                              // Action-bound (uses SMALL_ICON or NAME)
chip.setTrailingIcon(closeIcon, "Remove", () -> remove(it)); // convenience for icon + tooltip + click
```

The trailing button has its own hover / press states and **does not bubble** clicks to the chip's own action listeners.

---

## Styling — typed token setters

Styling is driven by **roles** and **scale steps**, never raw `Color` / `Insets` / pixel values:

```java
chip.setSurfaceRole(ColorRole.SECONDARY_CONTAINER); // override the variant's default surface
chip.setShape(ShapeScale.FULL);                     // capsule shape (default is SM = 8px)
chip.setPadding(SpaceScale.MD, SpaceScale.XS);      // horizontal × vertical from the spacing ladder
chip.setBorderWidth(2);                             // stroke width is genuine geometry; no token equivalent
```

### Color resolution

- **Surface** — the per-instance `setSurfaceRole` override if set, else the variant's `surfaceRole()`. `GHOST` resolves to transparent until hovered / pressed / selected / focused.
- **Foreground** — always the `on`-pair of the effective surface role (e.g. `PRIMARY_CONTAINER` → `ON_PRIMARY_CONTAINER`). No per-instance foreground setter — that would re-introduce unpaired surface/foreground.
- **Border** — the variant's `borderRole()`, swapped to `ColorRole.PRIMARY` when the chip is selected or focused so OUTLINED chips read as "the picked one" even under the uniform 12 % selected overlay.
- **State layers** — hover (8 %), pressed (10 %), selected (12 %) composited per the M3 `StateLayer` model, tinted by the surface's `on`-role.

### Theming

App-wide chip theming happens by installing a different palette through `ElwhaTheme.install(...)` — every chip re-skins on the next paint, no per-component intervention. There is **no `ElwhaChip.*` UIManager namespace** in the rebuilt API; the previous escape-hatch keys were removed because nothing is left for them to do that the role / scale system doesn't already cover.

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

`ChipSelectionMode.{NONE, SINGLE, SINGLE_MANDATORY, MULTIPLE}`. Multi-selection supports Shift-click for range, Cmd / Ctrl-click for toggle, and `Cmd/Ctrl+A` for select-all.

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

## Playground

`ElwhaChipPlayground` is the canonical interactive surface — a `Variant gallery` tab (every variant × every interaction mode × {idle, hover, pressed, selected, focused, disabled}, plus the factory-preset row and the trailing-icon sampler) and a `Live list` tab driven by the `chip.list` container. A light / dark / system mode toggle re-installs the Elwha theme so the binding rule is exercised end-to-end.

The same two panels are also surfaced inside `ThemePlayground`'s top-level `Chip` tab — both entry points compose the shared builders in [`ChipPlaygroundPanels`](playground/ChipPlaygroundPanels.java) so the validation matrix stays in lockstep.

```
mvn -q exec:java -Dexec.mainClass=com.owspfm.elwha.chip.ElwhaChipPlayground
mvn -q exec:java -Dexec.mainClass=com.owspfm.elwha.theme.playground.ThemePlayground
```

---

## Independence

This package depends only on FlatLaf, standard Swing, and the Elwha theme package. The `chip/`, `chip/list/`, `chip/playground/`, `list/`, `theme/`, and `icons/` directories together are the full lib.
