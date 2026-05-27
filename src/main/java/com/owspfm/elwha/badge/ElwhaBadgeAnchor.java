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
 * Placement primitive that attaches an {@link ElwhaBadge} to a host icon-bearing component at the
 * upper-trailing corner of the host's icon bounding box, per M3 Expressive badge anchor geometry
 * (design doc §5). Owns three concerns the consumer would otherwise have to assemble manually:
 * placement (this story), RTL mirroring (S4, #213), and push-model accessibility wiring (S5, #214).
 * Mirrors the FAB §15 floating-FAB recipe as a first-class primitive — the lesson #205 tracked.
 *
 * <p><strong>Z-order.</strong> The badge is added to the host's nearest {@link JLayeredPane}
 * ancestor (the root pane's layered pane in the common JFrame/JDialog case) at {@link
 * JLayeredPane#PALETTE_LAYER}. That floats it above ordinary content but below modal dialogs.
 *
 * <p><strong>Bounds geometry.</strong> The badge's bottom-leading corner is pinned a variant-
 * dependent offset from the icon's top-trailing corner — Small 6 × 6 dp, Large 14 × 12 dp (design
 * doc §5.1). As Large content widens via {@link ElwhaBadge#setContent(String)} the container grows
 * leftward, keeping the pin glued in place (design doc §5.2). For S3 the anchor is LTR- only — RTL
 * flipping lands in S4 (#213).
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
    Objects.requireNonNull(host, "host");
    Objects.requireNonNull(badge, "badge");
    if (!(host instanceof JComponent jc)) {
      throw new IllegalArgumentException("IconBearing host must be a JComponent");
    }
    return doAttach(jc, host::getIconBounds, badge);
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
    return doAttach(host, () -> frozen, badge);
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
      final JComponent host, final Supplier<Rectangle> iconBoundsSupplier, final ElwhaBadge badge) {
    final Attachment prior = (Attachment) host.getClientProperty(HOST_ATTACHMENT_KEY);
    if (prior != null) {
      prior.detach();
    }
    final Attachment attachment = new Attachment(host, iconBoundsSupplier, badge);
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

    private JLayeredPane layeredPane;
    private boolean detached;

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
          if ((e.getChangeFlags() & HierarchyEvent.PARENT_CHANGED) != 0) {
            reseatToCurrentHierarchy();
          }
        };

    private final PropertyChangeListener badgeContentListener = e -> refresh();

    private Attachment(
        final JComponent host,
        final Supplier<Rectangle> iconBoundsSupplier,
        final ElwhaBadge badge) {
      this.host = host;
      this.iconBoundsSupplier = iconBoundsSupplier;
      this.badge = badge;
    }

    private void install() {
      host.addComponentListener(hostBoundsListener);
      host.addHierarchyBoundsListener(ancestorBoundsListener);
      host.addHierarchyListener(hierarchyListener);
      badge.addPropertyChangeListener(ElwhaBadge.PROPERTY_CONTENT, badgeContentListener);
      reseatToCurrentHierarchy();
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
        layeredPane.add(badge, JLayeredPane.PALETTE_LAYER);
        refresh();
      }
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
      final Rectangle icon = iconBoundsSupplier.get();
      final Point topTrailingInHost = new Point(icon.x + icon.width, icon.y);
      final Point topTrailingInLayered =
          SwingUtilities.convertPoint(host, topTrailingInHost, layeredPane);
      final int offsetY =
          badge.getVariant() == ElwhaBadge.Variant.SMALL ? SMALL_OFFSET_Y_DP : LARGE_OFFSET_Y_DP;
      final int offsetX =
          badge.getVariant() == ElwhaBadge.Variant.SMALL ? SMALL_OFFSET_X_DP : LARGE_OFFSET_X_DP;
      final Dimension pref = badge.getPreferredSize();
      final int bottomLeadingX = topTrailingInLayered.x - offsetX;
      final int bottomLeadingY = topTrailingInLayered.y + offsetY;
      final int badgeX = bottomLeadingX - pref.width;
      final int badgeY = bottomLeadingY - pref.height;
      badge.setBounds(badgeX, badgeY, pref.width, pref.height);
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
      badge.removePropertyChangeListener(ElwhaBadge.PROPERTY_CONTENT, badgeContentListener);
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
