package com.owspfm.elwha.badge.playground;

import com.owspfm.elwha.badge.ElwhaBadge;
import com.owspfm.elwha.badge.ElwhaBadgeAnchor;
import com.owspfm.elwha.theme.ElwhaTheme;
import com.owspfm.elwha.theme.MaterialPalettes;
import com.owspfm.elwha.theme.Mode;
import java.awt.ComponentOrientation;
import java.awt.Container;
import java.awt.Dimension;
import java.awt.Point;
import java.awt.Rectangle;
import javax.swing.JLabel;
import javax.swing.JRootPane;
import javax.swing.SwingUtilities;

/**
 * Headless verification harness for {@link ElwhaBadgeAnchor#attachTrailingEdge} / {@link
 * ElwhaBadgeAnchor.AnchorMode#TRAILING_EDGE} ([#219]). Not an interactive playground — it mounts a
 * fixed-size host in an off-screen {@link JRootPane} hierarchy, attaches a Large badge at the
 * trailing edge, and asserts the resulting badge bounds; it exits non-zero on any mismatch, so it
 * doubles as a smoke gate.
 *
 * <p>Asserts, in both orientations, that the badge's trailing edge aligns to the host composition's
 * trailing edge (right in LTR, left in RTL) and that the badge is vertically centered on the host —
 * the M3 "Favorites 84" placement.
 *
 * @author Charles Bryan
 * @version v0.3.0
 * @since v0.3.0
 */
public final class TrailingEdgeBadgeDemo {

  private TrailingEdgeBadgeDemo() {}

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

    verify(ComponentOrientation.LEFT_TO_RIGHT);
    verify(ComponentOrientation.RIGHT_TO_LEFT);

    if (failures > 0) {
      System.err.println("FAIL: " + failures + " check(s) failed.");
      System.exit(1);
    }
    System.out.println("PASS: trailing-edge badge pins to the composition's trailing edge.");
  }

  private static void verify(final ComponentOrientation orientation) {
    final boolean ltr = orientation.isLeftToRight();
    final String tag = ltr ? "LTR" : "RTL";

    // A stand-in label+icon composition host; only its bounds matter for trailing-edge placement.
    final JLabel host = new JLabel("Favorites");
    host.setComponentOrientation(orientation);

    final JRootPane root = new JRootPane();
    root.setSize(400, 200);
    root.getLayeredPane().setBounds(0, 0, 400, 200);
    final Container content = root.getContentPane();
    content.setLayout(null);
    content.setBounds(0, 0, 400, 200);
    content.add(host);
    host.setBounds(40, 60, 160, 40);

    final ElwhaBadge badge = ElwhaBadge.large("84");
    ElwhaBadgeAnchor.attachTrailingEdge(host, badge);

    final Rectangle b = badge.getBounds();
    final Dimension pref = badge.getPreferredSize();
    final Point hostTL = SwingUtilities.convertPoint(host, new Point(0, 0), root.getLayeredPane());
    final int hostLeft = hostTL.x;
    final int hostRight = hostTL.x + host.getWidth();
    final int hostCenterY = hostTL.y + host.getHeight() / 2;
    final int badgeCenterY = b.y + b.height / 2;

    check(tag + ": badge has non-empty bounds", b.width > 0 && b.height > 0);
    if (ltr) {
      check(
          tag
              + ": badge trailing edge == host right ("
              + (b.x + b.width)
              + " vs "
              + hostRight
              + ")",
          b.x + b.width == hostRight);
    } else {
      check(
          tag + ": badge leading edge == host left (" + b.x + " vs " + hostLeft + ")",
          b.x == hostLeft);
    }
    check(
        tag + ": badge vertically centered (" + badgeCenterY + " vs " + hostCenterY + ")",
        Math.abs(badgeCenterY - hostCenterY) <= 1);
    System.out.println(
        "  " + tag + " → badge " + b + " (pref " + pref.width + "x" + pref.height + ")");
  }

  private static void check(final String label, final boolean ok) {
    System.out.println((ok ? "  ok   " : "  FAIL ") + label);
    if (!ok) {
      failures++;
    }
  }
}
