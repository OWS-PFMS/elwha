package com.owspfm.elwha.colorpicker;

import java.awt.Color;
import java.util.List;

/**
 * The fixed swatch catalog backing the SWATCHES pane (design doc {@code
 * elwha-color-picker-design.md} §5): the nineteen 2014 Material palette hues plus a ten-step
 * monochrome ramp, each with the canonical 50–900 shade run. Constants are verbatim from the
 * published Material palette — the recognized "Material color picker" vocabulary (Flutter's
 * MaterialPicker, FlexColorPicker primaries).
 *
 * @author Charles Bryan
 * @version v0.5.0
 * @since v0.5.0
 */
final class MaterialSwatchCatalog {

  /** Shades per hue — the 50, 100, 200 … 900 run. */
  static final int SHADE_COUNT = 10;

  /** The column showing in the hue grid — index of the "500" shade. */
  static final int REPRESENTATIVE_SHADE = 5;

  private static final String[] SHADE_NAMES = {
    "50", "100", "200", "300", "400", "500", "600", "700", "800", "900",
  };

  private static final List<Hue> HUES =
      List.of(
          hue("Red", 0xFFEBEE, 0xFFCDD2, 0xEF9A9A, 0xE57373, 0xEF5350, 0xF44336, 0xE53935,
              0xD32F2F, 0xC62828, 0xB71C1C),
          hue("Pink", 0xFCE4EC, 0xF8BBD0, 0xF48FB1, 0xF06292, 0xEC407A, 0xE91E63, 0xD81B60,
              0xC2185B, 0xAD1457, 0x880E4F),
          hue("Purple", 0xF3E5F5, 0xE1BEE7, 0xCE93D8, 0xBA68C8, 0xAB47BC, 0x9C27B0, 0x8E24AA,
              0x7B1FA2, 0x6A1B9A, 0x4A148C),
          hue("Deep Purple", 0xEDE7F6, 0xD1C4E9, 0xB39DDB, 0x9575CD, 0x7E57C2, 0x673AB7, 0x5E35B1,
              0x512DA8, 0x4527A0, 0x311B92),
          hue("Indigo", 0xE8EAF6, 0xC5CAE9, 0x9FA8DA, 0x7986CB, 0x5C6BC0, 0x3F51B5, 0x3949AB,
              0x303F9F, 0x283593, 0x1A237E),
          hue("Blue", 0xE3F2FD, 0xBBDEFB, 0x90CAF9, 0x64B5F6, 0x42A5F5, 0x2196F3, 0x1E88E5,
              0x1976D2, 0x1565C0, 0x0D47A1),
          hue("Light Blue", 0xE1F5FE, 0xB3E5FC, 0x81D4FA, 0x4FC3F7, 0x29B6F6, 0x03A9F4, 0x039BE5,
              0x0288D1, 0x0277BD, 0x01579B),
          hue("Cyan", 0xE0F7FA, 0xB2EBF2, 0x80DEEA, 0x4DD0E1, 0x26C6DA, 0x00BCD4, 0x00ACC1,
              0x0097A7, 0x00838F, 0x006064),
          hue("Teal", 0xE0F2F1, 0xB2DFDB, 0x80CBC4, 0x4DB6AC, 0x26A69A, 0x009688, 0x00897B,
              0x00796B, 0x00695C, 0x004D40),
          hue("Green", 0xE8F5E9, 0xC8E6C9, 0xA5D6A7, 0x81C784, 0x66BB6A, 0x4CAF50, 0x43A047,
              0x388E3C, 0x2E7D32, 0x1B5E20),
          hue("Light Green", 0xF1F8E9, 0xDCEDC8, 0xC5E1A5, 0xAED581, 0x9CCC65, 0x8BC34A, 0x7CB342,
              0x689F38, 0x558B2F, 0x33691E),
          hue("Lime", 0xF9FBE7, 0xF0F4C3, 0xE6EE9C, 0xDCE775, 0xD4E157, 0xCDDC39, 0xC0CA33,
              0xAFB42B, 0x9E9D24, 0x827717),
          hue("Yellow", 0xFFFDE7, 0xFFF9C4, 0xFFF59D, 0xFFF176, 0xFFEE58, 0xFFEB3B, 0xFDD835,
              0xFBC02D, 0xF9A825, 0xF57F17),
          hue("Amber", 0xFFF8E1, 0xFFECB3, 0xFFE082, 0xFFD54F, 0xFFCA28, 0xFFC107, 0xFFB300,
              0xFFA000, 0xFF8F00, 0xFF6F00),
          hue("Orange", 0xFFF3E0, 0xFFE0B2, 0xFFCC80, 0xFFB74D, 0xFFA726, 0xFF9800, 0xFB8C00,
              0xF57C00, 0xEF6C00, 0xE65100),
          hue("Deep Orange", 0xFBE9E7, 0xFFCCBC, 0xFFAB91, 0xFF8A65, 0xFF7043, 0xFF5722, 0xF4511E,
              0xE64A19, 0xD84315, 0xBF360C),
          hue("Brown", 0xEFEBE9, 0xD7CCC8, 0xBCAAA4, 0xA1887F, 0x8D6E63, 0x795548, 0x6D4C41,
              0x5D4037, 0x4E342E, 0x3E2723),
          hue("Grey", 0xFAFAFA, 0xF5F5F5, 0xEEEEEE, 0xE0E0E0, 0xBDBDBD, 0x9E9E9E, 0x757575,
              0x616161, 0x424242, 0x212121),
          hue("Blue Grey", 0xECEFF1, 0xCFD8DC, 0xB0BEC5, 0x90A4AE, 0x78909C, 0x607D8B, 0x546E7A,
              0x455A64, 0x37474F, 0x263238),
          hue("Monochrome", 0xFFFFFF, 0xE6E6E6, 0xCCCCCC, 0xB3B3B3, 0x999999, 0x808080, 0x666666,
              0x4D4D4D, 0x333333, 0x000000));

