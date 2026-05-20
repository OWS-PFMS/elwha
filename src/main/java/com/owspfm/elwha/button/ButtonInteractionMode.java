package com.owspfm.elwha.button;

/**
 * Interaction semantics for an {@link ElwhaButton}.
 *
 * <p>Controls focus, keyboard handling, and whether the button holds a persistent selected state.
 * The {@code SELECTABLE} mode is rejected at runtime on the {@link ButtonVariant#TEXT} variant per
 * M3 spec — see {@code docs/research/elwha-button-design.md} §7 for the symmetric guard rule.
 *
 * @author Charles Bryan
 * @version v0.2.0
 * @since v0.2.0
 */
public enum ButtonInteractionMode {

  /**
   * Push-button behavior. Fires an {@link java.awt.event.ActionEvent} on click and on Space/Enter
   * when focused. No persistent selected state.
   *
   * @version v0.2.0
   * @since v0.2.0
   */
  CLICKABLE,

  /**
   * Toggle behavior. Click or Space/Enter flips a persistent selected state with a visual indicator
   * (variant-specific surface swap for {@link ButtonVariant#FILLED} + {@link
   * ButtonVariant#OUTLINED}; uniform {@link com.owspfm.elwha.theme.StateLayer#SELECTED} overlay +
   * {@link com.owspfm.elwha.theme.ColorRole#PRIMARY} border swap for {@link ButtonVariant#ELEVATED}
   * + {@link ButtonVariant#FILLED_TONAL}; rejected on {@link ButtonVariant#TEXT}). Fires an {@link
   * java.awt.event.ActionEvent} on toggle and a {@code "selected"} {@link
   * java.beans.PropertyChangeEvent}.
   *
   * @version v0.2.0
   * @since v0.2.0
   */
  SELECTABLE
}
