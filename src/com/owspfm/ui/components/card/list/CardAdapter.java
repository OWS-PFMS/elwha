package com.owspfm.ui.components.card.list;

import com.owspfm.ui.components.card.FlatCard;

/**
 * Single-method functional interface that maps a domain item to a {@link FlatCard}.
 *
 * <p>The adapter is invoked once per visible item by the host {@link FlatCardList}. Implementations
 * should return a freshly-built card for each call — the list does not pool or recycle card
 * instances (Swing handles repaints differently from Android RecyclerView, so item recycling is not
 * a clear win and complicates the API).
 *
 * <p>Example:
 *
 * <pre>{@code
 * CardAdapter<Cycle> adapter = (cycle, index) ->
 *     new FlatCard()
 *         .setVariant(CardVariant.OUTLINED)
 *         .setHeader("Cycle #" + (index + 1), cycle.summary())
 *         .setBody(buildCycleBody(cycle));
 * }</pre>
 *
 * @param <T> the item type
 * @author Charles Bryan
 * @version v1.1.0-alpha.2
 * @since v1.1.0-alpha.2
 */
@FunctionalInterface
public interface CardAdapter<T> {

  /**
   * Builds a card for the given item.
   *
   * @param theItem the domain item
   * @param theIndex the visible index of the item in the list
   * @return a non-null {@link FlatCard}
   */
  FlatCard cardFor(T theItem, int theIndex);
}
