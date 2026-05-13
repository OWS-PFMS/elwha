package com.owspfm.ui.components.pill.list;

/**
 * Receives selection-change events from a {@link PillSelectionModel}.
 *
 * @param <T> the item type
 * @author Charles Bryan
 * @version v0.1.0
 * @since v0.1.0
 */
@FunctionalInterface
public interface PillSelectionListener<T> {

  /**
   * Invoked after the selection set changes.
   *
   * @param theEvent the event carrying the new selection
    * @version v0.1.0
    * @since v0.1.0
   */
  void selectionChanged(PillSelectionEvent<T> theEvent);
}
