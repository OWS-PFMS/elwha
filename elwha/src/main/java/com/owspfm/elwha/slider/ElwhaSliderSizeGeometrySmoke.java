package com.owspfm.elwha.slider;

import com.owspfm.elwha.theme.ColorRole;
import com.owspfm.elwha.theme.ElwhaTheme;
import com.owspfm.elwha.theme.MaterialPalettes;
import com.owspfm.elwha.theme.Mode;
import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;

/**
 * Headless geometry guard for the Phase-4 / S1 {@link ElwhaSlider.Size} scale (story #369). Asserts
 * that (a) the handle-height ladder matches the M3 §M table exactly ({@code 44/44/52/68/108}), (b)
 * the track / handle geometry grows <em>monotonically</em> from {@code XS} to {@code XL} both in
 * the reported {@linkplain ElwhaSlider#getPreferredSize() preferred size} and in the
 * actually-painted track thickness, and (c) the {@code XS} preset is byte-for-byte unchanged from
 * the pre-Phase-4 constants ({@link ElwhaSlider#HANDLE_HEIGHT_PX} / {@link
 * ElwhaSlider#TRACK_HEIGHT_PX}). Runs in CI's headless JVM.
 *
 * @author Charles Bryan
 * @version v0.4.0
 * @since v0.4.0
 */
public final class ElwhaSliderSizeGeometrySmoke {

  private ElwhaSliderSizeGeometrySmoke() {}

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

    final ElwhaSlider.Size[] sizes = ElwhaSlider.Size.values();
    final int[] expectedHandle = {44, 44, 52, 68, 108};
    final int[] expectedTrack = {16, 24, 40, 56, 96};

    // (a) handle-height ladder == §M table, via the bubble-off preferred height (== handle height).
    for (int i = 0; i < sizes.length; i++) {
      final ElwhaSlider slider = new ElwhaSlider(0, 100, 60);
      slider.setSizeVariant(sizes[i]);
      final int prefH = slider.getPreferredSize().height;
      check(
          sizes[i] + " preferred height == handle height " + expectedHandle[i],
          prefH == expectedHandle[i]);
    }

    // (b) monotonic growth in preferred height + painted track thickness; (c) XS unchanged.
    int prevPref = -1;
    int prevTrack = -1;
    for (int i = 0; i < sizes.length; i++) {
      final ElwhaSlider slider = new ElwhaSlider(0, 100, 60);
      slider.setSizeVariant(sizes[i]);
      final int prefH = slider.getPreferredSize().height;
      slider.setSize(240, prefH);

      final BufferedImage img = new BufferedImage(240, prefH, BufferedImage.TYPE_INT_ARGB);
      final Graphics2D g = img.createGraphics();
      try {
        slider.paint(g);
      } finally {
        g.dispose();
      }

      final int track = activeTrackThickness(img, prefH);
      check(sizes[i] + " preferred height grows monotonically", prefH >= prevPref);
      check(sizes[i] + " painted track thickness grows monotonically", track >= prevTrack);
      check(
          sizes[i] + " painted track ~= §M track height " + expectedTrack[i],
          Math.abs(track - expectedTrack[i]) <= 3);
      prevPref = prefH;
      prevTrack = track;
    }

    // (c) XS byte-identical to the pre-Phase-4 constants.
    final ElwhaSlider xs = new ElwhaSlider(0, 100, 60);
    xs.setSizeVariant(ElwhaSlider.Size.XS);
    check(
        "XS preferred height == HANDLE_HEIGHT_PX (" + ElwhaSlider.HANDLE_HEIGHT_PX + ")",
        xs.getPreferredSize().height == ElwhaSlider.HANDLE_HEIGHT_PX);
    check("default size is XS", new ElwhaSlider().getSizeVariant() == ElwhaSlider.Size.XS);

    System.out.println(
        "ElwhaSliderSizeGeometrySmoke: OK (ladder + monotonic growth + XS unchanged)");
  }

  /**
   * Counts the vertical run of active (PRIMARY) track pixels at a column sampled past the largest
   * outer-corner radius (XL = 28) yet left of the handle gap, so the full track band is measured.
   */
  private static int activeTrackThickness(final BufferedImage img, final int h) {
    final Color primary = ColorRole.PRIMARY.resolve();
    final int x = 48;
    int count = 0;
    for (int y = 0; y < h; y++) {
      if (nearColor(img.getRGB(x, y), primary)) {
        count++;
      }
    }
    return count;
  }

  private static boolean nearColor(final int argb, final Color target) {
    final Color c = new Color(argb, true);
    if (c.getAlpha() < 200) {
      return false;
    }
    return Math.abs(c.getRed() - target.getRed()) <= 6
        && Math.abs(c.getGreen() - target.getGreen()) <= 6
        && Math.abs(c.getBlue() - target.getBlue()) <= 6;
  }

  private static void check(final String what, final boolean ok) {
    if (!ok) {
      System.err.println("FAIL: " + what);
      System.exit(1);
    }
    System.out.println("  ok: " + what);
  }
}
