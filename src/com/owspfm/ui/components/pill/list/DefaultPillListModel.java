package com.owspfm.ui.components.pill.list;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;

/**
 * Default {@link PillListModel} implementation backed by an {@link ArrayList}.
 *
 * <p>Mirrors the API surface of {@link com.owspfm.ui.components.card.list.DefaultCardListModel}.
 *
 * @param <T> the item type
 * @author Charles Bryan
 * @version v1.1.0-alpha.3
 * @since v1.1.0-alpha.3
 */
public class DefaultPillListModel<T> implements PillListModel<T> {

  private final List<T> myItems = new ArrayList<>();
  private final List<PillListDataListener> myListeners = new ArrayList<>();

  /** Creates an empty model. */
  public DefaultPillListModel() {
    super();
  }

  /**
   * Creates a model pre-populated with the given items.
   *
   * @param theItems the initial items (copied; null treated as empty)
   */
  public DefaultPillListModel(final Collection<? extends T> theItems) {
    super();
    if (theItems != null) {
      myItems.addAll(theItems);
    }
  }

  @Override
  public int getSize() {
    return myItems.size();
  }

  @Override
  public T getElementAt(final int theIndex) {
    return myItems.get(theIndex);
  }

  @Override
  public Iterator<T> iterator() {
    return Collections.unmodifiableList(myItems).iterator();
  }

  /**
   * Appends an item.
   *
   * @param theItem the item to append (null is permitted)
   */
  public void add(final T theItem) {
    final int index = myItems.size();
    myItems.add(theItem);
    fire(PillListDataEvent.Type.ADDED, index, index);
  }

  /**
   * Inserts an item at the given index.
   *
   * @param theIndex the insertion index (0..size)
   * @param theItem the item to insert
   */
  public void add(final int theIndex, final T theItem) {
    myItems.add(theIndex, theItem);
    fire(PillListDataEvent.Type.ADDED, theIndex, theIndex);
  }

  /**
   * Appends a batch of items, firing a single ADDED event covering the inserted range.
   *
   * @param theItems the items to append (null/empty is a no-op)
   */
  public void addAll(final Collection<? extends T> theItems) {
    if (theItems == null || theItems.isEmpty()) {
      return;
    }
    final int from = myItems.size();
    myItems.addAll(theItems);
    fire(PillListDataEvent.Type.ADDED, from, myItems.size() - 1);
  }

  /**
   * Removes the first occurrence of the given item.
   *
   * @param theItem the item to remove
   * @return true if a matching item was found and removed
   */
  public boolean remove(final T theItem) {
    final int idx = myItems.indexOf(theItem);
    if (idx < 0) {
      return false;
    }
    myItems.remove(idx);
    fire(PillListDataEvent.Type.REMOVED, idx, idx);
    return true;
  }

  /**
   * Removes the item at the given index.
   *
   * @param theIndex the index to remove
   * @return the removed item
   */
  public T remove(final int theIndex) {
    final T removed = myItems.remove(theIndex);
    fire(PillListDataEvent.Type.REMOVED, theIndex, theIndex);
    return removed;
  }

  /**
   * Replaces the item at the given index.
   *
   * @param theIndex the index to replace
   * @param theItem the replacement item
   * @return the previous item
   */
  public T set(final int theIndex, final T theItem) {
    final T old = myItems.set(theIndex, theItem);
    fire(PillListDataEvent.Type.CHANGED, theIndex, theIndex);
    return old;
  }

  /**
   * Moves an item to a new index. A no-op if {@code theFrom == theTo}.
   *
   * @param theFrom the source index
   * @param theTo the destination index
   */
  public void move(final int theFrom, final int theTo) {
    if (theFrom == theTo) {
      return;
    }
    if (theFrom < 0 || theFrom >= myItems.size() || theTo < 0 || theTo >= myItems.size()) {
      throw new IndexOutOfBoundsException("move(" + theFrom + " -> " + theTo + ")");
    }
    final T item = myItems.remove(theFrom);
    myItems.add(theTo, item);
    fire(PillListDataEvent.Type.MOVED, theFrom, theTo);
  }

  /** Removes every item, firing a single REMOVED event spanning the prior range. */
  public void clear() {
    if (myItems.isEmpty()) {
      return;
    }
    final int last = myItems.size() - 1;
    myItems.clear();
    fire(PillListDataEvent.Type.REMOVED, 0, last);
  }

  /**
   * Returns whether the model contains the given item.
   *
   * @param theItem the item
   * @return true if present
   */
  public boolean contains(final T theItem) {
    return myItems.contains(theItem);
  }

  /**
   * Returns the index of the first occurrence of the given item, or -1.
   *
   * @param theItem the item
   * @return the index, or -1 if not present
   */
  public int indexOf(final T theItem) {
    return myItems.indexOf(theItem);
  }

  @Override
  public void addPillListDataListener(final PillListDataListener theListener) {
    if (theListener != null && !myListeners.contains(theListener)) {
      myListeners.add(theListener);
    }
  }

  @Override
  public void removePillListDataListener(final PillListDataListener theListener) {
    myListeners.remove(theListener);
  }

  private void fire(final PillListDataEvent.Type theType, final int theI0, final int theI1) {
    final PillListDataEvent evt = new PillListDataEvent(this, theType, theI0, theI1);
    for (PillListDataListener l : new ArrayList<>(myListeners)) {
      l.contentsChanged(evt);
    }
  }
}
