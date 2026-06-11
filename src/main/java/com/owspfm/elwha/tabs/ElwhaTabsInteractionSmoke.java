package com.owspfm.elwha.tabs;

import com.owspfm.elwha.theme.ColorRole;
import com.owspfm.elwha.theme.ElwhaTheme;
import com.owspfm.elwha.theme.MaterialPalettes;
import com.owspfm.elwha.theme.Mode;
import com.owspfm.elwha.theme.StateLayer;
import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.event.MouseEvent;
import java.awt.image.BufferedImage;

/**
 * S2 headless guard for {@link ElwhaTabs} interaction (#427): click-activation semantics (active
 * click no-op, release-outside abort), the ChangeListener/ActionListener split, the disabled
 * cascade, and pixel-asserted state-layer blends — including the primary variant's
 * inactive-pressed→PRIMARY tint quirk and the inactive content-color lift.
 *
 * @author Charles Bryan
 * @version v0.4.0
 * @since v0.4.0
 */
public final class ElwhaTabsInteractionSmoke {

  private static final int BAR_WIDTH = 480;
  private static final int BLEND_TOLERANCE = 3;

  private ElwhaTabsInteractionSmoke() {}

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

    checkClickSemantics();
    checkDisabledCascade();
    checkStateLayerPixels();

