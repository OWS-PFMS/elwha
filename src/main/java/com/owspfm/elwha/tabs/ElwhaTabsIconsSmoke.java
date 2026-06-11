package com.owspfm.elwha.tabs;

import com.owspfm.elwha.badge.ElwhaBadge;
import com.owspfm.elwha.icons.MaterialIcons;
import com.owspfm.elwha.theme.ColorRole;
import com.owspfm.elwha.theme.ElwhaTheme;
import com.owspfm.elwha.theme.MaterialPalettes;
import com.owspfm.elwha.theme.Mode;
import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.Rectangle;
import java.awt.image.BufferedImage;

/**
 * S4 headless guard for {@link ElwhaTab} icons and badges (#429): per-form preferred sizing
 * (stacked 64 / inline 48 / icon-only), the secondary always-inline rule, the inline-icon toggle,
 * bottom-alignment of inline tabs in a 64&nbsp;px bar, content-hugging indicator spans per form,
 * icon paint + active tint, {@code IconBearing} bounds, and badge attach/detach in both anchor
 * modes.
 *
 * @author Charles Bryan
 * @version v0.4.0
 * @since v0.4.0
 */
public final class ElwhaTabsIconsSmoke {

  private static final int BAR_WIDTH = 480;

  private ElwhaTabsIconsSmoke() {}

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

    checkFormSizing();
    checkBarHeightAndAlignment();
    checkIndicatorSpans();
    checkIconPaint();
    checkBadges();

