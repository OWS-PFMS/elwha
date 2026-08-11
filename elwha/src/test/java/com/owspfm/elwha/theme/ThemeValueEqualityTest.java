package com.owspfm.elwha.theme;

import static org.assertj.core.api.Assertions.assertThat;

import com.owspfm.elwha.testkit.EdtInterceptor;
import com.owspfm.elwha.testkit.ThemeExtension;
import java.awt.Color;
import java.util.Set;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

/**
 * Tier A coverage of the install API's value-equality contract, ruled for the 1.0 freeze in #698.
 *
 * <p>{@link Palette}, {@link Theme}, {@link Typography} and {@link Config} are immutable value
 * types that carried identity equality, which is what #671 actually hit: {@code baseline.json}
 * loaded twice produced two equal-but-distinct themes, {@code primary().contains(baseline())} was
 * false, and a palette picker could not mark the installed theme as selected. That was fixed by
 * caching one instance per resource path — a fix that only ever covered the bundled entry points,
 * leaving a consumer who loaded a theme through {@link PaletteLoader} with an object that could
 * never match a bundled one. {@link Mode} needs no ruling: it is an enum.
 *
 * @see <a href="https://github.com/OWS-PFMS/elwha/issues/698">#698</a>
 */
@ExtendWith({EdtInterceptor.class, ThemeExtension.class})
class ThemeValueEqualityTest {

  @Test
  void twoIndependentLoadsOfOnePaletteResourceAreEqual() {
    // The #671 scenario with the cache taken out of the picture: PaletteLoader is what a consumer
    // shipping its own palette calls, and it returns a fresh object every time.
    final Theme first =
        PaletteLoader.loadTheme("/com/owspfm/elwha/theme/palettes/primary/blue.json");
    final Theme second =
        PaletteLoader.loadTheme("/com/owspfm/elwha/theme/palettes/primary/blue.json");

    assertThat(first).as("two loads are genuinely distinct objects").isNotSameAs(second);
    assertThat(first).as("but they describe the same theme").isEqualTo(second);
    assertThat(first.hashCode()).isEqualTo(second.hashCode());
    assertThat(Set.of(first))
        .as("so a set or a contains() check treats them as one")
        .contains(second);
  }

  @Test
  void aConsumerLoadedThemeMatchesTheBundledOneItDuplicates() {
    // The half the resource-keyed cache could not reach: the consumer's own copy of the same JSON.
    final Theme bundled = MaterialPalettes.baseline();
    final Theme reloaded =
        PaletteLoader.loadTheme("/com/owspfm/elwha/theme/palettes/primary/baseline.json");

    assertThat(reloaded).isNotSameAs(bundled).isEqualTo(bundled);
    assertThat(MaterialPalettes.primary())
        .as("which is what lets a picker mark it as the installed entry")
        .contains(reloaded);
  }

  @Test
  void palettesDifferingInOneRoleAreNotEqual() {
    final Palette base = MaterialPalettes.baseline().light();
    final Palette tweaked = rebuild(base, ColorRole.PRIMARY, Color.MAGENTA);

    assertThat(tweaked).isNotEqualTo(base);
    assertThat(rebuild(base, ColorRole.PRIMARY, base.get(ColorRole.PRIMARY)))
        .as("and an identical rebuild still is equal")
        .isEqualTo(base);
  }

  @Test
  void themeEqualityIsNotJustTheName() {
    final Theme baseline = MaterialPalettes.baseline();
    final Theme impostor =
        new Theme(
            baseline.name(),
            rebuild(baseline.light(), ColorRole.PRIMARY, Color.MAGENTA),
            baseline.dark());

    assertThat(impostor).as("same name, different palette").isNotEqualTo(baseline);
    assertThat(new Theme(baseline.name(), baseline.light(), baseline.dark()))
        .as("same name, same palettes")
        .isEqualTo(baseline);
    assertThat(new Theme("Other", baseline.light(), baseline.dark()))
        .as("different name, same palettes")
        .isNotEqualTo(baseline);
  }

  @Test
  void ofFamilyRoundTripIsLosslessByEqualityAndNotOnlyByRendering() {
    // Typography.ofFamily(existing.familyName()) is documented as a lossless round trip. Without
    // equality it was lossless in what it painted and produced an object that compared unequal to
    // its source — which would surface as a spurious Config difference.
    final Typography defaults = Typography.defaults();

    assertThat(Typography.ofFamily(defaults.familyName()))
        .isNotSameAs(defaults)
        .isEqualTo(defaults);
    assertThat(Typography.ofFamily("Serif")).isNotEqualTo(defaults);
  }

  @Test
  void configComparesAllFourSettings() {
    final Config base =
        Config.builder()
            .theme(MaterialPalettes.baseline())
            .mode(Mode.LIGHT)
            .typography(Typography.defaults())
            .build();

    assertThat(
            Config.builder()
                .theme(MaterialPalettes.baseline())
                .mode(Mode.LIGHT)
                .typography(Typography.defaults())
                .build())
        .as("an independently built but identical config")
        .isEqualTo(base)
        .hasSameHashCodeAs(base);

    assertThat(base.withMode(Mode.DARK)).isNotEqualTo(base);
    assertThat(base.withTheme(MaterialPalettes.primary().get(0))).isNotEqualTo(base);
    assertThat(base.withTypography(Typography.ofFamily("Serif"))).isNotEqualTo(base);
    assertThat(base.withReducedMotion(true)).isNotEqualTo(base);
    assertThat(base.withReducedMotion(null)).as("null is the resting override").isEqualTo(base);
  }

  /**
   * The use #698 named: comparing the live config against one about to be installed, so a redundant
   * {@code install} — base LAF re-setup, the whole key table, {@code updateComponentTreeUI} on
   * every window — can be skipped.
   */
  @Test
  void liveConfigCanBeComparedAgainstAProposedOne() {
    final Config live = ElwhaTheme.current();

    assertThat(live.withMode(live.mode()))
        .as("a no-op derivation equals what is installed")
        .isEqualTo(live);
    assertThat(live.withMode(live.mode() == Mode.DARK ? Mode.LIGHT : Mode.DARK))
        .as("a real change does not")
        .isNotEqualTo(live);
  }

  @Test
  void toStringIdentifiesTheValueWithoutDumpingIt() {
    assertThat(MaterialPalettes.baseline().toString())
        .isEqualTo("Theme[" + MaterialPalettes.baseline().name() + "]");
    assertThat(Typography.defaults().toString()).startsWith("Typography[");
    assertThat(MaterialPalettes.baseline().light().toString())
        .as("a fingerprint, not all 49 roles")
        .matches("Palette\\[primary=#[0-9A-F]{6}, surface=#[0-9A-F]{6}\\]");
  }

  private static Palette rebuild(final Palette source, final ColorRole role, final Color value) {
    final Palette.Builder builder = Palette.builder();
    for (final ColorRole each : ColorRole.values()) {
      builder.set(each, each == role ? value : source.get(each));
    }
    return builder.build();
  }
}
