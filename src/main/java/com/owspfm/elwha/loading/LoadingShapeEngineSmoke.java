package com.owspfm.elwha.loading;

import com.owspfm.elwha.theme.ColorRole;
import com.owspfm.elwha.theme.ElwhaTheme;
import com.owspfm.elwha.theme.MaterialPalettes;
import com.owspfm.elwha.theme.Mode;
import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.io.File;
import javax.imageio.ImageIO;
import javax.swing.JComponent;

/**
 * Headless guard for the S1 (spike) shape-morph engine (story #514). Asserts the {@link
 * RoundedPolygonShape} radius profiles (length, normalization, per-shape variance), the {@link
 * ShapeMorph} per-angle lerp (midpoint differs from both endpoints; seamless wrap), and that the
 * {@link ElwhaLoadingIndicator} skeleton paints a non-empty, centered shape. Also dumps a contact
 * sheet PNG ({@code target/loading-shape-contact-sheet.png}) for visual tuning of the
 * reconstructions. Runs in CI's headless JVM.
 *
 * @author Charles Bryan
 * @version v0.5.0
 * @since v0.5.0
 */
public final class LoadingShapeEngineSmoke {

  private LoadingShapeEngineSmoke() {}

  /**
   * Runs the guard. Exits non-zero on any failed assertion.
   *
   * @param args unused
   * @throws Exception on image-write failure
   * @version v0.5.0
   * @since v0.5.0
   */
  public static void main(final String[] args) throws Exception {
    System.setProperty("java.awt.headless", "true");
    ElwhaTheme.install(
        ElwhaTheme.config().theme(MaterialPalettes.baseline()).mode(Mode.LIGHT).build());

    final RoundedPolygonShape[] all = {
      LoadingShapes.CIRCLE,
      LoadingShapes.SUNNY,
      LoadingShapes.COOKIE_9,
      LoadingShapes.COOKIE_4,
      LoadingShapes.PENTAGON,
      LoadingShapes.PILL,
      LoadingShapes.OVAL,
      LoadingShapes.SOFT_BURST
    };
    final String[] names = {
      "Circle", "Sunny", "Cookie9", "Cookie4", "Pentagon", "Pill", "Oval", "SoftBurst"
    };

    for (int i = 0; i < all.length; i++) {
      final float[] p = all[i].radii();
      check(
          names[i] + ": profile length " + RoundedPolygonShape.SAMPLE_COUNT,
          p.length == RoundedPolygonShape.SAMPLE_COUNT);
      float min = 2f;
      float max = 0f;
      for (final float v : p) {
        check(names[i] + ": radius in [0,1]", v >= -0.001f && v <= 1.001f);
        min = Math.min(min, v);
        max = Math.max(max, v);
      }
      check(names[i] + ": normalized (max==1)", Math.abs(max - 1f) < 0.01f);
    }

    // Circle is near-constant; the stars/cookies have real radial variance.
    check("Circle near-constant", variance(LoadingShapes.CIRCLE.radii()) < 1e-4f);
    check("Sunny has variance", spread(LoadingShapes.SUNNY.radii()) > 0.1f);
    check("Cookie4 scallops", spread(LoadingShapes.COOKIE_4.radii()) > 0.2f);
    check("SoftBurst gentle lobes", spread(LoadingShapes.SOFT_BURST.radii()) > 0.05f);

    // Morph midpoint differs from both endpoints, and seamless (endpoints connect).
    final float[] a = LoadingShapes.SOFT_BURST.radii();
    final float[] b = LoadingShapes.COOKIE_9.radii();
    final float[] mid = ShapeMorph.lerp(a, b, 0.5f);
    check("morph midpoint != A", maxAbsDiff(mid, a) > 0.02f);
    check("morph midpoint != B", maxAbsDiff(mid, b) > 0.02f);
    check("morph t=0 == A", maxAbsDiff(ShapeMorph.lerp(a, b, 0f), a) < 1e-4f);
    check("morph t=1 == B", maxAbsDiff(ShapeMorph.lerp(a, b, 1f), b) < 1e-4f);

    // Component paints a non-empty, centered shape.
    final ElwhaLoadingIndicator ind = new ElwhaLoadingIndicator();
    final BufferedImage img = paint(ind);
    final Color primary = ColorRole.PRIMARY.resolve();
    final int filled = countColor(img, primary);
    check("indicator paints non-empty", filled > 200);
    check("paint roughly centered", centeredOn(img, primary));

    writeContactSheet(all, names);
    System.out.println("LoadingShapeEngineSmoke: PASS");
  }

