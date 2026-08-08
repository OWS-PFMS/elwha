package com.owspfm.elwha.switches;

import static org.assertj.core.api.Assertions.assertThat;

import com.owspfm.elwha.testkit.EdtInterceptor;
import com.owspfm.elwha.testkit.ThemeExtension;
import java.beans.PropertyChangeEvent;
import java.util.ArrayList;
import java.util.List;
import javax.accessibility.AccessibleContext;
import javax.accessibility.AccessibleState;
import javax.swing.JLabel;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

/**
 * Tier A coverage of the naming rules and the accessible surface the {@code
 * ElwhaSwitchInteractionTest} spike does not reach: the two ways a switch can be named and which
 * one wins, the value channel as distinct from the action channel, and the state-change
 * announcements.
 *
 * <p>A switch never labels itself — research §A requires an adjacent label — so the name precedence
 * is load-bearing rather than cosmetic: get it wrong and a screen reader announces an unnamed
 * toggle.
 */
@ExtendWith({EdtInterceptor.class, ThemeExtension.class})
class ElwhaSwitchLabelAccessibilityTest {

  private static AccessibleContext contextOf(final ElwhaSwitch toggle) {
    return toggle.getAccessibleContext();
  }

  /** Associates a {@link JLabel} with the switch the way a form would. */
  private static void labelFor(final ElwhaSwitch toggle, final String text) {
    new JLabel(text).setLabelFor(toggle);
  }

  // ------------------------------------------------------------------ label

  @Test
  void labelRoundTripsAndClears() {
    final ElwhaSwitch toggle = new ElwhaSwitch();
    assertThat(toggle.getAccessibleLabel()).as("a new switch carries no label").isNull();

    toggle.setAccessibleLabel("Wi-Fi");
    assertThat(toggle.getAccessibleLabel()).as("the label round-trips").isEqualTo("Wi-Fi");

    toggle.setAccessibleLabel(null);
    assertThat(toggle.getAccessibleLabel()).as("and null clears it").isNull();
  }

  @Test
  void anUnnamedSwitchReportsNoName() {
    assertThat(contextOf(new ElwhaSwitch()).getAccessibleName())
        .as(
            "nothing invents a name — an unlabelled switch is a caller bug the API cannot paper"
                + " over")
        .isNull();
  }

  @Test
  void anAssociatedLabelSuppliesTheName() {
    final ElwhaSwitch toggle = new ElwhaSwitch();

    labelFor(toggle, "Wi-Fi");

    assertThat(contextOf(toggle).getAccessibleName())
        .as("a JLabel.setLabelFor association names the switch with no extra call")
        .isEqualTo("Wi-Fi");
  }

  @Test
  void anExplicitLabelOutranksAnAssociatedOne() {
    final ElwhaSwitch toggle = new ElwhaSwitch();
    labelFor(toggle, "Wi-Fi");

    toggle.setAccessibleLabel("Wireless network");

    assertThat(contextOf(toggle).getAccessibleName())
        .as("setLabel takes precedence over the labelFor fallback")
        .isEqualTo("Wireless network");
  }

  @Test
  void anEmptyLabelFallsBackRatherThanBlankingTheName() {
    final ElwhaSwitch toggle = new ElwhaSwitch();
    labelFor(toggle, "Wi-Fi");
    toggle.setAccessibleLabel("Wireless network");

    toggle.setAccessibleLabel("");

    assertThat(contextOf(toggle).getAccessibleName())
        .as("an empty label counts as absent, so the association takes over again")
        .isEqualTo("Wi-Fi");
  }

  @Test
  void clearingTheLabelFallsBackToTheAssociation() {
    final ElwhaSwitch toggle = new ElwhaSwitch();
    labelFor(toggle, "Wi-Fi");
    toggle.setAccessibleLabel("Wireless network");

    toggle.setAccessibleLabel(null);

    assertThat(contextOf(toggle).getAccessibleName())
        .as("null falls back the same way empty does")
        .isEqualTo("Wi-Fi");
  }

  // ----------------------------------------------------------------- action

  @Test
  void oneClickActionIsExposed() {
    final AccessibleContext context = contextOf(new ElwhaSwitch());

    assertThat(context.getAccessibleAction().getAccessibleActionCount())
        .as("a switch does exactly one thing")
        .isEqualTo(1);
    assertThat(context.getAccessibleAction().getAccessibleActionDescription(0))
        .as("and describes it as a click")
        .isEqualTo("click");
    assertThat(context.getAccessibleAction().getAccessibleActionDescription(1))
        .as("with nothing behind it")
        .isNull();
  }

  @Test
  void anOutOfRangeActionIndexIsRefused() {
    assertThat(contextOf(new ElwhaSwitch()).getAccessibleAction().doAccessibleAction(1))
        .as("there is no second action to perform")
        .isFalse();
  }

