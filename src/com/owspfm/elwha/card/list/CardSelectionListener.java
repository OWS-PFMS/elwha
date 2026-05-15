package com.owspfm.elwha.card.list;

/**
 * Receives selection-change events from a {@link CardSelectionModel}.
 *
 * @param <T> the item type
 * @author Charles Bryan
 * @version v0.1.0
 * @since v0.1.0
 */
@FunctionalInterface
public interface CardSelectionListener<T> {

  /**
   * Invoked after the selection set changes.
   *
   * @param event the event carrying the new selection
   * @version v0.1.0
   * @since v0.1.0
   */
  void selectionChanged(CardSelectionEvent<T> event);
}
