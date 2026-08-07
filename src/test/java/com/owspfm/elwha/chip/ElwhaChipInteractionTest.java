package com.owspfm.elwha.chip;

import static org.assertj.core.api.Assertions.assertThat;

import com.owspfm.elwha.testkit.EdtInterceptor;
import com.owspfm.elwha.testkit.Input;
import com.owspfm.elwha.testkit.ThemeExtension;
import java.awt.Cursor;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.InputEvent;
import java.awt.event.KeyEvent;
import java.awt.event.MouseEvent;
import java.beans.PropertyChangeEvent;
import java.util.ArrayList;
import java.util.List;
import javax.accessibility.AccessibleRole;
import javax.swing.JComponent;
import javax.swing.KeyStroke;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;

/**
 * Tier A coverage of what each {@link ChipInteractionMode} promises. The distinction that matters
 * most is CLICKABLE versus SELECTABLE: both fire an action, but only SELECTABLE owns a selected
 * state, and a CLICKABLE chip must never self-toggle — that is the wiring a hosting list depends on
 * when it takes selection over.
 */
@ExtendWith({EdtInterceptor.class, ThemeExtension.class})
class ElwhaChipInteractionTest {

  private static final String ACTIVATE = "elwhachip.activate";
  private static final String CONTEXT_MENU = "elwhachip.contextMenu";
  private static final int WIDTH = 120;
  private static final int HEIGHT = 40;
  private static final int CENTER_X = 60;
  private static final int CENTER_Y = 20;

  private static ElwhaChip sized(final ChipInteractionMode mode) {
    final ElwhaChip chip = new ElwhaChip("Demand").setInteractionMode(mode);
    chip.setSize(WIDTH, HEIGHT);
    return chip;
  }

  private static Object binding(final ElwhaChip chip, final KeyStroke stroke) {
    return chip.getInputMap(JComponent.WHEN_FOCUSED).get(stroke);
  }

  // ------------------------------------------------------------------- modes

  @ParameterizedTest
  @EnumSource(ChipInteractionMode.class)
  void everyModeRoundTrips(final ChipInteractionMode mode) {
    assertThat(new ElwhaChip("Demand").setInteractionMode(mode).getInteractionMode())
        .as(mode + " round-trips")
        .isEqualTo(mode);
  }

  @Test
  void aNullModeIsIgnored() {
    final ElwhaChip chip = sized(ChipInteractionMode.CLICKABLE);

    chip.setInteractionMode(null);

    assertThat(chip.getInteractionMode())
        .as("null is a no-op — there is no 'no interaction' value beyond STATIC")
        .isEqualTo(ChipInteractionMode.CLICKABLE);
  }

  @Test
  void onlyTheActivatableModesAreFocusable() {
    assertThat(sized(ChipInteractionMode.STATIC).isFocusable())
        .as("an inert chip is not a keyboard stop")
        .isFalse();
    assertThat(sized(ChipInteractionMode.HOVERABLE).isFocusable())
        .as("nor is one that only lights up under the pointer")
        .isFalse();
    assertThat(sized(ChipInteractionMode.CLICKABLE).isFocusable())
        .as("a clickable chip is reachable by keyboard")
        .isTrue();
    assertThat(sized(ChipInteractionMode.SELECTABLE).isFocusable())
        .as("and so is a selectable one")
        .isTrue();
  }

  @Test
  void cursorFollowsTheMode() {
    assertThat(sized(ChipInteractionMode.STATIC).getCursor().getType())
        .as("an inert chip keeps the default pointer")
        .isEqualTo(Cursor.DEFAULT_CURSOR);
    assertThat(sized(ChipInteractionMode.CLICKABLE).getCursor().getType())
        .as("an interactive one advertises itself with the hand")
        .isEqualTo(Cursor.HAND_CURSOR);
  }

  // ------------------------------------------------------------------ clicks

  @Test
  void aClickableChipFiresAnActionAndNeverSelfToggles() {
    final ElwhaChip chip = sized(ChipInteractionMode.CLICKABLE);
    final List<ActionEvent> fired = new ArrayList<>();
    chip.addActionListener(fired::add);

    Input.click(chip, CENTER_X, CENTER_Y);

    assertThat(fired).as("a click fires one action").hasSize(1);
    assertThat(fired.get(0).getActionCommand()).as("with the click command").isEqualTo("click");
    assertThat(chip.isSelected())
        .as("and CLICKABLE holds no selection of its own — a host list owns that")
        .isFalse();
  }

