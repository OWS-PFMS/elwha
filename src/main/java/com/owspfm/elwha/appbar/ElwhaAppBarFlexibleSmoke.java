package com.owspfm.elwha.appbar;

import com.owspfm.elwha.theme.ColorRole;
import com.owspfm.elwha.theme.ElwhaTheme;
import com.owspfm.elwha.theme.MaterialPalettes;
import com.owspfm.elwha.theme.Mode;
import com.owspfm.elwha.theme.TypeRole;
import com.owspfm.elwha.theme.Typography;
import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;

/**
 * S2 headless guard for the {@link TypeRole} display tier + {@link ElwhaAppBar} flexible expanded
 * chrome (#456): the three display roles exist with the M3 sizes and resolve through the installed
 * {@link Typography}, and the flexible variants paint the expanded headline block (16&nbsp;px
 * margin, bottom-anchored, role-correct title/subtitle colors) with a clean strip above it.
 *
 * @author Charles Bryan
 * @version v0.4.0
 * @since v0.4.0
 */
public final class ElwhaAppBarFlexibleSmoke {

  private static final int BAR_WIDTH = 640;

  private ElwhaAppBarFlexibleSmoke() {}

  /**
   * Runs the guard; exits non-zero on the first failed check.
   *
   * @param args unused
   * @version v0.4.0
   * @since v0.4.0
   */
  public static void main(final String[] args) {
    System.setProperty("java.awt.headless", "true");
    ElwhaTheme.install(
        ElwhaTheme.config().theme(MaterialPalettes.baseline()).mode(Mode.LIGHT).build());

    checkDisplayTier();
    checkExpandedChrome(Mode.LIGHT);
    ElwhaTheme.install(
        ElwhaTheme.config().theme(MaterialPalettes.baseline()).mode(Mode.DARK).build());
    checkExpandedChrome(Mode.DARK);

    System.out.println(
        "ElwhaAppBarFlexibleSmoke: OK (display tier roles + typography, expanded headline chrome"
            + " light+dark)");
  }

  private static void checkDisplayTier() {
    check("scale is the full 15", TypeRole.values().length == 15);
    check(
        "DISPLAY_LARGE 57 regular",
        TypeRole.DISPLAY_LARGE.pt() == 57 && !TypeRole.DISPLAY_LARGE.medium());
    check(
        "DISPLAY_MEDIUM 45 regular",
        TypeRole.DISPLAY_MEDIUM.pt() == 45 && !TypeRole.DISPLAY_MEDIUM.medium());
    check(
        "DISPLAY_SMALL 36 regular",
        TypeRole.DISPLAY_SMALL.pt() == 36 && !TypeRole.DISPLAY_SMALL.medium());
    check("display key", TypeRole.DISPLAY_SMALL.key().equals("displaySmall"));

    check(
        "Typography.defaults covers DISPLAY_SMALL",
        Typography.defaults().get(TypeRole.DISPLAY_SMALL) != null);
    check(
        "DISPLAY_SMALL resolves at 36 through the installed theme",
        TypeRole.DISPLAY_SMALL.resolve().getSize() == 36);
    check(
        "DISPLAY_LARGE resolves at 57 through the installed theme",
        TypeRole.DISPLAY_LARGE.resolve().getSize() == 57);

    check(
        "variant expanded roles",
        AppBarVariant.MEDIUM_FLEXIBLE.expandedTitleRole() == TypeRole.HEADLINE_MEDIUM
            && AppBarVariant.MEDIUM_FLEXIBLE.expandedSubtitleRole() == TypeRole.LABEL_LARGE
            && AppBarVariant.LARGE_FLEXIBLE.expandedTitleRole() == TypeRole.DISPLAY_SMALL
            && AppBarVariant.LARGE_FLEXIBLE.expandedSubtitleRole() == TypeRole.TITLE_MEDIUM
            && AppBarVariant.SMALL.expandedTitleRole() == TypeRole.TITLE_LARGE);
  }

