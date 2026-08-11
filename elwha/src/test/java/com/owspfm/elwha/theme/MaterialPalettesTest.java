package com.owspfm.elwha.theme;

import static org.assertj.core.api.Assertions.assertThat;

import com.owspfm.elwha.testkit.EdtInterceptor;
import com.owspfm.elwha.testkit.ThemeExtension;
import java.awt.Color;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.stream.Collectors;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

/**
 * Tier A coverage of the bundled theme tiers — that discovery is directory-derived rather than a
 * hardcoded list, that the two tiers stay disjoint, that spectral order holds (chromatic by hue,
 * neutrals last), and that every discovered palette is installable. Oracle: the {@link
 * MaterialPalettes} contract and {@code CLAUDE.md}'s palettes section.
 */
@ExtendWith({EdtInterceptor.class, ThemeExtension.class})
class MaterialPalettesTest {

  /** Name fragments that push a theme behind the chromatic spread. */
  private static final List<String> NEUTRAL_KEYWORDS = List.of("grey", "gray", "brown");

  // ------------------------------------------------------------- baseline

  @Test
  void baselineLoadsAsACompleteTwoSchemeTheme() {
    final Theme baseline = MaterialPalettes.baseline();

    assertThat(baseline.name()).as("the baseline names itself").isEqualTo("Material Baseline");
    for (final ColorRole role : ColorRole.values()) {
      assertThat(baseline.light().get(role)).as("baseline light carries %s", role).isNotNull();
      assertThat(baseline.dark().get(role)).as("baseline dark carries %s", role).isNotNull();
    }
  }

  @Test
  void baselineIsLoadedOnceAndShared() {
    assertThat(MaterialPalettes.baseline())
        .as("the baseline is parsed once and cached, not re-read per call")
        .isSameAs(MaterialPalettes.baseline());
  }

  @Test
  void baselineIsAMemberOfThePrimaryTier() {
    assertThat(names(MaterialPalettes.primary()))
        .as("the baseline ships inside the curated tier rather than beside it")
        .contains("Material Baseline");
  }

  @Test
  void baselineIsTheVeryInstanceThePrimaryTierCarries() {
    assertThat(MaterialPalettes.primary())
        .as("one palette resource means one Theme, so a picker can match the installed one")
        .contains(MaterialPalettes.baseline());

    assertThat(
            MaterialPalettes.primary().stream()
                .filter(theme -> theme == MaterialPalettes.baseline())
                .count())
        .as("and exactly one entry is it — not an equal copy loaded a second time")
        .isEqualTo(1L);
  }

  // ------------------------------------------------------------ discovery

  @Test
  void bothTiersDiscoverTheirDirectories() {
    assertThat(MaterialPalettes.primary())
        .as("the curated tier is the baseline plus the ROYGBIV demo set")
        .hasSize(8);
    assertThat(MaterialPalettes.secondary())
        .as("the exploration tier is the ten remaining Theme Builder palettes")
        .hasSize(10);
  }

  @Test
  void eachTierIsLoadedOnceAndShared() {
    assertThat(MaterialPalettes.primary())
        .as("the primary tier is discovered once and cached")
        .isSameAs(MaterialPalettes.primary());
    assertThat(MaterialPalettes.secondary())
        .as("the secondary tier is discovered once and cached")
        .isSameAs(MaterialPalettes.secondary());
  }

  @Test
  void tiersAreImmutableToCallers() {
    assertThat(MaterialPalettes.primary())
        .as("a caller cannot add a theme to the shared tier list")
        .isUnmodifiable();
  }

  @Test
  void everyDiscoveredThemeIsCompleteInBothModes() {
    for (final Theme theme : allBundled()) {
      for (final ColorRole role : ColorRole.values()) {
        assertThat(theme.light().get(role))
            .as("%s light carries %s", theme.name(), role)
            .isNotNull();
        assertThat(theme.dark().get(role)).as("%s dark carries %s", theme.name(), role).isNotNull();
      }
    }
  }

  // ------------------------------------------------------- tier disjointness

  @Test
  void twoTiersShareNoTheme() {
    assertThat(names(MaterialPalettes.primary()))
        .as("the tiers are additive — a theme belongs to exactly one of them")
        .doesNotContainAnyElementsOf(names(MaterialPalettes.secondary()));
  }

