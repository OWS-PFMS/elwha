package com.owspfm.elwha.loading;

import com.owspfm.elwha.theme.ColorRole;
import com.owspfm.elwha.theme.ElwhaTheme;
import com.owspfm.elwha.theme.MaterialPalettes;
import com.owspfm.elwha.theme.Mode;
import com.owspfm.elwha.theme.MorphAnimator;
import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.io.File;
import javax.imageio.ImageIO;
import javax.swing.JComponent;

/**
 * Headless guard for the S2 indeterminate choreography + clock lifecycle (story #515). Asserts the
 * deterministic timeline (shape advances one step per {@code STEP_MS}; rotation strictly
 * increases), that the live component animates (two paints across a sleep differ), and that
 * {@linkplain MorphAnimator#isReducedMotion() reduced motion} freezes it. Dumps an indeterminate
 * filmstrip PNG ({@code target/loading-indeterminate-filmstrip.png}) for visual confirmation. Runs
 * headless.
 *
 * @author Charles Bryan
 * @version v0.5.0
 * @since v0.5.0
 */
public final class LoadingIndeterminateSmoke {

  private LoadingIndeterminateSmoke() {}

  /**
   * Runs the guard. Exits non-zero on any failed assertion.
   *
   * @param args unused
   * @throws Exception on image-write or sleep failure
   * @version v0.5.0
   * @since v0.5.0
   */
  public static void main(final String[] args) throws Exception {
    System.setProperty("java.awt.headless", "true");
    ElwhaTheme.install(
        ElwhaTheme.config().theme(MaterialPalettes.baseline()).mode(Mode.LIGHT).build());
    MorphAnimator.setReducedMotion(false);

    final int n = LoadingShapes.INDETERMINATE.length;

    // Step boundaries land exactly on each sequence shape, in order.
    for (int k = 0; k < n + 2; k++) {
      final float[] atStep =
          ElwhaLoadingIndicator.indeterminateProfile((long) k * ElwhaLoadingIndicator.STEP_MS);
      final float[] expected = LoadingShapes.INDETERMINATE[k % n].radii();
      check("step " + k + " == sequence[" + (k % n) + "]", maxAbsDiff(atStep, expected) < 1e-4f);
    }

    // Mid-step differs from both endpoints (a real morph is in flight).
    final long midElapsed = ElwhaLoadingIndicator.STEP_MS / 3;
    final float[] mid = ElwhaLoadingIndicator.indeterminateProfile(midElapsed);
    check("mid-step != shape0", maxAbsDiff(mid, LoadingShapes.INDETERMINATE[0].radii()) > 0.01f);
    check("mid-step != shape1", maxAbsDiff(mid, LoadingShapes.INDETERMINATE[1].radii()) > 0.01f);

    // Rotation strictly increases over the timeline.
    double prev = -1e9;
    boolean monotonic = true;
    for (int ms = 0; ms <= 5000; ms += 100) {
      final double rot = ElwhaLoadingIndicator.indeterminateRotationRad(ms);
      if (rot <= prev) {
        monotonic = false;
      }
      prev = rot;
    }
    check("rotation strictly increasing", monotonic);
    check(
        "rotation advances ~one full turn over a 4666ms+ window",
        ElwhaLoadingIndicator.indeterminateRotationRad(ElwhaLoadingIndicator.GLOBAL_ROTATION_MS)
            > Math.toRadians(360.0));

    // The live component paints motion (phase derives from the wall clock).
    final ElwhaLoadingIndicator ind = new ElwhaLoadingIndicator();
    ind.setIndicatorSize(48);
    final BufferedImage a = paint(ind);
    Thread.sleep(140);
    final BufferedImage b = paint(ind);
    check("indeterminate moves between paints", pixelsDiffer(a, b));
    check("indeterminate paints non-empty", countNonEmpty(a) > 200);

    // Reduced motion freezes the spinner on the settled shape.
    MorphAnimator.setReducedMotion(true);
    final ElwhaLoadingIndicator frozen = new ElwhaLoadingIndicator();
    frozen.setIndicatorSize(48);
    final BufferedImage f1 = paint(frozen);
    Thread.sleep(140);
    final BufferedImage f2 = paint(frozen);
    check("reduced-motion frozen (paints identical)", !pixelsDiffer(f1, f2));
    MorphAnimator.setReducedMotion(false);

    writeFilmstrip();
    System.out.println("LoadingIndeterminateSmoke: PASS");
  }

  private static void writeFilmstrip() throws Exception {
    final int cell = 80;
    final int frames = 9;
    final BufferedImage strip = new BufferedImage(cell * frames, cell, BufferedImage.TYPE_INT_ARGB);
    final Graphics2D g = strip.createGraphics();
    g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
    g.setColor(Color.WHITE);
    g.fillRect(0, 0, strip.getWidth(), strip.getHeight());
    g.setColor(ColorRole.PRIMARY.resolve());
    for (int i = 0; i < frames; i++) {
      final long elapsed = (long) (i * ElwhaLoadingIndicator.STEP_MS * 0.55);
      final float cx = i * cell + cell / 2f;
      g.fill(
          ShapeMorph.toPath(
              ElwhaLoadingIndicator.indeterminateProfile(elapsed),
              cx,
              cell / 2f,
              cell / 2f - 8f,
              ElwhaLoadingIndicator.indeterminateRotationRad(elapsed)));
    }
    g.dispose();
    final File out = new File("target/loading-indeterminate-filmstrip.png");
    out.getParentFile().mkdirs();
    ImageIO.write(strip, "png", out);
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

  private static boolean pixelsDiffer(final BufferedImage a, final BufferedImage b) {
    for (int y = 0; y < a.getHeight(); y++) {
      for (int x = 0; x < a.getWidth(); x++) {
        if (a.getRGB(x, y) != b.getRGB(x, y)) {
          return true;
        }
      }
    }
    return false;
  }

  private static int countNonEmpty(final BufferedImage img) {
    int n = 0;
    for (int y = 0; y < img.getHeight(); y++) {
      for (int x = 0; x < img.getWidth(); x++) {
        if (((img.getRGB(x, y) >>> 24) & 0xFF) > 40) {
          n++;
        }
      }
    }
    return n;
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
