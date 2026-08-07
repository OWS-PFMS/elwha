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
