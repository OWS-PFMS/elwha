package com.owspfm.elwha.colorpicker;

import static com.owspfm.elwha.testkit.WaitFor.onEdt;
import static com.owspfm.elwha.testkit.WaitFor.waitFor;
import static org.assertj.core.api.Assertions.assertThat;

import com.owspfm.elwha.button.ElwhaButton;
import com.owspfm.elwha.surface.ElwhaSurface;
import com.owspfm.elwha.testkit.GuiSteps;
import com.owspfm.elwha.testkit.GuiToolkit;
import com.owspfm.elwha.testkit.ThemeExtension;
import com.owspfm.elwha.theme.Mode;
import com.owspfm.elwha.theme.MorphAnimator;
import java.awt.Color;
import java.awt.Component;
import java.awt.Container;
import java.awt.FlowLayout;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.Robot;
import java.awt.event.KeyEvent;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Supplier;
import javax.swing.JFrame;
import javax.swing.JLayeredPane;
import javax.swing.SwingUtilities;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

/**
 * Tier B coverage of the colour picker's two <em>presentations</em> — the S6 backlog. The picker's
 * own model and panes are covered headless; what needed a display is the shell around them: the
 * popover's anchored placement and light dismiss, and the dialog's confirm/cancel routes driven by
 * real clicks rather than by calling the callbacks.
 *
 * <p><b>The eyedropper is deliberately absent.</b> {@code ScreenSampler.open()} captures every
 * {@link java.awt.GraphicsDevice} through {@code Robot.createScreenCapture} and shows an
 * undecorated always-on-top window over the result — on a developer machine that is a grab of the
 * real desktop and a full-screen window in front of whatever else is running. There is no seam to
 * point it at a synthetic surface, so its Escape dispatcher stays a manual-smoke item rather than
 * something this suite arms.
 */
@Tag("gui")
@ExtendWith(GuiToolkit.class)
class ElwhaColorPickerOverlayGuiTest {

  private static final AtomicInteger FRAME_SLOT = new AtomicInteger();

  private JFrame frame;
  private ElwhaButton trigger;
  private ElwhaButton sink;
  private Robot robot;
  private final List<Runnable> teardown = new ArrayList<>();