  private static void checkExpandedChrome(final Mode mode) {
    final String tag = " [" + mode + "]";
    final Color surface = ColorRole.SURFACE.resolve();
    final Color onSurface = ColorRole.ON_SURFACE.resolve();
    final Color onSurfaceVariant = ColorRole.ON_SURFACE_VARIANT.resolve();

    final ElwhaAppBar medium = ElwhaAppBar.mediumFlexible();
    medium.setTitle("Headline");
    medium.setSize(BAR_WIDTH, 112);
    final BufferedImage m = render(medium, 112);
    check(
        "medium expanded title ink in the bottom block" + tag,
        bandHasColorNear(m, 66, 110, onSurface));
    check("medium strip is clean at rest (no collapsed title)" + tag, bandIsSurface(m, 4, 50));
    final int firstInk = firstInkX(m, 0, 66, 110);
    check("expanded title starts at the 16px margin" + tag, firstInk >= 14 && firstInk <= 20);

    final ElwhaAppBar large = ElwhaAppBar.largeFlexible();
    large.setTitle("Headline");
    large.setSubtitle("Subtitle");
    large.setSize(BAR_WIDTH, 152);
    final BufferedImage l = render(large, 152);
    check(
        "large expanded title ink (DISPLAY_SMALL) present" + tag,
        bandHasColorNear(l, 70, 124, onSurface));
    check(
        "large subtitle ink (ON_SURFACE_VARIANT) below the title" + tag,
        bandHasColorNear(l, 118, 150, onSurfaceVariant));
    check("large container fill" + tag, sample(l, BAR_WIDTH - 5, 70).equals(surface));

    final ElwhaAppBar centered = ElwhaAppBar.largeFlexible();
    centered.setTitle("Centered");
    centered.setTitleCentered(true);
    centered.setSize(BAR_WIDTH, 120);
    final BufferedImage c = render(centered, 120);
    final int left = firstInkX(c, 0, 66, 118);
    final int right = lastInkX(c, 66, 118);
    check(
        "centered expanded title symmetric" + tag,
        Math.abs((left + right) / 2 - BAR_WIDTH / 2) <= 8);
  }

  private static BufferedImage render(final ElwhaAppBar bar, final int height) {
    bar.doLayout();
    final BufferedImage image = new BufferedImage(BAR_WIDTH, height, BufferedImage.TYPE_INT_ARGB);
    final Graphics2D g = image.createGraphics();
    try {
      bar.paint(g);
    } finally {
      g.dispose();
    }
    return image;
  }

  private static Color sample(final BufferedImage image, final int x, final int y) {
    return new Color(image.getRGB(x, y), true);
  }

  private static boolean bandIsSurface(final BufferedImage image, final int yFrom, final int yTo) {
    final Color surface = ColorRole.SURFACE.resolve();
    for (int y = yFrom; y <= yTo; y++) {
      for (int x = 0; x < image.getWidth(); x++) {
        if (colorDistance(sample(image, x, y), surface) > 12) {
          return false;
        }
      }
    }
    return true;
  }

  private static boolean bandHasColorNear(
      final BufferedImage image, final int yFrom, final int yTo, final Color c) {
    for (int y = yFrom; y <= yTo; y++) {
      for (int x = 0; x < image.getWidth(); x++) {
        if (colorDistance(sample(image, x, y), c) < 60) {
          return true;
        }
      }
    }
    return false;
  }

  private static int firstInkX(
      final BufferedImage image, final int fromX, final int yFrom, final int yTo) {
    final Color surface = ColorRole.SURFACE.resolve();
    for (int x = fromX; x < image.getWidth(); x++) {
      for (int y = yFrom; y <= yTo; y++) {
        if (colorDistance(sample(image, x, y), surface) > 60) {
          return x;
        }
      }
    }
    return -1;
  }

  private static int lastInkX(final BufferedImage image, final int yFrom, final int yTo) {
    final Color surface = ColorRole.SURFACE.resolve();
    for (int x = image.getWidth() - 1; x >= 0; x--) {
      for (int y = yFrom; y <= yTo; y++) {
        if (colorDistance(sample(image, x, y), surface) > 60) {
          return x;
        }
      }
    }
    return -1;
  }

  private static int colorDistance(final Color a, final Color b) {
    return Math.abs(a.getRed() - b.getRed())
        + Math.abs(a.getGreen() - b.getGreen())
        + Math.abs(a.getBlue() - b.getBlue());
  }

  private static void check(final String what, final boolean ok) {
    if (!ok) {
      System.err.println("FAIL: " + what);
      System.exit(1);
    }
    System.out.println("  ok: " + what);
  }
}
