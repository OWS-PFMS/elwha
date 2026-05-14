package com.owspfm.ui.components.chip.list;

import java.util.List;

/**
 * Tracks selection state for a {@link FlatChipList}.
 *
 * <p>Operates on item identity (not index) so selection survives filter / sort changes.
 *
 * @param <T> the item type
 * @author Charles Bryan
 * @version v0.1.0
 * @since v0.1.0
 */
public interface ChipSelectionModel<T> {

  /**
   * Returns whether the given item is currently selected.
   *
   * @param item the item to test
   * @return true if selected
   * @version v0.1.0
   * @since v0.1.0
   */
  boolean isSelected(T item);

  /**
   * Returns the currently selected items.
   *
   * @return the selected items, in their model order; never null
   * @version v0.1.0
   * @since v0.1.0
   */
  List<T> getSelected();

  /**
   * Replaces the entire selection.
   *
   * @param selected the new selection (null treated as empty)
   * @version v0.1.0
   * @since v0.1.0
   */
  void setSelected(List<T> selected);

  /**
   * Adds an item to the selection (no-op if already selected).
   *
   * @param item the item
   * @version v0.1.0
   * @since v0.1.0
   */
  void add(T item);

  /**
   * Removes an item from the selection (no-op if not selected).
   *
   * @param item the item
   * @version v0.1.0
   * @since v0.1.0
   */
  void remove(T item);

  /**
   * Toggles the membership of the given item.
   *
   * @param item the item
   * @version v0.1.0
   * @since v0.1.0
   */
  void toggle(T item);

  /** Clears the selection. */
  void clearSelection();

  /**
   * Registers a selection-change listener.
   *
   * @param listener the listener
   * @version v0.1.0
   * @since v0.1.0
   */
  void addSelectionListener(ChipSelectionListener<T> listener);

  /**
   * Removes a previously registered selection-change listener.
   *
   * @param listener the listener to remove
   * @version v0.1.0
   * @since v0.1.0
   */
  void removeSelectionListener(ChipSelectionListener<T> listener);
}
