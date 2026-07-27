package com.owspfm.elwha.loading;

import java.awt.Shape;
import java.awt.geom.AffineTransform;
import java.awt.geom.Ellipse2D;
import java.awt.geom.Path2D;
import java.awt.geom.PathIterator;
import java.awt.geom.RoundRectangle2D;
import java.util.ArrayList;
import java.util.List;

/**
 * An immutable, star-convex M3 shape captured as a <strong>radius profile</strong>: a fixed-length
 * array of radii sampled at uniform angles {@code θ_k = 2π·k/N}, centroid-relative and normalized
 * so the largest radius is {@code 1}. This is the foundation of the loading indicator's shape morph
 * (design {@code elwha-loading-indicator-design.md} §2): because every M3 loading-indicator shape
 * is star-convex (a ray from the centroid crosses the boundary exactly once), a shape reduces to a
 * single-valued function {@code r(θ)}, and a morph between two shapes is a per-angle {@code lerp}
 * of their profiles — seamless by construction (the profile is a closed periodic function over the
 * same angular grid), with no feature-matching {@code Morph} machinery to port.
 *
 * <p>Profiles are built once from a {@link Shape} outline by ray-casting {@link #SAMPLE_COUNT} rays
 * from the origin and recording the boundary distance along each. The outline is built with rounded
 * corners (quadratic fillets) so the rounding is captured into the profile; rendering the profile
 * back as a closed polyline ({@link ShapeMorph#toPath}) reproduces the outline, rounded corners
 * included.
 *
 * <p><strong>Not public API.</strong> Package-private to {@code com.owspfm.elwha.loading}. Extract
 * to {@code theme/} only when a second consumer needs it (design §2).
 *
 * @author Charles Bryan
 * @version v0.5.0
 * @since v0.5.0
 */
final class RoundedPolygonShape {

  /** The number of angular samples per profile — 2° steps; smooth past 64px (design §12). */
  static final int SAMPLE_COUNT = 180;

  private final float[] profile;

  private RoundedPolygonShape(final float[] profile) {
    this.profile = profile;
  }

  /**
   * The radius profile — {@code radii[k]} is the boundary radius at angle {@code θ_k = 2π·k/N},
   * normalized so the maximum entry is {@code 1}. The returned array is the live backing array; the
   * loading package treats it as read-only.
   *
   * @return the {@link #SAMPLE_COUNT}-length radius profile
   * @version v0.5.0
   * @since v0.5.0
   */
  float[] radii() {
    return profile;
  }

  /**
   * Builds a shape from a closed, star-convex outline centered on the origin.
   *
   * @param outline the source outline (centered at the origin)
   * @return the sampled, normalized shape
   * @version v0.5.0
   * @since v0.5.0
   */
  static RoundedPolygonShape fromOutline(final Shape outline) {
    final List<double[]> pts = flatten(outline);
    final float[] prof = new float[SAMPLE_COUNT];
    float max = 0f;
    for (int k = 0; k < SAMPLE_COUNT; k++) {
      final double theta = 2.0 * Math.PI * k / SAMPLE_COUNT;
      final double dx = Math.cos(theta);
      final double dy = Math.sin(theta);
      double best = 0.0;
      final int m = pts.size();
      for (int i = 0; i < m; i++) {
        final double[] p1 = pts.get(i);
        final double[] p2 = pts.get((i + 1) % m);
        final double r = rayHit(dx, dy, p1[0], p1[1], p2[0], p2[1]);
        if (r > best) {
          best = r;
        }
      }
      prof[k] = (float) best;
      if (prof[k] > max) {
        max = prof[k];
      }
    }
    if (max > 0f) {
      for (int k = 0; k < SAMPLE_COUNT; k++) {
        prof[k] /= max;
      }
    }
    return new RoundedPolygonShape(prof);
  }

  /**
   * A regular star — {@code points} outer vertices at radius 1 alternating with inner vertices at
   * {@code innerRatio}, every corner rounded by {@code roundness}.
   *
   * @param points the number of outer points
   * @param innerRatio the inner-vertex radius as a fraction of the outer radius
   * @param roundness the corner rounding in {@code [0, 1]} (fraction of the shorter adjacent edge)
   * @param rotationDeg a rotation applied to the whole shape, degrees
   * @return the shape
   * @version v0.5.0
   * @since v0.5.0
   */
  static RoundedPolygonShape star(
      final int points, final double innerRatio, final double roundness, final double rotationDeg) {
    final double[][] verts = new double[points * 2][];
    final double rot = Math.toRadians(rotationDeg);
    for (int i = 0; i < points * 2; i++) {
      final double a = rot - Math.PI / 2.0 + Math.PI * i / points;
      final double r = (i % 2 == 0) ? 1.0 : innerRatio;
      verts[i] = new double[] {r * Math.cos(a), r * Math.sin(a)};
    }
    return fromOutline(roundedPath(verts, roundness));
  }

  /**
   * A regular polygon with rounded corners.
   *
   * @param sides the number of sides
   * @param roundness the corner rounding in {@code [0, 1]}
   * @param rotationDeg a rotation applied to the whole shape, degrees
   * @return the shape
   * @version v0.5.0
   * @since v0.5.0
   */
  static RoundedPolygonShape polygon(
      final int sides, final double roundness, final double rotationDeg) {
    final double[][] verts = new double[sides][];
    final double rot = Math.toRadians(rotationDeg);
    for (int i = 0; i < sides; i++) {
      final double a = rot - Math.PI / 2.0 + 2.0 * Math.PI * i / sides;
      verts[i] = new double[] {Math.cos(a), Math.sin(a)};
    }
    return fromOutline(roundedPath(verts, roundness));
  }

