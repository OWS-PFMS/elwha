package com.owspfm.elwha.list;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

/**
 * Default {@link ElwhaSelectionModel} implementation backed by a {@link LinkedHashSet} so insertion
 * order is preserved. Replaces {@code DefaultCardSelectionModel} / {@code
 * DefaultChipSelectionModel}.
 *
 * @param <T> the item type
 * @author Charles Bryan
 * @version v0.1.0
 * @since v0.1.0
 */
public class DefaultElwhaSelectionModel<T> implements ElwhaSelectionModel<T> {

  private final Set<T> selected = new LinkedHashSet<>();
  private final List<ElwhaSelectionListener<T>> listeners = new ArrayList<>();

  /**
   * Creates an empty selection model.
   *
   * @version v0.1.0
   * @since v0.1.0
   */
  public DefaultElwhaSelectionModel() {
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
  public void setSelected(final List<T> items) {
    selected.clear();
    if (items != null) {
      selected.addAll(items);
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
  public void addSelectionListener(final ElwhaSelectionListener<T> listener) {
    if (listener != null && !listeners.contains(listener)) {
      listeners.add(listener);
    }
  }

  @Override
  public void removeSelectionListener(final ElwhaSelectionListener<T> listener) {
    listeners.remove(listener);
  }

  private void fire() {
    final List<T> snapshot = getSelected();
    final ElwhaSelectionEvent<T> evt = new ElwhaSelectionEvent<>(this, snapshot);
    for (ElwhaSelectionListener<T> l : new ArrayList<>(listeners)) {
      l.selectionChanged(evt);
    }
  }
}
