package com.owspfm.elwha.fab.playground;

import com.owspfm.elwha.button.ElwhaButton;
import com.owspfm.elwha.fab.ElwhaFab;
import com.owspfm.elwha.iconbutton.ElwhaIconButton;
import com.owspfm.elwha.icons.MaterialIcons;
import com.owspfm.elwha.theme.ElwhaTheme;
import com.owspfm.elwha.theme.MaterialPalettes;
import com.owspfm.elwha.theme.Mode;
import java.awt.Dimension;
import java.awt.Graphics2D;
import java.awt.event.FocusEvent;
import java.awt.event.FocusListener;
import java.awt.image.BufferedImage;
import java.util.Arrays;
import javax.swing.JComponent;

/**
 * Headless verification harness for the focus-visible focus ring ([#260]). Not an interactive
 * playground — it renders {@link ElwhaFab}, {@link ElwhaButton}, and {@link ElwhaIconButton} in
 * three focus states and exits non-zero on any regression, so it doubles as a smoke gate.
 *
 * <p>For each component it asserts:
 *
 * <ul>
 *   <li>focus gained via a keyboard {@code TRAVERSAL_*} cause <strong>changes</strong> the render
 *       (the 2&nbsp;dp focus ring appears), and
 *   <li>focus gained via {@link FocusEvent.Cause#MOUSE_EVENT} is pixel-identical to the unfocused
 *       render (no ring on a pointer interaction).
 * </ul>
 *
 * The focus listeners are fired directly (rather than via real focus traversal) so the check is
 * deterministic and display-independent.
 *
 * @author Charles Bryan
 * @version v0.3.0
 * @since v0.3.0
 */
public final class FocusVisibleRingDemo {

  private FocusVisibleRingDemo() {}

  private static int failures;

  /**
   * Runs the verification and exits non-zero if any check fails.
   *
   * @param args ignored
   */
  public static void main(final String[] args) {
    System.setProperty("java.awt.headless", "true");
    ElwhaTheme.install(
        ElwhaTheme.config().theme(MaterialPalettes.baseline()).mode(Mode.LIGHT).build());

    check("ElwhaFab", ElwhaFab.standard(MaterialIcons.add(24)));
    check("ElwhaButton", new ElwhaButton("OK", MaterialIcons.add(24)));
    check("ElwhaIconButton", new ElwhaIconButton(MaterialIcons.add(24)));

    if (failures > 0) {
      System.err.println("FAIL: " + failures + " check(s) failed.");
      System.exit(1);
    }
    System.out.println("PASS: focus ring shows on keyboard focus only.");
  }

  private static void check(final String name, final JComponent comp) {
    final BufferedImage unfocused = render(comp);

    fireFocusGained(comp, FocusEvent.Cause.TRAVERSAL_FORWARD);
    final BufferedImage keyboard = render(comp);

    fireFocusLost(comp);
    fireFocusGained(comp, FocusEvent.Cause.MOUSE_EVENT);
    final BufferedImage mouse = render(comp);

    report(name + ": keyboard focus paints a ring", !equal(unfocused, keyboard));
    report(name + ": mouse focus paints no ring (identical to unfocused)", equal(unfocused, mouse));
  }

  private static void fireFocusGained(final JComponent comp, final FocusEvent.Cause cause) {
    final FocusEvent e = new FocusEvent(comp, FocusEvent.FOCUS_GAINED, false, null, cause);
    for (final FocusListener fl : comp.getFocusListeners()) {
      fl.focusGained(e);
    }
  }

  private static void fireFocusLost(final JComponent comp) {
    final FocusEvent e =
        new FocusEvent(comp, FocusEvent.FOCUS_LOST, false, null, FocusEvent.Cause.UNKNOWN);
    for (final FocusListener fl : comp.getFocusListeners()) {
      fl.focusLost(e);
    }
  }

  private static BufferedImage render(final JComponent c) {
    final Dimension d = c.getPreferredSize();
    c.setSize(d);
    c.doLayout();
    final BufferedImage img =
        new BufferedImage(Math.max(1, d.width), Math.max(1, d.height), BufferedImage.TYPE_INT_ARGB);
    final Graphics2D g = img.createGraphics();
    try {
      c.printAll(g);
    } finally {
      g.dispose();
    }
    return img;
  }

  private static boolean equal(final BufferedImage a, final BufferedImage b) {
    if (a.getWidth() != b.getWidth() || a.getHeight() != b.getHeight()) {
      return false;
    }
    final int[] pa = a.getRGB(0, 0, a.getWidth(), a.getHeight(), null, 0, a.getWidth());
    final int[] pb = b.getRGB(0, 0, b.getWidth(), b.getHeight(), null, 0, b.getWidth());
    return Arrays.equals(pa, pb);
  }

  private static void report(final String label, final boolean ok) {
    System.out.println((ok ? "  ok   " : "  FAIL ") + label);
    if (!ok) {
      failures++;
    }
  }
}
