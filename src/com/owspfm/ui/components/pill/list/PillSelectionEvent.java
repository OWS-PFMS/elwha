package com.owspfm.ui.components.pill.list;

import java.util.EventObject;
import java.util.List;

/**
 * Fired when the selection of a {@link FlatPillList} changes.
 *
 * @param <T> the item type
 * @author Charles Bryan
 * @version v0.1.0
 * @since v0.1.0
 */
public class PillSelectionEvent<T> extends EventObject {

  private final List<T> mySelected;

  /**
   * Constructs a new selection event.
   *
   * @param theSource the originating selection model
   * @param theSelected the new selected items (defensive copy expected from caller)
    * @version v0.1.0
    * @since v0.1.0
   */
  public PillSelectionEvent(final Object theSource, final List<T> theSelected) {
    super(theSource);
    mySelected = theSelected;
  }

  /**
   * Returns the selected items in the order they appear in the underlying model.
   *
   * @return immutable view of the new selection
    * @version v0.1.0
    * @since v0.1.0
   */
  public List<T> getSelected() {
    return mySelected;
  }
}
