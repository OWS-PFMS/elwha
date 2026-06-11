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
 * mounts an A → B → C chain and asserts the M3 hover rule: opening a submenu morphs nothing on its
 * own, the submenu the pointer is <em>over</em> rounds up to {@code LG} while its immediate parent
 * squares off to {@code SM}, every other level (including a just-opened, unhovered submenu) rests
 * at {@code MD}, and it all reverses as the pointer moves between levels. Exits non-zero on any
 * failed assertion.
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
      checks += 13;
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

          // Chain A → B(share) → C(deep).
          final ElwhaMenu deepMenu = ElwhaMenu.builder().addItem(ElwhaMenuItem.of("Leaf")).build();
          final ElwhaSubMenuItem deep = ElwhaSubMenuItem.of("More", deepMenu);
          final ElwhaMenu shareMenu =
              ElwhaMenu.builder().addItem(ElwhaMenuItem.of("Email")).addItem(deep).build();
          final ElwhaSubMenuItem share = ElwhaSubMenuItem.of("Share", shareMenu);
          final ElwhaMenu rootMenu =
              ElwhaMenu.builder().addItem(ElwhaMenuItem.of("Rename")).addItem(share).build();

          rootMenu.open(anchor);
          frame.getRootPane().getLayeredPane().validate();
          fail[0] += check(rootMenu.currentContainerRadius() == md, "standalone root is MD");

          share.activate(0); // B opens — by itself this must NOT morph anything.
          rootMenu.applyActiveMorph(rootMenu); // pointer on A (the parent)
          fail[0] +=
              check(rootMenu.currentContainerRadius() == md, "open submenu does not morph A");
          fail[0] +=
              check(
                  shareMenu.currentContainerRadius() == md,
                  "a just-opened, unhovered submenu stays MD");

          rootMenu.applyActiveMorph(shareMenu); // pointer moves onto B
          fail[0] += check(shareMenu.currentContainerRadius() == lg, "hovering B rounds it to LG");
          fail[0] += check(rootMenu.currentContainerRadius() == sm, "A (B's parent) squares to SM");

          rootMenu.applyActiveMorph(rootMenu); // pointer back onto A
          fail[0] +=
              check(
                  shareMenu.currentContainerRadius() == md
                      && rootMenu.currentContainerRadius() == md,
                  "moving back onto A rests both at MD");

          deep.activate(0); // C opens (chain A → B → C)
          rootMenu.applyActiveMorph(deepMenu); // pointer onto C
          fail[0] += check(deepMenu.currentContainerRadius() == lg, "hovering C rounds it to LG");
          fail[0] +=
              check(shareMenu.currentContainerRadius() == sm, "B (C's parent) squares to SM");
          fail[0] +=
              check(
                  rootMenu.currentContainerRadius() == md,
                  "A rests at MD when the pointer is on C");

          rootMenu.applyActiveMorph(shareMenu); // pointer back onto B
          fail[0] +=
              check(deepMenu.currentContainerRadius() == md, "C rests at MD while B is hovered");
          fail[0] += check(shareMenu.currentContainerRadius() == lg, "B rounds back to LG");
          fail[0] +=
              check(rootMenu.currentContainerRadius() == sm, "A squares only when B is hovered");

          shareMenu.close(MenuDismissCause.ESCAPE); // collapses B (and C) back to the lone root
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