  private MaterialSwatchCatalog() {}

  /**
   * Returns the catalog's hues in spectral-then-neutral order.
   *
   * @return an immutable list of twenty hues
   */
  static List<Hue> hues() {
    return HUES;
  }

  /**
   * Returns the display name of a shade column ("50" … "900").
   *
   * @param shadeIndex the shade index, {@code 0 … SHADE_COUNT-1}
   * @return the shade name
   */
  static String shadeName(final int shadeIndex) {
    return SHADE_NAMES[shadeIndex];
  }

  /**
   * Finds the catalog position of an exact (RGB-equal) color.
   *
   * @param color the color to look up
   * @return {@code {hueIndex, shadeIndex}}, or {@code null} when the color is not in the catalog
   */
  static int[] find(final Color color) {
    final int rgb = color.getRGB() & 0xFFFFFF;
    for (int h = 0; h < HUES.size(); h++) {
      final Color[] shades = HUES.get(h).shades();
      for (int s = 0; s < shades.length; s++) {
        if ((shades[s].getRGB() & 0xFFFFFF) == rgb) {
          return new int[] {h, s};
        }
      }
    }
    return null;
  }

  private static Hue hue(final String name, final int... rgb) {
    final Color[] shades = new Color[rgb.length];
    for (int i = 0; i < rgb.length; i++) {
      shades[i] = new Color(rgb[i]);
    }
    return new Hue(name, shades);
  }

  /**
   * One catalog hue — a display name plus its ten-shade run.
   *
   * @param name the hue's display name (a11y cell naming)
   * @param shades the 50–900 shade run, light to dark
   * @author Charles Bryan
   * @version v0.5.0
   * @since v0.5.0
   */
  record Hue(String name, Color[] shades) {}
}
