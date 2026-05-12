package com.owspfm.ui.components.pill.list;

import com.owspfm.ui.components.pill.FlatPill;

/**
 * Single-method functional interface that maps a domain item to a {@link FlatPill}.
 *
 * <p>Invoked once per visible item by the host {@link FlatPillList}. Implementations should return
 * a freshly-built pill for each call.
 *
 * <p>Example:
 *
 * <pre>{@code
 * PillAdapter<Factor> adapter = (factor, idx) ->
 *     new FlatPill(factor.name())
 *         .setLeadingIcon(factor.icon())
 *         .setVariant(PillVariant.OUTLINED);
 * }</pre>
 *
 * @param <T> the item type
 * @author Charles Bryan
 * @version v1.1.0-alpha.3
 * @since v1.1.0-alpha.3
 */
@FunctionalInterface
public interface PillAdapter<T> {

  /**
   * Builds a pill for the given item.
   *
   * @param theItem the domain item
   * @param theIndex the visible index of the item in the list
   * @return a non-null {@link FlatPill}
   */
  FlatPill pillFor(T theItem, int theIndex);
}
