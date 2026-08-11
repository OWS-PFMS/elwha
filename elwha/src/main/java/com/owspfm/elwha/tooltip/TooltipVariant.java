package com.owspfm.elwha.tooltip;

/**
 * The two M3 tooltip variants. Plain tooltips briefly describe a UI element; rich tooltips provide
 * additional context — an optional subhead, supporting text, and optional actions. See {@code
 * elwha-tooltip-research.md} §V.
 *
 * @author Charles Bryan (cfb3@uw.edu)
 * @version v0.4.0
 * @since v0.4.0
 */
public enum TooltipVariant {

  /**
   * The label-only inverse-surface bubble — the canonical affordance for icon-only controls.
   *
   * @version v0.4.0
   * @since v0.4.0
   */
  PLAIN,

  /**
   * The surface-container card with optional subhead, supporting text, and text-button actions.
   *
   * @version v0.4.0
   * @since v0.4.0
   */
  RICH
}
