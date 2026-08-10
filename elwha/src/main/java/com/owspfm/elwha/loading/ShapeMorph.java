package com.owspfm.elwha.loading;

import java.awt.geom.Path2D;

/**
 * The loading indicator's morph + render math over {@link RoundedPolygonShape} radius profiles.
 * {@link #lerp} interpolates two profiles per-angle (seamless because both are closed periodic
 * functions over the same angular grid); {@link #toPath} turns a profile into a filled closed path
 * at a center, scale, and rotation. The continuous spin is expressed entirely as a {@code rotation}
 * offset on the sampling angle (design {@code elwha-loading-indicator-design.md} §2, §6).
 *
 * <p><strong>Not public API.</strong> Package-private to {@code com.owspfm.elwha.loading}.
 *
 * @author Charles Bryan
 * @version v0.5.0
 * @since v0.5.0
 */
final class ShapeMorph {

  private ShapeMorph() {}

  /**
   * Per-angle linear interpolation of two profiles.
   *
   * @param a the profile at {@code t = 0}
   * @param b the profile at {@code t = 1}
   * @param t the morph phase (clamped to {@code [0, 1]})
   * @return a fresh interpolated profile (same length as {@code a})
   * @version v0.5.0
   * @since v0.5.0
   */
  static float[] lerp(final float[] a, final float[] b, final float t) {
    final float tt = Math.max(0f, Math.min(1f, t));
    final float[] out = new float[a.length];
    for (int k = 0; k < a.length; k++) {
      out[k] = a[k] + (b[k] - a[k]) * tt;
    }
    return out;
  }

  /**
   * Builds a closed filled path from a radius profile.
   *
   * @param profile the radius profile (each entry in {@code [0, 1]})
   * @param centerX the center x, px
   * @param centerY the center y, px
   * @param scale the radius scale (the painted outer radius for a profile entry of {@code 1}), px
   * @param rotationRad the rotation applied to the sampling angle, radians (positive = clockwise on
   *     screen)
   * @return the closed path
   * @version v0.5.0
   * @since v0.5.0
   */
  static Path2D.Float toPath(
      final float[] profile,
      final float centerX,
      final float centerY,
      final float scale,
      final double rotationRad) {
    final int n = profile.length;
    final Path2D.Float path = new Path2D.Float();
    for (int k = 0; k < n; k++) {
      final double theta = 2.0 * Math.PI * k / n + rotationRad;
      final float r = profile[k] * scale;
      final float x = centerX + (float) (r * Math.cos(theta));
      final float y = centerY + (float) (r * Math.sin(theta));
      if (k == 0) {
        path.moveTo(x, y);
      } else {
        path.lineTo(x, y);
      }
    }
    path.closePath();
    return path;
  }
}
