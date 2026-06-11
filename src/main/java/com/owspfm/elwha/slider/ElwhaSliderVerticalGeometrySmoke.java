package com.owspfm.elwha.slider;

import com.owspfm.elwha.theme.ColorRole;
import com.owspfm.elwha.theme.ElwhaTheme;
import com.owspfm.elwha.theme.MaterialPalettes;
import com.owspfm.elwha.theme.Mode;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;

/**
 * Headless geometry guard for the Phase-5 / S1 vertical orientation (story #385). Asserts that (a)
 * the {@linkplain ElwhaSlider#getPreferredSize() preferred-size} long/short axes <em>swap</em>
 * versus the horizontal slider, (b) a vertical standard slider fills <strong>bottom-up</strong> —
 * the active ({@link ColorRole#PRIMARY}) fill occupies the bottom of the track and the inactive
 * ({@link ColorRole#SECONDARY_CONTAINER}) container the top, with the active run growing as the
 * value rises, and (c) the centered variant transposes too (its active fill does <em>not</em> touch
 * the bottom end, since inactive track flanks both ends). Runs in CI's headless JVM.
 *
 * @author Charles Bryan
 * @version v0.4.0
 * @since v0.4.0
 */
public final class ElwhaSliderVerticalGeometrySmoke {

  private static final int LONG = 260;

  private ElwhaSliderVerticalGeometrySmoke() {}

  /**
   * Runs the guard. Exits non-zero on any failed assertion.
   *
   * @param args unused
   * @version v0.4.0
   * @since v0.4.0
   */
  public static void main(final String[] args) {
    System.setProperty("java.awt.headless", "true");
    ElwhaTheme.install(
        ElwhaTheme.config().theme(MaterialPalettes.baseline()).mode(Mode.LIGHT).build());

    // (a) preferred-size axes swap versus horizontal.
    final ElwhaSlider horizontal = new ElwhaSlider(0, 100, 70);
    final Dimension hp = horizontal.getPreferredSize();
    final ElwhaSlider vert = new ElwhaSlider(0, 100, 70);
    vert.setOrientation(ElwhaSlider.Orientation.VERTICAL);
    final Dimension vp = vert.getPreferredSize();
    check("vertical preferred width == horizontal preferred height", vp.width == hp.height);
    check("vertical preferred height == horizontal preferred length", vp.height == hp.width);

    // (b) bottom-up fill: at value 70, the bottom track end is active, the top end inactive. Sample
    // a column offset from center so the end-stop dots (on the track center line) don't shadow it.
    final BufferedImage img70 = paintVertical(ElwhaSlider.Variant.STANDARD, 0, 70);
    final int col = img70.getWidth() / 2 + 5;
    check("top of vertical track is inactive (SECONDARY_CONTAINER)", isInactive(img70, col, 16));
    check("bottom of vertical track is active (PRIMARY)", isActive(img70, col, LONG - 8));

    // active fill grows from the bottom as the value rises.
    final int active20 = activeCount(paintVertical(ElwhaSlider.Variant.STANDARD, 0, 20), col);
    final int active80 = activeCount(paintVertical(ElwhaSlider.Variant.STANDARD, 0, 80), col);
    check("active fill is taller at value 80 than at value 20", active80 > active20);
    check("active fill is non-trivial at value 80", active80 > LONG / 3);

    // (c) centered vertical transposes: active fill flanked by inactive at the bottom end.
    final BufferedImage centered = paintVerticalCentered(50, 75);
    final int ccol = centered.getWidth() / 2 + 5;
    check(
        "centered vertical bottom end is inactive (fill starts from the origin, not the bottom)",
        isInactive(centered, ccol, LONG - 8));
    check(
        "centered vertical has active fill somewhere in the column",
        activeCount(centered, ccol) > 0);

    System.out.println(
        "ElwhaSliderVerticalGeometrySmoke: OK (axis swap + bottom-up fill + centered transpose)");
  }

  private static BufferedImage paintVertical(
      final ElwhaSlider.Variant variant, final int min, final int value) {
    final ElwhaSlider s = new ElwhaSlider(min, 100, value);
    s.setVariant(variant);
    s.setOrientation(ElwhaSlider.Orientation.VERTICAL);
    return paint(s);
  }

  private static BufferedImage paintVerticalCentered(final int origin, final int value) {
    final ElwhaSlider s = new ElwhaSlider(0, 100, value);
    s.setVariant(ElwhaSlider.Variant.CENTERED);
    s.setOrigin(origin);
    s.setOrientation(ElwhaSlider.Orientation.VERTICAL);
    return paint(s);
  }

  private static BufferedImage paint(final ElwhaSlider s) {
    final int w = s.getPreferredSize().width;
    s.setSize(w, LONG);
    final BufferedImage img = new BufferedImage(w, LONG, BufferedImage.TYPE_INT_ARGB);
    final Graphics2D g = img.createGraphics();
    try {
      s.paint(g);
    } finally {
      g.dispose();
    }
    return img;
  }

  private static int activeCount(final BufferedImage img, final int x) {
    final Color primary = ColorRole.PRIMARY.resolve();
    int count = 0;
    for (int y = 0; y < img.getHeight(); y++) {
      if (nearColor(img.getRGB(x, y), primary)) {
        count++;
      }
    }
    return count;
  }

  private static boolean isActive(final BufferedImage img, final int x, final int y) {
    return nearColor(img.getRGB(x, y), ColorRole.PRIMARY.resolve());
  }

  private static boolean isInactive(final BufferedImage img, final int x, final int y) {
    return nearColor(img.getRGB(x, y), ColorRole.SECONDARY_CONTAINER.resolve());
  }

  private static boolean nearColor(final int argb, final Color target) {
    final Color c = new Color(argb, true);
    if (c.getAlpha() < 200) {
      return false;
    }
    return Math.abs(c.getRed() - target.getRed()) <= 8
        && Math.abs(c.getGreen() - target.getGreen()) <= 8
        && Math.abs(c.getBlue() - target.getBlue()) <= 8;
  }

  private static void check(final String what, final boolean ok) {
    if (!ok) {
      System.err.println("FAIL: " + what);
      System.exit(1);
    }
    System.out.println("  ok: " + what);
  }
}
