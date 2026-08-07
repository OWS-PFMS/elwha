package com.owspfm.elwha.list;

/**
 * Receives selection-change events from an {@link ElwhaSelectionModel}.
 *
 * @param <T> the item type
 * @author Charles Bryan
 * @version v0.5.0
 * @since v0.5.0
 */
@FunctionalInterface
public interface ElwhaSelectionListener<T> {

  /**
   * Invoked after the selection changes — from a user gesture or a programmatic write alike. Writes
   * that leave the selection unchanged do not reach listeners.
   *
   * @param event the event carrying the new selection
   * @version v0.5.0
   * @since v0.5.0
   */
  void selectionChanged(ElwhaSelectionEvent<T> event);
}
