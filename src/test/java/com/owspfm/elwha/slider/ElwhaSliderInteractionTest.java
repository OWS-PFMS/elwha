package com.owspfm.elwha.slider;

import static org.assertj.core.api.Assertions.assertThat;

import com.owspfm.elwha.slider.ElwhaSlider.Orientation;
import com.owspfm.elwha.testkit.EdtInterceptor;
import com.owspfm.elwha.testkit.Input;
import com.owspfm.elwha.testkit.ThemeExtension;
import java.awt.ComponentOrientation;
import javax.swing.DefaultBoundedRangeModel;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

/**
 * Tier A coverage of the slider's input contracts — the pointer gesture (press jumps, drag tracks,
 * release commits) with its {@code valueIsAdjusting} bracket, the {@code WHEN_FOCUSED} InputMap
 * wiring behind every keyboard action, and the disabled guards from #432: an unqualified {@code
 * isEnabled()} inside an anonymous {@code AbstractAction} binds to the Action's own always-true
 * flag, so a disabled slider stayed keyboard-drivable. That bug is invisible unless a test asserts
 * a <em>disabled</em> component's bound keys do nothing.
 *
 * <p>Keyboard coverage goes through {@code Input.pressBoundKey}, which asserts the binding exists
 * before invoking the action it names — invoking actions by bare name would leave the wiring itself
 * untested, which is exactly how a keymap regression ships.
 */
@ExtendWith({EdtInterceptor.class, ThemeExtension.class})
class ElwhaSliderInteractionTest {

  private static final int LENGTH = 240;

  private static ElwhaSlider percent() {
    final ElwhaSlider slider = new ElwhaSlider(0, 100, 50);
    slider.setSize(LENGTH, slider.getPreferredSize().height);
    return slider;
  }

  private static int midY(final ElwhaSlider slider) {
    return slider.getHeight() / 2;
  }

  // ------------------------------------------------------------- pointer

  @Test
  void aPressJumpsTheHandleToWhereItLanded() {
    final ElwhaSlider slider = percent();

    Input.press(slider, slider.getWidth() - ElwhaSlider.HANDLE_WIDTH_PX / 2, midY(slider));

    assertThat(slider.getValue())
        .as("clicking the track is a jump, not a no-op — the handle comes to the pointer")
        .isEqualTo(100);
  }

  @Test
  void aDragTracksThePointer() {
    final ElwhaSlider slider = percent();

    Input.press(slider, 120, midY(slider));
    Input.drag(slider, 2, midY(slider));

    assertThat(slider.getValue()).as("the handle follows the drag to the start").isZero();

    Input.drag(slider, 238, midY(slider));
    assertThat(slider.getValue()).as("and back to the end").isEqualTo(100);
  }

  @Test
  void adjustingFlagBracketsTheWholeGesture() {
    final ElwhaSlider slider = percent();

    assertThat(slider.getValueIsAdjusting()).as("a resting slider is not adjusting").isFalse();

    Input.press(slider, 120, midY(slider));
    assertThat(slider.getValueIsAdjusting())
        .as("the flag is raised from the press, so a listener can tell live drags from commits")
        .isTrue();

    Input.drag(slider, 60, midY(slider));
    assertThat(slider.getValueIsAdjusting()).as("and stays up through the drag").isTrue();

    Input.release(slider, 60, midY(slider));
    assertThat(slider.getValueIsAdjusting()).as("release commits and lowers it").isFalse();
  }

  @Test
  void aListenerSeesTheAdjustingFlagMidDrag() {
    final ElwhaSlider slider = percent();
    final boolean[] sawAdjusting = {false};
    final boolean[] sawCommitted = {false};
    slider.addChangeListener(
        e -> {
          if (slider.getValueIsAdjusting()) {
            sawAdjusting[0] = true;
          } else {
            sawCommitted[0] = true;
          }
        });

    Input.press(slider, 120, midY(slider));
    Input.drag(slider, 60, midY(slider));
    Input.release(slider, 60, midY(slider));

    assertThat(sawAdjusting[0]).as("intermediate drag updates arrive marked as adjusting").isTrue();
    assertThat(sawCommitted[0])
        .as("and the release arrives unmarked — the committed value")
        .isTrue();
  }

