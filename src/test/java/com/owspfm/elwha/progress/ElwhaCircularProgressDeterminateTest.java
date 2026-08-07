package com.owspfm.elwha.progress;

import static org.assertj.core.api.Assertions.assertThat;

import com.owspfm.elwha.testkit.EdtInterceptor;
import com.owspfm.elwha.testkit.Pixels;
import com.owspfm.elwha.testkit.ThemeExtension;
import com.owspfm.elwha.theme.ColorRole;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.image.BufferedImage;
import javax.swing.BorderFactory;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

/**
 * Tier A coverage of the circular determinate ring — design doc §5: the diameter table, the
 * value→sweep mapping starting at twelve o'clock and running clockwise, the cap-aware gap at
 * <em>both</em> arc ends, and the seamless-ring endpoints at 0% and 100%.
 *
 * <p>Chrome is probed along the stroke centerline via {@link Ring} rather than at hand-picked
 * cartesian points, so an assertion reads as "at 45° around the ring" — the frame the spec is
 * written in.
 */
@ExtendWith({EdtInterceptor.class, ThemeExtension.class})
class ElwhaCircularProgressDeterminateTest {

  private static Color active() {
    return ColorRole.PRIMARY.resolve();
  }

  private static Color track() {
    return ColorRole.SECONDARY_CONTAINER.resolve();
  }

  private static ElwhaCircularProgressIndicator ringAt(final int percent) {
    return new ElwhaCircularProgressIndicator(0, 100, percent);
  }

  private static BufferedImage render(final ElwhaCircularProgressIndicator ring) {
    final int d = ring.getPreferredSize().width;
    return Pixels.render(ring, d, d);
  }

  // --------------------------------------------------------- diameter table

  @ParameterizedTest
  @CsvSource({
    // thickness, wavy, expected diameter — the design §5 redline table (40 / 44 / 48 / 52)
    "4, false, 40",
    "8, false, 44",
    "4, true, 48",
    "8, true, 52"
  })
  void diameterGrowsWithThicknessAndTheWavyShape(
      final int thickness, final boolean wavy, final int diameter) {
    final ElwhaCircularProgressIndicator ring = new ElwhaCircularProgressIndicator();
    ring.setTrackThickness(thickness);
    ring.setWavy(wavy);

    assertThat(ring.getDiameter())
        .as("§5 — thickness past the 4 dp default grows the ring 1:1; the wave band adds 8")
        .isEqualTo(diameter);
  }

  @Test
  void ringIsSquareAndDoesNotStretch() {
    final ElwhaCircularProgressIndicator ring = new ElwhaCircularProgressIndicator();
    final Dimension expected =
        new Dimension(
            ElwhaCircularProgressIndicator.SIZE_DEFAULT_PX,
            ElwhaCircularProgressIndicator.SIZE_DEFAULT_PX);

    assertThat(ring.getPreferredSize()).isEqualTo(expected);
    assertThat(ring.getMinimumSize())
        .as("§5 — a fixed-size widget: minimum, preferred and maximum all agree")
        .isEqualTo(expected);
    assertThat(ring.getMaximumSize()).isEqualTo(expected);
  }

  @Test
  void insetsAreAddedAroundTheRing() {
    final ElwhaCircularProgressIndicator ring = new ElwhaCircularProgressIndicator();
    ring.setBorder(BorderFactory.createEmptyBorder(6, 6, 6, 6));

    assertThat(ring.getPreferredSize()).isEqualTo(new Dimension(52, 52));
  }

  @Test
  void baseSizeIsClampedToSomethingPaintable() {
    final ElwhaCircularProgressIndicator ring = new ElwhaCircularProgressIndicator();

    ring.setIndicatorSize(1);

    assertThat(ring.getIndicatorSize())
        .as("a sub-stroke diameter has no ring left to draw, so the setter floors it")
        .isEqualTo(8);
  }

  // ------------------------------------------------------ value → sweep

  @Test
  void anEmptyRingIsASeamlessTrackCircle() {
    final BufferedImage image = render(ringAt(0));
    final ElwhaCircularProgressIndicator ring = ringAt(0);

    assertThat(Ring.degreesOf(image, ring, track()))
        .as("§5 — at 0% the track closes into an unbroken ring, with no gap to interrupt it")
        .isGreaterThan(350);
    assertThat(Ring.degreesOf(image, ring, active()))
        .as("and there is no active arc at all")
        .isZero();
  }

  @Test
  void aFullRingIsASeamlessActiveCircle() {
    final BufferedImage image = render(ringAt(100));
    final ElwhaCircularProgressIndicator ring = ringAt(100);

    assertThat(Ring.degreesOf(image, ring, active()))
        .as("§5 — at 100% the active indicator closes the ring instead")
        .isGreaterThan(350);
    assertThat(Ring.degreesOf(image, ring, track())).as("and the track is fully covered").isZero();
  }

  @Test
  void activeArcStartsAtTwelveOClock() {
    final ElwhaCircularProgressIndicator ring = ringAt(25);
    final BufferedImage image = render(ring);

    // 90° itself lands on the round cap's anti-aliased tip, so the pair of probes brackets the
    // start rather than standing on it: solid one degree inside, nothing a few degrees outside.
    assertThat(Ring.isAt(image, ring, 88, active()))
        .as("§5 — twelve o'clock is the origin of the sweep")
        .isTrue();
    assertThat(Ring.isAt(image, ring, 95, active()))
        .as("§5 — and the arc does not spill back past it")
        .isFalse();
  }

