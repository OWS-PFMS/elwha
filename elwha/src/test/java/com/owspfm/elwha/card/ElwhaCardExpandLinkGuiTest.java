package com.owspfm.elwha.card;

import static com.owspfm.elwha.testkit.WaitFor.onEdt;
import static com.owspfm.elwha.testkit.WaitFor.waitFor;
import static org.assertj.core.api.Assertions.assertThat;

import com.owspfm.elwha.button.ElwhaButton;
import com.owspfm.elwha.testkit.GuiSteps;
import com.owspfm.elwha.testkit.GuiToolkit;
import com.owspfm.elwha.testkit.Pixels;
import com.owspfm.elwha.testkit.ThemeExtension;
import com.owspfm.elwha.theme.ColorRole;
import com.owspfm.elwha.theme.Mode;
import com.owspfm.elwha.theme.MorphAnimator;
import java.awt.Color;
import java.awt.FlowLayout;
import java.awt.Robot;
import java.awt.event.KeyEvent;
import java.util.concurrent.atomic.AtomicInteger;
import javax.swing.JFrame;
import javax.swing.SwingUtilities;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

/**
 * Tier B coverage of the expand link's keyboard focus indicator. The ring is drawn on {@code
 * isFocusOwner()}, which needs the real {@code KeyboardFocusManager} to have arbitrated focus — a
 * headless label owns nothing, and the determinism rules ban dispatching a synthetic focus event to
 * fake it.
 *
 * <p>The link is a real tab stop (focusable, with its own {@code WHEN_FOCUSED} Space/Enter
 * bindings), and until #606 it painted nothing when focus arrived — a keyboard user could Tab onto
 * the disclosure control with no indication on screen of where focus had gone. The chrome is read
 * from an offscreen repaint of the live component rather than a screen capture, so the assertion
 * does not depend on window decorations or display scaling.
 */
@Tag("gui")
@ExtendWith(GuiToolkit.class)
class ElwhaCardExpandLinkGuiTest {

  private static final AtomicInteger FRAME_SLOT = new AtomicInteger();

  /** Mid-height on the left edge — past the corner arc, inside the 2 px ring stroke. */
  private static final int RING_PROBE_X = 1;

  private JFrame frame;
  private ElwhaButton sink;
  private ElwhaCard card;
  private ElwhaCardExpandLink link;
  private Robot robot;

  @BeforeEach
  void showALinkBesideASink() throws Exception {
    robot = new Robot();
    robot.setAutoDelay(50);
    robot.setAutoWaitForIdle(true);
    final int slot = FRAME_SLOT.getAndIncrement();
    SwingUtilities.invokeAndWait(
        () -> {
          MorphAnimator.setReducedMotion(true);
          ThemeExtension.install(Mode.LIGHT);
          sink = ElwhaButton.textButton("Elsewhere");
          card = ElwhaCard.filledCard().setCollapsible(true);
          link = new ElwhaCardExpandLink(card, "Show more", "Show less");
          frame = new JFrame("ElwhaCardExpandLinkGuiTest");
          frame.setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
          frame.setLayout(new FlowLayout(FlowLayout.LEADING, 20, 20));
          frame.add(sink);
          frame.add(link);
          frame.setSize(600, 300);
          frame.setLocation(40 + slot * 40, 40);
          frame.setVisible(true);
        });
    robot.waitForIdle();
    waitFor("frame gains focus", () -> frame.isFocused());
    SwingUtilities.invokeAndWait(() -> sink.requestFocusInWindow());
    waitFor("focus parks on the sink", () -> sink.isFocusOwner());
    robot.mouseMove(2, 2);
    robot.waitForIdle();
  }

  @AfterEach
  void disposeFrame() throws Exception {
    SwingUtilities.invokeAndWait(() -> frame.dispose());
  }

  private void assertRing(final boolean expected, final String what) throws Exception {
    final Color want = expected ? ColorRole.SECONDARY.resolve() : ColorRole.SURFACE.resolve();
    waitFor(
        what,
        () ->
            near(
                Pixels.render(link, link.getWidth(), link.getHeight())
                    .getRGB(RING_PROBE_X, link.getHeight() / 2),
                want));
    Pixels.assertPixelNear(
        Pixels.render(link, link.getWidth(), link.getHeight()),
        RING_PROBE_X,
        link.getHeight() / 2,
        want,
        what);
  }

  private static boolean near(final int argb, final Color want) {
    final Color got = new Color(argb, true);
    return Math.abs(got.getRed() - want.getRed()) <= 10
        && Math.abs(got.getGreen() - want.getGreen()) <= 10
        && Math.abs(got.getBlue() - want.getBlue()) <= 10;
  }

  @Test
  void tabbingOntoTheLinkShowsWhereFocusLanded() throws Exception {
    assertRing(false, "an unfocused link is just a link — no ring");

    GuiSteps.keyUntil(robot, KeyEvent.VK_TAB, "Tab reaches the link", () -> link.isFocusOwner());

    assertThat(onEdt(() -> sink.isFocusOwner())).as("the sink released focus").isFalse();
    assertRing(true, "the tab stop announces itself, so the keyboard user can see where they are");
  }

  @Test
  void focusRingClearsWhenFocusMovesOn() throws Exception {
    GuiSteps.keyUntil(robot, KeyEvent.VK_TAB, "Tab reaches the link", () -> link.isFocusOwner());
    assertRing(true, "the link holds focus");

    SwingUtilities.invokeAndWait(() -> sink.requestFocusInWindow());
    waitFor("focus returns to the sink", () -> sink.isFocusOwner());

    assertRing(false, "a link that no longer holds focus stops claiming it does");
  }
}
