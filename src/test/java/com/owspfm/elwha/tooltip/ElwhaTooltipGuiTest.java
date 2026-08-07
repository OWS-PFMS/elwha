package com.owspfm.elwha.tooltip;

import static com.owspfm.elwha.testkit.WaitFor.onEdt;
import static com.owspfm.elwha.testkit.WaitFor.waitFor;
import static org.assertj.core.api.Assertions.assertThat;

import com.owspfm.elwha.button.ElwhaButton;
import com.owspfm.elwha.testkit.GuiToolkit;
import com.owspfm.elwha.testkit.ThemeExtension;
import com.owspfm.elwha.theme.Mode;
import com.owspfm.elwha.theme.MorphAnimator;
import java.awt.Component;
import java.awt.FlowLayout;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.Robot;
import java.awt.event.InputEvent;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Supplier;
import javax.swing.JFrame;
import javax.swing.SwingUtilities;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

/**
 * Tier B coverage of the tooltip's <em>trigger</em> — the one part of the component that is made of
 * a real pointer and a real clock. The dwell before a show, the linger after the pointer leaves,
 * the anchor&nbsp;∪&nbsp;surface union that keeps a tooltip open while the pointer crosses onto it,
 * and the self-re-arming expiry (epic&nbsp;#445) are all driven by {@code MouseInfo} polling and
 * platform enter/exit events; synthesizing them would test the synthesis.
 *
 * <p>Timing is asserted by polling for the state change, never by sleeping for a duration — the
 * delays are configured to their shortest useful values so the polls resolve quickly.
 *
 * <p><b>Why "away" means another control, not off-window.</b> {@code Robot.mouseMove} teleports
 * rather than travelling, so a jump from the anchor to a point outside every window crosses no
 * component boundary and this display stack delivers no exit or motion event at all — the linger is
 * never armed even once, and the tooltip stays up. A real pointer always crosses the window edge
 * and produces that exit. Moving to a sibling control is the same gesture with delivery this tier
 * can depend on; the off-window jump is left to manual smoke rather than faked here.
 */
@Tag("gui")
@ExtendWith(GuiToolkit.class)
class ElwhaTooltipGuiTest {

  private static final AtomicInteger FRAME_SLOT = new AtomicInteger();

  /** Short enough that a poll resolves fast, long enough that a dwell is still a dwell. */
  private static final int SHOW_DELAY_MS = 60;

  private static final int HIDE_DELAY_MS = 120;

  private JFrame frame;
  private ElwhaButton anchor;
  private ElwhaButton sink;
  private Robot robot;
  private final List<ElwhaTooltip> live = new ArrayList<>();

  @BeforeEach
  void showAnAnchorAndASink() throws Exception {
    robot = new Robot();
    robot.setAutoDelay(50);
    robot.setAutoWaitForIdle(true);
    final int slot = FRAME_SLOT.getAndIncrement();
    SwingUtilities.invokeAndWait(
        () -> {
          MorphAnimator.setReducedMotion(true);
          ThemeExtension.install(Mode.LIGHT);
          anchor = ElwhaButton.filledButton("Rename");
          sink = ElwhaButton.textButton("Elsewhere");
          frame = new JFrame("ElwhaTooltipGuiTest");
          frame.setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
          frame.setLayout(new FlowLayout(FlowLayout.LEADING, 40, 120));
          frame.add(anchor);
          frame.add(sink);
          frame.setSize(700, 420);
          frame.setLocation(40 + slot * 40, 40);
          frame.setVisible(true);
        });
    robot.waitForIdle();
    waitFor("frame gains focus", () -> frame.isFocused());
    SwingUtilities.invokeAndWait(() -> sink.requestFocusInWindow());
    waitFor("focus parks on the sink", () -> sink.isFocusOwner());
    parkPointerAway();
  }

  @AfterEach
  void disposeFrame() throws Exception {
    for (final ElwhaTooltip tooltip : live) {
      SwingUtilities.invokeAndWait(
          () -> {
            tooltip.detach();
            tooltip.dismiss();
          });
    }
    live.clear();
    SwingUtilities.invokeAndWait(() -> frame.dispose());
  }

