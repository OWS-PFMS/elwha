package com.owspfm.elwha.badge;

import java.awt.Dimension;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.event.ComponentAdapter;
import java.awt.event.ComponentEvent;
import java.awt.event.ComponentListener;
import java.awt.event.HierarchyBoundsAdapter;
import java.awt.event.HierarchyBoundsListener;
import java.awt.event.HierarchyEvent;
import java.awt.event.HierarchyListener;
import java.beans.PropertyChangeListener;
import java.util.Objects;
import java.util.function.Supplier;
import javax.swing.JComponent;
import javax.swing.JLayeredPane;
import javax.swing.JRootPane;
import javax.swing.SwingUtilities;

/**
 * Placement primitive that attaches an {@link ElwhaBadge} to a host icon-bearing component, per M3
 * Expressive badge anchor geometry (design doc §5). Two placements are offered via {@link
 * AnchorMode}: the default {@link AnchorMode#ICON_CORNER} pins the badge to the upper-trailing
 * corner of the host's icon bounding box (the canonical nav-rail case); {@link
 * AnchorMode#TRAILING_EDGE} pins it to the host composition's trailing edge, vertically centered —
 * the M3 "Favorites 84" pattern, where a Large badge follows a label + icon row ([#219]). Owns
 * three concerns the consumer would otherwise have to assemble manually: placement, RTL mirroring
 * (S4, #213), and push-model accessibility wiring (S5, #214). Mirrors the FAB §15 floating-FAB
 * recipe as a first-class primitive — the lesson #205 tracked.
 *
 * <p><strong>Z-order.</strong> The badge is added to the host's nearest {@link JLayeredPane}
 * ancestor (the root pane's layered pane in the common JFrame/JDialog case) at {@link
 * JLayeredPane#PALETTE_LAYER}. That floats it above ordinary content but below modal dialogs.
 *
 * <p><strong>Bounds geometry.</strong> The badge's bottom-leading corner is pinned a variant-
 * dependent offset from the icon's top-trailing corner — Small 6 × 6 dp, Large 14 × 12 dp (design
 * doc §5.1). As Large content widens via {@link ElwhaBadge#setContent(String)} the container grows
 * leading-ward, keeping the pin glued in place (design doc §5.2). The anchor reads {@code
 * host.getComponentOrientation()} at every bounds computation and at any {@code
 * componentOrientation} property change on the host, so RTL hosts mirror the badge to the icon's
 * upper-leading corner automatically — design doc §11.
 *
 * <p><strong>Invariant: one badge per host.</strong> Each host carries at most one anchored badge.
 * Re-attaching replaces the prior attachment (its badge is detached first); there is no multi-slot
 * semantic in M3.
 *
 * <p><strong>Late attach is supported.</strong> The host need not be in a hierarchy at attach time
 * — installation is deferred until the host is added (via an internal {@link HierarchyListener}).
 * The {@link Attachment} returned is valid from the moment {@link #attach(IconBearing, ElwhaBadge)}
 * returns regardless of when actual badge mounting happens.
 *
 * <p><strong>Push-model accessibility.</strong> On attach, the host's pre-attach accessible name is
 * captured and replaced with {@code "{hostBaseName} {badge.accessibilityText}"} (host name first,
 * badge text appended per design doc §10.4). The anchor listens for {@link
 * ElwhaBadge#PROPERTY_ACCESSIBILITY_TEXT} on the badge and updates the spliced name in lock-step.
 * On detach, the host's accessible name is restored to its captured pre-attach value, and the
 * badge's {@link javax.accessibility.AccessibleRelation#LABEL_FOR LABEL_FOR} relation back to the
 * host is cleared. Badges are non-focusable and have no independent accessible action — AT users
 * address the badge by navigating to the host destination.
 *
 * @author Charles Bryan
 * @version v0.3.0
 * @since v0.3.0
 */
public final class ElwhaBadgeAnchor {

  /** Small badge offset (icon top-trailing → badge bottom-leading), vertical, in dp. */
  private static final int SMALL_OFFSET_Y_DP = 6;

  /** Small badge offset (icon top-trailing → badge bottom-leading), horizontal, in dp. */
  private static final int SMALL_OFFSET_X_DP = 6;

  /** Large badge offset (icon top-trailing → badge bottom-leading), vertical, in dp. */
  private static final int LARGE_OFFSET_Y_DP = 14;

