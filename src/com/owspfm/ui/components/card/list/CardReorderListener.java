package com.owspfm.ui.components.card.list;

/**
 * Receives reorder events from a {@link FlatCardList}.
 *
 * @param <T> the item type
 * @author Charles Bryan
 * @version v1.1.0-alpha.2
 * @since v1.1.0-alpha.2
 */
@FunctionalInterface
public interface CardReorderListener<T> {

  /**
   * Invoked after the user drops a dragged card and the model has been updated.
   *
   * @param theEvent the reorder event
   */
  void cardReordered(CardReorderEvent<T> theEvent);
}
