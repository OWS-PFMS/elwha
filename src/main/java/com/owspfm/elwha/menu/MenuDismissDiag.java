package com.owspfm.elwha.menu;

import com.owspfm.elwha.button.ButtonInteractionMode;
import com.owspfm.elwha.button.ElwhaButton;
import com.owspfm.elwha.theme.ElwhaTheme;
import com.owspfm.elwha.theme.MaterialPalettes;
import com.owspfm.elwha.theme.Mode;
import com.owspfm.elwha.theme.MorphAnimator;
import java.awt.Component;
import java.awt.GraphicsEnvironment;
import java.awt.Point;
import java.awt.Robot;
import java.awt.event.InputEvent;
import javax.swing.JFrame;
import javax.swing.JLayeredPane;
import javax.swing.JPanel;
import javax.swing.SwingUtilities;

/**
 * Windowed regression guard for finding F7 — a menu must tear down on outside-click and item-click
 * with the entrance animation running (reduced-motion OFF), across many open/close cycles including
 * an F4 supersede. Robot-driven over three cycles; exits non-zero if any menu stays mounted after a
 * dismiss. Guards the gap that let F7 ship: the other menu smokes use reduced motion (synchronous
 * teardown) and so never exercised the animated close race (a dismiss within the entrance's first
 * tick, where {@code reverse()} from progress 0 produced no ticks and the overlay wedged open).
 * Needs a display.
 *
 * @author Charles Bryan (cfb3@uw.edu)
 * @version v0.4.0
 * @since v0.4.0
 */
public final class MenuDismissDiag {

  private MenuDismissDiag() {}

  private static JFrame frame;
  private static ElwhaButton trigger;
  private static ElwhaButton trigger2;
  private static int failures;

  public static void main(final String[] args) throws Exception {
    if (GraphicsEnvironment.isHeadless()) {
      System.out.println("needs a display; skipping");
      return;
    }
    ElwhaTheme.install(
        ElwhaTheme.config().theme(MaterialPalettes.baseline()).mode(Mode.LIGHT).build());
    MorphAnimator.setReducedMotion(false); // animated — the path the operator hit

    SwingUtilities.invokeAndWait(
        () -> {
          frame = new JFrame("dismiss-diag");
          trigger =
              ElwhaButton.outlinedButton("T1").setInteractionMode(ButtonInteractionMode.SELECTABLE);
          trigger2 =
              ElwhaButton.outlinedButton("T2").setInteractionMode(ButtonInteractionMode.SELECTABLE);
          final JPanel content = new JPanel();
          content.add(trigger);
          content.add(trigger2);
          frame.setContentPane(content);
          frame.setSize(600, 460);
          frame.setLocationRelativeTo(null);
          frame.setVisible(true);
        });
    pump();

    final Robot robot = new Robot();
    robot.setAutoDelay(40);

    // Run the dismissal probes several cycles to surface any static-state accumulation (OPEN
    // registry / openMenu tracker) like a real Showcase session.
    for (int cycle = 1; cycle <= 3; cycle++) {
      System.out.println("=== cycle " + cycle + " ===");
      probeOutsideClick(robot, cycle);
      probeItemClick(robot, cycle);
      probeF4ThenOutside(robot, cycle);
    }
    System.out.println();
    System.out.println(failures == 0 ? "PASS" : "FAIL — " + failures + " menu(s) stayed mounted");
    System.exit(failures == 0 ? 0 : 1);
  }

  private static void expectClosed(final String label, final MenuDismissCause cause) {
    final boolean ok = !mounted() && cause != null;
    if (!ok) {
      failures++;
    }
    System.out.println("  " + (ok ? "ok   " : "FAIL ") + label + " cause=" + cause);
  }

  private static void probeOutsideClick(final Robot robot, final int cycle) throws Exception {
    final MenuDismissCause[] c = {null};
    final ElwhaMenu m = twoItemMenu(c);
    SwingUtilities.invokeAndWait(() -> m.open(trigger));
    pump();
    clickScreen(robot, frame.getLocationOnScreen().x + 20, frame.getLocationOnScreen().y + 420);
    settle();
    expectClosed("outside-click", c[0]);
  }

  private static void probeItemClick(final Robot robot, final int cycle) throws Exception {
    final MenuDismissCause[] c = {null};
    final int[] fired = {0};
    final ElwhaMenuItem it = ElwhaMenuItem.of("Pick");
    it.addActionListener(e -> fired[0]++);
    final ElwhaMenu m =
        ElwhaMenu.builder()
            .addItem(it)
            .addItem(ElwhaMenuItem.of("Other"))
            .onClose(x -> c[0] = x)
            .build();
    SwingUtilities.invokeAndWait(() -> m.open(trigger));
    pump();
    final Point p = centerOnScreen(it);
    if (p != null) {
      clickScreen(robot, p.x, p.y);
      settle();
    }
    expectClosed("item-click (fired=" + fired[0] + ")", c[0]);
  }

  private static void probeF4ThenOutside(final Robot robot, final int cycle) throws Exception {
    final ElwhaMenu a = twoItemMenu(new MenuDismissCause[] {null});
    final MenuDismissCause[] cb = {null};
    final ElwhaMenu b = twoItemMenu(cb);
    SwingUtilities.invokeAndWait(() -> a.open(trigger));
    pump();
    SwingUtilities.invokeAndWait(() -> b.open(trigger2)); // F4 force-closes A
    pump();
    clickScreen(robot, frame.getLocationOnScreen().x + 20, frame.getLocationOnScreen().y + 420);
    settle();
    expectClosed("F4-then-outside", cb[0]);
  }

  private static ElwhaMenu twoItemMenu(final MenuDismissCause[] cause) {
    return ElwhaMenu.builder()
        .addItem(ElwhaMenuItem.of("One"))
        .addItem(ElwhaMenuItem.of("Two"))
        .onClose(c -> cause[0] = c)
        .build();
  }

  private static void clickScreen(final Robot robot, final int x, final int y) {
    robot.mouseMove(x, y);
    robot.mousePress(InputEvent.BUTTON1_DOWN_MASK);
    robot.mouseRelease(InputEvent.BUTTON1_DOWN_MASK);
  }

  private static Point centerOnScreen(final Component c) {
    if (!c.isShowing()) {
      return null;
    }
    final Point p = c.getLocationOnScreen();
    return new Point(p.x + c.getWidth() / 2, p.y + c.getHeight() / 2);
  }

  private static boolean mounted() {
    final JLayeredPane lp = frame.getRootPane().getLayeredPane();
    for (final Component c : lp.getComponents()) {
      if (c instanceof javax.swing.JComponent jc
          && jc.getAccessibleContext() != null
          && "Menu".equals(jc.getAccessibleContext().getAccessibleName())) {
        return true;
      }
    }
    return false;
  }

  private static void settle() throws Exception {
    pump();
    Thread.sleep(700); // longer than the 300ms exit animation
    pump();
  }

  private static void pump() throws Exception {
    SwingUtilities.invokeAndWait(() -> {});
    SwingUtilities.invokeAndWait(() -> {});
  }
}
