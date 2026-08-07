package com.owspfm.elwha.showcase;

import static org.assertj.core.api.Assertions.assertThat;

import com.owspfm.elwha.button.ElwhaButton;
import com.owspfm.elwha.navrail.ElwhaNavRailDestination;
import com.owspfm.elwha.navrail.ElwhaNavigationRail;
import com.owspfm.elwha.showcase.ElwhaShowcase.LeafEntry;
import com.owspfm.elwha.testkit.EdtInterceptor;
import com.owspfm.elwha.testkit.ThemeExtension;
import java.awt.Component;
import java.util.ArrayList;
import java.util.List;
import javax.swing.JScrollPane;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

/**
 * The navigation model: the rail's three area destinations, the card swap they drive, the
 * back-to-landing affordance wrapped around every leaf, and the scroll region the floating FAB
 * follows. Design doc §3 and §6 — the rail owns lateral navigation, and the content area is a
 * CardLayout showing exactly one surface at a time.
 */
@ExtendWith({EdtInterceptor.class, ThemeExtension.class})
class ShowcaseNavigationTest {

  // The rail wired to the shared Showcase — the one showCard keeps in sync.
  private static ElwhaNavigationRail rail() {
    return ShowcaseFixture.rail();
  }

  // A rail built off a bare Showcase, for the structural assertions that would otherwise read a
  // selection some earlier navigation left behind.
  private static ElwhaNavigationRail freshRail() {
    return new ElwhaShowcase().buildShowcaseRail();
  }

  // ------------------------------------------------------------------- rail

  @Test
  void railCarriesOneDestinationPerArea() {
    final List<String> labels = new ArrayList<>();
    for (final ElwhaNavRailDestination destination : freshRail().getPrimary()) {
      labels.add(destination.getLabel());
    }

    assertThat(labels)
        .as("§3 — the rail's primaries are the three areas, in catalog order")
        .containsExactlyElementsOf(ShowcaseFixture.areas());
  }

  @Test
  void railOpensOnFoundations() {
    assertThat(freshRail().getSelected().getLabel())
        .as("§3 — the rail selects its first primary, which is why Foundations registers first")
        .isEqualTo(ElwhaShowcase.AREA_FOUNDATIONS);
  }

  @Test
  void railCarriesItsChromeSlots() {
    final ElwhaNavigationRail target = freshRail();

    assertThat(target.getMenuButton()).as("§3 — the rail owns the hamburger").isNotNull();
    assertThat(target.getFab()).as("§3 — the rail's FAB routes to the Home index").isNotNull();
    assertThat(target.getTrailingActions())
        .as("§3 — help and about live in the trailing utility row")
        .hasSize(2);
  }

  @Test
  void railIsNamedForAssistiveTechnology() {
    assertThat(freshRail().getAccessibleContext().getAccessibleName())
        .isEqualTo("Showcase navigation rail");
  }

  // -------------------------------------------------------- the card swap

  @Test
  void showingALeafLeavesExactlyOneCardVisible() {
    for (final String label : ShowcaseFixture.leafLabels()) {
      ShowcaseFixture.show(label);

      assertThat(ShowcaseFixture.visibleCards())
          .as("§6 — one surface at a time: %s", label)
          .hasSize(1);
    }
  }

  @Test
  void showingALeafWrapsItInABackAffordance() {
    for (final LeafEntry entry : ShowcaseFixture.leaves()) {
      final Component card = ShowcaseFixture.show(entry.label);

      assertThat(ShowcaseFixture.descendants(card))
          .as("§6 — the leaf's own surface is what the card shows: %s", entry.label)
          .contains(entry.surface);
    }
  }

  @Test
  void everyLeafOffersAWayBackToItsArea() {
    for (final LeafEntry entry : ShowcaseFixture.leaves()) {
      final Component card = ShowcaseFixture.show(entry.label);

      assertThat(backButton(card))
          .as(
              "§6 — the back strip stands in for breadcrumbs, so no leaf may lack one: %s",
              entry.label)
          .isNotNull();
      assertThat(backButton(card).getText())
          .as("%s — the affordance names the area it returns to", entry.label)
          .isEqualTo("← " + entry.area);
    }
  }

  @Test
  void backAffordanceReturnsToTheAreaLanding() {
    for (final String area : ShowcaseFixture.areas()) {
      final LeafEntry entry = ShowcaseFixture.showcase().leavesIn(area).get(0);
      final Component landing = ShowcaseFixture.show(landingKeyFor(area));
      final Component card = ShowcaseFixture.show(entry.label);

      backButton(card).doClick();

      assertThat(ShowcaseFixture.visibleCard())
          .as("§6 — back from %s lands on the %s page", entry.label, area)
          .isSameAs(landing);
    }
  }

  // ------------------------------------------------- rail / content sync

  @Test
  void openingALeafSelectsItsAreaOnTheRail() {
    for (final LeafEntry entry : ShowcaseFixture.leaves()) {
      ShowcaseFixture.show(entry.label);

      assertThat(rail().getSelected().getLabel())
          .as(
              "§6 — the rail tracks where the content area is: %s lives under %s",
              entry.label, entry.area)
          .isEqualTo(entry.area);
    }
  }

