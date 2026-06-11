package com.owspfm.elwha.appbar;

import com.owspfm.elwha.iconbutton.ElwhaIconButton;
import com.owspfm.elwha.icons.MaterialIcons;
import com.owspfm.elwha.theme.ColorRole;
import com.owspfm.elwha.theme.ElwhaTheme;
import com.owspfm.elwha.theme.MaterialPalettes;
import com.owspfm.elwha.theme.Mode;
import java.awt.Color;
import java.awt.FontMetrics;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;

/**
 * S1 headless guard for {@link ElwhaAppBar} small-bar static chrome (#455): variant preferred
 * heights, the 4-48-0-4 slot geometry, the no-nav 16&nbsp;px title inset, ellipsis clipping, and
 * pixel-asserted container / title / subtitle paint in light and dark modes.
 *
 * @author Charles Bryan
 * @version v0.4.0
 * @since v0.4.0
 */
public final class ElwhaAppBarChromeSmoke {

  private static final int BAR_WIDTH = 640;

  private ElwhaAppBarChromeSmoke() {}

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

    checkPreferredHeights();
    checkSlotGeometry();
    checkTitleRegions();
    checkEllipsis();
    checkChromePixels(Mode.LIGHT);
    ElwhaTheme.install(
        ElwhaTheme.config().theme(MaterialPalettes.baseline()).mode(Mode.DARK).build());
    checkChromePixels(Mode.DARK);

