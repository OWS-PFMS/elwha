package com.owspfm.ui.components.card.list;

import java.util.List;

/**
 * Tracks selection state for a {@link FlatCardList}.
 *
 * <p>Implementations operate on the item type rather than indices so that selection survives
 * filter/sort changes (the visible index of a selected item may change without a real selection
 * change).
 *
 * @param <T> the item type
 * @author Charles Bryan
 * @version v0.1.0
 * @since v0.1.0
 */
public interface CardSelectionModel<T> {

  /**
   * Returns whether the given item is currently selected.
   *
   * @param theItem the item to test
   * @return true if selected
   * @version v0.1.0
   * @since v0.1.0
   */
  boolean isSelected(T theItem);

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
   * @param theSelected the new selection (null treated as empty)
   * @version v0.1.0
   * @since v0.1.0
   */
  void setSelected(List<T> theSelected);

  /**
   * Adds an item to the selection (no-op if already selected).
   *
   * @param theItem the item
   * @version v0.1.0
   * @since v0.1.0
   */
  void add(T theItem);

  /**
   * Removes an item from the selection (no-op if not selected).
   *
   * @param theItem the item
   * @version v0.1.0
   * @since v0.1.0
   */
  void remove(T theItem);

  /**
   * Toggles the membership of the given item.
   *
   * @param theItem the item
   * @version v0.1.0
   * @since v0.1.0
   */
  void toggle(T theItem);

  /** Clears the selection. */
  void clearSelection();

  /**
   * Registers a selection-change listener.
   *
   * @param theListener the listener
   * @version v0.1.0
   * @since v0.1.0
   */
  void addSelectionListener(CardSelectionListener<T> theListener);

  /**
   * Removes a previously registered selection-change listener.
   *
   * @param theListener the listener to remove
   * @version v0.1.0
   * @since v0.1.0
   */
  void removeSelectionListener(CardSelectionListener<T> theListener);
}
