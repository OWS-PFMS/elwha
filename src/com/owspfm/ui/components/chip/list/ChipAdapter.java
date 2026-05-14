package com.owspfm.ui.components.chip.list;

import com.owspfm.ui.components.chip.FlatChip;

/**
 * Single-method functional interface that maps a domain item to a {@link FlatChip}.
 *
 * <p>Invoked once per visible item by the host {@link FlatChipList}. Implementations should return
 * a freshly-built chip for each call.
 *
 * <p>Example:
 *
 * <pre>{@code
 * ChipAdapter<Factor> adapter = (factor, idx) ->
 *     new FlatChip(factor.name())
 *         .setLeadingIcon(factor.icon())
 *         .setVariant(ChipVariant.OUTLINED);
 * }</pre>
 *
 * @param <T> the item type
 * @author Charles Bryan
 * @version v0.1.0
 * @since v0.1.0
 */
@FunctionalInterface
public interface ChipAdapter<T> {

  /**
   * Builds a chip for the given item.
   *
   * @param item the domain item
   * @param index the visible index of the item in the list
   * @return a non-null {@link FlatChip}
   * @version v0.1.0
   * @since v0.1.0
   */
  FlatChip chipFor(T item, int index);
}
