package com.owspfm.ui.components.chip.list;

/**
 * Receives selection-change events from a {@link ChipSelectionModel}.
 *
 * @param <T> the item type
 * @author Charles Bryan
 * @version v0.1.0
 * @since v0.1.0
 */
@FunctionalInterface
public interface ChipSelectionListener<T> {

  /**
   * Invoked after the selection set changes.
   *
   * @param event the event carrying the new selection
   * @version v0.1.0
   * @since v0.1.0
   */
  void selectionChanged(ChipSelectionEvent<T> event);
}
