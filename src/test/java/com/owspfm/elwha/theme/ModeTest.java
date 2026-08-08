package com.owspfm.elwha.theme;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assumptions.assumeFalse;

import com.owspfm.elwha.testkit.EdtInterceptor;
import com.owspfm.elwha.testkit.ThemeExtension;
import java.lang.reflect.Method;
import java.util.Locale;
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
  void modelIsTwoConcreteModesAndOneFollowTheOs() {
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

  // ------------------------------------------------------------- probe bound

  /**
   * The probe is private, and deliberately so — but the bound it now carries is the whole point of
   * #655, and nothing else in the class exposes it. Reflection is the only seam.
   */
  private static Object runProbe(final String... command) throws Exception {
    final Method probe = Mode.class.getDeclaredMethod("runProbe", String[].class);
    probe.setAccessible(true);
    return probe.invoke(null, (Object) command);
  }

  @Test
  void aStalledProbeIsKilledRatherThanWaitedOn() throws Exception {
    assumeFalse(
        System.getProperty("os.name", "").toLowerCase(Locale.ROOT).contains("win"),
        "needs a POSIX sleep to stall on");

    final long startedAt = System.nanoTime();
    final Object result = runProbe("sleep", "20");
    final long elapsedMs = (System.nanoTime() - startedAt) / 1_000_000L;

    assertThat(result)
        .as("a probe that outruns its deadline reads as a failure, so SYSTEM falls back to LIGHT")
        .isNull();
    assertThat(elapsedMs)
        .as("install runs this on the EDT, so the wait is bounded — not the child's whole lifetime")
        .isLessThan(5_000L);
  }

  @Test
  void aProbeThatAnswersIsStillRead() throws Exception {
    assumeFalse(
        System.getProperty("os.name", "").toLowerCase(Locale.ROOT).contains("win"),
        "needs a POSIX echo to answer");

    assertThat(runProbe("echo", "Dark"))
        .as("bounding the wait must not cost us the output of a probe that does answer in time")
        .asString()
        .contains("Dark");
  }
}