  @Test
  void aDragBeyondTheTrackClampsRatherThanRunningAway() {
    final ElwhaSlider slider = percent();

    Input.press(slider, 120, midY(slider));
    Input.drag(slider, -400, midY(slider));

    assertThat(slider.getValue()).as("the value cannot leave its range").isZero();
  }

  @Test
  void aDragWithoutAPressIsIgnored() {
    final ElwhaSlider slider = percent();

    Input.drag(slider, 10, midY(slider));

    assertThat(slider.getValue())
        .as("a drag that started elsewhere must not hijack the handle")
        .isEqualTo(50);
  }

  @Test
  void aVerticalPressNearTheTopIsTheMaximum() {
    final ElwhaSlider slider = new ElwhaSlider(0, 100, 50);
    slider.setOrientation(Orientation.VERTICAL);
    slider.setSize(slider.getPreferredSize().width, LENGTH);

    Input.press(slider, slider.getWidth() / 2, ElwhaSlider.HANDLE_WIDTH_PX / 2);

    assertThat(slider.getValue())
        .as("a vertical slider fills bottom-up, so the top of the track is the maximum")
        .isEqualTo(100);
  }

  @Test
  void aVerticalPressNearTheBottomIsTheMinimum() {
    final ElwhaSlider slider = new ElwhaSlider(0, 100, 50);
    slider.setOrientation(Orientation.VERTICAL);
    slider.setSize(slider.getPreferredSize().width, LENGTH);

    Input.press(slider, slider.getWidth() / 2, LENGTH - ElwhaSlider.HANDLE_WIDTH_PX / 2);

    assertThat(slider.getValue()).as("and the bottom is the minimum").isZero();
  }

  @Test
  void aRightToLeftPressMirrors() {
    final ElwhaSlider slider = percent();
    slider.setComponentOrientation(ComponentOrientation.RIGHT_TO_LEFT);

    Input.press(slider, ElwhaSlider.HANDLE_WIDTH_PX / 2, midY(slider));

    assertThat(slider.getValue())
        .as("under RTL the left end of the track is the maximum")
        .isEqualTo(100);
  }

  @Test
  void aPressSnapsToAStopWhenStopsAreOn() {
    final ElwhaSlider slider = percent();
    slider.setStops(25);

    Input.press(slider, 130, midY(slider));

    assertThat(slider.getValue() % 25)
        .as("a pointer gesture lands on a stop just as the keyboard does")
        .isZero();
  }

  // ------------------------------------------------------------ keyboard

  @Test
  void arrowKeysAreBoundAndStepByTheUnitIncrement() {
    final ElwhaSlider slider = percent();
    slider.setUnitIncrement(5);

    Input.pressBoundKey(slider, "pressed RIGHT", "elwhaSlider.right");
    assertThat(slider.getValue()).as("Right steps up one unit").isEqualTo(55);

    Input.pressBoundKey(slider, "pressed LEFT", "elwhaSlider.left");
    assertThat(slider.getValue()).as("Left steps back down").isEqualTo(50);

    Input.pressBoundKey(slider, "pressed UP", "elwhaSlider.increase");
    assertThat(slider.getValue()).as("Up always increases").isEqualTo(55);

    Input.pressBoundKey(slider, "pressed DOWN", "elwhaSlider.decrease");
    assertThat(slider.getValue()).as("and Down always decreases").isEqualTo(50);
  }

  @Test
  void pageKeysAreBoundAndStepByTheBlockIncrement() {
    final ElwhaSlider slider = percent();
    slider.setBlockIncrement(20);

    Input.pressBoundKey(slider, "pressed PAGE_UP", "elwhaSlider.blockUp");
    assertThat(slider.getValue()).as("Page Up jumps a block").isEqualTo(70);

    Input.pressBoundKey(slider, "pressed PAGE_DOWN", "elwhaSlider.blockDown");
    assertThat(slider.getValue()).as("Page Down jumps back").isEqualTo(50);
  }

