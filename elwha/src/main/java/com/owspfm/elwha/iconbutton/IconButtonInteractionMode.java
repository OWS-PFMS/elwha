package com.owspfm.elwha.iconbutton;

/**
 * Interaction semantics for an {@link ElwhaIconButton}.
 *
 * <p>The interaction mode controls focus, keyboard handling, and whether the button holds a
 * persistent selected state. Unlike {@link com.owspfm.elwha.chip.ChipInteractionMode} there is no
 * {@code STATIC} or {@code HOVERABLE} option — an icon button always interacts.
 *
 * @author Charles Bryan
 * @version v0.1.0
 * @since v0.1.0
 */
public enum IconButtonInteractionMode {

  /**
   * Push-button behavior. Fires an {@link java.awt.event.ActionEvent} on click and on Space/Enter
   * when focused. No persistent selected state.
   *
   * @version v0.1.0
   * @since v0.1.0
   */
  CLICKABLE,

  /**
   * Toggle behavior. Click or Space/Enter flips a persistent selected state with a visual indicator
   * (state-layer overlay, optional icon swap via {@code setIcons}, and — for {@link
   * IconButtonVariant#STANDARD} — a {@link com.owspfm.elwha.theme.ColorRole#PRIMARY} icon tint).
   * Fires an {@link java.awt.event.ActionEvent} on toggle and a {@code "selected"} {@link
   * java.beans.PropertyChangeEvent}.
   *
   * @version v0.1.0
   * @since v0.1.0
   */
  SELECTABLE
}
