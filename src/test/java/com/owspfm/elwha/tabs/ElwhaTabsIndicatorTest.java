package com.owspfm.elwha.tabs;

import static org.assertj.core.api.Assertions.assertThat;

import com.owspfm.elwha.testkit.EdtInterceptor;
import com.owspfm.elwha.testkit.PaintLog;
import com.owspfm.elwha.testkit.Pixels;
import com.owspfm.elwha.testkit.ThemeExtension;
import com.owspfm.elwha.theme.ColorRole;
import com.owspfm.elwha.theme.MorphAnimator;
import java.awt.ComponentOrientation;
import java.awt.Rectangle;
import java.awt.geom.Rectangle2D;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

/**
 * Tier A coverage of the single, container-owned active indicator — design doc §2 (one indicator,
 * owned by the bar and painted after its children so it always sits on top), §3–§4 (the per-variant
 * indicator geometry), and §6 (where a retargeted slide ends up).
 *
 * <p><strong>What is and is not asserted about motion.</strong> The indicator's slide is a {@code
 * MorphAnimator} tween whose easing and progress ramp are covered by that class's own suite in
 * {@code theme/}. Here the bar's side of the contract is checked: the endpoints it feeds the tween
 * (each tab's at-rest rect, per variant and orientation) and where a slide — including one
 * retargeted mid-flight — comes to rest. Reduced motion is pinned by the harness, so no assertion
 * below observes a tween in progress; the mid-slide interpolation itself is not reachable from this
 * tier without opening a second production seam, which was not taken.
 */
@ExtendWith({EdtInterceptor.class, ThemeExtension.class})
class ElwhaTabsIndicatorTest {

  private static final int BAR_WIDTH = 400;

  private static ElwhaTabs laidOut(final ElwhaTabs bar) {
    bar.setSize(BAR_WIDTH, bar.getPreferredSize().height);
    bar.doLayout();
    return bar;
  }

  private static ElwhaTabs barOf(final TabsVariant variant, final String... labels) {
    final ElwhaTabs bar = new ElwhaTabs(variant);
    for (final String label : labels) {
      bar.addTab(label);
    }
    return laidOut(bar);
  }

  // ------------------------------------------------------------ rest geometry

  @Test
  void aPrimaryIndicatorHugsTheTabsContent() {
    final ElwhaTabs bar = barOf(TabsVariant.PRIMARY, "One", "Two", "Three");
    final ElwhaTab tab = bar.getTabAt(1);

    final Rectangle indicator = bar.indicatorRestRect(tab);
    final Rectangle span = tab.contentSpan();

    assertThat(indicator.x)
        .as("§3 — a primary indicator spans the content cluster, not the whole cell")
        .isEqualTo(tab.getX() + span.x);
    assertThat(indicator.width).isEqualTo(span.width);
    assertThat(indicator.width)
        .as("and the cell really is wider than its content, so this is a live distinction")
        .isLessThan(tab.getWidth());
  }

  @Test
  void aSecondaryIndicatorSpansTheWholeTab() {
    final ElwhaTabs bar = barOf(TabsVariant.SECONDARY, "One", "Two", "Three");
    final ElwhaTab tab = bar.getTabAt(1);

    final Rectangle indicator = bar.indicatorRestRect(tab);

    assertThat(indicator.x)
        .as("§4 — a secondary indicator is the full tab width")
        .isEqualTo(tab.getX());
    assertThat(indicator.width).isEqualTo(tab.getWidth());
  }

  @Test
  void perVariantIndicatorHeightsMatchTheSpec() {
    final ElwhaTabs primary = barOf(TabsVariant.PRIMARY, "One", "Two");
    final ElwhaTabs secondary = barOf(TabsVariant.SECONDARY, "One", "Two");

    assertThat(primary.indicatorRestRect(primary.getTabAt(0)).height)
        .as("§3 — primary indicators are 3 dp tall")
        .isEqualTo(ElwhaTabs.PRIMARY_INDICATOR_HEIGHT_PX);
    assertThat(secondary.indicatorRestRect(secondary.getTabAt(0)).height)
        .as("§4 — secondary indicators are 2 dp")
        .isEqualTo(ElwhaTabs.SECONDARY_INDICATOR_HEIGHT_PX);
  }

