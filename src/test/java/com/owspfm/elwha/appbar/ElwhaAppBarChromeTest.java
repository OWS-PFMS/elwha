package com.owspfm.elwha.appbar;

import static org.assertj.core.api.Assertions.assertThat;

import com.owspfm.elwha.iconbutton.ElwhaIconButton;
import com.owspfm.elwha.icons.MaterialIcons;
import com.owspfm.elwha.testkit.EdtInterceptor;
import com.owspfm.elwha.testkit.Input;
import com.owspfm.elwha.testkit.PaintLog;
import com.owspfm.elwha.testkit.ThemeExtension;
import com.owspfm.elwha.theme.ColorRole;
import com.owspfm.elwha.theme.TypeRole;
import java.awt.ComponentOrientation;
import java.awt.FontMetrics;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import javax.swing.JComponent;
import javax.swing.JLabel;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.EnumSource;

/**
 * Tier A coverage of the app bar's static chrome — design doc §3 (the leading navigation slot, the
 * trailing action run, the bar-painted title and subtitle) and §5 (the per-variant height and type
 * tables), plus the RTL mirror of the whole composition.
 */
@ExtendWith({EdtInterceptor.class, ThemeExtension.class})
class ElwhaAppBarChromeTest {

  private static final int BAR_WIDTH = 480;

  private static ElwhaIconButton iconButton() {
    return new ElwhaIconButton(MaterialIcons.symbol("menu").unselected());
  }

  private static ElwhaAppBar laidOut(final ElwhaAppBar bar) {
    bar.setSize(BAR_WIDTH, bar.getPreferredSize().height);
    bar.doLayout();
    return bar;
  }

  // ------------------------------------------------------------ variant table

  @ParameterizedTest
  @CsvSource({
    // variant, expanded height without subtitle, with subtitle — the §5 token table
    "SMALL, 64, 64",
    "MEDIUM_FLEXIBLE, 112, 136",
    "LARGE_FLEXIBLE, 120, 152"
  })
  void variantHeightTableMatchesTheSpec(
      final AppBarVariant variant, final int plain, final int withSubtitle) {
    assertThat(variant.expandedHeightPx(false)).isEqualTo(plain);
    assertThat(variant.expandedHeightPx(true))
        .as("§5 — a subtitle adds a line to the expanded block")
        .isEqualTo(withSubtitle);
  }

  @Test
  void onlyTheSmallVariantIsInflexible() {
    assertThat(AppBarVariant.SMALL.isFlexible())
        .as("§5 — the small bar is already the strip, so it has no collapse to do")
        .isFalse();
    assertThat(AppBarVariant.MEDIUM_FLEXIBLE.isFlexible()).isTrue();
    assertThat(AppBarVariant.LARGE_FLEXIBLE.isFlexible()).isTrue();
  }

  @ParameterizedTest
  @CsvSource({
    "SMALL, TITLE_LARGE, LABEL_MEDIUM",
    "MEDIUM_FLEXIBLE, HEADLINE_MEDIUM, LABEL_LARGE",
    "LARGE_FLEXIBLE, DISPLAY_SMALL, TITLE_MEDIUM"
  })
  void expandedTypeScaleGrowsWithTheVariant(
      final AppBarVariant variant, final TypeRole title, final TypeRole subtitle) {
    assertThat(variant.expandedTitleRole())
        .as("§5 — a taller bar carries a larger expanded title")
        .isEqualTo(title);
    assertThat(variant.expandedSubtitleRole()).isEqualTo(subtitle);
  }

  @Test
  void factoriesPinTheVariant() {
    assertThat(ElwhaAppBar.small().getVariant()).isEqualTo(AppBarVariant.SMALL);
    assertThat(ElwhaAppBar.mediumFlexible().getVariant()).isEqualTo(AppBarVariant.MEDIUM_FLEXIBLE);
    assertThat(ElwhaAppBar.largeFlexible().getVariant()).isEqualTo(AppBarVariant.LARGE_FLEXIBLE);
    assertThat(new ElwhaAppBar().getVariant())
        .as("§5 — small is the default")
        .isEqualTo(AppBarVariant.SMALL);
  }

  @ParameterizedTest
  @EnumSource(AppBarVariant.class)
  void aBarIsNeverShorterThanItsStrip(final AppBarVariant variant) {
    assertThat(new ElwhaAppBar(variant).getPreferredSize().height)
        .isGreaterThanOrEqualTo(ElwhaAppBar.STRIP_HEIGHT_PX);
  }

