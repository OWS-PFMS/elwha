package com.owspfm.elwha.list;

import java.util.List;

/**
 * Tracks selection state for an {@link ElwhaItemList}.
 *
 * <p>Selection is keyed on item <strong>identity</strong>, not index, so it survives filter and
 * sort changes: an item that scrolls out of the visible set stays selected and comes back selected.
 * The card family's index-and-retain approach lost selection on every filter change; this contract
 * exists so that cannot happen.
 *
 * <p>The mode ({@link SelectionMode}) lives on the list, not here — the model stores what is
 * selected, the list decides what a gesture means.
 *
 * @param <T> the item type
 * @author Charles Bryan
 * @version v0.5.0
 * @since v0.5.0
 */
public interface ElwhaSelectionModel<T> {

  /**
   * Returns whether the given item is currently selected.
   *
   * @param item the item to test
   * @return true if selected
   * @version v0.5.0
   * @since v0.5.0
   */
  boolean isSelected(T item);

  /**
   * Returns the current selection as an insertion-ordered snapshot. The snapshot does not track
   * subsequent changes.
   *
   * @return the selected items, in the order they were selected; never null
   * @version v0.5.0
   * @since v0.5.0
   */
  List<T> getSelected();

  /**
   * Replaces the entire selection, firing one event if the result differs from the previous
   * contents.
   *
   * @param selected the new selection; null is treated as empty
   * @version v0.5.0
   * @since v0.5.0
   */
  void setSelected(List<T> selected);

  /**
   * Adds an item to the selection, firing one event. A no-op, and silent, if already selected.
   *
   * @param item the item to select
   * @version v0.5.0
   * @since v0.5.0
   */
  void add(T item);

  /**
   * Removes an item from the selection, firing one event. A no-op, and silent, if not selected.
   *
   * @param item the item to deselect
   * @version v0.5.0
   * @since v0.5.0
   */
  void remove(T item);

  /**
   * Flips the given item's membership, firing one event.
   *
   * @param item the item to toggle
   * @version v0.5.0
   * @since v0.5.0
   */
  void toggle(T item);

  /**
   * Empties the selection, firing one event. A no-op, and silent, if already empty.
   *
   * @version v0.5.0
   * @since v0.5.0
   */
  void clearSelection();

  /**
   * Registers a listener notified after every change to the selection, whether the change came from
   * a user gesture or a programmatic write. Writes that leave the selection unchanged do not fire.
   *
   * @param listener the listener; null and duplicates are ignored
   * @version v0.5.0
   * @since v0.5.0
   */
  void addSelectionListener(ElwhaSelectionListener<T> listener);

  /**
   * Removes a previously registered listener.
   *
   * @param listener the listener to remove; unknown listeners are ignored
   * @version v0.5.0
   * @since v0.5.0
   */
  void removeSelectionListener(ElwhaSelectionListener<T> listener);
}
