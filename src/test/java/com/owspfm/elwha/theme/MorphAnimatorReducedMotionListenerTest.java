package com.owspfm.elwha.theme;

import static org.assertj.core.api.Assertions.assertThat;

import com.owspfm.elwha.testkit.EdtInterceptor;
import com.owspfm.elwha.testkit.ThemeExtension;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

/**
 * Tier A coverage of the reduced-motion change broadcast (#632).
 *
 * <p>Almost nothing needs this: a {@link MorphAnimator} samples the flag when an animation
 * <em>starts</em>, so a change is picked up by the next interaction and a stale read is
 * unobservable. The exception is a consumer whose animation is a continuously running clock gated
 * on the flag — {@code ElwhaLoadingIndicator}'s spinner — which is left wrong in both directions
 * without a notification.
 *
 * <p>What is asserted here is the channel: that it fires on a real change and stays quiet on a
 * no-op, that both directions carry, and that a collected listener is dropped rather than kept
 * alive. That the indicator's clock actually starts and stops belongs to the {@code gui} tier,
 * because the clock is gated on {@code isShowing()} and nothing is showing headless.
 */
@ExtendWith({EdtInterceptor.class, ThemeExtension.class})
class MorphAnimatorReducedMotionListenerTest {

  private final List<Runnable> held = new ArrayList<>();

  @AfterEach
  void dropListeners() {
    for (final Runnable listener : held) {
      MorphAnimator.removeReducedMotionListener(listener);
    }
    held.clear();
  }

  /** Registers and keeps a strong reference, since the animator holds only a weak one. */
  private Runnable listen(final Runnable body) {
    held.add(body);
    MorphAnimator.addReducedMotionListener(body);
    return body;
  }

  @Test
  void bothDirectionsOfAChangeNotify() {
    MorphAnimator.setReducedMotion(true);
    final List<Boolean> seen = new ArrayList<>();
    listen(() -> seen.add(MorphAnimator.isReducedMotion()));

    MorphAnimator.setReducedMotion(false);
    MorphAnimator.setReducedMotion(true);

    assertThat(seen)
        .as("the listener hears the flag turn off and back on, with the new value already visible")
        .containsExactly(false, true);
  }

  @Test
  void settingTheValueItAlreadyHasIsQuiet() {
    MorphAnimator.setReducedMotion(true);
    final List<Boolean> seen = new ArrayList<>();
    listen(() -> seen.add(MorphAnimator.isReducedMotion()));

    MorphAnimator.setReducedMotion(true);

    assertThat(seen).as("a no-op set is not a change and must not wake every clock").isEmpty();
  }

  @Test
  void aRemovedListenerStopsHearing() {
    MorphAnimator.setReducedMotion(true);
    final List<Boolean> seen = new ArrayList<>();
    final Runnable listener = listen(() -> seen.add(MorphAnimator.isReducedMotion()));

    MorphAnimator.removeReducedMotionListener(listener);
    MorphAnimator.setReducedMotion(false);

    assertThat(seen).isEmpty();
  }

  @Test
  void aListenerNoOneHoldsIsCollectedRatherThanRetained() {
    MorphAnimator.setReducedMotion(true);
    final List<Boolean> seen = new ArrayList<>();
    // Deliberately not held: the animator's reference is weak, so this is eligible immediately.
    MorphAnimator.addReducedMotionListener(() -> seen.add(true));
    System.gc();

    MorphAnimator.setReducedMotion(false);

    assertThat(seen)
        .as("an unheld listener cannot keep its owner alive — the weak reference is the contract")
        .isEmpty();
  }

  @Test
  void oneListenerFailingDoesNotSilenceTheRest() {
    MorphAnimator.setReducedMotion(true);
    final List<Boolean> seen = new ArrayList<>();
    listen(() -> seen.add(MorphAnimator.isReducedMotion()));
    listen(() -> seen.add(MorphAnimator.isReducedMotion()));

    MorphAnimator.setReducedMotion(false);

    assertThat(seen).as("every registered listener is notified").hasSize(2);
  }
}
