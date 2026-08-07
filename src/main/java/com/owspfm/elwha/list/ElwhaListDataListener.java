package com.owspfm.elwha.list;

/**
 * Receives fine-grained change events from an {@link ElwhaListModel}.
 *
 * <p>Listeners are invoked synchronously, on whichever thread mutated the model — mutate on the
 * Swing EDT.
 *
 * @param <T> the item type
 * @author Charles Bryan
 * @version v0.5.0
 * @since v0.5.0
 */
@FunctionalInterface
public interface ElwhaListDataListener<T> {

  /**
   * Invoked after the model's contents change.
   *
   * @param event the event describing what was added, removed, changed, moved, or replaced
   *     wholesale
   * @version v0.5.0
   * @since v0.5.0
   */
  void contentsChanged(ElwhaListDataEvent<T> event);
}