  @Test
  void minimumWidthIsTwoSlots() {
    assertThat(ElwhaAppBar.small().getMinimumSize().width)
        .as("§3 — enough room for a nav slot and one action, and no less")
        .isEqualTo(ElwhaAppBar.SLOT_SIZE_PX * 2);
  }

  // ------------------------------------------------------------------- slots

  @Test
  void navigationSlotSitsAtTheLeadingEdge() {
    final ElwhaAppBar bar = ElwhaAppBar.small();
    final ElwhaIconButton nav = iconButton();
    bar.setNavigationIcon(nav);
    laidOut(bar);

    assertThat(nav.getX())
        .as("§3 — the nav slot opens the bar at the 4 dp edge space")
        .isGreaterThanOrEqualTo(ElwhaAppBar.EDGE_SPACE_PX);
    assertThat(nav.getX())
        .as("and stays within its 48 dp slot")
        .isLessThan(ElwhaAppBar.EDGE_SPACE_PX + ElwhaAppBar.SLOT_SIZE_PX);
  }

  @Test
  void slottedChromeIsVerticallyCentredInTheStrip() {
    final ElwhaAppBar bar = ElwhaAppBar.small();
    final ElwhaIconButton nav = iconButton();
    bar.setNavigationIcon(nav);
    laidOut(bar);

    final int above = nav.getY();
    final int below = ElwhaAppBar.STRIP_HEIGHT_PX - (nav.getY() + nav.getHeight());
    assertThat(Math.abs(above - below))
        .as("§3 — chrome is centred on the 64 dp strip, not on the expanded container")
        .isLessThanOrEqualTo(1);
  }

  @Test
  void actionsRunInwardFromTheTrailingEdge() {
    final ElwhaAppBar bar = ElwhaAppBar.small();
    final ElwhaIconButton first = bar.addAction(iconButton());
    final ElwhaIconButton second = bar.addAction(iconButton());
    laidOut(bar);

    assertThat(second.getX() + second.getWidth())
        .as("§3 — the last action added sits closest to the trailing edge")
        .isGreaterThan(first.getX() + first.getWidth());
    assertThat(second.getX() + second.getWidth())
        .isLessThanOrEqualTo(BAR_WIDTH - ElwhaAppBar.EDGE_SPACE_PX);
  }

  @Test
  void actionsDoNotOverlapEachOther() {
    final ElwhaAppBar bar = ElwhaAppBar.small();
    final ElwhaIconButton first = bar.addAction(iconButton());
    final ElwhaIconButton second = bar.addAction(iconButton());
    final ElwhaIconButton third = bar.addAction(iconButton());
    laidOut(bar);

    assertThat(first.getX() + first.getWidth()).isLessThanOrEqualTo(second.getX());
    assertThat(second.getX() + second.getWidth()).isLessThanOrEqualTo(third.getX());
  }

  @Test
  void navigationSlotAndTheActionRunNeverCollide() {
    final ElwhaAppBar bar = ElwhaAppBar.small();
    final ElwhaIconButton nav = iconButton();
    bar.setNavigationIcon(nav);
    final ElwhaIconButton action = bar.addAction(iconButton());
    laidOut(bar);

    assertThat(nav.getX() + nav.getWidth()).isLessThan(action.getX());
  }

  @Test
  void actionListIsReportedInAddOrder() {
    final ElwhaAppBar bar = ElwhaAppBar.small();
    final ElwhaIconButton first = bar.addAction(iconButton());
    final ElwhaIconButton second = bar.addAction(iconButton());

    assertThat(bar.getActions()).containsExactly(first, second);
  }

  @Test
  void removingAnActionTakesItOutOfTheBar() {
    final ElwhaAppBar bar = ElwhaAppBar.small();
    final ElwhaIconButton action = bar.addAction(iconButton());

    bar.removeAction(action);

    assertThat(bar.getActions()).isEmpty();
    assertThat(action.getParent()).isNull();
  }

  @Test
  void replacingTheNavigationIconUnmountsTheOldOne() {
    final ElwhaAppBar bar = ElwhaAppBar.small();
    final ElwhaIconButton first = iconButton();
    final ElwhaIconButton second = iconButton();
    bar.setNavigationIcon(first);

    bar.setNavigationIcon(second);

    assertThat(bar.getNavigationIcon()).isSameAs(second);
    assertThat(first.getParent()).as("§3 — one nav slot, one occupant").isNull();
  }

