package com.owspfm.elwha.theme;

import static org.assertj.core.api.Assertions.assertThat;

import com.owspfm.elwha.testkit.EdtInterceptor;
import com.owspfm.elwha.testkit.ThemeExtension;
import java.awt.Color;
import java.util.EnumSet;
import java.util.Set;
import javax.swing.UIManager;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;

/**
 * Tier A coverage of the color half of the token vocabulary — the locked 49-role set, the
 * mechanical enum-to-key mapping, resolution against the installed palette in both modes, the
 * mode-invariance of the fixed accents, and the compiled-in baseline fallback. Oracle: {@code
 * docs/research/elwha-token-taxonomy.md} §1.
 */
@ExtendWith({EdtInterceptor.class, ThemeExtension.class})
class ColorRoleTest {

  /** The mode-invariant fixed accents — taxonomy §1.1 says these hold one value across modes. */
  private static final Set<ColorRole> FIXED_ACCENTS =
      EnumSet.of(
          ColorRole.PRIMARY_FIXED,
          ColorRole.PRIMARY_FIXED_DIM,
          ColorRole.ON_PRIMARY_FIXED,
          ColorRole.ON_PRIMARY_FIXED_VARIANT,
          ColorRole.SECONDARY_FIXED,
          ColorRole.SECONDARY_FIXED_DIM,
          ColorRole.ON_SECONDARY_FIXED,
          ColorRole.ON_SECONDARY_FIXED_VARIANT,
          ColorRole.TERTIARY_FIXED,
          ColorRole.TERTIARY_FIXED_DIM,
          ColorRole.ON_TERTIARY_FIXED,
          ColorRole.ON_TERTIARY_FIXED_VARIANT);

  /**
   * Roles a light and a dark scheme must not agree on. Deliberately not "every role": {@code
   * shadow} and {@code scrim} are pure black in both modes by M3 construction, and the fixed
   * accents are mode-invariant on purpose.
   */
  private static final Set<ColorRole> MUST_DIFFER_ACROSS_MODES =
      EnumSet.of(
          ColorRole.PRIMARY,
          ColorRole.ON_PRIMARY,
          ColorRole.PRIMARY_CONTAINER,
          ColorRole.SECONDARY,
          ColorRole.TERTIARY,
          ColorRole.ERROR,
          ColorRole.SURFACE,
          ColorRole.ON_SURFACE,
          ColorRole.SURFACE_VARIANT,
          ColorRole.SURFACE_CONTAINER,
          ColorRole.SURFACE_CONTAINER_HIGHEST,
          ColorRole.OUTLINE,
          ColorRole.OUTLINE_VARIANT,
          ColorRole.INVERSE_SURFACE,
          ColorRole.BACKGROUND,
          ColorRole.ON_BACKGROUND);

  // ------------------------------------------------------------- the role set

  @Test
  void roleSetIsTheFullM3Scheme() {
    assertThat(ColorRole.values())
        .as("the taxonomy locks the complete standard M3 scheme at 49 roles")
        .hasSize(49);
  }

  @Test
  void roleSetSplitsIntoThirtySevenModeVaryingAndTwelveFixed() {
    assertThat(FIXED_ACCENTS)
        .as("twelve accents are mode-invariant, leaving thirty-seven mode-varying roles")
        .hasSize(12);
  }

  // ------------------------------------------------------------ key mechanics

  @ParameterizedTest
  @EnumSource(ColorRole.class)
  void keyIsTheMechanicalCamelCaseOfTheEnumName(final ColorRole role) {
    assertThat(role.key())
        .as("SCREAMING_SNAKE maps mechanically to camelCase")
        .isEqualTo(camelCase(role.name()));
  }

  @ParameterizedTest
  @EnumSource(ColorRole.class)
  void uiKeyIsTheKeyUnderTheColorNamespace(final ColorRole role) {
    assertThat(role.uiKey())
        .as("every color role lives under Elwha.color.*")
        .isEqualTo("Elwha.color." + role.key());
  }

  @Test
  void keysAreUnique() {
    assertThat(EnumSet.allOf(ColorRole.class).stream().map(ColorRole::key).distinct().count())
        .as("no two roles share a token key")
        .isEqualTo(ColorRole.values().length);
  }

  // ------------------------------------------------------------- resolution

  @ParameterizedTest
  @EnumSource(
      value = Mode.class,
      names = {"LIGHT", "DARK"})
  void everyRoleResolvesUnderTheBaselinePalette(final Mode mode) {
    ThemeExtension.install(mode);

    for (final ColorRole role : ColorRole.values()) {
      assertThat(role.resolve()).as("%s resolves in %s mode", role, mode).isNotNull();
    }
  }

