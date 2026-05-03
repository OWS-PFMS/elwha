package com.owspfm.ui.components.card.list;

/**
 * Receives fine-grained change events from a {@link CardListModel}.
 *
 * <p>Listeners are invoked on the Swing EDT. Implementations must be cheap and side-effect free
 * with respect to the firing model — re-entrant mutations are not supported.
 *
 * @author Charles Bryan
 * @version v1.1.0-alpha.2
 * @since v1.1.0-alpha.2
 */
@FunctionalInterface
public interface CardListDataListener {

  /**
   * Invoked after the model state changes.
   *
   * @param theEvent the change event describing what was added, removed, changed, or moved
   */
  void contentsChanged(CardListDataEvent theEvent);
}