  private static void writeContactSheet(final RoundedPolygonShape[] all, final String[] names)
      throws Exception {
    final int cell = 90;
    final int cols = all.length;
    final int rows = 2; // row 0: each shape; row 1: SoftBurst->Cookie9 morph filmstrip
    final BufferedImage sheet =
        new BufferedImage(cols * cell, rows * cell, BufferedImage.TYPE_INT_ARGB);
    final Graphics2D g = sheet.createGraphics();
    g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
    g.setColor(Color.WHITE);
    g.fillRect(0, 0, sheet.getWidth(), sheet.getHeight());
    g.setColor(ColorRole.PRIMARY.resolve());
    for (int i = 0; i < all.length; i++) {
      final float cx = i * cell + cell / 2f;
      final float cy = cell / 2f;
      g.fill(ShapeMorph.toPath(all[i].radii(), cx, cy, cell / 2f - 8f, 0.0));
    }
    final float[] from = LoadingShapes.SOFT_BURST.radii();
    final float[] to = LoadingShapes.COOKIE_9.radii();
    for (int i = 0; i < cols; i++) {
      final float t = i / (float) (cols - 1);
      final float cx = i * cell + cell / 2f;
      final float cy = cell + cell / 2f;
      g.fill(ShapeMorph.toPath(ShapeMorph.lerp(from, to, t), cx, cy, cell / 2f - 8f, 0.0));
    }
    g.dispose();
    final File out = new File("target/loading-shape-contact-sheet.png");
    out.getParentFile().mkdirs();
    ImageIO.write(sheet, "png", out);
    System.out.println("wrote " + out.getAbsolutePath());
  }

  private static BufferedImage paint(final JComponent c) {
    c.setSize(c.getPreferredSize());
    c.doLayout();
    final BufferedImage img =
        new BufferedImage(c.getWidth(), c.getHeight(), BufferedImage.TYPE_INT_ARGB);
    final Graphics2D g = img.createGraphics();
    c.paint(g);
    g.dispose();
    return img;
  }

  private static int countColor(final BufferedImage img, final Color c) {
    int n = 0;
    for (int y = 0; y < img.getHeight(); y++) {
      for (int x = 0; x < img.getWidth(); x++) {
        if (closeTo(img.getRGB(x, y), c)) {
          n++;
        }
      }
    }
    return n;
  }

  private static boolean centeredOn(final BufferedImage img, final Color c) {
    long sx = 0;
    long sy = 0;
    long n = 0;
    for (int y = 0; y < img.getHeight(); y++) {
      for (int x = 0; x < img.getWidth(); x++) {
        if (closeTo(img.getRGB(x, y), c)) {
          sx += x;
          sy += y;
          n++;
        }
      }
    }
    if (n == 0) {
      return false;
    }
    final double mx = sx / (double) n;
    final double my = sy / (double) n;
    return Math.abs(mx - img.getWidth() / 2.0) < 3.0 && Math.abs(my - img.getHeight() / 2.0) < 3.0;
  }

  private static boolean closeTo(final int rgb, final Color c) {
    final int a = (rgb >>> 24) & 0xFF;
    if (a < 40) {
      return false;
    }
    final int r = (rgb >> 16) & 0xFF;
    final int g = (rgb >> 8) & 0xFF;
    final int b = rgb & 0xFF;
    return Math.abs(r - c.getRed()) < 40
        && Math.abs(g - c.getGreen()) < 40
        && Math.abs(b - c.getBlue()) < 40;
  }

  private static float spread(final float[] p) {
    float min = 2f;
    float max = 0f;
    for (final float v : p) {
      min = Math.min(min, v);
      max = Math.max(max, v);
    }
    return max - min;
  }

  private static float variance(final float[] p) {
    float mean = 0f;
    for (final float v : p) {
      mean += v;
    }
    mean /= p.length;
    float var = 0f;
    for (final float v : p) {
      var += (v - mean) * (v - mean);
    }
    return var / p.length;
  }

  private static float maxAbsDiff(final float[] a, final float[] b) {
    float d = 0f;
    for (int i = 0; i < a.length; i++) {
      d = Math.max(d, Math.abs(a[i] - b[i]));
    }
    return d;
  }

  private static void check(final String label, final boolean condition) {
    if (!condition) {
      System.err.println("FAIL: " + label);
      System.exit(1);
    }
    System.out.println("ok: " + label);
  }
}
