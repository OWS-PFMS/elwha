package com.owspfm.elwha.showcase;

import com.owspfm.elwha.selectfield.ElwhaSelectField;
import com.owspfm.elwha.theme.ElwhaTheme;
import com.owspfm.elwha.theme.MaterialPalettes;
import java.awt.Component;
import java.awt.Container;
import java.util.ArrayList;
import java.util.List;
import javax.swing.JLabel;

/**
 * Phase 1 close-out guard (#378): the Select Field Showcase leaf builds and surfaces the expected
 * surfaces. Builds the leaf headlessly and asserts the Workbench exposes the variant / option-count
 * / state controls + a selected-value readout, and the Gallery hosts the variant×state matrix, a
 * long-list select, and pre-selected selects — i.e. real {@link ElwhaSelectField} instances.
 *
 * <p>Run: {@code mvn -q compile exec:java
 * -Dexec.mainClass="com.owspfm.elwha.showcase.SelectFieldShowcaseSmoke"}.
 *
 * @author Charles Bryan
 * @version v0.4.0
 * @since v0.4.0
 */
public final class SelectFieldShowcaseSmoke {

  private SelectFieldShowcaseSmoke() {}

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
      final Component leaf = SelectFieldShowcasePanels.buildComponent();
      final List<Component> all = new ArrayList<>();
      collect(leaf, all);

      check("Workbench exposes a 'Variant' control", hasLabel(all, "Variant"));
      check("Workbench exposes an 'Option count' control", hasLabel(all, "Option count"));
      check("Workbench exposes an 'Error text' control", hasLabel(all, "Error text"));
      check(
          "Workbench shows a selected-value readout",
          all.stream()
              .anyMatch(
                  c ->
                      c instanceof JLabel label
                          && label.getText() != null
                          && label.getText().startsWith("Selected value:")));
      check("Gallery adds a 'Variants × states' section", hasLabel(all, "Variants × states"));
      check(
          "Gallery adds a 'Long option list (scrolls)' section",
          hasLabel(all, "Long option list (scrolls)"));
      check("Gallery adds a 'Pre-selected value' section", hasLabel(all, "Pre-selected value"));

      long selects = all.stream().filter(c -> c instanceof ElwhaSelectField).count();
      check("leaf hosts multiple ElwhaSelectField instances (" + selects + ")", selects >= 5);

      boolean anyPreselected =
          all.stream()
              .anyMatch(c -> c instanceof ElwhaSelectField<?> s && s.getSelectedValue() != null);
      check("Gallery hosts a pre-selected ElwhaSelectField", anyPreselected);
    } catch (final RuntimeException ex) {
      check("leaf builds (threw " + ex + ")", false);
    }

    System.out.println(
        failures == 0
            ? "PASS — all Phase-1 close-out checks green"
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
