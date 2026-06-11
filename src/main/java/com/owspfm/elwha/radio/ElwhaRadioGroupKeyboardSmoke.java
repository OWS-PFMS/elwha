package com.owspfm.elwha.radio;

import com.owspfm.elwha.theme.ElwhaTheme;
import com.owspfm.elwha.theme.MaterialPalettes;
import com.owspfm.elwha.theme.Mode;
import java.awt.ComponentOrientation;
import java.awt.event.KeyEvent;
import javax.swing.JComponent;
import javax.swing.KeyStroke;

/**
 * Headless guard for the S5 {@link ElwhaRadioGroup} keyboard contract (story #421): arrow bindings
 * install/uninstall with membership, navigation wraps and skips disabled members, selection follows
 * focus as a user gesture (one {@code ActionListener} fire per arrival), horizontal arrows mirror
 * under RTL orientation, and the three roving-tab-stop focusable rules hold (selected-only /
 * all-when-empty / restore-on-remove). True focus transit needs a window — the keyboard demo walks
 * that live; here the action handlers are driven directly.
 *
 * @author Charles Bryan
 * @version v0.4.0
 * @since v0.4.0
 */
public final class ElwhaRadioGroupKeyboardSmoke {

  private ElwhaRadioGroupKeyboardSmoke() {}

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

    checkBindingLifecycle();
    checkNavigation();
    checkRtl();
    checkRovingRules();

    System.out.println("ElwhaRadioGroupKeyboardSmoke: OK (arrows + roving rules + RTL)");
  }

  private static void checkBindingLifecycle() {
    final ElwhaRadioGroup group = new ElwhaRadioGroup();
    final ElwhaRadioButton radio = new ElwhaRadioButton();
    final KeyStroke down = KeyStroke.getKeyStroke(KeyEvent.VK_DOWN, 0);

    check(
        "ungrouped radio has no arrow binding",
        radio.getInputMap(JComponent.WHEN_FOCUSED).get(down) == null);
    group.add(radio);
    check(
        "grouped radio gains the arrow bindings",
        radio.getInputMap(JComponent.WHEN_FOCUSED).get(down) != null
            && radio.getActionMap().get("elwhaRadioGroup.down") != null);
    group.remove(radio);
    check(
        "removal uninstalls the bindings",
        radio.getInputMap(JComponent.WHEN_FOCUSED).get(down) == null
            && radio.getActionMap().get("elwhaRadioGroup.down") == null);
    check("removal restores plain focusability", radio.isFocusable());
  }

  private static void checkNavigation() {
    final ElwhaRadioGroup group = new ElwhaRadioGroup();
    final ElwhaRadioButton a = new ElwhaRadioButton(true);
    final ElwhaRadioButton b = new ElwhaRadioButton();
    final ElwhaRadioButton c = new ElwhaRadioButton();
    group.add(a);
    group.add(b);
    group.add(c);

    final int[] arrivals = {0};
    b.addActionListener(e -> arrivals[0]++);
    c.addActionListener(e -> arrivals[0]++);
    a.addActionListener(e -> arrivals[0]++);

    arrow(a, "down");
    check("Down moves the selection forward", group.getSelected() == b);
    check("arrival is a user gesture (ActionListener fired)", arrivals[0] == 1);

    arrow(b, "down");
    check("Down again reaches the last member", group.getSelected() == c);
    arrow(c, "down");
    check("Down wraps from last to first", group.getSelected() == a);
    arrow(a, "up");
    check("Up wraps from first to last", group.getSelected() == c);
    check("four arrivals, four action fires", arrivals[0] == 4);

    b.setEnabled(false);
    arrow(c, "up");
    check("Up skips the disabled middle member", group.getSelected() == a);
    check("disabled member never selected by navigation", !b.isSelected());

    final ElwhaRadioGroup solo = new ElwhaRadioGroup();
    final ElwhaRadioButton only = new ElwhaRadioButton(true);
    solo.add(only);
    arrow(only, "down");
    check("single-member group navigation is a no-op", solo.getSelected() == only);
  }

  private static void checkRtl() {
    final ElwhaRadioGroup group = new ElwhaRadioGroup();
    final ElwhaRadioButton a = new ElwhaRadioButton(true);
    final ElwhaRadioButton b = new ElwhaRadioButton();
    group.add(a);
    group.add(b);

    arrow(a, "right");
    check("LTR Right moves forward", group.getSelected() == b);
    arrow(b, "left");
    check("LTR Left moves back", group.getSelected() == a);

    a.setComponentOrientation(ComponentOrientation.RIGHT_TO_LEFT);
    b.setComponentOrientation(ComponentOrientation.RIGHT_TO_LEFT);
    arrow(a, "left");
    check("RTL Left moves forward", group.getSelected() == b);
    arrow(b, "right");
    check("RTL Right moves back", group.getSelected() == a);

    arrow(a, "down");
    check("vertical arrows ignore orientation", group.getSelected() == b);
  }

  private static void checkRovingRules() {
    final ElwhaRadioGroup group = new ElwhaRadioGroup();
    final ElwhaRadioButton a = new ElwhaRadioButton();
    final ElwhaRadioButton b = new ElwhaRadioButton();
    final ElwhaRadioButton c = new ElwhaRadioButton();
    group.add(a);
    group.add(b);
    group.add(c);

    check(
        "rule 3: none selected/focused → all focusable",
        a.isFocusable() && b.isFocusable() && c.isFocusable());

    group.setSelected(b);
    check(
        "rule 1: selected member is the only tab stop",
        !a.isFocusable() && b.isFocusable() && !c.isFocusable());

    arrow(b, "down");
    check(
        "navigation moves the tab stop with the selection",
        !a.isFocusable() && !b.isFocusable() && c.isFocusable());

    group.clearSelection();
    check(
        "clearing the selection restores all stops",
        a.isFocusable() && b.isFocusable() && c.isFocusable());

    group.setSelected(a);
    a.setEnabled(false);
    check(
        "disabled selected member falls back to all-focusable",
        a.isFocusable() && b.isFocusable() && c.isFocusable());
  }

  // ------------------------------------------------------------------ plumbing

  private static void arrow(final ElwhaRadioButton radio, final String direction) {
    radio.getActionMap().get("elwhaRadioGroup." + direction).actionPerformed(null);
  }

  private static void check(final String what, final boolean ok) {
    if (!ok) {
      System.err.println("FAIL: " + what);
      System.exit(1);
    }
    System.out.println("  ok: " + what);
  }
}
