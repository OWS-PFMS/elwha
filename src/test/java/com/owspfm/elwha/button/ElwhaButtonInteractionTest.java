package com.owspfm.elwha.button;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.owspfm.elwha.testkit.EdtInterceptor;
import com.owspfm.elwha.testkit.Input;
import com.owspfm.elwha.testkit.ThemeExtension;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

/**
 * Tier A event-contract coverage for {@link ElwhaButton} — what a click, a keyboard activation, a
 * programmatic {@code doClick}, and each setter actually emit, plus the disabled guards that the
 * {@code AbstractAction.isEnabled()} shadowing bug (#432) class turns on.
 *
 * <p>Mouse input travels through the real {@code dispatchEvent} pipeline and keyboard coverage
 * asserts the {@code WHEN_FOCUSED} binding before invoking the mapped action, so the wiring is part
 * of the contract rather than an assumption.
 */
@ExtendWith({EdtInterceptor.class, ThemeExtension.class})
class ElwhaButtonInteractionTest {

  private static final String ACTIVATE = "elwhabutton.activate";

  private static ElwhaButton sized(final ElwhaButton button) {
    final Dimension pref = button.getPreferredSize();
    button.setSize(pref.width, pref.height);
    return button;
  }

  private static ElwhaButton selectable(final ButtonVariant variant) {
    return sized(
        new ElwhaButton("Save")
            .setVariant(variant)
            .setInteractionMode(ButtonInteractionMode.SELECTABLE));
  }

  private static int centerX(final ElwhaButton button) {
    return button.getWidth() / 2;
  }

  private static int centerY(final ElwhaButton button) {
    return button.getHeight() / 2;
  }

  // ------------------------------------------------------------- activation

  @Test
  void aClickFiresEachActionListenerExactlyOnce() {
    final ElwhaButton button = sized(new ElwhaButton("Save"));
    final List<ActionEvent> heard = new ArrayList<>();
    button.addActionListener(heard::add);

    Input.click(button, centerX(button), centerY(button));

    assertThat(heard).as("a press-release pair inside the button activates it once").hasSize(1);
    assertThat(heard.get(0).getActionCommand())
        .as("the action command identifies the activation")
        .isEqualTo("click");
    assertThat(heard.get(0).getSource()).as("the button is the event source").isSameAs(button);
  }

  @Test
  void releasingOutsideTheButtonCancelsTheClick() {
    final ElwhaButton button = sized(new ElwhaButton("Save"));
    final int[] fired = {0};
    button.addActionListener(e -> fired[0]++);

    Input.press(button, centerX(button), centerY(button));
    Input.release(button, 900, 900);

    assertThat(fired[0]).as("a drag-off release is a cancelled click, not an activation").isZero();
  }

  @Test
  void aPressThatNeverLandedInsideActivatesNothing() {
    final ElwhaButton button = sized(new ElwhaButton("Save"));
    final int[] fired = {0};
    button.addActionListener(e -> fired[0]++);

    Input.press(button, 900, 900);
    Input.release(button, centerX(button), centerY(button));

    assertThat(fired[0]).as("a release with no matching press inside fires nothing").isZero();
  }

  @Test
  void spaceAndEnterAreBoundWhenFocusedAndBothActivate() {
    final ElwhaButton button = sized(new ElwhaButton("Save"));
    final int[] fired = {0};
    button.addActionListener(e -> fired[0]++);

    Input.pressBoundKey(button, "pressed SPACE", ACTIVATE);
    Input.pressBoundKey(button, "pressed ENTER", ACTIVATE);

    assertThat(fired[0]).as("both bound keys activate the button").isEqualTo(2);
  }

  @Test
  void doClickActivatesWithoutAnyPointerInput() {
    final ElwhaButton button = new ElwhaButton("Save");
    final int[] fired = {0};
    button.addActionListener(e -> fired[0]++);

    button.doClick();

    assertThat(fired[0])
        .as("doClick is the JButton analogue for host-driven activation")
        .isEqualTo(1);
  }

