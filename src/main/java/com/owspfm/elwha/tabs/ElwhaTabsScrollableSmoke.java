package com.owspfm.elwha.tabs;

import com.owspfm.elwha.theme.ElwhaTheme;
import com.owspfm.elwha.theme.MaterialPalettes;
import com.owspfm.elwha.theme.Mode;
import com.owspfm.elwha.theme.MorphAnimator;
import java.awt.Rectangle;
import java.awt.event.MouseWheelEvent;

/**
 * S5 headless guard for {@link TabMode#SCROLLABLE} (#430): the 72/264 width clamps + ellipsized
 * spans, offset layout + clamping, wheel scrolling (and FIXED ignoring it), the snap scroll-to
 * formula with its 48&nbsp;px margin, the live 300&nbsp;ms tween (wheel cancels it),
 * activation auto-scroll, active-kept-visible across child mutations, and indicator/scroll
 * composition.
 *
 * @author Charles Bryan
 * @version v0.4.0
 * @since v0.4.0
 */
public final class ElwhaTabsScrollableSmoke {

  private static final int BAR_WIDTH = 300;
  private static final int SETTLE_TIMEOUT_MS = 2000;

  private ElwhaTabsScrollableSmoke() {}

  // Reports displayable so scrollToTab tweens instead of snapping.
  private static final class DisplayableTabs extends ElwhaTabs {
    private DisplayableTabs() {
      super(TabsVariant.PRIMARY);
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

    checkWidthClamps();
    checkOffsetLayoutAndWheel();
    checkSnapScrollTo();
    checkLiveTweenAndCancel();
    checkActivationAutoScroll();
    checkMutationKeepsActiveVisible();

    System.out.println(
        "ElwhaTabsScrollableSmoke: OK (width clamps + ellipsis, offset layout, wheel + FIXED"
            + " ignore, 48px-margin scroll-to, live tween + wheel cancel, activation"
            + " auto-scroll, mutation visibility, indicator composition)");
    System.exit(0);
  }

  private static void checkWidthClamps() {
    final ElwhaTabs bar = new ElwhaTabs(TabsVariant.PRIMARY);
    bar.setTabMode(TabMode.SCROLLABLE);
    final ElwhaTab tiny = bar.addTab("Hi");
    final ElwhaTab huge =
        bar.addTab("An exceptionally long tab label that hits the 264px cap and ellipsizes");
    final ElwhaTab medium = bar.addTab("Section one");
    bar.setSize(BAR_WIDTH, 48);
    bar.doLayout();

    check("tiny tab floors at 72", tiny.getWidth() == 72);
    check("huge tab caps at 264", huge.getWidth() == 264);
    check("medium tab takes its preferred width",
        medium.getWidth() == medium.getPreferredSize().width);
    check("capped label ellipsizes within the padding box",
        huge.contentSpan().width <= 264 - 2 * ElwhaTab.H_PADDING_PX);
  }

  private static void checkOffsetLayoutAndWheel() {
    final ElwhaTabs bar = scrollableBar(new ElwhaTabs(TabsVariant.PRIMARY));
    check("offset 0 puts tab 0 at x=0", bar.getTabAt(0).getX() == 0);

    bar.setScrollOffset(100);
    check("offset shifts tabs left", bar.getTabAt(0).getX() == -100);

    bar.setScrollOffset(99999);
    final int maxOffset = bar.getScrollOffset();
    check("offset clamps to content", maxOffset > 0 && maxOffset < 99999);
    final ElwhaTab lastTab = bar.getTabAt(bar.getTabCount() - 1);
    check("at max offset the last tab's trailing edge meets the bar edge",
        lastTab.getX() + lastTab.getWidth() == BAR_WIDTH);

    bar.setScrollOffset(0);
    wheel(bar, 3);
    check("wheel scrolls forward", bar.getScrollOffset() == 90);
    wheel(bar, -9999);
    check("wheel clamps at 0", bar.getScrollOffset() == 0);

    final Rectangle indicatorAt0 = bar.currentIndicatorRect();
    bar.setScrollOffset(50);
    final Rectangle indicatorAt50 = bar.currentIndicatorRect();
    check("indicator translates with the scroll offset",
        indicatorAt50.x == indicatorAt0.x - 50
            && indicatorAt50.width == indicatorAt0.width);
    bar.setScrollOffset(0);

    final ElwhaTabs fixed = new ElwhaTabs(TabsVariant.PRIMARY);
    fixed.addTab("One");
    fixed.addTab("Two");
    fixed.setSize(BAR_WIDTH, 48);
    fixed.doLayout();
    wheel(fixed, 3);
    check("FIXED mode ignores the wheel", fixed.getScrollOffset() == 0);
  }

