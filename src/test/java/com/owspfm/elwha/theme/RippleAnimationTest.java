package com.owspfm.elwha.theme;

import static org.assertj.core.api.Assertions.assertThat;

import com.owspfm.elwha.testkit.EdtInterceptor;
import com.owspfm.elwha.testkit.ThemeExtension;
import java.awt.Point;
import javax.swing.JPanel;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

/**
 * Tier A coverage of the ripple clock extracted from {@code ElwhaTab} and {@code
 * ElwhaNavRailDestination} (#640), and of the disabled guard that landed with it (#686).
 *
 * <p>The timing itself is wall-clock driven and belongs to the gui tier; what is pinned here is the
 * paint gate — the single accessor both components consult before drawing anything.
 */
@ExtendWith({EdtInterceptor.class, ThemeExtension.class})
class RippleAnimationTest {

  private final JPanel host = new JPanel();

  @Test
  void anUntouchedAnimationIsIdle() {
    final RippleAnimation ripple = new RippleAnimation(host);

    assertThat(ripple.isActive())
        .as("nothing has been started, so there is nothing to draw")
        .isFalse();
    assertThat(ripple.progress()).as("an idle ripple reads as finished").isEqualTo(1f);
    assertThat(ripple.origin()).as("and has no origin to draw around").isNull();
  }

  @Test
  void startingSeedsTheOriginAndOpensTheGate() {
    final RippleAnimation ripple = new RippleAnimation(host);

    ripple.start(new Point(12, 7));

    assertThat(ripple.isActive()).as("a fresh ripple on an enabled host draws").isTrue();
    assertThat(ripple.origin())
        .as("around the point it was started from")
        .isEqualTo(new Point(12, 7));
    assertThat(ripple.progress()).as("having only just begun").isLessThan(1f);
  }

  /**
   * #686 — before this guard, nothing in either component's ripple paint path consulted {@code
   * isEnabled()}. #683 fixed the symptom by completing the ripple when a component is disabled
   * through {@code setEnabled}, but a host disabled by any other route still had a live ripple and
   * a paint path willing to draw it.
   */
  @Test
  void aDisabledHostDrawsNoRippleEvenWithOneInFlight() {
    final RippleAnimation ripple = new RippleAnimation(host);
    ripple.start(new Point(4, 4));

    host.setEnabled(false);

    assertThat(ripple.isActive())
        .as("interactive chrome must never paint on a control that refuses interaction")
        .isFalse();
    assertThat(ripple.progress())
        .as("the clock is untouched — the guard gates painting, it does not cancel the animation")
        .isLessThan(1f);
  }

  @Test
  void reEnablingTheHostLetsAnUnfinishedRippleDrawAgain() {
    final RippleAnimation ripple = new RippleAnimation(host);
    ripple.start(new Point(4, 4));
    host.setEnabled(false);

    host.setEnabled(true);

    assertThat(ripple.isActive()).as("the gate reads the host live rather than latching").isTrue();
  }

  @Test
  void finishingClosesTheGateAndLandsTheClock() {
    final RippleAnimation ripple = new RippleAnimation(host);
    ripple.start(new Point(4, 4));

    ripple.finish();

    assertThat(ripple.progress())
        .as("removeNotify cleanup lands the ripple rather than freezing it half-drawn")
        .isEqualTo(1f);
    assertThat(ripple.isActive()).as("so nothing is left to paint").isFalse();
  }

  @Test
  void restartingReplacesTheRippleInFlight() {
    final RippleAnimation ripple = new RippleAnimation(host);
    ripple.start(new Point(4, 4));

    ripple.start(new Point(40, 40));

    assertThat(ripple.origin())
        .as("a second press spreads from where it landed, not from the first")
        .isEqualTo(new Point(40, 40));
    assertThat(ripple.isActive()).as("and is itself drawable").isTrue();
  }
}
