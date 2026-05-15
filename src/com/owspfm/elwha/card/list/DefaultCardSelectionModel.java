package com.owspfm.elwha.card.list;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

/**
 * Default {@link CardSelectionModel} backed by a {@link LinkedHashSet}.
 *
 * @param <T> the item type
 * @author Charles Bryan
 * @version v0.1.0
 * @since v0.1.0
 */
public class DefaultCardSelectionModel<T> implements CardSelectionModel<T> {

  private final Set<T> selected = new LinkedHashSet<>();
  private final List<CardSelectionListener<T>> listeners = new ArrayList<>();

  /** Creates an empty selection model. */
  public DefaultCardSelectionModel() {
    super();
  }

  @Override
  public boolean isSelected(final T item) {
    return selected.contains(item);
  }

  @Override
  public List<T> getSelected() {
    return Collections.unmodifiableList(new ArrayList<>(selected));
  }

  @Override
  public void setSelected(final List<T> selected) {
    selected.clear();
    if (selected != null) {
      selected.addAll(selected);
    }
    fire();
  }

  @Override
  public void add(final T item) {
    if (selected.add(item)) {
      fire();
    }
  }

  @Override
  public void remove(final T item) {
    if (selected.remove(item)) {
      fire();
    }
  }

  @Override
  public void toggle(final T item) {
    if (!selected.remove(item)) {
      selected.add(item);
    }
    fire();
  }

  @Override
  public void clearSelection() {
    if (selected.isEmpty()) {
      return;
    }
    selected.clear();
    fire();
  }

  @Override
  public void addSelectionListener(final CardSelectionListener<T> listener) {
    if (listener != null && !listeners.contains(listener)) {
      listeners.add(listener);
    }
  }

  @Override
  public void removeSelectionListener(final CardSelectionListener<T> listener) {
    listeners.remove(listener);
  }

  private void fire() {
    final List<T> snapshot = getSelected();
    final CardSelectionEvent<T> evt = new CardSelectionEvent<>(this, snapshot);
    for (CardSelectionListener<T> l : new ArrayList<>(listeners)) {
      l.selectionChanged(evt);
    }
  }
}
