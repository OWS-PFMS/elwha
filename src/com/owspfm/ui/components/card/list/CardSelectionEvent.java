package com.owspfm.ui.components.card.list;

import java.util.EventObject;
import java.util.List;

/**
 * Fired when the selection of a {@link FlatCardList} changes.
 *
 * @param <T> the item type
 * @author Charles Bryan
 * @version v1.1.0-alpha.2
 * @since v1.1.0-alpha.2
 */
public class CardSelectionEvent<T> extends EventObject {

  private final List<T> mySelected;

  /**
   * Constructs a new selection event.
   *
   * @param theSource the originating selection model
   * @param theSelected the new selected items (defensive copy expected from caller)
   */
  public CardSelectionEvent(final Object theSource, final List<T> theSelected) {
    super(theSource);
    mySelected = theSelected;
  }

  /**
   * Returns the selected items in the order they appear in the underlying model.
   *
   * @return immutable view of the new selection
   */
  public List<T> getSelected() {
    return mySelected;
  }
}
