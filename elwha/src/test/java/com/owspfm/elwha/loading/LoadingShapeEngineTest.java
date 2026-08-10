package com.owspfm.elwha.loading;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.within;

import com.owspfm.elwha.testkit.EdtInterceptor;
import com.owspfm.elwha.testkit.ThemeExtension;
import java.awt.geom.Path2D;
import java.awt.geom.PathIterator;
import java.awt.geom.Point2D;
import java.awt.geom.Rectangle2D;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

/**
 * Tier A coverage of the self-contained shape-morph engine — design doc §2: every M3
 * loading-indicator shape is star-convex, so a shape is the function {@code r(θ)} sampled on a
 * fixed angular grid, a morph is a per-angle lerp between two such profiles, and the spin is a
 * rotation offset applied when the profile is rendered back to a path.
 *
 * <p>Because the engine is pure, the morph can be asserted directly at chosen phase values rather
 * than by rendering a running animation: <em>identical</em> phase gives an identical profile,
 * <em>distinct</em> phase gives a distinct one. That is the whole determinism story for this
 * package.
 */
@ExtendWith({EdtInterceptor.class, ThemeExtension.class})
class LoadingShapeEngineTest {

  private static float maxOf(final float[] profile) {
    float max = 0f;
    for (final float r : profile) {
      max = Math.max(max, r);
    }
    return max;
  }

  private static float minOf(final float[] profile) {
    float min = Float.MAX_VALUE;
    for (final float r : profile) {
      min = Math.min(min, r);
    }
    return min;
  }

  private static double distance(final float[] a, final float[] b) {
    double sum = 0;
    for (int i = 0; i < a.length; i++) {
      sum += Math.abs(a[i] - b[i]);
    }
    return sum;
  }

  private static int vertexCount(final Path2D.Float path) {
    int n = 0;
    final PathIterator it = path.getPathIterator(null);
    final double[] coords = new double[6];
    while (!it.isDone()) {
      final int seg = it.currentSegment(coords);
      if (seg == PathIterator.SEG_MOVETO || seg == PathIterator.SEG_LINETO) {
        n++;
      }
      it.next();
    }
    return n;
  }

  // -------------------------------------------------------- radius profiles

  @Test
  void everyCatalogShapeSamplesOntoTheSameAngularGrid() {
    for (final RoundedPolygonShape shape : LoadingShapes.INDETERMINATE) {
      assertThat(shape.radii())
          .as("§2 — a per-angle lerp is only meaningful if both profiles share the grid")
          .hasSize(RoundedPolygonShape.SAMPLE_COUNT);
    }
    assertThat(LoadingShapes.CIRCLE.radii()).hasSize(RoundedPolygonShape.SAMPLE_COUNT);
    assertThat(LoadingShapes.SOFT_BURST.radii()).hasSize(RoundedPolygonShape.SAMPLE_COUNT);
  }

  @Test
  void everyProfileIsNormalizedToAUnitOuterRadius() {
    for (final RoundedPolygonShape shape : LoadingShapes.INDETERMINATE) {
      assertThat(maxOf(shape.radii()))
          .as("§2 — normalizing means one scale factor drives every shape's painted size")
          .isCloseTo(1f, within(0.001f));
    }
  }

  @Test
  void everyProfileIsStrictlyInsideTheUnitDisc() {
    for (final RoundedPolygonShape shape : LoadingShapes.INDETERMINATE) {
      assertThat(minOf(shape.radii()))
          .as("§2 — star-convex means r(θ) is positive everywhere; a zero would pinch the outline")
          .isGreaterThan(0f);
      assertThat(maxOf(shape.radii())).isLessThanOrEqualTo(1.001f);
    }
  }

  @Test
  void aCircleHasAConstantRadius() {
    final float[] circle = LoadingShapes.CIRCLE.radii();

    assertThat(maxOf(circle) - minOf(circle))
        .as("§2 — a circle is the degenerate profile: the same radius at every sampled angle")
        .isZero();
  }

  @Test
  void everyLobedShapeVariesItsRadiusAroundTheTurn() {
    // The shallowest form in the catalog (the soft burst) swings about 0.09; the deepest (the
    // pill) about 0.39. The floor below sits under the shallowest and far above the circle's
    // exact zero, so it distinguishes "has features" from "is a disc" without pinning any one
    // shape's eye-tuned depth.
    for (final RoundedPolygonShape shape : LoadingShapes.INDETERMINATE) {
      assertThat(maxOf(shape.radii()) - minOf(shape.radii()))
          .as("§2 — a lobed shape is exactly a profile whose radius swings; that is the trick")
          .isGreaterThan(0.05f);
    }
  }

