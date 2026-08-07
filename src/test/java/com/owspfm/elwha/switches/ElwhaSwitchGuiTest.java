package com.owspfm.elwha.switches;

import static org.assertj.core.api.Assertions.assertThat;

import com.github.caciocavallosilano.cacio.ctc.junit.CacioExtension;
import com.owspfm.elwha.theme.ColorRole;
import com.owspfm.elwha.theme.ElwhaTheme;
import com.owspfm.elwha.theme.MaterialPalettes;
import com.owspfm.elwha.theme.Mode;
import com.owspfm.elwha.theme.MorphAnimator;
import java.awt.Color;
import java.awt.FlowLayout;
import java.awt.KeyboardFocusManager;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.Robot;
import java.awt.event.InputEvent;
import java.awt.event.KeyEvent;
import java.awt.image.BufferedImage;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.BooleanSupplier;
import javax.swing.JFrame;
import javax.swing.SwingUtilities;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

/**
 * Tier B spike for the #439 framework selection — proves the load-bearing unknowns of the Cacio
 * virtual-toolkit tier on a real Elwha component: FlatLaf + {@code ElwhaTheme} install under Cacio,
 * a top-level window realizes and paints, {@code java.awt.Robot} drives the real input pipeline,
 * {@code KeyboardFocusManager} arbitrates real focus (traversal moves it), and a screen capture of
 * the virtual framebuffer shows token-correct pixels. Everything here is impossible under {@code
 * java.awt.headless=true} — which is exactly why this tier exists.
 *
 * <p>Runs only in the {@code gui-test} surefire execution (tag {@code gui}), in its own forked JVM:
 * mixing Cacio's toolkit with an already-initialized JDK toolkit is a documented segfault.
 *
 * @author Charles Bryan
 * @version v0.5.0
 * @since v0.5.0
 */
@Tag("gui")
@ExtendWith(CacioExtension.class)
class ElwhaSwitchGuiTest {

  private static final int TIMEOUT_MS = 4000;

  private JFrame frame;
  private ElwhaSwitch first;
  private ElwhaSwitch second;
  private Robot robot;

  @BeforeEach
  void showTwoSwitches() throws Exception {
    robot = new Robot();
    SwingUtilities.invokeAndWait(
        () -> {
          MorphAnimator.setReducedMotion(true);
          ElwhaTheme.install(
              ElwhaTheme.config().theme(MaterialPalettes.baseline()).mode(Mode.LIGHT).build());
          first = new ElwhaSwitch();
          second = new ElwhaSwitch();
          frame = new JFrame("ElwhaSwitchGuiTest");
          frame.setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
          frame.setLayout(new FlowLayout());
          frame.add(first);
          frame.add(second);
          frame.pack();
          frame.setLocation(100, 100);
          frame.setVisible(true);
        });
    robot.waitForIdle();
    waitFor("frame gains focus", () -> frame.isFocused());
  }

  @AfterEach
  void disposeFrame() throws Exception {
    SwingUtilities.invokeAndWait(() -> frame.dispose());
  }

  @Test
  void realFocusOwnershipTraversalAndKeyToggle() throws Exception {
    SwingUtilities.invokeAndWait(() -> first.requestFocusInWindow());
    waitFor("first switch owns focus", () -> first.isFocusOwner());

    robot.keyPress(KeyEvent.VK_SPACE);
    robot.keyRelease(KeyEvent.VK_SPACE);
    robot.waitForIdle();
    waitFor("Space through the real pipeline toggles the focused switch", () -> first.isSelected());

    robot.keyPress(KeyEvent.VK_TAB);
    robot.keyRelease(KeyEvent.VK_TAB);
    robot.waitForIdle();
    waitFor("Tab moves real focus to the second switch", () -> second.isFocusOwner());
    assertThat(onEdt(() -> first.isFocusOwner())).as("first switch released focus").isFalse();

    robot.keyPress(KeyEvent.VK_SPACE);
    robot.keyRelease(KeyEvent.VK_SPACE);
    robot.waitForIdle();
    waitFor("Space toggles the newly focused switch", () -> second.isSelected());
    assertThat(
            onEdt(
                () ->
                    KeyboardFocusManager.getCurrentKeyboardFocusManager().getFocusOwner()
                        == second))
        .as("KeyboardFocusManager agrees on the focus owner")
        .isTrue();
  }

  @Test
  void robotClickTogglesAndTheFramebufferShowsPrimaryTrack() throws Exception {
    final Point center = onEdtPoint(() -> centerOnScreen(first));
    robot.mouseMove(center.x, center.y);
    robot.waitForIdle();
    robot.mousePress(InputEvent.BUTTON1_DOWN_MASK);
    robot.mouseRelease(InputEvent.BUTTON1_DOWN_MASK);
    robot.waitForIdle();
    waitFor("Robot click through the real pipeline toggles", () -> first.isSelected());

    waitFor(
        "virtual framebuffer shows the PRIMARY selected track",
        () -> {
          final Rectangle bounds = boundsOnScreen(first);
          final BufferedImage shot = robot.createScreenCapture(bounds);
          final Color want = ColorRole.PRIMARY.resolve();
          final Color got = new Color(shot.getRGB(9, first.getHeight() / 2), true);
          return Math.abs(got.getRed() - want.getRed()) <= 10
              && Math.abs(got.getGreen() - want.getGreen()) <= 10
              && Math.abs(got.getBlue() - want.getBlue()) <= 10;
        });
  }

  private static Point centerOnScreen(final ElwhaSwitch s) {
    final Point p = s.getLocationOnScreen();
    return new Point(p.x + s.getWidth() / 2, p.y + s.getHeight() / 2);
  }

  private static Rectangle boundsOnScreen(final ElwhaSwitch s) {
    final Point p = s.getLocationOnScreen();
    return new Rectangle(p.x, p.y, s.getWidth(), s.getHeight());
  }

  private static boolean onEdt(final BooleanSupplier read) throws Exception {
    final AtomicBoolean value = new AtomicBoolean();
    SwingUtilities.invokeAndWait(() -> value.set(read.getAsBoolean()));
    return value.get();
  }

  private static Point onEdtPoint(final java.util.function.Supplier<Point> read) throws Exception {
    final AtomicReference<Point> value = new AtomicReference<>();
    SwingUtilities.invokeAndWait(() -> value.set(read.get()));
    return value.get();
  }

  /** Polls the condition on the EDT every 20ms until true or {@link #TIMEOUT_MS} elapses. */
  private static void waitFor(final String what, final BooleanSupplier condition) throws Exception {
    final long deadline = System.currentTimeMillis() + TIMEOUT_MS;
    while (System.currentTimeMillis() < deadline) {
      if (onEdt(condition)) {
        return;
      }
      Thread.sleep(20);
    }
    assertThat(onEdt(condition)).as(what).isTrue();
  }
}
