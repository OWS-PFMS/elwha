package com.owspfm.ui.components.card.list;

/**
 * Receives selection-change events from a {@link CardSelectionModel}.
 *
 * @param <T> the item type
 * @author Charles Bryan
 * @version v1.1.0-alpha.2
 * @since v1.1.0-alpha.2
 */
@FunctionalInterface
public interface CardSelectionListener<T> {

  /**
   * Invoked after the selection set changes.
   *
   * @param theEvent the event carrying the new selection
   */
  void selectionChanged(CardSelectionEvent<T> theEvent);
}