  private static void checkSnapScrollTo() {
    final ElwhaTabs bar = scrollableBar(new ElwhaTabs(TabsVariant.PRIMARY));
    final ElwhaTab last = bar.getTabAt(bar.getTabCount() - 1);
    bar.scrollToTab(last);
    check("snap scroll-to brings the last tab fully into view",
        last.getX() >= 0 && last.getX() + last.getWidth() <= BAR_WIDTH);
    check("snap scroll-to keeps the 48px margin where content allows",
        last.getX() + last.getWidth() <= BAR_WIDTH - Math.min(48,
            bar.getScrollOffset() == 0 ? 0 : 48)
            || last.getX() + last.getWidth() == BAR_WIDTH);

    final ElwhaTab first = bar.getTabAt(0);
    bar.scrollToTab(first);
    check("snap scroll-to back to the first tab returns to offset 0",
        bar.getScrollOffset() == 0);
  }

  private static void checkLiveTweenAndCancel() throws Exception {
    final ElwhaTabs bar = scrollableBar(new DisplayableTabs());
    final ElwhaTab last = bar.getTabAt(bar.getTabCount() - 1);

    bar.scrollToTab(last);
    final Integer mid = sampleMidScroll(bar, 0);
    check("scroll tween passes through a mid offset", mid != null && mid > 0);
    check("scroll tween settles with the last tab visible", awaitVisible(bar, last));

    bar.scrollToTab(bar.getTabAt(0));
    Thread.sleep(40);
    final int beforeWheel = bar.getScrollOffset();
    wheel(bar, -1);
    final int afterWheel = bar.getScrollOffset();
    Thread.sleep(80);
    check("wheel cancels the in-flight tween",
        bar.getScrollOffset() == afterWheel && afterWheel == Math.max(0, beforeWheel - 30));
  }

  private static void checkActivationAutoScroll() throws Exception {
    final ElwhaTabs bar = scrollableBar(new DisplayableTabs());
    final int lastIndex = bar.getTabCount() - 1;
    bar.setActiveTabIndex(lastIndex);
    check("activation auto-scrolls the active tab into view",
        awaitVisible(bar, bar.getTabAt(lastIndex)));
  }

  private static void checkMutationKeepsActiveVisible() {
    final ElwhaTabs bar = scrollableBar(new ElwhaTabs(TabsVariant.PRIMARY));
    final int lastIndex = bar.getTabCount() - 1;
    bar.setActiveTabIndex(lastIndex);
    final ElwhaTab active = bar.getTabAt(lastIndex);
    check("setup: active tab visible at max scroll",
        active.getX() >= 0 && active.getX() + active.getWidth() <= BAR_WIDTH);

    bar.removeTab(bar.getTabAt(0));
    check("removing a leading tab keeps the active tab visible",
        active.getX() >= 0 && active.getX() + active.getWidth() <= BAR_WIDTH);

    bar.addTab("Appended");
    check("appending keeps the active tab visible",
        active.getX() >= 0 && active.getX() + active.getWidth() <= BAR_WIDTH);
  }

  // ----------------------------------------------------------------- helpers

  private static ElwhaTabs scrollableBar(final ElwhaTabs bar) {
    bar.setTabMode(TabMode.SCROLLABLE);
    for (int i = 1; i <= 8; i++) {
      bar.addTab("Section " + i);
    }
    bar.setSize(BAR_WIDTH, 48);
    bar.doLayout();
    return bar;
  }

  private static void wheel(final ElwhaTabs bar, final int rotation) {
    bar.dispatchEvent(
        new MouseWheelEvent(
            bar,
            MouseWheelEvent.MOUSE_WHEEL,
            System.currentTimeMillis(),
            0,
            10,
            10,
            0,
            false,
            MouseWheelEvent.WHEEL_UNIT_SCROLL,
            1,
            rotation));
  }

  private static Integer sampleMidScroll(final ElwhaTabs bar, final int from)
      throws InterruptedException {
    final long deadline = System.currentTimeMillis() + SETTLE_TIMEOUT_MS;
    while (System.currentTimeMillis() < deadline) {
      final int now = bar.getScrollOffset();
      if (now != from) {
        return now;
      }
      Thread.sleep(5);
    }
    return null;
  }

  private static boolean awaitVisible(final ElwhaTabs bar, final ElwhaTab tab)
      throws InterruptedException {
    final long deadline = System.currentTimeMillis() + SETTLE_TIMEOUT_MS;
    while (System.currentTimeMillis() < deadline) {
      if (tab.getX() >= 0 && tab.getX() + tab.getWidth() <= BAR_WIDTH) {
        return true;
      }
      Thread.sleep(10);
    }
    return false;
  }

  private static void check(final String what, final boolean ok) {
    if (!ok) {
      System.err.println("FAIL: " + what);
      System.exit(1);
    }
    System.out.println("  ok: " + what);
  }
}
