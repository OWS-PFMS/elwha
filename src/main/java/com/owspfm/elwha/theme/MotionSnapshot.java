package com.owspfm.elwha.theme;

import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.geom.AffineTransform;
import java.awt.image.BufferedImage;
import javax.swing.JComponent;

/**
 * A cached device-resolution render of a surface, reused across the frames of an entrance or exit
 * tween — the shared engine behind {@code ElwhaDialog}, {@code ElwhaFullScreenDialog} and the side
 * sheet's slide, which each carried their own copy of it.
 *
 * <p><strong>Why a surface rasterizes itself at all.</strong> A transform-and-composite tween wants
 * one stable render, not a fresh one per frame. Re-running the full shadow and child paint every
 * tick is the dominant cost behind open/close lag under load, and re-rasterizing text at a
 * different scale each frame makes glyph advances snap to the pixel grid differently every time —
 * the left/right "shuffle" the snapshot exists to prevent. So the surface is drawn once at full
 * size, and each of the ~18 frames only offsets, scales or fades that one bitmap.
 *
 * <p><strong>Usage.</strong> A host with a {@code paint} override drives three calls:
 *
 * <pre>{@code
 * private final MotionSnapshot snapshot = new MotionSnapshot();
 *
 * public void paint(Graphics g) {
 *   float p = clamp(motionProgress);
 *   if (p >= 1f) {                       // steady state: no transform, no buffer
 *     snapshot.release();
 *     super.paint(g);
 *     return;
 *   }
 *   BufferedImage raster = snapshot.rasterFor((Graphics2D) g, getWidth(), getHeight(), super::paint);
 *   // ... the host's own transform + composite, then drawImage(raster, 0, 0, w, h, null)
 * }
 *
 * public boolean isPaintingOrigin() {
 *   return snapshot.paintingOrigin(motionProgress < 1f);
 * }
 * }</pre>
 *
 * <h2>The invalidation, and why {@code isPaintingOrigin} is the hook</h2>
 *
 * <p>The three original copies keyed their cache on {@code (deviceWidth, deviceHeight)} alone (<a
 * href="https://github.com/OWS-PFMS/elwha/issues/713">#713</a>). Nothing else could invalidate it,
 * and being a painting origin routes <em>every</em> descendant repaint back through the host's
 * {@code paint} — which then found a same-sized raster and re-blitted the stale one. So a spinner,
 * a caret, a per-tick ripple or any content that changed during the entrance rendered frozen for
 * the length of the tween. Bounded in time, but a real cache-key gap: the two mechanisms were each
 * correct alone and wrong together.
 *
 * <p>Detecting the change directly is not cheap, but detecting that one <em>happened</em> is:
 * {@link JComponent#isPaintingOrigin()} is consulted by {@code SwingUtilities.getPaintingOrigin},
 * which walks up from a repainting component and <strong>starts at that component's
 * parent</strong>. A host is therefore asked the question only when a <em>descendant</em> is
 * redirecting its repaint up — never when the host repaints itself, which is what the tween's own
 * frames do (the animator repaints the surface). Measured on JDK 21 rather than assumed: a surface
 * repainting itself queries its own {@code isPaintingOrigin()} zero times, a child repainting
 * queries it once.
 *
 * <p>That is why {@link #paintingOrigin(boolean)} both answers the question and marks the raster
 * stale. The coupling is deliberate and lives here so it is documented once instead of three times.
 * It also fails safe: if a future JDK asked more often than it does today, the raster would be
 * re-rendered more often — costing the caching, never the correctness.
 *
 * @author Charles Bryan
 * @version v0.5.0
 * @since v0.5.0
 */
public final class MotionSnapshot {

  /**
   * How the host renders itself live into a given {@link Graphics} — in practice a {@code
   * super::paint} reference, which is the whole reason this is a callback rather than a component
   * argument: a superclass paint cannot be invoked from outside the subclass.
   *
   * @author Charles Bryan
   * @version v0.5.0
   * @since v0.5.0
   */
  @FunctionalInterface
  public interface Render {

    /**
     * Paints the host's live content.
     *
     * @param g the graphics to paint into, already scaled to device resolution
     * @version v0.5.0
     * @since v0.5.0
     */
    void paint(Graphics g);
  }

  private BufferedImage raster;
  private int rasterWidth;
  private int rasterHeight;
  private boolean stale;

  /** Creates an empty snapshot; the first {@link #rasterFor} renders it. */
  public MotionSnapshot() {}

  /**
   * The raster to blit for this frame, rendering it when the cache cannot serve — because there is
   * none, because the device resolution changed, or because a descendant repainted since the last
   * one (see the class notes).
   *
   * <p>The buffer is sized in <em>device</em> pixels taken from {@code frame}'s own transform, and
   * the render is scaled to match, so a HiDPI host rasterizes at full fidelity rather than at
   * logical size and back up.
   *
   * @param frame the graphics for the frame being painted, read for its scale
   * @param width the host's width in logical pixels
   * @param height the host's height in logical pixels
   * @param render how to paint the host's live content
   * @return the raster to draw, never {@code null}
   * @version v0.5.0
   * @since v0.5.0
   */
  public BufferedImage rasterFor(
      final Graphics2D frame, final int width, final int height, final Render render) {
    final AffineTransform tx = frame.getTransform();
    final double sx = tx.getScaleX() > 0 ? tx.getScaleX() : 1.0;
    final double sy = tx.getScaleY() > 0 ? tx.getScaleY() : 1.0;
    final int deviceW = Math.max(1, (int) Math.ceil(width * sx));
    final int deviceH = Math.max(1, (int) Math.ceil(height * sy));
    if (raster != null && !stale && rasterWidth == deviceW && rasterHeight == deviceH) {
      return raster;
    }
    final BufferedImage rendered = new BufferedImage(deviceW, deviceH, BufferedImage.TYPE_INT_ARGB);
    final Graphics2D bg = rendered.createGraphics();
    try {
      bg.scale(sx, sy);
      render.paint(bg);
    } finally {
      bg.dispose();
    }
    raster = rendered;
    rasterWidth = deviceW;
    rasterHeight = deviceH;
    stale = false;
    return rendered;
  }

  /**
   * The host's {@link JComponent#isPaintingOrigin()} answer — and the invalidation hook, because
   * being asked <em>is</em> the signal that a descendant is repainting through the host. See the
   * class notes for why the two are one call.
   *
   * @param animating whether the host is mid-tween, i.e. whether it is a painting origin at all
   * @return {@code animating}, unchanged, for the host to return
   * @version v0.5.0
   * @since v0.5.0
   */
  public boolean paintingOrigin(final boolean animating) {
    stale |= animating;
    return animating;
  }

  /**
   * Drops the cached raster. Hosts call this on reaching the steady state, so a multi-megabyte
   * buffer does not outlive the ~300ms it was for, and so the next open re-renders against whatever
   * the theme, content and size have become.
   *
   * @version v0.5.0
   * @since v0.5.0
   */
  public void release() {
    raster = null;
    rasterWidth = 0;
    rasterHeight = 0;
    stale = false;
  }
}
