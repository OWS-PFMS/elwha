package com.owspfm.elwha.appbar;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.within;

import com.owspfm.elwha.testkit.EdtInterceptor;
import com.owspfm.elwha.testkit.Pixels;
import com.owspfm.elwha.testkit.ThemeExtension;
import com.owspfm.elwha.theme.ColorRole;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;

/**
 * Tier A coverage of the app bar's scroll-driven behaviours — design doc §8: the lift (a container
 * re-role once the content has moved at all) and the flexible variants' collapse (a height ramp
 * over the difference between the expanded token height and the 64 dp strip).
 *
 * <p>Both are driven through a <strong>real, laid-out {@link javax.swing.JScrollPane}</strong>
 * ({@link ScrollFixture}) rather than by writing values at a bare scroll bar. A scroll bar with no
 * view behind it has an empty range, so a written value is clamped straight back to zero and the
 * binding never fires — the shortcut looks like it works and tests nothing.
 */
@ExtendWith({EdtInterceptor.class, ThemeExtension.class})
class ElwhaAppBarScrollTest {

  private static final int STRIP = ElwhaAppBar.STRIP_HEIGHT_PX;

  private static int collapseRange(final AppBarVariant variant, final boolean withSubtitle) {
    return variant.expandedHeightPx(withSubtitle) - STRIP;
  }

  // ------------------------------------------------------------- the binding

  @Test
  void barReportsTheSourceItWasGiven() {
    final ScrollFixture scroll = ScrollFixture.create();
    final ElwhaAppBar bar = ElwhaAppBar.small();

    bar.setScrollSource(scroll.pane());

    assertThat(bar.getScrollSource()).isSameAs(scroll.pane());
  }

  @Test
  void fixtureProducesARealScrollRange() {
    final ScrollFixture scroll = ScrollFixture.create();

    assertThat(scroll.scrollTo(200))
        .as("the whole point of the fixture: a written scroll position actually sticks")
        .isEqualTo(200);
    assertThat(scroll.scrollTo(1_000_000))
        .as("and is clamped by the real content extent, not by an empty range")
        .isEqualTo(ScrollFixture.VIEW_HEIGHT - scroll.pane().getViewport().getExtentSize().height);
  }

  // -------------------------------------------------------------------- lift

  @Test
  void aBarAtRestIsNotLifted() {
    final ScrollFixture scroll = ScrollFixture.create();
    final ElwhaAppBar bar = ElwhaAppBar.small();
    bar.setLiftOnScroll(true);

    bar.setScrollSource(scroll.pane());

    assertThat(bar.isLifted())
        .as("§8 — content flush with the top of its pane needs no separation")
        .isFalse();
  }

  @Test
  void anyScrollAtAllLiftsTheBar() {
    final ScrollFixture scroll = ScrollFixture.create();
    final ElwhaAppBar bar = ElwhaAppBar.small();
    bar.setLiftOnScroll(true);
    bar.setScrollSource(scroll.pane());

    scroll.scrollTo(1);

    assertThat(bar.isLifted())
        .as("§8 — the lift is a boolean 'content has moved', not a ramp")
        .isTrue();
  }

  @Test
  void scrollingBackToTheTopDropsTheLift() {
    final ScrollFixture scroll = ScrollFixture.create();
    final ElwhaAppBar bar = ElwhaAppBar.small();
    bar.setLiftOnScroll(true);
    bar.setScrollSource(scroll.pane());
    scroll.scrollTo(300);

    scroll.scrollTo(0);

    assertThat(bar.isLifted()).as("§8 — and it is reversible").isFalse();
  }

  @Test
  void aBarWithLiftDisabledStaysFlatHoweverFarTheContentScrolls() {
    final ScrollFixture scroll = ScrollFixture.create();
    final ElwhaAppBar bar = ElwhaAppBar.small();
    bar.setLiftOnScroll(false);
    bar.setScrollSource(scroll.pane());

    scroll.scrollTo(900);

    assertThat(bar.isLifted())
        .as("§8 — the lift is opt-in; a consumer who declines it gets no re-role")
        .isFalse();
  }

