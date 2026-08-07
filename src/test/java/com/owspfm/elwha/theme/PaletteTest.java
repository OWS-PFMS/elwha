package com.owspfm.elwha.theme;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.owspfm.elwha.testkit.EdtInterceptor;
import com.owspfm.elwha.testkit.ThemeExtension;
import java.awt.Color;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

/**
 * Tier A coverage of the role-to-color map itself — the completeness gate that makes {@link
 * Palette#get} a never-null contract, and the builder's own argument validation. Oracle: {@code
 * docs/research/elwha-theme-install-api.md} §1.1.
 */
@ExtendWith({EdtInterceptor.class, ThemeExtension.class})
class PaletteTest {

  private static Palette.Builder complete() {
    final Palette.Builder builder = Palette.builder();
    for (final ColorRole role : ColorRole.values()) {
      builder.set(role, new Color(role.ordinal(), 0, 0));
    }
    return builder;
  }

  @Test
  void aCompletePaletteReturnsEveryRoleItWasGiven() {
    final Palette palette = complete().build();

    for (final ColorRole role : ColorRole.values()) {
      assertThat(palette.get(role))
          .as("%s round-trips through the palette", role)
          .isEqualTo(new Color(role.ordinal(), 0, 0));
    }
  }

  @Test
  void anIncompletePaletteRefusesToBuildAndNamesWhatIsMissing() {
    final Palette.Builder builder = Palette.builder().set(ColorRole.PRIMARY, Color.RED);

    assertThatThrownBy(builder::build)
        .as("a partial palette would resolve some roles to null at paint time — fail at build")
        .isInstanceOf(IllegalStateException.class)
        .hasMessageContaining("48 role(s)")
        .hasMessageContaining("ON_PRIMARY");
  }

  @Test
  void anEmptyPaletteReportsEveryRoleAtOnce() {
    assertThatThrownBy(() -> Palette.builder().build())
        .as("all missing roles are reported in one pass rather than one per rebuild")
        .isInstanceOf(IllegalStateException.class)
        .hasMessageContaining("49 role(s)");
  }

  @Test
  void aLaterAssignmentReplacesAnEarlierOne() {
    final Palette palette = complete().set(ColorRole.PRIMARY, Color.GREEN).build();

    assertThat(palette.get(ColorRole.PRIMARY))
        .as("the builder is an accumulator — the last write for a role wins")
        .isEqualTo(Color.GREEN);
  }

  @Test
  void theBuilderRejectsNullArguments() {
    assertThatThrownBy(() -> Palette.builder().set(null, Color.RED))
        .as("a null role has no key to store under")
        .isInstanceOf(NullPointerException.class);
    assertThatThrownBy(() -> Palette.builder().set(ColorRole.PRIMARY, null))
        .as("a null color is what completeness exists to prevent")
        .isInstanceOf(NullPointerException.class);
  }

  @Test
  void aBuiltPaletteDoesNotFollowLaterBuilderWrites() {
    final Palette.Builder builder = complete();
    final Palette snapshot = builder.build();

    builder.set(ColorRole.PRIMARY, Color.MAGENTA);

    assertThat(snapshot.get(ColorRole.PRIMARY))
        .as("build() copies — a palette is immutable once handed out")
        .isNotEqualTo(Color.MAGENTA);
  }
}
