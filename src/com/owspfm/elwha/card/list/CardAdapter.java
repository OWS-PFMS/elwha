package com.owspfm.elwha.card.list;

import com.owspfm.elwha.card.ElwhaCard;

/**
 * Single-method functional interface that maps a domain item to a {@link ElwhaCard}.
 *
 * <p>The adapter is invoked once per visible item by the host {@link ElwhaCardList}. Implementations
 * should return a freshly-built card for each call — the list does not pool or recycle card
 * instances (Swing handles repaints differently from Android RecyclerView, so item recycling is not
 * a clear win and complicates the API).
 *
 * <p>Example:
 *
 * <pre>{@code
 * CardAdapter<Cycle> adapter = (cycle, index) ->
 *     new ElwhaCard()
 *         .setVariant(CardVariant.OUTLINED)
 *         .setHeader("Cycle #" + (index + 1), cycle.summary())
 *         .setBody(buildCycleBody(cycle));
 * }</pre>
 *
 * @param <T> the item type
 * @author Charles Bryan
 * @version v0.1.0
 * @since v0.1.0
 */
@FunctionalInterface
public interface CardAdapter<T> {

  /**
   * Builds a card for the given item.
   *
   * @param item the domain item
   * @param index the visible index of the item in the list
   * @return a non-null {@link ElwhaCard}
   * @version v0.1.0
   * @since v0.1.0
   */
  ElwhaCard cardFor(T item, int index);
}
