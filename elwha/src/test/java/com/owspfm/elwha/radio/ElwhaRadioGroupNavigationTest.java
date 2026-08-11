package com.owspfm.elwha.radio;

import static org.assertj.core.api.Assertions.assertThat;

import com.owspfm.elwha.testkit.EdtInterceptor;
import com.owspfm.elwha.testkit.Input;
import com.owspfm.elwha.testkit.ThemeExtension;
import java.awt.ComponentOrientation;
import java.awt.event.ActionEvent;
import java.awt.event.KeyEvent;
import javax.swing.Action;
import javax.swing.JComponent;
import javax.swing.KeyStroke;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

/**
 * Tier A coverage of the group's keyboard contract (design §9): the arrow bindings the group
 * installs and removes, wrapping and disabled-skipping navigation order, the right-to-left flip on
 * the horizontal pair, selection-follows-focus, and the roving tab stop.
 *
 * <p>The roving tab stop is asserted here at <em>policy</em> level — which members carry the
 * focusable flag after each state change. Real focus ownership needs a realized window and belongs
 * to the {@code gui} tier; nothing below fakes a focus event.
 */
@ExtendWith({EdtInterceptor.class, ThemeExtension.class})
class ElwhaRadioGroupNavigationTest {

  private static final String UP = "elwhaRadioGroup.up";
  private static final String DOWN = "elwhaRadioGroup.down";
  private static final String LEFT = "elwhaRadioGroup.left";
  private static final String RIGHT = "elwhaRadioGroup.right";

  private static Object binding(final ElwhaRadioButton radio, final int keyCode) {
    return radio.getInputMap(JComponent.WHEN_FOCUSED).get(KeyStroke.getKeyStroke(keyCode, 0));
  }

  // ------------------------------------------------------------- installation

  @Test
  void joiningAGroupInstallsAllFourArrowBindings() {
    final ElwhaRadioButton radio = new ElwhaRadioButton();
    assertThat(binding(radio, KeyEvent.VK_DOWN))
        .as("an ungrouped radio has no arrow behavior")
        .isNull();

    new ElwhaRadioGroup().add(radio);

    assertThat(binding(radio, KeyEvent.VK_UP)).as("Up is bound").isEqualTo(UP);
    assertThat(binding(radio, KeyEvent.VK_DOWN)).as("Down is bound").isEqualTo(DOWN);
    assertThat(binding(radio, KeyEvent.VK_LEFT)).as("Left is bound").isEqualTo(LEFT);
    assertThat(binding(radio, KeyEvent.VK_RIGHT)).as("Right is bound").isEqualTo(RIGHT);
  }

  @Test
  void keypadArrowsAreBoundToo() {
    final ElwhaRadioButton radio = new ElwhaRadioButton();
    new ElwhaRadioGroup().add(radio);

    assertThat(binding(radio, KeyEvent.VK_KP_UP)).as("keypad Up navigates as well").isEqualTo(UP);
    assertThat(binding(radio, KeyEvent.VK_KP_DOWN)).as("keypad Down too").isEqualTo(DOWN);
    assertThat(binding(radio, KeyEvent.VK_KP_LEFT)).as("keypad Left too").isEqualTo(LEFT);
    assertThat(binding(radio, KeyEvent.VK_KP_RIGHT)).as("keypad Right too").isEqualTo(RIGHT);
  }

  @Test
  void leavingAGroupRemovesTheBindingsAndRestoresPlainFocusability() {
    final ElwhaRadioButton radio = new ElwhaRadioButton();
    final ElwhaRadioGroup group = new ElwhaRadioGroup();
    group.add(radio);

    group.remove(radio);

    assertThat(binding(radio, KeyEvent.VK_DOWN))
        .as("an ungrouped radio has no arrow behavior again")
        .isNull();
    assertThat(radio.getActionMap().get(DOWN)).as("and no leftover action").isNull();
    assertThat(radio.isFocusable()).as("and is an ordinary tab stop once more").isTrue();
  }

