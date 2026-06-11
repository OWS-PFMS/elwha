package com.owspfm.elwha.menu;

import com.owspfm.elwha.theme.ElwhaTheme;
import com.owspfm.elwha.theme.MaterialPalettes;
import com.owspfm.elwha.theme.Mode;
import java.awt.Graphics2D;
import java.awt.GraphicsEnvironment;
import java.awt.event.MouseEvent;
import java.awt.image.BufferedImage;
import java.util.Arrays;
import javax.swing.JFrame;
import javax.swing.JPanel;
import javax.swing.SwingUtilities;

/**
 * Windowed regression guard for the PR #395 smoke finding: a menu item that was hovered / pressed
 * when its surface tore down (or a {@code setVisibleItems} relayout removed it) repainted its stale
 * hover fill or a frozen mid-ripple frame on the next open — items are cached across opens and
 * {@code mouseExited} never fires for a removed component. Synthesizes enter+press on one item,
 * removes and re-adds it (the teardown analog that triggers {@code removeNotify}), then
 * pixel-compares its render against an identically-configured pristine twin. Exits non-zero on any
 * pixel diff. Needs a display.
 *
 * @author Charles Bryan
 * @version v0.4.0
 * @since v0.4.0
 */
public final class MenuStaleHoverDiag {

  private MenuStaleHoverDiag() {}

  private static int failures;

  /**
   * Runs the diagnostic.
   *
   * @param args ignored
   * @throws Exception on EDT failures
   */
  public static void main(final String[] args) throws Exception {
    if (GraphicsEnvironment.isHeadless()) {
      System.out.println("needs a display; skipping");
      return;
    }
    ElwhaTheme.install(
        ElwhaTheme.config().theme(MaterialPalettes.baseline()).mode(Mode.LIGHT).build());

    final JFrame[] frame = new JFrame[1];
    SwingUtilities.invokeAndWait(
        () -> {
          frame[0] = new JFrame("stale-hover-diag");
          final ElwhaMenuItem victim = ElwhaMenuItem.of("Clojure");
          final ElwhaMenuItem control = ElwhaMenuItem.of("Clojure");
          final JPanel panel = new JPanel();
          panel.add(victim);
          panel.add(control);
          frame[0].setContentPane(panel);
          frame[0].pack();
          frame[0].setVisible(true);

          // Hover + press the victim (hover fill on, ripple mid-flight), then remove + re-add it —
          // exactly what a menu teardown or a filter relayout does to a cached item under the
          // cursor. No mouseExited is ever delivered.
          dispatch(victim, MouseEvent.MOUSE_ENTERED);
          dispatch(victim, MouseEvent.MOUSE_PRESSED);
          panel.remove(victim);
          panel.add(victim, 0);
          panel.revalidate();

          final BufferedImage victimRender = render(victim);
          final BufferedImage controlRender = render(control);
          check(
              "re-added item renders identically to a pristine twin (no stale hover/ripple)",
              samePixels(victimRender, controlRender));
        });

    System.out.println(failures == 0 ? "PASS" : "FAIL — " + failures + " check(s)");
    SwingUtilities.invokeAndWait(frame[0]::dispose);
    System.exit(failures == 0 ? 0 : 1);
  }

  private static void dispatch(final ElwhaMenuItem item, final int id) {
    item.dispatchEvent(
        new MouseEvent(
            item, id, System.currentTimeMillis(), 0, 8, 8, 0, false, MouseEvent.BUTTON1));
  }

  private static BufferedImage render(final ElwhaMenuItem item) {
    final int w = Math.max(1, item.getPreferredSize().width);
    final int h = Math.max(1, item.getPreferredSize().height);
    item.setSize(w, h);
    final BufferedImage image = new BufferedImage(w, h, BufferedImage.TYPE_INT_ARGB);
    final Graphics2D g2 = image.createGraphics();
    try {
      item.paint(g2);
    } finally {
      g2.dispose();
    }
    return image;
  }

  private static boolean samePixels(final BufferedImage a, final BufferedImage b) {
    if (a.getWidth() != b.getWidth() || a.getHeight() != b.getHeight()) {
      return false;
    }
    for (int y = 0; y < a.getHeight(); y++) {
      final int[] rowA = a.getRGB(0, y, a.getWidth(), 1, null, 0, a.getWidth());
      final int[] rowB = b.getRGB(0, y, b.getWidth(), 1, null, 0, b.getWidth());
      if (!Arrays.equals(rowA, rowB)) {
        return false;
      }
    }
    return true;
  }

  private static void check(final String label, final boolean ok) {
    System.out.println((ok ? "  ok   " : "  FAIL ") + label);
    if (!ok) {
      failures++;
    }
  }
}
