package com.owspfm.elwha.radio;

import static org.assertj.core.api.Assertions.assertThat;

import com.owspfm.elwha.testkit.EdtInterceptor;
import com.owspfm.elwha.testkit.ThemeExtension;
import java.beans.PropertyChangeEvent;
import java.util.ArrayList;
import java.util.List;
import javax.accessibility.AccessibleContext;
import javax.accessibility.AccessibleRelation;
import javax.accessibility.AccessibleRole;
import javax.accessibility.AccessibleState;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

/**
 * Tier A coverage of the accessible shape: Swing's native radio vocabulary, the action-versus-value
 * split (an assistive-technology click is a user gesture and fires {@code ActionListener}s; a value
 * write is programmatic and does not), and the {@code MEMBER_OF} relation.
 *
 * <p>The relation carries a regression guard. {@code AccessibleJComponent} hands back a
 * <em>persistent</em> relation set, and {@code AccessibleRelationSet.add} merges targets into an
 * existing same-key relation rather than replacing them — so recomputing membership through {@code
 * add} accumulates stale targets that never go away. The tests below re-read the relation after
 * membership changes and assert the target list matches the group exactly.
 */
@ExtendWith({EdtInterceptor.class, ThemeExtension.class})
class ElwhaRadioButtonAccessibilityTest {

  private static AccessibleContext contextOf(final ElwhaRadioButton radio) {
    return radio.getAccessibleContext();
  }

  // ------------------------------------------------------------------- role

  @Test
  void roleIsTheNativeRadioButton() {
    assertThat(contextOf(new ElwhaRadioButton()).getAccessibleRole())
        .as("Swing has a real radio role, so no compromise mapping is needed")
        .isEqualTo(AccessibleRole.RADIO_BUTTON);
  }

  @Test
  void accessibleContextIsCreatedOnceAndReused() {
    final ElwhaRadioButton radio = new ElwhaRadioButton();

    assertThat(radio.getAccessibleContext())
        .as("repeat calls hand back the same context, so listeners registered on it survive")
        .isSameAs(radio.getAccessibleContext());
  }

  // ------------------------------------------------------------------ states

  @Test
  void checkedTracksTheSelection() {
    final ElwhaRadioButton radio = new ElwhaRadioButton();
    final AccessibleContext context = contextOf(radio);

    assertThat(context.getAccessibleStateSet().contains(AccessibleState.CHECKED))
        .as("an unselected radio is not CHECKED")
        .isFalse();

    radio.setSelected(true);
    assertThat(context.getAccessibleStateSet().contains(AccessibleState.CHECKED))
        .as("a selected one is")
        .isTrue();

    radio.setSelected(false);
    assertThat(context.getAccessibleStateSet().contains(AccessibleState.CHECKED))
        .as("and the flag follows it back down")
        .isFalse();
  }

  @Test
  void selectionChangesFireTheStateProperty() {
    final ElwhaRadioButton radio = new ElwhaRadioButton();
    final AccessibleContext context = contextOf(radio);
    final List<PropertyChangeEvent> events = new ArrayList<>();
    context.addPropertyChangeListener(
        event -> {
          if (AccessibleContext.ACCESSIBLE_STATE_PROPERTY.equals(event.getPropertyName())) {
            events.add(event);
          }
        });

    radio.setSelected(true);
    assertThat(events).as("selecting announces the flag").hasSize(1);
    assertThat(events.get(0).getNewValue())
        .as("as an arriving CHECKED state")
        .isEqualTo(AccessibleState.CHECKED);

    radio.setSelected(false);
    assertThat(events.get(1).getOldValue())
        .as("and deselecting announces it departing")
        .isEqualTo(AccessibleState.CHECKED);
  }

  // ----------------------------------------------------------------- action

  @Test
  void oneClickActionIsExposed() {
    final AccessibleContext context = contextOf(new ElwhaRadioButton());

    assertThat(context.getAccessibleAction().getAccessibleActionCount())
        .as("a radio does exactly one thing")
        .isEqualTo(1);
    assertThat(context.getAccessibleAction().getAccessibleActionDescription(0))
        .as("and describes it as a click")
        .isEqualTo("click");
    assertThat(context.getAccessibleAction().getAccessibleActionDescription(1))
        .as("with nothing behind it")
        .isNull();
  }

