package com.owspfm.ui.components.pill.list;

import java.util.EventObject;

/**
 * Fired by a {@link FlatPillList} after the user drops a dragged pill.
 *
 * <p>By the time listeners run, the model has already been mutated via {@link
 * DefaultPillListModel#move(int, int)}.
 *
 * @param <T> the item type
 * @author Charles Bryan
 * @version v1.1.0-alpha.3
 * @since v1.1.0-alpha.3
 */
public class PillReorderEvent<T> extends EventObject {

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
  public PillReorderEvent(
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