  @ParameterizedTest
  @EnumSource(
      value = Mode.class,
      names = {"LIGHT", "DARK"})
  void everyRoleResolvesToTheInstalledPalettesValue(final Mode mode) {
    ThemeExtension.install(mode);
    final Palette installed = MaterialPalettes.baseline().paletteFor(mode);

    for (final ColorRole role : ColorRole.values()) {
      assertThat(role.resolve().getRGB())
          .as("%s resolves to the installed palette's value, not a compiled-in constant", role)
          .isEqualTo(installed.get(role).getRGB());
    }
  }

  @Test
  void loadBearingRolesDifferBetweenLightAndDark() {
    final Palette light = MaterialPalettes.baseline().light();
    final Palette dark = MaterialPalettes.baseline().dark();

    for (final ColorRole role : MUST_DIFFER_ACROSS_MODES) {
      assertThat(light.get(role))
          .as("%s must not read the same in light and dark", role)
          .isNotEqualTo(dark.get(role));
    }
  }

  @Test
  void fixedAccentsHoldOneValueAcrossModes() {
    final Palette light = MaterialPalettes.baseline().light();
    final Palette dark = MaterialPalettes.baseline().dark();

    for (final ColorRole role : FIXED_ACCENTS) {
      assertThat(light.get(role))
          .as("%s is a fixed accent — the point is that a mode toggle does not move it", role)
          .isEqualTo(dark.get(role));
    }
  }

  @Test
  void switchingModeMovesTheResolvedValue() {
    ThemeExtension.install(Mode.LIGHT);
    final Color lightSurface = ColorRole.SURFACE.resolve();

    ThemeExtension.install(Mode.DARK);

    assertThat(ColorRole.SURFACE.resolve())
        .as("re-installing in the other mode re-resolves rather than serving a cached value")
        .isNotEqualTo(lightSurface);
  }

  // --------------------------------------------------------------- fallback

  @Test
  void anAbsentKeyFallsBackToTheCompiledBaseline() {
    UIManager.put(ColorRole.PRIMARY.uiKey(), null);

    assertThat(ColorRole.PRIMARY.resolve())
        .as("an unthemed role degrades to its compiled M3 baseline rather than returning null")
        .isEqualTo(ColorRole.PRIMARY.baseline());
  }

  @ParameterizedTest
  @EnumSource(ColorRole.class)
  void everyRoleCarriesABaselineValue(final ColorRole role) {
    assertThat(role.baseline()).as("%s has a last-ditch fallback color", role).isNotNull();
  }

  // ----------------------------------------------------------- on-pairings

  @Test
  void containerRolesPairWithTheirForeground() {
    assertThat(ColorRole.PRIMARY.on())
        .as("PRIMARY pairs with its foreground")
        .contains(ColorRole.ON_PRIMARY);
    assertThat(ColorRole.PRIMARY_CONTAINER.on())
        .as("the container pairs with the container foreground")
        .contains(ColorRole.ON_PRIMARY_CONTAINER);
    assertThat(ColorRole.SURFACE.on())
        .as("SURFACE pairs with ON_SURFACE")
        .contains(ColorRole.ON_SURFACE);
    assertThat(ColorRole.INVERSE_SURFACE.on())
        .as("the inverse pair follows the INVERSE_ON_SURFACE naming, not ON_INVERSE_SURFACE")
        .contains(ColorRole.INVERSE_ON_SURFACE);
  }

  @Test
  void foregroundAndUnpairedRolesReportNoPair() {
    assertThat(ColorRole.ON_PRIMARY.on()).as("a foreground has no foreground of its own").isEmpty();
    assertThat(ColorRole.OUTLINE.on()).as("an outline is not a container").isEmpty();
    assertThat(ColorRole.SURFACE_CONTAINER_HIGH.on())
        .as("the elevation ladder carries no defined pairing")
        .isEmpty();
    assertThat(ColorRole.SHADOW.on()).as("utility colors carry no pairing").isEmpty();
  }

  @ParameterizedTest
  @EnumSource(ColorRole.class)
  void aPairedForegroundIsNeverItselfPaired(final ColorRole role) {
    role.on()
        .ifPresent(
            foreground ->
                assertThat(foreground.on())
                    .as("%s pairs with %s, which must be a leaf foreground", role, foreground)
                    .isEmpty());
  }

  private static String camelCase(final String screamingSnake) {
    final StringBuilder camel = new StringBuilder();
    boolean upperNext = false;
    for (final char c : screamingSnake.toCharArray()) {
      if (c == '_') {
        upperNext = true;
        continue;
      }
      camel.append(upperNext ? Character.toUpperCase(c) : Character.toLowerCase(c));
      upperNext = false;
    }
    return camel.toString();
  }
}
