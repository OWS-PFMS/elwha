package com.owspfm.elwha.slider;

import static org.assertj.core.api.Assertions.assertThat;

import com.owspfm.elwha.slider.ElwhaSlider.Orientation;
import com.owspfm.elwha.testkit.EdtInterceptor;
import com.owspfm.elwha.testkit.ThemeExtension;
import java.awt.ComponentOrientation;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.junit.jupiter.params.provider.ValueSource;

/**
 * Tier A coverage of the slider's value space — the value&harr;pixel mapping in both orientations,
 * the two independent mirroring rules (a horizontal RTL slider mirrors; a vertical one never does,
 * it always fills bottom-up), the range clamps, and stops snapping.
 *
 * <p>The mapping is asserted through {@code handleCenterX} / {@code valueForX}, the package-private
 * inverse pair every gesture and paint goes through — so a mapping bug shows up here rather than as
 * a handle that lands a few pixels off in a screenshot.
 */
@ExtendWith({EdtInterceptor.class, ThemeExtension.class})
class ElwhaSliderValueSpaceTest {

  private static final int LENGTH = 240;

  /** A slider laid out at a known long-axis extent, so pixel expectations are exact. */
  private static ElwhaSlider sized(final ElwhaSlider slider) {
    if (slider.getOrientation() == Orientation.VERTICAL) {
      slider.setSize(slider.getPreferredSize().width, LENGTH);
    } else {
      slider.setSize(LENGTH, slider.getPreferredSize().height);
    }
    return slider;
  }

  private static ElwhaSlider percent() {
    return sized(new ElwhaSlider(0, 100, 50));
  }

  /** The handle-travel inset — half the resting pill width at either end of the long axis. */
  private static int inset() {
    return ElwhaSlider.HANDLE_WIDTH_PX / 2;
  }

  // ----------------------------------------------------- forward mapping

  @Test
  void theMinimumParksTheHandleAtTheTravelStart() {
    final ElwhaSlider slider = percent();

    slider.setValue(0);

    assertThat(slider.handleCenterX())
        .as("the handle centre inset by its own half-width, so the pill's edge meets the track end")
        .isEqualTo(inset());
  }

  @Test
  void theMaximumParksTheHandleAtTheTravelEnd() {
    final ElwhaSlider slider = percent();

    slider.setValue(100);

    assertThat(slider.handleCenterX())
        .as("symmetrically inset at the far end")
        .isEqualTo(LENGTH - inset());
  }

  @Test
  void theMidpointLandsHalfwayAlongTheTravel() {
    final ElwhaSlider slider = percent();

    slider.setValue(50);

    assertThat(slider.handleCenterX())
        .as("half the value range is half the travel — the mapping is linear")
        .isEqualTo(inset() + (LENGTH - 2 * inset()) / 2);
  }

  @ParameterizedTest
  @ValueSource(ints = {0, 1, 17, 50, 83, 99, 100})
  void valueForXInvertsHandleCenterX(final int value) {
    final ElwhaSlider slider = percent();
    slider.setValue(value);

    assertThat(slider.valueForX(slider.handleCenterX()))
        .as("the pair is a genuine inverse, so a click on the handle never nudges the value")
        .isEqualTo(value);
  }

  @Test
  void aPositionBeyondTheTrackClampsIntoRange() {
    final ElwhaSlider slider = percent();

    assertThat(slider.valueForX(-500)).as("dragging past the start clamps to the minimum").isZero();
    assertThat(slider.valueForX(5000)).as("and past the end clamps to the maximum").isEqualTo(100);
  }

  @Test
  void aDegenerateRangeParksAtTheMinimumRatherThanDividingByZero() {
    final ElwhaSlider slider = sized(new ElwhaSlider(7, 7, 7));

    assertThat(slider.handleCenterX()).as("a zero-width range has one position").isEqualTo(inset());
    assertThat(slider.valueForX(120)).as("and every x maps to it").isEqualTo(7);
  }

  // ------------------------------------------------------------ mirroring

  @Test
  void aRightToLeftHorizontalSliderMirrorsTheTravel() {
    final ElwhaSlider slider = percent();
    slider.setComponentOrientation(ComponentOrientation.RIGHT_TO_LEFT);

    slider.setValue(0);
    assertThat(slider.handleCenterX())
        .as("under RTL the minimum sits at the right-hand end")
        .isEqualTo(LENGTH - inset());

    slider.setValue(100);
    assertThat(slider.handleCenterX()).as("and the maximum at the left").isEqualTo(inset());
  }

  @Test
  void aRightToLeftSlidersInverseMirrorsToo() {
    final ElwhaSlider slider = percent();
    slider.setComponentOrientation(ComponentOrientation.RIGHT_TO_LEFT);

    assertThat(slider.valueForX(inset()))
        .as("a click at the left edge of an RTL slider is the maximum, not the minimum")
        .isEqualTo(100);
  }

