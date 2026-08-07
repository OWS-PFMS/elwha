package com.owspfm.elwha.radio;

import static org.assertj.core.api.Assertions.assertThat;

import com.owspfm.elwha.testkit.EdtInterceptor;
import com.owspfm.elwha.testkit.Input;
import com.owspfm.elwha.testkit.ThemeExtension;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

/**
 * Tier A coverage of the pointer and keyboard paths into a user-gesture select, and the disabled
 * guards on each. Space follows Swing button semantics — the press arms and the release commits —
 * so both halves are asserted through their {@code WHEN_FOCUSED} bindings rather than by invoking
 * the actions by name, which would leave a lost binding undetected.
 */
@ExtendWith({EdtInterceptor.class, ThemeExtension.class})
class ElwhaRadioButtonInteractionTest {

  private static final String PRESS_ACTION = "elwhaRadio.press";
  private static final String RELEASE_ACTION = "elwhaRadio.release";
  private static final int SIDE = 48;
  private static final int CENTER = 24;

  private static ElwhaRadioButton sized() {
    final ElwhaRadioButton radio = new ElwhaRadioButton();
    radio.setSize(SIDE, SIDE);
    return radio;
  }

  // ------------------------------------------------------------- pointer path

  @Test
  void clickSelectsAndFiresOnce() {
    final ElwhaRadioButton radio = sized();
    final int[] actions = {0};
    radio.addActionListener(e -> actions[0]++);

    Input.click(radio, CENTER, CENTER);

    assertThat(radio.isSelected()).as("a click in the target selects").isTrue();
    assertThat(actions[0]).as("one gesture fires one action event").isEqualTo(1);
  }

  @Test
  void pressAloneDoesNotSelect() {
    final ElwhaRadioButton radio = sized();
    final int[] actions = {0};
    radio.addActionListener(e -> actions[0]++);

    Input.press(radio, CENTER, CENTER);

    assertThat(radio.isSelected())
        .as("the press only arms — the gesture commits on release")
        .isFalse();
    assertThat(actions[0]).as("and fires nothing yet").isZero();
  }

  @Test
  void releaseOutsideBoundsCancelsTheClick() {
    final ElwhaRadioButton radio = sized();
    final int[] actions = {0};
    radio.addActionListener(e -> actions[0]++);

    Input.press(radio, CENTER, CENTER);
    Input.release(radio, SIDE + 40, CENTER);

    assertThat(radio.isSelected()).as("dragging off the target abandons the select").isFalse();
    assertThat(actions[0]).as("a cancelled click fires nothing").isZero();
  }

  @Test
  void releaseWithNoArmedPressIsIgnored() {
    final ElwhaRadioButton radio = sized();
    final int[] actions = {0};
    radio.addActionListener(e -> actions[0]++);

    Input.release(radio, CENTER, CENTER);

    assertThat(radio.isSelected()).as("a stray release selects nothing").isFalse();
    assertThat(actions[0]).as("and fires nothing").isZero();
  }

  @Test
  void clickingASelectedRadioNeverDeselectsIt() {
    final ElwhaRadioButton radio = new ElwhaRadioButton(true);
    radio.setSize(SIDE, SIDE);
    final int[] events = {0};
    radio.addActionListener(e -> events[0]++);
    radio.addChangeListener(e -> events[0]++);

    Input.click(radio, CENTER, CENTER);

    assertThat(radio.isSelected())
        .as("a radio is not a toggle — clicking the selected one leaves it selected")
        .isTrue();
    assertThat(events[0]).as("and fires nothing, because nothing changed").isZero();
  }

  @Test
  void clickingTheLabelAreaAlsoSelects() {
    final ElwhaRadioButton radio = new ElwhaRadioButton("Weekly");
    radio.setSize(radio.getPreferredSize());

    Input.click(radio, radio.getWidth() - 4, CENTER);

    assertThat(radio.isSelected()).as("the label extends the click target").isTrue();
  }

  // ------------------------------------------------------------ keyboard path

  @Test
  void spacePressArmsAndSpaceReleaseCommits() {
    final ElwhaRadioButton radio = sized();

    Input.pressBoundKey(radio, "pressed SPACE", PRESS_ACTION);
    assertThat(radio.isSelected()).as("Space press alone does not select").isFalse();

    Input.pressBoundKey(radio, "released SPACE", RELEASE_ACTION);
    assertThat(radio.isSelected()).as("Space release commits the select").isTrue();
  }

  @Test
  void spaceFiresTheActionListenerLikeAClick() {
    final ElwhaRadioButton radio = sized();
    final int[] actions = {0};
    radio.addActionListener(e -> actions[0]++);

    Input.pressBoundKey(radio, "pressed SPACE", PRESS_ACTION);
    Input.pressBoundKey(radio, "released SPACE", RELEASE_ACTION);

    assertThat(actions[0]).as("keyboard activation is a user gesture too").isEqualTo(1);
  }

  @Test
  void aSpaceReleaseWithNoPressCommitsNothing() {
    final ElwhaRadioButton radio = sized();
    final int[] actions = {0};
    radio.addActionListener(e -> actions[0]++);

    Input.pressBoundKey(radio, "released SPACE", RELEASE_ACTION);

    assertThat(radio.isSelected())
        .as("the release action is guarded on the armed press, so a stray one does nothing")
        .isFalse();
    assertThat(actions[0]).as("and fires nothing").isZero();
  }

  @Test
  void repeatedSpacePressesArmOnlyOnce() {
    final ElwhaRadioButton radio = sized();
    final int[] actions = {0};
    radio.addActionListener(e -> actions[0]++);

    Input.pressBoundKey(radio, "pressed SPACE", PRESS_ACTION);
    Input.pressBoundKey(radio, "pressed SPACE", PRESS_ACTION);
    Input.pressBoundKey(radio, "released SPACE", RELEASE_ACTION);

    assertThat(actions[0])
        .as("key auto-repeat re-fires the press action, which must not stack up commits")
        .isEqualTo(1);
  }

  // -------------------------------------------------------------- disabled

  @Test
  void aDisabledRadioIgnoresThePointer() {
    final ElwhaRadioButton radio = sized();
    radio.setEnabled(false);
    final int[] actions = {0};
    radio.addActionListener(e -> actions[0]++);

    Input.click(radio, CENTER, CENTER);

    assertThat(radio.isSelected()).as("a disabled radio never selects from a click").isFalse();
    assertThat(actions[0]).as("and fires nothing").isZero();
  }

  @Test
  void aDisabledRadioIgnoresBothBoundKeyActions() {
    final ElwhaRadioButton radio = sized();
    radio.setEnabled(false);
    final int[] actions = {0};
    radio.addActionListener(e -> actions[0]++);

    Input.pressBoundKey(radio, "pressed SPACE", PRESS_ACTION);
    Input.pressBoundKey(radio, "released SPACE", RELEASE_ACTION);

    assertThat(radio.isSelected())
        .as("the guards live in the action bodies, so invoking them directly is still refused")
        .isFalse();
    assertThat(actions[0]).as("and fires nothing").isZero();
  }

  @Test
  void reEnablingRestoresTheGesturePath() {
    final ElwhaRadioButton radio = sized();
    radio.setEnabled(false);
    Input.click(radio, CENTER, CENTER);

    radio.setEnabled(true);
    Input.click(radio, CENTER, CENTER);

    assertThat(radio.isSelected())
        .as("the guard is a live read of isEnabled, not a latched flag")
        .isTrue();
  }
}
