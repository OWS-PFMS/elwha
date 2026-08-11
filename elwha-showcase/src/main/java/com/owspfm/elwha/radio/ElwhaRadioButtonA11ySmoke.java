package com.owspfm.elwha.radio;

import com.owspfm.elwha.theme.ElwhaTheme;
import com.owspfm.elwha.theme.MaterialPalettes;
import com.owspfm.elwha.theme.Mode;
import javax.accessibility.AccessibleContext;
import javax.accessibility.AccessibleRelation;
import javax.accessibility.AccessibleRole;
import javax.accessibility.AccessibleState;
import javax.swing.JLabel;

/**
 * Headless guard for the S6 {@link ElwhaRadioButton} accessible surface (story #422): the native
 * {@link AccessibleRole#RADIO_BUTTON} role, CHECKED state + state-change notification, the "click"
 * action's user-gesture semantics (fires {@code ActionListener}, refuses disabled, no-ops on the
 * already-selected), the 0/1 {@code AccessibleValue} routing group exclusion programmatically,
 * naming via {@code setLabel} and the {@code JLabel.setLabelFor} fallback, and the live MEMBER_OF
 * relation.
 *
 * @author Charles Bryan
 * @version v0.4.0
 * @since v0.4.0
 */
public final class ElwhaRadioButtonA11ySmoke {

  private ElwhaRadioButtonA11ySmoke() {}

  /**
   * Runs the guard. Exits non-zero on any failed assertion.
   *
   * @param args unused
   * @version v0.4.0
   * @since v0.4.0
   */
  public static void main(final String[] args) {
    System.setProperty("java.awt.headless", "true");
    ElwhaTheme.install(
        ElwhaTheme.config().theme(MaterialPalettes.baseline()).mode(Mode.LIGHT).build());

    checkRoleAndState();
    checkAction();
    checkValue();
    checkNaming();
    checkMemberOf();

    System.out.println(
        "ElwhaRadioButtonA11ySmoke: OK (role, state, action, value, name, relation)");
  }

  private static void checkRoleAndState() {
    final ElwhaRadioButton radio = new ElwhaRadioButton();
    final AccessibleContext ac = radio.getAccessibleContext();
    check("role is RADIO_BUTTON", ac.getAccessibleRole() == AccessibleRole.RADIO_BUTTON);
    check(
        "unselected has no CHECKED state",
        !ac.getAccessibleStateSet().contains(AccessibleState.CHECKED));

    final int[] stateEvents = {0};
    ac.addPropertyChangeListener(
        e -> {
          if (AccessibleContext.ACCESSIBLE_STATE_PROPERTY.equals(e.getPropertyName())) {
            stateEvents[0]++;
          }
        });
    radio.setSelected(true);
    check("selected gains CHECKED", ac.getAccessibleStateSet().contains(AccessibleState.CHECKED));
    check("state change notifies assistive tech", stateEvents[0] == 1);
    radio.setSelected(false);
    check("deselect drops CHECKED + notifies", stateEvents[0] == 2);
  }

  private static void checkAction() {
    final ElwhaRadioButton radio = new ElwhaRadioButton();
    final AccessibleContext ac = radio.getAccessibleContext();
    final int[] actions = {0};
    radio.addActionListener(e -> actions[0]++);

    check("one accessible action", ac.getAccessibleAction().getAccessibleActionCount() == 1);
    check(
        "action description is 'click'",
        "click".equals(ac.getAccessibleAction().getAccessibleActionDescription(0)));
    check("doAccessibleAction selects", ac.getAccessibleAction().doAccessibleAction(0));
    check(
        "assistive tech acts as the user (ActionListener fired)",
        radio.isSelected() && actions[0] == 1);
    check("re-doing the action reports done", ac.getAccessibleAction().doAccessibleAction(0));
    check("but the already-selected radio fires nothing", actions[0] == 1);
    check("out-of-range action refused", !ac.getAccessibleAction().doAccessibleAction(1));

    radio.setEnabled(false);
    radio.setSelected(false);
    check("disabled action refused", !ac.getAccessibleAction().doAccessibleAction(0));
    check("disabled action mutates nothing", !radio.isSelected());
  }

