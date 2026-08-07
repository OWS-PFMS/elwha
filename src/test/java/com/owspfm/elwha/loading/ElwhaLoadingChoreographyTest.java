package com.owspfm.elwha.loading;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.within;

import com.owspfm.elwha.testkit.EdtInterceptor;
import com.owspfm.elwha.testkit.ThemeExtension;
import java.util.HashSet;
import java.util.Set;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

/**
 * Tier A coverage of the loading indicator's choreography — design doc §6: the indeterminate
 * 7-shape loop stepping every {@code STEP_MS} with an {@code EMPHASIZED} settle, the linear spin
 * plus per-step quarter-turn kick, and the determinate {@code Circle → SoftBurst} sweep.
 *
 * <p>The component already exposes its choreography as pure functions of an elapsed-ms timeline, so
 * every frame state below is evaluated at an <em>injected</em> time value. No timer runs, nothing
 * sleeps, and "what does step three look like" is answered by asking for step three.
 */
@ExtendWith({EdtInterceptor.class, ThemeExtension.class})
class ElwhaLoadingChoreographyTest {

  private static final int SHAPES = LoadingShapes.INDETERMINATE.length;
  private static final long LOOP_MS = (long) SHAPES * ElwhaLoadingIndicator.STEP_MS;

  private static double distance(final float[] a, final float[] b) {
    double sum = 0;
    for (int i = 0; i < a.length; i++) {
      sum += Math.abs(a[i] - b[i]);
    }
    return sum;
  }

  private static double degreesAt(final long ms) {
    return Math.toDegrees(ElwhaLoadingIndicator.indeterminateRotationRad(ms));
  }

  // --------------------------------------------------- indeterminate shapes

  @Test
  void timelineStartsOnTheFirstShapeOfTheLoop() {
    assertThat(
            distance(
                ElwhaLoadingIndicator.indeterminateProfile(0),
                LoadingShapes.INDETERMINATE[0].radii()))
        .as("§6 — the loop opens on its first form, not mid-morph")
        .isLessThan(0.001);
  }

  @ParameterizedTest
  @ValueSource(ints = {0, 1, 2, 3, 4, 5, 6})
  void eachStepSettlesOntoItsShapeBeforeTheNextOneBegins(final int step) {
    // Sampled at the end of the step's dwell, after the EMPHASIZED settle has completed.
    final long atRest =
        (long) step * ElwhaLoadingIndicator.STEP_MS + ElwhaLoadingIndicator.STEP_MS - 1;

    assertThat(
            distance(
                ElwhaLoadingIndicator.indeterminateProfile(atRest),
                LoadingShapes.INDETERMINATE[(step + 1) % SHAPES].radii()))
        .as(
            "§6 — each %d ms step completes its morph and dwells on the next shape",
            ElwhaLoadingIndicator.STEP_MS)
        .isLessThan(0.001);
  }

  @Test
  void shapeLoopReturnsToItsStartAfterSevenSteps() {
    assertThat(
            distance(
                ElwhaLoadingIndicator.indeterminateProfile(LOOP_MS),
                ElwhaLoadingIndicator.indeterminateProfile(0)))
        .as("§3 — seven shapes, then round again")
        .isLessThan(0.001);
  }

  @Test
  void shapeIsPeriodicAcrossWholeLoops() {
    assertThat(
            distance(
                ElwhaLoadingIndicator.indeterminateProfile(1234),
                ElwhaLoadingIndicator.indeterminateProfile(1234 + 3 * LOOP_MS)))
        .as("the morph loop is closed, so any moment recurs one loop later")
        .isLessThan(0.001);
  }

  @Test
  void sameInstantAlwaysYieldsTheSameShape() {
    assertThat(ElwhaLoadingIndicator.indeterminateProfile(777))
        .as("the choreography is a pure function of elapsed time — no hidden state")
        .containsExactly(ElwhaLoadingIndicator.indeterminateProfile(777));
  }

  @Test
  void distinctInstantsWithinAStepYieldDistinctShapes() {
    assertThat(
            distance(
                ElwhaLoadingIndicator.indeterminateProfile(50),
                ElwhaLoadingIndicator.indeterminateProfile(300)))
        .as("§6 — the settle is a real interpolation, not a snap at the step boundary")
        .isGreaterThan(0.5);
  }

  @Test
  void shapeIsStillDuringAStepsDwell() {
    final int dwellStart = ElwhaLoadingIndicator.MORPH_MS;
    final int dwellEnd = ElwhaLoadingIndicator.STEP_MS - 1;

    assertThat(
            distance(
                ElwhaLoadingIndicator.indeterminateProfile(dwellStart),
                ElwhaLoadingIndicator.indeterminateProfile(dwellEnd)))
        .as(
            "§6 — the morph settles over %d ms of a %d ms step, then holds",
            ElwhaLoadingIndicator.MORPH_MS, ElwhaLoadingIndicator.STEP_MS)
        .isLessThan(0.001);
  }

