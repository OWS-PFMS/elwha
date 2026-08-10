package com.owspfm.elwha.tabs;

import static org.assertj.core.api.Assertions.assertThat;

import com.owspfm.elwha.icons.MaterialIcons;
import com.owspfm.elwha.testkit.EdtInterceptor;
import com.owspfm.elwha.testkit.ThemeExtension;
import java.awt.ComponentOrientation;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;

/**
 * Tier A coverage of the tab bar's two layout modes — design doc §5/§7: {@link TabMode#FIXED}
 * splits the bar into equal shares, {@link TabMode#SCROLLABLE} lays tabs out at their content
 * widths clamped to the M3 72–264 px range and scrolls the overflow, and both mirror under RTL.
 */
@ExtendWith({EdtInterceptor.class, ThemeExtension.class})
class ElwhaTabsLayoutTest {

  private static final int BAR_WIDTH = 300;

  private static ElwhaTabs barOf(final String... labels) {
    final ElwhaTabs bar = new ElwhaTabs();
    for (final String label : labels) {
      bar.addTab(label);
    }
    return bar;
  }

  private static ElwhaTabs laidOut(final ElwhaTabs bar, final int width) {
    bar.setSize(width, bar.getPreferredSize().height);
    bar.doLayout();
    return bar;
  }

  // ------------------------------------------------------------------ FIXED

  @Test
  void fixedIsTheDefaultMode() {
    assertThat(new ElwhaTabs().getTabMode())
        .as("§5 — a bar shows all its tabs until told otherwise")
        .isEqualTo(TabMode.FIXED);
  }

  @Test
  void fixedTabsSplitTheBarEvenly() {
    final ElwhaTabs bar = laidOut(barOf("One", "Two", "Three", "Four"), 400);

    for (int i = 0; i < bar.getTabCount(); i++) {
      assertThat(bar.getTabAt(i).getWidth())
          .as("§5 — every fixed tab gets an equal share of the bar, tab %d", i)
          .isEqualTo(100);
    }
  }

  @Test
  void fixedTabsSpreadTheRemainderAcrossTheLeadingTabs() {
    final ElwhaTabs bar = laidOut(barOf("One", "Two", "Three", "Four"), 402);

    assertThat(bar.getTabAt(0).getWidth())
        .as("§5 — a width that does not divide evenly gives the spare pixels to the first tabs")
        .isEqualTo(101);
    assertThat(bar.getTabAt(1).getWidth()).isEqualTo(101);
    assertThat(bar.getTabAt(2).getWidth()).isEqualTo(100);
    assertThat(bar.getTabAt(3).getWidth()).isEqualTo(100);
  }

  @Test
  void fixedTabsTileTheBarWithNoGapsOrOverlap() {
    final ElwhaTabs bar = laidOut(barOf("One", "Two", "Three", "Four", "Five"), 403);

    int expectedX = 0;
    for (int i = 0; i < bar.getTabCount(); i++) {
      assertThat(bar.getTabAt(i).getX())
          .as("§5 — tab %d starts exactly where its predecessor ended", i)
          .isEqualTo(expectedX);
      expectedX += bar.getTabAt(i).getWidth();
    }
    assertThat(expectedX).as("§5 — and together they fill the bar exactly").isEqualTo(403);
  }

  @Test
  void fixedTabsAreBottomAlignedAgainstTheDivider() {
    final ElwhaTabs bar = laidOut(barOf("One", "Two"), BAR_WIDTH);

    for (int i = 0; i < bar.getTabCount(); i++) {
      final ElwhaTab tab = bar.getTabAt(i);
      assertThat(tab.getY() + tab.getHeight())
          .as("§4 — tab content sits on the bar's bottom edge, where the indicator meets it")
          .isEqualTo(bar.getHeight());
    }
  }

  // ------------------------------------------------------------- SCROLLABLE

  @Test
  void scrollableTabsTakeTheirContentWidth() {
    final ElwhaTabs bar = new ElwhaTabs();
    bar.setTabMode(TabMode.SCROLLABLE);
    final ElwhaTab narrow = bar.addTab("A very much longer tab label here");
    laidOut(bar, BAR_WIDTH);

    assertThat(narrow.getWidth())
        .as("§5 — scrollable tabs are sized by their content, not by the bar")
        .isEqualTo(narrow.getPreferredSize().width);
  }