  @Test
  void aVerticalSliderIsNeverMirroredByComponentOrientation() {
    final ElwhaSlider slider = new ElwhaSlider(0, 100, 0);
    slider.setOrientation(Orientation.VERTICAL);
    sized(slider);
    final int upright = slider.handleCenterX();

    slider.setComponentOrientation(ComponentOrientation.RIGHT_TO_LEFT);

    assertThat(slider.handleCenterX())
        .as("vertical always fills bottom-up — RTL has no say in it")
        .isEqualTo(upright);
  }

  @Test
  void aVerticalSlidersTravelRunsAlongItsHeight() {
    final ElwhaSlider slider = new ElwhaSlider(0, 100, 100);
    slider.setOrientation(Orientation.VERTICAL);
    sized(slider);

    assertThat(slider.handleCenterX())
        .as("the logical long axis is the height once the paint frame is rotated")
        .isEqualTo(LENGTH - inset());
  }

  // ---------------------------------------------------------------- clamps

  @Test
  void settingAValueOutsideTheRangeClamps() {
    final ElwhaSlider slider = percent();

    slider.setValue(-40);
    assertThat(slider.getValue()).as("below the minimum clamps up").isZero();

    slider.setValue(400);
    assertThat(slider.getValue()).as("above the maximum clamps down").isEqualTo(100);
  }

  @Test
  void movingTheBoundsCarriesTheValueWithThem() {
    final ElwhaSlider slider = percent();

    slider.setMinimum(60);

    assertThat(slider.getValue())
        .as("a value below a raised floor cannot stay where it was")
        .isGreaterThanOrEqualTo(60);
  }

  @Test
  void theModelIsTheSingleSourceOfTruth() {
    final ElwhaSlider slider = percent();

    slider.getModel().setValue(23);

    assertThat(slider.getValue())
        .as("the slider reads through to its BoundedRangeModel — no shadow copy")
        .isEqualTo(23);
  }

  // ----------------------------------------------------------------- stops

  @Test
  void continuousIsTheDefault() {
    final ElwhaSlider slider = percent();

    assertThat(slider.isStopsEnabled()).as("no stops until asked for").isFalse();
    assertThat(slider.getStops()).as("with a zero step").isZero();
  }

  @Test
  void stopsSnapAValueToTheNearestMultipleOfTheStep() {
    final ElwhaSlider slider = percent();
    slider.setStops(25);

    slider.setValue(30);
    assertThat(slider.getValue()).as("30 rounds down to the 25 stop").isEqualTo(25);

    slider.setValue(40);
    assertThat(slider.getValue()).as("and 40 rounds up to the 50 stop").isEqualTo(50);
  }

  @Test
  void stopsAreMeasuredFromTheMinimumNotFromZero() {
    final ElwhaSlider slider = sized(new ElwhaSlider(10, 110, 10));
    slider.setStops(25);

    slider.setValue(38);

    assertThat(slider.getValue())
        .as("the stop grid starts at the minimum, so 10/35/60/... not 0/25/50/...")
        .isEqualTo(35);
  }

  @Test
  void enablingStopsSnapsTheValueAlreadyHeld() {
    final ElwhaSlider slider = percent();
    slider.setValue(37);

    slider.setStops(25);

    assertThat(slider.getValue())
        .as("turning stops on cannot leave the handle between two stops")
        .isEqualTo(25);
  }

  @Test
  void aZeroOrNegativeStepReturnsToContinuous() {
    final ElwhaSlider slider = percent();
    slider.setStops(25);

    slider.setStops(0);
    assertThat(slider.isStopsEnabled()).as("zero disables stops").isFalse();

    slider.setStops(-4);
    assertThat(slider.getStops()).as("and a negative step clamps to the same off state").isZero();
  }

  @Test
  void arrowsAdvanceByOneStopWhileStopsAreOn() {
    final ElwhaSlider slider = percent();
    slider.setUnitIncrement(1);

    assertThat(slider.effectiveUnitIncrement())
        .as("a continuous slider steps by its unit increment")
        .isEqualTo(1);

    slider.setStops(25);
    assertThat(slider.effectiveUnitIncrement())
        .as("stops mode overrides it so an arrow always lands on a stop")
        .isEqualTo(25);
  }

  @Test
  void snappingClampsAtTheRangeEndsRatherThanOvershooting() {
    final ElwhaSlider slider = percent();
    slider.setStops(30);

    slider.setValue(100);

    assertThat(slider.getValue())
        .as("the last stop grid line past the maximum must not push the value out of range")
        .isEqualTo(90);
  }

  // ----------------------------------------------------------- increments

  @Test
  void theIncrementDefaultsAreTheDocumentedOnes() {
    final ElwhaSlider slider = percent();

    assertThat(slider.getUnitIncrement()).as("one unit per arrow").isEqualTo(1);
    assertThat(slider.getBlockIncrement()).as("ten per page key").isEqualTo(10);
  }

  @ParameterizedTest
  @EnumSource(Orientation.class)
  void theOrientationRoundTrips(final Orientation orientation) {
    final ElwhaSlider slider = percent();

    slider.setOrientation(orientation);

    assertThat(slider.getOrientation()).as("%s round-trips", orientation).isEqualTo(orientation);
  }
}
