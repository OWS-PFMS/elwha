package com.owspfm.elwha.progress;

import static org.assertj.core.api.Assertions.assertThat;

import com.owspfm.elwha.testkit.EdtInterceptor;
import com.owspfm.elwha.testkit.Pixels;
import com.owspfm.elwha.testkit.ThemeExtension;
import com.owspfm.elwha.theme.ColorRole;
import java.awt.Color;
import java.awt.image.BufferedImage;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

/**
 * Tier A coverage of the linear indeterminate choreography — design doc §6: the 1750 ms two-line
 * cycle, its track spans and flanking gaps, and the determinate round-trip.
 *
 * <p><strong>This class is the deterministic replacement for {@code
 * ElwhaLinearProgressIndeterminateSmoke}</strong>, the one reproducibly-flaky incumbent. That smoke
 * slept 60 ms between samples for 1.9 s and asserted "a line is visible in nearly every frame",
 * with a tolerance of three blank samples. The cycle used to open with a genuinely blank window —
 * the first line's head needs ~84 ms to clear half a pixel under {@code EMPHASIZED_ACCELERATE} and
 * the second line has not started — so how many blank samples a run collected depended on where the
 * sleeps happened to land. That is a race, and it failed about three runs in five.
 *
 * <p>Every frame below is rendered at an <em>injected</em> clock value ({@link PinnedClock}), so
 * "visible at 400 ms" is a fact rather than a sample. #576 closed the window itself by flooring an
 * emerging line at one capsule cap, so the property asserted here is now the strong one:
 * <em>every</em> millisecond of the cycle paints a line.
 */
@ExtendWith({EdtInterceptor.class, ThemeExtension.class})
class ElwhaLinearProgressIndeterminateTest {

  private static final int WIDTH = ElwhaLinearProgressIndicator.PREFERRED_WIDTH_PX;
  private static final int MID_Y = 2;
  private static final int CYCLE_MS = 1750;

  private static PinnedClock.Linear indeterminateBar() {
    final PinnedClock.Linear bar = new PinnedClock.Linear();
    bar.setIndeterminate(true);
    return bar;
  }

  private static boolean near(final BufferedImage image, final int x, final Color want) {
    final Color got = new Color(image.getRGB(x, MID_Y), true);
    return Math.abs(got.getRed() - want.getRed()) <= 10
        && Math.abs(got.getGreen() - want.getGreen()) <= 10
        && Math.abs(got.getBlue() - want.getBlue()) <= 10;
  }

  /** Start x of every run of {@code want} along the indicator's mid row. */
  private static List<Integer> runsOf(final BufferedImage image, final Color want) {
    final List<Integer> starts = new ArrayList<>();
    boolean inside = false;
    for (int x = 0; x < WIDTH; x++) {
      final boolean hit = near(image, x, want);
      if (hit && !inside) {
        starts.add(x);
      }
      inside = hit;
    }
    return starts;
  }

  /** Total px along the mid row painted in the active role. */
  private static int activeWidth(final BufferedImage image) {
    int count = 0;
    for (int x = 0; x < WIDTH; x++) {
      if (near(image, x, ColorRole.PRIMARY.resolve())) {
        count++;
      }
    }
    return count;
  }

  private static int activeRunCount(final BufferedImage image) {
    return runsOf(image, ColorRole.PRIMARY.resolve()).size();
  }

  private static boolean sameRaster(final BufferedImage a, final BufferedImage b) {
    if (a.getWidth() != b.getWidth() || a.getHeight() != b.getHeight()) {
      return false;
    }
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
    final PinnedClock.Linear bar = indeterminateBar();

    final BufferedImage first = bar.frameAt(600);
    final BufferedImage again = bar.frameAt(600);

    assertThat(sameRaster(first, again))
        .as("the same clock value paints the same pixels — nothing here reads the wall clock")
        .isTrue();
  }

  @Test
  void twoIndicatorsAtTheSameClockValuePaintIdentically() {
    final BufferedImage one = indeterminateBar().frameAt(900);
    final BufferedImage other = indeterminateBar().frameAt(900);

    assertThat(sameRaster(one, other))
        .as("no per-instance state leaks into the frame; the timeline fully determines it")
        .isTrue();
  }

  @Test
  void cycleRepeatsExactlyOncePerCycleLength() {
    final PinnedClock.Linear bar = indeterminateBar();

    assertThat(sameRaster(bar.frameAt(400), bar.frameAt(400 + CYCLE_MS)))
        .as("§6 — the linear timeline is a pure loop, so one cycle later is the same frame")
        .isTrue();
    assertThat(sameRaster(bar.frameAt(1200), bar.frameAt(1200 + 3 * CYCLE_MS)))
        .as("and it stays in phase after several cycles")
        .isTrue();
  }