  @Test
  void liftingReRolesTheContainer() {
    final ElwhaAppBar bar = ElwhaAppBar.small();
    bar.setTitle("Inbox");
    bar.setSize(400, bar.getPreferredSize().height);

    Pixels.assertPixelNear(
        Pixels.render(bar, 400, bar.getHeight()),
        390,
        4,
        ColorRole.SURFACE.resolve(),
        "§8 — a flat bar sits on the plain surface role");

    bar.setLifted(true);

    Pixels.assertPixelNear(
        Pixels.render(bar, 400, bar.getHeight()),
        390,
        4,
        ColorRole.SURFACE_CONTAINER.resolve(),
        "§8 — lifting swaps the container role, which is how the bar reads as separated");
  }

  @Test
  void liftCanBeDrivenDirectly() {
    final ElwhaAppBar bar = ElwhaAppBar.small();

    bar.setLifted(true);
    assertThat(bar.isLifted())
        .as("§8 — a consumer with its own scroll plumbing can set the state itself")
        .isTrue();

    bar.setLifted(false);
    assertThat(bar.isLifted()).isFalse();
  }

  @Test
  void clearingTheScrollSourceDropsTheLift() {
    final ScrollFixture scroll = ScrollFixture.create();
    final ElwhaAppBar bar = ElwhaAppBar.small();
    bar.setLiftOnScroll(true);
    bar.setScrollSource(scroll.pane());
    scroll.scrollTo(300);

    bar.setScrollSource(null);

    assertThat(bar.isLifted())
        .as("§8 — with nothing to react to, the bar returns to its resting state")
        .isFalse();
    assertThat(bar.getScrollSource()).isNull();
  }

  @Test
  void aDetachedBarStopsFollowingItsOldSource() {
    final ScrollFixture scroll = ScrollFixture.create();
    final ElwhaAppBar bar = ElwhaAppBar.small();
    bar.setLiftOnScroll(true);
    bar.setScrollSource(scroll.pane());
    bar.setScrollSource(null);

    scroll.scrollTo(500);

    assertThat(bar.isLifted())
        .as("the binding detaches, so a later scroll on the old pane is not heard")
        .isFalse();
  }

  @Test
  void aBarBoundToAnAlreadyScrolledPaneAdoptsThatStateImmediately() {
    final ScrollFixture scroll = ScrollFixture.create();
    scroll.scrollTo(400);
    final ElwhaAppBar bar = ElwhaAppBar.small();
    bar.setLiftOnScroll(true);

    bar.setScrollSource(scroll.pane());

    assertThat(bar.isLifted())
        .as("§8 — binding mid-scroll must not leave the bar in a stale resting state")
        .isTrue();
  }

  // ---------------------------------------------------------------- collapse

  @Test
  void aSmallBarDoesNotCollapse() {
    final ScrollFixture scroll = ScrollFixture.create();
    final ElwhaAppBar bar = ElwhaAppBar.small();
    bar.setScrollSource(scroll.pane());

    scroll.scrollTo(500);

    assertThat(bar.getCollapsedFraction())
        .as("§8 — the small bar is already the strip; there is nothing to collapse into")
        .isZero();
    assertThat(bar.getPreferredSize().height).isEqualTo(STRIP);
  }

  @ParameterizedTest
  @EnumSource(
      value = AppBarVariant.class,
      names = {"MEDIUM_FLEXIBLE", "LARGE_FLEXIBLE"})
  void aFlexibleBarStartsFullyExpanded(final AppBarVariant variant) {
    final ElwhaAppBar bar = new ElwhaAppBar(variant);

    assertThat(bar.getCollapsedFraction()).isZero();
    assertThat(bar.getPreferredSize().height)
        .as("§5 — the variant's expanded token height")
        .isEqualTo(variant.expandedHeightPx(false));
  }

  @ParameterizedTest
  @EnumSource(
      value = AppBarVariant.class,
      names = {"MEDIUM_FLEXIBLE", "LARGE_FLEXIBLE"})
  void collapseCompletesOverTheHeightItHasToGive(final AppBarVariant variant) {
    final ScrollFixture scroll = ScrollFixture.create();
    final ElwhaAppBar bar = new ElwhaAppBar(variant);
    bar.setScrollSource(scroll.pane());
    final int range = collapseRange(variant, false);

    scroll.scrollTo(range);

    assertThat(bar.getCollapsedFraction())
        .as("§8 — scrolling by the expanded-to-strip difference finishes the collapse exactly")
        .isEqualTo(1f);
    assertThat(bar.getPreferredSize().height)
        .as("§5 — and lands the bar on the 64 dp strip")
        .isEqualTo(STRIP);
  }

