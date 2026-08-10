package com.owspfm.elwha.progress;

import java.awt.Color;
import java.awt.Point;
import java.awt.image.BufferedImage;

/**
 * Polar probes for the circular indicator's chrome.
 *
 * <p>A ring is the wrong shape for row-scan probing, so these helpers sample <em>along</em> it:
 * pick an angle, land on the stroke's centerline, read the pixel. Sweeping every degree turns "how
 * much arc is painted" and "is any track visible anywhere" into counts rather than eyeballed
 * coordinates.
 *
 * <p>Angles are the mathematical convention the indicator paints in — degrees counter-clockwise
 * from three o'clock, so 90° is twelve o'clock, which is where the determinate arc starts.
 */
final class Ring {

  private Ring() {}

  /** The stroke centerline radius for a flat indicator at its default geometry. */
  static float radiusOf(final ElwhaCircularProgressIndicator indicator) {
    final int d = indicator.getDiameter();
    final float thickness = indicator.getTrackThickness();
    final float waveReserve = indicator.isWavy() ? indicator.getWaveAmplitude() : 0f;
    return (d - thickness) / 2f - waveReserve;
  }

  /**
   * The pixel on the stroke centerline at {@code degrees}, for an indicator painted at its size.
   */
  static Point at(final ElwhaCircularProgressIndicator indicator, final double degrees) {
    final float center = indicator.getPreferredSize().width / 2f;
    final float r = radiusOf(indicator);
    final double theta = Math.toRadians(degrees);
    return new Point(
        Math.round(center + (float) (r * Math.cos(theta))),
        Math.round(center - (float) (r * Math.sin(theta))));
  }

  /** Whether the pixel on the ring at {@code degrees} is within tolerance of {@code want}. */
  static boolean isAt(
      final BufferedImage image,
      final ElwhaCircularProgressIndicator indicator,
      final double degrees,
      final Color want) {
    final Point p = at(indicator, degrees);
    if (p.x < 0 || p.y < 0 || p.x >= image.getWidth() || p.y >= image.getHeight()) {
      return false;
    }
    final Color got = new Color(image.getRGB(p.x, p.y), true);
    return Math.abs(got.getRed() - want.getRed()) <= 10
        && Math.abs(got.getGreen() - want.getGreen()) <= 10
        && Math.abs(got.getBlue() - want.getBlue()) <= 10;
  }

  /** How many whole degrees around the ring carry {@code want} — the painted arc length. */
  static int degreesOf(
      final BufferedImage image, final ElwhaCircularProgressIndicator indicator, final Color want) {
    int hits = 0;
    for (int deg = 0; deg < 360; deg++) {
      if (isAt(image, indicator, deg, want)) {
        hits++;
      }
    }
    return hits;
  }
}
