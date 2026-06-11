package com.owspfm.elwha.appbar;

import com.owspfm.elwha.fab.ElwhaFab;
import com.owspfm.elwha.fab.ElwhaFabAnchor;
import com.owspfm.elwha.icons.MaterialIcons;
import com.owspfm.elwha.theme.ColorRole;
import com.owspfm.elwha.theme.ElwhaTheme;
import com.owspfm.elwha.theme.MaterialPalettes;
import com.owspfm.elwha.theme.Mode;
import com.owspfm.elwha.theme.ScrollSourceBinding;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.util.ArrayList;
import java.util.List;
import javax.swing.JPanel;
import javax.swing.JScrollPane;

/**
 * S3 headless guard for {@code ScrollSourceBinding} + {@link ElwhaAppBar} lift-on-scroll (#457):
 * binding delivery/swap/detach semantics, scroll-driven lift state both directions, the
 * lift-on-scroll flag, the forced-lift gallery hook with pixel-asserted SURFACE_CONTAINER fill, and
 * a no-throw regression pass over the refactored {@link ElwhaFabAnchor} scroll responses.
 *
 * @author Charles Bryan
 * @version v0.4.0
 * @since v0.4.0
 */
public final class ElwhaAppBarLiftSmoke {

  private static final int BAR_WIDTH = 640;

  private ElwhaAppBarLiftSmoke() {}

  /**
   * Runs the guard; exits non-zero on the first failed check.
   *
   * @param args unused
   * @version v0.4.0
   * @since v0.4.0
   */
  public static void main(final String[] args) {
    System.setProperty("java.awt.headless", "true");
    ElwhaTheme.install(
        ElwhaTheme.config().theme(MaterialPalettes.baseline()).mode(Mode.LIGHT).build());

    checkBinding();
    checkBarLift();
    checkFabAnchorRegression();

    System.out.println(
        "ElwhaAppBarLiftSmoke: OK (binding semantics, scroll-driven lift, lift flag, forced-lift"
            + " pixels, FabAnchor regression)");
  }

  private static JScrollPane tallScroller() {
    final JPanel content = new JPanel();
    content.setPreferredSize(new Dimension(400, 4000));
    final JScrollPane scroller = new JScrollPane(content);
    scroller.setSize(400, 300);
    scroller.doLayout();
    return scroller;
  }

  private static void checkBinding() {
    final JScrollPane scroller = tallScroller();
    final List<int[]> events = new ArrayList<>();
    final ScrollSourceBinding binding =
        new ScrollSourceBinding((value, delta) -> events.add(new int[] {value, delta}));

    binding.setSource(scroller);
    check("setSource alone does not attach", !binding.isAttached());
    scroller.getVerticalScrollBar().getModel().setValue(100);
    check("unattached binding delivers nothing", events.isEmpty());
    check("value() readable while unattached", binding.value() == 100);

    binding.attach();
    check("attach", binding.isAttached());
    check("attach itself fires nothing", events.isEmpty());
    scroller.getVerticalScrollBar().getModel().setValue(150);
    check("delivery", events.size() == 1 && events.get(0)[0] == 150 && events.get(0)[1] == 50);
    scroller.getVerticalScrollBar().getModel().setValue(150);
    check("no-delta change swallowed", events.size() == 1);
    scroller.getVerticalScrollBar().getModel().setValue(120);
    check("negative delta", events.get(1)[0] == 120 && events.get(1)[1] == -30);

    final JScrollPane other = tallScroller();
    binding.setSource(other);
    check("source swap detaches", !binding.isAttached());
    scroller.getVerticalScrollBar().getModel().setValue(300);
    check("old source silent after swap", events.size() == 2);
    binding.attach();
    other.getVerticalScrollBar().getModel().setValue(40);
    check("new source delivers after re-attach", events.size() == 3 && events.get(2)[0] == 40);

    binding.detach();
    other.getVerticalScrollBar().getModel().setValue(80);
    check("detach stops delivery", events.size() == 3);
  }

  private static void checkBarLift() {
    final JScrollPane scroller = tallScroller();
    final ElwhaAppBar bar = ElwhaAppBar.small();
    bar.setTitle("Inbox");
    bar.setSize(BAR_WIDTH, 64);

    bar.setScrollSource(scroller);
    check("at top, not lifted", !bar.isLifted());
    scroller.getVerticalScrollBar().getModel().setValue(80);
    check("scrolled under lifts", bar.isLifted());
    scroller.getVerticalScrollBar().getModel().setValue(0);
    check("back to top unlifts", !bar.isLifted());

    scroller.getVerticalScrollBar().getModel().setValue(60);
    check("setup: lifted again", bar.isLifted());
    bar.setLiftOnScroll(false);
    check("lift flag off unlifts immediately", !bar.isLifted());
    scroller.getVerticalScrollBar().getModel().setValue(120);
    check("no lift while flag off", !bar.isLifted());
    bar.setLiftOnScroll(true);
    check("flag back on re-evaluates current offset", bar.isLifted());

    bar.setScrollSource(null);
    check("clearing the source unlifts", !bar.isLifted());

    final Color surface = ColorRole.SURFACE.resolve();
    final Color surfaceContainer = ColorRole.SURFACE_CONTAINER.resolve();
    check("rest container is SURFACE", sample(render(bar), BAR_WIDTH - 5, 5).equals(surface));
    bar.setLifted(true);
    check("forced lift state", bar.isLifted());
    check(
        "forced lift paints SURFACE_CONTAINER (snap path)",
        sample(render(bar), BAR_WIDTH - 5, 5).equals(surfaceContainer));
    bar.setLifted(false);
    check("forced unlift restores SURFACE", sample(render(bar), BAR_WIDTH - 5, 5).equals(surface));
  }

  private static void checkFabAnchorRegression() {
    final JScrollPane scroller = tallScroller();
    final ElwhaFab fab = ElwhaFab.extended(MaterialIcons.add(), "Compose");
    final ElwhaFabAnchor anchor = new ElwhaFabAnchor(scroller, fab);
    anchor.setSize(500, 400);
    anchor.doLayout();

    anchor.setScrollResponse(ElwhaFabAnchor.ScrollResponse.HIDE);
    scroller.getVerticalScrollBar().getModel().setValue(120);
    scroller.getVerticalScrollBar().getModel().setValue(40);
    anchor.setScrollResponse(ElwhaFabAnchor.ScrollResponse.SHRINK);
    scroller.getVerticalScrollBar().getModel().setValue(200);
    scroller.getVerticalScrollBar().getModel().setValue(60);
    anchor.setScrollResponse(ElwhaFabAnchor.ScrollResponse.NONE);
    scroller.getVerticalScrollBar().getModel().setValue(400);
    anchor.setScrollSource(null);
    anchor.setScrollSource(scroller);
    anchor.doLayout();
    check("FabAnchor scroll responses survive the binding refactor", true);
  }

  private static BufferedImage render(final ElwhaAppBar bar) {
    bar.doLayout();
    final BufferedImage image = new BufferedImage(BAR_WIDTH, 64, BufferedImage.TYPE_INT_ARGB);
    final Graphics2D g = image.createGraphics();
    try {
      bar.paint(g);
    } finally {
      g.dispose();
    }
    return image;
  }

  private static Color sample(final BufferedImage image, final int x, final int y) {
    return new Color(image.getRGB(x, y), true);
  }

  private static void check(final String what, final boolean ok) {
    if (!ok) {
      System.err.println("FAIL: " + what);
      System.exit(1);
    }
    System.out.println("  ok: " + what);
  }
}
