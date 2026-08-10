package com.owspfm.elwha.card.fixes;

import com.owspfm.elwha.card.ElwhaCard;
import com.owspfm.elwha.card.ElwhaCardMedia;
import com.owspfm.elwha.surface.ElwhaSurface;
import com.owspfm.elwha.theme.ColorRole;
import com.owspfm.elwha.theme.ElwhaTheme;
import com.owspfm.elwha.theme.MaterialPalettes;
import com.owspfm.elwha.theme.Mode;
import java.awt.Color;
import java.awt.Component;
import java.awt.Container;
import java.awt.Graphics2D;
import java.awt.Insets;
import java.awt.image.BufferedImage;
import javax.swing.JLabel;

/**
 * Headless smoke gate for single-pass corner antialiasing ([#163]). Not an interactive playground —
 * it renders an {@link ElwhaCard} with a full-bleed opaque {@link ElwhaCardMedia} cover and asserts
 * the rounded corner carries no chassis-fill tint term, the fringe the old two-pass paint left
 * behind. Exits non-zero on any mismatch so it doubles as a CI gate.
 *
 * <p><strong>How the fringe is detected.</strong> The card is rendered over a pure-blue backdrop
 * with a pure-red media cover. Every honest corner pixel is therefore a red-over-blue blend, whose
 * green channel is ~0. The chassis fill (a light surface role, green ≈ 0xE0) only appears at the
 * corner if the fill and the media antialiased their edges in two separate passes — so any corner
 * pixel with a non-trivial green channel is a fill fringe. After the fix the fill is folded into
 * the same clip buffer as the media and shares one antialiased cut, so green stays at the floor.
 *
 * @author Charles Bryan
 * @version v0.4.0
 * @since v0.4.0
 */
public final class SinglePassCornerAaDemo {

  private SinglePassCornerAaDemo() {}

  private static final int SCALE = 3;
  private static final Color BACKDROP = Color.BLUE;
  private static final Color MEDIA = Color.RED;
  private static final int GREEN_FLOOR = 40;

  private static int failures;

  /**
   * Runs the verification and exits non-zero if any check fails.
   *
   * @param args ignored
   */
  public static void main(final String[] args) {
    System.setProperty("java.awt.headless", "true");
    ElwhaTheme.install(
        ElwhaTheme.config().theme(MaterialPalettes.baseline()).mode(Mode.LIGHT).build());

    fillIsActuallyGreenish();
    cornerHasNoFillFringe();
    cornerIsCutToBackdrop();
    plainBorderedSurfaceRenders();

    if (failures > 0) {
      System.err.println("FAIL: " + failures + " check(s) failed.");
      System.exit(1);
    }
    System.out.println("PASS: single-pass corner AA — no fill fringe, media cut, border intact.");
  }

  /**
   * Sensitivity guard: the chassis fill must be visibly green-ish, otherwise the green-channel
   * fringe probe below could pass vacuously. Reads the center of a media-less filled card.
   */
  private static void fillIsActuallyGreenish() {
    final ElwhaCard card = ElwhaCard.filledCard();
    card.add(new JLabel(" "));
    final BufferedImage img = render(card, 120, 90);
    final Color fill = new Color(img.getRGB(img.getWidth() / 2, img.getHeight() / 2), true);
    check(
        "chassis fill is green-ish (probe is sensitive) — got " + fill,
        fill.getGreen() > 150 && fill.getAlpha() > 200);
  }

  /**
   * The whole top-left and top-right corner band must be a clean red-over-blue blend (green ~0).
   */
  private static void cornerHasNoFillFringe() {
    final int w = 200;
    final int h = 130;
    final ElwhaCard card = mediaCard(h);
    final BufferedImage img = render(card, w, h);
    final Insets ins = card.getInsets();
    final int bx = ins.left * SCALE;
    final int by = ins.top * SCALE;
    final int bw = (w - ins.left - ins.right) * SCALE;
    final int band = (12 + 4) * SCALE; // arc (~12) + slack, in device pixels

    int boundaryPixels = 0;
    int worstGreen = 0;
    for (int dy = 0; dy < band; dy++) {
      for (int dx = 0; dx < band; dx++) {
        final int leftX = bx + dx;
        final int rightX = bx + bw - 1 - dx;
        final int y = by + dy;
        worstGreen = Math.max(worstGreen, scanGreen(img, leftX, y));
        worstGreen = Math.max(worstGreen, scanGreen(img, rightX, y));
        if (isCurveBoundary(pixel(img, leftX, y)) || isCurveBoundary(pixel(img, rightX, y))) {
          boundaryPixels++;
        }
      }
    }
    check("corner is genuinely antialiased (boundary pixels exist)", boundaryPixels > 0);
    check(
        "no fill fringe along the corner (max green " + worstGreen + " < " + GREEN_FLOOR + ")",
        worstGreen < GREEN_FLOOR);
  }

