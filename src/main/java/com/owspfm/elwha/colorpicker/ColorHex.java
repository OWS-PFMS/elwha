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

  /**
   * Parses web hex — {@code #RRGGBB} or shorthand {@code #RGB}, plus {@code #RRGGBBAA} /
   * {@code #RGBA} when {@code allowAlpha}. The leading {@code #} is optional; case is ignored.
   *
   * @param text the candidate text
   * @param allowAlpha whether 4/8-digit alpha forms are accepted
   * @return the parsed color, or {@code null} when the text is not valid hex
   */
  static Color parse(final String text, final boolean allowAlpha) {
    if (text == null) {
      return null;
    }
    String digits = text.trim();
    if (digits.startsWith("#")) {
      digits = digits.substring(1);
    }
    if (digits.length() == 3 || (allowAlpha && digits.length() == 4)) {
      final StringBuilder expanded = new StringBuilder();
      for (final char c : digits.toCharArray()) {
        expanded.append(c).append(c);
      }
      digits = expanded.toString();
    }
    final boolean withAlpha = digits.length() == 8;
    if (digits.length() != 6 && !(allowAlpha && withAlpha)) {
      return null;
    }
    try {
      final int red = Integer.parseInt(digits.substring(0, 2), 16);
      final int green = Integer.parseInt(digits.substring(2, 4), 16);
      final int blue = Integer.parseInt(digits.substring(4, 6), 16);
      final int alpha = withAlpha ? Integer.parseInt(digits.substring(6, 8), 16) : 255;
      return new Color(red, green, blue, alpha);
    } catch (final NumberFormatException e) {
      return null;
    }
  }
}