  @Test
  void indicatorSitsOnTheBarsBottomEdge() {
    final ElwhaTabs bar = barOf(TabsVariant.PRIMARY, "One", "Two");

    final Rectangle indicator = bar.indicatorRestRect(bar.getTabAt(0));

    assertThat(indicator.y + indicator.height)
        .as("§3 — the indicator is bottom-aligned, sitting over the divider")
        .isEqualTo(bar.getHeight());
  }

  @Test
  void eachTabHasItsOwnIndicatorSlot() {
    final ElwhaTabs bar = barOf(TabsVariant.SECONDARY, "One", "Two", "Three");

    final Rectangle first = bar.indicatorRestRect(bar.getTabAt(0));
    final Rectangle second = bar.indicatorRestRect(bar.getTabAt(1));

    assertThat(second.x)
        .as("the slots march across the bar with the tabs")
        .isEqualTo(first.x + first.width);
  }

  @Test
  void indicatorSlotsMirrorUnderRightToLeft() {
    final ElwhaTabs bar = new ElwhaTabs(TabsVariant.SECONDARY);
    bar.addTab("One");
    bar.addTab("Two");
    bar.addTab("Three");
    bar.applyComponentOrientation(ComponentOrientation.RIGHT_TO_LEFT);
    laidOut(bar);

    final Rectangle first = bar.indicatorRestRect(bar.getTabAt(0));

    assertThat(first.x + first.width)
        .as("§3 — the first tab's indicator is at the right edge once the bar mirrors")
        .isEqualTo(BAR_WIDTH);
  }

  // ---------------------------------------------------------- current rect

  @Test
  void currentRectIsTheActiveTabsRestRectWhenNothingIsMoving() {
    final ElwhaTabs bar = barOf(TabsVariant.PRIMARY, "One", "Two", "Three");
    bar.setActiveTabIndex(2);

    assertThat(bar.currentIndicatorRect())
        .as("§6 — at rest the indicator is exactly where its tab's slot says")
        .isEqualTo(bar.indicatorRestRect(bar.getTabAt(2)));
  }

  @Test
  void anEmptyBarHasNoIndicatorRect() {
    assertThat(laidOut(new ElwhaTabs()).currentIndicatorRect())
        .as("§2 — no active tab, no indicator; the paint short-circuits rather than guessing")
        .isNull();
  }

  @Test
  void activatingATabMovesTheIndicatorToIt() {
    final ElwhaTabs bar = barOf(TabsVariant.SECONDARY, "One", "Two", "Three");
    final Rectangle atFirst = bar.currentIndicatorRect();

    bar.setActiveTabIndex(2);

    assertThat(bar.currentIndicatorRect())
        .as("§6 — the indicator follows the selection")
        .isNotEqualTo(atFirst);
    assertThat(bar.currentIndicatorRect()).isEqualTo(bar.indicatorRestRect(bar.getTabAt(2)));
  }

  @Test
  void aRetargetedSlideSettlesOnTheLastTabActivated() {
    final ElwhaTabs bar = barOf(TabsVariant.SECONDARY, "One", "Two", "Three", "Four");

    bar.setActiveTabIndex(1);
    bar.setActiveTabIndex(3);
    bar.setActiveTabIndex(2);

    assertThat(bar.currentIndicatorRect())
        .as("§6 — a slide retargeted mid-flight ends on the newest target, never a stale one")
        .isEqualTo(bar.indicatorRestRect(bar.getTabAt(2)));
  }

  @Test
  void indicatorFollowsAResizeOfTheBar() {
    final ElwhaTabs bar = barOf(TabsVariant.SECONDARY, "One", "Two", "Three");
    bar.setActiveTabIndex(1);
    final Rectangle before = bar.currentIndicatorRect();

    bar.setSize(800, bar.getHeight());
    bar.doLayout();

    assertThat(bar.currentIndicatorRect().width)
        .as("§6 — the rest rect is derived from live tab bounds, so a relayout carries it along")
        .isGreaterThan(before.width);
  }

  @Test
  void reducedMotionIsPinnedSoNoAssertionHereRacesATween() {
    assertThat(MorphAnimator.isReducedMotion())
        .as("the harness guarantees the slide resolves immediately")
        .isTrue();
  }

  // -------------------------------------------------------- the overlay lock

