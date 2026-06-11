package com.owspfm.elwha.showcase;

import com.owspfm.elwha.appbar.ElwhaAppBar;
import com.owspfm.elwha.checkbox.ElwhaCheckbox;
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
import javax.swing.JScrollPane;

/**
 * Headless guard for the Showcase App Bar leaf (story #462). Builds the {@link
 * AppBarShowcasePanels} Workbench + Gallery without a display, asserting the workbench stages
 * exactly one live bar over the named scroll stub (driving the stub's scrollbar model lifts the
 * bar), exercising every Workbench checkbox through its apply path via {@code doClick}, counting
 * the gallery's ten configuration bars (with the collapsed row at fraction&nbsp;1 and the lifted
 * row lifted), and laying out + painting both surfaces into a {@link BufferedImage}.
 *
 * @author Charles Bryan
 * @version v0.4.0
 * @since v0.4.0
 */
public final class ElwhaAppBarShowcaseSmoke {

  private ElwhaAppBarShowcaseSmoke() {}

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

    final JComponent workbench = AppBarShowcasePanels.buildWorkbench();
    check("workbench builds", workbench != null);
    check(
        "workbench stages one live ElwhaAppBar", collect(workbench, ElwhaAppBar.class).size() == 1);

    final ElwhaAppBar bar = collect(workbench, ElwhaAppBar.class).get(0);
    JScrollPane scroller = null;
    for (JScrollPane candidate : collect(workbench, JScrollPane.class)) {
      if ("appBarShowcaseScroller".equals(candidate.getName())) {
        scroller = candidate;
      }
    }
    check("the named scroll stub is staged", scroller != null);
    check("the bar is wired to the stub", bar.getScrollSource() == scroller);
    scroller.getVerticalScrollBar().getModel().setValue(80);
    check("driving the stub lifts the bar", bar.isLifted());
    scroller.getVerticalScrollBar().getModel().setValue(0);
    check("back to top unlifts", !bar.isLifted());

    for (ElwhaCheckbox box : collect(workbench, ElwhaCheckbox.class)) {
      box.doClick();
    }
    check(
        "every checkbox applies without throwing (bar rebuilt)",
        collect(workbench, ElwhaAppBar.class).size() == 1);

    workbench.setSize(1100, 720);
    layoutTree(workbench);
    paint(workbench, 1100, 720);
    check("workbench paints headless", true);

    final JComponent gallery = AppBarShowcasePanels.buildGallery();
    final List<ElwhaAppBar> bars = collect(gallery, ElwhaAppBar.class);
    check("gallery stacks the ten configuration bars", bars.size() == 10);
    boolean sawCollapsed = false;
    boolean sawLifted = false;
    for (ElwhaAppBar candidate : bars) {
      sawCollapsed |= candidate.getCollapsedFraction() == 1f;
      sawLifted |= candidate.isLifted();
    }
    check("a collapsed row is forced to fraction 1", sawCollapsed);
    check("a lifted row is forced lifted", sawLifted);

    gallery.setSize(900, gallery.getPreferredSize().height);
    layoutTree(gallery);
    paint(gallery, 900, Math.max(1, gallery.getHeight()));
    check("gallery paints headless", true);

    System.out.println(
        "ElwhaAppBarShowcaseSmoke: OK (workbench stage + scroll-lift wiring, checkbox apply"
            + " paths, 10-bar gallery with forced collapse/lift, headless paint)");
  }

  private static <T extends Component> List<T> collect(final Component root, final Class<T> type) {
    final List<T> found = new ArrayList<>();
    if (type.isInstance(root)) {
      found.add(type.cast(root));
    }
    if (root instanceof Container container) {
      for (Component child : container.getComponents()) {
        found.addAll(collect(child, type));
      }
    }
    return found;
  }

  private static void layoutTree(final Component root) {
    root.doLayout();
    if (root instanceof Container container) {
      for (Component child : container.getComponents()) {
        layoutTree(child);
      }
    }
  }

  private static void paint(final Component root, final int width, final int height) {
    final BufferedImage image =
        new BufferedImage(Math.max(1, width), Math.max(1, height), BufferedImage.TYPE_INT_ARGB);
    final Graphics2D g = image.createGraphics();
    try {
      root.paint(g);
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
