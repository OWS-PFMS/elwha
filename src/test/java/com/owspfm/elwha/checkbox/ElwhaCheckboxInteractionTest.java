package com.owspfm.elwha.checkbox;

import static org.assertj.core.api.Assertions.assertThat;

import com.owspfm.elwha.checkbox.ElwhaCheckbox.CheckState;
import com.owspfm.elwha.testkit.EdtInterceptor;
import com.owspfm.elwha.testkit.Input;
import com.owspfm.elwha.testkit.ThemeExtension;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

/**
 * Tier A coverage of what counts as a <em>user</em> toggle and what only the property-change
 * listener hears — the split the design doc §8 draws between {@code ActionListener} (user gestures
 * only) and {@code PropertyChangeEvent} (every change) — plus the pointer and keyboard paths that
 * produce one, and the disabled guards on both.
 *
 * <p>The keyboard path is asserted through the {@code WHEN_FOCUSED} binding rather than by invoking
 * the action by name, so a lost binding fails here rather than surviving as dead wiring.
 */
@ExtendWith({EdtInterceptor.class, ThemeExtension.class})
class ElwhaCheckboxInteractionTest {

  private static final String TOGGLE_ACTION = "elwhacheckbox.toggle";
  private static final int SIDE = 48;
  private static final int CENTER = 24;

  private static ElwhaCheckbox sized() {
    final ElwhaCheckbox box = new ElwhaCheckbox();
    box.setSize(SIDE, SIDE);
    return box;
  }

  // ------------------------------------------------------- the pointer path

  @Test
  void clickTogglesAndFiresOneActionEvent() {
    final ElwhaCheckbox box = sized();
    final List<ActionEvent> fired = new ArrayList<>();
    box.addActionListener(fired::add);

    Input.click(box, CENTER, CENTER);

    assertThat(box.isChecked()).as("a click in the target toggles").isTrue();
    assertThat(fired).as("one gesture fires one action event").hasSize(1);
    assertThat(fired.get(0).getActionCommand())
        .as("the action command names the gesture, not the resulting state")
        .isEqualTo("toggle");
    assertThat(fired.get(0).getSource()).as("the checkbox is the event source").isSameAs(box);
  }

  @Test
  void pressAloneDoesNotToggle() {
    final ElwhaCheckbox box = sized();
    final int[] fired = {0};
    box.addActionListener(e -> fired[0]++);

    Input.press(box, CENTER, CENTER);

    assertThat(box.isChecked())
        .as(
            "the press only arms — the M3 gesture commits on release, per the macOS rapid-click"
                + " fix")
        .isFalse();
    assertThat(fired[0]).as("an armed press has fired nothing yet").isZero();
  }

  @Test
  void releaseOutsideBoundsCancelsTheClick() {
    final ElwhaCheckbox box = sized();
    final int[] fired = {0};
    box.addActionListener(e -> fired[0]++);

    Input.press(box, CENTER, CENTER);
    Input.release(box, SIDE + 40, CENTER);

    assertThat(box.isChecked()).as("dragging off the target abandons the toggle").isFalse();
    assertThat(fired[0]).as("a cancelled click fires nothing").isZero();
  }

  @Test
  void releaseWithNoArmedPressIsIgnored() {
    final ElwhaCheckbox box = sized();
    final int[] fired = {0};
    box.addActionListener(e -> fired[0]++);

    Input.release(box, CENTER, CENTER);

    assertThat(box.isChecked()).as("a stray release toggles nothing").isFalse();
    assertThat(fired[0]).as("a stray release fires nothing").isZero();
  }

  @Test
  void clickingTheLabelAreaAlsoToggles() {
    final ElwhaCheckbox box = new ElwhaCheckbox("Remember me");
    box.setSize(box.getPreferredSize());

    Input.click(box, box.getWidth() - 4, CENTER);

    assertThat(box.isChecked())
        .as("the whole component including the label text is the click target")
        .isTrue();
  }

  // ------------------------------------------------------ the keyboard path

  @Test
  void spaceIsBoundWhenFocusedAndToggles() {
    final ElwhaCheckbox box = sized();

    Input.pressBoundKey(box, "pressed SPACE", TOGGLE_ACTION);

    assertThat(box.isChecked()).as("Space commits the toggle in one action").isTrue();
  }

  @Test
  void spaceFiresTheActionListenerLikeAClick() {
    final ElwhaCheckbox box = sized();
    final List<ActionEvent> fired = new ArrayList<>();
    box.addActionListener(fired::add);

    Input.pressBoundKey(box, "pressed SPACE", TOGGLE_ACTION);

    assertThat(fired).as("keyboard activation is a user gesture too").hasSize(1);
    assertThat(fired.get(0).getActionCommand())
        .as("and carries the same action command as a click")
        .isEqualTo("toggle");
  }

