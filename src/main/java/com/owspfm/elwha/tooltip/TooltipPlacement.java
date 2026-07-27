package com.owspfm.elwha.tooltip;

/**
 * The preferred vertical side of the anchor — M3: "Tooltips appear directly below or above [the]
 * anchor element." The engine flips to the opposite side when the preferred side would clip the
 * viewport ({@code elwha-tooltip-design.md} §5).
 *
 * @author Charles Bryan (cfb3@uw.edu)
 * @version v0.4.0
 * @since v0.4.0
 */
public enum TooltipPlacement {

  /**
   * Above the anchor (the M3 default preference).
   *
   * @version v0.4.0
   * @since v0.4.0
   */
  ABOVE,

  /**
   * Below the anchor.
   *
   * @version v0.4.0
   * @since v0.4.0
   */
  BELOW
}
