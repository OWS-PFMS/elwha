package com.owspfm.elwha.card.list;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.function.Consumer;

/**
 * In-memory {@link CardListModel} backed by an {@link ArrayList}. Suitable for small and
 * medium-sized lists where the model lives in-process.
 *
 * @param <T> the item type
 * @author Charles Bryan
 * @version v0.2.0
 * @since v0.2.0
 */
public final class DefaultCardListModel<T> implements CardListModel<T> {

  private final List<T> items = new ArrayList<>();
  private final List<Consumer<CardListModel<T>>> listeners = new ArrayList<>();

  /** Creates an empty model. */
  public DefaultCardListModel() {
    // empty
  }

  /**
   * Creates a model pre-populated with the given items.
   *
   * @param initialItems the initial items (must not be {@code null})
   */
  public DefaultCardListModel(final List<T> initialItems) {
    setItems(initialItems);
  }

  @Override
  public void setItems(final List<T> newItems) {
    Objects.requireNonNull(newItems, "items");
    items.clear();
    items.addAll(newItems);
    fire();
  }

  @Override
  public List<T> getItems() {
    return Collections.unmodifiableList(new ArrayList<>(items));
  }

  @Override
  public void add(final T item) {
    items.add(item);
    fire();
  }

  @Override
  public void remove(final int index) {
    items.remove(index);
    fire();
  }

  @Override
  public void move(final int fromIndex, final int toIndex) {
    if (fromIndex == toIndex) {
      return;
    }
    final T item = items.remove(fromIndex);
    items.add(toIndex, item);
    fire();
  }

  @Override
  public void addChangeListener(final Consumer<CardListModel<T>> listener) {
    if (listener != null) {
      listeners.add(listener);
    }
  }

  @Override
  public void removeChangeListener(final Consumer<CardListModel<T>> listener) {
    listeners.remove(listener);
  }

  private void fire() {
    for (final Consumer<CardListModel<T>> l : new ArrayList<>(listeners)) {
      l.accept(this);
    }
  }
}
