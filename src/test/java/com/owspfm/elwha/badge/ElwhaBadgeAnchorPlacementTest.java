package com.owspfm.elwha.badge;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;

import com.owspfm.elwha.badge.AnchorFixture.IconHost;
import com.owspfm.elwha.testkit.EdtInterceptor;
import com.owspfm.elwha.testkit.PaintLog;
import com.owspfm.elwha.testkit.ThemeExtension;
import com.owspfm.elwha.theme.ElwhaLayers;
import java.awt.Dimension;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.geom.Rectangle2D;
import javax.swing.JComponent;
import javax.swing.JLayeredPane;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

/**
 * Tier A coverage of {@link ElwhaBadgeAnchor} placement — design doc §5 (the offset table and the
 * pin behavior), §9.2 (attachment lifecycle) and §11 (RTL mirroring), driven through a
 * hand-laid-out root-pane hierarchy ({@link AnchorFixture}) so the coordinate conversions the
 * anchor performs are genuinely exercised rather than collapsing to the identity.
 *
 * <p>Placement is asserted as arithmetic against the documented offsets, not against captured
 * numbers: every expectation below re-derives the pinned corner the way §5.1 states it, so a change
 * to the offsets fails here with the spec sentence in the message.
 *
 * <p>The bounds-versus-painted-body distinction (#493) shows up twice and is asserted both times:
 * the badge's own painted container fills its bounds exactly, so anchoring against {@code
 * getPreferredSize()} is honest; and on the host side {@link
 * ElwhaBadgeAnchor.AnchorMode#TRAILING_EDGE} pins to the host's <em>bounds</em> while {@link
 * ElwhaBadgeAnchor.AnchorMode#LABEL_TRAILING} pins to the content rect the host feeds it — which is
 * the whole reason both modes exist.
 */
@ExtendWith({EdtInterceptor.class, ThemeExtension.class})
class ElwhaBadgeAnchorPlacementTest {

  private static final int SMALL_OFFSET_X = 6;
  private static final int SMALL_OFFSET_Y = 6;
  private static final int LARGE_OFFSET_X = 12;
  private static final int LARGE_OFFSET_Y = 14;
  private static final int LABEL_TRAILING_GAP = 4;

  private static final Rectangle ICON = new Rectangle(12, 8, 24, 24);

  private static IconHost host() {
    return new IconHost(new Rectangle(ICON));
  }

  /** The icon's top-trailing corner in layered-pane coordinates, LTR. */
  private static Point iconTopRightInLayeredPane(final JComponent host, final Rectangle icon) {
    final Point origin = AnchorFixture.hostOriginInLayeredPane(host);
    return new Point(origin.x + icon.x + icon.width, origin.y + icon.y);
  }

  /** The icon's top-leading corner in layered-pane coordinates — the RTL mirror. */
  private static Point iconTopLeftInLayeredPane(final JComponent host, final Rectangle icon) {
    final Point origin = AnchorFixture.hostOriginInLayeredPane(host);
    return new Point(origin.x + icon.x, origin.y + icon.y);
  }

  // ------------------------------------------------------- ICON_CORNER, LTR

  @Test
  void aSmallBadgePinsItsBottomLeadingCornerSixDpInsideTheIconCorner() {
    final IconHost host = host();
    AnchorFixture.with(host, 20, 16, 48, 48);
    final ElwhaBadge badge = ElwhaBadge.small();

    ElwhaBadgeAnchor.attach(host, badge);

    final Point corner = iconTopRightInLayeredPane(host, ICON);
    final Dimension pref = badge.getPreferredSize();
    assertThat(badge.getX())
        .as("§5.1 — the pin sits 6 dp inward from the icon's trailing edge")
        .isEqualTo(corner.x - SMALL_OFFSET_X);
    assertThat(badge.getY())
        .as("§5.1 — and 6 dp below the icon's top edge, measured to the badge's bottom")
        .isEqualTo(corner.y + SMALL_OFFSET_Y - pref.height);
  }

