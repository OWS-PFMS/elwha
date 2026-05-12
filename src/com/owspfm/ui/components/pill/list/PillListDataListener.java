package com.owspfm.ui.components.pill.list;

/**
 * Receives fine-grained change events from a {@link PillListModel}.
 *
 * <p>Listeners are invoked on the Swing EDT.
 *
 * @author Charles Bryan
 * @version v1.1.0-alpha.3
 * @since v1.1.0-alpha.3
 */
@FunctionalInterface
public interface PillListDataListener {

  /**
   * Invoked after the model state changes.
   *
   * @param theEvent the change event describing what was added, removed, changed, or moved
   */
  void contentsChanged(PillListDataEvent theEvent);
}
