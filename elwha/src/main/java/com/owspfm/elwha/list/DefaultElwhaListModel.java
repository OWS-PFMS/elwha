package com.owspfm.elwha.list;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;

/**
 * Default {@link ElwhaListModel} implementation backed by an {@link ArrayList}. Every mutator is
 * supported, so a list bound to this model has drag-reorder and the keyboard move bindings
 * available.
 *
 * <p>Not thread-safe; mutate on the Swing EDT.
 *
 * @param <T> the item type
 * @author Charles Bryan
 * @version v0.5.0
 * @since v0.5.0
 */
public class DefaultElwhaListModel<T> implements ElwhaListModel<T> {

  private final List<T> items = new ArrayList<>();
  private final List<ElwhaListDataListener<T>> listeners = new ArrayList<>();

  /**
   * Creates an empty model.
   *
   * @version v0.5.0
   * @since v0.5.0
   */
  public DefaultElwhaListModel() {
    super();
  }

  /**
   * Creates a model pre-populated with the given items. The collection is copied, so later changes
   * to it do not reach the model.
   *
   * @param items the initial items; null is treated as empty
   * @version v0.5.0
   * @since v0.5.0
   */
  public DefaultElwhaListModel(final Collection<? extends T> items) {
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
  public List<T> getItems() {
    return List.copyOf(items);
  }

  @Override
  public Iterator<T> iterator() {
    return Collections.unmodifiableList(items).iterator();
  }

  @Override
  public void setItems(final List<T> items) {
    this.items.clear();
    if (items != null) {
      this.items.addAll(items);
    }
    fire(ElwhaListDataEvent.Type.STRUCTURE, -1, -1);
  }

  @Override
  public void add(final T item) {
    final int index = items.size();
    items.add(item);
    fire(ElwhaListDataEvent.Type.ADDED, index, index);
  }

  @Override
  public void add(final int index, final T item) {
    items.add(index, item);
    fire(ElwhaListDataEvent.Type.ADDED, index, index);
  }

  @Override
  public void addAll(final Collection<? extends T> items) {
    if (items == null || items.isEmpty()) {
      return;
    }
    final int from = this.items.size();
    this.items.addAll(items);
    fire(ElwhaListDataEvent.Type.ADDED, from, this.items.size() - 1);
  }

  @Override
  public boolean remove(final T item) {
    final int index = items.indexOf(item);
    if (index < 0) {
      return false;
    }
    items.remove(index);
    fire(ElwhaListDataEvent.Type.REMOVED, index, index);
    return true;
  }

  @Override
  public T remove(final int index) {
    final T removed = items.remove(index);
    fire(ElwhaListDataEvent.Type.REMOVED, index, index);
    return removed;
  }

  @Override
  public T set(final int index, final T item) {
    final T previous = items.set(index, item);
    fire(ElwhaListDataEvent.Type.CHANGED, index, index);
    return previous;
  }

  @Override
  public void move(final int fromIndex, final int toIndex) {
    if (fromIndex == toIndex) {
      return;
    }
    if (fromIndex < 0 || fromIndex >= items.size() || toIndex < 0 || toIndex >= items.size()) {
      throw new IndexOutOfBoundsException("move(" + fromIndex + " -> " + toIndex + ")");
    }
    final T item = items.remove(fromIndex);
    items.add(toIndex, item);
    fire(ElwhaListDataEvent.Type.MOVED, fromIndex, toIndex);
  }

  @Override
  public void clear() {
    if (items.isEmpty()) {
      return;
    }
    items.clear();
    fire(ElwhaListDataEvent.Type.STRUCTURE, -1, -1);
  }

  @Override
  public boolean contains(final T item) {
    return items.contains(item);
  }

  @Override
  public int indexOf(final T item) {
    return items.indexOf(item);
  }

  @Override
  public void addListDataListener(final ElwhaListDataListener<T> listener) {
    if (listener != null && !listeners.contains(listener)) {
      listeners.add(listener);
    }
  }

  @Override
  public void removeListDataListener(final ElwhaListDataListener<T> listener) {
    listeners.remove(listener);
  }

  private void fire(final ElwhaListDataEvent.Type type, final int index0, final int index1) {
    final ElwhaListDataEvent<T> event = new ElwhaListDataEvent<>(this, type, index0, index1);
    for (final ElwhaListDataListener<T> listener : new ArrayList<>(listeners)) {
      listener.contentsChanged(event);
    }
  }
}
