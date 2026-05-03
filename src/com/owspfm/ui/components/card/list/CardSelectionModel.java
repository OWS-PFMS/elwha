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
 * @version v1.1.0-alpha.2
 * @since v1.1.0-alpha.2
 */
public interface CardSelectionModel<T> {

  /**
   * Returns whether the given item is currently selected.
   *
   * @param theItem the item to test
   * @return true if selected
   */
  boolean isSelected(T theItem);

  /**
   * Returns the currently selected items.
   *
   * @return the selected items, in their model order; never null
   */
  List<T> getSelected();

  /**
   * Replaces the entire selection.
   *
   * @param theSelected the new selection (null treated as empty)
   */
  void setSelected(List<T> theSelected);

  /**
   * Adds an item to the selection (no-op if already selected).
   *
   * @param theItem the item
   */
  void add(T theItem);

  /**
   * Removes an item from the selection (no-op if not selected).
   *
   * @param theItem the item
   */
  void remove(T theItem);

  /**
   * Toggles the membership of the given item.
   *
   * @param theItem the item
   */
  void toggle(T theItem);

  /** Clears the selection. */
  void clearSelection();

  /**
   * Registers a selection-change listener.
   *
   * @param theListener the listener
   */
  void addSelectionListener(CardSelectionListener<T> theListener);

  /**
   * Removes a previously registered selection-change listener.
   *
   * @param theListener the listener to remove
   */
  void removeSelectionListener(CardSelectionListener<T> theListener);
}
