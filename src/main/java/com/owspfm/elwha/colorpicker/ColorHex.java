package com.owspfm.elwha.colorpicker;

import java.awt.Color;

/**
 * Hex formatting for the color picker. Web byte order throughout: {@code #RRGGBB}, with alpha
 * appended as {@code #RRGGBBAA} (research doc {@code elwha-color-picker-research.md} §M) — AWT's
 * {@code 0xAARRGGBB} int layout is an implementation detail that never leaks into display text.
 *
 * @author Charles Bryan
 * @version v0.5.0
 * @since v0.5.0
 */
final class ColorHex {

  private ColorHex() {}

  /**
   * Formats a color as uppercase web hex — {@code #RRGGBB}, or {@code #RRGGBBAA} when {@code
   * withAlpha}.
   *
   * @param color the color to format
   * @param withAlpha whether to append the alpha byte
   * @return the uppercase hex string, leading {@code #}
   */
  static String format(final Color color, final boolean withAlpha) {
    final String rgb =
        String.format("#%02X%02X%02X", color.getRed(), color.getGreen(), color.getBlue());
    return withAlpha ? rgb + String.format("%02X", color.getAlpha()) : rgb;
  }
}
