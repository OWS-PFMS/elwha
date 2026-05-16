package com.owspfm.elwha.list;

import java.util.EventObject;

/**
 * Fired by an {@link ElwhaItemList} after the user drops a dragged item. By the time listeners run,
 * the underlying model has already been mutated via {@link DefaultElwhaListModel#move(int, int)}.
 *
 * <p>Replaces {@code CardReorderEvent} / {@code ChipReorderEvent}.
 *
 * @param <T> the item type
 * @author Charles Bryan
 * @version v0.1.0
 * @since v0.1.0
 */
public class ElwhaReorderEvent<T> extends EventObject {

  private final T item;
  private final int fromIndex;
  private final int toIndex;

  /**
   * Constructs a new reorder event.
   *
   * @param source the originating list
   * @param item the moved item
   * @param fromIndex source index
   * @param toIndex destination index
   * @version v0.1.0
   * @since v0.1.0
   */
  public ElwhaReorderEvent(
      final Object source, final T item, final int fromIndex, final int toIndex) {
    super(source);
    this.item = item;
    this.fromIndex = fromIndex;
    this.toIndex = toIndex;
  }

  /**
   * Returns the moved item.
   *
   * @return the item
   * @version v0.1.0
   * @since v0.1.0
   */
  public T getItem() {
    return item;
  }

  /**
   * Returns the source index.
   *
   * @return the from index
   * @version v0.1.0
   * @since v0.1.0
   */
  public int getFromIndex() {
    return fromIndex;
  }

  /**
   * Returns the destination index.
   *
   * @return the to index
   * @version v0.1.0
   * @since v0.1.0
   */
  public int getToIndex() {
    return toIndex;
  }
}
