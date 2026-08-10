package com.owspfm.elwha.buttongroup;

/**
 * The single colour style a {@link ButtonGroupVariant#CONNECTED} {@link ElwhaButtonGroup} applies
 * uniformly to every segment — {@link #FILLED}, {@link #TONAL}, or {@link #OUTLINED}.
 *
 * <p>M3 forbids mixing colour styles within a connected group; an {@code ElwhaButtonGroup} enforces
 * this by owning one style and stamping it onto every segment (design doc §8). The three styles map
 * onto {@link com.owspfm.elwha.button.ButtonVariant} / {@link
 * com.owspfm.elwha.iconbutton.IconButtonVariant}. The elevated and text treatments are deliberately
 * absent — M3 marks elevated "not recommended" in a group and excludes text buttons entirely.
 *
 * <p>The style is ignored by the {@link ButtonGroupVariant#STANDARD} variant, where M3 allows
 * segments to mix colour styles freely.
 *
 * @author Charles Bryan
 * @version v0.3.0
 * @since v0.3.0
 */
public enum ButtonGroupColorStyle {

  /**
   * The high-emphasis filled treatment — maps to {@link
   * com.owspfm.elwha.button.ButtonVariant#FILLED} / {@link
   * com.owspfm.elwha.iconbutton.IconButtonVariant#FILLED}.
   *
   * @version v0.3.0
   * @since v0.3.0
   */
  FILLED,

  /**
   * The moderate-emphasis tonal treatment — maps to {@link
   * com.owspfm.elwha.button.ButtonVariant#FILLED_TONAL} / {@link
   * com.owspfm.elwha.iconbutton.IconButtonVariant#FILLED_TONAL}.
   *
   * @version v0.3.0
   * @since v0.3.0
   */
  TONAL,

  /**
   * The medium-emphasis outlined treatment — maps to {@link
   * com.owspfm.elwha.button.ButtonVariant#OUTLINED} / {@link
   * com.owspfm.elwha.iconbutton.IconButtonVariant#OUTLINED}.
   *
   * @version v0.3.0
   * @since v0.3.0
   */
  OUTLINED
}
