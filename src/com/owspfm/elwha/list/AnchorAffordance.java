package com.owspfm.elwha.list;

/**
 * Visual treatment for the anchor leading-slot affordance on an {@link ElwhaItemList}. Lifted from
 * Chip's {@code IconAffordance}; type-distinguished from {@link PinAffordance} so a future
 * divergence between the two doesn't require a rename pass.
 *
 * @author Charles Bryan
 * @version v0.1.0
 * @since v0.1.0
 */
public enum AnchorAffordance {

  /** No leading-icon affordance shown. The anchor state remains queryable via API. */
  NONE,

  /**
   * Static glyph shown only when the item is the anchor — not clickable. Default; matches the
   * historical "leading icon as state indicator" behavior.
   *
   * @version v0.1.0
   * @since v0.1.0
   */
  INDICATOR,

  /**
   * Clickable button: persistent filled glyph on the anchored item; hover-revealed outline glyph on
   * non-anchored items; click sets / clears the anchor.
   *
   * @version v0.1.0
   * @since v0.1.0
   */
  BUTTON
}