    System.out.println(
        "ElwhaAppBarChromeSmoke: OK (variant heights, slot geometry, title regions, ellipsis,"
            + " light+dark chrome pixels)");
  }

  private static void checkPreferredHeights() {
    check("SMALL preferred height 64", ElwhaAppBar.small().getPreferredSize().height == 64);
    final ElwhaAppBar smallSub = ElwhaAppBar.small();
    smallSub.setSubtitle("sub");
    check("SMALL + subtitle stays 64", smallSub.getPreferredSize().height == 64);

    final ElwhaAppBar medium = ElwhaAppBar.mediumFlexible();
    check("MEDIUM_FLEXIBLE expanded 112", medium.getPreferredSize().height == 112);
    medium.setSubtitle("sub");
    check("MEDIUM_FLEXIBLE + subtitle 136", medium.getPreferredSize().height == 136);
    medium.setSubtitle(null);
    check("subtitle clear restores 112", medium.getPreferredSize().height == 112);

    final ElwhaAppBar large = ElwhaAppBar.largeFlexible();
    check("LARGE_FLEXIBLE expanded 120", large.getPreferredSize().height == 120);
    large.setSubtitle("sub");
    check("LARGE_FLEXIBLE + subtitle 152", large.getPreferredSize().height == 152);

    check("variant getter", medium.getVariant() == AppBarVariant.MEDIUM_FLEXIBLE);
    check("no-arg ctor is SMALL", new ElwhaAppBar().getVariant() == AppBarVariant.SMALL);
  }

  private static void checkSlotGeometry() {
    final ElwhaAppBar bar = ElwhaAppBar.small();
    bar.setTitle("Inbox");
    final ElwhaIconButton nav = bar.setNavigationIcon(MaterialIcons.menu(), "Navigation", null);
    final ElwhaIconButton first = bar.addAction(MaterialIcons.favorite(), "Favorite", null);
    final ElwhaIconButton second = bar.addAction(MaterialIcons.moreVert(), "More", null);
    bar.setSize(BAR_WIDTH, 64);
    bar.doLayout();

    check("nav button centered in the leading 48 slot (x=8)", nav.getX() == 8);
    check("nav button vertically centered", nav.getY() == (64 - nav.getHeight()) / 2);
    check(
        "last action's slot ends 4 from the trailing edge",
        second.getX() + second.getWidth() / 2 == BAR_WIDTH - 4 - 24);
    check(
        "action slots abut (48 apart, zero gap)",
        second.getX() - first.getX() == ElwhaAppBar.SLOT_SIZE_PX);
    check(
        "getActions order", bar.getActions().get(0) == first && bar.getActions().get(1) == second);

    bar.removeAction(first);
    bar.doLayout();
    check(
        "removeAction reflows the run",
        second.getX() + second.getWidth() / 2 == BAR_WIDTH - 4 - 24);
    check("getNavigationIcon", bar.getNavigationIcon() == nav);
    bar.setNavigationIcon((ElwhaIconButton) null);
    check("clearing the nav button", bar.getNavigationIcon() == null);
  }

  private static void checkTitleRegions() {
    final ElwhaAppBar noNav = ElwhaAppBar.small();
    noNav.setTitle("Title");
    noNav.setSize(BAR_WIDTH, 64);
    final BufferedImage image = render(noNav, 64);
    final int firstInk = firstInkX(image, 0, 12, 52);
    check("no-nav title starts at the 16px inset", firstInk >= 14 && firstInk <= 20);

    final ElwhaAppBar withNav = ElwhaAppBar.small();
    withNav.setTitle("Title");
    withNav.setNavigationIcon(MaterialIcons.menu(), "Navigation", null);
    withNav.setSize(BAR_WIDTH, 64);
    final BufferedImage navImage = render(withNav, 64);
    final int navFirstInk = firstInkX(navImage, 44, 12, 52);
    check("nav-present title starts after the slot (52)", navFirstInk >= 50 && navFirstInk <= 58);

    final ElwhaAppBar centered = ElwhaAppBar.small();
    centered.setTitle("Centered");
    centered.setTitleCentered(true);
    centered.setSize(BAR_WIDTH, 64);
    final BufferedImage centeredImage = render(centered, 64);
    final int left = firstInkX(centeredImage, 0, 12, 52);
    final int right = lastInkX(centeredImage, 12, 52);
    check(
        "centered title is symmetric about the container",
        Math.abs((left + right) / 2 - BAR_WIDTH / 2) <= 8);
  }

  private static void checkEllipsis() {
    final ElwhaAppBar bar = ElwhaAppBar.small();
    final FontMetrics fm =
        bar.getFontMetrics(com.owspfm.elwha.theme.TypeRole.TITLE_LARGE.resolve());
    final String text = "A very long headline that cannot possibly fit";
    final String clipped = ElwhaAppBar.clipText(text, fm, 120);
    check("clipped text ends with ellipsis", clipped.endsWith("…"));
    check("clipped text fits", fm.stringWidth(clipped) <= 120);
    check("short text untouched", ElwhaAppBar.clipText("Hi", fm, 120).equals("Hi"));
    check(
        "ellipsis-only space yields the bare ellipsis",
        ElwhaAppBar.clipText(text, fm, fm.stringWidth("…")).equals("…"));
  }

  private static void checkChromePixels(final Mode mode) {
    final String tag = " [" + mode + "]";
    final Color surface = ColorRole.SURFACE.resolve();
    final Color onSurface = ColorRole.ON_SURFACE.resolve();
    final Color onSurfaceVariant = ColorRole.ON_SURFACE_VARIANT.resolve();

    final ElwhaAppBar bar = ElwhaAppBar.small();
    bar.setTitle("Inbox");
    bar.setSubtitle("Synced");
    bar.setSize(BAR_WIDTH, 64);
    final BufferedImage image = render(bar, 64);

    check("container fill is SURFACE" + tag, sample(image, BAR_WIDTH - 5, 5).equals(surface));
    check("title band paints toward ON_SURFACE" + tag, bandHasColorNear(image, 10, 36, onSurface));
    check(
        "subtitle band paints toward ON_SURFACE_VARIANT" + tag,
        bandHasColorNear(image, 36, 58, onSurfaceVariant));

    final ElwhaAppBar flexible = ElwhaAppBar.mediumFlexible();
    flexible.setTitle("Headline");
    flexible.setSize(BAR_WIDTH, 112);
    final BufferedImage flexImage = render(flexible, 112);
    check(
        "flexible shell fills SURFACE full-height" + tag,
        sample(flexImage, 10, 100).equals(surface));
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

  // The first/last x (scanning from fromX) in the row band whose pixel departs the SURFACE fill
  // — text ink.
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
