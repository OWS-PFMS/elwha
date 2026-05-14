package com.owspfm.ui.components.chip.list;

/**
 * Receives reorder events from a {@link FlatChipList}.
 *
 * @param <T> the item type
 * @author Charles Bryan
 * @version v0.1.0
 * @since v0.1.0
 */
@FunctionalInterface
public interface ChipReorderListener<T> {

  /**
   * Invoked after the user drops a dragged chip and the model has been updated.
   *
   * @param event the reorder event
   * @version v0.1.0
   * @since v0.1.0
   */
  void chipReordered(ChipReorderEvent<T> event);
}
