package com.owspfm.elwha.showcase;

import static org.assertj.core.api.Assertions.assertThat;

import com.owspfm.elwha.card.ElwhaCard;
import com.owspfm.elwha.card.ElwhaCardSupportingText;
import com.owspfm.elwha.showcase.ElwhaShowcase.LeafEntry;
import com.owspfm.elwha.testkit.EdtInterceptor;
import com.owspfm.elwha.testkit.Input;
import com.owspfm.elwha.testkit.ThemeExtension;
import java.awt.Component;
import java.util.ArrayList;
import java.util.List;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.Scrollable;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

/**
 * The four landing pages — Home plus one per area — and the actionable cards that are the
 * Showcase's navigation surface. Design doc §5: Home is the master index, each area landing covers
 * only its own leaves, and every card routes to the leaf it names.
 */
@ExtendWith({EdtInterceptor.class, ThemeExtension.class})
class ShowcaseLandingTest {

  /** The Showcase's own frame geometry — the configuration #737 was reported against. */
  private static final int FRAME_WIDTH = 1240;

  private static final int FRAME_HEIGHT = 4000;

  private static List<ElwhaCard> cardsOn(final String key) {
    return ShowcaseFixture.findAll(ShowcaseFixture.show(key), ElwhaCard.class);
  }

  private static List<String> openLabels(final List<ElwhaCard> cards) {
    final List<String> labels = new ArrayList<>();
    for (final ElwhaCard card : cards) {
      labels.add(card.getToolTipText().replace("Open ", ""));
    }
    return labels;
  }

  // ------------------------------------------------------------------- Home

  @Test
  void homeIndexesEveryLeaf() {
    final List<ElwhaCard> cards = cardsOn(ElwhaShowcase.HOME_KEY);

    assertThat(openLabels(cards))
        .as("§5 — Home is the master index: every surface in the library, on one page")
        .containsExactlyInAnyOrderElementsOf(ShowcaseFixture.leafLabels());
  }

  @Test
  void homeGroupsLeavesByAreaInRailOrder() {
    final List<String> expected = new ArrayList<>();
    for (final String area : ShowcaseFixture.areas()) {
      for (final LeafEntry entry : ShowcaseFixture.showcase().leavesIn(area)) {
        expected.add(entry.label);
      }
    }

    assertThat(openLabels(cardsOn(ElwhaShowcase.HOME_KEY)))
        .as(
            "§5 — Home reads area by area in the rail's order; #441 — component family by family"
                + " inside each, alphabetically within a family")
        .containsExactlyElementsOf(expected);
  }

  // ---------------------------------------------------------- area landings

  @Test
  void anAreaLandingCoversOnlyItsOwnLeaves() {
    for (final String area : ShowcaseFixture.areas()) {
      final List<String> expected = new ArrayList<>();
      for (final LeafEntry entry : ShowcaseFixture.showcase().leavesIn(area)) {
        expected.add(entry.label);
      }

      assertThat(openLabels(cardsOn(landingKeyFor(area))))
          .as("§5 — %s landing", area)
          .containsExactlyElementsOf(expected);
    }
  }

  @Test
  void everyAreaLandingIsPopulated() {
    for (final String area : ShowcaseFixture.areas()) {
      assertThat(cardsOn(landingKeyFor(area))).as("%s landing", area).isNotEmpty();
    }
  }

  // ------------------------------------------------------------- card shape

  @Test
  void everyLandingCardIsActionable() {
    for (final ElwhaCard card : cardsOn(ElwhaShowcase.HOME_KEY)) {
      assertThat(card.isActionable())
          .as(
              "§5 — the cards are the navigation surface, not decoration: %s",
              card.getToolTipText())
          .isTrue();
    }
  }

  @Test
  void everyLandingCardCarriesItsLeafsSupportingCopy() {
    final List<ElwhaCard> cards = cardsOn(ElwhaShowcase.HOME_KEY);

    for (final ElwhaCard card : cards) {
      final String label = card.getToolTipText().replace("Open ", "");
      final ElwhaCardSupportingText supporting =
          ShowcaseFixture.findFirst(card, ElwhaCardSupportingText.class);

      assertThat(supporting).as("%s has no supporting text", label).isNotNull();
      assertThat(supporting.getText())
          .as("%s — the card renders the catalog blurb", label)
          .contains(ShowcaseFixture.leaf(label).supporting);
    }
  }

