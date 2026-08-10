package com.owspfm.elwha.menu;

/**
 * How a {@link Layout#GROUPED} {@link ElwhaMenu} separates its groups (M3 "Gaps &amp; dividers",
 * research §H/§N). Mirrors M3's exact nouns per the terminology lock (design §P).
 *
 * <p>The expressive default is {@link #GAP}; M3 forbids gaps in a scrollable menu ("currently
 * unsupported"), so a menu that must scroll is forced to {@link #DIVIDER}.
 *
 * @author Charles Bryan (cfb3@uw.edu)
 * @version v0.4.0
 * @since v0.4.0
 */
public enum Separator {

  /**
   * Expressive gap — each group renders as its own rounded card, separated by transparent space.
   * Unsupported when the menu scrolls.
   *
   * @version v0.4.0
   * @since v0.4.0
   */
  GAP,

  /**
   * Subtle 1&nbsp;dp divider line between groups within a single container. The forced choice for a
   * scrollable menu.
   *
   * @version v0.4.0
   * @since v0.4.0
   */
  DIVIDER
}
