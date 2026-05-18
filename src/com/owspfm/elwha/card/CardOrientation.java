package com.owspfm.elwha.card;

/**
 * The two compose-time orientation choices for {@link ElwhaCard}. See {@code
 * docs/research/elwha-card-v3-spec.md} §15.
 *
 * <p>{@link #VERTICAL} cards use {@code BoxLayout(Y_AXIS)} — {@code add()} order is stack order.
 * {@link #HORIZONTAL} cards use a custom 2-column LayoutManager — content goes in via {@link
 * ElwhaCard#setLeadingColumn} / {@link ElwhaCard#setTrailingColumn}, not {@code add()}.
 *
 * <p>Switching orientation at runtime is supported but expensive (rebuilds the layout). Consumers
 * should pick orientation at compose time.
 *
 * @author Charles Bryan
 * @version v0.2.0
 * @since v0.2.0
 */
public enum CardOrientation {
  /** Vertical stack — {@code BoxLayout(Y_AXIS)}. {@code add()} order = layout order. */
  VERTICAL,

  /** Two-column horizontal — explicit leading / trailing columns. {@code add()} is not used. */
  HORIZONTAL
}