  @Test
  void aShortScrollableTabIsWidenedToTheMinimum() {
    final ElwhaTabs bar = new ElwhaTabs();
    bar.setTabMode(TabMode.SCROLLABLE);
    final ElwhaTab tiny = bar.addTab("A");
    laidOut(bar, BAR_WIDTH);

    assertThat(tiny.getPreferredSize().width)
        .as("the fixture's tab really is narrower than the M3 floor")
        .isLessThan(ElwhaTabs.SCROLLABLE_MIN_TAB_WIDTH_PX);
    assertThat(tiny.getWidth())
        .as("§5 — the M3 72 px floor keeps a one-letter tab tappable")
        .isEqualTo(ElwhaTabs.SCROLLABLE_MIN_TAB_WIDTH_PX);
  }

  @Test
  void aVeryLongScrollableTabIsCappedAtTheMaximum() {
    final ElwhaTabs bar = new ElwhaTabs();
    bar.setTabMode(TabMode.SCROLLABLE);
    final ElwhaTab huge =
        bar.addTab("An extremely long tab label that runs on well past any reasonable width");
    laidOut(bar, 900);

    assertThat(huge.getWidth())
        .as("§5 — the M3 264 px ceiling truncates rather than letting one tab own the strip")
        .isEqualTo(ElwhaTabs.SCROLLABLE_MAX_TAB_WIDTH_PX);
  }

  @Test
  void scrollableTabsRunPastTheBarEdge() {
    final ElwhaTabs bar = new ElwhaTabs();
    bar.setTabMode(TabMode.SCROLLABLE);
    for (int i = 0; i < 8; i++) {
      bar.addTab("Section " + i);
    }
    laidOut(bar, BAR_WIDTH);

    final ElwhaTab last = bar.getTabAt(7);
    assertThat(last.getX() + last.getWidth())
        .as("§5 — overflow is the point of the mode; the strip is wider than the bar")
        .isGreaterThan(BAR_WIDTH);
  }

  @Test
  void scrollingShiftsEveryTabByTheSameOffset() {
    final ElwhaTabs bar = new ElwhaTabs();
    bar.setTabMode(TabMode.SCROLLABLE);
    for (int i = 0; i < 8; i++) {
      bar.addTab("Section " + i);
    }
    laidOut(bar, BAR_WIDTH);
    final int firstBefore = bar.getTabAt(0).getX();
    final int lastBefore = bar.getTabAt(7).getX();

    bar.setScrollOffset(120);

    assertThat(bar.getScrollOffset()).isEqualTo(120);
    assertThat(bar.getTabAt(0).getX())
        .as("§5 — scrolling translates the whole strip, it does not relayout it")
        .isEqualTo(firstBefore - 120);
    assertThat(bar.getTabAt(7).getX()).isEqualTo(lastBefore - 120);
  }

  @Test
  void scrollOffsetIsClampedToTheStripsTravel() {
    final ElwhaTabs bar = new ElwhaTabs();
    bar.setTabMode(TabMode.SCROLLABLE);
    for (int i = 0; i < 8; i++) {
      bar.addTab("Section " + i);
    }
    laidOut(bar, BAR_WIDTH);

    bar.setScrollOffset(-500);
    assertThat(bar.getScrollOffset())
        .as("§5 — the strip cannot scroll before its first tab")
        .isZero();

    bar.setScrollOffset(100_000);
    final ElwhaTab last = bar.getTabAt(7);
    assertThat(last.getX() + last.getWidth())
        .as("§5 — nor past its last, which lands flush with the bar's trailing edge")
        .isEqualTo(BAR_WIDTH);
  }

  @Test
  void aStripThatFitsCannotScrollAtAll() {
    final ElwhaTabs bar = new ElwhaTabs();
    bar.setTabMode(TabMode.SCROLLABLE);
    bar.addTab("One");
    bar.addTab("Two");
    laidOut(bar, 900);

    bar.setScrollOffset(200);

    assertThat(bar.getScrollOffset())
        .as("§5 — with no overflow there is no travel, so the offset stays pinned at zero")
        .isZero();
  }

