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
   * Spans card edge-to-edge — ignores the card's content padding. The card's {@code
   * VerticalCardLayout} lays a {@code FULL} divider at full card width with {@code x=0}, regardless
   * of its position in the child order, so the painted 1 dp line meets the chassis's rounded outer
   * corners.
   */
  FULL,

  /** Respects content padding — paints across the divider's own bounds inside the padded row. */
  INSET
}
