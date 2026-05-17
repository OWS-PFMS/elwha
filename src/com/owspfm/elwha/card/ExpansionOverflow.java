package com.owspfm.elwha.card;

/**
 * Strategy for how an expanded {@link ElwhaCard} handles content taller than the resting layout.
 * See {@code docs/research/elwha-card-v3-spec.md} §14.4.
 *
 * @author Charles Bryan
 * @version v0.2.0
 * @since v0.2.0
 */
public enum ExpansionOverflow {
  /**
   * Default. The card grows to its expanded preferred size; sibling layout reacts. Matches the V1
   * collapse behavior.
   */
  GROW,

  /**
   * The card caps at an internal max height; the body installs an internal {@code JScrollPane}.
   * Siblings stay put. M3 desktop pattern for cards in tight layouts.
   */
  SCROLL
}