  /** Large badge offset (icon top-trailing → badge bottom-leading), horizontal, in dp. */
  private static final int LARGE_OFFSET_X_DP = 12;

  /** Client-property key on the host marking the active attachment. */
  private static final String HOST_ATTACHMENT_KEY = "ElwhaBadgeAnchor.attachment";

  private ElwhaBadgeAnchor() {}

  /**
   * Where the badge is pinned relative to its host — the two placements M3 documents (design doc §1
   * / §14).
   *
   * @author Charles Bryan
   * @version v0.3.0
   * @since v0.3.0
   */
  public enum AnchorMode {

    /**
     * The badge's bottom-leading corner is pinned a variant-dependent offset from the host's icon
     * top-trailing corner — the canonical nav-rail / icon-with-badge case. Requires the host to
     * expose icon bounds (an {@link IconBearing} host or the explicit-bounds overload).
     *
     * @version v0.3.0
     * @since v0.3.0
     */
    ICON_CORNER,

    /**
     * The badge is pinned at the host composition's overall trailing edge, vertically centered on
     * the host — the M3 "Favorites 84" pattern, where a Large badge follows a label + icon row at
     * its trailing end rather than sitting on the icon's corner (design doc §1 Do/Don't). The icon
     * bounds are not consulted; the host's full bounds drive placement, so this mode works on any
     * host (icon-bearing or not). RTL mirrors the trailing edge to the leading (left) edge.
     *
     * <p>The badge is centered on the host's vertical center, the robust approximation of the
     * spec's "centered on the label baseline" for an arbitrary host composition.
     *
     * @version v0.3.0
     * @since v0.3.0
     */
    TRAILING_EDGE
  }

  /**
   * Attaches the given badge to an {@link IconBearing} host. The host must also be a {@link
   * JComponent}. If the host already has an anchored badge, the prior attachment is detached first.
   *
   * @param host the host component; must implement {@link IconBearing} and be a {@link JComponent}
   * @param badge the badge to anchor
   * @return an attachment handle that can be passed to {@link #detach(Attachment)}
   * @throws NullPointerException if either argument is {@code null}
   * @throws IllegalArgumentException if {@code host} is not a {@link JComponent}
   * @version v0.3.0
   * @since v0.3.0
   */
  public static Attachment attach(final IconBearing host, final ElwhaBadge badge) {
    return attach(host, badge, AnchorMode.ICON_CORNER);
  }

  /**
   * Attaches the given badge to an {@link IconBearing} host at the requested {@link AnchorMode}.
   * The host must also be a {@link JComponent}. If the host already has an anchored badge, the
   * prior attachment is detached first.
   *
   * <p>{@link AnchorMode#ICON_CORNER} pins the badge to the host's icon corner (the icon bounds are
   * read from {@link IconBearing#getIconBounds()}); {@link AnchorMode#TRAILING_EDGE} pins it to the
   * host composition's trailing edge and ignores the icon bounds.
   *
   * @param host the host component; must implement {@link IconBearing} and be a {@link JComponent}
   * @param badge the badge to anchor
   * @param mode the placement mode
   * @return an attachment handle that can be passed to {@link #detach(Attachment)}
   * @throws NullPointerException if any argument is {@code null}
   * @throws IllegalArgumentException if {@code host} is not a {@link JComponent}
   * @version v0.3.0
   * @since v0.3.0
   */
  public static Attachment attach(
      final IconBearing host, final ElwhaBadge badge, final AnchorMode mode) {
    Objects.requireNonNull(host, "host");
    Objects.requireNonNull(badge, "badge");
    Objects.requireNonNull(mode, "mode");
    if (!(host instanceof JComponent jc)) {
      throw new IllegalArgumentException("IconBearing host must be a JComponent");
    }
    return doAttach(jc, host::getIconBounds, badge, mode);
  }