  @Test
  void homeAndEndAreBoundToTheRangeEnds() {
    final ElwhaSlider slider = percent();

    Input.pressBoundKey(slider, "pressed HOME", "elwhaSlider.min");
    assertThat(slider.getValue()).as("Home is the minimum").isZero();

    Input.pressBoundKey(slider, "pressed END", "elwhaSlider.max");
    assertThat(slider.getValue()).as("End is the maximum").isEqualTo(100);
  }

  @Test
  void holdingSpacePromotesTheArrowsToTheBlockIncrement() {
    final ElwhaSlider slider = percent();
    slider.setUnitIncrement(1);
    slider.setBlockIncrement(20);

    Input.pressBoundKey(slider, "pressed SPACE", "elwhaSlider.spaceDown");
    Input.pressBoundKey(slider, "pressed RIGHT", "elwhaSlider.right");
    assertThat(slider.getValue()).as("Space plus an arrow is the coarse adjustment").isEqualTo(70);

    Input.pressBoundKey(slider, "released SPACE", "elwhaSlider.spaceUp");
    Input.pressBoundKey(slider, "pressed RIGHT", "elwhaSlider.right");
    assertThat(slider.getValue()).as("and releasing Space restores the fine one").isEqualTo(71);
  }

  @Test
  void leftAndRightArrowsMirrorUnderRightToLeftButUpAndDownNever() {
    final ElwhaSlider slider = percent();
    slider.setComponentOrientation(ComponentOrientation.RIGHT_TO_LEFT);

    Input.pressBoundKey(slider, "pressed RIGHT", "elwhaSlider.right");
    assertThat(slider.getValue())
        .as("under RTL, Right moves toward the visually-right end, which is the lower value")
        .isEqualTo(49);

    Input.pressBoundKey(slider, "pressed UP", "elwhaSlider.increase");
    assertThat(slider.getValue()).as("Up still means more, in any orientation").isEqualTo(50);
  }

  @Test
  void aVerticalSlidersArrowsAreNotMirroredByRightToLeft() {
    final ElwhaSlider slider = new ElwhaSlider(0, 100, 50);
    slider.setOrientation(Orientation.VERTICAL);
    slider.setComponentOrientation(ComponentOrientation.RIGHT_TO_LEFT);
    slider.setSize(slider.getPreferredSize().width, LENGTH);

    Input.pressBoundKey(slider, "pressed RIGHT", "elwhaSlider.right");

    assertThat(slider.getValue())
        .as("a vertical slider never mirrors, so Right still increases")
        .isEqualTo(51);
  }

  @Test
  void arrowsAdvanceOneStopAtATimeWhenStopsAreOn() {
    final ElwhaSlider slider = percent();
    slider.setUnitIncrement(1);
    slider.setStops(25);

    Input.pressBoundKey(slider, "pressed RIGHT", "elwhaSlider.right");

    assertThat(slider.getValue())
        .as("an arrow moves to the next stop rather than between stops")
        .isEqualTo(75);
  }

  // ------------------------------------------------------ disabled (#432)

  @Test
  void aDisabledSliderIgnoresEveryBoundKey() {
    final ElwhaSlider slider = percent();
    slider.setEnabled(false);

    Input.pressBoundKey(slider, "pressed RIGHT", "elwhaSlider.right");
    Input.pressBoundKey(slider, "pressed LEFT", "elwhaSlider.left");
    Input.pressBoundKey(slider, "pressed UP", "elwhaSlider.increase");
    Input.pressBoundKey(slider, "pressed DOWN", "elwhaSlider.decrease");
    Input.pressBoundKey(slider, "pressed PAGE_UP", "elwhaSlider.blockUp");
    Input.pressBoundKey(slider, "pressed PAGE_DOWN", "elwhaSlider.blockDown");
    Input.pressBoundKey(slider, "pressed HOME", "elwhaSlider.min");
    Input.pressBoundKey(slider, "pressed END", "elwhaSlider.max");

    assertThat(slider.getValue())
        .as(
            "focus survives setEnabled(false), so a disabled slider keeps receiving its bound keys"
                + " — the actions themselves have to refuse (#432)")
        .isEqualTo(50);
  }

