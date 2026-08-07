package com.owspfm.elwha.theme;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.owspfm.elwha.testkit.EdtInterceptor;
import com.owspfm.elwha.testkit.ThemeExtension;
import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.util.Arrays;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

/**
 * Tier A coverage of the shape-morph painter's stateless contract — endpoint fidelity, the easing
 * composition it performs on the caller's raw progress, and deterministic paint at a fixed phase.
 * Progress is injected throughout; no timer is ever started (testing.md determinism rule 1).
 */
@ExtendWith({EdtInterceptor.class, ThemeExtension.class})
class ShapeMorphPainterTest {

  private static final int WIDTH = 48;
  private static final int HEIGHT = 48;
  private static final CornerRadii SQUARE = CornerRadii.uniform(0);
  private static final CornerRadii ROUND = CornerRadii.uniform(24);

  private static BufferedImage morphAt(final float progress) {
    final BufferedImage image = new BufferedImage(WIDTH, HEIGHT, BufferedImage.TYPE_INT_ARGB);
    final Graphics2D g = image.createGraphics();
    try {
      g.setColor(Color.WHITE);
      g.fillRect(0, 0, WIDTH, HEIGHT);
      ShapeMorphPainter.paint(
          g,
          WIDTH,
          HEIGHT,
          SQUARE,
          ROUND,
          progress,
          Easing.LINEAR,
          ColorRole.PRIMARY,
          null,
          null,
          0f);
    } finally {
      g.dispose();
    }
    return image;
  }

  private static int[] pixels(final BufferedImage image) {
    return image.getRGB(0, 0, WIDTH, HEIGHT, null, 0, WIDTH);
  }

  // ----------------------------------------------------------- interpolation

  @Test
  void endpointsRenderTheirOwnGeometryExactly() {
    assertThat(ShapeMorphPainter.interpolate(SQUARE, ROUND, 0f, Easing.LINEAR))
        .as("a steady-state component at rest reports exactly its from-geometry")
        .isEqualTo(SQUARE);
    assertThat(ShapeMorphPainter.interpolate(SQUARE, ROUND, 1f, Easing.LINEAR))
        .as("and exactly its to-geometry when the morph has landed")
        .isEqualTo(ROUND);
  }

  @Test
  void midpointSitsBetweenTheEndpoints() {
    final CornerRadii middle = ShapeMorphPainter.interpolate(SQUARE, ROUND, 0.5f, Easing.LINEAR);

    assertThat(middle.topLeftPx())
        .as("a linear half-way phase lands half way between the two radii")
        .isEqualTo(12);
  }

  @Test
  void aNullEasingIsTreatedAsLinear() {
    assertThat(ShapeMorphPainter.interpolate(SQUARE, ROUND, 0.25f, null))
        .as("omitting a curve is the identity curve, not an error")
        .isEqualTo(ShapeMorphPainter.interpolate(SQUARE, ROUND, 0.25f, Easing.LINEAR));
  }

  @Test
  void easingIsComposedOnTopOfTheCallersProgress() {
    final CornerRadii eased = ShapeMorphPainter.interpolate(SQUARE, ROUND, 0.25f, Easing.STANDARD);

    assertThat(eased)
        .as("one animator feeding several morphs can give each its own curve")
        .isNotEqualTo(ShapeMorphPainter.interpolate(SQUARE, ROUND, 0.25f, Easing.LINEAR));
    assertThat(eased.topLeftPx())
        .as("the emphasized standard curve front-loads, so it is ahead of linear at a quarter")
        .isGreaterThan(6);
  }

  @Test
  void everyCurveStillHonorsTheEndpoints() {
    for (final Easing easing :
        new Easing[] {
          Easing.LINEAR,
          Easing.STANDARD,
          Easing.STANDARD_DECELERATE,
          Easing.STANDARD_ACCELERATE,
          Easing.EMPHASIZED,
          Easing.EMPHASIZED_DECELERATE
        }) {
      assertThat(ShapeMorphPainter.interpolate(SQUARE, ROUND, 0f, easing))
          .as("no curve may move the resting geometry")
          .isEqualTo(SQUARE);
      assertThat(ShapeMorphPainter.interpolate(SQUARE, ROUND, 1f, easing))
          .as("no curve may move the landed geometry")
          .isEqualTo(ROUND);
    }
  }

  @Test
  void phasesOutsideTheUnitIntervalClampToTheEndpoints() {
    assertThat(ShapeMorphPainter.interpolate(SQUARE, ROUND, -1f, Easing.LINEAR))
        .as("a stray negative phase does not extrapolate past the from-geometry")
        .isEqualTo(SQUARE);
    assertThat(ShapeMorphPainter.interpolate(SQUARE, ROUND, 5f, Easing.LINEAR))
        .as("a stray large phase does not extrapolate past the to-geometry")
        .isEqualTo(ROUND);
  }

  @Test
  void bothEndpointsAreRequired() {
    assertThatThrownBy(() -> ShapeMorphPainter.interpolate(null, ROUND, 0.5f, Easing.LINEAR))
        .as("a morph without a start has no geometry to interpolate from")
        .isInstanceOf(NullPointerException.class);
    assertThatThrownBy(() -> ShapeMorphPainter.interpolate(SQUARE, null, 0.5f, Easing.LINEAR))
        .as("a morph without a destination has nothing to interpolate toward")
        .isInstanceOf(NullPointerException.class);
  }

  // ------------------------------------------------------------------ paint

  @Test
  void oneProgressValueAlwaysPaintsTheSameRaster() {
    assertThat(pixels(morphAt(0.35f)))
        .as("the painter owns no state, so two calls at one phase are pixel-identical")
        .isEqualTo(pixels(morphAt(0.35f)));
  }

  @Test
  void differentProgressValuesPaintDifferentRasters() {
    assertThat(Arrays.equals(pixels(morphAt(0f)), pixels(morphAt(1f))))
        .as("the whole point is that the body geometry moves between frames")
        .isFalse();
  }

  @Test
  void paintedCornerFollowsTheInterpolatedGeometry() {
    assertThat(new Color(morphAt(0f).getRGB(0, 0)))
        .as("at rest the square end-state fills the corner")
        .isEqualTo(ColorRole.PRIMARY.resolve());
    assertThat(new Color(morphAt(1f).getRGB(0, 0)))
        .as("landed, the fully rounded end-state has cut the corner away")
        .isEqualTo(Color.WHITE);
  }

  @Test
  void bodyCentreIsFilledAtEveryPhase() {
    for (final float progress : new float[] {0f, 0.25f, 0.5f, 0.75f, 1f}) {
      assertThat(new Color(morphAt(progress).getRGB(WIDTH / 2, HEIGHT / 2)))
          .as("the morph changes the outline, never whether the surface paints")
          .isEqualTo(ColorRole.PRIMARY.resolve());
    }
  }
}