  @Test
  void accessibleActionFiresBothListenerKinds() {
    final ElwhaSwitch toggle = new ElwhaSwitch();
    final int[] actions = {0};
    final int[] changes = {0};
    toggle.addActionListener(e -> actions[0]++);
    toggle.addPropertyChangeListener(ElwhaSwitch.PROPERTY_SELECTED, e -> changes[0]++);

    contextOf(toggle).getAccessibleAction().doAccessibleAction(0);

    assertThat(actions[0])
        .as("assistive technology acts as the user, so the action listeners fire")
        .isEqualTo(1);
    assertThat(changes[0]).as("along with the selection property").isEqualTo(1);
  }

  @Test
  void accessibleActionTogglesInBothDirections() {
    final ElwhaSwitch toggle = new ElwhaSwitch(true);

    contextOf(toggle).getAccessibleAction().doAccessibleAction(0);

    assertThat(toggle.isSelected())
        .as("unlike a radio, the switch's action is a toggle and can turn it off")
        .isFalse();
  }

  // ------------------------------------------------------------------ value

  @Test
  void valueRangeIsZeroToOne() {
    final AccessibleContext context = contextOf(new ElwhaSwitch());

    assertThat(context.getAccessibleValue().getMinimumAccessibleValue())
        .as("a binary control has a floor of zero")
        .isEqualTo(0);
    assertThat(context.getAccessibleValue().getMaximumAccessibleValue())
        .as("and a ceiling of one")
        .isEqualTo(1);
  }

  @Test
  void writingTheValueIsProgrammatic() {
    final ElwhaSwitch toggle = new ElwhaSwitch();
    final int[] actions = {0};
    final int[] changes = {0};
    toggle.addActionListener(e -> actions[0]++);
    toggle.addPropertyChangeListener(ElwhaSwitch.PROPERTY_SELECTED, e -> changes[0]++);

    final boolean accepted = contextOf(toggle).getAccessibleValue().setCurrentAccessibleValue(1);

    assertThat(accepted).as("the write is accepted").isTrue();
    assertThat(toggle.isSelected()).as("and lands").isTrue();
    assertThat(changes[0]).as("a value write is a state change").isEqualTo(1);
    assertThat(actions[0])
        .as("but not a user gesture — the value channel routes to setSelected, not the toggle")
        .isZero();
  }

  @Test
  void writingZeroDeselects() {
    final ElwhaSwitch toggle = new ElwhaSwitch(true);

    contextOf(toggle).getAccessibleValue().setCurrentAccessibleValue(0);

    assertThat(toggle.isSelected()).as("zero maps onto the programmatic deselect").isFalse();
  }

  @Test
  void writingANullValueIsRefused() {
    final ElwhaSwitch toggle = new ElwhaSwitch();

    assertThat(contextOf(toggle).getAccessibleValue().setCurrentAccessibleValue(null))
        .as("null is not a number this control can store")
        .isFalse();
    assertThat(toggle.isSelected()).as("and changes nothing").isFalse();
  }

  // ----------------------------------------------------------- state events

  @Test
  void selectionChangesFireTheStateProperty() {
    final ElwhaSwitch toggle = new ElwhaSwitch();
    final AccessibleContext context = contextOf(toggle);
    final List<PropertyChangeEvent> events = new ArrayList<>();
    context.addPropertyChangeListener(
        event -> {
          if (AccessibleContext.ACCESSIBLE_STATE_PROPERTY.equals(event.getPropertyName())) {
            events.add(event);
          }
        });

    toggle.setSelected(true);
    assertThat(events).as("selecting announces the flag").hasSize(1);
    assertThat(events.get(0).getNewValue())
        .as("as an arriving CHECKED state")
        .isEqualTo(AccessibleState.CHECKED);

    toggle.setSelected(false);
    assertThat(events.get(1).getOldValue())
        .as("and deselecting announces it departing")
        .isEqualTo(AccessibleState.CHECKED);
  }

  @Test
  void aRedundantWriteAnnouncesNothing() {
    final ElwhaSwitch toggle = new ElwhaSwitch(true);
    final AccessibleContext context = contextOf(toggle);
    final int[] announcements = {0};
    context.addPropertyChangeListener(
        event -> {
          if (AccessibleContext.ACCESSIBLE_STATE_PROPERTY.equals(event.getPropertyName())) {
            announcements[0]++;
          }
        });

    toggle.setSelected(true);

    assertThat(announcements[0]).as("nothing changed, so there is nothing to announce").isZero();
  }

  @Test
  void accessibleContextIsCreatedOnceAndReused() {
    final ElwhaSwitch toggle = new ElwhaSwitch();

    assertThat(toggle.getAccessibleContext())
        .as("repeat calls hand back the same context, so listeners registered on it survive")
        .isSameAs(toggle.getAccessibleContext());
  }
}