  @Test
  void indeterminateLoopIsSevenDistinctShapes() {
    assertThat(LoadingShapes.INDETERMINATE).as("§3 — the M3 indeterminate loop").hasSize(7);
    for (int i = 0; i < LoadingShapes.INDETERMINATE.length; i++) {
      for (int j = i + 1; j < LoadingShapes.INDETERMINATE.length; j++) {
        assertThat(
                distance(
                    LoadingShapes.INDETERMINATE[i].radii(), LoadingShapes.INDETERMINATE[j].radii()))
            .as("shapes %d and %d must actually differ, or the loop would stall visually", i, j)
            .isGreaterThan(1.0);
      }
    }
  }

  // ------------------------------------------------------------------- lerp

  @Test
  void aMorphAtZeroIsTheStartShape() {
    final float[] a = LoadingShapes.CIRCLE.radii();
    final float[] b = LoadingShapes.SOFT_BURST.radii();

    assertThat(ShapeMorph.lerp(a, b, 0f))
        .as("§2 — the endpoints of a morph are the shapes themselves")
        .containsExactly(a);
  }

  @Test
  void aMorphAtOneIsTheEndShape() {
    final float[] a = LoadingShapes.CIRCLE.radii();
    final float[] b = LoadingShapes.SOFT_BURST.radii();

    assertThat(ShapeMorph.lerp(a, b, 1f)).containsExactly(b);
  }

  @Test
  void aMorphAtTheHalfwayPointIsTheAverageOfBothProfiles() {
    final float[] a = LoadingShapes.CIRCLE.radii();
    final float[] b = LoadingShapes.SOFT_BURST.radii();

    final float[] mid = ShapeMorph.lerp(a, b, 0.5f);

    for (int k = 0; k < mid.length; k++) {
      assertThat(mid[k])
          .as("§2 — a morph is a per-angle lerp, sample %d", k)
          .isCloseTo((a[k] + b[k]) / 2f, within(0.0001f));
    }
  }

  @ParameterizedTest
  @ValueSource(floats = {-1f, -0.001f})
  void aMorphPhaseBelowZeroClampsToTheStartShape(final float phase) {
    assertThat(
            ShapeMorph.lerp(LoadingShapes.CIRCLE.radii(), LoadingShapes.SOFT_BURST.radii(), phase))
        .as("an out-of-range phase must not extrapolate into a shape that is not on the path")
        .containsExactly(LoadingShapes.CIRCLE.radii());
  }

  @ParameterizedTest
  @ValueSource(floats = {1.001f, 5f})
  void aMorphPhaseAboveOneClampsToTheEndShape(final float phase) {
    assertThat(
            ShapeMorph.lerp(LoadingShapes.CIRCLE.radii(), LoadingShapes.SOFT_BURST.radii(), phase))
        .containsExactly(LoadingShapes.SOFT_BURST.radii());
  }

  @Test
  void aMorphNeverMutatesEitherEndpoint() {
    final float[] a = LoadingShapes.CIRCLE.radii();
    final float[] before = a.clone();

    ShapeMorph.lerp(a, LoadingShapes.SOFT_BURST.radii(), 0.5f);

    assertThat(a)
        .as("§2 — the catalog profiles are shared, so a morph must return a fresh array")
        .containsExactly(before);
  }

  @Test
  void identicalPhasesProduceIdenticalProfiles() {
    final float[] first =
        ShapeMorph.lerp(LoadingShapes.CIRCLE.radii(), LoadingShapes.SOFT_BURST.radii(), 0.37f);
    final float[] again =
        ShapeMorph.lerp(LoadingShapes.CIRCLE.radii(), LoadingShapes.SOFT_BURST.radii(), 0.37f);

    assertThat(first)
        .as("the engine is pure — the same phase is the same shape, always")
        .containsExactly(again);
  }

  @Test
  void distinctPhasesProduceDistinctProfiles() {
    final float[] a = LoadingShapes.CIRCLE.radii();
    final float[] b = LoadingShapes.SOFT_BURST.radii();

    assertThat(distance(ShapeMorph.lerp(a, b, 0.25f), ShapeMorph.lerp(a, b, 0.75f)))
        .as("and different phases are visibly different shapes — otherwise nothing would animate")
        .isGreaterThan(1.0);
  }

