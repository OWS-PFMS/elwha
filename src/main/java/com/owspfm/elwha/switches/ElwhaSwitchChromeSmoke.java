package com.owspfm.elwha.switches;

import com.owspfm.elwha.theme.ColorRole;
import com.owspfm.elwha.theme.ElwhaTheme;
import com.owspfm.elwha.theme.MaterialPalettes;
import com.owspfm.elwha.theme.Mode;
import com.owspfm.elwha.theme.StateLayer;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;

/**
 * Headless S1 guard (story #402) — renders the four static {@link ElwhaSwitch} state cells into a
 * {@link BufferedImage} over a {@link ColorRole#SURFACE} ground and pixel-asserts the research §T
 * role mapping in light <em>and</em> dark mode (proving the paint-time token resolve), plus the
 * selection API + {@code ChangeListener} contract and the 60&times;40 state-layer-inclusive
 * preferred size.
 *
 * @author Charles Bryan
 * @version v0.4.0
 * @since v0.4.0
 */
public final class ElwhaSwitchChromeSmoke {

  private static final int W = 60;
  private static final int H = 40;
  private static final int CY = 20;

  private ElwhaSwitchChromeSmoke() {}

  /**
   * Runs the smoke checks; exits non-zero on the first failure.
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
    checkChrome("light");

    ElwhaTheme.install(ElwhaTheme.current().withMode(Mode.DARK));
    checkChrome("dark");

    System.out.println(
        "ElwhaSwitchChromeSmoke: OK (selection API + ChangeListener contract + preferred size +"
            + " light/dark §T chrome pixels)");
  }

  private static void checkApi() {
    final ElwhaSwitch api = new ElwhaSwitch();
    check("default is unselected", !api.isSelected());
    check("convenience constructor seeds selected", new ElwhaSwitch(true).isSelected());
    check(
        "preferred size is the 60x40 halo box", api.getPreferredSize().equals(new Dimension(W, H)));
    check("minimum size matches preferred", api.getMinimumSize().equals(new Dimension(W, H)));

    final int[] fired = {0};
    api.addChangeListener(e -> fired[0]++);
    api.setSelected(true);
    check("setSelected(true) fires ChangeListener once", fired[0] == 1);
    api.setSelected(true);
    check("same-value setSelected does not fire", fired[0] == 1);
    api.setSelected(false);
    check("setSelected(false) fires again", fired[0] == 2);
  }

  private static void checkChrome(final String modeTag) {
    // Unselected enabled: scH interior, OUTLINE ring at the left cap, OUTLINE handle at cx=20.
    final BufferedImage off = render(switchAt(false, true));
    check(
        modeTag + ": unselected track is SURFACE_CONTAINER_HIGHEST",
        near(off, 44, CY, ColorRole.SURFACE_CONTAINER_HIGHEST.resolve()));
    check(
        modeTag + ": unselected outline ring is OUTLINE",
        near(off, 5, CY, ColorRole.OUTLINE.resolve()));
    check(
        modeTag + ": unselected handle is OUTLINE", near(off, 20, CY, ColorRole.OUTLINE.resolve()));

    // Selected enabled: PRIMARY track, ON_PRIMARY handle at cx=40, no outline ring.
    final BufferedImage on = render(switchAt(true, true));
    check(modeTag + ": selected track is PRIMARY", near(on, 10, CY, ColorRole.PRIMARY.resolve()));
    check(
        modeTag + ": selected cap has no outline ring",
        near(on, 5, CY, ColorRole.PRIMARY.resolve()));
    check(
        modeTag + ": selected handle is ON_PRIMARY",
        near(on, 40, CY, ColorRole.ON_PRIMARY.resolve()));

    // Disabled unselected: 12% scH track + 38% ON_SURFACE handle, blended over SURFACE.
    final Color ground = ColorRole.SURFACE.resolve();
    final float container = StateLayer.disabledContainerOpacity();
    final float content = StateLayer.disabledContentOpacity();
    final BufferedImage disabledOff = render(switchAt(false, false));
    final Color disabledOffTrack =
        mix(ground, ColorRole.SURFACE_CONTAINER_HIGHEST.resolve(), container);
    check(
        modeTag + ": disabled unselected track is scH @ 0.12 over SURFACE",
        near(disabledOff, 44, CY, disabledOffTrack));
    check(
        modeTag + ": disabled unselected handle is ON_SURFACE @ 0.38",
        near(disabledOff, 20, CY, mix(disabledOffTrack, ColorRole.ON_SURFACE.resolve(), content)));

    // Disabled selected: 12% ON_SURFACE track, opaque SURFACE handle (the §T asymmetry).
    final BufferedImage disabledOn = render(switchAt(true, false));
    check(
        modeTag + ": disabled selected track is ON_SURFACE @ 0.12 over SURFACE",
        near(disabledOn, 10, CY, mix(ground, ColorRole.ON_SURFACE.resolve(), container)));
    check(
        modeTag + ": disabled selected handle is opaque SURFACE",
        near(disabledOn, 40, CY, ColorRole.SURFACE.resolve()));
  }

  private static ElwhaSwitch switchAt(final boolean selected, final boolean enabled) {
    final ElwhaSwitch s = new ElwhaSwitch(selected);
    s.setEnabled(enabled);
    return s;
  }

  private static BufferedImage render(final ElwhaSwitch s) {
    s.setSize(W, H);
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
    final Color got = new Color(img.getRGB(x, y), true);
    return Math.abs(got.getRed() - want.getRed()) <= 10
        && Math.abs(got.getGreen() - want.getGreen()) <= 10
        && Math.abs(got.getBlue() - want.getBlue()) <= 10;
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
