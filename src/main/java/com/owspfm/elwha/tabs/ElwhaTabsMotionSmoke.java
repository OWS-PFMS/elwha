package com.owspfm.elwha.tabs;

import com.owspfm.elwha.theme.ElwhaTheme;
import com.owspfm.elwha.theme.MaterialPalettes;
import com.owspfm.elwha.theme.Mode;
import com.owspfm.elwha.theme.MorphAnimator;
import java.awt.Rectangle;

/**
 * S3 headless guard for the {@link ElwhaTabs} indicator slide (#428): snap semantics
 * (non-displayable, initial auto-activation, reduced motion), live mid-flight x+width
 * interpolation between unequal tabs, settle-at-rest, and the mid-slide retarget — driven
 * against a test subclass that reports itself displayable so the animator engages headlessly.
 *
 * @author Charles Bryan
 * @version v0.4.0
 * @since v0.4.0
 */
public final class ElwhaTabsMotionSmoke {

  private static final int BAR_WIDTH = 480;
  private static final int SETTLE_TIMEOUT_MS = 2000;

  private ElwhaTabsMotionSmoke() {}

  // Reports displayable so activate() engages the slide without a real peer.
  private static final class DisplayableTabs extends ElwhaTabs {
    private DisplayableTabs(final TabsVariant variant) {
      super(variant);
    }

    @Override
    public boolean isDisplayable() {
      return true;
    }
  }

  /**
   * Runs the guard; exits non-zero on the first failed check.
   *
   * @param args unused
   * @throws Exception on interrupted waits
   * @version v0.4.0
   * @since v0.4.0
   */
  public static void main(final String[] args) throws Exception {
    System.setProperty("java.awt.headless", "true");
    ElwhaTheme.install(
        ElwhaTheme.config().theme(MaterialPalettes.baseline()).mode(Mode.LIGHT).build());
    MorphAnimator.setReducedMotion(false);

    checkSnapSemantics();
    checkLiveSlide();
    checkRetarget();
    checkReducedMotion();

    System.out.println(
        "ElwhaTabsMotionSmoke: OK (snap semantics, mid-flight x+width interpolation, settle,"
            + " retarget, reduced motion)");
    System.exit(0);
  }

  private static void checkSnapSemantics() {
    final ElwhaTabs bar = laidOut(new ElwhaTabs(TabsVariant.PRIMARY));
    check("initial auto-activation rests on tab 0",
        bar.currentIndicatorRect().equals(bar.indicatorRestRect(bar.getTabAt(0))));

    bar.setActiveTabIndex(2);
    check("non-displayable activation snaps to rest",
        bar.currentIndicatorRect().equals(bar.indicatorRestRect(bar.getTabAt(2))));
  }

  private static void checkLiveSlide() throws Exception {
    final ElwhaTabs bar = laidOut(new DisplayableTabs(TabsVariant.PRIMARY));
    final Rectangle from = bar.indicatorRestRect(bar.getTabAt(0));
    final Rectangle to = bar.indicatorRestRect(bar.getTabAt(2));
    check("setup: unequal spans make width animation observable", from.width != to.width);

    bar.setActiveTabIndex(2);
    final Rectangle mid = sampleMidFlight(bar, from, to);
    check("mid-flight x strictly between endpoints",
        mid != null && between(mid.x, from.x, to.x));
    check("mid-flight width strictly between endpoints",
        mid != null && between(mid.width, from.width, to.width));

    check("slide settles at the destination rest rect", awaitRest(bar, to));
  }

  private static void checkRetarget() throws Exception {
    final ElwhaTabs bar = laidOut(new DisplayableTabs(TabsVariant.SECONDARY));
    final Rectangle from = bar.indicatorRestRect(bar.getTabAt(0));
    final Rectangle far = bar.indicatorRestRect(bar.getTabAt(3));

    bar.setActiveTabIndex(3);
    final Rectangle mid = sampleMidFlight(bar, from, far);
    check("retarget setup reached mid-flight", mid != null);

    bar.setActiveTabIndex(0);
    final Rectangle back = bar.currentIndicatorRect();
    check("retarget starts from the in-flight rect, not a snap to tab 0", back.x > from.x);
    check("retarget settles back at tab 0", awaitRest(bar, from));
  }

  private static void checkReducedMotion() {
    MorphAnimator.setReducedMotion(true);
    try {
      final ElwhaTabs bar = laidOut(new DisplayableTabs(TabsVariant.PRIMARY));
      bar.setActiveTabIndex(2);
      check("reduced motion lands the indicator immediately",
          bar.currentIndicatorRect().equals(bar.indicatorRestRect(bar.getTabAt(2))));
    } finally {
      MorphAnimator.setReducedMotion(false);
    }
  }

  // ----------------------------------------------------------------- helpers

  private static ElwhaTabs laidOut(final ElwhaTabs bar) {
    bar.addTab("Hi");
    bar.addTab("A much longer label");
    bar.addTab("Mid");
    bar.addTab("Tiny");
    bar.setSize(BAR_WIDTH, ElwhaTabs.BAR_HEIGHT_INLINE_PX);
    bar.doLayout();
    return bar;
  }

  // Polls until the indicator detaches from BOTH endpoints (strictly mid-flight), or times out.
  private static Rectangle sampleMidFlight(
      final ElwhaTabs bar, final Rectangle from, final Rectangle to) throws InterruptedException {
    final long deadline = System.currentTimeMillis() + SETTLE_TIMEOUT_MS;
    while (System.currentTimeMillis() < deadline) {
      final Rectangle now = bar.currentIndicatorRect();
      if (!now.equals(from) && !now.equals(to)) {
        return now;
      }
      Thread.sleep(5);
    }
    return null;
  }

  private static boolean awaitRest(final ElwhaTabs bar, final Rectangle rest)
      throws InterruptedException {
    final long deadline = System.currentTimeMillis() + SETTLE_TIMEOUT_MS;
    while (System.currentTimeMillis() < deadline) {
      if (bar.currentIndicatorRect().equals(rest)) {
        return true;
      }
      Thread.sleep(10);
    }
    return false;
  }

  private static boolean between(final int v, final int a, final int b) {
    return v > Math.min(a, b) && v < Math.max(a, b);
  }

  private static void check(final String what, final boolean ok) {
    if (!ok) {
      System.err.println("FAIL: " + what);
      System.exit(1);
    }
    System.out.println("  ok: " + what);
  }
}
