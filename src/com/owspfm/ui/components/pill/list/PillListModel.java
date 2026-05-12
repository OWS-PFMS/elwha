package com.owspfm.ui.components.pill.list;

import java.util.Iterator;

/**
 * Observable, ordered collection of items rendered by a {@link FlatPillList}.
 *
 * <p>Modeled on {@link javax.swing.ListModel} and on the sibling {@link
 * com.owspfm.ui.components.card.list.CardListModel}, with the same MOVED-event extension.
 *
 * <p>Implementations are not required to be thread-safe; callers should mutate them on the Swing
 * EDT.
 *
 * @param <T> the item type
 * @author Charles Bryan
 * @version v1.1.0-alpha.3
 * @since v1.1.0-alpha.3
 */
public interface PillListModel<T> extends Iterable<T> {

  /**
   * Returns the current item count.
   *
   * @return number of items in the model
   */
  int getSize();

  /**
   * Returns the item at the given index.
   *
   * @param theIndex the zero-based index
   * @return the item
   * @throws IndexOutOfBoundsException if {@code theIndex} is out of range
   */
  T getElementAt(int theIndex);

  /**
   * Returns an iterator over the model's items in order.
   *
   * @return an iterator over the items
   */
  @Override
  Iterator<T> iterator();

  /**
   * Registers a listener for change events.
   *
   * @param theListener the listener; null is ignored
   */
  void addPillListDataListener(PillListDataListener theListener);

  /**
   * Removes a previously registered listener.
   *
   * @param theListener the listener to remove
   */
  void removePillListDataListener(PillListDataListener theListener);
}
