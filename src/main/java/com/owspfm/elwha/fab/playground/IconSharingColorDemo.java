package com.owspfm.elwha.fab.playground;

import com.formdev.flatlaf.extras.FlatSVGIcon;
import com.owspfm.elwha.button.ButtonVariant;
import com.owspfm.elwha.button.ElwhaButton;
import com.owspfm.elwha.fab.ElwhaFab;
import com.owspfm.elwha.iconbutton.ElwhaIconButton;
import com.owspfm.elwha.iconbutton.IconButtonVariant;
import com.owspfm.elwha.icons.MaterialIcons;
import com.owspfm.elwha.theme.ElwhaTheme;
import com.owspfm.elwha.theme.MaterialPalettes;
import com.owspfm.elwha.theme.Mode;
import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.Insets;
import java.awt.image.BufferedImage;
import javax.swing.JComponent;

/**
 * Headless verification harness for the FlatSVGIcon shared-icon color fix ([#197]). Not an
 * interactive playground — it renders components offscreen, samples pixels, and exits non-zero on
 * any failure, so it doubles as a smoke gate.
 *
 * <p>Proves the fix two independent ways:
 *
 * <ol>
 *   <li><strong>Geometry-free (Issue A):</strong> after constructing an {@link ElwhaFab}, an {@link
 *       ElwhaButton}, and an {@link ElwhaIconButton} all from the <em>same</em> {@link FlatSVGIcon}
 *       instance, that shared icon's own {@link FlatSVGIcon#getColorFilter() colorFilter} is
 *       unchanged. Before the fix, each constructor stomped the shared instance's single filter
 *       field, so the last-constructed component's color won for all of them and the consumer's own
 *       filter was clobbered.
 *   <li><strong>Render proof (Issues A &amp; B):</strong> two FABs built from one shared icon at
 *       different color styles paint the glyph in their own colors; and a single FAB repainted
 *       after {@link ElwhaFab#setColor} renders the glyph at the new color on the next paint
 *       (confirming the live-filter path — FlatLaf 3.2.5 applies the color filter at paint time, so
 *       there is no stale raster to invalidate).
 * </ol>
 *
 * @author Charles Bryan
 * @version v0.3.0
 * @since v0.3.0
 */
public final class IconSharingColorDemo {

  private IconSharingColorDemo() {}

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

    sharedFilterUntouched();
    fabPaintsOwnColor();
    fabSetColorRepaints();

    if (failures > 0) {
      System.err.println("FAIL: " + failures + " check(s) failed.");
      System.exit(1);
    }
    System.out.println("PASS: all icon-sharing color checks passed.");
  }

  // Issue A, geometry-free: the shared icon's own filter must survive construction of all three.
  private static void sharedFilterUntouched() {
    final FlatSVGIcon shared = MaterialIcons.add(24);
    final FlatSVGIcon.ColorFilter before = shared.getColorFilter();

    ElwhaFab.standard(shared).setColor(ElwhaFab.Color.PRIMARY_CONTAINER);
    new ElwhaButton("B", shared).setVariant(ButtonVariant.FILLED);
    new ElwhaIconButton(shared).setVariant(IconButtonVariant.FILLED);

    final FlatSVGIcon.ColorFilter after = shared.getColorFilter();
    check(
        "shared icon's colorFilter is untouched after constructing Fab + Button + IconButton",
        before == after);
  }

  // Issue A, render proof: two FABs sharing one icon paint the glyph in their own on-container
  // color.
  private static void fabPaintsOwnColor() {
    final FlatSVGIcon shared = MaterialIcons.add(24);
    final ElwhaFab a = ElwhaFab.standard(shared).setColor(ElwhaFab.Color.PRIMARY_CONTAINER);
    final ElwhaFab b = ElwhaFab.standard(shared).setColor(ElwhaFab.Color.TERTIARY);

    final Color glyphA = glyphColor(a);
    final Color glyphB = glyphColor(b);
    final Color expectA = ElwhaFab.Color.PRIMARY_CONTAINER.onContainerRole().resolve();
    final Color expectB = ElwhaFab.Color.TERTIARY.onContainerRole().resolve();

    check("FAB-A glyph ≈ onPrimaryContainer (got " + hex(glyphA) + ")", near(glyphA, expectA));
    check("FAB-B glyph ≈ onTertiary (got " + hex(glyphB) + ")", near(glyphB, expectB));
    check(
        "two FABs sharing one icon paint distinct glyph colors ("
            + hex(glyphA)
            + " vs "
            + hex(glyphB)
            + ")",
        !near(glyphA, glyphB));
  }

  // Issue B render proof: setColor re-renders the glyph fresh on the next paint.
  private static void fabSetColorRepaints() {
    final ElwhaFab fab = ElwhaFab.standard(MaterialIcons.add(24));
    final Color first = glyphColor(fab);
    fab.setColor(ElwhaFab.Color.TERTIARY);
    final Color second = glyphColor(fab);
    final Color expectSecond = ElwhaFab.Color.TERTIARY.onContainerRole().resolve();

    check(
        "setColor(TERTIARY) repaints glyph at the new color (was "
            + hex(first)
            + ", now "
            + hex(second)
            + ")",
        !near(first, second) && near(second, expectSecond));
  }

  // The Standard FAB centers a '+' glyph; the bars cross at the body center, so the center pixel
  // sits on the glyph stroke (on-container color), painted over the container fill.
  private static Color glyphColor(final ElwhaFab fab) {
    final BufferedImage img = render(fab);
    final Insets halo = fab.getShadowInsets();
    final int cx = halo.left + (img.getWidth() - halo.left - halo.right) / 2;
    final int cy = halo.top + (img.getHeight() - halo.top - halo.bottom) / 2;
    return new Color(img.getRGB(cx, cy), true);
  }

  private static BufferedImage render(final JComponent c) {
    final java.awt.Dimension d = c.getPreferredSize();
    c.setSize(d);
    c.doLayout();
    final BufferedImage img =
        new BufferedImage(Math.max(1, d.width), Math.max(1, d.height), BufferedImage.TYPE_INT_ARGB);
    final Graphics2D g = img.createGraphics();
    try {
      c.printAll(g);
    } finally {
      g.dispose();
    }
    return img;
  }

  private static boolean near(final Color x, final Color y) {
    final int dr = x.getRed() - y.getRed();
    final int dg = x.getGreen() - y.getGreen();
    final int db = x.getBlue() - y.getBlue();
    return dr * dr + dg * dg + db * db <= 60 * 60;
  }

  private static String hex(final Color c) {
    return String.format("#%02X%02X%02X", c.getRed(), c.getGreen(), c.getBlue());
  }

  private static void check(final String label, final boolean ok) {
    System.out.println((ok ? "  ok   " : "  FAIL ") + label);
    if (!ok) {
      failures++;
    }
  }
}
