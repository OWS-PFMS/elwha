package com.owspfm.elwha.checkbox;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.owspfm.elwha.checkbox.ElwhaCheckbox.CheckState;
import com.owspfm.elwha.testkit.EdtInterceptor;
import com.owspfm.elwha.testkit.ThemeExtension;
import java.beans.PropertyChangeEvent;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;

/**
 * Tier A coverage of the tri-state model and the plain properties around it — the cycle order, the
 * boolean conveniences and how they read an indeterminate box, and the {@code checkState}
 * property-change contract that parent/child wiring hangs off. Oracle: {@code
 * docs/research/elwha-checkbox-design.md} §8 and the class Javadoc.
 */
@ExtendWith({EdtInterceptor.class, ThemeExtension.class})
class ElwhaCheckboxStateTest {

  // ------------------------------------------------------------- construction

  @Test
  void freshCheckboxStartsUncheckedAndInteractive() {
    final ElwhaCheckbox box = new ElwhaCheckbox();

    assertThat(box.getCheckState())
        .as("a new checkbox starts unchecked")
        .isEqualTo(CheckState.UNCHECKED);
    assertThat(box.isChecked()).as("unchecked reads false").isFalse();
    assertThat(box.isIndeterminate()).as("unchecked is not indeterminate").isFalse();
    assertThat(box.isFocusable()).as("a checkbox is a keyboard stop from birth").isTrue();
    assertThat(box.isEnabled()).as("a new checkbox is enabled").isTrue();
    assertThat(box.isOpaque())
        .as("an opaque box would have Swing pre-fill a square under the round container")
        .isFalse();
    assertThat(box.getLabel()).as("no label unless one is given").isNull();
    assertThat(box.isErrorShown()).as("the error treatment is off by default").isFalse();
    assertThat(box.getAccessibleLabel()).as("no accessible-name override by default").isNull();
  }

  @Test
  void labelConstructorSetsTheLabel() {
    assertThat(new ElwhaCheckbox("Remember me").getLabel())
        .as("the one-arg constructor is sugar for setLabel")
        .isEqualTo("Remember me");
  }

  // ------------------------------------------------------------------ setters

  @ParameterizedTest
  @EnumSource(CheckState.class)
  void everyStateRoundTripsThroughTheSetter(final CheckState state) {
    final ElwhaCheckbox box = new ElwhaCheckbox();
    box.setCheckState(state);

    assertThat(box.getCheckState()).as(state + " round-trips").isEqualTo(state);
  }

  @Test
  void aNullStateIsRejected() {
    assertThatThrownBy(() -> new ElwhaCheckbox().setCheckState(null))
        .as("there is no fourth 'no state' value — null is a caller bug")
        .isInstanceOf(NullPointerException.class);
  }

  @Test
  void indeterminateReadsAsNotChecked() {
    final ElwhaCheckbox box = new ElwhaCheckbox();
    box.setCheckState(CheckState.INDETERMINATE);

    assertThat(box.isChecked())
        .as("indeterminate is the 'neither' answer, matching the M3 implementations")
        .isFalse();
    assertThat(box.isIndeterminate()).as("indeterminate reads true on its own getter").isTrue();
  }

  @Test
  void setCheckedMapsOntoTheTwoBooleanStates() {
    final ElwhaCheckbox box = new ElwhaCheckbox();

    box.setChecked(true);
    assertThat(box.getCheckState())
        .as("setChecked(true) lands on CHECKED")
        .isEqualTo(CheckState.CHECKED);
    box.setChecked(false);
    assertThat(box.getCheckState())
        .as("setChecked(false) lands on UNCHECKED")
        .isEqualTo(CheckState.UNCHECKED);
  }

  @Test
  void setCheckedFalseAlsoClearsAnIndeterminateBox() {
    final ElwhaCheckbox box = new ElwhaCheckbox();
    box.setCheckState(CheckState.INDETERMINATE);

    box.setChecked(false);

    assertThat(box.getCheckState())
        .as("setChecked(false) is an absolute write, not a toggle away from checked")
        .isEqualTo(CheckState.UNCHECKED);
  }

  @Test
  void clearingIndeterminateLandsOnUnchecked() {
    final ElwhaCheckbox box = new ElwhaCheckbox();
    box.setIndeterminate(true);
    assertThat(box.getCheckState())
        .as("setIndeterminate(true) enters the third state")
        .isEqualTo(CheckState.INDETERMINATE);

    box.setIndeterminate(false);

    assertThat(box.getCheckState())
        .as("clearing indeterminate falls back to unchecked")
        .isEqualTo(CheckState.UNCHECKED);
  }

