package com.owspfm.elwha.card;

import java.awt.Graphics2D;

/**
 * Functional interface for procedurally-rendered {@link ElwhaCardMedia} content. The chassis
 * invokes {@link #paint(Graphics2D, int, int)} during {@link ElwhaCardMedia#paintComponent} with
 * the actual slot dimensions, so painters can scale / clip to the assigned area without having to
 * close over the component reference or read {@code Graphics2D.getClipBounds} (which can be mutated
 * by the chassis's own clip operations).
 *
 * <p>Replaces the v0.2-development {@code Consumer<Graphics2D>} painter callback shape, which gave
 * consumers no slot-size context and forced workarounds. Used by {@link
 * ElwhaCardMedia#painter(MediaPainter)}.
 *
 * @author Charles Bryan
 * @version v0.2.0
 * @since v0.2.0
 */
@FunctionalInterface
public interface MediaPainter {

  /**
   * Renders the media content into the slot.
   *
   * @param g the graphics context (the chassis sets antialiasing + bilinear interpolation hints
   *     before calling; consumers may further configure freely without mutating the caller's
   *     graphics — the chassis hands a defensive copy)
   * @param width the slot width in pixels
   * @param height the slot height in pixels
   * @version v0.2.0
   * @since v0.2.0
   */
  void paint(Graphics2D g, int width, int height);
}
