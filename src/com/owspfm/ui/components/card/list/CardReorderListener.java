package com.owspfm.ui.components.card.list;

/**
 * Receives reorder events from a {@link FlatCardList}.
 *
 * @param <T> the item type
 * @author Charles Bryan
 * @version v0.1.0
 * @since v0.1.0
 */
@FunctionalInterface
public interface CardReorderListener<T> {

  /**
   * Invoked after the user drops a dragged card and the model has been updated.
   *
   * @param theEvent the reorder event
    * @version v0.1.0
    * @since v0.1.0
   */
  void cardReordered(CardReorderEvent<T> theEvent);
}
