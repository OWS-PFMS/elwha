package com.owspfm.elwha.slider;

import com.owspfm.elwha.theme.ColorRole;
import com.owspfm.elwha.theme.ElwhaTheme;
import com.owspfm.elwha.theme.MaterialPalettes;
import com.owspfm.elwha.theme.Mode;
import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;

/**
 * Headless guard for Phase-2 / S1 (story #351). Asserts the {@link ElwhaSlider.Variant#CENTERED}
 * variant API (variant + default/custom origin) and proves from offscreen renders that (a) the
 * active {@link ColorRole#PRIMARY} fill emanates from the origin and grows in <em>both</em>
 * directions (rightward above the origin, leftward below it), with {@link
 * ColorRole#SECONDARY_CONTAINER} inactive track on the far outer sides, and (b) stop indicators
 * render at <em>both</em> outer ends in centered mode. No display required.
 *
 * @author Charles Bryan
 * @version v0.4.0
 * @since v0.4.0
 */
public final class ElwhaSliderCenteredSmoke {

  private static final int W = 240;
  private static final int H = ElwhaSlider.HANDLE_HEIGHT_PX;
  private static final int CY = H / 2;

  private ElwhaSliderCenteredSmoke() {}

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

    // --- API: variant + origin defaults ---
    final ElwhaSlider api = new ElwhaSlider(-50, 50, 0);
    check("default variant is STANDARD", api.getVariant() == ElwhaSlider.Variant.STANDARD);
    api.setVariant(ElwhaSlider.Variant.CENTERED);
    check("setVariant CENTERED sticks", api.getVariant() == ElwhaSlider.Variant.CENTERED);
    check("default origin is 0 when 0 is in range", api.getOrigin() == 0);

    final ElwhaSlider midOrigin = new ElwhaSlider(20, 80, 50);
    midOrigin.setVariant(ElwhaSlider.Variant.CENTERED);
    check("default origin is the midpoint when 0 is out of range", midOrigin.getOrigin() == 50);
    midOrigin.setOrigin(30);
    check("setOrigin overrides the default", midOrigin.getOrigin() == 30);

    boolean threw = false;
    try {
      api.setVariant(null);
    } catch (final IllegalArgumentException ex) {
      threw = true;
    }
    check("setVariant(null) is rejected", threw);

    // --- Fill grows rightward when value is above the origin (origin 0, value +30) ---
    final BufferedImage above = render(centered(-50, 50, 30));
    check(
        "above origin: active PRIMARY between origin and handle (value +15)",
        isPrimary(above, valueX(above.getWidth(), -50, 50, 15)));
    check(
        "above origin: inactive on the far (below-origin) side (value -30)",
        isSecondary(above, valueX(above.getWidth(), -50, 50, -30)));

    // --- Fill grows leftward when value is below the origin (origin 0, value -30) ---
    final BufferedImage below = render(centered(-50, 50, -30));
    check(
        "below origin: active PRIMARY between handle and origin (value -15)",
        isPrimary(below, valueX(below.getWidth(), -50, 50, -15)));
    check(
        "below origin: inactive on the far (above-origin) side (value +30)",
        isSecondary(below, valueX(below.getWidth(), -50, 50, 30)));

    // --- Stop indicators at BOTH outer ends in centered mode ---
    final ElwhaSlider stops = centered(-50, 50, 0);
    stops.setStops(25);
    final BufferedImage dots = render(stops);
    check(
        "centered stops: inactive dot at the low end (min)",
        isInactiveDot(dots, valueX(dots.getWidth(), -50, 50, -50)));
    check(
        "centered stops: inactive dot at the high end (max)",
        isInactiveDot(dots, valueX(dots.getWidth(), -50, 50, 50)));

    // --- Centered continuous mode shows end stops at both outer ends ---
    final BufferedImage cont = render(centered(-50, 50, 0));
    check(
        "centered continuous: end stop at min",
        isInactiveDot(cont, valueX(cont.getWidth(), -50, 50, -50)));
    check(
        "centered continuous: end stop at max",
        isInactiveDot(cont, valueX(cont.getWidth(), -50, 50, 50)));

    System.out.println(
        "ElwhaSliderCenteredSmoke: OK (variant/origin API + bidirectional origin fill + both-end"
            + " stops)");
  }

  private static ElwhaSlider centered(final int min, final int max, final int value) {
    final ElwhaSlider s = new ElwhaSlider(min, max, value);
    s.setVariant(ElwhaSlider.Variant.CENTERED);
    return s;
  }

  private static BufferedImage render(final ElwhaSlider slider) {
    slider.setSize(W, H);
    final BufferedImage img = new BufferedImage(W, H, BufferedImage.TYPE_INT_ARGB);
    final Graphics2D g = img.createGraphics();
    try {
      slider.paint(g);
    } finally {
      g.dispose();
    }
    return img;
  }

  /** The pixel-x of a value, matching {@link ElwhaSlider#xForValue(int)} geometry. */
  private static int valueX(final int width, final int min, final int max, final int value) {
    final float frac = (value - min) / (float) (max - min);
    final int inset = ElwhaSlider.HANDLE_WIDTH_PX / 2;
    return Math.round(inset + frac * (width - 2 * inset));
  }

  private static boolean isPrimary(final BufferedImage img, final int x) {
    return nearColor(img.getRGB(clampX(img, x), CY), ColorRole.PRIMARY.resolve());
  }

  private static boolean isSecondary(final BufferedImage img, final int x) {
    return nearColor(img.getRGB(clampX(img, x), CY), ColorRole.SECONDARY_CONTAINER.resolve());
  }

  private static boolean isInactiveDot(final BufferedImage img, final int x) {
    return nearColor(img.getRGB(clampX(img, x), CY), ColorRole.ON_SECONDARY_CONTAINER.resolve());
  }

  private static int clampX(final BufferedImage img, final int x) {
    return Math.max(0, Math.min(img.getWidth() - 1, x));
  }

  private static boolean nearColor(final int argb, final Color target) {
    final Color c = new Color(argb, true);
    if (c.getAlpha() < 160) {
      return false;
    }
    return Math.abs(c.getRed() - target.getRed()) <= 10
        && Math.abs(c.getGreen() - target.getGreen()) <= 10
        && Math.abs(c.getBlue() - target.getBlue()) <= 10;
  }

  private static void check(final String what, final boolean ok) {
    if (!ok) {
      System.err.println("FAIL: " + what);
      System.exit(1);
    }
    System.out.println("  ok: " + what);
  }
}