    System.out.println(
        "ElwhaTabsIconsSmoke: OK (form sizing, secondary inline rule, inline toggle, 64px bar"
            + " bottom-align, indicator spans, icon paint + active tint, IconBearing, badges)");
  }

  private static void checkFormSizing() {
    final ElwhaTabs primaryBar = ElwhaTabs.primary();
    final ElwhaTab stacked = primaryBar.addTab(ElwhaTab.of(MaterialIcons.symbol("home"), "Home"));
    check("primary icon+label defaults stacked", stacked.isStacked());
    check("stacked preferred height is 64", stacked.getPreferredSize().height == 64);

    stacked.setInlineIcon(true);
    check("inline-icon toggle leaves the stack", !stacked.isStacked());
    check("inline preferred height is 48", stacked.getPreferredSize().height == 48);
    final int inlineWidth = stacked.getPreferredSize().width;
    stacked.setInlineIcon(false);
    check("stacked width hugs max(icon,label)", stacked.getPreferredSize().width < inlineWidth);

    final ElwhaTabs secondaryBar = ElwhaTabs.secondary();
    final ElwhaTab secIcon =
        secondaryBar.addTab(ElwhaTab.of(MaterialIcons.symbol("home"), "Travel"));
    check("secondary icon+label is never stacked", !secIcon.isStacked());
    check("secondary icon tab stays 48 tall", secIcon.getPreferredSize().height == 48);

    final ElwhaTab iconOnly = ElwhaTab.iconOnly(MaterialIcons.symbol("favorite"), "Favorites");
    check("icon-only preferred height is 48", iconOnly.getPreferredSize().height == 48);
    check("icon-only preferred width is 24 + padding",
        iconOnly.getPreferredSize().width == 24 + 2 * ElwhaTab.H_PADDING_PX);

    boolean threw = false;
    try {
      ElwhaTab.iconOnly(MaterialIcons.symbol("home"), "  ");
    } catch (IllegalArgumentException expected) {
      threw = true;
    }
    check("icon-only requires a non-blank accessible label", threw);
  }

  private static void checkBarHeightAndAlignment() {
    final ElwhaTabs bar = ElwhaTabs.primary();
    bar.addTab(ElwhaTab.of(MaterialIcons.symbol("home"), "Stacked"));
    final ElwhaTab labelOnly = bar.addTab("Label only");
    check("bar preferred height grows to 64 with a stacked tab",
        bar.getPreferredSize().height == 64);

    bar.setSize(BAR_WIDTH, 64);
    bar.doLayout();
    check("inline tab bottom-aligns in the 64px bar",
        labelOnly.getY() == 16 && labelOnly.getHeight() == 48);
    check("stacked tab fills the 64px bar", bar.getTabAt(0).getY() == 0
        && bar.getTabAt(0).getHeight() == 64);

    final ElwhaTabs inlineBar = ElwhaTabs.primary();
    inlineBar.addTab("One");
    inlineBar.addTab("Two");
    check("all-inline bar stays 48 tall", inlineBar.getPreferredSize().height == 48);
  }

  private static void checkIndicatorSpans() {
    final ElwhaTabs bar = ElwhaTabs.primary();
    final ElwhaTab stacked = bar.addTab(ElwhaTab.of(MaterialIcons.symbol("home"), "Home"));
    final ElwhaTab iconOnly =
        bar.addTab(ElwhaTab.iconOnly(MaterialIcons.symbol("favorite"), "Favorites"));
    final ElwhaTab inline = bar.addTab(ElwhaTab.of(MaterialIcons.symbol("info"), "Information"));
    inline.setInlineIcon(true);
    bar.setSize(BAR_WIDTH, 64);
    bar.doLayout();

    final Rectangle stackedSpan = stacked.contentSpan();
    final int labelW = stacked.getFontMetrics(
        com.owspfm.elwha.theme.TypeRole.TITLE_SMALL.resolve()).stringWidth("Home");
    check("stacked span hugs max(icon, label)", stackedSpan.width == Math.max(24, labelW));

    check("icon-only span is the icon", iconOnly.contentSpan().width == 24);

    final int infoW = inline.getFontMetrics(
        com.owspfm.elwha.theme.TypeRole.TITLE_SMALL.resolve()).stringWidth("Information");
    check("inline span is icon + gap + label",
        inline.contentSpan().width == 24 + ElwhaTab.INLINE_GAP_PX + infoW);

    bar.setActiveTab(iconOnly);
    final Rectangle rest = bar.indicatorRestRect(iconOnly);
    check("indicator hugs the icon-only span",
        rest.width == 24 && rest.x == iconOnly.getX() + iconOnly.contentSpan().x);
  }

  private static void checkIconPaint() {
    final ElwhaTabs bar = ElwhaTabs.primary();
    bar.addTab(ElwhaTab.iconOnly(MaterialIcons.symbol("home"), "Home"));
    bar.addTab(ElwhaTab.iconOnly(MaterialIcons.symbol("favorite"), "Favorites"));
    bar.setSize(BAR_WIDTH, 48);
    bar.doLayout();
    final BufferedImage image = render(bar, 48);

    final ElwhaTab active = bar.getTabAt(0);
    final Rectangle icon = active.getIconBounds();
    check("active icon area carries PRIMARY pixels",
        regionHasColorNear(image, active.getX() + icon.x, icon.y, icon.width, icon.height,
            ColorRole.PRIMARY.resolve()));

    final ElwhaTab inactive = bar.getTabAt(1);
    final Rectangle icon2 = inactive.getIconBounds();
    check("inactive icon area carries ON_SURFACE_VARIANT pixels",
        regionHasColorNear(image, inactive.getX() + icon2.x, icon2.y, icon2.width, icon2.height,
            ColorRole.ON_SURFACE_VARIANT.resolve()));
  }

  private static void checkBadges() {
    final ElwhaTab iconTab = ElwhaTab.of(MaterialIcons.symbol("home"), "Inbox");
    final ElwhaBadge count = ElwhaBadge.large(88);
    iconTab.setBadge(count);
    check("badge round-trips on an icon tab", iconTab.getBadge() == count);
    check("IconBearing reports a 24px icon box",
        iconTab.getIconBounds().width == 24 && iconTab.getIconBounds().height == 24);

    final ElwhaBadge dot = ElwhaBadge.small();
    iconTab.setBadge(dot);
    check("re-badging swaps cleanly", iconTab.getBadge() == dot);
    iconTab.setBadge(null);
    check("null detaches", iconTab.getBadge() == null);

    final ElwhaTab labelTab = ElwhaTab.of("Updates");
    labelTab.setBadge(ElwhaBadge.small());
    check("label-only tab accepts a trailing-edge badge", labelTab.getBadge() != null);
    labelTab.setBadge(null);
    check("trailing-edge badge detaches", labelTab.getBadge() == null);
  }

  // ----------------------------------------------------------------- helpers

  private static BufferedImage render(final ElwhaTabs bar, final int height) {
    final BufferedImage image =
        new BufferedImage(BAR_WIDTH, height, BufferedImage.TYPE_INT_ARGB);
    final Graphics2D g = image.createGraphics();
    try {
      bar.paint(g);
    } finally {
      g.dispose();
    }
    return image;
  }

  private static boolean regionHasColorNear(
      final BufferedImage image, final int x, final int y, final int w, final int h,
      final Color c) {
    for (int yy = y; yy < y + h; yy++) {
      for (int xx = x; xx < x + w; xx++) {
        final Color px = new Color(image.getRGB(xx, yy), true);
        final int d =
            Math.abs(px.getRed() - c.getRed())
                + Math.abs(px.getGreen() - c.getGreen())
                + Math.abs(px.getBlue() - c.getBlue());
        if (d < 60) {
          return true;
        }
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
