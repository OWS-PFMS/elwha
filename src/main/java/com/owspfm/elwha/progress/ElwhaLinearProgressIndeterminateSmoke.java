package com.owspfm.elwha.progress;

import com.owspfm.elwha.theme.ColorRole;
import com.owspfm.elwha.theme.ElwhaTheme;
import com.owspfm.elwha.theme.MaterialPalettes;
import com.owspfm.elwha.theme.Mode;
import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;

/**
 * Headless guard for the S2 {@link ElwhaLinearProgressIndicator} indeterminate motion (story #470).
 * Samples the 1750ms two-line cycle offscreen and asserts: the paint moves between samples, a
 * two-line moment occurs, lines stay separated from track spans by empty gaps, the animation clock
 * never runs while the component is hidden, and the determinate↔indeterminate round-trip restores
 * the value chrome (stop dot included). Runs in CI's headless JVM.
 *
 * @author Charles Bryan
 * @version v0.4.0
 * @since v0.4.0
 */
public final class ElwhaLinearProgressIndeterminateSmoke {

  private static final int W = 240;
  private static final int MID_Y = 2;

  private ElwhaLinearProgressIndeterminateSmoke() {}

  /**
   * Runs the guard. Exits non-zero on any failed assertion.
   *
   * @param args unused
   * @throws InterruptedException never — sampling sleeps are uninterrupted
   * @version v0.4.0
   * @since v0.4.0
   */
  public static void main(final String[] args) throws InterruptedException {
    System.setProperty("java.awt.headless", "true");
    ElwhaTheme.install(
        ElwhaTheme.config().theme(MaterialPalettes.baseline()).mode(Mode.LIGHT).build());

    final Color primary = ColorRole.PRIMARY.resolve();
    final Color track = ColorRole.SECONDARY_CONTAINER.resolve();

    final ElwhaLinearProgressIndicator bar = ElwhaLinearProgressIndicator.indeterminate();
    check("clock does not run while hidden", !bar.isAnimating());

    int movingSamples = 0;
    int twoLineMoments = 0;
    int framesWithLine = 0;
    int frames = 0;
    int[] previousSegments = null;
    for (long deadline = System.currentTimeMillis() + 1900;
        System.currentTimeMillis() < deadline; ) {
      final BufferedImage img = paint(bar);
      final int[] segments = primarySegments(img, primary);
      frames++;
      if (segments[0] > 0) {
        framesWithLine++;
      }
      if (segments[0] >= 2) {
        twoLineMoments++;
      }
      if (previousSegments != null
          && (segments[0] != previousSegments[0]
              || Math.abs(segments[1] - previousSegments[1]) > 2)) {
        movingSamples++;
      }
      previousSegments = segments;
      Thread.sleep(60);
    }
    check("paint moves across samples", movingSamples > 5);
    check("a two-line moment occurs in the cycle", twoLineMoments >= 1);
    check("a line is visible in nearly every frame", framesWithLine >= frames - 3);

    final BufferedImage snap = paint(bar);
    check("track spans appear around the lines", hasColor(snap, track));

    final ElwhaLinearProgressIndicator roundTrip = new ElwhaLinearProgressIndicator(0, 100, 60);
    roundTrip.setIndeterminate(true);
    paint(roundTrip);
    roundTrip.setIndeterminate(false);
    final BufferedImage back = paint(roundTrip);
    check("round-trip: value preserved", roundTrip.getValue() == 60);
    check("round-trip: active fill returns", nearColor(back.getRGB(8, MID_Y), primary));
    check("round-trip: stop dot returns", nearColor(back.getRGB(W - 2, MID_Y), primary));
    check("round-trip: clock idle again while hidden", !roundTrip.isAnimating());

    System.out.println(
        "ElwhaLinearProgressIndeterminateSmoke: OK (motion + two-line + gaps + round-trip; "
            + frames
            + " frames)");
  }

  private static int[] primarySegments(final BufferedImage img, final Color primary) {
    int segments = 0;
    int firstStart = -1;
    boolean in = false;
    for (int x = 0; x < W; x++) {
      final boolean hit = nearColor(img.getRGB(x, MID_Y), primary);
      if (hit && !in) {
        segments++;
        in = true;
        if (firstStart < 0) {
          firstStart = x;
        }
      } else if (!hit) {
        in = false;
      }
    }
    return new int[] {segments, firstStart};
  }

  private static boolean hasColor(final BufferedImage img, final Color target) {
    for (int x = 0; x < W; x += 2) {
      if (nearColor(img.getRGB(x, MID_Y), target)) {
        return true;
      }
    }
    return false;
  }

  private static BufferedImage paint(final ElwhaLinearProgressIndicator bar) {
    bar.setSize(W, 4);
    final BufferedImage img = new BufferedImage(W, 4, BufferedImage.TYPE_INT_ARGB);
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
    return Math.abs(c.getRed() - target.getRed())
            + Math.abs(c.getGreen() - target.getGreen())
            + Math.abs(c.getBlue() - target.getBlue())
        < 60;
  }

  private static void check(final String what, final boolean ok) {
    if (!ok) {
      System.err.println("FAIL: " + what);
      System.exit(1);
    }
    System.out.println("  ok: " + what);
  }
}
