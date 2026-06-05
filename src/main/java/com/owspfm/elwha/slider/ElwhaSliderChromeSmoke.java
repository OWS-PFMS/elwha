package com.owspfm.elwha.slider;

import com.owspfm.elwha.theme.ColorRole;
import com.owspfm.elwha.theme.ElwhaTheme;
import com.owspfm.elwha.theme.MaterialPalettes;
import com.owspfm.elwha.theme.Mode;
import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;

/**
 * Headless render guard for the S1 {@link ElwhaSlider} chrome skeleton (story #342). Paints a
 * slider at a mid value into an offscreen image and asserts the active ({@link ColorRole#PRIMARY})
 * and inactive ({@link ColorRole#SECONDARY_CONTAINER}) track segments both appear on the correct
 * sides of the handle — proving the split-track geometry + token theming render with no display.
 * Runs in CI's headless JVM ({@code -Djava.awt.headless=true} safe).
 *
 * @author Charles Bryan
 * @version v0.4.0
 * @since v0.4.0
 */
public final class ElwhaSliderChromeSmoke {

  private ElwhaSliderChromeSmoke() {}

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

    final int w = 240;
    final int h = ElwhaSlider.HANDLE_HEIGHT_PX;
    final ElwhaSlider slider = new ElwhaSlider(0, 100, 60);
    slider.setSize(w, h);

    final BufferedImage img = new BufferedImage(w, h, BufferedImage.TYPE_INT_ARGB);
    final Graphics2D g = img.createGraphics();
    try {
      slider.paint(g);
    } finally {
      g.dispose();
    }

    final Color primary = ColorRole.PRIMARY.resolve();
    final Color inactive = ColorRole.SECONDARY_CONTAINER.resolve();
    final int trackY = h / 2;

    // Sample a column near the leading edge (active side) and near the trailing edge (inactive).
    final boolean activeOnLeft = nearColor(img.getRGB(8, trackY), primary);
    final boolean inactiveOnRight = nearColor(img.getRGB(w - 8, trackY), inactive);
    final int handleX = slider.handleCenterX();
    final boolean handleIsPrimary = nearColor(img.getRGB(handleX, trackY), primary);

    check("active (PRIMARY) track on the leading side", activeOnLeft);
    check("inactive (SECONDARY_CONTAINER) track on the trailing side", inactiveOnRight);
    check("handle paints in PRIMARY", handleIsPrimary);

    System.out.println("ElwhaSliderChromeSmoke: OK (active+inactive split + handle rendered)");
  }

  private static boolean nearColor(final int argb, final Color target) {
    final Color c = new Color(argb, true);
    if (c.getAlpha() < 200) {
      return false;
    }
    return Math.abs(c.getRed() - target.getRed()) <= 6
        && Math.abs(c.getGreen() - target.getGreen()) <= 6
        && Math.abs(c.getBlue() - target.getBlue()) <= 6;
  }

  private static void check(final String what, final boolean ok) {
    if (!ok) {
      System.err.println("FAIL: " + what);
      System.exit(1);
    }
    System.out.println("  ok: " + what);
  }
}