  @Test
  void aLargeBadgeUsesTheLargerOffsetPair() {
    final IconHost host = host();
    AnchorFixture.with(host, 20, 16, 48, 48);
    final ElwhaBadge badge = ElwhaBadge.large("42");

    ElwhaBadgeAnchor.attach(host, badge);

    final Point corner = iconTopRightInLayeredPane(host, ICON);
    assertThat(badge.getX())
        .as("§5.1 — Large is pinned 12 dp inward, not 6")
        .isEqualTo(corner.x - LARGE_OFFSET_X);
    assertThat(badge.getY())
        .as("§5.1 — and 14 dp down, so the taller pill still overlaps the icon corner")
        .isEqualTo(corner.y + LARGE_OFFSET_Y - badge.getPreferredSize().height);
  }

  @Test
  void badgeIsSizedToItsPreferredSize() {
    final IconHost host = host();
    AnchorFixture.with(host, 20, 16, 48, 48);
    final ElwhaBadge badge = ElwhaBadge.large("999+");

    ElwhaBadgeAnchor.attach(host, badge);

    assertThat(badge.getSize())
        .as("the anchor places the badge at its natural size — it never stretches it")
        .isEqualTo(badge.getPreferredSize());
  }

  @Test
  void anchoredBoundsAreExactlyWhatTheBadgePaintsInto() {
    final IconHost host = host();
    AnchorFixture.with(host, 20, 16, 48, 48);
    final ElwhaBadge badge = ElwhaBadge.large("999+");

    ElwhaBadgeAnchor.attach(host, badge);

    final PaintLog log = PaintLog.capture(badge, badge.getWidth(), badge.getHeight());
    final Rectangle2D container =
        log.shapes().stream().filter(p -> !p.stroked()).findFirst().orElseThrow().bounds();
    assertThat(
            new Rectangle(
                (int) container.getX(),
                (int) container.getY(),
                (int) container.getWidth(),
                (int) container.getHeight()))
        .as("#493 — the badge's painted body fills its bounds, so pinning bounds pins the pixels")
        .isEqualTo(new Rectangle(0, 0, badge.getWidth(), badge.getHeight()));
  }

  @Test
  void aWideningLargeBadgeHoldsItsPinnedCornerAndExtendsTrailingWard() {
    final IconHost host = host();
    AnchorFixture.with(host, 20, 16, 48, 48);
    final ElwhaBadge badge = ElwhaBadge.large("1");
    ElwhaBadgeAnchor.attach(host, badge);
    final int pinnedLeadingEdge = badge.getX();
    final int pinnedTop = badge.getY();
    final int narrowTrailingEdge = badge.getX() + badge.getWidth();

    badge.setContent("999+");

    assertThat(badge.getX())
        .as("§5.2 — the pinned bottom-leading corner is what stays put as content grows")
        .isEqualTo(pinnedLeadingEdge);
    assertThat(badge.getX() + badge.getWidth())
        .as(
            "§5.2 — the trailing edge moves outward, extending past the icon rather than fanning "
                + "back across it")
        .isGreaterThan(narrowTrailingEdge);
    assertThat(badge.getY())
        .as("the pill height is fixed, so the vertical pin is untouched")
        .isEqualTo(pinnedTop);
  }

  @Test
  void aWideningLargeBadgeExtendsLeftwardInRightToLeft() {
    final IconHost host = host();
    final AnchorFixture fixture = AnchorFixture.with(host, 20, 16, 48, 48);
    final ElwhaBadge badge = ElwhaBadge.large("1");
    ElwhaBadgeAnchor.attach(host, badge);
    fixture.applyRightToLeft();
    final int pinnedLeadingEdge = badge.getX() + badge.getWidth();
    final int narrowTrailingEdge = badge.getX();

    badge.setContent("999+");

    assertThat(badge.getX() + badge.getWidth())
        .as("§5.2 — in RTL the pinned leading corner is the badge's right edge")
        .isEqualTo(pinnedLeadingEdge);
    assertThat(badge.getX())
        .as("§5.2 — so the trailing edge moves leftward as the pill widens")
        .isLessThan(narrowTrailingEdge);
  }

