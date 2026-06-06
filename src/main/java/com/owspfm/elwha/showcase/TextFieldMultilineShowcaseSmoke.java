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
 * Phase 2 close-out guard (#354): the Text Field Showcase leaf surfaces the multi-line work. Builds
 * the leaf headlessly and asserts the Workbench grew an <i>Input mode</i> control and the Gallery
 * grew a <i>Multi-line &amp; text area</i> section that actually hosts an {@link ElwhaTextField} in
 * a multi-line mode.
 *
 * <p>Run: {@code mvn -q compile exec:java
 * -Dexec.mainClass="com.owspfm.elwha.showcase.TextFieldMultilineShowcaseSmoke"}.
 *
 * @author Charles Bryan
 * @version v0.4.0
 * @since v0.4.0
 */
public final class TextFieldMultilineShowcaseSmoke {

  private TextFieldMultilineShowcaseSmoke() {}

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

      check("Workbench exposes an 'Input mode' control", hasLabel(all, "Input mode"));
      check("Workbench exposes a 'Text-area rows' control", hasLabel(all, "Text-area rows"));
      check(
          "Gallery adds a 'Multi-line & text area' section",
          hasLabel(all, "Multi-line & text area"));

      boolean anyMultiline = false;
      boolean anyTextArea = false;
      for (final Component c : all) {
        if (c instanceof ElwhaTextField field && field.isMultiline()) {
          anyMultiline = true;
          if (field.getInputMode() == ElwhaTextField.InputMode.TEXT_AREA) {
            anyTextArea = true;
          }
        }
      }
      check("Gallery hosts a multi-line ElwhaTextField", anyMultiline);
      check("Gallery hosts a fixed text-area ElwhaTextField", anyTextArea);
    } catch (final RuntimeException ex) {
      check("leaf builds (threw " + ex + ")", false);
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
