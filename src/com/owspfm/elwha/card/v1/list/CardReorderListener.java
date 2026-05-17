package com.owspfm.elwha.card.v1.list;

/**
 * Receives reorder events from a {@link ElwhaCardList}.
 *
 * @param <T> the item type
 * @author Charles Bryan
 * @version v0.2.0
 * @since v0.2.0
 */
@FunctionalInterface
public interface CardReorderListener<T> {

  /**
   * Invoked after the user drops a dragged card and the model has been updated.
   *
   * @param event the reorder event
   * @version v0.2.0
   * @since v0.2.0
   */
  void cardReordered(CardReorderEvent<T> event);
}