  @Test
  void reEnablingRestoresTheKeyboard() {
    final ElwhaSlider slider = percent();
    slider.setEnabled(false);
    Input.pressBoundKey(slider, "pressed END", "elwhaSlider.max");
    assertThat(slider.getValue()).as("inert while disabled").isEqualTo(50);

    slider.setEnabled(true);
    Input.pressBoundKey(slider, "pressed END", "elwhaSlider.max");

    assertThat(slider.getValue())
        .as("and live again once re-enabled — so the inert result was not vacuous")
        .isEqualTo(100);
  }

  @Test
  void aDisabledSliderIgnoresThePointerToo() {
    final ElwhaSlider slider = percent();
    slider.setEnabled(false);

    Input.press(slider, 10, midY(slider));
    Input.drag(slider, 200, midY(slider));
    Input.release(slider, 200, midY(slider));

    assertThat(slider.getValue()).as("no gesture reaches a disabled slider").isEqualTo(50);
    assertThat(slider.getValueIsAdjusting())
        .as("and it never enters the adjusting state")
        .isFalse();
  }

  @Test
  void aDisabledSliderDoesNotEvenHover() {
    final ElwhaSlider slider = percent();
    slider.setEnabled(false);

    Input.enter(slider, 120, midY(slider));

    assertThat(slider.getCursor().getType())
        .as("a disabled control offers no pointing-hand affordance")
        .isEqualTo(java.awt.Cursor.DEFAULT_CURSOR);
  }

  // ------------------------------------------------------------ listeners

  @Test
  void aChangeListenerHearsEveryValueChange() {
    final ElwhaSlider slider = percent();
    final int[] changes = {0};
    slider.addChangeListener(e -> changes[0]++);

    slider.setValue(60);
    slider.setValue(70);

    assertThat(changes[0]).as("one notification per change").isEqualTo(2);
  }

  @Test
  void aNoOpSetFiresNothing() {
    final ElwhaSlider slider = percent();
    final int[] changes = {0};
    slider.addChangeListener(e -> changes[0]++);

    slider.setValue(50);

    assertThat(changes[0]).as("setting the value it already holds is not a change").isZero();
  }

  @Test
  void leavingTheHierarchyReleasesTheValueModel() {
    final DefaultBoundedRangeModel shared = new DefaultBoundedRangeModel(50, 0, 0, 100);
    final ElwhaSlider slider = new ElwhaSlider(shared);
    assertThat(shared.getChangeListeners()).as("the slider subscribes at construction").hasSize(1);

    slider.addNotify();
    slider.removeNotify();

    assertThat(shared.getChangeListeners())
        .as("#610 — a caller-supplied model would otherwise pin every slider it has backed")
        .isEmpty();
  }

  @Test
  void aReAddedSliderIsSubscribedOnceAndReadsTheModelAsItStandsNow() {
    final DefaultBoundedRangeModel shared = new DefaultBoundedRangeModel(50, 0, 0, 100);
    final ElwhaSlider slider = new ElwhaSlider(shared);
    slider.addNotify();
    slider.removeNotify();

    shared.setValue(80);
    slider.addNotify();

    assertThat(shared.getChangeListeners())
        .as("a stage swap re-arms the subscription exactly once")
        .hasSize(1);
    assertThat(slider.getValue())
        .as("and the slider reads the model as it stands now, not as it left it")
        .isEqualTo(80);
  }

  @Test
  void aRemovedListenerStopsHearing() {
    final ElwhaSlider slider = percent();
    final int[] changes = {0};
    final javax.swing.event.ChangeListener listener = e -> changes[0]++;
    slider.addChangeListener(listener);

    slider.removeChangeListener(listener);
    slider.setValue(60);

    assertThat(changes[0]).as("removal takes effect").isZero();
  }
}