  @Test
  void navigationSlotCanBeCleared() {
    final ElwhaAppBar bar = ElwhaAppBar.small();
    bar.setNavigationIcon(iconButton());

    bar.setNavigationIcon((ElwhaIconButton) null);

    assertThat(bar.getNavigationIcon()).isNull();
  }

  @Test
  void aTrailingElementIsAnEscapeHatchForNonIconChrome() {
    final ElwhaAppBar bar = ElwhaAppBar.small();
    final JComponent avatar = new JLabel("AB");
    avatar.setSize(40, 40);

    bar.addTrailingElement(avatar);
    laidOut(bar);

    assertThat(avatar.getParent())
        .as("§3 — the Expressive imagery/avatar allowance is a plain JComponent slot")
        .isSameAs(bar);
    assertThat(avatar.getX() + avatar.getWidth())
        .isLessThanOrEqualTo(BAR_WIDTH - ElwhaAppBar.EDGE_SPACE_PX);
  }

  @Test
  void aTrailingElementCanBeRemoved() {
    final ElwhaAppBar bar = ElwhaAppBar.small();
    final JComponent avatar = new JLabel("AB");
    bar.addTrailingElement(avatar);

    bar.removeTrailingElement(avatar);

    assertThat(avatar.getParent()).isNull();
  }

  // ------------------------------------------- the navigation-icon ownership rule

  @Test
  void barAttachesNoBehaviourToTheNavigationIcon() {
    final ElwhaAppBar bar = ElwhaAppBar.small();
    final AtomicInteger fired = new AtomicInteger();
    final ElwhaIconButton nav =
        bar.setNavigationIcon(
            MaterialIcons.symbol("menu").unselected(),
            "Open navigation",
            e -> fired.incrementAndGet());
    laidOut(bar);

    Input.click(nav, nav.getWidth() / 2, nav.getHeight() / 2);

    assertThat(fired.get()).as("the consumer's listener runs").isOne();
    assertThat(bar.getNavigationIcon())
        .as(
            "and the bar does not swap the glyph or take over the slot the way the nav rail's "
                + "menu button does — the app bar's leading slot is purely the consumer's")
        .isSameAs(nav);
  }

  @Test
  void convenienceSetterNamesTheButtonForAssistiveTech() {
    final ElwhaAppBar bar = ElwhaAppBar.small();

    final ElwhaIconButton nav =
        bar.setNavigationIcon(
            MaterialIcons.symbol("menu").unselected(), "Open navigation", e -> {});

    assertThat(nav.getAccessibleContext().getAccessibleName())
        .as("§3 — an icon-only control is unreadable without a name, so the shorthand demands one")
        .isEqualTo("Open navigation");
  }

  @Test
  void actionConvenienceAlsoNamesItsButton() {
    final ElwhaAppBar bar = ElwhaAppBar.small();

    final ElwhaIconButton action =
        bar.addAction(MaterialIcons.symbol("close").unselected(), "Dismiss", e -> {});

    assertThat(action.getAccessibleContext().getAccessibleName()).isEqualTo("Dismiss");
  }

  // ------------------------------------------------------------------- text

  @Test
  void titleIsPaintedByTheBar() {
    final ElwhaAppBar bar = ElwhaAppBar.small();
    bar.setTitle("Inbox");
    laidOut(bar);

    assertThat(PaintLog.capture(bar, BAR_WIDTH, bar.getHeight()).painted("Inbox"))
        .as("§3 — title and subtitle are bar-painted text, not child components")
        .isTrue();
  }

  @Test
  void subtitleIsPaintedBeneathTheTitle() {
    final ElwhaAppBar bar = ElwhaAppBar.small();
    bar.setTitle("Inbox");
    bar.setSubtitle("12 unread");
    laidOut(bar);

    final PaintLog log = PaintLog.capture(bar, BAR_WIDTH, bar.getHeight());

    assertThat(log.painted("12 unread")).isTrue();
    assertThat(log.text("12 unread").orElseThrow().y())
        .as("§3 — the subtitle is the second line of the text block")
        .isGreaterThan(log.text("Inbox").orElseThrow().y());
  }