  @Test
  void activeArcRunsClockwiseFromTheTop() {
    final ElwhaCircularProgressIndicator ring = ringAt(25);
    final BufferedImage image = render(ring);

    assertThat(Ring.isAt(image, ring, 45, active()))
        .as("§5 — a quarter turn covers twelve to three o'clock, i.e. clockwise")
        .isTrue();
    assertThat(Ring.isAt(image, ring, 135, active()))
        .as("§5 — and not counter-clockwise into the eleven o'clock side")
        .isFalse();
  }

  @ParameterizedTest
  @CsvSource({"25, 45", "50, 0", "75, 315"})
  void sweptArcCoversItsFractionOfTheRing(final int percent, final int midAngle) {
    final ElwhaCircularProgressIndicator ring = ringAt(percent);
    final BufferedImage image = render(ring);

    assertThat(Ring.isAt(image, ring, midAngle, active()))
        .as("§5 — %d%% sweeps through %d° on the ring", percent, midAngle)
        .isTrue();
  }

  @Test
  void activeArcLengthTracksTheValue() {
    final ElwhaCircularProgressIndicator quarter = ringAt(25);
    final ElwhaCircularProgressIndicator half = ringAt(50);

    final int quarterDegrees = Ring.degreesOf(render(quarter), quarter, active());
    final int halfDegrees = Ring.degreesOf(render(half), half, active());

    assertThat(quarterDegrees)
        .as("§5 — a quarter of the run is about a quarter of the ring")
        .isBetween(80, 100);
    assertThat(halfDegrees).as("§5 — and half of it about half the ring").isBetween(170, 190);
  }

  @Test
  void trackTakesTheRemainderLessBothGaps() {
    final ElwhaCircularProgressIndicator ring = ringAt(50);
    final BufferedImage image = render(ring);

    final int activeDegrees = Ring.degreesOf(image, ring, active());
    final int trackDegrees = Ring.degreesOf(image, ring, track());

    assertThat(activeDegrees + trackDegrees)
        .as("§5 — the gap is cut from both arc ends, so the two arcs never sum to a full circle")
        .isLessThan(340);
    assertThat(trackDegrees).as("but the track is still clearly present").isGreaterThan(100);
  }

  @Test
  void aGapSeparatesTheArcsOnBothSidesOfTheActiveIndicator() {
    final ElwhaCircularProgressIndicator ring = ringAt(50);
    final BufferedImage image = render(ring);

    // At 50% the arc sweeps clockwise from 90° through 0° to 270°, so its tail joint is just
    // counter-clockwise of 90° and its head joint just clockwise of 270°. Both gaps are probed
    // from the far side of the joint, away from the round caps' anti-aliased tips.
    assertThat(Ring.isAt(image, ring, 105, active()) || Ring.isAt(image, ring, 105, track()))
        .as("§5 — the track-active space at the arc's tail end is a real hole in the chrome")
        .isFalse();
    assertThat(Ring.isAt(image, ring, 258, active()) || Ring.isAt(image, ring, 258, track()))
        .as("§5 — and the head end has its own, which is what 'cap-aware, both ends' means")
        .isFalse();
  }

  @Test
  void aWiderGapTakesMoreOutOfTheTrack() {
    final ElwhaCircularProgressIndicator narrow = ringAt(50);
    final ElwhaCircularProgressIndicator wide = ringAt(50);
    wide.setIndicatorTrackGapSize(12);

    assertThat(Ring.degreesOf(render(wide), wide, track()))
        .as("the gap token is subtracted from the track, not the active arc")
        .isLessThan(Ring.degreesOf(render(narrow), narrow, track()));
  }

  // -------------------------------------------------------------- color axis

  @Test
  void arcRolesAreOverridable() {
    final ElwhaCircularProgressIndicator ring = ringAt(50);
    ring.setIndicatorColorRole(ColorRole.TERTIARY);
    ring.setTrackColorRole(ColorRole.ERROR_CONTAINER);

    final BufferedImage image = render(ring);

    assertThat(Ring.isAt(image, ring, 0, ColorRole.TERTIARY.resolve()))
        .as("the active role reaches the paint")
        .isTrue();
    assertThat(Ring.isAt(image, ring, 180, ColorRole.ERROR_CONTAINER.resolve()))
        .as("and so does the track role")
        .isTrue();
  }

  @Test
  void ringHasNoStopIndicatorConcept() {
    final ElwhaCircularProgressIndicator ring = ringAt(0);
    final BufferedImage image = render(ring);

    assertThat(Ring.degreesOf(image, ring, active()))
        .as("§5 — the stop dot is a linear-only part; an empty ring carries no primary at all")
        .isZero();
  }

  // -------------------------------------------------------------- degenerate

  @Test
  void aRingWithNoRoomLeftPaintsNothingRatherThanThrowing() {
    final ElwhaCircularProgressIndicator ring = ringAt(50);

    final BufferedImage image = Pixels.render(ring, 3, 3);

    assertThat(image.getWidth())
        .as("painting a ring into a 3 px box is a no-op, not a crash")
        .isEqualTo(3);
  }
}
