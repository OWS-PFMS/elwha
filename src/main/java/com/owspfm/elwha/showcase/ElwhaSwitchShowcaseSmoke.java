package com.owspfm.elwha.showcase;

import com.owspfm.elwha.checkbox.ElwhaCheckbox;
import com.owspfm.elwha.selectfield.ElwhaSelectField;
import com.owspfm.elwha.switches.ElwhaSwitch;
import com.owspfm.elwha.textfield.ElwhaTextField;
import com.owspfm.elwha.theme.ElwhaTheme;
import com.owspfm.elwha.theme.MaterialPalettes;
import com.owspfm.elwha.theme.Mode;
import java.awt.Component;
import java.awt.Container;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import javax.swing.JComponent;

/**
 * Headless guard for the Showcase Switch leaf (story #407). Builds the {@link SwitchShowcasePanels}
 * Workbench + Gallery without a display, asserting the gallery is the full 4×7 state matrix (4 icon
 * configurations × unselected / selected / hover / focused / pressed / two disabled cells — both
 * disabled columns because the spec's disabled treatment is asymmetric), exercising every Workbench
 * checkbox through its apply path via {@code doClick}, and laying out + painting both surfaces into
 * a {@link BufferedImage}.
 *
 * @author Charles Bryan
 * @version v0.4.0
 * @since v0.4.0
 */
public final class ElwhaSwitchShowcaseSmoke {

  private ElwhaSwitchShowcaseSmoke() {}

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

    final JComponent workbench = SwitchShowcasePanels.buildWorkbench();
    check("workbench builds", workbench != null);
    check("workbench hosts one live ElwhaSwitch", countSwitches(workbench) == 1);
    // Two ElwhaTextFields: the label control + the one embedded in the icon-mode select field.
    check(
        "the label control is a dogfooded ElwhaTextField",
        collect(workbench, ElwhaTextField.class).size() == 2);
    final ElwhaSwitch staged = collect(workbench, ElwhaSwitch.class).get(0);
    check(
        "the visible stage label names the switch via labelFor",
        "Wi-Fi".equals(staged.getAccessibleContext().getAccessibleName()));

    int clicked = 0;
    for (final ElwhaCheckbox box : collect(workbench, ElwhaCheckbox.class)) {
      box.doClick();
      box.doClick();
      clicked++;
    }
    check("every workbench checkbox exercises the apply path (x2)", clicked >= 4);
    check(
        "the icon-mode selector is a dogfooded ElwhaSelectField",
        collect(workbench, ElwhaSelectField.class).size() == 1);
    @SuppressWarnings("unchecked")
    final ElwhaSelectField<SwitchShowcasePanels.IconMode> modeField =
        (ElwhaSelectField<SwitchShowcasePanels.IconMode>)
            collect(workbench, ElwhaSelectField.class).get(0);
    for (final SwitchShowcasePanels.IconMode mode : SwitchShowcasePanels.IconMode.values()) {
      modeField.setSelectedValue(mode);
    }
    check(
        "the icon-mode select field exercises all three modes through the apply path",
        modeField.getSelectedValue() == SwitchShowcasePanels.IconMode.SELECTED_ONLY);
    renderOnce(workbench);
    check("workbench lays out and paints headlessly", true);

    final JComponent gallery = SwitchShowcasePanels.buildGallery();
    check("gallery builds", gallery != null);
    // 4 icon configurations × 7 state columns = 28 switches.
    check("gallery is the 4x7 state matrix", countSwitches(gallery) == 28);
    check("gallery disabled cells span both selection states", countDisabled(gallery) == 8);
    check(
        "gallery icon rows carry the icons configurations",
        countIconsVisible(gallery) == 14 && countOnlySelectedIcon(gallery) == 7);
    renderOnce(gallery);
    check("gallery lays out and paints headlessly", true);

    System.out.println(
        "ElwhaSwitchShowcaseSmoke: OK (workbench apply path + 4x7 gallery matrix + headless"
            + " layout/paint)");
  }

  private static int countSwitches(final Component root) {
    return collect(root, ElwhaSwitch.class).size();
  }

  private static int countDisabled(final Component root) {
    return (int) collect(root, ElwhaSwitch.class).stream().filter(s -> !s.isEnabled()).count();
  }

  private static int countIconsVisible(final Component root) {
    return (int)
        collect(root, ElwhaSwitch.class).stream().filter(ElwhaSwitch::isIconsVisible).count();
  }

  private static int countOnlySelectedIcon(final Component root) {
    return (int)
        collect(root, ElwhaSwitch.class).stream()
            .filter(ElwhaSwitch::isShowOnlySelectedIcon)
            .count();
  }

  private static <T extends Component> java.util.List<T> collect(
      final Component root, final Class<T> type) {
    final java.util.List<T> found = new java.util.ArrayList<>();
    if (type.isInstance(root)) {
      found.add(type.cast(root));
    }
    if (root instanceof Container container) {
      for (final Component child : container.getComponents()) {
        found.addAll(collect(child, type));
      }
    }
    return found;
  }

  /** Sizes the surface to its preferred size, lays out the tree, and paints it offscreen. */
  private static void renderOnce(final JComponent surface) {
    surface.setSize(surface.getPreferredSize());
    layoutTree(surface);
    final BufferedImage img =
        new BufferedImage(
            Math.max(1, surface.getWidth()),
            Math.max(1, surface.getHeight()),
            BufferedImage.TYPE_INT_ARGB);
    final Graphics2D g = img.createGraphics();
    try {
      surface.paint(g);
    } finally {
      g.dispose();
    }
  }

  private static void layoutTree(final Component c) {
    c.doLayout();
    if (c instanceof Container container) {
      for (final Component child : container.getComponents()) {
        layoutTree(child);
      }
    }
  }

  private static void check(final String what, final boolean ok) {
    if (!ok) {
      System.err.println("FAIL: " + what);
      System.exit(1);
    }
    System.out.println("  ok: " + what);
  }
}
