package com.owspfm.elwha.chip;

/**
 * Interaction semantics for a {@link ElwhaChip}.
 *
 * <p>The interaction mode controls cursor, focus, keyboard handling, and the visual feedback
 * applied on hover, press, and selection. It is independent of the visual {@link ChipVariant}.
 *
 * @author Charles Bryan
 * @version v0.1.0
 * @since v0.1.0
 */
public enum ChipInteractionMode {

  /** Non-interactive surface. The chip does not respond to mouse or keyboard input. */
  STATIC,

  /**
   * Provides hover feedback but does not fire actions or hold selection state. Useful for chips
   * whose trailing button handles its own click.
   *
   * @version v0.1.0
   * @since v0.1.0
   */
  HOVERABLE,

  /**
   * Behaves like a push button. Hover and press feedback, focusable, fires an {@link
   * java.awt.event.ActionEvent} on click and on Space/Enter when focused.
   *
   * @version v0.1.0
   * @since v0.1.0
   */
  CLICKABLE,

  /**
   * Toggle behavior. Click or Space/Enter flips a persistent selected state with a distinct visual
   * indicator. Fires an {@link java.awt.event.ActionEvent} on toggle and a {@code "selected"}
   * {@link java.beans.PropertyChangeEvent}.
   *
   * @version v0.1.0
   * @since v0.1.0
   */
  SELECTABLE
}
