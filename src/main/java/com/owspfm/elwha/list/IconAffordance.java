package com.owspfm.elwha.list;

/**
 * Visual treatment for the pin / anchor leading-slot affordance on {@link ElwhaItemList}. Pin and
 * anchor each carry their own independent setting — a clickable pin alongside a menu-only anchor
 * (or the reverse) is a common configuration.
 *
 * <p>Hoisted to the top level from the chip family's nested enum in story #69 (spec §8). The glyphs
 * render through {@code ElwhaChip}'s leading-affordance slot, so they apply when the adapter builds
 * an {@code ElwhaChip} and are inert — with no loss of pin/anchor behavior — on other component
 * types.
 *
 * @author Charles Bryan
 * @version v0.5.0
 * @since v0.5.0
 */
public enum IconAffordance {

  /**
   * No leading-icon affordance is shown. Pin / anchor state stays queryable through the API and
   * through the context menu.
   *
   * @version v0.5.0
   * @since v0.5.0
   */
  NONE,

  /**
   * Static glyph shown only while the item is pinned / anchored, not clickable. The default.
   *
   * @version v0.5.0
   * @since v0.5.0
   */
  INDICATOR,

  /**
   * Clickable button. For pin: an outline glyph on every item, filled when pinned; click toggles.
   * For anchor: a persistent filled glyph on the anchored item and a hover-revealed outline glyph
   * on the others; click sets or clears the anchor.
   *
   * @version v0.5.0
   * @since v0.5.0
   */
  BUTTON
}