  @Test
  void everyLandingCardNamesItsLeafInTheTooltip() {
    for (final ElwhaCard card : cardsOn(ElwhaShowcase.HOME_KEY)) {
      assertThat(card.getToolTipText()).startsWith("Open ");
    }
  }

  // -------------------------------------------------------------- the route

  @Test
  void clickingALandingCardOpensThatLeaf() {
    for (final String label : ShowcaseFixture.leafLabels()) {
      final Component expected = cardNamed(label);
      final ElwhaCard card = laidOutCardFor(label);

      Input.click(card, card.getWidth() / 2, card.getHeight() / 2);

      assertThat(ShowcaseFixture.visibleCard())
          .as("§5 — a card click routes to the leaf it names: %s", label)
          .isSameAs(expected);
    }
  }

  @Test
  void clickingACardLeavesExactlyOneCardShowing() {
    final ElwhaCard card = laidOutCardFor("Button");

    Input.click(card, card.getWidth() / 2, card.getHeight() / 2);

    assertThat(ShowcaseFixture.visibleCards())
        .as("CardLayout shows one card; two visible means a stale card was never hidden")
        .hasSize(1);
  }

  // --------------------------------------------------------------- geometry

  @Test
  void landingGridIsWidthCapped() {
    final Component home = ShowcaseFixture.show(ElwhaShowcase.HOME_KEY);

    final List<JPanel> grids = new ArrayList<>();
    for (final JPanel panel : ShowcaseFixture.findAll(home, JPanel.class)) {
      if (panel.getLayout() instanceof java.awt.GridLayout) {
        grids.add(panel);
      }
    }

    int families = 0;
    for (final String area : ShowcaseFixture.areas()) {
      families += ShowcaseFixture.showcase().familiesIn(area).size();
    }
    assertThat(grids)
        .as("#441 — Home renders one card grid per component family, not one per area")
        .hasSize(families);
    for (final JPanel grid : grids) {
      assertThat(grid.getMaximumSize().width)
          .as("§5 — cards hold a dashboard-tile measure instead of stretching to the frame width")
          .isEqualTo(960);
    }
  }

  @Test
  void aLandingPageTracksItsViewportWidth() {
    final Component home = ShowcaseFixture.show(ElwhaShowcase.HOME_KEY);
    final JScrollPane scroll = ShowcaseFixture.findFirst(home, JScrollPane.class);

    assertThat(scroll).isNotNull();
    final Component view = scroll.getViewport().getView();
    assertThat(view).isInstanceOf(Scrollable.class);
    assertThat(((Scrollable) view).getScrollableTracksViewportWidth())
        .as("§5 — the page follows the viewport width so the capped grid never forces h-scroll")
        .isTrue();
    assertThat(((Scrollable) view).getScrollableTracksViewportHeight())
        .as("§5 — but it grows vertically, which is what scrolls")
        .isFalse();
  }

  /**
   * Every landing card holds its own content after <strong>one</strong> layout pass — the invariant
   * #737 broke, and the single pass is the whole point of the test.
   *
   * <p>Two passes hid the defect: measured on the pre-fix code, laying the Components landing out
   * twice left nothing overflowing, while laying it out once left 23 of 25 cards painting their
   * copy past the card's bottom border. The card was answering size queries against the width it
   * was holding rather than the one it was about to be given, so the first pass described the
   * previous pass's geometry and only the second caught up. A page that is measured once — a {@code
   * CardLayout} child becoming visible, a first show — kept the first answer.
   *
   * <p>The catalog is swept rather than listed: the cards that overflowed were simply the ones with
   * long blurbs, so a hand-kept list would go stale the first time a blurb is reworded, and the
   * card that starts overflowing next is by definition not on it. Every child is checked, not just
   * the supporting copy, so the header and divider are covered by the same assertion.
   */
  @Test
  void oneLayoutPassIsEnoughToFitEveryLandingCardsContent() {
    for (final String key : landingKeys()) {
      final Component landing = ShowcaseFixture.show(key);
      ShowcaseFixture.layOut(landing, FRAME_WIDTH, FRAME_HEIGHT);

      assertNoCardOverflows(landing, key, FRAME_WIDTH);
    }
  }

