package com.owspfm.elwha.showcase;

import static org.assertj.core.api.Assertions.assertThat;

import com.owspfm.elwha.testkit.EdtInterceptor;
import com.owspfm.elwha.testkit.ThemeExtension;
import java.util.List;
import java.util.Set;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

/**
 * The registration-completeness sweep — every shipped component is presented somewhere in The Elwha
 * Showcase, or this fails.
 *
 * <p>Both sides are discovered, never listed. The roster comes from scanning the compiled library
 * for public, concrete, top-level {@code Elwha*} components; coverage comes from the Showcase's own
 * compiled references plus the classes a built catalog actually instantiates. A component that
 * ships without a leaf appears in neither, so it lands in the gap — which is the point: the sweep
 * has to fail for the next component someone forgets, not just describe today's catalog.
 */
@ExtendWith({EdtInterceptor.class, ThemeExtension.class})
class ShowcaseRegistryTest {

  // -------------------------------------------------------------- the sweep

  @Test
  void everyShippedComponentIsPresentedByTheShowcase() {
    final List<String> gaps =
        ShowcaseFixture.notPresented(
            ShowcaseFixture.shippedComponents(), ShowcaseFixture.presentedComponents());

    assertThat(gaps)
        .as(
            "every shipped component needs a Showcase leaf — these have none, so add one (or, if "
                + "the type is not a mountable component, tighten the roster rule)")
        .isEmpty();
  }

  @Test
  void aComponentNoLeafPresentsIsReportedAsAGap() {
    final String unpresented = "com.owspfm.elwha.example.ElwhaNeverPresented";

    final List<String> gaps =
        ShowcaseFixture.notPresented(List.of(unpresented), ShowcaseFixture.presentedComponents());

    assertThat(gaps)
        .as("the sweep must report a component nothing presents, not wave everything through")
        .containsExactly(unpresented);
  }

  // ------------------------------------------------------------- the roster

  @Test
  void rosterIsDiscoveredFromTheCompiledLibrary() {
    assertThat(ShowcaseFixture.shippedComponents())
        .as("a catalog this size cannot plausibly scan to a handful of classes")
        .hasSizeGreaterThan(25);
  }

  @Test
  void rosterCarriesOnlyLibraryComponents() {
    for (final String component : ShowcaseFixture.shippedComponents()) {
      assertThat(component).startsWith("com.owspfm.elwha.");
      assertThat(component).doesNotContain("$");
    }
  }

  @Test
  void rosterExcludesStoryTimeHarnesses() {
    for (final String component : ShowcaseFixture.shippedComponents()) {
      assertThat(component)
          .as("demos, smokes, guards, diagnostics, and playground launchers are not shipped API")
          .doesNotEndWith("Demo")
          .doesNotEndWith("Smoke")
          .doesNotEndWith("Guard")
          .doesNotEndWith("Diag")
          .doesNotEndWith("Playground");
    }
  }

  @Test
  void rosterExcludesTheShowcaseAndItsPlaygrounds() {
    for (final String component : ShowcaseFixture.shippedComponents()) {
      assertThat(component)
          .as("the harness is not the product")
          .doesNotContain(".showcase.")
          .doesNotContain(".playground.")
          .doesNotContain(".fixes.");
    }
  }

  @Test
  void rosterIncludesAnOverlayHostThatIsNotAComponent() {
    assertThat(ShowcaseFixture.shippedComponents())
        .as(
            "overlays mount on a layered pane rather than extending Component, and would slip the "
                + "sweep entirely if the roster rule only looked for Components")
        .contains("com.owspfm.elwha.dialog.ElwhaDialog", "com.owspfm.elwha.menu.ElwhaMenu");
  }

  @Test
  void rosterExcludesModelsEventsAndUtilities() {
    assertThat(ShowcaseFixture.shippedComponents())
        .as("a listener, an event, or a static utility class is not something a leaf can present")
        .doesNotContain(
            "com.owspfm.elwha.list.ElwhaSelectionEvent",
            "com.owspfm.elwha.list.ElwhaListDataListener",
            "com.owspfm.elwha.theme.ElwhaTheme");
  }

  // ------------------------------------------------------------ the coverage

  @Test
  void coverageCountsWhatTheShowcaseReferences() {
    assertThat(ShowcaseFixture.presentedComponents())
        .as(
            "the Showcase names ElwhaBadge in the badge workbench, which only builds one once a "
                + "control runs — the reference half of the sweep is what catches it")
        .contains("com.owspfm.elwha.badge.ElwhaBadge");
  }

  @Test
  void coverageCountsWhatABuiltLeafInstantiates() {
    assertThat(ShowcaseFixture.presentedComponents())
        .as(
            "a card header builds its own subtitle, so no Showcase class names ElwhaCardSubtitle — "
                + "the realised half of the sweep is what catches it")
        .contains("com.owspfm.elwha.card.ElwhaCardSubtitle");
  }

  @Test
  void coverageIsNotAnUnboundedWalkOfTheLibrary() {
    final Set<String> presented = ShowcaseFixture.presentedComponents();

    assertThat(presented)
        .as(
            "coverage expands through panel builders only — a diagnostic harness nothing in the "
                + "Showcase names must stay outside it, or the sweep would wave everything through")
        .doesNotContain("com.owspfm.elwha.card.fixes.CardFixesDemo");
  }
}
