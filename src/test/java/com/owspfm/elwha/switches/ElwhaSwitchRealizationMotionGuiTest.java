package com.owspfm.elwha.switches;

import static org.assertj.core.api.Assertions.assertThat;

import com.owspfm.elwha.testkit.GuiToolkit;
import com.owspfm.elwha.testkit.Pixels;
import com.owspfm.elwha.testkit.ThemeExtension;
import com.owspfm.elwha.theme.Mode;
import com.owspfm.elwha.theme.MorphAnimator;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.image.BufferedImage;
import java.util.concurrent.atomic.AtomicInteger;
import javax.swing.JFrame;
import javax.swing.SwingUtilities;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

/**
 * Tier B evidence for #685 — why a realized {@link ElwhaSwitch} could paint translucent chrome
 * inside its halo bounds that a freshly constructed one did not, with every interaction flag
 * confirmed inert.
 *
 * <p><strong>It is the animation clock, and it is intended.</strong> {@code animateAllowed()} is
 * {@code isDisplayable() && isEnabled()}, and it decides whether a state change tweens or snaps. A
 * switch that has never been realized snaps every change to its rest frame; a realized one runs the
 * M3 slide, so for the ~300&nbsp;ms of that slide it paints a partially-swept track and a handle
 * mid-morph, which composites translucently over whatever is behind it. Snapping while
 * undisplayable is deliberate — it is what stops a component animating its way in on first paint.
 *
 * <p>The reason the original observation looked unattributed is that {@code hovered}, {@code
 * pressed}, {@code dragging}, {@code focusVisible} and {@code rippleOrigin} are all genuinely inert
 * throughout: the tween is a separate piece of state, and reflecting over the interaction flags
 * does not see it. Measured mid-flight, {@code slideTween} reads {@code value=0.993} against {@code
 * target=0.0} and {@code sizeTween} {@code 22.7} against {@code 16.0} while every flag above is
 * false or null.
 *
 * <p><strong>The testing rule this leaves:</strong> a raster comparison between a realized
 * component and a fresh one is only meaningful once the realized one's tweens have settled. "At
 * rest" has to include the animation clock, not just the interaction flags.
 */
@Tag("gui")
@ExtendWith(GuiToolkit.class)
class ElwhaSwitchRealizationMotionGuiTest {

  private static final AtomicInteger FRAME_SLOT = new AtomicInteger();

  /** A ground no palette carries, so partially-covering chrome is unambiguous. */
  private static final Color GROUND = Color.MAGENTA;

  /** Long enough into the 300 ms slide to be unambiguously mid-flight, short enough to still be. */
  private static final int MID_SLIDE_MS = 120;

  private JFrame frame;
  private ElwhaSwitch realized;

  @BeforeEach
  void showASwitch() throws Exception {
    ThemeExtension.install(Mode.LIGHT);
    SwingUtilities.invokeAndWait(
        () -> {
          MorphAnimator.setReducedMotion(false);
          realized = new ElwhaSwitch();
          frame = new JFrame("switch-motion-" + FRAME_SLOT.incrementAndGet());
          frame.setLayout(new FlowLayout());
          frame.add(realized);
          frame.setSize(240, 160);
          frame.setLocation(30, 30);
          frame.setVisible(true);
        });
    Thread.sleep(300);
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

  private static int differingPixels(final BufferedImage a, final BufferedImage b) {
    int differing = 0;
    for (int y = 0; y < a.getHeight(); y++) {
      for (int x = 0; x < a.getWidth(); x++) {
        if (a.getRGB(x, y) != b.getRGB(x, y)) {
          differing++;
        }
      }
    }
    return differing;
  }

  /** Renders the realized switch and a never-realized one at the same size, and diffs them. */
  private int realizedVersusFresh() throws Exception {
    final int[] result = new int[1];
    SwingUtilities.invokeAndWait(
        () -> {
          final ElwhaSwitch fresh = new ElwhaSwitch();
          final Dimension size = fresh.getPreferredSize();
          result[0] =
              differingPixels(
                  Pixels.render(realized, size.width, size.height, GROUND),
                  Pixels.render(fresh, size.width, size.height, GROUND));
        });
    return result[0];
  }

  @Test
  void anIdleRealizedSwitchIsPixelIdenticalToAFreshOne() throws Exception {
    assertThat(realizedVersusFresh())
        .as("realization alone changes nothing — there is no displayability-keyed paint path")
        .isZero();
  }

  @Test
  void aRealizedSwitchMidSlideDivergesAndThenConverges() throws Exception {
    SwingUtilities.invokeAndWait(() -> realized.setSelected(true));
    Thread.sleep(MID_SLIDE_MS);
    SwingUtilities.invokeAndWait(() -> realized.setSelected(false));

    assertThat(realizedVersusFresh())
        .as(
            "#685 — mid-slide the realized switch paints intermediate chrome the snapped fresh one"
                + " never shows, with every interaction flag inert on both")
        .isGreaterThan(0);

    Thread.sleep(600);

    assertThat(realizedVersusFresh())
        .as("and converges once the tween settles, so the divergence is the clock, not a defect")
        .isZero();
  }

  @Test
  void anUndisplayableSwitchSnapsTheSameChangeInsteadOfTweening() throws Exception {
    final int[] result = new int[1];
    SwingUtilities.invokeAndWait(
        () -> {
          final ElwhaSwitch fresh = new ElwhaSwitch();
          final ElwhaSwitch rest = new ElwhaSwitch();
          // The same on-then-off a realized switch would still be animating through.
          fresh.setSelected(true);
          fresh.setSelected(false);
          final Dimension size = rest.getPreferredSize();
          result[0] =
              differingPixels(
                  Pixels.render(fresh, size.width, size.height, GROUND),
                  Pixels.render(rest, size.width, size.height, GROUND));
        });

    assertThat(result[0])
        .as("undisplayable means snap, which is what keeps a component from animating in")
        .isZero();
  }
}