  // ------------------------------------------------------------------ helpers

  private ElwhaTooltip attached(final String text) throws Exception {
    final ElwhaTooltip tooltip = ElwhaTooltip.plain(text);
    tooltip.setShowDelayMs(SHOW_DELAY_MS);
    tooltip.setHideDelayMs(HIDE_DELAY_MS);
    live.add(tooltip);
    SwingUtilities.invokeAndWait(() -> tooltip.attach(anchor));
    return tooltip;
  }

  private <T> T read(final Supplier<T> supplier) throws Exception {
    final AtomicReference<T> value = new AtomicReference<>();
    SwingUtilities.invokeAndWait(() -> value.set(supplier.get()));
    return value.get();
  }

  private Point centerOf(final Component component) throws Exception {
    return read(
        () -> {
          final Point origin = component.getLocationOnScreen();
          return new Point(
              origin.x + component.getWidth() / 2, origin.y + component.getHeight() / 2);
        });
  }

  /** Moves the pointer well outside the frame, so no hover machinery is armed. */
  private void parkPointerAway() {
    robot.mouseMove(2, 2);
    robot.waitForIdle();
  }

  private void hoverOver(final Component component) throws Exception {
    final Point center = centerOf(component);
    // Two moves: the first crosses into the component (the platform enter event), the second
    // settles inside it, so a coalesced single jump can never be the only motion delivered.
    robot.mouseMove(center.x, center.y - 2);
    robot.mouseMove(center.x, center.y);
    robot.waitForIdle();
  }

  // ------------------------------------------------------------------- dwell

  @Test
  void hoveringTheAnchorShowsTheTooltipAfterItsDwell() throws Exception {
    final ElwhaTooltip tooltip = attached("Rename this layer");

    hoverOver(anchor);

    waitFor(
        "a real pointer resting on the anchor past the dwell brings the tooltip up",
        () -> tooltip.isTooltipShowing());
  }

  @Test
  void aShownTooltipLandsAboveItsAnchorOnTheRealScreen() throws Exception {
    final ElwhaTooltip tooltip = attached("Rename this layer");
    hoverOver(anchor);
    waitFor("the tooltip is up", () -> tooltip.isTooltipShowing());

    final Rectangle surface = read(tooltip::surfaceScreenBounds);
    final Rectangle anchorBounds =
        read(() -> new Rectangle(anchor.getLocationOnScreen(), anchor.getSize()));

    assertThat(surface.y + surface.height)
        .as("the body sits above the anchor, which only a realized anchor can establish")
        .isLessThanOrEqualTo(anchorBounds.y);
    assertThat(surface.x + surface.width / 2)
        .as("centered on it, within a pixel of rounding")
        .isBetween(
            anchorBounds.x + anchorBounds.width / 2 - 2,
            anchorBounds.x + anchorBounds.width / 2 + 2);
  }

  @Test
  void anAnchorKeepsKeyboardFocusWhileTheTooltipIsUp() throws Exception {
    final ElwhaTooltip tooltip = attached("Rename this layer");

    hoverOver(anchor);
    waitFor("the tooltip is up", () -> tooltip.isTooltipShowing());

    assertThat(onEdt(() -> sink.isFocusOwner()))
        .as("a passive overlay never takes focus — the control the user was in keeps it")
        .isTrue();
  }

  // ------------------------------------------------------------------ linger

  @Test
  void movingThePointerAwayHidesTheTooltipAfterItsLinger() throws Exception {
    final ElwhaTooltip tooltip = attached("Rename this layer");
    hoverOver(anchor);
    waitFor("the tooltip is up", () -> tooltip.isTooltipShowing());

    hoverOver(sink);

    waitFor(
        "the linger expires against a pointer that is outside the anchor and the surface both",
        () -> !tooltip.isTooltipShowing());
  }