  @Test
  void doClickTogglesASelectableButtonToo() {
    final ElwhaButton button = selectable(ButtonVariant.FILLED);

    button.doClick();

    assertThat(button.isSelected()).as("doClick flips selection exactly as a click would").isTrue();
  }

  @Test
  void removingAListenerStopsItsDelivery() {
    final ElwhaButton button = sized(new ElwhaButton("Save"));
    final int[] fired = {0};
    final java.awt.event.ActionListener listener = e -> fired[0]++;
    button.addActionListener(listener);
    button.addActionListener(null);

    button.removeActionListener(listener);
    Input.click(button, centerX(button), centerY(button));

    assertThat(fired[0])
        .as("a removed listener hears nothing, and a null one was never added")
        .isZero();
  }

  // ----------------------------------------------------- listener accessor

  @Test
  void registeredActionListenersReadBack() {
    final ElwhaButton button = new ElwhaButton("Save");
    final ActionListener first = e -> {};
    final ActionListener second = e -> {};

    button.addActionListener(first);
    button.addActionListener(second);

    assertThat(button.getActionListeners())
        .as("#678 — listener accumulation was unassertable without a counting subclass")
        .containsExactly(first, second);

    button.removeActionListener(first);
    assertThat(button.getActionListeners()).containsExactly(second);
  }

  @Test
  void aReturnedListenerArrayIsACopy() {
    final ElwhaButton button = new ElwhaButton("Save");
    button.addActionListener(e -> {});

    button.getActionListeners()[0] = null;

    assertThat(button.getActionListeners())
        .as("a caller cannot unregister a listener by writing into the returned array")
        .doesNotContainNull();
  }

  // -------------------------------------------------------- disabled guards

  @Test
  void aDisabledButtonFiresNothingThroughAnyPath() {
    final ElwhaButton button = sized(new ElwhaButton("Save"));
    button.setEnabled(false);
    final int[] fired = {0};
    button.addActionListener(e -> fired[0]++);

    Input.click(button, centerX(button), centerY(button));
    Input.pressBoundKey(button, "pressed SPACE", ACTIVATE);
    Input.pressBoundKey(button, "pressed ENTER", ACTIVATE);
    button.doClick();

    assertThat(fired[0])
        .as("mouse, both key bindings, and doClick are all guarded while disabled")
        .isZero();
  }

  @Test
  void aDisabledSelectableButtonNeverToggles() {
    final ElwhaButton button = selectable(ButtonVariant.FILLED);
    button.setEnabled(false);
    final int[] selectionChanges = {0};
    button.addSelectionChangeListener(e -> selectionChanges[0]++);

    Input.click(button, centerX(button), centerY(button));
    Input.pressBoundKey(button, "pressed SPACE", ACTIVATE);

    assertThat(button.isSelected()).as("a disabled toggle holds its state").isFalse();
    assertThat(selectionChanges[0]).as("and reports no selection change").isZero();
  }

  // ------------------------------------------------------------- selection

  @Test
  void clickingASelectableButtonTogglesAndFiresBothContracts() {
    final ElwhaButton button = selectable(ButtonVariant.FILLED);
    final int[] actions = {0};
    final List<Object> selections = new ArrayList<>();
    button.addActionListener(e -> actions[0]++);
    button.addSelectionChangeListener(e -> selections.add(e.getNewValue()));

    Input.click(button, centerX(button), centerY(button));
    Input.click(button, centerX(button), centerY(button));

    assertThat(button.isSelected()).as("two clicks return to unselected").isFalse();
    assertThat(actions[0]).as("each toggle is also an activation").isEqualTo(2);
    assertThat(selections)
        .as("the selected property reports each new value in order")
        .containsExactly(true, false);
  }

  @Test
  void setSelectedIsInertOnAPushButton() {
    final ElwhaButton button = new ElwhaButton("Save");
    final int[] changes = {0};
    button.addSelectionChangeListener(e -> changes[0]++);

    button.setSelected(true);

    assertThat(button.isSelected())
        .as("a CLICKABLE button has no persistent selected state")
        .isFalse();
    assertThat(changes[0]).as("and fires no property change for a state it does not hold").isZero();
  }

