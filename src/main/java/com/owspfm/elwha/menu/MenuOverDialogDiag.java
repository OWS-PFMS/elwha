package com.owspfm.elwha.menu;

import com.owspfm.elwha.button.ElwhaButton;
import com.owspfm.elwha.dialog.ElwhaDialog;
import com.owspfm.elwha.theme.ElwhaTheme;
import com.owspfm.elwha.theme.MaterialPalettes;
import com.owspfm.elwha.theme.Mode;
import com.owspfm.elwha.theme.MorphAnimator;
import java.awt.Component;
import java.awt.GraphicsEnvironment;
import java.awt.KeyboardFocusManager;
import java.awt.Point;
import java.awt.Robot;
import java.awt.event.InputEvent;
import javax.swing.JLayeredPane;
import javax.swing.JPanel;
import javax.swing.JRootPane;
import javax.swing.SwingUtilities;

/**
 * Windowed regression guard for finding F1 — a menu opened from inside a live {@link ElwhaDialog}
 * must light-dismiss. Opens a dialog, opens a menu from a body trigger, Robot-clicks outside the
 * menu, and asserts the menu tears down (exits non-zero if it lingers). Also logs the focus-owner
 * timeline so a future regression is diagnosable. The root cause was two overlapping focus listeners
 * (the dialog's modal trap + the menu's light-dismiss) fighting; fixed by having only the topmost
 * overlay react to focus escapes. Needs a display (Robot); skips headless.
 *
 * @author Charles Bryan (cfb3@uw.edu)
 * @version v0.4.0
 * @since v0.4.0
 */
public final class MenuOverDialogDiag {

  private MenuOverDialogDiag() {}

  public static void main(final String[] args) throws Exception {
    if (GraphicsEnvironment.isHeadless()) {
      System.out.println("needs a display; skipping");
      return;
    }
    ElwhaTheme.install(
        ElwhaTheme.config().theme(MaterialPalettes.baseline()).mode(Mode.LIGHT).build());
    // Default-motion (animated) run, matching how the operator runs the demos.
    MorphAnimator.setReducedMotion(false);

    KeyboardFocusManager.getCurrentKeyboardFocusManager()
        .addPropertyChangeListener(
            "focusOwner",
            e -> System.out.println("  focusOwner -> " + describe((Component) e.getNewValue())));

    final javax.swing.JFrame frame = new javax.swing.JFrame("diag");
    final ElwhaButton root = ElwhaButton.outlinedButton("root");
    SwingUtilities.invokeAndWait(
        () -> {
          final JPanel content = new JPanel();
          content.add(root);
          frame.setContentPane(content);
          frame.setSize(640, 480);
          frame.setLocationRelativeTo(null);
          frame.setVisible(true);
        });
    pump();

    // Open a dialog with an in-body trigger.
    final ElwhaButton inDialog = ElwhaButton.filledButton("open menu");
    final MenuDismissCause[] cause = {null};
    SwingUtilities.invokeAndWait(
        () -> {
          final JPanel body = new JPanel();
          body.add(inDialog);
          ElwhaDialog.builder()
              .headline("Dialog")
              .content(body)
              .confirmAction(ElwhaButton.textButton("Done"))
              .build()
              .show(root);
        });
    pump();
    System.out.println("after dialog open: focusOwner=" + describe(focusOwner()));

    final ElwhaMenu menu =
        ElwhaMenu.builder()
            .addItem(ElwhaMenuItem.of("Alpha"))
            .addItem(ElwhaMenuItem.of("Beta"))
            .onClose(c -> cause[0] = c)
            .build();
    SwingUtilities.invokeAndWait(() -> menu.open(inDialog));
    pump();
    System.out.println(
        "after menu.open: mounted=" + mounted(frame) + " focusOwner=" + describe(focusOwner()));

    // Real outside click: top-left corner of the frame (on the dialog scrim, outside the menu).
    final Robot robot = new Robot();
    robot.setAutoDelay(50);
    final Point origin = frame.getLocationOnScreen();
    robot.mouseMove(origin.x + 30, origin.y + 60);
    robot.mousePress(InputEvent.BUTTON1_DOWN_MASK);
    robot.mouseRelease(InputEvent.BUTTON1_DOWN_MASK);
    pump();
    Thread.sleep(600); // let the exit animation complete
    pump();
    System.out.println("after outside click: mounted=" + mounted(frame) + " cause=" + cause[0]);

    final boolean dismissed = cause[0] != null && !mounted(frame);
    System.out.println(
        dismissed ? "PASS — dismissed via " + cause[0] : "FAIL — menu did not dismiss (F1)");
    System.exit(dismissed ? 0 : 1);
  }

  private static Component focusOwner() {
    return KeyboardFocusManager.getCurrentKeyboardFocusManager().getFocusOwner();
  }

  private static boolean mounted(final javax.swing.JFrame frame) {
    final JRootPane rp = frame.getRootPane();
    final JLayeredPane lp = rp.getLayeredPane();
    for (final Component c : lp.getComponents()) {
      if (c instanceof javax.swing.JComponent jc
          && jc.getAccessibleContext() != null
          && "Menu".equals(jc.getAccessibleContext().getAccessibleName())) {
        return true;
      }
    }
    return false;
  }

  private static String describe(final Component c) {
    if (c == null) {
      return "null";
    }
    String name = c.getClass().getSimpleName();
    if (c instanceof javax.swing.JComponent jc && jc.getAccessibleContext() != null) {
      final String an = jc.getAccessibleContext().getAccessibleName();
      if (an != null && !an.isEmpty()) {
        name += "(" + an + ")";
      }
    }
    return name;
  }

  private static void pump() throws Exception {
    SwingUtilities.invokeAndWait(() -> {});
    SwingUtilities.invokeAndWait(() -> {});
  }
}