  @BeforeEach
  void showATriggerAndASink() throws Exception {
    robot = new Robot();
    robot.setAutoDelay(50);
    robot.setAutoWaitForIdle(true);
    final int slot = FRAME_SLOT.getAndIncrement();
    SwingUtilities.invokeAndWait(
        () -> {
          MorphAnimator.setReducedMotion(true);
          ThemeExtension.install(Mode.LIGHT);
          trigger = ElwhaButton.filledButton("Pick");
          sink = ElwhaButton.textButton("Elsewhere");
          frame = new JFrame("ElwhaColorPickerOverlayGuiTest");
          frame.setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
          frame.setLayout(new FlowLayout(FlowLayout.LEADING, 30, 30));
          frame.add(trigger);
          frame.add(sink);
          frame.setSize(1100, 1000);
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
    for (final Runnable close : teardown) {
      SwingUtilities.invokeAndWait(close);
    }
    teardown.clear();
    SwingUtilities.invokeAndWait(() -> frame.dispose());
  }

  // ------------------------------------------------------------------ helpers

  private <T> T read(final Supplier<T> supplier) throws Exception {
    final AtomicReference<T> value = new AtomicReference<>();
    SwingUtilities.invokeAndWait(() -> value.set(supplier.get()));
    return value.get();
  }

  /** Overlay components on the frame's layered pane, front-most first. Call on the EDT. */
  private List<Component> mounted() {
    final List<Component> out = new ArrayList<>();
    for (final Component child : frame.getLayeredPane().getComponents()) {
      if (child != frame.getContentPane()) {
        out.add(child);
      }
    }
    return out;
  }

  private Point centerOffEdt(final Component component) {
    try {
      return read(
          () -> {
            final Point origin = component.getLocationOnScreen();
            return new Point(
                origin.x + component.getWidth() / 2, origin.y + component.getHeight() / 2);
          });
    } catch (final Exception e) {
      throw new IllegalStateException(e);
    }
  }

  /**
   * The popover's painted body in pane coordinates. The mounted surface carries the elevation halo
   * as reserve, so its own rect overhangs the body on every side — comparing that to the anchor
   * would measure the shadow, which is exactly what the placement engine backs out.
   */
  private Rectangle popoverBodyInPane() {
    final Component wrapper = mounted().get(0);
    final ElwhaSurface body = (ElwhaSurface) ((Container) wrapper).getComponent(0);
    final java.awt.Insets halo = body.getShadowInsets();
    return new Rectangle(
        wrapper.getX() + halo.left,
        wrapper.getY() + halo.top,
        wrapper.getWidth() - halo.left - halo.right,
        wrapper.getHeight() - halo.top - halo.bottom);
  }

  /** The first descendant with the given accessible name — how a labelled action is located. */
  private static Component namedDescendant(final Container root, final String name) {
    for (final Component child : root.getComponents()) {
      if (child instanceof ElwhaButton button && name.equals(button.getText())) {
        return button;
      }
      if (child instanceof Container container) {
        final Component found = namedDescendant(container, name);
        if (found != null) {
          return found;
        }
      }
    }
    return null;
  }

  // ----------------------------------------------------------------- popover

  @Test
  void aPopoverOpensBelowItsAnchorOnThePopupBand() throws Exception {
    final ElwhaColorPickerPopover popover = new ElwhaColorPickerPopover();
    teardown.add(popover::close);

    SwingUtilities.invokeAndWait(() -> popover.show(trigger));
    waitFor("the popover is on screen", () -> popover.isShowing());

    assertThat(read(() -> mounted().size())).as("the popover mounts one surface").isEqualTo(1);
    assertThat(read(() -> frame.getLayeredPane().getLayer(mounted().get(0))))
        .as("on the popup band, like the menu")
        .isEqualTo(JLayeredPane.POPUP_LAYER.intValue());

    final Rectangle body = read(this::popoverBodyInPane);
    final Rectangle anchor =
        read(
            () ->
                SwingUtilities.convertRectangle(
                    trigger.getParent(), trigger.getBounds(), frame.getLayeredPane()));
    assertThat(body.y)
        .as("the painted body sits below the anchor — placement only a realized anchor establishes")
        .isGreaterThanOrEqualTo(anchor.y + anchor.height);
    assertThat(body.x).as("leading edges aligned").isEqualTo(anchor.x);
  }

  @Test
  void aPressOutsideLightDismissesThePopover() throws Exception {
    final List<String> dismissals = new ArrayList<>();
    final ElwhaColorPickerPopover popover = new ElwhaColorPickerPopover();
    popover.onDismiss(() -> dismissals.add("closed"));
    teardown.add(popover::close);
    SwingUtilities.invokeAndWait(() -> popover.show(trigger));
    waitFor("the popover is on screen", () -> popover.isShowing());

    GuiSteps.clickUntil(
        robot,
        frame,
        () -> centerOffEdt(sink),
        "a press outside the popover dismisses it",
        () -> !popover.isShowing());

    assertThat(dismissals).as("and the dismiss hook fires once").hasSize(1);
    assertThat(read(() -> mounted().size())).isZero();
  }

  @Test
  void escapeDismissesThePopover() throws Exception {
    final ElwhaColorPickerPopover popover = new ElwhaColorPickerPopover();
    teardown.add(popover::close);
    SwingUtilities.invokeAndWait(() -> popover.show(trigger));
    waitFor("the popover is on screen", () -> popover.isShowing());

    GuiSteps.keyUntil(
        robot, KeyEvent.VK_ESCAPE, "Escape dismisses the popover", () -> !popover.isShowing());

    assertThat(read(() -> mounted().size())).isZero();
  }

  @Test
  void aPopoverReportsTheColorItWasSeededWith() throws Exception {
    final ElwhaColorPickerPopover popover = new ElwhaColorPickerPopover();
    popover.setInitialColor(new Color(0x3366CC));
    teardown.add(popover::close);

    SwingUtilities.invokeAndWait(() -> popover.show(trigger));
    waitFor("the popover is on screen", () -> popover.isShowing());

    assertThat(read(popover::getColor))
        .as("the seeded colour survives the shell being realized")
        .isEqualTo(new Color(0x3366CC));
  }

  // ------------------------------------------------------------------ dialog

  @Test
  void aDialogOpensOverTheContentOnTheDialogBand() throws Exception {
    final ElwhaColorPickerDialog dialog = new ElwhaColorPickerDialog();
    teardown.add(dialog::dismiss);

    SwingUtilities.invokeAndWait(() -> dialog.show(trigger));
    waitFor("the dialog and its scrim are mounted", () -> mounted().size() == 2);

    for (final Component child : read(this::mounted)) {
      assertThat(read(() -> frame.getLayeredPane().getLayer(child)))
          .as("a colour-picker dialog is an ordinary Elwha dialog on the modal band")
          .isEqualTo(JLayeredPane.MODAL_LAYER.intValue());
    }
  }

  @Test
  void confirmingThroughARealClickReportsTheChosenColor() throws Exception {
    final List<Color> confirmed = new ArrayList<>();
    final ElwhaColorPickerDialog dialog = new ElwhaColorPickerDialog();
    dialog.setInitialColor(new Color(0x3366CC));
    dialog.onConfirm(confirmed::add);
    teardown.add(dialog::dismiss);
    SwingUtilities.invokeAndWait(() -> dialog.show(trigger));
    waitFor("the dialog is mounted", () -> mounted().size() == 2);

    final Component confirm = read(() -> namedDescendant((Container) mounted().get(0), "OK"));
    assertThat(confirm).as("the dialog offers a confirming action").isNotNull();

    GuiSteps.clickUntil(
        robot,
        frame,
        () -> centerOffEdt(confirm),
        "clicking the confirming action commits the colour",
        () -> !confirmed.isEmpty());

    assertThat(confirmed)
        .as("with the colour the picker was showing")
        .containsExactly(new Color(0x3366CC));
    waitFor("and the dialog closes", () -> mounted().isEmpty());
  }

  @Test
  void cancellingThroughARealClickReportsNoColor() throws Exception {
    final List<Color> confirmed = new ArrayList<>();
    final List<String> cancelled = new ArrayList<>();
    final ElwhaColorPickerDialog dialog = new ElwhaColorPickerDialog();
    dialog.onConfirm(confirmed::add);
    dialog.onCancel(() -> cancelled.add("cancelled"));
    teardown.add(dialog::dismiss);
    SwingUtilities.invokeAndWait(() -> dialog.show(trigger));
    waitFor("the dialog is mounted", () -> mounted().size() == 2);

    final Component cancel = read(() -> namedDescendant((Container) mounted().get(0), "Cancel"));
    assertThat(cancel).as("the dialog offers a cancelling action").isNotNull();

    GuiSteps.clickUntil(
        robot,
        frame,
        () -> centerOffEdt(cancel),
        "clicking cancel dismisses without a colour",
        () -> !cancelled.isEmpty());

    assertThat(confirmed).as("no colour is reported on a cancel").isEmpty();
    waitFor("and the dialog closes", () -> mounted().isEmpty());
  }

  @Test
  void escapeCancelsTheDialog() throws Exception {
    final List<String> cancelled = new ArrayList<>();
    final ElwhaColorPickerDialog dialog = new ElwhaColorPickerDialog();
    dialog.onCancel(() -> cancelled.add("cancelled"));
    teardown.add(dialog::dismiss);
    SwingUtilities.invokeAndWait(() -> dialog.show(trigger));
    waitFor("the dialog is mounted", () -> mounted().size() == 2);

    GuiSteps.keyUntil(
        robot, KeyEvent.VK_ESCAPE, "Escape dismisses the dialog", () -> mounted().isEmpty());

    assertThat(cancelled).as("an Escape out of a colour dialog is a cancel").hasSize(1);
    assertThat(onEdt(() -> mounted().isEmpty())).isTrue();
  }
}