  /**
   * Attaches the given badge to a non-{@link IconBearing} host with an explicit icon bounding box.
   * The bounds are captured at attach time; the consumer must call {@link Attachment#refresh()} (or
   * detach + re-attach) if the icon position changes within the host.
   *
   * @param host the host component
   * @param iconBounds the icon bounding box in {@code host}'s coordinate space (defensively copied)
   * @param badge the badge to anchor
   * @return an attachment handle that can be passed to {@link #detach(Attachment)}
   * @throws NullPointerException if any argument is {@code null}
   * @version v0.3.0
   * @since v0.3.0
   */
  public static Attachment attach(
      final JComponent host, final Rectangle iconBounds, final ElwhaBadge badge) {
    Objects.requireNonNull(host, "host");
    Objects.requireNonNull(iconBounds, "iconBounds");
    Objects.requireNonNull(badge, "badge");
    final Rectangle frozen = new Rectangle(iconBounds);
    return doAttach(host, () -> frozen, badge, AnchorMode.ICON_CORNER);
  }

  /**
   * Attaches the given badge to an arbitrary host at its trailing edge ({@link
   * AnchorMode#TRAILING_EDGE}) — the M3 "Favorites 84" pattern, where a Large badge follows a label
   * + icon composition at the row's trailing end, vertically centered. The host need not be {@link
   * IconBearing} (the icon bounds are not consulted in this mode); any {@link JComponent}
   * composition works. RTL mirrors the trailing edge to the leading (left) edge. If the host
   * already has an anchored badge, the prior attachment is detached first.
   *
   * @param host the host composition
   * @param badge the badge to anchor (a Large badge per the M3 pattern, though not enforced)
   * @return an attachment handle that can be passed to {@link #detach(Attachment)}
   * @throws NullPointerException if either argument is {@code null}
   * @version v0.3.0
   * @since v0.3.0
   */
  public static Attachment attachTrailingEdge(final JComponent host, final ElwhaBadge badge) {
    Objects.requireNonNull(host, "host");
    Objects.requireNonNull(badge, "badge");
    return doAttach(host, Rectangle::new, badge, AnchorMode.TRAILING_EDGE);
  }

  /**
   * Detaches a badge from its host, removing all installed listeners and restoring the host to its
   * pre-attach state. Idempotent — calling {@code detach()} twice is a no-op.
   *
   * @param attachment the handle returned by {@code attach(...)}
   * @throws NullPointerException if {@code attachment} is {@code null}
   * @version v0.3.0
   * @since v0.3.0
   */
  public static void detach(final Attachment attachment) {
    Objects.requireNonNull(attachment, "attachment");
    attachment.detach();
  }

  private static Attachment doAttach(
      final JComponent host,
      final Supplier<Rectangle> iconBoundsSupplier,
      final ElwhaBadge badge,
      final AnchorMode mode) {
    final Attachment prior = (Attachment) host.getClientProperty(HOST_ATTACHMENT_KEY);
    if (prior != null) {
      prior.detach();
    }
    final Attachment attachment = new Attachment(host, iconBoundsSupplier, badge, mode);
    host.putClientProperty(HOST_ATTACHMENT_KEY, attachment);
    attachment.install();
    return attachment;
  }

  /**
   * Opaque handle returned by {@code attach(...)} — pass to {@link
   * ElwhaBadgeAnchor#detach(Attachment)} to undo the attachment. {@link #refresh()} is exposed for
   * consumers using the explicit-bounds overload whose icon position has moved.
   *
   * @author Charles Bryan
   * @version v0.3.0
   * @since v0.3.0
   */
  public static final class Attachment {

    private final JComponent host;
    private final Supplier<Rectangle> iconBoundsSupplier;
    private final ElwhaBadge badge;
    private final AnchorMode mode;

    private JLayeredPane layeredPane;
    private boolean detached;

    private String hostBaseName;
    private boolean hostBaseNameCaptured;

    private final ComponentListener hostBoundsListener =
        new ComponentAdapter() {
          @Override
          public void componentResized(final ComponentEvent e) {
            refresh();
          }

          @Override
          public void componentMoved(final ComponentEvent e) {
            refresh();
          }
        };

    private final HierarchyBoundsListener ancestorBoundsListener =
        new HierarchyBoundsAdapter() {
          @Override
          public void ancestorMoved(final HierarchyEvent e) {
            refresh();
          }

          @Override
          public void ancestorResized(final HierarchyEvent e) {
            refresh();
          }
        };

    private final HierarchyListener hierarchyListener =
        e -> {
          final long flags = e.getChangeFlags();
          if ((flags & HierarchyEvent.PARENT_CHANGED) != 0) {
            reseatToCurrentHierarchy();
          }
          if ((flags & HierarchyEvent.SHOWING_CHANGED) != 0) {
            syncBadgeVisibility();
          }
        };

