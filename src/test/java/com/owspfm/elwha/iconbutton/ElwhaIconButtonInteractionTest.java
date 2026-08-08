package com.owspfm.elwha.iconbutton;

import static org.assertj.core.api.Assertions.assertThat;

import com.owspfm.elwha.testkit.EdtInterceptor;
import com.owspfm.elwha.testkit.Input;
import com.owspfm.elwha.testkit.ThemeExtension;
import java.awt.Component;
import java.awt.Graphics;
import java.awt.event.ActionEvent;
import java.util.ArrayList;
import java.util.List;
import javax.swing.Icon;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

/**
 * Tier A event-contract coverage for {@link ElwhaIconButton} — activation through the real mouse
 * pipeline and the {@code WHEN_FOCUSED} bindings, the disabled guards of the #432 class, and the
 * declarative {@code setIcons(resting, selected)} swap including its press-preview flash.
 *
 * <p>Which glyph is live is read through {@link ElwhaIconButton#getIconBounds()} using two
 * differently-sized fixture icons, so the swap is observed without probing a single glyph pixel.
 */
@ExtendWith({EdtInterceptor.class, ThemeExtension.class})
class ElwhaIconButtonInteractionTest {

  private static final String ACTIVATE = "elwhaiconbutton.activate";
  private static final int RESTING_SIDE = 16;
  private static final int SELECTED_SIDE = 28;
  private static final int SIDE = IconButtonSize.XL.containerPx();

  /** A fixed-size icon distinguishable by its dimensions alone. */
  private static final class SizedIcon implements Icon {
    private final int side;

    SizedIcon(final int side) {
      this.side = side;
    }

    @Override
    public void paintIcon(final Component c, final Graphics g, final int x, final int y) {
      // fixture only; the swap is observed through the advertised bounds
    }

    @Override
    public int getIconWidth() {
      return side;
    }

    @Override
    public int getIconHeight() {
      return side;
    }
  }

  private static ElwhaIconButton sized() {
    final ElwhaIconButton button = new ElwhaIconButton().setButtonSize(IconButtonSize.XL);
    button.setSize(SIDE, SIDE);
    return button;
  }

  private static ElwhaIconButton withIconPair() {
    final ElwhaIconButton button =
        sized().setIcons(new SizedIcon(RESTING_SIDE), new SizedIcon(SELECTED_SIDE));
    button.setSize(SIDE, SIDE);
    return button;
  }

  // ------------------------------------------------------------- activation

  @Test
  void aClickFiresEachActionListenerExactlyOnce() {
    final ElwhaIconButton button = sized();
    final List<ActionEvent> heard = new ArrayList<>();
    button.addActionListener(heard::add);

    Input.click(button, SIDE / 2, SIDE / 2);

    assertThat(heard).as("a press-release pair inside the button activates it once").hasSize(1);
    assertThat(heard.get(0).getActionCommand())
        .as("the action command identifies the activation")
        .isEqualTo("click");
  }

  @Test
  void releasingOutsideTheButtonCancelsTheClick() {
    final ElwhaIconButton button = sized();
    final int[] fired = {0};
    button.addActionListener(e -> fired[0]++);

    Input.press(button, SIDE / 2, SIDE / 2);
    Input.release(button, 900, 900);

    assertThat(fired[0]).as("a drag-off release is a cancelled click, not an activation").isZero();
  }

  @Test
  void spaceAndEnterAreBoundWhenFocusedAndBothActivate() {
    final ElwhaIconButton button = sized();
    final int[] fired = {0};
    button.addActionListener(e -> fired[0]++);

    Input.pressBoundKey(button, "pressed SPACE", ACTIVATE);
    Input.pressBoundKey(button, "pressed ENTER", ACTIVATE);

    assertThat(fired[0]).as("both bound keys activate the button").isEqualTo(2);
  }

  @Test
  void removingAListenerStopsItsDelivery() {
    final ElwhaIconButton button = sized();
    final int[] fired = {0};
    final java.awt.event.ActionListener listener = e -> fired[0]++;
    button.addActionListener(listener);
    button.addActionListener(null);

    button.removeActionListener(listener);
    Input.click(button, SIDE / 2, SIDE / 2);

    assertThat(fired[0])
        .as("a removed listener hears nothing, and a null one was never added")
        .isZero();
  }

