package com.owspfm.ui.components.pill.list;

/**
 * Receives fine-grained change events from a {@link PillListModel}.
 *
 * <p>Listeners are invoked on the Swing EDT.
 *
 * @author Charles Bryan
 * @version v0.1.0
 * @since v0.1.0
 */
@FunctionalInterface
public interface PillListDataListener {

  /**
   * Invoked after the model state changes.
   *
   * @param theEvent the change event describing what was added, removed, changed, or moved
   * @version v0.1.0
   * @since v0.1.0
   */
  void contentsChanged(PillListDataEvent theEvent);
}
