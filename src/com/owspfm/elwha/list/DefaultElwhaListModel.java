package com.owspfm.elwha.list;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;

/**
 * Default {@link ElwhaListModel} implementation backed by an {@link ArrayList}. Mirrors the API
 * surface of {@link javax.swing.DefaultListModel} and adds explicit {@link #move(int, int)} and
 * batch convenience methods. Every mutation fires a single {@link ElwhaListDataEvent}.
 *
 * <p>Replaces {@code DefaultCardListModel} / {@code DefaultChipListModel}.
 *
 * @param <T> the item type
 * @author Charles Bryan
 * @version v0.1.0
 * @since v0.1.0
 */
public class DefaultElwhaListModel<T> implements ElwhaListModel<T> {

  private final List<T> items = new ArrayList<>();
  private final List<ElwhaListDataListener<T>> listeners = new ArrayList<>();

  /**
   * Creates an empty model.
   *
   * @version v0.1.0
   * @since v0.1.0
   */
  public DefaultElwhaListModel() {
    super();
  }

  /**
   * Creates a model pre-populated with the given items.
   *
   * @param initial the initial items (copied; null treated as empty)
   * @version v0.1.0
   * @since v0.1.0
   */
  public DefaultElwhaListModel(final Collection<? extends T> initial) {
    super();
    if (initial != null) {
      items.addAll(initial);
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
   * @param item the item to append (null permitted)
   * @version v0.1.0
   * @since v0.1.0
   */
  public void add(final T item) {
    final int index = items.size();
    items.add(item);
    fire(ElwhaListDataEvent.Type.ADDED, index, index);
  }

  /**
   * Inserts an item at the given index.
   *
   * @param index insertion index (0..size)
   * @param item the item
   * @version v0.1.0
   * @since v0.1.0
   */
  public void add(final int index, final T item) {
    items.add(index, item);
    fire(ElwhaListDataEvent.Type.ADDED, index, index);
  }

  /**
   * Appends a batch of items, firing a single ADDED event covering the inserted range.
   *
   * @param batch the items to append (null/empty is a no-op)
   * @version v0.1.0
   * @since v0.1.0
   */
  public void addAll(final Collection<? extends T> batch) {
    if (batch == null || batch.isEmpty()) {
      return;
    }
    final int from = items.size();
    items.addAll(batch);
    fire(ElwhaListDataEvent.Type.ADDED, from, items.size() - 1);
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
    fire(ElwhaListDataEvent.Type.REMOVED, idx, idx);
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
    fire(ElwhaListDataEvent.Type.REMOVED, index, index);
    return removed;
  }

  /**
   * Replaces the item at the given index.
   *
   * @param index the index
   * @param item the replacement item
   * @return the previous item
   * @version v0.1.0
   * @since v0.1.0
   */
  public T set(final int index, final T item) {
    final T previous = items.set(index, item);
    fire(ElwhaListDataEvent.Type.CHANGED, index, index);
    return previous;
  }

  /**
   * Moves the item at {@code fromIndex} to {@code toIndex} and fires a single {@link
   * ElwhaListDataEvent.Type#MOVED} event. No-op if the indices are equal.
   *
   * @param fromIndex source index
   * @param toIndex destination index
   * @version v0.1.0
   * @since v0.1.0
   */
  public void move(final int fromIndex, final int toIndex) {
    if (fromIndex == toIndex) {
      return;
    }
    final T item = items.remove(fromIndex);
    items.add(toIndex, item);
    fire(ElwhaListDataEvent.Type.MOVED, fromIndex, toIndex);
  }

  /** Removes every item. */
  public void clear() {
    if (items.isEmpty()) {
      return;
    }
    final int last = items.size() - 1;
    items.clear();
    fire(ElwhaListDataEvent.Type.REMOVED, 0, last);
  }

  /**
   * Replaces every item with the given batch and fires a single CHANGED event spanning the larger
   * of the prior and new sizes.
   *
   * @param batch the new items (null treated as empty)
   * @version v0.1.0
   * @since v0.1.0
   */
  public void replaceAll(final Collection<? extends T> batch) {
    items.clear();
    if (batch != null) {
      items.addAll(batch);
    }
    fire(ElwhaListDataEvent.Type.CHANGED, 0, Math.max(0, items.size() - 1));
  }

  @Override
  public void addListDataListener(final ElwhaListDataListener<T> listener) {
    if (listener != null) {
      listeners.add(listener);
    }
  }

  @Override
  public void removeListDataListener(final ElwhaListDataListener<T> listener) {
    listeners.remove(listener);
  }

  private void fire(final ElwhaListDataEvent.Type type, final int i0, final int i1) {
    final ElwhaListDataEvent<T> event = new ElwhaListDataEvent<>(this, type, i0, i1);
    for (ElwhaListDataListener<T> l : new ArrayList<>(listeners)) {
      l.contentsChanged(event);
    }
  }
}
