package com.owspfm.elwha.colorpicker;

import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.Rectangle;
import java.awt.Shape;

/**
 * The transparency checkerboard backing alpha tracks and the preview swatch (design doc {@code
 * elwha-color-picker-design.md} §9). Deliberately untokenized — the white/grey board depicts
 * physical transparency, not themed chrome, and stays identical in light and dark (the convention
 * every surveyed picker shares).
 *
 * @author Charles Bryan
 * @version v0.5.0
 * @since v0.5.0
 */
final class Checkerboard {

  /** The board's square edge in pixels. */
  static final int SQUARE = 8;

  private static final Color LIGHT = new Color(0xFFFFFF);
  private static final Color DARK = new Color(0xCCCCCC);

  private Checkerboard() {}

  /**
   * Fills a shape with the checkerboard (clipped to it).
   *
   * @param g2 the graphics to paint into
   * @param shape the shape to fill
   */
  static void fill(final Graphics2D g2, final Shape shape) {
    final Graphics2D clipped = (Graphics2D) g2.create();
    try {
      clipped.clip(shape);
      final Rectangle bounds = shape.getBounds();
      for (int y = bounds.y; y < bounds.y + bounds.height; y += SQUARE) {
        for (int x = bounds.x; x < bounds.x + bounds.width; x += SQUARE) {
          final boolean even = ((x - bounds.x) / SQUARE + (y - bounds.y) / SQUARE) % 2 == 0;
          clipped.setColor(even ? LIGHT : DARK);
          clipped.fillRect(x, y, SQUARE, SQUARE);
        }
      }
    } finally {
      clipped.dispose();
    }
  }
}
