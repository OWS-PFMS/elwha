package com.owspfm.elwha.tooltip;

/**
 * Horizontal alignment of the tooltip against its anchor — M3: plain tooltips "can be placed flush
 * with either the end, center, or start of the anchor"; rich tooltips hang off the start or end
 * corner. {@code START}/{@code END} are direction-aware: they resolve through the anchor's {@link
 * java.awt.ComponentOrientation} and mirror under RTL ({@code elwha-tooltip-design.md} §5).
 *
 * @author Charles Bryan (cfb3@uw.edu)
 * @version v0.4.0
 * @since v0.4.0
 */
public enum TooltipAlignment {

  /**
   * The tooltip's leading edge sits flush with the anchor's leading edge.
   *
   * @version v0.4.0
   * @since v0.4.0
   */
  START,

  /**
   * The tooltip centers on the anchor (the plain-variant default).
   *
   * @version v0.4.0
   * @since v0.4.0
   */
  CENTER,

  /**
   * The tooltip's trailing edge sits flush with the anchor's trailing edge (the rich-variant
   * default).
   *
   * @version v0.4.0
   * @since v0.4.0
   */
  END
}
