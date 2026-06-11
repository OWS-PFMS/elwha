package com.owspfm.elwha.tooltip;

import com.owspfm.elwha.theme.ElwhaTheme;
import com.owspfm.elwha.theme.MaterialPalettes;
import com.owspfm.elwha.theme.Mode;
import com.owspfm.elwha.theme.MorphAnimator;
import java.awt.Component;
import java.awt.GraphicsEnvironment;
import java.awt.Toolkit;
import java.awt.event.FocusEvent;
import java.awt.event.MouseEvent;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.BooleanSupplier;
import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JPanel;
import javax.swing.JTextField;
import javax.swing.SwingUtilities;

/**
 * Guard for #448 — the trigger contract: attach/detach lifecycle (re-attach throws, detach removes
 * every listener), delay-setter validation, and — when a display is available — the event-driven
 * behaviors end to end via posted AWT events: hover dwell, linger expiry with the pointer outside
 * the union, linger cancel on re-entry, press-to-dismiss, one-tooltip-at-a-time eviction,
 * keyboard-caused focus showing immediately while mouse-caused focus stays quiet, wheel dismissal,
 * and anchor-removal teardown.
 *
 * <p>Pointer-position-dependent paths (linger expiry, focus-lost) consult the <em>real</em> mouse
 * via {@link java.awt.MouseInfo}; the frame parks at the screen's bottom-right corner to keep the
 * physical pointer out of the union while events are synthesized.
 *
 * @author Charles Bryan (cfb3@uw.edu)
 * @version v0.4.0
 * @since v0.4.0
 */
public final class TooltipTriggerSmoke {

  private static int checks;
  private static int failures;

  private TooltipTriggerSmoke() {}

  /**
   * Runs the guard; exits non-zero on any failure.
   *
   * @param args unused
   * @throws Exception on EDT plumbing failures
   * @version v0.4.0
   * @since v0.4.0
   */
  public static void main(final String[] args) throws Exception {
    MorphAnimator.setReducedMotion(true);
    ElwhaTheme.install(
        ElwhaTheme.config().theme(MaterialPalettes.baseline()).mode(Mode.LIGHT).build());

    contractChecks();
    if (!GraphicsEnvironment.isHeadless()) {
      behaviorChecks();
    } else {
      System.out.println("(headless — event-driven behavior checks skipped)");
    }

    System.out.println(
        failures == 0 ? "PASS — " + checks + " checks" : "FAIL — " + failures + "/" + checks);
    System.exit(failures == 0 ? 0 : 1);
  }

  private static void contractChecks() {
    final ElwhaTooltip tip = ElwhaTooltip.plain("contract");
    final JButton a = new JButton("a");
    tip.attach(a);
    check(tip.getAttachedAnchor() == a, "attach records the anchor");
    boolean threw = false;
    try {
      tip.attach(new JButton("b"));
    } catch (final IllegalStateException e) {
      threw = true;
    }
    check(threw, "re-attach throws IllegalStateException");
    tip.detach();
    check(tip.getAttachedAnchor() == null, "detach clears the anchor");
    tip.attach(a);
    tip.detach();
    check(a.getMouseListeners().length <= 2, "detach removes the trigger mouse listener");

    boolean negative = false;
    try {
      tip.setShowDelayMs(-1);
    } catch (final IllegalArgumentException e) {
      negative = true;
    }
    check(negative, "negative show delay throws");
    check(
        tip.getShowDelayMs() == ElwhaTooltip.DEFAULT_SHOW_DELAY_MS
            && tip.getHideDelayMs() == ElwhaTooltip.DEFAULT_HIDE_DELAY_MS,
        "MDC default delays 500/600");
  }

