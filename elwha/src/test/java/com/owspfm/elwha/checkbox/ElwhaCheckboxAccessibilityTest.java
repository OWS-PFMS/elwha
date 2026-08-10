package com.owspfm.elwha.checkbox;

import static org.assertj.core.api.Assertions.assertThat;

import com.owspfm.elwha.checkbox.ElwhaCheckbox.CheckState;
import com.owspfm.elwha.testkit.EdtInterceptor;
import com.owspfm.elwha.testkit.ThemeExtension;
import java.beans.PropertyChangeEvent;
import java.util.ArrayList;
import java.util.List;
import javax.accessibility.AccessibleContext;
import javax.accessibility.AccessibleRole;
import javax.accessibility.AccessibleState;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

/**
 * Tier A coverage of the accessible shape design doc §9 pins: the native {@code CHECK_BOX} role,
 * the two standard tri-state flags, the three-way accessible-name precedence a label-less box
 * depends on, and the error description. Assistive technology reads all of this before it reads
 * anything painted, so the shape is contract rather than polish.
 */
@ExtendWith({EdtInterceptor.class, ThemeExtension.class})
class ElwhaCheckboxAccessibilityTest {

  // ------------------------------------------------------------------- role

  @Test
  void roleIsTheNativeCheckBox() {
    assertThat(new ElwhaCheckbox().getAccessibleContext().getAccessibleRole())
        .as("Swing has a real checkbox role, so no compromise mapping is needed")
        .isEqualTo(AccessibleRole.CHECK_BOX);
  }

  @Test
  void accessibleContextIsCreatedOnceAndReused() {
    final ElwhaCheckbox box = new ElwhaCheckbox();

    assertThat(box.getAccessibleContext())
        .as("repeat calls hand back the same context, so listeners registered on it survive")
        .isSameAs(box.getAccessibleContext());
  }

  // ------------------------------------------------------------------ states

  @Test
  void uncheckedCarriesNeitherStateFlag() {
    final AccessibleContext context = new ElwhaCheckbox().getAccessibleContext();

    assertThat(context.getAccessibleStateSet().contains(AccessibleState.CHECKED))
        .as("an unchecked box is not CHECKED")
        .isFalse();
    assertThat(context.getAccessibleStateSet().contains(AccessibleState.INDETERMINATE))
        .as("nor INDETERMINATE")
        .isFalse();
  }

  @Test
  void checkedCarriesCheckedOnly() {
    final ElwhaCheckbox box = new ElwhaCheckbox();
    box.setChecked(true);
    final AccessibleContext context = box.getAccessibleContext();

    assertThat(context.getAccessibleStateSet().contains(AccessibleState.CHECKED))
        .as("a checked box reports CHECKED")
        .isTrue();
    assertThat(context.getAccessibleStateSet().contains(AccessibleState.INDETERMINATE))
        .as("and the two flags are mutually exclusive")
        .isFalse();
  }

  @Test
  void indeterminateCarriesIndeterminateOnly() {
    final ElwhaCheckbox box = new ElwhaCheckbox();
    box.setIndeterminate(true);
    final AccessibleContext context = box.getAccessibleContext();

    assertThat(context.getAccessibleStateSet().contains(AccessibleState.INDETERMINATE))
        .as("a partially-checked box reports INDETERMINATE")
        .isTrue();
    assertThat(context.getAccessibleStateSet().contains(AccessibleState.CHECKED))
        .as("and never both at once")
        .isFalse();
  }

  @Test
  void stateSetTracksLiveChanges() {
    final ElwhaCheckbox box = new ElwhaCheckbox();
    final AccessibleContext context = box.getAccessibleContext();

    box.setChecked(true);
    assertThat(context.getAccessibleStateSet().contains(AccessibleState.CHECKED))
        .as("the state set is computed on read, not snapshotted at construction")
        .isTrue();

    box.setChecked(false);
    assertThat(context.getAccessibleStateSet().contains(AccessibleState.CHECKED))
        .as("and follows the box back down")
        .isFalse();
  }

  // -------------------------------------------------------- state-change events

  @Test
  void enteringAndLeavingCheckedFiresTheStateProperty() {
    final ElwhaCheckbox box = new ElwhaCheckbox();
    final AccessibleContext context = box.getAccessibleContext();
    final List<PropertyChangeEvent> events = new ArrayList<>();
    context.addPropertyChangeListener(
        event -> {
          if (AccessibleContext.ACCESSIBLE_STATE_PROPERTY.equals(event.getPropertyName())) {
            events.add(event);
          }
        });

    box.setChecked(true);
    assertThat(events).as("entering checked announces the flag once").hasSize(1);
    assertThat(events.get(0).getNewValue())
        .as("as an arriving CHECKED state")
        .isEqualTo(AccessibleState.CHECKED);

    box.setChecked(false);
    assertThat(events).as("leaving it announces once more").hasSize(2);
    assertThat(events.get(1).getOldValue())
        .as("as a departing CHECKED state")
        .isEqualTo(AccessibleState.CHECKED);
  }

