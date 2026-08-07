package com.owspfm.elwha.switches;

import static org.assertj.core.api.Assertions.assertThat;

import com.owspfm.elwha.testkit.EdtInterceptor;
import com.owspfm.elwha.testkit.Input;
import com.owspfm.elwha.testkit.ThemeExtension;
import java.awt.ComponentOrientation;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

/**
 * Tier A coverage of the drag gesture's edges — the threshold that separates a sloppy click from a
 * scrub, what a drag does that a click does not, and the guards around a gesture that starts or
 * ends on a disabled switch. The {@code ElwhaSwitchInteractionTest} spike covers the happy path
 * (drag commits to the nearest half); everything here is the case work around it.
 */
@ExtendWith({EdtInterceptor.class, ThemeExtension.class})
class ElwhaSwitchDragTest {

  private static final int WIDTH = 60;
  private static final int HEIGHT = 40;
  private static final int CENTER_Y = 20;
  private static final int TRAVEL_START_X = 20;
  private static final int TRAVEL_END_X = 40;

  private static ElwhaSwitch sized(final ElwhaSwitch toggle) {
    toggle.setSize(WIDTH, HEIGHT);
    return toggle;
  }

  // ------------------------------------------------------------- threshold

  @Test
  void aMovementUnderTheThresholdIsStillAClick() {
    final ElwhaSwitch toggle = sized(new ElwhaSwitch());
    final int[] actions = {0};
    toggle.addActionListener(e -> actions[0]++);

    Input.press(toggle, 30, CENTER_Y);
    Input.drag(toggle, 30 + ElwhaSwitch.DRAG_THRESHOLD_PX - 1, CENTER_Y);
    Input.release(toggle, 30 + ElwhaSwitch.DRAG_THRESHOLD_PX - 1, CENTER_Y);

    assertThat(toggle.isSelected())
        .as("a shaky hand should not turn a tap into a scrub that lands back where it started")
        .isTrue();
    assertThat(actions[0]).as("and it commits once, as a click").isEqualTo(1);
  }

  @Test
  void aMovementAtTheThresholdBecomesADrag() {
    final ElwhaSwitch toggle = sized(new ElwhaSwitch());
    final int[] actions = {0};
    toggle.addActionListener(e -> actions[0]++);

    Input.press(toggle, TRAVEL_START_X, CENTER_Y);
    Input.drag(toggle, TRAVEL_START_X + ElwhaSwitch.DRAG_THRESHOLD_PX, CENTER_Y);
    Input.release(toggle, TRAVEL_START_X + ElwhaSwitch.DRAG_THRESHOLD_PX, CENTER_Y);

    assertThat(toggle.isSelected())
        .as("once it is a drag the commit is by position, and this one never crossed the midpoint")
        .isFalse();
    assertThat(actions[0]).as("so nothing was committed and nothing fired").isZero();
  }

  @Test
  void dragThresholdIsMeasuredFromThePressPointNotTheHandle() {
    final ElwhaSwitch toggle = sized(new ElwhaSwitch());

    Input.press(toggle, TRAVEL_END_X, CENTER_Y);
    Input.drag(toggle, TRAVEL_END_X - 1, CENTER_Y);
    Input.release(toggle, TRAVEL_END_X - 1, CENTER_Y);

    assertThat(toggle.isSelected())
        .as("pressing far from the handle and barely moving is a click on the track, not a scrub")
        .isTrue();
  }

  // ----------------------------------------------------------- drag versus click

  @Test
  void aDragCommitsEvenWhenTheReleaseLandsOutsideTheBounds() {
    final ElwhaSwitch toggle = sized(new ElwhaSwitch());
    final int[] actions = {0};
    toggle.addActionListener(e -> actions[0]++);

    Input.press(toggle, TRAVEL_START_X, CENTER_Y);
    Input.drag(toggle, WIDTH + 200, CENTER_Y);
    Input.release(toggle, WIDTH + 200, CENTER_Y);

    assertThat(toggle.isSelected())
        .as(
            "a scrub past the end of the track still commits — the finger left the widget, but the"
                + " gesture was complete")
        .isTrue();
    assertThat(actions[0]).as("firing once").isEqualTo(1);
  }

  @Test
  void aPlainClickReleasedOutsideTheBoundsIsCancelled() {
    final ElwhaSwitch toggle = sized(new ElwhaSwitch());
    final int[] actions = {0};
    toggle.addActionListener(e -> actions[0]++);

    Input.press(toggle, 30, CENTER_Y);
    Input.release(toggle, WIDTH + 200, CENTER_Y);

    assertThat(toggle.isSelected())
        .as("with no drag in between, leaving the bounds abandons the gesture")
        .isFalse();
    assertThat(actions[0]).as("and fires nothing").isZero();
  }