  @Test
  void titleAndSubtitleTakeTheirOwnContentRoles() {
    final ElwhaAppBar bar = ElwhaAppBar.small();
    bar.setTitle("Inbox");
    bar.setSubtitle("12 unread");
    laidOut(bar);

    final PaintLog log = PaintLog.capture(bar, BAR_WIDTH, bar.getHeight());

    assertThat(log.text("Inbox").orElseThrow().color())
        .as("§3 — the title is the bar's primary content")
        .isEqualTo(ColorRole.ON_SURFACE.resolve());
    assertThat(log.text("12 unread").orElseThrow().color())
        .as("§3 — the subtitle is subordinate")
        .isEqualTo(ColorRole.ON_SURFACE_VARIANT.resolve());
  }

  @Test
  void titleStartsAfterTheNavigationSlot() {
    final ElwhaAppBar bar = ElwhaAppBar.small();
    final ElwhaIconButton nav = iconButton();
    bar.setNavigationIcon(nav);
    bar.setTitle("Inbox");
    laidOut(bar);

    assertThat(PaintLog.capture(bar, BAR_WIDTH, bar.getHeight()).text("Inbox").orElseThrow().x())
        .as("§3 — the title region begins where the nav slot ends")
        .isGreaterThanOrEqualTo(nav.getX() + nav.getWidth());
  }

  @Test
  void aTitleWithNoNavigationIconStartsAtTheTextInset() {
    final ElwhaAppBar bar = ElwhaAppBar.small();
    bar.setTitle("Inbox");
    laidOut(bar);

    assertThat(PaintLog.capture(bar, BAR_WIDTH, bar.getHeight()).text("Inbox").orElseThrow().x())
        .as("§3 — with no nav slot the title takes the 16 dp text inset instead")
        .isEqualTo(ElwhaAppBar.TITLE_INSET_PX);
  }

  @Test
  void centringMovesTheTitleAwayFromTheLeadingInset() {
    final ElwhaAppBar bar = ElwhaAppBar.small();
    bar.setTitle("Inbox");
    laidOut(bar);
    final double leading =
        PaintLog.capture(bar, BAR_WIDTH, bar.getHeight()).text("Inbox").orElseThrow().x();

    bar.setTitleCentered(true);

    assertThat(bar.isTitleCentered()).isTrue();
    assertThat(PaintLog.capture(bar, BAR_WIDTH, bar.getHeight()).text("Inbox").orElseThrow().x())
        .as("§3 — the centre-title option is the M3 'center-aligned' small-bar form")
        .isGreaterThan(leading);
  }

  @Test
  void aBarWithNoTextPaintsNone() {
    final ElwhaAppBar bar = laidOut(ElwhaAppBar.small());

    assertThat(PaintLog.capture(bar, BAR_WIDTH, bar.getHeight()).texts())
        .as("an untitled bar is still a legitimate bar")
        .isEmpty();
  }

  @Test
  void titleWidensThePreferredSize() {
    final ElwhaAppBar narrow = ElwhaAppBar.small();
    narrow.setTitle("Hi");
    final ElwhaAppBar wide = ElwhaAppBar.small();
    wide.setTitle("A considerably longer application bar title");

    assertThat(wide.getPreferredSize().width)
        .as("§3 — the bar asks for room for its own text plus its slots")
        .isGreaterThan(narrow.getPreferredSize().width);
  }

  // ------------------------------------------ expanded headline wrapping (#478)

  private static final String LONG_TITLE = "Quarterly revenue and operating expenses";

  // A wrapping headline is height-for-width: the bar cannot know how many lines it takes until it
  // has a width to measure against, so tests hand it one before asking what height it wants.
  private static ElwhaAppBar measuredAt(final ElwhaAppBar bar, final int width) {
    bar.setSize(width, 0);
    return bar;
  }

  private static int lineHeight(final ElwhaAppBar bar, final AppBarVariant variant) {
    return bar.getFontMetrics(variant.expandedTitleRole().resolve()).getHeight();
  }

  private static PaintLog paintExpanded(final ElwhaAppBar bar, final int width) {
    measuredAt(bar, width);
    return PaintLog.capture(bar, width, bar.getPreferredSize().height);
  }

  @Test
  void headlinesDoNotWrapUntilTheyAreAllowedTo() {
    final ElwhaAppBar bar = ElwhaAppBar.mediumFlexible();
    bar.setTitle(LONG_TITLE);

    assertThat(bar.getTitleMaxLines())
        .as("§12 — V1 shipped single-line everywhere and wrapping is the opt-in on top of it")
        .isOne();
    assertThat(measuredAt(bar, 480).getPreferredSize().height)
        .as("so a long title still ellipsizes into the variant's token height")
        .isEqualTo(AppBarVariant.MEDIUM_FLEXIBLE.expandedHeightPx(false));
  }

