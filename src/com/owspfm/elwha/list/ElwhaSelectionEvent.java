package com.owspfm.elwha.list;

import java.util.EventObject;
import java.util.List;

/**
 * Fired by an {@link ElwhaSelectionModel} when the selection set changes. Replaces {@code
 * CardSelectionEvent} / {@code ChipSelectionEvent}.
 *
 * @param <T> the item type
 * @author Charles Bryan
 * @version v0.1.0
 * @since v0.1.0
 */
public class ElwhaSelectionEvent<T> extends EventObject {

  private final List<T> selected;

  /**
   * Constructs a new selection event.
   *
   * @param source the originating selection model
   * @param selected the new selected items (defensive copy expected from caller)
   * @version v0.1.0
   * @since v0.1.0
   */
  public ElwhaSelectionEvent(final Object source, final List<T> selected) {
    super(source);
    this.selected = selected;
  }

  /**
   * Returns the selected items in their model order.
   *
   * @return immutable view of the new selection
   * @version v0.1.0
   * @since v0.1.0
   */
  public List<T> getSelected() {
    return selected;
  }
}
