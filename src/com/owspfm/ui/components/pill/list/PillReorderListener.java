package com.owspfm.ui.components.pill.list;

/**
 * Receives reorder events from a {@link FlatPillList}.
 *
 * @param <T> the item type
 * @author Charles Bryan
 * @version v1.1.0-alpha.3
 * @since v1.1.0-alpha.3
 */
@FunctionalInterface
public interface PillReorderListener<T> {

  /**
   * Invoked after the user drops a dragged pill and the model has been updated.
   *
   * @param theEvent the reorder event
   */
  void pillReordered(PillReorderEvent<T> theEvent);
}