  @Test
  void accessibleActionCountsAsAUserGesture() {
    final ElwhaRadioButton radio = new ElwhaRadioButton();
    final int[] actions = {0};
    final int[] changes = {0};
    radio.addActionListener(e -> actions[0]++);
    radio.addChangeListener(e -> changes[0]++);

    final boolean performed = contextOf(radio).getAccessibleAction().doAccessibleAction(0);

    assertThat(performed).as("the action reports that it ran").isTrue();
    assertThat(radio.isSelected()).as("and selects the radio").isTrue();
    assertThat(actions[0])
        .as("assistive technology acts as the user, so the action listeners fire")
        .isEqualTo(1);
    assertThat(changes[0]).as("along with the change listeners").isEqualTo(1);
  }

  @Test
  void anOutOfRangeActionIndexIsRefused() {
    assertThat(contextOf(new ElwhaRadioButton()).getAccessibleAction().doAccessibleAction(1))
        .as("there is no second action to perform")
        .isFalse();
  }

  @Test
  void accessibleActionIsRefusedWhileDisabled() {
    final ElwhaRadioButton radio = new ElwhaRadioButton();
    radio.setEnabled(false);

    assertThat(contextOf(radio).getAccessibleAction().doAccessibleAction(0))
        .as("a disabled radio refuses the action rather than silently ignoring it")
        .isFalse();
    assertThat(radio.isSelected()).as("and stays unselected").isFalse();
  }

  // ------------------------------------------------------------------ value

  @Test
  void valueIsTheZeroOrOneSelection() {
    final ElwhaRadioButton radio = new ElwhaRadioButton();
    final AccessibleContext context = contextOf(radio);

    assertThat(context.getAccessibleValue().getCurrentAccessibleValue())
        .as("unselected reads zero")
        .isEqualTo(0);
    radio.setSelected(true);
    assertThat(context.getAccessibleValue().getCurrentAccessibleValue())
        .as("selected reads one")
        .isEqualTo(1);
    assertThat(context.getAccessibleValue().getMinimumAccessibleValue())
        .as("with a floor of zero")
        .isEqualTo(0);
    assertThat(context.getAccessibleValue().getMaximumAccessibleValue())
        .as("and a ceiling of one")
        .isEqualTo(1);
  }

  @Test
  void writingTheValueIsProgrammatic() {
    final ElwhaRadioButton radio = new ElwhaRadioButton();
    final int[] actions = {0};
    final int[] changes = {0};
    radio.addActionListener(e -> actions[0]++);
    radio.addChangeListener(e -> changes[0]++);

    final boolean accepted = contextOf(radio).getAccessibleValue().setCurrentAccessibleValue(1);

    assertThat(accepted).as("the write is accepted").isTrue();
    assertThat(radio.isSelected()).as("and lands").isTrue();
    assertThat(changes[0]).as("a value write is a state change").isEqualTo(1);
    assertThat(actions[0])
        .as("but not a user gesture, so no action event — the setSelected route, not doClick")
        .isZero();
  }

  @Test
  void writingZeroDeselects() {
    final ElwhaRadioButton radio = new ElwhaRadioButton(true);

    contextOf(radio).getAccessibleValue().setCurrentAccessibleValue(0);

    assertThat(radio.isSelected()).as("zero maps onto the programmatic deselect").isFalse();
  }

  @Test
  void writingANullValueIsRefused() {
    final ElwhaRadioButton radio = new ElwhaRadioButton();

    assertThat(contextOf(radio).getAccessibleValue().setCurrentAccessibleValue(null))
        .as("null is not a number this control can store")
        .isFalse();
    assertThat(radio.isSelected()).as("and changes nothing").isFalse();
  }

  // ------------------------------------------------------------------- name

  @Test
  void nameComesFromTheVisualLabel() {
    assertThat(contextOf(new ElwhaRadioButton("Weekly")).getAccessibleName())
        .as("a labelled radio names itself from the text the user can see")
        .isEqualTo("Weekly");
  }

  @Test
  void accessibleLabelNamesALabellessRadio() {
    final ElwhaRadioButton radio = new ElwhaRadioButton();
    radio.setAccessibleLabel("Weekly digest");

    assertThat(contextOf(radio).getAccessibleName())
        .as("research §A: a radio always needs a name, and this is the aria-label analogue")
        .isEqualTo("Weekly digest");
  }

  @Test
  void accessibleLabelOverridesTheVisualLabel() {
    final ElwhaRadioButton radio = new ElwhaRadioButton("Wk");
    radio.setAccessibleLabel("Weekly digest");

    assertThat(contextOf(radio).getAccessibleName())
        .as("a spelled-out accessible name beats the abbreviated visible one")
        .isEqualTo("Weekly digest");
  }

