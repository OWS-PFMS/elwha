package com.owspfm.elwha.slider;

import com.owspfm.elwha.icons.MaterialIcons;
import com.owspfm.elwha.theme.ColorRole;
import com.owspfm.elwha.theme.ElwhaTheme;
import com.owspfm.elwha.theme.MaterialPalettes;
import com.owspfm.elwha.theme.Mode;
import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;

/**
 * Headless guard for the Phase-4 / S2 {@link ElwhaSlider#setInsetIcon inset icon} (story #370).
 * Renders a standard {@code L} slider and asserts the glyph paints in the <em>active</em> track's
 * leading slot at a high value and <em>swaps into the inactive</em> track at a low value (M3
 * swap-at-zero), then asserts an {@code XS} slider paints no glyph (the documented no-op). Glyph
 * presence is detected as opaque pixels that diverge from the flat track color in the sampled box.
 * Runs in CI's headless JVM.
 *
 * @author Charles Bryan
 * @version v0.4.0
 * @since v0.4.0
 */
public final class ElwhaSliderInsetIconSmoke {

  private static final int W = 240;

  private ElwhaSliderInsetIconSmoke() {}

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

    final Color primary = ColorRole.PRIMARY.resolve();
    final Color inactive = ColorRole.SECONDARY_CONTAINER.resolve();

    // --- API round-trip ---
    final ElwhaSlider api = new ElwhaSlider(0, 100, 50);
    api.setSizeVariant(ElwhaSlider.Size.L);
    api.setInsetIcon(MaterialIcons.brightnessAuto());
    check("getInsetIcon returns the set icon", api.getInsetIcon() != null);
    api.setInsetIcon(null);
    check("null clears the inset icon", api.getInsetIcon() == null);

    // --- high value: glyph in the active (PRIMARY) leading slot, NOT on the trailing side ---
    final BufferedImage hi = renderL(80);
    final int hiH = hi.getHeight();
    final int leadGlyph = glyphPixels(hi, 12, 36, hiH / 2 - 8, hiH / 2 + 8, primary);
    final int trailAtHigh = glyphPixels(hi, 200, 224, hiH / 2 - 8, hiH / 2 + 8, inactive);
    check("inset glyph renders in the active-track leading slot at a high value", leadGlyph > 12);
    check(
        "inset glyph is leading-only at a high value (none on the trailing side)",
        trailAtHigh <= 6);

    // --- low value: glyph swaps to the inactive (SECONDARY_CONTAINER) trailing side ---
    final BufferedImage lo = renderL(4);
    final int loH = lo.getHeight();
    final int trailGlyph = glyphPixels(lo, 40, 90, loH / 2 - 8, loH / 2 + 8, inactive);
    check("inset glyph swaps into the inactive track at a low value", trailGlyph > 12);

    // --- XS: documented no-op, no glyph painted ---
    final ElwhaSlider xs = new ElwhaSlider(0, 100, 80);
    xs.setSizeVariant(ElwhaSlider.Size.XS);
    xs.setInsetIcon(MaterialIcons.brightnessAuto());
    final BufferedImage xsImg = render(xs);
    final int xsH = xsImg.getHeight();
    final int xsGlyph = glyphPixels(xsImg, 4, 28, xsH / 2 - 3, xsH / 2 + 3, primary);
    check("XS slider paints no inset glyph (no-op)", xsGlyph <= 4);

    System.out.println("ElwhaSliderInsetIconSmoke: OK (active + swap-to-inactive + XS no-op)");
  }

  private static BufferedImage renderL(final int value) {
    final ElwhaSlider slider = new ElwhaSlider(0, 100, value);
    slider.setSizeVariant(ElwhaSlider.Size.L);
    slider.setInsetIcon(MaterialIcons.brightnessAuto());
    return render(slider);
  }

  private static BufferedImage render(final ElwhaSlider slider) {
    final int h = slider.getPreferredSize().height;
    slider.setSize(W, h);
    final BufferedImage img = new BufferedImage(W, h, BufferedImage.TYPE_INT_ARGB);
    final Graphics2D g = img.createGraphics();
    try {
      slider.paint(g);
    } finally {
      g.dispose();
    }
    return img;
  }

  /** Counts opaque pixels in the box whose color diverges from the flat track {@code bg}. */
  private static int glyphPixels(
      final BufferedImage img,
      final int x0,
      final int x1,
      final int y0,
      final int y1,
      final Color bg) {
    int count = 0;
    for (int y = Math.max(0, y0); y < Math.min(img.getHeight(), y1); y++) {
      for (int x = Math.max(0, x0); x < Math.min(img.getWidth(), x1); x++) {
        final Color c = new Color(img.getRGB(x, y), true);
        if (c.getAlpha() >= 200 && !near(c, bg)) {
          count++;
        }
      }
    }
    return count;
  }

  private static boolean near(final Color c, final Color target) {
    return Math.abs(c.getRed() - target.getRed()) <= 12
        && Math.abs(c.getGreen() - target.getGreen()) <= 12
        && Math.abs(c.getBlue() - target.getBlue()) <= 12;
  }

  private static void check(final String what, final boolean ok) {
    if (!ok) {
      System.err.println("FAIL: " + what);
      System.exit(1);
    }
    System.out.println("  ok: " + what);
  }
}
