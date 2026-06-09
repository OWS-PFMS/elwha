package com.owspfm.elwha.menu;

import com.owspfm.elwha.theme.ElwhaTheme;
import com.owspfm.elwha.theme.MaterialPalettes;
import com.owspfm.elwha.theme.Mode;
import com.owspfm.elwha.theme.MorphAnimator;
import com.owspfm.elwha.theme.ShapeScale;
import java.awt.GraphicsEnvironment;
import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JPanel;
import javax.swing.SwingUtilities;

/**
 * Headless + windowed guard for epic #322 S3 — the active-state corner shape-morph. The pure
 * portion pins the rest-shape triplet; the windowed portion (reduced motion → synchronous snap)
 * mounts a submenu chain and asserts the container radius tracks each level's chain role: a
 * standalone root is {@code MD}, a focused submenu rounds up to {@code LG}, and an ancestor squares
 * off to {@code SM}; the swap reverses as focus crosses levels back. Exits non-zero on any failed
 * assertion.
 *
 * @author Charles Bryan (cfb3@uw.edu)
 * @version v0.4.0
 * @since v0.4.0
 */
public final class MenuSubmenuMorphSmoke {

  private MenuSubmenuMorphSmoke() {}

  public static void main(final String[] args) throws Exception {
    final boolean headless = GraphicsEnvironment.isHeadless();
    if (headless) {
      System.setProperty("java.awt.headless", "true");
    }
    ElwhaTheme.install(
        ElwhaTheme.config().theme(MaterialPalettes.baseline()).mode(Mode.LIGHT).build());
    MorphAnimator.setReducedMotion(true);

    final int sm = ShapeScale.SM.px();
    final int md = ShapeScale.MD.px();
    final int lg = ShapeScale.LG.px();

    int checks = 0;
    int failures = 0;

    failures +=
        check(
            sm < md && md < lg,
            "rest-shape triplet SM < MD < LG (" + sm + "/" + md + "/" + lg + ")");
    checks++;

    if (!headless) {
      failures += windowedMorphProof(sm, md, lg);
      checks += 7;
    } else {
      System.out.println("  skip (headless) windowed shape-morph proof");
    }

    System.out.println();
    System.out.println(
        failures == 0
            ? "PASS — " + checks + " checks"
            : "FAIL — " + failures + "/" + checks + " checks failed");
    System.exit(failures == 0 ? 0 : 1);
  }

  private static int windowedMorphProof(final int sm, final int md, final int lg) throws Exception {
    final int[] fail = {0};
    SwingUtilities.invokeAndWait(
        () -> {
          final JFrame frame = new JFrame("morph smoke");
          final JButton anchor = new JButton("Open");
          final JPanel content = new JPanel();
          content.add(anchor);
          frame.setContentPane(content);
          frame.setSize(700, 480);
          frame.setLocationRelativeTo(null);
          frame.setVisible(true);

          final ElwhaMenu shareMenu =
              ElwhaMenu.builder().addItem(ElwhaMenuItem.of("Email")).build();
          final ElwhaSubMenuItem share = ElwhaSubMenuItem.of("Share", shareMenu);
          final ElwhaMenu rootMenu =
              ElwhaMenu.builder().addItem(ElwhaMenuItem.of("Rename")).addItem(share).build();

          rootMenu.open(anchor);
          frame.getRootPane().getLayeredPane().validate();
          fail[0] += check(rootMenu.currentContainerRadius() == md, "standalone root is MD");
          rootMenu.setMorphActive(true);
          fail[0] +=
              check(
                  rootMenu.currentContainerRadius() == md,
                  "a standalone menu ignores active state (stays MD)");

          share.activate(0);
          // The active level rounds up; the other squares off — and it reverses, the operator's
          // "submenu rounds on enter, squares on exit; the containing menu morphs opposite".
          shareMenu.setMorphActive(true);
          rootMenu.setMorphActive(false);
          fail[0] +=
              check(
                  shareMenu.currentContainerRadius() == lg,
                  "active (hovered) submenu rounds to LG");
          fail[0] +=
              check(rootMenu.currentContainerRadius() == sm, "the containing menu squares to SM");

          shareMenu.setMorphActive(false);
          rootMenu.setMorphActive(true);
          fail[0] +=
              check(
                  shareMenu.currentContainerRadius() == sm,
                  "submenu squares back when the pointer leaves it");
          fail[0] +=
              check(
                  rootMenu.currentContainerRadius() == lg,
                  "the containing menu rounds up opposite");

          // Closing the submenu collapses the chain — the lone root returns to the MD rest.
          shareMenu.close(MenuDismissCause.ESCAPE);
          fail[0] +=
              check(
                  rootMenu.currentContainerRadius() == md,
                  "the root returns to MD once the chain collapses");

          frame.dispose();
        });
    return fail[0];
  }

  private static int check(final boolean ok, final String label) {
    System.out.println((ok ? "  ok   " : "  FAIL ") + label);
    return ok ? 0 : 1;
  }
}
