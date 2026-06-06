package com.owspfm.elwha.slider;

import com.owspfm.elwha.theme.ColorRole;
import com.owspfm.elwha.theme.ElwhaTheme;
import com.owspfm.elwha.theme.MaterialPalettes;
import com.owspfm.elwha.theme.Mode;
import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;

/**
 * Headless guard for Phase-3 / S1 (story #359). Asserts the {@link ElwhaSlider.Variant#RANGE}
 * two-value API ({@code range(...)} factory, lower/upper clamping to {@code [min, max]} and to each
 * other) and proves from offscreen renders that (a) the active {@link ColorRole#PRIMARY} fill spans
 * <em>only between</em> the two handles, with {@link ColorRole#SECONDARY_CONTAINER} inactive track
 * on both outer sides, (b) <em>both</em> pill handles render (a PRIMARY column high above the track,
 * where the centered active track does not reach), and (c) stop dots are active only between the
 * handles. No display required.
 *
 * @author Charles Bryan
 * @version v0.4.0
 * @since v0.4.0
 */
public final class ElwhaSliderRangeChromeSmoke {

  private static final int W = 240;
  private static final int H = ElwhaSlider.HANDLE_HEIGHT_PX;
  private static final int CY = H / 2;
  private static final int HANDLE_TOP_Y = 4;

  private ElwhaSliderRangeChromeSmoke() {}

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

    // --- API: factory + clamping ---
    final ElwhaSlider api = ElwhaSlider.range(0, 100, 30, 70);
    check("range() sets RANGE variant", api.getVariant() == ElwhaSlider.Variant.RANGE);
    check("range() lower value", api.getLowerValue() == 30);
    check("range() upper value", api.getUpperValue() == 70);

    api.setLowerValue(90);
    check("lower clamps to upper (no cross)", api.getLowerValue() == 70);

    final ElwhaSlider api2 = ElwhaSlider.range(0, 100, 30, 70);
    api2.setUpperValue(10);
    check("upper clamps to lower (no cross)", api2.getUpperValue() == 30);

    final ElwhaSlider api3 = ElwhaSlider.range(0, 100, 30, 70);
    api3.setLowerValue(-5);
    check("lower clamps to minimum", api3.getLowerValue() == 0);
    api3.setUpperValue(500);
    check("upper clamps to maximum", api3.getUpperValue() == 100);

    // --- Chrome: active fill only between the handles ---
    final BufferedImage img = render(ElwhaSlider.range(0, 100, 30, 70));
    check(
        "active PRIMARY between the handles (value 50)",
        isPrimary(img, valueX(W, 0, 100, 50), CY));
    check(
        "inactive on the outer-left side (value 15)",
        isSecondary(img, valueX(W, 0, 100, 15), CY));
    check(
        "inactive on the outer-right side (value 85)",
        isSecondary(img, valueX(W, 0, 100, 85), CY));

    // --- Both handles render: PRIMARY column above the centered track band ---
    check(
        "lower handle renders (PRIMARY high above track at value 30)",
        isPrimary(img, valueX(W, 0, 100, 30), HANDLE_TOP_Y));
    check(
        "upper handle renders (PRIMARY high above track at value 70)",
        isPrimary(img, valueX(W, 0, 100, 70), HANDLE_TOP_Y));
    check(
        "no handle between (transparent high above track at value 50)",
        isTransparent(img, valueX(W, 0, 100, 50), HANDLE_TOP_Y));

    // --- Stops: active only between the handles ---
    final ElwhaSlider stopsSlider = ElwhaSlider.range(0, 100, 20, 80);
    stopsSlider.setStops(10);
    final BufferedImage dots = render(stopsSlider);
    check(
        "interior stop active between the handles (value 50)",
        isActiveDot(dots, valueX(W, 0, 100, 50)));
    check(
        "stop outside the span is inactive (value 10)",
        isInactiveDot(dots, valueX(W, 0, 100, 10)));

    System.out.println(
        "ElwhaSliderRangeChromeSmoke: OK (range API + between-handle fill + two handles + stops)");
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

  private static boolean isPrimary(final BufferedImage img, final int x, final int y) {
    return nearColor(img.getRGB(clampX(img, x), y), ColorRole.PRIMARY.resolve());
  }

  private static boolean isSecondary(final BufferedImage img, final int x, final int y) {
    return nearColor(img.getRGB(clampX(img, x), y), ColorRole.SECONDARY_CONTAINER.resolve());
  }

  private static boolean isActiveDot(final BufferedImage img, final int x) {
    return nearColor(img.getRGB(clampX(img, x), CY), ColorRole.ON_PRIMARY.resolve());
  }

  private static boolean isInactiveDot(final BufferedImage img, final int x) {
    return nearColor(img.getRGB(clampX(img, x), CY), ColorRole.ON_SECONDARY_CONTAINER.resolve());
  }

  private static boolean isTransparent(final BufferedImage img, final int x, final int y) {
    return new Color(img.getRGB(clampX(img, x), y), true).getAlpha() < 16;
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
