package com.owspfm.elwha.progress;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.within;

import com.owspfm.elwha.testkit.EdtInterceptor;
import com.owspfm.elwha.testkit.ThemeExtension;
import com.owspfm.elwha.theme.ColorRole;
import com.owspfm.elwha.theme.Easing;
import java.awt.image.BufferedImage;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

/**
 * Tier A coverage of the circular indeterminate choreography — design doc §6: the 6000 ms
 * grow/shrink cycle, the trackless anatomy the S4 build decision settled, and the visible-sweep
 * floor that keeps the arc from vanishing at the turn.
 *
 * <p>Like its linear sibling, every frame is rendered at an <em>injected</em> clock value ({@link
 * PinnedClock}) rather than sampled from a running timer, so "the sweep peaks halfway through the
 * cycle" is checked against the actual halfway frame instead of whichever frame a sleep landed on.
 */
@ExtendWith({EdtInterceptor.class, ThemeExtension.class})
class ElwhaCircularProgressIndeterminateTest {

  private static final int CYCLE_MS = 6000;

  private static PinnedClock.Circular spinner() {
    final PinnedClock.Circular ring = new PinnedClock.Circular();
    ring.setIndeterminate(true);
    return ring;
  }

  private static int sweepDegreesAt(final PinnedClock.Circular ring, final long ms) {
    return Ring.degreesOf(ring.frameAt(ms), ring, ColorRole.PRIMARY.resolve());
  }

  private static boolean sameRaster(final BufferedImage a, final BufferedImage b) {
    for (int y = 0; y < a.getHeight(); y++) {
      for (int x = 0; x < a.getWidth(); x++) {
        if (a.getRGB(x, y) != b.getRGB(x, y)) {
          return false;
        }
      }
    }
    return true;
  }

  // ------------------------------------------------------------ determinism

  @Test
  void paintIsAFunctionOfTheInjectedClockAlone() {
    final PinnedClock.Circular ring = spinner();

    assertThat(sameRaster(ring.frameAt(2200), ring.frameAt(2200)))
        .as("the same clock value paints the same ring — no wall clock in the loop")
        .isTrue();
  }

  @Test
  void differentPointsInTheCyclePaintDifferently() {
    final PinnedClock.Circular ring = spinner();

    assertThat(sameRaster(ring.frameAt(1000), ring.frameAt(2500)))
        .as("§6 — the figure both sweeps and rotates, so no two moments look alike")
        .isFalse();
  }

  // ------------------------------------------------------- the sweep profile

  @Test
  void sweepGrowsThroughTheFirstHalfOfTheCycle() {
    final PinnedClock.Circular ring = spinner();

    final int early = sweepDegreesAt(ring, 300);
    final int middling = sweepDegreesAt(ring, 1500);
    final int peak = sweepDegreesAt(ring, CYCLE_MS / 2);

    assertThat(middling)
        .as("§6 — the arc opens out as the head runs ahead of the tail")
        .isGreaterThan(early);
    assertThat(peak).as("§6 — reaching its widest at the half-cycle").isGreaterThan(middling);
  }

  @Test
  void sweepShrinksThroughTheSecondHalfOfTheCycle() {
    final PinnedClock.Circular ring = spinner();

    final int peak = sweepDegreesAt(ring, CYCLE_MS / 2);
    final int closing = sweepDegreesAt(ring, 4500);
    final int end = sweepDegreesAt(ring, CYCLE_MS - 50);

    assertThat(closing).as("§6 — the tail then catches up, closing the arc").isLessThan(peak);
    assertThat(end).as("and it is nearly shut by the end of the cycle").isLessThan(closing);
  }

  @Test
  void peakSweepStopsShortOfAFullTurn() {
    final PinnedClock.Circular ring = spinner();

    assertThat(sweepDegreesAt(ring, CYCLE_MS / 2))
        .as("§6 — the 0.78-turn cap keeps the arc from closing into a ring at its widest")
        .isBetween(250, 300);
  }

  @Test
  void anArcIsVisibleAtEveryPointOfTheCycle() {
    final PinnedClock.Circular ring = spinner();

    final List<Long> blank = new ArrayList<>();
    for (long ms = 0; ms < CYCLE_MS; ms += 25) {
      if (sweepDegreesAt(ring, ms) == 0) {
        blank.add(ms);
      }
    }

    assertThat(blank)
        .as(
            "§6 — the 6° visible-sweep floor collapses the arc to a dot rather than to nothing, so"
                + " the circular indicator never flashes empty the way the linear one does at its"
                + " cycle boundary")
        .isEmpty();
  }

  @Test
  void arcRotatesEvenWhileItsSweepIsUnchanged() {
    final PinnedClock.Circular ring = spinner();

    // Two moments equidistant from the half-cycle carry the same sweep by construction; only the
    // rotation distinguishes them.
    final BufferedImage before = ring.frameAt(2000);
    final BufferedImage after = ring.frameAt(4000);

    assertThat(sameRaster(before, after))
        .as("§6 — the 1080°-per-cycle base rotation plus the advance kicks keep the figure moving")
        .isFalse();
  }

