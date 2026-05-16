package com.owspfm.elwha.list;

import java.awt.Component;

/**
 * Single-method functional interface that maps a domain item to a rendered {@link Component}.
 *
 * <p>The adapter is invoked once per visible item by {@link ElwhaItemList}. Implementations should
 * return a freshly-built component for each call — the list does not pool or recycle.
 *
 * <p>Replaces {@code com.owspfm.elwha.card.list.CardAdapter} and {@code
 * com.owspfm.elwha.chip.list.ChipAdapter}; the unified generic class lets one adapter type cover
 * any component the consumer wants to render (Card, Chip, IconButton, custom).
 *
 * <p>Example:
 *
 * <pre>{@code
 * ElwhaListAdapter<Cycle> adapter = (cycle, index) ->
 *     new ElwhaCard("Cycle #" + (index + 1))
 *         .setVariant(CardVariant.OUTLINED)
 *         .setSubhead(cycle.summary());
 * }</pre>
 *
 * @param <T> the item type
 * @author Charles Bryan
 * @version v0.1.0
 * @since v0.1.0
 */
@FunctionalInterface
public interface ElwhaListAdapter<T> {

  /**
   * Builds a component for the given item.
   *
   * @param item the domain item
   * @param index the visible index of the item in the list
   * @return a non-null {@link Component}
   * @version v0.1.0
   * @since v0.1.0
   */
  Component componentFor(T item, int index);
}
