package com.owspfm.elwha.switches;

import static com.owspfm.elwha.testkit.WaitFor.onEdt;
import static com.owspfm.elwha.testkit.WaitFor.waitFor;
import static org.assertj.core.api.Assertions.assertThat;

import com.owspfm.elwha.testkit.GuiSteps;
import com.owspfm.elwha.testkit.GuiToolkit;
import com.owspfm.elwha.testkit.Pixels;
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

  private static final java.util.concurrent.atomic.AtomicInteger FRAME_SLOT =
      new java.util.concurrent.atomic.AtomicInteger();

  private JFrame frame;
  private ElwhaSwitch first;
  private ElwhaSwitch second;
  private Robot robot;

  @BeforeEach
  void showTwoSwitches() throws Exception {
    robot = new Robot();
    // Each test's frame gets its own screen slot: under bare Xvfb the previous test's window can
    // still be tearing down at the shared position, and events aimed there go to the dying window.
    final int slot = FRAME_SLOT.getAndIncrement();
    final int frameX = 100 + slot * 320;
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
          frame.setLocation(frameX, 100);
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

    GuiSteps.keyUntil(
        robot,
        KeyEvent.VK_SPACE,
        "Space through the real pipeline toggles the focused switch",
        () -> first.isSelected());

    GuiSteps.keyUntil(
        robot,
        KeyEvent.VK_TAB,
        "Tab moves real focus to the second switch",
        () -> second.isFocusOwner());
    assertThat(onEdt(() -> first.isFocusOwner())).as("first switch released focus").isFalse();

    GuiSteps.keyUntil(
        robot,
        KeyEvent.VK_SPACE,
        "Space toggles the newly focused switch",
        () -> second.isSelected());
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
    for (int attempt = 0; attempt < 3 && toggles.get() == 0; attempt++) {
      final Point center = onEdtPoint(() -> centerOnScreen(first));
      robot.mouseMove(center.x, center.y);
      robot.mousePress(InputEvent.BUTTON1_DOWN_MASK);
      robot.mouseRelease(InputEvent.BUTTON1_DOWN_MASK);
      robot.waitForIdle();
      final long deadline = System.currentTimeMillis() + 1500;
      while (System.currentTimeMillis() < deadline && toggles.get() == 0) {
        Thread.sleep(20);
      }
    }
    if (toggles.get() == 0) {
      dumpScreen("click-delivery-failure");
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

  // ------------------------------------------------- focus-visible (#630)

  /** Render size for the halo probe, matching the Tier A state-layer fixture. */
  private static final int PROBE_W = 60;

  private static final int PROBE_H = 40;

  /** Above the unselected handle, inside the state-layer halo circle. */
  private static final int OFF_HALO_X = 20;

  /** Above the selected handle, where the halo rides once the switch is on. */
  private static final int ON_HALO_X = 40;

  private static final int HALO_Y = 2;

  private static final Color PROBE_GROUND = Color.MAGENTA;

  /**
   * Whether the halo is painted at {@code x}, read from an offscreen repaint of the live switch.
   *
   * <p>Hover is cleared first: this tier cannot guarantee that parking the pointer produces a
   * delivered {@code MOUSE_EXITED} before the probe (it did not, under Cacio), and the hover layer
   * sits close enough to the focus layer that no pixel tolerance separates them. Clearing it makes
   * the probe read the focus half alone, which is what #630 is about; that hover paints its own
   * layer, and that press outranks it, are pinned in {@code ElwhaSwitchStateLayerTest}.
   */
  private boolean haloPaintedAt(final ElwhaSwitch toggle, final int x) {
    toggle.setHovered(false);
    final BufferedImage shot = Pixels.render(toggle, PROBE_W, PROBE_H, PROBE_GROUND);
    final Color got = new Color(shot.getRGB(x, HALO_Y), true);
    return Math.abs(got.getRed() - PROBE_GROUND.getRed()) > 10
        || Math.abs(got.getGreen() - PROBE_GROUND.getGreen()) > 10
        || Math.abs(got.getBlue() - PROBE_GROUND.getBlue()) > 10;
  }

  @Test
  void aKeyboardTraversalArmsTheFocusTreatmentAndAClickDoesNot() throws Exception {
    SwingUtilities.invokeAndWait(() -> first.requestFocusInWindow());
    waitFor("the first switch owns focus", () -> first.isFocusOwner());
    assertThat(onEdt(() -> haloPaintedAt(first, OFF_HALO_X)))
        .as("a programmatic focus request is not a keyboard interaction, so no halo")
        .isFalse();

    GuiSteps.keyUntil(
        robot,
        KeyEvent.VK_TAB,
        "Tab moves real focus onto the second switch",
        () -> second.isFocusOwner());

    waitFor("a keyboard traversal arms the focus halo", () -> haloPaintedAt(second, OFF_HALO_X));

    // Now take focus the other way. The click toggles the switch it lands on, so the halo — which
    // rides the handle — is probed at the selected position afterwards.
    final Point center = onEdtPoint(() -> centerOnScreen(first));
    robot.mouseMove(center.x, center.y);
    robot.mousePress(InputEvent.BUTTON1_DOWN_MASK);
    robot.mouseRelease(InputEvent.BUTTON1_DOWN_MASK);
    robot.waitForIdle();
    waitFor("the click moved real focus to the first switch", () -> first.isFocusOwner());
    // Park the pointer so the hover layer cannot stand in for the focus layer at the probe.
    robot.mouseMove(0, 0);
    robot.waitForIdle();

    waitFor(
        "#630 — a click grabs focus but is not a focus-visible interaction, so it leaves no halo"
            + " behind; the switch used to gate on raw isFocusOwner() and light up on every click",
        () -> !haloPaintedAt(first, first.isSelected() ? ON_HALO_X : OFF_HALO_X));
    assertThat(onEdt(() -> second.isFocusOwner()))
        .as("and the traversal-focused switch really did give focus up")
        .isFalse();
  }

  /** Captures the whole virtual screen into surefire-reports so the CI artifact carries it. */
  private void dumpScreen(final String label) {
    try {
      final Rectangle screen = new Rectangle(java.awt.Toolkit.getDefaultToolkit().getScreenSize());
      final java.io.File dir = new java.io.File("target/surefire-reports");
      dir.mkdirs();
      javax.imageio.ImageIO.write(
          robot.createScreenCapture(screen), "png", new java.io.File(dir, label + ".png"));
    } catch (final Exception e) {
      System.err.println("screen dump failed: " + e);
    }
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
