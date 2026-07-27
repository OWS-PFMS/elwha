package com.owspfm.elwha.loading;

/**
 * The M3 loading-indicator shape catalog and the two morph sequences (design {@code
 * elwha-loading-indicator-design.md} §3). The shapes are visually-faithful radius-profile
 * reconstructions of the Material shape-library forms the Compose loading indicator cycles through
 * — the byte-exact vertex lists are not published, so the reconstructions are tuned by eye against
 * the spec renders (the progress-epic faithful-interpretation precedent).
 *
 * <p>The <strong>indeterminate</strong> sequence is the 7-shape loop {@code SoftBurst →
 * Cookie9Sided → Pentagon → Pill → Sunny → Cookie4Sided → Oval →} (wrapping back to SoftBurst). The
 * <strong>determinate</strong> sequence is {@code Circle → SoftBurst}, driven by progress.
 *
 * <p><strong>Not public API.</strong> Package-private to {@code com.owspfm.elwha.loading}.
 *
 * @author Charles Bryan
 * @version v0.5.0
 * @since v0.5.0
 */
final class LoadingShapes {

  private LoadingShapes() {}

  /** A near-perfect circle — the determinate start shape. */
  static final RoundedPolygonShape CIRCLE = RoundedPolygonShape.circle();

  /** An 8-point sun with gently rounded points. */
  static final RoundedPolygonShape SUNNY = RoundedPolygonShape.star(8, 0.80, 0.55, 0.0);

  /** A 9-lobe scalloped cookie. */
  static final RoundedPolygonShape COOKIE_9 = RoundedPolygonShape.star(9, 0.80, 1.0, -90.0);

  /** A soft 4-lobe scalloped cookie / cushion. */
  static final RoundedPolygonShape COOKIE_4 = RoundedPolygonShape.star(4, 0.68, 1.0, 0.0);

  /** A rounded pentagon. */
  static final RoundedPolygonShape PENTAGON = RoundedPolygonShape.polygon(5, 0.45, 0.0);

  /** A horizontal capsule. */
  static final RoundedPolygonShape PILL = RoundedPolygonShape.roundedRect(2.0, 1.22, 0.61, 0.0);

  /** A tilted oval. */
  static final RoundedPolygonShape OVAL = RoundedPolygonShape.ellipse(1.0, 0.64, -45.0);

  /** A soft 10-lobe burst. */
  static final RoundedPolygonShape SOFT_BURST = RoundedPolygonShape.star(10, 0.85, 0.85, 0.0);

  /** The indeterminate morph loop (wrap handled by the caller via {@code index % length}). */
  static final RoundedPolygonShape[] INDETERMINATE = {
    SOFT_BURST, COOKIE_9, PENTAGON, PILL, SUNNY, COOKIE_4, OVAL
  };

  /** The determinate morph sequence, mapped across the progress fraction. */
  static final RoundedPolygonShape[] DETERMINATE = {CIRCLE, SOFT_BURST};
}
