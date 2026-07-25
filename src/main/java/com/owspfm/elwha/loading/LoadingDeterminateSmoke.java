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
import javax.swing.DefaultBoundedRangeModel;
import javax.swing.JComponent;

/**
 * Headless guard for the S4 determinate mode (story #517). Asserts the {@code Circle → SoftBurst}
 * progress morph, the −180° progress-mapped rotation, the {@link
 * javax.swing.BoundedRangeModel}-backed value API (including model sharing), and that the render
 * tracks progress. Dumps a determinate filmstrip PNG. Runs headless.
 *
 * @author Charles Bryan
 * @version v0.5.0
 * @since v0.5.0
 */
public final class LoadingDeterminateSmoke {

  private LoadingDeterminateSmoke() {}

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

    check("determinate() is determinate", !ElwhaLoadingIndicator.determinate().isIndeterminate());
    check(
        "containedDeterminate() is contained + determinate",
        ElwhaLoadingIndicator.containedDeterminate().isContained()
            && !ElwhaLoadingIndicator.containedDeterminate().isIndeterminate());

    // Sequence endpoints.
    check(
        "progress 0 == Circle",
        maxAbsDiff(ElwhaLoadingIndicator.determinateProfile(0f), LoadingShapes.CIRCLE.radii())
            < 1e-4f);
    check(
        "progress 1 == SoftBurst",
        maxAbsDiff(ElwhaLoadingIndicator.determinateProfile(1f), LoadingShapes.SOFT_BURST.radii())
            < 1e-4f);
    check(
        "progress 0.5 between",
        maxAbsDiff(ElwhaLoadingIndicator.determinateProfile(0.5f), LoadingShapes.CIRCLE.radii())
            > 0.02f);

    // Rotation = -progress*180.
    check("rotation 0% == 0", Math.abs(ElwhaLoadingIndicator.determinateRotationRad(0f)) < 1e-6);
    check(
        "rotation 100% == -180deg",
        Math.abs(ElwhaLoadingIndicator.determinateRotationRad(1f) + Math.PI) < 1e-6);
    check(
        "rotation 50% == -90deg",
        Math.abs(ElwhaLoadingIndicator.determinateRotationRad(0.5f) + Math.PI / 2) < 1e-6);

    // Value API + extent-aware fraction.
    final ElwhaLoadingIndicator d = ElwhaLoadingIndicator.determinate();
    d.setValue(50);
    check("fraction at 50/100 == 0.5", Math.abs(d.getProgressFraction() - 0.5f) < 1e-4f);
    d.setValue(0);
    check("fraction at 0 == 0", d.getProgressFraction() == 0f);
    d.setValue(100);
    check("fraction at 100 == 1", Math.abs(d.getProgressFraction() - 1f) < 1e-4f);

    // Model sharing (the slider/progress precedent).
    final DefaultBoundedRangeModel shared = new DefaultBoundedRangeModel(0, 0, 0, 100);
    final ElwhaLoadingIndicator a = ElwhaLoadingIndicator.determinate(shared);
    shared.setValue(30);
    check("shared model reflected", a.getValue() == 30);
    a.setValue(70);
    check("setValue writes through to shared model", shared.getValue() == 70);

    // Render tracks progress.
    final ElwhaLoadingIndicator r = ElwhaLoadingIndicator.determinate();
    r.setIndicatorSize(48);
    r.setValue(0);
    final BufferedImage at0 = paint(r);
    r.setValue(100);
    final BufferedImage at100 = paint(r);
    check("determinate paints non-empty", countNonEmpty(at0) > 200);
    check("determinate render changes with progress", pixelsDiffer(at0, at100));

    writeFilmstrip();
    System.out.println("LoadingDeterminateSmoke: PASS");
  }

  private static void writeFilmstrip() throws Exception {
    final int cell = 80;
    final int frames = 6;
    final BufferedImage strip = new BufferedImage(cell * frames, cell, BufferedImage.TYPE_INT_ARGB);
    final Graphics2D g = strip.createGraphics();
    g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
    g.setColor(Color.WHITE);
    g.fillRect(0, 0, strip.getWidth(), strip.getHeight());
    g.setColor(ColorRole.PRIMARY.resolve());
    for (int i = 0; i < frames; i++) {
      final float f = i / (float) (frames - 1);
      g.fill(
          ShapeMorph.toPath(
              ElwhaLoadingIndicator.determinateProfile(f),
              i * cell + cell / 2f,
              cell / 2f,
              cell / 2f - 8f,
              ElwhaLoadingIndicator.determinateRotationRad(f)));
    }
    g.dispose();
    final File out = new File("target/loading-determinate-filmstrip.png");
    out.getParentFile().mkdirs();
    ImageIO.write(strip, "png", out);
    System.out.println("wrote " + out.getAbsolutePath());
  }

  private static BufferedImage paint(final JComponent comp) {
    comp.setSize(comp.getPreferredSize());
    comp.doLayout();
    final BufferedImage img =
        new BufferedImage(comp.getWidth(), comp.getHeight(), BufferedImage.TYPE_INT_ARGB);
    final Graphics2D g = img.createGraphics();
    comp.paint(g);
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