  @Test
  void enteringIndeterminateFiresItsOwnStateProperty() {
    final ElwhaCheckbox box = new ElwhaCheckbox();
    final AccessibleContext context = box.getAccessibleContext();
    final List<Object> arrivals = new ArrayList<>();
    context.addPropertyChangeListener(
        event -> {
          if (AccessibleContext.ACCESSIBLE_STATE_PROPERTY.equals(event.getPropertyName())
              && event.getNewValue() != null) {
            arrivals.add(event.getNewValue());
          }
        });

    box.setIndeterminate(true);

    assertThat(arrivals)
        .as("the indeterminate flag is announced under its own state, not folded into CHECKED")
        .containsExactly(AccessibleState.INDETERMINATE);
  }

  @Test
  void movingFromIndeterminateToCheckedAnnouncesBothFlags() {
    final ElwhaCheckbox box = new ElwhaCheckbox();
    box.setIndeterminate(true);
    final AccessibleContext context = box.getAccessibleContext();
    final List<PropertyChangeEvent> events = new ArrayList<>();
    context.addPropertyChangeListener(
        event -> {
          if (AccessibleContext.ACCESSIBLE_STATE_PROPERTY.equals(event.getPropertyName())) {
            events.add(event);
          }
        });

    box.setCheckState(CheckState.CHECKED);

    assertThat(events)
        .as("one transition crosses two flags, so both the arrival and the departure fire")
        .hasSize(2);
  }

  // ------------------------------------------------------------------- name

  @Test
  void nameComesFromTheVisualLabel() {
    assertThat(new ElwhaCheckbox("Remember me").getAccessibleContext().getAccessibleName())
        .as("a labelled checkbox names itself from the text the user can see")
        .isEqualTo("Remember me");
  }

  @Test
  void aLabellessCheckboxHasNoNameUntilOneIsGiven() {
    assertThat(new ElwhaCheckbox().getAccessibleContext().getAccessibleName())
        .as("nothing invents a name — a label-less box needs setAccessibleLabel")
        .isNull();
  }

  @Test
  void accessibleLabelNamesALabellessCheckbox() {
    final ElwhaCheckbox box = new ElwhaCheckbox();
    box.setAccessibleLabel("Accept terms");

    assertThat(box.getAccessibleContext().getAccessibleName())
        .as("the aria-label analogue supplies the name when there is no visible text")
        .isEqualTo("Accept terms");
  }

  @Test
  void accessibleLabelOverridesTheVisualLabel() {
    final ElwhaCheckbox box = new ElwhaCheckbox("Terms");
    box.setAccessibleLabel("Accept the terms of service");

    assertThat(box.getAccessibleContext().getAccessibleName())
        .as("a spelled-out accessible name beats the abbreviated visible one")
        .isEqualTo("Accept the terms of service");
  }

  @Test
  void anExplicitAccessibleNameOutranksBoth() {
    final ElwhaCheckbox box = new ElwhaCheckbox("Terms");
    box.setAccessibleLabel("Accept terms");
    box.getAccessibleContext().setAccessibleName("Explicit");

    assertThat(box.getAccessibleContext().getAccessibleName())
        .as("a name written straight onto the context is the last word")
        .isEqualTo("Explicit");
  }

  @Test
  void clearingTheAccessibleLabelFallsBackToTheVisualLabel() {
    final ElwhaCheckbox box = new ElwhaCheckbox("Terms");
    box.setAccessibleLabel("Accept terms");
    box.setAccessibleLabel(null);

    assertThat(box.getAccessibleContext().getAccessibleName())
        .as("null restores the fallback rather than blanking the name")
        .isEqualTo("Terms");
  }

  // ------------------------------------------------------------ description

  @Test
  void errorFlagSurfacesInTheDescription() {
    final ElwhaCheckbox box = new ElwhaCheckbox("Terms");
    assertThat(box.getAccessibleContext().getAccessibleDescription())
        .as("no description while the box is fine")
        .isNull();

    box.setErrorShown(true);

    assertThat(box.getAccessibleContext().getAccessibleDescription())
        .as("the error treatment is announced, not left as a colour-only signal")
        .isEqualTo("Error");
  }

  @Test
  void anExplicitDescriptionOutranksTheErrorText() {
    final ElwhaCheckbox box = new ElwhaCheckbox("Terms");
    box.setErrorShown(true);
    box.getAccessibleContext().setAccessibleDescription("You must accept to continue");

    assertThat(box.getAccessibleContext().getAccessibleDescription())
        .as("a caller-written description replaces the generic error string")
        .isEqualTo("You must accept to continue");
  }

  @Test
  void clearingTheErrorFlagClearsTheDescription() {
    final ElwhaCheckbox box = new ElwhaCheckbox("Terms");
    box.setErrorShown(true);
    box.setErrorShown(false);

    assertThat(box.getAccessibleContext().getAccessibleDescription())
        .as("the description follows the flag rather than latching")
        .isNull();
  }
}
