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

/**
 * The four landing pages — Home plus one per area — and the actionable cards that are the
 * Showcase's navigation surface. Design doc §5: Home is the master index, each area landing covers
 * only its own leaves, and every card routes to the leaf it names.
 */
@ExtendWith({EdtInterceptor.class, ThemeExtension.class})
class ShowcaseLandingTest {

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

  // ----------------------------------------------------------------- detail

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
