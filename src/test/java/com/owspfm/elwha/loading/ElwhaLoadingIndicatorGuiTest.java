package com.owspfm.elwha.loading;

import static com.owspfm.elwha.testkit.WaitFor.onEdt;
import static org.assertj.core.api.Assertions.assertThat;

import com.owspfm.elwha.testkit.GuiToolkit;
import com.owspfm.elwha.testkit.ThemeExtension;
import com.owspfm.elwha.theme.Mode;
import com.owspfm.elwha.theme.MorphAnimator;
import java.awt.FlowLayout;
import java.util.concurrent.atomic.AtomicInteger;
import javax.swing.JFrame;
import javax.swing.SwingUtilities;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

/**
 * Tier B coverage of the loading indicator's animation clock responding to a runtime reduced-motion
 * toggle (#632).
 *
 * <p>This cannot be a headless test. The clock is gated on {@code isShowing()}, which needs a
 * realized window at the top of the hierarchy, so the demand is unconditionally false without a
 * display and neither direction of the toggle is observable. The broadcast channel itself is
 * covered headlessly by {@code MorphAnimatorReducedMotionListenerTest}; what is asserted here is
 * the half that needs a real window — that a <em>live</em> spinner starts and stops.
 *
 * <p>The bug: reduced motion was sampled only in the demand update, which runs on {@code
 * SHOWING_CHANGED} and on mode changes. Flipping it under a showing spinner therefore did nothing —
 * on left the 60 fps clock burning while the paint had already snapped static, and off left the
 * clock stopped, so the spinner stayed frozen until it was removed and re-added.
 */
@Tag("gui")
@ExtendWith(GuiToolkit.class)
class ElwhaLoadingIndicatorGuiTest {

  private static final AtomicInteger FRAME_SLOT = new AtomicInteger();

  private JFrame frame;
  private ElwhaLoadingIndicator indicator;

  @BeforeEach
  void showAnIndicator() throws Exception {
    ThemeExtension.install(Mode.LIGHT);
    SwingUtilities.invokeAndWait(
        () -> {
          MorphAnimator.setReducedMotion(false);
          indicator = new ElwhaLoadingIndicator();
          frame = new JFrame("loading-" + FRAME_SLOT.incrementAndGet());
          frame.setLayout(new FlowLayout());
          frame.add(indicator);
          frame.setSize(200, 160);
          frame.setLocation(40, 40);
          frame.setVisible(true);
        });
  }

  @AfterEach
  void disposeTheFrame() throws Exception {
    SwingUtilities.invokeAndWait(
        () -> {
          MorphAnimator.setReducedMotion(true);
          if (frame != null) {
            frame.dispose();
          }
        });
  }

  @Test
  void turningReducedMotionOnStopsALiveSpinnersClock() throws Exception {
    assertThat(onEdt(() -> indicator.isAnimating()))
        .as("a showing indeterminate spinner runs its clock to begin with")
        .isTrue();

    SwingUtilities.invokeAndWait(() -> MorphAnimator.setReducedMotion(true));

    assertThat(onEdt(() -> indicator.isAnimating()))
        .as("the clock stops rather than burning frames behind a paint that snapped static")
        .isFalse();
  }

  @Test
  void turningReducedMotionOffRestartsAFrozenSpinner() throws Exception {
    SwingUtilities.invokeAndWait(() -> MorphAnimator.setReducedMotion(true));
    assertThat(onEdt(() -> indicator.isAnimating())).isFalse();

    SwingUtilities.invokeAndWait(() -> MorphAnimator.setReducedMotion(false));

    assertThat(onEdt(() -> indicator.isAnimating()))
        .as("and restarts, rather than staying frozen until the component is re-added")
        .isTrue();
  }

  @Test
  void aDeterminateIndicatorStaysStoppedThroughTheToggle() throws Exception {
    SwingUtilities.invokeAndWait(() -> indicator.setIndeterminate(false));
    assertThat(onEdt(() -> indicator.isAnimating()))
        .as("determinate mode is progress-driven, so it never runs the loop clock")
        .isFalse();

    SwingUtilities.invokeAndWait(() -> MorphAnimator.setReducedMotion(true));
    assertThat(onEdt(() -> indicator.isAnimating())).isFalse();

    SwingUtilities.invokeAndWait(() -> MorphAnimator.setReducedMotion(false));
    assertThat(onEdt(() -> indicator.isAnimating()))
        .as("turning motion back on must not start a clock the mode does not want")
        .isFalse();
  }
}
