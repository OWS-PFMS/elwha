package com.owspfm.elwha.colorpicker;

import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.Rectangle;
import java.awt.Shape;
import java.awt.TexturePaint;
import java.awt.image.BufferedImage;

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
  private static final BufferedImage PATTERN = pattern();

  private Checkerboard() {}

  /**
   * Fills a shape with the checkerboard. A {@code TexturePaint} fill, not a clip — Java2D clipping
   * is never antialiased, so clipped rounded corners stair-step.
   *
   * @param g2 the graphics to paint into
   * @param shape the shape to fill
   */
  static void fill(final Graphics2D g2, final Shape shape) {
    final Graphics2D scoped = (Graphics2D) g2.create();
    try {
      final Rectangle bounds = shape.getBounds();
      scoped.setPaint(
          new TexturePaint(PATTERN, new Rectangle(bounds.x, bounds.y, SQUARE * 2, SQUARE * 2)));
      scoped.fill(shape);
    } finally {
      scoped.dispose();
    }
  }

  private static BufferedImage pattern() {
    final BufferedImage image =
        new BufferedImage(SQUARE * 2, SQUARE * 2, BufferedImage.TYPE_INT_RGB);
    final Graphics2D g2 = image.createGraphics();
    g2.setColor(LIGHT);
    g2.fillRect(0, 0, SQUARE * 2, SQUARE * 2);
    g2.setColor(DARK);
    g2.fillRect(SQUARE, 0, SQUARE, SQUARE);
    g2.fillRect(0, SQUARE, SQUARE, SQUARE);
    g2.dispose();
    return image;
  }
}
