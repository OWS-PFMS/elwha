package com.owspfm.elwha.theme;

/**
 * The easing curves the shape-morph helper composes on top of a normalized {@code [0, 1]} animation
 * progress — M3's motion-token vocabulary expressed as evaluable functions. Cubic-bezier presets
 * map {@code [0, 1]} → {@code [0, 1]} through the curve defined by their two control points; the
 * spring preset evaluates a critically-damped second-order step response normalized to settle at
 * {@code t = 1}.
 *
 * <p>The pinned values mirror {@code docs/research/elwha-button-anim-design.md} §3 — the cubic
 * coefficients are the M3 motion-token presets ({@code motion.easing.standard}, {@code
 * motion.easing.emphasized.decelerate}, etc.), and the spring preset is M3's {@code
 * spring.spatial.default} at the §15.3-locked damping ratio of {@code 0.85}.
 *
 * <p><strong>Not part of the public API.</strong> Declared {@code public} only to cross the {@code
 * .theme} package boundary into the component packages that consume it via {@link
 * ShapeMorphPainter} and {@link MorphAnimator}.
 *
 * @author Charles Bryan
 * @version v0.3.0
 * @since v0.3.0
 */
@FunctionalInterface
public interface Easing {

  /**
   * Maps an unscaled progress {@code t} in {@code [0, 1]} to its eased value, also in {@code [0,
   * 1]}. Values outside the unit interval are accepted and computed but not clamped — the morph
   * helpers feed already-clamped {@code t}.
   *
   * @param t the unscaled progress
   * @return the eased value
   * @version v0.3.0
   * @since v0.3.0
   */
  float ease(float t);

  /** The identity curve — {@code ease(t) = t}. Mostly useful for testing. */
  Easing LINEAR = t -> t;

  /** {@code motion.easing.standard} — cubic-bezier {@code (0.2, 0, 0, 1)}. */
  Easing STANDARD = cubicBezier(0.2f, 0f, 0f, 1f);

  /** {@code motion.easing.standard.decelerate} — cubic-bezier {@code (0, 0, 0, 1)}. */
  Easing STANDARD_DECELERATE = cubicBezier(0f, 0f, 0f, 1f);

  /** {@code motion.easing.standard.accelerate} — cubic-bezier {@code (0.3, 0, 1, 1)}. */
  Easing STANDARD_ACCELERATE = cubicBezier(0.3f, 0f, 1f, 1f);

  /**
   * {@code motion.easing.emphasized} — the same cubic-bezier as {@link #STANDARD} per M3's motion
   * tokens; the difference between the two presets is the duration token they pair with, not the
   * curve shape (design doc §3).
   */
  Easing EMPHASIZED = STANDARD;

  /** {@code motion.easing.emphasized.decelerate} — cubic-bezier {@code (0.05, 0.7, 0.1, 1)}. */
  Easing EMPHASIZED_DECELERATE = cubicBezier(0.05f, 0.7f, 0.1f, 1f);

  /** {@code motion.easing.emphasized.accelerate} — cubic-bezier {@code (0.3, 0, 0.8, 0.15)}. */
  Easing EMPHASIZED_ACCELERATE = cubicBezier(0.3f, 0f, 0.8f, 0.15f);

  /**
   * {@code spring.spatial.default} — a critically-damped (per §15.3, damping ratio {@code 0.85})
   * spring evaluation normalized so the step response settles to {@code 1.0} at {@code t = 1}. The
   * underlying motion has imperceptible overshoot at this damping; tuning to {@code 0.7}-{@code
   * 0.75} would surface the playful underdamped read M3 Expressive uses elsewhere, deferred per
   * §15.3 until smoke-testing.
   */
  Easing SPRING_SPATIAL_DEFAULT = spring(0.85f);

  /**
   * Constructs a cubic-bezier easing curve from the two control points {@code (x1, y1)} and {@code
   * (x2, y2)}. The implicit endpoints are {@code (0, 0)} and {@code (1, 1)}. The returned function
   * inverts the curve's parametric {@code x(s)} via Newton-Raphson to find the {@code s} for a
   * given {@code t}, then evaluates {@code y(s)} — standard cubic-bezier easing implementation.
   *
   * @param x1 the first control point's x coordinate
   * @param y1 the first control point's y coordinate
   * @param x2 the second control point's x coordinate
   * @param y2 the second control point's y coordinate
   * @return the easing curve
   * @version v0.3.0
   * @since v0.3.0
   */
  static Easing cubicBezier(final float x1, final float y1, final float x2, final float y2) {
    return t -> {
      if (t <= 0f) {
        return 0f;
      }
      if (t >= 1f) {
        return 1f;
      }
      final float s = solveBezierX(t, x1, x2);
      return bezier(s, y1, y2);
    };
  }

  /**
   * Constructs a critically-damped underdamped spring easing — a second-order step response
   * normalized to reach {@code 1.0} at {@code t = 1}. {@code damping} below {@code 1.0} produces a
   * brief overshoot; {@code 1.0} is critically damped (no overshoot, slower rise); values above
   * {@code 1.0} are clamped to {@code 1.0}.
   *
   * <p>The settling factor is fixed at {@code ω = 8.0} so the response is visually complete by the
   * host animator's nominal duration — the morph helpers feed already-windowed progress, so the
   * spring evaluates within its own normalized {@code [0, 1]} regardless of the host's wall-clock
   * duration.
   *
   * @param damping the damping ratio in {@code (0, 1]} — {@code 0.85} is the §15.3-locked default
   * @return the easing curve
   * @version v0.3.0
   * @since v0.3.0
   */
  static Easing spring(final float damping) {
    final float zeta = Math.max(0.01f, Math.min(1f, damping));
    final float omega = 8f;
    return t -> {
      if (t <= 0f) {
        return 0f;
      }
      if (t >= 1f) {
        return 1f;
      }
      if (zeta >= 1f) {
        // Critically damped — closed form (1 - (1 + ωt) e^(-ωt)).
        final float wt = omega * t;
        return 1f - (1f + wt) * (float) Math.exp(-wt);
      }
      final float wd = omega * (float) Math.sqrt(1f - zeta * zeta);
      final float decay = (float) Math.exp(-zeta * omega * t);
      final float oscillation =
          (float) Math.cos(wd * t) + (zeta * omega / wd) * (float) Math.sin(wd * t);
      return 1f - decay * oscillation;
    };
  }

  private static float bezier(final float s, final float p1, final float p2) {
    final float u = 1f - s;
    return 3f * u * u * s * p1 + 3f * u * s * s * p2 + s * s * s;
  }

  private static float bezierDerivative(final float s, final float p1, final float p2) {
    final float u = 1f - s;
    return 3f * u * u * p1 + 6f * u * s * (p2 - p1) + 3f * s * s * (1f - p2);
  }

  private static float solveBezierX(final float t, final float x1, final float x2) {
    // Newton-Raphson is fast and stable for well-behaved control points (the M3 presets all are);
    // 8 iterations comfortably reaches float precision.
    float s = t;
    for (int i = 0; i < 8; i++) {
      final float xs = bezier(s, x1, x2);
      final float dxs = bezierDerivative(s, x1, x2);
      if (Math.abs(dxs) < 1e-6f) {
        break;
      }
      final float ns = s - (xs - t) / dxs;
      if (Math.abs(ns - s) < 1e-6f) {
        return Math.max(0f, Math.min(1f, ns));
      }
      s = ns;
    }
    return Math.max(0f, Math.min(1f, s));
  }
}
