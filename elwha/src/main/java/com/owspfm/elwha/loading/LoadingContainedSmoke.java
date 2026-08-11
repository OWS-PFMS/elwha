package com.owspfm.elwha.loading;

import com.owspfm.elwha.theme.ColorRole;
import com.owspfm.elwha.theme.ElwhaTheme;
import com.owspfm.elwha.theme.MaterialPalettes;
import com.owspfm.elwha.theme.Mode;
import com.owspfm.elwha.theme.MorphAnimator;
import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.io.File;
import javax.imageio.ImageIO;
import javax.swing.JComponent;

/**
 * Headless guard for the S3 contained configuration (story #516). Asserts the {@link
 * ElwhaLoadingIndicator#contained()} factory's M3 color pairing, the container preferred size, and
 * that a contained render shows the {@code primaryContainer} circle behind an {@code
 * onPrimaryContainer} active shape with the right inset. Dumps a standard-vs-contained PNG. Runs
 * headless.
 *
 * @author Charles Bryan
 * @version v0.5.0
 * @since v0.5.0
 */
public final class LoadingContainedSmoke {

  private LoadingContainedSmoke() {}

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

    final ElwhaLoadingIndicator c = ElwhaLoadingIndicator.contained();
    check("contained() is contained", c.isContained());
    check(
        "contained() active = onPrimaryContainer",
        c.getIndicatorColorRole() == ColorRole.ON_PRIMARY_CONTAINER);
    check(
        "contained() container = primaryContainer",
        c.getContainerColorRole() == ColorRole.PRIMARY_CONTAINER);
    check(
        "contained preferred size = container size",
        c.getPreferredSize().width == ElwhaLoadingIndicator.CONTAINER_SIZE_DEFAULT_PX);

    final ElwhaLoadingIndicator std = new ElwhaLoadingIndicator();
    check(
        "standard preferred size = indicator size",
        std.getPreferredSize().width == ElwhaLoadingIndicator.INDICATOR_SIZE_DEFAULT_PX);

    // Freeze on the settled shape so the contained render is deterministic.
    MorphAnimator.setReducedMotion(true);
    final BufferedImage img = paint(c);
    final Color container = ColorRole.PRIMARY_CONTAINER.resolve();
    final Color active = ColorRole.ON_PRIMARY_CONTAINER.resolve();
    final int cx = img.getWidth() / 2;
    final int cy = img.getHeight() / 2;

    check("center = active (onPrimaryContainer)", isColor(img.getRGB(cx, cy), active));
    // r≈21: outside the ~18px active shape, inside the 24px container → container fill.
    check("ring r=21 above center = container", isColor(img.getRGB(cx, cy - 21), container));
    check("ring r=21 right of center = container", isColor(img.getRGB(cx + 21, cy), container));
    // Corner is outside the container circle → transparent.
    check("corner transparent", ((img.getRGB(1, 1) >>> 24) & 0xFF) < 40);

    // Standard paints no container.
    final BufferedImage stdImg = paint(std);
    check("standard corner transparent", ((stdImg.getRGB(1, 1) >>> 24) & 0xFF) < 40);
    MorphAnimator.setReducedMotion(false);

    writeCompare(std, c);
    System.out.println("LoadingContainedSmoke: PASS");
  }

  private static void writeCompare(final ElwhaLoadingIndicator std, final ElwhaLoadingIndicator c)
      throws Exception {
    MorphAnimator.setReducedMotion(true);
    final BufferedImage a = paint(std);
    final BufferedImage b = paint(c);
    final int pad = 16;
    final BufferedImage out =
        new BufferedImage(a.getWidth() + b.getWidth() + pad * 3, 80, BufferedImage.TYPE_INT_ARGB);
    final Graphics2D g = out.createGraphics();
    g.setColor(Color.WHITE);
    g.fillRect(0, 0, out.getWidth(), out.getHeight());
    g.drawImage(a, pad, (80 - a.getHeight()) / 2, null);
    g.drawImage(b, pad * 2 + a.getWidth(), (80 - b.getHeight()) / 2, null);
    g.dispose();
    MorphAnimator.setReducedMotion(false);
    final File f = new File("target/loading-contained-compare.png");
    f.getParentFile().mkdirs();
    ImageIO.write(out, "png", f);
    System.out.println("wrote " + f.getAbsolutePath());
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

  private static boolean isColor(final int rgb, final Color c) {
    if (((rgb >>> 24) & 0xFF) < 40) {
      return false;
    }
    return Math.abs(((rgb >> 16) & 0xFF) - c.getRed()) < 36
        && Math.abs(((rgb >> 8) & 0xFF) - c.getGreen()) < 36
        && Math.abs((rgb & 0xFF) - c.getBlue()) < 36;
  }

  private static void check(final String label, final boolean condition) {
    if (!condition) {
      System.err.println("FAIL: " + label);
      System.exit(1);
    }
    System.out.println("ok: " + label);
  }
}