  @ParameterizedTest
  @EnumSource(
      value = AppBarVariant.class,
      names = {"MEDIUM_FLEXIBLE", "LARGE_FLEXIBLE"})
  void aWrappedHeadlineGrowsTheBarByOneLineOfItsOwnTitleRole(final AppBarVariant variant) {
    final ElwhaAppBar bar = new ElwhaAppBar(variant);
    bar.setTitle(LONG_TITLE);
    bar.setTitleMaxLines(2);

    assertThat(measuredAt(bar, 480).getPreferredSize().height)
        .as(
            "#478 — the token heights are quoted for one line, so a second line is worth exactly"
                + " one line of the type role that bar paints its headline in")
        .isEqualTo(variant.expandedHeightPx(false) + lineHeight(bar, variant));
  }

  @Test
  void aTallerVariantGrowsByMoreBecauseItsHeadlineIsBigger() {
    final ElwhaAppBar medium = ElwhaAppBar.mediumFlexible();
    medium.setTitle(LONG_TITLE);
    medium.setTitleMaxLines(2);
    final ElwhaAppBar large = ElwhaAppBar.largeFlexible();
    large.setTitle(LONG_TITLE);
    large.setTitleMaxLines(2);

    final int mediumGrowth =
        measuredAt(medium, 480).getPreferredSize().height
            - AppBarVariant.MEDIUM_FLEXIBLE.expandedHeightPx(false);
    final int largeGrowth =
        measuredAt(large, 480).getPreferredSize().height
            - AppBarVariant.LARGE_FLEXIBLE.expandedHeightPx(false);

    assertThat(largeGrowth)
        .as("#478 — a Display Small line costs more height than a Headline Medium one")
        .isGreaterThan(mediumGrowth);
  }

  @Test
  void aTitleThatAlreadyFitsIsLeftAlone() {
    final ElwhaAppBar bar = ElwhaAppBar.mediumFlexible();
    bar.setTitle("Inbox");
    bar.setTitleMaxLines(2);

    assertThat(measuredAt(bar, 480).getPreferredSize().height)
        .as("#478 — the budget is a ceiling, not a reservation; short titles cost nothing")
        .isEqualTo(AppBarVariant.MEDIUM_FLEXIBLE.expandedHeightPx(false));
  }

  @Test
  void aSmallBarNeverWraps() {
    final ElwhaAppBar bar = ElwhaAppBar.small();
    bar.setTitle(LONG_TITLE);
    bar.setTitleMaxLines(2);

    assertThat(measuredAt(bar, 480).getPreferredSize().height)
        .as("#478 — wrapping is a flexible-variant affordance; the 64 dp strip has no second line")
        .isEqualTo(ElwhaAppBar.STRIP_HEIGHT_PX);
    assertThat(PaintLog.capture(bar, 480, ElwhaAppBar.STRIP_HEIGHT_PX).texts())
        .as("and its title stays one ellipsized run")
        .hasSize(1);
  }

  @Test
  void wrappingBreaksBetweenWordsRatherThanThroughThem() {
    final ElwhaAppBar bar = ElwhaAppBar.mediumFlexible();
    bar.setTitle(LONG_TITLE);
    bar.setTitleMaxLines(2);

    final List<String> lines =
        paintExpanded(bar, 480).texts().stream().map(PaintLog.Text::string).toList();

    assertThat(lines).as("#478 — a two-line headline is two painted runs").hasSize(2);
    assertThat(String.join(" ", lines))
        .as("and the break costs nothing but the space it happened at")
        .isEqualTo(LONG_TITLE);
  }

  @Test
  void lastAllowedLineEllipsizesWhatIsLeftOver() {
    final ElwhaAppBar bar = ElwhaAppBar.mediumFlexible();
    bar.setTitle(LONG_TITLE + " for the western region and its subsidiaries");
    bar.setTitleMaxLines(2);

    final List<String> lines =
        paintExpanded(bar, 480).texts().stream().map(PaintLog.Text::string).toList();

    assertThat(lines).hasSize(2);
    assertThat(lines.get(1))
        .as("#478 — the budget still terminates the headline; it does not run off the end")
        .endsWith("…");
  }

