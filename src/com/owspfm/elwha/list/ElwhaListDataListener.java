package com.owspfm.elwha.list;

/**
 * Receives fine-grained change events from an {@link ElwhaListModel}. Listeners run on the Swing
 * EDT; implementations must be cheap and side-effect free with respect to the firing model.
 *
 * <p>Replaces {@code CardListDataListener} / {@code ChipListDataListener}.
 *
 * @param <T> the item type
 * @author Charles Bryan
 * @version v0.1.0
 * @since v0.1.0
 */
@FunctionalInterface
public interface ElwhaListDataListener<T> {

  /**
   * Invoked after the model state changes.
   *
   * @param event the change event
   * @version v0.1.0
   * @since v0.1.0
   */
  void contentsChanged(ElwhaListDataEvent<T> event);
}
