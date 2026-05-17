package com.owspfm.elwha.card;

/**
 * The two M3-sanctioned divider treatments for {@link ElwhaCardDivider}. See {@code
 * docs/research/elwha-card-v3-spec.md} §5.4.
 *
 * @author Charles Bryan
 * @version v0.2.0
 * @since v0.2.0
 */
public enum DividerStyle {
  /**
   * Spans card edge-to-edge — intended to ignore the card's content padding. (Visual edge-bleed is
   * achieved via Card layout integration; in the v0.2 stub the divider paints across its own
   * bounds, so consumers wanting true edge bleed must configure the Card padding accordingly.)
   */
  FULL,

  /** Respects content padding — paints across the divider's own bounds inside the padded row. */
  INSET
}
