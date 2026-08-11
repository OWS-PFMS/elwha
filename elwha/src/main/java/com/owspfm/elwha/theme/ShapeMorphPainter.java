package com.owspfm.elwha.theme;

import java.awt.Graphics2D;

/**
 * Library-internal helper that paints a {@link SurfacePainter} surface body at a per-frame
 * interpolated {@link CornerRadii}, the foundation of the M3 Expressive shape-morph motion. Pairs
 * with {@link MorphAnimator} the way {@link RipplePainter} pairs with each consumer's own ripple
 * Timer — this painter is stateless, the consumer drives {@code progress} from its own clock and
 * repaints, exactly as today's ripple does.
 *
 * <p>The painter composes {@link Easing} on top of the consumer's raw {@code progress} (so a single
 * {@link MorphAnimator} feeding several morphs can apply a different curve to each), then defers
 * rendering to {@link SurfacePainter#paint(Graphics2D, int, int, CornerRadii, ColorRole,
 * StateLayer, ColorRole, float)} — the surface look is unchanged, only its geometry interpolates
 * frame-by-frame. The motion vocabulary (curves, durations, the {@code spring.spatial.default}
 * preset) is locked in {@code docs/research/elwha-button-anim-design.md} §3.
 *
 * <p><strong>Not part of the public API.</strong> Declared {@code public} only to cross the {@code
 * .theme} package boundary into the component packages that consume it.
 *
 * @author Charles Bryan
 * @version v0.3.0
 * @since v0.3.0
 */
public final class ShapeMorphPainter {

  private ShapeMorphPainter() {}

  /**
   * Interpolates between two radii sets through {@code easing} and returns the per-corner radii at
   * the eased phase. Equivalent to {@code CornerRadii.interpolate(from, to, easing.ease(progress))}
   * with {@link Easing#LINEAR} substituted when {@code easing} is {@code null}.
   *
   * @param from the radii at {@code progress = 0}
   * @param to the radii at {@code progress = 1}
   * @param progress the raw animation phase in {@code [0, 1]}
   * @param easing the curve to apply; {@code null} is treated as {@link Easing#LINEAR}
   * @return the interpolated radii at {@code easing(progress)}
   * @throws NullPointerException if {@code from} or {@code to} is {@code null}
   * @version v0.3.0
   * @since v0.3.0
   */
  public static CornerRadii interpolate(
      final CornerRadii from, final CornerRadii to, final float progress, final Easing easing) {
    final Easing e = easing != null ? easing : Easing.LINEAR;
    return CornerRadii.interpolate(from, to, e.ease(progress));
  }

  /**
   * Paints the surface body at the interpolated geometry — convenience that combines {@link
   * #interpolate(CornerRadii, CornerRadii, float, Easing)} with {@link SurfacePainter#paint(
   * Graphics2D, int, int, CornerRadii, ColorRole, StateLayer, ColorRole, float)}.
   *
   * <p>The caller still owns the {@link MorphAnimator} driving {@code progress} and the {@link
   * Easing} applied to it. Callers needing a render path other than {@code SurfacePainter} — for
   * instance, an {@code ElwhaIconButton} composing a shape morph with its own glyph paint — should
   * call {@link #interpolate(CornerRadii, CornerRadii, float, Easing)} directly and feed the radii
   * to whatever paint they already do.
   *
   * @param g the graphics context (not mutated; the underlying {@code SurfacePainter} takes a
   *     defensive copy)
   * @param width the host body width in pixels
   * @param height the host body height in pixels
   * @param from the radii at {@code progress = 0}
   * @param to the radii at {@code progress = 1}
   * @param progress the raw animation phase in {@code [0, 1]}; {@code 0} renders {@code from}'s
   *     geometry, {@code 1} renders {@code to}'s
   * @param easing the curve to apply to {@code progress}; {@code null} is treated as {@link
   *     Easing#LINEAR}
   * @param surfaceRole the surface fill role (passed through to {@code SurfacePainter.paint})
   * @param overlay the state-layer overlay (passed through to {@code SurfacePainter.paint})
   * @param borderRole the border stroke role (passed through to {@code SurfacePainter.paint})
   * @param borderWidthPx the border stroke width in pixels (passed through to {@code
   *     SurfacePainter.paint})
   * @throws NullPointerException if {@code from} or {@code to} is {@code null}
   * @version v0.3.0
   * @since v0.3.0
   */
  public static void paint(
      final Graphics2D g,
      final int width,
      final int height,
      final CornerRadii from,
      final CornerRadii to,
      final float progress,
      final Easing easing,
      final ColorRole surfaceRole,
      final StateLayer overlay,
      final ColorRole borderRole,
      final float borderWidthPx) {
    final CornerRadii radii = interpolate(from, to, progress, easing);
    SurfacePainter.paint(g, width, height, radii, surfaceRole, overlay, borderRole, borderWidthPx);
  }
}
