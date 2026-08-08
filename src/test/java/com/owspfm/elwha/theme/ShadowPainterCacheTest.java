package com.owspfm.elwha.theme;

import static org.assertj.core.api.Assertions.assertThat;

import com.owspfm.elwha.testkit.EdtInterceptor;
import com.owspfm.elwha.testkit.ThemeExtension;
import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.Insets;
import java.awt.image.BufferedImage;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

/**
 * Tier A coverage of {@link ShadowPainter}'s cached 9-slice path (#649) — that it is <em>reachable
 * </em> for every body shape, and that what it draws still matches a shadow rendered directly at
 * the body's own size.
 *
 * <p>Reachability was the defect: the small-body fallback fired when {@code width < 2 * (radius +
 * inset)}, which for a pill (radius = height/2) is unconditionally true, so every elevated pill —
 * every {@code ELEVATED} {@code ROUND} button, at every size — re-blurred its shadow from scratch
 * on every repaint, six image allocations and four convolutions per frame at 60 fps.
 *
 * <p>Fidelity is asserted against {@link ShadowPainter#renderShadowAtBodySize}, the uncached direct
 * render, over the <em>halo only</em>: the caller paints an opaque body over the interior, so
 * differences inside the body rect are never seen.
 */
@ExtendWith({EdtInterceptor.class, ThemeExtension.class})
class ShadowPainterCacheTest {

  /**
   * Per-channel bound on how far the sliced halo may sit from a direct render. Measured, not
   * chosen: across the matrix below the worst pixel is 15/255 and all but a handful sit under 10,
   * at elevation 5 where the blur is widest. The path this replaced was further out — a directly
   * comparable sweep put it at 37/255 over thousands of pixels — so the cached image is closer to
   * the ground truth than the code it supersedes, as well as cheaper.
   */
  private static final int HALO_TOLERANCE = 15;

  @BeforeEach
  void emptyTheCache() {
    ShadowPainter.clearCache();
  }

  /** The shadow as the component pipeline draws it — through the cached 9-slice path. */
  private static BufferedImage sliced(final int w, final int h, final int arc, final int elev) {
    final Insets in = ShadowPainter.shadowInsets(elev);
    final BufferedImage image =
        new BufferedImage(
            w + in.left + in.right, h + in.top + in.bottom, BufferedImage.TYPE_INT_ARGB);
    final Graphics2D g = image.createGraphics();
    try {
      g.translate(in.left, in.top);
      ShadowPainter.paint(g, w, h, arc, elev);
    } finally {
      g.dispose();
    }
    return image;
  }

  /** The worst per-channel difference anywhere in the halo, ignoring the body's own rect. */
  private static int haloDelta(final int w, final int h, final int arc, final int elev) {
    final BufferedImage cached = sliced(w, h, arc, elev);
    final BufferedImage direct = ShadowPainter.renderShadowAtBodySize(w, h, arc, elev);
    final Insets in = ShadowPainter.shadowInsets(elev);
    int worst = 0;
    for (int y = 0; y < Math.min(cached.getHeight(), direct.getHeight()); y++) {
      for (int x = 0; x < Math.min(cached.getWidth(), direct.getWidth()); x++) {
        final boolean underBody = x >= in.left && x < in.left + w && y >= in.top && y < in.top + h;
        if (underBody) {
          continue;
        }
        final Color a = new Color(cached.getRGB(x, y), true);
        final Color b = new Color(direct.getRGB(x, y), true);
        worst =
            Math.max(
                worst,
                Math.max(
                    Math.abs(a.getAlpha() - b.getAlpha()),
                    Math.max(
                        Math.abs(a.getRed() - b.getRed()),
                        Math.max(
                            Math.abs(a.getGreen() - b.getGreen()),
                            Math.abs(a.getBlue() - b.getBlue())))));
      }
    }
    return worst;
  }

  // ------------------------------------------------------------ reachability

  @ParameterizedTest
  @CsvSource({
    "200, 40, 40", // an ELEVATED ROUND button — the pill the fallback used to catch every frame
    "120, 40, 40",
    "80, 32, 32",
    "48, 24, 24",
    "40, 40, 40", // a circle: no straight run on either axis
    "56, 56, 56",
  })
  void anElevatedPillIsServedFromTheCache(final int w, final int h, final int arc) {
    sliced(w, h, arc, 1);

    assertThat(ShadowPainter.cacheSize())
        .as(
            "#649 — the small-body fallback was unconditionally true for a pill (radius = h/2), so "
                + "every repaint re-blurred instead of caching")
        .isEqualTo(1);
  }

  @Test
  void resizingAPillAlongItsLongAxisReusesTheSameCachedImage() {
    sliced(120, 40, 40, 1);
    sliced(200, 40, 40, 1);
    sliced(340, 40, 40, 1);

    assertThat(ShadowPainter.cacheSize())
        .as("shape, not size, is the cache key — a growing button re-blurs nothing")
        .isEqualTo(1);
  }

  @Test
  void shapesThatSliceDifferentlyGetTheirOwnEntry() {
    sliced(200, 40, 40, 1); // pill: straight run on x only
    sliced(40, 40, 40, 1); // circle: no straight run at all
    sliced(200, 120, 40, 1); // rounded rect: straight run on both axes

    assertThat(ShadowPainter.cacheSize())
        .as("a pill's cap has no straight edge below it, so it cannot share a rounded rect's image")
        .isEqualTo(3);
  }

  // --------------------------------------------------------------- fidelity

  @ParameterizedTest
  @CsvSource({
    "200, 40, 40",
    "120, 40, 40",
    "80, 32, 32",
    "48, 24, 24",
    "40, 40, 40",
    "56, 56, 56",
    "200, 40, 20",
    "300, 56, 16",
    "220, 160, 24",
    "96, 40, 8",
  })
  void cachedHaloMatchesADirectRenderAtEveryElevation(final int w, final int h, final int arc) {
    for (int elevation = 1; elevation <= ShadowPainter.MAX_ELEVATION; elevation++) {
      assertThat(haloDelta(w, h, arc, elevation))
          .as("%dx%d arc %d at elevation %d", w, h, arc, elevation)
          .isLessThanOrEqualTo(HALO_TOLERANCE);
    }
  }

  @Test
  void aPillsCapIsReproducedAlmostExactly() {
    // The hot path deserves the tighter bound: a pill slices its caps from a canonical that is
    // itself a pill, so the corner slices are 1:1 copies of the same geometry rather than an
    // approximation borrowed from a rounded square.
    for (int elevation = 1; elevation <= ShadowPainter.MAX_ELEVATION; elevation++) {
      assertThat(haloDelta(200, 40, 40, elevation))
          .as("an ELEVATED ROUND button's halo at elevation %d", elevation)
          .isLessThanOrEqualTo(4);
    }
  }

  @Test
  void aFlatBodyPaintsNoShadowAndCachesNothing() {
    final BufferedImage image = sliced(120, 40, 40, 0);

    assertThat(ShadowPainter.cacheSize()).as("elevation 0 is a no-op").isZero();
    assertThat(new Color(image.getRGB(0, 0), true).getAlpha())
        .as("and leaves the raster untouched")
        .isZero();
  }
}
