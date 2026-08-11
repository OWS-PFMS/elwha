package com.owspfm.elwha.chip;

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
import java.awt.Point;
import java.awt.Robot;
import java.awt.event.InputEvent;
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
 * Tier B coverage of the chip's focus-visible contract (#630) — that a keyboard traversal shows the
 * focus treatment and a pointer click does not. Whether it paints turns on the <em>cause</em> of a
 * real {@code FocusEvent} ({@link com.owspfm.elwha.theme.FocusVisible}), and the determinism rules
 * ban dispatching synthetic focus events, so only a real traversal and a real click can tell the
 * two apart. Before the fix the chip gated on raw {@code isFocusOwner()} while requesting focus
 * from its own {@code mousePressed}, so every click left the treatment behind.
 *
 * <p>The chip is the family's clearest probe for this: its focus treatment is a 2 dp {@link
 * ColorRole#PRIMARY} border on the container edge, distinct in both geometry and colour from the
 * hover and press state layers that fill the body. The sibling sites gate the same flag the same
 * way; the switch's shared halo is asserted for the traversal direction in {@code
 * ElwhaSwitchGuiTest} and for its one-layer-at-a-time policy headless.
 */
@Tag("gui")
@ExtendWith(GuiToolkit.class)
class ElwhaChipGuiTest {

  private static final AtomicInteger FRAME_SLOT = new AtomicInteger();

  /** On the chip's left border at mid-height, inside the 2 px focused stroke. */
  private static final int BORDER_PROBE_X = 1;

  private JFrame frame;
  private ElwhaChip first;
  private ElwhaChip second;
  private Robot robot;

  @BeforeEach
  void showTwoChips() throws Exception {
    robot = new Robot();
    robot.setAutoDelay(50);
    robot.setAutoWaitForIdle(true);
    final int slot = FRAME_SLOT.getAndIncrement();
    SwingUtilities.invokeAndWait(
        () -> {
          MorphAnimator.setReducedMotion(true);
          ThemeExtension.install(Mode.LIGHT);
          first = ElwhaChip.assistChip("Alpha");
          second = ElwhaChip.assistChip("Beta");
          frame = new JFrame("ElwhaChipGuiTest");
          frame.setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
          frame.setLayout(new FlowLayout(FlowLayout.LEADING, 30, 30));
          frame.add(first);
          frame.add(second);
          frame.setSize(600, 200);
          frame.setLocation(60 + slot * 40, 60);
          frame.setVisible(true);
        });
    robot.waitForIdle();
    waitFor("frame gains focus", () -> frame.isFocused());
    robot.mouseMove(2, 2);
    robot.waitForIdle();
  }

  @AfterEach
  void disposeFrame() throws Exception {
    SwingUtilities.invokeAndWait(() -> frame.dispose());
  }

  /**
   * Whether the focused border is painted, read from an offscreen repaint of the live chip.
   *
   * <p>Only the focus treatment reaches the border: hover and press composite a state layer over
   * the body fill and leave the outline role alone, so this probe cannot be fooled by a pointer
   * state the tier failed to quiesce.
   */
  private boolean focusBorderPainted(final ElwhaChip chip) {
    final Color got =
        new Color(
            Pixels.render(chip, chip.getWidth(), chip.getHeight())
                .getRGB(BORDER_PROBE_X, chip.getHeight() / 2),
            true);
    final Color want = ColorRole.PRIMARY.resolve();
    return Math.abs(got.getRed() - want.getRed()) <= 10
        && Math.abs(got.getGreen() - want.getGreen()) <= 10
        && Math.abs(got.getBlue() - want.getBlue()) <= 10;
  }

  @Test
  void aKeyboardTraversalShowsTheFocusBorder() throws Exception {
    SwingUtilities.invokeAndWait(() -> first.requestFocusInWindow());
    waitFor("the first chip owns focus", () -> first.isFocusOwner());
    assertThat(onEdt(() -> focusBorderPainted(second)))
        .as("the unfocused chip keeps its resting outline")
        .isFalse();

    GuiSteps.keyUntil(
        robot,
        KeyEvent.VK_TAB,
        "Tab moves real focus onto the second chip",
        () -> second.isFocusOwner());

    waitFor("a keyboard traversal arms the focus border", () -> focusBorderPainted(second));
  }

  @Test
  void aPointerClickTakesFocusWithoutShowingTheFocusBorder() throws Exception {
    SwingUtilities.invokeAndWait(() -> first.requestFocusInWindow());
    waitFor("the first chip owns focus", () -> first.isFocusOwner());
    GuiSteps.keyUntil(
        robot,
        KeyEvent.VK_TAB,
        "Tab moves real focus onto the second chip",
        () -> second.isFocusOwner());
    waitFor("the traversal armed the border", () -> focusBorderPainted(second));

    final Point center = onEdt2(() -> centerOnScreen(first));
    robot.mouseMove(center.x, center.y);
    robot.mousePress(InputEvent.BUTTON1_DOWN_MASK);
    robot.mouseRelease(InputEvent.BUTTON1_DOWN_MASK);
    robot.waitForIdle();
    waitFor("the click moved real focus to the first chip", () -> first.isFocusOwner());
    robot.mouseMove(2, 2);
    robot.waitForIdle();

    waitFor(
        "#630 — a click grabs focus but is not a focus-visible interaction, so no focus border",
        () -> !focusBorderPainted(first));
    assertThat(onEdt(() -> focusBorderPainted(second)))
        .as("and the chip that lost focus dropped its border with it")
        .isFalse();
  }

  private static Point centerOnScreen(final ElwhaChip chip) {
    final Point origin = chip.getLocationOnScreen();
    return new Point(origin.x + chip.getWidth() / 2, origin.y + chip.getHeight() / 2);
  }

  private <T> T onEdt2(final java.util.function.Supplier<T> supplier) throws Exception {
    final java.util.concurrent.atomic.AtomicReference<T> value =
        new java.util.concurrent.atomic.AtomicReference<>();
    SwingUtilities.invokeAndWait(() -> value.set(supplier.get()));
    return value.get();
  }
}
