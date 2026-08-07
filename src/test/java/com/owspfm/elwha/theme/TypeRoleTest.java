package com.owspfm.elwha.theme;

import static org.assertj.core.api.Assertions.assertThat;

import com.owspfm.elwha.testkit.EdtInterceptor;
import com.owspfm.elwha.testkit.ThemeExtension;
import java.awt.Font;
import java.util.EnumMap;
import java.util.EnumSet;
import java.util.Map;
import java.util.Set;
import javax.swing.UIManager;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;

/**
 * Tier A coverage of the type half of the token vocabulary — the full 15-role M3 scale (the display
 * tier landed with the app bar in v0.4.0), the locked point sizes, the 400/500 weight split, and
 * resolution to the bundled Inter faces. Oracle: {@code docs/research/elwha-token-taxonomy.md} §2
 * plus its 2026-06-11 display-tier amendment.
 */
@ExtendWith({EdtInterceptor.class, ThemeExtension.class})
class TypeRoleTest {

  /** The locked point-size table from taxonomy §2, with the display tier from the amendment. */
  private static final Map<TypeRole, Integer> LOCKED_PT = new EnumMap<>(TypeRole.class);

  /** The M3 500-weight roles; every other role is Regular (400). */
  private static final Set<TypeRole> MEDIUM_WEIGHT_ROLES =
      EnumSet.of(
          TypeRole.TITLE_MEDIUM,
          TypeRole.TITLE_SMALL,
          TypeRole.LABEL_LARGE,
          TypeRole.LABEL_MEDIUM,
          TypeRole.LABEL_SMALL);

  static {
    LOCKED_PT.put(TypeRole.DISPLAY_LARGE, 57);
    LOCKED_PT.put(TypeRole.DISPLAY_MEDIUM, 45);
    LOCKED_PT.put(TypeRole.DISPLAY_SMALL, 36);
    LOCKED_PT.put(TypeRole.HEADLINE_LARGE, 32);
    LOCKED_PT.put(TypeRole.HEADLINE_MEDIUM, 28);
    LOCKED_PT.put(TypeRole.HEADLINE_SMALL, 24);
    LOCKED_PT.put(TypeRole.TITLE_LARGE, 22);
    LOCKED_PT.put(TypeRole.TITLE_MEDIUM, 16);
    LOCKED_PT.put(TypeRole.TITLE_SMALL, 14);
    LOCKED_PT.put(TypeRole.BODY_LARGE, 16);
    LOCKED_PT.put(TypeRole.BODY_MEDIUM, 14);
    LOCKED_PT.put(TypeRole.BODY_SMALL, 12);
    LOCKED_PT.put(TypeRole.LABEL_LARGE, 14);
    LOCKED_PT.put(TypeRole.LABEL_MEDIUM, 12);
    LOCKED_PT.put(TypeRole.LABEL_SMALL, 11);
  }

  @Test
  void scaleIsTheFullFifteenRoleM3Ladder() {
    assertThat(TypeRole.values())
        .as("five sizes across display, headline, title, body, and label")
        .hasSize(15);
    assertThat(LOCKED_PT.keySet())
        .as("every shipped role appears in the locked size table")
        .containsExactlyInAnyOrder(TypeRole.values());
  }

  @Test
  void everyTierShipsLargeMediumAndSmall() {
    for (final String tier : new String[] {"DISPLAY", "HEADLINE", "TITLE", "BODY", "LABEL"}) {
      assertThat(
              EnumSet.allOf(TypeRole.class).stream()
                  .filter(role -> role.name().startsWith(tier))
                  .count())
          .as("the %s tier ships large, medium, and small", tier)
          .isEqualTo(3);
    }
  }

  @ParameterizedTest
  @EnumSource(TypeRole.class)
  void everyRoleCarriesItsLockedPointSize(final TypeRole role) {
    assertThat(role.pt())
        .as("%s is locked at %dpt in the type table", role, LOCKED_PT.get(role))
        .isEqualTo(LOCKED_PT.get(role));
  }

  @ParameterizedTest
  @EnumSource(TypeRole.class)
  void weightSplitFollowsTheM3Table(final TypeRole role) {
    assertThat(role.medium())
        .as("%s is a %s-weight role", role, MEDIUM_WEIGHT_ROLES.contains(role) ? "500" : "400")
        .isEqualTo(MEDIUM_WEIGHT_ROLES.contains(role));
  }

  @ParameterizedTest
  @EnumSource(TypeRole.class)
  void uiKeyIsTheKeyUnderTheTypeNamespace(final TypeRole role) {
    assertThat(role.uiKey())
        .as("every type role lives under Elwha.type.*")
        .isEqualTo("Elwha.type." + role.key());
  }

  // ------------------------------------------------------------- resolution

  @ParameterizedTest
  @EnumSource(TypeRole.class)
  void everyRoleResolvesToTheBundledInterAtItsLockedSize(final TypeRole role) {
    final Font resolved = role.resolve();

    assertThat(resolved).as("%s resolves a real font", role).isNotNull();
    // The bundled static Medium TTF declares its own AWT family ("Inter Medium"), so a
    // medium-weight role legitimately reports a different family string than a regular one —
    // which is the proof that the 500 weight is a real face, not a synthesized bold.
    assertThat(resolved.getFamily())
        .as("%s renders in the bundled Inter faces, never a platform logical font", role)
        .isEqualTo(role.medium() ? "Inter Medium" : "Inter");
    assertThat(resolved.getSize())
        .as("%s resolves at its locked point size", role)
        .isEqualTo(LOCKED_PT.get(role));
  }

  @Test
  void bundledFamilyIsInter() {
    assertThat(Typography.defaults().familyName())
        .as("Elwha ships Inter so the M3 400/500 weight distinction has real faces")
        .isEqualTo("Inter");
  }

  @Test
  void mediumAndRegularRolesAtOneSizeResolveDifferentFaces() {
    assertThat(TypeRole.TITLE_MEDIUM.resolve().getFontName())
        .as("16pt medium and 16pt regular are different faces, not the same font twice")
        .isNotEqualTo(TypeRole.BODY_LARGE.resolve().getFontName());
    assertThat(TypeRole.TITLE_MEDIUM.resolve().getSize())
        .as("the two roles agree on size — only the weight differs")
        .isEqualTo(TypeRole.BODY_LARGE.resolve().getSize());
  }

  @Test
  void anAbsentKeyFallsBackToASizedSansSerif() {
    UIManager.put(TypeRole.BODY_MEDIUM.uiKey(), null);
    final Font fallback = TypeRole.BODY_MEDIUM.resolve();

    assertThat(fallback)
        .as("an unthemed role degrades to a best-effort font rather than returning null")
        .isNotNull();
    assertThat(fallback.getSize())
        .as("the fallback still honors the role's point size")
        .isEqualTo(14);
  }

  @Test
  void switchingModeLeavesTypographyAlone() {
    final Font light = TypeRole.BODY_MEDIUM.resolve();

    ThemeExtension.install(Mode.DARK);

    assertThat(TypeRole.BODY_MEDIUM.resolve())
        .as("type roles are mode-invariant — only color moves on a mode switch")
        .isEqualTo(light);
  }
}
