package com.owspfm.elwha.list;

import java.util.EventObject;
import java.util.List;

/**
 * Snapshot of an {@link ElwhaSelectionModel}'s contents, fired after the selection changes.
 *
 * <p>The model fires only when the selection actually differs from what it held — a redundant write
 * is silent.
 *
 * @param <T> the item type
 * @serial exclude
 * @author Charles Bryan
 * @version v0.5.0
 * @since v0.5.0
 */
public class ElwhaSelectionEvent<T> extends EventObject {

  private final transient List<T> selected;

  /**
   * Constructs a new selection event.
   *
   * @param source the originating selection model
   * @param selected the new selection; the caller is expected to pass an immutable snapshot
   * @version v0.5.0
   * @since v0.5.0
   */
  public ElwhaSelectionEvent(final Object source, final List<T> selected) {
    super(source);
    this.selected = selected;
  }

  /**
   * Returns the selection as of this event, in the order the items were selected.
   *
   * @return an unmodifiable view of the new selection; never null
   * @version v0.5.0
   * @since v0.5.0
   */
  public List<T> getSelected() {
    return selected;
  }
}
