package com.owspfm.elwha.menu;

import com.owspfm.elwha.icons.MaterialIcons;
import com.owspfm.elwha.theme.ElwhaTheme;
import com.owspfm.elwha.theme.MaterialPalettes;
import com.owspfm.elwha.theme.Mode;
import com.owspfm.elwha.theme.MorphAnimator;
import java.awt.Component;
import java.awt.GraphicsEnvironment;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JFrame;
import javax.swing.JLayeredPane;
import javax.swing.JPanel;
import javax.swing.SwingUtilities;

/**
 * Headless + windowed guard for epic #322 S1 — the parent–child overlay <strong>chain</strong>
 * dismissal mechanics that lock design §2. The pure portion asserts the {@link ElwhaSubMenuItem}
 * contract; the windowed portion (skipped with no display) mounts a real submenu chain and drives
 * it, asserting:
 *
 * <ul>
 *   <li>opening a submenu mounts a <em>second</em> menu surface while the parent stays open (focus
 *       chain in, parent not dismissed);
 *   <li>arbitrary nesting depth — a second-level submenu mounts a third surface;
 *   <li>Esc/Left on the leaf closes <em>one</em> level and re-arms the parent's roving focus on the
 *       opener item (the opener's expanded state clears);
 *   <li>selecting a leaf action item closes the <em>whole</em> chain;
 *   <li>an outside-the-chain dismissal ({@code closeChain}) collapses every open level at once.
 * </ul>
 *
 * <p>Runs with reduced motion so teardown is synchronous and assertable on the same EDT tick; the
 * motion-on close race is covered by {@code MenuDismissDiag}. Exits non-zero on any failed
 * assertion.
 *
 * @author Charles Bryan (cfb3@uw.edu)
 * @version v0.4.0
 * @since v0.4.0
 */
public final class MenuChainDismissSmoke {

  private MenuChainDismissSmoke() {}

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

    // --- Pure ElwhaSubMenuItem contract (headless) ---
    final ElwhaMenu sub = ElwhaMenu.builder().addItem(ElwhaMenuItem.of("Leaf")).build();
    final ElwhaSubMenuItem trigger = ElwhaSubMenuItem.of(MaterialIcons.start(20), "Share", sub);
    failures += check(trigger.getSubMenu() == sub, "submenu item exposes its nested menu");
    checks++;
    failures += check(!trigger.isExpanded(), "submenu item starts collapsed");
    checks++;
    final ElwhaMenu root = ElwhaMenu.builder().addItem(trigger).build();
    failures += check(root.getItems().contains(trigger), "submenu item is a normal menu item");
    checks++;

    if (!headless) {
      failures += windowedChainProof();
      checks += WINDOWED_CHECKS;
    } else {
      System.out.println("  skip (headless) windowed submenu-chain proof");
    }

    System.out.println();
    System.out.println(
        failures == 0
            ? "PASS — " + checks + " checks"
            : "FAIL — " + failures + "/" + checks + " checks failed");
    System.exit(failures == 0 ? 0 : 1);
  }

  private static final int WINDOWED_CHECKS = 9;

  // Mounts a 3-level chain on a real frame and drives it directly (item activation = the same path
  // as click). Counts mounted menu surfaces to prove parent-stays-open, one-level unwind, and
  // chain-wide dismiss. Runs on the EDT under reduced motion (synchronous teardown).
  private static int windowedChainProof() throws Exception {
    final int[] fail = {0};
    SwingUtilities.invokeAndWait(
        () -> {
          final JFrame frame = new JFrame("chain smoke");
          final JButton anchor = new JButton("Open");
          final JPanel content = new JPanel();
          content.add(anchor);
          frame.setContentPane(content);
          frame.setSize(640, 460);
          frame.setLocationRelativeTo(null);
          frame.setVisible(true);
          final JLayeredPane lp = frame.getRootPane().getLayeredPane();

          // Level 3: Share › Email/Link.
          final ElwhaMenuItem email = ElwhaMenuItem.of("Email");
          final ElwhaMenu shareMenu =
              ElwhaMenu.builder().addItem(email).addItem(ElwhaMenuItem.of("Link")).build();
          // Level 2: Advanced › Share/More.
          final ElwhaSubMenuItem share = ElwhaSubMenuItem.of("Share", shareMenu);
          final ElwhaMenu advancedMenu =
              ElwhaMenu.builder().addItem(share).addItem(ElwhaMenuItem.of("More")).build();
          // Level 1 (root): Rename / Advanced / Delete.
          final ElwhaSubMenuItem advanced = ElwhaSubMenuItem.of("Advanced", advancedMenu);
          final MenuDismissCause[] rootClose = {null};
          final ElwhaMenu rootMenu =
              ElwhaMenu.builder()
                  .addItem(ElwhaMenuItem.of("Rename"))
                  .addItem(advanced)
                  .addItem(ElwhaMenuItem.of("Delete"))
                  .onClose(c -> rootClose[0] = c)
                  .build();

          rootMenu.open(anchor);
          fail[0] += check(menuCount(lp) == 1, "root menu mounted (count=" + menuCount(lp) + ")");

          advanced.activate(0);
          fail[0] +=
              check(
                  menuCount(lp) == 2,
                  "opening submenu keeps parent open (count=" + menuCount(lp) + ")");
          fail[0] += check(advanced.isExpanded(), "opener marked expanded while submenu is up");

          share.activate(0);
          fail[0] +=
              check(
                  menuCount(lp) == 3,
                  "nested submenu mounts a 3rd level (count=" + menuCount(lp) + ")");

          // Esc on the leaf (level 3) closes ONE level; level 2 stays, its opener re-armed.
          shareMenu.close(MenuDismissCause.ESCAPE);
          fail[0] +=
              check(menuCount(lp) == 2, "Esc closes one level only (count=" + menuCount(lp) + ")");
          fail[0] += check(!share.isExpanded(), "leaf-opener expanded cleared on one-level close");

          // Selecting a leaf action item closes the WHOLE chain.
          final ElwhaMenuItem more = advancedMenu.getItems().get(1);
          more.activate(0);
          fail[0] +=
              check(
                  menuCount(lp) == 0,
                  "leaf selection closes the whole chain (count=" + menuCount(lp) + ")");
          fail[0] +=
              check(rootClose[0] == MenuDismissCause.SELECTION, "root close cause = SELECTION");

          // Re-open and prove an outside-the-chain dismiss collapses every level at once.
          rootMenu.open(anchor);
          advanced.activate(0);
          advancedMenu.closeChain(MenuDismissCause.OUTSIDE_PRESS);
          fail[0] +=
              check(
                  menuCount(lp) == 0,
                  "outside-chain dismiss closes all (count=" + menuCount(lp) + ")");

          frame.dispose();
        });
    return fail[0];
  }

  private static int menuCount(final JLayeredPane lp) {
    int n = 0;
    for (final Component c : lp.getComponents()) {
      if (c instanceof JComponent jc
          && "Menu".equals(jc.getAccessibleContext().getAccessibleName())) {
        n++;
      }
    }
    return n;
  }

  private static int check(final boolean ok, final String label) {
    System.out.println((ok ? "  ok   " : "  FAIL ") + label);
    return ok ? 0 : 1;
  }
}
