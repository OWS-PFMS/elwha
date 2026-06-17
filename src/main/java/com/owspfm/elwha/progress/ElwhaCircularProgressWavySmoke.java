package com.owspfm.elwha.progress;

import com.owspfm.elwha.theme.ColorRole;
import com.owspfm.elwha.theme.ElwhaTheme;
import com.owspfm.elwha.theme.MaterialPalettes;
import com.owspfm.elwha.theme.Mode;
import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;

/**
 * Headless guard for the S5 {@link ElwhaCircularProgressIndicator} wavy shape (story #473).
 * Measures the active arc's stroke-centroid radial profile around the ring and asserts: the wavy
 * arc scallops (crest-to-trough radial variance ≥ 2px) while a flat ring stays even, the track
 * behind a wavy arc stays flat, the ≥95% amplitude ramp flattens, the 100% closed wavy ring has
 * full coverage with scallops, the wavy diameters follow the redlines (48/52), and the wavy
 * indeterminate arc spins. Runs in CI's headless JVM.
 *
 * @author Charles Bryan
 * @version v0.4.0
 * @since v0.4.0
 */
public final class ElwhaCircularProgressWavySmoke {

  private ElwhaCircularProgressWavySmoke() {}

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

    final ElwhaCircularProgressIndicator wavy = ElwhaCircularProgressIndicator.wavy();
    wavy.setValue(50);
    check("wavy diameter = 48", wavy.getPreferredSize().width == 48);
    final BufferedImage img = paint(wavy);
    check("wavy active arc scallops (≥2px radial variance)", outerVariance(img, primary) >= 2f);
    check(
        "track behind the wavy arc stays flat (<1.75px centroid)",
        outerVariance(img, track) < 1.75f);

    final ElwhaCircularProgressIndicator flat = new ElwhaCircularProgressIndicator(0, 100, 50);
    check(
        "flat active arc stays even (<1.75px centroid)",
        outerVariance(paint(flat), primary) < 1.75f);

    final ElwhaCircularProgressIndicator high = ElwhaCircularProgressIndicator.wavy();
    high.setValue(97);
    check("ramp: ≥95% paints flat", outerVarianceInWindow(paint(high), primary, 135, 405) < 1.75f);

    final ElwhaCircularProgressIndicator closed = ElwhaCircularProgressIndicator.wavy();
    closed.setValue(100);
    final BufferedImage closedImg = paint(closed);
    check(
        "100%: the ramp settles the closed ring flat (spec: amplitude 0 at \u226595%)",
        outerVariance(closedImg, primary) < 1.75f);
    check("100%: closed ring covers every angle", coverage(closedImg, primary) >= 355);

    final ElwhaCircularProgressIndicator thick = ElwhaCircularProgressIndicator.wavy();
    thick.setTrackThickness(8);
    check("wavy thick diameter = 52", thick.getPreferredSize().width == 52);

    final ElwhaCircularProgressIndicator indet = ElwhaCircularProgressIndicator.wavyIndeterminate();
    check("wavy indeterminate diameter = 48", indet.getPreferredSize().width == 48);
    boolean scalloped = false;
    int moving = 0;
    Integer previousCoverage = null;
    for (int i = 0; i < 12; i++) {
      final BufferedImage frame = paint(indet);
      scalloped |= outerVariance(frame, primary) >= 2f;
      final int cov = coverage(frame, primary);
      if (previousCoverage != null && Math.abs(cov - previousCoverage) > 2) {
        moving++;
      }
      previousCoverage = cov;
      Thread.sleep(90);
    }
    check("wavy indeterminate scallops within the cycle", scalloped);
    check("wavy indeterminate arc moves/breathes", moving > 4);

    System.out.println("ElwhaCircularProgressWavySmoke: OK (scallop + flat track + ramp + ring)");
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
   * Crest-to-trough spread of the color's stroke-centroid radius — measured only at angles ≥5°
   * inside the arc's angular ends, so the round-cap taper doesn't read as wave variance.
   */
  private static float outerVariance(final BufferedImage img, final Color target) {
    final boolean[] present = new boolean[360];
    for (int a = 0; a < 360; a++) {
      present[a] = outerExtent(img, target, a) > 0f;
    }
    float min = Float.MAX_VALUE;
    float max = -1f;
    boolean seen = false;
    for (int a = 0; a < 360; a += 2) {
      boolean interior = true;
      for (int d = -16; d <= 16 && interior; d++) {
        interior = present[(a + d + 360) % 360];
      }
      if (!interior) {
        continue;
      }
      final float r = outerExtent(img, target, a);
      min = Math.min(min, r);
      max = Math.max(max, r);
      seen = true;
    }
    return seen ? max - min : 0f;
  }

  /**
   * Variance over an explicit angular window — for near-full arcs whose tiny hole leaves no
   * fully-empty ray to seed the cap-exclusion logic (the determinate hole sits just past 12
   * o'clock, so a window like {@code [120°, 420°]} avoids it and both caps).
   */
  private static float outerVarianceInWindow(
      final BufferedImage img, final Color target, final int fromDeg, final int toDeg) {
    float min = Float.MAX_VALUE;
    float max = -1f;
    boolean seen = false;
    for (int a = fromDeg; a <= toDeg; a += 2) {
      final float r = outerExtent(img, target, ((a % 360) + 360) % 360);
      if (r <= 0f) {
        continue;
      }
      min = Math.min(min, r);
      max = Math.max(max, r);
      seen = true;
    }
    return seen ? max - min : 0f;
  }

  /**
   * The stroke's centroid radius along a ray — the midpoint of the innermost and outermost hits.
   * Anti-aliased edge loss hits both edges symmetrically, so the centroid is quantization-stable
   * where a raw outermost-extent read is not; a flat stroke reads constant, a wavy one swings the
   * full amplitude.
   */
  private static float outerExtent(final BufferedImage img, final Color target, final int angle) {
    final float c = img.getWidth() / 2f;
    final double rad = Math.toRadians(angle);
    float outer = 0f;
    float inner = 0f;
    for (float r = c; r >= 2f; r -= 0.5f) {
      final int x = Math.round(c + (float) (r * Math.cos(rad)) - 0.5f);
      final int y = Math.round(c - (float) (r * Math.sin(rad)) - 0.5f);
      if (x < 0 || y < 0 || x >= img.getWidth() || y >= img.getHeight()) {
        continue;
      }
      if (nearColor(img.getRGB(x, y), target)) {
        if (outer == 0f) {
          outer = r;
        }
        inner = r;
      } else if (outer > 0f) {
        break;
      }
    }
    return outer == 0f ? 0f : (outer + inner) / 2f;
  }

  /** Number of whole degrees whose ray hits the color anywhere. */
  private static int coverage(final BufferedImage img, final Color target) {
    final float c = img.getWidth() / 2f;
    int hits = 0;
    for (int a = 0; a < 360; a++) {
      final double rad = Math.toRadians(a);
      for (float r = c; r >= 4f; r -= 0.5f) {
        final int x = Math.round(c + (float) (r * Math.cos(rad)));
        final int y = Math.round(c - (float) (r * Math.sin(rad)));
        if (x < 0 || y < 0 || x >= img.getWidth() || y >= img.getHeight()) {
          continue;
        }
        if (nearColor(img.getRGB(x, y), target)) {
          hits++;
          break;
        }
      }
    }
    return hits;
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