  @Test
  void aSelectableChipTogglesAndFires() {
    final ElwhaChip chip = sized(ChipInteractionMode.SELECTABLE);
    final int[] actions = {0};
    chip.addActionListener(e -> actions[0]++);

    Input.click(chip, CENTER_X, CENTER_Y);
    assertThat(chip.isSelected()).as("the first click selects").isTrue();
    assertThat(actions[0]).as("firing once").isEqualTo(1);

    Input.click(chip, CENTER_X, CENTER_Y);
    assertThat(chip.isSelected()).as("and the second deselects").isFalse();
    assertThat(actions[0]).as("firing again").isEqualTo(2);
  }

  @Test
  void aStaticChipIgnoresClicks() {
    final ElwhaChip chip = sized(ChipInteractionMode.STATIC);
    final int[] actions = {0};
    chip.addActionListener(e -> actions[0]++);

    Input.click(chip, CENTER_X, CENTER_Y);

    assertThat(actions[0]).as("an inert chip fires nothing").isZero();
    assertThat(chip.isSelected()).as("and selects nothing").isFalse();
  }

  @Test
  void aHoverableChipFiresNoAction() {
    final ElwhaChip chip = sized(ChipInteractionMode.HOVERABLE);
    final int[] actions = {0};
    chip.addActionListener(e -> actions[0]++);

    Input.click(chip, CENTER_X, CENTER_Y);

    assertThat(actions[0])
        .as("HOVERABLE gives feedback only — its trailing button handles the real click")
        .isZero();
  }

  @Test
  void releasingOutsideTheBoundsCancelsTheClick() {
    final ElwhaChip chip = sized(ChipInteractionMode.CLICKABLE);
    final int[] actions = {0};
    chip.addActionListener(e -> actions[0]++);

    Input.press(chip, CENTER_X, CENTER_Y);
    Input.release(chip, WIDTH + 200, CENTER_Y);

    assertThat(actions[0]).as("dragging off the chip abandons the click").isZero();
  }

  @Test
  void releasingWithNoArmedPressIsIgnored() {
    final ElwhaChip chip = sized(ChipInteractionMode.SELECTABLE);
    final int[] actions = {0};
    chip.addActionListener(e -> actions[0]++);

    Input.release(chip, CENTER_X, CENTER_Y);

    assertThat(actions[0]).as("a stray release fires nothing").isZero();
    assertThat(chip.isSelected()).as("and toggles nothing").isFalse();
  }

  @Test
  void cancelPendingClickDropsAnArmedPress() {
    final ElwhaChip chip = sized(ChipInteractionMode.SELECTABLE);
    final int[] actions = {0};
    chip.addActionListener(e -> actions[0]++);

    Input.press(chip, CENTER_X, CENTER_Y);
    final ElwhaChip returned = chip.cancelPendingClick();
    Input.release(chip, CENTER_X, CENTER_Y);

    assertThat(actions[0])
        .as("a host that has turned the press into a drag cancels the click it would have fired")
        .isZero();
    assertThat(chip.isSelected()).as("and no toggle lands either").isFalse();
    assertThat(returned).as("the call is fluent").isSameAs(chip);
  }

  // ---------------------------------------------------------------- keyboard

  @Test
  void spaceAndEnterBothActivate() {
    final ElwhaChip chip = sized(ChipInteractionMode.CLICKABLE);

    assertThat(binding(chip, KeyStroke.getKeyStroke(KeyEvent.VK_SPACE, 0)))
        .as("Space activates a chip")
        .isEqualTo(ACTIVATE);
    assertThat(binding(chip, KeyStroke.getKeyStroke(KeyEvent.VK_ENTER, 0)))
        .as("and so does Enter — a chip reads as a button, unlike the switch")
        .isEqualTo(ACTIVATE);
  }

  @Test
  void spaceFiresTheActionLikeAClick() {
    final ElwhaChip chip = sized(ChipInteractionMode.CLICKABLE);
    final int[] actions = {0};
    chip.addActionListener(e -> actions[0]++);

    Input.pressBoundKey(chip, "pressed SPACE", ACTIVATE);

    assertThat(actions[0]).as("keyboard activation is a real activation").isEqualTo(1);
  }

  @Test
  void spaceTogglesASelectableChip() {
    final ElwhaChip chip = sized(ChipInteractionMode.SELECTABLE);

    Input.pressBoundKey(chip, "pressed SPACE", ACTIVATE);

    assertThat(chip.isSelected())
        .as("the keyboard path runs the same activation the pointer does")
        .isTrue();
  }

