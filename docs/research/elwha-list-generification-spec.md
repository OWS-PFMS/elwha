# ElwhaItemList<T> — List-family generification spec

**Status:** LOCKED for v0.1 build. Fixes the unified `ElwhaItemList<T extends Component>` API surface, the audit between `ElwhaCardList<T>` and `ElwhaChipList<T>`, the two new enums (`SelectionMode`, `ReorderAffordance`), the generic model / event / listener class signatures, and the full migration map. Source-code implementation is story #69; playground migration + legacy-class deletion is story #70.

**Drafted:** 2026-05-16

**Author:** Charles Bryan (`cfb3@uw.edu`).

**Parents:**
- [`elwha-design-direction.md`](elwha-design-direction.md) — narrow primitives, max coverage; the `max(funcA, funcB)` principle this epic operationalizes.
- [`elwha-card-v2-spec.md`](elwha-card-v2-spec.md) — Card V2 changes Card's internals; this spec changes the list container. Independent axes — the Card / list rebuilds compose cleanly.
- [`../development/component-api-conventions.md`](../development/component-api-conventions.md) — five locked doctrine rules (#62) the unified class is expected to match.

**Epic:** [#67](https://github.com/OWS-PFMS/elwha/issues/67) — `ElwhaItemList<T> — collapse Card/Chip list families into a single generic implementation`.

**Origin / supersedes:** `OWS-PFMS/OWS-Local-Search-GUI#252` (filed before the lib existed; absorbed by #67).

---

## TL;DR

1. **Single concrete class:** `ElwhaItemList<T extends Component>` replaces both `ElwhaCardList<T>` and `ElwhaChipList<T>`. The narrow `ElwhaList<T>` interface is **kept verbatim** — zero churn for callers that already use it as a parameter type.
2. **`max(funcA, funcB)`** — when the two families have different capabilities for the same concern, the richer side wins. No feature loss in the collapse.
3. **Selection:** unified `SelectionMode = { NONE, SINGLE, SINGLE_MANDATORY, MULTI }` (Chip's superset). Card's `MULTIPLE` and Chip's `MULTIPLE` both migrate to `MULTI`; Chip's `SINGLE_MANDATORY` carries forward.
4. **Reorder affordance:** new `ReorderAffordance = { CURSOR_SWAP, HOVER_ICON, BOTH, NONE }` enum. Cursor-swap (Card's grab/grabbing PNGs) and hover-revealed icon (Chip's leading-icon affordance) are both preserved; the new enum lets the consumer pick one, both, or neither.
5. **Pin / anchor / animation / visible-items / item→component:** all lifted from their existing family to the unified class per the operator-locked superset choices in epic #67.
6. **Generic supporting types:** `ElwhaListModel<T>` / `DefaultElwhaListModel<T>` / `ElwhaListAdapter<T>` / `ElwhaListDataEvent<T>` / `ElwhaListDataListener<T>` / `ElwhaSelectionModel<T>` / `DefaultElwhaSelectionModel<T>` / `ElwhaSelectionEvent<T>` / `ElwhaSelectionListener<T>` / `ElwhaReorderEvent<T>` / `ElwhaReorderListener<T>`. **12 classes** replace the 22 current parallel `Card*` / `Chip*` types.
7. **Dropped:** `ElwhaCardList.Orientation` alias (just re-exports `ElwhaListOrientation`'s two values; pure noise), Chip-only `MovementMode` enum (folded into `setReorderable(boolean)` + `setReorderAffordance(...)` + the pin / anchor predicates), Chip-only `IconAffordance` enum (replaced by `ReorderAffordance` for reorder + pin/anchor-specific affordance setters).
8. **Doctrine:** matches every #62 rule — `getX()`-only getters, single-arg convenience-free constructor (`(model, adapter)` is the natural pair), no per-instance `setBorderRole` (lists aren't variant-bearing), symmetric `borderWidth` not applicable (list container itself isn't bordered today).

---

## 1. The §9 bar

Design direction §9: *"Build a component only when raw Swing + tokens can't express what you need."* This isn't a new component — it's the **collapse** of two existing components into one. The §9-equivalent question: *do `ElwhaCardList<T>` and `ElwhaChipList<T>` carry enough shared behavior to justify a single implementation, instead of two near-duplicates?*

Concrete evidence:
- **22 parallel classes** exist today (`Card*` / `Chip*` adapters, models, events, listeners, selection models). Adding a third list family (e.g., `ElwhaIconButtonList<T>` if one is ever filed) would add 11 more.
- **Every divergence is documentable**: Card uses cursor-swap, Chip uses hover-icon (both UX-valid; pick via enum). Card lacks `SINGLE_MANDATORY` (Chip-only feature; lift it). Card has animation, Chip lacks it (lift it). Pin / anchor are Chip-only behaviors (lift them).
- **The `T extends Component` bound** is the load-bearing constraint. Both families' `T` is "whatever the consumer renders" — the adapter produces a `Component`, the list lays it out. Once `T extends Component` is the contract, the list can stop caring whether the component is an `ElwhaCard` or an `ElwhaChip`.

Verdict: collapse is justified, and the cost of keeping two parallel implementations (every new behavior added twice, every bug fixed twice) is the real ongoing tax.

## 2. Full audit table

Every public method on `ElwhaCardList<T>` and `ElwhaChipList<T>`, categorized.

| Method | On Card | On Chip | Status in V2 | Notes |
|---|---|---|---|---|
| `ElwhaXList(model, adapter)` ctor | ✓ | ✓ | **identical** → `ElwhaItemList(model, adapter)` | Adapter is generic; same signature shape |
| `getModel()` | ✓ | ✓ | **identical** → `ElwhaListModel<T>` | Type generalized |
| `getSelectionModel()` | ✓ | ✓ | **identical** → `ElwhaSelectionModel<T>` | Type generalized |
| `setOrientation(ElwhaListOrientation)` / `getOrientation()` | ✓ | ✓ | **identical** | Already token on shared interface |
| `setColumns(int)` / `getColumns()` | ✓ | ✓ | **identical** | |
| `setItemGap(int)` / `getItemGap()` | ✓ | ✓ | **identical** | |
| `setListPadding(Insets)` | ✓ | ✓ | **identical** | |
| `setFilter(Predicate<T>)` | ✓ | ✓ | **identical** | |
| `setSortOrder(Comparator<T>)` | ✓ | ✓ | **identical** | |
| `setEmptyState(JComponent)` | ✓ | ✓ | **identical** | |
| `setLoading(boolean)` / `setLoadingComponent(JComponent)` | ✓ | ✓ | **identical** | |
| `setReorderable(boolean)` / `isReorderable()` | ✓ | ✓ | **identical** | Combined with `setReorderAffordance(...)` for visual treatment |
| `setSelectionMode(...)` / `getSelectionMode()` | Card: `CardSelectionMode`; Chip: `ChipSelectionMode` (superset) | — | **divergent (pick)** → `SelectionMode` (Chip's superset wins; Card gains `SINGLE_MANDATORY`) | See §3 |
| `setReorderHandle(ReorderHandle)` | ✓ (`WHOLE_CARD` / `LEADING_ICON` / `TRAILING_HANDLE`) | — | **card-only (lift)** → `setReorderHandle(ReorderHandle)` carried forward verbatim | Chip's reorder is whole-chip; new enum value `WHOLE_ITEM` aliases `WHOLE_CARD` |
| `setMovementMode(MovementMode)` / `getMovementMode()` | — | ✓ (`STATIC` / `MOVABLE` / `PINNED` / `ANCHORED`) | **chip-only (split + drop)** | `STATIC` ↔ `setReorderable(false)`; `MOVABLE` ↔ `setReorderable(true)` (no pin/anchor); `PINNED` ↔ `setReorderable(true)` + `setPinPredicate(...)`; `ANCHORED` ↔ `setReorderable(true)` + `setAnchorPredicate(...)`. Enum is gone — the predicates already encode the mode |
| `setPinPredicate` / `setPinAction` / `isPinned` / `togglePin` / `pinStateChanged` / `createPinMenuItem` | — | ✓ | **chip-only (lift)** → all 6 carried forward verbatim | |
| `setAnchorPredicate` / `setAnchorAction` / `isAnchored` / `setAnchor` / `clearAnchor` / `getAnchoredItem` / `anchorStateChanged` / `createAnchorMenuItem` | — | ✓ | **chip-only (lift)** → all 8 carried forward verbatim | |
| `setPinAffordance(IconAffordance)` / `getPinAffordance()` | — | ✓ | **chip-only (lift, rename)** → `setPinAffordance(PinAffordance)` (enum renamed for symmetry with `AnchorAffordance`) | Values: `NONE / INDICATOR / BUTTON` |
| `setAnchorAffordance(IconAffordance)` / `getAnchorAffordance()` | — | ✓ | **chip-only (lift, rename)** → `setAnchorAffordance(AnchorAffordance)` | Same values |
| `getVisibleItems()` | — | ✓ | **chip-only (lift)** → `getVisibleItems()` carried forward | Card today recomputes it inline; generic class exposes it |
| `setAnimateChanges(boolean)` / `setAnimationDuration(int)` | ✓ | — | **card-only (lift)** | |
| `setReorderAffordance(ReorderAffordance)` / `getReorderAffordance()` | — | — | **new (pick / merge)** → §4 | Card's cursor-swap and Chip's hover-icon both preserved via the new enum |
| `getCardFor(T)` / `getChipFor(T)` | ✓ / — | — / ✓ | **divergent (rename)** → `getComponentFor(T) : T` | Returns the typed component instance |
| `addReorderListener(...)` / `removeReorderListener(...)` | ✓ (`CardReorderListener<T>`) | ✓ (`ChipReorderListener<T>`) | **divergent (collapse)** → `ElwhaReorderListener<T>` + `ElwhaReorderEvent<T>` | Identical shape; just generic |
| `ElwhaCardList.Orientation` inner class | ✓ | — | **dropped (no replacement)** | Re-exported `ElwhaListOrientation.VERTICAL` / `.GRID` only; pure noise. Callers migrate to `ElwhaListOrientation` directly |
| `updateUI()` / `getAccessibleContext()` | ✓ | ✓ | **identical** (inherited Swing) | |

## 3. `SelectionMode` enum

**Locked values** (epic #67): `{ NONE, SINGLE, SINGLE_MANDATORY, MULTI }`. Note the rename `MULTIPLE` → `MULTI` (matches the locked decision in #67 body — terser and aligns with the M3 vocabulary).

```java
public enum SelectionMode {
  /** Selection is disabled; items never enter the selected state via list-driven interaction. */
  NONE,

  /**
   * Zero or one item selected — click an unselected item to select it; click the already-selected
   * item to deselect it. Filter-chip / toggle-style semantics. Card's previous SINGLE behavior
   * required external programmatic toggle; under the unified contract the click-to-deselect
   * applies uniformly.
   */
  SINGLE,

  /**
   * Exactly one item always selected — click-to-deselect is suppressed; clicking the already-
   * selected item is a no-op. Tab-strip / segmented-control / radio-group semantics. The list
   * auto-selects the first visible item whenever the mode is entered and no item is currently
   * selected; re-selects the first remaining item whenever a model change leaves the selection
   * empty. Inherited from Chip's superset.
   */
  SINGLE_MANDATORY,

  /**
   * Any number of items may be selected; Shift-click range and Cmd/Ctrl-click toggle behaviors
   * carry forward from both families.
   */
  MULTI
}
```

**Migration:** `CardSelectionMode.{NONE, SINGLE, MULTIPLE}` → `SelectionMode.{NONE, SINGLE, MULTI}`. `ChipSelectionMode.{NONE, SINGLE, SINGLE_MANDATORY, MULTIPLE}` → `SelectionMode.{NONE, SINGLE, SINGLE_MANDATORY, MULTI}`.

## 4. `ReorderAffordance` enum

Card and Chip each chose a different reorder UI affordance. Both are valid M3-adjacent idioms; the unified class lets the consumer pick.

```java
public enum ReorderAffordance {
  /**
   * Card's idiom — the whole item shows a grab cursor on hover (and grabbing cursor during a
   * drag) using the bundled Capitaine cursor PNGs. No persistent visual hint when not hovered.
   * The drag-and-drop is whole-item ({@link ReorderHandle#WHOLE_ITEM}) by default; the original
   * card-list value {@link ReorderHandle#TRAILING_HANDLE} is preserved for grip-only drag UIs.
   */
  CURSOR_SWAP,

  /**
   * Chip's idiom — a leading-slot drag handle icon appears on hover and stays during a drag.
   * The cursor stays default. Better for dense rows where a cursor swap is noisy; works well
   * with the pin / anchor leading-icon affordances since the same slot is repurposed only when
   * needed.
   */
  HOVER_ICON,

  /** Both — cursor swaps AND the leading-slot icon appears on hover. Maximum discoverability. */
  BOTH,

  /**
   * Neither — drag-to-reorder is still enabled by {@link ElwhaItemList#setReorderable(boolean)},
   * but no visual hint is added by the list. Use when the consumer's items carry their own
   * drag affordance.
   */
  NONE
}
```

**Default:** `CURSOR_SWAP` when the items are large (Card-like — uses Card's default); `HOVER_ICON` when items are small (Chip-like). Practical implementation: default is `CURSOR_SWAP` — if a consumer wants the chip-style hover icon, they pass it explicitly. Single default keeps the migration story simple.

**Migration:** Chip's old `setReorderable(true)` + `MovementMode.MOVABLE` → `setReorderable(true) + setReorderAffordance(ReorderAffordance.HOVER_ICON)` for visual parity. Card's old `setReorderable(true)` → unchanged (default affordance keeps cursor-swap).

## 5. Generic supporting types

12 generic classes replace 22 parallel `Card*` / `Chip*` types.

```java
// Adapter
@FunctionalInterface
public interface ElwhaListAdapter<T extends Component> {
  /** Builds the rendered component for the given item; never null. */
  T componentFor(Object item, int index);
}

// Model
public interface ElwhaListModel<T> {
  int size();
  T get(int index);
  void addListDataListener(ElwhaListDataListener<T> listener);
  void removeListDataListener(ElwhaListDataListener<T> listener);
}

public class DefaultElwhaListModel<T> implements ElwhaListModel<T> {
  public DefaultElwhaListModel();
  public DefaultElwhaListModel(java.util.Collection<? extends T> initial);
  public void add(T item);
  public void addAll(java.util.Collection<? extends T> items);
  public void remove(T item);
  public void remove(int index);
  public void clear();
  public void replaceAll(java.util.Collection<? extends T> items);
  // ... ElwhaListModel API
}

// Data events / listeners
public final class ElwhaListDataEvent<T> { /* index, item, kind: ADDED/REMOVED/CHANGED/RESET */ }
public interface ElwhaListDataListener<T> { void onChange(ElwhaListDataEvent<T> event); }

// Selection model / events / listeners
public interface ElwhaSelectionModel<T> {
  boolean isSelected(T item);
  java.util.List<T> getSelected();
  void setSelected(java.util.Collection<T> items);
  void clear();
  void addSelectionListener(ElwhaSelectionListener<T> listener);
  void removeSelectionListener(ElwhaSelectionListener<T> listener);
}

public class DefaultElwhaSelectionModel<T> implements ElwhaSelectionModel<T> { /* ... */ }

public final class ElwhaSelectionEvent<T> { /* selectedNow, selectedBefore */ }
public interface ElwhaSelectionListener<T> { void onChange(ElwhaSelectionEvent<T> event); }

// Reorder events / listeners
public final class ElwhaReorderEvent<T> { /* item, fromIndex, toIndex */ }
public interface ElwhaReorderListener<T> { void onReorder(ElwhaReorderEvent<T> event); }
```

**Note on parameter typing.** The locked epic says `ElwhaItemList<T extends Component>` (the component IS the item). For models, the items are typically domain objects (a `Cycle`, a `Factor`), and the adapter turns each into a component. The cleanest naming: keep `T` on the list bound to the *component* type (matches the spirit of the locked decision), and let the model/adapter generics use a separate type parameter or `Object` for the domain item.

Actually re-reading epic #67 carefully: the existing `ElwhaList<T>` interface uses `T` as the domain item (the same `T` consumers pass to `setFilter(Predicate<T>)` and the model). The epic title says `<T extends Component>` but the body's `getComponentFor(T)` doesn't make sense if `T` is already the component.

**Resolution:** `T` is the **domain item** type (matches existing `ElwhaList<T>`). The component is whatever the adapter produces. The bound `T extends Component` in the epic title is loose — the actual contract is that the adapter's `componentFor(T, int) -> Component` produces something the list can lay out. Drop the `T extends Component` bound; `T` is unconstrained, the *component* is what the adapter returns.

Updated:

```java
public class ElwhaItemList<T> extends JPanel implements ElwhaList<T> { ... }

@FunctionalInterface
public interface ElwhaListAdapter<T> {
  Component componentFor(T item, int index);
}
```

This keeps the existing `ElwhaList<T>` contract intact (the whole reason for keeping the interface verbatim) and avoids inventing a useless second type parameter. The Card and Chip lists today both follow this pattern; the generification is a true rename + collapse, not an API shape change.

## 6. `ElwhaItemList<T>` API surface

```java
public class ElwhaItemList<T> extends JPanel implements ElwhaList<T> {

  // Construction
  public ElwhaItemList(ElwhaListModel<T> model, ElwhaListAdapter<T> adapter);

  // Backing model + adapter
  public ElwhaListModel<T> getModel();
  public ElwhaSelectionModel<T> getSelectionModel();

  // Layout — implements ElwhaList<T>
  public ElwhaItemList<T> setOrientation(ElwhaListOrientation orientation);
  public ElwhaListOrientation getOrientation();
  public ElwhaItemList<T> setColumns(int columns);
  public int getColumns();
  public ElwhaItemList<T> setItemGap(int gap);
  public int getItemGap();
  public ElwhaItemList<T> setListPadding(Insets insets);
  public ElwhaItemList<T> setEmptyState(JComponent component);
  public ElwhaItemList<T> setLoading(boolean loading);
  public ElwhaItemList<T> setLoadingComponent(JComponent component);
  public ElwhaItemList<T> setFilter(Predicate<T> filter);
  public ElwhaItemList<T> setSortOrder(Comparator<T> comparator);

  // Selection — Chip's superset wins
  public ElwhaItemList<T> setSelectionMode(SelectionMode mode);
  public SelectionMode getSelectionMode();

  // Reorder — Card's WHOLE_ITEM / TRAILING_HANDLE handles + Chip's affordance
  public ElwhaItemList<T> setReorderable(boolean reorderable);
  public boolean isReorderable();
  public ElwhaItemList<T> setReorderHandle(ReorderHandle handle);
  public ReorderHandle getReorderHandle();
  public ElwhaItemList<T> setReorderAffordance(ReorderAffordance affordance);
  public ReorderAffordance getReorderAffordance();

  // Pin — Chip's superset, lifted
  public ElwhaItemList<T> setPinPredicate(Predicate<T> isPinned);
  public ElwhaItemList<T> setPinAction(BiConsumer<T, Boolean> action);
  public ElwhaItemList<T> setPinAffordance(PinAffordance affordance);
  public PinAffordance getPinAffordance();
  public boolean isPinned(T item);
  public void togglePin(T item);
  public void pinStateChanged();
  public JMenuItem createPinMenuItem(T item);

  // Anchor — Chip's superset, lifted
  public ElwhaItemList<T> setAnchorPredicate(Predicate<T> isAnchored);
  public ElwhaItemList<T> setAnchorAction(Consumer<T> action);
  public ElwhaItemList<T> setAnchorAffordance(AnchorAffordance affordance);
  public AnchorAffordance getAnchorAffordance();
  public boolean isAnchored(T item);
  public T getAnchoredItem();
  public void setAnchor(T item);
  public void clearAnchor();
  public void anchorStateChanged();
  public JMenuItem createAnchorMenuItem(T item);

  // Animation — Card's, lifted
  public ElwhaItemList<T> setAnimateChanges(boolean animate);
  public ElwhaItemList<T> setAnimationDuration(int ms);

  // Visible items — Chip's, lifted
  public List<T> getVisibleItems();

  // Item → component — generic rename
  public Component getComponentFor(T item);

  // Listeners
  public void addReorderListener(ElwhaReorderListener<T> listener);
  public void removeReorderListener(ElwhaReorderListener<T> listener);
}
```

`PinAffordance` and `AnchorAffordance` are renamed copies of `IconAffordance` (one each, for symmetry and to make the type-level distinction explicit). Values: `NONE / INDICATOR / BUTTON`.

`ReorderHandle` is preserved with one enum-value rename: `WHOLE_CARD` → `WHOLE_ITEM` (the others — `LEADING_ICON`, `TRAILING_HANDLE` — are unchanged).

## 7. Migration table

### `ElwhaCardList<T>` → `ElwhaItemList<T>`

| V1 | V2 |
|---|---|
| `new ElwhaCardList<>(model, adapter)` (with `CardListModel<T>` + `CardAdapter<T>`) | `new ElwhaItemList<>(model, adapter)` (with `ElwhaListModel<T>` + `ElwhaListAdapter<T>`) |
| `CardListModel<T>` / `DefaultCardListModel<T>` | `ElwhaListModel<T>` / `DefaultElwhaListModel<T>` |
| `CardAdapter<T>` — `cardFor(T, int)` | `ElwhaListAdapter<T>` — `componentFor(T, int)` |
| `CardListDataEvent` / `CardListDataListener` | `ElwhaListDataEvent<T>` / `ElwhaListDataListener<T>` |
| `CardSelectionMode.{NONE, SINGLE, MULTIPLE}` | `SelectionMode.{NONE, SINGLE, MULTI}` |
| `CardSelectionModel<T>` / `DefaultCardSelectionModel<T>` | `ElwhaSelectionModel<T>` / `DefaultElwhaSelectionModel<T>` |
| `CardSelectionEvent<T>` / `CardSelectionListener<T>` | `ElwhaSelectionEvent<T>` / `ElwhaSelectionListener<T>` |
| `CardReorderEvent<T>` / `CardReorderListener<T>` | `ElwhaReorderEvent<T>` / `ElwhaReorderListener<T>` |
| `getCardFor(T)` | `getComponentFor(T)` (returns `Component`; downcast if needed) |
| `setReorderHandle(ReorderHandle.WHOLE_CARD)` | `setReorderHandle(ReorderHandle.WHOLE_ITEM)` |
| `setReorderHandle(ReorderHandle.LEADING_ICON / TRAILING_HANDLE)` | unchanged |
| `ElwhaCardList.Orientation.VERTICAL` / `.GRID` | `ElwhaListOrientation.VERTICAL` / `.GRID` directly |

### `ElwhaChipList<T>` → `ElwhaItemList<T>`

| V1 | V2 |
|---|---|
| `new ElwhaChipList<>(model, adapter)` (with `ChipListModel<T>` + `ChipAdapter<T>`) | `new ElwhaItemList<>(model, adapter)` |
| `ChipListModel<T>` / `DefaultChipListModel<T>` | `ElwhaListModel<T>` / `DefaultElwhaListModel<T>` |
| `ChipAdapter<T>` — `chipFor(T, int)` | `ElwhaListAdapter<T>` — `componentFor(T, int)` |
| `ChipListDataEvent` / `ChipListDataListener` | `ElwhaListDataEvent<T>` / `ElwhaListDataListener<T>` |
| `ChipSelectionMode.{NONE, SINGLE, SINGLE_MANDATORY, MULTIPLE}` | `SelectionMode.{NONE, SINGLE, SINGLE_MANDATORY, MULTI}` |
| `ChipSelectionModel<T>` / `DefaultChipSelectionModel<T>` | `ElwhaSelectionModel<T>` / `DefaultElwhaSelectionModel<T>` |
| `ChipSelectionEvent<T>` / `ChipSelectionListener<T>` | `ElwhaSelectionEvent<T>` / `ElwhaSelectionListener<T>` |
| `ChipReorderEvent<T>` / `ChipReorderListener<T>` | `ElwhaReorderEvent<T>` / `ElwhaReorderListener<T>` |
| `getChipFor(T)` | `getComponentFor(T)` |
| `setMovementMode(MovementMode.STATIC)` | `setReorderable(false)` |
| `setMovementMode(MovementMode.MOVABLE)` | `setReorderable(true)` (default affordance is `CURSOR_SWAP`; pass `HOVER_ICON` for chip-parity) |
| `setMovementMode(MovementMode.PINNED)` | `setReorderable(true)` + `setPinPredicate(...)` |
| `setMovementMode(MovementMode.ANCHORED)` | `setReorderable(true)` + `setAnchorPredicate(...)` |
| `setPinAffordance(IconAffordance.X)` / `getPinAffordance()` | `setPinAffordance(PinAffordance.X)` / `getPinAffordance()` |
| `setAnchorAffordance(IconAffordance.X)` / `getAnchorAffordance()` | `setAnchorAffordance(AnchorAffordance.X)` / `getAnchorAffordance()` |
| `setReorderable(true)` alone (no movement mode) | `setReorderable(true) + setReorderAffordance(ReorderAffordance.HOVER_ICON)` for visual parity |
| Pin / anchor methods (6 + 8) | unchanged signatures, on the unified class |
| `getVisibleItems()` | unchanged |

### Drop without replacement

- `ElwhaCardList.Orientation` inner class (#70 deletion)
- Chip-only `MovementMode` enum — folded into `setReorderable` + predicates
- Chip-only `IconAffordance` enum — replaced by two type-distinct affordance enums (`PinAffordance`, `AnchorAffordance`)

## 8. `ReorderHandle` mapping

```java
public enum ReorderHandle {
  /** Drag from anywhere on the item. Default; equivalent to the old WHOLE_CARD value. */
  WHOLE_ITEM,
  /** Drag from the item's leading icon slot only. */
  LEADING_ICON,
  /** Drag from a dedicated trailing-edge grip. */
  TRAILING_HANDLE
}
```

`WHOLE_CARD` is a static final alias on `ReorderHandle` for the duration of one minor version? **No** — pre-1.0, no shims. Callers update.

## 9. Cross-references

### #62 doctrine

| Rule | `ElwhaItemList<T>` |
|---|---|
| §1 Getter naming — `getX()` only | `getSelectionMode()`, `getOrientation()`, `getColumns()`, `getItemGap()`, `getReorderHandle()`, `getReorderAffordance()`, `getPinAffordance()`, `getAnchorAffordance()`, `getModel()`, `getSelectionModel()`, `getVisibleItems()`, `getAnchoredItem()`, `getComponentFor(T)`. No `getEffectiveX()` anywhere. |
| §2 Per-variant static factories | **N/A** — `ElwhaItemList<T>` has no variant axis. |
| §3 Single-arg convenience ctor | **N/A** — `(model, adapter)` is the natural pair; no single-arg shorthand makes sense (a list without an adapter has no rendering contract). |
| §4 Border-role exposure on variant-bearing | **N/A** — list container isn't a variant-bearing painted surface. |
| §5 Symmetric border-width | **N/A** — list container has no border API today. |

### #63 (Card V2) coordination

Per epic #67: "Independent of #63 — Card V2 changes Card's internals; this changes the list container." However the playground migration in #70 will use `ElwhaCard` V2 since #63 is sequenced first. Story #69's implementation can begin in parallel with #65; story #70 needs #66 done.

### `ElwhaList<T>` interface

The narrow `com.owspfm.elwha.list.ElwhaList<T>` interface is **unchanged**. Every method on it (orientation / gap / padding / empty / loading / filter / sort) remains exactly as today. `ElwhaItemList<T>` implements it the same way `ElwhaCardList<T>` and `ElwhaChipList<T>` do today. Callers using `ElwhaList<T>` as a parameter type need no changes.

## 10. Out of scope for v0.1

- **`ElwhaCarousel<T>`** — explicitly deferred per epic #67. Does not implement `ElwhaList<T>` as part of this epic; ships standalone when filed.
- **New aggregate primitives (Grid as a sibling)** — `ElwhaListOrientation.GRID` already covers grid as a list mode.
- **Adapter for `ElwhaIconButton`** as a future list-family member — not in v0.1; mentioned only to motivate the generification.

## 11. Implementation guard-rails (#69 reads first)

- **Add `ElwhaItemList<T>` alongside the legacy `Card*` / `Chip*` classes.** Do NOT delete the legacy classes in #69 — that is #70's job. The point is to land the new stack independently and let #70 sweep up after the playground migrations land.
- **Pre-1.0 — no compat shims.** Don't add `@Deprecated` legacy aliases on the new classes for the old names. The migration is one-shot; deletion is #70.
- **`ReorderHandle.WHOLE_CARD` rename to `WHOLE_ITEM`** is a hard rename, not an alias. Callers update in #70.
- **`PinAffordance` and `AnchorAffordance` are two enums** with identical value lists. They mean the same thing visually but type-distinguish so a future divergence (e.g., anchor-only `STATIC_INDICATOR` value) doesn't require a renaming pass.
- **Cursor PNG resources** — Card today loads grab / grabbing PNGs from `src/main/resources/com/owspfm/elwha/card/list/cursors/`. The unified `ReorderAffordance.CURSOR_SWAP` and `BOTH` modes need them. Move to `src/main/resources/com/owspfm/elwha/list/cursors/` in #69 and update the loader's path. The `Cursors.java` helper class either moves to the new package or folds into `ElwhaItemList` internals — pick the smaller blast-radius option during implementation; document in the PR.
- **Don't change `ElwhaList<T>`.** That contract is locked.
- **Don't break the per-family playgrounds at compile** — story #69 lands the generic stack and minimally migrates the playgrounds. Story #70 does full polish and deletes the legacy classes.
