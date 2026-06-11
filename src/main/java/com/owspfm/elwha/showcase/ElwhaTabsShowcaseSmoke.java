package com.owspfm.elwha.showcase;

import com.owspfm.elwha.tabs.ElwhaTabs;
import com.owspfm.elwha.theme.ElwhaTheme;
import com.owspfm.elwha.theme.MaterialPalettes;
import com.owspfm.elwha.theme.Mode;
import java.awt.Component;
import java.awt.Container;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.util.ArrayList;
import java.util.List;
import javax.swing.JCheckBox;
import javax.swing.JComponent;

/**
 * Headless guard for the Showcase Tabs leaf (story #433). Builds the {@link TabsShowcasePanels}
 * Workbench + Gallery without a display, asserting the workbench stages exactly one live bar over
 * the {@link java.awt.CardLayout} pages recipe (activating a tab switches the visible page),
 * exercising every Workbench checkbox through its apply path via {@code doClick} (each rebuilds or
 * re-stamps the bar without throwing), counting the gallery's ten configuration bars, and laying
 * out + painting both surfaces into a {@link BufferedImage}.
 *
 * @author Charles Bryan
 * @version v0.4.0
 * @since v0.4.0
 */
public final class ElwhaTabsShowcaseSmoke {

  private ElwhaTabsShowcaseSmoke() {}

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

    final JComponent workbench = TabsShowcasePanels.buildWorkbench();
    check("workbench builds", workbench != null);
    check("workbench stages one live ElwhaTabs", collect(workbench, ElwhaTabs.class).size() == 1);

    final ElwhaTabs bar = collect(workbench, ElwhaTabs.class).get(0);
    final Container pages = findByName(workbench, "tabsShowcasePages");
    check("the CardLayout pages panel is staged", pages != null);
    check("page 0 starts visible", "page-0".equals(visibleChildName(pages)));
    bar.setActiveTabIndex(2);
    check("activating tab 2 switches to page 2", "page-2".equals(visibleChildName(pages)));

    for (JCheckBox box : collect(workbench, JCheckBox.class)) {
      box.doClick();
    }
    check(
        "every checkbox applies without throwing (bar rebuilt)",
        collect(workbench, ElwhaTabs.class).size() == 1);

    workbench.setSize(1100, 720);
    layoutTree(workbench);
    paint(workbench, 1100, 720);
    check("workbench paints headless", true);

    final JComponent gallery = TabsShowcasePanels.buildGallery();
    final List<ElwhaTabs> bars = collect(gallery, ElwhaTabs.class);
    check("gallery stacks the ten configuration bars", bars.size() == 10);

    gallery.setSize(900, gallery.getPreferredSize().height);
    layoutTree(gallery);
    paint(gallery, 900, Math.max(1, gallery.getHeight()));
    check("gallery paints headless", true);

    System.out.println(
        "ElwhaTabsShowcaseSmoke: OK (workbench stage + CardLayout recipe, checkbox apply paths,"
            + " 10-bar gallery, headless paint of both surfaces)");
  }

  private static String visibleChildName(final Container pages) {
    for (Component child : pages.getComponents()) {
      if (child.isVisible()) {
        return child.getName();
      }
    }
    return null;
  }

  private static Container findByName(final Container root, final String name) {
    if (name.equals(root.getName())) {
      return root;
    }
    for (Component child : root.getComponents()) {
      if (child instanceof Container container) {
        final Container hit = findByName(container, name);
        if (hit != null) {
          return hit;
        }
      }
    }
    return null;
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
