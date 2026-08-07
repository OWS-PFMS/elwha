package com.owspfm.elwha.list;

import javax.swing.JComponent;

/**
 * Maps a domain item to the {@link JComponent} that renders it inside an {@link ElwhaItemList}.
 *
 * <p>Invoked once per visible item on every rebuild. Return a freshly-built component each call —
 * the list installs its own listeners and chrome state on whatever it receives, and reusing one
 * instance across two items would cross those wires.
 *
 * <p>Components implementing {@link ElwhaListItemView} — {@code ElwhaCard} and {@code ElwhaChip} do
 * — additionally receive selection state, drag chrome, and list-driven interaction. Anything else
 * renders and lays out normally, just without those visuals.
 *
 * <pre>{@code
 * ElwhaItemAdapter<Factor> adapter =
 *     (factor, index) -> ElwhaChip.filterChip(factor.name()).setLeadingIcon(factor.icon());
 * }</pre>
 *
 * @param <T> the item type
 * @author Charles Bryan
 * @version v0.5.0
 * @since v0.5.0
 */
@FunctionalInterface
public interface ElwhaItemAdapter<T> {

  /**
   * Builds the component for one item.
   *
   * @param item the domain item
   * @param visibleIndex the item's index among the currently visible items — after filtering,
   *     sorting, and pin/anchor partitioning
   * @return the component to render; returning null skips the item
   * @version v0.5.0
   * @since v0.5.0
   */
  JComponent componentFor(T item, int visibleIndex);
}
