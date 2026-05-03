package com.owspfm.ui.components.card.list;

import java.util.EventObject;

/**
 * Fired by a {@link FlatCardList} after the user drops a dragged card.
 *
 * <p>The event carries both the source and destination indices in the underlying model. By the time
 * listeners run, the model has already been mutated via {@link DefaultCardListModel#move(int,
 * int)}.
 *
 * @param <T> the item type
 * @author Charles Bryan
 * @version v1.1.0-alpha.2
 * @since v1.1.0-alpha.2
 */
public class CardReorderEvent<T> extends EventObject {

  private final T myItem;
  private final int myFromIndex;
  private final int myToIndex;

  /**
   * Constructs a new reorder event.
   *
   * @param theSource the originating list
   * @param theItem the moved item
   * @param theFromIndex the source index
   * @param theToIndex the destination index
   */
  public CardReorderEvent(
      final Object theSource, final T theItem, final int theFromIndex, final int theToIndex) {
    super(theSource);
    myItem = theItem;
    myFromIndex = theFromIndex;
    myToIndex = theToIndex;
  }

  /**
   * Returns the moved item.
   *
   * @return the item
   */
  public T getItem() {
    return myItem;
  }

  /**
   * Returns the source index in the underlying model.
   *
   * @return the from index
   */
  public int getFromIndex() {
    return myFromIndex;
  }

  /**
   * Returns the destination index in the underlying model.
   *
   * @return the to index
   */
  public int getToIndex() {
    return myToIndex;
  }
}
