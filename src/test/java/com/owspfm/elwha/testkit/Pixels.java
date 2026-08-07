package com.owspfm.elwha.testkit;

import static org.assertj.core.api.Assertions.assertThat;

import com.owspfm.elwha.theme.ColorRole;
import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import javax.swing.JComponent;

/**
 * Offscreen render + pixel-probe helpers for headless component tests — the shared form of the
 * {@code near}/{@code mix}/render idiom the {@code *Smoke} mains evolved (previously duplicated per
 * file). Components are painted unrealized onto an opaque {@link ColorRole#SURFACE} ground so
 * translucent state layers composite predictably; probes assert per-channel closeness with a
 * readable hex diff on failure.
 *
 * @author Charles Bryan
 * @version v0.5.0
 * @since v0.5.0
 */
public final class Pixels {

  /** Per-channel tolerance for {@link #assertPixelNear} — matches the smokes' proven value. */
  private static final int TOLERANCE = 10;

  private Pixels() {}

  /**
   * Sizes, lays out, and paints an unrealized component onto an opaque {@link ColorRole#SURFACE}
   * ground.
   *
   * @param component the component to paint (never realized; no peer required)
   * @param width raster width in px
   * @param height raster height in px
   * @return the painted raster
   * @version v0.5.0
   * @since v0.5.0
   */
  public static BufferedImage render(
      final JComponent component, final int width, final int height) {
    return render(component, width, height, ColorRole.SURFACE.resolve());
  }

  /**
   * Sizes, lays out, and paints an unrealized component onto an opaque ground of the given color.
   *
   * @param component the component to paint (never realized; no peer required)
   * @param width raster width in px
   * @param height raster height in px
   * @param ground the opaque ground color painted before the component
   * @return the painted raster
   * @version v0.5.0
   * @since v0.5.0
   */
  public static BufferedImage render(
      final JComponent component, final int width, final int height, final Color ground) {
    component.setSize(width, height);
    component.doLayout();
    final BufferedImage image = new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB);
    final Graphics2D g = image.createGraphics();
    try {
      g.setColor(ground);
      g.fillRect(0, 0, width, height);
      component.paint(g);
    } finally {
      g.dispose();
    }
    return image;
  }

  /**
   * Asserts the pixel at {@code (x, y)} is within ±10 per channel of {@code want}, failing with
   * both colors as hex.
   *
   * @param image the raster to probe
   * @param x probe x
   * @param y probe y
   * @param want the expected color
   * @param what human-readable description of what the probe verifies
   * @version v0.5.0
   * @since v0.5.0
   */
  public static void assertPixelNear(
      final BufferedImage image, final int x, final int y, final Color want, final String what) {
    final Color got = new Color(image.getRGB(x, y), true);
    final boolean near =
        Math.abs(got.getRed() - want.getRed()) <= TOLERANCE
            && Math.abs(got.getGreen() - want.getGreen()) <= TOLERANCE
            && Math.abs(got.getBlue() - want.getBlue()) <= TOLERANCE;
    assertThat(near)
        .as(
            "%s — expected %s ±%d/channel at (%d,%d) but was %s",
            what, hex(want), TOLERANCE, x, y, hex(got))
        .isTrue();
  }

  /**
   * Source-over blend of {@code over} at {@code alpha} onto an opaque {@code base} — the expected
   * result of painting an M3 state layer over a surface.
   *
   * @param base the opaque ground
   * @param over the state-layer color
   * @param alpha the state-layer opacity in {@code [0,1]}
   * @return the composited color
   * @version v0.5.0
   * @since v0.5.0
   */
  public static Color mix(final Color base, final Color over, final float alpha) {
    return new Color(
        Math.round(base.getRed() + (over.getRed() - base.getRed()) * alpha),
        Math.round(base.getGreen() + (over.getGreen() - base.getGreen()) * alpha),
        Math.round(base.getBlue() + (over.getBlue() - base.getBlue()) * alpha));
  }

  private static String hex(final Color c) {
    return String.format("#%02X%02X%02X", c.getRed(), c.getGreen(), c.getBlue());
  }
}
