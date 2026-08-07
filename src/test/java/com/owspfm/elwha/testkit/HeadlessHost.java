package com.owspfm.elwha.testkit;

import java.awt.Component;
import java.awt.Rectangle;
import java.awt.event.ComponentEvent;
import java.util.ArrayList;
import java.util.List;
import javax.swing.JComponent;
import javax.swing.JLayeredPane;
import javax.swing.JPanel;
import javax.swing.JRootPane;

/**
 * A mountable overlay host for the headless tier — a sized, laid-out {@link JRootPane} carrying one
 * anchor component. Elwha's in-window overlays resolve their host through {@code
 * SwingUtilities.getRootPane(parent)} and attach to that pane's {@link JLayeredPane}, and a {@code
 * JRootPane} supplies both without a {@link java.awt.Window} — so mount, z-band, chain, and
 * teardown contracts are reachable under {@code java.awt.headless=true}, where constructing a
 * {@code JFrame} would throw.
 *
 * <p><strong>What this host cannot represent.</strong> Nothing here is <em>showing</em> in the AWT
 * sense ({@code Component.isShowing()} needs a realized window at the top of the hierarchy), so the
 * anchored placement engines degrade to a zero-rect anchor and no real focus is arbitrated. Assert
 * placement through the placement engines' pure static functions instead, and real placement and
 * focus ownership in the {@code gui} tier.
 *
 * @author Charles Bryan
 * @version v0.5.0
 * @since v0.5.0
 */
public final class HeadlessHost {

  private final JRootPane rootPane;
  private final JComponent anchor;

  /**
   * Creates a host of the given size with an anchor at a default interior position.
   *
   * @param width the host width in px
   * @param height the host height in px
   * @version v0.5.0
   * @since v0.5.0
   */
  public HeadlessHost(final int width, final int height) {
    this(width, height, new Rectangle(40, 60, 120, 32));
  }

  /**
   * Creates a host of the given size with an anchor at the given bounds.
   *
   * @param width the host width in px
   * @param height the host height in px
   * @param anchorBounds the anchor's bounds within the content pane
   * @version v0.5.0
   * @since v0.5.0
   */
  public HeadlessHost(final int width, final int height, final Rectangle anchorBounds) {
    this.rootPane = new JRootPane();
    this.anchor = new JPanel();
    anchor.setFocusable(true);
    rootPane.getContentPane().setLayout(null);
    rootPane.getContentPane().add(anchor);
    anchor.setBounds(anchorBounds);
    resize(width, height);
  }

  /**
   * Resizes the host and re-lays its panes — the seam a resize-driven relayout test drives.
   *
   * @param width the new width in px
   * @param height the new height in px
   * @version v0.5.0
   * @since v0.5.0
   */
  public void resize(final int width, final int height) {
    rootPane.setSize(width, height);
    rootPane.doLayout();
    rootPane.getLayeredPane().doLayout();
    rootPane.getContentPane().doLayout();
  }

  /**
   * Resizes the host and delivers the resize notification synchronously — the seam an overlay's
   * relayout-on-resize listener reacts to. AWT posts {@code COMPONENT_RESIZED} to the event queue
   * rather than dispatching it inline, and a headless test method already runs on the dispatch
   * thread, so a queued event would not be processed before the assertions run. Dispatching it here
   * delivers exactly the event a real resize produces, without the queue round-trip.
   *
   * @param width the new width in px
   * @param height the new height in px
   * @version v0.5.0
   * @since v0.5.0
   */
  public void resizeAndNotify(final int width, final int height) {
    resize(width, height);
    layeredPane()
        .dispatchEvent(new ComponentEvent(layeredPane(), ComponentEvent.COMPONENT_RESIZED));
  }

  /**
   * The host root pane — pass {@link #anchor()} (or any descendant) to an overlay's {@code show}.
   *
   * @return the root pane
   * @version v0.5.0
   * @since v0.5.0
   */
  public JRootPane rootPane() {
    return rootPane;
  }

  /**
   * The layered pane overlays mount on.
   *
   * @return the layered pane
   * @version v0.5.0
   * @since v0.5.0
   */
  public JLayeredPane layeredPane() {
    return rootPane.getLayeredPane();
  }

  /**
   * The anchor component inside the content pane.
   *
   * @return the anchor
   * @version v0.5.0
   * @since v0.5.0
   */
  public JComponent anchor() {
    return anchor;
  }

  /**
   * The z-band a mounted component sits on.
   *
   * @param component a child of the layered pane
   * @return the layer constraint the component was added with
   * @version v0.5.0
   * @since v0.5.0
   */
  public int layerOf(final Component component) {
    return layeredPane().getLayer(component);
  }

  /**
   * Everything mounted on the layered pane other than the content pane — i.e. the overlay surfaces
   * and backdrops currently attached, in the pane's own child order.
   *
   * @return the mounted overlay components
   * @version v0.5.0
   * @since v0.5.0
   */
  public List<Component> mounted() {
    final List<Component> out = new ArrayList<>();
    for (final Component child : layeredPane().getComponents()) {
      if (child != rootPane.getContentPane()) {
        out.add(child);
      }
    }
    return out;
  }
}