  @Test
  void aWordWiderThanTheLineBreaksInsteadOfOverflowing() {
    final ElwhaAppBar bar = ElwhaAppBar.mediumFlexible();
    final FontMetrics fm =
        bar.getFontMetrics(AppBarVariant.MEDIUM_FLEXIBLE.expandedTitleRole().resolve());

    final List<String> lines =
        ElwhaAppBar.wrapHeadline("Supercalifragilisticexpialidocious", fm, 120, 2);

    assertThat(lines).as("there is no word boundary to break at, so it breaks anyway").hasSize(2);
    assertThat(fm.stringWidth(lines.get(0)))
        .as("#478 — and neither line escapes the width it was measured for")
        .isLessThanOrEqualTo(120);
    assertThat(fm.stringWidth(lines.get(1))).isLessThanOrEqualTo(120);
  }

  @Test
  void wrappedLinesStackUpwardsFromTheSameBottomAnchor() {
    final ElwhaAppBar bar = ElwhaAppBar.mediumFlexible();
    bar.setTitle(LONG_TITLE);
    bar.setTitleMaxLines(2);
    bar.setSubtitle("12 unread");

    final PaintLog log = paintExpanded(bar, 480);
    final List<PaintLog.Text> texts = log.texts();

    assertThat(texts).hasSize(3);
    assertThat(texts.get(0).y())
        .as("#478 — extra lines grow the block upward; the anatomy below them does not move")
        .isLessThan(texts.get(1).y());
    assertThat(texts.get(2).y())
        .as("§7 — and the subtitle stays the bottom line of the block")
        .isGreaterThan(texts.get(1).y());
  }

  @Test
  void aNarrowerBarNeedsTheSecondLineThatAWiderOneDoesNot() {
    final ElwhaAppBar wide = ElwhaAppBar.mediumFlexible();
    wide.setTitle(LONG_TITLE);
    wide.setTitleMaxLines(2);
    final ElwhaAppBar narrow = ElwhaAppBar.mediumFlexible();
    narrow.setTitle(LONG_TITLE);
    narrow.setTitleMaxLines(2);

    assertThat(measuredAt(wide, 720).getPreferredSize().height)
        .as("#478 — wrapping is height-for-width: the same title costs nothing when it fits")
        .isEqualTo(AppBarVariant.MEDIUM_FLEXIBLE.expandedHeightPx(false));
    assertThat(measuredAt(narrow, 480).getPreferredSize().height)
        .as("and a line when it does not")
        .isGreaterThan(measuredAt(wide, 720).getPreferredSize().height);
  }

  @Test
  void resizingABarReflowsItsHeadline() {
    final ElwhaAppBar bar = ElwhaAppBar.mediumFlexible();
    bar.setTitle(LONG_TITLE);
    bar.setTitleMaxLines(2);
    final int wrapped = measuredAt(bar, 480).getPreferredSize().height;

    bar.setSize(720, wrapped);
    bar.doLayout();

    assertThat(bar.getPreferredSize().height)
        .as("#478 — the line count is re-measured against the width the bar actually has")
        .isEqualTo(AppBarVariant.MEDIUM_FLEXIBLE.expandedHeightPx(false));
  }

  @Test
  void collapsedTitleStaysOneLineHoweverTheExpandedOneWraps() {
    final ElwhaAppBar bar = ElwhaAppBar.mediumFlexible();
    bar.setTitle(LONG_TITLE);
    bar.setTitleMaxLines(2);
    measuredAt(bar, 480);
    bar.setCollapsedFraction(1f);

    final List<PaintLog.Text> texts =
        PaintLog.capture(bar, 480, ElwhaAppBar.STRIP_HEIGHT_PX).texts();

    assertThat(texts)
        .as("#478 — M3 wraps the headline, not the strip; a collapsed bar is the small bar")
        .hasSize(1);
    assertThat(texts.get(0).font())
        .as("§4 — painted in the strip's own type role")
        .isEqualTo(TypeRole.TITLE_LARGE.resolve());
  }

