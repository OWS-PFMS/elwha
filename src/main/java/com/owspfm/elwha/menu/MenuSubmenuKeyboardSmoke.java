package com.owspfm.elwha.menu;

import com.owspfm.elwha.theme.ElwhaTheme;
import com.owspfm.elwha.theme.MaterialPalettes;
import com.owspfm.elwha.theme.Mode;
import com.owspfm.elwha.theme.MorphAnimator;
import java.awt.Component;
import java.awt.GraphicsEnvironment;
import java.awt.event.ActionEvent;
import java.util.ArrayList;
import java.util.List;
import javax.accessibility.AccessibleRole;
import javax.accessibility.AccessibleState;
import javax.swing.Action;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JFrame;
import javax.swing.JLayeredPane;
import javax.swing.JPanel;
import javax.swing.SwingUtilities;

/**
 * Headless + windowed guard for epic #322 S4 — keyboard chain navigation and nested accessibility.
 * The pure portion asserts the {@code EXPANDABLE}/{@code COLLAPSED} trigger states and the {@code
 * POPUP_MENU} surface role; the windowed portion drives the bound Right/Left key actions and
 * asserts the chain opens/closes accordingly, the trigger reports {@code EXPANDED} with the open
 * submenu as its accessible child, and focus restores to the opener item. Exits non-zero on any
 * failed assertion.
 *
 * @author Charles Bryan (cfb3@uw.edu)
 * @version v0.4.0
 * @since v0.4.0
 */
public final class MenuSubmenuKeyboardSmoke {

  private MenuSubmenuKeyboardSmoke() {}

  public static void main(final String[] args) throws Exception {
    final boolean headless = GraphicsEnvironment.isHeadless();
    if (headless) {
      System.setProperty("java.awt.headless", "true");
    }
    ElwhaTheme.install(
        ElwhaTheme.config().theme(MaterialPalettes.baseline()).mode(Mode.LIGHT).build());
    MorphAnimator.setReducedMotion(true);

    int checks = 0;
    int failures = 0;

    // --- Pure a11y contract (headless) ---
    final ElwhaMenuItem plain = ElwhaMenuItem.of("Rename");
    failures +=
        check(
            !plain
                .getAccessibleContext()
                .getAccessibleStateSet()
                .contains(AccessibleState.EXPANDABLE),
            "a plain menu item is not EXPANDABLE");
    checks++;

    final ElwhaMenu leaf = ElwhaMenu.builder().addItem(ElwhaMenuItem.of("Email")).build();
    final ElwhaSubMenuItem sub = ElwhaSubMenuItem.of("Share", leaf);
    final var states = sub.getAccessibleContext().getAccessibleStateSet();
    failures += check(states.contains(AccessibleState.EXPANDABLE), "submenu trigger is EXPANDABLE");
    checks++;
    failures +=
        check(
            states.contains(AccessibleState.COLLAPSED)
                && !states.contains(AccessibleState.EXPANDED),
            "collapsed submenu trigger is COLLAPSED, not EXPANDED");
    checks++;
    failures +=
        check(
            leaf.renderPreview().getAccessibleContext().getAccessibleRole()
                == AccessibleRole.POPUP_MENU,
            "each menu surface is a POPUP_MENU");
    checks++;

    if (!headless) {
      failures += windowedKeyboardProof();
      checks += 7;
    } else {
      System.out.println("  skip (headless) windowed keyboard/a11y proof");
    }

    System.out.println();
    System.out.println(
        failures == 0
            ? "PASS — " + checks + " checks"
            : "FAIL — " + failures + "/" + checks + " checks failed");
    System.exit(failures == 0 ? 0 : 1);
  }

  private static int windowedKeyboardProof() throws Exception {
    final int[] fail = {0};
    SwingUtilities.invokeAndWait(
        () -> {
          final JFrame frame = new JFrame("keyboard smoke");
          final JButton anchor = new JButton("Open");
          final JPanel content = new JPanel();
          content.add(anchor);
          frame.setContentPane(content);
          frame.setSize(680, 460);
          frame.setLocationRelativeTo(null);
          frame.setVisible(true);
          final JLayeredPane lp = frame.getRootPane().getLayeredPane();

          final ElwhaMenu shareMenu =
              ElwhaMenu.builder().addItem(ElwhaMenuItem.of("Email")).build();
          final ElwhaSubMenuItem share = ElwhaSubMenuItem.of("Share", shareMenu);
          final ElwhaMenu rootMenu =
              ElwhaMenu.builder()
                  .addItem(ElwhaMenuItem.of("Rename"))
                  .addItem(share)
                  .addItem(ElwhaMenuItem.of("Delete"))
                  .build();

          rootMenu.open(anchor);
          lp.validate();
          final JComponent rootSurface = surfaces(lp).get(0);

          // Move the roving focus to the submenu item (index 1), then Right opens it.
          invoke(rootSurface, "menu.next");
          invoke(rootSurface, "menu.openSub");
          fail[0] += check(surfaces(lp).size() == 2, "Right-arrow opens the submenu via keyboard");
          fail[0] += check(share.isExpanded(), "trigger EXPANDED while submenu is open");
          fail[0] +=
              check(
                  share
                      .getAccessibleContext()
                      .getAccessibleStateSet()
                      .contains(AccessibleState.EXPANDED),
                  "trigger reports the EXPANDED a11y state");
          fail[0] +=
              check(
                  share.getAccessibleContext().getAccessibleChildrenCount() == 1
                      && share
                              .getAccessibleContext()
                              .getAccessibleChild(0)
                              .getAccessibleContext()
                              .getAccessibleRole()
                          == AccessibleRole.POPUP_MENU,
                  "open submenu is the trigger's accessible POPUP_MENU child");

          // Left closes the leaf back to its opener. Drive every mounted surface's Left action —
          // only the submenu (which has a chain parent) responds; the root is a no-op. (The layered
          // pane reorders on moveToFront, so an index can't identify the leaf reliably.)
          for (final JComponent s : surfaces(lp)) {
            invoke(s, "menu.closeSub");
          }
          fail[0] += check(surfaces(lp).size() == 1, "Left-arrow closes the submenu via keyboard");
          fail[0] += check(!share.isExpanded(), "trigger collapsed after Left");
          fail[0] += check(rootMenu.focusedIndex() == 1, "focus restored to the opener item");

          frame.dispose();
        });
    return fail[0];
  }

  private static void invoke(final JComponent surface, final String actionKey) {
    final Action action = surface.getActionMap().get(actionKey);
    if (action != null) {
      action.actionPerformed(new ActionEvent(surface, ActionEvent.ACTION_PERFORMED, actionKey));
    }
  }

  private static List<JComponent> surfaces(final JLayeredPane lp) {
    final List<JComponent> out = new ArrayList<>();
    for (final Component c : lp.getComponents()) {
      if (c instanceof JComponent jc
          && "Menu".equals(jc.getAccessibleContext().getAccessibleName())) {
        out.add(jc);
      }
    }
    return out;
  }

  private static int check(final boolean ok, final String label) {
    System.out.println((ok ? "  ok   " : "  FAIL ") + label);
    return ok ? 0 : 1;
  }
}