  @Test
  void scrollToTabBringsAnOffscreenTabIntoViewWithItsMargin() {
    final ElwhaTabs bar = new ElwhaTabs();
    bar.setTabMode(TabMode.SCROLLABLE);
    for (int i = 0; i < 8; i++) {
      bar.addTab("Section " + i);
    }
    laidOut(bar, BAR_WIDTH);
    final ElwhaTab target = bar.getTabAt(6);

    bar.scrollToTab(target);
    bar.doLayout();

    assertThat(target.getX())
        .as("§7 — the requested tab lands inside the bar")
        .isGreaterThanOrEqualTo(0);
    assertThat(target.getX() + target.getWidth())
        .as("§7 — fully, not clipped at the trailing edge")
        .isLessThanOrEqualTo(BAR_WIDTH);
  }

  @Test
  void scrollToTabLeavesTheNeighbourMarginVisible() {
    final ElwhaTabs bar = new ElwhaTabs();
    bar.setTabMode(TabMode.SCROLLABLE);
    for (int i = 0; i < 8; i++) {
      bar.addTab("Section " + i);
    }
    laidOut(bar, BAR_WIDTH);

    bar.scrollToTab(bar.getTabAt(4));
    bar.doLayout();

    final ElwhaTab target = bar.getTabAt(4);
    assertThat(target.getX())
        .as(
            "§7 — the material-web formula keeps %d px of the previous tab peeking, so the strip "
                + "reads as scrollable",
            ElwhaTabs.SCROLL_MARGIN_PX)
        .isGreaterThanOrEqualTo(ElwhaTabs.SCROLL_MARGIN_PX - 1);
  }

  @Test
  void scrollToTabIsANoOpInFixedMode() {
    final ElwhaTabs bar = laidOut(barOf("One", "Two", "Three"), BAR_WIDTH);

    bar.scrollToTab(bar.getTabAt(2));

    assertThat(bar.getScrollOffset())
        .as("§5 — fixed bars show everything already; there is nothing to scroll to")
        .isZero();
  }

  @Test
  void switchingToScrollableRelayoutsAtContentWidths() {
    final ElwhaTabs bar = laidOut(barOf("One", "Two", "Three"), 900);
    final int fixedWidth = bar.getTabAt(0).getWidth();

    bar.setTabMode(TabMode.SCROLLABLE);
    bar.doLayout();

    assertThat(bar.getTabAt(0).getWidth())
        .as("§5 — the mode is a live switch, not a construction-time choice")
        .isLessThan(fixedWidth);
  }

  // -------------------------------------------------------------------- RTL

  @ParameterizedTest
  @EnumSource(TabMode.class)
  void rightToLeftLaysTabsOutFromTheRightEdge(final TabMode mode) {
    final ElwhaTabs bar = barOf("One", "Two", "Three");
    bar.setTabMode(mode);
    bar.applyComponentOrientation(ComponentOrientation.RIGHT_TO_LEFT);
    laidOut(bar, BAR_WIDTH);

    assertThat(bar.getTabAt(0).getX() + bar.getTabAt(0).getWidth())
        .as("§5 — the first tab is the rightmost one under RTL, in %s mode", mode)
        .isEqualTo(BAR_WIDTH);
    assertThat(bar.getTabAt(2).getX())
        .as("and the last one sits furthest left")
        .isLessThan(bar.getTabAt(0).getX());
  }

  @Test
  void rightToLeftFixedTabsStillTileTheBarExactly() {
    final ElwhaTabs bar = barOf("One", "Two", "Three", "Four");
    bar.applyComponentOrientation(ComponentOrientation.RIGHT_TO_LEFT);
    laidOut(bar, 403);

    int expectedRight = 403;
    for (int i = 0; i < bar.getTabCount(); i++) {
      assertThat(bar.getTabAt(i).getX() + bar.getTabAt(i).getWidth())
          .as("§5 — mirrored, tab %d still butts against its predecessor", i)
          .isEqualTo(expectedRight);
      expectedRight -= bar.getTabAt(i).getWidth();
    }
    assertThat(expectedRight).isZero();
  }

