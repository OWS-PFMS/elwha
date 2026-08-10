package com.owspfm.elwha.theme;

/**
 * Library-internal helper exposing the content-side primitives of an M3 Expressive shape-morph —
 * the icon translation, label cross-fade, and container width interpolation that pair with {@link
 * ShapeMorphPainter}'s body-geometry morph. Parallel to {@link ShapeMorphPainter} the way {@link
 * RipplePainter} pairs with each consumer's own ripple Timer: stateless, the caller drives {@code
 * progress} from its own clock (a {@link MorphAnimator}) and repaints frame-by-frame.
 *
 * <p>Unlike {@link ShapeMorphPainter#interpolate(CornerRadii, CornerRadii, float, Easing)} the
 * primitives here do <strong>not</strong> compose {@link Easing} on top of the caller's {@code
 * progress} — each consumer settles its own curve once and feeds the already-eased value in, so a
 * single {@link MorphAnimator} feeding three parallel content transitions (e.g. width + icon X +
 * label alpha on {@code ElwhaFab}) can share one eased value across all three primitives. The
 * design vocabulary (curves, durations, the {@code container-emphasized} 0.5 cross-fade inflection)
 * is locked in {@code docs/research/elwha-fab-design.md} §9.
 *
 * <p><strong>Not part of the public API.</strong> Declared {@code public} only to cross the {@code
 * .theme} package boundary into the component packages that consume it (today {@code
 * com.owspfm.elwha.fab}; future Navigation Rail expand / collapse).
 *
 * @author Charles Bryan
 * @version v0.3.0
 * @since v0.3.0
 */
public final class ContentMorphPainter {

  private ContentMorphPainter() {}

  /**
   * Default cross-fade inflection point for {@link #labelAlpha(float)} — {@code 0.5f}, the M3
   * container-emphasized midpoint where the outgoing label has fully faded out and the incoming
   * label begins fading in. See design doc §9.1.
   *
   * @since v0.3.0
   */
  public static final float DEFAULT_LABEL_CROSSFADE_INFLECTION = 0.5f;

  /**
   * Returns the eased horizontal icon position interpolated between two anchors — the "icon
   * translation" half of the M3 FAB ↔ Extended FAB morph (centered ↔ leading inset per design doc
   * §9.1).
   *
   * <p>The caller passes an already-eased progress value (typically {@code easing.ease(animator
   * .progress())}); this helper performs a single linear interpolation between {@code fromX} and
   * {@code toX} and rounds to the nearest pixel. {@code progress} outside {@code [0, 1]} is not
   * clamped — over- and under-shoot is valid M3 spring behavior.
   *
   * @param fromX the icon X position at {@code progress = 0}, in pixels
   * @param toX the icon X position at {@code progress = 1}, in pixels
   * @param progress the already-eased animation phase in {@code [0, 1]} (over- / under-shoot
   *     permitted)
   * @return the interpolated icon X position in pixels, rounded to the nearest integer
   * @version v0.3.0
   * @since v0.3.0
   */
  public static int iconX(final int fromX, final int toX, final float progress) {
    return Math.round(fromX + (toX - fromX) * progress);
  }

  /**
   * Returns the label opacity at the default {@code 0.5} cross-fade inflection — equivalent to
   * {@link #labelAlpha(float, float)} with {@link #DEFAULT_LABEL_CROSSFADE_INFLECTION}. The
   * outgoing label is fully transparent at {@code progress = 0.5}, the incoming label is fully
   * opaque at {@code progress = 1.0}.
   *
   * @param progress the already-eased animation phase in {@code [0, 1]}
   * @return the label opacity in {@code [0, 1]}
   * @version v0.3.0
   * @since v0.3.0
   */
  public static float labelAlpha(final float progress) {
    return labelAlpha(progress, DEFAULT_LABEL_CROSSFADE_INFLECTION);
  }

  /**
   * Returns the label opacity with a caller-specified cross-fade inflection point — the {@code
   * progress} value at which the label first becomes visible (forward direction) or fully fades
   * (reverse direction). The default {@code 0.5} matches the M3 container-emphasized pattern in
   * {@code elwha-fab-design.md} §9.1; consumers needing an earlier / later crossover pass a custom
   * value here.
   *
   * <p>Behavior: at {@code progress <= inflection} returns {@code 0}; at {@code progress = 1}
   * returns {@code 1}; in between the value ramps linearly. Going Standard → Extended (forward),
   * the body expands through the first half with no visible label, then the label fades in during
   * the second half. Going Extended → Standard (reverse), the same curve runs backwards.
   *
   * @param progress the already-eased animation phase in {@code [0, 1]}
   * @param inflection the cross-fade midpoint in {@code (0, 1)} — the {@code progress} value below
   *     which the label is fully transparent
   * @return the label opacity clamped to {@code [0, 1]}
   * @throws IllegalArgumentException if {@code inflection} is not in {@code (0, 1)}
   * @version v0.3.0
   * @since v0.3.0
   */
  public static float labelAlpha(final float progress, final float inflection) {
    if (!(inflection > 0f && inflection < 1f)) {
      throw new IllegalArgumentException(
          "inflection must be in (0, 1), exclusive — got " + inflection);
    }
    final float scaled = (progress - inflection) / (1f - inflection);
    if (scaled <= 0f) {
      return 0f;
    }
    if (scaled >= 1f) {
      return 1f;
    }
    return scaled;
  }

  /**
   * Returns the eased container width interpolated between two endpoints — the "container width"
   * half of the M3 FAB ↔ Extended FAB morph. Used at layout time to drive a host's preferred-size
   * width through {@link MorphAnimator}'s frame ticks, and at paint time to size the body geometry
   * that {@link SurfacePainter} / {@link ShadowPainter} render into.
   *
   * <p>Short-circuits at the endpoints for paint-time stability (a steady-state component should
   * report exactly its end-state width, not a rounding-noise approximation).
   *
   * @param fromWidth the container width at {@code progress = 0}, in pixels
   * @param toWidth the container width at {@code progress = 1}, in pixels
   * @param progress the already-eased animation phase in {@code [0, 1]}
   * @return the interpolated container width in pixels, rounded to the nearest integer
   * @version v0.3.0
   * @since v0.3.0
   */
  public static int containerWidth(final int fromWidth, final int toWidth, final float progress) {
    if (progress <= 0f) {
      return fromWidth;
    }
    if (progress >= 1f) {
      return toWidth;
    }
    return Math.round(fromWidth + (toWidth - fromWidth) * progress);
  }
}
