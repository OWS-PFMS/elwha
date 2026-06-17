/**
 * The Elwha Material 3 Expressive loading indicator — epic <a
 * href="https://github.com/OWS-PFMS/elwha/issues/468">#468</a>. {@link
 * com.owspfm.elwha.loading.ElwhaLoadingIndicator} is a dedicated {@link javax.swing.JComponent}
 * that continuously rotates a filled rounded-polygon while morphing it through a fixed sequence of
 * M3 shape-library forms — the short-wait spinner M3 says "should replace most uses of the
 * indeterminate circular progress indicator." It is a <em>separate</em> component from the progress
 * indicators ({@link com.owspfm.elwha.progress}), not a variant of them.
 *
 * <p>The package carries its own self-contained shape-morph engine ({@link
 * com.owspfm.elwha.loading.RoundedPolygonShape} radius profiles + {@link
 * com.owspfm.elwha.loading.ShapeMorph}) because Elwha's existing {@code ShapeMorphPainter} only
 * interpolates corner radii on a rounded rectangle. Every M3 loading-indicator shape is
 * star-convex, so a shape is a radius profile {@code r(θ)} and morphing is a per-angle lerp —
 * seamless, no feature-matching. Spec: {@code docs/research/elwha-loading-indicator-design.md} +
 * {@code elwha-loading-indicator-research.md}.
 *
 * @author Charles Bryan
 * @version v0.5.0
 * @since v0.5.0
 */
package com.owspfm.elwha.loading;
