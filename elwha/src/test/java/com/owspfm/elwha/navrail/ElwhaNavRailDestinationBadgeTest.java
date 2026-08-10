package com.owspfm.elwha.navrail;

import static org.assertj.core.api.Assertions.assertThat;

import com.owspfm.elwha.badge.ElwhaBadge;
import com.owspfm.elwha.testkit.EdtInterceptor;
import com.owspfm.elwha.testkit.ThemeExtension;
import java.awt.Rectangle;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

/**
 * Tier A coverage of the #300 contract: a destination's badge is re-pinned when the rail changes
 * variant. Collapsed anchors on the icon's corner, because the icon is the whole visual anchor in a
 * 96 dp column; Expanded anchors at the row's trailing edge, because once the label sits beside the
 * icon the corner placement collides with the text.
 *
 * <p>The anchored mode is not exposed, so each case asserts the <em>placed geometry</em> instead —
 * which is the better test anyway: it is the badge's actual position that the contract is about.
 * Collapsed puts the badge up beside the icon; Expanded puts it out at the row's trailing end,
 * vertically centred. Those are far enough apart to tell apart without reading any private state.
 */
@ExtendWith({EdtInterceptor.class, ThemeExtension.class})
class ElwhaNavRailDestinationBadgeTest {

  private static final int RAIL_HEIGHT = 420;

  /** A rail mounted in a layered-pane hierarchy so anchored badges have somewhere to live. */
  private static ElwhaNavigationRail mountedRail(final ElwhaNavigationRail rail) {
    RailFixture.laidOut(rail, 4);
    RailFixture.mount(rail, rail.getPreferredSize().width, RAIL_HEIGHT);
    return rail;
  }

  /** The badge's bounds expressed relative to its host destination. */
  private static Rectangle relativeToHost(
      final ElwhaBadge badge, final ElwhaNavRailDestination host) {
    final java.awt.Point hostOnPane =
        javax.swing.SwingUtilities.convertPoint(host, 0, 0, badge.getParent());
    return new Rectangle(
        badge.getX() - hostOnPane.x,
        badge.getY() - hostOnPane.y,
        badge.getWidth(),
        badge.getHeight());
  }

  // --------------------------------------------------------------- Collapsed

  @Test
  void aCollapsedDestinationAnchorsItsBadgeOnTheIconCorner() {
    final ElwhaNavigationRail rail = mountedRail(ElwhaNavigationRail.collapsed());
    final ElwhaNavRailDestination host = rail.getPrimary().get(1);
    final ElwhaBadge badge = ElwhaBadge.small();

    host.setBadge(badge);

    final Rectangle placed = relativeToHost(badge, host);
    final Rectangle icon = host.getIconBounds();
    assertThat(placed.x)
        .as("#300 — the badge sits on the icon's trailing side, not out at the cell edge")
        .isBetween(icon.x, icon.x + icon.width);
    assertThat(placed.y + placed.height)
        .as("#300 — and near the icon's top edge, which is where the corner anchor pins it")
        .isBetween(icon.y - 2, icon.y + icon.height / 2);
  }

  @Test
  void aCollapsedBadgeStaysWellInsideTheColumn() {
    final ElwhaNavigationRail rail = mountedRail(ElwhaNavigationRail.collapsed());
    final ElwhaNavRailDestination host = rail.getPrimary().get(1);
    final ElwhaBadge badge = ElwhaBadge.large(9);

    host.setBadge(badge);

    final Rectangle placed = relativeToHost(badge, host);
    assertThat(placed.x)
        .as("#300 — a corner-anchored badge must not overhang the 96 dp column")
        .isNotNegative();
    assertThat(placed.x + placed.width).isLessThanOrEqualTo(host.getWidth());
  }

  // ---------------------------------------------------------------- Expanded

  @Test
  void anExpandedDestinationAnchorsItsBadgeAtTheRowsTrailingEdge() {
    final ElwhaNavigationRail rail = mountedRail(ElwhaNavigationRail.expanded());
    final ElwhaNavRailDestination host = rail.getPrimary().get(1);
    final ElwhaBadge badge = ElwhaBadge.large(9);

    host.setBadge(badge);

    final Rectangle placed = relativeToHost(badge, host);
    assertThat(placed.x + placed.width)
        .as("#300 — the Expanded row puts the count out at the far end, past the label")
        .isEqualTo(host.getWidth());
  }

  @Test
  void anExpandedBadgeIsVerticallyCentredOnItsRow() {
    final ElwhaNavigationRail rail = mountedRail(ElwhaNavigationRail.expanded());
    final ElwhaNavRailDestination host = rail.getPrimary().get(1);
    final ElwhaBadge badge = ElwhaBadge.large(9);

    host.setBadge(badge);

    final Rectangle placed = relativeToHost(badge, host);
    final int above = placed.y;
    final int below = host.getHeight() - (placed.y + placed.height);
    assertThat(Math.abs(above - below))
        .as("#300 — trailing-edge anchoring centres on the row, unlike the corner anchor")
        .isLessThanOrEqualTo(1);
  }

  // ------------------------------------------------------------- the re-pin