  @Test
  void indicatorPaintsAfterEveryTab() {
    final ElwhaTabs bar = barOf(TabsVariant.SECONDARY, "One", "Two", "Three");
    bar.setActiveTabIndex(1);
    final Rectangle indicator = bar.currentIndicatorRect();

    final PaintLog log = PaintLog.capture(bar, BAR_WIDTH, bar.getPreferredSize().height);
    final List<PaintLog.Painted> shapes = log.shapes();

    int lastTabPaint = -1;
    int indicatorPaint = -1;
    for (int i = 0; i < shapes.size(); i++) {
      final Rectangle2D bounds = shapes.get(i).bounds();
      if (bounds.getHeight() >= bar.getHeight() - 4 && bounds.getWidth() < BAR_WIDTH) {
        lastTabPaint = i;
      }
      if (Math.abs(bounds.getY() - indicator.y) < 1.5
          && Math.abs(bounds.getWidth() - indicator.width) < 2) {
        indicatorPaint = i;
      }
    }

    assertThat(indicatorPaint)
        .as(
            "§2 — the indicator is painted from paintChildren, after super, so it is never "
                + "overdrawn by a tab's own chrome")
        .isGreaterThan(lastTabPaint);
  }

  @Test
  void indicatorIsPaintedInThePrimaryRole() {
    final ElwhaTabs bar = barOf(TabsVariant.SECONDARY, "One", "Two", "Three");
    bar.setActiveTabIndex(0);
    final Rectangle indicator = bar.currentIndicatorRect();

    Pixels.assertPixelNear(
        Pixels.render(bar, BAR_WIDTH, bar.getPreferredSize().height),
        indicator.x + indicator.width / 2,
        indicator.y + indicator.height / 2,
        ColorRole.PRIMARY.resolve(),
        "§3 — the active indicator carries the primary role in both variants");
  }

  @Test
  void onlyOneIndicatorIsEverPainted() {
    final ElwhaTabs bar = barOf(TabsVariant.SECONDARY, "One", "Two", "Three");
    bar.setActiveTabIndex(1);
    final Rectangle indicator = bar.currentIndicatorRect();
    final java.awt.image.BufferedImage image =
        Pixels.render(bar, BAR_WIDTH, bar.getPreferredSize().height);
    final java.awt.Color primary = ColorRole.PRIMARY.resolve();
    final int probeY = indicator.y + indicator.height / 2;

    int runs = 0;
    boolean inside = false;
    for (int x = 0; x < BAR_WIDTH; x++) {
      final java.awt.Color got = new java.awt.Color(image.getRGB(x, probeY), true);
      final boolean hit =
          Math.abs(got.getRed() - primary.getRed()) <= 10
              && Math.abs(got.getGreen() - primary.getGreen()) <= 10
              && Math.abs(got.getBlue() - primary.getBlue()) <= 10;
      if (hit && !inside) {
        runs++;
      }
      inside = hit;
    }

    assertThat(runs)
        .as("§2 — the architecture lock: one indicator owned by the bar, not one per tab")
        .isEqualTo(1);
  }

  // ------------------------------------------------------------- the divider

  @Test
  void dividerRunsTheFullBarWidth() {
    final ElwhaTabs bar = barOf(TabsVariant.PRIMARY, "One", "Two", "Three");
    bar.setActiveTabIndex(1);
    final java.awt.image.BufferedImage image =
        Pixels.render(bar, BAR_WIDTH, bar.getPreferredSize().height);

    // Probed away from the active tab, where the indicator would otherwise cover the divider.
    Pixels.assertPixelNear(
        image,
        4,
        bar.getHeight() - 1,
        ColorRole.OUTLINE_VARIANT.resolve(),
        "§4 — the divider separates the bar from the content below it, across the whole width");
  }

  @Test
  void indicatorSitsOnTopOfTheDivider() {
    final ElwhaTabs bar = barOf(TabsVariant.SECONDARY, "One", "Two", "Three");
    bar.setActiveTabIndex(0);
    final Rectangle indicator = bar.currentIndicatorRect();

    Pixels.assertPixelNear(
        Pixels.render(bar, BAR_WIDTH, bar.getPreferredSize().height),
        indicator.x + indicator.width / 2,
        bar.getHeight() - 1,
        ColorRole.PRIMARY.resolve(),
        "§4 — where they overlap the indicator wins, because it paints last");
  }
}
