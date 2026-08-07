package com.owspfm.elwha.list;

import java.util.EventObject;

/**
 * Fired by an {@link ElwhaItemList} after a reorder commits — a dropped drag, a keyboard move, or a
 * context-menu move.
 *
 * <p>Both indices are <strong>model</strong> indices, and the model has already been mutated by the
 * time listeners run.
 *
 * @param <T> the item type
 * @serial exclude
 * @author Charles Bryan
 * @version v0.5.0
 * @since v0.5.0
 */
public class ElwhaReorderEvent<T> extends EventObject {

  private final transient T item;
  private final int fromIndex;
  private final int toIndex;

  /**
   * Constructs a new reorder event.
   *
   * @param source the originating list
   * @param item the moved item
   * @param fromIndex the source index in the model
   * @param toIndex the destination index in the model
   * @version v0.5.0
   * @since v0.5.0
   */
  public ElwhaReorderEvent(
      final Object source, final T item, final int fromIndex, final int toIndex) {
    super(source);
    this.item = item;
    this.fromIndex = fromIndex;
    this.toIndex = toIndex;
  }

  /**
   * Returns the item that moved.
   *
   * @return the moved item
   * @version v0.5.0
   * @since v0.5.0
   */
  public T getItem() {
    return item;
  }

  /**
   * Returns the index the item occupied in the model before the move.
   *
   * @return the source model index
   * @version v0.5.0
   * @since v0.5.0
   */
  public int getFromIndex() {
    return fromIndex;
  }

  /**
   * Returns the index the item occupies in the model after the move.
   *
   * @return the destination model index
   * @version v0.5.0
   * @since v0.5.0
   */
  public int getToIndex() {
    return toIndex;
  }
}