  @Test
  void aMorphSweepsMonotonicallyAwayFromTheStartShape() {
    final float[] a = LoadingShapes.CIRCLE.radii();
    final float[] b = LoadingShapes.SOFT_BURST.radii();
    double previous = -1;

    for (float t = 0f; t <= 1f; t += 0.1f) {
      final double away = distance(ShapeMorph.lerp(a, b, t), a);
      assertThat(away)
          .as("§2 — a linear blend moves steadily from one shape to the other, never doubling back")
          .isGreaterThanOrEqualTo(previous);
      previous = away;
    }
  }

  // ------------------------------------------------------------------ paths

  @Test
  void aProfileRendersAsAClosedPolylineWithOneVertexPerSample() {
    final Path2D.Float path = ShapeMorph.toPath(LoadingShapes.CIRCLE.radii(), 50, 50, 20, 0);

    assertThat(vertexCount(path))
        .as("§2 — the profile is rendered back one sample at a time")
        .isEqualTo(RoundedPolygonShape.SAMPLE_COUNT);
  }

  @Test
  void aRenderedPathIsCentredWhereItWasAsked() {
    final Path2D.Float path = ShapeMorph.toPath(LoadingShapes.CIRCLE.radii(), 60, 40, 20, 0);
    final Rectangle2D bounds = path.getBounds2D();

    assertThat(bounds.getCenterX()).isCloseTo(60, within(0.5));
    assertThat(bounds.getCenterY()).isCloseTo(40, within(0.5));
  }

  @Test
  void scaleIsThePaintedOuterRadius() {
    final Rectangle2D bounds =
        ShapeMorph.toPath(LoadingShapes.CIRCLE.radii(), 50, 50, 20, 0).getBounds2D();

    assertThat(bounds.getWidth())
        .as("§2 — a unit-normalized profile at scale s paints a shape of outer radius s")
        .isCloseTo(40, within(1.0));
  }

  @Test
  void doublingTheScaleDoublesTheShape() {
    final Rectangle2D small =
        ShapeMorph.toPath(LoadingShapes.SOFT_BURST.radii(), 50, 50, 10, 0).getBounds2D();
    final Rectangle2D large =
        ShapeMorph.toPath(LoadingShapes.SOFT_BURST.radii(), 50, 50, 20, 0).getBounds2D();

    assertThat(large.getWidth()).isCloseTo(small.getWidth() * 2, within(0.5));
  }

  @Test
  void rotationMovesTheVerticesAroundTheCentre() {
    final float[] burst = LoadingShapes.SOFT_BURST.radii();

    final Point2D unrotated = ShapeMorph.toPath(burst, 50, 50, 20, 0).getCurrentPoint();
    final Point2D rotated = ShapeMorph.toPath(burst, 50, 50, 20, Math.PI / 4).getCurrentPoint();

    assertThat(unrotated.distance(rotated))
        .as("§2 — the spin is expressed as a rotation of the sampling angle, nothing more")
        .isGreaterThan(1.0);
  }

  @Test
  void aFullTurnOfRotationLandsBackOnTheSameShape() {
    final float[] burst = LoadingShapes.SOFT_BURST.radii();

    final Rectangle2D none = ShapeMorph.toPath(burst, 50, 50, 20, 0).getBounds2D();
    final Rectangle2D full = ShapeMorph.toPath(burst, 50, 50, 20, 2 * Math.PI).getBounds2D();

    assertThat(full.getX()).isCloseTo(none.getX(), within(0.01));
    assertThat(full.getWidth()).isCloseTo(none.getWidth(), within(0.01));
  }

  @Test
  void rotationDoesNotChangeTheShapesSize() {
    final float[] burst = LoadingShapes.SOFT_BURST.radii();

    final Rectangle2D upright = ShapeMorph.toPath(burst, 50, 50, 20, 0).getBounds2D();
    final Rectangle2D tilted =
        ShapeMorph.toPath(burst, 50, 50, 20, Math.toRadians(37)).getBounds2D();

    assertThat(tilted.getWidth())
        .as("a rotation is rigid — it moves the figure without resizing it")
        .isCloseTo(upright.getWidth(), within(1.5));
  }
}
