package com.owspfm.elwha.card.list;

import java.util.Iterator;

/**
 * Observable, ordered collection of items rendered by a {@link ElwhaCardList}.
 *
 * <p>Modeled on {@link javax.swing.ListModel}, with two key differences:
 *
 * <ul>
 *   <li>Events use a richer {@link CardListDataEvent} type that includes an explicit MOVED variant
 *   <li>The interface is generic in {@code T} (no need for a renderer pattern — the {@link
 *       CardAdapter} maps an item to a card)
 * </ul>
 *
 * <p>Implementations are not required to be thread-safe; callers should mutate them on the Swing
 * EDT.
 *
 * @param <T> the item type
 * @author Charles Bryan
 * @version v0.1.0
 * @since v0.1.0
 */
public interface CardListModel<T> extends Iterable<T> {

  /**
   * Returns the current item count.
   *
   * @return number of items in the model
   * @version v0.1.0
   * @since v0.1.0
   */
  int getSize();

  /**
   * Returns the item at the given index.
   *
   * @param index the zero-based index
   * @return the item
   * @throws IndexOutOfBoundsException if {@code index} is out of range
   * @version v0.1.0
   * @since v0.1.0
   */
  T getElementAt(int index);

  /**
   * Returns an iterator over the model's items in order.
   *
   * <p>Iterators are not required to be fail-fast; do not mutate the model while iterating.
   *
   * @return an iterator over the items
   * @version v0.1.0
   * @since v0.1.0
   */
  @Override
  Iterator<T> iterator();

  /**
   * Registers a listener for change events.
   *
   * @param listener the listener; null is ignored
   * @version v0.1.0
   * @since v0.1.0
   */
  void addCardListDataListener(CardListDataListener listener);

  /**
   * Removes a previously registered listener.
   *
   * @param listener the listener to remove
   * @version v0.1.0
   * @since v0.1.0
   */
  void removeCardListDataListener(CardListDataListener listener);
}
