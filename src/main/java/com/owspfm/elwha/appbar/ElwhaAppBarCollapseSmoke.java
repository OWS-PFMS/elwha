package com.owspfm.elwha.appbar;

import com.owspfm.elwha.theme.ColorRole;
import com.owspfm.elwha.theme.ElwhaTheme;
import com.owspfm.elwha.theme.MaterialPalettes;
import com.owspfm.elwha.theme.Mode;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import javax.swing.JPanel;
import javax.swing.JScrollPane;

/**
 * S4 headless guard for {@link ElwhaAppBar} flexible collapse (#460): scroll-position-driven
 * fraction and height both directions (scrubbing symmetry), the subtitle-dependent collapse range,
 * lift+collapse composition at full collapse (SURFACE_CONTAINER strip with the collapsed title
 * visible), the crossfade gates (no collapsed title below 0.7, no expanded headline at 1), the
 * forced-fraction gallery hook, and SMALL-variant immunity.
 *
 * @author Charles Bryan
 * @version v0.4.0
 * @since v0.4.0
 */
public final class ElwhaAppBarCollapseSmoke {

  private static final int BAR_WIDTH = 640;

  private ElwhaAppBarCollapseSmoke() {}

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

    checkFractionTracking();
    checkSubtitleRange();
    checkCrossfadeAndLiftComposition();
    checkHookAndSmallImmunity();

