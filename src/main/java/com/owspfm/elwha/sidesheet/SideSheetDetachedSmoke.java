package com.owspfm.elwha.sidesheet;

import com.owspfm.elwha.theme.ColorRole;
import com.owspfm.elwha.theme.ElwhaTheme;
import com.owspfm.elwha.theme.MaterialPalettes;
import com.owspfm.elwha.theme.Mode;
import com.owspfm.elwha.theme.MorphAnimator;
import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.Insets;
import java.awt.image.BufferedImage;

/**
 * S1 headless guard for the detached {@link SheetPosture} (#508): the margin insets, the footprint
 * width, the collapse-to-zero closed width, and pixel-asserted detached chrome — a floating body
 * inset by the margin, all four corners rounded (the window-edge corners round too, unlike a docked
 * modal), and the edge divider suppressed.
 *
 * @author Charles Bryan (cfb3@uw.edu)
 * @version v0.5.0
 * @since v0.5.0
 */
public final class SideSheetDetachedSmoke {

  private static final int M = ElwhaSideSheet.DETACHED_MARGIN_PX;
  private static final int BODY = ElwhaSideSheet.SHEET_WIDTH_PX;
  private static final int W = BODY + 2 * M;
  private static final int H = 400;

  private SideSheetDetachedSmoke() {}

  /**
   * Runs the guard; exits non-zero on the first failed check.
   *
   * @param args unused
   * @version v0.5.0
   * @since v0.5.0
   */
  public static void main(final String[] args) {
    System.setProperty("java.awt.headless", "true");
    ElwhaTheme.install(
        ElwhaTheme.config().theme(MaterialPalettes.baseline()).mode(Mode.LIGHT).build());

    checkGeometry();
    checkDetachedPixels();
    checkCollapse();

    System.out.println(
        "SideSheetDetachedSmoke: OK (margin insets, footprint width, collapse-to-0, detached"
            + " all-corner rounding + divider suppression)");
  }

  private static void checkGeometry() {
    final ElwhaSideSheet sheet = ElwhaSideSheet.standardSheet("Detached");
    check("default posture DOCKED", sheet.getSheetPosture() == SheetPosture.DOCKED);

    final Insets docked = sheet.getInsets();
    check(
        "docked posture has zero margin insets",
        docked.top == 0 && docked.left == 0 && docked.bottom == 0 && docked.right == 0);
    check("docked footprint == sheet width", sheet.modalFootprintWidth() == BODY);

    sheet.setSheetPosture(SheetPosture.DETACHED);
    check("posture setter", sheet.getSheetPosture() == SheetPosture.DETACHED);
    final Insets detached = sheet.getInsets();
    check(
        "detached posture insets a margin on all four sides",
        detached.top == M && detached.left == M && detached.bottom == M && detached.right == M);
    check(
        "detached footprint == sheet width + 2*margin",
        sheet.modalFootprintWidth() == BODY + 2 * M);
    check(
        "detached open preferred width == footprint",
        sheet.getPreferredSize().width == BODY + 2 * M);

    sheet.setSheetPosture(SheetPosture.DOCKED);
    check(
        "posture reverts cleanly to zero insets",
        sheet.getInsets().left == 0 && sheet.getPreferredSize().width == BODY);
  }

  private static void checkDetachedPixels() {
    final ElwhaSideSheet standard = ElwhaSideSheet.standardSheet("Detached");
    standard.setSheetPosture(SheetPosture.DETACHED);
    final BufferedImage img = render(standard);
    final Color surface = ColorRole.SURFACE.resolve();

    check("detached body fill is SURFACE", rgbEquals(img, W / 2, H / 2, surface));
    check("margin gutter is transparent (left)", alpha(img, M / 2, H / 2) == 0);
    check("margin gutter is transparent (top)", alpha(img, W / 2, M / 2) == 0);

    // All four corners of the floating body round — including the window-edge corners, which a
    // docked modal leaves square. The exact body-corner pixel falls outside the 16px arc.
    check("body top-left corner rounded", alpha(img, M, M) < 255);
    check("body top-right (window-edge) corner rounded", alpha(img, W - M - 1, M) < 255);
    check("body bottom-left corner rounded", alpha(img, M, H - M - 1) < 255);
    check("body bottom-right (window-edge) corner rounded", alpha(img, W - M - 1, H - M - 1) < 255);

    // The content-facing edge carries no divider in the detached posture (the rounded float reads
    // as a panel without one): a few px inside the body's left edge is plain SURFACE.
    check(
        "no edge divider on the detached content-facing edge",
        rgbEquals(img, M + 4, H / 2, surface));

    final ElwhaSideSheet modal = ElwhaSideSheet.modalSheet("Detached modal");
    modal.setSheetPosture(SheetPosture.DETACHED);
    final BufferedImage modalImg = render(modal);
    check(
        "detached modal fill is SURFACE_CONTAINER_LOW",
        rgbEquals(modalImg, W / 2, H / 2, ColorRole.SURFACE_CONTAINER_LOW.resolve()));
    check("detached modal window-edge corner also rounded", alpha(modalImg, W - M - 1, M) < 255);
  }

  private static void checkCollapse() {
    MorphAnimator.setReducedMotion(true);
    try {
      final ElwhaSideSheet sheet = ElwhaSideSheet.standardSheet("Collapse");
      sheet.setSheetPosture(SheetPosture.DETACHED);
      sheet.close();
      check(
          "fully-collapsed detached sheet reports width 0 (no margin sliver)",
          sheet.getPreferredSize().width == 0);
    } finally {
      MorphAnimator.setReducedMotion(false);
    }
  }

  private static BufferedImage render(final ElwhaSideSheet sheet) {
    sheet.setSize(W, H);
    sheet.doLayout();
    final BufferedImage img = new BufferedImage(W, H, BufferedImage.TYPE_INT_ARGB);
    final Graphics2D g = img.createGraphics();
    try {
      sheet.paint(g);
    } finally {
      g.dispose();
    }
    return img;
  }

  private static boolean rgbEquals(
      final BufferedImage img, final int x, final int y, final Color expected) {
    final int rgb = img.getRGB(x, y);
    return (rgb >>> 24) == 255 && (rgb & 0xFFFFFF) == (expected.getRGB() & 0xFFFFFF);
  }

  private static int alpha(final BufferedImage img, final int x, final int y) {
    return img.getRGB(x, y) >>> 24;
  }

  private static void check(final String what, final boolean ok) {
    if (!ok) {
      System.err.println("FAIL: " + what);
      System.exit(1);
    }
  }
}
