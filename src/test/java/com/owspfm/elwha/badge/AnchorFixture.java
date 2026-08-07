package com.owspfm.elwha.badge;

import java.awt.ComponentOrientation;
import java.awt.Dimension;
import java.awt.Rectangle;
import java.awt.event.ComponentEvent;
import javax.swing.JComponent;
import javax.swing.JLayeredPane;
import javax.swing.JPanel;
import javax.swing.JRootPane;

/**
 * A hand-laid-out root-pane hierarchy for the anchor tests. {@link ElwhaBadgeAnchor} mounts its
 * badge on the host's nearest {@link JLayeredPane} ancestor and converts coordinates through the
 * chain, so the assertions need a real ancestor chain with real offsets — but not a realized
 * window, which the headless tier has no way to produce.
 *
 * <p>The host is deliberately nested one level below the content pane at a non-zero origin: with
 * the host sitting directly at the layered pane's origin, every {@code convertPoint} in the anchor
 * would be the identity and the placement assertions would pass whether or not the conversion
 * happened at all.
 *
 * <p>Bounds are assigned explicitly rather than through layout managers. {@code Container.validate}
 * is a no-op without a peer, so a layout-manager hierarchy would report zero-sized children here.
 *
 * <p><strong>Visibility caveat.</strong> Nothing in this hierarchy is {@code showing} — there is no
 * window — so the anchor's {@code host.isShowing()} mirror always parks the badge invisible.
 * Placement still runs and {@code setBounds} is still applied, which is what these tests read; the
 * visibility mirror itself is a display-bound behavior and is not asserted from this tier.
 */
final class AnchorFixture {

  /** Root pane size — larger than any fixture host, so nothing clamps. */
  static final int ROOT_WIDTH = 400;

  /** Root pane size — larger than any fixture host, so nothing clamps. */
  static final int ROOT_HEIGHT = 300;

  /** Holder origin inside the content pane; the offset every coordinate conversion must apply. */
  static final int HOLDER_X = 30;

  /** Holder origin inside the content pane; the offset every coordinate conversion must apply. */
  static final int HOLDER_Y = 40;

  private final JRootPane root;
  private final JPanel holder;

  private AnchorFixture() {
    root = new JRootPane();
    root.setSize(ROOT_WIDTH, ROOT_HEIGHT);
    root.getLayeredPane().setBounds(0, 0, ROOT_WIDTH, ROOT_HEIGHT);
    root.getContentPane().setLayout(null);
    root.getContentPane().setBounds(0, 0, ROOT_WIDTH, ROOT_HEIGHT);
    holder = new JPanel(null);
    holder.setBounds(HOLDER_X, HOLDER_Y, 300, 200);
    root.getContentPane().add(holder);
  }

  /** Creates an empty hierarchy, ready for {@link #mount}. */
  static AnchorFixture create() {
    return new AnchorFixture();
  }

  /**
   * Creates a hierarchy with the host already mounted at the given bounds inside the holder — the
   * common case, where the badge is attached to a host that is already in a hierarchy.
   */
  static AnchorFixture with(
      final JComponent host, final int x, final int y, final int w, final int h) {
    final AnchorFixture fixture = create();
    fixture.mount(host, x, y, w, h);
    return fixture;
  }

  /**
   * Adds the host to the holder at the given bounds, firing the hierarchy events a real add does.
   */
  void mount(final JComponent host, final int x, final int y, final int w, final int h) {
    host.setBounds(x, y, w, h);
    holder.add(host);
  }

  /** Removes the host from the hierarchy, firing the same events a real remove does. */
  void unmount(final JComponent host) {
    holder.remove(host);
  }

  /** The layered pane the anchor mounts badges onto. */
  JLayeredPane layeredPane() {
    return root.getLayeredPane();
  }

  /** Flips the whole hierarchy to right-to-left, as a locale switch would. */
  void applyRightToLeft() {
    root.applyComponentOrientation(ComponentOrientation.RIGHT_TO_LEFT);
  }

  /**
   * The host origin expressed in layered-pane coordinates — the translation every expected
   * placement below is written against.
   */
  static java.awt.Point hostOriginInLayeredPane(final JComponent host) {
    return new java.awt.Point(HOLDER_X + host.getX(), HOLDER_Y + host.getY());
  }

  /**
   * Relocates the host and delivers the move notification synchronously.
   *
   * <p>{@code Component.setLocation} <em>posts</em> {@code COMPONENT_MOVED} to the event queue
   * rather than dispatching it, so inside an on-EDT test the anchor's listener would not run until
   * after the test method returned. Dispatching the event explicitly puts it through the same
   * {@code Component.dispatchEvent} pipeline the toolkit would — the {@code testkit/Input} idiom
   * for mouse events — so the listener registration is genuinely exercised; only the delivery
   * timing is made synchronous.
   */
  static void moveTo(final JComponent host, final int x, final int y) {
    host.setLocation(x, y);
    host.dispatchEvent(new ComponentEvent(host, ComponentEvent.COMPONENT_MOVED));
  }

  /** Resizes the host and delivers the resize notification synchronously; see {@link #moveTo}. */
  static void resizeTo(final JComponent host, final int width, final int height) {
    host.setSize(width, height);
    host.dispatchEvent(new ComponentEvent(host, ComponentEvent.COMPONENT_RESIZED));
  }

  /**
   * A minimal {@link IconBearing} host with a settable icon rect — the anchor's whole host contract
   * is "hand me the icon box", so a stub isolates the placement math from any real component's
   * layout.
   *
   * <p>Exposes an {@link javax.accessibility.AccessibleContext}, as every production host does; a
   * bare {@link JComponent} returns {@code null} there, which is a separate path the anchor guards
   * and the accessibility suite covers on its own.
   */
  static final class IconHost extends JComponent implements IconBearing {

    private Rectangle iconBounds;

    IconHost(final Rectangle iconBounds) {
      this.iconBounds = iconBounds;
    }

    void setIconBounds(final Rectangle bounds) {
      this.iconBounds = bounds;
    }

    @Override
    public Rectangle getIconBounds() {
      // Fresh copy per call, per the IconBearing contract.
      return new Rectangle(iconBounds);
    }

    @Override
    public Dimension getPreferredSize() {
      return new Dimension(48, 48);
    }

    @Override
    public javax.accessibility.AccessibleContext getAccessibleContext() {
      if (accessibleContext == null) {
        accessibleContext = new AccessibleJComponent() {};
      }
      return accessibleContext;
    }
  }
}
