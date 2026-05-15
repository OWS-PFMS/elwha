package com.owspfm.elwha.card.list;

import java.util.EventObject;

/**
 * Fired by a {@link ElwhaCardList} after the user drops a dragged card.
 *
 * <p>The event carries both the source and destination indices in the underlying model. By the time
 * listeners run, the model has already been mutated via {@link DefaultCardListModel#move(int,
 * int)}.
 *
 * @param <T> the item type
 * @author Charles Bryan
 * @version v0.1.0
 * @since v0.1.0
 */
public class CardReorderEvent<T> extends EventObject {

  private final T item;
  private final int fromIndex;
  private final int toIndex;

  /**
   * Constructs a new reorder event.
   *
   * @param source the originating list
   * @param item the moved item
   * @param fromIndex the source index
   * @param toIndex the destination index
   * @version v0.1.0
   * @since v0.1.0
   */
  public CardReorderEvent(
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
   * Returns the source index in the underlying model.
   *
   * @return the from index
   * @version v0.1.0
   * @since v0.1.0
   */
  public int getFromIndex() {
    return fromIndex;
  }

  /**
   * Returns the destination index in the underlying model.
   *
   * @return the to index
   * @version v0.1.0
   * @since v0.1.0
   */
  public int getToIndex() {
    return toIndex;
  }
}
