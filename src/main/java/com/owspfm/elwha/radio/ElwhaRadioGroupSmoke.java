package com.owspfm.elwha.radio;

import com.owspfm.elwha.theme.ElwhaTheme;
import com.owspfm.elwha.theme.MaterialPalettes;
import com.owspfm.elwha.theme.Mode;
import java.awt.event.MouseEvent;

/**
 * Headless guard for the S4 {@link ElwhaRadioGroup} selection model (story #420): user-gesture and
 * programmatic exclusion, the once-per-change group listener (with consistent {@code getSelected()}
 * inside member listeners), {@code setSelected}/{@code clearSelection} semantics, the
 * first-selected-wins add rule, selection-adoption on add, selected-member removal, and
 * group-to-group moves.
 *
 * @author Charles Bryan
 * @version v0.4.0
 * @since v0.4.0
 */
public final class ElwhaRadioGroupSmoke {

  private static final int SIZE = ElwhaRadioButton.STATE_LAYER_SIZE_PX;

  private ElwhaRadioGroupSmoke() {}

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

    checkExclusion();
    checkProgrammaticApi();
    checkAddRules();
    checkRemoveAndMove();

    System.out.println("ElwhaRadioGroupSmoke: OK (exclusion + events + membership rules)");
  }

  private static void checkExclusion() {
    final ElwhaRadioGroup group = new ElwhaRadioGroup();
    final ElwhaRadioButton a = sized(new ElwhaRadioButton());
    final ElwhaRadioButton b = sized(new ElwhaRadioButton());
    final ElwhaRadioButton c = sized(new ElwhaRadioButton());
    group.add(a);
    group.add(b);
    group.add(c);

    final int[] groupEvents = {0};
    final boolean[] consistentInside = {true};
    group.addChangeListener(e -> groupEvents[0]++);
    a.addChangeListener(
        e -> {
          if (!a.isSelected() && group.getSelected() == a) {
            consistentInside[0] = false;
          }
        });

    click(a);
    check("click selects A", group.getSelected() == a && a.isSelected());
    check("one group event for first select", groupEvents[0] == 1);

    final int[] actionsOnA = {0};
    a.addActionListener(e -> actionsOnA[0]++);
    click(b);
    check("click B deselects A", group.getSelected() == b && !a.isSelected() && b.isSelected());
    check("one group event per selection move", groupEvents[0] == 2);
    check("group state consistent inside A's deselect listener", consistentInside[0]);
    check("losing selection fires no ActionListener on A", actionsOnA[0] == 0);

    click(b);
    check("re-click selected member fires no group event", groupEvents[0] == 2);

    check("members keep navigation order", group.getMembers().equals(java.util.List.of(a, b, c)));
  }

  private static void checkProgrammaticApi() {
    final ElwhaRadioGroup group = new ElwhaRadioGroup();
    final ElwhaRadioButton a = sized(new ElwhaRadioButton());
    final ElwhaRadioButton b = sized(new ElwhaRadioButton());
    group.add(a);
    group.add(b);
    final int[] groupEvents = {0};
    final int[] actions = {0};
    group.addChangeListener(e -> groupEvents[0]++);
    a.addActionListener(e -> actions[0]++);
    b.addActionListener(e -> actions[0]++);

    group.setSelected(a);
    check("group.setSelected selects", group.getSelected() == a);
    b.setSelected(true);
    check("member setSelected(true) routes exclusion", group.getSelected() == b && !a.isSelected());
    check("programmatic paths fire no ActionListener", actions[0] == 0);
    check("two group events so far", groupEvents[0] == 2);

    group.clearSelection();
    check("clearSelection empties", group.getSelected() == null && !b.isSelected());
    check("clearSelection fires the group listener", groupEvents[0] == 3);
    group.clearSelection();
    check("clearSelection is idempotent", groupEvents[0] == 3);

    group.setSelected(b);
    b.setSelected(false);
    check("direct deselect of the holder empties the group", group.getSelected() == null);

    boolean threw = false;
    try {
      group.setSelected(sized(new ElwhaRadioButton()));
    } catch (final IllegalArgumentException e) {
      threw = true;
    }
    check("setSelected(non-member) throws", threw);
  }

  private static void checkAddRules() {
    final ElwhaRadioGroup group = new ElwhaRadioGroup();
    final ElwhaRadioButton holder = sized(new ElwhaRadioButton(true));
    group.add(holder);
    check("selected radio joining empty group is adopted", group.getSelected() == holder);

    final ElwhaRadioButton late = sized(new ElwhaRadioButton(true));
    final int[] groupEvents = {0};
    group.addChangeListener(e -> groupEvents[0]++);
    group.add(late);
    check("first-selected-wins: incoming radio deselected", !late.isSelected());
    check("existing selection kept", group.getSelected() == holder && holder.isSelected());
    check("normalizing add fires no group event", groupEvents[0] == 0);

    group.add(late);
    check("re-add is a no-op", group.getMembers().size() == 2);
  }

  private static void checkRemoveAndMove() {
    final ElwhaRadioGroup first = new ElwhaRadioGroup();
    final ElwhaRadioGroup second = new ElwhaRadioGroup();
    final ElwhaRadioButton a = sized(new ElwhaRadioButton());
    final ElwhaRadioButton b = sized(new ElwhaRadioButton());
    first.add(a);
    first.add(b);
    first.setSelected(a);

    final int[] firstEvents = {0};
    first.addChangeListener(e -> firstEvents[0]++);

    first.remove(a);
    check("removing the holder clears the group selection", first.getSelected() == null);
    check("removal fires the group listener", firstEvents[0] == 1);
    check("removed radio keeps its own state", a.isSelected());
    check("removed radio is ungrouped", a.getGroup() == null);

    first.setSelected(b);
    second.add(b);
    check("cross-group add moves the radio", b.getGroup() == second);
    check(
        "old group loses the member and its selection",
        first.getSelected() == null && first.getMembers().isEmpty());
    check("selected radio adopted by the new empty group", second.getSelected() == b);
  }

  // ------------------------------------------------------------------ plumbing

  private static ElwhaRadioButton sized(final ElwhaRadioButton radio) {
    radio.setSize(SIZE, SIZE);
    return radio;
  }

  private static void click(final ElwhaRadioButton radio) {
    final int cx = SIZE / 2;
    radio.dispatchEvent(
        new MouseEvent(
            radio, MouseEvent.MOUSE_PRESSED, 0, 0, cx, cx, 1, false, MouseEvent.BUTTON1));
    radio.dispatchEvent(
        new MouseEvent(
            radio, MouseEvent.MOUSE_RELEASED, 0, 0, cx, cx, 1, false, MouseEvent.BUTTON1));
  }

  private static void check(final String what, final boolean ok) {
    if (!ok) {
      System.err.println("FAIL: " + what);
      System.exit(1);
    }
    System.out.println("  ok: " + what);
  }
}
