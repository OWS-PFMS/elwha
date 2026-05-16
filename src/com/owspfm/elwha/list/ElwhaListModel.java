package com.owspfm.elwha.list;

import java.util.Iterator;

/**
 * Observable, ordered collection of items rendered by an {@link ElwhaItemList}. Replaces {@code
 * CardListModel} / {@code ChipListModel} as the single generic model contract.
 *
 * <p>Modeled on {@link javax.swing.ListModel}, with two differences:
 *
 * <ul>
 *   <li>Events use a richer {@link ElwhaListDataEvent} type that includes an explicit MOVED variant
 *   <li>The interface is generic in {@code T} (no renderer pattern — the {@link ElwhaListAdapter}
 *       maps an item to a component)
 * </ul>
 *
 * <p>Implementations are not required to be thread-safe; mutate on the Swing EDT.
 *
 * @param <T> the item type
 * @author Charles Bryan
 * @version v0.1.0
 * @since v0.1.0
 */
public interface ElwhaListModel<T> extends Iterable<T> {

  /**
   * Returns the current item count.
   *
   * @return number of items
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
   * Returns an iterator over the items in order. Not required to be fail-fast; do not mutate the
   * model while iterating.
   *
   * @return an iterator
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
  void addListDataListener(ElwhaListDataListener<T> listener);

  /**
   * Removes a previously registered listener.
   *
   * @param listener the listener to remove
   * @version v0.1.0
   * @since v0.1.0
   */
  void removeListDataListener(ElwhaListDataListener<T> listener);
}
