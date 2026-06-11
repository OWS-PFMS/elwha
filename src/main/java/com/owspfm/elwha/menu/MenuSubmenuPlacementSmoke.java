package com.owspfm.elwha.menu;

import com.owspfm.elwha.theme.ElwhaTheme;
import com.owspfm.elwha.theme.MaterialPalettes;
import com.owspfm.elwha.theme.Mode;
import com.owspfm.elwha.theme.MorphAnimator;
import java.awt.Dimension;
import java.awt.GraphicsEnvironment;
import java.awt.Rectangle;
import javax.swing.Icon;
import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JPanel;
import javax.swing.SwingUtilities;

/**
 * Headless + windowed guard for epic #322 S2 — the {@link ElwhaSubMenuItem} caret contract and the
 * pure side-placement geometry ({@link AbstractElwhaMenuOverlay#placeBeside}). The pure portion
 * asserts the {@code START_END} side-anchor, the trailing→leading flip on viewport clip, the
 * vertical shift, and RTL mirroring; the windowed portion mounts a real submenu and asserts it
 * lands beside (not below) its opener. Exits non-zero on any failed assertion.
 *
 * @author Charles Bryan (cfb3@uw.edu)
 * @version v0.4.0
 * @since v0.4.0
 */
public final class MenuSubmenuPlacementSmoke {

  private MenuSubmenuPlacementSmoke() {}

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

    // --- Caret contract (headless) ---
    final ElwhaMenu leaf = ElwhaMenu.builder().addItem(ElwhaMenuItem.of("Leaf")).build();
    final ElwhaSubMenuItem item = ElwhaSubMenuItem.of("Share", leaf);
    failures += check(item.getTrailingIcon() != null, "submenu item auto-places a trailing caret");
    checks++;
    boolean threw = false;
    try {
      item.setTrailingIcon((Icon) null);
    } catch (final UnsupportedOperationException ok) {
      threw = true;
    }
    failures += check(threw, "setTrailingIcon is unsupported (slot reserved for the caret)");
    checks++;

    // --- Pure placeBeside geometry (headless) ---
    final Dimension pref = new Dimension(180, 120);
    final Rectangle opener = new Rectangle(100, 50, 200, 44);

    final Rectangle trailing = AbstractElwhaMenuOverlay.placeBeside(opener, pref, 600, 400, false);
    failures +=
        check(
            trailing.x == 100 + 200 + AbstractElwhaMenuOverlay.SUBMENU_GAP_PX && trailing.y == 50,
            "LTR opens to the trailing side, top-aligned: " + trailing);
    checks++;

    // Opener near the right edge: trailing clips, must flip to the leading (left) side.
    final Rectangle rightOpener = new Rectangle(500, 50, 80, 44);
    final Rectangle flipped =
        AbstractElwhaMenuOverlay.placeBeside(rightOpener, pref, 600, 400, false);
    failures +=
        check(
            flipped.x == 500 - AbstractElwhaMenuOverlay.SUBMENU_GAP_PX - 180,
            "flips to the leading side on trailing clip: " + flipped);
    checks++;

    // Opener near the bottom: surface shifts up to stay in the viewport.
    final Rectangle lowOpener = new Rectangle(100, 350, 80, 44);
    final Rectangle shifted =
        AbstractElwhaMenuOverlay.placeBeside(lowOpener, pref, 600, 400, false);
    failures += check(shifted.y + 120 <= 400, "shifts up to stay in the viewport: " + shifted);
    checks++;

    // RTL: trailing side is the left; with room on the left, opens to the left of the opener.
    final Rectangle rtlOpener = new Rectangle(300, 50, 80, 44);
    final Rectangle rtl = AbstractElwhaMenuOverlay.placeBeside(rtlOpener, pref, 600, 400, true);
    failures +=
        check(
            rtl.x == 300 - AbstractElwhaMenuOverlay.SUBMENU_GAP_PX - 180,
            "RTL opens to the leading (left) side: " + rtl);
    checks++;

    // RTL near the left edge: left side clips, flips to the right.
    final Rectangle leftOpener = new Rectangle(50, 50, 80, 44);
    final Rectangle rtlFlip =
        AbstractElwhaMenuOverlay.placeBeside(leftOpener, pref, 600, 400, true);
    failures +=
        check(
            rtlFlip.x == 50 + 80 + AbstractElwhaMenuOverlay.SUBMENU_GAP_PX,
            "RTL flips to the trailing (right) side on clip: " + rtlFlip);
    checks++;

    if (!headless) {
      failures += windowedSideProof();
      checks += 2;
    } else {
      System.out.println("  skip (headless) windowed side-placement proof");
    }

    System.out.println();
    System.out.println(
        failures == 0
            ? "PASS — " + checks + " checks"
            : "FAIL — " + failures + "/" + checks + " checks failed");
    System.exit(failures == 0 ? 0 : 1);
  }

  // Mounts a real submenu and asserts it lands beside (x shifted right past the root), not below
  // (same x as the trigger). Runs on the EDT under reduced motion.
  private static int windowedSideProof() throws Exception {
    final int[] fail = {0};
    SwingUtilities.invokeAndWait(
        () -> {
          final JFrame frame = new JFrame("placement smoke");
          final JButton anchor = new JButton("Open");
          final JPanel content = new JPanel();
          content.add(anchor);
          frame.setContentPane(content);
          frame.setSize(680, 480);
          frame.setLocationRelativeTo(null);
          frame.setVisible(true);

          final ElwhaMenu shareMenu =
              ElwhaMenu.builder()
                  .addItem(ElwhaMenuItem.of("Email"))
                  .addItem(ElwhaMenuItem.of("Copy link"))
                  .build();
          final ElwhaSubMenuItem share = ElwhaSubMenuItem.of("Share", shareMenu);
          final ElwhaMenu rootMenu =
              ElwhaMenu.builder()
                  .addItem(ElwhaMenuItem.of("Rename"))
                  .addItem(share)
                  .addItem(ElwhaMenuItem.of("Delete"))
                  .build();

          rootMenu.open(anchor);
          // The menu surface revalidates asynchronously after open; flush that layout now so the
          // opener item has its real width before the submenu anchors to it (a live user always
          // opens a submenu on a later EDT cycle, after this validation has run).
          frame.getRootPane().getLayeredPane().validate();
          share.activate(0);

          final Rectangle rootBounds = rootMenu.surfaceBounds();
          final Rectangle subBounds = shareMenu.surfaceBounds();
          fail[0] +=
              check(
                  rootBounds != null && subBounds != null, "both root + submenu surfaces mounted");
          if (rootBounds != null && subBounds != null) {
            // Side-placed: the submenu's top sits within the parent's vertical span (top-aligned to
            // an item), not stacked below it, and it is offset horizontally — left or right by the
            // flip. A below-placed menu would instead start at the parent's bottom edge.
            final boolean besideVertically =
                subBounds.y >= rootBounds.y - 8 && subBounds.y < rootBounds.y + rootBounds.height;
            final boolean offsetHorizontally = Math.abs(subBounds.x - rootBounds.x) > 24;
            fail[0] +=
                check(
                    besideVertically && offsetHorizontally,
                    "submenu side-placed beside the opener (sub="
                        + subBounds
                        + ", root="
                        + rootBounds
                        + ")");
          } else {
            fail[0]++;
          }
          frame.dispose();
        });
    return fail[0];
  }

  private static int check(final boolean ok, final String label) {
    System.out.println((ok ? "  ok   " : "  FAIL ") + label);
    return ok ? 0 : 1;
  }
}
