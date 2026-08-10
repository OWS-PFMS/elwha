package com.owspfm.elwha.theme;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.owspfm.elwha.testkit.EdtInterceptor;
import com.owspfm.elwha.testkit.ThemeExtension;
import java.awt.Font;
import java.awt.GraphicsEnvironment;
import java.awt.font.TextAttribute;
import java.util.Arrays;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;

/**
 * Tier A coverage of the typography half of a {@link Config} — the bundled-Inter defaults, the
 * registration that makes Inter resolvable as an installed family, and the {@link
 * Typography#ofFamily(String)} path with its {@link TextAttribute#WEIGHT_MEDIUM} fallback for
 * families with no Medium face. Oracle: {@code docs/research/elwha-token-taxonomy.md} §2.2 (Q3).
 */
@ExtendWith({EdtInterceptor.class, ThemeExtension.class})
class TypographyTest {

  @Test
  void defaultsAreCachedRatherThanRebuiltPerCall() {
    assertThat(Typography.defaults())
        .as("the bundled faces are loaded and registered once, then shared")
        .isSameAs(Typography.defaults());
  }

  @Test
  void defaultsBuildOnTheBundledInterFamily() {
    assertThat(Typography.defaults().familyName())
        .as("Inter is bundled so the M3 400/500 split has real faces — not a synthetic bold")
        .isEqualTo("Inter");
  }

  @Test
  void loadingTheDefaultsRegistersInterWithTheGraphicsEnvironment() {
    Typography.defaults();

    assertThat(
            Arrays.asList(
                GraphicsEnvironment.getLocalGraphicsEnvironment().getAvailableFontFamilyNames()))
        .as("registration is what lets a consumer name the family directly")
        .contains("Inter");
  }

  @ParameterizedTest
  @EnumSource(TypeRole.class)
  void everyRoleGetsASizedFace(final TypeRole role) {
    final Font font = Typography.defaults().get(role);

    assertThat(font).as("%s has a font", role).isNotNull();
    assertThat(font.getFamily())
        .as("%s is derived from one of the two bundled Inter faces", role)
        .isEqualTo(role.medium() ? "Inter Medium" : "Inter");
    assertThat(font.getSize()).as("%s is derived to its own point size", role).isEqualTo(role.pt());
  }

  @Test
  void mediumRolesTakeTheMediumFaceNotTheRegularOne() {
    final Typography typography = Typography.defaults();

    assertThat(typography.get(TypeRole.LABEL_LARGE).getFontName())
        .as("a 500-weight role resolves the real Medium face")
        .isNotEqualTo(typography.get(TypeRole.BODY_MEDIUM).getFontName());
  }

  @Test
  void familyNameReportsTheRegularFaceEvenThoughMediumRolesCarryTheirOwn() {
    assertThat(Typography.defaults().familyName())
        .as("the reported family is the Regular face's — the static Medium TTF names itself apart")
        .isEqualTo(Typography.defaults().get(TypeRole.BODY_MEDIUM).getFamily());
    assertThat(Typography.defaults().get(TypeRole.LABEL_LARGE).getFamily())
        .as("the medium roles resolve the separately-named Medium face")
        .isEqualTo("Inter Medium");
  }

  @Test
  void ofFamilyRoundTripsTheRequestedFamily() {
    Typography.defaults();

    assertThat(Typography.ofFamily("Inter").familyName())
        .as("the requested family is what the typography reports")
        .isEqualTo("Inter");
  }

  @Test
  void ofFamilyAsksForMediumWeightWhenTheFamilyHasNoMediumFace() {
    final Typography typography = Typography.ofFamily(Font.SANS_SERIF);

    assertThat(new Font(Font.SANS_SERIF + " Medium", Font.PLAIN, 12).getFamily())
        .as("the premise: no 'SansSerif Medium' is installed, so a lookup lands on Dialog")
        .isNotEqualTo(Font.SANS_SERIF + " Medium");
    assertThat(typography.get(TypeRole.TITLE_MEDIUM).getAttributes().get(TextAttribute.WEIGHT))
        .as("a 500-weight role requests WEIGHT_MEDIUM, honored where the family supplies glyphs")
        .isEqualTo(TextAttribute.WEIGHT_MEDIUM);
    assertThat(typography.get(TypeRole.BODY_MEDIUM).getAttributes().get(TextAttribute.WEIGHT))
        .as("a 400-weight role carries no weight override")
        .isNotEqualTo(TextAttribute.WEIGHT_MEDIUM);
  }

  @Test
  void ofFamilyPrefersARealMediumFaceOverSynthesis() {
    Typography.defaults();

    final Typography typography = Typography.ofFamily("Inter");

    assertThat(typography.get(TypeRole.LABEL_LARGE).getFamily())
        .as("Inter ships a real 500 face, and a real face always beats a synthesised weight")
        .isEqualTo("Inter Medium");
    assertThat(typography.get(TypeRole.BODY_MEDIUM).getFamily())
        .as("the 400-weight roles still take the family that was asked for")
        .isEqualTo("Inter");
  }

  @Test
  void aTypographyRoundTripsThroughItsOwnFamilyNameWithoutLosingTheMediumFace() {
    final Typography original = Typography.defaults();

    final Typography roundTripped = Typography.ofFamily(original.familyName());

    for (final TypeRole role : TypeRole.values()) {
      assertThat(roundTripped.get(role).getFamily())
          .as(
              "%s survives ofFamily(familyName()) — the round trip a consumer actually writes",
              role)
          .isEqualTo(original.get(role).getFamily());
      assertThat(roundTripped.get(role).getSize())
          .as("%s keeps its point size across the round trip", role)
          .isEqualTo(original.get(role).getSize());
    }
  }

  @ParameterizedTest
  @EnumSource(TypeRole.class)
  void ofFamilyCoversEveryRoleAtItsLockedSize(final TypeRole role) {
    Typography.defaults();

    assertThat(Typography.ofFamily("Inter").get(role).getSize())
        .as("%s keeps its point size on a consumer-supplied family", role)
        .isEqualTo(role.pt());
  }

  @Test
  void ofFamilyRejectsANullFamily() {
    assertThatThrownBy(() -> Typography.ofFamily(null))
        .as("a null family name is a programming error, not a fallback trigger")
        .isInstanceOf(NullPointerException.class);
  }

  @Test
  void installedTypographyIsWhatTheTypeRolesResolve() {
    ElwhaTheme.install(
        ElwhaTheme.config()
            .theme(MaterialPalettes.baseline())
            .mode(Mode.LIGHT)
            .typography(Typography.ofFamily("Inter"))
            .build());

    assertThat(TypeRole.BODY_MEDIUM.resolve().getSize())
        .as("install writes the config's typography into the type keys")
        .isEqualTo(TypeRole.BODY_MEDIUM.pt());
    assertThat(TypeRole.BODY_MEDIUM.resolve().getFamily())
        .as("a consumer-supplied typography reaches the resolved roles")
        .isEqualTo("Inter");
  }

  @Test
  void installWritesTheGlobalDefaultFontFromBodyMedium() {
    assertThat(javax.swing.UIManager.getFont("defaultFont").getSize())
        .as("raw Swing inherits bodyMedium as its default font")
        .isEqualTo(TypeRole.BODY_MEDIUM.pt());
  }
}
