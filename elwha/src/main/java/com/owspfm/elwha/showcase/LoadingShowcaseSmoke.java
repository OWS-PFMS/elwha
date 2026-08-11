package com.owspfm.elwha.showcase;

import com.owspfm.elwha.loading.ElwhaLoadingIndicator;
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
 * Headless guard for the Showcase Loading leaf (story #519). Builds the {@link
 * LoadingShowcasePanels} Workbench + Gallery without a display, asserting the workbench stages
 * exactly one live {@link ElwhaLoadingIndicator} sharing its model with the value {@link
 * ElwhaSlider} (writes through either land in the other), that the initial apply landed (standard
 * indeterminate at 56px), that the gallery holds the twelve configurations, and that both surfaces
 * lay out and paint into a {@link BufferedImage}.
 *
 * @author Charles Bryan
 * @version v0.5.0
 * @since v0.5.0
 */
public final class LoadingShowcaseSmoke {

  private LoadingShowcaseSmoke() {}

  /**
   * Runs the guard. Exits non-zero on any failed assertion.
   *
   * @param args unused
   * @version v0.5.0
   * @since v0.5.0
   */
  public static void main(final String[] args) {
    System.setProperty("java.awt.headless", "true");
    ElwhaTheme.install(
        ElwhaTheme.config().theme(MaterialPalettes.baseline()).mode(Mode.LIGHT).build());

    final JComponent workbench = LoadingShowcasePanels.buildWorkbench();
    check("workbench builds", workbench != null);
    final List<ElwhaLoadingIndicator> staged = collect(workbench, ElwhaLoadingIndicator.class);
    check("workbench stages exactly one live indicator", staged.size() == 1);
    final ElwhaLoadingIndicator indicator = staged.get(0);
    check("initial apply: indeterminate", indicator.isIndeterminate());
    check("initial apply: standard (not contained)", !indicator.isContained());
    check("initial apply: 56px", indicator.getIndicatorSize() == 56);

    final ElwhaSlider valueSlider = collect(workbench, ElwhaSlider.class).get(0);
    indicator.setValue(80);
    check("the slider and the indicator share one model", valueSlider.getValue() == 80);
    valueSlider.setValue(35);
    check("writes through the slider land in the indicator", indicator.getValue() == 35);

    workbench.setSize(1100, 720);
    layoutTree(workbench);
    paint(workbench, 1100, 720);
    check("workbench paints headless", true);

    final JComponent gallery = LoadingShowcasePanels.buildGallery();
    check(
        "gallery holds the twelve configurations",
        collect(gallery, ElwhaLoadingIndicator.class).size() == 12);
    gallery.setSize(900, Math.max(1, gallery.getPreferredSize().height));
    layoutTree(gallery);
    paint(gallery, 900, Math.max(1, gallery.getHeight()));
    check("gallery paints headless", true);

    System.out.println(
        "LoadingShowcaseSmoke: OK (staged indicator + shared model, initial apply, 12-cell gallery,"
            + " headless paint of both surfaces)");
  }

  private static <T extends Component> List<T> collect(final Container root, final Class<T> type) {
    final List<T> out = new ArrayList<>();
    collectInto(root, type, out);
    return out;
  }

  private static <T extends Component> void collectInto(
      final Container root, final Class<T> type, final List<T> out) {
    for (final Component child : root.getComponents()) {
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
      for (final Component child : container.getComponents()) {
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
