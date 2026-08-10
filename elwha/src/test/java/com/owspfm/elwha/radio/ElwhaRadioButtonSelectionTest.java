package com.owspfm.elwha.radio;

import static org.assertj.core.api.Assertions.assertThat;

import com.owspfm.elwha.testkit.EdtInterceptor;
import com.owspfm.elwha.testkit.ThemeExtension;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

/**
 * Tier A coverage of the two-listener event surface the class Javadoc pins: {@code
 * PROPERTY_SELECTED} fires on every selection change, {@code ActionListener} hears only user
 * gestures. The asymmetry that makes a radio different from a checkbox is also here — a user
 * gesture can only ever <em>select</em> (research §B: "clicking on a radio input always selects
 * it"), so re-affirming an already-selected radio is silent while a programmatic deselect is
 * perfectly legal.
 */
@ExtendWith({EdtInterceptor.class, ThemeExtension.class})
class ElwhaRadioButtonSelectionTest {

  // ------------------------------------------------------------- construction

  @Test
  void freshRadioStartsUnselectedAndFocusable() {
    final ElwhaRadioButton radio = new ElwhaRadioButton();

    assertThat(radio.isSelected()).as("a new radio starts unselected").isFalse();
    assertThat(radio.isFocusable()).as("and is a keyboard stop from birth").isTrue();
    assertThat(radio.isEnabled()).as("and enabled").isTrue();
    assertThat(radio.isOpaque()).as("and transparent, so the ring's corners stay clean").isFalse();
    assertThat(radio.getLabel()).as("with no label").isNull();
    assertThat(radio.getGroup()).as("and no group until one adopts it").isNull();
  }

  @Test
  void constructorsCoverTheStateAndLabelCombinations() {
    assertThat(new ElwhaRadioButton(true).isSelected())
        .as("the boolean constructor seeds the selection")
        .isTrue();
    assertThat(new ElwhaRadioButton("Weekly").getLabel())
        .as("the string constructor seeds the label")
        .isEqualTo("Weekly");
    assertThat(new ElwhaRadioButton("Weekly").isSelected())
        .as("and leaves the radio unselected")
        .isFalse();

    final ElwhaRadioButton both = new ElwhaRadioButton("Weekly", true);
    assertThat(both.getLabel()).as("the two-arg constructor seeds the label").isEqualTo("Weekly");
    assertThat(both.isSelected()).as("and the selection").isTrue();
  }

  @Test
  void blankLabelsNormalizeToNoLabel() {
    final ElwhaRadioButton radio = new ElwhaRadioButton("Weekly");

    radio.setLabel("  ");
    assertThat(radio.getLabel()).as("a blank label is no label").isNull();
  }

  // ----------------------------------------------------- programmatic writes

  @Test
  void setSelectedFiresTheSelectionPropertyOnly() {
    final ElwhaRadioButton radio = new ElwhaRadioButton();
    final int[] changes = {0};
    final int[] actions = {0};
    radio.addPropertyChangeListener(ElwhaRadioButton.PROPERTY_SELECTED, e -> changes[0]++);
    radio.addActionListener(e -> actions[0]++);

    radio.setSelected(true);

    assertThat(radio.isSelected()).as("the write lands").isTrue();
    assertThat(changes[0]).as("a programmatic write is still a state change").isEqualTo(1);
    assertThat(actions[0])
        .as("but it is not a user gesture, so no action event — material-web parity")
        .isZero();
  }

  @Test
  void programmaticDeselectIsLegal() {
    final ElwhaRadioButton radio = new ElwhaRadioButton(true);
    final int[] changes = {0};
    radio.addPropertyChangeListener(ElwhaRadioButton.PROPERTY_SELECTED, e -> changes[0]++);

    radio.setSelected(false);

    assertThat(radio.isSelected())
        .as("only user gestures are select-only — the app may deselect at will")
        .isFalse();
    assertThat(changes[0]).as("and the change is announced").isEqualTo(1);
  }

  @Test
  void writingTheSameSelectionFiresNothing() {
    final ElwhaRadioButton radio = new ElwhaRadioButton(true);
    final int[] changes = {0};
    radio.addPropertyChangeListener(ElwhaRadioButton.PROPERTY_SELECTED, e -> changes[0]++);

    radio.setSelected(true);

    assertThat(changes[0]).as("a redundant write is not a change").isZero();
  }

  @Test
  void selectionEventNamesTheRadioAsItsSource() {
    final ElwhaRadioButton radio = new ElwhaRadioButton();
    final List<Object> sources = new ArrayList<>();
    radio.addPropertyChangeListener(
        ElwhaRadioButton.PROPERTY_SELECTED, e -> sources.add(e.getSource()));

    radio.setSelected(true);

    assertThat(sources)
        .as("a listener on several radios can tell which one moved")
        .containsExactly(radio);
  }

  // ---------------------------------------------------------- user gestures

  @Test
  void doClickSelectsAndFiresBothListenerKinds() {
    final ElwhaRadioButton radio = new ElwhaRadioButton();
    final List<ActionEvent> actions = new ArrayList<>();
    final int[] changes = {0};
    radio.addActionListener(actions::add);
    radio.addPropertyChangeListener(ElwhaRadioButton.PROPERTY_SELECTED, e -> changes[0]++);

    radio.doClick();

    assertThat(radio.isSelected()).as("doClick performs the user-equivalent select").isTrue();
    assertThat(actions).as("a user gesture fires the action listeners").hasSize(1);
    assertThat(changes[0]).as("and the selection property fires too").isEqualTo(1);
  }