  @Test
  void programmaticSetSelectedFiresTheSelectedPropertyAndNoAction() {
    final ElwhaButton button = selectable(ButtonVariant.FILLED);
    final int[] actions = {0};
    final int[] changes = {0};
    button.addActionListener(e -> actions[0]++);
    button.addSelectionChangeListener(e -> changes[0]++);

    button.setSelected(true);

    assertThat(changes[0]).as("setSelected fires the selected property once").isEqualTo(1);
    assertThat(actions[0])
        .as("setSelected is not an activation and fires no ActionListener")
        .isZero();
  }

  @Test
  void resettingTheSameSelectionFiresNothing() {
    final ElwhaButton button = selectable(ButtonVariant.FILLED).setSelected(true);
    final int[] changes = {0};
    button.addSelectionChangeListener(e -> changes[0]++);

    button.setSelected(true);

    assertThat(changes[0]).as("an idempotent setSelected is silent").isZero();
  }

  @Test
  void leavingSelectableModeClearsALatchedSelection() {
    final ElwhaButton button = selectable(ButtonVariant.FILLED).setSelected(true);

    button.setInteractionMode(ButtonInteractionMode.CLICKABLE);

    assertThat(button.isSelected())
        .as("a push button carries no latched selection back into a later toggle mode")
        .isFalse();
  }

  @Test
  void textVariantRefusesToggleFromEitherDirection() {
    assertThatThrownBy(
            () ->
                new ElwhaButton("Save")
                    .setVariant(ButtonVariant.TEXT)
                    .setInteractionMode(ButtonInteractionMode.SELECTABLE))
        .as("making a TEXT button selectable is rejected")
        .isInstanceOf(IllegalStateException.class)
        .hasMessageContaining("M3 prohibits toggle on the TEXT variant");

    assertThatThrownBy(
            () ->
                new ElwhaButton("Save")
                    .setInteractionMode(ButtonInteractionMode.SELECTABLE)
                    .setVariant(ButtonVariant.TEXT))
        .as("and so is switching an existing toggle to TEXT")
        .isInstanceOf(IllegalStateException.class)
        .hasMessageContaining("M3 prohibits toggle on the TEXT variant");
  }

  // --------------------------------------------------------- pressed property

  @Test
  void pressedPropertyBracketsAPointerPress() {
    final ElwhaButton button = sized(new ElwhaButton("Save"));
    final List<Object> pressed = new ArrayList<>();
    button.addPropertyChangeListener(
        ElwhaButton.PROPERTY_PRESSED, e -> pressed.add(e.getNewValue()));

    Input.press(button, centerX(button), centerY(button));
    Input.release(button, centerX(button), centerY(button));

    assertThat(pressed)
        .as("the group's width-ripple hook sees the press go up and come back down")
        .containsExactly(true, false);
  }

  @Test
  void programmaticPressHookFiresTheSamePropertyOnceEach() {
    final ElwhaButton button = new ElwhaButton("Save");
    final List<Object> pressed = new ArrayList<>();
    button.addPropertyChangeListener(
        ElwhaButton.PROPERTY_PRESSED, e -> pressed.add(e.getNewValue()));

    button.setPressed(true);
    button.setPressed(true);
    button.setPressed(false);

    assertThat(pressed)
        .as("only real transitions are reported — a repeated set is silent")
        .containsExactly(true, false);
  }

  @Test
  void aDisabledButtonNeverEntersThePressedState() {
    final ElwhaButton button = sized(new ElwhaButton("Save"));
    button.setEnabled(false);
    final List<Object> pressed = new ArrayList<>();
    button.addPropertyChangeListener(
        ElwhaButton.PROPERTY_PRESSED, e -> pressed.add(e.getNewValue()));

    Input.press(button, centerX(button), centerY(button));
    Input.release(button, centerX(button), centerY(button));

    assertThat(pressed).as("a disabled button reports no press at all").isEmpty();
  }

