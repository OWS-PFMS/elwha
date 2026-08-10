package com.owspfm.elwha.list;

/**
 * Receives reorder events from an {@link ElwhaItemList}.
 *
 * @param <T> the item type
 * @author Charles Bryan
 * @version v0.5.0
 * @since v0.5.0
 */
@FunctionalInterface
public interface ElwhaReorderListener<T> {

  /**
   * Invoked after a user-driven reorder — drag-drop, keyboard move, or context-menu move — has
   * committed to the model. Programmatic {@code model.move(…)} calls do not fire this listener;
   * register an {@link ElwhaListDataListener} to observe those.
   *
   * @param event the reorder event, carrying model indices
   * @version v0.5.0
   * @since v0.5.0
   */
  void itemReordered(ElwhaReorderEvent<T> event);
}
