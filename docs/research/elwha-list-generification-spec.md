# ElwhaItemList&lt;T&gt; — list-family generification spec (#68 → epic #67)

**Status:** SPEC — locked for #69 unless a finding below forces an operator call · **Date:** 2026-08-07
· **Supersedes:** the per-family surfaces of `card/list/` and `chip/list/` · **Gates satisfied:**
#62 (API conventions — closed; conformance in §10), #63 (Card V3 — shipped; this spec is written
against V3, not the V1 the epic's table was drafted from)

This is the S1 research deliverable of epic #67: the full Card/Chip list audit, the unified
`ElwhaItemList<T>` surface, the two locked enums, the generic model/event/listener signatures, and
the migration tables. No source changes in this story.

---

## 1. Corrections to the epic's premises (audited 2026-08-07)

The epic's superset table was drafted against the **V1** card list. Four premises need restating
against what actually ships:

1. **"Reorder event/listener identical on both" is false for V3.** `ElwhaCardList` (V3) has *no*
   reorder event, listener, or registration method — reorder commits silently through
   `model.move()`. The identical `Card*`/`Chip*` event pair exists only between `card/v1` and
   chip. The unified `ElwhaReorderEvent<T>`/`ElwhaReorderListener<T>` therefore derive from
   chip's (`ChipReorderEvent` — item + fromIndex + toIndex), which V1's happened to mirror.
2. **"Animation — Card's wins" refers to V1-only API.** `setAnimateChanges` /
   `setAnimationDuration` / `getCardFor` live at `card/v1/list/ElwhaCardList.java:541,554,592`.
   V3 has no animation of any kind. The unified class *re-implements* the V1 fade-in contract
   (it is not a lift from live code).