  // --------------------------------------------------------- preferred size

  @Test
  void barIsAtLeastTheM3InlineHeight() {
    assertThat(barOf("One").getPreferredSize().height)
        .as("§4 — the M3 inline tab-bar height")
        .isEqualTo(ElwhaTabs.BAR_HEIGHT_INLINE_PX);
  }

  @Test
  void aStackedTabRaisesTheBarHeight() {
    final ElwhaTabs bar = new ElwhaTabs();
    bar.addTab("Flat");
    bar.addTab(ElwhaTab.of(MaterialIcons.symbol("home"), "Home"));

    assertThat(bar.getPreferredSize().height)
        .as("§4 — the tallest tab sets the bar height, so a stacked icon+label tab grows it")
        .isEqualTo(ElwhaTab.STACKED_CONTENT_HEIGHT_PX);
  }

  @Test
  void aFixedBarAsksForItsWidestTabTimesTheTabCount() {
    final ElwhaTabs bar = barOf("Overview", "Specs", "Reviews");

    int widest = 0;
    for (int i = 0; i < bar.getTabCount(); i++) {
      widest = Math.max(widest, bar.getTabAt(i).getPreferredSize().width);
    }

    assertThat(bar.getPreferredSize().width)
        .as("§5 / #766 — equal-slice layout means the widest label governs every slice")
        .isEqualTo(widest * bar.getTabCount());
  }

  @Test
  void aScrollableBarAsksForTheSumOfItsClampedTabWidths() {
    final ElwhaTabs bar = barOf("Overview", "Specs", "Reviews");
    bar.setTabMode(TabMode.SCROLLABLE);

    int sum = 0;
    for (int i = 0; i < bar.getTabCount(); i++) {
      sum +=
          Math.max(
              ElwhaTabs.SCROLLABLE_MIN_TAB_WIDTH_PX,
              Math.min(
                  bar.getTabAt(i).getPreferredSize().width, ElwhaTabs.SCROLLABLE_MAX_TAB_WIDTH_PX));
    }

    assertThat(bar.getPreferredSize().width)
        .as("§5 — a scrollable bar's natural width is the sum of its content-sized tabs")
        .isEqualTo(sum);
  }

  @Test
  void fixedTabsAtExactlyPreferredSizeGiveEveryLabelItsNaturalWidth() {
    final ElwhaTabs bar = barOf("Overview", "Specs", "Reviews");

    laidOut(bar, bar.getPreferredSize().width);

    for (int i = 0; i < bar.getTabCount(); i++) {
      assertThat(bar.getTabAt(i).getWidth())
          .as(
              "#766 — at exactly preferred size no fixed tab is shorted below its content, tab %d",
              i)
          .isGreaterThanOrEqualTo(bar.getTabAt(i).getPreferredSize().width);
    }
  }

  @Test
  void fixedTabsAtExactlyPreferredSizeGiveEveryLabelItsNaturalWidthInRtl() {
    final ElwhaTabs bar = barOf("Overview", "Specs", "Reviews");
    bar.applyComponentOrientation(ComponentOrientation.RIGHT_TO_LEFT);

    laidOut(bar, bar.getPreferredSize().width);

    for (int i = 0; i < bar.getTabCount(); i++) {
      assertThat(bar.getTabAt(i).getWidth())
          .as("#766 — the RTL layout branch honors the same floor, tab %d", i)
          .isGreaterThanOrEqualTo(bar.getTabAt(i).getPreferredSize().width);
    }
  }

  @Test
  void anEmptyBarLaysOutWithoutComplaint() {
    final ElwhaTabs bar = new ElwhaTabs();

    laidOut(bar, BAR_WIDTH);

    assertThat(bar.getTabCount()).isZero();
    assertThat(bar.getPreferredSize().height).isEqualTo(ElwhaTabs.BAR_HEIGHT_INLINE_PX);
  }
}