  @Test
  void disablingMidPressReleasesThePressAsIfTheCursorHadLeft() {
    final ElwhaButton button = sized(new ElwhaButton("Save"));
    Input.press(button, centerX(button), centerY(button));
    final List<Object> pressed = new ArrayList<>();
    button.addPropertyChangeListener(
        ElwhaButton.PROPERTY_PRESSED, e -> pressed.add(e.getNewValue()));

    button.setEnabled(false);

    assertThat(pressed)
        .as("the group's width-borrow is released rather than stranded by the disable")
        .containsExactly(false);
    assertThat(button.getCursor().getType())
        .as("and a disabled button stops advertising itself as clickable")
        .isEqualTo(Cursor.DEFAULT_CURSOR);
  }

  @Test
  void reEnablingStartsFromRestRatherThanReplayingTheOldPress() {
    final ElwhaButton button = sized(new ElwhaButton("Save"));
    Input.enter(button, centerX(button), centerY(button));
    Input.press(button, centerX(button), centerY(button));

    button.setEnabled(false);
    final List<Object> pressed = new ArrayList<>();
    button.addPropertyChangeListener(
        ElwhaButton.PROPERTY_PRESSED, e -> pressed.add(e.getNewValue()));
    button.setEnabled(true);

    assertThat(pressed)
        .as("re-enabling revives nothing — the press was already cleared on the way down")
        .isEmpty();
    assertThat(button.getCursor().getType())
        .as("and the pointer cursor comes back with the button")
        .isEqualTo(Cursor.HAND_CURSOR);
  }

  @Test
  void aPollDetectedExitReleasesThePressLikeAMouseExit() {
    final ElwhaButton button = sized(new ElwhaButton("Save"));
    Input.enter(button, centerX(button), centerY(button));
    Input.press(button, centerX(button), centerY(button));
    final List<Object> pressed = new ArrayList<>();
    button.addPropertyChangeListener(
        ElwhaButton.PROPERTY_PRESSED, e -> pressed.add(e.getNewValue()));

    // The poll's own guard: an unrealized button is not showing, which is the case the poll exists
    // to catch — the pointer left the window (or the button was hidden) with the press still down,
    // so no mouseExited will ever arrive.
    button.pollHoverState();

    assertThat(pressed)
        .as("a press ended by the poll is still a press the button group hears end")
        .containsExactly(false);
  }

  // --------------------------------------------------------- interaction mode

  @Test
  void droppingOutOfSelectableModeReportsTheDeselection() {
    final ElwhaButton button = selectable(ButtonVariant.FILLED);
    button.setSelected(true);
    final List<Object> selected = new ArrayList<>();
    button.addPropertyChangeListener(
        ElwhaButton.PROPERTY_SELECTED, e -> selected.add(e.getNewValue()));

    button.setInteractionMode(ButtonInteractionMode.CLICKABLE);

    assertThat(button.isSelected()).as("a push button holds no selection").isFalse();
    assertThat(selected)
        .as("and a selection listener is told, rather than left believing the old value")
        .containsExactly(false);
  }

  @Test
  void droppingOutOfSelectableModeIsSilentWhenNothingWasSelected() {
    final ElwhaButton button = selectable(ButtonVariant.FILLED);
    final List<Object> selected = new ArrayList<>();
    button.addPropertyChangeListener(
        ElwhaButton.PROPERTY_SELECTED, e -> selected.add(e.getNewValue()));

    button.setInteractionMode(ButtonInteractionMode.CLICKABLE);

    assertThat(selected).as("there is no transition to report").isEmpty();
  }

  // ------------------------------------------------------------- ripple gate

  @Test
  void rippleGateRoundTripsAndDoesNotTouchActivation() {
    final ElwhaButton button = sized(new ElwhaButton("Save"));
    final int[] fired = {0};
    button.addActionListener(e -> fired[0]++);

    button.setRippleEnabled(false);
    assertThat(button.isRippleEnabled()).as("the ripple gate round-trips").isFalse();

    Input.click(button, centerX(button), centerY(button));

    assertThat(fired[0]).as("suppressing the ripple leaves activation untouched").isEqualTo(1);
    assertThat(button.setRippleEnabled(true).isRippleEnabled())
        .as("and the gate can be reopened")
        .isTrue();
  }
}