  @Test
  void changingVariantRePinsAnAlreadyAttachedBadge() {
    final ElwhaNavigationRail rail = mountedRail(ElwhaNavigationRail.collapsed());
    final ElwhaNavRailDestination host = rail.getPrimary().get(1);
    final ElwhaBadge badge = ElwhaBadge.large(9);
    host.setBadge(badge);
    final Rectangle collapsed = relativeToHost(badge, host);

    rail.setVariant(ElwhaNavigationRail.Variant.EXPANDED);
    RailFixture.relayout(rail);

    final Rectangle expanded = relativeToHost(badge, host);
    assertThat(expanded)
        .as("#300 — the badge is re-anchored on a variant change, not left on the stale mode")
        .isNotEqualTo(collapsed);
    assertThat(expanded.x + expanded.width)
        .as("and it lands at the Expanded row's trailing edge")
        .isEqualTo(host.getWidth());
  }

  @Test
  void changingBackRePinsToTheIconCorner() {
    final ElwhaNavigationRail rail = mountedRail(ElwhaNavigationRail.expanded());
    final ElwhaNavRailDestination host = rail.getPrimary().get(1);
    final ElwhaBadge badge = ElwhaBadge.large(9);
    host.setBadge(badge);

    rail.setVariant(ElwhaNavigationRail.Variant.COLLAPSED);
    RailFixture.relayout(rail);

    final Rectangle placed = relativeToHost(badge, host);
    final Rectangle icon = host.getIconBounds();
    assertThat(placed.x)
        .as("#300 — the re-pin runs both ways")
        .isBetween(icon.x, icon.x + icon.width);
  }

  @Test
  void aBadgeAttachedWhileExpandedStartsOnTheTrailingEdge() {
    final ElwhaNavigationRail rail = mountedRail(ElwhaNavigationRail.expanded());
    final ElwhaNavRailDestination host = rail.getPrimary().get(2);

    final ElwhaBadge badge = ElwhaBadge.large(3);
    host.setBadge(badge);

    assertThat(relativeToHost(badge, host).x + badge.getWidth())
        .as("#300 — the anchor mode is chosen from the current variant at attach time")
        .isEqualTo(host.getWidth());
  }

  @Test
  void aWideningBadgeStaysPinnedToTheTrailingEdge() {
    final ElwhaNavigationRail rail = mountedRail(ElwhaNavigationRail.expanded());
    final ElwhaNavRailDestination host = rail.getPrimary().get(1);
    final ElwhaBadge badge = ElwhaBadge.large(9);
    host.setBadge(badge);

    badge.setContent(999_999);

    final Rectangle placed = relativeToHost(badge, host);
    assertThat(placed.x + placed.width)
        .as("#300 — a relabelled badge re-places rather than drifting off the row's end")
        .isEqualTo(host.getWidth());
    assertThat(badge.getContent()).isEqualTo("999+");
  }

  // --------------------------------------------------------- attach lifecycle

  @Test
  void badgeIsReadBackFromItsHost() {
    final ElwhaNavigationRail rail = mountedRail(ElwhaNavigationRail.collapsed());
    final ElwhaNavRailDestination host = rail.getPrimary().get(0);
    final ElwhaBadge badge = ElwhaBadge.small();

    host.setBadge(badge);

    assertThat(host.getBadge()).isSameAs(badge);
    assertThat(badge.getParent()).as("and it is mounted on the layered pane").isNotNull();
  }

  @Test
  void clearingTheBadgeUnmountsIt() {
    final ElwhaNavigationRail rail = mountedRail(ElwhaNavigationRail.collapsed());
    final ElwhaNavRailDestination host = rail.getPrimary().get(0);
    final ElwhaBadge badge = ElwhaBadge.small();
    host.setBadge(badge);

    host.setBadge(null);

    assertThat(host.getBadge()).isNull();
    assertThat(badge.getParent()).isNull();
  }

  @Test
  void replacingABadgeUnmountsThePreviousOne() {
    final ElwhaNavigationRail rail = mountedRail(ElwhaNavigationRail.collapsed());
    final ElwhaNavRailDestination host = rail.getPrimary().get(0);
    final ElwhaBadge first = ElwhaBadge.small();
    final ElwhaBadge second = ElwhaBadge.large(2);
    host.setBadge(first);

    host.setBadge(second);

    assertThat(host.getBadge()).isSameAs(second);
    assertThat(first.getParent())
        .as("one badge per destination, per the anchor's own invariant")
        .isNull();
  }

  @Test
  void aVariantChangeOnABadgelessDestinationIsHarmless() {
    final ElwhaNavigationRail rail = mountedRail(ElwhaNavigationRail.collapsed());

    rail.setVariant(ElwhaNavigationRail.Variant.EXPANDED);

    assertThat(rail.getPrimary().get(0).getBadge())
        .as("the re-pin path must tolerate having nothing to re-pin")
        .isNull();
  }

  @Test
  void badgeSplicesItsAnnouncementOntoTheDestinationName() {
    final ElwhaNavigationRail rail = mountedRail(ElwhaNavigationRail.collapsed());
    final ElwhaNavRailDestination host = rail.getPrimary().get(1);

    host.setBadge(ElwhaBadge.large(3));

    assertThat(host.getAccessibleContext().getAccessibleName())
        .as("the push-model a11y wiring reaches nav-rail hosts too")
        .contains("3 new notifications");
  }
}
