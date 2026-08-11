package com.owspfm.elwha.showcase;

import com.owspfm.elwha.selectfield.ElwhaSelectField;
import com.owspfm.elwha.textfield.ElwhaTextField;
import com.owspfm.elwha.theme.ElwhaTheme;
import com.owspfm.elwha.theme.MaterialPalettes;
import java.awt.Component;
import java.awt.Container;
import java.util.ArrayList;
import java.util.List;
import javax.swing.JLabel;

/**
 * Phase 2 close-out guard (#394): the Select Field Showcase leaf surfaces the editable combo.
 * Builds the leaf headlessly and asserts the Workbench grew the "Editable combo" control section,
 * the Gallery grew the "Filtering (editable combo)" section, and the gallery hosts genuinely
 * editable {@link ElwhaSelectField}s (one constrained, one free-text-allowed) whose embedded
 * editors accept typed text. The filter/commit behavior itself is covered item-by-item in {@code
 * com.owspfm.elwha.selectfield.SelectFieldFilterSmoke} / {@code SelectFieldCommitSmoke}; the
 * Phase-1 leaf checks remain in {@code SelectFieldShowcaseSmoke}.
 *
 * <p>Run: {@code mvn -q compile exec:java
 * -Dexec.mainClass="com.owspfm.elwha.showcase.SelectFieldPhase2CloseoutSmoke"}.
 *
 * @author Charles Bryan
 * @version v0.4.0
 * @since v0.4.0
 */
public final class SelectFieldPhase2CloseoutSmoke {

  private SelectFieldPhase2CloseoutSmoke() {}

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

      check("Workbench grows an 'Editable combo' control section", hasLabel(all, "Editable combo"));
      check(
          "Gallery adds a 'Filtering (editable combo)' section",
          hasLabel(all, "Filtering (editable combo)"));

      final List<ElwhaSelectField<?>> editables = new ArrayList<>();
      for (final Component c : all) {
        if (c instanceof ElwhaSelectField<?> s && s.isEditable()) {
          editables.add(s);
        }
      }
      check(
          "leaf hosts editable ElwhaSelectFields (" + editables.size() + ")",
          editables.size() >= 2);
      check(
          "gallery hosts a free-text-allowed combo",
          editables.stream().anyMatch(ElwhaSelectField::isFreeTextAllowed));
      check(
          "gallery hosts a constrained editable combo",
          editables.stream().anyMatch(s -> !s.isFreeTextAllowed()));

      boolean typedOk = true;
      for (final ElwhaSelectField<?> combo : editables) {
        final ElwhaTextField field = (ElwhaTextField) combo.getComponent(0);
        field.getEditor().setText("berry");
        typedOk &= "berry".equals(combo.getText()) && !field.isReadOnly();
        field.getEditor().setText("");
      }
      check("every editable combo accepts typed text (read-only lifted)", typedOk);
    } catch (final RuntimeException ex) {
      check("leaf builds and drives (threw " + ex + ")", false);
    }

    System.out.println(
        failures == 0
            ? "PASS — all Phase-2 close-out checks green"
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
