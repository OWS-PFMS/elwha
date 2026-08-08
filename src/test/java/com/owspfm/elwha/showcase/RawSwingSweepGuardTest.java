package com.owspfm.elwha.showcase;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

/**
 * The dogfood doctrine, enforced rather than remembered: the storefront demonstrates Elwha's own
 * primitives, never the raw Swing control they replace.
 *
 * <p>The six {@code *SweepGuard} classes are {@code main}s, runnable on their own for a readable
 * report. Nothing ran them together, which is how a sweep rots — so this drives all six on the same
 * scan the guards use, and CI fails on a regression instead of a maintainer noticing one.
 *
 * <p>It also holds the allowlists honest in both directions. An entry is an admission that one file
 * still needs a raw control, so a stale entry — kept after the file was swept, or after the file
 * was deleted — silently un-guards it. Those are asserted away too.
 */
class RawSwingSweepGuardTest {

  static List<Arguments> guards() {
    return List.of(
        Arguments.of("JButton", JButtonSweepGuard.ALLOWLIST),
        Arguments.of("JComboBox", JComboBoxSweepGuard.ALLOWLIST),
        Arguments.of("JCheckBox", JCheckBoxSweepGuard.ALLOWLIST),
        Arguments.of("JToggleButton", JToggleButtonSweepGuard.ALLOWLIST),
        Arguments.of("JRadioButton", JRadioButtonSweepGuard.ALLOWLIST),
        Arguments.of("JTextField", JTextFieldSweepGuard.ALLOWLIST));
  }

  @ParameterizedTest(name = "no raw {0} in the storefront")
  @MethodSource("guards")
  void storefrontDemonstratesElwhaNotRawSwing(
      final String widget, final Map<String, String> allowlist) throws IOException {
    assertThat(RawSwingSweep.violations(widget, allowlist))
        .as(
            "the Showcase and the playgrounds are what a consumer reads to learn the library — a "
                + "raw %s there teaches the wrong thing (#424 / #321)",
            widget)
        .isEmpty();
  }

  @ParameterizedTest(name = "{0} allowlist carries no stale entry")
  @MethodSource("guards")
  void everyAllowlistedKeepIsStillAKeep(final String widget, final Map<String, String> allowlist)
      throws IOException {
    for (final Map.Entry<String, String> keep : allowlist.entrySet()) {
      assertThat(RawSwingSweep.stillNeedsExemption(widget, keep.getKey()))
          .as(
              "%s no longer constructs a raw %s, so its allowlist entry (%s) exempts it from a "
                  + "guard it would now pass — drop the entry rather than leave the file unguarded",
              keep.getKey(), widget, keep.getValue())
          .isTrue();
    }
  }

  @Test
  void guardedSurfaceIsTheStorefront() throws IOException {
    final List<String> guarded =
        RawSwingSweep.guardedFiles().stream().map(path -> path.toString()).toList();

    assertThat(guarded)
        .as("a scan that found nothing would pass every guard vacuously")
        .hasSizeGreaterThan(60);
    assertThat(guarded)
        .as("the Showcase itself is the thing being guarded")
        .anyMatch(path -> path.endsWith("showcase/ElwhaShowcase.java"));
    assertThat(guarded)
        .as("every component's playground is guarded, not a hardcoded subset (#424 widened #317)")
        .anyMatch(path -> path.contains("navrail/playground/"))
        .anyMatch(path -> path.contains("selectfield/playground/"));
    assertThat(guarded)
        .as("the frozen card/fixes harnesses stay out of scope, as they were for #317")
        .noneMatch(path -> path.contains("card/fixes/"));
  }
}
