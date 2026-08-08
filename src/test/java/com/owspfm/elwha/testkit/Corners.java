package com.owspfm.elwha.testkit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.within;

import java.awt.Color;
import java.awt.image.BufferedImage;

/**
 * Measures the corner radius a component actually rendered, so a shape assertion is evidence rather
 * than a reading of the paint code.
 *
 * <p>The probe counts, inside a square box anchored on one corner of the painted body, how many
 * pixels fall <em>outside</em> the body silhouette — the area the rounding cut away. For a
 * round-rect of corner radius {@code r} that cut is exactly {@code r² · (1 − π/4)}, so the radius
 * inverts straight back out of the count. Anti-aliased edge pixels are classified by whichever
 * reference color they sit nearer, which puts the counted boundary on the geometric arc rather than
 * inside or outside it.
 *
 * <p>Probe against a configuration with no border stroke: a stroke in a third color has no side of
 * the two-way classification to land on. Render onto a ground far from the body fill — the bundled
 * tests use magenta — and keep the probe box clear of glyphs and content by sizing {@code span} to
 * the expected radius. An elevated body needs both: sample {@code outside} from the halo just past
 * the corner rather than from the flat ground, and keep the ground itself far from the fill, or the
 * halo gradient reads as body and the measurement collapses toward half.
 *
 * @author Charles Bryan
 * @version v0.5.0
 * @since v0.5.0
 */
public final class Corners {

  /** The fraction of a corner's bounding square that a quarter-round cuts away: {@code 1 − π/4}. */
  private static final double CUT_FRACTION = 1.0 - Math.PI / 4.0;

  private Corners() {}

  /**
   * Estimates the top-left corner radius of a body painted at {@code (bodyX, bodyY)}.
   *
   * @param image the raster to probe
   * @param bodyX the body's left edge in image coordinates
   * @param bodyY the body's top edge in image coordinates
   * @param span the probe box edge in px; must be at least the radius being measured
   * @param body the body's resting fill color
   * @param outside the color immediately outside the corner (the ground, or the local halo)
   * @return the measured corner radius in px
   * @version v0.5.0
   * @since v0.5.0
   */
  public static double topLeftRadius(
      final BufferedImage image,
      final int bodyX,
      final int bodyY,
      final int span,
      final Color body,
      final Color outside) {
    int cut = 0;
    for (int j = 0; j < span; j++) {
      for (int i = 0; i < span; i++) {
        if (!nearerToBody(image.getRGB(bodyX + i, bodyY + j), body, outside)) {
          cut++;
        }
      }
    }
    return Math.sqrt(cut / CUT_FRACTION);
  }

  /**
   * Asserts a body's top-left corner renders at {@code expectedRadius}, within a tolerance that
   * absorbs the half-pixel the anti-aliased arc can shift the counted boundary.
   *
   * @param image the raster to probe
   * @param bodyX the body's left edge in image coordinates
   * @param bodyY the body's top edge in image coordinates
   * @param span the probe box edge in px; must be at least the radius being measured
   * @param body the body's resting fill color
   * @param outside the color immediately outside the corner
   * @param expectedRadius the radius the component is contracted to paint
   * @param what human-readable description of what the probe verifies
   * @version v0.5.0
   * @since v0.5.0
   */
  public static void assertTopLeftRadius(
      final BufferedImage image,
      final int bodyX,
      final int bodyY,
      final int span,
      final Color body,
      final Color outside,
      final double expectedRadius,
      final String what) {
    final double measured = topLeftRadius(image, bodyX, bodyY, span, body, outside);
    assertThat(measured)
        .as(
            "%s — corner-cut probe measured radius %.2f, expected %.1f",
            what, measured, expectedRadius)
        .isCloseTo(expectedRadius, within(1.2));
  }

  private static boolean nearerToBody(final int argb, final Color body, final Color outside) {
    final Color got = new Color(argb, true);
    return distanceSquared(got, body) <= distanceSquared(got, outside);
  }

  private static int distanceSquared(final Color a, final Color b) {
    final int dr = a.getRed() - b.getRed();
    final int dg = a.getGreen() - b.getGreen();
    final int db = a.getBlue() - b.getBlue();
    return dr * dr + dg * dg + db * db;
  }
}