  @Test
  void boundKeyActionRefusesOnAStaticChip() {
    final ElwhaChip chip = sized(ChipInteractionMode.STATIC);
    final int[] actions = {0};
    chip.addActionListener(e -> actions[0]++);

    Input.pressBoundKey(chip, "pressed SPACE", ACTIVATE);

    assertThat(actions[0])
        .as("the binding stays installed but the action refuses while the mode is inert")
        .isZero();
  }

  @Test
  void contextMenuKeysAreBound() {
    final ElwhaChip chip = sized(ChipInteractionMode.CLICKABLE);

    assertThat(binding(chip, KeyStroke.getKeyStroke(KeyEvent.VK_CONTEXT_MENU, 0)))
        .as("the dedicated context-menu key is bound")
        .isEqualTo(CONTEXT_MENU);
    assertThat(binding(chip, KeyStroke.getKeyStroke(KeyEvent.VK_F10, InputEvent.SHIFT_DOWN_MASK)))
        .as("and so is the Shift-F10 fallback for keyboards without one")
        .isEqualTo(CONTEXT_MENU);
  }

  @Test
  void contextMenuActionInvokesTheCallbackWithASyntheticEvent() {
    final ElwhaChip chip = sized(ChipInteractionMode.CLICKABLE);
    final List<MouseEvent> seen = new ArrayList<>();
    chip.setContextMenuCallback(seen::add);

    Input.pressBoundKey(chip, "pressed CONTEXT_MENU", CONTEXT_MENU);

    assertThat(seen).as("the keyboard route reaches the same callback the pointer does").hasSize(1);
    assertThat(seen.get(0).getSource())
        .as("naming the chip as the source so a popup can anchor to it")
        .isSameAs(chip);
    assertThat(seen.get(0).isPopupTrigger()).as("and flagged as a popup trigger").isTrue();
  }

  @Test
  void contextMenuActionIsANoOpWithNoCallback() {
    final ElwhaChip chip = sized(ChipInteractionMode.CLICKABLE);

    assertThat(chip.hasContextMenu()).as("no callback is installed by default").isFalse();

    Input.pressBoundKey(chip, "pressed CONTEXT_MENU", CONTEXT_MENU);

    assertThat(chip.hasContextMenu()).as("and invoking the action installs nothing").isFalse();
  }

  @Test
  void aContextMenuCallbackCanBeClearedAgain() {
    final ElwhaChip chip = sized(ChipInteractionMode.CLICKABLE);

    chip.setContextMenuCallback(event -> {});
    assertThat(chip.hasContextMenu()).as("the callback is reported as installed").isTrue();

    chip.setContextMenuCallback(null);
    assertThat(chip.hasContextMenu()).as("and null clears it").isFalse();
  }

  // ------------------------------------------------------------- selection

  @Test
  void selectionFiresAPropertyChangeWithBothValues() {
    final ElwhaChip chip = sized(ChipInteractionMode.SELECTABLE);
    final List<PropertyChangeEvent> events = new ArrayList<>();
    chip.addSelectionChangeListener(events::add);

    chip.setSelected(true);

    assertThat(events).as("a programmatic write is still a selection change").hasSize(1);
    assertThat(events.get(0).getOldValue()).as("carrying the old value").isEqualTo(false);
    assertThat(events.get(0).getNewValue()).as("and the new one").isEqualTo(true);
  }

  @Test
  void aRedundantSelectionWriteFiresNothing() {
    final ElwhaChip chip = sized(ChipInteractionMode.SELECTABLE);
    chip.setSelected(true);
    final int[] events = {0};
    chip.addSelectionChangeListener(e -> events[0]++);

    chip.setSelected(true);

    assertThat(events[0]).as("nothing changed, so nothing is announced").isZero();
  }

  @Test
  void aClickOnASelectableChipAnnouncesTheToggle() {
    final ElwhaChip chip = sized(ChipInteractionMode.SELECTABLE);
    final int[] events = {0};
    chip.addSelectionChangeListener(e -> events[0]++);

    Input.click(chip, CENTER_X, CENTER_Y);

    assertThat(events[0]).as("the user route announces through the same property").isEqualTo(1);
  }

  // -------------------------------------------------------------- disabled

  @Test
  void aDisabledChipIgnoresClicks() {
    final ElwhaChip chip = sized(ChipInteractionMode.SELECTABLE);
    chip.setEnabled(false);
    final int[] actions = {0};
    chip.addActionListener(e -> actions[0]++);

    Input.click(chip, CENTER_X, CENTER_Y);

    assertThat(actions[0]).as("a disabled chip fires nothing").isZero();
    assertThat(chip.isSelected()).as("and toggles nothing").isFalse();
  }