  @Test
  void aContentChangeRePlacesTheBadgeWithoutReAttaching() {
    final IconHost host = host();
    AnchorFixture.with(host, 20, 16, 48, 48);
    final ElwhaBadge badge = ElwhaBadge.large("1");
    ElwhaBadgeAnchor.attach(host, badge);
    final int before = badge.getWidth();

    badge.setContent("999+");

    assertThat(badge.getWidth())
        .as("the anchor listens on PROPERTY_CONTENT so a relabel re-measures in place")
        .isGreaterThan(before);
  }

  @Test
  void movingTheHostMovesTheBadgeWithIt() {
    final IconHost host = host();
    AnchorFixture.with(host, 20, 16, 48, 48);
    final ElwhaBadge badge = ElwhaBadge.small();
    ElwhaBadgeAnchor.attach(host, badge);
    final Point before = badge.getLocation();

    AnchorFixture.moveTo(host, 60, 90);

    assertThat(badge.getLocation())
        .as("the badge tracks its host — the component listener re-places on move")
        .isEqualTo(new Point(before.x + 40, before.y + 74));
  }

  @Test
  void resizingTheHostRePlacesAgainstTheNewGeometry() {
    final JComponent host = new JComponent() {};
    AnchorFixture.with(host, 20, 16, 160, 40);
    final ElwhaBadge badge = ElwhaBadge.large("84");
    ElwhaBadgeAnchor.attachTrailingEdge(host, badge);

    AnchorFixture.resizeTo(host, 200, 40);

    assertThat(badge.getX() + badge.getWidth())
        .as("a re-laid-out host drags its trailing-edge badge along to the new edge")
        .isEqualTo(AnchorFixture.hostOriginInLayeredPane(host).x + 200);
  }

  @Test
  void aHostWithNoIconYetParksTheBadgeRatherThanFloatingItInSpace() {
    final IconHost host = new IconHost(new Rectangle());
    AnchorFixture.with(host, 20, 16, 48, 48);
    final ElwhaBadge badge = ElwhaBadge.small();

    ElwhaBadgeAnchor.attach(host, badge);

    assertThat(badge.getBounds())
        .as("an empty icon box means there is nothing to pin to, so no placement is applied")
        .isEqualTo(new Rectangle(0, 0, 0, 0));
    assertThat(badge.isVisible())
        .as("and the badge is hidden rather than left drawing at the host's origin")
        .isFalse();
  }

  @Test
  void anIconArrivingLaterPlacesTheBadgeOnTheNextRefresh() {
    final IconHost host = new IconHost(new Rectangle());
    AnchorFixture.with(host, 20, 16, 48, 48);
    final ElwhaBadge badge = ElwhaBadge.small();
    final ElwhaBadgeAnchor.Attachment attachment = ElwhaBadgeAnchor.attach(host, badge);

    host.setIconBounds(new Rectangle(ICON));
    attachment.refresh();

    assertThat(badge.getX())
        .as("the empty-bounds guard is a deferral, not a permanent opt-out")
        .isEqualTo(iconTopRightInLayeredPane(host, ICON).x - SMALL_OFFSET_X);
  }

  // ------------------------------------------------------- ICON_CORNER, RTL

  @Test
  void rightToLeftMirrorsTheBadgeToTheIconsLeadingCorner() {
    final IconHost host = host();
    final AnchorFixture fixture = AnchorFixture.with(host, 20, 16, 48, 48);
    final ElwhaBadge badge = ElwhaBadge.small();
    ElwhaBadgeAnchor.attach(host, badge);

    fixture.applyRightToLeft();

    final Point corner = iconTopLeftInLayeredPane(host, ICON);
    assertThat(badge.getX() + badge.getWidth())
        .as(
            "§11 — in RTL the pinned corner is the badge's bottom-right, 6 dp inside the icon's"
                + " left")
        .isEqualTo(corner.x + SMALL_OFFSET_X);
    assertThat(badge.getY())
        .as("§11 — mirroring is horizontal only; the vertical pin is unchanged")
        .isEqualTo(corner.y + SMALL_OFFSET_Y - badge.getPreferredSize().height);
  }