  @Test
  void actionCommandIsAlwaysSelected() {
    final ElwhaRadioButton radio = new ElwhaRadioButton();
    final List<ActionEvent> actions = new ArrayList<>();
    radio.addActionListener(actions::add);

    radio.doClick();

    assertThat(actions.get(0).getActionCommand())
        .as("no user gesture deselects a radio, so there is no 'deselected' command to carry")
        .isEqualTo("selected");
    assertThat(actions.get(0).getSource()).as("the radio is the source").isSameAs(radio);
  }

  @Test
  void reaffirmingAnAlreadySelectedRadioFiresNothing() {
    final ElwhaRadioButton radio = new ElwhaRadioButton(true);
    final int[] actions = {0};
    final int[] changes = {0};
    radio.addActionListener(e -> actions[0]++);
    radio.addPropertyChangeListener(ElwhaRadioButton.PROPERTY_SELECTED, e -> changes[0]++);

    radio.doClick();

    assertThat(radio.isSelected()).as("it stays selected").isTrue();
    assertThat(actions[0]).as("re-affirming fires no action event").isZero();
    assertThat(changes[0]).as("and no selection event — nothing changed").isZero();
  }

  @Test
  void doClickIsANoOpWhileDisabled() {
    final ElwhaRadioButton radio = new ElwhaRadioButton();
    radio.setEnabled(false);
    final int[] actions = {0};
    radio.addActionListener(e -> actions[0]++);

    radio.doClick();

    assertThat(radio.isSelected()).as("a disabled radio never selects from a user path").isFalse();
    assertThat(actions[0]).as("and fires nothing").isZero();
  }

  @Test
  void programmaticWritesStillLandWhileDisabled() {
    final ElwhaRadioButton radio = new ElwhaRadioButton();
    radio.setEnabled(false);

    radio.setSelected(true);

    assertThat(radio.isSelected())
        .as("disabled blocks user gestures, not the app's own model writes")
        .isTrue();
  }

  // -------------------------------------------------------- listener plumbing

  @Test
  void removedListenersStopHearingChanges() {
    final ElwhaRadioButton radio = new ElwhaRadioButton();
    final int[] changes = {0};
    final int[] actions = {0};
    final PropertyChangeListener changeListener = e -> changes[0]++;
    final ActionListener actionListener = e -> actions[0]++;
    radio.addPropertyChangeListener(ElwhaRadioButton.PROPERTY_SELECTED, changeListener);
    radio.addActionListener(actionListener);
    radio.doClick();

    radio.removePropertyChangeListener(ElwhaRadioButton.PROPERTY_SELECTED, changeListener);
    radio.removeActionListener(actionListener);
    radio.setSelected(false);
    radio.doClick();

    assertThat(changes[0])
        .as("the removed selection listener heard only the first gesture")
        .isEqualTo(1);
    assertThat(actions[0]).as("and so did the removed action listener").isEqualTo(1);
  }

  @Test
  void everyRegisteredListenerHearsEachGesture() {
    final ElwhaRadioButton radio = new ElwhaRadioButton();
    final int[] first = {0};
    final int[] second = {0};
    radio.addActionListener(e -> first[0]++);
    radio.addActionListener(e -> second[0]++);

    radio.doClick();

    assertThat(first[0]).as("the first listener fires").isEqualTo(1);
    assertThat(second[0]).as("and so does the second").isEqualTo(1);
  }

  // ------------------------------------------------------- accessible label

  @Test
  void accessibleLabelOverrideRoundTrips() {
    final ElwhaRadioButton radio = new ElwhaRadioButton();

    radio.setAccessibleLabel("Weekly digest");

    assertThat(radio.getAccessibleLabel())
        .as("the accessible-name override is stored independently of the visual label")
        .isEqualTo("Weekly digest");
    assertThat(radio.getLabel()).as("and creates no visual label").isNull();
  }

  // -------------------------------------------------------- property changes

  @Test
  void everySelectionChangeCarriesBothValues() {
    final ElwhaRadioButton radio = new ElwhaRadioButton();
    final List<PropertyChangeEvent> events = new ArrayList<>();
    radio.addPropertyChangeListener(ElwhaRadioButton.PROPERTY_SELECTED, events::add);

    radio.setSelected(true);
    radio.setSelected(false);

    assertThat(events).as("one event per real transition").hasSize(2);
    assertThat(events.get(0).getOldValue()).as("the old value is carried").isEqualTo(false);
    assertThat(events.get(0).getNewValue()).as("the new value is carried").isEqualTo(true);
    assertThat(events.get(1).getOldValue()).as("and again on the way back").isEqualTo(true);
    assertThat(events.get(1).getNewValue()).isEqualTo(false);
  }

  @Test
  void aSelectionListenerIsNotWokenByOtherProperties() {
    final ElwhaRadioButton radio = new ElwhaRadioButton();
    final int[] changes = {0};
    radio.addPropertyChangeListener(ElwhaRadioButton.PROPERTY_SELECTED, e -> changes[0]++);

    radio.setEnabled(false);

    assertThat(changes[0])
        .as("subscription is key-scoped — the enabled property must not wake it")
        .isZero();
  }
}