  @Test
  void openingAnAreaLandingSelectsThatArea() {
    for (final String area : ShowcaseFixture.areas()) {
      ShowcaseFixture.show(landingKeyFor(area));

      assertThat(rail().getSelected().getLabel()).isEqualTo(area);
    }
  }

  @Test
  void homeBelongsToNoAreaSoItLeavesTheRailAlone() {
    ShowcaseFixture.show("Button");
    final ElwhaNavRailDestination before = rail().getSelected();

    ShowcaseFixture.show(ElwhaShowcase.HOME_KEY);

    assertThat(ShowcaseFixture.showcase().primaryForKey(ElwhaShowcase.HOME_KEY))
        .as("§3 — Home is the master index, not an area, so no primary owns it")
        .isNull();
    assertThat(rail().getSelected())
        .as("and a key with no owning primary leaves the current selection standing")
        .isSameAs(before);
  }

  @Test
  void anUnknownKeyOwnsNoPrimary() {
    assertThat(ShowcaseFixture.showcase().primaryForKey("__not_a_card")).isNull();
  }

  @Test
  void everyLeafKeyResolvesToItsAreasPrimary() {
    for (final LeafEntry entry : ShowcaseFixture.leaves()) {
      assertThat(ShowcaseFixture.showcase().primaryForKey(entry.label).getLabel())
          .as("%s", entry.label)
          .isEqualTo(entry.area);
    }
  }

  // ---------------------------------------------------- the FAB's scroll source

  @Test
  void aLandingPageOffersItsPageScrollToTheFab() {
    final Component landing = ShowcaseFixture.show(ElwhaShowcase.HOME_KEY);

    assertThat(ElwhaShowcase.findScrollPane(landing))
        .as("#274 — a landing is a real page scroll, so the floating FAB shrinks with it")
        .isNotNull();
  }

  @Test
  void aComponentWorkbenchOffersNoPageScrollToTheFab() {
    final ComponentWorkbench workbench = new ComponentWorkbench();

    assertThat(ShowcaseFixture.findAll(workbench, JScrollPane.class))
        .as("the stage, the controls column, and the code view each ride in a scroll pane")
        .hasSizeGreaterThanOrEqualTo(3);
    assertThat(ElwhaShowcase.findScrollPane(workbench))
        .as(
            "#310 — none of a Workbench's inner panes is a page scroll, so it offers the floating "
                + "FAB nothing to shrink on")
        .isNull();
  }

  @Test
  void aContainerWorkbenchOffersNoPageScrollToTheFab() {
    final ContainerWorkbench workbench = new ContainerWorkbench();

    assertThat(ShowcaseFixture.findAll(workbench, JScrollPane.class))
        .as("the controls column and the event log each ride in a scroll pane")
        .hasSizeGreaterThanOrEqualTo(2);
    assertThat(ElwhaShowcase.findScrollPane(workbench))
        .as(
            "#310 — a container workbench's controls column and event log are inner panes for the "
                + "same reason a component workbench's are: scrolling the knobs is not paging "
                + "content, and the floating FAB must not shrink on it")
        .isNull();
  }

  @Test
  void everyWorkbenchInnerScrollIsTaggedForTheFabToSkip() {
    for (final JScrollPane pane :
        ShowcaseFixture.findAll(new ComponentWorkbench(), JScrollPane.class)) {
      assertThat(pane.getClientProperty(ComponentWorkbench.FAB_SCROLL_IGNORE))
          .as("#310 — the skip is opt-in per pane, so an untagged one silently drives the FAB")
          .isEqualTo(Boolean.TRUE);
    }
    for (final JScrollPane pane :
        ShowcaseFixture.findAll(new ContainerWorkbench(), JScrollPane.class)) {
      assertThat(pane.getClientProperty(ComponentWorkbench.FAB_SCROLL_IGNORE))
          .as("#310 — and a container workbench's panes are inner panes too")
          .isEqualTo(Boolean.TRUE);
    }
  }

  @Test
  void activeCardIsWhereTheFabLooksForItsScroll() {
    ShowcaseFixture.show(ElwhaShowcase.HOME_KEY);
    final JScrollPane onHome = ShowcaseFixture.showcase().activeCardScrollPane();

    ShowcaseFixture.show(ElwhaShowcase.FOUNDATIONS_KEY);
    final JScrollPane onFoundations = ShowcaseFixture.showcase().activeCardScrollPane();

    assertThat(onHome)
        .as("#274 — the content is a CardLayout, so the scroll region is the active card")
        .isNotNull();
    assertThat(onFoundations)
        .as("and it re-resolves on every card swap rather than holding the prior card's pane")
        .isNotNull()
        .isNotSameAs(onHome);
  }

  // ----------------------------------------------------------------- detail

  private static ElwhaButton backButton(final Component card) {
    for (final ElwhaButton button : ShowcaseFixture.findAll(card, ElwhaButton.class)) {
      if (button.getText() != null && button.getText().startsWith("← ")) {
        return button;
      }
    }
    return null;
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
}