  private static void behaviorChecks() throws Exception {
    final AtomicReference<JFrame> frameRef = new AtomicReference<>();
    final AtomicReference<JButton> anchorRef = new AtomicReference<>();
    final AtomicReference<JButton> otherRef = new AtomicReference<>();
    final AtomicReference<JTextField> fieldRef = new AtomicReference<>();
    final AtomicReference<JPanel> panelRef = new AtomicReference<>();
    SwingUtilities.invokeAndWait(
        () -> {
          final JFrame frame = new JFrame("TooltipTriggerSmoke");
          final JPanel panel = new JPanel(new java.awt.GridBagLayout());
          final JPanel row = new JPanel();
          final JButton anchorButton = new JButton("anchor");
          final JButton otherButton = new JButton("other");
          final JTextField field = new JTextField("park", 10);
          row.add(field);
          row.add(anchorButton);
          row.add(otherButton);
          panel.add(row);
          frame.add(panel);
          frame.setSize(520, 320);
          final java.awt.Rectangle screen = frame.getGraphicsConfiguration().getBounds();
          frame.setLocation(screen.x + screen.width - 540, screen.y + screen.height - 360);
          frame.setVisible(true);
          frameRef.set(frame);
          anchorRef.set(anchorButton);
          otherRef.set(otherButton);
          fieldRef.set(field);
          panelRef.set(panel);
        });
    Thread.sleep(250);

    final ElwhaTooltip tip = ElwhaTooltip.plain("triggered");
    tip.setShowDelayMs(60);
    tip.setHideDelayMs(120);
    final ElwhaTooltip otherTip = ElwhaTooltip.plain("the other one");
    otherTip.setShowDelayMs(60);
    otherTip.setHideDelayMs(120);
    SwingUtilities.invokeAndWait(
        () -> {
          tip.attach(anchorRef.get());
          otherTip.attach(otherRef.get());
        });

    // Hover dwell: enter → not yet → shown after the dwell.
    postMouse(anchorRef.get(), MouseEvent.MOUSE_ENTERED);
    pumpEdt();
    check(!tip.isTooltipShowing(), "no show before the dwell elapses");
    check(waitFor(tip::isTooltipShowing, 1500), "hover dwell shows the tooltip");

    // Linger: exit (real pointer is far away) → still up inside the linger → gone after it.
    postMouse(anchorRef.get(), MouseEvent.MOUSE_EXITED);
    pumpEdt();
    check(tip.isTooltipShowing(), "still shown the instant the pointer leaves");
    check(waitFor(() -> !tip.isTooltipShowing(), 2000), "linger expiry dismisses");

    // Linger cancel: show again, exit, re-enter inside the window — stays up.
    postMouse(anchorRef.get(), MouseEvent.MOUSE_ENTERED);
    check(waitFor(tip::isTooltipShowing, 1500), "re-show after dismissal works");
    postMouse(anchorRef.get(), MouseEvent.MOUSE_EXITED);
    Thread.sleep(40);
    postMouse(anchorRef.get(), MouseEvent.MOUSE_ENTERED);
    Thread.sleep(400);
    check(tip.isTooltipShowing(), "re-entering the anchor cancels the pending linger");

    // Press-to-dismiss.
    postMouse(anchorRef.get(), MouseEvent.MOUSE_PRESSED);
    check(waitFor(() -> !tip.isTooltipShowing(), 1000), "anchor press dismisses");

    // One at a time: show on anchor, then hover the other anchor — eviction on the second show.
    postMouse(anchorRef.get(), MouseEvent.MOUSE_ENTERED);
    check(waitFor(tip::isTooltipShowing, 1500), "first tooltip up");
    postMouse(otherRef.get(), MouseEvent.MOUSE_ENTERED);
    check(waitFor(otherTip::isTooltipShowing, 1500), "second tooltip shows");
    check(
        waitFor(() -> !tip.isTooltipShowing(), 1000),
        "showing the second evicts the first (one at a time)");
    SwingUtilities.invokeAndWait(otherTip::dismiss);
    pumpEdt();

    // Keyboard-caused focus shows immediately; mouse-caused focus stays quiet.
    final AtomicBoolean focused = new AtomicBoolean();
    SwingUtilities.invokeAndWait(
        () -> focused.set(fieldRef.get().requestFocusInWindow(FocusEvent.Cause.TRAVERSAL)));
    pumpEdt();
    if (focused.get() && frameRef.get().isFocused()) {
      SwingUtilities.invokeAndWait(
          () -> anchorRef.get().requestFocusInWindow(FocusEvent.Cause.TRAVERSAL));
      check(waitFor(tip::isTooltipShowing, 1000), "keyboard-caused focus shows immediately");
      SwingUtilities.invokeAndWait(
          () -> fieldRef.get().requestFocusInWindow(FocusEvent.Cause.TRAVERSAL));
      check(waitFor(() -> !tip.isTooltipShowing(), 1000), "focus leaving the anchor dismisses");
      SwingUtilities.invokeAndWait(
          () -> anchorRef.get().requestFocusInWindow(FocusEvent.Cause.MOUSE_EVENT));
      Thread.sleep(250);
      check(!tip.isTooltipShowing(), "mouse-caused focus does not trigger");
      SwingUtilities.invokeAndWait(
          () -> fieldRef.get().requestFocusInWindow(FocusEvent.Cause.TRAVERSAL));
      pumpEdt();
    } else {
      System.out.println("(window not focused — keyboard-focus trigger checks skipped)");
    }

    // Wheel dismisses.
    SwingUtilities.invokeAndWait(() -> tip.show(anchorRef.get()));
    pumpEdt();
    check(tip.isTooltipShowing(), "programmatic show for the wheel check");
    final Component panel = panelRef.get();
    Toolkit.getDefaultToolkit()
        .getSystemEventQueue()
        .postEvent(
            new java.awt.event.MouseWheelEvent(
                panel,
                MouseEvent.MOUSE_WHEEL,
                System.currentTimeMillis(),
                0,
                5,
                5,
                0,
                false,
                java.awt.event.MouseWheelEvent.WHEEL_UNIT_SCROLL,
                1,
                1));
    check(waitFor(() -> !tip.isTooltipShowing(), 1000), "wheel input dismisses");

    // Anchor removal dismisses.
    SwingUtilities.invokeAndWait(() -> tip.show(anchorRef.get()));
    pumpEdt();
    check(tip.isTooltipShowing(), "programmatic show for the removal check");
    SwingUtilities.invokeAndWait(() -> anchorRef.get().getParent().remove(anchorRef.get()));
    check(waitFor(() -> !tip.isTooltipShowing(), 1000), "anchor removal dismisses");

    SwingUtilities.invokeAndWait(() -> frameRef.get().dispose());
  }

  private static void postMouse(final Component target, final int id) {
    Toolkit.getDefaultToolkit()
        .getSystemEventQueue()
        .postEvent(
            new MouseEvent(
                target,
                id,
                System.currentTimeMillis(),
                0,
                target.getWidth() / 2,
                target.getHeight() / 2,
                0,
                false,
                id == MouseEvent.MOUSE_PRESSED ? MouseEvent.BUTTON1 : MouseEvent.NOBUTTON));
  }

  private static void pumpEdt() throws Exception {
    SwingUtilities.invokeAndWait(() -> {});
  }

  private static boolean waitFor(final BooleanSupplier condition, final long timeoutMs)
      throws Exception {
    final long deadline = System.currentTimeMillis() + timeoutMs;
    while (System.currentTimeMillis() < deadline) {
      final AtomicBoolean state = new AtomicBoolean();
      SwingUtilities.invokeAndWait(() -> state.set(condition.getAsBoolean()));
      if (state.get()) {
        return true;
      }
      Thread.sleep(20);
    }
    return false;
  }

  private static void check(final boolean ok, final String label) {
    checks++;
    if (!ok) {
      failures++;
      System.out.println("FAIL: " + label);
    } else {
      System.out.println("  ok: " + label);
    }
  }
}