    System.out.println(
        "ElwhaTabsInteractionSmoke: OK (click semantics, listener split, disabled cascade,"
            + " layer blends incl. inactive-pressed PRIMARY quirk + content lift)");
  }

  private static void checkClickSemantics() {
    final ElwhaTabs bar = laidOutBar(ElwhaTabs.primary());
    final int[] changes = {0};
    final int[] actions = {0, 0, 0};
    bar.addChangeListener(e -> changes[0]++);
    for (int i = 0; i < 3; i++) {
      final int idx = i;
      bar.getTabAt(i).addActionListener(e -> actions[idx]++);
    }

    click(bar.getTabAt(1));
    check("click activates tab 1", bar.getActiveTabIndex() == 1);
    check("click fires one change", changes[0] == 1);
    check("click fires tab 1 action", actions[1] == 1);
    check("no action on other tabs", actions[0] == 0 && actions[2] == 0);

    click(bar.getTabAt(1));
    check("clicking the active tab is a no-op", changes[0] == 1 && actions[1] == 1);

    bar.setActiveTabIndex(2);
    check("programmatic activation fires change", changes[0] == 2);
    check("programmatic activation fires NO action", actions[2] == 0);

    pressReleaseOutside(bar.getTabAt(0));
    check("release outside aborts activation", bar.getActiveTabIndex() == 2 && actions[0] == 0);
  }

  private static void checkDisabledCascade() {
    final ElwhaTabs bar = laidOutBar(ElwhaTabs.secondary());
    final int[] changes = {0};
    bar.addChangeListener(e -> changes[0]++);

    bar.setEnabled(false);
    check("disable cascades to tabs", !bar.getTabAt(1).isEnabled());
    click(bar.getTabAt(1));
    check("disabled bar ignores clicks", bar.getActiveTabIndex() == 0 && changes[0] == 0);

    bar.setEnabled(true);
    check("re-enable cascades to tabs", bar.getTabAt(1).isEnabled());
    final ElwhaTab late = bar.addTab("Late");
    check("addTab stamps enabled state", late.isEnabled());
    click(bar.getTabAt(1));
    check("re-enabled bar activates again", bar.getActiveTabIndex() == 1 && changes[0] == 1);
  }

  private static void checkStateLayerPixels() {
    final Color surface = ColorRole.SURFACE.resolve();

    final ElwhaTabs primaryBar = laidOutBar(ElwhaTabs.primary());
    final ElwhaTab inactive = primaryBar.getTabAt(1);

    inactive.setHovered(true);
    Color expected = StateLayer.HOVER.over(surface, ColorRole.ON_SURFACE);
    check(
        "inactive hover layer blends ON_SURFACE @ 0.08",
        near(layerSample(primaryBar, inactive), expected));
    check(
        "hover lifts inactive content to ON_SURFACE",
        rowHasColorNear(
            render(primaryBar),
            24,
            ColorRole.ON_SURFACE.resolve(),
            inactive.getX(),
            inactive.getX() + inactive.getWidth()));
    inactive.setHovered(false);

    inactive.setPressed(true);
    expected = StateLayer.PRESSED.over(surface, ColorRole.PRIMARY);
    check(
        "primary inactive PRESSED layer tints PRIMARY (the quirk)",
        near(layerSample(primaryBar, inactive), expected));
    inactive.setPressed(false);

    final ElwhaTab active = primaryBar.getTabAt(0);
    active.setHovered(true);
    expected = StateLayer.HOVER.over(surface, ColorRole.PRIMARY);
    check(
        "active primary hover layer tints PRIMARY",
        near(layerSample(primaryBar, active), expected));
    active.setHovered(false);

    final ElwhaTabs secondaryBar = laidOutBar(ElwhaTabs.secondary());
    final ElwhaTab secondaryInactive = secondaryBar.getTabAt(2);
    secondaryInactive.setPressed(true);
    expected = StateLayer.PRESSED.over(surface, ColorRole.ON_SURFACE);
    check(
        "secondary inactive pressed layer tints ON_SURFACE",
        near(layerSample(secondaryBar, secondaryInactive), expected));
    secondaryInactive.setPressed(false);

    final ElwhaTabs disabledBar = laidOutBar(ElwhaTabs.primary());
    disabledBar.setEnabled(true);
    final BufferedImage enabledRender = render(disabledBar);
    disabledBar.setEnabled(false);
    final BufferedImage disabledRender = render(disabledBar);
    final ElwhaTab first = disabledBar.getTabAt(0);
    check(
        "disabled content never reaches full-strength color",
        rowHasColorNear(
                enabledRender,
                24,
                ColorRole.PRIMARY.resolve(),
                first.getX(),
                first.getX() + first.getWidth())
            && !rowHasColorNear(
                disabledRender,
                24,
                ColorRole.PRIMARY.resolve(),
                first.getX(),
                first.getX() + first.getWidth()));
  }

  // ----------------------------------------------------------------- helpers

  private static ElwhaTabs laidOutBar(final ElwhaTabs bar) {
    bar.addTab("One");
    bar.addTab("Two");
    bar.addTab("Three");
    bar.setSize(BAR_WIDTH, ElwhaTabs.BAR_HEIGHT_INLINE_PX);
    bar.doLayout();
    return bar;
  }

  private static void click(final ElwhaTab tab) {
    final int x = tab.getWidth() / 2;
    final int y = tab.getHeight() / 2;
    tab.dispatchEvent(mouse(tab, MouseEvent.MOUSE_PRESSED, x, y));
    tab.dispatchEvent(mouse(tab, MouseEvent.MOUSE_RELEASED, x, y));
  }

  private static void pressReleaseOutside(final ElwhaTab tab) {
    tab.dispatchEvent(mouse(tab, MouseEvent.MOUSE_PRESSED, tab.getWidth() / 2, 10));
    tab.dispatchEvent(mouse(tab, MouseEvent.MOUSE_RELEASED, -20, -20));
  }

  private static MouseEvent mouse(final ElwhaTab tab, final int id, final int x, final int y) {
    return new MouseEvent(
        tab, id, System.currentTimeMillis(), 0, x, y, 1, false, MouseEvent.BUTTON1);
  }

  // Samples a layer-only pixel inside the tab: near the leading edge, above the indicator band,
  // away from label glyphs.
  private static Color layerSample(final ElwhaTabs bar, final ElwhaTab tab) {
    final BufferedImage image = render(bar);
    return new Color(image.getRGB(tab.getX() + 4, 8), true);
  }

  private static BufferedImage render(final ElwhaTabs bar) {
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

  private static boolean near(final Color a, final Color b) {
    return Math.abs(a.getRed() - b.getRed()) <= BLEND_TOLERANCE
        && Math.abs(a.getGreen() - b.getGreen()) <= BLEND_TOLERANCE
        && Math.abs(a.getBlue() - b.getBlue()) <= BLEND_TOLERANCE;
  }

  private static boolean rowHasColorNear(
      final BufferedImage image, final int y, final Color c, final int fromX, final int toX) {
    for (int x = Math.max(0, fromX); x < Math.min(image.getWidth(), toX); x++) {
      final Color px = new Color(image.getRGB(x, y), true);
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
