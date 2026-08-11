package com.owspfm.elwha.list;

import java.util.Collection;
import java.util.List;

/**
 * Observable, ordered collection of items rendered by an {@link ElwhaItemList}.
 *
 * <p>The unified successor to the card and chip families' parallel model interfaces (spec §5). It
 * merges the card side's decision to declare mutators <em>on the interface</em> with the chip
 * side's typed event layer. Declaring the mutators here is what lets a custom model participate in
 * drag-reorder: the chip list gated reorder behind {@code instanceof DefaultChipListModel}, which
 * silently made every custom model un-reorderable.
 *
 * <p><strong>Read-only implementations.</strong> A model that does not support mutation may throw
 * {@link UnsupportedOperationException} from any mutator — the {@link java.util.List} precedent.
 * {@code ElwhaItemList} probes {@link #move(int, int)} once per model install rather than mid-
 * gesture, and degrades by disabling drag and keyboard reorder with a {@code FINE} log. Refusing a
 * mutation is never a hard failure.
 *
 * <p>Implementations are not required to be thread-safe; mutate them on the Swing EDT.
 *
 * @param <T> the item type
 * @author Charles Bryan
 * @version v0.5.0
 * @since v0.5.0
 */
public interface ElwhaListModel<T> extends Iterable<T> {

  /**
   * Returns the current item count.
   *
   * @return the number of items in the model
   * @version v0.5.0
   * @since v0.5.0
   */
  int getSize();

  /**
   * Returns the item at the given index.
   *
   * @param index the zero-based index
   * @return the item at that index
   * @throws IndexOutOfBoundsException if {@code index} is out of range
   * @version v0.5.0
   * @since v0.5.0
   */
  T getElementAt(int index);

  /**
   * Returns an unmodifiable snapshot of the items in model order. The snapshot does not track
   * subsequent mutations.
   *
   * @return the items, in model order; never null
   * @version v0.5.0
   * @since v0.5.0
   */
  List<T> getItems();

  /**
   * Replaces the entire contents, firing one {@link ElwhaListDataEvent.Type#STRUCTURE} event.
   *
   * @param items the new contents; null is treated as empty
   * @throws UnsupportedOperationException if the model is read-only
   * @version v0.5.0
   * @since v0.5.0
   */
  void setItems(List<T> items);

  /**
   * Appends an item, firing one {@link ElwhaListDataEvent.Type#ADDED} event.
   *
   * @param item the item to append; null is permitted
   * @throws UnsupportedOperationException if the model is read-only
   * @version v0.5.0
   * @since v0.5.0
   */
  void add(T item);

  /**
   * Inserts an item, firing one {@link ElwhaListDataEvent.Type#ADDED} event.
   *
   * @param index the insertion index, in {@code [0, getSize()]}
   * @param item the item to insert; null is permitted
   * @throws IndexOutOfBoundsException if {@code index} is out of range
   * @throws UnsupportedOperationException if the model is read-only
   * @version v0.5.0
   * @since v0.5.0
   */
  void add(int index, T item);

  /**
   * Appends a batch, firing one {@link ElwhaListDataEvent.Type#ADDED} event covering the inserted
   * range.
   *
   * @param items the items to append; null or empty is a no-op that fires nothing
   * @throws UnsupportedOperationException if the model is read-only
   * @version v0.5.0
   * @since v0.5.0
   */
  void addAll(Collection<? extends T> items);

  /**
   * Removes the first occurrence of the given item, firing one {@link
   * ElwhaListDataEvent.Type#REMOVED} event when a match is found.
   *
   * @param item the item to remove
   * @return true if a matching item was found and removed
   * @throws UnsupportedOperationException if the model is read-only
   * @version v0.5.0
   * @since v0.5.0
   */
  boolean remove(T item);

  /**
   * Removes the item at the given index, firing one {@link ElwhaListDataEvent.Type#REMOVED} event.
   *
   * @param index the index to remove
   * @return the removed item
   * @throws IndexOutOfBoundsException if {@code index} is out of range
   * @throws UnsupportedOperationException if the model is read-only
   * @version v0.5.0
   * @since v0.5.0
   */
  T remove(int index);

  /**
   * Replaces the item at the given index, firing one {@link ElwhaListDataEvent.Type#CHANGED} event.
   *
   * @param index the index to replace
   * @param item the replacement item
   * @return the previous item at that index
   * @throws IndexOutOfBoundsException if {@code index} is out of range
   * @throws UnsupportedOperationException if the model is read-only
   * @version v0.5.0
   * @since v0.5.0
   */
  T set(int index, T item);

  /**
   * Moves one item to a new index, firing one {@link ElwhaListDataEvent.Type#MOVED} event.
   * Destination indices are interpreted post-removal, matching {@code List.remove} followed by
   * {@code List.add}. A no-op when the indices are equal.
   *
   * <p>This is the method {@code ElwhaItemList} probes to decide whether drag-reorder and the
   * keyboard move bindings are available.
   *
   * @param fromIndex the source index
   * @param toIndex the destination index
   * @throws IndexOutOfBoundsException if either index is out of range
   * @throws UnsupportedOperationException if the model does not support reordering
   * @version v0.5.0
   * @since v0.5.0
   */
  void move(int fromIndex, int toIndex);

  /**
   * Removes every item, firing one {@link ElwhaListDataEvent.Type#STRUCTURE} event. A no-op on an
   * already-empty model.
   *
   * @throws UnsupportedOperationException if the model is read-only
   * @version v0.5.0
   * @since v0.5.0
   */
  void clear();

  /**
   * Returns whether the model contains the given item.
   *
   * @param item the item to look for
   * @return true if present
   * @version v0.5.0
   * @since v0.5.0
   */
  boolean contains(T item);

  /**
   * Returns the index of the first occurrence of the given item.
   *
   * @param item the item to look for
   * @return the index, or {@code -1} if not present
   * @version v0.5.0
   * @since v0.5.0
   */
  int indexOf(T item);

  /**
   * Registers a listener notified after every content change.
   *
   * @param listener the listener; null and duplicates are ignored
   * @version v0.5.0
   * @since v0.5.0
   */
  void addListDataListener(ElwhaListDataListener<T> listener);

  /**
   * Removes a previously registered listener.
   *
   * @param listener the listener to remove; unknown listeners are ignored
   * @version v0.5.0
   * @since v0.5.0
   */
  void removeListDataListener(ElwhaListDataListener<T> listener);
}
