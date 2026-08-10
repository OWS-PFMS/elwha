package com.owspfm.elwha.showcase;

import com.owspfm.elwha.textfield.ElwhaTextField;
import com.owspfm.elwha.theme.ElwhaTheme;
import com.owspfm.elwha.theme.MaterialPalettes;
import java.awt.Component;
import java.awt.Container;
import java.util.ArrayList;
import java.util.List;
import javax.swing.JLabel;

/**
 * Phase 3 close-out guard (#364): the Text Field Showcase leaf surfaces the counter + supporting-
 * visibility work. Builds the leaf headlessly and asserts the Workbench grew a <i>Max length</i>
 * and a <i>Visibility</i> control and the Gallery grew a <i>Counter &amp; supporting visibility</i>
 * section that actually hosts an {@link ElwhaTextField} with a counter and one in {@code ON_FOCUS}.
 *
 * <p>Run: {@code mvn -q compile exec:java
 * -Dexec.mainClass="com.owspfm.elwha.showcase.TextFieldCounterShowcaseSmoke"}.
 *
 * @author Charles Bryan
 * @version v0.4.0
 * @since v0.4.0
 */
public final class TextFieldCounterShowcaseSmoke {

  private TextFieldCounterShowcaseSmoke() {}

  private static int failures;

  /**
   * Runs the guard; exits non-zero on any failed check.
   *
   * @param args ignored
   */
  public static void main(final String[] args) {
    System.setProperty("java.awt.headless", "true");
    ElwhaTheme.install(ElwhaTheme.config().theme(MaterialPalettes.baseline()).build());

    try {
      final Component leaf = TextFieldShowcasePanels.buildComponent();
      final List<Component> all = new ArrayList<>();
      collect(leaf, all);

      check("Workbench exposes a 'Max length' control", hasLabel(all, "Max length"));
      check("Workbench exposes a 'Visibility' control", hasLabel(all, "Visibility"));
      check(
          "Gallery adds a 'Counter & supporting visibility' section",
          hasLabel(all, "Counter & supporting visibility"));

      boolean anyCounter = false;
      boolean anyOnFocus = false;
      for (final Component c : all) {
        if (c instanceof ElwhaTextField field) {
          if (field.getMaxLength() >= 0) {
            anyCounter = true;
          }
          if (field.getSupportingTextVisibility()
              == ElwhaTextField.SupportingTextVisibility.ON_FOCUS) {
            anyOnFocus = true;
          }
        }
      }
      check("Gallery hosts a counter ElwhaTextField", anyCounter);
      check("Gallery hosts an ON_FOCUS ElwhaTextField", anyOnFocus);
    } catch (final RuntimeException ex) {
      check("leaf builds (threw " + ex + ")", false);
    }

    System.out.println(
        failures == 0
            ? "PASS — all Phase-3 close-out checks green"
            : "FAIL — " + failures + " check(s)");
    System.exit(failures == 0 ? 0 : 1);
  }

  private static void collect(final Component root, final List<Component> out) {
    out.add(root);
    if (root instanceof Container container) {
      for (final Component child : container.getComponents()) {
        collect(child, out);
      }
    }
  }

  private static boolean hasLabel(final List<Component> all, final String text) {
    for (final Component c : all) {
      if (c instanceof JLabel label && text.equals(label.getText())) {
        return true;
      }
    }
    return false;
  }

  private static void check(final String label, final boolean ok) {
    System.out.println((ok ? "  ok   " : "  FAIL ") + label);
    if (!ok) {
      failures++;
    }
  }
}
