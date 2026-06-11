package com.owspfm.elwha.showcase;

import com.owspfm.elwha.checkbox.ElwhaCheckbox;
import com.owspfm.elwha.progress.AbstractElwhaProgressIndicator;
import com.owspfm.elwha.progress.ElwhaCircularProgressIndicator;
import com.owspfm.elwha.progress.ElwhaLinearProgressIndicator;
import com.owspfm.elwha.slider.ElwhaSlider;
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
 * Headless guard for the Showcase Progress leaf (story #475). Builds the {@link
 * ProgressShowcasePanels} Workbench + Gallery without a display, asserting the workbench stages
 * exactly one live indicator sharing its {@code BoundedRangeModel} with the value {@link
 * ElwhaSlider} (writes through the indicator land in the slider), exercising every Workbench
 * checkbox through its live apply path via {@code doClick} (the staged instance survives — only
 * the variant swap rebuilds), counting the gallery's thirteen configurations (seven linear + six
 * circular), and laying out + painting both surfaces into a {@link BufferedImage}.
 *
 * @author Charles Bryan
 * @version v0.4.0
 * @since v0.4.0
 */
public final class ProgressShowcaseSmoke {

  private ProgressShowcaseSmoke() {}

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

    final JComponent workbench = ProgressShowcasePanels.buildWorkbench();
    check("workbench builds", workbench != null);
    final List<AbstractElwhaProgressIndicator> staged =
        collect(workbench, AbstractElwhaProgressIndicator.class);
    check("workbench stages exactly one live indicator", staged.size() == 1);
    check(
        "the default staged variant is linear",
        staged.get(0) instanceof ElwhaLinearProgressIndicator);

    final ElwhaSlider valueSlider = collect(workbench, ElwhaSlider.class).get(0);
    staged.get(0).setValue(80);
    check("the slider and the indicator share one model", valueSlider.getValue() == 80);
    valueSlider.setValue(35);
    check("writes through the slider land in the indicator", staged.get(0).getValue() == 35);

    for (ElwhaCheckbox box : collect(workbench, ElwhaCheckbox.class)) {
      box.doClick();
    }
    final List<AbstractElwhaProgressIndicator> afterClicks =
        collect(workbench, AbstractElwhaProgressIndicator.class);
    check("checkbox apply paths are live (no rebuild, no throw)", afterClicks.size() == 1);
    check("the staged instance survived the live applies", afterClicks.get(0) == staged.get(0));
    check("the indeterminate checkbox landed on the component", staged.get(0).isIndeterminate());
    check("the wavy checkbox landed on the component", staged.get(0).isWavy());

    workbench.setSize(1100, 720);
    layoutTree(workbench);
    paint(workbench, 1100, 720);
    check("workbench paints headless", true);

    final JComponent gallery = ProgressShowcasePanels.buildGallery();
    check(
        "gallery stacks seven linear configurations",
        collect(gallery, ElwhaLinearProgressIndicator.class).size() == 7);
    check(
        "gallery shows six circular configurations",
        collect(gallery, ElwhaCircularProgressIndicator.class).size() == 6);

    gallery.setSize(900, Math.max(1, gallery.getPreferredSize().height));
    layoutTree(gallery);
    paint(gallery, 900, Math.max(1, gallery.getHeight()));
    check("gallery paints headless", true);

    System.out.println(
        "ProgressShowcaseSmoke: OK (staged indicator + shared model, live apply paths, 13-cell"
            + " gallery, headless paint of both surfaces)");
  }

  private static <T extends Component> List<T> collect(final Container root, final Class<T> type) {
    final List<T> out = new ArrayList<>();
    collectInto(root, type, out);
    return out;
  }

  private static <T extends Component> void collectInto(
      final Container root, final Class<T> type, final List<T> out) {
    for (Component child : root.getComponents()) {
      if (type.isInstance(child)) {
        out.add(type.cast(child));
      }
      if (child instanceof Container container) {
        collectInto(container, type, out);
      }
    }
  }

  private static void layoutTree(final Component root) {
    root.doLayout();
    if (root instanceof Container container) {
      for (Component child : container.getComponents()) {
        layoutTree(child);
      }
    }
  }

  private static void paint(final JComponent component, final int width, final int height) {
    final BufferedImage image = new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB);
    final Graphics2D g = image.createGraphics();
    try {
      component.paint(g);
    } finally {
      g.dispose();
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
