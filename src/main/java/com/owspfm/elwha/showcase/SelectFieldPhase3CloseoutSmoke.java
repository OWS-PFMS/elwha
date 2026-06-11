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
 * Phase 3 close-out guard (#400): the Select Field Showcase leaf surfaces the multi-select. Builds
 * the leaf headlessly and asserts the Workbench grew the "Multi-select" control section, the
 * Gallery grew the "Multi-select (summary display)" section, and the gallery hosts genuine
 * multi-select {@link ElwhaSelectField}s with pre-checked values — one inside the summary limit
 * (joined display strings) and one past it (the count form). The model/summary/keyboard behavior
 * itself is covered item-by-item in {@code com.owspfm.elwha.selectfield.SelectFieldMultiSpikeSmoke}
 * / {@code SelectFieldSummarySmoke} / {@code SelectFieldMultiA11ySmoke}; the Phase-1/2 leaf checks
 * remain in {@code SelectFieldShowcaseSmoke} / {@code SelectFieldPhase2CloseoutSmoke}.
 *
 * <p>Run: {@code mvn -q compile exec:java
 * -Dexec.mainClass="com.owspfm.elwha.showcase.SelectFieldPhase3CloseoutSmoke"}.
 *
 * @author Charles Bryan
 * @version v0.4.0
 * @since v0.4.0
 */
public final class SelectFieldPhase3CloseoutSmoke {

  private SelectFieldPhase3CloseoutSmoke() {}

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

      check("Workbench grows a 'Multi-select' control section", hasLabel(all, "Multi-select"));
      check(
          "Gallery adds a 'Multi-select (summary display)' section",
          hasLabel(all, "Multi-select (summary display)"));

      final List<ElwhaSelectField<?>> multis = new ArrayList<>();
      for (final Component c : all) {
        if (c instanceof ElwhaSelectField<?> s && s.isMultiSelect()) {
          multis.add(s);
        }
      }
      check(
          "gallery hosts multi-select ElwhaSelectFields (" + multis.size() + ")",
          multis.size() >= 2);
      check(
          "every gallery multi-select is pre-checked",
          !multis.isEmpty() && multis.stream().noneMatch(s -> s.getSelectedValues().isEmpty()));
      check(
          "one renders the joined summary",
          multis.stream()
              .anyMatch(s -> s.getText().contains(", ") && !s.getText().endsWith("selected")));
      check(
          "one overflows to the count form",
          multis.stream()
              .anyMatch(
                  s ->
                      s.getText().endsWith("selected")
                          && s.getSelectedValues().size() > s.getSummaryLimit()));
      check(
          "multi-selects are non-editable (the documented V1 exclusion)",
          multis.stream().noneMatch(ElwhaSelectField::isEditable));
    } catch (final RuntimeException ex) {
      check("leaf builds and drives (threw " + ex + ")", false);
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
