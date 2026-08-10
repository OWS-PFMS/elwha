package com.owspfm.elwha.theme;

/**
 * An immutable set of four independent corner radii — top-left, top-right, bottom-right,
 * bottom-left, clockwise from the top-left — in pixels.
 *
 * <p>The uniform case ({@link #uniform(int)}) is the ordinary single-radius round-rect. The
 * per-corner case ({@link #of(int, int, int, int)}) is what a connected {@code ElwhaButtonGroup}
 * needs: a segment's outward-facing corners take the group shape while its butted inner corners
 * stay nearly square, so a row of segments reads as one connected control.
 *
 * <p><strong>Units.</strong> Each value is a corner <em>radius</em> in pixels — not the {@code
 * arcWidth} diameter the {@link SurfacePainter#paint(java.awt.Graphics2D, int, int, int, ColorRole,
 * StateLayer, ColorRole, float) int-arc} overloads take. Radii are clamped to {@code >= 0} at
 * construction; the consuming painter additionally clamps each corner to half the shorter body axis
 * at paint time, so an over-large radius degrades to a clean pill rather than an artifact. To
 * request a pill end-cap, pass any radius {@code >=} half the body height.
 *
 * @author Charles Bryan
 * @version v0.3.0
 * @since v0.3.0
 */
public final class CornerRadii {

  private final int topLeft;
  private final int topRight;
  private final int bottomRight;
  private final int bottomLeft;

  private CornerRadii(
      final int topLeft, final int topRight, final int bottomRight, final int bottomLeft) {
    this.topLeft = Math.max(0, topLeft);
    this.topRight = Math.max(0, topRight);
    this.bottomRight = Math.max(0, bottomRight);
    this.bottomLeft = Math.max(0, bottomLeft);
  }

  /**
   * Creates a per-corner radii set, clockwise from the top-left corner.
   *
   * @param topLeft the top-left corner radius in pixels
   * @param topRight the top-right corner radius in pixels
   * @param bottomRight the bottom-right corner radius in pixels
   * @param bottomLeft the bottom-left corner radius in pixels
   * @return the radii set (negative inputs are clamped to {@code 0})
   * @version v0.3.0
   * @since v0.3.0
   */
  public static CornerRadii of(
      final int topLeft, final int topRight, final int bottomRight, final int bottomLeft) {
    return new CornerRadii(topLeft, topRight, bottomRight, bottomLeft);
  }

  /**
   * Creates a radii set with the same radius on all four corners.
   *
   * @param radius the corner radius in pixels, applied to every corner
   * @return the uniform radii set (a negative input is clamped to {@code 0})
   * @version v0.3.0
   * @since v0.3.0
   */
  public static CornerRadii uniform(final int radius) {
    return new CornerRadii(radius, radius, radius, radius);
  }

  /**
   * Returns the top-left corner radius in pixels.
   *
   * @return the top-left radius
   * @version v0.3.0
   * @since v0.3.0
   */
  public int topLeftPx() {
    return topLeft;
  }

  /**
   * Returns the top-right corner radius in pixels.
   *
   * @return the top-right radius
   * @version v0.3.0
   * @since v0.3.0
   */
  public int topRightPx() {
    return topRight;
  }

  /**
   * Returns the bottom-right corner radius in pixels.
   *
   * @return the bottom-right radius
   * @version v0.3.0
   * @since v0.3.0
   */
  public int bottomRightPx() {
    return bottomRight;
  }

  /**
   * Returns the bottom-left corner radius in pixels.
   *
   * @return the bottom-left radius
   * @version v0.3.0
   * @since v0.3.0
   */
  public int bottomLeftPx() {
    return bottomLeft;
  }

  /**
   * Returns whether all four corners carry the same radius.
   *
   * @return {@code true} if the radii are uniform
   * @version v0.3.0
   * @since v0.3.0
   */
  public boolean isUniform() {
    return topLeft == topRight && topRight == bottomRight && bottomRight == bottomLeft;
  }

  /**
   * Returns the largest of the four corner radii — useful for clip / bounds reasoning.
   *
   * @return the largest corner radius in pixels
   * @version v0.3.0
   * @since v0.3.0
   */
  public int largestPx() {
    return Math.max(Math.max(topLeft, topRight), Math.max(bottomRight, bottomLeft));
  }

  /**
   * Linearly interpolates between two radii sets, corner by corner. {@code t = 0} returns {@code
   * from}'s values, {@code t = 1} returns {@code to}'s values, in-between {@code t} returns the
   * per-corner mix rounded to the nearest pixel. {@code t} outside {@code [0, 1]} is clamped — the
   * morph painter consumes already-eased {@code t}, but defensive clamping keeps stray values from
   * extrapolating past the endpoints.
   *
   * @param from the radii at {@code t = 0}
   * @param to the radii at {@code t = 1}
   * @param t the interpolation phase; clamped to {@code [0, 1]}
   * @return a new {@code CornerRadii} with each corner interpolated
   * @throws NullPointerException if {@code from} or {@code to} is {@code null}
   * @version v0.3.0
   * @since v0.3.0
   */
  public static CornerRadii interpolate(
      final CornerRadii from, final CornerRadii to, final float t) {
    if (from == null) {
      throw new NullPointerException("from");
    }
    if (to == null) {
      throw new NullPointerException("to");
    }
    final float clamped = Math.max(0f, Math.min(1f, t));
    return of(
        lerp(from.topLeft, to.topLeft, clamped),
        lerp(from.topRight, to.topRight, clamped),
        lerp(from.bottomRight, to.bottomRight, clamped),
        lerp(from.bottomLeft, to.bottomLeft, clamped));
  }

  private static int lerp(final int a, final int b, final float t) {
    return Math.round(a + (b - a) * t);
  }

  @Override
  public boolean equals(final Object obj) {
    if (this == obj) {
      return true;
    }
    if (!(obj instanceof CornerRadii other)) {
      return false;
    }
    return topLeft == other.topLeft
        && topRight == other.topRight
        && bottomRight == other.bottomRight
        && bottomLeft == other.bottomLeft;
  }

  @Override
  public int hashCode() {
    int result = topLeft;
    result = 31 * result + topRight;
    result = 31 * result + bottomRight;
    result = 31 * result + bottomLeft;
    return result;
  }

  @Override
  public String toString() {
    return "CornerRadii["
        + topLeft
        + ", "
        + topRight
        + ", "
        + bottomRight
        + ", "
        + bottomLeft
        + "]";
  }
}
