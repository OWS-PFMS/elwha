package com.owspfm.elwha.theme;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.within;

import com.owspfm.elwha.testkit.EdtInterceptor;
import com.owspfm.elwha.testkit.ThemeExtension;
import java.lang.reflect.Field;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import javax.swing.JPanel;
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

  // ------------------------------------------------------- progress listeners

  @Test
  void everyRegisteredListenerHearsASyntheticTick() {
    final MorphAnimator animator = new MorphAnimator(new JPanel(), MorphAnimator.SHORT3_MS);
    final AtomicInteger first = new AtomicInteger();
    final AtomicInteger second = new AtomicInteger();
    animator.addProgressListener(first::incrementAndGet);
    animator.addProgressListener(second::incrementAndGet);

    animator.snapTo(1f);

    assertThat(first.get()).as("a parent broadcasting to children hears every snap").isEqualTo(1);
    assertThat(second.get()).as("and so does every sibling listener").isEqualTo(1);
  }

  @Test
  void aListenerCanRemoveItselfMidDispatchWithoutBreakingTheBroadcast() {
    final MorphAnimator animator = new MorphAnimator(new JPanel(), MorphAnimator.SHORT3_MS);
    final AtomicInteger survivorCalls = new AtomicInteger();
    final Runnable[] selfRemoving = new Runnable[1];
    selfRemoving[0] = () -> animator.removeProgressListener(selfRemoving[0]);
    animator.addProgressListener(selfRemoving[0]);
    animator.addProgressListener(survivorCalls::incrementAndGet);

    animator.snapTo(1f);
    animator.snapTo(0f);

    assertThat(survivorCalls.get())
        .as("dispatch iterates a snapshot, so a mid-dispatch removal neither throws nor skips")
        .isEqualTo(2);
  }

  @Test
  void removingAListenerStopsItHearingLaterTicks() {
    final MorphAnimator animator = new MorphAnimator(new JPanel(), MorphAnimator.SHORT3_MS);
    final AtomicInteger calls = new AtomicInteger();
    final Runnable listener = calls::incrementAndGet;
    animator.addProgressListener(listener);

    animator.snapTo(1f);
    animator.removeProgressListener(listener);
    animator.snapTo(0f);

    assertThat(calls.get()).as("a removed listener is off the broadcast for good").isEqualTo(1);
  }

  // --------------------------------------------------- OS reduced-motion probe

  /**
   * Loads a second, independent copy of the class so its static state is untouched by everything
   * this JVM has already done to the real one — the only way to observe what class initialisation
   * on its own does. Parented at the platform loader so {@code java.desktop} still resolves while
   * {@code MorphAnimator} itself is defined fresh.
   */
  private static Class<?> freshlyInitialisedAnimator() throws Exception {
    final URL classes = MorphAnimator.class.getProtectionDomain().getCodeSource().getLocation();
    try (URLClassLoader isolated =
        new URLClassLoader(new URL[] {classes}, ClassLoader.getPlatformClassLoader())) {
      return Class.forName(MorphAnimator.class.getName(), true, isolated);
    }
  }

  private static Object staticField(final Class<?> type, final String name) throws Exception {
    final Field field = type.getDeclaredField(name);
    field.setAccessible(true);
    return field.get(null);
  }

  @Test
  void classInitialisationProbesNothing() throws Exception {
    final Class<?> fresh = freshlyInitialisedAnimator();

    assertThat(((AtomicBoolean) staticField(fresh, "OS_SIGNAL_REQUESTED")).get())
        .as("class init is triggered by the first ElwhaButton, so it must not shell out to the OS")
        .isFalse();
    assertThat(staticField(fresh, "reducedMotion"))
        .as("and it must reach no verdict, since it asked nothing")
        .isEqualTo(false);
  }

  @Test
  void firstUseIsWhatRequestsTheOsSignal() throws Exception {
    final Class<?> fresh = freshlyInitialisedAnimator();

    fresh.getMethod("isReducedMotion").invoke(null);

    assertThat(((AtomicBoolean) staticField(fresh, "OS_SIGNAL_REQUESTED")).get())
        .as("the probe is deferred to first use, not skipped altogether")
        .isTrue();
  }

  @Test
  void anExplicitOverrideSuppressesTheOsProbeEntirely() throws Exception {
    final Class<?> fresh = freshlyInitialisedAnimator();

    fresh.getMethod("setReducedMotion", boolean.class).invoke(null, false);

    assertThat(((AtomicBoolean) staticField(fresh, "OS_SIGNAL_REQUESTED")).get())
        .as("an explicit override outranks the OS, so asking it costs a subprocess for nothing")
        .isTrue();
    assertThat(fresh.getMethod("isReducedMotion").invoke(null))
        .as("and reading it back does not start one either")
        .isEqualTo(false);
  }

  @Test
  void noProbeThreadOutlivesTheFixture() {
    assertThat(Thread.getAllStackTraces().keySet())
        .as("every fixture pins the flag explicitly, so no run of the suite forks gsettings")
        .noneMatch(thread -> MorphAnimator.OS_PROBE_THREAD.equals(thread.getName()));
  }

  @Test
  void motionDurationsAreTheLockedM3Tokens() {
    assertThat(MorphAnimator.SHORT3_MS).as("short3 is 150ms").isEqualTo(150);
    assertThat(MorphAnimator.MEDIUM2_MS).as("medium2 is 300ms").isEqualTo(300);
    assertThat(MorphAnimator.MEDIUM3_MS).as("medium3 is 350ms").isEqualTo(350);
  }
}
