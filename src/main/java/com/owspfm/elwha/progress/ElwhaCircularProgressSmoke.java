package com.owspfm.elwha.progress;

import com.owspfm.elwha.theme.ColorRole;
import com.owspfm.elwha.theme.ElwhaTheme;
import com.owspfm.elwha.theme.MaterialPalettes;
import com.owspfm.elwha.theme.Mode;
import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;

/**
 * Headless guard for the S4 {@link ElwhaCircularProgressIndicator} ring (story #472). Walks the
 * stroke-radius circle classifying pixels and asserts the determinate anatomy — active arc
 * clockwise from 12 o'clock, track elsewhere, exactly two empty gaps between them, seamless full
 * rings at 0%/100% — plus the redline diameters (40/44 and the indicatorSize knob), and the
 * indeterminate choreography (arc present, moving, no track). Runs in CI's headless JVM.
 *
 * @author Charles Bryan
 * @version v0.4.0
 * @since v0.4.0
 */
public final class ElwhaCircularProgressSmoke {

  private ElwhaCircularProgressSmoke() {}

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

    final ElwhaCircularProgressIndicator half = new ElwhaCircularProgressIndicator(0, 100, 50);
    final BufferedImage img = paint(half);
    final char[] ring = ringClasses(img, primary, track);
    check("50%: active (PRIMARY) on the right (3 o'clock)", ring[0] == 'p');
    check("50%: track on the left (9 o'clock)", ring[180] == 't');
    check("50%: exactly two gaps separate active and track", emptyRuns(ring) == 2);
    check(
        "50%: active occupies roughly half the ring",
        countOf(ring, 'p') > 130 && countOf(ring, 'p') < 190);

    final ElwhaCircularProgressIndicator zero = new ElwhaCircularProgressIndicator(0, 100, 0);
    final char[] zeroRing = ringClasses(paint(zero), primary, track);
    check(
        "0%: full seamless track ring",
        countOf(zeroRing, 't') >= 355 && countOf(zeroRing, 'p') == 0);

    final ElwhaCircularProgressIndicator full = new ElwhaCircularProgressIndicator(0, 100, 100);
    final char[] fullRing = ringClasses(paint(full), primary, track);
    check(
        "100%: full seamless active ring",
        countOf(fullRing, 'p') >= 355 && countOf(fullRing, 't') == 0);

    final ElwhaCircularProgressIndicator base = new ElwhaCircularProgressIndicator();
    check("flat default diameter = 40", base.getPreferredSize().width == 40);
    base.setTrackThickness(8);
    check("thick 8px diameter = 44", base.getPreferredSize().width == 44);
    base.setTrackThickness(4);
    base.setIndicatorSize(48);
    check("indicatorSize knob drives the base diameter", base.getPreferredSize().width == 48);

    final ElwhaCircularProgressIndicator indet = ElwhaCircularProgressIndicator.indeterminate();
    check("clock does not run while hidden", !indet.isAnimating());
    int moving = 0;
    int arcFrames = 0;
    int trackFrames = 0;
    Integer previousStart = null;
    for (int i = 0; i < 14; i++) {
      final char[] frame = ringClasses(paint(indet), primary, track);
      final int start = firstIndexOf(frame, 'p');
      if (start >= 0) {
        arcFrames++;
        if (previousStart != null && angularDistance(start, previousStart) > 2) {
          moving++;
        }
        previousStart = start;
      }
      if (countOf(frame, 't') > 0) {
        trackFrames++;
      }
      Thread.sleep(90);
    }
    check("indeterminate: arc present every frame", arcFrames == 14);
    check("indeterminate: arc moves between frames", moving > 8);
    check("indeterminate: no track painted", trackFrames == 0);

    final ElwhaCircularProgressIndicator roundTrip = new ElwhaCircularProgressIndicator(0, 100, 50);
    roundTrip.setIndeterminate(true);
    paint(roundTrip);
    roundTrip.setIndeterminate(false);
    final char[] backRing = ringClasses(paint(roundTrip), primary, track);
    check("round-trip: determinate anatomy returns", backRing[0] == 'p' && backRing[180] == 't');

    System.out.println("ElwhaCircularProgressSmoke: OK (anatomy + gaps + diameters + motion)");
  }

  private static BufferedImage paint(final ElwhaCircularProgressIndicator ring) {
    final int d = ring.getDiameter();
    ring.setSize(d, d);
    final BufferedImage img = new BufferedImage(d, d, BufferedImage.TYPE_INT_ARGB);
    final Graphics2D g = img.createGraphics();
    try {
      ring.paint(g);
    } finally {
      g.dispose();
    }
    return img;
  }

  /**
   * Classifies the stroke-center circle at every whole degree (0 = 3 o'clock, counterclockwise like
   * {@link java.awt.geom.Arc2D}): {@code 'p'} primary, {@code 't'} track, {@code '.'} empty.
   */
  private static char[] ringClasses(
      final BufferedImage img, final Color primary, final Color track) {
    final float c = img.getWidth() / 2f;
    final float radius = (img.getWidth() - 4) / 2f;
    final char[] classes = new char[360];
    for (int a = 0; a < 360; a++) {
      final double rad = Math.toRadians(a);
      final int x = Math.round(c + (float) (radius * Math.cos(rad)));
      final int y = Math.round(c - (float) (radius * Math.sin(rad)));
      final int argb =
          img.getRGB(
              Math.min(img.getWidth() - 1, Math.max(0, x)),
              Math.min(img.getHeight() - 1, Math.max(0, y)));
      if (nearColor(argb, primary)) {
        classes[a] = 'p';
      } else if (nearColor(argb, track)) {
        classes[a] = 't';
      } else {
        classes[a] = '.';
      }
    }
    return classes;
  }

  private static int emptyRuns(final char[] ring) {
    int runs = 0;
    for (int a = 0; a < 360; a++) {
      final boolean empty = ring[a] == '.';
      final boolean prevEmpty = ring[(a + 359) % 360] == '.';
      if (empty && !prevEmpty) {
        runs++;
      }
    }
    return runs;
  }

  private static int countOf(final char[] ring, final char cls) {
    int count = 0;
    for (final char c : ring) {
      if (c == cls) {
        count++;
      }
    }
    return count;
  }

  private static int firstIndexOf(final char[] ring, final char cls) {
    for (int a = 0; a < 360; a++) {
      if (ring[a] == cls) {
        return a;
      }
    }
    return -1;
  }

  private static int angularDistance(final int a, final int b) {
    final int diff = Math.abs(a - b) % 360;
    return Math.min(diff, 360 - diff);
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