  // -------------------------------------------------------- disabled guards

  @Test
  void aDisabledIconButtonFiresNothingThroughAnyPath() {
    final ElwhaIconButton button = sized();
    button.setEnabled(false);
    final int[] fired = {0};
    button.addActionListener(e -> fired[0]++);

    Input.click(button, SIDE / 2, SIDE / 2);
    Input.pressBoundKey(button, "pressed SPACE", ACTIVATE);
    Input.pressBoundKey(button, "pressed ENTER", ACTIVATE);

    assertThat(fired[0]).as("mouse and both key bindings are guarded while disabled").isZero();
  }

  @Test
  void aDisabledToggleNeverFlips() {
    final ElwhaIconButton button = sized().setInteractionMode(IconButtonInteractionMode.SELECTABLE);
    button.setEnabled(false);
    final int[] changes = {0};
    button.addSelectionChangeListener(e -> changes[0]++);

    Input.click(button, SIDE / 2, SIDE / 2);
    Input.pressBoundKey(button, "pressed SPACE", ACTIVATE);

    assertThat(button.isSelected()).as("a disabled toggle holds its state").isFalse();
    assertThat(changes[0]).as("and reports no selection change").isZero();
  }

  // ------------------------------------------------------------- selection

  @Test
  void clickingASelectableIconButtonTogglesAndFiresBothContracts() {
    final ElwhaIconButton button = sized().setInteractionMode(IconButtonInteractionMode.SELECTABLE);
    final int[] actions = {0};
    final List<Object> selections = new ArrayList<>();
    button.addActionListener(e -> actions[0]++);
    button.addSelectionChangeListener(e -> selections.add(e.getNewValue()));

    Input.click(button, SIDE / 2, SIDE / 2);
    Input.click(button, SIDE / 2, SIDE / 2);

    assertThat(button.isSelected()).as("two clicks return to unselected").isFalse();
    assertThat(actions[0]).as("each toggle is also an activation").isEqualTo(2);
    assertThat(selections)
        .as("the selected property reports each new value in order")
        .containsExactly(true, false);
  }

  @Test
  void clickingAClickableIconButtonLeavesTheSelectedFlagAlone() {
    final ElwhaIconButton button = sized();

    Input.click(button, SIDE / 2, SIDE / 2);

    assertThat(button.isSelected()).as("a push button holds no selection").isFalse();
  }

  @Test
  void programmaticSetSelectedFiresTheSelectedPropertyAndNoAction() {
    final ElwhaIconButton button = sized().setInteractionMode(IconButtonInteractionMode.SELECTABLE);
    final int[] actions = {0};
    final int[] changes = {0};
    button.addActionListener(e -> actions[0]++);
    button.addSelectionChangeListener(e -> changes[0]++);

    button.setSelected(true);
    button.setSelected(true);

    assertThat(changes[0]).as("only a real transition fires the selected property").isEqualTo(1);
    assertThat(actions[0]).as("and setSelected is never an activation").isZero();
  }

  // ------------------------------------------------------------- icon swap

  @Test
  void aSelectedToggleRendersItsSelectedGlyph() {
    final ElwhaIconButton button =
        withIconPair().setInteractionMode(IconButtonInteractionMode.SELECTABLE);

    assertThat(button.getIconBounds().width)
        .as("the resting glyph is live while unselected")
        .isEqualTo(RESTING_SIDE);

    button.setSelected(true);

    assertThat(button.getIconBounds().width)
        .as("the selected glyph takes over once toggled on")
        .isEqualTo(SELECTED_SIDE);
  }

  @Test
  void aLivePressPreviewsTheSelectedGlyphEvenOnAPushButton() {
    final ElwhaIconButton button = withIconPair();

    Input.press(button, SIDE / 2, SIDE / 2);
    assertThat(button.getIconBounds().width)
        .as("the press flashes the filled glyph as tactile feedback")
        .isEqualTo(SELECTED_SIDE);

    Input.release(button, SIDE / 2, SIDE / 2);
    assertThat(button.getIconBounds().width)
        .as("and a push button reverts on release — the preview never sticks")
        .isEqualTo(RESTING_SIDE);
  }

