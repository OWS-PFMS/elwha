package com.owspfm.elwha.theme;

import static org.assertj.core.api.Assertions.assertThat;

import com.owspfm.elwha.testkit.EdtInterceptor;
import com.owspfm.elwha.testkit.ThemeExtension;
import java.awt.AlphaComposite;
import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.Point;
import java.awt.image.BufferedImage;
import java.util.Arrays;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

/**
 * Tier A coverage of the shared press-ripple painter's <em>stateless</em> contract — that a given
 * progress value always paints the same thing, that different phases paint differently, and that
 * the documented suppression cases paint nothing. Nothing here samples a clock: the painter takes
 * injected progress by design, which is exactly what lets an animation-frame assertion be
 * deterministic (testing.md determinism rule 1).
 */
@ExtendWith({EdtInterceptor.class, ThemeExtension.class})
class RipplePainterTest {

  private static final int WIDTH = 60;
  private static final int HEIGHT = 40;
  private static final Point ORIGIN = new Point(30, 20);
  private static final int ARC = 12;

  private static BufferedImage rippleAt(final float progress) {
    return rippleAt(progress, ORIGIN);
  }

  private static BufferedImage rippleAt(final float progress, final Point origin) {
    final BufferedImage image = new BufferedImage(WIDTH, HEIGHT, BufferedImage.TYPE_INT_ARGB);
    final Graphics2D g = image.createGraphics();
    try {
      g.setColor(Color.WHITE);
      g.fillRect(0, 0, WIDTH, HEIGHT);
      RipplePainter.paint(g, WIDTH, HEIGHT, origin, progress, ARC, Color.BLACK);
    } finally {
      g.dispose();
    }
    return image;
  }

  private static int[] pixels(final BufferedImage image) {
    return image.getRGB(0, 0, WIDTH, HEIGHT, null, 0, WIDTH);
  }

  private static BufferedImage blank() {
    final BufferedImage image = new BufferedImage(WIDTH, HEIGHT, BufferedImage.TYPE_INT_ARGB);
    final Graphics2D g = image.createGraphics();
    try {
      g.setColor(Color.WHITE);
      g.fillRect(0, 0, WIDTH, HEIGHT);
    } finally {
      g.dispose();
    }
    return image;
  }

  @Test
  void peakOpacityIsTheDocumentedPressTint() {
    assertThat(RipplePainter.PEAK_OPACITY)
        .as("the press state layer peaks at 10 percent, surfaced for variants that tune it")
        .isEqualTo(0.10f);
  }

  @Test
  void oneProgressValueAlwaysPaintsTheSameRaster() {
    assertThat(pixels(rippleAt(0.3f)))
        .as("the painter owns no state, so two calls at one phase are pixel-identical")
        .isEqualTo(pixels(rippleAt(0.3f)));
  }

  @Test
  void differentProgressValuesPaintDifferentRasters() {
    assertThat(Arrays.equals(pixels(rippleAt(0.1f)), pixels(rippleAt(0.5f))))
        .as("injected progress is what advances the animation — the frames must differ")
        .isFalse();
  }

  @Test
  void rippleExpandsAsProgressAdvances() {
    assertThat(tintedPixelCount(rippleAt(0.4f)))
        .as("a later phase in the expand window covers more of the body")
        .isGreaterThan(tintedPixelCount(rippleAt(0.1f)));
  }

  @Test
  void tintFadesOutBeforeTheEnd() {
    final Color early = new Color(rippleAt(0.05f).getRGB(ORIGIN.x, ORIGIN.y));
    final Color late = new Color(rippleAt(0.95f).getRGB(ORIGIN.x, ORIGIN.y));

    assertThat(late.getRed())
        .as("the fade tail runs to fully transparent, so the seed point returns toward the ground")
        .isGreaterThan(early.getRed());
  }

  @Test
  void aCompletedRipplePaintsNothing() {
    assertThat(pixels(rippleAt(1f)))
        .as("progress at or past 1 means the fade tail has finished — no paint at all")
        .isEqualTo(pixels(blank()));
    assertThat(pixels(rippleAt(2f)))
        .as("an overshooting phase is suppressed the same way")
        .isEqualTo(pixels(blank()));
  }

  @Test
  void aRippleWithNoSeedPointPaintsNothing() {
    assertThat(pixels(rippleAt(0.4f, null)))
        .as("a null origin means no press has been recorded, so there is nothing to draw")
        .isEqualTo(pixels(blank()));
  }

  @Test
  void rippleStaysInsideTheHostsCornerRadius() {
    final BufferedImage image = rippleAt(0.9f);

    assertThat(new Color(image.getRGB(0, 0)))
        .as("the ripple clips to the host's round-rect outline rather than squaring its corners")
        .isEqualTo(Color.WHITE);
  }

  @Test
  void callersGraphicsIsNotLeftMutated() {
    final BufferedImage image = blank();
    final Graphics2D g = image.createGraphics();
    try {
      g.setColor(Color.RED);
      RipplePainter.paint(g, WIDTH, HEIGHT, ORIGIN, 0.4f, ARC, Color.BLACK);

      assertThat(g.getColor())
          .as("the painter works on a defensive copy, so clip and compositing stay the caller's")
          .isEqualTo(Color.RED);
      assertThat(g.getClipBounds())
          .as("the caller's graphics comes back with no clip at all, exactly as it went in")
          .isNull();
      assertThat(g.getComposite())
          .as("the ripple's alpha composite does not leak back out either")
          .isEqualTo(AlphaComposite.SrcOver);
    } finally {
      g.dispose();
    }
  }

  @Test
  void perCornerOverloadPaintsAndSuppressesTheSameWay() {
    final CornerRadii radii = CornerRadii.of(12, 0, 12, 0);
    final BufferedImage image = new BufferedImage(WIDTH, HEIGHT, BufferedImage.TYPE_INT_ARGB);
    final Graphics2D g = image.createGraphics();
    try {
      g.setColor(Color.WHITE);
      g.fillRect(0, 0, WIDTH, HEIGHT);
      RipplePainter.paint(g, WIDTH, HEIGHT, ORIGIN, 0.9f, radii, Color.BLACK);
    } finally {
      g.dispose();
    }

    assertThat(new Color(image.getRGB(0, 0)))
        .as("a rounded corner is cut out of the ripple")
        .isEqualTo(Color.WHITE);
    assertThat(new Color(image.getRGB(WIDTH - 1, 0)))
        .as("the butted square corner of a group segment is not")
        .isNotEqualTo(Color.WHITE);
  }

  private static int tintedPixelCount(final BufferedImage image) {
    int count = 0;
    for (final int rgb : pixels(image)) {
      if (new Color(rgb).getRed() < 250) {
        count++;
      }
    }
    return count;
  }
}
