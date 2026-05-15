package com.owspfm.elwha.card.list;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;

/**
 * Default {@link CardListModel} implementation backed by an {@link ArrayList}.
 *
 * <p>Mirrors the API surface of {@link javax.swing.DefaultListModel} and adds explicit {@link
 * #move(int, int)} and {@link #addAll(Collection)} convenience methods. Every mutation fires a
 * single {@link CardListDataEvent} after the underlying list has been updated.
 *
 * @param <T> the item type
 * @author Charles Bryan
 * @version v0.1.0
 * @since v0.1.0
 */
public class DefaultCardListModel<T> implements CardListModel<T> {

  private final List<T> items = new ArrayList<>();
  private final List<CardListDataListener> listeners = new ArrayList<>();

  /** Creates an empty model. */
  public DefaultCardListModel() {
    super();
  }

  /**
   * Creates a model pre-populated with the given items.
   *
   * @param items the initial items (copied; null treated as empty)
   * @version v0.1.0
   * @since v0.1.0
   */
  public DefaultCardListModel(final Collection<? extends T> items) {
    super();
    if (items != null) {
      this.items.addAll(items);
    }
  }

  @Override
  public int getSize() {
    return items.size();
  }

  @Override
  public T getElementAt(final int index) {
    return items.get(index);
  }

  @Override
  public Iterator<T> iterator() {
    return Collections.unmodifiableList(items).iterator();
  }

  /**
   * Appends an item.
   *
   * @param item the item to append (null is permitted)
   * @version v0.1.0
   * @since v0.1.0
   */
  public void add(final T item) {
    final int index = items.size();
    items.add(item);
    fire(CardListDataEvent.Type.ADDED, index, index);
  }

  /**
   * Inserts an item at the given index.
   *
   * @param index the insertion index (0..size)
   * @param item the item to insert
   * @version v0.1.0
   * @since v0.1.0
   */
  public void add(final int index, final T item) {
    items.add(index, item);
    fire(CardListDataEvent.Type.ADDED, index, index);
  }

  /**
   * Appends a batch of items, firing a single ADDED event covering the inserted range.
   *
   * @param items the items to append (null/empty is a no-op)
   * @version v0.1.0
   * @since v0.1.0
   */
  public void addAll(final Collection<? extends T> items) {
    if (items == null || items.isEmpty()) {
      return;
    }
    final int from = this.items.size();
    this.items.addAll(items);
    fire(CardListDataEvent.Type.ADDED, from, this.items.size() - 1);
  }

  /**
   * Removes the first occurrence of the given item.
   *
   * @param item the item to remove
   * @return true if a matching item was found and removed
   * @version v0.1.0
   * @since v0.1.0
   */
  public boolean remove(final T item) {
    final int idx = items.indexOf(item);
    if (idx < 0) {
      return false;
    }
    items.remove(idx);
    fire(CardListDataEvent.Type.REMOVED, idx, idx);
    return true;
  }

  /**
   * Removes the item at the given index.
   *
   * @param index the index to remove
   * @return the removed item
   * @version v0.1.0
   * @since v0.1.0
   */
  public T remove(final int index) {
    final T removed = items.remove(index);
    fire(CardListDataEvent.Type.REMOVED, index, index);
    return removed;
  }

  /**
   * Replaces the item at the given index.
   *
   * @param index the index to replace
   * @param item the replacement item
   * @return the previous item
   * @version v0.1.0
   * @since v0.1.0
   */
  public T set(final int index, final T item) {
    final T old = items.set(index, item);
    fire(CardListDataEvent.Type.CHANGED, index, index);
    return old;
  }

  /**
   * Moves an item to a new index.
   *
   * <p>Indices are interpreted in the model's current state — i.e., {@code to} is the final resting
   * index of the item. A no-op if {@code from == to}.
   *
   * @param from the source index
   * @param to the destination index
   * @version v0.1.0
   * @since v0.1.0
   */
  public void move(final int from, final int to) {
    if (from == to) {
      return;
    }
    if (from < 0 || from >= items.size() || to < 0 || to >= items.size()) {
      throw new IndexOutOfBoundsException("move(" + from + " -> " + to + ")");
    }
    final T item = items.remove(from);
    items.add(to, item);
    fire(CardListDataEvent.Type.MOVED, from, to);
  }

  /** Removes every item, firing a single REMOVED event spanning the prior range. */
  public void clear() {
    if (items.isEmpty()) {
      return;
    }
    final int last = items.size() - 1;
    items.clear();
    fire(CardListDataEvent.Type.REMOVED, 0, last);
  }

  /**
   * Returns whether the model contains the given item.
   *
   * @param item the item
   * @return true if present
   * @version v0.1.0
   * @since v0.1.0
   */
  public boolean contains(final T item) {
    return items.contains(item);
  }

  /**
   * Returns the index of the first occurrence of the given item, or -1.
   *
   * @param item the item
   * @return the index, or -1 if not present
   * @version v0.1.0
   * @since v0.1.0
   */
  public int indexOf(final T item) {
    return items.indexOf(item);
  }

  @Override
  public void addCardListDataListener(final CardListDataListener listener) {
    if (listener != null && !listeners.contains(listener)) {
      listeners.add(listener);
    }
  }

  @Override
  public void removeCardListDataListener(final CardListDataListener listener) {
    listeners.remove(listener);
  }

  private void fire(final CardListDataEvent.Type type, final int i0, final int i1) {
    final CardListDataEvent evt = new CardListDataEvent(this, type, i0, i1);
    for (CardListDataListener l : new ArrayList<>(listeners)) {
      l.contentsChanged(evt);
    }
  }
}