  // ------------------------------------------------------------- navigation

  @Test
  void downMovesToTheNextMemberAndSelectsIt() {
    final ElwhaRadioGroup group = new ElwhaRadioGroup();
    final ElwhaRadioButton first = new ElwhaRadioButton();
    final ElwhaRadioButton second = new ElwhaRadioButton();
    group.add(first);
    group.add(second);

    Input.pressBoundKey(first, "pressed DOWN", DOWN);

    assertThat(second.isSelected())
        .as("selection follows focus — arriving at a member selects it")
        .isTrue();
    assertThat(group.getSelected()).as("and the group agrees").isSameAs(second);
  }

  @Test
  void upMovesToThePreviousMember() {
    final ElwhaRadioGroup group = new ElwhaRadioGroup();
    final ElwhaRadioButton first = new ElwhaRadioButton();
    final ElwhaRadioButton second = new ElwhaRadioButton();
    group.add(first);
    group.add(second);

    Input.pressBoundKey(second, "pressed UP", UP);

    assertThat(first.isSelected()).as("Up steps backwards through membership order").isTrue();
  }

  @Test
  void navigationWrapsAtBothEnds() {
    final ElwhaRadioGroup group = new ElwhaRadioGroup();
    final ElwhaRadioButton first = new ElwhaRadioButton();
    final ElwhaRadioButton middle = new ElwhaRadioButton();
    final ElwhaRadioButton last = new ElwhaRadioButton();
    group.add(first);
    group.add(middle);
    group.add(last);

    Input.pressBoundKey(last, "pressed DOWN", DOWN);
    assertThat(first.isSelected()).as("Down past the end wraps to the first member").isTrue();

    Input.pressBoundKey(first, "pressed UP", UP);
    assertThat(last.isSelected()).as("and Up past the start wraps to the last").isTrue();
  }

  @Test
  void horizontalArrowsMatchTheVerticalDirections() {
    final ElwhaRadioGroup group = new ElwhaRadioGroup();
    final ElwhaRadioButton first = new ElwhaRadioButton();
    final ElwhaRadioButton second = new ElwhaRadioButton();
    group.add(first);
    group.add(second);

    Input.pressBoundKey(first, "pressed RIGHT", RIGHT);
    assertThat(second.isSelected()).as("Right steps forward like Down").isTrue();

    Input.pressBoundKey(second, "pressed LEFT", LEFT);
    assertThat(first.isSelected()).as("and Left steps back like Up").isTrue();
  }

  @Test
  void rightToLeftFlipsTheHorizontalArrowsOnly() {
    final ElwhaRadioGroup group = new ElwhaRadioGroup();
    final ElwhaRadioButton first = new ElwhaRadioButton();
    final ElwhaRadioButton second = new ElwhaRadioButton();
    group.add(first);
    group.add(second);
    first.setComponentOrientation(ComponentOrientation.RIGHT_TO_LEFT);
    second.setComponentOrientation(ComponentOrientation.RIGHT_TO_LEFT);

    Input.pressBoundKey(first, "pressed LEFT", LEFT);
    assertThat(second.isSelected())
        .as("under a right-to-left orientation Left is the forward direction")
        .isTrue();

    Input.pressBoundKey(second, "pressed DOWN", DOWN);
    assertThat(first.isSelected())
        .as("while the vertical pair is direction-fixed and still wraps forward")
        .isTrue();
  }

  @Test
  void navigationSkipsDisabledMembers() {
    final ElwhaRadioGroup group = new ElwhaRadioGroup();
    final ElwhaRadioButton first = new ElwhaRadioButton();
    final ElwhaRadioButton middle = new ElwhaRadioButton();
    final ElwhaRadioButton last = new ElwhaRadioButton();
    group.add(first);
    group.add(middle);
    group.add(last);
    middle.setEnabled(false);

    Input.pressBoundKey(first, "pressed DOWN", DOWN);

    assertThat(last.isSelected()).as("a disabled member is stepped over, not landed on").isTrue();
    assertThat(middle.isSelected()).as("and never selected").isFalse();
  }