  @Test
  void aWrappedHeadlineMirrorsWithTheRestOfTheBar() {
    final ElwhaAppBar bar = ElwhaAppBar.mediumFlexible();
    bar.setTitle(LONG_TITLE);
    bar.setTitleMaxLines(2);
    bar.applyComponentOrientation(ComponentOrientation.RIGHT_TO_LEFT);

    final List<PaintLog.Text> texts = paintExpanded(bar, 480).texts();

    assertThat(texts).hasSize(2);
    final double firstEnd = texts.get(0).x() + widthOf(bar, texts.get(0));
    final double secondEnd = texts.get(1).x() + widthOf(bar, texts.get(1));
    assertThat(Math.abs(firstEnd - secondEnd))
        .as("§9 — both lines hang off the trailing margin once the orientation flips")
        .isLessThanOrEqualTo(1.0);
  }

  private static double widthOf(final ElwhaAppBar bar, final PaintLog.Text text) {
    return bar.getFontMetrics(text.font()).stringWidth(text.string());
  }

  @Test
  void lineBudgetIsAtLeastOne() {
    final ElwhaAppBar bar = ElwhaAppBar.mediumFlexible();

    bar.setTitleMaxLines(0);
    assertThat(bar.getTitleMaxLines()).as("a headline of no lines is not a thing").isOne();

    bar.setTitleMaxLines(-3);
    assertThat(bar.getTitleMaxLines()).isOne();
  }

  // -------------------------------------------------------------------- RTL

  @Test
  void rightToLeftMovesTheNavigationSlotToTheRight() {
    final ElwhaAppBar bar = ElwhaAppBar.small();
    final ElwhaIconButton nav = iconButton();
    bar.setNavigationIcon(nav);
    bar.applyComponentOrientation(ComponentOrientation.RIGHT_TO_LEFT);
    laidOut(bar);

    assertThat(nav.getX() + nav.getWidth())
        .as("§3 — leading means right once the orientation flips")
        .isGreaterThan(BAR_WIDTH - ElwhaAppBar.EDGE_SPACE_PX - ElwhaAppBar.SLOT_SIZE_PX);
  }

  @Test
  void rightToLeftMovesTheActionRunToTheLeft() {
    final ElwhaAppBar bar = ElwhaAppBar.small();
    final ElwhaIconButton action = bar.addAction(iconButton());
    bar.applyComponentOrientation(ComponentOrientation.RIGHT_TO_LEFT);
    laidOut(bar);

    assertThat(action.getX()).as("§3 — and trailing means left").isLessThan(BAR_WIDTH / 2);
  }

  @Test
  void rightToLeftKeepsTheNavigationSlotClearOfTheActions() {
    final ElwhaAppBar bar = ElwhaAppBar.small();
    final ElwhaIconButton nav = iconButton();
    bar.setNavigationIcon(nav);
    final ElwhaIconButton action = bar.addAction(iconButton());
    bar.applyComponentOrientation(ComponentOrientation.RIGHT_TO_LEFT);
    laidOut(bar);

    assertThat(action.getX() + action.getWidth())
        .as("the mirror preserves the no-overlap invariant")
        .isLessThan(nav.getX());
  }

  @Test
  void rightToLeftMovesTheTitleToTheRightHalf() {
    final ElwhaAppBar bar = ElwhaAppBar.small();
    bar.setTitle("Inbox");
    bar.applyComponentOrientation(ComponentOrientation.RIGHT_TO_LEFT);
    laidOut(bar);

    assertThat(PaintLog.capture(bar, BAR_WIDTH, bar.getHeight()).text("Inbox").orElseThrow().x())
        .as("§3 — the title region mirrors with everything else")
        .isGreaterThan(BAR_WIDTH / 2.0);
  }

  // ------------------------------------------------------------ enablement

  @Test
  void disablingTheBarDisablesItsChrome() {
    final ElwhaAppBar bar = ElwhaAppBar.small();
    final ElwhaIconButton nav = iconButton();
    bar.setNavigationIcon(nav);
    final ElwhaIconButton action = bar.addAction(iconButton());

    bar.setEnabled(false);

    assertThat(nav.isEnabled()).isFalse();
    assertThat(action.isEnabled()).isFalse();
  }

  @Test
  void reEnablingRestoresEachChildsOwnState() {
    final ElwhaAppBar bar = ElwhaAppBar.small();
    final ElwhaIconButton enabled = bar.addAction(iconButton());
    final ElwhaIconButton alreadyOff = bar.addAction(iconButton());
    alreadyOff.setEnabled(false);

    bar.setEnabled(false);
    bar.setEnabled(true);

    assertThat(enabled.isEnabled()).isTrue();
    assertThat(alreadyOff.isEnabled())
        .as("a child the consumer had already disabled must not be silently switched on")
        .isFalse();
  }
}