  @Test
  void twoOrientationsPlaceTheBadgeOnOppositeSidesOfTheIcon() {
    final IconHost ltrHost = host();
    AnchorFixture.with(ltrHost, 20, 16, 48, 48);
    final ElwhaBadge ltrBadge = ElwhaBadge.small();
    ElwhaBadgeAnchor.attach(ltrHost, ltrBadge);

    final IconHost rtlHost = host();
    final AnchorFixture rtl = AnchorFixture.with(rtlHost, 20, 16, 48, 48);
    final ElwhaBadge rtlBadge = ElwhaBadge.small();
    ElwhaBadgeAnchor.attach(rtlHost, rtlBadge);
    rtl.applyRightToLeft();

    final int iconCenterX =
        AnchorFixture.hostOriginInLayeredPane(ltrHost).x + ICON.x + ICON.width / 2;
    assertThat(ltrBadge.getX())
        .as("§11 — LTR puts the badge on the icon's right")
        .isGreaterThan(iconCenterX);
    assertThat(rtlBadge.getX())
        .as("§11 — RTL puts it on the left, not merely at a different offset")
        .isLessThan(iconCenterX);
  }

  @Test
  void aLaterOrientationFlipReplacesTheBadgeWithoutReAttaching() {
    final IconHost host = host();
    final AnchorFixture fixture = AnchorFixture.with(host, 20, 16, 48, 48);
    final ElwhaBadge badge = ElwhaBadge.large("9");
    ElwhaBadgeAnchor.attach(host, badge);
    final int before = badge.getX();

    fixture.applyRightToLeft();

    assertThat(badge.getX())
        .as("the anchor listens for componentOrientation, so a locale switch re-places live")
        .isNotEqualTo(before);
  }

  // ------------------------------------------------------------ TRAILING_EDGE

  @Test
  void trailingEdgeFlushesTheBadgeAgainstTheHostsTrailingEdge() {
    final JComponent host = new JComponent() {};
    AnchorFixture.with(host, 20, 16, 160, 40);
    final ElwhaBadge badge = ElwhaBadge.large("84");

    ElwhaBadgeAnchor.attachTrailingEdge(host, badge);

    final Point origin = AnchorFixture.hostOriginInLayeredPane(host);
    assertThat(badge.getX() + badge.getWidth())
        .as("the M3 'Favorites 84' row puts the badge on the composition's trailing edge")
        .isEqualTo(origin.x + host.getWidth());
  }

  @Test
  void trailingEdgeCentersTheBadgeVerticallyOnTheHost() {
    final JComponent host = new JComponent() {};
    AnchorFixture.with(host, 20, 16, 160, 40);
    final ElwhaBadge badge = ElwhaBadge.large("84");

    ElwhaBadgeAnchor.attachTrailingEdge(host, badge);

    final Point origin = AnchorFixture.hostOriginInLayeredPane(host);
    assertThat(badge.getY())
        .as("centered on the host's vertical center — the robust stand-in for the label baseline")
        .isEqualTo(origin.y + (host.getHeight() - badge.getPreferredSize().height) / 2);
  }

  @Test
  void trailingEdgeMirrorsToTheLeadingEdgeInRightToLeft() {
    final JComponent host = new JComponent() {};
    final AnchorFixture fixture = AnchorFixture.with(host, 20, 16, 160, 40);
    final ElwhaBadge badge = ElwhaBadge.large("84");
    ElwhaBadgeAnchor.attachTrailingEdge(host, badge);

    fixture.applyRightToLeft();

    assertThat(badge.getX())
        .as("§11 — trailing means left once the orientation flips")
        .isEqualTo(AnchorFixture.hostOriginInLayeredPane(host).x);
  }

  @Test
  void trailingEdgeNeedsNoIconBearingHost() {
    final JComponent plain = new JComponent() {};
    AnchorFixture.with(plain, 20, 16, 160, 40);

    ElwhaBadgeAnchor.attachTrailingEdge(plain, ElwhaBadge.large("84"));

    assertThat(plain).as("the mode consults host bounds only").isNotInstanceOf(IconBearing.class);
  }

  @Test
  void aZeroSizedHostGetsNoTrailingEdgePlacement() {
    final JComponent host = new JComponent() {};
    AnchorFixture.with(host, 20, 16, 0, 0);
    final ElwhaBadge badge = ElwhaBadge.large("84");

    ElwhaBadgeAnchor.attachTrailingEdge(host, badge);

    assertThat(badge.getBounds())
        .as("a host with no extent has no trailing edge to pin against")
        .isEqualTo(new Rectangle(0, 0, 0, 0));
  }

