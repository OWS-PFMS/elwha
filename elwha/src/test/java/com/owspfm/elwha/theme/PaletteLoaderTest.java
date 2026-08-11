package com.owspfm.elwha.theme;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.owspfm.elwha.testkit.EdtInterceptor;
import com.owspfm.elwha.testkit.ThemeExtension;
import java.awt.Color;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

/**
 * Tier A coverage of the palette JSON contract — a happy-path load of a bundled resource, the
 * documented {@code name} fallback and unknown-key tolerance, and every documented failure mode.
 * Oracle: Appendix B of {@code docs/research/elwha-theme-install-api.md}.
 *
 * <p>The malformed fixtures live under {@code src/test/resources/com/owspfm/elwha/theme/
 * testpalettes/} — a sibling of the bundled {@code palettes/} tree rather than inside it, so they
 * can never be picked up by {@link MaterialPalettes}' directory discovery.
 */
@ExtendWith({EdtInterceptor.class, ThemeExtension.class})
class PaletteLoaderTest {

  private static final String BUNDLED_BLUE = "/com/owspfm/elwha/theme/palettes/primary/blue.json";
  private static final String FIXTURES = "/com/owspfm/elwha/theme/testpalettes/";

  // ------------------------------------------------------------- happy path

  @Test
  void aBundledPaletteLoadsBothSchemesComplete() {
    final Theme theme = PaletteLoader.loadTheme(BUNDLED_BLUE);

    assertThat(theme.name()).as("the top-level name becomes the theme name").isNotBlank();
    for (final ColorRole role : ColorRole.values()) {
      assertThat(theme.light().get(role)).as("light carries %s", role).isNotNull();
      assertThat(theme.dark().get(role)).as("dark carries %s", role).isNotNull();
    }
  }

  @Test
  void hexStringsDecodeToTheirColors() {
    final Theme theme = PaletteLoader.loadTheme(FIXTURES + "unnamed.json");

    assertThat(theme.light().get(ColorRole.SHADOW))
        .as("a #rrggbb string decodes to the color it names")
        .isEqualTo(Color.BLACK);
  }

  @Test
  void anAbsentNameFallsBackToTheResourcePath() {
    final Theme theme = PaletteLoader.loadTheme(FIXTURES + "unnamed.json");

    assertThat(theme.name())
        .as("a palette with no name is identified by where it was loaded from")
        .isEqualTo(FIXTURES + "unnamed.json");
  }

  @Test
  void unknownKeysAreIgnoredRatherThanRejected() {
    final Theme theme = PaletteLoader.loadTheme(FIXTURES + "unnamed.json");

    assertThat(theme.light().get(ColorRole.PRIMARY))
        .as("a key outside the role vocabulary is skipped and the rest still loads")
        .isNotNull();
  }

  @Test
  void everyLoadProducesAFreshTheme() {
    assertThat(PaletteLoader.loadTheme(BUNDLED_BLUE))
        .as("the loader does not cache — caching is MaterialPalettes' job")
        .isNotSameAs(PaletteLoader.loadTheme(BUNDLED_BLUE));
  }

  // --------------------------------------------------------- failure modes

  @Test
  void aNullResourcePathIsRejected() {
    assertThatThrownBy(() -> PaletteLoader.loadTheme(null))
        .as("a null path is a programming error, not a missing-resource case")
        .isInstanceOf(NullPointerException.class);
  }

  @Test
  void aMissingResourceNamesThePathItLookedFor() {
    assertThatThrownBy(() -> PaletteLoader.loadTheme("/nowhere/absent.json"))
        .as("a resource that is not on the classpath fails with the path it tried")
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessageContaining("/nowhere/absent.json");
  }

  @Test
  void aRootThatIsNotAnObjectIsRejected() {
    assertThatThrownBy(() -> PaletteLoader.loadTheme(FIXTURES + "not-an-object.json"))
        .as("the schema's root is a JSON object, not an array")
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessageContaining("Malformed palette resource");
  }

  @Test
  void anAbsentSchemeObjectIsRejected() {
    assertThatThrownBy(() -> PaletteLoader.loadTheme(FIXTURES + "no-light.json"))
        .as("both light and dark are required — a one-mode palette is not a theme")
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessageContaining("light");
  }

  @Test
  void aSchemeThatIsNotAnObjectIsRejected() {
    assertThatThrownBy(() -> PaletteLoader.loadTheme(FIXTURES + "light-not-an-object.json"))
        .as("a scheme must be an object of role keys")
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessageContaining("light");
  }

  @Test
  void anIncompletePaletteReportsHowManyRolesAreMissing() {
    assertThatThrownBy(() -> PaletteLoader.loadTheme(FIXTURES + "incomplete-light.json"))
        .as("completeness is enforced at load so no role can resolve to null later")
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessageContaining("48 role(s)");
  }

  @Test
  void anUndecodableColorNamesTheOffendingRole() {
    assertThatThrownBy(() -> PaletteLoader.loadTheme(FIXTURES + "invalid-hex.json"))
        .as("a bad hex value fails with the role that carries it, not a bare parse error")
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessageContaining("primary")
        .hasMessageContaining("not-a-color");
  }
}