  @Test
  void twoTiersShareNoPrimaryColor() {
    final List<Color> primaryTierColors =
        MaterialPalettes.primary().stream()
            .map(theme -> theme.light().get(ColorRole.PRIMARY))
            .collect(Collectors.toList());

    for (final Theme theme : MaterialPalettes.secondary()) {
      assertThat(primaryTierColors)
          .as(
              "%s is in the secondary tier because its color is not already in the primary one",
              theme.name())
          .doesNotContain(theme.light().get(ColorRole.PRIMARY));
    }
  }

  @Test
  void everyBundledThemeHasAUniqueName() {
    final List<String> all = names(allBundled());

    assertThat(all).as("no two bundled palettes answer to the same name").doesNotHaveDuplicates();
  }

  // ------------------------------------------------------- spectral order

  @Test
  void chromaticThemesRunRedToVioletWithinEachTier() {
    assertSpectral(MaterialPalettes.primary(), "primary");
    assertSpectral(MaterialPalettes.secondary(), "secondary");
  }

  @Test
  void neutralFamilyThemesCollectAtTheEnd() {
    final List<Theme> secondary = MaterialPalettes.secondary();
    final List<String> trailing =
        names(secondary.subList(secondary.size() - neutralCount(secondary), secondary.size()));

    assertThat(neutralCount(secondary))
        .as("the exploration tier does carry neutrals — otherwise this asserts nothing")
        .isPositive();
    for (final String name : trailing) {
      assertThat(isNeutral(name)).as("%s sorts behind the chromatic spread", name).isTrue();
    }
  }

  @Test
  void discoveryOrderIsStableAcrossCalls() {
    assertThat(names(MaterialPalettes.primary()))
        .as("the order a palette picker renders does not shuffle between calls")
        .isEqualTo(names(MaterialPalettes.primary()));
    assertThat(names(MaterialPalettes.secondary()))
        .as("the exploration tier is equally stable")
        .isEqualTo(names(MaterialPalettes.secondary()));
  }

  // ------------------------------------------------------------- installing

  @Test
  void everyBundledThemeInstallsAndReachesTheResolvedRoles() {
    for (final Theme theme : allBundled()) {
      ElwhaTheme.install(ElwhaTheme.config().theme(theme).mode(Mode.LIGHT).build());

      assertThat(ColorRole.PRIMARY.resolve().getRGB())
          .as("installing %s writes its own light primary into the token keys", theme.name())
          .isEqualTo(theme.light().get(ColorRole.PRIMARY).getRGB());

      ElwhaTheme.install(ElwhaTheme.config().theme(theme).mode(Mode.DARK).build());

      assertThat(ColorRole.PRIMARY.resolve().getRGB())
          .as("installing %s in dark writes its dark primary", theme.name())
          .isEqualTo(theme.dark().get(ColorRole.PRIMARY).getRGB());
    }
  }

  private static void assertSpectral(final List<Theme> tier, final String label) {
    float previousHue = -1f;
    for (final Theme theme : tier) {
      if (isNeutral(theme.name())) {
        continue;
      }
      final float hue = primaryHue(theme);
      assertThat(hue)
          .as("the %s tier is hue-ordered, and %s breaks the run", label, theme.name())
          .isGreaterThanOrEqualTo(previousHue);
      previousHue = hue;
    }
  }

  private static float primaryHue(final Theme theme) {
    final Color primary = theme.light().get(ColorRole.PRIMARY);
    return Color.RGBtoHSB(primary.getRed(), primary.getGreen(), primary.getBlue(), null)[0];
  }

  private static boolean isNeutral(final String name) {
    final String lower = name.toLowerCase(Locale.ROOT);
    return NEUTRAL_KEYWORDS.stream().anyMatch(lower::contains);
  }

  private static int neutralCount(final List<Theme> tier) {
    return (int) tier.stream().filter(theme -> isNeutral(theme.name())).count();
  }

  private static List<Theme> allBundled() {
    final List<Theme> all = new ArrayList<>(MaterialPalettes.primary());
    all.addAll(MaterialPalettes.secondary());
    return all;
  }

  private static List<String> names(final List<Theme> themes) {
    return themes.stream().map(Theme::name).collect(Collectors.toList());
  }
}
