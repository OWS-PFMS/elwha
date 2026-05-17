package com.owspfm.elwha.card.list;

import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.Set;
import java.util.function.Consumer;

/**
 * Selection-state holder for {@link ElwhaCardList}. Implements the four {@link CardSelectionMode}
 * semantics including {@link CardSelectionMode#SINGLE_MANDATORY} (always at least one selected).
 *
 * @param <T> the item type
 * @author Charles Bryan
 * @version v0.2.0
 * @since v0.2.0
 */
public final class CardSelectionModel<T> {

  private CardSelectionMode mode = CardSelectionMode.NONE;
  private final Set<T> selected = new LinkedHashSet<>();
  private final java.util.List<Consumer<CardSelectionModel<T>>> listeners =
      new java.util.ArrayList<>();

  /**
   * Sets the active selection mode. Switching to {@link CardSelectionMode#NONE} clears the current
   * selection.
   *
   * @param newMode the new mode (must not be {@code null})
   * @version v0.2.0
   * @since v0.2.0
   */
  public void setSelectionMode(final CardSelectionMode newMode) {
    this.mode = java.util.Objects.requireNonNull(newMode, "mode");
    if (newMode == CardSelectionMode.NONE) {
      selected.clear();
      fire();
    }
  }

  /**
   * @return the active selection mode
   * @version v0.2.0
   * @since v0.2.0
   */
  public CardSelectionMode getSelectionMode() {
    return mode;
  }

  /**
   * Replaces the selected set.
   *
   * @param newSelected the new set (must not be {@code null})
   * @version v0.2.0
   * @since v0.2.0
   */
  public void setSelectedItems(final Set<T> newSelected) {
    java.util.Objects.requireNonNull(newSelected, "selected");
    selected.clear();
    selected.addAll(newSelected);
    fire();
  }

  /**
   * @return an unmodifiable snapshot of the selected set, insertion-ordered
   * @version v0.2.0
   * @since v0.2.0
   */
  public Set<T> getSelectedItems() {
    return java.util.Collections.unmodifiableSet(new LinkedHashSet<>(selected));
  }

  /**
   * Toggles an item according to the current mode. Returns the new selected state for that item.
   *
   * @param item the item to toggle
   * @return whether the item is selected after the toggle
   * @version v0.2.0
   * @since v0.2.0
   */
  public boolean toggle(final T item) {
    switch (mode) {
      case NONE:
        return false;
      case SINGLE:
        if (selected.contains(item)) {
          selected.clear();
          fire();
          return false;
        }
        selected.clear();
        selected.add(item);
        fire();
        return true;
      case SINGLE_MANDATORY:
        if (selected.contains(item)) {
          return true;
        }
        selected.clear();
        selected.add(item);
        fire();
        return true;
      case MULTIPLE:
        final boolean nowSelected = !selected.contains(item);
        if (nowSelected) {
          selected.add(item);
        } else {
          selected.remove(item);
        }
        fire();
        return nowSelected;
      default:
        return false;
    }
  }

  /**
   * @param item the item
   * @return whether the item is currently selected
   * @version v0.2.0
   * @since v0.2.0
   */
  public boolean isSelected(final T item) {
    return selected.contains(item);
  }

  /**
   * Registers a listener notified on every selection change.
   *
   * @param listener the listener
   * @version v0.2.0
   * @since v0.2.0
   */
  public void addChangeListener(final Consumer<CardSelectionModel<T>> listener) {
    if (listener != null) {
      listeners.add(listener);
    }
  }

  /**
   * Removes a previously registered change listener.
   *
   * @param listener the listener
   * @version v0.2.0
   * @since v0.2.0
   */
  public void removeChangeListener(final Consumer<CardSelectionModel<T>> listener) {
    listeners.remove(listener);
  }

  private void fire() {
    for (final Consumer<CardSelectionModel<T>> l : new java.util.ArrayList<>(listeners)) {
      l.accept(this);
    }
  }

  /** Discards any selections for items no longer in {@code presentItems}. */
  void retainAll(final Set<T> presentItems) {
    final Set<T> retained = new HashSet<>(selected);
    retained.retainAll(presentItems);
    if (retained.size() != selected.size()) {
      selected.clear();
      selected.addAll(retained);
      fire();
    }
  }
}
