package com.owspfm.elwha.showcase;

import static org.assertj.core.api.Assertions.assertThat;

import com.owspfm.elwha.showcase.ElwhaShowcase.LeafEntry;
import com.owspfm.elwha.testkit.EdtInterceptor;
import com.owspfm.elwha.testkit.ThemeExtension;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

/**
 * The shape of the catalog itself — the label / blurb / area record every landing card and every
 * navigation route is built from. Design doc §4: the catalog is the single registry, registration
 * order is the rail's order, and each leaf belongs to exactly one area.
 */
@ExtendWith({EdtInterceptor.class, ThemeExtension.class})
class ShowcaseCatalogTest {

  @Test
  void catalogIsPopulated() {
    assertThat(ShowcaseFixture.leaves())
        .as("the storefront ships one leaf per component; an empty catalog is a broken build")
        .hasSizeGreaterThan(20);
  }

  @Test
  void everyLeafIsKeyedByItsOwnLabel() {
    for (final String label : ShowcaseFixture.leafLabels()) {
      assertThat(ShowcaseFixture.leaf(label).label)
          .as("§4 — the registry key is the display label, which is also the CardLayout card name")
          .isEqualTo(label);
    }
  }

  @Test
  void everyLeafCarriesALabel() {
    for (final LeafEntry entry : ShowcaseFixture.leaves()) {
      assertThat(entry.label).isNotBlank();
    }
  }

  @Test
  void everyLeafCarriesSupportingCopy() {
    for (final LeafEntry entry : ShowcaseFixture.leaves()) {
      assertThat(entry.supporting)
          .as(
              "%s — the landing card renders this blurb, so an empty one ships a blank card",
              entry.label)
          .isNotBlank();
    }
  }

  @Test
  void everyLeafCarriesARealisedSurface() {
    for (final LeafEntry entry : ShowcaseFixture.leaves()) {
      assertThat(entry.surface).as("%s", entry.label).isNotNull();
    }
  }

  @Test
  void everyLeafBelongsToOneOfTheThreeAreas() {
    for (final LeafEntry entry : ShowcaseFixture.leaves()) {
      assertThat(entry.area)
          .as("%s — an unknown area has no landing to route back to", entry.label)
          .isIn(ShowcaseFixture.areas());
    }
  }

  @Test
  void everyAreaHasLeaves() {
    for (final String area : ShowcaseFixture.areas()) {
      assertThat(ShowcaseFixture.showcase().leavesIn(area))
          .as("%s — an empty area landing is a dead rail destination", area)
          .isNotEmpty();
    }
  }

  @Test
  void areasPartitionTheCatalog() {
    final List<LeafEntry> combined = new ArrayList<>();
    for (final String area : ShowcaseFixture.areas()) {
      combined.addAll(ShowcaseFixture.showcase().leavesIn(area));
    }

    assertThat(combined)
        .as("§4 — every leaf is reachable from exactly one area landing, none twice, none orphaned")
        .containsExactlyInAnyOrderElementsOf(ShowcaseFixture.leaves());
  }

  @Test
  void anAreaListsOnlyItsOwnLeaves() {
    for (final String area : ShowcaseFixture.areas()) {
      for (final LeafEntry entry : ShowcaseFixture.showcase().leavesIn(area)) {
        assertThat(entry.area).isEqualTo(area);
      }
    }
  }

  @Test
  void anAreaListsItsLeavesAlphabeticallyWithinEachFamily() {
    for (final String area : ShowcaseFixture.areas()) {
      for (final List<LeafEntry> family : ShowcaseFixture.showcase().familiesIn(area).values()) {
        assertThat(family)
            .as(
                "#441 — landing cards read left-to-right, top-down by label inside %s / %s",
                area, family.get(0).group)
            .isSortedAccordingTo(Comparator.comparing((LeafEntry entry) -> entry.label));
      }
    }
  }

  @Test
  void anAreasFamiliesRunInTaxonomyOrder() {
    assertThat(ShowcaseFixture.showcase().familiesIn(ElwhaShowcase.AREA_COMPONENTS).keySet())
        .as(
            "#441 — the Components landing groups by the same families CLAUDE.md documents the"
                + " packages with, in that order")
        .containsExactly(
            ElwhaShowcase.GROUP_ACTIONS,
            ElwhaShowcase.GROUP_SELECTION,
            ElwhaShowcase.GROUP_FIELDS,
            ElwhaShowcase.GROUP_SURFACES,
            ElwhaShowcase.GROUP_OVERLAYS,
            ElwhaShowcase.GROUP_NAVIGATION,
            ElwhaShowcase.GROUP_FEEDBACK);
  }

  @Test
  void anAreaThatDoesNotSubdivideIsItsOwnSingleFamily() {
    for (final String area :
        List.of(ElwhaShowcase.AREA_FOUNDATIONS, ElwhaShowcase.AREA_CONTAINERS)) {
      assertThat(ShowcaseFixture.showcase().familiesIn(area).keySet())
          .as("#441 — %s does not subdivide, so its landing renders one unheaded grid", area)
          .containsExactly(area);
    }
  }

  @Test
  void everyLeafCarriesAFamily() {
    for (final LeafEntry entry : ShowcaseFixture.leaves()) {
      assertThat(entry.group)
          .as(
              "%s — a leaf with no family sorts to the head of its area and reads misplaced",
              entry.label)
          .isNotBlank();
    }
  }

  @Test
  void familiesPartitionTheirArea() {
    for (final String area : ShowcaseFixture.areas()) {
      final List<LeafEntry> combined = new ArrayList<>();
      ShowcaseFixture.showcase().familiesIn(area).values().forEach(combined::addAll);

      assertThat(combined)
          .as("#441 — the grouped landing renders exactly the flat list, once each: %s", area)
          .isEqualTo(ShowcaseFixture.showcase().leavesIn(area));
    }
  }

  @Test
  void registrationOrderGroupsEachAreaTogether() {
    final List<String> order = new ArrayList<>();
    for (final LeafEntry entry : ShowcaseFixture.leaves()) {
      if (order.isEmpty() || !order.get(order.size() - 1).equals(entry.area)) {
        order.add(entry.area);
      }
    }

    assertThat(order)
        .as(
            "§4 — registration order is the rail's order: each area's leaves are contiguous and "
                + "Foundations registers first, because the rail selects its first primary on open")
        .containsExactlyElementsOf(ShowcaseFixture.areas());
  }

  @Test
  void noLeafLabelCollidesWithALandingKey() {
    final List<String> landingKeys =
        List.of(
            ElwhaShowcase.HOME_KEY,
            ElwhaShowcase.FOUNDATIONS_KEY,
            ElwhaShowcase.COMPONENTS_KEY,
            ElwhaShowcase.CONTAINERS_KEY);

    assertThat(ShowcaseFixture.leafLabels())
        .as("leaf labels and landing keys share one CardLayout namespace")
        .doesNotContainAnyElementsOf(landingKeys);
  }

  @Test
  void everyLeafHasACardInTheContentArea() {
    assertThat(ShowcaseFixture.showcase().content.getComponentCount())
        .as("§4 — four landings plus one wrapped card per leaf")
        .isEqualTo(4 + ShowcaseFixture.leaves().size());
  }
}
