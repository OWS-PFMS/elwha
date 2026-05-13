package com.owspfm.ui.components.pill.list;

/**
 * Receives reorder events from a {@link FlatPillList}.
 *
 * @param <T> the item type
 * @author Charles Bryan
 * @version v0.1.0
 * @since v0.1.0
 */
@FunctionalInterface
public interface PillReorderListener<T> {

  /**
   * Invoked after the user drops a dragged pill and the model has been updated.
   *
   * @param theEvent the reorder event
   * @version v0.1.0
   * @since v0.1.0
   */
  void pillReordered(PillReorderEvent<T> theEvent);
}
