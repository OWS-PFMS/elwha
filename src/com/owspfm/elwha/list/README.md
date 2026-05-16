# ElwhaItemList<T>

Unified, generic list primitive — single concrete implementation replacing both `ElwhaCardList<T>` and `ElwhaChipList<T>` per [epic #67](https://github.com/OWS-PFMS/elwha/issues/67). Renders any item type whose adapter produces a `Component`; manages selection (4 modes), drag-to-reorder, filter, sort, empty / loading placeholders, pin, anchor, and animated changes.

Spec: [`docs/research/elwha-list-generification-spec.md`](../../../../../../docs/research/elwha-list-generification-spec.md).

## Quick start

```java
DefaultElwhaListModel<Cycle> model = new DefaultElwhaListModel<>(cycles);
ElwhaListAdapter<Cycle> adapter = (cycle, idx) ->
    new ElwhaCard("Cycle #" + (idx + 1))
        .setVariant(CardVariant.OUTLINED)
        .setSubhead(cycle.summary())
        .setMedia(buildCycleBody(cycle));

ElwhaItemList<Cycle> list = new ElwhaItemList<>(model, adapter)
    .setSelectionMode(SelectionMode.SINGLE)
    .setReorderable(true);

list.getSelectionModel().addSelectionListener(evt -> { /* ... */ });
list.addReorderListener(evt -> { /* model already mutated */ });
```

## Concepts

### Model (`ElwhaListModel<T>` / `DefaultElwhaListModel<T>`)

Observable, ordered collection of items. Mirrors `javax.swing.ListModel` with an explicit `MOVED` event for in-place reorder. `DefaultElwhaListModel<T>` is the standard `ArrayList`-backed implementation; consumers can supply their own.

### Adapter (`ElwhaListAdapter<T>`)

Single-method functional interface mapping a domain item to a rendered `Component`. The list invokes it once per visible item — no pooling or recycling.

### Selection (`SelectionMode`, `ElwhaSelectionModel<T>`)

`SelectionMode = { NONE, SINGLE, SINGLE_MANDATORY, MULTI }` — Chip's superset wins per [epic #67](https://github.com/OWS-PFMS/elwha/issues/67). `SINGLE_MANDATORY` auto-selects the first visible item when activated and re-selects the first remaining item whenever the selection becomes empty.

### Reorder (`ReorderHandle`, `ReorderAffordance`)

`ReorderHandle = { WHOLE_ITEM, LEADING_ICON, TRAILING_HANDLE }` controls where on the item the drag gesture is recognized (`WHOLE_CARD` from the legacy enum is renamed `WHOLE_ITEM`).

`ReorderAffordance = { CURSOR_SWAP, HOVER_ICON, BOTH, NONE }` controls how the list signals reorderability:
- `CURSOR_SWAP` — Card's idiom; grab / grabbing cursor PNGs on hover and drag
- `HOVER_ICON` — Chip's idiom; leading-slot drag-handle icon on hover *(currently behaves as `CURSOR_SWAP` pending the chip-visual port)*
- `BOTH` — both *(currently behaves as `CURSOR_SWAP`)*
- `NONE` — drag still enabled, no visual hint

### Pin / anchor (`PinAffordance`, `AnchorAffordance`)

Predicate-driven partitioning. `setPinPredicate(Predicate<T>)` and `setAnchorPredicate(Predicate<T>)` mark items; pinned items render first, the anchor item locks to the leading slot among non-pinned. Affordance enums (`{ NONE, INDICATOR, BUTTON }` each) control the leading-slot visual treatment *(visual rendering pending the chip-visual port; the predicate-driven query + partition behavior is functional)*.

## API summary

```java
public class ElwhaItemList<T> extends JPanel implements ElwhaList<T> {
  ElwhaItemList(ElwhaListModel<T> model, ElwhaListAdapter<T> adapter);

  // Layout (inherited interface)
  ElwhaItemList<T> setOrientation(ElwhaListOrientation);
  ElwhaItemList<T> setColumns(int);
  ElwhaItemList<T> setItemGap(int);
  ElwhaItemList<T> setListPadding(Insets);
  ElwhaItemList<T> setFilter(Predicate<T>);
  ElwhaItemList<T> setSortOrder(Comparator<T>);
  ElwhaItemList<T> setEmptyState(JComponent);
  ElwhaItemList<T> setLoading(boolean);
  ElwhaItemList<T> setLoadingComponent(JComponent);

  // Selection
  ElwhaItemList<T> setSelectionMode(SelectionMode);
  ElwhaSelectionModel<T> getSelectionModel();

  // Reorder
  ElwhaItemList<T> setReorderable(boolean);
  ElwhaItemList<T> setReorderHandle(ReorderHandle);
  ElwhaItemList<T> setReorderAffordance(ReorderAffordance);

  // Pin
  ElwhaItemList<T> setPinPredicate(Predicate<T>);
  ElwhaItemList<T> setPinAction(BiConsumer<T, Boolean>);
  ElwhaItemList<T> setPinAffordance(PinAffordance);
  boolean isPinned(T);
  void togglePin(T);
  void pinStateChanged();
  JMenuItem createPinMenuItem(T);

  // Anchor
  ElwhaItemList<T> setAnchorPredicate(Predicate<T>);
  ElwhaItemList<T> setAnchorAction(Consumer<T>);
  ElwhaItemList<T> setAnchorAffordance(AnchorAffordance);
  boolean isAnchored(T);
  T getAnchoredItem();
  void setAnchor(T);
  void clearAnchor();
  void anchorStateChanged();
  JMenuItem createAnchorMenuItem(T);

  // Query
  List<T> getVisibleItems();
  Component getComponentFor(T);

  // Animation
  ElwhaItemList<T> setAnimateChanges(boolean);
  ElwhaItemList<T> setAnimationDuration(int);

  // Listeners
  void addReorderListener(ElwhaReorderListener<T>);
}
```

## Implementation status — v0.1.0

| Behavior | Status |
| --- | --- |
| `VERTICAL` layout | ✅ implemented |
| `GRID` / `HORIZONTAL` / `WRAP` layouts | ⚠️ accepted by setter, fall back to `VERTICAL` |
| `SelectionMode` (all 4) | ✅ implemented including `SINGLE_MANDATORY` auto-select |
| Reorder (`WHOLE_ITEM` + `CURSOR_SWAP`) | ✅ implemented |
| `HOVER_ICON` / `BOTH` reorder affordances | ⚠️ accepted, behave as `CURSOR_SWAP` |
| Pin / anchor predicates + queries | ✅ functional |
| Pin / anchor leading-icon visual rendering | ⚠️ enums stored, no glyph painted |
| Filter, sort, empty / loading | ✅ implemented |
| Cross-fade animation | ⚠️ `setAnimateChanges` / `setAnimationDuration` accept values, no timer |
| `getVisibleItems()`, `getComponentFor(T)` | ✅ implemented |

The four ⚠️ items are tracked as the remaining work before the legacy `ElwhaChipList<T>` (still present in `com.owspfm.elwha.chip.list`) can be safely deleted without regression.

## Migration from `ElwhaCardList<T>`

The legacy `ElwhaCardList<T>` and its 14 parallel `Card*` support classes were deleted in [#70](https://github.com/OWS-PFMS/elwha/issues/70). Mapping:

| Legacy | V2 |
| --- | --- |
| `ElwhaCardList<T>` | `ElwhaItemList<T>` |
| `CardListModel<T>` / `DefaultCardListModel<T>` | `ElwhaListModel<T>` / `DefaultElwhaListModel<T>` |
| `CardAdapter<T>` — `cardFor(T, int)` | `ElwhaListAdapter<T>` — `componentFor(T, int)` |
| `CardListDataEvent` / `CardListDataListener` | `ElwhaListDataEvent<T>` / `ElwhaListDataListener<T>` |
| `CardSelectionMode.{NONE, SINGLE, MULTIPLE}` | `SelectionMode.{NONE, SINGLE, MULTI}` |
| `CardSelectionModel<T>` / `DefaultCardSelectionModel<T>` | `ElwhaSelectionModel<T>` / `DefaultElwhaSelectionModel<T>` |
| `CardSelectionEvent<T>` / `CardSelectionListener<T>` | `ElwhaSelectionEvent<T>` / `ElwhaSelectionListener<T>` |
| `CardReorderEvent<T>` / `CardReorderListener<T>` | `ElwhaReorderEvent<T>` / `ElwhaReorderListener<T>` |
| `getCardFor(T) : ElwhaCard` | `getComponentFor(T) : Component` (downcast if needed) |
| `ReorderHandle.WHOLE_CARD` | `ReorderHandle.WHOLE_ITEM` |
| `ElwhaCardList.Orientation.{VERTICAL, GRID}` | `ElwhaListOrientation.{VERTICAL, GRID}` directly |

## Migration from `ElwhaChipList<T>` (pending)

The chip-list class and its 11 parallel support types are still in place pending the chip-visual port (`HOVER_ICON` affordance, pin / anchor `INDICATOR` / `BUTTON` glyph rendering, animation timer). When the port lands, the migration table will mirror Card's: `ChipListModel<T>` → `ElwhaListModel<T>`, `ChipSelectionMode.{NONE, SINGLE, SINGLE_MANDATORY, MULTIPLE}` → `SelectionMode.{NONE, SINGLE, SINGLE_MANDATORY, MULTI}`, etc.