  private static void checkValue() {
    final ElwhaRadioGroup group = new ElwhaRadioGroup();
    final ElwhaRadioButton a = new ElwhaRadioButton(true);
    final ElwhaRadioButton b = new ElwhaRadioButton();
    group.add(a);
    group.add(b);
    final int[] actions = {0};
    b.addActionListener(e -> actions[0]++);

    final AccessibleContext ac = b.getAccessibleContext();
    check(
        "value range is 0..1",
        ac.getAccessibleValue().getMinimumAccessibleValue().intValue() == 0
            && ac.getAccessibleValue().getMaximumAccessibleValue().intValue() == 1);
    check(
        "unselected value is 0",
        ac.getAccessibleValue().getCurrentAccessibleValue().intValue() == 0);
    check("set value 1 accepted", ac.getAccessibleValue().setCurrentAccessibleValue(1));
    check(
        "value write selects and routes exclusion",
        b.isSelected() && !a.isSelected() && group.getSelected() == b);
    check("value write is programmatic (no ActionListener)", actions[0] == 0);
    check(
        "selected value is 1", ac.getAccessibleValue().getCurrentAccessibleValue().intValue() == 1);
    check("null value refused", !ac.getAccessibleValue().setCurrentAccessibleValue(null));
  }

  private static void checkNaming() {
    final ElwhaRadioButton named = new ElwhaRadioButton();
    named.setLabel("Compact view");
    check(
        "setLabel drives the accessible name",
        "Compact view".equals(named.getAccessibleContext().getAccessibleName()));
    check("getLabel round-trips", "Compact view".equals(named.getLabel()));

    final ElwhaRadioButton labeled = new ElwhaRadioButton();
    final JLabel label = new JLabel("Cozy view");
    label.setLabelFor(labeled);
    check(
        "JLabel.setLabelFor is the fallback name source",
        "Cozy view".equals(labeled.getAccessibleContext().getAccessibleName()));

    named.setLabel(null);
    check("cleared label falls back", named.getAccessibleContext().getAccessibleName() == null);

    final ElwhaRadioButton overridden = new ElwhaRadioButton("Visible text");
    overridden.setAccessibleLabel("Spoken name");
    check(
        "setAccessibleLabel overrides the visual label",
        "Spoken name".equals(overridden.getAccessibleContext().getAccessibleName()));
    overridden.setAccessibleLabel(null);
    check(
        "cleared override falls back to the visual label",
        "Visible text".equals(overridden.getAccessibleContext().getAccessibleName()));
  }

  private static void checkMemberOf() {
    final ElwhaRadioGroup group = new ElwhaRadioGroup();
    final ElwhaRadioButton a = new ElwhaRadioButton();
    final ElwhaRadioButton b = new ElwhaRadioButton();
    final ElwhaRadioButton lone = new ElwhaRadioButton();
    group.add(a);
    group.add(b);

    final AccessibleRelation memberOf =
        a.getAccessibleContext().getAccessibleRelationSet().get(AccessibleRelation.MEMBER_OF);
    check("grouped radio carries MEMBER_OF", memberOf != null);
    check("MEMBER_OF targets the full membership", memberOf.getTarget().length == 2);

    check(
        "ungrouped radio has no MEMBER_OF",
        lone.getAccessibleContext().getAccessibleRelationSet().get(AccessibleRelation.MEMBER_OF)
            == null);

    group.remove(b);
    final AccessibleRelation after =
        a.getAccessibleContext().getAccessibleRelationSet().get(AccessibleRelation.MEMBER_OF);
    check("relation answers the live membership after remove", after.getTarget().length == 1);
    check(
        "removed radio loses the relation",
        b.getAccessibleContext().getAccessibleRelationSet().get(AccessibleRelation.MEMBER_OF)
            == null);
  }

  private static void check(final String what, final boolean ok) {
    if (!ok) {
      System.err.println("FAIL: " + what);
      System.exit(1);
    }
    System.out.println("  ok: " + what);
  }
}