  @Test
  void differentPointsInTheCyclePaintDifferently() {
    final PinnedClock.Linear bar = indeterminateBar();

    assertThat(sameRaster(bar.frameAt(400), bar.frameAt(800)))
        .as("the choreography actually moves — this is the motion the smoke sampled for")
        .isFalse();
  }

  @Test
  void leadingEdgeAdvancesAcrossTheRun() {
    final PinnedClock.Linear bar = indeterminateBar();

    // The rightmost run, not the leftmost: by 700 ms the second line has emerged at the leading
    // edge (#576's floor makes that visible), so run 0 is no longer the line under test.
    final List<Integer> at300 = runsOf(bar.frameAt(300), ColorRole.PRIMARY.resolve());
    final List<Integer> at700 = runsOf(bar.frameAt(700), ColorRole.PRIMARY.resolve());
    final int earlier = at300.get(at300.size() - 1);
    final int later = at700.get(at700.size() - 1);

    assertThat(later)
        .as("§6 — the first line sweeps toward the trailing end as the cycle runs")
        .isGreaterThan(earlier);
  }

  // -------------------------------------------------- the flake class, closed

  @Test
  void aLineIsVisibleAtEveryMillisecondOfTheCycle() {
    final PinnedClock.Linear bar = indeterminateBar();

    final List<Long> blank = new ArrayList<>();
    for (long ms = 0; ms < CYCLE_MS; ms++) {
      if (activeRunCount(bar.frameAt(ms)) == 0) {
        blank.add(ms);
      }
    }

    assertThat(blank)
        .as(
            "every one of the %d ms of the cycle paints an active line — the property the "
                + "incumbent smoke could only sample, and #576 closed the last hole in",
            CYCLE_MS)
        .isEmpty();
  }

  /**
   * #576 — the cycle used to open with ~84 ms of empty bar, recurring every 1750 ms. It followed
   * from the spec-mandated easing rather than from a coding error: the first line's span is {@code
   * [tail=0, head]}, and {@code EMPHASIZED_ACCELERATE} keeps the head under half a pixel for that
   * long, while the second line does not start until 650 ms. Retiming the channels would have meant
   * abandoning the Compose keyframe constants, so instead an emerging line is floored at one
   * capsule cap — the smallest a round-ended M3 indicator can honestly be.
   */
  @Test
  void anEmergingLineIsFlooredAtOneCapsuleCapRatherThanCulled() {
    final PinnedClock.Linear bar = indeterminateBar();

    final List<Integer> atCycleStart = runsOf(bar.frameAt(0), ColorRole.PRIMARY.resolve());

    assertThat(atCycleStart)
        .as("the very first millisecond of the cycle already paints the first line's leading cap")
        .isNotEmpty();
    assertThat(atCycleStart.get(0))
        .as("and it is born at the leading edge, where the span's tail is pinned")
        .isLessThanOrEqualTo(2);
  }

  @Test
  void theFloorStopsBitingOnceTheLineHasRealWidth() {
    final PinnedClock.Linear bar = indeterminateBar();

    final int nub = activeWidth(bar.frameAt(0));
    final int grown = activeWidth(bar.frameAt(400));

    assertThat(nub)
        .as("an emerging line is one cap wide — the track thickness, not a stub of arbitrary size")
        .isLessThanOrEqualTo(8);
    assertThat(grown)
        .as("and the floor is a no-op the moment the natural span passes the track thickness")
        .isGreaterThan(nub * 4);
  }

  @Test
  void aFinishingLineStillCollapsesToNothing() {
    final PinnedClock.Linear bar = indeterminateBar();

    final int late = activeWidth(bar.frameAt(1740));
    final int earlier = activeWidth(bar.frameAt(1500));

    assertThat(late)
        .as(
            "the floor is for a line coming in, not one going out — the second line's tail must "
                + "still be able to catch its head and vanish at the trailing edge")
        .isLessThan(earlier);
  }

  @Test
  void twoLineMomentOccursDuringTheCycle() {
    final PinnedClock.Linear bar = indeterminateBar();

    long twoLineFrames = 0;
    for (long ms = 0; ms < CYCLE_MS; ms += 5) {
      if (activeRunCount(bar.frameAt(ms)) >= 2) {
        twoLineFrames++;
      }
    }

    assertThat(twoLineFrames)
        .as("§6 — the second line overlaps the first for part of the cycle")
        .isPositive();
  }

  @Test
  void cycleNeverPaintsMoreThanTwoLines() {
    final PinnedClock.Linear bar = indeterminateBar();

    long worst = 0;
    for (long ms = 0; ms < CYCLE_MS; ms += 5) {
      worst = Math.max(worst, activeRunCount(bar.frameAt(ms)));
    }

    assertThat(worst).as("§6 — it is a two-line choreography, exhaustively").isEqualTo(2);
  }

