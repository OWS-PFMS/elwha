package com.owspfm.elwha.showcase;

import com.owspfm.elwha.radio.ElwhaRadioButton;
import com.owspfm.elwha.theme.ElwhaTheme;
import com.owspfm.elwha.theme.MaterialPalettes;
import com.owspfm.elwha.theme.Mode;
import java.awt.Component;
import java.awt.Container;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import javax.swing.JComboBox;
import javax.swing.JComponent;
import javax.swing.SwingUtilities;

/**
 * Headless guard for the Showcase Radio button leaf (story #423). Builds the {@link
 * RadioButtonShowcasePanels} Workbench + Gallery surfaces without a display, asserts the gallery
 * carries the 2×5 state matrix (both press-swap cells, both disabled cells) plus the live 3-member
 * grouped strip, renders the gallery to an offscreen image, and drives the workbench's selection
 * control to prove the control rail applies to the live group.
 *
 * @author Charles Bryan
 * @version v0.4.0
 * @since v0.4.0
 */
public final class ElwhaRadioButtonShowcaseSmoke {

  private ElwhaRadioButtonShowcaseSmoke() {}

  /**
   * Runs the guard. Exits non-zero on any failed assertion.
   *
   * @param args unused
   * @version v0.4.0
   * @since v0.4.0
   */
  public static void main(final String[] args) throws Exception {
    System.setProperty("java.awt.headless", "true");
    ElwhaTheme.install(
        ElwhaTheme.config().theme(MaterialPalettes.baseline()).mode(Mode.LIGHT).build());

    SwingUtilities.invokeAndWait(ElwhaRadioButtonShowcaseSmoke::run);
  }

  private static void run() {
    final JComponent gallery = RadioButtonShowcasePanels.buildGallery();
    check("gallery builds", gallery != null);
    // 2 config rows × 5 state columns = 10 matrix radios + the 3-member grouped strip = 13.
    check("gallery is the 2×5 matrix + grouped strip", count(gallery, r -> true) == 13);
    // The Selected row (5) + the strip's first member = 6 selected radios.
    check("selected row + strip selection", count(gallery, ElwhaRadioButton::isSelected) == 6);
    // One disabled cell per row.
    check("both disabled cells present", count(gallery, r -> !r.isEnabled()) == 2);
    // Only the strip radios are grouped — and the group's roving rules leave exactly one tab stop.
    check("the strip radios are grouped", count(gallery, r -> r.getGroup() != null) == 3);
    check(
        "the strip radios carry built-in labels",
        count(gallery, r -> r.getGroup() != null && r.getLabel() != null) == 3);
    check(
        "the grouped strip is a single tab stop",
        count(gallery, r -> r.getGroup() != null && r.isFocusable()) == 1);

    gallery.setSize(900, 400);
    gallery.doLayout();
    final BufferedImage img = new BufferedImage(900, 400, BufferedImage.TYPE_INT_ARGB);
    final Graphics2D g = img.createGraphics();
    try {
      gallery.paint(g);
    } finally {
      g.dispose();
    }
    check("gallery renders offscreen without throwing", true);

    final JComponent workbench = RadioButtonShowcasePanels.buildWorkbench();
    check("workbench builds", workbench != null);
    check("workbench hosts the live 3-member group", count(workbench, r -> true) == 3);
    check("workbench starts with the first member selected", selectedIndex(workbench) == 0);

    final JComboBox<?> selectedBox = findCombo(workbench);
    check("workbench exposes the selection control", selectedBox != null);
    selectedBox.setSelectedIndex(2);
    check("selection control applies to the live group", selectedIndex(workbench) == 2);
    selectedBox.setSelectedIndex(3);
    check("'(none)' clears the group selection", selectedIndex(workbench) == -1);

    System.out.println("ElwhaRadioButtonShowcaseSmoke: OK (leaf panels + live controls)");
  }

  private static int selectedIndex(final Component root) {
    final java.util.List<ElwhaRadioButton> radios = new java.util.ArrayList<>();
    collect(root, radios);
    for (int i = 0; i < radios.size(); i++) {
      if (radios.get(i).isSelected()) {
        return i;
      }
    }
    return -1;
  }

  private static void collect(final Component root, final java.util.List<ElwhaRadioButton> out) {
    if (root instanceof ElwhaRadioButton radio) {
      out.add(radio);
    }
    if (root instanceof Container container) {
      for (final Component child : container.getComponents()) {
        collect(child, out);
      }
    }
  }

  private static int count(
      final Component root, final java.util.function.Predicate<ElwhaRadioButton> test) {
    int count = (root instanceof ElwhaRadioButton radio && test.test(radio)) ? 1 : 0;
    if (root instanceof Container container) {
      for (final Component child : container.getComponents()) {
        count += count(child, test);
      }
    }
    return count;
  }

  private static JComboBox<?> findCombo(final Component root) {
    if (root instanceof JComboBox<?> combo) {
      return combo;
    }
    if (root instanceof Container container) {
      for (final Component child : container.getComponents()) {
        final JComboBox<?> found = findCombo(child);
        if (found != null) {
          return found;
        }
      }
    }
    return null;
  }

  private static void check(final String what, final boolean ok) {
    if (!ok) {
      System.err.println("FAIL: " + what);
      System.exit(1);
    }
    System.out.println("  ok: " + what);
  }
}
