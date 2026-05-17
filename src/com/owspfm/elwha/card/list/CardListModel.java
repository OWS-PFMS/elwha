package com.owspfm.elwha.card.list;

import java.util.List;
import java.util.function.Consumer;

/**
 * Data model for {@link ElwhaCardList} — owns the item collection and notifies registered listeners
 * on change. Mirrors the V1 model API contract carried forward to V3.
 *
 * @param <T> the item type
 * @author Charles Bryan
 * @version v0.2.0
 * @since v0.2.0
 */
public interface CardListModel<T> {

  /**
   * Replaces the entire item list. Fires a change notification.
   *
   * @param items the new items (must not be {@code null})
   * @version v0.2.0
   * @since v0.2.0
   */
  void setItems(List<T> items);

  /**
   * @return an unmodifiable snapshot of the current items
   * @version v0.2.0
   * @since v0.2.0
   */
  List<T> getItems();

  /**
   * Appends a single item to the end of the list.
   *
   * @param item the item to add
   * @version v0.2.0
   * @since v0.2.0
   */
  void add(T item);

  /**
   * Removes the item at the given index.
   *
   * @param index the index to remove
   * @version v0.2.0
   * @since v0.2.0
   */
  void remove(int index);

  /**
   * Moves an item from one index to another.
   *
   * @param fromIndex the source index
   * @param toIndex the destination index
   * @version v0.2.0
   * @since v0.2.0
   */
  void move(int fromIndex, int toIndex);

  /**
   * Registers a listener notified after every model change. The callback receives this model so the
   * listener can re-query state.
   *
   * @param listener the listener
   * @version v0.2.0
   * @since v0.2.0
   */
  void addChangeListener(Consumer<CardListModel<T>> listener);

  /**
   * Removes a previously registered change listener.
   *
   * @param listener the listener
   * @version v0.2.0
   * @since v0.2.0
   */
  void removeChangeListener(Consumer<CardListModel<T>> listener);
}
