package com.owspfm.elwha.tabs;

import com.owspfm.elwha.theme.ColorRole;
import com.owspfm.elwha.theme.ElwhaTheme;
import com.owspfm.elwha.theme.MaterialPalettes;
import com.owspfm.elwha.theme.Mode;
import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.Rectangle;
import java.awt.image.BufferedImage;

/**
 * S1 headless guard for {@link ElwhaTabs} static chrome (#426): selection API + mandatory-one
 * semantics, FIXED layout geometry, and pixel-asserted container / divider / indicator paint for
 * both variants in light and dark modes.
 *
 * @author Charles Bryan
 * @version v0.4.0
 * @since v0.4.0
 */
public final class ElwhaTabsChromeSmoke {

  private static final int BAR_WIDTH = 480;

  private ElwhaTabsChromeSmoke() {}

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

    checkSelectionApi();
    checkRemovalSemantics();
    checkFixedLayout();
    checkChromePixels(Mode.LIGHT);
    ElwhaTheme.install(
        ElwhaTheme.config().theme(MaterialPalettes.baseline()).mode(Mode.DARK).build());
    checkChromePixels(Mode.DARK);

    System.out.println(
        "ElwhaTabsChromeSmoke: OK (selection API, mandatory-one, fixed layout, light+dark"
            + " container/divider/indicator pixels, both variants)");
  }

  private static void checkSelectionApi() {
    final ElwhaTabs bar = ElwhaTabs.primary();
    final int[] changes = {0};
    bar.addChangeListener(e -> changes[0]++);

    check("empty bar has activeTabIndex -1", bar.getActiveTabIndex() == -1);
    check("empty bar has null activeTab", bar.getActiveTab() == null);

    final ElwhaTab first = bar.addTab("One");
    check("first add auto-activates index 0", bar.getActiveTabIndex() == 0);
    check("first add auto-activation is silent", changes[0] == 0);
    check("auto-activated tab isActive", first.isActive());

    bar.addTab("Two");
    bar.addTab("Three");
    check("later adds do not steal activation", bar.getActiveTabIndex() == 0);

    bar.setActiveTabIndex(2);
    check("setActiveTabIndex activates", bar.getActiveTabIndex() == 2);
    check("activation fires one change", changes[0] == 1);
    check("previous tab deactivated", !first.isActive());
    check("getActiveTab tracks index", bar.getActiveTab() == bar.getTabAt(2));

    bar.setActiveTabIndex(2);
    check("re-activating active index is a no-op", changes[0] == 1);
    bar.setActiveTabIndex(7);
    check("out-of-range index ignored", bar.getActiveTabIndex() == 2 && changes[0] == 1);

    bar.setActiveTab(bar.getTabAt(1));
    check("setActiveTab activates by reference", bar.getActiveTabIndex() == 1 && changes[0] == 2);
    bar.setActiveTab(ElwhaTab.of("foreign"));
    check("foreign tab ignored", bar.getActiveTabIndex() == 1 && changes[0] == 2);

    check("variant getter", bar.getVariant() == TabsVariant.PRIMARY);
    check("secondary factory variant", ElwhaTabs.secondary().getVariant() == TabsVariant.SECONDARY);
  }

  private static void checkRemovalSemantics() {
    final ElwhaTabs bar = ElwhaTabs.secondary();
    final ElwhaTab a = bar.addTab("A");
    final ElwhaTab b = bar.addTab("B");
    final ElwhaTab c = bar.addTab("C");
    final int[] changes = {0};
    bar.addChangeListener(e -> changes[0]++);

    bar.setActiveTabIndex(1);
    check("setup: B active", bar.getActiveTab() == b && changes[0] == 1);

    bar.removeTab(a);
    check("removing before active keeps B active", bar.getActiveTab() == b);
    check("index shifts down after removal", bar.getActiveTabIndex() == 0);
    check("non-active removal fires no change", changes[0] == 1);

    bar.removeTab(b);
    check("removing active re-activates first", bar.getActiveTab() == c);
    check("active removal fires change", changes[0] == 2);
    check("removed tab no longer active", !b.isActive());

    bar.removeTab(c);
    check("last removal empties selection", bar.getActiveTabIndex() == -1);
    check("tab count zero", bar.getTabCount() == 0);
  }

  private static void checkFixedLayout() {
    final ElwhaTabs bar = ElwhaTabs.primary();
    bar.addTab("One");
    bar.addTab("Two");
    bar.addTab("Three");
    bar.setSize(BAR_WIDTH + 1, ElwhaTabs.BAR_HEIGHT_INLINE_PX);
    bar.doLayout();

    check("preferred height is 48", bar.getPreferredSize().height == 48);
    final int w0 = bar.getTabAt(0).getWidth();
    final int w1 = bar.getTabAt(1).getWidth();
    final int w2 = bar.getTabAt(2).getWidth();
    check("equal widths fill the bar", w0 + w1 + w2 == BAR_WIDTH + 1);
    check("remainder distributed leading-first", w0 == 161 && w1 == 160 && w2 == 160);
    check("tabs abut", bar.getTabAt(1).getX() == w0 && bar.getTabAt(2).getX() == w0 + w1);
    check("tabs fill bar height", bar.getTabAt(0).getHeight() == 48);
  }

  private static void checkChromePixels(final Mode mode) {
    final String tag = " [" + mode + "]";
    final Color surface = ColorRole.SURFACE.resolve();
    final Color divider = ColorRole.OUTLINE_VARIANT.resolve();
    final Color primary = ColorRole.PRIMARY.resolve();

    final ElwhaTabs primaryBar = ElwhaTabs.primary();
    primaryBar.addTab("One");
    primaryBar.addTab("Two");
    primaryBar.addTab("Three");
    final BufferedImage p = render(primaryBar);

    check("container fill is SURFACE" + tag, sample(p, 5, 5).equals(surface));
    check("divider is OUTLINE_VARIANT" + tag, sample(p, 5, 47).equals(divider));

    final Rectangle rest = primaryBar.indicatorRestRect(primaryBar.getActiveTab());
    final int cx = rest.x + rest.width / 2;
    check("primary indicator hugs content (narrower than tab)" + tag,
        rest.width < primaryBar.getTabAt(0).getWidth() - 2 * ElwhaTab.H_PADDING_PX + 1);
    check("primary indicator center is PRIMARY (y=45)" + tag, sample(p, cx, 45).equals(primary));
    check("primary indicator covers divider row" + tag, sample(p, cx, 47).equals(primary));
    check("no indicator outside content span" + tag,
        sample(p, primaryBar.getTabAt(0).getX() + 4, 46).equals(surface)
            && sample(p, primaryBar.getTabAt(0).getX() + 4, 47).equals(divider));
    check("no indicator under inactive tab" + tag,
        sample(p, primaryBar.getTabAt(1).getX() + primaryBar.getTabAt(1).getWidth() / 2, 46)
            .equals(surface));

    check("active primary label paints toward PRIMARY" + tag, rowHasColorNear(p, 24, primary));

    final ElwhaTabs secondaryBar = ElwhaTabs.secondary();
    secondaryBar.addTab("One");
    secondaryBar.addTab("Two");
    secondaryBar.addTab("Three");
    final BufferedImage s = render(secondaryBar);

    final ElwhaTab active = secondaryBar.getActiveTab();
    check("secondary indicator spans full tab (leading edge)" + tag,
        sample(s, active.getX() + 1, 46).equals(primary));
    check("secondary indicator spans full tab (trailing edge)" + tag,
        sample(s, active.getX() + active.getWidth() - 2, 46).equals(primary));
    check("secondary indicator is 2px (y=45 is SURFACE)" + tag,
        sample(s, active.getX() + active.getWidth() / 2, 45).equals(surface));
    check("secondary divider beyond active tab" + tag,
        sample(s, active.getX() + active.getWidth() + 5, 47).equals(divider));
  }

  private static BufferedImage render(final ElwhaTabs bar) {
    bar.setSize(BAR_WIDTH, ElwhaTabs.BAR_HEIGHT_INLINE_PX);
    bar.doLayout();
    final BufferedImage image =
        new BufferedImage(BAR_WIDTH, ElwhaTabs.BAR_HEIGHT_INLINE_PX, BufferedImage.TYPE_INT_ARGB);
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

  private static boolean rowHasColorNear(final BufferedImage image, final int y, final Color c) {
    for (int x = 0; x < image.getWidth(); x++) {
      final Color px = sample(image, x, y);
      final int d =
          Math.abs(px.getRed() - c.getRed())
              + Math.abs(px.getGreen() - c.getGreen())
              + Math.abs(px.getBlue() - c.getBlue());
      if (d < 60) {
        return true;
      }
    }
    return false;
  }

  private static void check(final String what, final boolean ok) {
    if (!ok) {
      System.err.println("FAIL: " + what);
      System.exit(1);
    }
    System.out.println("  ok: " + what);
  }
}
