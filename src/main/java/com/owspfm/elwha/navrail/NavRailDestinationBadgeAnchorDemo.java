package com.owspfm.elwha.navrail;

import com.owspfm.elwha.badge.ElwhaBadge;
import com.owspfm.elwha.icons.MaterialIcons;
import com.owspfm.elwha.theme.ElwhaTheme;
import com.owspfm.elwha.theme.MaterialPalettes;
import com.owspfm.elwha.theme.Mode;
import java.awt.ComponentOrientation;
import java.awt.Container;
import java.awt.Point;
import java.awt.Rectangle;
import javax.swing.JRootPane;
import javax.swing.SwingUtilities;

/**
 * Headless verification harness for {@link ElwhaNavRailDestination}'s variant-dependent badge
 * anchoring (story #300). Not an interactive playground — it mounts a destination in an off-screen
 * {@link JRootPane} hierarchy, attaches a Large badge while Collapsed, then toggles the host
 * variant to Expanded, asserting the badge bounds after each transition; it exits non-zero on any
 * mismatch, so it doubles as a smoke gate.
 *
 * <p>Drives the destination's variant directly via the package-private {@code
 * setHostVariant(Variant)} (the same call the rail makes) and asserts, in both orientations:
 *
 * <ul>
 *   <li><strong>Collapsed</strong> → the badge's bottom-leading corner pins to the icon's
 *       top-trailing corner offset inward by the Large badge offsets ({@link
 *       ElwhaBadgeAnchor.AnchorMode#ICON_CORNER}).
 *   <li><strong>Expanded</strong> (after a variant toggle on the <em>same</em> destination, with
 *       the badge already attached) → the badge re-pins to the row's trailing edge, vertically
 *       centered ({@link ElwhaBadgeAnchor.AnchorMode#TRAILING_EDGE}) — proving the re-anchor fires
 *       on the variant change, not only at {@code setBadge()} time.
 * </ul>
 *
 * @author Charles Bryan
 * @version v0.4.0
 * @since v0.4.0
 */
public final class NavRailDestinationBadgeAnchorDemo {

  // Large-badge offsets (icon top-trailing → badge bottom-leading), mirrored from the package-
  // private constants in ElwhaBadgeAnchor (design doc §5.1). Kept in sync by the assertions below.
  private static final int LARGE_OFFSET_X = 12;
  private static final int LARGE_OFFSET_Y = 14;

  private static int failures;