  // ------------------------------------------------ programmatic vs gesture

  @Test
  void programmaticWritesNeverFireTheActionListener() {
    final ElwhaCheckbox box = sized();
    final int[] fired = {0};
    box.addActionListener(e -> fired[0]++);

    box.setChecked(true);
    box.setCheckState(CheckState.INDETERMINATE);
    box.setIndeterminate(false);

    assertThat(fired[0])
        .as("ActionListener is reserved for user gestures — programmatic writes use the property")
        .isZero();
    assertThat(box.getCheckState())
        .as("but the writes themselves landed")
        .isEqualTo(CheckState.UNCHECKED);
  }

  @Test
  void doClickIsAUserEquivalentToggle() {
    final ElwhaCheckbox box = sized();
    final int[] fired = {0};
    box.addActionListener(e -> fired[0]++);

    box.doClick();

    assertThat(box.isChecked()).as("doClick runs the same cycle a click does").isTrue();
    assertThat(fired[0]).as("and fires the action listeners like a real gesture").isEqualTo(1);
  }

  // -------------------------------------------------------- listener plumbing

  @Test
  void addingANullListenerIsIgnored() {
    final ElwhaCheckbox box = sized();

    box.addActionListener(null);

    assertThat(box.isChecked()).as("a null registration does not break the next toggle").isFalse();
    box.doClick();
    assertThat(box.isChecked())
        .as("the toggle still runs with a null in the registration path")
        .isTrue();
  }

  @Test
  void removedListenersStopHearingToggles() {
    final ElwhaCheckbox box = sized();
    final int[] fired = {0};
    final ActionListener listener = e -> fired[0]++;
    box.addActionListener(listener);
    box.doClick();

    box.removeActionListener(listener);
    box.doClick();

    assertThat(fired[0]).as("a removed listener hears nothing further").isEqualTo(1);
  }

  @Test
  void everyRegisteredListenerHearsEachToggle() {
    final ElwhaCheckbox box = sized();
    final int[] first = {0};
    final int[] second = {0};
    box.addActionListener(e -> first[0]++);
    box.addActionListener(e -> second[0]++);

    box.doClick();

    assertThat(first[0]).as("the first listener fires").isEqualTo(1);
    assertThat(second[0]).as("and so does the second").isEqualTo(1);
  }

  // ------------------------------------------------------- disabled guards

  @Test
  void aDisabledCheckboxIgnoresThePointer() {
    final ElwhaCheckbox box = sized();
    box.setEnabled(false);
    final int[] fired = {0};
    box.addActionListener(e -> fired[0]++);

    Input.click(box, CENTER, CENTER);

    assertThat(box.isChecked()).as("a disabled checkbox never toggles from a click").isFalse();
    assertThat(fired[0]).as("and fires nothing").isZero();
  }

  @Test
  void aDisabledCheckboxIgnoresTheBoundKeyAction() {
    final ElwhaCheckbox box = sized();
    box.setEnabled(false);
    final int[] fired = {0};
    box.addActionListener(e -> fired[0]++);

    Input.pressBoundKey(box, "pressed SPACE", TOGGLE_ACTION);

    assertThat(box.isChecked())
        .as("the guard lives in the action body, so invoking it directly is still refused")
        .isFalse();
    assertThat(fired[0]).as("and fires nothing").isZero();
  }

  @Test
  void doClickIsANoOpWhileDisabled() {
    final ElwhaCheckbox box = sized();
    box.setEnabled(false);
    final int[] fired = {0};
    box.addActionListener(e -> fired[0]++);

    box.doClick();

    assertThat(box.isChecked())
        .as("doClick honors the disabled guard like every user path")
        .isFalse();
    assertThat(fired[0]).as("and fires nothing").isZero();
  }

  @Test
  void programmaticWritesStillLandWhileDisabled() {
    final ElwhaCheckbox box = sized();
    box.setEnabled(false);

    box.setChecked(true);

    assertThat(box.isChecked())
        .as("disabled blocks user gestures, not the app's own model writes")
        .isTrue();
  }

  @Test
  void reEnablingRestoresTheGesturePath() {
    final ElwhaCheckbox box = sized();
    box.setEnabled(false);
    Input.click(box, CENTER, CENTER);

    box.setEnabled(true);
    Input.click(box, CENTER, CENTER);

    assertThat(box.isChecked())
        .as("the guard is a live read of isEnabled, not a latched flag")
        .isTrue();
  }
}