3. **The `Cursors` blocker (#96) is in both families.** `chip/list/ElwhaChipList.java:3` imports
   `com.owspfm.elwha.card.v1.list.Cursors` exactly as `card/list/ElwhaCardList.java:4` does.
   §7 resolves this structurally.
4. **Chip anchor is 8 methods + 4 affordance accessors** (the epic said 3), and pin is 6 + the
   same 4 affordance accessors. All lift.

One drafting slip in the epic header, flagged rather than silently followed: the class is written
there as `ElwhaItemList<T extends Component>`, but the epic's own table generalizes `getCardFor`
to `getComponentFor(T)` — i.e. `T` is the **domain item type**, mapped to a component by an
adapter. That is also how both shipped families and all four real consumers work. **This spec
fixes `T` as the unbounded item type**; the component type is `JComponent` via the adapter.

## 2. Audit summary

Full inventory (2026-08-07, `main` @ `c347584`): `list/` 3 files / 222 LOC (interface + orientation
enum), `card/list/` 6 files / 1,038 LOC, `chip/list/` 14 files / 3,373 LOC. Chip is the richer
family almost everywhere; card's list is a fifth the size and `final`.

| Concern | Card V3 | Chip | Unified takes |
|---|---|---|---|
| `ElwhaList<T>` 12-method contract | ✔ | ✔ (+ no-op guards on setters) | chip's no-op-guarded form |
| Model | mutators **on the interface**, untyped `Consumer` callback | read-only interface + mutators only on `Default…`, **typed events** | card's mutator placement + chip's typed events (§5) |
| Selection model | `final` **class**, `Set`-based, mode lives on the model, fires unconditionally | **interface** + default impl, identity-based, change-guarded, typed events | chip's shape; mode accessor on the list (§4) |
| Selection semantics | naive per-item toggle; no modifiers; no mandatory auto-seed; **filter destroys selection** (`retainAll` post-filter); **stale chrome visuals in SINGLE** | modifier handling (range/toggle/collapse), mandatory auto-seeds, selection survives filter/sort by identity, visuals synced per press | chip's, wholesale — fixes both card bugs |
| Reorder engine | drag the card itself + 2dp `PRIMARY` drop-indicator bar; siblings static; live-position slotting | siblings animate into the gap; natural-position slotting (anti-oscillation); partition clamps; release-point "throw" fix | chip's engine + card's `setDragged` chrome lift (§6) |
| Reorder keyboard | per-card Cmd+↑/↓ move, Delete | list-level arrows/Home/End/Space/Enter/Ctrl-A nav+activate | **union** (§6) |
| Context menu | built-in Move/Delete menu, always-on, no opt-out | none built-in; opt-in Pin/Anchor injection when caller has no menu | chip's injection rule, applied to a reorder menu too (§6) |
| Pin / anchor / movement modes | — | `MovementMode` {STATIC, MOVABLE, PINNED, ANCHORED}, 6 pin + 8 anchor methods + 4 affordance accessors | lift verbatim (§8) |
| Empty/loading | null component ⇒ silently renders nothing (violates the interface's "null restores default") | built-in "No items to show" / "Loading…" defaults | chip's built-ins |
| Layout | stock Box/Grid/Flow layouts + **filler struts as real children** | four hand-written layout managers (drag-aware) | chip's (kills strut pollution; enables RTL) |
| Item→component lookup | none on V3 (private map) | `getChipFor(T)` | `getComponentFor(T)` (epic-locked name) |
| `getVisibleItems()` | — | ✔ | lift |
| A11y | none — inherits PANEL, struts appear as accessible children (regression vs V1) | `LIST` role + visible-children mapping | chip's + `AccessibleSelection` (new, §9) |
| RTL | absent | absent | **new requirement on #69** (§9) |

Parallel classes: 20 in the two families (6 card + 14 chip) collapse to the 13 of §3 — plus
`card/v1/list`'s 12 leave via #96.

## 3. The unified family — class census

All in `com.owspfm.elwha.list` (the interface's package becomes the family's home):

```
list/
  ElwhaList<T>                     (existing interface — unchanged, still the cross-cutting contract)
  ElwhaListOrientation             (existing enum — unchanged)
  ElwhaItemList<T>                 the one concrete list  [extends JPanel, implements Accessible, ElwhaList<T>]
  ElwhaItemAdapter<T>              @FunctionalInterface  JComponent componentFor(T item, int visibleIndex)
  ElwhaListItemView                capability interface hosted components may implement (§3a)
  ElwhaListModel<T>                model interface — read + mutators + typed listeners (§5)
  DefaultElwhaListModel<T>         ArrayList implementation
  ElwhaListDataEvent<T>            typed {ADDED, REMOVED, CHANGED, MOVED, STRUCTURE} + index0/index1
  ElwhaListDataListener<T>         @FunctionalInterface
  ElwhaSelectionModel<T>           selection interface (§4)
  DefaultElwhaSelectionModel<T>    LinkedHashSet identity-based implementation
  ElwhaSelectionEvent<T>           snapshot event
  ElwhaSelectionListener<T>        @FunctionalInterface
  ElwhaReorderEvent<T>             item + fromIndex + toIndex (model indices)
  ElwhaReorderListener<T>          @FunctionalInterface
  SelectionMode                    {NONE, SINGLE, SINGLE_MANDATORY, MULTIPLE}   (top-level, §4)
  MovementMode                     {STATIC, MOVABLE, PINNED, ANCHORED}          (top-level — no longer nested)
  IconAffordance                   {NONE, INDICATOR, BUTTON}                    (top-level — no longer nested)
  ReorderAffordance                {CURSOR_SWAP, HOVER_ICON, BOTH, NONE}        (§7)
  cursors/  (resources)            the reorder cursor assets — relocated from card/v1 (§7)
```

Naming note: the epic wrote the multi constant as `MULTI`; both shipped enums say `MULTIPLE` and
every consumer types it. **`MULTIPLE` stays** (flagged as epic drift, not re-litigated substance).

### 3a. `ElwhaListItemView` — the component capability interface

The list hosts arbitrary `JComponent`s, but selection/drag polish needs component cooperation.
Both `ElwhaCard` and `ElwhaChip` *already* share the load-bearing signatures — the interface
formalizes the de-facto contract; they implement it with near-zero code:

```java
public interface ElwhaListItemView {
  void setSelected(boolean selected);      // push model state onto the chrome (both have it)
  boolean isSelected();
  void cancelPendingClick();               // drag-won-over-click suppression (both have it)
  void setListInteractive(boolean interactive);  // card → setSelectable-off + clickable; chip → CLICKABLE mode
  default void setDragged(boolean dragged) {}    // card lifts elevation + DRAGGED layer; chip default no-op
}
```

A hosted component that does **not** implement it still works — it just gets no selection visuals,
no drag chrome, and click/drag disambiguation falls back to the list's own threshold logic.

**Selection ownership is unified on the chip pattern: the list owns it.** The list handles the
press, consults `SelectionMode`, mutates the model, and pushes state onto every rendered view
(`setSelected`). The card family's inverted wiring (chrome self-toggles, list observes) is what
produced both audited bugs — the stale-visual SINGLE case and the filter-kills-selection case —
and it does not survive.

## 4. Selection

```java
public ElwhaItemList<T> setSelectionMode(SelectionMode mode);   // on the list (chip locus)
public SelectionMode getSelectionMode();
public ElwhaSelectionModel<T> getSelectionModel();
public ElwhaItemList<T> setSelectionModel(ElwhaSelectionModel<T> model);   // interface, swappable

public interface ElwhaSelectionModel<T> {
  boolean isSelected(T item);
  List<T> getSelected();                       // insertion-ordered snapshot
  void setSelected(List<T> selected);          // null ⇒ empty
  void add(T item);   void remove(T item);   void toggle(T item);   void clearSelection();
  void addSelectionListener(ElwhaSelectionListener<T> listener);
  void removeSelectionListener(ElwhaSelectionListener<T> listener);
}
```

Mode semantics (chip's, verbatim — they are the strict superset):

- **NONE** — items not selectable; clicks still fire item actions.
- **SINGLE** — toggleable: clicking the selected item deselects it.
- **SINGLE_MANDATORY** — tab-strip semantics: clicking the selected item is a no-op, and the list
  **auto-seeds** the first visible item whenever selection is empty (on mode entry and after every
  model/filter change).
- **MULTIPLE** — plain click collapses to the clicked item; Cmd/Ctrl-click toggles;
  Shift-click extends a range over *visible* order; Ctrl/Cmd-A selects all visible.

Selection is **identity-based and survives filter/sort** (the model never drops entries because
they scrolled out of the visible set). Events are change-guarded — no fire when nothing changed.

## 5. Model

Card's mutator placement (interface-level — the audit showed chip's `instanceof
DefaultChipListModel` reorder guard makes custom chip models silently un-reorderable, and card's
design has no such trap) merged with chip's typed event layer:

```java
public interface ElwhaListModel<T> extends Iterable<T> {
  int getSize();          T getElementAt(int index);
  List<T> getItems();     // unmodifiable snapshot
  void setItems(List<T> items);
  void add(T item);       void add(int index, T item);    void addAll(Collection<? extends T> items);
  boolean remove(T item); T remove(int index);            T set(int index, T item);
  void move(int fromIndex, int toIndex);                  void clear();
  boolean contains(T item);                               int indexOf(T item);
  void addListDataListener(ElwhaListDataListener<T> listener);
  void removeListDataListener(ElwhaListDataListener<T> listener);
}
```

`DefaultElwhaListModel<T>` implements everything over an `ArrayList` (two ctors: `()`,
`(List<T>)`). A read-only custom model may throw `UnsupportedOperationException` from mutators —
the `java.util.List` precedent — and the list degrades gracefully: reorder and keyboard-delete
disable when a mutation throws is *not* acceptable mid-gesture, so `ElwhaItemList` probes
capability once per model install (documented contract: models that support reordering implement
`move`; the list exposes `setReorderable`/`MovementMode` regardless and no-ops with a `FINE` log
when the model refuses — never a hard failure, never an `instanceof` on a concrete class).

`ElwhaListDataEvent<T>`: `{ADDED, REMOVED, CHANGED, MOVED, STRUCTURE}` + `getIndex0()` /
`getIndex1()` (MOVED carries from→to; STRUCTURE covers `setItems`/`clear`).

## 6. Reorder

Engine: **chip's**, which is the polished superset — siblings animate into the opening gap
(30%-per-tick, 16ms), natural-preferred-position slotting (immune to the mid-animation feedback
oscillation the chip javadoc documents), partition clamps for pin/anchor, the release-point
"throw" recomputation, and Z-order restoration on no-op drops. Card's contributions that carry
over: the `setDragged` chrome lift via `ElwhaListItemView` (elevation + DRAGGED state layer on
components that support it) and drag-bounds clamping so shadow halos stay inside the list.

Card's 2dp drop-indicator bar is **retired**: its function (communicating the drop target) is
performed by the live gap animation; under reduced motion the gap snaps instantly, which still
marks the target. Recorded as the one place unification picks a single visual idiom — sanctioned
by the same logic the epic already applied to affordances.

Events fire with **model** indices: `ElwhaReorderEvent<T>(source, item, fromIndex, toIndex)` after
the model mutation commits.

Keyboard is the **union** of the two disjoint surfaces:

| binding | action | from |
|---|---|---|
| ↑/←, ↓/→, Home, End | move focus across visible items (wraps per orientation) | chip |
| Space / Enter | activate ≙ selection press with no modifiers | chip |
| Ctrl/Cmd+A | select all visible (MULTIPLE only) | chip |
| Cmd/Ctrl+↑, Cmd/Ctrl+↓ | reorder the focused item (when reorderable) | card |
| Delete, Cmd/Ctrl+Backspace | delete the focused item (when the model mutates) | card |

Context menu: card's always-on built-in does not survive; the chip injection rule generalizes —
the list injects a Move up / Move down / Delete section **only when** the item's component carries
no caller-attached menu, alongside the existing Pin/Anchor injection. `createPinMenuItem` /
`createAnchorMenuItem` stay for callers composing their own menus; a matching
`createReorderMenuItems(T)` joins them.

## 7. ReorderAffordance + the cursor assets (resolves the #96 blocker)

```java
public enum ReorderAffordance { CURSOR_SWAP, HOVER_ICON, BOTH, NONE }
public ElwhaItemList<T> setReorderAffordance(ReorderAffordance affordance);
public ReorderAffordance getReorderAffordance();
```

- **CURSOR_SWAP** (default — today's behavior on both families): grab cursor on hover over a
  draggable item, grabbing during the drag, symmetric restore (the audit caught card restoring to
  the *default* cursor after a drop — the unified restore is always to the hover-state cursor).
- **HOVER_ICON**: an M3 drag-handle glyph revealed on hover in the item's leading region. Note
  honestly: this is a **new** capability — the epic's premise that chip already had a
  hover-revealed *reorder* icon conflated `IconAffordance`, which governs pin/anchor glyphs. The
  enum is locked, so #69 builds the handle; `MaterialIcons` already bundles a suitable glyph
  family.
- **BOTH** / **NONE** — compose / suppress.

**Cursor assets move home.** The grab/grabbing cursors and their loader become
`list/ReorderCursors` (package-private) + `resources/com/owspfm/elwha/list/cursors/`, keeping the
three-tier fallback (bundled PNG → Java2D-painted silhouette → `MOVE_CURSOR`), the
luminance-driven theme variant, and the design-space hotspots. This removes the
`card.v1.list.Cursors` import from **both** live families — after #69+#70, `card/v1` has zero
inbound code dependencies. Asset content is owned by **#531** (operator-directed replacement of
the Capitaine PNGs with a high-quality redesigned set; `CURSOR_SWAP` survives per the 2026-08-06
decision). Sequencing: #69 relocates the *loader* and whatever assets exist; #531 swaps the PNGs
in place whenever it lands — the two do not block each other. **#96 must also sweep the seven
javadoc `{@link}` references to `card.v1` types** (now build-breaking — doclint errors are fatal
since #545): `card/package-info.java:11`, `card/list/package-info.java:8`,
`list/package-info.java:3`, `chip/list/ChipListDataEvent.java:8`,
`chip/list/DefaultChipListModel.java:12`, `chip/list/ChipListModel.java:9`,
`chip/list/ElwhaChipList.java:2252` — the chip ones vanish with #70's deletions; the
package-infos need edits.

## 8. Pin, anchor, movement — lifted verbatim

`MovementMode` semantics, the implicit mode flips (`setPinPredicate(non-null)` ⇒ PINNED,
`setAnchorPredicate(non-null)` ⇒ ANCHORED, PINNED/ANCHORED mutually exclusive with predicate
nulling at FINE), `setReorderable` back-compat mapping, the caller-owns-state contract
(`pinStateChanged()` / `anchorStateChanged()` invalidation hooks), per-partition comparator
application, partition drag clamps, and all 14 pin/anchor methods + 4 affordance accessors carry
over with `Chip` struck from the names (`getComponentFor` replaces `getChipFor`;
`createPinMenuItem(T)` unchanged). `IconAffordance` glyphs apply through `ElwhaChip`'s leading
affordance today; on the generic list they apply when the adapter's component is an `ElwhaChip`
(same behavior), and are documented inert otherwise until a future capability extends them — no
feature loss, no invented generalization.

Comparator-active reorder stays disabled (`canReorder` requires no active sort), and the audited
dead-code warning branch is not carried over.

## 9. Cross-cutting requirements on #69

- **Empty/loading built-ins** (chip's) — honoring the interface's "null restores the built-in
  default," which card currently violates (silent no-op `setLoading(true)`).
- **A11y:** `AccessibleRole.LIST`, visible-children mapping (chip baseline), **plus
  `AccessibleSelection`** (new — both families lack it; the #438 suite's a11y dimension expects
  it). No filler struts as accessible children — gaps live in the layout managers.
- **RTL:** absent from every current layout path (audited: zero `ComponentOrientation` references
  in any list package). The four hand-written layout managers make it implementable; #69 treats
  RTL mirroring as a first-class requirement (M3 + the #438 suite's RTL dimension both demand it).
- **Reduced motion:** gap animation and any fade snap under `MorphAnimator.isReducedMotion()`.
- **Animation (V1 contract re-implemented):** `setAnimateChanges(boolean)` +
  `setAnimationDuration(int)` — add/remove fade-in per V1's semantics (≥50ms clamp), default off.
- The `ElwhaList<T>` interface itself is untouched (epic-locked). Its getter asymmetry (no
  `getListPadding`/`isLoading`/`getFilter`/`getSortOrder`) is noted for #440's API review — not
  changed here.
- Javadoc to the #529 house style from day one (`@serial exclude`, full member docs — new code
  carries no debt).

## 10. #62 conventions conformance

- `getX()`-only naming throughout; no `getEffectiveX` anywhere in the surface. ✔
- No `Variant` enum ⇒ no static factories (per doctrine). Constructor:
  `ElwhaItemList(ElwhaListModel<T> model, ElwhaItemAdapter<T> adapter)` — both required,
  `NullPointerException` via `Objects.requireNonNull` (card's exception type; chip's IAE loses).
  Adapter is swappable post-construction (`setAdapter`) — card's settable-renderer capability wins
  over chip's final field; a swap triggers a full rebuild.
- Container role (§6 of the conventions): model/adapter composition, chrome-only concerns stay on
  the hosted components. ✔
- Fluent setters return `ElwhaItemList<T>` (covariant over `ElwhaList<T>`), matching both current
  implementations. ✔
- Symmetric accessors for every new axis introduced here (`getSelectionMode`, `getMovementMode`,
  `getReorderAffordance`, `getPinAffordance`, `getAnchorAffordance`, `isReorderable`). ✔

## 11. Migration tables (for #70)

Real consumers are exactly four classes (audited): `showcase/CardListContainer`,
`card/playground/ElwhaCardListShowcase` (mounted by `ElwhaCardPlayground` + `ThemePlayground`),
`showcase/ChipListContainer`, `chip/playground/ChipPlaygroundPanels`.

**Card V3 → unified**

| today | becomes |
|---|---|
| `new DefaultCardListModel<>(items)` | `new DefaultElwhaListModel<>(items)` |
| `new ElwhaCardList<>(model)` + `setCellRenderer(f)` | `new ElwhaItemList<>(model, (item, i) -> f.apply(item))` |
| `setCellRenderer(Function<T, ElwhaCard>)` | `setAdapter(ElwhaItemAdapter<T>)` (index param added) |
| `getSelectionModel().setSelectionMode(CardSelectionMode.X)` | `setSelectionMode(SelectionMode.X)` (list-level) |
| `getSelectionModel().getSelectedItems()` → `Set<T>` | `getSelectionModel().getSelected()` → `List<T>` |
| `getSelectionModel().addChangeListener(Consumer)` | `addSelectionListener(ElwhaSelectionListener<T>)` |
| `model.addChangeListener(Consumer)` | `model.addListDataListener(ElwhaListDataListener<T>)` |
| built-in context menu (implicit) | injected only when the component has no caller menu |
| `ElwhaCardList.Orientation` (V1 leftover) | `ElwhaListOrientation` directly |

**Chip → unified**

| today | becomes |
|---|---|
| `new ElwhaChipList<>(model, adapter)` | `new ElwhaItemList<>(model, adapter)` (adapter shape unchanged) |
| `ChipListModel` custom impls | implement `ElwhaListModel` (add mutators or throw UOE) |
| `DefaultChipListModel` | `DefaultElwhaListModel` (same mutator set + interface promotion) |
| `ChipSelectionMode.X` | `SelectionMode.X` (same constants) |
| `getChipFor(T)` | `getComponentFor(T)` (returns `JComponent`) |
| `addReorderListener(ChipReorderListener)` | `addReorderListener(ElwhaReorderListener)` (same shape) |
| `ElwhaChipList.MovementMode` / `.IconAffordance` | top-level `MovementMode` / `IconAffordance` (import change only) |
| pin/anchor/affordance methods | unchanged names |

Everything else (the `ElwhaList<T>` contract) migrates by find/replace of the class name.

## 12. Sequencing into #69 / #70

1. **#69** builds the `list/` family + `ElwhaListItemView` implementations on `ElwhaCard` /
   `ElwhaChip`, relocates the cursor loader/assets, and ships a fresh interactive demo per the
   per-story-demo rule. Test-suite coordination: **#438-S7 tests the unified class** — its
   model/selection specs become #69's regression net; write them against this spec's semantics.
2. **#70** migrates the four consumers, deletes the 20 parallel classes + the V1 leftover
   `Orientation` re-export, sweeps the two live-family javadoc refs, and lands the CHANGELOG
   "Changed" entry (pre-1.0 break, per policy).
3. **#96** deletes `card/v1` afterward — by then its only inbound references are the two
   package-info `{@link}`s, swept in the same PR.

Operator smoke expected at the #69 boundary (selection/drag/pin/anchor across all four
orientations, LTR+RTL, reduced motion on/off) — the phase plan will restate it at handoff.