  // ----------------------------------------------------------- LABEL_TRAILING

  @Test
  void labelTrailingFlowsTheBadgeAfterTheContentRectWithAGap() {
    final Rectangle label = new Rectangle(40, 12, 60, 16);
    final IconHost host = new IconHost(label);
    AnchorFixture.with(host, 20, 16, 160, 40);
    final ElwhaBadge badge = ElwhaBadge.large("999+");

    ElwhaBadgeAnchor.attach(host, badge, ElwhaBadgeAnchor.AnchorMode.LABEL_TRAILING);

    final Point origin = AnchorFixture.hostOriginInLayeredPane(host);
    assertThat(badge.getX())
        .as("the M3 'Label 999+' row leaves a 4 dp gap after the text")
        .isEqualTo(origin.x + label.x + label.width + LABEL_TRAILING_GAP);
  }

  @Test
  void labelTrailingCentersTheBadgeOnTheContentRect() {
    final Rectangle label = new Rectangle(40, 12, 60, 16);
    final IconHost host = new IconHost(label);
    AnchorFixture.with(host, 20, 16, 160, 40);
    final ElwhaBadge badge = ElwhaBadge.large("999+");

    ElwhaBadgeAnchor.attach(host, badge, ElwhaBadgeAnchor.AnchorMode.LABEL_TRAILING);

    final Point origin = AnchorFixture.hostOriginInLayeredPane(host);
    assertThat(badge.getY())
        .as("centered on the rect it trails, not on the host cell")
        .isEqualTo(origin.y + label.y + (label.height - badge.getPreferredSize().height) / 2);
  }

  @Test
  void labelTrailingMirrorsToBeforeTheRectInRightToLeft() {
    final Rectangle label = new Rectangle(40, 12, 60, 16);
    final IconHost host = new IconHost(label);
    final AnchorFixture fixture = AnchorFixture.with(host, 20, 16, 160, 40);
    final ElwhaBadge badge = ElwhaBadge.large("999+");
    ElwhaBadgeAnchor.attach(host, badge, ElwhaBadgeAnchor.AnchorMode.LABEL_TRAILING);

    fixture.applyRightToLeft();

    final Point origin = AnchorFixture.hostOriginInLayeredPane(host);
    assertThat(badge.getX() + badge.getWidth())
        .as("§11 — the badge leads the text once the orientation flips")
        .isEqualTo(origin.x + label.x - LABEL_TRAILING_GAP);
  }

  @Test
  void labelTrailingTracksTheContentWhileTrailingEdgeTracksTheCell() {
    // The same host twice: a 60 dp content rect centered inside a 200 dp cell. This is the
    // #493 distinction in miniature — one mode reads the painted content's rect, the other reads
    // the component's bounds, and on a centered composition they are nowhere near each other.
    final Rectangle content = new Rectangle(70, 12, 60, 16);

    final IconHost labelHost = new IconHost(content);
    AnchorFixture.with(labelHost, 20, 16, 200, 40);
    final ElwhaBadge labelBadge = ElwhaBadge.large("999+");
    ElwhaBadgeAnchor.attach(labelHost, labelBadge, ElwhaBadgeAnchor.AnchorMode.LABEL_TRAILING);

    final IconHost edgeHost = new IconHost(content);
    AnchorFixture.with(edgeHost, 20, 16, 200, 40);
    final ElwhaBadge edgeBadge = ElwhaBadge.large("999+");
    ElwhaBadgeAnchor.attach(edgeHost, edgeBadge, ElwhaBadgeAnchor.AnchorMode.TRAILING_EDGE);

    final Point origin = AnchorFixture.hostOriginInLayeredPane(labelHost);
    assertThat(labelBadge.getX())
        .as("LABEL_TRAILING follows the text the host feeds it")
        .isEqualTo(origin.x + content.x + content.width + LABEL_TRAILING_GAP);
    assertThat(edgeBadge.getX() + edgeBadge.getWidth())
        .as("TRAILING_EDGE follows the cell's own bounds instead")
        .isEqualTo(origin.x + 200);
    assertThat(edgeBadge.getX())
        .as("so on a centered composition the two modes land far apart, by design")
        .isGreaterThan(labelBadge.getX());
  }