  /**
   * The same invariant across frame widths, from a settled tree.
   *
   * <p>Narrow frames are the interesting direction — copy wraps to more lines in a narrower cell —
   * but a landing measured at a width it has never held reads its ancestors' stale widths, and
   * inside a {@link JScrollPane} the view is measured before it is given the width it will paint
   * at. That is stock Swing settling, not the #737 defect, so the tree is driven to a settled state
   * before the assertion rather than the assertion being loosened to accommodate it.
   */
  @ParameterizedTest(name = "no landing card overflows its body at {0} px")
  @ValueSource(ints = {900, 1100, 1400})
  void landingCardContentStaysInsideTheCardBodyAtAnyWidth(final int frameWidth) {
    for (final String key : landingKeys()) {
      final Component landing = ShowcaseFixture.show(key);
      ShowcaseFixture.layOut(landing, frameWidth, FRAME_HEIGHT);
      invalidateTree(landing);
      ShowcaseFixture.layOut(landing, frameWidth, FRAME_HEIGHT);

      assertNoCardOverflows(landing, key, frameWidth);
    }
  }

  private static void assertNoCardOverflows(
      final Component landing, final String key, final int frameWidth) {
    for (final ElwhaCard card : ShowcaseFixture.findAll(landing, ElwhaCard.class)) {
      final String label = String.valueOf(card.getToolTipText()).replace("Open ", "");
      // The painted body is the card's bounds less its shadow reserve, so content crossing this
      // line is drawn past the rounded border rather than merely close to it.
      final int bodyBottom = card.getHeight() - card.getInsets().bottom;
      for (final Component child : card.getComponents()) {
        assertThat(child.getY() + child.getHeight())
            .as(
                "#737 — %s card on %s at %d px: %s runs past the card body (card %dx%d, body"
                    + " bottom %d). A wrapping atom measured at a width it will not paint at"
                    + " reports too few lines, and the card is sized to that.",
                label,
                key,
                frameWidth,
                child.getClass().getSimpleName(),
                card.getWidth(),
                card.getHeight(),
                bodyBottom)
            .isLessThanOrEqualTo(bodyBottom);
      }
    }
  }

  private static void invalidateTree(final Component component) {
    component.invalidate();
    if (component instanceof java.awt.Container container) {
      for (final Component child : container.getComponents()) {
        invalidateTree(child);
      }
    }
  }

  /**
   * The half the overflow sweep cannot see on its own: a card grown to fit its copy would also pass
   * if the copy had silently collapsed to nothing. Every landing card still renders a
   * multi-line-capable blurb with real height.
   */
  @Test
  void everyLandingCardStillGivesItsBlurbRoomToRender() {
    final Component landing = ShowcaseFixture.show(ElwhaShowcase.HOME_KEY);
    ShowcaseFixture.layOut(landing, 1100, 4000);

    for (final ElwhaCard card : ShowcaseFixture.findAll(landing, ElwhaCard.class)) {
      final ElwhaCardSupportingText text =
          ShowcaseFixture.findFirst(card, ElwhaCardSupportingText.class);
      assertThat(text.getWidth())
          .as("%s — the blurb was given a real cell width to wrap at", card.getToolTipText())
          .isGreaterThan(0);
      assertThat(text.getHeight())
          .as("%s — and enough height to draw at least one line", card.getToolTipText())
          .isGreaterThanOrEqualTo(text.getFontMetrics(text.getFont()).getHeight());
    }
  }

  // ----------------------------------------------------------------- detail

  private static List<String> landingKeys() {
    final List<String> keys = new ArrayList<>();
    keys.add(ElwhaShowcase.HOME_KEY);
    for (final String area : ShowcaseFixture.areas()) {
      keys.add(landingKeyFor(area));
    }
    return keys;
  }

  private static String landingKeyFor(final String area) {
    if (ElwhaShowcase.AREA_FOUNDATIONS.equals(area)) {
      return ElwhaShowcase.FOUNDATIONS_KEY;
    }
    if (ElwhaShowcase.AREA_COMPONENTS.equals(area)) {
      return ElwhaShowcase.COMPONENTS_KEY;
    }
    return ElwhaShowcase.CONTAINERS_KEY;
  }

  // Home, shown and laid out, so its cards carry the bounds a click has to land inside.
  private static ElwhaCard laidOutCardFor(final String label) {
    final Component home = ShowcaseFixture.show(ElwhaShowcase.HOME_KEY);
    ShowcaseFixture.layOut(home, 1240, 780);
    for (final ElwhaCard card : ShowcaseFixture.findAll(home, ElwhaCard.class)) {
      if (("Open " + label).equals(card.getToolTipText())) {
        return card;
      }
    }
    throw new AssertionError("Home has no card for " + label);
  }

  private static Component cardNamed(final String label) {
    ShowcaseFixture.show(label);
    return ShowcaseFixture.visibleCard();
  }
}
