package com.owspfm.elwha.badge;

import java.awt.Rectangle;
import javax.swing.JComponent;

/**
 * Contract for components that own an icon at a known position inside their own coordinate space —
 * the placement target {@link ElwhaBadgeAnchor} anchors badges to. Hosts that conform expose their
 * icon bounding box via {@link #getIconBounds()}; the anchor primitive does the rest (placement,
 * RTL mirroring once S4 lands, accessibility wiring once S5 lands).
 *
 * <p>Implementations must also be a {@link JComponent} — the anchor primitive needs to install
 * listeners on the host and use Swing coordinate-conversion utilities against it. Non-conforming
 * hosts (e.g. components that own an icon but can't / shouldn't implement this interface) can still
 * anchor a badge via {@link ElwhaBadgeAnchor#attach(JComponent, Rectangle, ElwhaBadge)}, which
 * takes an explicit {@link Rectangle} instead.
 *
 * @author Charles Bryan
 * @version v0.3.0
 * @since v0.3.0
 */
public interface IconBearing {

  /**
   * Returns the icon's bounding box in this component's coordinate space. The returned rectangle
   * must reflect the current paint geometry — the anchor primitive queries this every time it
   * recomputes badge bounds (component resize, badge content change, ancestor move, etc.). A fresh
   * {@link Rectangle} should be returned each call; the anchor does not mutate the result but
   * shared state can lead to subtle aliasing if multiple anchors share a host.
   *
   * @return the icon's bounding box in this component's coordinate space; an empty rectangle if no
   *     icon is currently installed
   * @version v0.3.0
   * @since v0.3.0
   */
  Rectangle getIconBounds();
}
