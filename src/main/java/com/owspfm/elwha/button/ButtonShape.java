package com.owspfm.elwha.button;

/**
 * Corner-radius treatment for an {@link ElwhaButton}.
 *
 * <p>Two options: {@link #ROUND} (a full capsule — the M3 default; {@link
 * com.owspfm.elwha.theme.SurfacePainter} clamps the arc to {@code min(width, height)}) and {@link
 * #SQUARE} (per-size corner radius from the {@code BUTTON_SQUARE_CORNERS_DP} table inside {@link
 * ElwhaButton}). The shape is static for the component's lifetime — shape morph on press /
 * selection is deferred to a future animation epic per {@code docs/research/elwha-button-design.md}
 * §10.
 *
 * @author Charles Bryan
 * @version v0.2.0
 * @since v0.2.0
 */
public enum ButtonShape {

  /**
   * Full capsule treatment — the M3 default. The corner radius equals half the shorter axis,
   * yielding a pill silhouette at any aspect ratio.
   *
   * @version v0.2.0
   * @since v0.2.0
   */
  ROUND,

  /**
   * Square treatment with a per-size corner radius. Story 3 hardcodes the Small radius (12 px);
   * Story 4 generalizes the full size axis.
   *
   * @version v0.2.0
   * @since v0.2.0
   */
  SQUARE
}
