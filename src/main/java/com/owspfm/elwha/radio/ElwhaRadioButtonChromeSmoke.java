package com.owspfm.elwha.radio;

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
 * Headless render guard for the S1 {@link ElwhaRadioButton} static chrome (story #417). Paints the
 * four static cells (unselected/selected &times; enabled/disabled) over a {@link ColorRole#SURFACE}
 * ground in light <em>and</em> dark mode and pixel-asserts the ring band, the dot, the ring hole,
 * and the disabled 0.38 blends — plus the preferred-size contract and the {@code ChangeListener}
 * change-only firing rule. Runs in CI's headless JVM ({@code -Djava.awt.headless=true} safe).
 *
 * @author Charles Bryan
 * @version v0.4.0
 * @since v0.4.0
 */
public final class ElwhaRadioButtonChromeSmoke {

  private static final int SIZE = ElwhaRadioButton.STATE_LAYER_SIZE_PX;
  private static final int CX = SIZE / 2;

  private ElwhaRadioButtonChromeSmoke() {}

  /**
   * Runs the guard. Exits non-zero on any failed assertion.
   *
   * @param args unused
   * @version v0.4.0
   * @since v0.4.0
   */
  public static void main(final String[] args) {
    System.setProperty("java.awt.headless", "true");

    checkApi();
    for (final Mode mode : new Mode[] {Mode.LIGHT, Mode.DARK}) {
      ElwhaTheme.install(ElwhaTheme.config().theme(MaterialPalettes.baseline()).mode(mode).build());
      checkMode(mode);
    }

    System.out.println("ElwhaRadioButtonChromeSmoke: OK (4 static cells, light + dark)");
  }

  private static void checkApi() {
    final ElwhaRadioButton radio = new ElwhaRadioButton();
    check("preferred size is 40x40", radio.getPreferredSize().equals(new Dimension(40, 40)));
    check("minimum size is 40x40", radio.getMinimumSize().equals(new Dimension(40, 40)));
    check("default is unselected", !radio.isSelected());
    check("convenience constructor selects", new ElwhaRadioButton(true).isSelected());

    final int[] fired = {0};
    radio.addChangeListener(e -> fired[0]++);
    radio.setSelected(true);
    check("ChangeListener fires on change", fired[0] == 1);
    radio.setSelected(true);
    check("ChangeListener silent on no-op write", fired[0] == 1);
    radio.setSelected(false);
    check("ChangeListener fires on deselect", fired[0] == 2);
  }

  private static void checkMode(final Mode mode) {
    final String tag = " [" + mode + "]";
    final Color surface = ColorRole.SURFACE.resolve();
    final Color ringRest = ColorRole.ON_SURFACE_VARIANT.resolve();
    final Color primary = ColorRole.PRIMARY.resolve();
    final Color disabledBlend =
        mix(surface, ColorRole.ON_SURFACE.resolve(), StateLayer.disabledContentOpacity());

    final BufferedImage unselected = render(new ElwhaRadioButton(), surface);
    check("unselected ring is ON_SURFACE_VARIANT" + tag, sampleRing(unselected, ringRest));
    check("unselected center is bare SURFACE" + tag, near(unselected.getRGB(CX, CX), surface));

    final BufferedImage selected = render(new ElwhaRadioButton(true), surface);
    check("selected ring is PRIMARY" + tag, sampleRing(selected, primary));
    check("selected dot is PRIMARY" + tag, near(selected.getRGB(CX, CX), primary));
    check("dot-to-ring gap stays SURFACE" + tag, near(selected.getRGB(13, CX), surface));

    final ElwhaRadioButton disabledOff = new ElwhaRadioButton();
    disabledOff.setEnabled(false);
    final BufferedImage disabledU = render(disabledOff, surface);
    check("disabled ring blends ON_SURFACE @ 0.38" + tag, sampleRing(disabledU, disabledBlend));
    check(
        "disabled unselected center stays SURFACE" + tag, near(disabledU.getRGB(CX, CX), surface));

    final ElwhaRadioButton disabledOn = new ElwhaRadioButton(true);
    disabledOn.setEnabled(false);
    final BufferedImage disabledS = render(disabledOn, surface);
    check("disabled selected ring blends @ 0.38" + tag, sampleRing(disabledS, disabledBlend));
    check("disabled dot blends @ 0.38" + tag, near(disabledS.getRGB(CX, CX), disabledBlend));
    check("disabled gap stays SURFACE" + tag, near(disabledS.getRGB(13, CX), surface));
  }

  private static BufferedImage render(final ElwhaRadioButton radio, final Color ground) {
    radio.setSize(SIZE, SIZE);
    final BufferedImage img = new BufferedImage(SIZE, SIZE, BufferedImage.TYPE_INT_ARGB);
    final Graphics2D g = img.createGraphics();
    try {
      g.setColor(ground);
      g.fillRect(0, 0, SIZE, SIZE);
      radio.paint(g);
    } finally {
      g.dispose();
    }
    return img;
  }

  /** Samples the ring band on the diagonal — pixel-center distance 9.19, clear of both AA edges. */
  private static boolean sampleRing(final BufferedImage img, final Color expected) {
    return near(img.getRGB(13, 13), expected);
  }

  private static boolean near(final int argb, final Color target) {
    final Color c = new Color(argb, true);
    return c.getAlpha() == 255
        && Math.abs(c.getRed() - target.getRed()) <= 8
        && Math.abs(c.getGreen() - target.getGreen()) <= 8
        && Math.abs(c.getBlue() - target.getBlue()) <= 8;
  }

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
