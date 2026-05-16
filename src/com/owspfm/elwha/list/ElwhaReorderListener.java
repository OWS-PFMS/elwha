package com.owspfm.elwha.list;

/**
 * Receives reorder events from an {@link ElwhaItemList}. Replaces {@code CardReorderListener} /
 * {@code ChipReorderListener}.
 *
 * @param <T> the item type
 * @author Charles Bryan
 * @version v0.1.0
 * @since v0.1.0
 */
@FunctionalInterface
public interface ElwhaReorderListener<T> {

  /**
   * Invoked after the user drops a dragged item and the model has been updated.
   *
   * @param event the reorder event
   * @version v0.1.0
   * @since v0.1.0
   */
  void itemReordered(ElwhaReorderEvent<T> event);
}
