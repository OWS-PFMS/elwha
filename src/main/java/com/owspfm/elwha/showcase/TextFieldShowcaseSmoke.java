package com.owspfm.elwha.showcase;

import com.owspfm.elwha.theme.ElwhaTheme;
import com.owspfm.elwha.theme.MaterialPalettes;
import javax.swing.JTabbedPane;

/**
 * S8 headless guard (#338): the Text Field Showcase leaf builds without throwing and exposes both
 * the Workbench and Gallery tabs. The Workbench instantiates a {@link ComponentWorkbench} with the
 * dogfooded Elwha controls; the Gallery builds the variant&#215;state matrix and the slot examples.
 *
 * <p>Run: {@code mvn -q compile exec:java
 * -Dexec.mainClass="com.owspfm.elwha.showcase.TextFieldShowcaseSmoke"}.
 *
 * @author Charles Bryan
 * @version v0.4.0
 * @since v0.4.0
 */
public final class TextFieldShowcaseSmoke {

  private TextFieldShowcaseSmoke() {}

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
      final JTabbedPane leaf = (JTabbedPane) TextFieldShowcasePanels.buildComponent();
      check("leaf builds", leaf != null);
      check("has Workbench + Gallery tabs", leaf.getTabCount() == 2);
      check("tab 0 is Workbench", "Workbench".equals(leaf.getTitleAt(0)));
      check("tab 1 is Gallery", "Gallery".equals(leaf.getTitleAt(1)));
      check(
          "Workbench tab is a ComponentWorkbench",
          leaf.getComponentAt(0) instanceof ComponentWorkbench);
    } catch (final RuntimeException ex) {
      check("leaf builds (threw " + ex + ")", false);
    }

    System.out.println(
        failures == 0 ? "PASS — all S8 checks green" : "FAIL — " + failures + " check(s)");
    System.exit(failures == 0 ? 0 : 1);
  }

  private static void check(final String label, final boolean ok) {
    System.out.println((ok ? "  ok   " : "  FAIL ") + label);
    if (!ok) {
      failures++;
    }
  }
}