  /**
   * A capsule / rounded rectangle centered on the origin.
   *
   * @param width the full width
   * @param height the full height
   * @param cornerRadius the corner arc radius (clamped to {@code min(width, height) / 2})
   * @param rotationDeg a rotation applied to the whole shape, degrees
   * @return the shape
   * @version v0.5.0
   * @since v0.5.0
   */
  static RoundedPolygonShape roundedRect(
      final double width,
      final double height,
      final double cornerRadius,
      final double rotationDeg) {
    final double r = Math.min(cornerRadius, Math.min(width, height) / 2.0);
    final Shape rect =
        new RoundRectangle2D.Double(-width / 2.0, -height / 2.0, width, height, 2.0 * r, 2.0 * r);
    return fromOutline(rotate(rect, rotationDeg));
  }

  /**
   * An ellipse centered on the origin.
   *
   * @param radiusX the semi-axis along x
   * @param radiusY the semi-axis along y
   * @param rotationDeg a rotation applied to the ellipse, degrees
   * @return the shape
   * @version v0.5.0
   * @since v0.5.0
   */
  static RoundedPolygonShape ellipse(
      final double radiusX, final double radiusY, final double rotationDeg) {
    final Shape e = new Ellipse2D.Double(-radiusX, -radiusY, 2.0 * radiusX, 2.0 * radiusY);
    return fromOutline(rotate(e, rotationDeg));
  }

  /**
   * A circle — a constant profile.
   *
   * @return the circle shape
   * @version v0.5.0
   * @since v0.5.0
   */
  static RoundedPolygonShape circle() {
    final float[] prof = new float[SAMPLE_COUNT];
    java.util.Arrays.fill(prof, 1f);
    return new RoundedPolygonShape(prof);
  }

  /** Builds a closed path through {@code verts} with every corner rounded by a quadratic fillet. */
  private static Path2D.Double roundedPath(final double[][] verts, final double roundness) {
    final int n = verts.length;
    final Path2D.Double path = new Path2D.Double();
    final double k = Math.max(0.0, Math.min(1.0, roundness));
    for (int i = 0; i < n; i++) {
      final double[] prev = verts[(i - 1 + n) % n];
      final double[] cur = verts[i];
      final double[] next = verts[(i + 1) % n];
      final double trimPrev = 0.5 * dist(cur, prev) * k;
      final double trimNext = 0.5 * dist(cur, next) * k;
      final double[] a = along(cur, prev, trimPrev);
      final double[] b = along(cur, next, trimNext);
      if (i == 0) {
        path.moveTo(a[0], a[1]);
      } else {
        path.lineTo(a[0], a[1]);
      }
      if (k > 0.0) {
        path.quadTo(cur[0], cur[1], b[0], b[1]);
      } else {
        path.lineTo(cur[0], cur[1]);
      }
    }
    path.closePath();
    return path;
  }

  private static double[] along(final double[] from, final double[] toward, final double d) {
    final double dx = toward[0] - from[0];
    final double dy = toward[1] - from[1];
    final double len = Math.hypot(dx, dy);
    if (len < 1e-9) {
      return new double[] {from[0], from[1]};
    }
    return new double[] {from[0] + dx / len * d, from[1] + dy / len * d};
  }

  private static double dist(final double[] p, final double[] q) {
    return Math.hypot(p[0] - q[0], p[1] - q[1]);
  }

  private static Shape rotate(final Shape s, final double rotationDeg) {
    if (rotationDeg == 0.0) {
      return s;
    }
    return AffineTransform.getRotateInstance(Math.toRadians(rotationDeg)).createTransformedShape(s);
  }

  /** Flattens an outline to its closed polyline vertex list. */
  private static List<double[]> flatten(final Shape outline) {
    final List<double[]> pts = new ArrayList<>();
    final PathIterator it = outline.getPathIterator(null, 0.005);
    final double[] c = new double[6];
    while (!it.isDone()) {
      final int type = it.currentSegment(c);
      if (type == PathIterator.SEG_MOVETO || type == PathIterator.SEG_LINETO) {
        pts.add(new double[] {c[0], c[1]});
      }
      it.next();
    }
    return pts;
  }

  /**
   * Distance from the origin to where the ray {@code s·(dx, dy)} ({@code s ≥ 0}) crosses segment
   * {@code (x1,y1)→(x2,y2)}, or {@code 0} if it does not.
   */
  private static double rayHit(
      final double dx,
      final double dy,
      final double x1,
      final double y1,
      final double x2,
      final double y2) {
    final double ex = x2 - x1;
    final double ey = y2 - y1;
    final double det = ex * dy - dx * ey;
    if (Math.abs(det) < 1e-12) {
      return 0.0;
    }
    final double s = (ex * y1 - ey * x1) / det;
    final double u = (dx * y1 - dy * x1) / det;
    if (s >= 0.0 && u >= -1e-9 && u <= 1.0 + 1e-9) {
      return s;
    }
    return 0.0;
  }
}
