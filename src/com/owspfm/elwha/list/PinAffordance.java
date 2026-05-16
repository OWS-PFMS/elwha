package com.owspfm.elwha.list;

/**
 * Visual treatment for the pin leading-slot affordance on an {@link ElwhaItemList}. Lifted from
 * Chip's {@code IconAffordance}; type-distinguished from {@link AnchorAffordance} so a future
 * divergence between the two doesn't require a rename pass.
 *
 * @author Charles Bryan
 * @version v0.1.0
 * @since v0.1.0
 */
public enum PinAffordance {

  /** No leading-icon affordance shown. The pin state remains queryable via API. */
  NONE,

  /**
   * Static glyph shown only when the item is pinned — not clickable. Default; matches the
   * historical "leading icon as state indicator" behavior.
   *
   * @version v0.1.0
   * @since v0.1.0
   */
  INDICATOR,

  /**
   * Clickable button: outline glyph on every item, filled when pinned; click toggles.
   *
   * @version v0.1.0
   * @since v0.1.0
   */
  BUTTON
}