  @Test
  void navigationStopsWhenEveryOtherMemberIsDisabled() {
    final ElwhaRadioGroup group = new ElwhaRadioGroup();
    final ElwhaRadioButton first = new ElwhaRadioButton();
    final ElwhaRadioButton second = new ElwhaRadioButton();
    group.add(first);
    group.add(second);
    second.setEnabled(false);

    Input.pressBoundKey(first, "pressed DOWN", DOWN);

    assertThat(second.isSelected()).as("there is nowhere to go, so nothing is selected").isFalse();
    assertThat(first.isSelected()).as("and the acting member is left alone").isFalse();
  }

  @Test
  void aSingleMemberGroupHasNowhereToNavigate() {
    final ElwhaRadioGroup group = new ElwhaRadioGroup();
    final ElwhaRadioButton only = new ElwhaRadioButton();
    group.add(only);

    Input.pressBoundKey(only, "pressed DOWN", DOWN);

    assertThat(only.isSelected()).as("a one-member group never navigates onto itself").isFalse();
  }

  @Test
  void aDisabledMemberDoesNotNavigate() {
    final ElwhaRadioGroup group = new ElwhaRadioGroup();
    final ElwhaRadioButton first = new ElwhaRadioButton();
    final ElwhaRadioButton second = new ElwhaRadioButton();
    group.add(first);
    group.add(second);
    first.setEnabled(false);

    Input.pressBoundKey(first, "pressed DOWN", DOWN);

    assertThat(second.isSelected())
        .as("a disabled radio cannot act, so its arrow keys do nothing")
        .isFalse();
  }

  @Test
  void aStaleActionFromAFormerGroupDoesNothing() {
    final ElwhaRadioGroup first = new ElwhaRadioGroup();
    final ElwhaRadioGroup second = new ElwhaRadioGroup();
    final ElwhaRadioButton mover = new ElwhaRadioButton();
    final ElwhaRadioButton oldSibling = new ElwhaRadioButton();
    first.add(mover);
    first.add(oldSibling);
    final Action stale = mover.getActionMap().get(DOWN);

    second.add(mover);
    stale.actionPerformed(new ActionEvent(mover, ActionEvent.ACTION_PERFORMED, DOWN));

    assertThat(oldSibling.isSelected())
        .as("an action held from a group the radio has left refuses to navigate the old group")
        .isFalse();
    assertThat(first.getSelected()).as("which stays empty").isNull();
  }

  @Test
  void arrivingFiresTheTargetsActionListeners() {
    final ElwhaRadioGroup group = new ElwhaRadioGroup();
    final ElwhaRadioButton first = new ElwhaRadioButton();
    final ElwhaRadioButton second = new ElwhaRadioButton();
    group.add(first);
    group.add(second);
    final int[] actions = {0};
    second.addActionListener(e -> actions[0]++);

    Input.pressBoundKey(first, "pressed DOWN", DOWN);

    assertThat(actions[0])
        .as("an arrow arrival is a user gesture, so the target's action listeners fire")
        .isEqualTo(1);
  }

  @Test
  void navigatingOntoTheSelectedMemberFiresNothing() {
    final ElwhaRadioGroup group = new ElwhaRadioGroup();
    final ElwhaRadioButton first = new ElwhaRadioButton();
    final ElwhaRadioButton second = new ElwhaRadioButton(true);
    group.add(first);
    group.add(second);
    final int[] actions = {0};
    second.addActionListener(e -> actions[0]++);

    Input.pressBoundKey(first, "pressed DOWN", DOWN);

    assertThat(second.isSelected()).as("the target stays selected").isTrue();
    assertThat(actions[0]).as("but re-affirming it fires nothing").isZero();
  }

  // --------------------------------------------------------- roving tab stop