  // ------------------------------------------------------------- the anatomy

  @Test
  void trackSpansFillTheRunAroundTheLines() {
    final BufferedImage frame = indeterminateBar().frameAt(600);

    assertThat(runsOf(frame, ColorRole.SECONDARY_CONTAINER.resolve()))
        .as("§6 — the linear indeterminate keeps its track, unlike the circular one")
        .isNotEmpty();
  }

  @Test
  void everyActiveLineIsFlankedByAnEmptyGap() {
    final BufferedImage frame = indeterminateBar().frameAt(600);
    final Color primary = ColorRole.PRIMARY.resolve();
    final Color track = ColorRole.SECONDARY_CONTAINER.resolve();

    boolean sawGap = false;
    for (int x = 1; x < WIDTH; x++) {
      if (near(frame, x, primary) && !near(frame, x - 1, primary)) {
        // One pixel back from a line's leading edge must be neither line nor track: the 4 dp
        // TrackActiveSpace is a hole in the chrome, not a butt joint.
        assertThat(near(frame, x - 2, track))
            .as("§6 — the track stops short of the line by the track-active space")
            .isFalse();
        sawGap = true;
      }
    }
    assertThat(sawGap).as("the frame under test actually contains a line edge to check").isTrue();
  }

  @Test
  void noStopDotIsPaintedWhileIndeterminate() {
    final PinnedClock.Linear bar = indeterminateBar();

    // The stop dot lives in the last 4 px of the run. Early in the cycle the lines are nowhere
    // near the trailing end, so anything primary out there would have to be the dot.
    final BufferedImage frame = bar.frameAt(200);

    assertThat(near(frame, WIDTH - 2, ColorRole.PRIMARY.resolve()))
        .as("§5 — the stop indicator is determinate-only")
        .isFalse();
  }

  @Test
  void runIsFullyCoveredByLinesTrackAndGapsOnly() {
    final BufferedImage frame = indeterminateBar().frameAt(1000);
    final Color primary = ColorRole.PRIMARY.resolve();
    final Color track = ColorRole.SECONDARY_CONTAINER.resolve();
    final Color ground = ColorRole.SURFACE.resolve();

    int unexplained = 0;
    for (int x = 0; x < WIDTH; x++) {
      if (!near(frame, x, primary) && !near(frame, x, track) && !near(frame, x, ground)) {
        unexplained++;
      }
    }

    assertThat(unexplained)
        .as("only anti-aliased edges may fall outside the three anatomy colors")
        .isLessThan(WIDTH / 8);
  }

  // ------------------------------------------------------------- mode switch

  @Test
  void switchingToIndeterminateLeavesTheModelAlone() {
    final ElwhaLinearProgressIndicator bar = new ElwhaLinearProgressIndicator(0, 100, 60);

    bar.setIndeterminate(true);

    assertThat(bar.getValue())
        .as("the mode is a display concern; the value survives to be resumed")
        .isEqualTo(60);
  }

  @Test
  void switchingBackToDeterminateRestoresTheValueChrome() {
    final ElwhaLinearProgressIndicator bar = new ElwhaLinearProgressIndicator(0, 100, 60);
    bar.setIndeterminate(true);
    Pixels.render(bar, WIDTH, bar.getPreferredSize().height);

    bar.setIndeterminate(false);
    final BufferedImage back = Pixels.render(bar, WIDTH, bar.getPreferredSize().height);

    assertThat(near(back, 8, ColorRole.PRIMARY.resolve()))
        .as("the active fill returns at the model's value")
        .isTrue();
    assertThat(near(back, WIDTH - 2, ColorRole.PRIMARY.resolve()))
        .as("and so does the stop dot")
        .isTrue();
  }

  @Test
  void indeterminateTimelineRestartsOnEachModeEntry() {
    final PinnedClock.Linear bar = new PinnedClock.Linear();
    bar.setIndeterminate(true);
    final BufferedImage atStart = bar.frameAt(0);

    bar.setIndeterminate(false);
    bar.setIndeterminate(true);

    assertThat(sameRaster(bar.frameAt(0), atStart))
        .as("re-entering indeterminate re-anchors the cycle rather than resuming mid-choreography")
        .isTrue();
  }

  @Test
  void clockNeverRunsWhileTheIndicatorIsNotShowing() {
    final ElwhaLinearProgressIndicator bar = ElwhaLinearProgressIndicator.indeterminate();

    assertThat(bar.isAnimating())
        .as("§6 — the timer is hierarchy-gated, so an off-screen indicator costs nothing")
        .isFalse();
  }
}