    private final PropertyChangeListener badgeContentListener = e -> refresh();

    private final PropertyChangeListener hostOrientationListener = e -> refresh();

    private final PropertyChangeListener badgeAccessibilityListener = e -> applyAccessibleName();

    private Attachment(
        final JComponent host,
        final Supplier<Rectangle> iconBoundsSupplier,
        final ElwhaBadge badge,
        final AnchorMode mode) {
      this.host = host;
      this.iconBoundsSupplier = iconBoundsSupplier;
      this.badge = badge;
      this.mode = mode;
    }

    private void install() {
      host.addComponentListener(hostBoundsListener);
      host.addHierarchyBoundsListener(ancestorBoundsListener);
      host.addHierarchyListener(hierarchyListener);
      host.addPropertyChangeListener("componentOrientation", hostOrientationListener);
      badge.addPropertyChangeListener(ElwhaBadge.PROPERTY_CONTENT, badgeContentListener);
      badge.addPropertyChangeListener(
          ElwhaBadge.PROPERTY_ACCESSIBILITY_TEXT, badgeAccessibilityListener);
      // host.getAccessibleContext() can be null for bare JComponent subclasses that don't
      // initialize one (most production hosts will return non-null via AccessibleJComponent,
      // but the explicit-bounds overload accepts arbitrary JComponents). Skip the a11y splice
      // entirely when no context is exposed; the badge still paints / anchors correctly.
      final javax.accessibility.AccessibleContext ctx = host.getAccessibleContext();
      if (ctx != null) {
        hostBaseName = ctx.getAccessibleName();
        hostBaseNameCaptured = true;
        applyAccessibleName();
      }
      badge.anchorSetLabelFor(host);
      reseatToCurrentHierarchy();
    }

    private void applyAccessibleName() {
      if (detached || !hostBaseNameCaptured) {
        return;
      }
      final javax.accessibility.AccessibleContext ctx = host.getAccessibleContext();
      if (ctx == null) {
        return;
      }
      final String announcement = badge.getAccessibilityText();
      final String combined =
          hostBaseName == null || hostBaseName.isEmpty()
              ? announcement
              : hostBaseName + " " + announcement;
      ctx.setAccessibleName(combined);
    }

    private void reseatToCurrentHierarchy() {
      if (detached) {
        return;
      }
      final JRootPane root = SwingUtilities.getRootPane(host);
      final JLayeredPane next = root != null ? root.getLayeredPane() : null;
      if (next == layeredPane) {
        refresh();
        return;
      }
      if (layeredPane != null) {
        layeredPane.remove(badge);
        layeredPane.repaint();
      }
      layeredPane = next;
      if (layeredPane != null) {
        syncBadgeVisibility();
        layeredPane.add(badge, JLayeredPane.PALETTE_LAYER);
        refresh();
      }
    }

    /**
     * Badge sits on a shared {@link JLayeredPane} that doesn't follow the host's effective
     * visibility automatically — switching cards (e.g., {@code CardLayout}, {@code JTabbedPane})
     * hides the host but the badge keeps painting on top of whatever card replaces it. Mirror
     * {@code host.isShowing()} on the badge so it disappears alongside its host.
     */
    private void syncBadgeVisibility() {
      badge.setVisible(host.isShowing());
    }

