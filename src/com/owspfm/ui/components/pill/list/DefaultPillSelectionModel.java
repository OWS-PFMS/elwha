package com.owspfm.ui.components.pill.list;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

/**
 * Default {@link PillSelectionModel} backed by a {@link LinkedHashSet}.
 *
 * @param <T> the item type
 * @author Charles Bryan
 * @version v1.1.0-alpha.3
 * @since v1.1.0-alpha.3
 */
public class DefaultPillSelectionModel<T> implements PillSelectionModel<T> {

  private final Set<T> mySelected = new LinkedHashSet<>();
  private final List<PillSelectionListener<T>> myListeners = new ArrayList<>();

  /** Creates an empty selection model. */
  public DefaultPillSelectionModel() {
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
  public void addSelectionListener(final PillSelectionListener<T> theListener) {
    if (theListener != null && !myListeners.contains(theListener)) {
      myListeners.add(theListener);
    }
  }

  @Override
  public void removeSelectionListener(final PillSelectionListener<T> theListener) {
    myListeners.remove(theListener);
  }

  private void fire() {
    final List<T> snapshot = getSelected();
    final PillSelectionEvent<T> evt = new PillSelectionEvent<>(this, snapshot);
    for (PillSelectionListener<T> l : new ArrayList<>(myListeners)) {
      l.selectionChanged(evt);
    }
  }
}
