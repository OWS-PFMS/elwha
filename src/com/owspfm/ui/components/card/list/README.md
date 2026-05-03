# FlatCardList

A reusable, model-driven `Swing` list of `FlatCard`s. Drop-in replacement for the
hand-rolled `JPanel + BoxLayout + listener` scaffolding that tends to accumulate
around card-heavy surfaces.

This package has **no dependencies on application code** — only FlatLaf and
standard Swing — and depends only on its sibling `com.owspfm.ui.components.card`
package. Both directories can be lifted into a separate Maven module unchanged.

## Quick start

```java
DefaultCardListModel<Cycle> model = new DefaultCardListModel<>(cycles);
CardAdapter<Cycle> adapter = (cycle, idx) ->
    new FlatCard()
        .setVariant(CardVariant.OUTLINED)
        .setHeader("Cycle #" + (idx + 1), cycle.summary())
        .setBody(buildCycleBody(cycle));

FlatCardList<Cycle> list = new FlatCardList<>(model, adapter)
    .setSelectionMode(CardSelectionMode.SINGLE)
    .setReorderable(true);

list.getSelectionModel().addSelectionListener(evt -> { /* … */ });
list.addReorderListener(evt -> { /* model already mutated */ });
```

Sensible defaults: vertical orientation, no selection, no reorder, default gap,
and built-in empty / loading placeholders.

## API surface

### Backing model — `CardListModel<T>`

| Method | Purpose |
|---|---|
| `getSize()` | Item count |
| `getElementAt(int)` | Item at index |
| `iterator()` | In-order iteration |
| `addCardListDataListener(...)` | Observe changes |

`DefaultCardListModel<T>` is the standard implementation; mutators
(`add`, `addAll`, `remove`, `set`, `move`, `clear`) each fire one
`CardListDataEvent` after the change. The event type taxonomy includes an
explicit **`MOVED`** variant so the list can animate reorders without losing
item identity.

### Adapter — `CardAdapter<T>`

```java
@FunctionalInterface
public interface CardAdapter<T> {
  FlatCard cardFor(T item, int index);
}
```

A single-method functional interface — lambdas welcome. The adapter is invoked
once per visible item; cards are not pooled or recycled.

### Layout

```java
list.setOrientation(Orientation.VERTICAL);   // single column (default)
list.setOrientation(Orientation.GRID);
list.setColumns(3);                           // grid only
list.setItemGap(12);
list.setListPadding(new Insets(16, 16, 16, 16));
```

### Selection

```java
list.setSelectionMode(CardSelectionMode.NONE);     // default
list.setSelectionMode(CardSelectionMode.SINGLE);
list.setSelectionMode(CardSelectionMode.MULTIPLE);

CardSelectionModel<Cycle> sel = list.getSelectionModel();
sel.addSelectionListener(evt -> evt.getSelected().forEach(System.out::println));
```

`SINGLE` is exclusive. `MULTIPLE` supports Shift-click for ranges and
Cmd/Ctrl-click for individual toggles. Cards rendered into a selection-enabled
list are automatically given `CardInteractionMode.SELECTABLE` — adapters do not
have to wire it.

### Reorder

```java
list.setReorderable(true);
list.setReorderHandle(ReorderHandle.WHOLE_CARD);   // default
list.setReorderHandle(ReorderHandle.LEADING_ICON);
list.setReorderHandle(ReorderHandle.TRAILING_HANDLE);
list.addReorderListener(evt -> {
  // model already mutated; evt has fromIndex / toIndex / item
});
```

A translucent placeholder follows the dragged card and shows the drop target.
On drop, the model receives `move(from, to)` and listeners fire with the
**model** indices (not visible indices).

When a sort order is active, drag-to-reorder is disabled and a one-shot warning
is logged — sort is a view concern; reorder mutates the model; mixing them is
user-confusing.

### Filter and sort

```java
list.setFilter(item -> item.score() > threshold);   // null clears
list.setSortOrder(Comparator.comparing(Cycle::date)); // null clears
```

The model order is unchanged. Filter is applied first, then sort. Selection
survives a filter change for items that still match.

### Empty / loading state

```java
list.setEmptyState(myCustomPlaceholder);    // null restores default
list.setLoading(true);
list.setLoadingComponent(mySpinner);        // optional
```

### Animations

```java
list.setAnimateChanges(true);
list.setAnimationDuration(220);
```

Fade animations on add/remove and reorder. Disable for low-power scenarios.

### Keyboard navigation

| Key | Action |
|---|---|
| ↑ / ↓ | Move focus between cards |
| Home / End | Jump to first / last |
| PgUp / PgDn | Jump by ~5 items |
| Space / Enter | Activate the focused card |
| Cmd/Ctrl+A | Select all (in `MULTIPLE` mode) |

### Accessibility

The list itself reports `AccessibleRole.LIST`; rendered cards inherit Swing's
default panel role. Selection state is reflected through each card's normal
focus / selection visuals.

### Theme awareness

Colors and the placeholder accent derive from FlatLaf `UIManager` keys —
themes switch cleanly with no caller intervention.

## Demo and playground

Quick smoke test:

```
mvn -q exec:java \
  -Dexec.mainClass=com.owspfm.ui.components.card.list.FlatCardListDemo
```

The full playground (interactive controls, snippet pane, theme switcher) lives
in `com.owspfm.ui.components.card.playground.FlatCardPlayground` under the
**FlatCardList** tab.

## Extracting to a separate library

Like the parent `card` package, this package was designed for a clean
lift-and-shift:

1. Move the `com/owspfm/ui/components/card/list` directory (and its sibling
   `card` directory) into a new Maven module's `src/main/java/`.
2. Rename the package roots if you prefer a non-OWS namespace; the only
   references are intra-package.
3. Add `com.formdev:flatlaf` as the only runtime dependency.

There are no Singleton, static factory, or app-scope dependencies to unwind —
every collaborator is constructed directly by the caller.
