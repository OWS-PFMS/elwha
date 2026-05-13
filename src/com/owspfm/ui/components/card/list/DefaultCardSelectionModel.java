package com.owspfm.ui.components.card.list;

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

  private final Set<T> mySelected = new LinkedHashSet<>();
  private final List<CardSelectionListener<T>> myListeners = new ArrayList<>();

  /** Creates an empty selection model. */
  public DefaultCardSelectionModel() {
    super();
  }

  @Override
  public boolean isSelected(final T theItem) {
    return mySelected.contains(theItem);
  }

  @Override
  public List<T> getSelected() {
    return Collections.unmodifiableList(new ArrayList<>(mySelected));
  }

  @Override
  public void setSelected(final List<T> theSelected) {
    mySelected.clear();
    if (theSelected != null) {
      mySelected.addAll(theSelected);
    }
    fire();
  }

  @Override
  public void add(final T theItem) {
    if (mySelected.add(theItem)) {
      fire();
    }
  }

  @Override
  public void remove(final T theItem) {
    if (mySelected.remove(theItem)) {
      fire();
    }
  }

  @Override
  public void toggle(final T theItem) {
    if (!mySelected.remove(theItem)) {
      mySelected.add(theItem);
    }
    fire();
  }

  @Override
  public void clearSelection() {
    if (mySelected.isEmpty()) {
      return;
    }
    mySelected.clear();
    fire();
  }

  @Override
  public void addSelectionListener(final CardSelectionListener<T> theListener) {
    if (theListener != null && !myListeners.contains(theListener)) {
      myListeners.add(theListener);
    }
  }

  @Override
  public void removeSelectionListener(final CardSelectionListener<T> theListener) {
    myListeners.remove(theListener);
  }

  private void fire() {
    final List<T> snapshot = getSelected();
    final CardSelectionEvent<T> evt = new CardSelectionEvent<>(this, snapshot);
    for (CardSelectionListener<T> l : new ArrayList<>(myListeners)) {
      l.selectionChanged(evt);
    }
  }
}