  @Test
  void anExplicitAccessibleNameOutranksBoth() {
    final ElwhaRadioButton radio = new ElwhaRadioButton("Wk");
    radio.setAccessibleLabel("Weekly digest");
    contextOf(radio).setAccessibleName("Explicit");

    assertThat(contextOf(radio).getAccessibleName())
        .as("a name written straight onto the context is the last word")
        .isEqualTo("Explicit");
  }

  @Test
  void aLabellessRadioHasNoNameUntilOneIsGiven() {
    assertThat(contextOf(new ElwhaRadioButton()).getAccessibleName())
        .as("nothing invents a name")
        .isNull();
  }

  // -------------------------------------------------------- MEMBER_OF relation

  @Test
  void anUngroupedRadioCarriesNoMembership() {
    assertThat(
            contextOf(new ElwhaRadioButton())
                .getAccessibleRelationSet()
                .get(AccessibleRelation.MEMBER_OF))
        .as("with no group there is no membership to report")
        .isNull();
  }

  @Test
  void groupMembershipIsReportedAsMemberOf() {
    final ElwhaRadioButton first = new ElwhaRadioButton("Daily");
    final ElwhaRadioButton second = new ElwhaRadioButton("Weekly");
    final ElwhaRadioGroup group = new ElwhaRadioGroup();
    group.add(first);
    group.add(second);

    final AccessibleRelation relation =
        contextOf(first).getAccessibleRelationSet().get(AccessibleRelation.MEMBER_OF);

    assertThat(relation).as("a grouped radio reports its membership").isNotNull();
    assertThat(relation.getTarget())
        .as("and the targets are the group's members")
        .containsExactly(first, second);
  }

  @Test
  void reReadingAfterAJoinReplacesTheTargetsRatherThanMergingThem() {
    final ElwhaRadioButton first = new ElwhaRadioButton();
    final ElwhaRadioButton second = new ElwhaRadioButton();
    final ElwhaRadioGroup group = new ElwhaRadioGroup();
    group.add(first);
    group.add(second);
    contextOf(first).getAccessibleRelationSet();

    group.add(new ElwhaRadioButton());

    assertThat(
            contextOf(first)
                .getAccessibleRelationSet()
                .get(AccessibleRelation.MEMBER_OF)
                .getTarget())
        .as(
            "a third member makes three targets — AccessibleRelationSet.add would have merged to"
                + " five")
        .hasSize(3);
  }

  @Test
  void reReadingAfterALeaveShrinksTheTargets() {
    final ElwhaRadioButton first = new ElwhaRadioButton();
    final ElwhaRadioButton second = new ElwhaRadioButton();
    final ElwhaRadioButton third = new ElwhaRadioButton();
    final ElwhaRadioGroup group = new ElwhaRadioGroup();
    group.add(first);
    group.add(second);
    group.add(third);
    contextOf(first).getAccessibleRelationSet();

    group.remove(third);

    assertThat(
            contextOf(first)
                .getAccessibleRelationSet()
                .get(AccessibleRelation.MEMBER_OF)
                .getTarget())
        .as("a departed member is really gone from the relation, not just stale")
        .containsExactly(first, second);
  }

  @Test
  void leavingTheGroupRemovesTheRelationEntirely() {
    final ElwhaRadioButton radio = new ElwhaRadioButton();
    final ElwhaRadioGroup group = new ElwhaRadioGroup();
    group.add(radio);
    contextOf(radio).getAccessibleRelationSet();

    group.remove(radio);

    assertThat(contextOf(radio).getAccessibleRelationSet().get(AccessibleRelation.MEMBER_OF))
        .as("an ungrouped radio reports no membership even after having had one")
        .isNull();
  }

  @Test
  void movingToAnotherGroupReportsTheNewMembership() {
    final ElwhaRadioButton radio = new ElwhaRadioButton();
    final ElwhaRadioButton stayer = new ElwhaRadioButton();
    final ElwhaRadioButton newSibling = new ElwhaRadioButton();
    final ElwhaRadioGroup first = new ElwhaRadioGroup();
    final ElwhaRadioGroup second = new ElwhaRadioGroup();
    first.add(radio);
    first.add(stayer);
    second.add(newSibling);
    contextOf(radio).getAccessibleRelationSet();

    second.add(radio);

    assertThat(
            contextOf(radio)
                .getAccessibleRelationSet()
                .get(AccessibleRelation.MEMBER_OF)
                .getTarget())
        .as("the relation answers the current group, with no trace of the old one")
        .containsExactly(newSibling, radio);
  }
}
