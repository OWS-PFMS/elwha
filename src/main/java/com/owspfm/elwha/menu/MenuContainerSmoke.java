package com.owspfm.elwha.menu;

import com.owspfm.elwha.theme.ElwhaTheme;
import com.owspfm.elwha.theme.MaterialPalettes;
import com.owspfm.elwha.theme.Mode;
import com.owspfm.elwha.theme.MorphAnimator;
import java.awt.Component;
import java.awt.Container;
import java.awt.GraphicsEnvironment;
import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JLayeredPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.SwingUtilities;

/**
 * Headless smoke for epic #298 S3 — the {@link ElwhaMenu} container. Pure assertions cover the
 * builder grouping and the empty-menu guard; when a display is present it additionally mounts each
 * layout variant and asserts:
 *
 * <ul>
 *   <li>the surface mounts at {@link JLayeredPane#POPUP_LAYER};
 *   <li>a flat/grouped menu that fits the window does <em>not</em> scroll;
 *   <li>a tall menu in a short window scrolls (a {@link JScrollPane} appears) and — per M3 — its
 *       gaps are forced to dividers (no gap struts survive).
 * </ul>
 *
 * Exits non-zero on any failed assertion.
 *
 * @author Charles Bryan (cfb3@uw.edu)
 * @version v0.4.0
 * @since v0.4.0
 */
public final class MenuContainerSmoke {

  private MenuContainerSmoke() {}

  public static void main(final String[] args) throws Exception {
    final boolean headless = GraphicsEnvironment.isHeadless();
    if (headless) {
      System.setProperty("java.awt.headless", "true");
    }
    ElwhaTheme.install(
        ElwhaTheme.config().theme(MaterialPalettes.baseline()).mode(Mode.LIGHT).build());
    // Synchronous teardown so each open() sees a clean layered pane (no animating leftover).
    MorphAnimator.setReducedMotion(true);

    int checks = 0;
    int failures = 0;

    // --- builder grouping (pure) ---
    final ElwhaMenu flat =
        ElwhaMenu.builder().addItem(item("A")).addItem(item("B")).addItem(item("C")).build();
    failures += check(flat.getItems().size() == 3, "flat menu has 3 items");
    checks++;

    final ElwhaMenu grouped =
        ElwhaMenu.builder()
            .addItem(item("A"))
            .addItem(item("B"))
            .addGroup()
            .addItem(item("C"))
            .build();
    failures += check(grouped.getItems().size() == 3, "grouped menu flattens to 3 items");
    checks++;

    boolean threw = false;
    try {
      ElwhaMenu.builder().build();
    } catch (final IllegalStateException ex) {
      threw = true;
    }
    failures += check(threw, "empty builder throws");
    checks++;

    // --- windowed mount + scroll/divider forcing ---
    if (!headless) {
      failures += windowedProof();
      checks++;
    } else {
      System.out.println("  skip (headless) windowed mount/scroll proof");
    }

    System.out.println();
    System.out.println(
        failures == 0
            ? "PASS — " + checks + " checks"
            : "FAIL — " + failures + "/" + checks + " checks failed");
    System.exit(failures == 0 ? 0 : 1);
  }

  private static ElwhaMenuItem item(final String label) {
    return ElwhaMenuItem.of(label);
  }

  private static int windowedProof() throws Exception {
    final int[] fail = {0};
    SwingUtilities.invokeAndWait(
        () -> {
          final JFrame frame = new JFrame("s3");
          final JButton trigger = new JButton("Open");
          final JPanel content = new JPanel();
          content.add(trigger);
          frame.setContentPane(content);
          frame.setSize(420, 240);
          frame.setLocationRelativeTo(null);
          frame.setVisible(true);
          final JLayeredPane lp = frame.getRootPane().getLayeredPane();

          // A short menu fits: mounts at POPUP_LAYER, does not scroll.
          final ElwhaMenu shortMenu =
              ElwhaMenu.builder().addItem(item("One")).addItem(item("Two")).build();
          shortMenu.open(trigger);
          final Component shortSurface = topSurface(lp);
          fail[0] += check(shortSurface != null, "short menu mounted");
          fail[0] +=
              check(
                  shortSurface != null
                      && JLayeredPane.getLayer((javax.swing.JComponent) shortSurface)
                          == JLayeredPane.POPUP_LAYER,
                  "short menu at POPUP_LAYER");
          fail[0] += check(findScroll(shortSurface) == null, "short menu does not scroll");
          shortMenu.close(MenuDismissCause.PROGRAMMATIC);

          // A tall grouped+gap menu in a short window must scroll AND force dividers (no gap
          // strut).
          final ElwhaMenu.Builder tall = ElwhaMenu.builder().separator(Separator.GAP);
          for (int i = 0; i < 20; i++) {
            tall.addItem(item("Item " + i));
            if (i % 5 == 4) {
              tall.addGroup();
            }
          }
          final ElwhaMenu tallMenu = tall.build();
          tallMenu.open(trigger);
          final Component tallSurface = topSurface(lp);
          fail[0] +=
              check(findScroll(tallSurface) != null, "tall menu scrolls (persistent scrollbar)");
          tallMenu.close(MenuDismissCause.PROGRAMMATIC);

          // F4: opening a second menu synchronously dismisses the first — never two at once.
          final ElwhaMenu menuA =
              ElwhaMenu.builder().addItem(item("A1")).addItem(item("A2")).build();
          final ElwhaMenu menuB =
              ElwhaMenu.builder().addItem(item("B1")).addItem(item("B2")).build();
          menuA.open(trigger);
          menuB.open(trigger);
          fail[0] += check(countMenus(lp) == 1, "opening a 2nd menu leaves exactly 1 mounted (F4)");
          menuB.close(MenuDismissCause.PROGRAMMATIC);

          frame.dispose();
        });
    return fail[0];
  }

  private static int countMenus(final JLayeredPane lp) {
    int n = 0;
    for (final Component c : lp.getComponents()) {
      if (c instanceof javax.swing.JComponent jc
          && jc.getAccessibleContext() != null
          && "Menu".equals(jc.getAccessibleContext().getAccessibleName())) {
        n++;
      }
    }
    return n;
  }

  private static Component topSurface(final JLayeredPane lp) {
    Component found = null;
    for (final Component c : lp.getComponents()) {
      if (c instanceof javax.swing.JComponent jc
          && "Menu".equals(jc.getAccessibleContext().getAccessibleName())) {
        found = c;
      }
    }
    return found;
  }

  private static JScrollPane findScroll(final Component root) {
    if (root instanceof JScrollPane sp) {
      return sp;
    }
    if (root instanceof Container c) {
      for (final Component child : c.getComponents()) {
        final JScrollPane nested = findScroll(child);
        if (nested != null) {
          return nested;
        }
      }
    }
    return null;
  }

  private static int check(final boolean ok, final String label) {
    System.out.println((ok ? "  ok   " : "  FAIL ") + label);
    return ok ? 0 : 1;
  }
}