    System.out.println(
        "ElwhaAppBarCollapseSmoke: OK (fraction tracking both directions, subtitle range,"
            + " crossfade gates, lift composition, hook, SMALL immunity)");
  }

  private static JScrollPane tallScroller() {
    final JPanel content = new JPanel();
    content.setPreferredSize(new Dimension(400, 4000));
    final JScrollPane scroller = new JScrollPane(content);
    scroller.setSize(400, 300);
    scroller.doLayout();
    return scroller;
  }

  private static void checkFractionTracking() {
    final JScrollPane scroller = tallScroller();
    final ElwhaAppBar bar = ElwhaAppBar.mediumFlexible();
    bar.setTitle("Headline");
    bar.setScrollSource(scroller);

    check("at top: fraction 0", bar.getCollapsedFraction() == 0f);
    check("at top: preferred 112", bar.getPreferredSize().height == 112);

    scroller.getVerticalScrollBar().getModel().setValue(24);
    check("half range: fraction 0.5", Math.abs(bar.getCollapsedFraction() - 0.5f) < 0.01f);
    check("half range: preferred 88", bar.getPreferredSize().height == 88);

    scroller.getVerticalScrollBar().getModel().setValue(48);
    check("full range: fraction 1", bar.getCollapsedFraction() == 1f);
    check("full range: preferred 64", bar.getPreferredSize().height == 64);

    scroller.getVerticalScrollBar().getModel().setValue(300);
    check("beyond range clamps at 1", bar.getCollapsedFraction() == 1f);

    scroller.getVerticalScrollBar().getModel().setValue(12);
    check("scrub back re-expands (0.25)", Math.abs(bar.getCollapsedFraction() - 0.25f) < 0.01f);
    scroller.getVerticalScrollBar().getModel().setValue(0);
    check("back to top: fraction 0, preferred 112", bar.getPreferredSize().height == 112);

    final ElwhaAppBar sourceless = ElwhaAppBar.largeFlexible();
    sourceless.setTitle("Headline");
    check(
        "flexible without a scroll source stays expanded",
        sourceless.getPreferredSize().height == 120);
  }

  private static void checkSubtitleRange() {
    final JScrollPane scroller = tallScroller();
    final ElwhaAppBar bar = ElwhaAppBar.largeFlexible();
    bar.setTitle("Headline");
    bar.setSubtitle("Subtitle");
    bar.setScrollSource(scroller);

    scroller.getVerticalScrollBar().getModel().setValue(44);
    check(
        "subtitle range is 152-64=88 (44 → 0.5)",
        Math.abs(bar.getCollapsedFraction() - 0.5f) < 0.01f);
    bar.setSubtitle(null);
    scroller.getVerticalScrollBar().getModel().setValue(28);
    check(
        "no-subtitle range is 120-64=56 (28 → 0.5)",
        Math.abs(bar.getCollapsedFraction() - 0.5f) < 0.01f);
  }

  private static void checkCrossfadeAndLiftComposition() {
    final JScrollPane scroller = tallScroller();
    final ElwhaAppBar bar = ElwhaAppBar.mediumFlexible();
    bar.setTitle("Headline");
    bar.setScrollSource(scroller);
    final Color surfaceContainer = ColorRole.SURFACE_CONTAINER.resolve();
    final Color onSurface = ColorRole.ON_SURFACE.resolve();

    scroller.getVerticalScrollBar().getModel().setValue(48);
    check(
        "composition setup: collapsed + lifted",
        bar.getCollapsedFraction() == 1f && bar.isLifted());
    final BufferedImage collapsed = render(bar, 64);
    check(
        "collapsed strip paints SURFACE_CONTAINER",
        sample(collapsed, BAR_WIDTH - 5, 5).equals(surfaceContainer));
    check(
        "collapsed title visible in the strip at fraction 1",
        bandHasColorNear(collapsed, 16, 48, onSurface));

    scroller.getVerticalScrollBar().getModel().setValue(24);
    final BufferedImage half = render(bar, 88);
    check(
        "no collapsed title below the 0.7 gate (fraction 0.5)",
        !bandHasColorNear(half, 4, 20, onSurface));

    scroller.getVerticalScrollBar().getModel().setValue(48);
    final BufferedImage full = render(bar, 64);
    check(
        "no expanded headline remnant at fraction 1 (alpha 0)",
        bandIsColor(full, 56, 63, surfaceContainer));
  }

  private static void checkHookAndSmallImmunity() {
    final ElwhaAppBar flexible = ElwhaAppBar.mediumFlexible();
    flexible.setTitle("Headline");
    flexible.setCollapsedFraction(0.75f);
    check("forced fraction sticks", Math.abs(flexible.getCollapsedFraction() - 0.75f) < 0.001f);
    check("forced fraction drives preferred height", flexible.getPreferredSize().height == 76);
    flexible.setCollapsedFraction(2f);
    check("forced fraction clamps", flexible.getCollapsedFraction() == 1f);

    final JScrollPane scroller = tallScroller();
    final ElwhaAppBar small = ElwhaAppBar.small();
    small.setTitle("Inbox");
    small.setScrollSource(scroller);
    scroller.getVerticalScrollBar().getModel().setValue(200);
    check("SMALL ignores collapse (fraction 0)", small.getCollapsedFraction() == 0f);
    check("SMALL height stays 64", small.getPreferredSize().height == 64);
    small.setCollapsedFraction(0.5f);
    check("SMALL hook is a no-op", small.getCollapsedFraction() == 0f);
    check("SMALL still lifts", small.isLifted());
  }

  private static BufferedImage render(final ElwhaAppBar bar, final int height) {
    bar.setSize(BAR_WIDTH, height);
    bar.doLayout();
    final BufferedImage image = new BufferedImage(BAR_WIDTH, height, BufferedImage.TYPE_INT_ARGB);
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

  private static boolean bandHasColorNear(
      final BufferedImage image, final int rowFrom, final int rowTo, final Color c) {
    for (int y = rowFrom; y <= rowTo; y++) {
      for (int x = 0; x < image.getWidth(); x++) {
        if (colorDistance(sample(image, x, y), c) < 60) {
          return true;
        }
      }
    }
    return false;
  }

  private static boolean bandIsColor(
      final BufferedImage image, final int rowFrom, final int rowTo, final Color c) {
    for (int y = rowFrom; y <= rowTo; y++) {
      for (int x = 0; x < image.getWidth(); x++) {
        if (colorDistance(sample(image, x, y), c) > 12) {
          return false;
        }
      }
    }
    return true;
  }

  private static int colorDistance(final Color a, final Color b) {
    return Math.abs(a.getRed() - b.getRed())
        + Math.abs(a.getGreen() - b.getGreen())
        + Math.abs(a.getBlue() - b.getBlue());
  }

  private static void check(final String what, final boolean ok) {
    if (!ok) {
      System.err.println("FAIL: " + what);
      System.exit(1);
    }
    System.out.println("  ok: " + what);
  }
}