  /** Just outside the arc (inside the body rect) the media is cut, so the blue backdrop shows. */
  private static void cornerIsCutToBackdrop() {
    final int w = 200;
    final int h = 130;
    final ElwhaCard card = mediaCard(h);
    final BufferedImage img = render(card, w, h);
    final Insets ins = card.getInsets();
    final Color corner = new Color(pixel(img, ins.left * SCALE + 1, ins.top * SCALE + 1), true);
    check(
        "body corner cut to backdrop (blue, media not painted) — got " + corner,
        corner.getBlue() > 180 && corner.getRed() < 80);
  }

  /** A plain bordered surface with no children still renders a rounded, filled, bordered body. */
  private static void plainBorderedSurfaceRenders() {
    final ElwhaSurface surface =
        new ElwhaSurface()
            .setSurfaceRole(ColorRole.SURFACE_CONTAINER)
            .setBorderRole(ColorRole.OUTLINE)
            .setBorderWidth(2);
    final BufferedImage img = render(surface, 100, 60);
    final Color center = new Color(pixel(img, img.getWidth() / 2, img.getHeight() / 2), true);
    check("bordered childless surface paints a fill — got " + center, center.getAlpha() > 200);
    final Insets ins = surface.getInsets();
    final Color bodyCorner = new Color(pixel(img, ins.left * SCALE + 1, ins.top * SCALE + 1), true);
    check(
        "bordered surface corner is rounded (backdrop shows) — got " + bodyCorner,
        bodyCorner.getBlue() > 180 && bodyCorner.getRed() < 80);
  }

  private static int scanGreen(final BufferedImage img, final int x, final int y) {
    if (x < 0 || y < 0 || x >= img.getWidth() || y >= img.getHeight()) {
      return 0;
    }
    return new Color(img.getRGB(x, y), true).getGreen();
  }

  private static int pixel(final BufferedImage img, final int x, final int y) {
    final int cx = Math.max(0, Math.min(img.getWidth() - 1, x));
    final int cy = Math.max(0, Math.min(img.getHeight() - 1, y));
    return img.getRGB(cx, cy);
  }

  private static boolean isCurveBoundary(final int argb) {
    final Color c = new Color(argb, true);
    // A partially-covered red-over-blue pixel: both red and blue present, neither saturated.
    return c.getRed() > 30 && c.getRed() < 230 && c.getBlue() > 30 && c.getBlue() < 230;
  }

  private static ElwhaCard mediaCard(final int h) {
    final ElwhaCard card = ElwhaCard.filledCard();
    card.add(
        ElwhaCardMedia.painter(
                (g, mw, mh) -> {
                  g.setColor(MEDIA);
                  g.fillRect(0, 0, mw, mh);
                })
            .setPreferredHeight(h));
    return card;
  }

  private static BufferedImage render(final Component c, final int w, final int h) {
    c.setSize(w, h);
    layoutTree(c);
    final BufferedImage img = new BufferedImage(w * SCALE, h * SCALE, BufferedImage.TYPE_INT_ARGB);
    final Graphics2D g = img.createGraphics();
    try {
      g.setColor(BACKDROP);
      g.fillRect(0, 0, w * SCALE, h * SCALE);
      g.scale(SCALE, SCALE);
      c.paint(g);
    } finally {
      g.dispose();
    }
    return img;
  }

  private static void layoutTree(final Component c) {
    c.doLayout();
    if (c instanceof Container container) {
      for (final Component child : container.getComponents()) {
        layoutTree(child);
      }
    }
  }

  private static void check(final String label, final boolean ok) {
    if (!ok) {
      System.out.println("  FAIL " + label);
      failures++;
    } else {
      System.out.println("  ok   " + label);
    }
  }
}