  @Test
  void aTogglesPressPreviewSettlesOnTheCommittedState() {
    final ElwhaIconButton button =
        withIconPair().setInteractionMode(IconButtonInteractionMode.SELECTABLE);

    Input.click(button, SIDE / 2, SIDE / 2);

    assertThat(button.getIconBounds().width)
        .as("toggling on leaves the previewed glyph in place with no flicker")
        .isEqualTo(SELECTED_SIDE);

    Input.click(button, SIDE / 2, SIDE / 2);

    assertThat(button.getIconBounds().width)
        .as("and toggling off returns to the outline glyph")
        .isEqualTo(RESTING_SIDE);
  }

  @Test
  void omittingTheSelectedGlyphOptsOutOfTheSwapEntirely() {
    final ElwhaIconButton button =
        sized()
            .setIcons(new SizedIcon(RESTING_SIDE), null)
            .setInteractionMode(IconButtonInteractionMode.SELECTABLE);
    button.setSize(SIDE, SIDE);

    button.setSelected(true);

    assertThat(button.getIconBounds().width)
        .as("with no selected glyph installed the resting one renders in every state")
        .isEqualTo(RESTING_SIDE);
  }

  // --------------------------------------------------------- interaction mode

  @Test
  void aPushButtonCannotBeLatchedIntoItsSelectedGlyph() {
    final ElwhaIconButton button = withIconPair();
    final int[] changes = {0};
    button.addSelectionChangeListener(e -> changes[0]++);

    button.setSelected(true);

    assertThat(button.isSelected())
        .as("CLICKABLE has no persistent selected state, so the setter is inert")
        .isFalse();
    assertThat(changes[0]).as("and an inert setter fires nothing").isZero();
    assertThat(button.getIconBounds().width)
        .as(
            "the resting glyph stays live — currentIcon reads `selected`, so a latched flag would"
                + " have pinned the filled glyph on a push button forever")
        .isEqualTo(RESTING_SIDE);
  }

  @Test
  void droppingOutOfSelectableModeClearsAndReportsTheSelection() {
    final ElwhaIconButton button =
        withIconPair().setInteractionMode(IconButtonInteractionMode.SELECTABLE);
    button.setSelected(true);
    final List<Object> selected = new ArrayList<>();
    button.addPropertyChangeListener(
        ElwhaIconButton.PROPERTY_SELECTED, e -> selected.add(e.getNewValue()));

    button.setInteractionMode(IconButtonInteractionMode.CLICKABLE);

    assertThat(button.isSelected()).as("a push button holds no selection").isFalse();
    assertThat(selected)
        .as("and a selection listener is told, rather than left believing the old value")
        .containsExactly(false);
    assertThat(button.getIconBounds().width)
        .as("the glyph follows the state back to resting")
        .isEqualTo(RESTING_SIDE);
  }

  // ---------------------------------------------------- programmatic activation

  @Test
  void doClickDeliversTheActionSourcedAtTheButton() {
    final ElwhaIconButton button = sized();
    final List<Object> sources = new ArrayList<>();
    button.addActionListener(e -> sources.add(e.getSource()));

    button.doClick();

    assertThat(sources)
        .as(
            "#238 — a caller standing in for the user (an overflow menu row) has to reach the "
                + "consumer's handlers with the original button as the event source")
        .containsExactly(button);
  }

  @Test
  void doClickToggglesASelectableButton() {
    final ElwhaIconButton button = sized().setInteractionMode(IconButtonInteractionMode.SELECTABLE);

    button.doClick();

    assertThat(button.isSelected())
        .as("selection flips first, exactly as a real click orders it")
        .isTrue();
  }

  @Test
  void doClickIsInertOnADisabledButton() {
    final ElwhaIconButton button = sized();
    final int[] fired = {0};
    button.addActionListener(e -> fired[0]++);
    button.setEnabled(false);

    button.doClick();

    assertThat(fired[0]).as("#432 — every activation path shares the disabled guard").isZero();
  }

  // ------------------------------------------------------------- ripple gate

  @Test
  void aSuppressedRippleLeavesActivationUntouched() {
    final ElwhaIconButton button = sized().setRippleEnabled(false);
    final int[] fired = {0};
    button.addActionListener(e -> fired[0]++);

    Input.click(button, SIDE / 2, SIDE / 2);

    assertThat(button.isRippleEnabled()).as("the ripple gate round-trips").isFalse();
    assertThat(fired[0]).as("and gates only the ripple, never the action").isEqualTo(1);
  }
}
