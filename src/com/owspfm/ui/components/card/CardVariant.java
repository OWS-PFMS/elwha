package com.owspfm.ui.components.card;

/**
 * Surface-style variants for {@link FlatCard}, mirroring Material 3 / shadcn semantics.
 *
 * <p>The variant controls only the background and border treatment of the card surface; it does not
 * affect layout, slot composition, or interaction behavior.
 *
 * @author Charles Bryan
 * @version v0.1.0
 * @since v0.1.0
 */
public enum CardVariant {

  /**
   * Filled background with a soft drop-shadow that scales with elevation. The default for surfaces
   * that should visually float above the page (e.g., dialog content, hero cards).
   *
   * @version v0.1.0
   * @since v0.1.0
   */
  ELEVATED,

  /**
   * Filled background with a hairline border and no shadow. Best for dense layouts where shadows
   * would create visual noise (e.g., grids of cards).
   *
   * @version v0.1.0
   * @since v0.1.0
   */
  OUTLINED,

  /**
   * Tinted background (slightly stronger than the page) with no border and no shadow. Useful for
   * grouping related content without competing with surrounding emphasis.
   *
   * @version v0.1.0
   * @since v0.1.0
   */
  FILLED
}
