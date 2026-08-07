package com.owspfm.elwha.list;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

/**
 * Default {@link ElwhaSelectionModel} backed by a {@link LinkedHashSet}, which gives insertion
 * order for free and keeps membership tests constant-time.
 *
 * <p>Every mutator is change-guarded: a write that leaves the set identical fires nothing, so a
 * listener that rebuilds expensive chrome is not woken by redundant writes.
 *
 * @param <T> the item type
 * @author Charles Bryan
 * @version v0.5.0
 * @since v0.5.0
 */
public class DefaultElwhaSelectionModel<T> implements ElwhaSelectionModel<T> {

  private final Set<T> selected = new LinkedHashSet<>();
  private final List<ElwhaSelectionListener<T>> listeners = new ArrayList<>();

  /**
   * Creates an empty selection model.
   *
   * @version v0.5.0
   * @since v0.5.0
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
    return List.copyOf(selected);
  }

  @Override
  public void setSelected(final List<T> selected) {
    final Set<T> replacement = new LinkedHashSet<>();
    if (selected != null) {
      replacement.addAll(selected);
    }
    // Guarded on contents AND order: a caller reordering the same items is a real change to the
    // insertion-ordered snapshot listeners read.
    if (sameSequence(replacement)) {
      return;
    }
    this.selected.clear();
    this.selected.addAll(replacement);
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

  private boolean sameSequence(final Set<T> candidate) {
    if (candidate.size() != selected.size()) {
      return false;
    }
    final Iterator<T> mine = selected.iterator();
    final Iterator<T> theirs = candidate.iterator();
    while (mine.hasNext()) {
      if (mine.next() != theirs.next()) {
        return false;
      }
    }
    return true;
  }

  private void fire() {
    final List<T> snapshot = getSelected();
    final ElwhaSelectionEvent<T> event = new ElwhaSelectionEvent<>(this, snapshot);
    for (final ElwhaSelectionListener<T> listener : new ArrayList<>(listeners)) {
      listener.selectionChanged(event);
    }
  }
}
