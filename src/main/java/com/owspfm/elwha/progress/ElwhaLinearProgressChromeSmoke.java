package com.owspfm.elwha.progress;

import com.owspfm.elwha.theme.ColorRole;
import com.owspfm.elwha.theme.ElwhaTheme;
import com.owspfm.elwha.theme.MaterialPalettes;
import com.owspfm.elwha.theme.Mode;
import java.awt.Color;
import java.awt.ComponentOrientation;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;

/**
 * Headless render guard for the S1 {@link ElwhaLinearProgressIndicator} chrome (story #469).
 * Paints determinate bars offscreen and asserts the updated-M3 anatomy: active ({@code PRIMARY})
 * on the leading side, track ({@code SECONDARY_CONTAINER}) on the trailing side, an empty
 * track-active gap, the trailing {@code PRIMARY} stop dot, its hide-on-arrival at 100%, and RTL
 * mirroring. Runs in CI's headless JVM.
 *
 * @author Charles Bryan
 * @version v0.4.0
 * @since v0.4.0
 */
public final class ElwhaLinearProgressChromeSmoke {

  private static final int W = 240;

  private ElwhaLinearProgressChromeSmoke() {}

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

    final Color primary = ColorRole.PRIMARY.resolve();
    final Color track = ColorRole.SECONDARY_CONTAINER.resolve();

    final ElwhaLinearProgressIndicator bar = new ElwhaLinearProgressIndicator(0, 100, 60);
    final BufferedImage img = paint(bar, 4);
    final int midY = 2;

    check("active (PRIMARY) on the leading side", nearColor(img.getRGB(8, midY), primary));
    check("track (SECONDARY_CONTAINER) on the trailing side", nearColor(img.getRGB(200, midY), track));
    final int head = (int) (0.60f * W);
    check("track-active gap is empty", isClear(img.getRGB(head + 2, midY)));
    check("stop dot (PRIMARY) at the trailing end", nearColor(img.getRGB(W - 2, midY), primary));

    final ElwhaLinearProgressIndicator full = new ElwhaLinearProgressIndicator(0, 100, 100);
    final BufferedImage fullImg = paint(full, 4);
    check("100%: bar is all active at the trailing end", nearColor(fullImg.getRGB(W - 8, midY), primary));
    boolean anyTrack = false;
    for (int x = 0; x < W; x += 2) {
      anyTrack |= nearColor(fullImg.getRGB(x, midY), track);
    }
    check("100%: no track remains", !anyTrack);

    final ElwhaLinearProgressIndicator zero = new ElwhaLinearProgressIndicator(0, 100, 0);
    final BufferedImage zeroImg = paint(zero, 4);
    check("0%: track spans the leading side", nearColor(zeroImg.getRGB(8, midY), track));
    boolean anyActiveRun = false;
    for (int x = 0; x < W - 6; x += 2) {
      anyActiveRun |= nearColor(zeroImg.getRGB(x, midY), primary);
    }
    check("0%: no active indicator before the stop dot", !anyActiveRun);

    final ElwhaLinearProgressIndicator rtl = new ElwhaLinearProgressIndicator(0, 100, 60);
    rtl.setComponentOrientation(ComponentOrientation.RIGHT_TO_LEFT);
    final BufferedImage rtlImg = paint(rtl, 4);
    check("RTL: active (PRIMARY) on the right", nearColor(rtlImg.getRGB(W - 8, midY), primary));
    check("RTL: track on the left", nearColor(rtlImg.getRGB(40, midY), track));
    check("RTL: stop dot at the visual left end", nearColor(rtlImg.getRGB(2, midY), primary));

    final ElwhaLinearProgressIndicator thick = new ElwhaLinearProgressIndicator(0, 100, 60);
    thick.setTrackThickness(8);
    final BufferedImage thickImg = paint(thick, 8);
    check("thick 8px: active paints at full thickness", nearColor(thickImg.getRGB(8, 1), primary));
    check(
        "thick 8px: preferred height follows thickness",
        thick.getPreferredSize().height == 8);

    check("fraction clamps on a degenerate range", degenerateFractionIsZero());

    System.out.println("ElwhaLinearProgressChromeSmoke: OK (anatomy + edges + RTL + thick)");
  }

  private static boolean degenerateFractionIsZero() {
    final ElwhaLinearProgressIndicator degenerate = new ElwhaLinearProgressIndicator(5, 5, 5);
    return degenerate.getProgressFraction() == 0f;
  }

  private static BufferedImage paint(final ElwhaLinearProgressIndicator bar, final int height) {
    bar.setSize(W, height);
    final BufferedImage img = new BufferedImage(W, height, BufferedImage.TYPE_INT_ARGB);
    final Graphics2D g = img.createGraphics();
    try {
      bar.paint(g);
    } finally {
      g.dispose();
    }
    return img;
  }

  private static boolean nearColor(final int argb, final Color target) {
    final Color c = new Color(argb, true);
    if (c.getAlpha() < 200) {
      return false;
    }
    final int dr = Math.abs(c.getRed() - target.getRed());
    final int dg = Math.abs(c.getGreen() - target.getGreen());
    final int db = Math.abs(c.getBlue() - target.getBlue());
    return dr + dg + db < 60;
  }

  private static boolean isClear(final int argb) {
    return new Color(argb, true).getAlpha() < 60;
  }

  private static void check(final String what, final boolean ok) {
    if (!ok) {
      System.err.println("FAIL: " + what);
      System.exit(1);
    }
    System.out.println("  ok: " + what);
  }
}
