package com.owspfm.elwha.theme;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.owspfm.elwha.testkit.EdtInterceptor;
import com.owspfm.elwha.testkit.ThemeExtension;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

/**
 * Tier A coverage of the immutable install input — which fields are required, what the unset ones
 * default to, and that the {@code with*} derivations copy rather than mutate (they are what makes
 * {@code install(current().withMode(DARK))} the whole dark-mode toggle). Oracle: {@code
 * docs/research/elwha-theme-install-api.md} §2.
 */
@ExtendWith({EdtInterceptor.class, ThemeExtension.class})
class ConfigTest {

  private static Config.Builder builder() {
    return ElwhaTheme.config().theme(MaterialPalettes.baseline());
  }

  // ---------------------------------------------------------------- builder

  @Test
  void aThemeIsTheOnlyRequiredField() {
    final Config config = builder().build();

    assertThat(config.theme()).as("the theme round-trips").isSameAs(MaterialPalettes.baseline());
    assertThat(config.mode()).as("mode defaults to following the OS").isEqualTo(Mode.SYSTEM);
    assertThat(config.typography())
        .as("typography defaults to the bundled Inter")
        .isSameAs(Typography.defaults());
    assertThat(config.reducedMotion())
        .as("no reduced-motion override means defer to the OS signal")
        .isNull();
  }

  @Test
  void buildingWithoutAThemeFailsWithAnActionableMessage() {
    assertThatThrownBy(() -> ElwhaTheme.config().build())
        .as("there is no implicit palette — the caller must pick one")
        .isInstanceOf(IllegalStateException.class)
        .hasMessageContaining("theme(...)");
  }

  @Test
  void theBuilderRejectsNullsForEveryTypedField() {
    assertThatThrownBy(() -> ElwhaTheme.config().theme(null))
        .as("a null theme is rejected at the setter, not deferred to build()")
        .isInstanceOf(NullPointerException.class);
    assertThatThrownBy(() -> ElwhaTheme.config().mode(null))
        .as("a null mode is rejected — SYSTEM is the way to say 'let the OS decide'")
        .isInstanceOf(NullPointerException.class);
    assertThatThrownBy(() -> ElwhaTheme.config().typography(null))
        .as("a null typography is rejected — omit the call to take the default")
        .isInstanceOf(NullPointerException.class);
  }

  @Test
  void aNullReducedMotionOverrideIsTheDocumentedWayToDeferToTheOs() {
    assertThat(builder().reducedMotion(null).build().reducedMotion())
        .as("the tri-state override accepts null on purpose, unlike the other fields")
        .isNull();
    assertThat(builder().reducedMotion(true).build().reducedMotion())
        .as("an explicit true forces snap-to-target")
        .isTrue();
  }

  @Test
  void theBuilderIsChainable() {
    final Config config =
        ElwhaTheme.config()
            .theme(MaterialPalettes.baseline())
            .mode(Mode.DARK)
            .typography(Typography.defaults())
            .reducedMotion(true)
            .build();

    assertThat(config.mode()).as("every setter returns the builder").isEqualTo(Mode.DARK);
    assertThat(config.reducedMotion()).as("the last field in the chain still lands").isTrue();
  }

  @Test
  void eachBuildProducesAFreshConfig() {
    final Config.Builder shared = builder();

    assertThat(shared.build())
        .as("a config is a snapshot of the builder, not a live view of it")
        .isNotSameAs(shared.build());
  }

  // ------------------------------------------------------------ derivations

  @Test
  void withModeCopiesEverythingElse() {
    final Config original = builder().mode(Mode.LIGHT).reducedMotion(true).build();

    final Config derived = original.withMode(Mode.DARK);

    assertThat(derived.mode()).as("the named field changes").isEqualTo(Mode.DARK);
    assertThat(original.mode()).as("the original is untouched").isEqualTo(Mode.LIGHT);
    assertThat(derived.theme()).as("the theme carries over").isSameAs(original.theme());
    assertThat(derived.typography())
        .as("the typography carries over")
        .isSameAs(original.typography());
    assertThat(derived.reducedMotion())
        .as("the reduced-motion override carries over")
        .isEqualTo(original.reducedMotion());
  }

  @Test
  void withThemeCopiesEverythingElse() {
    final Theme other = MaterialPalettes.secondary().get(0);
    final Config original = builder().mode(Mode.DARK).build();

    final Config derived = original.withTheme(other);

    assertThat(derived.theme()).as("the named field changes").isSameAs(other);
    assertThat(derived.mode()).as("the mode survives a theme swap").isEqualTo(Mode.DARK);
  }

  @Test
  void withTypographyCopiesEverythingElse() {
    final Config original = builder().mode(Mode.DARK).build();

    final Config derived = original.withTypography(Typography.ofFamily("Inter"));

    assertThat(derived.typography())
        .as("the named field changes")
        .isNotSameAs(original.typography());
    assertThat(derived.mode()).as("the mode survives a typography swap").isEqualTo(Mode.DARK);
  }

  @Test
  void withReducedMotionAcceptsNullToHandControlBackToTheOs() {
    final Config original = builder().reducedMotion(true).build();

    assertThat(original.withReducedMotion(null).reducedMotion())
        .as("clearing the override is a supported derivation, not an error")
        .isNull();
  }

  @Test
  void theDerivationsRejectNullsForTheirTypedFields() {
    final Config original = builder().build();

    assertThatThrownBy(() -> original.withTheme(null))
        .as("a derived config is validated exactly like a built one")
        .isInstanceOf(NullPointerException.class);
    assertThatThrownBy(() -> original.withMode(null))
        .as("null mode stays rejected on the derivation path")
        .isInstanceOf(NullPointerException.class);
    assertThatThrownBy(() -> original.withTypography(null))
        .as("null typography stays rejected on the derivation path")
        .isInstanceOf(NullPointerException.class);
  }
}
