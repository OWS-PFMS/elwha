package com.owspfm.elwha.switches;

import static com.owspfm.elwha.testkit.WaitFor.onEdt;
import static com.owspfm.elwha.testkit.WaitFor.waitFor;
import static org.assertj.core.api.Assertions.assertThat;

import com.owspfm.elwha.testkit.GuiToolkit;
import com.owspfm.elwha.testkit.ThemeExtension;
import com.owspfm.elwha.theme.ColorRole;
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
import java.util.concurrent.atomic.AtomicReference;
import javax.swing.JFrame;
import javax.swing.SwingUtilities;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

/**
 * Tier B spike for the #439 framework selection — proves the display-bearing tier on a real Elwha
 * component: FlatLaf + {@code ElwhaTheme} install, a top-level window realizes and paints, {@code
 * java.awt.Robot} drives the real input pipeline, {@code KeyboardFocusManager} arbitrates real
 * focus (traversal moves it), and a screen capture shows token-correct pixels. Everything here is
 * impossible under {@code java.awt.headless=true} — which is exactly why this tier exists. The
 * hosting display stack is per-platform via {@link GuiToolkit}: Cacio's virtual toolkit on
 * macOS/Windows dev machines, the native toolkit under Xvfb on Linux CI.
 *
 * <p>Runs only in the {@code gui-test} surefire execution (tag {@code gui}), in its own forked JVM:
 * mixing Cacio's toolkit with an already-initialized JDK toolkit is a documented segfault.
 *
 * @author Charles Bryan
 * @version v0.5.0
 * @since v0.5.0
 */
@Tag("gui")
@ExtendWith(GuiToolkit.class)
class ElwhaSwitchGuiTest {

  private JFrame frame;
  private ElwhaSwitch first;
  private ElwhaSwitch second;
  private Robot robot;

  @BeforeEach
  void showTwoSwitches() throws Exception {
    robot = new Robot();
    // Paces every synthetic event: under X/XTEST an unthrottled press+release can outrun the
    // app's event processing (observed on CI as a click that never toggles). No-op cost on Cacio.
    robot.setAutoDelay(50);
    robot.setAutoWaitForIdle(true);
    SwingUtilities.invokeAndWait(
        () -> {
          MorphAnimator.setReducedMotion(true);
          ThemeExtension.install(Mode.LIGHT);
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
  void robotClickTogglesAndTheFramebufferShowsTheToggledTrack() throws Exception {
    final java.util.concurrent.atomic.AtomicInteger toggles =
        new java.util.concurrent.atomic.AtomicInteger();
    SwingUtilities.invokeAndWait(() -> first.addActionListener(e -> toggles.incrementAndGet()));

    // Under X/Xvfb a click can be lost against a freshly-mapped window even after the focus wait
    // (observed on CI as a click that never arrives, ~1-in-5 after event pacing alone). The
    // re-click is guarded on "nothing arrived yet", so a slow-but-delivered click is never
    // doubled by the guard; the assertions below are count-parity-based so even a late arrival
    // after a re-click cannot produce a false pass or a false fail.
    final Point center = onEdtPoint(() -> centerOnScreen(first));
    for (int attempt = 0; attempt < 3 && toggles.get() == 0; attempt++) {
      robot.mouseMove(center.x, center.y);
      robot.mousePress(InputEvent.BUTTON1_DOWN_MASK);
      robot.mouseRelease(InputEvent.BUTTON1_DOWN_MASK);
      robot.waitForIdle();
      final long deadline = System.currentTimeMillis() + 1500;
      while (System.currentTimeMillis() < deadline && toggles.get() == 0) {
        Thread.sleep(20);
      }
    }
    waitFor("Robot click through the real pipeline toggles", () -> toggles.get() > 0);
    waitFor(
        "selection state agrees with the delivered toggle count",
        () -> first.isSelected() == (toggles.get() % 2 == 1));

    // Park the pointer off the frame so the hover state layer can't tint the probed pixel.
    robot.mouseMove(0, 0);
    robot.waitForIdle();
    waitFor(
        "virtual framebuffer shows the toggled state's track color",
        () -> {
          final Rectangle bounds = boundsOnScreen(first);
          final BufferedImage shot = robot.createScreenCapture(bounds);
          final Color want =
              first.isSelected()
                  ? ColorRole.PRIMARY.resolve()
                  : ColorRole.SURFACE_CONTAINER_HIGHEST.resolve();
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

  private static Point onEdtPoint(final java.util.function.Supplier<Point> read) throws Exception {
    final AtomicReference<Point> value = new AtomicReference<>();
    SwingUtilities.invokeAndWait(() -> value.set(read.get()));
    return value.get();
  }
}