  @Test
  void labelTrailingWithAnEmptyRectPlacesNothing() {
    final IconHost host = new IconHost(new Rectangle());
    AnchorFixture.with(host, 20, 16, 160, 40);
    final ElwhaBadge badge = ElwhaBadge.large("9");

    ElwhaBadgeAnchor.attach(host, badge, ElwhaBadgeAnchor.AnchorMode.LABEL_TRAILING);

    assertThat(badge.getBounds())
        .as("no content rect, nothing to trail")
        .isEqualTo(new Rectangle(0, 0, 0, 0));
  }

  // ------------------------------------------------------- mounting lifecycle

  @Test
  void badgeIsMountedOnTheHostsLayeredPaneAtTheOverlayLayer() {
    final IconHost host = host();
    final AnchorFixture fixture = AnchorFixture.with(host, 20, 16, 48, 48);
    final ElwhaBadge badge = ElwhaBadge.small();

    ElwhaBadgeAnchor.attach(host, badge);

    final JLayeredPane pane = fixture.layeredPane();
    assertThat(badge.getParent()).as("the badge floats above ordinary content").isSameAs(pane);
    assertThat(pane.getLayer(badge))
        .as("#221 — the Elwha overlay band, not the shared PALETTE_LAYER")
        .isEqualTo(ElwhaLayers.OVERLAY_LAYER);
  }

  @Test
  void anExplicitLayerOverrideIsHonored() {
    final IconHost host = host();
    final AnchorFixture fixture = AnchorFixture.with(host, 20, 16, 48, 48);
    final ElwhaBadge badge = ElwhaBadge.small();

    ElwhaBadgeAnchor.attach(
        host, badge, ElwhaBadgeAnchor.AnchorMode.ICON_CORNER, JLayeredPane.POPUP_LAYER);

    assertThat(fixture.layeredPane().getLayer(badge))
        .as("the escape hatch for consumers with an exotic z-stack")
        .isEqualTo(JLayeredPane.POPUP_LAYER.intValue());
  }

  @Test
  void attachingBeforeTheHostJoinsAHierarchyDefersTheMount() {
    final IconHost host = host();
    final ElwhaBadge badge = ElwhaBadge.small();
    final AnchorFixture fixture = AnchorFixture.create();

    ElwhaBadgeAnchor.attach(host, badge);
    assertThat(badge.getParent()).as("nothing to mount onto yet").isNull();

    fixture.mount(host, 20, 16, 48, 48);

    assertThat(badge.getParent())
        .as("§9.2 — late attach is supported; the mount waits for the hierarchy")
        .isSameAs(fixture.layeredPane());
    assertThat(badge.getX())
        .as("and placement runs as soon as it lands")
        .isEqualTo(iconTopRightInLayeredPane(host, ICON).x - SMALL_OFFSET_X);
  }

  @Test
  void aHostLeavingTheHierarchyUnmountsTheBadge() {
    final IconHost host = host();
    final AnchorFixture fixture = AnchorFixture.with(host, 20, 16, 48, 48);
    final ElwhaBadge badge = ElwhaBadge.small();
    ElwhaBadgeAnchor.attach(host, badge);

    fixture.unmount(host);

    assertThat(badge.getParent())
        .as("a badge must not outlive its host on the shared layered pane")
        .isNull();
  }

  @Test
  void reAttachingReplacesThePriorBadgeOnTheSameHost() {
    final IconHost host = host();
    final AnchorFixture fixture = AnchorFixture.with(host, 20, 16, 48, 48);
    final ElwhaBadge first = ElwhaBadge.small();
    final ElwhaBadge second = ElwhaBadge.large("3");
    ElwhaBadgeAnchor.attach(host, first);

    ElwhaBadgeAnchor.attach(host, second);

    assertThat(first.getParent())
        .as("§9.3 — one badge per host; the prior attachment is detached first")
        .isNull();
    assertThat(second.getParent()).isSameAs(fixture.layeredPane());
  }