  // ---------------------------------------------------------- the anatomy

  @Test
  void noTrackIsPaintedWhileIndeterminate() {
    final PinnedClock.Circular ring = spinner();
    // Probed over a dark ground: see PinnedClock.Circular#frameAt(long, Color) — the arc's own
    // anti-aliased fringe over the light default ground aliases onto the track color.
    final java.awt.Color contrastingGround = ColorRole.INVERSE_SURFACE.resolve();

    for (long ms = 0; ms < CYCLE_MS; ms += 250) {
      assertThat(
              Ring.degreesOf(
                  ring.frameAt(ms, contrastingGround),
                  ring,
                  ColorRole.SECONDARY_CONTAINER.resolve()))
          .as("§6 (S4) — the visible-track anatomy is determinate-only, at %d ms", ms)
          .isZero();
    }
  }

  @Test
  void indicatorRoleStillDrivesTheArc() {
    final PinnedClock.Circular ring = spinner();
    ring.setIndicatorColorRole(ColorRole.TERTIARY);

    assertThat(Ring.degreesOf(ring.frameAt(3000), ring, ColorRole.TERTIARY.resolve()))
        .as("re-roling the active indicator applies in indeterminate mode too")
        .isGreaterThan(100);
  }

  @Test
  void switchingBackToDeterminateRestoresTheTrack() {
    final PinnedClock.Circular ring = spinner();
    ring.frameAt(1500);

    ring.setIndeterminate(false);
    ring.setValue(50);

    assertThat(Ring.degreesOf(ring.frameAt(0), ring, ColorRole.SECONDARY_CONTAINER.resolve()))
        .as("the track comes back with the value chrome")
        .isGreaterThan(100);
  }

  @Test
  void modelSurvivesTheModeRoundTrip() {
    final ElwhaCircularProgressIndicator ring = new ElwhaCircularProgressIndicator(0, 100, 70);

    ring.setIndeterminate(true);
    ring.setIndeterminate(false);

    assertThat(ring.getValue()).as("mode is a display concern, not a model one").isEqualTo(70);
  }

  // ---------------------------------------------- additional-rotation kicks

  /**
   * The original per-frame summation, kept as the oracle for the closed form (#622). Every kick
   * since the timeline began is evaluated and added, so this grows without bound.
   */
  private static double summedRotationDeg(final long elapsedMs) {
    final long kicks = elapsedMs / 1500;
    double total = 0;
    for (long i = 1; i <= kicks; i++) {
      total += Easing.STANDARD.ease(Math.min(1f, (elapsedMs - i * 1500) / 300f)) * 360.0;
    }
    return total;
  }

  private static double turnOf(final double degrees) {
    return ((degrees % 360.0) + 360.0) % 360.0;
  }

  @ParameterizedTest
  @ValueSource(
      longs = {0, 1, 1499, 1500, 1501, 1650, 1799, 1800, 1801, 3000, 7400, 45_000, 150_000})
  void theClosedFormAgreesWithSummingEveryKick(final long elapsedMs) {
    assertThat((double) ElwhaCircularProgressIndicator.additionalRotationDeg(elapsedMs))
        .as(
            "#622 — settled kicks each contribute exactly one whole turn, so counting them is the "
                + "same painted angle as summing them")
        .isCloseTo(turnOf(summedRotationDeg(elapsedMs)), within(0.001));
  }

  @Test
  void theKickTotalStaysBoundedHoweverLongTheRingHasSpun() {
    // Eight hours in. The old sum ran ~19,200 eased evaluations per frame here and had accumulated
    // far past the float precision where one degree is a single ULP.
    final long eightHours = 8L * 60 * 60 * 1000;

    final float total = ElwhaCircularProgressIndicator.additionalRotationDeg(eightHours);

    assertThat(total).as("the angle is reduced to a single turn").isBetween(0f, 360f);
    assertThat((double) total)
        .as("and still lands where the settled-plus-easing decomposition says it should")
        .isCloseTo(turnOf(summedRotationDeg(eightHours % 1500 + 3 * 1500)), within(0.001));
  }

  @Test
  void aLongRunningSpinKeepsMovingFrameToFrame() {
    final PinnedClock.Circular ring = spinner();
    final long dayIn = 24L * 60 * 60 * 1000;

    final BufferedImage first = ring.frameAt(dayIn);
    final BufferedImage next = ring.frameAt(dayIn + 100);

    assertThat(sameRaster(first, next))
        .as("#622 — the global rotation used to quantize once its float ULP passed one degree")
        .isFalse();
  }

  @Test
  void clockNeverRunsWhileTheIndicatorIsNotShowing() {
    final ElwhaCircularProgressIndicator ring = ElwhaCircularProgressIndicator.indeterminate();

    assertThat(ring.isAnimating())
        .as("§6 — hierarchy-gated, so an off-screen spinner costs nothing")
        .isFalse();
  }
}