  @Test
  void collapseIsProportionalOnTheWayDown() {
    final ScrollFixture scroll = ScrollFixture.create();
    final ElwhaAppBar bar = ElwhaAppBar.mediumFlexible();
    bar.setScrollSource(scroll.pane());
    final int range = collapseRange(AppBarVariant.MEDIUM_FLEXIBLE, false);

    scroll.scrollTo(range / 4);
    assertThat(bar.getCollapsedFraction()).isCloseTo(0.25f, within(0.02f));

    scroll.scrollTo(range / 2);
    assertThat(bar.getCollapsedFraction())
        .as("§8 — the fraction is the scroll offset over the collapsible height")
        .isCloseTo(0.5f, within(0.02f));
  }

  @Test
  void barHeightRampsWithTheCollapseFraction() {
    final ElwhaAppBar bar = ElwhaAppBar.mediumFlexible();
    final int expanded = AppBarVariant.MEDIUM_FLEXIBLE.expandedHeightPx(false);

    bar.setCollapsedFraction(0.5f);

    assertThat(bar.getPreferredSize().height)
        .as("§5 — half collapsed is half way between the expanded height and the strip")
        .isEqualTo(STRIP + Math.round((expanded - STRIP) * 0.5f));
  }

  @Test
  void collapsingPastTheRangeStaysCollapsed() {
    final ScrollFixture scroll = ScrollFixture.create();
    final ElwhaAppBar bar = ElwhaAppBar.largeFlexible();
    bar.setScrollSource(scroll.pane());

    scroll.scrollTo(2000);

    assertThat(bar.getCollapsedFraction())
        .as("§8 — the collapse saturates; it does not keep shrinking with the content")
        .isEqualTo(1f);
    assertThat(bar.getPreferredSize().height).isEqualTo(STRIP);
  }

  @Test
  void scrollingBackUpReExpandsTheBar() {
    final ScrollFixture scroll = ScrollFixture.create();
    final ElwhaAppBar bar = ElwhaAppBar.mediumFlexible();
    bar.setScrollSource(scroll.pane());
    scroll.scrollTo(500);

    scroll.scrollTo(0);

    assertThat(bar.getCollapsedFraction())
        .as("§8 — the collapse tracks position, not direction, so it fully reverses")
        .isZero();
    assertThat(bar.getPreferredSize().height)
        .isEqualTo(AppBarVariant.MEDIUM_FLEXIBLE.expandedHeightPx(false));
  }

  @Test
  void aSubtitleLengthensTheCollapseRun() {
    final ScrollFixture withSubtitle = ScrollFixture.create();
    final ElwhaAppBar bar = ElwhaAppBar.mediumFlexible();
    bar.setSubtitle("12 unread");
    bar.setScrollSource(withSubtitle.pane());
    final int plainRange = collapseRange(AppBarVariant.MEDIUM_FLEXIBLE, false);

    withSubtitle.scrollTo(plainRange);

    assertThat(bar.getCollapsedFraction())
        .as("§5 — a subtitled bar is taller, so the same scroll gets it less of the way down")
        .isLessThan(1f);
  }

  @Test
  void collapseFractionIsClampedWhenDrivenDirectly() {
    final ElwhaAppBar bar = ElwhaAppBar.mediumFlexible();

    bar.setCollapsedFraction(5f);
    assertThat(bar.getCollapsedFraction()).isEqualTo(1f);

    bar.setCollapsedFraction(-5f);
    assertThat(bar.getCollapsedFraction()).isZero();
  }

  @Test
  void aSmallBarIgnoresADirectCollapseRequest() {
    final ElwhaAppBar bar = ElwhaAppBar.small();

    bar.setCollapsedFraction(1f);

    assertThat(bar.getCollapsedFraction())
        .as("§5 — collapse is a flexible-variant behaviour; the small bar has no expanded state")
        .isZero();
  }

  @Test
  void liftAndCollapseAreDrivenByTheSameScroll() {
    final ScrollFixture scroll = ScrollFixture.create();
    final ElwhaAppBar bar = ElwhaAppBar.mediumFlexible();
    bar.setLiftOnScroll(true);
    bar.setScrollSource(scroll.pane());

    scroll.scrollTo(collapseRange(AppBarVariant.MEDIUM_FLEXIBLE, false) / 2);

    assertThat(bar.isLifted())
        .as("§8 — one binding feeds both responses, so they never disagree about the offset")
        .isTrue();
    assertThat(bar.getCollapsedFraction()).isBetween(0.4f, 0.6f);
  }
}
