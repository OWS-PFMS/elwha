package com.owspfm.elwha.theme;

import java.awt.AlphaComposite;
import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.Point;
import java.awt.RenderingHints;
import java.awt.geom.Ellipse2D;
import java.awt.geom.RoundRectangle2D;

/**
 * Library-internal helper that paints the M3-style expanding-circle press ripple — an
 * opacity-tinted ellipse seeded at the click point, expanding to the host body's diagonal then
 * fading out, clipped to the host's round-rect outline. Extracted verbatim from {@code
 * ElwhaCard.paintRipple} so the card, the upcoming {@code ElwhaButton} ({@link
 * com.owspfm.elwha.theme.ShadowPainter} sibling), and any future interactive primitive share one
 * ripple definition.
 *
 * <p><strong>Not part of the public API.</strong> Declared {@code public} only to cross the {@code
 * .theme} package boundary into the component packages that consume it.
 *
 * <p>The painter is stateless and does not own the animation timer — the caller seeds {@code
 * progress} from its own clock (or test harness) and repaints. The ripple animation curve is fixed:
 * a 250 ms expand phase (progress 0.000..0.625) with the fade tail starting at progress 0.375 and
 * running through progress 1.000, giving the M3-canonical 250 ms expand + 150 ms fade total
 * duration when the host's clock drives progress from 0 to 1 over 400 ms.
 *
 * @author Charles Bryan
 * @version v0.3.0
 * @since v0.2.0
 */
public final class RipplePainter {

  /**
   * The peak ripple opacity — matches {@code ElwhaCard.paintRipple}'s {@code 0.10f} factor.
   * Surfaces the constant for callers tuning the press-state layer compositing (e.g. button
   * variants that want a slightly stronger press tint).
   */
  public static final float PEAK_OPACITY = 0.10f;

  private RipplePainter() {}

  /**
   * Paints the ripple into {@code g} at component-local coordinates {@code (0, 0)..(width,
   * height)}. The caller is responsible for translating {@code g} to the host body's origin when
   * the body is inset from the component bounds (e.g. {@code ElwhaCard} shadow reserve).
   *
   * @param g the graphics context (not mutated; a defensive copy is taken for clip + compositing
   *     isolation)
   * @param width the host body width in pixels
   * @param height the host body height in pixels
   * @param origin the click-point in the same coordinate space as {@code (0,0)..(width,height)};
   *     {@code null} suppresses paint
   * @param progress the ripple animation phase in {@code [0, 1]}; values {@code >= 1f} suppress
   *     paint (the fade tail has completed)
   * @param cornerRadiusPx the host's round-rect corner radius in pixels; the ripple clips to this
   *     outline
   * @param tint the ripple fill color, painted at {@link #PEAK_OPACITY} scaled by the fade tail
   * @version v0.2.0
   * @since v0.2.0
   */
  public static void paint(
      Graphics2D g,
      int width,
      int height,
      Point origin,
      float progress,
      int cornerRadiusPx,
      Color tint) {
    if (origin == null || progress >= 1f || width <= 0 || height <= 0 || tint == null) {
      return;
    }
    final Graphics2D g2 = (Graphics2D) g.create();
    try {
      g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
      final int arc = Math.max(0, Math.min(cornerRadiusPx, Math.min(width, height)));
      g2.clip(new RoundRectangle2D.Float(0f, 0f, width, height, arc, arc));
      // Curve constants mirror ElwhaCard.paintRipple verbatim — expand phase reaches 1.0 at
      // progress 0.625 (= 250ms/400ms), fade tail starts at progress 0.375 (= 150ms/400ms) and
      // runs through 1.0. Overlap from 0.375..0.625 is intentional: the ripple fades while it's
      // still expanding to soften the peak.
      final float expand = Math.min(1f, progress * (400f / 250f));
      final float fade = Math.max(0f, 1f - Math.max(0f, (progress - 0.375f) / 0.625f));
      final int maxRadius = (int) Math.hypot(width, height);
      final int r = (int) (maxRadius * expand);
      g2.setComposite(AlphaComposite.SrcOver.derive(PEAK_OPACITY * fade));
      g2.setColor(tint);
      g2.fill(new Ellipse2D.Float(origin.x - r, origin.y - r, r * 2f, r * 2f));
    } finally {
      g2.dispose();
    }
  }

  /**
   * Per-corner counterpart to {@link #paint(Graphics2D, int, int, Point, float, int, Color)} — the
   * ripple clips to a four-radius rounded-rect outline instead of one uniform corner radius. A
   * connected {@code ElwhaButtonGroup} segment uses this so the press ripple stays inside the
   * segment's asymmetric corner treatment.
   *
   * @param g the graphics context (not mutated; a defensive copy is taken for clip + compositing
   *     isolation)
   * @param width the host body width in pixels
   * @param height the host body height in pixels
   * @param origin the click-point in the {@code (0,0)..(width,height)} space; {@code null}
   *     suppresses paint
   * @param progress the ripple animation phase in {@code [0, 1]}; values {@code >= 1f} suppress
   *     paint
   * @param radii the host's four corner radii; the ripple clips to this outline
   * @param tint the ripple fill color, painted at {@link #PEAK_OPACITY} scaled by the fade tail
   * @version v0.3.0
   * @since v0.3.0
   */
  public static void paint(
      Graphics2D g,
      int width,
      int height,
      Point origin,
      float progress,
      CornerRadii radii,
      Color tint) {
    if (origin == null || progress >= 1f || width <= 0 || height <= 0 || tint == null) {
      return;
    }
    final Graphics2D g2 = (Graphics2D) g.create();
    try {
      g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
      g2.clip(SurfacePainter.bodyShape(width, height, radii));
      final float expand = Math.min(1f, progress * (400f / 250f));
      final float fade = Math.max(0f, 1f - Math.max(0f, (progress - 0.375f) / 0.625f));
      final int maxRadius = (int) Math.hypot(width, height);
      final int r = (int) (maxRadius * expand);
      g2.setComposite(AlphaComposite.SrcOver.derive(PEAK_OPACITY * fade));
      g2.setColor(tint);
      g2.fill(new Ellipse2D.Float(origin.x - r, origin.y - r, r * 2f, r * 2f));
    } finally {
      g2.dispose();
    }
  }
}