  @Test
  void clearingIndeterminateOnACheckedBoxLeavesItChecked() {
    final ElwhaCheckbox box = new ElwhaCheckbox();
    box.setChecked(true);

    box.setIndeterminate(false);

    assertThat(box.getCheckState())
        .as("setIndeterminate(false) only unwinds the indeterminate state, never the checked one")
        .isEqualTo(CheckState.CHECKED);
  }

  // ------------------------------------------------------------- cycle order

  @Test
  void userToggleCyclesUncheckedToCheckedAndBack() {
    final ElwhaCheckbox box = new ElwhaCheckbox();

    box.doClick();
    assertThat(box.getCheckState())
        .as("the first toggle checks the box")
        .isEqualTo(CheckState.CHECKED);
    box.doClick();
    assertThat(box.getCheckState())
        .as("the second toggle unchecks it")
        .isEqualTo(CheckState.UNCHECKED);
  }

  @Test
  void userToggleExitsIndeterminateToChecked() {
    final ElwhaCheckbox box = new ElwhaCheckbox();
    box.setIndeterminate(true);

    box.doClick();

    assertThat(box.getCheckState())
        .as("a user gesture leaves the indeterminate state through CHECKED, never back to it")
        .isEqualTo(CheckState.CHECKED);
    box.doClick();
    assertThat(box.getCheckState())
        .as("from there the plain two-state cycle resumes — indeterminate is programmatic-only")
        .isEqualTo(CheckState.UNCHECKED);
  }

  // -------------------------------------------------------- property changes

  @Test
  void everyStateChangeFiresTheCheckStatePropertyWithBothValues() {
    final ElwhaCheckbox box = new ElwhaCheckbox();
    final List<PropertyChangeEvent> events = new ArrayList<>();
    box.addPropertyChangeListener(ElwhaCheckbox.PROPERTY_CHECK_STATE, events::add);

    box.setCheckState(CheckState.CHECKED);

    assertThat(events).as("a programmatic write is still a state change").hasSize(1);
    assertThat(events.get(0).getOldValue())
        .as("the old value is carried")
        .isEqualTo(CheckState.UNCHECKED);
    assertThat(events.get(0).getNewValue())
        .as("the new value is carried")
        .isEqualTo(CheckState.CHECKED);
  }

  @Test
  void aUserToggleFiresThePropertyToo() {
    final ElwhaCheckbox box = new ElwhaCheckbox();
    final int[] changes = {0};
    box.addPropertyChangeListener(ElwhaCheckbox.PROPERTY_CHECK_STATE, e -> changes[0]++);

    box.doClick();

    assertThat(changes[0])
        .as("the property fires on every change, so a listener never misses a user gesture")
        .isEqualTo(1);
  }

  @Test
  void writingTheSameStateFiresNothing() {
    final ElwhaCheckbox box = new ElwhaCheckbox();
    box.setChecked(true);
    final int[] changes = {0};
    box.addPropertyChangeListener(ElwhaCheckbox.PROPERTY_CHECK_STATE, e -> changes[0]++);

    box.setChecked(true);
    box.setCheckState(CheckState.CHECKED);

    assertThat(changes[0]).as("a redundant write is not a change").isZero();
  }

  @Test
  void setIndeterminateFalseOnACheckedBoxFiresNothing() {
    final ElwhaCheckbox box = new ElwhaCheckbox();
    box.setChecked(true);
    final int[] changes = {0};
    box.addPropertyChangeListener(ElwhaCheckbox.PROPERTY_CHECK_STATE, e -> changes[0]++);

    box.setIndeterminate(false);

    assertThat(changes[0]).as("a no-op clear fires nothing").isZero();
  }

  // ---------------------------------------------------------------- label

  @Test
  void blankLabelsNormalizeToNoLabel() {
    final ElwhaCheckbox box = new ElwhaCheckbox("Terms");

    box.setLabel("   ");
    assertThat(box.getLabel()).as("a blank label is no label, not a whitespace label").isNull();

    box.setLabel("Terms");
    box.setLabel(null);
    assertThat(box.getLabel()).as("null clears the label").isNull();
  }

  @Test
  void errorFlagRoundTrips() {
    final ElwhaCheckbox box = new ElwhaCheckbox();

    box.setErrorShown(true);
    assertThat(box.isErrorShown()).as("the error flag round-trips").isTrue();
    box.setErrorShown(false);
    assertThat(box.isErrorShown()).as("and clears again").isFalse();
  }

  @Test
  void accessibleLabelOverrideRoundTrips() {
    final ElwhaCheckbox box = new ElwhaCheckbox();

    box.setAccessibleLabel("Accept the terms");

    assertThat(box.getAccessibleLabel())
        .as("the accessible-name override is stored independently of the visual label")
        .isEqualTo("Accept the terms");
    assertThat(box.getLabel()).as("and does not create a visual label").isNull();
  }
}
