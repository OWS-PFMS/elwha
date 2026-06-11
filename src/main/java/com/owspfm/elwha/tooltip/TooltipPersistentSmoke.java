package com.owspfm.elwha.tooltip;

import com.owspfm.elwha.theme.ElwhaTheme;
import com.owspfm.elwha.theme.MaterialPalettes;
import com.owspfm.elwha.theme.Mode;
import com.owspfm.elwha.theme.MorphAnimator;
import java.awt.Component;
import java.awt.GraphicsEnvironment;
import java.awt.Toolkit;
import java.awt.event.KeyEvent;
import java.awt.event.MouseEvent;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.BooleanSupplier;
import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JLayeredPane;
import javax.swing.JPanel;
import javax.swing.SwingUtilities;

/**
 * Guard for #450 — the persistent-rich contract: {@code setPersistent} variant guard and builder
 * parity, then — windowed — the behavioral split end to end via posted events: hover is fully
 * disarmed, the anchor press toggles on and off, presses inside the contents are immune, an outside
 * press dismisses, Enter/Space on the anchor toggles, and one-at-a-time eviction holds across
 * flavors (a hover tooltip evicts a shown persistent one).
 *
 * @author Charles Bryan (cfb3@uw.edu)
 * @version v0.4.0
 * @since v0.4.0
 */
public final class TooltipPersistentSmoke {

  private static int checks;
  private static int failures;

  private TooltipPersistentSmoke() {}

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
      System.out.println("(headless — windowed behavior checks skipped)");
    }

    System.out.println(
        failures == 0 ? "PASS — " + checks + " checks" : "FAIL — " + failures + "/" + checks);
    System.exit(failures == 0 ? 0 : 1);
  }

  private static void contractChecks() {
    boolean threw = false;
    try {
      ElwhaTooltip.plain("p").setPersistent(true);
    } catch (final IllegalStateException e) {
      threw = true;
    }
    check(threw, "setPersistent on PLAIN throws");

    final ElwhaTooltip viaBuilder =
        ElwhaTooltip.rich().supportingText("b").persistent(true).build();
    check(viaBuilder.isPersistent(), "builder persistent(true) sticks");
    viaBuilder.setPersistent(false);
    check(!viaBuilder.isPersistent(), "setPersistent round-trips");
    check(!ElwhaTooltip.plain("p").isPersistent(), "plain reports not persistent");
  }

  private static void behaviorChecks() throws Exception {
    final AtomicReference<JFrame> frameRef = new AtomicReference<>();
    final AtomicReference<JButton> anchorRef = new AtomicReference<>();
    final AtomicReference<JButton> hoverAnchorRef = new AtomicReference<>();
    final AtomicReference<JPanel> panelRef = new AtomicReference<>();
    SwingUtilities.invokeAndWait(
        () -> {
          final JFrame frame = new JFrame("TooltipPersistentSmoke");
          final JPanel panel = new JPanel(new java.awt.GridBagLayout());
          final JPanel row = new JPanel();
          final JButton anchorButton = new JButton("persistent");
          final JButton hoverButton = new JButton("hover");
          row.add(anchorButton);
          row.add(hoverButton);
          panel.add(row);
          frame.add(panel);
          frame.setSize(560, 420);
          final java.awt.Rectangle screen = frame.getGraphicsConfiguration().getBounds();
          frame.setLocation(screen.x + screen.width - 580, screen.y + screen.height - 460);
          frame.setVisible(true);
          frameRef.set(frame);
          anchorRef.set(anchorButton);
          hoverAnchorRef.set(hoverButton);
          panelRef.set(panel);
        });
    Thread.sleep(250);

    final ElwhaTooltip persistent =
        ElwhaTooltip.rich()
            .subhead("Persistent")
            .supportingText("toggled, not hovered")
            .persistent(true)
            .build();
    final ElwhaTooltip hover = ElwhaTooltip.plain("hover flavor");
    hover.setShowDelayMs(60);
    hover.setHideDelayMs(120);
    SwingUtilities.invokeAndWait(
        () -> {
          persistent.attach(anchorRef.get());
          hover.attach(hoverAnchorRef.get());
        });

    // Hover is disarmed.
    postMouse(anchorRef.get(), MouseEvent.MOUSE_ENTERED);
    Thread.sleep(300);
    check(!persistent.isTooltipShowing(), "hover does not show a persistent tooltip");

    // Click toggles on, click toggles off.
    postMouse(anchorRef.get(), MouseEvent.MOUSE_PRESSED);
    check(waitFor(persistent::isTooltipShowing, 1000), "anchor press toggles on");
    postMouse(anchorRef.get(), MouseEvent.MOUSE_PRESSED);
    check(waitFor(() -> !persistent.isTooltipShowing(), 1000), "anchor press toggles off");

    // Inside press is immune; outside press dismisses.
    postMouse(anchorRef.get(), MouseEvent.MOUSE_PRESSED);
    check(waitFor(persistent::isTooltipShowing, 1000), "toggled on for the immunity check");
    final AtomicReference<TooltipSurface> surfaceRef = new AtomicReference<>();
    SwingUtilities.invokeAndWait(
        () -> {
          final JLayeredPane pane = frameRef.get().getRootPane().getLayeredPane();
          for (final Component c : pane.getComponentsInLayer(JLayeredPane.POPUP_LAYER)) {
            if (c instanceof TooltipSurface s) {
              surfaceRef.set(s);
            }
          }
        });
    check(surfaceRef.get() != null, "persistent surface mounted");
    postMouse(surfaceRef.get(), MouseEvent.MOUSE_PRESSED);
    Thread.sleep(250);
    check(persistent.isTooltipShowing(), "press inside the contents does not dismiss");
    postMouse(panelRef.get(), MouseEvent.MOUSE_PRESSED);
    check(waitFor(() -> !persistent.isTooltipShowing(), 1000), "outside press dismisses");

    // Enter/Space toggles.
    postKey(anchorRef.get(), KeyEvent.VK_SPACE);
    check(waitFor(persistent::isTooltipShowing, 1000), "Space on the anchor toggles on");
    postKey(anchorRef.get(), KeyEvent.VK_ENTER);
    check(waitFor(() -> !persistent.isTooltipShowing(), 1000), "Enter on the anchor toggles off");

    // Cross-flavor eviction: persistent up, then hover the other anchor.
    postMouse(anchorRef.get(), MouseEvent.MOUSE_PRESSED);
    check(waitFor(persistent::isTooltipShowing, 1000), "persistent up for the eviction check");
    postMouse(hoverAnchorRef.get(), MouseEvent.MOUSE_ENTERED);
    check(waitFor(hover::isTooltipShowing, 1500), "hover tooltip shows");
    check(
        waitFor(() -> !persistent.isTooltipShowing(), 1000),
        "hover show evicts the persistent one (one at a time)");
    SwingUtilities.invokeAndWait(hover::dismiss);

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
                Math.max(1, target.getWidth() / 2),
                Math.max(1, target.getHeight() / 2),
                0,
                false,
                id == MouseEvent.MOUSE_PRESSED ? MouseEvent.BUTTON1 : MouseEvent.NOBUTTON));
  }

  private static void postKey(final Component target, final int keyCode) {
    Toolkit.getDefaultToolkit()
        .getSystemEventQueue()
        .postEvent(
            new KeyEvent(
                target,
                KeyEvent.KEY_PRESSED,
                System.currentTimeMillis(),
                0,
                keyCode,
                KeyEvent.CHAR_UNDEFINED));
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
