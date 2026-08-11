package com.owspfm.elwha.appbar;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.within;

import com.owspfm.elwha.icons.MaterialIcons;
import com.owspfm.elwha.testkit.EdtInterceptor;
import com.owspfm.elwha.testkit.PaintLog;
import com.owspfm.elwha.testkit.Pixels;
import com.owspfm.elwha.testkit.ThemeExtension;
import com.owspfm.elwha.theme.ColorRole;
import com.owspfm.elwha.theme.TypeRole;
import java.awt.image.BufferedImage;
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
 *
 * <p>Every bar that drives a scroll is {@code addNotify}'d first. Since #631 the binding attaches
 * only while the bar is displayable — {@code removeNotify} is the sole detach path, so attaching
 * from an undisplayed bar strands the subscription — which makes mounting part of the setup rather
 * than an incidental detail.
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

    bar.addNotify();
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

    bar.addNotify();
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
    bar.addNotify();
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
    bar.addNotify();
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
    bar.addNotify();
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
    bar.addNotify();
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
    bar.addNotify();
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

    bar.addNotify();
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
    bar.addNotify();
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
    bar.addNotify();
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
    bar.addNotify();
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
    bar.addNotify();
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
    bar.addNotify();
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
    bar.addNotify();
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

  // ------------------------------------------- height-driven collapse (#525)

  private static final int BAR_WIDTH = 480;

  private static ElwhaAppBar flexibleSpecimen() {
    final ElwhaAppBar bar = ElwhaAppBar.largeFlexible();
    bar.setTitle("Headline");
    bar.setSubtitle("Subtitle");
    bar.setNavigationIcon(MaterialIcons.symbol("menu").unselected(), "Open navigation", e -> {});
    bar.addAction(MaterialIcons.symbol("close").unselected(), "Dismiss", e -> {});
    return bar;
  }

  private static int differingPixels(final BufferedImage a, final BufferedImage b) {
    int differing = 0;
    for (int y = 0; y < a.getHeight(); y++) {
      for (int x = 0; x < a.getWidth(); x++) {
        if (a.getRGB(x, y) != b.getRGB(x, y)) {
          differing++;
        }
      }
    }
    return differing;
  }

  @ParameterizedTest
  @EnumSource(
      value = AppBarVariant.class,
      names = {"MEDIUM_FLEXIBLE", "LARGE_FLEXIBLE"})
  void aBarGivenTheHeightItAskedForIsUnaffected(final AppBarVariant variant) {
    final ElwhaAppBar bar = new ElwhaAppBar(variant);

    bar.setSize(BAR_WIDTH, bar.getPreferredSize().height);

    assertThat(bar.getCollapsedFraction())
        .as("#525 — the degradation is invisible to every host that honours the preferred height")
        .isZero();
  }

  @Test
  void aBarSqueezedBelowItsPreferredHeightCollapsesInProportion() {
    final ElwhaAppBar bar = flexibleSpecimen();
    final int expanded = AppBarVariant.LARGE_FLEXIBLE.expandedHeightPx(true);

    bar.setSize(BAR_WIDTH, (expanded + STRIP) / 2);

    assertThat(bar.getCollapsedFraction())
        .as("#525 — half the collapsible height taken away is half the collapse done")
        .isCloseTo(0.5f, within(0.02f));
  }

  @Test
  void aBarSqueezedToTheStripIsFullyCollapsed() {
    final ElwhaAppBar bar = flexibleSpecimen();

    bar.setSize(BAR_WIDTH, STRIP);

    assertThat(bar.getCollapsedFraction())
        .as("#525 — 64 dp is the collapsed bar, whichever way the bar got there")
        .isEqualTo(1f);
  }

  @Test
  void anUnderAllocatedBarRendersExactlyAsIfItHadBeenScrolledThere() {
    final int expanded = AppBarVariant.LARGE_FLEXIBLE.expandedHeightPx(true);
    final int squeezed = 96;
    final ScrollFixture scroll = ScrollFixture.create();
    final ElwhaAppBar scrolled = flexibleSpecimen();
    scrolled.setLiftOnScroll(false);
    scrolled.addNotify();
    scrolled.setScrollSource(scroll.pane());
    scroll.scrollTo(expanded - squeezed);
    assertThat(scrolled.getPreferredSize().height)
        .as("the scrolled bar has collapsed itself to the height the other one is being handed")
        .isEqualTo(squeezed);

    final ElwhaAppBar squeezedBar = flexibleSpecimen();

    assertThat(
            differingPixels(
                Pixels.render(scrolled, BAR_WIDTH, squeezed),
                Pixels.render(squeezedBar, BAR_WIDTH, squeezed)))
        .as(
            "#525 — this is the whole fix: an under-allocated bar is not a special rendering mode,"
                + " it is the ordinary collapse read off the height the host gave it")
        .isZero();
  }

  @Test
  void anUnderAllocatedBarBringsUpItsCollapsedTitle() {
    final ElwhaAppBar bar = flexibleSpecimen();

    final PaintLog log = PaintLog.capture(bar, BAR_WIDTH, 72);

    assertThat(log.texts())
        .as(
            "#525 — at 72 px the expanded headline used to paint over the icon strip at full"
                + " strength with nothing else; the strip's own title now carries the bar")
        .anyMatch(text -> text.font().equals(TypeRole.TITLE_LARGE.resolve()));
  }

  @Test
  void extraHeightNeverReExpandsABarThatScrollHasCollapsed() {
    final ScrollFixture scroll = ScrollFixture.create();
    final ElwhaAppBar bar = flexibleSpecimen();
    bar.addNotify();
    bar.setScrollSource(scroll.pane());
    scroll.scrollTo(2000);

    bar.setSize(BAR_WIDTH, 400);

    assertThat(bar.getCollapsedFraction())
        .as("#525 — the two drivers only ever collapse the bar further; neither undoes the other")
        .isEqualTo(1f);
  }

  @Test
  void beingSqueezedDoesNotChangeWhatTheBarAsksFor() {
    final ElwhaAppBar bar = flexibleSpecimen();
    final int wanted = bar.getPreferredSize().height;

    bar.setSize(BAR_WIDTH, STRIP);

    assertThat(bar.getPreferredSize().height)
        .as(
            "#525 — the height-driven collapse is a render-time read. If it fed back into the"
                + " preferred size, one squeeze would latch the bar collapsed forever")
        .isEqualTo(wanted);
    assertThat(bar.getMinimumSize().height)
        .as("and the bar goes on telling layouts it cannot usefully be shorter")
        .isEqualTo(wanted);
  }

  @Test
  void aSmallBarHasNoHeightToGiveUp() {
    final ElwhaAppBar bar = ElwhaAppBar.small();
    bar.setTitle("Inbox");

    bar.setSize(BAR_WIDTH, 20);

    assertThat(bar.getCollapsedFraction())
        .as("#525 — collapse is a flexible-variant behaviour however the height arrives")
        .isZero();
  }

  @Test
  void anUnsizedBarReportsTheFractionItWasGiven() {
    final ElwhaAppBar bar = ElwhaAppBar.mediumFlexible();

    bar.setCollapsedFraction(0.4f);

    assertThat(bar.getCollapsedFraction())
        .as(
            "a bar no layout has touched yet has no allocation to read, so scroll is the whole"
                + " story")
        .isCloseTo(0.4f, within(0.001f));
  }

  @Test
  void aWrappedHeadlineRaisesTheHeightBothDriversMeasureAgainst() {
    final ElwhaAppBar bar = ElwhaAppBar.mediumFlexible();
    bar.setTitle("Quarterly revenue and operating expenses");
    bar.setTitleMaxLines(2);
    bar.setSize(BAR_WIDTH, 0);
    final int grown = bar.getPreferredSize().height;
    assertThat(grown).isGreaterThan(AppBarVariant.MEDIUM_FLEXIBLE.expandedHeightPx(false));

    bar.setSize(BAR_WIDTH, grown);
    assertThat(bar.getCollapsedFraction())
        .as("#478 × #525 — the grown height is the new fully-expanded height, not an over-run")
        .isZero();

    final ScrollFixture scroll = ScrollFixture.create();
    bar.addNotify();
    bar.setScrollSource(scroll.pane());
    scroll.scrollTo(grown - STRIP);

    assertThat(bar.getCollapsedFraction())
        .as("and the collapse run lengthens to match, so the bar still lands exactly on the strip")
        .isEqualTo(1f);
  }

  @Test
  void aWrappedBarUnderAllocatedToItsUnwrappedHeightIsAlreadyCollapsing() {
    final ElwhaAppBar bar = ElwhaAppBar.mediumFlexible();
    bar.setTitle("Quarterly revenue and operating expenses");
    bar.setTitleMaxLines(2);

    bar.setSize(BAR_WIDTH, AppBarVariant.MEDIUM_FLEXIBLE.expandedHeightPx(false));

    assertThat(bar.getCollapsedFraction())
        .as(
            "#478 × #525 — a host that budgeted for the token height is under-allocating a wrapped"
                + " bar, and gets the proportional collapse rather than a clipped second line")
        .isGreaterThan(0f);
  }

  // ---------------------------------------------------------------- teardown

  @Test
  void anUndisplayedBarDoesNotSubscribeToItsScrollSource() {
    final ScrollFixture scroll = ScrollFixture.create();
    final ElwhaAppBar bar = ElwhaAppBar.small();
    bar.setLiftOnScroll(true);
    final int baseline = scroll.modelListenerCount();

    bar.setScrollSource(scroll.pane());

    assertThat(scroll.modelListenerCount())
        .as(
            "#631 — removeNotify is the only detach path and it never fires for a bar that was"
                + " never added, so attaching here strands the subscription for the bar's whole"
                + " life")
        .isEqualTo(baseline);
  }

  @Test
  void addNotifyArmsAScrollSourceSetBeforeTheBarWasDisplayed() {
    final ScrollFixture scroll = ScrollFixture.create();
    final ElwhaAppBar bar = ElwhaAppBar.small();
    bar.setLiftOnScroll(true);
    bar.setScrollSource(scroll.pane());

    bar.addNotify();
    scroll.scrollTo(120);

    assertThat(bar.isLifted())
        .as("#631 — deferring the attach must not cost the bar its response once it is displayed")
        .isTrue();
  }

  @Test
  void aBarReAddedAfterScrollingStillPaintsItsLift() {
    final ScrollFixture scroll = ScrollFixture.create();
    final ElwhaAppBar bar = ElwhaAppBar.small();
    bar.setTitle("Inbox");
    bar.setLiftOnScroll(true);
    bar.addNotify();
    bar.setScrollSource(scroll.pane());
    scroll.scrollTo(300);
    assertThat(bar.isLifted()).as("the bar is lifted before the swap").isTrue();

    bar.removeNotify();
    bar.addNotify();

    assertThat(bar.isLifted()).as("and still reports lifted after it").isTrue();
    Pixels.assertPixelNear(
        Pixels.render(bar, 400, bar.getPreferredSize().height),
        390,
        4,
        ColorRole.SURFACE_CONTAINER.resolve(),
        "#626 — liftAnimator.stop() reset progress to 0 while `lifted` stayed true, and "
            + "updateLift early-returns on a matching flag, so the bar reported lifted while "
            + "painting SURFACE");
  }

  @Test
  void liftAndCollapseAreDrivenByTheSameScroll() {
    final ScrollFixture scroll = ScrollFixture.create();
    final ElwhaAppBar bar = ElwhaAppBar.mediumFlexible();
    bar.setLiftOnScroll(true);
    bar.addNotify();
    bar.setScrollSource(scroll.pane());

    scroll.scrollTo(collapseRange(AppBarVariant.MEDIUM_FLEXIBLE, false) / 2);

    assertThat(bar.isLifted())
        .as("§8 — one binding feeds both responses, so they never disagree about the offset")
        .isTrue();
    assertThat(bar.getCollapsedFraction()).isBetween(0.4f, 0.6f);
  }
}
