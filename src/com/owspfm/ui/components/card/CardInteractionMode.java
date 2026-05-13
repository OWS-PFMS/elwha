package com.owspfm.ui.components.card;

/**
 * Interaction semantics for a {@link FlatCard}.
 *
 * <p>The interaction mode controls cursor, focus, keyboard handling, and the visual feedback
 * applied on hover, press, and selection. It is independent of the visual {@link CardVariant} and
 * can be combined freely with collapsible behavior.
 *
 * @author Charles Bryan
 * @version v0.1.0
 * @since v0.1.0
 */
public enum CardInteractionMode {

  /** Non-interactive surface. The card does not respond to mouse or keyboard input. */
  STATIC,

  /**
   * Provides hover feedback (subtle background lift) but does not fire actions or hold selection
   * state. Useful for cards whose internal controls handle their own clicks.
   *
   * @version v0.1.0
   * @since v0.1.0
   */
  HOVERABLE,

  /**
   * Behaves like a button. Hover and press feedback, focusable, fires an {@link
   * java.awt.event.ActionEvent} on click and on Space/Enter when focused.
   *
   * @version v0.1.0
   * @since v0.1.0
   */
  CLICKABLE,

  /**
   * Toggle behavior. Click or Space/Enter flips a persistent selected state with a distinct visual
   * indicator. Fires an {@link java.awt.event.ActionEvent} on toggle and a "selected" {@link
   * java.beans.PropertyChangeEvent}.
   *
   * @version v0.1.0
   * @since v0.1.0
   */
  SELECTABLE
}