  @Test
  void aDisabledChipIgnoresTheBoundKeyAction() {
    final ElwhaChip chip = sized(ChipInteractionMode.SELECTABLE);
    chip.setEnabled(false);
    final int[] actions = {0};
    chip.addActionListener(e -> actions[0]++);

    Input.pressBoundKey(chip, "pressed SPACE", ACTIVATE);

    assertThat(actions[0])
        .as(
            "the guard is outer-qualified, so the action refuses rather than reading"
                + " AbstractAction's own always-true flag")
        .isZero();
    assertThat(chip.isSelected()).as("and nothing toggles").isFalse();
  }

  @Test
  void disablingDropsTheHandCursorAndDisablesTheTrailingButton() {
    final ElwhaChip chip = sized(ChipInteractionMode.CLICKABLE);
    chip.setTrailingIcon(new BlankIcon(), "Remove", () -> {});

    chip.setEnabled(false);

    assertThat(chip.getCursor().getType())
        .as("a disabled chip stops advertising itself as clickable")
        .isEqualTo(Cursor.DEFAULT_CURSOR);
    assertThat(chip.getTrailingButton().isEnabled())
        .as("and takes its inline button down with it")
        .isFalse();
  }

  @Test
  void reEnablingRestoresTheGesturePath() {
    final ElwhaChip chip = sized(ChipInteractionMode.SELECTABLE);
    chip.setEnabled(false);
    Input.click(chip, CENTER_X, CENTER_Y);

    chip.setEnabled(true);
    Input.click(chip, CENTER_X, CENTER_Y);

    assertThat(chip.isSelected())
        .as("the guard is a live read of isEnabled, not a latched flag")
        .isTrue();
  }

  // ------------------------------------------------------- listener plumbing

  @Test
  void addingANullActionListenerIsIgnored() {
    final ElwhaChip chip = sized(ChipInteractionMode.SELECTABLE);

    chip.addActionListener(null);
    Input.click(chip, CENTER_X, CENTER_Y);

    assertThat(chip.isSelected())
        .as("a null registration does not break the next activation")
        .isTrue();
  }

  @Test
  void removedActionListenersStopHearingClicks() {
    final ElwhaChip chip = sized(ChipInteractionMode.CLICKABLE);
    final int[] actions = {0};
    final ActionListener listener = e -> actions[0]++;
    chip.addActionListener(listener);
    Input.click(chip, CENTER_X, CENTER_Y);

    chip.removeActionListener(listener);
    Input.click(chip, CENTER_X, CENTER_Y);

    assertThat(actions[0]).as("a removed listener hears nothing further").isEqualTo(1);
  }

  // ------------------------------------------------------------------- a11y

  @Test
  void accessibleRoleFollowsTheMode() {
    assertThat(sized(ChipInteractionMode.SELECTABLE).getAccessibleContext().getAccessibleRole())
        .as("a chip that holds a selection announces as a toggle")
        .isEqualTo(AccessibleRole.TOGGLE_BUTTON);
    assertThat(sized(ChipInteractionMode.CLICKABLE).getAccessibleContext().getAccessibleRole())
        .as("and every other mode as a push button")
        .isEqualTo(AccessibleRole.PUSH_BUTTON);
    assertThat(sized(ChipInteractionMode.STATIC).getAccessibleContext().getAccessibleRole())
        .as("including the inert one")
        .isEqualTo(AccessibleRole.PUSH_BUTTON);
  }

  @Test
  void accessibleNameFallsBackToTheChipText() {
    final ElwhaChip chip = sized(ChipInteractionMode.CLICKABLE);

    assertThat(chip.getAccessibleContext().getAccessibleName())
        .as("the visible label names the chip")
        .isEqualTo("Demand");

    chip.getAccessibleContext().setAccessibleName("Demand filter");
    assertThat(chip.getAccessibleContext().getAccessibleName())
        .as("and an explicit name outranks it")
        .isEqualTo("Demand filter");
  }

  /** A zero-cost stand-in where a test only needs the trailing slot to be occupied. */
  private static final class BlankIcon implements javax.swing.Icon {

    @Override
    public void paintIcon(
        final java.awt.Component c, final java.awt.Graphics g, final int x, final int y) {
      // Nothing to draw — presence in the slot is the whole point.
    }

    @Override
    public int getIconWidth() {
      return 14;
    }

    @Override
    public int getIconHeight() {
      return 14;
    }
  }
}
