package com.owspfm.elwha.button;

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
import java.awt.FlowLayout;
import java.awt.KeyboardFocusManager;
import java.awt.Point;
import java.awt.Robot;
import java.awt.event.KeyEvent;
import java.awt.image.BufferedImage;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;
import javax.swing.JFrame;
import javax.swing.SwingUtilities;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

/**
 * Tier B coverage of the actions family's focus-visible contract — the one behavior in this family
 * that headless cannot represent. Whether the ring paints depends on the <em>cause</em> of a real
 * {@code FocusEvent} ({@link com.owspfm.elwha.theme.FocusVisible}), and the determinism rules ban
 * dispatching synthetic focus events, so only a real keyboard traversal and a real pointer click
 * can tell the two apart.
 *
 * <p>Focus itself is arbitrated by the real {@code KeyboardFocusManager}; the resulting chrome is
 * then read from an offscreen repaint of the live component rather than a screen capture, so the
 * assertion does not depend on window decorations or display scaling.
 */
@Tag("gui")
@ExtendWith(GuiToolkit.class)
class ElwhaButtonGuiTest {

  private static final AtomicInteger FRAME_SLOT = new AtomicInteger();

  /** Mid-height on a square body's straight left edge, inside the 2 px focus stroke. */
  private static final int RING_PROBE_X = 1;

  private JFrame frame;
  private ElwhaButton first;
  private ElwhaButton second;
  private Robot robot;

  @BeforeEach
  void showTwoButtons() throws Exception {
    robot = new Robot();
    final int slot = FRAME_SLOT.getAndIncrement();
    final int frameX = 100 + slot * 320;
    robot.setAutoDelay(50);
    robot.setAutoWaitForIdle(true);
    SwingUtilities.invokeAndWait(
        () -> {
          MorphAnimator.setReducedMotion(true);
          ThemeExtension.install(Mode.LIGHT);
          first = ringProbeButton();
          second = ringProbeButton();
          frame = new JFrame("ElwhaButtonGuiTest");
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

  /**
   * A label-free square tonal button: the whole body is chrome, its left edge is straight at
   * mid-height, and its resting container color differs sharply from the focus ring's.
   */
  private static ElwhaButton ringProbeButton() {
    return new ElwhaButton()
        .setVariant(ButtonVariant.FILLED_TONAL)
        .setShape(ButtonShape.SQUARE)
        .setButtonSize(ButtonSize.L);
  }

  @Test
  void keyboardTraversalMovesRealFocusAndArmsTheFocusRing() throws Exception {
    SwingUtilities.invokeAndWait(() -> first.requestFocusInWindow());
    waitFor("the first button owns focus", () -> first.isFocusOwner());

    assertRing(first, false, "programmatic focus is not a keyboard interaction, so no ring");

    GuiSteps.keyUntil(
        robot,
        KeyEvent.VK_TAB,
        "Tab moves real focus to the second button",
        () -> second.isFocusOwner());

    assertThat(onEdt(() -> first.isFocusOwner())).as("the first button released focus").isFalse();
    assertThat(
            onEdt(
                () ->
                    KeyboardFocusManager.getCurrentKeyboardFocusManager().getFocusOwner()
                        == second))
        .as("the KeyboardFocusManager agrees on the focus owner")
        .isTrue();
    assertRing(second, true, "a keyboard traversal arms the focus-visible ring");
  }

  @Test
  void aPointerClickTakesFocusWithoutShowingTheRing() throws Exception {
    SwingUtilities.invokeAndWait(() -> first.requestFocusInWindow());
    waitFor("the first button owns focus", () -> first.isFocusOwner());
    GuiSteps.keyUntil(
        robot,
        KeyEvent.VK_TAB,
        "Tab moves real focus to the second button",
        () -> second.isFocusOwner());
    assertRing(second, true, "the traversal armed the ring on the second button");

    clickUntilFocused(first);

    assertThat(onEdt(() -> second.isFocusOwner()))
        .as("the click moved focus off the keyboard-focused button")
        .isFalse();
    assertRing(
        first,
        false,
        "a click grabs focus but is not a focus-visible interaction — no ring for pointer users");
  }

  /** Presses the button through the real pipeline until it owns focus, guarded on that effect. */
  private void clickUntilFocused(final ElwhaButton button) throws Exception {
    GuiSteps.clickUntil(
        robot,
        frame,
        () -> {
          try {
            return centerOnScreen(button);
          } catch (final Exception e) {
            throw new IllegalStateException(e);
          }
        },
        "the clicked button owns focus",
        button::isFocusOwner);
    // Park the pointer off the frame so a lingering hover layer cannot tint the probed pixel.
    robot.mouseMove(0, 0);
    robot.waitForIdle();
  }

  private void assertRing(final ElwhaButton button, final boolean expected, final String what)
      throws Exception {
    final BufferedImage image = snapshot(button);
    Pixels.assertPixelNear(
        image,
        RING_PROBE_X,
        button.getHeight() / 2,
        expected ? ColorRole.PRIMARY.resolve() : ColorRole.SECONDARY_CONTAINER.resolve(),
        what);
  }

  private static BufferedImage snapshot(final ElwhaButton button) throws Exception {
    final AtomicReference<BufferedImage> image = new AtomicReference<>();
    SwingUtilities.invokeAndWait(
        () -> image.set(Pixels.render(button, button.getWidth(), button.getHeight())));
    return image.get();
  }

  private static Point centerOnScreen(final ElwhaButton button) throws Exception {
    final AtomicReference<Point> point = new AtomicReference<>();
    SwingUtilities.invokeAndWait(
        () -> {
          final Point origin = button.getLocationOnScreen();
          point.set(new Point(origin.x + button.getWidth() / 2, origin.y + button.getHeight() / 2));
        });
    return point.get();
  }
}