  @Test
  void aDragBackToTheStartingHalfCommitsNothing() {
    final ElwhaSwitch toggle = sized(new ElwhaSwitch(true));
    final int[] events = {0};
    toggle.addActionListener(e -> events[0]++);
    toggle.addChangeListener(e -> events[0]++);

    Input.press(toggle, TRAVEL_END_X, CENTER_Y);
    Input.drag(toggle, TRAVEL_END_X - 8, CENTER_Y);
    Input.release(toggle, TRAVEL_END_X - 8, CENTER_Y);

    assertThat(toggle.isSelected()).as("the state is unchanged").isTrue();
    assertThat(events[0])
        .as("a scrub that lands back on its own half fires nothing at all — not even a change")
        .isZero();
  }

  @Test
  void aDragWithNoArmedPressIsIgnored() {
    final ElwhaSwitch toggle = sized(new ElwhaSwitch());

    Input.drag(toggle, TRAVEL_END_X, CENTER_Y);
    Input.release(toggle, TRAVEL_END_X, CENTER_Y);

    assertThat(toggle.isSelected())
        .as("a drag event arriving without a press behind it starts no gesture")
        .isFalse();
  }

  // --------------------------------------------------------------- disabled

  @Test
  void aDisabledSwitchStartsNoGesture() {
    final ElwhaSwitch toggle = sized(new ElwhaSwitch());
    toggle.setEnabled(false);
    final int[] events = {0};
    toggle.addActionListener(e -> events[0]++);
    toggle.addChangeListener(e -> events[0]++);

    Input.press(toggle, TRAVEL_START_X, CENTER_Y);
    Input.drag(toggle, TRAVEL_END_X, CENTER_Y);
    Input.release(toggle, TRAVEL_END_X, CENTER_Y);

    assertThat(toggle.isSelected()).as("a disabled switch never commits a drag").isFalse();
    assertThat(events[0]).as("and fires nothing").isZero();
  }

  @Test
  void disablingMidGestureAbandonsTheCommit() {
    final ElwhaSwitch toggle = sized(new ElwhaSwitch());
    final int[] events = {0};
    toggle.addActionListener(e -> events[0]++);
    toggle.addChangeListener(e -> events[0]++);

    Input.press(toggle, TRAVEL_START_X, CENTER_Y);
    Input.drag(toggle, TRAVEL_END_X, CENTER_Y);
    toggle.setEnabled(false);
    Input.release(toggle, TRAVEL_END_X, CENTER_Y);

    assertThat(toggle.isSelected())
        .as("a switch disabled while the pointer is down drops the in-flight gesture on release")
        .isFalse();
    assertThat(events[0]).as("and fires nothing").isZero();
  }

  @Test
  void disablingMidGestureStillEndsThePress() {
    final ElwhaSwitch toggle = sized(new ElwhaSwitch());

    Input.press(toggle, TRAVEL_START_X, CENTER_Y);
    toggle.setEnabled(false);
    Input.release(toggle, TRAVEL_START_X, CENTER_Y);
    toggle.setEnabled(true);
    Input.release(toggle, TRAVEL_START_X, CENTER_Y);

    assertThat(toggle.isSelected())
        .as("the abandoned press is cleared, so a later stray release cannot resurrect it")
        .isFalse();
  }

  // --------------------------------------------------------------------- RTL

  @Test
  void aMirroredDragCommitsByValueNotByPixelDirection() {
    final ElwhaSwitch toggle = sized(new ElwhaSwitch(true));
    toggle.setComponentOrientation(ComponentOrientation.RIGHT_TO_LEFT);

    Input.press(toggle, TRAVEL_START_X, CENTER_Y);
    Input.drag(toggle, TRAVEL_END_X, CENTER_Y);
    Input.release(toggle, TRAVEL_END_X, CENTER_Y);

    assertThat(toggle.isSelected())
        .as("mirrored, dragging towards high x scrubs towards unselected")
        .isFalse();
  }

  @Test
  void aMirroredDragTheOtherWaySelects() {
    final ElwhaSwitch toggle = sized(new ElwhaSwitch());
    toggle.setComponentOrientation(ComponentOrientation.RIGHT_TO_LEFT);

    Input.press(toggle, TRAVEL_END_X, CENTER_Y);
    Input.drag(toggle, TRAVEL_START_X, CENTER_Y);
    Input.release(toggle, TRAVEL_START_X, CENTER_Y);

    assertThat(toggle.isSelected())
        .as("and dragging towards low x scrubs towards selected")
        .isTrue();
  }

  @Test
  void aMirroredClickStillTogglesRegardlessOfWhereItLands() {
    final ElwhaSwitch toggle = sized(new ElwhaSwitch());
    toggle.setComponentOrientation(ComponentOrientation.RIGHT_TO_LEFT);

    Input.click(toggle, 30, CENTER_Y);

    assertThat(toggle.isSelected())
        .as("a click is a toggle, not a position — mirroring does not change that")
        .isTrue();
  }
}
