package com.owspfm.elwha.showcase;

import com.owspfm.elwha.checkbox.ElwhaCheckbox;
import com.owspfm.elwha.sidesheet.ElwhaSideSheet;
import com.owspfm.elwha.theme.ElwhaTheme;
import com.owspfm.elwha.theme.MaterialPalettes;
import com.owspfm.elwha.theme.Mode;
import java.awt.Component;
import java.awt.Container;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.util.ArrayList;
import java.util.List;
import javax.swing.JComponent;

/**
 * Headless guard for the Showcase Side Sheet leaf (story #466). Builds the {@link
 * SideSheetShowcasePanels} Workbench + Gallery without a display, asserting the workbench stages
 * exactly one live docked {@link ElwhaSideSheet}, exercising every Workbench checkbox through
 * {@code doClick} (open/close, affordances, dividers, RTL, modal dismissibility — each applies
 * without throwing; the modal trigger itself needs a realized window and is exercised by {@code
 * SideSheetModalSmoke}), counting the gallery's five configuration sheets, and laying out +
 * painting both surfaces into a {@link BufferedImage}.
 *
 * @author Charles Bryan
 * @version v0.4.0
 * @since v0.4.0
 */
public final class SideSheetShowcaseSmoke {

  private SideSheetShowcaseSmoke() {}

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

    final JComponent workbench = SideSheetShowcasePanels.buildWorkbench();
    check("workbench builds", workbench != null);
    final List<ElwhaSideSheet> staged = collect(workbench, ElwhaSideSheet.class);
    check("workbench stages exactly one live sheet", staged.size() == 1);
    check("staged sheet is open", staged.get(0).isOpen());

    for (final ElwhaCheckbox box : collect(workbench, ElwhaCheckbox.class)) {
      box.doClick();
      box.doClick();
    }
    check(
        "every checkbox round-trips without throwing",
        collect(workbench, ElwhaSideSheet.class).size() == 1);

    workbench.setSize(1200, 720);
    layoutTree(workbench);
    paint(workbench, 1200, 720);

    final JComponent gallery = SideSheetShowcasePanels.buildGallery();
    final List<ElwhaSideSheet> sheets = collect(gallery, ElwhaSideSheet.class);
    check("gallery embeds the five configuration sheets", sheets.size() == 5);
    gallery.setSize(1480, 480);
    layoutTree(gallery);
    paint(gallery, 1480, 480);

    System.out.println(
        "SideSheetShowcaseSmoke: OK (workbench stages one live sheet, checkbox round-trips,"
            + " 5-sheet gallery, both surfaces paint headless)");
  }

  private static <T> List<T> collect(final Container root, final Class<T> type) {
    final List<T> out = new ArrayList<>();
    for (final Component child : root.getComponents()) {
      if (type.isInstance(child)) {
        out.add(type.cast(child));
      }
      if (child instanceof Container nested) {
        out.addAll(collect(nested, type));
      }
    }
    return out;
  }

  private static void layoutTree(final Container root) {
    root.doLayout();
    for (final Component child : root.getComponents()) {
      if (child instanceof Container nested) {
        layoutTree(nested);
      }
    }
  }

  private static void paint(final JComponent surface, final int w, final int h) {
    final BufferedImage img = new BufferedImage(w, h, BufferedImage.TYPE_INT_ARGB);
    final Graphics2D g = img.createGraphics();
    try {
      surface.paint(g);
    } finally {
      g.dispose();
    }
  }

  private static void check(final String what, final boolean ok) {
    if (!ok) {
      System.err.println("FAIL: " + what);
      System.exit(1);
    }
  }
}
