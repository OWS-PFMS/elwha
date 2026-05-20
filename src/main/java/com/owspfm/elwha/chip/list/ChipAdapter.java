package com.owspfm.elwha.chip.list;

import com.owspfm.elwha.chip.ElwhaChip;

/**
 * Single-method functional interface that maps a domain item to a {@link ElwhaChip}.
 *
 * <p>Invoked once per visible item by the host {@link ElwhaChipList}. Implementations should return
 * a freshly-built chip for each call.
 *
 * <p>Example:
 *
 * <pre>{@code
 * ChipAdapter<Factor> adapter = (factor, idx) ->
 *     new ElwhaChip(factor.name())
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
   * @return a non-null {@link ElwhaChip}
   * @version v0.1.0
   * @since v0.1.0
   */
  ElwhaChip chipFor(T item, int index);
}
