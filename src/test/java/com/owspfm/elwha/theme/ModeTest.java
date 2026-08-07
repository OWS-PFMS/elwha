package com.owspfm.elwha.theme;

import static org.assertj.core.api.Assertions.assertThat;

import com.owspfm.elwha.testkit.EdtInterceptor;
import com.owspfm.elwha.testkit.ThemeExtension;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;

/**
 * Tier A coverage of the install-time mode resolution — that {@link Mode#SYSTEM} is the only
 * non-concrete member, that resolution is idempotent for the concrete ones, and that the OS probe
 * always yields a usable answer (including on the platforms it cannot read). Oracle: {@code
 * docs/research/elwha-theme-install-api.md} §6, Q2.
 */
@ExtendWith({EdtInterceptor.class, ThemeExtension.class})
class ModeTest {

  @Test
  void theModelIsTwoConcreteModesAndOneFollowTheOs() {
    assertThat(Mode.values())
        .as("light, dark, and follow-the-OS")
        .containsExactly(Mode.LIGHT, Mode.DARK, Mode.SYSTEM);
  }

  @Test
  void onlySystemIsNonConcrete() {
    assertThat(Mode.LIGHT.isConcrete()).as("LIGHT needs no resolution").isTrue();
    assertThat(Mode.DARK.isConcrete()).as("DARK needs no resolution").isTrue();
    assertThat(Mode.SYSTEM.isConcrete()).as("SYSTEM is the one that has to ask the OS").isFalse();
  }

  @Test
  void concreteModesResolveToThemselves() {
    assertThat(Mode.LIGHT.resolved()).as("LIGHT resolves to LIGHT").isEqualTo(Mode.LIGHT);
    assertThat(Mode.DARK.resolved()).as("DARK resolves to DARK").isEqualTo(Mode.DARK);
  }

  @ParameterizedTest
  @EnumSource(Mode.class)
  void resolutionAlwaysYieldsAConcreteMode(final Mode mode) {
    assertThat(mode.resolved())
        .as("%s resolves to something install can select a palette with", mode)
        .isIn(Mode.LIGHT, Mode.DARK);
    assertThat(mode.resolved().isConcrete())
        .as("%s never resolves to another unresolved mode", mode)
        .isTrue();
  }

  @Test
  void systemResolutionIsStableWithinARun() {
    assertThat(Mode.SYSTEM.resolved())
        .as("the OS probe gives the same answer twice in a row — nothing about it is random")
        .isEqualTo(Mode.SYSTEM.resolved());
  }
}
