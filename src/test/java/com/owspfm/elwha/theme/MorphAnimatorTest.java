package com.owspfm.elwha.theme;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.within;

import com.owspfm.elwha.testkit.EdtInterceptor;
import com.owspfm.elwha.testkit.ThemeExtension;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

/**
 * Tier A coverage of the animator's <em>global</em> switches only — the reduced-motion flag every
 * fixture pins and the duration multiplier the Showcase drives. Nothing here starts a timer or
 * samples a phase: per testing.md determinism rule 1, frame-level behavior is asserted by injecting
 * progress into the stateless painters, never by racing a clock.
 */
@ExtendWith({EdtInterceptor.class, ThemeExtension.class})
class MorphAnimatorTest {

  @AfterEach
  void restoreTheGlobals() {
    MorphAnimator.setDurationMultiplier(1f);
  }

  @Test
  void fixtureLeavesReducedMotionPinnedOn() {
    assertThat(MorphAnimator.isReducedMotion())
        .as("an unpinned run reads the host OS accessibility setting and stops being reproducible")
        .isTrue();
  }

  @Test
  void reducedMotionFlagIsAGlobalReadBack() {
    MorphAnimator.setReducedMotion(false);
    assertThat(MorphAnimator.isReducedMotion())
        .as("the toggle is one JVM-wide switch, not per-animator state")
        .isFalse();

    MorphAnimator.setReducedMotion(true);
    assertThat(MorphAnimator.isReducedMotion()).as("and it flips back").isTrue();
  }

  @Test
  void durationMultiplierDefaultsToRealTime() {
    assertThat(MorphAnimator.durationMultiplier())
        .as("the §3 durations are pinned values, so the multiplier starts at one")
        .isEqualTo(1f, within(0.0001f));
  }

  @Test
  void durationMultiplierRoundTrips() {
    MorphAnimator.setDurationMultiplier(5f);

    assertThat(MorphAnimator.durationMultiplier())
        .as("the Showcase slows morphs down for observation through this one knob")
        .isEqualTo(5f, within(0.0001f));
  }

  @Test
  void durationMultiplierRefusesAZeroOrNegativeTickStep() {
    MorphAnimator.setDurationMultiplier(0f);
    assertThat(MorphAnimator.durationMultiplier())
        .as("a zero multiplier would divide the tick step by nothing, so it clamps")
        .isEqualTo(0.1f, within(0.0001f));

    MorphAnimator.setDurationMultiplier(-3f);
    assertThat(MorphAnimator.durationMultiplier())
        .as("a negative multiplier clamps to the same floor")
        .isEqualTo(0.1f, within(0.0001f));
  }

  @Test
  void motionDurationsAreTheLockedM3Tokens() {
    assertThat(MorphAnimator.SHORT3_MS).as("short3 is 150ms").isEqualTo(150);
    assertThat(MorphAnimator.MEDIUM2_MS).as("medium2 is 300ms").isEqualTo(300);
    assertThat(MorphAnimator.MEDIUM3_MS).as("medium3 is 350ms").isEqualTo(350);
  }
}