  @Test
  void crossingOntoTheTooltipItselfKeepsItOpen() throws Exception {
    final ElwhaTooltip tooltip = attached("Rename this layer");
    hoverOver(anchor);
    waitFor("the tooltip is up", () -> tooltip.isTooltipShowing());
    final Rectangle surface = read(tooltip::surfaceScreenBounds);

    robot.mouseMove(surface.x + surface.width / 2, surface.y + surface.height / 2);
    robot.waitForIdle();

    // The linger must expire at least once with the pointer in the union and re-arm rather than
    // giving a dismiss verdict — the epic #445 self-re-arming contract. Poll past two full lingers.
    final long deadline = System.currentTimeMillis() + 4L * HIDE_DELAY_MS;
    while (System.currentTimeMillis() < deadline) {
      assertThat(onEdt(() -> tooltip.isTooltipShowing()))
          .as("hovering the tooltip body is part of the hover union, so it stays up (WCAG 1.4.13)")
          .isTrue();
      Thread.sleep(20);
    }
  }

  @Test
  void leavingTheTooltipBodyStillHidesIt() throws Exception {
    final ElwhaTooltip tooltip = attached("Rename this layer");
    hoverOver(anchor);
    waitFor("the tooltip is up", () -> tooltip.isTooltipShowing());
    final Rectangle surface = read(tooltip::surfaceScreenBounds);
    robot.mouseMove(surface.x + surface.width / 2, surface.y + surface.height / 2);
    robot.waitForIdle();

    hoverOver(sink);

    waitFor(
        "once the pointer leaves the union the re-armed linger reaches a dismiss verdict",
        () -> !tooltip.isTooltipShowing());
  }

  // ------------------------------------------------------------------ press

  @Test
  void pressingTheAnchorDismissesATransientTooltip() throws Exception {
    final ElwhaTooltip tooltip = attached("Rename this layer");
    hoverOver(anchor);
    waitFor("the tooltip is up", () -> tooltip.isTooltipShowing());

    robot.mousePress(InputEvent.BUTTON1_DOWN_MASK);
    robot.mouseRelease(InputEvent.BUTTON1_DOWN_MASK);
    robot.waitForIdle();

    waitFor(
        "acting on the control makes its description noise, so the tooltip gets out of the way",
        () -> !tooltip.isTooltipShowing());
  }

  @Test
  void aPersistentRichTooltipTogglesOnAnchorPressesInstead() throws Exception {
    final ElwhaTooltip tooltip =
        ElwhaTooltip.rich().supportingText("Renames the selected layer").persistent(true).build();
    live.add(tooltip);
    SwingUtilities.invokeAndWait(() -> tooltip.attach(anchor));

    final Point center = centerOf(anchor);
    robot.mouseMove(center.x, center.y);
    robot.waitForIdle();
    // A persistent tooltip ignores hover entirely: only the press toggles it.
    assertThat(onEdt(() -> tooltip.isTooltipShowing()))
        .as("hovering a persistent tooltip's anchor does nothing")
        .isFalse();

    robot.mousePress(InputEvent.BUTTON1_DOWN_MASK);
    robot.mouseRelease(InputEvent.BUTTON1_DOWN_MASK);
    waitFor("the first press opens it", () -> tooltip.isTooltipShowing());

    robot.mousePress(InputEvent.BUTTON1_DOWN_MASK);
    robot.mouseRelease(InputEvent.BUTTON1_DOWN_MASK);
    waitFor(
        "and the second closes it — the anchor press is the toggle's sole authority",
        () -> !tooltip.isTooltipShowing());
  }

  // ------------------------------------------------------------ exclusivity

  @Test
  void showingASecondTooltipEvictsTheFirstOnTheRealDisplay() throws Exception {
    final ElwhaTooltip first = attached("Rename this layer");
    hoverOver(anchor);
    waitFor("the first tooltip is up", () -> first.isTooltipShowing());

    final ElwhaTooltip second = ElwhaTooltip.plain("Somewhere else");
    live.add(second);
    SwingUtilities.invokeAndWait(() -> second.show(sink));

    waitFor("the incumbent is evicted", () -> !first.isTooltipShowing());
    assertThat(onEdt(() -> second.isTooltipShowing())).isTrue();
  }
}