  @Test
  void withNoSelectionEveryMemberIsATabStop() {
    final ElwhaRadioGroup group = new ElwhaRadioGroup();
    final ElwhaRadioButton first = new ElwhaRadioButton();
    final ElwhaRadioButton second = new ElwhaRadioButton();
    group.add(first);
    group.add(second);

    assertThat(first.isFocusable()).as("an unselected group is enterable at any member").isTrue();
    assertThat(second.isFocusable()).as("including the last one").isTrue();
  }

  @Test
  void aSelectionCollapsesTheGroupToOneTabStop() {
    final ElwhaRadioGroup group = new ElwhaRadioGroup();
    final ElwhaRadioButton first = new ElwhaRadioButton();
    final ElwhaRadioButton second = new ElwhaRadioButton();
    final ElwhaRadioButton third = new ElwhaRadioButton();
    group.add(first);
    group.add(second);
    group.add(third);

    second.setSelected(true);

    assertThat(second.isFocusable())
        .as("the selected member is the group's single tab stop")
        .isTrue();
    assertThat(first.isFocusable()).as("its siblings drop out of the tab order").isFalse();
    assertThat(third.isFocusable()).as("all of them").isFalse();
  }

  @Test
  void tabStopMovesWithTheSelection() {
    final ElwhaRadioGroup group = new ElwhaRadioGroup();
    final ElwhaRadioButton first = new ElwhaRadioButton();
    final ElwhaRadioButton second = new ElwhaRadioButton();
    group.add(first);
    group.add(second);
    first.setSelected(true);

    second.setSelected(true);

    assertThat(second.isFocusable()).as("the new selection takes the tab stop").isTrue();
    assertThat(first.isFocusable()).as("and the old one gives it up").isFalse();
  }

  @Test
  void clearingTheSelectionReopensEveryMember() {
    final ElwhaRadioGroup group = new ElwhaRadioGroup();
    final ElwhaRadioButton first = new ElwhaRadioButton();
    final ElwhaRadioButton second = new ElwhaRadioButton();
    group.add(first);
    group.add(second);
    first.setSelected(true);

    group.clearSelection();

    assertThat(first.isFocusable())
        .as("with no selection the group is enterable anywhere again")
        .isTrue();
    assertThat(second.isFocusable()).as("at either member").isTrue();
  }

  @Test
  void onlyAnEnabledSelectionClaimsTheTabStop() {
    final ElwhaRadioGroup group = new ElwhaRadioGroup();
    final ElwhaRadioButton first = new ElwhaRadioButton();
    final ElwhaRadioButton second = new ElwhaRadioButton();
    group.add(first);
    group.add(second);
    first.setSelected(true);

    first.setEnabled(false);

    assertThat(second.isFocusable())
        .as("a disabled selection cannot be the way in, so the group reopens")
        .isTrue();
  }

  @Test
  void navigatingMovesTheTabStopToTheArrival() {
    final ElwhaRadioGroup group = new ElwhaRadioGroup();
    final ElwhaRadioButton first = new ElwhaRadioButton();
    final ElwhaRadioButton second = new ElwhaRadioButton();
    group.add(first);
    group.add(second);

    Input.pressBoundKey(first, "pressed DOWN", DOWN);

    assertThat(second.isFocusable()).as("the arrival becomes the single tab stop").isTrue();
    assertThat(first.isFocusable()).as("and the member it left gives it up").isFalse();
  }

  @Test
  void aRemovedMemberBecomesAnOrdinaryTabStopAgain() {
    final ElwhaRadioGroup group = new ElwhaRadioGroup();
    final ElwhaRadioButton first = new ElwhaRadioButton();
    final ElwhaRadioButton second = new ElwhaRadioButton();
    group.add(first);
    group.add(second);
    first.setSelected(true);
    assertThat(second.isFocusable()).as("the sibling starts outside the tab order").isFalse();

    group.remove(second);

    assertThat(second.isFocusable())
        .as("leaving the group restores plain focusability rather than stranding it")
        .isTrue();
  }
}