    /**
     * Recomputes and applies the badge's bounds against the host's current geometry and the badge's
     * current preferred size. Called automatically on host resize/move, ancestor resize/move,
     * hierarchy change, and badge content change — consumers using the explicit- bounds overload
     * can call this manually if their icon position shifts within the host.
     *
     * @version v0.3.0
     * @since v0.3.0
     */
    public void refresh() {
      if (detached || layeredPane == null) {
        return;
      }
      if (mode == AnchorMode.TRAILING_EDGE) {
        refreshTrailingEdge();
        return;
      }
      final Rectangle icon = iconBoundsSupplier.get();
      // Empty iconBounds — host has no icon currently installed. Without this guard the
      // anchor would still place the badge at (offsetY, -offsetX) relative to the host's
      // origin, floating in space. Hide until iconBounds become non-empty; the next refresh
      // (triggered by a host resize / parent change) re-shows.
      if (icon.width <= 0 || icon.height <= 0) {
        badge.setVisible(false);
        return;
      }
      // Re-sync visibility in case a prior refresh hid us; the SHOWING_CHANGED listener
      // alone doesn't fire when the icon transitions from absent to present.
      syncBadgeVisibility();
      final boolean ltr = host.getComponentOrientation().isLeftToRight();

      // "Top-trailing" in image coords: right edge in LTR, left edge in RTL.
      final int iconTopTrailingX = ltr ? icon.x + icon.width : icon.x;
      final Point topTrailingInHost = new Point(iconTopTrailingX, icon.y);
      final Point topTrailingInLayered =
          SwingUtilities.convertPoint(host, topTrailingInHost, layeredPane);

      final int offsetY =
          badge.getVariant() == ElwhaBadge.Variant.SMALL ? SMALL_OFFSET_Y_DP : LARGE_OFFSET_Y_DP;
      final int offsetX =
          badge.getVariant() == ElwhaBadge.Variant.SMALL ? SMALL_OFFSET_X_DP : LARGE_OFFSET_X_DP;
      final Dimension pref = badge.getPreferredSize();

      // Pinned-corner X (the badge's bottom-leading corner, which is bottom-left in LTR /
      // bottom-right in RTL) sits offsetX inward from the icon's top-trailing X.
      final int pinX = ltr ? topTrailingInLayered.x - offsetX : topTrailingInLayered.x + offsetX;
      final int pinY = topTrailingInLayered.y + offsetY;

      // Convert pinned corner → top-left for setBounds. In LTR the pin IS the left edge; in RTL
      // the pin is the right edge, so subtract the width.
      final int badgeX = ltr ? pinX : pinX - pref.width;
      final int badgeY = pinY - pref.height;

      badge.setBounds(badgeX, badgeY, pref.width, pref.height);
      badge.revalidate();
      badge.repaint();
    }

    // TRAILING_EDGE placement: the badge sits flush against the host composition's trailing edge,
    // vertically centered on the host — the M3 "Favorites 84" pattern. The icon bounds are not
    // consulted; the host's full bounds drive placement. RTL mirrors the trailing edge to the left.
    private void refreshTrailingEdge() {
      final int hostW = host.getWidth();
      final int hostH = host.getHeight();
      if (hostW <= 0 || hostH <= 0) {
        badge.setVisible(false);
        return;
      }
      syncBadgeVisibility();
      final boolean ltr = host.getComponentOrientation().isLeftToRight();
      final Dimension pref = badge.getPreferredSize();

      // Align the badge's trailing edge to the host's trailing edge (right in LTR, left in RTL) and
      // center it vertically on the host.
      final int badgeXInHost = ltr ? hostW - pref.width : 0;
      final int badgeYInHost = (hostH - pref.height) / 2;
      final Point topLeftInLayered =
          SwingUtilities.convertPoint(host, new Point(badgeXInHost, badgeYInHost), layeredPane);

      badge.setBounds(topLeftInLayered.x, topLeftInLayered.y, pref.width, pref.height);
      badge.revalidate();
      badge.repaint();
    }

    private void detach() {
      if (detached) {
        return;
      }
      detached = true;
      host.removeComponentListener(hostBoundsListener);
      host.removeHierarchyBoundsListener(ancestorBoundsListener);
      host.removeHierarchyListener(hierarchyListener);
      host.removePropertyChangeListener("componentOrientation", hostOrientationListener);
      badge.removePropertyChangeListener(ElwhaBadge.PROPERTY_CONTENT, badgeContentListener);
      badge.removePropertyChangeListener(
          ElwhaBadge.PROPERTY_ACCESSIBILITY_TEXT, badgeAccessibilityListener);
      if (hostBaseNameCaptured) {
        final javax.accessibility.AccessibleContext ctx = host.getAccessibleContext();
        if (ctx != null) {
          ctx.setAccessibleName(hostBaseName);
        }
      }
      badge.anchorSetLabelFor(null);
      if (layeredPane != null) {
        layeredPane.remove(badge);
        layeredPane.repaint();
        layeredPane = null;
      }
      if (host.getClientProperty(HOST_ATTACHMENT_KEY) == this) {
        host.putClientProperty(HOST_ATTACHMENT_KEY, null);
      }
    }
  }
}
