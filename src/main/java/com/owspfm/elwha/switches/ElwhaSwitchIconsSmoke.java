package com.owspfm.elwha.switches;

import com.owspfm.elwha.icons.MaterialIcons;
import com.owspfm.elwha.theme.ColorRole;
import com.owspfm.elwha.theme.ElwhaTheme;
import com.owspfm.elwha.theme.MaterialPalettes;
import com.owspfm.elwha.theme.Mode;
import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import javax.swing.Icon;

/**
 * Headless S4 guard (story #405) — asserts the icons contract: default glyph slots are non-null and
 * {@code null} restores them, both-icons mode grows the <em>unselected</em> handle to 24px while
 * selected-icon-only mode drops it back to 16px with no glyph, glyph pixels render in the §T roles
 * ({@code ON_PRIMARY_CONTAINER} on the selected handle, {@code SURFACE_CONTAINER_HIGHEST} on the
 * unselected one), and the disabled selected glyph blends {@code ON_SURFACE} @ 0.38 over the opaque
 * {@code SURFACE} handle. Glyph probes scan the handle's central disc rather than pinning exact
 * stroke pixels.
 *
 * @author Charles Bryan
 * @version v0.4.0
 * @since v0.4.0
 */
public final class ElwhaSwitchIconsSmoke {

  private static final int W = 60;
  private static final int H = 40;
  private static final int CY = 20;

  private ElwhaSwitchIconsSmoke() {}

  /**
   * Runs the guard. Exits non-zero on any failed assertion.
   *
   * @param args unused
   * @version v0.4.0
   * @since v0.4.0
   */
  public static void main(final String[] args) {
    System.setProperty("java.awt.headless", "true");
    ElwhaTheme.install(
        ElwhaTheme.config().theme(MaterialPalettes.baseline()).mode(Mode.LIGHT).build());

    checkApi();
    checkHandleSizing();
    checkGlyphPixels();
    checkDisabledGlyphs();

    System.out.println(
        "ElwhaSwitchIconsSmoke: OK (icon slots + with-icon handle sizing + glyph roles + disabled"
            + " glyph treatment)");
  }

  private static void checkApi() {
    final ElwhaSwitch s = new ElwhaSwitch();
    check("icons are hidden by default", !s.isIconsVisible() && !s.isShowOnlySelectedIcon());
    check(
        "default glyph slots are non-null",
        s.getSelectedIcon() != null && s.getUnselectedIcon() != null);

    final Icon custom = MaterialIcons.starFilled(ElwhaSwitch.ICON_SIZE_PX);
    s.setSelectedIcon(custom);
    check("a custom selected icon sticks", s.getSelectedIcon() == custom);
    s.setSelectedIcon(null);
    check(
        "null restores the default selected icon",
        s.getSelectedIcon() != null && s.getSelectedIcon() != custom);
  }

  private static void checkHandleSizing() {
    // Unselected handle d=16 spans x [12,28]; with icons it rides at 24 and spans [8,32].
    final ElwhaSwitch s = sized(new ElwhaSwitch());
    check(
        "no icons: x=9 is track interior (16px handle)",
        near(render(s), 9, CY, ColorRole.SURFACE_CONTAINER_HIGHEST.resolve()));
    s.setIconsVisible(true);
    check(
        "icons visible: the unselected handle rides at 24px (x=9 is handle)",
        near(render(s), 9, CY, ColorRole.OUTLINE.resolve()));
    s.setShowOnlySelectedIcon(true);
    check(
        "show-only-selected wins: the unselected handle returns to 16px",
        near(render(s), 9, CY, ColorRole.SURFACE_CONTAINER_HIGHEST.resolve()));
  }

  private static void checkGlyphPixels() {
    final ElwhaSwitch on = sized(new ElwhaSwitch(true));
    on.setIconsVisible(true);
    check(
        "the selected handle shows an ON_PRIMARY_CONTAINER glyph",
        anyNear(render(on), 40, CY, 6, ColorRole.ON_PRIMARY_CONTAINER.resolve()));

    final ElwhaSwitch off = sized(new ElwhaSwitch());
    off.setIconsVisible(true);
    check(
        "the unselected handle shows a SURFACE_CONTAINER_HIGHEST glyph",
        anyNear(render(off), 20, CY, 6, ColorRole.SURFACE_CONTAINER_HIGHEST.resolve()));

    final ElwhaSwitch selectedOnlyOff = sized(new ElwhaSwitch());
    selectedOnlyOff.setShowOnlySelectedIcon(true);
    check(
        "selected-icon-only: the unselected handle has no glyph",
        !anyNear(
            render(selectedOnlyOff), 20, CY, 6, ColorRole.SURFACE_CONTAINER_HIGHEST.resolve()));
  }

  private static void checkDisabledGlyphs() {
    final ElwhaSwitch s = sized(new ElwhaSwitch(true));
    s.setIconsVisible(true);
    s.setEnabled(false);
    final Color expected = mix(ColorRole.SURFACE.resolve(), ColorRole.ON_SURFACE.resolve(), 0.38f);
    check(
        "disabled selected glyph is ON_SURFACE @ 0.38 over the opaque SURFACE handle",
        anyNear(render(s), 40, CY, 6, expected));
  }

  private static ElwhaSwitch sized(final ElwhaSwitch s) {
    s.setSize(W, H);
    return s;
  }

  private static BufferedImage render(final ElwhaSwitch s) {
    final BufferedImage img = new BufferedImage(W, H, BufferedImage.TYPE_INT_ARGB);
    final Graphics2D g = img.createGraphics();
    try {
      g.setColor(ColorRole.SURFACE.resolve());
      g.fillRect(0, 0, W, H);
      s.paint(g);
    } finally {
      g.dispose();
    }
    return img;
  }

  private static boolean near(final BufferedImage img, final int x, final int y, final Color want) {
    return nearColor(new Color(img.getRGB(x, y), true), want, 10);
  }

  /**
   * Whether any pixel within the <em>disc</em> of {@code radius} around {@code (cx, cy)} is close
   * to {@code want} — circular so the scan never pokes past a 16px handle onto the track.
   */
  private static boolean anyNear(
      final BufferedImage img, final int cx, final int cy, final int radius, final Color want) {
    for (int y = cy - radius; y <= cy + radius; y++) {
      for (int x = cx - radius; x <= cx + radius; x++) {
        final int dx = x - cx;
        final int dy = y - cy;
        if (dx * dx + dy * dy > radius * radius) {
          continue;
        }
        if (nearColor(new Color(img.getRGB(x, y), true), want, 12)) {
          return true;
        }
      }
    }
    return false;
  }

  private static boolean nearColor(final Color got, final Color want, final int tolerance) {
    return Math.abs(got.getRed() - want.getRed()) <= tolerance
        && Math.abs(got.getGreen() - want.getGreen()) <= tolerance
        && Math.abs(got.getBlue() - want.getBlue()) <= tolerance;
  }

  /** Source-over blend of {@code over} at {@code alpha} onto an opaque {@code base}. */
  private static Color mix(final Color base, final Color over, final float alpha) {
    return new Color(
        Math.round(base.getRed() + (over.getRed() - base.getRed()) * alpha),
        Math.round(base.getGreen() + (over.getGreen() - base.getGreen()) * alpha),
        Math.round(base.getBlue() + (over.getBlue() - base.getBlue()) * alpha));
  }

  private static void check(final String what, final boolean ok) {
    if (!ok) {
      System.err.println("FAIL: " + what);
      System.exit(1);
    }
    System.out.println("  ok: " + what);
  }
}