  private NavRailDestinationBadgeAnchorDemo() {}

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
    System.out.println(
        "PASS: nav-rail badge pins to the icon corner (Collapsed) and the row trailing edge"
            + " (Expanded), re-anchoring on the variant toggle.");
  }

  private static void verify(final ComponentOrientation orientation) {
    final boolean ltr = orientation.isLeftToRight();
    final String dir = ltr ? "LTR" : "RTL";

    final ElwhaNavRailDestination dest =
        ElwhaNavRailDestination.of(MaterialIcons.symbol("favorite"), "Favorites");
    dest.setComponentOrientation(orientation);
    dest.setSelected(true);

    // --- Collapsed: badge attaches at the icon corner. ---
    dest.setHostVariant(ElwhaNavigationRail.Variant.COLLAPSED);
    final JRootPane root = mount(dest, orientation);
    dest.setBounds(
        60,
        60,
        ElwhaNavRailDestination.COLLAPSED_WIDTH_PX,
        ElwhaNavRailDestination.COLLAPSED_CONTENT_HEIGHT_PX);

    final ElwhaBadge badge = ElwhaBadge.large(84);
    dest.setBadge(badge);
    assertIconCorner(dir, dest, badge, root.getLayeredPane(), ltr);

    // --- Toggle to Expanded: the already-attached badge must re-pin to the trailing edge. ---
    dest.setHostVariant(ElwhaNavigationRail.Variant.EXPANDED);
    final int expandedWidth = dest.getPreferredSize().width;
    dest.setBounds(60, 60, expandedWidth, ElwhaNavRailDestination.EXPANDED_CONTENT_HEIGHT_PX);
    assertTrailingEdge(dir, dest, badge, root.getLayeredPane(), ltr);
  }

  private static JRootPane mount(
      final ElwhaNavRailDestination dest, final ComponentOrientation orientation) {
    final JRootPane root = new JRootPane();
    root.setComponentOrientation(orientation);
    root.setSize(480, 240);
    root.getLayeredPane().setBounds(0, 0, 480, 240);
    final Container content = root.getContentPane();
    content.setComponentOrientation(orientation);
    content.setLayout(null);
    content.setBounds(0, 0, 480, 240);
    content.add(dest);
    return root;
  }

  private static void assertIconCorner(
      final String dir,
      final ElwhaNavRailDestination dest,
      final ElwhaBadge badge,
      final java.awt.Component layered,
      final boolean ltr) {
    final Rectangle b = badge.getBounds();
    final Rectangle icon = dest.getIconBounds();
    final Point iconTopLeft = SwingUtilities.convertPoint(dest, new Point(icon.x, icon.y), layered);
    final int iconTrailingX = ltr ? iconTopLeft.x + icon.width : iconTopLeft.x;
    // Pinned corner: bottom-leading of the badge sits offsetX inward from the icon top-trailing X
    // and offsetY down from the icon top.
    final int expectedPinX = ltr ? iconTrailingX - LARGE_OFFSET_X : iconTrailingX + LARGE_OFFSET_X;
    final int expectedBottom = iconTopLeft.y + LARGE_OFFSET_Y;
    final int actualPinX = ltr ? b.x : b.x + b.width;

    check(dir + " collapsed: badge has non-empty bounds", b.width > 0 && b.height > 0);
    check(
        dir
            + " collapsed: badge pins to icon trailing-X ("
            + actualPinX
            + " vs "
            + expectedPinX
            + ")",
        actualPinX == expectedPinX);
    check(
        dir
            + " collapsed: badge bottom == icon-top + offset ("
            + (b.y + b.height)
            + " vs "
            + expectedBottom
            + ")",
        b.y + b.height == expectedBottom);
    System.out.println("  " + dir + " collapsed → badge " + b + " (icon " + icon + ")");
  }

  private static void assertTrailingEdge(
      final String dir,
      final ElwhaNavRailDestination dest,
      final ElwhaBadge badge,
      final java.awt.Component layered,
      final boolean ltr) {
    final Rectangle b = badge.getBounds();
    final Point hostTopLeft = SwingUtilities.convertPoint(dest, new Point(0, 0), layered);
    final int hostLeft = hostTopLeft.x;
    final int hostRight = hostTopLeft.x + dest.getWidth();
    final int hostCenterY = hostTopLeft.y + dest.getHeight() / 2;
    final int badgeCenterY = b.y + b.height / 2;

    check(dir + " expanded: badge has non-empty bounds", b.width > 0 && b.height > 0);
    if (ltr) {
      check(
          dir
              + " expanded: badge trailing edge == host right ("
              + (b.x + b.width)
              + " vs "
              + hostRight
              + ")",
          b.x + b.width == hostRight);
    } else {
      check(
          dir + " expanded: badge leading edge == host left (" + b.x + " vs " + hostLeft + ")",
          b.x == hostLeft);
    }
    check(
        dir + " expanded: badge vertically centered (" + badgeCenterY + " vs " + hostCenterY + ")",
        Math.abs(badgeCenterY - hostCenterY) <= 1);
    System.out.println(
        "  " + dir + " expanded  → badge " + b + " (host w=" + dest.getWidth() + ")");
  }

  private static void check(final String label, final boolean ok) {
    System.out.println((ok ? "  ok   " : "  FAIL ") + label);
    if (!ok) {
      failures++;
    }
  }
}
