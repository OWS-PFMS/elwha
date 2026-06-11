package com.owspfm.elwha.progress;

import com.owspfm.elwha.theme.ColorRole;
import com.owspfm.elwha.theme.ElwhaTheme;
import com.owspfm.elwha.theme.MaterialPalettes;
import com.owspfm.elwha.theme.Mode;
import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;

/**
 * Headless guard for the S3 {@link ElwhaLinearProgressIndicator} wavy shape (story #471). Paints
 * wavy bars offscreen and asserts: the active span displaces above and below the centerline
 * (crest + trough) while the track stays flat, the determinate amplitude ramp flattens at ≤10%
 * and ≥95% progress, the wave band grows the preferred height (10px / 14px thick), the
 * indeterminate wave renders, and the wavelength/speed knobs hold their contracts. Runs in CI's
 * headless JVM.
 *
 * @author Charles Bryan
 * @version v0.4.0
 * @since v0.4.0
 */
public final class ElwhaLinearProgressWavySmoke {

  private static final int W = 240;
  private static final int H = 10;
  private static final int MID_Y = 5;

  private ElwhaLinearProgressWavySmoke() {}

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
    final Color track = ColorRole.SECONDARY_CONTAINER.resolve();

    final ElwhaLinearProgressIndicator wavy = ElwhaLinearProgressIndicator.wavy();
    wavy.setValue(60);
    check("wavy preferred height = 10", wavy.getPreferredSize().height == 10);

    final BufferedImage img = paint(wavy, H);
    check("wave crest rises above the centerline band", minPrimaryY(img, primary, 0, 144) <= 2);
    check("wave trough dips below the centerline band", maxPrimaryY(img, primary, 0, 144) >= 8);
    check(
        "track stays flat (no track pixels off the mid band)",
        colorWithinBand(img, track, 150, W, MID_Y - 3, MID_Y + 3));
    check("stop dot still renders", nearColor(img.getRGB(W - 2, MID_Y), primary));

    final ElwhaLinearProgressIndicator low = ElwhaLinearProgressIndicator.wavy();
    low.setValue(5);
    final BufferedImage lowImg = paint(low, H);
    check(
        "ramp: ≤10% paints flat",
        colorWithinBand(lowImg, primary, 0, 24, MID_Y - 2, MID_Y + 2));

    final ElwhaLinearProgressIndicator high = ElwhaLinearProgressIndicator.wavy();
    high.setValue(97);
    final BufferedImage highImg = paint(high, H);
    check(
        "ramp: ≥95% paints flat",
        colorWithinBand(highImg, primary, 0, 220, MID_Y - 2, MID_Y + 2));

    final ElwhaLinearProgressIndicator thick = ElwhaLinearProgressIndicator.wavy();
    thick.setTrackThickness(8);
    check("wavy thick preferred height = 14", thick.getPreferredSize().height == 14);

    final ElwhaLinearProgressIndicator indet = ElwhaLinearProgressIndicator.wavyIndeterminate();
    boolean indetWavy = false;
    for (int i = 0; i < 20 && !indetWavy; i++) {
      final BufferedImage indetImg = paint(indet, H);
      indetWavy =
          minPrimaryY(indetImg, primary, 0, W) <= 2 || maxPrimaryY(indetImg, primary, 0, W) >= 8;
      sleep(80);
    }
    check("indeterminate wave renders off-band within the cycle", indetWavy);

    final ElwhaLinearProgressIndicator knobs = ElwhaLinearProgressIndicator.wavy();
    check("determinate wavelength default 40", knobs.getWavelengthDeterminate() == 40);
    check("indeterminate wavelength default 20", knobs.getWavelengthIndeterminate() == 20);
    check("speed auto = one determinate wavelength/s", knobs.getWaveSpeed() == 40f);
    knobs.setIndeterminate(true);
    check("speed auto follows mode wavelength", knobs.getWaveSpeed() == 20f);
    knobs.setIndeterminate(false);
    knobs.setWavelength(30);
    check(
        "setWavelength fans out to both modes",
        knobs.getWavelengthDeterminate() == 30 && knobs.getWavelengthIndeterminate() == 30);
    knobs.setWaveSpeed(0f);
    check("explicit zero speed freezes", knobs.getWaveSpeed() == 0f);
    knobs.setWaveSpeed(-1f);
    check("negative restores auto speed", knobs.getWaveSpeed() == 30f);

    System.out.println("ElwhaLinearProgressWavySmoke: OK (wave + ramp + flat track + knobs)");
  }

  private static void sleep(final long ms) {
    try {
      Thread.sleep(ms);
    } catch (final InterruptedException e) {
      Thread.currentThread().interrupt();
    }
  }

  private static int minPrimaryY(
      final BufferedImage img, final Color primary, final int x0, final int x1) {
    int min = Integer.MAX_VALUE;
    for (int x = x0; x < x1; x++) {
      for (int y = 0; y < H; y++) {
        if (nearColor(img.getRGB(x, y), primary)) {
          min = Math.min(min, y);
          break;
        }
      }
    }
    return min;
  }

  private static int maxPrimaryY(
      final BufferedImage img, final Color primary, final int x0, final int x1) {
    int max = -1;
    for (int x = x0; x < x1; x++) {
      for (int y = H - 1; y >= 0; y--) {
        if (nearColor(img.getRGB(x, y), primary)) {
          max = Math.max(max, y);
          break;
        }
      }
    }
    return max;
  }

  private static boolean colorWithinBand(
      final BufferedImage img,
      final Color target,
      final int x0,
      final int x1,
      final int yMin,
      final int yMax) {
    boolean seen = false;
    for (int x = x0; x < Math.min(x1, W); x++) {
      for (int y = 0; y < H; y++) {
        if (nearColor(img.getRGB(x, y), target)) {
          seen = true;
          if (y < yMin || y > yMax) {
            return false;
          }
        }
      }
    }
    return seen;
  }

  private static BufferedImage paint(final ElwhaLinearProgressIndicator bar, final int height) {
    bar.setSize(W, height);
    final BufferedImage img = new BufferedImage(W, height, BufferedImage.TYPE_INT_ARGB);
    final Graphics2D g = img.createGraphics();
    try {
      bar.paint(g);
    } finally {
      g.dispose();
    }
    return img;
  }

  private static boolean nearColor(final int argb, final Color target) {
    final Color c = new Color(argb, true);
    if (c.getAlpha() < 200) {
      return false;
    }
    return Math.abs(c.getRed() - target.getRed())
            + Math.abs(c.getGreen() - target.getGreen())
            + Math.abs(c.getBlue() - target.getBlue())
        < 60;
  }

  private static void check(final String what, final boolean ok) {
    if (!ok) {
      System.err.println("FAIL: " + what);
      System.exit(1);
    }
    System.out.println("  ok: " + what);
  }
}