  @Test
  void everyShapeInTheLoopIsVisitedOnce() {
    final Set<Integer> visited = new HashSet<>();

    for (int step = 0; step < SHAPES; step++) {
      final long atRest =
          (long) step * ElwhaLoadingIndicator.STEP_MS + ElwhaLoadingIndicator.STEP_MS - 1;
      final float[] profile = ElwhaLoadingIndicator.indeterminateProfile(atRest);
      for (int i = 0; i < SHAPES; i++) {
        if (distance(profile, LoadingShapes.INDETERMINATE[i].radii()) < 0.001) {
          visited.add(i);
        }
      }
    }

    assertThat(visited)
        .as("§3 — one full loop touches all seven forms, none skipped or repeated")
        .hasSize(SHAPES);
  }

  // ------------------------------------------------- indeterminate rotation

  @Test
  void rotationStartsAtZero() {
    assertThat(degreesAt(0)).as("§6 — the timeline opens unrotated").isCloseTo(0.0, within(0.001));
  }

  @Test
  void rotationAdvancesClockwiseAndNeverReverses() {
    double previous = -1;

    for (long ms = 0; ms <= 3 * LOOP_MS; ms += 37) {
      final double now = degreesAt(ms);
      assertThat(now)
          .as("§6 — spin, step kicks and the in-flight morph all push the same way, at %d ms", ms)
          .isGreaterThanOrEqualTo(previous);
      previous = now;
    }
  }

  @Test
  void aCompletedStepCommitsAQuarterTurnOnTopOfTheLinearSpin() {
    final int step = ElwhaLoadingIndicator.STEP_MS;
    final double linearOverOneStep =
        step / (double) ElwhaLoadingIndicator.GLOBAL_ROTATION_MS * 360.0;

    assertThat(degreesAt(step) - degreesAt(0))
        .as("§6 — one step is worth the linear spin plus one committed quarter turn")
        .isCloseTo(linearOverOneStep + ElwhaLoadingIndicator.QUARTER_TURN_DEG, within(0.5));
  }

  @Test
  void linearSpinCompletesAFullTurnInItsPeriod() {
    final long period = ElwhaLoadingIndicator.GLOBAL_ROTATION_MS;
    final long steps = period / ElwhaLoadingIndicator.STEP_MS;

    // Subtract the quarter-turn kicks committed over the same window to isolate the linear term.
    final double kicks = steps * ElwhaLoadingIndicator.QUARTER_TURN_DEG;

    assertThat(degreesAt(period) - kicks)
        .as("§6 — the %d ms global rotation is exactly one turn", period)
        .isCloseTo(360.0, within(ElwhaLoadingIndicator.QUARTER_TURN_DEG));
  }

  @Test
  void rotationIsNotPeriodicAcrossLoopsEvenThoughTheShapeIs() {
    assertThat(degreesAt(LOOP_MS))
        .as(
            "§6 — the shape loop closes but the spin keeps accumulating, so the figure never "
                + "repeats itself outright")
        .isGreaterThan(degreesAt(0) + 180);
  }

  @Test
  void sameInstantAlwaysYieldsTheSameRotation() {
    assertThat(degreesAt(1500)).as("pure in time, like the shape").isEqualTo(degreesAt(1500));
  }

  // --------------------------------------------------------- determinate

  @Test
  void determinateSweepRunsFromCircleToSoftBurst() {
    assertThat(distance(ElwhaLoadingIndicator.determinateProfile(0f), LoadingShapes.CIRCLE.radii()))
        .as("§3 — an empty determinate indicator is a circle")
        .isLessThan(0.001);
    assertThat(
            distance(
                ElwhaLoadingIndicator.determinateProfile(1f), LoadingShapes.SOFT_BURST.radii()))
        .as("§3 — and a complete one is the soft burst")
        .isLessThan(0.001);
  }

  @ParameterizedTest
  @ValueSource(floats = {0.1f, 0.25f, 0.5f, 0.75f, 0.9f})
  void determinateShapeIsDrivenByProgressAlone(final float fraction) {
    assertThat(ElwhaLoadingIndicator.determinateProfile(fraction))
        .as("no clock is involved in determinate mode at all")
        .containsExactly(ElwhaLoadingIndicator.determinateProfile(fraction));
  }

  @Test
  void determinateShapeMovesSteadilyWithProgress() {
    final float[] circle = LoadingShapes.CIRCLE.radii();
    double previous = -1;

    for (float f = 0f; f <= 1f; f += 0.1f) {
      final double away = distance(ElwhaLoadingIndicator.determinateProfile(f), circle);
      assertThat(away)
          .as("§3 — progress drives the morph monotonically")
          .isGreaterThanOrEqualTo(previous);
      previous = away;
    }
  }

  @Test
  void determinateRotationIsAHalfTurnAcrossTheWholeRun() {
    assertThat(Math.toDegrees(ElwhaLoadingIndicator.determinateRotationRad(0f)))
        .as("§6 — determinate has no continuous spin, only a progress-mapped sweep")
        .isCloseTo(0.0, within(0.001));
    assertThat(Math.toDegrees(ElwhaLoadingIndicator.determinateRotationRad(1f)))
        .as("§6 — a single −180° sweep over the run")
        .isCloseTo(-180.0, within(0.001));
  }

  @Test
  void determinateRotationIsLinearInProgress() {
    assertThat(Math.toDegrees(ElwhaLoadingIndicator.determinateRotationRad(0.5f)))
        .as("§6 — halfway through the run is halfway through the sweep")
        .isCloseTo(-90.0, within(0.001));
  }
}
