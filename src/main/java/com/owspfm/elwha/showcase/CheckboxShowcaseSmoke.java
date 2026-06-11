package com.owspfm.elwha.showcase;

import com.owspfm.elwha.theme.ElwhaTheme;
import com.owspfm.elwha.theme.MaterialPalettes;
import com.owspfm.elwha.theme.Mode;
import java.awt.Dimension;
import javax.swing.JComponent;

/**
 * Headless construction guard for the Checkbox Showcase leaf (story #414): builds both {@link
 * CheckboxShowcasePanels} surfaces (Workbench + Gallery) and lays them out offscreen, proving the
 * panels construct, populate, and size without a display. Runs in CI's headless JVM ({@code
 * -Djava.awt.headless=true} safe).
 *
 * @author Charles Bryan
 * @version v0.4.0
 * @since v0.4.0
 */
public final class CheckboxShowcaseSmoke {

  private CheckboxShowcaseSmoke() {}

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

    final JComponent workbench = CheckboxShowcasePanels.buildWorkbench();
    layout(workbench, "workbench");
    final JComponent gallery = CheckboxShowcasePanels.buildGallery();
    layout(gallery, "gallery");

    check("workbench has children", workbench.getComponentCount() > 0);
    check("gallery has the 7x5 matrix + headers", gallery.getComponentCount() >= 42);

    System.out.println("CheckboxShowcaseSmoke: OK (workbench + gallery construct headless)");
  }

  private static void layout(final JComponent c, final String tag) {
    final Dimension pref = c.getPreferredSize();
    check(tag + " preferred size is positive", pref.width > 0 && pref.height > 0);
    c.setSize(pref);
    c.doLayout();
  }

  private static void check(final String what, final boolean ok) {
    if (!ok) {
      System.err.println("FAIL: " + what);
      System.exit(1);
    }
    System.out.println("  ✓ " + what);
  }
}