  @Test
  void detachRemovesTheBadgeFromTheLayeredPane() {
    final IconHost host = host();
    AnchorFixture.with(host, 20, 16, 48, 48);
    final ElwhaBadge badge = ElwhaBadge.small();
    final ElwhaBadgeAnchor.Attachment attachment = ElwhaBadgeAnchor.attach(host, badge);

    ElwhaBadgeAnchor.detach(attachment);

    assertThat(badge.getParent()).as("detach restores the host to its pre-attach state").isNull();
  }

  @Test
  void detachIsIdempotent() {
    final IconHost host = host();
    AnchorFixture.with(host, 20, 16, 48, 48);
    final ElwhaBadgeAnchor.Attachment attachment =
        ElwhaBadgeAnchor.attach(host, ElwhaBadge.small());
    ElwhaBadgeAnchor.detach(attachment);

    ElwhaBadgeAnchor.detach(attachment);

    assertThat(attachment).as("§9.2 — a second detach is a no-op, not an error").isNotNull();
  }

  @Test
  void aDetachedAttachmentStopsTrackingTheHost() {
    final IconHost host = host();
    AnchorFixture.with(host, 20, 16, 48, 48);
    final ElwhaBadge badge = ElwhaBadge.small();
    final ElwhaBadgeAnchor.Attachment attachment = ElwhaBadgeAnchor.attach(host, badge);
    ElwhaBadgeAnchor.detach(attachment);
    final Rectangle parked = badge.getBounds();

    host.setLocation(120, 120);

    assertThat(badge.getBounds())
        .as("detach removes the listeners, so a later host move is not followed")
        .isEqualTo(parked);
  }

  @Test
  void explicitBoundsOverloadFreezesTheRectItWasGiven() {
    final JComponent host = new JComponent() {};
    AnchorFixture.with(host, 20, 16, 48, 48);
    final Rectangle supplied = new Rectangle(ICON);
    final ElwhaBadge badge = ElwhaBadge.small();

    ElwhaBadgeAnchor.attach(host, supplied, badge);
    supplied.setBounds(0, 0, 4, 4);

    assertThat(badge.getX())
        .as("the rect is defensively copied, so a caller's later mutation cannot move the badge")
        .isEqualTo(iconTopRightInLayeredPane(host, ICON).x - SMALL_OFFSET_X);
  }

  // ------------------------------------------------------------------ guards

  @Test
  void attachRejectsANonComponentHost() {
    final IconBearing notAComponent = Rectangle::new;

    assertThatExceptionOfType(IllegalArgumentException.class)
        .as("the anchor installs listeners on the host, so it must be a JComponent")
        .isThrownBy(() -> ElwhaBadgeAnchor.attach(notAComponent, ElwhaBadge.small()));
  }

  @Test
  void attachRejectsNullArguments() {
    final IconHost host = host();

    assertThatExceptionOfType(NullPointerException.class)
        .isThrownBy(() -> ElwhaBadgeAnchor.attach(null, ElwhaBadge.small()));
    assertThatExceptionOfType(NullPointerException.class)
        .isThrownBy(() -> ElwhaBadgeAnchor.attach(host, null));
    assertThatExceptionOfType(NullPointerException.class)
        .isThrownBy(() -> ElwhaBadgeAnchor.attach(host, ElwhaBadge.small(), null));
    assertThatExceptionOfType(NullPointerException.class)
        .isThrownBy(
            () ->
                ElwhaBadgeAnchor.attach(
                    host, ElwhaBadge.small(), ElwhaBadgeAnchor.AnchorMode.ICON_CORNER, null));
  }

  @Test
  void detachRejectsNull() {
    assertThatExceptionOfType(NullPointerException.class)
        .isThrownBy(() -> ElwhaBadgeAnchor.detach(null));
  }

  @Test
  void refreshBeforeAnyMountIsHarmless() {
    final IconHost host = host();
    final ElwhaBadge badge = ElwhaBadge.small();

    ElwhaBadgeAnchor.attach(host, badge).refresh();

    assertThat(badge.getBounds())
        .as("with no layered pane there is nothing to place against — and no exception")
        .isEqualTo(new Rectangle(0, 0, 0, 0));
  }
}
