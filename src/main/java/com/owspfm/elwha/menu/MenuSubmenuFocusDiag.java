package com.owspfm.elwha.menu;

import com.owspfm.elwha.theme.ElwhaTheme;
import com.owspfm.elwha.theme.MaterialPalettes;
import com.owspfm.elwha.theme.Mode;
import java.awt.Component;
import java.awt.GraphicsEnvironment;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JFrame;
import javax.swing.JLayeredPane;
import javax.swing.JPanel;
import javax.swing.SwingUtilities;

/**
 * Motion-on focus diagnostic for epic #322 — reproduces the operator's report that, with a 3-level
 * submenu chain open, collapsing the deepest level (a hover-away close) must NOT cascade the whole
 * chain shut via a transient focus bounce. Opens root → level&nbsp;2 → level&nbsp;3 with real
 * focus, then closes level&nbsp;3 the way hover-away does and asserts levels 1+2 stay open. Skipped
 * headless; exits non-zero if the chain over-collapses.
 *
 * @author Charles Bryan (cfb3@uw.edu)
 * @version v0.4.0
 * @since v0.4.0
 */
public final class MenuSubmenuFocusDiag {

  private MenuSubmenuFocusDiag() {}

  private static JFrame frame;
  private static JButton trigger;
  private static ElwhaMenu rootMenu;
  private static ElwhaSubMenuItem advanced;
  private static ElwhaMenu advancedMenu;
  private static ElwhaSubMenuItem share;

  public static void main(final String[] args) throws Exception {
    if (GraphicsEnvironment.isHeadless()) {
      System.out.println("skip (headless) — focus diagnostic needs a display");
      return;
    }
    ElwhaTheme.install(
        ElwhaTheme.config().theme(MaterialPalettes.baseline()).mode(Mode.LIGHT).build());

    SwingUtilities.invokeAndWait(MenuSubmenuFocusDiag::buildFrame);
    Thread.sleep(300);
    SwingUtilities.invokeAndWait(() -> rootMenu.open(trigger));
    Thread.sleep(300);
    SwingUtilities.invokeAndWait(() -> advanced.activate(0));
    Thread.sleep(300);
    SwingUtilities.invokeAndWait(() -> share.activate(0));
    Thread.sleep(400);

    final int[] before = {0};
    SwingUtilities.invokeAndWait(() -> before[0] = menuCount());
    System.out.println("  levels open after opening the 3-level chain: " + before[0]);

    // Collapse the deepest level the way a hover-away does (PROGRAMMATIC close of level 3).
    SwingUtilities.invokeAndWait(() -> advancedMenu.requestCloseSubMenu(share));
    Thread.sleep(700); // animated exit + focus settle

    final int[] after = {0};
    SwingUtilities.invokeAndWait(() -> after[0] = menuCount());
    System.out.println("  levels open after collapsing only level 3: " + after[0]);

    SwingUtilities.invokeAndWait(() -> frame.dispose());

    final boolean ok = before[0] == 3 && after[0] == 2;
    System.out.println(
        ok
            ? "PASS — root + level 2 stay open; only level 3 collapsed"
            : "FAIL — chain over-collapsed (before=" + before[0] + ", after=" + after[0] + ")");
    System.exit(ok ? 0 : 1);
  }

  private static void buildFrame() {
    frame = new JFrame("focus diag");
    trigger = new JButton("Open");
    final JPanel content = new JPanel();
    content.add(trigger);
    frame.setContentPane(content);
    frame.setSize(720, 480);
    frame.setLocationRelativeTo(null);
    frame.setVisible(true);
    frame.toFront();
    frame.requestFocus();

    final ElwhaMenu shareMenu =
        ElwhaMenu.builder()
            .addItem(ElwhaMenuItem.of("Email"))
            .addItem(ElwhaMenuItem.of("Copy link"))
            .build();
    share = ElwhaSubMenuItem.of("Share", shareMenu);
    advancedMenu = ElwhaMenu.builder().addItem(share).addItem(ElwhaMenuItem.of("Export")).build();
    advanced = ElwhaSubMenuItem.of("Advanced", advancedMenu);
    rootMenu =
        ElwhaMenu.builder()
            .addItem(ElwhaMenuItem.of("Rename"))
            .addItem(advanced)
            .addItem(ElwhaMenuItem.of("Delete"))
            .build();
  }

  private static int menuCount() {
    final JLayeredPane lp = frame.getRootPane().getLayeredPane();
    int n = 0;
    for (final Component c : lp.getComponents()) {
      if (c instanceof JComponent jc
          && "Menu".equals(jc.getAccessibleContext().getAccessibleName())) {
        n++;
      }
    }
    return n;
  }
}
